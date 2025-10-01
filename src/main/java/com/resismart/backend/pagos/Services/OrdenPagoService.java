package com.resismart.backend.pagos.Services;

import com.resismart.backend.Common.MensajeError;
import com.resismart.backend.contratos.Entities.Contrato;
import com.resismart.backend.contratos.Enums.EstadoContrato;
import com.resismart.backend.contratos.Repositories.ContratoRepository;
import com.resismart.backend.pagos.DTO.GeneracionMensualResponseDTO;
import com.resismart.backend.pagos.DTO.OrdenPagoResumenDTO;
import com.resismart.backend.pagos.Entities.OrdenPago;
import com.resismart.backend.pagos.Enums.EstadoOrdenPago;
import com.resismart.backend.pagos.Repositories.OrdenPagoRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrdenPagoService {

    private final OrdenPagoRepository ordenRepo;
    private final ContratoRepository contratoRepo;

    private OrdenPagoResumenDTO toResumen(OrdenPago op) {
        return new OrdenPagoResumenDTO(
                op.getId(),
                op.getContrato().getId(),
                op.getPeriodo(),
                op.getMonto(),
                op.getEstado(),
                op.getFechaEmision(),
                op.getFechaVencimiento()
        );
    }

    public List<OrdenPagoResumenDTO> listarPorContrato(Integer idContrato) {
        return ordenRepo.findByContrato_Id(idContrato).stream().map(this::toResumen).toList();
    }

    @Transactional
    public GeneracionMensualResponseDTO generarParaMes(int anio, int mes) {
        YearMonth ym = YearMonth.of(anio, mes);
        LocalDate inicioMes = ym.atDay(1);
        LocalDate finMes = ym.atEndOfMonth();

        List<Contrato> contratos = contratoRepo.findActivosVigentesEn(
                EstadoContrato.ACTIVO, inicioMes, finMes);

        int creadas = 0, existentes = 0;
        for (Contrato c : contratos) {
            if (ordenRepo.existsByContrato_IdAndPeriodo(c.getId(), inicioMes)) {
                existentes++;
                continue;
            }

            OrdenPago op = OrdenPago.builder()
                    .contrato(c)
                    .periodo(inicioMes)
                    .fechaEmision(LocalDate.now())
                    .fechaVencimiento(ym.atDay(10))
                    .monto(c.getMonto())
                    .estado(EstadoOrdenPago.PENDIENTE)
                    .build();

            ordenRepo.save(op);
            creadas++;
        }
        return new GeneracionMensualResponseDTO(creadas, existentes);
    }

    @Transactional
    public OrdenPagoResumenDTO marcarPagada(Integer id) {
        OrdenPago op = ordenRepo.findById(id)
                .orElseThrow(() -> new java.util.NoSuchElementException(MensajeError.PAGO_NO_ENCONTRADO.getMensaje()));
        op.setEstado(EstadoOrdenPago.PAGADA);
        return toResumen(op);
    }
}
