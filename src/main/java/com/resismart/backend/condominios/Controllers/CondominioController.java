package com.resismart.backend.condominios.Controllers;


import com.resismart.backend.condominios.DTO.*;
import com.resismart.backend.condominios.Services.CondominioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/Condominios")
public class CondominioController {

    private final CondominioService service;

    @PostMapping
    public ResponseEntity<?> crear(@Valid @RequestBody CondominioCreateDTO dto) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<Page<CondominioResumenDTO>> listar(Pageable pageable) {
        return ResponseEntity.ok(service.listar(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtener(@PathVariable Integer id,
                                     @RequestParam(defaultValue = "false") boolean detalle) {
        try {
            return ResponseEntity.ok(/*detalle ? service.obtenerConUnidades(id): */service.obtener(id));
        } catch (java.util.NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Integer id,
                                        @Valid @RequestBody CondominioUpdateDTO dto) {
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

    /* --- Unidades desde el condominio --- */
/*
    @PostMapping("/{id}/unidades")
    public ResponseEntity<?> agregarUnidad(@PathVariable Integer id,
                                           @Valid @RequestBody UnidadCreateDTO dto) {
        try {
            // forzar id del path
            dto.setIdCondominio(id);
            return ResponseEntity.status(HttpStatus.CREATED).body(service.agregarUnidad(id, dto));
        } catch (java.util.NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}/unidades")
    public ResponseEntity<?> listarUnidades(@PathVariable Integer id) {
        try {
            List<UnidadResumenDTO> uds = service.listarUnidadesDeCondominio(id);
            return ResponseEntity.ok(uds);
        } catch (java.util.NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}/resumen-ocupacion")
    public ResponseEntity<?> resumen(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(service.getResumenOcupacion(id));
        } catch (java.util.NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }*/
}