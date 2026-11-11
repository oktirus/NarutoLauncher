package ua.konoha.tv.nativepreview.panels;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import ua.konoha.tv.nativepreview.model.CatalogData;
import ua.konoha.tv.nativepreview.model.Scene;
import ua.konoha.tv.nativepreview.prefs.PreferencesManager;
import ua.konoha.tv.nativepreview.theme.KonohaTheme;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Scenes panel (.scene-grid) reproducing Chromium's 3-column wallpaper picker.
 * Full 16:9 aspect thumbnails with dark gradient overlay and Caveat title.
 */
public class ScenesPanel extends FrameLayout {
    public interface OnSceneSelectedListener {
        void onSceneSelected(Scene scene);
        void onCloseRequested();
    }

    private final PreferencesManager prefs;
    private final SideSheetContainer sheetContainer;
    private final GridLayout grid;
    private final ScrollView scrollView;
    private final Map<String, Bitmap> thumbCache = new ConcurrentHashMap<>();
    private final ExecutorService thumbExecutor = Executors.newFixedThreadPool(2);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private volatile boolean disposed;

    private int computedColumnCount = 3;
    private int computedCardWidth = 0;
    private OnSceneSelectedListener listener;

    public ScenesPanel(Context context, PreferencesManager prefs) {
        super(context);
        this.prefs = prefs;

        sheetContainer = new SideSheetContainer(context, true, "Архів Конохи", "Шпалери");
        sheetContainer.setOnCloseListener(() -> {
            if (listener != null) listener.onCloseRequested();
        });

        scrollView = new ScrollView(context);
        scrollView.setVerticalScrollBarEnabled(false);

        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);

