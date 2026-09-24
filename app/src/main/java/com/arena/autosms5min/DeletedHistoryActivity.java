package com.arena.autosms5min;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

/** A 24-hour local safety journal for SMS discarded by this app. */
public final class DeletedHistoryActivity extends Activity {
    private boolean dark;
    private TextView subtitle;
    private ListView list;
    private TextView empty;
    private Button retention;
    private ArrayAdapter<String> adapter;

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
        retention.setBackgroundColor(ThemeColors.incomingBubble(dark));
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
        List<DeletionLog.Entry> entries = DeletionLog.entries(this);
        String period = DeletionLog.retentionLabel(this);
        subtitle.setText(DeletionLog.totalDeleted(this) + " mensajes eliminados en total · El registro no es una copia de seguridad.");
        retention.setText("Conservar vistas previas: " + period + "  ›");
        List<String> rows = new ArrayList<>();
        DateFormat format = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        for (DeletionLog.Entry entry : entries) {
            String tag = entry.keyword.isEmpty() ? entry.label : entry.label + " · “" + entry.keyword + "”";
            rows.add(entry.sender + "\n[" + tag + "] · " + entry.reason + "\n"
                    + entry.preview + "\nEliminado: " + format.format(entry.time)
                    + " · Se borra del historial: " + format.format(entry.expiresAt));
        }
        adapter.clear();
        adapter.addAll(rows);
        adapter.notifyDataSetChanged();
        list.setVisibility(rows.isEmpty() ? View.GONE : View.VISIBLE);
        empty.setText("No hay eliminaciones recientes.\nLas vistas previas se borran definitivamente después de "
                + DeletionLog.retentionLabel(this) + ".");
        empty.setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
    }

    /**
     * Uses our own themed option sheet. The platform's light single-choice rows
     * became invisible on some Motorola dark-mode combinations.
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
            option.setText((selected ? "✓  " : "○  ") + labels[i]
                    + (selected ? "  · actual" : ""));
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

        chooser.setContentView(card);
        chooser.show();
        if (chooser.getWindow() != null) {
            chooser.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            chooser.getWindow().setLayout(getResources().getDisplayMetrics().widthPixels - dp(36),
                    LinearLayout.LayoutParams.WRAP_CONTENT);
        }
    }

    private void confirmClear() {
        new AlertDialog.Builder(this)
                .setTitle("Vaciar eliminados recientemente")
                .setMessage("Se borrarán las vistas previas y remitentes guardados en este historial. El contador total se conservará.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Vaciar", (dialog, which) -> {
                    DeletionLog.clear(this);
                    refresh();
                })
                .show();
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
