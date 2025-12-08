package com.resismart.backend.contratos.Controllers;

import com.resismart.backend.contratos.DTO.*;
import com.resismart.backend.contratos.Enums.EstadoContrato;
import com.resismart.backend.contratos.Services.ContratoService;
import com.resismart.backend.contratos.Repositories.ContratoRepository;
import com.resismart.backend.documentos.Services.PdfService;
import com.resismart.backend.contratos.Entities.Contrato;
import com.resismart.backend.users.Entities.Usuario;
import com.resismart.backend.users.Enums.Rol;
import com.resismart.backend.users.Repositories.UsuarioRepository;
import com.resismart.backend.condominios.Repositories.UnidadRepository;
import com.resismart.backend.condominios.Services.CondominioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador REST para la gestión de contratos de arrendamiento.
 * <p>
 * Expone operaciones de creación, consulta, actualización y eliminación de contratos,
 * además de acciones de negocio como la renovación y la rescisión básica.
 * </p>
 *
 * <h3>Convenciones</h3>
 * <ul>
 *   <li>Prefijo base: <code>/Contratos</code></li>
 *   <li>Formato de entrada/salida: <code>application/json</code></li>
 *   <li>Manejo de errores: cuerpo JSON con la clave <code>error</code> y código HTTP apropiado.</li>
 * </ul>
 *
 * @since 1.0
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/Contratos")
public class ContratoController {

    private final ContratoService service;
    private final ContratoRepository contratoRepository;
    private final PdfService pdfService;
    private final UsuarioRepository usuarioRepository;
    private final UnidadRepository unidadRepository;
    private final CondominioService condominioService;

