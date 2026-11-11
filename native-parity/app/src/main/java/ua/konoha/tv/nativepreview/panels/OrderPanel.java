package ua.konoha.tv.nativepreview.panels;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import ua.konoha.tv.nativepreview.R;
import ua.konoha.tv.nativepreview.apps.AppManager;
import ua.konoha.tv.nativepreview.model.AppItem;
import ua.konoha.tv.nativepreview.theme.KonohaTheme;
import ua.konoha.tv.nativepreview.views.DockView;
import ua.konoha.tv.nativepreview.views.GlyphIconView;

import java.util.List;

/**
 * Order panel (.favorite-order) reproducing Chromium's favorite ordering.
 * Side sheet (680px) with item rows, number badges, and enabled/disabled up/down buttons.
 */
public class OrderPanel extends FrameLayout {
    public interface OnOrderActionListener {
        void onBackToLibraryRequested();
        void onCloseRequested();
    }

    private final AppManager appManager;
    private final DockView dockView;
    private final SideSheetContainer sheetContainer;
    private final ScrollView scrollView;
    private final LinearLayout content;
    private final LinearLayout listContainer;
    private final TextView countView;

    private OnOrderActionListener listener;
    private String focusTargetPackage = null;
    private boolean focusTargetIsDown = false;

    public OrderPanel(Context context, AppManager appManager, DockView dockView) {
        super(context);
        this.appManager = appManager;
        this.dockView = dockView;

        sheetContainer = new SideSheetContainer(context, false, "Архів Конохи", "Порядок обраного");
        sheetContainer.setOnCloseListener(() -> {
            if (listener != null) listener.onCloseRequested();
        });

        scrollView = new ScrollView(context);
        scrollView.setVerticalScrollBarEnabled(false);

        content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);

        // Instruction
        TextView note = new TextView(context);
        note.setText("Обери стрілку ↑ або ↓ біля застосунку та натисни OK. Перші шість позицій видно на головному екрані. Зміни зберігаються одразу.");
        note.setTextColor(Color.parseColor("#afc1b7"));
        note.setTextSize(11);
        note.setTypeface(KonohaTheme.getNunito(context));
        note.setPadding(0, 0, 0, KonohaTheme.dp(context, 8));
        content.addView(note);

        countView = new TextView(context);
        countView.setTextColor(Color.parseColor("#d8c199"));
        countView.setTextSize(12);
        countView.setTypeface(KonohaTheme.getNunito(context));
        countView.setPadding(0, 0, 0, KonohaTheme.dp(context, 14));
        content.addView(countView);

        listContainer = new LinearLayout(context);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        content.addView(listContainer);

        // Link back to library
        TextView backLink = new TextView(context);
        backLink.setText("До архіву застосунків →");
        backLink.setTextColor(KonohaTheme.COLOR_GOLD);
        backLink.setTextSize(13);
        backLink.setTypeface(KonohaTheme.getNunitoBold(context));
        backLink.setFocusable(true);
        backLink.setFocusableInTouchMode(true);
        backLink.setClickable(true);
        backLink.setPadding(0, KonohaTheme.dp(context, 18), 0, KonohaTheme.dp(context, 24));
        backLink.setOnClickListener(v -> {
            if (listener != null) listener.onBackToLibraryRequested();
        });
        backLink.setOnFocusChangeListener((v, hasFocus) -> {
            backLink.setTextColor(hasFocus ? KonohaTheme.COLOR_ORANGE : KonohaTheme.COLOR_GOLD);
        });
        content.addView(backLink);

        scrollView.addView(content, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        sheetContainer.setBodyView(scrollView);
        addView(sheetContainer, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    }

    public void setOnOrderActionListener(OnOrderActionListener listener) {
        this.listener = listener;
    }

    public void renderList() {
        renderOrderList();
    }

    public void renderOrderList() {
        listContainer.removeAllViews();
        List<AppItem> favorites = appManager.getPinnedApps();
        countView.setText("Обраних застосунків: " + favorites.size() + ".");

        int count = favorites.size();
        View viewToFocus = null;

        for (int i = 0; i < count; i++) {
            AppItem app = favorites.get(i);
            boolean isFirst = (i == 0);
            boolean isLast = (i == count - 1);
            boolean isTopSix = (i < 6);

            LinearLayout row = createOrderRow(app, i + 1, isFirst, isLast, isTopSix);
            listContainer.addView(row);

            if (app.getPackageName().equals(focusTargetPackage)) {
                viewToFocus = focusTargetIsDown ? row.findViewById(R.id.order_down) : row.findViewById(R.id.order_up);
            }
        }

        if (viewToFocus != null) {
            final View fView = viewToFocus;
            post(() -> fView.requestFocus());
        }
        focusTargetPackage = null;
    }

    private LinearLayout createOrderRow(AppItem app, int position, boolean isFirst, boolean isLast, boolean isTopSix) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(KonohaTheme.dp(getContext(), 14), KonohaTheme.dp(getContext(), 10),
                KonohaTheme.dp(getContext(), 14), KonohaTheme.dp(getContext(), 10));

        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Color.parseColor("#102b38"));
        gd.setCornerRadius(KonohaTheme.dp(getContext(), 4));
        gd.setStroke(KonohaTheme.dp(getContext(), 1), Color.parseColor("#22dbbd7d"));
        row.setBackground(gd);

