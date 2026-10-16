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
        void onAppClick(ResolveInfo app);
        void onAppLongClick(ResolveInfo app, boolean onHome);
    }

    private final Activity activity;
    private final SharedPreferences preferences;
    private final PackageManager packageManager;
    private final List<ResolveInfo> allApps;
    private final List<ResolveInfo> drawerApps;
    private final List<String> fixedPackages;
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
        this.iconSizeKey = iconSizeKey;
        this.fontSizeKey = fontSizeKey;
        this.showLabelsKey = showLabelsKey;
        this.actions = actions;
    }

    private List<ResolveInfo> items() {
        if (drawer) return drawerApps;
        List<ResolveInfo> out = new ArrayList<>();
        if (fixedPackages != null) {
            for (String packageName : fixedPackages) {
                for (ResolveInfo app : allApps) {
                    if (app != null && app.activityInfo != null
                            && packageName.equals(app.activityInfo.packageName)) {
                        out.add(app);
                        break;
                    }
                }
            }
        }
        return out;
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

    private String cachedLabel(ResolveInfo app) {
        String key = key(app);
        String value = labelCache.get(key);
        if (value == null) {
            value = String.valueOf(app.loadLabel(packageManager));
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
            cell.setGravity(Gravity.CENTER);
            cell.setPadding(dp(2), dp(7), dp(2), dp(7));
            icon = new ImageView(activity);
            icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            int iconSize = preferences == null ? 96 : preferences.getInt(iconSizeKey, 96);
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(iconSize), dp(iconSize));
            iconParams.gravity = Gravity.CENTER;
            cell.addView(icon, iconParams);
            label = makeLabel();
            cell.addView(label, new LinearLayout.LayoutParams(-1, dp(32)));
        }
        List<ResolveInfo> currentItems = items();
        if (position < 0 || position >= currentItems.size()) return cell;
        ResolveInfo app = currentItems.get(position);
        int iconSize = preferences == null ? 96 : preferences.getInt(iconSizeKey, 96);
        ViewGroup.LayoutParams iconParams = icon.getLayoutParams();
        if (iconParams != null && (iconParams.width != dp(iconSize) || iconParams.height != dp(iconSize))) {
            iconParams.width = dp(iconSize);
            iconParams.height = dp(iconSize);
            icon.setLayoutParams(iconParams);
        }
        icon.setImageDrawable(cachedIcon(app));
        final String fullLabel = cachedLabel(app);
        label.setTag(fullLabel);
        label.setText(fullLabel);
        int fontChoice = preferences == null ? 1 : preferences.getInt(fontSizeKey, 1);
        label.setTextSize(new float[]{9f, 10f, 12f, 14f}[Math.max(0, Math.min(3, fontChoice))]);
        // Native ellipsizing avoids posting a measurement task for every recycled cell.
        label.setVisibility(preferences == null || preferences.getBoolean(showLabelsKey, true)
                ? View.VISIBLE : View.GONE);
        cell.setTag(drawer ? "launcher_app_cell" : "launcher_app:" + app.activityInfo.packageName);
        cell.setOnClickListener(v -> { if (actions != null) actions.onAppClick(app); });
        cell.setOnLongClickListener(v -> {
            if (actions != null) actions.onAppLongClick(app, !drawer);
            return true;
        });
        return cell;
    }
}
