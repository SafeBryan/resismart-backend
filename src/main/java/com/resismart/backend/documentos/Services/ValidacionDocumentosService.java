package com.resismart.backend.documentos.Services;

import com.resismart.backend.avisos.Enums.AvisoTipo;
import com.resismart.backend.avisos.Services.AvisoService;
import com.resismart.backend.documentos.DTO.DocumentoValidacionDTO;
import com.resismart.backend.documentos.Entities.Documento;
import com.resismart.backend.documentos.Entities.OrdenDocumento;
import com.resismart.backend.documentos.Enums.EstadoValidacion;
import com.resismart.backend.documentos.Repositories.DocumentoRepository;
import com.resismart.backend.documentos.Repositories.OrdenDocumentoRepository;
import com.resismart.backend.pagos.Entities.OrdenPago;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ValidacionDocumentosService {

    private final DocumentoRepository documentoRepo;
    private final OrdenDocumentoRepository ordenDocumentoRepository;
    private final AuditoriaDocumentosService auditoriaSrv;
    private final AvisoService avisoService;

    @Transactional
    public void validar(DocumentoValidacionDTO dto) {
        Objects.requireNonNull(dto, "Payload de validación requerido");
        Objects.requireNonNull(dto.getIdDocumento(), "idDocumento es requerido");
        Objects.requireNonNull(dto.getNuevoEstado(), "nuevoEstado es requerido");
        Objects.requireNonNull(dto.getValidadoPor(), "validadoPor es requerido");

        final Integer validadoPorId = parseUsuarioId(dto.getValidadoPor());

        Documento documento = documentoRepo.findById(dto.getIdDocumento())
                .orElseThrow(() -> new NoSuchElementException("Documento no encontrado"));

        if (dto.getNuevoEstado() == EstadoValidacion.PENDIENTE) {
            throw new IllegalArgumentException("Transición inválida: no se puede volver a PENDIENTE");
        }
        if (dto.getNuevoEstado() == EstadoValidacion.RECHAZADO &&
                (dto.getObservacion() == null || dto.getObservacion().isBlank())) {
            throw new IllegalArgumentException("Observación es obligatoria al RECHAZAR");
        }

        if (dto.getNuevoEstado().equals(documento.getEstadoValidacion())) {
            return;
        }

        documento.setEstadoValidacion(dto.getNuevoEstado());
        documento.setValidadoPor(validadoPorId);
        documentoRepo.save(documento);

        Map<String, Object> detalle = new HashMap<>();
        detalle.put("nuevoEstado", dto.getNuevoEstado().name());
        if (dto.getObservacion() != null && !dto.getObservacion().isBlank()) {
            detalle.put("observacion", dto.getObservacion());
        }

        auditoriaSrv.registrar(documento.getId(), "VALIDACION", validadoPorId, detalle);
        emitirAvisosValidacion(documento, dto, validadoPorId);
    }

    private void emitirAvisosValidacion(Documento documento,
                                        DocumentoValidacionDTO dto,
                                        Integer validadoPorId) {
        AvisoTipo tipo = (dto.getNuevoEstado() == EstadoValidacion.RECHAZADO)
                ? AvisoTipo.DOCUMENTO_RECHAZADO
                : AvisoTipo.DOCUMENTO_APROBADO;

        String mensaje = (tipo == AvisoTipo.DOCUMENTO_RECHAZADO)
                ? String.format("Tu documento \"%s\" fue rechazado. %s",
                documento.getNombreOriginal(),
                dto.getObservacion() != null ? "Motivo: " + dto.getObservacion() : "")
                : String.format("Tu documento \"%s\" fue aprobado.", documento.getNombreOriginal());

        Map<String, Object> metadataBase = new HashMap<>();
        metadataBase.put("documentoId", documento.getId());
        metadataBase.put("estado", documento.getEstadoValidacion());
        metadataBase.put("validadoPor", validadoPorId);
        if (dto.getObservacion() != null && !dto.getObservacion().isBlank()) {
            metadataBase.put("observacion", dto.getObservacion());
        }

        Integer creadorId = documento.getSubidoPor();
        if (creadorId != null) {
            Map<String, Object> metadata = new HashMap<>(metadataBase);
            metadata.put("usuarioId", creadorId);
            avisoService.enviarAvisoUsuario(
                    creadorId,
                    tipo,
                    tipo.getTituloDefecto(),
                    mensaje,
                    metadata
            );
        } else {
            avisoService.enviarAvisoBroadcast(
                    tipo,
                    tipo.getTituloDefecto(),
                    mensaje,
                    metadataBase
            );
        }

        List<OrdenDocumento> asociaciones = ordenDocumentoRepository.findByDocumento(documento);
        for (OrdenDocumento relacion : asociaciones) {
            OrdenPago orden = relacion.getOrden();
            if (orden == null || orden.getContrato() == null) {
                continue;
            }

            var contrato = orden.getContrato();
            if (contrato.getResidente() != null && contrato.getResidente().getUsuario() != null) {
                Integer usuarioId = contrato.getResidente().getUsuario().getId_usuario();
                if (usuarioId != null && !usuarioId.equals(creadorId)) {
                    Map<String, Object> metadata = new HashMap<>(metadataBase);
                    metadata.put("ordenPagoId", orden.getId());
                    metadata.put("contratoId", contrato.getId());
                    metadata.put("usuarioId", usuarioId);
                    avisoService.enviarAvisoUsuario(
                            usuarioId,
                            tipo,
                            tipo.getTituloDefecto(),
                            mensaje,
                            metadata
                    );
                }
            }

            if (contrato.getUnidad() != null && contrato.getUnidad().getCondominio() != null) {
                Integer condominioId = contrato.getUnidad().getCondominio().getId();
                Map<String, Object> metadata = new HashMap<>(metadataBase);
                metadata.put("ordenPagoId", orden.getId());
                metadata.put("contratoId", contrato.getId());
                metadata.put("condominioId", condominioId);
                avisoService.enviarAvisoCondominio(
                        condominioId,
                        tipo,
                        tipo.getTituloDefecto(),
                        mensaje,
                        metadata
                );
            }
        }
    }

    private Integer parseUsuarioId(String raw) {
        try {
            return Integer.valueOf(raw.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("El campo 'validadoPor' debe ser un ID numérico", ex);
        }
    }
}

