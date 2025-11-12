package com.resismart.backend.avisos.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resismart.backend.avisos.DTO.AvisoPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class AvisoWebSocketHub {

    private final ObjectMapper objectMapper;

    private final Set<WebSocketSession> todasLasSesiones = ConcurrentHashMap.newKeySet();
    private final Map<Integer, Set<WebSocketSession>> sesionesPorUsuario = new ConcurrentHashMap<>();

    public void registrar(WebSocketSession session, Integer usuarioId) {
        todasLasSesiones.add(session);
        if (usuarioId != null) {
            sesionesPorUsuario
                    .computeIfAbsent(usuarioId, k -> ConcurrentHashMap.newKeySet())
                    .add(session);
        }
        log.debug("Registrada sesión {} para userId {} (total sesiones={}, activas usuario={})",
                session.getId(), usuarioId, todasLasSesiones.size(),
                usuarioId != null ? sesionesPorUsuario.getOrDefault(usuarioId, Set.of()).size() : 0);
    }

    public void desregistrar(WebSocketSession session) {
        todasLasSesiones.remove(session);
        sesionesPorUsuario.values().forEach(set -> set.remove(session));
        log.debug("Sesión {} removida. Total sesiones activas={}", session.getId(), todasLasSesiones.size());
    }

    public boolean enviarATodos(AvisoPayload payload) {
        boolean entregado = enviarAConjunto(todasLasSesiones, payload);
        log.info("Emisión broadcast de aviso {} (tipo {}) – entregado={}",
                payload.getId(), payload.getTipo(), entregado);
        return entregado;
    }

    public boolean enviarAUsuario(Integer usuarioId, AvisoPayload payload) {
        if (usuarioId == null) {
            log.debug("Aviso {} no enviado: userId nulo", payload.getId());
            return false;
        }
        boolean entregado = enviarAConjunto(sesionesPorUsuario.get(usuarioId), payload);
        log.info("Aviso {} -> usuario {} ({})", payload.getId(), usuarioId,
                entregado ? "entregado" : "sin sesiones activas");
        return entregado;
    }

    public Set<Integer> enviarAUsuarios(Collection<Integer> usuarioIds, AvisoPayload payload) {
        if (usuarioIds == null || usuarioIds.isEmpty()) {
            return Set.of();
        }
        Set<Integer> entregados = ConcurrentHashMap.newKeySet();
        for (Integer usuarioId : usuarioIds) {
            if (usuarioId == null) {
                continue;
            }
            if (enviarAUsuario(usuarioId, payload)) {
                entregados.add(usuarioId);
            }
        }
        log.debug("Aviso {} enviado a {} usuarios conectados / {} destinatarios",
                payload.getId(), entregados.size(), usuarioIds.size());
        return entregados;
    }

    private boolean enviarAConjunto(Collection<WebSocketSession> sesiones, AvisoPayload payload) {
        if (sesiones == null || sesiones.isEmpty()) {
            return false;
        }

        final String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (IOException e) {
            log.error("Error serializando aviso {} para WS: {}", payload.getId(), e.getMessage(), e);
            return false;
        }

        TextMessage mensaje = new TextMessage(json);
        List<WebSocketSession> invalidadas = new ArrayList<>();
        boolean entregado = false;
        for (WebSocketSession session : sesiones) {
            if (session == null || !session.isOpen()) {
                invalidadas.add(session);
                continue;
            }
            try {
                session.sendMessage(mensaje);
                entregado = true;
            } catch (IOException e) {
                log.warn("Fallo enviando aviso {} a sesión {}: {}", payload.getId(), session.getId(), e.getMessage());
                invalidadas.add(session);
            }
        }
        invalidadas.forEach(this::desregistrar);
        return entregado;
    }
}
