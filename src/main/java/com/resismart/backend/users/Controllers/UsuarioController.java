package com.resismart.backend.users.Controllers;

import com.resismart.backend.users.DTO.UsuarioActualizarPasswordRequest;
import com.resismart.backend.users.DTO.UsuarioClienteCredencialesDTO;
import com.resismart.backend.users.DTO.UsuarioCrearRequest;
import com.resismart.backend.users.DTO.UsuarioEditarRequest;
import com.resismart.backend.users.DTO.UsuarioPerfilRequest;
import com.resismart.backend.users.Entities.Usuario;
import com.resismart.backend.users.Enums.Rol;
import com.resismart.backend.users.Repositories.UsuarioRepository;
import com.resismart.backend.users.Services.UsuarioService;
import com.resismart.backend.storage.LocalStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

@RestController
@RequestMapping("/Usuarios")
public class UsuarioController {
    @Autowired
    private UsuarioService usuarioService;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private LocalStorageService mediaStorage;

    @GetMapping("/whoami")
    public ResponseEntity<?> whoami(Authentication authentication) {
        try {
            if (authentication == null || authentication.getPrincipal() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No autenticado");
            }
            String username;
            Object principal = authentication.getPrincipal();
            if (principal instanceof org.springframework.security.core.userdetails.UserDetails userDetails) {
                username = userDetails.getUsername();
            } else if (principal instanceof String s) { // fallback when principal is username String
                username = s;
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No autenticado");
            }

            Usuario u = usuarioService.getUsuarioByEmail(username);
            if (u == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Usuario no encontrado");
            }
            return ResponseEntity.ok(u);
        } catch (RuntimeException e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    @PutMapping("/me")
    public ResponseEntity<?> actualizarPerfilPropio(
            Authentication authentication,
            @RequestBody UsuarioPerfilRequest request) {
        Usuario actual = getCurrentUser(authentication);
        try {
            Usuario actualizado = usuarioService.actualizarPerfilPropio(actual.getId_usuario(), request);
            return ResponseEntity.ok(actualizado);
        } catch (RuntimeException e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }


    @PostMapping("/guardar")
    public ResponseEntity<?> postUsuario(@RequestBody UsuarioCrearRequest usuario,
                                         Authentication authentication){
        if (authentication == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Usuario current = usuarioRepository.findByCorreo(authentication.getName()).orElse(null);
        if (current == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Rol nuevoRol = usuario.getRol();
        if (nuevoRol == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Rol requerido");
        }

        boolean autorizado = false;
        if (current.getRol() == Rol.ADMIN) {
            autorizado = (nuevoRol == Rol.ADMIN || nuevoRol == Rol.DUEÑO);
        } else if (current.getRol() == Rol.DUEÑO) {
            autorizado = (nuevoRol == Rol.RESIDENTE);
        }

        if (!autorizado) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Sin acceso para crear usuario con ese rol");
        }

        try {
            Usuario u=usuarioService.register(usuario);
            return ResponseEntity.created(new URI("/Usuarios/"+u.getId_usuario())).body(u);
        } catch (URISyntaxException | RuntimeException  e) {
            return ResponseEntity.internalServerError().body("Error: "+e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> putUsuario(@PathVariable int id, @RequestBody UsuarioEditarRequest usuario){
        try {
            // Asegurar que el ID del path se use en la edición
            usuario.setID_Usuario(id);
            Usuario u=usuarioService.putUsuario(usuario);
            return ResponseEntity.ok(u);
        } catch (RuntimeException  e) {
            return ResponseEntity.internalServerError().body("Error: "+e.getMessage());
        }
    }

    @PutMapping("/credencialesCliente")
    public ResponseEntity<?> putCredencialesUsuarioCliente(@RequestBody @Valid UsuarioClienteCredencialesDTO usuario){
        try {
            Usuario u=usuarioService.actualizarCredencialesUsuarioCliente(usuario);
            return ResponseEntity.created(new URI("/Usuarios/"+u.getId_usuario())).body(u);
        } catch (URISyntaxException | RuntimeException  e) {
            return ResponseEntity.internalServerError().body("Error: "+e.getMessage());
        }
    }

    @PutMapping("/{id}/password")
    public ResponseEntity<?> actualizarPassword(@PathVariable int id, @RequestBody @Valid UsuarioActualizarPasswordRequest req){
        try {
            Usuario u = usuarioService.actualizarPassword(id, req.getPassword());
            return ResponseEntity.ok(u);
        } catch (RuntimeException e) {
            return ResponseEntity.internalServerError().body("Error: "+e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<Usuario>> getAll(Authentication authentication){
        if (authentication == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        String correo = authentication.getName();
        Usuario current = usuarioRepository.findByCorreo(correo).orElse(null);
        if (current == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        // ADMIN -> todos; DUEÑO -> solo residentes de sus condominios
        return ResponseEntity.ok(usuarioService.getUsuariosVisiblesPara(current));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable int id,
                                     Authentication authentication){
        if (authentication == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        String correo = authentication.getName();
        Usuario current = usuarioRepository.findByCorreo(correo).orElse(null);
        if (current == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Usuario c = usuarioService.getUsuarioById(id);
        if (c == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Usuario no encontrado");
        }

        if (current.getRol() == Rol.ADMIN) {
            return ResponseEntity.ok(c);
        }
        if (current.getRol() == Rol.DUEÑO) {
            // DUEÑO solo puede ver residentes de sus condominios
            boolean visible = usuarioService.getUsuariosVisiblesPara(current).stream()
                    .anyMatch(u -> u.getId_usuario() == id);
            if (visible) return ResponseEntity.ok(c);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Sin acceso");
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Sin acceso");
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteById(@PathVariable int id,
                                        Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Usuario current = usuarioRepository.findByCorreo(authentication.getName()).orElse(null);
        if (current == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        // ADMIN no puede borrar usuarios (regla de negocio)
        if (current.getRol() == Rol.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("ADMIN no puede eliminar usuarios");
        }

        Usuario result = usuarioService.getUsuarioById(id);
        if (result != null) {
            usuarioService.deleteUsuario(id);
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Usuario no encontrado");
        }
    }

    @Operation(summary = "Subir avatar de usuario", description = "Permite actualizar el avatar. ADMIN puede subir de cualquier usuario; el propio usuario puede subir el suyo.")
    @PostMapping(value = "/{id}/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> subirAvatar(@PathVariable int id,
                                         @Parameter(description = "Archivo de imagen") @RequestParam("avatar") MultipartFile avatar,
                                         Authentication authentication) {
        return handleAvatarUpload(id, avatar, authentication);
    }

    @Operation(summary = "Subir avatar del usuario actual", description = "Actualiza el avatar del usuario autenticado.")
    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> subirMiAvatar(@Parameter(description = "Archivo de imagen") @RequestParam("avatar") MultipartFile avatar,
                                           Authentication authentication) {
        Usuario current = getCurrentUser(authentication);
        return handleAvatarUpload(current.getId_usuario(), avatar, authentication);
    }

    private ResponseEntity<?> handleAvatarUpload(int id, MultipartFile avatar, Authentication authentication) {
        Usuario current = getCurrentUser(authentication);
        Usuario objetivo = usuarioService.getUsuarioById(id);
        if (objetivo == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Usuario no encontrado");
        }
        boolean isAdmin = current.getRol() == Rol.ADMIN;
        boolean isSelf = current.getId_usuario() == objetivo.getId_usuario();
        if (!isAdmin && !isSelf) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Sin acceso para subir avatar");
        }
        if (avatar == null || avatar.isEmpty()) {
            return ResponseEntity.badRequest().body("Archivo vacio");
        }
        try {
            String filename = mediaStorage.guardarImagen(avatar, "users/" + current.getId_usuario() + "/avatar");
            objetivo.setAvatarUrl(filename);
            usuarioRepository.save(objetivo);
            return ResponseEntity.ok(objetivo);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.internalServerError().body("Error al guardar avatar: " + e.getMessage());
        }
    }

    private Usuario getCurrentUser(Authentication authentication) {
        Authentication auth = authentication != null ? authentication : SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autenticado");
        }
        return usuarioRepository.findByCorreo(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autenticado"));
    }
}
