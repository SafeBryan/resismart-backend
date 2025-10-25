package com.resismart.backend.users.Repositories;

import com.resismart.backend.users.Entities.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    Optional<Usuario> findByCorreo(String correo);

    @Query("""
            SELECT DISTINCT r.usuario
            FROM Residente r
            WHERE r.unidad.condominio.dueno.id_usuario = :idDueno
            """)
    List<Usuario> findUsuariosResidentesPorDueno(@Param("idDueno") int idDueno);
}
