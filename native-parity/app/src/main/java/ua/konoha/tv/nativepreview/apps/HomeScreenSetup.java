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
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;
import ua.konoha.tv.nativepreview.R;

/** User-approved HOME selection, with a one-time suggestion after first launch. */
public final class HomeScreenSetup {
    private static final String TAG = "HomeScreenSetup";
    private static final String PROMPT_SHOWN = "prompt_shown";
    private static final int REQUEST_HOME_ROLE = 501;
    private static final String STATE_AWAITING_ROLE = "homeSetup.awaitingRole";
    private static final String STATE_AWAITING_SETTINGS = "homeSetup.awaitingSettings";
    private final Activity activity;
    private final SharedPreferences preferences;
    private AlertDialog prompt;
    private boolean awaitingRoleResult;
    private boolean awaitingSettingsResult;

    public HomeScreenSetup(Activity activity) {
        this(activity, null);
    }

    public HomeScreenSetup(Activity activity, Bundle savedState) {
        this.activity = activity;
        preferences = activity.getSharedPreferences("home_screen_setup", Context.MODE_PRIVATE);
        if (savedState != null) {
            awaitingRoleResult = savedState.getBoolean(STATE_AWAITING_ROLE);
            awaitingSettingsResult = savedState.getBoolean(STATE_AWAITING_SETTINGS);
        }
    }

    public boolean isDefaultHome() {
        return activity.getPackageName().equals(resolvedHomePackage(activity));
    }

    static String resolvedHomePackage(Context context) {
        // A HOME role grant can disagree with the activity Android actually resolves.
        Intent home = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
        ResolveInfo resolved = context.getPackageManager().resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY);
        return resolved != null && resolved.activityInfo != null
                ? resolved.activityInfo.packageName : null;
    }

    public void showRedirectSettings() {
        new AlertDialog.Builder(activity)
                .setTitle(R.string.home_redirect_title)
                .setMessage(R.string.home_redirect_setup)
                .setPositiveButton(R.string.home_redirect_open_settings, (dialog, which) -> {
                    try {
                        activity.startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                    } catch (ActivityNotFoundException | SecurityException error) {
                        Toast.makeText(activity, R.string.home_redirect_settings_unavailable, Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    public void saveState(Bundle outState) {
        outState.putBoolean(STATE_AWAITING_ROLE, awaitingRoleResult);
        outState.putBoolean(STATE_AWAITING_SETTINGS, awaitingSettingsResult);
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
                .setMessage("У системному списку обери «Naruto · Коноха · Native» як домашній екран. "
                        + "Якщо з’явиться вибір «Лише раз» або «Завжди», обери «Завжди». "
                        + "Після повернення перевіримо, чи застосувався вибір.")
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
                    awaitingRoleResult = true;
                    activity.startActivityForResult(roles.createRequestRoleIntent(RoleManager.ROLE_HOME), REQUEST_HOME_ROLE);
                    return;
                } catch (ActivityNotFoundException | SecurityException | IllegalArgumentException error) {
                    awaitingRoleResult = false;
                    Log.w(TAG, "HOME role request unavailable; trying system settings", error);
                }
            }
        }
        openSystemSettings();
    }

    private void openSystemSettings() {
        for (String action : new String[]{Settings.ACTION_HOME_SETTINGS, Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS}) {
            try {
                activity.startActivity(new Intent(action));
                awaitingSettingsResult = true;
                return;
            } catch (ActivityNotFoundException | SecurityException error) {
                Log.w(TAG, "System settings unavailable: " + action, error);
            }
        }
        Toast.makeText(activity, "Не вдалося відкрити вибір домашнього екрана. "
                + "Перевірте його в системних налаштуваннях телевізора.", Toast.LENGTH_LONG).show();
    }

    public boolean onActivityResult(int requestCode) {
        if (requestCode != REQUEST_HOME_ROLE || !awaitingRoleResult) return false;
        awaitingRoleResult = false;
        showSelectionResult();
        return true;
    }

    public void onResume() {
        if (!awaitingSettingsResult) return;
        awaitingSettingsResult = false;
        showSelectionResult();
    }

    private void showSelectionResult() {
        if (activity.isFinishing() || activity.isDestroyed()) return;
        if (isDefaultHome()) {
            Toast.makeText(activity, "Коноха Native — домашній екран за замовчуванням", Toast.LENGTH_LONG).show();
            return;
        }
        if (prompt != null) return;
        prompt = new AlertDialog.Builder(activity)
                .setTitle("Домашній екран ще не змінено")
                .setMessage("Android поки не відкриває Коноху Native як домашній екран. "
                        + "У налаштуваннях домашнього екрана обери «Naruto · Коноха · Native». "
                        + "Стара «Naruto · Коноха» — інший застосунок. "
                        + "Після вибору перевір кнопку Home. Якщо вибір не зберігається, "
                        + "потрібно перевірити обмеження конкретної прошивки телевізора.")
                .setPositiveButton("Налаштування HOME", (dialog, which) -> openSystemSettings())
                .setNegativeButton("Пізніше", null)
                .create();
        prompt.setOnDismissListener(dialog -> prompt = null);
        prompt.show();
    }

    public void close() {
        awaitingRoleResult = false;
        awaitingSettingsResult = false;
        if (prompt != null) prompt.dismiss();
    }

    public boolean isPromptShowing() {
        return prompt != null && prompt.isShowing();
    }
}
