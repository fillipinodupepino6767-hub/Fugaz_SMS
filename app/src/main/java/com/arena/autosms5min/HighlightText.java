package com.arena.autosms5min;

import android.content.Context;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.text.style.CharacterSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Builds the coloured runs for a message body. The SMS text itself is never
 * modified: this only decorates a copy for display, exactly like
 * {@link MessageViews} never touches the stored body.
 *
 * <p>Matching mirrors {@link MessageClassifier}: accents and case are ignored,
 * and short words match whole words only, so highlighting "PIN" does not light
 * up "opino".
 */
final class HighlightText {
    private HighlightText() { }

    /**
     * @param words  words to light up; may be empty
     * @param date   a date fragment to light up when the person asked for dates
     *               (pass null when the message has no date to highlight)
     */
    static CharSequence apply(Context context, String body, List<String> words, String date) {
        String text = body == null ? "" : body;
        boolean highlightWords = words != null && !words.isEmpty();
        boolean highlightDate = date != null && !date.isEmpty();
        // Always a SpannableString, even with nothing to highlight: callers
        // (and their tests) can always treat the result as Spanned.
        SpannableString out = new SpannableString(text);
        if (text.isEmpty() || (!highlightWords && !highlightDate)) return out;

        boolean darkMode = AppState.isDarkMode(context);
        int style = HighlightStore.style(context);

        if (highlightDate) {
            style(out, text, date, darkMode, style);
        }
        if (highlightWords) {
            for (String word : sortedByLengthFirst(words)) {
                style(out, text, word, darkMode, style);
            }
        }
        return out;
    }

    /** Longer phrases first: a long match must win over a short word inside it. */
    private static List<String> sortedByLengthFirst(List<String> words) {
        List<String> copy = new ArrayList<>();
        for (String word : words) {
            if (word != null && !word.trim().isEmpty()) copy.add(word.trim());
        }
        Collections.sort(copy, (a, b) -> b.length() - a.length());
        return copy;
    }

    /**
     * Lights every whole-word occurrence of {@code needle}. Short keywords use
     * whole-word matching (so "PIN" does not match "opino"); longer ones match as
     * a substring, mirroring {@link MessageClassifier}.
     */
    private static void style(Spannable out, String text, String needle,
                              boolean darkMode, int style) {
        String haystack = normalize(text);
        String key = normalize(needle);
        if (key.isEmpty()) return;
        boolean substring = key.length() > 4 || key.contains(" ");
        int from = 0;
        while (from <= haystack.length()) {
            int index = haystack.indexOf(key, from);
            if (index < 0) return;
            int end = index + key.length();
            // normalize() only strips combining marks, so offsets stay aligned
            // with the original text: accent-insensitive matching is safe.
            boolean leftOk = index == 0 || !Character.isLetterOrDigit(haystack.charAt(index - 1));
            boolean rightOk = end >= haystack.length()
                    || !Character.isLetterOrDigit(haystack.charAt(end));
            // Skip anything a longer phrase already covered. Without this, a
            // short word inside "saldo insuficiente" would add a second,
            // overlapping span over text that is already highlighted.
            if ((substring || (leftOk && rightOk)) && !alreadyCovered(out, index, end)) {
                applySpan(out, index, end, darkMode, style);
            }
            from = index + 1;
        }
    }

    /** True when an existing highlight span already covers [start, end]. */
    private static boolean alreadyCovered(Spannable out, int start, int end) {
        CharacterSpan[] existing = out.getSpans(start, end, CharacterSpan.class);
        return existing != null && existing.length > 0;
    }

    private static void applySpan(Spannable out, int start, int end,
                                  boolean darkMode, int style) {
        int accent = ThemeColors.accent(darkMode);
        if (style == HighlightStore.STYLE_MARKER) {
            // A soft marker tint reads clearly in both themes without shouting.
            int tint = darkMode
                    ? android.graphics.Color.argb(90, 144, 202, 249)
                    : android.graphics.Color.argb(56, 21, 101, 192);
            out.setSpan(new BackgroundColorSpan(tint), start, end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            return;
        }
        out.setSpan(new ForegroundColorSpan(accent), start, end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (style == HighlightStore.STYLE_COLOR_BOLD) {
            out.setSpan(new StyleSpan(Typeface.BOLD), start, end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    /** Case- and accent-insensitive copy used only for finding offsets. */
    private static String normalize(String input) {
        if (input == null) return "";
        String stripped = java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return stripped.toLowerCase(java.util.Locale.ROOT);
    }
}
