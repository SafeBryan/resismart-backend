package com.resismart.backend.pagos.Services;

import com.resismart.backend.avisos.Enums.AvisoTipo;
import com.resismart.backend.avisos.Services.AvisoService;
import com.resismart.backend.contratos.Entities.Contrato;
import com.resismart.backend.contratos.Enums.EstadoContrato;
import com.resismart.backend.contratos.Repositories.ContratoRepository;
import com.resismart.backend.pagos.DTO.GeneracionMensualResponseDTO;
import com.resismart.backend.pagos.DTO.OrdenPagoResumenDTO;
import com.resismart.backend.pagos.Entities.OrdenPago;
import com.resismart.backend.pagos.Enums.EstadoOrdenPago;
import com.resismart.backend.pagos.Repositories.OrdenPagoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrdenPagoServiceTest {

    @Mock
    private OrdenPagoRepository ordenRepo;

    @Mock
    private ContratoRepository contratoRepo;

    @Mock
    private AvisoService avisoService;

    @InjectMocks
    private OrdenPagoService service;

    @Test
    void listarPorContrato_deberiaRetornarResumenDTOs() {
        // Arrange
        OrdenPago op1 = OrdenPago.builder()
                .id(1)
                .periodo(LocalDate.of(2025, 1, 1))
                .fechaEmision(LocalDate.of(2025, 1, 5))
                .fechaVencimiento(LocalDate.of(2025, 1, 10))
                .monto(new BigDecimal("100.00"))
                .estado(EstadoOrdenPago.PENDIENTE)
                .build();

        Contrato contrato = mock(Contrato.class);
        when(contrato.getId()).thenReturn(10);
        op1.setContrato(contrato);

        when(ordenRepo.findByContrato_Id(10)).thenReturn(List.of(op1));

        // Act
        List<OrdenPagoResumenDTO> result = service.listarPorContrato(10);

        // Assert
        assertThat(result).hasSize(1);
        OrdenPagoResumenDTO dto = result.get(0);
        assertThat(dto.getId()).isEqualTo(1);
        assertThat(dto.getIdContrato()).isEqualTo(10);
        assertThat(dto.getMonto()).isEqualByComparingTo("100.00");
        assertThat(dto.getEstado()).isEqualTo(EstadoOrdenPago.PENDIENTE);
    }

    @Test
    void generarParaMes_cuandoNoExisteOrden_creaNuevaYEmiteAvisos() {
        // Arrange
        int anio = 2025, mes = 1;
        YearMonth ym = YearMonth.of(anio, mes);
        LocalDate inicioMes = ym.atDay(1);
        LocalDate finMes = ym.atEndOfMonth();

        Contrato contrato = mock(Contrato.class);
        when(contrato.getId()).thenReturn(5);
        when(contrato.getMonto()).thenReturn(new BigDecimal("250.00"));

        // mocks anidados para emitirAvisoOrden
        var residente = mock(com.resismart.backend.residentes.Entities.Residente.class);
        var usuario = mock(com.resismart.backend.users.Entities.Usuario.class);
        when(usuario.getId_usuario()).thenReturn(99);
        when(residente.getUsuario()).thenReturn(usuario);
        when(contrato.getResidente()).thenReturn(residente);

        var unidad = mock(com.resismart.backend.condominios.Entities.Unidad.class);
        var condominio = mock(com.resismart.backend.condominios.Entities.Condominio.class);
        when(condominio.getId()).thenReturn(77);
        when(unidad.getCondominio()).thenReturn(condominio);
        when(contrato.getUnidad()).thenReturn(unidad);

        when(contratoRepo.findActivosVigentesEn(EstadoContrato.ACTIVO, inicioMes, finMes))
                .thenReturn(List.of(contrato));

        when(ordenRepo.existsByContrato_IdAndPeriodo(5, inicioMes)).thenReturn(false);

        ArgumentCaptor<OrdenPago> opCaptor = ArgumentCaptor.forClass(OrdenPago.class);
        when(ordenRepo.save(opCaptor.capture())).thenAnswer(inv -> {
            OrdenPago op = inv.getArgument(0);
            op.setId(123);
            return op;
        });

        // Act
        GeneracionMensualResponseDTO resp = service.generarParaMes(anio, mes);

        // Assert
        assertThat(resp.getCreadas()).isEqualTo(1);
        assertThat(resp.getExistentes()).isEqualTo(0);

        OrdenPago guardada = opCaptor.getValue();
        assertThat(guardada.getContrato()).isEqualTo(contrato);
        assertThat(guardada.getPeriodo()).isEqualTo(inicioMes);
        assertThat(guardada.getFechaVencimiento()).isEqualTo(ym.atDay(10));
        assertThat(guardada.getMonto()).isEqualByComparingTo("250.00");
        assertThat(guardada.getEstado()).isEqualTo(EstadoOrdenPago.PENDIENTE);

        verify(avisoService).enviarAvisoUsuario(
                eq(99),
                eq(AvisoTipo.ORDEN_PAGO_GENERADA),
                anyString(),
                anyString(),
                anyMap()
        );
        verify(avisoService).enviarAvisoCondominio(
                eq(77),
                eq(AvisoTipo.ORDEN_PAGO_GENERADA),
                anyString(),
                anyString(),
                anyMap()
        );
    }

    @Test
    void generarParaMes_cuandoYaExisteOrden_noCreaNada() {
        int anio = 2025, mes = 1;
        YearMonth ym = YearMonth.of(anio, mes);
        LocalDate inicioMes = ym.atDay(1);
        LocalDate finMes = ym.atEndOfMonth();

        Contrato contrato = mock(Contrato.class);
        when(contrato.getId()).thenReturn(5);

        when(contratoRepo.findActivosVigentesEn(EstadoContrato.ACTIVO, inicioMes, finMes))
                .thenReturn(List.of(contrato));

        when(ordenRepo.existsByContrato_IdAndPeriodo(5, inicioMes)).thenReturn(true);

        GeneracionMensualResponseDTO resp = service.generarParaMes(anio, mes);

        assertThat(resp.getCreadas()).isEqualTo(0);
        assertThat(resp.getExistentes()).isEqualTo(1);

        verify(ordenRepo, never()).save(any());
        verifyNoInteractions(avisoService);
    }

    @Test
    void marcarPagada_cuandoExiste_cambiaEstadoYEmiteAvisosUsuarioYCondominio() {
        // Arrange
        OrdenPago op = OrdenPago.builder()
                .id(10)
                .estado(EstadoOrdenPago.PENDIENTE)
                .periodo(LocalDate.of(2025, 1, 1))
                .fechaEmision(LocalDate.of(2025, 1, 5))
                .fechaVencimiento(LocalDate.of(2025, 1, 10))
                .monto(new BigDecimal("100.00"))
                .build();

        // Mock contrato y grafo mínimo para emitirAvisoOrden
        Contrato contrato = mock(Contrato.class);
        when(contrato.getId()).thenReturn(3);

        var residente = mock(com.resismart.backend.residentes.Entities.Residente.class);
        var usuario = mock(com.resismart.backend.users.Entities.Usuario.class);
        when(usuario.getId_usuario()).thenReturn(99);
        when(residente.getUsuario()).thenReturn(usuario);
        when(contrato.getResidente()).thenReturn(residente);

        var unidad = mock(com.resismart.backend.condominios.Entities.Unidad.class);
        var condominio = mock(com.resismart.backend.condominios.Entities.Condominio.class);
        when(condominio.getId()).thenReturn(77);
        when(unidad.getCondominio()).thenReturn(condominio);
        when(contrato.getUnidad()).thenReturn(unidad);

        op.setContrato(contrato);

        when(ordenRepo.findById(10)).thenReturn(java.util.Optional.of(op));

        // Act
        OrdenPagoResumenDTO dto = service.marcarPagada(10);

        // Assert: estado
        assertThat(op.getEstado()).isEqualTo(EstadoOrdenPago.PAGADA);
        assertThat(dto.getEstado()).isEqualTo(EstadoOrdenPago.PAGADA);
        assertThat(dto.getId()).isEqualTo(10);
        assertThat(dto.getIdContrato()).isEqualTo(3);

        // Assert: avisos
        verify(avisoService).enviarAvisoUsuario(
                eq(99),
                eq(AvisoTipo.ORDEN_PAGO_PAGADA),
                anyString(),
                anyString(),
                anyMap()
        );
        verify(avisoService).enviarAvisoCondominio(
                eq(77),
                eq(AvisoTipo.ORDEN_PAGO_PAGADA),
                anyString(),
                anyString(),
                anyMap()
        );
    }


    @Test
    void marcarPagada_cuandoNoExiste_lanzaNoSuchElement() {
        when(ordenRepo.findById(999)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> service.marcarPagada(999))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void listarFiltrado_aplicaPaginacionYDevuelveResumen() {
        OrdenPago op = OrdenPago.builder()
                .id(1)
                .periodo(LocalDate.of(2025, 1, 1))
                .fechaEmision(LocalDate.of(2025, 1, 5))
                .fechaVencimiento(LocalDate.of(2025, 1, 10))
                .monto(new BigDecimal("100.00"))
                .estado(EstadoOrdenPago.PENDIENTE)
                .build();
        Contrato contrato = mock(Contrato.class);
        when(contrato.getId()).thenReturn(1);
        op.setContrato(contrato);

        Page<OrdenPago> page = new PageImpl<>(List.of(op), PageRequest.of(0, 20), 1);
        when(ordenRepo.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        Page<OrdenPagoResumenDTO> result = service.listarFiltrado(
                1,
                EstadoOrdenPago.PENDIENTE,
                null,
                null,
                0,
                20,
                "fechaEmision",
                Sort.Direction.DESC
        );

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getIdContrato()).isEqualTo(1);
    }

    @Test
    void resumenPorEstado_devuelveMapaConConteoPorEstado() {
        OrdenPago op1 = new OrdenPago();
        op1.setEstado(EstadoOrdenPago.PENDIENTE);

        OrdenPago op2 = new OrdenPago();
        op2.setEstado(EstadoOrdenPago.PAGADA);

        OrdenPago op3 = new OrdenPago();
        op3.setEstado(EstadoOrdenPago.PAGADA);

        when(ordenRepo.findAll(any(Specification.class)))
                .thenReturn(List.of(op1, op2, op3));

        Map<String, Long> resumen = service.resumenPorEstado(null, null, null);

        assertThat(resumen.get("PENDIENTE")).isEqualTo(1L);
        assertThat(resumen.get("PAGADA")).isEqualTo(2L);
    }

    @Test
    void ingresosMensuales_agrupaPorMesYAcumulaMontos() {
        OrdenPago op1 = new OrdenPago();
        op1.setPeriodo(LocalDate.of(2025, 1, 1));
        op1.setMonto(new BigDecimal("100.00"));
        op1.setEstado(EstadoOrdenPago.PAGADA);

        OrdenPago op2 = new OrdenPago();
        op2.setPeriodo(LocalDate.of(2025, 1, 1));
        op2.setMonto(new BigDecimal("50.00"));
        op2.setEstado(EstadoOrdenPago.PAGADA);

        OrdenPago op3 = new OrdenPago();
        op3.setPeriodo(LocalDate.of(2025, 2, 1));
        op3.setMonto(new BigDecimal("200.00"));
        op3.setEstado(EstadoOrdenPago.PAGADA);

        when(ordenRepo.findAll(any(Specification.class)))
                .thenReturn(List.of(op1, op2, op3));

        var result = service.ingresosMensuales(null, null, null);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).mes()).isEqualTo(YearMonth.of(2025, 1));
        assertThat(result.get(0).montoTotal()).isEqualByComparingTo("150.00");
        assertThat(result.get(1).mes()).isEqualTo(YearMonth.of(2025, 2));
        assertThat(result.get(1).montoTotal()).isEqualByComparingTo("200.00");
    }
}
