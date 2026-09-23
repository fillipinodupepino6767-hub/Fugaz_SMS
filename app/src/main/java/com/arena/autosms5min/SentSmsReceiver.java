package com.arena.autosms5min;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Writes a sent item because the app holding the SMS role owns outgoing-message storage. */
public final class SentSmsReceiver extends BroadcastReceiver {
    static final String EXTRA_ADDRESS = "address";
    static final String EXTRA_BODY = "body";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (getResultCode() == Activity.RESULT_OK) {
            SmsStore.insertSent(context, intent.getStringExtra(EXTRA_ADDRESS),
                    intent.getStringExtra(EXTRA_BODY), System.currentTimeMillis());
        }
    }
}
