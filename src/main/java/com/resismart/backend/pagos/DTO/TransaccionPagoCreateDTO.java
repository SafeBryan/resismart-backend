package com.resismart.backend.pagos.DTO;

import com.resismart.backend.pagos.Enums.MetodoPago;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

@Data
public class TransaccionPagoCreateDTO {
    @NotNull
    private int ordenId;
    @NotNull
    private BigDecimal monto;
    private String referencia;
    @NotNull
    private MetodoPago metodoPago;
    private MultipartFile comprobante;
}
