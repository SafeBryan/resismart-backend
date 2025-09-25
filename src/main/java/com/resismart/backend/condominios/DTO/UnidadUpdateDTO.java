package com.resismart.backend.condominios.DTO;

import com.resismart.backend.condominios.Enums.UnidadEstado;
import lombok.Data;

@Data
public class UnidadUpdateDTO {
    private String numero;
    private UnidadEstado estado;
}