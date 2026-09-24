package com.arena.autosms5min;

import android.content.Context;

import java.util.List;
import java.util.Map;

/**
 * Refreshes message statuses: schedules inbox messages that should auto-delete
 * but have no schedule (e.g. received before an update), keeps the ones the
 * user configured to keep, and cleans stale schedule/archive entries.
 */
final class MessageMaintenance {

    static final class Result {
        int scheduled;
        int kept;
        int staleCleaned;
        int archivedPruned;

        String summary() {
            return scheduled + " mensaje(s) programados para borrarse solos.\n"
                    + kept + " mensaje(s) pasados a conservados según tus ajustes.\n"
                    + staleCleaned + " programa(s) viejo(s) limpiados.\n"
                    + archivedPruned + " archivado(s) inexistente(s) limpiados.";
        }
    }

    private MessageMaintenance() { }

    /** Single source of truth shared by the receivers and the manual refresh. */
    static boolean wantsAutoDelete(Context context, String label) {
        if (MessageClassifier.LABEL_IMPORTANT.equals(label)) {
            return AppState.shouldDeleteImportant(context);
        }
        if (MessageClassifier.LABEL_SPAM.equals(label)) {
            return AppState.shouldDeleteSpam(context);
        }
        return AppState.shouldDeleteNormal(context);
    }

    /** Schedules one inbox message if the current settings say it should auto-delete. */
    static void rescheduleIfWanted(Context context, SmsStore.SmsItem item) {
        if (item == null || item.type != SmsStore.TYPE_INBOX) return;
        long retention = AppState.retentionMillis(context);
        if (retention == AppState.NEVER) return;
        if (DeleteRegistry.dueAt(context, item.id) > 0L) return;
        if (!wantsAutoDelete(context, MessageClassifier.classify(item.body).label)) return;
        long dueAt = System.currentTimeMillis() + retention;
        DeleteRegistry.add(context, item.id, dueAt);
        DeleteScheduler.schedule(context, item.id, dueAt);
    }

    static Result refreshAll(Context context) {
        Result result = new Result();
        long retention = AppState.retentionMillis(context);

        // 1. Drop schedules whose SMS no longer exists.
        for (Map.Entry<Long, Long> entry : DeleteRegistry.all(context).entrySet()) {
            if (SmsStore.messageById(context, entry.getKey()) == null) {
                DeleteRegistry.remove(context, entry.getKey());
                DeleteScheduler.cancel(context, entry.getKey());
                result.staleCleaned++;
            }
        }

        // 2. Reconcile every visible inbox message with the current settings.
        List<SmsStore.SmsItem> all = SmsStore.allMessages(context);
        for (SmsStore.SmsItem item : all) {
            if (item.type != SmsStore.TYPE_INBOX) continue;
            long dueAt = DeleteRegistry.dueAt(context, item.id);
            boolean wants = retention != AppState.NEVER
                    && wantsAutoDelete(context, MessageClassifier.classify(item.body).label);
            if (wants && dueAt <= 0L) {
                long due = System.currentTimeMillis() + retention;
                DeleteRegistry.add(context, item.id, due);
                DeleteScheduler.schedule(context, item.id, due);
                result.scheduled++;
            } else if (!wants && dueAt > 0L) {
                DeleteScheduler.cancel(context, item.id);
                DeleteRegistry.remove(context, item.id);
                result.kept++;
            }
        }

        // 3. Prune archived IDs whose SMS is gone.
        result.archivedPruned = ArchiveStore.prune(context);
        return result;
    }
}
