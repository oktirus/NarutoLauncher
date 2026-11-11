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
import ua.konoha.tv.nativepreview.theme.KonohaTheme;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Runner game stage and view reproducing Chromium's "Шлях шинобі".
 * Fixed 1280x540 virtual stage with gold frame, Caveat state banners,
 * and exact sprites/physics.
 */
public class RunnerGameView extends View {
    private final PreferencesManager prefs;
    private final RunnerPhysics runner;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint framePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Map<String, Bitmap> bitmaps = new HashMap<>();
    private final java.util.Set<String> pendingBitmaps = new java.util.HashSet<>();
    private final java.util.concurrent.ThreadPoolExecutor bitmapExecutor = new java.util.concurrent.ThreadPoolExecutor(
            1, 1, 0L, java.util.concurrent.TimeUnit.MILLISECONDS, new java.util.concurrent.LinkedBlockingQueue<>());
    private final Handler bitmapHandler = new Handler(Looper.getMainLooper());
    private volatile long bitmapGeneration;
    private volatile boolean disposed;
    private final Path shurikenPath = new Path();
    private final RectF stageRect = new RectF();

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
                postDelayed(this, 16);
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
        framePaint.setStyle(Paint.Style.STROKE);
        framePaint.setStrokeWidth(KonohaTheme.dp(context, 1.5f));
        framePaint.setColor(Color.parseColor("#66d6b67c"));
        loadBitmaps();
    }

    public void setOnExitListener(Runnable listener) {
        this.onExitListener = listener;
    }

    public void setOnSoundListener(Runnable listener) {
        this.onSoundListener = listener;
    }

    private void loadBitmaps() {
        loadBitmapAsync("forest", "game/konoha-forest.webp");
        loadHeroBitmaps(runner.hero);
    }

    private void loadHeroBitmaps(String hero) {
        loadBitmapAsync(hero + "-motion", "game/game-" + hero + "-motion.webp");
        loadBitmapAsync(hero + "-slide", "game/game-" + hero + "-slide.webp");
    }

    private void unloadHeroBitmaps(String hero) {
        Bitmap motion = bitmaps.remove(hero + "-motion");
        if (motion != null && !motion.isRecycled()) motion.recycle();
        Bitmap slide = bitmaps.remove(hero + "-slide");
        if (slide != null && !slide.isRecycled()) slide.recycle();
    }

    private void loadBitmapAsync(final String key, final String assetPath) {
        if (disposed || bitmaps.containsKey(key)) return;
        synchronized (pendingBitmaps) {
            if (!pendingBitmaps.add(key)) return;
        }

        final long gen = bitmapGeneration;
        bitmapExecutor.execute(() -> {
            Bitmap bmp = null;
            try (InputStream is = getContext().getAssets().open(assetPath)) {
                bmp = BitmapFactory.decodeStream(is);
            } catch (Exception ignored) {}

            final Bitmap finalBmp = bmp;
            bitmapHandler.post(() -> {
                synchronized (pendingBitmaps) {
                    pendingBitmaps.remove(key);
                }
                if (disposed || gen != bitmapGeneration || finalBmp == null) {
                    if (finalBmp != null) finalBmp.recycle();
                    return;
                }
                bitmaps.put(key, finalBmp);
                invalidate();
            });
        });
    }

    public void startGame() {
        if ("running".equals(runner.status)) return;
        runner.start();
        lastTime = System.currentTimeMillis();
        removeCallbacks(tickRunnable);
        post(tickRunnable);
        invalidate();
    }

    public void pauseGame() {
        if ("running".equals(runner.status)) {
            runner.status = "paused";
            removeCallbacks(tickRunnable);
            invalidate();
        }
    }

    public void togglePause() {
        if ("running".equals(runner.status)) pauseGame();
        else if ("paused".equals(runner.status)) startGame();
    }

    public void jump() {
        if ("ready".equals(runner.status) || "over".equals(runner.status)) {
            startGame();
            return;
        }
        if ("paused".equals(runner.status)) {
            startGame();
            return;
        }
        runner.jump();
        invalidate();
    }

    public void crouch() {
        if ("running".equals(runner.status)) {
            runner.crouch();
            invalidate();
        }
    }

    public void setHero(String hero) {
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

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            performClick();
            return true;
        }
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            requestFocus();
            float x = event.getX();
            float y = event.getY();

            // Check if tapped hero buttons or action button
            if (y < KonohaTheme.dp(getContext(), 80)) {
                if (x < getWidth() * 0.3f && ("ready".equals(runner.status) || "over".equals(runner.status))) {
                    toggleHero();
                    return true;
                } else if (x > getWidth() * 0.85f) {
                    saveBestScore();
                    if (onExitListener != null) onExitListener.run();
                    return true;
                }
            } else if (y > getHeight() - KonohaTheme.dp(getContext(), 60)) {
                if (x < getWidth() * 0.5f) crouch();
                else jump();
                return true;
            } else {
                jump();
                return true;
            }
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
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

    @Override
    public boolean performAccessibilityAction(int action, Bundle arguments) {
        if (action == R.id.runner_jump || action == AccessibilityNodeInfo.ACTION_CLICK) jump();
        else if (action == R.id.runner_slide) crouch();
        else if (action == R.id.runner_pause) togglePause();
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

        // Dark background
        paint.setColor(KonohaTheme.COLOR_BG);
        canvas.drawRect(0, 0, w, h, paint);

        // Header toolbar:
        renderHeader(canvas, w);

        // Compute 1280x540 aspect stage rect
        float topH = KonohaTheme.dp(getContext(), 84);
        float bottomH = KonohaTheme.dp(getContext(), 56);
        float availH = h - topH - bottomH;
        float availW = w - KonohaTheme.dp(getContext(), 48);

        float stageW = availW;
        float stageH = stageW * (540f / 1280f);
        if (stageH > availH) {
            stageH = availH;
            stageW = stageH * (1280f / 540f);
        }

        float stageLeft = (w - stageW) / 2f;
        float stageTop = topH + (availH - stageH) / 2f;
        stageRect.set(stageLeft, stageTop, stageLeft + stageW, stageTop + stageH);

        // Render Stage
        canvas.save();
        canvas.clipRect(stageRect);
        canvas.translate(stageLeft, stageTop);
        float scale = stageW / (float) RunnerPhysics.W;
        canvas.scale(scale, scale);

        renderGameContent(canvas);

        if (!"running".equals(runner.status)) {
            renderStageOverlay(canvas);
        }

        canvas.restore();

        // Stage Golden Frame Border
        canvas.drawRect(stageRect, framePaint);

        // Footer
        renderFooter(canvas, w, h);
    }

    private void renderHeader(Canvas canvas, int w) {
        // Title: Шлях шинобі
        paint.reset();
        paint.setAntiAlias(true);
        paint.setTypeface(KonohaTheme.getCaveatBold(getContext()));
        paint.setTextSize(KonohaTheme.dp(getContext(), 32));
        paint.setColor(KonohaTheme.COLOR_PAPER);
        canvas.drawText("Шлях шинобі", KonohaTheme.dp(getContext(), 24), KonohaTheme.dp(getContext(), 40), paint);

        // Hero Tabs: [ Наруто ] [ Гамабунта ]
        boolean isNaruto = "naruto".equals(runner.hero);
        float tabY = KonohaTheme.dp(getContext(), 52);
        float tabH = KonohaTheme.dp(getContext(), 28);
        float tabW1 = KonohaTheme.dp(getContext(), 84);
        float tabW2 = KonohaTheme.dp(getContext(), 104);

        drawHeroTab(canvas, KonohaTheme.dp(getContext(), 24), tabY, tabW1, tabH, "Наруто", isNaruto);
        drawHeroTab(canvas, KonohaTheme.dp(getContext(), 114), tabY, tabW2, tabH, "Гамабунта", !isNaruto);

        // Scores (Top right)
        paint.setTypeface(KonohaTheme.getNunitoBold(getContext()));
        paint.setTextSize(KonohaTheme.dp(getContext(), 11));
        paint.setColor(Color.parseColor("#9eb1b8"));
        canvas.drawText("Шлях", w - KonohaTheme.dp(getContext(), 320), tabY + 8, paint);
        canvas.drawText("Рекорд", w - KonohaTheme.dp(getContext(), 200), tabY + 8, paint);

        paint.setTextSize(KonohaTheme.dp(getContext(), 18));
        paint.setColor(KonohaTheme.COLOR_PAPER);
        String scoreStr = String.format(java.util.Locale.ROOT, "%05d", runner.getScore());
        String bestStr = String.format(java.util.Locale.ROOT, "%05d", Math.max(runner.getScore(), prefs.getRunnerBest(runner.hero)));
        canvas.drawText(scoreStr, w - KonohaTheme.dp(getContext(), 320), tabY + 26, paint);
        canvas.drawText(bestStr, w - KonohaTheme.dp(getContext(), 200), tabY + 26, paint);

        // Pause button
        drawPillButton(canvas, w - KonohaTheme.dp(getContext(), 100), tabY, KonohaTheme.dp(getContext(), 74), tabH,
                "running".equals(runner.status) ? "⏸ Пауза" : "▶ Старт");

        // Close button 'x'
        paint.setColor(KonohaTheme.COLOR_PAPER);
        paint.setStrokeWidth(2f);
        paint.setStyle(Paint.Style.STROKE);
        float cx = w - KonohaTheme.dp(getContext(), 18);
        float cy = KonohaTheme.dp(getContext(), 24);
        float s = KonohaTheme.dp(getContext(), 6);
        canvas.drawLine(cx - s, cy - s, cx + s, cy + s, paint);
        canvas.drawLine(cx - s, cy + s, cx + s, cy - s, paint);
    }

    private void drawHeroTab(Canvas canvas, float x, float y, float w, float h, String text, boolean active) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(active ? KonohaTheme.COLOR_ORANGE : Color.parseColor("#153745"));
        canvas.drawRoundRect(x, y, x + w, y + h, 4f, 4f, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(active ? KonohaTheme.COLOR_GOLD : Color.parseColor("#44dbbd7d"));
        paint.setStrokeWidth(1.5f);
        canvas.drawRoundRect(x, y, x + w, y + h, 4f, 4f, paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(active ? Color.parseColor("#102b38") : KonohaTheme.COLOR_PAPER);
        paint.setTypeface(KonohaTheme.getNunitoBold(getContext()));
        paint.setTextSize(KonohaTheme.dp(getContext(), 12));
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(text, x + w / 2f, y + h * 0.68f, paint);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawPillButton(Canvas canvas, float x, float y, float w, float h, String text) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#153745"));
        canvas.drawRoundRect(x, y, x + w, y + h, 4f, 4f, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.parseColor("#44dbbd7d"));
        paint.setStrokeWidth(1f);
        canvas.drawRoundRect(x, y, x + w, y + h, 4f, 4f, paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(KonohaTheme.COLOR_PAPER);
        paint.setTypeface(KonohaTheme.getNunito(getContext()));
        paint.setTextSize(KonohaTheme.dp(getContext(), 11));
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(text, x + w / 2f, y + h * 0.66f, paint);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void renderStageOverlay(Canvas canvas) {
        paint.reset();
        paint.setAntiAlias(true);

        // Translucent dark veil over stage
        paint.setColor(Color.parseColor("#b3071c27"));
        canvas.drawRect(0, 0, (float) RunnerPhysics.W, (float) RunnerPhysics.H, paint);

        String subtitle;
        String title;
        String desc;
        String cta;

        if ("over".equals(runner.status)) {
            subtitle = "Ще один шлях.";
            title = "Забіг завершено.";
            desc = "Подолано: " + runner.getScore() + " м · Рекорд: " + prefs.getRunnerBest(runner.hero) + " м";
            cta = "OK · Спробувати ще";
        } else if ("paused".equals(runner.status)) {
            subtitle = "Перепочинок.";
            title = "Зупинка в лісі.";
            desc = "Натисни OK або P щоб продовжити забіг.";
            cta = "OK · Продовжити";
        } else {
            subtitle = "Наруто · Ніколи не здаватися.";
            title = "Ліс кличе.";
            desc = "Перестрибуй колоди й тренувальні стовпи.\nРоби підкат під летючими сюрікенами.";
            cta = "OK · Рушай";
        }

        // Subtitle
        paint.setTypeface(KonohaTheme.getNunito(getContext()));
        paint.setColor(Color.parseColor("#d0d2c0"));
        paint.setTextSize(16f);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(subtitle, (float) RunnerPhysics.W / 2f, 185f, paint);

        // Big Caveat Title
        paint.setTypeface(KonohaTheme.getCaveatBold(getContext()));
        paint.setColor(KonohaTheme.COLOR_PAPER);
        paint.setTextSize(68f);
        paint.setShadowLayer(18f, 0, 4f, Color.parseColor("#99000000"));
        canvas.drawText(title, (float) RunnerPhysics.W / 2f, 260f, paint);

        // Description
        paint.setTypeface(KonohaTheme.getNunito(getContext()));
        paint.setColor(Color.parseColor("#d9d7bf"));
        paint.setTextSize(15f);
        String[] lines = desc.split("\n");
        for (int i = 0; i < lines.length; i++) {
            canvas.drawText(lines[i], (float) RunnerPhysics.W / 2f, 310f + i * 24f, paint);
        }

        // CTA Button (paper style)
        float btnW = 180f;
        float btnH = 46f;
        float btnX = ((float) RunnerPhysics.W - btnW) / 2f;
        float btnY = 375f;

        paint.setColor(KonohaTheme.COLOR_PAPER);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(btnX, btnY, btnX + btnW, btnY + btnH, 6f, 6f, paint);

        paint.setColor(Color.parseColor("#243e44"));
        paint.setTypeface(KonohaTheme.getNunitoBold(getContext()));
        paint.setTextSize(16f);
        canvas.drawText(cta, (float) RunnerPhysics.W / 2f, btnY + 28f, paint);

        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void renderFooter(Canvas canvas, int w, int h) {
        float footY = h - KonohaTheme.dp(getContext(), 18);
        paint.setTypeface(KonohaTheme.getNunito(getContext()));
        paint.setTextSize(KonohaTheme.dp(getContext(), 12));
        paint.setColor(Color.parseColor("#afc1b7"));
        canvas.drawText("Обери героя й починай тренування.", KonohaTheme.dp(getContext(), 24), footY, paint);

        drawPillButton(canvas, w - KonohaTheme.dp(getContext(), 190), footY - KonohaTheme.dp(getContext(), 16),
                KonohaTheme.dp(getContext(), 80), KonohaTheme.dp(getContext(), 24), "↓ Підкат");
        drawPillButton(canvas, w - KonohaTheme.dp(getContext(), 100), footY - KonohaTheme.dp(getContext(), 16),
                KonohaTheme.dp(getContext(), 80), KonohaTheme.dp(getContext(), 24), "↑ Стрибок");
    }

    private void renderGameContent(Canvas canvas) {
        // 1. Forest background
        Bitmap bg = bitmaps.get("forest");
        if (bg != null && !bg.isRecycled()) {
            canvas.drawBitmap(bg, null, new RectF(0, 0, (float) RunnerPhysics.W, (float) RunnerPhysics.H), paint);
        } else {
            paint.setColor(Color.parseColor("#425957"));
            canvas.drawRect(0, 0, (float) RunnerPhysics.W, (float) RunnerPhysics.H, paint);
        }

        // 2. Road
        paint.setColor(Color.parseColor("#977948"));
        canvas.drawRect(0, (float) RunnerPhysics.GROUND, (float) RunnerPhysics.W, (float) RunnerPhysics.H, paint);
        paint.setColor(Color.parseColor("#dac18c"));
        canvas.drawRect(0, (float) RunnerPhysics.GROUND, (float) RunnerPhysics.W, (float) RunnerPhysics.GROUND + 4, paint);

        // Road texture dashes
        paint.setColor(Color.parseColor("#342d25"));
        paint.setAlpha(120);
        double d = runner.distance;
        for (int k = 0; k < 40; k++) {
            float x = (float) (((k * 91 - d * 1.02) % RunnerPhysics.W + RunnerPhysics.W) % RunnerPhysics.W);
            float y = (float) (RunnerPhysics.GROUND + 15 + (k % 4) * 21);
            canvas.drawRect(x, y, x + 16, y + 2, paint);
        }

        // 3. Obstacles
        for (RunnerPhysics.Obstacle o : runner.obstacles) {
            renderObstacle(canvas, o);
        }

        // 4. Hero shadow
        boolean gama = "gamabunta".equals(runner.hero);
        paint.setColor(Color.BLACK);
        paint.setAlpha(60);
        float shadowW = gama ? 58f : 35f;
        canvas.drawOval(new RectF(211 - shadowW, (float) RunnerPhysics.GROUND - 4, 211 + shadowW, (float) RunnerPhysics.GROUND + 10), paint);

        // 5. Hero animated sprite
        RunnerPhysics.Pose pose = RunnerPhysics.poseFor(runner);
        boolean isSlide = "slide".equals(pose.mode);
        String bmpKey = runner.hero + (isSlide ? "-slide" : "-motion");
        Bitmap heroBmp = bitmaps.get(bmpKey);

        if (heroBmp != null && !heroBmp.isRecycled()) {
            int cell = 256;
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

        // 6. Running dust
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
}
