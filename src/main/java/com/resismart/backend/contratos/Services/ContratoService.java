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

@Service
@RequiredArgsConstructor
public class ContratoService {

    private final ContratoRepository contratoRepo;
    private final UnidadRepository unidadRepo;
    private final ResidenteRepository residenteRepo;

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

    private Unidad ensureUnidad(Integer idUnidad) {
        return unidadRepo.findById(idUnidad)
                .orElseThrow(() -> new java.util.NoSuchElementException(MensajeError.UNIDAD_NO_ENCONTRADA.getMensaje()));
    }

    private Residente ensureResidente(Long idResidente) {
        return residenteRepo.findById(idResidente)
                .orElseThrow(() -> new java.util.NoSuchElementException(MensajeError.RESIDENTE_NO_ENCONTRADO.getMensaje()));
    }

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
        if (dto.getIdUnidad() != null) c.setUnidad(ensureUnidad(dto.getIdUnidad()));
        if (dto.getIdResidente() != null) c.setResidente(ensureResidente(dto.getIdResidente()));

        return toResumen(c);
    }

    public ContratoDetalleDTO obtener(Integer id) {
        Contrato c = contratoRepo.findWithUnidadAndResidenteById(id)
                .orElseThrow(() -> new java.util.NoSuchElementException(MensajeError.CONTRATO_NO_ENCONTRADO.getMensaje()));
        return toDetalle(c);
    }

    public List<ContratoResumenDTO> listarPorResidente(Long idResidente) {
        return contratoRepo.findByResidente_Id(idResidente).stream().map(this::toResumen).toList();
    }

    public List<ContratoResumenDTO> listarPorEstado(EstadoContrato estado) {
        return contratoRepo.findByEstado(estado).stream().map(this::toResumen).toList();
    }

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

    @Transactional
    public ContratoResumenDTO rescindir(Integer id, ContratoRescindirDTO dto) {
        Contrato c = contratoRepo.findById(id)
                .orElseThrow(() -> new java.util.NoSuchElementException(MensajeError.CONTRATO_NO_ENCONTRADO.getMensaje()));

        if (c.getEstado() != EstadoContrato.ACTIVO) {
            throw new IllegalStateException("Solo contratos activos pueden rescindirse");
        }

        c.rescindir();
        c.getUnidad().setEstado(UnidadEstado.LIBRE); // liberar la unidad
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
