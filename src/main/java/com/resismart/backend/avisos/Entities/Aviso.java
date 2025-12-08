package com.resismart.backend.avisos.Entities;

import com.resismart.backend.avisos.Enums.AvisoDestino;
import com.resismart.backend.avisos.Enums.AvisoTipo;
import com.resismart.backend.users.Entities.Usuario;
import com.resismart.backend.util.JsonMetadataConverter;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "aviso")
@SQLDelete(sql = "UPDATE aviso SET activo = false WHERE id = ?")
@Where(clause = "activo = true")
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

    @Convert(converter = JsonMetadataConverter.class)
    @Column(name = "metadata_json", columnDefinition = "text")
    private Map<String, Object> metadata;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private Usuario sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id")
    private Usuario receiver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Aviso parent;

    @Column(nullable = false)
    private boolean leido;

    @Column(nullable = false)
    private boolean activo;

    @PrePersist
    public void onPersist() {
        if (creadoEn == null) {
            creadoEn = LocalDateTime.now();
        }
        this.leido = false;
        this.activo = true;
    }
}

