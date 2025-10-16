package com.resismart.backend.documentos.Entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.Instant;
import java.util.Map;

// ⬇️ Importante: usa el tipo JSON nativo de Hibernate 6
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "auditoria_documentos")
public class AuditoriaDocumentos {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_auditoria")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_documento", nullable = false)
    private Documento documento;

    @NotBlank
    @Column(name = "accion", length = 30, nullable = false)
    private String accion;

    @NotNull
    @Column(name = "realizado_por", nullable = false)
    private Integer realizadoPor;

    @NotNull
    @Column(name = "fecha", nullable = false)
    private Instant fecha;

    /** Detalle adicional (ip, motivo, contexto, etc.). */
    @JdbcTypeCode(SqlTypes.JSON)                  // << clave
    @Column(name = "detalle", columnDefinition = "jsonb")
    private Map<String, Object> detalle;          // << ya no String
}
