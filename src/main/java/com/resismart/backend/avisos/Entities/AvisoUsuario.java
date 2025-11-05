package com.resismart.backend.avisos.Entities;

import com.resismart.backend.users.Entities.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "aviso_usuario",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_aviso_usuario",
                columnNames = {"id_aviso", "id_usuario"}
        )
)
public class AvisoUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_aviso_usuario")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_aviso", nullable = false)
    private Aviso aviso;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    @Column(name = "entregado_en")
    private Instant entregadoEn;

    @Column(name = "leido", nullable = false)
    private boolean leido;

    @PrePersist
    public void onPersist() {
        if (creadoEn == null) {
            creadoEn = Instant.now();
        }
    }
}

