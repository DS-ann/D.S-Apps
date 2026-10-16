package com.dsann.launcher;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.PixelFormat;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;

/** Draws an app icon with optional rounded-square backing and a selectable silhouette. */
final class LauncherIconDrawable extends Drawable {
    static final int SHAPE_ORIGINAL = 0;
    static final int SHAPE_ROUND = 1;
    static final int SHAPE_PEAR = 2;

    private final Drawable source;
    private final boolean showBackground;
    private final int shape;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Path path = new Path();

    LauncherIconDrawable(Drawable source, boolean showBackground, int shape) {
        this.source = source;
        this.showBackground = showBackground;
        this.shape = shape;
        setBounds(source.getBounds());
    }

    @Override public void draw(Canvas canvas) {
        RectF bounds = new RectF(getBounds());
        if (bounds.width() <= 0 || bounds.height() <= 0) return;

        if (showBackground) {
            paint.setColor(Color.rgb(48, 55, 76));
            paint.setStyle(Paint.Style.FILL);
            float corner = Math.min(bounds.width(), bounds.height()) * 0.23f;
            canvas.drawRoundRect(bounds, corner, corner, paint);
        }

        float inset = showBackground ? Math.min(bounds.width(), bounds.height()) * 0.10f : 0f;
        rect.set(bounds.left + inset, bounds.top + inset, bounds.right - inset, bounds.bottom - inset);
        if (rect.width() <= 0 || rect.height() <= 0) return;

        int save = canvas.save();
        if (shape == SHAPE_ROUND) {
            path.reset();
            path.addCircle(rect.centerX(), rect.centerY(),
                    Math.min(rect.width(), rect.height()) / 2f, Path.Direction.CW);
            canvas.clipPath(path);
        } else if (shape == SHAPE_PEAR) {
            // Pear silhouette: narrow rounded top, widening toward a full lower half.
            path.reset();
            path.moveTo(rect.centerX(), rect.top);
            path.cubicTo(rect.left + rect.width() * 0.20f, rect.top,
                    rect.left + rect.width() * 0.30f, rect.top + rect.height() * 0.24f,
                    rect.left + rect.width() * 0.18f, rect.top + rect.height() * 0.43f);
            path.cubicTo(rect.left - rect.width() * 0.02f, rect.top + rect.height() * 0.76f,
                    rect.left + rect.width() * 0.19f, rect.bottom,
                    rect.centerX(), rect.bottom);
            path.cubicTo(rect.right - rect.width() * 0.19f, rect.bottom,
                    rect.right + rect.width() * 0.02f, rect.top + rect.height() * 0.76f,
                    rect.right - rect.width() * 0.18f, rect.top + rect.height() * 0.43f);
            path.cubicTo(rect.right - rect.width() * 0.30f, rect.top + rect.height() * 0.24f,
                    rect.right - rect.width() * 0.20f, rect.top,
                    rect.centerX(), rect.top);
            path.close();
            canvas.clipPath(path);
        }

        source.setBounds(Math.round(rect.left), Math.round(rect.top),
                Math.round(rect.right), Math.round(rect.bottom));
        source.draw(canvas);
        canvas.restoreToCount(save);
    }

    @Override public void setAlpha(int alpha) {
        source.setAlpha(alpha);
        invalidateSelf();
    }

    @Override public void setColorFilter(ColorFilter colorFilter) {
        source.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override protected void onBoundsChange(android.graphics.Rect bounds) {
        source.setBounds(bounds);
    }
}
