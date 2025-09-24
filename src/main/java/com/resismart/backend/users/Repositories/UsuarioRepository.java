package com.resismart.backend.users.Repositories;

import com.resismart.backend.users.Entities.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository< Usuario,Integer> {
    public Optional<Usuario> findByCorreo(String correo);
}
