package com.arena.autosms5min;

import android.content.Context;
import android.content.SharedPreferences;

/** Small persistent UI and deletion preferences for this local-only SMS app. */
final class AppState {
    private static final String PREFS = "app_state";
    private static final String KEY_VISIBLE_SINCE = "visible_since";
    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_RETENTION_MS = "retention_ms";
    static final long ONE_MINUTE = 60_000L;
    static final long FIVE_MINUTES = 5L * ONE_MINUTE;
    static final long TEN_MINUTES = 10L * ONE_MINUTE;
    static final long THIRTY_MINUTES = 30L * ONE_MINUTE;
    static final long NEVER = -1L;

    private AppState() { }

    /**
     * Keeps old SMS that existed before the app was first used out of this
     * app's simple inbox. It never deletes those records.
     */
    static long visibleSince(Context context) {
        SharedPreferences prefs = prefs(context);
        long saved = prefs.getLong(KEY_VISIBLE_SINCE, -1L);
        if (saved > 0L) return saved;

        long now = System.currentTimeMillis();
        prefs.edit().putLong(KEY_VISIBLE_SINCE, now).apply();
        return now;
    }

    static boolean isDarkMode(Context context) {
        return prefs(context).getBoolean(KEY_DARK_MODE, false);
    }

    static void setDarkMode(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_DARK_MODE, enabled).apply();
    }

    /** The choice applies to new incoming SMS; existing scheduled SMS keep their original due time. */
    static long retentionMillis(Context context) {
        return prefs(context).getLong(KEY_RETENTION_MS, FIVE_MINUTES);
    }

    static void setRetentionMillis(Context context, long millis) {
        prefs(context).edit().putLong(KEY_RETENTION_MS, millis).apply();
    }

    static String retentionLabel(Context context) {
        return retentionLabel(retentionMillis(context));
    }

    static String retentionLabel(long millis) {
        if (millis == NEVER) return "Nunca automáticamente";
        if (millis == ONE_MINUTE) return "1 minuto";
        if (millis == FIVE_MINUTES) return "5 minutos";
        if (millis == TEN_MINUTES) return "10 minutos";
        if (millis == THIRTY_MINUTES) return "30 minutos";
        return "Personalizado";
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
