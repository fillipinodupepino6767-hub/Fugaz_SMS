package com.arena.autosms5min;

import java.text.Normalizer;
import java.util.Locale;

/** Lightweight, on-device labels. Labels are advisory and never auto-block a sender. */
final class MessageClassifier {
    static final String LABEL_IMPORTANT = "Importante";
    static final String LABEL_SPAM = "Posible spam";
    static final String LABEL_NORMAL = "Normal";

    /** Display forms (with accents) for the help text; matching uses normalized forms. */
    private static final String[] IMPORTANT_DISPLAY = {
            "código", "OTP", "verificación", "Google", "banco", "bancario", "seguridad",
            "contraseña", "password", "clave", "PIN", "token", "autenticación",
            "doble factor", "2FA", "inicio de sesión", "transacción", "transferencia",
            "tarjeta", "pago", "factura", "recibo", "saldo", "retiro", "depósito",
            "movimiento", "cita", "hospital", "clínica", "EDUS", "CCSS", "póliza",
            "aseguradora", "Hacienda", "tributación", "gobierno", "migración",
            "juzgado", "notificación judicial", "escuela", "colegio", "universidad",
            "vuelo", "reserva", "fraude", "ICE", "CNFL"
    };
    private static final String[] SPAM_DISPLAY = {
            "ganaste", "premio", "reclama", "haz clic", "haz click", "clic aquí",
            "click aquí", "oferta exclusiva", "oferta", "cashback", "préstamo",
            "préstamo aprobado", "crédito preaprobado", "bono", "recarga",
            "promoción", "urgente", "gratis", "sorteo", "lotería", "ganador",
            "felicidades", "felicidades", "descuento", "solo por hoy",
            "última oportunidad", "dinero rápido", "gana dinero", "trabajo desde casa",
            "empleo inmediato", "subsidio", "bono gobierno", "ayuda del gobierno",
            "paquete retenido", "paquete", "aduana", "correos", "entrega fallida",
            "cuenta bloqueada", "cuenta suspendida", "verifica tu cuenta",
            "verifique su cuenta", "actualiza tus datos", "actualice sus datos",
            "cripto", "bitcoin", "forex", "inversión", "invierte", "casino",
            "apuesta", "apuestas", "adulto", "sexo", "viagra", "embargo", "cobro",
            "deuda vencida", "crucero", "viaje gratis", "herencia", "millonario",
            "enlace", "http", "www.", "bit.ly", "t.ly", "tinyurl"
    };
    private static final String[] IMPORTANT = normalizedAll(IMPORTANT_DISPLAY);
    private static final String[] SPAM = normalizedAll(SPAM_DISPLAY);

    private MessageClassifier() { }

    static Result classify(String body) {
        String text = normalize(body);
        for (int i = 0; i < IMPORTANT.length; i++) {
            if (matches(text, IMPORTANT[i])) {
                return new Result(LABEL_IMPORTANT, IMPORTANT_DISPLAY[i]);
            }
        }
        for (int i = 0; i < SPAM.length; i++) {
            if (matches(text, SPAM[i])) {
                return new Result(LABEL_SPAM, SPAM_DISPLAY[i]);
            }
        }
        return new Result(LABEL_NORMAL, "");
    }

    static String helpText() {
        return "La etiqueta se calcula solo en el teléfono y es orientativa: no bloquea ni elimina automáticamente.\n\n"
                + "Importante: " + join(IMPORTANT_DISPLAY) + ".\n\n"
                + "Posible spam: " + join(SPAM_DISPLAY) + ".\n\n"
                + "En Configuración → Auto-eliminación por tipo puedes decidir si cada tipo se borra solo o se conserva. "
                + "Por defecto los importantes se conservan para proteger códigos y avisos del banco. "
                + "Un mensaje legítimo puede parecer spam y viceversa; revisa siempre el remitente y el contenido.";
    }

    /**
     * Short keywords use whole-word matching so "PIN" does not match "opino"
     * and "ICE" does not match "dice". Longer keywords and phrases use substring.
     */
    private static boolean matches(String text, String keyword) {
        if (keyword.isEmpty() || text.isEmpty()) return false;
        if (keyword.length() > 4 || keyword.contains(" ")) {
            return text.contains(keyword);
        }
        int index = text.indexOf(keyword);
        while (index >= 0) {
            boolean leftOk = index == 0 || !Character.isLetterOrDigit(text.charAt(index - 1));
            int end = index + keyword.length();
            boolean rightOk = end >= text.length() || !Character.isLetterOrDigit(text.charAt(end));
            if (leftOk && rightOk) return true;
            index = text.indexOf(keyword, index + 1);
        }
        return false;
    }

    private static String join(String[] items) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < items.length; i++) {
            if (i > 0) out.append(", ");
            out.append(items[i]);
        }
        return out.toString();
    }

    private static String[] normalizedAll(String[] items) {
        String[] result = new String[items.length];
        for (int i = 0; i < items.length; i++) result[i] = normalize(items[i]);
        return result;
    }

    private static String normalize(String input) {
        if (input == null) return "";
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return normalized.toLowerCase(Locale.ROOT);
    }

    static final class Result {
        final String label;
        final String keyword;
        Result(String label, String keyword) {
            this.label = label;
            this.keyword = keyword;
        }

        String display() {
            return keyword.isEmpty() ? label : label + " · \u201C" + keyword + "\u201D";
        }
    }
}
