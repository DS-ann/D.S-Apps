package com.dsann.launcher;

import android.app.Activity;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ClipData;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int HOST_ID = 1407;
    private static final String PREFS = "ds_launcher_prefs";
    private static final String KEY_HOME_APPS = "home_apps";
    private static final int COLUMNS = 4;
    private static final int PAGE_CAPACITY = 20;
    private final int BG = Color.rgb(10, 13, 24);
    private final Handler handler = new Handler();
    private PackageManager packageManager;
    private SharedPreferences prefs;
    private AppWidgetManager widgetManager;
    private AppWidgetHost widgetHost;
    private FrameLayout root;
    private LinearLayout homeScreen;
    private LinearLayout pagesStrip;
    private LinearLayout drawerScreen;
    private GridView drawerGrid;
    private TextView clockText;
    private TextView dateText;
    private TextView pageIndicator;
    private AppAdapter drawerAdapter;
    private List<ResolveInfo> allApps = new ArrayList<>();
    private List<ResolveInfo> drawerApps = new ArrayList<>();
    private final List<String> homePackages = new ArrayList<>();
    private final List<List<String>> pages = new ArrayList<>();
    private int currentPage = 0;
    private int extraPages = 0;
    private float gestureDownX, gestureDownY;
    private boolean drawerOpen = false;
    private final Runnable clockUpdater = new Runnable() {
        @Override public void run() {
            updateClock();
            handler.postDelayed(this, 60000L);
        }
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        packageManager = getPackageManager();
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        widgetManager = AppWidgetManager.getInstance(this);
        widgetHost = new AppWidgetHost(this, HOST_ID);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        loadApps();
        restoreHomeApps();
        buildInterface();
    }

    @Override public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            gestureDownX = event.getX();
            gestureDownY = event.getY();
        }
        boolean handled = super.dispatchTouchEvent(event);
        if (event.getAction() == MotionEvent.ACTION_UP) {
            float dx = event.getX() - gestureDownX;
            float dy = event.getY() - gestureDownY;
            if (Math.abs(dy) > dp(65) && Math.abs(dy) > Math.abs(dx)) {
                if (!drawerOpen && dy < 0) { showDrawer(); return true; }
                if (drawerOpen && dy > 0) { showHome(); return true; }
            } else if (!drawerOpen && Math.abs(dx) > dp(65) && Math.abs(dx) > Math.abs(dy)) {
                if (dx < 0) nextPage(); else previousPage();
                return true;
            }
        }
        return handled;
    }

    @Override protected void onResume() {
        super.onResume();
        handler.removeCallbacks(clockUpdater);
        handler.post(clockUpdater);
        loadApps();
        if (!drawerOpen && root != null) renderHomePages(false);
        try { widgetHost.startListening(); } catch (Exception ignored) {}
    }

    @Override protected void onPause() {
        handler.removeCallbacks(clockUpdater);
        try { widgetHost.stopListening(); } catch (Exception ignored) {}
        super.onPause();
    }

    @Override protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private void loadApps() {
        Intent query = new Intent(Intent.ACTION_MAIN, null);
        query.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> result = packageManager.queryIntentActivities(query, 0);
        Collections.sort(result, new Comparator<ResolveInfo>() {
            @Override public int compare(ResolveInfo a, ResolveInfo b) {
                return a.loadLabel(packageManager).toString().compareToIgnoreCase(b.loadLabel(packageManager).toString());
            }
        });
        allApps = result;
        drawerApps = new ArrayList<>(result);
        if (drawerAdapter != null) drawerAdapter.notifyDataSetChanged();
    }

    private void restoreHomeApps() {
        homePackages.clear();
        String saved = prefs.getString(KEY_HOME_APPS, "");
        if (saved != null && !saved.isEmpty()) {
            for (String p : saved.split("\\n")) if (!p.isEmpty() && findApp(p) != null && !homePackages.contains(p)) homePackages.add(p);
        }
        if (homePackages.isEmpty()) {
            // A few familiar shortcuts make the first page useful; all apps remain in the drawer.
            for (ResolveInfo app : allApps) {
                String p = app.activityInfo.packageName;
                if (!p.equals(getPackageName()) && homePackages.size() < 8 && !homePackages.contains(p)) homePackages.add(p);
            }
        }
        rebuildPages();
        persistHomeApps();
    }

    private void persistHomeApps() {
        StringBuilder s = new StringBuilder();
        for (String p : homePackages) { if (s.length() > 0) s.append('\n'); s.append(p); }
        prefs.edit().putString(KEY_HOME_APPS, s.toString()).apply();
    }

    private ResolveInfo findApp(String packageName) {
        for (ResolveInfo app : allApps) if (app.activityInfo.packageName.equals(packageName)) return app;
        return null;
    }

    private void rebuildPages() {
        pages.clear();
        for (int i = 0; i < homePackages.size(); i += PAGE_CAPACITY)
            pages.add(new ArrayList<>(homePackages.subList(i, Math.min(i + PAGE_CAPACITY, homePackages.size()))));
        if (pages.isEmpty()) pages.add(new ArrayList<String>());
        while (extraPages > 0 && pages.size() < ((homePackages.size() + PAGE_CAPACITY - 1) / PAGE_CAPACITY) + extraPages) pages.add(new ArrayList<String>());
        if (currentPage >= pages.size()) currentPage = pages.size() - 1;
    }

    private void buildInterface() {
        root = new FrameLayout(this);
        root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(11, 22, 51), Color.rgb(20, 42, 88), BG}));
        homeScreen = new LinearLayout(this);
        homeScreen.setOrientation(LinearLayout.VERTICAL);
        homeScreen.setPadding(dp(20), dp(14), dp(20), dp(8));
        root.addView(homeScreen, new FrameLayout.LayoutParams(-1, -1));
        buildHomeChrome();
        buildDrawer();
        setContentView(root);
        renderHomePages(false);
        showHome();
    }

    private void buildHomeChrome() {
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView brand = text("D.Sannigrahi", 12, Color.rgb(210, 220, 255), true);
        brand.setTypeface(Typeface.create("sans-serif-medium", Typeface.ITALIC));
        brand.setLetterSpacing(0.035f);
        header.addView(brand, new LinearLayout.LayoutParams(0, dp(32), 1f));
        TextView edit = text("＋", 26, Color.WHITE, false);
        edit.setGravity(Gravity.CENTER);
        edit.setOnClickListener(v -> showAddApps());
        header.addView(edit, new LinearLayout.LayoutParams(dp(42), dp(38)));
        homeScreen.addView(header);

        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setPadding(dp(18), dp(12), dp(18), dp(12));
        hero.setBackground(roundRect(new int[]{Color.rgb(49, 73, 143), Color.rgb(91, 65, 157)}, dp(22)));
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, -2);
        hp.topMargin = dp(8);
        homeScreen.addView(hero, hp);
        clockText = text("--:--", 40, Color.WHITE, true);
        clockText.setIncludeFontPadding(false);
        hero.addView(clockText);
        dateText = text("", 13, Color.rgb(226, 228, 250), false);
        LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(-1, -2);
        dp.topMargin = dp(3);
        hero.addView(dateText, dp);

        pagesStrip = new LinearLayout(this);
        pagesStrip.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(-1, 0, 1f);
        pp.topMargin = dp(12);
        homeScreen.addView(pagesStrip, pp);

        pageIndicator = text("", 11, Color.rgb(210, 220, 255), false);
        pageIndicator.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(-1, dp(28));
        homeScreen.addView(pageIndicator, ip);
        TextView drawerHint = text("Swipe up for all apps", 11, Color.rgb(180, 194, 224), false);
        drawerHint.setGravity(Gravity.CENTER);
        homeScreen.addView(drawerHint, new LinearLayout.LayoutParams(-1, dp(28)));
    }

    private void buildDrawer() {
        drawerScreen = new LinearLayout(this);
        drawerScreen.setOrientation(LinearLayout.VERTICAL);
        drawerScreen.setPadding(dp(18), dp(18), dp(18), dp(8));
        drawerScreen.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(15, 20, 36), Color.rgb(10, 13, 24)}));
        TextView title = text("Apps", 28, Color.WHITE, true);
        drawerScreen.addView(title, new LinearLayout.LayoutParams(-1, dp(48)));
        drawerGrid = new GridView(this);
        drawerGrid.setNumColumns(COLUMNS);
        drawerGrid.setVerticalSpacing(dp(8));
        drawerGrid.setHorizontalSpacing(dp(4));
        drawerGrid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        drawerGrid.setSelector(android.R.color.transparent);
        drawerAdapter = new AppAdapter(true);
        drawerGrid.setAdapter(drawerAdapter);
        drawerScreen.addView(drawerGrid, new LinearLayout.LayoutParams(-1, 0, 1f));
        drawerGrid.setOnItemLongClickListener((parent, view, position, id) -> {
            ResolveInfo app = drawerApps.get(position);
            addToHome(app.activityInfo.packageName);
            Toast.makeText(this, "Added to Home", Toast.LENGTH_SHORT).show();
            showHome();
            return true;
        });
        root.addView(drawerScreen, new FrameLayout.LayoutParams(-1, -1));
    }

    private void renderHomePages(boolean animate) {
        if (pagesStrip == null) return;
        pagesStrip.removeAllViews();
        rebuildPages();
        for (int pageIndex = 0; pageIndex < pages.size(); pageIndex++) {
            final int idx = pageIndex;
            GridView grid = new GridView(this);
            grid.setNumColumns(COLUMNS);
            grid.setVerticalSpacing(dp(8));
            grid.setHorizontalSpacing(dp(4));
            grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
            grid.setSelector(android.R.color.transparent);
            grid.setAdapter(new AppAdapter(false, pages.get(pageIndex)));
            grid.setOnItemLongClickListener((parent, view, position, id) -> {
                ResolveInfo app = findApp(pages.get(idx).get(position));
                if (app != null) {
                    new android.app.AlertDialog.Builder(this)
                            .setTitle(app.loadLabel(packageManager))
                            .setItems(new String[]{"Move earlier", "Move later", "Remove from Home", "Open app"}, (dialog, which) -> {
                                int itemIndex = homePackages.indexOf(app.activityInfo.packageName);
                                if (which == 0 && itemIndex > 0) {
                                    Collections.swap(homePackages, itemIndex, itemIndex - 1);
                                    persistHomeApps(); rebuildPages(); renderHomePages(true);
                                } else if (which == 1 && itemIndex >= 0 && itemIndex < homePackages.size() - 1) {
                                    Collections.swap(homePackages, itemIndex, itemIndex + 1);
                                    persistHomeApps(); rebuildPages(); renderHomePages(true);
                                } else if (which == 2) {
                                    homePackages.remove(app.activityInfo.packageName);
                                    persistHomeApps(); rebuildPages(); renderHomePages(false);
                                } else if (which == 3) launchApp(app);
                            }).show();
                }
                return true;
            });
            pagesStrip.addView(grid, new LinearLayout.LayoutParams(-1, -1));
        }
        pageIndicator.setText((currentPage + 1) + "  /  " + pages.size());
        if (currentPage > 0) pagesStrip.setTranslationX(-currentPage * getResources().getDisplayMetrics().widthPixels);
        else pagesStrip.setTranslationX(0);
        if (animate) {
            pagesStrip.setRotationY(-12f);
            pagesStrip.animate().rotationY(0f).setDuration(220).start();
        } else pagesStrip.setRotationY(0f);
    }

    private void nextPage() {
        if (currentPage + 1 < pages.size()) {
            currentPage++;
            renderHomePages(true);
            return;
        }
        // A new page is created only when the current page has reached capacity.
        if (pages.get(currentPage).size() >= PAGE_CAPACITY) {
            extraPages++;
            currentPage++;
            renderHomePages(true);
        }
    }

    private void previousPage() {
        if (currentPage > 0) { currentPage--; renderHomePages(true); }
    }

    private void showHome() {
        drawerOpen = false;
        drawerScreen.setVisibility(View.GONE);
        homeScreen.setVisibility(View.VISIBLE);
        homeScreen.animate().alpha(1f).setDuration(130).start();
    }

    private void showDrawer() {
        drawerOpen = true;
        drawerScreen.setAlpha(0f);
        drawerScreen.setVisibility(View.VISIBLE);
        homeScreen.setVisibility(View.GONE);
        drawerScreen.animate().alpha(1f).setDuration(180).start();
        loadApps();
    }

    private void addToHome(String packageName) {
        if (!homePackages.contains(packageName)) {
            homePackages.add(packageName);
            persistHomeApps();
            rebuildPages();
            currentPage = pages.size() - 1;
            renderHomePages(true);
        }
    }

    private void showAddApps() {
        final List<ResolveInfo> available = new ArrayList<>();
        for (ResolveInfo app : allApps) if (!homePackages.contains(app.activityInfo.packageName)) available.add(app);
        if (available.isEmpty()) {
            Toast.makeText(this, "All apps are already on Home", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] labels = new String[available.size()];
        for (int i = 0; i < available.size(); i++) labels[i] = available.get(i).loadLabel(packageManager).toString();
        new android.app.AlertDialog.Builder(this).setTitle("Add to Home").setItems(labels, (dialog, which) -> {
            addToHome(available.get(which).activityInfo.packageName);
        }).show();
    }

    private void launchApp(ResolveInfo app) {
        Intent intent = packageManager.getLaunchIntentForPackage(app.activityInfo.packageName);
        if (intent != null) {
            try { startActivity(intent); } catch (Exception e) { Toast.makeText(this, "Unable to open app", Toast.LENGTH_SHORT).show(); }
        }
    }

    private void updateClock() {
        if (clockText == null || dateText == null) return;
        Date now = new Date();
        clockText.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(now));
        dateText.setText(new SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(now));
    }

    private TextView text(String value, float size, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(size);
        v.setTextColor(color);
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return v;
    }

    private GradientDrawable roundRect(int[] colors, int radius) {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, colors);
        d.setCornerRadius(dp(radius));
        return d;
    }

    private int dp(float value) { return (int)(value * getResources().getDisplayMetrics().density + 0.5f); }

    private class AppAdapter extends BaseAdapter {
        private final boolean drawer;
        private final List<String> fixedPackages;
        AppAdapter(boolean drawer) { this(drawer, null); }
        AppAdapter(boolean drawer, List<String> fixedPackages) { this.drawer = drawer; this.fixedPackages = fixedPackages; }
        private List<ResolveInfo> items() {
            if (drawer) return drawerApps;
            List<ResolveInfo> out = new ArrayList<>();
            if (fixedPackages != null) for (String p : fixedPackages) { ResolveInfo app = findApp(p); if (app != null) out.add(app); }
            return out;
        }
        @Override public int getCount() { return items().size(); }
        @Override public Object getItem(int position) { return items().get(position); }
        @Override public long getItemId(int position) { return position; }
        @Override public View getView(int position, View recycled, ViewGroup parent) {
            LinearLayout cell;
            ImageView icon;
            TextView label;
            if (recycled instanceof LinearLayout) {
                cell = (LinearLayout) recycled;
                icon = (ImageView) cell.getChildAt(0);
                label = (TextView) cell.getChildAt(1);
            } else {
                cell = new LinearLayout(MainActivity.this);
                cell.setOrientation(LinearLayout.VERTICAL);
                cell.setGravity(Gravity.CENTER);
                cell.setPadding(dp(2), dp(7), dp(2), dp(7));
                icon = new ImageView(MainActivity.this);
                icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
                LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(dp(48), dp(48));
                ip.gravity = Gravity.CENTER;
                cell.addView(icon, ip);
                label = text("", 10, Color.rgb(236, 239, 249), false);
                label.setGravity(Gravity.CENTER);
                label.setMaxLines(2);
                label.setEllipsize(android.text.TextUtils.TruncateAt.END);
                cell.addView(label, new LinearLayout.LayoutParams(-1, dp(32)));
            }
            ResolveInfo app = items().get(position);
            icon.setImageDrawable(app.loadIcon(packageManager));
            label.setText(app.loadLabel(packageManager));
            cell.setOnClickListener(v -> launchApp(app));
            return cell;
        }
    }
}
