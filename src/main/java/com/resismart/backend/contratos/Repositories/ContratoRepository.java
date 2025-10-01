package com.resismart.backend.contratos.Repositories;

import com.resismart.backend.contratos.Entities.Contrato;
import com.resismart.backend.contratos.Enums.EstadoContrato;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ContratoRepository extends JpaRepository<Contrato, Integer> {

    boolean existsByUnidad_IdAndEstado(Integer idUnidad, EstadoContrato estado);

    List<Contrato> findByResidente_Id(Long idResidente);

    List<Contrato> findByEstado(EstadoContrato estado);

    boolean existsByUnidad_IdAndFechaInicioBeforeAndFechaFinAfter(
            Integer idUnidad,
            LocalDate fecha,
            LocalDate fecha2
    );

    @EntityGraph(attributePaths = {"unidad", "residente"})
    Optional<Contrato> findWithUnidadAndResidenteById(Integer id);

    @Query("""
        SELECT c FROM Contrato c
        WHERE c.estado = :estado
          AND c.fechaInicio <= :finMes
          AND (c.fechaFin IS NULL OR c.fechaFin >= :inicioMes)
    """)
    List<Contrato> findActivosVigentesEn(
            @Param("estado") EstadoContrato estado,
            @Param("inicioMes") LocalDate inicioMes,
            @Param("finMes") LocalDate finMes
    );
}
