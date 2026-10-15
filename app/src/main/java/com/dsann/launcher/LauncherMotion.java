package com.dsann.launcher;

import android.content.Context;

/** Shared animation timing so the Home Studio speed setting affects every launcher transition. */
final class LauncherMotion {
    private LauncherMotion() {}

    static long duration(Context context, long baseMillis) {
        int speed = context.getSharedPreferences("ds_launcher_prefs", Context.MODE_PRIVATE)
                .getInt("animation_speed", 1);
        float multiplier = speed == 0 ? 1.5f : (speed == 2 ? 0.65f : 1f);
        return Math.max(60L, Math.round(baseMillis * multiplier));
    }
}
