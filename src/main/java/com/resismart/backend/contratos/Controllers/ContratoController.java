package com.resismart.backend.contratos.Controllers;

import com.resismart.backend.contratos.DTO.*;
import com.resismart.backend.contratos.Enums.EstadoContrato;
import com.resismart.backend.contratos.Services.ContratoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/Contratos")
public class ContratoController {

    private final ContratoService service;

    /* ==== CRUD ==== */

    @PostMapping
    public ResponseEntity<?> crear(@Valid @RequestBody ContratoCreateDTO dto) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtener(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(service.obtener(id));
        } catch (java.util.NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Integer id,
                                        @Valid @RequestBody ContratoUpdateDTO dto) {
        try {
            return ResponseEntity.ok(service.actualizar(id, dto));
        } catch (java.util.NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Integer id) {
        try {
            service.eliminar(id);
            return ResponseEntity.noContent().build();
        } catch (java.util.NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    /* ==== Consultas ==== */

    @GetMapping("/residente/{idResidente}")
    public ResponseEntity<List<ContratoResumenDTO>> listarPorResidente(@PathVariable Long idResidente) {
        return ResponseEntity.ok(service.listarPorResidente(idResidente));
    }

    @GetMapping("/estado/{estado}")
    public ResponseEntity<List<ContratoResumenDTO>> listarPorEstado(@PathVariable EstadoContrato estado) {
        return ResponseEntity.ok(service.listarPorEstado(estado));
    }

    /* ==== Acciones de negocio ==== */

    @PostMapping("/{id}/renovar")
    public ResponseEntity<?> renovar(@PathVariable Integer id,
                                     @Valid @RequestBody ContratoRenovarDTO dto) {
        try {
            return ResponseEntity.ok(service.renovar(id, dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (java.util.NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/rescindir")
    public ResponseEntity<?> rescindir(@PathVariable Integer id,
                                       @RequestBody ContratoRescindirDTO dto) {
        try {
            return ResponseEntity.ok(service.rescindir(id, dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (java.util.NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }
}
