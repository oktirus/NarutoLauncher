package ua.konoha.tv.nativepreview.panels;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import ua.konoha.tv.nativepreview.model.CatalogData;
import ua.konoha.tv.nativepreview.model.Technique;
import ua.konoha.tv.nativepreview.prefs.PreferencesManager;
import ua.konoha.tv.nativepreview.views.TechniqueRenderer;

import java.util.HashMap;
import java.util.Map;

public class TechniquesPanel extends ScrollView {
    public interface OnTechniqueSelectedListener {
        void onTechniqueSelected(Technique technique);
        void onCloseRequested();
    }

    private final PreferencesManager prefs;
    private final TechniqueRenderer renderer;
    private final GridLayout grid;
    private final Map<String, Bitmap> previewBitmaps = new HashMap<>();
    private final Map<String, ImageView> previewViews = new HashMap<>();

    private int computedCardWidth = 0;
    private int computedColumnCount = 2;
    private boolean isListenerRegistered = false;

    private final TechniqueRenderer.OnBitmapLoadedListener bitmapListener = name -> {
        // C10: Відображаємо залежні ключі пластин на ідентифікатор картки техніки (gamabunta-idle -> gamabunta)
        String techId = "gamabunta-idle".equals(name) ? "gamabunta" : name;
        ImageView iv = previewViews.get(techId);
        if (iv != null) {
            iv.setImageDrawable(null);
            Bitmap old = previewBitmaps.remove(techId);
            if (old != null && !old.isRecycled()) old.recycle();
            Bitmap updated = getOrCreateStaticPreview(techId);
            if (updated != null) {
                iv.setImageBitmap(updated);
            }
        }
    };

    private OnTechniqueSelectedListener listener;

    public TechniquesPanel(Context context, PreferencesManager prefs, TechniqueRenderer renderer) {
        super(context);
        this.prefs = prefs;
        this.renderer = renderer;
        registerListener();

        setBackgroundColor(Color.parseColor("#f00d2936"));
        setPadding(dp(24), dp(20), dp(24), dp(20));

        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);

        // Header
        LinearLayout header = new LinearLayout(context);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(context);
        title.setText("Техніки навігації");
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

        TextView note = new TextView(context);
        note.setText("Обери техніку навігації. Вона огортатиме застосунок цільною чакрою. Усі десять прев’ю нижче статичні, без фонового навантаження.");
        note.setTextColor(Color.parseColor("#afc1b7"));
        note.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        note.setPadding(0, dp(10), 0, dp(14));
        content.addView(note);

        // Сітка карток технік (адаптивна)
        grid = new GridLayout(context);
        grid.setColumnCount(computedColumnCount);
        grid.setAlignmentMode(GridLayout.ALIGN_BOUNDS);
        content.addView(grid);

