package com.resismart.backend.pagos.Services;

import com.resismart.backend.Auth.EmailService;
import com.resismart.backend.avisos.Services.AvisoService;
import com.resismart.backend.pagos.Entities.OrdenPago;
import com.resismart.backend.users.Entities.Usuario;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FacturacionService {

    private final OrdenPagoService ordenPagoService;
    private final AvisoService avisoService;
    private final EmailService emailService;

    @Transactional
    public OrdenPagoService.GeneracionMensualResultado generarOrdenesParaMes(int anio, int mes) {
        return ordenPagoService.generarPendientesHasta(java.time.YearMonth.of(anio, mes));
    }

    @Transactional
    public void notificarOrdenesGeneradas(List<OrdenPago> ordenes) {
        if (ordenes == null || ordenes.isEmpty()) {
            log.info("No hay ordenes de pago nuevas para notificar");
            return;
        }
        for (OrdenPago orden : ordenes) {
            Integer contratoId = orden != null && orden.getContrato() != null ? orden.getContrato().getId() : null;
            Usuario usuario = obtenerUsuarioInquilino(orden);
            Integer usuarioId = usuario != null ? usuario.getId_usuario() : null;
            log.info("Notificando orden {} contrato={} usuario={}", orden != null ? orden.getId() : null, contratoId, usuarioId);
            try {
                avisoService.notificarOrdenPagoGenerada(orden);
            } catch (Exception e) {
                log.error("No se pudo registrar aviso para la orden {}: {}", orden != null ? orden.getId() : null, e.getMessage(), e);
            }
            if (usuario != null) {
                try {
                    emailService.enviarAvisoOrdenPagoGenerada(usuario, orden);
                } catch (Exception e) {
                    log.warn("No se pudo enviar correo para la orden {} y usuario {}: {}", orden != null ? orden.getId() : null, usuario.getId_usuario(), e.getMessage());
                }
            } else {
                log.warn("Orden {} sin usuario inquilino; se omite envio de correo", orden != null ? orden.getId() : null);
            }
        }
    }

    private Usuario obtenerUsuarioInquilino(OrdenPago orden) {
        if (orden == null || orden.getContrato() == null || orden.getContrato().getResidente() == null) {
            return null;
        }
        return orden.getContrato().getResidente().getUsuario();
    }
}
