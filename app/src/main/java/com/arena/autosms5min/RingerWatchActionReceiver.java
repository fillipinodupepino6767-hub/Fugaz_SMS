package com.arena.autosms5min;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;

/** One-tap actions from the silence-detected prompt notification. */
public final class RingerWatchActionReceiver extends BroadcastReceiver {
    static final String ACTION_ARM_DEFAULT = "com.arena.autosms5min.ARM_RINGER_DEFAULT";
    static final String ACTION_REARM_FROM_NOTE = "com.arena.autosms5min.REARM_FROM_NOTE";
    static final String ACTION_NOTE_DISMISSED = "com.arena.autosms5min.NOTE_DISMISSED";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        if (ACTION_REARM_FROM_NOTE.equals(intent.getAction())) {
            // From the "sonido activado por ti" note: silence the phone now
            // and arm the configured countdown in one tap.
            NotificationHelper.cancelExternalNote(context);
            long restoreAt = RingerTimer.arm(context, AppState.watchDefaultMinutes(context),
                    false, true);
            NotificationHelper.showWatchArmed(context, restoreAt);
            return;
        }
        if (ACTION_NOTE_DISMISSED.equals(intent.getAction())) {
            // Note swiped away: drop the atento state and let the service go.
            RingerTimer.setExternalNoted(context, false);
            if (!AppState.watchRinger(context) && !RingerTimer.isArmed(context)) {
                RingerWatchService.setEnabled(context, false);
            }
            return;
        }
        if (!ACTION_ARM_DEFAULT.equals(intent.getAction())) return;
        NotificationHelper.cancelWatchPrompt(context);
        AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        int mode = audio == null ? AudioManager.RINGER_MODE_NORMAL : audio.getRingerMode();
        boolean ringerQuiet = mode != AudioManager.RINGER_MODE_NORMAL;
        boolean dndOn = RingerTimer.currentZen(context) > 0;
        // Nothing silenced the phone (user already un-silenced): nothing to do.
        if (!ringerQuiet && !dndOn) return;
        // Pure No Molestar keeps the ringer in normal: schedule the restore
        // (lifts DND when policy access is granted) without touching the ringer.
        long restoreAt = RingerTimer.arm(context, AppState.watchDefaultMinutes(context),
                mode == AudioManager.RINGER_MODE_VIBRATE, ringerQuiet);
        NotificationHelper.showWatchArmed(context, restoreAt);
    }
}
