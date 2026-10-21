package com.arena.autosms5min;

import android.app.Activity;
import android.os.Looper;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.Shadows;
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

    /**
     * SharedPreferences.apply() writes asynchronously, so the store must be
     * flushed to the (in-memory) map before a test reads it back. Draining the
     * main looper is what actually runs those pending writes.
     */
    private void idle() {
        Shadows.shadowOf(Looper.getMainLooper()).idle();
    }

    /** The highlighted text of the first span of the given class. */
    private String highlighted(CharSequence out, Class<?> type) {
        Spanned spanned = (Spanned) out;
        Object[] spans = spanned.getSpans(0, out.length(), type);
        assertTrue("se esperaba al menos un resaltado de tipo " + type.getSimpleName(),
                count(spans) > 0);
        return out.subSequence(spanned.getSpanStart(spans[0]),
                spanned.getSpanEnd(spans[0])).toString();
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
        idle();
        HighlightStore.setStyle(activity, HighlightStore.STYLE_COLOR);
        idle();
        HighlightStore.addWord(activity, "verificación");
        idle();
        CharSequence out = HighlightText.apply(activity,
                "Tu VERIFICACION llego por correo",
                HighlightStore.words(activity), null);
        assertEquals("la palabra debe resaltarse aunque cambie mayúsculas y tilde", 1,
                count(((Spanned) out).getSpans(0, out.length(), ForegroundColorSpan.class)));
        assertEquals("VERIFICACION", highlighted(out, ForegroundColorSpan.class));
    }

    @Test public void shortWordsMatchWholeWordsOnly() {
        HighlightStore.setStyle(activity, HighlightStore.STYLE_COLOR);
        idle();
        HighlightStore.addWord(activity, "PIN");
        idle();
        // "opino" must NOT light up just because it contains "pin".
        CharSequence out = HighlightText.apply(activity, "opino y PIN",
                HighlightStore.words(activity), null);
        assertEquals(1, count(((Spanned) out).getSpans(0, out.length(),
                ForegroundColorSpan.class)));
        assertEquals("PIN", highlighted(out, ForegroundColorSpan.class));
    }

    @Test public void styleChoosesColourBoldOrMarker() {
        HighlightStore.addWord(activity, "urgente");
        idle();
        List<String> words = HighlightStore.words(activity);
        String body = "Es urgente venir";

        HighlightStore.setStyle(activity, HighlightStore.STYLE_COLOR);
        idle();
        CharSequence plain = HighlightText.apply(activity, body, words, null);
        assertEquals(1, count(((Spanned) plain).getSpans(0, plain.length(),
                ForegroundColorSpan.class)));
        assertEquals("solo color no debe poner negrita", 0,
                count(((Spanned) plain).getSpans(0, plain.length(), StyleSpan.class)));

        HighlightStore.setStyle(activity, HighlightStore.STYLE_COLOR_BOLD);
        idle();
        CharSequence bold = HighlightText.apply(activity, body, words, null);
        assertEquals(1, count(((Spanned) bold).getSpans(0, bold.length(), StyleSpan.class)));

        HighlightStore.setStyle(activity, HighlightStore.STYLE_MARKER);
        idle();
        CharSequence marker = HighlightText.apply(activity, body, words, null);
        assertEquals(1, count(((Spanned) marker).getSpans(0, marker.length(),
                BackgroundColorSpan.class)));
        assertEquals("el fondo marcado sustituye al color", 0,
                count(((Spanned) marker).getSpans(0, marker.length(),
                        ForegroundColorSpan.class)));
    }

    @Test public void dateIsHighlightedOnlyInsideIncomingText() {
        HighlightStore.setDatesEnabled(activity, true);
        idle();
        HighlightStore.setStyle(activity, HighlightStore.STYLE_COLOR);
        idle();
        String date = "15 de octubre";
        CharSequence out = HighlightText.apply(activity,
                "ElXtamo es el " + date + " a las 9",
                Collections.<String>emptyList(), Collections.singletonList(date));
        assertEquals(1, count(((Spanned) out).getSpans(0, out.length(),
                ForegroundColorSpan.class)));
        assertEquals(date, highlighted(out, ForegroundColorSpan.class));
    }

    @Test public void longerPhrasesWinOverShortWordsInside() {
        HighlightStore.setStyle(activity, HighlightStore.STYLE_COLOR);
        idle();
        HighlightStore.addWord(activity, "saldo");
        idle();
        HighlightStore.addWord(activity, "saldo insuficiente");
        idle();
        CharSequence out = HighlightText.apply(activity, "Tu saldo insuficiente today",
                HighlightStore.words(activity), null);
        assertEquals("la frase larga debe cubrir la corta sin partirla", 1,
                count(((Spanned) out).getSpans(0, out.length(), ForegroundColorSpan.class)));
        assertEquals("saldo insuficiente", highlighted(out, ForegroundColorSpan.class));
    }

    @Test public void onlyTheDateIsMarkedNotTheWholeMessage() {
        // Regression for v0.28.8: the inbox passed the ENTIRE body as the date
        // needle, so the message matched itself and every character lit up.
        // Dates must come from DateInText and cover only themselves.
        HighlightStore.setDatesEnabled(activity, true);
        HighlightStore.setStyle(activity, HighlightStore.STYLE_COLOR);
        String body = "PHILLIP LE GRAY VARGAS la CCSS recuerda su cita PRESENCIAL: "
                + "HOSPITAL SAN RAFAEL DE ALAJUEL, ODONTOLOGIA GENERAL el 19/10/2026 02:00 PM.";
        List<String> dates = DateInText.find(body);
        assertEquals("debe detectar la fecha numérica", 1, dates.size());
        assertEquals("19/10/2026", dates.get(0));
        CharSequence out = HighlightText.apply(activity, body,
                Collections.<String>emptyList(), dates);
        assertEquals("solo la fecha se resalta, no todo el mensaje", 1,
                count(((Spanned) out).getSpans(0, out.length(), ForegroundColorSpan.class)));
        assertEquals("19/10/2026", highlighted(out, ForegroundColorSpan.class));
    }

    @Test public void dateFinderLeavesOrdinaryTextAlone() {
        assertTrue("sin fechas no hay nada que resaltar",
                DateInText.find("Llama al 5061234 cuando puedas").isEmpty());
        List<String> found = DateInText.find("Traigo 3 tortillas y pago 2500 colones");
        assertTrue("números sueltos no son fechas", found.isEmpty());
    }

    @Test public void wordStoreReplacesCasingInsteadOfDuplicating() {
        HighlightStore.addWord(activity, "Urgente");
        idle();
        HighlightStore.addWord(activity, "urgente");
        idle();
        assertEquals(1, HighlightStore.words(activity).size());
        HighlightStore.removeWord(activity, "URGENTE");
        idle();
        assertTrue(HighlightStore.words(activity).isEmpty());
    }

    @Test public void togglingTheSwitchesDoesNotCorruptTheWordList() {
        // Regression: the boolean switch and the word list once shared the
        // prefs key "words", so toggling highlighting made the word list read
        // throw "Boolean cannot be cast to Set". Each key holds one type only.
        HighlightStore.setWordsEnabled(activity, false);
        idle();
        HighlightStore.addWord(activity, "banco");
        idle();
        assertEquals("la lista sobrevive al interruptor apagado", 1,
                HighlightStore.words(activity).size());
        HighlightStore.setWordsEnabled(activity, true);
        idle();
        HighlightStore.setDatesEnabled(activity, true);
        idle();
        assertEquals("banco", HighlightStore.words(activity).get(0));
        assertTrue(HighlightStore.wordsEnabled(activity));
        assertTrue(HighlightStore.datesEnabled(activity));
    }

    @Test public void styleFallsBackToADefaultOutsideTheStoredRange() {
        activity.getSharedPreferences("highlight_settings", Activity.MODE_PRIVATE)
                .edit().putInt("highlight_style", 99).apply();
        assertEquals(HighlightStore.STYLE_COLOR_BOLD, HighlightStore.style(activity));
        assertTrue(Arrays.asList("Solo color", "Color y negrita", "Fondo marcado")
                .contains(HighlightStore.styleLabel(HighlightStore.STYLE_MARKER)));
    }
}