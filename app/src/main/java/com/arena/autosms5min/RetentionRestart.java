package com.arena.autosms5min;

import android.app.Activity;
import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Independent, persistent notices. Changing a setting never restarts old timers. */
final class RetentionRestart {
    private final Activity activity;
    private final boolean history;
    private final ScrollView notice;
    private final TextView message;

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
        boolean dark = AppState.isDarkMode(activity);
        FrameLayout frame = new FrameLayout(activity);
        frame.addView(content, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        int pad = Ui.dp(activity, 12);
        card.setPadding(pad, pad, pad, pad);
        card.setBackground(ThemeColors.rounded(activity, ThemeColors.incomingBubble(dark), 12));
        message = new TextView(activity);
        Ui.text(message, 14f);
        message.setTextColor(ThemeColors.primaryText(dark));
        card.addView(message);
        Button restart = new Button(activity);
        Ui.styleActionButton(restart, dark);
        restart.setText("↻ REINICIAR");
        restart.setOnClickListener(v -> ThemedDialog.confirm(activity,
                "Reiniciar tiempo de " + (history ? "eliminados recientemente" : "entrantes"),
                "El nuevo plazo empieza desde cero al confirmar. Hasta entonces siguen vigentes los plazos anteriores. "
                        + (history ? "No se recupera nada que ya haya vencido."
                        : "Solo se reinician cuentas pendientes; los mensajes conservados o enviados no se modifican. Si elegiste «Nunca», se cancelan esas cuentas."),
                "Cancelar", "Reiniciar", () -> {
                    if (history) DeletionLog.restartRetained(activity);
                    else MessageMaintenance.restartAllPending(activity, AppState.retentionMillis(activity));
                    activity.getSharedPreferences("retention_restart", Context.MODE_PRIVATE)
                            .edit().putBoolean(key(history), false).apply();
                    updated.run();
                }));
        card.addView(restart);
        notice = new ScrollView(activity);
        notice.setFillViewport(false);
        notice.addView(card);
        // A floating card over the message area, capped to leave the toolbar reachable.
        int maxHeight = activity.getResources().getDisplayMetrics().heightPixels / 3;
        notice.setOnTouchListener((v, event) -> false);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(-1, maxHeight, Gravity.BOTTOM);
        params.setMargins(pad, pad, pad, pad);
        frame.addView(notice, params);
        activity.setContentView(frame);
        refresh();
    }

    void refresh() {
        boolean pending = activity.getSharedPreferences("retention_restart", Context.MODE_PRIVATE)
                .getBoolean(key(history), false);
        notice.setVisibility(pending ? View.VISIBLE : View.GONE);
        message.setText("↻ Se detectó un cambio en la configuración de la duración de eliminación de mensajes "
                + (history ? "de eliminados recientemente" : "entrantes")
                + ".\nNuevo tiempo: " + (history ? DeletionLog.retentionLabel(activity)
                : AppState.retentionLabel(activity))
                + ". Pulsa REINICIAR para actualizar los mensajes existentes y empezar su tiempo desde cero. Mientras tanto, conservan su plazo anterior.");
    }
}
