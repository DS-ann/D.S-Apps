package com.dsann.launcher;

import android.app.Activity;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
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
    private static final String KEY_COLUMNS = "home_columns";
    private static final String KEY_SHOW_DATE = "show_date";
    private static final String KEY_ICON_SIZE = "icon_size";
    private static final String KEY_WALLPAPER_URI = "wallpaper_uri";
    private static final int REQUEST_WALLPAPER = 7301;
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
    private FrameLayout pageViewport;
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
    private float gestureDownX, gestureDownY;
    private float longPressDownX, longPressDownY;
    private boolean longPressTriggered = false;
    private final Runnable homeLongPress = new Runnable() {
        @Override public void run() {
            if (!drawerOpen && root != null) {
                longPressTriggered = true;
                showCustomizationMenu();
            }
        }
    };
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
            longPressDownX = event.getX();
            longPressDownY = event.getY();
            longPressTriggered = false;
            handler.removeCallbacks(homeLongPress);
            if (!drawerOpen && !touchStartedOnAppCell(event.getRawX(), event.getRawY())) handler.postDelayed(homeLongPress, 2000L);
            return super.dispatchTouchEvent(event);
        }
        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            if (Math.abs(event.getX() - longPressDownX) > dp(12) || Math.abs(event.getY() - longPressDownY) > dp(12)) handler.removeCallbacks(homeLongPress);
        }
        if (event.getAction() == MotionEvent.ACTION_CANCEL || event.getAction() == MotionEvent.ACTION_UP) handler.removeCallbacks(homeLongPress);
        if (event.getAction() == MotionEvent.ACTION_UP) {
            if (longPressTriggered) return true;
            float dx = event.getX() - gestureDownX;
            float dy = event.getY() - gestureDownY;
            boolean verticalSwipe = Math.abs(dy) > dp(65) && Math.abs(dy) > Math.abs(dx)
                    && ((!drawerOpen && dy < 0) || (drawerOpen && dy > 0));
            boolean horizontalSwipe = !drawerOpen && Math.abs(dx) > dp(65) && Math.abs(dx) > Math.abs(dy);
            if (verticalSwipe || horizontalSwipe) {
                // Cancel the child click before handling a swipe, otherwise a swipe
                // beginning on an icon can accidentally launch that app on ACTION_UP.
                MotionEvent cancel = MotionEvent.obtain(event);
                cancel.setAction(MotionEvent.ACTION_CANCEL);
                super.dispatchTouchEvent(cancel);
                cancel.recycle();
                if (verticalSwipe) {
                    if (!drawerOpen && dy < 0) showDrawer();
                    else showHome();
                } else if (dx < 0) {
                    nextPage();
                } else {
                    previousPage();
                }
                return true;
            }
        }
        return super.dispatchTouchEvent(event);
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
        boolean removedMissingApps = false;
        // Avoid Collection.removeIf: it is not available on every Android 6 runtime.
        for (int i = homePackages.size() - 1; i >= 0; i--) {
            if (findApp(homePackages.get(i)) == null) {
                homePackages.remove(i);
                removedMissingApps = true;
            }
        }
        if (removedMissingApps) {
            persistHomeApps();
            int requiredPages = Math.max(1, (homePackages.size() + PAGE_CAPACITY - 1) / PAGE_CAPACITY);
            if (currentPage >= requiredPages) currentPage = requiredPages - 1;
            rebuildPages();
            if (root != null) renderHomePages(false);
        }
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
        // Keep the current empty page when the user has just swiped past a full page,
        // but discard stale page indexes after shortcuts are removed.
        if (currentPage > pages.size()) currentPage = pages.size() - 1;
        while (pages.size() <= currentPage) pages.add(new ArrayList<String>());
    }

    private void buildInterface() {
        root = new FrameLayout(this);
        applyHomeWallpaper();
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

        pageViewport = new FrameLayout(this);
        pageViewport.setClipChildren(true);
        pageViewport.setClipToPadding(true);
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(-1, 0, 1f);
        pp.topMargin = dp(12);
        homeScreen.addView(pageViewport, pp);

        // The viewport stays fixed while the page strip moves inside it.
        pagesStrip = new LinearLayout(this);
        pagesStrip.setOrientation(LinearLayout.HORIZONTAL);
        pagesStrip.setClipChildren(false);
        pageViewport.addView(pagesStrip, new FrameLayout.LayoutParams(-2, -1, Gravity.TOP | Gravity.LEFT));

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
            showAppOptions(drawerApps.get(position), false);
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
            grid.setNumColumns(prefs == null ? COLUMNS : prefs.getInt(KEY_COLUMNS, COLUMNS));
            grid.setVerticalSpacing(dp(8));
            grid.setHorizontalSpacing(dp(4));
            grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
            grid.setSelector(android.R.color.transparent);
            grid.setAdapter(new AppAdapter(false, pages.get(pageIndex)));
            int pageWidth = Math.max(1, getResources().getDisplayMetrics().widthPixels - dp(40));
            pagesStrip.addView(grid, new LinearLayout.LayoutParams(pageWidth, -1));
        }
        pageIndicator.setText((currentPage + 1) + "  /  " + pages.size());
        int pageWidth = Math.max(1, getResources().getDisplayMetrics().widthPixels - dp(40));
        pagesStrip.setTranslationX(-currentPage * pageWidth);
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

    private void showAppOptions(ResolveInfo app, boolean fromHome) {
        java.util.ArrayList<String> actions = new java.util.ArrayList<>();
        actions.add("App info");
        actions.add("Uninstall");
        int homeIndex = homePackages.indexOf(app.activityInfo.packageName);
        if (fromHome && homeIndex > 0) actions.add("Move earlier");
        if (fromHome && homeIndex >= 0 && homeIndex < homePackages.size() - 1) actions.add("Move later");
        if (fromHome) actions.add("Remove from Home");
        else actions.add("Add to Home");
        new android.app.AlertDialog.Builder(this).setTitle(app.loadLabel(packageManager))
            .setItems(actions.toArray(new String[0]), (dialog, which) -> {
                String action = actions.get(which);
                if (action.equals("App info")) {
                    Intent i = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    i.setData(android.net.Uri.parse("package:" + app.activityInfo.packageName));
                    startActivity(i);
                } else if (action.equals("Uninstall")) {
                    try {
                        startActivity(new Intent(Intent.ACTION_DELETE, android.net.Uri.parse("package:" + app.activityInfo.packageName)));
                    } catch (Exception e) {
                        Toast.makeText(this, "Uninstall isn't available for this app", Toast.LENGTH_SHORT).show();
                    }
                } else if (action.equals("Move earlier") || action.equals("Move later")) {
                    int from = homePackages.indexOf(app.activityInfo.packageName);
                    int to = from + (action.equals("Move earlier") ? -1 : 1);
                    if (from >= 0 && to >= 0 && to < homePackages.size()) {
                        Collections.swap(homePackages, from, to);
                        persistHomeApps();
                        rebuildPages();
                        currentPage = to / PAGE_CAPACITY;
                        renderHomePages(true);
                    }
                } else if (action.equals("Remove from Home")) {
                    homePackages.remove(app.activityInfo.packageName);
                    int requiredPages = Math.max(1, (homePackages.size() + PAGE_CAPACITY - 1) / PAGE_CAPACITY);
                    if (currentPage >= requiredPages) currentPage = requiredPages - 1;
                    persistHomeApps(); rebuildPages(); renderHomePages(false);
                } else {
                    addToHome(app.activityInfo.packageName);
                    showHome();
                }
            }).show();
    }

    private void showCustomizationMenu() {
        String[] options = {"3 columns", "4 columns", "5 columns", "Toggle date", "Smaller icons", "Larger icons", "Choose wallpaper", "System wallpaper picker", "Reset wallpaper", "Reset home layout"};
        new android.app.AlertDialog.Builder(this).setTitle("Home customization").setItems(options, (dialog, which) -> {
            if (which < 3) prefs.edit().putInt(KEY_COLUMNS, which == 0 ? 3 : which == 1 ? 4 : 5).apply();
            else if (which == 3) prefs.edit().putBoolean(KEY_SHOW_DATE, !prefs.getBoolean(KEY_SHOW_DATE, true)).apply();
            else if (which == 4 || which == 5) {
                int size = prefs.getInt(KEY_ICON_SIZE, 48) + (which == 4 ? -6 : 6);
                prefs.edit().putInt(KEY_ICON_SIZE, Math.max(32, Math.min(72, size))).apply();
            } else if (which == 6) {
                Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                pick.addCategory(Intent.CATEGORY_OPENABLE);
                pick.setType("image/*");
                pick.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                try { startActivityForResult(pick, REQUEST_WALLPAPER); }
                catch (Exception e) { Toast.makeText(this, "No image picker is available", Toast.LENGTH_LONG).show(); }
                return;
            } else if (which == 7) {
                try { startActivity(new Intent(Intent.ACTION_SET_WALLPAPER)); }
                catch (Exception e) { Toast.makeText(this, "System wallpaper picker unavailable", Toast.LENGTH_SHORT).show(); }
                return;
            } else if (which == 8) {
                prefs.edit().remove(KEY_WALLPAPER_URI).apply();
                applyHomeWallpaper();
            } else {
                homePackages.clear();
                prefs.edit().remove(KEY_HOME_APPS).remove(KEY_COLUMNS)
                        .remove(KEY_SHOW_DATE).remove(KEY_ICON_SIZE).apply();
                currentPage = 0;
                restoreHomeApps();
            }
            updateClock(); renderHomePages(true);
        }).show();
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_WALLPAPER || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        try {
            int flags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            if ((flags & Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0) {
                try { getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION); }
                catch (Exception ignored) {}
            }
            prefs.edit().putString(KEY_WALLPAPER_URI, uri.toString()).apply();
            if (applyHomeWallpaper()) Toast.makeText(this, "Home wallpaper updated", Toast.LENGTH_SHORT).show();
            else Toast.makeText(this, "Couldn't read that image. Choose another one.", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Couldn't set that wallpaper", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean touchStartedOnAppCell(float rawX, float rawY) {
        View target = findViewAt(root, (int) rawX, (int) rawY);
        while (target != null) {
            if ("launcher_app_cell".equals(target.getTag())) return true;
            if (target == root) break;
            android.view.ViewParent parent = target.getParent();
            target = parent instanceof View ? (View) parent : null;
        }
        return false;
    }

    private View findViewAt(View view, int rawX, int rawY) {
        if (view == null || view.getVisibility() != View.VISIBLE) return null;
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        int localX = rawX - location[0];
        int localY = rawY - location[1];
        if (localX < 0 || localY < 0 || localX >= view.getWidth() || localY >= view.getHeight()) return null;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = group.getChildCount() - 1; i >= 0; i--) {
                View found = findViewAt(group.getChildAt(i), rawX, rawY);
                if (found != null) return found;
            }
        }
        return view;
    }

    private boolean applyHomeWallpaper() {
        if (root == null) return false;
        String saved = prefs == null ? null : prefs.getString(KEY_WALLPAPER_URI, null);
        if (saved != null && !saved.isEmpty()) {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            try {
                Uri uri = Uri.parse(saved);
                try (java.io.InputStream in = getContentResolver().openInputStream(uri)) {
                    if (in != null) BitmapFactory.decodeStream(in, null, bounds);
                }
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw new java.io.IOException("Image could not be decoded");
                int screenW = Math.max(1, getResources().getDisplayMetrics().widthPixels);
                int screenH = Math.max(1, getResources().getDisplayMetrics().heightPixels);
                int sample = 1;
                while (bounds.outWidth / (sample * 2) >= screenW && bounds.outHeight / (sample * 2) >= screenH) sample *= 2;
                BitmapFactory.Options decode = new BitmapFactory.Options();
                decode.inSampleSize = sample;
                try (java.io.InputStream in = getContentResolver().openInputStream(uri)) {
                    android.graphics.Bitmap bitmap = in == null ? null : BitmapFactory.decodeStream(in, null, decode);
                    if (bitmap != null) {
                        BitmapDrawable wallpaper = new BitmapDrawable(getResources(), bitmap);
                        wallpaper.setGravity(Gravity.FILL);
                        root.setBackground(wallpaper);
                        return true;
                    }
                }
                throw new java.io.IOException("Image could not be decoded");
            } catch (Exception ignored) {
                prefs.edit().remove(KEY_WALLPAPER_URI).apply();
            }
        }
        root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(11, 22, 51), Color.rgb(20, 42, 88), BG}));
        return false;
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
        clockText.setText(new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(now));
        dateText.setVisibility(prefs.getBoolean(KEY_SHOW_DATE, true) ? View.VISIBLE : View.GONE);
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
                int iconSize = prefs == null ? 48 : prefs.getInt(KEY_ICON_SIZE, 48);
                LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(dp(iconSize), dp(iconSize));
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
            cell.setTag("launcher_app_cell");
            cell.setOnClickListener(v -> launchApp(app));
            cell.setOnLongClickListener(v -> { showAppOptions(app, !drawer); return true; });
            return cell;
        }
    }
}
