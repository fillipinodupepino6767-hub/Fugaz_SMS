package com.arena.autosms5min;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.List;

public final class ConversationActivity extends Activity {
    public static final String EXTRA_ADDRESS = "address";
    private String address;
    private LinearLayout messages;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        address = getIntent().getStringExtra(EXTRA_ADDRESS);
        if (address == null || address.trim().isEmpty()) {
            finish();
            return;
        }
        setTitle(address);
        buildUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        populateMessages();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(12), dp(12), dp(12), dp(12));

        TextView heading = new TextView(this);
        heading.setText(address + "\nLos entrantes desaparecen cinco minutos después de su llegada.");
        heading.setTextSize(18);
        root.addView(heading);

        ScrollView scroll = new ScrollView(this);
        messages = new LinearLayout(this);
        messages.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(messages);
        root.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout composer = new LinearLayout(this);
        composer.setOrientation(LinearLayout.HORIZONTAL);
        EditText text = new EditText(this);
        text.setHint("Escribe un SMS");
        text.setMinLines(1);
        composer.addView(text, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        Button send = new Button(this);
        send.setText("Enviar");
        send.setOnClickListener(v -> {
            String body = text.getText().toString();
            if (body.trim().isEmpty()) return;
            sendSms(body);
            text.setText("");
        });
        composer.addView(send);
        root.addView(composer);
        setContentView(root);
    }

    private void populateMessages() {
        if (messages == null) return;
        messages.removeAllViews();
        DateFormat format = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        List<SmsStore.SmsItem> items = SmsStore.messagesForAddress(this, address);
        for (SmsStore.SmsItem item : items) {
            TextView row = new TextView(this);
            String who = item.type == SmsStore.TYPE_SENT ? "Tú" : address;
            row.setText(who + "\n" + item.body + "\n" + format.format(item.date));
            row.setTextSize(16);
            row.setPadding(dp(12), dp(10), dp(12), dp(10));
            row.setBackgroundColor(item.type == SmsStore.TYPE_SENT ? 0xFFE3F2FD : 0xFFF5F5F5);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, dp(4), 0, dp(4));
            row.setLayoutParams(params);
            messages.addView(row);
        }
    }

    private void sendSms(String body) {
        try {
            Intent callback = new Intent(this, SentSmsReceiver.class)
                    .putExtra(SentSmsReceiver.EXTRA_ADDRESS, address)
                    .putExtra(SentSmsReceiver.EXTRA_BODY, body);
            PendingIntent sent = PendingIntent.getBroadcast(this, (int) System.currentTimeMillis(), callback,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            SmsManager.getDefault().sendTextMessage(address, null, body, sent, null);
            Toast.makeText(this, "Enviando SMS…", Toast.LENGTH_SHORT).show();
        } catch (SecurityException error) {
            Toast.makeText(this, "Falta el permiso para enviar SMS.", Toast.LENGTH_LONG).show();
        } catch (IllegalArgumentException error) {
            Toast.makeText(this, "Número o mensaje no válido.", Toast.LENGTH_LONG).show();
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + .5f);
    }
}
