package com.arena.autosms5min;

import android.content.Context;
import android.content.SharedPreferences;

/** Small persistent UI preferences for this local-only SMS app. */
final class AppState {
    private static final String PREFS = "app_state";
    private static final String KEY_VISIBLE_SINCE = "visible_since";
    private static final String KEY_DARK_MODE = "dark_mode";

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

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
