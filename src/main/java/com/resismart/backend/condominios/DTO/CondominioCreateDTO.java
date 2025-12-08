package com.resismart.backend.condominios.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CondominioCreateDTO {
    @NotBlank private String nombre;
    @NotBlank
    private String direccion;
    private String telefono;
    @Email
    private String correo;
    private Integer idDueno;
    private Integer maxUsuarios;
}
