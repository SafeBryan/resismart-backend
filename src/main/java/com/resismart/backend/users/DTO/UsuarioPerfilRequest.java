package com.resismart.backend.users.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload para que un usuario autenticado actualice su propio perfil.
 * Todos los campos son opcionales; solo se actualizan los que se envían.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UsuarioPerfilRequest {
    private String nombre;
    private String apellido;
    private String email;
    private String telefono;
}

