package com.arena.autosms5min;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Rounded dialogs that follow the app's dark/light theme. The platform alert
 * dialog kept a white style on some dark-mode phones (Motorola, Samsung),
 * so every app dialog goes through this helper. Content always scrolls, which
 * also fixes large-font phones where only the first options were visible.
 */
final class ThemedDialog {
    interface OnChoice { void onChoice(int which); }
    interface OnText { void onText(String value); }

    private ThemedDialog() { }

    static void message(Activity activity, String title, String message, String okLabel) {
        Dialog dialog = new Dialog(activity);
        LinearLayout card = card(activity, dialog, title, message);
        LinearLayout buttons = buttonRow(activity);
        Button ok = textButton(activity, okLabel == null ? "Entendido" : okLabel, true);
        ok.setOnClickListener(v -> dialog.dismiss());
        buttons.addView(ok);
        card.addView(buttons);
        finish(activity, dialog, card);
    }

    static void confirm(Activity activity, String title, String message,
                        String cancelLabel, String okLabel, Runnable onOk) {
        confirm(activity, title, message, cancelLabel, okLabel, null, onOk);
    }

    /** Confirm with a callback for the cancel button too (used by the help tour). */
    static void confirm(Activity activity, String title, String message,
                        String cancelLabel, String okLabel,
                        Runnable onCancel, Runnable onOk) {
        Dialog dialog = new Dialog(activity);
        LinearLayout card = card(activity, dialog, title, message);
        LinearLayout buttons = buttonRow(activity);
        Button cancel = textButton(activity, cancelLabel == null ? "Cancelar" : cancelLabel, false);
        cancel.setOnClickListener(v -> {
            dialog.dismiss();
            if (onCancel != null) onCancel.run();
        });
        Button ok = textButton(activity, okLabel == null ? "Aceptar" : okLabel, true);
        ok.setOnClickListener(v -> {
            dialog.dismiss();
            if (onOk != null) onOk.run();
        });
        buttons.addView(cancel);
        buttons.addView(ok);
        card.addView(buttons);
        finish(activity, dialog, card);
    }

    /** Windows currently on screen (helpers use it to avoid stacking dialogs). */
    private static int activeCount;
    static int activeCount() { return activeCount; }

    /**
     * One floating window of the guided help tour, pinned to the bottom so the
     * settings section behind stays visible. ◀ goes to the previous tip, ✕
     * closes the tour and the strong button shows the next tip (or finishes).
     */
    static void tourStep(Activity activity, String title, String message,
                         boolean canGoBack, String nextLabel,
                         Runnable onBack, Runnable onNext, Runnable onClosed) {
        Dialog dialog = new Dialog(activity);
        LinearLayout card = card(activity, dialog, title, message);
        LinearLayout buttons = buttonRow(activity);
        if (canGoBack) {
            Button back = textButton(activity, "\u2190 Atr\u00e1s", false);
            back.setOnClickListener(v -> dialog.dismiss());
            buttons.addView(back);
        }
        Button close = textButton(activity, "\u2715", false);
        close.setOnClickListener(v -> dialog.dismiss());
        buttons.addView(close);
        Button next = textButton(activity, nextLabel, true);
        next.setOnClickListener(v -> {
            dialog.dismiss();
            if (onNext != null) onNext.run();
        });
        buttons.addView(next);
        card.addView(buttons);
        // onClosed fires on every dismissal (✕, ◀, Siguiente, back gesture), so
        // the host can stop its section highlight before the next step starts.
        finish(activity, dialog, card, true, onClosed);
    }

