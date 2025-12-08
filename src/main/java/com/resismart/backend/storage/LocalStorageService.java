package com.resismart.backend.storage;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.Set;
import java.util.UUID;

@Service("mediaLocalStorageService")
@Primary
public class LocalStorageService implements StoragePort {

    @Value("${storage.local.base-path:uploads}")
    private String basePath;

    private Path root;

    @PostConstruct
    public void init() {
        try {
            root = Paths.get(basePath).toAbsolutePath().normalize();
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo inicializar el storage local", e);
        }
    }

    @Override
    public String guardarArchivo(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Archivo vacío");
        }
        String original = file.getOriginalFilename() == null ? "file" : StringUtils.cleanPath(file.getOriginalFilename());
        String extension = "";
        int dot = original.lastIndexOf('.');
        if (dot >= 0) {
            extension = original.substring(dot);
        }
        String filename = UUID.randomUUID() + extension;
        Path target = root.resolve(filename).normalize();
        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return filename;
        } catch (IOException e) {
            throw new RuntimeException("Error al guardar archivo", e);
        }
    }

    @Override
    public String guardarArchivoEn(MultipartFile file, String subPath) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Archivo vacío");
        }
        String original = file.getOriginalFilename() == null ? "file" : StringUtils.cleanPath(file.getOriginalFilename());
        String extension = "";
        int dot = original.lastIndexOf('.');
        if (dot >= 0) {
            extension = original.substring(dot);
        }
        String folder = subPath == null ? "" : subPath.replace("\\", "/").replaceAll("[^a-zA-Z0-9_./-]", "");
        Path relativeDir = folder.isBlank() ? null : Paths.get(folder).normalize();
        String filename = UUID.randomUUID() + extension;
        Path target = (relativeDir != null ? root.resolve(relativeDir) : root).resolve(filename).normalize();
        try {
            ensureInsideRoot(target);
            Files.createDirectories(target.getParent());
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return root.relativize(target).toString().replace('\\', '/');
        } catch (IOException e) {
            throw new RuntimeException("Error al guardar archivo", e);
        }
    }

    /**
     * Guarda una imagen validando MIME y usando un prefijo en el nombre físico.
     * Si el prefijo contiene subcarpetas (p.ej. users/123/avatar) se crearán dentro de uploads.
     */
    public String guardarImagen(MultipartFile file, String prefix) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Archivo vacío");
        }
        String contentType = file.getContentType();
        Set<String> allowed = Set.of("image/png", "image/jpeg", "image/webp");
        if (contentType == null || !allowed.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Tipo de archivo no soportado. Solo se permiten imágenes PNG, JPG o WEBP");
        }

        String extension = resolverExtension(file, contentType);
        String sanitized = (prefix == null || prefix.isBlank()) ? "img" : prefix.replaceAll("[^a-zA-Z0-9_./-]", "");
        Path prefPath = Paths.get(sanitized).normalize();
        Path relativeDir = prefPath.getParent();
        String baseName = prefPath.getFileName() != null ? prefPath.getFileName().toString() : "img";
        String filename = (baseName.isBlank() ? "img" : baseName) + "_" + System.currentTimeMillis() + extension;
        Path target = (relativeDir != null ? root.resolve(relativeDir) : root).resolve(filename).normalize();
        try {
            ensureInsideRoot(target);
            Files.createDirectories(target.getParent());
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return root.relativize(target).toString().replace('\\', '/');
        } catch (IOException e) {
            throw new RuntimeException("Error al guardar imagen", e);
        }
    }

    private String resolverExtension(MultipartFile file, String contentType) {
        String extension = "";
        String original = file.getOriginalFilename();
        if (original != null && original.contains(".")) {
            extension = original.substring(original.lastIndexOf('.'));
        } else if ("image/png".equalsIgnoreCase(contentType)) {
            extension = ".png";
        } else if ("image/jpeg".equalsIgnoreCase(contentType)) {
            extension = ".jpg";
        } else if ("image/webp".equalsIgnoreCase(contentType)) {
            extension = ".webp";
        }
        if (extension.isBlank()) {
            extension = ".bin";
        }
        return extension;
    }

    @Override
    public Resource cargarArchivo(String filename) {
        try {
            String cleaned = filename.startsWith("/") ? filename.substring(1) : filename;
            if (cleaned.startsWith("files/")) {
                cleaned = cleaned.substring("files/".length());
            }
            Path file = root.resolve(cleaned).normalize();
            if (Files.exists(file)) {
                Resource resource = new UrlResource(file.toUri());
                if (resource.exists() && resource.isReadable()) {
                    return resource;
                }
            }
            // Fallback: si no hay subcarpetas y fue guardado en raíz en versiones anteriores
            if (!cleaned.contains("/")) {
                Path legacy = root.resolve(cleaned).normalize();
                if (Files.exists(legacy)) {
                    Resource legacyRes = new UrlResource(legacy.toUri());
                    if (legacyRes.exists() && legacyRes.isReadable()) {
                        return legacyRes;
                    }
                }
            }
            // Fallback a recursos de classpath (defaults estáticos empacados)
            Resource classpath = new ClassPathResource("static/files/" + cleaned);
            if (classpath.exists() && classpath.isReadable()) {
                return classpath;
            }
            throw new NoSuchFileException(filename);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo cargar el archivo: " + filename, e);
        }
    }

    private void ensureInsideRoot(Path target) throws IOException {
        if (!target.toAbsolutePath().normalize().startsWith(root)) {
            throw new SecurityException("Intento de path traversal fuera del storage");
        }
    }
}
