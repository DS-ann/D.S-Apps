package com.dsann.launcher;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.Outline;
import android.view.ViewOutlineProvider;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Shared app-cell renderer for the home screen and app drawer. */
public final class LauncherAppAdapter extends BaseAdapter {
    public interface Actions {
        void onAppClick(ResolveInfo app, View sourceView);
        void onAppLongClick(ResolveInfo app, boolean onHome);
    }

    private final Activity activity;
    private final SharedPreferences preferences;
    private final PackageManager packageManager;
    private final List<ResolveInfo> allApps;
    private final List<ResolveInfo> drawerApps;
    private final List<String> fixedPackages;
    private final List<ResolveInfo> homeItems = new ArrayList<>();
    private final boolean drawer;
    private final String iconSizeKey;
    private final String fontSizeKey;
    private final String showLabelsKey;
    private final Actions actions;
    private final Map<String, String> labelCache = new HashMap<>();
    private final Map<String, String> packageArtworkIdentityCache =
            new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<String, android.graphics.drawable.Drawable.ConstantState> iconCache = new HashMap<>();
    // A recycled GridView cell can be rebound before its icon arrives. Track weak view waiters
    // so the completion updates only cells still representing that app.
    private final Map<String, ArrayList<java.lang.ref.WeakReference<ImageView>>> iconLoadWaiters =
            new HashMap<>();
    private final java.util.Set<String> pendingIconLoads = new java.util.HashSet<>();
    private int iconCacheGeneration;
    private Runnable dataChangedListener;
    // Temporary ImageViews keep artwork waiters alive while all drawer icons are prewarmed at
    // Home. Completed entries are released immediately so the cache remains suitable for low-RAM
    // Android 6 devices rather than retaining a second permanent icon grid.
    private final android.os.Handler artworkPrewarmHandler =
            new android.os.Handler(android.os.Looper.getMainLooper());
    private final ArrayList<ImageView> artworkPrewarmWaiters = new ArrayList<>();
    private int artworkPrewarmGeneration;
    private long lastArtworkPrewarmSignature = Long.MIN_VALUE;
    private boolean artworkPrewarmComplete;

    // PackageManager icon resolution is I/O/resource-heavy on Android 6. Never do it on the
    // UI thread during the drawer's opening animation or while GridView is scrolling.
    private static final ViewOutlineProvider ROUNDED_SQUARE_OUTLINE =
            new ViewOutlineProvider() {
                @Override public void getOutline(View view, Outline outline) {
                    int width = view.getWidth();
                    int height = view.getHeight();
                    if (width <= 0 || height <= 0) {
                        outline.setEmpty();
                        return;
                    }
                    float radius = Math.min(width, height) * 0.23f;
                    outline.setRoundRect(0, 0, width, height, radius);
                }
            };
    private static final ViewOutlineProvider ROUND_OUTLINE = new ViewOutlineProvider() {
        @Override public void getOutline(View view, Outline outline) {
            int width = view.getWidth();
            int height = view.getHeight();
            if (width <= 0 || height <= 0) {
                outline.setEmpty();
                return;
            }
            outline.setOval(0, 0, width, height);
        }
    };

    private static final java.util.concurrent.ExecutorService ICON_LOAD_EXECUTOR =
            java.util.concurrent.Executors.newFixedThreadPool(1,
                    new java.util.concurrent.ThreadFactory() {
                        @Override public Thread newThread(Runnable runnable) {
                            Thread thread = new Thread(() -> {
                                try {
                                    android.os.Process.setThreadPriority(
                                            android.os.Process.THREAD_PRIORITY_BACKGROUND);
                                } catch (RuntimeException ignored) {}
                                runnable.run();
                            }, "LauncherAppIconLoader");
                            thread.setDaemon(true);
                            return thread;
                        }
                    });

