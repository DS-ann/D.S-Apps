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
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import java.lang.ref.WeakReference;

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
    static final int ARTWORK_NOTHING_MONOCHROME = 4;

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
    // Icon conversion must never run on GridView's UI-thread bind/scroll path. Results are
    // shared by cache key, then applied to any still-matching drawable instances on main.
    private static final Handler ARTWORK_HANDLER = new Handler(Looper.getMainLooper());
    private static final Object ARTWORK_REQUEST_LOCK = new Object();
    private static final java.util.Map<ArtworkCacheKey, java.util.ArrayList<WeakReference<LauncherIconDrawable>>>
            ARTWORK_WAITERS = new java.util.HashMap<>();
    private static final java.util.Set<ArtworkCacheKey> ARTWORK_PENDING = new java.util.HashSet<>();
    private static final java.util.concurrent.ExecutorService ARTWORK_EXECUTOR =
            java.util.concurrent.Executors.newFixedThreadPool(2, new java.util.concurrent.ThreadFactory() {
                @Override public Thread newThread(Runnable runnable) {
                    Thread thread = new Thread(() -> {
                        try {
                            // Icon recolouring is non-interactive work. Keep it below the UI
                            // thread's priority so first-open bursts cannot starve animations.
                            android.os.Process.setThreadPriority(
                                    android.os.Process.THREAD_PRIORITY_BACKGROUND);
                        } catch (RuntimeException ignored) {}
                        runnable.run();
                    }, "LauncherIconArtwork");
                    thread.setDaemon(true);
                    return thread;
                }
            });

    private Drawable source;
    private Drawable originalSource;
    private Drawable.ConstantState originalSourceState;
    private String sourceCacheToken;
    // Force the first appearance bind through the cache so Original icons get the outline too.
    private int artworkTheme = -1;
    private boolean showBackground;
    private boolean fitInsideBackground;
    private int shape;
    private int backgroundColor;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Path path = new Path();
    private final android.graphics.Rect bounds = new android.graphics.Rect();
    // Shape geometry is constant between bounds/style changes; do not rebuild it on every draw.
    private boolean shapePathDirty = true;
    // Reuse these six points rather than allocating seven arrays every icon redraw.
    private final float[][] hexagonVertices = new float[6][2];

    LauncherIconDrawable(Drawable source, boolean showBackground, int shape, int backgroundColor) {
        this(source, showBackground, shape, backgroundColor, ARTWORK_ORIGINAL, null, false);
    }

    LauncherIconDrawable(Drawable source, boolean showBackground, int shape, int backgroundColor,
            int artworkTheme) {
        this(source, showBackground, shape, backgroundColor, artworkTheme, null, false);
    }

    LauncherIconDrawable(Drawable source, boolean showBackground, int shape, int backgroundColor,
            int artworkTheme, String stableCacheToken) {
        this(source, showBackground, shape, backgroundColor, artworkTheme, stableCacheToken, false);
    }

    LauncherIconDrawable(Drawable source, boolean showBackground, int shape, int backgroundColor,
            int artworkTheme, String stableCacheToken, boolean fitInsideBackground) {
        this.originalSource = source;
        this.source = source;
        this.originalSourceState = source == null ? null : source.getConstantState();
        this.sourceCacheToken = stableCacheToken;
        this.showBackground = showBackground;
        this.fitInsideBackground = fitInsideBackground;
        this.shape = clampShape(shape);
        this.backgroundColor = backgroundColor;
        if (source != null) setBounds(source.getBounds());
        updateAppearance(showBackground, shape, backgroundColor, artworkTheme, fitInsideBackground);
    }

    static int clampShape(int shape) {
        return Math.max(SHAPE_SQUARE, Math.min(SHAPE_TILTED_PEAR, shape));
    }

    static int clampArtworkTheme(int theme) {
        return Math.max(ARTWORK_ORIGINAL, Math.min(ARTWORK_NOTHING_MONOCHROME, theme));
    }

    private static final class ArtworkCacheKey {
        final Drawable.ConstantState state;
        final String token;
        final int theme;

        ArtworkCacheKey(Drawable.ConstantState state, String token, int theme) {
            this.state = state;
            this.token = token;
            this.theme = theme;
        }

        @Override public int hashCode() {
            int identityHash = state == null ? 0 : System.identityHashCode(state);
            int tokenHash = token == null ? 0 : token.hashCode();
            return (identityHash * 31 + tokenHash) * 31 + theme;
        }

        @Override public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof ArtworkCacheKey)) return false;
            ArtworkCacheKey key = (ArtworkCacheKey) other;
            if (theme != key.theme) return false;
            if (state != key.state) return false;
            if (token == null) return key.token == null;
            return token.equals(key.token);
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
        if (preferences.getInt("icon_artwork_theme", ARTWORK_ORIGINAL)
                == ARTWORK_NOTHING_MONOCHROME) {
            return 0;
        }
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

    static int backgroundColorFromPreferences(SharedPreferences preferences) {
        return spectrumColorForPosition(backgroundPositionFromPreferences(preferences));
    }

    static int backgroundColorForChoice(int choice) {
        switch (choice) {
            case BACKGROUND_WHITE: return Color.WHITE;
            case BACKGROUND_GREY: return Color.GRAY;
            case BACKGROUND_BLACK:
            default: return Color.BLACK;
        }
    }

    void updateAppearance(boolean showBackground, int shape, int backgroundColor) {
        updateAppearance(showBackground, shape, backgroundColor, artworkTheme, fitInsideBackground);
    }

    void updateAppearance(boolean showBackground, int shape, int backgroundColor,
            int requestedArtworkTheme) {
        updateAppearance(showBackground, shape, backgroundColor, requestedArtworkTheme,
                fitInsideBackground);
    }

    void updateAppearance(boolean showBackground, int shape, int backgroundColor,
            int requestedArtworkTheme, boolean fitInsideBackground) {
        int clampedShape = clampShape(shape);
        int clampedArtworkTheme = clampArtworkTheme(requestedArtworkTheme);
        boolean unchanged = this.showBackground == showBackground
                && this.shape == clampedShape
                && this.backgroundColor == backgroundColor
                && this.artworkTheme == clampedArtworkTheme
                && this.fitInsideBackground == fitInsideBackground;
        if (unchanged) return;
        if (this.showBackground != showBackground || this.shape != clampedShape) {
            shapePathDirty = true;
        }
        if (this.artworkTheme != clampedArtworkTheme) {
            this.artworkTheme = clampedArtworkTheme;
            applyArtworkTheme();
        }
        this.showBackground = showBackground;
        this.fitInsideBackground = fitInsideBackground;
        this.shape = clampedShape;
        this.backgroundColor = backgroundColor;
        invalidateSelf();
    }

    /** Reuse the per-cell drawing objects when GridView binds a recycled cell. */
    void updateContent(Drawable source, boolean showBackground, int shape,
            int backgroundColor, int requestedArtworkTheme) {
        updateContent(source, showBackground, shape, backgroundColor, requestedArtworkTheme,
                sourceCacheToken);
    }

    void updateContent(Drawable source, boolean showBackground, int shape,
            int backgroundColor, int requestedArtworkTheme, String stableCacheToken) {
        updateContent(source, showBackground, shape, backgroundColor, requestedArtworkTheme,
                stableCacheToken, fitInsideBackground);
    }

    void updateContent(Drawable source, boolean showBackground, int shape,
            int backgroundColor, int requestedArtworkTheme, String stableCacheToken,
            boolean fitInsideBackground) {
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
                && this.artworkTheme == clampArtworkTheme(requestedArtworkTheme)
                && this.fitInsideBackground == fitInsideBackground;
        this.originalSource = source;
        this.originalSourceState = nextState;
        this.sourceCacheToken = stableCacheToken;
        if (!sameSource) this.artworkTheme = -1;
        if (this.source != null) this.source.setBounds(getBounds());
        if (sameSource && sameAppearance) return;
        updateAppearance(showBackground, shape, backgroundColor, requestedArtworkTheme,
                fitInsideBackground);
    }

    private void applyArtworkTheme() {
        Drawable original = originalSource;
        int theme = clampArtworkTheme(artworkTheme);
        if (original == null) {
            source = null;
            return;
        }

        Drawable.ConstantState state = original.getConstantState();
        // Drawables without ConstantState cannot safely be cloned for a worker. Keep this
        // uncommon fallback correct; normal installed-app icons take the asynchronous path.
        if (state == null) {
            source = artworkForTheme(original, theme, sourceCacheToken);
            if (source != null) source.setBounds(getBounds());
            return;
        }

        ArtworkCacheKey key = new ArtworkCacheKey(state, sourceCacheToken, theme);
        Bitmap cached = ARTWORK_CACHE.get(key);
        if (cached != null) {
            applyArtworkBitmap(cached);
            return;
        }

        // Show the original logo temporarily instead of blocking the transition/scroll
        // while its themed bitmap is prepared. Completion invalidates this drawable.
        source = original;
        source.setBounds(getBounds());
        requestArtworkBitmap(key, state, theme, sourceCacheToken, this);
    }

    private void applyArtworkBitmap(Bitmap bitmap) {
        BitmapDrawable result = new BitmapDrawable(Resources.getSystem(), bitmap);
        result.setFilterBitmap(true);
        result.setDither(true);
        result.setBounds(getBounds());
        source = result;
        invalidateSelf();
    }

    private boolean matchesArtworkRequest(ArtworkCacheKey key, int theme) {
        if (artworkTheme != theme || originalSource == null) return false;
        return key.equals(new ArtworkCacheKey(
                originalSource.getConstantState(), sourceCacheToken, theme));
    }

    private static void requestArtworkBitmap(ArtworkCacheKey key,
            Drawable.ConstantState state, int theme, String stableCacheToken,
            LauncherIconDrawable waiter) {
        boolean startWorker = false;
        synchronized (ARTWORK_REQUEST_LOCK) {
            java.util.ArrayList<WeakReference<LauncherIconDrawable>> waiters = ARTWORK_WAITERS.get(key);
            if (waiters == null) {
                waiters = new java.util.ArrayList<>();
                ARTWORK_WAITERS.put(key, waiters);
            }
            waiters.add(new WeakReference<>(waiter));
            if (ARTWORK_PENDING.add(key)) startWorker = true;
        }
        if (!startWorker) return;

        ARTWORK_EXECUTOR.execute(() -> {
            Bitmap themed = null;
            try {
                Drawable workerDrawable = state.newDrawable();
                if (workerDrawable != null) workerDrawable = workerDrawable.mutate();
                themed = createThemedArtworkBitmap(workerDrawable, theme);
            } catch (RuntimeException ignored) {
                // A vendor Drawable may not support worker-thread rasterisation; retain its
                // original icon rather than crash the drawer.
            }
            final Bitmap result = themed;
            ARTWORK_HANDLER.post(() -> finishArtworkRequest(key, theme, result));
        });
    }

    private static void finishArtworkRequest(ArtworkCacheKey key, int theme, Bitmap bitmap) {
        java.util.ArrayList<WeakReference<LauncherIconDrawable>> waiters;
        synchronized (ARTWORK_REQUEST_LOCK) {
            ARTWORK_PENDING.remove(key);
            waiters = ARTWORK_WAITERS.remove(key);
        }
        if (bitmap != null && bitmap.getByteCount() <= MAX_ARTWORK_CACHE_BYTES) {
            ARTWORK_CACHE.put(key, bitmap);
        }
        if (waiters == null || bitmap == null) return;
        for (WeakReference<LauncherIconDrawable> reference : waiters) {
            LauncherIconDrawable drawable = reference.get();
            if (drawable != null && drawable.matchesArtworkRequest(key, theme)) {
                drawable.applyArtworkBitmap(bitmap);
            }
        }
    }

    /** Synchronous fallback for an unusual Drawable without a ConstantState. */
    private static Drawable artworkForTheme(Drawable original, int theme, String stableCacheToken) {
        int clampedTheme = clampArtworkTheme(theme);
        if (original == null) return null;

        Drawable.ConstantState state = original.getConstantState();
        ArtworkCacheKey key = state == null && (stableCacheToken == null || stableCacheToken.isEmpty())
                ? null : new ArtworkCacheKey(state, stableCacheToken, clampedTheme);
        Bitmap bitmap = key == null ? null : ARTWORK_CACHE.get(key);
        if (bitmap == null) {
            bitmap = createThemedArtworkBitmap(original, clampedTheme);
            if (bitmap == null) return original;
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

    /**
     * Prefer an app's declared monochrome adaptive-icon layer on Android 13+. This is the
     * only reliable way to recover a logo's intended shape without guessing from its colours.
     * Older devices retain the universal bitmap fallback below.
     */
    private static Bitmap createThemedArtworkBitmap(Drawable drawable, int theme) {
        if (drawable == null) return null;
        Bitmap output = null;
        if (theme == ARTWORK_NOTHING_MONOCHROME && android.os.Build.VERSION.SDK_INT >= 33) {
            // Keep API-33-only framework references isolated so this class remains loadable
            // on the launcher's Android 6 minimum API.
            output = Api33MonochromeRenderer.render(drawable);
        }
        if (output == null) {
            Bitmap rendered = rasterize(drawable);
            if (rendered == null) return null;
            output = theme == ARTWORK_ORIGINAL ? rendered : recolourArtwork(rendered, theme);
        }
        // Apply only the outer silhouette contour after all artwork/theme processing. The
        // monochrome conversion itself stays at its stable pre-experiment implementation.
        addInnerBlackBorder(output);
        return output;
    }

    /**
     * Adds an inward-only black contour along the icon silhouette. Transparent space is flood
     * filled from the bitmap edges first, so enclosed transparent holes and internal logo
     * details are not treated as the icon's outer border. The original colour/theme conversion
     * is never used to calculate the contour.
     */
    private static void addInnerBlackBorder(Bitmap bitmap) {
        final int width = bitmap.getWidth();
        final int height = bitmap.getHeight();
        if (width < 3 || height < 3) return;

        final int count = width * height;
        final int[] sourcePixels = new int[count];
        bitmap.getPixels(sourcePixels, 0, width, 0, 0, width, height);

        // Work on the binary alpha silhouette, not on RGB values. That prevents the outline
        // from changing internal lettering, symbols, or monochrome logo details.
        final boolean[] exterior = new boolean[count];
        final int[] queue = new int[count];
        int head = 0;
        int tail = 0;

        // Flood-fill transparent/antialiased pixels connected to the outside. Closed holes
        // within letters and symbols are deliberately not treated as the outer silhouette.
        for (int x = 0; x < width; x++) {
            int top = x;
            int bottom = (height - 1) * width + x;
            if (Color.alpha(sourcePixels[top]) < 96 && !exterior[top]) {
                exterior[top] = true;
                queue[tail++] = top;
            }
            if (Color.alpha(sourcePixels[bottom]) < 96 && !exterior[bottom]) {
                exterior[bottom] = true;
                queue[tail++] = bottom;
            }
        }
        for (int y = 1; y < height - 1; y++) {
            int left = y * width;
            int right = left + width - 1;
            if (Color.alpha(sourcePixels[left]) < 96 && !exterior[left]) {
                exterior[left] = true;
                queue[tail++] = left;
            }
            if (Color.alpha(sourcePixels[right]) < 96 && !exterior[right]) {
                exterior[right] = true;
                queue[tail++] = right;
            }
        }

        while (head < tail) {
            int index = queue[head++];
            int x = index % width;
            int y = index / width;
            for (int dy = -1; dy <= 1; dy++) {
                int ny = y + dy;
                if (ny < 0 || ny >= height) continue;
                for (int dx = -1; dx <= 1; dx++) {
                    if (dx == 0 && dy == 0) continue;
                    int nx = x + dx;
                    if (nx < 0 || nx >= width) continue;
                    int next = ny * width + nx;
                    if (exterior[next] || Color.alpha(sourcePixels[next]) >= 96) continue;
                    exterior[next] = true;
                    queue[tail++] = next;
                }
            }
        }

        // Double the original border thickness, but make it adaptive to local stroke width.
        // A fixed inward band can consume an entire thin glyph (e.g. +, -, digits or letters).
        // Increase the prior doubled contour by a further 50% while retaining the local
        // stroke-width safeguard below for thin letters, digits and mathematical symbols.
        final int thickness = Math.max(1, Math.round(3f * Math.max(1,
                Math.round(Math.min(width, height) * 0.009f))));
        final int[] depth = new int[count];
        java.util.Arrays.fill(depth, -1);
        head = 0;
        tail = 0;

        // Start a four-neighbour distance transform at the exterior edge of the foreground.
        // Seed canvas-edge pixels too, for conventional opaque square/backplate icons.
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = y * width + x;
                if (Color.alpha(sourcePixels[index]) < 96) continue;
                boolean boundary = x == 0 || x == width - 1 || y == 0 || y == height - 1;
                if (!boundary) {
                    boundary = exterior[index - 1] || exterior[index + 1]
                            || exterior[index - width] || exterior[index + width];
                }
                if (boundary) {
                    depth[index] = 1;
                    queue[tail++] = index;
                }
            }
        }

        while (head < tail) {
            int index = queue[head++];
            int x = index % width;
            int y = index / width;
            int nextDepth = depth[index] + 1;
            if (x > 0) tail = enqueueOutlineDepth(index - 1, sourcePixels, depth, queue, tail, nextDepth);
            if (x + 1 < width) tail = enqueueOutlineDepth(index + 1, sourcePixels, depth, queue, tail, nextDepth);
            if (y > 0) tail = enqueueOutlineDepth(index - width, sourcePixels, depth, queue, tail, nextDepth);
            if (y + 1 < height) tail = enqueueOutlineDepth(index + width, sourcePixels, depth, queue, tail, nextDepth);
        }

        final int safeCoreSearch = thickness + 1;
        final int[] outputPixels = sourcePixels.clone();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = y * width + x;
                int alpha = Color.alpha(sourcePixels[index]);
                if (alpha < 96 || exterior[index] || depth[index] < 1
                        || depth[index] > thickness) continue;

                // Only darken an edge pixel when the local stroke has room for an inner fill
                // pixel. If no such core exists nearby, keep the entire tiny stroke visible
                // instead of turning a thin plus/minus/text glyph into a black blob.
                boolean hasVisibleCore = false;
                for (int dy = -safeCoreSearch; dy <= safeCoreSearch && !hasVisibleCore; dy++) {
                    int ny = y + dy;
                    if (ny < 0 || ny >= height) continue;
                    int horizontalReach = safeCoreSearch - Math.abs(dy);
                    for (int dx = -horizontalReach; dx <= horizontalReach; dx++) {
                        int nx = x + dx;
                        if (nx < 0 || nx >= width) continue;
                        if (depth[ny * width + nx] > thickness) {
                            hasVisibleCore = true;
                            break;
                        }
                    }
                }
                if (hasVisibleCore) outputPixels[index] = Color.argb(alpha, 0, 0, 0);
            }
        }
        bitmap.setPixels(outputPixels, 0, width, 0, 0, width, height);
    }

    private static int enqueueOutlineDepth(int index, int[] pixels, int[] depth,
            int[] queue, int tail, int nextDepth) {
        if (depth[index] < 0 && Color.alpha(pixels[index]) >= 96) {
            depth[index] = nextDepth;
            queue[tail++] = index;
        }
        return tail;
    }

    private static final class Api33MonochromeRenderer {
        static Bitmap render(Drawable source) {
            if (!(source instanceof android.graphics.drawable.AdaptiveIconDrawable)) return null;
            android.graphics.drawable.AdaptiveIconDrawable adaptive =
                    (android.graphics.drawable.AdaptiveIconDrawable) source;
            Drawable monochrome;
            try {
                monochrome = adaptive.getMonochrome();
            } catch (RuntimeException ignored) {
                return null;
            }
            if (monochrome == null) return null;

            Bitmap fullIcon = rasterize(adaptive);
            Bitmap glyph = rasterize(monochrome);
            if (fullIcon == null || glyph == null) {
                if (fullIcon != null) fullIcon.recycle();
                if (glyph != null) glyph.recycle();
                return null;
            }
            if (glyph.getWidth() != fullIcon.getWidth()
                    || glyph.getHeight() != fullIcon.getHeight()) {
                Bitmap scaled = Bitmap.createScaledBitmap(
                        glyph, fullIcon.getWidth(), fullIcon.getHeight(), true);
                if (scaled != glyph) glyph.recycle();
                glyph = scaled;
            }

            Bitmap backgroundBitmap = null;
            try {
                Drawable background = adaptive.getBackground();
                if (background != null) backgroundBitmap = rasterize(background);
            } catch (RuntimeException ignored) {}

            int width = fullIcon.getWidth();
            int height = fullIcon.getHeight();
            int count = width * height;
            int[] fullPixels = new int[count];
            int[] glyphPixels = new int[count];
            fullIcon.getPixels(fullPixels, 0, width, 0, 0, width, height);
            glyph.getPixels(glyphPixels, 0, width, 0, 0, width, height);

            if (backgroundBitmap != null) backgroundBitmap.recycle();
            // Nothing-style artwork uses a white icon field and a black glyph. Do not let
            // an app's original background colour invert this, which previously produced
            // inconsistent dark/white circles for otherwise similar icons.
            int backgroundTone = 255;
            int foregroundTone = 0;
            int[] outputPixels = new int[count];
            for (int i = 0; i < count; i++) {
                int iconAlpha = Color.alpha(fullPixels[i]);
                if (iconAlpha < 8) {
                    outputPixels[i] = Color.TRANSPARENT;
                    continue;
                }
                // Blend glyph edge coverage into the two-tone artwork so antialiasing survives.
                float coverage = Color.alpha(glyphPixels[i]) / 255f;
                int tone = Math.round(backgroundTone * (1f - coverage)
                        + foregroundTone * coverage);
                outputPixels[i] = Color.argb(iconAlpha, tone, tone, tone);
            }

            Bitmap output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            output.setDensity(fullIcon.getDensity());
            output.setPixels(outputPixels, 0, width, 0, 0, width, height);
            fullIcon.recycle();
            glyph.recycle();
            return output;
        }
    }

    /**
     * Converts every app icon through the same monochrome pipeline. When the source has
     * a confidently detected backplate, the backplate determines black/white polarity,
     * foreground regions use the opposite tone, and strong internal colour boundaries
     * become thin separators. No package names or per-app artwork are consulted.
     */
    private static Bitmap recolourArtwork(Bitmap rendered, int theme) {
        if (theme == ARTWORK_NOTHING_MONOCHROME) {
            return recolourNothingMonochrome(rendered);
        }

        int width = rendered.getWidth();
        int height = rendered.getHeight();
        int count = width * height;
        int[] pixels = new int[count];
        rendered.getPixels(pixels, 0, width, 0, 0, width, height);

        float[] sourceHsv = new float[3];
        float[] targetHsv = new float[3];
        for (int index = 0; index < count; index++) {
            int pixel = pixels[index];
            int alpha = Color.alpha(pixel);
            if (alpha == 0) continue;

            // Recolour by original hue while leaving alpha and the app's own backplate
            // treatment untouched in the mixed/gradient themes.
            Color.colorToHSV(pixel, sourceHsv);
            float hue = sourceHsv[0];
            float saturation = sourceHsv[1];
            float value = sourceHsv[2];
            if (saturation < 0.16f && (value < 0.12f || value > 0.96f)) continue;

            int themed = artworkColour(theme, hue, saturation, value);
            Color.colorToHSV(themed, targetHsv);
            targetHsv[2] = value;
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

    private static Bitmap recolourNothingMonochrome(Bitmap rendered) {
        int width = rendered.getWidth();
        int height = rendered.getHeight();
        int count = width * height;
        int[] pixels = new int[count];
        rendered.getPixels(pixels, 0, width, 0, 0, width, height);

        boolean[] backdrop = findMonochromeBackdrop(pixels, width, height);
        boolean hasBackdrop = backdrop != null && backdrop.length == count;
        int transparentCount = 0;
        for (int pixel : pixels) {
            if (Color.alpha(pixel) < 24) transparentCount++;
        }

        // Extend only the detected mask's immediate edge by matching neighbouring pixels.
        // The previous image-wide colour match could mistakenly convert a similarly coloured
        // part of the logo itself into background; local expansion keeps curved/antialiased
        // icon edges clean without erasing matching details elsewhere.
        if (hasBackdrop) {
            backdrop = expandMonochromeBackdropAtEdges(pixels, backdrop, width, height);
        }

        boolean[] separators = thickenMonochromeSeparators(
                findMonochromeSeparators(pixels, hasBackdrop ? backdrop : null, width, height),
                pixels, hasBackdrop ? backdrop : null, width, height);

        // Recover small, enclosed high-contrast details (for example a white mark inside a
        // coloured badge or a calculator symbol inside a key). Larger regions and any region
        // connected to the app backdrop/exterior are deliberately excluded, so the main logo
        // segments and app background keep the established universal conversion rules.
        boolean[] enclosedDetails = null;
        boolean[] repeatedTileInterior = null;
        boolean[] repeatedTileOutline = null;
        if (hasBackdrop || transparentCount > count * 0.025f) {
            boolean[] detectedBackdrop = hasBackdrop ? backdrop : null;
            enclosedDetails = findEnclosedContrastDetails(
                    pixels, detectedBackdrop, width, height);
            repeatedTileInterior = new boolean[count];
            repeatedTileOutline = new boolean[count];
            findRepeatedTileArtwork(pixels, detectedBackdrop, width, height,
                    repeatedTileInterior, repeatedTileOutline);
        }

        // Fixed Nothing-inspired palette: white original backplate, black foreground. For
        // transparent standalone glyphs, transparency is retained and only the glyph is black.
        final int backgroundTone = 255;
        final int foregroundTone = 0;
        final int noBackdropForeground = 0;
        final int noBackdropSeparator = 255;

        for (int index = 0; index < count; index++) {
            int pixel = pixels[index];
            int alpha = Color.alpha(pixel);
            if (alpha == 0) continue;

            int mono;
            boolean tileInterior = repeatedTileInterior != null
                    && repeatedTileInterior[index];
            boolean tileOutline = repeatedTileOutline != null
                    && repeatedTileOutline[index];
            if (tileOutline) {
                // A detected repeated key/button keeps a clear dark outline around its light
                // interior. This prevents keyboard and calculator layouts from collapsing into
                // one black mass when their key fills and foreground glyphs become monochrome.
                mono = foregroundTone;
            } else if (tileInterior) {
                mono = backgroundTone;
            } else if (hasBackdrop) {
                // White fill and white internal cut-lines preserve distinct pieces in divided
                // logos; app-background pixels are never recoloured based on their original hue.
                if (backdrop[index] || separators[index]
                        || (enclosedDetails != null && enclosedDetails[index])) {
                    mono = backgroundTone;
                } else {
                    mono = foregroundTone;
                }
            } else if (transparentCount > count * 0.025f) {
                // Source has genuine transparent margins: use a black silhouette and retain
                // white seams only at meaningful internal colour divisions.
                mono = separators[index]
                        || (enclosedDetails != null && enclosedDetails[index])
                        ? noBackdropSeparator : noBackdropForeground;
            } else {
                // When no reliable background is present in an opaque legacy icon, do not
                // invent a split through the logo. Keep its tonal detail as neutral greys.
                float luminance = pixelLuminance(pixel);
                float contrasted = 128f + (luminance - 128f) * 1.24f;
                mono = Math.max(0, Math.min(255, Math.round(contrasted)));
            }
            pixels[index] = Color.argb(alpha, mono, mono, mono);
        }

        Bitmap output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        output.setDensity(rendered.getDensity());
        output.setPixels(pixels, 0, width, 0, 0, width, height);
        rendered.recycle();
        return output;
    }

    /**
     * Preserve antialiased edges of a detected background without using a global colour
     * replacement. Only pixels directly next to the already-connected background mask can
     * be expanded, and only when multiple adjacent background pixels agree with the candidate.
     * This avoids wiping similarly coloured glyph details while retaining flat/gradient edges.
     */
    private static boolean[] expandMonochromeBackdropAtEdges(int[] pixels,
            boolean[] backdrop, int width, int height) {
        if (backdrop == null || backdrop.length != pixels.length) return backdrop;
        boolean[] expanded = backdrop.clone();
        for (int index = 0; index < backdrop.length; index++) {
            if (!backdrop[index]) continue;
            int x = index % width;
            int y = index / width;
            int reference = pixels[index];
            for (int dy = -1; dy <= 1; dy++) {
                int ny = y + dy;
                if (ny < 0 || ny >= height) continue;
                for (int dx = -1; dx <= 1; dx++) {
                    int nx = x + dx;
                    if (nx < 0 || nx >= width) continue;
                    int next = ny * width + nx;
                    if (backdrop[next] || Color.alpha(pixels[next]) < 48) continue;
                    int candidate = pixels[next];
                    if (Math.abs(Color.red(candidate) - Color.red(reference)) > 60
                            || Math.abs(Color.green(candidate) - Color.green(reference)) > 60
                            || Math.abs(Color.blue(candidate) - Color.blue(reference)) > 60) {
                        continue;
                    }
                    int agreeingNeighbours = 0;
                    for (int by = -1; by <= 1; by++) {
                        int sy = ny + by;
                        if (sy < 0 || sy >= height) continue;
                        for (int bx = -1; bx <= 1; bx++) {
                            int sx = nx + bx;
                            if (sx < 0 || sx >= width) continue;
                            int neighbour = sy * width + sx;
                            if (!backdrop[neighbour] || Color.alpha(pixels[neighbour]) < 80) continue;
                            int sample = pixels[neighbour];
                            if (Math.abs(Color.red(candidate) - Color.red(sample)) <= 60
                                    && Math.abs(Color.green(candidate) - Color.green(sample)) <= 60
                                    && Math.abs(Color.blue(candidate) - Color.blue(sample)) <= 60) {
                                agreeingNeighbours++;
                            }
                        }
                    }
                    if (agreeingNeighbours >= 2) expanded[next] = true;
                }
            }
        }
        return expanded;
    }

    private static float pixelLuminance(int pixel) {
        return Color.red(pixel) * 0.2126f
                + Color.green(pixel) * 0.7152f
                + Color.blue(pixel) * 0.0722f;
    }

    private static boolean isNearColor(int pixel, int red, int green, int blue, int tolerance) {
        return Color.alpha(pixel) >= 80
                && Math.abs(Color.red(pixel) - red) <= tolerance
                && Math.abs(Color.green(pixel) - green) <= tolerance
                && Math.abs(Color.blue(pixel) - blue) <= tolerance;
    }

    /**
     * Finds strong colour changes between neighbouring non-background pixels. The resulting
     * one-pixel separators preserve structure inside logos such as segmented play symbols,
     * while a plain glyph stays plain. Separators become white cut-lines on black glyphs.
     * This operates on all icons identically.
     */
    private static boolean[] findMonochromeSeparators(int[] pixels, boolean[] backdrop,
            int width, int height) {
        boolean[] separators = new boolean[pixels.length];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = y * width + x;
                if (Color.alpha(pixels[index]) < 160
                        || (backdrop != null && backdrop[index])) continue;
                if (x + 1 < width) {
                    int right = index + 1;
                    if (Color.alpha(pixels[right]) >= 160
                            && (backdrop == null || !backdrop[right])
                            && strongColourBoundary(pixels[index], pixels[right])) {
                        separators[index] = true;
                        separators[right] = true;
                    }
                }
                if (y + 1 < height) {
                    int down = index + width;
                    if (Color.alpha(pixels[down]) >= 160
                            && (backdrop == null || !backdrop[down])
                            && strongColourBoundary(pixels[index], pixels[down])) {
                        separators[index] = true;
                        separators[down] = true;
                    }
                }
            }
        }
        return separators;
    }

    /**
     * Widen meaningful internal colour boundaries slightly before converting them to white
     * cut-lines. A one-source-pixel seam was almost invisible at launcher icon size, so divided
     * logos (for example triangular/multisegment marks) lost their internal structure.
     * Never thicken through transparent edges or a detected backplate.
     */
    private static boolean[] thickenMonochromeSeparators(boolean[] source, int[] pixels,
            boolean[] backdrop, int width, int height) {
        if (source == null || source.length != pixels.length) return source;
        boolean[] expanded = source.clone();
        for (int index = 0; index < source.length; index++) {
            if (!source[index]) continue;
            int x = index % width;
            int y = index / width;
            for (int dy = -1; dy <= 1; dy++) {
                int ny = y + dy;
                if (ny < 0 || ny >= height) continue;
                for (int dx = -1; dx <= 1; dx++) {
                    int nx = x + dx;
                    if (nx < 0 || nx >= width) continue;
                    int next = ny * width + nx;
                    if (Color.alpha(pixels[next]) < 160
                            || (backdrop != null && backdrop.length == pixels.length
                                    && backdrop[next])) continue;
                    // Do not expand a seam onto an exterior edge, which would shrink a
                    // paper-plane, handset or letter-shaped silhouette.
                    boolean exteriorEdge = false;
                    for (int ey = -1; ey <= 1 && !exteriorEdge; ey++) {
                        int yy = ny + ey;
                        if (yy < 0 || yy >= height) {
                            exteriorEdge = true;
                            break;
                        }
                        for (int ex = -1; ex <= 1; ex++) {
                            int xx = nx + ex;
                            if (xx < 0 || xx >= width
                                    || Color.alpha(pixels[yy * width + xx]) < 24) {
                                exteriorEdge = true;
                                break;
                            }
                        }
                    }
                    if (!exteriorEdge) expanded[next] = true;
                }
            }
        }
        return expanded;
    }

    private static boolean strongColourBoundary(int first, int second) {
        int redDifference = Math.abs(Color.red(first) - Color.red(second));
        int greenDifference = Math.abs(Color.green(first) - Color.green(second));
        int blueDifference = Math.abs(Color.blue(first) - Color.blue(second));
        int largestDifference = Math.max(redDifference, Math.max(greenDifference, blueDifference));
        int totalDifference = redDifference + greenDifference + blueDifference;
        // Recover boundaries between small adjacent logo segments at icon resolution. The
        // dual threshold still avoids marking gradual gradients as seams.
        return largestDifference >= 48 && totalDifference >= 88;
    }

    /**
     * Lower, detail-only boundary threshold: the separator pass remains conservative, while
     * this pass can still segment antialiased letters and small symbols whose adjacent pixels
     * differ less than a major logo segment. Component geometry and contrast reject most
     * gradient noise before anything is changed.
     */
    private static boolean detailColourBoundary(int first, int second) {
        int redDifference = Math.abs(Color.red(first) - Color.red(second));
        int greenDifference = Math.abs(Color.green(first) - Color.green(second));
        int blueDifference = Math.abs(Color.blue(first) - Color.blue(second));
        int largestDifference = Math.max(redDifference,
                Math.max(greenDifference, blueDifference));
        int totalDifference = redDifference + greenDifference + blueDifference;
        return largestDifference >= 30 && totalDifference >= 54;
    }

    /**
     * Finds small, enclosed high-contrast colour regions that carry internal logo meaning.
     * The component is segmented using the same universal colour-boundary threshold as the
     * separator pass. A candidate is retained as a white detail only when it is compact,
     * does not touch transparent space or the detected app backplate, and differs strongly
     * in luminance from its surrounding foreground. This preserves things such as letters,
     * digits, handset shapes, camera details and symbols without special-casing any package.
     *
     * This intentionally does not recolour broad logo regions: the size and enclosure checks
     * stop major multicolour segments from being mistaken for small foreground details.
     */
    private static boolean[] findEnclosedContrastDetails(int[] pixels, boolean[] backdrop,
            int width, int height) {
        final int count = pixels.length;
        final boolean[] details = new boolean[count];
        final int[] labels = new int[count];
        java.util.Arrays.fill(labels, -1);
        final int[] queue = new int[count];
        final int[] dx = { -1, 1, 0, 0 };
        final int[] dy = { 0, 0, -1, 1 };
        final int minArea = Math.max(3, Math.round(count * 0.00012f));
        // Small glyphs such as numerals and keyboard legends can occupy more than 9% of a
        // low-resolution icon. Permit larger candidates only when the shape/contrast checks
        // below identify a compact, strongly contrasted interior region.
        final int maxArea = Math.max(minArea, Math.round(count * 0.18f));
        int nextLabel = 0;

        for (int start = 0; start < count; start++) {
            if (labels[start] >= 0 || Color.alpha(pixels[start]) < 160
                    || (backdrop != null && backdrop[start])) {
                continue;
            }

            final int label = nextLabel++;
            int head = 0;
            int tail = 0;
            queue[tail++] = start;
            labels[start] = label;
            boolean touchesExterior = false;
            int minX = width, minY = height, maxX = -1, maxY = -1;

            while (head < tail) {
                int index = queue[head++];
                int x = index % width;
                int y = index / width;
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
                int current = pixels[index];
                for (int direction = 0; direction < 4; direction++) {
                    int nx = x + dx[direction];
                    int ny = y + dy[direction];
                    if (nx < 0 || nx >= width || ny < 0 || ny >= height) {
                        touchesExterior = true;
                        continue;
                    }
                    int neighbour = ny * width + nx;
                    if (backdrop != null && backdrop[neighbour]) {
                        touchesExterior = true;
                        continue;
                    }
                    if (Color.alpha(pixels[neighbour]) < 160) {
                        // Do not reinterpret details at the outer alpha edge as enclosed marks.
                        touchesExterior = true;
                        continue;
                    }
                    if (labels[neighbour] >= 0) continue;
                    if (detailColourBoundary(current, pixels[neighbour])) continue;
                    labels[neighbour] = label;
                    queue[tail++] = neighbour;
                }
            }

            int area = tail;
            int componentWidth = maxX - minX + 1;
            int componentHeight = maxY - minY + 1;
            int boundsArea = Math.max(1, componentWidth * componentHeight);
            float aspectRatio = Math.max(componentWidth, componentHeight)
                    / (float) Math.max(1, Math.min(componentWidth, componentHeight));
            float fillRatio = area / (float) boundsArea;
            // Reject long seams and sparse gradient fragments. Text counters and icon symbols
            // are compact, bounded shapes; the limits still allow thin minus/I-like glyphs.
            if (touchesExterior || area < minArea || area > maxArea
                    || aspectRatio > 9f || fillRatio < 0.035f
                    || (area > count * 0.09f
                            && (aspectRatio > 4f || fillRatio < 0.12f))) continue;

            long componentLuminance = 0;
            long componentRed = 0, componentGreen = 0, componentBlue = 0;
            long surroundingLuminance = 0;
            long surroundingRed = 0, surroundingGreen = 0, surroundingBlue = 0;
            int surroundingSamples = 0;
            for (int i = 0; i < area; i++) {
                int index = queue[i];
                int componentPixel = pixels[index];
                componentLuminance += Math.round(pixelLuminance(componentPixel));
                componentRed += Color.red(componentPixel);
                componentGreen += Color.green(componentPixel);
                componentBlue += Color.blue(componentPixel);
                int x = index % width;
                int y = index / width;
                for (int direction = 0; direction < 4; direction++) {
                    int nx = x + dx[direction];
                    int ny = y + dy[direction];
                    if (nx < 0 || nx >= width || ny < 0 || ny >= height) continue;
                    int neighbour = ny * width + nx;
                    if (labels[neighbour] == label
                            || (backdrop != null && backdrop[neighbour])
                            || Color.alpha(pixels[neighbour]) < 160) {
                        continue;
                    }
                    if (detailColourBoundary(pixels[index], pixels[neighbour])) {
                        int surroundingPixel = pixels[neighbour];
                        surroundingLuminance += Math.round(pixelLuminance(surroundingPixel));
                        surroundingRed += Color.red(surroundingPixel);
                        surroundingGreen += Color.green(surroundingPixel);
                        surroundingBlue += Color.blue(surroundingPixel);
                        surroundingSamples++;
                    }
                }
            }

            if (surroundingSamples < Math.max(4, area / 2)) continue;
            float componentMean = componentLuminance / (float) area;
            float surroundingMean = surroundingLuminance / (float) surroundingSamples;
            int redDifference = Math.round(Math.abs(componentRed / (float) area
                    - surroundingRed / (float) surroundingSamples));
            int greenDifference = Math.round(Math.abs(componentGreen / (float) area
                    - surroundingGreen / (float) surroundingSamples));
            int blueDifference = Math.round(Math.abs(componentBlue / (float) area
                    - surroundingBlue / (float) surroundingSamples));
            int largestChannelDifference = Math.max(redDifference,
                    Math.max(greenDifference, blueDifference));
            int totalChannelDifference = redDifference + greenDifference + blueDifference;
            float luminanceDifference = Math.abs(componentMean - surroundingMean);
            boolean strongLuminanceContrast = luminanceDifference >= 34f;
            boolean strongColourContrast = largestChannelDifference >= 36
                    && totalChannelDifference >= 72;
            // Larger candidates are accepted only with clearly separated luminance, not merely
            // a hue change. This keeps broad adjacent logo segments from becoming white while
            // allowing enclosed letter/digit strokes to survive monochrome conversion.
            if ((!strongLuminanceContrast && !strongColourContrast)
                    || (area > count * 0.09f && luminanceDifference < 66f)) continue;

            // The accepted region is small, interior, and demonstrably distinct from its
            // surrounding colour. Preserve all its core pixels as white negative-space detail.
            for (int i = 0; i < area; i++) details[queue[i]] = true;
        }
        return details;
    }

    private static final class TileCandidate {
        final int label;
        final int left, top, right, bottom, area;
        final float centerX, centerY, aspectRatio, fillRatio;

        TileCandidate(int label, int left, int top, int right, int bottom, int area) {
            this.label = label;
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
            this.area = area;
            int componentWidth = right - left + 1;
            int componentHeight = bottom - top + 1;
            centerX = (left + right) * 0.5f;
            centerY = (top + bottom) * 0.5f;
            aspectRatio = Math.max(componentWidth, componentHeight)
                    / (float) Math.max(1, Math.min(componentWidth, componentHeight));
            fillRatio = area / (float) Math.max(1, componentWidth * componentHeight);
        }

        int width() { return right - left + 1; }
        int height() { return bottom - top + 1; }
        int maxDimension() { return Math.max(width(), height()); }
    }

    /**
     * Detects repeated, compact, tile-like artwork arranged in a row/column grid. These often
     * represent keyboard keys or calculator buttons. Their fills are converted to white with
     * a black contour, while adjacent letters/symbols remain solid black. Geometry—not package
     * names or icon identity—drives this pass, so irregular wordmarks and segmented brand marks
     * are left alone.
     *
     * The image is already cached and processed off the UI thread. Component labelling stays
     * bounded to the icon bitmap, and the pairwise layout check is skipped for unusually busy
     * images to avoid quadratic work on noisy artwork.
     */
    private static void findRepeatedTileArtwork(int[] pixels, boolean[] backdrop,
            int width, int height, boolean[] tileInterior, boolean[] tileOutline) {
        int count = pixels.length;
        if (count == 0 || width < 8 || height < 8) return;
        int minTileArea = Math.max(12, Math.round(count * 0.0008f));
        int maxTileArea = Math.round(count * 0.16f);
        int minDimension = Math.max(4, Math.round(Math.min(width, height) * 0.025f));
        int[] labels = new int[count];
        java.util.Arrays.fill(labels, -1);
        int[] queue = new int[count];
        java.util.ArrayList<TileCandidate> candidates = new java.util.ArrayList<>();
        int nextLabel = 0;

        for (int start = 0; start < count; start++) {
            if (labels[start] >= 0 || Color.alpha(pixels[start]) < 160
                    || (backdrop != null && backdrop[start])) continue;
            int head = 0, tail = 0;
            int minX = width, minY = height, maxX = -1, maxY = -1;
            boolean touchesBackground = false;
            boolean touchesCanvasEdge = false;
            queue[tail++] = start;
            labels[start] = nextLabel;

            while (head < tail) {
                int index = queue[head++];
                int x = index % width;
                int y = index / width;
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
                if (x == 0 || x == width - 1 || y == 0 || y == height - 1) {
                    touchesCanvasEdge = true;
                }
                int current = pixels[index];
                for (int direction = 0; direction < 4; direction++) {
                    int nx = x + (direction == 0 ? -1 : direction == 1 ? 1 : 0);
                    int ny = y + (direction == 2 ? -1 : direction == 3 ? 1 : 0);
                    if (nx < 0 || nx >= width || ny < 0 || ny >= height) {
                        touchesCanvasEdge = true;
                        continue;
                    }
                    int neighbour = ny * width + nx;
                    if (Color.alpha(pixels[neighbour]) < 160
                            || (backdrop != null && backdrop[neighbour])) {
                        touchesBackground = true;
                        continue;
                    }
                    if (labels[neighbour] >= 0) continue;
                    if (detailColourBoundary(current, pixels[neighbour])) continue;
                    labels[neighbour] = nextLabel;
                    queue[tail++] = neighbour;
                }
            }

            int componentWidth = maxX - minX + 1;
            int componentHeight = maxY - minY + 1;
            float aspect = Math.max(componentWidth, componentHeight)
                    / (float) Math.max(1, Math.min(componentWidth, componentHeight));
            float fill = tail / (float) Math.max(1, componentWidth * componentHeight);
            if (touchesCanvasEdge || !touchesBackground || tail < minTileArea
                    || tail > maxTileArea || componentWidth < minDimension
                    || componentHeight < minDimension || aspect < 0.58f
                    || aspect > 1.72f || fill < 0.40f) {
                nextLabel++;
                continue;
            }

            candidates.add(new TileCandidate(nextLabel, minX, minY, maxX, maxY, tail));
            nextLabel++;
            // Dense, highly fragmented artwork is not a reliable candidate for a tile-grid
            // heuristic. Bail out rather than spend CPU on an unbounded pairwise comparison.
            if (candidates.size() > 256) return;
        }

        if (candidates.size() < 3) return;
        boolean[] repeatedLabels = new boolean[Math.max(1, nextLabel)];
        for (int i = 0; i < candidates.size(); i++) {
            TileCandidate a = candidates.get(i);
            int rowNeighbours = 0;
            int columnNeighbours = 0;
            for (int j = 0; j < candidates.size(); j++) {
                if (i == j) continue;
                TileCandidate b = candidates.get(j);
                float widthRatio = a.width() / (float) Math.max(1, b.width());
                float heightRatio = a.height() / (float) Math.max(1, b.height());
                if (widthRatio < 0.50f || widthRatio > 2.0f
                        || heightRatio < 0.50f || heightRatio > 2.0f) continue;
                float alignmentTolerance = Math.max(2f,
                        Math.min(a.height(), b.height()) * 0.34f);
                float rowDistance = Math.abs(a.centerY - b.centerY);
                float columnDistance = Math.abs(a.centerX - b.centerX);
                float xGap = Math.max(0f, Math.max(a.left, b.left)
                        - Math.min(a.right, b.right) - 1f);
                float yGap = Math.max(0f, Math.max(a.top, b.top)
                        - Math.min(a.bottom, b.bottom) - 1f);
                float maxDim = Math.max(a.maxDimension(), b.maxDimension());
                if (rowDistance <= alignmentTolerance
                        && columnDistance >= Math.min(a.width(), b.width()) * 0.70f
                        && xGap <= maxDim * 3.2f) {
                    rowNeighbours++;
                }
                alignmentTolerance = Math.max(2f,
                        Math.min(a.width(), b.width()) * 0.34f);
                if (columnDistance <= alignmentTolerance
                        && rowDistance >= Math.min(a.height(), b.height()) * 0.70f
                        && yGap <= maxDim * 3.2f) {
                    columnNeighbours++;
                }
            }
            if (rowNeighbours >= 2 || columnNeighbours >= 2) {
                repeatedLabels[a.label] = true;
            }
        }

        boolean[] selectedPixels = new boolean[count];
        for (int i = 0; i < count; i++) {
            int label = labels[i];
            if (label >= 0 && label < repeatedLabels.length && repeatedLabels[label]) {
                tileInterior[i] = true;
                selectedPixels[i] = true;
            }
        }

        // Draw a one-source-pixel contour plus a restrained adjacent pixel where opaque.
        // Do not outline transparent exterior corners, and do not consume a different glyph.
        for (int index = 0; index < count; index++) {
            if (!selectedPixels[index]) continue;
            int x = index % width;
            int y = index / width;
            boolean touchesBackground = false;
            for (int dy = -1; dy <= 1 && !touchesBackground; dy++) {
                int ny = y + dy;
                if (ny < 0 || ny >= height) { touchesBackground = true; break; }
                for (int dx = -1; dx <= 1; dx++) {
                    int nx = x + dx;
                    if (nx < 0 || nx >= width) { touchesBackground = true; break; }
                    int neighbour = ny * width + nx;
                    if (Color.alpha(pixels[neighbour]) < 80
                            || (backdrop != null && backdrop[neighbour])) {
                        touchesBackground = true;
                        break;
                    }
                }
            }
            if (!touchesBackground) continue;
            tileOutline[index] = true;
            for (int dy = -1; dy <= 1; dy++) {
                int ny = y + dy;
                if (ny < 0 || ny >= height) continue;
                for (int dx = -1; dx <= 1; dx++) {
                    int nx = x + dx;
                    if (nx < 0 || nx >= width) continue;
                    int neighbour = ny * width + nx;
                    if (Color.alpha(pixels[neighbour]) < 80) continue;
                    if (selectedPixels[neighbour]
                            || (backdrop != null && backdrop[neighbour])) {
                        tileOutline[neighbour] = true;
                    }
                }
            }
        }
    }

    /**
     * Detect a backplate only when it occupies a broad region connected to the outer edge,
     * or forms a broad field just inside transparent icon corners. Never pick the most
     * common colour from the entire logo: that was why single Play Store colour regions
     * were incorrectly classified as backgrounds.
     */
    private static boolean[] findMonochromeBackdrop(int[] pixels, int width, int height) {
        boolean[] edgeBackdrop = findOpaqueBackdrop(pixels, width, height);
        if (isCredibleBackdropMask(edgeBackdrop, width, height, pixels.length * 0.18f,
                width * 0.55f, height * 0.55f)) {
            return edgeBackdrop;
        }
        if (!hasTransparentOuterEdge(pixels, width, height)) return null;
        return findTransparentCornerBackdrop(pixels, width, height);
    }

    private static boolean isCredibleBackdropMask(boolean[] mask, int width, int height,
            float minimumPixels, float minimumWidth, float minimumHeight) {
        if (mask == null || mask.length != width * height) return false;
        int count = 0;
        int minX = width, minY = height, maxX = -1, maxY = -1;
        for (int i = 0; i < mask.length; i++) {
            if (!mask[i]) continue;
            count++;
            int x = i % width;
            int y = i / width;
            if (x < minX) minX = x;
            if (x > maxX) maxX = x;
            if (y < minY) minY = y;
            if (y > maxY) maxY = y;
        }
        return count >= minimumPixels && maxX >= minX && maxY >= minY
                && maxX - minX + 1 >= minimumWidth
                && maxY - minY + 1 >= minimumHeight;
    }

    private static boolean hasTransparentOuterEdge(int[] pixels, int width, int height) {
        int borderCount = width * 2 + Math.max(0, height - 2) * 2;
        int transparent = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (x != 0 && x != width - 1 && y != 0 && y != height - 1) continue;
                if (Color.alpha(pixels[y * width + x]) < 24) transparent++;
            }
        }
        return transparent >= borderCount * 0.08f;
    }

    private static boolean[] findTransparentCornerBackdrop(int[] pixels, int width, int height) {
        int minX = width, minY = height, maxX = -1, maxY = -1;
        int opaqueCount = 0;
        for (int i = 0; i < pixels.length; i++) {
            if (Color.alpha(pixels[i]) < 80) continue;
            int x = i % width;
            int y = i / width;
            if (x < minX) minX = x;
            if (x > maxX) maxX = x;
            if (y < minY) minY = y;
            if (y > maxY) maxY = y;
            opaqueCount++;
        }
        if (opaqueCount < pixels.length * 0.18f || maxX <= minX || maxY <= minY) return null;
        int contentWidth = maxX - minX + 1;
        int contentHeight = maxY - minY + 1;
        if (contentWidth < width * 0.55f || contentHeight < height * 0.55f) return null;

        int bandX = Math.max(2, Math.round(contentWidth * 0.10f));
        int bandY = Math.max(2, Math.round(contentHeight * 0.10f));
        int[] ringCounts = new int[32768];
        int ringSamples = 0;
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                boolean inRing = x - minX < bandX || maxX - x < bandX
                        || y - minY < bandY || maxY - y < bandY;
                if (!inRing) continue;
                int pixel = pixels[y * width + x];
                if (Color.alpha(pixel) < 160) continue;
                int bucket = ((Color.red(pixel) >> 3) << 10)
                        | ((Color.green(pixel) >> 3) << 5)
                        | (Color.blue(pixel) >> 3);
                ringCounts[bucket]++;
                ringSamples++;
            }
        }
        if (ringSamples < (contentWidth + contentHeight) * 0.35f) return null;

        int dominantBucket = 0;
        int dominantRingCount = 0;
        for (int i = 0; i < ringCounts.length; i++) {
            if (ringCounts[i] > dominantRingCount) {
                dominantRingCount = ringCounts[i];
                dominantBucket = i;
            }
        }
        if (dominantRingCount < ringSamples * 0.48f) return null;

        int red = ((dominantBucket >> 10) & 31) * 8 + 4;
        int green = ((dominantBucket >> 5) & 31) * 8 + 4;
        int blue = (dominantBucket & 31) * 8 + 4;
        int[] allColourCounts = new int[32768];
        for (int pixel : pixels) {
            if (Color.alpha(pixel) < 80) continue;
            int bucket = ((Color.red(pixel) >> 3) << 10)
                    | ((Color.green(pixel) >> 3) << 5)
                    | (Color.blue(pixel) >> 3);
            allColourCounts[bucket]++;
        }
        if (allColourCounts[dominantBucket] < opaqueCount * 0.28f) return null;

        boolean[] mask = new boolean[pixels.length];
        int[] queue = new int[pixels.length];
        int tail = 0;
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                boolean inRing = x - minX < bandX || maxX - x < bandX
                        || y - minY < bandY || maxY - y < bandY;
                if (!inRing) continue;
                int index = y * width + x;
                if (Color.alpha(pixels[index]) >= 80
                        && isBackdropPixel(pixels[index], red, green, blue)) {
                    mask[index] = true;
                    queue[tail++] = index;
                }
            }
        }
        int head = 0;
        while (head < tail) {
            int index = queue[head++];
            int x = index % width;
            if (x > 0) tail = enqueueBackdrop(index - 1, pixels, mask, queue, tail,
                    red, green, blue);
            if (x + 1 < width) tail = enqueueBackdrop(index + 1, pixels, mask, queue, tail,
                    red, green, blue);
            if (index >= width) tail = enqueueBackdrop(index - width, pixels, mask, queue, tail,
                    red, green, blue);
            if (index + width < pixels.length) {
                tail = enqueueBackdrop(index + width, pixels, mask, queue, tail,
                        red, green, blue);
            }
        }
        if (!isCredibleBackdropMask(mask, width, height, pixels.length * 0.18f,
                contentWidth * 0.55f, contentHeight * 0.55f)
                || tail < opaqueCount * 0.36f) {
            return null;
        }
        return mask;
    }

    /**
     * Returns a mask of confidently identified built-in background pixels. A null result
     * means no reliable edge-connected backdrop was found.
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
        // Complex legacy icons sometimes use a smooth background gradient rather than a
        // solid colour. Fit a conservative four-corner colour surface and mask only the
        // edge-connected pixels that agree with it; ambiguous borders still remain intact.
        if (dominantCount < opaqueBorder * 0.24f) {
            return findGradientBackdrop(pixels, width, height, opaqueBorder);
        }
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

    private static boolean[] findGradientBackdrop(int[] pixels, int width, int height,
            int opaqueBorder) {
        if (width < 4 || height < 4) return null;
        int[] corners = new int[] {
                averageCornerColor(pixels, width, height, false, false),
                averageCornerColor(pixels, width, height, true, false),
                averageCornerColor(pixels, width, height, false, true),
                averageCornerColor(pixels, width, height, true, true)
        };

        int matchingBorder = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (x != 0 && x != width - 1 && y != 0 && y != height - 1) continue;
                int pixel = pixels[y * width + x];
                if (Color.alpha(pixel) >= 232
                        && matchesGradientBackdrop(pixel, x, y, width, height, corners, 44)) {
                    matchingBorder++;
                }
            }
        }
        if (matchingBorder < opaqueBorder * 0.68f) return null;

        boolean[] mask = new boolean[pixels.length];
        int[] queue = new int[pixels.length];
        int tail = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (x != 0 && x != width - 1 && y != 0 && y != height - 1) continue;
                int index = y * width + x;
                if (!mask[index] && Color.alpha(pixels[index]) >= 80
                        && matchesGradientBackdrop(pixels[index], x, y, width, height, corners, 44)) {
                    mask[index] = true;
                    queue[tail++] = index;
                }
            }
        }
        int head = 0;
        while (head < tail) {
            int index = queue[head++];
            int x = index % width;
            if (x > 0) tail = enqueueGradientBackdrop(index - 1, pixels, mask, queue,
                    tail, width, height, corners);
            if (x + 1 < width) tail = enqueueGradientBackdrop(index + 1, pixels, mask, queue,
                    tail, width, height, corners);
            if (index >= width) tail = enqueueGradientBackdrop(index - width, pixels, mask, queue,
                    tail, width, height, corners);
            if (index + width < pixels.length) {
                tail = enqueueGradientBackdrop(index + width, pixels, mask, queue,
                        tail, width, height, corners);
            }
        }
        return mask;
    }

    private static int averageCornerColor(int[] pixels, int width, int height,
            boolean right, boolean bottom) {
        long red = 0L, green = 0L, blue = 0L;
        int count = 0;
        int sampleWidth = Math.min(4, width);
        int sampleHeight = Math.min(4, height);
        for (int sy = 0; sy < sampleHeight; sy++) {
            int y = bottom ? height - 1 - sy : sy;
            for (int sx = 0; sx < sampleWidth; sx++) {
                int x = right ? width - 1 - sx : sx;
                int pixel = pixels[y * width + x];
                if (Color.alpha(pixel) < 232) continue;
                red += Color.red(pixel);
                green += Color.green(pixel);
                blue += Color.blue(pixel);
                count++;
            }
        }
        if (count == 0) {
            return pixels[(bottom ? height - 1 : 0) * width + (right ? width - 1 : 0)];
        }
        return Color.rgb((int) (red / count), (int) (green / count), (int) (blue / count));
    }

    private static boolean matchesGradientBackdrop(int pixel, int x, int y, int width, int height,
            int[] corners, int tolerance) {
        if (Color.alpha(pixel) < 80) return false;
        float u = x / (float) Math.max(1, width - 1);
        float v = y / (float) Math.max(1, height - 1);
        float topWeight = 1f - v;
        float leftWeight = 1f - u;
        float expectedRed = Color.red(corners[0]) * leftWeight * topWeight
                + Color.red(corners[1]) * u * topWeight
                + Color.red(corners[2]) * leftWeight * v
                + Color.red(corners[3]) * u * v;
        float expectedGreen = Color.green(corners[0]) * leftWeight * topWeight
                + Color.green(corners[1]) * u * topWeight
                + Color.green(corners[2]) * leftWeight * v
                + Color.green(corners[3]) * u * v;
        float expectedBlue = Color.blue(corners[0]) * leftWeight * topWeight
                + Color.blue(corners[1]) * u * topWeight
                + Color.blue(corners[2]) * leftWeight * v
                + Color.blue(corners[3]) * u * v;
        return Math.abs(Color.red(pixel) - expectedRed) <= tolerance
                && Math.abs(Color.green(pixel) - expectedGreen) <= tolerance
                && Math.abs(Color.blue(pixel) - expectedBlue) <= tolerance;
    }

    private static int enqueueGradientBackdrop(int index, int[] pixels, boolean[] mask, int[] queue,
            int tail, int width, int height, int[] corners) {
        if (!mask[index] && matchesGradientBackdrop(pixels[index], index % width, index / width,
                width, height, corners, 44)) {
            mask[index] = true;
            queue[tail++] = index;
        }
        return tail;
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

        // Background OFF means native app icons: no background fill, inner fill, or shape
        // clipping. Background ON applies the selected shape to both backing and artwork.
        rect.set(left, top, right, bottom);
        if (rect.width() <= 0 || rect.height() <= 0) return;
        // Fit inside the outer launcher shape only while the background option is enabled.
        // The outer shape and the icon view's measured dimensions are not changed.
        float artworkLeft = left;
        float artworkTop = top;
        float artworkRight = right;
        float artworkBottom = bottom;
        if (showBackground && fitInsideBackground) {
            float insetX = width * 0.10f;
            float insetY = height * 0.10f;
            artworkLeft += insetX;
            artworkTop += insetY;
            artworkRight -= insetX;
            artworkBottom -= insetY;
        }
        source.setBounds(Math.round(artworkLeft), Math.round(artworkTop),
                Math.round(artworkRight), Math.round(artworkBottom));
        if (!showBackground) {
            source.draw(canvas);
            return;
        }

        if (shapePathDirty) {
            setShapePath(rect);
            shapePathDirty = false;
        }
        paint.setColor(backgroundColor);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawPath(path, paint);

        int save = canvas.save();
        canvas.clipPath(path);
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
        // A narrower crown leans left; the wider base leans right. The side curves
        // reach both horizontal bounds while remaining inside the original square box.
        path.moveTo(left + w * 0.42f, top);
        path.cubicTo(left + w * 0.20f, top,
                left + w * 0.06f, top + h * 0.18f,
                left + w * 0.06f, top + h * 0.38f);
        path.cubicTo(left + w * 0.05f, top + h * 0.48f,
                left, top + h * 0.51f,
                left, top + h * 0.56f);
        path.cubicTo(left, top + h * 0.75f,
                left + w * 0.23f, bottom,
                left + w * 0.54f, bottom);
        path.cubicTo(left + w * 0.81f, bottom,
                right, bottom - h * 0.22f,
                right, top + h * 0.56f);
        path.cubicTo(right, top + h * 0.38f,
                right - w * 0.05f, top + h * 0.17f,
                left + w * 0.42f, top);
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
