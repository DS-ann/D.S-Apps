package com.dsann.launcher;

import android.appwidget.AppWidgetManager;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

/**
 * Normalizes persisted home shortcuts and widgets for the launcher's single home screen.
 * Legacy page numbers are intentionally collapsed to page zero.
 */
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

        List<String> packages = new ArrayList<>();
        if (homePackages != null) {
            for (String packageName : homePackages) {
                if (packageName == null || packageName.trim().isEmpty() || packages.contains(packageName)) {
                    continue;
                }
                packages.add(packageName);
            }
        }

        // Preserve every shortcut on the one page. If the saved list exceeds the nominal
        // grid capacity, extend the logical row count so saved positions remain distinct.
        int effectiveRows = Math.max(rows, (packages.size() + columns - 1) / columns);
        boolean[][] used = new boolean[effectiveRows][columns];
        SharedPreferences.Editor edit = prefs.edit();
        for (int index = 0; index < packages.size(); index++) {
            String packageName = packages.get(index);
            int defaultCol = index % columns;
            int defaultRow = index / columns;
            float savedX = prefs.getFloat(iconXPrefix + packageName,
                    defaultCol / (float) Math.max(1, columns - 1));
            float savedY = prefs.getFloat(iconYPrefix + packageName,
                    defaultRow / (float) Math.max(1, effectiveRows - 1));
            int wantedCol = Math.max(0, Math.min(columns - 1,
                    Math.round(savedX * Math.max(1, columns - 1))));
            int wantedRow = Math.max(0, Math.min(effectiveRows - 1,
                    Math.round(savedY * Math.max(1, effectiveRows - 1))));
            int col = wantedCol;
            int row = wantedRow;
            if (used[row][col]) {
                int bestDistance = Integer.MAX_VALUE;
                for (int candidateRow = 0; candidateRow < effectiveRows; candidateRow++) {
                    for (int candidateCol = 0; candidateCol < columns; candidateCol++) {
                        if (used[candidateRow][candidateCol]) continue;
                        int distance = Math.abs(candidateCol - wantedCol)
                                + Math.abs(candidateRow - wantedRow);
                        if (distance < bestDistance) {
                            bestDistance = distance;
                            col = candidateCol;
                            row = candidateRow;
                        }
                    }
                }
            }
            used[row][col] = true;
            edit.putInt(iconPagePrefix + packageName, 0);
            edit.putFloat(iconXPrefix + packageName,
                    col / (float) Math.max(1, columns - 1));
            edit.putFloat(iconYPrefix + packageName,
                    row / (float) Math.max(1, effectiveRows - 1));
        }

        List<Integer> validWidgetIds = new ArrayList<>();
        String stored = prefs.getString(widgetIdsKey, "");
        if (stored != null && !stored.isEmpty()) {
            for (String line : stored.split("\\n")) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split(":");
                if (parts.length != 2) continue;
                try {
                    int id = Integer.parseInt(parts[0].trim());
                    Integer.parseInt(parts[1].trim()); // Validate legacy page field.
                    if (widgetManager == null || widgetManager.getAppWidgetInfo(id) == null) {
                        WidgetController.removeWidget(prefs, widgetIdsKey, id);
                        continue;
                    }
                    if (!validWidgetIds.contains(id)) validWidgetIds.add(id);
                } catch (NumberFormatException ignored) {
                    // Skip malformed persistence records.
                }
            }
        }

        StringBuilder normalizedWidgets = new StringBuilder();
        for (Integer id : validWidgetIds) {
            if (normalizedWidgets.length() > 0) normalizedWidgets.append('\n');
            normalizedWidgets.append(id).append(":0");
        }
        edit.putString(widgetIdsKey, normalizedWidgets.toString());
        edit.apply();

        List<List<String>> pages = new ArrayList<>();
        pages.add(packages);
        return new Result(pages, 0);
    }

}
