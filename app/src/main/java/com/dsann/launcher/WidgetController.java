package com.dsann.launcher;

import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Build;
import android.view.ViewGroup;
import android.widget.FrameLayout;

/** Shared widget persistence operations, kept out of the activity's view code. */
final class WidgetController {
    private WidgetController() {}

    static void updateWidgetSize(AppWidgetManager manager, int widgetId, AppWidgetHostView hostView,
                                 float density, int widthPx, int heightPx) {
        int widthDp = Math.max(1, Math.round(widthPx / Math.max(0.1f, density)));
        int heightDp = Math.max(1, Math.round(heightPx / Math.max(0.1f, density)));
        Bundle options = new Bundle();
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, widthDp);
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, widthDp);
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, heightDp);
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, heightDp);
        try { manager.updateAppWidgetOptions(widgetId, options); } catch (Exception ignored) {}
        try {
            // The four-dimension overload was added after Android 6 (API 23).
            // Calling it unconditionally can throw NoSuchMethodError on older devices.
            if (Build.VERSION.SDK_INT >= 31) {
                hostView.updateAppWidgetSize(options, widthDp, heightDp, widthDp, heightDp);
            } else {
                hostView.updateAppWidgetSize(options, widthDp, heightDp);
            }
        } catch (Exception ignored) {}
    }

    static boolean containsWidgetId(String stored, int widgetId) {
        if (stored == null || stored.isEmpty()) return false;
        for (String entry : stored.split("\\n")) {
            String[] parts = entry.split(":");
            if (parts.length >= 2) {
                try {
                    if (Integer.parseInt(parts[0]) == widgetId) return true;
                } catch (NumberFormatException ignored) {}
            }
        }
        return false;
    }

    static void saveGeometry(SharedPreferences prefs, String widgetIdsKey, int widgetId,
                             int pageIndex, FrameLayout container) {
        if (container == null || !(container.getLayoutParams() instanceof FrameLayout.LayoutParams)) return;
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) container.getLayoutParams();
        prefs.edit().putInt("widget_x_" + widgetId, lp.leftMargin)
                .putInt("widget_y_" + widgetId, lp.topMargin)
                .putInt("widget_w_" + widgetId, lp.width)
                .putInt("widget_h_" + widgetId, lp.height).apply();

        String stored = prefs.getString(widgetIdsKey, "");
        StringBuilder updated = new StringBuilder();
        boolean found = false;
        for (String entry : stored.split("\\n")) {
            String[] parts = entry.split(":");
            if (parts.length < 2) continue;
            if (parts[0].equals(String.valueOf(widgetId))) {
                entry = widgetId + ":" + pageIndex;
                found = true;
            }
            if (updated.length() > 0) updated.append('\n');
            updated.append(entry);
        }
        if (!found) {
            if (updated.length() > 0) updated.append('\n');
            updated.append(widgetId).append(':').append(pageIndex);
        }
        prefs.edit().putString(widgetIdsKey, updated.toString()).apply();
    }

    static void removeWidget(SharedPreferences prefs, String widgetIdsKey, int widgetId) {
        String stored = prefs.getString(widgetIdsKey, "");
        StringBuilder updated = new StringBuilder();
        for (String entry : stored.split("\\n")) {
            String[] parts = entry.split(":");
            if (parts.length > 0 && parts[0].equals(String.valueOf(widgetId))) continue;
            if (!entry.trim().isEmpty()) {
                if (updated.length() > 0) updated.append('\n');
                updated.append(entry);
            }
        }
        prefs.edit().putString(widgetIdsKey, updated.toString())
                .remove("widget_x_" + widgetId).remove("widget_y_" + widgetId)
                .remove("widget_w_" + widgetId).remove("widget_h_" + widgetId).apply();
    }
}
