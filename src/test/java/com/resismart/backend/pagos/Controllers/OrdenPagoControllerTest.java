package com.resismart.backend.pagos.Controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resismart.backend.Auth.Jwt.JwtService;
import com.resismart.backend.pagos.DTO.GeneracionMensualResponseDTO;
import com.resismart.backend.pagos.DTO.OrdenPagoResumenDTO;
import com.resismart.backend.pagos.Enums.EstadoOrdenPago;
import com.resismart.backend.pagos.Services.OrdenPagoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrdenPagoController.class)
class OrdenPagoControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private OrdenPagoService service;

    // 🔐 Mockeamos JwtService para que el filtro JwtAuthenticationFilter se pueda crear
    @MockBean
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void listarPorContrato_deberiaRetornar200YLista() throws Exception {
        OrdenPagoResumenDTO dto = new OrdenPagoResumenDTO(
                1, 10,
                LocalDate.of(2025, 1, 1),
                new BigDecimal("100.00"),
                EstadoOrdenPago.PENDIENTE,
                LocalDate.of(2025, 1, 5),
                LocalDate.of(2025, 1, 10)
        );

        when(service.listarPorContrato(10)).thenReturn(List.of(dto));

        mvc.perform(get("/OrdenesPago/contrato/{idContrato}", 10))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].idContrato").value(10))
                .andExpect(jsonPath("$[0].monto").value(100.00));
    }

    @Test
    void listarGeneral_sinFlat_retornaPageResponse() throws Exception {
        OrdenPagoResumenDTO dto = new OrdenPagoResumenDTO(
                1, 10,
                LocalDate.of(2025, 1, 1),
                new BigDecimal("100.00"),
                EstadoOrdenPago.PENDIENTE,
                LocalDate.of(2025, 1, 5),
                LocalDate.of(2025, 1, 10)
        );

        Page<OrdenPagoResumenDTO> page = new PageImpl<>(List.of(dto), PageRequest.of(0, 20), 1);

        when(service.listarFiltrado(
                any(), any(), any(), any(),
                anyInt(), anyInt(), anyString(), any()
        )).thenReturn(page);

        mvc.perform(get("/OrdenesPago")
                        .param("page", "0")
                        .param("size", "20")
                        .param("sortBy", "fechaEmision")
                        .param("sortDir", "DESC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.content[0].id").value(1));
    }

    @Test
    void listarGeneral_conFlatTrue_retornaArregloYHeaderTotal() throws Exception {
        OrdenPagoResumenDTO dto = new OrdenPagoResumenDTO(
                1, 10,
                LocalDate.of(2025, 1, 1),
                new BigDecimal("100.00"),
                EstadoOrdenPago.PENDIENTE,
                LocalDate.of(2025, 1, 5),
                LocalDate.of(2025, 1, 10)
        );

        Page<OrdenPagoResumenDTO> page = new PageImpl<>(List.of(dto), PageRequest.of(0, 20), 1);

        when(service.listarFiltrado(
                any(), any(), any(), any(),
                anyInt(), anyInt(), anyString(), any()
        )).thenReturn(page);

        mvc.perform(get("/OrdenesPago")
                        .param("flat", "true"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "1"))
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void resumen_deberiaRetornarMapaPorEstado() throws Exception {
        Map<String, Long> resumen = Map.of(
                "PAGADA", 2L,
                "PENDIENTE", 3L
        );

        when(service.resumenPorEstado(any(), any(), any())).thenReturn(resumen);

        mvc.perform(get("/OrdenesPago/resumen"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.PAGADA").value(2))
                .andExpect(jsonPath("$.PENDIENTE").value(3));
    }

    @Test
    void ingresosMensuales_deberiaRetornarLista() throws Exception {
        var dto1 = new OrdenPagoService.IngresoMensualDTO(
                YearMonth.of(2025, 1),
                new BigDecimal("150.00")
        );
        var dto2 = new OrdenPagoService.IngresoMensualDTO(
                YearMonth.of(2025, 2),
                new BigDecimal("200.00")
        );

        when(service.ingresosMensuales(any(), any(), any()))
                .thenReturn(List.of(dto1, dto2));

        mvc.perform(get("/OrdenesPago/ingresos-mensuales"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].mes").value("2025-01"))
                .andExpect(jsonPath("$[0].montoTotal").value(150.00))
                .andExpect(jsonPath("$[1].mes").value("2025-02"))
                .andExpect(jsonPath("$[1].montoTotal").value(200.00));
    }

    @Test
    void generarParaMes_ok_deberiaRetornar200() throws Exception {
        GeneracionMensualResponseDTO dto = new GeneracionMensualResponseDTO(3, 1);
        when(service.generarParaMes(2025, 1)).thenReturn(dto);

        mvc.perform(post("/OrdenesPago/generar")
                        .with(csrf())
                        .param("anio", "2025")
                        .param("mes", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.creadas").value(3))
                .andExpect(jsonPath("$.existentes").value(1));
    }

    @Test
    void generarParaMes_parametroInvalido_deberiaRetornar400() throws Exception {
        when(service.generarParaMes(2025, 13))
                .thenThrow(new IllegalArgumentException("Mes inválido"));

        mvc.perform(post("/OrdenesPago/generar")
                        .with(csrf())
                        .param("anio", "2025")
                        .param("mes", "13"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Mes inválido"));
    }

    @Test
    void pagar_ok_deberiaRetornar200() throws Exception {
        OrdenPagoResumenDTO dto = new OrdenPagoResumenDTO(
                1, 10,
                LocalDate.of(2025, 1, 1),
                new BigDecimal("100.00"),
                EstadoOrdenPago.PAGADA,
                LocalDate.of(2025, 1, 5),
                LocalDate.of(2025, 1, 10)
        );

        when(service.marcarPagada(1)).thenReturn(dto);

        mvc.perform(post("/OrdenesPago/{id}/pagar", 1)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PAGADA"));
    }

    @Test
    void pagar_cuandoNoExisteOrden_deberiaRetornar404() throws Exception {
        when(service.marcarPagada(999))
                .thenThrow(new NoSuchElementException("Orden no encontrada"));

        mvc.perform(post("/OrdenesPago/{id}/pagar", 999)
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Orden no encontrada"));
    }
}
