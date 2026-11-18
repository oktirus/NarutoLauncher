package ua.konoha.tv.nativepreview.panels;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import ua.konoha.tv.nativepreview.model.AppItem;
import ua.konoha.tv.nativepreview.theme.KonohaTheme;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Konoha TV Context Menu designed specifically for TV remotes (D-pad).
 * Provides fast, intuitive actions for any app:
 * - Launch app
 * - Move left/right on dock (if called from dock)
 * - Add to / Remove from favorites (Pin/Unpin)
 * - Reorder favorites
 * - Open system app info
 * - Close/Dismiss
 */
public class AppContextMenuPanel extends FrameLayout {
    public interface OnContextMenuActionListener {
        void onLaunch(AppItem app);
        void onTogglePin(AppItem app);
        void onMove(AppItem app, int direction); // -1: left, +1: right
        void onOpenOrder();
        void onAppInfo(AppItem app);
        void onDismiss();
    }

    private final AppItem app;
    private final boolean isFromDock;
    private final int dockPosition;
    private final int dockCount;
    private final OnContextMenuActionListener listener;
    private View firstFocusableAction;
    private boolean waitingForInitialKeyUp = true;

    public AppContextMenuPanel(Context context, AppItem app, boolean isFromDock,
                               int dockPosition, int dockCount,
                               OnContextMenuActionListener listener) {
        super(context);
        this.app = app;
        this.isFromDock = isFromDock;
        this.dockPosition = dockPosition;
        this.dockCount = dockCount;
        this.listener = listener;

        init();
    }

    private void init() {
        setFocusable(false);
        setFocusableInTouchMode(false);
        setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);

        // 1. Dark acrylic scrim background (70% dim)
        setBackgroundColor(Color.parseColor("#B3071c27"));

        // Click outside closes menu
        setOnClickListener(v -> {
            if (listener != null) listener.onDismiss();
        });

