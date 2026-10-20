package com.dsann.launcher;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

/** Drag-to-position image preview used by the wallpaper crop dialog. */
public final class WallpaperCropView extends View {
    private final Bitmap bitmap;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private float scale, x, y, lastX, lastY;

    public WallpaperCropView(Context context, Bitmap bitmap) {
        super(context);
        this.bitmap = bitmap;
    }

    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        scale = Math.max(w / (float) bitmap.getWidth(), h / (float) bitmap.getHeight());
        x = (w - bitmap.getWidth() * scale) / 2f;
        y = (h - bitmap.getHeight() * scale) / 2f;
    }

    @Override protected void onDraw(Canvas canvas) {
        canvas.drawBitmap(bitmap, null,
                new RectF(x, y, x + bitmap.getWidth() * scale, y + bitmap.getHeight() * scale), paint);
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            lastX = event.getX();
            lastY = event.getY();
            return true;
        }
        if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
            x += event.getX() - lastX;
            y += event.getY() - lastY;
            lastX = event.getX();
            lastY = event.getY();
            x = Math.min(0, Math.max(getWidth() - bitmap.getWidth() * scale, x));
            y = Math.min(0, Math.max(getHeight() - bitmap.getHeight() * scale, y));
            invalidate();
            return true;
        }
        return true;
    }

    public Bitmap crop() {
        int left = Math.max(0, (int) Math.floor(-x / scale));
        int top = Math.max(0, (int) Math.floor(-y / scale));
        int width = Math.min(bitmap.getWidth() - left, Math.round(getWidth() / scale));
        int height = Math.min(bitmap.getHeight() - top, Math.round(getHeight() / scale));
        return Bitmap.createBitmap(bitmap, left, top, width, height);
    }
}