        grid = new GridLayout(context);
        grid.setColumnCount(computedColumnCount);
        grid.setAlignmentMode(GridLayout.ALIGN_BOUNDS);
        content.addView(grid, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        scrollView.addView(content, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        sheetContainer.setBodyView(scrollView);
        addView(sheetContainer, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        if (w > 0) {
            int sheetW = Math.min(KonohaTheme.dp(getContext(), 1050), (int) (w * 0.95f));
            int pad = KonohaTheme.dp(getContext(), 72);
            int availableWidth = Math.max(100, sheetW - pad);
            int margin = KonohaTheme.dp(getContext(), 6);
            computedColumnCount = 3;
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
        int cardHeight = (int) (computedCardWidth * (9f / 16f));
        for (int i = 0; i < grid.getChildCount(); i++) {
            View child = grid.getChildAt(i);
            GridLayout.LayoutParams lp = (GridLayout.LayoutParams) child.getLayoutParams();
            if (lp != null && (lp.width != computedCardWidth || lp.height != cardHeight)) {
                lp.width = computedCardWidth;
                lp.height = cardHeight;
                child.setLayoutParams(lp);
            }
        }
    }

    public void setOnSceneSelectedListener(OnSceneSelectedListener listener) {
        this.listener = listener;
    }

    public void renderScenes() {
        grid.removeAllViews();
        String currentSceneId = prefs.getScene();

        int availableWidth = getWidth() > 0 ? getWidth() : getResources().getDisplayMetrics().widthPixels;
        int sheetWidth = Math.min(KonohaTheme.dp(getContext(), 1050), (int) (availableWidth * 0.95f));
        int netWidth = Math.max(100, sheetWidth - KonohaTheme.dp(getContext(), 72));
        int margin = KonohaTheme.dp(getContext(), 6);
        computedColumnCount = 3;
        computedCardWidth = (netWidth - margin * 2 * computedColumnCount) / computedColumnCount;
        grid.setColumnCount(computedColumnCount);

        View activeCard = null;
        View[] cards = new View[CatalogData.SCENES.size()];
        for (int i = 0; i < CatalogData.SCENES.size(); i++) {
            Scene scene = CatalogData.SCENES.get(i);
            boolean isSel = scene.getId().equals(currentSceneId);
            View card = createSceneCard(scene, isSel);
            card.setId(View.generateViewId());
            cards[i] = card;
            grid.addView(card);
            if (isSel && activeCard == null) activeCard = card;
        }

        // Configure deterministic D-Pad spatial graph for the 3-column grid
        for (int i = 0; i < cards.length; i++) {
            int col = i % computedColumnCount;
            if (col < computedColumnCount - 1 && i + 1 < cards.length) {
                cards[i].setNextFocusRightId(cards[i + 1].getId());
            }
            if (col > 0 && i - 1 >= 0) {
                cards[i].setNextFocusLeftId(cards[i - 1].getId());
            }
            if (i + computedColumnCount < cards.length) {
                cards[i].setNextFocusDownId(cards[i + computedColumnCount].getId());
            }
            if (i - computedColumnCount >= 0) {
                cards[i].setNextFocusUpId(cards[i - computedColumnCount].getId());
            }
        }

        if (activeCard != null) {
            final View fCard = activeCard;
            post(() -> fCard.requestFocus());
        }
    }

    private View createSceneCard(Scene scene, boolean isActive) {
        FrameLayout card = new FrameLayout(getContext());
        card.setFocusable(true);
        card.setFocusableInTouchMode(true);
        card.setClickable(true);

        GridLayout.LayoutParams glp = new GridLayout.LayoutParams();
        glp.width = computedCardWidth > 0 ? computedCardWidth : KonohaTheme.dp(getContext(), 260);
        glp.height = (int) (glp.width * (9f / 16f));
        int m = KonohaTheme.dp(getContext(), 6);
        glp.setMargins(m, m, m, m);
        card.setLayoutParams(glp);

        // 1. Background Image (Center crop)
        ImageView thumbView = new ImageView(getContext());
        thumbView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        loadThumbBitmapAsync(scene.getThumbAsset(), thumbView);
        card.addView(thumbView, new FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT
        ));

        // 2. Bottom Gradient Scrim Overlay (linear-gradient(0deg, #081e2beb 0%, transparent 90%))
        View scrimView = new View(getContext());
        GradientDrawable scrimGrad = new GradientDrawable(
                GradientDrawable.Orientation.BOTTOM_TOP,
                new int[]{Color.parseColor("#f0081e2b"), Color.TRANSPARENT}
        );
        scrimView.setBackground(scrimGrad);
        card.addView(scrimView, new FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT
        ));

        // 3. Text Overlay: Caveat Title + Nunito Mood
        LinearLayout textBox = new LinearLayout(getContext());
        textBox.setOrientation(LinearLayout.VERTICAL);
        textBox.setPadding(KonohaTheme.dp(getContext(), 16), 0, KonohaTheme.dp(getContext(), 12), KonohaTheme.dp(getContext(), 12));
        FrameLayout.LayoutParams tbLp = new FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.BOTTOM
        );
        textBox.setLayoutParams(tbLp);

        TextView titleView = new TextView(getContext());
        titleView.setText(scene.getName());
        titleView.setTextColor(KonohaTheme.COLOR_PAPER);
        titleView.setTextSize(26);
        titleView.setTypeface(KonohaTheme.getCaveatBold(getContext()));
        titleView.setShadowLayer(8f, 0, 2f, Color.parseColor("#99000000"));
        textBox.addView(titleView);

        TextView moodView = new TextView(getContext());
        moodView.setText(scene.getMood());
        moodView.setTextColor(Color.parseColor("#d7d3b6"));
        moodView.setTextSize(11);
        moodView.setTypeface(KonohaTheme.getNunito(getContext()));
        textBox.addView(moodView);

        card.addView(textBox);

        // Border and focus state
        final GradientDrawable normalBorder = new GradientDrawable();
        normalBorder.setColor(Color.TRANSPARENT);
        normalBorder.setCornerRadius(KonohaTheme.dp(getContext(), 4));
        normalBorder.setStroke(KonohaTheme.dp(getContext(), 1), isActive ? KonohaTheme.COLOR_ORANGE : Color.parseColor("#44dbbd7d"));
        card.setForeground(normalBorder);

        card.setOnClickListener(v -> {
            prefs.setScene(scene.getId());
            if (listener != null) listener.onSceneSelected(scene);
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
            v.setScaleX(hasFocus ? 1.04f : 1.0f);
            v.setScaleY(hasFocus ? 1.04f : 1.0f);
            GradientDrawable d = new GradientDrawable();
            d.setColor(Color.TRANSPARENT);
            d.setCornerRadius(KonohaTheme.dp(getContext(), 4));
            d.setStroke(KonohaTheme.dp(getContext(), hasFocus ? 2 : 1),
                    hasFocus ? KonohaTheme.COLOR_GOLD : (isActive ? KonohaTheme.COLOR_ORANGE : Color.parseColor("#44dbbd7d")));
            v.setForeground(d);
        });

        return card;
    }

    private void loadThumbBitmapAsync(String assetPath, ImageView target) {
        if (assetPath == null || disposed) return;
        Bitmap cached = thumbCache.get(assetPath);
        if (cached != null) {
            target.setImageBitmap(cached);
            return;
        }

        thumbExecutor.execute(() -> {
            if (disposed) return;
            try (InputStream is = getContext().getAssets().open(assetPath)) {
                Bitmap bmp = BitmapFactory.decodeStream(is);
                if (bmp != null) {
                    if (disposed) {
                        bmp.recycle();
                        return;
                    }
                    thumbCache.put(assetPath, bmp);
                    mainHandler.post(() -> {
                        if (!disposed) target.setImageBitmap(bmp);
                    });
                }
            } catch (Exception ignored) {}
        });
    }

    public void clearCache() {
        thumbCache.clear();
    }

    public void release() {
        disposed = true;
        thumbExecutor.shutdownNow();
        java.util.List<Bitmap> toRecycle = new java.util.ArrayList<>(thumbCache.values());
        thumbCache.clear();
        for (Bitmap bmp : toRecycle) {
            if (bmp != null && !bmp.isRecycled()) bmp.recycle();
        }
    }
}
