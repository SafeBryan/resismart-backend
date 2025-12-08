package com.resismart.backend.eventos.Entities;

import com.resismart.backend.condominios.Entities.Condominio;
import com.resismart.backend.eventos.Enums.EventoEstado;
import com.resismart.backend.eventos.Enums.EventoTipo;
import com.resismart.backend.users.Entities.Usuario;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "evento")
@SQLDelete(sql = "UPDATE evento SET activo = false WHERE id_evento = ?")
@Where(clause = "activo = true")
public class Evento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_evento")
    private Integer id;

    @NotBlank
    private String titulo;

    @NotBlank
    @Column(columnDefinition = "text")
    private String descripcion;

    @NotNull
    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    private String lugar;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", length = 20)
    private EventoTipo tipo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_condominio", referencedColumnName = "id_condominio", nullable = false)
    private Condominio condominio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creado_por", referencedColumnName = "id_usuario", nullable = false)
    private Usuario creadoPor;

    @CreationTimestamp
    @Column(name = "fecha_creacion", updatable = false)
    private Instant fechaCreacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 20)
    private EventoEstado estado;

    @OneToMany(mappedBy = "evento", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EventoParticipante> participantes = new ArrayList<>();

    @Column(nullable = false)
    private boolean activo;

    private String bannerUrl;

    public String getBannerUrl() {
        return (bannerUrl == null || bannerUrl.isBlank()) ? "/assets/defaults/event.png" : bannerUrl;
    }

    @PrePersist
    public void prePersist() {
        this.activo = true;
    }

    // Helpers
    public void addParticipante(EventoParticipante p) {
        participantes.add(p);
        p.setEvento(this);
    }
    public void removeParticipante(EventoParticipante p) {
        participantes.remove(p);
        p.setEvento(null);
    }
}

