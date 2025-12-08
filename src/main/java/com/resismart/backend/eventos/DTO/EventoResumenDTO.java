package com.resismart.backend.eventos.DTO;

import com.resismart.backend.eventos.Enums.EventoEstado;
import com.resismart.backend.eventos.Enums.EventoTipo;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class EventoResumenDTO {
    private Integer id;
    private String titulo;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private EventoTipo tipo;
    private EventoEstado estado;
    private Integer idCondominio;
    private String bannerUrl;
}