    public LauncherAppAdapter(Activity activity, SharedPreferences preferences, PackageManager packageManager,
            List<ResolveInfo> allApps, List<ResolveInfo> drawerApps, List<String> fixedPackages,
            boolean drawer, String iconSizeKey, String fontSizeKey, String showLabelsKey, Actions actions) {
        this.activity = activity;
        this.preferences = preferences;
        this.packageManager = packageManager;
        this.allApps = allApps;
        this.drawerApps = drawerApps;
        this.fixedPackages = fixedPackages;
        this.drawer = drawer;
        if (!drawer && fixedPackages != null) {
            Map<String, ResolveInfo> appsByPackage = new HashMap<>();
            for (ResolveInfo app : allApps) {
                if (app != null && app.activityInfo != null) {
                    appsByPackage.put(app.activityInfo.packageName, app);
                }
            }
            for (String packageName : fixedPackages) {
                ResolveInfo app = appsByPackage.get(packageName);
                if (app != null) homeItems.add(app);
            }
        }
        this.iconSizeKey = iconSizeKey;
        this.fontSizeKey = fontSizeKey;
        this.showLabelsKey = showLabelsKey;
        this.actions = actions;
    }

    private List<ResolveInfo> items() {
        return drawer ? drawerApps : homeItems;
    }

