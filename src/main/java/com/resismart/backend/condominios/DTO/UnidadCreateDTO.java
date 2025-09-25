package com.resismart.backend.condominios.DTO;

import com.resismart.backend.condominios.Enums.UnidadEstado;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UnidadCreateDTO {
    @NotBlank private String numero;
    private UnidadEstado estado;      // opcional
    @NotNull private Integer idCondominio;
}