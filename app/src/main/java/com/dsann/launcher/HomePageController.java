package com.dsann.launcher;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

/** Renders home pages and icon cells; gesture behavior remains owned by the activity. */
final class HomePageController {
    interface Host {
        LinearLayout getPagesStrip();
        FrameLayout getPageViewport();
        TextView getPageIndicator();
        List<List<String>> getPages();
        List<ResolveInfo> getAllApps();
        List<ResolveInfo> getDrawerApps();
        int getCurrentPage();
        void rebuildPages();
        int getHomeColumns();
        ResolveInfo findApp(String packageName);
        void installHomeIconTouch(View cell, String packageName);
        void restoreWidgetsOnPages();
        void launchApp(ResolveInfo app);
        void showAppOptions(ResolveInfo app, boolean onHome);
    }

    private final Activity activity;
    private final SharedPreferences prefs;
    private final PackageManager packageManager;
    private final Host host;
    private final String iconXKey;
    private final String iconYKey;
    private final String showLabelsKey;
    private final String iconSizeKey;
    private final String fontSizeKey;
    private final String showPageIndicatorKey;
    private final int homeRows;

    HomePageController(Activity activity, SharedPreferences prefs, PackageManager packageManager,
                       Host host, String iconXKey, String iconYKey, String showLabelsKey, String iconSizeKey, String fontSizeKey, String showPageIndicatorKey, int homeRows) {
        this.activity = activity;
        this.prefs = prefs;
        this.packageManager = packageManager;
        this.host = host;
        this.iconXKey = iconXKey;
        this.iconYKey = iconYKey;
        this.showLabelsKey = showLabelsKey;
        this.iconSizeKey = iconSizeKey;
        this.fontSizeKey = fontSizeKey;
        this.showPageIndicatorKey = showPageIndicatorKey;
        this.homeRows = homeRows;
    }

