package com.arena.autosms5min;

import android.app.Activity;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * The highlighting added in v0.28.8: chosen words and incoming dates are
 * coloured inside a copy of the message, and the stored SMS is never touched.
 *
 * <p>Span styles are asserted rather than rendered pixels: under Robolectric's
 * graphics shadow the exact colour values are not what is being tested here —
 * what matters is WHICH characters carry a highlight and how many runs there
 * are, so a change that highlights the wrong text still fails.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class HighlightTextTest {
    private ActivityController<Activity> controller;
    private Activity activity;

    @Before public void setUp() {
        controller = Robolectric.buildActivity(Activity.class).setup();
        activity = controller.get();
        activity.getSharedPreferences("highlight_settings", Activity.MODE_PRIVATE)
                .edit().clear().commit();
    }

    @After public void tearDown() {
        controller.pause().stop().destroy();
    }

    private int count(Object[] spans) {
        return spans == null ? 0 : spans.length;
    }

    @Test public void plainTextIsLeftUntouchedWhenNothingIsConfigured() {
        String body = "Tu verificacion es 1234";
        CharSequence out = HighlightText.apply(activity, body,
                Collections.<String>emptyList(), null);
        assertEquals(body, out.toString());
        assertEquals(0, count(((Spanned) out).getSpans(0, out.length(), ForegroundColorSpan.class)));
    }

    @Test public void matchingIgnoresCaseAndAccents() {
        HighlightStore.setWordsEnabled(activity, true);
        HighlightStore.setStyle(activity, HighlightStore.STYLE_COLOR);
        HighlightStore.addWord(activity, "verificación");
        CharSequence out = HighlightText.apply(activity,
                "Tu VERIFICACION llego por correo",
                HighlightStore.words(activity), null);
        ForegroundColorSpan[] spans =
                ((Spanned) out).getSpans(0, out.length(), ForegroundColorSpan.class);
        assertEquals("la palabra debe resaltarse aunque cambie mayúsculas y tilde", 1,
                count(spans));
        assertEquals("VERIFICACION", out.subSequence(spans[0].getSpanStart(),
                spans[0].getSpanEnd()).toString());
    }

    @Test public void shortWordsMatchWholeWordsOnly() {
        HighlightStore.setStyle(activity, HighlightStore.STYLE_COLOR);
        HighlightStore.addWord(activity, "PIN");
        // "opino" must NOT light up just because it contains "pin".
        CharSequence out = HighlightText.apply(activity, "opino y PIN",
                HighlightStore.words(activity), null);
        ForegroundColorSpan[] spans =
                ((Spanned) out).getSpans(0, out.length(), ForegroundColorSpan.class);
        assertEquals(1, count(spans));
        assertEquals("PIN", out.subSequence(spans[0].getSpanStart(),
                spans[0].getSpanEnd()).toString());
    }

    @Test public void styleChoosesColourBoldOrMarker() {
        HighlightStore.addWord(activity, "urgente");
        List<String> words = HighlightStore.words(activity);
        String body = "Es urgente venir";

        HighlightStore.setStyle(activity, HighlightStore.STYLE_COLOR);
        CharSequence plain = HighlightText.apply(activity, body, words, null);
        assertEquals(1, count(((Spanned) plain).getSpans(0, plain.length(),
                ForegroundColorSpan.class)));
        assertEquals("solo color no debe poner negrita", 0,
                count(((Spanned) plain).getSpans(0, plain.length(), StyleSpan.class)));

        HighlightStore.setStyle(activity, HighlightStore.STYLE_COLOR_BOLD);
        CharSequence bold = HighlightText.apply(activity, body, words, null);
        assertEquals(1, count(((Spanned) bold).getSpans(0, bold.length(), StyleSpan.class)));

        HighlightStore.setStyle(activity, HighlightStore.STYLE_MARKER);
        CharSequence marker = HighlightText.apply(activity, body, words, null);
        assertEquals(1, count(((Spanned) marker).getSpans(0, marker.length(),
                BackgroundColorSpan.class)));
        assertEquals("el fondo marcado sustituye al color", 0,
                count(((Spanned) marker).getSpans(0, marker.length(),
                        ForegroundColorSpan.class)));
    }

    @Test public void dateIsHighlightedOnlyInsideIncomingText() {
        HighlightStore.setDatesEnabled(activity, true);
        HighlightStore.setStyle(activity, HighlightStore.STYLE_COLOR);
        String date = "15 de octubre";
        CharSequence out = HighlightText.apply(activity,
                "ElXtamo es el " + date + " a las 9",
                Collections.<String>emptyList(), date);
        ForegroundColorSpan[] spans =
                ((Spanned) out).getSpans(0, out.length(), ForegroundColorSpan.class);
        assertEquals(1, count(spans));
        assertEquals(date, out.subSequence(spans[0].getSpanStart(),
                spans[0].getSpanEnd()).toString());
    }

    @Test public void longerPhrasesWinOverShortWordsInside() {
        HighlightStore.setStyle(activity, HighlightStore.STYLE_COLOR);
        HighlightStore.addWord(activity, "saldo");
        HighlightStore.addWord(activity, "saldo insuficiente");
        CharSequence out = HighlightText.apply(activity, "Tu saldo insuficiente today",
                HighlightStore.words(activity), null);
        ForegroundColorSpan[] spans =
                ((Spanned) out).getSpans(0, out.length(), ForegroundColorSpan.class);
        assertEquals("la frase larga debe cobrir la corta sin partirla", 1, count(spans));
        assertEquals("saldo insuficiente", out.subSequence(spans[0].getSpanStart(),
                spans[0].getSpanEnd()).toString());
    }

    @Test public void wordStoreReplacesCasingInsteadOfDuplicating() {
        HighlightStore.addWord(activity, "Urgente");
        HighlightStore.addWord(activity, "urgente");
        assertEquals(1, HighlightStore.words(activity).size());
        HighlightStore.removeWord(activity, "URGENTE");
        assertTrue(HighlightStore.words(activity).isEmpty());
    }

    @Test public void styleFallsBackToADefaultOutsideTheStoredRange() {
        activity.getSharedPreferences("highlight_settings", Activity.MODE_PRIVATE)
                .edit().putInt("style", 99).apply();
        assertEquals(HighlightStore.STYLE_COLOR_BOLD, HighlightStore.style(activity));
        assertTrue(Arrays.asList("Solo color", "Color y negrita", "Fondo marcado")
                .contains(HighlightStore.styleLabel(HighlightStore.STYLE_MARKER)));
    }
}