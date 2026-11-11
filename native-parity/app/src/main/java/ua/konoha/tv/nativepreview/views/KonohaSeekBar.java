package ua.konoha.tv.nativepreview.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import ua.konoha.tv.nativepreview.theme.KonohaTheme;

/**
 * Custom volume slider (.volume-control) with orange progress, paper thumb,
 * and D-pad left/right step support.
 */
public class KonohaSeekBar extends View {
    public interface OnProgressChangeListener {
        void onProgressChanged(KonohaSeekBar seekBar, int progress);
    }

    private int max = 100;
    private int progress = 45;
    private OnProgressChangeListener listener;

    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint thumbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint focusRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF trackRect = new RectF();
    private final RectF progressRect = new RectF();

    public KonohaSeekBar(Context context) {
        this(context, null);
    }

    public KonohaSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        setFocusable(true);
        setFocusableInTouchMode(true);

        trackPaint.setColor(Color.parseColor("#153745"));
        progressPaint.setColor(KonohaTheme.COLOR_ORANGE);
        thumbPaint.setColor(KonohaTheme.COLOR_PAPER);

        focusRingPaint.setStyle(Paint.Style.STROKE);
        focusRingPaint.setStrokeWidth(KonohaTheme.dp(context, 1.5f));
        focusRingPaint.setColor(KonohaTheme.COLOR_ORANGE);
    }

    public void setOnProgressChangeListener(OnProgressChangeListener listener) {
        this.listener = listener;
    }

    public void setMax(int max) {
        this.max = Math.max(1, max);
        invalidate();
    }

    public void setProgress(int progress) {
        int newProgress = Math.max(0, Math.min(max, progress));
        if (this.progress != newProgress) {
            this.progress = newProgress;
            invalidate();
            if (listener != null) listener.onProgressChanged(this, this.progress);
        }
    }

    public int getProgress() {
        return progress;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            setProgress(progress - 5);
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            setProgress(progress + 5);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        int h = KonohaTheme.dp(getContext(), 24);
        setMeasuredDimension(w, h);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        float cy = h / 2f;
        float trackH = KonohaTheme.dp(getContext(), 6);
        float thumbR = KonohaTheme.dp(getContext(), 7);

        float left = thumbR;
        float right = w - thumbR;
        float usableW = right - left;

        float ratio = (float) progress / max;
        float thumbX = left + usableW * ratio;

        // Background track
        trackRect.set(left, cy - trackH / 2f, right, cy + trackH / 2f);
        canvas.drawRoundRect(trackRect, trackH / 2f, trackH / 2f, trackPaint);

        // Progress
        if (thumbX > left) {
            progressRect.set(left, cy - trackH / 2f, thumbX, cy + trackH / 2f);
            canvas.drawRoundRect(progressRect, trackH / 2f, trackH / 2f, progressPaint);
        }

        // Thumb
        canvas.drawCircle(thumbX, cy, thumbR, thumbPaint);

        // Focus ring
        if (hasFocus()) {
            canvas.drawCircle(thumbX, cy, thumbR + KonohaTheme.dp(getContext(), 3), focusRingPaint);
        }
    }
}
