package com.resismart.backend.residentes.Controllers;

import com.resismart.backend.residentes.DTO.ResidenteDTO;
import com.resismart.backend.residentes.DTO.ResidentePerfilRequest;
import com.resismart.backend.residentes.DTO.ResidenteRespuestaDTO;
import com.resismart.backend.residentes.Services.ResidenteService;
import com.resismart.backend.users.Entities.Usuario;
import com.resismart.backend.users.Enums.Rol;
import com.resismart.backend.users.Repositories.UsuarioRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/Residentes")
public class ResidenteController {
    @Autowired
    private ResidenteService residenteService;
    @Autowired private UsuarioRepository usuarioRepository;

    /**
     * Alta de residente (ADMIN o DUENO).
     * @return DTO con el residente creado.
     */
    @PostMapping
    public ResponseEntity<?> saveCliente(@Valid @RequestBody ResidenteDTO residenteDTO,
                                         org.springframework.security.core.Authentication authentication){
        if (authentication == null) return ResponseEntity.status(401).build();
        Usuario current = usuarioRepository.findByCorreo(authentication.getName()).orElse(null);
        if (current == null) return ResponseEntity.status(401).build();

        try {
            if (current.getRol() != Rol.ADMIN && current.getRol() != Rol.DUEÑO) {
                return ResponseEntity.status(403).body("Sin acceso");
            }

            return ResponseEntity.ok(residenteService.saveCliente(residenteDTO));
        } catch (java.util.NoSuchElementException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (Exception e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Obtiene un residente por id (ADMIN o DUENO).
     * @return DTO de residente o 404/403.
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getClienteById(@PathVariable Long id,
                                            org.springframework.security.core.Authentication authentication) {
        if (authentication == null) return ResponseEntity.status(401).build();
        Usuario current = usuarioRepository.findByCorreo(authentication.getName()).orElse(null);
        if (current == null) return ResponseEntity.status(401).build();

        if (current.getRol() == Rol.ADMIN || current.getRol() == Rol.DUEÑO) {
            return residenteService.getCliente(id)
                    .map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } else {
            return ResponseEntity.status(403).body("Sin acceso");
        }
    }

    /**
     * Lista residentes (ADMIN o DUENO).
     * @return lista de residentes.
     */
    @GetMapping
    public ResponseEntity<?> getAllClientes(org.springframework.security.core.Authentication authentication) {
        if (authentication == null) return ResponseEntity.status(401).build();
        Usuario current = usuarioRepository.findByCorreo(authentication.getName()).orElse(null);
        if (current == null) return ResponseEntity.status(401).build();

        if (current.getRol() == Rol.ADMIN || current.getRol() == Rol.DUEÑO) {
            return ResponseEntity.ok(residenteService.getAllClientes());
        } else {
            return ResponseEntity.status(403).body("Sin acceso");
        }
    }

    /**
     * Actualiza perfil propio (solo RESIDENTE).
     * @return DTO de residente actualizado.
     */
    @PutMapping("/me")
    public ResponseEntity<?> actualizarPerfilPropio(
            org.springframework.security.core.Authentication authentication,
            @RequestBody ResidentePerfilRequest request) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).body("No autenticado");
        }

        Usuario current = usuarioRepository.findByCorreo(authentication.getName()).orElse(null);
        if (current == null) {
            return ResponseEntity.status(401).body("No autenticado");
        }
        if (current.getRol() != Rol.RESIDENTE) {
            return ResponseEntity.status(403).body("Sin acceso");
        }

        try {
            ResidenteRespuestaDTO actualizado = residenteService.actualizarPerfilPropio(current.getId_usuario(), request);
            return ResponseEntity.ok(actualizado);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    /**
     * Actualiza un residente (ADMIN o DUENO).
     * @return DTO actualizado.
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateCliente(@PathVariable Long id, @Valid @RequestBody ResidenteDTO residenteDTO,
                                           org.springframework.security.core.Authentication authentication){
        if (authentication == null) return ResponseEntity.status(401).build();
        Usuario current = usuarioRepository.findByCorreo(authentication.getName()).orElse(null);
        if (current == null) return ResponseEntity.status(401).build();

        if (current.getRol() == Rol.ADMIN || current.getRol() == Rol.DUEÑO) {
            return  ResponseEntity.ok(residenteService.updateCliente(id, residenteDTO));
        }
        return ResponseEntity.status(403).body("Sin acceso");
    }

    /**
     * Baja logica de un residente (ADMIN no puede).
     * @return 204 o 403/404.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCliente(@PathVariable Long id,
                                           org.springframework.security.core.Authentication authentication) {
        if (authentication == null) return ResponseEntity.status(401).build();
        Usuario current = usuarioRepository.findByCorreo(authentication.getName()).orElse(null);
        if (current == null) return ResponseEntity.status(401).build();

        // ADMIN no puede borrar inquilinos (regla de negocio previa)
        if (current.getRol() == Rol.ADMIN) {
            return ResponseEntity.status(403).body("Admin no puede eliminar inquilinos");
        }

        try {
            residenteService.deleteCliente(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException ex) {
            // Si hay contratos activos u otras validaciones de negocio
            return ResponseEntity.status(409).body(ex.getMessage());
        }
    }

    /**
     * Lista residentes de un condominio.
     * Relación ahora se hará vía contratos; se devuelve lista vacía por ahora.
     */
    @GetMapping("/condominio/{id}")
    public ResponseEntity<?> getAllClientesPorCondominio(@PathVariable Integer id,
                                                         org.springframework.security.core.Authentication authentication) {
        if (authentication == null) return ResponseEntity.status(401).build();
        Usuario current = usuarioRepository.findByCorreo(authentication.getName()).orElse(null);
        if (current == null) return ResponseEntity.status(401).build();

        if (current.getRol() == Rol.ADMIN || current.getRol() == Rol.DUEÑO) {
            return ResponseEntity.ok(residenteService.getAllClientesPorCondominio(id));
        } else {
            return ResponseEntity.status(403).body("Sin acceso");
        }
    }

    /**
     * Obtiene residente por id de usuario (ADMIN o DUENO; RESIDENTE no aplica).
     * @return DTO de residente o 404.
     */
    @GetMapping("/por-usuario/{idUsuario}")
    public ResponseEntity<ResidenteRespuestaDTO> getByUsuario(@PathVariable Long idUsuario) {
        return residenteService.findByUsuarioId(idUsuario)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Endpoint legado con ruta duplicada (/Residentes/Residentes/por-usuario/{idUsuario}).
     * @deprecated Usar {@link #getByUsuario(Long)} en /Residentes/por-usuario/{idUsuario}.
     */
    @Deprecated
    @GetMapping("/Residentes/por-usuario/{idUsuario}")
    public ResponseEntity<ResidenteRespuestaDTO> getByUsuarioLegacy(@PathVariable Long idUsuario) {
        return getByUsuario(idUsuario);
    }
}
