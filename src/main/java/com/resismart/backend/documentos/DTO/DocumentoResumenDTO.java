package com.resismart.backend.documentos.DTO;

import com.resismart.backend.documentos.Enums.EstadoValidacion;
import com.resismart.backend.documentos.Enums.TipoDocumento;
import lombok.*;

import java.time.Instant;

/**
 * DTO ligero para listados/paginaciones del catálogo de documentos.
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class DocumentoResumenDTO {

    /** Identificador del documento. */
    private Integer idDocumento;

    /** Tipo del documento. */
    private TipoDocumento tipo;

    /** Nombre original del archivo. */
    private String nombreOriginal;

    /** Fecha/hora de subida (UTC). */
    private Instant fechaSubida;

    /** Estado de validación (PENDIENTE, APROBADO, RECHAZADO). */
    private EstadoValidacion estadoValidacion;

    /** Tamaño del archivo en bytes. */
    private long sizeBytes;
}
