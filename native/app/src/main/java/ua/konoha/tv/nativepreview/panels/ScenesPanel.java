package ua.konoha.tv.nativepreview.panels;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
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

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class ScenesPanel extends ScrollView {
    public interface OnSceneSelectedListener {
        void onSceneSelected(Scene scene);
        void onCloseRequested();
    }

    private final PreferencesManager prefs;
    private final GridLayout grid;
    private final Map<String, Bitmap> thumbCache = new HashMap<>();
    private final ExecutorService thumbExecutor = Executors.newFixedThreadPool(2);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private volatile long thumbGeneration = 0;
    private volatile boolean disposed;

    private int computedCardWidth = 0;
    private int computedColumnCount = 3;

    private OnSceneSelectedListener listener;

    public ScenesPanel(Context context, PreferencesManager prefs) {
        super(context);
        this.prefs = prefs;

        setBackgroundColor(Color.parseColor("#f00d2936"));
        setPadding(dp(24), dp(20), dp(24), dp(20));

        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);

        // Header
        LinearLayout header = new LinearLayout(context);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(context);
        title.setText("Шпалери Конохи");
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
        note.setText("Вісімнадцять шпалер: сцени Наруто та героїв. Вітер, листя та чакра рухаються окремо від панорами.");
        note.setTextColor(Color.parseColor("#afc1b7"));
        note.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        note.setPadding(0, dp(10), 0, dp(14));
        content.addView(note);

        // Адаптивна сітка
        grid = new GridLayout(context);
        grid.setColumnCount(computedColumnCount);
        grid.setAlignmentMode(GridLayout.ALIGN_BOUNDS);
        content.addView(grid);

        addView(content);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        if (w > 0) {
            int availableWidth = w - dp(48);
            AdaptiveGridHelper.LayoutResult res = AdaptiveGridHelper.compute(
                    availableWidth, dp(180), dp(320), dp(6), 1, 6
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

    public void setOnSceneSelectedListener(OnSceneSelectedListener listener) {
        this.listener = listener;
    }

    public void renderScenes() {
        if (disposed) return;
        thumbGeneration++;
        ((ThreadPoolExecutor) thumbExecutor).getQueue().clear();
        grid.removeAllViews();
        String currentSceneId = prefs.getScene();

        int availableWidth = getWidth() > 0 ? getWidth() - dp(48) : getResources().getDisplayMetrics().widthPixels - dp(48);
        AdaptiveGridHelper.LayoutResult res = AdaptiveGridHelper.compute(
                availableWidth, dp(180), dp(320), dp(6), 1, 6
        );
        computedColumnCount = res.columnCount;
        computedCardWidth = res.cardWidthPx;
        grid.setColumnCount(computedColumnCount);

        for (Scene scene : CatalogData.SCENES) {
            View card = createSceneCard(scene, scene.getId().equals(currentSceneId));
            grid.addView(card);
        }
    }

    private View createSceneCard(Scene scene, boolean isActive) {
        LinearLayout card = new LinearLayout(getContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setFocusable(true);
        card.setFocusableInTouchMode(true);
        card.setPadding(dp(6), dp(6), dp(6), dp(8));
        card.setBackgroundColor(isActive ? Color.parseColor("#382312") : Color.parseColor("#102b38"));

        GridLayout.LayoutParams glp = new GridLayout.LayoutParams();
        glp.width = computedCardWidth > 0 ? computedCardWidth : dp(200);
        glp.height = GridLayout.LayoutParams.WRAP_CONTENT;
        glp.setMargins(dp(6), dp(6), dp(6), dp(6));
        card.setLayoutParams(glp);

        // Thumbnail (асинхронне фонове завантаження)
        ImageView thumbView = new ImageView(getContext());
        LinearLayout.LayoutParams thumbLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(112)
        );
        thumbView.setLayoutParams(thumbLp);
        thumbView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        loadThumbBitmapAsync(scene.getThumbAsset(), thumbView);
        card.addView(thumbView);

        // Name
        TextView nameView = new TextView(getContext());
        nameView.setText(scene.getName() + (isActive ? " (Активна)" : ""));
        nameView.setTextColor(isActive ? Color.parseColor("#f2c576") : Color.WHITE);
        nameView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        nameView.setTypeface(Typeface.DEFAULT_BOLD);
        nameView.setPadding(0, dp(6), 0, 0);
        card.addView(nameView);

        // Mood
        TextView moodView = new TextView(getContext());
        moodView.setText(scene.getMood());
        moodView.setTextColor(Color.parseColor("#9eb1b8"));
        moodView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        card.addView(moodView);

        card.setOnClickListener(v -> {
            prefs.setScene(scene.getId());
            if (listener != null) listener.onSceneSelected(scene);
        });

        card.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                v.setBackgroundColor(Color.parseColor("#2a5568"));
            } else {
                v.setBackgroundColor(isActive ? Color.parseColor("#382312") : Color.parseColor("#102b38"));
            }
        });

        return card;
    }

    private void loadThumbBitmapAsync(String path, ImageView thumbView) {
        if (disposed) return;
        thumbView.setTag(path);
        Bitmap cached = thumbCache.get(path);
        if (cached != null && !cached.isRecycled()) {
            thumbView.setImageBitmap(cached);
            return;
        }
        thumbView.setBackgroundColor(Color.parseColor("#153340"));
        final long gen = thumbGeneration;
        thumbExecutor.execute(() -> {
            if (disposed || gen != thumbGeneration) return;
            Bitmap bmp = null;
            try (InputStream is = getContext().getAssets().open(path)) {
                bmp = BitmapFactory.decodeStream(is);
            } catch (Exception ignored) {}

            final Bitmap decoded = bmp;
            if (decoded != null) {
                mainHandler.post(() -> {
                    synchronized (ScenesPanel.this) {
                        if (!disposed && gen == thumbGeneration && path.equals(thumbView.getTag())) {
                            Bitmap existing = thumbCache.get(path);
                            if (existing == null || existing.isRecycled()) {
                                thumbCache.put(path, decoded);
                                thumbView.setImageBitmap(decoded);
                            } else {
                                thumbView.setImageBitmap(existing);
                                if (existing != decoded) decoded.recycle();
                            }
                            return;
                        }
                    }
                    if (!decoded.isRecycled()) {
                        decoded.recycle();
                    }
                });
            }
        });
    }

    public synchronized void clearCache() {
        thumbGeneration++;
        ((ThreadPoolExecutor) thumbExecutor).getQueue().clear();
        clearImages(grid);
        grid.removeAllViews();
        for (Bitmap bmp : thumbCache.values()) {
            if (bmp != null && !bmp.isRecycled()) {
                bmp.recycle();
            }
        }
        thumbCache.clear();
    }

    public synchronized void release() {
        disposed = true;
        clearCache();
        thumbExecutor.shutdownNow();
    }

    private void clearImages(View view) {
        if (view instanceof ImageView) ((ImageView) view).setImageDrawable(null);
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) clearImages(group.getChildAt(i));
        }
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics()
        );
    }
}
