package com.resismart.backend.eventos.Repositories;

import com.resismart.backend.eventos.Entities.Evento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventoRepository extends JpaRepository<Evento, Integer> {
    List<Evento> findByCondominio_IdOrderByFechaInicioDesc(Integer condominioId);
}

