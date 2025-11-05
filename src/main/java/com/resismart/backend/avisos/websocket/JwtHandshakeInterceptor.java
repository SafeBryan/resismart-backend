package com.resismart.backend.avisos.websocket;

import com.resismart.backend.Auth.Jwt.JwtService;
import com.resismart.backend.users.Entities.Usuario;
import com.resismart.backend.users.Repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        String token = resolveToken(request);
        if (token == null) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        String username = jwtService.getUsernameFromToken(token);
        Optional<Usuario> usuarioOpt = usuarioRepository.findByCorreo(username);
        if (usuarioOpt.isEmpty()) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        Usuario usuario = usuarioOpt.get();
        if (!jwtService.isTokenValid(token, usuario)) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        attributes.put("userId", usuario.getId_usuario());
        attributes.put("rol", usuario.getRol() != null ? usuario.getRol().name() : null);

        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
        // No-op
    }

    private String resolveToken(ServerHttpRequest request) {
        String tokenParam = obtenerParametro(request, "token");
        if (tokenParam == null || tokenParam.isBlank()) {
            return null;
        }
        return tokenParam.startsWith("Bearer ") ? tokenParam.substring(7) : tokenParam;
    }

    private String obtenerParametro(ServerHttpRequest request, String nombre) {
        Map<String, List<String>> queryParams = UriComponentsBuilder.fromUri(request.getURI())
                .build()
                .getQueryParams();
        String valor = queryParams.getOrDefault(nombre, List.of())
                .stream()
                .findFirst()
                .orElse(null);

        if (valor == null && request instanceof ServletServerHttpRequest servletRequest) {
            String raw = servletRequest.getServletRequest().getParameter(nombre);
            if (raw != null && !raw.isBlank()) {
                valor = raw;
            }
        }
        return valor;
    }
}
