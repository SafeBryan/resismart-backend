package com.resismart.backend.contratos.Entities;

import com.resismart.backend.contratos.Enums.EstadoContrato;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ContratoEntityTest {

    @Test
    void renovarActualizaFechaFinYEstado() {
        Contrato contrato = Contrato.builder()
                .fechaInicio(LocalDate.of(2024, 1, 1))
                .fechaFin(LocalDate.of(2024, 12, 31))
                .estado(EstadoContrato.ACTIVO)
                .build();

        LocalDate nuevaFecha = LocalDate.of(2025, 12, 31);
        contrato.renovar(nuevaFecha);

        assertThat(contrato.getFechaFin()).isEqualTo(nuevaFecha);
        assertThat(contrato.getEstado()).isEqualTo(EstadoContrato.ACTIVO);
    }

    @Test
    void rescindirFijaEstadoYFechaFinEnHoy() {
        Contrato contrato = Contrato.builder()
                .fechaInicio(LocalDate.of(2024, 1, 1))
                .estado(EstadoContrato.ACTIVO)
                .build();

        LocalDate hoy = LocalDate.now();
        contrato.rescindir();

        assertThat(contrato.getEstado()).isEqualTo(EstadoContrato.RESCINDIDO);
        assertThat(contrato.getFechaFin()).isEqualTo(hoy);
    }
}
