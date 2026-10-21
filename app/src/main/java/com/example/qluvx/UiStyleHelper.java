package com.example.qluvx;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class UiStyleHelper {

    public static final int COLOR_BG_DEEP = Color.rgb(8, 8, 14);
    public static final int COLOR_CARD_BG = Color.rgb(18, 18, 30);
    public static final int COLOR_CARD_ELEVATED = Color.rgb(26, 26, 44);
    public static final int COLOR_NEON_PINK = Color.rgb(255, 55, 180);
    public static final int COLOR_ELECTRIC_VIOLET = Color.rgb(124, 77, 255);
    public static final int COLOR_CYBER_CYAN = Color.rgb(0, 229, 255);
    public static final int COLOR_LIVE_GREEN = Color.rgb(0, 230, 118);
    public static final int COLOR_MUTED_RED = Color.rgb(255, 64, 96);
    public static final int COLOR_TEXT_SECONDARY = Color.rgb(175, 178, 205);

    public static int dp(Context context, float value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                context.getResources().getDisplayMetrics()
        );
    }

    public static GradientDrawable roundedBox(Context context, int fillColor, float cornerDp, int strokeColor, float strokeDp) {
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.RECTANGLE);
        gd.setColor(fillColor);
        gd.setCornerRadius(dp(context, cornerDp));
        if (strokeDp > 0) {
            gd.setStroke(dp(context, strokeDp), strokeColor);
        }
        return gd;
    }

    public static GradientDrawable gradientBox(Context context, int startColor, int endColor, float cornerDp, int strokeColor, float strokeDp) {
        GradientDrawable gd = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{startColor, endColor}
        );
        gd.setShape(GradientDrawable.RECTANGLE);
        gd.setCornerRadius(dp(context, cornerDp));
        if (strokeDp > 0) {
            gd.setStroke(dp(context, strokeDp), strokeColor);
        }
        return gd;
    }

    public static RippleDrawable rippleButton(Context context, int fillColor, float cornerDp, int strokeColor, float strokeDp) {
        GradientDrawable content = roundedBox(context, fillColor, cornerDp, strokeColor, strokeDp);
        GradientDrawable mask = roundedBox(context, Color.WHITE, cornerDp, 0, 0);
        return new RippleDrawable(ColorStateList.valueOf(Color.argb(75, 255, 255, 255)), content, mask);
    }

    public static RippleDrawable rippleGradientButton(Context context, int startColor, int endColor, float cornerDp) {
        GradientDrawable content = gradientBox(context, startColor, endColor, cornerDp, 0, 0);
        GradientDrawable mask = roundedBox(context, Color.WHITE, cornerDp, 0, 0);
        return new RippleDrawable(ColorStateList.valueOf(Color.argb(90, 255, 255, 255)), content, mask);
    }

    public static TextView createPillButton(Context context, String label, int bgColor, int textColor, View.OnClickListener listener) {
        TextView btn = new TextView(context);
        btn.setText(label);
        btn.setTextColor(textColor);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        btn.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        btn.setGravity(Gravity.CENTER);
        btn.setMinHeight(dp(context, 48));
        btn.setMinimumHeight(dp(context, 48));
        btn.setPadding(dp(context, 16), dp(context, 10), dp(context, 16), dp(context, 10));
        btn.setBackground(rippleButton(context, bgColor, 24f, Color.argb(55, 255, 255, 255), 1f));
        btn.setContentDescription(label);
        if (listener != null) {
            btn.setOnClickListener(listener);
        }
        return btn;
    }

    public static TextView createTagBadge(Context context, String text, int fillColor, int textColor) {
        TextView badge = new TextView(context);
        badge.setText(text);
        badge.setTextColor(textColor);
        badge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
        badge.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        badge.setPadding(dp(context, 8), dp(context, 3), dp(context, 8), dp(context, 3));
        badge.setBackground(roundedBox(context, fillColor, 10f, 0, 0));
        return badge;
    }

    public static LinearLayout.LayoutParams params(int w, int h, int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(w, h);
        lp.setMargins(left, top, right, bottom);
        return lp;
    }
}
