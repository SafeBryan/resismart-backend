package com.resismart.backend.eventos.Services;

import com.resismart.backend.Common.MensajeError;
import com.resismart.backend.avisos.Enums.AvisoTipo;
import com.resismart.backend.avisos.Services.AvisoService;
import com.resismart.backend.condominios.Entities.Condominio;
import com.resismart.backend.condominios.Repositories.CondominioRepository;
import com.resismart.backend.eventos.DTO.*;
import com.resismart.backend.eventos.Entities.Evento;
import com.resismart.backend.eventos.Entities.EventoParticipante;
import com.resismart.backend.eventos.Enums.AsistenciaEstado;
import com.resismart.backend.eventos.Mappers.EventoMapper;
import com.resismart.backend.eventos.Repositories.EventoParticipanteRepository;
import com.resismart.backend.eventos.Repositories.EventoRepository;
import com.resismart.backend.users.Entities.Usuario;
import com.resismart.backend.users.Repositories.UsuarioRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EventoService {

    @Autowired private EventoRepository eventoRepo;
    @Autowired private EventoParticipanteRepository participanteRepo;
    @Autowired private CondominioRepository condominioRepo;
    @Autowired private UsuarioRepository usuarioRepo;
    @Autowired private AvisoService avisoService;

    /* ===== Helpers ===== */
    private Evento getEventoOrThrow(Integer id) {
        return eventoRepo.findById(id)
                .orElseThrow(() -> new java.util.NoSuchElementException(MensajeError.EVENTO_NO_ENCONTRADO.getMensaje()));
    }
    private Condominio getCondominioOrThrow(Integer id) {
        return condominioRepo.findById(id)
                .orElseThrow(() -> new java.util.NoSuchElementException(MensajeError.CONDOMINIO_NO_ENCONTRADO.getMensaje()));
    }
    private Usuario getUsuarioOrThrow(Integer id) {
        return usuarioRepo.findById(id)
                .orElseThrow(() -> new java.util.NoSuchElementException(MensajeError.USUARIO_NO_ENCONTRADO.getMensaje()));
    }

    private Usuario getUsuarioOrThrowByCorreo(String correo) {
        return usuarioRepo.findByCorreo(correo)
                .orElseThrow(() -> new java.util.NoSuchElementException(MensajeError.USUARIO_NO_ENCONTRADO.getMensaje()));
    }

    private List<EventoParticipanteDTO> mapParticipantes(List<EventoParticipante> list) {
        return list.stream().map(EventoMapper::toParticipanteDTO).toList();
    }

    /* ===== CRUD Evento ===== */
    @Transactional
    public EventoDetalleDTO crear(EventoCreateDTO dto) {
        Condominio condominio = getCondominioOrThrow(dto.getIdCondominio());
        Usuario creador = getUsuarioOrThrow(dto.getIdCreador());

        Evento e = EventoMapper.toEntityForCreate(dto, condominio, creador);
        eventoRepo.save(e);
        emitirAvisoEvento(e, AvisoTipo.EVENTO_NUEVO,
                String.format("Se programó el evento \"%s\" para %s", e.getTitulo(), e.getFechaInicio()));

        return EventoMapper.toDetalle(e, List.of());
    }

    public EventoDetalleDTO obtener(Integer id) {
        Evento e = getEventoOrThrow(id);
        List<EventoParticipanteDTO> participantes = participanteRepo.findByEvento_Id(id).stream()
                .map(EventoMapper::toParticipanteDTO)
                .toList();
        return EventoMapper.toDetalle(e, participantes);
    }

    public List<EventoResumenDTO> listarPorCondominio(Integer condominioId) {
        if (!condominioRepo.existsById(condominioId))
            throw new java.util.NoSuchElementException(MensajeError.CONDOMINIO_NO_ENCONTRADO.getMensaje());
        return eventoRepo.findByCondominio_IdOrderByFechaInicioDesc(condominioId)
                .stream().map(EventoMapper::toResumen).toList();
    }

    @Transactional
    public EventoDetalleDTO actualizar(Integer id, EventoUpdateDTO dto) {
        Evento e = getEventoOrThrow(id);
        if (dto.getTitulo() != null) e.setTitulo(dto.getTitulo());
        if (dto.getDescripcion() != null) e.setDescripcion(dto.getDescripcion());
        if (dto.getFechaInicio() != null) e.setFechaInicio(dto.getFechaInicio());
        if (dto.getFechaFin() != null) e.setFechaFin(dto.getFechaFin());
        if (dto.getLugar() != null) e.setLugar(dto.getLugar());
        if (dto.getTipo() != null) e.setTipo(dto.getTipo());
        if (dto.getEstado() != null) e.setEstado(dto.getEstado());

        List<EventoParticipanteDTO> participantes = participanteRepo.findByEvento_Id(id).stream()
                .map(EventoMapper::toParticipanteDTO)
                .toList();
        emitirAvisoEvento(e, AvisoTipo.EVENTO_ACTUALIZADO,
                String.format("El evento \"%s\" fue actualizado.", e.getTitulo()));
        return EventoMapper.toDetalle(e, participantes);
    }

    @Transactional
    public void eliminar(Integer id) {
        Evento evento = getEventoOrThrow(id);
        emitirAvisoEvento(evento, AvisoTipo.EVENTO_CANCELADO,
                String.format("El evento \"%s\" fue cancelado.", evento.getTitulo()));
        eventoRepo.delete(evento);
    }

    /* ===== Participantes ===== */
    @Transactional
    public EventoParticipanteDTO agregarParticipante(Integer eventoId, EventoParticipanteAddDTO dto) {
        Evento evento = getEventoOrThrow(eventoId);
        Usuario usuario = getUsuarioOrThrow(dto.getIdUsuario());

        if (participanteRepo.existsByEventoIdAndUsuarioId(eventoId, usuario.getId_usuario())) {
            throw new IllegalArgumentException(MensajeError.PARTICIPANTE_YA_EXISTE.getMensaje());
        }

        EventoParticipante ep = EventoParticipante.builder()
                .evento(evento)
                .usuario(usuario)
                .asistencia(dto.getAsistencia())
                .build();
        if (dto.getAsistencia() != null && dto.getAsistencia() != AsistenciaEstado.PENDIENTE) {
            ep.setFechaRespuesta(LocalDateTime.now());
        }
        participanteRepo.save(ep);
        return EventoMapper.toParticipanteDTO(ep);
    }

    public List<EventoParticipanteDTO> listarParticipantes(Integer eventoId) {
        // asegura existencia del evento
        if (!eventoRepo.existsById(eventoId))
            throw new java.util.NoSuchElementException(MensajeError.EVENTO_NO_ENCONTRADO.getMensaje());
        return participanteRepo.findByEvento_Id(eventoId).stream()
                .map(EventoMapper::toParticipanteDTO)
                .toList();
    }

    @Transactional
    public EventoParticipanteDTO actualizarAsistencia(Integer eventoId, Integer usuarioId, AsistenciaEstado nuevaAsistencia) {
        EventoParticipante ep = participanteRepo.findByEventoIdAndUsuarioId(eventoId, usuarioId)
                .orElseThrow(() -> new java.util.NoSuchElementException(MensajeError.PARTICIPANTE_NO_ENCONTRADO.getMensaje()));
        ep.setAsistencia(nuevaAsistencia);
        ep.setFechaRespuesta(LocalDateTime.now());
        return EventoMapper.toParticipanteDTO(ep);
    }

    @Transactional
    public void eliminarParticipante(Integer eventoId, Integer usuarioId) {
        if (!participanteRepo.existsByEventoIdAndUsuarioId(eventoId, usuarioId))
            throw new java.util.NoSuchElementException(MensajeError.PARTICIPANTE_NO_ENCONTRADO.getMensaje());
        participanteRepo.deleteByEventoIdAndUsuarioId(eventoId, usuarioId);
    }

    @Transactional
    public EventoParticipanteDTO confirmarAsistencia(Integer eventoId, Integer usuarioId, AsistenciaEstado estado) {
        // Si existe, actualiza; si no, crea con el estado indicado
        EventoParticipante ep = participanteRepo.findByEventoIdAndUsuarioId(eventoId, usuarioId).orElse(null);
        if (ep == null) {
            Evento evento = getEventoOrThrow(eventoId);
            Usuario usuario = getUsuarioOrThrow(usuarioId);
            ep = EventoParticipante.builder()
                    .evento(evento)
                    .usuario(usuario)
                    .asistencia(estado != null ? estado : AsistenciaEstado.PENDIENTE)
                    .build();
        } else {
            ep.setAsistencia(estado != null ? estado : AsistenciaEstado.PENDIENTE);
        }
        ep.setFechaRespuesta(LocalDateTime.now());
        participanteRepo.save(ep);
        return EventoMapper.toParticipanteDTO(ep);
    }

    @Transactional
    public EventoParticipanteDTO confirmarAsistenciaActual(Integer eventoId, String correo) {
        Usuario usuario = getUsuarioOrThrowByCorreo(correo);
        return confirmarAsistencia(eventoId, usuario.getId_usuario(), AsistenciaEstado.CONFIRMADO);
    }

    @Transactional
    public EventoParticipanteDTO actualizarAsistenciaActual(Integer eventoId, String correo, AsistenciaEstado estado) {
        Usuario usuario = getUsuarioOrThrowByCorreo(correo);
        return confirmarAsistencia(eventoId, usuario.getId_usuario(), estado);
    }

    private void emitirAvisoEvento(Evento evento, AvisoTipo tipo, String mensaje) {
        if (evento == null || evento.getCondominio() == null) {
            return;
        }
        var condominio = evento.getCondominio();
        var metadata = new java.util.HashMap<String, Object>();
        metadata.put("eventoId", evento.getId());
        metadata.put("condominioId", condominio.getId());
        metadata.put("titulo", evento.getTitulo());
        metadata.put("fechaInicio", evento.getFechaInicio());
        metadata.put("estado", evento.getEstado());

        avisoService.enviarAvisoCondominio(
                condominio.getId(),
                tipo,
                tipo.getTituloDefecto(),
                mensaje,
                metadata
        );
    }
}
