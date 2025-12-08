package com.resismart.backend.pagos.Controllers;

import com.resismart.backend.pagos.DTO.GeneracionMensualResponseDTO;
import com.resismart.backend.pagos.DTO.OrdenPagoDetalleDTO;
import com.resismart.backend.pagos.DTO.OrdenPagoResumenDTO;
import com.resismart.backend.pagos.Enums.EstadoOrdenPago;
import com.resismart.backend.pagos.Services.CobranzaService;
import com.resismart.backend.pagos.Services.FacturacionService;
import com.resismart.backend.pagos.Services.PagoService;
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

@RestController
@RequiredArgsConstructor
@RequestMapping("/OrdenesPago")
public class OrdenPagoController {

    private final OrdenPagoService service;
    private final PagoService pagoService;
    private final FacturacionService facturacionService;
    private final CobranzaService cobranzaService;

    // ===========================
    // Endpoints de consulta (existente)
    // ===========================

    /** Lista todas las órdenes de pago asociadas a un contrato. */
    @GetMapping("/contrato/{idContrato}")
    public ResponseEntity<List<OrdenPagoResumenDTO>> listar(@PathVariable Integer idContrato) {
        return ResponseEntity.ok(service.listarPorContrato(idContrato));
    }

    /** Detalle de una orden de pago, incluyendo transacciones asociadas. */
    @GetMapping("/{id}")
    public ResponseEntity<OrdenPagoDetalleDTO> detalle(@PathVariable Integer id) {
        return ResponseEntity.ok(service.detalle(id));
    }

    // ===========================
    // NUEVO: listado general con filtros + paginación (+modo flat)
    // ===========================

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
            var resultado = facturacionService.generarOrdenesParaMes(anio, mes);
            facturacionService.notificarOrdenesGeneradas(resultado.creadas());
            return ResponseEntity.ok(Map.of(
                    "mensaje", "Ordenes generadas para " + anio + "-" + mes,
                    "creadas", resultado.creadas() != null ? resultado.creadas().size() : 0,
                    "existentes", resultado.existentes()
            ));
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
            pagoService.pagarEnVentanilla(id, null);
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
