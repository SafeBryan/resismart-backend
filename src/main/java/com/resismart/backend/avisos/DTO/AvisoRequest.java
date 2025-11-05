package com.resismart.backend.avisos.DTO;

import com.resismart.backend.avisos.Enums.AvisoDestino;
import com.resismart.backend.avisos.Enums.AvisoTipo;
import lombok.Data;

import java.util.Map;

@Data
public class AvisoRequest {
    private AvisoTipo tipo;
    private String titulo;
    private String mensaje;
    private AvisoDestino destino;
    private String destinoReferencia;
    private Map<String, Object> metadata;
}

