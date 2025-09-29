package com.resismart.backend.residentes.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class ResidenteDTO {
    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    private String apellido;


    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email debe ser válido")
    private String email;

    @Size(max = 10, message = "El teléfono no puede exceder 10 caracteres")
    private String telefono;

    @NotBlank(message = "La cédula es obligatoria")
    @Size(max = 10, message = "La cedula no deebe tener mas de 10 caracteres")
    private String cedula;


    private int idUnidad;
}
