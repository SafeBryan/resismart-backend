package com.resismart.backend.avisos.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resismart.backend.Auth.Jwt.JwtService;
import com.resismart.backend.avisos.Services.AvisoService;
import com.resismart.backend.users.Entities.Usuario;
import com.resismart.backend.users.Repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class AvisoWebSocketHandler extends TextWebSocketHandler {

    private final AvisoWebSocketHub hub;
    private final AvisoService avisoService;
    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Integer usuarioId = resolverUsuario(session);
        if (usuarioId == null) {
            log.warn("Sesión WS {} rechazada: no se pudo resolver usuario (remote={})",
                    session.getId(), session.getRemoteAddress());
            safeClose(session, CloseStatus.NOT_ACCEPTABLE.withReason("Usuario no resuelto"));
            return;
        }

        session.getAttributes().put("userId", usuarioId);
        hub.registrar(session, usuarioId);
        log.info("Sesión WS {} abierta para usuario {} (remote={})",
                session.getId(), usuarioId, session.getRemoteAddress());
        try {
            avisoService.entregarPendientesUsuario(usuarioId);
        } catch (Exception ex) {
            log.error("Error entregando avisos pendientes a usuario {}: {}", usuarioId, ex.getMessage(), ex);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        Integer usuarioId = (Integer) session.getAttributes().get("userId");
        if (usuarioId == null) {
            log.warn("Mensaje entrante ignorado: sesión {} sin usuario asociado", session.getId());
            return;
        }
        String payload = message.getPayload();
        if (!StringUtils.hasText(payload)) {
            return;
        }
        try {
            WsAckMessage ack = objectMapper.readValue(payload, WsAckMessage.class);
            if (ack.isAck()) {
                List<Long> ids = ack.effectiveIds();
                if (CollectionUtils.isEmpty(ids)) {
                    return;
                }
                int actualizados = avisoService.marcarAvisosLeidos(usuarioId, ids);
                log.debug("Usuario {} confirmó {} avisos vía WS", usuarioId, actualizados);
            } else {
                log.debug("Mensaje entrante sin acción para sesión {}: {}", session.getId(), payload);
            }
        } catch (Exception ex) {
            log.warn("No se pudo interpretar mensaje WS de usuario {}: {}", usuarioId, ex.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        hub.desregistrar(session);
        log.info("Sesión WS {} cerrada ({}). userId={}", session.getId(), status, session.getAttributes().get("userId"));
    }

    private Integer resolverUsuario(WebSocketSession session) {
        Object attr = session.getAttributes().get("userId");
        if (attr instanceof Integer id) {
            return id;
        }
        String token = extraerToken(session);
        if (!StringUtils.hasText(token)) {
            return null;
        }
        try {
            String username = jwtService.getUsernameFromToken(token);
            Optional<Usuario> usuarioOpt = usuarioRepository.findByCorreo(username);
            return usuarioOpt.map(Usuario::getId_usuario).orElse(null);
        } catch (Exception ex) {
            log.warn("No se pudo derivar usuario desde token en sesión {}: {}", session.getId(), ex.getMessage());
            return null;
        }
    }

    private String extraerToken(WebSocketSession session) {
        Object attrToken = session.getAttributes().get("token");
        if (attrToken instanceof String token && StringUtils.hasText(token)) {
            return normalizarToken(token);
        }
        URI uri = session.getUri();
        if (uri != null) {
            MultiValueMap<String, String> params = UriComponentsBuilder.fromUri(uri)
                    .build()
                    .getQueryParams();
            String tokenParam = params.getFirst("token");
            if (StringUtils.hasText(tokenParam)) {
                return normalizarToken(tokenParam);
            }
        }
        List<String> headers = session.getHandshakeHeaders().getOrDefault(HttpHeaders.AUTHORIZATION, List.of());
        for (String header : headers) {
            if (StringUtils.hasText(header)) {
                return normalizarToken(header);
            }
        }
        return null;
    }

    private String normalizarToken(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return trimmed.substring(7).trim();
        }
        return trimmed;
    }

    private void safeClose(WebSocketSession session, CloseStatus status) {
        try {
            session.close(status);
        } catch (IOException e) {
            log.debug("Error cerrando sesión {}: {}", session.getId(), e.getMessage());
        }
    }

    private record WsAckMessage(String tipo, List<Long> avisos, List<Long> ids) {
        boolean isAck() {
            if (tipo == null) {
                return avisos != null || ids != null;
            }
            String normalized = tipo.trim().toLowerCase();
            return "ack".equals(normalized) || "visto".equals(normalized) || "leido".equals(normalized);
        }

        List<Long> effectiveIds() {
            if (avisos != null && !avisos.isEmpty()) {
                return avisos;
            }
            return ids != null ? ids : List.of();
        }
    }
}
