package com.arena.autosms5min;

import java.util.Locale;

/** Formats the remaining time based on the same due time used by the delete alarm. */
final class CountdownFormatter {
    private CountdownFormatter() { }

    /** Compact clock used inside conversation cards, e.g. 4:07. */
    static String formatRemaining(long dueAtMillis) {
        long remaining = Math.max(0L, dueAtMillis - System.currentTimeMillis());
        long minutes = remaining / 60_000L;
        long seconds = (remaining / 1_000L) % 60L;
        return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
    }

    /** Verbose inbox countdown, e.g. "1 min 05 s" or "48 segundos". */
    static String formatVerbose(long dueAtMillis) {
        long remaining = Math.max(0L, dueAtMillis - System.currentTimeMillis());
        long minutes = remaining / 60_000L;
        long seconds = (remaining / 1_000L) % 60L;
        if (minutes <= 0L) {
            return seconds == 1L ? "1 segundo" : seconds + " segundos";
        }
        return String.format(Locale.getDefault(), "%d min %02d s", minutes, seconds);
    }

    /** History countdown, e.g. "5 h 12 min", "32 min 10 s" or "6 d 4 h". */
    static String formatHistoryRemaining(long expiresAtMillis) {
        long remaining = Math.max(0L, expiresAtMillis - System.currentTimeMillis());
        long hours = remaining / 3_600_000L;
        long minutes = (remaining / 60_000L) % 60L;
        if (hours >= 48L) {
            return String.format(Locale.getDefault(), "%d d %d h", hours / 24L, hours % 24L);
        }
        if (hours >= 1L) {
            return String.format(Locale.getDefault(), "%d h %02d min", hours, minutes);
        }
        long seconds = (remaining / 1_000L) % 60L;
        if (minutes >= 1L) {
            return String.format(Locale.getDefault(), "%d min %02d s", minutes, seconds);
        }
        return seconds == 1L ? "1 segundo" : seconds + " segundos";
    }
}
