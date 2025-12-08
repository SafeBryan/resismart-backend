package com.resismart.backend.residentes.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class ResidenteDTO {
    // Si se envía usuarioId se usará el usuario existente; de lo contrario se creará uno nuevo con los datos abajo.
    private Long usuarioId;

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    private String apellido;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email no tiene un formato válido")
    private String email;

    @NotBlank(message = "La cédula es obligatoria")
    private String cedula;

    private String telefono;

    @NotNull(message = "El condominio es obligatorio")
    private Long condominioId;
}
