package com.resismart.backend.condominios.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data @AllArgsConstructor
public class CondominioResumenDTO {
    private Integer id;
    private String nombre;
    private String direccion;
    private String telefono;
    private String correo;
    private Instant creadoEn;
    private Integer idDueno;
}