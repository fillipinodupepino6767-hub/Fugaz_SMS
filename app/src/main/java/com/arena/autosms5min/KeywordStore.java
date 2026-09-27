package com.arena.autosms5min;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * User-defined classification keywords. They take priority over the built-in
 * lists. All methods tolerate a null context (desktop unit tests) by doing nothing.
 */
final class KeywordStore {
    private static final String PREFS = "custom_keywords";
    private static final String KEY_SPAM = "spam";
    private static final String KEY_IMPORTANT = "important";
    private static final int MAX_WORDS = 100;
    private static final int MAX_LENGTH = 60;

    private KeywordStore() { }

    static List<String> spamWords(Context context) {
        return all(context, KEY_SPAM);
    }

    static List<String> importantWords(Context context) {
        return all(context, KEY_IMPORTANT);
    }

    static void addSpam(Context context, String word) {
        add(context, KEY_SPAM, word);
    }

    static void addImportant(Context context, String word) {
        add(context, KEY_IMPORTANT, word);
    }

    static void removeSpam(Context context, String word) {
        remove(context, KEY_SPAM, word);
    }

    static void removeImportant(Context context, String word) {
        remove(context, KEY_IMPORTANT, word);
    }

    private static List<String> all(Context context, String key) {
        if (context == null) return new ArrayList<>();
        Set<String> stored = prefs(context).getStringSet(key, Collections.emptySet());
        List<String> result = new ArrayList<>(stored);
        Collections.sort(result, String.CASE_INSENSITIVE_ORDER);
        return result;
    }

    private static void add(Context context, String key, String word) {
        if (context == null || word == null) return;
        String clean = word.trim();
        if (clean.isEmpty()) return;
        if (clean.length() > MAX_LENGTH) clean = clean.substring(0, MAX_LENGTH).trim();
        if (clean.isEmpty()) return;
        Set<String> updated = new LinkedHashSet<>(
                prefs(context).getStringSet(key, Collections.emptySet()));
        // Avoid near-duplicate casings of the same word.
        for (String existing : new ArrayList<>(updated)) {
            if (existing.equalsIgnoreCase(clean)) updated.remove(existing);
        }
        updated.add(clean);
        while (updated.size() > MAX_WORDS) updated.remove(updated.iterator().next());
        prefs(context).edit().putStringSet(key, updated).apply();
    }

    private static void remove(Context context, String key, String word) {
        if (context == null || word == null) return;
        Set<String> updated = new LinkedHashSet<>(
                prefs(context).getStringSet(key, Collections.emptySet()));
        for (String existing : new ArrayList<>(updated)) {
            if (existing.equalsIgnoreCase(word.trim())) updated.remove(existing);
        }
        prefs(context).edit().putStringSet(key, updated).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
