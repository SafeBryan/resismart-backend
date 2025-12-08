package com.resismart.backend.condominios.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CondominioLicenciaDTO {
    private Integer condominioId;
    private Integer maxUsuarios;
    private Integer usuariosActivos;
}
