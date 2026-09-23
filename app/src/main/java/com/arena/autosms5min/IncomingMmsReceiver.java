package com.arena.autosms5min;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Role-eligibility receiver only. This project is intentionally SMS-only and does not
 * download/render MMS payloads. The README makes this limitation explicit.
 */
public final class IncomingMmsReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // No-op: do not claim that this proof-of-concept is a complete MMS client.
    }
}
