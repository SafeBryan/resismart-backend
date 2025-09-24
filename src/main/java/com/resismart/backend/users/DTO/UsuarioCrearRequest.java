package com.resismart.backend.users.DTO;

import com.resismart.backend.users.Enums.Rol;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UsuarioCrearRequest {
    String email;
    String password;
    String nombre;
    String apellido;
    Rol rol;
}
