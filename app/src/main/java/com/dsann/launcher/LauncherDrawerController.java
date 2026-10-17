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
        LinearLayout headerRow = new LinearLayout(activity);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams headerParams =
                new LinearLayout.LayoutParams(-1, LauncherUiUtils.dp(activity, 48));
        headerParams.bottomMargin = LauncherUiUtils.dp(activity, 4);
        TextView title = LauncherUiUtils.text(activity, "My Apps", 28,
                android.graphics.Color.WHITE, true);
        title.setGravity(Gravity.CENTER);
        title.setTypeface(android.graphics.Typeface.create("cursive", android.graphics.Typeface.NORMAL));
        title.setLetterSpacing(0.015f);
        headerRow.addView(title, new LinearLayout.LayoutParams(0, -1, 1f));
        final TextView searchButton = LauncherUiUtils.text(activity, "⌕", 27,
                android.graphics.Color.WHITE, true);
        searchButton.setGravity(Gravity.CENTER);
        searchButton.setContentDescription("Search apps");
        searchButton.setBackgroundResource(android.R.drawable.list_selector_background);
        searchButton.setPadding(LauncherUiUtils.dp(activity, 10), 0,
                LauncherUiUtils.dp(activity, 10), 0);
        headerRow.addView(searchButton, new LinearLayout.LayoutParams(
                LauncherUiUtils.dp(activity, 44), -1));
        screen.addView(headerRow, headerParams);

        GridView grid = new GridView(activity);
        grid.setNumColumns(actions.getColumns());
        grid.setVerticalSpacing(LauncherUiUtils.dp(activity, spacing));
        grid.setHorizontalSpacing(LauncherUiUtils.dp(activity, spacing / 2));
        grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        grid.setSelector(android.R.color.transparent);
        final Runnable[] closeSearchRef = new Runnable[1];
        LauncherAppAdapter adapter = new LauncherAppAdapter(activity, prefs, packageManager, allApps, drawerApps, null,
                true, iconSizeKey, fontSizeKey, showLabelsKey, new LauncherAppAdapter.Actions() {
                    @Override public void onAppClick(ResolveInfo app) { if (closeSearchRef[0] != null) closeSearchRef[0].run(); actions.launchApp(app); }
                    @Override public void onAppLongClick(ResolveInfo app, boolean onHome) {
                        actions.showAppOptions(app, onHome);
                    }
                });
        grid.setAdapter(adapter);
        screen.addView(grid, new LinearLayout.LayoutParams(-1, 0, 1f));

        // Keep the inline query field immediately under the header so it remains visible
        // above the keyboard on Android 6 even when adjustResize is ineffective.
        LinearLayout searchRow = new LinearLayout(activity);
        searchRow.setGravity(Gravity.CENTER_VERTICAL);
        searchRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams searchRowParams =
                new LinearLayout.LayoutParams(-1, LauncherUiUtils.dp(activity, 42));
        searchRowParams.bottomMargin = LauncherUiUtils.dp(activity, 2);
        searchRow.setVisibility(View.GONE);
        screen.addView(searchRow, 1, searchRowParams);

        final List<ResolveInfo> originalApps = new java.util.ArrayList<>(drawerApps);
        final EditText[] activeQuery = new EditText[1];

        Runnable restoreApps = () -> {
            drawerApps.clear();
            drawerApps.addAll(originalApps);
            adapter.notifyDataSetChanged();
            grid.setSelection(0);
        };
        Runnable closeSearch = () -> {
            android.view.inputmethod.InputMethodManager imm =
                    (android.view.inputmethod.InputMethodManager) activity.getSystemService(
                            Activity.INPUT_METHOD_SERVICE);
            if (imm != null && activeQuery[0] != null) {
                imm.hideSoftInputFromWindow(activeQuery[0].getWindowToken(), 0);
            }
            activeQuery[0] = null;
            searchRow.removeAllViews();
            searchRow.setVisibility(View.GONE);
            restoreApps.run();
        };
        closeSearchRef[0] = closeSearch;

        searchButton.setOnClickListener(v -> {
            if (activeQuery[0] != null) return;
            searchRow.setVisibility(View.VISIBLE);
            searchRow.removeAllViews();
            EditText queryInput = new EditText(activity);
            activeQuery[0] = queryInput;
            queryInput.setSingleLine(true);
            queryInput.setHint("Search apps");
            queryInput.setTextSize(16);
            queryInput.setTextColor(android.graphics.Color.WHITE);
            queryInput.setHintTextColor(android.graphics.Color.LTGRAY);
            queryInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                    | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            queryInput.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);
            queryInput.setPadding(LauncherUiUtils.dp(activity, 12), 0,
                    LauncherUiUtils.dp(activity, 8), 0);
            queryInput.setBackground(LauncherUiUtils.roundRect(activity,
                    new int[]{android.graphics.Color.rgb(35, 42, 61), android.graphics.Color.rgb(24, 30, 46)},
                    LauncherUiUtils.dp(activity, 12)));
            searchRow.addView(queryInput, new LinearLayout.LayoutParams(0, -1, 1f));

            TextView closeButton = LauncherUiUtils.text(activity, "×", 25,
                    android.graphics.Color.WHITE, true);
            closeButton.setGravity(Gravity.CENTER);
            closeButton.setContentDescription("Close app search");
            closeButton.setBackgroundResource(android.R.drawable.list_selector_background);
            searchRow.addView(closeButton, new LinearLayout.LayoutParams(
                    LauncherUiUtils.dp(activity, 42), -1));
            closeButton.setOnClickListener(closeView -> closeSearch.run());

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
            queryInput.setOnEditorActionListener((field, actionId, event) -> {
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH
                        && !drawerApps.isEmpty()) {
                    ResolveInfo match = drawerApps.get(0);
                    closeSearch.run();
                    actions.launchApp(match);
                    return true;
                }
                return false;
            });
            queryInput.requestFocus();
            queryInput.post(() -> {
                android.view.inputmethod.InputMethodManager imm =
                        (android.view.inputmethod.InputMethodManager) activity.getSystemService(
                                Activity.INPUT_METHOD_SERVICE);
                if (imm != null) imm.showSoftInput(queryInput,
                        android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
            });
        });
        root.addView(screen, new FrameLayout.LayoutParams(-1, -1));
        return new BuiltDrawer(screen, grid, adapter);
    }
}
