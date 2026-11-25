package ua.konoha.tv.nativepreview;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Looper;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowChoreographer;
import org.robolectric.shadows.ShadowSystemClock;
import ua.konoha.tv.nativepreview.apps.AppManager;
import ua.konoha.tv.nativepreview.audio.AudioController;
import ua.konoha.tv.nativepreview.game.RunnerPhysics;
import ua.konoha.tv.nativepreview.model.AppItem;
import ua.konoha.tv.nativepreview.panels.LibraryPanel;
import ua.konoha.tv.nativepreview.panels.SettingsPanel;
import ua.konoha.tv.nativepreview.panels.SideSheetContainer;
import ua.konoha.tv.nativepreview.prefs.PreferencesManager;
import ua.konoha.tv.nativepreview.views.*;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import static org.junit.Assert.*;
import static ua.konoha.tv.nativepreview.NativeLifecycleTest.*;

/** User actions against production Views; software rendering is not a TV/GPU/audio test. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, qualifiers = "w1366dp-h768dp-land-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
public class LauncherRegressionTest {
    private ActivityController<MainActivity> controller;

    private MainActivity start() throws Exception {
        ShadowChoreographer.setPaused(true);
        controller = Robolectric.buildActivity(MainActivity.class).setup().visible();
        MainActivity activity = controller.get();
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(16));
        AppManager apps = field(activity, "appManager");
        drain((ExecutorService) field(apps, "executor"));
        layout(activity);
        return activity;
    }

    @After public void destroy() {
        if (controller != null) controller.pause().stop().destroy();
    }

    @Test public void menuFromHomeOpensSettings() throws Exception {
        MainActivity activity = start();
        activity.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MENU));
        assertTrue(field(activity, "activePanel") instanceof SettingsPanel);
    }

    @Test public void aboutGroupsUpdatesAndResourcesAndBackRestoresFocus() throws Exception {
        MainActivity activity = start();
        call(activity, "openSettings");
        layout(activity);
        SettingsPanel panel = field(activity, "settingsPanel");
        View about = (View) findText(panel, "Про Коноху та ресурси").getParent();
        assertTrue(about.performClick());
        layout(activity);
        assertNotNull(findText(panel, "Оновлення Конохи"));
        View label = findText(panel, "Показувати CPU та RAM");
        KonohaSwitch toggle = findView((View) label.getParent().getParent(), KonohaSwitch.class);
        PreferencesManager prefs = field(activity, "prefs");
        View monitor = field(field(activity, "resources"), "label");
        assertEquals(View.GONE, monitor.getVisibility());
        toggle.performClick();
        assertTrue(prefs.shouldShowResources());
        assertEquals(View.VISIBLE, monitor.getVisibility());
        toggle.performClick();
        assertFalse(prefs.shouldShowResources());
        assertEquals(View.GONE, monitor.getVisibility());
        activity.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK));
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(16));
        assertSame(about, focused(activity));
        assertSame(panel, field(activity, "activePanel"));
    }

    @Test public void homeIntentClosesCurrentPanel() throws Exception {
        MainActivity activity = start();
        call(activity, "openGame");
        controller.newIntent(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME));
        assertNull("Home leaves the game", field(activity, "activePanel"));
    }

    @Test public void firstOpenFocusesFirstAppWithoutDelayedInput() throws Exception {
        ShadowChoreographer.setPaused(true);
        controller = Robolectric.buildActivity(MainActivity.class).create().start();
        MainActivity activity = controller.get();
        View brand = field(activity, "brandBtn");
        assertTrue(brand.requestFocusFromTouch());
        controller.resume().visible();
        drain((ExecutorService) field(field(activity, "appManager"), "executor"));
        layout(activity);
        DockView dock = field(activity, "dockView");
        assertSame(dock.getFirstCard(), focused(activity));
        assertSame("Aura is ready without a D-pad press", dock.getFirstCard(),
                field(field(activity, "chakraFocusView"), "targetView"));
    }

    @Test public void homeIntentFocusesFirstAppInsteadOfRememberedCard() throws Exception {
        MainActivity activity = start();
        DockView dock = field(activity, "dockView");
        ViewGroup cards = (ViewGroup) dock.getFirstCard().getParent();
        assertTrue(cards.getChildCount() > 2);
        assertTrue(cards.getChildAt(1).requestFocusFromTouch());
        call(activity, "openSettings");
        controller.newIntent(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME));
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(16));
        assertSame(dock.getFirstCard(), focused(activity));
        assertSame(dock.getFirstCard(), field(field(activity, "chakraFocusView"), "targetView"));
    }

    @Test public void homeRestoresFocusSilentlyUntilDirectionalNavigation() throws Exception {
        MainActivity activity = start();
        DockView dock = field(activity, "dockView");
        ViewGroup cards = (ViewGroup) dock.getFirstCard().getParent();
        assertTrue(cards.getChildCount() > 2);
        assertTrue(cards.getChildAt(1).requestFocusFromTouch());
        controller.pause();
        AudioController original = field(activity, "audioController");
        RecordingAudio audio = new RecordingAudio(activity, field(activity, "prefs"));
        java.lang.reflect.Field audioField = MainActivity.class.getDeclaredField("audioController");
        audioField.setAccessible(true);
        audioField.set(activity, audio);
        original.release();

        controller.newIntent(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME));
        controller.resume();
        drain((ExecutorService) field(field(activity, "appManager"), "executor"));
        layout(activity);
        assertSame(dock.getFirstCard(), focused(activity));
        assertEquals("System HOME, layout and resume are silent", 0, audio.focusSounds);

        assertTrue(activity.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT)));
        assertSame(cards.getChildAt(1), focused(activity));
        assertEquals("One sound for one actual move", 1, audio.focusSounds);
    }

    @Test public void homeWhilePausedFocusesFirstAppAfterResume() throws Exception {
        MainActivity activity = start();
        DockView dock = field(activity, "dockView");
        ViewGroup cards = (ViewGroup) dock.getFirstCard().getParent();
        assertTrue(cards.getChildAt(1).requestFocusFromTouch());
        controller.pause();
        controller.newIntent(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME));
        controller.resume();
        drain((ExecutorService) field(field(activity, "appManager"), "executor"));
        layout(activity);
        assertSame(dock.getFirstCard(), focused(activity));
        assertSame(dock.getFirstCard(), field(field(activity, "chakraFocusView"), "targetView"));
    }

    @Test public void homeWithNoPinnedAppsFocusesArchive() throws Exception {
        MainActivity activity = start();
        PreferencesManager prefs = field(activity, "prefs");
        prefs.setPins(List.of());
        DockView dock = field(activity, "dockView");
        dock.render();
        controller.newIntent(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME));
        assertSame(dock.getArchiveCard(), focused(activity));
        assertSame(dock.getArchiveCard(), field(field(activity, "chakraFocusView"), "targetView"));
    }

    @Test public void ordinaryResumePreservesRememberedApp() throws Exception {
        MainActivity activity = start();
        DockView dock = field(activity, "dockView");
        ViewGroup cards = (ViewGroup) dock.getFirstCard().getParent();
        View second = cards.getChildAt(1);
        assertTrue(second.requestFocusFromTouch());
        String packageName = ((AppItem) second.getTag()).getPackageName();
        controller.pause().resume();
        drain((ExecutorService) field(field(activity, "appManager"), "executor"));
        layout(activity);
        assertSame(dock.findCardByPackage(packageName), focused(activity));
    }

    @Test public void initialAuraUsesFirstMeasuredBoundsWithoutGlidingFromOrigin() throws Exception {
        MainActivity activity = start();
        android.widget.ImageView emblem = new android.widget.ImageView(activity);
        LinearLayout card = new LinearLayout(activity);
        card.addView(emblem, new LinearLayout.LayoutParams(100, 100));
        TechniqueRenderer renderer = field(activity, "techniqueRenderer");
        ChakraFocusView aura = new ChakraFocusView(activity, renderer);
        aura.setTargetView(card);
        android.widget.FrameLayout root = field(activity, "rootLayout");
        root.addView(card, new android.widget.FrameLayout.LayoutParams(100, 100));
        root.addView(aura, new android.widget.FrameLayout.LayoutParams(-1, -1));
        layout(activity);
        Bitmap bitmap = Bitmap.createBitmap(aura.getWidth(), aura.getHeight(), Bitmap.Config.ARGB_8888);
        try {
            aura.draw(new Canvas(bitmap));
            assertEquals((Float) field(aura, "targetCx"), (Float) field(aura, "currentCx"), 0.01f);
            assertEquals((Float) field(aura, "targetCy"), (Float) field(aura, "currentCy"), 0.01f);
            assertTrue((Float) field(aura, "currentR") > 0f);
        } finally { root.removeView(aura); root.removeView(card); bitmap.recycle(); }
    }

    @Test public void aboutCreditsOktirusAndNativeAsPrimary() throws Exception {
        MainActivity activity = start();
        call(activity, "showAboutDialog");
        android.app.AlertDialog dialog = org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
        try {
            TextView message = dialog.findViewById(android.R.id.message);
            String text = message.getText().toString();
            assertTrue(text.contains("Розробник: Oktirus"));
            assertTrue(text.contains("Основна версія"));
            assertTrue(text.contains("Chromium"));
            assertFalse(text.contains("ua.konoha.tv.nativepreview"));
        } finally { dialog.dismiss(); }
    }

    @Test public void motionOffKeepsChidoriFrameStatic() throws Exception {
        MainActivity activity = start();
        ChakraFocusView focus = field(activity, "chakraFocusView");
        DockView dock = field(activity, "dockView");
        focus.setTechnique("chidori");
        focus.setMotionEnabled(false);
        focus.setTargetView(dock.getFirstCard());
        Bitmap first = Bitmap.createBitmap(1366, 768, Bitmap.Config.ARGB_8888);
        Bitmap second = Bitmap.createBitmap(1366, 768, Bitmap.Config.ARGB_8888);
        try {
            // Settle the selection before comparing frames a second apart.
            for (int i = 0; i < 50; i++) { focus.invalidate(); focus.draw(new Canvas(first)); }
            first.eraseColor(0);
            focus.invalidate();
            focus.draw(new Canvas(first));
            ShadowSystemClock.advanceBy(Duration.ofSeconds(1));
            focus.invalidate();
            focus.draw(new Canvas(second));
            assertTrue("Motion off freezes the effect", first.sameAs(second));
        } finally { first.recycle(); second.recycle(); }
    }

    @Test public void initialPinsOnlyContainInstalledPackages() throws Exception {
        MainActivity activity = start();
        AppManager apps = field(activity, "appManager");
        PreferencesManager prefs = field(activity, "prefs");
        prefs.setPins(null);
        AppItem vlc = new AppItem("org.videolan.vlc", "VLC", "vlc", "", null);
        publish(apps, List.of(vlc));
        assertEquals(List.of("org.videolan.vlc"), prefs.getPins());
        publish(apps, List.of(vlc));
        assertEquals("A refresh keeps the same real favorite", 1, apps.getDockApps().size());
    }

    @Test public void moveFavoriteSkipsUninstalledPackages() throws Exception {
        MainActivity activity = start();
        AppManager apps = field(activity, "appManager");
        PreferencesManager prefs = field(activity, "prefs");
        prefs.setPins(List.of("app.a", "missing.x", "app.b"));
        publish(apps, List.of(new AppItem("app.a", "A", "", "", null),
                new AppItem("app.b", "B", "", "", null)));
        assertTrue(apps.moveFavorite("app.b", -1));
        assertEquals("app.b", apps.getPinnedApps().get(0).getPackageName());
        assertTrue("Preserve temporarily unavailable pins", prefs.getPins().contains("missing.x"));
    }

    @Test public void oldGeneratedPreviewPinsRecoverWhenRealAppsAreDiscovered() throws Exception {
        MainActivity activity = start();
        AppManager apps = field(activity, "appManager");
        PreferencesManager prefs = field(activity, "prefs");
        prefs.setPins(List.of("preview.smarttube", "preview.netflix"));
        AppItem vlc = new AppItem("org.videolan.vlc", "VLC", "vlc", "", null);
        publish(apps, List.of(vlc));
        assertEquals("Previously generated demo pins must not leave an empty Home", List.of("org.videolan.vlc"), prefs.getPins());
        prefs.setPins(List.of());
        publish(apps, List.of(vlc));
        assertTrue("An explicitly empty favorite list stays empty", prefs.getPins().isEmpty());
    }

    @Test public void visibleModalKeepsFocusWhenDockRefreshes() throws Exception {
        MainActivity activity = start();
        call(activity, "openSettings");
        layout(activity);
        SettingsPanel panel = field(activity, "settingsPanel");
        SideSheetContainer sheet = field(panel, "sheetContainer");
        assertTrue(sheet.getCloseButton().requestFocusFromTouch());
        DockView dock = field(activity, "dockView");
        dock.render();
        assertTrue("Refresh cannot focus Home behind the sheet", inside(focused(activity), panel));
        assertFalse("Even explicit D-pad traversal cannot enter Home", dock.getFirstCard().requestFocus());
    }

    @Test public void slidersRespondToTouch() throws Exception {
        KonohaSeekBar slider = new KonohaSeekBar(start());
        slider.layout(0, 0, 200, 24);
        tap(slider, 180, 12);
        assertTrue(slider.getProgress() > 45);
        tap(slider, 0, 12);
        assertEquals(0, slider.getProgress());
        tap(slider, 200, 12);
        assertEquals(100, slider.getProgress());
    }

    @Test public void tapPauseDoesNotExitRunner() throws Exception {
        MainActivity activity = start();
        call(activity, "openGame");
        layout(activity);
        RunnerGameView game = field(activity, "runnerGameView");
        game.startGame();
        tap(game, game.getWidth() - 63, 66);
        assertSame(game, field(activity, "activePanel"));
        RunnerPhysics physics = field(game, "runner");
        assertEquals("paused", physics.status);
        tap(game, game.getWidth() - 63, 66);
        assertEquals("running", physics.status);
    }

    @Test public void audioToggleRefreshesMusic() throws Exception {
        MainActivity activity = start();
        PreferencesManager prefs = field(activity, "prefs");
        prefs.setAudioEnabled(false);
        RecordingAudio audio = new RecordingAudio(activity, prefs);
        try {
            SettingsPanel panel = new SettingsPanel(activity, prefs, audio);
            openAudioSettings(panel);
            KonohaSwitch toggle = field(panel, "audioSwitch");
            audio.musicUpdates = 0;
            toggle.performClick();
            assertTrue(prefs.isAudioEnabled());
            assertTrue("Enabling audio updates music immediately", audio.musicUpdates > 0);
            audio.musicUpdates = 0;
            toggle.performClick();
            assertFalse(prefs.isAudioEnabled());
            assertTrue("Disabling audio updates music immediately", audio.musicUpdates > 0);
        } finally { audio.release(); }
    }

    @Test public void techniquePreviewEnablesAudioAndPlaysSelectedCue() throws Exception {
        MainActivity activity = start();
        PreferencesManager prefs = field(activity, "prefs");
        prefs.setAudioEnabled(false);
        prefs.setTechnique("chidori");
        RecordingAudio audio = new RecordingAudio(activity, prefs);
        try {
            SettingsPanel panel = new SettingsPanel(activity, prefs, audio);
            openAudioSettings(panel);
            findText(panel, "Використати:").performClick();
            assertTrue("Explicit preview enables audio like Chromium", prefs.isAudioEnabled());
            assertEquals("chidori", audio.previewTechnique);
            KonohaSwitch toggle = field(panel, "audioSwitch");
            assertTrue(toggle.isChecked());
        } finally { audio.release(); }
    }

    private static void openAudioSettings(SettingsPanel panel) {
        assertTrue(((View) findText(panel, "Звук і музика").getParent()).performClick());
    }

    @Test public void libraryFocusTargetsAnAuraAroundTheCard() throws Exception {
        MainActivity activity = start();
        call(activity, "openLibrary");
        layout(activity);
        LibraryPanel panel = field(activity, "libraryPanel");
        AppManager apps = field(activity, "appManager");
        View card = panel.findCardByPackage(apps.getAllApps().get(0).getPackageName());
        assertTrue(card.requestFocusFromTouch());
        ChakraFocusView aura = findView(panel, ChakraFocusView.class);
        assertNotNull("The archive has an actual effect layer", aura);
        assertSame(card, aura.getTargetView());
        controller.pause();
        assertTrue(aura.isPaused());
    }

    @Test public void unusedFocusLayerDoesNotKeepInvalidating() throws Exception {
        MainActivity activity = start();
        TechniqueRenderer renderer = new TechniqueRenderer(activity);
        CountingFocus layer = new CountingFocus(activity, renderer);
        ViewGroup root = field(activity, "rootLayout");
        root.addView(layer, new ViewGroup.LayoutParams(100, 100));
        layer.invalidations = 0;
        try {
            Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(500));
            assertEquals("No target means no continuous frame loop", 0, layer.invalidations);
        } finally { root.removeView(layer); renderer.release(); }
    }

    @Test public void libraryFrogDoesNotInheritAnotherLayersLandingTime() throws Exception {
        MainActivity activity = start();
        call(activity, "openLibrary");
        layout(activity);
        LibraryPanel panel = field(activity, "libraryPanel");
        AppManager apps = field(activity, "appManager");
        TechniqueRenderer renderer = field(activity, "techniqueRenderer");
        View card = panel.findCardByPackage(apps.getAllApps().get(0).getPackageName());
        assertTrue(card.requestFocusFromTouch());
        panel.setTechnique("gamabunta");
        renderer.ensureBitmap("gamabunta-idle");
        drain((ExecutorService) field(renderer, "decodeExecutor"));
        ChakraFocusView aura = findView(panel, ChakraFocusView.class);
        Bitmap before = Bitmap.createBitmap(aura.getWidth(), aura.getHeight(), Bitmap.Config.ARGB_8888);
        Bitmap after = Bitmap.createBitmap(aura.getWidth(), aura.getHeight(), Bitmap.Config.ARGB_8888);
        try {
            // Changing technique also interpolates its radius; compare settled frames.
            for (int i = 0; i < 50; i++) { aura.invalidate(); aura.draw(new Canvas(before)); }
            before.eraseColor(0);
            aura.invalidate();
            aura.draw(new Canvas(before));
            // A separate consumer of the bitmap cache finishes a hop on its own clock.
            renderer.triggerGamabuntaJump(new android.graphics.PointF(100, 200),
                    new android.graphics.PointF(300, 200), 1, 100000);
            renderer.drawTechnique(new Canvas(after), "gamabunta", 300, 200, 45, 100800, 0, false);
            after.eraseColor(0);
            aura.invalidate();
            aura.draw(new Canvas(after));
            assertTrue("Other layers cannot alter this frog's pose/coordinates", before.sameAs(after));
        } finally { before.recycle(); after.recycle(); }
    }

    @Test public void pausedDecorDoesNotKeepInvalidating() throws Exception {
        MainActivity activity = start();
        CountingBackground background = new CountingBackground(activity);
        ViewGroup root = field(activity, "rootLayout");
        root.addView(background, new ViewGroup.LayoutParams(100, 100));
        background.setDecorPaused(true);
        background.invalidations = 0;
        try {
            Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(500));
            assertEquals("Paused decor has no continuous frame loop", 0, background.invalidations);
        } finally { root.removeView(background); background.release(); }
    }

    private static View layout(MainActivity activity) {
        View root = activity.getWindow().getDecorView();
        for (int i = 0; i < 2; i++) {
            root.measure(View.MeasureSpec.makeMeasureSpec(1366, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(768, View.MeasureSpec.EXACTLY));
            root.layout(0, 0, 1366, 768);
        }
        root.getViewTreeObserver().dispatchOnGlobalLayout();
        return root;
    }

    private static View focused(MainActivity activity) throws Exception {
        return ((View) field(activity, "rootLayout")).findFocus();
    }

    private static boolean inside(View view, View ancestor) {
        while (view != null) {
            if (view == ancestor) return true;
            view = view.getParent() instanceof View ? (View) view.getParent() : null;
        }
        return false;
    }

    private static void tap(View view, float x, float y) {
        long time = SystemClock.uptimeMillis();
        for (int action : new int[]{MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP}) {
            MotionEvent event = MotionEvent.obtain(time, time + 16, action, x, y, 0);
            try { view.dispatchTouchEvent(event); } finally { event.recycle(); }
        }
    }

    private static <T extends View> T findView(View view, Class<T> type) {
        if (type.isInstance(view)) return type.cast(view);
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                T found = findView(group.getChildAt(i), type);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static View findText(View view, String prefix) {
        if (view instanceof TextView && ((TextView) view).getText().toString().startsWith(prefix)) return view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View found = findText(group.getChildAt(i), prefix);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static void publish(AppManager apps, List<AppItem> inventory) throws Exception {
        Map<String, AppItem> byPackage = new LinkedHashMap<>();
        for (AppItem item : inventory) byPackage.put(item.getPackageName(), item);
        Method method = AppManager.class.getDeclaredMethod("publishReload", long.class, Map.class,
                List.class, AppManager.OnAppsReloadedListener.class);
        method.setAccessible(true);
        method.invoke(apps, ((Long) field(apps, "reloadGeneration")).longValue(), byPackage,
                new ArrayList<>(inventory), null);
    }

    private static class RecordingAudio extends AudioController {
        int musicUpdates;
        int focusSounds;
        String previewTechnique;
        RecordingAudio(Context context, PreferencesManager prefs) { super(context, prefs); }
        @Override public synchronized void updateMusic() { musicUpdates++; }
        @Override public synchronized void playUiSound() {}
        @Override public synchronized void playFocusSound() { focusSounds++; }
        public synchronized boolean playTechniquePreview(String id) { previewTechnique = id; return true; }
    }

    private static class CountingFocus extends ChakraFocusView {
        int invalidations;
        CountingFocus(Context context, TechniqueRenderer renderer) { super(context, renderer); }
        @Override public void invalidate() { invalidations++; super.invalidate(); }
    }

    private static class CountingBackground extends VillageBackgroundView {
        int invalidations;
        CountingBackground(Context context) { super(context); }
        @Override public void invalidate() { invalidations++; super.invalidate(); }
    }
}
