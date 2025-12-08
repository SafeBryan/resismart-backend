package com.resismart.backend.residentes.Entities;

import com.resismart.backend.condominios.Entities.Condominio;
import com.resismart.backend.users.Entities.Usuario;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

@Data
@Table(name = "Residentes")
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE Residentes SET activo = false WHERE id = ?")
@Where(clause = "activo = true")
public class Residente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20, unique = true)
    private String cedula;

    @Column(nullable = false, length = 20)
    private String telefono;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", referencedColumnName = "id_usuario", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_condominio")
    private Condominio condominio;

    @Column(nullable = false)
    private boolean activo;

    private String avatarUrl;

    public String getAvatarUrl() {
        return (avatarUrl == null || avatarUrl.isBlank()) ? "/assets/defaults/user.png" : avatarUrl;
    }

    @PrePersist
    public void prePersist() {
        this.activo = true;
    }
}
