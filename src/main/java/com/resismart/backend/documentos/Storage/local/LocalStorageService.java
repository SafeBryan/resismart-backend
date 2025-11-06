package com.resismart.backend.documentos.Storage.local;

import com.resismart.backend.documentos.Storage.StorageNotFoundException;
import com.resismart.backend.documentos.Storage.StoragePort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.UUID;

@Service
public class LocalStorageService implements StoragePort {

    // Configurable por application.yml: storage.local.base-path: uploads
    @Value("${storage.local.base-path:uploads}")
    private String basePath;

    private Path root; // absoluto y normalizado

    @PostConstruct
    void init() {
        try {
            this.root = Paths.get(basePath).toAbsolutePath().normalize();
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo crear el directorio base de storage: " + basePath, e);
        }
    }

    @Override
    public String save(String preferredKey, MultipartFile file) {
        try {
            String original = (file.getOriginalFilename() != null)
                    ? Paths.get(file.getOriginalFilename()).getFileName().toString()
                    : "archivo.bin";

            // Nombre seguro (evita caracteres problemáticos)
            String safeOriginal = original.replaceAll("[\\\\/:*?\"<>|]+", "_").trim();
            if (safeOriginal.isBlank()) safeOriginal = "archivo.bin";

            // Si te pasan una key, úsala; caso contrario crea una “uuid-nombre”
            String key = (preferredKey == null || preferredKey.isBlank())
                    ? (UUID.randomUUID() + "-" + safeOriginal)
                    : preferredKey;

            // Normalizar y verificar que NO escape de la carpeta base
            Path target = root.resolve(key).normalize();
            checkInsideRoot(target);

            Files.createDirectories(target.getParent());
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            // Devuelve la clave relativa (con /)
            return root.relativize(target).toString().replace('\\', '/');

        } catch (IOException e) {
            throw new RuntimeException("Error al guardar archivo", e);
        }
    }

    @Override
    public InputStream read(String storageKey) {
        try {
            Path path = root.resolve(storageKey).normalize();
            checkInsideRoot(path);
            if (!Files.exists(path) || !Files.isRegularFile(path)) {
                throw new StorageNotFoundException(storageKey);
            }
            return Files.newInputStream(path, StandardOpenOption.READ);
        } catch (StorageNotFoundException e) {
            throw e; // deja que el controller lo convierta en 404
        } catch (IOException e) {
            throw new RuntimeException("No se pudo leer el archivo: " + storageKey, e);
        }
    }

    @Override
    public boolean exists(String storageKey) {
        Path path = root.resolve(storageKey).normalize();
        try {
            checkInsideRoot(path);
            return Files.exists(path) && Files.isRegularFile(path) && Files.isReadable(path);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void delete(String storageKey) {
        try {
            Path path = root.resolve(storageKey).normalize();
            checkInsideRoot(path);
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo eliminar el archivo: " + storageKey, e);
        }
    }

    private void checkInsideRoot(Path path) {
        // Anti path traversal: la ruta destino DEBE empezar por el root
        if (!path.startsWith(root)) {
            throw new SecurityException("Path traversal detectado fuera de " + root);
        }
    }
}
