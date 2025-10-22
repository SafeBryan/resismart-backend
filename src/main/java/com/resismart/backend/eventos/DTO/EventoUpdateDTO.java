package com.resismart.backend.eventos.DTO;

import com.resismart.backend.eventos.Enums.EventoEstado;
import com.resismart.backend.eventos.Enums.EventoTipo;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EventoUpdateDTO {
    private String titulo;
    private String descripcion;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private String lugar;
    private EventoTipo tipo;
    private EventoEstado estado;
}

