package com.resismart.backend.condominios.Services;


import com.resismart.backend.Common.MensajeError;
import com.resismart.backend.condominios.DTO.*;
import com.resismart.backend.condominios.Entities.*;
import com.resismart.backend.condominios.Enums.UnidadEstado;
import com.resismart.backend.condominios.Repositories.*;
import com.resismart.backend.users.Entities.Usuario;
import com.resismart.backend.users.Enums.Rol;
import com.resismart.backend.users.Repositories.UsuarioRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CondominioService {

    private final CondominioRepository condominioRepo;
    private final UnidadRepository unidadRepo;
    private final UsuarioRepository usuarioRepo;

    /* ==== Mappers ==== */
    private CondominioResumenDTO toResumen(Condominio c) {
        return new CondominioResumenDTO(
                c.getId(), c.getNombre(), c.getDireccion(),
                c.getTelefono(), c.getCorreo(), c.getCreadoEn(),
                c.getDueno().getId_usuario()
        );
    }
    private UnidadResumenDTO toUnidadDTO(Unidad u) {
        return new UnidadResumenDTO(u.getId(), u.getNumero(), u.getEstado());
    }
    private CondominioDetalleDTO toDetalle(Condominio c) {
        List<UnidadResumenDTO> uds = c.getUnidades().stream().map(this::toUnidadDTO).toList();
        return new CondominioDetalleDTO(
                c.getId(), c.getNombre(), c.getDireccion(),
                c.getTelefono(), c.getCorreo(), c.getCreadoEn(),
                c.getDueno().getId_usuario(), uds
        );
    }

    /* ==== Helpers ==== */
    private Usuario ensureDueno(Integer idDueno) {
        Usuario u = usuarioRepo.findById(idDueno)
                .orElseThrow(() -> new java.util.NoSuchElementException(MensajeError.USUARIO_NO_ENCONTRADO.getMensaje()));
        if (u.getRol() != Rol.DUEÑO) {
            throw new IllegalArgumentException(MensajeError.USUARIO_SIN_ROL_DUENO.getMensaje());
        }
        return u;
    }

    /* ==== CRUD Condominio ==== */

    @Transactional
    public CondominioResumenDTO crear(CondominioCreateDTO dto) {
        if (dto.getCorreo() != null && condominioRepo.existsByCorreoIgnoreCase(dto.getCorreo())) {
            throw new IllegalArgumentException(MensajeError.EMAIL_REGISTRADO.getMensaje());
        }
        Usuario dueno = ensureDueno(dto.getIdDueno());

        Condominio c = Condominio.builder()
                .nombre(dto.getNombre())
                .direccion(dto.getDireccion())
                .telefono(dto.getTelefono())
                .correo(dto.getCorreo())
                .dueno(dueno)
                .build();
        condominioRepo.save(c);
        return toResumen(c);
    }

    @Transactional
    public CondominioResumenDTO actualizar(Integer id, CondominioUpdateDTO dto) {
        Condominio c = condominioRepo.findById(id)
                .orElseThrow(() -> new java.util.NoSuchElementException(MensajeError.CONDOMINIO_NO_ENCONTRADO.getMensaje()));

        if (dto.getNombre() != null) c.setNombre(dto.getNombre());
        if (dto.getDireccion() != null) c.setDireccion(dto.getDireccion());
        if (dto.getTelefono() != null) c.setTelefono(dto.getTelefono());
        if (dto.getCorreo() != null && !dto.getCorreo().equalsIgnoreCase(c.getCorreo())) {
            if (condominioRepo.existsByCorreoIgnoreCase(dto.getCorreo())) {
                throw new IllegalArgumentException(MensajeError.EMAIL_REGISTRADO.getMensaje());
            }
            c.setCorreo(dto.getCorreo());
        }
        if (dto.getIdDueno() != null) c.setDueno(ensureDueno(dto.getIdDueno()));

        return toResumen(c);
    }

    public CondominioResumenDTO obtener(Integer id) {
        Condominio c = condominioRepo.findById(id)
                .orElseThrow(() -> new java.util.NoSuchElementException(MensajeError.CONDOMINIO_NO_ENCONTRADO.getMensaje()));
        return toResumen(c);
    }

    public CondominioDetalleDTO obtenerConUnidades(Integer id) {
        Condominio c = condominioRepo.findWithUnidadesById(id)
                .orElseThrow(() -> new java.util.NoSuchElementException(MensajeError.CONDOMINIO_NO_ENCONTRADO.getMensaje()));
        return toDetalle(c);
    }

    public Page<CondominioResumenDTO> listar(Pageable pageable) {
        return condominioRepo.findAll(pageable).map(this::toResumen);
    }

    public Page<CondominioResumenDTO> listarPorDueno(Integer idDueno, Pageable pageable) {
        return condominioRepo.findAllByDueno(idDueno, pageable).map(this::toResumen);
    }

    @Transactional
    public void eliminar(Integer id) {
        if (!condominioRepo.existsById(id))
            throw new java.util.NoSuchElementException(MensajeError.CONDOMINIO_NO_ENCONTRADO.getMensaje());
        condominioRepo.deleteById(id);
    }

    /* ==== Gestión de Unidades dentro del Condominio ==== */

    @Transactional
    public UnidadResumenDTO agregarUnidad(Integer condominioId, UnidadCreateDTO dto) {
        Condominio c = condominioRepo.findById(condominioId)
                .orElseThrow(() -> new java.util.NoSuchElementException(MensajeError.CONDOMINIO_NO_ENCONTRADO.getMensaje()));

        if (unidadRepo.existsByNumeroIgnoreCaseAndCondominio_Id(dto.getNumero(), condominioId)) {
            throw new IllegalArgumentException(MensajeError.UNIDAD_DUPLICADA.getMensaje());
        }

        Unidad u = Unidad.builder()
                .numero(dto.getNumero())
                .estado(dto.getEstado()) // default en @PrePersist si null
                .condominio(c)
                .build();

        c.addUnidad(u);
        return toUnidadDTO(u);
    }

    public List<UnidadResumenDTO> listarUnidadesDeCondominio(Integer condominioId) {
        if (!condominioRepo.existsById(condominioId))
            throw new java.util.NoSuchElementException(MensajeError.CONDOMINIO_NO_ENCONTRADO.getMensaje());
        return unidadRepo.findByCondominio_Id(condominioId).stream().map(this::toUnidadDTO).toList();
    }

    @Transactional
    public void eliminarUnidad(Integer condominioId, Integer unidadId) {
        Condominio c = condominioRepo.findWithUnidadesById(condominioId)
                .orElseThrow(() -> new java.util.NoSuchElementException(MensajeError.CONDOMINIO_NO_ENCONTRADO.getMensaje()));
        Unidad u = c.getUnidades().stream()
                .filter(x -> x.getId().equals(unidadId))
                .findFirst()
                .orElseThrow(() -> new java.util.NoSuchElementException(MensajeError.UNIDAD_NO_ENCONTRADA.getMensaje()));
        c.removeUnidad(u); // orphanRemoval => borra
    }

    public static class ResumenOcupacion {
        public final long total, libres, ocupadas, mantenimiento;
        public ResumenOcupacion(long total, long libres, long ocupadas, long mantenimiento) {
            this.total = total; this.libres = libres; this.ocupadas = ocupadas; this.mantenimiento = mantenimiento;
        }
    }

    public ResumenOcupacion getResumenOcupacion(Integer condominioId) {
        if (!condominioRepo.existsById(condominioId))
            throw new java.util.NoSuchElementException(MensajeError.CONDOMINIO_NO_ENCONTRADO.getMensaje());
        long total = unidadRepo.countByCondominio_Id(condominioId);
        long libres = unidadRepo.countByCondominio_IdAndEstado(condominioId, UnidadEstado.LIBRE);
        long ocupadas = unidadRepo.countByCondominio_IdAndEstado(condominioId, UnidadEstado.OCUPADA);
        long mantenimiento = unidadRepo.countByCondominio_IdAndEstado(condominioId, UnidadEstado.MANTENIMIENTO);
        return new ResumenOcupacion(total, libres, ocupadas, mantenimiento);
    }

    public boolean esDuenoDeCondominio(Integer condominioId, Integer duenoId) {
        return condominioRepo.existsByIdAndDueno(condominioId, duenoId);
    }
}
