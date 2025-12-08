package com.resismart.backend.contratos.Services;

import com.resismart.backend.contratos.Entities.Contrato;
import com.resismart.backend.contratos.Enums.EstadoContrato;
import com.resismart.backend.contratos.Repositories.ContratoRepository;
import com.resismart.backend.condominios.Enums.UnidadEstado;
import com.resismart.backend.condominios.Repositories.UnidadRepository;
import com.resismart.backend.pagos.Entities.OrdenPago;
import com.resismart.backend.pagos.Enums.EstadoOrdenPago;
import com.resismart.backend.pagos.Repositories.OrdenPagoRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ContratoLifecycleService {

    private final ContratoRepository contratoRepository;
    private final OrdenPagoRepository ordenPagoRepository;
    private final UnidadRepository unidadRepository;

    @Transactional
    public void actualizarEstadosContratos(LocalDate hoy) {
        // Activar pendientes que ya iniciaron
        contratoRepository.findByEstado(EstadoContrato.PENDIENTE).forEach(c -> {
            if (c.getFechaInicio() != null && !c.getFechaInicio().isAfter(hoy)) {
                c.setEstado(EstadoContrato.ACTIVO);
                c.getUnidad().setEstado(UnidadEstado.OCUPADA);
            }
        });

        // Finalizar contratos vencidos
        contratoRepository.findByEstado(EstadoContrato.ACTIVO).forEach(c -> {
            if (c.getFechaFin() != null && c.getFechaFin().isBefore(hoy)) {
                finalizarContrato(c);
            }
        });
    }

    @Transactional
    public Contrato renovarContrato(Long idContrato, LocalDate nuevaFechaFin) {
        Contrato contrato = contratoRepository.findById(idContrato.intValue())
                .orElseThrow(() -> new java.util.NoSuchElementException("Contrato no encontrado"));
        if (contrato.getEstado() == EstadoContrato.RESCINDIDO) {
            throw new IllegalStateException("No se puede renovar un contrato rescindido");
        }
        if (contrato.getFechaFin() != null && !nuevaFechaFin.isAfter(contrato.getFechaFin())) {
            throw new IllegalArgumentException("La nueva fecha de fin debe ser posterior a la actual");
        }
        contrato.setFechaFin(nuevaFechaFin);
        if (!contrato.getFechaInicio().isAfter(LocalDate.now()) && !nuevaFechaFin.isBefore(LocalDate.now())) {
            contrato.setEstado(EstadoContrato.ACTIVO);
            contrato.getUnidad().setEstado(UnidadEstado.OCUPADA);
        }
        return contrato;
    }

    @Transactional
    public Contrato rescindirContrato(Long idContrato, String motivo) {
        Contrato contrato = contratoRepository.findById(idContrato.intValue())
                .orElseThrow(() -> new java.util.NoSuchElementException("Contrato no encontrado"));
        contrato.setEstado(EstadoContrato.RESCINDIDO);
        contrato.setFechaFin(LocalDate.now());
        liberarUnidadSiCorresponde(contrato);
        return contrato;
    }

    private void finalizarContrato(Contrato contrato) {
        List<OrdenPago> ordenes = ordenPagoRepository.findByContrato_Id(contrato.getId());
        boolean todasPagadas = ordenes.stream().allMatch(o -> o.getEstado() == EstadoOrdenPago.PAGADA);
        // Siempre pasa a FINALIZADO, las deudas quedan reflejadas en las órdenes
        contrato.setEstado(EstadoContrato.FINALIZADO);
        if (todasPagadas) {
            contrato.setEstado(EstadoContrato.FINALIZADO);
        }
        liberarUnidadSiCorresponde(contrato);
    }

    private void liberarUnidadSiCorresponde(Contrato contrato) {
        Integer idUnidad = contrato.getUnidad().getId();
        boolean hayOtroActivo = contratoRepository.existsByUnidad_IdAndEstado(idUnidad, EstadoContrato.ACTIVO);
        if (!hayOtroActivo) {
            unidadRepository.findById(idUnidad).ifPresent(u -> u.setEstado(UnidadEstado.LIBRE));
        }
    }
}
