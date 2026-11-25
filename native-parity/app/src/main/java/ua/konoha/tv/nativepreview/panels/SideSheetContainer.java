package ua.konoha.tv.nativepreview.panels;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import ua.konoha.tv.nativepreview.theme.KonohaTheme;
import ua.konoha.tv.nativepreview.views.GlyphIconView;

/**
 * Standard side-sheet container reproducing .scroll-panel.
 * Slides from the right with exact width (680px regular, 1050px wide),
 * golden left rim shadow, Caveat title, subtitle, and elegant close button.
 */
public class SideSheetContainer extends FrameLayout {
    public interface OnCloseListener {
        void onClose();
    }

    private final boolean isWide;
    private final FrameLayout sheetLayout;
    private final LinearLayout headerLayout;
    private final TextView subtitleView;
    private final TextView titleView;
    private final FrameLayout closeButton;
    private final GlyphIconView closeIcon;
    private final FrameLayout bodyContainer;

    private final Paint scrimPaint = new Paint();
    private final Paint goldRimPaint = new Paint();
    private final Paint shadowPaint = new Paint();

    private OnCloseListener closeListener;

    public SideSheetContainer(Context context, boolean isWide, String subtitle, String title) {
        super(context);
        this.isWide = isWide;

        setFocusable(false);
        setFocusableInTouchMode(false);
        setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);

        scrimPaint.setColor(Color.parseColor("#99071c27")); // 60% dimming
        goldRimPaint.setColor(Color.parseColor("#f2c576"));
        goldRimPaint.setStrokeWidth(KonohaTheme.dp(context, 2f));
        goldRimPaint.setStyle(Paint.Style.STROKE);

        shadowPaint.setColor(Color.parseColor("#e6071c27"));
        shadowPaint.setStyle(Paint.Style.FILL);

        // Clicking scrim closes sheet
        setOnClickListener(v -> {
            if (closeListener != null) closeListener.onClose();
        });

        // Sheet itself on the right
        sheetLayout = new FrameLayout(context);
        sheetLayout.setClickable(true); // Don't pass clicks through to scrim

        GradientDrawable bg = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{KonohaTheme.COLOR_PANEL_START, KonohaTheme.COLOR_PANEL_END}
        );
        sheetLayout.setBackground(bg);
        sheetLayout.setPadding(
                KonohaTheme.dp(context, 36),
                KonohaTheme.dp(context, 32),
                KonohaTheme.dp(context, 36),
                KonohaTheme.dp(context, 24)
        );

        LinearLayout verticalBox = new LinearLayout(context);
        verticalBox.setOrientation(LinearLayout.VERTICAL);

        // Header: Subtitle + Title + Close Button
        headerLayout = new LinearLayout(context);
        headerLayout.setOrientation(LinearLayout.HORIZONTAL);
        headerLayout.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout titleCol = new LinearLayout(context);
        titleCol.setOrientation(LinearLayout.VERTICAL);

        subtitleView = new TextView(context);
        subtitleView.setText(subtitle != null ? subtitle : "Архів Конохи");
        subtitleView.setTextColor(Color.parseColor("#afc1b7"));
        subtitleView.setTextSize(11);
        subtitleView.setTypeface(KonohaTheme.getNunito(context));
        titleCol.addView(subtitleView);

        titleView = new TextView(context);
        titleView.setText(title);
        titleView.setTextColor(KonohaTheme.COLOR_PAPER);
        titleView.setTextSize(34);
        titleView.setTypeface(KonohaTheme.getCaveatBold(context));
        titleCol.addView(titleView);

        headerLayout.addView(titleCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        // Close button: 38x38 box with 'x' icon
        closeButton = new FrameLayout(context);
        int cbSize = KonohaTheme.dp(context, 38);
        LinearLayout.LayoutParams cbLp = new LinearLayout.LayoutParams(cbSize, cbSize);
        closeButton.setLayoutParams(cbLp);
        closeButton.setFocusable(true);

        GradientDrawable cbBg = new GradientDrawable();
        cbBg.setColor(Color.parseColor("#33102b38"));
        cbBg.setCornerRadius(KonohaTheme.dp(context, 4));
        cbBg.setStroke(KonohaTheme.dp(context, 1), Color.parseColor("#44d8c199"));
        closeButton.setBackground(cbBg);

        closeIcon = new GlyphIconView(context);
        closeIcon.setGlyph(GlyphIconView.Glyph.CLOSE);
        closeIcon.setIconColor(KonohaTheme.COLOR_PAPER);
        int iconSize = KonohaTheme.dp(context, 16);
        FrameLayout.LayoutParams ciLp = new FrameLayout.LayoutParams(iconSize, iconSize, Gravity.CENTER);
        closeButton.addView(closeIcon, ciLp);

        closeButton.setOnClickListener(v -> {
            if (closeListener != null) closeListener.onClose();
        });

        closeButton.setOnFocusChangeListener((v, hasFocus) -> {
            GradientDrawable d = new GradientDrawable();
            d.setColor(hasFocus ? Color.parseColor("#80173b47") : Color.parseColor("#33102b38"));
            d.setCornerRadius(KonohaTheme.dp(context, 4));
            d.setStroke(KonohaTheme.dp(context, 1), hasFocus ? KonohaTheme.COLOR_ORANGE : Color.parseColor("#44d8c199"));
            v.setBackground(d);
        });

        headerLayout.addView(closeButton);
        verticalBox.addView(headerLayout);

        // Body container
        bodyContainer = new FrameLayout(context);
        verticalBox.addView(bodyContainer, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f
        ));

        sheetLayout.addView(verticalBox, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ));

        addView(sheetLayout);
    }

    public void setOnCloseListener(OnCloseListener listener) {
        this.closeListener = listener;
    }

    public void setTitle(String title) {
        titleView.setText(title);
    }

    public void setBodyView(View view) {
        bodyContainer.removeAllViews();
        if (view != null) {
            bodyContainer.addView(view, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            ));
        }
    }

    public View getCloseButton() {
        return closeButton;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        int h = MeasureSpec.getSize(heightMeasureSpec);

        if (w > 0) {
            int sheetW;
            if (isWide) {
                sheetW = Math.min(KonohaTheme.dp(getContext(), 1050), (int) (w * 0.95f));
            } else {
                sheetW = Math.min(KonohaTheme.dp(getContext(), 680), (int) (w * 0.94f));
            }
            LayoutParams lp = (LayoutParams) sheetLayout.getLayoutParams();
            if (lp != null) {
                lp.width = sheetW;
                lp.height = LayoutParams.MATCH_PARENT;
                lp.gravity = Gravity.END;
                sheetLayout.setLayoutParams(lp);
            }
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        // Draw scrim on the left of sheet
        int sheetLeft = sheetLayout.getLeft();
        if (sheetLeft > 0) {
            canvas.drawRect(0, 0, sheetLeft, getHeight(), scrimPaint);
            // Draw golden left rim line
            canvas.drawLine(sheetLeft, 0, sheetLeft, getHeight(), goldRimPaint);
        }
        super.dispatchDraw(canvas);
    }
}
