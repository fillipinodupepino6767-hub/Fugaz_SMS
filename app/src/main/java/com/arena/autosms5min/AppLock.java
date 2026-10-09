package com.arena.autosms5min;

import android.app.Activity;
import android.hardware.biometrics.BiometricPrompt;
import android.os.Build;
import android.os.CancellationSignal;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

/**
 * Optional app PIN lock (privacy for the inbox). The PIN is stored only as a
 * salted SHA-256 hash; a cover view painted over the window hides all content
 * while the prompt is up, and cancelling closes the activity instead of
 * showing anything. Unlocking can optionally use the fingerprint sensor
 * (Android 9+): the fingerprint is a separate opt-in that starts disabled and
 * only turns on when the user confirms it with their own finger. The PIN
 * always remains as fallback.
 */
final class AppLock {
    private static final String PREFS = "app_lock";
    private static final String KEY_HASH = "pin_hash";
    private static final String KEY_SALT = "salt";
    private static final String KEY_ON = "lock_on";
    private static final String KEY_BIO = "bio_on";

    /** Why a fingerprint attempt did not succeed ("" = user simply backed out). */
    interface BioFail {
        void fail(String reason);
    }

    private AppLock() { }

    static boolean isSet(Activity activity) {
        return prefs(activity).contains(KEY_HASH);
    }

    static boolean enabled(Activity activity) {
        return isSet(activity) && prefs(activity).getBoolean(KEY_ON, false);
    }

    static void setPin(Activity activity, String pin) {
        String salt = UUID.randomUUID().toString();
        prefs(activity).edit()
                .putString(KEY_SALT, salt)
                .putString(KEY_HASH, sha256(salt, pin))
                .putBoolean(KEY_ON, true)
                .apply();
    }

    static void setEnabled(Activity activity, boolean on) {
        prefs(activity).edit().putBoolean(KEY_ON, on).apply();
    }

    /** Fingerprint unlock: a separate choice, off until the user enables it. */
    static boolean bioEnabled(Activity activity) {
        return isSet(activity) && prefs(activity).getBoolean(KEY_BIO, false);
    }

    static void setBio(Activity activity, boolean on) {
        prefs(activity).edit().putBoolean(KEY_BIO, on).apply();
    }

    static void clear(Activity activity) {
        prefs(activity).edit()
                .remove(KEY_HASH)
                .remove(KEY_SALT)
                .remove(KEY_ON)
                .remove(KEY_BIO)
                .apply();
    }

    static boolean check(Activity activity, String pin) {
        String stored = prefs(activity).getString(KEY_HASH, null);
        String salt = prefs(activity).getString(KEY_SALT, "");
        if (stored == null || pin == null || pin.isEmpty()) return false;
        return stored.equals(sha256(salt, pin));
    }

    /** Cheap sanity check for user-entered PINs (digits, 4–10 long). */
    static boolean pinFormatOk(String pin) {
        return pin != null && pin.matches("\\d{4,10}");
    }

