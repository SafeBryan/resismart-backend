package com.resismart.backend.documentos.Services;

import com.resismart.backend.documentos.DTO.DocumentoValidacionDTO;
import com.resismart.backend.documentos.Entities.Documento;
import com.resismart.backend.documentos.Enums.EstadoValidacion;
import com.resismart.backend.documentos.Repositories.DocumentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ValidacionDocumentosService {

    private final DocumentoRepository documentoRepo;
    private final AuditoriaDocumentosService auditoriaSrv;

    @Transactional
    public void validar(DocumentoValidacionDTO dto) {
        // --- Validaciones básicas ---
        Objects.requireNonNull(dto, "Payload de validación requerido");
        Objects.requireNonNull(dto.getIdDocumento(), "idDocumento es requerido");
        Objects.requireNonNull(dto.getNuevoEstado(), "nuevoEstado es requerido");
        Objects.requireNonNull(dto.getValidadoPor(), "validadoPor es requerido");

        // Convertir String -> Integer (las entidades y auditoría usan Integer)
        final Integer validadoPorId = parseUsuarioId(dto.getValidadoPor());

        Documento d = documentoRepo.findById(dto.getIdDocumento())
                .orElseThrow(() -> new NoSuchElementException("Documento no encontrado"));

        if (dto.getNuevoEstado() == EstadoValidacion.PENDIENTE) {
            throw new IllegalArgumentException("Transición inválida: no se puede volver a PENDIENTE");
        }
        if (dto.getNuevoEstado() == EstadoValidacion.RECHAZADO &&
                (dto.getObservacion() == null || dto.getObservacion().isBlank())) {
            throw new IllegalArgumentException("Observación es obligatoria al RECHAZAR");
        }

        // Idempotencia
        if (dto.getNuevoEstado().equals(d.getEstadoValidacion())) {
            return;
        }

        // Persistir cambio
        d.setEstadoValidacion(dto.getNuevoEstado());
        d.setValidadoPor(validadoPorId); // <-- ahora Integer
        documentoRepo.save(d);

        // Armar detalle sin nulls
        Map<String, Object> detalle = new HashMap<>();
        detalle.put("nuevoEstado", dto.getNuevoEstado().name());
        if (dto.getObservacion() != null && !dto.getObservacion().isBlank()) {
            detalle.put("observacion", dto.getObservacion());
        }

        // Auditoría con Integer idUsuario
        auditoriaSrv.registrar(d.getId(), "VALIDACION", validadoPorId, detalle);
    }

    private Integer parseUsuarioId(String raw) {
        try {
            return Integer.valueOf(raw.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("El campo 'validadoPor' debe ser un ID numérico", ex);
        }
    }
}
