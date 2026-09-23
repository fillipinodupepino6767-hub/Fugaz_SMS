package com.arena.autosms5min;

import android.Manifest;
import android.app.Activity;
import android.app.role.RoleManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
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

public final class MainActivity extends Activity {
    private static final int REQUEST_SMS_ROLE = 400;
    private static final int REQUEST_PERMISSIONS = 401;
    private final List<SmsStore.SmsItem> shownItems = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private TextView title;
    private TextView status;
    private TextView note;
    private Button setupButton;
    private Button themeButton;
    private boolean dark;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        dark = AppState.isDarkMode(this);
        ThemeColors.applySystemBars(this, dark);
        buildUi();
        requestRoleAndPermissionsIfNeeded();
        handleComposeIntent(getIntent());
    }

    @Override
    protected void onResume() {
        super.onResume();
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
        root.setPadding(dp(16), dp(16), dp(16), dp(8));
        root.setBackgroundColor(ThemeColors.background(dark));

        title = new TextView(this);
        title.setText("SMS 5 minutos");
        title.setTextSize(24);
        title.setTextColor(ThemeColors.primaryText(dark));
        root.addView(title);

        status = new TextView(this);
        status.setPadding(0, dp(8), 0, dp(8));
        status.setTextColor(ThemeColors.secondaryText(dark));
        root.addView(status);

        // This button disappears as soon as Android confirms that this is the default SMS app.
        setupButton = new Button(this);
        setupButton.setText("CONFIGURAR COMO APP SMS PREDETERMINADA");
        setupButton.setOnClickListener(v -> requestRoleAndPermissionsIfNeeded());
        root.addView(setupButton);

        note = new TextView(this);
        note.setText("Cada SMS entrante se elimina cinco minutos después de recibirse. Toca un SMS y usa Conservar si no quieres que se borre.");
        note.setPadding(0, dp(4), 0, dp(4));
        note.setTextColor(ThemeColors.secondaryText(dark));
        root.addView(note);

        themeButton = new Button(this);
        themeButton.setText(dark ? "USAR MODO CLARO" : "USAR MODO OSCURO");
        themeButton.setOnClickListener(v -> {
            AppState.setDarkMode(this, !dark);
            recreate();
        });
        root.addView(themeButton);

        ListView list = new ListView(this);
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, new ArrayList<>()) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView row = (TextView) super.getView(position, convertView, parent);
                row.setTextColor(ThemeColors.primaryText(dark));
                row.setTextSize(16);
                row.setPadding(dp(12), dp(12), dp(12), dp(12));
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
        setContentView(root);
    }

    private void requestRoleAndPermissionsIfNeeded() {
        if (!isDefaultSmsApp()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                RoleManager roles = getSystemService(RoleManager.class);
                if (roles != null && roles.isRoleAvailable(RoleManager.ROLE_SMS)) {
                    startActivityForResult(roles.createRequestRoleIntent(RoleManager.ROLE_SMS), REQUEST_SMS_ROLE);
                }
            } else {
                Intent change = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
                change.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, getPackageName());
                startActivityForResult(change, REQUEST_SMS_ROLE);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && checkSelfPermission(Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{
                    Manifest.permission.RECEIVE_SMS,
                    Manifest.permission.READ_SMS,
                    Manifest.permission.SEND_SMS
            }, REQUEST_PERMISSIONS);
        }
        updateStatus();
    }

    private boolean isDefaultSmsApp() {
        return getPackageName().equals(Telephony.Sms.getDefaultSmsPackage(this));
    }

    private void updateStatus() {
        if (status == null) return;
        boolean isDefault = isDefaultSmsApp();
        if (isDefault) {
            status.setText("Estado: app SMS predeterminada. El borrado automático está activo.");
            setupButton.setVisibility(View.GONE);
        } else {
            status.setText("Estado: falta elegir esta app como aplicación SMS predeterminada.");
            setupButton.setVisibility(View.VISIBLE);
        }
    }

    private void refresh() {
        updateStatus();
        List<SmsStore.SmsItem> all = SmsStore.allMessages(this);
        shownItems.clear();
        shownItems.addAll(all);
        List<String> labels = new ArrayList<>();
        DateFormat format = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        for (SmsStore.SmsItem item : all) {
            String direction = item.type == SmsStore.TYPE_SENT ? "Tú → " : "← ";
            labels.add(direction + item.address + "\n" + item.body + "\n" + format.format(item.date));
        }
        adapter.clear();
        adapter.addAll(labels);
        adapter.notifyDataSetChanged();
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
