package com.resismart.backend.pagos.DTO;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrdenPagoPagarDTO {
    @NotNull
    private Boolean pagada;
}
