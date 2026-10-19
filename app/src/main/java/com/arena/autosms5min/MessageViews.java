package com.arena.autosms5min;

import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Separate semantic blocks; never inserts spaces/newlines into the actual SMS. */
final class MessageViews extends LinearLayout {
    private final TextView sender;
    private final TextView category;
    private final TextView body;
    private final TextView date;
    private final TextView status;

    MessageViews(Context context, boolean dark) {
        super(context);
        setOrientation(VERTICAL);
        sender = field(16, ThemeColors.primaryText(dark), 0);
        sender.setTypeface(Typeface.create("sans-serif-medium",
                AppState.boldText(context) ? Typeface.BOLD : Typeface.NORMAL));
        category = field(12, ThemeColors.accent(dark), 4);
        body = field(16, ThemeColors.primaryText(dark), 10);
        body.setLineSpacing(Ui.dp(context, 2), 1.18f);
        date = field(12, ThemeColors.secondaryText(dark), 10);
        status = field(12, ThemeColors.accent(dark), 5);
    }

    private TextView field(float size, int color, int gap) {
        TextView text = new TextView(getContext());
        Ui.text(text, size);
        text.setTextColor(color);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = Ui.dp(getContext(), gap);
        addView(text, params);
        return text;
    }

    void bind(String from, String label, String text, String timestamp, String countdown) {
        sender.setText(from);
        category.setText(label);
        category.setVisibility(label.isEmpty() ? View.GONE : View.VISIBLE);
        body.setText(text);
        date.setText(timestamp);
        updateStatus(countdown);
    }

    void updateStatus(String countdown) {
        status.setText(countdown);
        status.setVisibility(countdown.isEmpty() ? View.GONE : View.VISIBLE);
    }
}
