package com.resismart.backend.contratos.Services;

import com.resismart.backend.Common.MensajeError;
import com.resismart.backend.contratos.DTO.*;
import com.resismart.backend.contratos.Entities.Contrato;
import com.resismart.backend.contratos.Enums.EstadoContrato;
import com.resismart.backend.contratos.Repositories.ContratoRepository;
import com.resismart.backend.condominios.Entities.Unidad;
import com.resismart.backend.condominios.Enums.UnidadEstado;
import com.resismart.backend.condominios.Repositories.UnidadRepository;
import com.resismart.backend.residentes.Entities.Residente;
import com.resismart.backend.residentes.Repositories.ResidenteRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Servicio de aplicación para la gestión del ciclo de vida de contratos:
 * creación, consulta, actualización, renovación y rescisión básica.
 * <p>
 * Este servicio centraliza las reglas de negocio relacionadas con:
 * <ul>
 *   <li>Validación de existencia de {@link Unidad} y {@link Residente}.</li>
 *   <li>Verificación del estado de la {@link Unidad} (LIBRE/OCUPADA/MANTENIMIENTO) al crear contratos.</li>
 *   <li>Transiciones de estado de {@link Contrato} ({@link EstadoContrato#ACTIVO}, {@link EstadoContrato#RESCINDIDO}).</li>
 *   <li>Impacto sobre la unidad (ocupar o liberar) en operaciones de negocio.</li>
 * </ul>
 *
 * <h3>Transaccionalidad</h3>
 * Los métodos que mutan estado están anotados con {@link Transactional} para asegurar atomicidad
 * y consistencia entre entidades relacionadas (p. ej., contrato y unidad).
 *
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
public class ContratoService {

    private final ContratoRepository contratoRepo;
    private final UnidadRepository unidadRepo;
    private final ResidenteRepository residenteRepo;

    /* =========================================================
       Mappers internos (Entidad -> DTO)
       ========================================================= */

    /**
     * Convierte una entidad {@link Contrato} a su representación resumida.
     *
     * @param c entidad contrato no nula.
     * @return DTO con los datos mínimos de un contrato.
     */
    private ContratoResumenDTO toResumen(Contrato c) {
        return new ContratoResumenDTO(
                c.getId(),
                c.getUnidad().getId(),
                c.getResidente().getId(),
                c.getFechaInicio(),
                c.getFechaFin(),
                c.getMonto(),
                c.getEstado()
        );
    }

    /**
     * Convierte una entidad {@link Contrato} a su representación detallada,
     * incluyendo información de unidad y residente (nombre completo).
     *
     * @param c entidad contrato no nula (se asume cargada con unidad y residente).
     * @return DTO detallado de contrato.
     */
    private ContratoDetalleDTO toDetalle(Contrato c) {
        return new ContratoDetalleDTO(
                c.getId(),
                c.getFechaInicio(),
                c.getFechaFin(),
                c.getMonto(),
                c.getEstado(),
                c.getUnidad().getId(),
                c.getUnidad().getNumero(),
                c.getResidente().getId(),
                c.getResidente().getUsuario().getNombres() + " " + c.getResidente().getUsuario().getApellidos()
        );
    }

    /* =========================================================
       Helpers de validación/carga
       ========================================================= */

    /**
     * Obtiene una {@link Unidad} por su identificador o lanza excepción si no existe.
     *
     * @param idUnidad identificador de la unidad.
     * @return entidad {@link Unidad} persistida.
     * @throws java.util.NoSuchElementException si no se encuentra la unidad.
     */
    private Unidad ensureUnidad(Integer idUnidad) {
        return unidadRepo.findById(idUnidad)
                .orElseThrow(() -> new java.util.NoSuchElementException(MensajeError.UNIDAD_NO_ENCONTRADA.getMensaje()));
    }

    /**
     * Obtiene un {@link Residente} por su identificador o lanza excepción si no existe.
     *
     * @param idResidente identificador del residente (tipo Long).
     * @return entidad {@link Residente} persistida.
     * @throws java.util.NoSuchElementException si no se encuentra el residente.
     */
    private Residente ensureResidente(Long idResidente) {
        return residenteRepo.findById(idResidente)
                .orElseThrow(() -> new java.util.NoSuchElementException(MensajeError.RESIDENTE_NO_ENCONTRADO.getMensaje()));
    }

    /* =========================================================
       Casos de uso (públicos)
       ========================================================= */

    /**
     * Crea un nuevo contrato marcándolo como {@link EstadoContrato#ACTIVO} y
     * ocupa la {@link Unidad} asociada (estado {@link UnidadEstado#OCUPADA}).
     * <p>
     * Reglas:
     * <ul>
     *   <li>La unidad debe existir y estar en estado {@link UnidadEstado#LIBRE}.</li>
     *   <li>El residente debe existir.</li>
     *   <li>Los datos de fechas/monto deben venir validados por {@code ContratoCreateDTO}.</li>
     * </ul>
     *
     * @param dto datos de creación del contrato (unidad, residente, fechas y monto).
     * @return representación resumida del contrato creado.
     * @throws java.util.NoSuchElementException si no existe unidad o residente.
     * @throws IllegalStateException            si la unidad no está {@code LIBRE}.
     */
    @Transactional
    public ContratoResumenDTO crear(ContratoCreateDTO dto) {
        Unidad u = ensureUnidad(dto.getIdUnidad());
        Residente r = ensureResidente(dto.getIdResidente());

        if (u.getEstado() != UnidadEstado.LIBRE) {
            throw new IllegalStateException("La unidad ya está ocupada o en mantenimiento");
        }

        Contrato c = Contrato.builder()
                .unidad(u)
                .residente(r)
                .fechaInicio(dto.getFechaInicio())
                .fechaFin(dto.getFechaFin())
                .monto(dto.getMonto())
                .estado(EstadoContrato.ACTIVO)
                .build();

        contratoRepo.save(c);
        u.setEstado(UnidadEstado.OCUPADA); // Impacto de negocio: ocupar unidad
        return toResumen(c);
    }

    /**
     * Actualiza campos mutables de un contrato existente: fechas, monto, unidad o residente.
     * <p>
     * Consideraciones:
     * <ul>
     *   <li>Si se cambia la unidad, solo se valida existencia (no se modifica el estado de unidades aquí).</li>
     *   <li>Las reglas adicionales (p. ej., impedir mover un contrato ACTIVO a una unidad ocupada)
     *       pueden tratarse en futuras iteraciones según requerimientos.</li>
     * </ul>
     *
     * @param id  identificador del contrato a actualizar.
     * @param dto datos a modificar (parciales).
     * @return representación resumida del contrato actualizado.
     * @throws java.util.NoSuchElementException si el contrato no existe, o si la nueva unidad/residente no existen.
     */
    @Transactional
    public ContratoResumenDTO actualizar(Integer id, ContratoUpdateDTO dto) {
        Contrato c = contratoRepo.findById(id)
                .orElseThrow(() -> new java.util.NoSuchElementException(MensajeError.CONTRATO_NO_ENCONTRADO.getMensaje()));

        if (dto.getFechaInicio() != null) c.setFechaInicio(dto.getFechaInicio());
        if (dto.getFechaFin() != null) c.setFechaFin(dto.getFechaFin());
        if (dto.getMonto() != null) c.setMonto(dto.getMonto());
        if (dto.getIdUnidad() != null) c.setUnidad(ensureUnidad(dto.getIdUnidad()));
        if (dto.getIdResidente() != null) c.setResidente(ensureResidente(dto.getIdResidente()));

        return toResumen(c);
    }

    /**
     * Obtiene el detalle de un contrato (incluye datos de unidad y residente).
     *
     * @param id identificador del contrato.
     * @return DTO detallado del contrato.
     * @throws java.util.NoSuchElementException si el contrato no existe.
     */
    public ContratoDetalleDTO obtener(Integer id) {
        Contrato c = contratoRepo.findWithUnidadAndResidenteById(id)
                .orElseThrow(() -> new java.util.NoSuchElementException(MensajeError.CONTRATO_NO_ENCONTRADO.getMensaje()));
        return toDetalle(c);
    }

    /**
     * Lista los contratos asociados a un residente específico.
     *
     * @param idResidente identificador del residente.
     * @return lista de contratos en formato resumen.
     */
    public List<ContratoResumenDTO> listarPorResidente(Long idResidente) {
        return contratoRepo.findByResidente_Id(idResidente).stream().map(this::toResumen).toList();
    }

    /**
     * Lista los contratos por estado.
     *
     * @param estado estado objetivo (ACTIVO, RESCINDIDO, etc.).
     * @return lista de contratos en formato resumen.
     */
    public List<ContratoResumenDTO> listarPorEstado(EstadoContrato estado) {
        return contratoRepo.findByEstado(estado).stream().map(this::toResumen).toList();
    }

    /**
     * Renueva un contrato activo actualizando su fecha de fin.
     * <p>
     * Reglas:
     * <ul>
     *   <li>Solo contratos en estado {@link EstadoContrato#ACTIVO} pueden renovarse.</li>
     *   <li>La nueva fecha de fin debe ser posterior a la fecha actual.</li>
     * </ul>
     *
     * @param id  identificador del contrato a renovar.
     * @param dto datos de renovación (nueva fecha de fin).
     * @return representación resumida del contrato renovado.
     * @throws java.util.NoSuchElementException si el contrato no existe.
     * @throws IllegalStateException            si el contrato no está ACTIVO.
     * @throws IllegalArgumentException         si la nueva fecha de fin no es futura.
     */
    @Transactional
    public ContratoResumenDTO renovar(Integer id, ContratoRenovarDTO dto) {
        Contrato c = contratoRepo.findById(id)
                .orElseThrow(() -> new java.util.NoSuchElementException(MensajeError.CONTRATO_NO_ENCONTRADO.getMensaje()));

        if (c.getEstado() != EstadoContrato.ACTIVO) {
            throw new IllegalStateException("Solo contratos activos pueden renovarse");
        }
        if (dto.getNuevaFechaFin().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("La nueva fecha de fin debe ser futura");
        }

        c.renovar(dto.getNuevaFechaFin());
        return toResumen(c);
    }

    /**
     * Rescinde un contrato activo y libera la unidad asociada (pasa a {@link UnidadEstado#LIBRE}).
     * <p>
     * Reglas:
     * <ul>
     *   <li>Solo contratos en estado {@link EstadoContrato#ACTIVO} pueden rescindirse.</li>
     *   <li>La fecha de fin del contrato se fija al día actual.</li>
     * </ul>
     *
     * @param id  identificador del contrato a rescindir.
     * @param dto datos adicionales (motivo, si aplica a auditoría futura).
     * @return representación resumida del contrato rescindido.
     * @throws java.util.NoSuchElementException si el contrato no existe.
     * @throws IllegalStateException            si el contrato no está ACTIVO.
     */
    @Transactional
    public ContratoResumenDTO rescindir(Integer id, ContratoRescindirDTO dto) {
        Contrato c = contratoRepo.findById(id)
                .orElseThrow(() -> new java.util.NoSuchElementException(MensajeError.CONTRATO_NO_ENCONTRADO.getMensaje()));

        if (c.getEstado() != EstadoContrato.ACTIVO) {
            throw new IllegalStateException("Solo contratos activos pueden rescindirse");
        }

        c.rescindir();
        c.getUnidad().setEstado(UnidadEstado.LIBRE); // Impacto de negocio: liberar unidad
        return toResumen(c);
    }

    /**
     * Elimina un contrato por identificador.
     * <p>
     * Nota: Esta operación realiza una eliminación física. Si se requiere auditoría
     * o políticas de retención, considerar baja lógica o soft-delete en futuras iteraciones.
     *
     * @param id identificador del contrato a eliminar.
     * @throws java.util.NoSuchElementException si el contrato no existe.
     */
    @Transactional
    public void eliminar(Integer id) {
        if (!contratoRepo.existsById(id)) {
            throw new java.util.NoSuchElementException(MensajeError.CONTRATO_NO_ENCONTRADO.getMensaje());
        }
        contratoRepo.deleteById(id);
    }
}
