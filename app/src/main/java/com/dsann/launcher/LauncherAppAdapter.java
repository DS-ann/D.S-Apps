package com.dsann.launcher;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
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
    private final Map<String, android.graphics.drawable.Drawable.ConstantState> iconCache = new HashMap<>();
    // A recycled GridView cell can be rebound before its icon arrives. Track weak view waiters
    // so the completion updates only cells still representing that app.
    private final Map<String, ArrayList<java.lang.ref.WeakReference<ImageView>>> iconLoadWaiters =
            new HashMap<>();
    private final java.util.Set<String> pendingIconLoads = new java.util.HashSet<>();
    private int iconCacheGeneration;

    // PackageManager icon resolution is I/O/resource-heavy on Android 6. Never do it on the
    // UI thread during the drawer's opening animation or while GridView is scrolling.
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
        android.graphics.drawable.Drawable existingIcon = icon.getDrawable();
        if (existingIcon instanceof LauncherIconDrawable) {
            ((LauncherIconDrawable) existingIcon).updateContent(appIcon, roundedSquareBackground,
                    iconShape, backgroundColor, artworkTheme, appKey, fitInsideBackground);
        } else {
            icon.setImageDrawable(new LauncherIconDrawable(appIcon, roundedSquareBackground,
                    iconShape, backgroundColor, artworkTheme, appKey, fitInsideBackground));
        }
    }

    /** Called only when PackageManager reports an installed-app change. */
    public void clearPackageCaches() {
        iconCacheGeneration++;
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
