package ua.konoha.tv.nativepreview.views;

import android.content.Context;
import android.graphics.*;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Chakra Focus overlay drawing the active technique aura centered on .app-emblem.
 * Eliminates legacy rectangular orange bracket lines.
 * Runs on a continuous 60 FPS frame loop with smooth lerping and motion trails.
 */
public class ChakraFocusView extends View {
    private final TechniqueRenderer renderer;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private String currentTechnique = "rasengan";
    private View targetView = null;
    private final RectF targetRect = new RectF();
    private final int[] targetLocation = new int[2];
    private final int[] overlayLocation = new int[2];

    private boolean isMotionEnabled = true;
    private boolean isActivityPaused = false;
    private boolean isDisposed = false;
    private boolean isListenerRegistered = false;
    private float burstLife = 0f;
    private long lastFrameTime = 0;
    private long animationStartTime = 0;

    // Smooth focus tracking coordinates
    private float currentCx = -1f;
    private float currentCy = -1f;
    private float currentR = 0f;
    private float targetCx = -1f;
    private float targetCy = -1f;
    private float targetR = 0f;
    private float currentAlpha = 0f;

    private static class Trail {
        float ax, ay, bx, by;
        float life;
        String technique;
    }
    private final List<Trail> activeTrails = new ArrayList<>();

    private final TechniqueRenderer.OnBitmapLoadedListener loadListener = name -> {
        if (name == null) return;
        boolean matches = name.equals(currentTechnique)
                || ("gamabunta".equals(currentTechnique) && name.startsWith("gamabunta"));
        if (matches) {
            postInvalidate();
        }
    };

    private final Runnable animationRunnable = new Runnable() {
        @Override
        public void run() {
            if (isDisposed || isActivityPaused) return;
            if (isAttachedToWindow() && getVisibility() == VISIBLE) {
                invalidate();
            }
            if (!isActivityPaused && isAttachedToWindow()) {
                postDelayed(this, 16);
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
        startAnimationLoop();
    }

    public boolean isPaused() {
        return isActivityPaused;
    }

    private void startAnimationLoop() {
        removeCallbacks(animationRunnable);
        if (!isActivityPaused && isAttachedToWindow()) {
            post(animationRunnable);
        }
    }

    public void setTechnique(String technique) {
        this.currentTechnique = technique != null ? technique : "rasengan";
        invalidate();
    }

    public void setMotionEnabled(boolean enabled) {
        this.isMotionEnabled = enabled;
        startAnimationLoop();
    }

    public void triggerBurst() {
        this.burstLife = 1.0f;
        invalidate();
    }

    public void setTargetView(View view) {
        if (this.targetView == view) return;
        View old = this.targetView;
        this.targetView = view;
        this.burstLife = 1.0f;

        if (view != null) {
            View emblem = findEmblemView(view);
            emblem.getLocationInWindow(targetLocation);
            getLocationInWindow(overlayLocation);
            float left = targetLocation[0] - overlayLocation[0];
            float top = targetLocation[1] - overlayLocation[1];
            float newTargetX = left + emblem.getWidth() / 2f;
            float newTargetY = top + emblem.getHeight() / 2f;
            float baseR = Math.min(emblem.getWidth(), emblem.getHeight()) * 0.45f;
            float newTargetR = baseR * getTechniqueScale(currentTechnique);

            if (currentCx > 0 && currentCy > 0 && isMotionEnabled) {
                float dist = (float) Math.hypot(newTargetX - currentCx, newTargetY - currentCy);
                if (dist > 10f) {
                    Trail t = new Trail();
                    t.ax = currentCx;
                    t.ay = currentCy;
                    t.bx = newTargetX;
                    t.by = newTargetY;
                    t.life = 0.45f;
                    t.technique = currentTechnique;
                    activeTrails.add(t);
                    if (activeTrails.size() > 6) activeTrails.remove(0);
                }
            } else {
                currentCx = newTargetX;
                currentCy = newTargetY;
                currentR = newTargetR;
            }

            targetCx = newTargetX;
            targetCy = newTargetY;
            targetR = newTargetR;
        }

        if ("gamabunta".equals(currentTechnique) && renderer != null && old != null && view != null) {
            old.getLocationInWindow(targetLocation);
            getLocationInWindow(overlayLocation);
            float fromX = targetLocation[0] - overlayLocation[0] + old.getWidth() / 2f;
            float fromY = targetLocation[1] - overlayLocation[1] + old.getHeight() / 2f;

            view.getLocationInWindow(targetLocation);
            float toX = targetLocation[0] - overlayLocation[0] + view.getWidth() / 2f;
            float toY = targetLocation[1] - overlayLocation[1] + view.getHeight() / 2f;

            long now = android.os.SystemClock.uptimeMillis() - animationStartTime;
            renderer.triggerGamabuntaJump(new PointF(fromX, fromY), new PointF(toX, toY), toX >= fromX ? 1 : -1, now);
        }

        startAnimationLoop();
        invalidate();
    }

    private View findEmblemView(View root) {
        if (root instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) root;
            for (int i = 0; i < vg.getChildCount(); i++) {
                View child = vg.getChildAt(i);
                if (child instanceof ImageView) {
                    return child;
                }
            }
        }
        return root;
    }

    public View getTargetView() {
        return targetView;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        isDisposed = false;
        registerBitmapListener();
        startAnimationLoop();
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
        if (visibility == VISIBLE) startAnimationLoop();
        else removeCallbacks(animationRunnable);
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility == VISIBLE) startAnimationLoop();
        else removeCallbacks(animationRunnable);
    }

