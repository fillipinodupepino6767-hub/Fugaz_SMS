package com.arena.autosms5min;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Handles the Conservar button directly from an incoming-SMS notification. */
public final class KeepSmsReceiver extends BroadcastReceiver {
    static final String ACTION_KEEP = "com.arena.autosms5min.KEEP_ONE_SMS";
    static final String EXTRA_SMS_ID = "sms_id";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!ACTION_KEEP.equals(intent.getAction())) return;
        long smsId = intent.getLongExtra(EXTRA_SMS_ID, -1L);
        if (smsId < 0L) return;
        DeleteScheduler.cancel(context, smsId);
        DeleteRegistry.remove(context, smsId);
        NotificationHelper.cancel(context, smsId);
    }
}
