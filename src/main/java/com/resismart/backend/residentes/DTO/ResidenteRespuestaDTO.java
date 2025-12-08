package com.resismart.backend.residentes.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResidenteRespuestaDTO {
    private Long id;
    private String cedula;
    private String telefono;
    private Integer usuarioId;
    private String usuarioNombre;
    private String usuarioApellido;
    private String usuarioEmail;
    private String usuarioRol;
    private Boolean usuarioEstado;
    private Boolean usuarioActivo;
    private Long condominioId;
    private String condominioNombre;
}
