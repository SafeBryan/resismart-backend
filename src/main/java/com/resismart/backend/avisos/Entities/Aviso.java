package com.resismart.backend.avisos.Entities;

import com.resismart.backend.avisos.Enums.AvisoDestino;
import com.resismart.backend.avisos.Enums.AvisoTipo;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "aviso")
public class Aviso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 60)
    private AvisoTipo tipo;

    @Column(nullable = false, length = 140)
    private String titulo;

    @Column(nullable = false, columnDefinition = "text")
    private String mensaje;

    @Enumerated(EnumType.STRING)
    @Column(name = "destino", nullable = false, length = 20)
    private AvisoDestino destino;

    @Column(name = "destino_referencia", length = 120)
    private String destinoReferencia;

    @Column(name = "metadata_json", columnDefinition = "text")
    private String metadataJson;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    @PrePersist
    public void onPersist() {
        if (creadoEn == null) {
            creadoEn = Instant.now();
        }
    }
}

