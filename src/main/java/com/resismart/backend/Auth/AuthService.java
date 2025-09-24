package com.resismart.backend.Auth;



import com.resismart.backend.Auth.Jwt.JwtService;
import com.resismart.backend.users.Entities.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import com.resismart.backend.users.Repositories.UsuarioRepository;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    @Autowired
    private UsuarioRepository usuariosRepository;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private AuthenticationManager authenticationManager;

    public AuthResponse login(LoginRequest request) {
        try {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(),request.getPassword()));
        Usuario usuario=usuariosRepository.findByCorreo(request.getEmail()).orElseThrow();
        String token= jwtService.getToken(usuario);
        return AuthResponse.builder()
                .token(token)
                .build();
        } catch (AuthenticationException | NullPointerException e) {
            throw new RuntimeException(e);
        }
    }


}
