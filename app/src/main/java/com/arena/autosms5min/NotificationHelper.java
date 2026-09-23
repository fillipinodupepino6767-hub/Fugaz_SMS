package com.arena.autosms5min;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

final class NotificationHelper {
    private static final String CHANNEL_ID = "incoming_sms";

    private NotificationHelper() { }

    static void showIncoming(Context context, long smsId, String address, String body) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;
        createChannel(manager);
        Intent open = new Intent(context, ConversationActivity.class)
                .putExtra(ConversationActivity.EXTRA_ADDRESS, address);
        PendingIntent contentIntent = PendingIntent.getActivity(context, notificationId(smsId), open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        android.app.Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new android.app.Notification.Builder(context, CHANNEL_ID)
                : new android.app.Notification.Builder(context);
        builder.setSmallIcon(android.R.drawable.sym_action_chat)
                .setContentTitle(address == null ? "SMS recibido" : address)
                .setContentText(body)
                .setStyle(new android.app.Notification.BigTextStyle().bigText(body))
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setWhen(System.currentTimeMillis());
        manager.notify(notificationId(smsId), builder.build());
    }

    static void cancel(Context context, long smsId) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) manager.cancel(notificationId(smsId));
    }

    private static void createChannel(NotificationManager manager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "SMS entrantes", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Avisos temporales de SMS entrantes");
            manager.createNotificationChannel(channel);
        }
    }

    private static int notificationId(long smsId) {
        return (int) (smsId ^ (smsId >>> 32));
    }
}
