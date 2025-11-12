package com.resismart.backend.eventos.Controllers;

import com.resismart.backend.eventos.DTO.*;
import com.resismart.backend.eventos.Services.EventoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/Eventos")
public class EventoController {

    @Autowired private EventoService service;

    /* ==== CRUD Evento ==== */

    @PostMapping
    public ResponseEntity<?> crear(@Valid @RequestBody EventoCreateDTO dto) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (java.util.NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
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

    @GetMapping("/condominio/{idCondominio}")
    public ResponseEntity<?> listarPorCondominio(@PathVariable Integer idCondominio) {
        try {
            List<EventoResumenDTO> lista = service.listarPorCondominio(idCondominio);
            return ResponseEntity.ok(lista);
        } catch (java.util.NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/condominio/{idCondominio}/detallado")
    public ResponseEntity<?> listarDetalladoPorCondominio(@PathVariable Integer idCondominio) {
        try {
            return ResponseEntity.ok(service.listarDetalladosPorCondominio(idCondominio));
        } catch (java.util.NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Integer id,
                                        @Valid @RequestBody EventoUpdateDTO dto) {
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

    /* ==== Participantes ==== */

    @GetMapping("/{id}/participantes")
    public ResponseEntity<?> listarParticipantes(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(service.listarParticipantes(id));
        } catch (java.util.NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/participantes")
    public ResponseEntity<?> agregarParticipante(@PathVariable Integer id,
                                                 @Valid @RequestBody EventoParticipanteAddDTO dto) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(service.agregarParticipante(id, dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (java.util.NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}/participantes/{usuarioId}")
    public ResponseEntity<?> eliminarParticipante(@PathVariable Integer id,
                                                  @PathVariable Integer usuarioId) {
        try {
            service.eliminarParticipante(id, usuarioId);
            return ResponseEntity.noContent().build();
        } catch (java.util.NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    /* ==== Asistencia ==== */

    @PostMapping("/{id}/asistencia")
    public ResponseEntity<?> confirmarAsistencia(@PathVariable Integer id, Authentication authentication) {
        try {
            return ResponseEntity.ok(service.confirmarAsistenciaActual(id, authentication.getName()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (java.util.NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/asistencia")
    public ResponseEntity<?> actualizarAsistencia(@PathVariable Integer id,
                                                  @Valid @RequestBody EventoAsistenciaRequest req,
                                                  Authentication authentication) {
        try {
            return ResponseEntity.ok(service.actualizarAsistenciaActual(id, authentication.getName(), req.getEstado()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (java.util.NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
