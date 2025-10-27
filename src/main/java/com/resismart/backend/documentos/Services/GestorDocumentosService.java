package com.resismart.backend.documentos.Services;

import com.resismart.backend.documentos.DTO.*;
import com.resismart.backend.documentos.Entities.Documento;
import com.resismart.backend.documentos.Entities.ContratoDocumento;
import com.resismart.backend.documentos.Entities.OrdenDocumento;
import com.resismart.backend.documentos.Enums.EstadoValidacion;
import com.resismart.backend.documentos.Repositories.*;
import com.resismart.backend.documentos.Storage.StoragePort;
import com.resismart.backend.contratos.Entities.Contrato;
import com.resismart.backend.pagos.Entities.OrdenPago;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GestorDocumentosService {

    private final DocumentoRepository documentoRepo;
    private final ContratoDocumentoRepository contratoDocRepo;
    private final OrdenDocumentoRepository ordenDocRepo;
    private final AuditoriaDocumentosService auditoriaSrv;
    private final StoragePort storage;

    @PersistenceContext
    private EntityManager em;

    // Lista blanca de campos permitidos para ordenar (nombres de la ENTIDAD Documento)
    private static final Set<String> CAMPOS_ORDEN = Set.of(
            "id", "fechaSubida", "nombreOriginal", "estadoValidacion", "sizeBytes", "tipo"
    );

    // ============================
    // API principal
    // ============================

    @Transactional
    public DocumentoDetalleDTO upload(DocumentoUploadDTO dto, Integer usuarioId) {
        MultipartFile file = dto.getArchivo();

        String sha256 = Optional.ofNullable(dto.getSha256())
                .orElseGet(() -> calcSha256(file));

        String storageKey = storage.save(null, file);

        Documento d = new Documento();
        d.setTipo(dto.getTipo());
        d.setNombreOriginal(dto.getNombreOriginal());
        d.setStorageKey(storageKey);
        d.setFechaSubida(Instant.now());
        d.setSubidoPor(usuarioId);
        d.setEstadoValidacion(EstadoValidacion.PENDIENTE);
        d.setValidadoPor(null);
        d.setMimeType(dto.getMimeType());
        d.setSizeBytes(
                (dto.getSizeBytes() != null && dto.getSizeBytes() > 0)
                        ? dto.getSizeBytes()
                        : file.getSize()
        );
        d.setSha256(sha256);

        d = documentoRepo.save(d);

        auditoriaSrv.registrar(d.getId(), "UPLOAD", usuarioId, Map.of(
                "mimeType", d.getMimeType(),
                "size", d.getSizeBytes(),
                "sha256", d.getSha256()
        ));

        return toDetalle(d);
    }

    @Transactional(readOnly = true)
    public DocumentoDetalleDTO detalle(Integer idDocumento) {
        Documento d = documentoRepo.findById(idDocumento)
                .orElseThrow(() -> new NoSuchElementException("Documento no encontrado"));
        return toDetalle(d);
    }

    @Transactional(readOnly = true)
    public Page<DocumentoResumenDTO> listar(DocumentoFiltroDTO f) {
        // Saneamos y normalizamos paginación y orden
        int pageNum = Math.max(0, Optional.ofNullable(f.getPage()).orElse(0));
        int size = Optional.ofNullable(f.getSize()).orElse(20);
        size = Math.max(1, Math.min(200, size)); // 1..200

        String sortBySeguro = Optional.ofNullable(f.getSortBy())
                .map(String::trim)
                .filter(CAMPOS_ORDEN::contains)
                .orElse("fechaSubida");

        Sort.Direction dir = "ASC".equalsIgnoreCase(f.getSortDir()) ? Sort.Direction.ASC : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(pageNum, size, Sort.by(dir, sortBySeguro));

        // Cargamos página base (sin filtros complejos; se filtra en memoria con los repos actuales)
        Page<Documento> page = documentoRepo.findAll(pageable);

        // Filtro base (tipo, estado, fechas, subidoPor)
        List<Documento> base = page.getContent().stream()
                .filter(d -> f.getTipo() == null || d.getTipo() == f.getTipo())
                .filter(d -> f.getEstadoValidacion() == null || d.getEstadoValidacion() == f.getEstadoValidacion())
                .filter(d -> f.getFechaDesde() == null || (d.getFechaSubida() != null && !d.getFechaSubida().isBefore(f.getFechaDesde())))
                .filter(d -> f.getFechaHasta() == null || (d.getFechaSubida() != null && !d.getFechaSubida().isAfter(f.getFechaHasta())))
                .filter(d -> {
                    if (f.getSubidoPor() == null || f.getSubidoPor().isBlank()) return true;
                    String subidoPorStr = d.getSubidoPor() == null ? null : String.valueOf(d.getSubidoPor());
                    return f.getSubidoPor().equals(subidoPorStr);
                })
                .collect(Collectors.toList());

        // Filtros por asociación (contrato / orden / tipoRelacion)
        if (f.getIdContrato() != null || f.getIdOrden() != null || f.getTipoRelacion() != null) {
            Set<Integer> allow = new HashSet<>();

            if (f.getIdContrato() != null) {
                if (f.getTipoRelacion() != null) {
                    allow.addAll(
                            contratoDocRepo.findByContratoIdAndTipoRelacion(f.getIdContrato(), f.getTipoRelacion())
                                    .stream().map(cd -> cd.getDocumento().getId()).toList()
                    );
                } else {
                    allow.addAll(
                            contratoDocRepo.findByContratoId(f.getIdContrato())
                                    .stream().map(cd -> cd.getDocumento().getId()).toList()
                    );
                }
            }

            if (f.getIdOrden() != null) {
                if (f.getTipoRelacion() != null) {
                    allow.addAll(
                            ordenDocRepo.findByOrdenIdAndTipoRelacion(f.getIdOrden(), f.getTipoRelacion())
                                    .stream().map(od -> od.getDocumento().getId()).toList()
                    );
                } else {
                    allow.addAll(
                            ordenDocRepo.findByOrdenId(f.getIdOrden())
                                    .stream().map(od -> od.getDocumento().getId()).toList()
                    );
                }
            }

            base = base.stream()
                    .filter(d -> allow.isEmpty() || allow.contains(d.getId()))
                    .collect(Collectors.toList());
        }

        // Nota: como filtramos en memoria, el total devuelto es del subset ya filtrado.
        List<DocumentoResumenDTO> contenido = base.stream().map(this::toResumen).toList();
        return new PageImpl<>(contenido, pageable, contenido.size());
    }

    @Transactional
    public void asociar(DocumentoAsociacionDTO dto, Integer usuarioId) {
        if ((dto.getIdContrato() == null && dto.getIdOrden() == null) ||
                (dto.getIdContrato() != null && dto.getIdOrden() != null)) {
            throw new IllegalArgumentException("Debe enviar exactamente uno: idContrato o idOrden");
        }

        Documento d = documentoRepo.findById(dto.getIdDocumento())
                .orElseThrow(() -> new NoSuchElementException("Documento no encontrado"));

        if (dto.getIdContrato() != null) {
            if (contratoDocRepo.existsByContratoIdAndDocumentoId(dto.getIdContrato(), d.getId())) {
                return;
            }

            ContratoDocumento cd = new ContratoDocumento();
            cd.setContrato(em.getReference(Contrato.class, dto.getIdContrato()));
            cd.setDocumento(d);
            cd.setTipoRelacion(dto.getTipoRelacion());
            contratoDocRepo.save(cd);

            auditoriaSrv.registrar(d.getId(), "ASOCIAR_CONTRATO", usuarioId, Map.of(
                    "idContrato", dto.getIdContrato(),
                    "tipoRelacion", dto.getTipoRelacion().name()
            ));

        } else {
            if (ordenDocRepo.existsByOrdenIdAndDocumentoId(dto.getIdOrden(), d.getId())) {
                return;
            }

            OrdenDocumento od = new OrdenDocumento();
            od.setOrden(em.getReference(OrdenPago.class, dto.getIdOrden()));
            od.setDocumento(d);
            od.setTipoRelacion(dto.getTipoRelacion());
            ordenDocRepo.save(od);

            auditoriaSrv.registrar(d.getId(), "ASOCIAR_ORDEN", usuarioId, Map.of(
                    "idOrden", dto.getIdOrden(),
                    "tipoRelacion", dto.getTipoRelacion().name()
            ));
        }
    }

    // ============================
    // Helpers
    // ============================

    private DocumentoDetalleDTO toDetalle(Documento d) {
        return DocumentoDetalleDTO.builder()
                .idDocumento(d.getId())
                .tipo(d.getTipo())
                .nombreOriginal(d.getNombreOriginal())
                .storageKey(d.getStorageKey())
                .fechaSubida(d.getFechaSubida())
                .subidoPor(d.getSubidoPor() == null ? null : String.valueOf(d.getSubidoPor()))
                .estadoValidacion(d.getEstadoValidacion())
                .validadoPor(d.getValidadoPor() == null ? null : String.valueOf(d.getValidadoPor()))
                .mimeType(d.getMimeType())
                .sizeBytes(d.getSizeBytes() == null ? 0L : d.getSizeBytes())
                .sha256(d.getSha256())
                .build();
    }

    private DocumentoResumenDTO toResumen(Documento d) {
        return DocumentoResumenDTO.builder()
                .idDocumento(d.getId())
                .tipo(d.getTipo())
                .nombreOriginal(d.getNombreOriginal())
                .fechaSubida(d.getFechaSubida())
                .estadoValidacion(d.getEstadoValidacion())
                .sizeBytes(d.getSizeBytes() == null ? 0L : d.getSizeBytes())
                .build();
    }

    private String calcSha256(MultipartFile file) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(file.getBytes());
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("No se pudo calcular SHA-256", e);
        }
    }
}
