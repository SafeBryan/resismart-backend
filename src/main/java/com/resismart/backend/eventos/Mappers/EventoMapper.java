package com.resismart.backend.eventos.Mappers;

import com.resismart.backend.condominios.Entities.Condominio;
import com.resismart.backend.eventos.DTO.*;
import com.resismart.backend.eventos.Entities.Evento;
import com.resismart.backend.eventos.Entities.EventoParticipante;
import com.resismart.backend.eventos.Enums.EventoEstado;
import com.resismart.backend.users.Entities.Usuario;

import java.util.List;

public class EventoMapper {

    public static Evento toEntityForCreate(EventoCreateDTO dto, Condominio condominio, Usuario creador) {
        Evento e = Evento.builder()
                .titulo(dto.getTitulo())
                .descripcion(dto.getDescripcion())
                .fechaInicio(dto.getFechaInicio())
                .fechaFin(dto.getFechaFin())
                .lugar(dto.getLugar())
                .tipo(dto.getTipo())
                .condominio(condominio)
                .creadoPor(creador)
                .estado(dto.getEstado() != null ? dto.getEstado() : EventoEstado.PROGRAMADO)
                .build();
        return e;
    }

    public static EventoResumenDTO toResumen(Evento e) {
        return new EventoResumenDTO(
                e.getId(),
                e.getTitulo(),
                e.getFechaInicio(),
                e.getFechaFin(),
                e.getTipo(),
                e.getEstado(),
                e.getCondominio().getId(),
                defaultBanner(e)
        );
    }

    public static EventoDetalleDTO toDetalle(Evento e, List<EventoParticipanteDTO> participantes) {
        return new EventoDetalleDTO(
                e.getId(), e.getTitulo(), e.getDescripcion(),
                e.getFechaInicio(), e.getFechaFin(), e.getLugar(),
                e.getTipo(),
                e.getCondominio().getId(),
                e.getCreadoPor().getId_usuario(),
                e.getFechaCreacion(),
                e.getEstado(),
                participantes,
                defaultBanner(e)
        );
    }

    public static EventoParticipanteDTO toParticipanteDTO(EventoParticipante p) {
        return new EventoParticipanteDTO(
                p.getId(),
                p.getUsuario().getId_usuario(),
                p.getAsistencia(),
                p.getFechaRespuesta()
        );
    }

    private static String defaultBanner(Evento e) {
        if (e == null || e.getBannerUrl() == null || e.getBannerUrl().isBlank()) {
            return "/assets/defaults/event.png";
        }
        return e.getBannerUrl();
    }
}

