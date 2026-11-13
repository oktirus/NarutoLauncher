package ua.konoha.tv.nativepreview.views;

import android.content.Context;
import android.graphics.*;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityNodeInfo;
import ua.konoha.tv.nativepreview.R;

import ua.konoha.tv.nativepreview.game.RunnerHudLayout;
import ua.konoha.tv.nativepreview.game.RunnerPhysics;
import ua.konoha.tv.nativepreview.prefs.PreferencesManager;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class RunnerGameView extends View {
    private final PreferencesManager prefs;
    private final RunnerPhysics runner;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Map<String, Bitmap> bitmaps = new HashMap<>();
    private final java.util.Set<String> pendingBitmaps = new java.util.HashSet<>();
    private final java.util.concurrent.ThreadPoolExecutor bitmapExecutor = new java.util.concurrent.ThreadPoolExecutor(
            1, 1, 0L, java.util.concurrent.TimeUnit.MILLISECONDS, new java.util.concurrent.LinkedBlockingQueue<>());
    private final Handler bitmapHandler = new Handler(Looper.getMainLooper());
    private volatile long bitmapGeneration;
    private volatile boolean disposed;
    private final Path shurikenPath = new Path();

    private final RunnerHudLayout hudLayout = new RunnerHudLayout();

    private Runnable onExitListener;
    private Runnable onSoundListener;

    private long lastTime = 0;
    private boolean isPausedByLifecycle = false;

    private final Runnable tickRunnable = new Runnable() {
        @Override
        public void run() {
            if ("running".equals(runner.status) && !isPausedByLifecycle && isAttachedToWindow()
                    && getWindowVisibility() == VISIBLE && isShown()) {
                long now = System.currentTimeMillis();
                double dt = lastTime > 0 ? (now - lastTime) / 1000.0 : 0.016;
                if (dt > 0.05) dt = 0.05;
                lastTime = now;

                String before = runner.status;
                runner.step(dt);
                if ("running".equals(before) && "over".equals(runner.status)) {
                    saveBestScore();
                    if (onSoundListener != null) onSoundListener.run();
                }
                invalidate();
                postOnAnimation(this);
            }
        }
    };

    public RunnerGameView(Context context, PreferencesManager prefs) {
        super(context);
        this.prefs = prefs;
        String hero = prefs.getRunnerHero();
        if (!"gamabunta".equals(hero)) hero = "naruto";
        this.runner = new RunnerPhysics(hero);
        setFocusable(true);
        setFocusableInTouchMode(true);
        setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        loadBitmaps();
    }

    public RunnerHudLayout getHudLayout() {
        return hudLayout;
    }

    public void updateHudRects(int w, int h) {
        hudLayout.update(w, h);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateHudRects(w, h);
    }

    public void setOnExitListener(Runnable listener) {
        this.onExitListener = listener;
    }

    public void setOnSoundListener(Runnable listener) {
        this.onSoundListener = listener;
    }

    private void loadBitmaps() {
        if (!bitmaps.containsKey("forest")) {
            loadSingleBitmap("forest", "game/konoha-forest.webp");
        }
        loadHeroBitmaps(runner.hero);
    }

    private void loadHeroBitmaps(String hero) {
        String motionKey = hero + "-motion";
        String slideKey = hero + "-slide";
        if (!bitmaps.containsKey(motionKey)) {
            loadSingleBitmap(motionKey, "game/game-" + hero + "-motion.webp");
        }
        if (!bitmaps.containsKey(slideKey)) {
            loadSingleBitmap(slideKey, "game/game-" + hero + "-slide.webp");
        }
    }

    private void unloadHeroBitmaps(String hero) {
        String motionKey = hero + "-motion";
        String slideKey = hero + "-slide";
        Bitmap m = bitmaps.remove(motionKey);
        if (m != null && !m.isRecycled()) m.recycle();
        Bitmap s = bitmaps.remove(slideKey);
        if (s != null && !s.isRecycled()) s.recycle();
    }

    private void loadSingleBitmap(String key, String path) {
        if (disposed || !pendingBitmaps.add(key)) return;
        final long generation = bitmapGeneration;
        bitmapExecutor.execute(() -> {
            if (disposed || generation != bitmapGeneration) return;
            Bitmap decoded;
            try (InputStream is = getContext().getAssets().open(path)) {
                decoded = BitmapFactory.decodeStream(is);
            } catch (Exception ignored) { decoded = null; }
            final Bitmap loaded = decoded;
            bitmapHandler.post(() -> {
                if (disposed || generation != bitmapGeneration ||
                        !("forest".equals(key) || key.startsWith(runner.hero + "-"))) {
                    if (loaded != null) loaded.recycle();
                    return;
                }
                pendingBitmaps.remove(key);
                if (loaded == null) return;
                Bitmap existing = bitmaps.get(key);
                if (existing != null && !existing.isRecycled()) loaded.recycle();
                else bitmaps.put(key, loaded);
                invalidate();
            });
        });
    }

    public void startGame() {
        if (disposed || isPausedByLifecycle) return;
        removeCallbacks(tickRunnable);
        if ("paused".equals(runner.status)) {
            runner.status = "running";
        } else {
            runner.start();
        }
        lastTime = System.currentTimeMillis();
        postOnAnimation(tickRunnable);
        invalidate();
    }

    public void pauseGame() {
        if ("running".equals(runner.status)) {
            runner.status = "paused";
            saveBestScore();
            removeCallbacks(tickRunnable);
            invalidate();
        }
    }

    public void togglePause() {
        if ("running".equals(runner.status)) pauseGame();
        else if ("paused".equals(runner.status)) startGame();
    }

    public void jump() {
        if (!"running".equals(runner.status)) {
            startGame();
            return;
        }
        if (runner.jump() && onSoundListener != null) {
            onSoundListener.run();
        }
        invalidate();
    }

    public void crouch() {
        if ("running".equals(runner.status)) {
            runner.crouch();
            invalidate();
        }
    }

    public void setHero(String hero) {
        if (!"ready".equals(runner.status) && !"over".equals(runner.status)) return;
        saveBestScore();
        if (!"gamabunta".equals(hero)) hero = "naruto";
        if (hero.equals(runner.hero)) return;
        String oldHero = runner.hero;
        bitmapGeneration++;
        bitmapExecutor.getQueue().clear();
        pendingBitmaps.clear();
        runner.setHero(hero);
        runner.reset();
        unloadHeroBitmaps(oldHero);
        loadBitmaps();
        prefs.setRunnerHero(hero);
        invalidate();
    }

    public void toggleHero() {
        setHero("naruto".equals(runner.hero) ? "gamabunta" : "naruto");
    }

    private void saveBestScore() {
        int score = runner.getScore();
        prefs.setRunnerBest(runner.hero, score);
    }

    public void onPauseLifecycle() {
        isPausedByLifecycle = true;
        pauseGame();
        clearCache();
    }

    public void onResumeLifecycle() {
        if (disposed) return;
        isPausedByLifecycle = false;
        if (bitmaps.isEmpty()) {
            loadBitmaps();
        }
        if ("running".equals(runner.status)) {
            runner.status = "paused";
        }
        invalidate();
    }

    public void release() {
        saveBestScore();
        isPausedByLifecycle = true;
        disposed = true;
        clearCache();
        bitmapExecutor.shutdownNow();
    }

    private void clearCache() {
        bitmapGeneration++;
        bitmapExecutor.getQueue().clear();
        pendingBitmaps.clear();
        removeCallbacks(tickRunnable);
        for (Bitmap bmp : bitmaps.values()) {
            if (bmp != null && !bmp.isRecycled()) bmp.recycle();
        }
        bitmaps.clear();
    }

    @Override protected void onDetachedFromWindow() {
        removeCallbacks(tickRunnable);
        super.onDetachedFromWindow();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE) {
            saveBestScore();
            if (onExitListener != null) onExitListener.run();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_MENU || keyCode == KeyEvent.KEYCODE_P) {
            if (event.getRepeatCount() == 0) {
                togglePause();
            }
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            if ("ready".equals(runner.status) || "over".equals(runner.status)) {
                toggleHero();
                return true;
            }
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_CENTER
                || keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_SPACE) {
            jump();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            crouch();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    public void handlePauseButton() {
        if ("ready".equals(runner.status) || "over".equals(runner.status) || "paused".equals(runner.status)) {
            startGame();
        } else if ("running".equals(runner.status)) {
            pauseGame();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            performClick();
            return true;
        }
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            requestFocus();
            RunnerHudLayout.Action action = hudLayout.resolveAction(event.getX(), event.getY(), getHeight());
            switch (action) {
                case EXIT:
                    saveBestScore();
                    if (onExitListener != null) onExitListener.run();
                    return true;
                case PAUSE_TOGGLE:
                    handlePauseButton();
                    return true;
                case HERO_TOGGLE:
                    if ("ready".equals(runner.status) || "over".equals(runner.status)) {
                        toggleHero();
                    }
                    return true;
                case CROUCH:
                    crouch();
                    return true;
                case JUMP:
                    jump();
                    return true;
                default:
                    return false;
            }
        }
        return super.onTouchEvent(event);
    }

    @Override public boolean performClick() {
        super.performClick();
        return true;
    }

    @Override public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setContentDescription("Шлях шинобі · " + ("naruto".equals(runner.hero) ? "Наруто" : "Гамабунта"));
        info.addAction(new AccessibilityNodeInfo.AccessibilityAction(R.id.runner_jump, "Стрибок"));
        info.addAction(new AccessibilityNodeInfo.AccessibilityAction(R.id.runner_slide, "Підкат"));
        info.addAction(new AccessibilityNodeInfo.AccessibilityAction(R.id.runner_pause,
                "running".equals(runner.status) ? "Пауза" : "Грати"));
        info.addAction(new AccessibilityNodeInfo.AccessibilityAction(R.id.runner_exit, "Вийти з гри"));
        if ("ready".equals(runner.status) || "over".equals(runner.status)) {
            info.addAction(new AccessibilityNodeInfo.AccessibilityAction(R.id.runner_hero, "Змінити героя"));
        }
    }

    @Override public boolean performAccessibilityAction(int action, Bundle arguments) {
        if (action == R.id.runner_jump || action == AccessibilityNodeInfo.ACTION_CLICK) jump();
        else if (action == R.id.runner_slide) crouch();
        else if (action == R.id.runner_pause) handlePauseButton();
        else if (action == R.id.runner_hero) toggleHero();
        else if (action == R.id.runner_exit) {
            saveBestScore();
            if (onExitListener != null) onExitListener.run();
        } else return super.performAccessibilityAction(action, arguments);
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;

        // Масштабуємо з віртуальних 1280x540 до розмірів цього View
        float scaleX = (float) w / (float) RunnerPhysics.W;
        float scaleY = (float) h / (float) RunnerPhysics.H;

        canvas.save();
        canvas.scale(scaleX, scaleY);

        renderGameContent(canvas);

        canvas.restore();

        // HUD / Текст інтерфейсу поверх
        renderOverlayHUD(canvas, w, h);
    }

    private void renderGameContent(Canvas canvas) {
        // 1. Фон лісу Конохи
        Bitmap bg = bitmaps.get("forest");
        if (bg != null && !bg.isRecycled()) {
            canvas.drawBitmap(bg, null, new RectF(0, 0, (float) RunnerPhysics.W, (float) RunnerPhysics.H), paint);
        } else {
            paint.setColor(Color.parseColor("#425957"));
            canvas.drawRect(0, 0, (float) RunnerPhysics.W, (float) RunnerPhysics.H, paint);
        }

        // 2. Дорога
        paint.setColor(Color.parseColor("#977948"));
        canvas.drawRect(0, (float) RunnerPhysics.GROUND, (float) RunnerPhysics.W, (float) RunnerPhysics.H, paint);
        paint.setColor(Color.parseColor("#dac18c"));
        canvas.drawRect(0, (float) RunnerPhysics.GROUND, (float) RunnerPhysics.W, (float) RunnerPhysics.GROUND + 4, paint);

        // Текстура дороги (рухомі штрихи)
        paint.setColor(Color.parseColor("#342d25"));
        paint.setAlpha(120);
        double d = runner.distance;
        for (int k = 0; k < 40; k++) {
            float x = (float) (((k * 91 - d * 1.02) % RunnerPhysics.W + RunnerPhysics.W) % RunnerPhysics.W);
            float y = (float) (RunnerPhysics.GROUND + 15 + (k % 4) * 21);
            canvas.drawRect(x, y, x + 16, y + 2, paint);
        }

        // 3. Перешкоди
        for (RunnerPhysics.Obstacle o : runner.obstacles) {
            renderObstacle(canvas, o);
        }

        // 4. Тінь героя
        boolean gama = "gamabunta".equals(runner.hero);
        paint.setColor(Color.BLACK);
        paint.setAlpha(60);
        float shadowW = gama ? 58f : 35f;
        canvas.drawOval(new RectF(211 - shadowW, (float) RunnerPhysics.GROUND - 4, 211 + shadowW, (float) RunnerPhysics.GROUND + 10), paint);

        // 5. Герой (анімований спрайт)
        RunnerPhysics.Pose pose = RunnerPhysics.poseFor(runner);
        boolean isSlide = "slide".equals(pose.mode);
        String bmpKey = runner.hero + (isSlide ? "-slide" : "-motion");
        Bitmap heroBmp = bitmaps.get(bmpKey);

        if (heroBmp != null && !heroBmp.isRecycled()) {
            int cell = 256; // Runtime спрайт 256px
            int cols = isSlide ? 2 : 4;
            int col = pose.frame % cols;
            int row = pose.frame / cols;

            Rect src = new Rect(col * cell, row * cell, (col + 1) * cell, (row + 1) * cell);

            float drawSize = isSlide ? (gama ? 145.615f : 142f) : (gama ? 142f : 116f);
            float drawX = (float) (gama ? 222 : 207);
            float drawY = (float) (RunnerPhysics.GROUND - runner.y + pose.hop);

            float baselineOffset = drawSize * (249.0f / 256.0f);
            RectF dst = new RectF(drawX - drawSize / 2, drawY - baselineOffset, drawX + drawSize / 2, drawY + (drawSize - baselineOffset));

            paint.reset();
            paint.setAntiAlias(true);
            paint.setFilterBitmap(true);
            paint.setAlpha(255);
            canvas.drawBitmap(heroBmp, src, dst, paint);
        }

        // 6. Пил під ногами під час бігу
        if ("running".equals(runner.status) && runner.y <= 1 && runner.duck <= 0) {
            paint.setColor(Color.parseColor("#ddcba2"));
            paint.setAlpha(120);
            for (int k = 0; k < 5; k++) {
                double age = (runner.elapsed * 2.0 + k * 0.18) % 1.0;
                float px = (float) (170 - age * 50);
                float py = (float) (RunnerPhysics.GROUND - 3 - age * 8);
                canvas.drawCircle(px, py, (float) (3 + age * 5), paint);
            }
        }
    }

    private void renderObstacle(Canvas canvas, RunnerPhysics.Obstacle o) {
        if ("shuriken".equals(o.kind)) {
            canvas.save();
            canvas.translate((float) (o.x + o.w / 2), (float) (o.y + o.h / 2));
            canvas.rotate((float) (runner.elapsed * 500.0));
            paint.setColor(Color.parseColor("#bfd0cf"));
            paint.setStyle(Paint.Style.FILL_AND_STROKE);

            shurikenPath.reset();
            for (int k = 0; k < 8; k++) {
                double a = k * Math.PI / 4;
                double r = (k % 2 == 1) ? 5.0 : Math.min(o.w, o.h) / 2.0;
                float px = (float) (Math.cos(a) * r);
                float py = (float) (Math.sin(a) * r);
                if (k == 0) shurikenPath.moveTo(px, py);
                else shurikenPath.lineTo(px, py);
            }
            shurikenPath.close();
            canvas.drawPath(shurikenPath, paint);
            paint.setColor(Color.parseColor("#28443d"));
            canvas.drawCircle(0, 0, 3, paint);
            canvas.restore();
        } else if ("log".equals(o.kind)) {
            paint.setColor(Color.parseColor("#785a37"));
            paint.setStyle(Paint.Style.FILL);
            canvas.drawRect((float) o.x, (float) o.y, (float) (o.x + o.w), (float) (o.y + o.h), paint);

            paint.setColor(Color.parseColor("#cbab70"));
            canvas.drawOval(new RectF((float) (o.x + o.w - 12), (float) o.y, (float) (o.x + o.w), (float) (o.y + o.h)), paint);
        } else {
            // Training stake
            paint.setColor(Color.parseColor("#a98a54"));
            paint.setStyle(Paint.Style.FILL);
            canvas.drawRect((float) (o.x + 6), (float) o.y, (float) (o.x + o.w - 6), (float) (o.y + o.h), paint);

            paint.setColor(Color.parseColor("#dfc58d"));
            canvas.drawRect((float) o.x, (float) (o.y + 12), (float) (o.x + o.w), (float) (o.y + 21), paint);
            canvas.drawRect((float) o.x, (float) (o.y + 45), (float) (o.x + o.w), (float) (o.y + 54), paint);
        }
    }

    private void renderOverlayHUD(Canvas canvas, int w, int h) {
        hudLayout.update(w, h);

        paint.reset();
        paint.setAntiAlias(true);

        // Панель статусу (в компактному режимі у власному рядку під кнопками)
        paint.setColor(Color.WHITE);
        paint.setTextSize(hudLayout.isCompact ? 18f : 24f);
        paint.setTypeface(Typeface.DEFAULT_BOLD);

        String scoreStr = String.format("Шлях: %05d м", runner.getScore());
        String bestStr = String.format("Рекорд: %05d м", Math.max(runner.getScore(), prefs.getRunnerBest(runner.hero)));
        if (hudLayout.isCompact) {
            canvas.drawText(scoreStr + " · " + bestStr, hudLayout.scoreBounds.left, hudLayout.scoreBounds.centerY() + 6, paint);
        } else {
            canvas.drawText(scoreStr, hudLayout.scoreBounds.left, hudLayout.scoreBounds.top + 25, paint);
            canvas.drawText(bestStr, hudLayout.scoreBounds.left, hudLayout.scoreBounds.top + 60, paint);
        }

        // 1. Кнопка виходу (Exit)
        paint.setColor(Color.parseColor("#441f1a"));
        drawBounds(canvas, hudLayout.exitBounds, 8f);
        paint.setColor(Color.WHITE);
        paint.setTextSize(18f);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("Вихід", hudLayout.exitBounds.centerX(), hudLayout.exitBounds.centerY() + 6, paint);

        // 2. Кнопка паузи (Pause / Play)
        paint.setColor(Color.parseColor("#1b3944"));
        drawBounds(canvas, hudLayout.pauseBounds, 8f);
        paint.setColor(Color.WHITE);
        String pauseText = "running".equals(runner.status) ? "Пауза" : "Грати";
        canvas.drawText(pauseText, hudLayout.pauseBounds.centerX(), hudLayout.pauseBounds.centerY() + 6, paint);

        // 3. Кнопка героя (Hero)
        String heroName = "naruto".equals(runner.hero) ? "Наруто" : "Гамабунта";
        paint.setColor(Color.parseColor("#153745"));
        drawBounds(canvas, hudLayout.heroBounds, 8f);
        paint.setColor(Color.parseColor("#f2c576"));
        paint.setTextSize(hudLayout.isCompact ? 14f : 18f);
        canvas.drawText("Герой: " + heroName, hudLayout.heroBounds.centerX(), hudLayout.heroBounds.centerY() + 6, paint);
        paint.setTextAlign(Paint.Align.LEFT);

        // Повідомлення стану
        if (!"running".equals(runner.status)) {
            paint.setColor(Color.parseColor("#cc000000"));
            canvas.drawRect(w * 0.2f, h * 0.25f, w * 0.8f, h * 0.75f, paint);

            paint.setColor(Color.parseColor("#f9eaca"));
            paint.setTextSize(hudLayout.isCompact ? 28f : 42f);
            paint.setTextAlign(Paint.Align.CENTER);

            String title;
            String sub;
            if ("over".equals(runner.status)) {
                title = "Забіг завершено!";
                sub = "Подолано: " + runner.getScore() + " м · Натисни OK щоб повторити";
            } else if ("paused".equals(runner.status)) {
                title = "Пауза";
                sub = "Натисни OK або P щоб продовжити";
            } else {
                title = "Шлях шинобі: Ліс кличе";
                sub = "OK / ↑ — Стрибок | ↓ — Підкат | Menu — Пауза | Back — Вихід";
            }

            canvas.drawText(title, w / 2f, h * 0.45f, paint);
            paint.setTextSize(hudLayout.isCompact ? 16f : 22f);
            paint.setColor(Color.WHITE);
            canvas.drawText(sub, w / 2f, h * 0.58f, paint);
            paint.setTextAlign(Paint.Align.LEFT);
        }
    }

    private void drawBounds(Canvas canvas, RunnerHudLayout.Bounds b, float radius) {
        canvas.drawRoundRect(b.left, b.top, b.right, b.bottom, radius, radius, paint);
    }
}
