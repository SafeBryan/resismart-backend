package com.resismart.backend.eventos.DTO;

import com.resismart.backend.eventos.Enums.AsistenciaEstado;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EventoAsistenciaRequest {
    @NotNull private AsistenciaEstado estado;
}

