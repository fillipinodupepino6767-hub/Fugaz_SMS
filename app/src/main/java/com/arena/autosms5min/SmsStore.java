package com.arena.autosms5min;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;

import java.util.ArrayList;
import java.util.List;

final class SmsStore {
    static final int TYPE_INBOX = 1;
    static final int TYPE_SENT = 2;

    private SmsStore() { }

    static long insertIncoming(Context context, String address, String body, long timestamp) {
        ContentValues values = commonValues(address, body, timestamp, TYPE_INBOX);
        values.put(Telephony.TextBasedSmsColumns.READ, 0);
        values.put(Telephony.TextBasedSmsColumns.SEEN, 0);
        Uri uri = context.getContentResolver().insert(Telephony.Sms.Inbox.CONTENT_URI, values);
        return uri == null ? -1L : ContentUris.parseId(uri);
    }

    static void insertSent(Context context, String address, String body, long timestamp) {
        ContentValues values = commonValues(address, body, timestamp, TYPE_SENT);
        values.put(Telephony.TextBasedSmsColumns.READ, 1);
        values.put(Telephony.TextBasedSmsColumns.SEEN, 1);
        context.getContentResolver().insert(Telephony.Sms.Sent.CONTENT_URI, values);
    }

    static int delete(Context context, long id) {
        Uri uri = ContentUris.withAppendedId(Telephony.Sms.CONTENT_URI, id);
        return context.getContentResolver().delete(uri, null, null);
    }

    static List<SmsItem> allMessages(Context context) {
        return read(context, null, null, Telephony.TextBasedSmsColumns.DATE + " DESC");
    }

    static List<SmsItem> messagesForAddress(Context context, String address) {
        return read(context, Telephony.TextBasedSmsColumns.ADDRESS + "=?", new String[]{address},
                Telephony.TextBasedSmsColumns.DATE + " ASC");
    }

    private static List<SmsItem> read(Context context, String selection, String[] args, String order) {
        List<SmsItem> result = new ArrayList<>();
        String[] projection = {
                Telephony.TextBasedSmsColumns._ID,
                Telephony.TextBasedSmsColumns.ADDRESS,
                Telephony.TextBasedSmsColumns.BODY,
                Telephony.TextBasedSmsColumns.DATE,
                Telephony.TextBasedSmsColumns.TYPE
        };
        try (Cursor cursor = context.getContentResolver().query(
                Telephony.Sms.CONTENT_URI, projection, selection, args, order)) {
            if (cursor == null) return result;
            int idIndex = cursor.getColumnIndexOrThrow(Telephony.TextBasedSmsColumns._ID);
            int addressIndex = cursor.getColumnIndexOrThrow(Telephony.TextBasedSmsColumns.ADDRESS);
            int bodyIndex = cursor.getColumnIndexOrThrow(Telephony.TextBasedSmsColumns.BODY);
            int dateIndex = cursor.getColumnIndexOrThrow(Telephony.TextBasedSmsColumns.DATE);
            int typeIndex = cursor.getColumnIndexOrThrow(Telephony.TextBasedSmsColumns.TYPE);
            while (cursor.moveToNext()) {
                result.add(new SmsItem(cursor.getLong(idIndex), cursor.getString(addressIndex),
                        cursor.getString(bodyIndex), cursor.getLong(dateIndex), cursor.getInt(typeIndex)));
            }
        } catch (SecurityException ignored) {
            // The UI will remain empty until the user grants the SMS role and permissions.
        }
        return result;
    }

    private static ContentValues commonValues(String address, String body, long timestamp, int type) {
        ContentValues values = new ContentValues();
        values.put(Telephony.TextBasedSmsColumns.ADDRESS, address);
        values.put(Telephony.TextBasedSmsColumns.BODY, body);
        values.put(Telephony.TextBasedSmsColumns.DATE, timestamp);
        values.put(Telephony.TextBasedSmsColumns.DATE_SENT, timestamp);
        values.put(Telephony.TextBasedSmsColumns.TYPE, type);
        return values;
    }

    static final class SmsItem {
        final long id;
        final String address;
        final String body;
        final long date;
        final int type;

        SmsItem(long id, String address, String body, long date, int type) {
            this.id = id;
            this.address = address == null ? "Desconocido" : address;
            this.body = body == null ? "" : body;
            this.date = date;
            this.type = type;
        }
    }
}
