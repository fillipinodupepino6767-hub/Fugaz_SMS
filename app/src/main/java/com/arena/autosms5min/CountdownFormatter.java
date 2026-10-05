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

    /**
     * History countdown, e.g. "7 h 23 min 45 s", "32 min 10 s" or "6 d 4 h 12 min".
     * Long spans always include minutes (and seconds under 48 h) so they visibly
     * tick instead of looking frozen for a whole hour.
     */
    static String formatHistoryRemaining(long expiresAtMillis) {
        long remaining = Math.max(0L, expiresAtMillis - System.currentTimeMillis());
        long hours = remaining / 3_600_000L;
        long minutes = (remaining / 60_000L) % 60L;
        long seconds = (remaining / 1_000L) % 60L;
        if (hours >= 48L) {
            return String.format(Locale.getDefault(), "%d d %d h %02d min",
                    hours / 24L, hours % 24L, minutes);
        }
        if (hours >= 1L) {
            return String.format(Locale.getDefault(), "%d h %02d min %02d s", hours, minutes, seconds);
        }
        if (minutes >= 1L) {
            return String.format(Locale.getDefault(), "%d min %02d s", minutes, seconds);
        }
        return seconds == 1L ? "1 segundo" : seconds + " segundos";
    }
}