        // 2. Central modal dialog card
        LinearLayout card = new LinearLayout(getContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setClickable(true); // Don't propagate clicks to scrim

        GradientDrawable cardBg = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.parseColor("#183d4a"), Color.parseColor("#0e2834")}
        );
        cardBg.setCornerRadius(KonohaTheme.dp(getContext(), 12));
        cardBg.setStroke(KonohaTheme.dp(getContext(), 2), KonohaTheme.COLOR_GOLD);

        int padH = KonohaTheme.dp(getContext(), 24);
        int padV = KonohaTheme.dp(getContext(), 20);
        card.setPadding(padH, padV, padH, padV);

        int dialogWidth = Math.min(
                KonohaTheme.dp(getContext(), 460),
                (int) (getResources().getDisplayMetrics().widthPixels * 0.85f)
        );
        ScrollView cardScroll = new ScrollView(getContext());
        cardScroll.setBackground(cardBg);
        cardScroll.setFocusable(false);
        cardScroll.setSmoothScrollingEnabled(false);
        LayoutParams cardLp = new LayoutParams(dialogWidth, LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        cardLp.topMargin = KonohaTheme.dp(getContext(), 16);
        cardLp.bottomMargin = cardLp.topMargin;
        addView(cardScroll, cardLp);
        cardScroll.addView(card, new ScrollView.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        // 3. Header: App Icon + Titles
        LinearLayout header = new LinearLayout(getContext());
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(0, 0, 0, KonohaTheme.dp(getContext(), 14));

        ImageView iconView = new ImageView(getContext());
        int iconSize = KonohaTheme.dp(getContext(), 52);
        iconView.setLayoutParams(new LinearLayout.LayoutParams(iconSize, iconSize));
        iconView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        bindAppIcon(iconView, app);
        header.addView(iconView);

        LinearLayout textCol = new LinearLayout(getContext());
        textCol.setOrientation(LinearLayout.VERTICAL);
        textCol.setPadding(KonohaTheme.dp(getContext(), 14), 0, 0, 0);

        TextView titleView = new TextView(getContext());
        titleView.setText(app.getName());
        titleView.setTextColor(Color.parseColor("#fff3d0"));
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        titleView.setTypeface(KonohaTheme.getNunitoBold(getContext()));
        titleView.setSingleLine(true);
        titleView.setEllipsize(TextUtils.TruncateAt.END);
        textCol.addView(titleView);

        TextView statusView = new TextView(getContext());
        String statusText;
        if (isFromDock) {
            statusText = "★ Головний екран (позиція " + (dockPosition + 1) + " з " + dockCount + ")";
        } else if (app.isPinned()) {
            statusText = "★ В обраному";
        } else {
            statusText = "✦ В архіві застосунків";
        }
        statusView.setText(statusText);
        statusView.setTextColor(app.isPinned() ? KonohaTheme.COLOR_GOLD : Color.parseColor("#8aa2ad"));
        statusView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        statusView.setTypeface(KonohaTheme.getNunito(getContext()));
        textCol.addView(statusView);

        header.addView(textCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        card.addView(header);

        // Divider
        View divider = new View(getContext());
        divider.setBackgroundColor(Color.parseColor("#33f2c576"));
        card.addView(divider, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, KonohaTheme.dp(getContext(), 1)));

        // 4. Action List Container
        LinearLayout actionList = new LinearLayout(getContext());
        actionList.setOrientation(LinearLayout.VERTICAL);
        actionList.setPadding(0, KonohaTheme.dp(getContext(), 10), 0, 0);
        card.addView(actionList);

        List<View> actionButtons = new ArrayList<>();

        // Action 1: Відкрити
        View btnLaunch = createActionButton("▶  Відкрити застосунок", () -> {
            if (listener != null) listener.onLaunch(app);
        });
        actionButtons.add(btnLaunch);
        actionList.addView(btnLaunch);
        firstFocusableAction = btnLaunch;

        // Action 2: Move Left (if on dock and position > 0)
        if (isFromDock && dockPosition > 0) {
            View btnMoveLeft = createActionButton("◀  Перемістити ліворуч", () -> {
                if (listener != null) listener.onMove(app, -1);
            });
            actionButtons.add(btnMoveLeft);
            actionList.addView(btnMoveLeft);
        }

        // Action 3: Move Right (if on dock and position < dockCount - 1)
        if (isFromDock && dockPosition < dockCount - 1) {
            View btnMoveRight = createActionButton("▶  Перемістити праворуч", () -> {
                if (listener != null) listener.onMove(app, 1);
            });
            actionButtons.add(btnMoveRight);
            actionList.addView(btnMoveRight);
        }

        // Action 4: Toggle Pin (Add or Remove from dock)
        String pinLabel = app.isPinned() ? "☆  Прибрати з обраного" : "★  Додати до обраного";
        View btnPin = createActionButton(pinLabel, () -> {
            if (listener != null) listener.onTogglePin(app);
        });
        actionButtons.add(btnPin);
        actionList.addView(btnPin);

        // Action 5: Order Panel (if pinned)
        if (app.isPinned()) {
            View btnOrder = createActionButton("↕  Змінити порядок обраного", () -> {
                if (listener != null) listener.onOpenOrder();
            });
            actionButtons.add(btnOrder);
            actionList.addView(btnOrder);
        }

        // Action 6: App Info
        View btnInfo = createActionButton("⚙  Налаштування застосунку", () -> {
            if (listener != null) listener.onAppInfo(app);
        });
        actionButtons.add(btnInfo);
        actionList.addView(btnInfo);

        // Action 7: Close
        View btnCancel = createActionButton("✕  Закрити меню", () -> {
            if (listener != null) listener.onDismiss();
        });
        actionButtons.add(btnCancel);
        actionList.addView(btnCancel);

        // Explicit D-pad focus isolation: lock focus strictly within this dialog
        for (int i = 0; i < actionButtons.size(); i++) {
            actionButtons.get(i).setId(View.generateViewId());
        }
        for (int i = 0; i < actionButtons.size(); i++) {
            View btn = actionButtons.get(i);
            if (i > 0) {
                btn.setNextFocusUpId(actionButtons.get(i - 1).getId());
            } else {
                btn.setNextFocusUpId(btn.getId());
            }
            if (i < actionButtons.size() - 1) {
                btn.setNextFocusDownId(actionButtons.get(i + 1).getId());
            } else {
                btn.setNextFocusDownId(btn.getId());
            }
            btn.setNextFocusLeftId(btn.getId());
            btn.setNextFocusRightId(btn.getId());
        }

        // Handle Back key
        setFocusableInTouchMode(true);
        setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN
                    && (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE)) {
                if (listener != null) listener.onDismiss();
                return true;
            }
            return false;
        });

        waitingForInitialKeyUp = true;

        post(() -> {
            if (firstFocusableAction != null) {
                firstFocusableAction.requestFocus();
            }
        });
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER
                || keyCode == KeyEvent.KEYCODE_ENTER
                || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) {
            if (waitingForInitialKeyUp) {
                if (event.getAction() == KeyEvent.ACTION_UP) {
                    waitingForInitialKeyUp = false;
                }
                return true; // Consume repeat DOWNs and the initial long-press UP
            }
        }
        return super.dispatchKeyEvent(event);
    }

    private View createActionButton(String text, Runnable onClick) {
        TextView btn = new TextView(getContext());
        btn.setText(text);
        btn.setTextColor(Color.parseColor("#e6ede8"));
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        btn.setTypeface(KonohaTheme.getNunitoBold(getContext()));
        btn.setGravity(Gravity.CENTER_VERTICAL);
        btn.setPadding(
                KonohaTheme.dp(getContext(), 16),
                KonohaTheme.dp(getContext(), 10),
                KonohaTheme.dp(getContext(), 16),
                KonohaTheme.dp(getContext(), 10)
        );

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(0, KonohaTheme.dp(getContext(), 4), 0, KonohaTheme.dp(getContext(), 4));
        btn.setLayoutParams(lp);

        btn.setFocusable(true);
        btn.setFocusableInTouchMode(true);
        btn.setClickable(true);

        GradientDrawable normalBg = new GradientDrawable();
        normalBg.setColor(Color.parseColor("#102b38"));
        normalBg.setCornerRadius(KonohaTheme.dp(getContext(), 6));
        normalBg.setStroke(KonohaTheme.dp(getContext(), 1), Color.parseColor("#1f4150"));
        btn.setBackground(normalBg);

        btn.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) btn.setPressed(false);
            GradientDrawable d = new GradientDrawable();
            d.setColor(hasFocus ? Color.parseColor("#2e2618") : Color.parseColor("#102b38"));
            d.setCornerRadius(KonohaTheme.dp(getContext(), 6));
            d.setStroke(
                    KonohaTheme.dp(getContext(), hasFocus ? 2 : 1),
                    hasFocus ? KonohaTheme.COLOR_GOLD : Color.parseColor("#1f4150")
            );
            btn.setBackground(d);
            btn.setTextColor(hasFocus ? KonohaTheme.COLOR_GOLD : Color.parseColor("#e6ede8"));
            btn.setScaleX(hasFocus ? 1.02f : 1.0f);
            btn.setScaleY(hasFocus ? 1.02f : 1.0f);
        });

        btn.setOnClickListener(v -> {
            if (!waitingForInitialKeyUp) {
                onClick.run();
            }
        });

        btn.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER
                    || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) {
                if (waitingForInitialKeyUp) {
                    if (event.getAction() == KeyEvent.ACTION_UP) {
                        waitingForInitialKeyUp = false;
                    }
                    return true;
                }
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (event.getRepeatCount() == 0) btn.setPressed(true);
                    return true;
                } else if (event.getAction() == KeyEvent.ACTION_UP) {
                    boolean wasPressed = btn.isPressed();
                    btn.setPressed(false);
                    if (wasPressed && !event.isCanceled()) onClick.run();
                    return true;
                }
            }
            return false;
        });

        return btn;
    }

    private void bindAppIcon(ImageView iconView, AppItem app) {
        String assetPath = app.getAssetPath();
        if (assetPath != null) {
            try (InputStream is = getContext().getAssets().open(assetPath)) {
                Bitmap bmp = BitmapFactory.decodeStream(is);
                if (bmp != null) {
                    iconView.setImageBitmap(bmp);
                    return;
                }
            } catch (Exception ignored) {}
        }
        if (app.getSystemIcon() != null) {
            iconView.setImageDrawable(app.getSystemIcon());
        }
    }
}
