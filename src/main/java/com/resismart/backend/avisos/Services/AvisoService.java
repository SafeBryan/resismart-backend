package com.resismart.backend.avisos.Services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resismart.backend.avisos.DTO.AvisoPayload;
import com.resismart.backend.avisos.DTO.AvisoRequest;
import com.resismart.backend.avisos.Entities.Aviso;
import com.resismart.backend.avisos.Entities.AvisoUsuario;
import com.resismart.backend.avisos.Enums.AvisoDestino;
import com.resismart.backend.avisos.Enums.AvisoTipo;
import com.resismart.backend.avisos.Repositories.AvisoRepository;
import com.resismart.backend.avisos.Repositories.AvisoUsuarioRepository;
import com.resismart.backend.avisos.websocket.AvisoWebSocketHub;
import com.resismart.backend.condominios.Repositories.CondominioRepository;
import com.resismart.backend.residentes.Repositories.ResidenteRepository;
import com.resismart.backend.users.Entities.Usuario;
import com.resismart.backend.users.Enums.Rol;
import com.resismart.backend.users.Repositories.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AvisoService {

    private final AvisoRepository avisoRepository;
    private final AvisoUsuarioRepository avisoUsuarioRepository;
    private final AvisoWebSocketHub avisoWebSocketHub;
    private final UsuarioRepository usuarioRepository;
    private final ResidenteRepository residenteRepository;
    private final CondominioRepository condominioRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public AvisoPayload emitirDesdeRequest(AvisoRequest request) {
        if (request.getTipo() == null) {
            throw new IllegalArgumentException("El tipo de aviso es obligatorio");
        }
        if (request.getDestino() == null) {
            throw new IllegalArgumentException("El destino del aviso es obligatorio");
        }
        String titulo = request.getTitulo() != null
                ? request.getTitulo()
                : request.getTipo().getTituloDefecto();
        String mensaje = request.getMensaje() != null ? request.getMensaje() : "";

        return persistirYEmitir(
                request.getTipo(),
                titulo,
                mensaje,
                request.getDestino(),
                request.getDestinoReferencia(),
                request.getMetadata()
        );
    }

    @Transactional
    public AvisoPayload enviarAvisoUsuario(Integer usuarioId,
                                           AvisoTipo tipo,
                                           String titulo,
                                           String mensaje,
                                           Map<String, Object> metadata) {
        return persistirYEmitir(
                tipo,
                titulo != null ? titulo : tipo.getTituloDefecto(),
                mensaje,
                AvisoDestino.USUARIO,
                usuarioId != null ? String.valueOf(usuarioId) : null,
                metadata
        );
    }

    @Transactional
    public AvisoPayload enviarAvisoCondominio(Integer condominioId,
                                              AvisoTipo tipo,
                                              String titulo,
                                              String mensaje,
                                              Map<String, Object> metadata) {
        return persistirYEmitir(
                tipo,
                titulo != null ? titulo : tipo.getTituloDefecto(),
                mensaje,
                AvisoDestino.CONDOMINIO,
                condominioId != null ? String.valueOf(condominioId) : null,
                metadata
        );
    }

    @Transactional
    public AvisoPayload enviarAvisoRol(String rol,
                                       AvisoTipo tipo,
                                       String titulo,
                                       String mensaje,
                                       Map<String, Object> metadata) {
        return persistirYEmitir(
                tipo,
                titulo != null ? titulo : tipo.getTituloDefecto(),
                mensaje,
                AvisoDestino.ROL,
                rol,
                metadata
        );
    }

    @Transactional
    public AvisoPayload enviarAvisoBroadcast(AvisoTipo tipo,
                                             String titulo,
                                             String mensaje,
                                             Map<String, Object> metadata) {
        return persistirYEmitir(
                tipo,
                titulo != null ? titulo : tipo.getTituloDefecto(),
                mensaje,
                AvisoDestino.TODOS,
                null,
                metadata
        );
    }

    @Transactional(readOnly = true)
    public List<AvisoPayload> listarPorUsuario(Integer usuarioId) {
        if (usuarioId == null) {
            return List.of();
        }
        return avisoUsuarioRepository
                .buscarPorUsuario(usuarioId, PageRequest.of(0, 50))
                .stream()
                .map(AvisoUsuario::getAviso)
                .map(this::toPayload)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AvisoPayload> listarPorCondominio(Integer condominioId) {
        return avisoRepository
                .findTop50ByDestinoAndDestinoReferenciaOrderByCreadoEnDesc(
                        AvisoDestino.CONDOMINIO,
                        String.valueOf(condominioId)
                ).stream()
                .map(this::toPayload)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AvisoPayload> listarBroadcast() {
        return avisoRepository
                .findTop50ByDestinoOrderByCreadoEnDesc(AvisoDestino.TODOS)
                .stream()
                .map(this::toPayload)
                .toList();
    }

    @Transactional
    public void entregarPendientesUsuario(int usuarioId) {
        List<AvisoUsuario> pendientes = avisoUsuarioRepository
                .pendientesPorUsuario(usuarioId, PageRequest.of(0, 50));
        if (pendientes.isEmpty()) {
            return;
        }
        for (AvisoUsuario registro : pendientes) {
            Aviso aviso = registro.getAviso();
            AvisoPayload payload = toPayload(aviso);
            boolean entregado = avisoWebSocketHub.enviarAUsuario(usuarioId, payload);
            if (entregado) {
                avisoUsuarioRepository.marcarEntregado(aviso.getId(), usuarioId, Instant.now());
            } else {
                break;
            }
        }
    }

    private AvisoPayload persistirYEmitir(AvisoTipo tipo,
                                          String titulo,
                                          String mensaje,
                                          AvisoDestino destino,
                                          String destinoReferencia,
                                          Map<String, Object> metadata) {
        String metadataJson = serialize(metadata);
        Aviso aviso = Aviso.builder()
                .tipo(tipo)
                .titulo(titulo)
                .mensaje(mensaje)
                .destino(destino)
                .destinoReferencia(destinoReferencia)
                .metadataJson(metadataJson)
                .build();
        Aviso guardado = avisoRepository.save(aviso);
        AvisoPayload payload = toPayload(guardado);

        List<Integer> destinatarios = resolverDestinatarios(destino, destinoReferencia);
        if (!destinatarios.isEmpty()) {
            registrarAvisoUsuarios(guardado, destinatarios);
            notificarDestinatarios(guardado, payload, destinatarios);
        }
        return payload;
    }

    private void registrarAvisoUsuarios(Aviso aviso, List<Integer> destinatarios) {
        Set<Integer> ids = new LinkedHashSet<>(destinatarios);
        List<AvisoUsuario> registros = new ArrayList<>();
        for (Integer usuarioId : ids) {
            if (usuarioId == null) continue;
            try {
                AvisoUsuario au = AvisoUsuario.builder()
                        .aviso(aviso)
                        .usuario(usuarioRepository.getReferenceById(usuarioId))
                        .leido(false)
                        .build();
                registros.add(au);
            } catch (EntityNotFoundException ignored) {
            }
        }
        if (!registros.isEmpty()) {
            avisoUsuarioRepository.saveAll(registros);
        }
    }

    private void notificarDestinatarios(Aviso aviso,
                                        AvisoPayload payload,
                                        List<Integer> destinatarios) {
        Set<Integer> ids = new HashSet<>(destinatarios);
        for (Integer usuarioId : ids) {
            if (usuarioId == null) continue;
            boolean entregado = avisoWebSocketHub.enviarAUsuario(usuarioId, payload);
            if (entregado) {
                avisoUsuarioRepository.marcarEntregado(aviso.getId(), usuarioId, Instant.now());
            }
        }
    }

    private List<Integer> resolverDestinatarios(AvisoDestino destino, String destinoReferencia) {
        return switch (destino) {
            case USUARIO -> {
                Integer usuarioId = parseEntero(destinoReferencia);
                if (usuarioId == null) {
                    yield List.of();
                }
                yield usuarioRepository.findById(usuarioId)
                        .filter(Usuario::isEstado)
                        .map(u -> List.of(u.getId_usuario()))
                        .orElse(List.of());
            }
            case CONDOMINIO -> {
                Integer condominioId = parseEntero(destinoReferencia);
                if (condominioId == null) {
                    yield List.of();
                }
                Set<Integer> ids = new LinkedHashSet<>();
                condominioRepository.findById(condominioId).ifPresent(condominio -> {
                    Usuario dueno = condominio.getDueno();
                    if (dueno != null && dueno.isEstado()) {
                        ids.add(dueno.getId_usuario());
                    }
                });
                residenteRepository.findResidentesPorCondominio(condominioId)
                        .stream()
                        .map(res -> res.getUsuario())
                        .filter(Objects::nonNull)
                        .filter(Usuario::isEstado)
                        .map(Usuario::getId_usuario)
                        .forEach(ids::add);
                yield new ArrayList<>(ids);
            }
            case ROL -> {
                List<Integer> ids = List.of();
                if (destinoReferencia != null && !destinoReferencia.isBlank()) {
                    try {
                        Rol rol = Rol.valueOf(destinoReferencia.trim());
                        ids = new ArrayList<>(usuarioRepository.findIdsPorRol(rol));
                    } catch (IllegalArgumentException ignored) {
                        ids = List.of();
                    }
                }
                yield ids;
            }
            case TODOS -> new ArrayList<>(usuarioRepository.findIdsUsuariosActivos());
        };
    }


    private AvisoPayload toPayload(Aviso aviso) {
        return AvisoPayload.builder()
                .id(aviso.getId())
                .tipo(aviso.getTipo())
                .titulo(aviso.getTitulo())
                .mensaje(aviso.getMensaje())
                .destino(aviso.getDestino())
                .destinoReferencia(aviso.getDestinoReferencia())
                .emitidoEn(aviso.getCreadoEn())
                .metadata(deserialize(aviso.getMetadataJson()))
                .build();
    }

    private String serialize(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No fue posible serializar la metadata del aviso", e);
        }
    }

    private Map<String, Object> deserialize(String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(metadataJson, HashMap.class);
        } catch (JsonProcessingException e) {
            return Collections.emptyMap();
        }
    }

    private Integer parseEntero(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(valor.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
