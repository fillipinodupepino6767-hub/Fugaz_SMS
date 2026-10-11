package com.arena.autosms5min;

import android.app.Activity;
import android.app.Dialog;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * Interface size chosen by the person: a slider from 40 % to 200 % over the
 * app's own default. Every phone starts at 100 % (the app ignores the system
 * font size and display-size settings) and this picker is the only thing that
 * changes it. The dialog shows the exact percentage and a live preview while
 * the slider moves; applying recreates the screen underneath.
 */
final class UiScale {
    static final int MIN_PERCENT = 40;
    static final int MAX_PERCENT = 200;
    static final int DEFAULT_PERCENT = 100;

    private UiScale() { }

    /** Current size as a whole percentage (100 = default on every phone). */
    static int percent(android.content.Context context) {
        return Math.round(AppState.uiScale(context) * 100f);
    }

    static String label(android.content.Context context) {
        return percent(context) + " %";
    }

    /** Slider picker; afterPick runs when the person applies the new size. */
    static void showPicker(Activity activity, Runnable afterPick) {
        boolean dark = AppState.isDarkMode(activity);
        Dialog dialog = new Dialog(activity);
        LinearLayout card = ThemedDialog.card(activity, dialog, "Tamaño de la interfaz",
                "Todos los teléfonos empiezan con el mismo tamaño (100 %): la app ya no "
                        + "sigue el tamaño de letra ni el tamaño de pantalla del teléfono. "
                        + "Mueve el deslizador para elegir del 40 % al 200 % y mira la vista "
                        + "previa. Al aplicar, cambia toda la app al instante.");

        // Big percentage readout with fine -1 % / +1 % buttons around it.
        LinearLayout percentRow = new LinearLayout(activity);
        percentRow.setOrientation(LinearLayout.HORIZONTAL);
        percentRow.setGravity(Gravity.CENTER_VERTICAL);
        Button minus = stepButton(activity, dark, "\u22121 %");
        TextView percentView = new TextView(activity);
        Ui.text(percentView, 24f);
        percentView.setTypeface(percentView.getTypeface(), Typeface.BOLD);
        percentView.setTextColor(ThemeColors.primaryText(dark));
        percentView.setGravity(Gravity.CENTER);
        Button plus = stepButton(activity, dark, "+1 %");
        percentRow.addView(minus, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        percentRow.addView(percentView, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        percentRow.addView(plus, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        card.addView(percentRow);

        // Live preview of a message bubble + a button at the chosen size.
        LinearLayout preview = new LinearLayout(activity);
        preview.setOrientation(LinearLayout.VERTICAL);
        preview.setBackground(ThemeColors.rounded(activity, ThemeColors.sentBubble(dark), 12));
        TextView previewText = new TextView(activity);
        previewText.setText("Vista previa: así se verá un mensaje de ejemplo.");
        previewText.setTextColor(ThemeColors.primaryText(dark));
        preview.addView(previewText);
        LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        previewParams.setMargins(0, Ui.dp(activity, 10), 0, Ui.dp(activity, 4));
        card.addView(preview, previewParams);

        // The slider itself, with its limits labelled underneath.
        SeekBar slider = new SeekBar(activity);
        slider.setMax(MAX_PERCENT - MIN_PERCENT);
        ColorStateList accent = ColorStateList.valueOf(ThemeColors.accent(dark));
        slider.setThumbTintList(accent);
        slider.setProgressTintList(accent);
        LinearLayout.LayoutParams sliderParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        sliderParams.setMargins(0, Ui.dp(activity, 10), 0, 0);
        card.addView(slider, sliderParams);

        LinearLayout limits = new LinearLayout(activity);
        limits.setOrientation(LinearLayout.HORIZONTAL);
        TextView minLabel = new TextView(activity);
        minLabel.setText(MIN_PERCENT + " %");
        Ui.text(minLabel, 13f);
        minLabel.setTextColor(ThemeColors.secondaryText(dark));
        TextView maxLabel = new TextView(activity);
        maxLabel.setText(MAX_PERCENT + " %");
        Ui.text(maxLabel, 13f);
        maxLabel.setTextColor(ThemeColors.secondaryText(dark));
        TextView hint = new TextView(activity);
        hint.setText("desliza para elegir");
        Ui.text(hint, 13f);
        hint.setTextColor(ThemeColors.secondaryText(dark));
        hint.setGravity(Gravity.CENTER);
        limits.addView(minLabel);
        limits.addView(hint, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        limits.addView(maxLabel);
        card.addView(limits);

        // Everything above reads this one value; the slider, buttons and
        // preview all go through it so they cannot drift apart.
        final int[] chosen = {Math.round(AppState.uiScale(activity) * 100f)};
        Runnable render = () -> {
            int percent = chosen[0];
            percentView.setText(percent + " %" + (percent == DEFAULT_PERCENT
                    ? "\n(predeterminado)" : ""));
            preview.setPadding(Ui.dp(activity, 14, percent / 100f),
                    Ui.dp(activity, 10, percent / 100f),
                    Ui.dp(activity, 14, percent / 100f),
                    Ui.dp(activity, 10, percent / 100f));
            Ui.text(previewText, 15f, percent / 100f);
            if (slider.getProgress() != percent - MIN_PERCENT) {
                slider.setProgress(percent - MIN_PERCENT);
            }
        };
        SeekBar.OnSeekBarChangeListener listener = new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                chosen[0] = MIN_PERCENT + progress;
                render.run();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override public void onStopTrackingTouch(SeekBar seekBar) { }
        };
        slider.setOnSeekBarChangeListener(listener);
        minus.setOnClickListener(v -> {
            if (chosen[0] > MIN_PERCENT) {
                chosen[0]--;
                render.run();
            }
        });
        plus.setOnClickListener(v -> {
            if (chosen[0] < MAX_PERCENT) {
                chosen[0]++;
                render.run();
            }
        });
        render.run();

        card.addView(ThemedDialog.optionButton(activity,
                "Usar " + DEFAULT_PERCENT + " % (predeterminado)", false, v -> {
                    chosen[0] = DEFAULT_PERCENT;
                    render.run();
                }));

        LinearLayout buttons = ThemedDialog.buttonRow(activity);
        Button cancel = ThemedDialog.textButton(activity, "Cancelar", false);
        cancel.setOnClickListener(v -> dialog.dismiss());
        Button apply = ThemedDialog.textButton(activity, "Aplicar", true);
        apply.setOnClickListener(v -> {
            dialog.dismiss();
            AppState.setUiScale(activity, chosen[0] / 100f);
            if (afterPick != null) afterPick.run();
        });
        buttons.addView(cancel);
        buttons.addView(apply);
        card.addView(buttons);
        ThemedDialog.finish(activity, dialog, card);
    }

    /** Small -1 % / +1 % buttons flanking the percentage readout. */
    private static Button stepButton(Activity activity, boolean dark, String text) {
        Button button = new Button(activity);
        button.setAllCaps(false);
        button.setText(text);
        Ui.text(button, 15f);
        button.setTextColor(ThemeColors.accent(dark));
        button.setBackground(ThemeColors.rounded(activity, ThemeColors.background(dark), 10));
        button.setMinWidth(Ui.dp(activity, 64));
        button.setMinHeight(Ui.dp(activity, 48));
        return button;
    }
}
