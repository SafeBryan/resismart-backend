package com.resismart.backend.Auth;

import com.resismart.backend.Auth.Entities.PasswordResetToken;
import com.resismart.backend.Auth.Jwt.JwtService;
import com.resismart.backend.Auth.Repositories.PasswordResetTokenRepository;
import com.resismart.backend.users.Entities.Usuario;
import com.resismart.backend.users.Repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final UsuarioRepository usuariosRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
            Usuario usuario = usuariosRepository.findByCorreo(request.getEmail()).orElseThrow();
            String token = jwtService.getToken(usuario);
            return AuthResponse.builder()
                    .token(token)
                    .build();
        } catch (AuthenticationException | NullPointerException e) {
            throw new RuntimeException(e);
        }
    }

    @Transactional
    public void solicitarRecuperacion(String email) {
        log.info("[forgot-password] Solicitud recibida para {}", email);
        Usuario usuario = usuariosRepository.findByCorreo(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        String token = UUID.randomUUID().toString();
        LocalDateTime expiration = LocalDateTime.now().plusMinutes(15);

        PasswordResetToken prt = passwordResetTokenRepository.findByUsuario(usuario).orElse(null);
        if (prt != null) {
            prt.setToken(token);
            prt.setFechaExpiracion(expiration);
        } else {
            prt = PasswordResetToken.builder()
                    .token(token)
                    .usuario(usuario)
                    .fechaExpiracion(expiration)
                    .build();
        }

        passwordResetTokenRepository.save(prt);
        log.info("[forgot-password] Token generado para {} expira en {}", email, prt.getFechaExpiracion());
        emailService.enviarCorreoRecuperacion(usuario.getCorreo(), token);
    }

    public boolean validarToken(String token) {
        return passwordResetTokenRepository.findByToken(token)
                .filter(t -> t.getFechaExpiracion().isAfter(LocalDateTime.now()))
                .isPresent();
    }

    @Transactional
    public void cambiarPassword(String token, String nuevaPassword) {
        log.info("[reset-password] Intentando reset con token {}", token);
        PasswordResetToken prt = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Token invalido"));
        if (prt.getFechaExpiracion().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Token expirado");
        }
        Usuario usuario = prt.getUsuario();
        usuario.setPassword_hash(passwordEncoder.encode(nuevaPassword));
        usuariosRepository.save(usuario);
        passwordResetTokenRepository.delete(prt);
        log.info("[reset-password] Password actualizada y token consumido para usuario {}", usuario.getCorreo());
    }

}
