package com.resismart.backend.users.Services;

import com.resismart.backend.Auth.EmailService;
import com.resismart.backend.Common.MensajeError;
import com.resismart.backend.users.DTO.UsuarioClienteCredencialesDTO;
import com.resismart.backend.users.DTO.UsuarioCrearRequest;
import com.resismart.backend.users.DTO.UsuarioEditarRequest;
import com.resismart.backend.users.DTO.UsuarioPerfilRequest;
import com.resismart.backend.users.Entities.Usuario;
import com.resismart.backend.users.Enums.Rol;
import com.resismart.backend.users.Repositories.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;

@Service
public class UsuarioService {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UsuarioRepository usuariosRepository;
    @Autowired private EmailService emailService;

    private static final String PASSWORD_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final String PASSWORD_SYMBOLS = "!@#$%&*?";
    private static final int PASSWORD_DEFAULT_LENGTH = 16;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String DEFAULT_AVATAR = "defaults/default-avatar.png";

    public List<Usuario> getUsuarios(){
        return usuariosRepository.findAll();
    }

    public List<Usuario> getUsuariosVisiblesPara(Usuario solicitante) {
        if (solicitante == null) return List.of();
        return switch (solicitante.getRol()) {
            case ADMIN -> usuariosRepository.findAll();
            case DUEÑO -> List.of(); // Relación por contratos; sin consulta directa
            default -> List.of();
        };
    }

    public Usuario getUsuarioByEmail(String email){
        return usuariosRepository.findByCorreo(email).orElse(null);
    }

    public Usuario getUsuarioById(int id){
        return usuariosRepository.findById(id).orElse(null);
    }

    /**
     * Registra un usuario sin asignarlo a unidad/condominio.
     * Nota: El conteo de licencia (maxUsuarios por condominio) solo aplica
     * cuando se crea el Residente asociado mediante {@code ResidenteService.saveCliente}.
     * Si no se envia password se genera una temporal segura, se envia por correo y el usuario
     * puede cambiarla luego con /auth/forgot-password.
     */
    @Transactional
    public Usuario register(UsuarioCrearRequest request) {
        String rawPassword = request.getPassword();
        boolean generated = false;
        // Si no se especifica password, generamos una temporal segura y la enviamos por correo.
        if (rawPassword == null || rawPassword.isBlank()) {
            rawPassword = generarPasswordTemporal(PASSWORD_DEFAULT_LENGTH);
            generated = true;
        }

        Usuario u= Usuario.builder()
                .rol(request.getRol())
                .nombres(request.getNombre())
                .apellidos(request.getApellido())
                .estado(true)
                .telefono(request.getTelefono())
                .correo(request.getEmail())
                .avatarUrl(DEFAULT_AVATAR)
                .password_hash(passwordEncoder.encode(rawPassword))
                .build();
        Usuario saved = usuariosRepository.save(u);

        if (generated) {
            enviarCredenciales(saved, rawPassword);
        }

        return saved;
    }

    public Usuario putUsuario(UsuarioEditarRequest request) {
        Usuario usuario = usuariosRepository.findById(request.getID_Usuario())
                .orElseThrow(() -> new RuntimeException(MensajeError.USUARIO_NO_ENCONTRADO.getMensaje()));

        // Validar duplicidad de email si cambio
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
        boolean desactivando = usuario.isEstado() && !request.isEstado() && usuario.getRol() == Rol.RESIDENTE;

        if (nombreApellidoCambiado) {
            usuario.setNombres(request.getNombre());
            usuario.setApellidos(request.getApellido());
        }

        // Actualizar correo si cambio
        if (emailCambiado) {
            usuario.setCorreo(request.getEmail());
        }

        // Actualizar telefono si viene en la solicitud
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

                // Enviar notificacion por correo
                String destinatario = request.getEmail();
                String asunto = "Actualizacion de correo en Walk Seguros";
                String cuerpo = "Estimado " + request.getNombre() + " " + request.getApellido() + ",\n\n" +
                        "Su correo ha sido actualizado en Walk Seguros. A continuacion, tus credenciales:\n\n" +
                        "Usuario: " + request.getEmail() + "\n" +
                        "Contrasena: Su contrasena es la misma con la que ingresa normalmente al sistema\n\n" +
                        "Por favor, inicia sesion en nuestra aplicacion en el siguiente enlace:\n" +
                        "http://localhost:5173\n\n";

                emailService.enviarCorreo(destinatario, asunto, cuerpo);
            }
            clienteRepository.save(cliente);
        }*/

        usuario.setRol(request.getRol());
        usuario.setEstado(request.isEstado());

        // Si se desactiva un residente, ya no liberamos unidad aquí; el vínculo se maneja por Contrato.

        return usuariosRepository.save(usuario);
    }

    private String generarPasswordTemporal(int length) {
        String pool = PASSWORD_ALPHABET + PASSWORD_SYMBOLS;
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int idx = SECURE_RANDOM.nextInt(pool.length());
            sb.append(pool.charAt(idx));
        }
        return sb.toString();
    }

    private void enviarCredenciales(Usuario usuario, String passwordPlano) {
        try {
            emailService.enviarCredencialesUsuario(usuario, passwordPlano);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(UsuarioService.class)
                    .warn("No se pudo enviar credenciales al usuario {}: {}", usuario.getCorreo(), e.getMessage());
        }
    }

    @Transactional
    public Usuario actualizarPassword(int idUsuario, String nuevaPassword) {
        if (nuevaPassword == null || nuevaPassword.isBlank()) {
            throw new RuntimeException("La contrasena no puede estar vacia");
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

        // Verificar si el nuevo email ya esta en uso por otro usuario
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

        // Actualizar contrasena si se envia
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
        // ADMIN no puede eliminar usuarios (refuerzo en capa de servicio)
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            Object principal = auth.getPrincipal();
            if (principal instanceof org.springframework.security.core.userdetails.UserDetails userDetails) {
                var current = usuariosRepository.findByCorreo(userDetails.getUsername()).orElse(null);
                if (current != null && current.getRol() == Rol.ADMIN) {
                    throw new org.springframework.security.access.AccessDeniedException("ADMIN no puede eliminar usuarios");
                }
            }
        }
        Usuario objetivo = usuariosRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(MensajeError.USUARIO_NO_ENCONTRADO.getMensaje()));

        

        // Desactivar lógicamente (soft delete vía @SQLDelete + flags)
        objetivo.setEstado(false);
        objetivo.setActivo(false);
        usuariosRepository.save(objetivo);
        usuariosRepository.deleteById(id);
    }
}
