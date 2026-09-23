package com.arena.autosms5min;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashMap;
import java.util.Map;

/** Persists only IDs and due times for SMS received while this app is default. */
final class DeleteRegistry {
    private static final String PREFS = "pending_auto_delete";

    private DeleteRegistry() { }

    static void add(Context context, long smsId, long dueAtMillis) {
        prefs(context).edit().putLong(Long.toString(smsId), dueAtMillis).apply();
    }

    static void remove(Context context, long smsId) {
        prefs(context).edit().remove(Long.toString(smsId)).apply();
    }

    static long dueAt(Context context, long smsId) {
        return prefs(context).getLong(Long.toString(smsId), -1L);
    }

    static Map<Long, Long> all(Context context) {
        Map<String, ?> raw = prefs(context).getAll();
        Map<Long, Long> result = new HashMap<>();
        for (Map.Entry<String, ?> entry : raw.entrySet()) {
            try {
                if (entry.getValue() instanceof Long) {
                    result.put(Long.parseLong(entry.getKey()), (Long) entry.getValue());
                }
            } catch (NumberFormatException ignored) {
                // Ignore any malformed preference rather than stopping rescheduling after reboot.
            }
        }
        return result;
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
