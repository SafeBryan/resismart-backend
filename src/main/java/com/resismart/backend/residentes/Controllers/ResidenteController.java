package com.resismart.backend.residentes.Controllers;


import com.resismart.backend.residentes.DTO.ResidenteDTO;
import com.resismart.backend.residentes.DTO.ResidentePerfilRequest;
import com.resismart.backend.residentes.DTO.ResidenteRespuestaDTO;
import com.resismart.backend.residentes.Services.ResidenteService;
import com.resismart.backend.condominios.Services.CondominioService;
import com.resismart.backend.users.Entities.Usuario;
import com.resismart.backend.users.Enums.Rol;
import com.resismart.backend.users.Repositories.UsuarioRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/Residentes")
public class ResidenteController {
    @Autowired
    private ResidenteService residenteService;
    @Autowired private CondominioService condominioService;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private com.resismart.backend.residentes.Repositories.ResidenteRepository residenteRepository;

    @PostMapping
    public ResponseEntity<?> saveCliente(@Valid @RequestBody ResidenteDTO residenteDTO){
        try {
            return ResponseEntity.ok(residenteService.saveCliente(residenteDTO));
        }catch (Exception e){
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getClienteById(@PathVariable Long id,
                                            org.springframework.security.core.Authentication authentication) {
        if (authentication == null) return ResponseEntity.status(401).build();
        String correo = authentication.getName();
        Usuario current = usuarioRepository.findByCorreo(correo).orElse(null);
        if (current == null) return ResponseEntity.status(401).build();

        if (current.getRol() == Rol.ADMIN) {
            return residenteService.getCliente(id)
                    .map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } else if (current.getRol() == Rol.DUEÑO) {
            if (!residenteService.esResidenteDeDueno(id, current.getId_usuario())) {
                return ResponseEntity.status(403).body("Sin acceso");
            }
            return residenteService.getCliente(id)
                    .map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } else {
            return ResponseEntity.status(403).body("Sin acceso");
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllClientes(org.springframework.security.core.Authentication authentication) {
        if (authentication == null) return ResponseEntity.status(401).build();
        String correo = authentication.getName();
        Usuario current = usuarioRepository.findByCorreo(correo).orElse(null);
        if (current == null) return ResponseEntity.status(401).build();

        if (current.getRol() == Rol.ADMIN) {
            return ResponseEntity.ok(residenteService.getAllClientes());
        } else if (current.getRol() == Rol.DUEÑO) {
            return ResponseEntity.ok(residenteService.getAllClientesPorDueno(current.getId_usuario()));
        } else {
            return ResponseEntity.status(403).body("Sin acceso");
        }
    }

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

    @PutMapping("/{id}")
    public ResponseEntity<ResidenteRespuestaDTO> updateCliente(@PathVariable Long id, @Valid @RequestBody ResidenteDTO residenteDTO){
        return  ResponseEntity.ok(residenteService.updateCliente(id, residenteDTO));

    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCliente(@PathVariable Long id,
                                           org.springframework.security.core.Authentication authentication) {
        if (authentication == null) return ResponseEntity.status(401).build();
        String correo = authentication.getName();
        Usuario current = usuarioRepository.findByCorreo(correo).orElse(null);
        if (current == null) return ResponseEntity.status(401).build();

        if (current.getRol() == Rol.ADMIN || residenteRepository.existsByIdAndDueno(id, current.getId_usuario())) {
            residenteService.deleteCliente(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.status(403).body("Sin acceso");
        }
    }

    @GetMapping("/condominio/{id}")
    public ResponseEntity<?> getAllClientesPorCondominio(@PathVariable Integer id,
                                                         org.springframework.security.core.Authentication authentication) {
        if (authentication == null) return ResponseEntity.status(401).build();
        String correo = authentication.getName();
        Usuario current = usuarioRepository.findByCorreo(correo).orElse(null);
        if (current == null) return ResponseEntity.status(401).build();

        if (current.getRol() == Rol.ADMIN || condominioService.esDuenoDeCondominio(id, current.getId_usuario())) {
            return ResponseEntity.ok(residenteService.getAllClientesPorCondominio(id));
        } else {
            return ResponseEntity.status(403).body("Sin acceso");
        }
    }

    @GetMapping("/Residentes/por-usuario/{idUsuario}")
    public ResponseEntity<ResidenteRespuestaDTO> getByUsuario(@PathVariable Long idUsuario) {
        return residenteService.findByUsuarioId(idUsuario)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
