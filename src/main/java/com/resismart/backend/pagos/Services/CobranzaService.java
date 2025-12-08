package com.resismart.backend.pagos.Services;

import com.resismart.backend.pagos.Entities.OrdenPago;
import com.resismart.backend.pagos.Enums.EstadoOrdenPago;
import com.resismart.backend.pagos.Repositories.OrdenPagoRepository;
import com.resismart.backend.avisos.Enums.AvisoTipo;
import com.resismart.backend.avisos.Services.AvisoService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CobranzaService {

    private final OrdenPagoRepository ordenPagoRepository;
    private final AvisoService avisoService;

    @Value("${app.cobranza.dias-gracia-mora:15}")
    private int diasGraciaMora;

    @Transactional
    public void actualizarOrdenesVencidas(LocalDate hoy) {
        List<OrdenPago> pendientes = ordenPagoRepository.findByEstadoAndFechaVencimientoBefore(EstadoOrdenPago.PENDIENTE, hoy);
        pendientes.forEach(op -> op.setEstado(EstadoOrdenPago.VENCIDA));
    }

    @Transactional
    public void actualizarOrdenesEnMora(LocalDate hoy) {
        List<OrdenPago> vencidas = ordenPagoRepository.findByEstado(EstadoOrdenPago.VENCIDA);
        vencidas.stream()
                .filter(op -> op.getFechaVencimiento() != null && op.getFechaVencimiento().plusDays(diasGraciaMora).isBefore(hoy))
                .forEach(op -> op.setEstado(EstadoOrdenPago.EN_MORA));
    }

    @Transactional
    public void actualizarEstadosOrdenes(LocalDate hoy) {
        actualizarOrdenesVencidas(hoy);
        actualizarOrdenesEnMora(hoy);
    }

    @Transactional
    public void notificarOrdenesProximasAVencer(LocalDate hoy, int diasAviso) {
        LocalDate hasta = hoy.plusDays(diasAviso);
        List<OrdenPago> pendientes = ordenPagoRepository.buscarPendientesConVencimientoEntre(hoy, hasta);
        pendientes.forEach(this::emitirAvisoProximoVencer);
    }

    @Transactional
    @Scheduled(cron = "0 0 3 * * *")
    public void schedulerActualizarEstados() {
        actualizarEstadosOrdenes(LocalDate.now());
    }

    private void emitirAvisoProximoVencer(OrdenPago orden) {
        if (orden == null || orden.getContrato() == null || orden.getContrato().getResidente() == null) return;
        var residente = orden.getContrato().getResidente();
        if (residente.getUsuario() == null) return;
        Integer usuarioId = residente.getUsuario().getId_usuario();
        if (usuarioId == null) return;
        var metadata = new java.util.HashMap<String, Object>();
        metadata.put("ordenId", orden.getId());
        metadata.put("contratoId", orden.getContrato().getId());
        metadata.put("fechaVencimiento", orden.getFechaVencimiento());
        metadata.put("saldoPendiente", orden.getSaldoPendiente());
        avisoService.enviarAvisoUsuario(
                usuarioId,
                AvisoTipo.ORDEN_PAGO_PROX_VENCER,
                AvisoTipo.ORDEN_PAGO_PROX_VENCER.getTituloDefecto(),
                "Tu orden de pago vence el " + orden.getFechaVencimiento(),
                metadata
        );
    }
}
