package com.arena.autosms5min;

import java.util.Locale;

/** Formats the remaining time based on the same due time used by the delete alarm. */
final class CountdownFormatter {
    private CountdownFormatter() { }

    static String formatRemaining(long dueAtMillis) {
        long remaining = Math.max(0L, dueAtMillis - System.currentTimeMillis());
        long minutes = remaining / 60_000L;
        long seconds = (remaining / 1_000L) % 60L;
        return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
    }
}
