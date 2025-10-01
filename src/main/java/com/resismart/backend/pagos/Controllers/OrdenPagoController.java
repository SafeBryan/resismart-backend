package com.resismart.backend.pagos.Controllers;

import com.resismart.backend.pagos.DTO.GeneracionMensualResponseDTO;
import com.resismart.backend.pagos.DTO.OrdenPagoResumenDTO;
import com.resismart.backend.pagos.Services.OrdenPagoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador REST para la gestión de Órdenes de Pago.
 * <p>
 * Expone endpoints para:
 * <ul>
 *   <li>Listar órdenes de pago asociadas a un contrato específico.</li>
 *   <li>Generar automáticamente órdenes de pago mensuales para contratos activos.</li>
 *   <li>Marcar órdenes de pago como pagadas.</li>
 * </ul>
 *
 * <h3>Convenciones</h3>
 * <ul>
 *   <li>Prefijo de ruta: <code>/OrdenesPago</code>.</li>
 *   <li>Respuestas normalizadas con {@link ResponseEntity}.</li>
 *   <li>Manejo explícito de errores: <code>400 Bad Request</code> para validaciones,
 *       <code>404 Not Found</code> si no existe el recurso,
 *       y <code>500 Internal Server Error</code> para errores inesperados.</li>
 * </ul>
 *
 * @since 1.0
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/OrdenesPago")
public class OrdenPagoController {

    private final OrdenPagoService service;

    // ===========================
    // Endpoints de consulta
    // ===========================

    /**
     * Lista todas las órdenes de pago asociadas a un contrato.
     *
     * @param idContrato identificador único del contrato.
     * @return lista de {@link OrdenPagoResumenDTO}, posiblemente vacía.
     */
    @GetMapping("/contrato/{idContrato}")
    public ResponseEntity<List<OrdenPagoResumenDTO>> listar(@PathVariable Integer idContrato) {
        return ResponseEntity.ok(service.listarPorContrato(idContrato));
    }

    // ===========================
    // Endpoints de negocio
    // ===========================

    /**
     * Genera órdenes de pago para todos los contratos activos y vigentes en un mes específico.
     * <p>
     * Parámetros obligatorios:
     * <ul>
     *   <li>{@code anio}: año objetivo (ej. 2025).</li>
     *   <li>{@code mes}: mes objetivo (1–12).</li>
     * </ul>
     *
     * @param anio año del período a generar.
     * @param mes  mes del período a generar.
     * @return {@link GeneracionMensualResponseDTO} con conteo de órdenes creadas y ya existentes.
     */
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

    /**
     * Marca una orden de pago como pagada.
     *
     * @param id identificador único de la orden de pago.
     * @return {@link OrdenPagoResumenDTO} con el estado actualizado.
     */
    @PostMapping("/{id}/pagar")
    public ResponseEntity<?> pagar(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(service.marcarPagada(id));
        } catch (java.util.NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }
}
