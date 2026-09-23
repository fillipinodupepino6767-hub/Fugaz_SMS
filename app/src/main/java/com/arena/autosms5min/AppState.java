package com.arena.autosms5min;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Keeps a local cutoff so old messages that existed before this app was used
 * are not displayed by the app's simple inbox.
 */
final class AppState {
    private static final String PREFS = "app_state";
    private static final String KEY_VISIBLE_SINCE = "visible_since";

    private AppState() { }

    static long visibleSince(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        long saved = prefs.getLong(KEY_VISIBLE_SINCE, -1L);
        if (saved > 0L) return saved;

        long now = System.currentTimeMillis();
        prefs.edit().putLong(KEY_VISIBLE_SINCE, now).apply();
        return now;
    }
}