    /** Full-screen cover painted over the window; GONE until locking. */
    static View installCover(Activity activity) {
        View cover = new View(activity);
        cover.setBackgroundColor(ThemeColors.background(AppState.isDarkMode(activity)));
        ((ViewGroup) activity.getWindow().getDecorView()).addView(cover,
                new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));
        cover.setVisibility(View.GONE);
        return cover;
    }

    /**
     * Shows the cover and asks for the unlock: fingerprint first when the
     * user opted into it, PIN otherwise. onOk runs after a success; cancelling
     * (button or back) finishes the activity — content never shows.
     */
    static void ask(final Activity activity, final View cover, final Runnable onOk) {
        cover.setVisibility(View.VISIBLE);
        if (bioEnabled(activity) && promptBio(activity,
                "Fugaz SMS bloqueada 🔒",
                "Confirma tu huella para abrir la app, o toca «Usar PIN».",
                "Usar PIN",
                new Runnable() {
                    @Override
                    public void run() {
                        cover.setVisibility(View.GONE);
                        if (onOk != null) onOk.run();
                    }
                },
                reason -> askPin(activity, cover, onOk))) {
            return;
        }
        askPin(activity, cover, onOk);
    }

    /** PIN dialog over the cover; wrong PIN retries, cancelling finishes. */
    private static void askPin(final Activity activity, final View cover, final Runnable onOk) {
        if (activity.isFinishing() || activity.isDestroyed()) return;
        cover.setVisibility(View.VISIBLE);
        ThemedDialog.input(activity, "Fugaz SMS bloqueada 🔒",
                "Escribe tu PIN para abrir la app.",
                "",
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIANT_PASSWORD,
                null, "Salir", "Entrar",
                null,
                value -> {
                    if (check(activity, value)) {
                        cover.setVisibility(View.GONE);
                        if (onOk != null) onOk.run();
                    } else {
                        Toast.makeText(activity, "PIN incorrecto. Intenta de nuevo.",
                                Toast.LENGTH_SHORT).show();
                        askPin(activity, cover, onOk);
                    }
                },
                () -> activity.finish());
    }

    /**
     * Shows the system fingerprint dialog (Android 9+). Returns false when
     * the phone/OS cannot show it, so callers fall back straight to the PIN.
     * onOk runs on success; onFail carries a short reason ("" when the user
     * just cancelled) so the caller decides what to do next.
     */
    static boolean promptBio(final Activity activity, String title, String description,
            String negativeText, Runnable onOk, BioFail onFail) {
        if (Build.VERSION.SDK_INT < 28) return false;
        try {
            final boolean[] done = {false};
            CancellationSignal signal = new CancellationSignal();
            BiometricPrompt prompt = new BiometricPrompt.Builder(activity)
                    .setTitle(title)
                    .setDescription(description)
                    .setNegativeButton(negativeText, activity.getMainExecutor(),
                            (dialog, which) -> {
                                if (done[0]) return;
                                done[0] = true;
                                if (!activity.isFinishing() && !activity.isDestroyed()) {
                                    onFail.fail("");
                                }
                            })
                    .build();
            prompt.authenticate(signal, activity.getMainExecutor(),
                    new BiometricPrompt.AuthenticationCallback() {
                        @Override
                        public void onAuthenticationSucceeded(
                                BiometricPrompt.AuthenticationResult result) {
                            if (done[0]) return;
                            done[0] = true;
                            if (!activity.isFinishing() && !activity.isDestroyed()) {
                                onOk.run();
                            }
                        }

                        @Override
                        public void onAuthenticationError(int code, CharSequence msg) {
                            if (done[0]) return;
                            done[0] = true;
                            if (activity.isFinishing() || activity.isDestroyed()) return;
                            String reason = "";
                            if (code == BiometricPrompt.BIOMETRIC_ERROR_NO_BIOMETRICS) {
                                reason = "no hay huellas registradas en este teléfono";
                            } else if (code == BiometricPrompt.BIOMETRIC_ERROR_HW_NOT_PRESENT) {
                                reason = "este teléfono no tiene sensor de huella";
                            } else if (code == BiometricPrompt.BIOMETRIC_ERROR_HW_UNAVAILABLE) {
                                reason = "el sensor de huella no está disponible ahora";
                            } else if (code == BiometricPrompt.BIOMETRIC_ERROR_LOCKOUT
                                    || code == BiometricPrompt.BIOMETRIC_ERROR_LOCKOUT_PERMANENT) {
                                reason = "huella bloqueada por intentos fallidos";
                            } else if (code == BiometricPrompt.BIOMETRIC_ERROR_UNABLE_TO_PROCESS
                                    || code == BiometricPrompt.BIOMETRIC_ERROR_TIMEOUT) {
                                reason = "no se pudo leer la huella";
                            }
                            onFail.fail(reason);
                        }
                    });
            return true;
        } catch (Throwable unavailable) {
            return false;
        }
    }

    private static android.content.SharedPreferences prefs(Activity activity) {
        return activity.getSharedPreferences(PREFS, Activity.MODE_PRIVATE);
    }

    private static String sha256(String salt, String pin) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((salt + ":" + pin).getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (Exception impossible) {
            return "";
        }
    }
}
