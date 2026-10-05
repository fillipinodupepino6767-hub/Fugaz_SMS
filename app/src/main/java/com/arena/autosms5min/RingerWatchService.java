package com.arena.autosms5min;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Build;
import android.os.IBinder;

/**
 * Watches the ringer mode while enabled. Android 8+ does not deliver
 * RINGER_MODE_CHANGED to manifest receivers, so this foreground service
 * registers it dynamically. On a real normal -> silent/vibrate transition
 * (Volume Styles, system buttons, any app) it posts a prompt notification;
 * the user arms the timer with one tap. Nothing is ever armed by itself.
 */
public final class RingerWatchService extends Service {
    private static final int FOREGROUND_ID = 610026;
    /** Our own arm/restore mode changes are ignored inside this window. */
    private static final long SELF_CHANGE_WINDOW_MS = 15_000L;
    private boolean registered;
    private final BroadcastReceiver ringerChanged = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            onRingerModeChanged(context);
        }
    };

    static void setEnabled(Context context, boolean enabled) {
        Intent intent = new Intent(context, RingerWatchService.class);
        try {
            if (enabled) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent);
                } else {
                    context.startService(intent);
                }
            } else {
                context.stopService(intent);
            }
        } catch (Exception ignored) {
            // If the system refuses the start, the toggle simply has no effect.
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        startForeground(FOREGROUND_ID, NotificationHelper.watchServiceNotification(this));
        RingerTimer.setLastSeenMode(this, currentMode());
        if (!registered) {
            registerReceiver(ringerChanged,
                    new IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION));
            registered = true;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!AppState.watchRinger(this)) {
            stopSelf();
            return START_NOT_STICKY;
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (registered) {
            try {
                unregisterReceiver(ringerChanged);
            } catch (Exception ignored) {
                // Already unregistered.
            }
            registered = false;
        }
        NotificationHelper.cancelWatchPrompt(this);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private int currentMode() {
        AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        return audio == null ? AudioManager.RINGER_MODE_NORMAL : audio.getRingerMode();
    }

    static void onRingerModeChanged(Context context) {
        if (!AppState.watchRinger(context)) return;
        AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        int mode = audio == null ? AudioManager.RINGER_MODE_NORMAL : audio.getRingerMode();
        int last = RingerTimer.lastSeenMode(context);
        RingerTimer.setLastSeenMode(context, mode);
        // Only fresh normal -> silent/vibrate transitions prompt. Returning to
        // normal dismisses any pending prompt instead of nagging.
        if (mode == AudioManager.RINGER_MODE_NORMAL) {
            NotificationHelper.cancelWatchPrompt(context);
            return;
        }
        if (last != AudioManager.RINGER_MODE_NORMAL) return;
        if (RingerTimer.isArmed(context)) return;
        if (System.currentTimeMillis() - RingerTimer.selfChangeAt(context) < SELF_CHANGE_WINDOW_MS) {
            return;
        }
        NotificationHelper.showRingerWatchPrompt(context,
                mode == AudioManager.RINGER_MODE_VIBRATE);
    }
}
