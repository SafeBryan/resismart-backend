package com.resismart.backend.condominios.Controllers;

import com.resismart.backend.condominios.DTO.*;
import com.resismart.backend.condominios.Services.UnidadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/Unidades")
public class UnidadController {

    private final UnidadService service;

    @PostMapping
    public ResponseEntity<?> crear(@Valid @RequestBody UnidadCreateDTO dto) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(dto));
        } catch (java.util.NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtener(@PathVariable Integer id) {
        try {
            UnidadResumenDTO dto = service.obtener(id);
            return ResponseEntity.ok(dto);
        } catch (java.util.NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/por-condominio/{condominioId}")
    public ResponseEntity<?> listarPorCondominio(@PathVariable Integer condominioId) {
        try {
            List<UnidadResumenDTO> list = service.listarPorCondominio(condominioId);
            return ResponseEntity.ok(list);
        } catch (java.util.NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Integer id,
                                        @Valid @RequestBody UnidadUpdateDTO dto) {
        try {
            return ResponseEntity.ok(service.actualizar(id, dto));
        } catch (java.util.NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
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
}