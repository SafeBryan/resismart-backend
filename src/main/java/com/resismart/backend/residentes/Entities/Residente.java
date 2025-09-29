package com.resismart.backend.residentes.Entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.resismart.backend.condominios.Entities.Unidad;
import com.resismart.backend.users.Entities.Usuario;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Table(name = "Residentes")
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Residente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false,length = 10,unique = true)
    private String cedula;

    @Column(nullable = false,length = 10)
    private String telefono;

    @ManyToOne
    @JoinColumn(name = "ID_Usuario", nullable = false)
    private Usuario usuario;

    @ManyToOne
    @JoinColumn(name="ID_Unidad", nullable = false)
    private Unidad unidad;

}
