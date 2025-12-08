package com.resismart.backend.pagos.Repositories;

import com.resismart.backend.pagos.Entities.TransaccionPago;
import com.resismart.backend.pagos.Enums.EstadoTransaccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface TransaccionPagoRepository extends JpaRepository<TransaccionPago, Long> {
    @Query("""
        SELECT COALESCE(SUM(t.monto), 0)
        FROM TransaccionPago t
        WHERE t.estado = com.resismart.backend.pagos.Enums.EstadoTransaccion.APROBADO
          AND t.fechaPago BETWEEN :inicio AND :fin
          AND (:condominioId IS NULL OR t.ordenPago.contrato.unidad.condominio.id = :condominioId)
    """)
    BigDecimal sumarIngresosAprobados(@Param("inicio") LocalDateTime inicio,
                                      @Param("fin") LocalDateTime fin,
                                      @Param("condominioId") Integer condominioId);

    List<TransaccionPago> findTop5ByEstadoOrderByFechaPagoDesc(EstadoTransaccion estado);

    List<TransaccionPago> findTop5ByEstadoAndOrdenPago_Contrato_Unidad_Condominio_IdOrderByFechaPagoDesc(EstadoTransaccion estado,
                                                                                                          Integer condominioId);

    java.util.Optional<TransaccionPago> findTop1ByOrdenPago_IdOrderByFechaPagoDesc(Integer ordenId);

    @Query("""
        SELECT COALESCE(SUM(t.monto), 0)
        FROM TransaccionPago t
        WHERE t.estado = com.resismart.backend.pagos.Enums.EstadoTransaccion.APROBADO
          AND t.ordenPago.id = :ordenId
    """)
    BigDecimal sumarMontosAprobadosPorOrden(@Param("ordenId") Integer ordenId);

    List<TransaccionPago> findByEstadoOrderByFechaPagoDesc(EstadoTransaccion estado);

    List<TransaccionPago> findByOrdenPago_IdOrderByFechaPagoDesc(Integer ordenId);
}
