package com.resismart.backend.condominios.Controllers;

import com.resismart.backend.contratos.Entities.Contrato;
import com.resismart.backend.contratos.Enums.EstadoContrato;
import com.resismart.backend.contratos.Repositories.ContratoRepository;
import com.resismart.backend.condominios.DTO.UnidadCreateDTO;
import com.resismart.backend.condominios.DTO.UnidadResumenDTO;
import com.resismart.backend.condominios.DTO.UnidadUpdateDTO;
import com.resismart.backend.condominios.Services.CondominioService;
import com.resismart.backend.condominios.Services.UnidadService;
import com.resismart.backend.condominios.Repositories.UnidadRepository;
import com.resismart.backend.residentes.Entities.Residente;
import com.resismart.backend.residentes.Repositories.ResidenteRepository;
import com.resismart.backend.users.Entities.Usuario;
import com.resismart.backend.users.Enums.Rol;
import com.resismart.backend.users.Repositories.UsuarioRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    private final ResidenteRepository residenteRepository;
    private final ContratoRepository contratoRepository;

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

    /**
     * Retorna la unidad asociada al contrato del residente autenticado.
     * Si hay varios contratos, prioriza el ACTIVO y luego el primero de la lista.
     */
    @GetMapping("/me")
    public ResponseEntity<?> obtenerUnidadActual(org.springframework.security.core.Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Usuario current = usuarioRepository.findByCorreo(authentication.getName()).orElse(null);
        if (current == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (current.getRol() != Rol.RESIDENTE) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Solo residentes"));
        }

        Residente residente = residenteRepository.findByUsuarioId(current.getId_usuario())
                .orElse(null);
        if (residente == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "No se encontró el residente"));
        }

        List<Contrato> contratos = contratoRepository.findByResidente_Id(residente.getId());
        if (contratos == null || contratos.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "No tienes contratos activos"));
        }

        Contrato contratoActivo = contratos.stream()
                .filter(c -> c.getEstado() == EstadoContrato.ACTIVO)
                .findFirst()
                .orElse(contratos.get(0));

        if (contratoActivo.getUnidad() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Contrato sin unidad asociada"));
        }

        var unidad = contratoActivo.getUnidad();
        var condominio = unidad.getCondominio();

        return ResponseEntity.ok(Map.of(
                "id", unidad.getId(),
                "numero", unidad.getNumero(),
                "estado", unidad.getEstado(),
                "condominioId", condominio != null ? condominio.getId() : null,
                "condominioNombre", condominio != null ? condominio.getNombre() : null
        ));
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
