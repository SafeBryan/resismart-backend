package com.resismart.backend.residentes.DTO;


import com.resismart.backend.condominios.Entities.Unidad;
import com.resismart.backend.users.Entities.Usuario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResidenteRespuestaDTO {
    private Long id_Cliente;

    @Size(max = 10, message = "El teléfono no puede exceder 10 caracteres")
    private String telefono;


    @NotBlank(message = "La cédula es obligatoria")
    @Size(max = 10, message = "La cedula no deebe tener mas de 10 caracteres")
    private String cedula;

    private Usuario usuario;
    private Unidad unidad;


}
