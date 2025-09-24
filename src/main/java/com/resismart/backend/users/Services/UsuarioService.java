package com.resismart.backend.users.Services;

import com.resismart.backend.Common.MensajeError;
import com.resismart.backend.users.DTO.UsuarioClienteCredencialesDTO;
import com.resismart.backend.users.DTO.UsuarioCrearRequest;
import com.resismart.backend.users.DTO.UsuarioEditarRequest;
import com.resismart.backend.users.Entities.Usuario;
import com.resismart.backend.users.Repositories.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UsuarioService {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UsuarioRepository usuariosRepository;

    /*@Autowired
    private ClienteRepository clienteRepository;*/
    public List<Usuario> getUsuarios(){
        return usuariosRepository.findAll();
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

        if (nombreApellidoCambiado) {
            usuario.setNombres(request.getNombre());
            usuario.setApellidos(request.getApellido());
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

        if (request.getContraseña() != null && !request.getContraseña().trim().isEmpty()) {
            usuario.setPassword_hash(passwordEncoder.encode(request.getContraseña()));
        }

        usuario.setRol(request.getRol());
        usuario.setEstado(request.isEstado());

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


    public void deleteUsuario(int id){
        usuariosRepository.deleteById(id);
    }
}
