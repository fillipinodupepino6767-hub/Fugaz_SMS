package com.arena.autosms5min;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Short local safety log, never a backup. Every entry owns an expiry timestamp.
 * Once it expires (or is cleared), it is removed permanently and is never restored
 * if the user later chooses a longer history period.
 */
final class DeletionLog {
    static final long ONE_HOUR_MS = 60L * 60L * 1000L;
    private static final String PREFS = "deletion_log";
    private static final String KEY_ENTRIES = "entries";
    private static final String KEY_TOTAL = "total_deleted";
    private static final String KEY_RETENTION = "preview_retention_ms";
    private static final long DEFAULT_RETENTION_MS = 24L * ONE_HOUR_MS;
    private static final int MAX_ENTRIES = 80;

    private DeletionLog() { }

    static void add(Context context, String sender, String body, String reason) {
        List<Entry> entries = load(context);
        MessageClassifier.Result classification = MessageClassifier.classify(body);
        long now = System.currentTimeMillis();
        entries.add(0, new Entry(sender, preview(body), now, now + retentionMillis(context), reason,
                classification.label, classification.keyword));
        trim(entries);
        save(context, entries);
        prefs(context).edit().putInt(KEY_TOTAL, totalDeleted(context) + 1).apply();
        HistoryExpiryScheduler.scheduleNext(context);
    }

    static void addSummary(Context context, int count, String reason) {
        if (count <= 0) return;
        List<Entry> entries = load(context);
        long now = System.currentTimeMillis();
        entries.add(0, new Entry("Limpieza de SMS", "Se eliminaron " + count + " SMS locales antiguos.",
                now, now + retentionMillis(context), reason, "Limpieza", ""));
        trim(entries);
        save(context, entries);
        prefs(context).edit().putInt(KEY_TOTAL, totalDeleted(context) + count).apply();
        HistoryExpiryScheduler.scheduleNext(context);
    }

    /** Returns only non-expired entries and physically removes expired entries from local storage. */
    static List<Entry> entries(Context context) {
        List<Entry> result = load(context);
        save(context, result);
        HistoryExpiryScheduler.scheduleNext(context);
        return result;
    }

    static void pruneExpired(Context context) {
        save(context, load(context));
    }

    static int totalDeleted(Context context) {
        return prefs(context).getInt(KEY_TOTAL, 0);
    }

    static void clear(Context context) {
        prefs(context).edit().remove(KEY_ENTRIES).apply();
        HistoryExpiryScheduler.scheduleNext(context);
    }

    static long retentionMillis(Context context) {
        return prefs(context).getLong(KEY_RETENTION, DEFAULT_RETENTION_MS);
    }

    /**
     * Shortening applies to existing previews too. Lengthening never extends an
     * entry already created, so a purged preview can never come back.
     */
    static void setRetentionMillis(Context context, long value) {
        long safeValue = Math.max(ONE_HOUR_MS, value);
        prefs(context).edit().putLong(KEY_RETENTION, safeValue).apply();
        List<Entry> entries = load(context); // load applies the new, possibly shorter limit.
        save(context, entries);
        HistoryExpiryScheduler.scheduleNext(context);
    }

    static String retentionLabel(Context context) {
        long hours = retentionMillis(context) / ONE_HOUR_MS;
        return hours == 1 ? "1 hora" : hours + " horas";
    }

    /** Earliest retained entry, used by the cleanup alarm. Long.MAX_VALUE means none. */
    static long nextExpiry(Context context) {
        long earliest = Long.MAX_VALUE;
        for (Entry entry : load(context)) earliest = Math.min(earliest, entry.expiresAt);
        return earliest;
    }

    private static List<Entry> load(Context context) {
        List<Entry> result = new ArrayList<>();
        long now = System.currentTimeMillis();
        long currentLimit = retentionMillis(context);
        String raw = prefs(context).getString(KEY_ENTRIES, "[]");
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.getJSONObject(i);
                long time = item.optLong("time", 0L);
                // Compatibility with v11: old entries use the former 24-hour limit.
                long savedExpiry = item.optLong("expiresAt", time + DEFAULT_RETENTION_MS);
                // A lower new setting removes older retained data; a higher setting never restores it.
                long expiresAt = Math.min(savedExpiry, time + currentLimit);
                Entry entry = new Entry(item.optString("sender", "Desconocido"),
                        item.optString("preview", ""), time, expiresAt,
                        item.optString("reason", "Eliminado"), item.optString("label", "Normal"),
                        item.optString("keyword", ""));
                if (entry.expiresAt > now) result.add(entry);
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
                item.put("expiresAt", entry.expiresAt);
                item.put("reason", entry.reason);
                item.put("label", entry.label);
                item.put("keyword", entry.keyword);
                array.put(item);
            } catch (JSONException ignored) { }
        }
        prefs(context).edit().putString(KEY_ENTRIES, array.toString()).apply();
    }

    private static void trim(List<Entry> entries) {
        while (entries.size() > MAX_ENTRIES) entries.remove(entries.size() - 1);
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
        final long expiresAt;
        final String reason;
        final String label;
        final String keyword;

        Entry(String sender, String preview, long time, long expiresAt, String reason,
              String label, String keyword) {
            this.sender = sender == null || sender.isEmpty() ? "Desconocido" : sender;
            this.preview = preview;
            this.time = time;
            this.expiresAt = expiresAt;
            this.reason = reason;
            this.label = label;
            this.keyword = keyword;
        }
    }
}
