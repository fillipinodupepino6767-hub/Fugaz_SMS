package com.arena.autosms5min;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Reads active cellular subscriptions; phone numbers depend on carrier/SIM availability. */
final class SimInfo {
    private SimInfo() { }

    static boolean hasPermission(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true;
        return context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
                && (Build.VERSION.SDK_INT < Build.VERSION_CODES.O
                || context.checkSelfPermission(Manifest.permission.READ_PHONE_NUMBERS) == PackageManager.PERMISSION_GRANTED);
    }

    static int phoneSlotCount(Context context) {
        TelephonyManager telephony = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (telephony == null) return 0;
        try {
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? telephony.getPhoneCount() : 1;
        } catch (SecurityException ignored) {
            return 0;
        }
    }

    static List<Card> activeCards(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1 || !hasPermission(context)) {
            return Collections.emptyList();
        }
        try {
            SubscriptionManager manager = (SubscriptionManager) context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
            if (manager == null) return Collections.emptyList();
            List<SubscriptionInfo> subscriptions = manager.getActiveSubscriptionInfoList();
            if (subscriptions == null) return Collections.emptyList();
            List<Card> cards = new ArrayList<>();
            for (SubscriptionInfo info : subscriptions) {
                String name = info.getDisplayName() == null ? "SIM " + (info.getSimSlotIndex() + 1)
                        : info.getDisplayName().toString();
                String carrier = info.getCarrierName() == null ? "Operador no disponible"
                        : info.getCarrierName().toString();
                String number = info.getNumber();
                if (number == null || number.trim().isEmpty()) {
                    TelephonyManager telephony = ((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE))
                            .createForSubscriptionId(info.getSubscriptionId());
                    number = telephony.getLine1Number();
                }
                if (number == null || number.trim().isEmpty()) number = "No disponible en la SIM";
                cards.add(new Card(info.getSubscriptionId(), info.getSimSlotIndex(), name, carrier, number));
            }
            return cards;
        } catch (SecurityException ignored) {
            return Collections.emptyList();
        }
    }

    static final class Card {
        final int subscriptionId;
        final int slotIndex;
        final String name;
        final String carrier;
        final String number;

        Card(int subscriptionId, int slotIndex, String name, String carrier, String number) {
            this.subscriptionId = subscriptionId;
            this.slotIndex = slotIndex;
            this.name = name;
            this.carrier = carrier;
            this.number = number;
        }

        String label() {
            return "SIM " + (slotIndex + 1) + " · " + name + "\nOperador: " + carrier + "\nNúmero: " + number;
        }
    }
}
