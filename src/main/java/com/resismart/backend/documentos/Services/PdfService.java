package com.resismart.backend.documentos.Services;

import com.resismart.backend.contratos.Entities.Contrato;
import com.resismart.backend.residentes.Entities.Residente;
import com.resismart.backend.condominios.Entities.Unidad;
import com.resismart.backend.users.Entities.Usuario;
import org.springframework.stereotype.Service;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class PdfService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public byte[] generarContratoPdf(Contrato contrato) {
        try {
            String html = buildHtml(contrato);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("No se pudo generar el PDF del contrato", e);
        }
    }

    private String buildHtml(Contrato contrato) {
        if (contrato == null) {
            throw new IllegalArgumentException("Contrato requerido");
        }
        Residente residente = contrato.getResidente();
        Usuario usuario = residente != null ? residente.getUsuario() : null;
        Unidad unidad = contrato.getUnidad();

        String nombreInquilino = usuario != null
                ? ((safe(usuario.getNombres()) + " " + safe(usuario.getApellidos())).trim())
                : "N/A";
        String unidadDesc = unidad != null ? safe(unidad.getNumero()) : "N/A";
        String monto = formatMoney(contrato.getMonto());
        String fecha = (contrato.getFechaInicio() != null ? DATE_FMT.format(contrato.getFechaInicio()) : DATE_FMT.format(LocalDate.now()));

        String htmlTemplate = """
                <html>
                <head>
                  <meta charset="UTF-8"/>
                  <style>
                    body { font-family: Arial, sans-serif; margin: 32px; }
                    h1 { color: #333; }
                    .section { margin-top: 16px; }
                    .label { font-weight: bold; }
                  </style>
                </head>
                <body>
                  <h1>Contrato de Arrendamiento</h1>
                  <div class="section"><span class="label">Inquilino:</span> {{nombreInquilino}}</div>
                  <div class="section"><span class="label">Unidad:</span> {{unidad}}</div>
                  <div class="section"><span class="label">Monto mensual:</span> {{monto}}</div>
                  <div class="section"><span class="label">Fecha inicio:</span> {{fecha}}</div>
                </body>
                </html>
                """;

        return htmlTemplate
                .replace("{{nombreInquilino}}", nombreInquilino)
                .replace("{{unidad}}", unidadDesc)
                .replace("{{monto}}", monto)
                .replace("{{fecha}}", fecha);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String formatMoney(BigDecimal value) {
        if (value == null) return "0.00";
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
