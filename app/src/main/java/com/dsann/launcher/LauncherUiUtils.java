package com.dsann.launcher;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.TextView;
import android.view.View;
import java.util.Map;
import java.util.WeakHashMap;

/** Shared view and density helpers used across launcher screens. */
final class LauncherUiUtils {
    private static final Map<View, ObjectAnimator[]> MOVE_HINTS = new WeakHashMap<>();

    private LauncherUiUtils() {}

    /** Subtle move-ready feedback: 20% scale-up with a low-amplitude dance. */
    static void startMoveHint(View view, Context context) {
        if (view == null) return;
        stopMoveHint(view);
        view.setScaleX(1.2f);
        view.setScaleY(1.2f);
        ObjectAnimator rotation = ObjectAnimator.ofFloat(view, View.ROTATION, -2f, 2f, -2f);
        rotation.setDuration(420L);
        rotation.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        ObjectAnimator bob = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y,
                -dp(context, 1.5f), dp(context, 1.5f), -dp(context, 1.5f));
        bob.setDuration(420L);
        bob.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        MOVE_HINTS.put(view, new ObjectAnimator[]{rotation, bob});
        rotation.start();
        bob.start();
    }

    static void stopMoveHint(View view) {
        if (view == null) return;
        ObjectAnimator[] animators = MOVE_HINTS.remove(view);
        if (animators != null) {
            for (ObjectAnimator animator : animators) {
                if (animator != null) animator.cancel();
            }
        }
        view.setRotation(0f);
        view.setTranslationY(0f);
        view.setScaleX(1f);
        view.setScaleY(1f);
    }

    static int dp(Context context, float value) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                context.getResources().getDisplayMetrics()));
    }

    static TextView text(Context context, String value, float sizeSp, int color, boolean bold) {
        TextView view = new TextView(context);
        view.setText(value);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
        view.setTextColor(color);
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setIncludeFontPadding(false);
        view.setTypeface(android.graphics.Typeface.create(
                "sans-serif", bold ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL));
        view.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT);
        return view;
    }

    static GradientDrawable roundRect(Context context, int[] colors, float radiusPx) {
        GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.TL_BR, colors);
        drawable.setCornerRadius(radiusPx);
        return drawable;
    }
}
