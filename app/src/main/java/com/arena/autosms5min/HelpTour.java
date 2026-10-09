package com.arena.autosms5min;

import android.app.Activity;
import android.content.Intent;

/**
 * One-time welcome plus a guided help tour: floating windows pinned to the
 * bottom of the screen. When hosted by Settings, each step scrolls the screen
 * behind to the section it explains and flashes it, so the window not only
 * talks about a setting but points at it. ◀ goes back to the previous tip,
 * reopening the tour resumes on the last section shown.
 *
 * The welcome appears once per app version (first install or after an update)
 * so someone who already opened an older build sees it again when updating.
 */
final class HelpTour {

    private HelpTour() { }

    /** Implemented by the screen behind the tour: scroll to + flash a section. */
    interface Host {
        void showStepAt(String anchorKey, int index);
    }

    /** Steps: [title, body, anchor key in Settings]. */
    private static final String[][] STEPS = {
        {"⚙ Empezar aquí",
            "La configuración automática (Configuración → ①) hace 5 pasos en orden:\n\n"
                + "1️⃣ Permitir ajustes restringidos (solo Android 15/16 con APK instalado: "
                + "te lleva a la info de la app y el menú ⋮, con PIN o huella).\n"
                + "2️⃣ Permisos.\n"
                + "3️⃣ Notificaciones.\n"
                + "4️⃣ Alarmas exactas.\n"
                + "5️⃣ App SMS predeterminada.\n\n"
                + "Al volver de cada pantalla del sistema, la app continúa sola. "
                + "La pantalla de atrás (esta Configuración) se coloca sola en la sección "
                + "de la que hablo.",
            "setup"},
        {"① y ② en Configuración",
            "① CONFIGURACIÓN AUTOMÁTICA hace todo lo anterior en orden, sin que tengas que "
                + "buscar cada ajuste.\n\n"
                + "② CONFIGURAR COMO APP SMS PREDETERMINADA es solo el paso final: Android "
                + "preguntará «¿usar Fugaz SMS como app SMS?». Sin ese paso la app no recibe "
                + "mensajes.\n\n"
                + "Los dos botones están separados a propósito para que no se pisen.",
            "setup"},
        {"⏳ Bandeja temporal",
            "Tus SMS nuevos se borran solos al cumplir el tiempo elegido (TIEMPO DE BORRADO; "
                + "por defecto 5 minutos).\n\n"
                + "En AUTO-ELIMINACIÓN POR TIPO eliges qué se borra: normales, posible spam e "
                + "importantes. Los importantes NO se borran solos por defecto: así se protegen "
                + "códigos de banco y alertas.\n\n"
                + "Los mensajes que tú envías nunca se borran solos. En ⌛ Eliminados "
                + "recientemente revisas lo borrado antes de que desaparezca del todo.",
            "tipos"},
        {"📱 Solo SMS de texto",
            "Fugaz SMS no usa internet: recibe los SMS que llegan por la señal del celular.\n\n"
                + "Los chats por internet (Wi-Fi o datos) de Google Mensajes o iPhone NO pueden "
                + "llegar aquí: Google no da acceso a otras apps.\n\n"
                + "Si te escriben por chat y no lo ves: usa el botón de esta sección para "
                + "pasar Google Mensajes a «solo SMS».",
            "google"},
        {"👆 Bandeja: deslizar y toques",
            "Deslizar un mensaje a la derecha o izquierda lo borra o archiva, como en Gmail; "
                + "cada lado se configura por separado aquí abajo.\n\n"
                + "Mantén presionado un mensaje para abrirlo, archivarlo o eliminarlo.\n\n"
                + "📦 Archivados no se borran solos. 🔎 El buscador de la bandeja filtra por "
                + "nombre, número o texto.",
            "deslizar"},
        {"🔔 Avisos, silencio y vigilancia",
            "Si no escuchas mensajes: PROBAR SONIDO DE NOTIFICACIÓN (sección Permisos). Si la "
                + "prueba suena, el problema es del volumen o de otra app, no de Fugaz SMS.\n\n"
                + "⏱ TEMPORIZADOR DE SILENCIO (sección Sonido): silencia o vibra AHORA (útil en "
                + "el colegio) y el sonido vuelve solo al terminar el tiempo. También puedes "
                + "configurar el sonido al desactivarlo, o configurarlo manualmente.\n\n"
                + "👁 VIGILAR SILENCIO: si otra app o el sistema ponen el teléfono en silencio "
                + "o vibración, te llega un aviso para activar el temporizador de un toque. "
                + "No detecta «No molestar» puro.",
            "sonido"},
        {"🆘 Ayuda y saldo",
            "La barra amarilla de la bandeja siempre dice qué falta; tócala (o REPARAR) y vas "
                + "directo al paso pendiente.\n\n"
                + "DIAGNÓSTICO DE RECEPCIÓN = por qué no llegan mensajes.\n"
                + "CÓMO FUNCIONA = referencia completa de todo.\n\n"
                + "💰 SALDO DE MIS LÍNEAS: Kolbi *888# o SMS al 8888 · Movistar/Liberty SMS al "
                + "606 · Claro *611#.\n\n"
                + "Todo queda local en tu teléfono: sin cuentas, sin internet, sin publicidad.",
            "saldo"},
    };

