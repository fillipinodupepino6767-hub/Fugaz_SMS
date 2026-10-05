package com.arena.autosms5min;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;

/** One-tap actions from the silence-detected prompt notification. */
public final class RingerWatchActionReceiver extends BroadcastReceiver {
    static final String ACTION_ARM_DEFAULT = "com.arena.autosms5min.ARM_RINGER_DEFAULT";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !ACTION_ARM_DEFAULT.equals(intent.getAction())) return;
        NotificationHelper.cancelWatchPrompt(context);
        AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        int mode = audio == null ? AudioManager.RINGER_MODE_NORMAL : audio.getRingerMode();
        // The phone is already silent: only schedule the restore, no need to re-silence.
        if (mode == AudioManager.RINGER_MODE_NORMAL) return;
        long restoreAt = RingerTimer.arm(context, AppState.watchDefaultMinutes(context),
                mode == AudioManager.RINGER_MODE_VIBRATE);
        NotificationHelper.showWatchArmed(context, restoreAt);
    }
}
