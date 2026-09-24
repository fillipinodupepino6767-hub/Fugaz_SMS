package com.arena.autosms5min;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

/** Schedules removal of the oldest short-lived deletion-history entry. */
final class HistoryExpiryScheduler {
    static final String ACTION_PURGE_HISTORY = "com.arena.autosms5min.PURGE_DELETION_HISTORY";
    private static final int REQUEST_CODE = 77431;

    private HistoryExpiryScheduler() { }

    static void scheduleNext(Context context) {
        AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarms == null) return;
        long next = DeletionLog.nextExpiry(context);
        PendingIntent pending = pendingIntent(context, PendingIntent.FLAG_UPDATE_CURRENT);
        if (next == Long.MAX_VALUE) {
            alarms.cancel(pending);
            return;
        }
        // This is intentionally inexact: log expiry is private housekeeping, not an SMS deadline.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, pending);
        } else {
            alarms.set(AlarmManager.RTC_WAKEUP, next, pending);
        }
    }

    private static PendingIntent pendingIntent(Context context, int extraFlags) {
        Intent intent = new Intent(context, HistoryExpiryReceiver.class)
                .setAction(ACTION_PURGE_HISTORY)
                .setData(Uri.parse("autosms5min://history-expiry"));
        return PendingIntent.getBroadcast(context, REQUEST_CODE, intent,
                extraFlags | PendingIntent.FLAG_IMMUTABLE);
    }
}
