package com.resismart.backend.users.Services;

import com.resismart.backend.Auth.EmailService;
import com.resismart.backend.users.DTO.UsuarioCrearRequest;
import com.resismart.backend.users.Entities.Usuario;
import com.resismart.backend.users.Enums.Rol;
import com.resismart.backend.users.Repositories.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private UsuarioService usuarioService;

    @Test
    void getUsuariosVisiblesParaDevuelveTodosCuandoEsAdmin() {
        Usuario admin = Usuario.builder()
                .id_usuario(1)
                .rol(Rol.ADMIN)
                .build();
        List<Usuario> usuarios = List.of(
                Usuario.builder().id_usuario(2).rol(Rol.RESIDENTE).build()
        );

        when(usuarioRepository.findAll()).thenReturn(usuarios);

        List<Usuario> resultado = usuarioService.getUsuariosVisiblesPara(admin);

        assertThat(resultado).isEqualTo(usuarios);
        verify(usuarioRepository).findAll();
    }

    @Test
    void getUsuariosVisiblesParaDevuelveVacioCuandoEsDueno() {
        Usuario dueno = Usuario.builder()
                .id_usuario(7)
                .rol(Rol.DUEÑO)
                .build();

        List<Usuario> resultado = usuarioService.getUsuariosVisiblesPara(dueno);

        assertThat(resultado).isEmpty();
    }

    @Test
    void registerCodificaPasswordYGuardaUsuario() {
        UsuarioCrearRequest request = UsuarioCrearRequest.builder()
                .email("nuevo@ejemplo.com")
                .password("plain-pass")
                .nombre("Ada")
                .apellido("Lovelace")
                .telefono("555-0110")
                .rol(Rol.RESIDENTE)
                .build();

        when(passwordEncoder.encode("plain-pass")).thenReturn("hashed");
        when(usuarioRepository.save(any(Usuario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Usuario resultado = usuarioService.register(request);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        Usuario persistido = captor.getValue();

        assertThat(persistido.getCorreo()).isEqualTo("nuevo@ejemplo.com");
        assertThat(persistido.getPassword_hash()).isEqualTo("hashed");
        assertThat(persistido.isEstado()).isTrue();
        assertThat(persistido.getRol()).isEqualTo(Rol.RESIDENTE);

        assertThat(resultado).isSameAs(persistido);
        verify(passwordEncoder).encode("plain-pass");
        verify(emailService, never()).enviarCredencialesUsuario(any(Usuario.class), anyString());
    }

    @Test
    void registerGeneraPasswordYEnviaCorreoSiNoSeEnvio() {
        UsuarioCrearRequest request = UsuarioCrearRequest.builder()
                .email("sinpass@ejemplo.com")
                .nombre("Grace")
                .apellido("Hopper")
                .telefono("555-0111")
                .rol(Rol.DUEÑO)
                .build();

        when(passwordEncoder.encode(anyString())).thenAnswer(invocation -> "hashed-" + invocation.getArgument(0));
        when(usuarioRepository.save(any(Usuario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Usuario resultado = usuarioService.register(request);

        ArgumentCaptor<String> rawCaptor = ArgumentCaptor.forClass(String.class);
        verify(passwordEncoder).encode(rawCaptor.capture());
        String rawUsed = rawCaptor.getValue();
        assertThat(rawUsed).isNotBlank();
        assertThat(rawUsed.length()).isGreaterThanOrEqualTo(12);

        assertThat(resultado.getPassword_hash()).startsWith("hashed-");
        assertThat(resultado.getCorreo()).isEqualTo("sinpass@ejemplo.com");
        assertThat(resultado.isEstado()).isTrue();

        ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
        ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).enviarCredencialesUsuario(usuarioCaptor.capture(), passwordCaptor.capture());

        Usuario usuarioParaCorreo = usuarioCaptor.getValue();
        assertThat(usuarioParaCorreo.getCorreo()).isEqualTo("sinpass@ejemplo.com");
        assertThat(usuarioParaCorreo.getNombres()).isEqualTo("Grace");
        assertThat(usuarioParaCorreo.getApellidos()).isEqualTo("Hopper");
        assertThat(passwordCaptor.getValue()).isEqualTo(rawUsed);
    }
}
