package com.resismart.backend.pagos.Entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.resismart.backend.pagos.Enums.EstadoTransaccion;
import com.resismart.backend.pagos.Enums.MetodoPago;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "transaccion_pago")
@SQLDelete(sql = "UPDATE transaccion_pago SET activo = false WHERE id = ?")
@Where(clause = "activo = true")
public class TransaccionPago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(name = "fecha_pago", nullable = false)
    private LocalDateTime fechaPago;

    @Column(length = 120)
    private String referencia;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pago", length = 20, nullable = false)
    private MetodoPago metodoPago;

    @Column(name = "comprobante_storage_key")
    private String comprobanteStorageKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 20, nullable = false)
    private EstadoTransaccion estado;

    @Column(name = "aprobado_por_admin_id")
    private Integer aprobadoPorAdminId;

    @Column(name = "rechazado_por_admin_id")
    private Integer rechazadoPorAdminId;

    @Column(name = "motivo_rechazo", length = 255)
    private String motivoRechazo;

    @Column(name = "fecha_aprobacion")
    private LocalDateTime fechaAprobacion;

    @Column(name = "fecha_rechazo")
    private LocalDateTime fechaRechazo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_orden_pago", nullable = false)
    @ToString.Exclude
    @JsonIgnoreProperties({"contrato", "transacciones", "hibernateLazyInitializer", "handler"})
    private OrdenPago ordenPago;

    @Column(nullable = false)
    private boolean activo;

    @PrePersist
    public void prePersist() {
        this.activo = true;
        if (this.fechaPago == null) {
            this.fechaPago = LocalDateTime.now(ZoneId.systemDefault());
        }
    }
}
