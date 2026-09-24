package com.arena.autosms5min;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

final class NotificationHelper {
    private static final String CHANNEL_ID = "incoming_sms";
    private static final String DELETION_CHANNEL_ID = "deleted_sms";

    private NotificationHelper() { }

    static void showIncoming(Context context, long smsId, String address, String body) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;
        createChannels(manager);
        Intent open = new Intent(context, ConversationActivity.class)
                .putExtra(ConversationActivity.EXTRA_ADDRESS, address);
        PendingIntent contentIntent = PendingIntent.getActivity(context, notificationId(smsId), open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Intent keep = new Intent(context, KeepSmsReceiver.class)
                .setAction(KeepSmsReceiver.ACTION_KEEP)
                .putExtra(KeepSmsReceiver.EXTRA_SMS_ID, smsId);
        PendingIntent keepIntent = PendingIntent.getBroadcast(context, notificationId(smsId), keep,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        android.app.Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new android.app.Notification.Builder(context, CHANNEL_ID)
                : new android.app.Notification.Builder(context);
        builder.setSmallIcon(android.R.drawable.sym_action_chat)
                .setContentTitle(address == null ? "SMS recibido" : address)
                .setContentText(body)
                .setStyle(new android.app.Notification.BigTextStyle().bigText(body))
                .setContentIntent(contentIntent)
                .addAction(new android.app.Notification.Action.Builder(
                        android.R.drawable.ic_menu_save, "Conservar", keepIntent).build())
                .setAutoCancel(true)
                .setWhen(System.currentTimeMillis());
        manager.notify(notificationId(smsId), builder.build());
    }

    /** Posts a privacy-preserving status notification after an automatic SMS deletion. */
    static void showAutomaticDeletion(Context context, long smsId, String body) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;
        createChannels(manager);
        MessageClassifier.Result classification = MessageClassifier.classify(body);
        String title;
        String message;
        if ("Importante".equals(classification.label)) {
            title = "Mensaje eliminado: podría ser importante";
            message = "Revísalo en Eliminados recientemente antes de que venza su historial.";
        } else if ("Posible spam".equals(classification.label)) {
            title = "Mensaje de posible spam eliminado correctamente";
            message = "La etiqueta es orientativa; puedes revisar su vista previa si lo necesitas.";
        } else {
            title = "Mensaje eliminado automáticamente";
            message = "Se eliminó según el tiempo de retención elegido.";
        }
        Intent history = new Intent(context, DeletedHistoryActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(context, deletionNotificationId(smsId), history,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        android.app.Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new android.app.Notification.Builder(context, DELETION_CHANNEL_ID)
                : new android.app.Notification.Builder(context);
        builder.setSmallIcon(android.R.drawable.ic_menu_delete)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new android.app.Notification.BigTextStyle().bigText(message))
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setWhen(System.currentTimeMillis());
        manager.notify(deletionNotificationId(smsId), builder.build());
    }

    static void cancel(Context context, long smsId) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) manager.cancel(notificationId(smsId));
    }

    private static void createChannels(NotificationManager manager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel incoming = new NotificationChannel(CHANNEL_ID,
                    "SMS entrantes", NotificationManager.IMPORTANCE_DEFAULT);
            incoming.setDescription("Avisos temporales de SMS entrantes");
            manager.createNotificationChannel(incoming);
            NotificationChannel deleted = new NotificationChannel(DELETION_CHANNEL_ID,
                    "SMS eliminados", NotificationManager.IMPORTANCE_DEFAULT);
            deleted.setDescription("Avisos después de eliminar un SMS temporal automáticamente");
            manager.createNotificationChannel(deleted);
        }
    }

    private static int notificationId(long smsId) {
        return (int) (smsId ^ (smsId >>> 32));
    }

    private static int deletionNotificationId(long smsId) {
        return notificationId(smsId) ^ 0x4B1E2D;
    }
}
