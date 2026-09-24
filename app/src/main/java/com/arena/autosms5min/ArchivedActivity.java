package com.arena.autosms5min;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

/** Messages hidden from the inbox via archive. Nothing here auto-deletes. */
public final class ArchivedActivity extends Activity {
    private final List<SmsStore.SmsItem> shownItems = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private ListView list;
    private TextView empty;
    private TextView subtitle;
    private boolean dark;

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
        title.setText("Archivados");
        title.setTextSize(22);
        title.setTextColor(ThemeColors.primaryText(dark));
        toolbar.addView(title);
        root.addView(toolbar);

        subtitle = new TextView(this);
        subtitle.setTextColor(ThemeColors.secondaryText(dark));
        subtitle.setPadding(dp(4), 0, dp(4), dp(4));
        root.addView(subtitle);

        list = new ListView(this);
        list.setDividerHeight(dp(1));
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, new ArrayList<>()) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView row = (TextView) super.getView(position, convertView, parent);
                row.setTextColor(ThemeColors.primaryText(dark));
                row.setTextSize(16);
                row.setPadding(dp(16), dp(14), dp(16), dp(14));
                row.setBackgroundColor(ThemeColors.background(dark));
                return row;
            }
        };
        list.setAdapter(adapter);
        list.setOnItemClickListener((parent, view, position, id) -> openConversation(position));
        list.setOnItemLongClickListener((parent, view, position, id) -> {
            showOptions(position);
            return true;
        });
        root.addView(list, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        empty = new TextView(this);
        empty.setText("No hay mensajes archivados.\nDesliza un mensaje en la bandeja o manténlo presionado para archivarlo.");
        empty.setGravity(Gravity.CENTER);
        empty.setTextSize(17);
        empty.setTextColor(ThemeColors.secondaryText(dark));
        root.addView(empty, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);
    }

    private void refresh() {
        ArchiveStore.prune(this);
        List<SmsStore.SmsItem> all = SmsStore.allMessages(this);
        shownItems.clear();
        List<String> labels = new ArrayList<>();
        DateFormat format = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        for (SmsStore.SmsItem item : all) {
            if (!ArchiveStore.isArchived(this, item.id)) continue;
            shownItems.add(item);
            String direction = item.type == SmsStore.TYPE_SENT ? "Tú → " : "← ";
            MessageClassifier.Result classification = MessageClassifier.classify(item.body);
            labels.add(direction + item.address + "\n[" + classification.display() + "]\n"
                    + item.body + "\n" + format.format(item.date));
        }
        adapter.clear();
        adapter.addAll(labels);
        adapter.notifyDataSetChanged();
        list.setVisibility(labels.isEmpty() ? View.GONE : View.VISIBLE);
        empty.setVisibility(labels.isEmpty() ? View.VISIBLE : View.GONE);
        subtitle.setText(labels.isEmpty() ? "" : labels.size() + " archivado(s) · nada aquí se borra solo.");
    }

    private void openConversation(int position) {
        if (position < 0 || position >= shownItems.size()) return;
        Intent conversation = new Intent(this, ConversationActivity.class);
        conversation.putExtra(ConversationActivity.EXTRA_ADDRESS, shownItems.get(position).address);
        startActivity(conversation);
    }

    private void showOptions(int position) {
        if (position < 0 || position >= shownItems.size()) return;
        SmsStore.SmsItem item = shownItems.get(position);
        ThemedDialog.items(this, item.address, "Elige una acción para este mensaje archivado.",
                new String[]{"Abrir conversación", "Desarchivar (volver a la bandeja)", "Eliminar definitivamente"},
                "Cancelar", which -> {
                    if (which == 0) openConversation(position);
                    else if (which == 1) unarchive(item);
                    else confirmDelete(item);
                });
    }

    private void unarchive(SmsStore.SmsItem item) {
        ArchiveStore.unarchive(this, item.id);
        MessageMaintenance.rescheduleIfWanted(this, item);
        Toast.makeText(this, "Mensaje devuelto a la bandeja.", Toast.LENGTH_SHORT).show();
        refresh();
    }

    private void confirmDelete(SmsStore.SmsItem item) {
        ThemedDialog.confirm(this, "Eliminar definitivamente",
                "Se borrará este SMS de la base local de Android. Esta acción no se puede deshacer.",
                "Cancelar", "Eliminar", () -> {
                    DeleteScheduler.cancel(this, item.id);
                    DeleteRegistry.remove(this, item.id);
                    ArchiveStore.unarchive(this, item.id);
                    int deleted = SmsStore.delete(this, item.id);
                    NotificationHelper.cancel(this, item.id);
                    if (deleted > 0) {
                        DeletionLog.add(this, item.address, item.body, "Eliminado manualmente");
                    }
                    Toast.makeText(this, deleted > 0 ? "SMS eliminado." : "No se pudo eliminar el SMS.",
                            Toast.LENGTH_SHORT).show();
                    refresh();
                });
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + .5f);
    }
}
