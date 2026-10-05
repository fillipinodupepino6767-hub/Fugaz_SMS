package com.arena.autosms5min;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;

/** Fired by the silence timer: turns the ringer back on and tells the user. */
public final class RingerRestoreReceiver extends BroadcastReceiver {
    static final String ACTION_RESTORE = "com.arena.autosms5min.RESTORE_RINGER";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !ACTION_RESTORE.equals(intent.getAction())) return;
        RingerTimer.cancel(context); // Clears state first; this fire is one-shot.
        RingerTimer.noteSelfChange(context); // The watch must ignore our own restore.
        AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audio != null) {
            try {
                audio.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
            } catch (SecurityException ignored) {
                // Nothing else we can do without audio settings access.
            }
        }
        // If the silence came from Do Not Disturb, the ringer mode alone is not enough.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            NotificationManager notifications =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            try {
                if (notifications != null && notifications.isNotificationPolicyAccessGranted()) {
                    notifications.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL);
                }
            } catch (SecurityException ignored) {
                // Access was revoked; the ringer mode above is the best we can do.
            }
        }
        NotificationHelper.showRingerRestored(context);
    }
}
