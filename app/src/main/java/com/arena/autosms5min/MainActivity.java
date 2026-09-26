package com.arena.autosms5min;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

/** Main inbox: search, swipe to delete/archive like Gmail; everything else lives in Settings. */
public final class MainActivity extends Activity {
    private static final int REQUEST_SMS_ROLE = 400;
    private final List<SmsStore.SmsItem> shownItems = new ArrayList<>();
    private InboxAdapter adapter;
    private RecyclerView list;
    private LinearLayout emptyState;
    private LinearLayout warningBanner;
    private TextView warningText;
    private TextView subtitle;
    private TextView emptyTitle;
    private TextView emptyBody;
    private EditText searchBox;
    private String query = "";
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
        toolbar.setPadding(dp(8), dp(10), dp(8), dp(8));
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
        title.setText("Fugaz SMS");
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
        Button archived = new Button(this);
        archived.setText("\uD83D\uDCE6");
        archived.setTextSize(20);
        archived.setContentDescription("Archivados");
        archived.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        archived.setMinWidth(dp(52));
        archived.setOnClickListener(v -> startActivity(new Intent(this, ArchivedActivity.class)));
        toolbar.addView(archived, new LinearLayout.LayoutParams(dp(52), dp(52)));
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

        LinearLayout searchRow = new LinearLayout(this);
        searchRow.setOrientation(LinearLayout.HORIZONTAL);
        searchRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        searchParams.setMargins(dp(12), 0, dp(12), dp(6));
        searchRow.setLayoutParams(searchParams);
        searchBox = new EditText(this);
        searchBox.setHint("🔎 Buscar por nombre, número o texto");
        searchBox.setSingleLine(true);
        searchBox.setHintTextColor(ThemeColors.secondaryText(dark));
        searchBox.setTextColor(ThemeColors.primaryText(dark));
        searchBox.setBackground(ThemeColors.rounded(this, ThemeColors.incomingBubble(dark), 10));
        searchBox.setPadding(dp(14), dp(10), dp(14), dp(10));
        searchBox.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override public void afterTextChanged(Editable s) {
                query = s.toString().trim();
                refresh();
            }
        });
        searchRow.addView(searchBox, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        Button clearSearch = new Button(this);
        clearSearch.setText("✕");
        clearSearch.setTextColor(ThemeColors.accent(dark));
        clearSearch.setContentDescription("Limpiar búsqueda");
        clearSearch.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        clearSearch.setOnClickListener(v -> searchBox.setText(""));
        searchRow.addView(clearSearch, new LinearLayout.LayoutParams(dp(48), dp(48)));
        root.addView(searchRow);

        list = new RecyclerView(this);
        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new InboxAdapter(dark, new InboxAdapter.OnItem() {
            @Override public void onClick(int position) { openConversation(position); }
            @Override public void onLongPress(int position) { showOptions(position); }
        });
        list.setAdapter(adapter);
        new ItemTouchHelper(swipeCallback()).attachToRecyclerView(list);
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

        emptyTitle = new TextView(this);
        emptyTitle.setText("Sin mensajes");
        emptyTitle.setTextColor(ThemeColors.primaryText(dark));
        emptyTitle.setTextSize(24);
        emptyTitle.setGravity(Gravity.CENTER);
        emptyTitle.setPadding(0, dp(12), 0, dp(4));
        emptyState.addView(emptyTitle);

        emptyBody = new TextView(this);
        emptyBody.setText("Todo despejado y limpio ✨\nTu bandeja se tomó un respiro.");
        emptyBody.setTextColor(ThemeColors.secondaryText(dark));
        emptyBody.setTextSize(16);
        emptyBody.setGravity(Gravity.CENTER);
        emptyState.addView(emptyBody);
        root.addView(emptyState, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);
    }

    /** Gmail-style swipe. Each side's action is configurable in Settings. */
    private ItemTouchHelper.SimpleCallback swipeCallback() {
        return new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public int getMovementFlags(RecyclerView recycler, RecyclerView.ViewHolder holder) {
                int flags = 0;
                if (AppState.swipeRightAction(MainActivity.this) != AppState.SWIPE_NOTHING) {
                    flags |= ItemTouchHelper.RIGHT;
                }
                if (AppState.swipeLeftAction(MainActivity.this) != AppState.SWIPE_NOTHING) {
                    flags |= ItemTouchHelper.LEFT;
                }
                return makeMovementFlags(0, flags);
            }

            @Override
            public boolean onMove(RecyclerView recycler, RecyclerView.ViewHolder holder,
                                  RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder holder, int direction) {
                int position = holder.getBindingAdapterPosition();
                if (position < 0 || position >= shownItems.size()) {
                    refresh();
                    return;
                }
                SmsStore.SmsItem item = shownItems.get(position);
                int action = direction == ItemTouchHelper.RIGHT
                        ? AppState.swipeRightAction(MainActivity.this)
                        : AppState.swipeLeftAction(MainActivity.this);
                if (action == AppState.SWIPE_DELETE) {
                    deleteMessage(item, "Eliminado por deslizamiento", true);
                } else if (action == AppState.SWIPE_ARCHIVE) {
                    archiveMessage(item);
                }
                refresh();
            }

            @Override
            public void onChildDraw(Canvas canvas, RecyclerView recycler,
                                    RecyclerView.ViewHolder holder, float dX, float dY,
                                    int actionState, boolean isActive) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE && dX != 0f) {
                    int action = dX > 0f ? AppState.swipeRightAction(MainActivity.this)
                            : AppState.swipeLeftAction(MainActivity.this);
                    if (action != AppState.SWIPE_NOTHING) {
                        View row = holder.itemView;
                        boolean delete = action == AppState.SWIPE_DELETE;
                        Paint background = new Paint();
                        background.setColor(delete ? Color.rgb(198, 40, 40) : Color.rgb(46, 125, 50));
                        if (dX > 0f) {
                            canvas.drawRect(row.getLeft(), row.getTop(), row.getLeft() + dX,
                                    row.getBottom(), background);
                        } else {
                            canvas.drawRect(row.getRight() + dX, row.getTop(), row.getRight(),
                                    row.getBottom(), background);
                        }
                        Paint label = new Paint();
                        label.setColor(Color.WHITE);
                        label.setTextSize(dp(16));
                        label.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                        String text = delete ? "ELIMINAR" : "ARCHIVAR";
                        float centerY = row.getTop() + row.getHeight() / 2f + dp(6);
                        if (dX > 0f) {
                            canvas.drawText(text, row.getLeft() + dp(20), centerY, label);
                        } else {
                            float width = label.measureText(text);
                            canvas.drawText(text, row.getRight() - dp(20) - width, centerY, label);
                        }
                    }
                }
                super.onChildDraw(canvas, recycler, holder, dX, dY, actionState, isActive);
            }
        };
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
        ThemedDialog.items(this, ContactNames.displayName(this, item.address),
                "Elige una acción para este mensaje.",
                new String[]{"Abrir conversación", "Archivar", "Eliminar este SMS"},
                "Cancelar", which -> {
                    if (which == 0) openConversation(position);
                    else if (which == 1) {
                        archiveMessage(item);
                        refresh();
                    } else {
                        confirmDelete(item);
                    }
                });
    }

    private void archiveMessage(SmsStore.SmsItem item) {
        DeleteScheduler.cancel(this, item.id);
        DeleteRegistry.remove(this, item.id);
        NotificationHelper.cancel(this, item.id);
        ArchiveStore.archive(this, item.id);
        Toast.makeText(this, "Mensaje archivado.", Toast.LENGTH_SHORT).show();
    }

    private void confirmDelete(SmsStore.SmsItem item) {
        ThemedDialog.confirm(this, "Eliminar SMS",
                "¿Eliminar este SMS de " + ContactNames.singleLineLabel(this, item.address)
                        + "? Esta acción no se puede deshacer.",
                "Cancelar", "Eliminar", () -> {
                    deleteMessage(item, "Eliminado manualmente", true);
                    refresh();
                });
    }

    private void deleteMessage(SmsStore.SmsItem item, String reason, boolean toast) {
        DeleteScheduler.cancel(this, item.id);
        DeleteRegistry.remove(this, item.id);
        ArchiveStore.unarchive(this, item.id);
        int deleted = SmsStore.delete(this, item.id);
        NotificationHelper.cancel(this, item.id);
        if (deleted > 0) {
            DeletionLog.add(this, item.address, item.body, reason);
        }
        if (toast) {
            Toast.makeText(this, deleted > 0 ? "SMS eliminado." : "No se pudo eliminar el SMS.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private boolean matchesQuery(SmsStore.SmsItem item) {
        if (query.isEmpty()) return true;
        String lowered = query.toLowerCase();
        return item.address.toLowerCase().contains(lowered)
                || item.body.toLowerCase().contains(lowered)
                || ContactNames.displayName(this, item.address).toLowerCase().contains(lowered);
    }

    private void refresh() {
        // Keep the scroll position: the 1-second countdown ticker rebuilds the rows.
        LinearLayoutManager layout = (LinearLayoutManager) list.getLayoutManager();
        int firstVisible = layout == null ? 0 : layout.findFirstVisibleItemPosition();
        View topChild = (layout == null) ? null : layout.findViewByPosition(firstVisible);
        int topOffset = topChild == null ? 0 : topChild.getTop();

        List<SmsStore.SmsItem> all = SmsStore.allMessages(this);
        shownItems.clear();
        List<InboxAdapter.Row> rows = new ArrayList<>();
        DateFormat format = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        boolean neverMode = AppState.retentionMillis(this) == AppState.NEVER;
        int total = 0;
        for (SmsStore.SmsItem item : all) {
            if (ArchiveStore.isArchived(this, item.id)) continue;
            total++;
            if (!matchesQuery(item)) continue;
            shownItems.add(item);
            String direction = item.type == SmsStore.TYPE_SENT ? "Tú → " : "← ";
            String name = ContactNames.displayName(this, item.address);
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
            String text = direction + ContactNames.twoLineLabel(this, item.address)
                    + "\n[" + classification.display() + "]\n"
                    + item.body + "\n" + format.format(item.date) + countdown;
            rows.add(new InboxAdapter.Row(text, ContactNames.avatarLetter(name),
                    ContactNames.avatarColor(name)));
        }
        adapter.setRows(rows);
        if (layout != null && firstVisible >= 0) layout.scrollToPositionWithOffset(firstVisible, topOffset);
        boolean hasMessages = !shownItems.isEmpty();
        list.setVisibility(hasMessages ? View.VISIBLE : View.GONE);
        emptyState.setVisibility(hasMessages ? View.GONE : View.VISIBLE);
        if (!query.isEmpty()) {
            subtitle.setText("🔎 " + shownItems.size() + " de " + total + " · filtro: «" + query + "»");
            emptyTitle.setText("Sin resultados");
            emptyBody.setText("Nada coincide con «" + query + "».\nPrueba con otro nombre, número o palabra.");
        } else {
            subtitle.setText("Bandeja temporal · borrado: " + AppState.retentionLabel(this));
            emptyTitle.setText("Sin mensajes");
            emptyBody.setText("Todo despejado y limpio ✨\nTu bandeja se tomó un respiro.");
        }

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

    /** Adapter: one row per message, with a letter avatar plus the styled text. */
    private static final class InboxAdapter extends RecyclerView.Adapter<InboxAdapter.Holder> {
        interface OnItem {
            void onClick(int position);
            void onLongPress(int position);
        }

        static final class Row {
            final String text;
            final String letter;
            final int color;
            Row(String text, String letter, int color) {
                this.text = text;
                this.letter = letter;
                this.color = color;
            }
        }

        static final class Holder extends RecyclerView.ViewHolder {
            final TextView avatar;
            final TextView text;
            Holder(View row, TextView avatar, TextView text) {
                super(row);
                this.avatar = avatar;
                this.text = text;
            }
        }

        private final List<Row> rows = new ArrayList<>();
        private final boolean dark;
        private final OnItem listener;

        InboxAdapter(boolean dark, OnItem listener) {
            this.dark = dark;
            this.listener = listener;
        }

        void setRows(List<Row> next) {
            rows.clear();
            rows.addAll(next);
            notifyDataSetChanged();
        }

        @Override
        public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
            float density = parent.getContext().getResources().getDisplayMetrics().density;
            int pad = (int) (12 * density + .5f);
            int avatarSize = (int) (52 * density + .5f);
            LinearLayout row = new LinearLayout(parent.getContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(pad, pad, pad, pad);
            row.setBackgroundColor(ThemeColors.background(dark));
            row.setLayoutParams(new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            TextView avatar = new TextView(parent.getContext());
            avatar.setGravity(Gravity.CENTER);
            avatar.setTextColor(Color.WHITE);
            avatar.setTextSize(20);
            avatar.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(avatarSize, avatarSize);
            avatarParams.setMargins(0, 0, pad, 0);
            row.addView(avatar, avatarParams);
            TextView text = new TextView(parent.getContext());
            text.setTextColor(ThemeColors.primaryText(dark));
            text.setTextSize(16);
            row.addView(text, new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            return new Holder(row, avatar, text);
        }

        @Override
        public void onBindViewHolder(Holder holder, int position) {
            Row row = rows.get(position);
            holder.text.setText(row.text);
            holder.avatar.setText(row.letter);
            GradientDrawable circle = new GradientDrawable();
            circle.setShape(GradientDrawable.OVAL);
            circle.setColor(row.color);
            holder.avatar.setBackground(circle);
            holder.itemView.setOnClickListener(v -> {
                int current = holder.getBindingAdapterPosition();
                if (current >= 0) listener.onClick(current);
            });
            holder.itemView.setOnLongClickListener(v -> {
                int current = holder.getBindingAdapterPosition();
                if (current >= 0) listener.onLongPress(current);
                return true;
            });
        }

        @Override
        public int getItemCount() {
            return rows.size();
        }
    }
}
