// OrdenPagoRepository.java
package com.resismart.backend.pagos.Repositories;

import com.resismart.backend.pagos.Entities.OrdenPago;
import com.resismart.backend.contratos.Entities.Contrato;
import com.resismart.backend.pagos.Enums.EstadoOrdenPago;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.math.BigDecimal;
import java.util.Collection;

public interface OrdenPagoRepository extends JpaRepository<OrdenPago, Integer>, JpaSpecificationExecutor<OrdenPago> {

    boolean existsByContrato_IdAndPeriodo(Integer idContrato, LocalDate periodo);

    List<OrdenPago> findByContrato_Id(Integer idContrato);

    List<OrdenPago> findByPeriodo(LocalDate periodo);

    List<OrdenPago> findByEstado(com.resismart.backend.pagos.Enums.EstadoOrdenPago estado);

    List<OrdenPago> findByEstadoAndFechaVencimientoBefore(com.resismart.backend.pagos.Enums.EstadoOrdenPago estado, LocalDate fecha);

    @Query("""
        SELECT op FROM OrdenPago op
        JOIN FETCH op.contrato c
        JOIN FETCH c.residente r
        JOIN FETCH r.usuario u
        JOIN FETCH c.unidad un
        JOIN FETCH un.condominio cond
        WHERE op.estado = com.resismart.backend.pagos.Enums.EstadoOrdenPago.PENDIENTE
          AND op.fechaVencimiento BETWEEN :desde AND :hasta
    """)
    List<OrdenPago> buscarPendientesConVencimientoEntre(LocalDate desde, LocalDate hasta);

    @Query("""
        SELECT COALESCE(SUM(op.saldoPendiente), 0)
        FROM OrdenPago op
        WHERE op.activo = true
          AND (:condominioId IS NULL OR op.contrato.unidad.condominio.id = :condominioId)
    """)
    BigDecimal sumarSaldoPendiente(@Param("condominioId") Integer condominioId);

    boolean existsByContratoInAndEstadoIn(Collection<Contrato> contratos, Collection<EstadoOrdenPago> estados);

    java.util.Optional<OrdenPago> findTop1ByContrato_IdOrderByPeriodoDesc(Integer contratoId);

    List<OrdenPago> findByEstadoInAndFechaVencimientoBeforeAndSaldoPendienteGreaterThan(
            Collection<EstadoOrdenPago> estados,
            LocalDate fechaVencimiento,
            BigDecimal saldoMinimo
    );

    List<OrdenPago> findByEstadoInAndSaldoPendienteGreaterThan(
            Collection<EstadoOrdenPago> estados,
            BigDecimal saldoMinimo
    );
}
