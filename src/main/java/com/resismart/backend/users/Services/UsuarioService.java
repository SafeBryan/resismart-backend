package com.resismart.backend.users.Services;

import com.resismart.backend.Common.MensajeError;
import com.resismart.backend.users.DTO.UsuarioClienteCredencialesDTO;
import com.resismart.backend.users.DTO.UsuarioCrearRequest;
import com.resismart.backend.users.DTO.UsuarioEditarRequest;
import com.resismart.backend.users.DTO.UsuarioPerfilRequest;
import com.resismart.backend.users.Entities.Usuario;
import com.resismart.backend.users.Repositories.UsuarioRepository;
import com.resismart.backend.residentes.Repositories.ResidenteRepository;
import com.resismart.backend.condominios.Repositories.UnidadRepository;
import com.resismart.backend.condominios.Enums.UnidadEstado;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UsuarioService {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UsuarioRepository usuariosRepository;
    @Autowired private ResidenteRepository residenteRepository;
    @Autowired private UnidadRepository unidadRepository;

    /*@Autowired
    private ClienteRepository clienteRepository;*/
    public List<Usuario> getUsuarios(){
        return usuariosRepository.findAll();
    }
    public List<Usuario> getUsuariosVisiblesPara(Usuario solicitante) {
        if (solicitante == null) return List.of();
        return switch (solicitante.getRol()) {
            case ADMIN -> usuariosRepository.findAll();
            case DUEÑO -> usuariosRepository.findUsuariosResidentesPorDueno(solicitante.getId_usuario());
            default -> List.of();
        };
    }
    public Usuario getUsuarioByEmail(String email){
        return usuariosRepository.findByCorreo(email).orElse(null);
    }
    public Usuario getUsuarioById(int id){
        return usuariosRepository.findById(id).orElse(null);
    }
    @Transactional
    public Usuario register(UsuarioCrearRequest request) {
        Usuario u= Usuario.builder()
                .rol(request.getRol())
                .nombres(request.getNombre())
                .apellidos(request.getApellido())
                .estado(true)
                .telefono(request.getTelefono())
                .correo(request.getEmail())
                .password_hash(passwordEncoder.encode(request.getPassword()))
                .build();
        return usuariosRepository.save(u);
    }
    public Usuario putUsuario(UsuarioEditarRequest request) {
        Usuario usuario = usuariosRepository.findById(request.getID_Usuario())
                .orElseThrow(() -> new RuntimeException(MensajeError.USUARIO_NO_ENCONTRADO.getMensaje()));

        // Validar duplicidad de email si cambió
        if (!usuario.getCorreo().equals(request.getEmail())) {
            usuariosRepository.findByCorreo(request.getEmail())
                    .filter(u -> !(u.getId_usuario()==request.getID_Usuario()))
                    .ifPresent(u -> {
                        throw new RuntimeException(MensajeError.EMAIL_REGISTRADO.getMensaje());
                    });
        }

        boolean nombreApellidoCambiado = !usuario.getNombres().equals(request.getNombre()) ||
                !usuario.getApellidos().equals(request.getApellido());

        boolean emailCambiado = !usuario.getCorreo().equals(request.getEmail());
        boolean telefonoCambiado = request.getTelefono() != null && (usuario.getTelefono() == null || !usuario.getTelefono().equals(request.getTelefono()));
        boolean desactivando = usuario.isEstado() && !request.isEstado() && usuario.getRol() == com.resismart.backend.users.Enums.Rol.RESIDENTE;

        if (nombreApellidoCambiado) {
            usuario.setNombres(request.getNombre());
            usuario.setApellidos(request.getApellido());
        }

        // Actualizar correo si cambió
        if (emailCambiado) {
            usuario.setCorreo(request.getEmail());
        }

        // Actualizar teléfono si viene en la solicitud
        if (telefonoCambiado) {
            usuario.setTelefono(request.getTelefono());
        }
/*
        // Solo actualizar el cliente si el rol es CLIENTE
        if (usuario.getRol() == Rol.Cliente) {
            Cliente cliente = clienteRepository.findByEmail(usuario.getEmail())
                    .orElseThrow(() -> new RuntimeException(MensajeError.CLIENTE_NO_ENCONTRADO.getMensaje()));

            if (nombreApellidoCambiado) {
                cliente.setNombre(request.getNombre());
                cliente.setApellido(request.getApellido());
            }
            if (emailCambiado) {
                usuario.setEmail(request.getEmail());

                // Enviar notificación por correo
                String destinatario = request.getEmail();
                String asunto = "Actualización de correo en Walk Seguros";
                String cuerpo = "Estimado " + request.getNombre() + " " + request.getApellido() + ",\n\n" +
                        "Su correo ha sido actualizado en Walk Seguros. A continuación, tus credenciales:\n\n" +
                        "Usuario: " + request.getEmail() + "\n" +
                        "Contraseña: Su contraseña es la misma con la que ingresa normalmente al sistema\n\n" +
                        "Por favor, inicia sesión en nuestra aplicación en el siguiente enlace:\n" +
                        "http://localhost:5173\n\n";

                emailService.enviarCorreo(destinatario, asunto, cuerpo);
            }
            clienteRepository.save(cliente);
        }*/

        usuario.setRol(request.getRol());
        usuario.setEstado(request.isEstado());

        if (desactivando) {
            residenteRepository.findByUsuarioId(usuario.getId_usuario()).ifPresent(res -> {
                var unidad = res.getUnidad();
                unidad.setEstado(UnidadEstado.LIBRE);
                unidadRepository.save(unidad);
            });
        }

        return usuariosRepository.save(usuario);
    }

    @Transactional
    public Usuario actualizarPassword(int idUsuario, String nuevaPassword) {
        if (nuevaPassword == null || nuevaPassword.isBlank()) {
            throw new RuntimeException("La contraseña no puede estar vacía");
        }
        Usuario usuario = usuariosRepository.findById(idUsuario)
                .orElseThrow(() -> new RuntimeException(MensajeError.USUARIO_NO_ENCONTRADO.getMensaje()));
        usuario.setPassword_hash(passwordEncoder.encode(nuevaPassword));
        return usuariosRepository.save(usuario);
    }



    public Usuario actualizarCredencialesUsuarioCliente(UsuarioClienteCredencialesDTO request) {
        // Buscar usuario por ID
        Usuario usuario = usuariosRepository.findById(request.getIdUsuario())
                .orElseThrow(() -> new RuntimeException(MensajeError.USUARIO_NO_ENCONTRADO.getMensaje()));

        // Verificar si el nuevo email ya está en uso por otro usuario
        usuariosRepository.findByCorreo(request.getEmail())
                .filter(u -> !(u.getId_usuario()==request.getIdUsuario()))
                .ifPresent(u -> {
                    throw new RuntimeException(MensajeError.EMAIL_REGISTRADO.getMensaje());
                });
/*
        // Buscar cliente por ID
        Cliente cliente = clienteRepository.findById(request.getIdCliente())
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));*/

        // Actualizar email si es distinto
        if (!usuario.getCorreo().equals(request.getEmail())) {
            usuario.setCorreo(request.getEmail());
            //cliente.setEmail(request.getEmail());
        }

        // Actualizar contraseña si se envía
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            usuario.setPassword_hash(passwordEncoder.encode(request.getPassword()));
        }

        usuariosRepository.save(usuario);
        //clienteRepository.save(cliente);

        return usuario;
    }

    @Transactional
    public Usuario actualizarPerfilPropio(int idUsuario, UsuarioPerfilRequest request) {
        Usuario usuario = usuariosRepository.findById(idUsuario)
                .orElseThrow(() -> new RuntimeException(MensajeError.USUARIO_NO_ENCONTRADO.getMensaje()));

        if (request == null) {
            return usuario;
        }

        if (request.getNombre() != null && !request.getNombre().isBlank()) {
            usuario.setNombres(request.getNombre());
        }
        if (request.getApellido() != null && !request.getApellido().isBlank()) {
            usuario.setApellidos(request.getApellido());
        }

        if (request.getEmail() != null && !request.getEmail().isBlank()
                && !request.getEmail().equals(usuario.getCorreo())) {
            usuariosRepository.findByCorreo(request.getEmail())
                    .filter(u -> u.getId_usuario() != idUsuario)
                    .ifPresent(u -> {
                        throw new RuntimeException(MensajeError.EMAIL_REGISTRADO.getMensaje());
                    });
            usuario.setCorreo(request.getEmail());
        }

        if (request.getTelefono() != null) {
            usuario.setTelefono(request.getTelefono().isBlank() ? null : request.getTelefono());
        }

        return usuariosRepository.save(usuario);
    }


    public void deleteUsuario(int id){
        usuariosRepository.deleteById(id);
    }
}
