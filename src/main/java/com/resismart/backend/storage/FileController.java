package com.resismart.backend.storage;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileController {

    private final StoragePort storagePort;

    /**
     * Sirve archivos permitiendo rutas con subcarpetas dentro de /files/**.
     */
    @GetMapping("/**")
    public ResponseEntity<Resource> getFile(HttpServletRequest request) throws IOException {
        String uri = request.getRequestURI();
        // Extrae la ruta relativa después de /files/
        String relative = uri.replaceFirst(".*/files/?", "");
        Resource resource = storagePort.cargarArchivo(relative);

        String contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        if (contentType == null) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}
