package ua.konoha.tv.nativepreview.views;

import android.content.Context;
import android.graphics.*;
import android.view.View;

public class ChakraFocusView extends View {
    private final TechniqueRenderer renderer;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private String currentTechnique = "rasengan";
    private View targetView = null;
    private final RectF targetRect = new RectF();
    private final RectF prevRect = new RectF();
    private final RectF ringRect = new RectF();
    private final int[] targetLocation = new int[2];
    private final int[] overlayLocation = new int[2];
    private static final int COLOR_ORANGE = Color.parseColor("#ed943f");

    private boolean isMotionEnabled = true;
    private boolean isActivityPaused = false;
    private boolean isListenerRegistered = false;
    private float burstLife = 0f;
    private long lastFrameTime = 0;

    private final TechniqueRenderer.OnBitmapLoadedListener loadListener = name -> {
        if (name == null) return;
        boolean matches = name.equals(currentTechnique)
                || ("gamabunta".equals(currentTechnique) && name.startsWith("gamabunta"));
        if (matches) {
            // Load-triggered invalidation: redraws without starting an animation loop!
            postInvalidate();
        }
    };

    private final Runnable animationRunnable = new Runnable() {
        @Override
        public void run() {
            if (shouldAnimate()) {
                invalidate();
                postOnAnimation(this);
            }
        }
    };

    public ChakraFocusView(Context context, TechniqueRenderer renderer) {
        super(context);
        this.renderer = renderer;
    }

    private synchronized void registerBitmapListener() {
        if (!isListenerRegistered && renderer != null) {
            renderer.addOnBitmapLoadedListener(loadListener);
            isListenerRegistered = true;
        }
    }

    private synchronized void unregisterBitmapListener() {
        if (isListenerRegistered && renderer != null) {
            renderer.removeOnBitmapLoadedListener(loadListener);
            isListenerRegistered = false;
        }
    }

    public void pause() {
        isActivityPaused = true;
        removeCallbacks(animationRunnable);
    }

    public void resume() {
        isActivityPaused = false;
        checkAnimationState();
    }

    public boolean isPaused() {
        return isActivityPaused;
    }

    private boolean shouldAnimate() {
        if (isActivityPaused) return false;
        if (!isAttachedToWindow()) return false;
        if (getWindowVisibility() != VISIBLE) return false;
        if (getVisibility() != VISIBLE || !isShown()) return false;
        if (!isMotionEnabled) return false;
        if (targetView == null || !targetView.isAttachedToWindow() || targetView.getVisibility() != VISIBLE || !targetView.isShown()) {
            return false;
        }
        return true;
    }

    public void setTechnique(String technique) {
        this.currentTechnique = technique;
        invalidate();
    }

    public void setMotionEnabled(boolean enabled) {
        this.isMotionEnabled = enabled;
        checkAnimationState();
    }

    public void setTargetView(View target) {
        if (this.targetView == target) return;
        if (target == null) {
            this.targetView = null;
            checkAnimationState();
            invalidate();
            return;
        }

        prevRect.set(targetRect);

        this.targetView = target;
        target.getLocationInWindow(targetLocation);
        getLocationInWindow(overlayLocation);

        float left = targetLocation[0] - overlayLocation[0];
        float top = targetLocation[1] - overlayLocation[1];
        targetRect.set(left, top, left + target.getWidth(), top + target.getHeight());

        if ("gamabunta".equals(currentTechnique) && prevRect.width() > 0) {
            PointF from = new PointF(prevRect.centerX(), prevRect.centerY());
            PointF to = new PointF(targetRect.centerX(), targetRect.centerY());
            int dir = to.x >= from.x ? 1 : -1;
            renderer.triggerGamabuntaJump(from, to, dir);
        }

        checkAnimationState();
        invalidate();
    }

    public void triggerBurst() {
        burstLife = 1.0f;
        invalidate();
    }

    private void checkAnimationState() {
        removeCallbacks(animationRunnable);
        if (shouldAnimate()) {
            lastFrameTime = System.currentTimeMillis();
            postOnAnimation(animationRunnable);
        } else {
            invalidate();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        registerBitmapListener();
        checkAnimationState();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeCallbacks(animationRunnable);
        unregisterBitmapListener();
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        checkAnimationState();
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        checkAnimationState();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (targetView == null || !targetView.isAttachedToWindow() || targetView.getVisibility() != VISIBLE || !targetView.isShown()) {
            if (targetView != null && !targetView.isAttachedToWindow()) {
                targetView = null;
                checkAnimationState();
            }
            return;
        }

        // Оновлюємо координати, якщо розмір чи позиція цілі змінилися
        targetView.getLocationInWindow(targetLocation);
        getLocationInWindow(overlayLocation);
        float left = targetLocation[0] - overlayLocation[0];
        float top = targetLocation[1] - overlayLocation[1];
        targetRect.set(left, top, left + targetView.getWidth(), top + targetView.getHeight());

        long now = System.currentTimeMillis();
        float dt = lastFrameTime > 0 ? (now - lastFrameTime) / 1000f : 0.016f;
        if (dt > 0.05f) dt = 0.05f;
        lastFrameTime = now;

        if (burstLife > 0f) {
            burstLife -= dt * 2f;
            if (burstLife < 0f) burstLife = 0f;
        }

        float cx = targetRect.centerX();
        float cy = targetRect.centerY();
        float r = Math.min(targetRect.width(), targetRect.height()) * 0.44f;

        // 1. Малювання активної техніки на Canvas
        renderer.drawTechnique(canvas, currentTechnique, cx, cy, r, now, burstLife, !isMotionEnabled);

        // 2. Рамка фокусу з куточками Конохи
        paint.reset();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(COLOR_ORANGE);
        paint.setStrokeWidth(3f);

        float pad = 4f;
        ringRect.set(targetRect.left - pad, targetRect.top - pad, targetRect.right + pad, targetRect.bottom + pad);

        // Куточки
        float cornerLen = 14f;
        // TL
        canvas.drawLine(ringRect.left, ringRect.top, ringRect.left + cornerLen, ringRect.top, paint);
        canvas.drawLine(ringRect.left, ringRect.top, ringRect.left, ringRect.top + cornerLen, paint);
        // TR
        canvas.drawLine(ringRect.right - cornerLen, ringRect.top, ringRect.right, ringRect.top, paint);
        canvas.drawLine(ringRect.right, ringRect.top, ringRect.right, ringRect.top + cornerLen, paint);
        // BL
        canvas.drawLine(ringRect.left, ringRect.bottom - cornerLen, ringRect.left, ringRect.bottom, paint);
        canvas.drawLine(ringRect.left, ringRect.bottom, ringRect.left + cornerLen, ringRect.bottom, paint);
        // BR
        canvas.drawLine(ringRect.right - cornerLen, ringRect.bottom, ringRect.right, ringRect.bottom, paint);
        canvas.drawLine(ringRect.right, ringRect.bottom - cornerLen, ringRect.right, ringRect.bottom, paint);
    }
}
