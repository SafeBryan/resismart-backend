package com.resismart.backend.contratos.DTO;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ContratoCreateDTO {
    @NotNull private Integer idUnidad;
    @NotNull private Long idResidente;
    @NotNull private LocalDate fechaInicio;
    private LocalDate fechaFin;
    @NotNull private BigDecimal monto;
    @NotNull private BigDecimal montoAlquiler;
    @NotNull private BigDecimal montoAlicuota;
}
