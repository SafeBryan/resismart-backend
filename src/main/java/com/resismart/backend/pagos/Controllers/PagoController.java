package com.resismart.backend.pagos.Controllers;

import com.resismart.backend.pagos.DTO.TransaccionAprobarDTO;
import com.resismart.backend.pagos.DTO.TransaccionPagoCreateDTO;
import com.resismart.backend.pagos.DTO.TransaccionRechazarDTO;
import com.resismart.backend.pagos.Entities.TransaccionPago;
import com.resismart.backend.pagos.Enums.EstadoTransaccion;
import com.resismart.backend.pagos.Services.PagoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/pagos")
@RequiredArgsConstructor
public class PagoController {

    private final PagoService pagoService;

    @GetMapping("/transaccion")
    public ResponseEntity<List<TransaccionPago>> listar(@RequestParam(required = false) EstadoTransaccion estado,
                                                        @RequestParam(required = false) Integer ordenId) {
        return ResponseEntity.ok(pagoService.listarTransacciones(estado, ordenId));
    }

    @PostMapping(value = "/transaccion", consumes = "multipart/form-data")
    public ResponseEntity<?> registrar(@ModelAttribute @Valid TransaccionPagoCreateDTO dto) {
        try {
            TransaccionPago tx = pagoService.registrarTransaccion(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(tx);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/transaccion/{id}/aprobar")
    public ResponseEntity<?> aprobar(@PathVariable Long id, @RequestBody @Valid TransaccionAprobarDTO body) {
        try {
            TransaccionPago tx = pagoService.aprobarTransaccion(id, body.getIdAdmin());
            return ResponseEntity.ok(tx);
        } catch (java.util.NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/transaccion/{id}/rechazar")
    public ResponseEntity<?> rechazar(@PathVariable Long id, @RequestBody TransaccionRechazarDTO body) {
        try {
            TransaccionPago tx = pagoService.rechazarTransaccion(
                    id,
                    body != null ? body.getMotivo() : null,
                    body != null ? body.getIdAdmin() : null
            );
            return ResponseEntity.ok(tx);
        } catch (java.util.NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
