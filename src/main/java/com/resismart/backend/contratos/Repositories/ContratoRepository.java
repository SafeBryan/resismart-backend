package com.resismart.backend.contratos.Repositories;

import com.resismart.backend.contratos.Entities.Contrato;
import com.resismart.backend.contratos.Enums.EstadoContrato;
import com.resismart.backend.residentes.Entities.Residente;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
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

    // com.resismart.backend.contratos.Repositories.ContratoRepository
    @Query("""
  select c
  from Contrato c
  join fetch c.unidad u
  join fetch c.residente r
  join fetch r.usuario ru
  where r.id = :idResidente
""")
    List<Contrato> findAllByResidenteIdWithJoins(@Param("idResidente") Long idResidente);

    @Query("""
  select c
  from Contrato c
  join fetch c.unidad u
  join fetch c.residente r
  join fetch r.usuario ru
  where c.estado = :estado
""")
    List<Contrato> findAllByEstadoWithJoins(@Param("estado") EstadoContrato estado);

    boolean existsByResidenteAndEstadoIn(Residente residente, Collection<EstadoContrato> estados);

    boolean existsByResidente_IdAndEstado(Long idResidente, EstadoContrato estado);

    @Query("""
        SELECT DISTINCT c.residente
        FROM Contrato c
        WHERE c.unidad.condominio.id = :condominioId
          AND c.estado IN :estados
    """)
    List<Residente> findResidentesPorCondominioYEstados(@Param("condominioId") Integer condominioId,
                                                        @Param("estados") Collection<EstadoContrato> estados);

    @Query("""
        SELECT COUNT(DISTINCT c.residente.id)
        FROM Contrato c
        WHERE c.unidad.condominio.id = :condominioId
          AND c.estado = com.resismart.backend.contratos.Enums.EstadoContrato.ACTIVO
    """)
    long countResidentesActivosPorCondominio(@Param("condominioId") Integer condominioId);

}
