package com.arena.autosms5min;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.text.InputType;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Holds all controls and guidance so the inbox can stay intentionally calm. */
public final class SettingsActivity extends Activity implements HelpTour.Host {
    /** Extra: start the contextual help tour right after opening. */
    public static final String EXTRA_START_HELP = "start_help";
    /** Extra: open the reception diagnostics right away (from the yellow bar). */
    public static final String EXTRA_OPEN_DIAGNOSTICS = "open_diagnostics";
    private static final int REQUEST_SMS_ROLE = 400;
    private static final int REQUEST_EXPORT = 420;
    private static final int REQUEST_IMPORT = 421;
    private static final String GOOGLE_MESSAGES_PKG = "com.google.android.apps.messaging";
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
    private Button contactsButton;
    private Button deletedHistoryButton;
    private Button themeButton;
    private Button watchButton;
    private Button watchTimeButton;
    private Button privButton;
    private Button changePinButton;
    private Button bioButton;
    private ScrollView scroll;
    private final Map<String, View> helpAnchors = new HashMap<>();
    private final List<TextView> notes = new ArrayList<>();
    private boolean helpPending;
    private View blinkingView;
    private android.graphics.drawable.Drawable blinkingSaved;
    private boolean blinkPhaseOn;
    /** Keeps the highlighted section blinking while its help window is shown. */
    private final Runnable blinkRunnable = new Runnable() {
        @Override
        public void run() {
            if (blinkingView == null) return;
            blinkPhaseOn = !blinkPhaseOn;
            if (blinkPhaseOn) {
                blinkingView.setBackground(
                        ThemeColors.rounded(SettingsActivity.this, highlightColor(), 8));
            } else if (blinkingSaved != null) {
                blinkingView.setBackground(blinkingSaved);
            } else {
                blinkingView.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            }
            blinkingView.postDelayed(this, blinkPhaseOn ? 700L : 500L);
        }
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        dark = AppState.isDarkMode(this);
        ThemeColors.applySystemBars(this, dark);
        buildUi();
        ensureSmsPermissionsIfDefault();
        if (getIntent().getBooleanExtra(EXTRA_START_HELP, false)) {
            getIntent().putExtra(EXTRA_START_HELP, false);
            helpPending = true; // hold celebration/reminder until the tour starts
            scroll.post(new Runnable() {
                @Override
                public void run() {
                    helpPending = false;
                    HelpTour.start(SettingsActivity.this, SettingsActivity.this);
                }
            });
        } else if (getIntent().getBooleanExtra(EXTRA_OPEN_DIAGNOSTICS, false)) {
            getIntent().putExtra(EXTRA_OPEN_DIAGNOSTICS, false);
            scroll.post(new Runnable() {
                @Override
                public void run() {
                    showDiagnostics();
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (dark != AppState.isDarkMode(this)) {
            recreate();
            return;
        }
        // Resume the step-by-step flow after the user returns from system screens
        // (restricted settings, notifications, exact alarms, SMS role).
        if (SetupHelper.isAutoSetupPending(this)) {
            SetupHelper.continueAutoSetupIfPending(this, REQUEST_SMS_ROLE);
        }
        refreshUi();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        ensureSmsPermissionsIfDefault();
        if (requestCode == REQUEST_EXPORT && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            exportTo(data.getData());
        } else if (requestCode == REQUEST_IMPORT && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            importFrom(data.getData());
        }
        // Auto-setup resume is handled in onResume (covers role + settings screens).
        refreshUi();
    }

    private void exportTo(Uri uri) {
        try (java.io.OutputStream out = getContentResolver().openOutputStream(uri)) {
            if (out == null) throw new java.io.IOException("sin flujo de escritura");
            out.write(SettingsPort.exportJson(this)
                    .getBytes(java.nio.charset.StandardCharsets.UTF_8));
            Toast.makeText(this, "Ajustes exportados \u2713", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "No se pudo exportar: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void importFrom(Uri uri) {
        try (java.io.InputStream in = getContentResolver().openInputStream(uri)) {
            if (in == null) throw new java.io.IOException("sin flujo de lectura");
            java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
            byte[] chunk = new byte[4096];
            int read;
            while ((read = in.read(chunk)) != -1) buffer.write(chunk, 0, read);
            int applied = SettingsPort.importJson(this,
                    new String(buffer.toByteArray(), java.nio.charset.StandardCharsets.UTF_8));
            Toast.makeText(this, "Ajustes importados \u2713 (" + applied + " valores)",
                    Toast.LENGTH_LONG).show();
            if (AppState.watchRinger(this) || RingerTimer.isArmed(this)) {
                RingerWatchService.ensureRunning(this);
            } else {
                RingerWatchService.setEnabled(this, false);
            }
            recreate();
        } catch (Exception e) {
            Toast.makeText(this, "No se pudo importar: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        refreshUi();
    }

    private void buildUi() {
        scroll = new ScrollView(this);
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
        Ui.styleToolbarBack(back, dark);
        back.setOnClickListener(v -> finish());
        toolbar.addView(back, new LinearLayout.LayoutParams(dp(52), dp(56)));
        TextView title = new TextView(this);
        title.setText("Configuración");
        Ui.text(title, 24f);
        title.setTextColor(ThemeColors.primaryText(dark));
        toolbar.addView(title, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        Button help = new Button(this);
        help.setText("\u2753");
        Ui.text(help, 22f);
        help.setTextColor(ThemeColors.accent(dark));
        help.setContentDescription("Ayuda");
        help.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        help.setOnClickListener(v -> HelpTour.start(this, this));
        toolbar.addView(help, new LinearLayout.LayoutParams(dp(52), dp(56)));
        root.addView(toolbar);

        status = new TextView(this);
        status.setTextColor(ThemeColors.secondaryText(dark));
        Ui.text(status, 15f);
        status.setLineSpacing(0f, 1.2f);
        status.setPadding(0, dp(6), 0, dp(10));
        root.addView(status);

        final Button notesToggle = new Button(this);
        notesToggle.setAllCaps(false);
        Ui.text(notesToggle, 14f);
        notesToggle.setTextColor(ThemeColors.accent(dark));
        notesToggle.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        notesToggle.setGravity(Gravity.END);
        notesToggle.setMinHeight(dp(40));
        applyNotesVisibility(notesToggle);
        notesToggle.setOnClickListener(v -> {
            boolean visible = !AppState.notesVisible(this);
            AppState.setNotesVisible(this, visible);
            applyNotesVisibility(notesToggle);
        });
        root.addView(notesToggle, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        root.addView(noteText("Toca ① primero y sigue los 5 pasos; la app predeterminada es el paso ②. Toca ❓ arriba para las explicaciones."));

        Button autoSetup = actionButton("① CONFIGURACIÓN AUTOMÁTICA");
        autoSetup.setOnClickListener(v -> SetupHelper.runAutoSetup(this, REQUEST_SMS_ROLE));
        helpAnchors.put("setup", autoSetup);
        root.addView(autoSetup);

        setupButton = actionButton("② CONFIGURAR COMO APP SMS PREDETERMINADA");
        setupButton.setOnClickListener(v -> showDefaultSmsWarning());
        root.addView(setupButton);

                TextView anchor_mensajes = sectionTitle("Mensajes");
        helpAnchors.put("mensajes", anchor_mensajes);
        root.addView(anchor_mensajes);
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

        Button keywords = actionButton("MIS PALABRAS CLAVE");
        keywords.setOnClickListener(v -> showKeywordManager());
        root.addView(keywords);

                TextView anchor_tipos = sectionTitle("Auto-eliminación por tipo");
        helpAnchors.put("tipos", anchor_tipos);
        root.addView(anchor_tipos);
        TextView typeNote = noteText("Los importantes no se borran solos: así se protegen códigos y bancos.");
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

                TextView anchor_deslizar = sectionTitle("Deslizar en bandeja");
        helpAnchors.put("deslizar", anchor_deslizar);
        root.addView(anchor_deslizar);
        root.addView(noteText("Como en Gmail. Mantén presionado un mensaje para ver sus opciones."));
        swipeRightButton = actionButton("");
        swipeRightButton.setOnClickListener(v -> showSwipePicker(true));
        root.addView(swipeRightButton);
        swipeLeftButton = actionButton("");
        swipeLeftButton.setOnClickListener(v -> showSwipePicker(false));
        root.addView(swipeLeftButton);

                TextView anchor_permisos = sectionTitle("Permisos y sistema");
        helpAnchors.put("permisos", anchor_permisos);
        root.addView(anchor_permisos);
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
        contactsButton = actionButton("");
        contactsButton.setOnClickListener(v -> {
            if (!SetupHelper.hasContactsPermission(this)) {
                SetupHelper.requestMissingRuntimePermissions(this);
            } else {
                Toast.makeText(this, "Permiso de contactos concedido: verás nombres y fotos.",
                        Toast.LENGTH_SHORT).show();
            }
        });
        root.addView(contactsButton);
        Button diagnostics = actionButton("DIAGNÓSTICO DE RECEPCIÓN (¿POR QUÉ NO LLEGAN?)");
        diagnostics.setOnClickListener(v -> showDiagnostics());
        root.addView(diagnostics);
        Button testSound = actionButton("PROBAR SONIDO DE NOTIFICACIÓN");
        testSound.setOnClickListener(v -> {
            NotificationHelper.showTest(this);
            Toast.makeText(this, "Notificación de prueba enviada.", Toast.LENGTH_SHORT).show();
        });
        root.addView(testSound);

                TextView anchor_google = sectionTitle("Google Mensajes y RCS");
        helpAnchors.put("google", anchor_google);
        root.addView(anchor_google);
        root.addView(noteText("Solo SMS por señal del celular: los chats por internet (Wi-Fi o datos) no llegan aquí."));
        Button openGm = actionButton("ABRIR GOOGLE MENSAJES (PASAR A SOLO SMS)");
        openGm.setOnClickListener(v -> showGoogleMessagesGuide());
        root.addView(openGm);

                TextView anchor_sonido = sectionTitle("Sonido");
        helpAnchors.put("sonido", anchor_sonido);
        root.addView(anchor_sonido);
        root.addView(noteText("Silencia ahora y el sonido vuelve solo cuando se cumpla el tiempo."));
        Button silenceTimer = actionButton("TEMPORIZADOR DE SILENCIO");
        silenceTimer.setOnClickListener(v -> {
            try {
                startActivity(new Intent(this, SilenceTimerActivity.class));
            } catch (Exception failed) {
                Toast.makeText(this, "No se pudo abrir el temporizador.", Toast.LENGTH_LONG).show();
            }
        });
        root.addView(silenceTimer);

        root.addView(noteText("Si otra app, el sistema o No Molestar silencian el teléfono, te avisamos para activar el temporizador de un toque."));
        watchButton = actionButton("");
        watchButton.setOnClickListener(v -> {
            boolean enabled = !AppState.watchRinger(this);
            AppState.setWatchRinger(this, enabled);
            // Keep the watcher alive while a countdown still needs it.
            RingerWatchService.setEnabled(this, enabled || RingerTimer.isArmed(this));
            Toast.makeText(this, enabled ? "Vigilancia activada." : "Vigilancia desactivada.",
                    Toast.LENGTH_SHORT).show();
            refreshUi();
        });
        root.addView(watchButton);
        watchTimeButton = actionButton("");
        watchTimeButton.setOnClickListener(v -> showWatchTimePicker());
        root.addView(watchTimeButton);

                TextView anchor_saldo = sectionTitle("Saldo");
        helpAnchors.put("saldo", anchor_saldo);
        root.addView(anchor_saldo);
        root.addView(noteText("Canales oficiales: Kolbi *888# · Movistar/Liberty SMS 606 · Claro *611#."));
        Button balance = actionButton("SALDO DE MIS LÍNEAS");
        balance.setOnClickListener(v -> {
            try {
                startActivity(new Intent(this, BalanceActivity.class));
            } catch (Exception failed) {
                Toast.makeText(this, "No se pudo abrir el saldo.", Toast.LENGTH_LONG).show();
            }
        });
        root.addView(balance);

        root.addView(sectionTitle("Apariencia"));
        themeButton = actionButton("");
        themeButton.setOnClickListener(v -> showThemePicker());
        root.addView(themeButton);

                root.addView(sectionTitle("Privacidad"));
        privButton = actionButton("");
        privButton.setOnClickListener(v -> toggleLock());
        root.addView(privButton);
        helpAnchors.put("privacidad", privButton);
        changePinButton = actionButton("CAMBIAR PIN");
        changePinButton.setOnClickListener(v -> promptNewPin());
        root.addView(changePinButton);
        bioButton = actionButton("");
        bioButton.setOnClickListener(v -> toggleBio());
        root.addView(bioButton);

        root.addView(sectionTitle("Datos"));
        Button export = actionButton("EXPORTAR AJUSTES (ARCHIVO)");
        export.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT)
                    .addCategory(Intent.CATEGORY_OPENABLE)
                    .setType("application/json")
                    .putExtra(Intent.EXTRA_TITLE, "fugaz-sms-ajustes.json");
            try {
                startActivityForResult(intent, REQUEST_EXPORT);
            } catch (Exception failed) {
                Toast.makeText(this, "Este Android no ofrece guardar archivos.",
                        Toast.LENGTH_LONG).show();
            }
        });
        root.addView(export);
        Button importBtn = actionButton("IMPORTAR AJUSTES");
        importBtn.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT)
                    .addCategory(Intent.CATEGORY_OPENABLE)
                    .setType("application/json");
            try {
                startActivityForResult(intent, REQUEST_IMPORT);
            } catch (Exception failed) {
                Toast.makeText(this, "Este Android no ofrece abrir archivos.",
                        Toast.LENGTH_LONG).show();
            }
        });
        root.addView(importBtn);

        TextView anchor_ayuda = sectionTitle("Ayuda");
        helpAnchors.put("ayuda", anchor_ayuda);
        root.addView(anchor_ayuda);
        Button quickGuide = actionButton("GUÍA RÁPIDA (VENTANAS CON ✕)");
        quickGuide.setOnClickListener(v -> HelpTour.start(this, this));
        root.addView(quickGuide);

        Button welcomeAgain = actionButton("MENSAJE DE BIENVENIDA");
        welcomeAgain.setOnClickListener(v -> HelpTour.showWelcome(this, null));
        root.addView(welcomeAgain);

        Button howItWorks = actionButton("CÓMO FUNCIONA FUGAZ SMS");
        howItWorks.setOnClickListener(v -> showHowItWorks());
        root.addView(howItWorks);

        Button classification = actionButton("CLASIFICACIÓN Y PALABRAS CLAVE");
        classification.setOnClickListener(v -> ThemedDialog.message(this,
                "Clasificación local", MessageClassifier.helpText(this), "Entendido"));
        root.addView(classification);

        TextView footer = new TextView(this);
        footer.setText("Versión 0.27.6 beta · Solo SMS de texto\nNo recibe chats por internet (Google Mensajes o iPhone).");
        footer.setTextColor(ThemeColors.secondaryText(dark));
        footer.setPadding(dp(4), dp(18), dp(4), 0);
        root.addView(footer);
        setContentView(scroll);
        applyNotesVisibility(notesToggle);
    }

    /** Contextual help: scroll the list to the section and flash it. */
    @Override
    public void showStepAt(String anchorKey, int index) {
        final View anchor = helpAnchors.get(anchorKey);
        if (anchor == null || scroll == null) return;
        scroll.post(new Runnable() {
            @Override
            public void run() {
                scroll.smoothScrollTo(0, Math.max(0, anchor.getTop() - dp(14)));
                startBlink(anchor);
            }
        });
    }

    /** Stops any previous section first (next/back never leaves two blinking). */
    private void startBlink(View anchor) {
        stopBlink();
        blinkingView = anchor;
        blinkingSaved = anchor.getBackground();
        blinkPhaseOn = false;
        anchor.post(blinkRunnable);
    }

    private void stopBlink() {
        if (blinkingView != null) {
            blinkingView.removeCallbacks(blinkRunnable);
            if (blinkingSaved != null) {
                blinkingView.setBackground(blinkingSaved);
            } else {
                blinkingView.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            }
        }
        blinkingView = null;
        blinkingSaved = null;
        blinkPhaseOn = false;
    }

    @Override
    public void onTourClosed() {
        stopBlink();
    }

    /**
     * Highlight that is darker than the background but lighter than the text
     * and deliberately not the blue accent: amber (light) / bronze (dark).
     */
    private int highlightColor() {
        return dark ? android.graphics.Color.rgb(83, 62, 18)
                    : android.graphics.Color.rgb(255, 179, 0);
    }

    private void applyNotesVisibility(Button toggle) {
        boolean visible = AppState.notesVisible(this);
        for (TextView note : notes) {
            note.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
        if (toggle != null) {
            toggle.setText(visible ? "💬 Mensajes de ayuda ▾ (ocultar)"
                    : "💬 Mensajes de ayuda ▸ (mostrar)");
        }
    }

    private Button actionButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        Ui.styleActionButton(button, dark);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dp(4), 0, dp(4));
        button.setLayoutParams(params);
        return button;
    }

    private TextView sectionTitle(String text) {
        TextView title = new TextView(this);
        title.setText(text.toUpperCase());
        Ui.text(title, 14f);
        title.setTextColor(ThemeColors.accent(dark));
        title.setPadding(dp(4), dp(20), 0, dp(6));
        return title;
    }

    private TextView noteText(String text) {
        TextView note = new TextView(this);
        note.setText(text);
        Ui.text(note, 15f);
        note.setTextColor(ThemeColors.secondaryText(dark));
        note.setPadding(dp(4), 0, dp(4), dp(8));
        note.setLineSpacing(0f, 1.15f);
        notes.add(note);
        return note;
    }

    private void refreshUi() {
        maybeNudge();
        if (privButton != null) {
            privButton.setText(AppLock.isSet(this)
                    ? ("BLOQUEO AL ABRIR: " + (AppLock.enabled(this)
                            ? "SÍ (PIN ACTIVO)" : "NO (PIN GUARDADO)"))
                    : "BLOQUEO AL ABRIR: SIN PIN (TOCA PARA CREAR)");
            changePinButton.setVisibility(AppLock.isSet(this) ? View.VISIBLE : View.GONE);
            bioButton.setVisibility(AppLock.isSet(this) ? View.VISIBLE : View.GONE);
            bioButton.setText("DESBLOQUEO CON HUELLA: "
                    + (AppLock.bioEnabled(this) ? "SÍ (ACTIVA)" : "NO (APAGADA)"));
        }
        boolean isDefault = isDefaultSmsApp();
        boolean smsPerms = SetupHelper.hasSmsPermissions(this);
        boolean notifOn = SetupHelper.notificationsEnabled(this);
        boolean exactOk = !SetupHelper.needsExactAlarmCheck()
                || SetupHelper.canScheduleExactAlarms(this);
        setupButton.setVisibility(isDefault ? View.GONE : View.VISIBLE);
        if (isDefault && smsPerms && notifOn && exactOk) {
            status.setText("\u2705 Todo listo: Fugaz SMS est\u00e1 activa y lista para recibir SMS.");
        } else {
            StringBuilder check = new StringBuilder("Estado:\n");
            check.append(isDefault ? "\u2705" : "\u274C")
                    .append(" App SMS predeterminada\n");
            if (!isDefault) {
                check.append("\u23F3 Permisos de SMS (se piden despu\u00e9s del paso \u2461)");
            } else {
                check.append(smsPerms ? "\u2705" : "\u274C").append(" Permisos de SMS");
            }
            check.append("\n");
            check.append(notifOn ? "\u2705" : "\u274C").append(" Notificaciones\n");
            check.append(exactOk ? "\u2705" : "\u274C").append(" Alarmas exactas");
            status.setText(check.toString());
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
        contactsButton.setText("CONTACTOS (VER NOMBRES Y FOTOS): "
                + (SetupHelper.hasContactsPermission(this) ? "ACTIVADOS" : "SIN PERMISO (TOCA PARA PEDIRLO)"));
        themeButton.setText("APARIENCIA: " + AppState.themeLabel(this).toUpperCase());
        watchButton.setText("VIGILAR SILENCIO DE OTRAS APPS: "
                + (AppState.watchRinger(this) ? "SÍ" : "NO"));
        watchTimeButton.setText("TIEMPO AUTOMÁTICO: "
                + AppState.watchMinutesLabel(this).toUpperCase());
    }

    private void runRefresh() {
        MessageMaintenance.Result result = MessageMaintenance.refreshAll(this);
        refreshUi();
        ThemedDialog.message(this, "Estado actualizado", result.summary()
                + "\n\nLos mensajes enviados siempre se conservan; elimínalos manualmente si lo deseas.",
                "Entendido");
    }

    private void showWatchTimePicker() {
        final long[] values = {15L, 30L, 60L, 120L, 240L, 480L};
        final String[] labels = {"15 minutos", "30 minutos", "1 hora", "2 horas", "4 horas", "8 horas"};
        long current = AppState.watchDefaultMinutes(this);
        int checked = 3;
        for (int i = 0; i < values.length; i++) if (values[i] == current) checked = i;
        ThemedDialog.singleChoice(this, "Tiempo automático",
                "Al tocar Activar en el aviso de silencio, el sonido volverá solo después de este tiempo.",
                labels, checked, which -> {
                    AppState.setWatchDefaultMinutes(this, values[which]);
                    refreshUi();
                });
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
                + "• Nombres de contactos: " + (SetupHelper.hasContactsPermission(this) ? "Sí" : "No (sin permiso)") + "\n"
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
                + "llegarán como SMS normales. También puedes usar el botón Abrir Google Mensajes de esta pantalla.";
        ThemedDialog.message(this, "Diagnóstico de recepción", body, "Entendido");
    }

    private String yesNo(boolean value) {
        return value ? "Sí" : "No";
    }

    /**
     * No API exists to link with Google Messages, so this opens it directly and
     * guides the user to switch chats to SMS-only. Falls back to Play Store.
     */
    private void showGoogleMessagesGuide() {
        boolean installed;
        try {
            installed = getPackageManager().getLaunchIntentForPackage(GOOGLE_MESSAGES_PKG) != null;
        } catch (Exception e) {
            installed = false;
        }
        String steps = "Para que tus mensajes lleguen como SMS a esta app:\n\n"
                + "1. Abre Google Mensajes.\n"
                + "2. Toca tu foto de perfil → Ajustes de Mensajes.\n"
                + "3. Entra a Chats RCS y DESACTÍVALOS.\n\n"
                + "Para una sola persona: abre su chat en Google Mensajes, toca su nombre arriba "
                + "y elige Solo enviar SMS/MMS.\n\n"
                + "Nota honesta: ninguna app puede conectarse por dentro con Google Mensajes; "
                + "este botón solo la abre para que hagas el ajuste tú.";
        if (installed) {
            ThemedDialog.confirm(this, "Pasar Google Mensajes a solo SMS", steps,
                    "Cerrar", "Abrir Google Mensajes", () -> {
                        Intent launch = getPackageManager().getLaunchIntentForPackage(GOOGLE_MESSAGES_PKG);
                        if (launch != null) startActivity(launch);
                        else {
                            Toast.makeText(this, "No se pudo abrir Google Mensajes.",
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        } else {
            ThemedDialog.confirm(this, "Google Mensajes no está instalado",
                    "Google Mensajes no está instalado en este teléfono. Instálalo para poder "
                            + "ajustar el RCS, o pide a tus contactos que te escriban por SMS.\n\n" + steps,
                    "Cerrar", "Abrir Play Store", () -> openPlayStore(GOOGLE_MESSAGES_PKG));
        }
    }

    private void openPlayStore(String pkg) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + pkg)));
        } catch (ActivityNotFoundException noMarket) {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=" + pkg)));
        }
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
                "Usar Fugaz SMS como app predeterminada",
                "Esta app debe ser la aplicación SMS predeterminada para recibir y administrar SMS. "
                        + "Los SMS nuevos se eliminarán según el tiempo elegido; puedes conservarlos desde la notificación o conversación. "
                        + "Esta versión beta gestiona SMS de texto, no MMS ni chats por internet (RCS de Google Mensajes o iPhone).",
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
                        + "Si hay cuentas atrás en curso, después podrás reiniciarlas con el nuevo tiempo o dejarlas como están.",
                labels, checked, which -> {
                    long selected = values[which];
                    AppState.setRetentionMillis(this, selected);
                    refreshUi();
                    askApplyRetentionToPending(selected);
                });
    }

    /** Offers to restart in-flight countdowns with the newly chosen retention. */
    private void askApplyRetentionToPending(long retention) {
        int pending = MessageMaintenance.countValidPending(this);
        if (pending <= 0) return;
        if (retention == AppState.NEVER) {
            ThemedDialog.confirm(this, "Cancelar cuentas en curso",
                    "Cambiaste a «Nunca automáticamente» y hay " + pending
                            + " mensaje(s) con cuenta atrás en curso. ¿Cancelo esas cuentas? (esos mensajes se conservarán)",
                    "Mantenerlas", "Cancelarlas", () -> {
                        int cancelled = MessageMaintenance.cancelAllPending(this);
                        Toast.makeText(this, cancelled + " cuenta(s) cancelada(s).",
                                Toast.LENGTH_SHORT).show();
                    });
        } else {
            ThemedDialog.confirm(this, "Aplicar a los pendientes",
                    "Hay " + pending + " mensaje(s) con cuenta atrás en curso (conservan su hora original). ¿Los reinicias con el nuevo tiempo ("
                            + AppState.retentionLabel(this) + ") desde ahora?",
                    "Solo nuevos", "Reiniciar todo", () -> {
                        int restarted = MessageMaintenance.restartAllPending(this, retention);
                        Toast.makeText(this, restarted + " cuenta(s) reiniciada(s).",
                                Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void showBlockedSenders() {
        List<String> senders = Blocklist.all(this);
        if (senders.isEmpty()) {
            ThemedDialog.message(this, "Números bloqueados",
                    "No hay números bloqueados. Abre una conversación y usa Bloquear número para añadir uno.",
                    "Cerrar");
            return;
        }
        String[] items = new String[senders.size()];
        for (int i = 0; i < senders.size(); i++) {
            items[i] = ContactNames.singleLineLabel(this, senders.get(i));
        }
        ThemedDialog.items(this, "Números bloqueados",
                "Toca un número para desbloquearlo.", items, "Cerrar", which -> confirmUnblock(senders.get(which)));
    }

    private void confirmUnblock(String sender) {
        ThemedDialog.confirm(this, "Desbloquear " + ContactNames.displayName(this, sender),
                "Los próximos SMS de " + ContactNames.singleLineLabel(this, sender)
                        + " volverán a recibirse normalmente.",
                "Cancelar", "Desbloquear", () -> {
                    Blocklist.unblock(this, sender);
                    Toast.makeText(this, ContactNames.displayName(this, sender) + " desbloqueado.",
                            Toast.LENGTH_SHORT).show();
                    refreshUi();
                });
    }

    private void showKeywordManager() {
        final List<String> important = KeywordStore.importantWords(this);
        final List<String> spam = KeywordStore.spamWords(this);
        List<String> rows = new ArrayList<>();
        rows.add("➕ Añadir palabra de spam");
        rows.add("➕ Añadir palabra importante");
        for (String word : important) rows.add("⭐ " + word + "  (importante · toca para quitar)");
        for (String word : spam) rows.add("🚫 " + word + "  (spam · toca para quitar)");
        ThemedDialog.items(this, "Mis palabras clave",
                "Tus palabras tienen prioridad sobre las listas internas. Úsalas para corregir casos como promos que se cuelan o avisos que se marcan mal.",
                rows.toArray(new String[0]), "Cerrar", which -> {
                    if (which == 0) showAddKeyword(true);
                    else if (which == 1) showAddKeyword(false);
                    else {
                        int index = which - 2;
                        if (index < important.size()) confirmRemoveKeyword(false, important.get(index));
                        else confirmRemoveKeyword(true, spam.get(index - important.size()));
                    }
                });
    }

    private void showAddKeyword(final boolean spam) {
        ThemedDialog.input(this, spam ? "Añadir palabra de spam" : "Añadir palabra importante",
                "Ejemplos: el nombre de una tienda que te spamea, o una palabra de tus avisos. Se compara sin mayúsculas ni tildes.",
                "", android.text.InputType.TYPE_CLASS_TEXT,
                null, "Cancelar", "Añadir", null, value -> {
                    String word = value == null ? "" : value.trim();
                    if (word.isEmpty()) {
                        Toast.makeText(this, "Escribe una palabra.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (spam) KeywordStore.addSpam(this, word);
                    else KeywordStore.addImportant(this, word);
                    Toast.makeText(this, "«" + word + "» añadida.", Toast.LENGTH_SHORT).show();
                    showKeywordManager();
                });
    }

    private void confirmRemoveKeyword(final boolean spam, final String word) {
        ThemedDialog.confirm(this, "Quitar «" + word + "»",
                "Se quitará de tu lista " + (spam ? "de spam." : "de importantes.")
                        + " Volverán a mandar las listas internas para esa palabra.",
                "Cancelar", "Quitar", () -> {
                    if (spam) KeywordStore.removeSpam(this, word);
                    else KeywordStore.removeImportant(this, word);
                    showKeywordManager();
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
                        + "Incluye mensajes recibidos, enviados y borradores antiguos. No elimina copias de seguridad, MMS ni chats por internet.",
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
                        + "• Esta app solo recibe SMS de texto (señal del celular, sin internet). Los chats por internet — Wi-Fi o datos — de Google Mensajes o iPhone NO pueden llegar aquí: Google no da acceso a otras apps. Usa el botón Abrir Google Mensajes para pasarlos a solo SMS, o Diagnóstico de recepción para más detalles.\n\n"
                        + "• La bandeja muestra el nombre y la foto de tus contactos si das el permiso; si no, verás los números como siempre.\n\n"
                        + "• Usa el buscador de la bandeja para filtrar por nombre, número o texto, y el de cada conversación para encontrar un mensaje.\n\n"
                        + "• Si no escuchas avisos, usa Probar sonido de notificación: si la prueba suena, el problema es del volumen o de otra app, no de Fugaz SMS.\n\n"
                        + "• El clasificador detecta promos (cupones, adelantos de saldo, descuentos, enlaces) como posible spam aunque mencionen saldo o bancos; los códigos y alertas de fraude siguen siendo importantes.\n\n"
                        + "• Si cambias el tiempo de borrado con cuentas en curso, podrás reiniciarlas con el nuevo tiempo o dejarlas como están.\n\n"
                        + "• En Mis palabras clave puedes añadir tus propias palabras de spam o importantes; las tuyas mandan sobre las listas internas.\n\n"
                        + "• Al bloquear un número puedes borrar también sus mensajes existentes de una vez; si una conversación queda vacía, se cierra sola.\n\n"
                        + "• El temporizador de silencio vibra o silencia el teléfono ahora y restaura el sonido solo después de los minutos que escribas. Si usas No molestar, dale el permiso extra que pide la pantalla.\n\n"
                        + "• Si activas Vigilar silencio de otras apps, cuando Volume Styles, el sistema o No Molestar pongan el teléfono en silencio o vibración te llega una notificación: de un toque activas el temporizador con tu tiempo automático, sin abrir la app.\n\n"
                        + "• En Saldo de mis líneas consultas el saldo por línea: Kolbi marca *888# desde el marcador o pide el saldo por SMS al 8888, Liberty/Movistar lo pide por SMS al 606, y siempre puedes abrir el menú de la SIM de tu operador.\n\n"
                        + "• Desliza un mensaje en la bandeja para eliminarlo o archivarlo, como en Gmail. Cada lado se configura por separado.\n\n"
                        + "• Mantén presionado un mensaje para abrirlo, archivarlo o eliminarlo. Los archivados no se borran solos.\n\n"
                        + "• Toca un registro en Eliminados recientemente para recuperarlo a la bandeja o archivarlo antes de que venza.\n\n"
                        + "• Actualizar estado de mensajes programa los entrantes que no tengan cuenta atrás y aplica tus ajustes actuales.\n\n"
                        + "• En Apariencia puedes usar Claro, Oscuro o Automático (sigue el modo del teléfono).\n\n"
                        + "• Puedes tocar Conservar desde la notificación o desde una conversación para evitar que un mensaje se borre.\n\n"
                        + "• Puedes bloquear remitentes desde una conversación. Sus próximos SMS se descartan localmente sin notificación.\n\n"
                        + "• En SIM y envío puedes ver las SIM activas, el número que el operador exponga y elegir la SIM para SMS salientes.\n\n"
                        + "• Compartir envía el texto a otra app, como WhatsApp o correo; esa app puede usar Wi-Fi o datos, pero no convierte el SMS en un SMS por Wi-Fi.\n\n"
                        + "• Si algo falla (notificaciones, borrado), usa Configuración automática: primero desbloquea ajustes restringidos (Android 15/16), luego permisos, notificaciones, alarmas y al final la app predeterminada. El botón de app predeterminada va aparte.\n\n"
                        + "• Esta es una app beta para SMS de texto. Google Mensajes puede seguir mostrando su historial o chats por internet.",
                "Entendido");
    }

    private boolean isDefaultSmsApp() {
        return SetupHelper.isDefaultSms(this);
    }

    private void toggleLock() {
        if (!AppLock.isSet(this)) {
            promptNewPin();
            return;
        }
        AppLock.setEnabled(this, !AppLock.enabled(this));
        Toast.makeText(this, AppLock.enabled(this)
                        ? "Bloqueo activado: se pedirá PIN al abrir."
                        : "Bloqueo desactivado (el PIN queda guardado).",
                Toast.LENGTH_SHORT).show();
        refreshUi();
    }

    /**
     * Fingerprint unlock is its own opt-in: it starts disabled and only turns
     * on after the user confirms with their own finger. Cancelling keeps it off.
     */
    private void toggleBio() {
        if (AppLock.bioEnabled(this)) {
            AppLock.setBio(this, false);
            Toast.makeText(this, "Desbloqueo con huella desactivado.",
                    Toast.LENGTH_SHORT).show();
            refreshUi();
            return;
        }
        boolean shown = AppLock.promptBio(this, "Activar desbloqueo con huella",
                "Confirma con tu huella para encenderlo. Si cancelas, sigue apagado.",
                "Ahora no",
                () -> {
                    AppLock.setBio(this, true);
                    Toast.makeText(this, "Desbloqueo con huella activado ✓",
                            Toast.LENGTH_SHORT).show();
                    refreshUi();
                },
                reason -> {
                    if (reason != null && !reason.isEmpty()) {
                        Toast.makeText(this, "No se activó: " + reason,
                                Toast.LENGTH_LONG).show();
                    }
                });
        if (!shown) {
            Toast.makeText(this,
                    "Este teléfono no ofrece huella (requiere Android 9 o superior).",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void promptNewPin() {
        ThemedDialog.input(this, "PIN de la app",
                "Escribe un PIN de 4 a 10 dígitos. Se pedirá cada vez que abras Fugaz SMS.",
                "", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIANT_PASSWORD,
                null, "Cancelar", "Guardar", null, value -> {
                    if (!AppLock.pinFormatOk(value)) {
                        Toast.makeText(this, "Usa de 4 a 10 dígitos.",
                                Toast.LENGTH_SHORT).show();
                        promptNewPin();
                        return;
                    }
                    AppLock.setPin(this, value);
                    Toast.makeText(this, "PIN guardado. Bloqueo activado ✓",
                            Toast.LENGTH_SHORT).show();
                    refreshUi();
                });
    }

    private void maybeNudge() {
        if (helpPending) return; // don't stack on the guided tour
        SetupHelper.maybeRemindSetup(this, REQUEST_SMS_ROLE);
        SetupHelper.maybeCelebrate(this);
    }

    private int dp(int value) {
        return Ui.dp(this, value);
    }
}
