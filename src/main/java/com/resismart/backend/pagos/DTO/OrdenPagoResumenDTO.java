package com.resismart.backend.pagos.DTO;

import com.resismart.backend.pagos.Enums.EstadoOrdenPago;
import com.resismart.backend.pagos.Enums.EstadoTransaccion;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @AllArgsConstructor
public class OrdenPagoResumenDTO {
    private Integer id;
    private Integer idContrato;
    private LocalDate periodo;
    private BigDecimal montoBase;
    private BigDecimal moraAcumulada;
    private BigDecimal saldoPendiente;
    private EstadoOrdenPago estado;
    private LocalDate fechaEmision;
    private LocalDate fechaVencimiento;
    private EstadoTransaccion ultimaTransaccionEstado;
    private Long ultimaTransaccionId;
    private LocalDateTime ultimaTransaccionFecha;
    private BigDecimal ultimaTransaccionMonto;
}
