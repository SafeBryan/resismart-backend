package com.resismart.backend.pagos.Services;

import com.resismart.backend.Common.MensajeError;
import com.resismart.backend.contratos.Entities.Contrato;
import com.resismart.backend.contratos.Enums.EstadoContrato;
import com.resismart.backend.contratos.Repositories.ContratoRepository;
import com.resismart.backend.pagos.DTO.GeneracionMensualResponseDTO;
import com.resismart.backend.pagos.DTO.OrdenPagoResumenDTO;
import com.resismart.backend.pagos.Entities.OrdenPago;
import com.resismart.backend.pagos.Enums.EstadoOrdenPago;
import com.resismart.backend.pagos.Repositories.OrdenPagoRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio de aplicación para la gestión de Órdenes de Pago.
 * <p>
 * Nuevos features para dashboards:
 * <ul>
 *   <li>Listado general con filtros (+paginación/orden): {@link #listarFiltrado(Integer, EstadoOrdenPago, LocalDate, LocalDate, int, int, String, Sort.Direction)}</li>
 *   <li>Resumen por estado (KPIs): {@link #resumenPorEstado(Integer, LocalDate, LocalDate)}</li>
 *   <li>Ingresos mensuales (sumatoria de PAGADAS): {@link #ingresosMensuales(Integer, LocalDate, LocalDate)}</li>
 * </ul>
 * Mantiene métodos existentes: listar por contrato, generar mensual, marcar pagada.
 */
@Service
@RequiredArgsConstructor
public class OrdenPagoService {

    private final OrdenPagoRepository ordenRepo;
    private final ContratoRepository contratoRepo;

    // ===========================
    // Mapeador interno
    // ===========================

    private OrdenPagoResumenDTO toResumen(OrdenPago op) {
        return new OrdenPagoResumenDTO(
                op.getId(),
                op.getContrato().getId(),
                op.getPeriodo(),
                op.getMonto(),
                op.getEstado(),
                op.getFechaEmision(),
                op.getFechaVencimiento()
        );
    }

    // ===========================
    // EXISTENTE: Casos de uso
    // ===========================

    /** Lista todas las órdenes de pago asociadas a un contrato. */
    public List<OrdenPagoResumenDTO> listarPorContrato(Integer idContrato) {
        return ordenRepo.findByContrato_Id(idContrato).stream().map(this::toResumen).toList();
    }

    /** Genera órdenes para contratos activos vigentes en el mes indicado. */
    @Transactional
    public GeneracionMensualResponseDTO generarParaMes(int anio, int mes) {
        YearMonth ym = YearMonth.of(anio, mes);
        LocalDate inicioMes = ym.atDay(1);
        LocalDate finMes = ym.atEndOfMonth();

        List<Contrato> contratos = contratoRepo.findActivosVigentesEn(
                EstadoContrato.ACTIVO, inicioMes, finMes);

        int creadas = 0, existentes = 0;
        for (Contrato c : contratos) {
            if (ordenRepo.existsByContrato_IdAndPeriodo(c.getId(), inicioMes)) {
                existentes++;
                continue;
            }

            OrdenPago op = OrdenPago.builder()
                    .contrato(c)
                    .periodo(inicioMes)
                    .fechaEmision(LocalDate.now())
                    .fechaVencimiento(ym.atDay(10))
                    .monto(c.getMonto())
                    .estado(EstadoOrdenPago.PENDIENTE)
                    .build();

            ordenRepo.save(op);
            creadas++;
        }
        return new GeneracionMensualResponseDTO(creadas, existentes);
    }

    /** Marca una orden de pago como PAGADA. */
    @Transactional
    public OrdenPagoResumenDTO marcarPagada(Integer id) {
        OrdenPago op = ordenRepo.findById(id)
                .orElseThrow(() -> new java.util.NoSuchElementException(MensajeError.PAGO_NO_ENCONTRADO.getMensaje()));
        op.setEstado(EstadoOrdenPago.PAGADA);
        return toResumen(op);
    }

    // ===========================
    // NUEVO: Listado general con filtros (para dashboards)
    // ===========================

    /**
     * Listado general con filtros (contratoId, estado, rango por fechaEmision) + paginación/orden.
     * Útil para poblar tablas del dashboard sin fan-out.
     */
    public Page<OrdenPagoResumenDTO> listarFiltrado(
            @Nullable Integer contratoId,
            @Nullable EstadoOrdenPago estado,
            @Nullable LocalDate from,
            @Nullable LocalDate to,
            int page, int size,
            String sortBy, Sort.Direction dir
    ) {
        size = Math.max(1, Math.min(200, size));

        // Campos seguros para ordenar (evitamos injection)
        List<String> camposPermitidos = List.of("fechaEmision", "fechaVencimiento", "periodo", "id");
        if (!camposPermitidos.contains(sortBy)) sortBy = "fechaEmision";

        Pageable pageable = PageRequest.of(page, size, Sort.by(dir, sortBy));

        Specification<OrdenPago> spec = Specification
                .where(contratoEq(contratoId))
                .and(estadoEq(estado))
                .and(fechaEmisionDesde(from))
                .and(fechaEmisionHasta(to));

        Page<OrdenPago> pageEnt = ordenRepo.findAll(spec, pageable);
        return pageEnt.map(this::toResumen);
    }

    // ===========================
    // NUEVO: KPIs / Resumen por estado
    // ===========================

    /**
     * Devuelve un mapa {ESTADO -> cantidad} aplicando filtros (contratoId, rango fechas por fechaEmision).
     * Ideal para KPIs rápidos en el dashboard.
     */
    public Map<String, Long> resumenPorEstado(
            @Nullable Integer contratoId,
            @Nullable LocalDate from,
            @Nullable LocalDate to
    ) {
        Specification<OrdenPago> spec = Specification
                .where(contratoEq(contratoId))
                .and(fechaEmisionDesde(from))
                .and(fechaEmisionHasta(to));

        return ordenRepo.findAll(spec).stream()
                .collect(Collectors.groupingBy(op -> op.getEstado().name(), Collectors.counting()));
    }

    // ===========================
    // NUEVO: Ingresos mensuales (sumatoria de PAGADAS)
    // ===========================

    /** DTO simple para ingresos mensuales. */
    public record IngresoMensualDTO(YearMonth mes, BigDecimal montoTotal) {}

    /**
     * Suma montos de órdenes PAGADA agrupadas por YearMonth (usando periodo o fechaEmision).
     * Por defecto usamos {@code periodo} para el agrupamiento mensual (coincide con tu modelo).
     */
    public List<IngresoMensualDTO> ingresosMensuales(
            @Nullable Integer contratoId,
            @Nullable LocalDate from,
            @Nullable LocalDate to
    ) {
        Specification<OrdenPago> spec = Specification
                .where(contratoEq(contratoId))
                .and(estadoEq(EstadoOrdenPago.PAGADA))
                .and(fechaEmisionDesde(from))
                .and(fechaEmisionHasta(to));

        return ordenRepo.findAll(spec).stream()
                .collect(Collectors.groupingBy(
                        op -> YearMonth.from(op.getPeriodo() != null ? op.getPeriodo() : op.getFechaEmision()),
                        Collectors.mapping(OrdenPago::getMonto,
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
                ))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey()) // orden por mes asc
                .map(e -> new IngresoMensualDTO(e.getKey(), e.getValue()))
                .toList();
    }

    // ===========================
    // Specifications helpers
    // ===========================

    private Specification<OrdenPago> contratoEq(@Nullable Integer contratoId) {
        return (root, cq, cb) -> contratoId == null ? null : cb.equal(root.get("contrato").get("id"), contratoId);
    }

    private Specification<OrdenPago> estadoEq(@Nullable EstadoOrdenPago estado) {
        return (root, cq, cb) -> estado == null ? null : cb.equal(root.get("estado"), estado);
    }

    private Specification<OrdenPago> fechaEmisionDesde(@Nullable LocalDate from) {
        return (root, cq, cb) -> from == null ? null : cb.greaterThanOrEqualTo(root.get("fechaEmision"), from);
    }

    private Specification<OrdenPago> fechaEmisionHasta(@Nullable LocalDate to) {
        return (root, cq, cb) -> to == null ? null : cb.lessThanOrEqualTo(root.get("fechaEmision"), to);
    }
}
