package com.resismart.backend.dashboard.Controllers;

import com.resismart.backend.dashboard.DTO.DashboardFinancieroDTO;
import com.resismart.backend.dashboard.Services.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/financiero")
    public ResponseEntity<DashboardFinancieroDTO> obtener(@RequestParam(name = "condominioId", required = false) Integer condominioId) {
        // En un escenario real, se resolvería condominioId a partir del usuario autenticado.
        DashboardFinancieroDTO dto = dashboardService.obtenerResumenFinanciero(condominioId);
        return ResponseEntity.ok(dto);
    }
}
