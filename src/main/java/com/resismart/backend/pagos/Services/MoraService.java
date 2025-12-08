package com.resismart.backend.pagos.Services;

import com.resismart.backend.Auth.EmailService;
import com.resismart.backend.avisos.Enums.AvisoTipo;
import com.resismart.backend.avisos.Services.AvisoService;
import com.resismart.backend.documentos.Entities.OrdenDocumento;
import com.resismart.backend.documentos.Enums.EstadoValidacion;
import com.resismart.backend.documentos.Repositories.OrdenDocumentoRepository;
import com.resismart.backend.pagos.Entities.OrdenPago;
import com.resismart.backend.pagos.Enums.EstadoOrdenPago;
import com.resismart.backend.pagos.Repositories.OrdenPagoRepository;
import com.resismart.backend.pagos.Repositories.TransaccionPagoRepository;
import com.resismart.backend.users.Entities.Usuario;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MoraService {

    private final OrdenPagoRepository ordenPagoRepository;
    private final TransaccionPagoRepository transaccionPagoRepository;
    private final AvisoService avisoService;
    private final EmailService emailService;
    private final OrdenDocumentoRepository ordenDocumentoRepository;

    @Value("${resismart.mora.tasaMensual:0.02}")
    private BigDecimal tasaMensual;

    @Transactional
    public void recalcularMora(OrdenPago orden) {
        if (orden == null || orden.getId() == null) {
            return;
        }

        if (tieneDocumentoPendiente(orden)) {
            log.debug("Saltando mora para orden {} por documentos pendientes", orden.getId());
            return;
        }

        LocalDate hoy = LocalDate.now();
        BigDecimal base = defaultMonto(orden.getMontoBase());
        orden.setImpuesto(BigDecimal.ZERO);
        BigDecimal totalBase = base;
        BigDecimal totalPagado = defaultMonto(transaccionPagoRepository.sumarMontosAprobadosPorOrden(orden.getId()));

        boolean forzarMoraPorAtraso = tieneTresPendientesPrevios(orden);
        boolean vencida = orden.getFechaVencimiento() != null && hoy.isAfter(orden.getFechaVencimiento());
        boolean debeCalcularMora = (vencida || forzarMoraPorAtraso)
                && totalBase.compareTo(BigDecimal.ZERO) > 0;

        BigDecimal moraCalculada = BigDecimal.ZERO;

        if (debeCalcularMora) {
            long diasAtraso;
            if (orden.getFechaVencimiento() != null) {
                diasAtraso = Math.max(1, ChronoUnit.DAYS.between(orden.getFechaVencimiento(), hoy));
            } else if (orden.getPeriodo() != null) {
                diasAtraso = Math.max(1, ChronoUnit.DAYS.between(orden.getPeriodo().withDayOfMonth(1), hoy));
            } else {
                diasAtraso = 30; // fallback conservador
            }
            BigDecimal tasaDiaria = tasaMensual
                    .divide(BigDecimal.valueOf(30), 6, RoundingMode.HALF_UP);
            moraCalculada = totalBase
                    .multiply(tasaDiaria)
                    .multiply(BigDecimal.valueOf(diasAtraso))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal totalConMora = totalBase.add(moraCalculada);
        BigDecimal saldo = totalConMora.subtract(totalPagado);
        if (saldo.compareTo(BigDecimal.ZERO) < 0) {
            saldo = BigDecimal.ZERO;
        }

        orden.setMoraAcumulada(moraCalculada);
        orden.setSaldoPendiente(saldo);

        if (saldo.compareTo(BigDecimal.ZERO) <= 0) {
            orden.setEstado(EstadoOrdenPago.PAGADA);
            orden.setMoraAcumulada(BigDecimal.ZERO);
        } else if (forzarMoraPorAtraso || (orden.getFechaVencimiento() != null && hoy.isAfter(orden.getFechaVencimiento()))) {
            orden.setEstado(EstadoOrdenPago.EN_MORA);
        } else {
            orden.setEstado(EstadoOrdenPago.PENDIENTE);
        }
    }

    @Transactional
    public void aplicarMoraDiaria() {
        LocalDate hoy = LocalDate.now();
        List<EstadoOrdenPago> estados = List.of(
                EstadoOrdenPago.PENDIENTE,
                EstadoOrdenPago.VENCIDA,
                EstadoOrdenPago.EN_MORA
        );

        List<OrdenPago> ordenes = ordenPagoRepository
                .findByEstadoInAndSaldoPendienteGreaterThan(
                        estados, BigDecimal.ZERO);

        for (OrdenPago orden : ordenes) {
            EstadoOrdenPago anterior = orden.getEstado();
            try {
                if (tieneDocumentoPendiente(orden)) {
                    log.debug("Saltando mora diaria para orden {} por documentos pendientes", orden.getId());
                    continue;
                }
                recalcularMora(orden);
                ordenPagoRepository.save(orden);
                if (anterior != EstadoOrdenPago.EN_MORA && orden.getEstado() == EstadoOrdenPago.EN_MORA) {
                    notificarEntradaMora(orden);
                }
            } catch (Exception e) {
                log.warn("No se pudo recalcular mora para orden {}: {}", orden.getId(), e.getMessage());
            }
        }
    }

    private void notificarEntradaMora(OrdenPago orden) {
        var contrato = orden.getContrato();
        var residente = contrato != null ? contrato.getResidente() : null;
        Usuario usuario = residente != null ? residente.getUsuario() : null;
        Integer usuarioId = usuario != null ? usuario.getId_usuario() : null;

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("ordenId", orden.getId());
        metadata.put("periodo", orden.getPeriodo());
        metadata.put("contratoId", contrato != null ? contrato.getId() : null);
        metadata.put("mora", orden.getMoraAcumulada());
        metadata.put("saldoPendiente", orden.getSaldoPendiente());

        String periodoTexto = orden.getPeriodo() != null ? YearMonth.from(orden.getPeriodo()).toString() : "#"+orden.getId();
        String mensaje = String.format(
                "Tu orden de pago %s ha entrado en mora. Mora acumulada: %s.",
                periodoTexto,
                orden.getMoraAcumulada()
        );

        if (usuarioId != null) {
            avisoService.enviarAvisoUsuario(
                    usuarioId,
                    AvisoTipo.ALERTA_GENERAL,
                    AvisoTipo.ALERTA_GENERAL.getTituloDefecto(),
                    mensaje,
                    metadata
            );
        }
        if (usuario != null) {
            emailService.enviarAvisoOrdenPagoEnMora(usuario, orden);
        }
    }

    private BigDecimal defaultMonto(BigDecimal valor) {
        return valor != null ? valor : BigDecimal.ZERO;
    }

    private boolean tieneDocumentoPendiente(OrdenPago orden) {
        List<OrdenDocumento> docs = ordenDocumentoRepository.findByOrdenId(orden.getId());
        return docs.stream().anyMatch(rel -> rel.getDocumento() != null
                && EstadoValidacion.PENDIENTE.equals(rel.getDocumento().getEstadoValidacion()));
    }

    private boolean tieneTresPendientesPrevios(OrdenPago orden) {
        if (orden.getContrato() == null || orden.getContrato().getId() == null) return false;
        List<OrdenPago> todas = ordenPagoRepository.findByContrato_Id(orden.getContrato().getId());
        LocalDate periodoActual = orden.getPeriodo();
        long countPreviasNoPagadas = todas.stream()
                .filter(op -> !EstadoOrdenPago.PAGADA.equals(op.getEstado()))
                .filter(op -> op.getId() != null && !op.getId().equals(orden.getId()))
                .filter(op -> periodoActual == null || (op.getPeriodo() != null && op.getPeriodo().isBefore(periodoActual)))
                .count();
        return countPreviasNoPagadas >= 3;
    }
}
