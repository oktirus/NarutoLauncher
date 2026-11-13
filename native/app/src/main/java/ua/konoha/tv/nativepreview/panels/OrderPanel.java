package ua.konoha.tv.nativepreview.panels;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

import ua.konoha.tv.nativepreview.apps.AppManager;
import ua.konoha.tv.nativepreview.model.AppItem;
import ua.konoha.tv.nativepreview.views.DockView;

import java.util.List;

public class OrderPanel extends ScrollView {
    public interface OnOrderActionListener {
        void onBackToLibraryRequested();
        void onCloseRequested();
    }

    private final AppManager appManager;
    private final DockView dockView;
    private final LinearLayout content;
    private final LinearLayout listContainer;
    private final TextView statusView;

    private OnOrderActionListener listener;
    private String focusTargetPackage = null;
    private boolean focusTargetIsDown = false;

    public OrderPanel(Context context, AppManager appManager, DockView dockView) {
        super(context);
        this.appManager = appManager;
        this.dockView = dockView;

        setBackgroundColor(Color.parseColor("#f00d2936"));
        setPadding(dp(24), dp(20), dp(24), dp(20));

        content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);

        // Header
        LinearLayout header = new LinearLayout(context);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(context);
        title.setText("Порядок обраного");
        title.setTextColor(Color.parseColor("#f2c576"));
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        header.addView(title, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        Button closeBtn = new Button(context);
        closeBtn.setText("✕ Закрити");
        closeBtn.setTextColor(Color.WHITE);
        closeBtn.setBackgroundColor(Color.parseColor("#254756"));
        closeBtn.setOnClickListener(v -> {
            if (listener != null) listener.onCloseRequested();
        });
        header.addView(closeBtn);
        content.addView(header);

        // Інструкція
        TextView note = new TextView(context);
        note.setText("Обери стрілку ↑ або ↓ біля застосунку та натисни OK. Перші шість позицій видно на головному екрані. Зміни зберігаються одразу.");
        note.setTextColor(Color.parseColor("#afc1b7"));
        note.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        note.setPadding(0, dp(10), 0, dp(10));
        content.addView(note);

        statusView = new TextView(context);
        statusView.setTextColor(Color.parseColor("#ed943f"));
        statusView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        statusView.setPadding(0, 0, 0, dp(12));
        content.addView(statusView);

        // Список рядків
        listContainer = new LinearLayout(context);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        content.addView(listContainer);

        // Кнопка переходу назад до архіву
        Button backBtn = new Button(context);
        backBtn.setText("← До архіву застосунків");
        backBtn.setTextColor(Color.WHITE);
        backBtn.setBackgroundColor(Color.parseColor("#1b4050"));
        LinearLayout.LayoutParams backLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        backLp.setMargins(0, dp(16), 0, dp(24));
        backBtn.setLayoutParams(backLp);
        backBtn.setOnClickListener(v -> {
            if (listener != null) listener.onBackToLibraryRequested();
        });
        content.addView(backBtn);

        addView(content);
    }

    public void setOnOrderActionListener(OnOrderActionListener listener) {
        this.listener = listener;
    }

    public void renderList() {
        listContainer.removeAllViews();
        List<AppItem> list = appManager.getPinnedApps();
        statusView.setText("Обраних застосунків: " + list.size());

        if (list.isEmpty()) {
            TextView emptyView = new TextView(getContext());
            emptyView.setText("Спочатку закріпи застосунки в архіві.");
            emptyView.setTextColor(Color.parseColor("#8aa2ad"));
            emptyView.setPadding(0, dp(20), 0, dp(20));
            listContainer.addView(emptyView);
            return;
        }

        for (int i = 0; i < list.size(); i++) {
            AppItem app = list.get(i);
            View row = createRow(app, i, list.size());
            listContainer.addView(row);
        }
    }

    private View createRow(AppItem app, int index, int total) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(8), dp(12), dp(8));
        row.setBackgroundColor(index < 6 ? Color.parseColor("#153745") : Color.parseColor("#0e2531"));

        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        rlp.setMargins(0, dp(3), 0, dp(3));
        row.setLayoutParams(rlp);

        // Номер
        TextView numView = new TextView(getContext());
        numView.setText(String.valueOf(index + 1));
        numView.setTextColor(Color.parseColor("#f2c576"));
        numView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        numView.setTypeface(Typeface.DEFAULT_BOLD);
        numView.setPadding(0, 0, dp(12), 0);
        row.addView(numView);

        // Іконка
        ImageView iconView = new ImageView(getContext());
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dp(44), dp(44));
        iconView.setLayoutParams(iconLp);
        iconView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        Drawable icon = dockView.getAppIconDrawable(app);
        if (icon != null) iconView.setImageDrawable(icon);
        row.addView(iconView);

        // Інформація (назва та стан)
        LinearLayout info = new LinearLayout(getContext());
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(dp(12), 0, dp(12), 0);

        TextView name = new TextView(getContext());
        name.setText(app.getName());
        name.setTextColor(Color.WHITE);
        name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        name.setTypeface(Typeface.DEFAULT_BOLD);
        info.addView(name);

        TextView loc = new TextView(getContext());
        loc.setText(index < 6 ? "На головному екрані" : "Поза головним екраном");
        loc.setTextColor(index < 6 ? Color.parseColor("#a8d5ba") : Color.parseColor("#8aa2ad"));
        loc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        info.addView(loc);

        row.addView(info, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        // Стрілки ↑ і ↓
        Button upBtn = new Button(getContext());
        upBtn.setText("↑");
        upBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        upBtn.setTextColor(Color.WHITE);
        upBtn.setBackgroundColor(Color.parseColor("#254756"));
        upBtn.setEnabled(index > 0);
        upBtn.setOnClickListener(v -> {
            if (appManager.moveFavorite(app.getPackageName(), -1)) {
                dockView.render();
                focusTargetPackage = app.getPackageName();
                focusTargetIsDown = false;
                renderList();
                statusView.setText(app.getName() + ": позиція " + index + " із " + total);
            }
        });
        row.addView(upBtn, new LinearLayout.LayoutParams(dp(44), dp(44)));

        Button downBtn = new Button(getContext());
        downBtn.setText("↓");
        downBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        downBtn.setTextColor(Color.WHITE);
        downBtn.setBackgroundColor(Color.parseColor("#254756"));
        downBtn.setEnabled(index < total - 1);
        LinearLayout.LayoutParams downLp = new LinearLayout.LayoutParams(dp(44), dp(44));
        downLp.setMargins(dp(6), 0, 0, 0);
        downBtn.setLayoutParams(downLp);
        downBtn.setOnClickListener(v -> {
            if (appManager.moveFavorite(app.getPackageName(), 1)) {
                dockView.render();
                focusTargetPackage = app.getPackageName();
                focusTargetIsDown = true;
                renderList();
                statusView.setText(app.getName() + ": позиція " + (index + 2) + " із " + total);
            }
        });
        row.addView(downBtn);

        if (app.getPackageName().equals(focusTargetPackage)) {
            Button target = focusTargetIsDown ? (downBtn.isEnabled() ? downBtn : upBtn) : (upBtn.isEnabled() ? upBtn : downBtn);
            target.post(target::requestFocus);
        }

        return row;
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics()
        );
    }
}
