package ua.konoha.tv.nativepreview.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import ua.konoha.tv.nativepreview.theme.KonohaTheme;

/**
 * Technique shortcut button (.technique-shortcut) in Section Heading.
 * Dot + Technique Name + Shuriken icon.
 */
public class TechniqueShortcutButton extends LinearLayout {
    private final View dotView;
    private final TextView nameView;
    private final GlyphIconView shurikenIcon;
    private final RectF borderRect = new RectF();
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private boolean isFocusedState = false;

    public TechniqueShortcutButton(Context context) {
        this(context, null);
    }

    public TechniqueShortcutButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setFocusable(true);
        setFocusableInTouchMode(true);
        setWillNotDraw(false);

        int padH = KonohaTheme.dp(context, 6);
        int padV = KonohaTheme.dp(context, 4);
        setPadding(padH, padV, padH, padV);

        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(KonohaTheme.dp(context, 1));
        borderPaint.setColor(KonohaTheme.COLOR_ORANGE);

        // Colored Dot
        dotView = new View(context) {
            private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            @Override
            protected void onDraw(Canvas canvas) {
                p.setColor(dotColor);
                canvas.drawCircle(getWidth() / 2f, getHeight() / 2f, getWidth() / 2f, p);
            }
        };
        int dotSize = KonohaTheme.dp(context, 6);
        addView(dotView, new LayoutParams(dotSize, dotSize));

        // Name
        nameView = new TextView(context);
        nameView.setTypeface(KonohaTheme.getNunito(context));
        nameView.setTextColor(Color.parseColor("#d9d7bf"));
        nameView.setTextSize(11);
        LayoutParams nLp = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        nLp.setMargins(KonohaTheme.dp(context, 6), 0, KonohaTheme.dp(context, 6), 0);
        addView(nameView, nLp);

        // Shuriken
        shurikenIcon = new GlyphIconView(context);
        shurikenIcon.setGlyph(GlyphIconView.Glyph.SHURIKEN);
        shurikenIcon.setIconColor(KonohaTheme.COLOR_ORANGE);
        int sSize = KonohaTheme.dp(context, 16);
        addView(shurikenIcon, new LayoutParams(sSize, sSize));

        setOnFocusChangeListener((v, hasFocus) -> {
            isFocusedState = hasFocus;
            invalidate();
        });
    }

    private int dotColor = KonohaTheme.COLOR_ORANGE;

    public void setTechnique(String name, int color) {
        nameView.setText(name);
        this.dotColor = color;
        shurikenIcon.setIconColor(color);
        dotView.invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (isFocusedState) {
            float s = borderPaint.getStrokeWidth();
            borderRect.set(s, s, getWidth() - s, getHeight() - s);
            canvas.drawRoundRect(borderRect, KonohaTheme.dp(getContext(), 4), KonohaTheme.dp(getContext(), 4), borderPaint);
        }
        super.onDraw(canvas);
    }
}
