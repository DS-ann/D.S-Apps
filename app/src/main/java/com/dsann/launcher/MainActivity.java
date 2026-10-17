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
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Drawable;
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
import java.util.Calendar;
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
    private static final String KEY_ICON_BACKGROUND_COLOR = "icon_background_color";
    private static final String KEY_DRAWER_COLUMNS = "drawer_columns";
    private static final String KEY_DRAWER_ICON_SIZE = "drawer_icon_size";
    private static final String KEY_HOME_FONT_SIZE = "home_font_size";
    private static final String KEY_DRAWER_FONT_SIZE = "drawer_font_size";
    private static final String KEY_CLOCK_STYLE = "clock_style";
    private static final String KEY_PAGE_ROTATION = "page_rotation_degrees";
    private static final String KEY_CUBE_ROTATION = "cube_rotation_degrees";
    private static final String KEY_ANIMATION_SPEED = "animation_speed";
    private static final String KEY_PAGE_TRANSITION = "page_transition";
    private static final String KEY_DRAWER_TRANSITION = "drawer_transition";
    private static final String KEY_DRAWER_OPACITY = "drawer_background_opacity";
    private static final String KEY_CLOCK_SIZE = "clock_size"; // Legacy preference; retained for compatibility.
    private static final String KEY_CLOCK_LENGTH = "clock_length_percent";
    private static final String KEY_CLOCK_BREADTH = "clock_breadth_percent";
    private static final String KEY_CLOCK_BG_OPACITY = "clock_background_opacity";
    private static final String KEY_CLOCK_POSITION = "clock_position";
    private static final String KEY_CLOCK_WIDGET_X = "clock_widget_x";
    private static final String KEY_CLOCK_WIDGET_Y = "clock_widget_y";
    private static final String KEY_CLOCK_WIDGET_ENABLED = "clock_widget_enabled";
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
    private static final String KEY_SHORTCUT_X = "contact_shortcut_x";
    private static final String KEY_SHORTCUT_Y = "contact_shortcut_y";
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
    private boolean clockUpdatesActive = false;
    private final java.util.Map<TextView, Integer> appliedClockStyles = new java.util.WeakHashMap<>();
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
    private final View[] workspaceGridOverlays = new View[2];
    // Items temporarily displaced during a drag are restored as soon as the
    // moving item leaves their original occupied area.
    private final java.util.IdentityHashMap<View, int[]> liveDisplacedItems = new java.util.IdentityHashMap<>();
    private int workspaceGridCellWidth = 1;
    private int workspaceGridCellHeight = 1;
    private int workspaceGridWidgetWidth = 1;
    private int workspaceGridWidgetHeight = 1;
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
    private final LinearLayout[] clockPanelsByPage = new LinearLayout[2];
    private final TextView[] clockTextsByPage = new TextView[2];
    private final TextView[] amPmTextsByPage = new TextView[2];
    private final TextView[] dateTextsByPage = new TextView[2];
    private final FrameLayout[] clockRowsByPage = new FrameLayout[2];
    private final FrameLayout[] clockGroupsByPage = new FrameLayout[2];
    private int activeClockPage = 0;
    private View clockWidgetResizeHandle;
    private final View[] clockEdgeHandles = new View[4];
    private boolean clockWidgetEditing = false;
    private boolean clockWidgetDragging = false;
    private boolean clockWidgetMoving = false;
    private float clockWidgetDownRawX, clockWidgetDownRawY;
    private float clockWidgetStartX, clockWidgetStartY;
    private int clockWidgetStartLength, clockWidgetStartBreadth;
    private int clockWidgetStartScreenX, clockWidgetStartScreenY;
    private int clockWidgetStartPanelWidth, clockWidgetStartPanelHeight;
    private float clockWidgetStartPanelScaleX = 1f;
    private boolean clockWidgetLongPressActivated = false;
    // 0 = none, 1 = left edge, 2 = right edge, 3 = top edge, 4 = bottom edge.
    private int clockResizeEdge = 0;
    // Invalidates stale layout callbacks when a newer resize frame or gesture starts.
    private int clockResizeGeneration = 0;
    private boolean gestureStartedOnClock = false;
    private TextView pageIndicator;
    private LauncherAppAdapter drawerAdapter;
    private List<ResolveInfo> allApps = new ArrayList<>();
    private List<ResolveInfo> drawerApps = new ArrayList<>();
    private final List<String> homePackages = new ArrayList<>();
    private final List<List<String>> pages = new ArrayList<>();
    private int currentPage = 0;
    private float gestureDownX, gestureDownY;
    private float drawerSwipeProgress = 0f;
    private boolean drawerSwipeVisualActive = false;
    private boolean gestureStartedOnAppCell = false;
    private boolean gestureStartedOnWidget = false;
    private boolean gestureStartedOnContactShortcut = false;
    private boolean gestureStartedOnResizeHandle = false;
    private boolean pageSwipeActive = false;
    private boolean pageSwipeChildCancelled = false;
    private boolean verticalSwipeClaimed = false;
    private boolean gestureStartedOnDrawerGrid = false;
    private boolean drawerCloseSwipeClaimed = false;
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
            if (intent == null) return;
            String action = intent.getAction();
            if (!Intent.ACTION_PACKAGE_ADDED.equals(action)
                    && !Intent.ACTION_PACKAGE_REMOVED.equals(action)) {
                return;
            }
            // Ignore the remove/add broadcasts generated while an app is being updated.
            // The drawer list is refreshed only for a genuine install or uninstall.
            if (intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) return;

            // PackageManager can briefly return a stale launcher-activity query immediately
            // after the broadcast. Debounce the refresh so the installed/uninstalled package
            // is reflected by the single drawer refresh.
            handler.removeCallbacks(refreshAppsAfterPackageChange);
            handler.postDelayed(refreshAppsAfterPackageChange, 800L);
        }
    };
    private final Runnable refreshAppsAfterPackageChange = new Runnable() {
        @Override public void run() { loadApps(); }
    };
    private final Runnable clockUpdater = new Runnable() {
        @Override public void run() {
            if (!clockUpdatesActive) return;
            updateClock();
            // Align refreshes to the system minute boundary instead of drifting
            // from the time at which the launcher was opened.
            long remainder = System.currentTimeMillis() % 60000L;
            handler.postDelayed(this, remainder == 0L ? 60000L : 60000L - remainder);
        }
    };
    private final BroadcastReceiver timeChangesReceiver = new BroadcastReceiver() {
        @Override public void onReceive(android.content.Context context, Intent intent) {
            if (!clockUpdatesActive) return;
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
                    @Override public void updateLiveDisplacements(FrameLayout page, View movingView) {
                        MainActivity.this.updateLiveDisplacements(page, movingView);
                    }
                    @Override public void finishLiveDisplacements(boolean commit, View movingView) {
                        MainActivity.this.finishLiveDisplacements(commit, movingView);
                    }
                    @Override public void setWorkspaceGridVisible(boolean visible, View movingView) {
                        MainActivity.this.setWorkspaceGridVisible(visible, movingView);
                    }
                });
        // Let the wallpaper flow behind the status bar so it feels like launcher chrome,
        // while retaining the system icons and reserving their inset for launcher content.
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(BG);
        getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
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
            verticalSwipeClaimed = false;
            drawerCloseSwipeClaimed = false;
            drawerSwipeProgress = 0f;
            drawerSwipeVisualActive = false;
            gestureStartedOnDrawerGrid = drawerOpen && touchStartedOnDrawerGrid(event.getRawX(), event.getRawY());
            drawerAtTopOnGestureDown = !drawerOpen || isDrawerAtTop();
            gestureStartedOnAppCell = touchStartedOnAppCell(event.getRawX(), event.getRawY());
            gestureStartedOnWidget = touchStartedOnWidget(event.getRawX(), event.getRawY());
            gestureStartedOnContactShortcut = touchStartedOnContactShortcut(event.getRawX(), event.getRawY());
            gestureStartedOnResizeHandle = touchStartedOnResizeHandle(event.getRawX(), event.getRawY());
            // Resize grips live in the root overlay and sit partly outside the
            // clock's normal hit rectangle. Test them BEFORE outside-tap dismissal.
            clockResizeEdge = clockWidgetEditing
                    ? hitClockResizeEdge(event.getRawX(), event.getRawY()) : 0;
            gestureStartedOnClock = touchStartedOnClock(event.getRawX(), event.getRawY())
                    || clockResizeEdge != 0;
            if (clockWidgetEditing && !gestureStartedOnClock) {
                exitClockWidgetEditMode();
            }
            if (clockWidgetEditing && gestureStartedOnClock && clockResizeEdge == 0) {
                // In edit mode, a fresh touch on the clock belongs to clock movement,
                // never to workspace page/drawer gestures.
                clockWidgetMoving = true;
                clockWidgetDownRawX = event.getRawX();
                clockWidgetDownRawY = event.getRawY();
                clockWidgetStartX = clockPanel.getTranslationX();
                clockWidgetStartY = clockPanel.getTranslationY();
                setWorkspaceGridVisible(true, clockPanel);
                handler.removeCallbacks(clockWidgetLongPress);
                return true;
            }
            if (clockResizeEdge != 0 && clockWidgetEditing) {
                    clockWidgetDragging = true;
                    clockWidgetDownRawX = event.getRawX();
                    clockWidgetDownRawY = event.getRawY();
                    clockWidgetStartLength = getClockLength();
                    clockWidgetStartBreadth = getClockBreadth();
                    int[] clockStartLocation = new int[2];
                    clockPanel.getLocationOnScreen(clockStartLocation);
                    clockWidgetStartScreenX = clockStartLocation[0];
                    clockWidgetStartScreenY = clockStartLocation[1];
                    clockWidgetStartPanelWidth = clockPanel.getWidth();
                    clockWidgetStartPanelHeight = clockPanel.getHeight();
                    clockWidgetStartPanelScaleX = clockPanel.getScaleX();
                    clockResizeGeneration++;
                    clockWidgetStartX = clockPanel.getTranslationX();
                    clockWidgetStartY = clockPanel.getTranslationY();
                    setWorkspaceGridVisible(true, clockPanel);
                    return true;
                }
            handler.removeCallbacks(homeLongPress);
            handler.removeCallbacks(clockWidgetLongPress);
            // Clock edit/resize gestures must never be interpreted as workspace gestures.
            if (gestureStartedOnClock && !drawerOpen) handler.postDelayed(clockWidgetLongPress, 650L);
            if (!drawerOpen && !gestureStartedOnAppCell && !gestureStartedOnWidget
                    && !gestureStartedOnContactShortcut && !gestureStartedOnClock)
                handler.postDelayed(homeLongPress, 2000L);
            return super.dispatchTouchEvent(event);
        }

        float dx = event.getX() - gestureDownX;
        float dy = event.getY() - gestureDownY;
        if (action == MotionEvent.ACTION_MOVE) {
            if (clockWidgetMoving && clockWidgetEditing && clockPanel != null) {
                float density = Math.max(0.5f, getResources().getDisplayMetrics().density);
                float moveX = (event.getRawX() - clockWidgetDownRawX) / density;
                float moveY = (event.getRawY() - clockWidgetDownRawY) / density;
                setClampedClockTranslation(clockWidgetStartX + moveX,
                        clockWidgetStartY + moveY, true);
                updateClockResizeHandles();
                return true;
            }
            if (clockResizeEdge != 0 && clockWidgetDragging) {
                float density = Math.max(0.5f, getResources().getDisplayMetrics().density);
                int resizeDx = Math.round((event.getRawX() - clockWidgetDownRawX) / density);
                int resizeDy = Math.round((event.getRawY() - clockWidgetDownRawY) / density);
                int lengthDelta = clockResizeEdge == 1 ? -resizeDx : (clockResizeEdge == 2 ? resizeDx : 0);
                int breadthDelta = clockResizeEdge == 3 ? -resizeDy : (clockResizeEdge == 4 ? resizeDy : 0);
                prefs.edit()
                        .putInt(clockPrefKey(KEY_CLOCK_LENGTH, activeClockPage), Math.max(80, Math.min(250, clockWidgetStartLength + lengthDelta)))
                        .putInt(clockPrefKey(KEY_CLOCK_BREADTH, activeClockPage), Math.max(80, Math.min(250, clockWidgetStartBreadth + breadthDelta)))
                        .apply();
                applyClockPositionAndSize();
                final int resizingEdge = clockResizeEdge;
                final int resizeGeneration = ++clockResizeGeneration;
                clockPanel.post(() -> {
                    // Complete the final geometry correction even if ACTION_UP arrived
                    // before this layout callback ran; discard only stale resize frames.
                    if (resizeGeneration != clockResizeGeneration || clockPanel == null) return;
                    int[] now = new int[2];
                    clockPanel.getLocationOnScreen(now);
                    float targetX = clockWidgetStartScreenX;
                    float targetY = clockWidgetStartScreenY;
                    // Preserve the opposite VISUAL edge. Vertical scaling changes
                    // the panel's drawn height while its reserved layout height stays fixed.
                    if (resizingEdge == 1) {
                        float startVisualWidth = clockWidgetStartPanelWidth * clockWidgetStartPanelScaleX;
                        float currentVisualWidth = clockPanel.getWidth() * clockPanel.getScaleX();
                        targetX += startVisualWidth - currentVisualWidth;
                    } else if (resizingEdge == 3) {
                        // The panel now changes its real measured height with breadth;
                        // preserve the opposite (bottom) edge using actual visual bounds.
                        float startVisualHeight = clockWidgetStartPanelHeight;
                        float currentVisualHeight = clockPanel.getHeight() * clockPanel.getScaleY();
                        targetY += startVisualHeight - currentVisualHeight;
                    }
                    setClampedClockTranslation(
                            clockPanel.getTranslationX() + targetX - now[0],
                            clockPanel.getTranslationY() + targetY - now[1], true);
                    // Clamp the actual scaled panel after layout; translation-only
                    // movement already had a boundary, but breadth/length resize did not.
                    float[] bounded = clampClockTranslation(clockPanel,
                            clockPanel.getTranslationX(), clockPanel.getTranslationY());
                    setClampedClockTranslation(bounded[0], bounded[1], true);
                    updateClockResizeHandles();
                });
                return true;
            }
            // In the drawer, only a downward pull starting at the very top closes it.
            // All other vertical gestures are left to the app grid for normal scrolling.
            boolean verticalSwipe = Math.abs(dy) > LauncherUiUtils.dp(this, 12)
                    && Math.abs(dy) > Math.abs(dx) * 1.15f;
            if (drawerCloseSwipeClaimed) {
                int swipeHeight = Math.max(1, pageViewport != null && pageViewport.getHeight() > 0
                        ? pageViewport.getHeight() : getResources().getDisplayMetrics().heightPixels);
                drawerSwipeProgress = Math.max(0f, Math.min(1f, 1f - dy / (float) swipeHeight));
                LauncherScreenTransitions.updateDrawerSwipe(this, homeScreen, drawerScreen,
                        getDrawerTransition(), drawerSwipeProgress);
                return true;
            }
            if (drawerOpen && gestureStartedOnDrawerGrid && drawerAtTopOnGestureDown
                    && dy > 0 && verticalSwipe) {
                drawerCloseSwipeClaimed = true;
                int swipeHeight = Math.max(1, pageViewport != null && pageViewport.getHeight() > 0
                        ? pageViewport.getHeight() : getResources().getDisplayMetrics().heightPixels);
                drawerSwipeProgress = Math.max(0f, Math.min(1f, 1f - dy / (float) swipeHeight));
                homeScreen.animate().cancel();
                drawerScreen.animate().cancel();
                LauncherScreenTransitions.updateDrawerSwipe(this, homeScreen, drawerScreen,
                        getDrawerTransition(), drawerSwipeProgress);
                handler.removeCallbacks(homeLongPress);
                MotionEvent cancel = MotionEvent.obtain(event);
                cancel.setAction(MotionEvent.ACTION_CANCEL);
                super.dispatchTouchEvent(cancel);
                cancel.recycle();
                return true;
            }
            // A held item gesture belongs to its move/resize controller, not workspace navigation.
            // Fast vertical swipes can still start on home items; explicit resize handles are reserved.
            boolean heldItemGesture = (gestureStartedOnAppCell || gestureStartedOnWidget
                    || gestureStartedOnContactShortcut)
                    && event.getEventTime() - event.getDownTime() > 450L;
            boolean itemManipulation = gestureStartedOnClock || gestureStartedOnResizeHandle || iconDragArmed || iconDragging
                    || heldItemGesture;
            if (!drawerOpen && !itemManipulation
                    && !verticalSwipeClaimed && verticalSwipe) {
                verticalSwipeClaimed = true;
                int swipeHeight = Math.max(1, pageViewport != null && pageViewport.getHeight() > 0
                        ? pageViewport.getHeight() : getResources().getDisplayMetrics().heightPixels);
                drawerSwipeProgress = Math.max(0f, Math.min(1f, -dy / (float) swipeHeight));
                if (dy < 0f) {
                    drawerSwipeVisualActive = true;
                    homeScreen.animate().cancel();
                    drawerScreen.animate().cancel();
                    LauncherScreenTransitions.updateDrawerSwipe(this, homeScreen, drawerScreen,
                            getDrawerTransition(), drawerSwipeProgress);
                }
                handler.removeCallbacks(homeLongPress);
                handler.removeCallbacks(clockWidgetLongPress);
                MotionEvent cancel = MotionEvent.obtain(event);
                cancel.setAction(MotionEvent.ACTION_CANCEL);
                super.dispatchTouchEvent(cancel);
                cancel.recycle();
                return true;
            }
            if (verticalSwipeClaimed) {
                if (dy < 0f || drawerSwipeVisualActive) {
                    int swipeHeight = Math.max(1, pageViewport != null && pageViewport.getHeight() > 0
                            ? pageViewport.getHeight() : getResources().getDisplayMetrics().heightPixels);
                    drawerSwipeProgress = Math.max(0f, Math.min(1f, -dy / (float) swipeHeight));
                    if (dy < 0f) drawerSwipeVisualActive = true;
                    LauncherScreenTransitions.updateDrawerSwipe(this, homeScreen, drawerScreen,
                            getDrawerTransition(), drawerSwipeProgress);
                }
                return true;
            }
            if (drawerCloseSwipeClaimed) return true;
            if (Math.abs(event.getX() - longPressDownX) > LauncherUiUtils.dp(this, 12)
                    || Math.abs(event.getY() - longPressDownY) > LauncherUiUtils.dp(this, 12)) {
                handler.removeCallbacks(homeLongPress);
            }
            // A true wheel/cylinder turn: the two page surfaces pivot on their shared
            // vertical edge. Keep the strip stationary while the finger controls the angle.
            // Let a clear horizontal swipe starting on a widget change pages too.
            // The widget's touch stream is cancelled once the swipe threshold is crossed.
            // A gesture that starts on a widget belongs to its own touch/drag
            // controller. Do not let the launcher steal it for page navigation:
            // otherwise moving/resizing a widget can rotate pages underneath it.
            // Page changes remain available from the free workspace surface.
            if (!drawerOpen && pagesStrip != null && pagesStrip.getChildCount() > 1
                    // Once a page swipe has claimed the touch stream, item long-press/drag
                    // classification must not pause its progress midway through the gesture.
                    && (pageSwipeActive || (!gestureStartedOnClock && !gestureStartedOnContactShortcut
                        && !gestureStartedOnResizeHandle && !iconDragArmed && !iconDragging
                        && !heldItemGesture
                        && Math.abs(dx) > LauncherUiUtils.dp(this, 12)
                        // Permit diagonal swipes; once claimed, the gesture remains owned
                        // by the page transition regardless of later finger angle.
                        && Math.abs(dx) > Math.abs(dy) * 0.75f))) {
                // Lock the direction chosen when the swipe is first claimed.
                // Reversing the finger then rewinds progress instead of swapping faces
                // underneath the gesture and leaving a 3D transform stranded.
                int direction = pageSwipeActive && wheelSwipeDirection != 0
                        ? wheelSwipeDirection : (dx < 0f ? 1 : -1);
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
                    // Progress is signed against the locked direction. Moving back
                    // toward the starting point reverses the transition smoothly to 0;
                    // it must never change which neighbouring page is being animated.
                    float directedDistance = -dx * wheelSwipeDirection;
                    float progress = Math.max(0f, Math.min(1f,
                            directedDistance / (float) pageWidth));
                    wheelSwipeProgress = progress;
                    applyWheelSwipe(wheelSwipeDirection, progress);
                    return true;
                } else if (pageSwipeActive) {
                    // If the finger crosses back past the origin, return the wheel
                    // to its neutral pose instead of leaving the pages frozen mid-turn.
                    int pageWidth = pageViewport != null && pageViewport.getWidth() > 0
                            ? pageViewport.getWidth()
                            : Math.max(1, getResources().getDisplayMetrics().widthPixels
                                    - LauncherUiUtils.dp(this, 40));
                    wheelSwipeProgress = 0f;
                    applyWheelSwipe(wheelSwipeDirection, 0f);
                    return true;
                }
            }
            return pageSwipeActive || super.dispatchTouchEvent(event);
        }

        if (action == MotionEvent.ACTION_CANCEL) {
            handler.removeCallbacks(clockWidgetLongPress);
            if (clockWidgetMoving) {
                clockWidgetMoving = false;
                if (clockPanel != null) {
                    clockPanel.setTranslationX(clockWidgetStartX);
                    clockPanel.setTranslationY(clockWidgetStartY);
                }
                prefs.edit()
                        .putFloat(clockPrefKey(KEY_CLOCK_WIDGET_X, activeClockPage), clockWidgetStartX)
                        .putFloat(clockPrefKey(KEY_CLOCK_WIDGET_Y, activeClockPage), clockWidgetStartY)
                        .apply();
                setWorkspaceGridVisible(false, null);
                updateClockResizeHandles();
                return true;
            }
            if (clockResizeEdge != 0) {
                finishClockResize(true);
                return true;
            }
            handler.removeCallbacks(homeLongPress);
            if (drawerCloseSwipeClaimed) {
                drawerCloseSwipeClaimed = false;
                // Progress is the remaining-open fraction: a 10% downward
                // swipe leaves progress at 90%, which is enough to close to Home.
                if (drawerSwipeProgress > 0.90f) {
                    LauncherScreenTransitions.finishDrawerSwipeToDrawer(this, homeScreen, drawerScreen,
                            () -> drawerOpen = true, getDrawerTransition(), drawerSwipeProgress);
                } else {
                    LauncherScreenTransitions.finishDrawerSwipeToHome(this, homeScreen, drawerScreen,
                            () -> drawerOpen = false, getDrawerTransition(), drawerSwipeProgress);
                }
                return true;
            }
            if (drawerSwipeVisualActive) {
                drawerSwipeVisualActive = false;
                // A cancelled upward opening settles to the nearest stable screen.
                if (drawerSwipeProgress >= 0.10f) {
                    LauncherScreenTransitions.finishDrawerSwipeToDrawer(this, homeScreen, drawerScreen,
                            () -> drawerOpen = true, getDrawerTransition(), drawerSwipeProgress);
                } else {
                    LauncherScreenTransitions.cancelDrawerSwipe(this, homeScreen, drawerScreen,
                            getDrawerTransition());
                }
                return true;
            }
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
            handler.removeCallbacks(clockWidgetLongPress);
            if (clockWidgetMoving) {
                clockWidgetMoving = false;
                setWorkspaceGridVisible(false, null);
                updateClockResizeHandles();
                return true;
            }
            if (clockResizeEdge != 0) {
                finishClockResize(false);
                return true;
            }
            handler.removeCallbacks(homeLongPress);
            if (longPressTriggered) return true;
            if (drawerCloseSwipeClaimed) {
                drawerCloseSwipeClaimed = false;
                // A 10% downward pull is enough to complete the drawer-to-Home
                // transition; progress represents how much of the drawer remains open.
                if (drawerSwipeProgress > 0.90f) {
                    LauncherScreenTransitions.finishDrawerSwipeToDrawer(this, homeScreen, drawerScreen,
                            () -> drawerOpen = true, getDrawerTransition(), drawerSwipeProgress);
                } else {
                    LauncherScreenTransitions.finishDrawerSwipeToHome(this, homeScreen, drawerScreen,
                            () -> drawerOpen = false, getDrawerTransition(), drawerSwipeProgress);
                }
                return true;
            }
            if (drawerSwipeVisualActive) {
                drawerSwipeVisualActive = false;
                // A drawer-opening gesture owns the complete touch sequence. If the
                // finger reverses or lifts mid-transition, settle to the nearest
                // endpoint instead of starting control-center navigation or leaving
                // the two surfaces in an intermediate pose.
                if (drawerSwipeProgress >= 0.10f) {
                    LauncherScreenTransitions.finishDrawerSwipeToDrawer(this, homeScreen, drawerScreen,
                            () -> drawerOpen = true, getDrawerTransition(), drawerSwipeProgress);
                } else {
                    LauncherScreenTransitions.cancelDrawerSwipe(this, homeScreen, drawerScreen,
                            getDrawerTransition());
                }
                return true;
            }
            boolean itemManipulationOnRelease = gestureStartedOnClock || gestureStartedOnResizeHandle
                    || iconDragArmed || iconDragging
                    || ((gestureStartedOnAppCell || gestureStartedOnWidget
                        || gestureStartedOnContactShortcut)
                        && event.getEventTime() - event.getDownTime() > 450L);
            if (!drawerOpen && !itemManipulationOnRelease
                    && (verticalSwipeClaimed
                        || (Math.abs(dy) > LauncherUiUtils.dp(this, 65)
                            && Math.abs(dy) > Math.abs(dx) * 1.15f))) {
                if (dy < 0f) {
                    // Finish from the current pose, but do not force a tiny partial
                    // swipe all the way open. Always settle to home or drawer.
                    if (drawerSwipeProgress >= 0.10f) {
                        LauncherScreenTransitions.finishDrawerSwipeToDrawer(this, homeScreen, drawerScreen,
                                () -> drawerOpen = true, getDrawerTransition(), drawerSwipeProgress);
                    } else {
                        LauncherScreenTransitions.cancelDrawerSwipe(this, homeScreen, drawerScreen,
                                getDrawerTransition());
                    }
                    drawerSwipeVisualActive = false;
                } else {
                    openControlCenter();
                }
                return true;
            }
            if (pageSwipeActive) {
                pageSwipeActive = false;
                int pageWidth = pageViewport != null && pageViewport.getWidth() > 0
                        ? pageViewport.getWidth()
                        : Math.max(1, getResources().getDisplayMetrics().widthPixels
                                - LauncherUiUtils.dp(this, 40));
                int destination = currentPage;
                // Require more than 30% of the page width at release. Velocity does
                // not override this threshold, so a quick short flick cannot change pages.
                if (wheelSwipeProgress > 0.10f) {
                    int candidate = currentPage + wheelSwipeDirection;
                    if (candidate >= 0 && candidate < pagesStrip.getChildCount()) {
                        destination = candidate;
                    }
                }
                finishPageSwipe(destination, pageWidth);
                return true;
            }

            // Vertical gestures are claimed in ACTION_MOVE above.
        }
        return super.dispatchTouchEvent(event);
    }

    private boolean touchStartedOnDrawerGrid(float rawX, float rawY) {
        if (drawerGrid == null) return false;
        View target = findViewAt(root, (int) rawX, (int) rawY);
        while (target != null) {
            if (target == drawerGrid) return true;
            if (target == root) break;
            android.view.ViewParent parent = target.getParent();
            target = parent instanceof View ? (View) parent : null;
        }
        return false;
    }

    private boolean touchStartedOnClock(float rawX, float rawY) {
        // The clock is visually scaled and can overlap icons/widgets. findViewAt()
        // only checks untransformed layout bounds and ignores elevation, so it could
        // misclassify a touch on the visible clock as a workspace gesture.
        if (clockPanel != null && clockPanel.getVisibility() == View.VISIBLE
                && clockPanel.getWidth() > 0 && clockPanel.getHeight() > 0) {
            int[] location = new int[2];
            clockPanel.getLocationOnScreen(location);
            float visualWidth = clockPanel.getWidth() * Math.max(0.01f, clockPanel.getScaleX());
            float visualHeight = clockPanel.getHeight() * Math.max(0.01f, clockPanel.getScaleY());
            if (rawX >= location[0] && rawX < location[0] + visualWidth
                    && rawY >= location[1] && rawY < location[1] + visualHeight) return true;
        }
        for (View grip : clockEdgeHandles) {
            if (grip == null || grip.getVisibility() != View.VISIBLE) continue;
            int[] location = new int[2];
            grip.getLocationOnScreen(location);
            if (rawX >= location[0] && rawX < location[0] + grip.getWidth()
                    && rawY >= location[1] && rawY < location[1] + grip.getHeight()) return true;
        }
        View target = findViewAt(root, (int) rawX, (int) rawY);
        while (target != null) {
            if (target == clockPanel || "launcher_clock_widget".equals(target.getTag())) return true;
            for (View grip : clockEdgeHandles) if (target == grip) return true;
            if (target == root) break;
            android.view.ViewParent parent = target.getParent();
            target = parent instanceof View ? (View) parent : null;
        }
        return false;
    }

    /**
     * Finalize one clock resize transaction. A normal release applies the last
     * edge-anchor correction after layout; cancellation restores the complete
     * starting geometry and invalidates any queued frame from the abandoned drag.
     */
    private void finishClockResize(boolean cancelled) {
        if (clockResizeEdge == 0 || clockPanel == null) return;
        ++clockResizeGeneration; // Invalidate delayed edge corrections from MOVE frames.
        clockResizeEdge = 0;
        clockWidgetDragging = false;
        handler.removeCallbacks(clockWidgetLongPress);

        if (cancelled) {
            prefs.edit()
                    .putInt(clockPrefKey(KEY_CLOCK_LENGTH, activeClockPage), clockWidgetStartLength)
                    .putInt(clockPrefKey(KEY_CLOCK_BREADTH, activeClockPage), clockWidgetStartBreadth)
                    .putFloat(clockPrefKey(KEY_CLOCK_WIDGET_X, activeClockPage), clockWidgetStartX)
                    .putFloat(clockPrefKey(KEY_CLOCK_WIDGET_Y, activeClockPage), clockWidgetStartY)
                    .apply();
            applyClockPositionAndSize();
            clockPanel.setTranslationX(clockWidgetStartX);
            clockPanel.setTranslationY(clockWidgetStartY);
        } else {
            // Commit the position as it appears at finger-up. Do not schedule another
            // post-layout edge correction: it could run after the outside tap and make
            // the placed clock jump a few pixels.
            prefs.edit()
                    .putFloat(clockPrefKey(KEY_CLOCK_WIDGET_X, activeClockPage), clockPanel.getTranslationX())
                    .putFloat(clockPrefKey(KEY_CLOCK_WIDGET_Y, activeClockPage), clockPanel.getTranslationY())
                    .apply();
        }
        // Re-clamp after the final breadth/length layout has been measured. During
        // resizing the panel's measured dimensions can lag one frame behind the
        // new layout params, so the live-drag clamp alone is not sufficient.
        final int finalGeometryGeneration = clockResizeGeneration;
        setClampedClockTranslation(clockPanel.getTranslationX(), clockPanel.getTranslationY(), true);
        clockPanel.post(() -> {
            if (clockPanel == null || clockResizeGeneration != finalGeometryGeneration
                    || clockResizeEdge != 0) return;
            setClampedClockTranslation(clockPanel.getTranslationX(), clockPanel.getTranslationY(), true);
            updateClockResizeHandles();
        });
        setWorkspaceGridVisible(false, null);
        updateClockResizeHandles();
    }

    private void updateClockResizeHandles() {
        if (clockPanel == null || root == null || clockPanel.getWidth() <= 0 || clockPanel.getHeight() <= 0) return;
        int[] panel = new int[2];
        int[] rootLoc = new int[2];
        clockPanel.getLocationOnScreen(panel);
        root.getLocationOnScreen(rootLoc);
        // Resize grips follow the transformed visual rectangle, not the
        // fixed layout footprint reserved to keep home items from reflowing.
        int w = Math.max(1, Math.round(clockPanel.getWidth() * clockPanel.getScaleX()));
        int h = Math.max(1, Math.round(clockPanel.getHeight() * clockPanel.getScaleY()));
        int side = LauncherUiUtils.dp(this, 28);
        int thin = LauncherUiUtils.dp(this, 8);
        int[][] positions = new int[][] {
                {panel[0] - rootLoc[0], panel[1] - rootLoc[1] + h / 2 - side / 2},
                {panel[0] - rootLoc[0] + w - thin, panel[1] - rootLoc[1] + h / 2 - side / 2},
                {panel[0] - rootLoc[0] + w / 2 - side / 2, panel[1] - rootLoc[1]},
                {panel[0] - rootLoc[0] + w / 2 - side / 2, panel[1] - rootLoc[1] + h - thin}
        };
        for (int i = 0; i < clockEdgeHandles.length; i++) {
            View grip = clockEdgeHandles[i];
            if (grip == null) continue;
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) grip.getLayoutParams();
            lp.width = i < 2 ? thin : side;
            lp.height = i < 2 ? side : thin;
            lp.leftMargin = positions[i][0];
            lp.topMargin = positions[i][1];
            grip.setLayoutParams(lp);
            grip.bringToFront();
            grip.setVisibility(clockWidgetEditing ? View.VISIBLE : View.GONE);
        }
    }

    private int hitClockResizeEdge(float rawX, float rawY) {
        if (clockPanel == null || clockPanel.getWidth() <= 0 || clockPanel.getHeight() <= 0) return 0;
        // Hit-test the actual visible grips, with only a small accessibility margin.
        // This prevents corner touches from being misclassified as another edge.
        float margin = LauncherUiUtils.dp(this, 6);
        // At corners, prioritize top/bottom grips so a vertical resize can never
        // accidentally be interpreted as a horizontal resize.
        for (int i = 2; i <= 3; i++) {
            View grip = clockEdgeHandles[i];
            if (grip == null || grip.getVisibility() != View.VISIBLE) continue;
            int[] loc = new int[2];
            grip.getLocationOnScreen(loc);
            if (rawX >= loc[0] - margin && rawX <= loc[0] + grip.getWidth() + margin
                    && rawY >= loc[1] - margin && rawY <= loc[1] + grip.getHeight() + margin) {
                return i + 1;
            }
        }
        for (int i = 0; i <= 1; i++) {
            View grip = clockEdgeHandles[i];
            if (grip == null || grip.getVisibility() != View.VISIBLE) continue;
            int[] loc = new int[2];
            grip.getLocationOnScreen(loc);
            if (rawX >= loc[0] - margin && rawX <= loc[0] + grip.getWidth() + margin
                    && rawY >= loc[1] - margin && rawY <= loc[1] + grip.getHeight() + margin) {
                return i + 1;
            }
        }
        return 0;
    }

    private boolean touchStartedOnResizeHandle(float rawX, float rawY) {
        View target = findViewAt(root, (int) rawX, (int) rawY);
        while (target != null) {
            Object tag = target.getTag();
            if ("launcher_widget_resize_handle".equals(tag)
                    || "launcher_shortcut_resize_handle".equals(tag)
                    || "launcher_clock_resize_handle".equals(tag)) return true;
            if (target == root) break;
            android.view.ViewParent parent = target.getParent();
            target = parent instanceof View ? (View) parent : null;
        }
        return false;
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

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        // Android sends the HOME intent back to this singleTask launcher. Always
        // return to the first home page and dismiss the drawer on that action.
        if (root != null) {
            // A HOME intent is sent when the user presses the system Home button
            // from any foreground app. Always normalize the launcher to its home
            // surface rather than preserving a half-open drawer/transition state.
            boolean homeIntent = intent != null
                    && Intent.ACTION_MAIN.equals(intent.getAction())
                    && intent.hasCategory(Intent.CATEGORY_HOME);
            if (homeIntent || drawerOpen) showHome();
            if (currentPage != 0) showPage(0, true);
        }
    }

    @Override protected void onResume() {
        super.onResume();
        clockUpdatesActive = true;
        handler.removeCallbacks(clockUpdater);
        handler.post(clockUpdater);
        // The app list is loaded at startup and refreshed only by package add/remove broadcasts.
        try { widgetHost.startListening(); } catch (Exception ignored) {}
    }

    @Override protected void onPause() {
        // Leaving the launcher (opening an app or locking the screen) must never
        // leave clock widget edit grips/grid active when the launcher returns.
        if (clockWidgetEditing) exitClockWidgetEditMode();
        clockWidgetDragging = false;
        handler.removeCallbacks(clockWidgetLongPress);
        clockUpdatesActive = false;
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
                KEY_ICON_PAGE, KEY_WIDGET_IDS, KEY_ICON_X, KEY_ICON_Y, getHomeColumns(), getHomeGridRows());
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
                    @Override public void launchApp(ResolveInfo app, View sourceView) { MainActivity.this.launchApp(app, sourceView); }
                    @Override public void showAppOptions(ResolveInfo app, boolean onHome) { MainActivity.this.showAppOptions(app, onHome); }
                }, KEY_ICON_X, KEY_ICON_Y, KEY_SHOW_HOME_LABELS, KEY_ICON_SIZE, KEY_HOME_FONT_SIZE, KEY_SHOW_PAGE_INDICATOR, HOME_ROWS);
        renderHomePages(false);
        showHome();
    }

    private void setWorkspaceGridVisible(boolean visible, View movingView) {
        if (pagesStrip == null) return;
        int iconSize = prefs == null ? 48 : prefs.getInt(KEY_ICON_SIZE, 48);
        int cellWidth = Math.max(1, pageViewport != null && pageViewport.getWidth() > 0
                ? pageViewport.getWidth() / Math.max(1, getHomeColumns())
                : LauncherUiUtils.dp(this, 90));
        int cellHeight = LauncherUiUtils.dp(this, homeCellHeightDp(iconSize));
        for (int i = 0; i < Math.min(2, pagesStrip.getChildCount()); i++) {
            if (!(pagesStrip.getChildAt(i) instanceof FrameLayout)) continue;
            FrameLayout page = (FrameLayout) pagesStrip.getChildAt(i);
            if (workspaceGridOverlays[i] == null || workspaceGridOverlays[i].getParent() != page) {
                WorkspaceGridOverlay overlay = new WorkspaceGridOverlay(page);
                overlay.setClickable(false);
                overlay.setFocusable(false);
                overlay.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
                overlay.setVisibility(View.GONE);
                page.addView(overlay, new FrameLayout.LayoutParams(-1, -1));
                workspaceGridOverlays[i] = overlay;
            }
            WorkspaceGridOverlay overlay = (WorkspaceGridOverlay) workspaceGridOverlays[i];
            overlay.configure(cellWidth, cellHeight);
            overlay.setVisibility(visible ? View.VISIBLE : View.GONE);
            overlay.bringToFront();
            if (movingView != null && movingView.getParent() == page) movingView.bringToFront();
        }
    }

    private void refreshWorkspaceGrid() {
        for (View overlay : workspaceGridOverlays) {
            if (overlay != null && overlay.getVisibility() == View.VISIBLE) overlay.invalidate();
        }
    }

    private final class WorkspaceGridOverlay extends View {
        private final FrameLayout page;
        private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private int cellW = 1, cellH = 1;
        WorkspaceGridOverlay(FrameLayout page) {
            super(MainActivity.this);
            this.page = page;
            // Use a quiet filled placement marker, not a full grid of heavy lines.
            gridPaint.setStyle(Paint.Style.FILL);
            gridPaint.setColor(Color.argb(34, 120, 165, 255));
        }
        void configure(int cw, int ch) {
            cellW = Math.max(1, cw);
            cellH = Math.max(1, ch);
            invalidate();
        }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (page == null) return;
            for (int i = 0; i < page.getChildCount(); i++) {
                View item = page.getChildAt(i);
                if (item == this || item.getVisibility() != View.VISIBLE) continue;
                Object rawTag = item.getTag();
                if (!(rawTag instanceof String)) continue;
                String tag = (String) rawTag;
                if (!tag.startsWith("launcher_app:") && !tag.startsWith("launcher_widget:")
                        && !tag.equals("launcher_contact_shortcut")) continue;
                ViewGroup.LayoutParams rawParams = item.getLayoutParams();
                int left, top, right, bottom;
                if (rawParams instanceof FrameLayout.LayoutParams) {
                    FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) rawParams;
                    left = lp.leftMargin;
                    top = lp.topMargin;
                    if (tag.startsWith("launcher_app:")) {
                        left = Math.max(0, Math.round(left / (float) cellW) * cellW);
                        top = Math.max(0, Math.round(top / (float) cellH) * cellH);
                        right = Math.min(getWidth(), left + cellW);
                        bottom = Math.min(getHeight(), top + cellH);
                    } else {
                        right = left + Math.max(item.getWidth(), lp.width > 0 ? lp.width : 0);
                        bottom = top + Math.max(item.getHeight(), lp.height > 0 ? lp.height : 0);
                    }
                } else {
                    left = item.getLeft();
                    top = item.getTop();
                    right = item.getRight();
                    bottom = item.getBottom();
                }
                if (right > left && bottom > top) {
                    canvas.drawRoundRect(left + 2, top + 2, right - 2, bottom - 2,
                            LauncherUiUtils.dp(MainActivity.this, 6),
                            LauncherUiUtils.dp(MainActivity.this, 6), gridPaint);
                }
            }
        }
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
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, -2);
        hp.gravity = Gravity.TOP;
        hp.topMargin = LauncherUiUtils.dp(this, 2);
        homeScreen.addView(hero, hp);
        clockPanel = hero;
        // Keep the existing clock as a launcher-local widget: it can be moved and
        // resized in place without introducing AppWidgetHost overhead on Android 6.
        installClockWidgetInteraction();
        applyClockPanelColor(false);
        clockRow = new FrameLayout(this);
        clockRow.setClipChildren(false);
        clockRow.setClipToPadding(false);
        clockGroup = new FrameLayout(this);
        clockGroup.setClipChildren(false);
        clockGroup.setClipToPadding(false);
        clockText = new SevenSegmentClockTextView(this);
        clockText.setText("--:--");
        clockText.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.TOP);
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
        amPmText.setTextColor(clockText.getCurrentTextColor());
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
        dateText.setGravity(Gravity.CENTER);
        dateText.setLetterSpacing(0.04f);
        dateText.setShadowLayer(LauncherUiUtils.dp(this, 2), 0f, LauncherUiUtils.dp(this, 1),
                Color.argb(220, 0, 0, 0));
        LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, -2);
        // The clock is an overlay widget: resizing must not reflow the home grid.
        // Keep the date at a stable offset; the clock glyph may extend over items below.
        dp.topMargin = LauncherUiUtils.dp(this, 1);
        hero.addView(dateText, dp);
        // Detach before assigning FrameLayout.LayoutParams to this page overlay.
        homeScreen.removeView(hero);
        applyClockPositionAndSize();
        applyClockPanelColor(false);
        clockPanelsByPage[0] = clockPanel;
        clockTextsByPage[0] = clockText;
        amPmTextsByPage[0] = amPmText;
        dateTextsByPage[0] = dateText;
        clockRowsByPage[0] = clockRow;
        clockGroupsByPage[0] = clockGroup;
        buildSecondPageClock();
        activateClockForPage(0);

        pageViewport = new FrameLayout(this);
        // Permit the rotating faces to extend slightly beyond the viewport while turning.
        // The viewport clips all workspace content to the visible screen.
        pageViewport.setClipChildren(true);
        pageViewport.setClipToPadding(true);
        pageViewport.setCameraDistance(12000f * getResources().getDisplayMetrics().density);
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(-1, 0, 1f);
        pp.topMargin = LauncherUiUtils.dp(this, 2);
        homeScreen.addView(pageViewport, pp);

        // The viewport stays fixed while the page strip moves inside it.
        pagesStrip = new LinearLayout(this);
        pagesStrip.setOrientation(LinearLayout.HORIZONTAL);
        pagesStrip.setClipChildren(true);
        pagesStrip.setClipToPadding(true);
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
        // Dock icons sit at the bottom edge of the workspace; don't let the
        // page viewport or the dock's own bounds clip their upper/lower pixels.
        dockContainer.setClipChildren(false);
        dockContainer.setClipToPadding(false);
        dockContainer.setElevation(LauncherUiUtils.dp(this, 4));
        int initialDockIconSize = prefs.getInt(KEY_ICON_SIZE, 48);
        LinearLayout.LayoutParams dockParams = new LinearLayout.LayoutParams(-1,
                LauncherUiUtils.dp(this, Math.max(50, initialDockIconSize + 2)));
        dockParams.topMargin = LauncherUiUtils.dp(this, 2);
        dockParams.bottomMargin = LauncherUiUtils.dp(this, 2);
        homeScreen.addView(dockContainer, dockParams);
        restoreDockApps();
    }

    /** Home cell geometry must include the label's measured height when labels are enabled. */
    private int homeCellHeightDp(int iconSize) {
        boolean labelsVisible = prefs.getBoolean(KEY_SHOW_HOME_LABELS, true);
        return Math.max(56, iconSize + (labelsVisible ? 46 : 28));
    }

    private String clockPrefKey(String key, int page) {
        return page == 0 ? key : key + "_page_2";
    }

    private void activateClockForPage(int requestedPage) {
        int page = Math.max(0, Math.min(1, requestedPage));
        if (clockPanelsByPage[page] == null) return;
        activeClockPage = page;
        clockPanel = clockPanelsByPage[page];
        clockText = clockTextsByPage[page];
        amPmText = amPmTextsByPage[page];
        dateText = dateTextsByPage[page];
        clockRow = clockRowsByPage[page];
        clockGroup = clockGroupsByPage[page];
    }

    private void buildSecondPageClock() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setGravity(Gravity.RIGHT);
        panel.setPadding(LauncherUiUtils.dp(this, 2), LauncherUiUtils.dp(this, 2),
                LauncherUiUtils.dp(this, 2), LauncherUiUtils.dp(this, 2));
        panel.setClipChildren(false);
        panel.setClipToPadding(false);
        panel.setTag("launcher_clock_widget");
        panel.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.LEFT));

        FrameLayout row = new FrameLayout(this);
        row.setClipChildren(false);
        row.setClipToPadding(false);
        FrameLayout group = new FrameLayout(this);
        group.setClipChildren(false);
        group.setClipToPadding(false);
        TextView digits = new SevenSegmentClockTextView(this);
        digits.setText("--:--");
        digits.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.TOP);
        digits.setTextSize(78);
        digits.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, LauncherUiUtils.dp(this, 114),
                Gravity.LEFT | Gravity.CENTER_VERTICAL));
        digits.setTextColor(Color.rgb(196, 200, 209));
        digits.setIncludeFontPadding(false);
        digits.setTypeface(Typeface.create("monospace", Typeface.BOLD));
        digits.setLetterSpacing(0f);
        group.addView(digits);

        TextView marker = new TextView(this);
        marker.setTextColor(digits.getCurrentTextColor());
        marker.setTextSize(8);
        marker.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        marker.setGravity(Gravity.RIGHT | Gravity.BOTTOM);
        marker.setIncludeFontPadding(false);
        marker.setPadding(0, 0, 0, 0);
        group.addView(marker, new FrameLayout.LayoutParams(
                LauncherUiUtils.dp(this, 18), LauncherUiUtils.dp(this, 12),
                Gravity.RIGHT | Gravity.BOTTOM));
        row.addView(group, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, LauncherUiUtils.dp(this, 114),
                Gravity.RIGHT | Gravity.CENTER_VERTICAL));
        panel.addView(row, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, LauncherUiUtils.dp(this, 114)));

        TextView date = LauncherUiUtils.text(this, "", 13, Color.WHITE, true);
        date.setTypeface(Typeface.create("sans-serif-condensed-medium", Typeface.BOLD));
        date.setGravity(Gravity.CENTER);
        date.setLetterSpacing(0.04f);
        date.setShadowLayer(LauncherUiUtils.dp(this, 2), 0f, LauncherUiUtils.dp(this, 1),
                Color.argb(220, 0, 0, 0));
        LinearLayout.LayoutParams dateParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dateParams.topMargin = LauncherUiUtils.dp(this, 1);
        panel.addView(date, dateParams);

        clockPanelsByPage[1] = panel;
        clockTextsByPage[1] = digits;
        amPmTextsByPage[1] = marker;
        dateTextsByPage[1] = date;
        clockRowsByPage[1] = row;
        clockGroupsByPage[1] = group;
        activateClockForPage(1);
        applyClockStyle();
        applyClockPositionAndSize();
        applyClockPanelColor(false);
        installClockWidgetInteraction();
        activateClockForPage(0);
    }

    // SharedPreferences.getFloat() throws ClassCastException when an older build
    // stored the same setting as an Integer. Accept any numeric representation so
    // legacy clock positions cannot crash a page rebuild or launcher startup.
    private float getFloatPreferenceCompat(String key, float fallback) {
        Object value = prefs.getAll().get(key);
        return value instanceof Number ? ((Number) value).floatValue() : fallback;
    }

    private void attachClockPanelsToPages() {
        if (pagesStrip == null) return;
        for (int pageIndex = 0; pageIndex < clockPanelsByPage.length; pageIndex++) {
            if (pageIndex >= pagesStrip.getChildCount()
                    || !(pagesStrip.getChildAt(pageIndex) instanceof FrameLayout)) continue;
            LinearLayout panel = clockPanelsByPage[pageIndex];
            if (panel == null) continue;
            FrameLayout page = (FrameLayout) pagesStrip.getChildAt(pageIndex);
            // Also enforce clipping on pages that already existed before this rebuild.
            page.setClipChildren(true);
            page.setClipToPadding(true);
            if (panel.getParent() != page) {
                if (panel.getParent() instanceof ViewGroup) {
                    ((ViewGroup) panel.getParent()).removeView(panel);
                }
                page.addView(panel, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                        Gravity.TOP | Gravity.LEFT));
            }
            float fallbackX = pageIndex == 0 ? getFloatPreferenceCompat(KEY_CLOCK_WIDGET_X, 0f) : 0f;
            float fallbackY = pageIndex == 0 ? getFloatPreferenceCompat(KEY_CLOCK_WIDGET_Y, 0f) : 0f;
            panel.setTranslationX(getFloatPreferenceCompat(
                    clockPrefKey(KEY_CLOCK_WIDGET_X, pageIndex), fallbackX));
            panel.setTranslationY(getFloatPreferenceCompat(
                    clockPrefKey(KEY_CLOCK_WIDGET_Y, pageIndex), fallbackY));
            // Keep the clock above icons/widgets even after the page content is rebuilt.
            panel.bringToFront();
        }
        activateClockForPage(currentPage);
        applyClockPositionAndSize();
        // Page rebuilds are used by icon-shape, icon-background, size, font and
        // label settings. Refresh both clock backgrounds after layout without moving them.
        for (int pageIndex = 0; pageIndex < clockPanelsByPage.length; pageIndex++) {
            final int targetPage = pageIndex;
            final LinearLayout targetPanel = clockPanelsByPage[pageIndex];
            if (targetPanel == null) continue;
            targetPanel.post(() -> {
                if (targetPanel.getParent() == null || clockPanelsByPage[targetPage] != targetPanel) return;
                int previousPage = Math.max(0, Math.min(1, activeClockPage));
                activateClockForPage(targetPage);
                applyClockPanelColor(false);
                activateClockForPage(previousPage);
            });
        }
    }

    private void buildDrawer() {
        LauncherDrawerController.BuiltDrawer built = LauncherDrawerController.build(
                this, root, prefs, packageManager, allApps, drawerApps, drawerBackground(),
                prefs.getInt(KEY_DRAWER_SPACING, 8), KEY_DRAWER_ICON_SIZE, KEY_DRAWER_FONT_SIZE,
                KEY_SHOW_DRAWER_LABELS, new LauncherDrawerController.Actions() {
                    @Override public void launchApp(ResolveInfo app, View sourceView) { MainActivity.this.launchApp(app, sourceView); }
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
        attachClockPanelsToPages();
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
        int rows = getHomeGridRows();
        int cellW = Math.max(1, pageViewport.getWidth() / columns);
        int cellH = LauncherUiUtils.dp(this,
                homeCellHeightDp(prefs.getInt(KEY_ICON_SIZE, 48)));
        if (pageViewport.getWidth() <= 0 || pageViewport.getHeight() <= 0) return;

        FrameLayout chosenPage = null;
        int chosenLeft = 0, chosenTop = 0;
        int savedWidthDp = prefs.getInt(KEY_SHORTCUT_WIDTH_DP, 0);
        int savedHeightDp = prefs.getInt(KEY_SHORTCUT_HEIGHT_DP, 0);
        // Old span-only values could have been written by the broken resize logic.
        // Start compact unless a real pixel-size resize has been saved by this version.
        int shortcutSpanX = savedWidthDp > 0
                ? Math.max(1, Math.min(columns, prefs.getInt(KEY_SHORTCUT_SPAN_X, 1))) : 1;
        int shortcutSpanY = savedHeightDp > 0
                ? Math.max(1, Math.min(rows, prefs.getInt(KEY_SHORTCUT_SPAN_Y, 1))) : 1;
        // Check the complete resized shortcut footprint, not only its first grid cell.
        // Default contact shortcuts use a compact visual footprint, not a full
        // invisible grid cell. Explicitly resized shortcuts retain their saved size.
        int compactShortcutSize = LauncherUiUtils.dp(this, 48);
        // Old builds could persist an oversized footprint even before the user
        // deliberately resized it. Normalize those legacy values on restore.
        if (savedWidthDp > 96 || savedHeightDp > 96) {
            prefs.edit().remove(KEY_SHORTCUT_WIDTH_DP).remove(KEY_SHORTCUT_HEIGHT_DP)
                    .putInt(KEY_SHORTCUT_SPAN_X, 1).putInt(KEY_SHORTCUT_SPAN_Y, 1).apply();
            savedWidthDp = 0;
            savedHeightDp = 0;
        }
        // A direct-call shortcut is an app-like icon, not a miniature widget tile.
        // Its default footprint is the same compact size as the app icon cell.
        // Match the shortcut's default footprint to one normal home-app grid cell.
        int defaultShortcutWidth = cellW;
        int defaultShortcutHeight = cellH;
        int desiredTileWidth = Math.max(1, Math.min(pageViewport.getWidth(),
                savedWidthDp > 0 ? LauncherUiUtils.dp(this, savedWidthDp)
                        : Math.min(cellW, defaultShortcutWidth)));
        int desiredTileHeight = Math.max(1, Math.min(pageViewport.getHeight(),
                savedHeightDp > 0 ? LauncherUiUtils.dp(this, savedHeightDp)
                        : Math.min(cellH, defaultShortcutHeight)));
        // Restore a user-dragged position first, provided it is still visible and collision-free.
        float savedX = prefs.getFloat(KEY_SHORTCUT_X, -1f);
        float savedY = prefs.getFloat(KEY_SHORTCUT_Y, -1f);
        if (savedX >= 0f && savedY >= 0f && desiredPage < pagesStrip.getChildCount()) {
            View savedPageView = pagesStrip.getChildAt(desiredPage);
            if (savedPageView instanceof FrameLayout) {
                FrameLayout savedPage = (FrameLayout) savedPageView;
                int maxLeft = Math.max(0, pageViewport.getWidth() - desiredTileWidth);
                int maxTop = Math.max(0, pageViewport.getHeight() - desiredTileHeight);
                int savedLeft = Math.round(Math.min(1f, savedX) * maxLeft);
                int savedTop = Math.round(Math.min(1f, savedY) * maxTop);
                if (isContactShortcutPositionFree(savedPage, null, savedLeft, savedTop,
                        desiredTileWidth, desiredTileHeight)) {
                    chosenPage = savedPage;
                    chosenLeft = savedLeft;
                    chosenTop = savedTop;
                }
            }
        }
        // The app-slot finder accounts for app cells separately from widget bounds.
        // Use it before geometric fallback so the shortcut can occupy a genuinely free cell.
        for (int attempt = 0; attempt < pagesStrip.getChildCount() && chosenPage == null; attempt++) {
            int pageIndex = (desiredPage + attempt) % pagesStrip.getChildCount();
            View pageView = pagesStrip.getChildAt(pageIndex);
            if (!(pageView instanceof FrameLayout)) continue;
            int[] freeSlot = findFreeHomeAppSlot(pageIndex);
            if (freeSlot == null) continue;
            int left = freeSlot[0] * cellW + Math.max(0, (cellW - desiredTileWidth) / 2);
            int top = freeSlot[1] * cellH + Math.max(0, (cellH - desiredTileHeight) / 2);
            if (left + desiredTileWidth <= pageViewport.getWidth()
                    && top + desiredTileHeight <= pageViewport.getHeight()) {
                chosenPage = (FrameLayout) pageView;
                chosenLeft = left;
                chosenTop = top;
            }
        }
        for (int attempt = 0; attempt < pagesStrip.getChildCount() && chosenPage == null; attempt++) {
            int pageIndex = (desiredPage + attempt) % pagesStrip.getChildCount();
            View pageView = pagesStrip.getChildAt(pageIndex);
            if (!(pageView instanceof FrameLayout)) continue;
            FrameLayout page = (FrameLayout) pageView;
            for (int row = 0; row < rows && chosenPage == null; row++) {
                for (int col = 0; col < columns && chosenPage == null; col++) {
                    int candidateLeft = col * cellW + Math.max(0, (cellW - desiredTileWidth) / 2);
                    int candidateTop = row * cellH + Math.max(0, (cellH - desiredTileHeight) / 2);
                    if (candidateLeft + desiredTileWidth > pageViewport.getWidth()
                            || candidateTop + desiredTileHeight > pageViewport.getHeight()) continue;
                    android.graphics.Rect candidate = new android.graphics.Rect(candidateLeft, candidateTop,
                            candidateLeft + desiredTileWidth, candidateTop + desiredTileHeight);
                    boolean occupied = false;
                    for (int childIndex = 0; childIndex < page.getChildCount(); childIndex++) {
                        View child = page.getChildAt(childIndex);
                        if ("launcher_contact_shortcut".equals(child.getTag())
                                || "launcher_clock_widget".equals(child.getTag())) continue;
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
                        chosenLeft = candidateLeft;
                        chosenTop = candidateTop;
                    }
                }
            }
        }
        // Never report success while silently dropping the shortcut. If every
        // full-size tile collides, retry with a compact one-cell tile on either page.
        if (chosenPage == null) {
            desiredTileWidth = Math.max(1, cellW);
            desiredTileHeight = Math.max(1, Math.min(cellH, pageViewport.getHeight()));
            for (int attempt = 0; attempt < pagesStrip.getChildCount() && chosenPage == null; attempt++) {
                int pageIndex = (desiredPage + attempt) % pagesStrip.getChildCount();
                View pageView = pagesStrip.getChildAt(pageIndex);
                if (!(pageView instanceof FrameLayout)) continue;
                FrameLayout page = (FrameLayout) pageView;
                for (int row = 0; row < rows && chosenPage == null; row++) {
                    for (int col = 0; col < columns && chosenPage == null; col++) {
                        int left = col * cellW;
                        int top = row * cellH;
                        if (left + desiredTileWidth > pageViewport.getWidth()
                                || top + desiredTileHeight > pageViewport.getHeight()) continue;
                        android.graphics.Rect candidate = new android.graphics.Rect(left, top,
                                left + desiredTileWidth, top + desiredTileHeight);
                        boolean occupied = false;
                        for (int childIndex = 0; childIndex < page.getChildCount(); childIndex++) {
                            View child = page.getChildAt(childIndex);
                            if ("launcher_contact_shortcut".equals(child.getTag())) continue;
                            FrameLayout.LayoutParams p = child.getLayoutParams() instanceof FrameLayout.LayoutParams
                                    ? (FrameLayout.LayoutParams) child.getLayoutParams() : null;
                            int childLeft = p != null ? p.leftMargin : child.getLeft();
                            int childTop = p != null ? p.topMargin : child.getTop();
                            int childWidth = p != null && p.width > 0 ? p.width : child.getWidth();
                            int childHeight = p != null && p.height > 0 ? p.height : child.getHeight();
                            if (android.graphics.Rect.intersects(candidate, new android.graphics.Rect(
                                    childLeft, childTop, childLeft + Math.max(0, childWidth),
                                    childTop + Math.max(0, childHeight)))) { occupied = true; break; }
                        }
                        if (!occupied) {
                            chosenPage = page;
                            chosenLeft = left;
                            chosenTop = top;
                        }
                    }
                }
            }
        }
        if (chosenPage == null) {
            // The caller that creates a shortcut reports failure. Rendering during a
            // normal refresh must not emit a contradictory toast or claim success.
            return;
        }

        int actualShortcutPage = pagesStrip.indexOfChild(chosenPage);
        if (actualShortcutPage >= 0 && actualShortcutPage != desiredPage) {
            desiredPage = actualShortcutPage;
        }
        float normalizedX = chosenLeft / (float) Math.max(1, pageViewport.getWidth() - desiredTileWidth);
        float normalizedY = chosenTop / (float) Math.max(1, pageViewport.getHeight() - desiredTileHeight);
        prefs.edit().putInt(KEY_SHORTCUT_PAGE, desiredPage)
                .putFloat(KEY_SHORTCUT_X, normalizedX).putFloat(KEY_SHORTCUT_Y, normalizedY).apply();
        FrameLayout tile = new FrameLayout(this);
        tile.setTag("launcher_contact_shortcut");
        tile.setClipChildren(false);
        chosenPage.setClipChildren(false);
        LinearLayout tileContent = new LinearLayout(this);
        tileContent.setOrientation(LinearLayout.VERTICAL);
        tileContent.setGravity(Gravity.CENTER);
        tileContent.setPadding(0, 0, 0, 0);
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
        int initialIconSize = Math.max(1, Math.min(LauncherUiUtils.dp(this, 48), Math.min(desiredTileWidth, desiredTileHeight)));
        tileContent.addView(icon, new LinearLayout.LayoutParams(initialIconSize, initialIconSize));
        tileContent.setGravity(Gravity.CENTER);
        tileContent.setOrientation(LinearLayout.VERTICAL);
        TextView label = LauncherUiUtils.text(this,
                prefs.getString(KEY_SHORTCUT_LABEL, "Contact shortcut"), 12, Color.WHITE, true);
        label.setGravity(Gravity.CENTER);
        label.setMaxLines(2);
        label.setEllipsize(android.text.TextUtils.TruncateAt.END);
        // The movable/collision tile is exactly the icon bounds. Keep the label
        // visually below it without enlarging the shortcut tile itself.
        FrameLayout.LayoutParams labelParams = new FrameLayout.LayoutParams(
                Math.max(1, desiredTileWidth + LauncherUiUtils.dp(this, 24)),
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        labelParams.topMargin = desiredTileHeight + LauncherUiUtils.dp(this, 3);
        tile.addView(label, labelParams);
        int tileWidth = Math.min(pageViewport.getWidth() - chosenLeft, desiredTileWidth);
        int tileHeight = Math.min(pageViewport.getHeight() - chosenTop, desiredTileHeight);
        // Keep the real layout bounds equal to the resized tile; do not inflate it to a grid cell.
        FrameLayout.LayoutParams tileParams = new FrameLayout.LayoutParams(
                Math.max(1, tileWidth), Math.max(1, tileHeight), Gravity.TOP | Gravity.LEFT);
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
        resizeGrip.setTag("launcher_shortcut_resize_handle");
        FrameLayout.LayoutParams gripParams = new FrameLayout.LayoutParams(
                LauncherUiUtils.dp(this, 36), LauncherUiUtils.dp(this, 36), Gravity.RIGHT | Gravity.BOTTOM);
        tile.addView(resizeGrip, gripParams);
        resizeGrip.bringToFront();
        final boolean[] resizingShortcut = {false};
        final float[] resizeStart = {0f, 0f};
        final int[] resizeBase = {shortcutSpanX, shortcutSpanY};
        final int[] resizeBasePx = {tileWidth, tileHeight};
        final FrameLayout shortcutResizePage = chosenPage;
        resizeGrip.setOnTouchListener((view, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    resizingShortcut[0] = true;
                    setWorkspaceGridVisible(true, tile);
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
                        int previousWidth = p.width;
                        int previousHeight = p.height;
                        p.width = Math.max(cellW, Math.min(pageViewport.getWidth() - p.leftMargin,
                                resizeBasePx[0] + Math.round(event.getRawX() - resizeStart[0])));
                        p.height = Math.max(cellH, Math.min(pageViewport.getHeight() - p.topMargin,
                                resizeBasePx[1] + Math.round(event.getRawY() - resizeStart[1])));
                        // A shortcut may not be resized through an app, widget, or another
                        // placed item. Reject that frame instead of creating a hidden overlap.
                        boolean overlapsPlacedItem = false;
                        int tileRight = p.leftMargin + p.width;
                        int tileBottom = p.topMargin + p.height;
                        for (int i = 0; i < shortcutResizePage.getChildCount(); i++) {
                            View other = shortcutResizePage.getChildAt(i);
                            if (other == tile || !(other.getTag() instanceof String)) continue;
                            String otherTag = (String) other.getTag();
                            if (!otherTag.startsWith("launcher_app:")
                                    && !otherTag.startsWith("launcher_widget:")
                                    && !"launcher_contact_shortcut".equals(otherTag)
                                    && !"launcher_clock_widget".equals(otherTag)) continue;
                            FrameLayout.LayoutParams otherParams =
                                    (FrameLayout.LayoutParams) other.getLayoutParams();
                            int otherWidth = other.getWidth() > 0 ? other.getWidth() : otherParams.width;
                            int otherHeight = other.getHeight() > 0 ? other.getHeight() : otherParams.height;
                            if (p.leftMargin < otherParams.leftMargin + otherWidth
                                    && tileRight > otherParams.leftMargin
                                    && p.topMargin < otherParams.topMargin + otherHeight
                                    && tileBottom > otherParams.topMargin) {
                                overlapsPlacedItem = true;
                                break;
                            }
                        }
                        if (overlapsPlacedItem) {
                            p.width = previousWidth;
                            p.height = previousHeight;
                        }
                        tile.setLayoutParams(p);
                        // Keep the label below the resized icon tile instead of letting it
                        // remain at the original height and overlap the icon.
                        ViewGroup.LayoutParams rawLabel = label.getLayoutParams();
                        if (rawLabel instanceof FrameLayout.LayoutParams) {
                            FrameLayout.LayoutParams labelLp = (FrameLayout.LayoutParams) rawLabel;
                            labelLp.topMargin = p.height + LauncherUiUtils.dp(this, 3);
                            label.setLayoutParams(labelLp);
                        }
                        refreshWorkspaceGrid();
                        // Grow the actual contact/app icon as the tile grows; changing only
                        // the tile bounds leaves the icon visually stuck at its original size.
                        // Keep the initial one-cell shortcut compact. Scale from the original
                        // 28dp icon by the tile's grid-cell dimensions, so dragging smaller
                        // restores the original icon instead of leaving it enlarged.
                        float widthScale = (float) p.width / (float) cellW;
                        float heightScale = (float) p.height / (float) cellH;
                        float tileScale = Math.min(widthScale, heightScale);
                        int configuredIconDp = Math.max(24, prefs.getInt(KEY_ICON_SIZE, 48));
                        int iconSize = Math.max(LauncherUiUtils.dp(this, 24),
                                Math.min(LauncherUiUtils.dp(this, 120),
                                        Math.round(LauncherUiUtils.dp(this, configuredIconDp) * tileScale)));
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
                        setWorkspaceGridVisible(false, null);
                        // Keep the resized tile in place; rebuilding it here can move it
                        // to another free cell and makes the resize appear to snap back.
                    }
                    return true;
            }
            return true;
        });
        // Long-press arms dragging; a stationary long-press still opens the options menu.
        // This custom touch handler prevents the system long-click from stealing a drag.
        final float[] shortcutTouchStart = {0f, 0f};
        final int[] shortcutPositionStart = {chosenLeft, chosenTop};
        final boolean[] shortcutDragArmed = {false};
        final boolean[] shortcutDragging = {false};
        final boolean[] shortcutPopupShown = {false};
        final Runnable[] shortcutArmRunnable = {null};
        final Runnable[] shortcutPopupRunnable = {null};
        tile.setOnTouchListener((view, event) -> {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                shortcutTouchStart[0] = event.getRawX();
                shortcutTouchStart[1] = event.getRawY();
                shortcutPositionStart[0] = ((FrameLayout.LayoutParams) tile.getLayoutParams()).leftMargin;
                shortcutPositionStart[1] = ((FrameLayout.LayoutParams) tile.getLayoutParams()).topMargin;
                shortcutDragArmed[0] = false;
                shortcutDragging[0] = false;
                shortcutPopupShown[0] = false;
                handler.removeCallbacks(shortcutArmRunnable[0]);
                handler.removeCallbacks(shortcutPopupRunnable[0]);
                shortcutArmRunnable[0] = () -> {
                    if (!shortcutDragging[0] && tile.isAttachedToWindow()) {
                        shortcutDragArmed[0] = true;
                        LauncherUiUtils.startMoveHint(tile, this);
                    }
                };
                shortcutPopupRunnable[0] = () -> {
                    if (!shortcutDragging[0] && tile.isAttachedToWindow()) {
                        LauncherUiUtils.stopMoveHint(tile);
                        shortcutPopupShown[0] = true;
                        tile.performLongClick();
                    }
                };
                handler.postDelayed(shortcutArmRunnable[0], 1000L);
                handler.postDelayed(shortcutPopupRunnable[0], 2000L);
                return true;
            }
            if (action == MotionEvent.ACTION_MOVE) {
                float dx = event.getRawX() - shortcutTouchStart[0];
                float dy = event.getRawY() - shortcutTouchStart[1];
                boolean moved = Math.abs(dx) > LauncherUiUtils.dp(this, 8)
                        || Math.abs(dy) > LauncherUiUtils.dp(this, 8);
                if (moved && shortcutDragArmed[0] && !shortcutPopupShown[0]) {
                    handler.removeCallbacks(shortcutPopupRunnable[0]);
                    if (!shortcutDragging[0]) {
                        LauncherUiUtils.stopMoveHint(tile);
                        shortcutDragging[0] = true;
                        setWorkspaceGridVisible(true, tile);
                        tile.bringToFront();
                    }
                    FrameLayout.LayoutParams p = (FrameLayout.LayoutParams) tile.getLayoutParams();
                    int[] pageLocation = new int[2];
                    shortcutResizePage.getLocationOnScreen(pageLocation);
                    p.leftMargin = Math.max(0, Math.min(pageViewport.getWidth() - tile.getWidth(),
                            Math.round(event.getRawX() - pageLocation[0] - tile.getWidth() / 2f)));
                    p.topMargin = Math.max(0, Math.min(pageViewport.getHeight() - tile.getHeight(),
                            Math.round(event.getRawY() - pageLocation[1] - tile.getHeight() / 2f)));
                    tile.setLayoutParams(p);
                    updateLiveDisplacements(shortcutResizePage, tile);
                    refreshWorkspaceGrid();
                }
                return true;
            }
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                LauncherUiUtils.stopMoveHint(tile);
                handler.removeCallbacks(shortcutArmRunnable[0]);
                handler.removeCallbacks(shortcutPopupRunnable[0]);
                if (shortcutDragging[0]) {
                    FrameLayout.LayoutParams p = (FrameLayout.LayoutParams) tile.getLayoutParams();
                    if (action == MotionEvent.ACTION_CANCEL) {
                        finishLiveDisplacements(false, tile);
                        p.leftMargin = shortcutPositionStart[0];
                        p.topMargin = shortcutPositionStart[1];
                        tile.setLayoutParams(p);
                    } else {
                        int dragColumns = getHomeColumns();
                        int dragCellW = Math.max(1, pageViewport.getWidth() / dragColumns);
                        int dragCellH = LauncherUiUtils.dp(this, homeCellHeightDp(prefs.getInt(KEY_ICON_SIZE, 48)));
                        int proposedLeft = Math.max(0, Math.min(pageViewport.getWidth() - tile.getWidth(),
                                Math.round((p.leftMargin + tile.getWidth() / 2f - dragCellW / 2f) / dragCellW) * dragCellW));
                        int proposedTop = Math.max(0, Math.min(pageViewport.getHeight() - tile.getHeight(),
                                Math.round((p.topMargin + tile.getHeight() / 2f - dragCellH / 2f) / dragCellH) * dragCellH));
                        p.leftMargin = proposedLeft;
                        p.topMargin = proposedTop;
                        tile.setLayoutParams(p);
                        updateLiveDisplacements(shortcutResizePage, tile);
                        finishLiveDisplacements(true, tile);
                        prefs.edit().putInt(KEY_SHORTCUT_PAGE, pagesStrip.indexOfChild(shortcutResizePage))
                                .putFloat(KEY_SHORTCUT_X, proposedLeft / (float) Math.max(1,
                                        pageViewport.getWidth() - tile.getWidth()))
                                .putFloat(KEY_SHORTCUT_Y, proposedTop / (float) Math.max(1,
                                        pageViewport.getHeight() - tile.getHeight())).apply();
                    }
                    setWorkspaceGridVisible(false, null);
                    shortcutDragging[0] = false;
                } else if (!shortcutPopupShown[0] && action == MotionEvent.ACTION_UP) {
                    tile.performClick();
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
                            .remove(KEY_SHORTCUT_WIDTH_DP).remove(KEY_SHORTCUT_HEIGHT_DP)
                            .remove(KEY_SHORTCUT_X).remove(KEY_SHORTCUT_Y).apply();
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

    private boolean isContactShortcutPositionFree(FrameLayout page, View ignore, int left, int top,
            int width, int height) {
        if (page == null || width <= 0 || height <= 0 || pageViewport == null) return false;
        int columns = getHomeColumns();
        int cellW = Math.max(1, pageViewport.getWidth() / columns);
        int cellH = LauncherUiUtils.dp(this, homeCellHeightDp(prefs.getInt(KEY_ICON_SIZE, 48)));
        int rows = Math.max(1, pageViewport.getHeight() / cellH);
        // Test the snapped one-cell target, exactly as the app icon placer does.
        int col = Math.max(0, Math.min(columns - 1,
                Math.round((left + width / 2f - cellW / 2f) / cellW)));
        int row = Math.max(0, Math.min(rows - 1,
                Math.round((top + height / 2f - cellH / 2f) / cellH)));
        int snappedLeft = col * cellW, snappedTop = row * cellH;
        android.graphics.Rect candidate = new android.graphics.Rect(snappedLeft, snappedTop,
                snappedLeft + cellW, snappedTop + cellH);
        if (candidate.right > pageViewport.getWidth() || candidate.bottom > pageViewport.getHeight()) return false;
        for (int i = 0; i < page.getChildCount(); i++) {
            View child = page.getChildAt(i);
            if (child == ignore || "launcher_clock_widget".equals(child.getTag())
                    || child == workspaceGridOverlays[0] || child == workspaceGridOverlays[1]) continue;
            Object rawTag = child.getTag();
            String tag = rawTag instanceof String ? (String) rawTag : "";
            if (tag.startsWith("launcher_app:")) {
                FrameLayout.LayoutParams appLp = child.getLayoutParams() instanceof FrameLayout.LayoutParams
                        ? (FrameLayout.LayoutParams) child.getLayoutParams() : null;
                int appLeft = appLp != null ? appLp.leftMargin : child.getLeft();
                int appTop = appLp != null ? appLp.topMargin : child.getTop();
                int appCol = Math.max(0, Math.min(columns - 1, Math.round(appLeft / (float) cellW)));
                int appRow = Math.max(0, Math.min(rows - 1, Math.round(appTop / (float) cellH)));
                if (appCol == col && appRow == row) return false;
                continue;
            }
            if (tag.equals("launcher_contact_shortcut")) continue;
            FrameLayout.LayoutParams p = child.getLayoutParams() instanceof FrameLayout.LayoutParams
                    ? (FrameLayout.LayoutParams) child.getLayoutParams() : null;
            int childLeft = p != null ? p.leftMargin : child.getLeft();
            int childTop = p != null ? p.topMargin : child.getTop();
            int childWidth = p != null && p.width > 0 ? p.width : child.getWidth();
            int childHeight = p != null && p.height > 0 ? p.height : child.getHeight();
            if (childWidth > 0 && childHeight > 0 && android.graphics.Rect.intersects(candidate,
                    new android.graphics.Rect(childLeft, childTop, childLeft + childWidth, childTop + childHeight))) return false;
        }
        return true;
    }

    private boolean hasRenderedContactShortcut() {
        if (pagesStrip == null) return false;
        for (int i = 0; i < pagesStrip.getChildCount(); i++) {
            View page = pagesStrip.getChildAt(i);
            if (!(page instanceof ViewGroup)) continue;
            ViewGroup group = (ViewGroup) page;
            for (int j = 0; j < group.getChildCount(); j++) {
                if ("launcher_contact_shortcut".equals(group.getChildAt(j).getTag())) return true;
            }
        }
        return false;
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
        int desiredHeight = LauncherUiUtils.dp(this, Math.max(50, iconSize + 2));
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
            slotView.setClipChildren(false);
            slotView.setClipToPadding(false);
            LinearLayout.LayoutParams slotParams = new LinearLayout.LayoutParams(0, -1, 1f);
            int packageIndex = slot == 0 ? 0 : 1;
            if (packageIndex < dockPackages.size()) {
                final String packageName = dockPackages.get(packageIndex);
                ResolveInfo app = findApp(packageName);
                if (app != null) {
                    ImageView icon = new ImageView(this);
                    android.graphics.drawable.Drawable dockIcon = app.loadIcon(packageManager);
                    icon.setImageDrawable(dockIcon == null ? null : new LauncherIconDrawable(
                            dockIcon, prefs.getBoolean("icon_rounded_square_background", false),
                            prefs.getInt("app_icon_shape", LauncherIconDrawable.SHAPE_ORIGINAL),
                            LauncherIconDrawable.backgroundColorForChoice(
                                    prefs.getInt(KEY_ICON_BACKGROUND_COLOR, LauncherIconDrawable.BACKGROUND_BLACK))));
                    icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    FrameLayout.LayoutParams iconParams = new FrameLayout.LayoutParams(
                            LauncherUiUtils.dp(this, iconSize), LauncherUiUtils.dp(this, iconSize), Gravity.CENTER);
                    slotView.addView(icon, iconParams);
                    slotView.setContentDescription(String.valueOf(app.loadLabel(packageManager)));
                    slotView.setOnClickListener(v -> launchApp(app, slotView));
                    slotView.setOnLongClickListener(v -> {
                        showActionDialog("Dock app", new String[]{"−", "↗"},
                                new String[]{"Remove from dock", "Move to home screen"}, (dialog, which) -> {
                            if (which == 1) {
                                // Move through the same placement/space checks as any other
                                // home shortcut. Remove from the dock first so addToHome does
                                // not reject the intentional transfer as a duplicate.
                                if (homePackages.contains(packageName)) {
                                    dockPackages.remove(packageName);
                                    persistDockApps();
                                    renderHomePages(false);
                                    return;
                                }
                                dockPackages.remove(packageName);
                                persistDockApps();
                                if (!addToHome(packageName)) {
                                    dockPackages.add(packageName);
                                    persistDockApps();
                                    renderHomePages(false);
                                }
                            } else {
                                dockPackages.remove(packageName);
                                persistDockApps();
                                renderHomePages(false);
                            }
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
            if (!iconDragging && !longPressTriggered && app != null) launchApp(app, cell);
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
                    if (activeDragView == view) {
                        iconDragArmed = true;
                        LauncherUiUtils.startMoveHint(view, this);
                    }
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
                    LauncherUiUtils.stopMoveHint(view);
                    iconDragging = true;
                    if (iconShowOptions != null) handler.removeCallbacks(iconShowOptions);
                    setWorkspaceGridVisible(true, view);
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
                    int slotHeight = LauncherUiUtils.dp(this,
                            homeCellHeightDp(prefs.getInt(KEY_ICON_SIZE, 48)));
                    int gridRows = Math.max(1, page.getHeight() / slotHeight);
                    int requestedTop = Math.round(event.getRawY() - pageLocation[1] - view.getHeight() / 2f);
                    int snappedRow = Math.max(0, Math.min(gridRows - 1,
                            Math.round(requestedTop / (float) Math.max(1, slotHeight))));
                    // Keep drag snapping identical to the fixed row positions used by the renderer.
                    lp.topMargin = snappedRow * slotHeight;
                    view.setLayoutParams(lp);
                    refreshWorkspaceGrid();
                }
                return true;
            }
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            LauncherUiUtils.stopMoveHint(view);
            // Finish resize gestures here before they can fall through to launcher
            // gesture handling. Without this reset, the resize flag stayed armed
            // after finger-up and later swipes could keep changing clock dimensions.
            if (clockResizeEdge != 0) {
                handler.removeCallbacks(clockWidgetLongPress);
                clockResizeEdge = 0;
                clockWidgetDragging = false;
                setWorkspaceGridVisible(false, null);
                updateClockResizeHandles();
                return true;
            }
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
                            setWorkspaceGridVisible(false, null);
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
                    int columns = getHomeColumns();
                    int slotHeight = LauncherUiUtils.dp(this,
                            homeCellHeightDp(prefs.getInt(KEY_ICON_SIZE, 48)));
                    // Match HomePageController's fixed row spacing so the icon does not
                    // jump vertically when the drag is committed and the page is rebuilt.
                    int targetHeightForGrid = Math.max(1, targetPage.getHeight() > 0
                            ? targetPage.getHeight() : pageViewport.getHeight());
                    int rows = Math.max(1, targetHeightForGrid / slotHeight);
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
                    int cellHeight = slotHeight;
                    rows = Math.max(1, Math.min(rows, targetHeight / cellHeight));
                    float desiredLeft = event.getRawX() - viewportLocation[0] - cellWidth / 2f;
                    int targetCol = Math.max(0, Math.min(columns - 1,
                            Math.round(desiredLeft / cellWidth)));
                    float desiredTop = event.getRawY() - viewportLocation[1] - cellHeight / 2f;
                    int targetRow = Math.max(0, Math.min(rows - 1,
                            Math.round(desiredTop / (float) Math.max(1, cellHeight))));
                    int sourceCellWidth = Math.max(1, sourceView.getWidth() / columns);
                    int sourceCellHeight = slotHeight;
                    int sourceRows = Math.max(1, sourceView.getHeight() / sourceCellHeight);
                    int sourceCol = Math.max(0, Math.min(columns - 1,
                            Math.round(iconOriginalLeft / (float) sourceCellWidth)));
                    int sourceRow = Math.max(0, Math.min(sourceRows - 1,
                            Math.round(iconOriginalTop / (float) Math.max(1, sourceCellHeight))));
                    boolean[][] occupied = new boolean[rows][columns];
                    View occupant = null;
                    String otherPackage = null;
                    for (int i = 0; i < targetPage.getChildCount(); i++) {
                        View candidate = targetPage.getChildAt(i);
                        if (candidate == view || !(candidate.getTag() instanceof String)
                                || !((String) candidate.getTag()).startsWith("launcher_app:")) continue;
                        FrameLayout.LayoutParams cp = (FrameLayout.LayoutParams) candidate.getLayoutParams();
                        int cc = Math.max(0, Math.min(columns - 1, Math.round((float) cp.leftMargin / cellWidth)));
                        int cr = Math.max(0, Math.min(rows - 1,
                                Math.round(cp.topMargin / (float) Math.max(1, cellHeight))));
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
                    setWorkspaceGridVisible(false, null);
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
                    if (app != null) launchApp(app, view);
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
                // Each home page is an independent drawing surface. A clock or icon
                // may be oversized internally but cannot render across the page seam.
                page.setClipChildren(true);
                page.setClipToPadding(true);
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
        int targetPage = Math.max(0, Math.min(lastPage, requestedPage));
        if (clockWidgetEditing && targetPage != currentPage) {
            exitClockWidgetEditMode();
        }
        currentPage = targetPage;
        activateClockForPage(currentPage);
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
        // Pre-create GPU layers for the two faces most likely to move next.
        // Doing this while the page is idle avoids a hitch on the first swipe frame.
        preparePageLayersForSwipe(currentPage);
        if (animate) {
            pagesStrip.animate().translationX(targetX)
                    .setDuration(animationDuration(240L))
                    .setInterpolator(new android.view.animation.DecelerateInterpolator())
                    .withEndAction(this::releasePageLayers)
                    .start();
        } else {
            pagesStrip.setTranslationX(targetX);
            releasePageLayers();
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

        View incomingFace = to >= 0 && to < pagesStrip.getChildCount() ? pagesStrip.getChildAt(to) : null;
        // Keep the two page views in their real neighbouring positions. Translating the
        // incoming face over the outgoing one caused the next page to pop into view.
        // Only the parallax style moves the strip; 3D styles rotate the adjacent faces.
        // The wheel must combine face rotation with continuous horizontal travel.
        // Rotating two stationary neighbouring faces alone looks like a panel flip,
        // not a wheel carrying the next page into the viewport.
        pagesStrip.setTranslationX(-from * width
                - ((effect == 0 || effect == 2) ? direction * width * progress : 0f));
        // Cross-fade the pages without changing any transition geometry.
        outgoing.setAlpha(1f - progress);
        if (incomingFace != null) incomingFace.setAlpha(progress);
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
            float cubeAngle = getCubeRotationDegrees();
            outgoing.setRotationY(direction * cubeAngle * progress);
            outgoing.setTranslationZ(-density * 28f * progress);
            outgoing.setTranslationX(-direction * width * 0.04f * progress);
            if (to >= 0 && to < pagesStrip.getChildCount()) {
                View incoming = pagesStrip.getChildAt(to);
                incoming.setCameraDistance(10000f * density);
                incoming.setPivotX(direction > 0 ? 0f : incoming.getWidth());
                incoming.setPivotY(incoming.getHeight() * 0.5f);
                incoming.setRotationY(-direction * cubeAngle * (1f - progress));
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
            outgoing.setPivotX(direction > 0 ? width : 0f);
            outgoing.setPivotY(height * 0.5f);
            outgoing.setRotationY(direction * angle * progress);
            outgoing.setScaleX(1f - 0.035f * progress);
            outgoing.setScaleY(1f - 0.025f * progress);
            if (to >= 0 && to < pagesStrip.getChildCount()) {
                View incoming = pagesStrip.getChildAt(to);
                incoming.setCameraDistance(10000f * density);
                incoming.setPivotX(direction > 0 ? 0f : incoming.getWidth());
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
            private boolean cancelled;

            @Override public void onAnimationCancel(android.animation.Animator animation) {
                // A second touch, page rebuild, or lifecycle interruption can cancel
                // the animator. Never leave a cube face rotated or the strip between pages.
                cancelled = true;
            }

            @Override public void onAnimationEnd(android.animation.Animator animation) {
                int settledPage = cancelled ? from : to;
                currentPage = settledPage;
                activateClockForPage(currentPage);
                pagesStrip.setTranslationX(-settledPage * (float) width);
                resetPageTransforms();
                releasePageLayers();
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

    /**
     * Keep hardware display lists warm for only the visible page and its neighbour.
     * The active gesture then changes transforms on cached surfaces instead of
     * paying the layer-creation cost during the first frames of a swipe.
     */
    private void releasePageLayers() {
        if (pagesStrip == null) return;
        for (int i = 0; i < pagesStrip.getChildCount(); i++) {
            pagesStrip.getChildAt(i).setLayerType(View.LAYER_TYPE_NONE, null);
        }
    }

    private void preparePageLayersForSwipe(int pageIndex) {
        if (pagesStrip == null) return;
        int count = pagesStrip.getChildCount();
        int active = Math.max(0, Math.min(count - 1, pageIndex));
        int neighbour = active + 1 < count ? active + 1 : active - 1;
        for (int i = 0; i < count; i++) {
            View page = pagesStrip.getChildAt(i);
            if (i == active || i == neighbour) {
                page.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                page.setCameraDistance(10000f * getResources().getDisplayMetrics().density);
            } else {
                page.setLayerType(View.LAYER_TYPE_NONE, null);
            }
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

    private void openControlCenter() {
        if (NotificationShadeAccessibilityService.openQuickSettings()) return;
        Toast.makeText(this,
                "Enable PhoneBridge Launcher in Accessibility settings to open Quick Settings by swipe",
                Toast.LENGTH_LONG).show();
        try {
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        } catch (Exception ignored) {
            // The gesture remains safe if this device does not expose that settings page.
        }
    }

    private void showHome() {
        LauncherScreenTransitions.showHome(this, homeScreen, drawerScreen, () -> drawerOpen = false,
                getDrawerTransition());
    }

    private void showDrawer() { showDrawer(0f); }

    private void showDrawer(float swipeProgress) {
        LauncherScreenTransitions.showDrawer(this, homeScreen, drawerScreen, () -> {
            drawerOpen = true;
        }, getDrawerTransition(), Math.max(0f, Math.min(1f, swipeProgress)));
    }

    private boolean addToHome(String packageName) { return addToHome(packageName, currentPage); }

    private boolean addToHome(String packageName, int requestedPage) {
        if (packageName == null || packageName.trim().isEmpty()
                || packageName.equals(getPackageName()) || homePackages.contains(packageName)) {
            return false;
        }
        if (dockPackages.contains(packageName)) {
            Toast.makeText(this, "This app is already in the dock", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (findApp(packageName) == null) {
            Toast.makeText(this, "That app is no longer available", Toast.LENGTH_SHORT).show();
            return false;
        }
        int requested = Math.max(0, Math.min(1, requestedPage));
        int targetPage = -1;
        int[] freeSlot = null;
        // Prefer the requested screen, but use the other screen when the first has
        // no free cell. Never save an app that the renderer cannot place visibly.
        for (int attempt = 0; attempt < 2; attempt++) {
            int candidatePage = (requested + attempt) % 2;
            int[] candidateSlot = findFreeHomeAppSlot(candidatePage);
            if (candidateSlot != null) {
                targetPage = candidatePage;
                freeSlot = candidateSlot;
                break;
            }
        }
        if (targetPage < 0 || freeSlot == null) {
            Toast.makeText(this, "Both home screens are full. Remove or move an item first.",
                    Toast.LENGTH_LONG).show();
            return false;
        }

        int columns = getHomeColumns();
        int rows = getHomeGridRows();
        homePackages.add(packageName);
        prefs.edit()
                .putInt(KEY_ICON_PAGE + packageName, targetPage)
                .putFloat(KEY_ICON_X + packageName,
                        freeSlot[0] / (float) Math.max(1, columns - 1))
                .putFloat(KEY_ICON_Y + packageName,
                        freeSlot[1] / (float) Math.max(1, rows - 1))
                .apply();
        persistHomeApps();
        rebuildPages();
        currentPage = targetPage;
        ensurePageExists(targetPage);
        renderHomePages(false);
        showPage(targetPage, false);
        Toast.makeText(this, "Added to Home", Toast.LENGTH_SHORT).show();
        return true;
    }

    /**
     * Returns the first free grid cell on a page. App icons reserve one whole cell;
     * widgets and the custom shortcut reserve their real visible rectangle. Clock
     * panels are intentionally ignored because they are overlays, not grid occupants.
     */
    private int[] findFreeHomeAppSlot(int pageIndex) {
        int columns = getHomeColumns();
        int width = pageViewport != null && pageViewport.getWidth() > 0
                ? pageViewport.getWidth()
                : Math.max(1, getResources().getDisplayMetrics().widthPixels
                        - LauncherUiUtils.dp(this, 40));
        int height = pageViewport != null && pageViewport.getHeight() > 0
                ? pageViewport.getHeight()
                : Math.max(LauncherUiUtils.dp(this, 360),
                        getResources().getDisplayMetrics().heightPixels
                                - LauncherUiUtils.dp(this, 260));
        int cellWidth = Math.max(1, width / columns);
        int cellHeight = LauncherUiUtils.dp(this,
                homeCellHeightDp(prefs.getInt(KEY_ICON_SIZE, 48)));
        int rows = Math.max(1, height / cellHeight);
        boolean[][] occupied = new boolean[rows][columns];

        FrameLayout page = pagesStrip != null && pageIndex >= 0
                && pageIndex < pagesStrip.getChildCount()
                && pagesStrip.getChildAt(pageIndex) instanceof FrameLayout
                ? (FrameLayout) pagesStrip.getChildAt(pageIndex) : null;

        // Read existing app cells from the actual page when available. Fall back to
        // persisted coordinates if that page has not been laid out yet.
        boolean sawAppView = false;
        if (page != null) {
            for (int i = 0; i < page.getChildCount(); i++) {
                View child = page.getChildAt(i);
                Object rawTag = child.getTag();
                if (!(rawTag instanceof String)
                        || !((String) rawTag).startsWith("launcher_app:")) continue;
                sawAppView = true;
                ViewGroup.LayoutParams rawParams = child.getLayoutParams();
                int left = child.getLeft(), top = child.getTop();
                if (rawParams instanceof FrameLayout.LayoutParams) {
                    FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) rawParams;
                    left = lp.leftMargin;
                    top = lp.topMargin;
                }
                int col = Math.max(0, Math.min(columns - 1,
                        Math.round(left / (float) cellWidth)));
                int row = Math.max(0, Math.min(rows - 1,
                        Math.round(top / (float) cellHeight)));
                occupied[row][col] = true;
            }
        }
        if (!sawAppView) {
            for (String existing : homePackages) {
                if (prefs.getInt(KEY_ICON_PAGE + existing, 0) != pageIndex) continue;
                float sx = prefs.getFloat(KEY_ICON_X + existing, 0f);
                float sy = prefs.getFloat(KEY_ICON_Y + existing, 0f);
                int col = Math.max(0, Math.min(columns - 1,
                        Math.round(sx * Math.max(1, columns - 1))));
                int row = Math.max(0, Math.min(rows - 1,
                        Math.round(sy * Math.max(1, rows - 1))));
                occupied[row][col] = true;
            }
        }

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                if (occupied[row][col]) continue;
                android.graphics.Rect cell = new android.graphics.Rect(
                        col * cellWidth, row * cellHeight,
                        Math.min(width, (col + 1) * cellWidth),
                        Math.min(height, (row + 1) * cellHeight));
                boolean blocked = false;
                if (page != null) {
                    for (int i = 0; i < page.getChildCount(); i++) {
                        View child = page.getChildAt(i);
                        Object rawTag = child.getTag();
                        if (!(rawTag instanceof String)) continue;
                        String tag = (String) rawTag;
                        if ("launcher_clock_widget".equals(tag)
                                || tag.startsWith("launcher_app:")
                                || !tag.startsWith("launcher_widget:")
                                    && !"launcher_contact_shortcut".equals(tag)) continue;
                        ViewGroup.LayoutParams rawParams = child.getLayoutParams();
                        int left = child.getLeft(), top = child.getTop();
                        int childWidth = child.getWidth(), childHeight = child.getHeight();
                        if (rawParams instanceof FrameLayout.LayoutParams) {
                            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) rawParams;
                            left = lp.leftMargin;
                            top = lp.topMargin;
                            if (lp.width > 0) childWidth = lp.width;
                            if (lp.height > 0) childHeight = lp.height;
                        }
                        left += Math.round(child.getTranslationX());
                        top += Math.round(child.getTranslationY());
                        android.graphics.Rect bounds = new android.graphics.Rect(left, top,
                                left + Math.max(0, childWidth), top + Math.max(0, childHeight));
                        if (android.graphics.Rect.intersects(cell, bounds)) {
                            blocked = true;
                            break;
                        }
                    }
                }
                if (!blocked) return new int[]{col, row};
            }
        }
        return null;
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
            if (app == null || app.activityInfo == null) continue;
            String packageName = app.activityInfo.packageName;
            if (!packageName.equals(getPackageName()) && !homePackages.contains(packageName)
                    && !dockPackages.contains(packageName)) available.add(app);
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
                handler.post(() -> {
                    renderSavedContactShortcut();
                    if (!hasRenderedContactShortcut()) {
                        prefs.edit().remove(KEY_SHORTCUT_INTENT).remove(KEY_SHORTCUT_LABEL)
                                .remove(KEY_SHORTCUT_PAGE).remove(KEY_SHORTCUT_ICON)
                                .remove(KEY_SHORTCUT_X).remove(KEY_SHORTCUT_Y).apply();
                        Toast.makeText(this, "No free space for the shortcut. Move or remove an item first.",
                                Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Contact shortcut added", Toast.LENGTH_SHORT).show();
                    }
                });
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
            int resolvedPage = targetPage;
            int availableWidth = page.getWidth() > 0 ? page.getWidth()
                    : Math.max(LauncherUiUtils.dp(this, 180), getResources().getDisplayMetrics().widthPixels - LauncherUiUtils.dp(this, 40));
            int availableHeight = page.getHeight() > 0 ? page.getHeight()
                    : Math.max(LauncherUiUtils.dp(this, 240), getResources().getDisplayMetrics().heightPixels - LauncherUiUtils.dp(this, 260));
            int minWidth = LauncherUiUtils.dp(this, Math.max(0, info.minWidth));
            int minHeight = LauncherUiUtils.dp(this, Math.max(0, info.minHeight));
            // Start closer to the provider's requested dimensions instead of forcing
            // every widget into a large 180x100dp footprint that hides usable space.
            int width = Math.min(Math.max(LauncherUiUtils.dp(this, 96), minWidth),
                    Math.max(LauncherUiUtils.dp(this, 96), availableWidth - LauncherUiUtils.dp(this, 16)));
            int height = Math.min(Math.max(LauncherUiUtils.dp(this, 72), minHeight),
                    Math.max(LauncherUiUtils.dp(this, 72), availableHeight - LauncherUiUtils.dp(this, 80)));
            AppWidgetHostView widget = widgetHost.createView(this, widgetId, info);
            widget.setAppWidget(widgetId, info);
            widget.setPadding(0, 0, 0, 0);
            int[] freeWidgetPosition = findFreeWidgetPosition(page, width, height);
            if (freeWidgetPosition == null && pagesStrip != null) {
                // Check the other home screen at the original size before shrinking.
                int otherPageIndex = targetPage == 0 ? 1 : 0;
                if (otherPageIndex < pagesStrip.getChildCount()
                        && pagesStrip.getChildAt(otherPageIndex) instanceof FrameLayout) {
                    FrameLayout otherPage = (FrameLayout) pagesStrip.getChildAt(otherPageIndex);
                    int[] otherPosition = findFreeWidgetPosition(otherPage, width, height);
                    if (otherPosition != null) {
                        page = otherPage;
                        resolvedPage = otherPageIndex;
                        freeWidgetPosition = otherPosition;
                    }
                }
            }
            // If the requested footprint cannot fit on either page, retry at 50%
            // size. Keep the widget's aspect ratio and its original size whenever possible.
            if (freeWidgetPosition == null) {
                int smallerWidth = Math.max(LauncherUiUtils.dp(this, 48), Math.round(width * 0.5f));
                int smallerHeight = Math.max(LauncherUiUtils.dp(this, 48), Math.round(height * 0.5f));
                freeWidgetPosition = findFreeWidgetPosition(page, smallerWidth, smallerHeight);
                if (freeWidgetPosition == null && pagesStrip != null) {
                    int otherPageIndex = targetPage == 0 ? 1 : 0;
                    if (otherPageIndex < pagesStrip.getChildCount()
                            && pagesStrip.getChildAt(otherPageIndex) instanceof FrameLayout) {
                        FrameLayout otherPage = (FrameLayout) pagesStrip.getChildAt(otherPageIndex);
                        int[] otherPosition = findFreeWidgetPosition(otherPage, smallerWidth, smallerHeight);
                        if (otherPosition != null) {
                            page = otherPage;
                            resolvedPage = otherPageIndex;
                            freeWidgetPosition = otherPosition;
                        }
                    }
                }
                if (freeWidgetPosition != null) {
                    width = smallerWidth;
                    height = smallerHeight;
                    Toast.makeText(this, "Widget added at 50% size to fit the available space", Toast.LENGTH_SHORT).show();
                }
            }
            if (freeWidgetPosition == null) {
                try { widgetHost.deleteAppWidgetId(widgetId); } catch (Exception ignored) {}
                Toast.makeText(this, "No free space for this widget, even at 50% size", Toast.LENGTH_LONG).show();
                return;
            }
            addWidgetContainer(page, widget, widgetId, resolvedPage, freeWidgetPosition[0], freeWidgetPosition[1], width, height);
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
                saveWidgetGeometry(widgetId, resolvedPage, container);
                final FrameLayout placedWidget = container;
                final FrameLayout placedPage = page;
                page.post(() -> resolveWidgetIconCollisions(placedPage, placedWidget));
            } else {
                String stored = prefs.getString(KEY_WIDGET_IDS, "");
                String entry = widgetId + ":" + resolvedPage;
                if (!containsWidgetId(stored, widgetId)) {
                    prefs.edit().putString(KEY_WIDGET_IDS, stored.isEmpty() ? entry : stored + "\n" + entry).apply();
                }
            }
            widgetHost.startListening();
        });
    }

    private int[] findFreeWidgetPosition(FrameLayout page, int widgetWidth, int widgetHeight) {
        if (page == null) return null;
        int pageWidth = page.getWidth() > 0 ? page.getWidth()
                : Math.max(1, getResources().getDisplayMetrics().widthPixels - LauncherUiUtils.dp(this, 40));
        int pageHeight = page.getHeight() > 0 ? page.getHeight()
                : Math.max(LauncherUiUtils.dp(this, 240), getResources().getDisplayMetrics().heightPixels - LauncherUiUtils.dp(this, 260));
        int gap = Math.max(1, LauncherUiUtils.dp(this, 5));
        // Keep the full widget inside the workspace with a minimum 5dp inset.
        int minLeft = gap;
        int minTop = gap;
        int maxLeft = pageWidth - widgetWidth - gap;
        int maxTop = pageHeight - widgetHeight - gap;

        // The dock is a separate view on some layouts. Treat its top edge as a
        // hard workspace boundary and retain the same 5dp clearance above it.
        if (dockContainer != null && dockContainer.getVisibility() == View.VISIBLE
                && dockContainer.getHeight() > 0 && page.getWidth() > 0 && page.getHeight() > 0) {
            int[] pageLocation = new int[2];
            int[] dockLocation = new int[2];
            page.getLocationOnScreen(pageLocation);
            dockContainer.getLocationOnScreen(dockLocation);
            int dockTopInPage = dockLocation[1] - pageLocation[1];
            if (dockTopInPage >= 0 && dockTopInPage <= pageHeight + gap) {
                maxTop = Math.min(maxTop, dockTopInPage - widgetHeight - gap);
            }
        }
        if (maxLeft < minLeft || maxTop < minTop) return null;

        // Widgets use continuous free positioning, not app-icon grid cells.
        // Search in small increments and require at least 5dp clearance from
        // the screen/workspace edges and every existing workspace item.
        int step = Math.max(1, LauncherUiUtils.dp(this, 4));
        for (int top = minTop; top <= maxTop; top += step) {
            for (int left = minLeft; left <= maxLeft; left += step) {
                android.graphics.Rect candidate = new android.graphics.Rect(
                        left - gap, top - gap, left + widgetWidth + gap, top + widgetHeight + gap);
                boolean blocked = false;
                for (int i = 0; i < page.getChildCount(); i++) {
                    View child = page.getChildAt(i);
                    Object rawTag = child.getTag();
                    if (!(rawTag instanceof String)) continue;
                    String tag = (String) rawTag;
                    if (!(tag.startsWith("launcher_widget:")
                            || tag.startsWith("launcher_app:")
                            || "launcher_contact_shortcut".equals(tag)
                            || "launcher_clock_widget".equals(tag))) continue;
                    ViewGroup.LayoutParams raw = child.getLayoutParams();
                    int x = child.getLeft(), y = child.getTop();
                    int w = child.getWidth(), h = child.getHeight();
                    if (raw instanceof FrameLayout.LayoutParams) {
                        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) raw;
                        x = lp.leftMargin;
                        y = lp.topMargin;
                        if (lp.width > 0) w = lp.width;
                        if (lp.height > 0) h = lp.height;
                    }
                    if (tag.startsWith("launcher_app:")) {
                        int columns = getHomeColumns();
                        w = Math.max(1, pageWidth / Math.max(1, columns));
                        h = LauncherUiUtils.dp(this,
                                homeCellHeightDp(prefs.getInt(KEY_ICON_SIZE, 48)));
                    }
                    if (w > 0 && h > 0 && android.graphics.Rect.intersects(
                            candidate, new android.graphics.Rect(x, y, x + w, y + h))) {
                        blocked = true;
                        break;
                    }
                }
                if (!blocked) return new int[]{left, top};
            }
        }
        return null;
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
        launchApp(app, null);
    }

    private void launchApp(ResolveInfo app, View sourceView) {
        Intent intent = packageManager.getLaunchIntentForPackage(app.activityInfo.packageName);
        if (intent != null) {
            try {
                if (android.os.Build.VERSION.SDK_INT >= 16 && sourceView != null
                        && sourceView.getWidth() > 0 && sourceView.getHeight() > 0) {
                    // Scale the launched app from the tapped icon, matching the native
                    // launcher-to-app transition available on Android 6 and newer.
                    android.app.ActivityOptions options =
                            android.app.ActivityOptions.makeScaleUpAnimation(sourceView,
                                    0, 0, sourceView.getWidth(), sourceView.getHeight());
                    startActivity(intent, options.toBundle());
                } else {
                    startActivity(intent);
                }
            } catch (Exception e) {
                try {
                    startActivity(intent);
                } catch (Exception ignored) {
                    Toast.makeText(this, "Unable to open app", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void applyClockStyle() {
        int style = prefs == null ? 0 : prefs.getInt(KEY_CLOCK_STYLE, 0);
        if (clockText == null) return;
        Integer previousStyle = appliedClockStyles.get(clockText);
        // Always reassert the custom renderer mode. A cached style index is not
        // proof that the active TextView still has the correct drawing mode,
        // especially when switching between the two independent page clocks.
        if (previousStyle == null || previousStyle.intValue() != style
                || clockText instanceof SevenSegmentClockTextView) {
            LauncherClockStyler.apply(this, clockText, style);
        }
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
        appliedClockStyles.put(clockText, style);
    }

    private void updateClock() {
        if (clockPanelsByPage[0] == null || clockPanelsByPage[1] == null) return;
        Date now = new Date();
        String dateValue = new SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(now);
        boolean use24Hour = DateFormat.is24HourFormat(this);
        int restorePage = Math.max(0, Math.min(1, currentPage));
        for (int page = 0; page < clockPanelsByPage.length; page++) {
            activateClockForPage(page);
            String clockValue = DateFormat.getTimeFormat(this).format(now);
            if (amPmText != null) {
                if (use24Hour) {
                    if (amPmText.length() != 0) amPmText.setText("");
                    if (amPmText.getVisibility() != View.GONE) amPmText.setVisibility(View.GONE);
                } else {
                    String marker = new SimpleDateFormat("a", Locale.getDefault()).format(now);
                    int markerAt = clockValue.toUpperCase(Locale.getDefault())
                            .lastIndexOf(marker.toUpperCase(Locale.getDefault()));
                    if (markerAt >= 0) {
                        clockValue = (clockValue.substring(0, markerAt)
                                + clockValue.substring(markerAt + marker.length())).trim();
                    }
                    if (!marker.contentEquals(amPmText.getText())) amPmText.setText(marker);
                    if (amPmText.getVisibility() != View.VISIBLE) amPmText.setVisibility(View.VISIBLE);
                }
            }
            clockValue = clockValue.trim();
            boolean sevenSegmentClock = clockText instanceof SevenSegmentClockTextView
                    && prefs.getInt(KEY_CLOCK_STYLE, 0) == 5;
            android.text.SpannableStringBuilder displayClock =
                    new android.text.SpannableStringBuilder(clockValue);
            if (!sevenSegmentClock && !clockValue.isEmpty()) {
                // Reserve a leading invisible '1' when the hour has one digit.
                int firstDigit = -1;
                for (int i = 0; i < clockValue.length(); i++) {
                    if (Character.digit(clockValue.charAt(i), 10) >= 0) {
                        firstDigit = i;
                        break;
                    }
                }
                if (firstDigit >= 0) {
                    int hourEnd = firstDigit;
                    while (hourEnd < clockValue.length()
                            && Character.digit(clockValue.charAt(hourEnd), 10) >= 0) {
                        hourEnd++;
                    }
                    if (hourEnd - firstDigit == 1 && hourEnd < clockValue.length()) {
                        char hourDigit = clockValue.charAt(firstDigit);
                        char invisibleOne = (char) (hourDigit - Character.digit(hourDigit, 10) + 1);
                        displayClock.insert(0, String.valueOf(invisibleOne));
                        displayClock.setSpan(new android.text.style.ForegroundColorSpan(Color.TRANSPARENT),
                                0, 1, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }
                // Draw the colon's two dots at the vertical center of the clock
                // view/background instead of using the font's naturally raised colon.
                int colonAt = displayClock.toString().indexOf(':');
                if (colonAt >= 0) {
                    displayClock.setSpan(new CenteredColonSpan(LauncherUiUtils.dp(this, 114) / 2f),
                            colonAt, colonAt + 1, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
            // Only update the TextView when its displayed value actually changes.
            // This avoids needless layout invalidations from time/date broadcasts.
            if (!displayClock.toString().contentEquals(clockText.getText())) {
                // The seven-segment renderer draws within fixed bounds. Avoid
                // text-metric-driven remeasurement as hour digits change (e.g. 1 -> 2).
                if (sevenSegmentClock) {
                    clockText.setText(displayClock, TextView.BufferType.NORMAL);
                    clockText.postInvalidateOnAnimation();
                } else {
                    clockText.setText(displayClock);
                }
            }
            int dateVisibility = prefs.getBoolean(KEY_SHOW_DATE, true) ? View.VISIBLE : View.GONE;
            if (dateText.getVisibility() != dateVisibility) dateText.setVisibility(dateVisibility);
            if (!dateValue.contentEquals(dateText.getText())) dateText.setText(dateValue);
            // Layout, typography and panel appearance are configuration-driven and
            // are applied when the clock is created or settings change, not every minute.
        }
        activateClockForPage(restorePage);
        // A time/date refresh must only replace text. Re-running geometry here
        // recalculates the panel from the current digits/date, so the background
        // can shrink and the clock shifts horizontally at a minute/day boundary.
        // Geometry is recalculated on creation, style changes, and explicit resize.
        updateClockResizeHandles();
        applyBrandVisibility();
    }

    private void installClockWidgetInteraction() {
        if (clockPanel == null) return;
        clockPanel.setTag("launcher_clock_widget");
        clockPanel.setClipChildren(false);
        clockPanel.setClipToPadding(false);
        // Four visible resize grips are hosted by the root overlay, not inside the
        // vertical clock layout, so they never add unwanted height to the clock/date.
        for (int i = 0; i < clockEdgeHandles.length; i++) {
            if (clockEdgeHandles[i] != null) continue;
            View grip = new View(this);
            GradientDrawable gripBg = new GradientDrawable();
            gripBg.setColor(Color.rgb(115, 159, 255));
            gripBg.setCornerRadius(LauncherUiUtils.dp(this, 6));
            grip.setBackground(gripBg);
            grip.setVisibility(View.GONE);
            clockEdgeHandles[i] = grip;
            int size = LauncherUiUtils.dp(this, 28);
            FrameLayout.LayoutParams gripLp = new FrameLayout.LayoutParams(
                    i < 2 ? size : LauncherUiUtils.dp(this, 8),
                    i < 2 ? LauncherUiUtils.dp(this, 8) : size);
            root.addView(grip, gripLp);
        }
        updateClockResizeHandles();
        final int panelPage = activeClockPage;
        clockPanel.setOnTouchListener((view, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                activateClockForPage(panelPage);
                clockWidgetLongPressActivated = false;
                clockWidgetDownRawX = event.getRawX();
                clockWidgetDownRawY = event.getRawY();
                clockWidgetStartX = clockPanel.getTranslationX();
                clockWidgetStartY = clockPanel.getTranslationY();
                handler.removeCallbacks(clockWidgetLongPress);
                handler.postDelayed(clockWidgetLongPress, 650L);
                // Do not block a parent horizontal/vertical swipe until a long-press
                // actually puts the clock into widget-edit mode.
                if (view.getParent() != null) {
                    view.getParent().requestDisallowInterceptTouchEvent(false);
                }
                return true;
            }
            if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
                float dx = event.getRawX() - clockWidgetDownRawX;
                float dy = event.getRawY() - clockWidgetDownRawY;
                if (clockWidgetEditing && (Math.abs(dx) > LauncherUiUtils.dp(this, 5)
                        || Math.abs(dy) > LauncherUiUtils.dp(this, 5))) {
                    clockWidgetDragging = true;
                    float targetX = clockWidgetStartX + dx;
                    float targetY = clockWidgetStartY + dy;
                    // The clock is a free-positioned widget: do not constrain it to
                    // the header band or reject overlaps with workspace content.
                    setClampedClockTranslation(targetX, targetY, true);
                    updateClockResizeHandles();
                    setWorkspaceGridVisible(true, clockPanel);
                    updateLiveDisplacements((FrameLayout) clockPanel.getParent(), clockPanel);
                } else if (Math.abs(dx) > LauncherUiUtils.dp(this, 8)
                        || Math.abs(dy) > LauncherUiUtils.dp(this, 8)) {
                    handler.removeCallbacks(clockWidgetLongPress);
                    // Outside edit mode the clock must not swallow workspace swipes.
                    // Let the parent pager / global swipe detector take over.
                    if (view.getParent() != null) {
                        view.getParent().requestDisallowInterceptTouchEvent(false);
                    }
                    return false;
                }
                return true;
            }
            if (event.getActionMasked() == MotionEvent.ACTION_UP
                    || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                handler.removeCallbacks(clockWidgetLongPress);
                if (clockWidgetDragging) {
                    finishLiveDisplacements(event.getActionMasked() == MotionEvent.ACTION_UP, clockPanel);
                    clockWidgetDragging = false;
                    setWorkspaceGridVisible(false, null);
                    updateClockResizeHandles();
                }
                // A long press intentionally leaves the grips visible after ACTION_UP.
                // Only the root's outside-tap check dismisses edit mode.
                clockWidgetLongPressActivated = false;
                return true;
            }
            return true;
        });
        float fallbackX = activeClockPage == 0 ? getFloatPreferenceCompat(KEY_CLOCK_WIDGET_X, 0f) : 0f;
        float fallbackY = activeClockPage == 0 ? getFloatPreferenceCompat(KEY_CLOCK_WIDGET_Y, 0f) : 0f;
        float savedClockX = getFloatPreferenceCompat(clockPrefKey(KEY_CLOCK_WIDGET_X, activeClockPage), fallbackX);
        float savedClockY = getFloatPreferenceCompat(clockPrefKey(KEY_CLOCK_WIDGET_Y, activeClockPage), fallbackY);
        clockPanel.post(() -> {
            if (clockPanel == null || clockPanel.getParent() == null) return;
            setClampedClockTranslation(savedClockX, savedClockY, true);
            updateClockResizeHandles();
        });
    }

    private final Runnable clockWidgetLongPress = () -> {
        if (clockPanel == null || drawerOpen) return;
        clockWidgetLongPressActivated = true;
        clockWidgetEditing = true;
        if (clockWidgetResizeHandle != null) clockWidgetResizeHandle.setVisibility(View.GONE);
        for (View grip : clockEdgeHandles) if (grip != null) grip.setVisibility(View.VISIBLE);
        updateClockResizeHandles();
        // Keep the clock's actual colour background visible during editing.
        // The edge grips and workspace grid already communicate edit mode; replacing
        // the background here made it appear to vanish while moving/resizing.
        setWorkspaceGridVisible(true, clockPanel);
        Toast.makeText(this, "Clock widget: drag to move; drag the middle of any edge to resize; tap outside to finish",
                Toast.LENGTH_LONG).show();
    };

    private void exitClockWidgetEditMode() {
        clockWidgetEditing = false;
        if (clockWidgetResizeHandle != null) clockWidgetResizeHandle.setVisibility(View.GONE);
        for (View grip : clockEdgeHandles) if (grip != null) grip.setVisibility(View.GONE);
        applyClockPanelColor(false);
        setWorkspaceGridVisible(false, null);
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
        // Preserve the time-driven colour cycling while applying the user's
        // saved opacity uniformly to every stop in the gradient.
        int opacity = Math.max(0, Math.min(100,
                prefs == null ? 50 : prefs.getInt(KEY_CLOCK_BG_OPACITY, 50)));
        // 50% reproduces the original background alpha; 100% doubles it.
        // Clamp each channel at 255 so stronger settings remain valid ARGB.
        int opacityScale = opacity * 2;
        int leftEdge = Color.argb(Math.min(255, 125 * opacityScale / 100), leftR, leftG, leftB);
        int rightEdge = Color.argb(Math.min(255, 149 * opacityScale / 100), tone[0], tone[1], tone[2]);
        int leftMid = Color.argb(Math.min(255, 72 * opacityScale / 100), leftR, leftG, leftB);
        int rightMid = Color.argb(Math.min(255, 76 * opacityScale / 100), tone[0], tone[1], tone[2]);
        GradientDrawable next = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[] {leftEdge, leftMid,
                        Color.argb(Math.min(255, 22 * opacityScale / 100), 125, 155, 235),
                        rightMid, rightEdge});
        next.setCornerRadius(LauncherUiUtils.dp(this, 18));
        // Draw the shorter colour panel inside a zero-padding drawable. InsetDrawable
        // reports its insets as background padding on some Android versions, which
        // changes the LinearLayout's measured position when edit mode is dismissed.
        // Drawing the inset internally keeps clock geometry independent from its background.
        boolean sevenSegmentBackground = clockText instanceof SevenSegmentClockTextView
                && prefs.getInt(KEY_CLOCK_STYLE, 0) == 5;
        // Keep the coloured clock background at the full container height for both styles.
        float totalHeightReduction = 0f;
        ClockPanelBackgroundDrawable centeredBackground =
                new ClockPanelBackgroundDrawable(next, totalHeightReduction);
        if (animate) {
            hero.setBackground(centeredBackground);
            next.setAlpha(0);
            android.animation.ValueAnimator fade = android.animation.ValueAnimator.ofInt(0, 255);
            fade.setDuration(animationDuration(260L));
            fade.addUpdateListener(a -> next.setAlpha((Integer) a.getAnimatedValue()));
            fade.start();
        } else {
            hero.setBackground(centeredBackground);
        }
    }


    private boolean isWorkspaceItem(View view) {
        if (view == null || !(view.getTag() instanceof String)) return false;
        String tag = (String) view.getTag();
        // Include the clock in workspace displacement so widgets and shortcuts
        // cannot be dropped over it without the same push-aside behavior.
        return tag.startsWith("launcher_widget:") || tag.startsWith("launcher_app:")
                || "launcher_contact_shortcut".equals(tag)
                || "launcher_clock_widget".equals(tag);
    }

    private int[] itemGeometry(View view) {
        if (view == null) return new int[]{0, 0, 0, 0};
        float sx = Math.max(0.01f, view.getScaleX());
        float sy = Math.max(0.01f, view.getScaleY());
        // Use the actual on-screen View bounds, not requested LayoutParams sizes or
        // margins. This is essential for mixed-size widgets and shortcuts: their
        // collision footprint must match their rendered container, not a grid cell.
        int x = Math.round(view.getLeft() + view.getTranslationX()
                + view.getPivotX() * (1f - sx));
        int y = Math.round(view.getTop() + view.getTranslationY()
                + view.getPivotY() * (1f - sy));
        return new int[]{x, y, Math.max(1, Math.round(view.getWidth() * sx)),
                Math.max(1, Math.round(view.getHeight() * sy))};
    }

    private boolean overlapsWithGap(android.graphics.Rect a, android.graphics.Rect b, int gap) {
        android.graphics.Rect expanded = new android.graphics.Rect(
                a.left - gap, a.top - gap, a.right + gap, a.bottom + gap);
        return android.graphics.Rect.intersects(expanded, b);
    }

    /**
     * Keep the clock's scaled visual bounds inside its own home-page viewport.
     * Translation is clamped in parent coordinates, accounting for the scale pivot
     * so left/center/right-aligned clocks all respect the same screen edges.
     */
    private float[] clampClockTranslation(View panel, float translationX, float translationY) {
        if (panel == null || !(panel.getParent() instanceof ViewGroup)) {
            return new float[]{translationX, translationY};
        }
        ViewGroup parent = (ViewGroup) panel.getParent();
        int parentWidth = parent.getWidth();
        int parentHeight = parent.getHeight();
        if (parentWidth <= 0 || parentHeight <= 0 || panel.getWidth() <= 0 || panel.getHeight() <= 0) {
            return new float[]{translationX, translationY};
        }
        float scaleX = Math.max(0.01f, panel.getScaleX());
        float scaleY = Math.max(0.01f, panel.getScaleY());
        // A translation cannot fit an oversized clock. Reduce only the rendered
        // scale enough to fit the page, so neither edge can ever remain off-screen.
        if (panel.getWidth() > 0 && panel.getWidth() * scaleX > parentWidth) {
            scaleX = Math.max(0.01f, parentWidth / (float) panel.getWidth());
            panel.setScaleX(scaleX);
        }
        if (panel.getHeight() > 0 && panel.getHeight() * scaleY > parentHeight) {
            scaleY = Math.max(0.01f, parentHeight / (float) panel.getHeight());
            panel.setScaleY(scaleY);
        }
        float visualWidth = panel.getWidth() * scaleX;
        float visualHeight = panel.getHeight() * scaleY;
        float visualLeft = panel.getLeft() + translationX
                + panel.getPivotX() * (1f - scaleX);
        float visualTop = panel.getTop() + translationY
                + panel.getPivotY() * (1f - scaleY);
        if (visualLeft < 0f) {
            translationX -= visualLeft;
        } else if (visualLeft + visualWidth > parentWidth) {
            translationX -= visualLeft + visualWidth - parentWidth;
        }
        if (visualTop < 0f) {
            translationY -= visualTop;
        } else if (visualTop + visualHeight > parentHeight) {
            translationY -= visualTop + visualHeight - parentHeight;
        }
        return new float[]{translationX, translationY};
    }

    private void setClampedClockTranslation(float translationX, float translationY, boolean persist) {
        if (clockPanel == null) return;
        float[] clamped = clampClockTranslation(clockPanel, translationX, translationY);
        clockPanel.setTranslationX(clamped[0]);
        clockPanel.setTranslationY(clamped[1]);
        if (persist) {
            prefs.edit()
                    .putFloat(clockPrefKey(KEY_CLOCK_WIDGET_X, activeClockPage), clamped[0])
                    .putFloat(clockPrefKey(KEY_CLOCK_WIDGET_Y, activeClockPage), clamped[1])
                    .apply();
        }
    }

    /** Live push-aside behavior for workspace items while a widget or shortcut is dragged. */
    private void updateLiveDisplacements(FrameLayout page, View movingView) {
        if (page == null || movingView == null || movingView.getParent() != page
                || page.getWidth() <= 0 || page.getHeight() <= 0) return;

        int[] moving = itemGeometry(movingView);
        if (moving[2] <= 0 || moving[3] <= 0) return;
        android.graphics.Rect movingRect = new android.graphics.Rect(
                moving[0], moving[1], moving[0] + moving[2], moving[1] + moving[3]);
        // Start moving neighbours while the dragged item approaches from any side,
        // not only after their visible bounds already touch.
        // Push a neighbour only once the dragged item actually reaches its bounds.
        // A large approach gap makes nearby items jump before the drop is real.
        int gap = 0;

        // First restore items whose saved slots are no longer covered by the drag.
        // Their original slots are reserved below so another displaced item cannot
        // occupy a slot that must later be restored.
        java.util.Iterator<java.util.Map.Entry<View, int[]>> iterator =
                liveDisplacedItems.entrySet().iterator();
        while (iterator.hasNext()) {
            java.util.Map.Entry<View, int[]> entry = iterator.next();
            View displaced = entry.getKey();
            int[] original = entry.getValue();
            if (displaced.getParent() != page) {
                iterator.remove();
                continue;
            }
            int[] g = itemGeometry(displaced);
            android.graphics.Rect originalRect = new android.graphics.Rect(
                    original[0], original[1], original[0] + g[2], original[1] + g[3]);
            if (!overlapsWithGap(movingRect, originalRect, gap)) {
                if (displaced == clockPanel) {
                    // Clock geometry is translation/scale based; writing its visual
                    // coordinates into layout margins corrupts its saved position.
                    setClampedClockTranslation(
                            original[0] - clockPanel.getLeft()
                                    - clockPanel.getPivotX() * (1f - clockPanel.getScaleX()),
                            original[1] - clockPanel.getTop()
                                    - clockPanel.getPivotY() * (1f - clockPanel.getScaleY()), true);
                } else {
                    ViewGroup.LayoutParams raw = displaced.getLayoutParams();
                    if (raw instanceof FrameLayout.LayoutParams) {
                        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) raw;
                        lp.leftMargin = original[0];
                        lp.topMargin = original[1];
                        displaced.setLayoutParams(lp);
                    }
                }
                iterator.remove();
            }
        }

        for (int i = 0; i < page.getChildCount(); i++) {
            View other = page.getChildAt(i);
            if (other == movingView || !isWorkspaceItem(other)
                    || liveDisplacedItems.containsKey(other)) continue;

            int[] g = itemGeometry(other);
            if (g[2] <= 0 || g[3] <= 0) continue;
            android.graphics.Rect otherRect = new android.graphics.Rect(
                    g[0], g[1], g[0] + g[2], g[1] + g[3]);
            if (!overlapsWithGap(movingRect, otherRect, gap)) continue;

            int maxX = page.getWidth() - g[2] - gap;
            int maxY = page.getHeight() - g[3] - gap;
            int min = gap;
            if (maxX < min || maxY < min) continue;

            // Build candidate coordinates from the edges of real obstacles rather
            // than scanning every 4dp across the entire screen on every MOVE frame.
            // This works for mixed-size widgets and shortcuts and avoids missing a
            // narrow valid gap because the step does not divide the page dimensions.
            java.util.ArrayList<android.graphics.Rect> obstacles =
                    new java.util.ArrayList<android.graphics.Rect>();
            for (int j = 0; j < page.getChildCount(); j++) {
                View blocker = page.getChildAt(j);
                if (blocker == other || !isWorkspaceItem(blocker)) continue;
                int[] bg = itemGeometry(blocker);
                if (bg[2] <= 0 || bg[3] <= 0) continue;
                obstacles.add(new android.graphics.Rect(
                        bg[0], bg[1], bg[0] + bg[2], bg[1] + bg[3]));
            }
            for (java.util.Map.Entry<View, int[]> entry : liveDisplacedItems.entrySet()) {
                View reserved = entry.getKey();
                if (reserved == other || reserved.getParent() != page) continue;
                int[] original = entry.getValue();
                int[] rg = itemGeometry(reserved);
                obstacles.add(new android.graphics.Rect(
                        original[0], original[1], original[0] + rg[2], original[1] + rg[3]));
            }

            java.util.TreeSet<Integer> xCandidates = new java.util.TreeSet<Integer>();
            java.util.TreeSet<Integer> yCandidates = new java.util.TreeSet<Integer>();
            xCandidates.add(Math.max(min, Math.min(maxX, g[0])));
            yCandidates.add(Math.max(min, Math.min(maxY, g[1])));
            xCandidates.add(min);
            xCandidates.add(maxX);
            yCandidates.add(min);
            yCandidates.add(maxY);
            for (android.graphics.Rect obstacle : obstacles) {
                xCandidates.add(Math.max(min, Math.min(maxX, obstacle.left - g[2] - gap)));
                xCandidates.add(Math.max(min, Math.min(maxX, obstacle.right + gap)));
                yCandidates.add(Math.max(min, Math.min(maxY, obstacle.top - g[3] - gap)));
                yCandidates.add(Math.max(min, Math.min(maxY, obstacle.bottom + gap)));
            }

            int bestX = -1, bestY = -1, bestDistance = Integer.MAX_VALUE;
            for (Integer candidateY : yCandidates) {
                for (Integer candidateX : xCandidates) {
                    int x = candidateX.intValue();
                    int y = candidateY.intValue();
                    android.graphics.Rect candidateWithGap = new android.graphics.Rect(
                            x - gap, y - gap, x + g[2] + gap, y + g[3] + gap);
                    boolean blocked = false;
                    for (android.graphics.Rect obstacle : obstacles) {
                        if (android.graphics.Rect.intersects(candidateWithGap, obstacle)) {
                            blocked = true;
                            break;
                        }
                    }
                    if (blocked) continue;
                    int distance = Math.abs(x - g[0]) + Math.abs(y - g[1]);
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        bestX = x;
                        bestY = y;
                    }
                }
            }

            if (bestX >= 0) {
                liveDisplacedItems.put(other, new int[]{g[0], g[1]});
                if (other == clockPanel) {
                    setClampedClockTranslation(clockPanel.getTranslationX() + bestX - g[0],
                            clockPanel.getTranslationY() + bestY - g[1], true);
                } else {
                    FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) other.getLayoutParams();
                    lp.leftMargin = bestX;
                    lp.topMargin = bestY;
                    other.setLayoutParams(lp);
                }
            }
        }
    }

    /**
     * Final safety pass after a drag/resize. Treat every app, widget, shortcut and
     * clock as a rectangle and place each item into a genuinely free pixel position.
     * App icons keep their existing size; non-app items use their visual dimensions.
     */
    private void resolveAllWorkspaceOverlaps(FrameLayout page, View priorityView) {
        if (page == null || page.getWidth() <= 0 || page.getHeight() <= 0) return;
        java.util.ArrayList<View> items = new java.util.ArrayList<View>();
        if (priorityView != null && priorityView.getParent() == page && isWorkspaceItem(priorityView)) {
            items.add(priorityView);
        }
        for (int i = 0; i < page.getChildCount(); i++) {
            View child = page.getChildAt(i);
            if (child != priorityView && isWorkspaceItem(child)) items.add(child);
        }
        java.util.ArrayList<android.graphics.Rect> accepted = new java.util.ArrayList<android.graphics.Rect>();
        for (View item : items) {
            int[] g = itemGeometry(item);
            int w = Math.max(1, g[2]), h = Math.max(1, g[3]);
            int maxX = Math.max(0, page.getWidth() - w);
            int maxY = Math.max(0, page.getHeight() - h);
            int startX = Math.max(0, Math.min(maxX, g[0]));
            int startY = Math.max(0, Math.min(maxY, g[1]));
            java.util.TreeSet<Integer> xs = new java.util.TreeSet<Integer>();
            java.util.TreeSet<Integer> ys = new java.util.TreeSet<Integer>();
            xs.add(startX); ys.add(startY); xs.add(0); ys.add(0); xs.add(maxX); ys.add(maxY);
            for (android.graphics.Rect a : accepted) {
                xs.add(Math.max(0, Math.min(maxX, a.left - w)));
                xs.add(Math.max(0, Math.min(maxX, a.right)));
                ys.add(Math.max(0, Math.min(maxY, a.top - h)));
                ys.add(Math.max(0, Math.min(maxY, a.bottom)));
            }
            int bestX = -1, bestY = -1;
            long bestDistance = Long.MAX_VALUE;
            for (Integer cy : ys) for (Integer cx : xs) {
                int x = cx.intValue(), y = cy.intValue();
                android.graphics.Rect candidate = new android.graphics.Rect(x, y, x + w, y + h);
                boolean blocked = false;
                for (android.graphics.Rect a : accepted) {
                    if (android.graphics.Rect.intersects(candidate, a)) { blocked = true; break; }
                }
                if (blocked) continue;
                long distance = Math.abs((long) x - g[0]) + Math.abs((long) y - g[1]);
                if (distance < bestDistance) { bestDistance = distance; bestX = x; bestY = y; }
            }
            // Keep an oversized non-clock widget inside the page as far as possible.
            // A clock is scaled down by the clock clamp if it exceeds the viewport.
            if (bestX < 0) {
                if (item == clockPanel) {
                    float[] bounded = clampClockTranslation(clockPanel,
                            clockPanel.getTranslationX(), clockPanel.getTranslationY());
                    setClampedClockTranslation(bounded[0], bounded[1], true);
                    int[] now = itemGeometry(item);
                    accepted.add(new android.graphics.Rect(now[0], now[1], now[0] + now[2], now[1] + now[3]));
                } else {
                    // Do not knowingly move an item on top of another item when no
                    // fitting rectangle exists on this page.
                    accepted.add(new android.graphics.Rect(g[0], g[1], g[0] + w, g[1] + h));
                }
                continue;
            }
            if (item == clockPanel) {
                setClampedClockTranslation(clockPanel.getTranslationX() + bestX - g[0],
                        clockPanel.getTranslationY() + bestY - g[1], true);
            } else if (item.getLayoutParams() instanceof FrameLayout.LayoutParams) {
                FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) item.getLayoutParams();
                lp.leftMargin += bestX - g[0];
                lp.topMargin += bestY - g[1];
                item.setLayoutParams(lp);
            }
            accepted.add(new android.graphics.Rect(bestX, bestY, bestX + w, bestY + h));
            persistWorkspaceItemPosition(page, item, bestX, bestY, w, h);
        }
    }

    private void persistWorkspaceItemPosition(FrameLayout page, View item, int x, int y, int width, int height) {
        Object rawTag = item.getTag();
        if (!(rawTag instanceof String)) return;
        String tag = (String) rawTag;
        int pageIndex = pagesStrip == null ? 0 : Math.max(0, pagesStrip.indexOfChild(page));
        if (tag.startsWith("launcher_widget:")) {
            try { saveWidgetGeometry(Integer.parseInt(tag.substring("launcher_widget:".length())), pageIndex, (FrameLayout) item); }
            catch (NumberFormatException ignored) {}
        } else if (tag.startsWith("launcher_app:")) {
            int columns = getHomeColumns();
            int cellWidth = Math.max(1, page.getWidth() / columns);
            int cellHeight = LauncherUiUtils.dp(this, homeCellHeightDp(prefs.getInt(KEY_ICON_SIZE, 48)));
            String pkg = tag.substring("launcher_app:".length());
            int col = Math.max(0, Math.min(columns - 1, Math.round((float) x / cellWidth)));
            int row = Math.max(0, Math.round((float) y / cellHeight));
            saveIconSlot(pkg, col, row, columns);
            prefs.edit().putInt(KEY_ICON_PAGE + pkg, pageIndex).apply();
        } else if ("launcher_contact_shortcut".equals(tag)) {
            prefs.edit().putInt(KEY_SHORTCUT_PAGE, pageIndex)
                    .putFloat(KEY_SHORTCUT_X, x / (float) Math.max(1, page.getWidth() - width))
                    .putFloat(KEY_SHORTCUT_Y, y / (float) Math.max(1, page.getHeight() - height)).apply();
        } else if ("launcher_clock_widget".equals(tag)) {
            prefs.edit().putFloat(clockPrefKey(KEY_CLOCK_WIDGET_X, pageIndex), item.getTranslationX())
                    .putFloat(clockPrefKey(KEY_CLOCK_WIDGET_Y, pageIndex), item.getTranslationY()).apply();
        }
    }

    /** Finalize temporary push-aside positions on drop, or roll them back on cancel. */
    private void finishLiveDisplacements(boolean commit, View movingView) {
        if (liveDisplacedItems.isEmpty()) {
            if (movingView != null && movingView.getParent() instanceof FrameLayout) {
                resolveAllWorkspaceOverlaps((FrameLayout) movingView.getParent(), movingView);
                refreshWorkspaceGrid();
            }
            return;
        }
        int columns = getHomeColumns();
        int cellWidth = Math.max(1, pageViewport == null ? 1 : pageViewport.getWidth() / columns);
        int cellHeight = LauncherUiUtils.dp(this, homeCellHeightDp(prefs.getInt(KEY_ICON_SIZE, 48)));
        java.util.ArrayList<View> displacedViews = new java.util.ArrayList<>(liveDisplacedItems.keySet());
        for (View view : displacedViews) {
            int[] original = liveDisplacedItems.get(view);
            if (view.getParent() instanceof FrameLayout) {
                FrameLayout page = (FrameLayout) view.getParent();
                boolean keepDisplacement = commit;
                if (view == clockPanel) {
                    if (!keepDisplacement) {
                        setClampedClockTranslation(original[0] - clockPanel.getLeft()
                                        - clockPanel.getPivotX() * (1f - clockPanel.getScaleX()),
                                original[1] - clockPanel.getTop()
                                        - clockPanel.getPivotY() * (1f - clockPanel.getScaleY()), true);
                    } else {
                        int pageIndex = pagesStrip == null ? 0 : Math.max(0, pagesStrip.indexOfChild(page));
                        prefs.edit()
                                .putFloat(clockPrefKey(KEY_CLOCK_WIDGET_X, pageIndex), clockPanel.getTranslationX())
                                .putFloat(clockPrefKey(KEY_CLOCK_WIDGET_Y, pageIndex), clockPanel.getTranslationY())
                                .apply();
                    }
                    continue;
                }
                if (keepDisplacement && movingView != null
                        && movingView.getParent() == page) {
                    // Commit a neighbour's new slot only if the dragged item still
                    // occupies the neighbour's original slot after drop resolution.
                    // If collision handling sends the dragged widget elsewhere,
                    // put the neighbour back instead of leaving a pointless gap.
                    int[] movingGeometry = itemGeometry(movingView);
                    int[] neighbourGeometry = itemGeometry(view);
                    android.graphics.Rect movingRect = new android.graphics.Rect(
                            movingGeometry[0], movingGeometry[1],
                            movingGeometry[0] + movingGeometry[2],
                            movingGeometry[1] + movingGeometry[3]);
                    android.graphics.Rect originalRect = new android.graphics.Rect(
                            original[0], original[1],
                            original[0] + neighbourGeometry[2],
                            original[1] + neighbourGeometry[3]);
                    keepDisplacement = android.graphics.Rect.intersects(movingRect, originalRect);
                }
                if (!keepDisplacement) {
                    FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) view.getLayoutParams();
                    lp.leftMargin = original[0];
                    lp.topMargin = original[1];
                    view.setLayoutParams(lp);
                } else {
                    int[] g = itemGeometry(view);
                    Object rawTag = view.getTag();
                    if (rawTag instanceof String) {
                        String tag = (String) rawTag;
                        int pageIndex = pagesStrip == null ? 0 : Math.max(0, pagesStrip.indexOfChild(page));
                        if (tag.startsWith("launcher_widget:")) {
                            try { saveWidgetGeometry(Integer.parseInt(tag.substring("launcher_widget:".length())), pageIndex, (FrameLayout) view); }
                            catch (NumberFormatException ignored) {}
                        } else if (tag.startsWith("launcher_app:")) {
                            String pkg = tag.substring("launcher_app:".length());
                            int col = Math.max(0, Math.min(columns - 1, Math.round((float) g[0] / cellWidth)));
                            int row = Math.max(0, Math.round((float) g[1] / cellHeight));
                            saveIconSlot(pkg, col, row, columns);
                            prefs.edit().putInt(KEY_ICON_PAGE + pkg, pageIndex).apply();
                        } else if ("launcher_contact_shortcut".equals(tag)) {
                            prefs.edit().putInt(KEY_SHORTCUT_PAGE, pageIndex)
                                    .putFloat(KEY_SHORTCUT_X, g[0] / (float) Math.max(1, page.getWidth() - g[2]))
                                    .putFloat(KEY_SHORTCUT_Y, g[1] / (float) Math.max(1, page.getHeight() - g[3])).apply();
                        }
                    }
                }
            }
        }
        liveDisplacedItems.clear();
        if (movingView != null && movingView.getParent() instanceof FrameLayout) {
            resolveAllWorkspaceOverlaps((FrameLayout) movingView.getParent(), movingView);
        }
        refreshWorkspaceGrid();
    }

    private void resolveWidgetIconCollisions(FrameLayout page, FrameLayout widgetContainer) {
        if (page == null || widgetContainer == null || page.getWidth() <= 0 || page.getHeight() <= 0) return;
        int columns = getHomeColumns();
        int cellWidth = Math.max(1, page.getWidth() / columns);
        int cellHeight = LauncherUiUtils.dp(this,
                homeCellHeightDp(prefs.getInt(KEY_ICON_SIZE, 48)));
        int rows = Math.max(1, page.getHeight() / cellHeight);
        FrameLayout.LayoutParams widgetLp = (FrameLayout.LayoutParams) widgetContainer.getLayoutParams();
        int wx1 = widgetLp.leftMargin, wy1 = widgetLp.topMargin;
        // Layout params update before getWidth()/getHeight() do. Use the requested
        // geometry so resize-release collision checks see the new widget bounds.
        int widgetWidth = widgetLp.width > 0 ? widgetLp.width : widgetContainer.getWidth();
        int widgetHeight = widgetLp.height > 0 ? widgetLp.height : widgetContainer.getHeight();
        int wx2 = wx1 + widgetWidth, wy2 = wy1 + widgetHeight;
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
        // If a widget is moved over the contact shortcut, place the shortcut again
        // using its complete bounds and check both home pages.
        View contactShortcut = null;
        for (int i = 0; i < page.getChildCount(); i++) {
            View child = page.getChildAt(i);
            if ("launcher_contact_shortcut".equals(child.getTag())) {
                contactShortcut = child;
                break;
            }
        }
        if (contactShortcut != null) {
            FrameLayout.LayoutParams shortcutLp = (FrameLayout.LayoutParams) contactShortcut.getLayoutParams();
            android.graphics.Rect widgetBounds = new android.graphics.Rect(wx1, wy1, wx2, wy2);
            android.graphics.Rect shortcutBounds = new android.graphics.Rect(shortcutLp.leftMargin,
                    shortcutLp.topMargin, shortcutLp.leftMargin + contactShortcut.getWidth(),
                    shortcutLp.topMargin + contactShortcut.getHeight());
            if (android.graphics.Rect.intersects(widgetBounds, shortcutBounds)) {
                int pageIndex = pagesStrip == null ? 0 : pagesStrip.indexOfChild(page);
                prefs.edit().putInt(KEY_SHORTCUT_PAGE, pageIndex == 0 ? 1 : 0).apply();
                renderSavedContactShortcut();
            }
        }
    }

    private int getHomeColumns() { return Math.max(3, Math.min(6, prefs == null ? COLUMNS : prefs.getInt(KEY_COLUMNS, COLUMNS))); }
    private int getDrawerColumns() { return Math.max(3, Math.min(6, prefs == null ? COLUMNS : prefs.getInt(KEY_DRAWER_COLUMNS, COLUMNS))); }

    private int getHomeGridRows() {
        int pageHeight = pageViewport != null && pageViewport.getHeight() > LauncherUiUtils.dp(this, 100)
                ? pageViewport.getHeight()
                : Math.max(LauncherUiUtils.dp(this, 360),
                        getResources().getDisplayMetrics().heightPixels - LauncherUiUtils.dp(this, 260));
        int cellHeight = LauncherUiUtils.dp(this,
                Math.max(56, prefs == null ? 48 + 28 : prefs.getInt(KEY_ICON_SIZE, 48) + 28));
        return Math.max(1, pageHeight / Math.max(1, cellHeight));
    }

    private int pageCapacity() { return getHomeColumns() * getHomeGridRows(); }

    private void clearIconPositions() {
        SharedPreferences.Editor editor = prefs.edit();
        for (String key : prefs.getAll().keySet()) {
            if (key.startsWith(KEY_ICON_X) || key.startsWith(KEY_ICON_Y)) editor.remove(key);
        }
        editor.apply();
    }

    private void saveIconSlot(String packageName, int col, int row, int columns) {
        int slotHeight = LauncherUiUtils.dp(this,
                homeCellHeightDp(prefs.getInt(KEY_ICON_SIZE, 48)));
        int usableHeight = pageViewport != null && pageViewport.getHeight() > 0
                ? pageViewport.getHeight()
                : Math.max(slotHeight, getResources().getDisplayMetrics().heightPixels
                        - LauncherUiUtils.dp(this, 260));
        int rows = Math.max(1, usableHeight / slotHeight);
        int safeRow = Math.max(0, Math.min(rows - 1, row));
        prefs.edit().putFloat(KEY_ICON_X + packageName, col / (float) Math.max(1, columns - 1))
                .putFloat(KEY_ICON_Y + packageName, safeRow / (float) Math.max(1, rows - 1)).apply();
    }

    private GradientDrawable drawerBackground() {
        int theme = prefs == null ? 0 : prefs.getInt(KEY_COLOR_THEME, 0);
        GradientDrawable background;
        if (theme == 1) background = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{Color.rgb(8, 43, 63), Color.rgb(8, 17, 31)});
        else if (theme == 2) background = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{Color.rgb(49, 24, 76), Color.rgb(12, 13, 29)});
        else if (theme == 3) background = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{Color.rgb(36, 42, 55), Color.rgb(12, 15, 23)});
        else background = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{Color.rgb(15, 20, 36), Color.rgb(10, 13, 24)});
        int opacity = prefs == null ? 100 : Math.max(0, Math.min(100, prefs.getInt(KEY_DRAWER_OPACITY, 100)));
        background.setAlpha(Math.round(opacity * 2.55f));
        return background;
    }

    private int getDrawerTransition() {
        return Math.max(0, Math.min(3, prefs == null ? 0 : prefs.getInt(KEY_DRAWER_TRANSITION, 0)));
    }

    private void addSection(LinearLayout panel, String title) {
        // Give each category its own visual header so the settings don't read as
        // one long list of identical controls.
        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        headerRow.setPadding(LauncherUiUtils.dp(this, 10), LauncherUiUtils.dp(this, 9),
                LauncherUiUtils.dp(this, 10), LauncherUiUtils.dp(this, 9));
        headerRow.setBackground(LauncherUiUtils.roundRect(this,
                new int[]{Color.rgb(43, 58, 94), Color.rgb(28, 38, 66)}, LauncherUiUtils.dp(this, 12)));
        String glyphText = "⚙";
        int accent = Color.rgb(146, 171, 239);
        if (title.toLowerCase(Locale.ROOT).contains("home")) { glyphText = "⌂"; accent = Color.rgb(119, 205, 255); }
        else if (title.toLowerCase(Locale.ROOT).contains("clock")) { glyphText = "◷"; accent = Color.rgb(255, 196, 112); }
        else if (title.toLowerCase(Locale.ROOT).contains("animation")) { glyphText = "↔"; accent = Color.rgb(196, 154, 255); }
        else if (title.toLowerCase(Locale.ROOT).contains("drawer")) { glyphText = "▦"; accent = Color.rgb(111, 225, 190); }
        else if (title.toLowerCase(Locale.ROOT).contains("appearance")) { glyphText = "◐"; accent = Color.rgb(255, 145, 185); }
        else if (title.toLowerCase(Locale.ROOT).contains("manage")) { glyphText = "☷"; accent = Color.rgb(255, 186, 115); }
        TextView glyph = LauncherUiUtils.text(this, glyphText, 20, accent, true);
        glyph.setGravity(Gravity.CENTER);
        headerRow.addView(glyph, new LinearLayout.LayoutParams(LauncherUiUtils.dp(this, 32), LauncherUiUtils.dp(this, 30)));
        TextView header = LauncherUiUtils.text(this, title, 15, Color.WHITE, true);
        header.setPadding(LauncherUiUtils.dp(this, 8), 0, 0, 0);
        headerRow.addView(header, new LinearLayout.LayoutParams(0, -2, 1f));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = LauncherUiUtils.dp(this, 14);
        params.bottomMargin = LauncherUiUtils.dp(this, 9);
        panel.addView(headerRow, params);
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
        heading.setPadding(LauncherUiUtils.dp(this, 4), LauncherUiUtils.dp(this, 5), LauncherUiUtils.dp(this, 4), LauncherUiUtils.dp(this, 7));
        heading.setSingleLine(false);
        panel.addView(heading, new LinearLayout.LayoutParams(-1, -2));

        // Keep every option readable. Long labels (for example, animation names)
        // must use their natural width instead of being squeezed into equal slots.
        android.widget.HorizontalScrollView horizontal = new android.widget.HorizontalScrollView(this);
        horizontal.setHorizontalScrollBarEnabled(false);
        horizontal.setFillViewport(false);
        LinearLayout choicesRow = new LinearLayout(this);
        choicesRow.setGravity(Gravity.CENTER_VERTICAL);
        for (int i = 0; i < choices.length; i++) {
            final int value = i;
            TextView chip = LauncherUiUtils.text(this, choices[i], 12,
                    i == selected ? Color.WHITE : Color.rgb(196, 205, 228), i == selected);
            chip.setGravity(Gravity.CENTER);
            chip.setSingleLine(true);
            chip.setPadding(LauncherUiUtils.dp(this, 12), LauncherUiUtils.dp(this, 10),
                    LauncherUiUtils.dp(this, 12), LauncherUiUtils.dp(this, 10));
            chip.setBackground(LauncherUiUtils.roundRect(this,
                    i == selected ? new int[]{Color.rgb(73, 99, 177), Color.rgb(73, 74, 157)}
                            : new int[]{Color.rgb(34, 41, 62), Color.rgb(28, 34, 51)},
                    LauncherUiUtils.dp(this, 12)));
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-2, LauncherUiUtils.dp(this, 40));
            cp.setMargins(LauncherUiUtils.dp(this, 2), 0, LauncherUiUtils.dp(this, 5), LauncherUiUtils.dp(this, 8));
            choicesRow.addView(chip, cp);
            chip.setOnClickListener(v -> onSelect.onSelect(value));
        }
        horizontal.addView(choicesRow, new android.widget.HorizontalScrollView.LayoutParams(-2, -2));
        panel.addView(horizontal, new LinearLayout.LayoutParams(-1, -2));
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

        addSection(content, "Home layout & icons");
        addChoiceRow(content, "Home columns", getHomeColumns() - 3, new String[]{"3", "4", "5", "6"}, value -> {
            prefs.edit().putInt(KEY_COLUMNS, value + 3).apply();
            clearIconPositions();
            currentPage = Math.min(currentPage, Math.max(0, (homePackages.size() - 1) / pageCapacity()));
            renderHomePages(false); showCustomizationMenu(dialog, scroll.getScrollY(), false);
        });
        addChoiceRow(content, "Home icon size", sizeChoiceIndex(prefs.getInt(KEY_ICON_SIZE, 48)), new String[]{"Small", "Medium", "Large", "XL"}, value -> {
            prefs.edit().putInt(KEY_ICON_SIZE, new int[]{54, 64, 74, 84}[value]).apply();
            renderHomePages(false); showCustomizationMenu(dialog, scroll.getScrollY(), false);
        });
        addSettingRow(content, "▣", "Rounded-square icon background",
                prefs.getBoolean("icon_rounded_square_background", false) ? "On" : "Off", () -> {
            prefs.edit().putBoolean("icon_rounded_square_background",
                    !prefs.getBoolean("icon_rounded_square_background", false)).apply();
            renderHomePages(false);
            if (drawerAdapter != null) drawerAdapter.notifyDataSetChanged();
            renderDock();
            showCustomizationMenu(dialog, scroll.getScrollY(), false);
        });
        addChoiceRow(content, "App icon background color",
                Math.max(0, Math.min(2, prefs.getInt(KEY_ICON_BACKGROUND_COLOR, 0))),
                new String[]{"Black", "White", "Grey"}, value -> {
            prefs.edit().putInt(KEY_ICON_BACKGROUND_COLOR, value)
                    .putBoolean("icon_rounded_square_background", true).apply();
            renderHomePages(false);
            if (drawerAdapter != null) drawerAdapter.notifyDataSetChanged();
            renderDock();
            // Rebuilding workspace pages reattaches the existing clock panels. Reapply
            // each panel's inset background after the new layout pass so the old measured
            // height cannot leave the translucent clock background clipped to half-height.
            final int restorePage = Math.max(0, Math.min(1, currentPage));
            for (int page = 0; page < clockPanelsByPage.length; page++) {
                activateClockForPage(page);
                final int pageIndex = page;
                LinearLayout clockPanelForPage = clockPanel;
                if (clockPanelForPage != null) clockPanelForPage.post(() -> {
                    if (clockPanelForPage.getParent() == null) return;
                    int previousPage = Math.max(0, Math.min(1, activeClockPage));
                    activateClockForPage(pageIndex);
                    applyClockPanelColor(false);
                    activateClockForPage(previousPage);
                });
            }
            activateClockForPage(restorePage);
            showCustomizationMenu(dialog, scroll.getScrollY(), false);
        });
        addChoiceRow(content, "App icon shape", prefs.getInt("app_icon_shape", 0),
                new String[]{"Original", "Triangle", "Pear"}, value -> {
            prefs.edit().putInt("app_icon_shape", value).apply();
            renderHomePages(false);
            if (drawerAdapter != null) drawerAdapter.notifyDataSetChanged();
            showCustomizationMenu(dialog, scroll.getScrollY(), false);
        });
        addChoiceRow(content, "Home app font size", Math.min(3, prefs.getInt(KEY_HOME_FONT_SIZE, 1)), new String[]{"Small", "Medium", "Large", "XL"}, value -> {
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

        addSection(content, "Clock");
        addSliderRow(content, "Clock background opacity", 0, 100,
                prefs.getInt(KEY_CLOCK_BG_OPACITY, 50), "%", value -> {
            prefs.edit().putInt(KEY_CLOCK_BG_OPACITY, value).apply();
            applyClockPanelColor(false);
        });
        addChoiceRow(content, "Clock visual style", prefs.getInt(KEY_CLOCK_STYLE, 0),
                new String[]{"Digital", "Cyber HUD", "Neon Circuit", "Tech Minimal", "Terminal Digital", "7-Segment Digital"}, value -> {
            prefs.edit().putInt(KEY_CLOCK_STYLE, value).apply();
            // A style change must be applied to both independent clock views.
            // updateClock() changes the displayed value, not the typography/renderer.
            int restorePage = Math.max(0, Math.min(1, currentPage));
            for (int page = 0; page < clockTextsByPage.length; page++) {
                activateClockForPage(page);
                applyClockStyle();
                applyClockPanelColor(false);
            }
            // Update the displayed text before measuring either clock. Measuring
            // with the previous style's text/font made the panel jump or clip when
            // switching between LED and standard typography.
            activateClockForPage(restorePage);
            updateClock();
            for (int page = 0; page < clockTextsByPage.length; page++) {
                activateClockForPage(page);
                applyClockPositionAndSize();
                applyClockPanelColor(false);
            }
            activateClockForPage(restorePage);
            showCustomizationMenu(dialog, scroll.getScrollY(), false);
        });

        addSection(content, "Page animations");
        addChoiceRow(content, "Page transition effect", getPageTransition(),
                new String[]{"3D Wheel", "3D Cube", "Parallax", "Page Flip"}, value -> {
            prefs.edit().putInt(KEY_PAGE_TRANSITION, value).apply();
            showCustomizationMenu(dialog, scroll.getScrollY(), false);
        });
        addSliderRow(content, "Wheel rotation angle", 50, 180, getPageRotationDegrees(), "°", value -> {
            prefs.edit().putInt(KEY_PAGE_ROTATION, value).apply();
        });
        // Display a user-facing scale offset by 40 degrees; keep the stored animation
        // angle unchanged. Thus the existing internal 130° appears as 90° in Home Studio.
        addMappedSliderRow(content, "Cube rotation angle", 0, 140,
                Math.max(0, Math.min(140, getCubeRotationDegrees() - 40)), "°", value -> {
                    prefs.edit().putInt(KEY_CUBE_ROTATION, value + 40).apply();
                });
        addChoiceRow(content, "Animation speed", prefs.getInt(KEY_ANIMATION_SPEED, 1), new String[]{"Slow", "Medium", "Fast"}, value -> {
            prefs.edit().putInt(KEY_ANIMATION_SPEED, value).apply();
            showCustomizationMenu(dialog, scroll.getScrollY(), false);
        });

        addSection(content, "App drawer");
        addChoiceRow(content, "Drawer opening animation", getDrawerTransition(),
                new String[]{"Current", "3D Card Flip", "Depth Slide", "Rotating Carousel"}, value -> {
            prefs.edit().putInt(KEY_DRAWER_TRANSITION, value).apply();
            showCustomizationMenu(dialog, scroll.getScrollY(), false);
        });
        addSliderRow(content, "Drawer background opacity", 0, 100,
                prefs.getInt(KEY_DRAWER_OPACITY, 100), "%", value -> {
            prefs.edit().putInt(KEY_DRAWER_OPACITY, value).apply();
            if (drawerScreen != null) drawerScreen.setBackground(drawerBackground());
        });
        addChoiceRow(content, "Drawer columns", getDrawerColumns() - 3, new String[]{"3", "4", "5", "6"}, value -> {
            prefs.edit().putInt(KEY_DRAWER_COLUMNS, value + 3).apply();
            drawerGrid.setNumColumns(getDrawerColumns()); drawerAdapter.notifyDataSetChanged();
            showCustomizationMenu(dialog, scroll.getScrollY(), false);
        });
        addChoiceRow(content, "Drawer icon size", sizeChoiceIndex(prefs.getInt(KEY_DRAWER_ICON_SIZE, 48)), new String[]{"Small", "Medium", "Large", "XL"}, value -> {
            prefs.edit().putInt(KEY_DRAWER_ICON_SIZE, new int[]{54, 64, 74, 84}[value]).apply();
            drawerAdapter.notifyDataSetChanged(); showCustomizationMenu(dialog, scroll.getScrollY(), false);
        });
        addChoiceRow(content, "App drawer font size", Math.min(3, prefs.getInt(KEY_DRAWER_FONT_SIZE, 1)), new String[]{"Small", "Medium", "Large", "XL"}, value -> {
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

    private int getCubeRotationDegrees() {
        return Math.max(30, Math.min(180,
                prefs == null ? 90 : prefs.getInt(KEY_CUBE_ROTATION, 90)));
    }

    private int getPageRotationDegrees() {
        return Math.max(50, Math.min(180, prefs == null ? 90 : prefs.getInt(KEY_PAGE_ROTATION, 90)));
    }

    private int getClockLength() {
        int fallback = activeClockPage == 0 && prefs != null
                ? prefs.getInt(KEY_CLOCK_LENGTH, 100) : 100;
        return Math.max(80, Math.min(250, prefs == null ? 100
                : prefs.getInt(clockPrefKey(KEY_CLOCK_LENGTH, activeClockPage), fallback)));
    }

    private int getClockBreadth() {
        int fallback = activeClockPage == 0 && prefs != null
                ? prefs.getInt(KEY_CLOCK_BREADTH, 100) : 100;
        return Math.max(80, Math.min(250, prefs == null ? 100
                : prefs.getInt(clockPrefKey(KEY_CLOCK_BREADTH, activeClockPage), fallback)));
    }

    private int getClockPosition() {
        int fallback = activeClockPage == 0 && prefs != null
                ? prefs.getInt(KEY_CLOCK_POSITION, 2) : 2;
        return Math.max(0, Math.min(2, prefs == null ? 2
                : prefs.getInt(clockPrefKey(KEY_CLOCK_POSITION, activeClockPage), fallback)));
    }

    private float getEffectiveClockLengthScale() {
        float requested = getClockLength() / 100f;
        if (clockPanel == null || clockPanel.getWidth() <= 0) return requested;
        int available = Math.max(LauncherUiUtils.dp(this, 96),
                getResources().getDisplayMetrics().widthPixels - LauncherUiUtils.dp(this, 24));
        return Math.min(requested, available / (float) clockPanel.getWidth());
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

        final int alignment = clockGravity();
        final float requestedLengthScale = getClockLength() / 100f;
        final float breadthScale = getClockBreadth() / 100f;
        final int screenWidth = getResources().getDisplayMetrics().widthPixels;
        final boolean sevenSegment = clockText instanceof SevenSegmentClockTextView
                && prefs.getInt(KEY_CLOCK_STYLE, 0) == 5;
        // Establish the selected preset's measurement font before measuring any width.
        // Otherwise switching away from seven-segment measures one frame with its old font.
        clockText.setTextSize(sevenSegment ? 110f : 78f);

        // The 7-segment view draws its own glyphs; Paint.measureText() does not
        // describe those glyphs and made its container too narrow. Calculate the
        // renderer's actual natural width from its segment geometry instead.
        float naturalClockWidth;
        if (clockText instanceof SevenSegmentClockTextView
                && prefs.getInt(KEY_CLOCK_STYLE, 0) == 5) {
            String value = clockText.getText() == null ? "" : clockText.getText().toString();
            int digits = 0;
            int separators = 0;
            int firstHourDigits = 0;
            boolean stillReadingHour = true;
            for (int i = 0; i < value.length(); i++) {
                char ch = value.charAt(i);
                if (Character.digit(ch, 10) >= 0) {
                    digits++;
                    if (stillReadingHour) firstHourDigits++;
                } else if (!Character.isWhitespace(ch)) {
                    separators++;
                    stillReadingHour = false;
                }
            }
            // The LED renderer reserves an invisible leading cell for 1–9 hour
            // values. Include that cell in the measured panel width too.
            if (firstHourDigits == 1 && separators > 0) digits++;
            // The seven-segment renderer's natural width must not depend on
            // the user's vertical breadth setting. Otherwise the top/bottom grips
            // indirectly change panel width because this width is later scaled by
            // lengthScale. Vertical resizing changes rowHeight only.
            float clockHeight = LauncherUiUtils.dp(this, 114)
                    - clockText.getPaddingTop() - clockText.getPaddingBottom();
            naturalClockWidth = Math.max(1f,
                    (digits * 0.72f + separators * 0.34f) * clockHeight * 0.91f
                    + clockText.getPaddingLeft() + clockText.getPaddingRight());
        } else {
            // Measure a stable template, not the currently displayed digits.
            // The clock is refreshed each minute; proportional fonts give digits
            // different widths, and one-digit/two-digit hours can change the
            // measured panel width. A fixed 23:58:58 sample preserves the active
            // locale's separators and AM/PM formatting while reserving the same
            // width throughout the day.
            Calendar widthSample = Calendar.getInstance();
            widthSample.set(Calendar.HOUR_OF_DAY, 23);
            widthSample.set(Calendar.MINUTE, 58);
            widthSample.set(Calendar.SECOND, 58);
            widthSample.set(Calendar.MILLISECOND, 0);
            String widthTemplate = DateFormat.getTimeFormat(this).format(widthSample.getTime());
            if (!DateFormat.is24HourFormat(this)) {
                String marker = new SimpleDateFormat("a", Locale.getDefault())
                        .format(widthSample.getTime());
                int markerAt = widthTemplate.toUpperCase(Locale.getDefault())
                        .lastIndexOf(marker.toUpperCase(Locale.getDefault()));
                if (markerAt >= 0) {
                    widthTemplate = (widthTemplate.substring(0, markerAt)
                            + widthTemplate.substring(markerAt + marker.length())).trim();
                }
            }
            StringBuilder widestDigits = new StringBuilder(widthTemplate.length());
            for (int i = 0; i < widthTemplate.length(); i++) {
                char ch = widthTemplate.charAt(i);
                widestDigits.append(Character.digit(ch, 10) >= 0 ? '8' : ch);
            }
            naturalClockWidth = Math.max(1f,
                    clockText.getPaint().measureText(widestDigits.toString()));
        }
        // Keep the clock glyph inside the available clock-panel width. The AM/PM
        // marker and small side insets share that width, so long clocks shrink
        // automatically instead of drawing beyond the background.
        final int markerWidth = amPmText == null ? 0 : LauncherUiUtils.dp(this, 22);
        final int availablePanelWidth = Math.max(LauncherUiUtils.dp(this, 96),
                screenWidth - LauncherUiUtils.dp(this, 24));
        final int dateWidth = dateText == null ? 0
                : (int) Math.ceil(dateText.getPaint().measureText(dateText.getText().toString()));
        // Measure the panel at its natural width, then scale the complete panel.
        // This keeps the background, digits, AM/PM marker, and date in one resize
        // operation instead of shrinking only the clock glyphs inside a fixed panel.
        final int clockWithMarkerWidth = (int) Math.ceil(naturalClockWidth)
                + markerWidth + LauncherUiUtils.dp(this, 1);
        final int contentWidth = Math.max(clockWithMarkerWidth, dateWidth)
                + LauncherUiUtils.dp(this, 4);
        final int naturalPanelWidth = Math.max(LauncherUiUtils.dp(this, 96),
                contentWidth + LauncherUiUtils.dp(this, 8));
        // The clock's complete visual bounds must always fit inside this page.
        // Keep the requested size when it fits; if the user chooses an oversized
        // length, cap the rendered scale to the available page width.
        final float lengthScale = Math.min(requestedLengthScale,
                Math.max(0.01f, availablePanelWidth / (float) naturalPanelWidth));
        // Let the real clock panel height grow with the breadth setting. Scaling only
        // the drawing left its background short and caused the clock to look detached.
        final int naturalRowHeight = LauncherUiUtils.dp(this, 114);
        final int rowHeight = Math.max(1, Math.round(naturalRowHeight * breadthScale));

        clockPanel.setGravity(alignment);
        clockPanel.setPivotY(0f);
        // Keep the scaled clock visually above workspace icons and shortcuts.
        // Its actual hit bounds stay at the fixed layout size, so overlapping
        // visuals do not steal touches from the shortcut underneath.
        clockPanel.setElevation(LauncherUiUtils.dp(this, 8));
        // Resize the actual panel downward with the clock instead of scaling a
        // fixed-height background. The top edge stays anchored in place.
        clockPanel.setScaleY(1f);
        ViewGroup.LayoutParams rawPanelParams = clockPanel.getLayoutParams();
        FrameLayout.LayoutParams panelParams = rawPanelParams instanceof FrameLayout.LayoutParams
                ? (FrameLayout.LayoutParams) rawPanelParams
                : new FrameLayout.LayoutParams(naturalPanelWidth, rowHeight, alignment | Gravity.TOP);
        panelParams.gravity = alignment | Gravity.TOP;
        panelParams.width = naturalPanelWidth;
        clockPanel.setLayoutParams(panelParams);
        // Scale from the selected alignment anchor so left, center, and right
        // positioning remain visually stable while changing the clock length.
        clockPanel.setPivotX(alignment == Gravity.RIGHT ? panelParams.width
                : (alignment == Gravity.CENTER_HORIZONTAL ? panelParams.width / 2f : 0f));
        clockPanel.setScaleX(lengthScale);
        clockPanel.post(() -> {
            if (clockPanel == null) return;
            // Re-clamp after measuring/layout so the final scaled visual rectangle,
            // not the previous frame's dimensions, determines both screen edges.
            float[] bounded = clampClockTranslation(clockPanel,
                    clockPanel.getTranslationX(), clockPanel.getTranslationY());
            setClampedClockTranslation(bounded[0], bounded[1], true);
            updateClockResizeHandles();
        });
        clockPanel.setPadding(LauncherUiUtils.dp(this, 4), LauncherUiUtils.dp(this, 2),
                LauncherUiUtils.dp(this, 4), LauncherUiUtils.dp(this, 2));
        clockPanel.setClipChildren(false);
        clockPanel.setClipToPadding(false);

        if (clockRow != null) {
            ViewGroup.LayoutParams rowParams = clockRow.getLayoutParams();
            if (rowParams != null) {
                rowParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
                rowParams.height = rowHeight;
                clockRow.setLayoutParams(rowParams);
            }
            clockRow.setClipChildren(false);
            clockRow.setClipToPadding(false);
        }

        if (clockGroup != null) {
            if (amPmText != null) {
                FrameLayout.LayoutParams markerParams =
                        (FrameLayout.LayoutParams) amPmText.getLayoutParams();
                // Shift AM/PM 5dp right of the background edge for typography
                // clocks only; keep the seven-segment placement exactly as before.
                markerParams.rightMargin = sevenSegment ? 0 : -LauncherUiUtils.dp(this, 5);
                amPmText.setLayoutParams(markerParams);
            }
            FrameLayout.LayoutParams groupParams =
                    (FrameLayout.LayoutParams) clockGroup.getLayoutParams();
            groupParams.gravity = alignment | Gravity.TOP;
            groupParams.width = contentWidth;
            groupParams.height = rowHeight;
            clockGroup.setLayoutParams(groupParams);
            clockGroup.setClipChildren(false);
            clockGroup.setClipToPadding(false);
        }

        // Keep the glyph view at the left edge of its content group. The group
        // itself is aligned as a unit, leaving a dedicated slot to the right for AM/PM.
        // Non-seven-segment glyphs are vertically centered in the clock row so
        // their visual center aligns with the shortened, vertically centered background.
        clockText.setGravity(Gravity.LEFT | (sevenSegment ? Gravity.TOP : Gravity.CENTER_VERTICAL));
        // Use geometry derived from the current layout calculation, not getWidth()/getHeight()
        // from the previous frame. Those dimensions can be stale while the user resizes the
        // panel, causing scaled digits to jump or drift until the next layout pass.
        clockText.setPivotX(sevenSegment ? contentWidth / 2f : 0f);
        // The LED renderer sizes its segments from its measured view height.
        // Keep that view at the natural 114dp height and scale the rendered digits
        // by breadthScale explicitly; otherwise layout/background breadth changes
        // while the LED glyph view can retain stale measured bounds during a drag.
        clockText.setPivotY(sevenSegment ? 0f : rowHeight / 2f);
        // Normalize non-seven-segment glyph height across typography presets.
        // Typeface choices have different cap-height metrics at the same 78sp,
        // which otherwise makes some styles look smaller/larger. Use the default
        // sans-serif-light preset as the visual-height reference while preserving
        // each style's own typeface, weight, letter spacing, and shadow.
        // Keep non-LED glyphs within the same background bounds at startup.
        // The old unconditional 1.30x glyph scale made the digits exceed the
        // panel height before the user resized the clock.
        float normalizedNonLedHeightScale = 1.0f;
        if (!sevenSegment) {
            android.graphics.Paint referencePaint = new android.graphics.Paint(clockText.getPaint());
            referencePaint.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
            android.graphics.Paint.FontMetrics referenceMetrics = referencePaint.getFontMetrics();
            android.graphics.Paint.FontMetrics currentMetrics = clockText.getPaint().getFontMetrics();
            float referenceHeight = referenceMetrics.descent - referenceMetrics.ascent;
            float currentHeight = currentMetrics.descent - currentMetrics.ascent;
            if (referenceHeight > 0f && currentHeight > 0f) {
                normalizedNonLedHeightScale *= referenceHeight / currentHeight;
            }
        }
        // Increase only the seven-segment glyph width by 0.2 cm. At 360 dpi,
        // 0.2 cm is 28.35 physical pixels, or about 12.6 dp. Scale the glyph view
        // around its center so the panel/background, date, and AM/PM stay unchanged.
        float sevenSegmentExtraWidthDp = LauncherUiUtils.dp(this, 12.6f);
        float sevenSegmentWidthScale = sevenSegment
                ? (naturalClockWidth + sevenSegmentExtraWidthDp) / Math.max(1f, naturalClockWidth)
                : 1f;
        // Non-LED styles retain the same width treatment, now with consistent visual height.
        // Keep the text glyphs inside the width reserved by the background.
        // A 1.10x non-LED scale enlarged the digits after panel width was measured,
        // causing the background to look too narrow on initial layout.
        // Reduce the non-seven-segment glyph size by 20% from the previous
        // 1.4x setting, keeping the background dimensions unchanged.
        // The custom colon span remains vertically centered in the background.
        final float nonLedDigitScale = 1.12f;
        clockText.setScaleX(sevenSegment ? sevenSegmentWidthScale : nonLedDigitScale);
        // Keep the pivot tied to the newly calculated layout dimensions even during live resizing.
        clockText.setPivotX(sevenSegment ? contentWidth / 2f : 0f);
        clockText.setPivotY(sevenSegment ? 0f : rowHeight / 2f);
        // Extend non-seven-segment digits another 10 dp downward while keeping
        // their upper edge anchored to its existing position against the background.
        // The added scale is based on the fixed 114 dp clock row; a 5 dp translation
        // compensates for the centered pivot so the extra height goes downward.
        clockText.setScaleY(sevenSegment ? breadthScale
                : normalizedNonLedHeightScale * nonLedDigitScale
                        * (1.20f + 20f / 114f) * breadthScale);
        // Shift the seven-segment digits 15 dp left relative to their background.
        // Compensate for panel scaling so the visual offset remains exactly 15 dp
        // at every clock length setting; the panel/background itself stays put.
        clockText.setTranslationX(sevenSegment
                ? -LauncherUiUtils.dp(this, 15) / Math.max(0.01f, lengthScale) : 0f);
        // Keep the non-seven-segment upper edge fixed while the added glyph height
        // extends downward; preserve a net 5 dp upward offset from the original center.
        // The panel itself no longer uses scaleY, so this offset must remain a
        // fixed physical distance rather than being inversely scaled with breadth.
        clockText.setTranslationY(sevenSegment ? 0f : LauncherUiUtils.dp(this, 5));
        ViewGroup.LayoutParams rawClockParams = clockText.getLayoutParams();
        if (rawClockParams instanceof FrameLayout.LayoutParams) {
            FrameLayout.LayoutParams textParams = (FrameLayout.LayoutParams) rawClockParams;
            // Give the LED renderer the complete clock-group width. The colon is
            // centered against the actual clock background, not a narrower
            // wrap-content child containing only the digits.
            textParams.width = sevenSegment
                    ? ViewGroup.LayoutParams.MATCH_PARENT
                    : ViewGroup.LayoutParams.WRAP_CONTENT;
            // Keep the seven-segment renderer at its natural measured height
            // and scale the glyphs separately from the background. Non-LED clocks
            // continue to use the actual resized row height.
            textParams.height = sevenSegment ? naturalRowHeight : rowHeight;
            textParams.gravity = Gravity.LEFT | Gravity.TOP;
            clockText.setLayoutParams(textParams);
        }

        if (dateText != null) {
            // Center the date inside the clock panel regardless of the clock's
            // screen alignment; changing clock position must not push the date off-center.
            dateText.setGravity(Gravity.CENTER);
            ViewGroup.LayoutParams dateParams = dateText.getLayoutParams();
            if (dateParams != null) {
                dateParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
                dateText.setLayoutParams(dateParams);
            }
            // Use the same date size for every clock style. Counter-scale it so
            // panel resizing does not change the date's visual size.
            dateText.setTextSize(15f);
            // Only counter-scale horizontally for the panel's width transform.
            // The panel no longer scales vertically, so leave Y at 1 to preserve
            // the date's glyph proportions as clock breadth changes.
            dateText.setScaleX(1f / Math.max(0.01f, lengthScale));
            dateText.setScaleY(1f);
            dateText.setTranslationX(0f);
        }

        if (amPmText != null) {
            amPmText.setTextColor(clockText.getCurrentTextColor());
            amPmText.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
            // The panel itself scales horizontally and vertically, so keep the
            // marker's base size constant to avoid applying the resize twice.
            amPmText.setTextSize(9f);
            ViewGroup.LayoutParams markerRaw = amPmText.getLayoutParams();
            if (markerRaw instanceof FrameLayout.LayoutParams) {
                FrameLayout.LayoutParams markerParams = (FrameLayout.LayoutParams) markerRaw;
                markerParams.width = LauncherUiUtils.dp(this, 22);
                markerParams.height = LauncherUiUtils.dp(this, 12);
                markerParams.gravity = Gravity.LEFT | Gravity.TOP;
                // Anchor AM/PM to the final clock digit using the same rule for every
                // clock style. Compensate for the panel's horizontal scale so the marker
                // stays a fixed physical distance from the digit as the panel is resized.
                int cornerInset = LauncherUiUtils.dp(this, 2);
                int nonLedExtraRight = LauncherUiUtils.dp(this, 22);
                if (sevenSegment) {
                    // Preserve the existing LED marker position exactly.
                    markerParams.leftMargin = Math.max(0, (int) Math.ceil(naturalClockWidth)
                            - (int) Math.ceil((markerParams.width + cornerInset)
                                    / Math.max(0.01f, lengthScale)));
                } else {
                    // Keep the non-LED marker tied to the digit's layout position.
                    // Do not divide this anchor by the panel scale: inverse-scaled
                    // margins magnify small-slider changes and make AM/PM appear to
                    // race sideways as the clock gets smaller.
                    markerParams.leftMargin = Math.max(0, (int) Math.ceil(naturalClockWidth)
                            - markerParams.width - cornerInset + nonLedExtraRight);
                }
                // Follow the rendered digit bottom, not the fixed row bottom.
                // At short clock heights the non-LED glyph is vertically scaled around
                // its pivot, so a fixed row-based anchor makes AM/PM float away.
                float digitPivotY = clockText.getPivotY();
                float digitScaleY = clockText.getScaleY();
                float digitBottom = digitPivotY
                        + (rowHeight - digitPivotY) * digitScaleY
                        + clockText.getTranslationY();
                markerParams.topMargin = Math.max(0, Math.min(
                        Math.max(0, rowHeight - markerParams.height),
                        Math.round(digitBottom - markerParams.height
                                - LauncherUiUtils.dp(this, 1))));
                amPmText.setLayoutParams(markerParams);
            }
            // Counter-scale AM/PM as well as the date so neither font changes size
            // when the clock panel is resized. Keep offsets constant in screen pixels.
            // Counter-scale only the panel's horizontal transform. Its vertical
            // scale remains neutral so the AM/PM font shape never distorts; its
            // position is still recalculated above as the clock is resized.
            amPmText.setScaleX(1f / Math.max(0.01f, lengthScale));
            amPmText.setScaleY(1f);
            amPmText.setPivotX(0f);
            amPmText.setPivotY(0f);
            // The layout margins above are the single source of truth for placement;
            // style-specific translations made AM/PM drift away from the final digit.
            amPmText.setTranslationX(0f);
            amPmText.setTranslationY(0f);
        }
        clockText.invalidate();
    }

// Replacement span for non-seven-segment clocks: keep the colon's two dots
    // centered vertically in the clock row/background regardless of font preset.
    /**
     * A short clock background that never contributes padding to the widget.
     * This prevents background refreshes from shifting the clock on Android 6.
     */
    private static final class ClockPanelBackgroundDrawable extends Drawable {
        private final GradientDrawable gradient;
        private final float totalHeightReduction;

        ClockPanelBackgroundDrawable(GradientDrawable gradient, float totalHeightReduction) {
            this.gradient = gradient;
            this.totalHeightReduction = Math.max(0f, Math.min(0.8f, totalHeightReduction));
        }

        @Override public void draw(Canvas canvas) {
            android.graphics.Rect bounds = getBounds();
            int inset = Math.round(bounds.height() * totalHeightReduction / 2f);
            gradient.setBounds(bounds.left, bounds.top + inset, bounds.right, bounds.bottom - inset);
            gradient.draw(canvas);
        }

        @Override protected void onBoundsChange(android.graphics.Rect bounds) {
            invalidateSelf();
        }

        @Override public void setAlpha(int alpha) {
            gradient.setAlpha(alpha);
            invalidateSelf();
        }

        @Override public void setColorFilter(ColorFilter colorFilter) {
            gradient.setColorFilter(colorFilter);
            invalidateSelf();
        }

        @Override public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }

        @Override public boolean getPadding(android.graphics.Rect padding) {
            padding.set(0, 0, 0, 0);
            return false;
        }
    }

    private static final class CenteredColonSpan extends android.text.style.ReplacementSpan {
        private final float rowCenterY;

        CenteredColonSpan(float rowCenterY) {
            this.rowCenterY = rowCenterY;
        }

        @Override
        public int getSize(Paint paint, CharSequence text, int start, int end,
                Paint.FontMetricsInt fm) {
            // Compact the colon's reserved advance so the neighboring hour/minute
            // glyphs sit closer without changing their own typography or scale.
            return Math.max(1, Math.round(paint.measureText(":") * 0.45f));
        }

        @Override
        public void draw(Canvas canvas, CharSequence text, int start, int end,
                float x, int top, int y, int bottom, Paint paint) {
            Paint dots = new Paint(paint);
            dots.setStyle(Paint.Style.FILL);
            float naturalWidth = paint.measureText(":");
            float radius = Math.max(1f, paint.getTextSize() * 0.045f);
            // Keep the two dots centered in the original colon slot while the
            // narrower advance pulls the next (minute) digit closer. A slight
            // left bias also tightens the gap on the hour side.
            float centerX = x + naturalWidth * 0.25f;
            // Anchor the dots to the actual 114 dp clock-row center, not the font
            // line bounds (which vary by typeface and can sit above the background center).
            // Parent resizing scales the row and this coordinate together.
            float centerY = rowCenterY;
            float gap = paint.getTextSize() * 0.18f;
            canvas.drawCircle(centerX, centerY - gap, radius, dots);
            canvas.drawCircle(centerX, centerY + gap, radius, dots);
        }
    }

    private interface IntSliderAction { void onChange(int value); }

    private void addMappedSliderRow(LinearLayout panel, String title, int min, int max, int current,
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
        // Match the nearest actual preset. Threshold-based legacy mapping caused
        // Large (64 dp) to be highlighted as XL and treated the default 96 dp
        // fallback as XL even when no size had ever been selected.
        int[] sizes = {54, 64, 74, 84};
        int bestIndex = 0;
        int bestDistance = Math.abs(size - sizes[0]);
        for (int i = 1; i < sizes.length; i++) {
            int distance = Math.abs(size - sizes[i]);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    private void applyBrandVisibility() {
        if (brandText != null) brandText.setVisibility(prefs.getBoolean(KEY_SHOW_BRAND, true) ? View.VISIBLE : View.GONE);
    }
}
