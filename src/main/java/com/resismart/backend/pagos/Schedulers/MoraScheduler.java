package com.resismart.backend.pagos.Schedulers;

import com.resismart.backend.pagos.Services.MoraService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MoraScheduler {

    private final MoraService moraService;

    @Scheduled(cron = "0 * * * * *", zone = "America/Guayaquil")
    public void ejecutar() {
        try {
            moraService.aplicarMoraDiaria();
        } catch (Exception e) {
            log.error("Error al aplicar mora diaria: {}", e.getMessage(), e);
        }
    }
}
