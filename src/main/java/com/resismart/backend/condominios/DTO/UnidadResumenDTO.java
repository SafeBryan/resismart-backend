package com.resismart.backend.condominios.DTO;

import com.resismart.backend.condominios.Enums.UnidadEstado;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data @AllArgsConstructor
public class UnidadResumenDTO {
    private Integer id;
    private String numero;
    private UnidadEstado estado;
}