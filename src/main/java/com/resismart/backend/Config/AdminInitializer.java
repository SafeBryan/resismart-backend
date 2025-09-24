package com.resismart.backend.Config;

import com.resismart.backend.users.Entities.Usuario;
import com.resismart.backend.users.Enums.Rol;
import com.resismart.backend.users.Repositories.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminInitializer {

    @Bean
    CommandLineRunner initAdmin(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            String correoAdmin = "admin@resismart.com";

            if (usuarioRepository.findByCorreo(correoAdmin).isEmpty()) {
                Usuario admin = Usuario.builder()
                        .correo(correoAdmin)
                        .username("admin")
                        .password_hash(passwordEncoder.encode("1234"))
                        .rol(Rol.ADMIN) // Asegúrate que exista en tu enum Rol
                        .nombres("Administrador")
                        .apellidos("General")
                        .telefono("000000000")
                        .estado(true)
                        .build();

                usuarioRepository.save(admin);
                System.out.println("✅ Usuario administrador creado: " + correoAdmin);
            }
        };
    }
}
