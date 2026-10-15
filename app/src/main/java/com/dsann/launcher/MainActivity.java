package com.dsann.launcher;

import android.app.Activity;
import android.app.Dialog;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Intent;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
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
import android.provider.Settings;
import android.text.format.DateFormat;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ScrollView;
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
    private static final String KEY_DRAWER_COLUMNS = "drawer_columns";
    private static final String KEY_DRAWER_ICON_SIZE = "drawer_icon_size";
    private static final String KEY_HOME_FONT_SIZE = "home_font_size";
    private static final String KEY_DRAWER_FONT_SIZE = "drawer_font_size";
    private static final String KEY_CLOCK_STYLE = "clock_style";
    private static final String KEY_SHOW_HOME_LABELS = "show_home_labels";
    private static final String KEY_SHOW_DRAWER_LABELS = "show_drawer_labels";
    private static final String KEY_SHOW_PAGE_INDICATOR = "show_page_indicator";
    private static final String KEY_SHOW_BRAND = "show_brand";
    private static final String KEY_DRAWER_DESCENDING = "drawer_descending";
    private static final String KEY_DRAWER_SPACING = "drawer_spacing";
    private static final String KEY_COLOR_THEME = "color_theme";
    private static final String KEY_24_HOUR = "24_hour_clock";
    private static final String KEY_WALLPAPER_URI = "wallpaper_uri";
    private static final String KEY_ICON_X = "icon_x_";
    private static final String KEY_ICON_Y = "icon_y_";
    private static final String KEY_ICON_PAGE = "icon_page_";
    private static final String KEY_DOCK_APPS = "dock_apps";
    private static final String KEY_WIDGET_IDS = "widget_ids";
    private static final int REQUEST_WALLPAPER = 7301;
    private static final int REQUEST_WIDGET_PICK = 7302;
    private static final int REQUEST_WIDGET_BIND = 7303;
    private static final int REQUEST_WIDGET_CONFIGURE = 7304;
    private int pendingWidgetId = -1;
    private AppWidgetProviderInfo pendingWidgetInfo;
    private int pendingWidgetTargetPage = 0;
    private static final int COLUMNS = 4;
    private static final int HOME_ROWS = 5;
    private interface IntAction { void onSelect(int value); }
    private final int BG = Color.rgb(10, 13, 24);
    private final Handler handler = new Handler();
    private PackageManager packageManager;
    private SharedPreferences prefs;
    private AppWidgetManager widgetManager;
    private AppWidgetHost widgetHost;
    private WidgetInteractionController widgetInteractionController;
    private HomePageController homePageController;
    private FrameLayout root;
    private LinearLayout homeScreen;
    private TextView brandText;
    private LinearLayout pagesStrip;
    private FrameLayout pageViewport;
    private LinearLayout dockContainer;
    private final List<String> dockPackages = new ArrayList<>();
    private static final int DOCK_CAPACITY = 2;
    private LinearLayout drawerScreen;
    private GridView drawerGrid;
    private TextView clockText;
    private TextView amPmText;
    private TextView dateText;
    private LinearLayout clockPanel;
    private TextView pageIndicator;
    private LauncherAppAdapter drawerAdapter;
    private List<ResolveInfo> allApps = new ArrayList<>();
    private List<ResolveInfo> drawerApps = new ArrayList<>();
    private final List<String> homePackages = new ArrayList<>();
    private final List<List<String>> pages = new ArrayList<>();
    private int currentPage = 0;
    private float gestureDownX, gestureDownY;
    private boolean gestureStartedOnAppCell = false;
    private boolean pageSwipeActive = false;
    private boolean pageSwipeChildCancelled = false;
    private float longPressDownX, longPressDownY;
    private boolean longPressTriggered = false;
    private boolean iconDragArmed = false;
    private boolean iconDragging = false;
    private boolean iconMovedTooEarly = false;
    private float iconTouchRawX, iconTouchRawY;
    private int iconOriginalLeft, iconOriginalTop;
    private int iconDragPageIndex = -1;
    private int iconOriginalPageIndex = -1;
    private int iconEdgeLatch = 0;
    private View activeDragView;
    private String activeDragPackage;
    private Runnable iconArmDrag;
    private Runnable iconShowOptions;
    private final Runnable homeLongPress = new Runnable() {
        @Override public void run() {
            if (!drawerOpen && root != null) {
                longPressTriggered = true;
                if (currentPage == 1) showSecondPageMenu();
                else showCustomizationMenu();
            }
        }
    };
    private boolean drawerOpen = false;
    private boolean drawerAtTopOnGestureDown = true;
    private boolean packageReceiverRegistered = false;
    private final BroadcastReceiver packageChangesReceiver = new BroadcastReceiver() {
        @Override public void onReceive(android.content.Context context, Intent intent) {
            // Package broadcasts can arrive before PackageManager's launcher query is refreshed.
            // Debounce briefly, then refresh the existing list objects used by the drawer adapter.
            handler.removeCallbacks(refreshAppsAfterPackageChange);
            handler.postDelayed(refreshAppsAfterPackageChange, 300L);
        }
    };
    private final Runnable refreshAppsAfterPackageChange = new Runnable() {
        @Override public void run() { loadApps(); }
    };
    private final Runnable clockUpdater = new Runnable() {
        @Override public void run() {
            updateClock();
            // Align refreshes to the system minute boundary instead of drifting
            // from the time at which the launcher was opened.
            long remainder = System.currentTimeMillis() % 60000L;
            handler.postDelayed(this, remainder == 0L ? 60000L : 60000L - remainder);
        }
    };
    private final BroadcastReceiver timeChangesReceiver = new BroadcastReceiver() {
        @Override public void onReceive(android.content.Context context, Intent intent) {
            handler.removeCallbacks(clockUpdater);
            updateClock();
            long remainder = System.currentTimeMillis() % 60000L;
            handler.postDelayed(clockUpdater, remainder == 0L ? 60000L : 60000L - remainder);
        }
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        packageManager = getPackageManager();
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        IntentFilter packageFilter = new IntentFilter();
        packageFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        packageFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        packageFilter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        packageFilter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        packageFilter.addDataScheme("package");
        registerReceiver(packageChangesReceiver, packageFilter);
        packageReceiverRegistered = true;
        IntentFilter timeFilter = new IntentFilter();
        timeFilter.addAction(Intent.ACTION_TIME_TICK);
        timeFilter.addAction(Intent.ACTION_TIME_CHANGED);
        timeFilter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        timeFilter.addAction(Intent.ACTION_DATE_CHANGED);
        registerReceiver(timeChangesReceiver, timeFilter);
        widgetManager = AppWidgetManager.getInstance(this);
        widgetHost = new AppWidgetHost(this, HOST_ID);
        widgetInteractionController = new WidgetInteractionController(this, handler, widgetHost, prefs, KEY_WIDGET_IDS,
                new WidgetInteractionController.Listener() {
                    @Override public void saveWidgetGeometry(int widgetId, int pageIndex, FrameLayout container) {
                        MainActivity.this.saveWidgetGeometry(widgetId, pageIndex, container);
                    }
                    @Override public void ensurePageExists(int pageIndex) { MainActivity.this.ensurePageExists(pageIndex); }
                    @Override public void renderHomePages(boolean animate) { MainActivity.this.renderHomePages(animate); }
                    @Override public void showActionDialog(String title, String[] icons, String[] labels,
                            android.content.DialogInterface.OnClickListener callback) {
                        MainActivity.this.showActionDialog(title, icons, labels, callback);
                    }
                    @Override public void setCurrentPage(int page) { currentPage = page; }
                    @Override public int getCurrentPage() { return currentPage; }
                    @Override public void showPage(int page, boolean animate) { MainActivity.this.showPage(page, animate); }
                    @Override public LinearLayout getPagesStrip() { return pagesStrip; }
                    @Override public void resolveWidgetCollisions(FrameLayout page, FrameLayout widgetContainer) {
                        MainActivity.this.resolveWidgetIconCollisions(page, widgetContainer);
                    }
                });
        // Let the wallpaper flow behind the status bar so it feels like launcher chrome,
        // while retaining the system icons and reserving their inset for launcher content.
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        loadApps();
        restoreHomeApps();
        buildInterface();
    }

    @Override public boolean dispatchTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            gestureDownX = event.getX();
            gestureDownY = event.getY();
            longPressDownX = event.getX();
            longPressDownY = event.getY();
            longPressTriggered = false;
            pageSwipeActive = false;
            pageSwipeChildCancelled = false;
            drawerAtTopOnGestureDown = !drawerOpen || isDrawerAtTop();
            gestureStartedOnAppCell = touchStartedOnAppCell(event.getRawX(), event.getRawY());
            handler.removeCallbacks(homeLongPress);
            if (!drawerOpen && !gestureStartedOnAppCell) handler.postDelayed(homeLongPress, 2000L);
            return super.dispatchTouchEvent(event);
        }

        float dx = event.getX() - gestureDownX;
        float dy = event.getY() - gestureDownY;
        if (action == MotionEvent.ACTION_MOVE) {
            if (Math.abs(event.getX() - longPressDownX) > LauncherUiUtils.dp(this, 12)
                    || Math.abs(event.getY() - longPressDownY) > LauncherUiUtils.dp(this, 12)) {
                handler.removeCallbacks(homeLongPress);
            }
            // Move the strip continuously with the finger. The old implementation waited
            // until ACTION_UP, so users saw no live page movement while dragging.
            if (!drawerOpen && pagesStrip != null && pagesStrip.getChildCount() > 1
                    && Math.abs(dx) > LauncherUiUtils.dp(this, 12)
                    && Math.abs(dx) > Math.abs(dy) * 1.15f) {
                if (!pageSwipeActive) {
                    pageSwipeActive = true;
                    pagesStrip.animate().cancel();
                }
                if (!pageSwipeChildCancelled) {
                    MotionEvent cancel = MotionEvent.obtain(event);
                    cancel.setAction(MotionEvent.ACTION_CANCEL);
                    super.dispatchTouchEvent(cancel);
                    cancel.recycle();
                    pageSwipeChildCancelled = true;
                }
                int pageWidth = pageViewport != null && pageViewport.getWidth() > 0
                        ? pageViewport.getWidth()
                        : Math.max(1, getResources().getDisplayMetrics().widthPixels
                                - LauncherUiUtils.dp(this, 40));
                float proposed = -currentPage * pageWidth + dx;
                float clamped = Math.max(-pageWidth, Math.min(0f, proposed));
                pagesStrip.setTranslationX(clamped);

                // Add a restrained page-turn rotation while the strip follows the
                // finger. The incoming page rotates toward flat as it enters view.
                float progress = Math.max(-1f, Math.min(1f, dx / (float) pageWidth));
                for (int i = 0; i < pagesStrip.getChildCount(); i++) {
                    View face = pagesStrip.getChildAt(i);
                    face.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                    face.setCameraDistance(12000f * getResources().getDisplayMetrics().density);
                    if (i == currentPage) {
                        face.setPivotX(progress < 0f ? face.getWidth() : 0f);
                        face.setRotationY(-progress * 14f);
                        face.setAlpha(1f - Math.min(0.12f, Math.abs(progress) * 0.12f));
                    } else if (i == currentPage + 1 && progress < 0f) {
                        face.setPivotX(0f);
                        face.setRotationY((1f + progress) * -14f);
                        face.setAlpha(1f);
                    } else if (i == currentPage - 1 && progress > 0f) {
                        face.setPivotX(face.getWidth());
                        face.setRotationY((1f - progress) * 14f);
                        face.setAlpha(1f);
                    } else {
                        face.setRotationY(0f);
                        face.setAlpha(1f);
                    }
                }
                return true;
            }
            return pageSwipeActive || super.dispatchTouchEvent(event);
        }

        if (action == MotionEvent.ACTION_CANCEL) {
            handler.removeCallbacks(homeLongPress);
            if (pageSwipeActive) {
                pageSwipeActive = false;
                showPage(currentPage, true);
                return true;
            }
            return super.dispatchTouchEvent(event);
        }

        if (action == MotionEvent.ACTION_UP) {
            handler.removeCallbacks(homeLongPress);
            if (longPressTriggered) return true;
            if (pageSwipeActive) {
                pageSwipeActive = false;
                int pageWidth = pageViewport != null && pageViewport.getWidth() > 0
                        ? pageViewport.getWidth()
                        : Math.max(1, getResources().getDisplayMetrics().widthPixels
                                - LauncherUiUtils.dp(this, 40));
                int destination = currentPage;
                if (Math.abs(dx) > Math.max(LauncherUiUtils.dp(this, 65), pageWidth * 0.18f)) {
                    if (dx < 0 && currentPage < pages.size() - 1) destination = currentPage + 1;
                    else if (dx > 0 && currentPage > 0) destination = currentPage - 1;
                }
                showPage(destination, true);
                return true;
            }

            boolean verticalSwipe = Math.abs(dy) > LauncherUiUtils.dp(this, 65)
                    && Math.abs(dy) > Math.abs(dx)
                    && ((!drawerOpen && dy < 0) || (drawerOpen && dy > 0 && drawerAtTopOnGestureDown));
            if (verticalSwipe) {
                MotionEvent cancel = MotionEvent.obtain(event);
                cancel.setAction(MotionEvent.ACTION_CANCEL);
                super.dispatchTouchEvent(cancel);
                cancel.recycle();
                if (!drawerOpen && dy < 0) showDrawer();
                else showHome();
                return true;
            }
        }
        return super.dispatchTouchEvent(event);
    }

    @Override public void onBackPressed() {
        // Back closes the app drawer first. On the launcher home screen, consume
        // Back instead of letting the task fall through to system overview.
        if (drawerOpen) {
            showHome();
            return;
        }
        handler.removeCallbacks(homeLongPress);
        // Stay on the home screen; the system Recents action belongs to the
        // navigation bar/gesture, not the Back button.
    }

    @Override protected void onResume() {
        super.onResume();
        handler.removeCallbacks(clockUpdater);
        handler.post(clockUpdater);
        // loadApps() refreshes the home only when shortcuts actually disappear.
        // Rebuilding every icon view whenever returning from any app caused visible stutter.
        loadApps();
        try { widgetHost.startListening(); } catch (Exception ignored) {}
    }

    @Override protected void onPause() {
        handler.removeCallbacks(clockUpdater);
        try { widgetHost.stopListening(); } catch (Exception ignored) {}
        super.onPause();
    }

    @Override protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (packageReceiverRegistered) {
            try { unregisterReceiver(packageChangesReceiver); } catch (IllegalArgumentException ignored) {}
            packageReceiverRegistered = false;
        }
        try { unregisterReceiver(timeChangesReceiver); } catch (IllegalArgumentException ignored) {}
        super.onDestroy();
    }

    private void loadApps() {
        Intent query = new Intent(Intent.ACTION_MAIN, null);
        query.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> result = packageManager.queryIntentActivities(query, 0);
        Collections.sort(result, new Comparator<ResolveInfo>() {
            @Override public int compare(ResolveInfo a, ResolveInfo b) {
                int order = a.loadLabel(packageManager).toString().compareToIgnoreCase(b.loadLabel(packageManager).toString());
                return prefs != null && prefs.getBoolean(KEY_DRAWER_DESCENDING, false) ? -order : order;
            }
        });
        // Keep the list objects stable: LauncherAppAdapter and the drawer's long-press
        // listener retain references to these exact lists.
        allApps.clear();
        allApps.addAll(result);
        drawerApps.clear();
        // The launcher is a system-facing app, not a user app shortcut: keep it
        // discoverable by PackageManager for internal lookups, but hide it from its own drawer.
        String launcherPackage = getPackageName();
        for (ResolveInfo app : result) {
            if (app.activityInfo != null && !launcherPackage.equals(app.activityInfo.packageName)) {
                drawerApps.add(app);
            }
        }
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
            currentPage = Math.max(0, Math.min(1, currentPage));
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
        // Keep every saved shortcut. The two screens can grow vertically rather than
        // silently dropping shortcuts when a screen exceeds the preferred grid capacity.
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
        // The model always returns exactly two stable screens; it never compacts empty pages.
        HomePageModel.Result result = HomePageModel.rebuild(
                prefs, homePackages, widgetManager, currentPage, pageCapacity(),
                KEY_ICON_PAGE, KEY_WIDGET_IDS, KEY_ICON_X, KEY_ICON_Y, getHomeColumns(), HOME_ROWS);
        pages.clear();
        pages.addAll(result.pages);
        currentPage = result.currentPage;
    }

    private void buildInterface() {
        root = new FrameLayout(this);
        applyHomeWallpaper();
        homeScreen = new LinearLayout(this);
        homeScreen.setOrientation(LinearLayout.VERTICAL);
        homeScreen.setPadding(LauncherUiUtils.dp(this, 20), LauncherUiUtils.dp(this, 14), LauncherUiUtils.dp(this, 20), LauncherUiUtils.dp(this, 8));
        root.addView(homeScreen, new FrameLayout.LayoutParams(-1, -1));
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            homeScreen.setPadding(LauncherUiUtils.dp(this, 20), LauncherUiUtils.dp(this, 14) + insets.getSystemWindowInsetTop(), LauncherUiUtils.dp(this, 20), LauncherUiUtils.dp(this, 8));
            return insets;
        });
        buildHomeChrome();
        buildDrawer();
        setContentView(root);
        root.requestApplyInsets();
        homePageController = new HomePageController(this, prefs, packageManager,
                new HomePageController.Host() {
                    @Override public LinearLayout getPagesStrip() { return pagesStrip; }
                    @Override public FrameLayout getPageViewport() { return pageViewport; }
                    @Override public TextView getPageIndicator() { return pageIndicator; }
                    @Override public List<List<String>> getPages() { return pages; }
                    @Override public List<ResolveInfo> getAllApps() { return allApps; }
                    @Override public List<ResolveInfo> getDrawerApps() { return drawerApps; }
                    @Override public int getCurrentPage() { return currentPage; }
                    @Override public void rebuildPages() { MainActivity.this.rebuildPages(); }
                    @Override public int getHomeColumns() { return MainActivity.this.getHomeColumns(); }
                    @Override public ResolveInfo findApp(String packageName) { return MainActivity.this.findApp(packageName); }
                    @Override public void installHomeIconTouch(View cell, String packageName) { MainActivity.this.installHomeIconTouch(cell, packageName); }
                    @Override public void restoreWidgetsOnPages() { MainActivity.this.restoreWidgetsOnPages(); }
                    @Override public void syncWidgetViewsToPages() { MainActivity.this.syncWidgetViewsToPages(); }
                    @Override public void launchApp(ResolveInfo app) { MainActivity.this.launchApp(app); }
                    @Override public void showAppOptions(ResolveInfo app, boolean onHome) { MainActivity.this.showAppOptions(app, onHome); }
                }, KEY_ICON_X, KEY_ICON_Y, KEY_SHOW_HOME_LABELS, KEY_ICON_SIZE, KEY_HOME_FONT_SIZE, KEY_SHOW_PAGE_INDICATOR, HOME_ROWS);
        renderHomePages(false);
        showHome();
    }

    private void buildHomeChrome() {
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView brand = LauncherUiUtils.text(this, "D.Sannigrahi", 12, Color.rgb(210, 220, 255), true);
        brandText = brand;
        brand.setTypeface(Typeface.create("sans-serif-medium", Typeface.ITALIC));
        brand.setLetterSpacing(0.035f);
        header.addView(brand, new LinearLayout.LayoutParams(0, LauncherUiUtils.dp(this, 32), 1f));
        homeScreen.addView(header);

        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setGravity(Gravity.RIGHT);
        hero.setPadding(LauncherUiUtils.dp(this, 4), LauncherUiUtils.dp(this, 8), LauncherUiUtils.dp(this, 4), LauncherUiUtils.dp(this, 8));
        hero.setClipChildren(false);
        hero.setClipToPadding(false);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(
                Math.max(LauncherUiUtils.dp(this, 220), (int) ((getResources().getDisplayMetrics().widthPixels - LauncherUiUtils.dp(this, 40)) * 0.82f)),
                -2);
        hp.gravity = Gravity.RIGHT;
        hp.topMargin = LauncherUiUtils.dp(this, 8);
        homeScreen.addView(hero, hp);
        clockPanel = hero;
        applyClockPanelColor(false);
        LinearLayout clockRow = new LinearLayout(this);
        clockRow.setOrientation(LinearLayout.HORIZONTAL);
        clockRow.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        clockRow.setClipChildren(false);
        clockRow.setClipToPadding(false);
        clockText = new SevenSegmentClockTextView(this);
        clockText.setText("--:--");
        clockText.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        clockText.setTextSize(78);
        // Scale the clock's drawing area by 50%; the custom LED renderer fits digits to these bounds.
        int clockDisplayWidth = Math.min(LauncherUiUtils.dp(this, 340),
                Math.max(LauncherUiUtils.dp(this, 185), (int) ((getResources().getDisplayMetrics().widthPixels
                        - LauncherUiUtils.dp(this, 40)) * 0.82f) - LauncherUiUtils.dp(this, 38)));
        clockText.setLayoutParams(new LinearLayout.LayoutParams(clockDisplayWidth, LauncherUiUtils.dp(this, 114)));
        clockText.setTextColor(Color.WHITE);
        clockText.setIncludeFontPadding(false);
        clockText.setTypeface(Typeface.create("monospace", Typeface.BOLD));
        clockText.setLetterSpacing(0f);
        applyClockStyle();
        clockRow.addView(clockText);
        amPmText = new TextView(this);
        amPmText.setTextColor(Color.WHITE);
        amPmText.setTextSize(10);
        amPmText.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        amPmText.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
        amPmText.setIncludeFontPadding(false);
        amPmText.setPadding(LauncherUiUtils.dp(this, 4), 0, 0, 0);
        clockRow.addView(amPmText, new LinearLayout.LayoutParams(LauncherUiUtils.dp(this, 30), LauncherUiUtils.dp(this, 114)));
        hero.addView(clockRow, new LinearLayout.LayoutParams(-1, LauncherUiUtils.dp(this, 114)));
        dateText = LauncherUiUtils.text(this, "", 13, Color.rgb(190, 201, 218), false);
        dateText.setTypeface(Typeface.create("sans-serif-condensed-medium", Typeface.NORMAL));
        dateText.setGravity(Gravity.RIGHT);
        dateText.setLetterSpacing(0.075f);
        LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(-1, -2);
        dp.topMargin = LauncherUiUtils.dp(this, 3);
        hero.addView(dateText, dp);
        applyClockPanelColor(false);

        pageViewport = new FrameLayout(this);
        // Permit the rotating faces to extend slightly beyond the viewport while turning.
        pageViewport.setClipChildren(false);
        pageViewport.setClipToPadding(false);
        pageViewport.setCameraDistance(12000f * getResources().getDisplayMetrics().density);
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(-1, 0, 1f);
        pp.topMargin = LauncherUiUtils.dp(this, 12);
        homeScreen.addView(pageViewport, pp);

        // The viewport stays fixed while the page strip moves inside it.
        pagesStrip = new LinearLayout(this);
        pagesStrip.setOrientation(LinearLayout.HORIZONTAL);
        pagesStrip.setClipChildren(false);
        pageViewport.addView(pagesStrip, new FrameLayout.LayoutParams(-2, -1, Gravity.TOP | Gravity.LEFT));

        pageIndicator = LauncherUiUtils.text(this, "", 11, Color.rgb(210, 220, 255), false);
        pageIndicator.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(-1, LauncherUiUtils.dp(this, 28));
        homeScreen.addView(pageIndicator, ip);
        dockContainer = new LinearLayout(this);
        dockContainer.setOrientation(LinearLayout.HORIZONTAL);
        dockContainer.setGravity(Gravity.CENTER_VERTICAL);
        dockContainer.setPadding(0, LauncherUiUtils.dp(this, 2), 0, LauncherUiUtils.dp(this, 2));
        dockContainer.setBackgroundColor(Color.TRANSPARENT);
        int initialDockIconSize = prefs.getInt(KEY_ICON_SIZE, 48);
        LinearLayout.LayoutParams dockParams = new LinearLayout.LayoutParams(-1,
                LauncherUiUtils.dp(this, Math.max(56, initialDockIconSize + 8)));
        dockParams.topMargin = LauncherUiUtils.dp(this, 2);
        dockParams.bottomMargin = LauncherUiUtils.dp(this, 2);
        homeScreen.addView(dockContainer, dockParams);
        restoreDockApps();
    }

    private void buildDrawer() {
        LauncherDrawerController.BuiltDrawer built = LauncherDrawerController.build(
                this, root, prefs, packageManager, allApps, drawerApps, drawerBackground(),
                prefs.getInt(KEY_DRAWER_SPACING, 8), KEY_DRAWER_ICON_SIZE, KEY_DRAWER_FONT_SIZE,
                KEY_SHOW_DRAWER_LABELS, new LauncherDrawerController.Actions() {
                    @Override public void launchApp(ResolveInfo app) { MainActivity.this.launchApp(app); }
                    @Override public void showAppOptions(ResolveInfo app, boolean onHome) {
                        MainActivity.this.showAppOptions(app, onHome);
                    }
                    @Override public int getColumns() { return getDrawerColumns(); }
                });
        drawerScreen = built.screen;
        drawerGrid = built.grid;
        drawerAdapter = built.adapter;
    }

    private void renderHomePages(boolean animate) {
        // Every completed UI refresh rebuilds the page model from saved icons/widgets,
        // compacts away all empty pages, and remaps persisted page indexes before drawing.
        // This also cleans up pages left behind by widget removal or cross-page moves.
        rebuildPages();
        if (homePageController != null) homePageController.renderHomePages(animate);
        renderDock();
    }

    private void restoreDockApps() {
        dockPackages.clear();
        String saved = prefs.getString(KEY_DOCK_APPS, "");
        if (saved != null && !saved.isEmpty()) {
            for (String packageName : saved.split("\n")) {
                if (!packageName.isEmpty() && findApp(packageName) != null
                        && !dockPackages.contains(packageName) && dockPackages.size() < DOCK_CAPACITY) {
                    dockPackages.add(packageName);
                }
            }
        }
    }

    private void persistDockApps() {
        StringBuilder saved = new StringBuilder();
        for (String packageName : dockPackages) {
            if (saved.length() > 0) saved.append('\n');
            saved.append(packageName);
        }
        prefs.edit().putString(KEY_DOCK_APPS, saved.toString()).apply();
    }

    private void renderDock() {
        if (dockContainer == null) return;
        dockContainer.removeAllViews();
        int iconSize = prefs == null ? 48 : prefs.getInt(KEY_ICON_SIZE, 48);
        ViewGroup.LayoutParams dockLayoutParams = dockContainer.getLayoutParams();
        int desiredHeight = LauncherUiUtils.dp(this, Math.max(56, iconSize + 8));
        if (dockLayoutParams != null && dockLayoutParams.height != desiredHeight) {
            dockLayoutParams.height = desiredHeight;
            dockContainer.setLayoutParams(dockLayoutParams);
        }
        // Two app shortcuts maximum, with the app-drawer hint kept centered between them.
        for (int slot = 0; slot < 3; slot++) {
            if (slot == 1) {
                TextView drawerHint = LauncherUiUtils.text(this, "Swipe up for all apps", 11,
                        Color.rgb(180, 194, 224), false);
                drawerHint.setGravity(Gravity.CENTER);
                drawerHint.setSingleLine(true);
                drawerHint.setIncludeFontPadding(false);
                dockContainer.addView(drawerHint, new LinearLayout.LayoutParams(0, -1, 1f));
                continue;
            }

            FrameLayout slotView = new FrameLayout(this);
            slotView.setBackgroundColor(Color.TRANSPARENT);
            LinearLayout.LayoutParams slotParams = new LinearLayout.LayoutParams(0, -1, 1f);
            int packageIndex = slot == 0 ? 0 : 1;
            if (packageIndex < dockPackages.size()) {
                final String packageName = dockPackages.get(packageIndex);
                ResolveInfo app = findApp(packageName);
                if (app != null) {
                    ImageView icon = new ImageView(this);
                    icon.setImageDrawable(app.loadIcon(packageManager));
                    icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    FrameLayout.LayoutParams iconParams = new FrameLayout.LayoutParams(
                            LauncherUiUtils.dp(this, iconSize), LauncherUiUtils.dp(this, iconSize), Gravity.CENTER);
                    slotView.addView(icon, iconParams);
                    slotView.setContentDescription(String.valueOf(app.loadLabel(packageManager)));
                    slotView.setOnClickListener(v -> launchApp(app));
                    slotView.setOnLongClickListener(v -> {
                        showActionDialog("Dock app", new String[]{"−", "↗"},
                                new String[]{"Remove from dock", "Move to home screen"}, (dialog, which) -> {
                            if (which == 1 && !homePackages.contains(packageName)
                                    && homePackages.size() >= pageCapacity()) {
                                Toast.makeText(MainActivity.this, "Home screen is full.", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            dockPackages.remove(packageName);
                            if (which == 1 && !homePackages.contains(packageName)) homePackages.add(packageName);
                            persistDockApps();
                            persistHomeApps();
                            renderHomePages(false);
                        });
                        return true;
                    });
                }
            }
            dockContainer.addView(slotView, slotParams);
        }
    }

    private void installHomeIconTouch(View cell, String packageName) {
        cell.setOnClickListener(v -> {
            ResolveInfo app = findApp(packageName);
            if (!iconDragging && !longPressTriggered && app != null) launchApp(app);
        });
        cell.setOnLongClickListener(null);
        cell.setOnTouchListener((view, event) -> {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                iconTouchRawX = event.getRawX();
                iconTouchRawY = event.getRawY();
                iconDragArmed = false;
                iconDragging = false;
                iconMovedTooEarly = false;
                activeDragView = view;
                activeDragPackage = packageName;
                iconDragPageIndex = view.getParent() instanceof View
                        ? pagesStrip.indexOfChild((View) view.getParent()) : currentPage;
                iconEdgeLatch = 0;
                iconOriginalPageIndex = iconDragPageIndex;
                FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) view.getLayoutParams();
                iconOriginalLeft = lp.leftMargin;
                iconOriginalTop = lp.topMargin;
                if (iconArmDrag != null) handler.removeCallbacks(iconArmDrag);
                if (iconShowOptions != null) handler.removeCallbacks(iconShowOptions);
                iconArmDrag = () -> {
                    if (activeDragView == view) iconDragArmed = true;
                };
                iconShowOptions = () -> {
                    if (activeDragView == view && !iconDragging) {
                        longPressTriggered = true;
                        ResolveInfo app = findApp(packageName);
                        if (app != null) showAppOptions(app, true);
                    }
                };
                handler.postDelayed(iconArmDrag, 1000L);
                handler.postDelayed(iconShowOptions, 2000L);
                return true;
            }
            if (action == MotionEvent.ACTION_MOVE) {
                float dx = event.getRawX() - iconTouchRawX;
                float dy = event.getRawY() - iconTouchRawY;
                boolean moved = Math.abs(dx) > LauncherUiUtils.dp(this, 8) || Math.abs(dy) > LauncherUiUtils.dp(this, 8);
                if (!iconDragArmed && moved) {
                    iconMovedTooEarly = true;
                    if (iconArmDrag != null) handler.removeCallbacks(iconArmDrag);
                    if (iconShowOptions != null) handler.removeCallbacks(iconShowOptions);
                }
                if (!iconDragging && iconDragArmed && moved) {
                    iconDragging = true;
                    if (iconShowOptions != null) handler.removeCallbacks(iconShowOptions);
                    view.animate().scaleX(1.12f).scaleY(1.12f).alpha(0.92f).setDuration(100).start();
                    view.bringToFront();
                }
                if (iconDragging) {
                    // Fixed workspace: dragging never changes screens or creates pages.
                    iconEdgeLatch = 0;
                    FrameLayout page = (FrameLayout) view.getParent();
                    int pageWidth = Math.max(1, page.getWidth());
                    int[] pageLocation = new int[2];
                    page.getLocationOnScreen(pageLocation);
                    FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) view.getLayoutParams();
                    int maxX = Math.max(0, page.getWidth() - view.getWidth());
                    int maxY = Math.max(0, page.getHeight() - view.getHeight());
                    // Convert screen coordinates into the source page's own coordinate space.
                    // Compensate for the temporary translation used to preview a cross-page drag.
                    float pageOffsetX = view.getTranslationX();
                    lp.leftMargin = Math.max(0, Math.min(maxX,
                            Math.round(event.getRawX() - pageLocation[0] - view.getWidth() / 2f - pageOffsetX)));
                    lp.topMargin = Math.max(0, Math.min(maxY,
                            Math.round(event.getRawY() - pageLocation[1] - view.getHeight() / 2f)));
                    view.setLayoutParams(lp);
                }
                return true;
            }
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                if (iconArmDrag != null) handler.removeCallbacks(iconArmDrag);
                if (iconShowOptions != null) handler.removeCallbacks(iconShowOptions);
                if (iconDragging) {
                    int[] dockLocation = new int[2];
                    if (dockContainer != null && dockContainer.getWidth() > 0) {
                        dockContainer.getLocationOnScreen(dockLocation);
                        boolean releasedOnDock = event.getRawY() >= dockLocation[1]
                                && event.getRawY() <= dockLocation[1] + dockContainer.getHeight()
                                && event.getRawX() >= dockLocation[0]
                                && event.getRawX() <= dockLocation[0] + dockContainer.getWidth();
                        if (releasedOnDock && !dockPackages.contains(packageName)) {
                            if (dockPackages.size() < DOCK_CAPACITY) {
                                dockPackages.add(packageName);
                                homePackages.remove(packageName);
                                persistDockApps();
                                persistHomeApps();
                                prefs.edit().remove(KEY_ICON_X + packageName)
                                        .remove(KEY_ICON_Y + packageName)
                                        .remove(KEY_ICON_PAGE + packageName).apply();
                            } else {
                                Toast.makeText(this, "Dock is full. Remove an app first.", Toast.LENGTH_SHORT).show();
                            }
                            view.animate().cancel();
                            view.setTranslationX(0f);
                            view.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(100).start();
                            iconDragging = false;
                            iconDragArmed = false;
                            activeDragView = null;
                            activeDragPackage = null;
                            iconDragPageIndex = -1;
                            iconOriginalPageIndex = -1;
                            iconEdgeLatch = 0;
                            renderHomePages(false);
                            return true;
                        }
                    }
                    FrameLayout sourceView = (FrameLayout) view.getParent();
                    int sourcePage = pagesStrip.indexOfChild(sourceView);
                    int destinationPage = Math.max(0, Math.min(1, sourcePage));
                    boolean movedAcrossPage = destinationPage != sourcePage;
                    FrameLayout targetPage = destinationPage < pagesStrip.getChildCount()
                            ? (FrameLayout) pagesStrip.getChildAt(destinationPage) : sourceView;
                    int columns = getHomeColumns(), rows = HOME_ROWS;
                    // The dragged cell stays attached to its source page until ACTION_UP.
                    // Its layout margins therefore are not reliable destination coordinates.
                    // Map the release point to the visible viewport, where the destination
                    // page is aligned after the edge-page preview.
                    int targetWidth = Math.max(1, targetPage.getWidth() > 0
                            ? targetPage.getWidth() : pageViewport.getWidth());
                    int targetHeight = Math.max(1, targetPage.getHeight() > 0
                            ? targetPage.getHeight() : pageViewport.getHeight());
                    int[] viewportLocation = new int[2];
                    pageViewport.getLocationOnScreen(viewportLocation);
                    int cellWidth = Math.max(1, targetWidth / columns);
                    int cellHeight = Math.max(1, targetHeight / rows);
                    int targetCol = Math.max(0, Math.min(columns - 1,
                            (int) ((event.getRawX() - viewportLocation[0]) / cellWidth)));
                    int targetRow = Math.max(0, Math.min(rows - 1,
                            (int) ((event.getRawY() - viewportLocation[1]) / cellHeight)));
                    int sourceCellWidth = Math.max(1, sourceView.getWidth() / columns);
                    int sourceCellHeight = Math.max(1, sourceView.getHeight() / rows);
                    int sourceCol = Math.max(0, Math.min(columns - 1, iconOriginalLeft / sourceCellWidth));
                    int sourceRow = Math.max(0, Math.min(rows - 1, iconOriginalTop / sourceCellHeight));
                    boolean[][] occupied = new boolean[rows][columns];
                    View occupant = null;
                    String otherPackage = null;
                    for (int i = 0; i < targetPage.getChildCount(); i++) {
                        View candidate = targetPage.getChildAt(i);
                        if (candidate == view || !(candidate.getTag() instanceof String)
                                || !((String) candidate.getTag()).startsWith("launcher_app:")) continue;
                        FrameLayout.LayoutParams cp = (FrameLayout.LayoutParams) candidate.getLayoutParams();
                        int cc = Math.max(0, Math.min(columns - 1, Math.round((float) cp.leftMargin / cellWidth)));
                        int cr = Math.max(0, Math.min(rows - 1, Math.round((float) cp.topMargin / cellHeight)));
                        occupied[cr][cc] = true;
                        if (cc == targetCol && cr == targetRow) {
                            occupant = candidate;
                            otherPackage = ((String) candidate.getTag()).substring("launcher_app:".length());
                        }
                    }
                    // A drop onto an occupied cell swaps the two shortcuts instead of
                    // searching for a free cell and leaving the original shortcut behind.
                    // For a cross-page move, the displaced shortcut returns to the source page.
                    if (occupant != null && otherPackage != null) {
                        FrameLayout.LayoutParams otherLp =
                                (FrameLayout.LayoutParams) occupant.getLayoutParams();
                        otherLp.leftMargin = sourceCol * sourceCellWidth;
                        otherLp.topMargin = sourceRow * sourceCellHeight;
                        occupant.setLayoutParams(otherLp);
                        saveIconSlot(otherPackage, sourceCol, sourceRow, columns);
                        if (movedAcrossPage) {
                            prefs.edit().putInt(KEY_ICON_PAGE + otherPackage, sourcePage).apply();
                        }
                    }
                    // Persist the destination page before rebuilding the view hierarchy.
                    // The dragged view intentionally stays attached to its source page until UP;
                    // reparenting during MOVE cancels Android's active touch target.
                    saveIconSlot(packageName, targetCol, targetRow, columns);
                    int committedPage = destinationPage;
                    prefs.edit().putInt(KEY_ICON_PAGE + packageName, committedPage).apply();
                    ensurePageExists(committedPage);
                    currentPage = committedPage;
                    view.setTranslationX(0f);
                    view.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(140).start();
                    iconDragging = false;
                    iconDragArmed = false;
                    activeDragView = null;
                    activeDragPackage = null;
                    iconDragPageIndex = -1;
                    iconOriginalPageIndex = -1;
                    iconEdgeLatch = 0;
                    renderHomePages(false);
                    return true;
                }
                if (action == MotionEvent.ACTION_UP && longPressTriggered) {
                    activeDragView = null;
                    activeDragPackage = null;
                    return true;
                }
                if (action == MotionEvent.ACTION_UP && !iconMovedTooEarly) {
                    ResolveInfo app = findApp(packageName);
                    if (app != null) launchApp(app);
                }
                iconDragArmed = false;
                activeDragView = null;
                activeDragPackage = null;
                return true;
            }
            return iconDragging;
        });
    }

    private void ensurePageExists(int pageIndex) {
        // There are exactly two permanent home screens. This method only repairs missing
        // containers after recreation; it can never append a third screen.
        while (pages.size() < HomePageModel.PAGE_COUNT) pages.add(new ArrayList<String>());
        while (pages.size() > HomePageModel.PAGE_COUNT) pages.remove(pages.size() - 1);
        int pageWidth = pageViewport != null && pageViewport.getWidth() > 0
                ? pageViewport.getWidth()
                : Math.max(1, getResources().getDisplayMetrics().widthPixels
                        - LauncherUiUtils.dp(this, 40));
        if (pagesStrip != null) {
            while (pagesStrip.getChildCount() < HomePageModel.PAGE_COUNT) {
                FrameLayout page = new FrameLayout(this);
                page.setClipChildren(false);
                pagesStrip.addView(page, new LinearLayout.LayoutParams(pageWidth, -1));
            }
        }
    }

    /**
     * Page swipes only move the existing strip. Rebuilding the hierarchy for a swipe
     * unnecessarily detached AppWidgetHostViews and caused focus-related framework
     * crashes. Page membership is rebuilt only after a real content change.
     */
    private void showPage(int requestedPage, boolean animate) {
        if (pages.isEmpty()) rebuildPages();
        int lastPage = Math.max(0, pages.size() - 1);
        currentPage = Math.max(0, Math.min(lastPage, requestedPage));
        if (pagesStrip == null) return;
        int pageWidth = pageViewport != null && pageViewport.getWidth() > 0
                ? pageViewport.getWidth()
                : Math.max(1, getResources().getDisplayMetrics().widthPixels
                        - LauncherUiUtils.dp(this, 40));
        float targetX = -currentPage * pageWidth;
        pagesStrip.animate().cancel();
        // Keep the page faces flat and let the complete strip follow the finger.
        // This avoids the blank/clipped faces seen with 3D rotation on some devices.
        for (int i = 0; i < pagesStrip.getChildCount(); i++) {
            View page = pagesStrip.getChildAt(i);
            page.animate().cancel();
            page.setAlpha(1f);
            if (animate) {
                page.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                page.animate().rotationY(0f).alpha(1f)
                        .setDuration(240L)
                        .setInterpolator(new android.view.animation.DecelerateInterpolator())
                        .withEndAction(() -> page.setLayerType(View.LAYER_TYPE_NONE, null))
                        .start();
            } else {
                page.setRotationY(0f);
                page.setLayerType(View.LAYER_TYPE_NONE, null);
            }
        }
        if (animate) {
            pagesStrip.animate().translationX(targetX)
                    .setDuration(240L)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator())
                    .start();
        } else {
            pagesStrip.setTranslationX(targetX);
        }
        if (pageIndicator != null) {
            boolean show = pages.size() > 1 && prefs.getBoolean(KEY_SHOW_PAGE_INDICATOR, true);
            pageIndicator.setVisibility(show ? View.VISIBLE : View.GONE);
            pageIndicator.setText((currentPage + 1) + "  /  " + pages.size());
        }
    }

    private void nextPage() {
        if (currentPage < pages.size() - 1) showPage(currentPage + 1, true);
    }

    private void previousPage() {
        if (currentPage > 0) showPage(currentPage - 1, true);
    }

    private boolean isDrawerAtTop() {
        if (drawerGrid == null || drawerGrid.getChildCount() == 0) return true;
        if (drawerGrid.getFirstVisiblePosition() > 0) return false;
        View first = drawerGrid.getChildAt(0);
        return first == null || first.getTop() >= drawerGrid.getPaddingTop();
    }

    private void showHome() {
        LauncherScreenTransitions.showHome(this, homeScreen, drawerScreen, () -> drawerOpen = false);
    }

    private void showDrawer() {
        LauncherScreenTransitions.showDrawer(this, homeScreen, drawerScreen, () -> {
            drawerOpen = true;
            loadApps();
        });
    }

    private void addToHome(String packageName) { addToHome(packageName, currentPage); }

    private void addToHome(String packageName, int requestedPage) {
        if (packageName == null || packageName.equals(getPackageName()) || homePackages.contains(packageName)) return;
        int targetPage = Math.max(0, Math.min(1, requestedPage));
        homePackages.add(packageName);
        prefs.edit().putInt(KEY_ICON_PAGE + packageName, targetPage).apply();
        persistHomeApps();
        rebuildPages();
        currentPage = targetPage;
        ensurePageExists(targetPage);
        renderHomePages(false);
        showPage(targetPage, false);
    }

    private void showSecondPageMenu() {
        showActionDialog("Second home screen", new String[]{"▦", "+"},
                new String[]{"Add widget", "Add apps"}, (dialog, which) -> {
            currentPage = 1;
            showPage(1, false);
            if (which == 0) beginWidgetPicker(1);
            else if (which == 1) showAddApps(1);
        });
    }

    private void showAddApps() { showAddApps(currentPage); }

    private void showAddApps(int requestedPage) {
        final int destinationPage = Math.max(0, Math.min(1, requestedPage));
        final List<ResolveInfo> available = new ArrayList<>();
        for (ResolveInfo app : allApps) {
            String packageName = app.activityInfo.packageName;
            if (!packageName.equals(getPackageName()) && !homePackages.contains(packageName)) available.add(app);
        }
        if (available.isEmpty()) {
            Toast.makeText(this, "All apps are already on Home", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] labels = new String[available.size()];
        for (int i = 0; i < available.size(); i++) labels[i] = available.get(i).loadLabel(packageManager).toString();
        new android.app.AlertDialog.Builder(this).setTitle("Add to Home").setItems(labels, (dialog, which) -> {
            addToHome(available.get(which).activityInfo.packageName, destinationPage);
        }).show();
    }

    private void showAppOptions(ResolveInfo app, boolean fromHome) {
        List<String> labels = new ArrayList<>();
        List<String> icons = new ArrayList<>();
        List<String> actions = new ArrayList<>();
        labels.add("App info"); icons.add("ⓘ"); actions.add("App info");
        labels.add("Uninstall"); icons.add("⌫"); actions.add("Uninstall");
        int homeIndex = homePackages.indexOf(app.activityInfo.packageName);
        if (fromHome && homeIndex > 0) { labels.add("Move earlier"); icons.add("↑"); actions.add("Move earlier"); }
        if (fromHome && homeIndex >= 0 && homeIndex < homePackages.size() - 1) { labels.add("Move later"); icons.add("↓"); actions.add("Move later"); }
        if (fromHome) { labels.add("Remove from Home"); icons.add("−"); actions.add("Remove from Home"); }
        else { labels.add("Add to Home"); icons.add("+"); actions.add("Add to Home"); }
        showActionDialog(app.loadLabel(packageManager).toString(), icons.toArray(new String[0]), labels.toArray(new String[0]), (dialog, which) -> {
            String action = actions.get(which);
            if (action.equals("App info")) {
                Intent i = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                i.setData(Uri.parse("package:" + app.activityInfo.packageName));
                startActivity(i);
            } else if (action.equals("Uninstall")) {
                try { startActivity(new Intent(Intent.ACTION_DELETE, Uri.parse("package:" + app.activityInfo.packageName))); }
                catch (Exception e) { Toast.makeText(this, "Uninstall isn't available for this app", Toast.LENGTH_SHORT).show(); }
            } else if (action.equals("Move earlier") || action.equals("Move later")) {
                int from = homePackages.indexOf(app.activityInfo.packageName);
                int to = from + (action.equals("Move earlier") ? -1 : 1);
                if (from >= 0 && to >= 0 && to < homePackages.size()) {
                    Collections.swap(homePackages, from, to);
                    persistHomeApps();
                    rebuildPages();
                    currentPage = 0;
                    renderHomePages(false);
                }
            } else if (action.equals("Remove from Home")) {
                homePackages.remove(app.activityInfo.packageName);
                prefs.edit().remove(KEY_ICON_X + app.activityInfo.packageName).remove(KEY_ICON_Y + app.activityInfo.packageName).apply();
                currentPage = Math.max(0, Math.min(1, currentPage));
                persistHomeApps(); rebuildPages(); renderHomePages(false);
            } else {
                addToHome(app.activityInfo.packageName);
                showHome();
            }
        });
    }

    private void showActionDialog(String title, String[] icons, String[] labels, android.content.DialogInterface.OnClickListener listener) {
        Dialog dialog = new Dialog(this);
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(LauncherUiUtils.dp(this, 18), LauncherUiUtils.dp(this, 16), LauncherUiUtils.dp(this, 18), LauncherUiUtils.dp(this, 16));
        panel.setBackground(LauncherUiUtils.roundRect(this, new int[]{Color.rgb(28, 35, 59), Color.rgb(15, 19, 34)}, LauncherUiUtils.dp(this, 24)));
        TextView heading = LauncherUiUtils.text(this, title, 20, Color.WHITE, true);
        heading.setPadding(LauncherUiUtils.dp(this, 4), LauncherUiUtils.dp(this, 4), LauncherUiUtils.dp(this, 4), LauncherUiUtils.dp(this, 12));
        panel.addView(heading, new LinearLayout.LayoutParams(-1, -2));
        for (int i = 0; i < labels.length; i++) {
            final int index = i;
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(LauncherUiUtils.dp(this, 10), LauncherUiUtils.dp(this, 7), LauncherUiUtils.dp(this, 12), LauncherUiUtils.dp(this, 7));
            row.setBackground(LauncherUiUtils.roundRect(this, new int[]{Color.rgb(42, 51, 79), Color.rgb(31, 38, 61)}, LauncherUiUtils.dp(this, 15)));
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, LauncherUiUtils.dp(this, 54));
            rowParams.topMargin = LauncherUiUtils.dp(this, 5);
            TextView glyph = LauncherUiUtils.text(this, icons[i], 21, Color.rgb(178, 198, 255), true);
            glyph.setGravity(Gravity.CENTER);
            glyph.setBackground(LauncherUiUtils.roundRect(this, new int[]{Color.rgb(65, 83, 139), Color.rgb(51, 57, 112)}, LauncherUiUtils.dp(this, 12)));
            row.addView(glyph, new LinearLayout.LayoutParams(LauncherUiUtils.dp(this, 38), LauncherUiUtils.dp(this, 38)));
            TextView label = LauncherUiUtils.text(this, labels[i], 15, Color.rgb(245, 247, 255), false);
            label.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(0, -1, 1f);
            labelParams.leftMargin = LauncherUiUtils.dp(this, 12);
            row.addView(label, labelParams);
            row.setOnClickListener(v -> {
                dialog.dismiss();
                listener.onClick(dialog, index);
            });
            panel.addView(row, rowParams);
        }
        dialog.setContentView(panel);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            android.view.WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
            params.dimAmount = 0.58f;
            dialog.getWindow().setAttributes(params);
        }
        dialog.show();
        if (dialog.getWindow() != null) dialog.getWindow().setLayout(getResources().getDisplayMetrics().widthPixels - LauncherUiUtils.dp(this, 36), -2);
        panel.setAlpha(0f);
        panel.setScaleX(0.94f);
        panel.setScaleY(0.94f);
        panel.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(180).start();
    }

    private void beginWidgetPicker() { beginWidgetPicker(currentPage); }

    private void beginWidgetPicker(int requestedPage) {
        pendingWidgetTargetPage = Math.max(0, Math.min(1, requestedPage));
        new WidgetPickerDialog(this, widgetManager, packageManager,
                info -> beginBindingWidget(info)).show();
    }

    private void beginBindingWidget(AppWidgetProviderInfo info) {
        try {
            pendingWidgetId = widgetHost.allocateAppWidgetId();
            pendingWidgetInfo = info;
            Bundle options = new Bundle();
            if (widgetManager.bindAppWidgetIdIfAllowed(pendingWidgetId, info.provider, options)) {
                configureOrAddWidget(info);
                return;
            }
            Intent bind = new Intent(AppWidgetManager.ACTION_APPWIDGET_BIND);
            bind.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, pendingWidgetId);
            bind.putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider);
            bind.putExtra(AppWidgetManager.EXTRA_APPWIDGET_OPTIONS, options);
            startActivityForResult(bind, REQUEST_WIDGET_BIND);
        } catch (Exception e) {
            discardPendingWidget();
            Toast.makeText(this, "Couldn't start widget setup", Toast.LENGTH_LONG).show();
        }
    }

    private void configureOrAddWidget(AppWidgetProviderInfo info) {
        if (info == null || pendingWidgetId < 0) return;
        if (info.configure != null) {
            try {
                Intent configure = new Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
                configure.setComponent(info.configure);
                configure.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, pendingWidgetId);
                startActivityForResult(configure, REQUEST_WIDGET_CONFIGURE);
                return;
            } catch (Exception ignored) {}
        }
        addWidgetToPage(info, pendingWidgetId, pendingWidgetTargetPage);
        pendingWidgetId = -1;
        pendingWidgetInfo = null;
    }

    private void discardPendingWidget() {
        if (pendingWidgetId >= 0) {
            try { widgetHost.deleteAppWidgetId(pendingWidgetId); } catch (Exception ignored) {}
        }
        pendingWidgetId = -1;
        pendingWidgetInfo = null;
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_WIDGET_BIND || requestCode == REQUEST_WIDGET_CONFIGURE) {
            if (resultCode != RESULT_OK || pendingWidgetId < 0) {
                discardPendingWidget();
                return;
            }
            AppWidgetProviderInfo info = widgetManager.getAppWidgetInfo(pendingWidgetId);
            if (info == null) info = pendingWidgetInfo;
            if (info == null) {
                discardPendingWidget();
                Toast.makeText(this, "The selected widget could not be bound", Toast.LENGTH_LONG).show();
                return;
            }
            if (requestCode == REQUEST_WIDGET_BIND) {
                AppWidgetProviderInfo boundInfo = widgetManager.getAppWidgetInfo(pendingWidgetId);
                if (boundInfo == null) {
                    discardPendingWidget();
                    Toast.makeText(this, "Widget permission was not granted", Toast.LENGTH_LONG).show();
                    return;
                }
                configureOrAddWidget(boundInfo);
                return;
            }
            addWidgetToPage(info, pendingWidgetId, pendingWidgetTargetPage);
            pendingWidgetId = -1;
            pendingWidgetInfo = null;
            return;
        }
        if (requestCode != REQUEST_WALLPAPER || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        java.io.File savedImage = new java.io.File(getFilesDir(), "launcher_wallpaper_image");
        java.io.File tempImage = new java.io.File(getFilesDir(), "launcher_wallpaper_image.tmp");
        try {
            // Copy picker content into app-private storage so the image remains readable
            // even when the document provider grants only temporary URI access.
            try (java.io.InputStream in = getContentResolver().openInputStream(uri);
                 java.io.FileOutputStream out = new java.io.FileOutputStream(tempImage)) {
                if (in == null) throw new java.io.IOException("No image stream");
                byte[] buffer = new byte[16384];
                int count;
                while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
            }
            BitmapFactory.Options check = new BitmapFactory.Options();
            check.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(tempImage.getAbsolutePath(), check);
            if (check.outWidth <= 0 || check.outHeight <= 0) throw new java.io.IOException("Unreadable image");
            if (savedImage.exists() && !savedImage.delete()) throw new java.io.IOException("Cannot replace saved image");
            if (!tempImage.renameTo(savedImage)) {
                try (java.io.InputStream in = new java.io.FileInputStream(tempImage);
                     java.io.FileOutputStream out = new java.io.FileOutputStream(savedImage)) {
                    byte[] buffer = new byte[16384];
                    int count;
                    while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
                }
                tempImage.delete();
            }
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(savedImage.getAbsolutePath(), bounds);
            int screenW = Math.max(1, getResources().getDisplayMetrics().widthPixels);
            int screenH = Math.max(1, getResources().getDisplayMetrics().heightPixels);
            int sample = 1;
            while (bounds.outWidth / (sample * 2) > screenW * 2 || bounds.outHeight / (sample * 2) > screenH * 2) sample *= 2;
            BitmapFactory.Options decoded = new BitmapFactory.Options();
            decoded.inSampleSize = sample;
            Bitmap preview = BitmapFactory.decodeFile(savedImage.getAbsolutePath(), decoded);
            if (preview == null) throw new java.io.IOException("Unreadable image");
            showWallpaperCropper(preview, savedImage);
        } catch (Exception e) {
            tempImage.delete();
            Toast.makeText(this, "Couldn't read that image. Try selecting a locally saved copy.", Toast.LENGTH_LONG).show();
        }
    }

    private void showWallpaperCropper(Bitmap bitmap, java.io.File savedImage) {
        int screenW = getResources().getDisplayMetrics().widthPixels;
        int screenH = getResources().getDisplayMetrics().heightPixels;
        int cropH = Math.max(LauncherUiUtils.dp(this, 220), Math.min((int)(screenH * 0.56f), screenH - LauncherUiUtils.dp(this, 190)));
        int cropW = Math.max(LauncherUiUtils.dp(this, 120), Math.round(cropH * screenW / (float)Math.max(1, screenH)));
        WallpaperCropView preview = new WallpaperCropView(this, bitmap);
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(LauncherUiUtils.dp(this, 14), LauncherUiUtils.dp(this, 14), LauncherUiUtils.dp(this, 14), LauncherUiUtils.dp(this, 12));
        panel.setBackground(LauncherUiUtils.roundRect(this, new int[]{Color.rgb(24, 31, 53), Color.rgb(12, 16, 29)}, LauncherUiUtils.dp(this, 22)));
        panel.addView(LauncherUiUtils.text(this, "Crop wallpaper", 20, Color.WHITE, true));
        TextView hint = LauncherUiUtils.text(this, "Drag the image to adjust framing", 12, Color.rgb(185, 198, 225), false);
        hint.setPadding(0, LauncherUiUtils.dp(this, 4), 0, LauncherUiUtils.dp(this, 10));
        panel.addView(hint);
        FrameLayout frame = new FrameLayout(this);
        frame.setBackgroundColor(Color.BLACK);
        frame.addView(preview, new FrameLayout.LayoutParams(cropW, cropH, Gravity.CENTER));
        panel.addView(frame, new LinearLayout.LayoutParams(-1, cropH));
        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        TextView cancel = LauncherUiUtils.text(this, "Cancel", 14, Color.LTGRAY, true);
        cancel.setPadding(LauncherUiUtils.dp(this, 14), LauncherUiUtils.dp(this, 12), LauncherUiUtils.dp(this, 14), LauncherUiUtils.dp(this, 12));
        TextView use = LauncherUiUtils.text(this, "Use wallpaper", 14, Color.WHITE, true);
        use.setPadding(LauncherUiUtils.dp(this, 14), LauncherUiUtils.dp(this, 12), LauncherUiUtils.dp(this, 14), LauncherUiUtils.dp(this, 12));
        actions.addView(cancel); actions.addView(use); panel.addView(actions);
        Dialog dialog = new Dialog(this);
        dialog.setContentView(panel);
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(Math.min(screenW - LauncherUiUtils.dp(this, 24), cropW + LauncherUiUtils.dp(this, 36)), -2);
        }
        cancel.setOnClickListener(v -> { dialog.dismiss(); if (!bitmap.isRecycled()) bitmap.recycle(); });
        use.setOnClickListener(v -> {
            try {
                Bitmap cropped = preview.crop();
                try (java.io.FileOutputStream out = new java.io.FileOutputStream(savedImage)) {
                    cropped.compress(Bitmap.CompressFormat.JPEG, 88, out);
                }
                cropped.recycle();
                if (!bitmap.isRecycled()) bitmap.recycle();
                prefs.edit().putString(KEY_WALLPAPER_URI, Uri.fromFile(savedImage).toString()).apply();
                dialog.dismiss();
                if (applyHomeWallpaper()) Toast.makeText(this, "Wallpaper updated", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Couldn't crop this image", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void addWidgetToPage(AppWidgetProviderInfo info, int widgetId, int requestedPage) {
        if (pagesStrip == null || info == null) {
            Toast.makeText(this, "Open the home screen and try again", Toast.LENGTH_SHORT).show();
            return;
        }
        final int targetPage = Math.max(0, Math.min(1, requestedPage));
        currentPage = targetPage;
        rebuildPages();
        ensurePageExists(targetPage);
        renderHomePages(false);
        showPage(targetPage, false);
        // Wait for the page to be measured before sizing the host view. Immediately
        // after renderHomePages(), FrameLayout.getWidth()/getHeight() are often zero.
        pagesStrip.post(() -> {
            if (isFinishing() || pagesStrip == null) return;
            ensurePageExists(targetPage);
            if (targetPage >= pagesStrip.getChildCount()) {
                renderHomePages(false);
                return;
            }
            FrameLayout page = (FrameLayout) pagesStrip.getChildAt(targetPage);
            if (page == null) return;
            int availableWidth = page.getWidth() > 0 ? page.getWidth()
                    : Math.max(LauncherUiUtils.dp(this, 180), getResources().getDisplayMetrics().widthPixels - LauncherUiUtils.dp(this, 40));
            int availableHeight = page.getHeight() > 0 ? page.getHeight()
                    : Math.max(LauncherUiUtils.dp(this, 240), getResources().getDisplayMetrics().heightPixels - LauncherUiUtils.dp(this, 260));
            int minWidth = LauncherUiUtils.dp(this, Math.max(0, info.minWidth));
            int minHeight = LauncherUiUtils.dp(this, Math.max(0, info.minHeight));
            int width = Math.min(Math.max(LauncherUiUtils.dp(this, 180), minWidth),
                    Math.max(LauncherUiUtils.dp(this, 140), availableWidth - LauncherUiUtils.dp(this, 16)));
            int height = Math.min(Math.max(LauncherUiUtils.dp(this, 100), minHeight),
                    Math.max(LauncherUiUtils.dp(this, 90), availableHeight - LauncherUiUtils.dp(this, 80)));
            AppWidgetHostView widget = widgetHost.createView(this, widgetId, info);
            widget.setAppWidget(widgetId, info);
            addWidgetContainer(page, widget, widgetId, targetPage, LauncherUiUtils.dp(this, 8),
                    LauncherUiUtils.dp(this, 8), width, height);
            FrameLayout container = null;
            for (int i = 0; i < page.getChildCount(); i++) {
                View child = page.getChildAt(i);
                if (("launcher_widget:" + widgetId).equals(child.getTag()) && child instanceof FrameLayout) {
                    container = (FrameLayout) child;
                    break;
                }
            }
            if (container != null) {
                WidgetController.updateWidgetSize(widgetManager, widgetId, widget, getResources().getDisplayMetrics().density,
                        container.getLayoutParams().width, container.getLayoutParams().height);
                saveWidgetGeometry(widgetId, targetPage, container);
                final FrameLayout placedWidget = container;
                page.post(() -> resolveWidgetIconCollisions(page, placedWidget));
            } else {
                String stored = prefs.getString(KEY_WIDGET_IDS, "");
                String entry = widgetId + ":" + targetPage;
                if (!containsWidgetId(stored, widgetId)) {
                    prefs.edit().putString(KEY_WIDGET_IDS, stored.isEmpty() ? entry : stored + "\n" + entry).apply();
                }
            }
            widgetHost.startListening();
        });
    }

    private boolean containsWidgetId(String stored, int widgetId) {
        return WidgetController.containsWidgetId(stored, widgetId);
    }

    private void addWidgetContainer(FrameLayout page, AppWidgetHostView widget, int widgetId, int pageIndex,
            int x, int y, int width, int height) {
        widgetInteractionController.addWidgetContainer(page, widget, widgetId, pageIndex, x, y, width, height);
    }

    private void saveWidgetGeometry(int widgetId, int pageIndex, FrameLayout container) {
        WidgetController.saveGeometry(prefs, KEY_WIDGET_IDS, widgetId, pageIndex, container);
    }

    /**
     * Reparent existing widget containers to the normalized model page without destroying
     * their AppWidgetHostView instances. Android Launcher3 follows the same principle of
     * preserving workspace items while page membership changes.
     */
    private void clearFocusRecursively(View view) {
        if (view == null) return;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = group.getChildCount() - 1; i >= 0; i--) {
                clearFocusRecursively(group.getChildAt(i));
            }
        }
        if (view.hasFocus()) view.clearFocus();
    }

    private void syncWidgetViewsToPages() {
        if (pagesStrip == null || pagesStrip.getChildCount() == 0) return;
        String stored = prefs.getString(KEY_WIDGET_IDS, "");
        for (int pageIndex = pagesStrip.getChildCount() - 1; pageIndex >= 0; pageIndex--) {
            View pageView = pagesStrip.getChildAt(pageIndex);
            if (!(pageView instanceof FrameLayout)) continue;
            FrameLayout sourcePage = (FrameLayout) pageView;
            for (int childIndex = sourcePage.getChildCount() - 1; childIndex >= 0; childIndex--) {
                View child = sourcePage.getChildAt(childIndex);
                Object rawTag = child.getTag();
                if (!(rawTag instanceof String)
                        || !((String) rawTag).startsWith("launcher_widget:")) continue;
                int widgetId;
                try {
                    widgetId = Integer.parseInt(((String) rawTag).substring("launcher_widget:".length()));
                } catch (NumberFormatException ignored) {
                    sourcePage.removeViewAt(childIndex);
                    continue;
                }
                int targetPageIndex = -1;
                if (stored != null) {
                    for (String entry : stored.split("\\n")) {
                        String[] parts = entry.split(":");
                        if (parts.length != 2) continue;
                        try {
                            if (Integer.parseInt(parts[0].trim()) == widgetId) {
                                targetPageIndex = Integer.parseInt(parts[1].trim());
                                break;
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                }
                if (targetPageIndex < 0) {
                    clearFocusRecursively(child);
                    sourcePage.removeViewAt(childIndex);
                    continue;
                }
                targetPageIndex = Math.max(0, Math.min(pages.size() - 1, targetPageIndex));
                if (targetPageIndex == pageIndex || targetPageIndex >= pagesStrip.getChildCount()) continue;
                clearFocusRecursively(child);
                FrameLayout targetPage = (FrameLayout) pagesStrip.getChildAt(targetPageIndex);
                if (child.hasFocus()) child.clearFocus();
                ViewGroup parent = (ViewGroup) child.getParent();
                ViewGroup.LayoutParams oldParams = child.getLayoutParams();
                parent.removeView(child);
                targetPage.addView(child, oldParams);
                child.bringToFront();
            }
        }
    }

    private void restoreWidgetsOnPages() {
        if (pagesStrip == null) return;
        // Restore only after layout: providers often need real dimensions to render
        // correctly, and zero-sized host views can make widgets look broken.
        pagesStrip.post(() -> {
            if (pagesStrip == null || isFinishing()) return;
            String stored = prefs.getString(KEY_WIDGET_IDS, "");
            if (stored.isEmpty()) return;
            boolean removedStaleWidget = false;
            for (String entry : stored.split("\\n")) {
                String[] parts = entry.split(":");
                if (parts.length < 2) continue;
                try {
                    int id = Integer.parseInt(parts[0]);
                    int pageIndex = Integer.parseInt(parts[1]);
                    if (pageIndex < 0 || pageIndex >= pagesStrip.getChildCount()) continue;
                    FrameLayout page = (FrameLayout) pagesStrip.getChildAt(pageIndex);
                    boolean exists = false;
                    for (int i = 0; i < page.getChildCount(); i++) {
                        Object tag = page.getChildAt(i).getTag();
                        if (("launcher_widget:" + id).equals(tag)) { exists = true; break; }
                    }
                    if (exists) continue;
                    AppWidgetProviderInfo info = widgetManager.getAppWidgetInfo(id);
                    if (info == null) {
                        WidgetController.removeWidget(prefs, KEY_WIDGET_IDS, id);
                        removedStaleWidget = true;
                        continue;
                    }
                    int availableWidth = page.getWidth() > 0 ? page.getWidth()
                            : Math.max(LauncherUiUtils.dp(this, 180), getResources().getDisplayMetrics().widthPixels - LauncherUiUtils.dp(this, 40));
                    int availableHeight = page.getHeight() > 0 ? page.getHeight()
                            : Math.max(LauncherUiUtils.dp(this, 240), getResources().getDisplayMetrics().heightPixels - LauncherUiUtils.dp(this, 260));
                    int defaultWidth = Math.min(Math.max(LauncherUiUtils.dp(this, 180), LauncherUiUtils.dp(this, Math.max(0, info.minWidth))),
                            Math.max(LauncherUiUtils.dp(this, 140), availableWidth - LauncherUiUtils.dp(this, 16)));
                    int defaultHeight = Math.min(Math.max(LauncherUiUtils.dp(this, 100), LauncherUiUtils.dp(this, Math.max(0, info.minHeight))),
                            Math.max(LauncherUiUtils.dp(this, 90), availableHeight - LauncherUiUtils.dp(this, 80)));
                    int width = Math.min(Math.max(LauncherUiUtils.dp(this, 140), prefs.getInt("widget_w_" + id, defaultWidth)),
                            Math.max(LauncherUiUtils.dp(this, 140), availableWidth));
                    int height = Math.min(Math.max(LauncherUiUtils.dp(this, 90), prefs.getInt("widget_h_" + id, defaultHeight)),
                            Math.max(LauncherUiUtils.dp(this, 90), availableHeight));
                    int x = Math.max(0, Math.min(prefs.getInt("widget_x_" + id, LauncherUiUtils.dp(this, 8)),
                            Math.max(0, availableWidth - width)));
                    int y = Math.max(0, Math.min(prefs.getInt("widget_y_" + id, LauncherUiUtils.dp(this, 8)),
                            Math.max(0, availableHeight - height)));
                    AppWidgetHostView widget = widgetHost.createView(this, id, info);
                    widget.setAppWidget(id, info);
                    addWidgetContainer(page, widget, id, pageIndex, x, y, width, height);
                    WidgetController.updateWidgetSize(widgetManager, id, widget, getResources().getDisplayMetrics().density, width, height);
                    final View candidate = page.findViewWithTag("launcher_widget:" + id);
                    if (candidate instanceof FrameLayout) page.post(() -> resolveWidgetIconCollisions(page, (FrameLayout) candidate));
                } catch (Exception ignored) {}
            }
            if (removedStaleWidget) renderHomePages(false);
        });
    }

    private boolean touchStartedOnAppCell(float rawX, float rawY) {
        View target = findViewAt(root, (int) rawX, (int) rawY);
        while (target != null) {
            if ("launcher_app_cell".equals(target.getTag())
                    || (target.getTag() instanceof String && (((String) target.getTag()).startsWith("launcher_app:")
                    || ((String) target.getTag()).startsWith("launcher_widget:")
                    || ((String) target.getTag()).startsWith("launcher_widget_content:")))) return true;
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
        return LauncherWallpaperController.apply(this, root, prefs, KEY_WALLPAPER_URI, KEY_COLOR_THEME, BG);
    }

    private void launchApp(ResolveInfo app) {
        Intent intent = packageManager.getLaunchIntentForPackage(app.activityInfo.packageName);
        if (intent != null) {
            try { startActivity(intent); } catch (Exception e) { Toast.makeText(this, "Unable to open app", Toast.LENGTH_SHORT).show(); }
        }
    }

    private void applyClockStyle() {
        int style = prefs == null ? 0 : prefs.getInt(KEY_CLOCK_STYLE, 0);
        LauncherClockStyler.apply(this, clockText, style);
    }

    private void updateClock() {
        if (clockText == null || dateText == null) return;
        Date now = new Date();
        // Follow Android's system time format, but keep the AM/PM marker in a
        // separate small bold label so it never inherits the large digital font.
        String clockValue = DateFormat.getTimeFormat(this).format(now);
        boolean use24Hour = DateFormat.is24HourFormat(this);
        if (amPmText != null) {
            if (use24Hour) {
                amPmText.setText("");
                amPmText.setVisibility(View.GONE);
            } else {
                String marker = new SimpleDateFormat("a", Locale.getDefault()).format(now);
                int markerAt = clockValue.toUpperCase(Locale.getDefault())
                        .lastIndexOf(marker.toUpperCase(Locale.getDefault()));
                if (markerAt >= 0) {
                    clockValue = (clockValue.substring(0, markerAt)
                            + clockValue.substring(markerAt + marker.length())).trim();
                }
                amPmText.setText(marker);
                amPmText.setVisibility(View.VISIBLE);
            }
        }
        clockText.setText(clockValue.trim());
        dateText.setVisibility(prefs.getBoolean(KEY_SHOW_DATE, true) ? View.VISIBLE : View.GONE);
        dateText.setText(new SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(now));
        applyClockStyle();
        applyClockPanelColor(true);
        applyBrandVisibility();
    }

    private void applyClockPanelColor(boolean animate) {
        if (homeScreen == null) return;
        View hero = clockPanel;
        if (hero == null) return;
        int minute = new Date().getMinutes();
        int phase = (minute / 5) % 4;
        int[][] tones = new int[][] {
                {115, 159, 255}, {119, 137, 255}, {179, 124, 255}, {90, 190, 196}
        };
        int[] tone = tones[phase];
        int side = Color.argb(149, tone[0], tone[1], tone[2]); // 50% more opacity than the previous alpha 99.
        int centre = Color.argb(0, tone[0], tone[1], tone[2]);
        GradientDrawable next = new GradientDrawable(GradientDrawable.Orientation.RIGHT_LEFT,
                new int[] {side, Color.argb(63, tone[0], tone[1], tone[2]), centre});
        next.setCornerRadius(LauncherUiUtils.dp(this, 18));
        if (animate) {
            hero.setBackground(next);
            next.setAlpha(0);
            android.animation.ValueAnimator fade = android.animation.ValueAnimator.ofInt(0, 255);
            fade.setDuration(260L);
            fade.addUpdateListener(a -> next.setAlpha((Integer) a.getAnimatedValue()));
            fade.start();
        } else {
            hero.setBackground(next);
        }
    }

    private void resolveWidgetIconCollisions(FrameLayout page, FrameLayout widgetContainer) {
        if (page == null || widgetContainer == null || page.getWidth() <= 0 || page.getHeight() <= 0) return;
        int columns = getHomeColumns();
        int rows = HOME_ROWS;
        int cellWidth = Math.max(1, page.getWidth() / columns);
        int cellHeight = Math.max(1, page.getHeight() / rows);
        FrameLayout.LayoutParams widgetLp = (FrameLayout.LayoutParams) widgetContainer.getLayoutParams();
        int wx1 = widgetLp.leftMargin, wy1 = widgetLp.topMargin;
        int wx2 = wx1 + widgetContainer.getWidth(), wy2 = wy1 + widgetContainer.getHeight();
        boolean[][] occupied = new boolean[rows][columns];
        for (int i = 0; i < page.getChildCount(); i++) {
            View child = page.getChildAt(i);
            if (child == widgetContainer || !(child.getTag() instanceof String)
                    || !((String) child.getTag()).startsWith("launcher_app:")) continue;
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) child.getLayoutParams();
            int col = Math.max(0, Math.min(columns - 1, Math.round((float) lp.leftMargin / cellWidth)));
            int row = Math.max(0, Math.min(rows - 1, Math.round((float) lp.topMargin / cellHeight)));
            occupied[row][col] = true;
        }
        for (int i = 0; i < page.getChildCount(); i++) {
            View child = page.getChildAt(i);
            if (child == widgetContainer || !(child.getTag() instanceof String)
                    || !((String) child.getTag()).startsWith("launcher_app:")) continue;
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) child.getLayoutParams();
            int cx1 = lp.leftMargin, cy1 = lp.topMargin;
            int cx2 = cx1 + cellWidth, cy2 = cy1 + cellHeight;
            boolean overlaps = cx1 < wx2 && cx2 > wx1 && cy1 < wy2 && cy2 > wy1;
            if (!overlaps) continue;
            String pkg = ((String) child.getTag()).substring("launcher_app:".length());
            int fromCol = Math.max(0, Math.min(columns - 1, Math.round((float) lp.leftMargin / cellWidth)));
            int fromRow = Math.max(0, Math.min(rows - 1, Math.round((float) lp.topMargin / cellHeight)));
            occupied[fromRow][fromCol] = false;
            int bestCol = -1, bestRow = -1, bestDistance = Integer.MAX_VALUE;
            for (int row = 0; row < rows; row++) {
                for (int col = 0; col < columns; col++) {
                    if (occupied[row][col]) continue;
                    int x1 = col * cellWidth, y1 = row * cellHeight;
                    if (x1 < wx2 && x1 + cellWidth > wx1 && y1 < wy2 && y1 + cellHeight > wy1) continue;
                    int distance = Math.abs(col - fromCol) + Math.abs(row - fromRow);
                    if (distance < bestDistance) { bestDistance = distance; bestCol = col; bestRow = row; }
                }
            }
            if (bestCol >= 0) {
                lp.leftMargin = bestCol * cellWidth;
                lp.topMargin = bestRow * cellHeight;
                child.setLayoutParams(lp);
                child.setTranslationX(LauncherUiUtils.dp(this, 5));
                child.setTranslationY(LauncherUiUtils.dp(this, 3));
                child.animate().translationX(0f).translationY(0f).setDuration(150L).start();
                occupied[bestRow][bestCol] = true;
                saveIconSlot(pkg, bestCol, bestRow, columns);
            }
        }
    }

    private int getHomeColumns() { return Math.max(3, Math.min(6, prefs == null ? COLUMNS : prefs.getInt(KEY_COLUMNS, COLUMNS))); }
    private int getDrawerColumns() { return Math.max(3, Math.min(6, prefs == null ? COLUMNS : prefs.getInt(KEY_DRAWER_COLUMNS, COLUMNS))); }
    private int pageCapacity() { return getHomeColumns() * HOME_ROWS; }

    private void clearIconPositions() {
        SharedPreferences.Editor editor = prefs.edit();
        for (String key : prefs.getAll().keySet()) {
            if (key.startsWith(KEY_ICON_X) || key.startsWith(KEY_ICON_Y)) editor.remove(key);
        }
        editor.apply();
    }

    private void saveIconSlot(String packageName, int col, int row, int columns) {
        prefs.edit().putFloat(KEY_ICON_X + packageName, col / (float) Math.max(1, columns - 1))
                .putFloat(KEY_ICON_Y + packageName, row / (float) Math.max(1, HOME_ROWS - 1)).apply();
    }

    private GradientDrawable drawerBackground() {
        int theme = prefs == null ? 0 : prefs.getInt(KEY_COLOR_THEME, 0);
        if (theme == 1) return new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{Color.rgb(8, 43, 63), Color.rgb(8, 17, 31)});
        if (theme == 2) return new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{Color.rgb(49, 24, 76), Color.rgb(12, 13, 29)});
        if (theme == 3) return new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{Color.rgb(36, 42, 55), Color.rgb(12, 15, 23)});
        return new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{Color.rgb(15, 20, 36), Color.rgb(10, 13, 24)});
    }

    private void addSection(LinearLayout panel, String title) {
        TextView header = LauncherUiUtils.text(this, title.toUpperCase(Locale.getDefault()), 11, Color.rgb(146, 171, 239), true);
        header.setLetterSpacing(0.10f);
        header.setPadding(LauncherUiUtils.dp(this, 4), LauncherUiUtils.dp(this, 15), LauncherUiUtils.dp(this, 4), LauncherUiUtils.dp(this, 7));
        panel.addView(header, new LinearLayout.LayoutParams(-1, -2));
    }

    private void addSettingRow(LinearLayout panel, String icon, String title, String detail, Runnable action) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(LauncherUiUtils.dp(this, 10), LauncherUiUtils.dp(this, 8), LauncherUiUtils.dp(this, 10), LauncherUiUtils.dp(this, 8));
        row.setBackground(LauncherUiUtils.roundRect(this, new int[]{Color.rgb(36, 44, 68), Color.rgb(25, 31, 50)}, LauncherUiUtils.dp(this, 15)));
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, -2);
        rp.bottomMargin = LauncherUiUtils.dp(this, 6);
        TextView glyph = LauncherUiUtils.text(this, icon, 19, Color.rgb(177, 198, 255), true);
        glyph.setGravity(Gravity.CENTER);
        row.addView(glyph, new LinearLayout.LayoutParams(LauncherUiUtils.dp(this, 34), LauncherUiUtils.dp(this, 38)));
        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.setPadding(LauncherUiUtils.dp(this, 9), 0, 0, 0);
        copy.addView(LauncherUiUtils.text(this, title, 14, Color.WHITE, true));
        TextView sub = LauncherUiUtils.text(this, detail, 11, Color.rgb(176, 187, 213), false);
        sub.setPadding(0, LauncherUiUtils.dp(this, 2), 0, 0);
        copy.addView(sub);
        row.addView(copy, new LinearLayout.LayoutParams(0, -2, 1f));
        row.setOnClickListener(v -> action.run());
        panel.addView(row, rp);
    }

    private void addChoiceRow(LinearLayout panel, String title, int selected, String[] choices, IntAction onSelect) {
        TextView heading = LauncherUiUtils.text(this, title, 14, Color.WHITE, true);
        heading.setPadding(LauncherUiUtils.dp(this, 4), LauncherUiUtils.dp(this, 5), LauncherUiUtils.dp(this, 4), LauncherUiUtils.dp(this, 8));
        panel.addView(heading, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout choicesRow = new LinearLayout(this);
        choicesRow.setGravity(Gravity.CENTER_VERTICAL);
        for (int i = 0; i < choices.length; i++) {
            final int value = i;
            TextView chip = LauncherUiUtils.text(this, choices[i], 12, i == selected ? Color.WHITE : Color.rgb(196, 205, 228), i == selected);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(LauncherUiUtils.dp(this, 5), LauncherUiUtils.dp(this, 10), LauncherUiUtils.dp(this, 5), LauncherUiUtils.dp(this, 10));
            chip.setBackground(LauncherUiUtils.roundRect(this, i == selected ? new int[]{Color.rgb(73, 99, 177), Color.rgb(73, 74, 157)} : new int[]{Color.rgb(34, 41, 62), Color.rgb(28, 34, 51)}, LauncherUiUtils.dp(this, 12)));
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, LauncherUiUtils.dp(this, 40), 1f);
            cp.setMargins(LauncherUiUtils.dp(this, 2), 0, LauncherUiUtils.dp(this, 2), LauncherUiUtils.dp(this, 8));
            choicesRow.addView(chip, cp);
            chip.setOnClickListener(v -> onSelect.onSelect(value));
        }
        panel.addView(choicesRow);
    }

    private void showCustomizationMenu() {
        showCustomizationMenu(null, 0, true);
    }

    private void showCustomizationMenu(Dialog existingDialog, int restoreScrollY, boolean animateOpening) {
        Dialog dialog = existingDialog == null ? new Dialog(this) : existingDialog;
        // Keep the same dialog/window/scroll container while a setting changes.
        // Replacing the whole dialog content on every tap caused the visible flicker.
        boolean reuseContent = existingDialog != null && existingDialog.isShowing()
                && existingDialog.getWindow() != null
                && existingDialog.getWindow().getDecorView().findViewWithTag("home_studio_panel") instanceof LinearLayout
                && existingDialog.getWindow().getDecorView().findViewWithTag("home_studio_scroll") instanceof android.widget.ScrollView
                && existingDialog.getWindow().getDecorView().findViewWithTag("home_studio_content") instanceof LinearLayout;
        LinearLayout panel;
        android.widget.ScrollView scroll;
        LinearLayout content;
        if (reuseContent) {
            android.view.View decor = dialog.getWindow().getDecorView();
            panel = (LinearLayout) decor.findViewWithTag("home_studio_panel");
            scroll = (android.widget.ScrollView) decor.findViewWithTag("home_studio_scroll");
            content = (LinearLayout) decor.findViewWithTag("home_studio_content");
            content.removeAllViews();
        } else {
            panel = new LinearLayout(this);
            panel.setTag("home_studio_panel");
            panel.setOrientation(LinearLayout.VERTICAL);
            panel.setPadding(LauncherUiUtils.dp(this, 16), LauncherUiUtils.dp(this, 12), LauncherUiUtils.dp(this, 16), LauncherUiUtils.dp(this, 14));
            panel.setBackground(LauncherUiUtils.roundRect(this, new int[]{Color.rgb(24, 31, 53), Color.rgb(12, 16, 29)}, LauncherUiUtils.dp(this, 24)));
            TextView title = LauncherUiUtils.text(this, "Home Studio", 23, Color.WHITE, true);
            title.setPadding(LauncherUiUtils.dp(this, 4), LauncherUiUtils.dp(this, 5), LauncherUiUtils.dp(this, 4), LauncherUiUtils.dp(this, 3));
            panel.addView(title);
            TextView subtitle = LauncherUiUtils.text(this, "Personalize your home and app drawer", 12, Color.rgb(179, 191, 221), false);
            subtitle.setPadding(LauncherUiUtils.dp(this, 4), 0, LauncherUiUtils.dp(this, 4), LauncherUiUtils.dp(this, 7));
            panel.addView(subtitle);
            scroll = new android.widget.ScrollView(this);
            scroll.setTag("home_studio_scroll");
            content = new LinearLayout(this);
            content.setTag("home_studio_content");
            content.setOrientation(LinearLayout.VERTICAL);
            scroll.addView(content, new android.widget.ScrollView.LayoutParams(-1, -2));
        }

        addSection(content, "Home screen");
        addChoiceRow(content, "Home columns", getHomeColumns() - 3, new String[]{"3", "4", "5", "6"}, value -> {
            prefs.edit().putInt(KEY_COLUMNS, value + 3).apply();
            clearIconPositions();
            currentPage = Math.min(currentPage, Math.max(0, (homePackages.size() - 1) / pageCapacity()));
            renderHomePages(false); showCustomizationMenu(dialog, scroll.getScrollY(), false);
        });
        addChoiceRow(content, "Home icon size", sizeChoiceIndex(prefs.getInt(KEY_ICON_SIZE, 48)), new String[]{"Small", "Medium", "Large", "XL"}, value -> {
            prefs.edit().putInt(KEY_ICON_SIZE, new int[]{40, 48, 56, 64}[value]).apply();
            renderHomePages(false); showCustomizationMenu(dialog, scroll.getScrollY(), false);
        });
        addChoiceRow(content, "Home app font size", prefs.getInt(KEY_HOME_FONT_SIZE, 1), new String[]{"Small", "Medium", "Large", "XL"}, value -> {
            prefs.edit().putInt(KEY_HOME_FONT_SIZE, value).apply();
            renderHomePages(false); showCustomizationMenu(dialog, scroll.getScrollY(), false);
        });
        addSettingRow(content, "Aa", "App labels on Home", prefs.getBoolean(KEY_SHOW_HOME_LABELS, true) ? "Visible" : "Hidden", () -> {
            prefs.edit().putBoolean(KEY_SHOW_HOME_LABELS, !prefs.getBoolean(KEY_SHOW_HOME_LABELS, true)).apply();
            renderHomePages(false); showCustomizationMenu(dialog, scroll.getScrollY(), false);
        });
        addSettingRow(content, "◷", "Date under the clock", prefs.getBoolean(KEY_SHOW_DATE, true) ? "Visible" : "Hidden", () -> {
            prefs.edit().putBoolean(KEY_SHOW_DATE, !prefs.getBoolean(KEY_SHOW_DATE, true)).apply();
            updateClock(); showCustomizationMenu(dialog, scroll.getScrollY(), false);
        });
        addSettingRow(content, "◫", "Page indicator", prefs.getBoolean(KEY_SHOW_PAGE_INDICATOR, true) ? "Visible" : "Hidden", () -> {
            prefs.edit().putBoolean(KEY_SHOW_PAGE_INDICATOR, !prefs.getBoolean(KEY_SHOW_PAGE_INDICATOR, true)).apply();
            renderHomePages(false); showCustomizationMenu(dialog, scroll.getScrollY(), false);
        });
        addSettingRow(content, "D", "Launcher signature", prefs.getBoolean(KEY_SHOW_BRAND, true) ? "Visible" : "Hidden", () -> {
            prefs.edit().putBoolean(KEY_SHOW_BRAND, !prefs.getBoolean(KEY_SHOW_BRAND, true)).apply();
            applyBrandVisibility(); showCustomizationMenu(dialog, scroll.getScrollY(), false);
        });

        addSection(content, "App drawer");
        addChoiceRow(content, "Drawer columns", getDrawerColumns() - 3, new String[]{"3", "4", "5", "6"}, value -> {
            prefs.edit().putInt(KEY_DRAWER_COLUMNS, value + 3).apply();
            drawerGrid.setNumColumns(getDrawerColumns()); drawerAdapter.notifyDataSetChanged();
            showCustomizationMenu(dialog, scroll.getScrollY(), false);
        });
        addChoiceRow(content, "Drawer icon size", sizeChoiceIndex(prefs.getInt(KEY_DRAWER_ICON_SIZE, 48)), new String[]{"Small", "Medium", "Large", "XL"}, value -> {
            prefs.edit().putInt(KEY_DRAWER_ICON_SIZE, new int[]{40, 48, 56, 64}[value]).apply();
            drawerAdapter.notifyDataSetChanged(); showCustomizationMenu(dialog, scroll.getScrollY(), false);
        });
        addChoiceRow(content, "App drawer font size", prefs.getInt(KEY_DRAWER_FONT_SIZE, 1), new String[]{"Small", "Medium", "Large", "XL"}, value -> {
            prefs.edit().putInt(KEY_DRAWER_FONT_SIZE, value).apply();
            drawerAdapter.notifyDataSetChanged(); showCustomizationMenu(dialog, scroll.getScrollY(), false);
        });
        addSettingRow(content, "Aa", "App names in drawer", prefs.getBoolean(KEY_SHOW_DRAWER_LABELS, true) ? "Visible" : "Hidden", () -> {
            prefs.edit().putBoolean(KEY_SHOW_DRAWER_LABELS, !prefs.getBoolean(KEY_SHOW_DRAWER_LABELS, true)).apply();
            drawerAdapter.notifyDataSetChanged(); showCustomizationMenu(dialog, scroll.getScrollY(), false);
        });
        addSettingRow(content, "A↕", "Sort order", prefs.getBoolean(KEY_DRAWER_DESCENDING, false) ? "Z to A" : "A to Z", () -> {
            prefs.edit().putBoolean(KEY_DRAWER_DESCENDING, !prefs.getBoolean(KEY_DRAWER_DESCENDING, false)).apply();
            loadApps(); drawerAdapter.notifyDataSetChanged(); showCustomizationMenu(dialog, scroll.getScrollY(), false);
        });
        addSettingRow(content, "☷", "Drawer spacing", prefs.getInt(KEY_DRAWER_SPACING, 8) <= 5 ? "Compact" : "Comfortable", () -> {
            int spacing = prefs.getInt(KEY_DRAWER_SPACING, 8) <= 5 ? 10 : 4;
            prefs.edit().putInt(KEY_DRAWER_SPACING, spacing).apply();
            drawerGrid.setVerticalSpacing(LauncherUiUtils.dp(this, spacing)); drawerGrid.setHorizontalSpacing(LauncherUiUtils.dp(this, spacing / 2));
            showCustomizationMenu(dialog, scroll.getScrollY(), false);
        });

        addSection(content, "Appearance");
        addChoiceRow(content, "Color theme", prefs.getInt(KEY_COLOR_THEME, 0), new String[]{"Midnight", "Ocean", "Violet", "Graphite"}, value -> {
            prefs.edit().putInt(KEY_COLOR_THEME, value).apply();
            drawerScreen.setBackground(drawerBackground()); applyHomeWallpaper();
            showCustomizationMenu(dialog, scroll.getScrollY(), false);
        });
        addChoiceRow(content, "Clock style", prefs.getInt(KEY_CLOCK_STYLE, 0), new String[]{"Digital", "Cyber HUD", "Neon Circuit", "Tech Minimal", "Terminal Digital", "7-Segment Digital"}, value -> {
            prefs.edit().putInt(KEY_CLOCK_STYLE, value).apply();
            applyClockStyle();
            showCustomizationMenu(dialog, scroll.getScrollY(), false);
        });
        addSettingRow(content, "12/24", "System clock format", DateFormat.is24HourFormat(this) ? "24-hour" : "12-hour", () -> {
            dialog.dismiss();
            try { startActivity(new Intent(Settings.ACTION_DATE_SETTINGS)); }
            catch (Exception e) { Toast.makeText(this, "Open system Date & time settings to change the clock format", Toast.LENGTH_LONG).show(); }
        });
        addSettingRow(content, "▧", "Choose photo wallpaper", "Select an image from this device", () -> {
            dialog.dismiss();
            // GET_CONTENT is supported by older Android 6 gallery/file-picker apps.
            // The selected image is copied into private storage in onActivityResult.
            Intent pick = new Intent(Intent.ACTION_GET_CONTENT);
            pick.addCategory(Intent.CATEGORY_OPENABLE);
            pick.setType("image/*");
            try { startActivityForResult(Intent.createChooser(pick, "Choose wallpaper"), REQUEST_WALLPAPER); }
            catch (Exception e) {
                try {
                    Intent documents = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    documents.addCategory(Intent.CATEGORY_OPENABLE);
                    documents.setType("image/*");
                    documents.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                    startActivityForResult(documents, REQUEST_WALLPAPER);
                } catch (Exception ignored) {
                    Toast.makeText(this, "No image picker is available", Toast.LENGTH_LONG).show();
                }
            }
        });
        addSettingRow(content, "▣", "System wallpaper picker", "Open Android wallpaper settings", () -> {
            dialog.dismiss();
            try { startActivity(new Intent(Intent.ACTION_SET_WALLPAPER)); }
            catch (Exception e) { Toast.makeText(this, "System wallpaper picker unavailable", Toast.LENGTH_SHORT).show(); }
        });
        addSettingRow(content, "↺", "Reset wallpaper", "Restore the selected color theme", () -> {
            prefs.edit().remove(KEY_WALLPAPER_URI).apply(); applyHomeWallpaper();
            showCustomizationMenu(dialog, scroll.getScrollY(), false);
        });

        addSection(content, "Manage layout");
        addSettingRow(content, "▦", "Add widget", "Choose a clock, calendar, weather or other widget", () -> {
            dialog.dismiss();
            beginWidgetPicker(currentPage);
        });
        addSettingRow(content, "＋", "Add apps to Home", "Choose shortcuts for the home screen", () -> { dialog.dismiss(); showAddApps(currentPage); });
        addSettingRow(content, "⌂", "Reset Home layout", "Restore default shortcuts and grid", () -> {
            homePackages.clear();
            prefs.edit().remove(KEY_HOME_APPS).remove(KEY_COLUMNS).remove(KEY_SHOW_DATE).remove(KEY_ICON_SIZE)
                    .remove(KEY_SHOW_HOME_LABELS).remove(KEY_SHOW_PAGE_INDICATOR).apply();
            clearIconPositions();
            currentPage = 0; restoreHomeApps(); renderHomePages(false);
            showCustomizationMenu(dialog, scroll.getScrollY(), false);
        });
        addSettingRow(content, "⟲", "Reset all customizations", "Restore launcher defaults", () -> {
            prefs.edit().clear().apply();
            homePackages.clear(); currentPage = 0; restoreHomeApps();
            applyHomeWallpaper(); drawerScreen.setBackground(drawerBackground());
            drawerGrid.setNumColumns(getDrawerColumns()); drawerGrid.setVerticalSpacing(LauncherUiUtils.dp(this, 8)); drawerGrid.setHorizontalSpacing(LauncherUiUtils.dp(this, 4));
            applyBrandVisibility(); loadApps(); renderHomePages(false); showCustomizationMenu(dialog, scroll.getScrollY(), false);
        });

        if (!reuseContent) {
            panel.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
            dialog.setContentView(panel);
        }
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            android.view.WindowManager.LayoutParams wp = dialog.getWindow().getAttributes();
            wp.dimAmount = 0.60f; dialog.getWindow().setAttributes(wp);
        }
        boolean wasShowing = dialog.isShowing();
        if (!wasShowing) dialog.show();
        if (dialog.getWindow() != null) dialog.getWindow().setLayout(Math.min(getResources().getDisplayMetrics().widthPixels - LauncherUiUtils.dp(this, 24), LauncherUiUtils.dp(this, 480)),
                Math.min(getResources().getDisplayMetrics().heightPixels - LauncherUiUtils.dp(this, 70), LauncherUiUtils.dp(this, 720)));
        if (restoreScrollY > 0) scroll.post(() -> scroll.scrollTo(0, restoreScrollY));
        if (animateOpening && !wasShowing) {
            panel.setAlpha(0f); panel.setScaleX(0.96f); panel.setScaleY(0.96f);
            panel.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(190).start();
        }
    }

    private int sizeChoiceIndex(int size) {
        if (size <= 40) return 0;
        if (size <= 48) return 1;
        if (size <= 56) return 2;
        return 3;
    }

    private void applyBrandVisibility() {
        if (brandText != null) brandText.setVisibility(prefs.getBoolean(KEY_SHOW_BRAND, true) ? View.VISIBLE : View.GONE);
    }
}
