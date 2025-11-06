package com.resismart.backend.residentes.Repositories;

import com.resismart.backend.residentes.Entities.Residente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResidenteRepository extends JpaRepository<Residente, Long> {
    Optional<Residente> findByCedula(String cedula);

    @Query("""
    SELECT r
    FROM Residente r
    WHERE r.usuario.id_usuario=:idUsuario
    """)
    List<Residente> findResidentesPorUsuario(@Param("idUsuario") int idUsuario);

    @Query("""
    SELECT r
    FROM Residente r
    WHERE r.unidad.condominio.id=:idCondominio
    """)
    List<Residente> findResidentesPorCondominio(@Param("idCondominio") int idCondominio);

    @Query("""
    SELECT r
    FROM Residente r
    WHERE r.unidad.condominio.dueno.id_usuario = :idDueno
    """)
    List<Residente> findResidentesPorDueno(@Param("idDueno") int idDueno);

    @Query("""
    SELECT (COUNT(r) > 0)
    FROM Residente r
    WHERE r.id = :idResidente AND r.unidad.condominio.dueno.id_usuario = :idDueno
    """)
    boolean existsByIdAndDueno(@Param("idResidente") long idResidente, @Param("idDueno") int idDueno);

    @Query("""
    SELECT r FROM Residente r WHERE r.usuario.id_usuario = :idUsuario
    """)
    Optional<Residente> findByUsuarioId(@Param("idUsuario") int idUsuario);

    @Query("SELECT r FROM Residente r WHERE r.usuario.id = :idUsuario")
    Optional<Residente> findByUsuarioId(@Param("idUsuario") Long idUsuario);
}
