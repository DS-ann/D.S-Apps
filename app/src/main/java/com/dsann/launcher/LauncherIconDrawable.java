package com.dsann.launcher;

import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.PixelFormat;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;

/** Draws app icons with optional backing and selectable silhouettes. */
final class LauncherIconDrawable extends Drawable {
    static final int SHAPE_ORIGINAL = 0;
    static final int SHAPE_TRIANGLE = 1;
    static final int SHAPE_PEAR = 2;
    static final int SHAPE_ROUNDED_HEXAGON = 3;

    static final int BACKGROUND_BLACK = 0;
    static final int BACKGROUND_WHITE = 1;
    static final int BACKGROUND_GREY = 2;

    private static final int SPECTRUM_STEPS = 1000;
    private static final int[] SPECTRUM_COLORS = new int[] {
            Color.WHITE, Color.BLACK,
            Color.rgb(148, 0, 211),  // Violet
            Color.rgb(75, 0, 130),   // Indigo
            Color.BLUE,               // Blue
            Color.GREEN,              // Green
            Color.YELLOW,             // Yellow
            Color.rgb(255, 127, 0),   // Orange
            Color.RED                 // Red
    };

    private Drawable source;
    private boolean showBackground;
    private int shape;
    private int backgroundColor;
    private int innerShapeColor;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Path path = new Path();
    private final android.graphics.Rect bounds = new android.graphics.Rect();
    // Shape geometry is constant between bounds/style changes; do not rebuild it on every draw.
    private boolean shapePathDirty = true;
    // Reuse these six points rather than allocating seven arrays every icon redraw.
    private final float[][] hexagonVertices = new float[6][2];

    LauncherIconDrawable(Drawable source, boolean showBackground, int shape, int backgroundColor,
            int innerShapeColor) {
        this.source = source;
        this.showBackground = showBackground;
        this.shape = clampShape(shape);
        this.backgroundColor = backgroundColor;
        this.innerShapeColor = innerShapeColor;
        if (source != null) setBounds(source.getBounds());
    }

    static int clampShape(int shape) {
        return Math.max(SHAPE_ORIGINAL, Math.min(SHAPE_ROUNDED_HEXAGON, shape));
    }

    static int[] spectrumTrackColors() {
        return SPECTRUM_COLORS.clone();
    }

    static int spectrumColorForPosition(int position) {
        int bounded = Math.max(0, Math.min(SPECTRUM_STEPS, position));
        float scaled = bounded * (SPECTRUM_COLORS.length - 1f) / SPECTRUM_STEPS;
        int first = Math.min(SPECTRUM_COLORS.length - 2, (int) scaled);
        float fraction = scaled - first;
        int from = SPECTRUM_COLORS[first];
        int to = SPECTRUM_COLORS[first + 1];
        int red = Math.round(Color.red(from) + (Color.red(to) - Color.red(from)) * fraction);
        int green = Math.round(Color.green(from) + (Color.green(to) - Color.green(from)) * fraction);
        int blue = Math.round(Color.blue(from) + (Color.blue(to) - Color.blue(from)) * fraction);
        return Color.rgb(red, green, blue);
    }

    static int backgroundPositionFromPreferences(SharedPreferences preferences) {
        if (preferences == null) return 125;
        int saved = preferences.getInt("icon_background_color_position", -1);
        if (saved >= 0) return Math.max(0, Math.min(SPECTRUM_STEPS, saved));
        int legacyChoice = preferences.getInt("icon_background_color", BACKGROUND_BLACK);
        switch (legacyChoice) {
            case BACKGROUND_WHITE: return 0;
            case BACKGROUND_GREY: return 63;
            case BACKGROUND_BLACK:
            default: return 125;
        }
    }

    static int innerShapePositionFromPreferences(SharedPreferences preferences) {
        if (preferences == null) return 108;
        int saved = preferences.getInt("icon_inner_shape_color_position", -1);
        if (saved >= 0) return Math.max(0, Math.min(SPECTRUM_STEPS, saved));
        // Preserve the previous contrasting inner fill as closely as possible on upgrade.
        int legacyChoice = preferences.getInt("icon_background_color", BACKGROUND_BLACK);
        if (legacyChoice == BACKGROUND_WHITE) return 125;
        if (legacyChoice == BACKGROUND_GREY) return 90;
        return 108;
    }

    static int backgroundColorFromPreferences(SharedPreferences preferences) {
        return spectrumColorForPosition(backgroundPositionFromPreferences(preferences));
    }

    static int innerShapeColorFromPreferences(SharedPreferences preferences) {
        return spectrumColorForPosition(innerShapePositionFromPreferences(preferences));
    }

