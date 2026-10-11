package com.arena.autosms5min;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
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
    private static final int REQUEST_PICK_CONTACT = 710;
    private EditText addressBox;
    private EditText bodyBox;
    private TextView nameLine;
    private String pickedName;
    private boolean applyingPick;
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
        nameLine = new TextView(this);
        nameLine.setTextColor(ThemeColors.accent(dark));
        Ui.text(nameLine, 15f);
        nameLine.setPadding(dp(6), 0, dp(4), dp(2));
        nameLine.setVisibility(View.GONE);
        card.addView(nameLine);
        Button pickContact = new Button(this);
        pickContact.setText("👤 ELEGIR CONTACTO");
        Ui.styleActionButton(pickContact, dark);
        pickContact.setMinHeight(dp(46));
        Ui.text(pickContact, 14f);
        pickContact.setOnClickListener(v -> pickContact());
        LinearLayout.LayoutParams pickParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        pickParams.setMargins(0, 0, 0, dp(6));
        card.addView(pickContact, pickParams);
        addressBox.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override public void afterTextChanged(Editable s) {
                if (applyingPick) return;
                pickedName = null;
                updateNameLabel();
            }
        });
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
        note.setText("Puedes escribir el número o tocar 👤 ELEGIR CONTACTO: se abre el "
                + "selector de contactos de Android (incluye los de tu cuenta de Google, "
                + "con buscador) sin dar permisos nuevos. Si ya diste el permiso de "
                + "contactos, el nombre aparece solo al escribir el número. El SMS sale "
                + "por la línea elegida en Configuración (SIMs) y lo enviado queda en la "
                + "conversación, sin borrado automático.");
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

    /** System contact picker: phone and Google contacts, no permanent permission. */
    private void pickContact() {
        Intent pick = new Intent(Intent.ACTION_PICK)
                .setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE);
        try {
            startActivityForResult(pick, REQUEST_PICK_CONTACT);
        } catch (Exception failed) {
            Toast.makeText(this, "Este teléfono no ofrece un selector de contactos.",
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_PICK_CONTACT || resultCode != RESULT_OK
                || data == null || data.getData() == null) return;
        try (Cursor cursor = getContentResolver().query(data.getData(),
                new String[] {ContactsContract.CommonDataKinds.Phone.NUMBER,
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME},
                null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                String rawNumber = cursor.getString(0);
                pickedName = cursor.getString(1);
                if (rawNumber != null) {
                    applyingPick = true;
                    addressBox.setText(rawNumber.replaceAll("[^0-9+]", ""));
                    applyingPick = false;
                }
            }
        } catch (Exception ignored) {
            // Unreadable contact row: manual entry still works.
        }
        updateNameLabel();
    }

    /** Shows who the number belongs to: picked name, or contacts permission. */
    private void updateNameLabel() {
        String address = addressBox.getText().toString().trim();
        if (address.isEmpty()) {
            nameLine.setVisibility(View.GONE);
            return;
        }
        String label = pickedName != null && !pickedName.isEmpty()
                ? pickedName : ContactNames.displayName(this, address);
        if (label == null || label.equals(address)) {
            nameLine.setVisibility(View.GONE);
        } else {
            nameLine.setVisibility(View.VISIBLE);
            nameLine.setText("👤 " + label);
        }
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
