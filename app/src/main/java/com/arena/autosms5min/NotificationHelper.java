package com.arena.autosms5min;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;

import java.text.DateFormat;

final class NotificationHelper {
    private static final String CHANNEL_ID = "incoming_sms";
    private static final String DELETION_CHANNEL_ID = "deleted_sms";
    private static final String WATCH_CHANNEL_ID = "ringer_watch_min";
    private static final int TEST_NOTIFICATION_ID = 987654;
    private static final int WATCH_PROMPT_ID = 610027;
    private static final int WATCH_ARMED_ID = 610028;
    private static final int WATCH_SERVICE_ID = 610026;
    private static final int WATCH_ARM_CODE = 610031;
    private static final int WATCH_OPEN_CODE = 610032;

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
        MessageClassifier.Result classification = MessageClassifier.classify(context, body);
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

    /** Confirmation posted when the silence timer restores the ringer. */
    static void showRingerRestored(Context context) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;
        createChannels(manager);
        Intent open = new Intent(context, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 610025, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        String message = "El temporizador de silencio terminó: el sonido del teléfono está activado de nuevo.";
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(context, CHANNEL_ID)
                : new Notification.Builder(context);
        builder.setSmallIcon(android.R.drawable.sym_action_chat)
                .setContentTitle("Sonido restaurado 🔊")
                .setContentText(message)
                .setStyle(new Notification.BigTextStyle().bigText(message))
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_ALL)
                .setWhen(System.currentTimeMillis());
        manager.notify(610025, builder.build());
    }

    /**
     * Prompt posted when another app or the system silences the phone.
     * One tap arms the timer with the pre-configured default time.
     */
    static void showRingerWatchPrompt(Context context, boolean vibrate) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;
        createChannels(manager);
        String label = AppState.watchMinutesLabel(context);
        String title = vibrate ? "Se detectó vibración 📳" : "Se detectó silencio 🔇";
        String message = (vibrate ? "El teléfono pasó a vibración" : "El teléfono pasó a silencio")
                + " (otra app o el sistema). ¿Activo el temporizador de " + label
                + " para que el sonido vuelva solo?";
        Intent open = new Intent(context, SilenceTimerActivity.class);
        PendingIntent openIntent = PendingIntent.getActivity(context, WATCH_OPEN_CODE, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Intent arm = new Intent(context, RingerWatchActionReceiver.class)
                .setAction(RingerWatchActionReceiver.ACTION_ARM_DEFAULT);
        PendingIntent armIntent = PendingIntent.getBroadcast(context, WATCH_ARM_CODE, arm,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(context, CHANNEL_ID)
                : new Notification.Builder(context);
        builder.setSmallIcon(android.R.drawable.sym_action_chat)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new Notification.BigTextStyle().bigText(message))
                .setContentIntent(openIntent)
                .addAction(new Notification.Action.Builder(
                        android.R.drawable.ic_menu_save,
                        "ACTIVAR (" + label.toUpperCase() + ")", armIntent).build())
                .addAction(new Notification.Action.Builder(
                        android.R.drawable.ic_menu_edit, "ELEGIR TIEMPO", openIntent).build())
                .setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_ALL)
                .setWhen(System.currentTimeMillis());
        manager.notify(WATCH_PROMPT_ID, builder.build());
    }

    static void cancelWatchPrompt(Context context) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) manager.cancel(WATCH_PROMPT_ID);
    }

    /** Confirmation posted when the timer is armed from the watch prompt. */
    static void showWatchArmed(Context context, long restoreAt) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;
        createChannels(manager);
        String when = DateFormat.getTimeInstance(DateFormat.SHORT).format(restoreAt);
        String message = "El sonido volverá solo a las " + when + ". Toca para ver o cancelar el temporizador.";
        Intent open = new Intent(context, SilenceTimerActivity.class);
        PendingIntent openIntent = PendingIntent.getActivity(context, WATCH_OPEN_CODE, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(context, CHANNEL_ID)
                : new Notification.Builder(context);
        builder.setSmallIcon(android.R.drawable.sym_action_chat)
                .setContentTitle("Temporizador activado ⏳")
                .setContentText(message)
                .setStyle(new Notification.BigTextStyle().bigText(message))
                .setContentIntent(openIntent)
                .setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_ALL)
                .setWhen(System.currentTimeMillis());
        manager.notify(WATCH_ARMED_ID, builder.build());
    }

    /** Quiet persistent notification required while the ringer watch service runs. */
    static Notification watchServiceNotification(Context context) {
        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) createChannels(manager);
        Intent open = new Intent(context, SilenceTimerActivity.class);
        PendingIntent openIntent = PendingIntent.getActivity(context, WATCH_SERVICE_ID, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(context, WATCH_CHANNEL_ID)
                : new Notification.Builder(context);
        builder.setSmallIcon(android.R.drawable.sym_action_chat)
                .setContentTitle("Vigilando modo de sonido")
                .setContentText("Toca para abrir el temporizador de silencio.")
                .setPriority(Notification.PRIORITY_MIN)
                .setContentIntent(openIntent)
                .setOngoing(true)
                .setWhen(System.currentTimeMillis());
        return builder.build();
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
            NotificationChannel watch = new NotificationChannel(WATCH_CHANNEL_ID,
                    "Vigilancia de sonido", NotificationManager.IMPORTANCE_MIN);
            watch.setDescription("Aviso permanente mientras se vigila el modo de sonido");
            manager.createNotificationChannel(watch);
        }
    }

    private static int notificationId(long smsId) {
        return (int) (smsId ^ (smsId >>> 32));
    }

    private static int deletionNotificationId(long smsId) {
        return notificationId(smsId) ^ 0x4B1E2D;
    }
}
