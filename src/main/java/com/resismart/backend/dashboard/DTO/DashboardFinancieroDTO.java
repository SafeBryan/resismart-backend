package com.resismart.backend.dashboard.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class DashboardFinancieroDTO {
    private BigDecimal ingresosDelMes;
    private BigDecimal totalDeudaPorCobrar;
    private BigDecimal balanceGeneral;
    private List<TransaccionRecienteDTO> transaccionesRecientes;
}
