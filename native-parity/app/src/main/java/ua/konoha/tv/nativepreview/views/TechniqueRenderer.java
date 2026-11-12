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
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint highlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Path chidoriPath = new Path();
    private final Path chidoriForkPath = new Path();
    private final Path chidoriChargePath = new Path();
    private final Path chidoriCorePath = new Path();
    private final float[] chidoriPointsX = new float[10];
    private final float[] chidoriPointsY = new float[10];
    private final Path spiralPath = new Path();
    private final Path tailPath = new Path();
    private final Path flamePath = new Path();
    private final Path tomoePath = new Path();
    private final Path windPath = new Path();
    private final Path trailPath = new Path();
    private final Path kunaiPath = new Path();
    private final Path sealPath = new Path();
    private final Path clipPath = new Path();
    private final RectF dstRect = new RectF();
    private final RectF tempOval = new RectF();
    private static final int MESH_W = 1;
    private static final int MESH_H = 16;
    private final float[] meshVerts = new float[(MESH_W + 1) * (MESH_H + 1) * 2];

    private static final int COLOR_CHIDORI_BLUE = Color.parseColor("#459cff");
    private static final int COLOR_CHIDORI_WHITE = Color.parseColor("#ecfbff");
    private static final int COLOR_CHIDORI_GLOW = Color.parseColor("#67c7ff");
    private static final int COLOR_CHIDORI_SPARK = Color.parseColor("#ffffff");
    private static final int COLOR_DUST = Color.parseColor("#d9cfb6");

    private static final int[] PALETTE_SUMMONING = new int[]{Color.parseColor("#d8c9a5"), Color.parseColor("#7e816c"), Color.parseColor("#3a5149")};
    private static final int[] PALETTE_HIRAISHIN = new int[]{Color.parseColor("#f7e4a3"), Color.parseColor("#b28b38"), Color.parseColor("#69552e")};
    private static final int[] PALETTE_RASENSHURIKEN = new int[]{Color.parseColor("#e4ffff"), Color.parseColor("#72cbd1"), Color.parseColor("#2f858f")};
    private static final int[] PALETTE_DEFAULT = new int[]{Color.WHITE, Color.CYAN, Color.BLUE};
    private static final float[] GRADIENT_STOPS = new float[]{0.0f, 0.5f, 0.8f};

    private final float density;
    private String lastGradTech = null;
    private float lastGradR = -1f;
    private float lastGradCx = -1f;
    private float lastGradCy = -1f;
    private RadialGradient cachedGrad = null;

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
        float d = context.getResources().getDisplayMetrics().density;
        this.density = d > 0 ? d : 1.0f;
        // C01, C07: Бітмапи більше не завантажуються всі 10 жадібно у конструкторі.
        // Завантаження відбувається суворо ліниво on-demand через ensureBitmap().
    }

    private float dp(float px) {
        return Math.max(1.0f, px * (density / 1.5f));
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
        cachedGrad = null;
        lastGradTech = null;
        if (smartTubeIcon != null && !smartTubeIcon.isRecycled()) {
            smartTubeIcon.recycle();
            smartTubeIcon = null;
        }
        if (lampaIcon != null && !lampaIcon.isRecycled()) {
            lampaIcon.recycle();
            lampaIcon = null;
        }
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
        cachedGrad = null;
        lastGradTech = null;
        if (smartTubeIcon != null && !smartTubeIcon.isRecycled()) {
            smartTubeIcon.recycle();
            smartTubeIcon = null;
        }
        if (lampaIcon != null && !lampaIcon.isRecycled()) {
            lampaIcon.recycle();
            lampaIcon = null;
        }
        decodeExecutor.shutdownNow();
    }

    public void triggerGamabuntaJump(PointF from, PointF to, int direction, long now) {
        float r = 45f;
        frogState.fromPos.set(from.x + r * 0.5f, from.y - r * 0.26f);
        frogState.toPos.set(to.x + r * 0.5f, to.y - r * 0.26f);
        frogState.radius = r;
        frogState.startTime = now;
        double dist = Math.hypot(to.x - from.x, to.y - from.y);
        frogState.duration = (long) Math.min(700, Math.max(370, dist * 1.5));
        frogState.direction = direction != 0 ? direction : (to.x >= from.x ? 1 : -1);
        frogState.isJumping = true;
    }

    public void triggerGamabuntaJump(PointF from, PointF to, int direction) {
        triggerGamabuntaJump(from, to, direction, android.os.SystemClock.uptimeMillis());
    }

    /**
     * Основний метод малювання техніки на Canvas з повною процедурною анімацією
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

        if ("rasengan".equals(id)) {
            drawRasengan(canvas, cx, cy, r, t, burstLife, isPreview);
            return;
        }

        if ("kurama".equals(id)) {
            drawKurama(canvas, cx, cy, r, t, burstLife, isPreview);
            return;
        }

        if ("fireball".equals(id)) {
            drawFireball(canvas, cx, cy, r, t, burstLife, isPreview);
            return;
        }

        if ("amaterasu".equals(id)) {
            drawAmaterasu(canvas, cx, cy, r, t, burstLife, isPreview);
            return;
        }

        if ("sharingan".equals(id)) {
            drawSharingan(canvas, cx, cy, r, t, burstLife, isPreview);
            return;
        }

        if ("rasenshuriken".equals(id)) {
            drawRasenshuriken(canvas, cx, cy, r, t, burstLife, isPreview);
            return;
        }

        if ("summoning".equals(id)) {
            drawSummoning(canvas, cx, cy, r, t, burstLife, isPreview);
            return;
        }

        if ("hiraishin".equals(id)) {
            drawHiraishin(canvas, cx, cy, r, t, burstLife, isPreview);
            return;
        }

        drawTechniqueCore(canvas, id, cx, cy, r, t);
    }

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
        switch (id != null ? id : "") {
            case "summoning":
                palette = PALETTE_SUMMONING;
                break;
            case "hiraishin":
                palette = PALETTE_HIRAISHIN;
                break;
            case "rasenshuriken":
                palette = PALETTE_RASENSHURIKEN;
                break;
            default:
                palette = PALETTE_DEFAULT;
                break;
        }

        canvas.save();
        if (cachedGrad == null || !id.equals(lastGradTech) || lastGradR != r || lastGradCx != cx || lastGradCy != cy) {
            lastGradTech = id;
            lastGradR = r;
            lastGradCx = cx;
            lastGradCy = cy;
            cachedGrad = new RadialGradient(
                    cx - r * 0.15f, cy - r * 0.15f, r * 1.16f,
                    palette, GRADIENT_STOPS, Shader.TileMode.CLAMP
            );
        }
        paint.reset();
        paint.setAntiAlias(true);
        paint.setShader(cachedGrad);
        paint.setStyle(Paint.Style.FILL);
        paint.setAlpha(220);
        canvas.drawCircle(cx, cy, r * 1.12f, paint);
        paint.setShader(null);

        if ("summoning".equals(id) || "hiraishin".equals(id)) {
            strokePaint.setStyle(Paint.Style.STROKE);
            strokePaint.setColor("summoning".equals(id) ? Color.parseColor("#263c35") : Color.parseColor("#fae9a7"));
            strokePaint.setAlpha((int) (0.6f * 255));
            strokePaint.setStrokeWidth(dp(1.1f));
            for (int k = 0; k < 12; k++) {
                canvas.save();
                canvas.rotate(k * 30f, cx, cy);
                canvas.drawLine(cx + r * 0.19f, cy, cx + r * 0.38f, cy, strokePaint);
                canvas.drawLine(cx + r * 0.42f, cy + r * 0.08f, cx + r * 0.67f, cy + r * 0.08f, strokePaint);
                canvas.drawLine(cx + r * 0.67f, cy - r * 0.06f, cx + r * 0.93f, cy - r * 0.06f, strokePaint);
                canvas.restore();
            }
            strokePaint.setAlpha((int) (0.26f * 255));
            strokePaint.setStrokeWidth(r * 0.07f);
            canvas.drawCircle(cx, cy, r * 0.32f, strokePaint);
            canvas.drawCircle(cx, cy, r * 0.77f, strokePaint);
        }

        canvas.restore();
    }

    private void drawSpiral(Canvas c, float r, double t) {
        float dScale = density / 1.5f;
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeCap(Paint.Cap.ROUND);
        strokePaint.setStrokeJoin(Paint.Join.ROUND);

        for (int j = 0; j < 7; j++) {
            c.save();
            float angle = (float) ((Math.toDegrees(t * 1.45 + j * TAU / 7.0)) % 360.0);
            c.rotate(angle);

            strokePaint.setColor(j % 2 == 1 ? Color.parseColor("#d8faff") : Color.parseColor("#62cfea"));
            strokePaint.setStrokeWidth((j % 2 == 1 ? 1.0f : 2.0f) * dScale);
            strokePaint.setAlpha((int) (0.45f * 255));

            spiralPath.reset();
            for (int k = 0; k <= 44; k++) {
                double a = (double) k / 44.0 * 3.6;
                double rad = r * (0.72 + (double) k / 44.0 * 0.3);
                float x = (float) (Math.cos(a) * rad);
                float y = (float) (Math.sin(a) * rad * 0.96);
                if (k == 0) spiralPath.moveTo(x, y);
                else spiralPath.lineTo(x, y);
            }
            c.drawPath(spiralPath, strokePaint);
            c.restore();
        }

        // Зовнішнє кільце чистої чакри (з techniques.js:23)
        c.save();
        strokePaint.setColor(Color.parseColor("#bff7ff"));
        strokePaint.setStrokeWidth(1.0f * dScale);
        strokePaint.setAlpha((int) (0.5f * 255));
        c.drawCircle(0, 0, r * 0.94f, strokePaint);
        c.restore();
    }

    private void drawRasengan(Canvas canvas, float cx, float cy, float r, double t, float burstLife, boolean isPreview) {
        canvas.save();
        canvas.translate(cx, cy);

        // Растровий диск із плавним обертанням та диханням (technique-art.js: completePaintings)
        Bitmap bmp = ensureBitmap("rasengan");
        if (bmp != null && !bmp.isRecycled()) {
            canvas.save();
            float rot = (float) ((Math.toDegrees(t * 0.82)) % 360.0);
            canvas.rotate(rot);
            float size = r * 2.46f * (1.0f + (isPreview ? 0f : (float) (Math.sin(t * 1.5) * 0.016 + burstLife * 0.035)));
            paint.reset();
            paint.setAntiAlias(true);
            paint.setFilterBitmap(true);
            paint.setAlpha(214); // 0.84f * 255
            dstRect.set(-size / 2, -size / 2, size / 2, size / 2);
            canvas.drawBitmap(bmp, null, dstRect, paint);
            canvas.restore();
        } else {
            drawSpiral(canvas, r, t);
            canvas.save();
            canvas.translate(r * 0.68f, r * 0.67f);
            drawSpiral(canvas, r * 0.24f, -t * 2.0);
            canvas.restore();
        }

        // Енергетична хвиля вибору (burst ring)
        if (burstLife > 0f) {
            strokePaint.setStyle(Paint.Style.STROKE);
            strokePaint.setColor(Color.parseColor("#85e5ff"));
            strokePaint.setStrokeWidth(dp(1.2f));
            strokePaint.setAlpha((int) (burstLife * 0.5f * 255));
            canvas.drawCircle(0, 0, r * (1.04f + (1.0f - burstLife) * 0.14f), strokePaint);
        }

        canvas.restore();
    }

    private void drawKurama(Canvas canvas, float cx, float cy, float r, double t, float burstLife, boolean isPreview) {
        canvas.save();
        canvas.translate(cx, cy);

        // Хвильова деформація чакри Курами як базова підкладка (technique-art.js: completePaintings)
        Bitmap bmp = ensureBitmap("kurama");
        if (bmp != null && !bmp.isRecycled()) {
            float size = r * 2.46f * (1.0f + (isPreview ? 0f : (float) (Math.sin(t * 1.5) * 0.016 + burstLife * 0.035)));
            paint.reset();
            paint.setAntiAlias(true);
            paint.setFilterBitmap(true);
            paint.setAlpha(214); // 0.84f * 255 (technique-art.js:177)
            drawDeformedPlate(canvas, bmp, size, t, isPreview ? "still" : "tails");
        }

        // Енергетична хвиля вибору (burst ring)
        if (burstLife > 0f) {
            strokePaint.setStyle(Paint.Style.STROKE);
            strokePaint.setColor(Color.parseColor("#ff9a46"));
            strokePaint.setStrokeWidth(dp(1.2f));
            strokePaint.setAlpha((int) (burstLife * 0.5f * 255));
            canvas.drawCircle(0, 0, r * (1.04f + (1.0f - burstLife) * 0.14f), strokePaint);
        }

        canvas.restore();
    }

    private void drawFlames(Canvas canvas, float r, double t, boolean dark) {
        fillPaint.setStyle(Paint.Style.FILL);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeCap(Paint.Cap.ROUND);
        strokePaint.setStrokeJoin(Paint.Join.ROUND);

        for (int k = 0; k < 22; k++) {
            double a = k * TAU / 22.0;
            double phase = t * (dark ? 1.5 : 2.0) + k * 1.61;
            float h = (float) (r * (0.15 + 0.16 * (0.5 + 0.5 * Math.sin(phase))));
            float w = r * 0.105f;

            canvas.save();
            canvas.rotate((float) Math.toDegrees(a));
            canvas.translate(r * 0.97f, 0);
            canvas.rotate(90f);

            fillPaint.setColor(dark ? Color.parseColor("#150d22") : (k % 2 == 1 ? Color.parseColor("#f47a25") : Color.parseColor("#ffd16c")));
            strokePaint.setColor(dark ? Color.parseColor("#b388ff") : Color.parseColor("#ffb94a"));
            strokePaint.setStrokeWidth(dark ? 3.0f : 1.5f);

            flamePath.reset();
            flamePath.moveTo(-w, 0);
            flamePath.cubicTo(-w * 1.2f, -h * 0.5f, -w * 0.4f, -h * 0.5f, (float) (Math.sin(phase) * w), -h);
            flamePath.cubicTo(w * 0.6f, -h * 0.6f, w, -h * 0.2f, w, 0);
            flamePath.close();

            canvas.drawPath(flamePath, fillPaint);
            canvas.drawPath(flamePath, strokePaint);

            fillPaint.setColor(dark ? Color.parseColor("#7e57c2") : Color.parseColor("#ffe5a1"));
            fillPaint.setAlpha(dark ? 180 : 200);
            flamePath.reset();
            flamePath.moveTo(-w * 0.4f, 0);
            flamePath.quadTo(0, -h * 0.65f, w * 0.45f, 0);
            flamePath.close();
            canvas.drawPath(flamePath, fillPaint);

            canvas.restore();
        }
    }

    private void drawFireball(Canvas canvas, float cx, float cy, float r, double t, float burstLife, boolean isPreview) {
        canvas.save();
        canvas.translate(cx, cy);

        // Хвильова деформація полум'я (technique-art.js: completePaintings)
        Bitmap bmp = ensureBitmap("fireball");
        if (bmp != null && !bmp.isRecycled()) {
            float size = r * 2.46f * (1.0f + (isPreview ? 0f : (float) (Math.sin(t * 1.5) * 0.016 + burstLife * 0.035)));
            paint.reset();
            paint.setAntiAlias(true);
            paint.setFilterBitmap(true);
            paint.setAlpha(214); // 0.84f * 255 (technique-art.js:177)
            drawDeformedPlate(canvas, bmp, size, t, isPreview ? "still" : "flame");
        }

        if (burstLife > 0f) {
            strokePaint.setStyle(Paint.Style.STROKE);
            strokePaint.setColor(Color.parseColor("#ffb65c"));
            strokePaint.setStrokeWidth(dp(1.2f));
            strokePaint.setAlpha((int) (burstLife * 0.5f * 255));
            canvas.drawCircle(0, 0, r * (1.04f + (1.0f - burstLife) * 0.14f), strokePaint);
        }

        canvas.restore();
    }

    private void drawAmaterasu(Canvas canvas, float cx, float cy, float r, double t, float burstLife, boolean isPreview) {
        canvas.save();
        canvas.translate(cx, cy);

        // Хвильова деформація базової пластини Аматерасу (technique-art.js: completePaintings)
        Bitmap bmp = ensureBitmap("amaterasu");
        if (bmp != null && !bmp.isRecycled()) {
            float size = r * 2.46f * (1.0f + (isPreview ? 0f : (float) (Math.sin(t * 1.5) * 0.016 + burstLife * 0.035)));
            paint.reset();
            paint.setAntiAlias(true);
            paint.setFilterBitmap(true);
            paint.setAlpha(242); // 0.95f * 255 (technique-art.js:177)
            drawDeformedPlate(canvas, bmp, size, t, isPreview ? "still" : "flame");
        }

        if (burstLife > 0f) {
            strokePaint.setStyle(Paint.Style.STROKE);
            strokePaint.setColor(Color.parseColor("#c3a0ed"));
            strokePaint.setStrokeWidth(dp(1.2f));
            strokePaint.setAlpha((int) (burstLife * 0.5f * 255));
            canvas.drawCircle(0, 0, r * (1.04f + (1.0f - burstLife) * 0.14f), strokePaint);
        }

        canvas.restore();
    }

    private void drawTomoe(Canvas canvas, float s) {
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColor(Color.parseColor("#190b11"));
        canvas.drawCircle(0, 0, s, fillPaint);

        tomoePath.reset();
        tomoePath.moveTo(-s, 0);
        tomoePath.cubicTo(-s * 1.4f, -s * 1.7f, s * 0.7f, -s * 2.3f, s * 1.9f, -s * 1.5f);
        tomoePath.quadTo(s * 0.2f, -s * 1.4f, s * 0.6f, s * 0.1f);
        tomoePath.close();
        canvas.drawPath(tomoePath, fillPaint);
    }

    private void drawSharingan(Canvas canvas, float cx, float cy, float r, double t, float burstLife, boolean isPreview) {
        canvas.save();
        canvas.translate(cx, cy);

        // 1. Растровий диск Шарінгана з плавним обертанням (plate з technique-art.js)
        Bitmap bmp = ensureBitmap("sharingan");
        if (bmp != null && !bmp.isRecycled()) {
            canvas.save();
            float rot = (float) ((Math.toDegrees(t * 0.38)) % 360.0);
            canvas.rotate(rot);
            float size = r * 2.46f * (1.0f + (isPreview ? 0f : (float) (Math.sin(t * 1.5) * 0.016 + burstLife * 0.035)));
            paint.reset();
            paint.setAntiAlias(true);
            paint.setFilterBitmap(true);
            paint.setAlpha(214); // 0.84f * 255
            dstRect.set(-size / 2, -size / 2, size / 2, size / 2);
            canvas.drawBitmap(bmp, null, dstRect, paint);
            canvas.restore();

            // 2. Спекулярний білий відблиск на склі ока (technique-art.js:180)
            canvas.save();
            canvas.translate(-r * 0.68f, -r * 0.35f);
            canvas.rotate((float) Math.toDegrees(0.6));
            fillPaint.setStyle(Paint.Style.FILL);
            fillPaint.setColor(Color.parseColor("#fff5e4"));
            fillPaint.setAlpha(217); // 0.85f * 255
            tempOval.set(-r * 0.065f, -r * 0.12f, r * 0.065f, r * 0.12f);
            canvas.drawOval(tempOval, fillPaint);
            canvas.restore();
        } else {
            // Векторний fallback (techniques.js)
            drawSharinganFallback(canvas, r, t);
        }

        // 3. Енергетична хвиля вибору (burst ring)
        if (burstLife > 0f) {
            strokePaint.setStyle(Paint.Style.STROKE);
            strokePaint.setColor(Color.parseColor("#c8283b"));
            strokePaint.setStrokeWidth(dp(1.2f));
            strokePaint.setAlpha((int) (burstLife * 0.5f * 255));
            canvas.drawCircle(0, 0, r * (1.04f + (1.0f - burstLife) * 0.14f), strokePaint);
        }

        canvas.restore();
    }

    private void drawSharinganFallback(Canvas canvas, float r, double t) {
        // Червоні кільця райдужки
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setColor(Color.parseColor("#ee424b"));
        strokePaint.setStrokeWidth(dp(3.0f));
        strokePaint.setAlpha(165);
        canvas.drawCircle(0, 0, r, strokePaint);

        strokePaint.setColor(Color.parseColor("#741c28"));
        strokePaint.setStrokeWidth(dp(1.0f));
        strokePaint.setAlpha(200);
        canvas.drawCircle(0, 0, r * 0.79f, strokePaint);

        // 3 зовнішні томое, що обертаються за рухом годинника
        canvas.save();
        canvas.rotate((float) ((Math.toDegrees(t * 0.6)) % 360.0));
        for (int k = 0; k < 3; k++) {
            canvas.save();
            canvas.rotate((float) (k * 120));
            canvas.translate(r * 0.87f, 0);

            fillPaint.setColor(Color.parseColor("#ec4d54"));
            canvas.drawCircle(0, 0, r * 0.145f, fillPaint);
            drawTomoe(canvas, r * 0.075f);

            canvas.restore();
        }
        canvas.restore();

        // Міні-око зі зворотним обертанням у правому нижньому кутку
        canvas.save();
        canvas.translate(r * 0.67f, r * 0.65f);
        fillPaint.setColor(Color.parseColor("#bc2534"));
        canvas.drawCircle(0, 0, r * 0.22f, fillPaint);

        strokePaint.setColor(Color.parseColor("#ff7d7d"));
        strokePaint.setStrokeWidth(dp(1.0f));
        canvas.drawCircle(0, 0, r * 0.22f, strokePaint);

        canvas.rotate((float) ((Math.toDegrees(t * 0.6)) % 360.0));
        fillPaint.setColor(Color.parseColor("#160d14"));
        canvas.drawCircle(0, 0, r * 0.042f, fillPaint);

        for (int k = 0; k < 3; k++) {
            canvas.save();
            canvas.rotate((float) (k * 120));
            drawTomoe(canvas, r * 0.036f);
            canvas.restore();
        }
        canvas.restore();
    }

    private void drawRasenshuriken(Canvas canvas, float cx, float cy, float r, double t, float burstLife, boolean isPreview) {
        canvas.save();
        canvas.translate(cx, cy);
        float dScale = density / 1.5f;

        // 1. Внутрішнє енергетичне ядро чакри (core з technique-art.js:128-135)
        // 1.1. Радіальний градієнт стихії вітру
        if (cachedGrad == null || !"rasenshuriken".equals(lastGradTech) || lastGradR != r || lastGradCx != cx || lastGradCy != cy) {
            lastGradTech = "rasenshuriken";
            lastGradR = r;
            lastGradCx = cx;
            lastGradCy = cy;
            cachedGrad = new RadialGradient(
                    -r * 0.15f, -r * 0.15f, r * 1.16f,
                    new int[]{Color.parseColor("#e4ffff"), Color.parseColor("#72cbd1"), Color.parseColor("#2f858f"), Color.parseColor("#002f858f")},
                    new float[]{0.0f, 0.5f, 0.8f, 1.0f},
                    Shader.TileMode.CLAMP
            );
        }
        paint.reset();
        paint.setAntiAlias(true);
        paint.setShader(cachedGrad);
        paint.setStyle(Paint.Style.FILL);
        paint.setAlpha(230); // 0.9f * 255
        canvas.drawCircle(0, 0, r * 1.16f, paint);
        paint.setShader(null);

        // 1.2. 23 зустрічні швидкісні дуги чакри всередині кола відсікання r * 1.08f (core з technique-art.js:133-135)
        canvas.save();
        clipPath.reset();
        clipPath.addCircle(0, 0, r * 1.08f, Path.Direction.CW);
        canvas.clipPath(clipPath);

        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeCap(Paint.Cap.ROUND);
        for (int k = 0; k < 23; k++) {
            canvas.save();
            double rotSpeed = (k % 2 == 1 ? 1.1 : -0.85);
            float angle = (float) ((Math.toDegrees(t * rotSpeed + k * 2.399)) % 360.0);
            canvas.rotate(angle);
            canvas.scale(1.0f, 0.24f + (k % 5) * 0.13f);

            strokePaint.setColor(k % 3 != 0 ? Color.parseColor("#dafcff") : Color.parseColor("#4fbdea"));
            strokePaint.setStrokeWidth((0.7f + (k % 3) * 0.55f) * dScale);
            strokePaint.setAlpha((int) ((0.25f + (k % 4) * 0.1f) * 255));

            tempOval.set(r * 0.13f - r * (0.22f + k * 0.033f), -r * (0.22f + k * 0.033f),
                    r * 0.13f + r * (0.22f + k * 0.033f), r * (0.22f + k * 0.033f));
            canvas.drawArc(tempOval, (float) Math.toDegrees(0.15), (float) Math.toDegrees(4.75), false, strokePaint);
            canvas.restore();
        }
        canvas.restore(); // restore clip

        // 2. Растровий диск стихії вітру з високою швидкістю обертання (plate з technique-art.js:176-179: швидкість t * 2.05)
        Bitmap bmp = ensureBitmap("rasenshuriken");
        if (bmp != null && !bmp.isRecycled()) {
            canvas.save();
            float rot = (float) ((Math.toDegrees(t * 2.05)) % 360.0);
            canvas.rotate(rot);
            float size = r * 2.46f * (1.0f + (isPreview ? 0f : (float) (Math.sin(t * 1.5) * 0.016 + burstLife * 0.035)));
            paint.reset();
            paint.setAntiAlias(true);
            paint.setFilterBitmap(true);
            paint.setAlpha(214); // 0.84f * 255
            dstRect.set(-size / 2, -size / 2, size / 2, size / 2);
            canvas.drawBitmap(bmp, null, dstRect, paint);
            canvas.restore();
        } else {
            // Лише як fallback при затримці декодування бітмапа: векторні леза та спіраль (з techniques.js:79)
            drawRasenshurikenFallback(canvas, r, t, dScale);
        }

        // 3. Зовнішні орбітальні вихори чакри (accents з technique-art.js:113-114: швидкість t * 2.4)
        for (int k = 0; k < 18; k++) {
            double a = k * TAU / 18.0 + t * 2.4;
            float rad = r * (0.82f + (k % 3) * 0.09f);
            float ex = (float) (Math.cos(a) * r * 0.06f);
            float ey = (float) (Math.sin(a) * r * 0.06f);

            strokePaint.setColor(k % 2 == 1 ? Color.parseColor("#e6fdff") : Color.parseColor("#72d8ee"));
            strokePaint.setAlpha((int) ((0.2f + (k % 3) * 0.1f) * 255));
            strokePaint.setStrokeWidth((0.6f + (k % 3) * 0.45f) * dScale);

            canvas.save();
            canvas.translate(ex, ey);
            canvas.rotate((float) ((Math.toDegrees(a)) % 360.0));
            tempOval.set(-rad, -rad * 0.83f, rad, rad * 0.83f);
            canvas.drawArc(tempOval, (float) Math.toDegrees(a), (float) Math.toDegrees(0.55), false, strokePaint);
            canvas.restore();
        }

        // 4. Енергетична хвиля вибору (burst ring)
        if (burstLife > 0f) {
            strokePaint.setStyle(Paint.Style.STROKE);
            strokePaint.setColor(Color.parseColor("#b7f1e5"));
            strokePaint.setStrokeWidth(dp(1.2f));
            strokePaint.setAlpha((int) (burstLife * 0.5f * 255));
            canvas.drawCircle(0, 0, r * (1.04f + (1.0f - burstLife) * 0.14f), strokePaint);
        }

        canvas.restore();
    }

    private void drawRasenshurikenFallback(Canvas canvas, float r, double t, float dScale) {
        canvas.save();
        canvas.rotate((float) ((Math.toDegrees(t * 1.6)) % 360.0));
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColor(Color.parseColor("#33b8f5eb"));
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setColor(Color.parseColor("#b9efe5"));
        strokePaint.setStrokeWidth(1.3f * dScale);

        for (int k = 0; k < 4; k++) {
            canvas.rotate(90f);
            windPath.reset();
            windPath.moveTo(r * 0.72f, -r * 0.18f);
            windPath.quadTo(r * 1.27f, -r * 0.48f, r * 1.38f, -r * 0.08f);
            windPath.quadTo(r * 1.10f, -r * 0.17f, r * 0.86f, r * 0.13f);
            windPath.close();
            canvas.drawPath(windPath, fillPaint);
            canvas.drawPath(windPath, strokePaint);
        }
        canvas.restore();

        drawSpiral(canvas, r * 0.8f, -t * 0.7);
    }

    private void drawSummoning(Canvas canvas, float cx, float cy, float r, double t, float burstLife, boolean isPreview) {
        canvas.save();
        canvas.translate(cx, cy);

        // 1. Внутрішнє енергетичне ядро чакри (core: summoning)
        drawTechniqueCore(canvas, "summoning", 0, 0, r, t);

        // 2. Растрова платівка ефекту призову (effect-summoning.webp)
        Bitmap bmp = ensureBitmap("summoning");
        if (bmp != null && !bmp.isRecycled()) {
            canvas.save();
            float size = r * 2.46f * (1.0f + (isPreview ? 0f : (float) (Math.sin(t * 1.5) * 0.016 + burstLife * 0.035)));
            paint.reset();
            paint.setAntiAlias(true);
            paint.setFilterBitmap(true);
            paint.setAlpha(214); // 0.84f * 255
            dstRect.set(-size / 2, -size / 2, size / 2, size / 2);
            canvas.drawBitmap(bmp, null, dstRect, paint);
            canvas.restore();
        } else {
            drawSummoningFallback(canvas, r, t);
        }

        // 3. Акценти (accents: summoning — хмарки диму призову з technique-art.js:122)
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColor(Color.parseColor("#dddaca"));
        for (int k = 0; k < 7; k++) {
            double phase = (t * 0.27 + (double) k / 7.0) % 1.0;
            double a = k * TAU / 7.0;
            fillPaint.setAlpha((int) ((1.0 - phase) * 0.3 * 255));
            float px = (float) (Math.cos(a) * r * (1.0 + phase * 0.25));
            float py = (float) (Math.sin(a) * r * (1.0 + phase * 0.25) - phase * r * 0.12);
            canvas.drawCircle(px, py, (float) (r * (0.07 + phase * 0.09)), fillPaint);
        }

        // 4. Енергетична хвиля вибору (burst ring)
        if (burstLife > 0f) {
            strokePaint.setStyle(Paint.Style.STROKE);
            strokePaint.setColor(Color.parseColor("#e0d4b8"));
            strokePaint.setStrokeWidth(dp(1.2f));
            strokePaint.setAlpha((int) (burstLife * 0.5f * 255));
            canvas.drawCircle(0, 0, r * (1.04f + (1.0f - burstLife) * 0.14f), strokePaint);
        }

        canvas.restore();
    }

    private void drawSummoningFallback(Canvas canvas, float r, double t) {
        canvas.save();
        canvas.rotate((float) Math.toDegrees(Math.sin(t * 0.3) * 0.025));
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setColor(Color.parseColor("#cbb786"));
        strokePaint.setStrokeWidth(2.5f);
        canvas.drawCircle(0, 0, r, strokePaint);

        strokePaint.setColor(Color.parseColor("#a89775"));
        strokePaint.setStrokeWidth(1.5f);
        canvas.drawCircle(0, 0, r * 0.83f, strokePaint);

        strokePaint.setColor(Color.parseColor("#d9c895"));
        strokePaint.setStrokeWidth(1.8f);
        for (int k = 0; k < 8; k++) {
            canvas.save();
            canvas.rotate((float) (k * 45));
            sealPath.reset();
            sealPath.moveTo(r * 0.6f, 0);
            sealPath.lineTo(r * 0.8f, 0);
            sealPath.lineTo(r * 0.91f, r * 0.07f);
            sealPath.lineTo(r * 1.1f, r * 0.07f);
            sealPath.moveTo(r * 0.96f, -r * 0.055f);
            sealPath.lineTo(r * 1.07f, -r * 0.055f);
            canvas.drawPath(sealPath, strokePaint);
            canvas.restore();
        }
        canvas.restore();
    }

    private void drawHiraishin(Canvas canvas, float cx, float cy, float r, double t, float burstLife, boolean isPreview) {
        canvas.save();
        canvas.translate(cx, cy);

        // 1. Внутрішнє енергетичне ядро чакри (core: hiraishin)
        drawTechniqueCore(canvas, "hiraishin", 0, 0, r, t);

        // 2. Растрова платівка Хірайшина (effect-hiraishin.webp з кунаєм та формулою Мінато)
        Bitmap bmp = ensureBitmap("hiraishin");
        if (bmp != null && !bmp.isRecycled()) {
            canvas.save();
            float rot = (float) ((Math.toDegrees(Math.sin(t) * 0.018)) % 360.0);
            canvas.rotate(rot);
            float size = r * 2.46f * (1.0f + (isPreview ? 0f : (float) (Math.sin(t * 1.5) * 0.016 + burstLife * 0.035)));
            paint.reset();
            paint.setAntiAlias(true);
            paint.setFilterBitmap(true);
            paint.setAlpha(214); // 0.84f * 255
            dstRect.set(-size / 2, -size / 2, size / 2, size / 2);
            canvas.drawBitmap(bmp, null, dstRect, paint);
            canvas.restore();
        } else {
            drawHiraishinFallback(canvas, r, t);
        }

        // 3. Акценти Жовтої блискавки (accents: hiraishin з technique-art.js:124-126)
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeCap(Paint.Cap.ROUND);
        for (int k = 0; k < 6; k++) {
            double a = k * TAU / 6.0 + t * 1.8;
            strokePaint.setColor(Color.parseColor("#ffe57f"));
            strokePaint.setStrokeWidth(dp(2.2f));
            strokePaint.setAlpha((int) (0.45f * 255));
            float rad = r * (0.92f + (k % 2) * 0.14f);
            tempOval.set(-rad, -rad, rad, rad);
            canvas.drawArc(tempOval, (float) Math.toDegrees(a), (float) Math.toDegrees(0.45), false, strokePaint);
        }
        strokePaint.setColor(Color.parseColor("#fff0b5"));
        strokePaint.setStrokeWidth(dp(0.9f));
        strokePaint.setAlpha((int) ((0.25f + 0.12f * Math.sin(t * 1.7)) * 255));
        for (int k = 0; k < 6; k++) {
            double a = k * TAU / 6.0;
            canvas.drawLine((float) (Math.cos(a) * r * 0.82), (float) (Math.sin(a) * r * 0.82),
                    (float) (Math.cos(a + 0.05) * r), (float) (Math.sin(a + 0.05) * r), strokePaint);
        }

        // 4. Енергетична хвиля вибору (burst ring)
        if (burstLife > 0f) {
            strokePaint.setStyle(Paint.Style.STROKE);
            strokePaint.setColor(Color.parseColor("#ffd700"));
            strokePaint.setStrokeWidth(dp(1.2f));
            strokePaint.setAlpha((int) (burstLife * 0.5f * 255));
            canvas.drawCircle(0, 0, r * (1.04f + (1.0f - burstLife) * 0.14f), strokePaint);
        }

        canvas.restore();
    }

    private void drawHiraishinFallback(Canvas canvas, float r, double t) {
        // 4 кутові скоби формули просторово-часового бар'єра
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setColor(Color.parseColor("#ffe6a0"));
        strokePaint.setStrokeWidth(1.8f);
        strokePaint.setAlpha(130);
        for (int k = 0; k < 4; k++) {
            canvas.save();
            canvas.rotate((float) (k * 90));
            sealPath.reset();
            sealPath.moveTo(r * 0.72f, -r * 0.6f);
            sealPath.lineTo(r * 0.93f, -r * 0.6f);
            sealPath.lineTo(r * 0.93f, r * 0.28f);
            sealPath.moveTo(r * 0.93f, r * 0.38f);
            sealPath.lineTo(r * 0.93f, r * 0.6f);
            sealPath.lineTo(r * 0.76f, r * 0.6f);
            canvas.drawPath(sealPath, strokePaint);
            canvas.restore();
        }
    }

    public void drawTrail(Canvas canvas, String id, float ax, float ay, float bx, float by, float life, double t) {
        float alpha = Math.max(0f, Math.min(1f, life / 0.45f));
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeCap(Paint.Cap.ROUND);
        strokePaint.setStrokeWidth(3.0f);

        int color = Color.parseColor("#85e5ff");
        if ("chidori".equals(id)) color = Color.parseColor("#b8dfff");
        else if ("sharingan".equals(id)) color = Color.parseColor("#ff6265");
        else if ("kurama".equals(id)) color = Color.parseColor("#ff9a46");
        else if ("fireball".equals(id)) color = Color.parseColor("#ffb65c");
        else if ("amaterasu".equals(id)) color = Color.parseColor("#c3a0ed");
        else if ("hiraishin".equals(id)) color = Color.parseColor("#ffe59b");
        else if ("summoning".equals(id)) color = Color.parseColor("#d7c08b");
        else if ("rasenshuriken".equals(id)) color = Color.parseColor("#b7f1e5");

        strokePaint.setColor(color);
        strokePaint.setAlpha((int) (alpha * 0.65f * 255));

        trailPath.reset();
        trailPath.moveTo(ax, ay);
        float midX = (ax + bx) / 2f;
        float midY = (ay + by) / 2f - 30f;
        trailPath.quadTo(midX, midY, bx, by);
        canvas.drawPath(trailPath, strokePaint);
    }

    private void drawChidoriStroke(Canvas canvas, Path path, float alpha, float width, float r) {
        float dScale = density / 1.5f;
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeCap(Paint.Cap.ROUND);
        strokePaint.setStrokeJoin(Paint.Join.MITER);

        // 1. М'який неоновий ореол (відповідає shadowColor='#67c7ff', shadowBlur=r*0.085 у JS)
        strokePaint.setColor(COLOR_CHIDORI_GLOW);
        strokePaint.setStrokeWidth(Math.max(1.8f, (width * 3.0f + r * 0.032f) * dScale));
        strokePaint.setAlpha((int) (Math.min(1.0f, alpha * 0.38f) * 255));
        canvas.drawPath(path, strokePaint);

        // 2. Блакитне тіло розряду (відповідає strokeStyle='#459cff', lineWidth=width*3 у JS)
        strokePaint.setColor(COLOR_CHIDORI_BLUE);
        strokePaint.setStrokeWidth(Math.max(1.1f, width * 2.2f * dScale));
        strokePaint.setAlpha((int) (Math.min(1.0f, alpha * 0.85f) * 255));
        canvas.drawPath(path, strokePaint);

        // 3. Тонка надяскава біла нитка (відповідає strokeStyle='#ecfbff', lineWidth=width, shadowBlur=r*0.018 у JS)
        strokePaint.setColor(COLOR_CHIDORI_WHITE);
        strokePaint.setStrokeWidth(Math.max(0.6f, width * dScale));
        strokePaint.setAlpha((int) (Math.min(1.0f, alpha * 1.15f) * 255));
        canvas.drawPath(path, strokePaint);
    }

    /**
     * Точний 1:1 перенос функції electricity() з prototype/technique-art.js
     */
    private void drawChidori(Canvas canvas, float cx, float cy, float r, double t, float energy) {
        canvas.save();
        canvas.translate(cx, cy);

        long tick = (long) Math.floor(t * 24.0);
        float nX = (float) (Math.sin(t * 13.0) * r * 0.035);
        float nY = (float) (Math.cos(t * 11.0) * r * 0.035);

        // 22 розряди, що сходяться всередину до ядра
        for (int k = 0; k < 22; k++) {
            double angle = k * TAU / 22.0 + (noise(k + 3, tick) - 0.5) * 0.24;
            double reach = r * (0.96 + noise(k + 63, tick) * 0.34 + energy * 0.12);

            for (int j = 0; j <= 9; j++) {
                double u = (double) j / 9.0;
                double rr = reach * (1.0 - u);
                double side = (noise(k * 31 + j, tick) - 0.5) * r * 0.18 * Math.sin(u * Math.PI);
                chidoriPointsX[j] = (float) (nX + Math.cos(angle) * rr - Math.sin(angle) * side);
                chidoriPointsY[j] = (float) (nY + Math.sin(angle) * rr + Math.cos(angle) * side);
            }

            // Основний розряд:
            chidoriPath.reset();
            chidoriPath.moveTo(chidoriPointsX[0], chidoriPointsY[0]);
            for (int j = 1; j <= 9; j++) {
                chidoriPath.lineTo(chidoriPointsX[j], chidoriPointsY[j]);
            }
            float mainAlpha = (float) (0.32 + noise(k + 90, tick) * 0.4);
            float mainWidth = (float) (r * (0.006 + noise(k + 120, tick) * 0.006));
            drawChidoriStroke(canvas, chidoriPath, mainAlpha, mainWidth, r);

            // Два розгалуження (forks):
            for (int f = 0; f < 2; f++) {
                int joinIdx = 3 + f * 2;
                float joinX = chidoriPointsX[joinIdx];
                float joinY = chidoriPointsY[joinIdx];
                double a = angle + (f == 1 ? 1.0 : -1.0) * (0.4 + noise(k + f + 9, tick) * 0.6);

                chidoriForkPath.reset();
                for (int j = 0; j < 4; j++) {
                    double u = 1.0 - (double) j / 3.0;
                    float fx = (float) (joinX + Math.cos(a) * r * 0.28 * u + Math.sin(j * 4 + k + tick) * r * 0.025 * u);
                    float fy = (float) (joinY + Math.sin(a) * r * 0.28 * u);
                    if (j == 0) chidoriForkPath.moveTo(fx, fy);
                    else chidoriForkPath.lineTo(fx, fy);
                }
                float forkAlpha = (float) (0.35 + noise(k + 44, tick) * 0.4);
                float forkWidth = r * 0.005f;
                drawChidoriStroke(canvas, chidoriForkPath, forkAlpha, forkWidth, r);
            }

            // Біжучий імпульс заряду (OUTSIDE -> CENTER):
            double progress = (t * (0.85 + (k % 4) * 0.12) + k * 0.137) % 1.0;
            if (progress < 0) progress += 1.0;
            int index = (int) Math.floor(progress * 7.0);
            if (index > 7) index = 7;

            chidoriChargePath.reset();
            chidoriChargePath.moveTo(chidoriPointsX[index], chidoriPointsY[index]);
            chidoriChargePath.lineTo(chidoriPointsX[index + 1], chidoriPointsY[index + 1]);
            chidoriChargePath.lineTo(chidoriPointsX[index + 2], chidoriPointsY[index + 2]);

            float chargeAlpha = 0.7f + energy * 0.2f;
            float chargeWidth = r * 0.013f;
            drawChidoriStroke(canvas, chidoriChargePath, chargeAlpha, chargeWidth, r);
        }

        // Щільне живе ядро з 18 зигзагоподібних розрядів:
        for (int k = 0; k < 18; k++) {
            double a = k * 2.399 + t * 0.16;
            double rr = r * (0.12 + noise(k + 400, tick) * 0.17 + energy * 0.08);
            float p0x = (float) (nX + Math.cos(a) * rr);
            float p0y = (float) (nY + Math.sin(a) * rr);
            float p1x = (float) (nX + Math.cos(a + 0.6) * rr * 0.42);
            float p1y = (float) (nY + Math.sin(a + 0.6) * rr * 0.42);

            chidoriCorePath.reset();
            chidoriCorePath.moveTo(p0x, p0y);
            chidoriCorePath.lineTo(p1x, p1y);
            chidoriCorePath.lineTo(nX, nY);

            float coreAlpha = 0.8f;
            float coreWidth = (float) (r * (0.01 + noise(k + 800, tick) * 0.014));
            drawChidoriStroke(canvas, chidoriCorePath, coreAlpha, coreWidth, r);
        }

        // Кільце ударної хвилі при виборі картки:
        if (energy > 0f) {
            strokePaint.setStyle(Paint.Style.STROKE);
            strokePaint.setColor(COLOR_CHIDORI_GLOW);
            strokePaint.setStrokeWidth(dp(1.2f));
            strokePaint.setAlpha((int) (energy * 0.5f * 255));
            canvas.drawCircle(0, 0, r * (1.04f + (1.0f - energy) * 0.14f), strokePaint);
        }

        canvas.restore();
    }

    private double noise(double n, long tick) {
        double v = Math.sin(n * 127.1 + tick * 311.7) * 43758.5453;
        return v - Math.floor(v);
    }

    /**
     * C11: Гамабунта — виправлено протилежні орієнтації спрайтів, відновлення alpha 255
     */
    private void drawGamabunta(Canvas canvas, float cx, float cy, float r, long timeMs, boolean isPreview) {
        float perchX = cx + r * 0.5f;
        float perchY = cy - r * 0.26f;

        if (isPreview) {
            Bitmap idle = ensureBitmap("gamabunta-idle");
            if (idle != null && !idle.isRecycled()) {
                paint.reset();
                paint.setAntiAlias(true);
                paint.setFilterBitmap(true);
                paint.setAlpha(255);
                float size = r * 1.45f;
                dstRect.set(perchX - size / 2, perchY - size * 0.88f, perchX + size / 2, perchY + size * 0.12f);
                canvas.drawBitmap(idle, null, dstRect, paint);
            }
            return;
        }

        // 0. Summon landing seal on target card
        canvas.save();
        canvas.translate(cx, cy);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setColor(Color.parseColor("#c9b18a"));
        strokePaint.setStrokeWidth(1.1f);
        strokePaint.setAlpha((int) (0.25f * 255));
        canvas.drawCircle(0, 0, r * 1.14f, strokePaint);
        canvas.drawCircle(0, 0, r * 1.28f, strokePaint);
        for (int k = 0; k < 8; k++) {
            canvas.save();
            canvas.rotate(k * 45f);
            sealPath.reset();
            sealPath.moveTo(r * 1.03f, 0);
            sealPath.lineTo(r * 1.18f, 0);
            sealPath.lineTo(r * 1.18f, r * 0.055f);
            sealPath.lineTo(r * 1.32f, r * 0.055f);
            sealPath.moveTo(r * 1.22f, -r * 0.04f);
            sealPath.lineTo(r * 1.3f, -r * 0.04f);
            canvas.drawPath(sealPath, strokePaint);
            canvas.restore();
        }
        canvas.restore();

        long now = timeMs;
        float curX = perchX;
        float curY = perchY;
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
        float shadowY = perchY - curY;
        dstRect.set(curX - r * 0.34f, curY + shadowY + 4f, curX + r * 0.34f, curY + shadowY + 14f);
        canvas.drawOval(dstRect, paint);
        canvas.restore();

        // 2. Спрайт жаби (C11: обов'язкове скидання alpha у 255 та коректна інверсія орієнтації)
        String spriteKey = airborne ? "gamabunta-jump" : "gamabunta-idle";
        Bitmap frogBmp = ensureBitmap(spriteKey);
        if (frogBmp != null && !frogBmp.isRecycled()) {
            canvas.save();
            canvas.translate(curX, curY);

            double bounce = (!airborne && frogState.landTime > 0)
                    ? Math.sin((now - frogState.landTime) / 1000.0 * 19.0) * Math.exp(-(now - frogState.landTime) / 1000.0 * 8.0) * 0.13
                    : 0.0;
            float scaleX = (airborne ? -dir : dir) * (1.0f + (airborne ? -0.06f : (float) bounce));
            float scaleY = 1.0f + (airborne ? 0.12f : (float) -bounce);
            canvas.scale(scaleX, scaleY);

            paint.reset();
            paint.setAntiAlias(true);
            paint.setFilterBitmap(true);
            paint.setAlpha(255);

            float size = r * 1.45f;
            dstRect.set(-size / 2, -size * 0.88f, size / 2, size * 0.12f);
            canvas.drawBitmap(frogBmp, null, dstRect, paint);
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

    private Bitmap smartTubeIcon;
    private Bitmap lampaIcon;

    private synchronized Bitmap getSmartTubeIcon() {
        if (smartTubeIcon == null || smartTubeIcon.isRecycled()) {
            try (InputStream is = context.getAssets().open("icons/icon-smarttube.webp")) {
                smartTubeIcon = BitmapFactory.decodeStream(is);
            } catch (Exception ignored) {}
        }
        return smartTubeIcon;
    }

    private synchronized Bitmap getLampaIcon() {
        if (lampaIcon == null || lampaIcon.isRecycled()) {
            try (InputStream is = context.getAssets().open("icons/icon-lampa.webp")) {
                lampaIcon = BitmapFactory.decodeStream(is);
            } catch (Exception ignored) {}
        }
        return lampaIcon;
    }

    /**
     * Одноразове малювання статичного прев'ю техніки для меню вибору
     */
    public void drawStaticPreview(Canvas canvas, String id, int width, int height) {
        float cx = width / 2f;
        float cy = height / 2f;
        float r = Math.min(width, height) * 0.28f;

        // 1. Техніка аури або печатки призову позаду іконки
        drawTechnique(canvas, id, cx, cy, r, 0, 0, true);

        // 2. Зразок іконки SmartTube по центру
        Bitmap st = getSmartTubeIcon();
        if (st != null && !st.isRecycled()) {
            float iconSize = Math.min(width, height) * 0.46f;
            dstRect.set(cx - iconSize / 2f, cy - iconSize / 2f, cx + iconSize / 2f, cy + iconSize / 2f);
            paint.reset();
            paint.setAntiAlias(true);
            paint.setFilterBitmap(true);
            canvas.drawBitmap(st, null, dstRect, paint);
        }

        // 3. Для Гамабунти — жаба сидить зверху на іконці SmartTube
        if ("gamabunta".equals(id)) {
            Bitmap idle = ensureBitmap("gamabunta-idle");
            if (idle != null && !idle.isRecycled()) {
                paint.reset();
                paint.setAntiAlias(true);
                paint.setFilterBitmap(true);
                paint.setAlpha(255);
                float size = r * 1.45f;
                float perchX = cx + r * 0.5f;
                float perchY = cy - r * 0.26f;
                dstRect.set(perchX - size / 2f, perchY - size * 0.88f, perchX + size / 2f, perchY + size * 0.12f);
                canvas.drawBitmap(idle, null, dstRect, paint);
            }
        }
    }
}
