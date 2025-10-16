package com.resismart.backend.documentos.Entities;

import com.resismart.backend.documentos.Enums.EstadoValidacion;
import com.resismart.backend.documentos.Enums.TipoDocumento;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;

/**
 * Entidad de persistencia que representa un archivo documental dentro del sistema ResiSmart.
 * <p>
 * Un {@code Documento} puede corresponder a distintos tipos, como contratos,
 * comprobantes de pago u otros anexos administrativos. Se almacena información
 * sobre su ubicación física (o clave de almacenamiento), su estado de validación
 * y quién realizó las acciones principales (subida o validación).
 * </p>
 *
 * <h2>Reglas principales</h2>
 * <ul>
 *   <li>Todo documento tiene un tipo funcional (ej. CONTRATO, COMPROBANTE).</li>
 *   <li>Se guarda el nombre original y la clave de almacenamiento físico (storageKey).</li>
 *   <li>El estado de validación permite flujos de aprobación.</li>
 *   <li>La trazabilidad se conserva con campos de usuario y fecha de subida.</li>
 * </ul>
 *
 * @author
 *   Equipo ResiSmart
 * @since 1.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "documento")
public class Documento {

    /** Identificador único del documento. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_documento")
    private Integer id;

    /** Tipo lógico del documento (CONTRATO, COMPROBANTE, OTRO). */
    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private TipoDocumento tipo;

    /** Nombre original del archivo cargado por el usuario. */
    @Column(name = "nombre_original", nullable = false)
    private String nombreOriginal;

    /** Clave o ruta donde se almacena el archivo (local o remoto). */
    @Column(name = "storage_key", nullable = false)
    private String storageKey;

    /** Fecha y hora de subida del archivo. */
    @NotNull
    @Column(name = "fecha_subida", nullable = false)
    private Instant fechaSubida;

    /** Identificador del usuario que subió el documento. */
    @Column(name = "subido_por")
    private Integer subidoPor;

    /** Estado de validación del documento (PENDIENTE, APROBADO, RECHAZADO). */
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_validacion", length = 20)
    private EstadoValidacion estadoValidacion;

    /** Identificador del usuario que validó el documento (si aplica). */
    @Column(name = "validado_por")
    private Integer validadoPor;

    /** Tipo MIME del archivo (ej. application/pdf). */
    @Column(name = "mime_type", length = 50)
    private String mimeType;

    /** Tamaño del archivo en bytes. */
    @Column(name = "size_bytes")
    private Long sizeBytes;

    /** Hash SHA-256 opcional para detectar duplicados exactos. */
    @Column(name = "sha256", length = 64)
    private String sha256;
}
