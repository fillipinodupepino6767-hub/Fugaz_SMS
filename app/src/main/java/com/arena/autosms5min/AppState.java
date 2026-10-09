package com.arena.autosms5min;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

/** Small persistent UI and deletion preferences for this local-only SMS app. */
final class AppState {
    private static final String PREFS = "app_state";
    private static final String KEY_VISIBLE_SINCE = "visible_since";
    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_THEME_MODE = "theme_mode";
    private static final String KEY_RETENTION_MS = "retention_ms";
    private static final String KEY_OUTGOING_SUBSCRIPTION_ID = "outgoing_subscription_id";
    private static final String KEY_SIM_NUMBER_PREFIX = "manual_sim_number_";
    private static final String KEY_DELETE_NORMAL = "delete_normal";
    private static final String KEY_DELETE_SPAM = "delete_spam";
    private static final String KEY_DELETE_IMPORTANT = "delete_important";
    private static final String KEY_SWIPE_RIGHT = "swipe_right";
    private static final String KEY_SWIPE_LEFT = "swipe_left";
    private static final String KEY_LAST_SMS_AT = "last_sms_received_at";
    private static final String KEY_WATCH_RINGER = "watch_ringer";
    private static final String KEY_WATCH_MINUTES = "watch_default_minutes";
    private static final String KEY_HELP_SHOWN_VERSION = "help_shown_version";
    private static final String KEY_HELP_LAST_STEP = "help_last_step";
    private static final String KEY_NOTES_VISIBLE = "settings_notes_visible";
    /** Appearance modes. */
    static final int THEME_LIGHT = 0;
    static final int THEME_DARK = 1;
    static final int THEME_SYSTEM = 2;
    /** Swipe actions for the inbox list. */
    static final int SWIPE_NOTHING = 0;
    static final int SWIPE_DELETE = 1;
    static final int SWIPE_ARCHIVE = 2;
    static final long ONE_MINUTE = 60_000L;
    static final long FIVE_MINUTES = 5L * ONE_MINUTE;
    static final long TEN_MINUTES = 10L * ONE_MINUTE;
    static final long THIRTY_MINUTES = 30L * ONE_MINUTE;
    static final long NEVER = -1L;

    private AppState() { }

    /**
     * Keeps old SMS that existed before the app was first used out of this
     * app's simple inbox. It never deletes those records.
     */
    static long visibleSince(Context context) {
        SharedPreferences prefs = prefs(context);
        long saved = prefs.getLong(KEY_VISIBLE_SINCE, -1L);
        if (saved > 0L) return saved;

        long now = System.currentTimeMillis();
        prefs.edit().putLong(KEY_VISIBLE_SINCE, now).apply();
        return now;
    }

    /** New installs follow the phone; the old on/off switch migrates automatically. */
    static int themeMode(Context context) {
        SharedPreferences prefs = prefs(context);
        if (prefs.contains(KEY_THEME_MODE)) return prefs.getInt(KEY_THEME_MODE, THEME_SYSTEM);
        if (prefs.contains(KEY_DARK_MODE)) {
            int migrated = prefs.getBoolean(KEY_DARK_MODE, false) ? THEME_DARK : THEME_LIGHT;
            prefs.edit().putInt(KEY_THEME_MODE, migrated).apply();
            return migrated;
        }
        return THEME_SYSTEM;
    }

    static void setThemeMode(Context context, int mode) {
        prefs(context).edit().putInt(KEY_THEME_MODE, mode).apply();
    }

    static String themeLabel(Context context) {
        int mode = themeMode(context);
        if (mode == THEME_LIGHT) return "Claro";
        if (mode == THEME_DARK) return "Oscuro";
        return "Automático (teléfono)";
    }

    static boolean isDarkMode(Context context) {
        int mode = themeMode(context);
        if (mode == THEME_SYSTEM) {
            int night = context.getResources().getConfiguration().uiMode
                    & Configuration.UI_MODE_NIGHT_MASK;
            return night == Configuration.UI_MODE_NIGHT_YES;
        }
        return mode == THEME_DARK;
    }

    static void setDarkMode(Context context, boolean enabled) {
        setThemeMode(context, enabled ? THEME_DARK : THEME_LIGHT);
    }

    /** The choice applies to new incoming SMS; existing scheduled SMS keep their original due time. */
    static long retentionMillis(Context context) {
        return prefs(context).getLong(KEY_RETENTION_MS, FIVE_MINUTES);
    }

    static void setRetentionMillis(Context context, long millis) {
        prefs(context).edit().putLong(KEY_RETENTION_MS, millis).apply();
    }

    static String retentionLabel(Context context) {
        return retentionLabel(retentionMillis(context));
    }

    static String retentionLabel(long millis) {
        if (millis == NEVER) return "Nunca automáticamente";
        if (millis == ONE_MINUTE) return "1 minuto";
        if (millis == FIVE_MINUTES) return "5 minutos";
        if (millis == TEN_MINUTES) return "10 minutos";
        if (millis == THIRTY_MINUTES) return "30 minutos";
        return "Personalizado";
    }

    /** -1 means let Android use its currently selected/default cellular subscription. */
    static int outgoingSubscriptionId(Context context) {
        return prefs(context).getInt(KEY_OUTGOING_SUBSCRIPTION_ID, -1);
    }

    static void setOutgoingSubscriptionId(Context context, int subscriptionId) {
        prefs(context).edit().putInt(KEY_OUTGOING_SUBSCRIPTION_ID, subscriptionId).apply();
    }

