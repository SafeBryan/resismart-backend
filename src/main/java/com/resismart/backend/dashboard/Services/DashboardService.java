package com.resismart.backend.dashboard.Services;

import com.resismart.backend.dashboard.DTO.DashboardFinancieroDTO;
import com.resismart.backend.dashboard.DTO.TransaccionRecienteDTO;
import com.resismart.backend.pagos.Entities.TransaccionPago;
import com.resismart.backend.pagos.Enums.EstadoTransaccion;
import com.resismart.backend.pagos.Repositories.OrdenPagoRepository;
import com.resismart.backend.pagos.Repositories.TransaccionPagoRepository;
import com.resismart.backend.pagos.Services.CobranzaService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final TransaccionPagoRepository transaccionPagoRepository;
    private final OrdenPagoRepository ordenPagoRepository;
    private final CobranzaService cobranzaService;

    @Transactional
    public DashboardFinancieroDTO obtenerResumenFinanciero(Integer condominioId) {
        cobranzaService.actualizarEstadosOrdenes(LocalDate.now());

        LocalDate now = LocalDate.now();
        LocalDateTime inicioMes = now.withDayOfMonth(1).atStartOfDay();
        LocalDateTime finMes = now.plusMonths(1).withDayOfMonth(1).atStartOfDay().minusNanos(1);

        BigDecimal ingresos = safe(transaccionPagoRepository.sumarIngresosAprobados(inicioMes, finMes, condominioId));
        BigDecimal deuda = safe(ordenPagoRepository.sumarSaldoPendiente(condominioId));
        BigDecimal balance = ingresos;

        List<TransaccionRecienteDTO> recientes = mapRecientes(condominioId);

        return DashboardFinancieroDTO.builder()
                .ingresosDelMes(ingresos)
                .totalDeudaPorCobrar(deuda)
                .balanceGeneral(balance)
                .transaccionesRecientes(recientes)
                .build();
    }

    private List<TransaccionRecienteDTO> mapRecientes(Integer condominioId) {
        List<TransaccionPago> list = (condominioId == null)
                ? transaccionPagoRepository.findTop5ByEstadoOrderByFechaPagoDesc(EstadoTransaccion.APROBADO)
                : transaccionPagoRepository.findTop5ByEstadoAndOrdenPago_Contrato_Unidad_Condominio_IdOrderByFechaPagoDesc(
                        EstadoTransaccion.APROBADO, condominioId);
        return list.stream()
                .map(t -> new TransaccionRecienteDTO(
                        t.getId(),
                        t.getFechaPago(),
                        t.getMonto(),
                        t.getReferencia(),
                        t.getMetodoPago()
                ))
                .toList();
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
