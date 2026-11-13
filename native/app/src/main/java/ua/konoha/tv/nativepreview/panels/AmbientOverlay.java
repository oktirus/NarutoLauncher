package ua.konoha.tv.nativepreview.panels;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AmbientOverlay extends FrameLayout {
    public interface OnAmbientExitListener {
        void onAmbientExit();
    }

    private final TextView timeView;
    private final TextView dateView;
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
        setBackgroundColor(Color.parseColor("#d9061b27"));
        setFocusable(true);
        setFocusableInTouchMode(true);

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);

        TextView brand = new TextView(context);
        brand.setText("Коноха · Тихі миті теж стають спогадами.");
        brand.setTextColor(Color.parseColor("#e0d7b9"));
        brand.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        brand.setGravity(Gravity.CENTER);
        layout.addView(brand);

        timeView = new TextView(context);
        timeView.setTextColor(Color.WHITE);
        timeView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 84);
        timeView.setTypeface(Typeface.DEFAULT_BOLD);
        timeView.setGravity(Gravity.CENTER);
        timeView.setPadding(0, dp(12), 0, 0);
        layout.addView(timeView);

        dateView = new TextView(context);
        dateView.setTextColor(Color.parseColor("#d8d8be"));
        dateView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        dateView.setGravity(Gravity.CENTER);
        layout.addView(dateView);

        TextView quote = new TextView(context);
        quote.setText("«Там, де тебе чекають, завжди є дім.»");
        quote.setTextColor(Color.parseColor("#f2c576"));
        quote.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        quote.setGravity(Gravity.CENTER);
        quote.setPadding(0, dp(16), 0, dp(24));
        layout.addView(quote);

        Button exitBtn = new Button(context);
        exitBtn.setText("OK / Back — повернутися");
        exitBtn.setTextColor(Color.parseColor("#c2cbbb"));
        exitBtn.setBackgroundColor(Color.parseColor("#153745"));
        exitBtn.setOnClickListener(v -> exit());
        layout.addView(exitBtn);

        LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.CENTER;
        addView(layout, lp);

        updateTime();
    }

    public void setOnAmbientExitListener(OnAmbientExitListener listener) {
        this.exitListener = listener;
    }

    public void start() {
        setVisibility(VISIBLE);
        requestFocus();
        timeHandler.removeCallbacks(timeRunnable);
        timeHandler.post(timeRunnable);
    }

    public void stop() {
        timeHandler.removeCallbacks(timeRunnable);
        setVisibility(GONE);
    }

    public void pause() {
        timeHandler.removeCallbacks(timeRunnable);
    }

    public void resume() {
        if (getVisibility() == VISIBLE) {
            timeHandler.removeCallbacks(timeRunnable);
            timeHandler.post(timeRunnable);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        timeHandler.removeCallbacks(timeRunnable);
    }

    private void updateTime() {
        Date now = new Date();
        SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm", new Locale("uk", "UA"));
        SimpleDateFormat dateFmt = new SimpleDateFormat("EEEE, d MMMM", new Locale("uk", "UA"));
        timeView.setText(timeFmt.format(now));
        dateView.setText(dateFmt.format(now));
    }

    private void exit() {
        stop();
        if (exitListener != null) exitListener.onAmbientExit();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        exit();
        return true;
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics()
        );
    }
}
