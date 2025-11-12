package com.resismart.backend.avisos.Controllers;

import com.resismart.backend.avisos.DTO.AvisoLeidoRequest;
import com.resismart.backend.avisos.DTO.AvisoPayload;
import com.resismart.backend.avisos.DTO.AvisoRequest;
import com.resismart.backend.avisos.Services.AvisoService;
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
    public AvisoPayload crear(@RequestBody AvisoRequest request) {
        return avisoService.emitirDesdeRequest(request);
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
}

