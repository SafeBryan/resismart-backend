package com.resismart.backend.documentos.Controllers;

import com.resismart.backend.documentos.DTO.*;
import com.resismart.backend.documentos.Entities.AuditoriaDocumentos;
import com.resismart.backend.documentos.Entities.Documento;
import com.resismart.backend.documentos.Repositories.DocumentoRepository;
import com.resismart.backend.documentos.Services.AuditoriaDocumentosService;
import com.resismart.backend.documentos.Services.GestorDocumentosService;
import com.resismart.backend.documentos.Storage.StoragePort;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/documentos")
public class DocumentosController {

    private final GestorDocumentosService gestor;
    private final AuditoriaDocumentosService auditoriaSrv;
    private final StoragePort storage;
    private final DocumentoRepository documentoRepo;

    /**
     * Sube un documento (multipart/form-data).
     * Header requerido: X-USER (id Integer del usuario autenticado)
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentoDetalleDTO> upload(
            @ModelAttribute DocumentoUploadDTO dto,
            @RequestHeader("X-USER") Integer usuarioId
    ) {
        DocumentoDetalleDTO out = gestor.upload(dto, usuarioId);
        return ResponseEntity.status(HttpStatus.CREATED).body(out);
    }

    /** Detalle de un documento por id. */
    @GetMapping("/{idDocumento}")
    public ResponseEntity<DocumentoDetalleDTO> detalle(@PathVariable Integer idDocumento) {
        DocumentoDetalleDTO dto = gestor.detalle(idDocumento);
        return ResponseEntity.ok(dto);
    }

    /** Listado con filtros + paginación (query params). */

    @GetMapping
    public ResponseEntity<?> listar(
            @ModelAttribute DocumentoFiltroDTO filtros,
            @RequestParam(name = "flat", defaultValue = "false") boolean flat,
            @RequestParam(name = "withLinks", defaultValue = "true") boolean withLinks
    ) {
        var page = gestor.listar(filtros);

        if (flat) {
            // Modo “bonito” para el front: array + X-Total-Count
            var list = page.getContent().stream()
                    .map(dto -> DocItem.from(dto, withLinks))
                    .toList();

            return ResponseEntity.ok()
                    .header("X-Total-Count", String.valueOf(page.getTotalElements()))
                    .body(list);
        }

        return ResponseEntity.ok(PageResponse.of(page));
    }

    public record DocItem(
            Integer id,
            String nombre,
            String tipo,
            String estadoValidacion,
            String creadoEn,
            Long sizeBytes,
            String mimeType,
            String urlContenido
    ) {
        static DocItem from(DocumentoResumenDTO d, boolean withLinks) {
            String url = withLinks ? ("/documentos/" + d.getIdDocumento() + "/contenido") : null;
            return new DocItem(
                    d.getIdDocumento(),
                    d.getNombreOriginal(),
                    d.getTipo() == null ? null : d.getTipo().name(),
                    d.getEstadoValidacion() == null ? null : d.getEstadoValidacion().name(),
                    d.getFechaSubida() == null ? null : d.getFechaSubida().toString(),
                    d.getSizeBytes(),
                    null,
                    url
            );
        }
    }




    /**
     * Asociar un documento a un contrato u orden.
     * En body JSON: DocumentoAsociacionDTO
     * Header requerido: X-USER (id Integer del usuario autenticado)
     */
    @PostMapping("/asociar")
    public ResponseEntity<Void> asociar(
            @RequestBody DocumentoAsociacionDTO dto,
            @RequestHeader("X-USER") Integer usuarioId
    ) {
        gestor.asociar(dto, usuarioId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Descargar/leer contenido del documento.
     * Devuelve stream con Content-Type y Content-Disposition.
     */
    @GetMapping("/{idDocumento}/contenido")
    public ResponseEntity<InputStreamResource> descargar(@PathVariable Integer idDocumento) {
        Documento d = documentoRepo.findById(idDocumento)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Documento no encontrado"));

        try {
            InputStream is = storage.read(d.getStorageKey());
            String filename = d.getNombreOriginal() != null ? d.getNombreOriginal() : ("documento-" + d.getId());
            String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(d.getMimeType() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : d.getMimeType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(d.getSizeBytes() == null ? -1 : d.getSizeBytes()))
                    .body(new InputStreamResource(is));
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo leer el contenido", ex);
        }
    }

    /** Historial de auditoría de un documento. */
    @GetMapping("/{idDocumento}/auditoria")
    public ResponseEntity<List<AuditoriaDocumentos>> historial(@PathVariable Integer idDocumento) {
        List<AuditoriaDocumentos> list = auditoriaSrv.historial(idDocumento);
        return ResponseEntity.ok(list);
    }

    // --- DTO de respuesta paginada simple (para no exponer Page directamente) ---
    public record PageResponse<T>(
            int page, int size, long total, int totalPages, boolean first, boolean last, List<T> content
    ) {
        public static <T> PageResponse<T> of(org.springframework.data.domain.Page<T> p) {
            return new PageResponse<>(
                    p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages(),
                    p.isFirst(), p.isLast(), p.getContent()
            );
        }
    }
}
