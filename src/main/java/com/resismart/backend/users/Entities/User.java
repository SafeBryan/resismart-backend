package com.resismart.backend.users.Entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.resismart.backend.users.Enums.Rol;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "users", uniqueConstraints = {@UniqueConstraint(columnNames = {"correo"})})
public class User  {

    @Id
    @GeneratedValue
    public int id_usuario;

    public String username;

    @JsonIgnore
    public String password_hash;

    public Rol rol;

    public String nombres;

    public String apellidos;

    public String telefono;

    public String correo;

    public boolean estado;
    
}
