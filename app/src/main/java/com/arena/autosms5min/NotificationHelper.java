package com.arena.autosms5min;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;

final class NotificationHelper {
    private static final String CHANNEL_ID = "incoming_sms";
    private static final String DELETION_CHANNEL_ID = "deleted_sms";
    private static final int TEST_NOTIFICATION_ID = 987654;

    private NotificationHelper() { }

    /** Creates the channels early so the system notification screen lists them. */
    static void ensureChannels(Context context) {
        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) createChannels(manager);
    }

    static boolean areEnabled(Context context) {
        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        return manager != null && manager.areNotificationsEnabled();
    }

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
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(context, CHANNEL_ID)
                : new Notification.Builder(context);
        String name = ContactNames.displayName(context, address);
        builder.setSmallIcon(android.R.drawable.sym_action_chat)
                .setContentTitle(name)
                .setContentText(body)
                .setStyle(new Notification.BigTextStyle().bigText(body))
                .setContentIntent(contentIntent)
                .addAction(new Notification.Action.Builder(
                        android.R.drawable.ic_menu_save, "Conservar", keepIntent).build())
                .setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_ALL)
                .setWhen(System.currentTimeMillis());
        if (!name.equals(address)) builder.setSubText(address);
        Bitmap face = ContactNames.photo(context, address);
        if (face != null) builder.setLargeIcon(face);
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
        if (MessageClassifier.LABEL_IMPORTANT.equals(classification.label)) {
            title = "Mensaje eliminado: podría ser importante";
            message = "Revísalo en Eliminados recientemente antes de que venza su historial.";
        } else if (MessageClassifier.LABEL_SPAM.equals(classification.label)) {
            title = "Mensaje de posible spam eliminado correctamente";
            message = "La etiqueta es orientativa; puedes revisar su vista previa si lo necesitas.";
        } else {
            title = "Mensaje eliminado automáticamente";
            message = "Se eliminó según el tiempo de retención elegido.";
        }
        Intent history = new Intent(context, DeletedHistoryActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(context, deletionNotificationId(smsId), history,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(context, DELETION_CHANNEL_ID)
                : new Notification.Builder(context);
        builder.setSmallIcon(android.R.drawable.ic_menu_delete)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new Notification.BigTextStyle().bigText(message))
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_ALL)
                .setWhen(System.currentTimeMillis());
        manager.notify(deletionNotificationId(smsId), builder.build());
    }

    /**
     * Manual sound check from Settings. If this is heard, the app's channel is
     * fine and any silence comes from system volume, DND or another app.
     */
    static void showTest(Context context) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;
        createChannels(manager);
        Intent open = new Intent(context, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(context, TEST_NOTIFICATION_ID, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(context, CHANNEL_ID)
                : new Notification.Builder(context);
        String message = "Si escuchas esto, las notificaciones de Fugaz SMS suenan bien. "
                + "Si no suena, revisa el volumen, el modo No molestar u otras apps de volumen.";
        builder.setSmallIcon(android.R.drawable.sym_action_chat)
                .setContentTitle("Prueba de sonido 🔊")
                .setContentText(message)
                .setStyle(new Notification.BigTextStyle().bigText(message))
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_ALL)
                .setWhen(System.currentTimeMillis());
        manager.notify(TEST_NOTIFICATION_ID, builder.build());
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
