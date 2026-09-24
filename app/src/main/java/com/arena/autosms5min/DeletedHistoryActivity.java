package com.arena.autosms5min;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

/** Temporary safety journal for SMS discarded by this app. Tap an entry to recover it. */
public final class DeletedHistoryActivity extends Activity {
    private boolean dark;
    private TextView subtitle;
    private ListView list;
    private TextView empty;
    private Button retention;
    private ArrayAdapter<String> adapter;
    private List<DeletionLog.Entry> shownEntries = new ArrayList<>();
    private final Handler tickerHandler = new Handler(Looper.getMainLooper());
    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            refresh();
            tickerHandler.postDelayed(this, 1_000L);
        }
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        dark = AppState.isDarkMode(this);
        ThemeColors.applySystemBars(this, dark);
        buildUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
        tickerHandler.removeCallbacks(ticker);
        tickerHandler.postDelayed(ticker, 1_000L);
    }

    @Override
    protected void onPause() {
        tickerHandler.removeCallbacks(ticker);
        super.onPause();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(12), dp(12), dp(12), dp(12));
        root.setBackgroundColor(ThemeColors.background(dark));

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        Button back = new Button(this);
        back.setText("‹");
        back.setTextSize(32);
        back.setTextColor(ThemeColors.accent(dark));
        back.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        back.setOnClickListener(v -> finish());
        toolbar.addView(back, new LinearLayout.LayoutParams(dp(48), dp(52)));
        TextView title = new TextView(this);
        title.setText("Eliminados recientemente");
        title.setTextSize(22);
        title.setTextColor(ThemeColors.primaryText(dark));
        toolbar.addView(title, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        Button clear = new Button(this);
        clear.setText("VACIAR");
        clear.setTextColor(ThemeColors.accent(dark));
        clear.setTextSize(14);
        clear.setAllCaps(false);
        // Avoid the default white Material button against the dark interface.
        clear.setBackground(roundedBackground(ThemeColors.incomingBubble(dark), 10));
        clear.setOnClickListener(v -> confirmClear());
        toolbar.addView(clear);
        root.addView(toolbar);

        subtitle = new TextView(this);
        subtitle.setTextColor(ThemeColors.secondaryText(dark));
        subtitle.setPadding(dp(4), 0, dp(4), dp(4));
        root.addView(subtitle);

        retention = new Button(this);
        retention.setAllCaps(false);
        retention.setTextColor(ThemeColors.accent(dark));
        retention.setBackground(roundedBackground(ThemeColors.incomingBubble(dark), 10));
        retention.setGravity(Gravity.CENTER);
        retention.setOnClickListener(v -> chooseRetention());
        root.addView(retention, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        list = new ListView(this);
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, new ArrayList<>()) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView row = (TextView) super.getView(position, convertView, parent);
                row.setTextColor(ThemeColors.primaryText(dark));
                row.setTextSize(15);
                row.setPadding(dp(12), dp(14), dp(12), dp(14));
                row.setBackgroundColor(ThemeColors.incomingBubble(dark));
                return row;
            }
        };
        list.setAdapter(adapter);
        list.setOnItemClickListener((parent, view, position, id) -> showEntryOptions(position));
        root.addView(list, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        empty = new TextView(this);
        empty.setText("No hay eliminaciones recientes.\nEl registro conserva remitente y vista previa durante 24 horas.");
        empty.setGravity(Gravity.CENTER);
        empty.setTextSize(17);
        empty.setTextColor(ThemeColors.secondaryText(dark));
        root.addView(empty, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);
    }

    private void refresh() {
        int firstVisible = list.getFirstVisiblePosition();
        View topChild = list.getChildAt(0);
        int topOffset = topChild == null ? 0 : topChild.getTop();

        List<DeletionLog.Entry> entries = DeletionLog.entries(this);
        shownEntries = entries;
        String period = DeletionLog.retentionLabel(this);
        subtitle.setText(DeletionLog.totalDeleted(this) + " mensajes eliminados en total · "
                + "Toca un registro para recuperarlo. El historial guarda el mensaje completo "
                + "hasta que venza; no es una copia permanente.");
        retention.setText("Conservar vistas previas: " + period + "  ›");
        List<String> rows = new ArrayList<>();
        DateFormat format = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        for (DeletionLog.Entry entry : entries) {
            String tag = entry.keyword.isEmpty() ? entry.label : entry.label + " · \u201C" + entry.keyword + "\u201D";
            rows.add(entry.sender + "\n[" + tag + "] · " + entry.reason + "\n"
                    + entry.preview + "\nEliminado: " + format.format(entry.time)
                    + "\nSe borra del historial: " + format.format(entry.expiresAt)
                    + "  (quedan " + CountdownFormatter.formatHistoryRemaining(entry.expiresAt) + ")");
        }
        adapter.clear();
        adapter.addAll(rows);
        adapter.notifyDataSetChanged();
        list.setSelectionFromTop(firstVisible, topOffset);
        list.setVisibility(rows.isEmpty() ? View.GONE : View.VISIBLE);
        empty.setText("No hay eliminaciones recientes.\nLas vistas previas se borran definitivamente después de "
                + DeletionLog.retentionLabel(this) + ".");
        empty.setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showEntryOptions(int position) {
        if (position < 0 || position >= shownEntries.size()) return;
        DeletionLog.Entry entry = shownEntries.get(position);
        if (DeletionLog.isSummary(entry)) {
            ThemedDialog.items(this, entry.sender, entry.preview
                            + "\n\nEste es un registro informativo de limpieza, no un mensaje recuperable.",
                    new String[]{"Eliminar este registro"}, "Cancelar", which -> {
                        if (which == 0) {
                            DeletionLog.removeEntry(this, entry.time, entry.sender, entry.preview);
                            refresh();
                        }
                    });
            return;
        }
        String tag = entry.keyword.isEmpty() ? entry.label
                : entry.label + " · \u201C" + entry.keyword + "\u201D";
        if (entry.hasFull()) {
            ThemedDialog.items(this, entry.sender,
                    "[" + tag + "] · " + entry.reason + "\n" + entry.preview
                            + "\n\nRecuperar devuelve el mensaje COMPLETO a la app, conservado "
                            + "(sin borrado automático).",
                    new String[]{"Recuperar a la bandeja", "Recuperar en Archivados",
                            "Eliminar este registro"},
                    "Cancelar", which -> {
                        if (which == 0) recover(entry, false, true);
                        else if (which == 1) recover(entry, true, true);
                        else {
                            DeletionLog.removeEntry(this, entry.time, entry.sender, entry.preview);
                            refresh();
                        }
                    });
        } else {
            ThemedDialog.items(this, entry.sender,
                    "Este registro se guardó antes de la función de recuperación y solo conserva "
                            + "una vista previa:\n\n" + entry.preview,
                    new String[]{"Recuperar vista previa a la bandeja", "Eliminar este registro"},
                    "Cancelar", which -> {
                        if (which == 0) recover(entry, false, false);
                        else {
                            DeletionLog.removeEntry(this, entry.time, entry.sender, entry.preview);
                            refresh();
                        }
                    });
        }
    }

    private void recover(DeletionLog.Entry entry, boolean toArchive, boolean useFull) {
        if (!SetupHelper.isDefaultSms(this)) {
            Toast.makeText(this, "Para recuperar, la app debe ser la predeterminada.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        String text = useFull ? entry.full : entry.preview;
        if (text.isEmpty()) {
            Toast.makeText(this, "Este registro no tiene texto que recuperar.",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        long id;
        try {
            id = SmsStore.insertIncoming(this, entry.sender, text, System.currentTimeMillis());
        } catch (SecurityException denied) {
            id = -1L;
        }
        if (id > 0L) {
            if (toArchive) ArchiveStore.archive(this, id);
            DeletionLog.removeEntry(this, entry.time, entry.sender, entry.preview);
            Toast.makeText(this, toArchive ? "Recuperado en Archivados (conservado)."
                    : "Recuperado a la bandeja (conservado).", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "No se pudo recuperar el mensaje.", Toast.LENGTH_LONG).show();
        }
        refresh();
    }

    /**
     * Uses our own themed option sheet. The platform's light single-choice rows
     * became invisible on some Motorola dark-mode combinations. The content
     * scrolls so large-font phones can reach every option.
     */
    private void chooseRetention() {
        final String[] labels = {"1 hora", "6 horas", "12 horas", "24 horas", "72 horas", "7 días"};
        final long[] values = {DeletionLog.ONE_HOUR_MS, 6L * DeletionLog.ONE_HOUR_MS,
                12L * DeletionLog.ONE_HOUR_MS, 24L * DeletionLog.ONE_HOUR_MS,
                72L * DeletionLog.ONE_HOUR_MS, 168L * DeletionLog.ONE_HOUR_MS};
        final long current = DeletionLog.retentionMillis(this);

        final Dialog chooser = new Dialog(this);
        chooser.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(22), dp(22), dp(22), dp(14));
        card.setBackground(roundedBackground(ThemeColors.incomingBubble(dark), 18));

        TextView heading = new TextView(this);
        heading.setText("Tiempo de eliminados recientemente");
        heading.setTextSize(22);
        heading.setTextColor(ThemeColors.primaryText(dark));
        card.addView(heading);

        TextView explanation = new TextView(this);
        explanation.setText("Elige cuánto tiempo conservar las vistas previas. Al vencer, se eliminan definitivamente. Reducir el tiempo puede borrar entradas existentes; aumentarlo no recupera las ya eliminadas.");
        explanation.setTextSize(16);
        explanation.setTextColor(ThemeColors.secondaryText(dark));
        explanation.setPadding(0, dp(10), 0, dp(12));
        card.addView(explanation);

        for (int i = 0; i < labels.length; i++) {
            final long selectedValue = values[i];
            Button option = new Button(this);
            boolean selected = selectedValue == current;
            option.setAllCaps(false);
            option.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            option.setText((selected ? "\u2713  " : "\u25CB  ") + labels[i]
                    + (selected ? "  \u00B7 actual" : ""));
            option.setTextSize(16);
            option.setTextColor(ThemeColors.accent(dark));
            option.setBackground(roundedBackground(selected ? ThemeColors.sentBubble(dark)
                    : ThemeColors.background(dark), 10));
            LinearLayout.LayoutParams optionParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            optionParams.setMargins(0, dp(3), 0, dp(3));
            card.addView(option, optionParams);
            option.setOnClickListener(v -> {
                DeletionLog.setRetentionMillis(this, selectedValue);
                chooser.dismiss();
                refresh();
            });
        }

        Button cancel = new Button(this);
        cancel.setText("Cancelar");
        cancel.setAllCaps(false);
        cancel.setTextColor(ThemeColors.accent(dark));
        cancel.setBackgroundColor(Color.TRANSPARENT);
        cancel.setOnClickListener(v -> chooser.dismiss());
        LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cancelParams.gravity = Gravity.END;
        cancelParams.topMargin = dp(6);
        card.addView(cancel, cancelParams);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.TRANSPARENT);
        scroll.addView(card);
        chooser.setContentView(scroll);
        chooser.show();
        if (chooser.getWindow() != null) {
            chooser.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            chooser.getWindow().setLayout(getResources().getDisplayMetrics().widthPixels - dp(36),
                    LinearLayout.LayoutParams.WRAP_CONTENT);
        }
    }

    private void confirmClear() {
        ThemedDialog.confirm(this, "Vaciar eliminados recientemente",
                "Se borrarán las vistas previas y remitentes guardados en este historial. El contador total se conservará.",
                "Cancelar", "Vaciar", () -> {
                    DeletionLog.clear(this);
                    refresh();
                });
    }

    private GradientDrawable roundedBackground(int color, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        return drawable;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + .5f);
    }
}
