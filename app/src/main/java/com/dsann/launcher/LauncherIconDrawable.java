package com.dsann.launcher;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.PixelFormat;
import android.graphics.ColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.LruCache;

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

    static final int ARTWORK_ORIGINAL = 0;
    static final int ARTWORK_SPECTRUM_MIX = 1;
    static final int ARTWORK_OCEAN_GRADIENT = 2;
    static final int ARTWORK_SUNSET_GRADIENT = 3;

    private static final int MAX_ARTWORK_CACHE_BYTES = 4 * 1024 * 1024;
    private static final LruCache<ArtworkCacheKey, Bitmap> ARTWORK_CACHE =
            new LruCache<ArtworkCacheKey, Bitmap>(MAX_ARTWORK_CACHE_BYTES) {
                @Override protected int sizeOf(ArtworkCacheKey key, Bitmap value) {
                    return value == null ? 0 : value.getByteCount();
                }
            };

    private Drawable source;
    private Drawable originalSource;
    private int artworkTheme = ARTWORK_ORIGINAL;
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
        this(source, showBackground, shape, backgroundColor, innerShapeColor, ARTWORK_ORIGINAL);
    }

    LauncherIconDrawable(Drawable source, boolean showBackground, int shape, int backgroundColor,
            int innerShapeColor, int artworkTheme) {
        this.originalSource = source;
        this.source = source;
        this.showBackground = showBackground;
        this.shape = clampShape(shape);
        this.backgroundColor = backgroundColor;
        this.innerShapeColor = innerShapeColor;
        if (source != null) setBounds(source.getBounds());
        updateAppearance(showBackground, shape, backgroundColor, innerShapeColor, artworkTheme);
    }

    static int clampShape(int shape) {
        return Math.max(SHAPE_ORIGINAL, Math.min(SHAPE_ROUNDED_HEXAGON, shape));
    }

    static int clampArtworkTheme(int theme) {
        return Math.max(ARTWORK_ORIGINAL, Math.min(ARTWORK_SUNSET_GRADIENT, theme));
    }

    private static final class ArtworkCacheKey {
        final Drawable.ConstantState state;
        final int theme;

        ArtworkCacheKey(Drawable.ConstantState state, int theme) {
            this.state = state;
            this.theme = theme;
        }

        @Override public int hashCode() {
            return System.identityHashCode(state) * 31 + theme;
        }

        @Override public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof ArtworkCacheKey)) return false;
            ArtworkCacheKey key = (ArtworkCacheKey) other;
            return state == key.state && theme == key.theme;
        }
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
        updateAppearance(showBackground, shape, backgroundColor, innerShapeColor, artworkTheme);
    }

    void updateAppearance(boolean showBackground, int shape, int backgroundColor,
            int innerShapeColor, int requestedArtworkTheme) {
        int clampedShape = clampShape(shape);
        int clampedArtworkTheme = clampArtworkTheme(requestedArtworkTheme);
        if (this.showBackground != showBackground || this.shape != clampedShape) {
            shapePathDirty = true;
        }
        if (this.artworkTheme != clampedArtworkTheme) {
            this.artworkTheme = clampedArtworkTheme;
            this.source = artworkForTheme(originalSource, clampedArtworkTheme);
            if (this.source != null) this.source.setBounds(getBounds());
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
        updateContent(source, showBackground, shape, backgroundColor, innerShapeColor, artworkTheme);
    }

    void updateContent(Drawable source, boolean showBackground, int shape,
            int backgroundColor, int innerShapeColor, int requestedArtworkTheme) {
        // Always transform the PackageManager's original icon, never a previously themed
        // bitmap, so switching themes cannot progressively degrade or recolour old output.
        this.originalSource = source;
        this.source = source;
        this.artworkTheme = ARTWORK_ORIGINAL;
        if (this.source != null) this.source.setBounds(getBounds());
        updateAppearance(showBackground, shape, backgroundColor, innerShapeColor, requestedArtworkTheme);
    }

    private static Drawable artworkForTheme(Drawable original, int theme) {
        int clampedTheme = clampArtworkTheme(theme);
        if (original == null || clampedTheme == ARTWORK_ORIGINAL) return original;

        Drawable.ConstantState state = original.getConstantState();
        ArtworkCacheKey key = state == null ? null : new ArtworkCacheKey(state, clampedTheme);
        Bitmap bitmap = key == null ? null : ARTWORK_CACHE.get(key);
        if (bitmap == null) {
            Bitmap rendered = rasterize(original);
            if (rendered == null) return original;
            bitmap = recolourArtwork(rendered, clampedTheme);
            if (key != null && bitmap != null && bitmap.getByteCount() <= MAX_ARTWORK_CACHE_BYTES) {
                ARTWORK_CACHE.put(key, bitmap);
            }
        }
        if (bitmap == null) return original;
        BitmapDrawable result = new BitmapDrawable(Resources.getSystem(), bitmap);
        result.setFilterBitmap(true);
        result.setDither(true);
        return result;
    }

    private static Bitmap rasterize(Drawable drawable) {
        if (drawable == null) return null;
        int intrinsicWidth = drawable.getIntrinsicWidth();
        int intrinsicHeight = drawable.getIntrinsicHeight();
        if (intrinsicWidth <= 0 || intrinsicHeight <= 0) {
            intrinsicWidth = 144;
            intrinsicHeight = 144;
        }
        float longest = Math.max(intrinsicWidth, intrinsicHeight);
        float targetLongest = Math.max(96f, Math.min(192f, longest));
        float scale = targetLongest / longest;
        int width = Math.max(1, Math.round(intrinsicWidth * scale));
        int height = Math.max(1, Math.round(intrinsicHeight * scale));
        Bitmap bitmap;
        try {
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        } catch (RuntimeException ex) {
            return null;
        }
        bitmap.setDensity(Resources.getSystem().getDisplayMetrics().densityDpi);
        android.graphics.Rect previousBounds = new android.graphics.Rect(drawable.getBounds());
        try {
            drawable.setBounds(0, 0, width, height);
            drawable.draw(new Canvas(bitmap));
        } catch (RuntimeException ex) {
            bitmap.recycle();
            return null;
        } finally {
            drawable.setBounds(previousBounds);
        }
        return bitmap;
    }

    private static Bitmap recolourArtwork(Bitmap rendered, int theme) {
        int width = rendered.getWidth();
        int height = rendered.getHeight();
        int count = width * height;
        int[] pixels = new int[count];
        rendered.getPixels(pixels, 0, width, 0, 0, width, height);

        // Flattened pre-Android-8 icons can contain their own solid backdrop. Identify
        // only a clearly dominant edge-connected colour and leave that region untouched.
        // If an opaque icon has a complex/ambiguous border, preserve the whole icon rather
        // than risk recolouring its built-in background.
        boolean[] backdrop = findOpaqueBackdrop(pixels, width, height);
        if (backdrop == null) return rendered;

        for (int index = 0; index < count; index++) {
            int pixel = pixels[index];
            int alpha = Color.alpha(pixel);
            if (alpha == 0 || backdrop[index]) continue;
            int x = index % width;
            int y = index / width;
            float nx = width <= 1 ? 0.5f : x / (float) (width - 1);
            float ny = height <= 1 ? 0.5f : y / (float) (height - 1);
            int themed = artworkColour(theme, nx, ny);
            pixels[index] = Color.argb(alpha, Color.red(themed), Color.green(themed), Color.blue(themed));
        }

        Bitmap output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        output.setDensity(rendered.getDensity());
        output.setPixels(pixels, 0, width, 0, 0, width, height);
        rendered.recycle();
        return output;
    }

    /**
     * Returns a mask of confidently identified built-in background pixels. A null result
     * means that an opaque raster has no reliable background boundary and must be left
     * unchanged to honour the "artwork only" requirement.
     */
    private static boolean[] findOpaqueBackdrop(int[] pixels, int width, int height) {
        int borderCount = width * 2 + Math.max(0, height - 2) * 2;
        int transparentBorder = 0;
        int opaqueBorder = 0;
        int[] bucketCounts = new int[32768];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (x != 0 && x != width - 1 && y != 0 && y != height - 1) continue;
                int pixel = pixels[y * width + x];
                int alpha = Color.alpha(pixel);
                if (alpha < 24) {
                    transparentBorder++;
                } else if (alpha >= 232) {
                    opaqueBorder++;
                    int bucket = ((Color.red(pixel) >> 3) << 10)
                            | ((Color.green(pixel) >> 3) << 5)
                            | (Color.blue(pixel) >> 3);
                    bucketCounts[bucket]++;
                }
            }
        }
        // Transparent canvas edges already separate artwork from the outer background.
        if (transparentBorder >= borderCount * 0.20f || opaqueBorder < borderCount * 0.70f) {
            return new boolean[pixels.length];
        }

        int dominantBucket = 0;
        int dominantCount = 0;
        for (int i = 0; i < bucketCounts.length; i++) {
            if (bucketCounts[i] > dominantCount) {
                dominantCount = bucketCounts[i];
                dominantBucket = i;
            }
        }
        if (dominantCount < opaqueBorder * 0.16f) return null;
        int red = ((dominantBucket >> 10) & 31) * 8 + 4;
        int green = ((dominantBucket >> 5) & 31) * 8 + 4;
        int blue = (dominantBucket & 31) * 8 + 4;

        boolean[] mask = new boolean[pixels.length];
        int[] queue = new int[pixels.length];
        int tail = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (x != 0 && x != width - 1 && y != 0 && y != height - 1) continue;
                int index = y * width + x;
                if (!mask[index] && isBackdropPixel(pixels[index], red, green, blue)) {
                    mask[index] = true;
                    queue[tail++] = index;
                }
            }
        }
        int head = 0;
        while (head < tail) {
            int index = queue[head++];
            int x = index % width;
            if (x > 0) tail = enqueueBackdrop(index - 1, pixels, mask, queue, tail, red, green, blue);
            if (x + 1 < width) tail = enqueueBackdrop(index + 1, pixels, mask, queue, tail, red, green, blue);
            if (index >= width) tail = enqueueBackdrop(index - width, pixels, mask, queue, tail, red, green, blue);
            if (index + width < pixels.length) {
                tail = enqueueBackdrop(index + width, pixels, mask, queue, tail, red, green, blue);
            }
        }
        return mask;
    }

    private static int enqueueBackdrop(int index, int[] pixels, boolean[] mask, int[] queue,
            int tail, int red, int green, int blue) {
        if (!mask[index] && isBackdropPixel(pixels[index], red, green, blue)) {
            mask[index] = true;
            queue[tail++] = index;
        }
        return tail;
    }

    private static boolean isBackdropPixel(int pixel, int red, int green, int blue) {
        if (Color.alpha(pixel) < 80) return false;
        return Math.abs(Color.red(pixel) - red) <= 54
                && Math.abs(Color.green(pixel) - green) <= 54
                && Math.abs(Color.blue(pixel) - blue) <= 54;
    }

    private static int artworkColour(int theme, float x, float y) {
        float position;
        if (theme == ARTWORK_SPECTRUM_MIX) {
            position = x * 0.80f + y * 0.20f;
            if (position < 0.34f) return Color.rgb(39, 117, 255);   // electric blue
            if (position < 0.67f) return Color.rgb(139, 78, 220);  // violet
            return Color.rgb(255, 74, 153);                         // pink
        }
        if (theme == ARTWORK_OCEAN_GRADIENT) {
            position = x * 0.72f + y * 0.28f;
            return interpolatePalette(new int[] {
                    Color.rgb(21, 94, 239), Color.rgb(0, 184, 217), Color.rgb(104, 224, 181)
            }, position);
        }
        // Sunset gradient: warm amber flowing through coral-pink into violet.
        position = x * 0.70f + (1f - y) * 0.30f;
        return interpolatePalette(new int[] {
                Color.rgb(255, 155, 39), Color.rgb(255, 79, 118), Color.rgb(138, 69, 234)
        }, position);
    }

    private static int interpolatePalette(int[] palette, float position) {
        float bounded = Math.max(0f, Math.min(1f, position));
        float scaled = bounded * (palette.length - 1f);
        int first = Math.min(palette.length - 2, (int) scaled);
        float fraction = scaled - first;
        int from = palette[first];
        int to = palette[first + 1];
        return Color.rgb(
                Math.round(Color.red(from) + (Color.red(to) - Color.red(from)) * fraction),
                Math.round(Color.green(from) + (Color.green(to) - Color.green(from)) * fraction),
                Math.round(Color.blue(from) + (Color.blue(to) - Color.blue(from)) * fraction));
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