    private ResponseEntity<?> checkAccesoContratoPorUnidad(Integer unidadId, org.springframework.security.core.Authentication authentication) {
        if (authentication == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Usuario current = usuarioRepository.findByCorreo(authentication.getName()).orElse(null);
        if (current == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (current.getRol() == Rol.ADMIN) return null;
        if (current.getRol() == Rol.DUEÑO && unidadId != null) {
            com.resismart.backend.condominios.Entities.Unidad unidad = unidadRepository.findById(unidadId)
                    .orElseThrow(() -> new java.util.NoSuchElementException("Unidad no encontrada"));
            Integer condominioId = unidad.getCondominio() != null ? unidad.getCondominio().getId() : null;
            if (condominioId != null && condominioService.esDuenoDeCondominio(condominioId, current.getId_usuario())) {
                return null;
            }
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Sin acceso"));
    }

    private ResponseEntity<?> checkAccesoContratoPorId(Integer contratoId, org.springframework.security.core.Authentication authentication) {
        Contrato contrato = contratoRepository.findWithUnidadAndResidenteById(contratoId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Contrato no encontrado"));
        Integer unidadId = contrato.getUnidad() != null ? contrato.getUnidad().getId() : null;
        return checkAccesoContratoPorUnidad(unidadId, authentication);
    }


    /**
     * Crea un nuevo contrato.
     * <p>
     * Reglas principales:
     * <ul>
     *   <li>La unidad debe existir y estar LIBRE.</li>
     *   <li>El residente debe existir.</li>
     * </ul>
     *
     * @param dto cuerpo de la petición con los datos de creación del contrato.
     * @return <ul>
     *   <li><b>201 Created</b> con {@link ContratoResumenDTO} del contrato creado.</li>
     *   <li><b>400 Bad Request</b> si las validaciones fallan (por ejemplo, unidad no LIBRE).</li>
     *   <li><b>500 Internal Server Error</b> ante errores inesperados.</li>
     * </ul>
     */
    @PostMapping
    public ResponseEntity<?> crear(@Valid @RequestBody ContratoCreateDTO dto,
                                   org.springframework.security.core.Authentication authentication) {
        try {
            ResponseEntity<?> denied = checkAccesoContratoPorUnidad(dto.getIdUnidad(), authentication);
            if (denied != null) return denied;
            return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (java.util.NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Obtiene el detalle de un contrato por su identificador.
     *
     * @param id identificador único del contrato.
     * @return <ul>
     *   <li><b>200 OK</b> con {@link ContratoDetalleDTO}.</li>
     *   <li><b>404 Not Found</b> si el contrato no existe.</li>
     * </ul>
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> obtener(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(service.obtener(id));
        } catch (java.util.NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Actualiza campos mutables de un contrato (fechas, monto, unidad, residente).
     *
     * @param id  identificador del contrato a actualizar.
     * @param dto cuerpo con los campos a modificar (parciales).
     * @return <ul>
     *   <li><b>200 OK</b> con {@link ContratoResumenDTO} actualizado.</li>
     *   <li><b>404 Not Found</b> si el contrato no existe.</li>
     *   <li><b>400 Bad Request</b> si las validaciones fallan.</li>
     * </ul>
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Integer id,
                                        @Valid @RequestBody ContratoUpdateDTO dto,
                                        org.springframework.security.core.Authentication authentication) {
        try {
            ResponseEntity<?> denied = checkAccesoContratoPorId(id, authentication);
            if (denied != null) return denied;
            if (dto.getIdUnidad() != null) {
                denied = checkAccesoContratoPorUnidad(dto.getIdUnidad(), authentication);
                if (denied != null) return denied;
            }
            return ResponseEntity.ok(service.actualizar(id, dto));
        } catch (java.util.NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Elimina un contrato por su identificador.
     * <p>Operación de eliminación física.</p>
     *
     * @param id identificador del contrato a eliminar.
     * @return <ul>
     *   <li><b>204 No Content</b> si se eliminó correctamente.</li>
     *   <li><b>404 Not Found</b> si el contrato no existe.</li>
     * </ul>
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Integer id,
                                      org.springframework.security.core.Authentication authentication) {
        try {
            ResponseEntity<?> denied = checkAccesoContratoPorId(id, authentication);
            if (denied != null) return denied;
            service.eliminar(id);
            return ResponseEntity.noContent().build();
        } catch (java.util.NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    /* ==== Consultas ==== */

    /**
     * Lista contratos asociados a un residente.
     *
     * @param idResidente identificador del residente.
     * @return <b>200 OK</b> con lista de {@link ContratoResumenDTO}.
     */
    @GetMapping("/residente/{idResidente}")
    public ResponseEntity<List<ContratoResumenDTO>> listarPorResidente(@PathVariable Long idResidente) {
        return ResponseEntity.ok(service.listarPorResidente(idResidente));
    }

    /**
     * Lista contratos por estado.
     *
     * @param estado estado objetivo (p. ej., ACTIVO, RESCINDIDO).
     * @return <b>200 OK</b> con lista de {@link ContratoResumenDTO}.
     */
    @GetMapping("/estado/{estado}")
    public ResponseEntity<List<ContratoResumenDTO>> listarPorEstado(@PathVariable EstadoContrato estado) {
        return ResponseEntity.ok(service.listarPorEstado(estado));
    }

    /* ==== Acciones de negocio ==== */

    /**
     * Renueva un contrato activo actualizando su fecha de fin.
     *
     * @param id  identificador del contrato a renovar.
     * @param dto cuerpo con la nueva fecha de fin.
     * @return <ul>
     *   <li><b>200 OK</b> con {@link ContratoResumenDTO} renovado.</li>
     *   <li><b>400 Bad Request</b> si la nueva fecha es inválida.</li>
     *   <li><b>404 Not Found</b> si el contrato no existe.</li>
     * </ul>
     */
    @PostMapping("/{id}/renovar")
    public ResponseEntity<?> renovar(@PathVariable Integer id,
                                     @Valid @RequestBody ContratoRenovarDTO dto,
                                     org.springframework.security.core.Authentication authentication) {
        try {
            ResponseEntity<?> denied = checkAccesoContratoPorId(id, authentication);
            if (denied != null) return denied;
            return ResponseEntity.ok(service.renovar(id, dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (java.util.NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Rescinde un contrato activo y libera la unidad asociada.
     *
     * @param id  identificador del contrato a rescindir.
     * @param dto cuerpo con datos adicionales (motivo, opcional).
     * @return <ul>
     *   <li><b>200 OK</b> con {@link ContratoResumenDTO} rescindido.</li>
     *   <li><b>400 Bad Request</b> si la operación no cumple reglas de negocio.</li>
     *   <li><b>404 Not Found</b> si el contrato no existe.</li>
     * </ul>
     */
    @PostMapping("/{id}/rescindir")
    public ResponseEntity<?> rescindir(@PathVariable Integer id,
                                       @RequestBody ContratoRescindirDTO dto,
                                       org.springframework.security.core.Authentication authentication) {
        try {
            ResponseEntity<?> denied = checkAccesoContratoPorId(id, authentication);
            if (denied != null) return denied;
            return ResponseEntity.ok(service.rescindir(id, dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (java.util.NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}/descargar-pdf")
    public ResponseEntity<?> descargarPdf(@PathVariable Integer id) {
        try {
            Contrato contrato = contratoRepository.findWithUnidadAndResidenteById(id)
                    .orElseThrow(() -> new java.util.NoSuchElementException("Contrato no encontrado"));
            byte[] pdf = pdfService.generarContratoPdf(contrato);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=contrato_" + id + ".pdf")
                    .body(pdf);
        } catch (java.util.NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }
}
