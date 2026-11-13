package ua.konoha.tv.nativepreview.panels;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.*;

import ua.konoha.tv.nativepreview.apps.AppManager;
import ua.konoha.tv.nativepreview.model.AppItem;
import ua.konoha.tv.nativepreview.theme.KonohaTheme;
import ua.konoha.tv.nativepreview.views.DockView;
import ua.konoha.tv.nativepreview.views.TechniqueRenderer;
import ua.konoha.tv.nativepreview.views.ChakraFocusView;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Library panel (.library-grid) reproducing Chromium's App Archive.
 * 4-column side sheet (1050px) with custom search box, filter toolbar, and pin buttons.
 */
public class LibraryPanel extends FrameLayout {
    public interface OnLibraryActionListener {
        void onAppLaunch(AppItem app);
        void onOrderPanelRequested();
        void onCloseRequested();
        void onAppFocused(View view, AppItem app);
    }

    private final AppManager appManager;
    private final DockView dockView;
    private final SideSheetContainer sheetContainer;
    private final ScrollView scrollView;
    private final LinearLayout content;
    private final EditText searchInput;
    private final TextView countView;
    private final Button filterButton;
    private final Button orderButton;
    private final GridLayout grid;
    private final ChakraFocusView focusView;
    private boolean observingFocus;
    private final ViewTreeObserver.OnGlobalFocusChangeListener focusListener =
            (oldFocus, newFocus) -> updateAuraTarget(newFocus);
    private final ViewTreeObserver.OnScrollChangedListener scrollListener = this::invalidateAura;

    private int computedColumnCount = 4;
    private int computedCardWidth = 0;
    private boolean filterPinnedOnly = false;
    private OnLibraryActionListener listener;

    private final Map<String, Bitmap> iconCache = new HashMap<>();

    public LibraryPanel(Context context, AppManager appManager, DockView dockView, TechniqueRenderer techniqueRenderer) {
        super(context);
        this.appManager = appManager;
        this.dockView = dockView;

        sheetContainer = new SideSheetContainer(context, true, "Архів Конохи", "Архів застосунків");
        sheetContainer.setOnCloseListener(() -> {
            if (listener != null) listener.onCloseRequested();
        });

        scrollView = new ScrollView(context);
        scrollView.setVerticalScrollBarEnabled(false);

        content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);

        // 1. Search Bar (.library-search)
        searchInput = new EditText(context);
        searchInput.setHint("Знайти застосунок…");
        searchInput.setHintTextColor(Color.parseColor("#8aa2ad"));
        searchInput.setTextColor(Color.WHITE);
        searchInput.setTextSize(14);
        searchInput.setTypeface(KonohaTheme.getNunito(context));
        searchInput.setSingleLine(true);
        searchInput.setPadding(KonohaTheme.dp(context, 16), KonohaTheme.dp(context, 12),
                KonohaTheme.dp(context, 16), KonohaTheme.dp(context, 12));

        GradientDrawable searchBg = new GradientDrawable();
        searchBg.setColor(Color.parseColor("#153340"));
        searchBg.setCornerRadius(KonohaTheme.dp(context, 4));
        searchBg.setStroke(KonohaTheme.dp(context, 1), Color.parseColor("#33dbbd7d"));
        searchInput.setBackground(searchBg);

        LinearLayout.LayoutParams sLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        sLp.setMargins(0, 0, 0, KonohaTheme.dp(context, 16));
        searchInput.setLayoutParams(sLp);

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                renderApps();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        content.addView(searchInput);

        // 2. Toolbar (.library-toolbar)
        LinearLayout toolbar = new LinearLayout(context);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(0, 0, 0, KonohaTheme.dp(context, 16));

        countView = new TextView(context);
        countView.setTextColor(Color.parseColor("#bac8bc"));
        countView.setTextSize(12);
        countView.setTypeface(KonohaTheme.getNunito(context));
        toolbar.addView(countView, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        filterButton = createToolbarButton("Лише обране", v -> {
            filterPinnedOnly = !filterPinnedOnly;
            updateFilterButtonState();
            renderApps();
        });
        toolbar.addView(filterButton);

        orderButton = createToolbarButton("Порядок обраного", v -> {
            if (listener != null) listener.onOrderPanelRequested();
        });
        LinearLayout.LayoutParams oLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, KonohaTheme.dp(context, 32)
        );
        oLp.setMargins(KonohaTheme.dp(context, 10), 0, 0, 0);
        orderButton.setLayoutParams(oLp);
        toolbar.addView(orderButton);

