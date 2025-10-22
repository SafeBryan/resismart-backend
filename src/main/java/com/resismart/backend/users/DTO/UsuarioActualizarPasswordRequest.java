package com.resismart.backend.users.DTO;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UsuarioActualizarPasswordRequest {
    @NotBlank(message = "La contraseña es obligatoria")
    private String password;
}

