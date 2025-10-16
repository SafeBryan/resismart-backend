package com.resismart.backend.documentos.DTO;

import com.resismart.backend.documentos.Enums.EstadoValidacion;
import com.resismart.backend.documentos.Enums.TipoDocumento;
import lombok.*;

import java.time.Instant;

/**
 * DTO de detalle completo para ver un documento específico.
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class DocumentoDetalleDTO {

    /** Identificador del documento. */
    private Integer idDocumento;

    /** Tipo del documento (funcional). */
    private TipoDocumento tipo;

    /** Nombre original del archivo. */
    private String nombreOriginal;

    /** Clave/ruta interna en el storage (no pública). */
    private String storageKey;

    /** Fecha/hora de subida (UTC). */
    private Instant fechaSubida;

    /** Usuario que subió el archivo. */
    private String subidoPor;

    /** Estado de validación actual. */
    private EstadoValidacion estadoValidacion;

    /** Usuario que realizó la validación (si aplica). */
    private String validadoPor;

    /** Tipo MIME del archivo. */
    private String mimeType;

    /** Tamaño del archivo en bytes. */
    private long sizeBytes;

    /** Hash SHA-256 del archivo. */
    private String sha256;
}
