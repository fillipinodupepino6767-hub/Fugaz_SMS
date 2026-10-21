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
    private TextView headerHint;
    private TextView resultCount;
    private Button blockButton;
    private String query = "";
    private boolean dark;
    private String renderedSignature = "";
    private final java.util.List<Runnable> timerUpdates = new java.util.ArrayList<>();
    private final Handler countdownHandler = new Handler(Looper.getMainLooper());
    private final Runnable countdownTicker = new Runnable() {
        @Override public void run() {
            SmsStore.markThreadRead(ConversationActivity.this, address);
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
        if (AppLock.enabled(this)) {
            // Cold open (notification tap): cover + PIN; cancel finishes.
            AppLock.ask(this, AppLock.installCover(this), null);
        }
        SmsStore.markThreadRead(this, address);
        buildUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        SmsStore.markThreadRead(this, address);
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
        Button back = new Button(this);
        back.setText("‹");
        Ui.styleToolbarBack(back, dark);
        back.setOnClickListener(v -> finish());
        headerRow.addView(back, new LinearLayout.LayoutParams(dp(52), dp(56)));
        photoView = new ImageView(this);
        photoView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        LinearLayout.LayoutParams photoParams = new LinearLayout.LayoutParams(dp(56), dp(56));
        photoParams.setMargins(0, 0, dp(10), 0);
        headerRow.addView(photoView, photoParams);
        heading = new TextView(this);
        Ui.text(heading, 18f);
        heading.setTextColor(ThemeColors.primaryText(dark));
        LinearLayout headerText = new LinearLayout(this);
        headerText.setOrientation(LinearLayout.VERTICAL);
        headerText.addView(heading);
        headerHint = new TextView(this);
        Ui.text(headerHint, 12f);
        headerHint.setTextColor(ThemeColors.secondaryText(dark));
        headerHint.setPadding(0, dp(4), 0, 0);
        headerText.addView(headerHint);
        headerRow.addView(headerText, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        root.addView(headerRow);
        updateHeader();

        blockButton = secondaryButton(
                Blocklist.isBlocked(this, address) ? "DESBLOQUEAR NÚMERO" : "BLOQUEAR NÚMERO");
        blockButton.setOnClickListener(v -> {
            if (Blocklist.isBlocked(this, address)) {
                confirmUnblockSender();
            } else {
                confirmBlockSender();
            }
        });
        root.addView(blockButton);

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
        Ui.text(searchBox, 16f);
        searchBox.setMinHeight(dp(48));
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
        Ui.text(clearSearch, 16f);
        clearSearch.setTextColor(ThemeColors.accent(dark));
        clearSearch.setContentDescription("Limpiar búsqueda");
        clearSearch.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        clearSearch.setOnClickListener(v -> searchBox.setText(""));
        searchRow.addView(clearSearch, new LinearLayout.LayoutParams(dp(48), dp(48)));
        root.addView(searchRow);

        resultCount = new TextView(this);
        Ui.text(resultCount, 13f);
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
        Ui.text(text, 16f);
        text.setHintTextColor(ThemeColors.secondaryText(dark));
        text.setTextColor(ThemeColors.primaryText(dark));
        text.setMinLines(1);
        composer.addView(text, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        Button send = new Button(this);
        send.setText("ENVIAR");
        Ui.text(send, 15f);
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
        heading.setText(ContactNames.twoLineLabel(this, address));
        headerHint.setText(retentionHint());
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
            return "Nuevos SMS sin borrado automático";
        }
        return "Borrado de nuevos SMS: " + AppState.retentionLabel(this)
                + " · según categoría. Los conservados no se borran solos.";
    }

    private Button secondaryButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        Ui.text(button, 15f);
        button.setAllCaps(false);
        button.setTextColor(ThemeColors.accent(dark));
        button.setBackground(ThemeColors.rounded(this, ThemeColors.incomingBubble(dark), 10));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dp(6), 0, dp(2));
        button.setLayoutParams(params);
        return button;
    }

    private Button messageAction(String label) {
        Button button = new Button(this);
        Ui.text(button, 13f);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextColor(ThemeColors.accent(dark));
        button.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setMinHeight(dp(48));
        button.setPadding(dp(6), dp(8), dp(6), dp(8));
        button.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        return button;
    }

    private void populateMessages() {
        if (messages == null) return;
        DateFormat format = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        List<SmsStore.SmsItem> items = SmsStore.messagesForAddress(this, address);
        String name = ContactNames.displayName(this, address);
        String lowered = query.toLowerCase();
        // Highlight preferences and word list are part of the signature: changing
        // them in Settings must redraw the bubbles, not leave them stale.
        List<String> highlightWords = HighlightStore.wordsEnabled(this)
                ? HighlightStore.words(this) : java.util.Collections.<String>emptyList();
        StringBuilder signature = new StringBuilder(query)
                .append('#').append(HighlightStore.style(this))
                .append('#').append(HighlightStore.datesEnabled(this))
                .append('#').append(highlightWords);
        for (SmsStore.SmsItem item : items) {
            signature.append('|').append(item.id).append(':').append(item.type).append(':')
                    .append(item.date).append(':').append(item.body.length()).append(':').append(item.body)
                    .append(':').append(MessageClassifier.classify(this, item.body).display())
                    .append(':').append(DeleteRegistry.dueAt(this, item.id));
        }
        if (signature.toString().equals(renderedSignature)) {
            for (Runnable update : timerUpdates) update.run();
            return;
        }
        renderedSignature = signature.toString();
        timerUpdates.clear();
        messages.removeAllViews();
        int shown = 0;
        for (SmsStore.SmsItem item : items) {
            if (!query.isEmpty() && !item.body.toLowerCase().contains(lowered)) continue;
            shown++;
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(16), dp(16), dp(16), dp(12));
            card.setBackground(ThemeColors.rounded(this, item.type == SmsStore.TYPE_SENT
                    ? ThemeColors.sentBubble(dark) : ThemeColors.incomingBubble(dark), 12));
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            cardParams.setMargins(item.type == SmsStore.TYPE_SENT ? dp(22) : 0, dp(6),
                    item.type == SmsStore.TYPE_SENT ? 0 : dp(22), dp(6));
            card.setLayoutParams(cardParams);

            MessageViews row = new MessageViews(this, dark);
            String who = item.type == SmsStore.TYPE_SENT ? "Tú" : name;
            MessageClassifier.Result classification = MessageClassifier.classify(this, item.body);
            long scheduled = item.type == SmsStore.TYPE_INBOX ? DeleteRegistry.dueAt(this, item.id) : -1L;
            row.bind(who, classification.display(), item.body, format.format(item.date),
                    scheduled > 0 ? "⏳ Se elimina en " + CountdownFormatter.formatVerbose(scheduled)
                            : item.type == SmsStore.TYPE_SENT ? "Enviado" : "✓ Conservado · sin borrado automático",
                    highlightWords,
                    item.type == SmsStore.TYPE_INBOX ? DateInText.find(item.body) : null);
            card.addView(row);
            if (scheduled > 0) {
                timerUpdates.add(() -> row.updateStatus("⏳ Se elimina en "
                        + CountdownFormatter.formatVerbose(scheduled)));
            }

            LinearLayout actions = new LinearLayout(this);
            actions.setGravity(Gravity.END);
            LinearLayout.LayoutParams actionRowParams = new LinearLayout.LayoutParams(-1, -2);
            actionRowParams.topMargin = dp(10);
            card.addView(actions, actionRowParams);
            Button share = messageAction("Compartir");
            share.setOnClickListener(v -> shareMessage(item));
            actions.addView(share);

            if (item.type == SmsStore.TYPE_INBOX) {
                long dueAt = DeleteRegistry.dueAt(this, item.id);
                if (dueAt > 0L) {
                    Button keep = messageAction("Conservar");
                    keep.setOnClickListener(v -> keepMessage(item.id));
                    actions.addView(keep);
                } else {
                    // Messages that were conserved can later be removed deliberately.
                    Button delete = messageAction("Eliminar");
                    delete.setOnClickListener(v -> confirmDelete(item.id));
                    actions.addView(delete);
                }
            } else {
                // Sent messages are kept by design, but can be removed deliberately.
                Button deleteSent = messageAction("Eliminar");
                deleteSent.setOnClickListener(v -> confirmDelete(item.id));
                actions.addView(deleteSent);
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
                    blockButton.setText("DESBLOQUEAR NÚMERO");
                    Toast.makeText(this, ContactNames.displayName(this, address) + " bloqueado para SMS.",
                            Toast.LENGTH_LONG).show();
                    askDeleteExistingAfterBlock();
                });
    }

    /** After blocking, offer to remove the messages already received in one step. */
    private void askDeleteExistingAfterBlock() {
        List<SmsStore.SmsItem> existing = SmsStore.messagesForAddress(this, address);
        if (existing.isEmpty()) return;
        ThemedDialog.confirm(this, "Eliminar mensajes existentes",
                "Bloqueado. ¿Eliminar también los " + existing.size() + " SMS que ya tienes de "
                        + ContactNames.singleLineLabel(this, address)
                        + "? Se guardarán en Eliminados recientemente por si los necesitas.",
                "Conservarlos", "Eliminar", () -> deleteAllFromAddress());
    }

    private void deleteAllFromAddress() {
        List<SmsStore.SmsItem> existing = SmsStore.messagesForAddress(this, address);
        int deleted = 0;
        for (SmsStore.SmsItem item : existing) {
            DeleteScheduler.cancel(this, item.id);
            DeleteRegistry.remove(this, item.id);
            ArchiveStore.unarchive(this, item.id);
            if (SmsStore.delete(this, item.id) > 0) {
                deleted++;
                DeletionLog.add(this, item.address, item.body, "Eliminado manualmente");
                NotificationHelper.cancel(this, item.id);
            }
        }
        if (SmsStore.messagesForAddress(this, address).isEmpty()) {
            Toast.makeText(this, deleted + " SMS eliminados. Conversación cerrada.",
                    Toast.LENGTH_LONG).show();
            finish();
        } else {
            Toast.makeText(this, "Se eliminaron " + deleted + " de " + existing.size() + ".",
                    Toast.LENGTH_LONG).show();
            populateMessages();
        }
    }

    private void confirmUnblockSender() {
        ThemedDialog.confirm(this, "Desbloquear " + ContactNames.displayName(this, address),
                "Los próximos SMS de " + ContactNames.singleLineLabel(this, address)
                        + " volverán a recibirse normalmente.",
                "Cancelar", "Desbloquear", () -> {
                    Blocklist.unblock(this, address);
                    blockButton.setText("BLOQUEAR NÚMERO");
                    Toast.makeText(this, ContactNames.displayName(this, address) + " desbloqueado.",
                            Toast.LENGTH_SHORT).show();
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
        // An empty conversation closes itself instead of lingering with no content.
        if (deleted > 0 && SmsStore.messagesForAddress(this, address).isEmpty()) {
            finish();
            return;
        }
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
        return Ui.dp(this, value);
    }
}
