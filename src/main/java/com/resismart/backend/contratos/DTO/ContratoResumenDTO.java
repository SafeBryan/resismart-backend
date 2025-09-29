package com.resismart.backend.contratos.DTO;

import com.resismart.backend.contratos.Enums.EstadoContrato;
import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
public class ContratoResumenDTO {
    private Integer id;
    private Integer idUnidad;
    private Long idResidente;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private BigDecimal monto;
    private EstadoContrato estado;
}

