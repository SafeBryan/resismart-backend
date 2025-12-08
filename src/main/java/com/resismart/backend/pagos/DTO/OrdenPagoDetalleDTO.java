package com.resismart.backend.pagos.DTO;

import com.resismart.backend.pagos.Entities.TransaccionPago;
import com.resismart.backend.pagos.Enums.EstadoOrdenPago;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class OrdenPagoDetalleDTO {
    private Integer id;
    private EstadoOrdenPago estado;
    private LocalDate periodo;
    private LocalDate fechaEmision;
    private LocalDate fechaVencimiento;
    private BigDecimal montoBase;
    private BigDecimal impuesto;
    private BigDecimal mora;
    private BigDecimal saldoPendiente;
    private BigDecimal total;

    private Integer contratoId;
    private String contratoCodigo;
    private String inquilinoNombre;
    private String inquilinoCorreo;
    private String unidad;
    private String condominio;

    private List<TransaccionPago> transacciones;
}
