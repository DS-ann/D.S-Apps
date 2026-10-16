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
    static final int SHAPE_TRIANGLE = 1;
    static final int SHAPE_PEAR = 2;
    static final int BACKGROUND_BLACK = 0;
    static final int BACKGROUND_WHITE = 1;
    static final int BACKGROUND_GREY = 2;

    static int backgroundColorForChoice(int choice) {
        switch (choice) {
            case BACKGROUND_WHITE: return Color.WHITE;
            case BACKGROUND_GREY: return Color.GRAY;
            case BACKGROUND_BLACK:
            default: return Color.BLACK;
        }
    }

    private final Drawable source;
    private final boolean showBackground;
    private final int shape;
    private final int backgroundColor;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Path path = new Path();
    private final android.graphics.Rect bounds = new android.graphics.Rect();

    LauncherIconDrawable(Drawable source, boolean showBackground, int shape, int backgroundColor) {
        this.source = source;
        this.showBackground = showBackground;
        this.shape = shape;
        this.backgroundColor = backgroundColor;
        setBounds(source.getBounds());
    }

    @Override public void draw(Canvas canvas) {
        bounds.set(getBounds());
        float left = bounds.left;
        float top = bounds.top;
        float right = bounds.right;
        float bottom = bounds.bottom;
        float width = right - left;
        float height = bottom - top;
        if (width <= 0 || height <= 0) return;

        if (showBackground) {
            paint.setColor(backgroundColor);
            paint.setStyle(Paint.Style.FILL);
            float corner = Math.min(width, height) * 0.23f;
            canvas.drawRoundRect(left, top, right, bottom, corner, corner, paint);
        }

        // Use the full drawable bounds: no inset for any icon shape.
        rect.set(left, top, right, bottom);
        if (rect.width() <= 0 || rect.height() <= 0) return;

        if (shape == SHAPE_TRIANGLE || shape == SHAPE_PEAR) {
            // Fill the selected silhouette with a contrasting color so it stands
            // apart from the rounded-square tile even when the app icon is opaque.
            setShapePath(rect);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(contrastingShapeColor());
            canvas.drawPath(path, paint);

            // Draw the app artwork across the full silhouette. The contrasting
            // fill remains visible only wherever the source artwork is transparent;
            // do not inset the artwork or create a rim.
            int save = canvas.save();
            setShapePath(rect);
            canvas.clipPath(path);
            source.setBounds(Math.round(rect.left), Math.round(rect.top),
                    Math.round(rect.right), Math.round(rect.bottom));
            source.draw(canvas);
            canvas.restoreToCount(save);
        } else {
            source.setBounds(Math.round(rect.left), Math.round(rect.top),
                    Math.round(rect.right), Math.round(rect.bottom));
            source.draw(canvas);
        }
    }

    private int contrastingShapeColor() {
        if (backgroundColor == Color.WHITE) return Color.BLACK;
        if (backgroundColor == Color.GRAY) return Color.rgb(72, 72, 72);
        return Color.rgb(48, 48, 48);
    }

    private void setShapePath(RectF target) {
        path.reset();
        if (shape == SHAPE_TRIANGLE) {
            // Equilateral-style triangle, centered in the target bounds.
            path.moveTo(target.centerX(), target.top);
            path.lineTo(target.right, target.bottom);
            path.lineTo(target.left, target.bottom);
            path.close();
        } else if (shape == SHAPE_PEAR) {
            // Pear silhouette: narrow rounded top, widening toward a full lower half.
            path.moveTo(target.centerX(), target.top);
            path.cubicTo(target.left + target.width() * 0.20f, target.top,
                    target.left + target.width() * 0.30f, target.top + target.height() * 0.24f,
                    target.left + target.width() * 0.18f, target.top + target.height() * 0.43f);
            path.cubicTo(target.left - target.width() * 0.02f, target.top + target.height() * 0.76f,
                    target.left + target.width() * 0.19f, target.bottom,
                    target.centerX(), target.bottom);
            path.cubicTo(target.right - target.width() * 0.19f, target.bottom,
                    target.right + target.width() * 0.02f, target.top + target.height() * 0.76f,
                    target.right - target.width() * 0.18f, target.top + target.height() * 0.43f);
            path.cubicTo(target.right - target.width() * 0.30f, target.top + target.height() * 0.24f,
                    target.right - target.width() * 0.20f, target.top,
                    target.centerX(), target.top);
            path.close();
        }
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
