package com.arena.autosms5min;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

import java.util.Iterator;
import java.util.Map;

/**
 * Exports/imports the app's state as JSON so everything survives a phone
 * change or a reinstall: theme, retention, swipes, blocklist, keywords, SIM
 * choices, the app PIN (hash only) and fingerprint opt-in, archived message
 * IDs, the recently-deleted log, pending auto-delete schedules, the ringer
 * timer and setup reminders. Help/welcome keys are skipped so the new phone
 * still gets its first-run messages.
 */
final class SettingsPort {
    private static final String[] PREF_FILES = {
            "app_state", "blocked_senders", "custom_keywords", "app_lock",
            "archive_store", "deletion_log", "pending_auto_delete",
            "ringer_timer", "setup_helper"
    };
    private static final String[] SKIP_KEYS = {
            "help_shown_version"
    };

    private SettingsPort() { }

    static String exportJson(Context context) throws Exception {
        JSONObject root = new JSONObject();
        for (String name : PREF_FILES) {
            JSONObject file = new JSONObject();
            SharedPreferences prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE);
            for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
                if (shouldSkip(name, entry.getKey())) continue;
                Object value = entry.getValue();
                JSONObject item = new JSONObject();
                if (value instanceof String) {
                    item.put("t", "s").put("v", (String) value);
                } else if (value instanceof Boolean) {
                    item.put("t", "b").put("v", ((Boolean) value) ? 1 : 0);
                } else if (value instanceof Long) {
                    item.put("t", "l").put("v", ((Long) value).longValue());
                } else if (value instanceof Integer) {
                    item.put("t", "i").put("v", ((Integer) value).intValue());
                } else if (value instanceof Float) {
                    item.put("t", "f").put("v", ((Float) value).floatValue());
                } else if (value instanceof java.util.Set) {
                    org.json.JSONArray array = new org.json.JSONArray();
                    for (Object element : (java.util.Set<?>) value) {
                        array.put(String.valueOf(element));
                    }
                    item.put("t", "set").put("v", array);
                } else {
                    continue;
                }
                file.put(entry.getKey(), item);
            }
            root.put(name, file);
        }
        return root.toString(2);
    }

    /** Replaces the stored preferences with the JSON content; returns values applied. */
    static int importJson(Context context, String json) throws Exception {
        JSONObject root = new JSONObject(json);
        int applied = 0;
        for (String name : PREF_FILES) {
            if (!root.has(name)) continue;
            JSONObject file = root.getJSONObject(name);
            SharedPreferences prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit().clear();
            for (Iterator<String> keys = file.keys(); keys.hasNext(); ) {
                String key = keys.next();
                if (shouldSkip(name, key)) continue;
                JSONObject item = file.getJSONObject(key);
                String type = item.getString("t");
                switch (type) {
                    case "s": editor.putString(key, item.getString("v")); break;
                    case "b": editor.putBoolean(key, item.getInt("v") != 0); break;
                    case "l": editor.putLong(key, item.getLong("v")); break;
                    case "i": editor.putInt(key, item.getInt("v")); break;
                    case "f": editor.putFloat(key, (float) item.getDouble("v")); break;
                    case "set": {
                        org.json.JSONArray array = item.getJSONArray("v");
                        java.util.Set<String> set = new java.util.HashSet<>();
                        for (int i = 0; i < array.length(); i++) set.add(array.getString(i));
                        editor.putStringSet(key, set);
                        break;
                    }
                    default: continue;
                }
                applied++;
            }
            editor.commit();
        }
        return applied;
    }

    private static boolean shouldSkip(String file, String key) {
        if (!"app_state".equals(file)) return false;
        for (String skip : SKIP_KEYS) {
            if (skip.equals(key)) return true;
        }
        return false;
    }
}