        content.addView(toolbar);

        // 3. App Grid (4 columns)
        grid = new GridLayout(context);
        grid.setColumnCount(computedColumnCount);
        grid.setAlignmentMode(GridLayout.ALIGN_BOUNDS);
        content.addView(grid, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        scrollView.addView(content, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        // Ordinary techniques sit behind the cards; Gamabunta is above them.
        FrameLayout body = new FrameLayout(context);
        focusView = new ChakraFocusView(context, techniqueRenderer);
        body.addView(focusView, new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        scrollView.setElevation(10f);
        body.addView(scrollView, new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        body.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> invalidateAura());
        sheetContainer.setBodyView(body);
        addView(sheetContainer, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    }

    public void setOnLibraryActionListener(OnLibraryActionListener listener) {
        this.listener = listener;
    }

    public void open() {
        renderGrid();
    }

    public void close() {
        release();
    }

    public void pause() { focusView.pause(); }

    public void resume() {
        focusView.resume();
        renderGrid();
    }

    public void setTechnique(String technique) { focusView.setTechnique(technique); }

    public void setMotionEnabled(boolean enabled) { focusView.setMotionEnabled(enabled); }

    private void updateAuraTarget(View focused) {
        View card = focused;
        while (card != null && card.getParent() != grid) {
            card = card.getParent() instanceof View ? (View) card.getParent() : null;
        }
        focusView.setTargetView(card);
    }

    private void invalidateAura() { focusView.invalidate(); }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!observingFocus) {
            getViewTreeObserver().addOnGlobalFocusChangeListener(focusListener);
            getViewTreeObserver().addOnScrollChangedListener(scrollListener);
            observingFocus = true;
        }
    }

    private void stopObservingFocus() {
        if (observingFocus && getViewTreeObserver().isAlive()) {
            getViewTreeObserver().removeOnGlobalFocusChangeListener(focusListener);
            getViewTreeObserver().removeOnScrollChangedListener(scrollListener);
        }
        observingFocus = false;
    }

    @Override protected void onDetachedFromWindow() {
        stopObservingFocus();
        super.onDetachedFromWindow();
    }

    public void renderGrid() {
        renderApps();
    }

    public View findCardByPackage(String packageName) {
        if (packageName == null) return null;
        for (int i = 0; i < grid.getChildCount(); i++) {
            View item = grid.getChildAt(i);
            if (item != null && packageName.equals(item.getTag())) {
                return item;
            }
        }
        return null;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        if (w > 0) {
            int sheetW = Math.min(KonohaTheme.dp(getContext(), 1050), (int) (w * 0.95f));
            int pad = KonohaTheme.dp(getContext(), 72);
            int availableWidth = Math.max(100, sheetW - pad);
            int margin = KonohaTheme.dp(getContext(), 6);
            computedColumnCount = 4;
            computedCardWidth = (availableWidth - margin * 2 * computedColumnCount) / computedColumnCount;
            if (computedColumnCount != grid.getColumnCount() || computedCardWidth > 0) {
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
            GridLayout.LayoutParams lp = (GridLayout.LayoutParams) child.getLayoutParams();
            if (lp != null && lp.width != computedCardWidth) {
                lp.width = computedCardWidth;
                child.setLayoutParams(lp);
            }
        }
    }

    private Button createToolbarButton(String text, View.OnClickListener clickListener) {
        Button btn = new Button(getContext());
        btn.setText(text);
        btn.setTextColor(KonohaTheme.COLOR_PAPER);
        btn.setTextSize(11);
        btn.setTypeface(KonohaTheme.getNunitoBold(getContext()));
        btn.setFocusable(true);
        btn.setFocusableInTouchMode(true);
        btn.setPadding(KonohaTheme.dp(getContext(), 12), 0, KonohaTheme.dp(getContext(), 12), 0);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#153745"));
        bg.setCornerRadius(KonohaTheme.dp(getContext(), 3));
        bg.setStroke(KonohaTheme.dp(getContext(), 1), Color.parseColor("#44dbbd7d"));
        btn.setBackground(bg);

        btn.setOnClickListener(clickListener);
        btn.setOnFocusChangeListener((v, hasFocus) -> {
            GradientDrawable d = new GradientDrawable();
            d.setColor(hasFocus ? Color.parseColor("#2a4f60") : Color.parseColor("#153745"));
            d.setCornerRadius(KonohaTheme.dp(getContext(), 3));
            d.setStroke(KonohaTheme.dp(getContext(), hasFocus ? 2 : 1),
                    hasFocus ? KonohaTheme.COLOR_ORANGE : Color.parseColor("#44dbbd7d"));
            v.setBackground(d);
        });

        return btn;
    }

    private void updateFilterButtonState() {
        GradientDrawable bg = new GradientDrawable();
        if (filterPinnedOnly) {
            bg.setColor(KonohaTheme.COLOR_PAPER);
            filterButton.setTextColor(KonohaTheme.COLOR_INK);
        } else {
            bg.setColor(Color.parseColor("#153745"));
            filterButton.setTextColor(KonohaTheme.COLOR_PAPER);
        }
        bg.setCornerRadius(KonohaTheme.dp(getContext(), 3));
        bg.setStroke(KonohaTheme.dp(getContext(), 1), Color.parseColor("#44dbbd7d"));
        filterButton.setBackground(bg);
    }

    public void renderApps() {
        View focusedCard = grid.getFocusedChild();
        String focusedPackage = focusedCard != null ? (String) focusedCard.getTag() : null;
        boolean pinFocused = focusedCard != null && focusedCard.findFocus() != focusedCard;
        focusView.setTargetView(null);
        grid.removeAllViews();
        String query = searchInput.getText().toString().trim().toLowerCase(new Locale("uk", "UA"));

        List<AppItem> allApps = appManager.getAllApps();
        List<AppItem> filtered = new ArrayList<>();

        for (AppItem app : allApps) {
            if (filterPinnedOnly && !app.isPinned()) continue;
            if (!query.isEmpty() && !app.getName().toLowerCase(new Locale("uk", "UA")).contains(query)) {
                continue;
            }
            filtered.add(app);
        }

        countView.setText(filtered.size() + " застосунків");

        int availableWidth = getWidth() > 0 ? getWidth() : getResources().getDisplayMetrics().widthPixels;
        int sheetWidth = Math.min(KonohaTheme.dp(getContext(), 1050), (int) (availableWidth * 0.95f));
        int netWidth = Math.max(100, sheetWidth - KonohaTheme.dp(getContext(), 72));
        int margin = KonohaTheme.dp(getContext(), 6);
        computedColumnCount = 4;
        computedCardWidth = (netWidth - margin * 2 * computedColumnCount) / computedColumnCount;
        grid.setColumnCount(computedColumnCount);

        if (filtered.isEmpty()) {
            TextView empty = new TextView(getContext());
            empty.setText("Не знайдено жодного застосунку.");
            empty.setTextColor(Color.parseColor("#b9c8b8"));
            empty.setTextSize(13);
            empty.setTypeface(KonohaTheme.getNunito(getContext()));
            empty.setPadding(0, KonohaTheme.dp(getContext(), 40), 0, 0);
            empty.setGravity(Gravity.CENTER);
            grid.addView(empty);
            if (focusedPackage != null) sheetContainer.getCloseButton().requestFocus();
            return;
        }

        for (AppItem app : filtered) {
            View card = createLibraryCard(app);
            grid.addView(card);
        }
        if (focusedPackage != null) {
            View replacement = findCardByPackage(focusedPackage);
            if (replacement != null) {
                View target = pinFocused ? replacement.findViewById(android.R.id.button1) : replacement;
                target.requestFocus();
                updateAuraTarget(target);
            } else {
                sheetContainer.getCloseButton().requestFocus();
            }
        }
    }

    private View createLibraryCard(AppItem app) {
        LinearLayout card = new LinearLayout(getContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setFocusable(true);
        card.setFocusableInTouchMode(true);
        card.setClickable(true);
        card.setTag(app.getPackageName());
        card.setPadding(KonohaTheme.dp(getContext(), 10), KonohaTheme.dp(getContext(), 10),
                KonohaTheme.dp(getContext(), 10), KonohaTheme.dp(getContext(), 8));

        GridLayout.LayoutParams glp = new GridLayout.LayoutParams();
        glp.width = computedCardWidth > 0 ? computedCardWidth : KonohaTheme.dp(getContext(), 180);
        glp.height = GridLayout.LayoutParams.WRAP_CONTENT;
        int m = KonohaTheme.dp(getContext(), 6);
        glp.setMargins(m, m, m, m);
        card.setLayoutParams(glp);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.TRANSPARENT);
        bg.setCornerRadius(KonohaTheme.dp(getContext(), 4));
        card.setBackground(bg);

        // Icon
        ImageView iconView = new ImageView(getContext());
        int iconSize = KonohaTheme.dp(getContext(), 80);
        iconView.setLayoutParams(new LinearLayout.LayoutParams(iconSize, iconSize));
        iconView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        bindAppIcon(iconView, app);
        card.addView(iconView);

        // Title
        TextView nameView = new TextView(getContext());
        nameView.setText(app.getName());
        nameView.setTextColor(Color.WHITE);
        nameView.setTextSize(13);
        nameView.setTypeface(KonohaTheme.getNunitoBold(getContext()));
        nameView.setSingleLine(true);
        nameView.setEllipsize(TextUtils.TruncateAt.END);
        nameView.setGravity(Gravity.CENTER);
        nameView.setPadding(0, KonohaTheme.dp(getContext(), 6), 0, 0);
        card.addView(nameView);

        // Description
        TextView descView = new TextView(getContext());
        descView.setText(app.getDescription());
        descView.setTextColor(Color.parseColor("#8aa2ad"));
        descView.setTextSize(10);
        descView.setTypeface(KonohaTheme.getNunito(getContext()));
        descView.setSingleLine(true);
        descView.setEllipsize(TextUtils.TruncateAt.END);
        descView.setGravity(Gravity.CENTER);
        card.addView(descView);

        // Pin button
        TextView pinBtn = new TextView(getContext());
        pinBtn.setText(app.isPinned() ? "★ Закріплено" : "✦ Закріпити");
        pinBtn.setTextColor(app.isPinned() ? KonohaTheme.COLOR_ORANGE : Color.parseColor("#a5bbb1"));
        pinBtn.setTextSize(10);
        pinBtn.setTypeface(KonohaTheme.getNunito(getContext()));
        pinBtn.setPadding(0, KonohaTheme.dp(getContext(), 6), 0, 0);
        pinBtn.setClickable(true);
        pinBtn.setId(android.R.id.button1);
        pinBtn.setFocusable(true);
        pinBtn.setFocusableInTouchMode(true);
        pinBtn.setOnClickListener(v -> {
            appManager.togglePin(app.getPackageName());
            if (dockView != null) dockView.render();
            renderApps();
        });
        card.addView(pinBtn);

        card.setOnClickListener(v -> {
            if (listener != null) listener.onAppLaunch(app);
        });

        card.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == android.view.KeyEvent.ACTION_DOWN &&
                    (keyCode == android.view.KeyEvent.KEYCODE_DPAD_CENTER
                            || keyCode == android.view.KeyEvent.KEYCODE_ENTER
                            || keyCode == android.view.KeyEvent.KEYCODE_NUMPAD_ENTER)) {
                card.performClick();
                return true;
            }
            return false;
        });

