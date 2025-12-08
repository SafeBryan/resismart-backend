package com.resismart.backend.avisos.Services;

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
import com.resismart.backend.pagos.Entities.OrdenPago;
import com.resismart.backend.pagos.Entities.TransaccionPago;
import com.resismart.backend.residentes.Repositories.ResidenteRepository;
import com.resismart.backend.users.Entities.Usuario;
import com.resismart.backend.users.Enums.Rol;
import com.resismart.backend.users.Repositories.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.YearMonth;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AvisoService {

    private final AvisoRepository avisoRepository;
    private final AvisoUsuarioRepository avisoUsuarioRepository;
    private final AvisoWebSocketHub avisoWebSocketHub;
    private final UsuarioRepository usuarioRepository;
    private final ResidenteRepository residenteRepository;
    private final CondominioRepository condominioRepository;
    private final SimpMessagingTemplate messagingTemplate;
    /* ========= Nuevos entry points ========= */
    @Transactional
    public AvisoPayload crearAvisoGeneral(AvisoRequest request, Usuario emisor) {
        validarRequestBasico(request);
        validarPermisosGeneral(request, emisor);
        return persistirYEmitir(buildTitulo(request), buildMensaje(request), request, emisor, request.getReceiverId(), request.getParentId());
    }

    @Transactional
    public AvisoPayload crearAvisoPrivado(AvisoRequest request, Usuario emisor) {
        if (request.getReceiverId() == null) throw new IllegalArgumentException("Receiver requerido para privado");
        validarPermisosPrivado(request.getReceiverId(), emisor);
        request.setDestino(AvisoDestino.USUARIO);
        request.setDestinoReferencia(request.getReceiverId().toString());
        return persistirYEmitir(buildTituloPrivado(request), buildMensaje(request), request, emisor, request.getReceiverId(), request.getParentId());
    }

    @Transactional
    public AvisoPayload responderAviso(Long parentId, AvisoRequest request, Usuario emisor) {
        if (parentId == null) throw new IllegalArgumentException("parentId requerido");
        Aviso padre = avisoRepository.findById(parentId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Aviso padre no encontrado"));
        validarPermisosResponder(padre, emisor);
        Integer receiverId = padre.getSender() != null ? padre.getSender().getId_usuario() : null;
        request.setDestino(AvisoDestino.USUARIO);
        request.setDestinoReferencia(receiverId != null ? receiverId.toString() : null);
        request.setParentId(parentId);
        return persistirYEmitir("Re: " + padre.getTitulo(), buildMensaje(request), request, emisor, receiverId, parentId);
    }

    @Transactional(readOnly = true)
    public List<AvisoPayload> obtenerConversacion(Integer usuarioId1, Integer usuarioId2) {
        return avisoRepository.findConversacion(usuarioId1, usuarioId2)
                .stream()
                .map(this::toPayload)
                .toList();
    }

    public Usuario findUsuarioByCorreo(String correo) {
        return usuarioRepository.findByCorreo(correo).orElse(null);
    }

    public Usuario findUsuarioById(Integer id) {
        return usuarioRepository.findById(id).orElse(null);
    }

    @Transactional
    public void notificarOrdenPagoGenerada(OrdenPago ordenPago) {
        if (ordenPago == null) {
            log.warn("No se pudo notificar orden de pago: orden nula");
            return;
        }
        var contrato = ordenPago.getContrato();
        var residente = contrato != null ? contrato.getResidente() : null;
        Usuario usuario = residente != null ? residente.getUsuario() : null;
        Integer usuarioId = usuario != null ? usuario.getId_usuario() : null;
        if (usuarioId == null) {
            log.warn("Orden {} sin usuario inquilino asociado para aviso", ordenPago.getId());
            return;
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("idContrato", contrato != null ? contrato.getId() : null);
        metadata.put("idOrdenPago", ordenPago.getId());
        YearMonth periodo = ordenPago.getPeriodo() != null ? YearMonth.from(ordenPago.getPeriodo()) : null;
        metadata.put("periodo", periodo != null ? periodo.toString() : null);
        metadata.put("monto", ordenPago.getMontoBase());
        metadata.put("fechaVencimiento", ordenPago.getFechaVencimiento());

        String periodoTexto = periodo != null ? periodo.toString() : "periodo no disponible";
        String mensaje = "Se genero una nueva orden de pago del periodo " + periodoTexto + ".";

        enviarAvisoUsuario(
                usuarioId,
                AvisoTipo.ORDEN_PAGO_GENERADA,
                AvisoTipo.ORDEN_PAGO_GENERADA.getTituloDefecto(),
                mensaje,
                metadata
        );
        log.info("Aviso interno de orden de pago {} enviado a usuario {}", ordenPago.getId(), usuarioId);
    }

    @Transactional
    public void notificarOrdenPagoPagada(TransaccionPago transaccion) {
        if (transaccion == null || transaccion.getOrdenPago() == null) {
            log.warn("No se pudo notificar pago: transaccion u orden nula");
            return;
        }
        var orden = transaccion.getOrdenPago();
        var contrato = orden.getContrato();
        var residente = contrato != null ? contrato.getResidente() : null;
        Usuario usuario = residente != null ? residente.getUsuario() : null;
        Integer usuarioId = usuario != null ? usuario.getId_usuario() : null;
        if (usuarioId == null) {
            log.warn("Transaccion {} sin usuario inquilino asociado", transaccion.getId());
            return;
        }
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("idContrato", contrato != null ? contrato.getId() : null);
        metadata.put("idOrdenPago", orden.getId());
        metadata.put("idTransaccion", transaccion.getId());
        metadata.put("montoPagado", transaccion.getMonto());
        metadata.put("fechaPago", transaccion.getFechaPago());
        metadata.put("periodo", orden.getPeriodo() != null ? YearMonth.from(orden.getPeriodo()).toString() : null);

        enviarAvisoUsuario(
                usuarioId,
                AvisoTipo.ORDEN_PAGO_PAGADA,
                AvisoTipo.ORDEN_PAGO_PAGADA.getTituloDefecto(),
                "Tu pago fue aprobado",
                metadata
        );
        log.info("Aviso interno de pago aprobado transaccion {} enviado a usuario {}", transaccion.getId(), usuarioId);
    }

    @Transactional
    public void notificarOrdenPagoRechazada(TransaccionPago transaccion, String motivo) {
        if (transaccion == null || transaccion.getOrdenPago() == null) {
            log.warn("No se pudo notificar rechazo: transaccion u orden nula");
            return;
        }
        var orden = transaccion.getOrdenPago();
        var contrato = orden.getContrato();
        var residente = contrato != null ? contrato.getResidente() : null;
        Usuario usuario = residente != null ? residente.getUsuario() : null;
        Integer usuarioId = usuario != null ? usuario.getId_usuario() : null;
        if (usuarioId == null) {
            log.warn("Transaccion {} sin usuario inquilino asociado para rechazo", transaccion.getId());
            return;
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("idContrato", contrato != null ? contrato.getId() : null);
        metadata.put("idOrdenPago", orden.getId());
        metadata.put("idTransaccion", transaccion.getId());
        metadata.put("monto", transaccion.getMonto());
        metadata.put("motivo", motivo);
        metadata.put("periodo", orden.getPeriodo() != null ? YearMonth.from(orden.getPeriodo()).toString() : null);

        String mensaje = "Tu pago fue rechazado" + (motivo != null && !motivo.isBlank() ? (": " + motivo) : ".");
        enviarAvisoUsuario(
                usuarioId,
                AvisoTipo.DOCUMENTO_RECHAZADO,
                AvisoTipo.DOCUMENTO_RECHAZADO.getTituloDefecto(),
                mensaje,
                metadata
        );
        log.info("Aviso interno de pago rechazado transaccion {} enviado a usuario {}", transaccion.getId(), usuarioId);
    }

    @Transactional
    public AvisoPayload enviarAvisoUsuario(Integer usuarioId,
                                           AvisoTipo tipo,
                                           String titulo,
                                           String mensaje,
                                           Map<String, Object> metadata) {
        log.debug("Emitir aviso {} directo a usuario {}", tipo, usuarioId);
        AvisoRequest req = baseRequest(tipo, AvisoDestino.USUARIO, usuarioId != null ? String.valueOf(usuarioId) : null, metadata);
        return persistirYEmitir(titulo != null ? titulo : tipo.getTituloDefecto(), mensaje, req, null, usuarioId, null);
    }

    @Transactional
    public AvisoPayload enviarAvisoCondominio(Integer condominioId,
                                              AvisoTipo tipo,
                                              String titulo,
                                              String mensaje,
                                              Map<String, Object> metadata) {
        log.debug("Emitir aviso {} para condominio {}", tipo, condominioId);
        AvisoRequest req = baseRequest(tipo, AvisoDestino.CONDOMINIO, condominioId != null ? String.valueOf(condominioId) : null, metadata);
        return persistirYEmitir(titulo != null ? titulo : tipo.getTituloDefecto(), mensaje, req, null, null, null);
    }

    @Transactional
    public AvisoPayload enviarAvisoRol(String rol,
                                       AvisoTipo tipo,
                                       String titulo,
                                       String mensaje,
                                       Map<String, Object> metadata) {
        log.debug("Emitir aviso {} para rol {}", tipo, rol);
        AvisoRequest req = baseRequest(tipo, AvisoDestino.ROL, rol, metadata);
        return persistirYEmitir(titulo != null ? titulo : tipo.getTituloDefecto(), mensaje, req, null, null, null);
    }

    @Transactional
    public AvisoPayload enviarAvisoBroadcast(AvisoTipo tipo,
                                             String titulo,
                                             String mensaje,
                                             Map<String, Object> metadata) {
        log.debug("Emitir aviso {} en broadcast", tipo);
        AvisoRequest req = baseRequest(tipo, AvisoDestino.TODOS, null, metadata);
        return persistirYEmitir(titulo != null ? titulo : tipo.getTituloDefecto(), mensaje, req, null, null, null);
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
        log.info("Usuario {} tiene {} avisos pendientes por entregar", usuarioId, pendientes.size());
        if (pendientes.isEmpty()) {
            return;
        }
        for (AvisoUsuario registro : pendientes) {
            Aviso aviso = registro.getAviso();
            AvisoPayload payload = toPayload(aviso);
            boolean entregado = avisoWebSocketHub.enviarAUsuario(usuarioId, payload);
            if (entregado) {
                avisoUsuarioRepository.marcarEntregado(aviso.getId(), usuarioId, Instant.now());
                log.debug("Aviso {} entregado a usuario {}", aviso.getId(), usuarioId);
            } else {
                log.warn("No fue posible entregar aviso {} al usuario {} (sin sesión activa)", aviso.getId(), usuarioId);
                break;
            }
        }
    }

    @Transactional
    public int marcarAvisosLeidos(Integer usuarioId, Collection<Long> avisoIds) {
        if (usuarioId == null || avisoIds == null || avisoIds.isEmpty()) {
            return 0;
        }
        List<Long> ids = avisoIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return 0;
        }
        int actualizados = avisoUsuarioRepository.marcarLeidos(usuarioId, ids, Instant.now());
        log.debug("Usuario {} marcó {} avisos como leídos", usuarioId, actualizados);
        return actualizados;
    }

    private AvisoPayload persistirYEmitir(String titulo,
                                          String mensaje,
                                          AvisoRequest request,
                                          Usuario emisor,
                                          Integer receiverId,
                                          Long parentId) {
        Aviso aviso = Aviso.builder()
                .tipo(request.getTipo())
                .titulo(titulo)
                .mensaje(mensaje)
                .destino(request.getDestino())
                .destinoReferencia(request.getDestinoReferencia())
                .metadata(request.getMetadata())
                .sender(emisor)
                .receiver(receiverId != null ? usuarioRepository.findById(receiverId).orElse(null) : null)
                .parent(parentId != null ? avisoRepository.findById(parentId).orElse(null) : null)
                .build();
        Aviso guardado = avisoRepository.save(aviso);
        AvisoPayload payload = toPayload(guardado);

        List<Integer> destinatarios = resolverDestinatarios(request.getDestino(), request.getDestinoReferencia());
        log.info("Aviso {} persistido destino {} ({}) -> {} destinatarios",
                guardado.getId(), request.getDestino(), request.getDestinoReferencia(), destinatarios.size());
        if (!destinatarios.isEmpty()) {
            registrarAvisoUsuarios(guardado, destinatarios);
            notificarDestinatarios(guardado, payload, destinatarios);
        } else {
            log.warn("Aviso {} no tiene destinatarios calculados", guardado.getId());
        }
        enviarStomp(payload, request, receiverId);
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
            log.debug("Registrados {} destinatarios para aviso {}", registros.size(), aviso.getId());
        } else {
            log.warn("Aviso {} sin registros en aviso_usuario (lista vacía)", aviso.getId());
        }
    }

    private void notificarDestinatarios(Aviso aviso,
                                        AvisoPayload payload,
                                        List<Integer> destinatarios) {
        Set<Integer> ids = new HashSet<>(destinatarios);
        log.debug("Notificando aviso {} a {} usuarios conectados potenciales", aviso.getId(), ids.size());
        for (Integer usuarioId : ids) {
            if (usuarioId == null) continue;
            boolean entregado = avisoWebSocketHub.enviarAUsuario(usuarioId, payload);
            if (entregado) {
                avisoUsuarioRepository.marcarEntregado(aviso.getId(), usuarioId, Instant.now());
                log.debug("Aviso {} marcado como entregado en vivo al usuario {}", aviso.getId(), usuarioId);
            } else {
                log.trace("Usuario {} sin sesión activa para aviso {}", usuarioId, aviso.getId());
            }
        }
    }

    private List<Integer> resolverDestinatarios(AvisoDestino destino, String destinoReferencia) {
        return switch (destino) {
            case USUARIO -> {
                Integer usuarioId = parseEntero(destinoReferencia);
                if (usuarioId == null) {
                    log.warn("Aviso destino USUARIO sin id válido ({})", destinoReferencia);
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
                    log.warn("Aviso destino CONDOMINIO sin id válido ({})", destinoReferencia);
                    yield List.of();
                }
                Set<Integer> ids = new LinkedHashSet<>();
                condominioRepository.findById(condominioId).ifPresent(condominio -> {
                    Usuario dueno = condominio.getDueno();
                    if (dueno != null && dueno.isEstado()) {
                        ids.add(dueno.getId_usuario());
                    }
                });
                // Relación residentes-condominio ahora se resuelve vía contratos; no se añade aquí.
                if (ids.isEmpty()) {
                    log.warn("Aviso destino CONDOMINIO {} no resolvió usuarios activos", condominioId);
                }
                yield new ArrayList<>(ids);
            }
            case ROL -> {
                List<Integer> ids = List.of();
                if (destinoReferencia != null && !destinoReferencia.isBlank()) {
                    try {
                        Rol rol = Rol.valueOf(destinoReferencia.trim());
                        ids = new ArrayList<>(usuarioRepository.findIdsPorRol(rol));
                    } catch (IllegalArgumentException ignored) {
                        log.warn("Aviso destino ROL inválido: {}", destinoReferencia);
                        ids = List.of();
                    }
                }
                yield ids;
            }
            case TODOS -> new ArrayList<>(usuarioRepository.findIdsUsuariosActivos());
        };
    }

    private void enviarStomp(AvisoPayload payload, AvisoRequest request, Integer receiverId) {
        if (payload == null || request == null) return;
        switch (request.getDestino()) {
            case USUARIO -> {
                Integer uid = receiverId != null ? receiverId : parseEntero(request.getDestinoReferencia());
                if (uid != null) {
                    messagingTemplate.convertAndSend("/topic/avisos/usuarios/" + uid, payload);
                }
            }
            case CONDOMINIO -> {
                String ref = request.getDestinoReferencia();
                if (ref != null && !ref.isBlank()) {
                    messagingTemplate.convertAndSend("/topic/avisos/condominios/" + ref, payload);
                }
            }
            case TODOS -> messagingTemplate.convertAndSend("/topic/avisos/broadcast", payload);
            case ROL -> {
                if (request.getDestinoReferencia() != null) {
                    messagingTemplate.convertAndSend("/topic/avisos/roles/" + request.getDestinoReferencia(), payload);
                }
            }
        }
    }


    private AvisoPayload toPayload(Aviso aviso) {
        return AvisoPayload.builder()
                .id(aviso.getId())
                .tipo(aviso.getTipo())
                .titulo(aviso.getTitulo())
                .mensaje(aviso.getMensaje())
                .destino(aviso.getDestino())
                .destinoReferencia(aviso.getDestinoReferencia())
                .emitidoEn(aviso.getCreadoEn().atZone(ZoneId.systemDefault()).toInstant())
                .metadata(aviso.getMetadata())
                .senderId(aviso.getSender() != null ? aviso.getSender().getId_usuario() : null)
                .receiverId(aviso.getReceiver() != null ? aviso.getReceiver().getId_usuario() : null)
                .parentId(aviso.getParent() != null ? aviso.getParent().getId() : null)
                .build();
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

    /* ===== Helpers de validación y títulos ===== */
    private void validarRequestBasico(AvisoRequest request) {
        if (request.getTipo() == null) throw new IllegalArgumentException("El tipo de aviso es obligatorio");
        if (request.getDestino() == null) throw new IllegalArgumentException("El destino del aviso es obligatorio");
    }

    private void validarPermisosGeneral(AvisoRequest request, Usuario emisor) {
        if (emisor == null) throw new IllegalArgumentException("Emisor requerido");
        // Solo ADMIN puede enviar avisos generales (broadcast, rol, condominio o usuario directo)
        if (emisor.getRol() != com.resismart.backend.users.Enums.Rol.ADMIN) {
            throw new IllegalArgumentException("Solo ADMIN puede enviar avisos generales");
        }
        if (request.getDestino() == null) {
            throw new IllegalArgumentException("Destino requerido");
        }
    }

    private void validarPermisosPrivado(Integer receiverId, Usuario emisor) {
        if (emisor == null) throw new IllegalArgumentException("Emisor requerido");
        if (receiverId == null) throw new IllegalArgumentException("Destinatario requerido");
        // Regla simple: residentes solo pueden escribir a ADMIN/DUEÑO
        if (emisor.getRol() == Rol.RESIDENTE) {
            Usuario receptor = usuarioRepository.findById(receiverId).orElse(null);
            if (receptor == null || (receptor.getRol() != Rol.ADMIN && receptor.getRol() != Rol.DUEÑO)) {
                throw new IllegalArgumentException("Residente solo puede enviar privados a ADMIN/DUEÑO");
            }
        }
    }

    private void validarPermisosResponder(Aviso padre, Usuario emisor) {
        if (emisor == null) throw new IllegalArgumentException("Emisor requerido");
        if (padre.getSender() == null) return;
        if (emisor.getRol() == Rol.RESIDENTE) {
            Rol rolPadre = padre.getSender().getRol();
            if (rolPadre != Rol.ADMIN && rolPadre != Rol.DUEÑO) {
                throw new IllegalArgumentException("Residente solo puede responder a ADMIN/DUEÑO");
            }
        }
    }

    private String buildTitulo(AvisoRequest req) {
        return req.getTitulo() != null ? req.getTitulo() : req.getTipo().getTituloDefecto();
    }

    private String buildTituloPrivado(AvisoRequest req) {
        return req.getTitulo() != null ? req.getTitulo() : "Mensaje privado";
    }

    private String buildMensaje(AvisoRequest req) {
        return req.getMensaje() != null ? req.getMensaje() : "";
    }

    private AvisoRequest baseRequest(AvisoTipo tipo, AvisoDestino destino, String destinoRef, Map<String, Object> metadata) {
        AvisoRequest req = new AvisoRequest();
        req.setTipo(tipo);
        req.setDestino(destino);
        req.setDestinoReferencia(destinoRef);
        req.setMetadata(metadata);
        return req;
    }
}