        addView(content);
    }

    private synchronized void registerListener() {
        if (!isListenerRegistered && renderer != null) {
            renderer.addOnBitmapLoadedListener(bitmapListener);
            isListenerRegistered = true;
        }
    }

    private synchronized void unregisterListener() {
        if (isListenerRegistered && renderer != null) {
            renderer.removeOnBitmapLoadedListener(bitmapListener);
            isListenerRegistered = false;
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        registerListener();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        unregisterListener();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        if (w > 0) {
            int availableWidth = w - dp(48);
            AdaptiveGridHelper.LayoutResult res = AdaptiveGridHelper.compute(
                    availableWidth, dp(240), dp(420), dp(8), 1, 4
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

    public void setOnTechniqueSelectedListener(OnTechniqueSelectedListener listener) {
        this.listener = listener;
    }

    public void renderTechniques() {
        registerListener();
        previewViews.clear();
        grid.removeAllViews();
        String currentTechId = prefs.getTechnique();

        int availableWidth = getWidth() > 0 ? getWidth() - dp(48) : getResources().getDisplayMetrics().widthPixels - dp(48);
        AdaptiveGridHelper.LayoutResult res = AdaptiveGridHelper.compute(
                availableWidth, dp(240), dp(420), dp(8), 1, 4
        );
        computedColumnCount = res.columnCount;
        computedCardWidth = res.cardWidthPx;
        grid.setColumnCount(computedColumnCount);

        for (Technique tech : CatalogData.TECHNIQUES) {
            View card = createTechniqueCard(tech, tech.getId().equals(currentTechId));
            grid.addView(card);
        }
    }

    private View createTechniqueCard(Technique tech, boolean isSelected) {
        LinearLayout card = new LinearLayout(getContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setFocusable(true);
        card.setFocusableInTouchMode(true);
        card.setPadding(dp(12), dp(10), dp(12), dp(10));
        card.setBackgroundColor(isSelected ? Color.parseColor("#2e2618") : Color.parseColor("#102b38"));

        GridLayout.LayoutParams glp = new GridLayout.LayoutParams();
        glp.width = computedCardWidth > 0 ? computedCardWidth : dp(320);
        glp.height = GridLayout.LayoutParams.WRAP_CONTENT;
        glp.setMargins(dp(8), dp(8), dp(8), dp(8));
        card.setLayoutParams(glp);

        // Статичне прев'ю техніки на Canvas (одноразовий рендер на Bitmap, жодної фонової анімації!)
        ImageView previewImage = new ImageView(getContext());
        previewViews.put(tech.getId(), previewImage);
        LinearLayout.LayoutParams prevLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(120)
        );
        previewImage.setLayoutParams(prevLp);
        previewImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
        previewImage.setBackgroundColor(Color.parseColor("#081c24"));

        Bitmap staticBmp = getOrCreateStaticPreview(tech.getId());
        if (staticBmp != null) {
            previewImage.setImageBitmap(staticBmp);
        }
        card.addView(previewImage);

        // Назва техніки
        TextView name = new TextView(getContext());
        name.setText(tech.getName());
        name.setTextColor(isSelected ? Color.parseColor("#f2c576") : Color.WHITE);
        name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        name.setTypeface(Typeface.DEFAULT_BOLD);
        name.setPadding(0, dp(8), 0, 0);
        card.addView(name);

        // Володар (owner)
        TextView owner = new TextView(getContext());
        owner.setText(tech.getOwner());
        owner.setTextColor(Color.parseColor("#8aa2ad"));
        owner.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        card.addView(owner);

        // Опис
        TextView desc = new TextView(getContext());
        desc.setText(tech.getDescription());
        desc.setTextColor(Color.parseColor("#bac8bc"));
        desc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        desc.setPadding(0, dp(4), 0, dp(6));
        card.addView(desc);

        // Стан: Обрано / Обрати техніку
        TextView state = new TextView(getContext());
        state.setText(isSelected ? "★ Обрано" : "Обрати техніку");
        state.setTextColor(isSelected ? Color.parseColor("#ed943f") : Color.parseColor("#7aa197"));
        state.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        card.addView(state);

        card.setOnClickListener(v -> {
            prefs.setTechnique(tech.getId());
            if (listener != null) listener.onTechniqueSelected(tech);
        });

        card.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                v.setBackgroundColor(Color.parseColor("#3a3520"));
            } else {
                v.setBackgroundColor(isSelected ? Color.parseColor("#2e2618") : Color.parseColor("#102b38"));
            }
        });

        return card;
    }

    /**
     * Генерує статичний Bitmap з прев'ю техніки рівно один раз і кешує його.
     * Забезпечує вимогу: жодної безперервної анімації всередині меню технік.
     */
    private Bitmap getOrCreateStaticPreview(String id) {
        if (previewBitmaps.containsKey(id)) {
            return previewBitmaps.get(id);
        }

        int w = dp(240);
        int h = dp(120);
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        renderer.drawStaticPreview(canvas, id, w, h);

        previewBitmaps.put(id, bmp);
        return bmp;
    }

    public void release() {
        unregisterListener();
        clearCache();
    }

    public void clearCache() {
        for (ImageView preview : previewViews.values()) preview.setImageDrawable(null);
        previewViews.clear();
        grid.removeAllViews();
        for (Bitmap bmp : previewBitmaps.values()) {
            if (bmp != null && !bmp.isRecycled()) bmp.recycle();
        }
        previewBitmaps.clear();
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics()
        );
    }
}
