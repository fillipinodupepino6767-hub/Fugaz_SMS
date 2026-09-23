package com.arena.autosms5min;

import android.app.Activity;
import android.app.AlertDialog;
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
        clear.setOnClickListener(v -> confirmClear());
        toolbar.addView(clear);
        root.addView(toolbar);

        subtitle = new TextView(this);
        subtitle.setTextColor(ThemeColors.secondaryText(dark));
        subtitle.setPadding(dp(4), 0, dp(4), dp(8));
        root.addView(subtitle);

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
        subtitle.setText(DeletionLog.totalDeleted(this) + " mensajes eliminados en total · Vista previa disponible durante 24 horas");
        List<String> rows = new ArrayList<>();
        DateFormat format = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        for (DeletionLog.Entry entry : entries) {
            String tag = entry.keyword.isEmpty() ? entry.label : entry.label + " · “" + entry.keyword + "”";
            rows.add(entry.sender + "\n[" + tag + "] · " + entry.reason + "\n"
                    + entry.preview + "\n" + format.format(entry.time));
        }
        adapter.clear();
        adapter.addAll(rows);
        adapter.notifyDataSetChanged();
        list.setVisibility(rows.isEmpty() ? View.GONE : View.VISIBLE);
        empty.setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
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

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + .5f);
    }
}
