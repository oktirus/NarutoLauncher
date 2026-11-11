package ua.konoha.tv.nativepreview.panels;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import ua.konoha.tv.nativepreview.theme.KonohaTheme;
import ua.konoha.tv.nativepreview.views.GlyphIconView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Ambient screen ("Тиха мить") reproducing Chromium's #ambient.
 * Left-aligned giant Caveat clock, date, Caveat quote, and headband brand badge.
 */
public class AmbientOverlay extends FrameLayout {
    public interface OnAmbientExitListener {
        void onAmbientExit();
    }

    private final TextView timeView;
    private final TextView dateView;
    private final TextView quoteView;
    private final FrameLayout exitBtn;
    private final Handler timeHandler = new Handler(Looper.getMainLooper());
    private OnAmbientExitListener exitListener;

    private final Runnable timeRunnable = new Runnable() {
        @Override
        public void run() {
            updateTime();
            timeHandler.postDelayed(this, 10000);
        }
    };

    public AmbientOverlay(Context context) {
        super(context);
        // Translucent gradient from left so wallpaper remains visible
        GradientDrawable bg = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{Color.parseColor("#99061b27"), Color.parseColor("#1a061b27")}
        );
        setBackground(bg);
        setFocusable(true);
        setFocusableInTouchMode(true);
        setClickable(true);

        int padH = KonohaTheme.dp(context, 56);
        int padV = KonohaTheme.dp(context, 40);
        setPadding(padH, padV, padH, padV);

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);

        // 1. Brand badge top left
        LinearLayout brandBox = new LinearLayout(context);
        brandBox.setOrientation(LinearLayout.HORIZONTAL);
        brandBox.setGravity(Gravity.CENTER_VERTICAL);

        GlyphIconView leafIcon = new GlyphIconView(context);
        leafIcon.setGlyph(GlyphIconView.Glyph.LEAF_HEADBAND);
        leafIcon.setIconColor(KonohaTheme.COLOR_PAPER);
        int lSize = KonohaTheme.dp(context, 24);
        brandBox.addView(leafIcon, new LinearLayout.LayoutParams(lSize, lSize));

        TextView brandText = new TextView(context);
        brandText.setText("Тихі миті теж стають спогадами.");
        brandText.setTextColor(Color.parseColor("#e0d7b9"));
        brandText.setTextSize(12);
        brandText.setTypeface(KonohaTheme.getNunito(context));
        brandText.setPadding(KonohaTheme.dp(context, 10), 0, 0, 0);
        brandBox.addView(brandText);

        layout.addView(brandBox);

        // Spacer
        View s1 = new View(context);
        layout.addView(s1, new LinearLayout.LayoutParams(1, 0, 1.0f));

        // 2. Giant Caveat clock
        timeView = new TextView(context);
        timeView.setTextColor(KonohaTheme.COLOR_PAPER);
        timeView.setTextSize(110);
        timeView.setTypeface(KonohaTheme.getCaveatBold(context));
        timeView.setShadowLayer(25f, 0, 8f, Color.parseColor("#99000000"));
        layout.addView(timeView);

        // 3. Date
        dateView = new TextView(context);
        dateView.setTextColor(Color.parseColor("#d8d8be"));
        dateView.setTextSize(16);
        dateView.setTypeface(KonohaTheme.getNunito(context));
        dateView.setPadding(0, KonohaTheme.dp(context, 8), 0, 0);
        layout.addView(dateView);

        // 4. Quote
        quoteView = new TextView(context);
        quoteView.setText("Там, де тебе чекають, завжди є дім.");
        quoteView.setTextColor(Color.parseColor("#d8d4b4"));
        quoteView.setTextSize(24);
        quoteView.setTypeface(KonohaTheme.getCaveatBold(context));
        quoteView.setPadding(0, KonohaTheme.dp(context, 20), 0, 0);
        layout.addView(quoteView);

        // Spacer
        View s2 = new View(context);
        layout.addView(s2, new LinearLayout.LayoutParams(1, 0, 1.0f));

        // 5. Exit button bottom left
        exitBtn = new FrameLayout(context);
        exitBtn.setFocusable(true);
        exitBtn.setFocusableInTouchMode(true);
        exitBtn.setClickable(true);
        exitBtn.setPadding(KonohaTheme.dp(context, 14), KonohaTheme.dp(context, 8),
                KonohaTheme.dp(context, 14), KonohaTheme.dp(context, 8));

        GradientDrawable btnBg = new GradientDrawable();
        btnBg.setColor(Color.parseColor("#44102b38"));
        btnBg.setCornerRadius(KonohaTheme.dp(context, 4));
        btnBg.setStroke(KonohaTheme.dp(context, 1), Color.parseColor("#44dbbd7d"));
        exitBtn.setBackground(btnBg);

        TextView exitText = new TextView(context);
        exitText.setText("OK / Back — повернутися");
        exitText.setTextColor(Color.parseColor("#c2cbbb"));
        exitText.setTextSize(12);
        exitText.setTypeface(KonohaTheme.getNunito(context));
        exitBtn.addView(exitText);

        exitBtn.setOnClickListener(v -> exit());
        exitBtn.setOnFocusChangeListener((v, hasFocus) -> {
            GradientDrawable d = new GradientDrawable();
            d.setColor(hasFocus ? Color.parseColor("#80173b47") : Color.parseColor("#44102b38"));
            d.setCornerRadius(KonohaTheme.dp(getContext(), 4));
            d.setStroke(KonohaTheme.dp(getContext(), hasFocus ? 2 : 1),
                    hasFocus ? KonohaTheme.COLOR_ORANGE : Color.parseColor("#44dbbd7d"));
            v.setBackground(d);
        });

        LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        layout.addView(exitBtn, bLp);

        addView(layout, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ));

        setOnClickListener(v -> exit());
        updateTime();
    }

    public void setOnAmbientExitListener(OnAmbientExitListener listener) {
        this.exitListener = listener;
    }

    public void start() {
        setVisibility(VISIBLE);
        updateTime();
        timeHandler.removeCallbacks(timeRunnable);
        timeHandler.post(timeRunnable);
        post(() -> exitBtn.requestFocus());
    }

    public void stop() {
        setVisibility(GONE);
        timeHandler.removeCallbacks(timeRunnable);
    }

    public void resume() {
        if (getVisibility() == VISIBLE) {
            updateTime();
            timeHandler.removeCallbacks(timeRunnable);
            timeHandler.post(timeRunnable);
            post(() -> exitBtn.requestFocus());
        }
    }

    public void pause() {
        timeHandler.removeCallbacks(timeRunnable);
    }

    private void exit() {
        if (exitListener != null) exitListener.onAmbientExit();
    }

    private void updateTime() {
        Date now = new Date();
        SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm", new Locale("uk", "UA"));
        SimpleDateFormat dateFmt = new SimpleDateFormat("EEEE, d MMMM", new Locale("uk", "UA"));
        timeView.setText(timeFmt.format(now));
        dateView.setText(dateFmt.format(now));
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        updateTime();
        timeHandler.postDelayed(timeRunnable, 10000);
        post(() -> exitBtn.requestFocus());
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        timeHandler.removeCallbacks(timeRunnable);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ENTER
                || keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_MENU) {
            exit();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
