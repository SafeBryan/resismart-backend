package com.resismart.backend.condominios.Services;


import com.resismart.backend.Common.MensajeError;
import com.resismart.backend.condominios.DTO.*;
import com.resismart.backend.condominios.Entities.*;
import com.resismart.backend.condominios.Repositories.*;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UnidadService {

    private final UnidadRepository unidadRepo;
    private final CondominioRepository condominioRepo;

    private UnidadResumenDTO toDTO(Unidad u) {
        return new UnidadResumenDTO(u.getId(), u.getNumero(), u.getEstado());
    }

    @Transactional
    public UnidadResumenDTO crear(UnidadCreateDTO dto) {
        Condominio c = condominioRepo.findById(dto.getIdCondominio())
                .orElseThrow(() -> new java.util.NoSuchElementException(MensajeError.CONDOMINIO_NO_ENCONTRADO.getMensaje()));

        if (unidadRepo.existsByNumeroIgnoreCaseAndCondominio_Id(dto.getNumero(), c.getId()))
            throw new IllegalArgumentException(MensajeError.UNIDAD_DUPLICADA.getMensaje());

        Unidad u = Unidad.builder()
                .numero(dto.getNumero())
                .estado(dto.getEstado())
                .condominio(c)
                .build();
        u = unidadRepo.save(u);
        return toDTO(u);
    }

    public UnidadResumenDTO obtener(Integer id) {
        Unidad u = unidadRepo.findById(id)
                .orElseThrow(() -> new java.util.NoSuchElementException(MensajeError.UNIDAD_NO_ENCONTRADA.getMensaje()));
        return toDTO(u);
    }

    public List<UnidadResumenDTO> listarPorCondominio(Integer condominioId) {
        if (!condominioRepo.existsById(condominioId))
            throw new java.util.NoSuchElementException(MensajeError.CONDOMINIO_NO_ENCONTRADO.getMensaje());
        return unidadRepo.findByCondominio_Id(condominioId).stream().map(this::toDTO).toList();
    }

    @Transactional
    public UnidadResumenDTO actualizar(Integer id, UnidadUpdateDTO dto) {
        Unidad u = unidadRepo.findById(id)
                .orElseThrow(() -> new java.util.NoSuchElementException(MensajeError.UNIDAD_NO_ENCONTRADA.getMensaje()));

        if (dto.getNumero() != null) u.setNumero(dto.getNumero());
        if (dto.getEstado() != null) u.setEstado(dto.getEstado());
        return toDTO(u);
    }

    @Transactional
    public void eliminar(Integer id) {
        if (!unidadRepo.existsById(id))
            throw new java.util.NoSuchElementException(MensajeError.UNIDAD_NO_ENCONTRADA.getMensaje());
        unidadRepo.deleteById(id);
    }
}