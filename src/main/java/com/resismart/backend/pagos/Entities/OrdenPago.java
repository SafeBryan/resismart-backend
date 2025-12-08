package com.resismart.backend.pagos.Entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.resismart.backend.contratos.Entities.Contrato;
import com.resismart.backend.pagos.Enums.EstadoOrdenPago;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entidad de persistencia que representa una orden de cobro periódica
 * asociada a un {@link Contrato} dentro del sistema ResiSmart.
 * <p>
 * Cada orden corresponde a un período mensual (representado por el primer día del mes),
 * mantiene fechas de emisión y vencimiento, un monto a cobrar y un {@link EstadoOrdenPago}
 * que refleja su ciclo de vida (PENDIENTE, PAGADA, VENCIDA, EN_MORA).
 * </p>
 *
 * <h2>Reglas clave</h2>
 * <ul>
 *   <li>Unicidad por combinación <b>(contrato, período)</b>.</li>
 *   <li><code>periodo</code> se interpreta como el primer día del mes facturado (YYYY-MM-01).</li>
 *   <li>La generación automática establece por defecto estado PENDIENTE y fecha de vencimiento (p. ej., día 10).</li>
 * </ul>
 *
 * @since 1.0
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(
        name = "orden_pago",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_orden_pago_contrato_periodo",
                columnNames = {"id_contrato", "periodo"}
        )
)
@SQLDelete(sql = "UPDATE orden_pago SET activo = false WHERE id_orden = ?")
@Where(clause = "activo = true")
public class OrdenPago {

    /**
     * Identificador único de la orden (clave primaria).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_orden")
    private Integer id;

    /**
     * Contrato al que pertenece la orden de pago (obligatorio).
     * <p>Relación N:1 cargada en diferido (LAZY) para optimizar consultas.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_contrato", nullable = false)
    @ToString.Exclude
    @JsonIgnoreProperties({"unidad", "residente", "hibernateLazyInitializer", "handler"})
    private Contrato contrato;

    /**
     * Período facturado representado por el primer día del mes (YYYY-MM-01).
     * <p>Participa en la restricción de unicidad junto al contrato.</p>
     */
    @Column(name = "periodo", nullable = false)
    private LocalDate periodo;

    /**
     * Fecha en la que se emite la orden de pago.
     */
    @Column(name = "fecha_emision", nullable = false)
    private LocalDate fechaEmision;

    /**
     * Fecha límite para realizar el pago (vencimiento).
     * <p>Por política puede ser el día 10 del mes facturado, configurable.</p>
     */
    @Column(name = "fecha_vencimiento", nullable = false)
    private LocalDate fechaVencimiento;

    /**
     * Monto base a cobrar por la orden de pago.
     */
    @Column(name = "monto", nullable = false)
    private BigDecimal montoBase;

    @Column(name = "monto_impuesto", precision = 12, scale = 2)
    private BigDecimal impuesto;

    @Column(name = "mora_acumulada", precision = 12, scale = 2)
    private BigDecimal moraAcumulada;

    /**
     * Estado actual de la orden de pago.
     * <p>Valores típicos: {@link EstadoOrdenPago#PENDIENTE}, {@link EstadoOrdenPago#PAGADA},
     * {@link EstadoOrdenPago#VENCIDA}, {@link EstadoOrdenPago#EN_MORA}.</p>
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoOrdenPago estado;

    @Column(name = "saldo_pendiente", nullable = false)
    private BigDecimal saldoPendiente;

    @Column(nullable = false)
    private boolean activo;

    @OneToMany(mappedBy = "ordenPago", cascade = CascadeType.ALL, orphanRemoval = false)
    @JsonIgnoreProperties({"ordenPago", "hibernateLazyInitializer", "handler"})
    private java.util.List<com.resismart.backend.pagos.Entities.TransaccionPago> transacciones;

    @PrePersist
    public void prePersist() {
        if (this.saldoPendiente == null) {
            this.saldoPendiente = this.montoBase;
        }
        if (this.moraAcumulada == null) {
            this.moraAcumulada = BigDecimal.ZERO;
        }
        this.activo = true;
    }
}
