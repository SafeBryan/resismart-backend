package com.resismart.backend.pagos.DTO;

import lombok.Data;

@Data
public class TransaccionRechazarDTO {
    private String motivo;
    private Long idAdmin;
}
