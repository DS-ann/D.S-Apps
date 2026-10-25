package com.dsann.launcher;

import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.util.SizeF;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.util.Collections;

/** Shared widget persistence operations, kept out of the activity's view code. */
final class WidgetController {
    private WidgetController() {}

    private static boolean isPortraitConfiguration() {
        try {
            return Resources.getSystem().getConfiguration().orientation
                    == Configuration.ORIENTATION_PORTRAIT;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    static void updateWidgetSize(AppWidgetManager manager, int widgetId, AppWidgetHostView hostView,
                                 float density, int widthPx, int heightPx) {
        if (hostView == null) return;
        int widthDp = Math.max(1, Math.round(widthPx / Math.max(0.1f, density)));
        int heightDp = Math.max(1, Math.round(heightPx / Math.max(0.1f, density)));
        // The host-view size API already updates the widget options. Sending equivalent
        // data through both AppWidgetManager and AppWidgetHostView duplicated provider
        // callbacks at release, which could briefly select a stale/smaller layout.
        try {
            if (Build.VERSION.SDK_INT >= 31) {
                hostView.updateAppWidgetSize(new Bundle(),
                        Collections.singletonList(new SizeF(widthDp, heightDp)));
            } else {
                Bundle options = new Bundle();
                options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, widthDp);
                options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, widthDp);
                options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, heightDp);
                options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, heightDp);
                hostView.updateAppWidgetSize(options, widthDp, heightDp, widthDp, heightDp);
            }
        } catch (RuntimeException ignored) {
            // A provider may reject an unusual size; keep the launcher's selected frame intact.
        }
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
        // Commit widget dimensions and the host-ID registry in one transaction. Two
        // separate apply() calls left a crash/update window where geometry reached disk
        // but the widget ID was lost, making the widget impossible to restore next launch.
        String stored = prefs.getString(widgetIdsKey, "");
        StringBuilder updated = new StringBuilder();
        boolean found = false;
        if (stored != null && !stored.isEmpty()) {
            for (String entry : stored.split("\\n")) {
                String[] parts = entry.split(":");
                if (parts.length < 2) continue;
                if (parts[0].trim().equals(String.valueOf(widgetId))) {
                    entry = widgetId + ":" + pageIndex;
                    found = true;
                }
                if (updated.length() > 0) updated.append('\n');
                updated.append(entry);
            }
        }
        if (!found) {
            if (updated.length() > 0) updated.append('\n');
            updated.append(widgetId).append(':').append(pageIndex);
        }
        SharedPreferences.Editor editor = prefs.edit()
                .putInt("widget_x_" + widgetId, lp.leftMargin)
                .putInt("widget_y_" + widgetId, lp.topMargin)
                .putInt("widget_w_" + widgetId, lp.width)
                .putInt("widget_h_" + widgetId, lp.height)
                .putString(widgetIdsKey, updated.toString());
        if (isPortraitConfiguration()) {
            editor.putString("__portrait_workspace_geometry__launcher_widget:" + widgetId,
                    pageIndex + "," + lp.leftMargin + "," + lp.topMargin + ","
                            + lp.width + "," + lp.height + "," + container.getScaleX() + ","
                            + container.getScaleY() + "," + container.getTranslationX() + ","
                            + container.getTranslationY() + "," + container.getRotationX() + ","
                            + container.getRotationY());
        }
        editor.commit();
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
                .remove("widget_w_" + widgetId).remove("widget_h_" + widgetId)
                .remove("__portrait_workspace_geometry__launcher_widget:" + widgetId).commit();
    }
}
