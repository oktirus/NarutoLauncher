package ua.konoha.tv.nativepreview.views;

import android.content.Context;
import android.graphics.*;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityNodeInfo;
import ua.konoha.tv.nativepreview.R;

import ua.konoha.tv.nativepreview.game.RunnerCue;
import ua.konoha.tv.nativepreview.game.RunnerPhysics;
import ua.konoha.tv.nativepreview.prefs.PreferencesManager;
import ua.konoha.tv.nativepreview.theme.KonohaTheme;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

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
    private final RectF narutoTab = new RectF();
    private final RectF gamabuntaTab = new RectF();
    private final RectF pauseButton = new RectF();
    private final RectF closeButton = new RectF();
    private final RectF slideButton = new RectF();
    private final RectF jumpButton = new RectF();
    private final RectF world = new RectF(0, 0, (float) RunnerPhysics.W, (float) RunnerPhysics.H);
    private final LinearGradient roadShade = new LinearGradient(0, (float) RunnerPhysics.GROUND,
            0, (float) RunnerPhysics.H, 0xff9c8051, 0xff594531, Shader.TileMode.CLAMP);
    private float scoreLeft;
    private float bestLeft;
    private float scoreTop;

    private Runnable onExitListener;
    private Consumer<RunnerCue> onSoundListener;
    private Runnable onStopSounds;

    private long lastTime = 0;
    private boolean isPausedByLifecycle = false;

    private final Runnable tickRunnable = new Runnable() {
        @Override
        public void run() {
            if ("running".equals(runner.status) && !isPausedByLifecycle && isAttachedToWindow()
                    && getWindowVisibility() == VISIBLE && isShown()) {
                long now = SystemClock.uptimeMillis();
                double dt = lastTime > 0 ? (now - lastTime) / 1000.0 : 0.016;
                if (dt > 0.05) dt = 0.05;
                lastTime = now;

                boolean airborne = runner.y > 0.0;
                int clearedBefore = runner.cleared;
                runner.step(dt);
                if ("over".equals(runner.status)) {
                    saveBestScore();
                    stopSounds();
                    playSound(RunnerCue.HIT);
                } else {
                    if (airborne && runner.y == 0.0) playSound(RunnerCue.LAND);
                    if (runner.cleared > clearedBefore) playSound(RunnerCue.CLEAR);
                }
                invalidate();
                if ("running".equals(runner.status)) postDelayed(this, 16);
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

    public void setSoundCallbacks(Consumer<RunnerCue> play, Runnable stop) {
        this.onSoundListener = play;
        this.onStopSounds = stop;
    }

    private void playSound(RunnerCue cue) {
        if (!disposed && !isPausedByLifecycle && isAttachedToWindow() && onSoundListener != null) {
            onSoundListener.accept(cue);
        }
    }

    private void stopSounds() {
        if (onStopSounds != null) onStopSounds.run();
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
        lastTime = SystemClock.uptimeMillis();
        removeCallbacks(tickRunnable);
        post(tickRunnable);
        playSound(RunnerCue.START);
        invalidate();
    }

    public void pauseGame() {
        if ("running".equals(runner.status)) {
            runner.status = "paused";
            removeCallbacks(tickRunnable);
            invalidate();
        }
        stopSounds();
    }

    public void togglePause() {
        if ("running".equals(runner.status)) pauseGame();
        else startGame();
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
        boolean fromGround = runner.y == 0.0;
        if (runner.jump() && fromGround) playSound(RunnerCue.JUMP);
        invalidate();
    }

    public void crouch() {
        if ("running".equals(runner.status)) {
            boolean startsMove = runner.y > 0.0 ? runner.vy < 400.0 : runner.duck == 0.0;
            runner.crouch();
            if (startsMove) playSound(RunnerCue.SLIDE);
            invalidate();
        }
    }

    public void setHero(String hero) {
        saveBestScore();
        if (!"gamabunta".equals(hero)) hero = "naruto";
        if (hero.equals(runner.hero)) return;
        stopSounds();
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
        stopSounds();
        onSoundListener = null;
        onStopSounds = null;
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
        stopSounds();
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
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateControlBounds(w, h);
    }

    private void updateControlBounds(int w, int h) {
        float inset = KonohaTheme.dp(getContext(), 24);
        float gap = KonohaTheme.dp(getContext(), 8);
        float right = w - inset;
        float tabY = KonohaTheme.dp(getContext(), 52);
        float tabBottom = tabY + KonohaTheme.dp(getContext(), 28);
        pauseButton.set(right - KonohaTheme.dp(getContext(), 80), tabY, right, tabBottom);
        float tabWidth = Math.max(0, pauseButton.left - inset - 2 * gap);
        float narutoWidth = Math.min(KonohaTheme.dp(getContext(), 84), tabWidth * 0.45f);
        float gamabuntaWidth = Math.min(KonohaTheme.dp(getContext(), 104), tabWidth - narutoWidth);
        narutoTab.set(inset, tabY, inset + narutoWidth, tabBottom);
        gamabuntaTab.set(narutoTab.right + gap, tabY, narutoTab.right + gap + gamabuntaWidth, tabBottom);
        float closeSize = KonohaTheme.dp(getContext(), 36);
        closeButton.set(right - closeSize, KonohaTheme.dp(getContext(), 8), right,
                KonohaTheme.dp(getContext(), 8) + closeSize);
        float scoreWidth = KonohaTheme.dp(getContext(), 78);
        bestLeft = pauseButton.left - gap - scoreWidth;
        scoreLeft = bestLeft - scoreWidth;
        boolean compact = scoreLeft < gamabuntaTab.right + gap;
        scoreTop = tabY;
        if (compact) {
            scoreLeft = inset;
            bestLeft = scoreLeft + scoreWidth;
            scoreTop = tabBottom + gap;
        }
        float headerBottom = Math.max(tabBottom, scoreTop + KonohaTheme.dp(getContext(), 31));
        float footBottom = h - KonohaTheme.dp(getContext(), 12);
        float footTop = footBottom - KonohaTheme.dp(getContext(), 28);
        float actionWidth = KonohaTheme.dp(getContext(), 86);
        jumpButton.set(right - actionWidth, footTop, right, footBottom);
        slideButton.set(jumpButton.left - gap - actionWidth, footTop, jumpButton.left - gap, footBottom);

        float top = headerBottom + gap;
        float height = Math.max(0, footTop - gap - top);
        float stageWidth = Math.max(0, Math.min(w - 2 * inset,
                height * (float) (RunnerPhysics.W / RunnerPhysics.H)));
        float stageHeight = stageWidth * (float) (RunnerPhysics.H / RunnerPhysics.W);
        float stageLeft = (w - stageWidth) / 2f;
        float stageTop = top + (height - stageHeight) / 2f;
        stageRect.set(stageLeft, stageTop, stageLeft + stageWidth, stageTop + stageHeight);
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

            if (closeButton.contains(x, y)) {
                saveBestScore();
                if (onExitListener != null) onExitListener.run();
            } else if (pauseButton.contains(x, y)) {
                togglePause();
            } else if (narutoTab.contains(x, y) || gamabuntaTab.contains(x, y)) {
                if ("ready".equals(runner.status) || "over".equals(runner.status)) {
                    setHero(narutoTab.contains(x, y) ? "naruto" : "gamabunta");
                }
            } else if (slideButton.contains(x, y)) {
                crouch();
            } else if (jumpButton.contains(x, y) || stageRect.contains(x, y)) {
                jump();
            }
            return true;
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
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(KonohaTheme.COLOR_BG);
        canvas.drawRect(0, 0, w, h, paint);

        // Header toolbar:
        renderHeader(canvas);
        if (stageRect.isEmpty()) return;

        // Render Stage
        canvas.save();
        canvas.clipRect(stageRect);
        canvas.translate(stageRect.left, stageRect.top);
        float scale = stageRect.width() / (float) RunnerPhysics.W;
        canvas.scale(scale, scale);

        renderGameContent(canvas);

        if (!"running".equals(runner.status)) {
            renderStageOverlay(canvas);
        }

        canvas.restore();

        // Stage Golden Frame Border
        canvas.drawRect(stageRect, framePaint);

        // Footer
        renderFooter(canvas);
    }

    private void renderHeader(Canvas canvas) {
        // Title: Шлях шинобі
        paint.reset();
        paint.setAntiAlias(true);
        paint.setTypeface(KonohaTheme.getCaveatBold(getContext()));
        paint.setTextSize(KonohaTheme.dp(getContext(), 32));
        paint.setColor(KonohaTheme.COLOR_PAPER);
        canvas.drawText("Шлях шинобі", KonohaTheme.dp(getContext(), 24), KonohaTheme.dp(getContext(), 40), paint);

        // Hero Tabs: [ Наруто ] [ Гамабунта ]
        boolean isNaruto = "naruto".equals(runner.hero);
        drawHeroTab(canvas, narutoTab.left, narutoTab.top, narutoTab.width(), narutoTab.height(), "Наруто", isNaruto);
        drawHeroTab(canvas, gamabuntaTab.left, gamabuntaTab.top, gamabuntaTab.width(), gamabuntaTab.height(), "Гамабунта", !isNaruto);

        // Scores (Top right)
        paint.setTypeface(KonohaTheme.getNunitoBold(getContext()));
        paint.setTextSize(KonohaTheme.dp(getContext(), 11));
        paint.setColor(Color.parseColor("#9eb1b8"));
        float labelBaseline = scoreTop + KonohaTheme.dp(getContext(), 10);
        canvas.drawText("Шлях", scoreLeft, labelBaseline, paint);
        canvas.drawText("Рекорд", bestLeft, labelBaseline, paint);

        paint.setTextSize(KonohaTheme.dp(getContext(), 18));
        paint.setColor(KonohaTheme.COLOR_PAPER);
        String scoreStr = String.format(java.util.Locale.ROOT, "%05d", runner.getScore());
        String bestStr = String.format(java.util.Locale.ROOT, "%05d", Math.max(runner.getScore(), prefs.getRunnerBest(runner.hero)));
        float valueBaseline = scoreTop + KonohaTheme.dp(getContext(), 28);
        canvas.drawText(scoreStr, scoreLeft, valueBaseline, paint);
        canvas.drawText(bestStr, bestLeft, valueBaseline, paint);

        // Pause button
        String pauseLabel = switch (runner.status) {
            case "running" -> "Пауза";
            case "paused" -> "Продовжити";
            default -> "Старт";
        };
        drawPillButton(canvas, pauseButton.left, pauseButton.top, pauseButton.width(), pauseButton.height(), pauseLabel);

        // Close button 'x'
        paint.setColor(KonohaTheme.COLOR_PAPER);
        paint.setStrokeWidth(2f);
        paint.setStyle(Paint.Style.STROKE);
        float cx = closeButton.centerX();
        float cy = closeButton.centerY();
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
        paint.setStyle(Paint.Style.FILL);
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

    private void renderFooter(Canvas canvas) {
        float left = KonohaTheme.dp(getContext(), 24);
        String hint = "ready".equals(runner.status)
                ? "← → герой  ·  OK почати" : "Menu — пауза  ·  Back — вихід";
        paint.setTypeface(KonohaTheme.getNunito(getContext()));
        paint.setTextSize(KonohaTheme.dp(getContext(), 12));
        paint.setColor(Color.parseColor("#afc1b7"));
        paint.setTextAlign(Paint.Align.LEFT);
        float maxWidth = slideButton.left - left - KonohaTheme.dp(getContext(), 8);
        if (paint.measureText(hint) > maxWidth) {
            hint = "ready".equals(runner.status) ? "OK — почати" : "Menu — пауза";
        }
        if (paint.measureText(hint) <= maxWidth) {
            float baseline = slideButton.centerY() - (paint.ascent() + paint.descent()) / 2f;
            canvas.drawText(hint, left, baseline, paint);
        }

        drawPillButton(canvas, slideButton.left, slideButton.top, slideButton.width(), slideButton.height(), "↓ Підкат");
        drawPillButton(canvas, jumpButton.left, jumpButton.top, jumpButton.width(), jumpButton.height(), "↑ Стрибок");
    }

    private void renderGameContent(Canvas canvas) {
        // 1. Forest background
        paint.setStyle(Paint.Style.FILL);
        paint.setAlpha(255);
        Bitmap bg = bitmaps.get("forest");
        if (bg != null && !bg.isRecycled()) {
            canvas.drawBitmap(bg, null, world, paint);
        } else {
            paint.setColor(Color.parseColor("#425957"));
            canvas.drawRect(0, 0, (float) RunnerPhysics.W, (float) RunnerPhysics.H, paint);
        }

        renderTrail(canvas);

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

    private void renderTrail(Canvas canvas) {
        float ground = (float) RunnerPhysics.GROUND;
        double distance = runner.distance;
        // Small lanterns and leaves sit behind the collision line.
        for (int i = 0; i < 4; i++) {
            float x = trailPosition(107 + i * 320, distance * 0.56);
            float top = ground - 44f - (i % 2) * 12f;
            paint.setColor(0xff493f2d);
            canvas.drawRect(x - 2, top, x + 2, ground, paint);
            paint.setColor(0x33f4ae5a);
            canvas.drawCircle(x, top, 15, paint);
            paint.setColor(0xffe7af6c);
            canvas.drawRoundRect(x - 5, top - 7, x + 5, top + 7, 3, 3, paint);
            paint.setColor(0xff73583c);
            canvas.drawRect(x - 6, top - 8, x + 6, top - 5, paint);
        }
        for (int i = 0; i < 18; i++) {
            float x = trailPosition(i * 79 + 17, distance * (0.35 + (i % 3) * 0.13));
            float y = ground - 28 - (i * 37 % 148)
                    + (float) Math.sin(runner.elapsed * 1.3 + i) * 5;
            paint.setColor(i % 2 == 0 ? 0x6ebfba64 : 0x6e87955d);
            canvas.drawOval(x - 4, y, x + 3, y + 2.8f, paint);
        }

        paint.setColor(0xffffffff);
        paint.setShader(roadShade);
        canvas.drawRect(0, ground, world.right, world.bottom, paint);
        paint.setShader(null);
        paint.setColor(0xffdbc496);
        canvas.drawRect(0, ground, world.right, ground + 3, paint);
        for (int i = 0; i < 38; i++) {
            float x = trailPosition(i * 91, distance);
            float y = ground + 9 + (i * 43 % 81);
            float width = 3 + (i % 5) * 3;
            paint.setColor(i % 3 == 0 ? 0x939b916d : 0x86503c2b);
            canvas.drawOval(x - width, y, x + width, y + 2.6f, paint);
            if (i % 3 == 0) {
                paint.setColor(0xff61764d);
                paint.setStrokeWidth(1.4f);
                canvas.drawLine(x, ground + 3, x - 4, ground - 6, paint);
                canvas.drawLine(x, ground + 3, x + 3, ground - 9, paint);
                canvas.drawLine(x, ground + 3, x + 7, ground - 3, paint);
            }
        }
    }

    private float trailPosition(double position, double offset) {
        return (float) (((position - offset) % RunnerPhysics.W + RunnerPhysics.W) % RunnerPhysics.W);
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
