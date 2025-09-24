package com.resismart.backend.condominios.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data @AllArgsConstructor
public class CondominioDetalleDTO {
    private Integer id;
    private String nombre;
    private String direccion;
    private String telefono;
    private String correo;
    private Instant creadoEn;
    private Integer idDueno;
    //private List<UnidadResumenDTO> unidades;
}