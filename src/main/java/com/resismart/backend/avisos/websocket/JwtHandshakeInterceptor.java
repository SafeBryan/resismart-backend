package com.resismart.backend.avisos.websocket;

import com.resismart.backend.Auth.Jwt.JwtService;
import com.resismart.backend.users.Entities.Usuario;
import com.resismart.backend.users.Repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        String token = resolveToken(request);
        if (!StringUtils.hasText(token)) {
            log.warn("Handshake rechazado: no se recibió token JWT en la conexión WS {}", request.getURI());
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        final String username;
        try {
            username = jwtService.getUsernameFromToken(token);
        } catch (Exception ex) {
            log.warn("Handshake rechazado: token inválido ({})", ex.getMessage());
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        Optional<Usuario> usuarioOpt = usuarioRepository.findByCorreo(username);
        if (usuarioOpt.isEmpty()) {
            log.warn("Handshake rechazado: usuario {} no encontrado", username);
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        Usuario usuario = usuarioOpt.get();
        if (!jwtService.isTokenValid(token, usuario)) {
            log.warn("Handshake rechazado: token inválido para usuario {}", username);
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        attributes.put("userId", usuario.getId_usuario());
        attributes.put("rol", usuario.getRol() != null ? usuario.getRol().name() : null);
        attributes.put("username", username);
        attributes.put("token", token);
        log.info("Handshake WebSocket autorizado para usuario {} (id={}, rol={})",
                username, usuario.getId_usuario(), usuario.getRol());

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
        String tokenParam = normalizar(obtenerParametro(request, "token"));
        if (StringUtils.hasText(tokenParam)) {
            return tokenParam;
        }

        List<String> headers = request.getHeaders().getOrDefault(HttpHeaders.AUTHORIZATION, List.of());
        for (String header : headers) {
            String token = normalizar(header);
            if (StringUtils.hasText(token)) {
                return token;
            }
        }
        return null;
    }

    private String normalizar(String token) {
        if (!StringUtils.hasText(token)) {
            return null;
        }
        String trimmed = token.trim();
        if (trimmed.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return trimmed.substring(7).trim();
        }
        return trimmed;
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
