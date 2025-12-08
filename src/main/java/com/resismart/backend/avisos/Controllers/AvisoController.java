package com.resismart.backend.avisos.Controllers;

import com.resismart.backend.avisos.DTO.AvisoLeidoRequest;
import com.resismart.backend.avisos.DTO.AvisoPayload;
import com.resismart.backend.avisos.DTO.AvisoRequest;
import com.resismart.backend.avisos.Services.AvisoService;
import com.resismart.backend.users.Entities.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/Avisos")
@RequiredArgsConstructor
public class AvisoController {

    private final AvisoService avisoService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AvisoPayload crear(@RequestBody AvisoRequest request,
                              org.springframework.security.core.Authentication authentication) {
        Usuario emisor = resolveUsuario(authentication);
        return avisoService.crearAvisoGeneral(request, emisor);
    }

    @PostMapping("/privado")
    @ResponseStatus(HttpStatus.CREATED)
    public AvisoPayload crearPrivado(@RequestBody AvisoRequest request,
                                     org.springframework.security.core.Authentication authentication) {
        Usuario emisor = resolveUsuario(authentication);
        return avisoService.crearAvisoPrivado(request, emisor);
    }

    @PostMapping("/{id}/responder")
    @ResponseStatus(HttpStatus.CREATED)
    public AvisoPayload responder(@PathVariable Long id,
                                  @RequestBody AvisoRequest request,
                                  org.springframework.security.core.Authentication authentication) {
        Usuario emisor = resolveUsuario(authentication);
        return avisoService.responderAviso(id, request, emisor);
    }

    @GetMapping("/conversacion")
    public List<AvisoPayload> conversacion(@RequestParam Integer usuario1, @RequestParam Integer usuario2) {
        return avisoService.obtenerConversacion(usuario1, usuario2);
    }

    @GetMapping("/usuarios/{usuarioId}")
    public List<AvisoPayload> listarPorUsuario(@PathVariable Integer usuarioId) {
        return avisoService.listarPorUsuario(usuarioId);
    }

    @GetMapping("/condominios/{condominioId}")
    public List<AvisoPayload> listarPorCondominio(@PathVariable Integer condominioId) {
        return avisoService.listarPorCondominio(condominioId);
    }

    @GetMapping("/broadcast")
    public List<AvisoPayload> listarBroadcast() {
        return avisoService.listarBroadcast();
    }

    @PostMapping("/usuarios/{usuarioId}/leidos")
    public Map<String, Object> marcarLeidos(@PathVariable Integer usuarioId,
                                            @RequestBody(required = false) AvisoLeidoRequest request) {
        List<Long> ids = request != null ? request.getAvisoIds() : List.of();
        int actualizados = avisoService.marcarAvisosLeidos(usuarioId, ids);
        return Map.of(
                "usuarioId", usuarioId,
                "solicitados", ids,
                "actualizados", actualizados
        );
    }

    private Usuario resolveUsuario(org.springframework.security.core.Authentication authentication) {
        if (authentication == null) throw new RuntimeException("Usuario no autenticado");
        var principal = authentication.getPrincipal();
        if (principal instanceof org.springframework.security.core.userdetails.UserDetails u) {
            Usuario found = avisoService.findUsuarioByCorreo(u.getUsername());
            if (found != null) return found;
        }
        throw new RuntimeException("Usuario no autenticado");
    }
}

