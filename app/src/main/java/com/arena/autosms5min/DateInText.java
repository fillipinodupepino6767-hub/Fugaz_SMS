package com.arena.autosms5min;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Finds the dates written INSIDE a message body, so they can be highlighted
 * where they are actually written.
 *
 * <p>This exists because passing the whole body as the "date" needle made every
 * character match itself and the entire message lit up (v0.28.8). Here we look
 * for date-shaped fragments instead, and return each one as an exact substring
 * of the original text so the caller can highlight only that range.
 *
 * <p>Only shapes that really are dates are matched: numeric d/m/y (with - or /),
 * times, and month names in Spanish. Nothing else, so ordinary words that merely
 * contain digits are left alone.
 */
final class DateInText {
    /** 19/10/2026, 19-10-26, 19/10 */
    private static final Pattern NUMERIC = Pattern.compile(
            "\\b\\d{1,2}[/-]\\d{1,2}(?:[/-]\\d{2,4})?\\b");
    /** 19/10/2026 02:00 PM, 02:00, 2:00 p.m. */
    private static final Pattern TIME = Pattern.compile(
            "\\b\\d{1,2}:\\d{2}\\s*(?:[aApP]\\.?[mM]\\.?)?\\b");
    /**
     * Spanish month names, alone or as a full date: "octubre", "5 de octubre",
     * "octubre 5". Keeps the day number with the month when it is written next
     * to it.
     */
    private static final Pattern MONTH = Pattern.compile(
            "(?:\\b\\d{1,2}\\s+de\\s+)?\\b(?:enero|febrero|marzo|abril|mayo|junio|julio|"
                    + "agosto|septiembre|setiembre|octubre|noviembre|diciembre)\\b"
                    + "(?:\\s+\\d{1,2})?", Pattern.CASE_INSENSITIVE);

    private DateInText() { }

    /**
     * Every date-like fragment of {@code body}, in the order it appears.
     * Each entry is an exact substring of the input, so offsets computed on it
     * match the real text. Overlapping matches are dropped (a time inside a
     * numeric range is not returned twice).
     */
    static java.util.List<String> find(String body) {
        java.util.List<String> found = new java.util.ArrayList<>();
        if (body == null || body.isEmpty()) return found;
        for (Pattern pattern : new Pattern[]{NUMERIC, TIME, MONTH}) {
            Matcher matcher = pattern.matcher(body);
            while (matcher.find()) {
                String candidate = matcher.group().trim();
                if (candidate.isEmpty() || overlaps(found, candidate)) continue;
                found.add(candidate);
            }
        }
        return found;
    }

    /** True when an already-collected fragment sits inside this one. */
    private static boolean overlaps(java.util.List<String> found, String candidate) {
        for (String existing : found) {
            if (existing.contains(candidate)) return true;
        }
        return false;
    }
}