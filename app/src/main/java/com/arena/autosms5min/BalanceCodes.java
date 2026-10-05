package com.arena.autosms5min;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Carrier balance channels for Costa Rica. USSD codes are opened in the
 * dialer (apps cannot run them directly); SMS channels are sent from the
 * SIM the user picks and the reply arrives as a normal SMS.
 */
final class BalanceCodes {
    private BalanceCodes() { }

    static final class Plan {
        final String carrierLabel;
        /** USSD code to dial, or null when the carrier has no verified code. */
        final String ussdCode;
        /** SMS short code, or null when the carrier has no SMS channel. */
        final String smsNumber;
        final String smsText;
        /** True only when the carrier publishes the SMS query as free. */
        final boolean smsFree;
        final String note;

        Plan(String carrierLabel, String ussdCode, String smsNumber, String smsText,
             boolean smsFree, String note) {
            this.carrierLabel = carrierLabel;
            this.ussdCode = ussdCode;
            this.smsNumber = smsNumber;
            this.smsText = smsText;
            this.smsFree = smsFree;
            this.note = note;
        }
    }

    static Plan forCarrier(String carrier) {
        String key = normalize(carrier);
        if (key.contains("kolbi") || key.contains("k lbi") || key.equals("ice")
                || key.contains("instituto costarricense")) {
            return new Plan("Kolbi", "*888#", "8888", "SALDO", false,
                    "Código oficial de Kolbi: marca *888# y elige la opción 1, "
                            + "o envía SALDO por SMS al 8888.");
        }
        if (key.contains("liberty") || key.contains("movistar")) {
            return new Plan("Liberty (Movistar)", null, "606", "SALDO", true,
                    "Movistar publica la consulta como SMS gratis: envía SALDO al 606. "
                            + "Si tu línea ya migró a Liberty y no responde, usa el menú de la SIM.");
        }
        if (key.contains("claro")) {
            return new Plan("Claro", "*611#", "611", "SALDO", false,
                    "Código estándar de Claro en la región: *611# o SALDO por SMS al 611. "
                            + "Si tu línea usa otro, ábrelo desde el menú de la SIM.");
        }
        return new Plan(carrier == null || carrier.trim().isEmpty()
                ? "Tu operador" : carrier.trim(), null, null, null, false,
                "Operador no reconocido: usa el menú de la SIM para ver los servicios de tu operador.");
    }

    /** Lowercase without accents so "Kölbi" matches "kolbi". */
    static String normalize(String value) {
        if (value == null) return "";
        String lower = value.trim().toLowerCase(Locale.ROOT);
        String flat = Normalizer.normalize(lower, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return flat;
    }
}
