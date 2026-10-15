package com.dsann.launcher;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

/** Builds the app-drawer view and keeps its adapter wiring in one place. */
final class LauncherDrawerController {
    interface Actions {
        void launchApp(ResolveInfo app);
        void showAppOptions(ResolveInfo app, boolean onHome);
        int getColumns();
    }

    static final class BuiltDrawer {
        final LinearLayout screen;
        final GridView grid;
        final LauncherAppAdapter adapter;
        BuiltDrawer(LinearLayout screen, GridView grid, LauncherAppAdapter adapter) {
            this.screen = screen;
            this.grid = grid;
            this.adapter = adapter;
        }
    }

    private LauncherDrawerController() {}

    static BuiltDrawer build(Activity activity, FrameLayout root, SharedPreferences prefs,
                             PackageManager packageManager, List<ResolveInfo> allApps,
                             List<ResolveInfo> drawerApps, Drawable background, int spacing,
                             String iconSizeKey, String fontSizeKey, String showLabelsKey,
                             Actions actions) {
        LinearLayout screen = new LinearLayout(activity);
        screen.setOrientation(LinearLayout.VERTICAL);
        int side = LauncherUiUtils.dp(activity, 18);
        screen.setPadding(side, side, side, LauncherUiUtils.dp(activity, 8));
        screen.setBackground(background);
        TextView title = LauncherUiUtils.text(activity, "Apps", 28, android.graphics.Color.WHITE, true);
        screen.addView(title, new LinearLayout.LayoutParams(-1, LauncherUiUtils.dp(activity, 48)));

        GridView grid = new GridView(activity);
        grid.setNumColumns(actions.getColumns());
        grid.setVerticalSpacing(LauncherUiUtils.dp(activity, spacing));
        grid.setHorizontalSpacing(LauncherUiUtils.dp(activity, spacing / 2));
        grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        grid.setSelector(android.R.color.transparent);
        LauncherAppAdapter adapter = new LauncherAppAdapter(activity, prefs, packageManager, allApps, drawerApps, null,
                true, iconSizeKey, fontSizeKey, showLabelsKey, new LauncherAppAdapter.Actions() {
                    @Override public void onAppClick(ResolveInfo app) { actions.launchApp(app); }
                    @Override public void onAppLongClick(ResolveInfo app, boolean onHome) {
                        actions.showAppOptions(app, onHome);
                    }
                });
        grid.setAdapter(adapter);
        screen.addView(grid, new LinearLayout.LayoutParams(-1, 0, 1f));
        root.addView(screen, new FrameLayout.LayoutParams(-1, -1));
        return new BuiltDrawer(screen, grid, adapter);
    }
}
