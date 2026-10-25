package com.dsann.launcher;

import android.appwidget.AppWidgetManager;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;

import java.util.ArrayList;
import java.util.List;

/**
 * Fixed two-screen workspace. Page indexes are stable: empty pages are intentional,
 * and content is never compacted or silently moved to the other screen.
 */
final class HomePageModel {
    static final int PAGE_COUNT = 2;

    private static final String PORTRAIT_PAGE_PREFIX = "__portrait_icon_page__";
    private static final String PORTRAIT_X_PREFIX = "__portrait_icon_x__";
    private static final String PORTRAIT_Y_PREFIX = "__portrait_icon_y__";
    private static final String LANDSCAPE_X_PREFIX = "__landscape_icon_x__";
    private static final String LANDSCAPE_Y_PREFIX = "__landscape_icon_y__";
    private static final String LAST_LANDSCAPE_KEY = "__workspace_last_was_landscape";
    private static final String PORTRAIT_ROWS_KEY = "__workspace_portrait_grid_rows";

    private static boolean isLandscapeConfiguration(SharedPreferences prefs, int rows) {
        try {
            int orientation = Resources.getSystem().getConfiguration().orientation;
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) return true;
            if (orientation == Configuration.ORIENTATION_PORTRAIT) return false;
        } catch (RuntimeException ignored) {
            // Fall back to the last known portrait grid when system configuration is unavailable.
        }
        return rows < prefs.getInt(PORTRAIT_ROWS_KEY, rows);
    }

    static final class Result {
        final List<List<String>> pages;
        final int currentPage;
        Result(List<List<String>> pages, int currentPage) {
            this.pages = pages;
            this.currentPage = currentPage;
        }
    }

    private HomePageModel() {}

    static Result rebuild(SharedPreferences prefs, List<String> homePackages,
            AppWidgetManager widgetManager, int requestedPage, int capacity,
            String iconPagePrefix, String widgetIdsKey, String iconXPrefix, String iconYPrefix,
            int columns, int rows) {
        columns = Math.max(1, columns);
        rows = Math.max(1, rows);

        boolean landscapeLayout = isLandscapeConfiguration(prefs, rows);

        List<List<String>> pages = new ArrayList<>();
        pages.add(new ArrayList<String>());
        pages.add(new ArrayList<String>());

        List<String> unique = new ArrayList<>();
        if (homePackages != null) {
            for (String name : homePackages) {
                if (name != null && !name.trim().isEmpty() && !unique.contains(name)) unique.add(name);
            }
        }

        SharedPreferences.Editor edit = prefs.edit();
        int pageCapacity = Math.max(1, Math.min(capacity, columns * rows));
        int[] pageLoads = new int[PAGE_COUNT];
        // Keep each screen within its real visible grid. If old versions stored too
        // many apps on one page, migrate the overflow to the other page instead of
        // letting the renderer silently omit shortcuts below the dock.
        for (String name : unique) {
            String portraitPageKey = PORTRAIT_PAGE_PREFIX + name;
            int storedPage = prefs.getInt(iconPagePrefix + name,
                    prefs.getInt(portraitPageKey, 0));
            int preferredPage = clampPage(storedPage);
            int otherPage = 1 - preferredPage;
            int page = preferredPage;
            if (pageLoads[page] >= pageCapacity && pageLoads[otherPage] < pageCapacity) {
                page = otherPage;
            }
            pages.get(page).add(name);
            pageLoads[page]++;
            edit.putInt(iconPagePrefix + name, page);
        }

        // Normalize positions against the same row count used by the visible grid.
        for (int pageIndex = 0; pageIndex < PAGE_COUNT; pageIndex++) {
            List<String> items = pages.get(pageIndex);
            int effectiveRows = Math.max(rows, (items.size() + columns - 1) / columns);
            boolean[][] used = new boolean[effectiveRows][columns];
            for (int index = 0; index < items.size(); index++) {
                String name = items.get(index);
                int defaultCol = index % columns;
                int defaultRow = index / columns;
                float storedX = LauncherUiUtils.getFloatPreferenceCompat(prefs, iconXPrefix + name,
                        defaultCol / (float) Math.max(1, columns - 1));
                float storedY = LauncherUiUtils.getFloatPreferenceCompat(prefs, iconYPrefix + name,
                        defaultRow / (float) Math.max(1, effectiveRows - 1));
                String portraitXKey = PORTRAIT_X_PREFIX + name;
                String portraitYKey = PORTRAIT_Y_PREFIX + name;
                String landscapeXKey = LANDSCAPE_X_PREFIX + name;
                String landscapeYKey = LANDSCAPE_Y_PREFIX + name;
                float portraitX = prefs.getFloat(portraitXKey, storedX);
                float portraitY = prefs.getFloat(portraitYKey, storedY);
                // Seed landscape from the portrait arrangement only when no landscape
                // placement exists. Subsequent manual changes are orientation-local.
                float sx = landscapeLayout
                        ? prefs.getFloat(landscapeXKey, portraitX) : portraitX;
                float sy = landscapeLayout
                        ? prefs.getFloat(landscapeYKey, portraitY) : portraitY;
                int wantedCol = Math.max(0, Math.min(columns - 1,
                        Math.round(sx * Math.max(1, columns - 1))));
                int wantedRow = Math.max(0, Math.min(effectiveRows - 1,
                        Math.round(sy * Math.max(1, effectiveRows - 1))));
                int col = wantedCol;
                int row = wantedRow;
                if (used[row][col]) {
                    boolean found = false;
                    for (int distance = 1; distance <= effectiveRows * columns && !found; distance++) {
                        for (int rr = 0; rr < effectiveRows && !found; rr++) {
                            for (int cc = 0; cc < columns; cc++) {
                                if (!used[rr][cc]
                                        && Math.abs(cc - wantedCol) + Math.abs(rr - wantedRow) == distance) {
                                    col = cc;
                                    row = rr;
                                    found = true;
                                    break;
                                }
                            }
                        }
                    }
                }
                used[row][col] = true;
                float normalizedX = col / (float) Math.max(1, columns - 1);
                float normalizedY = row / (float) Math.max(1, effectiveRows - 1);
                edit.putInt(iconPagePrefix + name, pageIndex);
                edit.putFloat(iconXPrefix + name, normalizedX);
                edit.putFloat(iconYPrefix + name, normalizedY);
                // The page is shared structural state; only X/Y are orientation-specific.
                edit.putInt(PORTRAIT_PAGE_PREFIX + name, pageIndex);
                if (landscapeLayout) {
                    edit.putFloat(LANDSCAPE_X_PREFIX + name, normalizedX);
                    edit.putFloat(LANDSCAPE_Y_PREFIX + name, normalizedY);
                } else {
                    edit.putFloat(PORTRAIT_X_PREFIX + name, normalizedX);
                    edit.putFloat(PORTRAIT_Y_PREFIX + name, normalizedY);
                }
            }
        }

        List<Integer> widgetIds = new ArrayList<>();
        List<Integer> widgetPages = new ArrayList<>();
        String stored = prefs.getString(widgetIdsKey, "");
        if (stored != null && !stored.isEmpty()) {
            for (String line : stored.split("\\n")) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split(":");
                if (parts.length != 2) continue;
                try {
                    int id = Integer.parseInt(parts[0].trim());
                    int page = clampPage(Integer.parseInt(parts[1].trim()));
                    // AppWidgetManager may briefly return null while the provider package
                    // is being replaced or the system is rebuilding widget state. Preserve the
                    // host ID and its saved page; restoreWidgetsOnPages retries after the update.
                    // Removing it here made a transient update look like the user deleted a widget.
                    if (!widgetIds.contains(id)) {
                        widgetIds.add(id);
                        widgetPages.add(page);
                    }
                } catch (NumberFormatException ignored) {
                    // Ignore malformed records without crashing launcher startup.
                }
            }
        }
        StringBuilder normalizedWidgets = new StringBuilder();
        for (int i = 0; i < widgetIds.size(); i++) {
            if (normalizedWidgets.length() > 0) normalizedWidgets.append('\n');
            normalizedWidgets.append(widgetIds.get(i)).append(':').append(widgetPages.get(i));
        }
        edit.putString(widgetIdsKey, normalizedWidgets.toString());
        edit.putBoolean(LAST_LANDSCAPE_KEY, landscapeLayout);
        if (!landscapeLayout) edit.putInt(PORTRAIT_ROWS_KEY, rows);
        edit.apply();

        return new Result(pages, clampPage(requestedPage));
    }

    static int clampPage(int page) {
        return Math.max(0, Math.min(PAGE_COUNT - 1, page));
    }
}