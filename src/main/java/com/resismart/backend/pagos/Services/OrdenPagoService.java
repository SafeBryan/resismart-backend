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

/**
 * Servicio de aplicación para la gestión de Órdenes de Pago.
 * <p>
 * Centraliza las reglas de negocio para:
 * <ul>
 *   <li>Listar órdenes por contrato.</li>
 *   <li>Generar automáticamente órdenes mensuales para contratos activos y vigentes en un mes dado.</li>
 *   <li>Marcar órdenes como pagadas.</li>
 * </ul>
 *
 * <h3>Políticas clave</h3>
 * <ul>
 *   <li>Unicidad por (contrato, período). Antes de crear, se verifica existencia para evitar duplicados.</li>
 *   <li>El período se representa con el primer día del mes (YYYY-MM-01).</li>
 *   <li>Fecha de vencimiento por defecto: día 10 del mes facturado (configurable a futuro).</li>
 * </ul>
 *
 * <h3>Transaccionalidad</h3>
 * Los métodos que mutan estado están anotados con {@link Transactional} para asegurar atomicidad
 * entre operaciones de lectura/escritura y consistencia de datos.
 *
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
public class OrdenPagoService {

    private final OrdenPagoRepository ordenRepo;
    private final ContratoRepository contratoRepo;

    // ===========================
    // Mapeadores internos
    // ===========================

    /**
     * Convierte una entidad {@link OrdenPago} a su representación resumida.
     *
     * @param op entidad de orden de pago (no nula).
     * @return DTO con vista resumida de la orden.
     */
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

    // ===========================
    // Casos de uso
    // ===========================

    /**
     * Lista todas las órdenes de pago asociadas a un contrato.
     *
     * @param idContrato identificador del contrato.
     * @return lista de {@link OrdenPagoResumenDTO} para el contrato dado (posiblemente vacía).
     */
    public List<OrdenPagoResumenDTO> listarPorContrato(Integer idContrato) {
        return ordenRepo.findByContrato_Id(idContrato).stream().map(this::toResumen).toList();
    }

    /**
     * Genera órdenes de pago para todos los contratos en estado {@link EstadoContrato#ACTIVO}
     * que estén vigentes durante el mes indicado.
     * <p>
     * Reglas:
     * <ul>
     *   <li>El período de facturación se fija al primer día del mes: {@code YYYY-MM-01}.</li>
     *   <li>Si ya existe una orden para (contrato, período), se omite (se cuenta como existente).</li>
     *   <li>La fecha de emisión se fija al día de ejecución y la de vencimiento al día 10 del mes.</li>
     *   <li>El monto de la orden copia el monto del contrato vigente.</li>
     * </ul>
     *
     * @param anio año objetivo (p. ej., 2025).
     * @param mes  mes objetivo (1–12).
     * @return {@link GeneracionMensualResponseDTO} con contadores de creadas y existentes.
     */
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

    /**
     * Marca una orden de pago como {@link EstadoOrdenPago#PAGADA}.
     *
     * @param id identificador de la orden de pago.
     * @return {@link OrdenPagoResumenDTO} con el nuevo estado.
     * @throws java.util.NoSuchElementException si la orden no existe ({@link MensajeError#PAGO_NO_ENCONTRADO}).
     */
    @Transactional
    public OrdenPagoResumenDTO marcarPagada(Integer id) {
        OrdenPago op = ordenRepo.findById(id)
                .orElseThrow(() -> new java.util.NoSuchElementException(MensajeError.PAGO_NO_ENCONTRADO.getMensaje()));
        op.setEstado(EstadoOrdenPago.PAGADA);
        return toResumen(op);
    }
}
