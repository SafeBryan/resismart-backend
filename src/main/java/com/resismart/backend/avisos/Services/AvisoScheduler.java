package com.resismart.backend.avisos.Services;

import com.resismart.backend.avisos.Enums.AvisoTipo;
import com.resismart.backend.pagos.Entities.OrdenPago;
import com.resismart.backend.pagos.Enums.EstadoOrdenPago;
import com.resismart.backend.pagos.Repositories.OrdenPagoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AvisoScheduler {

    private final OrdenPagoRepository ordenPagoRepository;
    private final AvisoService avisoService;
    private static final DateTimeFormatter FECHA_CORTA_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Ejecuta un barrido diario a las 08:00 hora del servidor y emite avisos
     * a los residentes con órdenes de pago pendientes que vencen en los próximos cinco días.
     */
    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void avisarPagosProximos() {
        LocalDate hoy = LocalDate.now();
        LocalDate objetivo = hoy.plusDays(5);

        List<OrdenPago> ordenes = ordenPagoRepository.buscarPendientesConVencimientoEntre(objetivo, objetivo);

        for (OrdenPago orden : ordenes) {
            var contrato = orden.getContrato();
            var residente = contrato.getResidente();
            if (residente == null || residente.getUsuario() == null) {
                continue;
            }
            Integer usuarioId = residente.getUsuario().getId_usuario();
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("ordenPagoId", orden.getId());
            metadata.put("fechaVencimiento", orden.getFechaVencimiento());
            metadata.put("monto", orden.getMonto());
            metadata.put("estado", orden.getEstado());
            metadata.put("diasRestantes", 5);

            String mensaje = String.format(
                    "La orden de pago #%d vence en 5 días (el %s). Monto: %s",
                    orden.getId(),
                    orden.getFechaVencimiento(),
                    orden.getMonto()
            );

            avisoService.enviarAvisoUsuario(
                    usuarioId,
                    AvisoTipo.ORDEN_PAGO_PROX_VENCER,
                    AvisoTipo.ORDEN_PAGO_PROX_VENCER.getTituloDefecto(),
                    mensaje,
                    metadata
            );
        }
    }

    private String formatearFecha(LocalDate fecha) {
        return fecha != null ? fecha.format(FECHA_CORTA_FMT) : "sin fecha";
    }
}
