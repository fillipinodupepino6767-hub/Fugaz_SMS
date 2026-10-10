package com.arena.autosms5min;

import android.app.Activity;

/**
 * User-chosen interface scale: some phones (small icons, dense screens) render
 * the design sizes too tiny, so the whole app can be grown or shrunk from the
 * inbox or Settings. The factor multiplies every SP and DP the app lays out
 * (see {@link Ui}); activities recreate themselves after picking.
 */
final class UiScale {
    private static final float[] SCALES = {0.85f, 1f, 1.15f, 1.3f, 1.5f};
    private static final String[] LABELS = {
            "Pequeño (85 %)", "Normal (100 %)", "Grande (115 %)",
            "Muy grande (130 %)", "Enorme (150 %)"
    };

    private UiScale() { }

    /** Closest preset index to the stored scale (1 = Normal when unset). */
    static int index(android.content.Context context) {
        float current = AppState.uiScale(context);
        int best = 1;
        float bestDiff = Float.MAX_VALUE;
        for (int i = 0; i < SCALES.length; i++) {
            float diff = Math.abs(SCALES[i] - current);
            if (diff < bestDiff) {
                bestDiff = diff;
                best = i;
            }
        }
        return best;
    }

    static String label(android.content.Context context) {
        return LABELS[index(context)];
    }

    /** Preset picker; afterPick lets the caller recreate itself to apply. */
    static void showPicker(Activity activity, Runnable afterPick) {
        ThemedDialog.singleChoice(activity, "Tamaño de la interfaz",
                "Ajusta textos, botones e iconos de toda la app si en este teléfono "
                        + "se ven muy pequeños o muy grandes. Se aplica al instante "
                        + "en cada pantalla.",
                LABELS, index(activity), which -> {
                    AppState.setUiScale(activity, SCALES[which]);
                    if (afterPick != null) afterPick.run();
                });
    }
}
