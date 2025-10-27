package com.resismart.backend.eventos.DTO;

import com.resismart.backend.eventos.Enums.EventoEstado;
import com.resismart.backend.eventos.Enums.EventoTipo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EventoCreateDTO {
    @NotBlank private String titulo;
    @NotBlank private String descripcion;
    @NotNull private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private String lugar;
    private EventoTipo tipo;
    @NotNull private Integer idCondominio;
    @NotNull private Integer idCreador; // Usuario administrador que crea
    private EventoEstado estado; // opcional, por defecto PROGRAMADO
}

