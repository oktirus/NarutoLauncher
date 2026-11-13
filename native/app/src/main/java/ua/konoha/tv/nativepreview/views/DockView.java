package ua.konoha.tv.nativepreview.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import ua.konoha.tv.nativepreview.apps.AppManager;
import ua.konoha.tv.nativepreview.model.AppItem;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DockView extends HorizontalScrollView {
    public interface OnDockItemClickListener {
        void onAppClick(AppItem app);
        void onLibraryClick();
    }

    public interface OnDockItemFocusListener {
        void onItemFocused(View view, AppItem app);
        void onLibraryFocused(View view);
        void onDockFocusCleared();
    }

    private final AppManager appManager;
    private final LinearLayout container;
    private final Map<String, Bitmap> iconCache = new HashMap<>();
    private final Map<String, java.util.List<ImageView>> iconRequests = new HashMap<>();
    private final java.util.concurrent.ExecutorService iconExecutor = java.util.concurrent.Executors.newSingleThreadExecutor();
    private final android.os.Handler iconHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private volatile boolean disposed;

    private OnDockItemClickListener clickListener;
    private OnDockItemFocusListener focusListener;
    private int computedCardWidth = 0;

    public DockView(Context context, AppManager appManager) {
        super(context);
        this.appManager = appManager;

        setHorizontalScrollBarEnabled(false);
        setFillViewport(true);

        container = new LinearLayout(context);
        container.setOrientation(LinearLayout.HORIZONTAL);
        container.setGravity(Gravity.CENTER);
        container.setPadding(dp(16), dp(8), dp(16), dp(8));

        addView(container, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
    }

    public void setOnDockItemClickListener(OnDockItemClickListener listener) {
        this.clickListener = listener;
    }

    public void setOnDockItemFocusListener(OnDockItemFocusListener listener) {
        this.focusListener = listener;
    }

    /**
     * Адаптивне обчислення ширини карток дока відповідно до доступної ширини екрана
     * та фактичної кількості елементів (C06).
     */
    public int computeCardWidth(int availableWidthPx, int itemCount) {
        if (itemCount <= 0) return dp(130);
        int paddingPx = dp(32); // відступи контейнера зліва і справа
        int usableWidth = Math.max(0, availableWidthPx - paddingPx);
        int marginTotalPx = dp(12) * itemCount; // по 6dp з кожного боку
        int netWidth = usableWidth - marginTotalPx;
        int perCard = netWidth / itemCount;

        int minWidth = dp(112); // мінімальний читабельний розмір для TV
        int maxWidth = dp(200); // максимальна ширина на широких дисплеях

        if (perCard < minWidth) {
            return minWidth;
        } else if (perCard > maxWidth) {
            return maxWidth;
        } else {
            return perCard;
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w > 0) {
            int count = container.getChildCount();
            int newW = computeCardWidth(w, count);
            if (newW != computedCardWidth) {
                computedCardWidth = newW;
                applyCardWidths(newW);
            }
        }
    }

    private void applyCardWidths(int cardWidth) {
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            ViewGroup.LayoutParams lp = child.getLayoutParams();
            if (lp != null && lp.width != cardWidth) {
                lp.width = cardWidth;
                child.setLayoutParams(lp);
            }
        }
    }

    public void render() {
        container.removeAllViews();
        List<AppItem> dockApps = appManager.getDockApps();
        int totalItems = dockApps.size() + 1; // обрані + архів

        int availableWidth = getWidth() > 0 ? getWidth() : getResources().getDisplayMetrics().widthPixels;
        computedCardWidth = computeCardWidth(availableWidth, totalItems);

        // Картки обраних додатків
        for (AppItem app : dockApps) {
            View card = createAppCard(app, computedCardWidth);
            container.addView(card);
        }

        // Картка «Весь архів»
        View archiveCard = createArchiveCard(computedCardWidth);
        container.addView(archiveCard);
    }

    public View findCardByPackage(String packageName) {
        if (packageName == null) return null;
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            Object tag = child.getTag();
            if (tag instanceof AppItem && packageName.equals(((AppItem) tag).getPackageName())) {
                return child;
            }
        }
        return null;
    }

    public View getArchiveCard() {
        int count = container.getChildCount();
        if (count > 0) {
            View last = container.getChildAt(count - 1);
            if ("archive".equals(last.getTag())) return last;
        }
        return null;
    }

    public View getFirstCard() {
        return container.getChildCount() > 0 ? container.getChildAt(0) : null;
    }

    private View createAppCard(AppItem app, int cardWidth) {
        LinearLayout card = new LinearLayout(getContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setFocusable(true);
        card.setFocusableInTouchMode(true);
        card.setPadding(dp(8), dp(10), dp(8), dp(10));
        card.setTag(app);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(cardWidth, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(6), 0, dp(6), 0);
        card.setLayoutParams(lp);

        // Іконка
        ImageView iconView = new ImageView(getContext());
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dp(84), dp(84));
        iconView.setLayoutParams(iconLp);
        iconView.setScaleType(ImageView.ScaleType.FIT_CENTER);

        bindAppIcon(iconView, app);
        card.addView(iconView);

        // Назва
        TextView nameView = new TextView(getContext());
        nameView.setText(app.getName());
        nameView.setTextColor(Color.WHITE);
        nameView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        nameView.setTypeface(Typeface.DEFAULT_BOLD);
        nameView.setSingleLine(true);
        nameView.setEllipsize(TextUtils.TruncateAt.END);
        nameView.setGravity(Gravity.CENTER);
        nameView.setPadding(0, dp(6), 0, 0);
        card.addView(nameView);

        // Підпис / опис
        TextView descView = new TextView(getContext());
        descView.setText(app.getDescription());
        descView.setTextColor(Color.parseColor("#9eb1b8"));
        descView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        descView.setSingleLine(true);
        descView.setEllipsize(TextUtils.TruncateAt.END);
        descView.setGravity(Gravity.CENTER);
        card.addView(descView);

        card.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onAppClick(app);
        });

        card.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                if (focusListener != null) focusListener.onItemFocused(v, app);
            } else {
                if (focusListener != null) focusListener.onDockFocusCleared();
            }
        });

        return card;
    }

    private View createArchiveCard(int cardWidth) {
        LinearLayout card = new LinearLayout(getContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setFocusable(true);
        card.setFocusableInTouchMode(true);
        card.setPadding(dp(8), dp(10), dp(8), dp(10));
        card.setTag("archive");

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(cardWidth, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(6), 0, dp(6), 0);
        card.setLayoutParams(lp);

        ImageView iconView = new ImageView(getContext());
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dp(84), dp(84));
        iconView.setLayoutParams(iconLp);
        iconView.setScaleType(ImageView.ScaleType.FIT_CENTER);

        bindAssetIcon("icons/icon-archive.webp", iconView);
        card.addView(iconView);

        TextView nameView = new TextView(getContext());
        nameView.setText("Весь архів");
        nameView.setTextColor(Color.parseColor("#f2c576"));
        nameView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        nameView.setTypeface(Typeface.DEFAULT_BOLD);
        nameView.setSingleLine(true);
        nameView.setGravity(Gravity.CENTER);
        nameView.setPadding(0, dp(6), 0, 0);
        card.addView(nameView);

        TextView descView = new TextView(getContext());
        descView.setText("Усі застосунки");
        descView.setTextColor(Color.parseColor("#9eb1b8"));
        descView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        descView.setSingleLine(true);
        descView.setGravity(Gravity.CENTER);
        card.addView(descView);

        card.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onLibraryClick();
        });

        card.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                if (focusListener != null) focusListener.onLibraryFocused(v);
            } else {
                if (focusListener != null) focusListener.onDockFocusCleared();
            }
        });

        return card;
    }

    public Drawable getAppIconDrawable(AppItem app) {
        if (app.getKind() != null && !app.getKind().isEmpty()) {
            Bitmap bmp = loadAssetBitmap("icons/icon-" + app.getKind() + ".webp");
            if (bmp != null) {
                return new BitmapDrawable(getResources(), bmp);
            }
        }
        return app.getSystemIcon();
    }

    private Bitmap loadAssetBitmap(String path) {
        return iconCache.get(path);
    }

    public void bindAppIcon(ImageView image, AppItem app) {
        image.setImageDrawable(app.getSystemIcon());
        image.setBackgroundColor(Color.TRANSPARENT);
        if (app.getKind() != null && !app.getKind().isEmpty()) {
            bindAssetIcon("icons/icon-" + app.getKind() + ".webp", image);
        }
    }

    private void bindAssetIcon(String path, ImageView image) {
        image.setTag(path);
        Bitmap cached = iconCache.get(path);
        if (cached != null && !cached.isRecycled()) {
            image.setImageBitmap(cached);
            return;
        }
        if (disposed) return;
        java.util.List<ImageView> waiting = iconRequests.get(path);
        if (waiting != null) {
            waiting.add(image);
            return;
        }
        waiting = new java.util.ArrayList<>();
        waiting.add(image);
        iconRequests.put(path, waiting);
        iconExecutor.execute(() -> {
            if (disposed) return;
            Bitmap decoded;
            try (InputStream is = getContext().getAssets().open(path)) {
                decoded = BitmapFactory.decodeStream(is);
            } catch (Exception ignored) { decoded = null; }
            final Bitmap loaded = decoded;
            iconHandler.post(() -> {
                java.util.List<ImageView> consumers = iconRequests.remove(path);
                if (disposed) {
                    if (loaded != null) loaded.recycle();
                    return;
                }
                if (loaded == null) return;
                iconCache.put(path, loaded);
                if (consumers != null) for (ImageView consumer : consumers) {
                    if (path.equals(consumer.getTag())) consumer.setImageBitmap(loaded);
                }
            });
        });
    }

    public void release() {
        disposed = true;
        iconRequests.clear();
        iconExecutor.shutdownNow();
        clearImages(container);
        container.removeAllViews();
        for (Bitmap bmp : iconCache.values()) {
            if (bmp != null && !bmp.isRecycled()) {
                bmp.recycle();
            }
        }
        iconCache.clear();
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
