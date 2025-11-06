package com.resismart.backend.users.Repositories;

import com.resismart.backend.users.Entities.Usuario;
import com.resismart.backend.users.Enums.Rol;
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

    @Query("""
            SELECT u.id_usuario
            FROM Usuario u
            WHERE u.estado = true
            """)
    List<Integer> findIdsUsuariosActivos();

    @Query("""
            SELECT u.id_usuario
            FROM Usuario u
            WHERE u.estado = true
              AND u.rol = :rol
            """)
    List<Integer> findIdsPorRol(@Param("rol") Rol rol);
}
