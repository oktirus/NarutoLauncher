package ua.konoha.tv.nativepreview.views;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.TextView;

public class ToastOverlay extends FrameLayout {
    private final TextView messageView;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable hideRunnable = () -> setVisibility(GONE);

    public ToastOverlay(Context context) {
        super(context);
        setVisibility(GONE);

        messageView = new TextView(context);
        messageView.setTextColor(Color.parseColor("#213a40"));
        messageView.setBackgroundColor(Color.parseColor("#f4dfb6"));
        messageView.setTypeface(Typeface.DEFAULT_BOLD);
        messageView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        messageView.setPadding(dp(22), dp(12), dp(22), dp(12));
        messageView.setGravity(Gravity.CENTER);

        LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        lp.bottomMargin = dp(32);
        addView(messageView, lp);
    }

    public void show(String message, long durationMs) {
        handler.removeCallbacks(hideRunnable);
        messageView.setText(message);
        setVisibility(VISIBLE);
        handler.postDelayed(hideRunnable, durationMs);
    }

    public void hide() {
        handler.removeCallbacks(hideRunnable);
        setVisibility(GONE);
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics()
        );
    }
}
