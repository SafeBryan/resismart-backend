package com.resismart.backend.eventos.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class EventoConParticipantesDTO {
    private EventoResumenDTO evento;
    private List<EventoParticipanteDTO> participantes;
    private List<EventoParticipanteDTO> noAsisten;
}
