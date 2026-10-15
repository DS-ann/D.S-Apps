package com.dsann.launcher;

import android.appwidget.AppWidgetManager;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

/** Builds a stable multi-page workspace from persisted shortcut and widget membership. */
final class HomePageModel {
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
        capacity = Math.max(1, capacity);

        List<String> unique = new ArrayList<>();
        if (homePackages != null) {
            for (String name : homePackages) {
                if (name != null && !name.trim().isEmpty() && !unique.contains(name)) unique.add(name);
            }
        }

        // Preserve requested page membership, while preventing any page from exceeding
        // the configured icon capacity. A moved icon can therefore never vanish in overflow.
        List<List<String>> rawPages = new ArrayList<>();
        List<Integer> rawIconPages = new ArrayList<>();
        for (String name : unique) {
            int page = Math.max(0, prefs.getInt(iconPagePrefix + name, 0));
            int col = Math.max(1, columns);
            while (rawPages.size() <= page) rawPages.add(new ArrayList<String>());
            while (rawPages.get(page).size() >= capacity) {
                page++;
                while (rawPages.size() <= page) rawPages.add(new ArrayList<String>());
            }
            rawPages.get(page).add(name);
            rawIconPages.add(page);
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
                    int page = Math.max(0, Integer.parseInt(parts[1].trim()));
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

        int rawCount = Math.max(rawPages.size(), 1);
        for (Integer page : widgetPages) rawCount = Math.max(rawCount, page + 1);
        boolean[] occupiedPage = new boolean[rawCount];
        for (int i = 0; i < rawPages.size(); i++) {
            if (!rawPages.get(i).isEmpty()) occupiedPage[i] = true;
        }
        for (Integer page : widgetPages) if (page >= 0 && page < rawCount) occupiedPage[page] = true;

        // Compact only genuinely empty pages and build a stable old-to-new page map.
        int[] remap = new int[rawCount];
        List<List<String>> pages = new ArrayList<>();
        for (int old = 0; old < rawCount; old++) {
            if (occupiedPage[old]) {
                remap[old] = pages.size();
                pages.add(old < rawPages.size() ? new ArrayList<>(rawPages.get(old)) : new ArrayList<String>());
            } else {
                remap[old] = pages.size() - 1;
            }
        }
        if (pages.isEmpty()) pages.add(new ArrayList<String>());

        SharedPreferences.Editor edit = prefs.edit();
        for (int p = 0; p < rawPages.size(); p++) {
            int newPage = remap[Math.min(p, rawCount - 1)];
            List<String> items = rawPages.get(p);
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
                int col = wantedCol, row = wantedRow;
                if (used[row][col]) {
                    boolean found = false;
                    for (int distance = 1; distance <= effectiveRows * columns && !found; distance++) {
                        for (int rr = 0; rr < effectiveRows && !found; rr++) {
                            for (int cc = 0; cc < columns; cc++) {
                                if (!used[rr][cc] && Math.abs(cc - wantedCol) + Math.abs(rr - wantedRow) == distance) {
                                    col = cc; row = rr; found = true; break;
                                }
                            }
                        }
                    }
                }
                used[row][col] = true;
                edit.putInt(iconPagePrefix + name, newPage);
                edit.putFloat(iconXPrefix + name, col / (float) Math.max(1, columns - 1));
                edit.putFloat(iconYPrefix + name, row / (float) Math.max(1, effectiveRows - 1));
            }
        }

        StringBuilder normalizedWidgets = new StringBuilder();
        for (int i = 0; i < widgetIds.size(); i++) {
            int oldPage = Math.min(rawCount - 1, widgetPages.get(i));
            int newPage = remap[oldPage];
            if (normalizedWidgets.length() > 0) normalizedWidgets.append('\n');
            normalizedWidgets.append(widgetIds.get(i)).append(':').append(newPage);
        }
        edit.putString(widgetIdsKey, normalizedWidgets.toString());
        edit.apply();

        int current = Math.max(0, Math.min(pages.size() - 1, requestedPage));
        return new Result(pages, current);
    }
}
