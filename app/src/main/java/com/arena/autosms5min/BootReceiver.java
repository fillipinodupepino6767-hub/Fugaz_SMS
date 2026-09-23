package com.arena.autosms5min;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.Map;

/** Exact alarms are cleared by reboot, so restore pending five-minute deletions. */
public final class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;
        long now = System.currentTimeMillis();
        for (Map.Entry<Long, Long> entry : DeleteRegistry.all(context).entrySet()) {
            DeleteScheduler.schedule(context, entry.getKey(), Math.max(now + 1_000L, entry.getValue()));
        }
    }
}
