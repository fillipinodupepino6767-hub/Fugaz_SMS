package com.arena.autosms5min;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ServiceInfo;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;

/**
 * Watches the ringer mode while enabled. Android 8+ does not deliver
 * RINGER_MODE_CHANGED to manifest receivers, so this foreground service
 * registers it dynamically. On a real normal -> silent/vibrate transition
 * (Volume Styles, system buttons, any app) it posts a prompt notification;
 * the user arms the timer with one tap. Nothing is ever armed by itself.
 *
 * Do Not Disturb (No Molestar) does NOT change the ringer mode, so Android
 * never broadcasts it: the service also observes the Settings "zen_mode"
 * value and prompts when DND turns on. The ringer receiver is flagged
 * RECEIVER_EXPORTED because Android requires that for system broadcasts
 * (NOT_EXPORTED silently drops them on many devices); RINGER_MODE_CHANGED
 * is a protected broadcast, so only the system can send it.
 *
 * On Android 14+ the service uses the specialUse FGS type (declared in the
 * manifest with a subtype explaining the user-opted ringer watchdog).
 */
public final class RingerWatchService extends Service {
    private static final int FOREGROUND_ID = 610026;
    /** Our own arm/restore mode changes are ignored inside this window. */
    private static final long SELF_CHANGE_WINDOW_MS = 15_000L;
    /** True while the service instance is alive (survives in-process restarts). */
    private static volatile boolean running;
    private boolean registered;
    private ContentObserver zenObserver;

    private final BroadcastReceiver ringerChanged = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            onRingerModeChanged(context);
        }
    };

    /** Restart after process/service death without spamming start commands. */
    static void ensureRunning(Context context) {
        if (!AppState.watchRinger(context) && !RingerTimer.isArmed(context)) return;
        if (running) return;
        setEnabled(context, true);
    }

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
        running = true;
        promoteToForeground();
        RingerTimer.setLastSeenMode(this, currentMode());
        RingerTimer.setLastSeenZen(this, RingerTimer.currentZen(this));
        if (!registered) {
            IntentFilter filter = new IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION);
            if (Build.VERSION.SDK_INT >= 33) {
                // System broadcasts need RECEIVER_EXPORTED (see class docs).
                registerReceiver(ringerChanged, filter, Context.RECEIVER_EXPORTED);
            } else {
                registerReceiver(ringerChanged, filter);
            }
            registered = true;
        }
        // No Molestar never changes the ringer mode: watch zen_mode directly.
        zenObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {
            @Override public void onChange(boolean selfChange, Uri uri) {
                onZenChanged(RingerWatchService.this);
            }
        };
        try {
            getContentResolver().registerContentObserver(
                    Settings.Global.getUriFor("zen_mode"), false, zenObserver);
        } catch (Exception ignored) {
            zenObserver = null; // Rare OEM restriction; ringer watch still works.
        }
    }

    private void promoteToForeground() {
        android.app.Notification notification =
                NotificationHelper.watchServiceNotification(this);
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(FOREGROUND_ID, notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            } else {
                startForeground(FOREGROUND_ID, notification);
            }
        } catch (Exception failed) {
            // Last-resort: try without the typed overload so older ROMs still work.
            try {
                startForeground(FOREGROUND_ID, notification);
            } catch (Exception ignored) { }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Runs with the optional watch on, or while a countdown needs watching.
        if (!AppState.watchRinger(this) && !RingerTimer.isArmed(this)) {
            stopSelf();
            return START_NOT_STICKY;
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        running = false;
        if (registered) {
            try {
                unregisterReceiver(ringerChanged);
            } catch (Exception ignored) {
                // Already unregistered.
            }
            registered = false;
        }
        if (zenObserver != null) {
            try {
                getContentResolver().unregisterContentObserver(zenObserver);
            } catch (Exception ignored) {
                // Already unregistered.
            }
            zenObserver = null;
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
        AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        int mode = audio == null ? AudioManager.RINGER_MODE_NORMAL : audio.getRingerMode();
        int last = RingerTimer.lastSeenMode(context);
        RingerTimer.setLastSeenMode(context, mode);
        // Countdown running: sound back on by hand → note it once and keep
        // waiting (the restore at the end becomes a no-op if still active).
        if (RingerTimer.isArmed(context)) {
            if (mode == AudioManager.RINGER_MODE_NORMAL) {
                NotificationHelper.cancelWatchPrompt(context);
                if (RingerTimer.currentZen(context) == 0) {
                    noteExternalRestore(context);
                }
            }
            return;
        }
        if (!AppState.watchRinger(context)) return;
        // Only fresh normal -> silent/vibrate transitions prompt. Returning to
        // normal dismisses any pending prompt instead of nagging.
        if (mode == AudioManager.RINGER_MODE_NORMAL) {
            NotificationHelper.cancelWatchPrompt(context);
            return;
        }
        if (last != AudioManager.RINGER_MODE_NORMAL) return;
        if (System.currentTimeMillis() - RingerTimer.selfChangeAt(context) < SELF_CHANGE_WINDOW_MS) {
            return;
        }
        NotificationHelper.showRingerWatchPrompt(context,
                mode == AudioManager.RINGER_MODE_VIBRATE);
    }

    /** Rising edge of No Molestar (zen 0 -> on) prompts, same as silent mode. */
    static void onZenChanged(Context context) {
        int zen = RingerTimer.currentZen(context);
        int last = RingerTimer.lastSeenZen(context);
        RingerTimer.setLastSeenZen(context, zen);
        boolean armed = RingerTimer.isArmed(context);
        boolean watch = AppState.watchRinger(context);
        if (!armed && !watch) return;
        if (zen == 0) {
            NotificationHelper.cancelWatchPrompt(context);
            // DND lifted by hand while counting down: same external note.
            if (armed && RingerTimer.soundActive(context)) {
                noteExternalRestore(context);
            }
            return;
        }
        if (!watch || last != 0 || armed) return;
        NotificationHelper.showDndWatchPrompt(context);
    }

    /** "Seems you turned the sound back on yourself; waiting for you to stop it." */
    static void noteExternalRestore(Context context) {
        if (RingerTimer.externalNoted(context)) return;
        RingerTimer.setExternalNoted(context, true);
        NotificationHelper.showExternalRestoreNote(context);
    }
}
