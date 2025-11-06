package com.resismart.backend.documentos.Storage;

public class StorageNotFoundException extends RuntimeException {
    public StorageNotFoundException(String key) {
        super("Contenido no encontrado: " + key);
    }
}
