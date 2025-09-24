package com.resismart.backend.condominios.Entities;


import com.resismart.backend.users.Entities.Usuario;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "condominio")
public class Condominio {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_condominio")
    private Integer id;

    @NotBlank
    private String nombre;

    @NotBlank
    @Column(columnDefinition = "text")
    private String direccion;

    private String telefono;

    @Email
    private String correo;

    @CreationTimestamp
    @Column(name = "creado_en", updatable = false, nullable = false)
    private Instant creadoEn;

    // Dueño (Usuario con Rol.DUEÑO)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_dueno", referencedColumnName = "id_usuario", nullable = false)
    private Usuario dueno;
/*
    @OneToMany(mappedBy = "condominio", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Unidad> unidades = new ArrayList<>();

    // Helpers
    public void addUnidad(Unidad u) {
        unidades.add(u);
        u.setCondominio(this);
    }
    public void removeUnidad(Unidad u) {
        unidades.remove(u);
        u.setCondominio(null);
    }*/
}