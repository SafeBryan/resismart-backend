package com.resismart.backend.pagos.Entities;

import com.resismart.backend.contratos.Entities.Contrato;
import com.resismart.backend.pagos.Enums.EstadoOrdenPago;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(
        name = "orden_pago",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_orden_pago_contrato_periodo",
                columnNames = {"id_contrato", "periodo"}
        )
)
public class OrdenPago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_orden")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_contrato", nullable = false)
    private Contrato contrato;

    @Column(name = "periodo", nullable = false)
    private LocalDate periodo;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDate fechaEmision;

    @Column(name = "fecha_vencimiento", nullable = false)
    private LocalDate fechaVencimiento;

    @Column(name = "monto", nullable = false)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoOrdenPago estado;
}
