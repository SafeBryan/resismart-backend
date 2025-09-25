package com.resismart.backend.condominios.Entities;

import com.resismart.backend.condominios.Enums.UnidadEstado;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "unidad", uniqueConstraints = {
        @UniqueConstraint(name = "uk_unidad_numero_condominio", columnNames = {"numero","id_condominio"})
})
public class Unidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_unidad")
    private Integer id;

    @NotBlank
    private String numero;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UnidadEstado estado;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_condominio", nullable = false)
    private Condominio condominio;

    @PrePersist
    public void prePersist() {
        if (estado == null) estado = UnidadEstado.LIBRE; // valor por defecto
    }
}