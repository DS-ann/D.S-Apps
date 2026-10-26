package com.dsann.launcher;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
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
                    Bitmap bitmap = in == null ? null
                            : BitmapFactory.decodeStream(in, null, decode);
                    if (bitmap != null) {
                        if (activity.getResources().getConfiguration().orientation
                                == Configuration.ORIENTATION_LANDSCAPE) {
                            // Tablet-style landscape wallpaper: fill the screen without
                            // distorting the image by scaling it proportionally and cropping
                            // equal amounts from the top/bottom or left/right around the center.
                            root.setBackground(new CenterCropDrawable(bitmap));
                        } else {
                            // Keep the existing portrait wallpaper appearance unchanged.
                            BitmapDrawable wallpaper = new BitmapDrawable(
                                    activity.getResources(), bitmap);
                            wallpaper.setGravity(Gravity.FILL);
                            root.setBackground(wallpaper);
                        }
                        return true;
                    }
                }
                throw new IOException("Image could not be decoded");
            } catch (Exception ignored) {
                // Keep the saved URI: provider access can fail temporarily, and deleting
                // it here would make a transient read error permanently forget the wallpaper.
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

    /**
     * Draws a bitmap with CENTER_CROP semantics using the actual background bounds.
     * Unlike Gravity.FILL, the source bitmap keeps its aspect ratio in landscape.
     */
    private static final class CenterCropDrawable extends Drawable {
        private final Bitmap bitmap;
        private final Paint paint = new Paint(
                Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);

        CenterCropDrawable(Bitmap bitmap) {
            this.bitmap = bitmap;
        }

        @Override
        public void draw(Canvas canvas) {
            Rect bounds = getBounds();
            if (bounds.isEmpty() || bitmap.isRecycled()) return;

            float scale = Math.max(
                    bounds.width() / (float) bitmap.getWidth(),
                    bounds.height() / (float) bitmap.getHeight());
            float scaledWidth = bitmap.getWidth() * scale;
            float scaledHeight = bitmap.getHeight() * scale;
            float left = bounds.left + (bounds.width() - scaledWidth) / 2f;
            float top = bounds.top + (bounds.height() - scaledHeight) / 2f;
            RectF destination = new RectF(left, top, left + scaledWidth, top + scaledHeight);

            int saveCount = canvas.save();
            canvas.clipRect(bounds);
            canvas.drawBitmap(bitmap, null, destination, paint);
            canvas.restoreToCount(saveCount);
        }

        @Override
        public void setAlpha(int alpha) {
            paint.setAlpha(Math.max(0, Math.min(255, alpha)));
            invalidateSelf();
        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {
            paint.setColorFilter(colorFilter);
            invalidateSelf();
        }

        @Override
        public int getOpacity() {
            return bitmap.hasAlpha() ? PixelFormat.TRANSLUCENT : PixelFormat.OPAQUE;
        }

        @Override
        public int getIntrinsicWidth() {
            return bitmap.getWidth();
        }

        @Override
        public int getIntrinsicHeight() {
            return bitmap.getHeight();
        }
    }

    private static InputStream openStream(Activity activity, Uri uri) throws IOException {
        if ("file".equalsIgnoreCase(uri.getScheme())) {
            return new FileInputStream(new File(uri.getPath()));
        }
        return activity.getContentResolver().openInputStream(uri);
    }
}
