package com.resismart.backend.documentos.DTO;

import com.resismart.backend.documentos.Enums.TipoRelacion;
import jakarta.validation.constraints.*;
import lombok.*;

/**
 * DTO para asociar un documento ya cargado a un contrato u orden.
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class DocumentoAsociacionDTO {

    /** Id del documento a asociar. */
    @NotNull
    private Integer idDocumento;

    /** Id de contrato (alternativo a idOrden). */
    private Integer idContrato;

    /** Id de orden (alternativo a idContrato). */
    private Integer idOrden;

    /** Tipo de vínculo (ANEXO, COMPROBANTE, OTRO). */
    @NotNull
    private TipoRelacion tipoRelacion;
}
