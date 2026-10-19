package com.arena.autosms5min;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.Toast;

/** One blocking notice per change. Leaving the screen dismisses without restarting. */
final class RetentionRestart {
    private final Activity activity;
    private final boolean history;
    private final Runnable updated;
    private Dialog dialog;
    private boolean resumed;

    private static String key(boolean history) {
        return history ? "history_restart_pending" : "inbox_restart_pending";
    }

    static void mark(Context context, boolean history) {
        context.getSharedPreferences("retention_restart", Context.MODE_PRIVATE)
                .edit().putBoolean(key(history), true).apply();
    }

    RetentionRestart(Activity activity, View content, boolean history, Runnable updated) {
        this.activity = activity;
        this.history = history;
        this.updated = updated;
        activity.setContentView(content);
    }

    void onResume() {
        resumed = true;
        refresh();
    }

    void onPause() {
        resumed = false;
        // Consumed when shown: neither Home, screen lock, rotation nor process death
        // can leave a stale blocking dialog to reappear on the next launch.
        if (dialog != null) {
            dialog.dismiss();
            dialog = null;
        }
    }

    void refresh() {
        if (!resumed || activity.isFinishing() || activity.isDestroyed()
                || (dialog != null && dialog.isShowing()) || ThemedDialog.activeCount() > 0) return;
        android.content.SharedPreferences prefs = activity.getSharedPreferences(
                "retention_restart", Context.MODE_PRIVATE);
        if (!prefs.getBoolean(key(history), false)) return;
        String section = history ? "eliminados recientemente" : "mensajes entrantes";
        String duration = history ? DeletionLog.retentionLabel(activity) : AppState.retentionLabel(activity);
        // Commit before display so even an abrupt process kill cannot replay this notice.
        if (!prefs.edit().putBoolean(key(history), false).commit()) return;
        dialog = ThemedDialog.requiredAction(activity, "↻ Cambió el tiempo de " + section,
                "Se detectó un cambio en la duración de eliminación de " + section + ".\n\n"
                        + "Nuevo tiempo: " + duration + ".\n\n"
                        + "Pulsa REINICIAR para aplicar este tiempo a las cuentas existentes y empezarlas desde cero.\n\n"
                        + (history ? "No se recuperan registros que ya hayan vencido."
                        : "Solo se actualizan cuentas pendientes. No se tocan los enviados ni los conservados. Si elegiste «Nunca», se cancelan esas cuentas.")
                        + "\n\nSi sales de la app sin reiniciar, este aviso se desactiva y los mensajes mantienen sus plazos anteriores.",
                "↻ REINICIAR", () -> {
                    try {
                        if (history) DeletionLog.restartRetained(activity);
                        else MessageMaintenance.restartAllPending(activity, AppState.retentionMillis(activity));
                        dialog.dismiss();
                        dialog = null;
                        updated.run();
                    } catch (RuntimeException error) {
                        Toast.makeText(activity, "No se pudo reiniciar. Revisa los permisos e inténtalo de nuevo.",
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
}
