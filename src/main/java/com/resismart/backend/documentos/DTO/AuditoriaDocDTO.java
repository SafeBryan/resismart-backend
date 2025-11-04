package com.resismart.backend.documentos.DTO;

import lombok.*;
import java.time.Instant;
import java.util.Map;

/**
 * DTO de salida para auditoría de documentos.
 * Nota: Solo expone documentoId (no la entidad Documento) para evitar proxies LAZY.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditoriaDocDTO {

    /** Identificador del registro de auditoría. */
    private Integer id;

    /** Id del documento auditado (no se expone la entidad). */
    private Integer documentoId;

    /** Acción realizada (ej.: "SUBIDA", "VALIDACION_APROBADA", "VALIDACION_RECHAZADA"). */
    private String accion;

    /** Usuario responsable de la acción (username/cedula). */
    private String realizadoPor;

    /** Fecha/hora de la acción (UTC). */
    private Instant fecha;

    /** Detalle adicional en JSON (clave-valor). */
    private Map<String, Object> detalle;
}
