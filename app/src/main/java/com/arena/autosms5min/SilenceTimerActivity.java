package com.arena.autosms5min;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;

/**
 * Silence timer: vibrate/silence the phone now and get the sound back
 * automatically after the chosen minutes. Built for school hours.
 */
public final class SilenceTimerActivity extends Activity {
    private boolean dark;
    private TextView modeNow;
    private TextView status;
    private EditText minutesBox;
    private Button cancelButton;
    private Button dndButton;
    private TextView dndNote;
    private final Handler tickerHandler = new Handler(Looper.getMainLooper());
    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            refresh();
            tickerHandler.postDelayed(this, 1_000L);
        }
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        dark = AppState.isDarkMode(this);
        ThemeColors.applySystemBars(this, dark);
        buildUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (dark != AppState.isDarkMode(this)) {
            recreate();
            return;
        }
        refresh();
        tickerHandler.removeCallbacks(ticker);
        tickerHandler.postDelayed(ticker, 1_000L);
    }

    @Override
    protected void onPause() {
        tickerHandler.removeCallbacks(ticker);
        super.onPause();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(ThemeColors.background(dark));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(12), dp(16), dp(28));
        root.setBackgroundColor(ThemeColors.background(dark));
        scroll.addView(root);

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        Button back = new Button(this);
        back.setText("‹");
        back.setTextSize(32);
        back.setTextColor(ThemeColors.accent(dark));
        back.setContentDescription("Volver");
        back.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        back.setOnClickListener(v -> finish());
        toolbar.addView(back, new LinearLayout.LayoutParams(dp(48), dp(52)));
        TextView title = new TextView(this);
        title.setText("Temporizador de silencio");
        title.setTextSize(22);
        title.setTextColor(ThemeColors.primaryText(dark));
        toolbar.addView(title);
        root.addView(toolbar);

        root.addView(noteText("Escribe los minutos (o toca un acceso rápido), elige vibrar o "
                + "silenciar, y listo: el teléfono se silencia AHORA y el sonido vuelve solo "
                + "cuando se cumpla el tiempo. Así no se te olvida en el colegio."));

        modeNow = new TextView(this);
        modeNow.setTextSize(17);
        modeNow.setTextColor(ThemeColors.primaryText(dark));
        modeNow.setPadding(dp(4), dp(10), dp(4), dp(2));
        root.addView(modeNow);

        status = new TextView(this);
        status.setTextSize(17);
        status.setTextColor(ThemeColors.accent(dark));
        status.setPadding(dp(4), dp(2), dp(4), dp(10));
        root.addView(status);

        TextView presetsLabel = new TextView(this);
        presetsLabel.setText("ACCESOS RÁPIDOS (MINUTOS)");
        presetsLabel.setTextSize(13);
        presetsLabel.setTextColor(ThemeColors.accent(dark));
        presetsLabel.setPadding(dp(4), dp(6), 0, dp(5));
        root.addView(presetsLabel);

        LinearLayout presets = new LinearLayout(this);
        presets.setOrientation(LinearLayout.HORIZONTAL);
        presets.setGravity(Gravity.CENTER_VERTICAL);
        String[] quick = {"30", "60", "120", "240"};
        for (String value : quick) {
            Button preset = new Button(this);
            preset.setText(value.equals("60") ? "1 h" : value.equals("120") ? "2 h"
                    : value.equals("240") ? "4 h" : value + " min");
            preset.setAllCaps(false);
            preset.setTextColor(ThemeColors.accent(dark));
            preset.setBackground(ThemeColors.rounded(this, ThemeColors.incomingBubble(dark), 10));
            preset.setOnClickListener(v -> minutesBox.setText(value));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            params.setMargins(dp(2), 0, dp(2), 0);
            presets.addView(preset, params);
        }
        root.addView(presets);

        minutesBox = new EditText(this);
        minutesBox.setHint("Minutos: escribe de 1 a 720");
        minutesBox.setText("120");
        minutesBox.setSingleLine(true);
        minutesBox.setInputType(InputType.TYPE_CLASS_NUMBER);
        minutesBox.setHintTextColor(ThemeColors.secondaryText(dark));
        minutesBox.setTextColor(ThemeColors.primaryText(dark));
        minutesBox.setTextSize(20);
        minutesBox.setGravity(Gravity.CENTER);
        minutesBox.setBackground(ThemeColors.rounded(this, ThemeColors.incomingBubble(dark), 10));
        minutesBox.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams boxParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        boxParams.setMargins(0, dp(10), 0, dp(4));
        root.addView(minutesBox, boxParams);

        Button armVibrate = actionButton("📳 VIBRAR Y PROGRAMAR");
        armVibrate.setOnClickListener(v -> arm(true));
        root.addView(armVibrate);

        Button armSilent = actionButton("🔇 SILENCIAR Y PROGRAMAR");
        armSilent.setOnClickListener(v -> arm(false));
        root.addView(armSilent);

        cancelButton = actionButton("CANCELAR TEMPORIZADOR");
        cancelButton.setOnClickListener(v -> {
            RingerTimer.cancel(this);
            Toast.makeText(this, "Temporizador cancelado. El teléfono sigue como está.",
                    Toast.LENGTH_SHORT).show();
            refresh();
        });
        root.addView(cancelButton);

        dndNote = noteText("¿Silencias con No molestar? Sin el permiso de abajo, el temporizador "
                + "restaura el modo pero Android puede seguir callado. Tócalo una sola vez.");
        root.addView(dndNote);
        dndButton = actionButton("PERMITIR CAMBIAR NO MOLESTAR");
        dndButton.setOnClickListener(v -> SetupHelper.openDndSettings(this));
        root.addView(dndButton);

        setContentView(scroll);
    }

    private void arm(boolean vibrate) {
        long minutes;
        try {
            minutes = Long.parseLong(minutesBox.getText().toString().trim());
        } catch (NumberFormatException invalid) {
            Toast.makeText(this, "Escribe los minutos (1 a 720).", Toast.LENGTH_SHORT).show();
            return;
        }
        if (minutes < RingerTimer.MIN_MINUTES || minutes > RingerTimer.MAX_MINUTES) {
            Toast.makeText(this, "Escribe los minutos (1 a 720).", Toast.LENGTH_SHORT).show();
            return;
        }
        long restoreAt = RingerTimer.arm(this, minutes, vibrate);
        String when = DateFormat.getTimeInstance(DateFormat.SHORT).format(restoreAt);
        Toast.makeText(this, (vibrate ? "Vibración activada." : "Silencio activado.")
                + " El sonido vuelve a las " + when + ".", Toast.LENGTH_LONG).show();
        refresh();
    }

    private void refresh() {
        modeNow.setText("Estado ahora: " + RingerTimer.currentModeLabel(this));
        long restoreAt = RingerTimer.restoreAt(this);
        if (restoreAt > 0L) {
            String when = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                    .format(restoreAt);
            status.setText("⏳ Programado: el sonido vuelve " + when
                    + " (en " + CountdownFormatter.formatHistoryRemaining(restoreAt) + ").");
            cancelButton.setVisibility(View.VISIBLE);
        } else {
            status.setText("No hay temporizador activo.");
            cancelButton.setVisibility(View.GONE);
        }
        boolean needDnd = !SetupHelper.hasDndAccess(this);
        dndButton.setVisibility(needDnd ? View.VISIBLE : View.GONE);
        dndNote.setVisibility(needDnd ? View.VISIBLE : View.GONE);
    }

    private Button actionButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextColor(ThemeColors.accent(dark));
        button.setBackground(ThemeColors.rounded(this, ThemeColors.incomingBubble(dark), 10));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dp(3), 0, dp(3));
        button.setLayoutParams(params);
        return button;
    }

    private TextView noteText(String text) {
        TextView note = new TextView(this);
        note.setText(text);
        note.setTextSize(14);
        note.setTextColor(ThemeColors.secondaryText(dark));
        note.setPadding(dp(4), 0, dp(4), dp(6));
        return note;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + .5f);
    }
}
