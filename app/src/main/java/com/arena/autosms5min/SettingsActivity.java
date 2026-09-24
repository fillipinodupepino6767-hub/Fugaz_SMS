package com.arena.autosms5min;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.List;

/** Holds all controls and guidance so the inbox can stay intentionally calm. */
public final class SettingsActivity extends Activity {
    private static final int REQUEST_SMS_ROLE = 400;
    private boolean dark;
    private TextView status;
    private Button setupButton;
    private Button retentionButton;
    private Button blocklistButton;
    private Button refreshButton;
    private Button archivedButton;
    private Button normalDeleteButton;
    private Button spamDeleteButton;
    private Button importantDeleteButton;
    private Button swipeRightButton;
    private Button swipeLeftButton;
    private Button notifButton;
    private Button exactAlarmButton;
    private Button deletedHistoryButton;
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        ensureSmsPermissionsIfDefault();
        refreshUi();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
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

        Button autoSetup = actionButton("CONFIGURACIÓN AUTOMÁTICA");
        autoSetup.setOnClickListener(v -> SetupHelper.runAutoSetup(this, REQUEST_SMS_ROLE));
        root.addView(autoSetup);

        root.addView(sectionTitle("Mensajes"));
        retentionButton = actionButton("");
        retentionButton.setOnClickListener(v -> showRetentionPicker());
        root.addView(retentionButton);

        blocklistButton = actionButton("");
        blocklistButton.setOnClickListener(v -> showBlockedSenders());
        root.addView(blocklistButton);

        Button simSettings = actionButton("SIM Y ENVÍO DE SMS");
        simSettings.setOnClickListener(v -> startActivity(new Intent(this, SimSettingsActivity.class)));
        root.addView(simSettings);

        deletedHistoryButton = actionButton("");
        deletedHistoryButton.setOnClickListener(v -> startActivity(new Intent(this, DeletedHistoryActivity.class)));
        root.addView(deletedHistoryButton);

        refreshButton = actionButton("ACTUALIZAR ESTADO DE MENSAJES");
        refreshButton.setOnClickListener(v -> runRefresh());
        root.addView(refreshButton);

        archivedButton = actionButton("");
        archivedButton.setOnClickListener(v -> startActivity(new Intent(this, ArchivedActivity.class)));
        root.addView(archivedButton);

        Button cleanup = actionButton("ELIMINAR TODOS LOS SMS ANTERIORES");
        cleanup.setOnClickListener(v -> showLegacyCleanupConfirmation());
        root.addView(cleanup);

        root.addView(sectionTitle("Auto-eliminación por tipo"));
        TextView typeNote = noteText("Elige qué tipos se borran solos con el tiempo elegido. "
                + "Los importantes se conservan por defecto para proteger códigos y bancos.");
        root.addView(typeNote);
        normalDeleteButton = actionButton("");
        normalDeleteButton.setOnClickListener(v -> {
            AppState.setShouldDeleteNormal(this, !AppState.shouldDeleteNormal(this));
            refreshUi();
        });
        root.addView(normalDeleteButton);
        spamDeleteButton = actionButton("");
        spamDeleteButton.setOnClickListener(v -> {
            AppState.setShouldDeleteSpam(this, !AppState.shouldDeleteSpam(this));
            refreshUi();
        });
        root.addView(spamDeleteButton);
        importantDeleteButton = actionButton("");
        importantDeleteButton.setOnClickListener(v -> {
            AppState.setShouldDeleteImportant(this, !AppState.shouldDeleteImportant(this));
            if (AppState.shouldDeleteImportant(this)) {
                Toast.makeText(this, "Los importantes también se borrarán solos.", Toast.LENGTH_LONG).show();
            }
            refreshUi();
        });
        root.addView(importantDeleteButton);

        root.addView(sectionTitle("Deslizar en bandeja"));
        root.addView(noteText("Elige qué hace cada lado al deslizar un mensaje, como en Gmail. "
                + "También puedes mantener presionado un mensaje para ver sus opciones."));
        swipeRightButton = actionButton("");
        swipeRightButton.setOnClickListener(v -> showSwipePicker(true));
        root.addView(swipeRightButton);
        swipeLeftButton = actionButton("");
        swipeLeftButton.setOnClickListener(v -> showSwipePicker(false));
        root.addView(swipeLeftButton);

