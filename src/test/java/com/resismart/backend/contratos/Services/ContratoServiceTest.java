package com.resismart.backend.contratos.Services;

import com.resismart.backend.contratos.DTO.ContratoCreateDTO;
import com.resismart.backend.contratos.DTO.ContratoRenovarDTO;
import com.resismart.backend.contratos.DTO.ContratoRescindirDTO;
import com.resismart.backend.contratos.DTO.ContratoResumenDTO;
import com.resismart.backend.contratos.Entities.Contrato;
import com.resismart.backend.contratos.Enums.EstadoContrato;
import com.resismart.backend.contratos.Repositories.ContratoRepository;
import com.resismart.backend.condominios.Entities.Unidad;
import com.resismart.backend.condominios.Enums.UnidadEstado;
import com.resismart.backend.condominios.Repositories.UnidadRepository;
import com.resismart.backend.residentes.Entities.Residente;
import com.resismart.backend.residentes.Repositories.ResidenteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContratoServiceTest {

    @Mock
    private ContratoRepository contratoRepository;
    @Mock
    private UnidadRepository unidadRepository;
    @Mock
    private ResidenteRepository residenteRepository;

    @InjectMocks
    private ContratoService contratoService;

    @Test
    void crearContratoMarcaUnidadComoOcupada() {
        Unidad unidad = Unidad.builder()
                .id(10)
                .numero("A101")
                .estado(UnidadEstado.LIBRE)
                .build();
        Residente residente = Residente.builder()
                .id(20L)
                .build();
        ContratoCreateDTO dto = new ContratoCreateDTO();
        dto.setIdUnidad(10);
        dto.setIdResidente(20L);
        dto.setFechaInicio(LocalDate.of(2025, 1, 1));
        dto.setFechaFin(LocalDate.of(2025, 12, 31));
        dto.setMonto(BigDecimal.valueOf(1500));

        when(unidadRepository.findById(10)).thenReturn(Optional.of(unidad));
        when(residenteRepository.findById(20L)).thenReturn(Optional.of(residente));
        when(contratoRepository.save(any(Contrato.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ContratoResumenDTO resumen = contratoService.crear(dto);

        ArgumentCaptor<Contrato> contratoCaptor = ArgumentCaptor.forClass(Contrato.class);
        verify(contratoRepository).save(contratoCaptor.capture());
        Contrato contratoPersistido = contratoCaptor.getValue();

        assertThat(contratoPersistido.getUnidad()).isSameAs(unidad);
        assertThat(contratoPersistido.getResidente()).isSameAs(residente);
        assertThat(contratoPersistido.getEstado()).isEqualTo(EstadoContrato.ACTIVO);

        assertThat(unidad.getEstado()).isEqualTo(UnidadEstado.OCUPADA);
        assertThat(resumen.getEstado()).isEqualTo(EstadoContrato.ACTIVO);
        assertThat(resumen.getIdUnidad()).isEqualTo(unidad.getId());
        assertThat(resumen.getIdResidente()).isEqualTo(residente.getId());
    }

    @Test
    void crearContratoFallaCuandoUnidadNoEstaLibre() {
        Unidad unidad = Unidad.builder()
                .id(5)
                .estado(UnidadEstado.OCUPADA)
                .build();
        Residente residente = Residente.builder()
                .id(8L)
                .build();
        ContratoCreateDTO dto = new ContratoCreateDTO();
        dto.setIdUnidad(5);
        dto.setIdResidente(8L);
        dto.setFechaInicio(LocalDate.now());
        dto.setMonto(BigDecimal.TEN);

        when(unidadRepository.findById(5)).thenReturn(Optional.of(unidad));
        when(residenteRepository.findById(8L)).thenReturn(Optional.of(residente));

        assertThatThrownBy(() -> contratoService.crear(dto))
                .isInstanceOf(IllegalStateException.class);

        verify(contratoRepository, never()).save(any());
        assertThat(unidad.getEstado()).isEqualTo(UnidadEstado.OCUPADA);
    }

    @Test
    void rescindirContratoLiberaUnidad() {
        Unidad unidad = Unidad.builder()
                .id(7)
                .estado(UnidadEstado.OCUPADA)
                .build();
        Residente residente = Residente.builder()
                .id(15L)
                .build();
        Contrato contrato = Contrato.builder()
                .id(3)
                .unidad(unidad)
                .residente(residente)
                .estado(EstadoContrato.ACTIVO)
                .fechaInicio(LocalDate.now().minusMonths(6))
                .monto(BigDecimal.valueOf(1800))
                .build();

        when(contratoRepository.findById(3)).thenReturn(Optional.of(contrato));

        ContratoResumenDTO resumen = contratoService.rescindir(3, new ContratoRescindirDTO());

        assertThat(contrato.getEstado()).isEqualTo(EstadoContrato.RESCINDIDO);
        assertThat(unidad.getEstado()).isEqualTo(UnidadEstado.LIBRE);
        assertThat(resumen.getEstado()).isEqualTo(EstadoContrato.RESCINDIDO);
        assertThat(resumen.getIdUnidad()).isEqualTo(unidad.getId());
    }

    @Test
    void renovarContratoExtiendeVigencia() {
        Contrato contrato = Contrato.builder()
                .id(11)
                .estado(EstadoContrato.ACTIVO)
                .fechaFin(LocalDate.now().plusMonths(1))
                .build();
        ContratoRenovarDTO dto = new ContratoRenovarDTO();
        dto.setNuevaFechaFin(LocalDate.now().plusMonths(6));

        when(contratoRepository.findById(11)).thenReturn(Optional.of(contrato));

        ContratoResumenDTO resumen = contratoService.renovar(11, dto);

        assertThat(contrato.getFechaFin()).isEqualTo(dto.getNuevaFechaFin());
        assertThat(resumen.getEstado()).isEqualTo(EstadoContrato.ACTIVO);
    }

    @Test
    void renovarContratoFallaSiNoEstaActivo() {
        Contrato contrato = Contrato.builder()
                .id(12)
                .estado(EstadoContrato.RESCINDIDO)
                .build();
        ContratoRenovarDTO dto = new ContratoRenovarDTO();
        dto.setNuevaFechaFin(LocalDate.now().plusMonths(2));

        when(contratoRepository.findById(12)).thenReturn(Optional.of(contrato));

        assertThatThrownBy(() -> contratoService.renovar(12, dto))
                .isInstanceOf(IllegalStateException.class);
    }
}
