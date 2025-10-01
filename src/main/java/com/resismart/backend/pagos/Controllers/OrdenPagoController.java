package com.resismart.backend.pagos.Controllers;

import com.resismart.backend.pagos.DTO.GeneracionMensualResponseDTO;
import com.resismart.backend.pagos.DTO.OrdenPagoResumenDTO;
import com.resismart.backend.pagos.Services.OrdenPagoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/OrdenesPago")
public class OrdenPagoController {

    private final OrdenPagoService service;

    @GetMapping("/contrato/{idContrato}")
    public ResponseEntity<List<OrdenPagoResumenDTO>> listar(@PathVariable Integer idContrato) {
        return ResponseEntity.ok(service.listarPorContrato(idContrato));
    }

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

    @PostMapping("/{id}/pagar")
    public ResponseEntity<?> pagar(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(service.marcarPagada(id));
        } catch (java.util.NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }
}
