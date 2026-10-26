package com.dsann.launcher;

import android.appwidget.AppWidgetManager;
import android.content.SharedPreferences;

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
            int columns, int rows, boolean landscapeLayout) {
        columns = Math.max(1, columns);
        rows = Math.max(1, rows);

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
        // Page membership is structural shared state. Never rebalance apps between screens
        // merely because rotation changes the number of visible rows: a user must explicitly
        // move an icon to the other screen for its page assignment to change.
        for (String name : unique) {
            String portraitPageKey = PORTRAIT_PAGE_PREFIX + name;
            int storedPage = prefs.getInt(iconPagePrefix + name,
                    prefs.getInt(portraitPageKey, 0));
            int page = clampPage(storedPage);
            pages.get(page).add(name);
            edit.putInt(iconPagePrefix + name, page);
        }

        // Keep saved placement orientation-local and stable across recreation.
        // Re-normalizing Y against an estimated row count on every rebuild caused icons
        // to drift when the first startup pass used fallback viewport dimensions.
        // HomePageController repairs actual cell collisions once the page is measured.
        for (int pageIndex = 0; pageIndex < PAGE_COUNT; pageIndex++) {
            List<String> items = pages.get(pageIndex);
            for (int index = 0; index < items.size(); index++) {
                String name = items.get(index);
                int defaultCol = index % columns;
                int defaultRow = (index / columns) % rows;
                float defaultX = defaultCol / (float) Math.max(1, columns - 1);
                float defaultY = defaultRow / (float) Math.max(1, rows - 1);
                float storedX = LauncherUiUtils.getFloatPreferenceCompat(
                        prefs, iconXPrefix + name, defaultX);
                float storedY = LauncherUiUtils.getFloatPreferenceCompat(
                        prefs, iconYPrefix + name, defaultY);
                String portraitXKey = PORTRAIT_X_PREFIX + name;
                String portraitYKey = PORTRAIT_Y_PREFIX + name;
                String landscapeXKey = LANDSCAPE_X_PREFIX + name;
                String landscapeYKey = LANDSCAPE_Y_PREFIX + name;
                float portraitX = clampNormalized(
                        LauncherUiUtils.getFloatPreferenceCompat(prefs, portraitXKey, storedX));
                float portraitY = clampNormalized(
                        LauncherUiUtils.getFloatPreferenceCompat(prefs, portraitYKey, storedY));
                if (!prefs.contains(portraitXKey)) edit.putFloat(portraitXKey, portraitX);
                if (!prefs.contains(portraitYKey)) edit.putFloat(portraitYKey, portraitY);

                float activeX = landscapeLayout
                        ? clampNormalized(LauncherUiUtils.getFloatPreferenceCompat(
                                prefs, landscapeXKey, portraitX))
                        : portraitX;
                float activeY = landscapeLayout
                        ? clampNormalized(LauncherUiUtils.getFloatPreferenceCompat(
                                prefs, landscapeYKey, portraitY))
                        : portraitY;
                if (landscapeLayout) {
                    edit.putFloat(landscapeXKey, activeX);
                    edit.putFloat(landscapeYKey, activeY);
                } else {
                    edit.putFloat(portraitXKey, activeX);
                    edit.putFloat(portraitYKey, activeY);
                }
                // Legacy consumers read base keys; mirror the active orientation without
                // overwriting the saved coordinates of the other orientation.
                edit.putFloat(iconXPrefix + name, activeX);
                edit.putFloat(iconYPrefix + name, activeY);
                edit.putInt(PORTRAIT_PAGE_PREFIX + name, pageIndex);
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

    private static float clampNormalized(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    static int clampPage(int page) {
        return Math.max(0, Math.min(PAGE_COUNT - 1, page));
    }
}