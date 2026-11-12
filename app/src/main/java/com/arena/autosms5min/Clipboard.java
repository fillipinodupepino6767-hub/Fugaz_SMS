package com.arena.autosms5min;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Everything the app does with the phone clipboard: copying a message, copying
 * one of its links, pasting into the compose box, and wiping the clipboard
 * again when the copied text was sensitive.
 *
 * <p>The app stays completely offline: nothing here touches the network.
 *
 * <p>Security note: on Android 10+ an app may only read the clipboard while it
 * is actually in the foreground. That is exactly how {@link #read} is used (the
 * person is looking at the compose screen), so no special permission is needed
 * and none is requested.
 */
final class Clipboard {
    /** How long a sensitive copy (bank codes, OTP) is allowed to stay. */
    private static final long SENSITIVE_CLEAR_DELAY_MS = 60_000L;

    /**
     * Links inside a message. Kept deliberately simple: http(s) addresses,
     * "www." addresses and the common shorteners seen in SMS.
     */
    private static final Pattern URL = Pattern.compile(
            "(?:https?://|www\\.)[^\\s<>\"'()\\[\\]]+", Pattern.CASE_INSENSITIVE);

    /** Shorteners that hide the real destination behind a short name. */
    private static final Pattern SHORTENER = Pattern.compile(
            "(?:https?://)?(?:www\\.)?(?:bit\\.ly|tinyurl\\.com|t\\.co|t\\.ly|"
                    + "cutt\\.ly|is\\.gd|goo\\.gl|ow\\.ly|buff\\.ly|rebrand\\.ly|"
                    + "shorturl\\.at|rb\\.gy|t\\.me|lnkd\\.in)\\b[^\\s<>\"'()\\[\\]]*",
            Pattern.CASE_INSENSITIVE);

    private Clipboard() { }

    /**
     * Copies text and tells the person what happened.
     *
     * @param sensitive when true the clipboard is wiped again after a minute,
     *                  because the text is a bank code or similar
     */
    static void copyText(Context context, String text, String whatCopied, boolean sensitive) {
        if (text == null || text.isEmpty()) return;
        ClipboardManager manager = manager(context);
        if (manager == null) {
            Toast.makeText(context, "No se pudo copiar.", Toast.LENGTH_SHORT).show();
            return;
        }
        manager.setPrimaryClip(ClipData.newPlainText(whatCopied, text));
        Toast.makeText(context, whatCopied + " copiado al portapapeles.",
                Toast.LENGTH_SHORT).show();
        if (sensitive) {
            lastSensitiveText = text;
            scheduleSensitiveClear(context);
        }
    }

    /** Copies a link, warning when it is a shortener that hides its target. */
    static void copyLink(Context context, String url, boolean isShortener) {
        if (url == null || url.isEmpty()) return;
        copyText(context, url, "Enlace", false);
        if (isShortener) {
            // The app never opens links and never asks the network anything, so
            // it cannot expand a shortener. It can still warn the person.
            Toast.makeText(context,
                    "Es un enlace acortado: puede llevar a cualquier página. "
                            + "Ábrelo solo si confías.", Toast.LENGTH_LONG).show();
        }
    }

    /** Wipes the clipboard shortly after a sensitive copy. */
    private static void scheduleSensitiveClear(final Context context) {
        final Context app = context.getApplicationContext();
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override public void run() {
                ClipboardManager manager = manager(app);
                if (manager == null) return;
                // Only wipe it if our own text is still there: the person may
                // have copied something else in the meantime.
                ClipData clip = manager.getPrimaryClip();
                if (clip == null || clip.getItemCount() == 0) return;
                CharSequence current = clip.getItemAt(0).getText();
                if (current == null || !current.toString().equals(lastSensitiveText)) return;
                manager.setPrimaryClip(ClipData.newPlainText("", ""));
                Toast.makeText(app, "Portapapeles borrado (era un código sensible).",
                        Toast.LENGTH_LONG).show();
            }
        }, SENSITIVE_CLEAR_DELAY_MS);
    }

    /** The text we scheduled to wipe, so we never erase something else. */
    private static String lastSensitiveText;

    /** Reads the clipboard; only valid while the app is in the foreground. */
    static String read(Context context) {
        ClipboardManager manager = manager(context);
        if (manager == null) return "";
        ClipData clip = manager.getPrimaryClip();
        if (clip == null || clip.getItemCount() == 0) return "";
        CharSequence text = clip.getItemAt(0).getText();
        return text == null ? "" : text.toString();
    }

    /** Every link found in a message, in the order it appears. */
    static java.util.List<String> links(String body) {
        java.util.List<String> found = new java.util.ArrayList<>();
        if (body == null || body.isEmpty()) return found;
        Matcher matcher = URL.matcher(body);
        while (matcher.find()) {
            String link = matcher.group().trim();
            // Trailing punctuation is common after a link in a sentence.
            while (link.endsWith(".") || link.endsWith(",") || link.endsWith(";")) {
                link = link.substring(0, link.length() - 1);
            }
            if (!link.isEmpty() && !found.contains(link)) found.add(link);
        }
        return found;
    }

    /** True when a link hides its destination behind a shortener. */
    static boolean isShortener(String link) {
        return link != null && SHORTENER.matcher(link).matches();
    }

    private static ClipboardManager manager(Context context) {
        return (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
    }
}
