package com.resismart.backend.condominios.Entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.resismart.backend.condominios.Enums.UnidadEstado;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "unidad", uniqueConstraints = {
        @UniqueConstraint(name = "uk_unidad_numero_condominio", columnNames = {"numero","id_condominio"})
})
@SQLDelete(sql = "UPDATE unidad SET activo = false WHERE id_unidad = ?")
@Where(clause = "activo = true")
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
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonIgnoreProperties(value = {"unidades", "dueno", "hibernateLazyInitializer", "handler"}, allowSetters = true)
    private Condominio condominio;

    @Column(nullable = false)
    private boolean activo;

    @PrePersist
    public void prePersist() {
        if (estado == null) estado = UnidadEstado.LIBRE; // valor por defecto
        this.activo = true;
    }
}
