package com.resismart.backend.Common;

public enum MensajeError {
    EMAIL_REGISTRADO("El email ya está registrado"),
    CLIENTE_NO_ENCONTRADO("Cliente no encontrado"),
    CONTRATO_NO_ENCONTRADO("Contrato no encontrado"),
    CONTRATO_NO_PENDIENTE("Contrato no está en pendiente"),
    DOCUMENTO_NO_ENCONTRADO("Documento no encontrado"),
    ERROR_ELIMINAR_ARCHIVO("Error al eliminar el archivo"),
    EERROR_ENVIAR_CORREO("Error al enviar correo"),
    USUARIO_NO_ENCONTRADO("Usuario no encontrado"),
    TIPO_ARCHIVO_NO_PERMITIDO("El archivo que desea subir no está permitido solo: pdf, jpg, png"),
    REEMBOLSO_NO_ENCONTRADO("El reembolso no existe"),
    REEMBOLSO_APROBADO("El reembolso solo se puede editar cuando está pendiente o rechazado"),
    PAGO_NO_ENCONTRADO("El pago no se encontró"),
    CONDOMINIO_NO_ENCONTRADO("Condominio no encontrado"),
    UNIDAD_NO_ENCONTRADA("Unidad no encontrada"),
    USUARIO_SIN_ROL_DUENO("El usuario no tiene rol DUEÑO"),
    UNIDAD_DUPLICADA("Ya existe una unidad con ese número en el condominio"),
    UNIDAD_NO_PERTENECE("La unidad no pertenece al condominio"),
    CEDULA_REGISTRADA("La cédula ya está registrada"),
    RESIDENTE_NO_ENCONTRADO("Residente no encontrado"),
    EVENTO_NO_ENCONTRADO("Evento no encontrado"),
    PARTICIPANTE_YA_EXISTE("El usuario ya es participante del evento"),
    PARTICIPANTE_NO_ENCONTRADO("Participante no encontrado"),
    DOCUMENTO_NO_VALIDO("El documento no es válido o no cumple con las reglas definidas"),
    DOCUMENTO_YA_ASOCIADO("El documento ya está asociado a la entidad indicada"),
    DOCUMENTO_NO_ASOCIADO("El documento no está asociado a la entidad especificada"),
    DOCUMENTO_NO_COMPATIBLE("El documento no corresponde al tipo requerido"),
    DOCUMENTO_NO_LEIBLE("No se pudo leer el contenido del documento"),
    DOCUMENTO_NO_GUARDADO("Error al guardar el documento en el almacenamiento"),
    DOCUMENTO_NO_SUBIDO("El archivo no fue recibido correctamente"),
    DOCUMENTO_NO_VALIDADO("El documento aún no ha sido validado"),

    STORAGE_NO_DISPONIBLE("El servicio de almacenamiento no está disponible"),
    STORAGE_ERROR_LECTURA("Error al leer el archivo desde el almacenamiento"),
    STORAGE_ERROR_ESCRITURA("Error al escribir el archivo en el almacenamiento"),
    STORAGE_ERROR_ELIMINAR("Error al eliminar el archivo del almacenamiento"),
    STORAGE_KEY_INVALIDA("La clave de almacenamiento es inválida o no existe");

    private final String mensaje;

    MensajeError(String mensaje) {
        this.mensaje = mensaje;
    }

    public String getMensaje() {
        return mensaje;
    }
}
