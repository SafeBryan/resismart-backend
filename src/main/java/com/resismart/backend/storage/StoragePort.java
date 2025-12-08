package com.resismart.backend.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface StoragePort {
    /**
    * Guarda el archivo renombrándolo con UUID para evitar colisiones.
    * @return nombre físico almacenado.
    */
    String guardarArchivo(MultipartFile file);

    /**
     * Guarda un archivo dentro de un subdirectorio opcional (p.ej. users/{id}/comprobantes).
     * La implementación debe normalizar y crear las carpetas necesarias.
     * @return ruta relativa dentro del storage (sin prefijo /files).
     */
    default String guardarArchivoEn(MultipartFile file, String subPath) {
        // Implementaciones antiguas pueden delegar al método base si no necesitan carpeta.
        return guardarArchivo(file);
    }

    /**
    * Carga un archivo como recurso para servirlo vía HTTP.
    */
    Resource cargarArchivo(String filename);
}
