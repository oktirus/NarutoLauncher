package ua.konoha.tv.nativepreview.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import ua.konoha.tv.nativepreview.theme.KonohaTheme;

/**
 * Outlined button reproducing .hero-wallpapers and .glyph-button.
 */
public class OutlinedGlyphButton extends LinearLayout {
    private final GlyphIconView iconView;
    private final TextView labelView;
    private final RectF borderRect = new RectF();
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private boolean isFocusedState = false;
    private boolean isPill = false;
    private boolean isBorderless = false;
    private int normalBorderColor = Color.parseColor("#44d6b67c");
    private int focusedBorderColor = KonohaTheme.COLOR_ORANGE;
    private int normalBgColor = Color.parseColor("#6e122b36");
    private int focusedBgColor = Color.parseColor("#a0173b47");
    private float cornerRadius;

    public OutlinedGlyphButton(Context context) {
        this(context, null);
    }

    public OutlinedGlyphButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setFocusable(true);
        setFocusableInTouchMode(true);
        setWillNotDraw(false);

        cornerRadius = KonohaTheme.dp(context, 4);

        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(KonohaTheme.dp(context, 1));
        bgPaint.setStyle(Paint.Style.FILL);

        int padH = KonohaTheme.dp(context, 10);
        int padV = KonohaTheme.dp(context, 6);
        setPadding(padH, padV, padH, padV);

        iconView = new GlyphIconView(context);
        int iconSize = KonohaTheme.dp(context, 16);
        addView(iconView, new LayoutParams(iconSize, iconSize));

        labelView = new TextView(context);
        labelView.setTypeface(KonohaTheme.getNunito(context));
        labelView.setTextColor(KonohaTheme.COLOR_PAPER);
        labelView.setTextSize(11);
        LayoutParams lp = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(KonohaTheme.dp(context, 8), 0, 0, 0);
        addView(labelView, lp);
        labelView.setVisibility(GONE);

        setOnFocusChangeListener((v, hasFocus) -> {
            isFocusedState = hasFocus;
            invalidate();
        });
    }

    public void setPill(boolean pill) {
        this.isPill = pill;
        invalidate();
    }

    public void setBorderless(boolean borderless) {
        this.isBorderless = borderless;
        if (borderless) {
            int p = KonohaTheme.dp(getContext(), 6);
            setPadding(p, p, p, p);
        }
        invalidate();
    }

    public void setGlyph(GlyphIconView.Glyph glyph) {
        iconView.setGlyph(glyph);
    }

    public void setText(String text) {
        if (text != null && !text.isEmpty()) {
            labelView.setText(text);
            labelView.setVisibility(VISIBLE);
        } else {
            labelView.setVisibility(GONE);
        }
    }

    public void setIconSize(int sizeDp) {
        int px = KonohaTheme.dp(getContext(), sizeDp);
        iconView.setLayoutParams(new LayoutParams(px, px));
    }

    public void setColors(int normalBorder, int focusedBorder, int normalBg, int focusedBg) {
        this.normalBorderColor = normalBorder;
        this.focusedBorderColor = focusedBorder;
        this.normalBgColor = normalBg;
        this.focusedBgColor = focusedBg;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (isBorderless && !isFocusedState) {
            super.onDraw(canvas);
            return;
        }

        float stroke = borderPaint.getStrokeWidth();
        borderRect.set(stroke, stroke, getWidth() - stroke, getHeight() - stroke);
        float r = isPill ? (getHeight() / 2f) : cornerRadius;

        if (!isBorderless || isFocusedState) {
            bgPaint.setColor(isFocusedState ? focusedBgColor : normalBgColor);
            canvas.drawRoundRect(borderRect, r, r, bgPaint);

            borderPaint.setColor(isFocusedState ? focusedBorderColor : normalBorderColor);
            canvas.drawRoundRect(borderRect, r, r, borderPaint);
        }

        super.onDraw(canvas);
    }
}
