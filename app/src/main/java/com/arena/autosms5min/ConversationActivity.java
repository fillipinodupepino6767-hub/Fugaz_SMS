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
    private boolean dark;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        address = getIntent().getStringExtra(EXTRA_ADDRESS);
        if (address == null || address.trim().isEmpty()) {
            finish();
            return;
        }
        dark = AppState.isDarkMode(this);
        ThemeColors.applySystemBars(this, dark);
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
        root.setBackgroundColor(ThemeColors.background(dark));

        TextView heading = new TextView(this);
        heading.setText(address + "\nLos entrantes se borran cinco minutos después de su llegada, salvo que pulses Conservar.");
        heading.setTextSize(18);
        heading.setTextColor(ThemeColors.primaryText(dark));
        root.addView(heading);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(ThemeColors.background(dark));
        messages = new LinearLayout(this);
        messages.setOrientation(LinearLayout.VERTICAL);
        messages.setPadding(0, dp(8), 0, dp(8));
        scroll.addView(messages);
        root.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout composer = new LinearLayout(this);
        composer.setOrientation(LinearLayout.HORIZONTAL);
        EditText text = new EditText(this);
        text.setHint("Escribe un SMS");
        text.setHintTextColor(ThemeColors.secondaryText(dark));
        text.setTextColor(ThemeColors.primaryText(dark));
        text.setMinLines(1);
        composer.addView(text, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        Button send = new Button(this);
        send.setText("ENVIAR");
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
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(12), dp(10), dp(12), dp(10));
            card.setBackgroundColor(item.type == SmsStore.TYPE_SENT
                    ? ThemeColors.sentBubble(dark) : ThemeColors.incomingBubble(dark));
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            cardParams.setMargins(0, dp(4), 0, dp(4));
            card.setLayoutParams(cardParams);

            TextView row = new TextView(this);
            String who = item.type == SmsStore.TYPE_SENT ? "Tú" : address;
            row.setText(who + "\n" + item.body + "\n" + format.format(item.date));
            row.setTextSize(16);
            row.setTextColor(ThemeColors.primaryText(dark));
            card.addView(row);

            // A pending registry entry means this particular incoming SMS still has a delete alarm.
            if (item.type == SmsStore.TYPE_INBOX && DeleteRegistry.dueAt(this, item.id) > 0L) {
                Button keep = new Button(this);
                keep.setText("CONSERVAR ESTE SMS");
                keep.setTextColor(ThemeColors.accent(dark));
                keep.setOnClickListener(v -> keepMessage(item.id));
                card.addView(keep);
            }
            messages.addView(card);
        }
    }

    private void keepMessage(long smsId) {
        DeleteScheduler.cancel(this, smsId);
        DeleteRegistry.remove(this, smsId);
        NotificationHelper.cancel(this, smsId);
        Toast.makeText(this, "SMS conservado. No se eliminará automáticamente.", Toast.LENGTH_LONG).show();
        populateMessages();
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
