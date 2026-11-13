package ua.konoha.tv.nativepreview.views;

import android.content.Context;
import android.graphics.*;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import ua.konoha.tv.nativepreview.model.CatalogData;
import ua.konoha.tv.nativepreview.model.Scene;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VillageBackgroundView extends View {
    private static class Particle {
        float x, y, size, phase, speed;
    }

    private final List<Particle> particles = new ArrayList<>(28);
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path leafPath = new Path();
    private final RectF dstRect = new RectF();

    private LinearGradient leftScrim;
    private LinearGradient bottomScrim;
    private int lastW = 0, lastH = 0;
    private static final int COLOR_SCRIM_DARK = Color.parseColor("#d9071c27");
    private static final int COLOR_SCRIM_MID = Color.parseColor("#77071c27");
    private static final int COLOR_SCRIM_BOTTOM = Color.parseColor("#f0071c27");
    private static final int COLOR_DEFAULT_BG = Color.parseColor("#14323c");

    private Bitmap currentBitmap;
    private Bitmap nextBitmap;
    private long crossfadeStartTime = 0;
    private boolean isCrossfading = false;

    private final java.util.concurrent.ThreadPoolExecutor bgExecutor = new java.util.concurrent.ThreadPoolExecutor(
            1, 1, 0L, java.util.concurrent.TimeUnit.MILLISECONDS, new java.util.concurrent.LinkedBlockingQueue<>());
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private volatile long bgGeneration = 0;
    private volatile boolean disposed;
    private boolean activityPaused;

    private Scene currentScene;
    private boolean isMotionEnabled = true;
    private boolean isDecorPaused = false;
    private long lastFrameTime = 0;
    private long decorFrameTime = 0;

    private final Runnable animationRunnable = new Runnable() {
        @Override
        public void run() {
            if (!canRenderFrames() || !isMotionEnabled || isDecorPaused) return;
            invalidate();
            postOnAnimation(this);
        }
    };

    private final Runnable crossfadeRunnable = new Runnable() {
        @Override
        public void run() {
            if (!canRenderFrames()) return;
            if (isCrossfading) {
                invalidate();
                postOnAnimation(this);
            }
        }
    };

    public VillageBackgroundView(Context context) {
        super(context);
        initParticles();
    }

    private void initParticles() {
        particles.clear();
        for (int i = 0; i < 28; i++) {
            Particle p = new Particle();
            p.x = (float) ((i * 0.137 % 1.0) * 1920);
            p.y = (float) ((i * 0.283 % 1.0) * 1080);
            p.size = 3f + (i % 4) * 1.5f;
            p.phase = (float) (i * 1.7);
            p.speed = 15f + (i % 6) * 5f;
            particles.add(p);
        }
    }

    public void setScene(Scene scene, boolean animate) {
        if (disposed) return;
        this.currentScene = scene;
        if (scene == null) return;

        final long gen = ++bgGeneration;
        final String assetPath = scene.getImageAsset();
        bgExecutor.getQueue().clear();

        bgExecutor.execute(() -> {
            if (disposed || gen != bgGeneration) return;
            Bitmap decoded = loadSceneBitmap(assetPath);
            if (decoded != null) {
                mainHandler.post(() -> {
                    synchronized (VillageBackgroundView.this) {
                        if (disposed || gen != bgGeneration) {
                            decoded.recycle();
                            return;
                        }
                        applyNewSceneBitmap(decoded, animate);
                    }
                });
            }
        });
    }

    private void applyNewSceneBitmap(Bitmap newBmp, boolean animate) {
        if (!animate || currentBitmap == null || activityPaused) {
            if (currentBitmap != null && currentBitmap != newBmp) {
                currentBitmap.recycle();
            }
            if (nextBitmap != null && nextBitmap != newBmp) {
                nextBitmap.recycle();
            }
            currentBitmap = newBmp;
            nextBitmap = null;
            isCrossfading = false;
            removeCallbacks(crossfadeRunnable);
            invalidate();
            return;
        }

        if (nextBitmap != null && nextBitmap != newBmp) {
            nextBitmap.recycle();
        }
        nextBitmap = newBmp;
        isCrossfading = true;
        crossfadeStartTime = System.currentTimeMillis();
        removeCallbacks(crossfadeRunnable);
        postOnAnimation(crossfadeRunnable);
        invalidate();
    }

    private Bitmap loadSceneBitmap(String assetPath) {
        try (InputStream is = getContext().getAssets().open(assetPath)) {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inPreferredConfig = Bitmap.Config.RGB_565; // Економія пам'яті
            return BitmapFactory.decodeStream(is, null, opts);
        } catch (Exception e) {
            return null;
        }
    }

    public void setMotionEnabled(boolean enabled) {
        this.isMotionEnabled = enabled;
        checkAnimationState();
    }

    public void setDecorPaused(boolean paused) {
        this.isDecorPaused = paused;
        checkAnimationState();
    }

    public void pause() {
        activityPaused = true;
        removeCallbacks(animationRunnable);
        removeCallbacks(crossfadeRunnable);
        finishCrossfade();
    }

    public void resume() {
        activityPaused = false;
        checkAnimationState();
    }

    private boolean canRenderFrames() {
        return !disposed && !activityPaused && isAttachedToWindow()
                && getWindowVisibility() == VISIBLE && isShown();
    }

    private void finishCrossfade() {
        if (nextBitmap != null) {
            if (currentBitmap != null && currentBitmap != nextBitmap) currentBitmap.recycle();
            currentBitmap = nextBitmap;
            nextBitmap = null;
        }
        isCrossfading = false;
    }

    private void checkAnimationState() {
        removeCallbacks(animationRunnable);
        if (canRenderFrames() && isMotionEnabled && !isDecorPaused) {
            lastFrameTime = System.currentTimeMillis();
            postOnAnimation(animationRunnable);
        } else {
            invalidate();
        }
        removeCallbacks(crossfadeRunnable);
        if (isCrossfading && canRenderFrames()) postOnAnimation(crossfadeRunnable);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!disposed && currentBitmap == null && currentScene != null) setScene(currentScene, false);
        checkAnimationState();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeCallbacks(animationRunnable);
        removeCallbacks(crossfadeRunnable);
        bgGeneration++;
        bgExecutor.getQueue().clear();
        isCrossfading = false;
        if (currentBitmap != null) {
            currentBitmap.recycle();
            currentBitmap = null;
        }
        if (nextBitmap != null) {
            nextBitmap.recycle();
            nextBitmap = null;
        }
    }

    @Override protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        checkAnimationState();
    }

    @Override protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        checkAnimationState();
    }

    public void release() {
        disposed = true;
        bgGeneration++;
        pause();
        bgExecutor.shutdownNow();
        if (currentBitmap != null) currentBitmap.recycle();
        currentBitmap = null;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;

        long now = System.currentTimeMillis();
        if (decorFrameTime == 0 || (isMotionEnabled && !isDecorPaused)) decorFrameTime = now;
        float dt = lastFrameTime > 0 ? (now - lastFrameTime) / 1000f : 0.016f;
        if (dt > 0.05f) dt = 0.05f;
        lastFrameTime = now;

        // 1. Малювання фонового шару (з плавним переходом та паралаксом)
        if (isCrossfading && nextBitmap != null) {
            float progress = (now - crossfadeStartTime) / 1600f; // 1.6 секунди
            if (progress >= 1.0f) {
                if (currentBitmap != null && currentBitmap != nextBitmap) {
                    currentBitmap.recycle();
                }
                currentBitmap = nextBitmap;
                nextBitmap = null;
                isCrossfading = false;
                removeCallbacks(crossfadeRunnable);
                drawScaledBitmap(canvas, currentBitmap, w, h, 255, decorFrameTime);
            } else {
                drawScaledBitmap(canvas, currentBitmap, w, h, 255, decorFrameTime);
                drawScaledBitmap(canvas, nextBitmap, w, h, (int) (progress * 255), decorFrameTime);
            }
        } else if (currentBitmap != null) {
            drawScaledBitmap(canvas, currentBitmap, w, h, 255, decorFrameTime);
        } else {
            canvas.drawColor(COLOR_DEFAULT_BG);
        }

        // 2. Затемнення (Scrim matching prototype CSS)
        if (w != lastW || h != lastH || leftScrim == null) {
            lastW = w;
            lastH = h;
            leftScrim = new LinearGradient(
                    0, 0, w, 0,
                    new int[]{Color.parseColor("#c9071c27"), Color.parseColor("#77071c27"), Color.TRANSPARENT},
                    new float[]{0.0f, 0.31f, 0.64f}, Shader.TileMode.CLAMP
            );
            bottomScrim = new LinearGradient(
                    0, h, 0, h * 0.35f,
                    new int[]{Color.parseColor("#f0071c27"), Color.parseColor("#cc071c27"), Color.TRANSPARENT},
                    new float[]{0.0f, 0.37f, 1.0f},
                    Shader.TileMode.CLAMP
            );
        }
        paint.reset();
        paint.setShader(leftScrim);
        canvas.drawRect(0, 0, w, h, paint);
        paint.setShader(bottomScrim);
        canvas.drawRect(0, h * 0.35f, w, h, paint);
        paint.setShader(null);

        // 3. Частинки
        if (isMotionEnabled && currentScene != null) {
            String pMode = currentScene.getParticles();
            int[] palette = currentScene.getPalette();
            int color1 = palette != null && palette.length > 0 ? palette[0] : Color.WHITE;
            int color2 = palette != null && palette.length > 1 ? palette[1] : color1;

            paint.setStyle(Paint.Style.FILL);

            for (int i = 0; i < particles.size(); i++) {
                Particle p = particles.get(i);
                if (!isDecorPaused) {
                    p.x -= p.speed * dt * 2f;
                    if ("rain".equals(pMode)) {
                        p.y += dt * (180f + p.speed * 4f);
                    } else if ("snow".equals(pMode)) {
                        p.y += dt * (20f + p.speed * 0.5f);
                    } else {
                        p.y += Math.sin(now / 1700.0 + p.phase) * dt * 15f + dt * 6f;
                    }

                    if (p.x < -20) {
                        p.x = w + 20;
                        p.y = (float) ((p.phase * 0.111 % 1.0) * h);
                    }
                    if (p.y > h + 20) p.y = -20;
                }

                paint.setColor((i % 2 == 0) ? color1 : color2);
                paint.setAlpha((int) ((0.2f + Math.sin(decorFrameTime / 2000.0 + p.phase) * 0.1f) * 255));

                canvas.save();
                canvas.translate(p.x, p.y);

                if ("rain".equals(pMode)) {
                    canvas.rotate(-10f);
                    canvas.drawRect(0, 0, 1.5f, 10f + p.size, paint);
                } else if ("snow".equals(pMode) || "dust".equals(pMode) || "ember".equals(pMode)) {
                    canvas.drawCircle(0, 0, "snow".equals(pMode) ? p.size * 0.8f : p.size * 0.5f, paint);
                } else {
                    // Leaf shape
                    canvas.rotate((float) Math.toDegrees(Math.sin(decorFrameTime / 1300.0 + p.phase) * 1.5));
                    leafPath.reset();
                    leafPath.moveTo(-p.size, 0);
                    leafPath.quadTo(0, -p.size, p.size, 0);
                    leafPath.quadTo(0, p.size, -p.size, 0);
                    canvas.drawPath(leafPath, paint);
                }
                canvas.restore();
            }
        }
    }

    private void drawScaledBitmap(Canvas canvas, Bitmap bmp, int w, int h, int alpha, long now) {
        if (bmp == null || bmp.isRecycled()) return;
        paint.reset();
        paint.setAlpha(alpha);
        paint.setFilterBitmap(true);

        // Scale by 1.035x matching CSS scale(1.035) with subtle parallax
        float scale = Math.max((float) w / bmp.getWidth(), (float) h / bmp.getHeight()) * 1.035f;
        float scaledW = bmp.getWidth() * scale;
        float scaledH = bmp.getHeight() * scale;

        float extra = (scaledW - w) / 2f;
        float parallaxX = isMotionEnabled ? (float) (Math.sin(now / 8000.0) * extra * 0.5f) : 0f;
        float left = (w - scaledW) / 2f + parallaxX;
        float top = (h - scaledH) / 2f;

        dstRect.set(left, top, left + scaledW, top + scaledH);
        canvas.drawBitmap(bmp, null, dstRect, paint);
    }
}
