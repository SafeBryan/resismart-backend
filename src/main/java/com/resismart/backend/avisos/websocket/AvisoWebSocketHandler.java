package com.resismart.backend.avisos.websocket;

import com.resismart.backend.avisos.Services.AvisoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
@RequiredArgsConstructor
public class AvisoWebSocketHandler extends TextWebSocketHandler {

    private final AvisoWebSocketHub hub;
    private final AvisoService avisoService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Integer usuarioId = (Integer) session.getAttributes().get("userId");
        hub.registrar(session, usuarioId);
        if (usuarioId != null) {
            avisoService.entregarPendientesUsuario(usuarioId);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // Conexion es solo push desde el servidor; ignoramos mensajes entrantes.
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        hub.desregistrar(session);
    }
}
