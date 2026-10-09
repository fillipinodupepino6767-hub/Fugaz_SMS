package com.arena.autosms5min;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;

/**
 * Silence timer: the user silences the phone for a chosen time (school, meetings)
 * and the ringer comes back automatically, so messages and alarms are heard again.
 * Only the countdown state lives here; RingerRestoreReceiver does the restore.
 */
final class RingerTimer {
    private static final String PREFS = "ringer_timer";
    private static final String KEY_RESTORE_AT = "restore_at";
    private static final String KEY_SELF_AT = "self_change_at";
    private static final String KEY_LAST_MODE = "last_seen_mode";
    private static final String KEY_LAST_ZEN = "last_seen_zen";
    private static final int REQUEST_CODE = 61024;
    static final long MIN_MINUTES = 1L;
    static final long MAX_MINUTES = 720L; // 12 hours

    private RingerTimer() { }

    /** Silences now (vibrate or full silent) and restores the sound after the minutes. */
    static long arm(Context context, long minutes, boolean vibrate) {
        return arm(context, minutes, vibrate, true);
    }

    /**
     * @param changeRinger false when only Do Not Disturb silenced the phone:
     *                     schedule the restore without forcing the ringer.
     */
    static long arm(Context context, long minutes, boolean vibrate, boolean changeRinger) {
        long safeMinutes = Math.max(MIN_MINUTES, Math.min(MAX_MINUTES, minutes));
        long restoreAt = System.currentTimeMillis() + safeMinutes * 60_000L;
        AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (changeRinger && audio != null) {
            try {
                audio.setRingerMode(vibrate ? AudioManager.RINGER_MODE_VIBRATE
                        : AudioManager.RINGER_MODE_SILENT);
            } catch (SecurityException ignored) {
                // The timer is still armed; the user silences manually with the buttons.
            }
        }
        prefs(context).edit().putLong(KEY_RESTORE_AT, restoreAt).apply();
        noteSelfChange(context);
        schedule(context, restoreAt);
        return restoreAt;
    }

    /** Current Do Not Disturb (zen) value: 0 = off, >0 = on. Read needs no permission. */
    static int currentZen(Context context) {
        try {
            return android.provider.Settings.Global.getInt(
                    context.getContentResolver(), "zen_mode");
        } catch (Exception ignored) {
            return 0; // Missing or OEM-restricted: treat as off.
        }
    }

    static int lastSeenZen(Context context) {
        return prefs(context).getInt(KEY_LAST_ZEN, 0);
    }

    static void setLastSeenZen(Context context, int zen) {
        prefs(context).edit().putInt(KEY_LAST_ZEN, zen).apply();
    }

    /** Stops the timer. The phone stays as it is; the user un-silences manually. */
    static void cancel(Context context) {
        prefs(context).edit().remove(KEY_RESTORE_AT).apply();
        AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pending = pendingIntent(context, PendingIntent.FLAG_NO_CREATE);
        if (alarms != null && pending != null) {
            alarms.cancel(pending);
            pending.cancel();
        }
    }

    /** Restore time, or 0 when no timer is armed. Expired timers clean themselves. */
    static long restoreAt(Context context) {
        long at = prefs(context).getLong(KEY_RESTORE_AT, 0L);
        if (at <= 0L) return 0L;
        if (at <= System.currentTimeMillis()) {
            prefs(context).edit().remove(KEY_RESTORE_AT).apply();
            return 0L;
        }
        return at;
    }

    static boolean isArmed(Context context) {
        return restoreAt(context) > 0L;
    }

    /** Re-registers the alarm after reboot when the timer is still in the future. */
    static void rescheduleAfterBoot(Context context) {
        long at = prefs(context).getLong(KEY_RESTORE_AT, 0L);
        if (at > System.currentTimeMillis()) {
            schedule(context, at);
        } else if (at > 0L) {
            prefs(context).edit().remove(KEY_RESTORE_AT).apply();
        }
    }

    static String currentModeLabel(Context context) {
        AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audio == null) return "desconocido";
        switch (audio.getRingerMode()) {
            case AudioManager.RINGER_MODE_NORMAL: return "Sonido activado 🔊";
            case AudioManager.RINGER_MODE_VIBRATE: return "Vibración 📳";
            default: return "Silencio 🔇";
        }
    }

    /**
     * Marks our own arm/restore mode changes so the ringer watch does not
     * prompt for changes this app made itself.
     */
    static void noteSelfChange(Context context) {
        prefs(context).edit().putLong(KEY_SELF_AT, System.currentTimeMillis()).apply();
    }

    static long selfChangeAt(Context context) {
        return prefs(context).getLong(KEY_SELF_AT, 0L);
    }

    /** Last ringer mode seen by the watch; -1 means unknown. */
    static int lastSeenMode(Context context) {
        return prefs(context).getInt(KEY_LAST_MODE, -1);
    }

    static void setLastSeenMode(Context context, int mode) {
        prefs(context).edit().putInt(KEY_LAST_MODE, mode).apply();
    }

    private static void schedule(Context context, long restoreAt) {
        AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarms == null) return;
        PendingIntent pending = pendingIntent(context, PendingIntent.FLAG_UPDATE_CURRENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !canScheduleExact(alarms)) {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, restoreAt, pending);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, restoreAt, pending);
            } catch (SecurityException denied) {
                alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, restoreAt, pending);
            }
        } else {
            alarms.setExact(AlarmManager.RTC_WAKEUP, restoreAt, pending);
        }
    }

    private static boolean canScheduleExact(AlarmManager alarms) {
        try {
            return alarms.canScheduleExactAlarms();
        } catch (Exception ignored) {
            return false;
        }
    }

    private static PendingIntent pendingIntent(Context context, int extraFlags) {
        Intent intent = new Intent(context, RingerRestoreReceiver.class)
                .setAction(RingerRestoreReceiver.ACTION_RESTORE)
                .setData(Uri.parse("autosms5min://ringer-restore"));
        return PendingIntent.getBroadcast(context, REQUEST_CODE, intent,
                extraFlags | PendingIntent.FLAG_IMMUTABLE);
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
