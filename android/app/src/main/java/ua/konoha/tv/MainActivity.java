package ua.konoha.tv;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

/** Offline HOME shell. Discovery and Leanback-first launch follow cleanyourtv (MIT).
 * Original reference: AppLaunchActivity.java / MainActivity.java, Gathin, 2026.
 * No permissions, root, input interception service, package disabling or HOME mutation.
 */
public class MainActivity extends Activity {
    private static final String ORIGIN = "https://konoha.local";
    private WebView web;
    private final Map<String, ResolveInfo> apps = new TreeMap<>();

    @SuppressLint("SetJavaScriptEnabled")
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        web = new WebView(this);
        WebView.setWebContentsDebuggingEnabled((getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0);
        web.setBackgroundColor(0xff071c27);
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.getSettings().setAllowFileAccess(false);
        web.getSettings().setAllowContentAccess(false);
        web.getSettings().setMediaPlaybackRequiresUserGesture(false);
        web.getSettings().setSupportMultipleWindows(false);
        web.addJavascriptInterface(new Bridge(), "YoruNative");
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return true; // UI is a single bundled document; links cannot leave its trusted origin.
            }
            @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String path = uri.getPath();
                if (!"https".equals(uri.getScheme()) || !"konoha.local".equals(uri.getHost())
                    || path == null || path.contains("..")) return missing();
                try {
                    if (path.startsWith("/appicons/")) return appIcon(path.substring(10));
                    if (!path.startsWith("/prototype/") && !path.startsWith("/assets/")) return missing();
                    String file = "site" + path;
                    String extension = android.webkit.MimeTypeMap.getFileExtensionFromUrl(path);
                    String mime = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
                    if ("js".equals(extension)) mime = "text/javascript";
                    if (mime == null) mime = "application/octet-stream";
                    return new WebResourceResponse(mime, "UTF-8", getAssets().open(file));
                } catch (IOException | RuntimeException error) { return missing(); }
            }
        });
        setContentView(web);
        web.loadUrl(ORIGIN + "/prototype/index.html");
    }

    private WebResourceResponse missing() {
        return new WebResourceResponse("text/plain", "UTF-8", 404, "Not Found", null,
            new ByteArrayInputStream(new byte[0]));
    }

    private synchronized WebResourceResponse appIcon(String packageName) {
        ResolveInfo info = apps.get(packageName);
        if (info == null) return missing();
        Drawable drawable = info.loadIcon(getPackageManager());
        Bitmap bitmap = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, 128, 128);
        drawable.draw(canvas);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, output);
        bitmap.recycle();
        return new WebResourceResponse("image/png", null, new ByteArrayInputStream(output.toByteArray()));
    }

    private synchronized String inventory() {
        apps.clear();
        for (String category : new String[]{Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER}) {
            for (ResolveInfo info : getPackageManager().queryIntentActivities(
                new Intent(Intent.ACTION_MAIN).addCategory(category), 0)) {
                String pkg = info.activityInfo.packageName;
                if (!pkg.equals(getPackageName()) && info.activityInfo.exported
                    && info.activityInfo.enabled && info.activityInfo.applicationInfo.enabled
                    && !apps.containsKey(pkg)) apps.put(pkg, info);
            }
        }
        JSONArray result = new JSONArray();
        for (Map.Entry<String, ResolveInfo> entry : apps.entrySet()) {
            try {
                JSONObject item = new JSONObject();
                item.put("package", entry.getKey());
                item.put("name", entry.getValue().loadLabel(getPackageManager()).toString());
                item.put("icon", ORIGIN + "/appicons/" + entry.getKey());
                result.put(item);
            } catch (JSONException ignored) { /* This entry cannot be represented; keep other apps. */ }
        }
        return result.toString();
    }

    private class Bridge {
        @JavascriptInterface public String getApps() { return inventory(); }
        @JavascriptInterface public void launch(String pkg) {
            synchronized (MainActivity.this) { if (!apps.containsKey(pkg)) return; }
            runOnUiThread(() -> {
                // Leanback-first fallback adapted from cleanyourtv AppLaunchActivity (MIT).
                Intent launch = getPackageManager().getLeanbackLaunchIntentForPackage(pkg);
                if (launch == null) launch = getPackageManager().getLaunchIntentForPackage(pkg);
                try {
                    if (launch == null) throw new ActivityNotFoundException();
                    startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                } catch (ActivityNotFoundException | SecurityException error) {
                    Toast.makeText(MainActivity.this, "Не вдалося відкрити застосунок", Toast.LENGTH_SHORT).show();
                }
            });
        }
        @JavascriptInterface public void settings(String target) {
            runOnUiThread(() -> {
                Intent intent = new Intent("wifi".equals(target) ? Settings.ACTION_WIFI_SETTINGS : Settings.ACTION_SETTINGS);
                try { startActivity(intent); }
                catch (ActivityNotFoundException | SecurityException error) {
                    Toast.makeText(MainActivity.this, "Системні налаштування недоступні", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override protected void onResume() {
        super.onResume();
        if (web != null) {
            web.onResume();
            web.evaluateJavascript("window.YoruRefresh && window.YoruRefresh()", null);
        }
    }
    @Override protected void onPause() {
        if (web != null) {
            web.evaluateJavascript("window.YoruPause && window.YoruPause()", null);
            web.onPause();
        }
        super.onPause();
    }
    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        String key;
        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_DPAD_UP: key = "ArrowUp"; break;
            case KeyEvent.KEYCODE_DPAD_DOWN: key = "ArrowDown"; break;
            case KeyEvent.KEYCODE_DPAD_LEFT: key = "ArrowLeft"; break;
            case KeyEvent.KEYCODE_DPAD_RIGHT: key = "ArrowRight"; break;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER: key = "Enter"; break;
            case KeyEvent.KEYCODE_BACK: key = "Escape"; break;
            case KeyEvent.KEYCODE_MENU: key = "ContextMenu"; break;
            default: return super.dispatchKeyEvent(event);
        }
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            // Text entry retains native cursor and IME handling.
            if (event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.isCtrlPressed()) return super.dispatchKeyEvent(event);
            web.evaluateJavascript("window.YoruRemote && window.YoruRemote(" + JSONObject.quote(key) + ", true)", null);
        }
        return true;
    }
    @Override public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (web != null && level >= TRIM_MEMORY_RUNNING_LOW) {
            web.evaluateJavascript("window.KonohaTrimMemory && window.KonohaTrimMemory()", null);
        }
    }
    @Override protected void onDestroy() {
        if (web != null) { web.removeJavascriptInterface("YoruNative"); web.destroy(); }
        super.onDestroy();
    }
}