    static int backgroundColorForChoice(int choice) {
        switch (choice) {
            case BACKGROUND_WHITE: return Color.WHITE;
            case BACKGROUND_GREY: return Color.GRAY;
            case BACKGROUND_BLACK:
            default: return Color.BLACK;
        }
    }

    void updateAppearance(boolean showBackground, int shape, int backgroundColor, int innerShapeColor) {
        int clampedShape = clampShape(shape);
        if (this.showBackground != showBackground || this.shape != clampedShape) {
            shapePathDirty = true;
        }
        this.showBackground = showBackground;
        this.shape = clampedShape;
        this.backgroundColor = backgroundColor;
        this.innerShapeColor = innerShapeColor;
        invalidateSelf();
    }

    /** Reuse the per-cell drawing objects when GridView binds a recycled cell. */
    void updateContent(Drawable source, boolean showBackground, int shape,
            int backgroundColor, int innerShapeColor) {
        this.source = source;
        if (this.source != null) this.source.setBounds(getBounds());
        updateAppearance(showBackground, shape, backgroundColor, innerShapeColor);
    }

    @Override public void draw(Canvas canvas) {
        if (source == null) return;
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

        if (shape == SHAPE_TRIANGLE || shape == SHAPE_PEAR
                || shape == SHAPE_ROUNDED_HEXAGON) {
            if (shapePathDirty) {
                setShapePath(rect);
                shapePathDirty = false;
            }
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(innerShapeColor);
            canvas.drawPath(path, paint);

            // The app artwork follows the selected silhouette. Reuse the same cached
            // path for fill and clipping rather than calculating it twice per draw.
            int save = canvas.save();
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

    private void setShapePath(RectF target) {
        path.reset();
        if (shape == SHAPE_TRIANGLE) {
            path.moveTo(target.centerX(), target.top);
            if (showBackground) {
                // Round only the lower triangle corners; retain its full-width base.
                float corner = Math.min(target.width(), target.height()) * 0.23f;
                path.lineTo(target.right, target.bottom - corner);
                path.quadTo(target.right, target.bottom,
                        target.right - corner, target.bottom);
                path.lineTo(target.left + corner, target.bottom);
                path.quadTo(target.left, target.bottom,
                        target.left, target.bottom - corner);
            } else {
                path.lineTo(target.right, target.bottom);
                path.lineTo(target.left, target.bottom);
            }
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
        } else if (shape == SHAPE_ROUNDED_HEXAGON) {
            setRoundedHexagonPath(target);
        }
    }

    private void setRoundedHexagonPath(RectF target) {
        float inset = target.width() * 0.25f;
        float[][] vertices = hexagonVertices;
        vertices[0][0] = target.left + inset;
        vertices[0][1] = target.top;
        vertices[1][0] = target.right - inset;
        vertices[1][1] = target.top;
        vertices[2][0] = target.right;
        vertices[2][1] = target.centerY();
        vertices[3][0] = target.right - inset;
        vertices[3][1] = target.bottom;
        vertices[4][0] = target.left + inset;
        vertices[4][1] = target.bottom;
        vertices[5][0] = target.left;
        vertices[5][1] = target.centerY();
        float cornerDistance = Math.min(target.width(), target.height()) * 0.085f;
        for (int i = 0; i < vertices.length; i++) {
            float[] current = vertices[i];
            float[] previous = vertices[(i + vertices.length - 1) % vertices.length];
            float[] next = vertices[(i + 1) % vertices.length];
            float previousLength = (float) Math.hypot(previous[0] - current[0],
                    previous[1] - current[1]);
            float nextLength = (float) Math.hypot(next[0] - current[0], next[1] - current[1]);
            float beforeFraction = previousLength <= 0f ? 0f
                    : Math.min(0.18f, cornerDistance / previousLength);
            float afterFraction = nextLength <= 0f ? 0f
                    : Math.min(0.18f, cornerDistance / nextLength);
            float beforeX = current[0] + (previous[0] - current[0]) * beforeFraction;
            float beforeY = current[1] + (previous[1] - current[1]) * beforeFraction;
            float afterX = current[0] + (next[0] - current[0]) * afterFraction;
            float afterY = current[1] + (next[1] - current[1]) * afterFraction;
            if (i == 0) path.moveTo(beforeX, beforeY);
            else path.lineTo(beforeX, beforeY);
            path.quadTo(current[0], current[1], afterX, afterY);
        }
        path.close();
    }

    @Override public void setAlpha(int alpha) {
        if (source != null) source.setAlpha(alpha);
        invalidateSelf();
    }

    @Override public void setColorFilter(ColorFilter colorFilter) {
        if (source != null) source.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override protected void onBoundsChange(android.graphics.Rect bounds) {
        shapePathDirty = true;
        if (source != null) source.setBounds(bounds);
    }
}
