package com.resismart.backend.contratos.Entities;

import com.resismart.backend.condominios.Entities.Unidad;
import com.resismart.backend.contratos.Enums.TipoOcupante;
import com.resismart.backend.residentes.Entities.Residente;
import com.resismart.backend.contratos.Enums.EstadoContrato;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entidad de persistencia que representa un contrato de arrendamiento dentro del sistema ResiSmart.
 * <p>
 * Un {@code Contrato} vincula una {@link Unidad} con un {@link Residente} durante un período de tiempo,
 * define el {@code monto} a cobrar y mantiene un {@link EstadoContrato} que indica su vigencia
 * ({@code ACTIVO}, {@code RESCINDIDO}, {@code FINALIZADO}, etc.).
 * </p>
 *
 * <h2>Reglas de negocio principales</h2>
 * <ul>
 *   <li>Un contrato debe estar asociado a una unidad y a un residente (no nulos).</li>
 *   <li>Al crear un contrato válido, la unidad suele pasar a estado OCUPADA (gestión en el servicio).</li>
 *   <li>La renovación extiende la fecha de fin y mantiene el estado en ACTIVO.</li>
 *   <li>La rescisión marca el estado como RESCINDIDO y fija la fecha de fin al día actual.</li>
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
@Table(name = "contrato")
@SQLDelete(sql = "UPDATE contrato SET activo = false WHERE id_contrato = ?")
@Where(clause = "activo = true")
public class Contrato {

    /**
     * Identificador único del contrato (clave primaria).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_contrato")
    private Integer id;

    /**
     * Fecha de inicio de vigencia del contrato (obligatoria).
     */
    @NotNull
    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    /**
     * Fecha de finalización de vigencia del contrato (opcional).
     * <p>Puede ser nula cuando el contrato aún no tiene fecha de término definida.</p>
     */
    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    /**
     * Monto mensual pactado en el contrato.
     * <p>Se almacena con precisión 2 decimales.</p>
     */
    @NotNull
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @NotNull
    @Column(name = "monto_alquiler", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoAlquiler;

    @NotNull
    @Column(name = "monto_alicuota", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoAlicuota;

    @Column(name = "porcentaje_impuesto", precision = 5, scale = 2)
    private BigDecimal porcentajeImpuesto;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_ocupante", length = 20)
    private TipoOcupante tipoOcupante;

    /**
     * Estado actual del contrato.
     * <p>Ejemplos: {@link EstadoContrato#ACTIVO}, {@link EstadoContrato#RESCINDIDO}, {@link EstadoContrato#FINALIZADO}.</p>
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoContrato estado;

    /**
     * Unidad habitacional asociada al contrato (obligatoria).
     * <p>Relación N:1. Se carga en diferido (LAZY) para optimizar consultas.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_unidad", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Unidad unidad;

    /**
     * Residente asociado al contrato (obligatorio).
     * <p>Relación N:1. Se carga en diferido (LAZY) para optimizar consultas.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_residente", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Residente residente;

    @Column(nullable = false)
    private boolean activo;

    @PrePersist
    public void prePersist() {
        this.activo = true;
    }

    /**
     * Renueva el contrato extendiendo su fecha de fin y asegurando el estado {@link EstadoContrato#ACTIVO}.
     *
     * @param nuevaFechaFin nueva fecha de finalización (debe ser posterior a la fecha actual
     *                      y coherente con la lógica validada en el servicio).
     * @implNote Las validaciones de negocio (p. ej., que el contrato esté ACTIVO, que la nueva fecha
     *           sea futura, etc.) se realizan en la capa de servicio antes de invocar este método.
     */
    public void renovar(LocalDate nuevaFechaFin) {
        this.fechaFin = nuevaFechaFin;
        this.estado = EstadoContrato.ACTIVO;
    }

    /**
     * Rescinde el contrato marcándolo como {@link EstadoContrato#RESCINDIDO} y fijando {@code fechaFin} a hoy.
     *
     * @implNote La lógica adicional asociada (p. ej., liberar la unidad o generar asientos) se gestiona
     *           en la capa de servicio para mantener esta entidad como un modelo anémico coherente.
     */
    public void rescindir() {
        this.estado = EstadoContrato.RESCINDIDO;
        this.fechaFin = LocalDate.now();
    }
}
