package com.arena.autosms5min;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.role.RoleManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/** Holds all controls and guidance so the inbox can stay intentionally calm. */
public final class SettingsActivity extends Activity {
    private static final int REQUEST_SMS_ROLE = 400;
    private static final int REQUEST_PERMISSIONS = 401;
    private boolean dark;
    private TextView status;
    private Button setupButton;
    private Button retentionButton;
    private Button blocklistButton;
    private Button themeButton;

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
        if (dark != AppState.isDarkMode(this)) {
            recreate();
            return;
        }
        refreshUi();
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
        title.setText("Configuración");
        title.setTextSize(24);
        title.setTextColor(ThemeColors.primaryText(dark));
        toolbar.addView(title);
        root.addView(toolbar);

        status = new TextView(this);
        status.setTextColor(ThemeColors.secondaryText(dark));
        status.setPadding(0, dp(6), 0, dp(10));
        root.addView(status);

        setupButton = actionButton("CONFIGURAR COMO APP SMS PREDETERMINADA");
        setupButton.setOnClickListener(v -> showDefaultSmsWarning());
        root.addView(setupButton);

        root.addView(sectionTitle("Mensajes"));
        retentionButton = actionButton("");
        retentionButton.setOnClickListener(v -> showRetentionPicker());
        root.addView(retentionButton);

        blocklistButton = actionButton("");
        blocklistButton.setOnClickListener(v -> showBlockedSenders());
        root.addView(blocklistButton);

        Button cleanup = actionButton("ELIMINAR TODOS LOS SMS ANTERIORES");
        cleanup.setOnClickListener(v -> showLegacyCleanupConfirmation());
        root.addView(cleanup);

        root.addView(sectionTitle("Apariencia"));
        themeButton = actionButton("");
        themeButton.setOnClickListener(v -> {
            AppState.setDarkMode(this, !dark);
            recreate();
        });
        root.addView(themeButton);

        root.addView(sectionTitle("Ayuda"));
        Button howItWorks = actionButton("CÓMO FUNCIONA SMS 5 MINUTOS");
        howItWorks.setOnClickListener(v -> showHowItWorks());
        root.addView(howItWorks);

        TextView footer = new TextView(this);
        footer.setText("Versión beta · SMS de texto\nNo sustituye MMS ni chats RCS de Google Mensajes.");
        footer.setTextColor(ThemeColors.secondaryText(dark));
        footer.setPadding(dp(4), dp(18), dp(4), 0);
        root.addView(footer);
        setContentView(scroll);
    }

    private Button actionButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextColor(ThemeColors.accent(dark));
        button.setBackgroundColor(ThemeColors.incomingBubble(dark));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dp(3), 0, dp(3));
        button.setLayoutParams(params);
        return button;
    }

    private TextView sectionTitle(String text) {
        TextView title = new TextView(this);
        title.setText(text.toUpperCase());
        title.setTextSize(13);
        title.setTextColor(ThemeColors.accent(dark));
        title.setPadding(dp(4), dp(18), 0, dp(5));
        return title;
    }

    private void refreshUi() {
        boolean isDefault = isDefaultSmsApp();
        if (isDefault) {
            status.setText("SMS 5 minutos está activa como aplicación SMS predeterminada.");
            setupButton.setVisibility(View.GONE);
        } else {
            status.setText("Configura esta app como predeterminada para recibir y administrar SMS.");
            setupButton.setVisibility(View.VISIBLE);
        }
        retentionButton.setText("TIEMPO DE BORRADO: " + AppState.retentionLabel(this).toUpperCase());
        blocklistButton.setText("NÚMEROS BLOQUEADOS: " + Blocklist.count(this));
        themeButton.setText(dark ? "USAR MODO CLARO" : "USAR MODO OSCURO");
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
                    refreshUi();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void showBlockedSenders() {
        List<String> senders = Blocklist.all(this);
        if (senders.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("Números bloqueados")
                    .setMessage("No hay números bloqueados. Abre una conversación y usa Bloquear número para añadir uno.")
                    .setPositiveButton("Cerrar", null)
                    .show();
            return;
        }
        String[] items = senders.toArray(new String[0]);
        new AlertDialog.Builder(this)
                .setTitle("Números bloqueados")
                .setItems(items, (dialog, which) -> confirmUnblock(items[which]))
                .setNegativeButton("Cerrar", null)
                .show();
    }

    private void confirmUnblock(String sender) {
        new AlertDialog.Builder(this)
                .setTitle("Desbloquear " + sender)
                .setMessage("Los próximos SMS de este remitente volverán a recibirse normalmente.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Desbloquear", (dialog, which) -> {
                    Blocklist.unblock(this, sender);
                    Toast.makeText(this, sender + " desbloqueado.", Toast.LENGTH_SHORT).show();
                    refreshUi();
                })
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
                })
                .show();
    }

    private void showHowItWorks() {
        new AlertDialog.Builder(this)
                .setTitle("Cómo funciona")
                .setMessage("• Los SMS nuevos se eliminan aproximadamente tras el tiempo elegido.\n\n"
                        + "• Puedes tocar Conservar desde la notificación o desde una conversación para evitar que un mensaje se borre.\n\n"
                        + "• Puedes bloquear remitentes desde una conversación. Sus próximos SMS se descartan localmente sin notificación.\n\n"
                        + "• Eliminar SMS anteriores solo borra SMS locales; no elimina respaldos, MMS, RCS, copias del operador ni del remitente.\n\n"
                        + "• Esta es una app beta para SMS de texto. Google Mensajes puede seguir mostrando su historial o chats RCS.")
                .setPositiveButton("Entendido", null)
                .show();
    }

    private boolean isDefaultSmsApp() {
        return getPackageName().equals(Telephony.Sms.getDefaultSmsPackage(this));
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + .5f);
    }
}
