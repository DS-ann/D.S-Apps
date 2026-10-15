package com.dsann.launcher;

import android.app.Activity;
import android.app.Dialog;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Intent;
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
    private static final String KEY_WIDGET_IDS = "widget_ids";
    private static final int REQUEST_WALLPAPER = 7301;
    private static final int REQUEST_WIDGET_PICK = 7302;
    private static final int REQUEST_WIDGET_BIND = 7303;
    private static final int REQUEST_WIDGET_CONFIGURE = 7304;
    private int pendingWidgetId = -1;
    private AppWidgetProviderInfo pendingWidgetInfo;
    private static final int COLUMNS = 4;
    private static final int HOME_ROWS = 5;
    private interface IntAction { void onSelect(int value); }
    private final int BG = Color.rgb(10, 13, 24);
    private final Handler handler = new Handler();
    private PackageManager packageManager;
    private SharedPreferences prefs;
    private AppWidgetManager widgetManager;
    private AppWidgetHost widgetHost;
    private FrameLayout root;
    private LinearLayout homeScreen;
    private TextView brandText;
    private LinearLayout pagesStrip;
    private FrameLayout pageViewport;
    private LinearLayout drawerScreen;
    private GridView drawerGrid;
    private TextView clockText;
    private TextView dateText;
    private TextView pageIndicator;
    private LauncherAppAdapter drawerAdapter;
    private List<ResolveInfo> allApps = new ArrayList<>();
    private List<ResolveInfo> drawerApps = new ArrayList<>();
    private final List<String> homePackages = new ArrayList<>();
    private final List<List<String>> pages = new ArrayList<>();
    private int currentPage = 0;
    private float gestureDownX, gestureDownY;
    private float longPressDownX, longPressDownY;
    private boolean longPressTriggered = false;
    private boolean iconDragArmed = false;
    private boolean iconDragging = false;
    private boolean iconMovedTooEarly = false;
    private float iconTouchRawX, iconTouchRawY;
    private int iconOriginalLeft, iconOriginalTop;
    private View activeDragView;
    private String activeDragPackage;
    private Runnable iconArmDrag;
    private Runnable iconShowOptions;
    private final Runnable homeLongPress = new Runnable() {
        @Override public void run() {
            if (!drawerOpen && root != null) {
                longPressTriggered = true;
                showCustomizationMenu();
            }
        }
    };
    private boolean drawerOpen = false;
    private boolean drawerAtTopOnGestureDown = true;
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
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            gestureDownX = event.getX();
            gestureDownY = event.getY();
            longPressDownX = event.getX();
            longPressDownY = event.getY();
            longPressTriggered = false;
            drawerAtTopOnGestureDown = !drawerOpen || isDrawerAtTop();
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
                    && ((!drawerOpen && dy < 0) || (drawerOpen && dy > 0 && drawerAtTopOnGestureDown));
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
        allApps = result;
        drawerApps = new ArrayList<>();
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
            int requiredPages = Math.max(1, (homePackages.size() + pageCapacity() - 1) / pageCapacity());
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
        int capacity = pageCapacity();
        for (int i = 0; i < homePackages.size(); i++) {
            String packageName = homePackages.get(i);
            int pageIndex = prefs.contains(KEY_ICON_PAGE + packageName)
                    ? Math.max(0, Math.min(20, prefs.getInt(KEY_ICON_PAGE + packageName, 0)))
                    : i / capacity;
            ensurePageExists(pageIndex);
            pages.get(pageIndex).add(packageName);
        }
        if (pages.isEmpty()) pages.add(new ArrayList<String>());
        // Preserve pages that contain only widgets, including after a launcher restart.
        String widgetEntries = prefs.getString(KEY_WIDGET_IDS, "");
        if (!widgetEntries.isEmpty()) {
            for (String entry : widgetEntries.split("\\n")) {
                String[] parts = entry.split(":");
                if (parts.length != 2) continue;
                try { ensurePageExists(Math.max(0, Math.min(20, Integer.parseInt(parts[1])))); }
                catch (NumberFormatException ignored) {}
            }
        }
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
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            homeScreen.setPadding(dp(20), dp(14) + insets.getSystemWindowInsetTop(), dp(20), dp(8));
            return insets;
        });
        buildHomeChrome();
        buildDrawer();
        setContentView(root);
        root.requestApplyInsets();
        renderHomePages(false);
        showHome();
    }

    private void buildHomeChrome() {
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView brand = text("D.Sannigrahi", 12, Color.rgb(210, 220, 255), true);
        brandText = brand;
        brand.setTypeface(Typeface.create("sans-serif-medium", Typeface.ITALIC));
        brand.setLetterSpacing(0.035f);
        header.addView(brand, new LinearLayout.LayoutParams(0, dp(32), 1f));
        homeScreen.addView(header);

        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setPadding(dp(18), dp(12), dp(18), dp(12));
        // A smoked-glass black panel: translucent enough to let the wallpaper subtly show through.
        hero.setBackground(roundRect(new int[]{Color.argb(188, 17, 20, 27), Color.argb(168, 8, 10, 15)}, dp(22)));
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, -2);
        hp.topMargin = dp(8);
        homeScreen.addView(hero, hp);
        clockText = new SquarePixelClockTextView(this, prefs, KEY_CLOCK_STYLE);
        clockText.setText("--:--");
        clockText.setTextSize(40);
        clockText.setTextColor(Color.WHITE);
        clockText.setIncludeFontPadding(false);
        clockText.setTypeface(Typeface.create("monospace", Typeface.BOLD));
        clockText.setLetterSpacing(0.055f);
        applyClockStyle();
        hero.addView(clockText);
        dateText = text("", 13, Color.rgb(190, 201, 218), false);
        dateText.setTypeface(Typeface.create("sans-serif-condensed-medium", Typeface.NORMAL));
        dateText.setLetterSpacing(0.075f);
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
        drawerScreen.setBackground(drawerBackground());
        TextView title = text("Apps", 28, Color.WHITE, true);
        drawerScreen.addView(title, new LinearLayout.LayoutParams(-1, dp(48)));
        drawerGrid = new GridView(this);
        drawerGrid.setNumColumns(getDrawerColumns());
        drawerGrid.setVerticalSpacing(dp(prefs.getInt(KEY_DRAWER_SPACING, 8)));
        drawerGrid.setHorizontalSpacing(dp(prefs.getInt(KEY_DRAWER_SPACING, 8) / 2));
        drawerGrid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        drawerGrid.setSelector(android.R.color.transparent);
        drawerAdapter = new LauncherAppAdapter(this, prefs, packageManager, allApps, drawerApps, null,
                true, KEY_DRAWER_ICON_SIZE, KEY_DRAWER_FONT_SIZE, KEY_SHOW_DRAWER_LABELS,
                new LauncherAppAdapter.Actions() {
                    @Override public void onAppClick(ResolveInfo app) { launchApp(app); }
                    @Override public void onAppLongClick(ResolveInfo app, boolean onHome) { showAppOptions(app, onHome); }
                });
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
        int pageWidth = Math.max(1, getResources().getDisplayMetrics().widthPixels - dp(40));
        int columns = getHomeColumns();
        int rows = HOME_ROWS;
        for (int pageIndex = 0; pageIndex < pages.size(); pageIndex++) {
            FrameLayout page = new FrameLayout(this);
            page.setClipChildren(false);
            List<String> pagePackages = pages.get(pageIndex);
            for (int itemIndex = 0; itemIndex < pagePackages.size(); itemIndex++) {
                String packageName = pagePackages.get(itemIndex);
                if (findApp(packageName) == null) continue;
                View cell = new LauncherAppAdapter(this, prefs, packageManager, allApps, drawerApps, pagePackages,
                        false, KEY_ICON_SIZE, KEY_HOME_FONT_SIZE, KEY_SHOW_HOME_LABELS,
                        new LauncherAppAdapter.Actions() {
                            @Override public void onAppClick(ResolveInfo app) { launchApp(app); }
                            @Override public void onAppLongClick(ResolveInfo app, boolean onHome) { showAppOptions(app, onHome); }
                        }).getView(itemIndex, null, page);
                cell.setTag("launcher_app:" + packageName);
                int fallbackHeight = Math.max(dp(360), getResources().getDisplayMetrics().heightPixels - dp(260));
                int pageHeight = pageViewport.getHeight() > dp(100) ? pageViewport.getHeight() : fallbackHeight;
                int cellWidth = Math.max(dp(1), pageWidth / columns);
                int cellHeight = Math.max(dp(1), pageHeight / rows);
                FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(cellWidth, cellHeight, Gravity.TOP | Gravity.LEFT);
                int defaultCol = itemIndex % columns;
                int defaultRow = (itemIndex / columns) % rows;
                float savedX = prefs.getFloat(KEY_ICON_X + packageName, defaultCol / (float) Math.max(1, columns - 1));
                float savedY = prefs.getFloat(KEY_ICON_Y + packageName, defaultRow / (float) Math.max(1, rows - 1));
                int col = Math.max(0, Math.min(columns - 1, Math.round(savedX * Math.max(1, columns - 1))));
                int row = Math.max(0, Math.min(rows - 1, Math.round(savedY * Math.max(1, rows - 1))));
                lp.leftMargin = col * cellWidth;
                lp.topMargin = row * cellHeight;
                page.addView(cell, lp);
                installHomeIconTouch(cell, packageName);
            }
            pagesStrip.addView(page, new LinearLayout.LayoutParams(pageWidth, -1));
            page.post(() -> {
                if (page.getWidth() <= 0 || page.getHeight() <= 0) return;
                int actualCellWidth = Math.max(1, page.getWidth() / columns);
                int actualCellHeight = Math.max(1, page.getHeight() / rows);
                for (int childIndex = 0; childIndex < page.getChildCount(); childIndex++) {
                    View child = page.getChildAt(childIndex);
                    Object childTag = child.getTag();
                    if (!(childTag instanceof String) || !((String) childTag).startsWith("launcher_app:")) continue;
                    String childPackage = ((String) childTag).substring("launcher_app:".length());
                    int orderIndex = pagePackages.indexOf(childPackage);
                    if (orderIndex < 0) continue;
                    int defaultCol = orderIndex % columns;
                    int defaultRow = (orderIndex / columns) % rows;
                    float savedX = prefs.getFloat(KEY_ICON_X + childPackage, defaultCol / (float) Math.max(1, columns - 1));
                    float savedY = prefs.getFloat(KEY_ICON_Y + childPackage, defaultRow / (float) Math.max(1, rows - 1));
                    int col = Math.max(0, Math.min(columns - 1, Math.round(savedX * Math.max(1, columns - 1))));
                    int row = Math.max(0, Math.min(rows - 1, Math.round(savedY * Math.max(1, rows - 1))));
                    FrameLayout.LayoutParams actual = (FrameLayout.LayoutParams) child.getLayoutParams();
                    actual.width = actualCellWidth;
                    actual.height = actualCellHeight;
                    actual.leftMargin = col * actualCellWidth;
                    actual.topMargin = row * actualCellHeight;
                    child.setLayoutParams(actual);
                }
            });
        }
        restoreWidgetsOnPages();
        pageIndicator.setVisibility(prefs.getBoolean(KEY_SHOW_PAGE_INDICATOR, true) ? View.VISIBLE : View.GONE);
        pageIndicator.setText((currentPage + 1) + "  /  " + pages.size());
        pagesStrip.setTranslationX(-currentPage * pageWidth);
        if (animate) {
            pagesStrip.setRotationY(-18f);
            pagesStrip.setScaleX(0.94f);
            pagesStrip.animate().rotationY(0f).scaleX(1f).setDuration(360).start();
        } else {
            pagesStrip.setRotationY(0f);
            pagesStrip.setScaleX(1f);
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
                boolean moved = Math.abs(dx) > dp(8) || Math.abs(dy) > dp(8);
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
                    FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) view.getLayoutParams();
                    FrameLayout page = (FrameLayout) view.getParent();
                    int maxX = Math.max(0, page.getWidth() - view.getWidth());
                    int maxY = Math.max(0, page.getHeight() - view.getHeight());
                    lp.leftMargin = Math.max(0, Math.min(maxX, iconOriginalLeft + Math.round(dx)));
                    lp.topMargin = Math.max(0, Math.min(maxY, iconOriginalTop + Math.round(dy)));
                    view.setLayoutParams(lp);
                }
                return true;
            }
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                if (iconArmDrag != null) handler.removeCallbacks(iconArmDrag);
                if (iconShowOptions != null) handler.removeCallbacks(iconShowOptions);
                if (iconDragging) {
                    FrameLayout page = (FrameLayout) view.getParent();
                    int[] releaseLocation = new int[2];
                    page.getLocationOnScreen(releaseLocation);
                    FrameLayout.LayoutParams dragged = (FrameLayout.LayoutParams) view.getLayoutParams();
                    // Detect the edge from both the finger and the icon bounds. Touch events
                    // can be clipped by the icon cell before the finger reaches the screen edge.
                    int[] viewportLocation = new int[2];
                    pageViewport.getLocationOnScreen(viewportLocation);
                    boolean atRightEdge = event.getRawX() >= viewportLocation[0] + pageViewport.getWidth() - dp(36)
                            || dragged.leftMargin + view.getWidth() >= page.getWidth() - dp(36);
                    boolean moveToNextPage = atRightEdge;
                    int columns = getHomeColumns();
                    int rows = HOME_ROWS;
                    int cellWidth = Math.max(1, page.getWidth() / columns);
                    int cellHeight = Math.max(1, page.getHeight() / rows);
                    int targetCol = Math.max(0, Math.min(columns - 1, (dragged.leftMargin + view.getWidth() / 2) / cellWidth));
                    int targetRow = Math.max(0, Math.min(rows - 1, (dragged.topMargin + view.getHeight() / 2) / cellHeight));
                    int sourceCol = Math.max(0, Math.min(columns - 1, Math.round((float) iconOriginalLeft / cellWidth)));
                    int sourceRow = Math.max(0, Math.min(rows - 1, Math.round((float) iconOriginalTop / cellHeight)));
                    View occupant = null;
                    String otherPackage = null;
                    for (int i = 0; i < page.getChildCount(); i++) {
                        View candidate = page.getChildAt(i);
                        if (candidate == view || !(candidate.getTag() instanceof String)) continue;
                        String candidateTag = (String) candidate.getTag();
                        if (!candidateTag.startsWith("launcher_app:")) continue;
                        FrameLayout.LayoutParams candidateLp = (FrameLayout.LayoutParams) candidate.getLayoutParams();
                        int candidateCol = Math.max(0, Math.min(columns - 1, Math.round((float) candidateLp.leftMargin / cellWidth)));
                        int candidateRow = Math.max(0, Math.min(rows - 1, Math.round((float) candidateLp.topMargin / cellHeight)));
                        if (candidateCol == targetCol && candidateRow == targetRow) {
                            occupant = candidate;
                            otherPackage = candidateTag.substring("launcher_app:".length());
                            break;
                        }
                    }
                    if (occupant != null) {
                        FrameLayout.LayoutParams otherLp = (FrameLayout.LayoutParams) occupant.getLayoutParams();
                        otherLp.leftMargin = sourceCol * cellWidth;
                        otherLp.topMargin = sourceRow * cellHeight;
                        occupant.setLayoutParams(otherLp);
                        saveIconSlot(otherPackage, sourceCol, sourceRow, columns);
                    }
                    dragged.leftMargin = targetCol * cellWidth;
                    dragged.topMargin = targetRow * cellHeight;
                    view.setLayoutParams(dragged);
                    saveIconSlot(packageName, targetCol, targetRow, columns);
                    if (moveToNextPage) {
                        // Insert a fresh page immediately after the page being dragged from.
                        // Shift existing later pages (including widget-only pages) so the
                        // dragged shortcut lands on the new page instead of a stale page list.
                        int destinationPage = currentPage + 1;
                        SharedPreferences.Editor edit = prefs.edit();
                        int capacity = pageCapacity();
                        for (int packageIndex = 0; packageIndex < homePackages.size(); packageIndex++) {
                            String existingPackage = homePackages.get(packageIndex);
                            if (existingPackage.equals(packageName)) continue;
                            int existingPage = prefs.contains(KEY_ICON_PAGE + existingPackage)
                                    ? prefs.getInt(KEY_ICON_PAGE + existingPackage, 0)
                                    : packageIndex / capacity;
                            if (existingPage >= destinationPage) {
                                edit.putInt(KEY_ICON_PAGE + existingPackage, existingPage + 1);
                            }
                        }
                        String widgetEntries = prefs.getString(KEY_WIDGET_IDS, "");
                        StringBuilder shiftedWidgets = new StringBuilder();
                        if (!widgetEntries.isEmpty()) {
                            for (String entry : widgetEntries.split("\\n")) {
                                String[] parts = entry.split(":");
                                if (parts.length != 2) continue;
                                try {
                                    int id = Integer.parseInt(parts[0]);
                                    int widgetPage = Integer.parseInt(parts[1]);
                                    if (widgetPage >= destinationPage) widgetPage++;
                                    if (shiftedWidgets.length() > 0) shiftedWidgets.append('\n');
                                    shiftedWidgets.append(id).append(':').append(widgetPage);
                                } catch (NumberFormatException ignored) {}
                            }
                        }
                        edit.putString(KEY_WIDGET_IDS, shiftedWidgets.toString())
                                .putInt(KEY_ICON_PAGE + packageName, destinationPage)
                                .putFloat(KEY_ICON_X + packageName, 0f)
                                .putFloat(KEY_ICON_Y + packageName, targetRow / (float) Math.max(1, rows - 1))
                                .apply();
                        currentPage = destinationPage;
                    } else {
                        prefs.edit().putInt(KEY_ICON_PAGE + packageName, currentPage).apply();
                    }
                    view.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(140).start();
                    iconDragging = false;
                    iconDragArmed = false;
                    activeDragView = null;
                    activeDragPackage = null;
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
        while (pages.size() <= pageIndex) pages.add(new ArrayList<String>());
    }

    private void nextPage() {
        if (currentPage + 1 < pages.size()) {
            currentPage++;
            renderHomePages(true);
            return;
        }
        // A swipe creates a new page only when the current page is full.
        if (pages.get(currentPage).size() >= pageCapacity()) {
            currentPage++;
            renderHomePages(true);
        }
    }

    private void previousPage() {
        if (currentPage > 0) { currentPage--; renderHomePages(true); }
    }

    private boolean isDrawerAtTop() {
        if (drawerGrid == null || drawerGrid.getChildCount() == 0) return true;
        if (drawerGrid.getFirstVisiblePosition() > 0) return false;
        View first = drawerGrid.getChildAt(0);
        return first == null || first.getTop() >= drawerGrid.getPaddingTop();
    }

    private void showHome() {
        drawerOpen = false;
        drawerScreen.animate().cancel();
        homeScreen.animate().cancel();
        homeScreen.setVisibility(View.VISIBLE);
        homeScreen.setAlpha(0.82f);
        homeScreen.setScaleX(0.96f);
        homeScreen.setScaleY(0.96f);
        homeScreen.setTranslationY(dp(24));
        drawerScreen.animate().alpha(0f).translationY(dp(40)).scaleX(0.96f).scaleY(0.96f)
                .rotationX(-7f).setDuration(260).withEndAction(() -> drawerScreen.setVisibility(View.GONE)).start();
        homeScreen.animate().alpha(1f).translationY(0f).scaleX(1f).scaleY(1f)
                .setDuration(340).start();
    }

    private void showDrawer() {
        drawerOpen = true;
        loadApps();
        homeScreen.animate().cancel();
        drawerScreen.animate().cancel();
        drawerScreen.setVisibility(View.VISIBLE);
        drawerScreen.setAlpha(0f);
        drawerScreen.setScaleX(0.92f);
        drawerScreen.setScaleY(0.92f);
        drawerScreen.setTranslationY(getResources().getDisplayMetrics().heightPixels * 0.16f);
        drawerScreen.setRotationX(10f);
        drawerScreen.setPivotY(0f);
        homeScreen.animate().alpha(0f).scaleX(0.94f).scaleY(0.94f).translationY(-dp(22))
                .setDuration(250).withEndAction(() -> homeScreen.setVisibility(View.GONE)).start();
        drawerScreen.animate().alpha(1f).scaleX(1f).scaleY(1f).translationY(0f).rotationX(0f)
                .setDuration(420).start();
    }

    private void addToHome(String packageName) {
        if (!homePackages.contains(packageName)) {
            homePackages.add(packageName);
            persistHomeApps();
            rebuildPages();
            currentPage = pages.size() - 1;
            renderHomePages(false);
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
                    currentPage = to / pageCapacity();
                    renderHomePages(false);
                }
            } else if (action.equals("Remove from Home")) {
                homePackages.remove(app.activityInfo.packageName);
                prefs.edit().remove(KEY_ICON_X + app.activityInfo.packageName).remove(KEY_ICON_Y + app.activityInfo.packageName).apply();
                int requiredPages = Math.max(1, (homePackages.size() + pageCapacity() - 1) / pageCapacity());
                if (currentPage >= requiredPages) currentPage = requiredPages - 1;
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
        panel.setPadding(dp(18), dp(16), dp(18), dp(16));
        panel.setBackground(roundRect(new int[]{Color.rgb(28, 35, 59), Color.rgb(15, 19, 34)}, dp(24)));
        TextView heading = text(title, 20, Color.WHITE, true);
        heading.setPadding(dp(4), dp(4), dp(4), dp(12));
        panel.addView(heading, new LinearLayout.LayoutParams(-1, -2));
        for (int i = 0; i < labels.length; i++) {
            final int index = i;
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(10), dp(7), dp(12), dp(7));
            row.setBackground(roundRect(new int[]{Color.rgb(42, 51, 79), Color.rgb(31, 38, 61)}, dp(15)));
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, dp(54));
            rowParams.topMargin = dp(5);
            TextView glyph = text(icons[i], 21, Color.rgb(178, 198, 255), true);
            glyph.setGravity(Gravity.CENTER);
            glyph.setBackground(roundRect(new int[]{Color.rgb(65, 83, 139), Color.rgb(51, 57, 112)}, dp(12)));
            row.addView(glyph, new LinearLayout.LayoutParams(dp(38), dp(38)));
            TextView label = text(labels[i], 15, Color.rgb(245, 247, 255), false);
            label.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(0, -1, 1f);
            labelParams.leftMargin = dp(12);
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
        if (dialog.getWindow() != null) dialog.getWindow().setLayout(getResources().getDisplayMetrics().widthPixels - dp(36), -2);
        panel.setAlpha(0f);
        panel.setScaleX(0.94f);
        panel.setScaleY(0.94f);
        panel.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(180).start();
    }

    private void beginWidgetPicker() {
        List<AppWidgetProviderInfo> providers = new ArrayList<>(widgetManager.getInstalledProviders());
        Collections.sort(providers, new Comparator<AppWidgetProviderInfo>() {
            @Override public int compare(AppWidgetProviderInfo a, AppWidgetProviderInfo b) {
                String left = a == null ? "" : String.valueOf(a.loadLabel(packageManager));
                String right = b == null ? "" : String.valueOf(b.loadLabel(packageManager));
                return left.compareToIgnoreCase(right);
            }
        });
        List<AppWidgetProviderInfo> available = new ArrayList<>();
        for (AppWidgetProviderInfo info : providers) if (info != null && info.provider != null) available.add(info);
        if (available.isEmpty()) {
            Toast.makeText(this, "No installed app widgets are available", Toast.LENGTH_LONG).show();
            return;
        }
        String[] labels = new String[available.size()];
        String[] icons = new String[available.size()];
        for (int i = 0; i < available.size(); i++) {
            AppWidgetProviderInfo info = available.get(i);
            labels[i] = String.valueOf(info.loadLabel(packageManager));
            icons[i] = "▦";
        }
        showActionDialog("Choose a widget", icons, labels, (dialog, which) -> beginBindingWidget(available.get(which)));
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
        addWidgetToCurrentPage(info, pendingWidgetId);
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
            addWidgetToCurrentPage(info, pendingWidgetId);
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
        int cropH = Math.max(dp(220), Math.min((int)(screenH * 0.56f), screenH - dp(190)));
        int cropW = Math.max(dp(120), Math.round(cropH * screenW / (float)Math.max(1, screenH)));
        WallpaperCropView preview = new WallpaperCropView(this, bitmap);
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(14), dp(14), dp(14), dp(12));
        panel.setBackground(roundRect(new int[]{Color.rgb(24, 31, 53), Color.rgb(12, 16, 29)}, dp(22)));
        panel.addView(text("Crop wallpaper", 20, Color.WHITE, true));
        TextView hint = text("Drag the image to adjust framing", 12, Color.rgb(185, 198, 225), false);
        hint.setPadding(0, dp(4), 0, dp(10));
        panel.addView(hint);
        FrameLayout frame = new FrameLayout(this);
        frame.setBackgroundColor(Color.BLACK);
        frame.addView(preview, new FrameLayout.LayoutParams(cropW, cropH, Gravity.CENTER));
        panel.addView(frame, new LinearLayout.LayoutParams(-1, cropH));
        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        TextView cancel = text("Cancel", 14, Color.LTGRAY, true);
        cancel.setPadding(dp(14), dp(12), dp(14), dp(12));
        TextView use = text("Use wallpaper", 14, Color.WHITE, true);
        use.setPadding(dp(14), dp(12), dp(14), dp(12));
        actions.addView(cancel); actions.addView(use); panel.addView(actions);
        Dialog dialog = new Dialog(this);
        dialog.setContentView(panel);
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(Math.min(screenW - dp(24), cropW + dp(36)), -2);
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

    private void addWidgetToCurrentPage(AppWidgetProviderInfo info, int widgetId) {
        if (pagesStrip == null) {
            Toast.makeText(this, "Open the home screen and try again", Toast.LENGTH_SHORT).show();
            return;
        }
        rebuildPages();
        ensurePageExists(Math.max(0, currentPage));
        renderHomePages(false);
        if (currentPage < 0 || currentPage >= pagesStrip.getChildCount()) currentPage = Math.max(0, pagesStrip.getChildCount() - 1);
        FrameLayout page = (FrameLayout) pagesStrip.getChildAt(currentPage);
        AppWidgetHostView widget = widgetHost.createView(this, widgetId, info);
        widget.setAppWidget(widgetId, info);
        addWidgetContainer(page, widget, widgetId, currentPage, dp(8), dp(8),
                Math.max(dp(180), page.getWidth() / 2), Math.max(dp(100), page.getHeight() / 4));
        String stored = prefs.getString(KEY_WIDGET_IDS, "");
        String entry = widgetId + ":" + currentPage;
        if (!containsWidgetId(stored, widgetId)) {
            prefs.edit().putString(KEY_WIDGET_IDS, stored.isEmpty() ? entry : stored + "\n" + entry).apply();
        }
        Toast.makeText(this, "Widget added. Long-press to move; use the corner handle to resize.", Toast.LENGTH_LONG).show();
        widgetHost.startListening();
    }

    private boolean containsWidgetId(String stored, int widgetId) {
        if (stored == null || stored.isEmpty()) return false;
        for (String entry : stored.split("\\n")) {
            String[] parts = entry.split(":");
            if (parts.length >= 2) {
                try { if (Integer.parseInt(parts[0]) == widgetId) return true; }
                catch (NumberFormatException ignored) {}
            }
        }
        return false;
    }

    private void addWidgetContainer(FrameLayout page, AppWidgetHostView widget, int widgetId, int pageIndex,
            int x, int y, int width, int height) {
        final FrameLayout container = new FrameLayout(this);
        container.setTag("launcher_widget:" + widgetId);
        widget.setTag("launcher_widget_content:" + widgetId);
        widget.setFocusable(true);
        container.addView(widget, new FrameLayout.LayoutParams(-1, -1));
        final View resizeHandle = new View(this);
        GradientDrawable handleBg = new GradientDrawable();
        handleBg.setColor(Color.rgb(115, 159, 255));
        handleBg.setCornerRadius(dp(5));
        resizeHandle.setBackground(handleBg);
        resizeHandle.setVisibility(View.GONE);
        FrameLayout.LayoutParams handleLp = new FrameLayout.LayoutParams(dp(26), dp(26), Gravity.RIGHT | Gravity.BOTTOM);
        container.addView(resizeHandle, handleLp);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(Math.max(dp(140), width), Math.max(dp(90), height), Gravity.TOP | Gravity.LEFT);
        lp.leftMargin = x;
        lp.topMargin = y;
        page.addView(container, lp);
        container.bringToFront();

        // 0 = normal widget interaction, 1 = move mode after 1 second,
        // 2 = resize mode after 2 seconds. Keep the widget's ordinary taps intact.
        final int[] editMode = {0};
        final float[] down = {0f, 0f};
        final int[] origin = {x, y, lp.width, lp.height};
        final Runnable[] movePress = new Runnable[1];
        final Runnable[] resizePress = new Runnable[1];
        Runnable showEditChrome = () -> {
            resizeHandle.setVisibility(View.VISIBLE);
            container.setBackground(roundRect(new int[]{Color.argb(65, 115, 159, 255), Color.argb(30, 115, 159, 255)}, dp(8)));
        };
        movePress[0] = () -> {
            editMode[0] = 1;
            showEditChrome.run();
        };
        resizePress[0] = () -> {
            editMode[0] = 2;
            showEditChrome.run();
            Toast.makeText(this, "Drag to resize; release to save", Toast.LENGTH_SHORT).show();
        };
        // The AppWidgetHostView fills the container and consumes most touch
        // events, so attach the move gesture there; the resize handle stays separate.
        widget.setOnTouchListener((view, event) -> {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                down[0] = event.getRawX();
                down[1] = event.getRawY();
                FrameLayout.LayoutParams current = (FrameLayout.LayoutParams) container.getLayoutParams();
                origin[0] = current.leftMargin; origin[1] = current.topMargin;
                origin[2] = current.width; origin[3] = current.height;
                editMode[0] = 0;
                resizeHandle.setVisibility(View.GONE);
                container.setBackground(null);
                handler.removeCallbacks(movePress[0]);
                handler.removeCallbacks(resizePress[0]);
                handler.postDelayed(movePress[0], 1000L);
                handler.postDelayed(resizePress[0], 2000L);
                return false;
            }
            if (action == MotionEvent.ACTION_MOVE) {
                float deltaX = event.getRawX() - down[0];
                float deltaY = event.getRawY() - down[1];
                if (editMode[0] == 0) {
                    if (Math.abs(deltaX) > dp(10) || Math.abs(deltaY) > dp(10)) {
                        handler.removeCallbacks(movePress[0]);
                        handler.removeCallbacks(resizePress[0]);
                    }
                    return false;
                }
                FrameLayout.LayoutParams current = (FrameLayout.LayoutParams) container.getLayoutParams();
                if (editMode[0] == 1) {
                    current.leftMargin = Math.max(0, Math.min(page.getWidth() - container.getWidth(),
                            origin[0] + Math.round(deltaX)));
                    current.topMargin = Math.max(0, Math.min(page.getHeight() - container.getHeight(),
                            origin[1] + Math.round(deltaY)));
                } else {
                    current.width = Math.max(dp(140), origin[2] + Math.round(deltaX));
                    current.height = Math.max(dp(90), origin[3] + Math.round(deltaY));
                    current.width = Math.min(current.width, Math.max(dp(140), page.getWidth() - current.leftMargin));
                    current.height = Math.min(current.height, Math.max(dp(90), page.getHeight() - current.topMargin));
                }
                container.setLayoutParams(current);
                return true;
            }
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                handler.removeCallbacks(movePress[0]);
                handler.removeCallbacks(resizePress[0]);
                if (editMode[0] != 0) {
                    saveWidgetGeometry(widgetId, pageIndex, container);
                    resizeHandle.setVisibility(View.GONE);
                    container.setBackground(null);
                    editMode[0] = 0;
                    return true;
                }
            }
            return editMode[0] != 0;
        });
        resizeHandle.setOnTouchListener((v, event) -> {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                down[0] = event.getRawX(); down[1] = event.getRawY();
                FrameLayout.LayoutParams current = (FrameLayout.LayoutParams) container.getLayoutParams();
                origin[2] = current.width; origin[3] = current.height;
                return true;
            }
            if (action == MotionEvent.ACTION_MOVE) {
                FrameLayout.LayoutParams current = (FrameLayout.LayoutParams) container.getLayoutParams();
                current.width = Math.max(dp(140), Math.min(page.getWidth() - current.leftMargin,
                        origin[2] + Math.round(event.getRawX() - down[0])));
                current.height = Math.max(dp(90), Math.min(page.getHeight() - current.topMargin,
                        origin[3] + Math.round(event.getRawY() - down[1])));
                container.setLayoutParams(current);
                return true;
            }
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                saveWidgetGeometry(widgetId, pageIndex, container);
                return true;
            }
            return true;
        });
    }

    private void saveWidgetGeometry(int widgetId, int pageIndex, FrameLayout container) {
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) container.getLayoutParams();
        prefs.edit().putInt("widget_x_" + widgetId, lp.leftMargin)
                .putInt("widget_y_" + widgetId, lp.topMargin)
                .putInt("widget_w_" + widgetId, lp.width)
                .putInt("widget_h_" + widgetId, lp.height).apply();
        String stored = prefs.getString(KEY_WIDGET_IDS, "");
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
        prefs.edit().putString(KEY_WIDGET_IDS, updated.toString()).apply();
    }

    private void restoreWidgetsOnPages() {
        String stored = prefs.getString(KEY_WIDGET_IDS, "");
        if (stored.isEmpty()) return;
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
                if (info == null) continue;
                int width = prefs.getInt("widget_w_" + id, Math.max(dp(180), page.getWidth() / 2));
                int height = prefs.getInt("widget_h_" + id, Math.max(dp(100), page.getHeight() / 4));
                int x = prefs.getInt("widget_x_" + id, dp(8));
                int y = prefs.getInt("widget_y_" + id, dp(8));
                AppWidgetHostView widget = widgetHost.createView(this, id, info);
                widget.setAppWidget(id, info);
                addWidgetContainer(page, widget, id, pageIndex, x, y, width, height);
            } catch (Exception ignored) {}
        }
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

    private java.io.InputStream openWallpaperStream(Uri uri) throws java.io.IOException {
        if ("file".equalsIgnoreCase(uri.getScheme())) {
            return new java.io.FileInputStream(new java.io.File(uri.getPath()));
        }
        return getContentResolver().openInputStream(uri);
    }

    private boolean applyHomeWallpaper() {
        if (root == null) return false;
        String saved = prefs == null ? null : prefs.getString(KEY_WALLPAPER_URI, null);
        if (saved != null && !saved.isEmpty()) {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            try {
                Uri uri = Uri.parse(saved);
                try (java.io.InputStream in = openWallpaperStream(uri)) {
                    if (in != null) BitmapFactory.decodeStream(in, null, bounds);
                }
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw new java.io.IOException("Image could not be decoded");
                int screenW = Math.max(1, getResources().getDisplayMetrics().widthPixels);
                int screenH = Math.max(1, getResources().getDisplayMetrics().heightPixels);
                int sample = 1;
                while (bounds.outWidth / (sample * 2) > screenW || bounds.outHeight / (sample * 2) > screenH) sample *= 2;
                BitmapFactory.Options decode = new BitmapFactory.Options();
                decode.inSampleSize = sample;
                try (java.io.InputStream in = openWallpaperStream(uri)) {
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
        int theme = prefs == null ? 0 : prefs.getInt(KEY_COLOR_THEME, 0);
        if (theme == 1) root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{Color.rgb(5, 35, 52), Color.rgb(7, 65, 84), BG}));
        else if (theme == 2) root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{Color.rgb(43, 19, 69), Color.rgb(65, 39, 104), BG}));
        else if (theme == 3) root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{Color.rgb(42, 46, 58), Color.rgb(25, 29, 39), BG}));
        else root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{Color.rgb(11, 22, 51), Color.rgb(20, 42, 88), BG}));
        return false;
    }

    private void launchApp(ResolveInfo app) {
        Intent intent = packageManager.getLaunchIntentForPackage(app.activityInfo.packageName);
        if (intent != null) {
            try { startActivity(intent); } catch (Exception e) { Toast.makeText(this, "Unable to open app", Toast.LENGTH_SHORT).show(); }
        }
    }

    /** Draws the selected clock in a chunky 5x7 square-pixel font. */
    private void applyClockStyle() {
        if (clockText == null) return;
        int style = prefs == null ? 0 : prefs.getInt(KEY_CLOCK_STYLE, 0);
        clockText.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT);
        switch (style) {
            case 1: // Cyber HUD: sharply condensed, italic display lettering.
                clockText.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD_ITALIC));
                clockText.setTextSize(40);
                clockText.setLetterSpacing(0.035f);
                clockText.setTextColor(Color.rgb(87, 229, 255));
                clockText.setShadowLayer(dp(4), 0f, 0f, Color.argb(125, 0, 190, 255));
                break;
            case 2: // Neon Circuit: heavy geometric system sans, strong block-like digits.
                clockText.setTypeface(Typeface.create("sans-serif-black", Typeface.NORMAL));
                clockText.setTextSize(38);
                clockText.setLetterSpacing(0.015f);
                clockText.setTextColor(Color.rgb(112, 255, 190));
                clockText.setShadowLayer(dp(5), 0f, 0f, Color.argb(105, 0, 255, 135));
                break;
            case 3: // Tech Minimal: thin, airy futuristic typography with no glow.
                clockText.setTypeface(Typeface.create("sans-serif-thin", Typeface.NORMAL));
                clockText.setTextSize(44);
                clockText.setLetterSpacing(0.065f);
                clockText.setTextColor(Color.rgb(225, 237, 255));
                break;
            case 4: // Terminal Digital: bold fixed-width terminal glyphs with phosphor-green readout.
                clockText.setTypeface(Typeface.create("monospace", Typeface.BOLD));
                clockText.setTextSize(39);
                clockText.setLetterSpacing(0.09f);
                clockText.setTextColor(Color.rgb(125, 255, 150));
                clockText.setShadowLayer(dp(3), 0f, 0f, Color.argb(95, 60, 255, 100));
                break;
            case 5: // Square Pixel: custom chunky square-pixel numerals, not a system font.
                clockText.setTypeface(Typeface.create("monospace", Typeface.BOLD));
                clockText.setTextSize(40);
                clockText.setLetterSpacing(0f);
                clockText.setTextColor(Color.rgb(145, 255, 170));
                clockText.setShadowLayer(dp(2), 0f, 0f, Color.argb(70, 60, 255, 100));
                break;
            default: // Digital: fixed-width digital readout with bold squared-off glyphs.
                clockText.setTypeface(Typeface.create("sans-serif-monospace", Typeface.BOLD));
                clockText.setTextSize(40);
                clockText.setLetterSpacing(0.055f);
                clockText.setTextColor(Color.rgb(118, 204, 255));
                clockText.setShadowLayer(dp(5), 0f, 0f, Color.argb(135, 40, 132, 255));
                break;
        }
    }

    private void updateClock() {
        if (clockText == null || dateText == null) return;
        Date now = new Date();
        clockText.setText(new SimpleDateFormat(prefs.getBoolean(KEY_24_HOUR, false) ? "HH:mm" : "hh:mm a", Locale.getDefault()).format(now));
        dateText.setVisibility(prefs.getBoolean(KEY_SHOW_DATE, true) ? View.VISIBLE : View.GONE);
        dateText.setText(new SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(now));
        applyClockStyle();
        applyBrandVisibility();
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
        TextView header = text(title.toUpperCase(Locale.getDefault()), 11, Color.rgb(146, 171, 239), true);
        header.setLetterSpacing(0.10f);
        header.setPadding(dp(4), dp(15), dp(4), dp(7));
        panel.addView(header, new LinearLayout.LayoutParams(-1, -2));
    }

    private void addSettingRow(LinearLayout panel, String icon, String title, String detail, Runnable action) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(10), dp(8), dp(10), dp(8));
        row.setBackground(roundRect(new int[]{Color.rgb(36, 44, 68), Color.rgb(25, 31, 50)}, dp(15)));
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, -2);
        rp.bottomMargin = dp(6);
        TextView glyph = text(icon, 19, Color.rgb(177, 198, 255), true);
        glyph.setGravity(Gravity.CENTER);
        row.addView(glyph, new LinearLayout.LayoutParams(dp(34), dp(38)));
        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.setPadding(dp(9), 0, 0, 0);
        copy.addView(text(title, 14, Color.WHITE, true));
        TextView sub = text(detail, 11, Color.rgb(176, 187, 213), false);
        sub.setPadding(0, dp(2), 0, 0);
        copy.addView(sub);
        row.addView(copy, new LinearLayout.LayoutParams(0, -2, 1f));
        row.setOnClickListener(v -> action.run());
        panel.addView(row, rp);
    }

    private void addChoiceRow(LinearLayout panel, String title, int selected, String[] choices, IntAction onSelect) {
        TextView heading = text(title, 14, Color.WHITE, true);
        heading.setPadding(dp(4), dp(5), dp(4), dp(8));
        panel.addView(heading, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout choicesRow = new LinearLayout(this);
        choicesRow.setGravity(Gravity.CENTER_VERTICAL);
        for (int i = 0; i < choices.length; i++) {
            final int value = i;
            TextView chip = text(choices[i], 12, i == selected ? Color.WHITE : Color.rgb(196, 205, 228), i == selected);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(dp(5), dp(10), dp(5), dp(10));
            chip.setBackground(roundRect(i == selected ? new int[]{Color.rgb(73, 99, 177), Color.rgb(73, 74, 157)} : new int[]{Color.rgb(34, 41, 62), Color.rgb(28, 34, 51)}, dp(12)));
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, dp(40), 1f);
            cp.setMargins(dp(2), 0, dp(2), dp(8));
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
            panel.setPadding(dp(16), dp(12), dp(16), dp(14));
            panel.setBackground(roundRect(new int[]{Color.rgb(24, 31, 53), Color.rgb(12, 16, 29)}, dp(24)));
            TextView title = text("Home Studio", 23, Color.WHITE, true);
            title.setPadding(dp(4), dp(5), dp(4), dp(3));
            panel.addView(title);
            TextView subtitle = text("Personalize your home and app drawer", 12, Color.rgb(179, 191, 221), false);
            subtitle.setPadding(dp(4), 0, dp(4), dp(7));
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
            drawerGrid.setVerticalSpacing(dp(spacing)); drawerGrid.setHorizontalSpacing(dp(spacing / 2));
            showCustomizationMenu(dialog, scroll.getScrollY(), false);
        });

        addSection(content, "Appearance");
        addChoiceRow(content, "Color theme", prefs.getInt(KEY_COLOR_THEME, 0), new String[]{"Midnight", "Ocean", "Violet", "Graphite"}, value -> {
            prefs.edit().putInt(KEY_COLOR_THEME, value).apply();
            drawerScreen.setBackground(drawerBackground()); applyHomeWallpaper();
            showCustomizationMenu(dialog, scroll.getScrollY(), false);
        });
        addChoiceRow(content, "Clock style", prefs.getInt(KEY_CLOCK_STYLE, 0), new String[]{"Digital", "Cyber HUD", "Neon Circuit", "Tech Minimal", "Terminal Digital", "Square Pixel"}, value -> {
            prefs.edit().putInt(KEY_CLOCK_STYLE, value).apply();
            applyClockStyle();
            showCustomizationMenu(dialog, scroll.getScrollY(), false);
        });
        addSettingRow(content, "12/24", "Clock format", prefs.getBoolean(KEY_24_HOUR, false) ? "24-hour" : "12-hour", () -> {
            prefs.edit().putBoolean(KEY_24_HOUR, !prefs.getBoolean(KEY_24_HOUR, false)).apply();
            updateClock(); showCustomizationMenu(dialog, scroll.getScrollY(), false);
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
            beginWidgetPicker();
        });
        addSettingRow(content, "＋", "Add apps to Home", "Choose shortcuts for the home screen", () -> { dialog.dismiss(); showAddApps(); });
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
            drawerGrid.setNumColumns(getDrawerColumns()); drawerGrid.setVerticalSpacing(dp(8)); drawerGrid.setHorizontalSpacing(dp(4));
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
        if (dialog.getWindow() != null) dialog.getWindow().setLayout(Math.min(getResources().getDisplayMetrics().widthPixels - dp(24), dp(480)),
                Math.min(getResources().getDisplayMetrics().heightPixels - dp(70), dp(720)));
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
