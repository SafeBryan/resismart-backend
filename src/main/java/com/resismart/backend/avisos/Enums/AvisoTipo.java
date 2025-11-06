package com.resismart.backend.avisos.Enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Catálogo central de avisos que el backend puede emitir.
 * Cada entrada define un título por defecto que el cliente puede mostrar directamente.
 */
@Getter
@RequiredArgsConstructor
public enum AvisoTipo {
    EVENTO_NUEVO("Nuevo evento"),
    EVENTO_ACTUALIZADO("Evento actualizado"),
    EVENTO_CANCELADO("Evento cancelado"),
    ORDEN_PAGO_GENERADA("Orden de pago generada"),
    ORDEN_PAGO_PROX_VENCER("Pago próximo a vencer"),
    ORDEN_PAGO_PAGADA("Pago registrado"),
    DOCUMENTO_ASOCIADO("Documento asociado a orden"),
    DOCUMENTO_RECHAZADO("Documento rechazado"),
    DOCUMENTO_APROBADO("Documento aprobado"),
    ALERTA_GENERAL("Aviso general");

    private final String tituloDefecto;
}

