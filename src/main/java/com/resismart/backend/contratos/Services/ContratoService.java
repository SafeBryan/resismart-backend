package com.resismart.backend.contratos.Services;

import com.resismart.backend.Common.MensajeError;
import com.resismart.backend.contratos.DTO.*;
import com.resismart.backend.contratos.Entities.Contrato;
import com.resismart.backend.contratos.Enums.EstadoContrato;
import com.resismart.backend.contratos.Services.ContratoLifecycleService;
import com.resismart.backend.contratos.Repositories.ContratoRepository;
import com.resismart.backend.condominios.Entities.Unidad;
import com.resismart.backend.condominios.Enums.UnidadEstado;
import com.resismart.backend.condominios.Repositories.UnidadRepository;
import com.resismart.backend.condominios.Services.CondominioService;
import com.resismart.backend.residentes.Entities.Residente;
import com.resismart.backend.residentes.Repositories.ResidenteRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ContratoService {

    private final ContratoRepository contratoRepo;
    private final UnidadRepository unidadRepo;
    private final ResidenteRepository residenteRepo;
    private final ContratoLifecycleService lifecycleService;
    private final CondominioService condominioService;

    /* =========================================================
       Mappers internos (Entidad -> DTO)
       ========================================================= */

    /**
     * Resumen enriquecido para listados (Opción B).
     */
    private ContratoResumenDTO toResumen(Contrato c) {
        // Numero unidad
        Integer idUnidad = c.getUnidad() != null ? c.getUnidad().getId() : null;
        String numeroUnidad = c.getUnidad() != null ? c.getUnidad().getNumero() : null;

        // Nombre residente
        Long idResidente = c.getResidente() != null ? c.getResidente().getId() : null;
        String nombreResidente = Optional.ofNullable(c.getResidente())
                .map(Residente::getUsuario)
                .map(u -> (u.getNombres() == null ? "" : u.getNombres()) + " " +
                        (u.getApellidos() == null ? "" : u.getApellidos()))
                .orElse("")
                .trim();
        if (nombreResidente.isBlank()) nombreResidente = null;

        return new ContratoResumenDTO(
                c.getId(),
                c.getFechaInicio(),
                c.getFechaFin(),
                c.getMonto(),
                c.getMontoAlquiler(),
                c.getMontoAlicuota(),
                c.getEstado(),
                idUnidad,
                numeroUnidad,
                idResidente,
                nombreResidente
        );
    }

    /**
     * Detalle para ver un contrato específico (usa tu DTO existente).
     */
    private ContratoDetalleDTO toDetalle(Contrato c) {
        String nombreResidente = Optional.ofNullable(c.getResidente())
                .map(Residente::getUsuario)
                .map(u -> (u.getNombres() == null ? "" : u.getNombres()) + " " +
                        (u.getApellidos() == null ? "" : u.getApellidos()))
                .orElse("")
                .trim();
        if (nombreResidente.isBlank()) nombreResidente = null;

        return new ContratoDetalleDTO(
                c.getId(),
                c.getFechaInicio(),
                c.getFechaFin(),
                c.getMonto(),
                c.getMontoAlquiler(),
                c.getMontoAlicuota(),
                c.getEstado(),
                c.getUnidad() != null ? c.getUnidad().getId() : null,
                c.getUnidad() != null ? c.getUnidad().getNumero() : null,
                c.getResidente() != null ? c.getResidente().getId() : null,
                nombreResidente
        );
    }

    /* =========================================================
       Helpers de validación/carga
       ========================================================= */

    private Unidad ensureUnidad(Integer idUnidad) {
        return unidadRepo.findById(idUnidad)
                .orElseThrow(() -> new java.util.NoSuchElementException(MensajeError.UNIDAD_NO_ENCONTRADA.getMensaje()));
    }

    private Residente ensureResidente(Long idResidente) {
        return residenteRepo.findById(idResidente)
                .orElseThrow(() -> new java.util.NoSuchElementException(MensajeError.RESIDENTE_NO_ENCONTRADO.getMensaje()));
    }

    /* =========================================================
       Casos de uso (públicos)
       ========================================================= */

    @Transactional
    public ContratoResumenDTO crear(ContratoCreateDTO dto) {
        Unidad u = ensureUnidad(dto.getIdUnidad());
        Residente r = ensureResidente(dto.getIdResidente());

        if (u.getCondominio() != null && u.getCondominio().getId() != null) {
            condominioService.validarCupoUsuariosDisponibles(u.getCondominio().getId());
        }

        if (u.getEstado() != UnidadEstado.LIBRE) {
            throw new IllegalStateException("La unidad ya está ocupada o en mantenimiento");
        }

        Contrato c = Contrato.builder()
                .unidad(u)
                .residente(r)
                .fechaInicio(dto.getFechaInicio())
                .fechaFin(dto.getFechaFin())
                .monto(dto.getMonto())
                .montoAlquiler(dto.getMontoAlquiler())
                .montoAlicuota(dto.getMontoAlicuota())
                .estado(EstadoContrato.ACTIVO)
                .build();

        contratoRepo.save(c);
        u.setEstado(UnidadEstado.OCUPADA);
        return toResumen(c);
    }

    @Transactional
    public ContratoResumenDTO actualizar(Integer id, ContratoUpdateDTO dto) {
        Contrato c = contratoRepo.findById(id)
                .orElseThrow(() -> new java.util.NoSuchElementException(MensajeError.CONTRATO_NO_ENCONTRADO.getMensaje()));

        if (dto.getFechaInicio() != null) c.setFechaInicio(dto.getFechaInicio());
        if (dto.getFechaFin() != null) c.setFechaFin(dto.getFechaFin());
        if (dto.getMonto() != null) c.setMonto(dto.getMonto());
        if (dto.getMontoAlquiler() != null) c.setMontoAlquiler(dto.getMontoAlquiler());
        if (dto.getMontoAlicuota() != null) c.setMontoAlicuota(dto.getMontoAlicuota());
        if (dto.getIdUnidad() != null) c.setUnidad(ensureUnidad(dto.getIdUnidad()));
        if (dto.getIdResidente() != null) c.setResidente(ensureResidente(dto.getIdResidente()));

        return toResumen(c);
    }

    public ContratoDetalleDTO obtener(Integer id) {
        Contrato c = contratoRepo.findWithUnidadAndResidenteById(id)
                .orElseThrow(() -> new java.util.NoSuchElementException(MensajeError.CONTRATO_NO_ENCONTRADO.getMensaje()));
        return toDetalle(c);
    }

    /**
     * 🔴 Ahora usa el query con JOINs para traer unidad/residente/usuario
     * y poder llenar numeroUnidad / nombreResidente sin LazyException.
     */
    public List<ContratoResumenDTO> listarPorResidente(Long idResidente) {
        return contratoRepo.findAllByResidenteIdWithJoins(idResidente)
                .stream()
                .map(this::toResumen)
                .toList();
    }

    public List<ContratoResumenDTO> listarPorEstado(EstadoContrato estado) {
        return contratoRepo.findAllByEstadoWithJoins(estado)
                .stream()
                .map(this::toResumen)
                .toList();
    }

    @Transactional
    public ContratoResumenDTO renovar(Integer id, ContratoRenovarDTO dto) {
        Contrato c = lifecycleService.renovarContrato(Long.valueOf(id), dto.getNuevaFechaFin());
        return toResumen(c);
    }

    @Transactional
    public ContratoResumenDTO rescindir(Integer id, ContratoRescindirDTO dto) {
        Contrato c = lifecycleService.rescindirContrato(Long.valueOf(id), dto != null ? dto.getMotivo() : null);
        return toResumen(c);
    }

    @Transactional
    public void eliminar(Integer id) {
        if (!contratoRepo.existsById(id)) {
            throw new java.util.NoSuchElementException(MensajeError.CONTRATO_NO_ENCONTRADO.getMensaje());
        }
        contratoRepo.deleteById(id);
    }
}
