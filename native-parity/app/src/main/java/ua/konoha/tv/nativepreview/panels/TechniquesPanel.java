package ua.konoha.tv.nativepreview.panels;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import ua.konoha.tv.nativepreview.model.CatalogData;
import ua.konoha.tv.nativepreview.model.Technique;
import ua.konoha.tv.nativepreview.prefs.PreferencesManager;
import ua.konoha.tv.nativepreview.theme.KonohaTheme;
import ua.konoha.tv.nativepreview.views.TechniqueRenderer;

import java.util.HashMap;
import java.util.Map;

/**
 * Techniques panel (.technique-grid) reproducing Chromium's picker:
 * Wide side-sheet with 320x210 static preview thumbnails, character colors,
 * sample app icons inside preview, and exact typography.
 */
public class TechniquesPanel extends FrameLayout {
    public interface OnTechniqueSelectedListener {
        void onTechniqueSelected(Technique technique);
        void onCloseRequested();
    }

    private final PreferencesManager prefs;
    private final TechniqueRenderer renderer;
    private final SideSheetContainer sheetContainer;
    private final ScrollView scrollView;
    private final GridLayout grid;
    private final Map<String, Bitmap> previewBitmaps = new HashMap<>();
    private final Map<String, ImageView> previewViews = new HashMap<>();

    private int computedColumnCount = 3;
    private int computedCardWidth = 0;
    private boolean isListenerRegistered = false;
    private OnTechniqueSelectedListener listener;

    private final TechniqueRenderer.OnBitmapLoadedListener bitmapListener = name -> {
        String techId = "gamabunta-idle".equals(name) ? "gamabunta" : name;
        ImageView iv = previewViews.get(techId);
        if (iv != null) {
            Bitmap old = previewBitmaps.remove(techId);
            if (old != null && !old.isRecycled()) old.recycle();
            Bitmap updated = getOrCreateStaticPreview(techId, iv.getWidth(), iv.getHeight());
            if (updated != null) {
                iv.setImageBitmap(updated);
            }
        }
    };

    public TechniquesPanel(Context context, PreferencesManager prefs, TechniqueRenderer renderer) {
        super(context);
        this.prefs = prefs;
        this.renderer = renderer;
        registerListener();

        sheetContainer = new SideSheetContainer(context, true, "Архів Конохи", "Техніки навігації");
        sheetContainer.setOnCloseListener(() -> {
            if (listener != null) listener.onCloseRequested();
        });

        scrollView = new ScrollView(context);
        scrollView.setVerticalScrollBarEnabled(false);

        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);

        // Intro
        TextView note = new TextView(context);
        note.setText("Обери техніку навігації. Вона огортатиме застосунок цільною чакрою. Усі десять прев’ю нижче статичні, без фонового навантаження.");
        note.setTextColor(Color.parseColor("#afc1b7"));
        note.setTextSize(12);
        note.setTypeface(KonohaTheme.getNunito(context));
        note.setPadding(0, 0, 0, KonohaTheme.dp(context, 16));
        content.addView(note);

