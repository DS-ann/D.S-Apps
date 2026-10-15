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
        for (String name : unique) {
            int page = clampPage(prefs.getInt(iconPagePrefix + name, 0));
            pages.get(page).add(name);
        }

        // Normalize positions within each screen only. Do not shift icons to another page.
        for (int pageIndex = 0; pageIndex < PAGE_COUNT; pageIndex++) {
            List<String> items = pages.get(pageIndex);
            int effectiveRows = Math.max(rows, (items.size() + columns - 1) / columns);
            boolean[][] used = new boolean[effectiveRows][columns];
            for (int index = 0; index < items.size(); index++) {
                String name = items.get(index);
                int defaultCol = index % columns;
                int defaultRow = index / columns;
                float sx = prefs.getFloat(iconXPrefix + name,
                        defaultCol / (float) Math.max(1, columns - 1));
                float sy = prefs.getFloat(iconYPrefix + name,
                        defaultRow / (float) Math.max(1, effectiveRows - 1));
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
                edit.putInt(iconPagePrefix + name, pageIndex);
                edit.putFloat(iconXPrefix + name, col / (float) Math.max(1, columns - 1));
                edit.putFloat(iconYPrefix + name, row / (float) Math.max(1, effectiveRows - 1));
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
                    if (widgetManager == null || widgetManager.getAppWidgetInfo(id) == null) {
                        WidgetController.removeWidget(prefs, widgetIdsKey, id);
                        continue;
                    }
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
        edit.apply();

        return new Result(pages, clampPage(requestedPage));
    }

    static int clampPage(int page) {
        return Math.max(0, Math.min(PAGE_COUNT - 1, page));
    }
}