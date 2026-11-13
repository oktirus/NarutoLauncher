package ua.konoha.tv.nativepreview.views;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.*;

import java.io.InputStream;
import java.util.*;

public class TechniqueRenderer {
    private static final double TAU = Math.PI * 2;

    private static final Set<String> COMPLETE_PAINTINGS = new HashSet<>(Arrays.asList(
            "rasengan", "sharingan", "kurama", "fireball", "amaterasu"
    ));

    public interface OnBitmapLoadedListener {
        void onBitmapLoaded(String name);
    }

    private final Context context;
    private final Map<String, Bitmap> bitmaps = new HashMap<>();
    private final Map<String, Long> requestTokens = new HashMap<>();
    private long cacheGeneration = 0;
    private long nextRequestToken = 0;
    private boolean isDisposed = false;

    private final java.util.concurrent.ThreadPoolExecutor decodeExecutor = new java.util.concurrent.ThreadPoolExecutor(
            1, 1, 0L, java.util.concurrent.TimeUnit.MILLISECONDS, new java.util.concurrent.LinkedBlockingQueue<>());
    private final android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final List<OnBitmapLoadedListener> loadListeners = new java.util.concurrent.CopyOnWriteArrayList<>();

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path chidoriPath = new Path();
    private final RectF dstRect = new RectF();
    private static final int MESH_W = 1;
    private static final int MESH_H = 16;
    private final float[] meshVerts = new float[(MESH_W + 1) * (MESH_H + 1) * 2];

    private static final int COLOR_CHIDORI_BLUE = Color.parseColor("#459cff");
    private static final int COLOR_CHIDORI_WHITE = Color.parseColor("#ecfbff");
    private static final int COLOR_CHIDORI_GLOW = Color.parseColor("#67c7ff");
    private static final int COLOR_CHIDORI_SPARK = Color.parseColor("#ffffff");
    private static final int COLOR_DUST = Color.parseColor("#d9cfb6");

    // Стан стрибка Гамабунти
    public static class FrogState {
        public PointF currentPos = new PointF(0, 0);
        public PointF fromPos = new PointF(0, 0);
        public PointF toPos = new PointF(0, 0);
        public float radius = 50f;
        public long startTime = 0;
        public long duration = 450; // мс
        public int direction = 1; // 1 вправо, -1 вліво
        public boolean isJumping = false;
        public long landTime = 0;
    }

    private final FrogState frogState = new FrogState();

    public TechniqueRenderer(Context context) {
        this.context = context.getApplicationContext();
        // C01, C07: Бітмапи більше не завантажуються всі 10 жадібно у конструкторі.
        // Завантаження відбувається суворо ліниво on-demand через ensureBitmap().
    }

    public void addOnBitmapLoadedListener(OnBitmapLoadedListener listener) {
        if (listener != null) loadListeners.add(listener);
    }

    public void removeOnBitmapLoadedListener(OnBitmapLoadedListener listener) {
        loadListeners.remove(listener);
    }

    public static Set<String> platesFor(String technique) {
        if ("chidori".equals(technique)) return Collections.emptySet();
        if ("gamabunta".equals(technique)) {
            return new HashSet<>(Arrays.asList("gamabunta-idle", "gamabunta-jump"));
        }
        return Collections.singleton(technique);
    }

    public synchronized Bitmap ensureBitmap(String name) {
        Bitmap bmp = bitmaps.get(name);
        if (bmp != null && !bmp.isRecycled()) {
            return bmp;
        }
        if (isDisposed) return null;

        if (!requestTokens.containsKey(name)) {
            final long token = ++nextRequestToken;
            requestTokens.put(name, token);
            final long gen = cacheGeneration;

            decodeExecutor.execute(() -> {
                synchronized (TechniqueRenderer.this) {
                    Long activeToken = requestTokens.get(name);
                    if (isDisposed || gen != cacheGeneration || activeToken == null || activeToken != token) {
                        return; // Пропускаємо застарілий запит без витратного декодування
                    }
                }
                Bitmap loaded = decodePlateFromAssets(name);
                mainHandler.post(() -> commitDecodedBitmap(name, token, gen, loaded));
            });
        }
        return null;
    }

    private Bitmap decodePlateFromAssets(String name) {
        try (InputStream is = context.getAssets().open("effects/effect-" + name + ".webp")) {
            return BitmapFactory.decodeStream(is);
        } catch (Exception ignored) {
            return null;
        }
    }

