package com.resismart.backend.documentos.Storage.local;

import com.resismart.backend.documentos.Storage.StoragePort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.UUID;

@Service // 🔹 Esto registra el bean automáticamente
public class LocalStorageService implements StoragePort {

    private final Path root = Paths.get("uploads"); // Carpeta base local

    public LocalStorageService() {
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo crear el directorio de uploads", e);
        }
    }

    @Override
    public String save(String preferredKey, MultipartFile file) {
        try {
            String safeName = (file.getOriginalFilename() != null)
                    ? Path.of(file.getOriginalFilename()).getFileName().toString()
                    : "archivo.bin";

            String key = (preferredKey == null || preferredKey.isBlank())
                    ? UUID.randomUUID() + "-" + safeName
                    : preferredKey;

            Path target = root.resolve(key);
            Files.createDirectories(target.getParent());
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return key;
        } catch (IOException e) {
            throw new RuntimeException("Error al guardar archivo", e);
        }
    }

    @Override
    public InputStream read(String storageKey) {
        try {
            Path path = root.resolve(storageKey);
            return Files.newInputStream(path, StandardOpenOption.READ);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo leer el archivo: " + storageKey, e);
        }
    }

    @Override
    public void delete(String storageKey) {
        try {
            Files.deleteIfExists(root.resolve(storageKey));
        } catch (IOException e) {
            throw new RuntimeException("No se pudo eliminar el archivo: " + storageKey, e);
        }
    }
}
