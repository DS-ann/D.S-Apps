package com.dsann.launcher;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.SharedPreferences;
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
    private static final Map<View, float[]> MOVE_HINT_BASES = new WeakHashMap<>();

    private LauncherUiUtils() {}

    /** Subtle move-ready feedback: scale-up with a low-amplitude dance. */
    static void startMoveHint(View view, Context context) {
        startMoveHint(view, context, true);
    }

    /** Static move-ready feedback for position-sensitive items: enlarge without dancing. */
    static void startScaleHint(View view, float scale) {
        if (view == null) return;
        stopMoveHint(view);
        float baseRotation = view.getRotation();
        float baseTranslationY = view.getTranslationY();
        float baseScaleX = view.getScaleX();
        float baseScaleY = view.getScaleY();
        MOVE_HINT_BASES.put(view, new float[]{baseRotation, baseTranslationY, baseScaleX, baseScaleY});
        view.setScaleX(baseScaleX * scale);
        view.setScaleY(baseScaleY * scale);
    }

    /** Allows position-sensitive widgets (such as the clock) to dance without bobbing. */
    static void startMoveHint(View view, Context context, boolean verticalBob) {
        if (view == null) return;
        stopMoveHint(view);
        float baseRotation = view.getRotation();
        float baseTranslationY = view.getTranslationY();
        float baseScaleX = view.getScaleX();
        float baseScaleY = view.getScaleY();
        MOVE_HINT_BASES.put(view, new float[]{baseRotation, baseTranslationY, baseScaleX, baseScaleY});
        view.setScaleX(baseScaleX * 1.2f);
        view.setScaleY(baseScaleY * 1.2f);
        ObjectAnimator rotation = ObjectAnimator.ofFloat(view, View.ROTATION,
                baseRotation - 2f, baseRotation + 2f, baseRotation - 2f);
        rotation.setDuration(LauncherMotion.duration(context, 420L));
        rotation.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        if (verticalBob) {
            ObjectAnimator bob = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y,
                    baseTranslationY - dp(context, 1.5f),
                    baseTranslationY + dp(context, 1.5f),
                    baseTranslationY - dp(context, 1.5f));
            bob.setDuration(LauncherMotion.duration(context, 420L));
            bob.setRepeatCount(android.animation.ValueAnimator.INFINITE);
            MOVE_HINTS.put(view, new ObjectAnimator[]{rotation, bob});
            bob.start();
        } else {
            MOVE_HINTS.put(view, new ObjectAnimator[]{rotation});
        }
        rotation.start();
    }

    static void stopMoveHint(View view) {
        if (view == null) return;
        ObjectAnimator[] animators = MOVE_HINTS.remove(view);
        if (animators != null) {
            for (ObjectAnimator animator : animators) {
                if (animator != null) animator.cancel();
            }
        }
        float[] base = MOVE_HINT_BASES.remove(view);
        if (base != null) {
            view.setRotation(base[0]);
            view.setTranslationY(base[1]);
            view.setScaleX(base[2]);
            view.setScaleY(base[3]);
        } else {
            view.setRotation(0f);
            view.setTranslationY(0f);
            view.setScaleX(1f);
            view.setScaleY(1f);
        }
    }

    static int dp(Context context, float value) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                context.getResources().getDisplayMetrics()));
    }

    /**
     * Reads a floating-point preference and repairs legacy numeric values written
     * with another SharedPreferences type. This prevents reconstruction crashes
     * after an app update or process recreation.
     */
    static float getFloatPreferenceCompat(SharedPreferences preferences, String key,
                                          float fallback) {
        if (preferences == null) return fallback;
        try {
            return preferences.getFloat(key, fallback);
        } catch (ClassCastException legacyNumericType) {
            Object value = preferences.getAll().get(key);
            if (value instanceof Number) {
                float converted = ((Number) value).floatValue();
                preferences.edit().putFloat(key, converted).apply();
                return converted;
            }
            return fallback;
        }
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
        GradientDrawable drawable;
        if (colors == null || colors.length == 0) {
            // An empty palette should still draw safely and consistently.
            drawable = new GradientDrawable();
            drawable.setColor(Color.TRANSPARENT);
        } else if (colors.length == 1) {
            // A one-color palette is a solid fill, not a gradient. Passing it to the
            // orientation constructor triggers LinearGradient's "needs >= 2 colors"
            // exception when Android draws the view background.
            drawable = new GradientDrawable();
            drawable.setColor(colors[0]);
        } else {
            drawable = new GradientDrawable(GradientDrawable.Orientation.TL_BR, colors);
        }
        drawable.setCornerRadius(radiusPx);
        return drawable;
    }
}
