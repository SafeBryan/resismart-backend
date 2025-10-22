package com.resismart.backend.eventos.Entities;

import com.resismart.backend.eventos.Enums.AsistenciaEstado;
import com.resismart.backend.users.Entities.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "evento_participante",
       uniqueConstraints = @UniqueConstraint(name = "uk_evento_usuario", columnNames = {"id_evento", "id_usuario"}))
public class EventoParticipante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_evento_participante")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_evento", referencedColumnName = "id_evento", nullable = false)
    private Evento evento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", referencedColumnName = "id_usuario", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "asistencia", length = 20)
    private AsistenciaEstado asistencia;

    @Column(name = "fecha_respuesta")
    private LocalDateTime fechaRespuesta;

    @PrePersist
    public void prePersist() {
        if (asistencia == null) asistencia = AsistenciaEstado.PENDIENTE;
    }
}

