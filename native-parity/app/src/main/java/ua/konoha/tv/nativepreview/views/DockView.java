package ua.konoha.tv.nativepreview.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
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
import ua.konoha.tv.nativepreview.theme.KonohaTheme;

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
        container.setPadding(0, 0, 0, 0);

        addView(container, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
    }

    public void setOnDockItemClickListener(OnDockItemClickListener listener) {
        this.clickListener = listener;
    }

    public void setOnDockItemFocusListener(OnDockItemFocusListener listener) {
        this.focusListener = listener;
    }

    public int computeCardWidth(int availableWidthPx, int itemCount) {
        if (itemCount <= 0) return KonohaTheme.dp(getContext(), 130);
        int marginTotalPx = KonohaTheme.dp(getContext(), 12) * itemCount;
        int netWidth = Math.max(0, availableWidthPx - marginTotalPx);
        int perCard = netWidth / itemCount;

        int minWidth = KonohaTheme.dp(getContext(), 112);
        int maxWidth = KonohaTheme.dp(getContext(), 210);

        return Math.min(maxWidth, Math.max(minWidth, perCard));
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
        View focusedChild = container.getFocusedChild();
        Object tag = focusedChild != null ? focusedChild.getTag() : null;
        String focusedPackage = tag instanceof AppItem ? ((AppItem) tag).getPackageName() : null;
        boolean focusedArchive = "archive".equals(tag);

        container.removeAllViews();
        List<AppItem> dockApps = appManager.getDockApps();
        int totalItems = dockApps.size() + 1; // apps + archive

        int availableWidth = getWidth() > 0 ? getWidth() : getResources().getDisplayMetrics().widthPixels;
        computedCardWidth = computeCardWidth(availableWidth, totalItems);

        View toFocus = null;
        // App cards
        for (AppItem app : dockApps) {
            View card = createAppCard(app, computedCardWidth);
            container.addView(card);
            if (focusedPackage != null && focusedPackage.equals(app.getPackageName())) {
                toFocus = card;
            }
        }

        // Archive card
        View archiveCard = createArchiveCard(computedCardWidth);
        container.addView(archiveCard);
        if (focusedArchive) {
            toFocus = archiveCard;
        }

        if (toFocus != null) {
            toFocus.requestFocus();
        } else if (focusedChild != null && container.getChildCount() > 0) {
            container.getChildAt(0).requestFocus();
        }
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
        card.setPadding(KonohaTheme.dp(getContext(), 8), KonohaTheme.dp(getContext(), 6), KonohaTheme.dp(getContext(), 8), KonohaTheme.dp(getContext(), 6));
        card.setTag(app);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(cardWidth, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(KonohaTheme.dp(getContext(), 6), 0, KonohaTheme.dp(getContext(), 6), 0);
        card.setLayoutParams(lp);

        // Emblem (.app-emblem, contain centered)
        ImageView iconView = new ImageView(getContext());
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(KonohaTheme.dp(getContext(), 118), KonohaTheme.dp(getContext(), 118));
        iconView.setLayoutParams(iconLp);
        iconView.setScaleType(ImageView.ScaleType.FIT_CENTER);

        bindAppIcon(iconView, app);
        card.addView(iconView);

        // App Name (.app-name: Nunito Bold, color #fff3d0)
        TextView nameView = new TextView(getContext());
        nameView.setText(app.getName());
        nameView.setTextColor(Color.parseColor("#fff3d0"));
        nameView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        nameView.setTypeface(KonohaTheme.getNunitoBold(getContext()));
        nameView.setSingleLine(true);
        nameView.setEllipsize(TextUtils.TruncateAt.END);
        nameView.setGravity(Gravity.CENTER);
        nameView.setPadding(0, KonohaTheme.dp(getContext(), 6), 0, 0);
        card.addView(nameView);

        TextView metaView = new TextView(getContext());
        String metaText = app.getDescription();
        metaView.setText(metaText != null && !metaText.isEmpty() ? metaText : "Застосунок");
        metaView.setTextColor(Color.parseColor("#a8b8b0"));
        metaView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        metaView.setTypeface(KonohaTheme.getNunito(getContext()));
        metaView.setSingleLine(true);
        metaView.setEllipsize(TextUtils.TruncateAt.END);
        metaView.setGravity(Gravity.CENTER);
        metaView.setPadding(0, KonohaTheme.dp(getContext(), 2), 0, 0);
        card.addView(metaView);

        card.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onAppClick(app);
        });

        card.setOnFocusChangeListener((v, hasFocus) -> {
            iconView.animate()
                    .scaleX(hasFocus ? 1.045f : 1.0f)
                    .scaleY(hasFocus ? 1.045f : 1.0f)
                    .translationY(hasFocus ? -KonohaTheme.dp(getContext(), 5) : 0f)
                    .setDuration(180)
                    .start();

            nameView.setBackground(null);
            nameView.setTextColor(Color.parseColor("#fff3d0"));
            nameView.setPadding(0, KonohaTheme.dp(getContext(), 6), 0, 0);
            metaView.setBackground(null);
            metaView.setTextColor(hasFocus ? Color.parseColor("#c8cbb6") : Color.parseColor("#a8b8b0"));

            if (focusListener != null) {
                if (hasFocus) focusListener.onItemFocused(v, app);
                else focusListener.onDockFocusCleared();
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
        card.setPadding(KonohaTheme.dp(getContext(), 8), KonohaTheme.dp(getContext(), 6), KonohaTheme.dp(getContext(), 8), KonohaTheme.dp(getContext(), 6));
        card.setTag("archive");

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(cardWidth, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(KonohaTheme.dp(getContext(), 6), 0, KonohaTheme.dp(getContext(), 6), 0);
        card.setLayoutParams(lp);

        ImageView iconView = new ImageView(getContext());
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(KonohaTheme.dp(getContext(), 118), KonohaTheme.dp(getContext(), 118));
        iconView.setLayoutParams(iconLp);
        iconView.setScaleType(ImageView.ScaleType.FIT_CENTER);

        loadArchiveIcon(iconView);
        card.addView(iconView);

        TextView nameView = new TextView(getContext());
        nameView.setText("Весь архів");
        nameView.setTextColor(Color.parseColor("#fff3d0"));
        nameView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        nameView.setTypeface(KonohaTheme.getNunitoBold(getContext()));
        nameView.setSingleLine(true);
        nameView.setGravity(Gravity.CENTER);
        nameView.setPadding(0, KonohaTheme.dp(getContext(), 6), 0, 0);
        card.addView(nameView);

        TextView metaView = new TextView(getContext());
        metaView.setText("Усі застосунки");
        metaView.setTextColor(Color.parseColor("#a8b8b0"));
        metaView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        metaView.setTypeface(KonohaTheme.getNunito(getContext()));
        metaView.setSingleLine(true);
        metaView.setGravity(Gravity.CENTER);
        metaView.setPadding(0, KonohaTheme.dp(getContext(), 2), 0, 0);
        card.addView(metaView);

        card.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onLibraryClick();
        });

        card.setOnFocusChangeListener((v, hasFocus) -> {
            iconView.animate()
                    .scaleX(hasFocus ? 1.045f : 1.0f)
                    .scaleY(hasFocus ? 1.045f : 1.0f)
                    .translationY(hasFocus ? -KonohaTheme.dp(getContext(), 5) : 0f)
                    .setDuration(180)
                    .start();

            nameView.setBackground(null);
            nameView.setTextColor(Color.parseColor("#fff3d0"));
            nameView.setPadding(0, KonohaTheme.dp(getContext(), 6), 0, 0);
            metaView.setBackground(null);
            metaView.setTextColor(hasFocus ? Color.parseColor("#c8cbb6") : Color.parseColor("#a8b8b0"));

            if (focusListener != null) {
                if (hasFocus) focusListener.onLibraryFocused(v);
                else focusListener.onDockFocusCleared();
            }
        });

        return card;
    }

    private void loadArchiveIcon(ImageView iconView) {
        Bitmap cached = iconCache.get("app-archive");
        if (cached != null) {
            iconView.setImageBitmap(cached);
            return;
        }

        iconExecutor.execute(() -> {
            try (InputStream is = getContext().getAssets().open("icons/icon-archive.webp")) {
                Bitmap bmp = BitmapFactory.decodeStream(is);
                if (bmp != null) {
                    synchronized (iconCache) {
                        iconCache.put("app-archive", bmp);
                    }
                    iconHandler.post(() -> {
                        if (!disposed) iconView.setImageBitmap(bmp);
                    });
                }
            } catch (Exception ignored) {}
        });
    }

    private void bindAppIcon(ImageView iconView, AppItem app) {
        String assetPath = app.getAssetPath();
        if (assetPath != null) {
            Bitmap cached = iconCache.get(assetPath);
            if (cached != null) {
                iconView.setImageBitmap(cached);
                return;
            }

            synchronized (iconRequests) {
                List<ImageView> list = iconRequests.get(assetPath);
                if (list != null) {
                    list.add(iconView);
                    return;
                }
                list = new java.util.ArrayList<>();
                list.add(iconView);
                iconRequests.put(assetPath, list);
            }

            iconExecutor.execute(() -> {
                Bitmap bmp = null;
                try (InputStream is = getContext().getAssets().open(assetPath)) {
                    bmp = BitmapFactory.decodeStream(is);
                } catch (Exception ignored) {}

                final Bitmap finalBmp = bmp;
                iconHandler.post(() -> {
                    if (disposed) return;
                    List<ImageView> targets;
                    synchronized (iconRequests) {
                        targets = iconRequests.remove(assetPath);
                        if (finalBmp != null) iconCache.put(assetPath, finalBmp);
                    }
                    if (targets != null && finalBmp != null) {
                        for (ImageView target : targets) target.setImageBitmap(finalBmp);
                    } else if (targets != null) {
                        Drawable sys = app.getSystemIcon();
                        if (sys != null) {
                            for (ImageView target : targets) target.setImageDrawable(sys);
                        }
                    }
                });
            });
        } else {
            Drawable sysIcon = app.getSystemIcon();
            if (sysIcon != null) {
                iconView.setImageDrawable(sysIcon);
            } else {
                iconView.setImageBitmap(createMonogramBitmap(app.getName()));
            }
        }
    }

    private Bitmap createMonogramBitmap(String name) {
        int size = KonohaTheme.dp(getContext(), 84);
        Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        android.graphics.Canvas canvas = new android.graphics.Canvas(bmp);
        android.graphics.Paint p = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);

        p.setColor(Color.parseColor("#153745"));
        canvas.drawRoundRect(0, 0, size, size, KonohaTheme.dp(getContext(), 8), KonohaTheme.dp(getContext(), 8), p);

        p.setColor(KonohaTheme.COLOR_PAPER);
        p.setTextSize(size * 0.45f);
        p.setTypeface(KonohaTheme.getCaveatBold(getContext()));
        p.setTextAlign(android.graphics.Paint.Align.CENTER);

        String letter = (name != null && !name.isEmpty()) ? name.substring(0, Math.min(name.length(), 2)) : "?";
        canvas.drawText(letter, size / 2f, size * 0.62f, p);

        return bmp;
    }

    public void release() {
        disposed = true;
        iconHandler.removeCallbacksAndMessages(null);
        iconExecutor.shutdownNow();
        for (Bitmap bmp : iconCache.values()) {
            if (bmp != null && !bmp.isRecycled()) bmp.recycle();
        }
        iconCache.clear();
    }
}
