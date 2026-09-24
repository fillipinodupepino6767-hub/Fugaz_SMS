package com.arena.autosms5min;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

/** Deletes exactly the SMS ID scheduled by IncomingSmsReceiver. */
public final class DeleteAlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Uri data = intent.getData();
        if (data == null) return;
        long id;
        try {
            id = Long.parseLong(data.getLastPathSegment());
        } catch (NumberFormatException error) {
            return;
        }

        long dueAt = DeleteRegistry.dueAt(context, id);
        if (dueAt < 0) return; // It was already deleted or the schedule was cancelled.
        if (System.currentTimeMillis() < dueAt) {
            DeleteScheduler.schedule(context, id, dueAt); // wall clock changed backwards
            return;
        }

        SmsStore.SmsItem message = SmsStore.messageById(context, id);
        // The user may have switched this type to "keep" after the alarm was set.
        if (message != null && !MessageMaintenance.wantsAutoDelete(context,
                MessageClassifier.classify(message.body).label)) {
            DeleteRegistry.remove(context, id);
            NotificationHelper.cancel(context, id);
            return;
        }

        int deleted = SmsStore.delete(context, id);
        if (deleted > 0) {
            DeleteRegistry.remove(context, id);
            NotificationHelper.cancel(context, id);
            if (message != null) {
                DeletionLog.add(context, message.address, message.body, "Eliminado automáticamente");
                NotificationHelper.showAutomaticDeletion(context, id, message.body);
            }
        } else {
            // Do not silently forget a message if the provider was temporarily unavailable.
            // Retry one minute later, retaining the message's original schedule registry entry.
            DeleteScheduler.schedule(context, id, System.currentTimeMillis() + 60_000L);
        }
    }
}
