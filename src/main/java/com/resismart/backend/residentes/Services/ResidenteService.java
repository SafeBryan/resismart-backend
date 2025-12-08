package com.resismart.backend.residentes.Services;

import com.resismart.backend.Common.MensajeError;
import com.resismart.backend.contratos.Enums.EstadoContrato;
import com.resismart.backend.contratos.Repositories.ContratoRepository;
import com.resismart.backend.condominios.Entities.Condominio;
import com.resismart.backend.condominios.Repositories.CondominioRepository;
import com.resismart.backend.residentes.DTO.ResidenteDTO;
import com.resismart.backend.residentes.DTO.ResidentePerfilRequest;
import com.resismart.backend.residentes.DTO.ResidenteRespuestaDTO;
import com.resismart.backend.residentes.Entities.Residente;
import com.resismart.backend.residentes.Repositories.ResidenteRepository;
import com.resismart.backend.users.DTO.UsuarioCrearRequest;
import com.resismart.backend.users.Entities.Usuario;
import com.resismart.backend.users.Enums.Rol;
import com.resismart.backend.users.Repositories.UsuarioRepository;
import com.resismart.backend.users.Services.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ResidenteService {

    @Autowired
    private ResidenteRepository residenteRepository;
    @Autowired
    private UsuarioRepository usuariosRepository;
    @Autowired
    private UsuarioService usuarioService;
    @Autowired
    private ContratoRepository contratoRepository;
    @Autowired
    private CondominioRepository condominioRepository;

    @Transactional
    public ResidenteRespuestaDTO saveCliente(ResidenteDTO residenteDTO) {
        // Validar unicidad de cédula
        residenteRepository.findByCedula(residenteDTO.getCedula())
                .ifPresent(residente -> {
                    throw new RuntimeException(MensajeError.CEDULA_REGISTRADA.getMensaje());
                });

        Usuario usuario;
        if (residenteDTO.getUsuarioId() != null) {
            usuario = usuariosRepository.findById(residenteDTO.getUsuarioId().intValue())
                    .orElseThrow(() -> new RuntimeException(MensajeError.USUARIO_NO_ENCONTRADO.getMensaje()));
        } else {
            // Crear usuario RESIDENTE a partir de los datos recibidos
            UsuarioCrearRequest nuevo = UsuarioCrearRequest.builder()
                    .email(residenteDTO.getEmail())
                    .nombre(residenteDTO.getNombre())
                    .apellido(residenteDTO.getApellido())
                    .telefono(residenteDTO.getTelefono())
                    .rol(Rol.RESIDENTE)
                    .build();
            usuario = usuarioService.register(nuevo);
        }

        Condominio condominio = condominioRepository.findById(residenteDTO.getCondominioId().intValue())
                .orElseThrow(() -> new RuntimeException(MensajeError.CONDOMINIO_NO_ENCONTRADO.getMensaje()));

        Residente residente = Residente.builder()
                .cedula(residenteDTO.getCedula())
                .telefono(residenteDTO.getTelefono())
                .usuario(usuario)
                .condominio(condominio)
                .build();

        residente = propietarioSave(residente);

        // Devolver DTO usando el mapper centralizado
        return toRespuestaDTO(residente);
    }

    @Transactional
    public void deleteCliente(long id) {
        // ADMIN no puede eliminar inquilinos (refuerzo en capa de servicio)
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            Object principal = auth.getPrincipal();
            if (principal instanceof org.springframework.security.core.userdetails.UserDetails userDetails) {
                var usuario = usuariosRepository.findByCorreo(userDetails.getUsername()).orElse(null);
                if (usuario != null && usuario.getRol() == com.resismart.backend.users.Enums.Rol.ADMIN) {
                    throw new org.springframework.security.access.AccessDeniedException("ADMIN no puede eliminar inquilinos");
                }
            }
        }

        Residente residente = residenteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(MensajeError.RESIDENTE_NO_ENCONTRADO.getMensaje()));
        Usuario usuario = residente.getUsuario();

        // Validar contratos activos
        boolean tieneContratosActivos = contratoRepository.existsByResidente_IdAndEstado(id, EstadoContrato.ACTIVO);
        if (tieneContratosActivos) {
            throw new RuntimeException("No se puede eliminar el inquilino porque tiene contratos activos.");
        }

        // Eliminar residente (soft delete por @SQLDelete)
        residenteRepository.deleteById(id);

        // Desactivar lógicamente al usuario asociado si es residente
        if (usuario != null && usuario.getRol() == Rol.RESIDENTE) {
            usuario.setActivo(false);
            usuario.setEstado(false);
            usuariosRepository.save(usuario);
        }
    }

    // Método separado para facilitar pruebas / extensión si se requiere lógica adicional
    private Residente propietarioSave(Residente r) {
        return residenteRepository.save(r);
    }

    @Transactional(readOnly = true)
    public Optional<ResidenteRespuestaDTO> getCliente(Long id) {
        return residenteRepository.findById(id)
                .map(this::toRespuestaDTO);
    }

    @Transactional(readOnly = true)
    public List<ResidenteRespuestaDTO> getAllClientes() {
        return residenteRepository.findAll()
                .stream()
                .map(this::toRespuestaDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ResidenteRespuestaDTO> getAllClientesPorDueno(Integer idDueno) {
        return List.of();
    }

    @Transactional
    public ResidenteRespuestaDTO updateCliente(Long id, ResidenteDTO residenteDTO) {
        Residente residente = residenteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(MensajeError.CLIENTE_NO_ENCONTRADO.getMensaje()));

        // Validar unicidad de cédula si cambió
        if (!residente.getCedula().equals(residenteDTO.getCedula())) {
            residenteRepository.findByCedula(residenteDTO.getCedula())
                    .ifPresent(c -> {
                        throw new RuntimeException(MensajeError.CEDULA_REGISTRADA.getMensaje());
                    });
        }

        // Actualizar datos
        residente.setCedula(residenteDTO.getCedula());
        residente.setTelefono(residenteDTO.getTelefono());
        if (residenteDTO.getCondominioId() != null) {
            Condominio condominio = condominioRepository.findById(residenteDTO.getCondominioId().intValue())
                    .orElseThrow(() -> new RuntimeException(MensajeError.CONDOMINIO_NO_ENCONTRADO.getMensaje()));
            residente.setCondominio(condominio);
        }
        residente = residenteRepository.save(residente);

        return toRespuestaDTO(residente);
    }

    @Transactional(readOnly = true)
    public List<ResidenteRespuestaDTO> getAllClientesPorCondominio(Integer idCondominio) {
        if (idCondominio == null) {
            return List.of();
        }
        return residenteRepository.findResidentesPorCondominio(idCondominio.longValue())
                .stream()
                .map(this::toRespuestaDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean esResidenteDeDueno(long idResidente, int idDueno) {
        return residenteRepository.findResidentesPorDueno(idDueno).stream()
                .anyMatch(r -> r.getId() != null && r.getId() == idResidente);
    }

    @Transactional(readOnly = true)
    public Optional<ResidenteRespuestaDTO> findByUsuarioId(Long idUsuario) {
        return residenteRepository.findByUsuarioIdLong(idUsuario)
                .map(this::toRespuestaDTO);
    }

    @Transactional
    public ResidenteRespuestaDTO actualizarPerfilPropio(int idUsuario, ResidentePerfilRequest request) {
        Residente residente = residenteRepository.findByUsuarioId(idUsuario)
                .orElseThrow(() -> new RuntimeException(MensajeError.RESIDENTE_NO_ENCONTRADO.getMensaje()));

        if (request == null) {
            return toRespuestaDTO(residente);
        }

        Usuario usuario = residente.getUsuario();
        if (request.getTelefono() != null) {
            String telefono = request.getTelefono().trim();
            if (telefono.isEmpty()) {
                throw new IllegalArgumentException("El teléfono no puede estar vacío");
            }
            residente.setTelefono(telefono);
            usuario.setTelefono(telefono);
        }

        if (request.getCedula() != null) {
            String cedula = request.getCedula().trim();
            if (cedula.isEmpty()) {
                throw new IllegalArgumentException("La cédula no puede estar vacía");
            }
            if (!cedula.equals(residente.getCedula())) {
                residenteRepository.findByCedula(cedula)
                        .filter(r -> r.getId() != residente.getId())
                        .ifPresent(r -> {
                            throw new RuntimeException(MensajeError.CEDULA_REGISTRADA.getMensaje());
                        });
                residente.setCedula(cedula);
            }
        }

        usuariosRepository.save(usuario);
        Residente actualizado = residenteRepository.save(residente);

        return toRespuestaDTO(actualizado);
    }

    // =======================
    // Mapper centralizado
    // =======================
    private ResidenteRespuestaDTO toRespuestaDTO(Residente r) {
        var usuario = r.getUsuario();
        return ResidenteRespuestaDTO.builder()
                .id(r.getId())
                .cedula(r.getCedula())
                .telefono(r.getTelefono())
                .usuarioId(usuario != null ? usuario.getId_usuario() : null)
                .usuarioNombre(usuario != null ? usuario.getNombres() : null)
                .usuarioApellido(usuario != null ? usuario.getApellidos() : null)
                .usuarioEmail(usuario != null ? usuario.getCorreo() : null)
                .usuarioRol(usuario != null && usuario.getRol() != null ? usuario.getRol().name() : null)
                .usuarioEstado(usuario != null ? usuario.isEstado() : null)
                .usuarioActivo(usuario != null ? usuario.isActivo() : null)
                .condominioId(r.getCondominio() != null ? r.getCondominio().getId().longValue() : null)
                .condominioNombre(r.getCondominio() != null ? r.getCondominio().getNombre() : null)
                .build();
    }
}
