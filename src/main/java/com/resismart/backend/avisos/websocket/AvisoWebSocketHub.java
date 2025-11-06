package com.resismart.backend.avisos.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resismart.backend.avisos.DTO.AvisoPayload;
import lombok.RequiredArgsConstructor;
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
    }

    public void desregistrar(WebSocketSession session) {
        todasLasSesiones.remove(session);
        sesionesPorUsuario.values().forEach(set -> set.remove(session));
    }

    public boolean enviarATodos(AvisoPayload payload) {
        return enviarAConjunto(todasLasSesiones, payload);
    }

    public boolean enviarAUsuario(Integer usuarioId, AvisoPayload payload) {
        if (usuarioId == null) return false;
        return enviarAConjunto(sesionesPorUsuario.get(usuarioId), payload);
    }

    public Set<Integer> enviarAUsuarios(Collection<Integer> usuarioIds, AvisoPayload payload) {
        if (usuarioIds == null || usuarioIds.isEmpty()) {
            return Set.of();
        }
        Set<Integer> entregados = ConcurrentHashMap.newKeySet();
        for (Integer usuarioId : usuarioIds) {
            if (usuarioId == null) continue;
            if (enviarAUsuario(usuarioId, payload)) {
                entregados.add(usuarioId);
            }
        }
        return entregados;
    }

    private boolean enviarAConjunto(Collection<WebSocketSession> sesiones, AvisoPayload payload) {
        if (sesiones == null || sesiones.isEmpty()) {
            return false;
        }

        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (IOException e) {
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
                invalidadas.add(session);
            }
        }
        invalidadas.forEach(this::desregistrar);
        return entregado;
    }
}
