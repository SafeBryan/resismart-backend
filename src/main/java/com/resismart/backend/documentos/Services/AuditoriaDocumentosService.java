package com.resismart.backend.documentos.Services;

import com.resismart.backend.documentos.Entities.AuditoriaDocumentos;
import com.resismart.backend.documentos.Entities.Documento;
import com.resismart.backend.documentos.Repositories.AuditoriaDocumentosRepository;
import com.resismart.backend.documentos.Repositories.DocumentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Servicio encargado de registrar y consultar las acciones realizadas
 * sobre los documentos (subidas, validaciones, asociaciones, etc.).
 */
@Service
@RequiredArgsConstructor
public class AuditoriaDocumentosService {

    private final AuditoriaDocumentosRepository auditoriaRepo;
    private final DocumentoRepository documentoRepo;

    /**
     * Registra una nueva acción en la bitácora de auditoría.
     *
     * @param idDocumento ID del documento afectado
     * @param accion Acción realizada (UPLOAD, VALIDACION, ASOCIAR_CONTRATO, etc.)
     * @param idUsuario ID del usuario que la realizó
     * @param detalle Mapa con información adicional (opcional)
     */
    @Transactional
    public void registrar(Integer idDocumento, String accion, Integer idUsuario, Map<String, Object> detalle) {
        Documento documento = documentoRepo.findById(idDocumento)
                .orElseThrow(() -> new IllegalArgumentException("Documento no encontrado para auditoría"));

        AuditoriaDocumentos audit = new AuditoriaDocumentos();
        audit.setDocumento(documento);
        audit.setAccion(accion);
        audit.setRealizadoPor(idUsuario);
        audit.setFecha(Instant.now());
        audit.setDetalle(detalle); // <-- guardar Map como JSONB

        auditoriaRepo.save(audit);
    }

    /**
     * Retorna el historial de acciones registradas para un documento.
     */
    @Transactional(readOnly = true)
    public List<AuditoriaDocumentos> historial(Integer idDocumento) {
        return auditoriaRepo.findByDocumento_IdOrderByFechaDesc(idDocumento);
    }
}
