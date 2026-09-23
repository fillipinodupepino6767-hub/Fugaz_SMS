package com.arena.autosms5min;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.role.RoleManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Telephony;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

public final class MainActivity extends Activity {
    private static final int REQUEST_SMS_ROLE = 400;
    private static final int REQUEST_PERMISSIONS = 401;
    private final List<SmsStore.SmsItem> shownItems = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private TextView status;
    private TextView note;
    private Button setupButton;
    private Button retentionButton;
    private Button cleanupButton;
    private Button themeButton;
    private boolean dark;
    private final Handler countdownHandler = new Handler(Looper.getMainLooper());
    private final Runnable countdownTicker = new Runnable() {
        @Override public void run() {
            refresh();
            // The conversation view refreshes per second; the inbox only needs a lighter update.
            countdownHandler.postDelayed(this, 30_000L);
        }
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        dark = AppState.isDarkMode(this);
        ThemeColors.applySystemBars(this, dark);
        buildUi();
        ensureSmsPermissionsIfDefault();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
        countdownHandler.removeCallbacks(countdownTicker);
        countdownHandler.postDelayed(countdownTicker, 30_000L);
    }

    @Override
    protected void onPause() {
        countdownHandler.removeCallbacks(countdownTicker);
        super.onPause();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleComposeIntent(intent);
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(16), dp(16), dp(8));
        root.setBackgroundColor(ThemeColors.background(dark));

        TextView title = new TextView(this);
        title.setText("SMS 5 minutos");
        title.setTextSize(24);
        title.setTextColor(ThemeColors.primaryText(dark));
        root.addView(title);

        status = new TextView(this);
        status.setPadding(0, dp(8), 0, dp(8));
        status.setTextColor(ThemeColors.secondaryText(dark));
        root.addView(status);

        // This appears only while Android has another default SMS app.
        setupButton = new Button(this);
        setupButton.setText("CONFIGURAR COMO APP SMS PREDETERMINADA");
        setupButton.setOnClickListener(v -> showDefaultSmsWarning());
        root.addView(setupButton);

        note = new TextView(this);
        note.setPadding(0, dp(4), 0, dp(4));
        note.setTextColor(ThemeColors.secondaryText(dark));
        root.addView(note);

        retentionButton = new Button(this);
        retentionButton.setOnClickListener(v -> showRetentionPicker());
        root.addView(retentionButton);

        cleanupButton = new Button(this);
        cleanupButton.setText("ELIMINAR TODOS LOS SMS ANTERIORES");
        cleanupButton.setOnClickListener(v -> showLegacyCleanupConfirmation());
        root.addView(cleanupButton);

        themeButton = new Button(this);
        themeButton.setText(dark ? "USAR MODO CLARO" : "USAR MODO OSCURO");
        themeButton.setOnClickListener(v -> {
            AppState.setDarkMode(this, !dark);
            recreate();
        });
        root.addView(themeButton);

