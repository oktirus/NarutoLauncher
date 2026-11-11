package ua.konoha.tv.nativepreview.theme;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;

/**
 * Visual design system tokens for NarutoLauncher (matching Chromium v26).
 */
public final class KonohaTheme {
    private KonohaTheme() {}

    // Colors matching style.css and CSS cascade
    public static final int COLOR_BG = Color.parseColor("#071c27");
    public static final int COLOR_INK = Color.parseColor("#102b38");
    public static final int COLOR_PAPER = Color.parseColor("#f5e3b6");
    public static final int COLOR_ORANGE = Color.parseColor("#ed943f");
    public static final int COLOR_GOLD = Color.parseColor("#f2c576");
    public static final int COLOR_MUTED = Color.parseColor("#92aaa4");
    public static final int COLOR_MUTED_LIGHT = Color.parseColor("#d3d3bc");
    public static final int COLOR_WHITE = Color.WHITE;
    public static final int COLOR_PANEL_START = Color.parseColor("#173b47");
    public static final int COLOR_PANEL_END = Color.parseColor("#0d2936");
    public static final int COLOR_CARD_BG = Color.parseColor("#102b38");
    public static final int COLOR_CARD_ACTIVE = Color.parseColor("#2e2618");
    public static final int COLOR_CARD_BORDER = Color.parseColor("#f2c576");
    public static final int COLOR_BUTTON_BG = Color.parseColor("#122b36");
    public static final int COLOR_TRANSLUCENT_BLACK = Color.parseColor("#77071c27");

    // Cached Typefaces
    private static Typeface nunitoRegular;
    private static Typeface nunitoBold;
    private static Typeface caveatRegular;
    private static Typeface caveatBold;

    public static synchronized Typeface getNunito(Context context) {
        if (nunitoRegular == null) {
            try {
                nunitoRegular = Typeface.createFromAsset(context.getAssets(), "fonts/NunitoSans.ttf");
            } catch (Exception e) {
                nunitoRegular = Typeface.SANS_SERIF;
            }
        }
        return nunitoRegular;
    }

    public static synchronized Typeface getNunitoBold(Context context) {
        if (nunitoBold == null) {
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    nunitoBold = new Typeface.Builder(context.getAssets(), "fonts/NunitoSans.ttf")
                            .setFontVariationSettings("'wght' 700")
                            .build();
                }
            } catch (Exception ignored) {}
            if (nunitoBold == null) {
                try {
                    nunitoBold = Typeface.create(getNunito(context), Typeface.BOLD);
                } catch (Exception e) {
                    nunitoBold = getNunito(context);
                }
            }
        }
        return nunitoBold != null ? nunitoBold : getNunito(context);
    }

    public static synchronized Typeface getCaveat(Context context) {
        if (caveatRegular == null) {
            try {
                caveatRegular = Typeface.createFromAsset(context.getAssets(), "fonts/Caveat.ttf");
            } catch (Exception e) {
                caveatRegular = Typeface.SERIF;
            }
        }
        return caveatRegular;
    }

    public static synchronized Typeface getCaveatBold(Context context) {
        if (caveatBold == null) {
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    caveatBold = new Typeface.Builder(context.getAssets(), "fonts/Caveat.ttf")
                            .setFontVariationSettings("'wght' 700")
                            .build();
                }
            } catch (Exception ignored) {}
            if (caveatBold == null) {
                try {
                    caveatBold = Typeface.create(getCaveat(context), Typeface.BOLD);
                } catch (Exception e) {
                    caveatBold = getCaveat(context);
                }
            }
        }
        return caveatBold != null ? caveatBold : getCaveat(context);
    }

    public static int dp(Context context, float value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, context.getResources().getDisplayMetrics()
        );
    }

    public static GradientDrawable createPanelBackground() {
        GradientDrawable gd = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{COLOR_PANEL_START, COLOR_PANEL_END}
        );
        gd.setGradientType(GradientDrawable.LINEAR_GRADIENT);
        return gd;
    }

    public static GradientDrawable createRoundedBorder(int bgColor, int borderColor, float radiusPx, float strokePx) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(bgColor);
        gd.setCornerRadius(radiusPx);
        if (strokePx > 0) {
            gd.setStroke((int) strokePx, borderColor);
        }
        return gd;
    }
}
