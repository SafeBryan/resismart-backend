package com.resismart.backend.residentes.Services;

import com.resismart.backend.condominios.Enums.UnidadEstado;
import com.resismart.backend.condominios.Repositories.UnidadRepository;
import com.resismart.backend.condominios.Services.UnidadService;
import com.resismart.backend.residentes.DTO.ResidenteDTO;
import com.resismart.backend.residentes.DTO.ResidentePerfilRequest;
import com.resismart.backend.residentes.DTO.ResidenteRespuestaDTO;
import com.resismart.backend.Common.MensajeError;
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
    private UsuarioService usuarioService;
    @Autowired
    private UnidadService unidadService;
    @Autowired
    private UsuarioRepository usuariosRepository; // (No usado aquí, pero lo dejo si lo ocupas en otro lado)
    @Autowired
    private UnidadRepository unidadRepository;

    @Transactional
    public ResidenteRespuestaDTO saveCliente(ResidenteDTO residenteDTO) {
        // Validar unicidad de cédula (y podrías validar email si aplica)
        residenteRepository.findByCedula(residenteDTO.getCedula())
                .ifPresent(residente -> {
                    throw new RuntimeException(MensajeError.CEDULA_REGISTRADA.getMensaje());
                });

        // Construcción básica del Residente
        Residente residente = Residente.builder()
                .cedula(residenteDTO.getCedula())
                .telefono(residenteDTO.getTelefono())
                .unidad(unidadService.obtenerId(residenteDTO.getIdUnidad()))
                .build();

        // Crear usuario asociado
        Usuario u = usuarioService.register(
                UsuarioCrearRequest.builder()
                        .apellido(residenteDTO.getApellido())
                        .email(residenteDTO.getEmail())
                        .nombre(residenteDTO.getNombre())
                        .rol(Rol.RESIDENTE)
                        .password(residente.getCedula())
                        .build()
        );

        // Asociar usuario al residente y guardar
        residente.setUsuario(u);
        residente = propietarioSave(residente);

        // Devolver DTO usando el mapper centralizado
        return toRespuestaDTO(residente);
    }

    @Transactional
    public void deleteCliente(long id) {
        Residente residente = residenteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(MensajeError.CLIENTE_NO_ENCONTRADO.getMensaje()));

        // Liberar unidad
        var unidad = residente.getUnidad();
        unidad.setEstado(UnidadEstado.LIBRE);
        unidadRepository.save(unidad);

        // Eliminar residente
        residenteRepository.deleteById(id);
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
        return residenteRepository.findResidentesPorDueno(idDueno)
                .stream()
                .map(this::toRespuestaDTO)
                .toList();
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
        residente = residenteRepository.save(residente);

        return toRespuestaDTO(residente);
    }

    @Transactional(readOnly = true)
    public List<ResidenteRespuestaDTO> getAllClientesPorCondominio(Integer idCondominio) {
        return residenteRepository.findResidentesPorCondominio(idCondominio)
                .stream()
                .map(this::toRespuestaDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean esResidenteDeDueno(long idResidente, int idDueno) {
        return residenteRepository.existsByIdAndDueno(idResidente, idDueno);
    }

    @Transactional(readOnly = true)
    public Optional<ResidenteRespuestaDTO> findByUsuarioId(Long idUsuario) {
        return residenteRepository.findByUsuarioId(idUsuario)
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

        if (request.getNombre() != null) {
            String nombre = request.getNombre().trim();
            if (!nombre.isEmpty()) {
                usuario.setNombres(nombre);
            }
        }

        if (request.getApellido() != null) {
            String apellido = request.getApellido().trim();
            if (!apellido.isEmpty()) {
                usuario.setApellidos(apellido);
            }
        }

        if (request.getEmail() != null) {
            String email = request.getEmail().trim();
            if (!email.isEmpty() && !email.equals(usuario.getCorreo())) {
                usuariosRepository.findByCorreo(email)
                        .filter(u -> u.getId_usuario() != usuario.getId_usuario())
                        .ifPresent(u -> {
                            throw new RuntimeException(MensajeError.EMAIL_REGISTRADO.getMensaje());
                        });
                usuario.setCorreo(email);
            }
        }

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
        return ResidenteRespuestaDTO.builder()
                .id_Cliente(r.getId())
                .cedula(r.getCedula())
                .telefono(r.getTelefono())
                .usuario(r.getUsuario())
                .unidad(r.getUnidad())
                .build();
    }
}