        ListView list = new ListView(this);
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, new ArrayList<>()) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView row = (TextView) super.getView(position, convertView, parent);
                row.setTextColor(ThemeColors.primaryText(dark));
                row.setTextSize(16);
                row.setPadding(dp(12), dp(12), dp(12), dp(12));
                row.setBackgroundColor(ThemeColors.background(dark));
                return row;
            }
        };
        list.setAdapter(adapter);
        list.setOnItemClickListener((parent, view, position, id) -> {
            SmsStore.SmsItem item = shownItems.get(position);
            Intent conversation = new Intent(this, ConversationActivity.class);
            conversation.putExtra(ConversationActivity.EXTRA_ADDRESS, item.address);
            startActivity(conversation);
        });
        root.addView(list, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);
    }

    private void showDefaultSmsWarning() {
        new AlertDialog.Builder(this)
                .setTitle("Usar SMS 5 minutos como app predeterminada")
                .setMessage("Esta app debe ser la aplicación SMS predeterminada para recibir y administrar SMS. "
                        + "Los SMS nuevos se eliminarán según el tiempo elegido; puedes conservarlos desde la notificación o conversación. "
                        + "Esta versión beta gestiona SMS de texto, no MMS ni chats RCS de Google Mensajes.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Continuar", (dialog, which) -> requestSmsRole())
                .show();
    }

    private void requestSmsRole() {
        if (isDefaultSmsApp()) {
            ensureSmsPermissionsIfDefault();
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            RoleManager roles = getSystemService(RoleManager.class);
            if (roles != null && roles.isRoleAvailable(RoleManager.ROLE_SMS)) {
                startActivityForResult(roles.createRequestRoleIntent(RoleManager.ROLE_SMS), REQUEST_SMS_ROLE);
            }
        } else {
            Intent change = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
            change.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, getPackageName());
            startActivityForResult(change, REQUEST_SMS_ROLE);
        }
    }

    private void ensureSmsPermissionsIfDefault() {
        if (isDefaultSmsApp() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && checkSelfPermission(Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{
                    Manifest.permission.RECEIVE_SMS,
                    Manifest.permission.READ_SMS,
                    Manifest.permission.SEND_SMS
            }, REQUEST_PERMISSIONS);
        }
    }

    private void showRetentionPicker() {
        final long[] values = {
                AppState.ONE_MINUTE, AppState.FIVE_MINUTES, AppState.TEN_MINUTES,
                AppState.THIRTY_MINUTES, AppState.NEVER
        };
        final String[] labels = {
                "1 minuto", "5 minutos", "10 minutos", "30 minutos", "Nunca automáticamente"
        };
        long current = AppState.retentionMillis(this);
        int checked = 1;
        for (int i = 0; i < values.length; i++) if (values[i] == current) checked = i;
        new AlertDialog.Builder(this)
                .setTitle("Tiempo para borrar SMS nuevos")
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    AppState.setRetentionMillis(this, values[which]);
                    dialog.dismiss();
                    refresh();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void showLegacyCleanupConfirmation() {
        if (!isDefaultSmsApp()) {
            Toast.makeText(this, "Primero debes configurar esta app como aplicación SMS predeterminada.", Toast.LENGTH_LONG).show();
            return;
        }
        long cutoff = AppState.visibleSince(this);
        int count = SmsStore.countMessagesBefore(this, cutoff);
        if (count == 0) {
            Toast.makeText(this, "No hay SMS anteriores para eliminar.", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Eliminar " + count + " SMS anteriores")
                .setMessage("Esto borrará de la base local de Android todos los SMS anteriores al momento en que empezaste a usar esta app. "
                        + "Incluye mensajes recibidos, enviados y borradores antiguos. No elimina copias de seguridad, MMS ni chats RCS.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Continuar", (dialog, which) -> showFinalLegacyCleanupConfirmation(count, cutoff))
                .show();
    }

    private void showFinalLegacyCleanupConfirmation(int count, long cutoff) {
        new AlertDialog.Builder(this)
                .setTitle("Confirmación final")
                .setMessage("Vas a eliminar " + count + " SMS locales antiguos. Esta acción no se puede deshacer. ¿Deseas eliminarlos ahora?")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Eliminar " + count + " SMS", (dialog, which) -> {
                    int deleted = SmsStore.deleteMessagesBefore(this, cutoff);
                    Toast.makeText(this, deleted + " SMS antiguos eliminados.", Toast.LENGTH_LONG).show();
                    refresh();
                })
                .show();
    }

    private boolean isDefaultSmsApp() {
        return getPackageName().equals(Telephony.Sms.getDefaultSmsPackage(this));
    }

    private void updateStatus() {
        if (status == null) return;
        boolean isDefault = isDefaultSmsApp();
        if (isDefault) {
            status.setText("Estado: app SMS predeterminada. El borrado automático está activo.");
            setupButton.setVisibility(View.GONE);
        } else {
            status.setText("Estado: falta elegir esta app como aplicación SMS predeterminada.");
            setupButton.setVisibility(View.VISIBLE);
        }
        String chosen = AppState.retentionLabel(this);
        note.setText("Los SMS nuevos se borran aproximadamente después del tiempo elegido. Usa Conservar para evitar el borrado de un mensaje concreto.");
        retentionButton.setText("TIEMPO DE BORRADO: " + chosen.toUpperCase());
    }

    private void refresh() {
        updateStatus();
        List<SmsStore.SmsItem> all = SmsStore.allMessages(this);
        shownItems.clear();
        shownItems.addAll(all);
        List<String> labels = new ArrayList<>();
        DateFormat format = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        for (SmsStore.SmsItem item : all) {
            String direction = item.type == SmsStore.TYPE_SENT ? "Tú → " : "← ";
            long dueAt = DeleteRegistry.dueAt(this, item.id);
            String countdown = item.type == SmsStore.TYPE_INBOX && dueAt > 0L
                    ? "\nSe elimina aproximadamente en " + CountdownFormatter.formatRemaining(dueAt)
                    : "";
            labels.add(direction + item.address + "\n" + item.body + "\n"
                    + format.format(item.date) + countdown);
        }
        adapter.clear();
        adapter.addAll(labels);
        adapter.notifyDataSetChanged();
    }

    private void handleComposeIntent(Intent intent) {
        if (intent == null || !Intent.ACTION_SENDTO.equals(intent.getAction())) return;
        Uri data = intent.getData();
        if (data == null) return;
        String number = data.getSchemeSpecificPart();
        if (number == null || number.isEmpty()) return;
        Intent conversation = new Intent(this, ConversationActivity.class)
                .putExtra(ConversationActivity.EXTRA_ADDRESS, number);
        startActivity(conversation);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + .5f);
    }
}
