package com.resismart.backend.Auth;

import com.resismart.backend.pagos.Entities.OrdenPago;
import com.resismart.backend.pagos.Entities.TransaccionPago;
import com.resismart.backend.users.Entities.Usuario;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private volatile boolean smtpConfigLogged = false;
    private volatile boolean resetBaseLogged = false;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${app.reset-password.base-url:}")
    private String resetBaseUrl;

    private static final DateTimeFormatter FECHA_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter PERIODO_FMT = DateTimeFormatter.ofPattern("MM/yyyy");

    private final Map<String, String> templateCache = new ConcurrentHashMap<>();

    public void enviarCorreoRecuperacion(String to, String token) {
        try {
            logResetBaseOnce();
            String cuerpo = buildResetEmailBody(token);
            sendEmail(to, "Recuperacion de contrasena", cuerpo);
            log.info("Correo de recuperacion enviado a {}", to);
        } catch (Exception e) {
            log.warn("No se pudo enviar correo, mostrando token en logs. Error: {}", e.getMessage());
            log.info("Token de recuperacion para {}: {}", to, token);
        }
    }

    public void enviarCredencialesUsuario(Usuario usuario, String passwordPlano) {
        if (usuario == null || usuario.getCorreo() == null || usuario.getCorreo().isBlank()) {
            log.warn("No se pudo enviar correo de credenciales: usuario o correo vacio");
            return;
        }
        try {
            String cuerpo = buildCredencialesBody(usuario, passwordPlano);
            sendEmail(usuario.getCorreo(), "Bienvenido a ResiSmart - Credenciales de acceso", cuerpo);
            log.info("Correo de credenciales enviado a {}", usuario.getCorreo());
        } catch (Exception e) {
            log.warn("No se pudo enviar correo de credenciales a {}. Error: {}", usuario.getCorreo(), e.getMessage());
        }
    }

    public void enviarAvisoOrdenPagoGenerada(Usuario usuario, OrdenPago ordenPago) {
        if (usuario == null || ordenPago == null) {
            log.warn("No se pudo enviar correo de orden de pago: datos incompletos");
            return;
        }
        if (usuario.getCorreo() == null || usuario.getCorreo().isBlank()) {
            log.warn("No se pudo enviar correo de orden de pago: usuario {} sin correo", usuario.getId_usuario());
            return;
        }
        try {
            String asunto = construirAsuntoOrdenPago(ordenPago);
            String cuerpo = buildOrdenPagoGeneradaBody(usuario, ordenPago);
            sendEmail(usuario.getCorreo(), asunto, cuerpo);
            log.info("Correo de orden de pago {} enviado a {}", ordenPago.getId(), usuario.getCorreo());
        } catch (Exception e) {
            log.warn("No se pudo enviar correo de orden de pago {} a {}. Error: {}", ordenPago.getId(), usuario.getCorreo(), e.getMessage());
        }
    }

    public void enviarAvisoOrdenPagoPagada(Usuario usuario, TransaccionPago transaccion) {
        if (usuario == null || transaccion == null || transaccion.getOrdenPago() == null) {
            log.warn("No se pudo enviar correo de pago aprobado: datos incompletos");
            return;
        }
        if (usuario.getCorreo() == null || usuario.getCorreo().isBlank()) {
            log.warn("No se pudo enviar correo de pago aprobado: usuario {} sin correo", usuario.getId_usuario());
            return;
        }
        try {
            String asunto = "Pago aprobado - Contrato #" + contratoTexto(transaccion.getOrdenPago());
            String cuerpo = buildPagoAprobadoBody(usuario, transaccion);
            sendEmail(usuario.getCorreo(), asunto, cuerpo);
            log.info("Correo de pago aprobado {} enviado a {}", transaccion.getId(), usuario.getCorreo());
        } catch (Exception e) {
            log.warn("No se pudo enviar correo de pago aprobado {} a {}. Error: {}", transaccion.getId(), usuario.getCorreo(), e.getMessage());
        }
    }

    public void enviarAvisoOrdenPagoRechazada(Usuario usuario, TransaccionPago transaccion, String motivo) {
        if (usuario == null || transaccion == null || transaccion.getOrdenPago() == null) {
            log.warn("No se pudo enviar correo de pago rechazado: datos incompletos");
            return;
        }
        if (usuario.getCorreo() == null || usuario.getCorreo().isBlank()) {
            log.warn("No se pudo enviar correo de pago rechazado: usuario {} sin correo", usuario.getId_usuario());
            return;
        }
        try {
            String asunto = "Pago rechazado - Contrato #" + contratoTexto(transaccion.getOrdenPago());
            String cuerpo = buildPagoRechazadoBody(usuario, transaccion, motivo);
            sendEmail(usuario.getCorreo(), asunto, cuerpo);
            log.info("Correo de pago rechazado {} enviado a {}", transaccion.getId(), usuario.getCorreo());
        } catch (Exception e) {
            log.warn("No se pudo enviar correo de pago rechazado {} a {}. Error: {}", transaccion.getId(), usuario.getCorreo(), e.getMessage());
        }
    }

    public void enviarAvisoOrdenPagoEnMora(Usuario usuario, OrdenPago ordenPago) {
        if (usuario == null || ordenPago == null) {
            log.warn("No se pudo enviar correo de mora: datos incompletos");
            return;
        }
        if (usuario.getCorreo() == null || usuario.getCorreo().isBlank()) {
            log.warn("No se pudo enviar correo de mora: usuario {} sin correo", usuario.getId_usuario());
            return;
        }
        try {
            String asunto = "Orden de pago en mora - Contrato " + contratoTexto(ordenPago);
            String cuerpo = buildOrdenEnMoraBody(usuario, ordenPago);
            sendEmail(usuario.getCorreo(), asunto, cuerpo);
            log.info("Correo de mora de orden {} enviado a {}", ordenPago.getId(), usuario.getCorreo());
        } catch (Exception e) {
            log.warn("No se pudo enviar correo de mora {} a {}. Error: {}", ordenPago.getId(), usuario.getCorreo(), e.getMessage());
        }
    }

    private String construirAsuntoOrdenPago(OrdenPago ordenPago) {
        Integer contratoId = ordenPago.getContrato() != null ? ordenPago.getContrato().getId() : null;
        String contratoTexto = contratoId != null ? String.valueOf(contratoId) : "N/D";
        return "Nueva orden de pago generada - Contrato #" + contratoTexto;
    }

    private String buildOrdenPagoGeneradaBody(Usuario usuario, OrdenPago ordenPago) {
        Map<String, String> data = baseEmailData(
                "Cobranza",
                "Nueva orden de pago disponible",
                buildSaludo(usuario) + " generamos una orden de pago para tu contrato."
        );
        data.put("preheader", "Orden de pago " + contratoTexto(ordenPago) + " lista para pagar.");

        LinkedHashMap<String, String> detalles = new LinkedHashMap<>();
        detalles.put("Contrato", contratoTexto(ordenPago));
        detalles.put("Periodo", formatPeriodo(ordenPago.getPeriodo()));
        detalles.put("Monto", formatMoney(ordenPago.getMontoBase()));
        detalles.put("Vencimiento", formatFecha(ordenPago.getFechaVencimiento()));
        data.put("details", buildDetailsTable(detalles));

        data.put("cta_button", buildCtaButton(frontUrl("/pagos"), "Revisar orden"));
        data.put("footer_note", "Revisa tu panel para descargar comprobantes o pagar en linea.");
        data.put("footer_extra", "ResiSmart | Pagos claros y a tiempo.");
        return renderTemplate("base", data);
    }

    private String buildPagoAprobadoBody(Usuario usuario, TransaccionPago transaccion) {
        var orden = transaccion.getOrdenPago();
        Map<String, String> data = baseEmailData(
                "Pago aprobado",
                "Tu pago fue aprobado",
                buildSaludo(usuario) + " confirmamos que tu pago fue aprobado."
        );
        data.put("preheader", "Pago aprobado para el contrato " + contratoTexto(orden) + ".");

        LinkedHashMap<String, String> detalles = new LinkedHashMap<>();
        detalles.put("Contrato", contratoTexto(orden));
        detalles.put("Periodo", formatPeriodo(orden.getPeriodo()));
        detalles.put("Monto pagado", formatMoney(transaccion.getMonto()));
        detalles.put("Fecha de pago", formatFechaHora(transaccion.getFechaPago()));
        if (transaccion.getReferencia() != null && !transaccion.getReferencia().isBlank()) {
            detalles.put("Referencia", defaultString(transaccion.getReferencia()));
        }
        data.put("details", buildDetailsTable(detalles));
        data.put("cta_button", buildCtaButton(frontUrl("/pagos"), "Ver comprobante"));
        data.put("footer_note", "Gracias por mantener tus pagos al dia.");
        data.put("footer_extra", "ResiSmart | Control total de tus pagos.");
        return renderTemplate("base", data);
    }

    private String buildPagoRechazadoBody(Usuario usuario, TransaccionPago transaccion, String motivo) {
        var orden = transaccion.getOrdenPago();
        Map<String, String> data = baseEmailData(
                "Accion requerida",
                "Tu pago no pudo aprobarse",
                buildSaludo(usuario) + " tu pago no pudo aprobarse. Te compartimos los detalles."
        );
        data.put("preheader", "Pago rechazado para el contrato " + contratoTexto(orden) + ".");

        LinkedHashMap<String, String> detalles = new LinkedHashMap<>();
        detalles.put("Contrato", contratoTexto(orden));
        detalles.put("Periodo", formatPeriodo(orden.getPeriodo()));
        detalles.put("Monto reportado", formatMoney(transaccion.getMonto()));
        detalles.put("Motivo", defaultString(motivo != null && !motivo.isBlank() ? motivo : "Revisa el comprobante y vuelve a cargarlo."));
        if (transaccion.getReferencia() != null && !transaccion.getReferencia().isBlank()) {
            detalles.put("Referencia", defaultString(transaccion.getReferencia()));
        }
        data.put("details", buildDetailsTable(detalles));
        data.put("cta_button", buildCtaButton(frontUrl("/pagos"), "Revisar y reenviar pago"));
        data.put("footer_note", "Vuelve a subir el comprobante o realiza nuevamente el pago desde el portal.");
        return renderTemplate("base", data);
    }

    private String buildOrdenEnMoraBody(Usuario usuario, OrdenPago ordenPago) {
        Map<String, String> data = baseEmailData(
                "Recordatorio de pago",
                "Tu orden entro en mora",
                buildSaludo(usuario) + " tu orden de pago entro en mora. Regulariza cuanto antes para evitar recargos."
        );
        data.put("preheader", "Orden en mora para el contrato " + contratoTexto(ordenPago) + ".");

        LinkedHashMap<String, String> detalles = new LinkedHashMap<>();
        detalles.put("Contrato", contratoTexto(ordenPago));
        detalles.put("Periodo", formatPeriodo(ordenPago.getPeriodo()));
        detalles.put("Vencimiento", formatFecha(ordenPago.getFechaVencimiento()));
        detalles.put("Mora acumulada", formatMoney(ordenPago.getMoraAcumulada()));
        detalles.put("Saldo pendiente", formatMoney(ordenPago.getSaldoPendiente()));
        data.put("details", buildDetailsTable(detalles));
        data.put("cta_button", buildCtaButton(frontUrl("/pagos"), "Regularizar pago"));
        data.put("footer_note", "Si ya realizaste el pago, este correo se actualizara al confirmarse.");
        data.put("footer_extra", "ResiSmart | Mantente al dia con tus obligaciones.");
        return renderTemplate("base", data);
    }

    private String contratoTexto(OrdenPago ordenPago) {
        return (ordenPago.getContrato() != null && ordenPago.getContrato().getId() != null)
                ? "#" + ordenPago.getContrato().getId()
                : "No definido";
    }

    private void sendEmail(String to, String subject, String body) throws Exception {
        logSmtpConfigOnce();
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(
                message,
                MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                StandardCharsets.UTF_8.name()
        );
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(body, true);
        mailSender.send(message);
    }

    private void logSmtpConfigOnce() {
        if (smtpConfigLogged) return;
        if (mailSender instanceof JavaMailSenderImpl sender) {
            var props = sender.getJavaMailProperties();
            log.info(
                    "SMTP configurado host={} port={} username={} protocol={} auth={} starttls={}",
                    sender.getHost(),
                    sender.getPort(),
                    sender.getUsername(),
                    sender.getProtocol(),
                    props.getProperty("mail.smtp.auth"),
                    props.getProperty("mail.smtp.starttls.enable")
            );
        } else {
            log.info("SMTP mailSender no es JavaMailSenderImpl, tipo={}", mailSender.getClass().getSimpleName());
        }
        smtpConfigLogged = true;
    }

    private String buildResetLink(String token) {
        String base = resolveResetBase();
        String separator = base.contains("?") ? "&" : "?";
        return base + separator + "token=" + token;
    }

    private String resolveResetBase() {
        return (resetBaseUrl != null && !resetBaseUrl.isBlank())
                ? resetBaseUrl
                : frontendUrl + "/reset-password";
    }

    private void logResetBaseOnce() {
        if (resetBaseLogged) return;
        log.info("Enlaces de reset se generaran usando base={}", resolveResetBase());
        resetBaseLogged = true;
    }

    private String buildResetEmailBody(String token) {
        Map<String, String> data = baseEmailData(
                "Seguridad de cuenta",
                "Recupera tu acceso a ResiSmart",
                "Hola, recibimos una solicitud para restablecer tu acceso. Usa el enlace o el token temporal."
        );
        data.put("preheader", "Restablece tu acceso a ResiSmart usando el token enviado.");
        data.put("highlight", buildHighlight("Token temporal", token));
        data.put("cta_button", buildCtaButton(buildResetLink(token), "Restablecer contrasena"));
        data.put("footer_note", "Si no solicitaste este cambio, puedes ignorar este mensaje.");
        data.put("footer_extra", "El enlace expira pronto por motivos de seguridad.");
        return renderTemplate("base", data);
    }

    private String buildCredencialesBody(Usuario usuario, String passwordPlano) {
        String saludo = buildSaludo(usuario);
        Map<String, String> data = baseEmailData(
                "Tu acceso esta listo",
                "Bienvenido a ResiSmart",
                saludo + " activamos tu acceso al portal. Usa estas credenciales temporales para ingresar."
        );
        data.put("preheader", "Tus credenciales para usar ResiSmart estan listas.");

        LinkedHashMap<String, String> detalles = new LinkedHashMap<>();
        detalles.put("Usuario", defaultString(usuario.getCorreo()));
        detalles.put("Contrasena temporal", defaultString(passwordPlano));
        data.put("details", buildDetailsTable(detalles));

        data.put("cta_button", buildCtaButton(frontUrl("/"), "Ingresar a ResiSmart"));
        data.put("footer_note", "Por seguridad cambia la contrasena desde tu perfil al primer ingreso.");
        return renderTemplate("base", data);
    }

    private Map<String, String> baseEmailData(String eyebrow, String title, String intro) {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("preheader", "");
        data.put("eyebrow", escape(eyebrow));
        data.put("title", escape(title));
        data.put("intro", escape(intro));
        data.put("details", "");
        data.put("highlight", "");
        data.put("cta_button", "");
        data.put("footer_note", "");
        data.put("footer_extra", "ResiSmart | Gestion sin friccion.");
        return data;
    }

    private String renderTemplate(String name, Map<String, String> values) {
        String template = loadTemplate(name);
        String html = template;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            String key = "{{" + entry.getKey() + "}}";
            String value = entry.getValue() == null ? "" : entry.getValue();
            html = html.replace(key, value);
        }
        return html.replaceAll("\\{\\{[^}]+\\}\\}", "");
    }

    private String loadTemplate(String name) {
        return templateCache.computeIfAbsent(name, key -> {
            String location = "templates/email/" + key + ".html";
            try (InputStream is = new ClassPathResource(location).getInputStream()) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new IllegalStateException("No se pudo leer la plantilla de correo " + location, e);
            }
        });
    }

    private String buildDetailsTable(Map<String, String> details) {
        if (details == null || details.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        sb.append("<table class=\"details\" role=\"presentation\"><tbody>");
        details.forEach((label, value) -> sb
                .append("<tr>")
                .append("<td class=\"label\">").append(escape(label)).append("</td>")
                .append("<td class=\"value\">").append(escape(value)).append("</td>")
                .append("</tr>")
        );
        sb.append("</tbody></table>");
        return sb.toString();
    }

    private String buildHighlight(String title, String value) {
        if (value == null || value.isBlank()) return "";
        return "<div class=\"panel\">" +
                "<p class=\"panel-title\">" + escape(title) + "</p>" +
                "<div class=\"code\">" + escape(value) + "</div>" +
                "</div>";
    }

    private String buildCtaButton(String url, String label) {
        if (url == null || url.isBlank()) return "";
        return "<a class=\"cta\" href=\"" + escapeAttribute(url) + "\" target=\"_blank\" rel=\"noopener noreferrer\">" +
                escape(label == null || label.isBlank() ? "Ir a ResiSmart" : label) +
                "</a>";
    }

    private String buildSaludo(Usuario usuario) {
        if (usuario == null) return "Hola";
        String nombre = defaultString(usuario.getNombres());
        String apellido = defaultString(usuario.getApellidos());
        String completo = (nombre + " " + apellido).trim();
        return completo.isEmpty() ? "Hola" : "Hola " + completo;
    }

    private String defaultString(String value) {
        return value == null ? "" : value.trim();
    }

    private String escape(String value) {
        if (value == null) return "";
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String escapeAttribute(String value) {
        return escape(value);
    }

    private String formatPeriodo(LocalDate periodo) {
        if (periodo == null) return "N/D";
        return YearMonth.from(periodo).format(PERIODO_FMT);
    }

    private String formatFecha(LocalDate fecha) {
        if (fecha == null) return "N/D";
        return fecha.format(FECHA_FMT);
    }

    private String formatFechaHora(LocalDateTime fechaHora) {
        if (fechaHora == null) return "N/D";
        return fechaHora.format(FECHA_HORA_FMT);
    }

    private String formatMoney(BigDecimal monto) {
        if (monto == null) return "N/D";
        DecimalFormat df = new DecimalFormat("#,##0.00");
        return "$ " + df.format(monto);
    }

    private String frontUrl(String path) {
        if (path == null || path.isBlank()) {
            return frontendUrl;
        }
        String normalized = path.startsWith("/") ? path : "/" + path;
        return frontendUrl + normalized;
    }
}
