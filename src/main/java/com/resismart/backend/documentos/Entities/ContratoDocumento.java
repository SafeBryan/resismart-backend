package com.resismart.backend.documentos.Entities;

import com.resismart.backend.contratos.Entities.Contrato;
import com.resismart.backend.documentos.Enums.TipoRelacion;
import jakarta.persistence.*;
import lombok.*;

/**
 * Relación entre un {@link Documento} y un {@link Contrato}.
 * Permite adjuntar uno o varios documentos a un contrato.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "contrato_documento",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_contrato_documento", columnNames = {"id_contrato", "id_documento"})
        }
)
public class ContratoDocumento {

    /** Identificador único de la relación. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_contrato_documento")
    private Integer id;

    /** Contrato al que se asocia el documento. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_contrato", nullable = false)
    private Contrato contrato;

    /** Documento asociado. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_documento", nullable = false)
    private Documento documento;

    /** Tipo de relación (ANEXO, OTRO, etc.). */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_relacion", length = 30)
    private TipoRelacion tipoRelacion;
}
