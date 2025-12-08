package com.resismart.backend.dashboard.DTO;

import com.resismart.backend.pagos.Enums.MetodoPago;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class TransaccionRecienteDTO {
    private Long id;
    private LocalDateTime fechaPago;
    private BigDecimal monto;
    private String referencia;
    private MetodoPago metodoPago;
}