    private float getTechniqueScale(String id) {
        if ("gamabunta".equals(id)) return 1.0f;
        switch (id != null ? id : "") {
            case "summoning":
            case "hiraishin":
                return 1.40f;
            case "sharingan":
                return 1.38f;
            case "rasengan":
            case "amaterasu":
                return 1.36f;
            case "kurama":
            case "fireball":
                return 1.34f;
            case "chidori":
            case "rasenshuriken":
                return 1.32f;
            default:
                return 1.34f;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        long uptime = android.os.SystemClock.uptimeMillis();
        if (animationStartTime == 0) {
            animationStartTime = uptime;
        }
        long now = uptime - animationStartTime;
        float dt = lastFrameTime > 0 ? (uptime - lastFrameTime) / 1000f : 0.016f;
        if (dt > 0.05f) dt = 0.05f;
        lastFrameTime = uptime;

        if (burstLife > 0f) {
            burstLife -= dt * 2f;
            if (burstLife < 0f) burstLife = 0f;
        }

        // Draw motion trails between cards
        if (renderer != null && isMotionEnabled) {
            for (int i = activeTrails.size() - 1; i >= 0; i--) {
                Trail tr = activeTrails.get(i);
                tr.life -= dt;
                if (tr.life <= 0f) {
                    activeTrails.remove(i);
                } else {
                    renderer.drawTrail(canvas, tr.technique, tr.ax, tr.ay, tr.bx, tr.by, tr.life, now / 1000.0);
                }
            }
        }

        if (targetView != null && targetView.isAttachedToWindow() && targetView.getVisibility() == VISIBLE) {
            View emblem = findEmblemView(targetView);
            if (emblem.getWidth() > 0 && emblem.getHeight() > 0) {
                emblem.getLocationInWindow(targetLocation);
                getLocationInWindow(overlayLocation);
                float left = targetLocation[0] - overlayLocation[0];
                float top = targetLocation[1] - overlayLocation[1];
                targetCx = left + emblem.getWidth() / 2f;
                targetCy = top + emblem.getHeight() / 2f;
                float baseR = Math.min(emblem.getWidth(), emblem.getHeight()) * 0.45f;
                targetR = baseR * getTechniqueScale(currentTechnique);
            }
        }

        if (targetCx > 0 && targetCy > 0) {
            if (currentCx < 0) {
                currentCx = targetCx;
                currentCy = targetCy;
                currentR = targetR;
            } else if (isMotionEnabled) {
                // Smooth glide tracking
                currentCx += (targetCx - currentCx) * 0.35f;
                currentCy += (targetCy - currentCy) * 0.35f;
                currentR += (targetR - currentR) * 0.35f;
            } else {
                currentCx = targetCx;
                currentCy = targetCy;
                currentR = targetR;
            }

            float targetAlphaVal = (targetView != null && targetView.isAttachedToWindow()) ? 1.0f : 0.0f;
            currentAlpha += (targetAlphaVal - currentAlpha) * 0.25f;

            if (currentAlpha > 0.02f && renderer != null) {
                renderer.drawTechnique(canvas, currentTechnique, currentCx, currentCy, currentR, now, burstLife, !isMotionEnabled);
            }
        }
    }
}
