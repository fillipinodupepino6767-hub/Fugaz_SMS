package com.arena.autosms5min;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Purges expired deletion-log previews even when the inbox has not been opened. */
public final class HistoryExpiryReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!HistoryExpiryScheduler.ACTION_PURGE_HISTORY.equals(intent.getAction())) return;
        DeletionLog.pruneExpired(context);
        HistoryExpiryScheduler.scheduleNext(context);
    }
}
