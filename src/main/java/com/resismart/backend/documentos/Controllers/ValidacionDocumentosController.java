package com.resismart.backend.documentos.Controllers;

import com.resismart.backend.documentos.DTO.DocumentoValidacionDTO;
import com.resismart.backend.documentos.Services.ValidacionDocumentosService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/documentos")
public class ValidacionDocumentosController {

    private final ValidacionDocumentosService validacionSrv;

    /**
     * Cambia el estado de validación (APROBADO/RECHAZADO).
     * Body JSON: DocumentoValidacionDTO
     */
    @PostMapping("/validar")
    public ResponseEntity<Void> validar(@RequestBody DocumentoValidacionDTO dto) {
        validacionSrv.validar(dto);
        return ResponseEntity.noContent().build();
    }
}
