package com.arena.autosms5min;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The link handling added in v0.28.10: finding the links inside a message and
 * recognising the shorteners that hide where they really lead.
 *
 * <p>These are pure text rules, so they are tested without any Android
 * machinery: the app never opens a link and never uses the internet.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class ClipboardTest {
    @Test public void findsAPlainHttpLink() {
        List<String> links = Clipboard.links("Mira https://www.bccss.cr/turnos por favor");
        assertEquals(1, links.size());
        assertEquals("https://www.bccss.cr/turnos", links.get(0));
    }

    @Test public void findsAWwwLink() {
        List<String> links = Clipboard.links("Entra en www.ejemplo.com hoy");
        assertEquals(1, links.size());
        assertEquals("www.ejemplo.com", links.get(0));
    }

    @Test public void dropsTrailingPunctuation() {
        List<String> links = Clipboard.links("Ver https://ejemplo.com/turnos.");
        assertEquals(1, links.size());
        assertEquals("https://ejemplo.com/turnos", links.get(0));
    }

    @Test public void findsSeveralLinks() {
        List<String> links = Clipboard.links(
                "https://uno.com y https://dos.com y https://tres.com");
        assertEquals(3, links.size());
        assertEquals("https://dos.com", links.get(1));
    }

    @Test public void doesNotRepeatTheSameLink() {
        List<String> links = Clipboard.links("https://mismo.com y otra vez https://mismo.com");
        assertEquals(1, links.size());
    }

    @Test public void plainTextHasNoLinks() {
        assertTrue(Clipboard.links("Cita medica manana a las 9 con la doctora").isEmpty());
        assertTrue(Clipboard.links("").isEmpty());
        assertTrue(Clipboard.links(null).isEmpty());
    }

    @Test public void recognisesShorteners() {
        assertTrue(Clipboard.isShortener("https://bit.ly/3xYzAbC"));
        assertTrue(Clipboard.isShortener("https://tinyurl.com/abc"));
        assertTrue(Clipboard.isShortener("https://t.co/abc"));
        assertTrue(Clipboard.isShortener("https://cutt.ly/abc"));
    }

    @Test public void doesNotMistakeOrdinaryLinksForShorteners() {
        assertFalse(Clipboard.isShortener("https://www.bccss.cr/turnos"));
        assertFalse(Clipboard.isShortener("https://www.google.com/maps"));
        assertFalse(Clipboard.isShortener("https://banco.example.com/app"));
    }
}