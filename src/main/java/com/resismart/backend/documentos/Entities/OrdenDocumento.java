package com.resismart.backend.documentos.Entities;

import com.resismart.backend.pagos.Entities.OrdenPago;
import com.resismart.backend.documentos.Enums.TipoRelacion;
import jakarta.persistence.*;
import lombok.*;

/**
 * Relación entre un {@link Documento} y una {@link OrdenPago}.
 * Permite asociar comprobantes u otros documentos de respaldo
 * a una orden de pago dentro del sistema ResiSmart.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "orden_documento",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_orden_documento", columnNames = {"id_orden", "id_documento"})
        }
)
public class OrdenDocumento {

    /** Identificador único de la relación. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_orden_documento")
    private Integer id;

    /** Orden de pago asociada al documento. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_orden", nullable = false)
    private OrdenPago orden;

    /** Documento asociado (por ejemplo, comprobante). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_documento", nullable = false)
    private Documento documento;

    /** Tipo de relación entre la orden y el documento. */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_relacion", length = 30)
    private TipoRelacion tipoRelacion;
}
