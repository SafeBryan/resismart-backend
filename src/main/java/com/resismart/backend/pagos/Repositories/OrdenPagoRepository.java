// OrdenPagoRepository.java
package com.resismart.backend.pagos.Repositories;

import com.resismart.backend.pagos.Entities.OrdenPago;
import org.springframework.data.jpa.repository.*;
import java.time.LocalDate;
import java.util.List;

public interface OrdenPagoRepository extends JpaRepository<OrdenPago, Integer>, JpaSpecificationExecutor<OrdenPago> {

    boolean existsByContrato_IdAndPeriodo(Integer idContrato, LocalDate periodo);

    List<OrdenPago> findByContrato_Id(Integer idContrato);

    List<OrdenPago> findByPeriodo(LocalDate periodo);

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
}
