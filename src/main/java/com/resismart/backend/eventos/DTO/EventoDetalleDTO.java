package com.resismart.backend.eventos.DTO;

import com.resismart.backend.eventos.Enums.EventoEstado;
import com.resismart.backend.eventos.Enums.EventoTipo;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class EventoDetalleDTO {
    private Integer id;
    private String titulo;
    private String descripcion;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private String lugar;
    private EventoTipo tipo;
    private Integer idCondominio;
    private Integer idCreador;
    private Instant fechaCreacion;
    private EventoEstado estado;
    private List<EventoParticipanteDTO> participantes;
}