    /**
     * Per-type auto-delete switches. Important messages are kept by default to
     * protect bank codes and security messages; normal and spam follow the
     * chosen retention time unless the user changes these switches.
     */
    static boolean shouldDeleteNormal(Context context) {
        return prefs(context).getBoolean(KEY_DELETE_NORMAL, true);
    }

    static void setShouldDeleteNormal(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_DELETE_NORMAL, enabled).apply();
    }

    static boolean shouldDeleteSpam(Context context) {
        return prefs(context).getBoolean(KEY_DELETE_SPAM, true);
    }

    static void setShouldDeleteSpam(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_DELETE_SPAM, enabled).apply();
    }

    static boolean shouldDeleteImportant(Context context) {
        return prefs(context).getBoolean(KEY_DELETE_IMPORTANT, false);
    }

    static void setShouldDeleteImportant(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_DELETE_IMPORTANT, enabled).apply();
    }

    /** Gmail-style swipe: right defaults to archive, left defaults to delete. */
    static int swipeRightAction(Context context) {
        return prefs(context).getInt(KEY_SWIPE_RIGHT, SWIPE_ARCHIVE);
    }

    static void setSwipeRightAction(Context context, int action) {
        prefs(context).edit().putInt(KEY_SWIPE_RIGHT, action).apply();
    }

    static int swipeLeftAction(Context context) {
        return prefs(context).getInt(KEY_SWIPE_LEFT, SWIPE_DELETE);
    }

    static void setSwipeLeftAction(Context context, int action) {
        prefs(context).edit().putInt(KEY_SWIPE_LEFT, action).apply();
    }

    static String swipeLabel(int action) {
        if (action == SWIPE_DELETE) return "Eliminar";
        if (action == SWIPE_ARCHIVE) return "Archivar";
        return "Nada";
    }

    /** 0 means no SMS has been received since this version was installed. */
    static long lastSmsReceivedAt(Context context) {
        return prefs(context).getLong(KEY_LAST_SMS_AT, 0L);
    }

    static void setLastSmsReceivedAt(Context context, long atMillis) {
        prefs(context).edit().putLong(KEY_LAST_SMS_AT, atMillis).apply();
    }

    /** Optional display fallback only; it does not write to or alter the SIM card. */
    static String manualSimNumber(Context context, int subscriptionId) {
        return prefs(context).getString(KEY_SIM_NUMBER_PREFIX + subscriptionId, "");
    }

    static void setManualSimNumber(Context context, int subscriptionId, String number) {
        String cleaned = number == null ? "" : number.trim();
        SharedPreferences.Editor editor = prefs(context).edit();
        if (cleaned.isEmpty()) editor.remove(KEY_SIM_NUMBER_PREFIX + subscriptionId);
        else editor.putString(KEY_SIM_NUMBER_PREFIX + subscriptionId, cleaned);
        editor.apply();
    }

    /** Ringer watch: prompt when another app or the system silences the phone. */
    static boolean watchRinger(Context context) {
        return prefs(context).getBoolean(KEY_WATCH_RINGER, false);
    }

    static void setWatchRinger(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_WATCH_RINGER, enabled).apply();
    }

    /** Default minutes armed by the one-tap action in the watch prompt. */
    static long watchDefaultMinutes(Context context) {
        long minutes = prefs(context).getLong(KEY_WATCH_MINUTES, 120L);
        if (minutes < RingerTimer.MIN_MINUTES) return RingerTimer.MIN_MINUTES;
        if (minutes > RingerTimer.MAX_MINUTES) return RingerTimer.MAX_MINUTES;
        return minutes;
    }

    static void setWatchDefaultMinutes(Context context, long minutes) {
        prefs(context).edit().putLong(KEY_WATCH_MINUTES, minutes).apply();
    }

    static String watchMinutesLabel(Context context) {
        return watchMinutesLabel(watchDefaultMinutes(context));
    }

    static String watchMinutesLabel(long minutes) {
        if (minutes == 60L) return "1 h";
        if (minutes % 60L == 0L) return (minutes / 60L) + " h";
        return minutes + " min";
    }

    /** Last app versionCode for which the welcome/help message was shown (0 = never). */
    static int shownHelpVersion(Context context) {
        return prefs(context).getInt(KEY_HELP_SHOWN_VERSION, 0);
    }

    static void setShownHelpVersion(Context context, int versionCode) {
        prefs(context).edit().putInt(KEY_HELP_SHOWN_VERSION, versionCode).apply();
    }

    /** Last help-tour step shown, so reopening ❓ resumes on the same section. */
    static int helpLastStep(Context context) {
        return prefs(context).getInt(KEY_HELP_LAST_STEP, 0);
    }

    static void setHelpLastStep(Context context, int step) {
        prefs(context).edit().putInt(KEY_HELP_LAST_STEP, step).apply();
    }

    /** Long explanatory notes in Settings can be hidden for people they bother. */
    static boolean notesVisible(Context context) {
        return prefs(context).getBoolean(KEY_NOTES_VISIBLE, true);
    }

    static void setNotesVisible(Context context, boolean visible) {
        prefs(context).edit().putBoolean(KEY_NOTES_VISIBLE, visible).apply();
    }

    /** Installed version code, used to re-show the welcome once after an update. */
    static int appVersionCode(Context context) {
        try {
            android.content.pm.PackageInfo info = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            if (android.os.Build.VERSION.SDK_INT >= 28) return (int) info.getLongVersionCode();
            return info.versionCode;
        } catch (Exception ignored) {
            return -1;
        }
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
