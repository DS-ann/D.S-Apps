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
        void moveWorkspaceWidgetToPage(View widget, int targetPageIndex, int left, int top);
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
    private final java.util.WeakHashMap<View, View.OnLayoutChangeListener>
            pendingLandscapeClockFitListeners =
                    new java.util.WeakHashMap<View, View.OnLayoutChangeListener>();
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
        return isWidgetWorkspaceItem(item);
    }

    private boolean isWidgetWorkspaceItem(View item) {
        return item != null && item.getTag() instanceof String
                && ((String) item.getTag()).startsWith("launcher_widget:");
    }

    private String landscapeWidgetSizePreferenceKey(View item, String kind, String axis) {
        if (!isWidgetWorkspaceItem(item)) return null;
        String id = ((String) item.getTag()).substring("launcher_widget:".length());
        return "__landscape_widget_" + kind + "_" + axis + "_" + id;
    }

    /**
     * Return the widget's intended size, not a size reduced by a previous landscape
     * repair. The last fitted dimensions distinguish an automatic reduction from a
     * deliberate resize performed by the user while the launcher remains in landscape.
     */
    private int[] getLandscapeWidgetBaseSize(View item, int currentWidth, int currentHeight) {
        WorkspaceGeometry portrait = readPortraitGeometry(item);
        String baseWidthKey = landscapeWidgetSizePreferenceKey(item, "base", "width");
        String baseHeightKey = landscapeWidgetSizePreferenceKey(item, "base", "height");
        String fittedWidthKey = landscapeWidgetSizePreferenceKey(item, "fitted", "width");
        String fittedHeightKey = landscapeWidgetSizePreferenceKey(item, "fitted", "height");

        int baseWidth = prefs.getInt(baseWidthKey, -1);
        int baseHeight = prefs.getInt(baseHeightKey, -1);
        int lastFittedWidth = prefs.getInt(fittedWidthKey, -1);
        int lastFittedHeight = prefs.getInt(fittedHeightKey, -1);

        if (baseWidth <= 0) {
            baseWidth = portrait != null && portrait.width > 0 ? portrait.width : currentWidth;
        } else if (lastFittedWidth > 0 && currentWidth > 0
                && currentWidth != lastFittedWidth) {
            // A changed live layout size is a user resize or a portrait restoration;
            // treat it as the new intended width rather than compounding the old shrink.
            baseWidth = portrait != null && currentWidth == portrait.width
                    ? portrait.width : currentWidth;
        }

        if (baseHeight <= 0) {
            baseHeight = portrait != null && portrait.height > 0 ? portrait.height : currentHeight;
        } else if (lastFittedHeight > 0 && currentHeight > 0
                && currentHeight != lastFittedHeight) {
            baseHeight = portrait != null && currentHeight == portrait.height
                    ? portrait.height : currentHeight;
        }

        baseWidth = Math.max(1, baseWidth);
        baseHeight = Math.max(1, baseHeight);
        prefs.edit().putInt(baseWidthKey, baseWidth).putInt(baseHeightKey, baseHeight).apply();
        return new int[]{baseWidth, baseHeight};
    }

    private void saveLandscapeWidgetFittedSize(View item, int width, int height) {
        if (!isWidgetWorkspaceItem(item) || width <= 0 || height <= 0) return;
        prefs.edit()
                .putInt(landscapeWidgetSizePreferenceKey(item, "fitted", "width"), width)
                .putInt(landscapeWidgetSizePreferenceKey(item, "fitted", "height"), height)
                .apply();
    }

    private void reserveAppGroupGridSlots(FrameLayout page, boolean[][] occupiedSlots,
            int columns, int rows, int pageHeight, int cellWidth, int cellHeight) {
        if (page == null || occupiedSlots == null || columns <= 0 || rows <= 0
                || cellWidth <= 0 || cellHeight <= 0) return;
        for (int index = 0; index < page.getChildCount(); index++) {
            View group = page.getChildAt(index);
            if (!isAppGroupWorkspaceItem(group) || group.getVisibility() != View.VISIBLE) continue;
            int width = group.getWidth();
            int height = group.getHeight();
            int left = group.getLeft();
            int top = group.getTop();
            ViewGroup.LayoutParams raw = group.getLayoutParams();
            if (raw instanceof FrameLayout.LayoutParams) {
                FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) raw;
                if (lp.width > 0) width = lp.width;
                if (lp.height > 0) height = lp.height;
                if (group.getWidth() <= 0) left = lp.leftMargin;
                if (group.getHeight() <= 0) top = lp.topMargin;
            }
            if (width <= 0 || height <= 0) continue;
            Rect groupBounds = workspaceBounds(group, left, top, width, height);
            for (int row = 0; row < rows; row++) {
                int slotTop = host.getHomeGridRowTop(row, rows, pageHeight, cellHeight);
                for (int col = 0; col < columns; col++) {
                    int slotLeft = col * cellWidth;
                    Rect slot = new Rect(slotLeft, slotTop,
                            slotLeft + cellWidth, slotTop + cellHeight);
                    if (Rect.intersects(groupBounds, slot)) occupiedSlots[row][col] = true;
                }
            }
        }
    }

    private String landscapeClockAutoScaleKey(int pageIndex, boolean horizontal) {
        return "workspace_landscape_clock_auto_scale_"
                + (horizontal ? "x" : "y") + "_page_" + pageIndex;
    }

    private String landscapeClockBaseScaleKey(int pageIndex, boolean horizontal) {
        return "workspace_landscape_clock_base_scale_"
                + (horizontal ? "x" : "y") + "_page_" + pageIndex;
    }

    private float clampLandscapeClockAutoScale(float value) {
        return Math.max(0.08f, Math.min(1f, value));
    }

    private boolean fitsWorkspacePosition(View item, int left, int top,
            int width, int height, int pageWidth, int pageHeight, ArrayList<Rect> occupied) {
        Rect candidate = workspaceBounds(item, left, top, width, height);
        return candidate.left >= 0 && candidate.top >= 0
                && candidate.right <= pageWidth && candidate.bottom <= pageHeight
                && !overlapsAny(candidate, occupied);
    }

    private ArrayList<Rect> collectOccupiedWorkspaceBounds(FrameLayout page, View ignoredItem) {
        ArrayList<Rect> occupied = new ArrayList<>();
        if (page == null) return occupied;
        for (int index = 0; index < page.getChildCount(); index++) {
            View child = page.getChildAt(index);
            if (child == ignoredItem || child.getVisibility() != View.VISIBLE) continue;
            Object rawTag = child.getTag();
            if (!(rawTag instanceof String)) continue;
            String tag = (String) rawTag;
            Rect bounds = null;
            if (tag.startsWith("launcher_app:")) {
                bounds = visibleAppBounds(child);
            } else if (isAppGroupWorkspaceItem(child) || isClockWorkspaceItem(child)
                    || isOrientationManagedItem(child)) {
                int width = child.getWidth();
                int height = child.getHeight();
                ViewGroup.LayoutParams raw = child.getLayoutParams();
                if (raw instanceof FrameLayout.LayoutParams) {
                    FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) raw;
                    if (lp.width > 0) width = lp.width;
                    if (lp.height > 0) height = lp.height;
                }
                if (width > 0 && height > 0) {
                    bounds = workspaceBounds(child, child.getLeft(), child.getTop(), width, height);
                }
            }
            if (bounds != null && !bounds.isEmpty()) occupied.add(bounds);
        }
        return occupied;
    }

    /**
     * Finds a collision-free landscape widget geometry with the smallest practical
     * dimensional change: horizontal length first, then height, and only then both.
     * Each candidate is tested at its original anchor before searching elsewhere on
     * the same page, so a widget does not lose height just because it needs less width.
     * Returns {left, top, width, height}, or null when the page cannot hold it.
     */
    private int[] findLandscapeWidgetFit(View widget, int preferredLeft, int preferredTop,
            int originalWidth, int originalHeight, int pageWidth, int pageHeight,
            ArrayList<Rect> occupied, int step, int fineStep) {
        if (widget == null || pageWidth <= 0 || pageHeight <= 0) return null;
        int baseWidth = Math.min(pageWidth, originalWidth);
        int baseHeight = Math.min(pageHeight, originalHeight);
        if (baseWidth <= 0 || baseHeight <= 0) return null;
        int minWidth = Math.min(baseWidth, LauncherUiUtils.dp(activity, 64));
        int minHeight = Math.min(baseHeight, LauncherUiUtils.dp(activity, 48));

        if (fitsWorkspacePosition(widget, preferredLeft, preferredTop, baseWidth, baseHeight,
                pageWidth, pageHeight, occupied)) {
            return new int[]{preferredLeft, preferredTop, baseWidth, baseHeight};
        }
        // Preserve full size when the item can fit elsewhere on this same screen.
        int[] fullSizePosition = findFreePosition(widget, preferredLeft, preferredTop,
                baseWidth, baseHeight, pageWidth, pageHeight, occupied, step);
        if (fullSizePosition == null) {
            fullSizePosition = findFreePosition(widget, preferredLeft, preferredTop,
                    baseWidth, baseHeight, pageWidth, pageHeight, occupied, fineStep);
        }
        if (fullSizePosition != null) {
            return new int[]{fullSizePosition[0], fullSizePosition[1], baseWidth, baseHeight};
        }

        // Modes: reduce length only, reduce height only, then reduce both dimensions.
        // Use 5% steps so sizing does not jump far past the actual free space.
        for (int mode = 0; mode < 3; mode++) {
            int width = baseWidth;
            int height = baseHeight;
            while ((mode == 0 && width > minWidth)
                    || (mode == 1 && height > minHeight)
                    || (mode == 2 && (width > minWidth || height > minHeight))) {
                int nextWidth = width;
                int nextHeight = height;
                if (mode == 0 || mode == 2) {
                    nextWidth = width > minWidth
                            ? Math.max(minWidth, Math.round(width * 0.95f)) : width;
                }
                if (mode == 1 || mode == 2) {
                    nextHeight = height > minHeight
                            ? Math.max(minHeight, Math.round(height * 0.95f)) : height;
                }
                if (nextWidth == width && nextHeight == height) break;
                width = nextWidth;
                height = nextHeight;

                if (fitsWorkspacePosition(widget, preferredLeft, preferredTop, width, height,
                        pageWidth, pageHeight, occupied)) {
                    return new int[]{preferredLeft, preferredTop, width, height};
                }
                int[] position = findFreePosition(widget, preferredLeft, preferredTop,
                        width, height, pageWidth, pageHeight, occupied, step);
                if (position == null) {
                    position = findFreePosition(widget, preferredLeft, preferredTop,
                            width, height, pageWidth, pageHeight, occupied, fineStep);
                }
                if (position != null) return new int[]{position[0], position[1], width, height};
            }
        }
        return null;
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
    /**
     * Fit the clock at its existing visual anchor. The landscape scale is persisted
     * separately from the user's clock size so MainActivity's normal measurement pass
     * cannot silently restore the oversized transform.
     */
    private boolean clockBoundsFitAtScale(View clock, int left, int top, int width, int height,
            int pageWidth, int pageHeight, ArrayList<Rect> occupied,
            float scaleX, float scaleY) {
        clock.setScaleX(scaleX);
        clock.setScaleY(scaleY);
        Rect bounds = workspaceBounds(clock, left, top, width, height);
        return bounds.left >= 0 && bounds.top >= 0
                && bounds.right <= pageWidth && bounds.bottom <= pageHeight
                && !overlapsAny(bounds, occupied);
    }

    private float largestClockAxisFactor(View clock, int left, int top, int width, int height,
            int pageWidth, int pageHeight, ArrayList<Rect> occupied,
            float baseScaleX, float baseScaleY, boolean horizontal, float minimumFactor) {
        float low = minimumFactor;
        float high = 1f;
        for (int pass = 0; pass < 12; pass++) {
            float middle = (low + high) * 0.5f;
            float sx = baseScaleX * (horizontal ? middle : 1f);
            float sy = baseScaleY * (horizontal ? 1f : middle);
            if (clockBoundsFitAtScale(clock, left, top, width, height,
                    pageWidth, pageHeight, occupied, sx, sy)) low = middle;
            else high = middle;
        }
        return low;
    }

    /**
     * Keep the clock at its existing page/anchor and reduce only the dimension needed
     * for collision clearance. The base dimensions are saved independently so a stale
     * landscape factor cannot make a clock remain tiny when a later pass has room.
     */
    private void scheduleClockFitAfterMeasurement(View clock) {
        if (clock == null || pendingLandscapeClockFitListeners.containsKey(clock)) return;
        View.OnLayoutChangeListener listener = new View.OnLayoutChangeListener() {
            @Override public void onLayoutChange(View view, int left, int top, int right, int bottom,
                    int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if (view.getWidth() <= 0 || view.getHeight() <= 0) return;
                view.removeOnLayoutChangeListener(this);
                pendingLandscapeClockFitListeners.remove(view);
                LinearLayout strip = host.getPagesStrip();
                if (strip != null && isLandscapeOrientation(strip)) {
                    // The initial repair can run before wrap-content clock panels have
                    // measured their date line. Re-run after the first real measurement,
                    // so a stale auto-shrink factor cannot leave the clock tiny or overlapping.
                    strip.post(() -> adaptLandscapeWorkspace(strip));
                }
            }
        };
        pendingLandscapeClockFitListeners.put(clock, listener);
        clock.addOnLayoutChangeListener(listener);
    }

    private void fitClockAtFixedPosition(View clock, FrameLayout page,
            ArrayList<Rect> occupied) {
        if (!(clock.getLayoutParams() instanceof FrameLayout.LayoutParams)) return;
        LinearLayout strip = host.getPagesStrip();
        int pageIndex = strip == null ? -1 : strip.indexOfChild(page);
        if (pageIndex < 0) return;

        FrameLayout.LayoutParams current = (FrameLayout.LayoutParams) clock.getLayoutParams();
        final int left = clock.getLeft();
        final int top = clock.getTop();
        int width = current.width > 0 ? current.width : clock.getWidth();
        int height = current.height > 0 ? current.height : clock.getHeight();
        if (width <= 0 || height <= 0) {
            scheduleClockFitAfterMeasurement(clock);
            return;
        }

        String keyX = landscapeClockAutoScaleKey(pageIndex, true);
        String keyY = landscapeClockAutoScaleKey(pageIndex, false);
        String baseKeyX = landscapeClockBaseScaleKey(pageIndex, true);
        String baseKeyY = landscapeClockBaseScaleKey(pageIndex, false);
        float previousLandscapeX = clampLandscapeClockAutoScale(
                LauncherUiUtils.getFloatPreferenceCompat(prefs, keyX, 1f));
        float previousLandscapeY = clampLandscapeClockAutoScale(
                LauncherUiUtils.getFloatPreferenceCompat(prefs, keyY, 1f));
        float actualScaleX = Math.max(0.01f, Math.abs(clock.getScaleX()));
        float actualScaleY = Math.max(0.01f, Math.abs(clock.getScaleY()));
        float baseScaleX = Math.max(0.01f, LauncherUiUtils.getFloatPreferenceCompat(
                prefs, baseKeyX, actualScaleX / previousLandscapeX));
        float baseScaleY = Math.max(0.01f, LauncherUiUtils.getFloatPreferenceCompat(
                prefs, baseKeyY, actualScaleY / previousLandscapeY));
        landscapeClockBaseScales.put(clock, new float[]{baseScaleX, baseScaleY});

        int pageWidth = page.getWidth();
        int pageHeight = page.getHeight();
        final float minimumFactor = 0.025f;
        float factorX = 1f;
        float factorY = 1f;
        boolean fits = clockBoundsFitAtScale(clock, left, top, width, height,
                pageWidth, pageHeight, occupied, baseScaleX, baseScaleY);

        if (!fits) {
            // Prefer horizontal length reduction so the clock height/date line is retained.
            if (clockBoundsFitAtScale(clock, left, top, width, height,
                    pageWidth, pageHeight, occupied, baseScaleX * minimumFactor, baseScaleY)) {
                factorX = largestClockAxisFactor(clock, left, top, width, height,
                        pageWidth, pageHeight, occupied, baseScaleX, baseScaleY,
                        true, minimumFactor);
            } else if (clockBoundsFitAtScale(clock, left, top, width, height,
                    pageWidth, pageHeight, occupied, baseScaleX, baseScaleY * minimumFactor)) {
                factorY = largestClockAxisFactor(clock, left, top, width, height,
                        pageWidth, pageHeight, occupied, baseScaleX, baseScaleY,
                        false, minimumFactor);
            } else {
                // If the obstacle crosses both dimensions, use the largest proportional
                // fit rather than repeated coarse reductions.
                float low = minimumFactor;
                float high = 1f;
                for (int pass = 0; pass < 12; pass++) {
                    float middle = (low + high) * 0.5f;
                    if (clockBoundsFitAtScale(clock, left, top, width, height,
                            pageWidth, pageHeight, occupied,
                            baseScaleX * middle, baseScaleY * middle)) low = middle;
                    else high = middle;
                }
                factorX = low;
                factorY = low;
            }
        }

        clock.setScaleX(baseScaleX * factorX);
        clock.setScaleY(baseScaleY * factorY);
        Rect candidate = workspaceBounds(clock, left, top, width, height);
        prefs.edit().putFloat(keyX, factorX).putFloat(keyY, factorY).apply();
        occupied.add(candidate);
        clock.requestLayout();
        clock.invalidate();
    }

    private void restorePortraitClockScales(LinearLayout strip) {
        if (strip == null) return;
        SharedPreferences.Editor editor = prefs.edit();
        boolean changed = false;
        for (int pageIndex = 0; pageIndex < strip.getChildCount(); pageIndex++) {
            View pageView = strip.getChildAt(pageIndex);
            if (!(pageView instanceof FrameLayout)) continue;
            FrameLayout page = (FrameLayout) pageView;
            for (int childIndex = 0; childIndex < page.getChildCount(); childIndex++) {
                View child = page.getChildAt(childIndex);
                if (!isClockWorkspaceItem(child)) continue;
                float[] baseScale = landscapeClockBaseScales.remove(child);
                if (baseScale != null) {
                    child.setScaleX(baseScale[0]);
                    child.setScaleY(baseScale[1]);
                    child.requestLayout();
                    child.invalidate();
                }
            }
            for (boolean horizontal : new boolean[]{true, false}) {
                String key = landscapeClockAutoScaleKey(pageIndex, horizontal);
                String baseKey = landscapeClockBaseScaleKey(pageIndex, horizontal);
                if (prefs.contains(key)) {
                    editor.remove(key);
                    changed = true;
                }
                if (prefs.contains(baseKey)) {
                    editor.remove(baseKey);
                    changed = true;
                }
            }
        }
        if (changed) editor.apply();
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
        ArrayList<View> widgetsNeedingOtherPage = new ArrayList<>();
        java.util.IdentityHashMap<View, int[]> originalWidgetSizes =
                new java.util.IdentityHashMap<>();
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
                int currentWidth = current.width > 0 ? current.width : item.getWidth();
                int currentHeight = current.height > 0 ? current.height : item.getHeight();
                if (currentWidth <= 0 || currentHeight <= 0) continue;

                boolean widget = isWidgetWorkspaceItem(item);
                int originalWidth = currentWidth;
                int originalHeight = currentHeight;
                if (widget) {
                    int[] baseSize = getLandscapeWidgetBaseSize(item, currentWidth, currentHeight);
                    originalWidth = baseSize[0];
                    originalHeight = baseSize[1];
                    originalWidgetSizes.put(item, baseSize);
                }
                int width = Math.min(pageWidth, originalWidth);
                int height = Math.min(pageHeight, originalHeight);
                boolean sizeChanged = width != currentWidth || height != currentHeight;
                int[] position = null;

                if (widget) {
                    int[] fit = findLandscapeWidgetFit(item, originalLeft, originalTop,
                            width, height, pageWidth, pageHeight, occupied, step, fineStep);
                    if (fit != null) {
                        position = new int[]{fit[0], fit[1]};
                        width = fit[2];
                        height = fit[3];
                        sizeChanged = width != currentWidth || height != currentHeight;
                    } else {
                        // No legal placement on this page even after dimensional shrink.
                        // Queue this widget for the other page without sacrificing its
                        // original size before the destination gets a chance to fit it.
                        widgetsNeedingOtherPage.add(item);
                        width = Math.min(width, LauncherUiUtils.dp(activity, 64));
                        height = Math.min(height, LauncherUiUtils.dp(activity, 48));
                        sizeChanged = width != currentWidth || height != currentHeight;
                        position = new int[]{
                                Math.max(0, Math.min(Math.max(0, pageWidth - width), originalLeft)),
                                Math.max(0, Math.min(Math.max(0, pageHeight - height), originalTop))};
                    }
                } else {
                    // Shortcuts keep their size and page; they never use the widget transfer path.
                    position = findFreePosition(item, originalLeft, originalTop, width, height,
                            pageWidth, pageHeight, occupied, step);
                    if (position == null) {
                        position = findFreePosition(item, originalLeft, originalTop, width, height,
                                pageWidth, pageHeight, occupied, fineStep);
                    }
                    if (position == null) {
                        position = new int[]{
                                Math.max(0, Math.min(Math.max(0, pageWidth - width), originalLeft)),
                                Math.max(0, Math.min(Math.max(0, pageHeight - height), originalTop))};
                    }
                }

                FrameLayout.LayoutParams placed = copyWorkspaceParams(item, width, height,
                        position[0], position[1]);
                if (widget) {
                    placed.gravity = Gravity.TOP | Gravity.LEFT;
                    placed.rightMargin = 0;
                    placed.bottomMargin = 0;
                }
                item.setLayoutParams(placed);
                Rect placedBounds = workspaceBounds(item, position[0], position[1], width, height);
                if (!widget || !widgetsNeedingOtherPage.contains(item)) occupied.add(placedBounds);
                if (sizeChanged && widget) resizeAppWidgetForLandscape(item, width, height);
                if (widget) saveLandscapeWidgetFittedSize(item, width, height);
                item.invalidate();
            }
        }

        // Only widgets may transfer screens, and only after shrinking and same-page
        // placement have both failed. The destination's final workspace bounds are rechecked.
        for (View widget : widgetsNeedingOtherPage) {
            if (!isWidgetWorkspaceItem(widget) || !(widget.getParent() instanceof FrameLayout)) continue;
            FrameLayout sourcePage = (FrameLayout) widget.getParent();
            int sourcePageIndex = strip.indexOfChild(sourcePage);
            if (sourcePageIndex < 0) continue;
            int targetPageIndex = sourcePageIndex == 0 ? 1 : 0;
            if (targetPageIndex >= strip.getChildCount()
                    || !(strip.getChildAt(targetPageIndex) instanceof FrameLayout)) continue;
            FrameLayout targetPage = (FrameLayout) strip.getChildAt(targetPageIndex);
            int targetWidth = targetPage.getWidth();
            int targetHeight = targetPage.getHeight();
            if (targetWidth <= 0 || targetHeight <= 0) continue;
            if (!(widget.getLayoutParams() instanceof FrameLayout.LayoutParams)) continue;
            FrameLayout.LayoutParams current = (FrameLayout.LayoutParams) widget.getLayoutParams();
            int currentWidth = current.width > 0 ? current.width : widget.getWidth();
            int currentHeight = current.height > 0 ? current.height : widget.getHeight();
            int[] originalSize = originalWidgetSizes.get(widget);
            int originalWidth = originalSize == null ? currentWidth : originalSize[0];
            int originalHeight = originalSize == null ? currentHeight : originalSize[1];
            int width = Math.min(targetWidth, originalWidth);
            int height = Math.min(targetHeight, originalHeight);
            int preferredLeft = Math.max(0,
                    Math.min(Math.max(0, targetWidth - width), current.leftMargin));
            int preferredTop = Math.max(0,
                    Math.min(Math.max(0, targetHeight - height), current.topMargin));
            ArrayList<Rect> destinationOccupied = collectOccupiedWorkspaceBounds(targetPage, widget);
            int[] fit = findLandscapeWidgetFit(widget, preferredLeft, preferredTop,
                    width, height, targetWidth, targetHeight, destinationOccupied, step, fineStep);
            if (fit == null) continue; // Both screens are saturated; retain the smallest visible source fallback.
            int[] position = new int[]{fit[0], fit[1]};
            width = fit[2];
            height = fit[3];
            boolean sizeChanged = width != currentWidth || height != currentHeight;

            FrameLayout.LayoutParams moved = copyWorkspaceParams(
                    widget, width, height, position[0], position[1]);
            moved.gravity = Gravity.TOP | Gravity.LEFT;
            moved.rightMargin = 0;
            moved.bottomMargin = 0;
            widget.setTranslationX(0f);
            widget.setTranslationY(0f);
            widget.setLayoutParams(moved);
            if (sizeChanged) resizeAppWidgetForLandscape(widget, width, height);
            saveLandscapeWidgetFittedSize(widget, width, height);
            host.moveWorkspaceWidgetToPage(widget, targetPageIndex, position[0], position[1]);
            widget.bringToFront();
            widget.requestLayout();
            widget.invalidate();
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
            int initialPageWidth = page.getWidth() > 0 ? page.getWidth() : pageWidth;
            final int appCellWidth = homeAppCellWidthPx(page, strip, columns, initialPageWidth);
            reserveAppGroupGridSlots(page, occupiedSlots, columns, rows,
                    pageHeight, appCellWidth, cellHeight);
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
                int cellWidth = appCellWidth;
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
                int actualRows = Math.max(1,
                        host.getHomeGridRowsForHeight(page.getHeight(), actualCellHeight));
                // The measured page can have fewer rows than the first render calculated.
                // Re-map normalized positions only through an occupied-slot grid so two
                // portrait rows can never collapse onto the same landscape cell.
                boolean[][] laidOutSlots = new boolean[actualRows][columns];
                // Group containers are fixed-size, not part of the resizable widget set.
                // Reserve the grid slots they visually occupy before placing app cells.
                for (int groupIndex = 0; groupIndex < page.getChildCount(); groupIndex++) {
                    View groupChild = page.getChildAt(groupIndex);
                    if (!isAppGroupWorkspaceItem(groupChild)
                            || groupChild.getVisibility() != View.VISIBLE
                            || groupChild.getWidth() <= 0 || groupChild.getHeight() <= 0) continue;
                    Rect groupBounds = workspaceBounds(groupChild, groupChild.getLeft(),
                            groupChild.getTop(), groupChild.getWidth(), groupChild.getHeight());
                    for (int groupRow = 0; groupRow < actualRows; groupRow++) {
                        int slotTop = host.getHomeGridRowTop(
                                groupRow, actualRows, page.getHeight(), actualCellHeight);
                        for (int groupCol = 0; groupCol < columns; groupCol++) {
                            int slotLeft = groupCol * actualCellWidth;
                            Rect slotBounds = new Rect(slotLeft, slotTop,
                                    slotLeft + actualCellWidth, slotTop + actualCellHeight);
                            if (Rect.intersects(groupBounds, slotBounds)) {
                                laidOutSlots[groupRow][groupCol] = true;
                            }
                        }
                    }
                }
                for (int childIndex = 0; childIndex < page.getChildCount(); childIndex++) {
                    View child = page.getChildAt(childIndex);
                    Object childTag = child.getTag();
                    if (!(childTag instanceof String)
                            || !((String) childTag).startsWith("launcher_app:")) continue;
                    String childPackage = ((String) childTag).substring("launcher_app:".length());
                    int orderIndex = pagePackages.indexOf(childPackage);
                    if (orderIndex < 0) continue;
                    int defaultCol = orderIndex % columns;
                    int defaultRow = (orderIndex / columns) % actualRows;
                    float savedX = LauncherUiUtils.getFloatPreferenceCompat(prefs, iconXKey + childPackage,
                            defaultCol / (float) Math.max(1, columns - 1));
                    float savedY = LauncherUiUtils.getFloatPreferenceCompat(prefs, iconYKey + childPackage,
                            defaultRow / (float) Math.max(1, actualRows - 1));
                    int col = Math.max(0, Math.min(columns - 1,
                            Math.round(savedX * Math.max(1, columns - 1))));
                    int row = Math.max(0, Math.min(actualRows - 1,
                            Math.round(savedY * Math.max(1, actualRows - 1))));
                    if (laidOutSlots[row][col]) {
                        int bestCol = -1, bestRow = -1, bestDistance = Integer.MAX_VALUE;
                        for (int candidateRow = 0; candidateRow < actualRows; candidateRow++) {
                            for (int candidateCol = 0; candidateCol < columns; candidateCol++) {
                                if (laidOutSlots[candidateRow][candidateCol]) continue;
                                int distance = Math.abs(candidateCol - col)
                                        + Math.abs(candidateRow - row);
                                if (distance < bestDistance) {
                                    bestDistance = distance;
                                    bestCol = candidateCol;
                                    bestRow = candidateRow;
                                }
                            }
                        }
                        if (bestCol >= 0) {
                            col = bestCol;
                            row = bestRow;
                        }
                    }
                    laidOutSlots[row][col] = true;
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
