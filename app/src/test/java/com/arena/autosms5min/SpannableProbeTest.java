package com.arena.autosms5min;

import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;

/**
 * Probe: which spannable classes actually survive Robolectric's shadow as
 * Spanned? Temporary scaffolding to pin down a ClassCastException seen in
 * HighlightTextTest; removed once the answer is known.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class SpannableProbeTest {
    @Test public void reportWhatIsSpanned() {
        Object builder = new SpannableStringBuilder("hola");
        Object string = new SpannableString("hola");
        System.out.println("PROBE builder class = " + builder.getClass().getName());
        System.out.println("PROBE builder isSpanned = " + (builder instanceof Spanned));
        System.out.println("PROBE string class = " + string.getClass().getName());
        System.out.println("PROBE string isSpanned = " + (string instanceof Spanned));
        System.out.println("PROBE builder toString = " + builder.toString());
        assertEquals("probe", "hola", builder.toString());
    }
}