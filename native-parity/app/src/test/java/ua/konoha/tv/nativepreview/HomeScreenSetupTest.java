package ua.konoha.tv.nativepreview;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.role.RoleManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
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
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowRoleManager;
import ua.konoha.tv.nativepreview.apps.HomeScreenSetup;
import ua.konoha.tv.nativepreview.audio.AudioController;
import ua.konoha.tv.nativepreview.panels.SettingsPanel;
import ua.konoha.tv.nativepreview.prefs.PreferencesManager;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class HomeScreenSetupTest {
    private ActivityController<TestActivity> controller;
    private TestActivity activity;
    private HomeScreenSetup setup;

    public static class TestActivity extends Activity {
        final List<Intent> opened = new ArrayList<>();
        final List<String> blocked = new ArrayList<>();
        @Override public void startActivity(Intent intent) {
            if (blocked.contains(intent.getAction())) throw new ActivityNotFoundException(intent.getAction());
            opened.add(intent);
        }
        @Override public void startActivityForResult(Intent intent, int code) {
            startActivity(intent);
        }
    }

    @Before public void start() {
        controller = Robolectric.buildActivity(TestActivity.class).setup().visible();
        activity = controller.get();
        activity.getSharedPreferences("home_screen_setup", Context.MODE_PRIVATE).edit().clear().commit();
        setup = new HomeScreenSetup(activity);
        homePackage("another.launcher");
    }

    @After public void stop() {
        setup.close();
        controller.pause().stop().destroy();
    }

    private void homePackage(String name) {
        ResolveInfo info = new ResolveInfo();
        info.activityInfo = new ActivityInfo();
        info.activityInfo.packageName = name;
        info.activityInfo.name = name + ".HomeActivity";
        Shadows.shadowOf(activity.getPackageManager()).setResolveInfosForIntent(
                new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), List.of(info));
    }

    @Test public void firstRunOffersChoiceOnceAndLaterDoesNotOpenSystemUi() {
        setup.showFirstRunPrompt();
        AlertDialog prompt = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(prompt);
        assertTrue(prompt.isShowing());
        prompt.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
        assertFalse(prompt.isShowing());
        assertTrue(activity.opened.isEmpty());
        new HomeScreenSetup(activity).showFirstRunPrompt();
        assertFalse("Later/cancel must not nag on every launch", prompt.isShowing());
        assertSame(prompt, ShadowAlertDialog.getLatestAlertDialog());
    }

    @Test public void firstRunPositiveButtonOpensHomeChoice() {
        setup.showFirstRunPrompt();
        ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
        assertEquals(Settings.ACTION_HOME_SETTINGS, activity.opened.get(0).getAction());
    }

    @Test public void defaultLauncherDoesNotShowFirstRunSuggestion() {
        homePackage(activity.getPackageName());
        assertTrue(setup.isDefaultHome());
        setup.showFirstRunPrompt();
        assertNull(ShadowAlertDialog.getLatestAlertDialog());
        assertTrue(activity.opened.isEmpty());
    }

    @Test @Config(sdk = {28, 35}) public void homeSettingsFallbackWorksWhenRoleUnavailable() {
        setup.openSelection();
        assertEquals(Settings.ACTION_HOME_SETTINGS, activity.opened.get(0).getAction());
    }

    @Test public void missingHomeSettingsFallsBackToDefaultApps() {
        activity.blocked.add(Settings.ACTION_HOME_SETTINGS);
        setup.openSelection();
        assertEquals(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS, activity.opened.get(0).getAction());
    }

    @Test public void missingSystemChoosersShowsInstructionsWithoutCrashing() {
        activity.blocked.add(Settings.ACTION_HOME_SETTINGS);
        activity.blocked.add(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS);
        setup.openSelection();
        assertTrue(activity.opened.isEmpty());
        assertTrue(org.robolectric.shadows.ShadowToast.getTextOfLatestToast().contains("домашнього екрана"));
    }

    @Test @Config(sdk = 35) public void availableHomeRoleUsesSystemConsentRequest() {
        RoleManager manager = activity.getSystemService(RoleManager.class);
        ShadowRoleManager roles = Shadows.shadowOf(manager);
        roles.addAvailableRole(RoleManager.ROLE_HOME);
        setup.openSelection();
        assertEquals("android.app.role.action.REQUEST_ROLE", activity.opened.get(0).getAction());
        assertEquals(RoleManager.ROLE_HOME, activity.opened.get(0).getStringExtra("android.intent.extra.ROLE_NAME"));
    }

    @Test @Config(sdk = 35) public void heldHomeRoleSuppressesSuggestionAndMenuStillOpensSettings() {
        ShadowRoleManager roles = Shadows.shadowOf(activity.getSystemService(RoleManager.class));
        roles.addAvailableRole(RoleManager.ROLE_HOME);
        roles.addHeldRole(RoleManager.ROLE_HOME);
        assertTrue(setup.isDefaultHome());
        setup.showFirstRunPrompt();
        assertNull(ShadowAlertDialog.getLatestAlertDialog());
        setup.openSelection();
        assertEquals(Settings.ACTION_HOME_SETTINGS, activity.opened.get(0).getAction());
    }

    @Test public void settingsRowReportsStatusAndCanOpenChoiceAgain() {
        PreferencesManager prefs = new PreferencesManager(activity);
        AudioController audio = new AudioController(activity, prefs);
        try {
            SettingsPanel panel = new SettingsPanel(activity, prefs, audio);
            View row = findText(panel, "Домашній екран");
            assertNotNull(row);
            panel.setHomeScreenActive(true);
            assertNotNull(findText(panel, "Коноха · активний"));
            panel.setHomeScreenActive(false);
            assertNotNull(findText(panel, "Зробити основним"));
            panel.setOnSettingsActionListener(new SettingsPanel.OnSettingsActionListener() {
                public void onHomeScreenRequested() { setup.openSelection(); }
                public void onUpdatesRequested() {}
                public void onTechniquesRequested() {}
                public void onAmbientRequested() {}
                public void onSystemSettingsRequested(String target) {}
                public void onRefreshAppsRequested() {}
                public void onAboutRequested() {}
                public void onCloseRequested() {}
            });
            ((View) row.getParent()).performClick();
            assertEquals(Settings.ACTION_HOME_SETTINGS, activity.opened.get(0).getAction());
        } finally { audio.release(); }
    }

    private static View findText(View view, String text) {
        if (view instanceof TextView && text.contentEquals(((TextView) view).getText())) return view;
        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View found = findText(group.getChildAt(i), text);
                if (found != null) return found;
            }
        }
        return null;
    }
}
