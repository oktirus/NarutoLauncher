package ua.konoha.tv.nativepreview.panels;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import ua.konoha.tv.nativepreview.apps.AppManager;
import ua.konoha.tv.nativepreview.model.AppItem;
import ua.konoha.tv.nativepreview.views.ChakraFocusView;
import ua.konoha.tv.nativepreview.views.DockView;
import ua.konoha.tv.nativepreview.views.TechniqueRenderer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LibraryPanel extends FrameLayout {
    public interface OnLibraryActionListener {
        void onAppLaunch(AppItem app);
        void onOrderPanelRequested();
        void onCloseRequested();
        void onAppFocused(View view, AppItem app);
    }

    private final AppManager appManager;
    private final DockView dockView;
    private final ChakraFocusView focusView;
    private final ScrollView scrollView;
    private final LinearLayout content;
    private final EditText searchInput;
    private final TextView countView;
    private final Button filterButton;
    private final GridLayout grid;

    private int computedCardWidth = 0;
    private int computedColumnCount = 4;
    private String lastFocusedPackage = null;
    private boolean filterPinnedOnly = false;
    private OnLibraryActionListener listener;

    public LibraryPanel(Context context, AppManager appManager, DockView dockView, TechniqueRenderer techniqueRenderer) {
        super(context);
        this.appManager = appManager;
        this.dockView = dockView;

        setBackgroundColor(Color.parseColor("#f00d2936"));

        // Фокусний шар чакри всередині бібліотеки позаду карток
        focusView = new ChakraFocusView(context, techniqueRenderer);
        addView(focusView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
        ));

        scrollView = new ScrollView(context);
        scrollView.setBackgroundColor(Color.TRANSPARENT);
        scrollView.setPadding(dp(24), dp(20), dp(24), dp(20));

        content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);

        // Заголовок
        LinearLayout header = new LinearLayout(context);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(context);
        title.setText("Архів застосунків");
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
        closeBtn.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) focusView.setTargetView(null);
        });
        header.addView(closeBtn);
        content.addView(header);

        // Поле пошуку
        searchInput = new EditText(context);
        searchInput.setHint("Знайти застосунок…");
        searchInput.setHintTextColor(Color.parseColor("#8aa2ad"));
        searchInput.setTextColor(Color.WHITE);
        searchInput.setBackgroundColor(Color.parseColor("#153340"));
        searchInput.setPadding(dp(12), dp(10), dp(12), dp(10));
        searchInput.setSingleLine(true);
        LinearLayout.LayoutParams searchLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        searchLp.setMargins(0, dp(14), 0, dp(10));
        searchInput.setLayoutParams(searchLp);
        searchInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) focusView.setTargetView(null);
        });
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { renderGrid(); }
            @Override public void afterTextChanged(Editable s) {}
        });
        content.addView(searchInput);

        // Тулбар: лічильник та кнопки фільтра
        LinearLayout toolbar = new LinearLayout(context);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(0, 0, 0, dp(12));

        countView = new TextView(context);
        countView.setTextColor(Color.parseColor("#abc1b8"));
        countView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        toolbar.addView(countView, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        filterButton = new Button(context);
        filterButton.setText("Лише обране");
        filterButton.setTextColor(Color.WHITE);
        filterButton.setBackgroundColor(Color.parseColor("#1b4050"));
        filterButton.setOnClickListener(v -> {
            filterPinnedOnly = !filterPinnedOnly;
            filterButton.setBackgroundColor(Color.parseColor(filterPinnedOnly ? "#ed943f" : "#1b4050"));
            renderGrid();
        });
        filterButton.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) focusView.setTargetView(null);
        });
        toolbar.addView(filterButton);

        Button orderBtn = new Button(context);
        orderBtn.setText("Порядок обраного");
        orderBtn.setTextColor(Color.WHITE);
        orderBtn.setBackgroundColor(Color.parseColor("#1b4050"));
        LinearLayout.LayoutParams orderLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        orderLp.setMargins(dp(8), 0, 0, 0);
        orderBtn.setLayoutParams(orderLp);
        orderBtn.setOnClickListener(v -> {
            if (listener != null) listener.onOrderPanelRequested();
        });
        orderBtn.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) focusView.setTargetView(null);
        });
        toolbar.addView(orderBtn);

        content.addView(toolbar);

        // Адаптивна сітка застосунків
        grid = new GridLayout(context);
        grid.setColumnCount(computedColumnCount);
        grid.setAlignmentMode(GridLayout.ALIGN_BOUNDS);
        content.addView(grid);

        scrollView.addView(content);
        addView(scrollView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
        ));
    }

    public void setTechnique(String technique) {
        focusView.setTechnique(technique);
    }

    public void setMotionEnabled(boolean enabled) {
        focusView.setMotionEnabled(enabled);
    }

    public void setOnLibraryActionListener(OnLibraryActionListener listener) {
        this.listener = listener;
    }

    public void open() {
        searchInput.setText("");
        filterPinnedOnly = false;
        filterButton.setBackgroundColor(Color.parseColor("#1b4050"));
        renderGrid();
        View target = null;
        if (lastFocusedPackage != null) {
            target = findCardByPackage(lastFocusedPackage);
        }
        if (target != null) {
            target.requestFocus();
            focusView.setTargetView(target);
        } else {
            filterButton.requestFocus();
        }
    }

    public void close() {
        focusView.pause();
        focusView.setTargetView(null);
        grid.removeAllViews();
    }

    public void pause() {
        focusView.pause();
    }

    public void resume() {
        focusView.resume();
        focusView.invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        if (w > 0) {
            int availableWidth = w - dp(48);
            AdaptiveGridHelper.LayoutResult res = AdaptiveGridHelper.compute(
                    availableWidth, dp(110), dp(180), dp(6), 2, 8
            );
            if (res.columnCount != computedColumnCount || res.cardWidthPx != computedCardWidth) {
                computedColumnCount = res.columnCount;
                computedCardWidth = res.cardWidthPx;
                PickerGrid.reflowColumns(grid, computedColumnCount);
                applyCardWidths();
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    private void applyCardWidths() {
        if (computedCardWidth <= 0) return;
        for (int i = 0; i < grid.getChildCount(); i++) {
            View child = grid.getChildAt(i);
            ViewGroup.LayoutParams lp = child.getLayoutParams();
            if (lp != null && lp.width != computedCardWidth) {
                lp.width = computedCardWidth;
                child.setLayoutParams(lp);
            }
        }
    }

    public void renderGrid() {
        View focused = findFocus();
        boolean restoreGridFocus = isInsideGrid(focused);
        focusView.setTargetView(null);
        grid.removeAllViews();
        String query = searchInput.getText().toString().trim().toLowerCase(Locale.ROOT);
        List<AppItem> all = appManager.getAllApps();
        List<AppItem> filtered = new ArrayList<>();

        for (AppItem app : all) {
            boolean matchesQuery = app.getName().toLowerCase(Locale.ROOT).contains(query);
            boolean matchesPin = !filterPinnedOnly || app.isPinned();
            if (matchesQuery && matchesPin) {
                filtered.add(app);
            }
        }

        countView.setText(filtered.size() + " застосунків");

        int availableWidth = getWidth() > 0 ? getWidth() - dp(48) : getResources().getDisplayMetrics().widthPixels - dp(48);
        AdaptiveGridHelper.LayoutResult res = AdaptiveGridHelper.compute(
                availableWidth, dp(110), dp(180), dp(6), 2, 8
        );
        computedColumnCount = res.columnCount;
        computedCardWidth = res.cardWidthPx;
        grid.setColumnCount(computedColumnCount);

        if (filtered.isEmpty()) {
            TextView emptyView = new TextView(getContext());
            emptyView.setText("Цього застосунку в архіві немає. Спробуй іншу назву або вимкни фільтр.");
            emptyView.setTextColor(Color.parseColor("#8aa2ad"));
            emptyView.setGravity(Gravity.CENTER);
            emptyView.setPadding(0, dp(40), 0, dp(40));
            grid.addView(emptyView);
            focusView.setTargetView(null);
            if (restoreGridFocus) filterButton.requestFocus();
            return;
        }

        for (AppItem app : filtered) {
            View card = createGridItem(app);
            grid.addView(card);
        }

        // Відновлення фокусу за ідентифікатором пакета (package identity focus recovery)
        if (restoreGridFocus) {
            View restoredCard = findCardByPackage(lastFocusedPackage);
            if (restoredCard == null) restoredCard = findCardByPackage(filtered.get(0).getPackageName());
            if (restoredCard != null) {
                restoredCard.requestFocus();
                focusView.setTargetView(restoredCard);
            }
        }
    }

    private boolean isInsideGrid(View view) {
        for (View current = view; current != null; ) {
            if (current == grid) return true;
            android.view.ViewParent parent = current.getParent();
            current = parent instanceof View ? (View) parent : null;
        }
        return false;
    }

    public View findCardByPackage(String packageName) {
        if (packageName == null) return null;
        for (int i = 0; i < grid.getChildCount(); i++) {
            View item = grid.getChildAt(i);
            if (item != null && packageName.equals(item.getTag())) {
                if (item instanceof ViewGroup && ((ViewGroup) item).getChildCount() > 0) {
                    return ((ViewGroup) item).getChildAt(0);
                }
                return item;
            }
        }
        return null;
    }

    private View createGridItem(AppItem app) {
        LinearLayout item = new LinearLayout(getContext());
        item.setTag(app.getPackageName());
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER_HORIZONTAL);
        item.setPadding(dp(8), dp(10), dp(8), dp(10));
        item.setBackgroundColor(Color.TRANSPARENT);

        GridLayout.LayoutParams glp = new GridLayout.LayoutParams();
        glp.width = computedCardWidth > 0 ? computedCardWidth : dp(140);
        glp.height = GridLayout.LayoutParams.WRAP_CONTENT;
        glp.setMargins(dp(6), dp(6), dp(6), dp(6));
        item.setLayoutParams(glp);

        // Картка для запуску
        LinearLayout card = new LinearLayout(getContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setFocusable(true);
        card.setFocusableInTouchMode(true);
        card.setPadding(dp(4), dp(4), dp(4), dp(4));

        ImageView iconView = new ImageView(getContext());
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dp(72), dp(72));
        iconView.setLayoutParams(iconLp);
        iconView.setScaleType(ImageView.ScaleType.FIT_CENTER);

        dockView.bindAppIcon(iconView, app);
        card.addView(iconView);

        TextView nameView = new TextView(getContext());
        nameView.setText(app.getName());
        nameView.setTextColor(Color.WHITE);
        nameView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        nameView.setTypeface(Typeface.DEFAULT_BOLD);
        nameView.setSingleLine(true);
        nameView.setEllipsize(TextUtils.TruncateAt.END);
        nameView.setGravity(Gravity.CENTER);
        nameView.setPadding(0, dp(4), 0, 0);
        card.addView(nameView);

        card.setOnClickListener(v -> {
            if (listener != null) listener.onAppLaunch(app);
        });

        card.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                lastFocusedPackage = app.getPackageName();
                focusView.setTargetView(v);
                if (listener != null) {
                    listener.onAppFocused(v, app);
                }
            }
        });

        item.addView(card);

        // Кнопка закріплення
        Button pinBtn = new Button(getContext());
        updatePinButton(pinBtn, app.isPinned());
        pinBtn.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) focusView.setTargetView(null);
        });
        pinBtn.setOnClickListener(v -> {
            boolean nowPinned = appManager.togglePin(app.getPackageName());
            updatePinButton(pinBtn, nowPinned);
            dockView.render();
            if (filterPinnedOnly && !nowPinned) {
                renderGrid();
            }
        });
        item.addView(pinBtn);

        return item;
    }

    private void updatePinButton(Button btn, boolean isPinned) {
        btn.setText(isPinned ? "★ Закріплено" : "☆ Закріпити");
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        btn.setTextColor(isPinned ? Color.parseColor("#ed943f") : Color.parseColor("#a5bbb1"));
        btn.setBackgroundColor(Color.TRANSPARENT);
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics()
        );
    }
}
