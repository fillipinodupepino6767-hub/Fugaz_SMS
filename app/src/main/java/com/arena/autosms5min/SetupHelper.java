package com.arena.autosms5min;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.role.RoleManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.provider.Telephony;

import java.util.ArrayList;
import java.util.List;

/**
 * One place for every system requirement: restricted settings (Android 13–16
 * sideload unlock), runtime permissions, SMS role, notifications and exact
 * alarms. Powers the automatic setup flow and the warning banner on the inbox.
 *
 * Order matters on Android 15/16 for sideloaded APKs: Allow restricted settings
 * first, then runtime permissions, then default SMS app, then the rest.
 */
final class SetupHelper {
    static final int REQUEST_RUNTIME = 410;
    private static final String PREFS = "setup_helper";
    private static final String KEY_PENDING = "auto_setup_pending";
    private static final String KEY_RESTRICTED_DONE = "restricted_step_done";
    private static final String KEY_RESTRICTED_AT = "restricted_opened_at";
    private static final String KEY_SETUP_STARTED_AT = "setup_started_at";
    private static final String KEY_SETUP_REMINDED = "setup_reminded";
    private static final String KEY_SETUP_CELEBRATED = "setup_celebrated";
    private static final long REMIND_AFTER_MS = 3L * 24L * 60L * 60L * 1000L;
    private static long lastContinueAt;

    private SetupHelper() { }

