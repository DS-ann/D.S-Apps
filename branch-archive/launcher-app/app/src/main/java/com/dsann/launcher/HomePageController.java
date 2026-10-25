package com.dsann.launcher;

import android.app.Activity;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.content.res.Configuration;
import android.graphics.Rect;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

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
        int getHomeCellHeightDp(int iconSize);
        int getHomeGridRowsForHeight(int pageHeightPx, int cellHeightPx);
        int getHomeGridRowTop(int row, int rowCount, int pageHeightPx, int cellHeightPx);
        ResolveInfo findApp(String packageName);
        void installHomeIconTouch(View cell, String packageName);
        void restoreWidgetsOnPages();
        void syncWidgetViewsToPages();
        void launchApp(ResolveInfo app, View sourceView);
        void showAppOptions(ResolveInfo app, boolean onHome, View sourceView);
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
    private final java.util.WeakHashMap<View, Boolean> workspaceItemAttachWatchers =
            new java.util.WeakHashMap<View, Boolean>();
    private final java.util.WeakHashMap<View, Boolean> orientationGeometryWatchers =
            new java.util.WeakHashMap<View, Boolean>();
    private final java.util.WeakHashMap<View, float[]> landscapeClockBaseScales =
            new java.util.WeakHashMap<View, float[]>();
    private static final String PORTRAIT_GEOMETRY_PREFIX = "__portrait_workspace_geometry__";

    private static final String PORTRAIT_APP_ROW_WIDTH_DP_KEY =
            "__portrait_home_app_row_width_dp";

    /**
     * Keeps the full app grid row at its portrait physical width in landscape.
     * The preference stores the total row span in dp, not a percentage of the
     * current orientation's page width.
     */
    private int homeAppCellWidthPx(FrameLayout page, LinearLayout strip,
                                   int columns, int fallbackPageWidth) {
        int safeColumns = Math.max(1, columns);
        android.util.DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
        float density = metrics.density > 0f ? metrics.density : 1f;
        int minimumCellWidth = LauncherUiUtils.dp(activity, 1);

        if (!isLandscapeOrientation(strip)) {
            int width = page != null && page.getWidth() > 0
                    ? page.getWidth() : fallbackPageWidth;
            return Math.max(minimumCellWidth, width / safeColumns);
        }

        float portraitRowWidthDp = prefs.getFloat(PORTRAIT_APP_ROW_WIDTH_DP_KEY, -1f);
        if (portraitRowWidthDp > 0f) {
            return Math.max(minimumCellWidth,
                    Math.round(portraitRowWidthDp * density / safeColumns));
        }

        // Cold start directly in landscape: use the display's short side as a
        // portrait-width fallback until a real portrait measurement is available.
        int shortSidePx = Math.min(metrics.widthPixels, metrics.heightPixels);
        return Math.max(minimumCellWidth, shortSidePx / safeColumns);
    }

    private void savePortraitHomeAppRowWidthDp(int cellWidthPx, int columns) {
        if (cellWidthPx <= 0 || columns <= 0) return;
        float density = activity.getResources().getDisplayMetrics().density;
        if (density <= 0f) density = 1f;
        float rowWidthDp = (cellWidthPx * (float) columns) / density;
        prefs.edit().putFloat(PORTRAIT_APP_ROW_WIDTH_DP_KEY, rowWidthDp).apply();
    }

    private static final class WorkspaceGeometry {
        final int page, left, top, width, height;
        final float scaleX, scaleY, translationX, translationY, rotationX, rotationY;
        WorkspaceGeometry(int page, int left, int top, int width, int height,
                          float scaleX, float scaleY, float translationX, float translationY,
                          float rotationX, float rotationY) {
            this.page = page; this.left = left; this.top = top; this.width = width; this.height = height;
            this.scaleX = scaleX; this.scaleY = scaleY; this.translationX = translationX;
            this.translationY = translationY; this.rotationX = rotationX; this.rotationY = rotationY;
        }
    }

    private boolean isLandscapeOrientation(LinearLayout strip) {
        int orientation = activity.getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) return true;
        if (orientation == Configuration.ORIENTATION_PORTRAIT) return false;
        if (strip != null && strip.getChildCount() > 0) {
            View page = strip.getChildAt(0);
            return page.getWidth() > 0 && page.getWidth() > page.getHeight();
        }
        return false;
    }

    private boolean isPortraitOrientation() {
        return !isLandscapeOrientation(host.getPagesStrip());
    }

    private boolean isOrientationManagedItem(View item) {
        if (item == null || !(item.getTag() instanceof String)) return false;
        String tag = (String) item.getTag();
        return tag.startsWith("launcher_widget:")
                || tag.contains("shortcut");
    }

    private String portraitGeometryKey(View item) {
        return item == null || !(item.getTag() instanceof String)
                ? null : PORTRAIT_GEOMETRY_PREFIX + item.getTag();
    }

    private int pageIndexOf(View item, LinearLayout strip) {
        if (item == null || strip == null || !(item.getParent() instanceof FrameLayout)) return -1;
        return strip.indexOfChild((View) item.getParent());
    }

    private WorkspaceGeometry readPortraitGeometry(View item) {
        String key = portraitGeometryKey(item);
        if (key == null) return null;
        String value = prefs.getString(key, null);
        if (value == null) return null;
        String[] parts = value.split(",");
        if (parts.length < 11) return null;
        try {
            return new WorkspaceGeometry(
                    Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]),
                    Integer.parseInt(parts[3]), Integer.parseInt(parts[4]),
                    Float.parseFloat(parts[5]), Float.parseFloat(parts[6]),
                    Float.parseFloat(parts[7]), Float.parseFloat(parts[8]),
                    Float.parseFloat(parts[9]), Float.parseFloat(parts[10]));
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private void savePortraitGeometry(View item, LinearLayout strip) {
        if (!isPortraitOrientation() || !isOrientationManagedItem(item)
                || !(item.getLayoutParams() instanceof FrameLayout.LayoutParams)) return;
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) item.getLayoutParams();
        int width = lp.width > 0 ? lp.width : item.getWidth();
        int height = lp.height > 0 ? lp.height : item.getHeight();
        int page = pageIndexOf(item, strip);
        if (page < 0 || width <= 0 || height <= 0) return;
        String value = page + "," + lp.leftMargin + "," + lp.topMargin + ","
                + width + "," + height + "," + item.getScaleX() + "," + item.getScaleY() + ","
                + item.getTranslationX() + "," + item.getTranslationY() + ","
                + item.getRotationX() + "," + item.getRotationY();
        prefs.edit().putString(portraitGeometryKey(item), value).apply();
    }

    private void watchOrientationGeometry(View item, LinearLayout strip) {
        if (!isOrientationManagedItem(item) || orientationGeometryWatchers.containsKey(item)) return;
        item.addOnLayoutChangeListener((view, left, top, right, bottom,
                oldLeft, oldTop, oldRight, oldBottom) -> savePortraitGeometry(view, strip));
        item.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override public void onViewAttachedToWindow(View view) {
                savePortraitGeometry(view, strip);
            }
            @Override public void onViewDetachedFromWindow(View view) { }
        });
        orientationGeometryWatchers.put(item, Boolean.TRUE);
    }

    private ArrayList<View> collectManagedItems(LinearLayout strip) {
        ArrayList<View> items = new ArrayList<>();
        if (strip == null) return items;
        for (int pageIndex = 0; pageIndex < strip.getChildCount(); pageIndex++) {
            View pageView = strip.getChildAt(pageIndex);
            if (!(pageView instanceof FrameLayout)) continue;
            FrameLayout page = (FrameLayout) pageView;
            for (int childIndex = 0; childIndex < page.getChildCount(); childIndex++) {
                View child = page.getChildAt(childIndex);
                if (isOrientationManagedItem(child)) items.add(child);
            }
        }
        return items;
    }

    private FrameLayout.LayoutParams copyWorkspaceParams(View item, int width, int height,
                                                          int left, int top) {
        ViewGroup.LayoutParams raw = item.getLayoutParams();
        FrameLayout.LayoutParams lp = raw instanceof FrameLayout.LayoutParams
                ? new FrameLayout.LayoutParams((FrameLayout.LayoutParams) raw)
                : new FrameLayout.LayoutParams(width, height, Gravity.TOP | Gravity.LEFT);
        lp.width = width;
        lp.height = height;
        lp.leftMargin = left;
        lp.topMargin = top;
        return lp;
    }

    private void syncWidgetGeometryPreference(View item, int pageIndex, FrameLayout.LayoutParams lp) {
        if (!isPortraitOrientation() || !(item.getTag() instanceof String)) return;
        String tag = (String) item.getTag();
        if (!tag.startsWith("launcher_widget:")) return;
        String idText = tag.substring("launcher_widget:".length());
        int widgetId;
        try { widgetId = Integer.parseInt(idText); }
        catch (NumberFormatException ignored) { return; }

        SharedPreferences.Editor editor = prefs.edit()
                .putInt("widget_x_" + widgetId, lp.leftMargin)
                .putInt("widget_y_" + widgetId, lp.topMargin)
                .putInt("widget_w_" + widgetId, lp.width)
                .putInt("widget_h_" + widgetId, lp.height);
        for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
            if (!entry.getKey().toLowerCase(Locale.US).contains("widget")
                    || !(entry.getValue() instanceof String)) continue;
            String stored = (String) entry.getValue();
            String[] lines = stored.split("\\n");
            boolean changed = false;
            StringBuilder updated = new StringBuilder();
            for (String line : lines) {
                String[] parts = line.split(":");
                if (parts.length == 2 && parts[0].trim().equals(String.valueOf(widgetId))) {
                    line = widgetId + ":" + pageIndex;
                    changed = true;
                }
                if (updated.length() > 0) updated.append('\n');
                updated.append(line);
            }
            if (changed) editor.putString(entry.getKey(), updated.toString());
        }
        editor.apply();
        if (item instanceof ViewGroup && ((ViewGroup) item).getChildCount() > 0
                && ((ViewGroup) item).getChildAt(0) instanceof AppWidgetHostView
                && lp.width > 0 && lp.height > 0) {
            WidgetController.updateWidgetSize(AppWidgetManager.getInstance(activity), widgetId,
                    (AppWidgetHostView) ((ViewGroup) item).getChildAt(0),
                    activity.getResources().getDisplayMetrics().density, lp.width, lp.height);
        }
    }

    private void restoreSavedWorkspaceGeometry(LinearLayout strip) {
        if (strip == null) return;
        ArrayList<View> items = collectManagedItems(strip);
        boolean landscape = isLandscapeOrientation(strip);
        for (View item : items) {
            // Keep the clock on its current page while in landscape. Its portrait page
            // is restored only after returning to portrait, never during rotation itself.
            if (landscape && isClockWorkspaceItem(item)) {
                watchOrientationGeometry(item, strip);
                continue;
            }
            WorkspaceGeometry geometry = readPortraitGeometry(item);
            if (geometry == null) {
                watchOrientationGeometry(item, strip);
                continue;
            }
            int targetPageIndex = Math.max(0, Math.min(strip.getChildCount() - 1, geometry.page));
            if (targetPageIndex < 0 || !(strip.getChildAt(targetPageIndex) instanceof FrameLayout)) continue;
            FrameLayout targetPage = (FrameLayout) strip.getChildAt(targetPageIndex);
            ViewParent parent = item.getParent();
            FrameLayout.LayoutParams lp = copyWorkspaceParams(item, geometry.width, geometry.height,
                    geometry.left, geometry.top);
            if (parent != targetPage) {
                if (parent instanceof ViewGroup) ((ViewGroup) parent).removeView(item);
                targetPage.addView(item, lp);
            } else {
                item.setLayoutParams(lp);
            }
            item.setScaleX(geometry.scaleX);
            item.setScaleY(geometry.scaleY);
            item.setTranslationX(geometry.translationX);
            item.setTranslationY(geometry.translationY);
            item.setRotationX(geometry.rotationX);
            item.setRotationY(geometry.rotationY);
            watchOrientationGeometry(item, strip);
            syncWidgetGeometryPreference(item, targetPageIndex, lp);
        }
    }

    private void snapshotPortraitWorkspaceGeometry(LinearLayout strip) {
        if (strip == null || !isPortraitOrientation()) return;
        for (View item : collectManagedItems(strip)) {
            watchOrientationGeometry(item, strip);
            savePortraitGeometry(item, strip);
        }
    }

    private Rect workspaceBounds(View item, int left, int top, int width, int height) {
        float sx = Math.max(0.01f, item.getScaleX());
        float sy = Math.max(0.01f, item.getScaleY());
        int visualLeft = Math.round(left + item.getTranslationX() + item.getPivotX() * (1f - sx));
        int visualTop = Math.round(top + item.getTranslationY() + item.getPivotY() * (1f - sy));
        int visualWidth = Math.max(1, Math.round(Math.max(1, width) * sx));
        int visualHeight = Math.max(1, Math.round(Math.max(1, height) * sy));
        return new Rect(visualLeft, visualTop, visualLeft + visualWidth, visualTop + visualHeight);
    }

    private Rect visibleAppBounds(View item) {
        if (!(item instanceof ViewGroup) || !(item.getTag() instanceof String)
                || !((String) item.getTag()).startsWith("launcher_app:")) return null;
        ViewGroup cell = (ViewGroup) item;
        Rect result = null;
        int rootLeft = item.getLeft();
        int rootTop = item.getTop();
        int count = Math.min(2, cell.getChildCount());
        for (int i = 0; i < count; i++) {
            View child = cell.getChildAt(i);
            if (child == null || child.getVisibility() != View.VISIBLE) continue;
            int childWidth = child.getWidth();
            int childHeight = child.getHeight();
            if (childWidth <= 0 || childHeight <= 0) continue;
            int left = rootLeft + child.getLeft();
            int top = rootTop + child.getTop();
            int width = childWidth;
            int height = childHeight;
            if (i == 1 && child instanceof TextView) {
                TextView label = (TextView) child;
                CharSequence text = label.getText();
                if (text == null || text.length() == 0) continue;
                android.graphics.Paint paint = label.getPaint();
                android.graphics.Paint.FontMetrics metrics = paint.getFontMetrics();
                int paintedWidth = Math.min(Math.max(0, childWidth
                        - label.getCompoundPaddingLeft() - label.getCompoundPaddingRight()),
                        Math.round(paint.measureText(text.toString())));
                width = Math.max(1, paintedWidth);
                height = Math.max(1, Math.round(metrics.descent - metrics.ascent));
                left += label.getCompoundPaddingLeft()
                        + Math.max(0, (childWidth - label.getCompoundPaddingLeft()
                                - label.getCompoundPaddingRight() - width) / 2);
                top += label.getCompoundPaddingTop()
                        + Math.max(0, (childHeight - label.getCompoundPaddingTop()
                                - label.getCompoundPaddingBottom() - height) / 2);
            }
            float childScaleX = Math.max(0.01f, Math.abs(child.getScaleX()));
            float childScaleY = Math.max(0.01f, Math.abs(child.getScaleY()));
            left = Math.round(left + child.getPivotX() * (1f - childScaleX));
            top = Math.round(top + child.getPivotY() * (1f - childScaleY));
            width = Math.max(1, Math.round(width * childScaleX));
            height = Math.max(1, Math.round(height * childScaleY));
            Rect part = new Rect(left, top, left + width, top + height);
            if (result == null) result = part;
            else result.union(part);
        }
        return result;
    }

    private ArrayList<Integer> orderedPositions(int max, int preferred, int step) {
        TreeSet<Integer> positions = new TreeSet<>();
        positions.add(0);
        positions.add(Math.max(0, max));
        positions.add(Math.max(0, Math.min(max, preferred)));
        for (int value = 0; value <= max; value += Math.max(1, step)) positions.add(value);
        ArrayList<Integer> ordered = new ArrayList<>(positions);
        final int reference = Math.max(0, Math.min(max, preferred));
        Collections.sort(ordered, (a, b) -> {
            int distance = Integer.compare(Math.abs(a - reference), Math.abs(b - reference));
            return distance != 0 ? distance : Integer.compare(a, b);
        });
        return ordered;
    }

    private int[] findFreePosition(View item, int preferredLeft, int preferredTop, int width, int height,
                                   int pageWidth, int pageHeight, ArrayList<Rect> occupied, int step) {
        if (width <= 0 || height <= 0 || width > pageWidth || height > pageHeight) return null;
        int maxLeft = pageWidth - width;
        int maxTop = pageHeight - height;
        ArrayList<Integer> topPositions = orderedPositions(maxTop, preferredTop, step);
        ArrayList<Integer> leftPositions = orderedPositions(maxLeft, preferredLeft, step);
        for (int top : topPositions) {
            for (int left : leftPositions) {
                Rect candidate = workspaceBounds(item, left, top, width, height);
                boolean intersects = false;
                for (Rect used : occupied) {
                    if (Rect.intersects(candidate, used)) { intersects = true; break; }
                }
                if (!intersects) return new int[]{left, top};
            }
        }
        return null;
    }

    private boolean isClockWorkspaceItem(View item) {
        return item != null && "launcher_clock_widget".equals(item.getTag());
    }

    private boolean isAppGroupWorkspaceItem(View item) {
        return item != null && item.getTag() instanceof String
                && ((String) item.getTag()).startsWith("launcher_app_group:");
    }

    private boolean isResizableLandscapeItem(View item) {
        if (!(item.getTag() instanceof String)) return false;
        String tag = (String) item.getTag();
        return tag.startsWith("launcher_widget:") || tag.contains("clock_widget");
    }

    private boolean overlapsAny(Rect candidate, ArrayList<Rect> occupied) {
        for (Rect used : occupied) {
            if (Rect.intersects(candidate, used)) return true;
        }
        return false;
    }

    /**
     * Landscape rotation reduces the clock's actual rendered scale, not just its
     * container dimensions. The original layout rectangle and position are retained.
     */
    private void fitClockAtFixedPosition(View clock, FrameLayout page,
            ArrayList<Rect> occupied) {
        if (!(clock.getLayoutParams() instanceof FrameLayout.LayoutParams)) return;
        FrameLayout.LayoutParams current = (FrameLayout.LayoutParams) clock.getLayoutParams();
        final int left = current.leftMargin;
        final int top = current.topMargin;
        int width = current.width > 0 ? current.width : clock.getWidth();
        int height = current.height > 0 ? current.height : clock.getHeight();
        if (width <= 0 || height <= 0) return;

        float[] baseScale = landscapeClockBaseScales.get(clock);
        if (baseScale == null) {
            baseScale = new float[]{Math.max(0.01f, Math.abs(clock.getScaleX())),
                    Math.max(0.01f, Math.abs(clock.getScaleY()))};
            landscapeClockBaseScales.put(clock, baseScale);
        }

        int pageWidth = page.getWidth();
        int pageHeight = page.getHeight();
        float factor = 1f;
        Rect candidate;
        while (true) {
            clock.setScaleX(baseScale[0] * factor);
            clock.setScaleY(baseScale[1] * factor);
            candidate = workspaceBounds(clock, left, top, width, height);
            boolean insidePage = candidate.left >= 0 && candidate.top >= 0
                    && candidate.right <= pageWidth && candidate.bottom <= pageHeight;
            if (insidePage && !overlapsAny(candidate, occupied)) break;
            if (factor <= 0.025f) break;
            factor = Math.max(0.025f, factor * 0.8f);
        }

        // Do not modify layout params or translations: the clock only shrinks.
        occupied.add(candidate);
        clock.requestLayout();
        clock.invalidate();
    }

    private void restorePortraitClockScales(LinearLayout strip) {
        if (strip == null) return;
        for (int pageIndex = 0; pageIndex < strip.getChildCount(); pageIndex++) {
            View pageView = strip.getChildAt(pageIndex);
            if (!(pageView instanceof FrameLayout)) continue;
            FrameLayout page = (FrameLayout) pageView;
            for (int childIndex = 0; childIndex < page.getChildCount(); childIndex++) {
                View child = page.getChildAt(childIndex);
                if (!isClockWorkspaceItem(child)) continue;
                float[] baseScale = landscapeClockBaseScales.remove(child);
                if (baseScale == null) continue;
                child.setScaleX(baseScale[0]);
                child.setScaleY(baseScale[1]);
                child.requestLayout();
                child.invalidate();
            }
        }
    }

    private void resizeAppWidgetForLandscape(View item, int width, int height) {
        if (!(item instanceof ViewGroup) || !(item.getTag() instanceof String)
                || !((String) item.getTag()).startsWith("launcher_widget:")) return;
        ViewGroup container = (ViewGroup) item;
        if (container.getChildCount() == 0 || !(container.getChildAt(0) instanceof AppWidgetHostView)) return;
        try {
            int widgetId = Integer.parseInt(((String) item.getTag()).substring("launcher_widget:".length()));
            WidgetController.updateWidgetSize(AppWidgetManager.getInstance(activity), widgetId,
                    (AppWidgetHostView) container.getChildAt(0),
                    activity.getResources().getDisplayMetrics().density, width, height);
        } catch (NumberFormatException ignored) {
            // Keep the container at its landscape-selected dimensions.
        }
    }

    private void adaptLandscapeWorkspace(LinearLayout strip) {
        if (strip == null) return;
        int step = Math.max(1, LauncherUiUtils.dp(activity, 4));
        int fineStep = Math.max(1, LauncherUiUtils.dp(activity, 1));
        for (int pageIndex = 0; pageIndex < strip.getChildCount(); pageIndex++) {
            View pageView = strip.getChildAt(pageIndex);
            if (!(pageView instanceof FrameLayout)) continue;
            FrameLayout page = (FrameLayout) pageView;
            int pageWidth = page.getWidth();
            int pageHeight = page.getHeight();
            if (pageWidth <= 0 || pageHeight <= 0) continue;

            ArrayList<Rect> occupied = new ArrayList<>();
            ArrayList<View> items = new ArrayList<>();
            for (int i = 0; i < page.getChildCount(); i++) {
                View child = page.getChildAt(i);
                if (child.getVisibility() != View.VISIBLE) continue;
                Object tag = child.getTag();
                if (!(tag instanceof String)) continue;
                String name = (String) tag;
                if (name.startsWith("launcher_app:")) {
                    Rect iconBounds = visibleAppBounds(child);
                    if (iconBounds != null && !iconBounds.isEmpty()) occupied.add(iconBounds);
                } else if (isAppGroupWorkspaceItem(child)) {
                    // App groups are already rendered at this orientation's grid row.
                    // Reserve their slots instead of sending them through pixel-based packing.
                    Rect groupBounds = workspaceBounds(child, child.getLeft(), child.getTop(),
                            child.getWidth(), child.getHeight());
                    if (!groupBounds.isEmpty()) occupied.add(groupBounds);
                } else if (isOrientationManagedItem(child) || isClockWorkspaceItem(child)) {
                    // Clock panels are distinct per page. They may be fitted in place,
                    // but must never enter the shared portrait-geometry save/restore path.
                    items.add(child);
                }
            }
            // The clock is anchored to its existing page and coordinates. Resize it first
            // and reserve its footprint so every other movable item yields space to it.
            for (int i = items.size() - 1; i >= 0; i--) {
                View candidate = items.get(i);
                if (!isClockWorkspaceItem(candidate)) continue;
                fitClockAtFixedPosition(candidate, page, occupied);
                items.remove(i);
            }

            // Keep shortcuts and groups in their rows where possible. Flexible widgets
            // are packed afterwards so they yield space before fixed-size launcher items.
            Collections.sort(items, (first, second) -> {
                int flexCompare = Boolean.compare(isResizableLandscapeItem(first), isResizableLandscapeItem(second));
                if (flexCompare != 0) return flexCompare;
                FrameLayout.LayoutParams a = first.getLayoutParams() instanceof FrameLayout.LayoutParams
                        ? (FrameLayout.LayoutParams) first.getLayoutParams() : null;
                FrameLayout.LayoutParams b = second.getLayoutParams() instanceof FrameLayout.LayoutParams
                        ? (FrameLayout.LayoutParams) second.getLayoutParams() : null;
                int topA = a == null ? first.getTop() : a.topMargin;
                int topB = b == null ? second.getTop() : b.topMargin;
                int rowCompare = Integer.compare(topA, topB);
                if (rowCompare != 0) return rowCompare;
                int leftA = a == null ? first.getLeft() : a.leftMargin;
                int leftB = b == null ? second.getLeft() : b.leftMargin;
                return Integer.compare(leftA, leftB);
            });

            for (View item : items) {
                if (!(item.getLayoutParams() instanceof FrameLayout.LayoutParams)) continue;
                FrameLayout.LayoutParams current = (FrameLayout.LayoutParams) item.getLayoutParams();
                int originalLeft = current.leftMargin;
                int originalTop = current.topMargin;
                int originalWidth = current.width > 0 ? current.width : item.getWidth();
                int originalHeight = current.height > 0 ? current.height : item.getHeight();
                if (originalWidth <= 0 || originalHeight <= 0) continue;
                int width = Math.min(pageWidth, originalWidth);
                int height = Math.min(pageHeight, originalHeight);
                int[] position = findFreePosition(item, originalLeft, originalTop, width, height,
                        pageWidth, pageHeight, occupied, step);
                if (position == null) {
                    position = findFreePosition(item, originalLeft, originalTop, width, height,
                            pageWidth, pageHeight, occupied, fineStep);
                }
                boolean resizable = isResizableLandscapeItem(item);
                while (position == null && resizable && (width > 1 || height > 1)) {
                    int reducedWidth = Math.max(1, Math.round(width * 0.8f));
                    int reducedHeight = Math.max(1, Math.round(height * 0.8f));
                    if (reducedWidth == width && width > 1) reducedWidth--;
                    if (reducedHeight == height && height > 1) reducedHeight--;
                    width = reducedWidth;
                    height = reducedHeight;
                    position = findFreePosition(item, originalLeft, originalTop, width, height,
                            pageWidth, pageHeight, occupied, step);
                    if (position == null) {
                        position = findFreePosition(item, originalLeft, originalTop, width, height,
                                pageWidth, pageHeight, occupied, fineStep);
                    }
                }
                if (position == null) {
                    // Keep a final fallback for an already completely saturated workspace.
                    position = new int[]{Math.max(0, Math.min(pageWidth - width, originalLeft)),
                            Math.max(0, Math.min(pageHeight - height, originalTop))};
                }
                FrameLayout.LayoutParams placed = copyWorkspaceParams(item, width, height,
                        position[0], position[1]);
                item.setLayoutParams(placed);
                Rect placedBounds = workspaceBounds(item, position[0], position[1], width, height);
                occupied.add(placedBounds);
                if (width != originalWidth || height != originalHeight) {
                    resizeAppWidgetForLandscape(item, width, height);
                }
                item.invalidate();
            }
        }
    }

    private static final String LAST_GEOMETRY_LANDSCAPE_KEY =
            "__workspace_geometry_last_applied_landscape";

    private void scheduleOrientationWorkspaceRepair(LinearLayout strip) {
        if (strip == null) return;
        // Wait for page measurement, icon grid correction, and widget restoration.
        strip.post(() -> strip.post(() -> {
            boolean landscape = isLandscapeOrientation(strip);
            boolean hasPreviousOrientation = prefs.contains(LAST_GEOMETRY_LANDSCAPE_KEY);
            boolean previousLandscape = prefs.getBoolean(
                    LAST_GEOMETRY_LANDSCAPE_KEY, landscape);

            // Restore portrait coordinates only once when entering landscape, and once
            // when returning to portrait. Repeated portrait renders must not undo user moves.
            if (!hasPreviousOrientation || landscape != previousLandscape) {
                restoreSavedWorkspaceGeometry(strip);
            }
            if (landscape) {
                adaptLandscapeWorkspace(strip);
            } else {
                restorePortraitClockScales(strip);
                snapshotPortraitWorkspaceGeometry(strip);
            }
            prefs.edit().putBoolean(LAST_GEOMETRY_LANDSCAPE_KEY, landscape).apply();
        }));
    }

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

    private boolean isWorkspaceItem(View view) {
        if (view == null || !(view.getTag() instanceof String)) return false;
        String tag = (String) view.getTag();
        return tag.startsWith("launcher_app:")
                || tag.startsWith("launcher_widget:")
                || tag.contains("shortcut")
                || tag.contains("group")
                || tag.contains("clock_widget");
    }

    /**
     * Recover only transient drawing state after a page transfer. A widget's non-zero
     * scale may represent intentional collision recovery, so preserve it and its margins.
     */
    private void normalizeWorkspaceItemVisuals(View item, boolean afterReparent) {
        if (item == null) return;
        if (afterReparent) item.animate().cancel();
        if (item.getVisibility() != View.VISIBLE) item.setVisibility(View.VISIBLE);
        if (Math.abs(item.getAlpha() - 1f) > 0.001f) item.setAlpha(1f);
        if (Math.abs(item.getScaleX()) < 0.01f) item.setScaleX(1f);
        if (Math.abs(item.getScaleY()) < 0.01f) item.setScaleY(1f);
        if (afterReparent) {
            if (Math.abs(item.getTranslationX()) > 0.001f) item.setTranslationX(0f);
            if (Math.abs(item.getTranslationY()) > 0.001f) item.setTranslationY(0f);
            if (Math.abs(item.getRotationX()) > 0.001f) item.setRotationX(0f);
            if (Math.abs(item.getRotationY()) > 0.001f) item.setRotationY(0f);
            if (Math.abs(item.getTranslationZ()) > 0.001f) item.setTranslationZ(0f);
        }
    }

    private void watchWorkspaceItemAttachment(View item) {
        if (!isWorkspaceItem(item) || workspaceItemAttachWatchers.containsKey(item)) return;
        final boolean[] wasAttached = {item.isAttachedToWindow()};
        item.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override public void onViewAttachedToWindow(View view) {
                if (wasAttached[0]) {
                    normalizeWorkspaceItemVisuals(view, true);
                } else {
                    wasAttached[0] = true;
                }
            }
            @Override public void onViewDetachedFromWindow(View view) { }
        });
        workspaceItemAttachWatchers.put(item, Boolean.TRUE);
    }

    private void normalizeWorkspaceItemsOnPage(FrameLayout page) {
        if (page == null || page.getParent() == null
                || page.getWidth() <= 0 || page.getHeight() <= 0) return;
        for (int i = 0; i < page.getChildCount(); i++) {
            View item = page.getChildAt(i);
            if (!isWorkspaceItem(item)) continue;
            watchWorkspaceItemAttachment(item);
            normalizeWorkspaceItemVisuals(item, false);
        }
    }

    private void normalizeMovedWorkspaceItemVisibility(LinearLayout strip, int pageCount) {
        if (strip == null) return;
        int limit = Math.min(pageCount, strip.getChildCount());
        for (int pageIndex = 0; pageIndex < limit; pageIndex++) {
            View child = strip.getChildAt(pageIndex);
            if (!(child instanceof FrameLayout)) continue;
            FrameLayout page = (FrameLayout) child;
            normalizeWorkspaceItemsOnPage(page);
            // Run after the drop/cleanup callbacks on this frame, which can otherwise
            // reapply the temporary hidden state after the synchronous render returns.
            page.post(() -> normalizeWorkspaceItemsOnPage(page));
        }
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
            // Keep every page's drawing confined to its own width.
            newPage.setClipChildren(true);
            newPage.setClipToPadding(true);
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
            page.setClipChildren(true);
            page.setClipToPadding(true);
            List<String> pagePackages = host.getPages().get(pageIndex);
            int fallbackHeight = Math.max(LauncherUiUtils.dp(activity, 360),
                    activity.getResources().getDisplayMetrics().heightPixels
                            - LauncherUiUtils.dp(activity, 260));
            // Use the actual page dimensions whenever available. The post-layout
            // correction below also uses page.getHeight(); mixing viewport and page
            // heights reinterprets normalized Y positions as different grid rows.
            int pageHeight = page.getHeight() > LauncherUiUtils.dp(activity, 100)
                    ? page.getHeight()
                    : (host.getPageViewport() != null
                            && host.getPageViewport().getHeight() > LauncherUiUtils.dp(activity, 100)
                                    ? host.getPageViewport().getHeight() : fallbackHeight);
            int configuredIconSize = prefs.getInt(iconSizeKey, 48);
            // Use the same label-aware slot height as dragging, snapping, capacity,
            // and collision checks. Added label height extends only below the icon.
            int cellHeight = LauncherUiUtils.dp(activity,
                    host.getHomeCellHeightDp(configuredIconSize));
            // Each app owns one fixed-height grid slot. Row capacity reserves the
            // label-enabled 25dp clearance above the dock through the shared host rule.
            final int rows = host.getHomeGridRowsForHeight(pageHeight, cellHeight);
            boolean[][] occupiedSlots = new boolean[rows][columns];
            LauncherAppAdapter homeAdapter = new LauncherAppAdapter(activity, prefs, packageManager,
                    host.getAllApps(), host.getDrawerApps(), pagePackages,
                    false, iconSizeKey, fontSizeKey, showLabelsKey,
                    new LauncherAppAdapter.Actions() {
                        @Override public void onAppClick(ResolveInfo app, View sourceView) { host.launchApp(app, sourceView); }
                        @Override public void onAppLongClick(ResolveInfo app, boolean onHome,
                                View sourceView) {
                            host.showAppOptions(app, onHome, sourceView);
                        }
                    });
            for (int itemIndex = 0; itemIndex < pagePackages.size(); itemIndex++) {
                String packageName = pagePackages.get(itemIndex);
                ResolveInfo app = host.findApp(packageName);
                if (app == null) continue;
                View cell = homeAdapter.getViewForApp(app, null, page);
                cell.setTag("launcher_app:" + packageName);
                int actualPageWidth = page.getWidth() > 0 ? page.getWidth() : pageWidth;
                int cellWidth = homeAppCellWidthPx(page, strip, columns, actualPageWidth);
                // In landscape, this cell pitch is derived from the saved portrait row span
                // in dp, so the app grid cannot stretch across the wider landscape page.
                int maxArtworkSize = Math.max(1,
                        cellWidth - LauncherUiUtils.dp(activity, 4));
                if (cell instanceof ViewGroup && ((ViewGroup) cell).getChildCount() > 0
                        && ((ViewGroup) cell).getChildAt(0) instanceof android.widget.ImageView) {
                    View iconView = ((ViewGroup) cell).getChildAt(0);
                    ViewGroup.LayoutParams iconParams = iconView.getLayoutParams();
                    if (iconParams != null) {
                        int fittedSize = Math.min(LauncherUiUtils.dp(activity, configuredIconSize),
                                maxArtworkSize);
                        iconParams.width = fittedSize;
                        iconParams.height = fittedSize;
                        iconView.setLayoutParams(iconParams);
                    }
                }
                FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                        cellWidth, cellHeight, Gravity.TOP | Gravity.LEFT);
                int defaultCol = itemIndex % columns;
                int defaultRow = (itemIndex / columns) % rows;
                float savedX = LauncherUiUtils.getFloatPreferenceCompat(prefs, iconXKey + packageName,
                        defaultCol / (float) Math.max(1, columns - 1));
                float savedY = LauncherUiUtils.getFloatPreferenceCompat(prefs, iconYKey + packageName,
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
                    if (bestCol >= 0) {
                        col = bestCol;
                        row = bestRow;
                        savedX = col / (float) Math.max(1, columns - 1);
                        savedY = row / (float) Math.max(1, rows - 1);
                        prefs.edit().putFloat(iconXKey + packageName, savedX)
                                .putFloat(iconYKey + packageName, savedY).apply();
                    }
                    // If every slot is occupied, keep the app attached at its saved slot.
                    // Size-change validation can then report/repair its real collision rather
                    // than silently omitting this app from the rendered page.
                }
                occupiedSlots[row][col] = true;
                lp.leftMargin = col * cellWidth;
                // Keep rows at their original fixed grid positions. In particular,
                // the lowest row stays one cell above the workspace edge instead of
                // being stretched down toward the dock.
                lp.topMargin = host.getHomeGridRowTop(row, rows, pageHeight, cellHeight);
                page.addView(cell, lp);
                host.installHomeIconTouch(cell, packageName);
            }
            page.post(() -> {
                if (page.getWidth() <= 0 || page.getHeight() <= 0) return;
                int actualCellWidth = homeAppCellWidthPx(page, strip, columns, page.getWidth());
                if (!isLandscapeOrientation(strip)) {
                    savePortraitHomeAppRowWidthDp(actualCellWidth, columns);
                }
                // Apply the same physical cell pitch in the post-layout pass; do not let
                // landscape page width overwrite the row's saved portrait span.
                int actualCellHeight = LauncherUiUtils.dp(activity,
                        host.getHomeCellHeightDp(configuredIconSize));
                int actualRows = host.getHomeGridRowsForHeight(page.getHeight(), actualCellHeight);
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
                    float savedX = LauncherUiUtils.getFloatPreferenceCompat(prefs, iconXKey + childPackage,
                            defaultCol / (float) Math.max(1, columns - 1));
                    float savedY = LauncherUiUtils.getFloatPreferenceCompat(prefs, iconYKey + childPackage,
                            defaultRow / (float) Math.max(1, actualRows - 1));
                    int col = Math.max(0, Math.min(columns - 1,
                            Math.round(savedX * Math.max(1, columns - 1))));
                    int row = Math.max(0, Math.min(actualRows - 1,
                            Math.round(savedY * Math.max(1, actualRows - 1))));
                    FrameLayout.LayoutParams actual = (FrameLayout.LayoutParams) child.getLayoutParams();
                    actual.width = actualCellWidth;
                    actual.height = actualCellHeight;
                    actual.leftMargin = col * actualCellWidth;
                    actual.topMargin = host.getHomeGridRowTop(
                            row, actualRows, page.getHeight(), actualCellHeight);
                    child.setLayoutParams(actual);
                }
            });
        }

        host.restoreWidgetsOnPages();
        normalizeMovedWorkspaceItemVisibility(strip, requiredPages);
        scheduleOrientationWorkspaceRepair(strip);
        if (host.getPageIndicator() != null) {
            boolean showIndicator = requiredPages > 1
                    && prefs.getBoolean(showPageIndicatorKey, true);
            host.getPageIndicator().setVisibility(showIndicator ? View.VISIBLE : View.GONE);
            host.getPageIndicator().setText(
                    (host.getCurrentPage() + 1) + "  /  " + requiredPages);
        }
        strip.animate().cancel();
        float targetX = -host.getCurrentPage() * pageWidth;
        if (animate) {
            strip.setLayerType(View.LAYER_TYPE_HARDWARE, null);
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
