package com.resismart.backend.eventos.Mappers;

import com.resismart.backend.eventos.DTO.EventoParticipanteDTO;
import com.resismart.backend.eventos.Entities.EventoParticipante;

public class EventoParticipanteMapper {
    public static EventoParticipanteDTO toDTO(EventoParticipante ep) {
        return new EventoParticipanteDTO(
                ep.getId(),
                ep.getUsuario().getId_usuario(),
                ep.getAsistencia(),
                ep.getFechaRespuesta()
        );
    }
}

