// com.resismart.backend.contratos.DTO.ContratoResumenDTO
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
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private BigDecimal monto;
    private BigDecimal montoAlquiler;
    private BigDecimal montoAlicuota;
    private EstadoContrato estado;
    private Integer idUnidad;
    private String numeroUnidad;
    private Long idResidente;
    private String nombreResidente;
}
