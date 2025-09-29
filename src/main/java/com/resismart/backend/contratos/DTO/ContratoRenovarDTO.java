package com.resismart.backend.contratos.DTO;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

@Data
public class ContratoRenovarDTO {
    @NotNull(message = "La nueva fecha de fin es obligatoria")
    private LocalDate nuevaFechaFin;
}
