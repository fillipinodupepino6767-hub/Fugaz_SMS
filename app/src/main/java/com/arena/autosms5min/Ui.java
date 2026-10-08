package com.arena.autosms5min;

import android.content.Context;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.Button;
import android.widget.TextView;

/**
 * Shared sizing so the UI stays readable on small phones and when the user
 * enlarges system text. Text uses SP (honours fontScale); layout uses DP.
 * On narrow screens the design SP is boosted a little so options do not look tiny.
 */
final class Ui {
    private Ui() { }

    static int dp(Context context, float value) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }

    /**
     * Design size in SP, with a mild boost on narrow phones. {@link TextView#setTextSize(float)}
     * already multiplies by the user fontScale; this only adjusts the base design size.
     */
    static float sp(Context context, float designSp) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        float widthDp = metrics.widthPixels / Math.max(0.5f, metrics.density);
        float boost = 1f;
        if (widthDp < 340f) boost = 1.18f;
        else if (widthDp < 380f) boost = 1.12f;
        else if (widthDp < 420f) boost = 1.06f;
        float fontScale = context.getResources().getConfiguration().fontScale;
        // User already enlarged text: keep layout usable.
        if (fontScale >= 1.35f) boost = Math.min(boost, 1.04f);
        else if (fontScale >= 1.15f) boost = Math.min(boost, 1.08f);
        return designSp * boost;
    }

    static void text(TextView view, float designSp) {
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp(view.getContext(), designSp));
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
