package com.arena.autosms5min;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

final class DeleteScheduler {
    static final long RETENTION_MS = 5L * 60L * 1000L;
    static final String ACTION_DELETE = "com.arena.autosms5min.DELETE_ONE_SMS";

    private DeleteScheduler() { }

    static void schedule(Context context, long smsId, long dueAtMillis) {
        AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarms == null) return;
        PendingIntent pending = pendingIntent(context, smsId, PendingIntent.FLAG_UPDATE_CURRENT);
        // Android 11 permits exact alarms. Doze may still delay actual receiver execution slightly.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, dueAtMillis, pending);
        } else {
            alarms.setExact(AlarmManager.RTC_WAKEUP, dueAtMillis, pending);
        }
    }

    static void cancel(Context context, long smsId) {
        AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pending = pendingIntent(context, smsId, PendingIntent.FLAG_NO_CREATE);
        if (alarms != null && pending != null) alarms.cancel(pending);
    }

    private static PendingIntent pendingIntent(Context context, long smsId, int extraFlags) {
        Intent intent = new Intent(context, DeleteAlarmReceiver.class)
                .setAction(ACTION_DELETE)
                // Data makes every alarm identity unique even if request codes happen to collide.
                .setData(Uri.parse("autosms5min://delete/" + smsId));
        int flags = extraFlags | PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(context, (int) (smsId ^ (smsId >>> 32)), intent, flags);
    }
}
