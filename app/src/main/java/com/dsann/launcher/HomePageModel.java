package com.dsann.launcher;

import android.appwidget.AppWidgetManager;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

/**
 * Single source of truth for home-page membership.
 *
 * Page membership is stored on each shortcut and widget. This class validates those
 * records, compacts gaps, removes stale widget IDs, and returns a contiguous model
 * before any view hierarchy is rendered.
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
            String iconPagePrefix, String widgetIdsKey) {
        capacity = Math.max(1, capacity);
        List<PageEntry> entries = new ArrayList<>();
        int maxPage = 0;

        for (int i = 0; i < homePackages.size(); i++) {
            String packageName = homePackages.get(i);
            int fallbackPage = i / capacity;
            int savedPage = prefs.contains(iconPagePrefix + packageName)
                    ? prefs.getInt(iconPagePrefix + packageName, fallbackPage) : fallbackPage;
            int page = clampPage(savedPage);
            entries.add(new PageEntry(packageName, page));
            maxPage = Math.max(maxPage, page);
        }

        List<WidgetEntry> widgets = new ArrayList<>();
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
                    boolean duplicate = false;
                    for (WidgetEntry existing : widgets) if (existing.id == id) { duplicate = true; break; }
                    if (!duplicate) {
                        widgets.add(new WidgetEntry(id, page));
                        maxPage = Math.max(maxPage, page);
                    }
                } catch (NumberFormatException ignored) {
                    // Ignore corrupt persistence records rather than creating phantom pages.
                }
            }
        }

        // Build a sparse representation, then map only occupied pages into a contiguous
        // zero-based sequence. A page is occupied by at least one valid shortcut or widget.
        boolean[] occupied = new boolean[maxPage + 1];
        for (PageEntry entry : entries) occupied[entry.page] = true;
        for (WidgetEntry widget : widgets) occupied[widget.page] = true;

        int[] remap = new int[occupied.length];
        int count = 0;
        for (int i = 0; i < occupied.length; i++) {
            remap[i] = occupied[i] ? count++ : -1;
        }
        if (count == 0) count = 1;

        List<List<String>> resultPages = new ArrayList<>();
        for (int i = 0; i < count; i++) resultPages.add(new ArrayList<String>());
        SharedPreferences.Editor edit = prefs.edit();

        for (PageEntry entry : entries) {
            int page = remap[entry.page];
            if (page < 0) page = 0;
            resultPages.get(page).add(entry.packageName);
            edit.putInt(iconPagePrefix + entry.packageName, page);
        }

        StringBuilder normalizedWidgets = new StringBuilder();
        for (WidgetEntry widget : widgets) {
            int page = remap[widget.page];
            if (page < 0) page = 0;
            if (normalizedWidgets.length() > 0) normalizedWidgets.append('\n');
            normalizedWidgets.append(widget.id).append(':').append(page);
        }
        edit.putString(widgetIdsKey, normalizedWidgets.toString());

        int safeRequested = Math.max(0, Math.min(requestedPage, occupied.length - 1));
        int mappedCurrent = 0;
        for (int i = 0; i < safeRequested; i++) if (occupied[i]) mappedCurrent++;
        if (occupied[safeRequested]) mappedCurrent = remap[safeRequested];
        mappedCurrent = Math.max(0, Math.min(mappedCurrent, count - 1));

        edit.apply();
        return new Result(resultPages, mappedCurrent);
    }

    private static int clampPage(int page) {
        return Math.max(0, Math.min(20, page));
    }

    private static final class PageEntry {
        final String packageName;
        final int page;
        PageEntry(String packageName, int page) {
            this.packageName = packageName;
            this.page = page;
        }
    }

    private static final class WidgetEntry {
        final int id;
        final int page;
        WidgetEntry(int id, int page) {
            this.id = id;
            this.page = page;
        }
    }
}