        // 1. Number
        TextView numView = new TextView(getContext());
        numView.setText(String.valueOf(position));
        numView.setTextColor(Color.parseColor("#9eb1b8"));
        numView.setTextSize(13);
        numView.setTypeface(KonohaTheme.getNunitoBold(getContext()));
        numView.setPadding(0, 0, KonohaTheme.dp(getContext(), 14), 0);
        row.addView(numView);

        // 2. Icon
        ImageView iconView = new ImageView(getContext());
        int iconSize = KonohaTheme.dp(getContext(), 36);
        row.addView(iconView, new LinearLayout.LayoutParams(iconSize, iconSize));
        if (app.getSystemIcon() != null) {
            iconView.setImageDrawable(app.getSystemIcon());
        }

        // 3. Title + Subtitle
        LinearLayout textCol = new LinearLayout(getContext());
        textCol.setOrientation(LinearLayout.VERTICAL);
        textCol.setPadding(KonohaTheme.dp(getContext(), 12), 0, KonohaTheme.dp(getContext(), 12), 0);

        TextView titleView = new TextView(getContext());
        titleView.setText(app.getName());
        titleView.setTextColor(KonohaTheme.COLOR_PAPER);
        titleView.setTextSize(13);
        titleView.setTypeface(KonohaTheme.getNunitoBold(getContext()));
        textCol.addView(titleView);

        TextView subView = new TextView(getContext());
        subView.setText(isTopSix ? "На головному екрані" : "В архіві");
        subView.setTextColor(isTopSix ? Color.parseColor("#7aa197") : Color.parseColor("#6e828a"));
        subView.setTextSize(10);
        subView.setTypeface(KonohaTheme.getNunito(getContext()));
        textCol.addView(subView);

        row.addView(textCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        // 4. Arrow buttons: Up & Down
        FrameLayout upBtn = createArrowButton(GlyphIconView.Glyph.ARROW_UP, isFirst, () -> {
            focusTargetPackage = app.getPackageName();
            focusTargetIsDown = false;
            appManager.moveFavorite(app.getPackageName(), -1);
            if (dockView != null) dockView.render();
            renderOrderList();
        });
        upBtn.setId(R.id.order_up);
        row.addView(upBtn);

        FrameLayout downBtn = createArrowButton(GlyphIconView.Glyph.ARROW_DOWN, isLast, () -> {
            focusTargetPackage = app.getPackageName();
            focusTargetIsDown = true;
            appManager.moveFavorite(app.getPackageName(), 1);
            if (dockView != null) dockView.render();
            renderOrderList();
        });
        downBtn.setId(R.id.order_down);
        LinearLayout.LayoutParams dLp = new LinearLayout.LayoutParams(
                KonohaTheme.dp(getContext(), 36), KonohaTheme.dp(getContext(), 36)
        );
        dLp.setMargins(KonohaTheme.dp(getContext(), 8), 0, 0, 0);
        downBtn.setLayoutParams(dLp);
        row.addView(downBtn);

        LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        rLp.setMargins(0, KonohaTheme.dp(getContext(), 4), 0, KonohaTheme.dp(getContext(), 4));
        row.setLayoutParams(rLp);

        return row;
    }

    private FrameLayout createArrowButton(GlyphIconView.Glyph glyph, boolean disabled, Runnable action) {
        FrameLayout btn = new FrameLayout(getContext());
        int size = KonohaTheme.dp(getContext(), 36);
        btn.setLayoutParams(new LinearLayout.LayoutParams(size, size));
        btn.setFocusable(!disabled);
        btn.setFocusableInTouchMode(!disabled);
        btn.setClickable(!disabled);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#153745"));
        bg.setCornerRadius(KonohaTheme.dp(getContext(), 4));
        bg.setStroke(KonohaTheme.dp(getContext(), 1), Color.parseColor("#33dbbd7d"));
        btn.setBackground(bg);

        GlyphIconView icon = new GlyphIconView(getContext());
        icon.setGlyph(glyph);
        icon.setIconColor(disabled ? Color.parseColor("#449eb1b8") : KonohaTheme.COLOR_PAPER);
        int iSize = KonohaTheme.dp(getContext(), 16);
        FrameLayout.LayoutParams iLp = new FrameLayout.LayoutParams(iSize, iSize, Gravity.CENTER);
        btn.addView(icon, iLp);

        if (!disabled) {
            btn.setOnClickListener(v -> action.run());
            btn.setOnFocusChangeListener((v, hasFocus) -> {
                GradientDrawable d = new GradientDrawable();
                d.setColor(hasFocus ? Color.parseColor("#2a4f60") : Color.parseColor("#153745"));
                d.setCornerRadius(KonohaTheme.dp(getContext(), 4));
                d.setStroke(KonohaTheme.dp(getContext(), hasFocus ? 2 : 1),
                        hasFocus ? KonohaTheme.COLOR_ORANGE : Color.parseColor("#33dbbd7d"));
                v.setBackground(d);
            });
        } else {
            btn.setAlpha(0.35f);
        }

        return btn;
    }
}
