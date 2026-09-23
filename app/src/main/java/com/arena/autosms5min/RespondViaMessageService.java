package com.arena.autosms5min;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/** Required component for the Android SMS role; this minimal version has no car-mode UI. */
public final class RespondViaMessageService extends Service {
    @Override public IBinder onBind(Intent intent) { return null; }
    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        stopSelf(startId);
        return START_NOT_STICKY;
    }
}
