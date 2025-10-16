package com.resismart.backend.documentos.DTO;

import com.resismart.backend.documentos.Enums.EstadoValidacion;
import jakarta.validation.constraints.*;
import lombok.*;

/**
 * DTO para registrar una acción de validación sobre un documento.
 * La transición de estado válida es controlada en el servicio.
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class DocumentoValidacionDTO {

    /** Identificador del documento a validar. */
    @NotNull
    private Integer idDocumento;

    /** Nuevo estado a aplicar (APROBADO o RECHAZADO). */
    @NotNull
    private EstadoValidacion nuevoEstado;

    /** Observación del validador (obligatoria si RECHAZADO). */
    @Size(max = 500)
    private String observacion;

    /** Usuario que realiza la validación. */
    @NotBlank
    @Size(max = 100)
    private String validadoPor;
}
