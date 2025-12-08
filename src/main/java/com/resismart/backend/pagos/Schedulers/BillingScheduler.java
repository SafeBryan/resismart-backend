package com.resismart.backend.pagos.Schedulers;

import com.resismart.backend.pagos.Entities.OrdenPago;
import com.resismart.backend.pagos.Services.FacturacionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class BillingScheduler {

    private final FacturacionService facturacionService;
    @Value("${app.facturacion.dia-ejecucion:1}")
    private int diaEjecucion;

    @PostConstruct
    void logConfig() {
        log.info("[BillingScheduler] Configuracion cargada: dia-ejecucion={}, cron=0 0 3 * * ? (America/Guayaquil)", diaEjecucion);
    }

    /**
     * Ejecuta diariamente a las 03:00 (zona Guayaquil) pero solo genera las
     * ordenes cuando coincide el dia configurado (por defecto, 1 de mes).
     */
    @Scheduled(cron = "0 * * * * ?", zone = "America/Guayaquil")
    @Transactional
    public void ejecutarGeneracionMensualOrdenes() {
        LocalDate hoy = LocalDate.now(ZoneId.of("America/Guayaquil"));
        /*if (hoy.getDayOfMonth() != Math.max(1, Math.min(diaEjecucion, hoy.lengthOfMonth()))) {
            log.info("Scheduler mensual: se omite ejecucion, hoy={} no es el dia configurado ({})", hoy, diaEjecucion);
            return;
        }
        */
        YearMonth periodo = YearMonth.from(hoy);
        log.info("Inicio scheduler mensual de ordenes de pago para {}", periodo);
        try {
            var resultado = facturacionService.generarOrdenesParaMes(periodo.getYear(), periodo.getMonthValue());
            List<OrdenPago> creadas = resultado.creadas() != null ? resultado.creadas() : List.of();
            log.info("Scheduler mensual genero {} orden(es); {} ya existian.", creadas.size(), resultado.existentes());

            facturacionService.notificarOrdenesGeneradas(creadas);
            log.info("Fin scheduler mensual de ordenes de pago para {}", periodo);
        } catch (Exception e) {
            log.error("Error en scheduler mensual de ordenes de pago para {}: {}", periodo, e.getMessage(), e);
        }
    }
}
