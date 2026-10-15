package com.dsann.launcher;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.view.Gravity;
import android.widget.FrameLayout;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/** Loads saved photo wallpapers and applies the launcher's theme fallback. */
final class LauncherWallpaperController {
    private LauncherWallpaperController() {}

    static boolean apply(Activity activity, FrameLayout root, SharedPreferences prefs,
                         String wallpaperKey, String themeKey, int fallbackColor) {
        if (root == null) return false;
        String saved = prefs == null ? null : prefs.getString(wallpaperKey, null);
        if (saved != null && !saved.isEmpty()) {
            try {
                Uri uri = Uri.parse(saved);
                BitmapFactory.Options bounds = new BitmapFactory.Options();
                bounds.inJustDecodeBounds = true;
                try (InputStream in = openStream(activity, uri)) {
                    if (in != null) BitmapFactory.decodeStream(in, null, bounds);
                }
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                    throw new IOException("Image could not be decoded");
                }
                int screenW = Math.max(1, activity.getResources().getDisplayMetrics().widthPixels);
                int screenH = Math.max(1, activity.getResources().getDisplayMetrics().heightPixels);
                int sample = 1;
                while (bounds.outWidth / (sample * 2) > screenW
                        || bounds.outHeight / (sample * 2) > screenH) sample *= 2;
                BitmapFactory.Options decode = new BitmapFactory.Options();
                decode.inSampleSize = sample;
                try (InputStream in = openStream(activity, uri)) {
                    android.graphics.Bitmap bitmap = in == null ? null
                            : BitmapFactory.decodeStream(in, null, decode);
                    if (bitmap != null) {
                        BitmapDrawable wallpaper = new BitmapDrawable(activity.getResources(), bitmap);
                        wallpaper.setGravity(Gravity.FILL);
                        root.setBackground(wallpaper);
                        return true;
                    }
                }
                throw new IOException("Image could not be decoded");
            } catch (Exception ignored) {
                if (prefs != null) prefs.edit().remove(wallpaperKey).apply();
            }
        }
        int theme = prefs == null ? 0 : prefs.getInt(themeKey, 0);
        if (theme == 1) {
            root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                    new int[]{Color.rgb(5, 35, 52), Color.rgb(7, 65, 84), fallbackColor}));
        } else if (theme == 2) {
            root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                    new int[]{Color.rgb(43, 19, 69), Color.rgb(65, 39, 104), fallbackColor}));
        } else if (theme == 3) {
            root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                    new int[]{Color.rgb(42, 46, 58), Color.rgb(25, 29, 39), fallbackColor}));
        } else {
            root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                    new int[]{Color.rgb(11, 22, 51), Color.rgb(20, 42, 88), fallbackColor}));
        }
        return false;
    }

    private static InputStream openStream(Activity activity, Uri uri) throws IOException {
        if ("file".equalsIgnoreCase(uri.getScheme())) {
            return new FileInputStream(new File(uri.getPath()));
        }
        return activity.getContentResolver().openInputStream(uri);
    }
}
