package com.resismart.backend.documentos.Services;

import com.resismart.backend.documentos.DTO.AuditoriaDocDTO;
import com.resismart.backend.documentos.Entities.AuditoriaDocumentos;
import com.resismart.backend.documentos.Entities.Documento;
import com.resismart.backend.documentos.Repositories.AuditoriaDocumentosRepository;
import com.resismart.backend.documentos.Repositories.DocumentoRepository;
import com.resismart.backend.users.Entities.Usuario; // 👈 tu entidad real
import com.resismart.backend.users.Repositories.UsuarioRepository; // 👈 tu repo real
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Servicio encargado de registrar y consultar las acciones realizadas
 * sobre los documentos (subidas, validaciones, asociaciones, etc.).
 */
@Service
@RequiredArgsConstructor
public class AuditoriaDocumentosService {

    private final AuditoriaDocumentosRepository auditoriaRepo;
    private final DocumentoRepository documentoRepo;
    private final UsuarioRepository usuarioRepo; // 🔗 para resolver nombres

    /**
     * Registra una nueva acción en la bitácora de auditoría.
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
        audit.setDetalle(detalle);

        auditoriaRepo.save(audit);
    }

    /**
     * Devuelve el historial de acciones registradas para un documento,
     * resolviendo los nombres de usuario en LOTE (sin N+1).
     */
    @Transactional(readOnly = true)
    public List<AuditoriaDocDTO> historial(Integer idDocumento) {
        List<AuditoriaDocumentos> audits =
                auditoriaRepo.findByDocumento_IdOrderByFechaDesc(idDocumento);

        // 1) IDs únicos de usuario presentes en la auditoría
        Set<Integer> userIds = audits.stream()
                .map(AuditoriaDocumentos::getRealizadoPor)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 2) Traer usuarios en una sola consulta
        Map<Integer, String> nombrePorId = userIds.isEmpty()
                ? Collections.emptyMap()
                : usuarioRepo.findAllById(userIds).stream()
                .collect(Collectors.toMap(
                        u -> Integer.valueOf(u.getId_usuario()),  // ← OJO: getId_usuario()
                        this::nombreCompletoSeguro
                ));

        // 3) Mapear a DTO seteando el nombre ya resuelto
        return audits.stream()
                .map(a -> toDTO(a, nombrePorId.get(a.getRealizadoPor())))
                .toList();
    }

    /** Construye "nombres apellidos" con fallback al correo si no hay nombres. */
    private String nombreCompletoSeguro(Usuario u) {
        String nombres = safe(u.getNombres());
        String apellidos = safe(u.getApellidos());
        String full = (nombres + " " + apellidos).trim();
        if (!full.isEmpty()) return full;

        // Fallback: correo (getUsername() devuelve correo, pero usemos el campo para claridad)
        String correo = safe(u.getCorreo());
        if (!correo.isEmpty()) return correo;

        // Último fallback: ID como texto
        return String.valueOf(u.getId_usuario());
    }

    private String safe(String s) { return s == null ? "" : s.trim(); }

    /** Crea el DTO con nombre ya resuelto (puede ser null si no se encontró usuario). */
    private AuditoriaDocDTO toDTO(AuditoriaDocumentos audit, String nombreUsuario) {
        Integer docId = (audit.getDocumento() != null) ? audit.getDocumento().getId() : null;

        return AuditoriaDocDTO.builder()
                .id(audit.getId())
                .documentoId(docId)
                .accion(audit.getAccion())
                .realizadoPor(nombreUsuario)   // ← aquí ya va el nombre
                .fecha(audit.getFecha())
                .detalle(audit.getDetalle())
                .build();
    }
}