    private int dp(float value) {
        return (int) (value * activity.getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView makeLabel() {
        TextView label = new TextView(activity);
        label.setTextSize(10);
        label.setTextColor(Color.rgb(236, 239, 249));
        label.setShadowLayer(dp(1), 0f, dp(1), Color.argb(180, 0, 0, 0));
        label.setGravity(Gravity.CENTER);
        label.setSingleLine(true);
        label.setMaxLines(1);
        label.setEllipsize(android.text.TextUtils.TruncateAt.END);
        label.setIncludeFontPadding(false);
        return label;
    }

    private String key(ResolveInfo app) {
        if (app == null || app.activityInfo == null) return "";
        return app.activityInfo.packageName + "/" + app.activityInfo.name;
    }

    /**
     * Artwork keys are scoped to one installed package version, not to a global package-list
     * generation. This keeps every unaffected application's artwork reusable across installs.
     */
    private String artworkSourceToken(String appKey, ResolveInfo app) {
        String packageName = app != null && app.activityInfo != null
                ? app.activityInfo.packageName : null;
        if (packageName == null) return appKey;
        String identity = packageArtworkIdentityCache.get(packageName);
        if (identity == null) identity = resolveAndCachePackageArtworkIdentity(app);
        return appKey + "#package-identity=" + identity;
    }

    /** Called by the drawer's icon worker so PackageManager metadata lookup stays off the UI thread. */
    private String resolveAndCachePackageArtworkIdentity(ResolveInfo app) {
        if (app == null || app.activityInfo == null) return "unknown";
        String packageName = app.activityInfo.packageName;
        String cached = packageArtworkIdentityCache.get(packageName);
        if (cached != null) return cached;
        String identity;
        try {
            android.content.pm.PackageInfo info = packageManager.getPackageInfo(packageName, 0);
            android.content.pm.ApplicationInfo application = info.applicationInfo;
            String sourceDir = application == null ? "" : String.valueOf(application.sourceDir);
            int uid = application == null ? -1 : application.uid;
            identity = "version=" + info.versionCode
                    + ";firstInstall=" + info.firstInstallTime
                    + ";lastUpdate=" + info.lastUpdateTime
                    + ";uid=" + uid
                    + ";source=" + sourceDir;
        } catch (PackageManager.NameNotFoundException | RuntimeException ignored) {
            android.content.pm.ApplicationInfo application = app.activityInfo.applicationInfo;
            String sourceDir = application == null ? "" : String.valueOf(application.sourceDir);
            long modified = 0L;
            long length = 0L;
            try {
                java.io.File source = application == null || application.sourceDir == null
                        ? null : new java.io.File(application.sourceDir);
                if (source != null) {
                    modified = source.lastModified();
                    length = source.length();
                }
            } catch (RuntimeException ignoredFile) {}
            identity = "fallback=" + sourceDir + ";uid="
                    + (application == null ? -1 : application.uid)
                    + ";modified=" + modified + ";length=" + length;
        }
        // Map.putIfAbsent() on java.util.Map is unavailable on Android 6 (API 23).
        // Synchronize this small cache section instead of invoking the API-24 interface method.
        synchronized (packageArtworkIdentityCache) {
            String prior = packageArtworkIdentityCache.get(packageName);
            if (prior != null) return prior;
            packageArtworkIdentityCache.put(packageName, identity);
            return identity;
        }
    }

    /** Forget one package's metadata after install/removal/update, without touching other apps. */
    void invalidatePackageArtworkIdentity(String packageName) {
        if (packageName != null) packageArtworkIdentityCache.remove(packageName);
    }

    String cachedLabel(ResolveInfo app) {
        String key = key(app);
        String value = labelCache.get(key);
        if (value == null) {
            try {
                value = String.valueOf(app.loadLabel(packageManager));
            } catch (RuntimeException ignored) {
                value = "";
            }
            labelCache.put(key, value);
        }
        return value;
    }

    /** Seed labels already retrieved during MainActivity.loadApps(), avoiding UI-thread lookups. */
    void seedLabelCache(Map<String, String> labels) {
        labelCache.clear();
        if (labels != null) labelCache.putAll(labels);
    }

    private android.graphics.drawable.Drawable cachedIcon(ResolveInfo app,
            ImageView imageView, String appKey) {
        android.graphics.drawable.Drawable.ConstantState state = iconCache.get(appKey);
        if (state != null) return state.newDrawable(activity.getResources());

        // Keep the established synchronous behavior for non-drawer uses; drawer misses are
        // populated asynchronously so package resource loading cannot stall animation/scroll.
        if (drawer) {
            requestIconLoad(app, imageView, appKey);
            return null;
        }
        android.graphics.drawable.Drawable loaded = app.loadIcon(packageManager);
        if (loaded == null) return null;
        state = loaded.getConstantState();
        if (state != null) iconCache.put(appKey, state);
        return loaded;
    }

    private void requestIconLoad(ResolveInfo app, ImageView imageView, String appKey) {
        ArrayList<java.lang.ref.WeakReference<ImageView>> waiters = iconLoadWaiters.get(appKey);
        if (waiters == null) {
            waiters = new ArrayList<>();
            iconLoadWaiters.put(appKey, waiters);
        }
        boolean alreadyWaiting = false;
        for (java.lang.ref.WeakReference<ImageView> reference : waiters) {
            if (reference.get() == imageView) {
                alreadyWaiting = true;
                break;
            }
        }
        if (!alreadyWaiting) waiters.add(new java.lang.ref.WeakReference<>(imageView));
        if (!pendingIconLoads.add(appKey)) return;

        final int generation = iconCacheGeneration;
        ICON_LOAD_EXECUTOR.execute(() -> {
            android.graphics.drawable.Drawable loaded = null;
            android.graphics.drawable.Drawable.ConstantState loadedState = null;
            try {
                // Resolve the package-specific artwork identity alongside the icon, on this
                // background worker. Unrelated package changes do not alter this identity.
                resolveAndCachePackageArtworkIdentity(app);
                loaded = app.loadIcon(packageManager);
                if (loaded != null) loadedState = loaded.getConstantState();
            } catch (RuntimeException ignored) {
                // A broken/removed app icon should not break the drawer or its scrolling.
            }
            final android.graphics.drawable.Drawable fallbackDrawable = loadedState == null ? loaded : null;
            final android.graphics.drawable.Drawable.ConstantState resultState = loadedState;
            activity.runOnUiThread(() -> {
                // Package updates invalidate the current batch. Do not let an old load
                // overwrite a newer app version or remove a newer generation's pending key.
                if (generation != iconCacheGeneration || activity.isFinishing()) return;
                pendingIconLoads.remove(appKey);
                ArrayList<java.lang.ref.WeakReference<ImageView>> targets =
                        iconLoadWaiters.remove(appKey);
                if (resultState != null) iconCache.put(appKey, resultState);
                if (targets == null) return;

                boolean fallbackUsed = false;
                for (java.lang.ref.WeakReference<ImageView> reference : targets) {
                    ImageView target = reference.get();
                    if (target == null || !appKey.equals(target.getTag())) continue;
                    android.graphics.drawable.Drawable ready;
                    if (resultState != null) {
                        ready = resultState.newDrawable(activity.getResources());
                    } else if (!fallbackUsed) {
                        ready = fallbackDrawable;
                        fallbackUsed = true;
                    } else {
                        ready = null;
                    }
                    if (ready != null) bindIconAppearance(target, app, ready, appKey);
                }
            });
        });
    }

    /** Apply current settings when a background icon load completes. */
    private void bindIconAppearance(ImageView icon, ResolveInfo app,
            android.graphics.drawable.Drawable appIcon, String appKey) {
        if (icon == null || appIcon == null) return;
        boolean roundedSquareBackground = preferences != null
                && preferences.getBoolean("icon_rounded_square_background", false);
        int iconShape = LauncherIconDrawable.clampShape(preferences == null
                ? LauncherIconDrawable.SHAPE_ORIGINAL
                : preferences.getInt("app_icon_shape", LauncherIconDrawable.SHAPE_ORIGINAL));
        int backgroundColor = LauncherIconDrawable.backgroundColorFromPreferences(preferences);
        int artworkTheme = LauncherIconDrawable.clampArtworkTheme(preferences == null
                ? LauncherIconDrawable.ARTWORK_ORIGINAL
                : preferences.getInt("icon_artwork_theme", LauncherIconDrawable.ARTWORK_ORIGINAL));
        boolean fitInsideBackground = preferences != null
                && preferences.getBoolean("icon_fit_inside_background", false);
        // Native outlines move circle/rounded-square clipping out of the per-frame Canvas
        // clipPath path. These are the common shapes and are natively clip-capable on API 21+.
        boolean outlineClip = roundedSquareBackground
                && (iconShape == LauncherIconDrawable.SHAPE_SQUARE
                    || iconShape == LauncherIconDrawable.SHAPE_ROUND)
                && android.os.Build.VERSION.SDK_INT >= 21;
        if (outlineClip) {
            ViewOutlineProvider provider = iconShape == LauncherIconDrawable.SHAPE_ROUND
                    ? ROUND_OUTLINE : ROUNDED_SQUARE_OUTLINE;
            if (icon.getOutlineProvider() != provider) {
                icon.setOutlineProvider(provider);
                icon.invalidateOutline();
            }
            if (!icon.getClipToOutline()) {
                icon.setClipToOutline(true);
                icon.invalidateOutline();
            }
        } else if (icon.getClipToOutline()) {
            icon.setClipToOutline(false);
        }

        android.graphics.drawable.Drawable existingIcon = icon.getDrawable();
        LauncherIconDrawable wrapped;
        String artworkToken = artworkSourceToken(appKey, app);
        if (existingIcon instanceof LauncherIconDrawable) {
            wrapped = (LauncherIconDrawable) existingIcon;
            wrapped.updateContent(appIcon, roundedSquareBackground, iconShape,
                    backgroundColor, artworkTheme, artworkToken, fitInsideBackground);
        } else {
            wrapped = new LauncherIconDrawable(appIcon, roundedSquareBackground,
                    iconShape, backgroundColor, artworkTheme, artworkToken, fitInsideBackground);
            icon.setImageDrawable(wrapped);
        }
        wrapped.setExternalShapeClip(outlineClip);
    }

    /**
     * Check cells currently visible in the drawer viewport. The cached opening layer stays
     * visible until their artwork has reached the final selected palette.
     */
    boolean areVisibleIconsReady(android.widget.GridView grid) {
        if (!drawer || grid == null || drawerApps.isEmpty()) return true;
        if (grid.getWidth() <= 0 || grid.getHeight() <= 0) return false;
        int childCount = grid.getChildCount();
        if (childCount <= 0) return false;
        boolean inspected = false;
        for (int childIndex = 0; childIndex < childCount; childIndex++) {
            View cell = grid.getChildAt(childIndex);
            int position = grid.getPositionForView(cell);
            if (position < 0 || position >= drawerApps.size()) continue;
            inspected = true;
            if (!(cell instanceof ViewGroup) || ((ViewGroup) cell).getChildCount() == 0) {
                return false;
            }
            View candidate = ((ViewGroup) cell).getChildAt(0);
            if (!(candidate instanceof ImageView)) return false;
            android.graphics.drawable.Drawable icon = ((ImageView) candidate).getDrawable();
            if (icon == null) return false;
            if (icon instanceof LauncherIconDrawable
                    && !((LauncherIconDrawable) icon).isArtworkReady()) return false;
        }
        return inspected;
    }

    /**
     * Apply the latest icon settings to the currently attached drawer cells without rebuilding
     * the grid or reloading package icons. The drawer view remains alive while hidden, so this
     * also restores settings changed in Home Studio before the next opening.
     */
    void refreshVisibleIconAppearance(android.widget.GridView grid) {
        if (!drawer || grid == null || preferences == null) return;
        boolean showBackground =
                preferences.getBoolean("icon_rounded_square_background", false);
        int iconShape = LauncherIconDrawable.clampShape(
                preferences.getInt("app_icon_shape", LauncherIconDrawable.SHAPE_ORIGINAL));
        int backgroundColor = LauncherIconDrawable.backgroundColorFromPreferences(preferences);
        int artworkTheme = LauncherIconDrawable.clampArtworkTheme(
                preferences.getInt("icon_artwork_theme", LauncherIconDrawable.ARTWORK_ORIGINAL));
        boolean fitInsideBackground =
                preferences.getBoolean("icon_fit_inside_background", false);
        boolean outlineClip = showBackground
                && (iconShape == LauncherIconDrawable.SHAPE_SQUARE
                    || iconShape == LauncherIconDrawable.SHAPE_ROUND)
                && android.os.Build.VERSION.SDK_INT >= 21;

        for (int childIndex = 0; childIndex < grid.getChildCount(); childIndex++) {
            View cell = grid.getChildAt(childIndex);
            int position = grid.getPositionForView(cell);
            if (position < 0 || position >= drawerApps.size()
                    || !(cell instanceof ViewGroup)) continue;

            ImageView icon = null;
            ViewGroup cellGroup = (ViewGroup) cell;
            for (int child = 0; child < cellGroup.getChildCount(); child++) {
                View candidate = cellGroup.getChildAt(child);
                if (candidate instanceof ImageView) {
                    icon = (ImageView) candidate;
                    break;
                }
            }
            if (icon == null) continue;
            android.graphics.drawable.Drawable current = icon.getDrawable();
            if (!(current instanceof LauncherIconDrawable)) continue;

            if (outlineClip) {
                ViewOutlineProvider provider = iconShape == LauncherIconDrawable.SHAPE_ROUND
                        ? ROUND_OUTLINE : ROUNDED_SQUARE_OUTLINE;
                if (icon.getOutlineProvider() != provider) {
                    icon.setOutlineProvider(provider);
                    icon.invalidateOutline();
                }
                if (!icon.getClipToOutline()) {
                    icon.setClipToOutline(true);
                    icon.invalidateOutline();
                }
            } else if (icon.getClipToOutline()) {
                icon.setClipToOutline(false);
            }
            LauncherIconDrawable wrapped = (LauncherIconDrawable) current;
            wrapped.updateAppearance(showBackground, iconShape, backgroundColor,
                    artworkTheme, fitInsideBackground);
            wrapped.setExternalShapeClip(outlineClip);
        }
    }

    void setOnDataChangedListener(Runnable listener) {
        dataChangedListener = listener;
    }

    /**
     * Starts all package-icon loads and selected-theme artwork conversions while the drawer is
     * hidden. The real GridView remains virtualized; only one lightweight waiter view per pending
     * icon is retained, and each waiter is released as soon as its final artwork is ready.
     */
    void prewarmAllDrawerArtwork() {
        if (!drawer || preferences == null || activity.isFinishing()) return;
        // Only artwork mode and installed package identities affect the expensive theme
        // conversion. Clock/widget preferences must not restart this preload on every opening.
        long signature = preferences.getInt("icon_artwork_theme",
                LauncherIconDrawable.ARTWORK_ORIGINAL);
        for (ResolveInfo app : drawerApps) {
            if (app == null || app.activityInfo == null) continue;
            signature = signature * 31L + key(app).hashCode();
            android.content.pm.ApplicationInfo application = app.activityInfo.applicationInfo;
            if (application != null && application.sourceDir != null) {
                try {
                    java.io.File apk = new java.io.File(application.sourceDir);
                    signature = signature * 31L + apk.lastModified();
                    signature = signature * 31L + apk.length();
                } catch (RuntimeException ignored) {}
            }
        }
        if (signature == lastArtworkPrewarmSignature) return;
        lastArtworkPrewarmSignature = signature;
        artworkPrewarmComplete = false;
        artworkPrewarmGeneration++;
        final int generation = artworkPrewarmGeneration;
        artworkPrewarmHandler.removeCallbacksAndMessages(null);
        artworkPrewarmWaiters.clear();

        for (ResolveInfo app : new ArrayList<>(drawerApps)) {
            if (app == null || app.activityInfo == null) continue;
            String appKey = key(app);
            ImageView waiter = new ImageView(activity);
            waiter.setTag(appKey);
            android.graphics.drawable.Drawable icon = cachedIcon(app, waiter, appKey);
            if (icon != null) bindIconAppearance(waiter, app, icon, appKey);
            artworkPrewarmWaiters.add(waiter);
        }
        pollArtworkPrewarm(generation, 0);
    }

    private void pollArtworkPrewarm(int generation, int checks) {
        if (generation != artworkPrewarmGeneration || activity.isFinishing()) return;
        java.util.Iterator<ImageView> iterator = artworkPrewarmWaiters.iterator();
        while (iterator.hasNext()) {
            ImageView waiter = iterator.next();
            String appKey = String.valueOf(waiter.getTag());
            android.graphics.drawable.Drawable icon = waiter.getDrawable();
            boolean pending;
            if (icon instanceof LauncherIconDrawable) {
                // Includes Original Colours: the source draws immediately, but the durable
                // per-app artwork entry must finish reading/generation before prewarm is complete.
                pending = !((LauncherIconDrawable) icon).isArtworkCacheReady();
            } else {
                pending = icon == null && pendingIconLoads.contains(appKey);
            }
            if (!pending) iterator.remove();
        }
        if (artworkPrewarmWaiters.isEmpty()) {
            artworkPrewarmComplete = true;
            return;
        }
        if (checks >= 300) {
            // A malformed third-party icon must not pin view/drawable references indefinitely.
            artworkPrewarmWaiters.clear();
            artworkPrewarmComplete = true;
            return;
        }
        artworkPrewarmHandler.postDelayed(
                () -> pollArtworkPrewarm(generation, checks + 1), 100L);
    }

    boolean isArtworkPrewarmComplete() {
        return artworkPrewarmComplete;
    }

    @Override public void notifyDataSetChanged() {
        if (dataChangedListener != null) dataChangedListener.run();
        super.notifyDataSetChanged();
    }

    /** Called only when PackageManager reports an installed-app change. */
    public void clearPackageCaches() {
        // Keep per-package artwork identities. Re-reading unchanged package metadata after
        // every install/uninstall would add needless PackageManager work during drawer refresh.
        iconCacheGeneration++;
        artworkPrewarmGeneration++;
        artworkPrewarmHandler.removeCallbacksAndMessages(null);
        artworkPrewarmWaiters.clear();
        lastArtworkPrewarmSignature = Long.MIN_VALUE;
        artworkPrewarmComplete = false;
        labelCache.clear();
        iconCache.clear();
        pendingIconLoads.clear();
        iconLoadWaiters.clear();
    }

    @Override public int getCount() { return drawer ? drawerApps.size() : items().size(); }
    @Override public Object getItem(int position) { return items().get(position); }
    @Override public long getItemId(int position) { return position; }

    @Override public View getView(int position, View recycled, ViewGroup parent) {
        List<ResolveInfo> currentItems = items();
        if (position < 0 || position >= currentItems.size()) return recycled != null ? recycled : new LinearLayout(activity);
        return getViewForApp(currentItems.get(position), recycled, parent);
    }

    View getViewForApp(ResolveInfo app, View recycled, ViewGroup parent) {
        if (app == null || app.activityInfo == null) return recycled != null ? recycled : new LinearLayout(activity);
        LinearLayout cell;
        ImageView icon;
        TextView label;
        if (recycled instanceof LinearLayout) {
            cell = (LinearLayout) recycled;
            icon = (ImageView) cell.getChildAt(0);
            label = (TextView) cell.getChildAt(1);
        } else {
            cell = new LinearLayout(activity);
            cell.setOrientation(LinearLayout.VERTICAL);
            if (drawer) {
                // Preserve the drawer's existing icon and label layout.
                cell.setGravity(Gravity.CENTER);
                cell.setPadding(dp(2), dp(7), dp(2), dp(7));
            } else {
                // Home icons stay at the same vertical coordinate with labels on or off.
                // The 14 dp top inset matches the centered icon position in the no-label cell.
                cell.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
                cell.setPadding(dp(2), dp(14), dp(2), dp(7));
            }
            icon = new ImageView(activity);
            icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            int iconSize = preferences == null ? 48 : preferences.getInt(iconSizeKey, 48);
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(iconSize), dp(iconSize));
            iconParams.gravity = Gravity.CENTER;
            cell.addView(icon, iconParams);
            label = makeLabel();
            // The exact home-label height is updated from the selected font's metrics
            // after the TextView is configured below; keep drawer sizing unchanged.
            cell.addView(label, new LinearLayout.LayoutParams(-1, dp(drawer ? 32 : 14)));
        }
        int iconSize = preferences == null ? 48 : preferences.getInt(iconSizeKey, 48);
        ViewGroup.LayoutParams iconParams = icon.getLayoutParams();
        if (iconParams != null && (iconParams.width != dp(iconSize) || iconParams.height != dp(iconSize))) {
            iconParams.width = dp(iconSize);
            iconParams.height = dp(iconSize);
            icon.setLayoutParams(iconParams);
        }
        String appKey = key(app);
        icon.setTag(appKey);
        android.graphics.drawable.Drawable appIcon = cachedIcon(app, icon, appKey);
        if (appIcon != null) {
            // Reuse the current wrapper for recycled cells; only the artwork content changes.
            bindIconAppearance(icon, app, appIcon, appKey);
        } else {
            // A missing drawer icon is loaded in the background. Do not leave the previous
            // recycled app's artwork visible while the new app's resources are being read.
            icon.setImageDrawable(null);
        }
        final String fullLabel = cachedLabel(app);
        if (!fullLabel.equals(label.getTag())) {
            label.setTag(fullLabel);
        }
        // Avoid resetting identical text on every GridView bind. TextView.setText()
        // invalidates and can trigger fresh text measurement during drawer animations,
        // which appears as intermittent label flicker on slower devices.
        if (!fullLabel.contentEquals(label.getText())) {
            label.setText(fullLabel);
        }
        // Font-size preferences are stored as the exact SP value selected by the slider.
        float targetTextSize = Math.max(8f, Math.min(20f,
                preferences == null ? 9f : preferences.getInt(fontSizeKey, 9)));
        if (Math.abs(label.getTextSize() - targetTextSize * activity.getResources().getDisplayMetrics().scaledDensity) > 0.5f) {
            label.setTextSize(targetTextSize);
        }
        // Apply shared system-font and colour choices before measuring each recycled Home/drawer label.
        int labelFontSelection = Math.max(0, Math.min(2,
                preferences == null ? 0 : preferences.getInt("home_label_font_family", 0)));
        String labelTypefaceFamily;
        switch (labelFontSelection) {
            case 1: labelTypefaceFamily = "serif"; break;
            case 2: labelTypefaceFamily = "monospace"; break;
            default: labelTypefaceFamily = "sans-serif";
        }
        Typeface wantedTypeface = Typeface.create(labelTypefaceFamily, Typeface.NORMAL);
        if (label.getTypeface() == null || !wantedTypeface.equals(label.getTypeface())) {
            label.setTypeface(wantedTypeface);
        }
        int labelColorPosition = Math.max(0, Math.min(1000,
                preferences == null ? 0 : preferences.getInt("home_label_color_position", 0)));
        int wantedLabelColor = LauncherIconDrawable.spectrumColorForPosition(labelColorPosition);
        if (label.getCurrentTextColor() != wantedLabelColor) label.setTextColor(wantedLabelColor);
        // Native ellipsizing avoids posting a measurement task for every recycled cell.
        int targetVisibility = preferences == null || preferences.getBoolean(showLabelsKey, true)
                ? View.VISIBLE : View.GONE;
        if (!drawer) {
            // Fit the label itself to its selected font metrics, with only a 1 dp
            // separation from the icon. The home grid slot is enlarged downward to
            // contain this measured height, so text no longer relies on overflow.
            android.graphics.Paint.FontMetrics metrics = label.getPaint().getFontMetrics();
            int textHeightPx = (int) Math.ceil(metrics.descent - metrics.ascent);
            int targetLabelHeight = Math.max(dp(12), textHeightPx + dp(2));
            ViewGroup.LayoutParams labelParams = label.getLayoutParams();
            if (labelParams instanceof LinearLayout.LayoutParams) {
                LinearLayout.LayoutParams linearParams = (LinearLayout.LayoutParams) labelParams;
                boolean changed = linearParams.height != targetLabelHeight
                        || linearParams.topMargin != dp(1);
                linearParams.height = targetLabelHeight;
                linearParams.topMargin = dp(1);
                if (changed) label.setLayoutParams(linearParams);
            }
            label.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
        }
        if (label.getVisibility() != targetVisibility) {
            label.setVisibility(targetVisibility);
        }
        // Keep a safety margin against font rasterization differences on Android 6.
        // The slot now includes the label and its small top margin, rather than relying
        // on this overflow setting to reveal clipped text.
        if (!drawer && targetVisibility == View.VISIBLE) {
            cell.setClipChildren(false);
            cell.setClipToPadding(false);
        }
        cell.setTag(drawer ? "launcher_app_cell" : "launcher_app:" + app.activityInfo.packageName);
        cell.setOnClickListener(v -> { if (actions != null) actions.onAppClick(app, icon); });
        cell.setOnLongClickListener(v -> {
            if (actions != null) actions.onAppLongClick(app, !drawer);
            return true;
        });
        return cell;
    }
}
