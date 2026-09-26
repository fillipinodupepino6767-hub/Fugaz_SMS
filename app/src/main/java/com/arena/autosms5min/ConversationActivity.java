package com.arena.autosms5min;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.telephony.SmsManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
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
    private ImageView photoView;
    private TextView heading;
    private TextView resultCount;
    private String query = "";
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
        buildUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateHeader();
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

        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        photoView = new ImageView(this);
        photoView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        LinearLayout.LayoutParams photoParams = new LinearLayout.LayoutParams(dp(56), dp(56));
        photoParams.setMargins(0, 0, dp(10), 0);
        headerRow.addView(photoView, photoParams);
        heading = new TextView(this);
        heading.setTextSize(18);
        heading.setTextColor(ThemeColors.primaryText(dark));
        headerRow.addView(heading, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        root.addView(headerRow);
        updateHeader();

        Button blockSender = secondaryButton(
                Blocklist.isBlocked(this, address) ? "DESBLOQUEAR NÚMERO" : "BLOQUEAR NÚMERO");
        blockSender.setOnClickListener(v -> {
            if (Blocklist.isBlocked(this, address)) {
                confirmUnblockSender();
            } else {
                confirmBlockSender();
            }
        });
        root.addView(blockSender);

        LinearLayout searchRow = new LinearLayout(this);
        searchRow.setOrientation(LinearLayout.HORIZONTAL);
        searchRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        searchParams.setMargins(0, dp(4), 0, 0);
        searchRow.setLayoutParams(searchParams);
        EditText searchBox = new EditText(this);
        searchBox.setHint("🔎 Buscar en esta conversación");
        searchBox.setSingleLine(true);
        searchBox.setHintTextColor(ThemeColors.secondaryText(dark));
        searchBox.setTextColor(ThemeColors.primaryText(dark));
        searchBox.setBackground(ThemeColors.rounded(this, ThemeColors.incomingBubble(dark), 10));
        searchBox.setPadding(dp(14), dp(10), dp(14), dp(10));
        searchBox.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override public void afterTextChanged(Editable s) {
                query = s.toString().trim();
                populateMessages();
            }
        });
        searchRow.addView(searchBox, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        Button clearSearch = new Button(this);
        clearSearch.setText("✕");
        clearSearch.setTextColor(ThemeColors.accent(dark));
        clearSearch.setContentDescription("Limpiar búsqueda");
        clearSearch.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        clearSearch.setOnClickListener(v -> searchBox.setText(""));
        searchRow.addView(clearSearch, new LinearLayout.LayoutParams(dp(48), dp(48)));
        root.addView(searchRow);

        resultCount = new TextView(this);
        resultCount.setTextSize(13);
        resultCount.setTextColor(ThemeColors.secondaryText(dark));
        resultCount.setPadding(dp(4), dp(2), dp(4), 0);
        resultCount.setVisibility(View.GONE);
        root.addView(resultCount);

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
        composer.setGravity(Gravity.CENTER_VERTICAL);
        EditText text = new EditText(this);
        text.setHint("Escribe un SMS");
        text.setHintTextColor(ThemeColors.secondaryText(dark));
        text.setTextColor(ThemeColors.primaryText(dark));
        text.setMinLines(1);
        composer.addView(text, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        Button send = new Button(this);
        send.setText("ENVIAR");
        send.setAllCaps(false);
        send.setTextColor(ThemeColors.accent(dark));
        send.setBackground(ThemeColors.rounded(this, ThemeColors.incomingBubble(dark), 10));
        send.setOnClickListener(v -> {
            String body = text.getText().toString();
            if (body.trim().isEmpty()) return;
            sendSms(body);
            text.setText("");
        });
        LinearLayout.LayoutParams sendParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        sendParams.setMargins(dp(8), 0, 0, 0);
        composer.addView(send, sendParams);
        root.addView(composer);
        setContentView(root);
    }

    /** Shows the contact name and photo when available; falls back to the raw number. */
    private void updateHeader() {
        String name = ContactNames.displayName(this, address);
        setTitle(name);
        heading.setText(ContactNames.twoLineLabel(this, address) + "\n" + retentionHint());
        Bitmap image = ContactNames.photo(this, address);
        if (image != null) {
            photoView.setImageBitmap(image);
        } else {
            photoView.setImageBitmap(ContactNames.letterBitmap(name, dp(56)));
        }
    }

    private String retentionHint() {
        long retention = AppState.retentionMillis(this);
        if (retention == AppState.NEVER) {
            return "El auto-borrado está en Nunca: los entrantes se conservan salvo que los elimines.";
        }
        return "Los entrantes se borran aprox. " + AppState.retentionLabel(this)
                + " después de su llegada, salvo que pulses Conservar.";
    }

    private Button secondaryButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextColor(ThemeColors.accent(dark));
        button.setBackground(ThemeColors.rounded(this, ThemeColors.incomingBubble(dark), 10));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dp(6), 0, dp(2));
        button.setLayoutParams(params);
        return button;
    }

    private void populateMessages() {
        if (messages == null) return;
        messages.removeAllViews();
        DateFormat format = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        List<SmsStore.SmsItem> items = SmsStore.messagesForAddress(this, address);
        String name = ContactNames.displayName(this, address);
        String lowered = query.toLowerCase();
        int shown = 0;
        for (SmsStore.SmsItem item : items) {
            if (!query.isEmpty() && !item.body.toLowerCase().contains(lowered)) continue;
            shown++;
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(12), dp(10), dp(12), dp(10));
            card.setBackground(ThemeColors.rounded(this, item.type == SmsStore.TYPE_SENT
                    ? ThemeColors.sentBubble(dark) : ThemeColors.incomingBubble(dark), 12));
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            cardParams.setMargins(0, dp(4), 0, dp(4));
            card.setLayoutParams(cardParams);

            TextView row = new TextView(this);
            String who = item.type == SmsStore.TYPE_SENT ? "Tú" : name;
            MessageClassifier.Result classification = MessageClassifier.classify(item.body);
            row.setText(who + "\n[" + classification.display() + "]\n" + item.body + "\n" + format.format(item.date));
            row.setTextSize(16);
            row.setTextColor(ThemeColors.primaryText(dark));
            card.addView(row);

            Button share = secondaryButton("COMPARTIR");
            share.setOnClickListener(v -> shareMessage(item));
            card.addView(share);

            if (item.type == SmsStore.TYPE_INBOX) {
                long dueAt = DeleteRegistry.dueAt(this, item.id);
                if (dueAt > 0L) {
                    TextView timer = new TextView(this);
                    timer.setText("⏳ Se elimina aproximadamente en "
                            + CountdownFormatter.formatVerbose(dueAt));
                    timer.setTextColor(ThemeColors.accent(dark));
                    timer.setPadding(0, dp(8), 0, 0);
                    card.addView(timer);

                    Button keep = secondaryButton("CONSERVAR ESTE SMS");
                    keep.setOnClickListener(v -> keepMessage(item.id));
                    card.addView(keep);
                } else {
                    // Messages that were conserved can later be removed deliberately.
                    Button delete = secondaryButton("ELIMINAR ESTE SMS");
                    delete.setOnClickListener(v -> confirmDelete(item.id));
                    card.addView(delete);
                }
            } else {
                // Sent messages are kept by design, but can be removed deliberately.
                Button deleteSent = secondaryButton("ELIMINAR ESTE SMS");
                deleteSent.setOnClickListener(v -> confirmDelete(item.id));
                card.addView(deleteSent);
            }
            messages.addView(card);
        }
        if (!query.isEmpty()) {
            resultCount.setVisibility(View.VISIBLE);
            resultCount.setText("🔎 " + shown + " de " + items.size() + " coinciden con «" + query + "»");
        } else {
            resultCount.setVisibility(View.GONE);
        }
    }

    private void confirmBlockSender() {
        ThemedDialog.confirm(this, "Bloquear " + ContactNames.displayName(this, address),
                "Los próximos SMS de " + ContactNames.singleLineLabel(this, address)
                        + " se descartarán inmediatamente, sin notificación y sin aparecer en la bandeja. Esto no bloquea llamadas.",
                "Cancelar", "Bloquear", () -> {
                    Blocklist.block(this, address);
                    Toast.makeText(this, ContactNames.displayName(this, address) + " bloqueado para SMS.",
                            Toast.LENGTH_LONG).show();
                    recreate();
                });
    }

    private void confirmUnblockSender() {
        ThemedDialog.confirm(this, "Desbloquear " + ContactNames.displayName(this, address),
                "Los próximos SMS de " + ContactNames.singleLineLabel(this, address)
                        + " volverán a recibirse normalmente.",
                "Cancelar", "Desbloquear", () -> {
                    Blocklist.unblock(this, address);
                    Toast.makeText(this, ContactNames.displayName(this, address) + " desbloqueado.",
                            Toast.LENGTH_SHORT).show();
                    recreate();
                });
    }

    private void keepMessage(long smsId) {
        DeleteScheduler.cancel(this, smsId);
        DeleteRegistry.remove(this, smsId);
        NotificationHelper.cancel(this, smsId);
        Toast.makeText(this, "SMS conservado. No se eliminará automáticamente.", Toast.LENGTH_LONG).show();
        populateMessages();
    }

    private void confirmDelete(long smsId) {
        ThemedDialog.confirm(this, "Eliminar SMS",
                "¿Quieres eliminar permanentemente este SMS? Esta acción no se puede deshacer.",
                "Cancelar", "Eliminar", () -> deleteNow(smsId));
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
        share.putExtra(Intent.EXTRA_TEXT,
                "SMS de " + ContactNames.singleLineLabel(this, item.address) + ":\n" + item.body);
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
