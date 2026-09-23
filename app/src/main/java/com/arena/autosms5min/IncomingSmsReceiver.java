package com.arena.autosms5min;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.telephony.SmsMessage;

/** Receives new text SMS only while this app holds Android's SMS role. */
public final class IncomingSmsReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Telephony.Sms.Intents.SMS_DELIVER_ACTION.equals(intent.getAction())) return;

        SmsMessage[] parts = Telephony.Sms.Intents.getMessagesFromIntent(intent);
        if (parts == null || parts.length == 0) return;

        String address = parts[0].getDisplayOriginatingAddress();
        // Blocked senders are deliberately not written to Android's local SMS provider.
        // This prevents both an inbox entry and a notification for future messages from that sender.
        if (Blocklist.isBlocked(context, address)) {
            setResultCode(Activity.RESULT_OK);
            return;
        }

        StringBuilder fullBody = new StringBuilder();
        long timestamp = parts[0].getTimestampMillis();
        for (SmsMessage part : parts) fullBody.append(part.getMessageBody());
        if (timestamp <= 0) timestamp = System.currentTimeMillis();

        long messageId = SmsStore.insertIncoming(context, address, fullBody.toString(), timestamp);
        if (messageId > 0) {
            long retention = AppState.retentionMillis(context);
            if (retention != AppState.NEVER) {
                long dueAt = System.currentTimeMillis() + retention;
                DeleteRegistry.add(context, messageId, dueAt);
                DeleteScheduler.schedule(context, messageId, dueAt);
            }
            NotificationHelper.showIncoming(context, messageId, address, fullBody.toString());
        }
        setResultCode(Activity.RESULT_OK);
    }
}
