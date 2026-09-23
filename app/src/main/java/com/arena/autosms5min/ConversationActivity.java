package com.arena.autosms5min;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.telephony.SmsManager;
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
    private final Handler countdownHandler = new Handler(Looper.getMainLooper());
    private final Runnable countdownTicker = new Runnable() {
        @Override public void run() {
            populateMessages();
            countdownHandler.postDelayed(this, 1_000L);
        }
    };

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
        countdownHandler.removeCallbacks(countdownTicker);
        countdownHandler.postDelayed(countdownTicker, 1_000L);
    }

    @Override
    protected void onPause() {
        countdownHandler.removeCallbacks(countdownTicker);
        super.onPause();
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

        Button blockSender = new Button(this);
        boolean currentlyBlocked = Blocklist.isBlocked(this, address);
        blockSender.setText(currentlyBlocked ? "DESBLOQUEAR NÚMERO" : "BLOQUEAR NÚMERO");
        blockSender.setOnClickListener(v -> {
            if (Blocklist.isBlocked(this, address)) {
                confirmUnblockSender();
            } else {
                confirmBlockSender();
            }
        });
        root.addView(blockSender);

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
            MessageClassifier.Result classification = MessageClassifier.classify(item.body);
            row.setText(who + "\n[" + classification.display() + "]\n" + item.body + "\n" + format.format(item.date));
            row.setTextSize(16);
            row.setTextColor(ThemeColors.primaryText(dark));
            card.addView(row);

            Button share = new Button(this);
            share.setText("COMPARTIR");
            share.setTextColor(ThemeColors.accent(dark));
            share.setOnClickListener(v -> shareMessage(item));
            card.addView(share);

            if (item.type == SmsStore.TYPE_INBOX) {
                long dueAt = DeleteRegistry.dueAt(this, item.id);
                if (dueAt > 0L) {
                    TextView timer = new TextView(this);
                    timer.setText("Se elimina aproximadamente en " + CountdownFormatter.formatRemaining(dueAt));
                    timer.setTextColor(ThemeColors.accent(dark));
                    timer.setPadding(0, dp(8), 0, 0);
                    card.addView(timer);

                    Button keep = new Button(this);
                    keep.setText("CONSERVAR ESTE SMS");
                    keep.setTextColor(ThemeColors.accent(dark));
                    keep.setOnClickListener(v -> keepMessage(item.id));
                    card.addView(keep);
                } else {
                    // Messages that were conserved can later be removed deliberately.
                    Button delete = new Button(this);
                    delete.setText("ELIMINAR ESTE SMS");
                    delete.setTextColor(ThemeColors.accent(dark));
                    delete.setOnClickListener(v -> confirmDelete(item.id));
                    card.addView(delete);
                }
            }
            messages.addView(card);
        }
    }

    private void confirmBlockSender() {
        new AlertDialog.Builder(this)
                .setTitle("Bloquear " + address)
                .setMessage("Los próximos SMS de este remitente se descartarán inmediatamente, sin notificación y sin aparecer en la bandeja. Esto no bloquea llamadas.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Bloquear", (dialog, which) -> {
                    Blocklist.block(this, address);
                    Toast.makeText(this, address + " bloqueado para SMS.", Toast.LENGTH_LONG).show();
                    recreate();
                })
                .show();
    }

    private void confirmUnblockSender() {
        new AlertDialog.Builder(this)
                .setTitle("Desbloquear " + address)
                .setMessage("Los próximos SMS de este remitente volverán a recibirse normalmente.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Desbloquear", (dialog, which) -> {
                    Blocklist.unblock(this, address);
                    Toast.makeText(this, address + " desbloqueado.", Toast.LENGTH_SHORT).show();
                    recreate();
                })
                .show();
    }

    private void keepMessage(long smsId) {
        DeleteScheduler.cancel(this, smsId);
        DeleteRegistry.remove(this, smsId);
        NotificationHelper.cancel(this, smsId);
        Toast.makeText(this, "SMS conservado. No se eliminará automáticamente.", Toast.LENGTH_LONG).show();
        populateMessages();
    }

    private void confirmDelete(long smsId) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar SMS")
                .setMessage("¿Quieres eliminar permanentemente este SMS? Esta acción no se puede deshacer.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Eliminar", (dialog, which) -> deleteNow(smsId))
                .show();
    }

    private void deleteNow(long smsId) {
        SmsStore.SmsItem message = SmsStore.messageById(this, smsId);
        DeleteScheduler.cancel(this, smsId);
        DeleteRegistry.remove(this, smsId);
        int deleted = SmsStore.delete(this, smsId);
        NotificationHelper.cancel(this, smsId);
        if (deleted > 0 && message != null) {
            DeletionLog.add(this, message.address, message.body, "Eliminado manualmente");
        }
        Toast.makeText(this, deleted > 0 ? "SMS eliminado." : "No se pudo eliminar el SMS.", Toast.LENGTH_SHORT).show();
        populateMessages();
    }

    /** Shares through any installed app; the chosen app may use Wi-Fi or mobile data. */
    private void shareMessage(SmsStore.SmsItem item) {
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, "SMS de " + item.address + ":\n" + item.body);
        startActivity(Intent.createChooser(share, "Compartir mensaje con"));
    }

    private void sendSms(String body) {
        try {
            Intent callback = new Intent(this, SentSmsReceiver.class)
                    .putExtra(SentSmsReceiver.EXTRA_ADDRESS, address)
                    .putExtra(SentSmsReceiver.EXTRA_BODY, body);
            PendingIntent sent = PendingIntent.getBroadcast(this, (int) System.currentTimeMillis(), callback,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            int selectedSubscriptionId = AppState.outgoingSubscriptionId(this);
            SmsManager manager = selectedSubscriptionId >= 0
                    ? SmsManager.getSmsManagerForSubscriptionId(selectedSubscriptionId)
                    : SmsManager.getDefault();
            manager.sendTextMessage(address, null, body, sent, null);
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
