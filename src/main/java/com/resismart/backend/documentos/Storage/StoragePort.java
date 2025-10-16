package com.resismart.backend.documentos.Storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * Puerto de almacenamiento para guardar y leer archivos binarios.
 * Implementaciones posibles: S3/MinIO, FileSystem, etc.
 */
public interface StoragePort {

    /**
     * Guarda el archivo y retorna la clave interna (storageKey).
     */
    String save(String preferredKey, MultipartFile file);

    /**
     * Obtiene un stream de lectura del contenido.
     */
    InputStream read(String storageKey);

    /**
     * Elimina el contenido (opcional).
     */
    default void delete(String storageKey) {}
}
