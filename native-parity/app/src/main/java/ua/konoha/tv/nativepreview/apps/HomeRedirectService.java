package ua.konoha.tv.nativepreview.apps;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;

import ua.konoha.tv.nativepreview.MainActivity;

/** Opt-in redirect when Google TV brings its stock Home activity to the foreground. */
public final class HomeRedirectService extends AccessibilityService {
    public static final String ACTION_REDIRECTED_HOME =
            "ua.konoha.tv.nativeparity.action.REDIRECTED_HOME";
    private static final String GOOGLE_TV = "com.google.android.apps.tv.launcherx";
    private static final String GOOGLE_HOME = GOOGLE_TV + ".home.HomeActivity";
    private static final long REDIRECT_DELAY_MS = 180;
    private static final long DUPLICATE_WINDOW_MS = 1_000;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Runnable redirect = this::openKonoha;
    private long ignoreUntil;

    public static boolean isEnabled(Context context) {
        AccessibilityManager manager = context.getSystemService(AccessibilityManager.class);
        if (manager == null) return false;
        for (AccessibilityServiceInfo configured :
                manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)) {
            ServiceInfo service = configured.getResolveInfo().serviceInfo;
            if (context.getPackageName().equals(service.packageName)
                    && HomeRedirectService.class.getName().equals(service.name)) return true;
        }
        return false;
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                || !TextUtils.equals(GOOGLE_TV, event.getPackageName())) return;
        // A stock overlay or profile screen may open just after the stock Home activity.
        mainHandler.removeCallbacks(redirect);
        if (TextUtils.equals(GOOGLE_HOME, event.getClassName())
                && SystemClock.uptimeMillis() >= ignoreUntil) {
            mainHandler.postDelayed(redirect, REDIRECT_DELAY_MS);
        }
    }

    private void openKonoha() {
        if (!GOOGLE_TV.equals(HomeScreenSetup.resolvedHomePackage(this))) return;
        ignoreUntil = SystemClock.uptimeMillis() + DUPLICATE_WINDOW_MS;
        try {
            // This is an ordinary app launch. Leave the system HOME task with Google TV.
            startActivity(new Intent(this, MainActivity.class)
                    .setAction(ACTION_REDIRECTED_HOME)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED));
        } catch (ActivityNotFoundException | SecurityException error) {
            Log.w("HomeRedirect", "Could not open Konoha", error);
        }
    }

    @Override public void onInterrupt() {
        mainHandler.removeCallbacks(redirect);
    }

    @Override public boolean onUnbind(Intent intent) {
        mainHandler.removeCallbacks(redirect);
        return super.onUnbind(intent);
    }

    @Override public void onDestroy() {
        mainHandler.removeCallbacks(redirect);
        super.onDestroy();
    }
}
