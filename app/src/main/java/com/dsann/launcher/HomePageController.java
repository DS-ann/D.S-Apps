package com.dsann.launcher;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
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
        void syncWidgetViewsToPages();
        void launchApp(ResolveInfo app, View sourceView);
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

    private void clearFocusRecursively(View view) {
        if (view == null) return;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            // Walk backwards in case a custom view changes its children while focus clears.
            for (int i = group.getChildCount() - 1; i >= 0; i--) {
                clearFocusRecursively(group.getChildAt(i));
            }
        }
        if (view.hasFocus()) view.clearFocus();
    }

    void renderHomePages(boolean animate) {
        LinearLayout strip = host.getPagesStrip();
        if (strip == null) return;

        // Match the page count to the persisted model without destroying the page strip.
        // Keeping page containers alive avoids detaching AppWidgetHostViews on every icon
        // move, which is safer for widgets that own focus or active touch state.
        int pageWidth = host.getPageViewport() != null && host.getPageViewport().getWidth() > 0
                ? host.getPageViewport().getWidth()
                : Math.max(1, activity.getResources().getDisplayMetrics().widthPixels
                        - LauncherUiUtils.dp(activity, 40));
        int requiredPages = HomePageModel.PAGE_COUNT;
        while (strip.getChildCount() < requiredPages) {
            FrameLayout newPage = new FrameLayout(activity);
            newPage.setClipChildren(false);
            strip.addView(newPage, new LinearLayout.LayoutParams(pageWidth, -1));
        }
        // Keep the strip and both page containers at the exact measured viewport width.
        // A stale width makes the second screen translate to the wrong offset and appear blank.
        for (int i = 0; i < requiredPages; i++) {
            View child = strip.getChildAt(i);
            if (child instanceof FrameLayout) {
                ViewGroup.LayoutParams pageParams = child.getLayoutParams();
                if (pageParams == null) pageParams = new LinearLayout.LayoutParams(pageWidth, -1);
                pageParams.width = pageWidth;
                pageParams.height = -1;
                child.setLayoutParams(pageParams);
            }
        }
        ViewGroup.LayoutParams stripParams = strip.getLayoutParams();
        if (stripParams != null) {
            stripParams.width = pageWidth * requiredPages;
            stripParams.height = -1;
            strip.setLayoutParams(stripParams);
        }

        // Rebuild shortcut cells only. Existing widget containers remain attached to their
        // page objects and are synchronized to their persisted page indexes separately.
        for (int pageIndex = 0; pageIndex < strip.getChildCount(); pageIndex++) {
            View pageView = strip.getChildAt(pageIndex);
            if (!(pageView instanceof FrameLayout)) continue;
            FrameLayout page = (FrameLayout) pageView;
            for (int childIndex = page.getChildCount() - 1; childIndex >= 0; childIndex--) {
                View child = page.getChildAt(childIndex);
                Object tag = child.getTag();
                if (tag instanceof String && ((String) tag).startsWith("launcher_app:")) {
                    child.animate().cancel();
                    page.removeViewAt(childIndex);
                }
            }
        }

        host.syncWidgetViewsToPages();

        // Empty trailing pages are removed only after widget containers have been moved
        // to the page indicated by the normalized model.
        while (strip.getChildCount() > requiredPages) {
            int lastIndex = strip.getChildCount() - 1;
            View last = strip.getChildAt(lastIndex);
            if (last instanceof ViewGroup) {
                ViewGroup group = (ViewGroup) last;
                for (int i = group.getChildCount() - 1; i >= 0; i--) {
                    View child = group.getChildAt(i);
                    Object tag = child.getTag();
                    if (tag instanceof String && ((String) tag).startsWith("launcher_widget:")) {
                        // Do not discard a widget if stale UI state left it on a trailing page.
                        FrameLayout fallback = (FrameLayout) strip.getChildAt(Math.max(0, requiredPages - 1));
                        ViewParent parent = child.getParent();
                        if (parent instanceof ViewGroup) ((ViewGroup) parent).removeView(child);
                        fallback.addView(child);
                    } else {
                        group.removeViewAt(i);
                    }
                }
            }
            strip.removeViewAt(lastIndex);
        }

        int columns = host.getHomeColumns();
        for (int pageIndex = 0; pageIndex < requiredPages; pageIndex++) {
            FrameLayout page = (FrameLayout) strip.getChildAt(pageIndex);
            page.setClipChildren(false);
            List<String> pagePackages = host.getPages().get(pageIndex);
            int fallbackHeight = Math.max(LauncherUiUtils.dp(activity, 360),
                    activity.getResources().getDisplayMetrics().heightPixels
                            - LauncherUiUtils.dp(activity, 260));
            int pageHeight = host.getPageViewport() != null
                    && host.getPageViewport().getHeight() > LauncherUiUtils.dp(activity, 100)
                    ? host.getPageViewport().getHeight() : fallbackHeight;
            int configuredIconSize = prefs.getInt(iconSizeKey, 96);
            boolean labelsVisible = prefs.getBoolean(showLabelsKey, true);
            int cellHeight = LauncherUiUtils.dp(activity,
                    Math.max(56, configuredIconSize + (labelsVisible ? 46 : 28)));
            // Each app owns one fixed-height grid slot. Compute the number of rows
            // from the usable workspace height so no slot extends into the dock.
            final int rows = Math.max(1, pageHeight / Math.max(1, cellHeight));
            boolean[][] occupiedSlots = new boolean[rows][columns];
            LauncherAppAdapter homeAdapter = new LauncherAppAdapter(activity, prefs, packageManager,
                    host.getAllApps(), host.getDrawerApps(), pagePackages,
                    false, iconSizeKey, fontSizeKey, showLabelsKey,
                    new LauncherAppAdapter.Actions() {
                        @Override public void onAppClick(ResolveInfo app, View sourceView) { host.launchApp(app, sourceView); }
                        @Override public void onAppLongClick(ResolveInfo app, boolean onHome) {
                            host.showAppOptions(app, onHome);
                        }
                    });
            for (int itemIndex = 0; itemIndex < pagePackages.size(); itemIndex++) {
                String packageName = pagePackages.get(itemIndex);
                ResolveInfo app = host.findApp(packageName);
                if (app == null) continue;
                View cell = homeAdapter.getViewForApp(app, null, page);
                cell.setTag("launcher_app:" + packageName);
                int cellWidth = Math.max(LauncherUiUtils.dp(activity, 1), pageWidth / columns);
                FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                        cellWidth, cellHeight, Gravity.TOP | Gravity.LEFT);
                int defaultCol = itemIndex % columns;
                int defaultRow = (itemIndex / columns) % rows;
                float savedX = prefs.getFloat(iconXKey + packageName,
                        defaultCol / (float) Math.max(1, columns - 1));
                float savedY = prefs.getFloat(iconYKey + packageName,
                        defaultRow / (float) Math.max(1, rows - 1));
                int col = Math.max(0, Math.min(columns - 1,
                        Math.round(savedX * Math.max(1, columns - 1))));
                int row = Math.max(0, Math.min(rows - 1,
                        Math.round(savedY * Math.max(1, rows - 1))));
                if (occupiedSlots[row][col]) {
                    int bestCol = -1, bestRow = -1, bestDistance = Integer.MAX_VALUE;
                    for (int candidateRow = 0; candidateRow < rows; candidateRow++) {
                        for (int candidateCol = 0; candidateCol < columns; candidateCol++) {
                            if (occupiedSlots[candidateRow][candidateCol]) continue;
                            int distance = Math.abs(candidateCol - col) + Math.abs(candidateRow - row);
                            if (distance < bestDistance) {
                                bestDistance = distance; bestCol = candidateCol; bestRow = candidateRow;
                            }
                        }
                    }
                    if (bestCol < 0) continue; // Never overlap another app or extend below the dock.
                    col = bestCol; row = bestRow;
                    savedX = col / (float) Math.max(1, columns - 1);
                    savedY = row / (float) Math.max(1, rows - 1);
                    prefs.edit().putFloat(iconXKey + packageName, savedX)
                            .putFloat(iconYKey + packageName, savedY).apply();
                }
                occupiedSlots[row][col] = true;
                lp.leftMargin = col * cellWidth;
                // Spread rows across the measured workspace so the bottom row reaches
                // the bottom of the usable page instead of leaving a large unused gap.
                int maxTop = Math.max(0, pageHeight - cellHeight);
                lp.topMargin = rows <= 1 ? 0
                        : Math.round(row * (maxTop / (float) (rows - 1)));
                page.addView(cell, lp);
                host.installHomeIconTouch(cell, packageName);
            }
            page.post(() -> {
                if (page.getWidth() <= 0 || page.getHeight() <= 0) return;
                int actualCellWidth = Math.max(1, page.getWidth() / columns);
                boolean actualLabelsVisible = prefs.getBoolean(showLabelsKey, true);
                int actualCellHeight = LauncherUiUtils.dp(activity,
                        Math.max(56, configuredIconSize + (actualLabelsVisible ? 46 : 28)));
                int actualRows = Math.max(1, page.getHeight() / actualCellHeight);
                for (int childIndex = 0; childIndex < page.getChildCount(); childIndex++) {
                    View child = page.getChildAt(childIndex);
                    Object childTag = child.getTag();
                    if (!(childTag instanceof String)
                            || !((String) childTag).startsWith("launcher_app:")) continue;
                    String childPackage = ((String) childTag).substring("launcher_app:".length());
                    int orderIndex = pagePackages.indexOf(childPackage);
                    if (orderIndex < 0) continue;
                    int defaultCol = orderIndex % columns;
                    int defaultRow = (orderIndex / columns) % rows;
                    float savedX = prefs.getFloat(iconXKey + childPackage,
                            defaultCol / (float) Math.max(1, columns - 1));
                    float savedY = prefs.getFloat(iconYKey + childPackage,
                            defaultRow / (float) Math.max(1, actualRows - 1));
                    int col = Math.max(0, Math.min(columns - 1,
                            Math.round(savedX * Math.max(1, columns - 1))));
                    int row = Math.max(0, Math.min(actualRows - 1,
                            Math.round(savedY * Math.max(1, actualRows - 1))));
                    FrameLayout.LayoutParams actual = (FrameLayout.LayoutParams) child.getLayoutParams();
                    actual.width = actualCellWidth;
                    actual.height = actualCellHeight;
                    actual.leftMargin = col * actualCellWidth;
                    int maxActualTop = Math.max(0, page.getHeight() - actualCellHeight);
                    actual.topMargin = actualRows <= 1 ? 0
                            : Math.round(row * (maxActualTop / (float) (actualRows - 1)));
                    child.setLayoutParams(actual);
                }
            });
        }

        host.restoreWidgetsOnPages();
        if (host.getPageIndicator() != null) {
            boolean showIndicator = requiredPages > 1
                    && prefs.getBoolean(showPageIndicatorKey, true);
            host.getPageIndicator().setVisibility(showIndicator ? View.VISIBLE : View.GONE);
            host.getPageIndicator().setText(
                    (host.getCurrentPage() + 1) + "  /  " + requiredPages);
        }
        strip.animate().cancel();
        strip.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        float targetX = -host.getCurrentPage() * pageWidth;
        if (animate) {
            strip.setCameraDistance(12000f * activity.getResources().getDisplayMetrics().density);
            strip.setRotationY(host.getCurrentPage() > 0 ? 3f : -3f);
            strip.setScaleX(1f);
            strip.animate().translationX(targetX).rotationY(0f).setDuration(LauncherMotion.duration(activity, 220L))
                    .withEndAction(() -> strip.setLayerType(View.LAYER_TYPE_NONE, null)).start();
        } else {
            strip.setTranslationX(targetX);
            strip.setRotationY(0f);
            strip.setScaleX(1f);
            strip.setLayerType(View.LAYER_TYPE_NONE, null);
        }
    }
}
