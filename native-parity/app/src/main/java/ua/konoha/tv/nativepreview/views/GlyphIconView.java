package ua.konoha.tv.nativepreview.views;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;
import ua.konoha.tv.nativepreview.theme.KonohaTheme;

/**
 * Custom vector icon view rendering SVG symbols from index.html on Canvas.
 */
public class GlyphIconView extends View {
    public enum Glyph {
        GAMEPAD,
        SCROLL,
        WALLPAPERS,
        SHURIKEN,
        CLOSE,
        LEAF_HEADBAND,
        ARROW_UP,
        ARROW_DOWN,
        SEAL
    }

    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private Glyph glyph = Glyph.GAMEPAD;
    private int iconColor = KonohaTheme.COLOR_PAPER;

    public GlyphIconView(Context context) {
        super(context);
        init();
    }

    public GlyphIconView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(2f);
        strokePaint.setStrokeCap(Paint.Cap.ROUND);
        strokePaint.setStrokeJoin(Paint.Join.ROUND);
        strokePaint.setColor(iconColor);

        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColor(iconColor);

        textPaint.setTypeface(Typeface.SERIF);
        textPaint.setColor(iconColor);
        textPaint.setTextSize(14f);
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setGlyph(Glyph glyph) {
        this.glyph = glyph;
        invalidate();
    }

    public void setIconColor(int color) {
        this.iconColor = color;
        strokePaint.setColor(color);
        fillPaint.setColor(color);
        textPaint.setColor(color);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        canvas.save();
        // Scale 24x24 standard SVG viewport to View size
        float scale = Math.min(w, h) / 24f;
        canvas.translate((w - 24f * scale) / 2f, (h - 24f * scale) / 2f);
        canvas.scale(scale, scale);

        strokePaint.setStrokeWidth(1.8f);

        switch (glyph) {
            case GAMEPAD:
                // <path d="M7 7h10q3 0 4 4l1 7q0 3-3 2l-4-3H9l-4 3q-3 1-3-2l1-7q1-4 4-4Z"/>
                path.reset();
                path.moveTo(7, 7);
                path.lineTo(17, 7);
                path.quadTo(20, 7, 21, 11);
                path.lineTo(22, 18);
                path.quadTo(22, 21, 19, 20);
                path.lineTo(15, 17);
                path.lineTo(9, 17);
                path.lineTo(5, 20);
                path.quadTo(2, 21, 2, 18);
                path.lineTo(3, 11);
                path.quadTo(4, 7, 7, 7);
                path.close();
                canvas.drawPath(path, strokePaint);

                // D-pad & buttons
                canvas.drawLine(7, 10, 7, 14, strokePaint);
                canvas.drawLine(5, 12, 9, 12, strokePaint);
                canvas.drawCircle(17, 11, 1.2f, fillPaint);
                canvas.drawCircle(19, 13, 1.2f, fillPaint);
                break;

            case SCROLL:
                // Settings scroll: M5 4h14v16H5z M3 3h4v18H3z M17 3h4v18h-4 M9 8h6 M9 12h6 M9 16h3
                path.reset();
                path.addRect(5, 4, 19, 20, Path.Direction.CW);
                canvas.drawPath(path, strokePaint);

                path.reset();
                path.addRoundRect(2, 2, 6, 22, 1f, 1f, Path.Direction.CW);
                path.addRoundRect(18, 2, 22, 22, 1f, 1f, Path.Direction.CW);
                canvas.drawPath(path, strokePaint);

                canvas.drawLine(9, 8, 15, 8, strokePaint);
                canvas.drawLine(9, 12, 15, 12, strokePaint);
                canvas.drawLine(9, 16, 13, 16, strokePaint);
                break;

            case WALLPAPERS:
                // Stacked pictures: M3 16V3h16 M7 7h14v14H7Z
                path.reset();
                path.moveTo(3, 16);
                path.lineTo(3, 3);
                path.lineTo(19, 3);
                canvas.drawPath(path, strokePaint);

                path.reset();
                path.addRect(7, 7, 21, 21, Path.Direction.CW);
                canvas.drawPath(path, strokePaint);

                canvas.drawCircle(16.5f, 11.5f, 1.5f, strokePaint);
                path.reset();
                path.moveTo(7, 18);
                path.lineTo(11, 14);
                path.lineTo(15, 18);
                path.lineTo(17, 16);
                path.lineTo(21, 20);
                canvas.drawPath(path, strokePaint);
                break;

            case SHURIKEN:
                // m12 2 3 7 7 3-7 3-3 7-3-7-7-3 7-3Z circle cx="12" cy="12" r="2"
                path.reset();
                path.moveTo(12, 2);
                path.lineTo(15, 9);
                path.lineTo(22, 12);
                path.lineTo(15, 15);
                path.lineTo(12, 22);
                path.lineTo(9, 15);
                path.lineTo(2, 12);
                path.lineTo(9, 9);
                path.close();
                canvas.drawPath(path, strokePaint);
                canvas.drawCircle(12, 12, 2f, strokePaint);
                break;

            case CLOSE:
                // M5 5 19 19 M5 19 19 5
                canvas.drawLine(5, 5, 19, 19, strokePaint);
                canvas.drawLine(5, 19, 19, 5, strokePaint);
                break;

            case ARROW_UP:
                canvas.drawLine(12, 19, 12, 5, strokePaint);
                path.reset();
                path.moveTo(6, 11);
                path.lineTo(12, 5);
                path.lineTo(18, 11);
                canvas.drawPath(path, strokePaint);
                break;

            case ARROW_DOWN:
                canvas.drawLine(12, 5, 12, 19, strokePaint);
                path.reset();
                path.moveTo(6, 13);
                path.lineTo(12, 19);
                path.lineTo(18, 13);
                canvas.drawPath(path, strokePaint);
                break;

            case SEAL:
                // Kanji 忍 inside a rotated rectangular seal
                path.reset();
                path.addRect(2, 2, 22, 22, Path.Direction.CW);
                canvas.drawPath(path, strokePaint);
                canvas.drawText("忍", 12f, 17f, textPaint);
                break;

            case LEAF_HEADBAND:
                // Konoha spiral / leaf emblem
                path.reset();
                path.moveTo(3, 8);
                path.lineTo(21, 8);
                path.quadTo(23, 12, 21, 16);
                path.lineTo(3, 16);
                path.quadTo(1, 12, 3, 8);
                path.close();
                canvas.drawPath(path, strokePaint);

                // Leaf spiral
                path.reset();
                path.addCircle(12, 12, 2.5f, Path.Direction.CW);
                canvas.drawPath(path, strokePaint);
                canvas.drawLine(12, 9.5f, 15, 8f, strokePaint);
                break;
        }

        canvas.restore();
    }
}
