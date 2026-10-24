package com.dsann.launcher;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Shared animation timing for all timed launcher motion.
 *
 * The slider stores a speed percentage: 100 is normal, lower values are slower.
 * The supported range ends at 100 so full slider travel corresponds to normal speed.
 * Existing 0/1/2 preset preferences migrate once to approximate speed percentages.
 */
final class LauncherMotion {
    private static final String PREFS = "ds_launcher_prefs";
    private static final String KEY_ANIMATION_SPEED = "animation_speed";
    private static final int DEFAULT_SPEED_PERCENT = 100;
    private static final int MIN_SPEED_PERCENT = 50;
    private static final int MAX_SPEED_PERCENT = 100;

    private LauncherMotion() {}

    static int speedPercent(Context context) {
        SharedPreferences preferences =
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (!preferences.contains(KEY_ANIMATION_SPEED)) return DEFAULT_SPEED_PERCENT;

        int stored = preferences.getInt(KEY_ANIMATION_SPEED, DEFAULT_SPEED_PERCENT);
        if (stored >= 0 && stored <= 2) {
            // Previous setting: Slow (0), Medium (1), Fast (2).
            stored = stored == 0 ? 67 : (stored == 1 ? 100 : 154);
            preferences.edit().putInt(KEY_ANIMATION_SPEED, stored).apply();
        }
        int clamped = Math.max(MIN_SPEED_PERCENT, Math.min(MAX_SPEED_PERCENT, stored));
        if (clamped != stored) {
            preferences.edit().putInt(KEY_ANIMATION_SPEED, clamped).apply();
        }
        return clamped;
    }

    static long duration(Context context, long baseMillis) {
        int speed = speedPercent(context);
        long scaled = Math.round(Math.max(0L, baseMillis) * (100f / speed));
        return Math.max(20L, scaled);
    }
}
