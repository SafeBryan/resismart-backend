package com.resismart.backend.pagos.Services;

import com.resismart.backend.Common.MensajeError;
import com.resismart.backend.avisos.Enums.AvisoTipo;
import com.resismart.backend.avisos.Services.AvisoService;
import com.resismart.backend.contratos.Entities.Contrato;
import com.resismart.backend.contratos.Enums.EstadoContrato;
import com.resismart.backend.contratos.Repositories.ContratoRepository;
import com.resismart.backend.pagos.DTO.GeneracionMensualResponseDTO;
import com.resismart.backend.pagos.DTO.OrdenPagoDetalleDTO;
import com.resismart.backend.pagos.DTO.OrdenPagoResumenDTO;
import com.resismart.backend.pagos.Entities.OrdenPago;
import com.resismart.backend.pagos.Enums.EstadoOrdenPago;
import com.resismart.backend.pagos.Enums.EstadoTransaccion;
import com.resismart.backend.pagos.Repositories.OrdenPagoRepository;
import com.resismart.backend.pagos.Repositories.TransaccionPagoRepository;
import com.resismart.backend.pagos.Entities.TransaccionPago;
import com.resismart.backend.users.Entities.Usuario;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
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
    private final TransaccionPagoRepository transaccionRepo;
    private final AvisoService avisoService;
    private final MoraService moraService;
    @Value("${app.facturacion.dia-vencimiento:10}")
    private int diaVencimiento;
    private static final DateTimeFormatter FECHA_CORTA_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ===========================
    // Mapeador interno
    // ===========================

    private OrdenPagoResumenDTO toResumen(OrdenPago op) {
        try {
            moraService.recalcularMora(op);
            ordenRepo.save(op);
        } catch (Exception e) {
            // evite romper el listado; log en debug
        }
        var ultimaTx = transaccionRepo.findTop1ByOrdenPago_IdOrderByFechaPagoDesc(op.getId());
        return new OrdenPagoResumenDTO(
                op.getId(),
                op.getContrato().getId(),
                op.getPeriodo(),
                op.getMontoBase(),
                op.getMoraAcumulada(),
                op.getSaldoPendiente(),
                op.getEstado(),
                op.getFechaEmision(),
                op.getFechaVencimiento(),
                ultimaTx.map(t -> t.getEstado()).orElse(null),
                ultimaTx.map(t -> t.getId()).orElse(null),
                ultimaTx.map(t -> t.getFechaPago()).orElse(null),
                ultimaTx.map(t -> t.getMonto()).orElse(null)
        );
    }

    // ===========================
    // EXISTENTE: Casos de uso
    // ===========================

    /** Lista todas las órdenes de pago asociadas a un contrato. */
    public List<OrdenPagoResumenDTO> listarPorContrato(Integer idContrato) {
        return ordenRepo.findByContrato_Id(idContrato).stream().map(this::toResumen).toList();
    }

    /** Resultado detallado de la generacion mensual. */
    public record GeneracionMensualResultado(List<OrdenPago> creadas, int existentes) {}

    /** Genera ordenes para contratos activos vigentes en el mes indicado. */
    @Transactional
    public GeneracionMensualResponseDTO generarParaMes(int anio, int mes) {
        GeneracionMensualResultado resultado = generarParaMesConDetalle(anio, mes);
        return new GeneracionMensualResponseDTO(
                resultado.creadas() != null ? resultado.creadas().size() : 0,
                resultado.existentes()
        );
    }

    /** Genera ordenes para contratos activos vigentes en el mes indicado y devuelve el detalle. */
    @Transactional
    public GeneracionMensualResultado generarParaMesConDetalle(int anio, int mes) {
        YearMonth ym = YearMonth.of(anio, mes);
        LocalDate inicioMes = ym.atDay(1);
        LocalDate finMes = ym.atEndOfMonth();

        List<Contrato> contratos = contratoRepo.findActivosVigentesEn(
                EstadoContrato.ACTIVO, inicioMes, finMes);

        int existentes = 0;
        List<OrdenPago> creadas = new ArrayList<>();
        for (Contrato c : contratos) {
            if (ordenRepo.existsByContrato_IdAndPeriodo(c.getId(), inicioMes)) {
                existentes++;
                continue;
            }

            BigDecimal montoBase = defaultMonto(c.getMonto());
            BigDecimal impuesto = BigDecimal.ZERO;
            BigDecimal saldo = montoBase;
            LocalDate vencimiento = LocalDate.of(anio, mes, Math.min(diaVencimiento, ym.lengthOfMonth()));

            OrdenPago op = OrdenPago.builder()
                    .contrato(c)
                    .periodo(inicioMes)
                    .fechaEmision(LocalDate.now())
                    .fechaVencimiento(vencimiento)
                    .montoBase(montoBase)
                    .impuesto(impuesto)
                    .moraAcumulada(BigDecimal.ZERO)
                    .saldoPendiente(saldo)
                    .estado(EstadoOrdenPago.PENDIENTE)
                    .build();

            creadas.add(ordenRepo.save(op));
        }
        return new GeneracionMensualResultado(creadas, existentes);
    }

    @Transactional
    public OrdenPagoDetalleDTO detalle(Integer id) {
        OrdenPago op = ordenRepo.findById(id).orElseThrow(() -> new java.util.NoSuchElementException("Orden no encontrada"));
        List<TransaccionPago> txs = transaccionRepo.findByOrdenPago_IdOrderByFechaPagoDesc(id);

        var contrato = op.getContrato();
        var residente = contrato != null ? contrato.getResidente() : null;
        var usuario = residente != null ? residente.getUsuario() : null;
        var unidad = contrato != null ? contrato.getUnidad() : null;
        var condominio = unidad != null ? unidad.getCondominio() : null;

            BigDecimal base = defaultMonto(op.getMontoBase());
            BigDecimal imp = BigDecimal.ZERO;
            BigDecimal mora = defaultMonto(op.getMoraAcumulada());
            BigDecimal total = base.add(mora);

        return OrdenPagoDetalleDTO.builder()
                .id(op.getId())
                .estado(op.getEstado())
                .periodo(op.getPeriodo())
                .fechaEmision(op.getFechaEmision())
                .fechaVencimiento(op.getFechaVencimiento())
                .montoBase(base)
                .impuesto(imp)
                .mora(mora)
                .saldoPendiente(defaultMonto(op.getSaldoPendiente()))
                .total(total)
                .contratoId(contrato != null ? contrato.getId() : null)
                .contratoCodigo(null)
                .inquilinoNombre(usuario != null ? (usuario.getNombres() + " " + usuario.getApellidos()).trim() : null)
                .inquilinoCorreo(usuario != null ? usuario.getCorreo() : null)
                .unidad(unidad != null ? unidad.getNumero() : null)
                .condominio(condominio != null ? condominio.getNombre() : null)
                .transacciones(txs)
                .build();
    }

    /**
     * Genera todas las órdenes pendientes hasta el mes objetivo (inclusive),
     * rellenando huecos si hay meses sin generar para contratos ACTIVO vigentes.
     */
    @Transactional
    public GeneracionMensualResultado generarPendientesHasta(YearMonth objetivo) {
        List<Contrato> contratos = contratoRepo.findAllByEstadoWithJoins(EstadoContrato.ACTIVO);
        List<OrdenPago> creadas = new ArrayList<>();
        int existentes = 0;

        for (Contrato contrato : contratos) {
            if (contrato == null || contrato.getFechaInicio() == null) continue;

            YearMonth inicioContrato = YearMonth.from(contrato.getFechaInicio());
            YearMonth finContrato = contrato.getFechaFin() != null ? YearMonth.from(contrato.getFechaFin()) : null;
            if (objetivo.isBefore(inicioContrato)) {
                continue; // no generar periodos anteriores al inicio del contrato
            }

            YearMonth start = inicioContrato;
            var ultima = ordenRepo.findTop1ByContrato_IdOrderByPeriodoDesc(contrato.getId());
            if (ultima.isPresent() && ultima.get().getPeriodo() != null) {
                start = YearMonth.from(ultima.get().getPeriodo()).plusMonths(1);
            }
            if (start.isBefore(inicioContrato)) start = inicioContrato;
            YearMonth end = finContrato != null && finContrato.isBefore(objetivo) ? finContrato : objetivo;
            if (start.isAfter(end)) continue;

            YearMonth cursor = start;
            while (!cursor.isAfter(end)) {
                LocalDate periodo = cursor.atDay(1);
                LocalDate finMes = cursor.atEndOfMonth();
                boolean vigente = contrato.getFechaInicio().isBefore(finMes.plusDays(1)) &&
                        (contrato.getFechaFin() == null || !contrato.getFechaFin().isBefore(periodo));
                if (!vigente) {
                    cursor = cursor.plusMonths(1);
                    continue;
                }

                if (ordenRepo.existsByContrato_IdAndPeriodo(contrato.getId(), periodo)) {
                    existentes++;
                    cursor = cursor.plusMonths(1);
                    continue;
                }

                BigDecimal montoBase = defaultMonto(contrato.getMonto());
                BigDecimal impuesto = BigDecimal.ZERO;
                BigDecimal saldo = montoBase;
                LocalDate vencimiento = LocalDate.of(cursor.getYear(), cursor.getMonthValue(),
                        Math.min(diaVencimiento, cursor.lengthOfMonth()));

                OrdenPago op = OrdenPago.builder()
                        .contrato(contrato)
                        .periodo(periodo)
                        .fechaEmision(LocalDate.now())
                        .fechaVencimiento(vencimiento)
                        .montoBase(montoBase)
                        .impuesto(impuesto)
                        .moraAcumulada(BigDecimal.ZERO)
                        .saldoPendiente(saldo)
                        .estado(EstadoOrdenPago.PENDIENTE)
                        .build();
                creadas.add(ordenRepo.save(op));
                cursor = cursor.plusMonths(1);
            }
        }
        return new GeneracionMensualResultado(creadas, existentes);
    }


    /** Marca una orden de pago como PAGADA. */
    @Transactional
    public OrdenPagoResumenDTO marcarPagada(Integer id) {
        OrdenPago op = ordenRepo.findById(id)
                .orElseThrow(() -> new java.util.NoSuchElementException(MensajeError.PAGO_NO_ENCONTRADO.getMensaje()));
        op.setSaldoPendiente(BigDecimal.ZERO);
        op.setEstado(EstadoOrdenPago.PAGADA);
        emitirAvisoOrden(op, AvisoTipo.ORDEN_PAGO_PAGADA,
                String.format("El pago de la orden #%d fue registrado.", op.getId()));
        return toResumen(op);
    }

    private BigDecimal defaultMonto(BigDecimal valor) {
        return valor != null ? valor : BigDecimal.ZERO;
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
                        Collectors.mapping(OrdenPago::getMontoBase,
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

    private String formatearFecha(LocalDate fecha) {
        return fecha != null ? fecha.format(FECHA_CORTA_FMT) : "sin fecha";
    }

    private void emitirAvisoOrden(OrdenPago orden, AvisoTipo tipo, String mensaje) {
        if (orden == null) {
            return;
        }
        var metadata = new HashMap<String, Object>();
        metadata.put("ordenPagoId", orden.getId());
        metadata.put("periodo", orden.getPeriodo());
        metadata.put("fechaEmision", orden.getFechaEmision());
        metadata.put("fechaVencimiento", orden.getFechaVencimiento());
        metadata.put("estado", orden.getEstado());
        metadata.put("monto", orden.getMontoBase());

        var contrato = orden.getContrato();
        if (contrato != null) {
            metadata.put("contratoId", contrato.getId());
            if (contrato.getResidente() != null && contrato.getResidente().getUsuario() != null) {
                Integer usuarioId = contrato.getResidente().getUsuario().getId_usuario();
                metadata.put("usuarioId", usuarioId);
                avisoService.enviarAvisoUsuario(
                        usuarioId,
                        tipo,
                        tipo.getTituloDefecto(),
                        mensaje,
                        metadata
                );
            }
            if (contrato.getUnidad() != null && contrato.getUnidad().getCondominio() != null) {
                Integer condominioId = contrato.getUnidad().getCondominio().getId();
                metadata.put("condominioId", condominioId);
                avisoService.enviarAvisoCondominio(
                        condominioId,
                        tipo,
                        tipo.getTituloDefecto(),
                        mensaje,
                        metadata
                );
            }
        } else {
            avisoService.enviarAvisoBroadcast(
                    tipo,
                    tipo.getTituloDefecto(),
                    mensaje,
                    metadata
            );
        }
    }
}
