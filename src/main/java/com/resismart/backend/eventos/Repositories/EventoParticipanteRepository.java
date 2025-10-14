package com.resismart.backend.eventos.Repositories;

import com.resismart.backend.eventos.Entities.EventoParticipante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EventoParticipanteRepository extends JpaRepository<EventoParticipante, Integer> {
    List<EventoParticipante> findByEvento_Id(Integer eventoId);

    @Query("select (count(ep) > 0) from EventoParticipante ep where ep.evento.id = :eventoId and ep.usuario.id_usuario = :usuarioId")
    boolean existsByEventoIdAndUsuarioId(@Param("eventoId") Integer eventoId, @Param("usuarioId") Integer usuarioId);

    @Query("select ep from EventoParticipante ep where ep.evento.id = :eventoId and ep.usuario.id_usuario = :usuarioId")
    Optional<EventoParticipante> findByEventoIdAndUsuarioId(@Param("eventoId") Integer eventoId, @Param("usuarioId") Integer usuarioId);

    @Modifying
    @Query("delete from EventoParticipante ep where ep.evento.id = :eventoId and ep.usuario.id_usuario = :usuarioId")
    void deleteByEventoIdAndUsuarioId(@Param("eventoId") Integer eventoId, @Param("usuarioId") Integer usuarioId);
}
