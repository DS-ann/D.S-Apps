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
    static final int SHAPE_SQUARE = 0;
    static final int SHAPE_ROUND = 1;
    static final int SHAPE_HEXAGON = 2;
    static final int SHAPE_PENTAGON = 3;
    static final int SHAPE_TILTED_PEAR = 4;

    // Compatibility aliases for existing internal call sites and legacy preference
    // terminology. Persisted old values are migrated in MainActivity.
    static final int SHAPE_ORIGINAL = SHAPE_SQUARE;
    static final int SHAPE_TRIANGLE = SHAPE_SQUARE;
    static final int SHAPE_PEAR = SHAPE_TILTED_PEAR;
    static final int SHAPE_ROUNDED_HEXAGON = SHAPE_HEXAGON;

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

    // Keep the cache large enough for a typical drawer page plus nearby rows, without
    // retaining multiple full-size copies of every installed icon on low-RAM devices.
    private static final int MAX_ARTWORK_CACHE_BYTES = 8 * 1024 * 1024;
    private static final boolean[] NO_BACKDROP_MASK = new boolean[0];
    private static final LruCache<ArtworkCacheKey, Bitmap> ARTWORK_CACHE =
            new LruCache<ArtworkCacheKey, Bitmap>(MAX_ARTWORK_CACHE_BYTES) {
                @Override protected int sizeOf(ArtworkCacheKey key, Bitmap value) {
                    return value == null ? 0 : value.getByteCount();
                }
            };

    private Drawable source;
    private Drawable originalSource;
    private Drawable.ConstantState originalSourceState;
    private String sourceCacheToken;
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
        this(source, showBackground, shape, backgroundColor, innerShapeColor, artworkTheme, null);
    }

    LauncherIconDrawable(Drawable source, boolean showBackground, int shape, int backgroundColor,
            int innerShapeColor, int artworkTheme, String stableCacheToken) {
        this.originalSource = source;
        this.source = source;
        this.originalSourceState = source == null ? null : source.getConstantState();
        this.sourceCacheToken = stableCacheToken;
        this.showBackground = showBackground;
        this.shape = clampShape(shape);
        this.backgroundColor = backgroundColor;
        this.innerShapeColor = innerShapeColor;
        if (source != null) setBounds(source.getBounds());
        updateAppearance(showBackground, shape, backgroundColor, innerShapeColor, artworkTheme);
    }

    static int clampShape(int shape) {
        return Math.max(SHAPE_SQUARE, Math.min(SHAPE_TILTED_PEAR, shape));
    }

    static int clampArtworkTheme(int theme) {
        return Math.max(ARTWORK_ORIGINAL, Math.min(ARTWORK_SUNSET_GRADIENT, theme));
    }

    private static final class ArtworkCacheKey {
        final Drawable.ConstantState state;
        final String token;
        final int theme;

        ArtworkCacheKey(Drawable.ConstantState state, String token, int theme) {
            this.state = state;
            this.token = state == null ? token : null;
            this.theme = theme;
        }

        @Override public int hashCode() {
            int identityHash = state == null ? (token == null ? 0 : token.hashCode())
                    : System.identityHashCode(state);
            return identityHash * 31 + theme;
        }

        @Override public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof ArtworkCacheKey)) return false;
            ArtworkCacheKey key = (ArtworkCacheKey) other;
            if (theme != key.theme) return false;
            if (state != null || key.state != null) return state != null && state == key.state;
            return token != null && token.equals(key.token);
        }
    }

    private static final int[] MIXED_PALETTE = {
            Color.rgb(39, 117, 255), Color.rgb(139, 78, 220), Color.rgb(255, 74, 153)
    };
    private static final int[] OCEAN_PALETTE = {
            Color.rgb(21, 94, 239), Color.rgb(0, 184, 217), Color.rgb(104, 224, 181)
    };
    private static final int[] SUNSET_PALETTE = {
            Color.rgb(255, 155, 39), Color.rgb(255, 79, 118), Color.rgb(138, 69, 234)
    };

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
        boolean unchanged = this.showBackground == showBackground
                && this.shape == clampedShape
                && this.backgroundColor == backgroundColor
                && this.innerShapeColor == innerShapeColor
                && this.artworkTheme == clampedArtworkTheme;
        if (unchanged) return;
        if (this.showBackground != showBackground || this.shape != clampedShape) {
            shapePathDirty = true;
        }
        if (this.artworkTheme != clampedArtworkTheme) {
            this.artworkTheme = clampedArtworkTheme;
            this.source = artworkForTheme(originalSource, clampedArtworkTheme, sourceCacheToken);
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
        updateContent(source, showBackground, shape, backgroundColor, innerShapeColor,
                requestedArtworkTheme, sourceCacheToken);
    }

    void updateContent(Drawable source, boolean showBackground, int shape,
            int backgroundColor, int innerShapeColor, int requestedArtworkTheme,
            String stableCacheToken) {
        // Android's GridView rebinds icon cells during normal scrolling. The previous
        // implementation reset every drawable to Original and rebuilt its themed wrapper
        // on every bind, causing avoidable allocation and repeated work on slow devices.
        Drawable.ConstantState nextState = source == null ? null : source.getConstantState();
        boolean sameSource = nextState != null
                ? nextState == originalSourceState
                : nextState == null && sourceCacheToken != null
                    && sourceCacheToken.equals(stableCacheToken);
        boolean sameAppearance = this.showBackground == showBackground
                && this.shape == clampShape(shape)
                && this.backgroundColor == backgroundColor
                && this.innerShapeColor == innerShapeColor
                && this.artworkTheme == clampArtworkTheme(requestedArtworkTheme);
        this.originalSource = source;
        this.originalSourceState = nextState;
        this.sourceCacheToken = stableCacheToken;
        if (!sameSource) this.artworkTheme = -1;
        if (this.source != null) this.source.setBounds(getBounds());
        if (sameSource && sameAppearance) return;
        updateAppearance(showBackground, shape, backgroundColor, innerShapeColor, requestedArtworkTheme);
    }

    private static Drawable artworkForTheme(Drawable original, int theme, String stableCacheToken) {
        int clampedTheme = clampArtworkTheme(theme);
        if (original == null || clampedTheme == ARTWORK_ORIGINAL) return original;

        Drawable.ConstantState state = original.getConstantState();
        ArtworkCacheKey key = state == null && (stableCacheToken == null || stableCacheToken.isEmpty())
                ? null : new ArtworkCacheKey(state, stableCacheToken, clampedTheme);
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
        boolean hasBackdrop = backdrop.length == count;

        float[] sourceHsv = new float[3];
        float[] targetHsv = new float[3];
        for (int index = 0; index < count; index++) {
            int pixel = pixels[index];
            int alpha = Color.alpha(pixel);
            if (alpha == 0 || (hasBackdrop && backdrop[index])) continue;

            // Recolour according to each pixel's original hue, not its screen position.
            // That preserves separate coloured regions in logos (for example Chrome's
            // ring and centre) so different apps no longer inherit the same colour bands.
            Color.colorToHSV(pixel, sourceHsv);
            float hue = sourceHsv[0];
            float saturation = sourceHsv[1];
            float value = sourceHsv[2];

            // Keep near-black contours and near-white highlights neutral; they carry
            // useful shape/contrast information at small launcher icon sizes.
            if (saturation < 0.16f && (value < 0.12f || value > 0.96f)) continue;

            int themed = artworkColour(theme, hue, saturation, value);
            Color.colorToHSV(themed, targetHsv);
            targetHsv[2] = value; // retain source shading and contour contrast
            if (saturation < 0.16f) {
                targetHsv[1] = Math.min(targetHsv[1], 0.58f);
            } else {
                targetHsv[1] = Math.max(0.52f, Math.min(0.92f, targetHsv[1]));
            }
            int rgb = Color.HSVToColor(targetHsv);
            pixels[index] = Color.argb(alpha, Color.red(rgb), Color.green(rgb), Color.blue(rgb));
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
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (x != 0 && x != width - 1 && y != 0 && y != height - 1) continue;
                int alpha = Color.alpha(pixels[y * width + x]);
                if (alpha < 24) transparentBorder++;
                else if (alpha >= 232) opaqueBorder++;
            }
        }
        // Transparent canvas edges already separate artwork from the outer background.
        // Return a shared empty sentinel: do not allocate a full per-pixel mask for the
        // overwhelmingly common transparent-icon case.
        if (transparentBorder >= borderCount * 0.20f || opaqueBorder < borderCount * 0.70f) {
            return NO_BACKDROP_MASK;
        }

        // Only opaque, edge-to-edge legacy icons need colour clustering. Deferring this
        // 32K histogram avoids about 128 KiB of short-lived allocation per normal icon.
        int[] bucketCounts = new int[32768];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (x != 0 && x != width - 1 && y != 0 && y != height - 1) continue;
                int pixel = pixels[y * width + x];
                if (Color.alpha(pixel) < 232) continue;
                int bucket = ((Color.red(pixel) >> 3) << 10)
                        | ((Color.green(pixel) >> 3) << 5)
                        | (Color.blue(pixel) >> 3);
                bucketCounts[bucket]++;
            }
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

    private static int artworkColour(int theme, float originalHue, float saturation, float value) {
        if (theme == ARTWORK_SPECTRUM_MIX) {
            // Map original hue families onto the same three theme colours in every app.
            // Six source hue sectors cycle through the three targets, preserving the
            // separations between coloured parts rather than painting by x/y position.
            int sector = ((int) ((originalHue + 30f) / 60f)) % 6;
            int paletteIndex;
            switch (sector) {
                case 0: case 3: paletteIndex = 0; break;
                case 1: case 4: paletteIndex = 1; break;
                default: paletteIndex = 2; break;
            }
            return MIXED_PALETTE[paletteIndex];
        }
        // The gradient follows the logo's original hue regions rather than drawing
        // the same left-to-right gradient across every icon. Chromatic divisions stay
        // recognisable while pixels within each region retain their original shading.
        float position = Math.max(0f, Math.min(1f, originalHue / 360f));
        if (theme == ARTWORK_OCEAN_GRADIENT) {
            return interpolatePalette(OCEAN_PALETTE, position);
        }
        return interpolatePalette(SUNSET_PALETTE, position);
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

        // Every choice uses the same full drawable bounds; the shape only clips pixels
        // outside its silhouette and never resizes or insets the icon artwork.
        rect.set(left, top, right, bottom);
        if (rect.width() <= 0 || rect.height() <= 0) return;
        if (shapePathDirty) {
            setShapePath(rect);
            shapePathDirty = false;
        }

        if (showBackground) {
            paint.setColor(backgroundColor);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawPath(path, paint);
        } else if (shape != SHAPE_SQUARE) {
            // Preserve the existing inner-colour behaviour while the main backing is off.
            // When the backing is on, its selected silhouette uses backgroundColor only.
            paint.setColor(innerShapeColor);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawPath(path, paint);
        }

        int save = canvas.save();
        canvas.clipPath(path);
        source.setBounds(Math.round(rect.left), Math.round(rect.top),
                Math.round(rect.right), Math.round(rect.bottom));
        source.draw(canvas);
        canvas.restoreToCount(save);
    }

    private void setShapePath(RectF target) {
        path.reset();
        switch (shape) {
            case SHAPE_ROUND: {
                float radius = Math.min(target.width(), target.height()) * 0.5f;
                path.addCircle(target.centerX(), target.centerY(), radius, Path.Direction.CW);
                break;
            }
            case SHAPE_HEXAGON:
                setRegularPolygonPath(target, 6, 0f);
                break;
            case SHAPE_PENTAGON:
                setRegularPolygonPath(target, 5, -90f);
                break;
            case SHAPE_TILTED_PEAR:
                setTiltedPearPath(target);
                break;
            case SHAPE_SQUARE:
            default: {
                // Preserve the current rounded-square visual, but use the same path to
                // clip the logo so no artwork leaks outside the square icon bounds.
                float corner = Math.min(target.width(), target.height()) * 0.23f;
                path.addRoundRect(target, corner, corner, Path.Direction.CW);
                break;
            }
        }
    }

    /**
     * Generate a regular polygon and normalize its vertex bounds to the existing icon
     * bounds. Hexagons use horizontal top/bottom edges; pentagons use an apex at the top.
     */
    private void setRegularPolygonPath(RectF target, int sides, float startAngleDegrees) {
        float minX = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        float[] xs = new float[sides];
        float[] ys = new float[sides];
        for (int i = 0; i < sides; i++) {
            double angle = Math.toRadians(startAngleDegrees + (360.0 * i / sides));
            xs[i] = (float) Math.cos(angle);
            ys[i] = (float) Math.sin(angle);
            minX = Math.min(minX, xs[i]);
            maxX = Math.max(maxX, xs[i]);
            minY = Math.min(minY, ys[i]);
            maxY = Math.max(maxY, ys[i]);
        }
        float scaleX = target.width() / Math.max(0.0001f, maxX - minX);
        float scaleY = target.height() / Math.max(0.0001f, maxY - minY);
        for (int i = 0; i < sides; i++) {
            float x = target.left + (xs[i] - minX) * scaleX;
            float y = target.top + (ys[i] - minY) * scaleY;
            if (i == 0) path.moveTo(x, y);
            else path.lineTo(x, y);
        }
        path.close();
    }

    /** A slanted pear silhouette, kept inside the same square-sized bounding rectangle. */
    private void setTiltedPearPath(RectF target) {
        float w = target.width();
        float h = target.height();
        float left = target.left;
        float top = target.top;
        float right = target.right;
        float bottom = target.bottom;
        // A narrower crown leans left; the wider base leans right. All control points
        // remain within the square bounding box, so adjacent layout spacing is unchanged.
        path.moveTo(left + w * 0.39f, top);
        path.cubicTo(left + w * 0.22f, top,
                left + w * 0.16f, top + h * 0.22f,
                left + w * 0.13f, top + h * 0.42f);
        path.cubicTo(left + w * 0.08f, top + h * 0.67f,
                left + w * 0.23f, bottom,
                left + w * 0.52f, bottom);
        path.cubicTo(left + w * 0.80f, bottom,
                right - w * 0.03f, top + h * 0.68f,
                right - w * 0.11f, top + h * 0.42f);
        path.cubicTo(right - w * 0.18f, top + h * 0.22f,
                left + w * 0.61f, top,
                left + w * 0.39f, top);
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
