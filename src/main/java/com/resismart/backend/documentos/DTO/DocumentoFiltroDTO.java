package com.resismart.backend.documentos.DTO;

import com.resismart.backend.documentos.Enums.EstadoValidacion;
import com.resismart.backend.documentos.Enums.TipoRelacion;
import com.resismart.backend.documentos.Enums.TipoDocumento;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.Instant;

/**
 * DTO para filtrar/buscar documentos con soporte a asociación (contrato/orden),
 * rango de fechas, paginación y ordenamiento.
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class DocumentoFiltroDTO {

    /** Tipo de documento (CONTRATO, COMPROBANTE, OTRO). */
    private TipoDocumento tipo;

    /** Estado de validación a filtrar. */
    private EstadoValidacion estadoValidacion;

    /** Fecha inicial de subida (inclusive, UTC). */
    private Instant fechaDesde;

    /** Fecha final de subida (inclusive, UTC). */
    private Instant fechaHasta;

    /** Usuario que subió el archivo. */
    @Size(max = 100)
    private String subidoPor;

    /** Filtro por asociación a contrato (opcional). */
    private Integer idContrato;

    /** Filtro por asociación a orden (opcional). */
    private Integer idOrden;

    /** Tipo de relación en el vínculo (ANEXO, COMPROBANTE, OTRO). */
    private TipoRelacion tipoRelacion;

    // -------- Paginación & orden --------

    /** Número de página (0-based). */
    @Min(0)
    @Builder.Default
    private Integer page = 0;

    /** Tamaño de página. */
    @Min(1) @Max(200)
    @Builder.Default
    private Integer size = 20;

    /** Campo de ordenamiento (por ejemplo: "fechaSubida","idDocumento"). */
    @Builder.Default
    private String sortBy = "fechaSubida";

    /** Dirección de orden ("ASC" o "DESC"). */
    @Builder.Default
    private String sortDir = "DESC";
}
