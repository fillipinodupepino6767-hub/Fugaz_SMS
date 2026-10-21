package com.arena.autosms5min;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Which words and dates get highlighted inside a message, and how. Two
 * independent switches: the person's own words (they always win) and the dates
 * of incoming SMS. The style picks how the highlight is drawn by
 * {@link HighlightText}: accent colour, accent + bold, or a marker background.
 *
 * <p>Kept separate from {@link KeywordStore} on purpose: a keyword that
 * classifies a message is not necessarily a word worth colouring, and turning
 * highlighting off must never change any label.
 */
final class HighlightStore {
    private static final String PREFS = "highlight_settings";
    private static final String KEY_WORDS = "words";
    private static final String KEY_DATES = "dates";
    private static final String KEY_STYLE = "style";
    private static final int MAX_WORDS = 60;
    private static final int MAX_LENGTH = 40;

    /** How a highlighted run is drawn; see {@link HighlightText}. */
    static final int STYLE_COLOR = 0;
    static final int STYLE_COLOR_BOLD = 1;
    static final int STYLE_MARKER = 2;

    private HighlightStore() { }

    /** Highlight the person's own words. On by default: highlighting is the point. */
    static boolean wordsEnabled(Context context) {
        return prefs(context).getBoolean(KEY_WORDS, true);
    }

    static void setWordsEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_WORDS, enabled).apply();
    }

    /** Highlight the date of incoming messages. Off by default: it is noisier. */
    static boolean datesEnabled(Context context) {
        return prefs(context).getBoolean(KEY_DATES, false);
    }

    static void setDatesEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_DATES, enabled).apply();
    }

    static int style(Context context) {
        int style = prefs(context).getInt(KEY_STYLE, STYLE_COLOR_BOLD);
        if (style < STYLE_COLOR || style > STYLE_MARKER) return STYLE_COLOR_BOLD;
        return style;
    }

    static void setStyle(Context context, int style) {
        prefs(context).edit().putInt(KEY_STYLE, style).apply();
    }

    static String styleLabel(int style) {
        if (style == STYLE_COLOR) return "Solo color";
        if (style == STYLE_MARKER) return "Fondo marcado";
        return "Color y negrita";
    }

    static List<String> words(Context context) {
        if (context == null) return new ArrayList<>();
        Set<String> stored = prefs(context).getStringSet(KEY_WORDS, Collections.emptySet());
        List<String> result = new ArrayList<>(stored);
        Collections.sort(result, String.CASE_INSENSITIVE_ORDER);
        return result;
    }

    static void addWord(Context context, String word) {
        add(context, word);
    }

    static void removeWord(Context context, String word) {
        if (context == null || word == null) return;
        Set<String> updated = new LinkedHashSet<>(
                prefs(context).getStringSet(KEY_WORDS, Collections.emptySet()));
        for (String existing : new ArrayList<>(updated)) {
            if (existing.equalsIgnoreCase(word.trim())) updated.remove(existing);
        }
        prefs(context).edit().putStringSet(KEY_WORDS, updated).apply();
    }

    private static void add(Context context, String word) {
        if (context == null || word == null) return;
        String clean = word.trim();
        if (clean.isEmpty()) return;
        if (clean.length() > MAX_LENGTH) clean = clean.substring(0, MAX_LENGTH).trim();
        if (clean.isEmpty()) return;
        Set<String> updated = new LinkedHashSet<>(
                prefs(context).getStringSet(KEY_WORDS, Collections.emptySet()));
        for (String existing : new ArrayList<>(updated)) {
            if (existing.equalsIgnoreCase(clean)) updated.remove(existing);
        }
        updated.add(clean);
        while (updated.size() > MAX_WORDS) updated.remove(updated.iterator().next());
        prefs(context).edit().putStringSet(KEY_WORDS, updated).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
