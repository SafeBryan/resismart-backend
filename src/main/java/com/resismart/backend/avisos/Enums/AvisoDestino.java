package com.resismart.backend.avisos.Enums;

/**
 * Define las distintas categorías de destinatarios a los que un aviso puede dirigirse.
 * Mantener esta enumeración acotada permite a la capa de infraestructura
 * resolver el canal WebSocket adecuado ({@code usuario}, {@code condominio}, etc.).
 */
public enum AvisoDestino {
    USUARIO,
    CONDOMINIO,
    ROL,
    TODOS
}

