package com.dsann.launcher;

import android.app.Activity;
import android.app.Dialog;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Picker for real app widgets and legacy CREATE_SHORTCUT activities (such as Contacts Direct Dial). */
final class WidgetPickerDialog {
    interface Listener {
        void onWidgetSelected(AppWidgetProviderInfo info);
        void onShortcutSelected(ResolveInfo shortcutActivity);
    }

    private static final class Entry {
        final AppWidgetProviderInfo widget;
        final ResolveInfo shortcut;
        final String label;
        Entry(AppWidgetProviderInfo widget, ResolveInfo shortcut, String label) {
            this.widget = widget; this.shortcut = shortcut; this.label = label;
        }
    }

    private final Activity activity;
    private final AppWidgetManager widgetManager;
    private final PackageManager packageManager;
    private final int accentColor;
    private final Listener listener;

    WidgetPickerDialog(Activity activity, AppWidgetManager widgetManager,
            PackageManager packageManager, int accentColor, Listener listener) {
        this.activity = activity;
        this.widgetManager = widgetManager;
        this.packageManager = packageManager;
        this.accentColor = accentColor == Color.TRANSPARENT
                ? Color.rgb(98, 211, 255) : accentColor;
        this.listener = listener;
    }

    private int tint(int color, int alpha) {
        return Color.argb(Math.max(0, Math.min(255, alpha)),
                Color.red(color), Color.green(color), Color.blue(color));
    }

    private int darken(int color) {
        return Color.rgb(Color.red(color) * 3 / 4,
                Color.green(color) * 3 / 4, Color.blue(color) * 3 / 4);
    }

    private android.graphics.drawable.GradientDrawable roundRect(int[] colors, float radiusDp) {
        android.graphics.drawable.GradientDrawable drawable;
        if (colors == null || colors.length == 0) {
            drawable = new android.graphics.drawable.GradientDrawable();
            drawable.setColor(Color.TRANSPARENT);
        } else if (colors.length == 1) {
            drawable = new android.graphics.drawable.GradientDrawable();
            drawable.setColor(colors[0]);
        } else {
            drawable = new android.graphics.drawable.GradientDrawable(
                    android.graphics.drawable.GradientDrawable.Orientation.TL_BR, colors);
        }
        drawable.setCornerRadius(LauncherUiUtils.dp(activity, radiusDp));
        return drawable;
    }

    private android.graphics.drawable.GradientDrawable controlBackground() {
        return roundRect(new int[]{Color.rgb(35, 43, 65), Color.rgb(22, 28, 45)}, 14);
    }

    private TextView text(String value, float size, int color, boolean bold) {
        return LauncherUiUtils.text(activity, value, size, color, bold);
    }

    private LinearLayout dialogPanel() {
        LinearLayout panel = new LinearLayout(activity);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(LauncherUiUtils.dp(activity, 16), LauncherUiUtils.dp(activity, 14),
                LauncherUiUtils.dp(activity, 16), LauncherUiUtils.dp(activity, 14));
        panel.setBackground(roundRect(new int[]{Color.rgb(28, 33, 57),
                Color.rgb(10, 14, 27)}, 26));
        return panel;
    }

    private void addHeader(LinearLayout panel, String title, String subtitle) {
        LinearLayout header = new LinearLayout(activity);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        View rail = new View(activity);
        rail.setBackground(roundRect(new int[]{accentColor}, 3));
        header.addView(rail, new LinearLayout.LayoutParams(
                LauncherUiUtils.dp(activity, 4), LauncherUiUtils.dp(activity, 44)));
        LinearLayout copy = new LinearLayout(activity);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.setGravity(Gravity.CENTER_VERTICAL);
        TextView heading = text(title, 20, Color.WHITE, true);
        copy.addView(heading, new LinearLayout.LayoutParams(-1, -2));
        TextView detail = text(subtitle, 12.5f, Color.rgb(190, 202, 226), false);
        detail.setMaxLines(2);
        detail.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(-1, -2);
        detailParams.topMargin = LauncherUiUtils.dp(activity, 3);
        copy.addView(detail, detailParams);
        LinearLayout.LayoutParams copyParams = new LinearLayout.LayoutParams(0, -2, 1f);
        copyParams.leftMargin = LauncherUiUtils.dp(activity, 11);
        header.addView(copy, copyParams);
        LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(-1, -2);
        headerParams.bottomMargin = LauncherUiUtils.dp(activity, 12);
        panel.addView(header, headerParams);
        View line = new View(activity);
        line.setBackground(roundRect(new int[]{accentColor}, 2));
        LinearLayout.LayoutParams lineParams = new LinearLayout.LayoutParams(-1,
                LauncherUiUtils.dp(activity, 2));
        lineParams.bottomMargin = LauncherUiUtils.dp(activity, 12);
        panel.addView(line, lineParams);
    }