    private static final String WELCOME =
            "Tu bandeja temporal de SMS: lo nuevo se borra solo al cumplir el tiempo que elijas "
                + "(por defecto 5 minutos). Solo SMS de texto, sin internet y sin cuentas.\n\n"
                + "IMPORTANTE: para recibir mensajes toca ⚙ Configuración → ① CONFIGURACIÓN "
                + "AUTOMÁTICA y sigue los 5 pasos. Android te pedirá «Permitir ajustes "
                + "restringidos» (con PIN o huella) si instalaste el APK.\n\n"
                + "«Ver la guía» abre 7 ventanitas que señalan cada ajuste en Configuración "
                "(con ← para volver atrás y ✕ para cerrar). El ❓ de la barra siempre la "
                + "vuelve a abrir en la sección donde la dejaste.";

    /** Start (or resume) the tour; each step is announced to the host. */
    static void start(final Activity activity, final Host host) {
        int saved = AppState.helpLastStep(activity);
        if (saved < 0 || saved >= STEPS.length) saved = 0;
        showStep(activity, saved, host);
    }

    /** Fallback when no host is available (windows only, no redirect). */
    static void start(Activity activity) {
        start(activity, null);
    }

    private static void showStep(final Activity activity, final int index, final Host host) {
        if (activity == null || activity.isFinishing()) return;
        AppState.setHelpLastStep(activity, index);
        if (host != null) host.showStepAt(STEPS[index][2], index);
        final boolean canBack = index > 0;
        final boolean last = index >= STEPS.length - 1;
        String title = STEPS[index][0] + "  ·  " + (index + 1) + "/" + STEPS.length;
        ThemedDialog.tourStep(activity, title, STEPS[index][1],
                canBack,
                last ? "Listo ✓" : "Siguiente →",
                canBack ? new Runnable() {
                    @Override
                    public void run() {
                        showStep(activity, index - 1, host);
                    }
                } : null,
                last ? new Runnable() {
                    @Override
                    public void run() {
                        AppState.setHelpLastStep(activity, 0); // finished: restart next time
                    }
                } : new Runnable() {
                    @Override
                    public void run() {
                        showStep(activity, index + 1, host);
                    }
                },
                null); // ✕ just closes; the step is already saved for resuming
    }

    /**
     * Shows the welcome once per app version (fresh install or update).
     * Returns true when the dialog was shown so the caller can postpone
     * other system prompts until the user picks an option.
     */
    static boolean showWelcomeIfNeeded(Activity activity, final Runnable afterWelcome) {
        int current = AppState.appVersionCode(activity);
        if (current <= 0) return false;
        if (AppState.shownHelpVersion(activity) == current) return false;
        AppState.setShownHelpVersion(activity, current);
        showWelcome(activity, afterWelcome);
        return true;
    }

    /** Welcome dialog available again from Settings → Ayuda (version gate ignored). */
    static void showWelcome(final Activity activity, final Runnable afterWelcome) {
        ThemedDialog.confirm(activity, "👋 ¡Hola! Esto es Fugaz SMS", WELCOME,
                "📖 Ver la guía", "Empezar",
                new Runnable() {
                    @Override
                    public void run() {
                        openTour(activity, afterWelcome);
                    }
                },
                new Runnable() {
                    @Override
                    public void run() {
                        if (afterWelcome != null) afterWelcome.run();
                    }
                });
    }

    /**
     * "Ver la guía" redirects to Configuración (the sections the tour talks
     * about live there). When the caller IS that screen, start in place.
     */
    private static void openTour(Activity activity, Runnable afterWelcome) {
        if (afterWelcome != null) afterWelcome.run();
        if (activity instanceof Host) {
            start(activity, (Host) activity);
        } else {
            activity.startActivity(new Intent(activity, SettingsActivity.class)
                    .putExtra(SettingsActivity.EXTRA_START_HELP, true));
        }
    }
}
