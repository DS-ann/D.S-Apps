package com.dsann.launcher;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.widget.TextView;

/** Small API-23-compatible view and density helpers shared by launcher screens. */
final class LauncherUiUtils {
    private LauncherUiUtils() {}

    static TextView text(Context context, String value, float size, int color, boolean bold) {
        TextView view = new TextView(context);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    static GradientDrawable roundRect(Context context, int[] colors, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, colors);
        drawable.setCornerRadius(dp(context, radiusDp));
        return drawable;
    }

    static int dp(Context context, float value) {
        return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
    }
}
