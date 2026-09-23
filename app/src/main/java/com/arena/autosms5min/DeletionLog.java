package com.arena.autosms5min;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Short local safety log. It retains sender and preview for 24h so automatic
 * deletion remains auditable; it is not a backup and can be cleared manually.
 */
final class DeletionLog {
    private static final String PREFS = "deletion_log";
    private static final String KEY_ENTRIES = "entries";
    private static final String KEY_TOTAL = "total_deleted";
    private static final long RETENTION_MS = 24L * 60L * 60L * 1000L;
    private static final int MAX_ENTRIES = 80;

    private DeletionLog() { }

    static void add(Context context, String sender, String body, String reason) {
        List<Entry> entries = load(context);
        MessageClassifier.Result classification = MessageClassifier.classify(body);
        entries.add(0, new Entry(sender, preview(body), System.currentTimeMillis(), reason,
                classification.label, classification.keyword));
        while (entries.size() > MAX_ENTRIES) entries.remove(entries.size() - 1);
        save(context, entries);
        prefs(context).edit().putInt(KEY_TOTAL, totalDeleted(context) + 1).apply();
    }

    static void addSummary(Context context, int count, String reason) {
        if (count <= 0) return;
        List<Entry> entries = load(context);
        entries.add(0, new Entry("Limpieza de SMS", "Se eliminaron " + count + " SMS locales antiguos.",
                System.currentTimeMillis(), reason, "Limpieza", ""));
        while (entries.size() > MAX_ENTRIES) entries.remove(entries.size() - 1);
        save(context, entries);
        prefs(context).edit().putInt(KEY_TOTAL, totalDeleted(context) + count).apply();
    }

    static List<Entry> entries(Context context) {
        List<Entry> result = load(context);
        save(context, result); // Persist removal of expired entries.
        return result;
    }

    static int totalDeleted(Context context) {
        return prefs(context).getInt(KEY_TOTAL, 0);
    }

    static void clear(Context context) {
        prefs(context).edit().remove(KEY_ENTRIES).apply();
    }

    private static List<Entry> load(Context context) {
        List<Entry> result = new ArrayList<>();
        long cutoff = System.currentTimeMillis() - RETENTION_MS;
        String raw = prefs(context).getString(KEY_ENTRIES, "[]");
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.getJSONObject(i);
                Entry entry = new Entry(item.optString("sender", "Desconocido"),
                        item.optString("preview", ""), item.optLong("time", 0L),
                        item.optString("reason", "Eliminado"), item.optString("label", "Normal"),
                        item.optString("keyword", ""));
                if (entry.time >= cutoff) result.add(entry);
            }
        } catch (JSONException ignored) {
            // A malformed local log is discarded rather than affecting SMS operations.
        }
        return result;
    }

    private static void save(Context context, List<Entry> entries) {
        JSONArray array = new JSONArray();
        for (Entry entry : entries) {
            JSONObject item = new JSONObject();
            try {
                item.put("sender", entry.sender);
                item.put("preview", entry.preview);
                item.put("time", entry.time);
                item.put("reason", entry.reason);
                item.put("label", entry.label);
                item.put("keyword", entry.keyword);
                array.put(item);
            } catch (JSONException ignored) { }
        }
        prefs(context).edit().putString(KEY_ENTRIES, array.toString()).apply();
    }

    private static String preview(String body) {
        if (body == null) return "";
        String compact = body.trim().replaceAll("\\s+", " ");
        return compact.length() <= 100 ? compact : compact.substring(0, 97) + "…";
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static final class Entry {
        final String sender;
        final String preview;
        final long time;
        final String reason;
        final String label;
        final String keyword;

        Entry(String sender, String preview, long time, String reason, String label, String keyword) {
            this.sender = sender == null || sender.isEmpty() ? "Desconocido" : sender;
            this.preview = preview;
            this.time = time;
            this.reason = reason;
            this.label = label;
            this.keyword = keyword;
        }
    }
}