        // Grid
        grid = new GridLayout(context);
        grid.setColumnCount(computedColumnCount);
        grid.setAlignmentMode(GridLayout.ALIGN_BOUNDS);
        content.addView(grid, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        // Footer note
        TextView footerNote = new TextView(context);
        footerNote.setText("↑↓←→ — порівняти; OK — застосувати. Вибір зберігається. За зменшення анімацій ефекти стають нерухомими. Хірайшин — техніка Мінато, Четвертого хокаге. Расенсюрикен — додатковий варіант зі стихією вітру.");
        footerNote.setTextColor(Color.parseColor("#8aa2ad"));
        footerNote.setTextSize(11);
        footerNote.setTypeface(KonohaTheme.getNunito(context));
        footerNote.setPadding(0, KonohaTheme.dp(context, 20), 0, KonohaTheme.dp(context, 24));
        content.addView(footerNote);

        scrollView.addView(content, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        sheetContainer.setBodyView(scrollView);
        addView(sheetContainer, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
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
            int sheetW = Math.min(KonohaTheme.dp(getContext(), 1050), (int) (w * 0.95f));
            int pad = KonohaTheme.dp(getContext(), 72);
            int availableWidth = Math.max(100, sheetW - pad);
            int margin = KonohaTheme.dp(getContext(), 6);
            AdaptiveGridHelper.LayoutResult res = AdaptiveGridHelper.compute(
                    availableWidth, KonohaTheme.dp(getContext(), 180), KonohaTheme.dp(getContext(), 320), margin, 1, 4
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
            GridLayout.LayoutParams lp = (GridLayout.LayoutParams) child.getLayoutParams();
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
        previewViews.clear();
        grid.removeAllViews();
        String currentTechId = prefs.getTechnique();

        int availableWidth = getWidth() > 0 ? getWidth() : getResources().getDisplayMetrics().widthPixels;
        int sheetWidth = Math.min(KonohaTheme.dp(getContext(), 1050), (int) (availableWidth * 0.95f));
        int netWidth = Math.max(100, sheetWidth - KonohaTheme.dp(getContext(), 72));
        int margin = KonohaTheme.dp(getContext(), 6);
        AdaptiveGridHelper.LayoutResult res = AdaptiveGridHelper.compute(
                netWidth, KonohaTheme.dp(getContext(), 180), KonohaTheme.dp(getContext(), 320), margin, 1, 4
        );
        computedColumnCount = res.columnCount;
        computedCardWidth = res.cardWidthPx;
        grid.setColumnCount(computedColumnCount);

        View selectedCard = null;
        for (Technique tech : CatalogData.TECHNIQUES) {
            boolean isSel = tech.getId().equals(currentTechId);
            View card = createTechniqueCard(tech, isSel);
            grid.addView(card);
            if (isSel) selectedCard = card;
        }
        if (selectedCard != null) {
            final View fCard = selectedCard;
            post(() -> fCard.requestFocus());
        }
    }

    private View createTechniqueCard(Technique tech, boolean isSelected) {
        LinearLayout card = new LinearLayout(getContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setFocusable(true);
        card.setClickable(true);
        card.setPadding(KonohaTheme.dp(getContext(), 8), KonohaTheme.dp(getContext(), 8),
                KonohaTheme.dp(getContext(), 8), KonohaTheme.dp(getContext(), 8));

        GridLayout.LayoutParams glp = new GridLayout.LayoutParams();
        glp.width = computedCardWidth > 0 ? computedCardWidth : KonohaTheme.dp(getContext(), 260);
        glp.height = GridLayout.LayoutParams.WRAP_CONTENT;
        int m = KonohaTheme.dp(getContext(), 4);
        glp.setMargins(m, m, m, m);
        card.setLayoutParams(glp);

        int cw = glp.width;
        // 1. Static Preview Canvas (with sample icon inside)
        ImageView previewImage = new ImageView(getContext());
        previewViews.put(tech.getId(), previewImage);
        int prevHeight = (int) (cw * (110f / 320f));
        LinearLayout.LayoutParams prevLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, prevHeight
        );
        previewImage.setLayoutParams(prevLp);
        previewImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
        previewImage.setBackgroundColor(Color.parseColor("#081c24"));

        Bitmap staticBmp = getOrCreateStaticPreview(tech.getId(), cw, prevHeight);
        if (staticBmp != null) {
            previewImage.setImageBitmap(staticBmp);
        }
        card.addView(previewImage);

        // 2. Technique Name (Colored with its character color)
        TextView name = new TextView(getContext());
        name.setText(tech.getName());
        name.setTextColor(getTechniqueColor(tech.getId()));
        name.setTextSize(15);
        name.setTypeface(KonohaTheme.getNunitoBold(getContext()));
        name.setPadding(0, KonohaTheme.dp(getContext(), 6), 0, 0);
        card.addView(name);

        // 3. Owner
        TextView owner = new TextView(getContext());
        owner.setText(tech.getOwner());
        owner.setTextColor(Color.parseColor("#dfcfaf"));
        owner.setTextSize(10);
        owner.setTypeface(KonohaTheme.getNunito(getContext()));
        card.addView(owner);

        // 4. Description
        TextView desc = new TextView(getContext());
        desc.setText(tech.getDescription());
        desc.setTextColor(Color.parseColor("#b8c8bf"));
        desc.setTextSize(11);
        desc.setTypeface(KonohaTheme.getNunito(getContext()));
        desc.setMaxLines(2);
        desc.setPadding(0, KonohaTheme.dp(getContext(), 2), 0, KonohaTheme.dp(getContext(), 4));
        card.addView(desc);

        // 5. State caption
        TextView state = new TextView(getContext());
        state.setText(isSelected ? "Обрано" : "Обрати техніку");
        state.setTextColor(isSelected ? KonohaTheme.COLOR_ORANGE : Color.parseColor("#7aa197"));
        state.setTextSize(10);
        state.setTypeface(KonohaTheme.getNunito(getContext()));
        card.addView(state);

        // Card background & borders
        final GradientDrawable normalBg = new GradientDrawable();
        normalBg.setColor(isSelected ? KonohaTheme.COLOR_CARD_ACTIVE : KonohaTheme.COLOR_CARD_BG);
        normalBg.setCornerRadius(KonohaTheme.dp(getContext(), 4));
        normalBg.setStroke(KonohaTheme.dp(getContext(), isSelected ? 2 : 1),
                isSelected ? KonohaTheme.COLOR_GOLD : Color.parseColor("#33dbbd7d"));
        card.setBackground(normalBg);

        card.setOnClickListener(v -> {
            prefs.setTechnique(tech.getId());
            if (listener != null) listener.onTechniqueSelected(tech);
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
            v.setScaleX(hasFocus ? 1.03f : 1.0f);
            v.setScaleY(hasFocus ? 1.03f : 1.0f);
            GradientDrawable d = new GradientDrawable();
            d.setColor(hasFocus ? Color.parseColor("#2a424e") : (isSelected ? KonohaTheme.COLOR_CARD_ACTIVE : KonohaTheme.COLOR_CARD_BG));
            d.setCornerRadius(KonohaTheme.dp(getContext(), 4));
            d.setStroke(KonohaTheme.dp(getContext(), 2), hasFocus ? KonohaTheme.COLOR_GOLD : (isSelected ? KonohaTheme.COLOR_GOLD : Color.parseColor("#33dbbd7d")));
            v.setBackground(d);
        });

        return card;
    }

    private int getTechniqueColor(String id) {
        switch (id) {
            case "rasengan": return Color.parseColor("#64d2ff");
            case "chidori": return Color.parseColor("#70b8ff");
            case "sharingan": return Color.parseColor("#ff5252");
            case "kurama": return Color.parseColor("#ff6f43");
            case "summoning": return Color.parseColor("#e0d4b8");
            case "hiraishin": return Color.parseColor("#ffd700");
            case "fireball": return Color.parseColor("#ff8a3d");
            case "amaterasu": return Color.parseColor("#b388ff");
            case "rasenshuriken": return Color.parseColor("#4dd0e1");
            case "gamabunta":
            default: return KonohaTheme.COLOR_ORANGE;
        }
    }

    private Bitmap getOrCreateStaticPreview(String id, int w, int h) {
        if (previewBitmaps.containsKey(id)) {
            return previewBitmaps.get(id);
        }
        int pw = Math.max(320, w);
        int ph = Math.max(210, h);
        Bitmap bmp = Bitmap.createBitmap(pw, ph, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        if (renderer != null) {
            renderer.drawStaticPreview(canvas, id, pw, ph);
        }
        previewBitmaps.put(id, bmp);
        return bmp;
    }

    public void clearCache() {
        previewBitmaps.clear();
        previewViews.clear();
    }

    public void release() {
        unregisterListener();
        for (Bitmap bmp : previewBitmaps.values()) {
            if (bmp != null && !bmp.isRecycled()) bmp.recycle();
        }
        previewBitmaps.clear();
        previewViews.clear();
    }
}