    private void commitDecodedBitmap(String name, long token, long gen, Bitmap loaded) {
        boolean shouldRecycle = false;
        boolean notify = false;

        synchronized (this) {
            Long activeToken = requestTokens.get(name);
            if (!isDisposed && gen == cacheGeneration && activeToken != null && activeToken == token) {
                requestTokens.remove(name);
                if (loaded != null && !loaded.isRecycled()) {
                    Bitmap existing = bitmaps.get(name);
                    if (existing != null && !existing.isRecycled()) {
                        shouldRecycle = true;
                    } else {
                        bitmaps.put(name, loaded);
                        notify = true;
                    }
                }
            } else {
                shouldRecycle = true;
                if (activeToken != null && activeToken == token) {
                    requestTokens.remove(name);
                }
            }
        }

        if (shouldRecycle && loaded != null && !loaded.isRecycled()) {
            loaded.recycle();
        }

        if (notify) {
            for (OnBitmapLoadedListener listener : loadListeners) {
                try {
                    listener.onBitmapLoaded(name);
                } catch (Exception ignored) {}
            }
        }
    }

    public synchronized Bitmap getBitmap(String name) {
        Bitmap bmp = bitmaps.get(name);
        if (bmp != null && !bmp.isRecycled()) {
            return bmp;
        }
        return null;
    }

