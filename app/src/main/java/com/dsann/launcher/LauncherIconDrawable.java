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

    // Scale the processed-artwork cache with the VM heap, but keep it bounded for low-RAM
    // phones. The small composite cache is independent so complex shape clipping is reused
    // during later scrolls without retaining an unbounded second copy of every icon.
    private static final int MAX_ARTWORK_CACHE_BYTES = calculateArtworkCacheCapacity();
    private static final int MAX_COMPOSITION_CACHE_BYTES =
            Math.max(4 * 1024 * 1024,
                    Math.min(8 * 1024 * 1024, MAX_ARTWORK_CACHE_BYTES / 4));
    private static final boolean[] NO_BACKDROP_MASK = new boolean[0];
    private static final LruCache<ArtworkCacheKey, Bitmap> ARTWORK_CACHE =
            new LruCache<ArtworkCacheKey, Bitmap>(MAX_ARTWORK_CACHE_BYTES) {
                @Override protected int sizeOf(ArtworkCacheKey key, Bitmap value) {
                    return value == null ? 0 : value.getByteCount();
                }
            };
    private static final LruCache<CompositionCacheKey, Bitmap> COMPOSITION_CACHE =
            new LruCache<CompositionCacheKey, Bitmap>(MAX_COMPOSITION_CACHE_BYTES) {
                @Override protected int sizeOf(CompositionCacheKey key, Bitmap value) {
                    return value == null ? 0 : value.getByteCount();
                }
            };
    private static final Paint COMPOSITION_BITMAP_PAINT =
            new Paint(Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);

    private static int calculateArtworkCacheCapacity() {
        long maxHeap = Runtime.getRuntime().maxMemory();
        long target = maxHeap / 8L;
        target = Math.max(16L * 1024L * 1024L,
                Math.min(32L * 1024L * 1024L, target));
        return (int) target;
    }

    /** Trim cached bitmaps only under system memory pressure; keep them when another app opens. */
    static void trimArtworkCaches(int level) {
        if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_COMPLETE) {
            ARTWORK_CACHE.evictAll();
            COMPOSITION_CACHE.evictAll();
        } else if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_MODERATE) {
            ARTWORK_CACHE.trimToSize(Math.max(1, MAX_ARTWORK_CACHE_BYTES / 4));
            COMPOSITION_CACHE.trimToSize(Math.max(1, MAX_COMPOSITION_CACHE_BYTES / 4));
        } else if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_BACKGROUND
                || level == android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL) {
            ARTWORK_CACHE.trimToSize(Math.max(1, MAX_ARTWORK_CACHE_BYTES / 2));
            COMPOSITION_CACHE.trimToSize(Math.max(1, MAX_COMPOSITION_CACHE_BYTES / 2));
        } else if (level == android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            ARTWORK_CACHE.trimToSize(Math.max(1, MAX_ARTWORK_CACHE_BYTES * 3 / 4));
            COMPOSITION_CACHE.trimToSize(Math.max(1, MAX_COMPOSITION_CACHE_BYTES * 3 / 4));
        }
    }
    // Icon conversion must never run on GridView's UI-thread bind/scroll path. Results are
    // shared by cache key, then applied to any still-matching drawable instances on main.
    private static final Handler ARTWORK_HANDLER = new Handler(Looper.getMainLooper());
    private static final Object ARTWORK_REQUEST_LOCK = new Object();
    private static final java.util.Map<ArtworkCacheKey, java.util.ArrayList<WeakReference<LauncherIconDrawable>>>
            ARTWORK_WAITERS = new java.util.HashMap<>();
    private static final java.util.Set<ArtworkCacheKey> ARTWORK_PENDING = new java.util.HashSet<>();
    private static final java.util.concurrent.ExecutorService ARTWORK_EXECUTOR =
            java.util.concurrent.Executors.newSingleThreadExecutor(new java.util.concurrent.ThreadFactory() {
                @Override public Thread newThread(Runnable runnable) {
                    Thread thread = new Thread(() -> {
                        try {
                            // Icon recolouring is non-interactive work. Keep it below the UI
                            // thread's priority so first-open bursts cannot starve animations.
                            android.os.Process.setThreadPriority(
                                    android.os.Process.THREAD_PRIORITY_LOWEST);
                        } catch (RuntimeException ignored) {}
                        runnable.run();
                    }, "LauncherIconArtwork");
                    thread.setDaemon(true);
                    return thread;
                }
            });

    private Drawable source;
    // Read from the worker before starting an expensive icon conversion; make these fields
    // visible across the UI/worker boundary as GridView recycles drawables during scrolling.
    private volatile Drawable originalSource;
    private volatile Drawable.ConstantState originalSourceState;
    private volatile String sourceCacheToken;
    private volatile int artworkTheme = -1;
    private boolean sourceIsProcessedArtwork;
    private int drawableAlpha = 255;
    private ColorFilter drawableColorFilter;
    // Force the first appearance bind through the cache so Original icons get the outline too.
    private boolean externalShapeClip;
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

    /** Cache key for a completed background-plus-artwork icon at the target draw size. */
    private static final class CompositionCacheKey {
        final ArtworkCacheKey artwork;
        final int width;
        final int height;
        final int shape;
        final int backgroundColor;
        final boolean fitInsideBackground;
        final boolean externalShapeClip;

        CompositionCacheKey(ArtworkCacheKey artwork, int width, int height, int shape,
                int backgroundColor, boolean fitInsideBackground, boolean externalShapeClip) {
            this.artwork = artwork;
            this.width = width;
            this.height = height;
            this.shape = shape;
            this.backgroundColor = backgroundColor;
            this.fitInsideBackground = fitInsideBackground;
            this.externalShapeClip = externalShapeClip;
        }

        @Override public int hashCode() {
            int result = artwork.hashCode();
            result = 31 * result + width;
            result = 31 * result + height;
            result = 31 * result + shape;
            result = 31 * result + backgroundColor;
            result = 31 * result + (fitInsideBackground ? 1 : 0);
            result = 31 * result + (externalShapeClip ? 1 : 0);
            return result;
        }

        @Override public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof CompositionCacheKey)) return false;
            CompositionCacheKey key = (CompositionCacheKey) other;
            return width == key.width && height == key.height && shape == key.shape
                    && backgroundColor == key.backgroundColor
                    && fitInsideBackground == key.fitInsideBackground
                    && externalShapeClip == key.externalShapeClip
                    && artwork.equals(key.artwork);
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

    /**
     * Returns the real accent palette used by each recoloured app-icon artwork theme.
     * Original Colours has no shared palette, so the clock keeps its Home Studio fallback.
     * Nothing Monochrome uses the same black/white pair as the icon treatment.
     */
    static int[] clockTransitionPaletteForArtwork(int theme) {
        switch (clampArtworkTheme(theme)) {
            case ARTWORK_SPECTRUM_MIX:
                return MIXED_PALETTE.clone();
            case ARTWORK_OCEAN_GRADIENT:
                return OCEAN_PALETTE.clone();
            case ARTWORK_SUNSET_GRADIENT:
                return SUNSET_PALETTE.clone();
            case ARTWORK_NOTHING_MONOCHROME:
                return new int[] {Color.WHITE, Color.BLACK, Color.WHITE};
            case ARTWORK_ORIGINAL:
            default:
                return null;
        }
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

    void setExternalShapeClip(boolean enabled) {
        if (externalShapeClip == enabled) return;
        externalShapeClip = enabled;
        invalidateSelf();
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
        sourceIsProcessedArtwork = false;
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
        sourceIsProcessedArtwork = true;
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
            // GridView recycles visible cells rapidly. Skip queued theme conversion if no
            // drawable still represents this app/style; otherwise old icons compete with the
            // current fling for CPU and can make the scroll feel sticky.
            if (!hasCurrentArtworkWaiter(key, theme)) return;
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

    private static boolean hasCurrentArtworkWaiter(ArtworkCacheKey key, int theme) {
        synchronized (ARTWORK_REQUEST_LOCK) {
            java.util.ArrayList<WeakReference<LauncherIconDrawable>> waiters =
                    ARTWORK_WAITERS.get(key);
            if (waiters == null || waiters.isEmpty()) {
                ARTWORK_WAITERS.remove(key);
                ARTWORK_PENDING.remove(key);
                return false;
            }
            java.util.Iterator<WeakReference<LauncherIconDrawable>> iterator = waiters.iterator();
            boolean current = false;
            while (iterator.hasNext()) {
                LauncherIconDrawable drawable = iterator.next().get();
                if (drawable == null || !drawable.matchesArtworkRequest(key, theme)) {
                    iterator.remove();
                } else {
                    current = true;
                }
            }
            if (!current) {
                ARTWORK_WAITERS.remove(key);
                ARTWORK_PENDING.remove(key);
            }
            return current;
        }
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

    /**
     * Converts original icon pixels to a two-tone Nothing-style artwork using the source
     * colour regions directly, not text/shape heuristics:
     * - neutral white areas remain white;
     * - every other filled area (including original black) becomes black;
     * - junctions between non-white source colours become white seams;
     * - strong junctions use a thin seam, while subtle/near-colour transitions use a
     *   one-source-pixel seam;
     * - no synthetic seam is added where either side of a junction is white.
     *
     * Transparent margins stay transparent. All decisions are based on the original raster,
     * before any monochrome pixels are written, so the output cannot feed back into detection.
     */
    private static Bitmap recolourNothingMonochrome(Bitmap rendered) {
        final int width = rendered.getWidth();
        final int height = rendered.getHeight();
        final int count = width * height;
        final int[] source = new int[count];
        rendered.getPixels(source, 0, width, 0, 0, width, height);

        final boolean[] sourceWhite = new boolean[count];
        for (int i = 0; i < count; i++) {
            sourceWhite[i] = isSourceWhiteSegment(source[i]);
        }

        // Original dark artwork takes priority over generated junction seams. Protect
        // near-black source pixels plus one adjacent ring of dark antialiased edge pixels.
        // The ring prevents a softened black outline from being recoloured white.
        final boolean[] protectedSourceBlack =
                findProtectedSourceBlackPixels(source, width, height);

        final boolean[] seams = new boolean[count];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                final int index = y * width + x;
                if (x + 1 < width) {
                    final int right = index + 1;
                    final int delta = sourceColourTransitionDelta(
                            source, sourceWhite, index, right);
                    if (delta > 0
                            && !isThinColourStripBetweenColourAndWhite(
                                    sourceWhite, width, height, index, right, 1, 0)
                            && isLocalHorizontalTransitionPeak(
                                    source, sourceWhite, width, height, x, y, delta)) {
                        final int maxDifference = largestSourceChannelDifference(
                                source[index], source[right]);
                        if (maxDifference >= 46 && delta >= 76) {
                            // Strong colour junction. Never paint over source-black detail;
                            // when only one side is protected, keep the seam on the other side.
                            if (!protectedSourceBlack[index]) seams[index] = true;
                            if (!protectedSourceBlack[right]) seams[right] = true;
                        } else if (maxDifference >= 5 && delta >= 9) {
                            // Subtle junctions normally use one pixel. If that pixel belongs
                            // to a protected dark stroke, shift the seam to the other side.
                            if (!protectedSourceBlack[index]) {
                                seams[index] = true;
                            } else if (!protectedSourceBlack[right]) {
                                seams[right] = true;
                            }
                        }
                    }
                }
                if (y + 1 < height) {
                    final int down = index + width;
                    final int delta = sourceColourTransitionDelta(
                            source, sourceWhite, index, down);
                    if (delta > 0
                            && !isThinColourStripBetweenColourAndWhite(
                                    sourceWhite, width, height, index, down, 0, 1)
                            && isLocalVerticalTransitionPeak(
                                    source, sourceWhite, width, height, x, y, delta)) {
                        final int maxDifference = largestSourceChannelDifference(
                                source[index], source[down]);
                        if (maxDifference >= 46 && delta >= 76) {
                            // Keep original dark strokes intact and place the seam only on
                            // the unprotected side when a junction touches dark artwork.
                            if (!protectedSourceBlack[index]) seams[index] = true;
                            if (!protectedSourceBlack[down]) seams[down] = true;
                        } else if (maxDifference >= 5 && delta >= 9) {
                            if (!protectedSourceBlack[index]) {
                                seams[index] = true;
                            } else if (!protectedSourceBlack[down]) {
                                seams[down] = true;
                            }
                        }
                    }
                }
            }
        }

        final int[] outputPixels = new int[count];
        for (int i = 0; i < count; i++) {
            final int alpha = Color.alpha(source[i]);
            if (alpha == 0) {
                outputPixels[i] = Color.TRANSPARENT;
                continue;
            }
            // A seam is never allowed to supersede an original black/dark pixel.
            final int tone = sourceWhite[i]
                    || (seams[i] && !protectedSourceBlack[i]) ? 255 : 0;
            outputPixels[i] = Color.argb(alpha, tone, tone, tone);
        }

        Bitmap output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        output.setDensity(rendered.getDensity());
        output.setPixels(outputPixels, 0, width, 0, 0, width, height);
        rendered.recycle();
        return output;
    }

    /**
     * Treat only neutral, light pixels as original white artwork. Saturated pale colours
     * (yellow, cyan, pastel fills, etc.) are still coloured fields and therefore become black.
     */
    private static boolean isSourceWhiteSegment(int pixel) {
        if (Color.alpha(pixel) < 32) return false;
        int red = Color.red(pixel);
        int green = Color.green(pixel);
        int blue = Color.blue(pixel);
        int minimum = Math.min(red, Math.min(green, blue));
        int maximum = Math.max(red, Math.max(green, blue));
        return minimum >= 190 && maximum - minimum <= 24;
    }

    /**
     * Identifies original dark artwork that must never be overwritten by generated white seams.
     * Pure/near-black source pixels seed the mask; one adjacent ring of darkened edge pixels is
     * also protected to preserve antialiased black strokes. Brighter neighbouring colour pixels
     * remain eligible for a seam, so a black-to-colour junction can still be outlined on the
     * coloured side rather than erasing the original dark drawing.
     */
    private static boolean[] findProtectedSourceBlackPixels(
            int[] source, int width, int height) {
        final int count = width * height;
        final boolean[] protectedPixels = new boolean[count];

        for (int i = 0; i < count; i++) {
            protectedPixels[i] = isSourceBlackSegment(source[i]);
        }

        // Protect only one ring from the original dark mask (not a recursively growing mask).
        // This catches antialias pixels blended against an adjacent icon colour without
        // swallowing entire coloured regions into the protected area.
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                final int index = y * width + x;
                if (protectedPixels[index] || Color.alpha(source[index]) < 32) continue;
                final int pixel = source[index];
                final int maximum = Math.max(Color.red(pixel),
                        Math.max(Color.green(pixel), Color.blue(pixel)));
                if (maximum > 192) continue;

                boolean touchesDarkCore = false;
                for (int dy = -1; dy <= 1 && !touchesDarkCore; dy++) {
                    final int ny = y + dy;
                    if (ny < 0 || ny >= height) continue;
                    for (int dx = -1; dx <= 1; dx++) {
                        if (dx == 0 && dy == 0) continue;
                        final int nx = x + dx;
                        if (nx < 0 || nx >= width) continue;
                        if (isSourceBlackSegment(source[ny * width + nx])) {
                            touchesDarkCore = true;
                            break;
                        }
                    }
                }
                if (touchesDarkCore) protectedPixels[index] = true;
            }
        }
        return protectedPixels;
    }

    /** Treat the original black and near-black filled pixels as immutable dark artwork. */
    private static boolean isSourceBlackSegment(int pixel) {
        if (Color.alpha(pixel) < 32) return false;
        int maximum = Math.max(Color.red(pixel),
                Math.max(Color.green(pixel), Color.blue(pixel)));
        return maximum <= 64;
    }

    /**
     * Returns zero for transparent or white-to-anything pairs. Otherwise returns the total
     * RGB difference for a junction between two non-white source colours.
     */
    private static int sourceColourTransitionDelta(int[] source, boolean[] sourceWhite,
            int firstIndex, int secondIndex) {
        if (Color.alpha(source[firstIndex]) < 160
                || Color.alpha(source[secondIndex]) < 160
                || sourceWhite[firstIndex] || sourceWhite[secondIndex]) {
            return 0;
        }
        int first = source[firstIndex];
        int second = source[secondIndex];
        return Math.abs(Color.red(first) - Color.red(second))
                + Math.abs(Color.green(first) - Color.green(second))
                + Math.abs(Color.blue(first) - Color.blue(second));
    }

    private static int largestSourceChannelDifference(int first, int second) {
        return Math.max(
                Math.abs(Color.red(first) - Color.red(second)),
                Math.max(
                        Math.abs(Color.green(first) - Color.green(second)),
                        Math.abs(Color.blue(first) - Color.blue(second))));
    }

    /**
     * Prevent generated white seams on colour junctions when a coloured strip no more than
     * two source pixels wide lies between another colour and white. The artwork is normalized
     * to 96–192 px before this pass; two source pixels are the pipeline's approximation of
     * a 1 dp hairline at launcher-icon size.
     *
     * Search outward from both endpoints of the candidate colour transition along its axis.
     * A white pixel within two source pixels suppresses this seam; wider coloured regions
     * remain eligible for the existing seam rules.
     */
    private static boolean isThinColourStripBetweenColourAndWhite(
            boolean[] sourceWhite, int width, int height,
            int firstIndex, int secondIndex, int dx, int dy) {
        if (sourceWhite[firstIndex] || sourceWhite[secondIndex]) return false;

        int firstX = firstIndex % width;
        int firstY = firstIndex / width;
        int secondX = secondIndex % width;
        int secondY = secondIndex / width;

        for (int distance = 1; distance <= 2; distance++) {
            int firstOutX = firstX - dx * distance;
            int firstOutY = firstY - dy * distance;
            if (firstOutX >= 0 && firstOutX < width
                    && firstOutY >= 0 && firstOutY < height
                    && sourceWhite[firstOutY * width + firstOutX]) {
                return true;
            }

            int secondOutX = secondX + dx * distance;
            int secondOutY = secondY + dy * distance;
            if (secondOutX >= 0 && secondOutX < width
                    && secondOutY >= 0 && secondOutY < height
                    && sourceWhite[secondOutY * width + secondOutX]) {
                return true;
            }
        }
        return false;
    }

    /**
     * Non-maximum suppression keeps colour transitions as lines instead of tracing every
     * pixel of a smooth gradient. The strongest adjacent-pixel difference along that axis wins.
     */
    private static boolean isLocalHorizontalTransitionPeak(int[] source, boolean[] sourceWhite,
            int width, int height, int x, int y, int delta) {
        int index = y * width + x;
        int previous = x > 0
                ? sourceColourTransitionDelta(source, sourceWhite, index - 1, index) : 0;
        int next = x + 2 < width
                ? sourceColourTransitionDelta(source, sourceWhite, index + 1, index + 2) : 0;
        return delta >= previous && delta > next;
    }

    private static boolean isLocalVerticalTransitionPeak(int[] source, boolean[] sourceWhite,
            int width, int height, int x, int y, int delta) {
        int index = y * width + x;
        int previous = y > 0
                ? sourceColourTransitionDelta(source, sourceWhite, index - width, index) : 0;
        int next = y + 2 < height
                ? sourceColourTransitionDelta(source, sourceWhite, index + width, index + 2 * width) : 0;
        return delta >= previous && delta > next;
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
        // A completed icon bitmap avoids drawing the backing, rasterizing the source, and
        // clipping custom silhouettes on every frame. The key contains every appearance
        // setting that affects pixels, so changing a setting safely selects another variant.
        Bitmap composedIcon = getCachedComposedIconBitmap(
                bounds.left, bounds.top, bounds.width(), bounds.height());
        if (composedIcon != null) {
            canvas.drawBitmap(composedIcon, bounds.left, bounds.top, COMPOSITION_BITMAP_PAINT);
            return;
        }

        if (externalShapeClip) {
            // The containing ImageView clips these supported shapes natively. Drawing the
            // backing and artwork directly avoids Canvas.clipPath on every scroll frame.
            if (shape == SHAPE_ROUND) {
                canvas.drawOval(rect, paint);
            } else {
                float corner = Math.min(rect.width(), rect.height()) * 0.23f;
                canvas.drawRoundRect(rect, corner, corner, paint);
            }
            source.draw(canvas);
            return;
        }

        canvas.drawPath(path, paint);
        int save = canvas.save();
        canvas.clipPath(path);
        source.draw(canvas);
        canvas.restoreToCount(save);
    }

    private Bitmap getCachedComposedIconBitmap(int left, int top, int width, int height) {
        // Keep Nothing Monochrome logos on the direct drawable path. Its thin internal seams,
        // cut-outs, and recovered letter details are sensitive to a second full-icon raster pass;
        // cache the processed artwork bitmap, but let the platform canvas composite it directly
        // so those structural details stay consistent with the pre-composition renderer.
        // Also avoid caching the temporary un-themed logo shown while conversion is pending, or
        // altered-alpha/filtered Drawables whose output depends on live state.
        if (!showBackground
                || clampArtworkTheme(artworkTheme) == ARTWORK_NOTHING_MONOCHROME
                || !sourceIsProcessedArtwork || !(source instanceof BitmapDrawable)
                || drawableAlpha != 255 || drawableColorFilter != null
                || originalSourceState == null || width <= 0 || height <= 0) {
            return null;
        }

        ArtworkCacheKey artworkKey = new ArtworkCacheKey(
                originalSourceState, sourceCacheToken, clampArtworkTheme(artworkTheme));
        CompositionCacheKey key = new CompositionCacheKey(artworkKey, width, height, shape,
                backgroundColor, fitInsideBackground, externalShapeClip);
        Bitmap cached = COMPOSITION_CACHE.get(key);
        if (cached != null) return cached;

        Bitmap rendered = null;
        try {
            rendered = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            rendered.setDensity(Resources.getSystem().getDisplayMetrics().densityDpi);
            Canvas offscreen = new Canvas(rendered);
            offscreen.translate(-left, -top);
            if (externalShapeClip) {
                if (shape == SHAPE_ROUND) {
                    offscreen.drawOval(rect, paint);
                } else {
                    float corner = Math.min(rect.width(), rect.height()) * 0.23f;
                    offscreen.drawRoundRect(rect, corner, corner, paint);
                }
                source.draw(offscreen);
            } else {
                offscreen.drawPath(path, paint);
                int save = offscreen.save();
                offscreen.clipPath(path);
                source.draw(offscreen);
                offscreen.restoreToCount(save);
            }
            if (rendered.getByteCount() > MAX_COMPOSITION_CACHE_BYTES) {
                rendered.recycle();
                return null;
            }
            COMPOSITION_CACHE.put(key, rendered);
            return rendered;
        } catch (RuntimeException | OutOfMemoryError ignored) {
            if (rendered != null && !rendered.isRecycled()) rendered.recycle();
            return null;
        }
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
        drawableAlpha = alpha;
        if (source != null) source.setAlpha(alpha);
        invalidateSelf();
    }

    @Override public void setColorFilter(ColorFilter colorFilter) {
        drawableColorFilter = colorFilter;
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