        card.setOnFocusChangeListener((v, hasFocus) -> {
            v.setScaleX(hasFocus ? 1.05f : 1.0f);
            v.setScaleY(hasFocus ? 1.05f : 1.0f);
            GradientDrawable d = new GradientDrawable();
            d.setColor(hasFocus ? Color.parseColor("#14153340") : Color.TRANSPARENT);
            d.setCornerRadius(KonohaTheme.dp(getContext(), 4));
            d.setStroke(KonohaTheme.dp(getContext(), hasFocus ? 2 : 0),
                    hasFocus ? KonohaTheme.COLOR_GOLD : Color.TRANSPARENT);
            v.setBackground(d);
            if (hasFocus && listener != null) {
                listener.onAppFocused(v, app);
            }
        });

        return card;
    }

    private void bindAppIcon(ImageView iconView, AppItem app) {
        String assetPath = app.getAssetPath();
        if (assetPath != null) {
            Bitmap cached = iconCache.get(assetPath);
            if (cached != null) {
                iconView.setImageBitmap(cached);
                return;
            }
            try (InputStream is = getContext().getAssets().open(assetPath)) {
                Bitmap bmp = BitmapFactory.decodeStream(is);
                if (bmp != null) {
                    iconCache.put(assetPath, bmp);
                    iconView.setImageBitmap(bmp);
                    return;
                }
            } catch (Exception ignored) {}
        }
        if (app.getSystemIcon() != null) {
            iconView.setImageDrawable(app.getSystemIcon());
        }
    }

    public void release() {
        stopObservingFocus();
        focusView.pause();
        focusView.setTargetView(null);
        grid.removeAllViews();
        for (Bitmap bmp : iconCache.values()) {
            if (bmp != null && !bmp.isRecycled()) bmp.recycle();
        }
        iconCache.clear();
    }
}
