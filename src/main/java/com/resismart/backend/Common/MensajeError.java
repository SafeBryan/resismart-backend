package com.resismart.backend.Common;

public enum MensajeError {
    EMAIL_REGISTRADO("El email ya está registrado"),
    CLIENTE_NO_ENCONTRADO("Cliente no encontrado"),
    CONTRATO_NO_ENCONTRADO("Contrato no encontrado"),
    CONTRATO_NO_PENDIENTE("Contrato no esta en pendiente"),
    DOCUMENTO_NO_ENCONTRADO("Documento no encontrado"),
    ERROR_ELIMINAR_ARCHIVO("Error al eliminar el archivo"),
    EERROR_ENVIAR_CORREO("Error al enviar correo"),
    USUARIO_NO_ENCONTRADO("Usuario no encontrado"),
    TIPO_ARCHIVO_NO_PERMITIDO("El archivo que desea subir no está permitido solo: pdf, jpg, png"),
    REEMBOLSO_NO_ENCONTRADO("El reembolso no existe"),
    REEMBOLSO_APROBADO("El reembolso solo se puede editar cuando está pendiente o rechazado"),
    PAGO_NO_ENCONTRADO("El pago no se encontro"),
    CONDOMINIO_NO_ENCONTRADO("Condominio no encontrado"),
    UNIDAD_NO_ENCONTRADA("Unidad no encontrada"),
    USUARIO_SIN_ROL_DUENO("El usuario no tiene rol DUEÑO"),
    UNIDAD_DUPLICADA("Ya existe una unidad con ese número en el condominio"),
    UNIDAD_NO_PERTENECE("La unidad no pertenece al condominio"),
    CEDULA_REGISTRADA("La cédula ya está registrada")
            ;


    private final String mensaje;

    MensajeError(String mensaje){
        this.mensaje=mensaje;
    }
    public String getMensaje() {
        return mensaje;
    }
}
