package com.resismart.backend.condominios.Repositories;

import com.resismart.backend.condominios.Entities.Unidad;
import com.resismart.backend.condominios.Enums.UnidadEstado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UnidadRepository extends JpaRepository<Unidad, Integer> {
    List<Unidad> findByCondominio_Id(Integer condominioId);
    long countByCondominio_Id(Integer condominioId);
    long countByCondominio_IdAndEstado(Integer condominioId, UnidadEstado estado);
    boolean existsByNumeroIgnoreCaseAndCondominio_Id(String numero, Integer condominioId);

    @Query("""
            SELECT (COUNT(u) > 0)
            FROM Unidad u
            WHERE u.id = :idUnidad AND u.condominio.dueno.id_usuario = :idDueno
            """)
    boolean existsByIdAndDueno(@Param("idUnidad") int idUnidad, @Param("idDueno") int idDueno);
}
