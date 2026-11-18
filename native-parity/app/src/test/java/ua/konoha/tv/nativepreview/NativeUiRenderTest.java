package ua.konoha.tv.nativepreview;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.annotation.LooperMode;
import ua.konoha.tv.nativepreview.prefs.PreferencesManager;
import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.ExecutorService;
import static org.junit.Assert.*;

/** Software Skia rendering of production Views; not an Android TV/GPU acceptance test. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, qualifiers = "w1366dp-h768dp-land-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
public class NativeUiRenderTest {
    @Test
    @Config(qualifiers = "w963dp-h541dp-land-mdpi")
    public void renderPrimaryLauncherDefaultsAndAbout() throws Exception {
        org.robolectric.shadows.ShadowChoreographer.setPaused(true);
        PreferencesManager prefs = new PreferencesManager(RuntimeEnvironment.getApplication());
        assertTrue(prefs.isAudioEnabled());
        assertTrue(prefs.isMotionEnabled());
        assertTrue(prefs.isAmbienceEnabled());
        assertEquals(60, prefs.getRotationSeconds());
        assertEquals(900, prefs.getIdleSeconds());
        ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup().visible();
        try {
            MainActivity activity = controller.get();
            flush(activity, "appManager", "executor");
            flush(activity, "backgroundView", "bgExecutor");
            flush(activity, "dockView", "iconExecutor");
            flush(activity, "techniqueRenderer", "decodeExecutor");
            capture(activity, "primary-defaults-home-963", 963, 541);
            NativeLifecycleTest.call(activity, "showAboutDialog");
            android.app.AlertDialog dialog = org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper())
                    .idleFor(java.time.Duration.ofMillis(16));
            captureView(dialog.getWindow().getDecorView(), "primary-about-963", 600, 500);
            dialog.dismiss();
            NativeLifecycleTest.call(activity, "openSettings");
            capture(activity, "primary-defaults-settings-963", 963, 541);
            activity.closeActivePanel();
            NativeLifecycleTest.call(activity, "openTechniques");
            flush(activity, "techniqueRenderer", "decodeExecutor");
            capture(activity, "primary-techniques-order-963", 963, 541);
        } finally { controller.pause().stop().destroy(); }
    }

    @Test public void renderHomeSetupSuggestionAndSettingsStatus() throws Exception {
        org.robolectric.shadows.ShadowChoreographer.setPaused(true);
        android.content.pm.ResolveInfo otherHome = new android.content.pm.ResolveInfo();
        otherHome.activityInfo = new android.content.pm.ActivityInfo();
        otherHome.activityInfo.packageName = "another.launcher";
        otherHome.activityInfo.name = "another.launcher.HomeActivity";
        org.robolectric.Shadows.shadowOf(RuntimeEnvironment.getApplication().getPackageManager())
                .setResolveInfosForIntent(new android.content.Intent(android.content.Intent.ACTION_MAIN)
                        .addCategory(android.content.Intent.CATEGORY_HOME), java.util.List.of(otherHome));
        PreferencesManager prefs = new PreferencesManager(RuntimeEnvironment.getApplication());
        prefs.setMotionEnabled(false);
        prefs.setAudioEnabled(false);
        prefs.setIdleSeconds(300);
        ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup().visible();
        try {
            MainActivity activity = controller.get();
            flush(activity, "appManager", "executor");
            flush(activity, "backgroundView", "bgExecutor");
            activity.onWindowFocusChanged(true);
            android.app.AlertDialog prompt = org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
            assertNotNull("First launch offers HOME setup", prompt);
            assertTrue(prompt.isShowing());
            ((Runnable) NativeLifecycleTest.field(activity, "idleRunnable")).run();
            assertEquals("Do not enter ambient over a HOME suggestion", View.GONE,
                    ((View) NativeLifecycleTest.field(activity, "ambientOverlay")).getVisibility());
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper())
                    .idleFor(java.time.Duration.ofMillis(16));
            captureView(prompt.getWindow().getDecorView(), "home-setup-prompt", 600, 360);
            prompt.dismiss();
            NativeLifecycleTest.call(activity, "openSettings");
            layout(activity, 963, 541);
            ua.konoha.tv.nativepreview.panels.SettingsPanel panel = NativeLifecycleTest.field(activity, "settingsPanel");
            android.widget.TextView value = NativeLifecycleTest.field(panel, "homeScreenValueText");
            View row = (View) value.getParent();
            assertTrue(row.requestFocusFromTouch());
            panel.setHomeScreenActive(false);
            capture(activity, "settings-home-not-default-963", 963, 541);
            panel.setHomeScreenActive(true);
            capture(activity, "settings-home-active-963", 963, 541);
            android.widget.TextView redirectValue = NativeLifecycleTest.field(panel, "homeRedirectValueText");
            View redirectRow = (View) redirectValue.getParent();
            assertSame(redirectRow, row.focusSearch(View.FOCUS_DOWN));
            assertTrue(redirectRow.requestFocus());
            capture(activity, "settings-home-redirect-963", 963, 541);
            assertTrue(redirectRow.performClick());
            android.app.AlertDialog redirect = org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
            assertTrue(redirect.isShowing());
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper())
                    .idleFor(java.time.Duration.ofMillis(16));
            captureView(redirect.getWindow().getDecorView(), "settings-home-redirect-prompt", 600, 360);
            redirect.dismiss();
        } finally { controller.pause().stop().destroy(); }
    }

    @Test public void renderFocusedLibraryEffects() throws Exception {
        org.robolectric.shadows.ShadowChoreographer.setPaused(true);
        new PreferencesManager(RuntimeEnvironment.getApplication()).setMotionEnabled(false);
        ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup().visible();
        try {
            MainActivity activity = controller.get();
            flush(activity, "appManager", "executor");
            flush(activity, "backgroundView", "bgExecutor");
            NativeLifecycleTest.call(activity, "openLibrary");
            layout(activity, 1366, 768);
            ua.konoha.tv.nativepreview.panels.LibraryPanel panel = NativeLifecycleTest.field(activity, "libraryPanel");
            ua.konoha.tv.nativepreview.apps.AppManager apps = NativeLifecycleTest.field(activity, "appManager");
            View card = panel.findCardByPackage(apps.getAllApps().get(0).getPackageName());
            assertTrue(card.requestFocusFromTouch());
            for (String technique : new String[]{"chidori", "gamabunta"}) {
                panel.setTechnique(technique);
                ua.konoha.tv.nativepreview.views.TechniqueRenderer renderer = NativeLifecycleTest.field(activity, "techniqueRenderer");
                for (String plate : ua.konoha.tv.nativepreview.views.TechniqueRenderer.platesFor(technique)) renderer.ensureBitmap(plate);
                flush(activity, "techniqueRenderer", "decodeExecutor");
                capture(activity, "library-focus-" + technique, 1366, 768);
            }
        } finally { controller.pause().stop().destroy(); }
    }

    @Test public void renderHomeAndAllMainPanels() throws Exception {
        org.robolectric.shadows.ShadowChoreographer.setPaused(true);
        PreferencesManager prefs = new PreferencesManager(RuntimeEnvironment.getApplication());
        prefs.setMotionEnabled(false);
        prefs.setAudioEnabled(false);
        ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup().visible();
        try {
            MainActivity activity = controller.get();
            flush(activity, "appManager", "executor");
            flush(activity, "backgroundView", "bgExecutor");
            flush(activity, "dockView", "iconExecutor");
            capture(activity, "home-1366", 1366, 768);
            for (String[] panel : new String[][]{
                    {"openLibrary", "library-1366", "libraryPanel"},
                    {"openScenes", "scenes-1366", "scenesPanel"},
                    {"openTechniques", "techniques-1366", "techniquesPanel"},
                    {"openGame", "game-1366", "runnerGameView"},
                    {"openSettings", "settings-1366", "settingsPanel"}}) {
                NativeLifecycleTest.call(activity, panel[0]);
                layout(activity, 1366, 768);
                if ("scenesPanel".equals(panel[2])) flush(activity, panel[2], "thumbExecutor");
                if ("techniquesPanel".equals(panel[2])) flush(activity, "techniqueRenderer", "decodeExecutor");
                if ("runnerGameView".equals(panel[2])) flush(activity, panel[2], "bitmapExecutor");
                flush(activity, "dockView", "iconExecutor");
                capture(activity, panel[1], 1366, 768);
                activity.closeActivePanel();
            }
            capture(activity, "home-1920", 1920, 1080);
        } finally {
            controller.pause().stop().destroy();
        }
    }

    @Test public void pickerCardsFitCompactAndTvWidths() throws Exception {
        org.robolectric.shadows.ShadowChoreographer.setPaused(true);
        new PreferencesManager(RuntimeEnvironment.getApplication()).setMotionEnabled(false);
        ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup().visible();
        try {
            MainActivity activity = controller.get();
            flush(activity, "appManager", "executor");
            for (String[] panel : new String[][]{{"openScenes", "scenesPanel"}, {"openTechniques", "techniquesPanel"},
                    {"openLibrary", "libraryPanel"}}) {
                NativeLifecycleTest.call(activity, panel[0]);
                if ("scenesPanel".equals(panel[1])) flush(activity, panel[1], "thumbExecutor");
                if ("techniquesPanel".equals(panel[1])) flush(activity, "techniqueRenderer", "decodeExecutor");
                flush(activity, "dockView", "iconExecutor");
                for (int width : new int[]{390, 1280, 1920}) {
                    layout(activity, width, 768);
                    View picker = NativeLifecycleTest.field(activity, panel[1]);
                    GridLayout grid = NativeLifecycleTest.field(picker, "grid");
                    capture(activity, panel[1] + "-" + width, width, 768);
                    for (int i = 0; i < grid.getChildCount(); i++) {
                        View card = grid.getChildAt(i);
                        assertTrue("Card left within grid at " + width, card.getLeft() >= 0);
                        assertTrue("Card " + i + " right=" + card.getRight() + "; grid=" + grid.getWidth()
                                + "; width=" + width + "; card width=" + card.getWidth(), card.getRight() <= grid.getWidth());
                    }
                    if (width == 390) capture(activity, panel[1] + "-390", width, 768);
                }
                activity.closeActivePanel();
            }
        } finally {
            controller.pause().stop().destroy();
        }
    }

    private static void flush(MainActivity activity, String owner, String executor) throws Exception {
        Object view = NativeLifecycleTest.field(activity, owner);
        NativeLifecycleTest.drain((ExecutorService) NativeLifecycleTest.field(view, executor));
    }

    private static View layout(MainActivity activity, int width, int height) {
        View root = activity.getWindow().getDecorView();
        // onSizeChanged reflows grids; the following measure applies their new widths.
        for (int pass = 0; pass < 2; pass++) {
            root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
            root.layout(0, 0, width, height);
        }
        return root;
    }

    private static void capture(MainActivity activity, String name, int width, int height) throws Exception {
        View root = layout(activity, width, height);
        captureView(root, name, width, height);
    }

    private static void captureView(View root, String name, int width, int height) throws Exception {
        root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
        root.layout(0, 0, width, height);
        invalidateTree(root);
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        root.draw(new Canvas(bitmap));
        File dir = new File("build/reports/native-ui");
        assertTrue(dir.isDirectory() || dir.mkdirs());
        try (FileOutputStream stream = new FileOutputStream(new File(dir, name + ".png"))) {
            assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream));
        }
        assertNotEquals("Software rendering must contain visible content", bitmap.getPixel(0, 0), bitmap.getPixel(width / 2, height / 2));
        bitmap.recycle();
    }

    private static void invalidateTree(View view) {
        view.invalidate();
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) invalidateTree(group.getChildAt(i));
        }
    }
}
