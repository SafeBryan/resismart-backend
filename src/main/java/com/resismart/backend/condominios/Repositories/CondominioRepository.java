package com.resismart.backend.condominios.Repositories;

import com.resismart.backend.condominios.Entities.Condominio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CondominioRepository extends JpaRepository<Condominio, Integer> {
    boolean existsByCorreoIgnoreCase(String correo);

    @EntityGraph(attributePaths = {"unidades"})
    Optional<Condominio> findWithUnidadesById(Integer id);

    @Query("""
            SELECT c
            FROM Condominio c
            WHERE c.dueno.id_usuario = :idDueno
            """)
    Page<Condominio> findAllByDueno(@Param("idDueno") int idDueno, Pageable pageable);

    @Query("""
            SELECT c.id
            FROM Condominio c
            WHERE c.dueno.id_usuario = :idDueno
            """)
    List<Integer> findIdsByDueno(@Param("idDueno") int idDueno);

    @Query("""
            SELECT (COUNT(c) > 0) FROM Condominio c
            WHERE c.id = :idCondominio AND c.dueno.id_usuario = :idDueno
            """)
    boolean existsByIdAndDueno(@Param("idCondominio") int idCondominio, @Param("idDueno") int idDueno);

    @Query("""
            SELECT (COUNT(c) > 0) FROM Condominio c
            WHERE c.dueno.id_usuario = :idDueno AND c.activo = true
            """)
    boolean existsActivoByDuenoId(@Param("idDueno") int idDueno);
}
