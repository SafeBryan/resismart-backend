package com.resismart.backend.condominios.Controllers;


import com.resismart.backend.condominios.DTO.*;
import com.resismart.backend.condominios.Services.CondominioService;
import com.resismart.backend.users.Entities.Usuario;
import com.resismart.backend.users.Enums.Rol;
import com.resismart.backend.users.Repositories.UsuarioRepository;
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
    private final UsuarioRepository usuarioRepository;

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
    public ResponseEntity<Page<CondominioResumenDTO>> listar(Pageable pageable,
                                                            org.springframework.security.core.Authentication authentication) {
        if (authentication == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        String correo = authentication.getName();
        Usuario current = usuarioRepository.findByCorreo(correo).orElse(null);
        if (current == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        if (current.getRol() == Rol.ADMIN) {
            return ResponseEntity.ok(service.listar(pageable));
        } else if (current.getRol() == Rol.DUEÑO) {
            return ResponseEntity.ok(service.listarPorDueno(current.getId_usuario(), pageable));
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtener(@PathVariable Integer id,
                                     @RequestParam(defaultValue = "false") boolean detalle,
                                     org.springframework.security.core.Authentication authentication) {
        try {
            String correo = authentication.getName();
            Usuario current = usuarioRepository.findByCorreo(correo).orElse(null);
            if (current == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

            if (current.getRol() == Rol.ADMIN || service.esDuenoDeCondominio(id, current.getId_usuario())) {
                return ResponseEntity.ok(detalle ? service.obtenerConUnidades(id): service.obtener(id));
            } else {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Sin acceso"));
            }
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

    @PostMapping("/{id}/unidades")
    public ResponseEntity<?> agregarUnidad(@PathVariable Integer id,
                                           @Valid @RequestBody UnidadCreateDTO dto,
                                           org.springframework.security.core.Authentication authentication) {
        try {
            // forzar id del path
            dto.setIdCondominio(id);
            String correo = authentication.getName();
            Usuario current = usuarioRepository.findByCorreo(correo).orElse(null);
            if (current == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            if (current.getRol() == Rol.ADMIN || service.esDuenoDeCondominio(id, current.getId_usuario())) {
                return ResponseEntity.status(HttpStatus.CREATED).body(service.agregarUnidad(id, dto));
            } else {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Sin acceso"));
            }
        } catch (java.util.NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}/unidades")
    public ResponseEntity<?> listarUnidades(@PathVariable Integer id,
                                            org.springframework.security.core.Authentication authentication) {
        try {
            String correo = authentication.getName();
            Usuario current = usuarioRepository.findByCorreo(correo).orElse(null);
            if (current == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            if (current.getRol() == Rol.ADMIN || service.esDuenoDeCondominio(id, current.getId_usuario())) {
                List<UnidadResumenDTO> uds = service.listarUnidadesDeCondominio(id);
                return ResponseEntity.ok(uds);
            } else {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Sin acceso"));
            }
        } catch (java.util.NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}/resumen-ocupacion")
    public ResponseEntity<?> resumen(@PathVariable Integer id,
                                     org.springframework.security.core.Authentication authentication) {
        try {
            String correo = authentication.getName();
            Usuario current = usuarioRepository.findByCorreo(correo).orElse(null);
            if (current == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            if (current.getRol() == Rol.ADMIN || service.esDuenoDeCondominio(id, current.getId_usuario())) {
                return ResponseEntity.ok(service.getResumenOcupacion(id));
            } else {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Sin acceso"));
            }
        } catch (java.util.NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }
}
