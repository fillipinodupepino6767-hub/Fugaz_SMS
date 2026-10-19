package com.arena.autosms5min;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Typeface;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowDialog;

import static org.junit.Assert.*;

/** Host-side regressions: modal lifecycle, original deadlines and shared typography. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class AppearanceAndRestartTest {
    private ActivityController<Activity> controller;
    private Activity activity;
    private RetentionRestart notice;

    @Before public void setUp() {
        controller = Robolectric.buildActivity(Activity.class).setup();
        activity = controller.get();
        for (String name : new String[]{"app_state", "deletion_log", "retention_restart"}) {
            assertTrue(activity.getSharedPreferences(name, Activity.MODE_PRIVATE).edit().clear().commit());
        }
        idle();
        notice = new RetentionRestart(activity, new LinearLayout(activity), true, () -> { });
    }

    @After public void tearDown() {
        // Closing the notice first: it must free the screen for the next test.
        notice.onPause();
        idle();
        controller.pause().stop().destroy();
        idle();
    }

    @Test public void leavingActiveNoticeDoesNotRestartOrReplayIt() {
        DeletionLog.add(activity, "123", "Texto original", "Prueba");
        long originalExpiry = DeletionLog.entries(activity).get(0).expiresAt;
        DeletionLog.setRetentionMillis(activity, 6 * DeletionLog.ONE_HOUR_MS);
        assertEquals(originalExpiry, DeletionLog.entries(activity).get(0).expiresAt);
        assertTrue("Changing history duration must mark a new notice",
                activity.getSharedPreferences("retention_restart", Activity.MODE_PRIVATE)
                        .getBoolean("history_restart_pending", false));
        notice.onResume();
        Dialog dialog = ShadowDialog.getLatestDialog();
        assertNotNull("A resumed screen must display its pending notice", dialog);
        assertTrue(dialog.isShowing());
        dialog.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK));
        dialog.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK));
        assertTrue("Back must not bypass the restart action", dialog.isShowing());
        notice.onPause();
        assertFalse(dialog.isShowing());
        notice.onResume();
        notice.refresh();
        assertFalse("Returning must not reopen a consumed notice", dialog.isShowing());
        assertEquals(originalExpiry, DeletionLog.entries(activity).get(0).expiresAt);
        // Recreating the helper models a new Activity/process reading persistent preferences.
        notice.onPause();
        notice = new RetentionRestart(activity, new LinearLayout(activity), true, () -> { });
        notice.onResume();
        assertFalse(dialog.isShowing());
    }

    @Test public void restartButtonAppliesNewDeadlineAndPreservesDeletionDate() {
        DeletionLog.add(activity, "123", "Texto original", "Prueba");
        long deletedAt = DeletionLog.entries(activity).get(0).time;
        DeletionLog.setRetentionMillis(activity, 6 * DeletionLog.ONE_HOUR_MS);
        assertTrue("Changing history duration must mark a new notice",
                activity.getSharedPreferences("retention_restart", Activity.MODE_PRIVATE)
                        .getBoolean("history_restart_pending", false));
        notice.onResume();
        Dialog dialog = ShadowDialog.getLatestDialog();
        assertNotNull("A resumed screen must display its pending notice", dialog);
        Button restart = findButton(dialog.findViewById(android.R.id.content));
        if (restart == null && dialog.getWindow() != null) {
            restart = findButton(dialog.getWindow().getDecorView());
        }
        assertNotNull("The notice must offer its single REINICIAR button", restart);
        long before = System.currentTimeMillis();
        restart.performClick();
        assertFalse(dialog.isShowing());
        DeletionLog.Entry entry = DeletionLog.entries(activity).get(0);
        assertEquals(deletedAt, entry.time);
        assertTrue(entry.expiresAt >= before + 6 * DeletionLog.ONE_HOUR_MS);
        assertTrue(entry.expiresAt <= System.currentTimeMillis() + 6 * DeletionLog.ONE_HOUR_MS);
        // A genuinely new duration change must still show a fresh notice.
        DeletionLog.setRetentionMillis(activity, 12 * DeletionLog.ONE_HOUR_MS);
        notice.refresh();
        Dialog fresh = ShadowDialog.getLatestDialog();
        assertNotNull("A new duration change must open a new notice", fresh);
        assertTrue(fresh.isShowing());
    }

    @Test public void sameValueDoesNotProduceANewNotice() {
        DeletionLog.setRetentionMillis(activity, DeletionLog.retentionMillis(activity));
        notice.onResume();
        assertFalse(activity.getSharedPreferences("retention_restart", Activity.MODE_PRIVATE)
                .getBoolean("history_restart_pending", false));
        assertEquals(0, ThemedDialog.activeCount());
    }

    @Test public void closedNoticeFreesTheScreenForTheNextWindow() {
        DeletionLog.setRetentionMillis(activity, 6 * DeletionLog.ONE_HOUR_MS);
        notice.onResume();
        Dialog dialog = ShadowDialog.getLatestDialog();
        assertNotNull(dialog);
        assertTrue(dialog.isShowing());
        assertEquals("The open notice occupies the screen", 1, ThemedDialog.activeCount());
        notice.onPause();
        assertFalse(dialog.isShowing());
        assertEquals("A closed window must free the slot for the next one",
                0, ThemedDialog.activeCount());
    }

    @Test public void textWeightIsOptionalAndDoesNotChangeSizeOrSmsContent() {
        TextView normal = new TextView(activity);
        AppState.setBoldText(activity, false);
        Ui.text(normal, 16);
        AppState.setBoldText(activity, true);
        assertTrue("Bold preference must update immediately", AppState.boldText(activity));
        TextView bold = new TextView(activity);
        Ui.text(bold, 16);
        // Typeface.isBold() cannot be read under the graphics shadow used here:
        // it builds typefaces through the pointer constructor, so it always
        // reports NORMAL. The style bits hold the same information and are
        // exactly what isBold() reads on a phone: (getStyle() & BOLD) != 0.
        assertEquals("Plain text must not be bold",
                Typeface.NORMAL, normal.getTypeface().getStyle());
        assertTrue("Highlighted text must be bold (native or synthetic)",
                (bold.getTypeface().getStyle() & Typeface.BOLD) != 0
                        || bold.getPaint().isFakeBoldText());
        assertEquals(normal.getTextSize(), bold.getTextSize(), 0.01f);
        MessageViews message = new MessageViews(activity, true);
        String sms = "Primera línea\n\nCódigo: 1234 5678";
        message.bind("Remitente", "Importante", sms, "Hoy", "Conservado");
        assertEquals(5, message.getChildCount());
        assertEquals(sms, ((TextView) message.getChildAt(2)).getText().toString());
        assertEquals(View.VISIBLE, message.getChildAt(4).getVisibility());
        message.updateStatus("");
        assertEquals(View.GONE, message.getChildAt(4).getVisibility());
    }

    /** Runs the tasks the main looper has waiting (dialog show/dismiss messages). */
    private void idle() {
        Shadows.shadowOf(Looper.getMainLooper()).idle();
    }

    private Button findButton(View view) {
        if (view == null) return null;
        if (view instanceof Button) return (Button) view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                Button found = findButton(group.getChildAt(i));
                if (found != null) return found;
            }
        }
        return null;
    }
}