    private TextView studioButton(String label, boolean primary) {
        TextView button = text(label, 14,
                primary ? Color.rgb(10, 15, 27) : Color.rgb(211, 220, 240), true);
        button.setGravity(Gravity.CENTER);
        button.setPadding(LauncherUiUtils.dp(activity, 10), LauncherUiUtils.dp(activity, 8),
                LauncherUiUtils.dp(activity, 10), LauncherUiUtils.dp(activity, 8));
        button.setMinHeight(LauncherUiUtils.dp(activity, 44));
        button.setBackground(primary
                ? roundRect(new int[]{accentColor, darken(accentColor)}, 13)
                : controlBackground());
        return button;
    }

    void show() {
        List<Entry> entries = new ArrayList<>();
        List<AppWidgetProviderInfo> providers = widgetManager.getInstalledProviders();
        if (providers != null) for (AppWidgetProviderInfo info : providers) {
            if (info == null || info.provider == null) continue;
            String label;
            try { label = String.valueOf(info.loadLabel(packageManager)); }
            catch (Exception e) { label = info.provider.getPackageName(); }
            entries.add(new Entry(info, null, label));
        }

        // Some launchers expose legacy CREATE_SHORTCUT activities beside real widgets.
        Intent createShortcut = new Intent(Intent.ACTION_CREATE_SHORTCUT);
        List<ResolveInfo> shortcutActivities;
        try { shortcutActivities = packageManager.queryIntentActivities(createShortcut, 0); }
        catch (Exception e) { shortcutActivities = Collections.emptyList(); }
        if (shortcutActivities != null) for (ResolveInfo shortcut : shortcutActivities) {
            if (shortcut == null || shortcut.activityInfo == null) continue;
            String label;
            try { label = String.valueOf(shortcut.loadLabel(packageManager)); }
            catch (Exception e) { label = shortcut.activityInfo.packageName; }
            entries.add(new Entry(null, shortcut, label));
        }
        Collections.sort(entries, new Comparator<Entry>() {
            @Override public int compare(Entry a, Entry b) {
                return a.label.compareToIgnoreCase(b.label);
            }
        });
        if (entries.isEmpty()) {
            Toast.makeText(activity, "No installed widgets or shortcuts are available",
                    Toast.LENGTH_LONG).show();
            return;
        }

        Dialog dialog = new Dialog(activity);
        LinearLayout panel = dialogPanel();
        addHeader(panel, "Choose a widget or shortcut",
                "Widgets and quick shortcuts available on this device");

        ScrollView scroll = new ScrollView(activity);
        scroll.setVerticalScrollBarEnabled(true);
        scroll.setFillViewport(false);
        LinearLayout list = new LinearLayout(activity);
        list.setOrientation(LinearLayout.VERTICAL);

        for (Entry entry : entries) {
            LinearLayout row = new LinearLayout(activity);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(LauncherUiUtils.dp(activity, 9), LauncherUiUtils.dp(activity, 7),
                    LauncherUiUtils.dp(activity, 10), LauncherUiUtils.dp(activity, 7));
            row.setBackground(controlBackground());
            View rail = new View(activity);
            rail.setBackground(roundRect(new int[]{accentColor}, 2));
            row.addView(rail, new LinearLayout.LayoutParams(
                    LauncherUiUtils.dp(activity, 3), LauncherUiUtils.dp(activity, 42)));

            ImageView preview = new ImageView(activity);
            preview.setScaleType(ImageView.ScaleType.FIT_CENTER);
            preview.setBackground(roundRect(new int[]{tint(accentColor, 48),
                    tint(accentColor, 20)}, 12));
            preview.setPadding(LauncherUiUtils.dp(activity, 4), LauncherUiUtils.dp(activity, 4),
                    LauncherUiUtils.dp(activity, 4), LauncherUiUtils.dp(activity, 4));
            Drawable image = null;
            if (entry.widget != null) {
                try {
                    if (entry.widget.previewImage != 0) {
                        image = entry.widget.loadPreviewImage(activity,
                                activity.getResources().getDisplayMetrics().densityDpi);
                    }
                } catch (Exception ignored) {}
                if (image == null) try {
                    image = entry.widget.loadIcon(activity,
                            activity.getResources().getDisplayMetrics().densityDpi);
                } catch (Exception ignored) {}
            } else {
                try { image = entry.shortcut.loadIcon(packageManager); }
                catch (Exception ignored) {}
            }
            if (image != null) preview.setImageDrawable(image);
            LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(
                    LauncherUiUtils.dp(activity, 58), LauncherUiUtils.dp(activity, 58));
            previewParams.leftMargin = LauncherUiUtils.dp(activity, 8);
            row.addView(preview, previewParams);

            LinearLayout details = new LinearLayout(activity);
            details.setOrientation(LinearLayout.VERTICAL);
            details.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams detailsParams = new LinearLayout.LayoutParams(0, -2, 1f);
            detailsParams.leftMargin = LauncherUiUtils.dp(activity, 11);
            detailsParams.rightMargin = LauncherUiUtils.dp(activity, 6);
            String packageName = entry.widget != null ? entry.widget.provider.getPackageName()
                    : entry.shortcut.activityInfo.packageName;
            TextView name = text(entry.label, 14.5f, Color.rgb(246, 248, 255), true);
            name.setMaxLines(2);
            name.setEllipsize(android.text.TextUtils.TruncateAt.END);
            details.addView(name, new LinearLayout.LayoutParams(-1, -2));
            TextView provider = text((entry.widget == null ? "Shortcut · " : "Widget · ")
                    + packageName, 10.5f, Color.rgb(158, 171, 202), false);
            provider.setMaxLines(1);
            provider.setSingleLine(true);
            provider.setEllipsize(android.text.TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams providerParams = new LinearLayout.LayoutParams(-1, -2);
            providerParams.topMargin = LauncherUiUtils.dp(activity, 3);
            details.addView(provider, providerParams);
            row.addView(details, detailsParams);

            TextView add = text("+", 22, accentColor, true);
            add.setGravity(Gravity.CENTER);
            add.setBackground(roundRect(new int[]{tint(accentColor, 54),
                    tint(accentColor, 22)}, 11));
            row.addView(add, new LinearLayout.LayoutParams(
                    LauncherUiUtils.dp(activity, 34), LauncherUiUtils.dp(activity, 38)));

            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2);
            rowParams.bottomMargin = LauncherUiUtils.dp(activity, 7);
            list.addView(row, rowParams);
            row.setOnClickListener(v -> {
                dialog.dismiss();
                if (entry.widget != null) listener.onWidgetSelected(entry.widget);
                else listener.onShortcutSelected(entry.shortcut);
            });
        }

        scroll.addView(list, new ScrollView.LayoutParams(-1, -2));
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(-1, 0, 1f);
        panel.addView(scroll, scrollParams);

        TextView cancel = studioButton("Cancel", false);
        LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(-1, -2);
        cancelParams.topMargin = LauncherUiUtils.dp(activity, 10);
        panel.addView(cancel, cancelParams);
        cancel.setOnClickListener(v -> dialog.dismiss());

        dialog.setContentView(panel);
        android.view.Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            android.view.WindowManager.LayoutParams params = window.getAttributes();
            params.dimAmount = 0.58f;
            window.setAttributes(params);
            window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        dialog.show();
        window = dialog.getWindow();
        if (window != null) {
            int screenHeight = activity.getResources().getDisplayMetrics().heightPixels;
            int desiredHeight = Math.min(screenHeight - LauncherUiUtils.dp(activity, 56),
                    LauncherUiUtils.dp(activity, 610));
            window.setLayout(activity.getResources().getDisplayMetrics().widthPixels
                    - LauncherUiUtils.dp(activity, 32), desiredHeight);
        }
        panel.setAlpha(0f);
        panel.setScaleX(0.97f);
        panel.setScaleY(0.97f);
        panel.animate().alpha(1f).scaleX(1f).scaleY(1f)
                .setDuration(LauncherMotion.duration(activity, 180L)).start();
    }
}
