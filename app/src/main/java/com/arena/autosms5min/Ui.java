package com.arena.autosms5min;

import android.content.Context;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.Button;
import android.widget.TextView;

/**
 * Shared sizing so the interface looks the SAME on every phone: the app no
 * longer follows the system font size or the "display size" setting, and no
 * longer auto-boosts small screens. Every screen is designed for a 360 dp wide
 * phone and scaled with the person's own choice ({@link AppState#uiScale},
 * the 70 %–150 % slider in {@link UiScale}); 100 % is the default everywhere.
 *
 * <p>Layout uses "design dp" and text uses "design sp"; both convert to pixels
 * through the same {@linkplain #stableDensity}, so system settings cannot grow
 * or shrink one without the other.
 */
final class Ui {
    /** Width the whole interface is designed for; 100 % spans exactly this. */
    private static final float DESIGN_WIDTH_DP = 360f;

    // Cache: density is requested for every view; recomputed only when the
    // visible screen changes (rotation keeps the smallest side, so no churn).
    private static int cachedSmallestPx = -1;
    private static float cachedDensity = -1f;

    private Ui() { }

    /**
     * Pixels per design dp: the screen's smallest side in real pixels divided
     * by the design width. Real pixels do not change when the person tweaks
     * the phone's font size or display size, so this density is stable.
     */
    static float stableDensity(Context context) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        int smallestPx = Math.min(metrics.widthPixels, metrics.heightPixels);
        if (smallestPx != cachedSmallestPx) {
            cachedSmallestPx = smallestPx;
            cachedDensity = Math.max(1, smallestPx) / DESIGN_WIDTH_DP;
        }
        return cachedDensity;
    }

    /** Design dp → pixels at the person's current interface scale. */
    static int dp(Context context, float value) {
        return dp(context, value, AppState.uiScale(context));
    }

    /** Design dp → pixels at an explicit scale (live slider preview). */
    static int dp(Context context, float value, float scale) {
        return Math.round(value * stableDensity(context) * scale);
    }

    /** Design text size → pixels at the person's current interface scale. */
    static float textPx(Context context, float designSp) {
        return textPx(context, designSp, AppState.uiScale(context));
    }

    /** Design text size → pixels at an explicit scale (live slider preview). */
    static float textPx(Context context, float designSp, float scale) {
        return designSp * stableDensity(context) * scale;
    }

    /**
     * Applies the design text size in pixels. Using PX (not SP) is what keeps
     * the system font size out of the app; the slider is the only multiplier.
     */
    static void text(TextView view, float designSp) {
        view.setTextSize(TypedValue.COMPLEX_UNIT_PX, textPx(view.getContext(), designSp));
    }

    /** Preview variant: sizes the view at a scale that is not saved yet. */
    static void text(TextView view, float designSp, float scale) {
        view.setTextSize(TypedValue.COMPLEX_UNIT_PX, textPx(view.getContext(), designSp, scale));
    }

    /** Shared look for the big settings / action buttons across screens. */
    static void styleActionButton(Button button, boolean dark) {
        Context context = button.getContext();
        button.setAllCaps(false);
        text(button, 16f);
        button.setTextColor(ThemeColors.accent(dark));
        button.setBackground(ThemeColors.rounded(context, ThemeColors.incomingBubble(dark), 12));
        button.setMinHeight(dp(context, 54));
        int padH = dp(context, 16);
        int padV = dp(context, 14);
        button.setPadding(padH, padV, padH, padV);
        button.setGravity(Gravity.CENTER);
    }

    static void styleToolbarBack(Button back, boolean dark) {
        text(back, 30f);
        back.setTextColor(ThemeColors.accent(dark));
        back.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        back.setContentDescription("Volver");
        back.setMinWidth(dp(back.getContext(), 48));
        back.setMinHeight(dp(back.getContext(), 52));
    }
}
