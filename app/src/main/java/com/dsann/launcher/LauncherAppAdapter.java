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

    private android.graphics.drawable.Drawable cachedIcon(ResolveInfo app) {
        String key = key(app);
        android.graphics.drawable.Drawable.ConstantState state = iconCache.get(key);
        if (state == null) {
            android.graphics.drawable.Drawable loaded = app.loadIcon(packageManager);
            if (loaded == null) return null;
            state = loaded.getConstantState();
            if (state != null) iconCache.put(key, state);
            return loaded;
        }
        return state.newDrawable(activity.getResources());
    }

    /** Called only when PackageManager reports an installed-app change. */
    public void clearPackageCaches() {
        labelCache.clear();
        iconCache.clear();
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
        android.graphics.drawable.Drawable appIcon = cachedIcon(app);
        if (appIcon != null) {
            boolean roundedSquareBackground = preferences != null
                    && preferences.getBoolean("icon_rounded_square_background", false);
            int iconShape = LauncherIconDrawable.clampShape(preferences == null
                    ? LauncherIconDrawable.SHAPE_ORIGINAL
                    : preferences.getInt("app_icon_shape", LauncherIconDrawable.SHAPE_ORIGINAL));
            int backgroundColor = LauncherIconDrawable.backgroundColorFromPreferences(preferences);
            int artworkTheme = LauncherIconDrawable.clampArtworkTheme(preferences == null
                    ? LauncherIconDrawable.ARTWORK_ORIGINAL
                    : preferences.getInt("icon_artwork_theme", LauncherIconDrawable.ARTWORK_ORIGINAL));
            // GridView repeatedly binds recycled cells while scrolling. Keep the
            // existing drawable's Paint/Path/geometry objects instead of allocating
            // a new custom drawable for each app bind.
            android.graphics.drawable.Drawable existingIcon = icon.getDrawable();
            if (existingIcon instanceof LauncherIconDrawable) {
                ((LauncherIconDrawable) existingIcon).updateContent(appIcon, roundedSquareBackground,
                        iconShape, backgroundColor, artworkTheme, key(app));
            } else {
                icon.setImageDrawable(new LauncherIconDrawable(appIcon, roundedSquareBackground, iconShape,
                        backgroundColor, artworkTheme, key(app)));
            }
        } else {
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
