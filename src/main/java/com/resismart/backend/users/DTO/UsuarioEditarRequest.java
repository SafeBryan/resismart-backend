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
public class UsuarioEditarRequest {
    Integer ID_Usuario;
    String nombre;
    String apellido;
    String email;
    String telefono;
    Rol rol;
    boolean estado;
}
