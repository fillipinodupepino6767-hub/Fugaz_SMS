package com.arena.autosms5min;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.Map;

/**
 * Exact alarms are cleared by reboot, so restore pending five-minute deletions.
 * ACTION_MY_PACKAGE_REPLACED is handled too: an app update force-stops every
 * process and kills the ringer-watch service, so it must come back by itself.
 */
public final class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent == null ? null : intent.getAction();
        boolean boot = Intent.ACTION_BOOT_COMPLETED.equals(action);
        boolean replaced = Intent.ACTION_MY_PACKAGE_REPLACED.equals(action);
        if (!boot && !replaced) return;
        long now = System.currentTimeMillis();
        for (Map.Entry<Long, Long> entry : DeleteRegistry.all(context).entrySet()) {
            DeleteScheduler.schedule(context, entry.getKey(), Math.max(now + 1_000L, entry.getValue()));
        }
        DeletionLog.pruneExpired(context);
        HistoryExpiryScheduler.scheduleNext(context);
        RingerTimer.rescheduleAfterBoot(context);
        RingerWatchService.ensureRunning(context);
    }
}
