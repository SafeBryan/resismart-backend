package com.resismart.backend.users.Entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.resismart.backend.users.Enums.Rol;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "usuarios", uniqueConstraints = {@UniqueConstraint(columnNames = {"correo"})})
@SQLDelete(sql = "UPDATE usuarios SET activo = false WHERE id_usuario = ?")
@Where(clause = "activo = true")
public class Usuario implements UserDetails {

    @Id
    @GeneratedValue
    private int id_usuario;

    private String username;

    @JsonIgnore
    private String password_hash;

    private Rol rol;

    private String nombres;

    private String apellidos;

    private String telefono;

    private String correo;

    private boolean estado;

    @Column(nullable = false)
    private boolean activo;

    private String avatarUrl;

    public String getAvatarUrl() {
        if (avatarUrl == null || avatarUrl.isBlank()) {
            return "/files/defaults/default-avatar.png";
        }
        if (avatarUrl.startsWith("/files/")) {
            return avatarUrl;
        }
        return "/files/" + avatarUrl;
    }

    @PrePersist
    public void prePersist() {
        this.activo = true;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(rol.name()));
    }

    @Override
    public String getPassword() {
        return this.password_hash;
    }

    @Override
    public String getUsername() {
        return this.correo;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return this.estado && this.activo;
    }

}
