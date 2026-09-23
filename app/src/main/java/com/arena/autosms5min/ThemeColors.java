package com.arena.autosms5min;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.view.View;

/** Centralized colors so the simple native views remain readable in both themes. */
final class ThemeColors {
    private ThemeColors() { }

    static int background(boolean dark) { return dark ? Color.rgb(18, 18, 18) : Color.WHITE; }
    static int primaryText(boolean dark) { return dark ? Color.rgb(242, 242, 242) : Color.rgb(32, 32, 32); }
    static int secondaryText(boolean dark) { return dark ? Color.rgb(205, 205, 205) : Color.rgb(80, 80, 80); }
    static int incomingBubble(boolean dark) { return dark ? Color.rgb(48, 48, 48) : Color.rgb(245, 245, 245); }
    static int sentBubble(boolean dark) { return dark ? Color.rgb(19, 67, 96) : Color.rgb(227, 242, 253); }
    static int accent(boolean dark) { return dark ? Color.rgb(144, 202, 249) : Color.rgb(21, 101, 192); }

    static void applySystemBars(Activity activity, boolean dark) {
        activity.getWindow().setStatusBarColor(background(dark));
        activity.getWindow().setNavigationBarColor(background(dark));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int flags = dark ? 0 : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            activity.getWindow().getDecorView().setSystemUiVisibility(flags);
        }
    }
}
