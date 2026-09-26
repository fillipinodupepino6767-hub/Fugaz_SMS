package com.arena.autosms5min;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.role.RoleManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.provider.Telephony;

import java.util.ArrayList;
import java.util.List;

/**
 * One place for every system requirement: SMS role, runtime permissions,
 * notifications and exact alarms (Android 12+). Powers the automatic setup
 * flow and the warning banner on the inbox.
 */
final class SetupHelper {
    static final int REQUEST_RUNTIME = 410;

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

    /** Requests every missing runtime permission in a single system dialog when possible. */
    static void requestMissingRuntimePermissions(Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return;
        List<String> missing = new ArrayList<>();
        // SMS permissions are only useful once the app holds the SMS role.
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

    static void openAppDetails(Activity activity) {
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(Uri.parse("package:" + activity.getPackageName()));
            activity.startActivity(intent);
        } catch (Exception ignored) { }
    }

    /**
     * Step-by-step automatic setup. Runtime permissions are requested directly;
     * system screens (default app, notifications, exact alarms) open on demand.
     */
    static void runAutoSetup(Activity activity, int roleRequestCode) {
        boolean smsMissing = isDefaultSms(activity) && !hasSmsPermissions(activity);
        boolean notifMissing = !hasNotificationPermission(activity);
        boolean contactsMissing = !hasContactsPermission(activity);
        if (smsMissing || notifMissing || contactsMissing) {
            requestMissingRuntimePermissions(activity);
            ThemedDialog.message(activity, "Permisos solicitados",
                    "Acepta los permisos del sistema. Después vuelve a tocar Configuración automática "
                            + "para continuar con los pasos que falten.",
                    "Entendido");
            return;
        }
        if (!isDefaultSms(activity)) {
            ThemedDialog.confirm(activity, "Paso 1: app predeterminada",
                    "Esta app debe ser la aplicación SMS predeterminada para recibir y borrar SMS. "
                            + "¿Quieres configurarla ahora?",
                    "Cancelar", "Continuar", () -> requestSmsRole(activity, roleRequestCode));
            return;
        }
        if (!notificationsEnabled(activity)) {
            ThemedDialog.confirm(activity, "Paso 2: notificaciones",
                    "Las notificaciones están bloqueadas en los ajustes del teléfono. Sin ellas no verás "
                            + "avisos de SMS nuevos ni de eliminados. ¿Abrir los ajustes de notificaciones?",
                    "Cancelar", "Abrir ajustes", () -> openNotificationSettings(activity));
            return;
        }
        if (!canScheduleExactAlarms(activity)) {
            ThemedDialog.confirm(activity, "Paso 3: alarmas exactas",
                    "En Android 12 o superior esta app necesita el permiso de alarmas exactas para borrar "
                            + "cada SMS a su hora. Sin él, el borrado puede retrasarse. ¿Abrir el ajuste?",
                    "Cancelar", "Abrir ajuste", () -> openExactAlarmSettings(activity));
            return;
        }
        ThemedDialog.message(activity, "Todo listo",
                "App predeterminada, permisos, notificaciones y alarmas configurados correctamente.",
                "Entendido");
    }
}
