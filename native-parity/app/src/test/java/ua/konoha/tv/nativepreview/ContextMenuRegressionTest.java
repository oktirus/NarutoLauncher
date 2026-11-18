package ua.konoha.tv.nativepreview;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewConfiguration;
import android.widget.ScrollView;
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
import ua.konoha.tv.nativepreview.apps.AppManager;
import ua.konoha.tv.nativepreview.apps.HomeScreenSetup;
import ua.konoha.tv.nativepreview.model.AppItem;
import ua.konoha.tv.nativepreview.panels.AppContextMenuPanel;
import ua.konoha.tv.nativepreview.panels.LibraryPanel;
import ua.konoha.tv.nativepreview.panels.OrderPanel;
import ua.konoha.tv.nativepreview.prefs.PreferencesManager;
import ua.konoha.tv.nativepreview.views.DockView;
import ua.konoha.tv.nativepreview.views.TVKeyHelper;
import java.time.Duration;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.function.BooleanSupplier;
import static org.junit.Assert.*;
import static ua.konoha.tv.nativepreview.NativeLifecycleTest.*;

/** Production Views and key routing; not a physical remote or GPU acceptance test. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, qualifiers = "w1366dp-h768dp-land-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
public class ContextMenuRegressionTest {
    private ActivityController<MainActivity> controller;

    private MainActivity start() throws Exception {
        ShadowChoreographer.setPaused(true);
        controller = Robolectric.buildActivity(MainActivity.class).setup().visible();
        MainActivity activity = controller.get();
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(16));
        drain((ExecutorService) field(field(activity, "appManager"), "executor"));
        layout(activity);
        return activity;
    }

    private void layout(MainActivity activity) {
        View root = activity.getWindow().getDecorView();
        for (int i = 0; i < 2; i++) {
            root.measure(View.MeasureSpec.makeMeasureSpec(1366, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(768, View.MeasureSpec.EXACTLY));
            root.layout(0, 0, 1366, 768);
        }
        root.getViewTreeObserver().dispatchOnGlobalLayout();
    }

    private AppContextMenuPanel openMenu(MainActivity activity) throws Exception {
        DockView dock = field(activity, "dockView");
        View card = dock.getFirstCard();
        card.requestFocusFromTouch();
        activity.showContextMenu((AppItem) card.getTag(), true, 0, card);
        layout(activity);
        return (AppContextMenuPanel) ((ViewGroup) field(activity, "contextMenuContainer")).getChildAt(0);
    }

    @After public void destroy() {
        if (controller != null) controller.pause().stop().destroy();
    }

    @Test public void homeClosesDockContextMenu() throws Exception {
        MainActivity activity = start();
        openMenu(activity);
        controller.newIntent(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME));
        assertEquals(View.GONE, ((View) field(activity, "contextMenuContainer")).getVisibility());
    }

    @Test public void homeFromSecondCardRestoresFirstCardAfterPendingCallbacks() throws Exception {
        MainActivity activity = start();
        DockView dock = field(activity, "dockView");
        View second = ((ViewGroup) dock.getFirstCard().getParent()).getChildAt(1);
        second.requestFocusFromTouch();
        activity.showContextMenu((AppItem) second.getTag(), true, 1, second);
        layout(activity);
        controller.newIntent(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME));
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(16));
        assertSame(dock.getFirstCard(), activity.getWindow().getDecorView().findFocus());
    }

    @Test public void ambientClosesDockContextMenu() throws Exception {
        MainActivity activity = start();
        openMenu(activity);
        call(activity, "enterAmbient");
        assertEquals(View.GONE, ((View) field(activity, "contextMenuContainer")).getVisibility());
    }

    @Test public void longHeldOkReleaseDoesNotLaunchButNextPressWorks() throws Exception {
        MainActivity activity = start();
        AppContextMenuPanel menu = openMenu(activity);
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(2500));
        menu.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_CENTER));
        View container = field(activity, "contextMenuContainer");
        assertEquals("Opening OK release must not select Launch", View.VISIBLE, container.getVisibility());
        menu.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER));
        menu.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_CENTER));
        assertEquals("A fresh OK press selects Launch", View.GONE, container.getVisibility());
    }

    @Test public void canceledOkDoesNotSelectAction() throws Exception {
        MainActivity activity = start();
        AppContextMenuPanel menu = openMenu(activity);
        menu.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_CENTER));
        menu.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER));
        KeyEvent release = KeyEvent.changeFlags(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_CENTER),
                KeyEvent.FLAG_CANCELED);
        menu.dispatchKeyEvent(release);
        assertEquals(View.VISIBLE, ((View) field(activity, "contextMenuContainer")).getVisibility());
    }

    @Test public void removingOneOfTwoFavoritesFocusesRemainingRemoveButton() throws Exception {
        MainActivity activity = start();
        AppManager apps = field(activity, "appManager");
        List<AppItem> inventory = apps.getAllApps();
        PreferencesManager prefs = field(activity, "prefs");
        prefs.setPins(List.of(inventory.get(0).getPackageName(), inventory.get(1).getPackageName()));
        call(activity, "openOrder");
        layout(activity);
        OrderPanel panel = field(activity, "orderPanel");
        ViewGroup rows = field(panel, "listContainer");
        View remove = rows.getChildAt(0).findViewById(R.id.order_remove);
        remove.requestFocusFromTouch();
        remove.performClick();
        layout(activity);
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(16));
        assertTrue(rows.getChildAt(0).findViewById(R.id.order_remove).hasFocus());
    }

    @Test public void movingFavoriteRewiresHeaderNavigation() throws Exception {
        MainActivity activity = start();
        DockView dock = field(activity, "dockView");
        AppItem app = (AppItem) dock.getFirstCard().getTag();
        AppContextMenuPanel menu = openMenu(activity);
        AppContextMenuPanel.OnContextMenuActionListener listener = field(menu, "listener");
        listener.onMove(app, 1);
        assertHeaderTargets(activity);
    }

    @Test public void unpinningFavoriteRewiresHeaderNavigation() throws Exception {
        MainActivity activity = start();
        DockView dock = field(activity, "dockView");
        AppItem app = (AppItem) dock.getFirstCard().getTag();
        AppContextMenuPanel menu = openMenu(activity);
        AppContextMenuPanel.OnContextMenuActionListener listener = field(menu, "listener");
        listener.onTogglePin(app);
        assertHeaderTargets(activity);
    }

    private void assertHeaderTargets(MainActivity activity) throws Exception {
        DockView dock = field(activity, "dockView");
        View scenes = field(activity, "sceneChoiceBtn");
        View techniques = field(activity, "techniqueBtn");
        assertSame(dock.getFirstCard(), activity.findViewById(scenes.getNextFocusDownId()));
        assertSame(dock.getArchiveCard(), activity.findViewById(techniques.getNextFocusDownId()));
    }

    @Test @Config(qualifiers = "w963dp-h541dp-land-mdpi")
    public void contextMenuActionsFitCompactLayout() throws Exception {
        verifyContextMenuLayout(963, 541);
    }

    @Test @Config(qualifiers = "w1920dp-h1080dp-land-mdpi")
    public void contextMenuActionsFitWideLayout() throws Exception {
        verifyContextMenuLayout(1920, 1080);
    }

    @Test @Config(qualifiers = "w640dp-h360dp-land-xhdpi")
    public void dpadScrollsToActionsAt720pXhdpi() throws Exception {
        MainActivity activity = start();
        AppContextMenuPanel menu = openMiddleCardMenu(activity);
        View root = measureMenu(activity, 1280, 720);
        View current = field(menu, "firstFocusableAction");
        assertTrue(current.requestFocusFromTouch());
        ViewGroup actions = (ViewGroup) current.getParent();
        assertEquals(7, actions.getChildCount());
        ScrollView viewport = (ScrollView) menu.getChildAt(0);
        assertEquals(0, viewport.getScrollY());
        captureMenu(root, 1280, 720, "context-menu-1280-first");

        for (int i = 1; i < actions.getChildCount(); i++) {
            View next = current.focusSearch(View.FOCUS_DOWN);
            assertSame(actions.getChildAt(i), next);
            assertTrue(next.requestFocus());
            assertFullyVisible(next);
            current = next;
        }
        assertTrue("The last action requires actual scrolling", viewport.getScrollY() > 0);
        captureMenu(root, 1280, 720, "context-menu-1280-last");
        menu.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_CENTER));
        menu.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER));
        menu.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_CENTER));
        assertEquals(View.GONE, ((View) field(activity, "contextMenuContainer")).getVisibility());
    }

    @Test public void archivePinWithFullDockStaysInFavoritesAndReportsWhereItIs() throws Exception {
        MainActivity activity = start();
        AppManager apps = field(activity, "appManager");
        List<AppItem> dockBefore = apps.getDockApps();
        assertEquals(6, dockBefore.size());
        AppItem target = apps.getAllApps().stream().filter(app -> !app.isPinned()).findFirst().orElseThrow();
        call(activity, "openLibrary");
        ua.konoha.tv.nativepreview.panels.LibraryPanel library = field(activity, "libraryPanel");
        View card = library.findCardByPackage(target.getPackageName());
        assertNotNull(card);
        activity.showContextMenu(target, false, -1, card);
        AppContextMenuPanel menu = (AppContextMenuPanel)
                ((ViewGroup) field(activity, "contextMenuContainer")).getChildAt(0);
        View firstAction = field(menu, "firstFocusableAction");
        ViewGroup actions = (ViewGroup) firstAction.getParent();
        TextView pin = (TextView) actions.getChildAt(1);
        assertTrue(pin.getText().toString().contains("Додати до обраного"));
        menu.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_CENTER));
        assertTrue(pin.performClick());
        assertTrue(apps.getPinnedApps().contains(target));
        assertEquals(dockBefore, apps.getDockApps());
        TextView feedback = field(field(activity, "toastOverlay"), "messageView");
        assertTrue(feedback.getText().toString().contains("поза головним екраном"));
    }

    @Test public void canceledCardOkDoesNotLaunchButNextPressWorks() throws Exception {
        int[] launches = {0};
        View card = attachKeyTestCard(launches, new int[1]);
        card.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER));
        KeyEvent canceled = KeyEvent.changeFlags(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_CENTER),
                KeyEvent.FLAG_CANCELED);
        card.dispatchKeyEvent(canceled);
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ViewConfiguration.getLongPressTimeout()));
        assertEquals(0, launches[0]);
        card.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER));
        card.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_CENTER));
        assertEquals(1, launches[0]);
    }

    @Test public void longCardOkKeepsIndependentClickAvailable() throws Exception {
        int[] launches = {0};
        int[] menus = {0};
        View card = attachKeyTestCard(launches, menus);
        card.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER));
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ViewConfiguration.getLongPressTimeout()));
        card.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_CENTER));
        assertEquals(1, menus[0]);
        assertEquals(0, launches[0]);
        assertTrue(card.performClick());
        assertEquals(1, launches[0]);
    }

    private View attachKeyTestCard(int[] launches, int[] menus) throws Exception {
        MainActivity activity = start();
        View card = new View(activity);
        TVKeyHelper.attach(card, new TVKeyHelper.OnTVCardClickListener() {
            @Override public void onClick(View view) { launches[0]++; }
            @Override public void onLongClick(View view) { menus[0]++; }
        });
        activity.setContentView(card);
        assertTrue(card.requestFocusFromTouch());
        return card;
    }

    private AppContextMenuPanel openMiddleCardMenu(MainActivity activity) throws Exception {
        DockView dock = field(activity, "dockView");
        View second = ((ViewGroup) dock.getFirstCard().getParent()).getChildAt(1);
        activity.showContextMenu((AppItem) second.getTag(), true, 1, second);
        return (AppContextMenuPanel)
                ((ViewGroup) field(activity, "contextMenuContainer")).getChildAt(0);
    }

    private void verifyContextMenuLayout(int width, int height) throws Exception {
        MainActivity activity = start();
        AppContextMenuPanel menu = openMiddleCardMenu(activity);
        View root = measureMenu(activity, width, height);
        View firstAction = field(menu, "firstFocusableAction");
        assertTrue(firstAction.requestFocusFromTouch());
        ViewGroup actions = (ViewGroup) firstAction.getParent();
        for (int i = 0; i < actions.getChildCount(); i++) {
            assertFullyVisible(actions.getChildAt(i));
        }
        captureMenu(root, width, height, "context-menu-" + width);
    }

    private static View measureMenu(MainActivity activity, int width, int height) {
        View root = activity.getWindow().getDecorView();
        root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
        root.layout(0, 0, width, height);
        return root;
    }

    private static void assertFullyVisible(View action) {
        Rect visible = new Rect();
        assertTrue("Action must be visible", action.getGlobalVisibleRect(visible));
        assertTrue("Action must not be cut off vertically", visible.height() >= action.getHeight());
    }

    private static void captureMenu(View root, int width, int height, String name) throws Exception {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        File directory = new File("build/reports/native-ui");
        assertTrue(directory.isDirectory() || directory.mkdirs());
        try (FileOutputStream stream = new FileOutputStream(new File(directory, name + ".png"))) {
            root.draw(new Canvas(bitmap));
            assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream));
        } finally { bitmap.recycle(); }
    }

    @Test public void pendingLongPressCannotOpenMenuAfterActivityPauses() throws Exception {
        MainActivity activity = start();
        DockView dock = field(activity, "dockView");
        View card = dock.getFirstCard();
        assertTrue(card.requestFocusFromTouch());
        assertTrue(card.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER)));
        controller.pause();
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ViewConfiguration.getLongPressTimeout()));
        assertEquals(View.GONE, ((View) field(activity, "contextMenuContainer")).getVisibility());
        controller.resume();
    }

    @Test public void okReleaseAfterLosingWindowFocusDoesNotLaunch() throws Exception {
        int[] launches = {0};
        int[] menus = {0};
        View card = attachKeyTestCard(launches, menus);
        MainActivity activity = controller.get();
        assertSame(card, activity.getWindow().getDecorView().findFocus());
        card.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER));
        activity.onWindowFocusChanged(false);
        card.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_CENTER));
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ViewConfiguration.getLongPressTimeout()));
        assertEquals(0, launches[0]);
        assertEquals(0, menus[0]);
        card.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER));
        card.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_CENTER));
        assertEquals(1, launches[0]);
    }

    @Test public void holdingBackClosesOnlyTheContextMenu() throws Exception {
        MainActivity activity = start();
        call(activity, "openLibrary");
        layout(activity);
        LibraryPanel library = field(activity, "libraryPanel");
        AppManager apps = field(activity, "appManager");
        AppItem app = apps.getAllApps().get(0);
        View card = library.findCardByPackage(app.getPackageName());
        assertTrue(card.requestFocusFromTouch());
        activity.showContextMenu(app, false, -1, card);
        long downTime = SystemClock.uptimeMillis();
        assertTrue(activity.dispatchKeyEvent(new KeyEvent(downTime, downTime, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK, 0)));
        assertEquals(View.GONE, ((View) field(activity, "contextMenuContainer")).getVisibility());
        assertTrue(activity.dispatchKeyEvent(new KeyEvent(downTime, downTime + 500, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK, 1)));
        assertSame(library, field(activity, "activePanel"));
        activity.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK));
        activity.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK));
        assertNull(field(activity, "activePanel"));
    }

    @Test public void holdingMenuDoesNotOpenSettingsAfterClosingContextMenu() throws Exception {
        MainActivity activity = start();
        openMenu(activity);
        long downTime = SystemClock.uptimeMillis();
        activity.dispatchKeyEvent(new KeyEvent(downTime, downTime, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MENU, 0));
        assertEquals(View.GONE, ((View) field(activity, "contextMenuContainer")).getVisibility());
        activity.dispatchKeyEvent(new KeyEvent(downTime, downTime + 500, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MENU, 1));
        assertNull(field(activity, "activePanel"));
        activity.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MENU));
        activity.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MENU));
        assertSame(field(activity, "settingsPanel"), field(activity, "activePanel"));
        assertNotNull(field(activity, "activePanel"));
    }

    @Test public void libraryAppInfoKeepsSelectedPackageOnReturn() throws Exception {
        MainActivity activity = start();
        call(activity, "openLibrary");
        layout(activity);
        LibraryPanel library = field(activity, "libraryPanel");
        AppManager apps = field(activity, "appManager");
        AppItem app = apps.getAllApps().get(4);
        View card = library.findCardByPackage(app.getPackageName());
        assertTrue(card.requestFocusFromTouch());
        activity.showContextMenu(app, false, -1, card);
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(16));
        ViewGroup container = field(activity, "contextMenuContainer");
        AppContextMenuPanel.OnContextMenuActionListener listener = field(container.getChildAt(0), "listener");
        listener.onAppInfo(app);
        Intent launched = Shadows.shadowOf(activity).getNextStartedActivity();
        assertNotNull(launched);
        assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, launched.getAction());
        assertEquals("package:" + app.getPackageName(), launched.getDataString());
        controller.pause().resume();
        drain((ExecutorService) field(apps, "executor"));
        layout(activity);
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(16));
        View restoredCard = library.findCardByPackage(app.getPackageName());
        assertNotSame(card, restoredCard);
        assertSame(restoredCard, activity.getWindow().getDecorView().findFocus());
    }

    @Test public void automaticUpdateCannotCoverAppContextMenu() throws Exception {
        MainActivity activity = start();
        controller.windowFocusChanged(true);
        HomeScreenSetup setup = field(activity, "homeScreenSetup");
        setup.close();
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(16));
        BooleanSupplier canPrompt = field(field(activity, "updateController"), "canPrompt");
        assertTrue(canPrompt.getAsBoolean());
        openMenu(activity);
        assertFalse(canPrompt.getAsBoolean());
        activity.closeContextMenu();
        assertTrue(canPrompt.getAsBoolean());
    }
}
