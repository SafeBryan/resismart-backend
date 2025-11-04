package com.resismart.backend.pagos.Controllers;

import com.resismart.backend.pagos.DTO.GeneracionMensualResponseDTO;
import com.resismart.backend.pagos.DTO.OrdenPagoResumenDTO;
import com.resismart.backend.pagos.Enums.EstadoOrdenPago;
import com.resismart.backend.pagos.Services.OrdenPagoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Controlador REST para la gestión de Órdenes de Pago.
 *
 * Endpoints:
 *  - GET  /OrdenesPago/contrato/{idContrato}     : listar órdenes por contrato (existente)
 *  - GET  /OrdenesPago                           : listado general con filtros + paginación (nuevo)
 *  - GET  /OrdenesPago/resumen                   : KPIs por estado (nuevo)
 *  - GET  /OrdenesPago/ingresos-mensuales        : serie de ingresos mensuales (nuevo)
 *  - POST /OrdenesPago/generar                   : generar órdenes por mes (existente)
 *  - POST /OrdenesPago/{id}/pagar                : marcar orden como pagada (existente)
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/OrdenesPago")
public class OrdenPagoController {

    private final OrdenPagoService service;

    // ===========================
    // Endpoints de consulta (existente)
    // ===========================

    /** Lista todas las órdenes de pago asociadas a un contrato. */
    @GetMapping("/contrato/{idContrato}")
    public ResponseEntity<List<OrdenPagoResumenDTO>> listar(@PathVariable Integer idContrato) {
        return ResponseEntity.ok(service.listarPorContrato(idContrato));
    }

    // ===========================
    // NUEVO: listado general con filtros + paginación (+modo flat)
    // ===========================

    /**
     * Listado general de órdenes con filtros y paginación.
     *
     * Query params:
     * - contratoId (opcional)
     * - estado (opcional) -> PENDIENTE|PAGADA|VENCIDA|EN_MORA
     * - from, to (opcional) -> ISO-8601 (YYYY-MM-DD) sobre fechaEmision
     * - page, size (paginación)
     * - sortBy (id|fechaEmision|fechaVencimiento|periodo), sortDir (ASC|DESC)
     * - flat=true -> devuelve solo el array y X-Total-Count en header
     */
    @GetMapping
    public ResponseEntity<?> listarGeneral(
            @RequestParam(required = false) Integer contratoId,
            @RequestParam(required = false) EstadoOrdenPago estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "fechaEmision") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir,
            @RequestParam(defaultValue = "false") boolean flat
    ) {
        Sort.Direction dir = "ASC".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;

        Page<OrdenPagoResumenDTO> result = service.listarFiltrado(
                contratoId, estado, from, to, page, size, sortBy, dir
        );

        if (flat) {
            return ResponseEntity.ok()
                    .header("X-Total-Count", String.valueOf(result.getTotalElements()))
                    .body(result.getContent());
        }

        return ResponseEntity.ok(PageResponse.of(result));
    }

    // ===========================
    // NUEVO: KPIs por estado
    // ===========================

    /**
     * Resumen por estado (KPIs) aplicando filtros contratoId + rango de fechas (fechaEmision).
     * Respuesta: { "PAGADA": 10, "PENDIENTE": 5, ... }
     */
    @GetMapping("/resumen")
    public ResponseEntity<Map<String, Long>> resumen(
            @RequestParam(required = false) Integer contratoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(service.resumenPorEstado(contratoId, from, to));
    }

    // ===========================
    // NUEVO: Ingresos mensuales
    // ===========================

    /**
     * Serie de ingresos mensuales (sumatoria de órdenes PAGADAS), agrupada por YearMonth.
     * Respuesta: [ { "mes": "2025-01", "montoTotal": 12345.67 }, ... ]
     */
    @GetMapping("/ingresos-mensuales")
    public ResponseEntity<List<OrdenPagoService.IngresoMensualDTO>> ingresosMensuales(
            @RequestParam(required = false) Integer contratoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(service.ingresosMensuales(contratoId, from, to));
    }

    // ===========================
    // Endpoints de negocio (existentes)
    // ===========================

    /** Genera órdenes de pago para un mes dado. */
    @PostMapping("/generar")
    public ResponseEntity<?> generarParaMes(@RequestParam int anio, @RequestParam int mes) {
        try {
            GeneracionMensualResponseDTO r = service.generarParaMes(anio, mes);
            return ResponseEntity.ok(r);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /** Marca una orden de pago como pagada. */
    @PostMapping("/{id}/pagar")
    public ResponseEntity<?> pagar(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(service.marcarPagada(id));
        } catch (java.util.NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    // ===========================
    // DTO de respuesta paginada (local)
    // ===========================

    public record PageResponse<T>(
            int page, int size, long total, int totalPages, boolean first, boolean last, List<T> content
    ) {
        public static <T> PageResponse<T> of(Page<T> p) {
            return new PageResponse<>(
                    p.getNumber(),
                    p.getSize(),
                    p.getTotalElements(),
                    p.getTotalPages(),
                    p.isFirst(),
                    p.isLast(),
                    p.getContent()
            );
        }
    }
}
