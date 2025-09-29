package com.resismart.backend.contratos.DTO;

import jakarta.validation.constraints.DecimalMin;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ContratoUpdateDTO {
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private BigDecimal monto;
    private Integer idUnidad;
    private Long idResidente;
}

