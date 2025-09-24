package com.resismart.backend.users.DTO;


import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UsuarioClienteCredencialesDTO {
    @NotNull(message = "El id es obligatorio")
    private Integer idUsuario;

    @NotNull(message = "El id cliente es obligatorio")
    private Long idCliente;

    String email;

    String password;


}
