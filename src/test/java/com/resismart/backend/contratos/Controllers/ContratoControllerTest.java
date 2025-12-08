package com.resismart.backend.contratos.Controllers;
/*
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resismart.backend.Auth.Jwt.JwtAuthenticationFilter;
import com.resismart.backend.contratos.DTO.*;
import com.resismart.backend.contratos.Enums.EstadoContrato;
import com.resismart.backend.contratos.Services.ContratoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = ContratoController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class
)
@AutoConfigureMockMvc(addFilters = false)
class ContratoControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private ContratoService contratoService;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void crearContratoDevuelve201() throws Exception {
        ContratoResumenDTO resumen = new ContratoResumenDTO(
                1,
                LocalDate.parse("2025-01-01"),
                LocalDate.parse("2025-12-31"),
                BigDecimal.valueOf(1200),
                EstadoContrato.ACTIVO,
                10,
                "A-101",
                50L,
                "John Doe"
        );
        when(contratoService.crear(any(ContratoCreateDTO.class))).thenReturn(resumen);

        ContratoCreateDTO request = new ContratoCreateDTO();
        request.setIdUnidad(10);
        request.setIdResidente(50L);
        request.setFechaInicio(LocalDate.parse("2025-01-01"));
        request.setFechaFin(LocalDate.parse("2025-12-31"));
        request.setMonto(BigDecimal.valueOf(1200));

        mockMvc.perform(post("/Contratos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.estado").value("ACTIVO"))
                .andExpect(jsonPath("$.numeroUnidad").value("A-101"));
    }

    @Test
    void crearContratoDevuelve400CuandoServiceLanzaIllegalArgument() throws Exception {
        when(contratoService.crear(any(ContratoCreateDTO.class)))
                .thenThrow(new IllegalArgumentException("Unidad ocupada"));

        ContratoCreateDTO request = new ContratoCreateDTO();
        request.setIdUnidad(10);
        request.setIdResidente(50L);
        request.setFechaInicio(LocalDate.parse("2025-01-01"));
        request.setMonto(BigDecimal.TEN);

        mockMvc.perform(post("/Contratos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Unidad ocupada")));
    }

    @Test
    void obtenerContratoDevuelve404SiNoExiste() throws Exception {
        when(contratoService.obtener(99)).thenThrow(new java.util.NoSuchElementException("No encontrado"));

        mockMvc.perform(get("/Contratos/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("No encontrado")));
    }

    @Test
    void listarPorEstadoRetorna200() throws Exception {
        ContratoResumenDTO resumen = new ContratoResumenDTO(
                2,
                LocalDate.parse("2024-05-01"),
                LocalDate.parse("2024-11-30"),
                BigDecimal.valueOf(1000),
                EstadoContrato.RESCINDIDO,
                11,
                "B-202",
                60L,
                "Jane Smith"
        );
        when(contratoService.listarPorEstado(EstadoContrato.RESCINDIDO)).thenReturn(List.of(resumen));

        mockMvc.perform(get("/Contratos/estado/RESCINDIDO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].estado").value("RESCINDIDO"));
    }

    @Test
    void renovarContratoPropagaErroresDeValidacion() throws Exception {
        doThrow(new IllegalArgumentException("Fecha invalida"))
                .when(contratoService).renovar(eq(5), any(ContratoRenovarDTO.class));

        ContratoRenovarDTO dto = new ContratoRenovarDTO();
        dto.setNuevaFechaFin(LocalDate.now().minusDays(1));

        mockMvc.perform(post("/Contratos/5/renovar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Fecha invalida")));
    }

    @Test
    void rescindirContratoDevuelve200() throws Exception {
        ContratoResumenDTO resumen = new ContratoResumenDTO(
                3,
                LocalDate.parse("2024-01-01"),
                LocalDate.parse("2024-06-01"),
                BigDecimal.valueOf(900),
                EstadoContrato.RESCINDIDO,
                12,
                "C-303",
                77L,
                "Alex Roe"
        );
        when(contratoService.rescindir(eq(3), any(ContratoRescindirDTO.class))).thenReturn(resumen);

        mockMvc.perform(post("/Contratos/3/rescindir")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ContratoRescindirDTO())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RESCINDIDO"));
    }
}




*/