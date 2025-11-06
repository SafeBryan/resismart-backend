package com.resismart.backend.avisos.DTO;

import com.resismart.backend.avisos.Enums.AvisoDestino;
import com.resismart.backend.avisos.Enums.AvisoTipo;
import lombok.*;

import java.time.Instant;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvisoPayload {
    private Long id;
    private AvisoTipo tipo;
    private String titulo;
    private String mensaje;
    private AvisoDestino destino;
    private String destinoReferencia;
    private Instant emitidoEn;
    private Map<String, Object> metadata;
}