    /** Scrollable single-choice sheet in the style of the history retention picker. */
    static void singleChoice(Activity activity, String title, String explanation,
                             String[] labels, int checked, OnChoice onChoice) {
        boolean dark = AppState.isDarkMode(activity);
        Dialog dialog = new Dialog(activity);
        LinearLayout card = card(activity, dialog, title, explanation);
        for (int i = 0; i < labels.length; i++) {
            final int which = i;
            boolean selected = which == checked;
            Button option = new Button(activity);
            option.setAllCaps(false);
            option.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            option.setText((selected ? "\u2713  " : "\u25CB  ") + labels[i]
                    + (selected ? "  \u00B7 actual" : ""));
            Ui.text(option, 16f);
            option.setMinHeight(dp(activity, 48));
            option.setTextColor(ThemeColors.accent(dark));
            option.setBackground(ThemeColors.rounded(activity,
                    selected ? ThemeColors.sentBubble(dark) : ThemeColors.background(dark), 10));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, dp(activity, 3), 0, dp(activity, 3));
            card.addView(option, params);
            option.setOnClickListener(v -> {
                dialog.dismiss();
                if (onChoice != null) onChoice.onChoice(which);
            });
        }
        LinearLayout buttons = buttonRow(activity);
        Button cancel = textButton(activity, "Cancelar", false);
        cancel.setOnClickListener(v -> dialog.dismiss());
        buttons.addView(cancel);
        card.addView(buttons);
        finish(activity, dialog, card);
    }

    /** Scrollable list of tappable full-width rows (e.g. blocked numbers). */
    static void items(Activity activity, String title, String message, String[] items,
                      String closeLabel, OnChoice onItem) {
        boolean dark = AppState.isDarkMode(activity);
        Dialog dialog = new Dialog(activity);
        LinearLayout card = card(activity, dialog, title, message);
        for (int i = 0; i < items.length; i++) {
            final int which = i;
            Button row = new Button(activity);
            row.setAllCaps(false);
            row.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            row.setText(items[i]);
            Ui.text(row, 16f);
            row.setMinHeight(dp(activity, 48));
            row.setTextColor(ThemeColors.primaryText(dark));
            row.setBackground(ThemeColors.rounded(activity, ThemeColors.background(dark), 10));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, dp(activity, 3), 0, dp(activity, 3));
            card.addView(row, params);
            row.setOnClickListener(v -> {
                dialog.dismiss();
                if (onItem != null) onItem.onChoice(which);
            });
        }
        LinearLayout buttons = buttonRow(activity);
        Button close = textButton(activity, closeLabel == null ? "Cerrar" : closeLabel, true);
        close.setOnClickListener(v -> dialog.dismiss());
        buttons.addView(close);
        card.addView(buttons);
        finish(activity, dialog, card);
    }

    /** Text input sheet with optional neutral action (used for the SIM display number). */
    static void input(Activity activity, String title, String message, String initial, int inputType,
                      String neutralLabel, String cancelLabel, String okLabel,
                      Runnable onNeutral, OnText onSave) {
        input(activity, title, message, initial, inputType, neutralLabel, cancelLabel, okLabel,
                onNeutral, onSave, null);
    }

    /** input() with a callback when the user cancels without saving. */
    static void input(Activity activity, String title, String message, String initial, int inputType,
                      String neutralLabel, String cancelLabel, String okLabel,
                      Runnable onNeutral, OnText onSave, Runnable onCancel) {
        boolean dark = AppState.isDarkMode(activity);
        Dialog dialog = new Dialog(activity);
        LinearLayout card = card(activity, dialog, title, message);
        final EditText field = new EditText(activity);
        field.setInputType(inputType);
        if (initial != null) field.setText(initial);
        field.setTextColor(ThemeColors.primaryText(dark));
        field.setHintTextColor(ThemeColors.secondaryText(dark));
        field.setBackground(ThemeColors.rounded(activity, ThemeColors.background(dark), 10));
        field.setPadding(dp(activity, 14), dp(activity, 12), dp(activity, 14), dp(activity, 12));
        LinearLayout.LayoutParams fieldParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        fieldParams.setMargins(0, dp(activity, 2), 0, dp(activity, 10));
        card.addView(field, fieldParams);
        // Vertical full-width buttons: safe on narrow screens and very large fonts.
        if (neutralLabel != null) {
            card.addView(optionButton(activity, neutralLabel, false, v -> {
                dialog.dismiss();
                if (onNeutral != null) onNeutral.run();
            }));
        }
        card.addView(optionButton(activity, cancelLabel == null ? "Cancelar" : cancelLabel, false,
                v -> {
                    dialog.dismiss();
                    if (onCancel != null) onCancel.run();
                }));
        card.addView(optionButton(activity, okLabel == null ? "Guardar" : okLabel, true, v -> {
            dialog.dismiss();
            if (onSave != null) onSave.onText(field.getText().toString());
        }));
        finish(activity, dialog, card);
    }

    private static Button optionButton(Activity activity, String text, boolean selected,
                                       android.view.View.OnClickListener listener) {
        boolean dark = AppState.isDarkMode(activity);
        Button button = new Button(activity);
        button.setAllCaps(false);
        button.setGravity(Gravity.CENTER);
        button.setText(text);
        Ui.text(button, 16f);
        button.setMinHeight(dp(activity, 48));
        button.setTextColor(ThemeColors.accent(dark));
        button.setBackground(ThemeColors.rounded(activity,
                selected ? ThemeColors.sentBubble(dark) : ThemeColors.background(dark), 10));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dp(activity, 3), 0, dp(activity, 3));
        button.setLayoutParams(params);
        button.setOnClickListener(listener);
        return button;
    }

    private static LinearLayout card(Activity activity, Dialog dialog, String title, String message) {
        boolean dark = AppState.isDarkMode(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(activity, 22), dp(activity, 22), dp(activity, 22), dp(activity, 14));
        card.setBackground(ThemeColors.rounded(activity, ThemeColors.incomingBubble(dark), 18));
        if (title != null) {
            TextView heading = new TextView(activity);
            heading.setText(title);
            Ui.text(heading, 22f);
            heading.setTextColor(ThemeColors.primaryText(dark));
            card.addView(heading);
        }
        if (message != null && !message.isEmpty()) {
            TextView body = new TextView(activity);
            body.setText(message);
            Ui.text(body, 16f);
            body.setTextColor(ThemeColors.secondaryText(dark));
            body.setPadding(0, dp(activity, 10), 0, dp(activity, 12));
            card.addView(body);
        }
        return card;
    }

    private static LinearLayout buttonRow(Activity activity) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.END;
        params.topMargin = dp(activity, 6);
        row.setLayoutParams(params);
        return row;
    }

    private static Button textButton(Activity activity, String text, boolean strong) {
        boolean dark = AppState.isDarkMode(activity);
        Button button = new Button(activity);
        button.setText(text);
        button.setAllCaps(false);
        Ui.text(button, 16f);
        button.setMinHeight(dp(activity, 48));
        button.setTextColor(ThemeColors.accent(dark));
        button.setBackgroundColor(Color.TRANSPARENT);
        if (strong) button.setTypeface(button.getTypeface(), Typeface.BOLD);
        return button;
    }

    private static void finish(Activity activity, Dialog dialog, LinearLayout card) {
        finish(activity, dialog, card, false, null);
    }

    private static void finish(Activity activity, Dialog dialog, LinearLayout card,
                               boolean pinToBottom, Runnable onClosed) {
        ScrollView scroll = new ScrollView(activity);
        scroll.setBackgroundColor(Color.TRANSPARENT);
        scroll.addView(card);
        dialog.setContentView(scroll);
        activeCount++;
        dialog.setOnDismissListener(d -> {
            activeCount--;
            if (onClosed != null) onClosed.run();
        });
        dialog.show();
        if (dialog.getWindow() != null) {
            Window window = dialog.getWindow();
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(
                    activity.getResources().getDisplayMetrics().widthPixels - dp(activity, 36),
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            if (pinToBottom) {
                WindowManager.LayoutParams attrs = window.getAttributes();
                attrs.gravity = Gravity.BOTTOM;
                attrs.y = dp(activity, 14);
                window.setAttributes(attrs);
                window.setDimAmount(0.2f);
            }
        }
    }

    private static int dp(Activity activity, int value) {
        return Ui.dp(activity, value);
    }
}