    public synchronized void trimMemory(Set<String> keep) {
        cacheGeneration++;
        requestTokens.clear();
        decodeExecutor.getQueue().clear();
        Iterator<Map.Entry<String, Bitmap>> it = bitmaps.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Bitmap> entry = it.next();
            if (!keep.contains(entry.getKey())) {
                Bitmap bmp = entry.getValue();
                if (bmp != null && !bmp.isRecycled()) {
                    bmp.recycle();
                }
                it.remove();
            }
        }
    }

    public synchronized void clearCache() {
        cacheGeneration++;
        requestTokens.clear();
        decodeExecutor.getQueue().clear();
        for (Bitmap bmp : bitmaps.values()) {
            if (bmp != null && !bmp.isRecycled()) {
                bmp.recycle();
            }
        }
        bitmaps.clear();
    }

    public synchronized void release() {
        isDisposed = true;
        cacheGeneration++;
        requestTokens.clear();
        loadListeners.clear();
        for (Bitmap bmp : bitmaps.values()) {
            if (bmp != null && !bmp.isRecycled()) {
                bmp.recycle();
            }
        }
        bitmaps.clear();
        decodeExecutor.shutdownNow();
    }

    public void triggerGamabuntaJump(PointF from, PointF to, int direction) {
        frogState.fromPos.set(from.x, from.y - 20);
        frogState.toPos.set(to.x, to.y - 20);
        frogState.radius = 45f;
        frogState.startTime = System.currentTimeMillis();
        double dist = Math.hypot(to.x - from.x, to.y - from.y);
        frogState.duration = (long) Math.min(700, Math.max(370, dist * 1.5));
        frogState.direction = direction != 0 ? direction : (to.x >= from.x ? 1 : -1);
        frogState.isJumping = true;
    }

    /**
     * Основний метод малювання техніки на Canvas
     */
    public void drawTechnique(Canvas canvas, String id, float cx, float cy, float r, long timeMs, float burstLife, boolean isPreview) {
        double t = timeMs / 1000.0;
        paint.reset();
        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);

        if ("gamabunta".equals(id)) {
            drawGamabunta(canvas, cx, cy, r, timeMs, isPreview);
            return;
        }

        if ("chidori".equals(id)) {
            drawChidori(canvas, cx, cy, r, t, burstLife);
            return;
        }

        // C10: Для 5 завершених картин (rasengan, sharingan, kurama, fireball, amaterasu)
        // базовий градієнтний диск НЕ малюється, що повністю відповідає v26!
        if (!COMPLETE_PAINTINGS.contains(id)) {
            drawTechniqueCore(canvas, id, cx, cy, r, t);
        }

        // Рендер накладеного растрового ефекту (ліниво завантаженого)
        Bitmap bmp = ensureBitmap(id);
        if (bmp != null && !bmp.isRecycled()) {
            canvas.save();
            canvas.translate(cx, cy);

            float rot = 0;
            if (!isPreview) {
                if ("rasengan".equals(id)) rot = (float) Math.toDegrees(t * 0.82);
                else if ("sharingan".equals(id)) rot = (float) Math.toDegrees(t * 0.38);
                else if ("rasenshuriken".equals(id)) rot = (float) Math.toDegrees(t * 2.05);
                else if ("hiraishin".equals(id)) rot = (float) Math.toDegrees(Math.sin(t) * 0.05);
            }
            canvas.rotate(rot);

            float size = r * 2.46f * (1.0f + (isPreview ? 0f : (float) (Math.sin(t * 1.5) * 0.016 + burstLife * 0.035)));
            paint.reset();
            paint.setAntiAlias(true);
            paint.setFilterBitmap(true);
            paint.setAlpha("amaterasu".equals(id) ? 242 : 215);

            String mode = ("fireball".equals(id) || "amaterasu".equals(id)) ? "flame" : ("kurama".equals(id) ? "tails" : "still");
            drawDeformedPlate(canvas, bmp, size, t, isPreview ? "still" : mode);
            canvas.restore();
        }

        // Акценти поверх техніки
        drawTechniqueAccents(canvas, id, cx, cy, r, t, burstLife, isPreview);
    }

    /**
     * C10: Хвильова деформація рядків для вогню (Fireball, Amaterasu) та чакри лиса (Kurama)
     */
    private void drawDeformedPlate(Canvas canvas, Bitmap bmp, float size, double t, String mode) {
        if (!"flame".equals(mode) && !"tails".equals(mode)) {
            dstRect.set(-size / 2, -size / 2, size / 2, size / 2);
            canvas.drawBitmap(bmp, null, dstRect, paint);
            return;
        }

        boolean isTails = "tails".equals(mode);
        for (int row = 0; row <= MESH_H; row++) {
            float yRatio = (float) row / MESH_H;
            float y = -size / 2 + size * yRatio;
            float offset = (float) Math.sin(t * (isTails ? 1.3 : 2.2) + yRatio * 7.0) * size * (isTails ? 0.014f : 0.009f);

            for (int col = 0; col <= MESH_W; col++) {
                float xRatio = (float) col / MESH_W;
                float x = -size / 2 + size * xRatio + offset;
                int idx = 2 * (row * (MESH_W + 1) + col);
                meshVerts[idx] = x;
                meshVerts[idx + 1] = y;
            }
        }

        canvas.drawBitmapMesh(bmp, MESH_W, MESH_H, meshVerts, 0, null, 0, paint);
    }

    private void drawTechniqueCore(Canvas canvas, String id, float cx, float cy, float r, double t) {
        int[] palette;
        switch (id) {
            case "summoning":
                palette = new int[]{Color.parseColor("#d8c9a5"), Color.parseColor("#7e816c"), Color.parseColor("#3a5149")};
                break;
            case "hiraishin":
                palette = new int[]{Color.parseColor("#f7e4a3"), Color.parseColor("#b28b38"), Color.parseColor("#69552e")};
                break;
            case "rasenshuriken":
                palette = new int[]{Color.parseColor("#e4ffff"), Color.parseColor("#72cbd1"), Color.parseColor("#2f858f")};
                break;
            default:
                palette = new int[]{Color.WHITE, Color.CYAN, Color.BLUE};
                break;
        }

        canvas.save();
        RadialGradient grad = new RadialGradient(
                cx - r * 0.15f, cy - r * 0.15f, r * 1.16f,
                palette, new float[]{0.0f, 0.5f, 0.8f}, Shader.TileMode.CLAMP
        );
        paint.reset();
        paint.setAntiAlias(true);
        paint.setShader(grad);
        paint.setStyle(Paint.Style.FILL);
        paint.setAlpha(220);
        canvas.drawCircle(cx, cy, r * 1.12f, paint);
        paint.setShader(null);
        canvas.restore();
    }

    private void drawTechniqueAccents(Canvas canvas, String id, float cx, float cy, float r, double t, float burstLife, boolean isPreview) {
        canvas.save();
        canvas.translate(cx, cy);

        paint.reset();
        paint.setAntiAlias(true);

        if ("sharingan".equals(id)) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.parseColor("#fff5e4"));
            paint.setAlpha(217);
            canvas.drawCircle(-r * 0.68f, -r * 0.35f, r * 0.08f, paint);
        } else if ("hiraishin".equals(id) || "summoning".equals(id)) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor("summoning".equals(id) ? Color.parseColor("#263c35") : Color.parseColor("#fae9a7"));
            paint.setStrokeWidth(1.5f);
            paint.setAlpha(150);

            for (int k = 0; k < 8; k++) {
                canvas.save();
                canvas.rotate((float) (k * 45));
                canvas.drawLine(r * 0.2f, 0, r * 0.6f, 0, paint);
                canvas.drawLine(r * 0.6f, 0, r * 0.65f, r * 0.08f, paint);
                canvas.restore();
            }
        }

        if (burstLife > 0f) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(Color.parseColor("#f2c576"));
            paint.setStrokeWidth(2.5f);
            paint.setAlpha((int) (burstLife * 255));
            canvas.drawCircle(0, 0, r * (1.1f + (1f - burstLife) * 0.25f), paint);
        }

        canvas.restore();
    }

    /**
     * C10: Процедурний Чідорі з 22 розрядами, розгалуженнями (forks) та центральним ядром
     */
    private void drawChidori(Canvas canvas, float cx, float cy, float r, double t, float energy) {
        canvas.save();
        canvas.translate(cx, cy);

        float nX = (float) (Math.sin(t * 13) * r * 0.035);
        float nY = (float) (Math.cos(t * 11) * r * 0.035);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);

        int tick = (int) Math.floor(t * 24);

        // 22 розряди, що сходяться всередину до ядра
        for (int k = 0; k < 22; k++) {
            double angle = k * TAU / 22.0 + (noise(k + 3, tick) - 0.5) * 0.24;
            double reach = r * (0.96 + noise(k + 63, tick) * 0.34 + energy * 0.12);

            chidoriPath.reset();
            float lastX = nX;
            float lastY = nY;
            float forkX = 0, forkY = 0;

            for (int j = 0; j <= 9; j++) {
                double u = (double) j / 9.0;
                double rr = reach * (1.0 - u);
                double side = (noise(k * 31 + j, tick) - 0.5) * r * 0.18 * Math.sin(u * Math.PI);
                float px = (float) (nX + Math.cos(angle) * rr - Math.sin(angle) * side);
                float py = (float) (nY + Math.sin(angle) * rr + Math.cos(angle) * side);
                if (j == 0) chidoriPath.moveTo(px, py);
                else chidoriPath.lineTo(px, py);

                if (j == 5) {
                    forkX = px;
                    forkY = py;
                }
            }

            // Зовнішнє блакитне сяйво
            paint.setColor(COLOR_CHIDORI_BLUE);
            paint.setStrokeWidth(3.5f);
            paint.setAlpha((int) ((0.32 + noise(k + 90, tick) * 0.4) * 255));
            canvas.drawPath(chidoriPath, paint);

            // Внутрішня біла нитка блискавки
            paint.setColor(COLOR_CHIDORI_WHITE);
            paint.setStrokeWidth(1.2f);
            paint.setAlpha(230);
            canvas.drawPath(chidoriPath, paint);

            // Розгалуження (forks)
            if (forkX != 0) {
                chidoriPath.reset();
                chidoriPath.moveTo(forkX, forkY);
                double forkAngle = angle + (k % 2 == 0 ? 0.4 : -0.4) * (0.4 + noise(k + 9, tick) * 0.6);
                float fEndX = (float) (forkX + Math.cos(forkAngle) * r * 0.25);
                float fEndY = (float) (forkY + Math.sin(forkAngle) * r * 0.25);
                chidoriPath.lineTo(fEndX, fEndY);

                paint.setColor(COLOR_CHIDORI_GLOW);
                paint.setStrokeWidth(1.0f);
                paint.setAlpha(180);
                canvas.drawPath(chidoriPath, paint);
            }
        }

        // Центральні короткі спалахи ядра
        for (int k = 0; k < 12; k++) {
            double a = k * 2.399 + t * 0.16;
            double rr = r * (0.12 + noise(k + 400, tick) * 0.17);
            float px = (float) (nX + Math.cos(a) * rr);
            float py = (float) (nY + Math.sin(a) * rr);

            paint.setColor(COLOR_CHIDORI_SPARK);
            paint.setStrokeWidth(2f);
            paint.setAlpha(240);
            canvas.drawLine(px, py, nX, nY, paint);
        }

        canvas.restore();
    }

    private double noise(int n, int tick) {
        double v = Math.sin(n * 127.1 + tick * 311.7) * 43758.5453;
        return v - Math.floor(v);
    }

    /**
     * C11: Гамабунта — виправлено протилежні орієнтації спрайтів, відновлення alpha 255
     */
    private void drawGamabunta(Canvas canvas, float cx, float cy, float r, long timeMs, boolean isPreview) {
        if (isPreview) {
            Bitmap idle = ensureBitmap("gamabunta-idle");
            if (idle != null && !idle.isRecycled()) {
                paint.reset();
                paint.setAntiAlias(true);
                paint.setFilterBitmap(true);
                paint.setAlpha(255);
                float size = r * 1.6f;
                RectF dst = new RectF(cx - size / 2, cy - size * 0.7f, cx + size / 2, cy + size * 0.3f);
                canvas.drawBitmap(idle, null, dst, paint);
            }
            return;
        }

        long now = timeMs > 0 ? timeMs : System.currentTimeMillis();
        float curX = cx;
        float curY = cy - 20;
        boolean airborne = false;
        int dir = frogState.direction;

        if (frogState.isJumping) {
            long elapsed = now - frogState.startTime;
            double u = Math.max(0.0, Math.min(1.0, (double) elapsed / (double) frogState.duration));
            double ease = u * u * (3.0 - 2.0 * u);

            double dist = Math.hypot(frogState.toPos.x - frogState.fromPos.x, frogState.toPos.y - frogState.fromPos.y);
            double height = Math.min(128.0, Math.max(42.0, dist * 0.24));

            curX = (float) (frogState.fromPos.x + (frogState.toPos.x - frogState.fromPos.x) * ease);
            curY = (float) (frogState.fromPos.y + (frogState.toPos.y - frogState.fromPos.y) * ease - Math.sin(Math.PI * u) * height);
            airborne = (u > 0.08 && u < 0.92);

            if (u >= 1.0) {
                frogState.isJumping = false;
                frogState.landTime = now;
                frogState.currentPos.set(frogState.toPos);
            }
        }

        // 1. Тінь
        canvas.save();
        paint.reset();
        paint.setAntiAlias(true);
        paint.setColor(Color.BLACK);
        paint.setAlpha(airborne ? 38 : 76);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawOval(new RectF(curX - r * 0.34f, cy + 5, curX + r * 0.34f, cy + 17), paint);
        canvas.restore();

        // 2. Спрайт жаби (C11: обов'язкове скидання alpha у 255 та коректна інверсія орієнтації)
        String spriteKey = airborne ? "gamabunta-jump" : "gamabunta-idle";
        Bitmap frogBmp = ensureBitmap(spriteKey);
        if (frogBmp != null && !frogBmp.isRecycled()) {
            canvas.save();
            canvas.translate(curX, curY);

            // C11: Спрайт стрибка малювався обличчям ліворуч, сидяча поза — праворуч.
            // Відповідно у польоті напрямок інвертується: airborne ? -dir : dir.
            float scaleX = (airborne ? -dir : dir);
            canvas.scale(scaleX, 1.0f);

            paint.reset();
            paint.setAntiAlias(true);
            paint.setFilterBitmap(true);
            paint.setAlpha(255); // Відновлення повної непрозорості

            float size = r * 1.5f;
            RectF dst = new RectF(-size / 2, -size * 0.88f, size / 2, size * 0.12f);
            canvas.drawBitmap(frogBmp, null, dst, paint);
            canvas.restore();
        }

        // 3. Хмара пилу при приземленні (0.85 секунди)
        long landElapsed = now - frogState.landTime;
        if (!airborne && frogState.landTime > 0 && landElapsed < 850) {
            float progress = (float) landElapsed / 850f;
            float spread = (float) (1.0 - Math.pow(1.0 - progress, 3.4));
            float alpha = (float) Math.pow(1.0 - progress, 1.35);

            paint.reset();
            paint.setAntiAlias(true);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(COLOR_DUST);

            for (int k = 0; k < 14; k++) {
                double angle = k * TAU / 14.0 + 0.12 * (k % 3);
                double speed = 0.9 + (k % 4) * 0.23;
                float px = (float) (curX + Math.cos(angle) * r * (0.12 + spread * speed));
                float py = (float) (cy + Math.sin(angle) * r * (0.03 + spread * speed * 0.42) - progress * r * 0.13);
                float pSize = r * (0.07f + (k % 3) * 0.014f + progress * 0.14f);

                paint.setAlpha((int) (alpha * 140));
                canvas.drawCircle(px, py, pSize, paint);
            }
        }
    }

    /**
     * Одноразове малювання статичного прев'ю техніки для меню вибору
     */
    public void drawStaticPreview(Canvas canvas, String id, int width, int height) {
        float cx = width / 2f;
        float cy = height / 2f - 10f;
        float r = Math.min(width, height) * 0.28f;
        drawTechnique(canvas, id, cx, cy, r, 0, 0, true);
    }
}
