package com.arena.autosms5min;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/**
 * Per-line balance lookup. Android blocks apps from running USSD codes or
 * reading their popups, and the SIM Toolkit menus live inside the SIM chip,
 * so this screen opens the dialer with the carrier code ready, sends the
 * official SMS query from the chosen SIM, or opens the operator SIM menu.
 */
public final class BalanceActivity extends Activity {
    private static final int REQUEST_SIM_PERMISSIONS = 502;
    private static final String[] STK_PACKAGES = {"com.android.stk", "com.android.stk2"};
    private boolean dark;
    private TextView status;
    private Button allowButton;
    private LinearLayout cards;

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
        title.setText("Saldo de mis líneas");
        title.setTextSize(22);
        title.setTextColor(ThemeColors.primaryText(dark));
        toolbar.addView(title);
        root.addView(toolbar);

        root.addView(noteText("Fugaz SMS no puede leer tu saldo directamente: Android no permite "
                + "a las apps marcar códigos ni ver su resultado, y el menú de servicios vive "
                + "dentro del chip. Por eso, para cada línea te abrimos el marcador con el código "
                + "oficial listo (tocas llamar y el saldo aparece en una ventana del sistema), "
                + "enviamos el SMS de consulta desde esa SIM (la respuesta llega como un SMS "
                + "normal), o abrimos el menú de la SIM de tu operador."));

        status = new TextView(this);
        status.setTextColor(ThemeColors.secondaryText(dark));
        status.setPadding(0, dp(8), 0, dp(8));
        root.addView(status);

        allowButton = actionButton("PERMITIR VER MIS SIM");
        allowButton.setOnClickListener(v -> requestSimPermission());
        root.addView(allowButton);

        cards = new LinearLayout(this);
        cards.setOrientation(LinearLayout.VERTICAL);
        root.addView(cards);
        setContentView(scroll);
    }

    private void requestSimPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                requestPermissions(new String[]{Manifest.permission.READ_PHONE_STATE,
                        Manifest.permission.READ_PHONE_NUMBERS}, REQUEST_SIM_PERMISSIONS);
            } else {
                requestPermissions(new String[]{Manifest.permission.READ_PHONE_STATE},
                        REQUEST_SIM_PERMISSIONS);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        refresh();
    }

    private void refresh() {
        cards.removeAllViews();
        if (!SimInfo.hasPermission(this)) {
            status.setText("Permite el acceso a tus SIM para mostrar cada línea con su operador y sus opciones de saldo.");
            allowButton.setVisibility(android.view.View.VISIBLE);
            return;
        }
        allowButton.setVisibility(android.view.View.GONE);
        List<SimInfo.Card> active = SimInfo.activeCards(this);
        if (active.isEmpty()) {
            status.setText("No se detectaron SIM activas.");
            return;
        }
        status.setText("SIM activas: " + active.size()
                + ". Si tienes dos líneas, el marcador te dejará elegir con cuál llamar.");
        for (SimInfo.Card card : active) {
            addCard(card, BalanceCodes.forCarrier(card.carrier));
        }
    }

    private void addCard(final SimInfo.Card card, final BalanceCodes.Plan plan) {
        TextView header = new TextView(this);
        header.setText("SIM " + (card.slotIndex + 1) + " · " + plan.carrierLabel
                + "\nNúmero: " + card.number);
        header.setTextSize(17);
        header.setTextColor(ThemeColors.primaryText(dark));
        header.setPadding(dp(4), dp(14), dp(4), dp(2));
        cards.addView(header);

        TextView note = noteText(plan.note);
        cards.addView(note);

        if (plan.ussdCode != null) {
            Button dial = actionButton("📞 MARCAR " + plan.ussdCode + " (GRATIS, ABRE EL MARCADOR)");
            dial.setOnClickListener(v -> openDialer(plan.ussdCode));
            cards.addView(dial);
        }
        if (plan.smsNumber != null) {
            String cost = plan.smsFree ? "GRATIS SEGÚN EL OPERADOR" : "PUEDE TENER COSTO (UN SMS)";
            Button sms = actionButton("✉️ PEDIR SALDO POR SMS AL " + plan.smsNumber + " (" + cost + ")");
            sms.setOnClickListener(v -> confirmBalanceSms(card, plan));
            cards.addView(sms);
        }
        Button stk = actionButton("🧾 ABRIR MENÚ DE LA SIM (" + plan.carrierLabel.toUpperCase() + ")");
        stk.setOnClickListener(v -> openStkMenu());
        cards.addView(stk);
    }

    private void openDialer(String ussdCode) {
        try {
            Uri uri = Uri.parse("tel:" + ussdCode.replace("#", "%23"));
            startActivity(new Intent(Intent.ACTION_DIAL, uri));
            Toast.makeText(this, "Toca llamar: el saldo aparece en una ventana del sistema.",
                    Toast.LENGTH_LONG).show();
        } catch (Exception failed) {
            Toast.makeText(this, "No se pudo abrir el marcador.", Toast.LENGTH_LONG).show();
        }
    }

    private void confirmBalanceSms(final SimInfo.Card card, final BalanceCodes.Plan plan) {
        String cost = plan.smsFree
                ? "El operador lo publica como gratis."
                : "Esto envía un SMS real desde la SIM " + (card.slotIndex + 1) + " y puede tener costo.";
        ThemedDialog.confirm(this, "Enviar " + plan.smsText + " al " + plan.smsNumber,
                cost + " La respuesta del operador llega como un SMS normal a tu bandeja.",
                "Cancelar", "Enviar SMS", () -> sendBalanceSms(card, plan));
    }

    private void sendBalanceSms(SimInfo.Card card, BalanceCodes.Plan plan) {
        try {
            SmsManager manager = SmsManager.getSmsManagerForSubscriptionId(card.subscriptionId);
            manager.sendTextMessage(plan.smsNumber, null, plan.smsText, null, null);
            Toast.makeText(this, "SMS enviado desde la SIM " + (card.slotIndex + 1)
                    + ". La respuesta llega como un SMS normal.", Toast.LENGTH_LONG).show();
        } catch (SecurityException denied) {
            Toast.makeText(this, "Falta el permiso de SMS: usa Configuración automática.",
                    Toast.LENGTH_LONG).show();
        } catch (Exception failed) {
            Toast.makeText(this, "No se pudo enviar el SMS.", Toast.LENGTH_LONG).show();
        }
    }

    private void openStkMenu() {
        for (String pkg : STK_PACKAGES) {
            try {
                Intent launch = getPackageManager().getLaunchIntentForPackage(pkg);
                if (launch != null) {
                    startActivity(launch);
                    return;
                }
            } catch (Exception ignored) {
                // Try the next STK package.
            }
        }
        Toast.makeText(this, "No se encontró el menú SIM en este teléfono.",
                Toast.LENGTH_LONG).show();
    }

    private Button actionButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
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