    void renderHomePages(boolean animate) {
        if (host.getPagesStrip() == null) return;
        host.getPagesStrip().removeAllViews();
        host.rebuildPages();
        int pageWidth = Math.max(1, activity.getResources().getDisplayMetrics().widthPixels - LauncherUiUtils.dp(activity, 40));
        int columns = host.getHomeColumns();
        int rows = homeRows;
        for (int pageIndex = 0; pageIndex < host.getPages().size(); pageIndex++) {
            FrameLayout page = new FrameLayout(activity);
            page.setClipChildren(false);
            List<String> pagePackages = host.getPages().get(pageIndex);
            for (int itemIndex = 0; itemIndex < pagePackages.size(); itemIndex++) {
                String packageName = pagePackages.get(itemIndex);
                if (host.findApp(packageName) == null) continue;
                View cell = new LauncherAppAdapter(activity, prefs, packageManager, host.getAllApps(), host.getDrawerApps(), pagePackages,
                        false, iconSizeKey, fontSizeKey, showLabelsKey,
                        new LauncherAppAdapter.Actions() {
                            @Override public void onAppClick(ResolveInfo app) { host.launchApp(app); }
                            @Override public void onAppLongClick(ResolveInfo app, boolean onHome) { host.showAppOptions(app, onHome); }
                        }).getView(itemIndex, null, page);
                cell.setTag("launcher_app:" + packageName);
                int fallbackHeight = Math.max(LauncherUiUtils.dp(activity, 360), activity.getResources().getDisplayMetrics().heightPixels - LauncherUiUtils.dp(activity, 260));
                int pageHeight = host.getPageViewport().getHeight() > LauncherUiUtils.dp(activity, 100) ? host.getPageViewport().getHeight() : fallbackHeight;
                int cellWidth = Math.max(LauncherUiUtils.dp(activity, 1), pageWidth / columns);
                int cellHeight = Math.max(LauncherUiUtils.dp(activity, 1), pageHeight / rows);
                FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(cellWidth, cellHeight, Gravity.TOP | Gravity.LEFT);
                int defaultCol = itemIndex % columns;
                int defaultRow = (itemIndex / columns) % rows;
                float savedX = prefs.getFloat(iconXKey + packageName, defaultCol / (float) Math.max(1, columns - 1));
                float savedY = prefs.getFloat(iconYKey + packageName, defaultRow / (float) Math.max(1, rows - 1));
                int col = Math.max(0, Math.min(columns - 1, Math.round(savedX * Math.max(1, columns - 1))));
                int row = Math.max(0, Math.min(rows - 1, Math.round(savedY * Math.max(1, rows - 1))));
                lp.leftMargin = col * cellWidth;
                lp.topMargin = row * cellHeight;
                page.addView(cell, lp);
                host.installHomeIconTouch(cell, packageName);
            }
            host.getPagesStrip().addView(page, new LinearLayout.LayoutParams(pageWidth, -1));
            page.post(() -> {
                if (page.getWidth() <= 0 || page.getHeight() <= 0) return;
                int actualCellWidth = Math.max(1, page.getWidth() / columns);
                int actualCellHeight = Math.max(1, page.getHeight() / rows);
                for (int childIndex = 0; childIndex < page.getChildCount(); childIndex++) {
                    View child = page.getChildAt(childIndex);
                    Object childTag = child.getTag();
                    if (!(childTag instanceof String) || !((String) childTag).startsWith("launcher_app:")) continue;
                    String childPackage = ((String) childTag).substring("launcher_app:".length());
                    int orderIndex = pagePackages.indexOf(childPackage);
                    if (orderIndex < 0) continue;
                    int defaultCol = orderIndex % columns;
                    int defaultRow = (orderIndex / columns) % rows;
                    float savedX = prefs.getFloat(iconXKey + childPackage, defaultCol / (float) Math.max(1, columns - 1));
                    float savedY = prefs.getFloat(iconYKey + childPackage, defaultRow / (float) Math.max(1, rows - 1));
                    int col = Math.max(0, Math.min(columns - 1, Math.round(savedX * Math.max(1, columns - 1))));
                    int row = Math.max(0, Math.min(rows - 1, Math.round(savedY * Math.max(1, rows - 1))));
                    FrameLayout.LayoutParams actual = (FrameLayout.LayoutParams) child.getLayoutParams();
                    actual.width = actualCellWidth;
                    actual.height = actualCellHeight;
                    actual.leftMargin = col * actualCellWidth;
                    actual.topMargin = row * actualCellHeight;
                    child.setLayoutParams(actual);
                }
            });
        }
        host.restoreWidgetsOnPages();
        host.getPageIndicator().setVisibility(prefs.getBoolean(showPageIndicatorKey, true) ? View.VISIBLE : View.GONE);
        host.getPageIndicator().setText((host.getCurrentPage() + 1) + "  /  " + host.getPages().size());
        host.getPagesStrip().animate().cancel();
        host.getPagesStrip().setLayerType(View.LAYER_TYPE_HARDWARE, null);
        float targetX = -host.getCurrentPage() * pageWidth;
        if (animate) {
            // Lightweight 3D page-turn hint: small rotation, no scaling or heavy overshoot.
            // Keep the strip on a hardware layer only for the short transition.
            host.getPagesStrip().setCameraDistance(12000f * activity.getResources().getDisplayMetrics().density);
            host.getPagesStrip().setRotationY(host.getCurrentPage() > 0 ? 3f : -3f);
            host.getPagesStrip().setScaleX(1f);
            host.getPagesStrip().animate()
                    .translationX(targetX)
                    .rotationY(0f)
                    .setDuration(220L)
                    .withEndAction(() -> host.getPagesStrip().setLayerType(View.LAYER_TYPE_NONE, null))
                    .start();
        } else {
            host.getPagesStrip().setTranslationX(targetX);
            host.getPagesStrip().setRotationY(0f);
            host.getPagesStrip().setScaleX(1f);
            host.getPagesStrip().setLayerType(View.LAYER_TYPE_NONE, null);
        }
    
    }
}
