package com.arena.autosms5min;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Local SMS-only sender blocklist. Blocking does not block phone calls. */
final class Blocklist {
    private static final String PREFS = "blocked_senders";
    private static final String KEY_SENDERS = "senders";

    private Blocklist() { }

    static boolean isBlocked(Context context, String sender) {
        return senders(context).contains(normalize(sender));
    }

    static void block(Context context, String sender) {
        String normalized = normalize(sender);
        if (normalized.isEmpty()) return;
        Set<String> updated = senders(context);
        updated.add(normalized);
        save(context, updated);
    }

    static void unblock(Context context, String sender) {
        Set<String> updated = senders(context);
        updated.remove(normalize(sender));
        save(context, updated);
    }

    static List<String> all(Context context) {
        List<String> result = new ArrayList<>(senders(context));
        Collections.sort(result, String.CASE_INSENSITIVE_ORDER);
        return result;
    }

    static int count(Context context) {
        return senders(context).size();
    }

    static String normalize(String sender) {
        if (sender == null) return "";
        // Preserve letters for branded senders/short codes and normalize common number formatting.
        return sender.trim().replaceAll("[\\s()\\-.]", "").toUpperCase(Locale.ROOT);
    }

    private static Set<String> senders(Context context) {
        Set<String> stored = prefs(context).getStringSet(KEY_SENDERS, Collections.emptySet());
        return new LinkedHashSet<>(stored);
    }

    private static void save(Context context, Set<String> senders) {
        prefs(context).edit().putStringSet(KEY_SENDERS, new LinkedHashSet<>(senders)).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
