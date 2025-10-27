package com.resismart.backend.eventos.DTO;

import com.resismart.backend.eventos.Enums.AsistenciaEstado;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class EventoParticipanteDTO {
    private Integer id;
    private Integer idUsuario;
    private AsistenciaEstado asistencia;
    private LocalDateTime fechaRespuesta;
}

