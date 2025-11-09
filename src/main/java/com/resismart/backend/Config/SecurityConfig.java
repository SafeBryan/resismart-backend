package com.resismart.backend.Config;


import com.resismart.backend.Auth.Jwt.JwtAuthenticationFilter;
import com.resismart.backend.users.Enums.Rol;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AuthenticationProvider authProvider;
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
               .csrf(csrf->
                        csrf.disable()
                        )
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(authRequest->
                        authRequest
                                .requestMatchers("/login").permitAll()
                                // WebSockets de avisos
                                .requestMatchers("/ws/avisos/**").permitAll()
                                // Perfil actual
                                .requestMatchers("/Usuarios/whoami").authenticated()
                                .requestMatchers("/Usuarios/me").authenticated()
                                .requestMatchers("/Usuarios/credencialesCliente").authenticated()
                                // Usuarios
                                .requestMatchers("/Usuarios").hasAnyAuthority(Rol.ADMIN.name(), Rol.DUEÑO.name())
                                .requestMatchers("/Usuarios/**").hasAnyAuthority(Rol.ADMIN.name(), Rol.DUEÑO.name())
                                // Condominios
                                .requestMatchers("/Condominios").hasAnyAuthority(Rol.ADMIN.name(), Rol.DUEÑO.name())
                                .requestMatchers("/Condominios/**").hasAnyAuthority(Rol.ADMIN.name(), Rol.DUEÑO.name())
                                // Residentes
                                .requestMatchers("/Residentes/me").hasAuthority(Rol.RESIDENTE.name())
                                // Eventos: crear solo ADMIN/DUEÑO
                                .requestMatchers(HttpMethod.POST, "/Eventos").hasAnyAuthority(Rol.ADMIN.name(), Rol.DUEÑO.name())
                                // Eventos participantes: alta/baja solo ADMIN/DUEÑO
                                .requestMatchers(HttpMethod.POST, "/Eventos/*/participantes").hasAnyAuthority(Rol.ADMIN.name(), Rol.DUEÑO.name())
                                .requestMatchers(HttpMethod.DELETE, "/Eventos/*/participantes/**").hasAnyAuthority(Rol.ADMIN.name(), Rol.DUEÑO.name())
                                // Swagger
                                .requestMatchers(
                                        "/swagger-ui.html",
                                        "/swagger-ui/**",
                                        "/v3/api-docs/**",
                                        "/api-docs/**"
                                ).permitAll()
                                // Resto autenticado
                                .anyRequest().authenticated()
                        )
                .sessionManagement( sessionManager->
                        sessionManager.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authProvider)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build()
                ;
    }

    @Bean
    public WebMvcConfigurer cors(){
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins("*")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*");
            }
        };
    }
}
