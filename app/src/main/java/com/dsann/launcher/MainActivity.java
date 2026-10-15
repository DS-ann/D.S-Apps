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
import android.widget.SeekBar;
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
    private static final String KEY_PAGE_ROTATION = "page_rotation_degrees";
    private static final String KEY_ANIMATION_SPEED = "animation_speed";
    private static final String KEY_PAGE_TRANSITION = "page_transition";
    private static final String KEY_CLOCK_SIZE = "clock_size"; // Legacy preference; retained for compatibility.
    private static final String KEY_CLOCK_LENGTH = "clock_length_percent";
    private static final String KEY_CLOCK_BREADTH = "clock_breadth_percent";
    private static final String KEY_CLOCK_POSITION = "clock_position";
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
    private static final String KEY_SHORTCUT_INTENT = "contact_shortcut_intent";
    private static final String KEY_SHORTCUT_LABEL = "contact_shortcut_label";
    private static final String KEY_SHORTCUT_PAGE = "contact_shortcut_page";
    private static final String KEY_SHORTCUT_ICON = "contact_shortcut_icon";
    private static final String KEY_SHORTCUT_SPAN_X = "contact_shortcut_span_x";
    private static final String KEY_SHORTCUT_SPAN_Y = "contact_shortcut_span_y";
    private static final String KEY_SHORTCUT_WIDTH_DP = "contact_shortcut_width_dp";
    private static final String KEY_SHORTCUT_HEIGHT_DP = "contact_shortcut_height_dp";
    private static final int REQUEST_WALLPAPER = 7301;
    private static final int REQUEST_WIDGET_PICK = 7302;
    private static final int REQUEST_WIDGET_BIND = 7303;
    private static final int REQUEST_WIDGET_CONFIGURE = 7304;
    private static final int REQUEST_CALL_PERMISSION = 7305;
    private static final int REQUEST_CREATE_SHORTCUT = 7306;
    private int pendingWidgetId = -1;
    private AppWidgetProviderInfo pendingWidgetInfo;
    private Intent pendingShortcutCallIntent;
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
    private FrameLayout clockRow;
    private FrameLayout clockGroup;
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
    private boolean gestureStartedOnWidget = false;
    private boolean gestureStartedOnContactShortcut = false;
    private boolean pageSwipeActive = false;
    private boolean pageSwipeChildCancelled = false;
    private float wheelSwipeProgress = 0f;
    private int wheelSwipeDirection = 0;
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
            // A resize outline is transient edit chrome. Dismiss it on the next tap
            // anywhere in the launcher, while preserving direct touches on its handle.
            if (widgetInteractionController != null) {
                widgetInteractionController.clearEditChromeOutsideTouch(event.getRawX(), event.getRawY());
            }
            gestureDownX = event.getX();
            gestureDownY = event.getY();
            longPressDownX = event.getX();
            longPressDownY = event.getY();
            longPressTriggered = false;
            pageSwipeActive = false;
            pageSwipeChildCancelled = false;
            drawerAtTopOnGestureDown = !drawerOpen || isDrawerAtTop();
            gestureStartedOnAppCell = touchStartedOnAppCell(event.getRawX(), event.getRawY());
            gestureStartedOnWidget = touchStartedOnWidget(event.getRawX(), event.getRawY());
            gestureStartedOnContactShortcut = touchStartedOnContactShortcut(event.getRawX(), event.getRawY());
            handler.removeCallbacks(homeLongPress);
            // Widget gestures belong to the widget interaction handler, never the
            // launcher's global long-press/customization or drawer gesture.
            if (!drawerOpen && !gestureStartedOnAppCell && !gestureStartedOnWidget)
                handler.postDelayed(homeLongPress, 2000L);
            return super.dispatchTouchEvent(event);
        }

        float dx = event.getX() - gestureDownX;
        float dy = event.getY() - gestureDownY;
        if (action == MotionEvent.ACTION_MOVE) {
            if (Math.abs(event.getX() - longPressDownX) > LauncherUiUtils.dp(this, 12)
                    || Math.abs(event.getY() - longPressDownY) > LauncherUiUtils.dp(this, 12)) {
                handler.removeCallbacks(homeLongPress);
            }
            // A true wheel/cylinder turn: the two page surfaces pivot on their shared
            // vertical edge. Keep the strip stationary while the finger controls the angle.
            if (!drawerOpen && !gestureStartedOnWidget && !gestureStartedOnContactShortcut
                    && pagesStrip != null && pagesStrip.getChildCount() > 1
                    && Math.abs(dx) > LauncherUiUtils.dp(this, 12)
                    && Math.abs(dx) > Math.abs(dy) * 1.15f) {
                int direction = dx < 0f ? 1 : -1;
                int adjacent = currentPage + direction;
                if (adjacent >= 0 && adjacent < pagesStrip.getChildCount()) {
                    if (!pageSwipeActive) {
                        pageSwipeActive = true;
                        pageSwipeChildCancelled = false;
                        wheelSwipeProgress = 0f;
                        wheelSwipeDirection = direction;
                        pagesStrip.animate().cancel();
                        View outgoing = pagesStrip.getChildAt(currentPage);
                        View incoming = pagesStrip.getChildAt(adjacent);
                        float density = getResources().getDisplayMetrics().density;
                        // Cache just the two moving surfaces during this gesture. This
                        // avoids repeatedly rebuilding their display lists on every MOVE.
                        outgoing.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                        incoming.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                        outgoing.setCameraDistance(10000f * density);
                        incoming.setCameraDistance(10000f * density);
                    } else if (wheelSwipeDirection != direction) {
                        wheelSwipeDirection = direction;
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
                    float progress = Math.max(0f, Math.min(1f, Math.abs(dx) / (float) pageWidth));
                    wheelSwipeProgress = progress;
                    applyWheelSwipe(wheelSwipeDirection, progress);
                    return true;
                }
            }
            return pageSwipeActive || super.dispatchTouchEvent(event);
        }

        if (action == MotionEvent.ACTION_CANCEL) {
            handler.removeCallbacks(homeLongPress);
            if (pageSwipeActive) {
                pageSwipeActive = false;
                int width = pageViewport != null && pageViewport.getWidth() > 0
                        ? pageViewport.getWidth() : Math.max(1, getResources().getDisplayMetrics().widthPixels);
                finishPageSwipe(currentPage, width);
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
                finishPageSwipe(destination, pageWidth);
                return true;
            }

            boolean verticalSwipe = !gestureStartedOnWidget && !gestureStartedOnAppCell && !gestureStartedOnContactShortcut
                    && Math.abs(dy) > LauncherUiUtils.dp(this, 65)
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

    private boolean touchStartedOnContactShortcut(float rawX, float rawY) {
        View target = findViewAt(root, (int) rawX, (int) rawY);
        while (target != null) {
            if ("launcher_contact_shortcut".equals(target.getTag())) return true;
            if (target == root) break;
            android.view.ViewParent parent = target.getParent();
            target = parent instanceof View ? (View) parent : null;
        }
        return false;
    }

    private boolean touchStartedOnWidget(float rawX, float rawY) {
        return viewAtRawPointIsWidget(root, rawX, rawY);
    }

    private boolean viewAtRawPointIsWidget(View view, float rawX, float rawY) {
        if (view == null || view.getVisibility() != View.VISIBLE) return false;
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        float localX = rawX - location[0];
        float localY = rawY - location[1];
        if (localX < 0 || localY < 0 || localX >= view.getWidth() || localY >= view.getHeight())
            return false;
        if (view instanceof AppWidgetHostView) return true;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = group.getChildCount() - 1; i >= 0; i--) {
                if (viewAtRawPointIsWidget(group.getChildAt(i), rawX, rawY)) return true;
            }
        }
        return false;
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
        // The app list is loaded at startup and refreshed only by package add/remove broadcasts.
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
        if (drawerAdapter != null) {
            drawerAdapter.clearPackageCaches();
            drawerAdapter.notifyDataSetChanged();
        }
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
        hero.setPadding(LauncherUiUtils.dp(this, 2), LauncherUiUtils.dp(this, 2), LauncherUiUtils.dp(this, 2), LauncherUiUtils.dp(this, 2));
        hero.setClipChildren(false);
        hero.setClipToPadding(false);
        // Give the clock the full usable width; a large LED clock must not be
        // measured inside the old 82%-width panel and clipped at its left edge.
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, -2);
        hp.gravity = Gravity.TOP;
        hp.topMargin = LauncherUiUtils.dp(this, 2);
        homeScreen.addView(hero, hp);
        clockPanel = hero;
        applyClockPanelColor(false);
        clockRow = new FrameLayout(this);
        clockRow.setClipChildren(false);
        clockRow.setClipToPadding(false);
        clockGroup = new FrameLayout(this);
        clockGroup.setClipChildren(false);
        clockGroup.setClipToPadding(false);
        clockText = new SevenSegmentClockTextView(this);
        clockText.setText("--:--");
        clockText.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        clockText.setTextSize(78);
        clockText.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, LauncherUiUtils.dp(this, 114),
                Gravity.LEFT | Gravity.CENTER_VERTICAL));
        // Satin-silver digits: softer than pure white, while retaining strong contrast.
        clockText.setTextColor(Color.rgb(196, 200, 209));
        clockText.setIncludeFontPadding(false);
        clockText.setTypeface(Typeface.create("monospace", Typeface.BOLD));
        clockText.setLetterSpacing(0f);
        applyClockStyle();
        clockGroup.addView(clockText);
        amPmText = new TextView(this);
        amPmText.setTextColor(Color.rgb(176, 182, 193));
        amPmText.setTextSize(8);
        amPmText.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        amPmText.setGravity(Gravity.RIGHT | Gravity.BOTTOM);
        amPmText.setIncludeFontPadding(false);
        amPmText.setPadding(0, 0, 0, 0);
        FrameLayout.LayoutParams markerParams = new FrameLayout.LayoutParams(
                LauncherUiUtils.dp(this, 18), LauncherUiUtils.dp(this, 12),
                Gravity.RIGHT | Gravity.BOTTOM);
        clockGroup.addView(amPmText, markerParams);
        FrameLayout.LayoutParams groupParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, LauncherUiUtils.dp(this, 114),
                clockGravity() | Gravity.CENTER_VERTICAL);
        clockRow.addView(clockGroup, groupParams);
        hero.addView(clockRow, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, LauncherUiUtils.dp(this, 114)));
        // Keep the existing 13sp date size; improve contrast instead of enlarging it.
        dateText = LauncherUiUtils.text(this, "", 13, Color.WHITE, true);
        dateText.setTypeface(Typeface.create("sans-serif-condensed-medium", Typeface.BOLD));
        dateText.setGravity(Gravity.RIGHT);
        dateText.setLetterSpacing(0.04f);
        dateText.setShadowLayer(LauncherUiUtils.dp(this, 2), 0f, LauncherUiUtils.dp(this, 1),
                Color.argb(220, 0, 0, 0));
        LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, -2);
        dp.topMargin = LauncherUiUtils.dp(this, 3);
        hero.addView(dateText, dp);
        applyClockPositionAndSize();
        applyClockPanelColor(false);

        pageViewport = new FrameLayout(this);
        // Permit the rotating faces to extend slightly beyond the viewport while turning.
        pageViewport.setClipChildren(false);
        pageViewport.setClipToPadding(false);
        pageViewport.setCameraDistance(12000f * getResources().getDisplayMetrics().density);
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(-1, 0, 1f);
        pp.topMargin = LauncherUiUtils.dp(this, 2);
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
        renderSavedContactShortcut();
        renderDock();
    }

    private void renderSavedContactShortcut() {
        if (pagesStrip == null || prefs == null) return;
        if (pageViewport == null) return;
        if (pageViewport.getWidth() <= 0 || pageViewport.getHeight() <= 0) {
            pageViewport.post(() -> renderSavedContactShortcut());
            return;
        }
        for (int i = 0; i < pagesStrip.getChildCount(); i++) {
            View pageView = pagesStrip.getChildAt(i);
            if (!(pageView instanceof ViewGroup)) continue;
            ViewGroup page = (ViewGroup) pageView;
            for (int j = page.getChildCount() - 1; j >= 0; j--) {
                View child = page.getChildAt(j);
                if ("launcher_contact_shortcut".equals(child.getTag())) page.removeViewAt(j);
            }
        }
        String savedUri = prefs.getString(KEY_SHORTCUT_INTENT, null);
        if (savedUri == null || savedUri.isEmpty() || pagesStrip.getChildCount() == 0) return;
        int desiredPage = Math.max(0, Math.min(pagesStrip.getChildCount() - 1,
                prefs.getInt(KEY_SHORTCUT_PAGE, 0)));
        int columns = Math.max(1, getHomeColumns());
        int rows = HOME_ROWS;
        int cellW = Math.max(1, pageViewport.getWidth() / columns);
        int cellH = Math.max(1, pageViewport.getHeight() / rows);
        if (pageViewport.getWidth() <= 0 || pageViewport.getHeight() <= 0) return;

        FrameLayout chosenPage = null;
        int chosenLeft = 0, chosenTop = 0;
        int shortcutSpanX = Math.max(1, Math.min(columns, prefs.getInt(KEY_SHORTCUT_SPAN_X, 1)));
        int shortcutSpanY = Math.max(1, Math.min(rows, prefs.getInt(KEY_SHORTCUT_SPAN_Y, 1)));
        for (int attempt = 0; attempt < pagesStrip.getChildCount() && chosenPage == null; attempt++) {
            int pageIndex = (desiredPage + attempt) % pagesStrip.getChildCount();
            View pageView = pagesStrip.getChildAt(pageIndex);
            if (!(pageView instanceof FrameLayout)) continue;
            FrameLayout page = (FrameLayout) pageView;
            for (int row = 0; row < rows && chosenPage == null; row++) {
                for (int col = 0; col < columns && chosenPage == null; col++) {
                    android.graphics.Rect candidate = new android.graphics.Rect(
                            col * cellW, row * cellH, (col + 1) * cellW, (row + 1) * cellH);
                    boolean occupied = false;
                    for (int childIndex = 0; childIndex < page.getChildCount(); childIndex++) {
                        View child = page.getChildAt(childIndex);
                        if ("launcher_contact_shortcut".equals(child.getTag())) continue;
                        android.graphics.Rect bounds;
                        ViewGroup.LayoutParams rawParams = child.getLayoutParams();
                        if (rawParams instanceof FrameLayout.LayoutParams) {
                            FrameLayout.LayoutParams childParams = (FrameLayout.LayoutParams) rawParams;
                            int left = childParams.leftMargin;
                            int top = childParams.topMargin;
                            int width = childParams.width > 0 ? childParams.width : child.getWidth();
                            int height = childParams.height > 0 ? childParams.height : child.getHeight();
                            bounds = new android.graphics.Rect(left, top, left + Math.max(0, width), top + Math.max(0, height));
                        } else {
                            bounds = new android.graphics.Rect(child.getLeft(), child.getTop(), child.getRight(), child.getBottom());
                        }
                        if (android.graphics.Rect.intersects(candidate, bounds)) { occupied = true; break; }
                    }
                    if (!occupied) {
                        chosenPage = page;
                        chosenLeft = col * cellW;
                        chosenTop = row * cellH;
                    }
                }
            }
        }
        if (chosenPage == null) return;

        FrameLayout tile = new FrameLayout(this);
        tile.setTag("launcher_contact_shortcut");
        tile.setClipChildren(false);
        LinearLayout tileContent = new LinearLayout(this);
        tileContent.setOrientation(LinearLayout.VERTICAL);
        tileContent.setGravity(Gravity.CENTER);
        tileContent.setPadding(LauncherUiUtils.dp(this, 3), LauncherUiUtils.dp(this, 3),
                LauncherUiUtils.dp(this, 3), LauncherUiUtils.dp(this, 3));
        tile.addView(tileContent, new FrameLayout.LayoutParams(-1, -1));
        ImageView icon = new ImageView(this);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        boolean iconLoaded = false;
        String iconFile = prefs.getString(KEY_SHORTCUT_ICON, null);
        if (iconFile != null) {
            try {
                java.io.File file = new java.io.File(getFilesDir(), iconFile);
                if (file.isFile()) {
                    Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                    if (bitmap != null) { icon.setImageBitmap(bitmap); iconLoaded = true; }
                }
            } catch (Exception ignored) {}
        }
        if (!iconLoaded) {
            try {
                Intent saved = Intent.parseUri(savedUri, Intent.URI_INTENT_SCHEME);
                if (saved.getComponent() != null) {
                    icon.setImageDrawable(packageManager.getActivityIcon(saved.getComponent()));
                    iconLoaded = true;
                }
            } catch (Exception ignored) {}
        }
        if (!iconLoaded) icon.setImageResource(android.R.drawable.sym_action_call);
        int initialIconSize = LauncherUiUtils.dp(this, 38);
        tileContent.addView(icon, new LinearLayout.LayoutParams(initialIconSize, initialIconSize));
        TextView label = LauncherUiUtils.text(this,
                prefs.getString(KEY_SHORTCUT_LABEL, "Contact shortcut"), 11, Color.WHITE, true);
        label.setGravity(Gravity.CENTER);
        label.setMaxLines(2);
        label.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(-1, -2);
        labelParams.topMargin = LauncherUiUtils.dp(this, 3);
        tileContent.addView(label, labelParams);
        int savedWidthDp = prefs.getInt(KEY_SHORTCUT_WIDTH_DP, 0);
        int savedHeightDp = prefs.getInt(KEY_SHORTCUT_HEIGHT_DP, 0);
        int tileWidth = savedWidthDp > 0
                ? Math.min(pageViewport.getWidth() - chosenLeft, LauncherUiUtils.dp(this, savedWidthDp))
                : Math.min(pageViewport.getWidth() - chosenLeft, cellW * shortcutSpanX);
        int tileHeight = savedHeightDp > 0
                ? Math.min(pageViewport.getHeight() - chosenTop, LauncherUiUtils.dp(this, savedHeightDp))
                : Math.min(pageViewport.getHeight() - chosenTop, cellH * shortcutSpanY);
        FrameLayout.LayoutParams tileParams = new FrameLayout.LayoutParams(
                Math.max(cellW, tileWidth), Math.max(cellH, tileHeight), Gravity.TOP | Gravity.LEFT);
        tileParams.leftMargin = chosenLeft;
        tileParams.topMargin = chosenTop;
        chosenPage.addView(tile, tileParams);
        // Resize handle mirrors widget editing: drag the blue lower-right grip to
        // grow/shrink the contact tile in whole grid-cell increments.
        View resizeGrip = new View(this);
        GradientDrawable gripBackground = new GradientDrawable();
        gripBackground.setColor(Color.rgb(115, 159, 255));
        gripBackground.setCornerRadius(LauncherUiUtils.dp(this, 5));
        resizeGrip.setBackground(gripBackground);
        resizeGrip.setVisibility(View.GONE);
        FrameLayout.LayoutParams gripParams = new FrameLayout.LayoutParams(
                LauncherUiUtils.dp(this, 22), LauncherUiUtils.dp(this, 22), Gravity.RIGHT | Gravity.BOTTOM);
        tile.addView(resizeGrip, gripParams);
        final boolean[] resizingShortcut = {false};
        final float[] resizeStart = {0f, 0f};
        final int[] resizeBase = {shortcutSpanX, shortcutSpanY};
        final int[] resizeBasePx = {tileWidth, tileHeight};
        resizeGrip.setOnTouchListener((view, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    resizingShortcut[0] = true;
                    resizeStart[0] = event.getRawX();
                    resizeStart[1] = event.getRawY();
                    resizeBase[0] = Math.max(1, (int) Math.ceil((float) tile.getWidth() / cellW));
                    resizeBase[1] = Math.max(1, (int) Math.ceil((float) tile.getHeight() / cellH));
                    resizeBasePx[0] = tile.getWidth();
                    resizeBasePx[1] = tile.getHeight();
                    tile.getParent().requestDisallowInterceptTouchEvent(true);
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (!resizingShortcut[0]) return true;
                    ViewGroup.LayoutParams raw = tile.getLayoutParams();
                    if (raw instanceof FrameLayout.LayoutParams) {
                        FrameLayout.LayoutParams p = (FrameLayout.LayoutParams) raw;
                        p.width = Math.max(cellW, Math.min(pageViewport.getWidth() - p.leftMargin,
                                resizeBasePx[0] + Math.round(event.getRawX() - resizeStart[0])));
                        p.height = Math.max(cellH, Math.min(pageViewport.getHeight() - p.topMargin,
                                resizeBasePx[1] + Math.round(event.getRawY() - resizeStart[1])));
                        tile.setLayoutParams(p);
                        // Grow the actual contact/app icon as the tile grows; changing only
                        // the tile bounds leaves the icon visually stuck at its original size.
                        int availableWidth = Math.max(LauncherUiUtils.dp(this, 38),
                                p.width - LauncherUiUtils.dp(this, 16));
                        int availableHeight = Math.max(LauncherUiUtils.dp(this, 38),
                                p.height - LauncherUiUtils.dp(this, 42));
                        int iconSize = Math.max(LauncherUiUtils.dp(this, 38),
                                Math.min(LauncherUiUtils.dp(this, 96),
                                        Math.min(availableWidth, availableHeight)));
                        ViewGroup.LayoutParams iconParams = icon.getLayoutParams();
                        if (iconParams != null && (iconParams.width != iconSize || iconParams.height != iconSize)) {
                            iconParams.width = iconSize;
                            iconParams.height = iconSize;
                            icon.setLayoutParams(iconParams);
                        }
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (resizingShortcut[0]) {
                        resizingShortcut[0] = false;
                        int finalX = Math.max(1, Math.min(columns, (int) Math.ceil((float) tile.getWidth() / cellW)));
                        int finalY = Math.max(1, Math.min(rows, (int) Math.ceil((float) tile.getHeight() / cellH)));
                        float density = getResources().getDisplayMetrics().density;
                        prefs.edit().putInt(KEY_SHORTCUT_SPAN_X, finalX)
                                .putInt(KEY_SHORTCUT_SPAN_Y, finalY)
                                .putInt(KEY_SHORTCUT_WIDTH_DP, Math.max(1, Math.round(tile.getWidth() / density)))
                                .putInt(KEY_SHORTCUT_HEIGHT_DP, Math.max(1, Math.round(tile.getHeight() / density)))
                                .apply();
                        if (tile.getParent() != null) tile.getParent().requestDisallowInterceptTouchEvent(false);
                        // Keep the resized tile in place; rebuilding it here can move it
                        // to another free cell and makes the resize appear to snap back.
                    }
                    return true;
            }
            return true;
        });
        tile.setOnClickListener(v -> {
            if (resizeGrip.getVisibility() == View.VISIBLE) {
                resizeGrip.setVisibility(View.GONE);
                tile.setBackgroundColor(Color.TRANSPARENT);
                return;
            }
            try {
                Intent target = Intent.parseUri(prefs.getString(KEY_SHORTCUT_INTENT, ""),
                        Intent.URI_INTENT_SCHEME);
                if (Intent.ACTION_CALL.equals(target.getAction())
                        && android.os.Build.VERSION.SDK_INT >= 23
                        && checkSelfPermission(android.Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                    pendingShortcutCallIntent = target;
                    requestPermissions(new String[]{android.Manifest.permission.CALL_PHONE}, REQUEST_CALL_PERMISSION);
                    return;
                }
                startActivity(target);
            } catch (SecurityException e) {
                Toast.makeText(this, "Permission is needed to use this shortcut", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Toast.makeText(this, "Unable to open this contact shortcut", Toast.LENGTH_LONG).show();
            }
        });
        // Contact shortcuts are launcher-owned tiles, not AppWidgetHostViews, so
        // provide the same long-press options explicitly.
        tile.setOnLongClickListener(v -> {
            showActionDialog(prefs.getString(KEY_SHORTCUT_LABEL, "Contact shortcut"),
                    new String[]{"↔", "−", "×", "↻"},
                    new String[]{"Move to other page", "Resize shortcut", "Remove shortcut", "Recreate contact shortcut"},
                    (dialog, which) -> {
                if (which == 0) {
                    int pageCount = pagesStrip == null ? 1 : Math.max(1, pagesStrip.getChildCount());
                    int targetPage = (prefs.getInt(KEY_SHORTCUT_PAGE, 0) == 0 && pageCount > 1) ? 1 : 0;
                    prefs.edit().putInt(KEY_SHORTCUT_PAGE, targetPage).apply();
                    currentPage = targetPage;
                    renderHomePages(false);
                    showPage(targetPage, true);
                } else if (which == 1) {
                    resizeGrip.setVisibility(View.VISIBLE);
                    tile.setBackground(LauncherUiUtils.roundRect(this,
                            new int[]{Color.argb(65, 115, 159, 255), Color.argb(30, 115, 159, 255)},
                            LauncherUiUtils.dp(this, 8)));
                    Toast.makeText(this, "Drag the blue corner handle to resize", Toast.LENGTH_LONG).show();
                } else if (which == 2) {
                    prefs.edit().remove(KEY_SHORTCUT_INTENT).remove(KEY_SHORTCUT_LABEL)
                            .remove(KEY_SHORTCUT_PAGE).remove(KEY_SHORTCUT_ICON)
                            .remove(KEY_SHORTCUT_SPAN_X).remove(KEY_SHORTCUT_SPAN_Y)
                            .remove(KEY_SHORTCUT_WIDTH_DP).remove(KEY_SHORTCUT_HEIGHT_DP).apply();
                    renderHomePages(false);
                } else if (which == 3) {
                    prefs.edit().remove(KEY_SHORTCUT_INTENT).remove(KEY_SHORTCUT_LABEL)
                            .remove(KEY_SHORTCUT_PAGE).remove(KEY_SHORTCUT_ICON)
                            .remove(KEY_SHORTCUT_SPAN_X).remove(KEY_SHORTCUT_SPAN_Y).apply();
                    beginWidgetPicker(currentPage);
                }
            });
            return true;
        });
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
                    view.animate().scaleX(1.12f).scaleY(1.12f).alpha(0.92f).setDuration(animationDuration(100)).start();
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
                            view.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(animationDuration(100)).start();
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
                    view.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(animationDuration(140)).start();
                    iconDragging = false;
                    iconDragArmed = false;
                    activeDragView = null;
                    activeDragPackage = null;
                    iconDragPageIndex = -1;
                    iconOriginalPageIndex = -1;
                    iconEdgeLatch = 0;
                    renderHomePages(false);
                    // After dropping an app, move it out of any widget footprint on the
                    // destination page. Re-run after layout so saved and visible positions agree.
                    final int collisionPageIndex = committedPage;
                    pageViewport.post(() -> {
                        if (pagesStrip == null || collisionPageIndex >= pagesStrip.getChildCount()) return;
                        FrameLayout collisionPage = (FrameLayout) pagesStrip.getChildAt(collisionPageIndex);
                        collisionPage.post(() -> {
                            for (int wi = 0; wi < collisionPage.getChildCount(); wi++) {
                                View item = collisionPage.getChildAt(wi);
                                Object tag = item.getTag();
                                if (tag instanceof String && ((String) tag).startsWith("launcher_widget:")
                                        && item instanceof FrameLayout) {
                                    resolveWidgetIconCollisions(collisionPage, (FrameLayout) item);
                                }
                            }
                        });
                    });
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
        // Return every face to its neutral state after a wheel turn.
        for (int i = 0; i < pagesStrip.getChildCount(); i++) {
            View page = pagesStrip.getChildAt(i);
            page.animate().cancel();
            page.setPivotX(page.getWidth() / 2f);
            page.setPivotY(page.getHeight() / 2f);
            page.setCameraDistance(12000f * getResources().getDisplayMetrics().density);
            page.setRotationY(0f);
            page.setScaleX(1f);
            page.setAlpha(1f);
            page.setTranslationX(0f);
            page.setTranslationZ(0f);
            page.setLayerType(View.LAYER_TYPE_NONE, null);
        }
        if (animate) {
            pagesStrip.animate().translationX(targetX)
                    .setDuration(animationDuration(240L))
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

    /**
     * Rotate the current page and its neighbour around their shared vertical edge.
     * No horizontal strip translation happens until the turn has finished, avoiding
     * the double-motion that made the old page-flip animation look jittery.
     */
    private int getPageTransition() {
        return Math.max(0, Math.min(3, prefs == null ? 0 : prefs.getInt(KEY_PAGE_TRANSITION, 0)));
    }

    private void applyWheelSwipe(int direction, float progress) {
        if (pagesStrip == null || pagesStrip.getChildCount() == 0) return;
        int from = Math.max(0, Math.min(pagesStrip.getChildCount() - 1, currentPage));
        int to = from + direction;
        View outgoing = pagesStrip.getChildAt(from);
        float width = Math.max(1, outgoing.getWidth());
        float height = Math.max(1, outgoing.getHeight());
        int effect = getPageTransition();
        float density = getResources().getDisplayMetrics().density;

        pagesStrip.setTranslationX(-from * width);
        View incomingFace = to >= 0 && to < pagesStrip.getChildCount() ? pagesStrip.getChildAt(to) : null;
        if (incomingFace != null) {
            incomingFace.setTranslationX(-direction * width);
            incomingFace.setAlpha(progress);
        }
        outgoing.setAlpha(1f - progress);
        // Parallax uses restrained transforms; no extra strip movement competes with them.
        if (effect == 2) {
            outgoing.setPivotX(direction > 0 ? width : 0f);
            outgoing.setPivotY(height * 0.5f);
            outgoing.setScaleX(1f - 0.045f * progress);
            outgoing.setScaleY(1f - 0.025f * progress);
            if (to >= 0 && to < pagesStrip.getChildCount()) {
                View incoming = pagesStrip.getChildAt(to);
                incoming.setPivotX(direction > 0 ? 0f : incoming.getWidth());
                incoming.setPivotY(incoming.getHeight() * 0.5f);
                incoming.setScaleX(0.955f + 0.045f * progress);
                incoming.setScaleY(0.975f + 0.025f * progress);
            }
            return;
        }

        outgoing.setCameraDistance(10000f * density);
        if (effect == 1) {
            // 3D Cube: rotate each face around the shared vertical edge with depth separation.
            outgoing.setPivotX(direction > 0 ? width : 0f);
            outgoing.setPivotY(height * 0.5f);
            outgoing.setRotationY(direction * 78f * progress);
            outgoing.setTranslationZ(-density * 28f * progress);
            outgoing.setTranslationX(-direction * width * 0.04f * progress);
            if (to >= 0 && to < pagesStrip.getChildCount()) {
                View incoming = pagesStrip.getChildAt(to);
                incoming.setCameraDistance(10000f * density);
                incoming.setPivotX(direction > 0 ? 0f : incoming.getWidth());
                incoming.setPivotY(incoming.getHeight() * 0.5f);
                incoming.setRotationY(-direction * 78f * (1f - progress));
                incoming.setTranslationZ(density * 28f * progress);
                incoming.setTranslationX(-direction * width + direction * width * 0.04f * (1f - progress));
            }
        } else if (effect == 3) {
            // Page Flip: hinge from the top edge on the X axis, like a turning sheet.
            outgoing.setPivotX(width * 0.5f);
            outgoing.setPivotY(0f);
            outgoing.setRotationX(direction * 68f * progress);
            outgoing.setRotationY(direction * 5f * progress);
            outgoing.setScaleX(1f - 0.04f * progress);
            if (to >= 0 && to < pagesStrip.getChildCount()) {
                View incoming = pagesStrip.getChildAt(to);
                incoming.setCameraDistance(10000f * density);
                incoming.setPivotX(incoming.getWidth() * 0.5f);
                incoming.setPivotY(0f);
                incoming.setRotationX(-direction * 68f * (1f - progress));
                incoming.setRotationY(-direction * 5f * (1f - progress));
                incoming.setScaleX(0.96f + 0.04f * progress);
            }
        } else {
            // 3D Wheel: broad rotation around the page center, controlled by the rotation slider.
            float angle = Math.min(180f, getPageRotationDegrees());
            outgoing.setPivotX(width * 0.5f);
            outgoing.setPivotY(height * 0.5f);
            outgoing.setRotationY(direction * angle * progress);
            outgoing.setScaleX(1f - 0.035f * progress);
            outgoing.setScaleY(1f - 0.025f * progress);
            if (to >= 0 && to < pagesStrip.getChildCount()) {
                View incoming = pagesStrip.getChildAt(to);
                incoming.setCameraDistance(10000f * density);
                incoming.setPivotX(incoming.getWidth() * 0.5f);
                incoming.setPivotY(incoming.getHeight() * 0.5f);
                incoming.setRotationY(-direction * angle * (1f - progress));
                incoming.setScaleX(0.965f + 0.035f * progress);
                incoming.setScaleY(0.975f + 0.025f * progress);
            }
        }
    }

    private void finishPageSwipe(int requestedPage, int pageWidth) {
        if (pagesStrip == null || pagesStrip.getChildCount() == 0) return;
        final int from = Math.max(0, Math.min(pagesStrip.getChildCount() - 1, currentPage));
        final int to = Math.max(0, Math.min(pagesStrip.getChildCount() - 1, requestedPage));
        final int direction = to == from ? wheelSwipeDirection
                : (to > from ? 1 : -1);
        final float startProgress = Math.max(0f, Math.min(1f, wheelSwipeProgress));
        final float endProgress = to == from ? 0f : 1f;
        final int width = Math.max(1, pageWidth);
        pagesStrip.animate().cancel();

        if (direction == 0 || from + direction < 0
                || from + direction >= pagesStrip.getChildCount()) {
            resetPageTransforms();
            pagesStrip.setTranslationX(-from * (float) width);
            wheelSwipeProgress = 0f;
            wheelSwipeDirection = 0;
            return;
        }

        final android.animation.ValueAnimator animator =
                android.animation.ValueAnimator.ofFloat(startProgress, endProgress);
        animator.setDuration(animationDuration(to == from ? 170L : 260L));
        animator.setInterpolator(new android.view.animation.DecelerateInterpolator(1.6f));
        animator.addUpdateListener(value -> applyWheelSwipe(direction, (Float) value.getAnimatedValue()));
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(android.animation.Animator animation) {
                currentPage = to;
                pagesStrip.setTranslationX(-to * (float) width);
                resetPageTransforms();
                wheelSwipeProgress = 0f;
                wheelSwipeDirection = 0;
                if (pageIndicator != null) {
                    boolean show = pages.size() > 1 && prefs.getBoolean(KEY_SHOW_PAGE_INDICATOR, true);
                    pageIndicator.setVisibility(show ? View.VISIBLE : View.GONE);
                    pageIndicator.setText((currentPage + 1) + "  /  " + pages.size());
                }
            }
        });
        animator.start();
    }

    private void resetPageTransforms() {
        if (pagesStrip == null) return;
        for (int i = 0; i < pagesStrip.getChildCount(); i++) {
            View face = pagesStrip.getChildAt(i);
            face.animate().cancel();
            face.setRotationY(0f);
            face.setRotationX(0f);
            face.setScaleX(1f);
            face.setScaleY(1f);
            face.setAlpha(1f);
            face.setTranslationX(0f);
            face.setTranslationY(0f);
            face.setTranslationZ(0f);
            face.setLayerType(View.LAYER_TYPE_NONE, null);
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
        panel.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(animationDuration(180)).start();
    }

    private void beginWidgetPicker() { beginWidgetPicker(currentPage); }

    private void beginWidgetPicker(int requestedPage) {
        pendingWidgetTargetPage = Math.max(0, Math.min(1, requestedPage));
        new WidgetPickerDialog(this, widgetManager, packageManager, new WidgetPickerDialog.Listener() {
            @Override public void onWidgetSelected(AppWidgetProviderInfo info) {
                beginBindingWidget(info);
            }
            @Override public void onShortcutSelected(ResolveInfo shortcutActivity) {
                beginCreateShortcut(shortcutActivity);
            }
        }).show();
    }

    private void beginCreateShortcut(ResolveInfo shortcutActivity) {
        if (shortcutActivity == null || shortcutActivity.activityInfo == null) return;
        try {
            Intent create = new Intent(Intent.ACTION_CREATE_SHORTCUT);
            create.setComponent(new android.content.ComponentName(
                    shortcutActivity.activityInfo.packageName, shortcutActivity.activityInfo.name));
            startActivityForResult(create, REQUEST_CREATE_SHORTCUT);
        } catch (Exception e) {
            Toast.makeText(this, "Couldn't open this shortcut", Toast.LENGTH_LONG).show();
        }
    }

    private void beginBindingWidget(AppWidgetProviderInfo info) {
        if (info == null) return;
        // Direct Dial widgets can initiate an immediate phone call. Request the
        // runtime permission before binding one, rather than waiting for its first tap.
        if (android.os.Build.VERSION.SDK_INT >= 23
                && requiresDirectDialPermission(info)
                && checkSelfPermission(android.Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            pendingWidgetInfo = info;
            requestPermissions(new String[]{android.Manifest.permission.CALL_PHONE}, REQUEST_CALL_PERMISSION);
            return;
        }
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

    private boolean requiresDirectDialPermission(AppWidgetProviderInfo info) {
        try {
            String label = String.valueOf(info.loadLabel(packageManager)).toLowerCase(Locale.ROOT);
            return label.contains("direct dial") || label.contains("direct call");
        } catch (Exception ignored) {
            return false;
        }
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_CALL_PERMISSION) return;
        boolean granted = grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        if (pendingShortcutCallIntent != null) {
            Intent callIntent = pendingShortcutCallIntent;
            pendingShortcutCallIntent = null;
            if (granted) {
                try { startActivity(callIntent); }
                catch (Exception e) { Toast.makeText(this, "Unable to start this call", Toast.LENGTH_LONG).show(); }
            } else {
                Toast.makeText(this, "Phone permission is needed to place this call", Toast.LENGTH_LONG).show();
            }
            return;
        }
        AppWidgetProviderInfo info = pendingWidgetInfo;
        pendingWidgetInfo = null;
        if (granted && info != null) {
            beginBindingWidget(info);
        } else {
            Toast.makeText(this, "Phone permission is needed for a Direct Dial widget", Toast.LENGTH_LONG).show();
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
        if (requestCode == REQUEST_CREATE_SHORTCUT) {
            if (resultCode != RESULT_OK || data == null) return;
            try {
                Intent shortcutIntent = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT);
                if (shortcutIntent == null) {
                    Toast.makeText(this, "The selected shortcut returned no launch action", Toast.LENGTH_LONG).show();
                    return;
                }
                String shortcutLabel = data.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);
                if (shortcutLabel == null || shortcutLabel.trim().isEmpty()) shortcutLabel = "Contact shortcut";
                prefs.edit()
                        .putString(KEY_SHORTCUT_INTENT, shortcutIntent.toUri(Intent.URI_INTENT_SCHEME))
                        .putString(KEY_SHORTCUT_LABEL, shortcutLabel)
                        .putInt(KEY_SHORTCUT_PAGE, pendingWidgetTargetPage)
                        .apply();
                Bitmap shortcutBitmap = null;
                Object iconExtra = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON);
                if (iconExtra instanceof Bitmap) shortcutBitmap = (Bitmap) iconExtra;
                if (shortcutBitmap == null) {
                    try {
                        Intent.ShortcutIconResource iconResource =
                                data.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE);
                        if (iconResource != null) {
                            android.content.res.Resources res = packageManager.getResourcesForApplication(iconResource.packageName);
                            int id = res.getIdentifier(iconResource.resourceName, null, null);
                            if (id != 0) {
                                android.graphics.drawable.Drawable drawable = res.getDrawable(id);
                                shortcutBitmap = Bitmap.createBitmap(
                                        Math.max(1, drawable.getIntrinsicWidth()), Math.max(1, drawable.getIntrinsicHeight()),
                                        Bitmap.Config.ARGB_8888);
                                Canvas canvas = new Canvas(shortcutBitmap);
                                drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                                drawable.draw(canvas);
                            }
                        }
                    } catch (Exception ignored) {}
                }
                if (shortcutBitmap != null) {
                    try (java.io.FileOutputStream output = openFileOutput("contact_shortcut_icon.png", MODE_PRIVATE)) {
                        shortcutBitmap.compress(Bitmap.CompressFormat.PNG, 100, output);
                        prefs.edit().putString(KEY_SHORTCUT_ICON, "contact_shortcut_icon.png").apply();
                    } catch (Exception ignored) {}
                } else {
                    prefs.edit().remove(KEY_SHORTCUT_ICON).apply();
                }
                renderSavedContactShortcut();
                Toast.makeText(this, "Contact shortcut added", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Couldn't add this shortcut", Toast.LENGTH_LONG).show();
            }
            return;
        }
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
        // Apply the same restrained silver tone to every clock preset, including 7-segment.
        clockText.setTextColor(Color.rgb(196, 200, 209));
        if (clockText instanceof SevenSegmentClockTextView) {
            SevenSegmentClockTextView ledClock = (SevenSegmentClockTextView) clockText;
            // Reassert the selected mode after every style refresh (including the
            // minute-boundary clock update) and keep the custom glyph paint visible.
            boolean ledMode = style == 5;
            if (ledMode) {
                clockText.setVisibility(View.VISIBLE);
                clockText.setAlpha(1f);
                // A very soft graphite halo adds depth without turning the digits white.
                clockText.setShadowLayer(LauncherUiUtils.dp(this, 2), 0f, 0f,
                        Color.argb(100, 196, 200, 209));
            }
            ledClock.setSevenSegmentMode(ledMode);
            ledClock.invalidate();
        }
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
        // Two complementary colour fields meet at the centre: cool teal/blue on
        // the left and the existing phase-driven violet/blue on the right.
        int leftR = Math.max(55, Math.min(220, tone[0] * 2 / 3));
        int leftG = Math.max(150, Math.min(225, tone[1] + 28));
        int leftB = Math.max(170, Math.min(240, tone[2] + 18));
        int leftEdge = Color.argb(125, leftR, leftG, leftB);
        int rightEdge = Color.argb(149, tone[0], tone[1], tone[2]);
        int leftMid = Color.argb(72, leftR, leftG, leftB);
        int rightMid = Color.argb(76, tone[0], tone[1], tone[2]);
        GradientDrawable next = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[] {leftEdge, leftMid, Color.argb(22, 125, 155, 235),
                        rightMid, rightEdge});
        next.setCornerRadius(LauncherUiUtils.dp(this, 18));
        if (animate) {
            hero.setBackground(next);
            next.setAlpha(0);
            android.animation.ValueAnimator fade = android.animation.ValueAnimator.ofInt(0, 255);
            fade.setDuration(animationDuration(260L));
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
                child.animate().translationX(0f).translationY(0f).setDuration(animationDuration(150L)).start();
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

        addSection(content, "Clock and motion");
        addSliderRow(content, "Page rotation", 50, 180, getPageRotationDegrees(), "°", value -> {
            prefs.edit().putInt(KEY_PAGE_ROTATION, value).apply();
        });
        addChoiceRow(content, "Launcher animation speed", prefs.getInt(KEY_ANIMATION_SPEED, 1), new String[]{"Slow", "Medium", "Fast"}, value -> {
            prefs.edit().putInt(KEY_ANIMATION_SPEED, value).apply();
            showCustomizationMenu(dialog, scroll.getScrollY(), false);
        });
        addChoiceRow(content, "Page transition effect", getPageTransition(),
                new String[]{"3D Wheel", "3D Cube", "Parallax", "Page Flip"}, value -> {
            prefs.edit().putInt(KEY_PAGE_TRANSITION, value).apply();
            showCustomizationMenu(dialog, scroll.getScrollY(), false);
        });
        addSliderRow(content, "Clock length", 80, 250, getClockLength(), "%", value -> {
            prefs.edit().putInt(KEY_CLOCK_LENGTH, value).apply();
            applyClockPositionAndSize();
        });
        addSliderRow(content, "Clock breadth", 80, 250, getClockBreadth(), "%", value -> {
            prefs.edit().putInt(KEY_CLOCK_BREADTH, value).apply();
            applyClockPositionAndSize();
        });
        addChoiceRow(content, "Clock position", getClockPosition(), new String[]{"Left", "Middle", "Right"}, value -> {
            prefs.edit().putInt(KEY_CLOCK_POSITION, value).apply();
            applyClockPositionAndSize();
            showCustomizationMenu(dialog, scroll.getScrollY(), false);
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
            panel.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(animationDuration(190)).start();
        }
    }

    private int getPageRotationDegrees() {
        return Math.max(50, Math.min(180, prefs == null ? 90 : prefs.getInt(KEY_PAGE_ROTATION, 90)));
    }

    private int getClockLength() {
        return Math.max(80, Math.min(250, prefs == null ? 100 : prefs.getInt(KEY_CLOCK_LENGTH, 100)));
    }

    private int getClockBreadth() {
        return Math.max(80, Math.min(250, prefs == null ? 100 : prefs.getInt(KEY_CLOCK_BREADTH, 100)));
    }

    private int getClockPosition() {
        return Math.max(0, Math.min(2, prefs == null ? 2 : prefs.getInt(KEY_CLOCK_POSITION, 2)));
    }

    private long animationDuration(long baseMillis) {
        return LauncherMotion.duration(this, baseMillis);
    }

    private int clockGravity() {
        int position = getClockPosition();
        return position == 0 ? Gravity.LEFT : (position == 1 ? Gravity.CENTER_HORIZONTAL : Gravity.RIGHT);
    }

    private void applyClockPositionAndSize() {
        if (clockPanel == null || clockText == null) return;
        int alignment = clockGravity();
        final float lengthScale = getClockLength() / 100f;
        final float breadthScale = getClockBreadth() / 100f;
        clockPanel.setGravity(alignment);
        LinearLayout.LayoutParams panelParams = (LinearLayout.LayoutParams) clockPanel.getLayoutParams();
        panelParams.gravity = alignment;
        panelParams.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        clockPanel.setLayoutParams(panelParams);

        int clockWidth = (int) Math.ceil(clockText.getPaint().measureText(clockText.getText().toString()) * lengthScale);
        int dateWidth = dateText == null ? 0
                : (int) Math.ceil(dateText.getPaint().measureText(dateText.getText().toString()));
        int contentWidth = Math.max(clockWidth, dateWidth) + LauncherUiUtils.dp(this, 20);
        int rowHeight = LauncherUiUtils.dp(this, Math.round(114f * breadthScale));
        if (clockRow != null) {
            ViewGroup.LayoutParams rowParams = clockRow.getLayoutParams();
            if (rowParams != null) { rowParams.width = contentWidth; rowParams.height = rowHeight; clockRow.setLayoutParams(rowParams); }
            clockRow.setClipChildren(false);
            clockRow.setClipToPadding(false);
        }
        if (clockGroup != null) {
            FrameLayout.LayoutParams groupParams = (FrameLayout.LayoutParams) clockGroup.getLayoutParams();
            groupParams.gravity = alignment | Gravity.CENTER_VERTICAL;
            groupParams.width = contentWidth;
            groupParams.height = rowHeight;
            clockGroup.setLayoutParams(groupParams);
            clockGroup.setClipChildren(false);
            clockGroup.setClipToPadding(false);
        }
        clockText.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        clockText.setTextSize(78f);
        clockText.setPivotX(0f);
        clockText.setPivotY(0f);
        clockText.setScaleX(lengthScale);
        clockText.setScaleY(breadthScale);
        ViewGroup.LayoutParams clockParams = clockText.getLayoutParams();
        if (clockParams != null) {
            clockParams.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            clockParams.height = LauncherUiUtils.dp(this, 114);
            clockText.setLayoutParams(clockParams);
        }
        if (dateText != null) {
            dateText.setGravity(alignment);
            ViewGroup.LayoutParams dateParams = dateText.getLayoutParams();
            if (dateParams != null) { dateParams.width = contentWidth; dateText.setLayoutParams(dateParams); }
        }
        if (amPmText != null) {
            amPmText.setTextSize(Math.max(6f, Math.min(9f, 78f * Math.min(lengthScale, breadthScale) * 0.10f)));
            ViewGroup.LayoutParams markerRaw = amPmText.getLayoutParams();
            if (markerRaw instanceof FrameLayout.LayoutParams) {
                FrameLayout.LayoutParams markerParams = (FrameLayout.LayoutParams) markerRaw;
                markerParams.width = LauncherUiUtils.dp(this, 18);
                markerParams.height = LauncherUiUtils.dp(this, 12);
                markerParams.gravity = Gravity.RIGHT | Gravity.BOTTOM;
                markerParams.rightMargin = 0;
                markerParams.bottomMargin = 0;
                amPmText.setLayoutParams(markerParams);
            }
        }
        clockText.invalidate();
    }

private void addSliderRow(LinearLayout panel, String title, int min, int max, int current,
            String suffix, IntSliderAction action) {
        LinearLayout block = new LinearLayout(this);
        block.setOrientation(LinearLayout.VERTICAL);
        block.setPadding(LauncherUiUtils.dp(this, 10), LauncherUiUtils.dp(this, 8),
                LauncherUiUtils.dp(this, 10), LauncherUiUtils.dp(this, 7));
        block.setBackground(LauncherUiUtils.roundRect(this,
                new int[]{Color.rgb(36, 44, 68), Color.rgb(25, 31, 50)}, LauncherUiUtils.dp(this, 14)));
        LinearLayout.LayoutParams blockParams = new LinearLayout.LayoutParams(-1, -2);
        blockParams.bottomMargin = LauncherUiUtils.dp(this, 7);
        TextView label = LauncherUiUtils.text(this, title, 14, Color.WHITE, true);
        TextView valueLabel = LauncherUiUtils.text(this, current + suffix, 12, Color.rgb(177, 198, 255), true);
        LinearLayout heading = new LinearLayout(this);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        heading.addView(label, new LinearLayout.LayoutParams(0, -2, 1f));
        heading.addView(valueLabel);
        block.addView(heading);
        SeekBar seekBar = new SeekBar(this);
        seekBar.setMax(max - min);
        seekBar.setProgress(Math.max(0, Math.min(max - min, current - min)));
        block.addView(seekBar, new LinearLayout.LayoutParams(-1, LauncherUiUtils.dp(this, 38)));
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                int value = min + progress;
                valueLabel.setText(value + suffix);
                if (fromUser) action.onChange(value);
            }
            @Override public void onStartTrackingTouch(SeekBar bar) {}
            @Override public void onStopTrackingTouch(SeekBar bar) {}
        });
        panel.addView(block, blockParams);
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
