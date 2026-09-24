package com.arena.autosms5min;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

/**
 * Local archive: hides messages from the inbox without deleting them from
 * Android's SMS provider. Archived messages keep no auto-delete schedule;
 * unarchiving reschedules them if the current settings say so.
 */
final class ArchiveStore {
    private static final String PREFS = "archive_store";
    private static final String KEY_IDS = "ids";

    private ArchiveStore() { }

    static void archive(Context context, long smsId) {
        Set<String> ids = copyOf(prefs(context));
        ids.add(Long.toString(smsId));
        prefs(context).edit().putStringSet(KEY_IDS, ids).apply();
    }

    static void unarchive(Context context, long smsId) {
        Set<String> ids = copyOf(prefs(context));
        if (ids.remove(Long.toString(smsId))) {
            prefs(context).edit().putStringSet(KEY_IDS, ids).apply();
        }
    }

    static boolean isArchived(Context context, long smsId) {
        return copyOf(prefs(context)).contains(Long.toString(smsId));
    }

    static int count(Context context) {
        return copyOf(prefs(context)).size();
    }

    /** Removes archived IDs whose SMS no longer exists. Returns how many were pruned. */
    static int prune(Context context) {
        Set<String> ids = copyOf(prefs(context));
        if (ids.isEmpty()) return 0;
        Set<String> alive = new HashSet<>();
        for (String raw : ids) {
            try {
                long id = Long.parseLong(raw);
                if (SmsStore.messageById(context, id) != null) alive.add(raw);
            } catch (NumberFormatException ignored) {
                // Drop malformed entries.
            }
        }
        int removed = ids.size() - alive.size();
        if (removed > 0) prefs(context).edit().putStringSet(KEY_IDS, alive).apply();
        return removed;
    }

    private static Set<String> copyOf(SharedPreferences prefs) {
        Set<String> saved = prefs.getStringSet(KEY_IDS, null);
        return saved == null ? new HashSet<>() : new HashSet<>(saved);
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