        root.addView(sectionTitle("Permisos y sistema"));
        notifButton = actionButton("");
        notifButton.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= 33 && !SetupHelper.hasNotificationPermission(this)) {
                SetupHelper.requestMissingRuntimePermissions(this);
            } else {
                SetupHelper.openNotificationSettings(this);
            }
        });
        root.addView(notifButton);
        exactAlarmButton = actionButton("");
        exactAlarmButton.setOnClickListener(v -> {
            if (SetupHelper.needsExactAlarmCheck() && !SetupHelper.canScheduleExactAlarms(this)) {
                SetupHelper.openExactAlarmSettings(this);
            } else {
                Toast.makeText(this, "Las alarmas están listas, no se requiere acción.", Toast.LENGTH_SHORT).show();
            }
        });
        root.addView(exactAlarmButton);
        Button diagnostics = actionButton("DIAGNÓSTICO DE RECEPCIÓN (¿POR QUÉ NO LLEGAN?)");
        diagnostics.setOnClickListener(v -> showDiagnostics());
        root.addView(diagnostics);

        root.addView(sectionTitle("Apariencia"));
        themeButton = actionButton("");
        themeButton.setOnClickListener(v -> showThemePicker());
        root.addView(themeButton);

        root.addView(sectionTitle("Ayuda"));
        Button howItWorks = actionButton("CÓMO FUNCIONA SMS 5 MINUTOS");
        howItWorks.setOnClickListener(v -> showHowItWorks());
        root.addView(howItWorks);

        Button classification = actionButton("CLASIFICACIÓN Y PALABRAS CLAVE");
        classification.setOnClickListener(v -> ThemedDialog.message(this,
                "Clasificación local", MessageClassifier.helpText(), "Entendido"));
        root.addView(classification);

        TextView footer = new TextView(this);
        footer.setText("Versión 0.19.0 beta · SMS de texto\nNo sustituye MMS ni chats RCS de Google Mensajes.");
        footer.setTextColor(ThemeColors.secondaryText(dark));
        footer.setPadding(dp(4), dp(18), dp(4), 0);
        root.addView(footer);
        setContentView(scroll);
    }

    private Button actionButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextColor(ThemeColors.accent(dark));
        button.setBackground(ThemeColors.rounded(this, ThemeColors.incomingBubble(dark), 10));
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

    private TextView noteText(String text) {
        TextView note = new TextView(this);
        note.setText(text);
        note.setTextSize(14);
        note.setTextColor(ThemeColors.secondaryText(dark));
        note.setPadding(dp(4), 0, dp(4), dp(6));
        return note;
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
        deletedHistoryButton.setText("ELIMINADOS RECIENTEMENTE · AJUSTAR TIEMPO ("
                + DeletionLog.retentionLabel(this).toUpperCase() + ")");
        archivedButton.setText("MENSAJES ARCHIVADOS: " + ArchiveStore.count(this));
        normalDeleteButton.setText("BORRAR NORMALES: "
                + (AppState.shouldDeleteNormal(this) ? "SÍ" : "NO"));
        spamDeleteButton.setText("BORRAR POSIBLE SPAM: "
                + (AppState.shouldDeleteSpam(this) ? "SÍ" : "NO"));
        importantDeleteButton.setText("BORRAR IMPORTANTES: "
                + (AppState.shouldDeleteImportant(this) ? "SÍ" : "NO"));
        swipeRightButton.setText("DESLIZAR A LA DERECHA →: "
                + AppState.swipeLabel(AppState.swipeRightAction(this)).toUpperCase());
        swipeLeftButton.setText("DESLIZAR A LA IZQUIERDA ←: "
                + AppState.swipeLabel(AppState.swipeLeftAction(this)).toUpperCase());
        notifButton.setText("NOTIFICACIONES: "
                + (SetupHelper.notificationsEnabled(this) ? "ACTIVADAS" : "DESACTIVADAS (TOCA PARA ABRIR AJUSTES)"));
        if (!SetupHelper.needsExactAlarmCheck()) {
            exactAlarmButton.setText("ALARMAS EXACTAS: NO REQUERIDO EN ESTE ANDROID");
        } else if (SetupHelper.canScheduleExactAlarms(this)) {
            exactAlarmButton.setText("ALARMAS EXACTAS: OK");
        } else {
            exactAlarmButton.setText("ALARMAS EXACTAS: PENDIENTE (TOCA PARA ABRIR AJUSTE)");
        }
        themeButton.setText("APARIENCIA: " + AppState.themeLabel(this).toUpperCase());
    }

    private void runRefresh() {
        MessageMaintenance.Result result = MessageMaintenance.refreshAll(this);
        refreshUi();
        ThemedDialog.message(this, "Estado actualizado", result.summary()
                + "\n\nLos mensajes enviados siempre se conservan; elimínalos manualmente si lo deseas.",
                "Entendido");
    }

    private void showThemePicker() {
        final int[] values = {AppState.THEME_LIGHT, AppState.THEME_DARK, AppState.THEME_SYSTEM};
        final String[] labels = {"Claro", "Oscuro", "Automático (según el teléfono)"};
        int current = AppState.themeMode(this);
        int checked = 2;
        for (int i = 0; i < values.length; i++) if (values[i] == current) checked = i;
        ThemedDialog.singleChoice(this, "Apariencia",
                "Automático usa el modo claro u oscuro que tengas activado en el teléfono y cambia solo con él.",
                labels, checked, which -> {
                    AppState.setThemeMode(this, values[which]);
                    recreate();
                });
    }

    private void showDiagnostics() {
        String last;
        long lastAt = AppState.lastSmsReceivedAt(this);
        if (lastAt <= 0L) {
            last = "Todavía ninguno desde esta versión";
        } else {
            last = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(lastAt);
        }
        int pending = DeleteRegistry.all(this).size();
        String body = "Estado ahora:\n"
                + "• App SMS predeterminada: " + (isDefaultSmsApp() ? "Sí" : "No") + "\n"
                + "• Permisos SMS: " + (SetupHelper.hasSmsPermissions(this) ? "OK" : "Faltan") + "\n"
                + "• Notificaciones: " + (SetupHelper.notificationsEnabled(this) ? "Activadas" : "Bloqueadas") + "\n"
                + "• Último SMS recibido: " + last + "\n"
                + "• Borrados programados: " + pending + " pendiente(s)\n"
                + "• Borrado: " + AppState.retentionLabel(this)
                + " · Normal:" + yesNo(AppState.shouldDeleteNormal(this))
                + " Spam:" + yesNo(AppState.shouldDeleteSpam(this))
                + " Importantes:" + yesNo(AppState.shouldDeleteImportant(this))
                + "\n\nSobre RCS (chats de Google Mensajes y iPhone):\n"
                + "Ninguna app que no sea Google Mensajes puede recibir chats RCS: Google no ofrece "
                + "acceso a terceros. Si alguien te escribe por chat, ese mensaje solo aparece en "
                + "Google Mensajes, nunca aquí, y tampoco se puede responder desde esta app.\n\n"
                + "Si quieres recibirlo TODO como SMS en esta app: abre Google Mensajes → tu foto → "
                + "Ajustes de Mensajes → Chats RCS y desactívalos. Desde ese momento los mensajes "
                + "llegarán como SMS normales.";
        ThemedDialog.message(this, "Diagnóstico de recepción", body, "Entendido");
    }

    private String yesNo(boolean value) {
        return value ? "Sí" : "No";
    }

    private void showSwipePicker(boolean rightSide) {
        final int[] values = {AppState.SWIPE_NOTHING, AppState.SWIPE_DELETE, AppState.SWIPE_ARCHIVE};
        final String[] labels = {"Nada (desactivar ese lado)", "Eliminar", "Archivar"};
        int current = rightSide ? AppState.swipeRightAction(this) : AppState.swipeLeftAction(this);
        int checked = 0;
        for (int i = 0; i < values.length; i++) if (values[i] == current) checked = i;
        ThemedDialog.singleChoice(this,
                rightSide ? "Deslizar a la derecha" : "Deslizar a la izquierda",
                "Elige qué ocurre al deslizar un mensaje de la bandeja hacia ese lado.",
                labels, checked, which -> {
                    if (rightSide) AppState.setSwipeRightAction(this, values[which]);
                    else AppState.setSwipeLeftAction(this, values[which]);
                    refreshUi();
                });
    }

    private void showDefaultSmsWarning() {
        ThemedDialog.confirm(this,
                "Usar SMS 5 minutos como app predeterminada",
                "Esta app debe ser la aplicación SMS predeterminada para recibir y administrar SMS. "
                        + "Los SMS nuevos se eliminarán según el tiempo elegido; puedes conservarlos desde la notificación o conversación. "
                        + "Esta versión beta gestiona SMS de texto, no MMS ni chats RCS de Google Mensajes.",
                "Cancelar", "Continuar", () -> requestSmsRole());
    }

    private void requestSmsRole() {
        if (isDefaultSmsApp()) {
            ensureSmsPermissionsIfDefault();
            refreshUi();
            return;
        }
        SetupHelper.requestSmsRole(this, REQUEST_SMS_ROLE);
    }

    private void ensureSmsPermissionsIfDefault() {
        SetupHelper.requestMissingRuntimePermissions(this);
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
        ThemedDialog.singleChoice(this, "Tiempo para borrar SMS nuevos",
                "Se aplica a los SMS que lleguen de ahora en adelante, según su tipo. "
                        + "Los que ya tienen cuenta atrás conservan su hora original.",
                labels, checked, which -> {
                    AppState.setRetentionMillis(this, values[which]);
                    refreshUi();
                });
    }

    private void showBlockedSenders() {
        List<String> senders = Blocklist.all(this);
        if (senders.isEmpty()) {
            ThemedDialog.message(this, "Números bloqueados",
                    "No hay números bloqueados. Abre una conversación y usa Bloquear número para añadir uno.",
                    "Cerrar");
            return;
        }
        String[] items = senders.toArray(new String[0]);
        ThemedDialog.items(this, "Números bloqueados",
                "Toca un número para desbloquearlo.", items, "Cerrar", which -> confirmUnblock(items[which]));
    }

    private void confirmUnblock(String sender) {
        ThemedDialog.confirm(this, "Desbloquear " + sender,
                "Los próximos SMS de este remitente volverán a recibirse normalmente.",
                "Cancelar", "Desbloquear", () -> {
                    Blocklist.unblock(this, sender);
                    Toast.makeText(this, sender + " desbloqueado.", Toast.LENGTH_SHORT).show();
                    refreshUi();
                });
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
        ThemedDialog.confirm(this, "Eliminar " + count + " SMS anteriores",
                "Esto borrará de la base local de Android todos los SMS anteriores al momento en que empezaste a usar esta app. "
                        + "Incluye mensajes recibidos, enviados y borradores antiguos. No elimina copias de seguridad, MMS ni chats RCS.",
                "Cancelar", "Continuar", () -> showFinalLegacyCleanupConfirmation(count, cutoff));
    }

    private void showFinalLegacyCleanupConfirmation(int count, long cutoff) {
        ThemedDialog.confirm(this, "Confirmación final",
                "Vas a eliminar " + count + " SMS locales antiguos. Esta acción no se puede deshacer. ¿Deseas eliminarlos ahora?",
                "Cancelar", "Eliminar " + count + " SMS", () -> {
                    int deleted = SmsStore.deleteMessagesBefore(this, cutoff);
                    DeletionLog.addSummary(this, deleted, "Limpieza de SMS anteriores");
                    Toast.makeText(this, deleted + " SMS antiguos eliminados.", Toast.LENGTH_LONG).show();
                });
    }

    private void showHowItWorks() {
        ThemedDialog.message(this, "Cómo funciona",
                "• Los SMS nuevos se eliminan aproximadamente tras el tiempo elegido, según su tipo (normal, posible spam o importante).\n\n"
                        + "• Los importantes se conservan por defecto; cámbialo en Auto-eliminación por tipo si quieres que también se borren.\n\n"
                        + "• Los SMS que TÚ envías siempre se conservan; elimínalos manualmente cuando quieras.\n\n"
                        + "• Los chats RCS (Google Mensajes, iPhone) NO pueden llegar a esta app: Google no da acceso a terceros. Usa Diagnóstico de recepción para más detalles.\n\n"
                        + "• Desliza un mensaje en la bandeja para eliminarlo o archivarlo, como en Gmail. Cada lado se configura por separado.\n\n"
                        + "• Mantén presionado un mensaje para abrirlo, archivarlo o eliminarlo. Los archivados no se borran solos.\n\n"
                        + "• Toca un registro en Eliminados recientemente para recuperarlo a la bandeja o archivarlo antes de que venza.\n\n"
                        + "• Actualizar estado de mensajes programa los entrantes que no tenían cuenta atrás y aplica tus ajustes actuales.\n\n"
                        + "• En Apariencia puedes usar Claro, Oscuro o Automático (sigue el modo del teléfono).\n\n"
                        + "• Puedes tocar Conservar desde la notificación o desde una conversación para evitar que un mensaje se borre.\n\n"
                        + "• Puedes bloquear remitentes desde una conversación. Sus próximos SMS se descartan localmente sin notificación.\n\n"
                        + "• En SIM y envío puedes ver las SIM activas, el número que el operador exponga y elegir la SIM para SMS salientes.\n\n"
                        + "• Compartir envía el texto a otra app, como WhatsApp o correo; esa app puede usar Wi-Fi o datos, pero no convierte el SMS en un SMS por Wi-Fi.\n\n"
                        + "• Si algo falla (notificaciones, borrado), usa Configuración automática: revisa rol, permisos y alarmas paso a paso.\n\n"
                        + "• Esta es una app beta para SMS de texto. Google Mensajes puede seguir mostrando su historial o chats RCS.",
                "Entendido");
    }

    private boolean isDefaultSmsApp() {
        return SetupHelper.isDefaultSms(this);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + .5f);
    }
}
