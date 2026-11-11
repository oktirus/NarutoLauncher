package ua.konoha.tv.nativepreview.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Checkable;
import ua.konoha.tv.nativepreview.theme.KonohaTheme;

/**
 * Custom toggle switch (.toggle) matching Chromium:
 * Oval pill with warm orange active track, #f5e3b6 circular thumb.
 */
public class KonohaSwitch extends View implements Checkable {
    public interface OnCheckedChangeListener {
        void onCheckedChanged(KonohaSwitch view, boolean isChecked);
    }

    private boolean isChecked = false;
    private OnCheckedChangeListener listener;

    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint thumbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint focusPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF trackRect = new RectF();
    private final RectF focusRect = new RectF();

    private final int trackOffColor = Color.parseColor("#153745");
    private final int trackOnColor = KonohaTheme.COLOR_ORANGE;
    private final int borderColor = Color.parseColor("#44d6b67c");
    private final int thumbColor = KonohaTheme.COLOR_PAPER;

    public KonohaSwitch(Context context) {
        this(context, null);
    }

    public KonohaSwitch(Context context, AttributeSet attrs) {
        super(context, attrs);
        setFocusable(true);
        setFocusableInTouchMode(true);
        setClickable(true);

        trackPaint.setStyle(Paint.Style.FILL);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(KonohaTheme.dp(context, 1));
        borderPaint.setColor(borderColor);
        thumbPaint.setStyle(Paint.Style.FILL);
        thumbPaint.setColor(thumbColor);

        focusPaint.setStyle(Paint.Style.STROKE);
        focusPaint.setStrokeWidth(KonohaTheme.dp(context, 2));
        focusPaint.setColor(KonohaTheme.COLOR_ORANGE);

        setOnClickListener(v -> toggle());
    }

    public void setOnCheckedChangeListener(OnCheckedChangeListener listener) {
        this.listener = listener;
    }

    @Override
    public void setChecked(boolean checked) {
        if (this.isChecked != checked) {
            this.isChecked = checked;
            invalidate();
            if (listener != null) listener.onCheckedChanged(this, checked);
        }
    }

    @Override
    public boolean isChecked() {
        return isChecked;
    }

    @Override
    public void toggle() {
        setChecked(!isChecked);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = KonohaTheme.dp(getContext(), 46);
        int h = KonohaTheme.dp(getContext(), 26);
        setMeasuredDimension(w, h);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        float radius = h / 2f;

        // Track
        trackRect.set(1f, 1f, w - 1f, h - 1f);
        trackPaint.setColor(isChecked ? trackOnColor : trackOffColor);
        canvas.drawRoundRect(trackRect, radius, radius, trackPaint);
        canvas.drawRoundRect(trackRect, radius, radius, borderPaint);

        // Thumb
        float thumbRadius = radius - KonohaTheme.dp(getContext(), 3);
        float thumbX = isChecked ? (w - radius) : radius;
        float thumbY = h / 2f;
        canvas.drawCircle(thumbX, thumbY, thumbRadius, thumbPaint);

        // Focus glow outline if focused
        if (hasFocus()) {
            focusRect.set(0, 0, w, h);
            canvas.drawRoundRect(focusRect, radius + 2, radius + 2, focusPaint);
        }
    }
}
