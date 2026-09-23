package com.arena.autosms5min;

import java.text.Normalizer;
import java.util.Locale;

/** Lightweight, on-device labels. Labels are advisory and never auto-block a sender. */
final class MessageClassifier {
    private static final String[] IMPORTANT = {
            "codigo", "otp", "verification", "verificacion", "google", "banco", "seguridad",
            "contrasena", "password", "inicio de sesion", "transaccion", "transferencia", "tarjeta"
    };
    private static final String[] SPAM = {
            "ganaste", "premio", "reclama", "haz clic", "haz click", "oferta exclusiva",
            "cashback", "prestamo", "bono", "recarga", "promocion", "urgente", "gratis"
    };

    private MessageClassifier() { }

    static Result classify(String body) {
        String text = normalize(body);
        for (String keyword : IMPORTANT) {
            if (text.contains(keyword)) return new Result("Importante", keyword);
        }
        for (String keyword : SPAM) {
            if (text.contains(keyword)) return new Result("Posible spam", keyword);
        }
        return new Result("Normal", "");
    }

    static String helpText() {
        return "La etiqueta se calcula solo en el teléfono y es orientativa: no bloquea ni elimina automáticamente.\n\n"
                + "Importante: código, OTP, verificación, Google, banco, seguridad, contraseña, transacción, transferencia o tarjeta.\n\n"
                + "Posible spam: ganaste, premio, reclama, haz clic, oferta exclusiva, cashback, préstamo, bono, recarga, promoción, urgente o gratis.\n\n"
                + "Un mensaje legítimo puede parecer spam y viceversa; revisa siempre el remitente y el contenido.";
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
            return keyword.isEmpty() ? label : label + " · “" + keyword + "”";
        }
    }
}
