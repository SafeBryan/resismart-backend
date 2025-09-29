package com.resismart.backend.contratos.Entities;

import com.resismart.backend.condominios.Entities.Unidad;
import com.resismart.backend.residentes.Entities.Residente;
import com.resismart.backend.contratos.Enums.EstadoContrato;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "contrato")
public class Contrato {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_contrato")
    private Integer id;

    @NotNull
    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    @NotNull
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoContrato estado;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_unidad", nullable = false)
    private Unidad unidad;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_residente", nullable = false)
    private Residente residente;

    public void renovar(LocalDate nuevaFechaFin) {
        this.fechaFin = nuevaFechaFin;
        this.estado = EstadoContrato.ACTIVO;
    }

    public void rescindir() {
        this.estado = EstadoContrato.RESCINDIDO;
        this.fechaFin = LocalDate.now();
    }
}
