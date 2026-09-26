package com.arena.autosms5min;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.provider.ContactsContract;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Contact display names (and photos) for phone numbers, with small in-memory
 * caches so the 1-second UI tickers stay cheap. Without READ_CONTACTS, or when
 * no contact matches, the raw address is shown. Nothing is stored on disk.
 */
final class ContactNames {
    private static final int MAX_CACHED_NAMES = 300;
    private static final int MAX_CACHED_PHOTOS = 60;
    private static final Map<String, String> NAME_CACHE =
            new LinkedHashMap<String, String>(64, .75f, true) {
                @Override protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                    return size() > MAX_CACHED_NAMES;
                }
            };
    private static final Map<String, Bitmap> PHOTO_CACHE =
            new LinkedHashMap<String, Bitmap>(16, .75f, true) {
                @Override protected boolean removeEldestEntry(Map.Entry<String, Bitmap> eldest) {
                    return size() > MAX_CACHED_PHOTOS;
                }
            };
    private static final int[] AVATAR_COLORS = {
            Color.rgb(0, 150, 136), Color.rgb(33, 150, 243), Color.rgb(63, 81, 181),
            Color.rgb(156, 39, 176), Color.rgb(233, 30, 99), Color.rgb(244, 67, 54),
            Color.rgb(255, 152, 0), Color.rgb(76, 175, 80), Color.rgb(121, 85, 72),
            Color.rgb(96, 125, 139), Color.rgb(0, 188, 212), Color.rgb(139, 195, 74)
    };

    private ContactNames() { }

    /** Display name for an address, or the address itself when unknown. Never null. */
    static String displayName(Context context, String address) {
        String fallback = (address == null || address.isEmpty()) ? "Desconocido" : address;
        if (address == null || address.isEmpty()) return fallback;
        synchronized (NAME_CACHE) {
            String cached = NAME_CACHE.get(address);
            if (cached != null) return cached;
        }
        String result = fallback;
        boolean allowed = SetupHelper.hasContactsPermission(context);
        if (allowed) {
            try {
                Uri filter = Uri.withAppendedPath(
                        ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(address));
                try (Cursor cursor = context.getContentResolver().query(filter,
                        new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME}, null, null, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        String found = cursor.getString(0);
                        if (found != null && !found.trim().isEmpty()) result = found.trim();
                    }
                }
            } catch (Exception ignored) {
                // Fall back to the raw address; contacts are cosmetic only.
            }
            synchronized (NAME_CACHE) {
                NAME_CACHE.put(address, result);
            }
        }
        return result;
    }

    /** "Name\nnumber" when a contact matches, otherwise just the address. */
    static String twoLineLabel(Context context, String address) {
        String name = displayName(context, address);
        String plain = (address == null || address.isEmpty()) ? "Desconocido" : address;
        return name.equals(plain) ? plain : name + "\n" + plain;
    }

    /** "Name (number)" when a contact matches, otherwise just the address. */
    static String singleLineLabel(Context context, String address) {
        String name = displayName(context, address);
        String plain = (address == null || address.isEmpty()) ? "Desconocido" : address;
        return name.equals(plain) ? plain : name + " (" + plain + ")";
    }

    /** Contact photo thumbnail, or null. Cached positives only. */
    static Bitmap photo(Context context, String address) {
        if (address == null || address.isEmpty()) return null;
        synchronized (PHOTO_CACHE) {
            Bitmap cached = PHOTO_CACHE.get(address);
            if (cached != null) return cached;
        }
        if (!SetupHelper.hasContactsPermission(context)) return null;
        Bitmap result = null;
        try {
            Uri filter = Uri.withAppendedPath(
                    ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(address));
            try (Cursor cursor = context.getContentResolver().query(filter,
                    new String[]{ContactsContract.PhoneLookup._ID}, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    long id = cursor.getLong(0);
                    Uri contactUri = ContentUris.withAppendedId(
                            ContactsContract.Contacts.CONTENT_URI, id);
                    try (InputStream in = ContactsContract.Contacts.openContactPhotoInputStream(
                            context.getContentResolver(), contactUri, true)) {
                        if (in != null) result = BitmapFactory.decodeStream(in);
                    }
                }
            }
        } catch (Exception ignored) {
            result = null;
        }
        if (result != null) {
            synchronized (PHOTO_CACHE) {
                PHOTO_CACHE.put(address, result);
            }
        }
        return result;
    }

    /** Stable avatar color for any key. */
    static int avatarColor(String key) {
        if (key == null || key.isEmpty()) return AVATAR_COLORS[0];
        return AVATAR_COLORS[Math.abs(key.hashCode()) % AVATAR_COLORS.length];
    }

    /** First letter for avatar circles, or "#" when there is none usable. */
    static String avatarLetter(String name) {
        if (name == null) return "#";
        String trimmed = name.trim();
        if (trimmed.isEmpty()) return "#";
        return trimmed.substring(0, 1).toUpperCase();
    }

    /** Circle bitmap with the initial, used when no contact photo exists. */
    static Bitmap letterBitmap(String name, int sizePx) {
        int safeSize = Math.max(8, sizePx);
        Bitmap bitmap = Bitmap.createBitmap(safeSize, safeSize, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        fill.setColor(avatarColor(name));
        float radius = safeSize / 2f;
        canvas.drawCircle(radius, radius, radius, fill);
        Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
        text.setColor(Color.WHITE);
        text.setTextSize(safeSize * 0.45f);
        text.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        text.setTextAlign(Paint.Align.CENTER);
        Paint.FontMetrics metrics = text.getFontMetrics();
        float baseline = radius - (metrics.ascent + metrics.descent) / 2f;
        canvas.drawText(avatarLetter(name), radius, baseline, text);
        return bitmap;
    }
}
