package ua.konoha.tv.nativepreview.apps;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.role.RoleManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

/** User-approved HOME selection, with a one-time suggestion after first launch. */
public final class HomeScreenSetup {
    private static final String TAG = "HomeScreenSetup";
    private static final String PROMPT_SHOWN = "prompt_shown";
    private static final int REQUEST_HOME_ROLE = 501;
    private final Activity activity;
    private final SharedPreferences preferences;
    private AlertDialog prompt;

    public HomeScreenSetup(Activity activity) {
        this.activity = activity;
        preferences = activity.getSharedPreferences("home_screen_setup", Context.MODE_PRIVATE);
    }

    public boolean isDefaultHome() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            RoleManager roles = activity.getSystemService(RoleManager.class);
            if (roles != null && roles.isRoleAvailable(RoleManager.ROLE_HOME)
                    && roles.isRoleHeld(RoleManager.ROLE_HOME)) return true;
        }
        Intent home = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
        ResolveInfo resolved = activity.getPackageManager().resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY);
        return resolved != null && resolved.activityInfo != null
                && activity.getPackageName().equals(resolved.activityInfo.packageName);
    }

    public void showFirstRunPrompt() {
        if (activity.isFinishing() || activity.isDestroyed() || prompt != null
                || preferences.getBoolean(PROMPT_SHOWN, false)) return;
        if (isDefaultHome()) {
            preferences.edit().putBoolean(PROMPT_SHOWN, true).apply();
            return;
        }
        prompt = new AlertDialog.Builder(activity)
                .setTitle("Зробити Коноху домашнім екраном?")
                .setMessage("Кнопка Home відкриватиме Коноху, і вона з’являтиметься після перезавантаження телевізора. "
                        + "Змінити вибір можна в налаштуваннях домашнього екрана.")
                .setPositiveButton("Обрати", (dialog, which) -> openSelection())
                .setNegativeButton("Пізніше", null)
                .create();
        prompt.setOnDismissListener(dialog -> prompt = null);
        prompt.show();
        preferences.edit().putBoolean(PROMPT_SHOWN, true).apply();
    }

    public void openSelection() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            RoleManager roles = activity.getSystemService(RoleManager.class);
            if (roles != null && roles.isRoleAvailable(RoleManager.ROLE_HOME)
                    && !roles.isRoleHeld(RoleManager.ROLE_HOME)) {
                try {
                    activity.startActivityForResult(roles.createRequestRoleIntent(RoleManager.ROLE_HOME), REQUEST_HOME_ROLE);
                    return;
                } catch (ActivityNotFoundException | SecurityException | IllegalArgumentException error) {
                    Log.w(TAG, "HOME role request unavailable; trying system settings", error);
                }
            }
        }
        for (String action : new String[]{Settings.ACTION_HOME_SETTINGS, Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS}) {
            try {
                activity.startActivity(new Intent(action));
                return;
            } catch (ActivityNotFoundException | SecurityException error) {
                Log.w(TAG, "System settings unavailable: " + action, error);
            }
        }
        Toast.makeText(activity, "Не вдалося відкрити вибір домашнього екрана. "
                + "Перевірте його в системних налаштуваннях телевізора.", Toast.LENGTH_LONG).show();
    }

    public void close() {
        if (prompt != null) prompt.dismiss();
    }

    public boolean isPromptShowing() {
        return prompt != null && prompt.isShowing();
    }
}
