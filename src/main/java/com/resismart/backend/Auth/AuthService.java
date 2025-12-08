package com.resismart.backend.Auth;

import com.resismart.backend.Auth.Entities.PasswordResetToken;
import com.resismart.backend.Auth.Jwt.JwtService;
import com.resismart.backend.Auth.Repositories.PasswordResetTokenRepository;
import com.resismart.backend.users.Entities.Usuario;
import com.resismart.backend.users.Repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
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

    public void solicitarRecuperacion(String email) {
        Usuario usuario = usuariosRepository.findByCorreo(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        passwordResetTokenRepository.deleteByUsuario(usuario);
        String token = UUID.randomUUID().toString();
        PasswordResetToken prt = PasswordResetToken.builder()
                .token(token)
                .usuario(usuario)
                .fechaExpiracion(LocalDateTime.now().plusMinutes(15))
                .build();
        passwordResetTokenRepository.save(prt);
        emailService.enviarCorreoRecuperacion(usuario.getCorreo(), token);
    }

    public boolean validarToken(String token) {
        return passwordResetTokenRepository.findByToken(token)
                .filter(t -> t.getFechaExpiracion().isAfter(LocalDateTime.now()))
                .isPresent();
    }

    public void cambiarPassword(String token, String nuevaPassword) {
        PasswordResetToken prt = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Token inválido"));
        if (prt.getFechaExpiracion().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Token expirado");
        }
        Usuario usuario = prt.getUsuario();
        usuario.setPassword_hash(passwordEncoder.encode(nuevaPassword));
        usuariosRepository.save(usuario);
        passwordResetTokenRepository.delete(prt);
    }

}
