package com.resismart.backend.condominios.Controllers;

import com.resismart.backend.condominios.DTO.*;
import com.resismart.backend.condominios.Services.UnidadService;
import com.resismart.backend.condominios.Services.CondominioService;
import com.resismart.backend.condominios.Repositories.UnidadRepository;
import com.resismart.backend.users.Entities.Usuario;
import com.resismart.backend.users.Enums.Rol;
import com.resismart.backend.users.Repositories.UsuarioRepository;
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
    private final CondominioService condominioService;
    private final UsuarioRepository usuarioRepository;
    private final UnidadRepository unidadRepository;

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
    public ResponseEntity<?> obtener(@PathVariable Integer id,
                                     org.springframework.security.core.Authentication authentication) {
        try {
            String correo = authentication.getName();
            Usuario current = usuarioRepository.findByCorreo(correo).orElse(null);
            if (current == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            if (current.getRol() == Rol.ADMIN || unidadRepository.existsByIdAndDueno(id, current.getId_usuario())) {
                UnidadResumenDTO dto = service.obtener(id);
                return ResponseEntity.ok(dto);
            } else {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Sin acceso"));
            }
        } catch (java.util.NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/por-condominio/{condominioId}")
    public ResponseEntity<?> listarPorCondominio(@PathVariable Integer condominioId,
                                                  org.springframework.security.core.Authentication authentication) {
        try {
            String correo = authentication.getName();
            Usuario current = usuarioRepository.findByCorreo(correo).orElse(null);
            if (current == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            if (current.getRol() == Rol.ADMIN || condominioService.esDuenoDeCondominio(condominioId, current.getId_usuario())) {
                List<UnidadResumenDTO> list = service.listarPorCondominio(condominioId);
                return ResponseEntity.ok(list);
            } else {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Sin acceso"));
            }
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
