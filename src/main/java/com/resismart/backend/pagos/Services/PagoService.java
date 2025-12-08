package com.resismart.backend.pagos.Services;

import com.resismart.backend.Auth.EmailService;
import com.resismart.backend.avisos.Enums.AvisoTipo;
import com.resismart.backend.avisos.Services.AvisoService;
import com.resismart.backend.pagos.DTO.TransaccionPagoCreateDTO;
import com.resismart.backend.pagos.Entities.OrdenPago;
import com.resismart.backend.pagos.Entities.TransaccionPago;
import com.resismart.backend.pagos.Enums.EstadoOrdenPago;
import com.resismart.backend.pagos.Enums.EstadoTransaccion;
import com.resismart.backend.pagos.Repositories.OrdenPagoRepository;
import com.resismart.backend.pagos.Repositories.TransaccionPagoRepository;
import com.resismart.backend.storage.StoragePort;
import com.resismart.backend.users.Entities.Usuario;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PagoService {

    private final OrdenPagoRepository ordenPagoRepository;
    private final TransaccionPagoRepository transaccionPagoRepository;
    private final StoragePort storagePort;
    private final AvisoService avisoService;
    private final EmailService emailService;
    private final MoraService moraService;

    @Transactional
    public TransaccionPago registrarTransaccion(TransaccionPagoCreateDTO dto) {
        OrdenPago orden = ordenPagoRepository.findById(dto.getOrdenId())
                .orElseThrow(() -> new java.util.NoSuchElementException("Orden de pago no encontrada"));

        moraService.recalcularMora(orden);
        ordenPagoRepository.save(orden);

        if (orden.getEstado() == EstadoOrdenPago.PAGADA) {
            throw new IllegalStateException("La orden ya esta pagada");
        }
        BigDecimal saldo = safe(orden.getSaldoPendiente());
        BigDecimal totalPendiente = saldo;
        if (dto.getMonto() != null && totalPendiente.compareTo(BigDecimal.ZERO) > 0 && dto.getMonto().compareTo(totalPendiente) > 0) {
            throw new IllegalArgumentException("El monto supera el saldo pendiente (incluyendo mora) de la orden");
        }

        String storageKey = guardarComprobante(dto.getComprobante(), orden);

        TransaccionPago tx = TransaccionPago.builder()
                .ordenPago(orden)
                .monto(dto.getMonto())
                .fechaPago(LocalDateTime.now())
                .referencia(dto.getReferencia())
                .metodoPago(dto.getMetodoPago())
                .comprobanteStorageKey(storageKey)
                .estado(EstadoTransaccion.PENDIENTE)
                .build();

        TransaccionPago guardada = transaccionPagoRepository.save(tx);
        log.info("Transaccion registrada id={} orden={} contrato={} usuario={}",
                guardada.getId(),
                orden.getId(),
                obtenerContratoId(orden),
                obtenerUsuarioId(orden));
        return guardada;
    }

    @Transactional
    public TransaccionPago aprobarTransaccion(Long transaccionId, Long idAdmin) {
        TransaccionPago tx = transaccionPagoRepository.findById(transaccionId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Transaccion no encontrada"));

        if (tx.getEstado() != EstadoTransaccion.PENDIENTE) {
            throw new IllegalStateException("Solo se pueden aprobar transacciones pendientes");
        }
        OrdenPago orden = tx.getOrdenPago();
        if (orden == null) {
            throw new IllegalStateException("Transaccion sin orden de pago asociada");
        }

        tx.setEstado(EstadoTransaccion.APROBADO);
        tx.setAprobadoPorAdminId(idAdmin != null ? idAdmin.intValue() : null);
        tx.setFechaAprobacion(LocalDateTime.now());
        tx.setMotivoRechazo(null);
        tx.setFechaRechazo(null);
        tx.setRechazadoPorAdminId(null);

        transaccionPagoRepository.save(tx);
        transaccionPagoRepository.flush();

        moraService.recalcularMora(orden);
        ordenPagoRepository.save(orden);

        emitirAvisoAprobacion(orden, tx);
        log.info("Transaccion aprobada id={} orden={} contrato={} usuario={} admin={}", tx.getId(),
                orden.getId(),
                obtenerContratoId(orden),
                obtenerUsuarioId(orden),
                idAdmin);
        return tx;
    }

    @Transactional
    public TransaccionPago rechazarTransaccion(Long transaccionId, String motivo, Long idAdmin) {
        TransaccionPago tx = transaccionPagoRepository.findById(transaccionId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Transaccion no encontrada"));
        if (tx.getEstado() != EstadoTransaccion.PENDIENTE) {
            throw new IllegalStateException("Solo se pueden rechazar transacciones pendientes");
        }
        OrdenPago orden = tx.getOrdenPago();
        if (orden == null) {
            throw new IllegalStateException("Transaccion sin orden de pago asociada");
        }

        String motivoFinal = motivo != null && !motivo.isBlank() ? motivo.trim() : null;

        tx.setEstado(EstadoTransaccion.RECHAZADO);
        tx.setMotivoRechazo(motivoFinal);
        tx.setRechazadoPorAdminId(idAdmin != null ? idAdmin.intValue() : null);
        tx.setFechaRechazo(LocalDateTime.now());
        tx.setAprobadoPorAdminId(null);
        tx.setFechaAprobacion(null);

        transaccionPagoRepository.save(tx);
        transaccionPagoRepository.flush();

        moraService.recalcularMora(orden);
        ordenPagoRepository.save(orden);

        emitirAvisoRechazo(orden, tx, motivoFinal);
        log.info("Transaccion rechazada id={} orden={} contrato={} usuario={} motivo={} admin={}", tx.getId(),
                orden.getId(),
                obtenerContratoId(orden),
                obtenerUsuarioId(orden),
                motivoFinal,
                idAdmin);
        return tx;
    }

    @Transactional
    public void pagarEnVentanilla(Integer idOrden, Long idAdmin) {
        OrdenPago orden = ordenPagoRepository.findById(idOrden)
                .orElseThrow(() -> new java.util.NoSuchElementException("Orden de pago no encontrada"));
        moraService.recalcularMora(orden);
        BigDecimal monto = orden.getSaldoPendiente() != null ? orden.getSaldoPendiente() : orden.getMontoBase();

        orden.setSaldoPendiente(BigDecimal.ZERO);
        orden.setEstado(EstadoOrdenPago.PAGADA);
        orden.setMoraAcumulada(BigDecimal.ZERO);
        ordenPagoRepository.save(orden);

        TransaccionPago tx = TransaccionPago.builder()
                .ordenPago(orden)
                .monto(monto)
                .fechaPago(LocalDateTime.now())
                .estado(EstadoTransaccion.APROBADO)
                .aprobadoPorAdminId(idAdmin != null ? idAdmin.intValue() : null)
                .fechaAprobacion(LocalDateTime.now())
                .build();
        transaccionPagoRepository.save(tx);
        transaccionPagoRepository.flush();
        emitirAvisoAprobacion(orden, tx);
        log.info("Pago en ventanilla registrado orden={} contrato={} usuario={} admin={}", orden.getId(),
                obtenerContratoId(orden),
                obtenerUsuarioId(orden),
                idAdmin);
    }

    private String guardarComprobante(MultipartFile archivo, OrdenPago orden) {
        if (archivo == null || archivo.isEmpty()) {
            return null;
        }
        Integer usuarioId;
        try {
            usuarioId = obtenerUsuarioId(orden);
        } catch (Exception ignored) {
            usuarioId = null;
        }
        String path = usuarioId != null ? "users/" + usuarioId + "/comprobantes" : "comprobantes";
        return storagePort.guardarArchivoEn(archivo, path);
    }

    private void emitirAvisoAprobacion(OrdenPago orden, TransaccionPago tx) {
        Usuario usuario = obtenerUsuarioInquilino(orden);
        Integer usuarioId = usuario != null ? usuario.getId_usuario() : null;
        if (usuarioId == null) {
            return;
        }
        Map<String, Object> metadata = metadataBasica(orden, tx);
        boolean pagada = orden.getSaldoPendiente() != null && orden.getSaldoPendiente().compareTo(BigDecimal.ZERO) == 0;
        AvisoTipo tipo = pagada ? AvisoTipo.ORDEN_PAGO_PAGADA : AvisoTipo.ALERTA_GENERAL;
        String periodo = orden.getPeriodo() != null ? YearMonth.from(orden.getPeriodo()).toString() : "#" + orden.getId();
        String mensaje = pagada
                ? String.format("Tu pago de la orden %s por %s ha sido aprobado.", periodo, tx.getMonto())
                : String.format("Tu pago de la orden %s por %s ha sido aprobado. Saldo pendiente: %s.",
                periodo, tx.getMonto(), orden.getSaldoPendiente());

        avisoService.enviarAvisoUsuario(
                usuarioId,
                tipo,
                tipo.getTituloDefecto(),
                mensaje,
                metadata
        );
        try {
            emailService.enviarAvisoOrdenPagoPagada(usuario, tx);
        } catch (Exception e) {
            log.warn("No se pudo enviar correo de pago aprobado {}: {}", tx.getId(), e.getMessage());
        }
    }

    private void emitirAvisoRechazo(OrdenPago orden, TransaccionPago tx, String motivo) {
        Usuario usuario = obtenerUsuarioInquilino(orden);
        Integer usuarioId = usuario != null ? usuario.getId_usuario() : null;
        if (usuarioId == null) {
            return;
        }
        Map<String, Object> metadata = metadataBasica(orden, tx);
        metadata.put("motivoRechazo", motivo);
        String periodo = orden.getPeriodo() != null ? YearMonth.from(orden.getPeriodo()).toString() : "#" + orden.getId();
        String mensaje = String.format(
                "Tu pago de la orden %s ha sido rechazado. Motivo: %s.",
                periodo,
                motivo != null && !motivo.isBlank() ? motivo : "Revisa el comprobante y vuelve a cargarlo."
        );

        avisoService.enviarAvisoUsuario(
                usuarioId,
                AvisoTipo.DOCUMENTO_RECHAZADO,
                AvisoTipo.DOCUMENTO_RECHAZADO.getTituloDefecto(),
                mensaje,
                metadata
        );
        try {
            emailService.enviarAvisoOrdenPagoRechazada(usuario, tx, motivo);
        } catch (Exception e) {
            log.warn("No se pudo enviar correo de pago rechazado {}: {}", tx.getId(), e.getMessage());
        }
    }

    @Transactional
    public List<TransaccionPago> listarTransacciones(@Nullable EstadoTransaccion estado, @Nullable Integer ordenId) {
        if (ordenId != null) {
            List<TransaccionPago> lista = transaccionPagoRepository.findByOrdenPago_IdOrderByFechaPagoDesc(ordenId);
            if (estado == null) return lista;
            return lista.stream().filter(tx -> estado.equals(tx.getEstado())).toList();
        }
        if (estado != null) {
            return transaccionPagoRepository.findByEstadoOrderByFechaPagoDesc(estado);
        }
        return transaccionPagoRepository.findAll();
    }

    private Usuario obtenerUsuarioInquilino(OrdenPago orden) {
        if (orden == null || orden.getContrato() == null || orden.getContrato().getResidente() == null) {
            return null;
        }
        return orden.getContrato().getResidente().getUsuario();
    }

    private Integer obtenerUsuarioId(OrdenPago orden) {
        Usuario usuario = obtenerUsuarioInquilino(orden);
        return usuario != null ? usuario.getId_usuario() : null;
    }

    private Integer obtenerContratoId(OrdenPago orden) {
        return orden != null && orden.getContrato() != null ? orden.getContrato().getId() : null;
    }

    private Map<String, Object> metadataBasica(OrdenPago orden, TransaccionPago tx) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("contratoId", orden != null && orden.getContrato() != null ? orden.getContrato().getId() : null);
        metadata.put("ordenId", orden != null ? orden.getId() : null);
        metadata.put("transaccionId", tx != null ? tx.getId() : null);
        metadata.put("periodo", orden != null ? orden.getPeriodo() : null);
        metadata.put("monto", tx != null ? tx.getMonto() : null);
        metadata.put("saldoPendiente", orden != null ? orden.getSaldoPendiente() : null);
        metadata.put("mora", orden != null ? orden.getMoraAcumulada() : null);
        metadata.put("fechaPago", tx != null ? tx.getFechaPago() : null);
        return metadata;
    }

    private BigDecimal safe(BigDecimal valor) {
        return valor != null ? valor : BigDecimal.ZERO;
    }
}
