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
    private static final String KEY_FONT_SIZE_SLIDER_VALUES = "font_size_slider_values_v1";
    private static final String KEY_CLOCK_STYLE = "clock_style";
    private static final String KEY_CLOCK_DATE_SIZE = "clock_date_size_sp";
    private static final String KEY_CLOCK_PM_SIZE = "clock_pm_size_sp";
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
    private static final String KEY_SHORTCUT_ICON_SIZE_DP = "contact_shortcut_icon_size_dp";
    private static final String KEY_SHORTCUT_X = "contact_shortcut_x";
    private static final String KEY_SHORTCUT_Y = "contact_shortcut_y";
    private static final String KEY_SHORTCUT_GRID_POSITION = "contact_shortcut_grid_position_v1";
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
    private static final long ICON_EDGE_SWITCH_DELAY_MS = 200L;
    private static final int ICON_EDGE_SWITCH_ZONE_DP = 24;
    private interface IntAction { void onSelect(int value); }
    private final int BG = Color.rgb(10, 13, 24);
    private final Handler handler = new Handler();
    private boolean clockUpdatesActive = false;
    private final java.util.Map<TextView, Integer> appliedClockStyles = new java.util.WeakHashMap<>();
    private PackageManager packageManager;
    private SharedPreferences prefs;
    private int homeStudioAccentColor = Color.rgb(119, 205, 255);
    private AppWidgetManager widgetManager;
    private AppWidgetHost widgetHost;
    private WidgetInteractionController widgetInteractionController;
    private HomePageController homePageController;
    private FrameLayout root;
    private LinearLayout homeScreen;
    private TextView brandText;
    private LinearLayout pagesStrip;
    private final View[] workspaceGridOverlays = new View[2];
    // Cross-screen drag emits touch updates faster than the display can redraw.
    // Coalesce grid-overlay invalidation to one request per rendered frame.
    private boolean workspaceGridRefreshScheduled = false;
    private final Runnable workspaceGridRefreshFrame = new Runnable() {
        @Override public void run() {
            workspaceGridRefreshScheduled = false;
            for (View overlay : workspaceGridOverlays) {
                if (overlay != null && overlay.getVisibility() == View.VISIBLE
                        && overlay.getParent() instanceof ViewGroup) {
                    overlay.invalidate();
                }
            }
        }
    };
    // Items temporarily displaced during a drag are restored as soon as the
    // moving item leaves their original occupied area.
    private final java.util.IdentityHashMap<View, int[]> liveDisplacedItems = new java.util.IdentityHashMap<>();
    // Final visual destination for each item displaced during the current gesture.
    // Kept separately from its original position so a quick drop can commit the
    // intended destination even if the 120 ms push animation has not finished.
    private final java.util.IdentityHashMap<View, int[]> liveDisplacementTargets =
            new java.util.IdentityHashMap<>();
    private static final class AppDisplacementAnimation {
        final View view;
        final int targetX;
        final int targetY;
        final int deltaX;
        final int deltaY;

        AppDisplacementAnimation(View view, int targetX, int targetY, int deltaX, int deltaY) {
            this.view = view;
            this.targetX = targetX;
            this.targetY = targetY;
            this.deltaX = deltaX;
            this.deltaY = deltaY;
        }
    }

    // App moves are staged for one live collision pass, then compatible shifts are
    // started as a single shared animation instead of a separate animator per icon.
    private final java.util.IdentityHashMap<View, int[]> pendingAppDisplacementTargets =
            new java.util.IdentityHashMap<>();
    private final java.util.IdentityHashMap<View, android.animation.ValueAnimator> appDisplacementAnimatorByView =
            new java.util.IdentityHashMap<>();

    // Last collision-free visual position for an actively dragged workspace item.
    // Hold the mover while a colliding neighbour animates away instead of letting
    // it visibly pass through the clock or another workspace item.
    private final java.util.IdentityHashMap<View, int[]> liveDragLastSafeGeometry =
            new java.util.IdentityHashMap<>();
    // Reused only during a single collision-planning pass. Entries are invalidated
    // whenever that pass cancels/re-targets an item's active displacement animation.
    private final java.util.IdentityHashMap<View, int[]> liveDragGeometryCache =
            new java.util.IdentityHashMap<>();
    // Relative icon/label bounds are stable during a single drag-planning pass. Caching
    // them avoids repeatedly measuring label text for every candidate target.
    private final java.util.IdentityHashMap<View, int[]> liveDragAppVisualBoundsCache =
            new java.util.IdentityHashMap<>();
    private boolean liveDragPlanningCacheActive;
    // Coalesce dense ACTION_MOVE streams into one collision-planning pass per display
    // frame. The view's geometry is still updated immediately; only expensive neighbour
    // planning is deferred. Drop/cancel paths flush or cancel this work synchronously.
    private FrameLayout pendingLiveDisplacementPage;
    private View pendingLiveDisplacementMover;
    private final Runnable liveDisplacementFrame = new Runnable() {
        @Override public void run() {
            FrameLayout page = pendingLiveDisplacementPage;
            View mover = pendingLiveDisplacementMover;
            pendingLiveDisplacementPage = null;
            pendingLiveDisplacementMover = null;
            if (page != null && mover != null && mover.getParent() == page) {
                updateLiveDisplacements(page, mover);
            }
        }
    };
    private final java.util.WeakHashMap<View, Boolean> workspaceScaleRestored =
            new java.util.WeakHashMap<View, Boolean>();
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
    private int clockWidgetStartVisualX, clockWidgetStartVisualY;
    private int clockWidgetStartVisualWidth, clockWidgetStartVisualHeight;
    private float clockWidgetStartPanelScaleX = 1f;
    private boolean clockWidgetLongPressActivated = false;
    // 0 = none, 1 = left edge, 2 = right edge, 3 = top edge, 4 = bottom edge.
    private int clockResizeEdge = 0;
    // Invalidates stale layout callbacks when a newer resize frame or gesture starts.
    private int clockResizeGeneration = 0;
    // Invalidates stale post-layout clamps queued by earlier live resize frames.
    private final int[] clockLayoutGenerationByPage = new int[2];
    private boolean gestureStartedOnClock = false;
    // Locks the whole touch sequence against launcher swipes if it began while
    // clock move/edit mode was already active.
    private boolean clockGestureLockedAtDown = false;
    private TextView pageIndicator;
    private LauncherAppAdapter drawerAdapter;
    private List<ResolveInfo> allApps = new ArrayList<>();
    private List<ResolveInfo> drawerApps = new ArrayList<>();
    private final List<String> homePackages = new ArrayList<>();
    private final List<List<String>> pages = new ArrayList<>();
    private int currentPage = 0;
    private float gestureDownX, gestureDownY;
    private float drawerSwipeProgress = 0f;
    private float drawerSwipeStartProgress = 0f;
    private float drawerSwipeAnchorY = 0f;
    private boolean drawerTransitionInterruptedAtDown = false;
    private boolean drawerSwipeVisualActive = false;
    private boolean gestureStartedOnAppCell = false;
    private boolean gestureStartedOnWidget = false;
    private boolean gestureStartedOnContactShortcut = false;
    private boolean gestureStartedOnResizeHandle = false;
    // A first pull at the drawer's top arms closing; the next downward pull closes.
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
    private boolean workspaceIconReparenting = false;
    private boolean suppressResizeOutsideTouchStream = false;
    private float iconTouchRawX, iconTouchRawY;
    private float iconDragFingerOffsetY;
    private int iconOriginalLeft, iconOriginalTop;
    private int iconDragPageIndex = -1;
    private int iconOriginalPageIndex = -1;
    private int iconEdgeLatch = 0;
    private int iconEdgeHoverTargetPage = -1;
    private float iconDragLastRawX;
    private float iconDragLastRawY;
    private android.animation.ValueAnimator iconDragPageAnimator;
    private View iconDragParallaxView;
    private View iconDragParallaxOutgoing;
    private View iconDragParallaxIncoming;
    private int iconDragParallaxTargetPage = -1;
    private int iconDragParallaxDirection;
    private int iconDragParallaxPageWidth;
    private float iconDragParallaxStartTranslationX;
    private final java.util.IdentityHashMap<View, Float> iconDragParallaxChildAlphas =
            new java.util.IdentityHashMap<View, Float>();
    private final Runnable iconEdgeSwitchRunnable = new Runnable() {
        @Override public void run() {
            View dragged = activeDragView;
            int targetPage = iconEdgeHoverTargetPage;
            if (!iconDragging || dragged == null || pageViewport == null || pagesStrip == null
                    || targetPage < 0 || targetPage >= pagesStrip.getChildCount()) {
                iconEdgeHoverTargetPage = -1;
                return;
            }
            int currentDragPage = iconDragPageIndex;
            if (Math.abs(targetPage - currentDragPage) != 1
                    || pageViewport.getWidth() <= 0) {
                iconEdgeHoverTargetPage = -1;
                return;
            }
            int[] viewportLocation = new int[2];
            pageViewport.getLocationOnScreen(viewportLocation);
            int edge = LauncherUiUtils.dp(MainActivity.this, ICON_EDGE_SWITCH_ZONE_DP);
            boolean stillAtRequestedEdge = targetPage > currentDragPage
                    ? iconDragLastRawX >= viewportLocation[0] + pageViewport.getWidth() - edge
                    : iconDragLastRawX <= viewportLocation[0] + edge;
            if (!stillAtRequestedEdge) {
                iconEdgeHoverTargetPage = -1;
                return;
            }
            iconEdgeHoverTargetPage = -1;
            switchIconDragPage(targetPage);
        }
    };
    private View activeDragView;
    private String activeDragPackage;
    private View workspaceTransferItem;
    private FrameLayout workspaceTransferSourcePage;
    private View workspaceTransferProxy;
    private int workspaceTransferOriginPage = -1;
    private int workspaceTransferTargetPage = -1;
    private int workspaceTransferProxyPage = -1;
    private int workspaceTransferEdgeHoverTargetPage = -1;
    private float workspaceTransferLastRawX;
    private float workspaceTransferLastRawY;
    private float workspaceTransferFingerOffsetX;
    private float workspaceTransferFingerOffsetY;
    private android.animation.ValueAnimator workspaceTransferPageAnimator;
    private float workspaceTransferAnimationStartStripX;
    private float workspaceTransferAnimationEndStripX;
    private float workspaceTransferAnimationStartItemX;
    private int workspaceTransferAnimationTargetPage = -1;
    private Runnable iconArmDrag;
    private Runnable iconShowOptions;
    private final Runnable workspaceTransferEdgeSwitchRunnable = new Runnable() {
        @Override public void run() {
            View item = workspaceTransferItem;
            int targetPage = workspaceTransferEdgeHoverTargetPage;
            if (item == null || targetPage < 0 || pagesStrip == null || pageViewport == null
                    || workspaceTransferTargetPage < 0
                    || Math.abs(targetPage - workspaceTransferTargetPage) != 1
                    || targetPage >= pagesStrip.getChildCount()) {
                clearWorkspaceTransferEdgeHover();
                return;
            }
            int[] viewportLocation = new int[2];
            pageViewport.getLocationOnScreen(viewportLocation);
            int edge = LauncherUiUtils.dp(MainActivity.this, ICON_EDGE_SWITCH_ZONE_DP);
            boolean stillAtEdge = targetPage > workspaceTransferTargetPage
                    ? workspaceTransferLastRawX >= viewportLocation[0] + pageViewport.getWidth() - edge
                    : workspaceTransferLastRawX <= viewportLocation[0] + edge;
            if (!stillAtEdge) {
                clearWorkspaceTransferEdgeHover();
                return;
            }
            workspaceTransferEdgeHoverTargetPage = -1;
            switchWorkspaceTransferPage(targetPage);
        }
    };
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
            if (Intent.ACTION_PACKAGE_REPLACED.equals(action)) {
                // Reset bounded retries after every replacement. Provider metadata and
                // AppWidgetHostView state can be briefly unavailable during app updates.
                handler.removeCallbacks(widgetRestoreRetry);
                widgetRestoreRetryScheduled = false;
                widgetRestoreRetryCount = 0;
                handler.removeCallbacks(refreshClockGeometryAfterPackageChange);
                handler.postDelayed(refreshClockGeometryAfterPackageChange, 350L);
                return;
            }
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
    private static final int MAX_WIDGET_RESTORE_RETRIES = 8;
    private boolean widgetRestoreRetryScheduled = false;
    private int widgetRestoreRetryCount = 0;
    private final Runnable widgetRestoreRetry = () -> {
        widgetRestoreRetryScheduled = false;
        if (isFinishing() || widgetRestoreRetryCount >= MAX_WIDGET_RESTORE_RETRIES) return;
        widgetRestoreRetryCount++;
        restoreWidgetsOnPages();
    };
    private void scheduleWidgetRestoreRetry() {
        if (widgetRestoreRetryScheduled || widgetRestoreRetryCount >= MAX_WIDGET_RESTORE_RETRIES
                || isFinishing()) return;
        widgetRestoreRetryScheduled = true;
        handler.postDelayed(widgetRestoreRetry, 750L);
    }
    private final Runnable refreshClockGeometryAfterPackageChange = new Runnable() {
        @Override public void run() {
            if (clockPanelsByPage[0] != null && clockPanelsByPage[1] != null) {
                int restorePage = Math.max(0, Math.min(1, currentPage));
                updateClock();
                for (int page = 0; page < clockPanelsByPage.length; page++) {
                    if (clockPanelsByPage[page] == null) continue;
                    activateClockForPage(page);
                    applyClockStyle();
                    applyClockPositionAndSize();
                    applyClockPanelColor(false);
                }
                activateClockForPage(restorePage);
                updateClockResizeHandles();
            }
            // A provider update can temporarily invalidate an AppWidgetHostView. Re-run
            // restoration after the package manager has had time to publish new metadata.
            restoreWidgetsOnPages();
        }
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
        normalizeAppIconSettings();
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
                    @Override public void updateLiveDisplacements(FrameLayout page, View movingView) {
                        MainActivity.this.updateLiveDisplacements(page, movingView);
                    }
                    @Override public void scheduleLiveDisplacementUpdate(FrameLayout page, View movingView) {
                        MainActivity.this.scheduleLiveDisplacementUpdate(page, movingView);
                    }
                    @Override public void finishLiveDisplacements(boolean commit, View movingView) {
                        MainActivity.this.finishLiveDisplacements(commit, movingView);
                    }
                    @Override public void setWorkspaceGridVisible(boolean visible, View movingView) {
                        MainActivity.this.setWorkspaceGridVisible(visible, movingView);
                    }
                    @Override public void beginWorkspaceTransferDrag(View item, int sourcePage,
                            float rawX, float rawY, float fingerOffsetX, float fingerOffsetY) {
                        MainActivity.this.beginWorkspaceTransferDrag(item, sourcePage, rawX, rawY,
                                fingerOffsetX, fingerOffsetY);
                    }
                    @Override public void updateWorkspaceTransferEdge(float rawX, float rawY) {
                        MainActivity.this.updateWorkspaceTransferEdge(rawX, rawY);
                    }
                    @Override public int getWorkspaceTransferTargetPage() {
                        return MainActivity.this.getWorkspaceTransferTargetPage();
                    }
                    @Override public void updateWorkspaceTransferDisplacements(View item, float rawX,
                            float rawY, float fingerOffsetX, float fingerOffsetY) {
                        MainActivity.this.updateWorkspaceTransferDisplacements(item, rawX, rawY,
                                fingerOffsetX, fingerOffsetY);
                    }
                    @Override public int[] prepareWidgetDrop(FrameLayout targetPage, View widget,
                            float rawX, float rawY, float fingerOffsetX, float fingerOffsetY) {
                        return MainActivity.this.prepareWorkspaceWidgetDrop(targetPage, widget,
                                rawX, rawY, fingerOffsetX, fingerOffsetY);
                    }
                    @Override public void updatePlacedWidgetSize(int widgetId, FrameLayout container) {
                        MainActivity.this.updatePlacedWidgetSize(widgetId, container);
                    }
                    @Override public int finishWorkspaceTransferAnimation() {
                        return MainActivity.this.finishWorkspaceTransferAnimation();
                    }
                    @Override public void finishWorkspaceTransferDrag(View item, boolean commit) {
                        MainActivity.this.finishWorkspaceTransferDrag(item, commit);
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
        if (suppressResizeOutsideTouchStream) {
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                suppressResizeOutsideTouchStream = false;
            }
            return true;
        }
        if (action == MotionEvent.ACTION_DOWN) {
            boolean dismissedResizeMode = false;
            if (widgetInteractionController != null) {
                dismissedResizeMode |= widgetInteractionController.clearEditChromeOutsideTouch(
                        event.getRawX(), event.getRawY());
            }
            if (dismissedResizeMode) {
                suppressResizeOutsideTouchStream = true;
                return true;
            }
            clockGestureLockedAtDown = clockWidgetMoving || clockWidgetEditing;
            gestureDownX = event.getX();
            gestureDownY = event.getY();
            longPressDownX = event.getX();
            longPressDownY = event.getY();
            longPressTriggered = false;
            pageSwipeActive = false;
            pageSwipeChildCancelled = false;
            verticalSwipeClaimed = false;
            drawerCloseSwipeClaimed = false;
            drawerTransitionInterruptedAtDown =
                    LauncherScreenTransitions.isDrawerTransitionRunning(
                            homeScreen, drawerScreen, getDrawerTransition());
            drawerSwipeStartProgress = LauncherScreenTransitions.getDrawerTransitionProgress(
                    homeScreen, drawerScreen, getDrawerTransition(), drawerOpen);
            drawerSwipeAnchorY = event.getY();
            drawerSwipeProgress = drawerSwipeStartProgress;
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
            // A tap anywhere outside the clock cancels its 1-second move-ready
            // dance immediately, even before the 2-second edit mode has begun.
            if (!gestureStartedOnClock && clockWidgetMoving && !clockWidgetEditing) {
                handler.removeCallbacks(clockWidgetLongPress);
                handler.removeCallbacks(clockWidgetEditLongPress);
                showClockMoveBorder(false);
                clockWidgetMoving = false;
                clockWidgetLongPressActivated = false;
                setWorkspaceGridVisible(false, null);
            }
            if (clockWidgetEditing && !gestureStartedOnClock) {
                exitClockWidgetEditMode();
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
                    int[] startGeometry = itemGeometry(clockPanel);
                    clockWidgetStartVisualX = startGeometry[0];
                    clockWidgetStartVisualY = startGeometry[1];
                    clockWidgetStartVisualWidth = startGeometry[2];
                    clockWidgetStartVisualHeight = startGeometry[3];
                    clockResizeGeneration++;
                    clockWidgetStartX = clockPanel.getTranslationX();
                    clockWidgetStartY = clockPanel.getTranslationY();
                    setWorkspaceGridVisible(true, clockPanel);
                    return true;
                }
            handler.removeCallbacks(homeLongPress);
            handler.removeCallbacks(clockWidgetLongPress);
            handler.removeCallbacks(clockWidgetEditLongPress);
            // Clock edit/resize gestures must never be interpreted as workspace gestures.
            if (gestureStartedOnClock && !drawerOpen) {
                handler.postDelayed(clockWidgetLongPress, 1000L);
                handler.postDelayed(clockWidgetEditLongPress, 2000L);
            }
            if (!drawerOpen && !gestureStartedOnAppCell && !gestureStartedOnWidget
                    && !gestureStartedOnContactShortcut && !gestureStartedOnClock)
                handler.postDelayed(homeLongPress, 2000L);
            return super.dispatchTouchEvent(event);
        }

        float dx = event.getX() - gestureDownX;
        float dy = event.getY() - gestureDownY;

        // A second gesture during a settling Parallex transition takes over at the
        // currently rendered progress. A tap does not interrupt the animation.
        if (action == MotionEvent.ACTION_MOVE && drawerTransitionInterruptedAtDown) {
            boolean transitionVerticalSwipe = Math.abs(dy) > LauncherUiUtils.dp(this, 12)
                    && Math.abs(dy) > Math.abs(dx) * 1.15f;
            if (!drawerSwipeVisualActive
                    && !LauncherScreenTransitions.isDrawerTransitionRunning(
                            homeScreen, drawerScreen, getDrawerTransition())) {
                drawerTransitionInterruptedAtDown = false;
                drawerSwipeStartProgress = drawerOpen ? 1f : 0f;
                drawerSwipeProgress = drawerSwipeStartProgress;
            } else if (!drawerSwipeVisualActive && transitionVerticalSwipe) {
                drawerSwipeStartProgress = LauncherScreenTransitions.interruptDrawerTransition(
                        this, homeScreen, drawerScreen, getDrawerTransition(), drawerOpen);
                drawerSwipeAnchorY = event.getY();
                drawerSwipeProgress = drawerSwipeStartProgress;
                drawerSwipeVisualActive = true;
                verticalSwipeClaimed = true;
                handler.removeCallbacks(homeLongPress);
                handler.removeCallbacks(clockWidgetLongPress);
                handler.removeCallbacks(clockWidgetEditLongPress);
                MotionEvent cancel = MotionEvent.obtain(event);
                cancel.setAction(MotionEvent.ACTION_CANCEL);
                super.dispatchTouchEvent(cancel);
                cancel.recycle();
                return true;
            } else if (drawerSwipeVisualActive) {
                int swipeHeight = Math.max(1, pageViewport != null && pageViewport.getHeight() > 0
                        ? pageViewport.getHeight() : getResources().getDisplayMetrics().heightPixels);
                drawerSwipeProgress = Math.max(0f, Math.min(1f,
                        drawerSwipeStartProgress - (event.getY() - drawerSwipeAnchorY) / swipeHeight));
                LauncherScreenTransitions.updateDrawerSwipe(this, homeScreen, drawerScreen,
                        getDrawerTransition(), drawerSwipeProgress);
                return true;
            }
        }

        // While the drawer is open, let a qualifying downward pull close it on the
        // first gesture whenever that gesture begins at the top of the app list. Users
        // may start on the "My Apps" header as well as on a grid cell; requiring the
        // GridView itself as the touch origin made the first pull get ignored.
        if (drawerOpen && !drawerCloseSwipeClaimed) {
            boolean verticalSwipe = Math.abs(dy) > LauncherUiUtils.dp(this, 12)
                    && Math.abs(dy) > Math.abs(dx) * 1.15f;
            if (action == MotionEvent.ACTION_MOVE && drawerAtTopOnGestureDown
                    && dy > 0f && verticalSwipe) {
                drawerCloseSwipeClaimed = true;
                int swipeHeight = Math.max(1, pageViewport != null && pageViewport.getHeight() > 0
                        ? pageViewport.getHeight() : getResources().getDisplayMetrics().heightPixels);
                drawerSwipeProgress = Math.max(0f,
                        Math.min(1f, drawerSwipeStartProgress - dy / (float) swipeHeight));
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
            // Ordinary app-list gestures remain owned by GridView, untouched by the
            // launcher's page/clock recognizers. This matters most for long scrolls.
            if (gestureStartedOnDrawerGrid) return super.dispatchTouchEvent(event);
        }
        if (action == MotionEvent.ACTION_MOVE) {
            // A touch that begins on the clock must still be allowed to become a
            // normal launcher swipe. The clock's child listener consumes ACTION_DOWN,
            // so explicitly cancel that child stream when movement crosses the swipe
            // threshold, then let the activity-level drawer/page recognizers handle it.
            boolean clockSwipeThreshold = Math.abs(dx) > LauncherUiUtils.dp(this, 12)
                    || Math.abs(dy) > LauncherUiUtils.dp(this, 12);
            if (gestureStartedOnClock && !clockWidgetEditing && !clockWidgetMoving
                    && clockResizeEdge == 0 && clockSwipeThreshold) {
                handler.removeCallbacks(clockWidgetLongPress);
                handler.removeCallbacks(clockWidgetEditLongPress);
                if (clockWidgetMoving) {
                    showClockMoveBorder(false);
                    clockWidgetMoving = false;
                    setWorkspaceGridVisible(false, null);
                }
                gestureStartedOnClock = false;
                if (!pageSwipeChildCancelled) {
                    MotionEvent cancel = MotionEvent.obtain(event);
                    cancel.setAction(MotionEvent.ACTION_CANCEL);
                    super.dispatchTouchEvent(cancel);
                    cancel.recycle();
                    pageSwipeChildCancelled = true;
                }
            }
            // The one-second move-ready state is the ONLY state that can move the
            // clock. Edit mode is reserved for resizing/customization and must not
            // turn a normal touch into a drag.
            if (clockWidgetMoving && !clockWidgetEditing && clockPanel != null) {
                if (Math.abs(dx) > LauncherUiUtils.dp(this, 5)
                        || Math.abs(dy) > LauncherUiUtils.dp(this, 5)) {
                    if (!clockWidgetDragging) {
                        int[] safeClockGeometry = itemGeometry(clockPanel);
                        liveDragLastSafeGeometry.put(clockPanel,
                                new int[]{safeClockGeometry[0], safeClockGeometry[1]});
                    }
                    clockWidgetDragging = true;
                    handler.removeCallbacks(clockWidgetEditLongPress);
                    // Android translation values and raw touch coordinates are both pixels.
                    // Dividing the touch delta by density made the clock lag behind the finger.
                    float moveX = event.getRawX() - clockWidgetDownRawX;
                    float moveY = event.getRawY() - clockWidgetDownRawY;
                    setClampedClockTranslation(clockWidgetStartX + moveX,
                            clockWidgetStartY + moveY, true);
                    updateClockResizeHandles();
                    setWorkspaceGridVisible(true, clockPanel);
                    if (clockPanel.getParent() instanceof FrameLayout) {
                        scheduleLiveDisplacementUpdate((FrameLayout) clockPanel.getParent(), clockPanel);
                    }
                }
                return true;
            }
            if (clockResizeEdge != 0 && clockWidgetDragging) {
                // Use the widget resizer's pixel-space rules: page boundaries are hard
                // limits, the opposite edge stays fixed, and overlapping candidates are rejected.
                if (!(clockPanel.getParent() instanceof FrameLayout)) return true;
                FrameLayout activePage = (FrameLayout) clockPanel.getParent();
                float resizeDeltaX = event.getRawX() - clockWidgetDownRawX;
                float resizeDeltaY = event.getRawY() - clockWidgetDownRawY;
                int pageWidth = activePage.getWidth();
                int pageHeight = activePage.getHeight();
                if (pageWidth <= 0 || pageHeight <= 0) return true;

                int left = clockWidgetStartVisualX;
                int top = clockWidgetStartVisualY;
                int right = left + clockWidgetStartVisualWidth;
                int bottom = top + clockWidgetStartVisualHeight;
                int minW = Math.min(LauncherUiUtils.dp(this, 140), pageWidth);
                int minH = Math.min(LauncherUiUtils.dp(this, 90), pageHeight);
                // Horizontal grips must change width only. Do not force the height
                // back up to the vertical minimum when the user deliberately makes
                // a seven-segment clock very short.
                boolean horizontalResize = clockResizeEdge == 1 || clockResizeEdge == 2;
                if (clockResizeEdge == 1) {
                    left = Math.round(clockWidgetStartVisualX + resizeDeltaX);
                    left = Math.max(0, Math.min(left, right - minW));
                } else if (clockResizeEdge == 2) {
                    right = Math.round(clockWidgetStartVisualX + clockWidgetStartVisualWidth + resizeDeltaX);
                    right = Math.max(left + minW, Math.min(right, pageWidth));
                } else if (clockResizeEdge == 3) {
                    top = Math.round(clockWidgetStartVisualY + resizeDeltaY);
                    top = Math.max(0, Math.min(top, bottom - minH));
                } else if (clockResizeEdge == 4) {
                    bottom = Math.round(clockWidgetStartVisualY + clockWidgetStartVisualHeight + resizeDeltaY);
                    bottom = Math.max(top + minH, Math.min(bottom, pageHeight));
                }
                left = Math.max(0, Math.min(left, pageWidth - minW));
                right = Math.max(left + minW, Math.min(right, pageWidth));
                if (horizontalResize) {
                    // Preserve the exact vertical bounds for left/right resizing.
                    top = clockWidgetStartVisualY;
                    bottom = clockWidgetStartVisualY + clockWidgetStartVisualHeight;
                } else {
                    top = Math.max(0, Math.min(top, pageHeight - minH));
                    bottom = Math.max(top + minH, Math.min(bottom, pageHeight));
                }
                android.graphics.Rect candidate = new android.graphics.Rect(left, top, right, bottom);

                // Match the ordinary widget resizer: reject a resize frame if its
                // proposed rectangle intersects another workspace item. Do not apply
                // preferences for a rectangle that the widget system would reject.
                boolean overlapsWorkspaceItem = false;
                for (int i = 0; i < activePage.getChildCount(); i++) {
                    View other = activePage.getChildAt(i);
                    if (other == clockPanel || !(other.getTag() instanceof String)) continue;
                    String tag = (String) other.getTag();
                    if (!(tag.startsWith("launcher_widget:") || tag.startsWith("launcher_app:")
                            || "launcher_contact_shortcut".equals(tag)
                            || "launcher_clock_widget".equals(tag))) continue;
                    int[] otherGeometry = itemGeometry(other);
                    if (android.graphics.Rect.intersects(candidate,
                            collisionRect(other, otherGeometry))) {
                        overlapsWorkspaceItem = true;
                        break;
                    }
                }
                if (overlapsWorkspaceItem) return true;

                // The panel's measured width is its unscaled natural width; scaleX
                // is the saved length percentage. Convert the requested visible width
                // directly to that scale. Do not multiply the old preference by a ratio
                // of visible widths: when the old clock is capped at the page edge, its
                // visible width no longer represents the saved scale and shrinking stalls.
                int lengthPercent = Math.round((right - left) * 100f
                        / (float) Math.max(1, clockWidgetStartPanelWidth));
                int breadthPercent;
                // The visible panel height is the actual row height for every
                // clock style. Avoid style-specific proportional math: it drifted
                // after relayout and made top/bottom resizing behave differently.
                int naturalRowHeight = LauncherUiUtils.dp(this, 114);
                // A left/right grip is strictly a width operation. Preserve the
                // saved height percentage rather than recalculating it from the
                // visible rectangle, which can be shorter than the 80% floor and
                // would otherwise jump taller when a width drag reaches its limit.
                breadthPercent = horizontalResize ? clockWidgetStartBreadth
                        : Math.round((bottom - top) * 100f / Math.max(1, naturalRowHeight));
                // Match widget-resizer minimums in workspace pixels rather than
                // imposing an unrelated 80% natural-width floor on the clock.
                // The latter made left/right handles appear stuck on wide clocks.
                int minLengthPercent = Math.max(20, (int) Math.ceil(
                        LauncherUiUtils.dp(this, 140) * 100f
                                / Math.max(1, clockWidgetStartPanelWidth)));
                lengthPercent = Math.max(minLengthPercent, Math.min(600, lengthPercent));
                // All clock styles use the same real panel row height. Map its
                // target height directly to the natural 114dp row, just as a
                // widget resize maps its rectangle to layout width/height.
                int maxBreadthPercent = Math.max(80, Math.min(600, (int) Math.ceil(
                        pageHeight * 100f / Math.max(1, naturalRowHeight))));
                breadthPercent = Math.max(80, Math.min(maxBreadthPercent, breadthPercent));
                prefs.edit()
                        .putInt(clockPrefKey(KEY_CLOCK_LENGTH, activeClockPage), lengthPercent)
                        .putInt(clockPrefKey(KEY_CLOCK_BREADTH, activeClockPage), breadthPercent)
                        .apply();
                applyClockPositionAndSize();
                final int resizingEdge = clockResizeEdge;
                final int targetLeft = left;
                final int targetTop = top;
                final int targetRight = right;
                final int targetBottom = bottom;
                final int resizeGeneration = ++clockResizeGeneration;
                clockPanel.post(() -> {
                    if (resizeGeneration != clockResizeGeneration || clockPanel == null
                            || !(clockPanel.getParent() instanceof FrameLayout)) return;
                    int[] actual = itemGeometry(clockPanel);
                    float adjustX = 0f;
                    float adjustY = 0f;
                    if (resizingEdge == 1) {
                        // A left-edge resize must keep the RIGHT edge fixed for every
                        // clock style. Anchor using the measured visual rectangle after
                        // relayout, not the unscaled panel width; otherwise typography
                        // clocks drift and appear to resize from the wrong side.
                        adjustX = targetRight - (actual[0] + actual[2]);
                    } else if (resizingEdge == 2) {
                        // A right-edge resize keeps the LEFT edge fixed.
                        adjustX = targetLeft - actual[0];
                    }
                    if (resizingEdge == 3) adjustY = targetBottom - (actual[1] + actual[3]);
                    else if (resizingEdge == 4) adjustY = targetTop - actual[1];
                    setClampedClockTranslation(clockPanel.getTranslationX() + adjustX,
                            clockPanel.getTranslationY() + adjustY, true);
                    float[] bounded = clampClockTranslation(clockPanel,
                            clockPanel.getTranslationX(), clockPanel.getTranslationY());
                    setClampedClockTranslation(bounded[0], bounded[1], true);
                    if (clockPanel.getParent() instanceof FrameLayout) {
                        scheduleLiveDisplacementUpdate((FrameLayout) clockPanel.getParent(), clockPanel);
                    }
                    updateClockResizeHandles();
                });
                return true;
            }
            // Do not allow page/drawer/control-center gestures during clock move/edit
            // sessions. Move-ready dragging and edge resizing were handled above.
            // Keep the lock for this entire touch sequence even if an outside touch
            // has just dismissed edit mode.
            if (clockWidgetEditing || clockGestureLockedAtDown) return true;

            // In the drawer, only a downward pull starting at the very top closes it.
            // All other vertical gestures are left to the app grid for normal scrolling.
            boolean verticalSwipe = Math.abs(dy) > LauncherUiUtils.dp(this, 12)
                    && Math.abs(dy) > Math.abs(dx) * 1.15f;
            if (drawerCloseSwipeClaimed) {
                int swipeHeight = Math.max(1, pageViewport != null && pageViewport.getHeight() > 0
                        ? pageViewport.getHeight() : getResources().getDisplayMetrics().heightPixels);
                drawerSwipeProgress = Math.max(0f, Math.min(1f, drawerSwipeStartProgress - dy / (float) swipeHeight));
                LauncherScreenTransitions.updateDrawerSwipe(this, homeScreen, drawerScreen,
                        getDrawerTransition(), drawerSwipeProgress);
                return true;
            }
            // A held item gesture belongs to its move/resize controller, not workspace navigation.
            // Fast vertical swipes can still start on home items; explicit resize handles are reserved.
            long gestureDuration = event.getEventTime() - event.getDownTime();
            boolean heldItemGesture = ((gestureStartedOnAppCell || gestureStartedOnWidget)
                    && gestureDuration > 450L)
                    || (gestureStartedOnContactShortcut && gestureDuration >= 1000L);
            boolean itemManipulation = gestureStartedOnClock || gestureStartedOnResizeHandle || iconDragArmed || iconDragging
                    || heldItemGesture;
            if (!drawerOpen && !itemManipulation
                    && !verticalSwipeClaimed && verticalSwipe) {
                verticalSwipeClaimed = true;
                int swipeHeight = Math.max(1, pageViewport != null && pageViewport.getHeight() > 0
                        ? pageViewport.getHeight() : getResources().getDisplayMetrics().heightPixels);
                drawerSwipeProgress = Math.max(0f, Math.min(1f, drawerSwipeStartProgress - dy / (float) swipeHeight));
                if (dy < 0f) {
                    drawerSwipeVisualActive = true;
                    homeScreen.animate().cancel();
                    drawerScreen.animate().cancel();
                    LauncherScreenTransitions.updateDrawerSwipe(this, homeScreen, drawerScreen,
                            getDrawerTransition(), drawerSwipeProgress);
                }
                handler.removeCallbacks(homeLongPress);
                handler.removeCallbacks(clockWidgetLongPress);
                handler.removeCallbacks(clockWidgetEditLongPress);
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
                    drawerSwipeProgress = Math.max(0f, Math.min(1f, drawerSwipeStartProgress - dy / (float) swipeHeight));
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
                    && (pageSwipeActive || (!gestureStartedOnClock
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
            handler.removeCallbacks(clockWidgetEditLongPress);
            clockGestureLockedAtDown = false;
            if (clockWidgetMoving && !clockWidgetEditing) {
                if (clockWidgetDragging && clockPanel != null
                        && clockPanel.getParent() instanceof FrameLayout) {
                    finishLiveDisplacements(false, clockPanel);
                }
                clockWidgetDragging = false;
                clockWidgetMoving = false;
                showClockMoveBorder(false);
                if (clockPanel != null) {
                    clockPanel.setTranslationX(clockWidgetStartX);
                    clockPanel.setTranslationY(clockWidgetStartY);
                }
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
                boolean interruptedTransition = drawerTransitionInterruptedAtDown;
                drawerTransitionInterruptedAtDown = false;
                // Normal gestures keep the existing 10% threshold. A gesture that
                // takes over a running animation settles at the nearest endpoint.
                if (interruptedTransition) {
                    if (drawerSwipeProgress >= 0.50f) {
                        LauncherScreenTransitions.finishDrawerSwipeToDrawer(this, homeScreen, drawerScreen,
                                () -> drawerOpen = true, getDrawerTransition(), drawerSwipeProgress);
                    } else {
                        LauncherScreenTransitions.finishDrawerSwipeToHome(this, homeScreen, drawerScreen,
                                () -> drawerOpen = false, getDrawerTransition(), drawerSwipeProgress);
                    }
                } else if (drawerSwipeProgress >= 0.10f) {
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
            handler.removeCallbacks(clockWidgetEditLongPress);
            clockGestureLockedAtDown = false;
            if (clockWidgetMoving && !clockWidgetEditing) {
                if (clockWidgetDragging && clockPanel != null
                        && clockPanel.getParent() instanceof FrameLayout) {
                    finishLiveDisplacements(true, clockPanel);
                }
                clockWidgetDragging = false;
                clockWidgetMoving = false;
                showClockMoveBorder(false);
                if (clockPanel != null) {
                    prefs.edit()
                            .putFloat(clockPrefKey(KEY_CLOCK_WIDGET_X, activeClockPage), clockPanel.getTranslationX())
                            .putFloat(clockPrefKey(KEY_CLOCK_WIDGET_Y, activeClockPage), clockPanel.getTranslationY())
                            .apply();
                }
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
                boolean interruptedTransition = drawerTransitionInterruptedAtDown;
                drawerTransitionInterruptedAtDown = false;
                // Interrupted settles choose the nearest endpoint; ordinary short
                // opening swipes keep their established 10% threshold.
                if (interruptedTransition && drawerSwipeProgress < 0.50f) {
                    LauncherScreenTransitions.finishDrawerSwipeToHome(this, homeScreen, drawerScreen,
                            () -> drawerOpen = false, getDrawerTransition(), drawerSwipeProgress);
                } else if (interruptedTransition || drawerSwipeProgress >= 0.10f) {
                    LauncherScreenTransitions.finishDrawerSwipeToDrawer(this, homeScreen, drawerScreen,
                            () -> drawerOpen = true, getDrawerTransition(), drawerSwipeProgress);
                } else {
                    LauncherScreenTransitions.cancelDrawerSwipe(this, homeScreen, drawerScreen,
                            getDrawerTransition());
                }
                return true;
            }
            long releaseGestureDuration = event.getEventTime() - event.getDownTime();
            boolean itemManipulationOnRelease = gestureStartedOnClock || gestureStartedOnResizeHandle
                    || iconDragArmed || iconDragging
                    || ((gestureStartedOnAppCell || gestureStartedOnWidget)
                        && releaseGestureDuration > 450L)
                    || (gestureStartedOnContactShortcut && releaseGestureDuration >= 1000L);
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
        if (drawerGrid == null || drawerGrid.getVisibility() != View.VISIBLE
                || drawerGrid.getWidth() <= 0 || drawerGrid.getHeight() <= 0) return false;
        int[] location = new int[2];
        drawerGrid.getLocationOnScreen(location);
        // Test the grid's actual screen rectangle. Recursive hit testing can select a
        // transformed/overlaid child instead, which made some drawer scrolls fall back
        // to launcher-level gesture recognition.
        return rawX >= location[0] && rawX < location[0] + drawerGrid.getWidth()
                && rawY >= location[1] && rawY < location[1] + drawerGrid.getHeight();
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
        handler.removeCallbacks(clockWidgetEditLongPress);

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
        if (clockPanel.getParent() instanceof FrameLayout) {
            finishLiveDisplacements(!cancelled, clockPanel);
        }
        setWorkspaceGridVisible(false, null);
        updateClockResizeHandles();
    }

    private void updateClockResizeHandles() {
        if (clockPanel == null || root == null || clockPanel.getWidth() <= 0 || clockPanel.getHeight() <= 0) return;
        if (!(clockPanel.getParent() instanceof View)) return;
        // Use the exact same visual-rectangle calculation as widget collision,
        // clamping, and resize transactions. Mixing getLocationOnScreen() with
        // itemGeometry() caused the handles and the resize math to disagree when
        // a panel had pending layout params, a non-central pivot, or translation.
        View pageView = clockPanel.getParent() instanceof View
                ? (View) clockPanel.getParent() : null;
        if (pageView == null) return;
        int[] geometry = itemGeometry(clockPanel);
        int[] rootOnScreen = new int[2];
        int[] pageOnScreen = new int[2];
        root.getLocationOnScreen(rootOnScreen);
        pageView.getLocationOnScreen(pageOnScreen);
        int visualLeft = pageOnScreen[0] - rootOnScreen[0] + geometry[0];
        int visualTop = pageOnScreen[1] - rootOnScreen[1] + geometry[1];
        int w = Math.max(1, geometry[2]);
        int h = Math.max(1, geometry[3]);
        // Match the widget resizer: 18dp-thick grips, 40dp long.
        int side = LauncherUiUtils.dp(this, 40);
        int thin = LauncherUiUtils.dp(this, 18);
        int[][] positions = new int[][] {
                {visualLeft, visualTop + h / 2 - side / 2},
                {visualLeft + w - thin, visualTop + h / 2 - side / 2},
                {visualLeft + w / 2 - side / 2, visualTop},
                {visualLeft + w / 2 - side / 2, visualTop + h - thin}
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
        Object tag = view.getTag();
        boolean widgetContainer = tag instanceof String
                && ((String) tag).startsWith("launcher_widget:");
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        float localX = rawX - location[0];
        float localY = rawY - location[1];
        float hitWidth = view.getWidth();
        float hitHeight = view.getHeight();
        // Long-press scales a widget by 10%. Include the scaled visual bounds as well
        // as the ordinary container bounds so edge touches cannot become home long-presses.
        if (widgetContainer) {
            hitWidth *= Math.max(1f, Math.abs(view.getScaleX()));
            hitHeight *= Math.max(1f, Math.abs(view.getScaleY()));
        }
        if (localX < 0 || localY < 0 || localX >= hitWidth || localY >= hitHeight)
            return false;
        // The interaction grip covers the full widget container, including any area
        // that is not part of the provider's AppWidgetHostView child.
        if (widgetContainer || view instanceof AppWidgetHostView) return true;
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
            if (homeIntent) {
                // Normalize the cached workspace without running an entrance animation.
                showHomeImmediately();
                if (currentPage != 0) showPage(0, false);
                // HOME returns directly to the cached workspace without a launcher entrance animation.
            } else if (drawerOpen) {
                showHome();
            }
        }
    }

    @Override protected void onResume() {
        super.onResume();
        clockUpdatesActive = true;
        handler.removeCallbacks(clockUpdater);
        handler.post(clockUpdater);
        // Do not reconnect every AppWidgetHost synchronously inside onResume: on
        // Android 6 that can delay the first Home frame when returning from an app.
        // Let the cached workspace draw first, then resume provider listening.
        handler.postDelayed(() -> {
            if (!clockUpdatesActive || widgetHost == null) return;
            try { widgetHost.startListening(); } catch (Exception ignored) {}
        }, 500L);
        // The app list is loaded at startup and refreshed only by package add/remove broadcasts.
    }

    @Override protected void onPause() {
        // Leaving the launcher (opening an app or locking the screen) must never
        // leave clock widget edit grips/grid active when the launcher returns.
        if (clockWidgetEditing) exitClockWidgetEditMode();
        clockWidgetDragging = false;
        handler.removeCallbacks(clockWidgetLongPress);
        handler.removeCallbacks(clockWidgetEditLongPress);
        clockWidgetMoving = false;
        clockWidgetLongPressActivated = false;
        clockUpdatesActive = false;
        handler.removeCallbacks(clockUpdater);
        try { widgetHost.stopListening(); } catch (Exception ignored) {}
        super.onPause();
    }

    @Override protected void onDestroy() {
        cancelPendingLiveDisplacementUpdate();
        liveDragGeometryCache.clear();
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
                    @Override public int getHomeCellHeightDp(int iconSize) {
                        return MainActivity.this.homeCellHeightDp(iconSize);
                    }
                    @Override public int getHomeGridRowsForHeight(int pageHeightPx, int cellHeightPx) {
                        return MainActivity.this.homeGridRowsForHeight(pageHeightPx, cellHeightPx);
                    }
                    @Override public int getHomeGridRowTop(
                            int row, int rowCount, int pageHeightPx, int cellHeightPx) {
                        return MainActivity.this.homeGridRowTop(
                                row, rowCount, pageHeightPx, cellHeightPx);
                    }
                    @Override public ResolveInfo findApp(String packageName) { return MainActivity.this.findApp(packageName); }
                    @Override public void installHomeIconTouch(View cell, String packageName) { MainActivity.this.installHomeIconTouch(cell, packageName); }
                    @Override public void restoreWidgetsOnPages() { MainActivity.this.restoreWidgetsOnPages(); }
                    @Override public void syncWidgetViewsToPages() { MainActivity.this.syncWidgetViewsToPages(); }
                    @Override public void launchApp(ResolveInfo app, View sourceView) { MainActivity.this.launchApp(app, sourceView); }
                    @Override public void showAppOptions(ResolveInfo app, boolean onHome) { MainActivity.this.showAppOptions(app, onHome); }
                }, KEY_ICON_X, KEY_ICON_Y, KEY_SHOW_HOME_LABELS, KEY_ICON_SIZE, KEY_HOME_FONT_SIZE, KEY_SHOW_PAGE_INDICATOR, HOME_ROWS);
        renderHomePages(false);
        showHome();
        // The first clock text/layout pass must happen after the page viewport has
        // real dimensions. Otherwise a fresh install/update can leave the first
        // page's clock empty or measured at zero until Home Studio rebuilds it.
        root.post(() -> {
            if (isFinishing() || root == null || clockPanelsByPage[0] == null) return;
            updateClock();
            activateClockForPage(Math.max(0, Math.min(1, currentPage)));
            applyClockPositionAndSize();
            attachClockPanelsToPages();
            updateClockResizeHandles();
        });
    }

    private void setWorkspaceGridVisible(boolean visible, View movingView) {
        if (pagesStrip == null) return;
        // Do not leave full-page invisible overlays attached between edit gestures.
        // They are visual guides only, never workspace items or collision surfaces.
        if (!visible) {
            for (int i = 0; i < workspaceGridOverlays.length; i++) {
                View overlay = workspaceGridOverlays[i];
                if (overlay != null && overlay.getParent() instanceof ViewGroup) {
                    ((ViewGroup) overlay.getParent()).removeView(overlay);
                }
                workspaceGridOverlays[i] = null;
            }
            return;
        }
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
            int desiredVisibility = visible ? View.VISIBLE : View.GONE;
            if (overlay.getVisibility() != desiredVisibility) {
                overlay.setVisibility(desiredVisibility);
            }
            // Keep the overlay above workspace items and the active mover above the
            // overlay, but avoid reordering children when they are already in that order.
            if (movingView != null && movingView.getParent() == page) {
                int lastIndex = page.getChildCount() - 1;
                if (page.indexOfChild(overlay) != lastIndex - 1
                        || page.indexOfChild(movingView) != lastIndex) {
                    overlay.bringToFront();
                    movingView.bringToFront();
                }
            } else if (page.indexOfChild(overlay) != page.getChildCount() - 1) {
                overlay.bringToFront();
            }
        }
    }

    private void refreshWorkspaceGrid() {
        if (workspaceGridRefreshScheduled) return;
        if (pageViewport == null) {
            for (View overlay : workspaceGridOverlays) {
                if (overlay != null && overlay.getVisibility() == View.VISIBLE) overlay.invalidate();
            }
            return;
        }
        workspaceGridRefreshScheduled = true;
        pageViewport.postOnAnimation(workspaceGridRefreshFrame);
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
            int nextW = Math.max(1, cw);
            int nextH = Math.max(1, ch);
            if (cellW == nextW && cellH == nextH) return;
            cellW = nextW;
            cellH = nextH;
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
                        int rows = homeGridRowsForHeight(getHeight(), cellH);
                        int row = homeGridRowFromTop(top, rows, getHeight(), cellH);
                        top = homeGridRowTop(row, rows, getHeight(), cellH);
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
        // AM/PM is shown at the start of the date line, never over the clock digits.
        amPmText = null;
        FrameLayout.LayoutParams groupParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, LauncherUiUtils.dp(this, 114),
                clockGravity() | Gravity.CENTER_VERTICAL);
        clockRow.addView(clockGroup, groupParams);
        hero.addView(clockRow, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, LauncherUiUtils.dp(this, 114)));
        // Keep the existing 13sp date size; improve contrast instead of enlarging it.
        dateText = LauncherUiUtils.text(this, "", 13, Color.WHITE, true);
        dateText.setTypeface(Typeface.create("cursive", Typeface.NORMAL));
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
        // Keep the complete workspace strip in a hardware layer so page changes
        // transform cached pixels instead of rebuilding every icon/widget frame.
        // Android still redraws individual contents when a live widget invalidates.
        pagesStrip.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        pageViewport.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        pagesStrip.setClipChildren(true);
        pagesStrip.setClipToPadding(true);
        pageViewport.addView(pagesStrip, new FrameLayout.LayoutParams(-2, -1, Gravity.TOP | Gravity.LEFT));

        // Keep the page indicator available without reserving a 28dp band between
        // the workspace and dock. The home grid can now anchor its last row directly
        // to the bottom of the workspace; the indicator lives in the existing header.
        pageIndicator = LauncherUiUtils.text(this, "", 11, Color.rgb(210, 220, 255), false);
        pageIndicator.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, LauncherUiUtils.dp(this, 28));
        ip.leftMargin = LauncherUiUtils.dp(this, 8);
        ip.rightMargin = LauncherUiUtils.dp(this, 8);
        header.addView(pageIndicator, ip);
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

    /**
     * Height of the home label including a small clearance above and below the text.
     * This mirrors LauncherAppAdapter's font-size presets and uses scaled density so
     * accessibility font scaling is respected on Android 6 as well.
     */
    private int homeFontSizeSp() {
        return Math.max(8, Math.min(20,
                prefs == null ? 9 : prefs.getInt(KEY_HOME_FONT_SIZE, 9)));
    }

    private int homeLabelHeightDp() {
        float targetTextSize = homeFontSizeSp();
        android.graphics.Paint labelPaint = new android.graphics.Paint(
                android.graphics.Paint.ANTI_ALIAS_FLAG);
        labelPaint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        labelPaint.setTextSize(targetTextSize * getResources().getDisplayMetrics().scaledDensity);
        android.graphics.Paint.FontMetrics metrics = labelPaint.getFontMetrics();
        float density = Math.max(0.1f, getResources().getDisplayMetrics().density);
        int textHeightDp = (int) Math.ceil((metrics.descent - metrics.ascent) / density);
        return Math.max(12, textHeightDp + 2);
    }

    /**
     * One shared row metric for rendering, capacity, snapping, and collision placement.
     * With home labels enabled, extra height is added below the icon to contain the
     * selected font's full line height; the top inset and icon anchor stay unchanged.
     */
    private int homeCellHeightDp(int iconSize) {
        int baseHeight = Math.max(56, iconSize + 28);
        if (prefs == null || !prefs.getBoolean(KEY_SHOW_HOME_LABELS, true)) {
            return baseHeight;
        }
        int requiredHeight = 14 + iconSize + 1 + homeLabelHeightDp() + 7;
        return Math.max(baseHeight, requiredHeight);
    }

    private boolean isHomeGridBottomAligned() {
        return prefs != null && prefs.getBoolean(KEY_SHOW_HOME_LABELS, true);
    }

    /**
     * Keep a 25dp breathing space between the label-enabled home grid and the dock.
     * With labels disabled, preserve the previous workspace capacity exactly.
     */
    private int homeGridBottomGapPx() {
        return isHomeGridBottomAligned() ? LauncherUiUtils.dp(this, 25) : 0;
    }

    /** Returns the row capacity after reserving any label-enabled dock clearance. */
    private int homeGridRowsForHeight(int pageHeightPx, int cellHeightPx) {
        int usableHeight = Math.max(1, pageHeightPx - homeGridBottomGapPx());
        return Math.max(1, usableHeight / Math.max(1, cellHeightPx));
    }

    /** Returns a legal app-row top in page pixels, bottom-anchored only with labels on. */
    private int homeGridRowTop(int row, int rowCount, int pageHeightPx, int cellHeightPx) {
        int rows = Math.max(1, rowCount);
        int cellHeight = Math.max(1, cellHeightPx);
        int safeRow = Math.max(0, Math.min(rows - 1, row));
        int topInset = isHomeGridBottomAligned()
                ? Math.max(0, pageHeightPx - rows * cellHeight - homeGridBottomGapPx()) : 0;
        return topInset + safeRow * cellHeight;
    }

    /** Maps a pixel top coordinate back to the nearest legal row for the current mode. */
    private int homeGridRowFromTop(float topPx, int rowCount, int pageHeightPx, int cellHeightPx) {
        int rows = Math.max(1, rowCount);
        int cellHeight = Math.max(1, cellHeightPx);
        int topInset = isHomeGridBottomAligned()
                ? Math.max(0, pageHeightPx - rows * cellHeight - homeGridBottomGapPx()) : 0;
        return Math.max(0, Math.min(rows - 1,
                Math.round((topPx - topInset) / (float) cellHeight)));
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

        // AM/PM is displayed at the start of the date line, not over the digits.
        TextView marker = null;
        row.addView(group, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, LauncherUiUtils.dp(this, 114),
                Gravity.RIGHT | Gravity.CENTER_VERTICAL));
        panel.addView(row, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, LauncherUiUtils.dp(this, 114)));

        TextView date = LauncherUiUtils.text(this, "", 13, Color.WHITE, true);
        date.setTypeface(Typeface.create("cursive", Typeface.NORMAL));
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
        // A page rebuild can occur while the inactive clock still has stale measured
        // bounds. Recompute style, width and height for each panel after its page lays out.
        for (int pageIndex = 0; pageIndex < clockPanelsByPage.length; pageIndex++) {
            final int targetPage = pageIndex;
            final LinearLayout targetPanel = clockPanelsByPage[pageIndex];
            if (targetPanel == null) continue;
            targetPanel.post(() -> {
                if (targetPanel.getParent() == null
                        || clockPanelsByPage[targetPage] != targetPanel) return;
                int previousPage = Math.max(0, Math.min(1, activeClockPage));
                activateClockForPage(targetPage);
                applyClockStyle();
                applyClockPositionAndSize();
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
        // Validate after the controller's posted layout corrections and clock remeasurements.
        // This keeps icon-label, font, page-indicator, and other settings changes collision-safe.
        validateWorkspacePagesAfterLayout();
    }

    private static final String KEY_SHORTCUT_IDS = "contact_shortcut_ids";

    private String shortcutPrefKey(String base, String id) {
        return base + "_" + id;
    }

    private java.util.ArrayList<String> getSavedShortcutIds() {
        java.util.ArrayList<String> ids = new java.util.ArrayList<>();
        String stored = prefs.getString(KEY_SHORTCUT_IDS, "");
        if (stored != null && !stored.trim().isEmpty()) {
            for (String id : stored.split(",")) {
                if (!id.trim().isEmpty() && !ids.contains(id.trim())) ids.add(id.trim());
            }
        }
        // Migrate the previous single-shortcut format once, preserving its settings.
        if (ids.isEmpty()) {
            String legacyUri = prefs.getString(KEY_SHORTCUT_INTENT, null);
            if (legacyUri != null && !legacyUri.isEmpty()) {
                String id = "legacy";
                String[] keys = {KEY_SHORTCUT_INTENT, KEY_SHORTCUT_LABEL, KEY_SHORTCUT_PAGE,
                        KEY_SHORTCUT_ICON, KEY_SHORTCUT_SPAN_X, KEY_SHORTCUT_SPAN_Y,
                        KEY_SHORTCUT_WIDTH_DP, KEY_SHORTCUT_HEIGHT_DP, KEY_SHORTCUT_ICON_SIZE_DP,
                        KEY_SHORTCUT_X, KEY_SHORTCUT_Y, KEY_SHORTCUT_GRID_POSITION};
                android.content.SharedPreferences.Editor edit = prefs.edit();
                for (String key : keys) {
                    if (!prefs.contains(key)) continue;
                    Object value = prefs.getAll().get(key);
                    String scoped = shortcutPrefKey(key, id);
                    if (value instanceof String) edit.putString(scoped, (String) value);
                    else if (value instanceof Integer) edit.putInt(scoped, (Integer) value);
                    else if (value instanceof Float) edit.putFloat(scoped, (Float) value);
                }
                edit.putString(KEY_SHORTCUT_IDS, id).apply();
                ids.add(id);
            }
        }
        return ids;
    }

    private void saveShortcutIds(java.util.List<String> ids) {
        prefs.edit().putString(KEY_SHORTCUT_IDS, android.text.TextUtils.join(",", ids)).apply();
    }

    private void removeSavedShortcut(String id) {
        String[] keys = {KEY_SHORTCUT_INTENT, KEY_SHORTCUT_LABEL, KEY_SHORTCUT_PAGE,
                KEY_SHORTCUT_ICON, KEY_SHORTCUT_SPAN_X, KEY_SHORTCUT_SPAN_Y,
                KEY_SHORTCUT_WIDTH_DP, KEY_SHORTCUT_HEIGHT_DP, KEY_SHORTCUT_ICON_SIZE_DP,
                KEY_SHORTCUT_X, KEY_SHORTCUT_Y, KEY_SHORTCUT_GRID_POSITION};
        android.content.SharedPreferences.Editor edit = prefs.edit();
        for (String key : keys) edit.remove(shortcutPrefKey(key, id));
        java.util.ArrayList<String> ids = getSavedShortcutIds();
        ids.remove(id);
        saveShortcutIds(ids);
        edit.apply();
        String iconFile = "contact_shortcut_icon_" + id + ".png";
        try { deleteFile(iconFile); } catch (Exception ignored) {}
    }

    private void renderSavedContactShortcut() {
        if (pagesStrip == null || prefs == null || pageViewport == null) return;
        if (pageViewport.getWidth() <= 0 || pageViewport.getHeight() <= 0) {
            pageViewport.post(this::renderSavedContactShortcut);
            return;
        }

        // Keep already-rendered contact shortcuts attached during ordinary workspace
        // refreshes. Recreating them on every setting change could make them vanish
        // briefly, lose their touch/resize state, or silently choose a different page.
        java.util.List<String> shortcutIds = getSavedShortcutIds();
        java.util.HashSet<String> savedIds = new java.util.HashSet<String>(shortcutIds);
        java.util.HashSet<String> renderedIds = new java.util.HashSet<String>();
        final String prefix = "launcher_shortcut_id:";
        for (int pageIndex = 0; pageIndex < pagesStrip.getChildCount(); pageIndex++) {
            View pageView = pagesStrip.getChildAt(pageIndex);
            if (!(pageView instanceof ViewGroup)) continue;
            ViewGroup page = (ViewGroup) pageView;
            for (int childIndex = page.getChildCount() - 1; childIndex >= 0; childIndex--) {
                View child = page.getChildAt(childIndex);
                if (!"launcher_contact_shortcut".equals(child.getTag())) continue;
                CharSequence description = child.getContentDescription();
                String value = description == null ? "" : description.toString();
                String id = value.startsWith(prefix) ? value.substring(prefix.length()) : "";
                // Remove only deleted/invalid duplicate views, not valid saved shortcuts.
                if (id.isEmpty() || !savedIds.contains(id) || !renderedIds.add(id)) {
                    child.animate().cancel();
                    page.removeViewAt(childIndex);
                }
            }
        }

        // Create only shortcuts that do not already have a view. Placement of an
        // existing shortcut is controlled by an explicit drag, not by a redraw.
        for (String id : shortcutIds) {
            if (!renderedIds.contains(id)) renderSavedContactShortcut(id);
        }
    }

    private void renderSavedContactShortcut(String shortcutId) {
        if (pagesStrip == null || prefs == null) return;
        if (pageViewport == null) return;
        if (pageViewport.getWidth() <= 0 || pageViewport.getHeight() <= 0) {
            pageViewport.post(() -> renderSavedContactShortcut());
            return;
        }
        String savedUri = prefs.getString(shortcutPrefKey(KEY_SHORTCUT_INTENT, shortcutId), null);
        if (savedUri == null || savedUri.isEmpty() || pagesStrip.getChildCount() == 0) return;
        int desiredPage = Math.max(0, Math.min(pagesStrip.getChildCount() - 1,
                prefs.getInt(shortcutPrefKey(KEY_SHORTCUT_PAGE, shortcutId), 0)));
        // Contact shortcuts use four columns, but the real tile is exactly icon-sized.
        // Grid-slot coordinates are separate from the actual icon bounds.
        int columns = 4;
        int rows = getHomeGridRows();
        int cellW = Math.max(1, pageViewport.getWidth() / columns);
        int cellH = LauncherUiUtils.dp(this,
                homeCellHeightDp(prefs.getInt(KEY_ICON_SIZE, 48)));
        if (pageViewport.getWidth() <= 0 || pageViewport.getHeight() <= 0) return;

        FrameLayout chosenPage = null;
        int chosenLeft = 0, chosenTop = 0; // Logical grid-cell origin.
        float savedX = prefs.getFloat(shortcutPrefKey(KEY_SHORTCUT_X, shortcutId), -1f);
        float savedY = prefs.getFloat(shortcutPrefKey(KEY_SHORTCUT_Y, shortcutId), -1f);
        boolean savedAsGrid = prefs.getBoolean(
                shortcutPrefKey(KEY_SHORTCUT_GRID_POSITION, shortcutId), false);
        int lastSavedLeft = -1;
        int lastSavedTop = -1;

        if (savedX >= 0f && savedY >= 0f && desiredPage < pagesStrip.getChildCount()) {
            View savedPageView = pagesStrip.getChildAt(desiredPage);
            if (savedPageView instanceof FrameLayout) {
                FrameLayout savedPage = (FrameLayout) savedPageView;
                int savedCol;
                int savedRow;
                if (savedAsGrid) {
                    savedCol = Math.round(Math.min(1f, savedX) * Math.max(1, columns - 1));
                    savedRow = Math.round(Math.min(1f, savedY) * Math.max(1, rows - 1));
                } else {
                    // Convert the previous free-form normalized pixels using saved dimensions,
                    // then snap the old tile's center into the new four-column grid.
                    int legacyWidthDp = prefs.getInt(
                            shortcutPrefKey(KEY_SHORTCUT_WIDTH_DP, shortcutId), 0);
                    int legacyHeightDp = prefs.getInt(
                            shortcutPrefKey(KEY_SHORTCUT_HEIGHT_DP, shortcutId), 0);
                    int legacyWidth = legacyWidthDp > 0
                            ? LauncherUiUtils.dp(this, legacyWidthDp) : cellW;
                    int legacyHeight = legacyHeightDp > 0
                            ? LauncherUiUtils.dp(this, legacyHeightDp) : cellH;
                    int legacyLeft = Math.round(Math.min(1f, savedX)
                            * Math.max(1, pageViewport.getWidth() - legacyWidth));
                    int legacyTop = Math.round(Math.min(1f, savedY)
                            * Math.max(1, pageViewport.getHeight() - legacyHeight));
                    savedCol = Math.round((legacyLeft + legacyWidth / 2f) / cellW - 0.5f);
                    int estimatedRowTop = legacyTop + legacyHeight / 2 - cellH / 2;
                    savedRow = homeGridRowFromTop(estimatedRowTop, rows,
                            pageViewport.getHeight(), cellH);
                }
                savedCol = Math.max(0, Math.min(columns - 1, savedCol));
                savedRow = Math.max(0, Math.min(rows - 1, savedRow));
                int savedLeft = savedCol * cellW;
                int savedTop = homeGridRowTop(savedRow, rows, pageViewport.getHeight(), cellH);
                lastSavedLeft = savedLeft;
                lastSavedTop = savedTop;
                if (isContactShortcutPositionFree(savedPage, null, savedLeft, savedTop,
                        cellW, cellH, shortcutId)) {
                    chosenPage = savedPage;
                    chosenLeft = savedLeft;
                    chosenTop = savedTop;
                }
            }
        }

        if (chosenPage == null && desiredPage < pagesStrip.getChildCount()
                && pagesStrip.getChildAt(desiredPage) instanceof FrameLayout) {
            FrameLayout page = (FrameLayout) pagesStrip.getChildAt(desiredPage);
            int[] free = findFreeContactShortcutGridPosition(page, shortcutId,
                    lastSavedLeft >= 0 ? lastSavedLeft : 0,
                    lastSavedTop >= 0 ? lastSavedTop : 0, null);
            if (free != null) {
                chosenPage = page;
                chosenLeft = free[0];
                chosenTop = free[1];
            }
        }

        if (chosenPage == null && lastSavedLeft >= 0 && lastSavedTop >= 0
                && desiredPage < pagesStrip.getChildCount()
                && pagesStrip.getChildAt(desiredPage) instanceof FrameLayout) {
            // Never discard a saved shortcut because its old position is temporarily blocked.
            chosenPage = (FrameLayout) pagesStrip.getChildAt(desiredPage);
            chosenLeft = Math.max(0, Math.min(pageViewport.getWidth() - cellW, lastSavedLeft));
            chosenTop = Math.max(0, Math.min(pageViewport.getHeight() - cellH, lastSavedTop));
        }
        if (chosenPage == null || pagesStrip.indexOfChild(chosenPage) != desiredPage) return;

        int chosenCol = Math.max(0, Math.min(columns - 1,
                Math.round(chosenLeft / (float) cellW)));
        int chosenRow = homeGridRowFromTop(chosenTop, rows, pageViewport.getHeight(), cellH);
        int initialIconSize = shortcutIconSizePx(cellW, cellH);
        int tileLeft = homeGridIconLeftAt(null, chosenCol, cellW, initialIconSize);
        int tileTop = homeGridRowTop(chosenRow, rows, pageViewport.getHeight(), cellH)
                + LauncherUiUtils.dp(this, 14);
        float normalizedX = chosenCol / (float) Math.max(1, columns - 1);
        float normalizedY = chosenRow / (float) Math.max(1, rows - 1);
        prefs.edit().putInt(shortcutPrefKey(KEY_SHORTCUT_PAGE, shortcutId), desiredPage)
                .putFloat(shortcutPrefKey(KEY_SHORTCUT_X, shortcutId), normalizedX)
                .putFloat(shortcutPrefKey(KEY_SHORTCUT_Y, shortcutId), normalizedY)
                .putBoolean(shortcutPrefKey(KEY_SHORTCUT_GRID_POSITION, shortcutId), true)
                .apply();

        FrameLayout tile = new FrameLayout(this);
        tile.setTag("launcher_contact_shortcut");
        tile.setContentDescription("launcher_shortcut_id:" + shortcutId);
        tile.setClipChildren(false);
        tile.setClipToPadding(false);
        chosenPage.setClipChildren(false);

        ImageView icon = new ImageView(this);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        boolean iconLoaded = false;
        String iconFile = prefs.getString(shortcutPrefKey(KEY_SHORTCUT_ICON, shortcutId), null);
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

        FrameLayout.LayoutParams iconParams = new FrameLayout.LayoutParams(
                initialIconSize, initialIconSize, Gravity.TOP | Gravity.LEFT);
        tile.addView(icon, iconParams);

        TextView label = LauncherUiUtils.text(this,
                prefs.getString(shortcutPrefKey(KEY_SHORTCUT_LABEL, shortcutId), "Contact shortcut"),
                homeFontSizeSp(), Color.WHITE, true);
        label.setGravity(Gravity.CENTER);
        label.setSingleLine(true);
        label.setMaxLines(1);
        label.setEllipsize(android.text.TextUtils.TruncateAt.END);
        label.setIncludeFontPadding(false);
        FrameLayout.LayoutParams labelParams = new FrameLayout.LayoutParams(
                cellW, LauncherUiUtils.dp(this, homeLabelHeightDp()), Gravity.TOP | Gravity.LEFT);
        labelParams.leftMargin = -Math.max(0, (cellW - initialIconSize) / 2);
        labelParams.topMargin = initialIconSize + LauncherUiUtils.dp(this, 1);
        tile.addView(label, labelParams);
        label.setVisibility(prefs.getBoolean(KEY_SHOW_HOME_LABELS, true)
                ? View.VISIBLE : View.GONE);

        FrameLayout.LayoutParams tileParams = new FrameLayout.LayoutParams(
                initialIconSize, initialIconSize, Gravity.TOP | Gravity.LEFT);
        tileParams.leftMargin = tileLeft;
        tileParams.topMargin = tileTop;
        chosenPage.addView(tile, tileParams);
        tile.setOnClickListener(v -> {
            try {
                Intent target = Intent.parseUri(prefs.getString(shortcutPrefKey(KEY_SHORTCUT_INTENT, shortcutId), ""),
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
        // Shortcuts must enter drag mode when the finger moves; a stationary
        // long press still opens the options popup. Keep the drag stream on the tile
        // so workspace swipes and click handling cannot steal it.
        final float[] shortcutTouchStart = new float[2];
        final float[] shortcutFingerOffset = new float[2];
        final int[] shortcutOrigin = new int[2];
        final int[] shortcutOriginalPage = {-1};
        final boolean[] shortcutDragging = new boolean[1];
        final boolean[] shortcutMoved = new boolean[1];
        final boolean[] shortcutMoveArmed = new boolean[1];
        final boolean[] shortcutPopupShown = new boolean[1];
        final boolean[] shortcutReparenting = new boolean[1];
        final android.os.Handler shortcutTouchHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        final Runnable armShortcutMove = () -> {
            if (shortcutPopupShown[0] || shortcutDragging[0]) return;
            shortcutMoveArmed[0] = true;
            setWorkspaceGridVisible(true, tile);
            // Match widgets: steady 10% enlargement after the move-ready hold.
            LauncherUiUtils.startScaleHint(tile, 1.10f);
        };
        final Runnable showShortcutPopup = () -> {
            if (!shortcutMoved[0] && !shortcutDragging[0]) {
                LauncherUiUtils.stopMoveHint(tile);
                setWorkspaceGridVisible(false, null);
                shortcutPopupShown[0] = true;
                tile.performLongClick();
            }
        };
        tile.setOnTouchListener((view, event) -> {
            if (shortcutReparenting[0]) return true;
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                shortcutTouchStart[0] = event.getRawX();
                shortcutTouchStart[1] = event.getRawY();
                int[] shortcutLocation = new int[2];
                tile.getLocationOnScreen(shortcutLocation);
                shortcutFingerOffset[0] = event.getRawX() - shortcutLocation[0];
                shortcutFingerOffset[1] = event.getRawY() - shortcutLocation[1];
                shortcutOriginalPage[0] = tile.getParent() instanceof View
                        && pagesStrip != null ? pagesStrip.indexOfChild((View) tile.getParent()) : currentPage;
                FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) tile.getLayoutParams();
                shortcutOrigin[0] = lp.leftMargin;
                shortcutOrigin[1] = lp.topMargin;
                shortcutDragging[0] = false;
                shortcutMoved[0] = false;
                shortcutMoveArmed[0] = false;
                shortcutPopupShown[0] = false;
                shortcutTouchHandler.removeCallbacks(armShortcutMove);
                shortcutTouchHandler.removeCallbacks(showShortcutPopup);
                shortcutTouchHandler.postDelayed(armShortcutMove, 1000L);
                shortcutTouchHandler.postDelayed(showShortcutPopup, 2000L);
                return true;
            }
            if (action == MotionEvent.ACTION_MOVE) {
                float dx = event.getRawX() - shortcutTouchStart[0];
                float dy = event.getRawY() - shortcutTouchStart[1];
                boolean pastSlop = Math.abs(dx) > LauncherUiUtils.dp(this, 8)
                        || Math.abs(dy) > LauncherUiUtils.dp(this, 8);
                if (pastSlop && shortcutMoveArmed[0] && !shortcutPopupShown[0]) {
                    boolean firstDragFrame = !shortcutDragging[0];
                    shortcutMoved[0] = true;
                    shortcutDragging[0] = true;
                    shortcutTouchHandler.removeCallbacks(showShortcutPopup);
                    tile.setPressed(false);
                    if (tile.getParent() != null) tile.getParent().requestDisallowInterceptTouchEvent(true);
                    if (firstDragFrame) {
                        tile.animate().cancel();
                        LauncherUiUtils.stopMoveHint(tile);
                        float baseScaleX = tile.getScaleX();
                        float baseScaleY = tile.getScaleY();
                        tile.setPivotX(tile.getWidth() / 2f);
                        tile.setPivotY(tile.getHeight() / 2f);
                        LauncherUiUtils.startScaleHint(tile, 1.10f);
                        float targetScaleX = baseScaleX * 1.10f;
                        float targetScaleY = baseScaleY * 1.10f;
                        tile.setScaleX(baseScaleX);
                        tile.setScaleY(baseScaleY);
                        tile.bringToFront();
                        tile.animate().scaleX(targetScaleX).scaleY(targetScaleY)
                                .alpha(1f).setDuration(animationDuration(120L)).start();
                        beginWorkspaceTransferDrag(tile, shortcutOriginalPage[0],
                                event.getRawX(), event.getRawY(),
                                shortcutFingerOffset[0], shortcutFingerOffset[1]);
                    }
                    updateWorkspaceTransferEdge(event.getRawX(), event.getRawY());
                    setWorkspaceGridVisible(true, tile);
                    FrameLayout page = tile.getParent() instanceof FrameLayout
                            ? (FrameLayout) tile.getParent() : null;
                    if (page != null) {
                        int[] pageLocation = new int[2];
                        page.getLocationOnScreen(pageLocation);
                        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) tile.getLayoutParams();
                        int left = Math.round(event.getRawX() - pageLocation[0]
                                - shortcutFingerOffset[0] - tile.getTranslationX()
                                - tile.getPivotX() * (1f - Math.max(0.01f, tile.getScaleX())));
                        int top = Math.round(event.getRawY() - pageLocation[1]
                                - shortcutFingerOffset[1] - tile.getTranslationY()
                                - tile.getPivotY() * (1f - Math.max(0.01f, tile.getScaleY())));
                        left = Math.max(0, Math.min(Math.max(0, page.getWidth() - tile.getWidth()), left));
                        top = Math.max(0, Math.min(Math.max(0, page.getHeight() - tile.getHeight()), top));
                        lp.leftMargin = left;
                        lp.topMargin = top;
                        tile.setLayoutParams(lp);
                        int transferPage = getWorkspaceTransferTargetPage();
                        if (transferPage >= 0 && transferPage != shortcutOriginalPage[0]) {
                            updateWorkspaceTransferDisplacements(tile, event.getRawX(), event.getRawY(),
                                    shortcutFingerOffset[0], shortcutFingerOffset[1]);
                        } else {
                            scheduleLiveDisplacementUpdate(page, tile);
                        }
                    }
                    return true;
                }
                // Keep consuming the gesture while waiting for the one-second
                // hold threshold so a workspace swipe cannot steal the touch.
                return true;
            }
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                shortcutTouchHandler.removeCallbacks(armShortcutMove);
                shortcutTouchHandler.removeCallbacks(showShortcutPopup);
                if (shortcutDragging[0]) {
                    boolean commit = action == MotionEvent.ACTION_UP;
                    int transferPage = finishWorkspaceTransferAnimation();
                    if (!commit) transferPage = shortcutOriginalPage[0];
                    if (transferPage < 0) transferPage = shortcutOriginalPage[0];
                    if (tile.getParent() instanceof FrameLayout) {
                        FrameLayout sourcePage = (FrameLayout) tile.getParent();
                        int sourcePageIndex = pagesStrip == null ? currentPage : pagesStrip.indexOfChild(sourcePage);
                        FrameLayout targetPage = sourcePage;
                        if (commit && pagesStrip != null && transferPage >= 0
                                && transferPage < pagesStrip.getChildCount()
                                && pagesStrip.getChildAt(transferPage) instanceof FrameLayout) {
                            targetPage = (FrameLayout) pagesStrip.getChildAt(transferPage);
                        }
                        tile.animate().cancel();
                        LauncherUiUtils.stopMoveHint(tile);
                        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) tile.getLayoutParams();
                        int[] targetLocation = new int[2];
                        targetPage.getLocationOnScreen(targetLocation);
                        int left = commit ? Math.round(event.getRawX() - targetLocation[0]
                                - shortcutFingerOffset[0]) : shortcutOrigin[0];
                        int top = commit ? Math.round(event.getRawY() - targetLocation[1]
                                - shortcutFingerOffset[1]) : shortcutOrigin[1];
                        left = Math.max(0, Math.min(Math.max(0, targetPage.getWidth() - tile.getWidth()), left));
                        top = Math.max(0, Math.min(Math.max(0, targetPage.getHeight() - tile.getHeight()), top));
                        if (commit && targetPage.getWidth() > 0 && targetPage.getHeight() > 0) {
                            int shortcutCellWidth = Math.max(1, targetPage.getWidth() / 4);
                            int shortcutCellHeight = LauncherUiUtils.dp(this,
                                    homeCellHeightDp(prefs.getInt(KEY_ICON_SIZE, 48)));
                            int shortcutRows = homeGridRowsForHeight(targetPage.getHeight(), shortcutCellHeight);
                            int shortcutCol = Math.max(0, Math.min(3,
                                    Math.round((left + tile.getWidth() / 2f) / shortcutCellWidth - 0.5f)));
                            int shortcutRow = homeGridRowFromTop(
                                    top - LauncherUiUtils.dp(this, 14), shortcutRows,
                                    targetPage.getHeight(), shortcutCellHeight);
                            left = homeGridIconLeftAt(null, shortcutCol, shortcutCellWidth, tile.getWidth());
                            top = homeGridRowTop(shortcutRow, shortcutRows,
                                    targetPage.getHeight(), shortcutCellHeight)
                                    + LauncherUiUtils.dp(this, 14);
                        }
                        lp.leftMargin = left;
                        lp.topMargin = top;
                        tile.setTranslationX(0f);
                        tile.setTranslationY(0f);
                        tile.setLayoutParams(lp);

                        if (commit && targetPage != sourcePage) {
                            updateWorkspaceTransferDisplacements(tile, event.getRawX(), event.getRawY(),
                                    shortcutFingerOffset[0], shortcutFingerOffset[1]);
                            shortcutReparenting[0] = true;
                            ((ViewGroup) sourcePage).removeView(tile);
                            targetPage.addView(tile, lp);
                            shortcutReparenting[0] = false;
                            // Validate the real destination tile, not just the transfer proxy.
                            updateLiveDisplacements(targetPage, tile);
                        }
                        finishWorkspaceTransferDrag(tile, commit);
                        finishLiveDisplacements(commit, tile);
                        float density = getResources().getDisplayMetrics().density;
                        int pageIndex = pagesStrip == null ? currentPage : pagesStrip.indexOfChild(targetPage);
                        FrameLayout.LayoutParams finalShortcutLp =
                                (FrameLayout.LayoutParams) tile.getLayoutParams();
                        int finalLeft = finalShortcutLp.leftMargin;
                        int finalTop = finalShortcutLp.topMargin;
                        if (commit) {
                            int cellHeight = LauncherUiUtils.dp(this,
                                    homeCellHeightDp(prefs.getInt(KEY_ICON_SIZE, 48)));
                            int shortcutRows = homeGridRowsForHeight(targetPage.getHeight(), cellHeight);
                            int[] slot = homeGridIconSlot(targetPage, tile, cellHeight, shortcutRows);
                            if (slot != null) {
                                prefs.edit()
                                        .putInt(shortcutPrefKey(KEY_SHORTCUT_PAGE, shortcutId),
                                                pageIndex >= 0 ? pageIndex : currentPage)
                                        .putFloat(shortcutPrefKey(KEY_SHORTCUT_X, shortcutId),
                                                slot[0] / (float) Math.max(1, 4 - 1))
                                        .putFloat(shortcutPrefKey(KEY_SHORTCUT_Y, shortcutId),
                                                slot[1] / (float) Math.max(1, shortcutRows - 1))
                                        .putBoolean(shortcutPrefKey(KEY_SHORTCUT_GRID_POSITION, shortcutId), true)
                                        .putInt(shortcutPrefKey(KEY_SHORTCUT_WIDTH_DP, shortcutId),
                                                Math.max(1, Math.round(finalShortcutLp.width / density)))
                                        .putInt(shortcutPrefKey(KEY_SHORTCUT_HEIGHT_DP, shortcutId),
                                                Math.max(1, Math.round(finalShortcutLp.height / density)))
                                        .apply();
                            }
                        }
                    } else {
                        LauncherUiUtils.stopMoveHint(tile);
                        finishWorkspaceTransferDrag(tile, commit);
                    }
                    shortcutDragging[0] = false;
                    // stopMoveHint() above restores the shortcut's pre-move scale.
                    // Do not force 1f here: crowded workspaces may have a saved scale.
                    if (tile.getParent() != null) tile.getParent().requestDisallowInterceptTouchEvent(false);
                    setWorkspaceGridVisible(false, null);
                    refreshWorkspaceGrid();
                    return true;
                }
                LauncherUiUtils.stopMoveHint(tile);
                // A one-second hold enters move/edit mode; releasing without moving
                // must not accidentally launch the contact/application shortcut.
                if (!shortcutPopupShown[0] && !shortcutMoved[0] && action == MotionEvent.ACTION_UP
                        && !shortcutMoveArmed[0]) {
                    tile.performClick();
                }
                if (shortcutMoveArmed[0]) setWorkspaceGridVisible(false, null);
                return true;
            }
            return true;
        });
        // Contact shortcuts are launcher-owned tiles, not AppWidgetHostViews, so
        // provide the same long-press options explicitly.
        tile.setOnLongClickListener(v -> {
            if (shortcutMoved[0]) return true;
            showActionDialog(prefs.getString(shortcutPrefKey(KEY_SHORTCUT_LABEL, shortcutId), "Contact shortcut"),
                    new String[]{"↔", "×", "↻"},
                    new String[]{"Move to other page", "Remove shortcut", "Recreate contact shortcut"},
                    (dialog, which) -> {
                if (which == 0) {
                    int pageCount = pagesStrip == null ? 1 : Math.max(1, pagesStrip.getChildCount());
                    int targetPage = (prefs.getInt(shortcutPrefKey(KEY_SHORTCUT_PAGE, shortcutId), 0) == 0 && pageCount > 1) ? 1 : 0;
                    prefs.edit().putInt(shortcutPrefKey(KEY_SHORTCUT_PAGE, shortcutId), targetPage).apply();
                    currentPage = targetPage;
                    renderHomePages(false);
                    showPage(targetPage, true);
                } else if (which == 1) {
                    removeSavedShortcut(shortcutId);
                    renderHomePages(false);
                } else if (which == 2) {
                    removeSavedShortcut(shortcutId);
                    beginWidgetPicker(currentPage);
                }
            });
            return true;
        });
    }

    private int getShortcutIconSizeDp() {
        int homeSize = Math.max(32, Math.min(74,
                prefs == null ? 48 : prefs.getInt(KEY_ICON_SIZE, 48)));
        int configured = homeSize;
        if (prefs != null && prefs.contains(KEY_SHORTCUT_ICON_SIZE_DP)) {
            configured = prefs.getInt(KEY_SHORTCUT_ICON_SIZE_DP, homeSize);
        } else if (prefs != null) {
            for (String id : getSavedShortcutIds()) {
                String legacyKey = shortcutPrefKey(KEY_SHORTCUT_ICON_SIZE_DP, id);
                if (prefs.contains(legacyKey)) {
                    configured = prefs.getInt(legacyKey, homeSize);
                    break;
                }
            }
        }
        return Math.max(24, Math.min(homeSize, configured));
    }

    private int shortcutIconSizePx(int cellWidth, int cellHeight) {
        int requested = LauncherUiUtils.dp(this, getShortcutIconSizeDp());
        int widthLimit = Math.max(1, cellWidth - LauncherUiUtils.dp(this, 4));
        int labelHeight = prefs != null && prefs.getBoolean(KEY_SHOW_HOME_LABELS, true)
                ? LauncherUiUtils.dp(this, homeLabelHeightDp()) + LauncherUiUtils.dp(this, 1) : 0;
        int heightLimit = Math.max(1, cellHeight - LauncherUiUtils.dp(this, 14 + 7) - labelHeight);
        return Math.max(1, Math.min(requested, Math.min(widthLimit, heightLimit)));
    }

    private android.graphics.Rect contactShortcutVisualRectAt(
            String shortcutId, int x, int y, int cellWidth, int cellHeight) {
        int iconSize = shortcutIconSizePx(cellWidth, cellHeight);
        int left = x + Math.max(0, (cellWidth - iconSize) / 2);
        int top = y + LauncherUiUtils.dp(this, 14);
        android.graphics.Rect visual = new android.graphics.Rect(left, top, left + iconSize, top + iconSize);
        if (prefs == null || !prefs.getBoolean(KEY_SHOW_HOME_LABELS, true)) return visual;
        String text = prefs.getString(shortcutPrefKey(KEY_SHORTCUT_LABEL, shortcutId), "Contact shortcut");
        android.graphics.Paint paint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        paint.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        paint.setTextSize(homeFontSizeSp() * getResources().getDisplayMetrics().scaledDensity);
        android.graphics.Paint.FontMetrics metrics = paint.getFontMetrics();
        int textHeight = Math.max(1, (int) Math.ceil(metrics.descent - metrics.ascent));
        int labelHeight = LauncherUiUtils.dp(this, homeLabelHeightDp());
        float availableWidth = Math.max(1, cellWidth - LauncherUiUtils.dp(this, 4));
        int textWidth = Math.max(1, (int) Math.ceil(Math.min(availableWidth, paint.measureText(text))));
        int labelLeft = x + (cellWidth - textWidth) / 2;
        int labelTop = y + LauncherUiUtils.dp(this, 14) + iconSize
                + LauncherUiUtils.dp(this, 1) + Math.max(0, (labelHeight - textHeight) / 2);
        visual.union(new android.graphics.Rect(labelLeft, labelTop, labelLeft + textWidth, labelTop + textHeight));
        return visual;
    }

    private int[] findFreeContactShortcutGridPosition(
            FrameLayout page, String shortcutId, int preferredX, int preferredY, View ignore) {
        if (page == null || page.getWidth() <= 0 || page.getHeight() <= 0) return null;
        int cellWidth = Math.max(1, page.getWidth() / 4);
        int cellHeight = LauncherUiUtils.dp(this,
                homeCellHeightDp(prefs == null ? 48 : prefs.getInt(KEY_ICON_SIZE, 48)));
        int rows = homeGridRowsForHeight(page.getHeight(), cellHeight);
        int bestX = -1, bestY = -1;
        long bestDistance = Long.MAX_VALUE;
        for (int row = 0; row < rows; row++) {
            int y = homeGridRowTop(row, rows, page.getHeight(), cellHeight);
            if (y + cellHeight > page.getHeight()) continue;
            for (int col = 0; col < 4; col++) {
                int x = col * cellWidth;
                if (x + cellWidth > page.getWidth()) continue;
                if (!isContactShortcutPositionFree(page, ignore, x, y,
                        cellWidth, cellHeight, shortcutId)) continue;
                long distance = Math.abs((long) x - preferredX) + Math.abs((long) y - preferredY);
                if (distance < bestDistance) { bestDistance = distance; bestX = x; bestY = y; }
            }
        }
        return bestX < 0 ? null : new int[]{bestX, bestY, cellWidth, cellHeight};
    }

    private boolean isContactShortcutPositionFree(FrameLayout page, View ignore, int left, int top,
            int width, int height, String shortcutId) {
        if (page == null || width <= 0 || height <= 0) return false;
        int cellWidth = Math.max(1, page.getWidth() / 4);
        int cellHeight = LauncherUiUtils.dp(this,
                homeCellHeightDp(prefs == null ? 48 : prefs.getInt(KEY_ICON_SIZE, 48)));
        int rows = homeGridRowsForHeight(page.getHeight(), cellHeight);
        int col = Math.max(0, Math.min(3, Math.round(left / (float) cellWidth)));
        int row = homeGridRowFromTop(top, rows, page.getHeight(), cellHeight);
        int snappedLeft = col * cellWidth;
        int snappedTop = homeGridRowTop(row, rows, page.getHeight(), cellHeight);
        if (snappedLeft < 0 || snappedTop < 0
                || snappedLeft + cellWidth > page.getWidth()
                || snappedTop + cellHeight > page.getHeight()) return false;
        android.graphics.Rect candidate = contactShortcutVisualRectAt(
                shortcutId, snappedLeft, snappedTop, cellWidth, cellHeight);
        android.graphics.Rect candidateWithGap = new android.graphics.Rect(candidate);
        int gap = LauncherUiUtils.dp(this, 2);
        candidateWithGap.inset(-gap, -gap);
        for (int i = 0; i < page.getChildCount(); i++) {
            View child = page.getChildAt(i);
            if (child == ignore || child == workspaceGridOverlays[0] || child == workspaceGridOverlays[1]
                    || child.getVisibility() != View.VISIBLE || !isWorkspaceItem(child)) continue;
            int[] geometry = itemGeometry(child);
            if (geometry[2] <= 0 || geometry[3] <= 0) continue;
            if (isHomeGridIcon(child)) {
                android.graphics.Rect other = appIconVisualRectAt(
                        child, geometry[0], geometry[1], geometry[2], geometry[3]);
                if (android.graphics.Rect.intersects(candidate, other)) return false;
            } else {
                android.graphics.Rect other = collisionRect(child, geometry);
                if (android.graphics.Rect.intersects(candidateWithGap, other)) return false;
            }
        }
        return true;
    }

    private void applyShortcutIconSizeToWorkspace() {
        if (pagesStrip == null) return;
        for (int pageIndex = 0; pageIndex < Math.min(2, pagesStrip.getChildCount()); pageIndex++) {
            View pageView = pagesStrip.getChildAt(pageIndex);
            if (!(pageView instanceof FrameLayout)) continue;
            FrameLayout page = (FrameLayout) pageView;
            int cellWidth = Math.max(1, page.getWidth() / 4);
            int cellHeight = LauncherUiUtils.dp(this,
                    homeCellHeightDp(prefs == null ? 48 : prefs.getInt(KEY_ICON_SIZE, 48)));
            int rows = homeGridRowsForHeight(page.getHeight(), cellHeight);
            for (int i = 0; i < page.getChildCount(); i++) {
                View child = page.getChildAt(i);
                if (!isWorkspaceContactShortcut(child) || !(child instanceof ViewGroup)) continue;
                ViewGroup shortcut = (ViewGroup) child;
                if (shortcut.getChildCount() < 2) continue;
                ImageView icon = shortcut.getChildAt(0) instanceof ImageView
                        ? (ImageView) shortcut.getChildAt(0) : null;
                TextView label = shortcut.getChildAt(1) instanceof TextView
                        ? (TextView) shortcut.getChildAt(1) : null;
                if (icon == null) continue;
                int[] slot = homeGridIconSlot(page, shortcut, cellHeight, rows);
                if (slot == null) continue;
                int target = shortcutIconSizePx(cellWidth, cellHeight);
                FrameLayout.LayoutParams tileParams = (FrameLayout.LayoutParams) shortcut.getLayoutParams();
                tileParams.width = target;
                tileParams.height = target;
                tileParams.leftMargin = homeGridIconLeftAt(null, slot[0], cellWidth, target);
                tileParams.topMargin = homeGridRowTop(slot[1], rows, page.getHeight(), cellHeight)
                        + LauncherUiUtils.dp(this, 14);
                shortcut.setLayoutParams(tileParams);

                ViewGroup.LayoutParams iconParams = icon.getLayoutParams();
                if (iconParams != null && (iconParams.width != target || iconParams.height != target)) {
                    iconParams.width = target;
                    iconParams.height = target;
                    icon.setLayoutParams(iconParams);
                }
                if (label != null && label.getLayoutParams() instanceof FrameLayout.LayoutParams) {
                    FrameLayout.LayoutParams labelParams =
                            (FrameLayout.LayoutParams) label.getLayoutParams();
                    labelParams.width = cellWidth;
                    labelParams.leftMargin = -Math.max(0, (cellWidth - target) / 2);
                    labelParams.topMargin = target + LauncherUiUtils.dp(this, 1);
                    label.setLayoutParams(labelParams);
                }
            }
            resolveAllWorkspaceOverlaps(page, null);
        }
        refreshWorkspaceGrid();
    }

    private void rebuildContactShortcutViews() {
        if (pagesStrip == null) return;
        for (int pageIndex = 0; pageIndex < pagesStrip.getChildCount(); pageIndex++) {
            View pageView = pagesStrip.getChildAt(pageIndex);
            if (!(pageView instanceof ViewGroup)) continue;
            ViewGroup page = (ViewGroup) pageView;
            for (int i = page.getChildCount() - 1; i >= 0; i--) {
                View child = page.getChildAt(i);
                if (isWorkspaceContactShortcut(child)) {
                    child.animate().cancel();
                    page.removeViewAt(i);
                }
            }
        }
        renderSavedContactShortcut();
    }

    private boolean hasRenderedContactShortcut(String shortcutId) {
        if (pagesStrip == null || shortcutId == null) return false;
        String expectedDescription = "launcher_shortcut_id:" + shortcutId;
        for (int i = 0; i < pagesStrip.getChildCount(); i++) {
            View page = pagesStrip.getChildAt(i);
            if (!(page instanceof ViewGroup)) continue;
            ViewGroup group = (ViewGroup) page;
            for (int j = 0; j < group.getChildCount(); j++) {
                View child = group.getChildAt(j);
                if ("launcher_contact_shortcut".equals(child.getTag())
                        && expectedDescription.contentEquals(child.getContentDescription())) return true;
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

    /**
     * Refresh only the icon artwork when a visual icon option changes. Rebuilding the
     * entire workspace here detached every shortcut and re-ran placement/capacity logic,
     * which caused visible flicker and could make icons appear to move or vanish briefly.
     */
    private void refreshWorkspaceIconAppearance() {
        if (pagesStrip != null) {
            boolean showBackground = prefs.getBoolean("icon_rounded_square_background", false);
            int shape = Math.max(LauncherIconDrawable.SHAPE_ORIGINAL,
                    Math.min(LauncherIconDrawable.SHAPE_PEAR,
                            prefs.getInt("app_icon_shape", LauncherIconDrawable.SHAPE_ORIGINAL)));
            int backgroundChoice = prefs.getInt(KEY_ICON_BACKGROUND_COLOR,
                    LauncherIconDrawable.BACKGROUND_BLACK);
            int backgroundColor = LauncherIconDrawable.backgroundColorForChoice(backgroundChoice);
            for (int pageIndex = 0; pageIndex < Math.min(2, pagesStrip.getChildCount()); pageIndex++) {
                View pageView = pagesStrip.getChildAt(pageIndex);
                if (!(pageView instanceof FrameLayout)) continue;
                FrameLayout page = (FrameLayout) pageView;
                for (int childIndex = 0; childIndex < page.getChildCount(); childIndex++) {
                    View child = page.getChildAt(childIndex);
                    Object rawTag = child.getTag();
                    if (!(rawTag instanceof String)
                            || !((String) rawTag).startsWith("launcher_app:")) continue;
                    String packageName = ((String) rawTag).substring("launcher_app:".length());
                    ResolveInfo app = findApp(packageName);
                    if (app == null || !(child instanceof ViewGroup)
                            || ((ViewGroup) child).getChildCount() == 0) continue;
                    View iconView = ((ViewGroup) child).getChildAt(0);
                    if (!(iconView instanceof android.widget.ImageView)) continue;
                    android.graphics.drawable.Drawable source = app.loadIcon(packageManager);
                    android.widget.ImageView icon = (android.widget.ImageView) iconView;
                    icon.setImageDrawable(source == null ? null : new LauncherIconDrawable(
                            source, showBackground, shape, backgroundColor));
                    icon.invalidate();
                }
            }
        }
        if (drawerAdapter != null) drawerAdapter.notifyDataSetChanged();
        renderDock();
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

    private void clearWorkspaceTransferEdgeHover() {
        handler.removeCallbacks(workspaceTransferEdgeSwitchRunnable);
        workspaceTransferEdgeHoverTargetPage = -1;
    }

    private void beginWorkspaceTransferDrag(View item, int sourcePage, float rawX, float rawY,
            float fingerOffsetX, float fingerOffsetY) {
        if (item == null || pagesStrip == null || sourcePage < 0
                || sourcePage >= pagesStrip.getChildCount()) return;
        if (workspaceTransferItem != null && workspaceTransferItem != item) {
            finishWorkspaceTransferDrag(workspaceTransferItem, false);
        }
        clearWorkspaceTransferEdgeHover();
        liveDragLastSafeGeometry.remove(item);
        if (item.getParent() instanceof FrameLayout) {
            int[] initialGeometry = itemGeometry(item);
            liveDragLastSafeGeometry.put(item,
                    new int[]{initialGeometry[0], initialGeometry[1]});
        }
        workspaceTransferItem = item;
        workspaceTransferSourcePage = pagesStrip.getChildAt(sourcePage) instanceof FrameLayout
                ? (FrameLayout) pagesStrip.getChildAt(sourcePage) : null;
        workspaceTransferOriginPage = sourcePage;
        workspaceTransferTargetPage = sourcePage;
        workspaceTransferLastRawX = rawX;
        workspaceTransferLastRawY = rawY;
        workspaceTransferFingerOffsetX = fingerOffsetX;
        workspaceTransferFingerOffsetY = fingerOffsetY;
        workspaceTransferProxy = null;
        workspaceTransferProxyPage = -1;
    }

    private void updateWorkspaceTransferEdge(float rawX, float rawY) {
        workspaceTransferLastRawX = rawX;
        workspaceTransferLastRawY = rawY;
        if (workspaceTransferItem == null || pageViewport == null || pagesStrip == null
                || pageViewport.getWidth() <= 0 || workspaceTransferTargetPage < 0) {
            clearWorkspaceTransferEdgeHover();
            return;
        }
        int[] viewportLocation = new int[2];
        pageViewport.getLocationOnScreen(viewportLocation);
        int edge = LauncherUiUtils.dp(this, ICON_EDGE_SWITCH_ZONE_DP);
        int targetPage = -1;
        if (workspaceTransferTargetPage + 1 < pagesStrip.getChildCount()
                && rawX >= viewportLocation[0] + pageViewport.getWidth() - edge) {
            targetPage = workspaceTransferTargetPage + 1;
        } else if (workspaceTransferTargetPage > 0
                && rawX <= viewportLocation[0] + edge) {
            targetPage = workspaceTransferTargetPage - 1;
        }
        if (targetPage < 0) {
            clearWorkspaceTransferEdgeHover();
            return;
        }
        if (targetPage == workspaceTransferEdgeHoverTargetPage) return;
        handler.removeCallbacks(workspaceTransferEdgeSwitchRunnable);
        workspaceTransferEdgeHoverTargetPage = targetPage;
        handler.postDelayed(workspaceTransferEdgeSwitchRunnable, ICON_EDGE_SWITCH_DELAY_MS);
    }

    private int getWorkspaceTransferTargetPage() {
        return workspaceTransferItem == null ? -1 : workspaceTransferTargetPage;
    }

    private void switchWorkspaceTransferPage(int requestedTargetPage) {
        if (workspaceTransferItem == null || pagesStrip == null || pageViewport == null) return;
        if (workspaceTransferPageAnimator != null) finishWorkspaceTransferAnimation();
        int fromPage = workspaceTransferTargetPage;
        if (requestedTargetPage == fromPage || requestedTargetPage < 0
                || requestedTargetPage >= pagesStrip.getChildCount()
                || Math.abs(requestedTargetPage - fromPage) != 1) return;

        // Each preview page owns its own temporary displacement transaction. Roll it
        // back before revealing the next page so source-page items never remain displaced.
        if (workspaceTransferProxy != null) {
            finishLiveDisplacements(false, workspaceTransferProxy);
            if (workspaceTransferProxy.getParent() instanceof ViewGroup) {
                ((ViewGroup) workspaceTransferProxy.getParent()).removeView(workspaceTransferProxy);
            }
            workspaceTransferProxy = null;
            workspaceTransferProxyPage = -1;
        } else if (!liveDisplacedItems.isEmpty() && workspaceTransferItem != null) {
            finishLiveDisplacements(false, workspaceTransferItem);
        }

        int pageWidth = Math.max(1, pageViewport.getWidth());
        float startStripX = pagesStrip.getTranslationX();
        float endStripX = -requestedTargetPage * (float) pageWidth;
        if (Math.abs(startStripX + fromPage * (float) pageWidth) > pageWidth * 0.5f) {
            startStripX = -fromPage * (float) pageWidth;
            pagesStrip.setTranslationX(startStripX);
        }
        workspaceTransferTargetPage = requestedTargetPage;
        currentPage = requestedTargetPage;
        activateClockForPage(requestedTargetPage);
        if (pageIndicator != null) {
            pageIndicator.setVisibility(prefs.getBoolean(KEY_SHOW_PAGE_INDICATOR, true)
                    ? View.VISIBLE : View.GONE);
            pageIndicator.setText((requestedTargetPage + 1) + "  /  " + pagesStrip.getChildCount());
        }

        pagesStrip.setClipChildren(false);
        pagesStrip.setClipToPadding(false);
        if (workspaceTransferSourcePage != null) {
            workspaceTransferSourcePage.setClipChildren(false);
            workspaceTransferSourcePage.setClipToPadding(false);
            workspaceTransferSourcePage.setElevation(LauncherUiUtils.dp(this, 30));
        }
        workspaceTransferAnimationStartStripX = startStripX;
        workspaceTransferAnimationEndStripX = endStripX;
        workspaceTransferAnimationStartItemX = workspaceTransferItem.getTranslationX();
        workspaceTransferAnimationTargetPage = requestedTargetPage;
        final android.animation.ValueAnimator animator =
                android.animation.ValueAnimator.ofFloat(startStripX, endStripX);
        workspaceTransferPageAnimator = animator;
        animator.setDuration(animationDuration(220L));
        animator.setInterpolator(new android.view.animation.DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            if (workspaceTransferPageAnimator != animator || pagesStrip == null) return;
            float stripX = ((Float) animation.getAnimatedValue()).floatValue();
            pagesStrip.setTranslationX(stripX);
            View dragged = workspaceTransferItem;
            if (dragged != null && dragged.getParent() == workspaceTransferSourcePage) {
                dragged.setTranslationX(workspaceTransferAnimationStartItemX
                        + (workspaceTransferAnimationStartStripX - stripX));
            }
        });
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(android.animation.Animator animation) {
                if (workspaceTransferPageAnimator != animator) return;
                workspaceTransferPageAnimator = null;
                if (pagesStrip != null) pagesStrip.setTranslationX(workspaceTransferAnimationEndStripX);
                View dragged = workspaceTransferItem;
                if (dragged != null && dragged.getParent() == workspaceTransferSourcePage) {
                    dragged.setTranslationX(workspaceTransferAnimationStartItemX
                            + (workspaceTransferAnimationStartStripX
                            - workspaceTransferAnimationEndStripX));
                }
            }
        });
        animator.start();

        // The source page uses the real dragged item for live collisions. Create an
        // invisible collision proxy only when previewing a different destination page.
        if (requestedTargetPage != workspaceTransferOriginPage) {
            createWorkspaceTransferProxy(requestedTargetPage);
            updateWorkspaceTransferDisplacements(workspaceTransferItem,
                    workspaceTransferLastRawX, workspaceTransferLastRawY,
                    workspaceTransferFingerOffsetX, workspaceTransferFingerOffsetY);
            final View transferItem = workspaceTransferItem;
            final int destinationIndex = requestedTargetPage;
            View destinationView = pagesStrip.getChildAt(destinationIndex);
            if (destinationView != null) destinationView.post(() -> {
                if (workspaceTransferItem == transferItem
                        && workspaceTransferTargetPage == destinationIndex) {
                    updateWorkspaceTransferDisplacements(transferItem,
                            workspaceTransferLastRawX, workspaceTransferLastRawY,
                            workspaceTransferFingerOffsetX, workspaceTransferFingerOffsetY);
                }
            });
        }
    }

    private void createWorkspaceTransferProxy(int pageIndex) {
        if (pagesStrip == null || workspaceTransferItem == null || pageIndex < 0
                || pageIndex >= pagesStrip.getChildCount()
                || !(pagesStrip.getChildAt(pageIndex) instanceof FrameLayout)) return;
        FrameLayout page = (FrameLayout) pagesStrip.getChildAt(pageIndex);
        View moving = workspaceTransferItem;
        ViewGroup.LayoutParams raw = moving.getLayoutParams();
        int width = raw instanceof FrameLayout.LayoutParams
                && ((FrameLayout.LayoutParams) raw).width > 0
                ? ((FrameLayout.LayoutParams) raw).width : moving.getWidth();
        int height = raw instanceof FrameLayout.LayoutParams
                && ((FrameLayout.LayoutParams) raw).height > 0
                ? ((FrameLayout.LayoutParams) raw).height : moving.getHeight();
        if (width <= 0 || height <= 0) return;
        View proxy = new View(this);
        proxy.setTag("workspace_transfer_proxy");
        proxy.setVisibility(View.INVISIBLE);
        proxy.setClickable(false);
        proxy.setFocusable(false);
        proxy.setScaleX(moving.getScaleX());
        proxy.setScaleY(moving.getScaleY());
        proxy.setPivotX(moving.getPivotX());
        proxy.setPivotY(moving.getPivotY());
        FrameLayout.LayoutParams proxyParams = new FrameLayout.LayoutParams(
                width, height, Gravity.TOP | Gravity.LEFT);
        page.addView(proxy, proxyParams);
        workspaceTransferProxy = proxy;
        workspaceTransferProxyPage = pageIndex;
    }

    private void cancelPendingLiveDisplacementUpdate() {
        FrameLayout pendingPage = pendingLiveDisplacementPage;
        if (pendingPage != null) pendingPage.removeCallbacks(liveDisplacementFrame);
        pendingLiveDisplacementPage = null;
        pendingLiveDisplacementMover = null;
    }

    private void scheduleLiveDisplacementUpdate(FrameLayout page, View movingView) {
        if (page == null || movingView == null || movingView.getParent() != page
                || page.getWidth() <= 0 || page.getHeight() <= 0) return;
        if (pendingLiveDisplacementPage == page && pendingLiveDisplacementMover == movingView) {
            return;
        }
        // A launcher workspace has one active drag owner. If ownership changes before
        // the next frame, keep only the newest owner instead of running stale collision
        // work against the old item's geometry.
        cancelPendingLiveDisplacementUpdate();
        pendingLiveDisplacementPage = page;
        pendingLiveDisplacementMover = movingView;
        page.postOnAnimation(liveDisplacementFrame);
    }

    private void updateWorkspaceTransferDisplacements(View item, float rawX, float rawY,
            float fingerOffsetX, float fingerOffsetY) {
        if (item == null || item != workspaceTransferItem || workspaceTransferTargetPage < 0
                || workspaceTransferTargetPage == workspaceTransferOriginPage
                || pagesStrip == null || workspaceTransferTargetPage >= pagesStrip.getChildCount()) return;
        if (!(pagesStrip.getChildAt(workspaceTransferTargetPage) instanceof FrameLayout)) return;
        FrameLayout page = (FrameLayout) pagesStrip.getChildAt(workspaceTransferTargetPage);
        if (workspaceTransferProxy == null || workspaceTransferProxyPage != workspaceTransferTargetPage
                || workspaceTransferProxy.getParent() != page) {
            if (workspaceTransferProxy != null && workspaceTransferProxy.getParent() instanceof ViewGroup) {
                ((ViewGroup) workspaceTransferProxy.getParent()).removeView(workspaceTransferProxy);
            }
            workspaceTransferProxy = null;
            workspaceTransferProxyPage = -1;
            createWorkspaceTransferProxy(workspaceTransferTargetPage);
        }
        View proxy = workspaceTransferProxy;
        if (proxy == null || page.getWidth() <= 0 || page.getHeight() <= 0) return;
        proxy.setScaleX(item.getScaleX());
        proxy.setScaleY(item.getScaleY());
        // Transfer geometry is measured in the destination page's final viewport
        // coordinates, not its temporarily translated screen location during the slide.
        int[] pageLocation = new int[2];
        pageViewport.getLocationOnScreen(pageLocation);
        int width = Math.max(1, proxy.getWidth());
        int height = Math.max(1, proxy.getHeight());
        if (proxy.getLayoutParams() instanceof FrameLayout.LayoutParams) {
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) proxy.getLayoutParams();
            if (lp.width > 0) width = lp.width;
            if (lp.height > 0) height = lp.height;
            float sx = Math.max(0.01f, proxy.getScaleX());
            float sy = Math.max(0.01f, proxy.getScaleY());
            int visibleWidth = Math.max(1, Math.round(width * sx));
            int visibleHeight = Math.max(1, Math.round(height * sy));
            int left = Math.max(0, Math.min(page.getWidth() - visibleWidth,
                    Math.round(rawX - pageLocation[0] - fingerOffsetX)));
            int top = Math.max(0, Math.min(page.getHeight() - visibleHeight,
                    Math.round(rawY - pageLocation[1] - fingerOffsetY)));
            // The proxy is only a collision marker. Translating it keeps its visual
            // collision bounds current without requesting a child relayout on every
            // touch event. Match itemGeometry()'s pending-layout origin if first layout
            // has not completed yet.
            int baseLeft = proxy.isLayoutRequested()
                    ? page.getPaddingLeft() + lp.leftMargin : proxy.getLeft();
            int baseTop = proxy.isLayoutRequested()
                    ? page.getPaddingTop() + lp.topMargin : proxy.getTop();
            proxy.setTranslationX(left - baseLeft - proxy.getPivotX() * (1f - sx));
            proxy.setTranslationY(top - baseTop - proxy.getPivotY() * (1f - sy));
            scheduleLiveDisplacementUpdate(page, proxy);
        }
    }

    private int finishWorkspaceTransferAnimation() {
        clearWorkspaceTransferEdgeHover();
        android.animation.ValueAnimator animator = workspaceTransferPageAnimator;
        if (animator != null) {
            int target = workspaceTransferAnimationTargetPage;
            animator.removeAllUpdateListeners();
            animator.removeAllListeners();
            animator.cancel();
            workspaceTransferPageAnimator = null;
            if (pagesStrip != null && target >= 0) {
                pagesStrip.setTranslationX(workspaceTransferAnimationEndStripX);
            }
            View dragged = workspaceTransferItem;
            if (dragged != null && dragged.getParent() == workspaceTransferSourcePage) {
                dragged.setTranslationX(workspaceTransferAnimationStartItemX
                        + (workspaceTransferAnimationStartStripX
                        - workspaceTransferAnimationEndStripX));
            }
            workspaceTransferTargetPage = target;
        }
        return workspaceTransferItem == null ? -1 : workspaceTransferTargetPage;
    }

    private void finishWorkspaceTransferDrag(View item, boolean commit) {
        if (item == null || item != workspaceTransferItem) return;
        finishWorkspaceTransferAnimation();
        clearWorkspaceTransferEdgeHover();
        if (!commit && workspaceTransferProxy != null && !liveDisplacedItems.isEmpty()) {
            finishLiveDisplacements(false, workspaceTransferProxy);
        }
        if (workspaceTransferProxy != null && workspaceTransferProxy.getParent() instanceof ViewGroup) {
            ((ViewGroup) workspaceTransferProxy.getParent()).removeView(workspaceTransferProxy);
        }
        workspaceTransferProxy = null;
        workspaceTransferProxyPage = -1;
        int restorePage = commit ? workspaceTransferTargetPage : workspaceTransferOriginPage;
        if (restorePage < 0) restorePage = 0;
        if (pagesStrip != null && pageViewport != null) {
            currentPage = Math.max(0, Math.min(restorePage, pagesStrip.getChildCount() - 1));
            int width = Math.max(1, pageViewport.getWidth());
            pagesStrip.setTranslationX(-currentPage * (float) width);
            pagesStrip.setClipChildren(true);
            pagesStrip.setClipToPadding(true);
            for (int p = 0; p < pagesStrip.getChildCount(); p++) {
                View child = pagesStrip.getChildAt(p);
                if (child instanceof ViewGroup) {
                    ((ViewGroup) child).setClipChildren(true);
                    ((ViewGroup) child).setClipToPadding(true);
                    child.setElevation(0f);
                }
            }
            activateClockForPage(currentPage);
            if (pageIndicator != null) {
                pageIndicator.setVisibility(prefs.getBoolean(KEY_SHOW_PAGE_INDICATOR, true)
                        ? View.VISIBLE : View.GONE);
                pageIndicator.setText((currentPage + 1) + "  /  " + pagesStrip.getChildCount());
            }
        }
        liveDragLastSafeGeometry.remove(item);
        workspaceTransferItem = null;
        workspaceTransferSourcePage = null;
        workspaceTransferOriginPage = -1;
        workspaceTransferTargetPage = -1;
        workspaceTransferEdgeHoverTargetPage = -1;
        workspaceTransferAnimationTargetPage = -1;
    }

    private void clearIconEdgeHover() {
        handler.removeCallbacks(iconEdgeSwitchRunnable);
        iconEdgeHoverTargetPage = -1;
    }

    private void updateIconEdgeHover(float rawX) {
        iconDragLastRawX = rawX;
        if (!iconDragging || pageViewport == null || pagesStrip == null
                || pageViewport.getWidth() <= 0) {
            clearIconEdgeHover();
            return;
        }
        int visiblePage = iconDragPageIndex;
        int pageCount = pagesStrip.getChildCount();
        int[] viewportLocation = new int[2];
        pageViewport.getLocationOnScreen(viewportLocation);
        int edge = LauncherUiUtils.dp(this, ICON_EDGE_SWITCH_ZONE_DP);
        int targetPage = -1;
        if (visiblePage + 1 < pageCount
                && rawX >= viewportLocation[0] + pageViewport.getWidth() - edge) {
            targetPage = visiblePage + 1;
        } else if (visiblePage > 0 && rawX <= viewportLocation[0] + edge) {
            targetPage = visiblePage - 1;
        }
        if (targetPage < 0) {
            clearIconEdgeHover();
            return;
        }
        if (targetPage == iconEdgeHoverTargetPage) return;
        handler.removeCallbacks(iconEdgeSwitchRunnable);
        iconEdgeHoverTargetPage = targetPage;
        handler.postDelayed(iconEdgeSwitchRunnable, ICON_EDGE_SWITCH_DELAY_MS);
    }

    /**
     * Reveal the adjacent screen during an icon drag without reparenting the actively
     * touched view (reparenting would cancel Android's touch target). The source page
     * temporarily draws above its neighbour and allows the dragged cell to overflow.
     * The view remains owned by its source page until the finger is released.
     */
    /**
     * Edge-triggered page changes always use the Parallax treatment, independent of
     * the user's normal page-transition preference. The dragged icon is compensated
     * against the strip translation on every frame so it stays under the finger while
     * the workspace moves underneath it.
     */
    private void switchIconDragPage(int targetPage) {
        if (iconDragPageAnimator != null) finishIconDragPageAnimation();
        View dragged = activeDragView;
        if (!iconDragging || dragged == null || !(dragged.getParent() instanceof FrameLayout)
                || pagesStrip == null || targetPage < 0
                || targetPage >= pagesStrip.getChildCount() || targetPage == iconDragPageIndex) {
            return;
        }
        int fromPage = currentPage;
        if (Math.abs(targetPage - fromPage) != 1) return;
        FrameLayout sourcePage = (FrameLayout) dragged.getParent();
        int sourcePageIndex = pagesStrip.indexOfChild(sourcePage);
        if (sourcePageIndex < 0) return;

        int direction = targetPage > fromPage ? 1 : -1;
        int pageWidth = Math.max(1, pageViewport != null && pageViewport.getWidth() > 0
                ? pageViewport.getWidth() : sourcePage.getWidth());
        View outgoing = pagesStrip.getChildAt(fromPage);
        View incoming = pagesStrip.getChildAt(targetPage);
        if (outgoing == null || incoming == null) return;

        if (targetPage != currentPage) {
            if (clockWidgetEditing) exitClockWidgetEditMode();
            dismissShortcutEditMode();
        }
        pagesStrip.animate().cancel();
        for (int i = 0; i < pagesStrip.getChildCount(); i++) {
            View face = pagesStrip.getChildAt(i);
            face.animate().cancel();
            face.setPivotX(face.getWidth() / 2f);
            face.setPivotY(face.getHeight() / 2f);
            face.setRotationX(0f);
            face.setRotationY(0f);
            face.setScaleX(1f);
            face.setScaleY(1f);
            face.setAlpha(1f);
            face.setTranslationX(0f);
            face.setTranslationZ(0f);
        }

        // Keep page state and indicators synchronized with the page being revealed.
        currentPage = targetPage;
        iconDragPageIndex = targetPage;
        activateClockForPage(targetPage);
        if (pageIndicator != null) {
            pageIndicator.setVisibility(prefs.getBoolean(KEY_SHOW_PAGE_INDICATOR, true)
                    ? View.VISIBLE : View.GONE);
            pageIndicator.setText((targetPage + 1) + "  /  " + pagesStrip.getChildCount());
        }

        // Roll back the prior page's preview before creating reservations on the
        // newly revealed page. Keep the transfer session alive until finger-up.
        if (!liveDisplacedItems.isEmpty()) finishLiveDisplacements(false, dragged);
        if (workspaceTransferProxy != null
                && workspaceTransferProxy.getParent() instanceof ViewGroup) {
            ((ViewGroup) workspaceTransferProxy.getParent()).removeView(workspaceTransferProxy);
        }
        workspaceTransferProxy = null;
        workspaceTransferProxyPage = -1;
        workspaceTransferTargetPage = targetPage;
        if (targetPage != workspaceTransferOriginPage) {
            createWorkspaceTransferProxy(targetPage);
            updateWorkspaceTransferDisplacements(dragged, iconDragLastRawX, iconDragLastRawY,
                    workspaceTransferFingerOffsetX, workspaceTransferFingerOffsetY);
        }
        pagesStrip.setTranslationX(-fromPage * (float) pageWidth);
        preparePageLayersForSwipe(fromPage);
        iconDragParallaxChildAlphas.clear();
        if (outgoing instanceof ViewGroup) {
            ViewGroup outgoingGroup = (ViewGroup) outgoing;
            for (int i = 0; i < outgoingGroup.getChildCount(); i++) {
                View child = outgoingGroup.getChildAt(i);
                if (child == dragged) continue;
                iconDragParallaxChildAlphas.put(child, child.getAlpha());
            }
        }
        if (incoming instanceof ViewGroup) {
            ViewGroup incomingGroup = (ViewGroup) incoming;
            for (int i = 0; i < incomingGroup.getChildCount(); i++) {
                View child = incomingGroup.getChildAt(i);
                if (child == dragged) continue;
                if (!iconDragParallaxChildAlphas.containsKey(child)) {
                    iconDragParallaxChildAlphas.put(child, child.getAlpha());
                }
                child.setAlpha(0f);
            }
        }

        outgoing.setPivotX(direction > 0 ? outgoing.getWidth() : 0f);
        outgoing.setPivotY(outgoing.getHeight() * 0.5f);
        incoming.setPivotX(direction > 0 ? 0f : incoming.getWidth());
        incoming.setPivotY(incoming.getHeight() * 0.5f);

        iconDragParallaxView = dragged;
        iconDragParallaxOutgoing = outgoing;
        iconDragParallaxIncoming = incoming;
        iconDragParallaxTargetPage = targetPage;
        iconDragParallaxDirection = direction;
        iconDragParallaxPageWidth = pageWidth;
        iconDragParallaxStartTranslationX = dragged.getTranslationX();

        final android.animation.ValueAnimator animator =
                android.animation.ValueAnimator.ofFloat(0f, 1f);
        iconDragPageAnimator = animator;
        animator.setDuration(animationDuration(220L));
        animator.setInterpolator(new android.view.animation.DecelerateInterpolator());
        animator.addUpdateListener(valueAnimator -> {
            if (iconDragPageAnimator != animator || pagesStrip == null) return;
            float progress = ((Float) valueAnimator.getAnimatedValue()).floatValue();
            pagesStrip.setTranslationX(-fromPage * (float) pageWidth
                    - direction * pageWidth * progress);
            if (iconDragParallaxView != null) {
                iconDragParallaxView.setTranslationX(iconDragParallaxStartTranslationX
                        + direction * pageWidth * progress);
            }
            outgoing.setScaleX(1f - 0.045f * progress);
            outgoing.setScaleY(1f - 0.025f * progress);
            incoming.setScaleX(0.955f + 0.045f * progress);
            incoming.setScaleY(0.975f + 0.025f * progress);
            for (java.util.Map.Entry<View, Float> entry : iconDragParallaxChildAlphas.entrySet()) {
                View child = entry.getKey();
                float baseAlpha = entry.getValue().floatValue();
                if (child.getParent() == outgoing) child.setAlpha(baseAlpha * (1f - progress));
                else if (child.getParent() == incoming) child.setAlpha(baseAlpha * progress);
            }
        });
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(android.animation.Animator animation) {
                if (iconDragPageAnimator == animator) finishIconDragPageAnimation();
            }
        });
        // Allow the dragged cell to cross the page seam without clipping while it
        // remains attached to its original touch target.
        pagesStrip.setClipChildren(false);
        pagesStrip.setClipToPadding(false);
        sourcePage.setClipChildren(false);
        sourcePage.setClipToPadding(false);
        sourcePage.setElevation(LauncherUiUtils.dp(this, 30));
        dragged.bringToFront();
        setWorkspaceGridVisible(true, dragged);
        refreshWorkspaceGrid();
        animator.start();
    }

    /** Finish a partially-running parallax transition without losing the drag offset. */
    private void finishIconDragPageAnimation() {
        android.animation.ValueAnimator animator = iconDragPageAnimator;
        if (animator == null) return;
        iconDragPageAnimator = null;
        animator.removeAllUpdateListeners();
        animator.removeAllListeners();
        animator.cancel();

        View dragged = iconDragParallaxView;
        int targetPage = iconDragParallaxTargetPage;
        int direction = iconDragParallaxDirection;
        int pageWidth = iconDragParallaxPageWidth;
        if (dragged != null && dragged.getParent() instanceof FrameLayout) {
            ViewGroup.LayoutParams raw = dragged.getLayoutParams();
            if (raw instanceof FrameLayout.LayoutParams) {
                FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) raw;
                // Finish from the latest pointer-updated position, not a stale
                // animation-start margin; the finger can still move during the animation.
                lp.leftMargin += direction * pageWidth;
                dragged.setLayoutParams(lp);
            }
            dragged.setTranslationX(iconDragParallaxStartTranslationX);
        }
        if (pagesStrip != null && targetPage >= 0) {
            pagesStrip.setTranslationX(-targetPage * (float) pageWidth);
            for (int i = 0; i < pagesStrip.getChildCount(); i++) {
                View face = pagesStrip.getChildAt(i);
                face.setAlpha(1f);
                face.setRotationX(0f);
                face.setRotationY(0f);
                face.setScaleX(1f);
                face.setScaleY(1f);
                face.setTranslationX(0f);
                face.setTranslationZ(0f);
            }
        }
        for (java.util.Map.Entry<View, Float> entry : iconDragParallaxChildAlphas.entrySet()) {
            View child = entry.getKey();
            if (child.getParent() != null) child.setAlpha(entry.getValue().floatValue());
        }
        iconDragParallaxChildAlphas.clear();
        iconDragParallaxView = null;
        iconDragParallaxOutgoing = null;
        iconDragParallaxIncoming = null;
        iconDragParallaxTargetPage = -1;
        releasePageLayers();
    }

    private void restoreIconDragPageVisuals() {
        if (pagesStrip == null) return;
        pagesStrip.setClipChildren(true);
        pagesStrip.setClipToPadding(true);
        if (iconOriginalPageIndex >= 0 && iconOriginalPageIndex < pagesStrip.getChildCount()) {
            ViewGroup source = (ViewGroup) pagesStrip.getChildAt(iconOriginalPageIndex);
            source.setClipChildren(true);
            source.setClipToPadding(true);
            source.setElevation(0f);
        }
    }

    private void installHomeIconTouch(View cell, String packageName) {
        cell.setOnClickListener(v -> {
            ResolveInfo app = findApp(packageName);
            if (!iconDragging && !longPressTriggered && app != null) launchApp(app, cell);
        });
        cell.setOnLongClickListener(null);
        final boolean[] iconReparenting = new boolean[1];
        cell.setOnTouchListener((view, event) -> {
            if (workspaceIconReparenting || iconReparenting[0]) return true;
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                clearIconEdgeHover();
                if (iconDragPageAnimator != null) finishIconDragPageAnimation();
                iconTouchRawX = event.getRawX();
                iconTouchRawY = event.getRawY();
                iconDragFingerOffsetY = Math.max(0f,
                        Math.min(event.getY(), Math.max(1, view.getHeight())));
                iconDragLastRawX = event.getRawX();
                iconDragLastRawY = event.getRawY();
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
                        LauncherUiUtils.stopMoveHint(view);
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
                iconDragLastRawX = event.getRawX();
                iconDragLastRawY = event.getRawY();
                workspaceTransferLastRawX = event.getRawX();
                workspaceTransferLastRawY = event.getRawY();
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
                    // App icons use a dedicated parallax page-slide gesture, but share
                    // the destination collision transaction used by widgets/shortcuts.
                    beginWorkspaceTransferDrag(view, iconOriginalPageIndex,
                            event.getRawX(), event.getRawY(),
                            Math.max(1, view.getWidth()) / 2f,
                            Math.max(1, view.getHeight()) / 2f);
                }
                if (iconDragging) {
                    // A sustained drag at the extreme edge reveals the adjacent existing
                    // page after 200 ms. The finger stays down and continues controlling
                    // the same view throughout the page switch.
                    updateIconEdgeHover(event.getRawX());
                    FrameLayout page = (FrameLayout) view.getParent();
                    int sourcePageIndex = pagesStrip.indexOfChild(page);
                    int[] pageLocation = new int[2];
                    page.getLocationOnScreen(pageLocation);
                    FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) view.getLayoutParams();
                    int maxX = Math.max(0, page.getWidth() - view.getWidth());
                    int maxY = Math.max(0, page.getHeight() - view.getHeight());
                    // Convert screen coordinates into the source page's own coordinate space.
                    // When the visible target is the other page, the source cell deliberately
                    // extends beyond its own bounds so it can remain under the finger.
                    float pageOffsetX = view.getTranslationX();
                    int desiredLeft = Math.round(event.getRawX() - pageLocation[0]
                            - view.getWidth() / 2f - pageOffsetX);
                    lp.leftMargin = iconDragPageIndex == sourcePageIndex
                            ? Math.max(0, Math.min(maxX, desiredLeft)) : desiredLeft;
                    int requestedTop = Math.round(event.getRawY()
                            - pageLocation[1] - iconDragFingerOffsetY);
                    // The guide grid remains active, but the dragged app follows the
                    // finger continuously. Grid snapping is applied only at drop time.
                    lp.topMargin = Math.max(0, Math.min(maxY, requestedTop));
                    view.setLayoutParams(lp);
                    // Preview collision displacement during an app drag on its current
                    // page. When the edge switch starts, that page's preview is rolled back
                    // and the destination page gets its own independent placement transaction.
                    if (iconDragPageIndex == sourcePageIndex && iconDragPageAnimator == null) {
                        scheduleLiveDisplacementUpdate(page, view);
                    } else if (workspaceTransferTargetPage >= 0
                            && workspaceTransferTargetPage != workspaceTransferOriginPage) {
                        updateWorkspaceTransferDisplacements(view, event.getRawX(), event.getRawY(),
                                workspaceTransferFingerOffsetX, workspaceTransferFingerOffsetY);
                    }
                    refreshWorkspaceGrid();
                }
                return true;
            }
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            iconDragLastRawX = event.getRawX();
            iconDragLastRawY = event.getRawY();
            workspaceTransferLastRawX = event.getRawX();
            workspaceTransferLastRawY = event.getRawY();
            clearIconEdgeHover();
            if (iconDragPageAnimator != null) finishIconDragPageAnimation();
            LauncherUiUtils.stopMoveHint(view);
            if (iconDragging) {
                // The 10% move scale is a drag affordance, not the drop footprint.
                // Cancel its property animator and restore canonical geometry BEFORE
                // collision planning, otherwise a free drop can trigger a false shuffle.
                view.animate().cancel();
                view.setScaleX(1f);
                view.setScaleY(1f);
                view.setAlpha(1f);
            }
            // Finish resize gestures here before they can fall through to launcher
            // gesture handling. Without this reset, the resize flag stayed armed
            // after finger-up and later swipes could keep changing clock dimensions.
            if (clockResizeEdge != 0) {
                handler.removeCallbacks(clockWidgetLongPress);
                handler.removeCallbacks(clockWidgetEditLongPress);
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
                            finishWorkspaceTransferDrag(view, false);
                            if (!liveDisplacedItems.isEmpty()) finishLiveDisplacements(false, view);
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
                            restoreIconDragPageVisuals();
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
                    int destinationPage = Math.max(0, Math.min(pagesStrip.getChildCount() - 1,
                            iconDragPageIndex >= 0 ? iconDragPageIndex : sourcePage));
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
                    float desiredTop = event.getRawY()
                            - viewportLocation[1] - iconDragFingerOffsetY;
                    int targetRow = homeGridRowFromTop(
                            desiredTop, rows, targetHeight, cellHeight);
                    int sourceCellWidth = Math.max(1, sourceView.getWidth() / columns);
                    int sourceCellHeight = slotHeight;
                    int sourceRows = Math.max(1, sourceView.getHeight() / sourceCellHeight);
                    int sourceCol = Math.max(0, Math.min(columns - 1,
                            Math.round(iconOriginalLeft / (float) sourceCellWidth)));
                    int sourceRow = homeGridRowFromTop(
                            iconOriginalTop, sourceRows, sourceView.getHeight(), sourceCellHeight);
                    boolean[][] occupied = new boolean[rows][columns];
                    View occupant = null;
                    String otherPackage = null;
                    for (int i = 0; i < targetPage.getChildCount(); i++) {
                        View candidate = targetPage.getChildAt(i);
                        if (candidate == view || !(candidate.getTag() instanceof String)
                                || !((String) candidate.getTag()).startsWith("launcher_app:")) continue;
                        FrameLayout.LayoutParams cp = (FrameLayout.LayoutParams) candidate.getLayoutParams();
                        int cc = Math.max(0, Math.min(columns - 1, Math.round((float) cp.leftMargin / cellWidth)));
                        int cr = homeGridRowFromTop(
                                cp.topMargin, rows, targetHeight, cellHeight);
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
                        otherLp.width = sourceCellWidth;
                        otherLp.height = sourceCellHeight;
                        otherLp.gravity = Gravity.TOP | Gravity.LEFT;
                        otherLp.leftMargin = sourceCol * sourceCellWidth;
                        otherLp.topMargin = homeGridRowTop(
                                sourceRow, sourceRows, sourceView.getHeight(), sourceCellHeight);
                        if (movedAcrossPage) {
                            // The destination app cell is swapped back to the vacated
                            // source slot before collision validation, rather than leaving
                            // a duplicate tagged app in the destination page.
                            workspaceIconReparenting = true;
                            targetPage.removeView(occupant);
                            sourceView.addView(occupant, otherLp);
                            workspaceIconReparenting = false;
                        } else {
                            occupant.setLayoutParams(otherLp);
                        }
                        saveIconSlot(otherPackage, sourceCol, sourceRow, columns);
                        if (movedAcrossPage) {
                            prefs.edit().putInt(KEY_ICON_PAGE + otherPackage, sourcePage).apply();
                        }
                    }
                    // Commit against the exact snapped release slot, not the last
                    // intermediate pointer frame. This prevents a final grid snap from
                    // creating a collision that only the slow post-render resolver would see.
                    boolean hadLiveDisplacementPlan;
                    FrameLayout.LayoutParams finalIconParams =
                            new FrameLayout.LayoutParams(cellWidth, cellHeight,
                                    Gravity.TOP | Gravity.LEFT);
                    finalIconParams.leftMargin = targetCol * cellWidth;
                    finalIconParams.topMargin = homeGridRowTop(
                            targetRow, rows, targetHeight, cellHeight);
                    view.animate().cancel();
                    view.setTranslationX(0f);
                    view.setTranslationY(0f);

                    if (!movedAcrossPage) {
                        view.setLayoutParams(finalIconParams);
                        updateLiveDisplacements(sourceView, view);
                    } else {
                        // Snap the destination-page collision proxy to the exact app-grid
                        // cell that will be committed. This catches overlap created by the
                        // release snap, even when the final MOVE event landed between cells.
                        workspaceTransferTargetPage = destinationPage;
                        if (workspaceTransferProxy == null
                                || workspaceTransferProxy.getParent() != targetPage) {
                            if (workspaceTransferProxy != null
                                    && workspaceTransferProxy.getParent() instanceof ViewGroup) {
                                ((ViewGroup) workspaceTransferProxy.getParent())
                                        .removeView(workspaceTransferProxy);
                            }
                            workspaceTransferProxy = null;
                            workspaceTransferProxyPage = -1;
                            createWorkspaceTransferProxy(destinationPage);
                        }
                        if (workspaceTransferProxy != null
                                && workspaceTransferProxy.getLayoutParams()
                                        instanceof FrameLayout.LayoutParams) {
                            workspaceTransferProxy.setScaleX(1f);
                            workspaceTransferProxy.setScaleY(1f);
                            workspaceTransferProxy.setPivotX(0f);
                            workspaceTransferProxy.setPivotY(0f);
                            // The preview proxy follows the finger with translations;
                            // clear them before snapping its layout margins to the final
                            // app-grid slot, otherwise release validation double-offsets it.
                            workspaceTransferProxy.setTranslationX(0f);
                            workspaceTransferProxy.setTranslationY(0f);
                            FrameLayout.LayoutParams proxyLp =
                                    (FrameLayout.LayoutParams) workspaceTransferProxy.getLayoutParams();
                            proxyLp.leftMargin = targetCol * cellWidth;
                            proxyLp.topMargin = homeGridRowTop(
                                    targetRow, rows, targetHeight, cellHeight);
                            workspaceTransferProxy.setLayoutParams(proxyLp);
                            updateLiveDisplacements(targetPage, workspaceTransferProxy);
                        }
                        // The icon stays in its original parent during MOVE. Reparent only
                        // once the release cell is known so collision reservations survive UP.
                        workspaceIconReparenting = true;
                        sourceView.removeView(view);
                        targetPage.addView(view, finalIconParams);
                        workspaceIconReparenting = false;
                        // Finalize collision checks against the real snapped icon, not
                        // solely its temporary destination-page proxy.
                        updateLiveDisplacements(targetPage, view);
                    }
                    hadLiveDisplacementPlan = !liveDisplacedItems.isEmpty();
                    finishWorkspaceTransferDrag(view, true);
                    int committedPage = destinationPage;
                    // Save the intended slot first. If the final visual collision check
                    // must move this item, its corrected slot is persisted afterward.
                    saveIconSlot(packageName, targetCol, targetRow, columns);
                    prefs.edit().putInt(KEY_ICON_PAGE + packageName, committedPage).apply();
                    finishLiveDisplacements(true, view);
                    ensurePageExists(committedPage);
                    currentPage = committedPage;
                    restoreIconDragPageVisuals();
                    view.setTranslationX(0f);
                    view.setScaleX(1f);
                    view.setScaleY(1f);
                    view.setAlpha(1f);
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
                        View pageView = pagesStrip.getChildAt(collisionPageIndex);
                        if (!(pageView instanceof FrameLayout)) return;
                        FrameLayout collisionPage = (FrameLayout) pageView;
                        collisionPage.post(() -> {
                            if (collisionPage.getParent() != pagesStrip) return;
                            // A committed live displacement plan already reserved and
                            // persisted every affected rectangle. Do not shuffle that layout
                            // a second time after the newly rendered app cells appear.
                            if (!hadLiveDisplacementPlan) {
                                resolveAllWorkspaceOverlaps(collisionPage, null);
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
    private void dismissShortcutEditMode() {
        setWorkspaceGridVisible(false, null);
    }

    private void showPage(int requestedPage, boolean animate) {
        if (pages.isEmpty()) rebuildPages();
        int lastPage = Math.max(0, pages.size() - 1);
        int targetPage = Math.max(0, Math.min(lastPage, requestedPage));
        if (targetPage != currentPage) {
            if (clockWidgetEditing) exitClockWidgetEditMode();
            dismissShortcutEditMode();
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
        // Pre-create GPU layers only when a transition will actually use them.
        // HOME-intent recovery and other instant page changes should not allocate
        // hardware layers just to release them again in the same frame.
        if (animate) preparePageLayersForSwipe(currentPage);
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
        if (drawerGrid == null || drawerAdapter == null || drawerAdapter.getCount() == 0) return true;
        // GridView child bounds can be empty/stale during rebinding.
        return !drawerGrid.canScrollVertically(-1);
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

    /**
     * Lightweight entrance effect when Android brings this singleTask launcher forward
     * after the system HOME button is pressed in another app. Uses ViewPropertyAnimator,
     * supported on Android 6, and never starts another Activity or rebuilds the workspace.
     */
    private void animateHomeReturnFromApp() {
        if (homeScreen == null || !homeScreen.isAttachedToWindow()) return;
        homeScreen.animate().cancel();
        final float density = getResources().getDisplayMetrics().density;
        homeScreen.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        homeScreen.setVisibility(View.VISIBLE);
        homeScreen.setAlpha(0.72f);
        homeScreen.setScaleX(0.965f);
        homeScreen.setScaleY(0.965f);
        homeScreen.setTranslationY(LauncherUiUtils.dp(this, 10));
        homeScreen.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .translationY(0f)
                .setDuration(LauncherMotion.duration(this, 220L))
                .withEndAction(() -> {
                    if (homeScreen != null) {
                        homeScreen.setAlpha(1f);
                        homeScreen.setScaleX(1f);
                        homeScreen.setScaleY(1f);
                        homeScreen.setTranslationY(0f);
                        homeScreen.setLayerType(View.LAYER_TYPE_NONE, null);
                    }
                })
                .start();
    }

    /** Fast path for the system HOME intent: no overlapping screen/page animations. */
    private void showHomeImmediately() {
        drawerOpen = false;
        if (homeScreen != null) {
            homeScreen.animate().cancel();
            homeScreen.setVisibility(View.VISIBLE);
            homeScreen.setAlpha(1f);
            homeScreen.setScaleX(1f);
            homeScreen.setScaleY(1f);
            homeScreen.setRotationX(0f);
            homeScreen.setRotationY(0f);
            homeScreen.setTranslationX(0f);
            homeScreen.setTranslationY(0f);
            homeScreen.setTranslationZ(0f);
            homeScreen.setLayerType(View.LAYER_TYPE_NONE, null);
        }
        if (drawerScreen != null) {
            drawerScreen.animate().cancel();
            drawerScreen.setVisibility(View.GONE);
            drawerScreen.setAlpha(1f);
            drawerScreen.setScaleX(1f);
            drawerScreen.setScaleY(1f);
            drawerScreen.setRotationX(0f);
            drawerScreen.setRotationY(0f);
            drawerScreen.setTranslationX(0f);
            drawerScreen.setTranslationY(0f);
            drawerScreen.setTranslationZ(0f);
            drawerScreen.setLayerType(View.LAYER_TYPE_NONE, null);
        }
        if (clockWidgetEditing) exitClockWidgetEditMode();
        dismissShortcutEditMode();
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
        // Adding an app can collide with the clock even on pages with no widgets.
        // Run the same all-item collision pass after the new icon cells are measured.
        final int addedAppPageIndex = targetPage;
        pageViewport.post(() -> {
            if (pagesStrip == null || addedAppPageIndex < 0
                    || addedAppPageIndex >= pagesStrip.getChildCount()) return;
            View targetView = pagesStrip.getChildAt(addedAppPageIndex);
            if (!(targetView instanceof FrameLayout)) return;
            FrameLayout targetFrame = (FrameLayout) targetView;
            targetFrame.post(() -> {
                if (targetFrame.getParent() == pagesStrip) {
                    resolveAllWorkspaceOverlaps(targetFrame, null);
                }
            });
        });
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

        // Use a rendered app cell as the artwork/label geometry template. A full grid
        // cell is not the icon's visual footprint and falsely blocks slots under a
        // widget's empty cell margins.
        View appIconTemplate = null;
        if (pagesStrip != null) {
            for (int pageIndexToSearch = 0; pageIndexToSearch < pagesStrip.getChildCount()
                    && appIconTemplate == null; pageIndexToSearch++) {
                View candidatePage = pagesStrip.getChildAt(pageIndexToSearch);
                if (!(candidatePage instanceof ViewGroup)) continue;
                ViewGroup group = (ViewGroup) candidatePage;
                for (int childIndex = 0; childIndex < group.getChildCount(); childIndex++) {
                    View child = group.getChildAt(childIndex);
                    if (isWorkspaceAppIcon(child)) {
                        appIconTemplate = child;
                        break;
                    }
                }
            }
        }

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
                int row = homeGridRowFromTop(top, rows, height, cellHeight);
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
                int cellLeft = col * cellWidth;
                int cellTop = homeGridRowTop(row, rows, height, cellHeight);
                android.graphics.Rect cell = new android.graphics.Rect(
                        cellLeft, cellTop,
                        Math.min(width, (col + 1) * cellWidth),
                        Math.min(height, cellTop + cellHeight));
                // Reserve only the visible app artwork and painted label. For safety on
                // a workspace with no existing app cell template, fall back to the full
                // cell rather than guessing typography geometry.
                android.graphics.Rect candidateAppVisual = appIconTemplate != null
                        ? collisionRectAt(appIconTemplate, cellLeft, cellTop, cellWidth, cellHeight)
                        : cell;
                boolean blocked = false;
                if (page != null) {
                    for (int i = 0; i < page.getChildCount(); i++) {
                        View child = page.getChildAt(i);
                        Object rawTag = child.getTag();
                        if (!(rawTag instanceof String)) continue;
                        String tag = (String) rawTag;
                        if (tag.startsWith("launcher_app:")) continue;
                        if (!(tag.startsWith("launcher_widget:")
                                || "launcher_contact_shortcut".equals(tag)
                                || "launcher_clock_widget".equals(tag))) continue;
                        int[] geometry = itemGeometry(child);
                        if (geometry[2] <= 0 || geometry[3] <= 0) continue;
                        android.graphics.Rect bounds = collisionRect(child, geometry);
                        if (android.graphics.Rect.intersects(candidateAppVisual, bounds)) {
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
                String shortcutId = Long.toHexString(System.currentTimeMillis()) + "_" +
                        Integer.toHexString((int) (Math.random() * Integer.MAX_VALUE));
                java.util.ArrayList<String> shortcutIds = getSavedShortcutIds();
                shortcutIds.add(shortcutId);
                saveShortcutIds(shortcutIds);
                prefs.edit()
                        .putString(shortcutPrefKey(KEY_SHORTCUT_INTENT, shortcutId), shortcutIntent.toUri(Intent.URI_INTENT_SCHEME))
                        .putString(shortcutPrefKey(KEY_SHORTCUT_LABEL, shortcutId), shortcutLabel)
                        .putInt(shortcutPrefKey(KEY_SHORTCUT_PAGE, shortcutId), pendingWidgetTargetPage)
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
                    try (java.io.FileOutputStream output = openFileOutput("contact_shortcut_icon_" + shortcutId + ".png", MODE_PRIVATE)) {
                        shortcutBitmap.compress(Bitmap.CompressFormat.PNG, 100, output);
                        prefs.edit().putString(shortcutPrefKey(KEY_SHORTCUT_ICON, shortcutId), "contact_shortcut_icon_" + shortcutId + ".png").apply();
                    } catch (Exception ignored) {}
                } else {
                    prefs.edit().remove(shortcutPrefKey(KEY_SHORTCUT_ICON, shortcutId)).apply();
                }
                handler.post(() -> {
                    renderSavedContactShortcut();
                    if (!hasRenderedContactShortcut(shortcutId)) {
                        removeSavedShortcut(shortcutId);
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
            int originalWidth = width;
            int originalHeight = height;
            int selectedScalePercent = 0;
            int[] freeWidgetPosition = null;
            for (int scalePercent = 100; scalePercent >= 20; scalePercent -= 20) {
                int candidateWidth = Math.max(1, Math.round(originalWidth * scalePercent / 100f));
                int candidateHeight = Math.max(1, Math.round(originalHeight * scalePercent / 100f));
                int[] candidatePosition =
                        findFreeWidgetPosition(page, candidateWidth, candidateHeight);
                if (candidatePosition == null) continue;
                width = candidateWidth;
                height = candidateHeight;
                freeWidgetPosition = candidatePosition;
                selectedScalePercent = scalePercent;
                break;
            }
            if (freeWidgetPosition == null) {
                try { widgetHost.deleteAppWidgetId(widgetId); } catch (Exception ignored) {}
                Toast.makeText(this, "No space available, remove first.", Toast.LENGTH_LONG).show();
                return;
            }
            // The placement search above has already made the decision; suppress the
            // controller's older wording if its defensive second check rejects placement.
            addWidgetContainer(page, widget, widgetId, resolvedPage,
                    freeWidgetPosition[0], freeWidgetPosition[1], width, height, false);
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
                if (selectedScalePercent < 100) {
                    Toast.makeText(this, "Widget resized to " + selectedScalePercent
                            + "% to fit available space.", Toast.LENGTH_SHORT).show();
                }
                saveWidgetGeometry(widgetId, resolvedPage, container);
                final FrameLayout placedWidget = container;
                final FrameLayout placedPage = page;
                page.post(() -> resolveWidgetIconCollisions(placedPage, placedWidget));
            } else {
                // The placement helper rejected the widget; never persist a phantom ID.
                try { widgetHost.deleteAppWidgetId(widgetId); } catch (Exception ignored) {}
                Toast.makeText(this, "No space available, remove first.", Toast.LENGTH_LONG).show();
                return;
            }
            widgetHost.startListening();
        });
    }

    /**
     * Prepare a widget drop before reparenting it. Cross-screen drops are previewed
     * with a destination proxy, so resolve that proxy synchronously and verify that
     * the destination can hold the widget after the planned neighbour movements.
     */
    private int[] prepareWorkspaceWidgetDrop(FrameLayout targetPage, View widget,
            float rawX, float rawY, float fingerOffsetX, float fingerOffsetY) {
        if (targetPage == null || widget == null || targetPage.getWidth() <= 0
                || targetPage.getHeight() <= 0 || workspaceTransferItem != widget
                || workspaceTransferTargetPage < 0 || pagesStrip == null
                || workspaceTransferTargetPage >= pagesStrip.getChildCount()) return null;

        boolean crossScreen = workspaceTransferTargetPage != workspaceTransferOriginPage;
        if (crossScreen) {
            updateWorkspaceTransferDisplacements(widget, rawX, rawY,
                    fingerOffsetX, fingerOffsetY);
            View proxy = workspaceTransferProxy;
            if (proxy == null || proxy.getParent() != targetPage) return null;
            // A next-frame update is too late for the release decision; flush the
            // latest pointer position before checking the destination's end-state.
            updateLiveDisplacements(targetPage, proxy);
        } else {
            if (widget.getParent() != targetPage) return null;
            updateLiveDisplacements(targetPage, widget);
        }

        ViewGroup.LayoutParams raw = widget.getLayoutParams();
        int width = raw instanceof FrameLayout.LayoutParams
                && ((FrameLayout.LayoutParams) raw).width > 0
                ? ((FrameLayout.LayoutParams) raw).width : widget.getWidth();
        int height = raw instanceof FrameLayout.LayoutParams
                && ((FrameLayout.LayoutParams) raw).height > 0
                ? ((FrameLayout.LayoutParams) raw).height : widget.getHeight();
        if (width <= 0 || height <= 0
                || !(raw instanceof FrameLayout.LayoutParams)) return null;
        FrameLayout.LayoutParams widgetParams = (FrameLayout.LayoutParams) raw;
        int preferredX = widgetParams.leftMargin;
        int preferredY = widgetParams.topMargin;
        for (int scalePercent = 100; scalePercent >= 20; scalePercent -= 20) {
            int candidateWidth = Math.max(1, Math.round(width * scalePercent / 100f));
            int candidateHeight = Math.max(1, Math.round(height * scalePercent / 100f));
            int[] position = findFreeWidgetPosition(targetPage, candidateWidth, candidateHeight,
                    widget, true, preferredX, preferredY);
            if (position != null) {
                // {left, top, width, height}; percentages are applied uniformly so
                // the widget's aspect ratio is preserved up to pixel rounding.
                return new int[]{position[0], position[1], candidateWidth, candidateHeight};
            }
        }
        return null;
    }

    private int[] findFreeWidgetPosition(FrameLayout page, int widgetWidth, int widgetHeight) {
        return findFreeWidgetPosition(page, widgetWidth, widgetHeight, null, false, -1, -1);
    }

    private int[] findFreeWidgetPosition(FrameLayout page, int widgetWidth, int widgetHeight,
            View ignoredView, boolean usePlannedDisplacements) {
        return findFreeWidgetPosition(page, widgetWidth, widgetHeight,
                ignoredView, usePlannedDisplacements, -1, -1);
    }

    private int[] findFreeWidgetPosition(FrameLayout page, int widgetWidth, int widgetHeight,
            View ignoredView, boolean usePlannedDisplacements, int preferredLeft, int preferredTop) {
        if (page == null) return null;
        int pageWidth = page.getWidth() > 0 ? page.getWidth()
                : Math.max(1, getResources().getDisplayMetrics().widthPixels - LauncherUiUtils.dp(this, 40));
        int pageHeight = page.getHeight() > 0 ? page.getHeight()
                : Math.max(LauncherUiUtils.dp(this, 240), getResources().getDisplayMetrics().heightPixels - LauncherUiUtils.dp(this, 260));
        int gap = Math.max(1, LauncherUiUtils.dp(this, 5));
        int minLeft = gap, minTop = gap;
        int maxLeft = pageWidth - widgetWidth - gap;
        int maxTop = pageHeight - widgetHeight - gap;

        // The dock is a hard lower boundary for widget placement.
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

        java.util.ArrayList<android.graphics.Rect> occupied =
                new java.util.ArrayList<android.graphics.Rect>();
        java.util.TreeSet<Integer> xCandidates = new java.util.TreeSet<Integer>();
        java.util.TreeSet<Integer> yCandidates = new java.util.TreeSet<Integer>();
        int step = Math.max(1, LauncherUiUtils.dp(this, 4));
        for (int x = minLeft; x <= maxLeft; x += step) xCandidates.add(x);
        for (int y = minTop; y <= maxTop; y += step) yCandidates.add(y);
        // A stepped scan must never skip the exact right/bottom legal position.
        xCandidates.add(maxLeft);
        yCandidates.add(maxTop);

        for (int i = 0; i < page.getChildCount(); i++) {
            View child = page.getChildAt(i);
            if (child == ignoredView) continue;
            Object rawTag = child.getTag();
            if (!(rawTag instanceof String)) continue;
            String tag = (String) rawTag;
            if (!(tag.startsWith("launcher_widget:") || tag.startsWith("launcher_app:")
                    || "launcher_contact_shortcut".equals(tag)
                    || "launcher_clock_widget".equals(tag))) continue;

            int[] geometry = itemGeometry(child);
            if (geometry[2] <= 0 || geometry[3] <= 0) continue;
            int[] plannedTarget = usePlannedDisplacements
                    ? liveDisplacementTargets.get(child) : null;
            android.graphics.Rect bounds = plannedTarget == null
                    ? collisionRect(child, geometry)
                    : collisionRectAt(child, plannedTarget[0], plannedTarget[1],
                            geometry[2], geometry[3]);
            // Keep obstacle bounds raw. The candidate rectangle is expanded by the
            // 5dp clearance below; expanding both sides here as well counted the gap
            // twice and incorrectly rejected tight but valid free spaces.
            occupied.add(bounds);
            xCandidates.add(Math.max(minLeft, Math.min(maxLeft, bounds.left - widgetWidth - gap)));
            xCandidates.add(Math.max(minLeft, Math.min(maxLeft, bounds.right + gap)));
            yCandidates.add(Math.max(minTop, Math.min(maxTop, bounds.top - widgetHeight - gap)));
            yCandidates.add(Math.max(minTop, Math.min(maxTop, bounds.bottom + gap)));
        }

        int bestX = -1, bestY = -1;
        long bestDistance = Long.MAX_VALUE;
        // Prefer the earliest row/column when there is no saved-position preference,
        // but evaluate all exact obstacle edges so narrow free pockets are not missed.
        for (Integer yValue : yCandidates) {
            int y = yValue.intValue();
            for (Integer xValue : xCandidates) {
                int x = xValue.intValue();
                android.graphics.Rect candidate = new android.graphics.Rect(
                        x - gap, y - gap, x + widgetWidth + gap, y + widgetHeight + gap);
                boolean blocked = false;
                for (android.graphics.Rect obstacle : occupied) {
                    if (android.graphics.Rect.intersects(candidate, obstacle)) {
                        blocked = true;
                        break;
                    }
                }
                if (blocked) continue;
                long distance = preferredLeft >= 0 && preferredTop >= 0
                        ? Math.abs((long) x - preferredLeft) + Math.abs((long) y - preferredTop)
                        : (long) (y - minTop) * Math.max(1, pageWidth) + (x - minLeft);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestX = x;
                    bestY = y;
                }
            }
        }
        return bestX < 0 ? null : new int[]{bestX, bestY};
    }

    private boolean containsWidgetId(String stored, int widgetId) {
        return WidgetController.containsWidgetId(stored, widgetId);
    }

    private void addWidgetContainer(FrameLayout page, AppWidgetHostView widget, int widgetId, int pageIndex,
            int x, int y, int width, int height) {
        addWidgetContainer(page, widget, widgetId, pageIndex, x, y, width, height, true);
    }

    private void addWidgetContainer(FrameLayout page, AppWidgetHostView widget, int widgetId, int pageIndex,
            int x, int y, int width, int height, boolean showNoSpaceToast) {
        widgetInteractionController.addWidgetContainer(
                page, widget, widgetId, pageIndex, x, y, width, height, showNoSpaceToast);
    }

    private void saveWidgetGeometry(int widgetId, int pageIndex, FrameLayout container) {
        WidgetController.saveGeometry(prefs, KEY_WIDGET_IDS, widgetId, pageIndex, container);
    }

    private void updatePlacedWidgetSize(int widgetId, FrameLayout container) {
        if (container == null
                || !(container.getLayoutParams() instanceof FrameLayout.LayoutParams)) return;
        AppWidgetHostView hostView = null;
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            if (child instanceof AppWidgetHostView) {
                hostView = (AppWidgetHostView) child;
                break;
            }
        }
        if (hostView == null) return;
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) container.getLayoutParams();
        WidgetController.updateWidgetSize(widgetManager, widgetId, hostView,
                getResources().getDisplayMetrics().density, lp.width, lp.height);
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
            if (stored.isEmpty()) {
                widgetRestoreRetryCount = 0;
                widgetRestoreRetryScheduled = false;
                handler.removeCallbacks(widgetRestoreRetry);
                return;
            }
            boolean retryNeeded = false;
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
                        // Null metadata is not proof that the user removed this widget;
                        // it is commonly transient while an application is updating.
                        // Keep the stored ID and retry, rather than silently deleting it.
                        retryNeeded = true;
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
                    FrameLayout restorePage = page;
                    int resolvedPageIndex = pageIndex;
                    // Restoring a saved widget must not fail merely because its original
                    // page became crowded after an update. Keep the saved page when any
                    // valid placement exists there; otherwise try the other page.
                    if (findFreeWidgetPosition(restorePage, width, height) == null) {
                        int otherPageIndex = pageIndex == 0 ? 1 : 0;
                        if (otherPageIndex < pagesStrip.getChildCount()
                                && pagesStrip.getChildAt(otherPageIndex) instanceof FrameLayout) {
                            FrameLayout otherPage = (FrameLayout) pagesStrip.getChildAt(otherPageIndex);
                            int[] otherPosition = findFreeWidgetPosition(otherPage, width, height);
                            if (otherPosition != null) {
                                restorePage = otherPage;
                                resolvedPageIndex = otherPageIndex;
                                x = otherPosition[0];
                                y = otherPosition[1];
                            }
                        }
                    }
                    // If neither page has enough room at the saved size, try the same
                    // two-page search at 50% before leaving the stored widget for retry.
                    if (findFreeWidgetPosition(restorePage, width, height) == null) {
                        int smallerWidth = Math.max(LauncherUiUtils.dp(this, 48), Math.round(width * 0.5f));
                        int smallerHeight = Math.max(LauncherUiUtils.dp(this, 48), Math.round(height * 0.5f));
                        int[] smallerPosition = findFreeWidgetPosition(restorePage, smallerWidth, smallerHeight);
                        if (smallerPosition == null) {
                            int otherPageIndex = resolvedPageIndex == 0 ? 1 : 0;
                            if (otherPageIndex < pagesStrip.getChildCount()
                                    && pagesStrip.getChildAt(otherPageIndex) instanceof FrameLayout) {
                                FrameLayout otherPage = (FrameLayout) pagesStrip.getChildAt(otherPageIndex);
                                smallerPosition = findFreeWidgetPosition(otherPage, smallerWidth, smallerHeight);
                                if (smallerPosition != null) {
                                    restorePage = otherPage;
                                    resolvedPageIndex = otherPageIndex;
                                }
                            }
                        }
                        if (smallerPosition != null) {
                            width = smallerWidth;
                            height = smallerHeight;
                            x = smallerPosition[0];
                            y = smallerPosition[1];
                        }
                    }
                    AppWidgetHostView widget = widgetHost.createView(this, id, info);
                    widget.setAppWidget(id, info);
                    addWidgetContainer(restorePage, widget, id, resolvedPageIndex, x, y, width, height, false);
                    WidgetController.updateWidgetSize(widgetManager, id, widget, getResources().getDisplayMetrics().density, width, height);
                    final int savedPageIndex = resolvedPageIndex;
                    final FrameLayout finalRestorePage = restorePage;
                    final View candidate = finalRestorePage.findViewWithTag("launcher_widget:" + id);
                    if (candidate instanceof FrameLayout) {
                        saveWidgetGeometry(id, savedPageIndex, (FrameLayout) candidate);
                        finalRestorePage.post(() ->
                                resolveWidgetIconCollisions(finalRestorePage, (FrameLayout) candidate));
                    }
                } catch (Exception ignored) {
                    // Failures during a provider refresh must be retried. The saved host ID
                    // and position remain untouched so the widget can reappear afterward.
                    retryNeeded = true;
                }
            }
            if (retryNeeded) {
                scheduleWidgetRestoreRetry();
            } else {
                widgetRestoreRetryCount = 0;
                widgetRestoreRetryScheduled = false;
                handler.removeCallbacks(widgetRestoreRetry);
            }
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

    private void refreshClockTypographyAndGeometry() {
        int restorePage = Math.max(0, Math.min(1, currentPage));
        updateClock();
        for (int page = 0; page < clockPanelsByPage.length; page++) {
            activateClockForPage(page);
            applyClockPositionAndSize();
            applyClockPanelColor(false);
        }
        activateClockForPage(restorePage);
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

    /**
     * Formats only the numeric time for the large clock. On some locales the
     * platform time pattern renders a day period as "p.m." or "a.m."; removing
     * the literal "PM" text from the formatted result does not catch those forms,
     * leaving a stray lowercase "p" rendered in the large clock font. Remove the
     * day-period field from the locale's pattern before formatting instead.
     */
    private String formatClockDigits(Date now, boolean use24Hour) {
        java.text.DateFormat formatter = DateFormat.getTimeFormat(this);
        if (!use24Hour && formatter instanceof SimpleDateFormat) {
            String pattern = ((SimpleDateFormat) formatter).toPattern();
            StringBuilder digitsPattern = new StringBuilder(pattern.length());
            boolean quoted = false;
            for (int i = 0; i < pattern.length(); i++) {
                char ch = pattern.charAt(i);
                if (ch == '\'') {
                    digitsPattern.append(ch);
                    if (i + 1 < pattern.length() && pattern.charAt(i + 1) == '\'') {
                        digitsPattern.append(pattern.charAt(++i));
                    } else {
                        quoted = !quoted;
                    }
                    continue;
                }
                if (!quoted && ch == 'a') {
                    while (digitsPattern.length() > 0 && isClockPatternSpace(
                            digitsPattern.charAt(digitsPattern.length() - 1))) {
                        digitsPattern.setLength(digitsPattern.length() - 1);
                    }
                    while (i + 1 < pattern.length() && pattern.charAt(i + 1) == 'a') i++;
                    while (i + 1 < pattern.length() && isClockPatternSpace(pattern.charAt(i + 1))) i++;
                    continue;
                }
                digitsPattern.append(ch);
            }
            try {
                return new SimpleDateFormat(digitsPattern.toString(), Locale.getDefault()).format(now);
            } catch (IllegalArgumentException ignored) {
                // Fall back to the platform formatter and its existing marker removal below.
            }
        }
        return formatter.format(now);
    }

    private boolean isClockPatternSpace(char ch) {
        return Character.isWhitespace(ch) || Character.isSpaceChar(ch);
    }

    private void updateClock() {
        if (clockPanelsByPage[0] == null || clockPanelsByPage[1] == null) return;
        Date now = new Date();
        boolean use24Hour = DateFormat.is24HourFormat(this);
        String dateValue = new SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(now);
        android.text.SpannableStringBuilder dateLine =
                new android.text.SpannableStringBuilder(dateValue);
        if (!use24Hour) {
            String period = new SimpleDateFormat("a", Locale.getDefault()).format(now);
            // Separate AM/PM from the date with a comma and style only the marker.
            dateLine.insert(0, period + ", ");
            // AM/PM follows the date-size slider exactly and uses a silver,
            // bold-italic treatment without a separate size override.
            dateLine.setSpan(new android.text.style.StyleSpan(Typeface.BOLD_ITALIC), 0, period.length(),
                    android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            dateLine.setSpan(new android.text.style.TypefaceSpan("sans-serif-black"),
                    0, period.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            dateLine.setSpan(new android.text.style.ForegroundColorSpan(Color.rgb(192, 192, 198)),
                    0, period.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            dateLine.setSpan(new android.text.style.RelativeSizeSpan(1f), 0, period.length(),
                    android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        int restorePage = Math.max(0, Math.min(1, currentPage));
        for (int page = 0; page < clockPanelsByPage.length; page++) {
            activateClockForPage(page);
            String clockValue = formatClockDigits(now, use24Hour);
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
                        // Figure space reserves a digit-width cell without drawing a '1'.
                        // Two-digit hours (10–12) are left untouched and display normally.
                        displayClock.insert(0, "\u2007");
                    }
                }
                // Draw the colon's two dots at the vertical center of the clock
                // view/background instead of using the font's naturally raised colon.
                int colonAt = displayClock.toString().indexOf(':');
                if (colonAt >= 0) {
                    // Tighten the minute pair by reducing the first digit's advance,
                    // leaving that digit anchored. Move the first hour digit toward
                    // the second digit while keeping the second digit anchored.
                    int minuteFirst = colonAt + 1;
                    if (minuteFirst + 1 < displayClock.length()
                            && Character.digit(displayClock.charAt(minuteFirst), 10) >= 0
                            && Character.digit(displayClock.charAt(minuteFirst + 1), 10) >= 0) {
                        displayClock.setSpan(new TightDigitSpan(false, clockText.getTextSize()
                                        * Math.max(0f, clockText.getLetterSpacing()) * 0.5f),
                                minuteFirst, minuteFirst + 1, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                    int hourEnd = colonAt - 1;
                    int hourStart = hourEnd;
                    while (hourStart >= 0 && Character.digit(displayClock.charAt(hourStart), 10) >= 0) hourStart--;
                    hourStart++;
                    if (hourEnd - hourStart + 1 >= 2) {
                        displayClock.setSpan(new TightDigitSpan(true, clockText.getTextSize()
                                        * Math.max(0f, clockText.getLetterSpacing()) * 0.5f),
                                hourStart, hourStart + 1, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
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
            if (!dateLine.toString().contentEquals(dateText.getText())) dateText.setText(dateLine);
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
        // A thin white outline indicates move-ready mode without animating or
        // scaling the clock. It is removed when edit mode begins or the clock is tapped.
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
        final LinearLayout panelView = clockPanel;
        final int panelPage = activeClockPage;
        clockPanel.setOnTouchListener((view, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                activateClockForPage(panelPage);
                if (clockWidgetMoving && !clockWidgetEditing) {
                    handler.removeCallbacks(clockWidgetLongPress);
                    handler.removeCallbacks(clockWidgetEditLongPress);
                    showClockMoveBorder(false);
                    clockWidgetMoving = false;
                    setWorkspaceGridVisible(false, null);
                }
                clockWidgetLongPressActivated = false;
                clockWidgetDownRawX = event.getRawX();
                clockWidgetDownRawY = event.getRawY();
                clockWidgetStartX = clockPanel.getTranslationX();
                clockWidgetStartY = clockPanel.getTranslationY();
                handler.removeCallbacks(clockWidgetLongPress);
                handler.removeCallbacks(clockWidgetEditLongPress);
                // Edit mode is resize/customization only: never arm move mode from it.
                if (!clockWidgetEditing) {
                    handler.postDelayed(clockWidgetLongPress, 1000L);
                    handler.postDelayed(clockWidgetEditLongPress, 2000L);
                }
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
                if (clockWidgetMoving && !clockWidgetEditing
                        && (Math.abs(dx) > LauncherUiUtils.dp(this, 5)
                        || Math.abs(dy) > LauncherUiUtils.dp(this, 5))) {
                    if (!clockWidgetDragging) {
                        int[] safeClockGeometry = itemGeometry(clockPanel);
                        liveDragLastSafeGeometry.put(clockPanel,
                                new int[]{safeClockGeometry[0], safeClockGeometry[1]});
                    }
                    clockWidgetDragging = true;
                    float targetX = clockWidgetStartX + dx;
                    float targetY = clockWidgetStartY + dy;
                    setClampedClockTranslation(targetX, targetY, true);
                    updateClockResizeHandles();
                    setWorkspaceGridVisible(true, clockPanel);
                    if (clockPanel.getParent() instanceof FrameLayout) {
                        scheduleLiveDisplacementUpdate((FrameLayout) clockPanel.getParent(), clockPanel);
                    }
                } else if (!clockWidgetMoving && !clockWidgetEditing
                        && (Math.abs(dx) > LauncherUiUtils.dp(this, 8)
                        || Math.abs(dy) > LauncherUiUtils.dp(this, 8))) {
                    handler.removeCallbacks(clockWidgetLongPress);
                    handler.removeCallbacks(clockWidgetEditLongPress);
                    // Do not consume the move: allow the parent pager and the
                    // activity-level swipe detector to receive the gesture sequence.
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
                handler.removeCallbacks(clockWidgetEditLongPress);
                if (clockWidgetDragging) {
                    finishLiveDisplacements(event.getActionMasked() == MotionEvent.ACTION_UP, clockPanel);
                    clockWidgetDragging = false;
                    setWorkspaceGridVisible(false, null);
                    updateClockResizeHandles();
                }
                // Move-ready is a press-and-hold hint only. Releasing the finger
                // or cancelling the gesture removes its outline and grid immediately.
                if (!clockWidgetEditing && clockWidgetMoving) {
                    showClockMoveBorder(false);
                    clockWidgetMoving = false;
                    setWorkspaceGridVisible(false, null);
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
        panelView.post(() -> {
            if (panelView.getParent() == null) return;
            // Restore this exact page's geometry without mutating the active page
            // or rewriting saved coordinates during an intermediate layout.
            setClampedClockTranslationForPanel(panelView, panelPage, savedClockX, savedClockY, false);
            if (clockPanel == panelView) updateClockResizeHandles();
        });
    }

    private final Runnable clockWidgetLongPress = () -> {
        if (clockPanel == null || drawerOpen || clockWidgetEditing) return;
        clockWidgetMoving = true;
        clockWidgetLongPressActivated = true;
        // Show a subtle move-ready outline without changing clock size or position.
        showClockMoveBorder(true);
        setWorkspaceGridVisible(true, clockPanel);
    };

    private final Runnable clockWidgetEditLongPress = () -> {
        if (clockPanel == null || drawerOpen || clockWidgetEditing) return;
        // Entering edit mode ends move-ready mode immediately. Commit any movement
        // already made during the one-second move window, then disable dragging.
        showClockMoveBorder(false);
        clockWidgetMoving = false;
        if (clockWidgetDragging && clockPanel.getParent() instanceof FrameLayout) {
            finishLiveDisplacements(true, clockPanel);
            prefs.edit()
                    .putFloat(clockPrefKey(KEY_CLOCK_WIDGET_X, activeClockPage), clockPanel.getTranslationX())
                    .putFloat(clockPrefKey(KEY_CLOCK_WIDGET_Y, activeClockPage), clockPanel.getTranslationY())
                    .apply();
        }
        clockWidgetDragging = false;
        clockWidgetLongPressActivated = true;
        clockWidgetEditing = true;
        if (clockWidgetResizeHandle != null) clockWidgetResizeHandle.setVisibility(View.GONE);
        for (View grip : clockEdgeHandles) if (grip != null) grip.setVisibility(View.VISIBLE);
        updateClockResizeHandles();
        setWorkspaceGridVisible(true, clockPanel);
        Toast.makeText(this, "Clock edit mode: drag to move; drag an edge to resize; tap outside to finish",
                Toast.LENGTH_LONG).show();
    };

    private void showClockMoveBorder(boolean visible) {
        if (clockPanel == null) return;
        if (!visible) {
            clockPanel.setForeground(null);
            return;
        }
        android.graphics.drawable.GradientDrawable outline =
                new android.graphics.drawable.GradientDrawable();
        outline.setColor(Color.TRANSPARENT);
        outline.setCornerRadius(LauncherUiUtils.dp(this, 2));
        outline.setStroke(LauncherUiUtils.dp(this, 1), Color.WHITE);
        clockPanel.setForeground(outline);
    }

    private void exitClockWidgetEditMode() {
        showClockMoveBorder(false);
        clockWidgetMoving = false;
        clockWidgetLongPressActivated = false;
        handler.removeCallbacks(clockWidgetLongPress);
        handler.removeCallbacks(clockWidgetEditLongPress);
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


    private boolean isClockWorkspaceItem(View view) {
        if (view == null) return false;
        return view == clockPanelsByPage[0] || view == clockPanelsByPage[1]
                || "launcher_clock_widget".equals(view.getTag());
    }

    private int workspacePageIndex(View view) {
        if (pagesStrip != null && view != null && view.getParent() instanceof View) {
            int pageIndex = pagesStrip.indexOfChild((View) view.getParent());
            if (pageIndex >= 0) return pageIndex;
        }
        return Math.max(0, Math.min(1, activeClockPage));
    }

    private void setClockVisualPosition(View panel, int visualLeft, int visualTop,
            boolean persist) {
        if (!isClockWorkspaceItem(panel)) return;
        int pageIndex = workspacePageIndex(panel);
        int[] current = itemGeometry(panel);
        float tx = panel.getTranslationX() + visualLeft - current[0];
        float ty = panel.getTranslationY() + visualTop - current[1];
        setClampedClockTranslationForPanel(panel, pageIndex, tx, ty, persist);
    }

    /**
     * Commits a live push-aside destination immediately. Delayed per-item animations
     * caused displaced widgets to lag behind the drag, then get cancelled/restarted
     * when the next MOVE arrived (the visible "dancing" behaviour).
     */
    private void placeDisplacedItemNow(View item, int left, int top) {
        if (item == null) return;
        item.animate().cancel();
        if (isClockWorkspaceItem(item)) {
            setClockVisualPosition(item, left, top, false);
            return;
        }
        ViewGroup.LayoutParams raw = item.getLayoutParams();
        if (raw instanceof FrameLayout.LayoutParams) {
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) raw;
            float sx = Math.max(0.01f, item.getScaleX());
            float sy = Math.max(0.01f, item.getScaleY());
            lp.leftMargin = left - Math.round(item.getPivotX() * (1f - sx));
            lp.topMargin = top - Math.round(item.getPivotY() * (1f - sy));
            item.setTranslationX(0f);
            item.setTranslationY(0f);
            item.setLayoutParams(lp);
        }
    }

    private void animateDisplacedItemTo(View item, int left, int top) {
        if (item == null) return;
        if (isHomeGridIcon(item)) {
            // A group animation may be re-targeted mid-drag. Freeze that cohort at its
            // current visual positions and re-queue its unchanged reservations together.
            cancelAppDisplacementAnimator(item, true);
            item.animate().cancel();
            pendingAppDisplacementTargets.put(item, new int[]{left, top});
            return;
        }

        int[] current = itemGeometry(item);
        item.animate().cancel();
        if (isClockWorkspaceItem(item)) {
            float targetX = item.getTranslationX() + left - current[0];
            float targetY = item.getTranslationY() + top - current[1];
            float[] bounded = clampClockTranslation(item, targetX, targetY);
            item.animate().translationX(bounded[0]).translationY(bounded[1])
                    .setDuration(animationDuration(140L))
                    .setInterpolator(new android.view.animation.DecelerateInterpolator())
                    .withEndAction(() -> refreshDragDisplacementsAfterAnimation(item, left, top))
                    .start();
            return;
        }
        ViewGroup.LayoutParams raw = item.getLayoutParams();
        if (!(raw instanceof FrameLayout.LayoutParams)) return;
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) raw;
        float sx = Math.max(0.01f, item.getScaleX());
        float sy = Math.max(0.01f, item.getScaleY());
        lp.leftMargin = left - Math.round(item.getPivotX() * (1f - sx));
        lp.topMargin = top - Math.round(item.getPivotY() * (1f - sy));
        item.setTranslationX(current[0] - left);
        item.setTranslationY(current[1] - top);
        item.setLayoutParams(lp);
        item.animate().translationX(0f).translationY(0f)
                .setDuration(animationDuration(140L))
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .withEndAction(() -> refreshDragDisplacementsAfterAnimation(item, left, top))
                .start();
    }

    /**
     * Cancels a shared app cohort. If only one member needs a new target, preserve the
     * other members' reservations and re-queue them so they continue from the same frame.
     */
    private void cancelAppDisplacementAnimator(View item, boolean preservePeers) {
        if (item == null) return;
        android.animation.ValueAnimator animator = appDisplacementAnimatorByView.get(item);
        if (animator == null) return;

        java.util.ArrayList<View> cohort = new java.util.ArrayList<View>();
        for (java.util.Map.Entry<View, android.animation.ValueAnimator> entry
                : new java.util.ArrayList<java.util.Map.Entry<View, android.animation.ValueAnimator>>(
                        appDisplacementAnimatorByView.entrySet())) {
            if (entry.getValue() != animator) continue;
            cohort.add(entry.getKey());
            appDisplacementAnimatorByView.remove(entry.getKey());
        }
        if (preservePeers) {
            for (View peer : cohort) {
                if (peer == item || !(peer.getParent() instanceof FrameLayout)) continue;
                int[] target = liveDisplacementTargets.get(peer);
                if (target != null) {
                    pendingAppDisplacementTargets.put(peer, new int[]{target[0], target[1]});
                }
            }
        }
        animator.cancel();
    }

    /** Start all queued app shifts together, sharing one animator for identical vectors. */
    private void flushPendingAppDisplacementAnimations() {
        if (pendingAppDisplacementTargets.isEmpty()) return;
        java.util.IdentityHashMap<View, int[]> pending =
                new java.util.IdentityHashMap<View, int[]>(pendingAppDisplacementTargets);
        pendingAppDisplacementTargets.clear();

        java.util.HashMap<Long, java.util.ArrayList<AppDisplacementAnimation>> groups =
                new java.util.HashMap<Long, java.util.ArrayList<AppDisplacementAnimation>>();
        for (java.util.Map.Entry<View, int[]> entry : pending.entrySet()) {
            View item = entry.getKey();
            int[] target = entry.getValue();
            if (!(item.getParent() instanceof FrameLayout)
                    || !(item.getLayoutParams() instanceof FrameLayout.LayoutParams)) continue;

            cancelAppDisplacementAnimator(item, false);
            item.animate().cancel();
            int[] current = itemGeometry(item);
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) item.getLayoutParams();
            float sx = Math.max(0.01f, item.getScaleX());
            float sy = Math.max(0.01f, item.getScaleY());
            lp.leftMargin = target[0]
                    - Math.round(item.getPivotX() * (1f - sx));
            lp.topMargin = target[1]
                    - Math.round(item.getPivotY() * (1f - sy));
            int dx = current[0] - target[0];
            int dy = current[1] - target[1];
            item.setTranslationX(dx);
            item.setTranslationY(dy);
            item.setLayoutParams(lp);
            if (dx == 0 && dy == 0) {
                item.setTranslationX(0f);
                item.setTranslationY(0f);
                continue;
            }

            // Exact vector equality is deliberate: all members of a cohort translate
            // by precisely the same amount, so their relative spacing never changes.
            long key = (((long) dx) << 32) | (dy & 0xffffffffL);
            java.util.ArrayList<AppDisplacementAnimation> group = groups.get(key);
            if (group == null) {
                group = new java.util.ArrayList<AppDisplacementAnimation>();
                groups.put(key, group);
            }
            group.add(new AppDisplacementAnimation(item, target[0], target[1], dx, dy));
        }

        for (java.util.ArrayList<AppDisplacementAnimation> group : groups.values()) {
            if (group.size() < 2) {
                AppDisplacementAnimation single = group.get(0);
                single.view.animate().translationX(0f).translationY(0f)
                        .setDuration(animationDuration(140L))
                        .setInterpolator(new android.view.animation.DecelerateInterpolator())
                        .withEndAction(() -> refreshDragDisplacementsAfterAnimation(
                                single.view, single.targetX, single.targetY))
                        .start();
                continue;
            }

            final java.util.ArrayList<AppDisplacementAnimation> cohort =
                    new java.util.ArrayList<AppDisplacementAnimation>(group);
            final android.animation.ValueAnimator animator =
                    android.animation.ValueAnimator.ofFloat(0f, 1f);
            final boolean[] cancelled = new boolean[]{false};
            animator.setDuration(animationDuration(140L));
            animator.setInterpolator(new android.view.animation.DecelerateInterpolator());
            animator.addUpdateListener(valueAnimator -> {
                float remaining = 1f - (Float) valueAnimator.getAnimatedValue();
                for (AppDisplacementAnimation movement : cohort) {
                    movement.view.setTranslationX(movement.deltaX * remaining);
                    movement.view.setTranslationY(movement.deltaY * remaining);
                }
            });
            animator.addListener(new android.animation.AnimatorListenerAdapter() {
                @Override public void onAnimationCancel(android.animation.Animator animation) {
                    cancelled[0] = true;
                }

                @Override public void onAnimationEnd(android.animation.Animator animation) {
                    for (AppDisplacementAnimation movement : cohort) {
                        if (appDisplacementAnimatorByView.get(movement.view) == animator) {
                            appDisplacementAnimatorByView.remove(movement.view);
                        }
                        if (cancelled[0]) continue;
                        movement.view.setTranslationX(0f);
                        movement.view.setTranslationY(0f);
                    }
                    if (!cancelled[0]) {
                        for (AppDisplacementAnimation movement : cohort) {
                            refreshDragDisplacementsAfterAnimation(
                                    movement.view, movement.targetX, movement.targetY);
                        }
                    }
                }
            });
            for (AppDisplacementAnimation movement : cohort) {
                appDisplacementAnimatorByView.put(movement.view, animator);
            }
            animator.start();
        }
    }

    /**
     * Move a contiguous app-icon chain one grid cell in a shared direction when possible.
     * The sweep is rejected if it hits a widget/shortcut or would finish over the dragged
     * icon. This is a fast, bounded first choice; the existing collision planner remains
     * the fallback when a safe common shift is unavailable.
     */
    private boolean tryShiftHomeIconCohort(FrameLayout page, View movingView, View seed,
            int preferredX, int preferredY) {
        if (page == null || movingView == null || seed == null
                || !isHomeGridIcon(movingView) || !isHomeGridIcon(seed)
                || liveDisplacedItems.containsKey(seed)
                || page.getWidth() <= 0 || page.getHeight() <= 0) return false;

        int columns = homeGridColumnsForItem(seed);
        int cellWidth = Math.max(1, page.getWidth() / columns);
        int iconSize = prefs == null ? 48 : prefs.getInt(KEY_ICON_SIZE, 48);
        int cellHeight = LauncherUiUtils.dp(this, homeCellHeightDp(iconSize));
        int rows = homeGridRowsForHeight(page.getHeight(), cellHeight);
        int[] seedSlot = homeGridIconSlot(page, seed, cellHeight, rows);
        if (seedSlot == null) return false;

        java.util.ArrayList<int[]> directions = new java.util.ArrayList<int[]>();
        java.util.HashSet<String> seenDirections = new java.util.HashSet<String>();
        if (preferredX >= 0 && preferredY >= 0) {
            int targetCol = Math.max(0, Math.min(columns - 1,
                    Math.round(preferredX / (float) cellWidth)));
            int targetRow = homeGridRowFromTop(preferredY, rows, page.getHeight(), cellHeight);
            int deltaCol = targetCol - seedSlot[0];
            int deltaRow = targetRow - seedSlot[1];
            if (Math.abs(deltaCol) >= Math.abs(deltaRow) && deltaCol != 0) {
                addAppShiftDirection(directions, seenDirections, Integer.signum(deltaCol), 0);
                if (deltaRow != 0) addAppShiftDirection(
                        directions, seenDirections, 0, Integer.signum(deltaRow));
            } else if (deltaRow != 0) {
                addAppShiftDirection(directions, seenDirections, 0, Integer.signum(deltaRow));
                if (deltaCol != 0) addAppShiftDirection(
                        directions, seenDirections, Integer.signum(deltaCol), 0);
            }
        }
        int[] moverGeometry = itemGeometry(movingView);
        int[] seedGeometry = itemGeometry(seed);
        int moverCenterX = moverGeometry[0] + moverGeometry[2] / 2;
        int moverCenterY = moverGeometry[1] + moverGeometry[3] / 2;
        int seedCenterX = seedGeometry[0] + seedGeometry[2] / 2;
        int seedCenterY = seedGeometry[1] + seedGeometry[3] / 2;
        if (Math.abs(seedCenterX - moverCenterX) >= Math.abs(seedCenterY - moverCenterY)) {
            addAppShiftDirection(directions, seenDirections,
                    moverCenterX < seedCenterX ? 1 : -1, 0);
            addAppShiftDirection(directions, seenDirections,
                    0, moverCenterY < seedCenterY ? 1 : -1);
        } else {
            addAppShiftDirection(directions, seenDirections,
                    0, moverCenterY < seedCenterY ? 1 : -1);
            addAppShiftDirection(directions, seenDirections,
                    moverCenterX < seedCenterX ? 1 : -1, 0);
        }
        addAppShiftDirection(directions, seenDirections, 1, 0);
        addAppShiftDirection(directions, seenDirections, -1, 0);
        addAppShiftDirection(directions, seenDirections, 0, 1);
        addAppShiftDirection(directions, seenDirections, 0, -1);

        for (int[] direction : directions) {
            int dxCells = direction[0], dyCells = direction[1];
            if (seedSlot[0] + dxCells < 0 || seedSlot[0] + dxCells >= columns
                    || seedSlot[1] + dyCells < 0 || seedSlot[1] + dyCells >= rows) continue;

            java.util.ArrayList<View> cohort = new java.util.ArrayList<View>();
            java.util.IdentityHashMap<View, Boolean> members =
                    new java.util.IdentityHashMap<View, Boolean>();
            java.util.IdentityHashMap<View, int[]> slots =
                    new java.util.IdentityHashMap<View, int[]>();
            cohort.add(seed);
            members.put(seed, Boolean.TRUE);
            slots.put(seed, seedSlot);
            boolean blocked = false;

            // Expanding only along the shifted row/column groups the whole chain but
            // avoids unexpectedly dragging unrelated apps from adjacent rows.
            for (int memberIndex = 0; memberIndex < cohort.size() && !blocked; memberIndex++) {
                View member = cohort.get(memberIndex);
                if (liveDisplacedItems.containsKey(member)
                        || appDisplacementAnimatorByView.containsKey(member)) {
                    blocked = true;
                    break;
                }
                int[] originSlot = slots.get(member);
                int targetCol = originSlot[0] + dxCells;
                int targetRow = originSlot[1] + dyCells;
                if (targetCol < 0 || targetCol >= columns || targetRow < 0 || targetRow >= rows) {
                    blocked = true;
                    break;
                }
                int[] memberGeometry = itemGeometry(member);
                int originX = homeGridIconLeftAt(
                        member, originSlot[0], cellWidth, memberGeometry[2]);
                int originY = homeGridIconTopAt(
                        page, member, originSlot[1], rows, cellHeight);
                int targetX = homeGridIconLeftAt(
                        member, targetCol, cellWidth, memberGeometry[2]);
                int targetY = homeGridIconTopAt(page, member, targetRow, rows, cellHeight);
                android.graphics.Rect startVisible = appIconVisualRectAt(
                        member, originX, originY, memberGeometry[2], memberGeometry[3]);
                android.graphics.Rect targetVisible = appIconVisualRectAt(
                        member, targetX, targetY, memberGeometry[2], memberGeometry[3]);
                android.graphics.Rect movingVisible = collisionRectForInteraction(
                        movingView, moverGeometry, member);
                if (android.graphics.Rect.intersects(targetVisible, movingVisible)) {
                    blocked = true;
                    break;
                }
                android.graphics.Rect swept = new android.graphics.Rect(startVisible);
                swept.union(targetVisible);

                for (int childIndex = 0; childIndex < page.getChildCount(); childIndex++) {
                    View blocker = page.getChildAt(childIndex);
                    if (blocker == member || blocker == movingView
                            || blocker.getVisibility() != View.VISIBLE
                            || !isWorkspaceItem(blocker) || members.containsKey(blocker)) continue;
                    int[] blockerGeometry = itemGeometry(blocker);
                    if (blockerGeometry[2] <= 0 || blockerGeometry[3] <= 0) continue;

                    boolean gridBlocker = isHomeGridIcon(blocker);
                    boolean sameGridFamily = gridBlocker
                            && (isWorkspaceContactShortcut(blocker) == isWorkspaceContactShortcut(seed));
                    int[] blockerSlot = gridBlocker
                            ? homeGridIconSlot(page, blocker, cellHeight, rows) : null;
                    boolean targetSlotOccupied = sameGridFamily && blockerSlot != null
                            && blockerSlot[0] == targetCol && blockerSlot[1] == targetRow;
                    android.graphics.Rect sweepForBlocker = new android.graphics.Rect(swept);
                    if (!gridBlocker) {
                        int margin = workspaceCollisionMargin(member);
                        if (margin > 0) sweepForBlocker.inset(-margin, -margin);
                    }
                    android.graphics.Rect blockerVisible =
                            collisionRectForInteraction(blocker, blockerGeometry, member);
                    int[] blockerTarget = liveDisplacementTargets.get(blocker);
                    boolean targetPathCollision = android.graphics.Rect.intersects(
                            sweepForBlocker, blockerVisible);
                    if (blockerTarget != null) {
                        android.graphics.Rect plannedBounds = collisionRectAtForInteraction(
                                blocker, blockerTarget[0], blockerTarget[1],
                                blockerGeometry[2], blockerGeometry[3], member);
                        targetPathCollision = targetPathCollision
                                || android.graphics.Rect.intersects(sweepForBlocker, plannedBounds);
                    }

                    if (!targetSlotOccupied && !targetPathCollision) continue;
                    if (!gridBlocker) {
                        blocked = true;
                        break;
                    }
                    // Do not mix unlike column geometries in one cohort; the generic
                    // planner handles app/shortcut interactions without mis-snapping.
                    boolean sameLane = sameGridFamily && (dxCells != 0
                            ? blockerSlot != null && blockerSlot[1] == originSlot[1]
                            : blockerSlot != null && blockerSlot[0] == originSlot[0]);
                    if (!sameLane || liveDisplacedItems.containsKey(blocker)
                            || appDisplacementAnimatorByView.containsKey(blocker)) {
                        blocked = true;
                        break;
                    }
                    if (!members.containsKey(blocker)) {
                        int[] actualSlot = homeGridIconSlot(page, blocker, cellHeight, rows);
                        if (actualSlot == null) {
                            blocked = true;
                            break;
                        }
                        members.put(blocker, Boolean.TRUE);
                        slots.put(blocker, actualSlot);
                        cohort.add(blocker);
                    }
                }
            }

            if (blocked || cohort.size() < 2) continue;
            // Commit all reservations first, then enqueue the whole same-vector shift.
            for (View member : cohort) {
                int[] originSlot = slots.get(member);
                int[] originGeometry = itemGeometry(member);
                int targetX = homeGridIconLeftAt(
                        member, originSlot[0] + dxCells, cellWidth, originGeometry[2]);
                int targetY = homeGridIconTopAt(
                        page, member, originSlot[1] + dyCells, rows, cellHeight);
                liveDisplacedItems.put(member,
                        new int[]{originGeometry[0], originGeometry[1]});
                liveDisplacementTargets.put(member, new int[]{targetX, targetY});
                liveDragGeometryCache.remove(member);
                animateDisplacedItemTo(member, targetX, targetY);
            }
            return true;
        }
        return false;
    }

    private void addAppShiftDirection(java.util.ArrayList<int[]> directions,
            java.util.HashSet<String> seen, int dx, int dy) {
        String key = dx + ":" + dy;
        if (seen.add(key)) directions.add(new int[]{dx, dy});
    }

    private boolean isWorkspaceItem(View view) {
        if (view == null || !(view.getTag() instanceof String)) return false;
        String tag = (String) view.getTag();
        // App cells are placement slots, not scalable widgets. Older versions
        // persisted emergency scale values for them; clear those legacy values
        // and keep the cell transform at 1 so its configured icon size is stable.
        if (!workspaceScaleRestored.containsKey(view)) {
            workspaceScaleRestored.put(view, Boolean.TRUE);
            if (tag.startsWith("launcher_app:")) {
                view.setScaleX(1f);
                view.setScaleY(1f);
                if (prefs != null) {
                    prefs.edit().remove("workspace_scale_x_" + tag)
                            .remove("workspace_scale_y_" + tag)
                            .remove("workspace_base_scale_x_" + tag)
                            .remove("workspace_base_scale_y_" + tag).apply();
                }
            } else {
                float sx = prefs == null ? 1f : prefs.getFloat("workspace_scale_x_" + tag, 1f);
                float sy = prefs == null ? 1f : prefs.getFloat("workspace_scale_y_" + tag, 1f);
                if (sx > 0f && sy > 0f && (sx < 0.999f || sy < 0.999f)) {
                    view.setScaleX(sx);
                    view.setScaleY(sy);
                }
            }
        }
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
        int baseX = view.getLeft();
        int baseY = view.getTop();
        int baseWidth = view.getWidth();
        int baseHeight = view.getHeight();

        // Pending layout params must be resolved using the child's gravity.
        // A centered/right-aligned clock can legitimately have leftMargin != getLeft();
        // treating that margin as its actual X coordinate shifts the resize handles
        // and makes left/right edge anchoring drift during live resizing.
        android.view.ViewParent parent = view.getParent();
        ViewGroup.LayoutParams raw = view.getLayoutParams();
        if (parent instanceof FrameLayout && raw instanceof FrameLayout.LayoutParams
                && view.isLayoutRequested()) {
            FrameLayout page = (FrameLayout) parent;
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) raw;
            if (lp.width > 0) baseWidth = lp.width;
            if (lp.height > 0) baseHeight = lp.height;

            int horizontalGravity = Gravity.getAbsoluteGravity(
                    lp.gravity, view.getLayoutDirection()) & Gravity.HORIZONTAL_GRAVITY_MASK;
            int verticalGravity = lp.gravity & Gravity.VERTICAL_GRAVITY_MASK;
            int contentWidth = Math.max(0, page.getWidth()
                    - page.getPaddingLeft() - page.getPaddingRight());
            int contentHeight = Math.max(0, page.getHeight()
                    - page.getPaddingTop() - page.getPaddingBottom());

            if (page.getWidth() > 0) {
                if (horizontalGravity == Gravity.RIGHT) {
                    baseX = page.getWidth() - page.getPaddingRight()
                            - lp.rightMargin - baseWidth;
                } else if (horizontalGravity == Gravity.CENTER_HORIZONTAL) {
                    baseX = page.getPaddingLeft() + (contentWidth - baseWidth) / 2
                            + lp.leftMargin - lp.rightMargin;
                } else {
                    baseX = page.getPaddingLeft() + lp.leftMargin;
                }
            } else {
                baseX = lp.leftMargin;
            }

            if (page.getHeight() > 0) {
                if (verticalGravity == Gravity.BOTTOM) {
                    baseY = page.getHeight() - page.getPaddingBottom()
                            - lp.bottomMargin - baseHeight;
                } else if (verticalGravity == Gravity.CENTER_VERTICAL) {
                    baseY = page.getPaddingTop() + (contentHeight - baseHeight) / 2
                            + lp.topMargin - lp.bottomMargin;
                } else {
                    baseY = page.getPaddingTop() + lp.topMargin;
                }
            } else {
                baseY = lp.topMargin;
            }
        }

        // Include translation and pivot-based scaling so the reported rectangle
        // matches the transformed visual bounds used by resize handles and collision
        // checks. Once layout has settled, use the actual laid-out left/top values.
        int x = Math.round(baseX + view.getTranslationX() + view.getPivotX() * (1f - sx));
        int y = Math.round(baseY + view.getTranslationY() + view.getPivotY() * (1f - sy));
        return new int[]{x, y, Math.max(1, Math.round(Math.max(1, baseWidth) * sx)),
                Math.max(1, Math.round(Math.max(1, baseHeight) * sy))};
    }

    private int[] cachedLiveDragGeometry(View view) {
        int[] geometry = liveDragGeometryCache.get(view);
        if (geometry == null) {
            geometry = itemGeometry(view);
            liveDragGeometryCache.put(view, geometry);
        }
        return geometry;
    }

    private boolean isWorkspaceAppIcon(View view) {
        return view != null && view.getTag() instanceof String
                && ((String) view.getTag()).startsWith("launcher_app:");
    }

    private boolean isWorkspaceContactShortcut(View view) {
        return view != null && "launcher_contact_shortcut".equals(view.getTag());
    }

    private boolean isHomeGridIcon(View view) {
        return isWorkspaceAppIcon(view) || isWorkspaceContactShortcut(view);
    }

    private int homeGridColumnsForItem(View item) {
        return isWorkspaceContactShortcut(item) ? 4 : Math.max(1, getHomeColumns());
    }

    private int homeGridIconLeftAt(View item, int col, int cellWidth, int fallbackWidth) {
        if (isWorkspaceAppIcon(item)) return col * cellWidth;
        int width = Math.max(1, fallbackWidth);
        if (item != null && item.getLayoutParams() instanceof FrameLayout.LayoutParams) {
            int configured = ((FrameLayout.LayoutParams) item.getLayoutParams()).width;
            if (configured > 0) width = configured;
        }
        return col * cellWidth + Math.max(0, (cellWidth - width) / 2);
    }

    private int homeGridIconTopAt(FrameLayout page, View item, int row, int rows, int cellHeight) {
        int top = homeGridRowTop(row, rows, page.getHeight(), cellHeight);
        return isWorkspaceContactShortcut(item) ? top + LauncherUiUtils.dp(this, 14) : top;
    }

    private int[] homeGridIconSlot(FrameLayout page, View item, int cellHeight, int rows) {
        if (page == null || item == null
                || !(item.getLayoutParams() instanceof FrameLayout.LayoutParams)) return null;
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) item.getLayoutParams();
        int columns = homeGridColumnsForItem(item);
        int cellWidth = Math.max(1, page.getWidth() / columns);
        int col = Math.max(0, Math.min(columns - 1,
                Math.round(lp.leftMargin / (float) cellWidth)));
        int row = homeGridRowFromTop(lp.topMargin, rows, page.getHeight(), cellHeight);
        return new int[]{col, row};
    }

    private boolean isAppIconCollisionParticipant(View view) {
        return isHomeGridIcon(view)
                || (view != null && "workspace_transfer_proxy".equals(view.getTag())
                    && isHomeGridIcon(workspaceTransferItem));
    }

    /**
     * Visual collision bounds for a home app cell. The cell fills its grid slot,
     * but only the icon artwork and (when enabled) the rendered label are visible
     * collision surfaces. Using the full cell made a large clock push icons away
     * even when the clock touched only empty cell padding.
     */
    private android.graphics.Rect appIconVisualRectAt(
            View view, int cellX, int cellY, int cellWidth, int cellHeight) {
        if (!liveDragPlanningCacheActive) {
            return calculateAppIconVisualRectAt(view, cellX, cellY, cellWidth, cellHeight);
        }
        int[] local = liveDragAppVisualBoundsCache.get(view);
        if (local == null || local[4] != cellWidth || local[5] != cellHeight) {
            android.graphics.Rect measured =
                    calculateAppIconVisualRectAt(view, 0, 0, cellWidth, cellHeight);
            local = new int[]{measured.left, measured.top, measured.right, measured.bottom,
                    cellWidth, cellHeight};
            liveDragAppVisualBoundsCache.put(view, local);
        }
        return new android.graphics.Rect(
                cellX + local[0], cellY + local[1],
                cellX + local[2], cellY + local[3]);
    }

    private android.graphics.Rect calculateAppIconVisualRectAt(
            View view, int cellX, int cellY, int cellWidth, int cellHeight) {
        android.graphics.Rect cellBounds = new android.graphics.Rect(
                cellX, cellY, cellX + Math.max(1, cellWidth), cellY + Math.max(1, cellHeight));
        View visualTemplate = view;
        if (view != null && "workspace_transfer_proxy".equals(view.getTag())
                && workspaceTransferItem instanceof ViewGroup) {
            visualTemplate = workspaceTransferItem;
        }
        if (!(visualTemplate instanceof ViewGroup)) return cellBounds;
        ViewGroup cell = (ViewGroup) visualTemplate;
        if (cell.getChildCount() == 0) return cellBounds;

        float parentScaleX = Math.max(0.01f, view.getScaleX());
        float parentScaleY = Math.max(0.01f, view.getScaleY());
        android.graphics.Rect visual = null;
        int childCount = Math.min(2, cell.getChildCount());
        for (int i = 0; i < childCount; i++) {
            View child = cell.getChildAt(i);
            if (child == null || child.getVisibility() != View.VISIBLE
                    || child.getWidth() <= 0 || child.getHeight() <= 0) continue;

            float localLeft = child.getLeft() + child.getTranslationX()
                    + child.getPivotX() * (1f - child.getScaleX());
            float localTop = child.getTop() + child.getTranslationY()
                    + child.getPivotY() * (1f - child.getScaleY());
            float localWidth = child.getWidth() * Math.max(0.01f, child.getScaleX());
            float localHeight = child.getHeight() * Math.max(0.01f, child.getScaleY());

            // The label TextView fills the cell width, while the painted text is
            // centered inside it. Measure the text rather than treating blank
            // label space as visible content.
            if (i == 1 && child instanceof android.widget.TextView) {
                android.widget.TextView label = (android.widget.TextView) child;
                CharSequence text = label.getText();
                if (text == null || text.length() == 0) continue;
                android.graphics.Paint paint = label.getPaint();
                android.graphics.Paint.FontMetrics metrics = paint.getFontMetrics();
                float paintedWidth = Math.min(
                        Math.max(0, label.getWidth() - label.getCompoundPaddingLeft()
                                - label.getCompoundPaddingRight()),
                        paint.measureText(text.toString()));
                float paintedHeight = Math.max(1f, metrics.descent - metrics.ascent);
                localLeft += label.getCompoundPaddingLeft()
                        + Math.max(0f, (label.getWidth()
                                - label.getCompoundPaddingLeft()
                                - label.getCompoundPaddingRight() - paintedWidth) / 2f);
                localTop += label.getCompoundPaddingTop()
                        + Math.max(0f, (label.getHeight()
                                - label.getCompoundPaddingTop()
                                - label.getCompoundPaddingBottom() - paintedHeight) / 2f);
                localWidth = paintedWidth;
                localHeight = paintedHeight;
            }

            int left = cellX + Math.round(localLeft * parentScaleX);
            int top = cellY + Math.round(localTop * parentScaleY);
            int right = left + Math.max(1, Math.round(localWidth * parentScaleX));
            int bottom = top + Math.max(1, Math.round(localHeight * parentScaleY));
            android.graphics.Rect childBounds = new android.graphics.Rect(left, top, right, bottom);
            if (cell.getClipChildren()) childBounds.intersect(cellBounds);
            if (childBounds.isEmpty()) continue;
            if (visual == null) visual = childBounds;
            else visual.union(childBounds);
        }
        return visual == null ? cellBounds : visual;
    }

    /**
     * Find an empty home-grid slot for an app cell using its visible content
     * bounds. Returned coordinates and dimensions match HomePageController's grid.
     */
    private int[] findFreeHomeAppSlot(FrameLayout page, View appItem,
            java.util.List<android.graphics.Rect> obstacles, int preferredX, int preferredY) {
        if (page == null || appItem == null || page.getWidth() <= 0 || page.getHeight() <= 0) {
            return null;
        }
        int columns = getHomeColumns();
        int cellWidth = Math.max(1, page.getWidth() / Math.max(1, columns));
        int iconSize = prefs == null ? 48 : prefs.getInt(KEY_ICON_SIZE, 48);
        int cellHeight = LauncherUiUtils.dp(this, homeCellHeightDp(iconSize));
        int rows = homeGridRowsForHeight(page.getHeight(), cellHeight);
        int bestX = -1;
        int bestY = -1;
        long bestDistance = Long.MAX_VALUE;
        for (int row = 0; row < rows; row++) {
            int y = homeGridRowTop(row, rows, page.getHeight(), cellHeight);
            if (y + cellHeight > page.getHeight()) continue;
            for (int col = 0; col < columns; col++) {
                int x = col * cellWidth;
                if (x + cellWidth > page.getWidth()) continue;
                android.graphics.Rect candidate = collisionRectAt(
                        appItem, x, y, cellWidth, cellHeight);
                boolean blocked = false;
                if (obstacles != null) {
                    for (android.graphics.Rect obstacle : obstacles) {
                        if (android.graphics.Rect.intersects(candidate, obstacle)) {
                            blocked = true;
                            break;
                        }
                    }
                }
                if (blocked) continue;
                long distance = Math.abs((long) x - preferredX)
                        + Math.abs((long) y - preferredY);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestX = x;
                    bestY = y;
                }
            }
        }
        return bestX < 0 ? null : new int[]{bestX, bestY, cellWidth, cellHeight};
    }

    private int workspaceCollisionMargin(View view) {
        // Apply only a tiny gap around the visible app artwork/label bounds.
        // collisionRect() has already removed the cell's empty area, so this does
        // not make neighboring grid slots collide merely because their cells touch.
        return isHomeGridIcon(view) ? LauncherUiUtils.dp(this, 2) : 0;
    }

    private android.graphics.Rect collisionRect(View view, int[] geometry) {
        int margin = workspaceCollisionMargin(view);
        android.graphics.Rect bounds = isHomeGridIcon(view)
                ? appIconVisualRectAt(view, geometry[0], geometry[1], geometry[2], geometry[3])
                : new android.graphics.Rect(geometry[0], geometry[1],
                        geometry[0] + geometry[2], geometry[1] + geometry[3]);
        if (margin == 0) return bounds;
        bounds.inset(-margin, -margin);
        return bounds;
    }

    private android.graphics.Rect collisionRectAt(View view, int x, int y, int width, int height) {
        int margin = workspaceCollisionMargin(view);
        android.graphics.Rect bounds = isHomeGridIcon(view)
                ? appIconVisualRectAt(view, x, y, width, height)
                : new android.graphics.Rect(x, y, x + width, y + height);
        if (margin == 0) return bounds;
        bounds.inset(-margin, -margin);
        return bounds;
    }

    /**
     * Pair-aware collision bounds. Two home app cells collide only when their visible
     * icon/label bounds overlap; the legacy safety margin still applies to mixed-type
     * interactions such as an app icon with a widget or shortcut.
     */
    private android.graphics.Rect collisionRectForInteraction(
            View view, int[] geometry, View other) {
        if (isAppIconCollisionParticipant(view) && isAppIconCollisionParticipant(other)) {
            return appIconVisualRectAt(view, geometry[0], geometry[1], geometry[2], geometry[3]);
        }
        return collisionRect(view, geometry);
    }

    private android.graphics.Rect collisionRectAtForInteraction(
            View view, int x, int y, int width, int height, View other) {
        if (isAppIconCollisionParticipant(view) && isAppIconCollisionParticipant(other)) {
            return appIconVisualRectAt(view, x, y, width, height);
        }
        return collisionRectAt(view, x, y, width, height);
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
        // Never mutate scale during boundary clamping. Changing scale while the
        // finger crosses an edge causes a visible flicker and makes resize feedback unstable.
        float visualWidth = panel.getWidth() * scaleX;
        float visualHeight = panel.getHeight() * scaleY;
        float visualLeft = panel.getLeft() + translationX
                + panel.getPivotX() * (1f - scaleX);
        float visualTop = panel.getTop() + translationY
                + panel.getPivotY() * (1f - scaleY);
        float maxVisualLeft = Math.max(0f, parentWidth - visualWidth);
        float maxVisualTop = Math.max(0f, parentHeight - visualHeight);
        float boundedLeft = Math.max(0f, Math.min(maxVisualLeft, visualLeft));
        float boundedTop = Math.max(0f, Math.min(maxVisualTop, visualTop));
        translationX += boundedLeft - visualLeft;
        translationY += boundedTop - visualTop;
        return new float[]{translationX, translationY};
    }

    private void setClampedClockTranslationForPanel(View panel, int page,
            float translationX, float translationY, boolean persist) {
        if (panel == null) return;
        float[] clamped = clampClockTranslation(panel, translationX, translationY);
        panel.setTranslationX(clamped[0]);
        panel.setTranslationY(clamped[1]);
        if (persist) {
            prefs.edit()
                    .putFloat(clockPrefKey(KEY_CLOCK_WIDGET_X, page), clamped[0])
                    .putFloat(clockPrefKey(KEY_CLOCK_WIDGET_Y, page), clamped[1])
                    .apply();
        }
    }

    private void setClampedClockTranslation(float translationX, float translationY, boolean persist) {
        setClampedClockTranslationForPanel(clockPanel, activeClockPage,
                translationX, translationY, persist);
    }

    /** Live push-aside behavior for workspace items while a widget or shortcut is dragged. */

    /**
     * Reverse temporary displacement as the finger moves back out of occupied space.
     * Keep the transaction's original position, but retarget the neighbour to that
     * position when it is no longer blocked by the mover or another item's current/
     * reserved bounds. The entry remains in the transaction so release can commit the
     * returned position and a renewed collision can push it away again.
     */
    private void restoreDisplacedItemsWhenDragReverses(FrameLayout page, View movingView,
            android.graphics.Rect movingRect) {
        if (page == null || movingView == null || movingRect == null) return;
        java.util.ArrayList<View> candidates =
                new java.util.ArrayList<View>(liveDisplacedItems.keySet());
        int[] movingGeometry = itemGeometry(movingView);
        for (View displaced : candidates) {
            if (displaced == movingView || displaced.getParent() != page) continue;
            int[] original = liveDisplacedItems.get(displaced);
            int[] target = liveDisplacementTargets.get(displaced);
            if (original == null || target == null
                    || (target[0] == original[0] && target[1] == original[1])) continue;

            int[] geometry = itemGeometry(displaced);
            int width = Math.max(1, geometry[2]);
            int height = Math.max(1, geometry[3]);
            android.graphics.Rect originalBounds = collisionRectAtForInteraction(
                    displaced, original[0], original[1], width, height, movingView);
            android.graphics.Rect movingBounds = collisionRectForInteraction(
                    movingView, movingGeometry, displaced);
            // Don't return an item while the mover still occupies its old visible area.
            if (android.graphics.Rect.intersects(movingBounds, originalBounds)) continue;
            if (originalBounds.left < 0 || originalBounds.top < 0
                    || originalBounds.right > page.getWidth()
                    || originalBounds.bottom > page.getHeight()) continue;

            boolean blocked = false;
            for (int i = 0; i < page.getChildCount(); i++) {
                View blocker = page.getChildAt(i);
                if (blocker == displaced || blocker == movingView
                        || blocker.getVisibility() != View.VISIBLE
                        || !isWorkspaceItem(blocker)) continue;
                int[] blockerGeometry = itemGeometry(blocker);
                if (blockerGeometry[2] <= 0 || blockerGeometry[3] <= 0) continue;
                if (android.graphics.Rect.intersects(originalBounds,
                        collisionRectForInteraction(blocker, blockerGeometry, displaced))) {
                    blocked = true;
                    break;
                }
                int[] blockerTarget = liveDisplacementTargets.get(blocker);
                if (blockerTarget != null
                        && android.graphics.Rect.intersects(originalBounds,
                                collisionRectAtForInteraction(blocker,
                                        blockerTarget[0], blockerTarget[1],
                                        blockerGeometry[2], blockerGeometry[3], displaced))) {
                    blocked = true;
                    break;
                }
            }
            if (blocked) continue;

            // Retargeting preserves the transaction record: if the finger moves forward
            // again, this same neighbour can be pushed away from its original position.
            liveDisplacementTargets.put(displaced,
                    new int[]{original[0], original[1]});
            animateDisplacedItemTo(displaced, original[0], original[1]);
        }
    }

    /** Refresh the active drag after a neighbour finishes moving, so push chains can
     * reverse back in order even when the finger pauses while an animation completes. */
    private void refreshDragDisplacementsAfterAnimation(View movedNeighbour,
            int targetX, int targetY) {
        int[] currentTarget = liveDisplacementTargets.get(movedNeighbour);
        if (currentTarget == null || currentTarget[0] != targetX || currentTarget[1] != targetY
                || !(movedNeighbour.getParent() instanceof FrameLayout)
                || pagesStrip == null) return;
        FrameLayout page = (FrameLayout) movedNeighbour.getParent();
        int pageIndex = pagesStrip.indexOfChild(page);
        if (workspaceTransferItem != null && workspaceTransferTargetPage == pageIndex) {
            if (workspaceTransferTargetPage != workspaceTransferOriginPage) {
                updateWorkspaceTransferDisplacements(workspaceTransferItem,
                        workspaceTransferLastRawX, workspaceTransferLastRawY,
                        workspaceTransferFingerOffsetX, workspaceTransferFingerOffsetY);
            } else if (workspaceTransferItem.getParent() == page) {
                scheduleLiveDisplacementUpdate(page, workspaceTransferItem);
            }
            return;
        }
        if (clockWidgetDragging && clockPanel != null && clockPanel.getParent() == page) {
            scheduleLiveDisplacementUpdate(page, clockPanel);
        }
    }

    /**
     * Chooses the shortest axis that separates a colliding neighbour from the dragged
     * item. The result is used as a stable push direction for the whole displacement
     * chain, so later MOVE events do not make each neighbour pick a different side.
     */
    private int[] resolveLiveDisplacementDirection(android.graphics.Rect movingBounds,
            android.graphics.Rect otherBounds) {
        int overlapX = Math.max(0, Math.min(movingBounds.right, otherBounds.right)
                - Math.max(movingBounds.left, otherBounds.left));
        int overlapY = Math.max(0, Math.min(movingBounds.bottom, otherBounds.bottom)
                - Math.max(movingBounds.top, otherBounds.top));
        long centerDeltaX = ((long) otherBounds.left + otherBounds.right)
                - ((long) movingBounds.left + movingBounds.right);
        long centerDeltaY = ((long) otherBounds.top + otherBounds.bottom)
                - ((long) movingBounds.top + movingBounds.bottom);
        if (overlapX <= overlapY || centerDeltaY == 0L) {
            int direction = centerDeltaX < 0L ? -1 : 1;
            if (centerDeltaX == 0L && otherBounds.left < movingBounds.left) direction = -1;
            return new int[]{direction, 0};
        }
        int direction = centerDeltaY < 0L ? -1 : 1;
        if (centerDeltaY == 0L && otherBounds.top < movingBounds.top) direction = -1;
        return new int[]{0, direction};
    }

    /**
     * Forward movement wins over sideways/backward escapes. Within the preferred
     * half-plane, keep the chain straight and choose the smallest necessary movement.
     */
    private long liveDisplacementCandidateScore(int x, int y, int baseX, int baseY,
            int pushDx, int pushDy) {
        long offsetX = (long) x - baseX;
        long offsetY = (long) y - baseY;
        long projection = offsetX * pushDx + offsetY * pushDy;
        long backwardPenalty = projection > 0L ? 0L : 1L;
        long perpendicular = pushDx != 0 ? Math.abs(offsetY) : Math.abs(offsetX);
        long distance = Math.abs(offsetX) + Math.abs(offsetY);
        return backwardPenalty * 1_000_000_000_000L
                + perpendicular * 1_000_000L + distance;
    }

    private void updateLiveDisplacements(FrameLayout page, View movingView) {
        // This entry point is used at transaction boundaries as well as by animation
        // callbacks. Any older queued pass is now stale; synchronous validation wins.
        cancelPendingLiveDisplacementUpdate();
        pendingAppDisplacementTargets.clear();
        liveDragGeometryCache.clear();
        if (page == null || movingView == null || movingView.getParent() != page
                || page.getWidth() <= 0 || page.getHeight() <= 0) return;

        int[] moving = itemGeometry(movingView);
        if (moving[2] <= 0 || moving[3] <= 0) return;
        android.graphics.Rect movingRect = collisionRect(movingView, moving);
        liveDragAppVisualBoundsCache.clear();
        liveDragPlanningCacheActive = true;
        // Start moving neighbours while the dragged item approaches from any side,
        // not only after their visible bounds already touch.
        // Push a neighbour only once the dragged item actually reaches its bounds.
        // A large approach gap makes nearby items jump before the drop is real.
        int gap = 0;

        // Displacement is a live preview, not a permanent one-way shuffle. When the
        // mover leaves a neighbour's original footprint, animate that neighbour back
        // if its original rectangle is free. If the finger reverses again, the same
        // reserved target is eligible to be pushed away once more.
        restoreDisplacedItemsWhenDragReverses(page, movingView, movingRect);
        // Reverse-restoration can retarget animations; take one consistent snapshot
        // after it, then reuse geometry for repeated obstacle tests in this pass.
        liveDragGeometryCache.clear();
        for (int i = 0; i < page.getChildCount(); i++) {
            View other = page.getChildAt(i);
            if (other == movingView || !isWorkspaceItem(other)) continue;

            boolean alreadyDisplaced = liveDisplacedItems.containsKey(other);
            int[] existingTarget = liveDisplacementTargets.get(other);
            int[] g = cachedLiveDragGeometry(other);
            if (g[2] <= 0 || g[3] <= 0) continue;
            android.graphics.Rect movingBoundsForOther =
                    collisionRectForInteraction(movingView, moving, other);
            android.graphics.Rect otherRect = collisionRectForInteraction(other, g, movingView);
            boolean currentCollides = overlapsWithGap(movingBoundsForOther, otherRect, gap);
            boolean targetCollides = alreadyDisplaced && existingTarget != null
                    && overlapsWithGap(movingBoundsForOther, collisionRectAtForInteraction(other,
                            existingTarget[0], existingTarget[1], g[2], g[3], movingView), gap);
            // An item whose assigned destination is clear must be allowed to finish
            // moving out of its original collision zone. Replanning from its current
            // animation frame on every MOVE cancelled its animation repeatedly.
            if (alreadyDisplaced && !targetCollides) continue;
            if (!alreadyDisplaced && !currentCollides) continue;

            android.graphics.Rect directionBounds = otherRect;
            if (!currentCollides && targetCollides && existingTarget != null) {
                directionBounds = collisionRectAtForInteraction(other,
                        existingTarget[0], existingTarget[1], g[2], g[3], movingView);
            }
            int[] pushDirection = resolveLiveDisplacementDirection(
                    movingBoundsForOther, directionBounds);
            int pushDx = pushDirection[0];
            int pushDy = pushDirection[1];
            int preferredDisplacementX = existingTarget == null ? g[0] : existingTarget[0];
            int preferredDisplacementY = existingTarget == null ? g[1] : existingTarget[1];

            // If the dragged item reaches a neighbour's planned destination, the complete
            // affected chain is replanned in one direction instead of reacting item by item.

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
            java.util.ArrayList<View> obstacleOwners = new java.util.ArrayList<View>();
            for (int j = 0; j < page.getChildCount(); j++) {
                View blocker = page.getChildAt(j);
                // The cross-screen drag proxy is intentionally not a workspace item
                // itself, but its footprint must still be reserved as an obstacle.
                // Otherwise a displaced neighbour can select the dragged item's
                // destination as its own "free" slot and appear not to move at all.
                if (blocker == other
                        || (blocker != movingView && !isWorkspaceItem(blocker))) continue;
                int[] bg = cachedLiveDragGeometry(blocker);
                if (bg[2] <= 0 || bg[3] <= 0) continue;
                // Reserve both the current visual footprint and any destination already
                // assigned to an in-flight neighbour. Without target reservations, two
                // items processed in the same MOVE event can choose the same empty pocket
                // because the first animation has not moved its view there yet.
                obstacles.add(collisionRectForInteraction(blocker, bg, other));
                obstacleOwners.add(blocker);
                int[] plannedTarget = liveDisplacementTargets.get(blocker);
                if (plannedTarget != null) {
                    obstacles.add(collisionRectAtForInteraction(blocker,
                            plannedTarget[0], plannedTarget[1],
                            Math.max(1, bg[2]), Math.max(1, bg[3]), other));
                    obstacleOwners.add(blocker);
                }
            }
            for (java.util.Map.Entry<View, int[]> entry : liveDisplacedItems.entrySet()) {
                View reserved = entry.getKey();
                if (reserved == other || reserved.getParent() != page) continue;
                int[] original = entry.getValue();
                int[] rg = cachedLiveDragGeometry(reserved);
                obstacles.add(collisionRectAtForInteraction(
                        reserved, original[0], original[1], rg[2], rg[3], other));
                obstacleOwners.add(reserved);
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

            int bestX = -1, bestY = -1;
            long bestScore = Long.MAX_VALUE;
            if (isHomeGridIcon(other)) {
                // App cells must remain snapped to the home grid during displacement.
                int columns = homeGridColumnsForItem(other);
                int cellWidth = Math.max(1, page.getWidth() / Math.max(1, columns));
                int iconSize = prefs == null ? 48 : prefs.getInt(KEY_ICON_SIZE, 48);
                int cellHeight = LauncherUiUtils.dp(this, homeCellHeightDp(iconSize));
                int rows = homeGridRowsForHeight(page.getHeight(), cellHeight);
                xCandidates.clear();
                yCandidates.clear();
                for (int col = 0; col < columns; col++) {
                    int x = homeGridIconLeftAt(other, col, cellWidth, g[2]);
                    if (x + g[2] <= page.getWidth()) xCandidates.add(x);
                }
                for (int row = 0; row < rows; row++) {
                    int y = homeGridIconTopAt(page, other, row, rows, cellHeight);
                    if (y + g[3] <= page.getHeight()) yCandidates.add(y);
                }
            }
            for (Integer candidateY : yCandidates) {
                for (Integer candidateX : xCandidates) {
                    int x = candidateX.intValue();
                    int y = candidateY.intValue();
                    // Compare primitive bounds directly: allocating a Rect for every
                    // candidate creates substantial short-lived garbage during live drag.
                    int candidateRight = x + g[2];
                    int candidateBottom = y + g[3];
                    android.graphics.Rect candidateVisible = isHomeGridIcon(other)
                            ? appIconVisualRectAt(other, x, y, g[2], g[3]) : null;
                    boolean blocked = false;
                    for (int obstacleIndex = 0; obstacleIndex < obstacles.size(); obstacleIndex++) {
                        android.graphics.Rect obstacle = obstacles.get(obstacleIndex);
                        View obstacleOwner = obstacleOwners.get(obstacleIndex);
                        if (candidateVisible != null
                                && isAppIconCollisionParticipant(obstacleOwner)) {
                            if (android.graphics.Rect.intersects(candidateVisible, obstacle)) {
                                blocked = true;
                                break;
                            }
                        } else if (x < obstacle.right && candidateRight > obstacle.left
                                && y < obstacle.bottom && candidateBottom > obstacle.top) {
                            blocked = true;
                            break;
                        }
                    }
                    if (blocked) continue;
                    long score = liveDisplacementCandidateScore(x, y,
                            preferredDisplacementX, preferredDisplacementY, pushDx, pushDy);
                    if (score < bestScore || (score == bestScore
                            && (bestY < 0 || y < bestY || (y == bestY && x < bestX)))) {
                        bestScore = score;
                        bestX = x;
                        bestY = y;
                    }
                }
            }

            // Edge-derived candidates are fast, but can miss a valid pocket when
            // obstacles form irregular shapes. Only when that search fails, scan the
            // page at a modest interval and choose the nearest collision-free position.
            // This makes displacement work consistently near the top as well as below.
            if (bestX < 0 && !isHomeGridIcon(other)) {
                int step = Math.max(1, LauncherUiUtils.dp(this, 6));
                for (int y = 0; y <= maxY + step; y += step) {
                    int candidateY = Math.min(maxY, y);
                    for (int x = 0; x <= maxX + step; x += step) {
                        int candidateX = Math.min(maxX, x);
                        // The 6dp fallback is only entered if the edge search found no
                        // usable target. Rank every free candidate by the same directional
                        // score as the fast path so it cannot reintroduce opposite-side jumps.
                        boolean blocked = false;
                        int candidateRight = candidateX + g[2];
                        int candidateBottom = candidateY + g[3];
                        for (android.graphics.Rect obstacle : obstacles) {
                            if (candidateX < obstacle.right && candidateRight > obstacle.left
                                    && candidateY < obstacle.bottom && candidateBottom > obstacle.top) {
                                blocked = true;
                                break;
                            }
                        }
                        if (blocked) continue;
                        long score = liveDisplacementCandidateScore(candidateX, candidateY,
                                preferredDisplacementX, preferredDisplacementY, pushDx, pushDy);
                        if (score < bestScore || (score == bestScore
                                && (bestY < 0 || candidateY < bestY
                                || (candidateY == bestY && candidateX < bestX)))) {
                            bestScore = score;
                            bestX = candidateX;
                            bestY = candidateY;
                        }
                    }
                    if (candidateY == maxY) break;
                }
            }

            if (!alreadyDisplaced && isHomeGridIcon(movingView)
                    && isHomeGridIcon(other)
                    && tryShiftHomeIconCohort(page, movingView, other, bestX, bestY)) {
                continue;
            }

            {
                // Plan a direction-aware push chain before accepting a nearby empty slot.
                // This prevents a shortcut/app/widget from escaping one way first and then
                // being pushed again when the dragged item advances another few pixels.
                java.util.IdentityHashMap<View, int[]> cascadePlan =
                        new java.util.IdentityHashMap<View, int[]>();
                java.util.IdentityHashMap<View, Boolean> recursionStack =
                        new java.util.IdentityHashMap<View, Boolean>();
                int[] searchBudget = new int[]{240};
                if (findCascadeDisplacementPlan(page, other, movingView, movingRect,
                        preferredDisplacementX, preferredDisplacementY, pushDx, pushDy,
                        cascadePlan, recursionStack, 0, searchBudget)) {
                    // Reserve every target before starting any animation. This prevents
                    // asynchronous animation callbacks from seeing a partly committed chain.
                    java.util.ArrayList<View> cascadeParticipantsToAnimate =
                            new java.util.ArrayList<View>();
                    for (java.util.Map.Entry<View, int[]> planned : cascadePlan.entrySet()) {
                        View participant = planned.getKey();
                        int[] destination = planned.getValue();
                        int[] oldTarget = liveDisplacementTargets.get(participant);
                        if (oldTarget != null && oldTarget[0] == destination[0]
                                && oldTarget[1] == destination[1]) continue;
                        participant.animate().cancel();
                        liveDragGeometryCache.remove(participant);
                        int[] currentGeometry = itemGeometry(participant);
                        liveDragGeometryCache.put(participant, currentGeometry);
                        if (!liveDisplacedItems.containsKey(participant)) {
                            liveDisplacedItems.put(participant,
                                    new int[]{currentGeometry[0], currentGeometry[1]});
                        }
                        liveDisplacementTargets.put(participant,
                                new int[]{destination[0], destination[1]});
                        cascadeParticipantsToAnimate.add(participant);
                    }
                    for (View participant : cascadeParticipantsToAnimate) {
                        int[] destination = liveDisplacementTargets.get(participant);
                        if (destination != null) {
                            animateDisplacedItemTo(participant, destination[0], destination[1]);
                        }
                    }
                    continue;
                }
            }

            if (bestX >= 0) {
                if (alreadyDisplaced && existingTarget != null
                        && existingTarget[0] == bestX && existingTarget[1] == bestY) continue;
                // Recompute the visual origin after cancelling any old motion so the
                // next translation starts from the exact frame that was on screen.
                other.animate().cancel();
                liveDragGeometryCache.remove(other);
                g = itemGeometry(other);
                liveDragGeometryCache.put(other, g);
                if (!liveDisplacedItems.containsKey(other)) {
                    liveDisplacedItems.put(other, new int[]{g[0], g[1]});
                }
                liveDisplacementTargets.put(other, new int[]{bestX, bestY});

                // Move the neighbour to its reserved free rectangle with a short
                // synchronized animation; the reservation is recorded before motion starts.
                animateDisplacedItemTo(other, bestX, bestY);
            }
        }
    
        flushPendingAppDisplacementAnimations();
        liveDragGeometryCache.clear();
        enforceLiveDragCollisionSafety(page, movingView);
        liveDragPlanningCacheActive = false;
        liveDragAppVisualBoundsCache.clear();
    }


    /**
     * Hold an actively dragged item at its last collision-free position if a visible
     * collision remains after live displacement is scheduled. Neighbours can have a
     * free destination while still occupying old bounds during their short animation.
     * Invisible destination proxies are excluded; real cross-page drops are checked at
     * their exact snapped destination before the gesture commits.
     */
    private void enforceLiveDragCollisionSafety(FrameLayout page, View movingView) {
        if (page == null || movingView == null || movingView.getParent() != page
                || "workspace_transfer_proxy".equals(movingView.getTag())) return;
        if (movingView == workspaceTransferItem
                && workspaceTransferOriginPage >= 0 && workspaceTransferTargetPage >= 0
                && workspaceTransferTargetPage != workspaceTransferOriginPage) return;

        int[] geometry = itemGeometry(movingView);
        boolean collision = false;
        for (int i = 0; i < page.getChildCount(); i++) {
            View other = page.getChildAt(i);
            if (other == movingView || other.getVisibility() != View.VISIBLE
                    || !isWorkspaceItem(other)) continue;
            int[] otherGeometry = itemGeometry(other);
            if (otherGeometry[2] <= 0 || otherGeometry[3] <= 0) continue;
            android.graphics.Rect movingBounds =
                    collisionRectForInteraction(movingView, geometry, other);
            android.graphics.Rect otherBounds =
                    collisionRectForInteraction(other, otherGeometry, movingView);
            if (android.graphics.Rect.intersects(movingBounds, otherBounds)) {
                collision = true;
                break;
            }
        }
        if (!collision) {
            liveDragLastSafeGeometry.put(movingView,
                    new int[]{geometry[0], geometry[1]});
            return;
        }

        int[] safe = liveDragLastSafeGeometry.get(movingView);
        if (safe == null) return;
        if (isClockWorkspaceItem(movingView)) {
            setClockVisualPosition(movingView, safe[0], safe[1], false);
        } else if (movingView.getLayoutParams() instanceof FrameLayout.LayoutParams) {
            FrameLayout.LayoutParams lp =
                    (FrameLayout.LayoutParams) movingView.getLayoutParams();
            float sx = Math.max(0.01f, movingView.getScaleX());
            float sy = Math.max(0.01f, movingView.getScaleY());
            lp.leftMargin = safe[0]
                    - Math.round(movingView.getPivotX() * (1f - sx));
            lp.topMargin = safe[1]
                    - Math.round(movingView.getPivotY() * (1f - sy));
            movingView.setTranslationX(0f);
            movingView.setTranslationY(0f);
            movingView.setLayoutParams(lp);
        }
    }
    /**
     * Recursively plans a short push chain when every direct destination is occupied.
     * The moving item and all planned destinations are reserved, while blockers may be
     * assigned another slot in the same transaction. A bounded search keeps this safe
     * for older devices when a workspace is densely populated.
     */


    private int compareCascadeCandidates(long scoreA, int xA, int yA,
            long scoreB, int xB, int yB) {
        int compare = Long.compare(scoreA, scoreB);
        if (compare == 0) compare = Integer.compare(yA, yB);
        if (compare == 0) compare = Integer.compare(xA, xB);
        return compare;
    }

    /**
     * Selects the nearest 96 candidate coordinates using a fixed-size max-heap.
     * It avoids allocating/sorting the full Cartesian product while preserving the
     * old stable-sort order: Manhattan distance, then ascending y/x.
     */
    private java.util.ArrayList<int[]> nearestCascadeCandidates(
            java.util.TreeSet<Integer> xs, java.util.TreeSet<Integer> ys,
            int preferredX, int preferredY, int pushDx, int pushDy) {
        final int limit = Math.min(96, xs.size() * ys.size());
        java.util.ArrayList<int[]> result = new java.util.ArrayList<int[]>(limit);
        if (limit <= 0) return result;

        int[] heapX = new int[limit];
        int[] heapY = new int[limit];
        long[] heapDistance = new long[limit];
        int count = 0;
        for (Integer cyValue : ys) {
            int y = cyValue.intValue();
            for (Integer cxValue : xs) {
                int x = cxValue.intValue();
                long distance = liveDisplacementCandidateScore(
                        x, y, preferredX, preferredY, pushDx, pushDy);
                if (count < limit) {
                    int index = count++;
                    heapX[index] = x;
                    heapY[index] = y;
                    heapDistance[index] = distance;
                    while (index > 0) {
                        int parent = (index - 1) >>> 1;
                        if (compareCascadeCandidates(heapDistance[parent],
                                heapX[parent], heapY[parent], heapDistance[index],
                                heapX[index], heapY[index]) >= 0) break;
                        swapCascadeHeapEntries(
                                heapX, heapY, heapDistance, parent, index);
                        index = parent;
                    }
                } else if (compareCascadeCandidates(distance, x, y,
                        heapDistance[0], heapX[0], heapY[0]) < 0) {
                    heapX[0] = x;
                    heapY[0] = y;
                    heapDistance[0] = distance;
                    int parent = 0;
                    while (true) {
                        int left = parent * 2 + 1;
                        if (left >= count) break;
                        int right = left + 1;
                        int largest = left;
                        if (right < count
                                && compareCascadeCandidates(heapDistance[right],
                                        heapX[right], heapY[right],
                                        heapDistance[left], heapX[left], heapY[left]) > 0) {
                            largest = right;
                        }
                        if (compareCascadeCandidates(heapDistance[parent],
                                heapX[parent], heapY[parent], heapDistance[largest],
                                heapX[largest], heapY[largest]) >= 0) break;
                        swapCascadeHeapEntries(
                                heapX, heapY, heapDistance, parent, largest);
                        parent = largest;
                    }
                }
            }
        }

        for (int i = 0; i < count; i++) {
            result.add(new int[]{heapX[i], heapY[i]});
        }
        java.util.Collections.sort(result, (a, b) -> {
            long da = liveDisplacementCandidateScore(
                    a[0], a[1], preferredX, preferredY, pushDx, pushDy);
            long db = liveDisplacementCandidateScore(
                    b[0], b[1], preferredX, preferredY, pushDx, pushDy);
            return compareCascadeCandidates(da, a[0], a[1], db, b[0], b[1]);
        });
        return result;
    }

    private void swapCascadeHeapEntries(
            int[] xs, int[] ys, long[] distances, int a, int b) {
        int x = xs[a];
        xs[a] = xs[b];
        xs[b] = x;
        int y = ys[a];
        ys[a] = ys[b];
        ys[b] = y;
        long distance = distances[a];
        distances[a] = distances[b];
        distances[b] = distance;
    }

    private boolean findCascadeDisplacementPlan(FrameLayout page, View item, View movingView,
            android.graphics.Rect movingRect, int preferredX, int preferredY,
            int pushDx, int pushDy,
            java.util.IdentityHashMap<View, int[]> plan,
            java.util.IdentityHashMap<View, Boolean> recursionStack,
            int depth, int[] searchBudget) {
        if (page == null || item == null || movingView == null
                || depth > 6 || searchBudget[0] <= 0
                || item == movingView || item.getParent() != page
                || recursionStack.containsKey(item)) return false;

        int[] geometry = itemGeometry(item);
        int width = Math.max(1, geometry[2]);
        int height = Math.max(1, geometry[3]);
        int maxX = Math.max(0, page.getWidth() - width);
        int maxY = Math.max(0, page.getHeight() - height);
        preferredX = Math.max(0, Math.min(maxX, preferredX));
        preferredY = Math.max(0, Math.min(maxY, preferredY));

        java.util.TreeSet<Integer> xs = new java.util.TreeSet<Integer>();
        java.util.TreeSet<Integer> ys = new java.util.TreeSet<Integer>();
        xs.add(preferredX); ys.add(preferredY);
        xs.add(0); ys.add(0); xs.add(maxX); ys.add(maxY);
        boolean appIcon = isHomeGridIcon(item);
        if (appIcon) {
            int columns = homeGridColumnsForItem(item);
            int cellWidth = Math.max(1, page.getWidth() / columns);
            int iconSize = prefs == null ? 48 : prefs.getInt(KEY_ICON_SIZE, 48);
            int cellHeight = LauncherUiUtils.dp(this, homeCellHeightDp(iconSize));
            int rows = homeGridRowsForHeight(page.getHeight(), cellHeight);
            xs.clear(); ys.clear();
            for (int col = 0; col < columns; col++) {
                int x = homeGridIconLeftAt(item, col, cellWidth, width);
                if (x + width <= page.getWidth()) xs.add(x);
            }
            for (int row = 0; row < rows; row++) {
                int y = homeGridIconTopAt(page, item, row, rows, cellHeight);
                if (y + height <= page.getHeight()) ys.add(y);
            }
        } else {
            for (int i = 0; i < page.getChildCount(); i++) {
                View blocker = page.getChildAt(i);
                if (blocker == item || blocker == movingView || !isWorkspaceItem(blocker)) continue;
                int[] bg = itemGeometry(blocker);
                if (bg[2] <= 0 || bg[3] <= 0) continue;
                android.graphics.Rect bounds = collisionRectForInteraction(blocker, bg, item);
                xs.add(Math.max(0, Math.min(maxX, bounds.left - width)));
                xs.add(Math.max(0, Math.min(maxX, bounds.right)));
                ys.add(Math.max(0, Math.min(maxY, bounds.top - height)));
                ys.add(Math.max(0, Math.min(maxY, bounds.bottom)));
                int[] target = plan.get(blocker);
                if (target == null) target = liveDisplacementTargets.get(blocker);
                if (target != null) {
                    android.graphics.Rect targetBounds = collisionRectAtForInteraction(blocker,
                            target[0], target[1], bg[2], bg[3], item);
                    xs.add(Math.max(0, Math.min(maxX, targetBounds.left - width)));
                    xs.add(Math.max(0, Math.min(maxX, targetBounds.right)));
                    ys.add(Math.max(0, Math.min(maxY, targetBounds.top - height)));
                    ys.add(Math.max(0, Math.min(maxY, targetBounds.bottom)));
                }
            }
            // Exact rectangle-edge positions are sufficient for the cascade planner.
            // Leave coarse-grid scanning to the non-recursive fallback so this bounded
            // search does not multiply thousands of scan candidates at every depth.
        }

        // Avoid allocating the full Cartesian product; retain only the nearest 96.
        java.util.ArrayList<int[]> candidates =
                nearestCascadeCandidates(xs, ys, preferredX, preferredY, pushDx, pushDy);

        recursionStack.put(item, Boolean.TRUE);
        for (int[] candidatePosition : candidates) {
            if (searchBudget[0]-- <= 0) break;
            int x = candidatePosition[0], y = candidatePosition[1];
            android.graphics.Rect candidateForMover =
                    collisionRectAtForInteraction(item, x, y, width, height, movingView);
            int[] movingGeometry = itemGeometry(movingView);
            android.graphics.Rect movingBoundsForItem =
                    collisionRectForInteraction(movingView, movingGeometry, item);
            if (android.graphics.Rect.intersects(candidateForMover, movingBoundsForItem)) continue;

            // Keep prior footprints reserved during a live preview. Otherwise another
            // chain member can occupy a displaced item's restore slot, so reversing the
            // drag no longer restores the original arrangement cleanly.
            boolean reservedOriginalConflict = false;
            for (java.util.Map.Entry<View, int[]> reservedEntry : liveDisplacedItems.entrySet()) {
                View reservedItem = reservedEntry.getKey();
                if (reservedItem == item || reservedItem == movingView
                        || reservedItem.getParent() != page) continue;
                int[] reservedGeometry = itemGeometry(reservedItem);
                if (reservedGeometry[2] <= 0 || reservedGeometry[3] <= 0) continue;
                int[] originalPosition = reservedEntry.getValue();
                android.graphics.Rect originalBounds = collisionRectAtForInteraction(
                        reservedItem, originalPosition[0], originalPosition[1],
                        reservedGeometry[2], reservedGeometry[3], item);
                if (android.graphics.Rect.intersects(candidateForMover, originalBounds)) {
                    reservedOriginalConflict = true;
                    break;
                }
            }
            if (reservedOriginalConflict) continue;

            java.util.ArrayList<View> blockers = new java.util.ArrayList<View>();
            for (int i = 0; i < page.getChildCount(); i++) {
                View blocker = page.getChildAt(i);
                if (blocker == item || blocker == movingView || !isWorkspaceItem(blocker)) continue;
                int[] bg = itemGeometry(blocker);
                if (bg[2] <= 0 || bg[3] <= 0) continue;

                android.graphics.Rect candidateForBlocker =
                        collisionRectAtForInteraction(item, x, y, width, height, blocker);
                int[] localTarget = plan.get(blocker);
                int[] existingTarget = liveDisplacementTargets.get(blocker);
                if (localTarget != null) {
                    android.graphics.Rect targetRect = collisionRectAtForInteraction(blocker,
                            localTarget[0], localTarget[1], bg[2], bg[3], item);
                    if (android.graphics.Rect.intersects(candidateForBlocker, targetRect)) blockers.add(blocker);
                } else {
                    android.graphics.Rect currentRect = collisionRectForInteraction(blocker, bg, item);
                    boolean hit = android.graphics.Rect.intersects(candidateForBlocker, currentRect);
                    if (existingTarget != null) {
                        android.graphics.Rect targetRect = collisionRectAtForInteraction(blocker,
                                existingTarget[0], existingTarget[1], bg[2], bg[3], item);
                        hit = hit || android.graphics.Rect.intersects(candidateForBlocker, targetRect);
                    }
                    if (hit) blockers.add(blocker);
                }
            }

            java.util.IdentityHashMap<View, int[]> trial =
                    new java.util.IdentityHashMap<View, int[]>(plan);
            trial.put(item, new int[]{x, y});
            boolean solved = true;
            for (View blocker : blockers) {
                if (recursionStack.containsKey(blocker)) {
                    solved = false;
                    break;
                }
                java.util.IdentityHashMap<View, Boolean> nextStack =
                        new java.util.IdentityHashMap<View, Boolean>(recursionStack);
                if (!findCascadeDisplacementPlan(page, blocker, movingView, movingRect,
                        currentOrPlannedX(blocker, trial), currentOrPlannedY(blocker, trial),
                        pushDx, pushDy, trial, nextStack, depth + 1, searchBudget)) {
                    solved = false;
                    break;
                }
            }
            if (solved) {
                plan.clear();
                plan.putAll(trial);
                recursionStack.remove(item);
                return true;
            }
        }
        recursionStack.remove(item);
        return false;
    }

    private int currentOrPlannedX(View item, java.util.IdentityHashMap<View, int[]> plan) {
        int[] target = plan.get(item);
        if (target == null) target = liveDisplacementTargets.get(item);
        if (target != null) return target[0];
        return itemGeometry(item)[0];
    }

    private int currentOrPlannedY(View item, java.util.IdentityHashMap<View, int[]> plan) {
        int[] target = plan.get(item);
        if (target == null) target = liveDisplacementTargets.get(item);
        if (target != null) return target[1];
        return itemGeometry(item)[1];
    }

    /**
     * Final safety pass after a drag/resize. Treat every app, widget, shortcut and
     * clock as a rectangle and place each item into a genuinely free pixel position.
     * App icons keep their existing size; non-app items use their visual dimensions.
     */
    private boolean hasWorkspaceVisualOverlaps(FrameLayout page) {
        if (page == null || page.getWidth() <= 0 || page.getHeight() <= 0) return false;
        java.util.ArrayList<android.graphics.Rect> visualBounds =
                new java.util.ArrayList<android.graphics.Rect>();
        for (int i = 0; i < page.getChildCount(); i++) {
            View item = page.getChildAt(i);
            if (!isWorkspaceItem(item) || item.getVisibility() != View.VISIBLE) continue;
            int[] g = itemGeometry(item);
            if (g[2] <= 0 || g[3] <= 0) continue;
            android.graphics.Rect bounds = isHomeGridIcon(item)
                    ? appIconVisualRectAt(item, g[0], g[1], g[2], g[3])
                    : new android.graphics.Rect(g[0], g[1], g[0] + g[2], g[1] + g[3]);
            // Out-of-page content still needs the safety resolver even when it
            // does not overlap another item.
            if (bounds.left < 0 || bounds.top < 0
                    || bounds.right > page.getWidth() || bounds.bottom > page.getHeight()) {
                return true;
            }
            for (android.graphics.Rect prior : visualBounds) {
                if (android.graphics.Rect.intersects(prior, bounds)) return true;
            }
            visualBounds.add(bounds);
        }
        return false;
    }

    private void resolveAllWorkspaceOverlaps(FrameLayout page, View priorityView) {
        if (page == null || page.getWidth() <= 0 || page.getHeight() <= 0) return;
        // The legacy resolver always rebuilt a nearest-free plan for every item,
        // even when the page was already visually collision-free. That caused needless
        // position changes and a costly post-drop pause. Make it a true no-op in that case.
        if (!hasWorkspaceVisualOverlaps(page)) return;
        java.util.ArrayList<View> items = new java.util.ArrayList<View>();
        // Preserve already-placed workspace items first. The moving item is placed
        // last, after the resolver knows which visible regions are actually free.
        // updateLiveDisplacements() already previews the intended push-aside layout;
        // this final pass must not accept a mover and leave its neighbors overlapping.
        for (int i = 0; i < page.getChildCount(); i++) {
            View child = page.getChildAt(i);
            if (child == priorityView || !isWorkspaceItem(child)) continue;
            if (isClockWorkspaceItem(child)) items.add(0, child);
            else items.add(child);
        }
        if (priorityView != null && priorityView.getParent() == page && isWorkspaceItem(priorityView)) {
            items.add(priorityView);
        }
        java.util.ArrayList<android.graphics.Rect> accepted = new java.util.ArrayList<android.graphics.Rect>();
        java.util.ArrayList<View> acceptedViews = new java.util.ArrayList<View>();
        for (View item : items) {
            if (item.getTag() instanceof String && prefs != null) {
                String tag = (String) item.getTag();
                String baseXKey = "workspace_base_scale_x_" + tag;
                String baseYKey = "workspace_base_scale_y_" + tag;
                if (prefs.contains(baseXKey) || prefs.contains(baseYKey)) {
                    item.setScaleX(prefs.getFloat(baseXKey, 1f));
                    item.setScaleY(prefs.getFloat(baseYKey, 1f));
                    prefs.edit().remove("workspace_scale_x_" + tag)
                            .remove("workspace_scale_y_" + tag)
                            .remove(baseXKey).remove(baseYKey).apply();
                }
            }
            int[] g = itemGeometry(item);
            int w = Math.max(1, g[2]), h = Math.max(1, g[3]);
            int maxX = Math.max(0, page.getWidth() - w);
            int maxY = Math.max(0, page.getHeight() - h);
            int startX = Math.max(0, Math.min(maxX, g[0]));
            int startY = Math.max(0, Math.min(maxY, g[1]));
            java.util.TreeSet<Integer> xs = new java.util.TreeSet<Integer>();
            java.util.TreeSet<Integer> ys = new java.util.TreeSet<Integer>();
            xs.add(startX); ys.add(startY); xs.add(0); ys.add(0); xs.add(maxX); ys.add(maxY);
            int margin = workspaceCollisionMargin(item);
            for (android.graphics.Rect a : accepted) {
                xs.add(Math.max(0, Math.min(maxX, a.left - w - margin)));
                xs.add(Math.max(0, Math.min(maxX, a.right + margin)));
                ys.add(Math.max(0, Math.min(maxY, a.top - h - margin)));
                ys.add(Math.max(0, Math.min(maxY, a.bottom + margin)));
            }
            if (isHomeGridIcon(item)) {
                // App cells may be collision-tested by their visible content, but
                // their origins can only be chosen from legal grid slots.
                xs.clear();
                ys.clear();
                int columns = homeGridColumnsForItem(item);
                int cellWidth = Math.max(1, page.getWidth() / Math.max(1, columns));
                int iconSize = prefs == null ? 48 : prefs.getInt(KEY_ICON_SIZE, 48);
                int cellHeight = LauncherUiUtils.dp(this, homeCellHeightDp(iconSize));
                int rows = homeGridRowsForHeight(page.getHeight(), cellHeight);
                if (isWorkspaceContactShortcut(item)) {
                    w = Math.max(1, g[2]);
                    h = Math.max(1, g[3]);
                } else {
                    w = cellWidth;
                    h = cellHeight;
                }
                maxX = Math.max(0, page.getWidth() - w);
                maxY = Math.max(0, page.getHeight() - h);
                for (int col = 0; col < columns; col++) {
                    int x = homeGridIconLeftAt(item, col, cellWidth, w);
                    if (x <= maxX) xs.add(x);
                }
                for (int row = 0; row < rows; row++) {
                    int y = homeGridIconTopAt(page, item, row, rows, cellHeight);
                    if (y <= maxY) ys.add(y);
                }
            }

            // Preserve any item that is already inside the page and clear of higher-
            // priority items. Only colliding items enter the nearest-free search below.
            // Besides avoiding needless repositioning, this removes most resolver work
            // when one item overlaps but the rest of the workspace is already correct.
            boolean currentInsidePage = g[0] >= 0 && g[1] >= 0
                    && g[0] + w <= page.getWidth() && g[1] + h <= page.getHeight();
            if (currentInsidePage) {
                android.graphics.Rect currentCandidate = collisionRectAt(item, g[0], g[1], w, h);
                boolean currentBlocked = false;
                for (android.graphics.Rect prior : accepted) {
                    if (android.graphics.Rect.intersects(currentCandidate, prior)) {
                        currentBlocked = true;
                        break;
                    }
                }
                if (!currentBlocked) {
                    accepted.add(currentCandidate);
                    acceptedViews.add(item);
                    continue;
                }
            }

            int bestX = -1, bestY = -1;
            long bestDistance = Long.MAX_VALUE;
            for (Integer cy : ys) for (Integer cx : xs) {
                int x = cx.intValue(), y = cy.intValue();
                android.graphics.Rect candidate = collisionRectAt(item, x, y, w, h);
                boolean blocked = false;
                for (android.graphics.Rect a : accepted) {
                    if (android.graphics.Rect.intersects(candidate, a)) { blocked = true; break; }
                }
                if (blocked) continue;
                long distance = Math.abs((long) x - g[0]) + Math.abs((long) y - g[1]);
                if (distance < bestDistance) { bestDistance = distance; bestX = x; bestY = y; }
            }
            // Edge-derived candidates can miss narrow free pockets around mixed-size
            // items. Before declaring the page full, perform an exact pixel scan at a
            // coarse step, including the final right/bottom edge. The nearest legal
            // rectangle wins; this is the same rectangle geometry used by drag/resize.
            if (bestX < 0 && !isHomeGridIcon(item)) {
                int step = Math.max(1, LauncherUiUtils.dp(this, 6));
                outerScan:
                for (int y = 0; y <= maxY; y += step) {
                    int candidateY = Math.min(maxY, y);
                    for (int x = 0; x <= maxX; x += step) {
                        int candidateX = Math.min(maxX, x);
                        android.graphics.Rect candidate = collisionRectAt(
                                item, candidateX, candidateY, w, h);
                        boolean blocked = false;
                        for (android.graphics.Rect a : accepted) {
                            if (android.graphics.Rect.intersects(candidate, a)) {
                                blocked = true;
                                break;
                            }
                        }
                        if (!blocked) {
                            long distance = Math.abs((long) candidateX - g[0])
                                    + Math.abs((long) candidateY - g[1]);
                            if (distance < bestDistance) {
                                bestDistance = distance;
                                bestX = candidateX;
                                bestY = candidateY;
                            }
                        }
                    }
                    if (candidateY == maxY) break outerScan;
                }
                // Explicitly test the far edges; stepping can otherwise skip them.
                int[][] edgeCandidates = new int[][]{
                        {maxX, startY}, {startX, maxY}, {maxX, maxY}
                };
                for (int[] edge : edgeCandidates) {
                    android.graphics.Rect candidate = collisionRectAt(
                            item, edge[0], edge[1], w, h);
                    boolean blocked = false;
                    for (android.graphics.Rect a : accepted) {
                        if (android.graphics.Rect.intersects(candidate, a)) {
                            blocked = true;
                            break;
                        }
                    }
                    if (!blocked) {
                        long distance = Math.abs((long) edge[0] - g[0])
                                + Math.abs((long) edge[1] - g[1]);
                        if (distance < bestDistance) {
                            bestDistance = distance;
                            bestX = edge[0];
                            bestY = edge[1];
                        }
                    }
                }
            }
            // Collision repair is strictly page-local. Cross-screen moves are handled only
            // by the explicit edge-drag interaction, never by layout or overlap repair.
            if (bestX < 0 && isHomeGridIcon(item)) {
                // If this page has no legal grid slot, keep the shortcut visible here.
                // Register its actual bounds so later items do not get placed on top of it.
                int[] retainedGeometry = itemGeometry(item);
                accepted.add(collisionRect(item, retainedGeometry));
                acceptedViews.add(item);
                continue;
            }

            // Last-resort protection for non-icon workspace items: if this page is crowded,
            // reduce visual scale in small steps to find a legal position.
            if (bestX < 0) {
                float originalScaleX = item.getScaleX();
                float originalScaleY = item.getScaleY();
                if (item.getTag() instanceof String && prefs != null) {
                    String scaleTag = (String) item.getTag();
                    prefs.edit()
                            .putFloat("workspace_base_scale_x_" + scaleTag, originalScaleX)
                            .putFloat("workspace_base_scale_y_" + scaleTag, originalScaleY)
                            .apply();
                }
                for (float factor = 0.90f; factor >= 0.05f && bestX < 0; factor -= 0.05f) {
                    item.setScaleX(originalScaleX * factor);
                    item.setScaleY(originalScaleY * factor);
                    g = itemGeometry(item);
                    w = Math.max(1, g[2]);
                    h = Math.max(1, g[3]);
                    maxX = Math.max(0, page.getWidth() - w);
                    maxY = Math.max(0, page.getHeight() - h);
                    int shrinkStep = Math.max(1, Math.min(LauncherUiUtils.dp(this, 6),
                            Math.max(1, Math.min(w, h) / 2)));
                    for (int y = 0; y <= maxY + shrinkStep && bestX < 0; y += shrinkStep) {
                        int candidateY = Math.min(maxY, y);
                        for (int x = 0; x <= maxX + shrinkStep; x += shrinkStep) {
                            int candidateX = Math.min(maxX, x);
                            android.graphics.Rect candidate = collisionRectAt(item, candidateX, candidateY, w, h);
                            boolean blocked = false;
                            for (android.graphics.Rect obstacle : accepted) {
                                if (android.graphics.Rect.intersects(candidate, obstacle)) {
                                    blocked = true;
                                    break;
                                }
                            }
                            if (!blocked) {
                                bestX = candidateX;
                                bestY = candidateY;
                                break;
                            }
                        }
                    }
                    if (bestX >= 0 && item.getTag() instanceof String && prefs != null) {
                        String scaleKey = (String) item.getTag();
                        prefs.edit()
                                .putFloat("workspace_scale_x_" + scaleKey, item.getScaleX())
                                .putFloat("workspace_scale_y_" + scaleKey, item.getScaleY())
                                .apply();
                    }
                }
                if (bestX < 0) {
                    item.setScaleX(originalScaleX);
                    item.setScaleY(originalScaleY);
                    g = itemGeometry(item);
                    w = Math.max(1, g[2]);
                    h = Math.max(1, g[3]);
                }
            }

            // Keep the item visible, but never deliberately register an overlapping
            // rectangle as accepted. The resolver has already tried exact placement,
            // moving a neighbour to the other page, and shrinking its visual bounds.
            if (bestX < 0) {
                if (isClockWorkspaceItem(item)) {
                    int pageIndex = workspacePageIndex(item);
                    float[] bounded = clampClockTranslation(item,
                            item.getTranslationX(), item.getTranslationY());
                    setClampedClockTranslationForPanel(item, pageIndex,
                            bounded[0], bounded[1], true);
                }
                // No legal local rectangle was found. Do not send the item to another
                // page as a side effect of rearranging this one. Protect the item's current
                // visible bounds from attracting additional items into the same region.
                int[] retainedGeometry = itemGeometry(item);
                accepted.add(collisionRect(item, retainedGeometry));
                acceptedViews.add(item);
                continue;
            }
            if (isClockWorkspaceItem(item)) {
                setClockVisualPosition(item, bestX, bestY, true);
            } else if (item.getLayoutParams() instanceof FrameLayout.LayoutParams) {
                FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) item.getLayoutParams();
                lp.leftMargin += bestX - g[0];
                lp.topMargin += bestY - g[1];
                item.setLayoutParams(lp);
            }
            int[] placedGeometry = itemGeometry(item);
            accepted.add(collisionRect(item, placedGeometry));
            acceptedViews.add(item);
            persistWorkspaceItemPosition(page, item, bestX, bestY, w, h);
        }
    }

    private String shortcutIdFromTile(View item) {
        CharSequence description = item == null ? null : item.getContentDescription();
        String value = description == null ? "" : description.toString();
        String prefix = "launcher_shortcut_id:";
        return value.startsWith(prefix) ? value.substring(prefix.length()) : "";
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
            int rows = homeGridRowsForHeight(page.getHeight(), cellHeight);
            int row = homeGridRowFromTop(y, rows, page.getHeight(), cellHeight);
            saveIconSlot(pkg, col, row, columns);
            prefs.edit().putInt(KEY_ICON_PAGE + pkg, pageIndex).apply();
        } else if ("launcher_contact_shortcut".equals(tag)) {
            int cellHeight = LauncherUiUtils.dp(this,
                    homeCellHeightDp(prefs == null ? 48 : prefs.getInt(KEY_ICON_SIZE, 48)));
            int rows = homeGridRowsForHeight(page.getHeight(), cellHeight);
            int[] slot = homeGridIconSlot(page, item, cellHeight, rows);
            if (slot != null) {
                String shortcutId = shortcutIdFromTile(item);
                prefs.edit()
                        .putInt(shortcutPrefKey(KEY_SHORTCUT_PAGE, shortcutId), pageIndex)
                        .putFloat(shortcutPrefKey(KEY_SHORTCUT_X, shortcutId),
                                slot[0] / (float) Math.max(1, 4 - 1))
                        .putFloat(shortcutPrefKey(KEY_SHORTCUT_Y, shortcutId),
                                slot[1] / (float) Math.max(1, rows - 1))
                        .putBoolean(shortcutPrefKey(KEY_SHORTCUT_GRID_POSITION, shortcutId), true)
                        .apply();
            }
        } else if ("launcher_clock_widget".equals(tag)) {
            prefs.edit().putFloat(clockPrefKey(KEY_CLOCK_WIDGET_X, pageIndex), item.getTranslationX())
                    .putFloat(clockPrefKey(KEY_CLOCK_WIDGET_Y, pageIndex), item.getTranslationY()).apply();
        }
    }

    /** Finalize temporary push-aside positions on drop, or roll them back on cancel. */
    private void finishLiveDisplacements(boolean commit, View movingView) {
        FrameLayout pendingPage = pendingLiveDisplacementPage;
        View pendingMover = pendingLiveDisplacementMover;
        cancelPendingLiveDisplacementUpdate();
        // A same-page drag may finish before the next display frame. Run its latest
        // collision pass before committing the displacement transaction so quick drops
        // behave exactly like drops after a rendered frame. Cancellation never flushes.
        if (commit && pendingPage != null && pendingMover == movingView
                && pendingMover.getParent() == pendingPage) {
            updateLiveDisplacements(pendingPage, pendingMover);
        }
        // Finalize the entire collision group together. A single dragged item may
        // displace any number of neighbours, so each neighbour's original and
        // destination rectangles must be handled independently and consistently.
        java.util.ArrayList<View> displacedViews =
                new java.util.ArrayList<View>(liveDisplacedItems.keySet());
        boolean committedLivePlan = commit && !displacedViews.isEmpty();
        // Commit the live plan as one transaction. A second resolver pass after release
        // used to reshuffle neighbors that had already reached their reserved positions.
        // The resolver is a no-op on a collision-free page and only reflows if the exact
        // release geometry still overlaps after the live displacement plan.

        for (View displaced : displacedViews) {
            int[] original = liveDisplacedItems.get(displaced);
            if (original == null || !(displaced.getParent() instanceof FrameLayout)) continue;

            FrameLayout page = (FrameLayout) displaced.getParent();
            int[] target = liveDisplacementTargets.get(displaced);
            // A successful drop commits the complete live displacement plan. Never
            // send a neighbor back to its old slot merely because the dropped item no
            // longer overlaps that old slot; that was the post-drop "second shuffle".
            // Cancellation still restores originals, and items without a reserved target
            // are not considered part of the committed plan.
            boolean keepDisplacement = commit && movingView != null
                    && movingView.getParent() == page && target != null;

            // Stop any in-flight animation before applying the final geometry;
            // otherwise its last frame could overwrite the committed position.
            cancelAppDisplacementAnimator(displaced, false);
            displaced.animate().cancel();
            if (keepDisplacement) {
                int[] destination = liveDisplacementTargets.get(displaced);
                if (isClockWorkspaceItem(displaced)) {
                    setClockVisualPosition(displaced, destination[0], destination[1], true);
                } else if (displaced.getLayoutParams() instanceof FrameLayout.LayoutParams) {
                    FrameLayout.LayoutParams lp =
                            (FrameLayout.LayoutParams) displaced.getLayoutParams();
                    float sx = Math.max(0.01f, displaced.getScaleX());
                    float sy = Math.max(0.01f, displaced.getScaleY());
                    lp.leftMargin = destination[0]
                            - Math.round(displaced.getPivotX() * (1f - sx));
                    lp.topMargin = destination[1]
                            - Math.round(displaced.getPivotY() * (1f - sy));
                    displaced.setTranslationX(0f);
                    displaced.setTranslationY(0f);
                    displaced.setLayoutParams(lp);
                }
                int[] finalGeometry = itemGeometry(displaced);
                // For grid-based app icons persist the exact reserved slot, not a
                // potentially stale pre-layout view origin from the just-updated margins.
                int persistX = isWorkspaceAppIcon(displaced)
                        ? destination[0] : finalGeometry[0];
                int persistY = isWorkspaceAppIcon(displaced)
                        ? destination[1] : finalGeometry[1];
                persistWorkspaceItemPosition(page, displaced,
                        persistX, persistY, finalGeometry[2], finalGeometry[3]);
            } else {
                if (isClockWorkspaceItem(displaced)) {
                    setClockVisualPosition(displaced, original[0], original[1], true);
                } else if (displaced.getLayoutParams() instanceof FrameLayout.LayoutParams) {
                    FrameLayout.LayoutParams lp =
                            (FrameLayout.LayoutParams) displaced.getLayoutParams();
                    float sx = Math.max(0.01f, displaced.getScaleX());
                    float sy = Math.max(0.01f, displaced.getScaleY());
                    lp.leftMargin = original[0]
                            - Math.round(displaced.getPivotX() * (1f - sx));
                    lp.topMargin = original[1]
                            - Math.round(displaced.getPivotY() * (1f - sy));
                    displaced.setTranslationX(0f);
                    displaced.setTranslationY(0f);
                    displaced.setLayoutParams(lp);
                }
            }
        }

        liveDisplacedItems.clear();
        liveDisplacementTargets.clear();
        if (commit || isClockWorkspaceItem(movingView)) {
            liveDragLastSafeGeometry.remove(movingView);
        }
        if (commit && !committedLivePlan && movingView != null
                && movingView.getParent() instanceof FrameLayout
                && isWorkspaceItem(movingView)) {
            FrameLayout page = (FrameLayout) movingView.getParent();
            resolveAllWorkspaceOverlaps(page, movingView);
        }
        refreshWorkspaceGrid();
    }

    /**
     * All workspace item types use the same rectangle-based collision resolver.
     * Keeping a widget-only resolver here caused it to disagree with live drag,
     * shortcut placement and clock displacement, which could re-overlap items
     * immediately after another resolver had moved them.
     */
    private void resolveWidgetIconCollisions(FrameLayout page, FrameLayout widgetContainer) {
        if (page == null || widgetContainer == null || widgetContainer.getParent() != page) return;
        resolveAllWorkspaceOverlaps(page, widgetContainer);
    }

    private void normalizeAppIconSettings() {
        if (prefs == null) return;
        int homeSize = prefs.getInt(KEY_ICON_SIZE, 48);
        int drawerSize = prefs.getInt(KEY_DRAWER_ICON_SIZE, 48);
        int homeColumns = Math.max(3, Math.min(6, prefs.getInt(KEY_COLUMNS, COLUMNS)));
        int drawerColumns = Math.max(3, Math.min(6, prefs.getInt(KEY_DRAWER_COLUMNS, COLUMNS)));
        SharedPreferences.Editor edit = prefs.edit();

        if (!prefs.getBoolean(KEY_FONT_SIZE_SLIDER_VALUES, false)) {
            int[] legacyFontSizes = {9, 9, 10, 12, 14};
            if (prefs.contains(KEY_HOME_FONT_SIZE)) {
                int old = prefs.getInt(KEY_HOME_FONT_SIZE, 1);
                edit.putInt(KEY_HOME_FONT_SIZE,
                        old >= 0 && old < legacyFontSizes.length ? legacyFontSizes[old] : 9);
            }
            if (prefs.contains(KEY_DRAWER_FONT_SIZE)) {
                int old = prefs.getInt(KEY_DRAWER_FONT_SIZE, 1);
                edit.putInt(KEY_DRAWER_FONT_SIZE,
                        old >= 0 && old < legacyFontSizes.length ? legacyFontSizes[old] : 9);
            }
            edit.putBoolean(KEY_FONT_SIZE_SLIDER_VALUES, true);
        }

        if (homeSize < 32 || homeSize > 74) {
            homeSize = Math.max(32, Math.min(74, homeSize));
            edit.putInt(KEY_ICON_SIZE, homeSize);
        }
        if (drawerSize < 32 || drawerSize > 74) {
            drawerSize = Math.max(32, Math.min(74, drawerSize));
            edit.putInt(KEY_DRAWER_ICON_SIZE, drawerSize);
        }
        int[] legacyFontSizes = {9, 9, 10, 12, 14};
        int homeFont = prefs.getInt(KEY_HOME_FONT_SIZE, 9);
        int drawerFont = prefs.getInt(KEY_DRAWER_FONT_SIZE, 9);
        if (!prefs.getBoolean(KEY_FONT_SIZE_SLIDER_VALUES, false)) {
            if (prefs.contains(KEY_HOME_FONT_SIZE)) {
                int old = prefs.getInt(KEY_HOME_FONT_SIZE, 1);
                homeFont = old >= 0 && old < legacyFontSizes.length ? legacyFontSizes[old] : 9;
            }
            if (prefs.contains(KEY_DRAWER_FONT_SIZE)) {
                int old = prefs.getInt(KEY_DRAWER_FONT_SIZE, 1);
                drawerFont = old >= 0 && old < legacyFontSizes.length ? legacyFontSizes[old] : 9;
            }
        }
        if (homeFont < 8 || homeFont > 20) edit.putInt(KEY_HOME_FONT_SIZE, Math.max(8, Math.min(20, homeFont)));
        if (drawerFont < 8 || drawerFont > 20) edit.putInt(KEY_DRAWER_FONT_SIZE, Math.max(8, Math.min(20, drawerFont)));
        // Six icons per row are available only for Small and Medium icon sizes.
        if (homeSize > 64 && homeColumns > 5) {
            homeColumns = 5;
            edit.putInt(KEY_COLUMNS, homeColumns);
            clearIconPositions();
        }
        if (drawerSize > 64 && drawerColumns > 5) {
            drawerColumns = 5;
            edit.putInt(KEY_DRAWER_COLUMNS, drawerColumns);
        }
        edit.apply();
    }

    private int getHomeColumns() {
        int columns = Math.max(3, Math.min(6,
                prefs == null ? COLUMNS : prefs.getInt(KEY_COLUMNS, COLUMNS)));
        int iconSize = prefs == null ? 48 : prefs.getInt(KEY_ICON_SIZE, 48);
        return iconSize > 64 ? Math.min(5, columns) : columns;
    }

    private int getDrawerColumns() {
        int columns = Math.max(3, Math.min(6,
                prefs == null ? COLUMNS : prefs.getInt(KEY_DRAWER_COLUMNS, COLUMNS)));
        int iconSize = prefs == null ? 48 : prefs.getInt(KEY_DRAWER_ICON_SIZE, 48);
        return iconSize > 64 ? Math.min(5, columns) : columns;
    }

    private int getHomeGridRows() {
        int pageHeight = pageViewport != null && pageViewport.getHeight() > LauncherUiUtils.dp(this, 100)
                ? pageViewport.getHeight()
                : Math.max(LauncherUiUtils.dp(this, 360),
                        getResources().getDisplayMetrics().heightPixels - LauncherUiUtils.dp(this, 260));
        int iconSize = prefs == null ? 48 : prefs.getInt(KEY_ICON_SIZE, 48);
        int cellHeight = LauncherUiUtils.dp(this, homeCellHeightDp(iconSize));
        return homeGridRowsForHeight(pageHeight, cellHeight);
    }

    private int pageCapacity() { return getHomeColumns() * getHomeGridRows(); }

    private void clearIconPositions() {
        SharedPreferences.Editor editor = prefs.edit();
        for (String key : prefs.getAll().keySet()) {
            if (key.startsWith(KEY_ICON_X) || key.startsWith(KEY_ICON_Y)) editor.remove(key);
        }
        editor.apply();
    }

    /**
     * Re-map app slots when the home column count changes without reordering the page.
     * Existing columns that still fit are kept exactly. Only icons that no longer fit
     * to the right (or below the new row capacity after an icon-size change) are moved,
     * each to the nearest unoccupied grid slot on its current page.
     */
    private void remapHomeIconSlotsForColumnChange(int oldColumns, int newColumns, int targetIconSize) {
        oldColumns = Math.max(3, Math.min(6, oldColumns));
        newColumns = Math.max(3, Math.min(6, newColumns));
        if (pagesStrip == null || oldColumns == newColumns) return;

        int oldCellHeight = LauncherUiUtils.dp(this,
                homeCellHeightDp(prefs.getInt(KEY_ICON_SIZE, 48)));
        int newCellHeight = LauncherUiUtils.dp(this, homeCellHeightDp(targetIconSize));
        for (int pageIndex = 0; pageIndex < Math.min(2, pagesStrip.getChildCount()); pageIndex++) {
            View pageView = pagesStrip.getChildAt(pageIndex);
            if (!(pageView instanceof FrameLayout)) continue;
            FrameLayout page = (FrameLayout) pageView;
            int width = page.getWidth() > 0 ? page.getWidth()
                    : (pageViewport != null && pageViewport.getWidth() > 0
                            ? pageViewport.getWidth()
                            : Math.max(1, getResources().getDisplayMetrics().widthPixels
                                    - LauncherUiUtils.dp(this, 40)));
            int height = page.getHeight() > 0 ? page.getHeight()
                    : (pageViewport != null && pageViewport.getHeight() > 0
                            ? pageViewport.getHeight()
                            : Math.max(LauncherUiUtils.dp(this, 360),
                                    getResources().getDisplayMetrics().heightPixels
                                            - LauncherUiUtils.dp(this, 260)));
            int oldCellWidth = Math.max(1, width / oldColumns);
            int oldRows = Math.max(1, height / Math.max(1, oldCellHeight));
            int rows = Math.max(1, height / Math.max(1, newCellHeight));

            final java.util.ArrayList<String> ordered = new java.util.ArrayList<String>();
            final java.util.HashMap<String, int[]> oldSlots =
                    new java.util.HashMap<String, int[]>();
            for (int i = 0; i < page.getChildCount(); i++) {
                View child = page.getChildAt(i);
                if (!(child.getTag() instanceof String)
                        || !((String) child.getTag()).startsWith("launcher_app:")
                        || !(child.getLayoutParams() instanceof FrameLayout.LayoutParams)) continue;
                String packageName = ((String) child.getTag()).substring("launcher_app:".length());
                FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) child.getLayoutParams();
                int col = Math.max(0, Math.round(lp.leftMargin / (float) oldCellWidth));
                int row = homeGridRowFromTop(
                        lp.topMargin, oldRows, height, oldCellHeight);
                ordered.add(packageName);
                oldSlots.put(packageName, new int[]{col, row});
            }
            java.util.Collections.sort(ordered, (a, b) -> {
                int[] pa = oldSlots.get(a);
                int[] pb = oldSlots.get(b);
                int rowOrder = Integer.compare(pa[1], pb[1]);
                return rowOrder != 0 ? rowOrder : Integer.compare(pa[0], pb[0]);
            });

            boolean[][] occupied = new boolean[rows][newColumns];
            java.util.HashMap<String, int[]> assigned =
                    new java.util.HashMap<String, int[]>();
            // Pass one: pin all slots that remain legal. This prevents a right-hand
            // overflow icon from stealing a position belonging to an existing icon.
            for (String packageName : ordered) {
                int[] old = oldSlots.get(packageName);
                if (old[0] >= newColumns || old[1] >= rows) continue;
                if (occupied[old[1]][old[0]]) continue;
                occupied[old[1]][old[0]] = true;
                assigned.put(packageName, new int[]{old[0], old[1]});
            }

            // Pass two: move only overflow/duplicate slots into the nearest free cell.
            for (String packageName : ordered) {
                if (assigned.containsKey(packageName)) continue;
                int[] old = oldSlots.get(packageName);
                int wantedCol = Math.max(0, Math.min(newColumns - 1, old[0]));
                int wantedRow = Math.max(0, Math.min(rows - 1, old[1]));
                int bestCol = -1, bestRow = -1, bestDistance = Integer.MAX_VALUE;
                for (int row = 0; row < rows; row++) {
                    for (int col = 0; col < newColumns; col++) {
                        if (occupied[row][col]) continue;
                        int distance = Math.abs(col - wantedCol) + Math.abs(row - wantedRow);
                        if (distance < bestDistance) {
                            bestDistance = distance;
                            bestCol = col;
                            bestRow = row;
                        }
                    }
                }
                if (bestCol >= 0) {
                    occupied[bestRow][bestCol] = true;
                    assigned.put(packageName, new int[]{bestCol, bestRow});
                }
            }

            SharedPreferences.Editor edit = prefs.edit();
            for (java.util.Map.Entry<String, int[]> entry : assigned.entrySet()) {
                String packageName = entry.getKey();
                int[] slot = entry.getValue();
                edit.putInt(KEY_ICON_PAGE + packageName, pageIndex)
                        .putFloat(KEY_ICON_X + packageName,
                                slot[0] / (float) Math.max(1, newColumns - 1))
                        .putFloat(KEY_ICON_Y + packageName,
                                slot[1] / (float) Math.max(1, rows - 1));
            }
            edit.apply();
        }
    }

    private void validateWorkspacePagesAfterLayout() {
        if (pagesStrip == null) return;
        for (int i = 0; i < Math.min(2, pagesStrip.getChildCount()); i++) {
            View pageView = pagesStrip.getChildAt(i);
            if (!(pageView instanceof FrameLayout)) continue;
            FrameLayout page = (FrameLayout) pageView;
            page.post(() -> {
                if (pagesStrip != null && page.getParent() == pagesStrip) {
                    resolveAllWorkspaceOverlaps(page, null);
                }
            });
        }
    }

    private void saveIconSlot(String packageName, int col, int row, int columns) {
        int slotHeight = LauncherUiUtils.dp(this,
                homeCellHeightDp(prefs.getInt(KEY_ICON_SIZE, 48)));
        int usableHeight = pageViewport != null && pageViewport.getHeight() > 0
                ? pageViewport.getHeight()
                : Math.max(slotHeight, getResources().getDisplayMetrics().heightPixels
                        - LauncherUiUtils.dp(this, 260));
        int rows = homeGridRowsForHeight(usableHeight, slotHeight);
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

    private int studioTint(int color, int alpha) {
        return Color.argb(Math.max(0, Math.min(255, alpha)),
                Color.red(color), Color.green(color), Color.blue(color));
    }

    private int studioDarken(int color) {
        return Color.rgb(Color.red(color) * 3 / 4,
                Color.green(color) * 3 / 4,
                Color.blue(color) * 3 / 4);
    }

    private GradientDrawable studioSolidRoundRect(int color, float radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(LauncherUiUtils.dp(this, radiusDp));
        return drawable;
    }

    private GradientDrawable studioControlBackground() {
        return LauncherUiUtils.roundRect(this,
                new int[]{Color.rgb(35, 43, 65), Color.rgb(22, 28, 45)},
                LauncherUiUtils.dp(this, 14));
    }

    private void addStudioAccentLine(LinearLayout container) {
        View line = new View(this);
        line.setBackground(studioSolidRoundRect(homeStudioAccentColor, 2));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1,
                LauncherUiUtils.dp(this, 3));
        params.bottomMargin = LauncherUiUtils.dp(this, 8);
        container.addView(line, params);
    }

    private String studioSectionDescription(String title) {
        String key = title.toLowerCase(Locale.ROOT);
        if (key.contains("home")) return "Grid, icon size, shortcuts and labels";
        if (key.contains("widget")) return "Add calendar, weather and other home-screen widgets";
        if (key.contains("clock")) return "Clock design, background and date typography";
        if (key.contains("animation")) return "Home page swipes and overall animation speed";
        if (key.contains("drawer")) return "App list layout, labels and opening motion";
        if (key.contains("appearance")) return "Wallpaper, colors and time format";
        if (key.contains("manage")) return "Add widgets or restore your layout";
        return "Launcher preferences";
    }

    private void addSection(LinearLayout panel, String title) {
        String key = title.toLowerCase(Locale.ROOT);
        String glyphText = "⚙";
        int accent = Color.rgb(146, 171, 239);
        if (key.contains("home")) { glyphText = "⌂"; accent = Color.rgb(98, 211, 255); }
        else if (key.contains("widget")) { glyphText = "▦"; accent = Color.rgb(112, 224, 190); }
        else if (key.contains("clock")) { glyphText = "◷"; accent = Color.rgb(255, 190, 95); }
        else if (key.contains("animation")) { glyphText = "↔"; accent = Color.rgb(198, 151, 255); }
        else if (key.contains("drawer")) { glyphText = "▦"; accent = Color.rgb(99, 229, 190); }
        else if (key.contains("appearance")) { glyphText = "◐"; accent = Color.rgb(255, 135, 184); }
        else if (key.contains("manage")) { glyphText = "☷"; accent = Color.rgb(255, 177, 111); }
        homeStudioAccentColor = accent;

        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        headerRow.setPadding(LauncherUiUtils.dp(this, 10), LauncherUiUtils.dp(this, 10),
                LauncherUiUtils.dp(this, 11), LauncherUiUtils.dp(this, 11));
        headerRow.setBackground(LauncherUiUtils.roundRect(this,
                new int[]{studioTint(accent, 44), Color.rgb(25, 31, 50)},
                LauncherUiUtils.dp(this, 14)));

        View rail = new View(this);
        rail.setBackground(LauncherUiUtils.roundRect(this,
                new int[]{accent}, LauncherUiUtils.dp(this, 3)));
        headerRow.addView(rail, new LinearLayout.LayoutParams(LauncherUiUtils.dp(this, 4),
                LauncherUiUtils.dp(this, 48)));

        TextView glyph = LauncherUiUtils.text(this, glyphText, 23, accent, true);
        glyph.setGravity(Gravity.CENTER);
        glyph.setBackground(LauncherUiUtils.roundRect(this,
                new int[]{studioTint(accent, 52), studioTint(accent, 22)},
                LauncherUiUtils.dp(this, 11)));
        LinearLayout.LayoutParams glyphParams = new LinearLayout.LayoutParams(
                LauncherUiUtils.dp(this, 42), LauncherUiUtils.dp(this, 42));
        glyphParams.leftMargin = LauncherUiUtils.dp(this, 9);
        glyphParams.rightMargin = LauncherUiUtils.dp(this, 10);
        headerRow.addView(glyph, glyphParams);

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        TextView header = LauncherUiUtils.text(this, title, 19, Color.WHITE, true);
        copy.addView(header, new LinearLayout.LayoutParams(-1, -2));
        TextView subtitle = LauncherUiUtils.text(this, studioSectionDescription(title),
                13, Color.rgb(190, 202, 226), false);
        subtitle.setPadding(0, LauncherUiUtils.dp(this, 3), 0, 0);
        copy.addView(subtitle, new LinearLayout.LayoutParams(-1, -2));
        headerRow.addView(copy, new LinearLayout.LayoutParams(0, -2, 1f));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = LauncherUiUtils.dp(this, 15);
        params.bottomMargin = LauncherUiUtils.dp(this, 9);
        panel.addView(headerRow, params);
    }

    private void addSettingRow(LinearLayout panel, String icon, String title, String detail, Runnable action) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(LauncherUiUtils.dp(this, 9), LauncherUiUtils.dp(this, 8),
                LauncherUiUtils.dp(this, 9), LauncherUiUtils.dp(this, 8));
        row.setBackground(studioControlBackground());
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, -2);
        rp.bottomMargin = LauncherUiUtils.dp(this, 7);

        View rail = new View(this);
        rail.setBackground(studioSolidRoundRect(homeStudioAccentColor, 2));
        row.addView(rail, new LinearLayout.LayoutParams(LauncherUiUtils.dp(this, 3),
                LauncherUiUtils.dp(this, 42)));

        TextView glyph = LauncherUiUtils.text(this, icon, 18, homeStudioAccentColor, true);
        glyph.setGravity(Gravity.CENTER);
        glyph.setBackground(LauncherUiUtils.roundRect(this,
                new int[]{studioTint(homeStudioAccentColor, 48), studioTint(homeStudioAccentColor, 22)},
                LauncherUiUtils.dp(this, 11)));
        LinearLayout.LayoutParams glyphParams = new LinearLayout.LayoutParams(
                LauncherUiUtils.dp(this, 36), LauncherUiUtils.dp(this, 36));
        glyphParams.leftMargin = LauncherUiUtils.dp(this, 9);
        glyphParams.rightMargin = LauncherUiUtils.dp(this, 9);
        row.addView(glyph, glyphParams);

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        TextView name = LauncherUiUtils.text(this, title, 15, Color.rgb(247, 249, 255), true);
        copy.addView(name);
        TextView sub = LauncherUiUtils.text(this, detail, 12, Color.rgb(183, 194, 217), false);
        sub.setPadding(0, LauncherUiUtils.dp(this, 3), 0, 0);
        sub.setMaxLines(2);
        copy.addView(sub);
        row.addView(copy, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView chevron = LauncherUiUtils.text(this, "›", 25, homeStudioAccentColor, false);
        chevron.setGravity(Gravity.CENTER);
        row.addView(chevron, new LinearLayout.LayoutParams(
                LauncherUiUtils.dp(this, 24), LauncherUiUtils.dp(this, 38)));
        row.setOnClickListener(v -> action.run());
        panel.addView(row, rp);
    }

    private void addChoiceRow(LinearLayout panel, String title, int selected, String[] choices, IntAction onSelect) {
        LinearLayout block = new LinearLayout(this);
        block.setOrientation(LinearLayout.VERTICAL);
        block.setPadding(LauncherUiUtils.dp(this, 10), LauncherUiUtils.dp(this, 8),
                LauncherUiUtils.dp(this, 10), LauncherUiUtils.dp(this, 9));
        block.setBackground(studioControlBackground());
        addStudioAccentLine(block);

        TextView heading = LauncherUiUtils.text(this, title, 15, Color.WHITE, true);
        heading.setPadding(LauncherUiUtils.dp(this, 1), 0, LauncherUiUtils.dp(this, 2),
                LauncherUiUtils.dp(this, 8));
        block.addView(heading, new LinearLayout.LayoutParams(-1, -2));

        // Use accent-colored chips with a clear active state; long option names remain scrollable.
        android.widget.HorizontalScrollView horizontal = new android.widget.HorizontalScrollView(this);
        horizontal.setHorizontalScrollBarEnabled(false);
        horizontal.setFillViewport(false);
        LinearLayout choicesRow = new LinearLayout(this);
        choicesRow.setGravity(Gravity.CENTER_VERTICAL);
        for (int i = 0; i < choices.length; i++) {
            final int value = i;
            boolean isSelected = i == selected;
            TextView chip = LauncherUiUtils.text(this, choices[i], 13,
                    isSelected ? Color.rgb(10, 15, 27) : Color.rgb(205, 214, 233), isSelected);
            chip.setGravity(Gravity.CENTER);
            chip.setSingleLine(true);
            chip.setPadding(LauncherUiUtils.dp(this, 13), LauncherUiUtils.dp(this, 10),
                    LauncherUiUtils.dp(this, 13), LauncherUiUtils.dp(this, 10));
            chip.setBackground(LauncherUiUtils.roundRect(this,
                    isSelected
                            ? new int[]{homeStudioAccentColor, studioDarken(homeStudioAccentColor)}
                            : new int[]{Color.rgb(39, 47, 68), Color.rgb(29, 36, 54)},
                    LauncherUiUtils.dp(this, 12)));
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-2,
                    LauncherUiUtils.dp(this, 42));
            cp.setMargins(LauncherUiUtils.dp(this, 2), 0, LauncherUiUtils.dp(this, 5), 0);
            choicesRow.addView(chip, cp);
            chip.setOnClickListener(v -> onSelect.onSelect(value));
        }
        horizontal.addView(choicesRow, new android.widget.HorizontalScrollView.LayoutParams(-2, -2));
        block.addView(horizontal, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout.LayoutParams blockParams = new LinearLayout.LayoutParams(-1, -2);
        blockParams.bottomMargin = LauncherUiUtils.dp(this, 7);
        panel.addView(block, blockParams);
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
            panel.setPadding(LauncherUiUtils.dp(this, 16), LauncherUiUtils.dp(this, 14),
                    LauncherUiUtils.dp(this, 16), LauncherUiUtils.dp(this, 14));
            panel.setBackground(LauncherUiUtils.roundRect(this,
                    new int[]{Color.rgb(28, 33, 57), Color.rgb(10, 14, 27)},
                    LauncherUiUtils.dp(this, 26)));
            TextView title = LauncherUiUtils.text(this, "Home Studio", 27,
                    Color.WHITE, true);
            title.setTypeface(android.graphics.Typeface.create("cursive",
                    android.graphics.Typeface.BOLD));
            title.addOnLayoutChangeListener((view, left, top, right, bottom,
                    oldLeft, oldTop, oldRight, oldBottom) -> {
                int textWidth = right - left;
                if (textWidth <= 0) return;
                title.getPaint().setShader(new android.graphics.LinearGradient(
                        0f, 0f, textWidth, 0f,
                        new int[]{Color.rgb(96, 231, 255), Color.rgb(143, 143, 255),
                                Color.rgb(255, 133, 204), Color.rgb(255, 208, 125)},
                        null, android.graphics.Shader.TileMode.CLAMP));
                title.invalidate();
            });
            title.setPadding(LauncherUiUtils.dp(this, 4), LauncherUiUtils.dp(this, 5),
                    LauncherUiUtils.dp(this, 4), LauncherUiUtils.dp(this, 4));
            panel.addView(title);
            TextView subtitle = LauncherUiUtils.text(this,
                    "Personalize your home screen and app drawer", 13,
                    Color.rgb(183, 197, 225), false);
            subtitle.setPadding(LauncherUiUtils.dp(this, 4), 0, LauncherUiUtils.dp(this, 4),
                    LauncherUiUtils.dp(this, 10));
            panel.addView(subtitle);

            TextView usageTip = LauncherUiUtils.text(this,
                    "TAP A ROW TO CHANGE IT  ·  SLIDE TO ADJUST",
                    11, Color.rgb(176, 222, 255), true);
            usageTip.setPadding(LauncherUiUtils.dp(this, 10), LauncherUiUtils.dp(this, 9),
                    LauncherUiUtils.dp(this, 10), LauncherUiUtils.dp(this, 9));
            usageTip.setBackground(LauncherUiUtils.roundRect(this,
                    new int[]{Color.rgb(23, 43, 63), Color.rgb(18, 29, 48)},
                    LauncherUiUtils.dp(this, 11)));
            LinearLayout.LayoutParams tipParams = new LinearLayout.LayoutParams(-1, -2);
            tipParams.bottomMargin = LauncherUiUtils.dp(this, 5);
            panel.addView(usageTip, tipParams);
            scroll = new android.widget.ScrollView(this);
            scroll.setTag("home_studio_scroll");
            content = new LinearLayout(this);
            content.setTag("home_studio_content");
            content.setOrientation(LinearLayout.VERTICAL);
            scroll.addView(content, new android.widget.ScrollView.LayoutParams(-1, -2));
        }

        addSection(content, "Home layout & icons");
        String[] homeColumnChoices = prefs.getInt(KEY_ICON_SIZE, 48) <= 64
                ? new String[]{"3", "4", "5", "6"} : new String[]{"3", "4", "5"};
        addChoiceRow(content, "Home columns", getHomeColumns() - 3, homeColumnChoices, value -> {
            int oldColumns = getHomeColumns();
            int newColumns = value + 3;
            remapHomeIconSlotsForColumnChange(oldColumns, newColumns,
                    prefs.getInt(KEY_ICON_SIZE, 48));
            prefs.edit().putInt(KEY_COLUMNS, newColumns).apply();
            currentPage = Math.max(0, Math.min(1, currentPage));
            rebuildPages();
            renderHomePages(false);
            showCustomizationMenu(dialog, scroll.getScrollY(), false);
        });
        addSliderRowOnStop(content, "Home icon size", 32, 74,
                Math.max(32, Math.min(74, prefs.getInt(KEY_ICON_SIZE, 48))), " dp", newSize -> {
            int oldColumns = getHomeColumns();
            boolean reduceColumns = newSize > 64 && oldColumns > 5;
            int newColumns = reduceColumns ? 5 : oldColumns;
            if (newColumns != oldColumns) remapHomeIconSlotsForColumnChange(oldColumns, newColumns, newSize);
            SharedPreferences.Editor edit = prefs.edit().putInt(KEY_ICON_SIZE, newSize);
            if (reduceColumns) edit.putInt(KEY_COLUMNS, newColumns);
            edit.apply();
            currentPage = Math.max(0, Math.min(1, currentPage));
            rebuildPages();
            renderHomePages(false);
            renderDock();
            rebuildContactShortcutViews();
            validateWorkspacePagesAfterLayout();
        });
        addSettingRow(content, "▣", "Rounded-square icon background",
                prefs.getBoolean("icon_rounded_square_background", false) ? "On" : "Off", () -> {
            prefs.edit().putBoolean("icon_rounded_square_background",
                    !prefs.getBoolean("icon_rounded_square_background", false)).apply();
            refreshWorkspaceIconAppearance();
            showCustomizationMenu(dialog, scroll.getScrollY(), false);
        });
        addChoiceRow(content, "App icon background color",
                Math.max(0, Math.min(2, prefs.getInt(KEY_ICON_BACKGROUND_COLOR, 0))),
                new String[]{"Black", "White", "Grey"}, value -> {
            prefs.edit().putInt(KEY_ICON_BACKGROUND_COLOR, value)
                    .putBoolean("icon_rounded_square_background", true).apply();
            refreshWorkspaceIconAppearance();
            showCustomizationMenu(dialog, scroll.getScrollY(), false);
        });
        addChoiceRow(content, "App icon shape", prefs.getInt("app_icon_shape", 0),
                new String[]{"Original", "Triangle", "Pear"}, value -> {
            prefs.edit().putInt("app_icon_shape", value).apply();
            refreshWorkspaceIconAppearance();
            showCustomizationMenu(dialog, scroll.getScrollY(), false);
        });
        addSliderRowOnStop(content, "Home app font size", 8, 20,
                homeFontSizeSp(), " sp", value -> {
            prefs.edit().putInt(KEY_HOME_FONT_SIZE, value).apply();
            renderHomePages(false);
            rebuildContactShortcutViews();
            validateWorkspacePagesAfterLayout();
        });
        int shortcutSizeMax = Math.max(24, Math.min(74, prefs.getInt(KEY_ICON_SIZE, 48)));
        addSliderRowOnStop(content, "Contact shortcut icon size", 24, shortcutSizeMax,
                Math.max(24, Math.min(shortcutSizeMax, getShortcutIconSizeDp())), " dp", value -> {
            prefs.edit().putInt(KEY_SHORTCUT_ICON_SIZE_DP, value).apply();
            applyShortcutIconSizeToWorkspace();
        });
        addSettingRow(content, "Aa", "App labels on Home", prefs.getBoolean(KEY_SHOW_HOME_LABELS, true) ? "Visible" : "Hidden", () -> {
            prefs.edit().putBoolean(KEY_SHOW_HOME_LABELS, !prefs.getBoolean(KEY_SHOW_HOME_LABELS, true)).apply();
            renderHomePages(false);
            rebuildContactShortcutViews();
            showCustomizationMenu(dialog, scroll.getScrollY(), false);
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

        addSection(content, "Widgets");
        addSettingRow(content, "▦", "Add widget",
                "Choose a clock, calendar, weather or other widget", () -> {
            dialog.dismiss();
            beginWidgetPicker(currentPage);
        });

        addSection(content, "Clocks");
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

        addSliderRow(content, "Date font size", 8, 24,
                Math.max(8, Math.min(24, prefs.getInt(KEY_CLOCK_DATE_SIZE, 15))), " sp", value -> {
            prefs.edit().putInt(KEY_CLOCK_DATE_SIZE, value).apply();
            refreshClockTypographyAndGeometry();
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
        addSliderRowWithHint(content, "Animation speed", 40, 200,
                LauncherMotion.speedPercent(this), "%",
                "Applies to timed launcher animations. Left = slower; 100% = normal; right = faster.",
                value -> prefs.edit().putInt(KEY_ANIMATION_SPEED, value).apply());

        addSection(content, "App drawer");
        addChoiceRow(content, "Drawer opening animation", getDrawerTransition(),
                new String[]{"Parallex", "3D Card Flip", "Depth Slide", "Rotating Carousel"}, value -> {
            prefs.edit().putInt(KEY_DRAWER_TRANSITION, value).apply();
            showCustomizationMenu(dialog, scroll.getScrollY(), false);
        });
        addSliderRow(content, "Drawer background opacity", 0, 100,
                prefs.getInt(KEY_DRAWER_OPACITY, 100), "%", value -> {
            prefs.edit().putInt(KEY_DRAWER_OPACITY, value).apply();
            if (drawerScreen != null) drawerScreen.setBackground(drawerBackground());
        });
        String[] drawerColumnChoices = prefs.getInt(KEY_DRAWER_ICON_SIZE, 48) <= 64
                ? new String[]{"3", "4", "5", "6"} : new String[]{"3", "4", "5"};
        addChoiceRow(content, "Drawer columns", getDrawerColumns() - 3, drawerColumnChoices, value -> {
            prefs.edit().putInt(KEY_DRAWER_COLUMNS, value + 3).apply();
            drawerGrid.setNumColumns(getDrawerColumns());
            drawerAdapter.notifyDataSetChanged();
            drawerGrid.setSelection(0);
            drawerGrid.requestLayout();
            showCustomizationMenu(dialog, scroll.getScrollY(), false);
        });
        addSliderRowOnStop(content, "Drawer icon size", 32, 74,
                Math.max(32, Math.min(74, prefs.getInt(KEY_DRAWER_ICON_SIZE, 48))), " dp", newSize -> {
            boolean reduceColumns = newSize > 64 && prefs.getInt(KEY_DRAWER_COLUMNS, COLUMNS) > 5;
            SharedPreferences.Editor edit = prefs.edit().putInt(KEY_DRAWER_ICON_SIZE, newSize);
            if (reduceColumns) edit.putInt(KEY_DRAWER_COLUMNS, 5);
            edit.apply();
            drawerGrid.setNumColumns(getDrawerColumns());
            drawerAdapter.notifyDataSetChanged();
            drawerGrid.setSelection(0);
            drawerGrid.requestLayout();
        });
        addSliderRowOnStop(content, "App drawer font size", 8, 20,
                Math.max(8, Math.min(20, prefs.getInt(KEY_DRAWER_FONT_SIZE, 9))), " sp", value -> {
            prefs.edit().putInt(KEY_DRAWER_FONT_SIZE, value).apply();
            drawerAdapter.notifyDataSetChanged();
            drawerGrid.requestLayout();
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

        TextView authorCredit = LauncherUiUtils.text(this, "made by debarghya Sannigrahi",
                11, Color.rgb(167, 180, 207), false);
        authorCredit.setTypeface(android.graphics.Typeface.create("cursive",
                android.graphics.Typeface.NORMAL));
        authorCredit.setGravity(Gravity.CENTER);
        authorCredit.setPadding(0, LauncherUiUtils.dp(this, 16), 0, LauncherUiUtils.dp(this, 7));
        content.addView(authorCredit, new LinearLayout.LayoutParams(-1, -2));

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
        if (dialog.getWindow() != null) dialog.getWindow().setLayout(Math.min(getResources().getDisplayMetrics().widthPixels - LauncherUiUtils.dp(this, 8), LauncherUiUtils.dp(this, 480)),
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
        // The widget-style resize grip has a 140dp minimum width. That can be
        // substantially less than 80% of a typography clock's natural width, so
        // the stored resize scale must be allowed below the Home Studio default.
        return Math.max(20, Math.min(600, prefs == null ? 100
                : prefs.getInt(clockPrefKey(KEY_CLOCK_LENGTH, activeClockPage), fallback)));
    }

    private int getClockBreadth() {
        int fallback = activeClockPage == 0 && prefs != null
                ? prefs.getInt(KEY_CLOCK_BREADTH, 100) : 100;
        return Math.max(80, Math.min(600, prefs == null ? 100
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
        // Use the actual workspace page width, not the physical display width.
        // The page may be narrower because of its container/insets; using display
        // width here creates a hidden horizontal gap and makes the left grip stop
        // before reaching the page edge.
        int pageWidth = clockPanel.getParent() instanceof View
                ? ((View) clockPanel.getParent()).getWidth() : 0;
        if (pageWidth <= 0) pageWidth = getResources().getDisplayMetrics().widthPixels;
        int available = Math.max(1, pageWidth);
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
        final LinearLayout targetPanel = clockPanel;
        final int targetPage = Math.max(0, Math.min(1, activeClockPage));
        final int geometryGeneration = ++clockLayoutGenerationByPage[targetPage];

        final int alignment = clockGravity();
        final float requestedLengthScale = getClockLength() / 100f;
        final float breadthScale = getClockBreadth() / 100f;
        final int screenWidth = clockPanel.getParent() instanceof View
                ? ((View) clockPanel.getParent()).getWidth()
                : getResources().getDisplayMetrics().widthPixels;
        final int availablePageWidth = Math.max(1, screenWidth);
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
        // Let the background reach the actual page edges. A display-width cap
        // leaves an unresizable strip when the workspace itself is narrower.
        final int availablePanelWidth = availablePageWidth;
        // Extra room for the deliberately separated LED hour / colon / minute
        // groups. Without reserving it, the max-width cap compresses the custom
        // glyph layout and the left grip appears to stop widening the background.
        int measuredDateWidth = 0;
        final int configuredDateSize = Math.max(8, Math.min(24, prefs.getInt(KEY_CLOCK_DATE_SIZE, 15)));
        final int configuredPmSize = Math.max(8, Math.min(24, prefs.getInt(KEY_CLOCK_PM_SIZE, 14)));
        if (dateText != null) {
            dateText.setTextSize(configuredDateSize);
            String measuredDate = dateText.getText().toString();
            // The AM/PM prefix is part of the date line in 12-hour mode. Measure
            // a complete representative line even during first layout, when the
            // TextView may still be empty, so the clock background cannot clip it.
            if (measuredDate.isEmpty()) {
                measuredDate = new SimpleDateFormat("EEEE, d MMMM", Locale.getDefault())
                        .format(new Date());
            }
            if (!DateFormat.is24HourFormat(this)
                    && !measuredDate.matches("(?i)^(AM|PM)(?:,|\\s).*")) {
                measuredDate = new SimpleDateFormat("a", Locale.getDefault()).format(new Date())
                        + "  " + measuredDate;
            }
            measuredDateWidth = (int) Math.ceil(dateText.getPaint().measureText(measuredDate));
            if (!DateFormat.is24HourFormat(this)) {
                String period = new SimpleDateFormat("a", Locale.getDefault()).format(new Date());
                float extraPmWidth = dateText.getPaint().measureText(period)
                        * (configuredPmSize / (float) configuredDateSize - 1f);
                measuredDateWidth = Math.max(1, (int) Math.ceil(measuredDateWidth + extraPmWidth));
            }
        }
        final int dateWidth = measuredDateWidth;
        // Measure the panel at its natural width, then scale the complete panel.
        // This keeps the background, digits, AM/PM marker, and date in one resize
        // operation instead of shrinking only the clock glyphs inside a fixed panel.
        final int clockWithMarkerWidth = (int) Math.ceil(naturalClockWidth)
                + markerWidth + LauncherUiUtils.dp(this, 1);
        final int contentWidth = Math.max(clockWithMarkerWidth, dateWidth)
                + LauncherUiUtils.dp(this, 4);
        final int ledGroupSpacing = sevenSegment ? LauncherUiUtils.dp(this, 30) : 0;
        final int naturalPanelWidth = Math.max(LauncherUiUtils.dp(this, 96),
                contentWidth + LauncherUiUtils.dp(this, 8) + ledGroupSpacing);
        // Treat the clock as a workspace widget: its visual rectangle may grow
        // across the full page, but never beyond the page's horizontal bounds.
        // The cap is based on the actual page width (not half-width/grid columns),
        // so normal typography clocks can expand all the way to either screen edge.
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
        targetPanel.post(() -> {
            // This callback belongs to the specific page panel measured above. Never
            // consult mutable clockPanel/activeClockPage here: they can point at the
            // other page by the time Android runs this posted layout task.
            if (geometryGeneration != clockLayoutGenerationByPage[targetPage]
                    || targetPanel != clockPanelsByPage[targetPage]
                    || targetPanel.getParent() == null) return;
            // While this exact panel is being resized, the resize transaction owns its
            // position and anchors the opposite edge.
            if (activeClockPage == targetPage && clockPanel == targetPanel
                    && clockWidgetEditing && clockWidgetDragging && clockResizeEdge != 0) {
                updateClockResizeHandles();
                return;
            }
            float[] bounded = clampClockTranslation(targetPanel,
                    targetPanel.getTranslationX(), targetPanel.getTranslationY());
            setClampedClockTranslationForPanel(
                    targetPanel, targetPage, bounded[0], bounded[1], true);
            if (activeClockPage == targetPage && clockPanel == targetPanel) {
                updateClockResizeHandles();
            }
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
        // Shift the digits and colon slightly left relative to the background.
        // For non-7-segment clocks with a one-digit hour (1–9), add a small
        // extra left shift to visually balance the reserved invisible leading cell.
        // Compensate for panel scaling so the offset remains stable at every length.
        boolean singleDigitHour = false;
        if (!sevenSegment && clockText.getText() != null) {
            String currentClock = clockText.getText().toString();
            int colon = currentClock.indexOf(':');
            if (colon > 0) {
                int hourDigits = 0;
                for (int i = 0; i < colon; i++) {
                    if (Character.digit(currentClock.charAt(i), 10) >= 0) hourDigits++;
                }
                singleDigitHour = hourDigits == 1;
            }
        }
        int horizontalOffset = LauncherUiUtils.dp(this, 5)
                + (singleDigitHour ? LauncherUiUtils.dp(this, 20) : 0);
        clockText.setTranslationX(sevenSegment
                ? -LauncherUiUtils.dp(this, 15) / Math.max(0.01f, lengthScale)
                : -horizontalOffset / Math.max(0.01f, lengthScale));
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
            dateText.setTextSize(Math.max(8, Math.min(24, prefs.getInt(KEY_CLOCK_DATE_SIZE, 15))));
            // Only counter-scale horizontally for the panel's width transform.
            // The panel no longer scales vertically, so leave Y at 1 to preserve
            // the date's glyph proportions as clock breadth changes.
            // Let the date scale with its panel. Counter-scaling here could let
            // long dates escape the background when the clock panel is width-capped.
            dateText.setScaleX(1f);
            dateText.setScaleY(1f);
            dateText.setTranslationX(0f);
        }

        if (amPmText != null) {
            amPmText.setTextColor(clockText.getCurrentTextColor());
            amPmText.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
            // Keep the marker's font size independent of workspace resizing.
            // 13.5sp is 50% taller than the previous 9sp setting.
            amPmText.setTextSize(13.5f);
            ViewGroup.LayoutParams markerRaw = amPmText.getLayoutParams();
            if (markerRaw instanceof FrameLayout.LayoutParams) {
                FrameLayout.LayoutParams markerParams = (FrameLayout.LayoutParams) markerRaw;
                // Reserve enough width for the enlarged bold "AM"/"PM" label;
                // its 13.5sp type remains 50% taller than the original 9sp marker.
                markerParams.width = LauncherUiUtils.dp(this, 32);
                markerParams.height = LauncherUiUtils.dp(this, 20);
                // Center the marker horizontally and vertically in the clock
                // group's colon gap. Reapplying this during every resize keeps it
                // inside the clock at every configured size and style.
                markerParams.gravity = Gravity.CENTER;
                markerParams.leftMargin = 0;
                markerParams.rightMargin = 0;
                markerParams.topMargin = 0;
                markerParams.bottomMargin = 0;
                // The seven-segment glyphs and their colon are shifted left by
                // 15dp; apply that exact same offset to AM/PM.
                amPmText.setTranslationX(sevenSegment ? -LauncherUiUtils.dp(this, 15) : 0f);
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

    private static final class TightDigitSpan extends android.text.style.ReplacementSpan {
        private final boolean moveDigitRight;
        private final float gap;

        TightDigitSpan(boolean moveDigitRight, float gap) {
            this.moveDigitRight = moveDigitRight;
            this.gap = Math.max(0f, gap);
        }

        @Override public int getSize(Paint paint, CharSequence text, int start, int end,
                Paint.FontMetricsInt fm) {
            return Math.max(1, Math.round(paint.measureText(text, start, end)
                    - (moveDigitRight ? 0f : gap)));
        }

        @Override public void draw(Canvas canvas, CharSequence text, int start, int end,
                float x, int top, int y, int bottom, Paint paint) {
            // For minutes, the first digit stays put and its reduced advance pulls
            // the second digit left. For hours, shift the first digit right while
            // preserving the second digit's original position.
            canvas.drawText(text, start, end, x + (moveDigitRight ? gap : 0f), y, paint);
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
                LauncherUiUtils.dp(this, 10), LauncherUiUtils.dp(this, 8));
        block.setBackground(studioControlBackground());
        addStudioAccentLine(block);
        LinearLayout.LayoutParams blockParams = new LinearLayout.LayoutParams(-1, -2);
        blockParams.bottomMargin = LauncherUiUtils.dp(this, 7);
        TextView label = LauncherUiUtils.text(this, title, 15, Color.WHITE, true);
        TextView valueLabel = LauncherUiUtils.text(this, current + suffix, 13,
                homeStudioAccentColor, true);
        LinearLayout heading = new LinearLayout(this);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        heading.addView(label, new LinearLayout.LayoutParams(0, -2, 1f));
        heading.addView(valueLabel);
        block.addView(heading);
        SeekBar seekBar = new SeekBar(this);
        seekBar.setMax(max - min);
        seekBar.setProgress(Math.max(0, Math.min(max - min, current - min)));
        android.content.res.ColorStateList tint =
                android.content.res.ColorStateList.valueOf(homeStudioAccentColor);
        seekBar.setProgressTintList(tint);
        seekBar.setThumbTintList(tint);
        seekBar.setProgressBackgroundTintList(
                android.content.res.ColorStateList.valueOf(Color.rgb(66, 74, 95)));
        block.addView(seekBar, new LinearLayout.LayoutParams(-1, LauncherUiUtils.dp(this, 42)));
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
        addSliderRowInternal(panel, title, min, max, current, suffix, action, false, null);
    }

    private void addSliderRowWithHint(LinearLayout panel, String title, int min, int max,
            int current, String suffix, String hint, IntSliderAction action) {
        addSliderRowInternal(panel, title, min, max, current, suffix, action, false, hint);
    }

    private void addSliderRowOnStop(LinearLayout panel, String title, int min, int max, int current,
            String suffix, IntSliderAction action) {
        addSliderRowInternal(panel, title, min, max, current, suffix, action, true, null);
    }

    private void addSliderRowInternal(LinearLayout panel, String title, int min, int max,
            int current, String suffix, IntSliderAction action, boolean applyOnStop) {
        addSliderRowInternal(panel, title, min, max, current, suffix, action, applyOnStop, null);
    }

    private void addSliderRowInternal(LinearLayout panel, String title, int min, int max,
            int current, String suffix, IntSliderAction action, boolean applyOnStop, String hint) {
        LinearLayout block = new LinearLayout(this);
        block.setOrientation(LinearLayout.VERTICAL);
        block.setPadding(LauncherUiUtils.dp(this, 10), LauncherUiUtils.dp(this, 8),
                LauncherUiUtils.dp(this, 10), LauncherUiUtils.dp(this, 8));
        block.setBackground(studioControlBackground());
        addStudioAccentLine(block);
        LinearLayout.LayoutParams blockParams = new LinearLayout.LayoutParams(-1, -2);
        blockParams.bottomMargin = LauncherUiUtils.dp(this, 7);

        TextView label = LauncherUiUtils.text(this, title, 15,
                Color.rgb(247, 249, 255), true);
        TextView valueLabel = LauncherUiUtils.text(this, current + suffix, 13,
                homeStudioAccentColor, true);
        LinearLayout heading = new LinearLayout(this);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        heading.addView(label, new LinearLayout.LayoutParams(0, -2, 1f));
        heading.addView(valueLabel);
        block.addView(heading);

        if (hint != null && !hint.isEmpty()) {
            TextView hintView = LauncherUiUtils.text(this, hint, 11.5f,
                    Color.rgb(184, 198, 223), false);
            hintView.setPadding(0, LauncherUiUtils.dp(this, 4), 0,
                    LauncherUiUtils.dp(this, 2));
            block.addView(hintView, new LinearLayout.LayoutParams(-1, -2));
        }

        SeekBar seekBar = new SeekBar(this);
        seekBar.setMax(max - min);
        seekBar.setProgress(Math.max(0, Math.min(max - min, current - min)));
        android.content.res.ColorStateList tint =
                android.content.res.ColorStateList.valueOf(homeStudioAccentColor);
        seekBar.setProgressTintList(tint);
        seekBar.setThumbTintList(tint);
        seekBar.setProgressBackgroundTintList(
                android.content.res.ColorStateList.valueOf(Color.rgb(66, 74, 95)));
        block.addView(seekBar, new LinearLayout.LayoutParams(-1, LauncherUiUtils.dp(this, 42)));
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                int value = min + progress;
                valueLabel.setText(value + suffix);
                if (fromUser && !applyOnStop) action.onChange(value);
            }
            @Override public void onStartTrackingTouch(SeekBar bar) {}
            @Override public void onStopTrackingTouch(SeekBar bar) {
                if (applyOnStop) action.onChange(min + bar.getProgress());
            }
        });
        panel.addView(block, blockParams);
    }

    private void applyBrandVisibility() {
        if (brandText != null) brandText.setVisibility(prefs.getBoolean(KEY_SHOW_BRAND, true) ? View.VISIBLE : View.GONE);
    }
}
