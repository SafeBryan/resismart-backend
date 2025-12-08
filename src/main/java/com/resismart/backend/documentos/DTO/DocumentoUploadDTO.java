package com.resismart.backend.documentos.DTO;

import com.resismart.backend.documentos.Enums.TipoDocumento;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * DTO para la carga de un documento (archivo + metadatos mínimos).
 * Se usa en el endpoint de subida. El hash (sha256) puede enviarse
 * para verificación de integridad del lado servidor.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentoUploadDTO {

    /** Tipo funcional del documento (CONTRATO, COMPROBANTE, OTRO). */
    @NotNull
    private TipoDocumento tipo;

    /** Archivo a subir (debe venir en multipart/form-data). */
    @NotNull(message = "El archivo es obligatorio")
    private MultipartFile archivo;

    /** Nombre original reportado por el cliente (informativo). */
    @NotBlank
    @Size(max = 255)
    private String nombreOriginal;

    /** Tipo MIME detectado/enviado. */
    @Size(max = 100)
    private String mimeType;

    /** Tamaño del archivo en bytes (opcional, el backend puede calcularlo). */
    @Positive
    private Long sizeBytes; // <- cambiado de 'long' a 'Long' para permitir null

    /** Hash SHA-256 en hexadecimal para verificación (opcional pero recomendado). */
    @Pattern(regexp = "^[a-fA-F0-9]{64}$", message = "SHA-256 inválido")
    private String sha256;

    /** Usuario responsable de la subida (cedula/username). */
    @Size(max = 100)
    private String subidoPor;
}
