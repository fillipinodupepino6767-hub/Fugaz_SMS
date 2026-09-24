package com.arena.autosms5min;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/** Lets the user inspect active SIMs and select the SIM used for outgoing text SMS. */
public final class SimSettingsActivity extends Activity {
    private static final int REQUEST_SIM_PERMISSIONS = 501;
    // Public SDKs do not expose this action as a Settings constant on every compile SDK.
    // The literal keeps the direct Motorola/Android SIM screen optional and safely falls back.
    private static final String ACTION_SIM_CARD_SETTINGS = "android.settings.SIM_CARD_SETTINGS";
    private boolean dark;
    private LinearLayout cards;
    private TextView status;

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
        refresh();
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
        back.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        back.setOnClickListener(v -> finish());
        toolbar.addView(back, new LinearLayout.LayoutParams(dp(48), dp(52)));
        TextView title = new TextView(this);
        title.setText("SIM y envío");
        title.setTextSize(24);
        title.setTextColor(ThemeColors.primaryText(dark));
        toolbar.addView(title);
        root.addView(toolbar);

        status = new TextView(this);
        status.setTextColor(ThemeColors.secondaryText(dark));
        status.setPadding(0, dp(8), 0, dp(8));
        root.addView(status);

        Button allow = actionButton("VER SIM ACTIVAS Y NÚMEROS");
        allow.setOnClickListener(v -> requestPermissionsIfNeeded());
        root.addView(allow);

        Button systemSimSettings = actionButton("ABRIR AJUSTES DEL SISTEMA: SIM Y NÚMEROS");
        systemSimSettings.setOnClickListener(v -> openSystemSimSettings());
        root.addView(systemSimSettings);

        Button automatic = actionButton("USAR SIM PREDETERMINADA DEL TELÉFONO");
        automatic.setOnClickListener(v -> {
            AppState.setOutgoingSubscriptionId(this, -1);
            Toast.makeText(this, "Se usará la SIM predeterminada del teléfono.", Toast.LENGTH_SHORT).show();
            refresh();
        });
        root.addView(automatic);

        cards = new LinearLayout(this);
        cards.setOrientation(LinearLayout.VERTICAL);
        root.addView(cards);
        setContentView(scroll);
    }

    private void requestPermissionsIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !SimInfo.hasPermission(this)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                requestPermissions(new String[]{Manifest.permission.READ_PHONE_STATE,
                        Manifest.permission.READ_PHONE_NUMBERS}, REQUEST_SIM_PERMISSIONS);
            } else {
                requestPermissions(new String[]{Manifest.permission.READ_PHONE_STATE}, REQUEST_SIM_PERMISSIONS);
            }
        }
    }

    private void refresh() {
        cards.removeAllViews();
        if (!SimInfo.hasPermission(this)) {
            status.setText("Permite acceso al estado y número de teléfono para revisar las SIM activas. El número puede no estar almacenado por el operador.");
            return;
        }
        int slots = SimInfo.phoneSlotCount(this);
        List<SimInfo.Card> active = SimInfo.activeCards(this);
        status.setText("Ranuras detectadas: " + slots + " · SIM activas: " + active.size()
                + "\nToca una SIM para usarla al enviar SMS. Si el operador no expone su número, abre los ajustes del sistema o guárdalo solo para verlo en esta app.");
        if (active.isEmpty()) {
            TextView none = new TextView(this);
            none.setText("No se detectaron SIM activas.");
            none.setTextColor(ThemeColors.secondaryText(dark));
            cards.addView(none);
            return;
        }
        int selected = AppState.outgoingSubscriptionId(this);
        for (SimInfo.Card card : active) {
            Button sim = actionButton(card.label() + (selected == card.subscriptionId ? "\n✓ Seleccionada para enviar" : ""));
            sim.setOnClickListener(v -> {
                AppState.setOutgoingSubscriptionId(this, card.subscriptionId);
                Toast.makeText(this, "SIM " + (card.slotIndex + 1) + " seleccionada para enviar SMS.", Toast.LENGTH_SHORT).show();
                refresh();
            });
            cards.addView(sim);
            Button editNumber = actionButton("Editar número mostrado para SIM " + (card.slotIndex + 1));
            editNumber.setTextColor(ThemeColors.secondaryText(dark));
            editNumber.setOnClickListener(v -> editDisplayedNumber(card));
            cards.addView(editNumber);
        }
    }

    private void openSystemSimSettings() {
        try {
            startActivity(new Intent(ACTION_SIM_CARD_SETTINGS));
        } catch (ActivityNotFoundException unavailable) {
            try {
                startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
            } catch (ActivityNotFoundException ignored) {
                Toast.makeText(this, "No se encontraron los ajustes de SIM del sistema.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void editDisplayedNumber(SimInfo.Card card) {
        String saved = AppState.manualSimNumber(this, card.subscriptionId);
        String initial = saved.isEmpty() && card.numberReportedBySystem ? card.number : saved;
        ThemedDialog.input(this, "Número mostrado · SIM " + (card.slotIndex + 1),
                "Este dato solo se guarda para mostrarlo en SMS 5 minutos. No modifica la SIM ni el número de tu operador.",
                initial, android.text.InputType.TYPE_CLASS_PHONE,
                "Borrar guardado", "Cancelar", "Guardar",
                () -> {
                    AppState.setManualSimNumber(this, card.subscriptionId, "");
                    refresh();
                },
                value -> {
                    AppState.setManualSimNumber(this, card.subscriptionId, value);
                    refresh();
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        refresh();
    }

    private Button actionButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        button.setTextColor(ThemeColors.primaryText(dark));
        button.setBackground(ThemeColors.rounded(this, ThemeColors.incomingBubble(dark), 10));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dp(4), 0, dp(4));
        button.setLayoutParams(params);
        return button;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + .5f);
    }
}
