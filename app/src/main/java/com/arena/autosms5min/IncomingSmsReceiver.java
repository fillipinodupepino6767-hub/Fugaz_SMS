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
        StringBuilder fullBody = new StringBuilder();
        long timestamp = parts[0].getTimestampMillis();
        for (SmsMessage part : parts) fullBody.append(part.getMessageBody());
        if (timestamp <= 0) timestamp = System.currentTimeMillis();

        // Blocked senders are not written to Android's SMS provider or notified.
        // A 24-hour safety log preserves only a short preview so the discard is auditable.
        if (Blocklist.isBlocked(context, address)) {
            DeletionLog.add(context, address, fullBody.toString(), "Bloqueado y descartado");
            setResultCode(Activity.RESULT_OK);
            return;
        }

        long messageId = SmsStore.insertIncoming(context, address, fullBody.toString(), timestamp);
        if (messageId > 0) {
            MessageClassifier.Result classification =
                    MessageClassifier.classify(fullBody.toString());
            long retention = AppState.retentionMillis(context);
            if (retention != AppState.NEVER && shouldAutoDelete(context, classification.label)) {
                long dueAt = System.currentTimeMillis() + retention;
                DeleteRegistry.add(context, messageId, dueAt);
                DeleteScheduler.schedule(context, messageId, dueAt);
            }
            NotificationHelper.showIncoming(context, messageId, address, fullBody.toString());
        }
        setResultCode(Activity.RESULT_OK);
    }

    private static boolean shouldAutoDelete(Context context, String label) {
        if (MessageClassifier.LABEL_IMPORTANT.equals(label)) {
            return AppState.shouldDeleteImportant(context);
        }
        if (MessageClassifier.LABEL_SPAM.equals(label)) {
            return AppState.shouldDeleteSpam(context);
        }
        return AppState.shouldDeleteNormal(context);
    }
}
