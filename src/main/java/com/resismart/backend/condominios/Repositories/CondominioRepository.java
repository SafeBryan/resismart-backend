package com.resismart.backend.condominios.Repositories;

import com.resismart.backend.condominios.Entities.Condominio;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CondominioRepository extends JpaRepository<Condominio, Integer> {
    boolean existsByCorreoIgnoreCase(String correo);
    @EntityGraph(attributePaths = {"unidades"})
    Optional<Condominio> findWithUnidadesById(Integer id);
}