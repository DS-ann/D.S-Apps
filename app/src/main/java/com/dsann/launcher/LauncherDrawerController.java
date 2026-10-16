package com.dsann.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
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
        TextView title = LauncherUiUtils.text(activity, "My Apps", 28,
                android.graphics.Color.WHITE, true);
        title.setGravity(Gravity.CENTER);
        title.setTypeface(android.graphics.Typeface.create("cursive", android.graphics.Typeface.NORMAL));
        title.setLetterSpacing(0.015f);
        LinearLayout.LayoutParams titleParams =
                new LinearLayout.LayoutParams(-1, LauncherUiUtils.dp(activity, 48));
        titleParams.bottomMargin = LauncherUiUtils.dp(activity, 4);
        screen.addView(title, titleParams);

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

        LinearLayout searchRow = new LinearLayout(activity);
        searchRow.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams searchRowParams =
                new LinearLayout.LayoutParams(-1, LauncherUiUtils.dp(activity, 42));
        searchRowParams.topMargin = LauncherUiUtils.dp(activity, 2);
        TextView searchButton = LauncherUiUtils.text(activity, "⌕", 27,
                android.graphics.Color.WHITE, true);
        searchButton.setGravity(Gravity.CENTER);
        searchButton.setContentDescription("Search apps");
        searchButton.setBackgroundResource(android.R.drawable.list_selector_background);
        searchButton.setPadding(LauncherUiUtils.dp(activity, 10), 0,
                LauncherUiUtils.dp(activity, 10), 0);
        searchRow.addView(searchButton, new LinearLayout.LayoutParams(
                LauncherUiUtils.dp(activity, 44), -1));
        screen.addView(searchRow, searchRowParams);

        searchButton.setOnClickListener(v -> {
            final List<ResolveInfo> originalApps = new java.util.ArrayList<>(drawerApps);
            EditText queryInput = new EditText(activity);
            queryInput.setSingleLine(true);
            queryInput.setHint("Search apps");
            queryInput.setTextSize(16);
            queryInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
            AlertDialog dialog = new AlertDialog.Builder(activity)
                    .setTitle("Search apps")
                    .setView(queryInput)
                    .setNegativeButton("Close", (d, which) -> d.dismiss())
                    .create();
            queryInput.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String needle = s.toString().trim().toLowerCase(java.util.Locale.ROOT);
                    drawerApps.clear();
                    if (needle.isEmpty()) {
                        drawerApps.addAll(originalApps);
                    } else {
                        for (ResolveInfo app : originalApps) {
                            String label;
                            try {
                                label = app.loadLabel(packageManager).toString();
                            } catch (RuntimeException ignored) {
                                label = "";
                            }
                            if (label.toLowerCase(java.util.Locale.ROOT).contains(needle)) {
                                drawerApps.add(app);
                            }
                        }
                    }
                    adapter.notifyDataSetChanged();
                    grid.setSelection(0);
                }
                @Override public void afterTextChanged(Editable s) {}
            });
            dialog.setOnDismissListener(d -> {
                drawerApps.clear();
                drawerApps.addAll(originalApps);
                adapter.notifyDataSetChanged();
            });
            dialog.show();
            queryInput.requestFocus();
        });

        root.addView(screen, new FrameLayout.LayoutParams(-1, -1));
        return new BuiltDrawer(screen, grid, adapter);
    }
}
