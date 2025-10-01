package com.resismart.backend.pagos.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data @AllArgsConstructor
public class GeneracionMensualResponseDTO {
    private int creadas;
    private int existentes;
}
