package com.arena.autosms5min;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

/** Main inbox: intentionally uncluttered; controls live in SettingsActivity. */
public final class MainActivity extends Activity {
    private static final int REQUEST_SMS_ROLE = 400;
    private final List<SmsStore.SmsItem> shownItems = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private ListView list;
    private LinearLayout emptyState;
    private LinearLayout warningBanner;
    private TextView warningText;
    private TextView subtitle;
    private boolean dark;
    private final Handler refreshHandler = new Handler(Looper.getMainLooper());
    private final Runnable refreshTicker = new Runnable() {
        @Override public void run() {
            refresh();
            refreshHandler.postDelayed(this, 1_000L);
        }
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        dark = AppState.isDarkMode(this);
        ThemeColors.applySystemBars(this, dark);
        buildUi();
        NotificationHelper.ensureChannels(this);
        // Automatic setup on launch: request whatever runtime permissions are missing.
        SetupHelper.requestMissingRuntimePermissions(this);
        handleComposeIntent(getIntent());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (dark != AppState.isDarkMode(this)) {
            recreate();
            return;
        }
        refresh();
        refreshHandler.removeCallbacks(refreshTicker);
        refreshHandler.postDelayed(refreshTicker, 1_000L);
    }

    @Override
    protected void onPause() {
        refreshHandler.removeCallbacks(refreshTicker);
        super.onPause();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        refresh();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleComposeIntent(intent);
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(ThemeColors.background(dark));

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(dp(8), dp(10), dp(16), dp(8));
        toolbar.setBackgroundColor(ThemeColors.background(dark));

        Button settings = new Button(this);
        settings.setText("⚙");
        settings.setTextSize(25);
        settings.setTextColor(ThemeColors.accent(dark));
        settings.setContentDescription("Configuración");
        settings.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        settings.setMinWidth(dp(52));
        settings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        toolbar.addView(settings, new LinearLayout.LayoutParams(dp(52), dp(52)));

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        TextView title = new TextView(this);
        title.setText("SMS 5 minutos");
        title.setTextColor(ThemeColors.primaryText(dark));
        title.setTextSize(23);
        subtitle = new TextView(this);
        subtitle.setText("Bandeja temporal y privada");
        subtitle.setTextColor(ThemeColors.secondaryText(dark));
        subtitle.setTextSize(13);
        titles.addView(title);
        titles.addView(subtitle);
        toolbar.addView(titles, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        Button deletedHistory = new Button(this);
        deletedHistory.setText("⌛");
        deletedHistory.setTextSize(22);
        deletedHistory.setTextColor(ThemeColors.accent(dark));
        deletedHistory.setContentDescription("Eliminados recientemente");
        deletedHistory.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        deletedHistory.setMinWidth(dp(52));
        deletedHistory.setOnClickListener(v -> startActivity(new Intent(this, DeletedHistoryActivity.class)));
        toolbar.addView(deletedHistory, new LinearLayout.LayoutParams(dp(52), dp(52)));
        root.addView(toolbar);

        warningBanner = new LinearLayout(this);
        warningBanner.setOrientation(LinearLayout.HORIZONTAL);
        warningBanner.setGravity(Gravity.CENTER_VERTICAL);
        warningBanner.setBackground(ThemeColors.rounded(this, ThemeColors.incomingBubble(dark), 10));
        warningBanner.setPadding(dp(12), dp(8), dp(8), dp(8));
        LinearLayout.LayoutParams bannerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        bannerParams.setMargins(dp(12), dp(2), dp(12), dp(6));
        warningBanner.setLayoutParams(bannerParams);
        warningText = new TextView(this);
        warningText.setTextColor(ThemeColors.primaryText(dark));
        warningText.setTextSize(14);
        warningBanner.addView(warningText, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        Button repair = new Button(this);
        repair.setText("REPARAR");
        repair.setAllCaps(false);
        repair.setTextColor(ThemeColors.accent(dark));
        repair.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        repair.setOnClickListener(v -> SetupHelper.runAutoSetup(this, REQUEST_SMS_ROLE));
        warningBanner.addView(repair);
        root.addView(warningBanner);

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
        list.setOnItemClickListener((parent, view, position, id) -> {
            SmsStore.SmsItem item = shownItems.get(position);
            Intent conversation = new Intent(this, ConversationActivity.class);
            conversation.putExtra(ConversationActivity.EXTRA_ADDRESS, item.address);
            startActivity(conversation);
        });
        root.addView(list, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        emptyState = new LinearLayout(this);
        emptyState.setOrientation(LinearLayout.VERTICAL);
        emptyState.setGravity(Gravity.CENTER);
        emptyState.setPadding(dp(32), dp(20), dp(32), dp(48));
        emptyState.setBackgroundColor(ThemeColors.background(dark));

        ImageView art = new ImageView(this);
        // The illustration is deliberately text-free: the accessible UI text stays below it.
        art.setImageResource(dark ? R.drawable.empty_inbox_dark : R.drawable.empty_inbox_light);
        art.setContentDescription("Bandeja limpia");
        art.setScaleType(ImageView.ScaleType.FIT_CENTER);
        art.setAdjustViewBounds(true);
        emptyState.addView(art, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(280)));

        TextView emptyTitle = new TextView(this);
        emptyTitle.setText("Sin mensajes");
        emptyTitle.setTextColor(ThemeColors.primaryText(dark));
        emptyTitle.setTextSize(24);
        emptyTitle.setGravity(Gravity.CENTER);
        emptyTitle.setPadding(0, dp(12), 0, dp(4));
        emptyState.addView(emptyTitle);

        TextView emptyBody = new TextView(this);
        emptyBody.setText("Todo despejado y limpio ✨\nTu bandeja se tomó un respiro.");
        emptyBody.setTextColor(ThemeColors.secondaryText(dark));
        emptyBody.setTextSize(16);
        emptyBody.setGravity(Gravity.CENTER);
        emptyState.addView(emptyBody);
        root.addView(emptyState, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);
    }

    private void refresh() {
        // Keep the scroll position: the 1-second countdown ticker rebuilds the rows.
        int firstVisible = list.getFirstVisiblePosition();
        View topChild = list.getChildAt(0);
        int topOffset = topChild == null ? 0 : topChild.getTop();

        List<SmsStore.SmsItem> all = SmsStore.allMessages(this);
        shownItems.clear();
        shownItems.addAll(all);
        List<String> labels = new ArrayList<>();
        DateFormat format = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        boolean neverMode = AppState.retentionMillis(this) == AppState.NEVER;
        for (SmsStore.SmsItem item : all) {
            String direction = item.type == SmsStore.TYPE_SENT ? "Tú → " : "← ";
            MessageClassifier.Result classification = MessageClassifier.classify(item.body);
            String countdown = "";
            if (item.type == SmsStore.TYPE_INBOX) {
                long dueAt = DeleteRegistry.dueAt(this, item.id);
                if (dueAt > 0L) {
                    countdown = "\n⏳ Se elimina aproximadamente en "
                            + CountdownFormatter.formatVerbose(dueAt);
                } else if (neverMode) {
                    countdown = "\nAuto-eliminación desactivada (Nunca)";
                } else {
                    countdown = "\n✓ Conservado · no se borra solo";
                }
            }
            labels.add(direction + item.address + "\n[" + classification.display() + "]\n"
                    + item.body + "\n" + format.format(item.date) + countdown);
        }
        adapter.clear();
        adapter.addAll(labels);
        adapter.notifyDataSetChanged();
        list.setSelectionFromTop(firstVisible, topOffset);
        boolean hasMessages = !all.isEmpty();
        list.setVisibility(hasMessages ? View.VISIBLE : View.GONE);
        emptyState.setVisibility(hasMessages ? View.GONE : View.VISIBLE);

        subtitle.setText("Bandeja temporal · borrado: " + AppState.retentionLabel(this));
        String problem = SetupHelper.bannerText(this);
        if (problem == null) {
            warningBanner.setVisibility(View.GONE);
        } else {
            warningBanner.setVisibility(View.VISIBLE);
            warningText.setText("⚠ " + problem);
        }
    }

    private void handleComposeIntent(Intent intent) {
        if (intent == null || !Intent.ACTION_SENDTO.equals(intent.getAction())) return;
        Uri data = intent.getData();
        if (data == null) return;
        String number = data.getSchemeSpecificPart();
        if (number == null || number.isEmpty()) return;
        Intent conversation = new Intent(this, ConversationActivity.class)
                .putExtra(ConversationActivity.EXTRA_ADDRESS, number);
        startActivity(conversation);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + .5f);
    }
}
