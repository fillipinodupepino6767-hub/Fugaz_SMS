package com.arena.autosms5min;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.text.InputType;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Compose and send a new SMS: the inbox only receives by default, this screen
 * adds the missing half (write to any number and send). The sent copy is
 * written by SentSmsReceiver so it appears in the conversation, and sent
 * messages are never auto-deleted.
 */
public final class ComposeActivity extends Activity {
    private EditText addressBox;
    private EditText bodyBox;
    private boolean dark;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        dark = AppState.isDarkMode(this);
        ThemeColors.applySystemBars(this, dark);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(ThemeColors.background(dark));

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(dp(8), dp(10), dp(8), dp(8));
        Button back = new Button(this);
        back.setText("‹");
        Ui.styleToolbarBack(back, dark);
        back.setOnClickListener(v -> finish());
        toolbar.addView(back);
        TextView title = new TextView(this);
        title.setText("✏️ SMS nuevo");
        title.setTextColor(ThemeColors.primaryText(dark));
        Ui.text(title, 20f);
        toolbar.addView(title, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        root.addView(toolbar);

        ScrollView scroll = new ScrollView(this);
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(4), dp(14), dp(16));

        addressBox = field("Número de destino (ej. 8888-8888)", true);
        card.addView(addressBox);
        bodyBox = field("Mensaje", false);
        bodyBox.setMinLines(4);
        bodyBox.setMaxLines(8);
        bodyBox.setGravity(Gravity.TOP);
        card.addView(bodyBox);

        Button send = new Button(this);
        send.setText("ENVIAR SMS");
        Ui.styleActionButton(send, dark);
        send.setOnClickListener(v -> send());
        card.addView(send);

        TextView note = new TextView(this);
        note.setText("El SMS sale por la línea elegida en Configuración (SIMs); si no "
                + "elegiste ninguna, por la predeterminada del teléfono. Lo enviado queda "
                + "en la conversación y NO se borra solo.");
        note.setTextColor(ThemeColors.secondaryText(dark));
        Ui.text(note, 14f);
        note.setPadding(dp(4), dp(10), dp(4), 0);
        note.setLineSpacing(0f, 1.15f);
        card.addView(note);

        scroll.addView(card);
        root.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);
    }

    private EditText field(String hint, boolean singleLine) {
        EditText box = new EditText(this);
        box.setHint(hint);
        box.setSingleLine(singleLine);
        if (singleLine) box.setInputType(InputType.TYPE_CLASS_PHONE);
        Ui.text(box, 16f);
        box.setHintTextColor(ThemeColors.secondaryText(dark));
        box.setTextColor(ThemeColors.primaryText(dark));
        box.setBackground(ThemeColors.rounded(this, ThemeColors.incomingBubble(dark), 12));
        box.setPadding(dp(14), dp(12), dp(14), dp(12));
        box.setMinHeight(dp(48));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dp(6), 0, dp(6));
        box.setLayoutParams(params);
        return box;
    }

    private void send() {
        String address = addressBox.getText().toString().trim();
        String body = bodyBox.getText().toString().trim();
        if (address.isEmpty()) {
            Toast.makeText(this, "Escribe el número de destino.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!address.matches("\\+?[0-9\\- ]{3,20}")) {
            Toast.makeText(this, "Ese número no parece válido (usa solo dígitos, + o guiones).",
                    Toast.LENGTH_LONG).show();
            return;
        }
        if (body.isEmpty()) {
            Toast.makeText(this, "Escribe el mensaje.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (Blocklist.isBlocked(this, address)) {
            Toast.makeText(this, "Ese número está en tu lista de bloqueados.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        try {
            Intent callback = new Intent(this, SentSmsReceiver.class)
                    .putExtra(SentSmsReceiver.EXTRA_ADDRESS, address)
                    .putExtra(SentSmsReceiver.EXTRA_BODY, body);
            PendingIntent sent = PendingIntent.getBroadcast(this,
                    (int) System.currentTimeMillis(), callback,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            int selectedSubscriptionId = AppState.outgoingSubscriptionId(this);
            SmsManager manager = selectedSubscriptionId >= 0
                    ? SmsManager.getSmsManagerForSubscriptionId(selectedSubscriptionId)
                    : SmsManager.getDefault();
            manager.sendTextMessage(address, null, body, sent, null);
            Toast.makeText(this, "Enviando SMS…", Toast.LENGTH_SHORT).show();
            finish();
        } catch (SecurityException error) {
            Toast.makeText(this, "Falta el permiso para enviar SMS.", Toast.LENGTH_LONG).show();
        } catch (IllegalArgumentException error) {
            Toast.makeText(this, "Número o mensaje no válido.", Toast.LENGTH_LONG).show();
        }
    }

    private int dp(int value) {
        return Ui.dp(this, value);
    }
}
