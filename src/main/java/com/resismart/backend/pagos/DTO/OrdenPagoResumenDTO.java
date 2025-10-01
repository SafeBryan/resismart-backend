package com.resismart.backend.pagos.DTO;

import com.resismart.backend.pagos.Enums.EstadoOrdenPago;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data @AllArgsConstructor
public class OrdenPagoResumenDTO {
    private Integer id;
    private Integer idContrato;
    private LocalDate periodo;
    private BigDecimal monto;
    private EstadoOrdenPago estado;
    private LocalDate fechaEmision;
    private LocalDate fechaVencimiento;
}
