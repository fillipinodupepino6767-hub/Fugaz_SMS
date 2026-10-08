package com.arena.autosms5min;

import android.app.Activity;

/**
 * One-time welcome plus a guided help tour: a sequence of floating windows,
 * each closed with ✕, that explains every part of the app in plain language.
 *
 * The welcome appears once per app version (first install or after an update)
 * so someone who already opened an older build sees it again when updating.
 */
final class HelpTour {

    private HelpTour() { }

    /** Steps: [title, body]. Opened from the ❓ buttons; user can close any time. */
    private static final String[][] STEPS = {
        {"⚙ Empezar aquí",
            "La configuración automática (Configuración → ①) hace 5 pasos en orden:\n\n"
                + "1️⃣ Permitir ajustes restringidos (solo Android 15/16 con APK instalado: "
                + "te lleva a la info de la app y el menú ⋮, con PIN o huella).\n"
                + "2️⃣ Permisos.\n"
                + "3️⃣ Notificaciones.\n"
                + "4️⃣ Alarmas exactas.\n"
                + "5️⃣ App SMS predeterminada.\n\n"
                + "Al volver de cada pantalla del sistema, la app continúa sola."},
        {"① y ② en Configuración",
            "① CONFIGURACIÓN AUTOMÁTICA hace todo lo anterior en orden, sin que tengas que "
                + "buscar cada ajuste.\n\n"
                + "② CONFIGURAR COMO APP SMS PREDETERMINADA es solo el paso final: Android "
                + "preguntará «¿usar Fugaz SMS como app SMS?». Sin ese paso la app no recibe "
                + "mensajes.\n\n"
                + "Los dos botones están separados a propósito para que no se pisen."},
        {"⏳ Bandeja temporal",
            "Tus SMS nuevos se borran solos al cumplir el tiempo elegido (Configuración → "
                + "TIEMPO DE BORRADO; por defecto 5 minutos).\n\n"
                + "En AUTO-ELIMINACIÓN POR TIPO eliges qué se borra: normales, posible spam e "
                + "importantes. Los importantes NO se borran solos por defecto: así se protegen "
                + "códigos de banco y alertas.\n\n"
                + "Los mensajes que tú envías nunca se borran solos. En ⌛ Eliminados "
                + "recientemente revisas lo borrado antes de que desaparezca del todo."},
        {"📱 Solo SMS de texto",
            "Fugaz SMS no usa internet: recibe los SMS que llegan por la señal del celular.\n\n"
                + "Los chats por internet (Wi-Fi o datos) de Google Mensajes o iPhone NO pueden "
                + "llegar aquí: Google no da acceso a otras apps.\n\n"
                + "Si te escriben por chat y no lo ves: Configuración → ABRIR GOOGLE MENSAJES "
                + "y pásalo a «solo SMS»."},
        {"👆 Bandeja: deslizar y toques",
            "Deslizar un mensaje a la derecha o izquierda lo borra o archiva, como en Gmail; "
                + "cada lado se configura por separado en Configuración.\n\n"
                + "Mantén presionado un mensaje para abrirlo, archivarlo o eliminarlo.\n\n"
                + "📦 Archivados no se borran solos. 🔎 El buscador de arriba filtra por nombre, "
                + "número o texto."},
        {"🔔 Avisos, silencio y vigilancia",
            "Si no escuchas mensajes: Configuración → PROBAR SONIDO DE NOTIFICACIÓN. Si la "
                + "prueba suena, el problema es del volumen o de otra app, no de Fugaz SMS.\n\n"
                + "⏱ TEMPORIZADOR DE SILENCIO: silencia o vibra AHORA (útil en el colegio) y el "
                + "sonido vuelve solo al terminar el tiempo.\n\n"
                + "👁 VIGILAR SILENCIO: si otra app (Volume Styles) o el sistema ponen el "
                + "teléfono en silencio o vibración, te llega un aviso para activar el "
                + "temporizador de un toque, con tu tiempo automático. Mientras vigila verás un "
                + "aviso permanente discreto en la bandeja de notificaciones. No detecta «No "
                + "molestar» puro: eso Android no lo comparte con otras apps."},
        {"🆘 Ayuda y saldo",
            "La barra amarilla de la bandeja siempre dice qué falta; tócala para configurar.\n\n"
                + "DIAGNÓSTICO DE RECEPCIÓN = por qué no llegan mensajes.\n"
                + "CÓMO FUNCIONA = referencia completa de todo.\n\n"
                + "💰 SALDO DE MIS LÍNEAS: Kolbi *888# o SMS al 8888 · Movistar/Liberty SMS al "
                + "606 · Claro *611#.\n\n"
                + "Todo queda local en tu teléfono: sin cuentas, sin internet, sin publicidad. "
                + "El ❓ de la barra siempre reabre esta guía."},
    };

    private static final String WELCOME =
            "Tu bandeja temporal de SMS: lo nuevo se borra solo al cumplir el tiempo que elijas "
                + "(por defecto 5 minutos). Solo SMS de texto, sin internet y sin cuentas.\n\n"
                + "IMPORTANTE: para recibir mensajes toca ⚙ Configuración → ① CONFIGURACIÓN "
                + "AUTOMÁTICA y sigue los 5 pasos. Android te pedirá «Permitir ajustes "
                + "restringidos» (con PIN o huella) si instalaste el APK.\n\n"
                + "¿Es tu primera vez o actualizaste la app? «Ver la guía» abre 7 ventanitas "
                + "cortas que se cierran con ✕. El ❓ de la barra siempre la vuelve a abrir.";

    /** Start the tour from the first tip; onFinished runs when the user closes it. */
    static void start(final Activity activity, final Runnable onFinished) {
        final boolean[] finished = {false};
        final Runnable done = new Runnable() {
            @Override
            public void run() {
                if (finished[0]) return;
                finished[0] = true;
                if (onFinished != null) onFinished.run();
            }
        };
        showStep(activity, 0, done);
    }

    static void start(Activity activity) {
        start(activity, null);
    }

    private static void showStep(final Activity activity, final int index, final Runnable done) {
        if (activity == null || activity.isFinishing()) return;
        final boolean last = index >= STEPS.length - 1;
        String title = STEPS[index][0] + "  ·  " + (index + 1) + "/" + STEPS.length;
        ThemedDialog.tourStep(activity, title, STEPS[index][1],
                last ? "Listo ✓" : "Siguiente →",
                last ? done : new Runnable() {
                    @Override
                    public void run() {
                        showStep(activity, index + 1, done);
                    }
                },
                done);
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
                // Cancel position: run the tour first, then the deferred prompts.
                new Runnable() {
                    @Override
                    public void run() {
                        start(activity, afterWelcome);
                    }
                },
                new Runnable() {
                    @Override
                    public void run() {
                        if (afterWelcome != null) afterWelcome.run();
                    }
                });
    }
}
