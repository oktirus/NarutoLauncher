package ua.konoha.tv.nativepreview;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.role.RoleManager;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
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
import ua.konoha.tv.nativepreview.apps.HomeRedirectService;
import ua.konoha.tv.nativepreview.audio.AudioController;
import ua.konoha.tv.nativepreview.panels.SettingsPanel;
import ua.konoha.tv.nativepreview.prefs.PreferencesManager;
import java.util.ArrayList;
import java.util.List;
import java.time.Duration;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class HomeScreenSetupTest {
    private static final ComponentName STOCK_HOME = new ComponentName("com.google.android.apps.tv.launcherx",
            "com.google.android.apps.tv.launcherx.home.HomeActivity");
    private ActivityController<TestActivity> controller;
    private TestActivity activity;
    private HomeScreenSetup setup;

    public static class TestActivity extends Activity {
        final List<Intent> opened = new ArrayList<>();
        final List<String> blocked = new ArrayList<>();
        int lastRequestCode;
        boolean windowFocused;
        @Override public boolean hasWindowFocus() { return windowFocused; }
        @Override public void startActivity(Intent intent) {
            if (blocked.contains(intent.getAction())) throw new ActivityNotFoundException(intent.getAction());
            opened.add(intent);
        }
        @Override public void startActivityForResult(Intent intent, int code) {
            lastRequestCode = code;
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
        homePackage(activity.getPackageName());
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
            assertTrue(((View) row.getParent()).performClick());
            row = findText(panel, "Основний лаунчер");
            panel.setHomeScreenActive(true);
            assertNotNull(findText(panel, "Коноха · вибрано"));
            panel.setHomeScreenActive(false);
            assertNotNull(findText(panel, "Зробити основним"));
            panel.setOnSettingsActionListener(new SettingsPanel.OnSettingsActionListener() {
                public void onHomeScreenRequested() { setup.openSelection(); }
                public void onHomeRedirectRequested() { setup.showRedirectSettings(); }
                public void onUpdatesRequested() {}
                public void onTechniquesRequested() {}
                public void onAmbientRequested() {}
                public void onSystemSettingsRequested(String target) {}
                public void onRefreshAppsRequested() {}
                public void onAboutRequested() {}
                public void onResourcesChanged() {}
                public void onCloseRequested() {}
            });
            ((View) row.getParent()).performClick();
            assertEquals(Settings.ACTION_HOME_SETTINGS, activity.opened.get(0).getAction());
            View redirectRow = findText(panel, "Коноха після Home");
            assertNotNull(redirectRow);
            panel.setHomeRedirectEnabled(true);
            assertNotNull(findText(panel, "Дозволено"));
            ((View) redirectRow.getParent()).performClick();
            AlertDialog redirect = ShadowAlertDialog.getLatestAlertDialog();
            assertTrue(redirect.isShowing());
            assertEquals(1, activity.opened.size());
            redirect.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
            Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
            assertEquals(Settings.ACTION_ACCESSIBILITY_SETTINGS, activity.opened.get(1).getAction());
        } finally { audio.release(); }
    }

    @Test @Config(sdk = 35) public void unsuccessfulRoleChoiceOffersSystemSettings() {
        Shadows.shadowOf(activity.getSystemService(RoleManager.class)).addAvailableRole(RoleManager.ROLE_HOME);
        setup.openSelection();
        assertTrue(setup.onActivityResult(activity.lastRequestCode));
        AlertDialog result = ShadowAlertDialog.getLatestAlertDialog();
        assertTrue(result.isShowing());
        assertEquals("Домашній екран ще не змінено", Shadows.shadowOf(result).getTitle());
        result.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
        assertEquals(Settings.ACTION_HOME_SETTINGS, activity.opened.get(1).getAction());
    }

    @Test @Config(sdk = 35) public void grantedRoleIsVerifiedBeforeSuccess() {
        ShadowRoleManager roles = Shadows.shadowOf(activity.getSystemService(RoleManager.class));
        roles.addAvailableRole(RoleManager.ROLE_HOME);
        setup.openSelection();
        roles.addHeldRole(RoleManager.ROLE_HOME);
        homePackage(activity.getPackageName());
        assertTrue(setup.onActivityResult(activity.lastRequestCode));
        assertEquals(activity.getString(R.string.home_selection_saved),
                Shadows.shadowOf(ShadowAlertDialog.getLatestAlertDialog()).getTitle());
        assertFalse("Duplicate results must not reopen UI", setup.onActivityResult(activity.lastRequestCode));
    }

    @Test public void returningFromSettingsChecksActualHomeAndOffersRetryOnce() {
        setup.openSelection();
        setup.onResume();
        AlertDialog result = ShadowAlertDialog.getLatestAlertDialog();
        assertTrue(result.isShowing());
        result.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
        setup.onResume();
        assertFalse(result.isShowing());
        assertEquals("No automatic repeated system UI", 1, activity.opened.size());
    }

    @Test public void successfulSettingsSelectionReportsSuccess() {
        setup.openSelection();
        homePackage(activity.getPackageName());
        setup.onResume();
        assertEquals(activity.getString(R.string.home_selection_saved),
                Shadows.shadowOf(ShadowAlertDialog.getLatestAlertDialog()).getTitle());
    }

    @Test public void focusReturnFromTvSettingsShowsInstructionsEvenWhenKonohaIsSelected() {
        setup.openSelection();
        homePackage(activity.getPackageName());
        registerStockHome();
        activity.windowFocused = true;
        setup.onWindowFocus();
        AlertDialog result = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull("The system overlay can return focus without resuming the Activity", result);
        assertTrue(result.isShowing());
        assertEquals(activity.getString(R.string.home_selection_check_google_tv), Shadows.shadowOf(result).getMessage());
        assertEquals(activity.getString(R.string.home_redirect_title),
                result.getButton(AlertDialog.BUTTON_POSITIVE).getText());
        setup.onResume();
        assertSame(result, ShadowAlertDialog.getLatestAlertDialog());
        clickPositive(result);
        assertEquals(activity.getString(R.string.home_redirect_setup),
                Shadows.shadowOf(ShadowAlertDialog.getLatestAlertDialog()).getMessage());
    }

    @Test @Config(sdk = 35) public void roleGrantDoesNotConfirmHomeWhenAnotherAppResolvesIt() {
        ShadowRoleManager roles = Shadows.shadowOf(activity.getSystemService(RoleManager.class));
        roles.addAvailableRole(RoleManager.ROLE_HOME);
        setup.openSelection();
        int requestCode = activity.lastRequestCode;
        recreateSetup();
        roles = Shadows.shadowOf(activity.getSystemService(RoleManager.class));
        roles.addHeldRole(RoleManager.ROLE_HOME);
        assertFalse(setup.isDefaultHome());
        assertTrue(setup.onActivityResult(requestCode));
        assertTrue(ShadowAlertDialog.getLatestAlertDialog().isShowing());
        assertFalse(setup.onActivityResult(requestCode));
    }

    @Test public void returningFromHomeSettingsAfterRecreationReadsResultOnce() {
        setup.openSelection();
        recreateSetup();
        setup.onResume();
        AlertDialog result = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull("Returning from Settings must still show the result", result);
        assertTrue(result.isShowing());
        result.dismiss();
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
        setup.onResume();
        assertFalse(result.isShowing());
        assertSame(result, ShadowAlertDialog.getLatestAlertDialog());
    }

    private void recreateSetup() {
        Bundle state = new Bundle();
        setup.saveState(state);
        setup.close();
        controller.recreate();
        activity = controller.get();
        setup = new HomeScreenSetup(activity, state);
        homePackage("another.launcher");
    }

    @Test public void ordinaryResumeAndUnrelatedResultsDoNotPrompt() {
        setup.onResume();
        assertFalse(setup.onActivityResult(123));
        assertNull(ShadowAlertDialog.getLatestAlertDialog());
        assertTrue(activity.opened.isEmpty());
    }

    @Test public void failedGoogleHomeChoiceOffersRedirectAndKeepsSystemRetry() {
        homePackage("com.google.android.apps.tv.launcherx");
        setup.openSelection();
        setup.onResume();
        AlertDialog failure = ShadowAlertDialog.getLatestAlertDialog();
        assertEquals("Домашній екран ще не змінено", Shadows.shadowOf(failure).getTitle());
        assertEquals("Налаштування HOME", failure.getButton(AlertDialog.BUTTON_NEUTRAL).getText().toString());
        clickPositive(failure);
        AlertDialog instructions = ShadowAlertDialog.getLatestAlertDialog();
        assertEquals(activity.getString(R.string.home_redirect_title), Shadows.shadowOf(instructions).getTitle());
        assertTrue(setup.isPromptShowing());
        assertEquals(1, activity.opened.size());
        clickPositive(instructions);
        assertEquals(Settings.ACTION_ACCESSIBILITY_SETTINGS, activity.opened.get(1).getAction());
    }

    @Test public void returningWithoutEnablingRedirectShowsRetryOnce() {
        homePackage("com.google.android.apps.tv.launcherx");
        setup.showRedirectSettings();
        clickPositive(ShadowAlertDialog.getLatestAlertDialog());
        setup.onResume();
        AlertDialog failure = ShadowAlertDialog.getLatestAlertDialog();
        assertEquals(activity.getString(R.string.home_redirect_failure_title), Shadows.shadowOf(failure).getTitle());
        assertEquals(activity.getString(R.string.home_redirect_not_enabled), Shadows.shadowOf(failure).getMessage());
        setup.onResume();
        assertSame(failure, ShadowAlertDialog.getLatestAlertDialog());
    }

    @Test public void enabledRedirectIsVerifiedByReturningThroughTheService() {
        homePackage(activity.getPackageName());
        registerStockHome();
        setup.showRedirectSettings();
        clickPositive(ShadowAlertDialog.getLatestAlertDialog());
        enableRedirect();
        setup.onWindowFocus();
        AlertDialog test = ShadowAlertDialog.getLatestAlertDialog();
        assertEquals(activity.getString(R.string.home_redirect_test), test.getButton(AlertDialog.BUTTON_POSITIVE).getText());
        clickPositive(test);
        assertTrue(activity.opened.get(1).hasCategory(Intent.CATEGORY_HOME));
        assertEquals(STOCK_HOME, activity.opened.get(1).getComponent());
        setup.onRedirectedHome();
        activity.windowFocused = true;
        setup.onWindowFocus();
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(4));
        assertEquals(activity.getString(R.string.home_redirect_test_passed),
                Shadows.shadowOf(ShadowAlertDialog.getLatestAlertDialog()).getTitle());
        assertTrue(setup.isPromptShowing());
    }

    @Test public void redirectWithoutConfirmationShowsFailureOnlyWhenKonohaIsBackOnScreen() {
        homePackage("com.google.android.apps.tv.launcherx");
        enableRedirect();
        setup.showRedirectSettings();
        clickPositive(ShadowAlertDialog.getLatestAlertDialog());
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(4));
        assertFalse(setup.isPromptShowing());
        activity.windowFocused = true;
        setup.onWindowFocus();
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(4));
        AlertDialog failure = ShadowAlertDialog.getLatestAlertDialog();
        assertEquals(activity.getString(R.string.home_redirect_failure_title), Shadows.shadowOf(failure).getTitle());
        assertEquals(activity.getString(R.string.home_redirect_test_failed), Shadows.shadowOf(failure).getMessage());
        assertTrue(failure.isShowing());
    }

    private void enableRedirect() {
        AccessibilityServiceInfo service = new AccessibilityServiceInfo();
        ResolveInfo info = new ResolveInfo();
        info.serviceInfo = new ServiceInfo();
        info.serviceInfo.packageName = activity.getPackageName();
        info.serviceInfo.name = HomeRedirectService.class.getName();
        org.robolectric.util.ReflectionHelpers.setField(service, "mResolveInfo", info);
        Shadows.shadowOf(activity.getSystemService(AccessibilityManager.class))
                .setEnabledAccessibilityServiceList(List.of(service));
    }

    private void registerStockHome() {
        ActivityInfo info = new ActivityInfo();
        info.packageName = STOCK_HOME.getPackageName();
        info.name = STOCK_HOME.getClassName();
        info.exported = true;
        Shadows.shadowOf(activity.getPackageManager()).addOrUpdateActivity(info);
    }

    private static void clickPositive(AlertDialog dialog) {
        assertTrue(dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick());
        Shadows.shadowOf(Looper.getMainLooper()).idle();
    }

    @Test public void closingDiscardsPendingSelection() {
        setup.openSelection();
        setup.close();
        setup.onResume();
        assertNull(ShadowAlertDialog.getLatestAlertDialog());
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