    /** True if this app holds the SMS role (RoleManager) or is the default SMS package. */
    static boolean isDefaultSms(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                RoleManager roles = context.getSystemService(RoleManager.class);
                if (roles != null && roles.isRoleAvailable(RoleManager.ROLE_SMS)
                        && roles.isRoleHeld(RoleManager.ROLE_SMS)) {
                    return true;
                }
            } catch (Exception ignored) {
                // Fall through to the Telephony check below.
            }
        }
        return context.getPackageName().equals(Telephony.Sms.getDefaultSmsPackage(context));
    }

    static boolean hasSmsPermissions(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true;
        return context.checkSelfPermission(Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
                && context.checkSelfPermission(Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
                && context.checkSelfPermission(Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED;
    }

    static boolean hasNotificationPermission(Context context) {
        if (Build.VERSION.SDK_INT < 33) return true;
        return context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    /** Optional cosmetic permission: contact names and photos instead of raw numbers. */
    static boolean hasContactsPermission(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true;
        return context.checkSelfPermission(Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_GRANTED;
    }

    /** Lets the silence timer also lift Do Not Disturb when restoring the sound. */
    static boolean hasDndAccess(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true;
        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        try {
            return manager != null && manager.isNotificationPolicyAccessGranted();
        } catch (Exception ignored) {
            return false;
        }
    }

    static boolean notificationsEnabled(Context context) {
        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        return manager != null && manager.areNotificationsEnabled();
    }

    static boolean needsExactAlarmCheck() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S;
    }

    static boolean canScheduleExactAlarms(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true;
        AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        try {
            return alarms != null && alarms.canScheduleExactAlarms();
        } catch (Exception ignored) {
            return false;
        }
    }

    /** Short label for the inbox banner, or null when everything is configured. */
    static String bannerText(Context context) {
        if (!isDefaultSms(context)) return "No es la app SMS predeterminada.";
        if (!hasSmsPermissions(context)) return "Faltan permisos de SMS.";
        if (!notificationsEnabled(context)) return "Notificaciones bloqueadas.";
        if (!canScheduleExactAlarms(context)) return "Permiso de alarmas exactas pendiente.";
        return null;
    }

    /** True when auto-setup left the app to a system screen and should resume on return. */
    static boolean isAutoSetupPending(Context context) {
        return setupPrefs(context).getBoolean(KEY_PENDING, false);
    }

    static void clearAutoSetupPending(Context context) {
        setupPrefs(context).edit().putBoolean(KEY_PENDING, false).apply();
    }

    private static void markPending(Context context) {
        setupPrefs(context).edit().putBoolean(KEY_PENDING, true).apply();
    }

    private static boolean restrictedStepDone(Context context) {
        return setupPrefs(context).getBoolean(KEY_RESTRICTED_DONE, false);
    }

    private static void markRestrictedDone(Context context) {
        setupPrefs(context).edit()
                .putBoolean(KEY_RESTRICTED_DONE, true)
                .putLong(KEY_RESTRICTED_AT, System.currentTimeMillis())
                .apply();
    }

    /**
     * Android 13+ can lock sensitive toggles (SMS on 15/16 sideload) behind
     * "Allow restricted settings". There is no public API to detect or open
     * that toggle directly — only App info, where the user taps ⋮.
     */
    private static boolean needsRestrictedSettingsStep(Context context) {
        if (Build.VERSION.SDK_INT < 33) return false;
        if (restrictedStepDone(context)) return false;
        // Needed when SMS is not fully ready yet (role or permissions).
        return !isDefaultSms(context) || !hasSmsPermissions(context);
    }

    /** Requests every missing runtime permission in a single system dialog when possible. */
    static void requestMissingRuntimePermissions(Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return;
        List<String> missing = new ArrayList<>();
        // SMS permissions only make sense once the app holds the SMS role; on
        // Android 15/16 sideload they also need restricted settings unlocked first.
        if (isDefaultSms(activity) && !hasSmsPermissions(activity)) {
            if (activity.checkSelfPermission(Manifest.permission.RECEIVE_SMS)
                    != PackageManager.PERMISSION_GRANTED) {
                missing.add(Manifest.permission.RECEIVE_SMS);
            }
            if (activity.checkSelfPermission(Manifest.permission.READ_SMS)
                    != PackageManager.PERMISSION_GRANTED) {
                missing.add(Manifest.permission.READ_SMS);
            }
            if (activity.checkSelfPermission(Manifest.permission.SEND_SMS)
                    != PackageManager.PERMISSION_GRANTED) {
                missing.add(Manifest.permission.SEND_SMS);
            }
        }
        if (Build.VERSION.SDK_INT >= 33 && !hasNotificationPermission(activity)) {
            missing.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        // Contacts are optional and independent of the SMS role; ask once alongside the rest.
        if (!hasContactsPermission(activity)) {
            missing.add(Manifest.permission.READ_CONTACTS);
        }
        if (!missing.isEmpty()) {
            activity.requestPermissions(missing.toArray(new String[0]), REQUEST_RUNTIME);
        }
    }

    static void requestSmsRole(Activity activity, int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            RoleManager roles = activity.getSystemService(RoleManager.class);
            if (roles != null && roles.isRoleAvailable(RoleManager.ROLE_SMS)) {
                activity.startActivityForResult(
                        roles.createRequestRoleIntent(RoleManager.ROLE_SMS), requestCode);
                return;
            }
        }
        Intent change = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
        change.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, activity.getPackageName());
        activity.startActivityForResult(change, requestCode);
    }

    static void openNotificationSettings(Activity activity) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, activity.getPackageName());
                activity.startActivity(intent);
            } else {
                openAppDetails(activity);
            }
        } catch (Exception ignored) {
            openAppDetails(activity);
        }
    }

    static void openExactAlarmSettings(Activity activity) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                        .setData(Uri.parse("package:" + activity.getPackageName()));
                activity.startActivity(intent);
            }
        } catch (Exception ignored) {
            // Some builds hide this screen; the scheduler falls back to inexact alarms.
        }
    }

    static void openDndSettings(Activity activity) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                activity.startActivity(new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS));
            }
        } catch (Exception ignored) {
            openAppDetails(activity);
        }
    }

    static void openAppDetails(Activity activity) {
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(Uri.parse("package:" + activity.getPackageName()));
            activity.startActivity(intent);
        } catch (Exception ignored) { }
    }

    /**
     * Opens App info so the user can unlock restricted settings (⋮ menu).
     * On Motorola the ⋮ sometimes appears only after opening Permissions.
     */
    static void openRestrictedSettingsGuide(Activity activity) {
        markRestrictedDone(activity);
        markPending(activity);
        openAppDetails(activity);
    }

    /**
     * Resume after returning from a system screen during automatic setup.
     * Call from Activity.onResume when {@link #isAutoSetupPending} is true.
     */
    static void continueAutoSetupIfPending(Activity activity, int roleRequestCode) {
        if (!isAutoSetupPending(activity)) return;
        long now = System.currentTimeMillis();
        if (now - lastContinueAt < 800L) return; // avoid double fire from resume + result
        lastContinueAt = now;
        clearAutoSetupPending(activity);
        runAutoSetup(activity, roleRequestCode, true);
    }

    /**
     * Step-by-step automatic setup in the order Android 15/16 needs for
     * sideloaded SMS apps:
     * 1) Allow restricted settings (App info → ⋮)
     * 2) Runtime permissions (notifications, contacts; SMS after default)
     * 3) Notifications channel access
     * 4) Exact alarms
     * 5) Default SMS app (last — separate button also available)
     *
     * @param resuming true when continuing after a system screen, so we skip
     *                 re-showing the restricted-settings intro if already done.
     */
    static void runAutoSetup(Activity activity, int roleRequestCode) {
        runAutoSetup(activity, roleRequestCode, false);
    }

    static void runAutoSetup(Activity activity, int roleRequestCode, boolean resuming) {
        // Step 1 — restricted settings unlock (critical on Android 15/16 sideload).
        if (needsRestrictedSettingsStep(activity)) {
            String body = "En Android 13 a 16, las apps instaladas fuera de Play Store "
                    + "bloquean permisos sensibles (SMS) hasta que tú los desbloquees.\n\n"
                    + "En la pantalla que se abrirá:\n"
                    + "1. Toca el menú ⋮ (arriba a la derecha). En Motorola a veces "
                    + "aparece al entrar primero en Permisos.\n"
                    + "2. Toca «Permitir ajustes restringidos».\n"
                    + "3. Confirma con PIN, patrón o huella.\n"
                    + "4. Vuelve a Fugaz SMS: la configuración automática continuará sola.\n\n"
                    + "Si no ves esa opción, la app ya está desbloqueada o tu Android no la pide: "
                    + "vuelve y toca de nuevo Configuración automática.";
            ThemedDialog.confirm(activity, "Paso 1: ajustes restringidos",
                    body, "Ahora no", "Abrir info de la app",
                    () -> openRestrictedSettingsGuide(activity));
            return;
        }

        // Step 2 — runtime permissions we can request without being default SMS.
        boolean notifMissing = !hasNotificationPermission(activity);
        boolean contactsMissing = !hasContactsPermission(activity);
        boolean smsMissing = isDefaultSms(activity) && !hasSmsPermissions(activity);
        if (notifMissing || contactsMissing || smsMissing) {
            requestMissingRuntimePermissions(activity);
            markPending(activity);
            ThemedDialog.message(activity, "Paso 2: permisos",
                    "Acepta los permisos del sistema (notificaciones, contactos"
                            + (smsMissing ? " y SMS" : "")
                            + "). Al volver, la configuración automática sigue con lo que falte.",
                    "Entendido");
            return;
        }

        // Step 3 — notifications blocked at channel / app level.
        if (!notificationsEnabled(activity)) {
            markPending(activity);
            ThemedDialog.confirm(activity, "Paso 3: notificaciones",
                    "Las notificaciones están bloqueadas en los ajustes del teléfono. Sin ellas no verás "
                            + "avisos de SMS nuevos ni de eliminados. ¿Abrir los ajustes de notificaciones?",
                    "Cancelar", "Abrir ajustes", () -> {
                        markPending(activity);
                        openNotificationSettings(activity);
                    });
            return;
        }

        // Step 4 — exact alarms (Android 12+).
        if (!canScheduleExactAlarms(activity)) {
            markPending(activity);
            ThemedDialog.confirm(activity, "Paso 4: alarmas exactas",
                    "En Android 12 o superior esta app necesita el permiso de alarmas exactas para borrar "
                            + "cada SMS a su hora. Sin él, el borrado puede retrasarse. ¿Abrir el ajuste?",
                    "Cancelar", "Abrir ajuste", () -> {
                        markPending(activity);
                        openExactAlarmSettings(activity);
                    });
            return;
        }

        // Step 5 — default SMS app last, so restricted settings + other perms
        // are already in place and the role request is more likely to stick.
        if (!isDefaultSms(activity)) {
            ThemedDialog.confirm(activity, "Paso 5: app SMS predeterminada",
                    "Último paso: esta app debe ser la aplicación SMS predeterminada para recibir y borrar SMS. "
                            + "También puedes hacerlo después con el botón aparte «Configurar como app SMS predeterminada». "
                            + "¿Configurarla ahora?",
                    "Después", "Configurar ahora", () -> {
                        markPending(activity);
                        requestSmsRole(activity, roleRequestCode);
                    });
            return;
        }

        // Default is set but SMS runtime perms may still be missing (first grant after role).
        if (!hasSmsPermissions(activity)) {
            requestMissingRuntimePermissions(activity);
            markPending(activity);
            ThemedDialog.message(activity, "Permisos de SMS",
                    "Acepta los permisos de SMS. Si aparecen en gris, vuelve a Configuración automática "
                            + "y repite el paso de ajustes restringidos (menú ⋮ en la info de la app).",
                    "Entendido");
            return;
        }

        clearAutoSetupPending(activity);
        celebrate(activity);
    }

    /** One-time celebration when everything is configured (setup flow or later). */
    static void celebrate(final Activity activity) {
        setupPrefs(activity).edit()
                .putBoolean(KEY_SETUP_CELEBRATED, true)
                .putBoolean(KEY_SETUP_REMINDED, true)
                .apply();
        ThemedDialog.confirm(activity, "\uD83C\uDF89 Todo listo",
                "Ajustes restringidos, permisos, notificaciones, alarmas y app predeterminada "
                        + "est\u00e1n configurados correctamente.\n\n\u00bfQuieres comprobar "
                        + "que el aviso de sonido suena?",
                "Cerrar", "Probar sonido",
                () -> NotificationHelper.showTest(activity));
    }

    /**
     * Quiet one-time nudge shown 3 days after the first open while setup is
     * still incomplete. Never stacks on other dialogs; skipped forever once
     * everything is configured.
     */
    static void maybeRemindSetup(Activity activity, int roleRequestCode) {
        SharedPreferences prefs = setupPrefs(activity);
        if (prefs.getBoolean(KEY_SETUP_REMINDED, false)) return;
        if (bannerText(activity) == null) {
            prefs.edit().putBoolean(KEY_SETUP_REMINDED, true).apply();
            return;
        }
        long started = prefs.getLong(KEY_SETUP_STARTED_AT, 0L);
        long now = System.currentTimeMillis();
        if (started == 0L) {
            prefs.edit().putLong(KEY_SETUP_STARTED_AT, now).apply();
            return;
        }
        if (now - started < REMIND_AFTER_MS) return;
        if (ThemedDialog.activeCount() > 0) return; // try again at a quiet moment
        prefs.edit().putBoolean(KEY_SETUP_REMINDED, true).apply();
        String falta = bannerText(activity);
        ThemedDialog.confirm(activity, "\uD83D\uDC4B \u00a1Sigues sin configurar!",
                "Pasaron 3 d\u00edas desde tu primera vez en Fugaz SMS y a\u00fan falta:\n\n\u2022 "
                        + falta + "\n\nToca \u00abConfigurar ahora\u00bb e inicia con ① "
                        + "(autom\u00e1tico); la app predeterminada es ②.",
                "Despu\u00e9s", "Configurar ahora",
                () -> runAutoSetup(activity, roleRequestCode));
    }

    /** One-time \uD83C\uDF89 when setup is complete but never celebrated yet. */
    static void maybeCelebrate(Activity activity) {
        SharedPreferences prefs = setupPrefs(activity);
        if (prefs.getBoolean(KEY_SETUP_CELEBRATED, false)) return;
        if (bannerText(activity) != null) return;
        if (ThemedDialog.activeCount() > 0) return; // no stacking with welcome/tour
        celebrate(activity);
    }

    /** First open timestamp used by the 3-day reminder. */
    static void noteFirstOpen(Context context) {
        SharedPreferences prefs = setupPrefs(context);
        if (!prefs.contains(KEY_SETUP_STARTED_AT)) {
            prefs.edit().putLong(KEY_SETUP_STARTED_AT, System.currentTimeMillis()).apply();
        }
    }

    private static SharedPreferences setupPrefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
