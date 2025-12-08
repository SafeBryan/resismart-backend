package com.resismart.backend.residentes.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload para que un residente autenticado actualice su propio perfil.
 * Todos los campos son opcionales; solo se aplican los que se envían.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResidentePerfilRequest {
    private String telefono;
    private String cedula;
}

