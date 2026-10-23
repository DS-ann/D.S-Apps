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
import android.os.Build;
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
    private static final android.view.ViewOutlineProvider HOME_ICON_ROUNDED_SQUARE_OUTLINE =
            new android.view.ViewOutlineProvider() {
                @Override public void getOutline(View view, android.graphics.Outline outline) {
                    int width = view.getWidth();
                    int height = view.getHeight();
                    if (width <= 0 || height <= 0) {
                        outline.setEmpty();
                        return;
                    }
                    float radius = Math.min(width, height) * 0.23f;
                    outline.setRoundRect(0, 0, width, height, radius);
                }
            };
    private static final android.view.ViewOutlineProvider HOME_ICON_ROUND_OUTLINE =
            new android.view.ViewOutlineProvider() {
                @Override public void getOutline(View view, android.graphics.Outline outline) {
                    int width = view.getWidth();
                    int height = view.getHeight();
                    if (width <= 0 || height <= 0) {
                        outline.setEmpty();
                        return;
                    }
                    outline.setOval(0, 0, width, height);
                }
            };

    private static final int HOST_ID = 1407;
    private static final String PREFS = "ds_launcher_prefs";
    private static final String KEY_HOME_APPS = "home_apps";
    private static final String KEY_APP_GROUP_IDS = "home_app_group_ids";
    private static final String APP_GROUP_TAG_PREFIX = "launcher_app_group:";
    private static final String KEY_COLUMNS = "home_columns";
    // Stable reference size used to anchor home-grid rows while the visible icon size changes.
    private static final String KEY_HOME_GRID_ANCHOR_ICON_SIZE = "home_grid_anchor_icon_size_v1";
    private static final String KEY_HOME_GRID_ANCHOR_CELL_HEIGHT_DP =
            "home_grid_anchor_cell_height_dp_v1";
    private static final String KEY_SHOW_DATE = "show_date";
    private static final String KEY_ICON_SIZE = "icon_size";
    private static final String KEY_ICON_BACKGROUND_COLOR = "icon_background_color";
    private static final String KEY_ICON_BACKGROUND_COLOR_POSITION = "icon_background_color_position";
    private static final String KEY_ICON_SHAPE_MIGRATION = "app_icon_shape_migration_v2";
    private static final String KEY_ICON_ARTWORK_THEME = "icon_artwork_theme";
    private static final String KEY_ICON_FIT_BACKGROUND = "icon_fit_inside_background";
    private static final String KEY_THEME_SHORTCUT_ARTWORK = "theme_widget_shortcut_artwork";
    private static final String KEY_DRAWER_COLUMNS = "drawer_columns";
    private static final String KEY_DRAWER_ICON_SIZE = "drawer_icon_size";
    private static final String KEY_HOME_FONT_SIZE = "home_font_size";
    private static final String KEY_DRAWER_FONT_SIZE = "drawer_font_size";
    private static final String KEY_FONT_SIZE_SLIDER_VALUES = "font_size_slider_values_v1";
    private static final String KEY_CLOCK_STYLE = "clock_style";
    private static final String KEY_CLOCK_DIGIT_GRADIENT = "animated_clock_digit_colours";
    private static final String KEY_CLOCK_LIVE_GRADIENT = "live_clock_colour_transition";
    private static final String KEY_CLOCK_ICON_ARTWORK = "clock_icon_artwork";
    private static final String KEY_CLOCK_DATE_SIZE = "clock_date_size_sp";
    private static final String KEY_CLOCK_PM_SIZE = "clock_pm_size_sp";
    private static final String KEY_CLOCK_DATE_FILL_COLOR_POSITION = "clock_date_fill_color_position";
    private static final String KEY_CLOCK_DATE_FONT = "clock_date_font_family";
    private static final String KEY_CLOCK_PM_FONT = "clock_ampm_font_family";
    private static final String KEY_HOME_LABEL_FONT = "home_label_font_family";
    private static final String KEY_HOME_LABEL_COLOR = "home_label_color_position";
    private static final String KEY_PAGE_ROTATION = "page_rotation_degrees";
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
    private static final String KEY_AUTO_REMOVED_CLOCK_PAGE_PREFIX = "resize_auto_removed_clock_page_";
    private static final String KEY_RESIZE_AUTO_SCALE_X_PREFIX = "workspace_resize_auto_scale_x_";
    private static final String KEY_RESIZE_AUTO_SCALE_Y_PREFIX = "workspace_resize_auto_scale_y_";
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
    private static final int REQUEST_HOME_ROLE = 7307;
    private int pendingWidgetId = -1;
    private AppWidgetProviderInfo pendingWidgetInfo;
    private Intent pendingShortcutCallIntent;
    private int pendingWidgetTargetPage = 0;
    private static final int COLUMNS = 4;
    private static final int HOME_ROWS = 5;
    private static final int CLOCK_DIGIT_SILVER = Color.rgb(196, 200, 209);
    private static final int[] CLOCK_DATE_GRADIENT_COLORS = new int[] {
            Color.rgb(96, 231, 255), Color.rgb(143, 143, 255),
            Color.rgb(255, 133, 204), Color.rgb(255, 208, 125)
    };
    private static final int[] CLOCK_DIGIT_GRADIENT_COLORS = new int[] {
            Color.rgb(77, 163, 255), Color.rgb(71, 240, 232),
            Color.rgb(169, 139, 255), Color.rgb(255, 120, 191),
            Color.rgb(217, 247, 255)
    };
    private static final int[] CLOCK_NOTHING_MATCH_DIGIT_GRADIENT_COLORS = new int[] {
            Color.BLACK, Color.rgb(68, 71, 78), Color.rgb(166, 170, 178),
            Color.rgb(82, 85, 92), Color.BLACK
    };
    private static final int[] CLOCK_WHITE_BORDER_COLORS = new int[] {
            Color.WHITE, Color.WHITE
    };
    private static final long ICON_EDGE_SWITCH_DELAY_MS = 200L;
    private static final int ICON_EDGE_SWITCH_ZONE_DP = 24;
    private interface IntAction { void onSelect(int value); }
    private final int BG = Color.rgb(10, 13, 24);
    private final Handler handler = new Handler();
    private boolean clockUpdatesActive = false;
    // Avoid reopening the system Home-role prompt repeatedly during a single Activity lifetime.
    private boolean homeRoleRequestAttempted = false;
    // Registered on Android 13+ because targetSdk 37 no longer relies on legacy Back dispatch.
    private Object backInvokedCallback;
    private final java.util.Map<TextView, Integer> appliedClockStyles = new java.util.WeakHashMap<>();
    private PackageManager packageManager;
    private SharedPreferences prefs;
    private final SharedPreferences.OnSharedPreferenceChangeListener drawerCachePreferenceListener =
            (sharedPreferences, key) -> handleDrawerCachePreferenceChanged(key);
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
    // Carries a failed live placement from the last MOVE frame to ACTION_UP.
    private final java.util.IdentityHashMap<View, Boolean> liveDragRejectedMoves =
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
    // Package/style refreshes may rebuild geometry but must preserve clock placement.
    private final boolean[] preserveClockPositionAfterPackageRefresh = new boolean[2];
    private final float[] packageRefreshClockTranslationX = new float[2];
    private final float[] packageRefreshClockTranslationY = new float[2];
    private int activeClockPage = 0;
    // Each clock panel has its own drawable; remember the last applied five-minute colour phase.
    private int lastClockPanelColorPhase = -1;
    private float clockColorPhase;
    private long clockColorLastFrameUptime;
    private long clockGradientResumeAtUptime;
    private boolean pageFlipOverflowEnabled;
    private boolean pageFlipSavedHomeClipChildren;
    private boolean pageFlipSavedHomeClipToPadding;
    private boolean pageFlipSavedViewportClipChildren;
    private boolean pageFlipSavedViewportClipToPadding;
    private boolean pageFlipSavedStripClipChildren;
    private boolean pageFlipSavedStripClipToPadding;
    private int pageFlipSavedViewportLayerType = View.LAYER_TYPE_HARDWARE;
    private int pageFlipSavedStripLayerType = View.LAYER_TYPE_HARDWARE;
    private final Runnable clockColorFrame = new Runnable() {
        @Override public void run() {
            if (!clockUpdatesActive || prefs == null
                    || !prefs.getBoolean(KEY_CLOCK_LIVE_GRADIENT, false)
                    || !prefs.getBoolean(KEY_CLOCK_DIGIT_GRADIENT, false)) {
                clockColorLastFrameUptime = 0L;
                return;
            }
            // Stop scheduling work during the finger-driven turn. Resume with the
            // current colour phase when the page has settled.
            if (pageTransitionBusy) {
                clockColorLastFrameUptime = 0L;
                return;
            }
            // The moving home/drawer surfaces are cached as hardware layers. Avoid
            // invalidating the home layer for clock-gradient changes during this motion.
            if (drawerSwipeVisualActive || drawerCloseSwipeClaimed
                    || LauncherScreenTransitions.isDrawerTransitionRunning(
                            homeScreen, drawerScreen, getDrawerTransition())) {
                clockColorLastFrameUptime = 0L;
                handler.postDelayed(this, 250L);
                return;
            }
            long now = android.os.SystemClock.uptimeMillis();
            if (clockColorLastFrameUptime > 0L) {
                long elapsed = Math.max(0L, Math.min(500L, now - clockColorLastFrameUptime));
                clockColorPhase = (clockColorPhase + elapsed / 10000f) % 1f;
            }
            clockColorLastFrameUptime = now;
            applyClockColorEffectsToActivePage(clockColorPhase);
            handler.postDelayed(this, 125L);
        }
    };
    private static final class ClockTextGradientState {
        int gradientSpanPx;
        int[] colors;
        android.graphics.LinearGradient shader;
        final android.graphics.Matrix matrix = new android.graphics.Matrix();
    }

    private static final class ClockTextOutlineSpan extends android.text.style.ReplacementSpan {
        private final int fillColor;
        private final int outlineColor;
        private final float borderPx;
        private final float innerBorderPx;
        private final float outerBorderPx;
        private final boolean preserveFill;
        private final boolean extraBold;

        ClockTextOutlineSpan(int fillColor, int outlineColor, float borderPx,
                float innerBorderPx) {
            this(fillColor, outlineColor, borderPx, innerBorderPx, 0f, false, true);
        }

        ClockTextOutlineSpan(int fillColor, int outlineColor, float borderPx,
                float innerBorderPx, float outerBorderPx) {
            this(fillColor, outlineColor, borderPx, innerBorderPx,
                    outerBorderPx, false, true);
        }

        // Keep the current TextView color/shader for ordinary and gradient modes.
        ClockTextOutlineSpan(int outlineColor, float borderPx) {
            this(outlineColor, borderPx, 0f);
        }

        // Preserve the existing inner contour color while keeping the current fill gradient.
        ClockTextOutlineSpan(int outlineColor, float borderPx, float innerBorderPx) {
            this(0, outlineColor, borderPx, innerBorderPx, 0f, true, true);
        }

        private ClockTextOutlineSpan(int fillColor, int outlineColor, float borderPx,
                float innerBorderPx, float outerBorderPx,
                boolean preserveFill, boolean extraBold) {
            this.fillColor = fillColor;
            this.outlineColor = outlineColor;
            this.borderPx = Math.max(0.1f, borderPx);
            this.innerBorderPx = Math.max(0f, innerBorderPx);
            this.outerBorderPx = Math.max(0f, outerBorderPx);
            this.preserveFill = preserveFill;
            this.extraBold = extraBold;
        }

        @Override public int getSize(Paint paint, CharSequence text, int start, int end,
                android.graphics.Paint.FontMetricsInt fontMetrics) {
            boolean oldFakeBold = paint.isFakeBoldText();
            try {
                if (extraBold) paint.setFakeBoldText(true);
                if (fontMetrics != null) paint.getFontMetricsInt(fontMetrics);
                return Math.round(paint.measureText(text, start, end));
            } finally {
                paint.setFakeBoldText(oldFakeBold);
            }
        }

        @Override public void draw(Canvas canvas, CharSequence text, int start, int end,
                float x, int top, int y, int bottom, Paint paint) {
            // Outline strokes may extend beyond the font glyph and span advance.
            // Clip the whole renderer to this span's allocated bounds so clock borders
            // cannot spill into neighbouring app/widget layout.
            int spanCanvasSave = canvas.save();
            float spanWidth = Math.max(1f, paint.measureText(text, start, end));
            canvas.clipRect(x, top, x + spanWidth, bottom);
            int oldColor = paint.getColor();
            Paint.Style oldStyle = paint.getStyle();
            float oldStrokeWidth = paint.getStrokeWidth();
            Paint.Join oldJoin = paint.getStrokeJoin();
            Paint.Cap oldCap = paint.getStrokeCap();
            boolean oldFakeBold = paint.isFakeBoldText();
            android.graphics.Shader oldShader = paint.getShader();
            android.graphics.Xfermode oldXfermode = paint.getXfermode();
            try {
                paint.setShader(null);
                paint.setXfermode(null);
                paint.setFakeBoldText(extraBold || oldFakeBold);

                // Artwork-matched mode already has a theme-specific outline. Draw an
                // underlay beyond it to add the requested outer rim.
                if (outerBorderPx > 0f) {
                    paint.setColor(Color.BLACK);
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth((borderPx + outerBorderPx) * 2f);
                    paint.setStrokeJoin(Paint.Join.ROUND);
                    paint.setStrokeCap(Paint.Cap.ROUND);
                    canvas.drawText(text, start, end, x, y, paint);
                }

                paint.setColor(outlineColor);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(borderPx * 2f);
                paint.setStrokeJoin(Paint.Join.ROUND);
                paint.setStrokeCap(Paint.Cap.ROUND);
                canvas.drawText(text, start, end, x, y, paint);

                if (preserveFill) {
                    // Restore the active solid/gradient fill rather than overriding it.
                    paint.setColor(oldColor);
                    paint.setStyle(oldStyle);
                    paint.setStrokeWidth(oldStrokeWidth);
                    paint.setStrokeJoin(oldJoin);
                    paint.setStrokeCap(oldCap);
                    paint.setShader(oldShader);
                    paint.setXfermode(oldXfermode);
                    paint.setFakeBoldText(extraBold || oldFakeBold);
                } else {
                    paint.setShader(null);
                    paint.setXfermode(null);
                    paint.setStyle(Paint.Style.FILL);
                    paint.setStrokeWidth(0f);
                    paint.setColor(fillColor);
                    paint.setFakeBoldText(extraBold || oldFakeBold);
                }
                canvas.drawText(text, start, end, x, y, paint);

                if (innerBorderPx > 0f) {
                    // Mask the fine black stroke to the glyph fill, retaining the
                    // existing matched-artwork outline while adding a true inner contour.
                    float textWidth = Math.max(1f, paint.measureText(text, start, end));
                    float strokeMargin = (borderPx + outerBorderPx) * 2f;
                    int innerLayer = canvas.saveLayer(x - strokeMargin, top - strokeMargin,
                            x + textWidth + strokeMargin, bottom + strokeMargin, null);
                    try {
                        paint.setShader(null);
                        paint.setXfermode(null);
                        paint.setFakeBoldText(extraBold || oldFakeBold);
                        paint.setColor(Color.BLACK);
                        paint.setStyle(Paint.Style.STROKE);
                        paint.setStrokeWidth(innerBorderPx * 2f);
                        paint.setStrokeJoin(Paint.Join.ROUND);
                        paint.setStrokeCap(Paint.Cap.ROUND);
                        canvas.drawText(text, start, end, x, y, paint);

                        paint.setStyle(Paint.Style.FILL);
                        paint.setStrokeWidth(0f);
                        paint.setColor(Color.WHITE);
                        paint.setXfermode(new android.graphics.PorterDuffXfermode(
                                android.graphics.PorterDuff.Mode.DST_IN));
                        canvas.drawText(text, start, end, x, y, paint);
                    } finally {
                        canvas.restoreToCount(innerLayer);
                    }
                }
            } finally {
                paint.setColor(oldColor);
                paint.setStyle(oldStyle);
                paint.setStrokeWidth(oldStrokeWidth);
                paint.setStrokeJoin(oldJoin);
                paint.setStrokeCap(oldCap);
                paint.setFakeBoldText(oldFakeBold);
                paint.setShader(oldShader);
                paint.setXfermode(oldXfermode);
                canvas.restoreToCount(spanCanvasSave);
            }
        }
    }
    private final java.util.WeakHashMap<TextView, ClockTextGradientState> clockDateGradients =
            new java.util.WeakHashMap<>();
    private final java.util.WeakHashMap<TextView, ClockTextGradientState> clockDigitGradients =
            new java.util.WeakHashMap<>();
    private final java.util.WeakHashMap<TextView, Boolean> dateGradientModeApplied =
            new java.util.WeakHashMap<>();
    private final java.util.WeakHashMap<TextView, String> dateTextAppearanceApplied =
            new java.util.WeakHashMap<>();
    private final java.util.WeakHashMap<TextView, String> clockDateGeometrySignatures =
            new java.util.WeakHashMap<>();
    private final java.util.WeakHashMap<TextView, Integer> clockPositionStyleSignatures =
            new java.util.WeakHashMap<>();
    private final java.util.WeakHashMap<TextView, Boolean> clockDigitInnerBorderModeApplied =
            new java.util.WeakHashMap<>();
    // Forces a fresh text/span buffer after renderer and package refreshes.
    private boolean forceClockTextRefresh = false;
    private View clockWidgetResizeHandle;
    private final View[] clockEdgeHandles = new View[4];
    private boolean clockWidgetEditing = false;
    private boolean clockWidgetDragging = false;
    private boolean clockWidgetMoving = false;
    private boolean clockGeometryRejectedAtLastMove = false;
    private float clockWidgetDownRawX, clockWidgetDownRawY;
    private float clockWidgetStartX, clockWidgetStartY;
    private int clockWidgetStartLength, clockWidgetStartBreadth;
    private int clockWidgetStartScreenX, clockWidgetStartScreenY;
    private int clockWidgetStartPanelWidth, clockWidgetStartPanelHeight;
    private int clockWidgetStartRowHeight, clockWidgetStartNonRowHeight;
    private float clockWidgetStartTextScaleY = 1f;
    private int clockWidgetLiveResizeLength, clockWidgetLiveResizeBreadth;
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
    // Pin the clock panel/page for the entire touch stream. Clock refresh and layout
    // callbacks temporarily activate the other page's clock, so gesture code must not
    // follow that mutable global reference between DOWN, MOVE, and UP.
    private LinearLayout clockGesturePanel;
    private int clockGesturePage = -1;
    // Locks the whole touch sequence against launcher swipes if it began while
    // clock move/edit mode was already active.
    private boolean clockGestureLockedAtDown = false;
    private TextView pageIndicator;
    private LauncherAppAdapter drawerAdapter;
    private Runnable refreshDrawerAppList;
    private int drawerArtworkReadyChecks = 0;
    private final Runnable drawerArtworkReadyCheck = new Runnable() {
        @Override public void run() {
            if (drawerAdapter == null || drawerScreen == null || isFinishing()) return;
            if (drawerAdapter.isArtworkPrewarmComplete()) {
                drawerArtworkReadyChecks = 0;
                return;
            }
            // Poll asynchronously while idle on Home, without creating a screenshot of the grid.
            if (++drawerArtworkReadyChecks <= 300) handler.postDelayed(this, 100L);
            else drawerArtworkReadyChecks = 0;
        }
    };
    private List<ResolveInfo> allApps = new ArrayList<>();
    private List<ResolveInfo> drawerApps = new ArrayList<>();
    // The same labels used to sort allApps are reused by drawer cells and search.
    private final java.util.Map<String, String> preloadedAppLabels = new java.util.HashMap<>();
    private final List<String> homePackages = new ArrayList<>();
    private final List<List<String>> pages = new ArrayList<>();
    private int currentPage = 0;
    private float gestureDownX, gestureDownY;
    private float drawerSwipeProgress = 0f;
    private float drawerSwipeStartProgress = 0f;
    private float drawerSwipeAnchorY = 0f;
    private boolean drawerSwipeVisualActive = false;
    private boolean gestureStartedOnAppCell = false;
    private boolean gestureStartedOnWidget = false;
    private boolean gestureStartedOnContactShortcut = false;
    private boolean gestureStartedOnResizeHandle = false;
    // A first pull at the drawer's top arms closing; the next downward pull closes.
    private boolean pageSwipeActive = false;
    // Remains true from the first finger-driven frame until the settle animator ends.
    private boolean pageTransitionBusy = false;
    private boolean pageTransitionBusyAtDown = false;
    // Input remains locked to the transition gesture, except that the incoming page
    // can receive child touches after at least 80% of its entrance has completed.
    private boolean pageTransitionGestureLocked = false;
    private boolean pageTransitionTouchPassthrough = false;
    // A reverse swipe may interrupt a page settle after 80% of its current direction.
    private boolean pageTransitionReverseGestureCandidate = false;
    private float pageTransitionTouchDownX = 0f;
    private float pageTransitionTouchDownY = 0f;
    private int pageTransitionGestureDirection = 0;
    private int pageTransitionGestureReverseTargetPage = -1;
    private int pageTransitionTargetPage = -1;
    private boolean pageSwipeChildCancelled = false;
    private boolean verticalSwipeClaimed = false;
    private boolean gestureStartedOnDrawerGrid = false;
    private boolean drawerCloseSwipeClaimed = false;
    private float wheelSwipeProgress = 0f;
    private int wheelSwipeDirection = 0;
    private android.animation.ValueAnimator pageSettleAnimator;
    private float longPressDownX, longPressDownY;
    private boolean longPressTriggered = false;
    private boolean iconDragArmed = false;
    private boolean iconDragging = false;
    private boolean iconMovedTooEarly = false;
    private boolean workspaceIconReparenting = false;
    private boolean suppressResizeOutsideTouchStream = false;
    // Ignore an entire touch stream if it starts while the drawer is settling.
    private boolean drawerTransitionGestureLocked = false;
    // At 80% transition completion, reserve a qualifying reverse swipe to reverse the
    // settling animation while preserving normal taps/scrolls on the incoming drawer grid.
    private boolean drawerTransitionReverseGestureCandidate = false;
    private boolean drawerTransitionReverseGesturePassthrough = false;
    private boolean drawerTransitionReverseGestureClaimed = false;
    private boolean pageTransitionReverseGestureClaimed = false;
    private boolean drawerTransitionWasOpening = false;
    private boolean drawerTransitionReverseSwipeAllowed = true;
    private float drawerTransitionTouchDownX = 0f;
    private float drawerTransitionTouchDownY = 0f;
    // Once the drawer is at least 80% open, keep a new grid touch owned by the app list
    // for the full gesture so it can scroll without being reinterpreted as navigation.
    private boolean drawerTransitionGridTouchActive = false;
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
    private String activeAppGroupMoveModeId;
    // Highlight app groups as workspace targets while the move grid is active.
    // A group's own move-ready state remains highlighted after the grid is hidden.
    private boolean workspaceMoveModeHighlightActive;
    private boolean gestureStartedOnAppGroupMoveMode;
    private String activeAppGroupMemberMoveModeId;
    private String activeAppGroupMemberMoveModePackage;
    private View activeAppGroupMemberMoveModeView;
    private boolean appGroupCreationPending;
    private boolean appGroupRenderDeferred;
    private final java.util.WeakHashMap<FrameLayout, Boolean> shortcutPageLayoutWatchers =
            new java.util.WeakHashMap<FrameLayout, Boolean>();
    // Defers generic overlap validation until resize-specific recovery completes.
    private boolean homeIconResizeRepairPending;
    private int resizeRepairCollisionCount;
    private int resizeRepairClockBackgroundCollisionCount;
    private int resizeRepairSpecialShrinkCount;
    private int resizeRepairNeighbourIconShrinkCount;
    private int resizeRepairMovedCount;
    private int resizeRepairRemovedWidgetCount;
    private int resizeRepairRemovedClockCount;
    private int resizeRepairUnresolvedCollisionCount;
    private int resizeRepairGroupRepositionCount;
    private long lastAppGroupCreationUptime;
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
    // Coalesce high-frequency touch MOVE events to one drawer pose update per display frame.
    private float drawerSwipePendingProgress;
    private int drawerSwipePendingEffect;
    private boolean drawerSwipeFrameScheduled = false;
    private final Runnable drawerSwipeFrame = () -> {
        drawerSwipeFrameScheduled = false;
        applyDrawerSwipeFrame();
    };
    private boolean drawerAtTopOnGestureDown = true;
    private boolean packageReceiverRegistered = false;
    private final BroadcastReceiver packageChangesReceiver = new BroadcastReceiver() {
        @Override public void onReceive(android.content.Context context, Intent intent) {
            if (intent == null) return;
            String action = intent.getAction();
            if (Intent.ACTION_PACKAGE_REPLACED.equals(action)) {
                String replacedPackage = intent.getData() == null ? null
                        : intent.getData().getSchemeSpecificPart();
                invalidatePackageArtwork(replacedPackage, true);
                // Reset bounded retries after every replacement. Provider metadata and
                // AppWidgetHostView state can be briefly unavailable during app updates.
                handler.removeCallbacks(widgetRestoreRetry);
                widgetRestoreRetryScheduled = false;
                widgetRestoreRetryCount = 0;
                handler.removeCallbacks(refreshClockGeometryAfterPackageChange);
                handler.postDelayed(refreshClockGeometryAfterPackageChange, 350L);
                // Rebuild ResolveInfo records after an update; launch activities/labels can
                // change even when the package name itself stays the same.
                scheduleAppDrawerRefresh();
                return;
            }
            boolean packageAdded = Intent.ACTION_PACKAGE_ADDED.equals(action);
            boolean packageRemoved = Intent.ACTION_PACKAGE_REMOVED.equals(action);
            boolean packageChanged = Intent.ACTION_PACKAGE_CHANGED.equals(action);
            if (!packageAdded && !packageRemoved && !packageChanged) return;

            // Ignore the remove/add pair emitted during an update; PACKAGE_REPLACED performs
            // the single authoritative refresh after the new package is fully available.
            if ((packageAdded || packageRemoved)
                    && intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) return;

            String changedPackage = intent.getData() == null ? null
                    : intent.getData().getSchemeSpecificPart();
            // A fresh install or full uninstall invalidates only this package. PACKAGE_CHANGED
            // also covers component enable/disable, so preserve its cache for quick re-enable.
            if (packageAdded || packageRemoved) invalidatePackageArtwork(changedPackage, true);

            // Debounce broadcasts so multi-component state changes cause only one PackageManager scan.
            scheduleAppDrawerRefresh();
        }
    };
    private void invalidatePackageArtwork(String packageName, boolean deletePersistentFiles) {
        if (packageName == null || packageName.isEmpty()) return;
        LauncherIconDrawable.invalidatePackageArtworkCache(packageName, deletePersistentFiles);
        if (drawerAdapter != null) drawerAdapter.invalidatePackageArtworkIdentity(packageName);
    }

    private final Runnable refreshAppsAfterPackageChange = new Runnable() {
        @Override public void run() { loadApps(); }
    };
    private void scheduleAppDrawerRefresh() {
        handler.removeCallbacks(refreshAppsAfterPackageChange);
        handler.postDelayed(refreshAppsAfterPackageChange, 800L);
    }
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
                // Re-apply the renderer before refreshing text. If styles are reapplied
                // after updateClock(), identical two-digit time text can bypass rebuilding
                // and leave a stale/missing replacement-span buffer on the clock view.
                for (int page = 0; page < clockPanelsByPage.length; page++) {
                    if (clockPanelsByPage[page] == null) continue;
                    activateClockForPage(page);
                    applyClockStyle();
                }
                forceClockTextRefresh = true;
                updateClock();
                for (int page = 0; page < clockPanelsByPage.length; page++) {
                    LinearLayout targetPanel = clockPanelsByPage[page];
                    if (targetPanel == null) continue;
                    // Package updates rebuild text/layout, not the widget's workspace position.
                    packageRefreshClockTranslationX[page] = targetPanel.getTranslationX();
                    packageRefreshClockTranslationY[page] = targetPanel.getTranslationY();
                    preserveClockPositionAfterPackageRefresh[page] = true;
                    activateClockForPage(page);
                    applyClockPositionAndSize();
                    applyClockPanelColor(false);
                }
                activateClockForPage(restorePage);
                updateClockResizeHandles();
                // Also recover after process-preserving updates. Recompute both clock
                // renderers after Android has completed the new text/layout pass.
                for (int page = 0; page < clockPanelsByPage.length; page++) {
                    scheduleClockRendererPostLayoutRecovery(page);
                }
            }
            // A provider update can temporarily invalidate an AppWidgetHostView. Re-run
            // restoration after the package manager has had time to publish new metadata.
            restoreWidgetsOnPages();
        }
    };

    /**
     * Rebuild the standard clock's text buffer only after its parent has completed layout.
     * ReplacementSpan advances and the colon's center are measured by TextView; a package
     * update can leave that view holding the previous Layout even after getText() is current.
     * A fresh SpannableStringBuilder plus a second-frame alignment pass drops those stale
     * measurements without changing the selected font, colours, spacing or saved geometry.
     */
    /**
     * Rebuild clock text only after TextView has had a layout pass. On Android 6,
     * refreshing a two-digit clock before the first Layout exists can leave its
     * ReplacementSpan advances stale; the digits then remain misplaced or invisible
     * until a time-change broadcast happens to rebuild the view.
     *
     * Standard clocks use the same digit and centered-colon spans for one- and
     * two-digit hours. Seven-segment rendering is not rewritten by this recovery.
     */
    private void scheduleClockRendererPostLayoutRecovery(final int page) {
        if (page < 0 || page >= clockPanelsByPage.length) return;
        final LinearLayout targetPanel = clockPanelsByPage[page];
        final TextView targetClock = clockTextsByPage[page];
        if (targetPanel == null || targetClock == null) return;

        targetPanel.postOnAnimation(() -> {
            if (isFinishing() || targetPanel.getParent() == null
                    || clockPanelsByPage[page] != targetPanel
                    || clockTextsByPage[page] != targetClock) return;

            int restorePage = Math.max(0, Math.min(1, activeClockPage));
            activateClockForPage(page);
            int style = prefs == null ? 0 : prefs.getInt(KEY_CLOCK_STYLE, 0);
            boolean standardClockStyle = !(targetClock instanceof SevenSegmentClockTextView
                    && style == 5);

            targetClock.setVisibility(View.VISIBLE);
            targetClock.setAlpha(1f);
            targetClock.requestLayout();
            targetClock.invalidate();

            // Wait one frame so the initial/retained TextView Layout is not reused
            // while reconstructing the spans. Rebuilding the Android 6 text before
            // this point is the startup path that could make a two-digit clock vanish.
            targetClock.postOnAnimation(() -> {
                if (isFinishing() || targetPanel.getParent() == null
                        || clockPanelsByPage[page] != targetPanel
                        || clockTextsByPage[page] != targetClock) return;

                int refreshRestorePage = Math.max(0, Math.min(1, activeClockPage));
                activateClockForPage(page);
                if (standardClockStyle) {
                    if (Build.VERSION.SDK_INT == Build.VERSION_CODES.M) {
                        // updateClock() rebuilds the same per-digit and centered-colon
                        // spans used for every standard hour, then updates both pages.
                        forceClockTextRefresh = true;
                        updateClock();
                        activateClockForPage(page);
                    } else {
                        CharSequence currentText = targetClock.getText();
                        if (currentText != null) {
                            targetClock.setText(
                                    new android.text.SpannableStringBuilder(currentText),
                                    TextView.BufferType.SPANNABLE);
                        }
                    }
                }

                targetClock.setVisibility(View.VISIBLE);
                targetClock.setAlpha(1f);
                targetClock.requestLayout();
                targetClock.invalidate();

                // Let the refreshed spans participate in a completed layout before
                // centering the visible glyphs within the unchanged clock background.
                targetClock.postOnAnimation(() -> {
                    if (isFinishing() || targetPanel.getParent() == null
                            || clockPanelsByPage[page] != targetPanel
                            || clockTextsByPage[page] != targetClock) return;
                    int finalRestorePage = Math.max(0, Math.min(1, activeClockPage));
                    activateClockForPage(page);
                    targetClock.setVisibility(View.VISIBLE);
                    targetClock.setAlpha(1f);
                    syncClockColonVerticalCenter();
                    updateClockDigitHorizontalOffset();
                    applyClockColorEffectsToActivePage(clockColorPhase);
                    targetClock.requestLayout();
                    targetClock.postInvalidateOnAnimation();
                    activateClockForPage(finalRestorePage);
                });
                activateClockForPage(refreshRestorePage);
            });
            activateClockForPage(restorePage);
        });
    }

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
        if (Build.VERSION.SDK_INT >= 33) {
            backInvokedCallback = Api33BackDispatcher.register(this);
        }
        packageManager = getPackageManager();
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        normalizeAppIconSettings();
        LauncherIconDrawable.configurePersistentArtworkCache(this,
                prefs.getInt(KEY_ICON_ARTWORK_THEME, LauncherIconDrawable.ARTWORK_ORIGINAL));
        normalizeClockVisualModePreferences();
        prefs.registerOnSharedPreferenceChangeListener(drawerCachePreferenceListener);
        IntentFilter packageFilter = new IntentFilter();
        packageFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        packageFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        packageFilter.addAction(Intent.ACTION_PACKAGE_CHANGED);
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
                    @Override public boolean canPlaceWorkspaceItem(FrameLayout page, View item,
                            int left, int top, int width, int height) {
                        return MainActivity.this.canPlaceWorkspaceItem(page, item, left, top, width, height);
                    }
                    @Override public void updateLiveDisplacements(FrameLayout page, View movingView) {
                        MainActivity.this.updateLiveDisplacements(page, movingView);
                    }
                    @Override public void scheduleLiveDisplacementUpdate(FrameLayout page, View movingView) {
                        MainActivity.this.scheduleLiveDisplacementUpdate(page, movingView);
                    }
                    @Override public boolean finishLiveDisplacements(boolean commit, View movingView) {
                        return MainActivity.this.finishLiveDisplacements(commit, movingView);
                    }
                    @Override public void setWorkspaceGridVisible(boolean visible, View movingView) {
                        MainActivity.this.setWorkspaceGridVisible(visible, movingView);
                    }
                    @Override public void refreshWorkspaceGrid() {
                        MainActivity.this.refreshWorkspaceGrid();
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
        if (Build.VERSION.SDK_INT >= 30) {
            getWindow().setDecorFitsSystemWindows(false);
        }
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
        // Once a reverse swipe takes ownership of the stream, consume its remaining
        // events. Otherwise ACTION_UP falls through to stale gesture coordinates and
        // can start a second navigation that overrides the reverse settle.
        if (drawerTransitionReverseGestureClaimed) {
            if (action == MotionEvent.ACTION_DOWN) {
                drawerTransitionReverseGestureClaimed = false;
            } else {
                if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                    drawerTransitionReverseGestureClaimed = false;
                }
                return true;
            }
        }
        if (pageTransitionReverseGestureClaimed) {
            if (action == MotionEvent.ACTION_DOWN) {
                pageTransitionReverseGestureClaimed = false;
            } else {
                if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                    pageTransitionReverseGestureClaimed = false;
                }
                return true;
            }
        }
        if (action == MotionEvent.ACTION_DOWN) {
            clockGesturePanel = null;
            clockGesturePage = -1;
        } else if (clockGesturePanel != null && clockGesturePage >= 0) {
            // Keep all movement and resize updates on the exact panel that received DOWN,
            // even if a clock refresh temporarily swaps activeClockPage between callbacks.
            activateClockForPage(clockGesturePage);
        }
        if (action == MotionEvent.ACTION_DOWN && !drawerOpen && drawerScreen != null
                && drawerScreen.getVisibility() == View.VISIBLE
                && !LauncherScreenTransitions.isDrawerTransitionRunning(
                        homeScreen, drawerScreen, getDrawerTransition())) {
            // The drawer is not the selected surface and no transition owns it. Hide
            // any stale visible copy before the topmost view can steal a home-icon tap.
            showHomeImmediately();
        }
        // During the last 20% of a drawer transition, detect an intentional reverse
        // swipe before handing the event to the grid. Ordinary taps and list scrolling
        // continue through to the child; a qualifying reverse cancels that child stream
        // and settles toward the opposite surface from the current visual progress.
        if (drawerTransitionReverseGestureCandidate) {
            float dx = event.getX() - drawerTransitionTouchDownX;
            float dy = event.getY() - drawerTransitionTouchDownY;
            boolean terminal = action == MotionEvent.ACTION_UP
                    || action == MotionEvent.ACTION_CANCEL;
            boolean verticalSwipe = Math.abs(dy) > LauncherUiUtils.dp(this, 12)
                    && Math.abs(dy) > Math.abs(dx) * 1.15f;
            boolean reversedDirection = drawerTransitionWasOpening ? dy > 0f : dy < 0f;
            if (action == MotionEvent.ACTION_MOVE && drawerTransitionReverseSwipeAllowed
                    && verticalSwipe && reversedDirection) {
                if (drawerTransitionReverseGesturePassthrough) {
                    MotionEvent cancel = MotionEvent.obtain(event);
                    cancel.setAction(MotionEvent.ACTION_CANCEL);
                    super.dispatchTouchEvent(cancel);
                    cancel.recycle();
                }
                boolean wasOpening = drawerTransitionWasOpening;
                drawerTransitionReverseGestureClaimed = true;
                drawerTransitionReverseGestureCandidate = false;
                drawerTransitionReverseGesturePassthrough = false;
                drawerTransitionGridTouchActive = false;
                float progress = LauncherScreenTransitions.getDrawerTransitionProgress(
                        homeScreen, drawerScreen, getDrawerTransition(), drawerOpen);
                if (wasOpening) {
                    LauncherScreenTransitions.finishDrawerSwipeToHome(this, homeScreen, drawerScreen,
                            () -> { drawerOpen = false; scheduleDrawerArtworkPrewarmAfterHomeTransition(); }, getDrawerTransition(), progress);
                } else {

                    LauncherScreenTransitions.finishDrawerSwipeToDrawer(this, homeScreen, drawerScreen,
                            this::markDrawerOpenAndRefreshArtwork, getDrawerTransition(), progress);
                }
                return true;
            }
            if (terminal) {
                boolean passthrough = drawerTransitionReverseGesturePassthrough;
                drawerTransitionReverseGestureCandidate = false;
                drawerTransitionReverseGesturePassthrough = false;
                if (passthrough) {
                    boolean handled = super.dispatchTouchEvent(event);
                    LauncherScreenTransitions.resumeDrawerTransitionIfPaused();
                    return handled;
                }
                LauncherScreenTransitions.resumeDrawerTransitionIfPaused();
                return true;
            }
            return drawerTransitionReverseGesturePassthrough
                    ? super.dispatchTouchEvent(event) : true;
        }

        // A drawer-list touch that starts at 80% open stays with the grid for the
        // whole stream; this enables scrolling/taps without giving the gesture to
        // workspace or drawer-navigation recognizers.
        if (drawerTransitionGridTouchActive) {
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                drawerTransitionGridTouchActive = false;
            }
            return super.dispatchTouchEvent(event);
        }
        if (pageTransitionReverseGestureCandidate) {
            float dx = event.getX() - pageTransitionTouchDownX;
            float dy = event.getY() - pageTransitionTouchDownY;
            boolean terminal = action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL;
            boolean horizontalSwipe = Math.abs(dx) > LauncherUiUtils.dp(this, 12)
                    && Math.abs(dx) > Math.abs(dy) * 0.75f;
            boolean reversedDirection = dx * pageTransitionGestureDirection > 0f;
            if (action == MotionEvent.ACTION_MOVE && horizontalSwipe && reversedDirection) {
                if (pageTransitionTouchPassthrough) {
                    MotionEvent cancel = MotionEvent.obtain(event);
                    cancel.setAction(MotionEvent.ACTION_CANCEL);
                    super.dispatchTouchEvent(cancel);
                    cancel.recycle();
                }
                int reverseTarget = pageTransitionGestureReverseTargetPage;
                pageTransitionTouchPassthrough = false;
                pageTransitionReverseGestureClaimed = true;
                pageTransitionReverseGestureCandidate = false;
                int pageWidth = pageViewport != null && pageViewport.getWidth() > 0
                        ? pageViewport.getWidth()
                        : Math.max(1, getResources().getDisplayMetrics().widthPixels
                                - LauncherUiUtils.dp(this, 40));
                // finishPageSwipe replaces the paused settle animator without allowing
                // its cancellation callback to overwrite the new target state.
                finishPageSwipe(reverseTarget, pageWidth);
                return true;
            }
            if (terminal) {
                boolean passthrough = pageTransitionTouchPassthrough;
                pageTransitionTouchPassthrough = false;
                pageTransitionReverseGestureCandidate = false;
                if (passthrough) {
                    boolean resumePageSettle = pageSettleAnimator != null
                            && pageSettleAnimator.isStarted() && pageSettleAnimator.isPaused();
                    boolean handled = super.dispatchTouchEvent(event);
                    if (resumePageSettle && pageSettleAnimator != null
                            && pageSettleAnimator.isStarted() && pageSettleAnimator.isPaused()) {
                        pageSettleAnimator.resume();
                    }
                    return handled;
                }
                if (pageSettleAnimator != null && pageSettleAnimator.isStarted()
                        && pageSettleAnimator.isPaused()) {
                    pageSettleAnimator.resume();
                }
                return true;
            }
            return pageTransitionTouchPassthrough ? super.dispatchTouchEvent(event) : true;
        }
        if (pageTransitionTouchPassthrough) {
            boolean terminal = action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL;
            if (terminal) pageTransitionTouchPassthrough = false;
            // Keep the incoming page's hit geometry stable during the tap/child gesture.
            // Resume the settle only after the child has received its UP/CANCEL.
            boolean resumePageSettle = terminal && pageSettleAnimator != null
                    && pageSettleAnimator.isStarted() && pageSettleAnimator.isPaused();
            boolean handled = super.dispatchTouchEvent(event);
            if (resumePageSettle && pageSettleAnimator != null
                    && pageSettleAnimator.isStarted() && pageSettleAnimator.isPaused()) {
                pageSettleAnimator.resume();
            }
            return handled;
        }
        // If a page is settling, no fresh gesture may start another navigation. At
        // 80% of the incoming page's entrance, child input becomes live; below that,
        // ignore the entire touch sequence rather than letting it take over mid-frame.
        if (pageTransitionGestureLocked) {
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                pageTransitionGestureLocked = false;
            }
            return true;
        }
        if (drawerTransitionGestureLocked) {
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                drawerTransitionGestureLocked = false;
            }
            return true;
        }
        if (action == MotionEvent.ACTION_DOWN
                && LauncherScreenTransitions.isDrawerTransitionRunning(
                        homeScreen, drawerScreen, getDrawerTransition())) {
            float transitionProgress = LauncherScreenTransitions.getDrawerTransitionProgress(
                    homeScreen, drawerScreen, getDrawerTransition(), drawerOpen);
            boolean opening = LauncherScreenTransitions.isDrawerTransitionOpening();
            // The progress value represents how open the drawer is. Therefore 80% of an
            // opening transition is progress >= .80, while 80% of a closing transition
            // is progress <= .20.
            boolean transitionPastEightyPercent = opening
                    ? transitionProgress >= 0.80f : transitionProgress <= 0.20f;
            boolean touchOnDrawerGrid = drawerScreen != null
                    && drawerScreen.getVisibility() == View.VISIBLE
                    && touchStartedOnDrawerGrid(event.getRawX(), event.getRawY());
            boolean drawerGridReady = opening && transitionPastEightyPercent && touchOnDrawerGrid;
            if (transitionPastEightyPercent) {
                drawerTransitionReverseGestureCandidate = true;
                drawerTransitionReverseGesturePassthrough = drawerGridReady;
                drawerTransitionWasOpening = opening;
                drawerTransitionTouchDownX = event.getX();
                drawerTransitionTouchDownY = event.getY();
                // Preserve normal list scrolling when the grid is not at its top edge;
                // otherwise a downward pull during opening is the reverse/close gesture.
                drawerTransitionReverseSwipeAllowed = !opening || !touchOnDrawerGrid
                        || isDrawerAtTop();
                LauncherScreenTransitions.pauseDrawerTransition(
                        homeScreen, drawerScreen, getDrawerTransition());
                if (drawerGridReady) return super.dispatchTouchEvent(event);
                return true;
            }
            drawerTransitionGestureLocked = true;
            return true;
        }
        if (action == MotionEvent.ACTION_DOWN && pageTransitionBusy) {
            boolean settlingBackToCurrentPage = pageTransitionTargetPage == currentPage;
            float transitionCompletion = settlingBackToCurrentPage
                    ? 1f - wheelSwipeProgress : wheelSwipeProgress;
            boolean transitionPastEightyPercent = pageTransitionTargetPage >= 0
                    && wheelSwipeDirection != 0 && transitionCompletion >= 0.80f;
            if (transitionPastEightyPercent) {
                pageTransitionReverseGestureCandidate = true;
                pageTransitionTouchDownX = event.getX();
                pageTransitionTouchDownY = event.getY();
                // When returning to the starting page, reverse direction is opposite to
                // the original page-change direction; otherwise it is the current direction.
                pageTransitionGestureDirection = settlingBackToCurrentPage
                        ? -wheelSwipeDirection : wheelSwipeDirection;
                pageTransitionGestureReverseTargetPage = settlingBackToCurrentPage
                        ? currentPage + wheelSwipeDirection : currentPage;
                boolean incomingPageReady = pagesStrip != null
                        && touchStartedOnTransitionPage(
                                pagesStrip.getChildAt(pageTransitionTargetPage),
                                event.getRawX(), event.getRawY());
                pageTransitionTouchPassthrough = incomingPageReady;
                // Hold the exact visual pose while deciding whether this touch reverses
                // the transition; a tap/scroll resumes the existing settle on release.
                if (pageSettleAnimator != null && pageSettleAnimator.isStarted()
                        && !pageSettleAnimator.isPaused()) {
                    pageSettleAnimator.pause();
                }
                if (incomingPageReady) return super.dispatchTouchEvent(event);
                return true;
            }
            pageTransitionGestureLocked = true;
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
            // Ensure the visible page is the active clock before resolving hit targets.
            // This is essential because edit/resize gestures are consumed at activity level
            // before the clock panel's own touch listener can activate its captured page.
            if (!drawerOpen) {
                activateClockForPage(Math.max(0, Math.min(1, currentPage)));
            }
            clockGestureLockedAtDown = clockWidgetMoving || clockWidgetEditing;
            gestureDownX = event.getX();
            gestureDownY = event.getY();
            longPressDownX = event.getX();
            longPressDownY = event.getY();
            longPressTriggered = false;
            pageSwipeActive = false;
            pageTransitionBusyAtDown = pageTransitionBusy;
            pageSwipeChildCancelled = false;
            verticalSwipeClaimed = false;
            drawerCloseSwipeClaimed = false;
            drawerSwipeStartProgress = LauncherScreenTransitions.getDrawerTransitionProgress(
                    homeScreen, drawerScreen, getDrawerTransition(), drawerOpen);
            drawerSwipeAnchorY = event.getY();
            drawerSwipeProgress = drawerSwipeStartProgress;
            drawerSwipeVisualActive = false;
            boolean touchOnVisibleDrawerGrid = drawerScreen != null
                    && drawerScreen.getVisibility() == View.VISIBLE
                    && touchStartedOnDrawerGrid(event.getRawX(), event.getRawY());
            gestureStartedOnDrawerGrid = touchOnVisibleDrawerGrid && drawerOpen;
            drawerAtTopOnGestureDown = !drawerOpen || isDrawerAtTop();
            clearAppGroupMoveModeIfTouchOutside(event.getRawX(), event.getRawY());
            String touchedGroupId = touchStartedOnAppGroup(event.getRawX(), event.getRawY());
            gestureStartedOnAppGroupMoveMode = touchedGroupId != null
                    && touchedGroupId.equals(activeAppGroupMoveModeId);
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
            if (gestureStartedOnClock && clockPanel != null) {
                clockGesturePanel = clockPanel;
                clockGesturePage = activeClockPage;
                clockGeometryRejectedAtLastMove = false;
            }
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
                    clockWidgetStartRowHeight = clockRow != null && clockRow.getHeight() > 0
                            ? clockRow.getHeight() : LauncherUiUtils.dp(this, 114);
                    // The date line contains the AM/PM prefix and may need more vertical
                    // space than an earlier TextView measure reserved (especially after a
                    // font/span refresh). Include its measured text and margins explicitly
                    // so vertical resizing never assigns that space to the digit row.
                    int nonRowChromeHeight = clockNonRowChromeHeight(clockPanel, dateText);
                    clockWidgetStartPanelHeight = Math.max(clockPanel.getHeight(),
                            clockWidgetStartRowHeight + nonRowChromeHeight);
                    clockWidgetStartNonRowHeight = Math.max(nonRowChromeHeight,
                            Math.max(0, clockWidgetStartPanelHeight - clockWidgetStartRowHeight));
                    clockWidgetStartPanelScaleX = clockPanel.getScaleX();
                    clockWidgetStartTextScaleY = clockText == null ? 1f : clockText.getScaleY();
                    clockWidgetLiveResizeLength = clockWidgetStartLength;
                    clockWidgetLiveResizeBreadth = clockWidgetStartBreadth;
                    int[] startGeometry = itemGeometry(clockPanel);
                    clockWidgetStartVisualX = startGeometry[0];
                    clockWidgetStartVisualY = startGeometry[1];
                    clockWidgetStartVisualWidth = startGeometry[2];
                    clockWidgetStartVisualHeight = Math.max(startGeometry[3],
                            clockWidgetStartPanelHeight);
                    clockResizeGeneration++;
                    clockWidgetStartX = clockPanel.getTranslationX();
                    clockWidgetStartY = clockPanel.getTranslationY();
                    setWorkspaceGridVisible(true, clockPanel);
                    return true;
                }
            // Once edit mode is open, dragging the clock body moves the widget while
            // dragging one of the four edge grips resizes it. Start the body gesture here
            // at the activity level so scaled text/date overhangs work even when the finger
            // is outside the panel's untransformed hit rectangle.
            if (clockWidgetEditing && gestureStartedOnClock && clockResizeEdge == 0
                    && clockPanel != null && clockPanel.getParent() instanceof FrameLayout) {
                clockWidgetDownRawX = event.getRawX();
                clockWidgetDownRawY = event.getRawY();
                clockWidgetStartX = clockPanel.getTranslationX();
                clockWidgetStartY = clockPanel.getTranslationY();
                clockWidgetDragging = false;
                int[] safeClockGeometry = itemGeometry(clockPanel);
                liveDragLastSafeGeometry.put(clockPanel,
                        new int[]{safeClockGeometry[0], safeClockGeometry[1]});
                clockGeometryRejectedAtLastMove = false;
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
                scheduleDrawerSwipeFrame(drawerSwipeProgress);
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
            // Coalesce high-frequency input into one clock transform per display frame.
            // This keeps movement locked to the finger without flooding layout/collision
            // work when the device reports multiple MOVE events within a single frame.
            LinearLayout moveReadyClock = clockGesturePanel != null
                    ? clockGesturePanel : clockPanel;
            if (clockWidgetMoving && !clockWidgetEditing && moveReadyClock != null) {
                scheduleClockDragMove(event.getRawX(), event.getRawY());
                return true;
            }
            // In edit mode, the clock body is a move handle. Edge grips stay isolated
            // in the resize branch below.
            LinearLayout editingClock = clockGesturePanel != null
                    ? clockGesturePanel : clockPanel;
            if (clockWidgetEditing && gestureStartedOnClock && clockResizeEdge == 0
                    && editingClock != null) {
                scheduleClockDragMove(event.getRawX(), event.getRawY());
                return true;
            }
            if (clockResizeEdge != 0 && clockWidgetDragging) {
                scheduleClockResizeMove(event.getRawX(), event.getRawY());
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
            // Consume vertical movement when this touch began during a page settle.
            // It must not open the drawer/control center as the pages are transitioning.
            if (pageTransitionBusyAtDown && !pageSwipeActive && verticalSwipe) {
                handler.removeCallbacks(homeLongPress);
                return true;
            }
            if (drawerCloseSwipeClaimed) {
                int swipeHeight = Math.max(1, pageViewport != null && pageViewport.getHeight() > 0
                        ? pageViewport.getHeight() : getResources().getDisplayMetrics().heightPixels);
                drawerSwipeProgress = Math.max(0f, Math.min(1f, drawerSwipeStartProgress - dy / (float) swipeHeight));
                scheduleDrawerSwipeFrame(drawerSwipeProgress);
                return true;
            }
            // A held item gesture belongs to its move/resize controller, not workspace navigation.
            // Fast vertical swipes can still start on home items; explicit resize handles are reserved.
            long gestureDuration = event.getEventTime() - event.getDownTime();
            boolean heldItemGesture = ((gestureStartedOnAppCell || gestureStartedOnWidget)
                    && gestureDuration > 450L)
                    || (gestureStartedOnContactShortcut && gestureDuration >= 1000L);
            boolean itemManipulation = gestureStartedOnClock || gestureStartedOnResizeHandle
                    || iconDragArmed || iconDragging || heldItemGesture
                    || gestureStartedOnAppGroupMoveMode;
            if (!drawerOpen && !itemManipulation
                    && !pageTransitionBusyAtDown && !pageTransitionBusy
                    && !verticalSwipeClaimed && verticalSwipe) {
                verticalSwipeClaimed = true;
                int swipeHeight = Math.max(1, pageViewport != null && pageViewport.getHeight() > 0
                        ? pageViewport.getHeight() : getResources().getDisplayMetrics().heightPixels);
                drawerSwipeProgress = Math.max(0f, Math.min(1f, drawerSwipeStartProgress - dy / (float) swipeHeight));
                if (dy < 0f) {
                    drawerSwipeVisualActive = true;
                    homeScreen.animate().cancel();
                    drawerScreen.animate().cancel();
                    scheduleDrawerSwipeFrame(drawerSwipeProgress);
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
                    scheduleDrawerSwipeFrame(drawerSwipeProgress);
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
                        && !gestureStartedOnAppGroupMoveMode && !heldItemGesture
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
                        pageTransitionBusy = true;
                        pageTransitionTargetPage = adjacent;
                        if (prefs != null && prefs.getBoolean(KEY_CLOCK_LIVE_GRADIENT, false)) {
                            handler.removeCallbacks(clockColorFrame);
                            clockColorLastFrameUptime = 0L;
                        }
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
            cancelClockDragMoveFrame();
            if (clockGesturePanel != null && clockGesturePage >= 0) {
                activateClockForPage(clockGesturePage);
            }
            handler.removeCallbacks(clockWidgetLongPress);
            handler.removeCallbacks(clockWidgetEditLongPress);
            clockGestureLockedAtDown = false;
            boolean cancelledEditorBodyMove = clockWidgetEditing && gestureStartedOnClock
                    && clockResizeEdge == 0 && clockWidgetDragging;
            if ((clockWidgetMoving && !clockWidgetEditing) || cancelledEditorBodyMove) {
                boolean keepEditorOpen = clockWidgetEditing;
                if (clockWidgetDragging && clockPanel != null
                        && clockPanel.getParent() instanceof FrameLayout) {
                    finishLiveDisplacements(false, clockPanel);
                }
                clockDragDisplacementPreviewActive = false;
                clockDragLastDisplacementX = Integer.MIN_VALUE;
                clockDragLastDisplacementY = Integer.MIN_VALUE;
                clockWidgetDragging = false;
                if (!keepEditorOpen) {
                    clockWidgetMoving = false;
                    showClockMoveBorder(false);
                }
                if (clockPanel != null) {
                    clockPanel.setTranslationX(clockWidgetStartX);
                    clockPanel.setTranslationY(clockWidgetStartY);
                }
                if (keepEditorOpen) {
                    setWorkspaceGridVisible(true, clockPanel);
                } else {
                    setWorkspaceGridVisible(false, null);
                }
                updateClockResizeHandles();
                return true;
            }
            if (clockWidgetEditing && gestureStartedOnClock && clockResizeEdge == 0) {
                updateClockResizeHandles();
                setWorkspaceGridVisible(true, clockPanel);
                return true;
            }
            if (clockResizeEdge != 0) {
                cancelClockResizeMoveFrame();
                finishClockResize(true);
                return true;
            }
            handler.removeCallbacks(homeLongPress);
            if (drawerCloseSwipeClaimed) {
                flushDrawerSwipeFrame();
                drawerCloseSwipeClaimed = false;
                // Progress is the remaining-open fraction: a 10% downward
                // swipe leaves progress at 90%, which is enough to close to Home.
                if (drawerSwipeProgress > 0.90f) {

                    LauncherScreenTransitions.finishDrawerSwipeToDrawer(this, homeScreen, drawerScreen,
                            this::markDrawerOpenAndRefreshArtwork, getDrawerTransition(), drawerSwipeProgress);
                } else {
                    LauncherScreenTransitions.finishDrawerSwipeToHome(this, homeScreen, drawerScreen,
                            () -> drawerOpen = false, getDrawerTransition(), drawerSwipeProgress);
                }
                return true;
            }
            if (drawerSwipeVisualActive) {
                flushDrawerSwipeFrame();
                drawerSwipeVisualActive = false;
                if (drawerSwipeProgress >= 0.10f) {

                    LauncherScreenTransitions.finishDrawerSwipeToDrawer(this, homeScreen, drawerScreen,
                            this::markDrawerOpenAndRefreshArtwork, getDrawerTransition(), drawerSwipeProgress);
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
            if (clockDragFramePending || clockWidgetDragging) {
                flushClockDragMove(event.getRawX(), event.getRawY());
            }
            if (clockGesturePanel != null && clockGesturePage >= 0) {
                activateClockForPage(clockGesturePage);
            }
            handler.removeCallbacks(clockWidgetLongPress);
            handler.removeCallbacks(clockWidgetEditLongPress);
            clockGestureLockedAtDown = false;
            boolean completedEditorBodyMove = clockWidgetEditing && gestureStartedOnClock
                    && clockResizeEdge == 0 && clockWidgetDragging;
            if ((clockWidgetMoving && !clockWidgetEditing) || completedEditorBodyMove) {
                boolean keepEditorOpen = clockWidgetEditing;
                int[] rememberedSafeGeometry = clockPanel == null
                        ? null : liveDragLastSafeGeometry.get(clockPanel);
                final int[] lastSafeClockGeometry = rememberedSafeGeometry == null
                        ? null : rememberedSafeGeometry.clone();
                boolean acceptedClockMove = true;
                if (clockWidgetDragging && clockPanel != null
                        && clockPanel.getParent() instanceof FrameLayout) {
                    // Flush the latest collision plan before commit even when the final
                    // movement was smaller than the per-frame planning threshold.
                    if (clockDragDisplacementPreviewActive) {
                        scheduleLiveDisplacementUpdate(
                                (FrameLayout) clockPanel.getParent(), clockPanel);
                    }
                    acceptedClockMove = finishLiveDisplacements(true, clockPanel);
                }
                clockDragDisplacementPreviewActive = false;
                clockDragLastDisplacementX = Integer.MIN_VALUE;
                clockDragLastDisplacementY = Integer.MIN_VALUE;
                if (!acceptedClockMove && clockPanel != null) {
                    boolean restoredLastSafe = false;
                    if (lastSafeClockGeometry != null
                            && clockPanel.getParent() instanceof FrameLayout) {
                        FrameLayout page = (FrameLayout) clockPanel.getParent();
                        setClockVisualPosition(clockPanel, lastSafeClockGeometry[0],
                                lastSafeClockGeometry[1], true);
                        int[] safeNow = itemGeometry(clockPanel);
                        restoredLastSafe = canPlaceWorkspaceItem(page, clockPanel,
                                new android.graphics.Rect(safeNow[0], safeNow[1],
                                        safeNow[0] + safeNow[2], safeNow[1] + safeNow[3]));
                    }
                    if (!restoredLastSafe) {
                        setClampedClockTranslation(clockWidgetStartX, clockWidgetStartY, true);
                    }
                }
                clockWidgetDragging = false;
                if (!keepEditorOpen) {
                    clockWidgetMoving = false;
                    showClockMoveBorder(false);
                }
                if (clockPanel != null) {
                    prefs.edit()
                            .putFloat(clockPrefKey(KEY_CLOCK_WIDGET_X, activeClockPage), clockPanel.getTranslationX())
                            .putFloat(clockPrefKey(KEY_CLOCK_WIDGET_Y, activeClockPage), clockPanel.getTranslationY())
                            .apply();
                }
                if (clockGeometryRejectedAtLastMove) showWorkspaceOverlapRejected();
                clockGeometryRejectedAtLastMove = false;
                if (keepEditorOpen) {
                    setWorkspaceGridVisible(true, clockPanel);
                } else {
                    setWorkspaceGridVisible(false, null);
                }
                updateClockResizeHandles();
                return true;
            }
            if (clockWidgetEditing && gestureStartedOnClock && clockResizeEdge == 0) {
                // A tap on the clock body in edit mode should not dismiss the editor.
                updateClockResizeHandles();
                setWorkspaceGridVisible(true, clockPanel);
                return true;
            }
            if (clockResizeEdge != 0) {
                flushClockResizeMove(event.getRawX(), event.getRawY());
                finishClockResize(false);
                return true;
            }
            handler.removeCallbacks(homeLongPress);
            if (longPressTriggered) return true;
            if (drawerCloseSwipeClaimed) {
                flushDrawerSwipeFrame();
                drawerCloseSwipeClaimed = false;
                // A 10% downward pull is enough to complete the drawer-to-Home
                // transition; progress represents how much of the drawer remains open.
                if (drawerSwipeProgress > 0.90f) {

                    LauncherScreenTransitions.finishDrawerSwipeToDrawer(this, homeScreen, drawerScreen,
                            this::markDrawerOpenAndRefreshArtwork, getDrawerTransition(), drawerSwipeProgress);
                } else {
                    LauncherScreenTransitions.finishDrawerSwipeToHome(this, homeScreen, drawerScreen,
                            () -> drawerOpen = false, getDrawerTransition(), drawerSwipeProgress);
                }
                return true;
            }
            if (drawerSwipeVisualActive) {
                flushDrawerSwipeFrame();
                drawerSwipeVisualActive = false;
                if (drawerSwipeProgress >= 0.10f) {

                    LauncherScreenTransitions.finishDrawerSwipeToDrawer(this, homeScreen, drawerScreen,
                            this::markDrawerOpenAndRefreshArtwork, getDrawerTransition(), drawerSwipeProgress);
                } else {
                    LauncherScreenTransitions.cancelDrawerSwipe(this, homeScreen, drawerScreen,
                            getDrawerTransition());
                }
                return true;
            }
            long releaseGestureDuration = event.getEventTime() - event.getDownTime();
            boolean itemManipulationOnRelease = gestureStartedOnClock || gestureStartedOnResizeHandle
                    || iconDragArmed || iconDragging || gestureStartedOnAppGroupMoveMode
                    || ((gestureStartedOnAppCell || gestureStartedOnWidget)
                        && releaseGestureDuration > 450L)
                    || (gestureStartedOnContactShortcut && releaseGestureDuration >= 1000L);
            if (!drawerOpen && !itemManipulationOnRelease
                    && !pageTransitionBusyAtDown && !pageTransitionBusy
                    && (verticalSwipeClaimed
                        || (Math.abs(dy) > LauncherUiUtils.dp(this, 65)
                            && Math.abs(dy) > Math.abs(dx) * 1.15f))) {
                if (dy < 0f) {
                    // Finish from the current pose, but do not force a tiny partial
                    // swipe all the way open. Always settle to home or drawer.
                    if (drawerSwipeProgress >= 0.10f) {

                    LauncherScreenTransitions.finishDrawerSwipeToDrawer(this, homeScreen, drawerScreen,
                                this::markDrawerOpenAndRefreshArtwork, getDrawerTransition(), drawerSwipeProgress);
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

    private boolean touchStartedOnTransitionPage(View page, float rawX, float rawY) {
        if (page == null || page.getVisibility() != View.VISIBLE
                || page.getWidth() <= 0 || page.getHeight() <= 0) return false;
        int[] location = new int[2];
        page.getLocationOnScreen(location);
        float width = page.getWidth() * Math.max(0.01f, Math.abs(page.getScaleX()));
        float height = page.getHeight() * Math.max(0.01f, Math.abs(page.getScaleY()));
        return rawX >= location[0] && rawX < location[0] + width
                && rawY >= location[1] && rawY < location[1] + height;
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
            // Keep the established seven-segment hit region untouched. Typography clocks
            // include their overhanging digits and date because both are visible workspace ink.
            if (prefs != null && prefs.getInt(KEY_CLOCK_STYLE, 0) == 5) {
                int[] location = new int[2];
                clockPanel.getLocationOnScreen(location);
                float visualWidth = clockPanel.getWidth()
                        * Math.max(0.01f, clockPanel.getScaleX());
                float visualHeight = clockPanel.getHeight()
                        * Math.max(0.01f, clockPanel.getScaleY());
                if (rawX >= location[0] && rawX < location[0] + visualWidth
                        && rawY >= location[1] && rawY < location[1] + visualHeight) return true;
            } else {
                View page = clockPanel.getParent() instanceof View
                        ? (View) clockPanel.getParent() : null;
                if (page != null) {
                    int[] geometry = itemGeometry(clockPanel);
                    android.graphics.Rect rendered = clockVisualBoundsAt(clockPanel,
                            geometry[0], geometry[1], geometry[2], geometry[3]);
                    int[] pageLocation = new int[2];
                    page.getLocationOnScreen(pageLocation);
                    rendered.offset(pageLocation[0], pageLocation[1]);
                    // While moving/editing, tolerate small mismatches from scaled child text
                    // bounds and screen insets; otherwise a valid body drag can be mistaken
                    // for an outside tap and the editor closes before the MOVE arrives.
                    if (clockWidgetEditing || clockWidgetMoving) {
                        int touchSlop = LauncherUiUtils.dp(this, 12);
                        rendered.inset(-touchSlop, -touchSlop);
                    }
                    if (rawX >= rendered.left && rawX < rendered.right
                            && rawY >= rendered.top && rawY < rendered.bottom) return true;
                }
            }
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
     * Apply at most one move transform per rendered frame. Raw touch events are
     * coalesced, while ACTION_UP flushes the final coordinates before committing.
     */
    private boolean clockDragFramePending = false;
    private float clockDragPendingRawX;
    private float clockDragPendingRawY;
    // Full collision planning is expensive on older devices. Only invoke it while the
    // clock actually touches another workspace item, and throttle replans by distance.
    private boolean clockDragDisplacementPreviewActive = false;
    private int clockDragLastDisplacementX = Integer.MIN_VALUE;
    private int clockDragLastDisplacementY = Integer.MIN_VALUE;
    private int clockDragLastCollisionCheckX = Integer.MIN_VALUE;
    private int clockDragLastCollisionCheckY = Integer.MIN_VALUE;
    private final Runnable clockDragFrame = () -> {
        clockDragFramePending = false;
        applyClockDragMove(clockDragPendingRawX, clockDragPendingRawY, false);
    };

    private void scheduleClockDragMove(float rawX, float rawY) {
        clockDragPendingRawX = rawX;
        clockDragPendingRawY = rawY;
        if (clockDragFramePending) return;
        clockDragFramePending = true;
        View target = clockGesturePanel != null ? clockGesturePanel : clockPanel;
        if (target == null) {
            clockDragFramePending = false;
            applyClockDragMove(rawX, rawY, false);
            return;
        }
        target.postOnAnimation(clockDragFrame);
    }

    private void flushClockDragMove(float rawX, float rawY) {
        View target = clockGesturePanel != null ? clockGesturePanel : clockPanel;
        if (target != null) target.removeCallbacks(clockDragFrame);
        clockDragFramePending = false;
        clockDragPendingRawX = rawX;
        clockDragPendingRawY = rawY;
        // Always run collision restoration against the actual final coordinates before
        // committing displaced neighbours on ACTION_UP.
        applyClockDragMove(rawX, rawY, true);
    }

    private void cancelClockDragMoveFrame() {
        View target = clockGesturePanel != null ? clockGesturePanel : clockPanel;
        if (target != null) target.removeCallbacks(clockDragFrame);
        clockDragFramePending = false;
    }

    private void applyClockDragMove(float rawX, float rawY, boolean forceCollisionCheck) {
        boolean moveReady = clockWidgetMoving && !clockWidgetEditing;
        boolean editingBody = clockWidgetEditing && gestureStartedOnClock && clockResizeEdge == 0;
        if (!moveReady && !editingBody) return;

        final LinearLayout movingPanel = clockGesturePanel != null
                ? clockGesturePanel : clockPanel;
        final int movingPage = clockGesturePage >= 0 ? clockGesturePage : activeClockPage;
        if (movingPanel == null || movingPage < 0 || movingPage >= clockPanelsByPage.length
                || movingPanel != clockPanelsByPage[movingPage]
                || !(movingPanel.getParent() instanceof FrameLayout)) return;

        float moveX = rawX - clockWidgetDownRawX;
        float moveY = rawY - clockWidgetDownRawY;
        float slop = LauncherUiUtils.dp(this, 5);
        if (!clockWidgetDragging && Math.abs(moveX) <= slop && Math.abs(moveY) <= slop) return;

        boolean firstMoveFrame = !clockWidgetDragging;
        if (firstMoveFrame) {
            int[] safeGeometry = itemGeometry(movingPanel);
            liveDragLastSafeGeometry.put(movingPanel,
                    new int[]{safeGeometry[0], safeGeometry[1]});
            clockDragDisplacementPreviewActive = false;
            clockDragLastDisplacementX = Integer.MIN_VALUE;
            clockDragLastDisplacementY = Integer.MIN_VALUE;
            clockDragLastCollisionCheckX = Integer.MIN_VALUE;
            clockDragLastCollisionCheckY = Integer.MIN_VALUE;
        }
        clockWidgetDragging = true;
        if (moveReady) handler.removeCallbacks(clockWidgetEditLongPress);
        setClampedClockTranslationForPanel(movingPanel, movingPage,
                clockWidgetStartX + moveX, clockWidgetStartY + moveY, false);
        if (firstMoveFrame) setWorkspaceGridVisible(true, movingPanel);
        if (movingPanel.getParent() instanceof FrameLayout) {
            FrameLayout page = (FrameLayout) movingPanel.getParent();
            int[] geometry = itemGeometry(movingPanel);
            int collisionStep = LauncherUiUtils.dp(this, 4);
            boolean scanCollisions = forceCollisionCheck
                    || clockDragLastCollisionCheckX == Integer.MIN_VALUE
                    || Math.abs(geometry[0] - clockDragLastCollisionCheckX) >= collisionStep
                    || Math.abs(geometry[1] - clockDragLastCollisionCheckY) >= collisionStep;
            // Moving the panel itself is updated every display frame, but collision
            // discovery is throttled to 4dp movement increments. Running a full item
            // geometry scan for every tiny MOVE event made the clock lag on older phones.
            if (scanCollisions) {
                boolean overlapping =
                        clockDragOverlapsWorkspaceItems(page, movingPanel, geometry);
                if (!overlapping) {
                    liveDragLastSafeGeometry.put(movingPanel,
                            new int[]{geometry[0], geometry[1]});
                }
                updateClockDragDisplacementPreview(page, movingPanel, geometry, overlapping);
                clockDragLastCollisionCheckX = geometry[0];
                clockDragLastCollisionCheckY = geometry[1];
            }
        }
        // The grips are hidden in move-ready mode; only reposition them when edit mode
        // is active, avoiding four root-overlay updates on every ordinary drag frame.
        if (clockWidgetEditing) scheduleClockResizeHandlesUpdate();
    }

    private boolean clockDragOverlapsWorkspaceItems(
            FrameLayout page, View movingPanel, int[] movingGeometry) {
        if (page == null || movingPanel == null || movingGeometry == null) return false;
        android.graphics.Rect movingBounds = collisionRectForInteraction(
                movingPanel, movingGeometry, null);
        for (int i = 0; i < page.getChildCount(); i++) {
            View other = page.getChildAt(i);
            if (other == movingPanel || other == workspaceTransferProxy
                    || other.getVisibility() != View.VISIBLE || !isWorkspaceItem(other)) continue;
            int[] geometry = itemGeometry(other);
            if (geometry[2] <= 0 || geometry[3] <= 0) continue;
            android.graphics.Rect current = collisionRectForInteraction(other, geometry, movingPanel);
            if (android.graphics.Rect.intersects(movingBounds, current)) return true;
            int[] target = liveDisplacementTargets.get(other);
            if (target != null) {
                android.graphics.Rect reserved = collisionRectAtForInteraction(
                        other, target[0], target[1], geometry[2], geometry[3], movingPanel);
                if (android.graphics.Rect.intersects(movingBounds, reserved)) return true;
            }
        }
        return false;
    }

    private void updateClockDragDisplacementPreview(
            FrameLayout page, View movingPanel, int[] geometry, boolean overlapping) {
        if (page == null || movingPanel == null || geometry == null) return;
        int threshold = LauncherUiUtils.dp(this, 12);
        boolean movedEnough = clockDragLastDisplacementX == Integer.MIN_VALUE
                || Math.abs(geometry[0] - clockDragLastDisplacementX) >= threshold
                || Math.abs(geometry[1] - clockDragLastDisplacementY) >= threshold;
        if (overlapping) {
            if (!clockDragDisplacementPreviewActive || movedEnough) {
                scheduleLiveDisplacementUpdate(page, movingPanel);
                clockDragLastDisplacementX = geometry[0];
                clockDragLastDisplacementY = geometry[1];
            }
            clockDragDisplacementPreviewActive = true;
        } else if (clockDragDisplacementPreviewActive) {
            // One final pass reverses any temporary neighbour movement.
            scheduleLiveDisplacementUpdate(page, movingPanel);
            clockDragDisplacementPreviewActive = false;
            clockDragLastDisplacementX = geometry[0];
            clockDragLastDisplacementY = geometry[1];
        }
    }

    /**
     * Resize validation checks only the strip added beyond the original rectangle.
     * Rechecking the full rectangle can freeze every frame if a neighbouring item
     * already intersects the clock's fixed portion; shrinking that edge should still
     * remain possible, and expansion is blocked only by newly occupied space.
     */
    /**
     * Limit expansion only where the new strip would meet a neighbour. Work is linear
     * in the page's child count, rather than repeatedly validating the complete rectangle
     * and binary-searching on every MOVE frame. The original rectangle is the fixed anchor
     * for the duration of one resize transaction.
     */
    private android.graphics.Rect constrainClockResizeExpansion(
            FrameLayout page, View clock, android.graphics.Rect requested, int edge) {
        android.graphics.Rect bounds = new android.graphics.Rect(requested);
        if (page == null || clock == null || edge < 1 || edge > 4) return bounds;
        final int startLeft = clockWidgetStartVisualX;
        final int startTop = clockWidgetStartVisualY;
        final int startRight = startLeft + clockWidgetStartVisualWidth;
        final int startBottom = startTop + clockWidgetStartVisualHeight;
        final int gap = Math.max(1, LauncherUiUtils.dp(this, 5));

        for (int i = 0; i < page.getChildCount(); i++) {
            View other = page.getChildAt(i);
            if (other == clock || other == workspaceTransferProxy
                    || other.getVisibility() != View.VISIBLE || !isWorkspaceItem(other)) continue;
            int[] geometry = itemGeometry(other);
            if (geometry[2] <= 0 || geometry[3] <= 0) continue;
            android.graphics.Rect obstacle = collisionRectForInteraction(other, geometry, clock);
            obstacle.inset(-gap, -gap);
            constrainClockExpansionAgainstObstacle(bounds, obstacle, edge,
                    startLeft, startTop, startRight, startBottom);

            int[] reserved = liveDisplacementTargets.get(other);
            if (reserved != null) {
                android.graphics.Rect reservedBounds = collisionRectAtForInteraction(
                        other, reserved[0], reserved[1], geometry[2], geometry[3], clock);
                reservedBounds.inset(-gap, -gap);
                constrainClockExpansionAgainstObstacle(bounds, reservedBounds, edge,
                        startLeft, startTop, startRight, startBottom);
            }
        }
        return bounds;
    }

    private void constrainClockExpansionAgainstObstacle(android.graphics.Rect bounds,
            android.graphics.Rect obstacle, int edge, int startLeft, int startTop,
            int startRight, int startBottom) {
        if (edge == 1 && bounds.left < startLeft
                && bounds.top < obstacle.bottom && bounds.bottom > obstacle.top
                && obstacle.left < startLeft && obstacle.right > bounds.left) {
            bounds.left = Math.max(bounds.left, Math.min(startLeft, obstacle.right));
        } else if (edge == 2 && bounds.right > startRight
                && bounds.top < obstacle.bottom && bounds.bottom > obstacle.top
                && obstacle.right > startRight && obstacle.left < bounds.right) {
            bounds.right = Math.min(bounds.right, Math.max(startRight, obstacle.left));
        } else if (edge == 3 && bounds.top < startTop
                && bounds.left < obstacle.right && bounds.right > obstacle.left
                && obstacle.top < startTop && obstacle.bottom > bounds.top) {
            bounds.top = Math.max(bounds.top, Math.min(startTop, obstacle.bottom));
        } else if (edge == 4 && bounds.bottom > startBottom
                && bounds.left < obstacle.right && bounds.right > obstacle.left
                && obstacle.bottom > startBottom && obstacle.top < bounds.bottom) {
            bounds.bottom = Math.min(bounds.bottom, Math.max(startBottom, obstacle.top));
        }
    }

    private boolean canApplyClockResizeCandidate(FrameLayout page, View clock,
            android.graphics.Rect candidate, int edge) {
        if (page == null || clock == null || candidate == null || edge < 1 || edge > 4
                || candidate.left < 0 || candidate.top < 0
                || candidate.right > page.getWidth() || candidate.bottom > page.getHeight()
                || candidate.width() <= 0 || candidate.height() <= 0) return false;

        int startLeft = clockWidgetStartVisualX;
        int startTop = clockWidgetStartVisualY;
        int startRight = startLeft + clockWidgetStartVisualWidth;
        int startBottom = startTop + clockWidgetStartVisualHeight;
        android.graphics.Rect addedArea = null;
        if (edge == 1 && candidate.left < startLeft) {
            addedArea = new android.graphics.Rect(candidate.left, candidate.top,
                    startLeft, candidate.bottom);
        } else if (edge == 2 && candidate.right > startRight) {
            addedArea = new android.graphics.Rect(startRight, candidate.top,
                    candidate.right, candidate.bottom);
        } else if (edge == 3 && candidate.top < startTop) {
            addedArea = new android.graphics.Rect(candidate.left, candidate.top,
                    candidate.right, startTop);
        } else if (edge == 4 && candidate.bottom > startBottom) {
            addedArea = new android.graphics.Rect(candidate.left, startBottom,
                    candidate.right, candidate.bottom);
        }
        // Moving an edge inward only frees space and remains legal if screen and
        // minimum bounds are valid. Do not freeze it because an old portion of the
        // clock already intersects another item's footprint.
        if (addedArea == null || addedArea.isEmpty()) return true;

        int gap = Math.max(1, LauncherUiUtils.dp(this, 5));
        addedArea.inset(-gap, -gap);
        for (int i = 0; i < page.getChildCount(); i++) {
            View other = page.getChildAt(i);
            if (other == clock || other == workspaceTransferProxy
                    || other.getVisibility() != View.VISIBLE || !isWorkspaceItem(other)) continue;
            int[] geometry = itemGeometry(other);
            if (geometry[2] <= 0 || geometry[3] <= 0) continue;
            android.graphics.Rect obstacle = collisionRectForInteraction(other, geometry, clock);
            if (android.graphics.Rect.intersects(addedArea, obstacle)) return false;
            int[] reserved = liveDisplacementTargets.get(other);
            if (reserved != null) {
                android.graphics.Rect reservedBounds = collisionRectAtForInteraction(
                        other, reserved[0], reserved[1], geometry[2], geometry[3], clock);
                if (android.graphics.Rect.intersects(addedArea, reservedBounds)) return false;
            }
        }
        return true;
    }

    private boolean clockResizeFramePending = false;
    private float clockResizePendingRawX;
    private float clockResizePendingRawY;
    private final Runnable clockResizeFrame = () -> {
        clockResizeFramePending = false;
        applyClockResizeMove(clockResizePendingRawX, clockResizePendingRawY);
    };

    private void scheduleClockResizeMove(float rawX, float rawY) {
        clockResizePendingRawX = rawX;
        clockResizePendingRawY = rawY;
        if (clockResizeFramePending) return;
        clockResizeFramePending = true;
        View target = clockGesturePanel != null ? clockGesturePanel : clockPanel;
        if (target == null) {
            clockResizeFramePending = false;
            applyClockResizeMove(rawX, rawY);
            return;
        }
        target.postOnAnimation(clockResizeFrame);
    }

    private void flushClockResizeMove(float rawX, float rawY) {
        View target = clockGesturePanel != null ? clockGesturePanel : clockPanel;
        if (target != null) target.removeCallbacks(clockResizeFrame);
        clockResizeFramePending = false;
        clockResizePendingRawX = rawX;
        clockResizePendingRawY = rawY;
        applyClockResizeMove(rawX, rawY);
    }

    private void cancelClockResizeMoveFrame() {
        View target = clockGesturePanel != null ? clockGesturePanel : clockPanel;
        if (target != null) target.removeCallbacks(clockResizeFrame);
        clockResizeFramePending = false;
    }

    /**
     * Minimum vertical space that must remain outside the resizable clock row.
     * The date TextView includes the AM/PM prefix as styled text; using only the
     * panel's previous measured-height remainder can undercount it after font/span
     * changes and clip the complete line during a resize.
     */
    private int clockNonRowChromeHeight(LinearLayout panel, TextView date) {
        if (panel == null) return 0;
        int height = panel.getPaddingTop() + panel.getPaddingBottom();
        if (date == null || date.getVisibility() == View.GONE) return height;

        int dateHeight = Math.max(date.getHeight(), date.getMeasuredHeight());
        if (dateHeight <= 0) {
            android.graphics.Paint.FontMetrics metrics = date.getPaint().getFontMetrics();
            dateHeight = Math.max(LauncherUiUtils.dp(this, 14),
                    (int) Math.ceil(metrics.descent - metrics.ascent)
                            + date.getPaddingTop() + date.getPaddingBottom());
        }
        ViewGroup.LayoutParams rawParams = date.getLayoutParams();
        if (rawParams instanceof LinearLayout.LayoutParams) {
            LinearLayout.LayoutParams dateParams = (LinearLayout.LayoutParams) rawParams;
            height += dateParams.topMargin + dateParams.bottomMargin;
        }
        return height + dateHeight;
    }

    private void applyClockResizeMove(float rawX, float rawY) {
        if (clockResizeEdge == 0 || !clockWidgetDragging) return;
        final LinearLayout resizePanel = clockGesturePanel != null
                ? clockGesturePanel : clockPanel;
        final int resizePage = clockGesturePage >= 0 ? clockGesturePage : activeClockPage;
        if (resizePanel == null || resizePanel != clockPanelsByPage[resizePage]
                || !(resizePanel.getParent() instanceof FrameLayout)) return;
        activateClockForPage(resizePage);

                // Use the widget resizer's pixel-space rules: page boundaries are hard
                // limits, the opposite edge stays fixed, and overlapping candidates are rejected.
                if (!(resizePanel.getParent() instanceof FrameLayout)) return;
                FrameLayout activePage = (FrameLayout) resizePanel.getParent();
                float resizeDeltaX = rawX - clockWidgetDownRawX;
                float resizeDeltaY = rawY - clockWidgetDownRawY;
                int pageWidth = activePage.getWidth();
                int pageHeight = activePage.getHeight();
                if (pageWidth <= 0 || pageHeight <= 0) return;

                int left = clockWidgetStartVisualX;
                int top = clockWidgetStartVisualY;
                int right = left + clockWidgetStartVisualWidth;
                int bottom = top + clockWidgetStartVisualHeight;
                // Never force the inactive axis to a minimum size: doing so changes the
                // opposite edge when a vertical grip is dragged and creates a false resize
                // boundary. The minimum is no larger than the rectangle at gesture start.
                int minW = Math.min(Math.min(LauncherUiUtils.dp(this, 140), pageWidth),
                        Math.max(1, clockWidgetStartVisualWidth));
                int breadthFloorHeight =
                        (LauncherUiUtils.dp(this, 114) * 80 + 99) / 100;
                int minRowHeight = Math.max(LauncherUiUtils.dp(this, 90),
                        breadthFloorHeight);
                // The visible panel includes a fixed date line and its insets. Apply the
                // 80% floor to the clock row, then add that fixed chrome back to the
                // minimum total height so date/AM-PM never gets pushed beyond the panel.
                int minH = Math.min(Math.min(
                        minRowHeight + clockWidgetStartNonRowHeight, pageHeight),
                        Math.max(1, clockWidgetStartVisualHeight));
                boolean horizontalResize = clockResizeEdge == 1 || clockResizeEdge == 2;
                if (horizontalResize) {
                    if (clockResizeEdge == 1) {
                        left = Math.round(clockWidgetStartVisualX + resizeDeltaX);
                        left = Math.max(0, Math.min(left, right - minW));
                    } else {
                        right = Math.round(clockWidgetStartVisualX
                                + clockWidgetStartVisualWidth + resizeDeltaX);
                        right = Math.max(left + minW, Math.min(right, pageWidth));
                    }
                    left = Math.max(0, Math.min(left, pageWidth - minW));
                    right = Math.max(left + minW, Math.min(right, pageWidth));
                    // A horizontal resize preserves both vertical edges exactly.
                    top = clockWidgetStartVisualY;
                    bottom = clockWidgetStartVisualY + clockWidgetStartVisualHeight;
                } else {
                    // A vertical resize preserves the original horizontal rectangle exactly.
                    left = clockWidgetStartVisualX;
                    right = clockWidgetStartVisualX + clockWidgetStartVisualWidth;
                    if (clockResizeEdge == 3) {
                        top = Math.round(clockWidgetStartVisualY + resizeDeltaY);
                        top = Math.max(0, Math.min(top, bottom - minH));
                        bottom = clockWidgetStartVisualY + clockWidgetStartVisualHeight;
                    } else {
                        top = clockWidgetStartVisualY;
                        bottom = Math.round(clockWidgetStartVisualY
                                + clockWidgetStartVisualHeight + resizeDeltaY);
                        bottom = Math.max(top + minH, Math.min(bottom, pageHeight));
                    }
                }
                // Only an expanding edge can collide with new workspace items. Shrinking
                // is always allowed after the page/minimum checks above, so skip the two
                // full-page collision scans for that common path. When expansion is
                // blocked, keep the last accepted geometry instead of snapping the grip
                // back to its DOWN coordinate; the user can reverse direction immediately.
                android.graphics.Rect constrainedBounds =
                        new android.graphics.Rect(left, top, right, bottom);
                boolean expandsIntoWorkspace =
                        (clockResizeEdge == 1 && left < clockWidgetStartVisualX)
                        || (clockResizeEdge == 2
                                && right > clockWidgetStartVisualX + clockWidgetStartVisualWidth)
                        || (clockResizeEdge == 3 && top < clockWidgetStartVisualY)
                        || (clockResizeEdge == 4
                                && bottom > clockWidgetStartVisualY + clockWidgetStartVisualHeight);
                if (expandsIntoWorkspace) {
                    constrainedBounds = constrainClockResizeExpansion(
                            activePage, resizePanel, constrainedBounds, clockResizeEdge);
                    if (!canApplyClockResizeCandidate(activePage, resizePanel,
                            constrainedBounds, clockResizeEdge)) {
                        clockGeometryRejectedAtLastMove = true;
                        return;
                    }
                }
                left = constrainedBounds.left;
                top = constrainedBounds.top;
                right = constrainedBounds.right;
                bottom = constrainedBounds.bottom;

                // The panel's measured width is its unscaled natural width; scaleX
                // is the saved length percentage. Convert the requested visible width
                // directly to that scale. Do not multiply the old preference by a ratio
                // of visible widths: when the old clock is capped at the page edge, its
                // visible width no longer represents the saved scale and shrinking stalls.
                int lengthPercent = Math.round((right - left) * 100f
                        / (float) Math.max(1, clockWidgetStartPanelWidth));
                int breadthPercent;
                int naturalRowHeight = LauncherUiUtils.dp(this, 114);
                int targetPanelHeight = Math.max(1, bottom - top);
                int targetRowHeight = Math.max(1,
                        targetPanelHeight - clockWidgetStartNonRowHeight);
                // A left/right grip is strictly a width operation. Preserve the
                // stored breadth percentage. Top/bottom resize maps only the clock row;
                // the date line and panel insets retain their original height.
                breadthPercent = horizontalResize ? clockWidgetStartBreadth
                        : Math.round(targetRowHeight * 100f / Math.max(1, naturalRowHeight));
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
                int maxAvailableRowHeight = Math.max(minRowHeight,
                        pageHeight - clockWidgetStartNonRowHeight);
                int maxBreadthPercent = Math.max(80, Math.min(600, (int) Math.ceil(
                        maxAvailableRowHeight * 100f / Math.max(1, naturalRowHeight))));
                breadthPercent = Math.max(80, Math.min(maxBreadthPercent, breadthPercent));
                // Bounds/collision validation already ran above. The candidate rectangle
                // has not changed since then; repeating the full page scan here cost another
                // frame on every resize event and caused visible handle stutter.
                if (resizePanel.getParent() != activePage
                        || left < 0 || top < 0 || right > pageWidth || bottom > pageHeight
                        || right <= left || bottom <= top) {
                    clockGeometryRejectedAtLastMove = true;
                    return;
                }
                clockGeometryRejectedAtLastMove = false;
                clockWidgetLiveResizeLength = lengthPercent;
                clockWidgetLiveResizeBreadth = breadthPercent;
                final int resizingEdge = clockResizeEdge;
                final int targetLeft = left;
                final int targetTop = top;
                final int targetRight = right;
                final int targetBottom = bottom;
                final int targetHeight = Math.max(1, targetBottom - targetTop);
                // targetRowHeight was derived from the validated candidate above and is
                // reused here to keep the date-line allowance identical for this frame.
                final int naturalRowHeightForResize = LauncherUiUtils.dp(this, 114);
                boolean sevenSegmentResize = clockTextsByPage[resizePage]
                        instanceof SevenSegmentClockTextView
                        && prefs.getInt(KEY_CLOCK_STYLE, 0) == 5;

                if (horizontalResize) {
                    // A length drag is a panel transform; avoid remeasuring every child.
                    resizePanel.setScaleX((targetRight - targetLeft)
                            / (float) Math.max(1, clockWidgetStartPanelWidth));
                } else {
                    // Breadth changes the real background/row height. Keep existing child
                    // typography and date styles; only update the row-related params.
                    ViewGroup.LayoutParams panelHeightParams = resizePanel.getLayoutParams();
                    if (panelHeightParams != null) {
                        panelHeightParams.height = targetHeight;
                        resizePanel.setLayoutParams(panelHeightParams);
                    }
                    if (clockRow != null) {
                        ViewGroup.LayoutParams rowHeightParams = clockRow.getLayoutParams();
                        if (rowHeightParams != null) {
                            rowHeightParams.height = targetRowHeight;
                            clockRow.setLayoutParams(rowHeightParams);
                        }
                    }
                    if (clockGroup != null) {
                        ViewGroup.LayoutParams groupHeightParams = clockGroup.getLayoutParams();
                        if (groupHeightParams != null) {
                            groupHeightParams.height = targetRowHeight;
                            clockGroup.setLayoutParams(groupHeightParams);
                        }
                    }
                    TextView resizeText = clockTextsByPage[resizePage];
                    if (resizeText != null) {
                        ViewGroup.LayoutParams textHeightParams = resizeText.getLayoutParams();
                        if (textHeightParams != null) {
                            textHeightParams.height = sevenSegmentResize
                                    ? naturalRowHeightForResize : targetRowHeight;
                            resizeText.setLayoutParams(textHeightParams);
                        }
                        resizeText.setPivotY(sevenSegmentResize ? 0f : targetRowHeight / 2f);
                        resizeText.setScaleY(sevenSegmentResize
                                ? targetRowHeight / (float) Math.max(1, naturalRowHeightForResize)
                                : clockWidgetStartTextScaleY * targetRowHeight
                                        / (float) Math.max(1, clockWidgetStartRowHeight));
                    }
                    syncClockColonVerticalCenter();
                }

                // Recompute translation from the absolute target edge each frame. Adding a
                // correction to the previous translation accumulates rounding/layout error
                // and makes the whole clock drift or fluctuate during resizing.
                float savedTranslationX = resizePanel.getTranslationX();
                float savedTranslationY = resizePanel.getTranslationY();
                resizePanel.setTranslationX(0f);
                resizePanel.setTranslationY(0f);
                int[] unshifted = itemGeometry(resizePanel);
                float correctedX = savedTranslationX;
                float correctedY = savedTranslationY;
                if (resizingEdge == 1) {
                    correctedX = targetRight - (unshifted[0] + unshifted[2]);
                } else if (resizingEdge == 2) {
                    correctedX = targetLeft - unshifted[0];
                }
                if (resizingEdge == 3) {
                    correctedY = targetBottom - (unshifted[1] + unshifted[3]);
                } else if (resizingEdge == 4) {
                    correctedY = targetTop - unshifted[1];
                }
                resizePanel.setTranslationX(correctedX);
                resizePanel.setTranslationY(correctedY);
                scheduleClockResizeHandlesUpdate();


    }

    private void finishClockResize(boolean cancelled) {
        if (clockResizeEdge == 0 || clockPanel == null) return;
        // On release, let the newest MOVE's queued edge-anchor correction finish after
        // layout. Only cancellation invalidates that correction.
        if (cancelled) ++clockResizeGeneration;
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
            // Commit the live geometry once when the finger is released; per-frame disk
            // scheduling made resize feedback compete with the next motion event.
            prefs.edit()
                    .putInt(clockPrefKey(KEY_CLOCK_LENGTH, activeClockPage), clockWidgetLiveResizeLength)
                    .putInt(clockPrefKey(KEY_CLOCK_BREADTH, activeClockPage), clockWidgetLiveResizeBreadth)
                    .putFloat(clockPrefKey(KEY_CLOCK_WIDGET_X, activeClockPage), clockPanel.getTranslationX())
                    .putFloat(clockPrefKey(KEY_CLOCK_WIDGET_Y, activeClockPage), clockPanel.getTranslationY())
                    .apply();
        }
        // The accepted resize rectangle is already page-bounded and collision-checked.
        // Do not run the move-mode clamp or nearest-free relocation here: either can
        // shift the complete clock after its opposite edge was anchored.
        final int finalGeometryGeneration = clockResizeGeneration;
        final LinearLayout finishedPanel = clockPanel;
        final int finishedPage = activeClockPage;
        if (!cancelled) {
            prefs.edit()
                    .putFloat(clockPrefKey(KEY_CLOCK_WIDGET_X, finishedPage),
                            finishedPanel.getTranslationX())
                    .putFloat(clockPrefKey(KEY_CLOCK_WIDGET_Y, finishedPage),
                            finishedPanel.getTranslationY())
                    .apply();
        }
        finishedPanel.post(() -> {
            if (finishedPanel != clockPanelsByPage[finishedPage]
                    || finishedPanel.getParent() == null
                    || clockResizeGeneration != finalGeometryGeneration
                    || clockResizeEdge != 0) return;
            if (cancelled) {
                setClampedClockTranslationForPanel(finishedPanel, finishedPage,
                        finishedPanel.getTranslationX(), finishedPanel.getTranslationY(), true);
            } else {
                // The last live frame already owns the edge correction.
                prefs.edit()
                        .putInt(clockPrefKey(KEY_CLOCK_LENGTH, finishedPage), clockWidgetLiveResizeLength)
                        .putInt(clockPrefKey(KEY_CLOCK_BREADTH, finishedPage), clockWidgetLiveResizeBreadth)
                        .putFloat(clockPrefKey(KEY_CLOCK_WIDGET_X, finishedPage),
                                finishedPanel.getTranslationX())
                        .putFloat(clockPrefKey(KEY_CLOCK_WIDGET_Y, finishedPage),
                                finishedPanel.getTranslationY())
                        .apply();
            }
            if (clockPanel == finishedPanel && activeClockPage == finishedPage) {
                updateClockResizeHandles();
            }
        });
        if (!cancelled && clockGeometryRejectedAtLastMove) showWorkspaceOverlapRejected();
        clockGeometryRejectedAtLastMove = false;
        setWorkspaceGridVisible(false, null);
        updateClockResizeHandles();
    }

    private void updateClockResizeHandles() {
        if (clockPanel == null || root == null
                || clockPanel.getWidth() <= 0 || clockPanel.getHeight() <= 0) return;
        if (!(clockPanel.getParent() instanceof View)) return;
        View pageView = (View) clockPanel.getParent();
        int[] geometry = itemGeometry(clockPanel);
        int[] rootOnScreen = new int[2];
        int[] pageOnScreen = new int[2];
        root.getLocationOnScreen(rootOnScreen);
        pageView.getLocationOnScreen(pageOnScreen);
        int visualLeft = pageOnScreen[0] - rootOnScreen[0] + geometry[0];
        int visualTop = pageOnScreen[1] - rootOnScreen[1] + geometry[1];
        int w = Math.max(1, geometry[2]);
        int h = Math.max(1, geometry[3]);
        int side = LauncherUiUtils.dp(this, 40);
        int thin = LauncherUiUtils.dp(this, 18);
        int edgeGap = LauncherUiUtils.dp(this, 1);
        int[][] positions = new int[][] {
                {visualLeft + edgeGap, visualTop + h / 2 - side / 2},
                {visualLeft + w - thin - edgeGap, visualTop + h / 2 - side / 2},
                {visualLeft + w / 2 - side / 2, visualTop + edgeGap},
                {visualLeft + w / 2 - side / 2, visualTop + h - thin - edgeGap}
        };
        boolean visible = clockWidgetEditing;
        for (int i = 0; i < clockEdgeHandles.length; i++) {
            View grip = clockEdgeHandles[i];
            if (grip == null) continue;
            // These grips are fixed-size root overlays. Translation moves them without
            // triggering a full root measure/layout on every MOVE event.
            grip.setX(positions[i][0]);
            grip.setY(positions[i][1]);
            if (visible && grip.getVisibility() != View.VISIBLE) {
                grip.bringToFront();
                grip.setVisibility(View.VISIBLE);
            } else if (!visible && grip.getVisibility() != View.GONE) {
                grip.setVisibility(View.GONE);
            }
        }
    }

    private boolean clockResizeHandlesFramePending = false;
    private final Runnable clockResizeHandlesFrame = () -> {
        clockResizeHandlesFramePending = false;
        updateClockResizeHandles();
    };

    private void scheduleClockResizeHandlesUpdate() {
        if (clockResizeHandlesFramePending) return;
        clockResizeHandlesFramePending = true;
        View target = clockGesturePanel != null ? clockGesturePanel : clockPanel;
        if (target == null) {
            clockResizeHandlesFramePending = false;
            updateClockResizeHandles();
            return;
        }
        target.postOnAnimation(clockResizeHandlesFrame);
    }

    private int hitClockResizeEdge(float rawX, float rawY) {
        if (clockPanel == null || clockPanel.getWidth() <= 0 || clockPanel.getHeight() <= 0) return 0;
        // Use a forgiving hit target around each visible grip. The grips remain visually
        // narrow, but a small finger-placement error must not turn a resize into a body drag.
        // At corners the top/bottom grips retain priority, preventing axis misclassification.
        float margin = LauncherUiUtils.dp(this, 12);
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
        // Android 6–12 use the legacy callback; Android 13+ use the registered
        // OnBackInvokedCallback above so targetSdk 37 keeps the same launcher behavior.
        handleBackNavigation();
    }

    private void handleBackNavigation() {
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

    // Isolate API-33-only types from MainActivity's fields and ordinary API-23 code paths.
    private static final class Api33BackDispatcher {
        @android.annotation.TargetApi(33)
        static Object register(MainActivity activity) {
            android.window.OnBackInvokedCallback callback = activity::handleBackNavigation;
            activity.getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT, callback);
            return callback;
        }

        @android.annotation.TargetApi(33)
        static void unregister(MainActivity activity, Object callback) {
            activity.getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(
                    (android.window.OnBackInvokedCallback) callback);
        }
    }

    // Newer framework classes stay inside version-specific helpers. This matters on
    // API 23, where a type referenced directly from MainActivity can fail verification
    // even when the code path is guarded by an SDK_INT check.
    private static final class Api29HomeRoleManager {
        @android.annotation.TargetApi(29)
        static Intent createHomeRoleRequest(Activity activity) {
            Object service = activity.getSystemService("role");
            if (!(service instanceof android.app.role.RoleManager)) return null;
            android.app.role.RoleManager manager =
                    (android.app.role.RoleManager) service;
            if (!manager.isRoleAvailable(android.app.role.RoleManager.ROLE_HOME)
                    || manager.isRoleHeld(android.app.role.RoleManager.ROLE_HOME)) return null;
            return manager.createRequestRoleIntent(android.app.role.RoleManager.ROLE_HOME);
        }

        @android.annotation.TargetApi(29)
        static boolean isHomeRoleHeld(Activity activity) {
            Object service = activity.getSystemService("role");
            return service instanceof android.app.role.RoleManager
                    && ((android.app.role.RoleManager) service)
                            .isRoleHeld(android.app.role.RoleManager.ROLE_HOME);
        }
    }

    private static final class Api30WindowInsets {
        @android.annotation.TargetApi(30)
        static int[] systemBarsAndIme(android.view.WindowInsets insets) {
            android.graphics.Insets bars = insets.getInsets(
                    android.view.WindowInsets.Type.systemBars());
            android.graphics.Insets ime = insets.getInsets(
                    android.view.WindowInsets.Type.ime());
            return new int[]{bars.top, Math.max(bars.bottom, ime.bottom)};
        }
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
        // A hidden INVISIBLE drawer can measure and warm its first viewport without appearing
        // over Home. GONE prevents GridView from laying out cells, so restore the warm state
        // when we return to the launcher and persist the finished viewport from there.
        if (!drawerOpen && drawerScreen != null && homeScreen != null
                && homeScreen.getVisibility() == View.VISIBLE
                && drawerScreen.getVisibility() == View.GONE) {
            drawerScreen.setVisibility(View.INVISIBLE);
            drawerScreen.requestLayout();
            handler.postDelayed(this::scheduleDrawerArtworkPrewarm, 120L);
        }
        clockUpdatesActive = true;
        handler.removeCallbacks(clockUpdater);
        handler.post(clockUpdater);
        // The system may kill a launcher process during installation, so its dynamic
        // PACKAGE_REPLACED receiver is not guaranteed to run. Recover the standard
        // two-digit renderer on resume as well as from the package broadcast.
        handler.postDelayed(() -> {
            if (!clockUpdatesActive) return;
            for (int page = 0; page < clockPanelsByPage.length; page++) {
                scheduleClockRendererPostLayoutRecovery(page);
            }
        }, 120L);
        startClockColorAnimator();
        // Do not reconnect every AppWidgetHost synchronously inside onResume: on
        // Android 6 that can delay the first Home frame when returning from an app.
        // Let the cached workspace draw first, then resume provider listening.
        handler.postDelayed(() -> {
            if (!clockUpdatesActive || widgetHost == null) return;
            try { widgetHost.startListening(); } catch (Exception ignored) {}
        }, 500L);
        // Avoid a PackageManager scan on every resume. Install, removal, update, and
        // component-enabled broadcasts refresh the drawer through packageChangesReceiver.
        // Android 10+ uses ROLE_HOME as the authoritative default-launcher selection.
        // If that role was never granted or has been revoked, request it visibly;
        // Android does not allow a launcher to silently reclaim the role.
        ensureHomeRoleOnAndroid10Plus();
    }

    private void ensureHomeRoleOnAndroid10Plus() {
        if (Build.VERSION.SDK_INT < 29 || homeRoleRequestAttempted || isFinishing()) return;
        try {
            Intent roleRequest = Api29HomeRoleManager.createHomeRoleRequest(this);
            if (roleRequest == null) return;
            homeRoleRequestAttempted = true;
            Toast.makeText(this,
                    "Choose PhoneBridge as the Home app to keep it as your default launcher.",
                    Toast.LENGTH_LONG).show();
            startActivityForResult(roleRequest, REQUEST_HOME_ROLE);
        } catch (RuntimeException ignored) {
            // Some OEM builds restrict role UI integration. Keep the launcher usable.
            homeRoleRequestAttempted = true;
        }
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
        stopClockColorAnimator();
        try { widgetHost.stopListening(); } catch (Exception ignored) {}
        super.onPause();
    }

    @Override public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        LauncherIconDrawable.trimArtworkCaches(level);
    }

    @Override protected void onDestroy() {
        if (prefs != null) {
            prefs.unregisterOnSharedPreferenceChangeListener(drawerCachePreferenceListener);
        }
        cancelPendingLiveDisplacementUpdate();
        liveDragGeometryCache.clear();
        handler.removeCallbacksAndMessages(null);
        if (packageReceiverRegistered) {
            try { unregisterReceiver(packageChangesReceiver); } catch (IllegalArgumentException ignored) {}
            packageReceiverRegistered = false;
        }
        try { unregisterReceiver(timeChangesReceiver); } catch (IllegalArgumentException ignored) {}
        if (Build.VERSION.SDK_INT >= 33 && backInvokedCallback != null) {
            Api33BackDispatcher.unregister(this, backInvokedCallback);
            backInvokedCallback = null;
        }
        super.onDestroy();
    }

    private void loadApps() {
        Intent query = new Intent(Intent.ACTION_MAIN, null);
        query.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> result = packageManager.queryIntentActivities(query, 0);
        // PackageManager label loading is relatively expensive on older devices. Sort
        // by a single precomputed label per activity instead of loading both labels for
        // every comparator invocation (O(n log n) resource lookups).
        final java.util.IdentityHashMap<ResolveInfo, String> appLabels =
                new java.util.IdentityHashMap<>();
        preloadedAppLabels.clear();
        for (ResolveInfo app : result) {
            String label;
            try {
                label = String.valueOf(app.loadLabel(packageManager));
            } catch (RuntimeException ignored) {
                label = "";
            }
            appLabels.put(app, label);
            if (app.activityInfo != null) {
                preloadedAppLabels.put(app.activityInfo.packageName + "/" + app.activityInfo.name,
                        label);
            }
        }
        final boolean descending = prefs != null
                && prefs.getBoolean(KEY_DRAWER_DESCENDING, false);
        Collections.sort(result, new Comparator<ResolveInfo>() {
            @Override public int compare(ResolveInfo a, ResolveInfo b) {
                String labelA = appLabels.get(a);
                String labelB = appLabels.get(b);
                int order = (labelA == null ? "" : labelA)
                        .compareToIgnoreCase(labelB == null ? "" : labelB);
                return descending ? -order : order;
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
            drawerAdapter.seedLabelCache(preloadedAppLabels);
            // Re-apply the live search query, if present, against the newly queried package
            // list instead of allowing search state to restore a stale startup snapshot.
            if (refreshDrawerAppList != null) refreshDrawerAppList.run();
            else drawerAdapter.notifyDataSetChanged();
            scheduleDrawerArtworkPrewarm();
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
            int topInset;
            int bottomInset;
            if (Build.VERSION.SDK_INT >= 30) {
                int[] modernInsets = Api30WindowInsets.systemBarsAndIme(insets);
                topInset = modernInsets[0];
                bottomInset = modernInsets[1];
            } else {
                topInset = insets.getSystemWindowInsetTop();
                // Before API 30, the navigation bar remains outside the content
                // frame. Only compensate for the status bar on these versions.
                bottomInset = 0;
            }
            homeScreen.setPadding(
                    LauncherUiUtils.dp(this, 20),
                    LauncherUiUtils.dp(this, 14) + topInset,
                    LauncherUiUtils.dp(this, 20),
                    LauncherUiUtils.dp(this, 8) + bottomInset);
            if (drawerScreen != null) {
                int drawerSide = LauncherUiUtils.dp(this, 18);
                drawerScreen.setPadding(drawerSide, drawerSide + topInset, drawerSide,
                        LauncherUiUtils.dp(this, 8) + bottomInset);
            }
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
        showHomeImmediately();
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
            // Build page 2's initial hardware texture after the first workspace frame,
            // rather than paying that cost on the final frames of the first forward swipe.
            root.postDelayed(() -> {
                if (isFinishing() || pagesStrip == null || pagesStrip.getChildCount() < 2) return;
                View secondPage = pagesStrip.getChildAt(1);
                secondPage.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                if (secondPage.isHardwareAccelerated()) secondPage.buildLayer();
            }, 280L);
        });
    }

    private void setWorkspaceGridVisible(boolean visible, View movingView) {
        workspaceMoveModeHighlightActive = visible;
        if (pagesStrip == null) {
            refreshAppGroupMoveModeHighlights();
            return;
        }
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
            refreshAppGroupMoveModeHighlights();
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
        refreshWorkspaceGrid();
        refreshAppGroupMoveModeHighlights();
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
            // More visible while moving, while remaining translucent over wallpaper.
            gridPaint.setColor(Color.argb(105, 88, 145, 255));
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
        // Handwritten launcher signature, with a lime-to-mint-to-teal gradient distinct
        // from the blue/violet Home Studio accent.
        brand.setTypeface(Typeface.create("cursive", Typeface.NORMAL));
        brand.setLetterSpacing(0.035f);
        brand.post(() -> {
            float textWidth = Math.max(1f, brand.getPaint().measureText(brand.getText().toString()));
            brand.getPaint().setShader(new android.graphics.LinearGradient(0f, 0f, textWidth, 0f,
                    new int[] {Color.rgb(190, 255, 92), Color.rgb(64, 232, 179),
                            Color.rgb(22, 135, 145)}, null, android.graphics.Shader.TileMode.CLAMP));
            brand.invalidate();
        });
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
        dateText = createClockDateTextView();
        dateText.setTypeface(Typeface.create(clockDateFontFamily(), Typeface.BOLD));
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
        // Do not cache the viewport and the complete two-page strip as nested
        // full-screen hardware layers. Each page gets its own temporary/cached layer
        // in preparePageLayersForSwipe()/releasePageLayers(); the extra parent textures
        // waste GPU memory on older devices and can worsen frame drops during 3D motion.
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
        return Math.max(8, Math.min(15,
                prefs == null ? 9 : prefs.getInt(KEY_HOME_FONT_SIZE, 9)));
    }

    private String homeLabelTypefaceFamily() {
        int family = prefs == null ? 0 : Math.max(0, Math.min(3,
                prefs.getInt(KEY_HOME_LABEL_FONT, 0)));
        switch (family) {
            case 1: return "serif";
            case 2: return "monospace";
            case 3: return "cursive";
            default: return "sans-serif";
        }
    }

    private int homeLabelColor() {
        int position = prefs == null ? 0 : Math.max(0, Math.min(1000,
                prefs.getInt(KEY_HOME_LABEL_COLOR, 0)));
        return LauncherIconDrawable.spectrumColorForPosition(position);
    }

    private int clockDateFillColor() {
        if (prefs == null || !prefs.contains(KEY_CLOCK_DATE_FILL_COLOR_POSITION)) {
            // Keep the existing default date fill until the user selects a new colour.
            return Color.rgb(238, 240, 244);
        }
        int position = Math.max(0, Math.min(1000,
                prefs.getInt(KEY_CLOCK_DATE_FILL_COLOR_POSITION, 0)));
        return LauncherIconDrawable.spectrumColorForPosition(position);
    }

    private String clockDateFontFamily() {
        int family = prefs == null ? 0 : Math.max(0, Math.min(3,
                prefs.getInt(KEY_CLOCK_DATE_FONT, 0)));
        switch (family) {
            case 1: return "sans-serif";
            case 2: return "serif";
            case 3: return "monospace";
            default: return "cursive";
        }
    }

    private String clockPmFontFamily() {
        int family = prefs == null ? 0 : Math.max(0, Math.min(3,
                prefs.getInt(KEY_CLOCK_PM_FONT, 0)));
        switch (family) {
            case 1: return "sans-serif";
            case 2: return "serif";
            case 3: return "monospace";
            default: return "sans-serif-black";
        }
    }

    private int homeLabelHeightDp() {
        float targetTextSize = homeFontSizeSp();
        android.graphics.Paint labelPaint = new android.graphics.Paint(
                android.graphics.Paint.ANTI_ALIAS_FLAG);
        labelPaint.setTypeface(Typeface.create(homeLabelTypefaceFamily(), Typeface.NORMAL));
        labelPaint.setTextSize(targetTextSize * getResources().getDisplayMetrics().scaledDensity);
        android.graphics.Paint.FontMetrics metrics = labelPaint.getFontMetrics();
        float density = Math.max(0.1f, getResources().getDisplayMetrics().density);
        int textHeightDp = (int) Math.ceil((metrics.descent - metrics.ascent) / density);
        return Math.max(12, textHeightDp + 2);
    }

    /**
     * Match the app-cell label sizing rule for launcher-owned shortcut labels. Use the actual
     * TextView paint after its selected size/typeface are applied so tall fonts are not clipped.
     */
    private int workspaceLabelHeightPx(TextView label) {
        if (label == null) return LauncherUiUtils.dp(this, homeLabelHeightDp());
        android.graphics.Paint.FontMetrics metrics = label.getPaint().getFontMetrics();
        int textHeightPx = (int) Math.ceil(metrics.descent - metrics.ascent);
        return Math.max(LauncherUiUtils.dp(this, 12),
                textHeightPx + LauncherUiUtils.dp(this, 2));
    }

    /**
     * Use the same native end-ellipsis behavior as app labels. Keep the full shortcut
     * title in the content description so Android can recalculate the ellipsis whenever
     * the label font or available width changes.
     */
    private void fitShortcutLabelWithDots(TextView label) {
        if (label == null) return;
        CharSequence source = label.getContentDescription();
        if (source == null) source = label.getText();
        final String fullText = source == null ? "" : source.toString();
        label.setSingleLine(true);
        label.setMaxLines(1);
        label.setEllipsize(null);
        if (label.getWidth() <= 0) return;
        float available = Math.max(0f, label.getWidth()
                - label.getPaddingLeft() - label.getPaddingRight());
        android.text.TextPaint paint = label.getPaint();
        String fitted = fullText;
        if (paint.measureText(fullText) > available) {
            String suffix = "..";
            while (suffix.length() > 0 && paint.measureText(suffix) > available) {
                suffix = suffix.substring(0, suffix.length() - 1);
            }
            int end = fullText.length();
            while (end > 0 && paint.measureText(fullText.substring(0, end) + suffix) > available) {
                end--;
            }
            fitted = fullText.substring(0, end) + suffix;
        }
        if (!fitted.contentEquals(label.getText())) label.setText(fitted);
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
     * Keep a 45dp breathing space between the label-enabled home grid and the dock.
     * With labels disabled, preserve the previous workspace capacity exactly.
     */
    private int homeGridBottomGapPx() {
        return isHomeGridBottomAligned() ? LauncherUiUtils.dp(this, 45) : 0;
    }

    /**
     * Keep the first row's origin stable while icon-size changes alter row height/capacity.
     * The reference size is captured from the user's existing layout once, so adopting this
     * row flow does not immediately jump an already-customized home screen on upgrade.
     */
    private int homeGridTopInsetPx(int pageHeightPx) {
        if (!isHomeGridBottomAligned()) return 0;
        int height = Math.max(1, pageHeightPx);
        int bottomGap = homeGridBottomGapPx();
        int currentSize = prefs == null ? 48 : prefs.getInt(KEY_ICON_SIZE, 48);
        int anchorSize = prefs == null ? currentSize
                : prefs.getInt(KEY_HOME_GRID_ANCHOR_ICON_SIZE, currentSize);
        anchorSize = Math.max(32, Math.min(74, anchorSize));
        int anchorCellHeightDp = prefs == null ? homeCellHeightDp(anchorSize)
                : prefs.getInt(KEY_HOME_GRID_ANCHOR_CELL_HEIGHT_DP, homeCellHeightDp(anchorSize));
        int anchorCellHeight = Math.max(1, LauncherUiUtils.dp(this,
                Math.max(1, anchorCellHeightDp)));
        int referenceRows = Math.max(1,
                Math.max(1, height - bottomGap) / anchorCellHeight);
        return Math.max(0, height - bottomGap - referenceRows * anchorCellHeight);
    }

    /** Returns row capacity after reserving the stable top origin and dock clearance. */
    private int homeGridRowsForHeight(int pageHeightPx, int cellHeightPx) {
        int usableHeight = Math.max(1,
                pageHeightPx - homeGridBottomGapPx() - homeGridTopInsetPx(pageHeightPx));
        return Math.max(1, usableHeight / Math.max(1, cellHeightPx));
    }

    /** Returns a row top from a fixed origin, like the app drawer's top-to-bottom grid. */
    private int homeGridRowTop(int row, int rowCount, int pageHeightPx, int cellHeightPx) {
        int rows = Math.max(1, rowCount);
        int cellHeight = Math.max(1, cellHeightPx);
        int safeRow = Math.max(0, Math.min(rows - 1, row));
        return homeGridTopInsetPx(pageHeightPx) + safeRow * cellHeight;
    }

    /** Maps a pixel top coordinate back to the nearest legal row for the current mode. */
    private int homeGridRowFromTop(float topPx, int rowCount, int pageHeightPx, int cellHeightPx) {
        int rows = Math.max(1, rowCount);
        int cellHeight = Math.max(1, cellHeightPx);
        int topInset = homeGridTopInsetPx(pageHeightPx);
        return Math.max(0, Math.min(rows - 1,
                Math.round((topPx - topInset) / (float) cellHeight)));
    }

    /**
     * Finds the legal grid slot with the greatest overlap of the dragged cell.
     * At least half of the dragged-cell area must enter the slot. Exact 50% ties
     * follow the drag direction, so drops behave consistently from every edge.
     */
    private int[] findHomeGridSlotForDragBoundary(int boundaryLeft, int boundaryTop,
            int boundaryWidth, int boundaryHeight, int pageWidth, int pageHeight,
            int columns, int rows, int cellWidth, int cellHeight,
            int horizontalDirection, int verticalDirection) {
        if (pageWidth <= 0 || pageHeight <= 0 || columns <= 0 || rows <= 0
                || cellWidth <= 0 || cellHeight <= 0) return null;

        int boundaryRight = boundaryLeft + Math.max(1, boundaryWidth);
        int boundaryBottom = boundaryTop + Math.max(1, boundaryHeight);
        long boundaryArea = (long) Math.max(1, boundaryWidth)
                * Math.max(1, boundaryHeight);
        long bestOverlapArea = 0L;
        int bestColumn = -1;
        int bestRow = -1;
        int bestDirectionScore = Integer.MIN_VALUE;

        // Test the actual legal grid rectangles, not a center point. Requiring at
        // least 50% of the dragged cell to lie in a slot makes the threshold equal
        // from left, right, top, and bottom, including when rows are bottom-aligned.
        for (int row = 0; row < rows; row++) {
            int gridTop = homeGridRowTop(row, rows, pageHeight, cellHeight);
            int gridBottom = Math.min(pageHeight, gridTop + cellHeight);
            for (int col = 0; col < columns; col++) {
                int gridLeft = col * cellWidth;
                int gridRight = Math.min(pageWidth, gridLeft + cellWidth);
                if (gridRight <= gridLeft || gridBottom <= gridTop) continue;
                int overlapWidth = Math.max(0,
                        Math.min(boundaryRight, gridRight) - Math.max(boundaryLeft, gridLeft));
                int overlapHeight = Math.max(0,
                        Math.min(boundaryBottom, gridBottom) - Math.max(boundaryTop, gridTop));
                long overlapArea = (long) overlapWidth * overlapHeight;
                if (overlapArea <= 0L || overlapArea * 2L < boundaryArea) continue;

                int directionScore = (horizontalDirection > 0 ? col
                        : horizontalDirection < 0 ? -col : 0)
                        + (verticalDirection > 0 ? row
                        : verticalDirection < 0 ? -row : 0);
                boolean better = overlapArea > bestOverlapArea;
                if (overlapArea == bestOverlapArea) {
                    better = directionScore > bestDirectionScore
                            || (directionScore == bestDirectionScore
                                && (row > bestRow || (row == bestRow && col > bestColumn)));
                }
                if (better) {
                    bestOverlapArea = overlapArea;
                    bestColumn = col;
                    bestRow = row;
                    bestDirectionScore = directionScore;
                }
            }
        }
        return bestColumn < 0 ? null : new int[]{bestColumn, bestRow};
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

    private SevenSegmentClockTextView createClockDateTextView() {
        SevenSegmentClockTextView view = new SevenSegmentClockTextView(this);
        // Reuse the renderer's established inner-contour drawing path in ordinary TextView mode.
        view.setSevenSegmentMode(false);
        view.setText("");
        view.setTextSize(13f);
        view.setTextColor(Color.WHITE);
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setIncludeFontPadding(false);
        view.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        view.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT);
        return view;
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
                clockGravity() | Gravity.CENTER_VERTICAL));
        panel.addView(row, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, LauncherUiUtils.dp(this, 114)));

        TextView date = createClockDateTextView();
        date.setTypeface(Typeface.create(clockDateFontFamily(), Typeface.BOLD));
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

    // Keep the Activity's preference reads on the same safe path used by the
    // page renderer and model, so legacy numeric types cannot crash restoration.
    private float getFloatPreferenceCompat(String key, float fallback) {
        return LauncherUiUtils.getFloatPreferenceCompat(prefs, key, fallback);
    }

    private void attachClockPanelsToPages() {
        if (pagesStrip == null) return;
        for (int pageIndex = 0; pageIndex < clockPanelsByPage.length; pageIndex++) {
            if (pageIndex >= pagesStrip.getChildCount()
                    || !(pagesStrip.getChildAt(pageIndex) instanceof FrameLayout)) continue;
            LinearLayout panel = clockPanelsByPage[pageIndex];
            if (panel == null) continue;
            FrameLayout page = (FrameLayout) pagesStrip.getChildAt(pageIndex);
            // Older builds persisted "auto-removed" clocks after a failed resize repair.
            // That state is obsolete: a layout refresh must always reattach and reveal the
            // page's clock instead of carrying a transient collision decision forward.
            String removedClockKey = KEY_AUTO_REMOVED_CLOCK_PAGE_PREFIX + pageIndex;
            if (prefs.getBoolean(removedClockKey, false)) {
                prefs.edit().remove(removedClockKey).apply();
            }
            panel.setVisibility(View.VISIBLE);
            panel.setAlpha(1f);
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
                    @Override public boolean canLaunchApp(View sourceView) {
                        return MainActivity.this.canLaunchAppFromSource(sourceView);
                    }
                    @Override public void launchApp(ResolveInfo app, View sourceView) {
                        MainActivity.this.launchApp(app, sourceView);
                    }
                    @Override public void showAppOptions(ResolveInfo app, boolean onHome) {
                        MainActivity.this.showAppOptions(app, onHome);
                    }
                    @Override public int getColumns() { return getDrawerColumns(); }
                    @Override public void onDrawerDataChanged() {
                        scheduleDrawerArtworkPrewarm();
                    }
                });
        drawerScreen = built.screen;
        drawerGrid = built.grid;
        drawerAdapter = built.adapter;
        refreshDrawerAppList = built.refreshAppList;
        drawerAdapter.seedLabelCache(preloadedAppLabels);
        // Start native icon loading and artwork generation for every drawer app while Home is
        // shown. The adapter releases each temporary waiter as its processed artwork is ready.
        drawerAdapter.prewarmAllDrawerArtwork();
        // The full per-app artwork cache is then prepared in private internal storage.
        scheduleDrawerArtworkPrewarm();
    }

    private void renderHomePages(boolean animate) {
        // Every completed UI refresh rebuilds the page model from saved icons/widgets,
        // compacts away all empty pages, and remaps persisted page indexes before drawing.
        // This also cleans up pages left behind by widget removal or cross-page moves.
        rebuildPages();
        if (homePageController != null) homePageController.renderHomePages(animate);
        renderAppGroups();
        renderSavedContactShortcut();
        ensureShortcutLabelsNotClipped();
        renderDock();
        attachClockPanelsToPages();
        // Validate after the controller's posted layout corrections and clock remeasurements.
        // This keeps icon-label, font, page-indicator, and other settings changes collision-safe.
        validateWorkspacePagesAfterLayout();
    }

    private static final String KEY_SHORTCUT_IDS = "contact_shortcut_ids";

    // App groups are launcher-owned grid items, persisted independently of homePackages.
    // Their members remain installed apps; adding/removing membership never uninstalls anything.
    private String appGroupKey(String field, String id) {
        return "home_app_group_" + field + "_" + id;
    }

    private boolean isAppGroupItem(View view) {
        return view != null && view.getTag() instanceof String
                && ((String) view.getTag()).startsWith(APP_GROUP_TAG_PREFIX);
    }

    private String appGroupId(View view) {
        if (!isAppGroupItem(view)) return null;
        return ((String) view.getTag()).substring(APP_GROUP_TAG_PREFIX.length());
    }

    private ArrayList<String> getAppGroupIds() {
        ArrayList<String> ids = new ArrayList<String>();
        if (prefs == null) return ids;
        String stored = prefs.getString(KEY_APP_GROUP_IDS, "");
        if (stored != null && !stored.trim().isEmpty()) {
            for (String line : stored.split("\\n")) {
                String id = line.trim();
                if (!id.isEmpty() && !ids.contains(id)) ids.add(id);
            }
        }
        return ids;
    }

    private void saveAppGroupIds(List<String> ids) {
        if (prefs == null) return;
        ArrayList<String> unique = new ArrayList<String>();
        if (ids != null) {
            for (String id : ids) {
                if (id != null && !id.trim().isEmpty() && !unique.contains(id)) unique.add(id);
            }
        }
        prefs.edit().putString(KEY_APP_GROUP_IDS,
                android.text.TextUtils.join("\n", unique)).apply();
    }

    private ArrayList<String> getAppGroupMembers(String id) {
        ArrayList<String> members = new ArrayList<String>();
        if (prefs == null || id == null) return members;
        String stored = prefs.getString(appGroupKey("members", id), "");
        if (stored == null || stored.trim().isEmpty()) return members;
        for (String line : stored.split("\\n")) {
            String packageName = line.trim();
            if (!packageName.isEmpty() && !members.contains(packageName)) {
                members.add(packageName);
            }
        }
        return members;
    }

    private void saveAppGroupMembers(String id, List<String> members) {
        if (prefs == null || id == null) return;
        prefs.edit().putString(appGroupKey("members", id),
                android.text.TextUtils.join("\n", members)).apply();
    }

    private String getAppGroupLabel(String id) {
        if (prefs == null || id == null) return "App Group";
        String label = prefs.getString(appGroupKey("label", id), "App Group");
        return label == null || label.trim().isEmpty() ? "App Group" : label.trim();
    }

    private int getAppGroupPage(String id) {
        return HomePageModel.clampPage(prefs == null ? 0
                : prefs.getInt(appGroupKey("page", id), 0));
    }

    private int[] appGroupPalette() {
        int theme = LauncherIconDrawable.clampArtworkTheme(prefs == null
                ? LauncherIconDrawable.ARTWORK_ORIGINAL
                : prefs.getInt(KEY_ICON_ARTWORK_THEME, LauncherIconDrawable.ARTWORK_ORIGINAL));
        int[] palette = LauncherIconDrawable.clockTransitionPaletteForArtwork(theme);
        if (palette == null || palette.length < 2) {
            return new int[]{Color.rgb(95, 113, 160), Color.rgb(35, 48, 77),
                    Color.rgb(16, 20, 34)};
        }
        return palette.clone();
    }

    private GradientDrawable appGroupGradient(boolean round) {
        int[] palette = appGroupPalette();
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR, palette);
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setCornerRadius(LauncherUiUtils.dp(this, round ? 4f : 24f));
        return drawable;
    }

    private Drawable createAppGroupMemberDrawable(ResolveInfo app) {
        if (app == null || app.activityInfo == null) return null;
        Drawable source;
        try {
            source = app.loadIcon(packageManager);
        } catch (RuntimeException error) {
            return null;
        }
        if (source == null) return null;
        boolean showBackground = prefs.getBoolean("icon_rounded_square_background", false);
        int shape = LauncherIconDrawable.clampShape(
                prefs.getInt("app_icon_shape", LauncherIconDrawable.SHAPE_ORIGINAL));
        int backgroundColor = LauncherIconDrawable.backgroundColorFromPreferences(prefs);
        int theme = LauncherIconDrawable.clampArtworkTheme(
                prefs.getInt(KEY_ICON_ARTWORK_THEME, LauncherIconDrawable.ARTWORK_ORIGINAL));
        boolean fitInside = prefs.getBoolean(KEY_ICON_FIT_BACKGROUND, false);
        String token = app.activityInfo.packageName + "#app-group";
        return new LauncherIconDrawable(source, showBackground, shape, backgroundColor,
                theme, token, fitInside);
    }

    private FrameLayout createAppGroupLogo(String groupId, int iconSizePx) {
        FrameLayout logo = new FrameLayout(this);
        logo.setTag("launcher_app_group_logo");
        GradientDrawable logoBackground = appGroupGradient(true);
        // Match the expanded panel's 24dp corner radius at the icon's starting scale.
        // Keeping the same rounded-square silhouette avoids an oval-to-square morph.
        logoBackground.setCornerRadius(Math.max(1f, iconSizePx * 0.08f));
        logoBackground.setStroke(Math.max(1, LauncherUiUtils.dp(this, 1)), Color.BLACK);
        logo.setBackground(logoBackground);
        logo.setClipChildren(true);
        logo.setClipToPadding(true);
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            logo.setOutlineProvider(android.view.ViewOutlineProvider.BACKGROUND);
            logo.setClipToOutline(true);
        }
        ArrayList<String> members = getAppGroupMembers(groupId);
        int subSize = Math.max(LauncherUiUtils.dp(this, 10),
                Math.round(iconSizePx * 0.44f));
        int inset = Math.max(1, Math.round(iconSizePx * 0.04f));
        int drawn = 0;
        for (String packageName : members) {
            if (drawn >= 4) break;
            ResolveInfo app = findApp(packageName);
            if (app == null) continue;
            ImageView image = new ImageView(this);
            image.setScaleType(ImageView.ScaleType.FIT_CENTER);
            image.setImageDrawable(createAppGroupMemberDrawable(app));
            image.setContentDescription(String.valueOf(app.loadLabel(packageManager)));
            int gravity;
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    subSize, subSize, Gravity.TOP | Gravity.LEFT);
            if (drawn == 0) {
                gravity = Gravity.TOP | Gravity.LEFT;
                params.leftMargin = inset;
                params.topMargin = inset;
            } else if (drawn == 1) {
                gravity = Gravity.TOP | Gravity.RIGHT;
                params.rightMargin = inset;
                params.topMargin = inset;
            } else if (drawn == 2) {
                gravity = Gravity.BOTTOM | Gravity.LEFT;
                params.leftMargin = inset;
                params.bottomMargin = inset;
            } else {
                gravity = Gravity.BOTTOM | Gravity.RIGHT;
                params.rightMargin = inset;
                params.bottomMargin = inset;
            }
            params.gravity = gravity;
            logo.addView(image, params);
            drawn++;
        }
        if (drawn == 0) {
            TextView plus = new TextView(this);
            plus.setText("+");
            plus.setTextColor(Color.WHITE);
            plus.setTextSize(25f);
            plus.setGravity(Gravity.CENTER);
            plus.setIncludeFontPadding(false);
            logo.addView(plus, new FrameLayout.LayoutParams(-1, -1));
        }
        View moveOverlay = new View(this);
        moveOverlay.setTag("launcher_app_group_move_highlight");
        moveOverlay.setClickable(false);
        moveOverlay.setFocusable(false);
        moveOverlay.setBackground(appGroupMoveHighlight());
        moveOverlay.setVisibility(View.GONE);
        logo.addView(moveOverlay, new FrameLayout.LayoutParams(-1, -1));
        return logo;
    }

    private boolean isAppGroupAppearancePreference(String key) {
        return KEY_ICON_ARTWORK_THEME.equals(key)
                || KEY_ICON_BACKGROUND_COLOR.equals(key)
                || KEY_ICON_BACKGROUND_COLOR_POSITION.equals(key)
                || KEY_ICON_FIT_BACKGROUND.equals(key)
                || KEY_HOME_FONT_SIZE.equals(key)
                || KEY_HOME_LABEL_FONT.equals(key)
                || KEY_HOME_LABEL_COLOR.equals(key)
                || "icon_rounded_square_background".equals(key)
                || "app_icon_shape".equals(key)
                || "show_home_labels".equals(key);
    }

    private void removeDuplicateAppGroupViews(String groupId, View keep) {
        if (groupId == null || pagesStrip == null) return;
        String expectedTag = APP_GROUP_TAG_PREFIX + groupId;
        // Prefer the dragged view only while it is still attached. If a layout callback
        // has already rebuilt it, keep that rebuilt canonical instance instead of either
        // retaining two views or deleting every copy of the group.
        if (keep == null || keep.getParent() == null || !expectedTag.equals(keep.getTag())) {
            keep = findAppGroupView(groupId);
        }
        if (keep == null) return;
        for (int pageIndex = 0; pageIndex < pagesStrip.getChildCount(); pageIndex++) {
            View pageView = pagesStrip.getChildAt(pageIndex);
            if (!(pageView instanceof ViewGroup)) continue;
            ViewGroup page = (ViewGroup) pageView;
            for (int i = page.getChildCount() - 1; i >= 0; i--) {
                View candidate = page.getChildAt(i);
                if (candidate == keep || !expectedTag.equals(candidate.getTag())) continue;
                candidate.animate().cancel();
                page.removeViewAt(i);
            }
        }
    }

    private void renderAppGroups() {
        if (prefs == null || pagesStrip == null || pageViewport == null) return;
        // A full home refresh can be triggered by a settings/layout callback during a
        // long-press drag. Rebuilding groups then detaches the active touch target and
        // can leave an old and new instance visible. Defer the group pass until release.
        if (workspaceTransferItem != null && isAppGroupItem(workspaceTransferItem)) {
            appGroupRenderDeferred = true;
            return;
        }
        appGroupRenderDeferred = false;
        if (pageViewport.getWidth() <= 0 || pageViewport.getHeight() <= 0) {
            pageViewport.post(this::renderAppGroups);
            return;
        }
        for (int pageIndex = 0; pageIndex < pagesStrip.getChildCount(); pageIndex++) {
            View pageView = pagesStrip.getChildAt(pageIndex);
            if (!(pageView instanceof ViewGroup)) continue;
            ViewGroup page = (ViewGroup) pageView;
            for (int i = page.getChildCount() - 1; i >= 0; i--) {
                View child = page.getChildAt(i);
                if (isAppGroupItem(child)) {
                    child.animate().cancel();
                    page.removeViewAt(i);
                }
            }
        }

        ArrayList<String> ids = getAppGroupIds();
        int columns = Math.max(1, getHomeColumns());
        int fallbackCellWidth = Math.max(1, pageViewport.getWidth() / columns);
        int iconSizeDp = Math.max(24, Math.min(84, prefs.getInt(KEY_ICON_SIZE, 48)));
        int iconSizePx = LauncherUiUtils.dp(this, iconSizeDp);
        int cellHeight = LauncherUiUtils.dp(this, homeCellHeightDp(iconSizeDp));
        for (String id : ids) {
            int pageIndex = getAppGroupPage(id);
            if (pageIndex >= pagesStrip.getChildCount()
                    || !(pagesStrip.getChildAt(pageIndex) instanceof FrameLayout)) continue;
            FrameLayout page = (FrameLayout) pagesStrip.getChildAt(pageIndex);
            int actualPageWidth = page.getWidth() > 0 ? page.getWidth() : pageViewport.getWidth();
            int cellWidth = Math.max(1, actualPageWidth > 0
                    ? actualPageWidth / columns : fallbackCellWidth);
            int rows = homeGridRowsForHeight(page.getHeight() > 0
                    ? page.getHeight() : pageViewport.getHeight(), cellHeight);
            boolean hasX = prefs.contains(appGroupKey("x", id));
            boolean hasY = prefs.contains(appGroupKey("y", id));
            int col, row;
            if (hasX && hasY) {
                col = Math.max(0, Math.min(columns - 1, Math.round(
                        prefs.getFloat(appGroupKey("x", id), 0f)
                                * Math.max(1, columns - 1))));
                row = Math.max(0, Math.min(rows - 1, Math.round(
                        prefs.getFloat(appGroupKey("y", id), 0f)
                                * Math.max(1, rows - 1))));
            } else {
                int[] free = findFreeAppGroupSlot(page, null);
                if (free == null) free = new int[]{0, Math.max(0, rows - 1)};
                col = Math.max(0, Math.min(columns - 1, free[0]));
                row = Math.max(0, Math.min(rows - 1, free[1]));
                prefs.edit().putInt(appGroupKey("page", id), pageIndex)
                        .putFloat(appGroupKey("x", id), col / (float) Math.max(1, columns - 1))
                        .putFloat(appGroupKey("y", id), row / (float) Math.max(1, rows - 1))
                        .apply();
            }

            // Large configured artwork must not extend into neighbouring grid cells.
            // Keep the logical slot unchanged and fit only the visible logo to that cell.
            int visibleGroupIconSizePx = Math.max(1, Math.min(iconSizePx,
                    cellWidth - LauncherUiUtils.dp(this, 4)));
            FrameLayout cell = new FrameLayout(this);
            cell.setTag(APP_GROUP_TAG_PREFIX + id);
            cell.setContentDescription("App group: " + getAppGroupLabel(id));
            cell.setClipChildren(false);
            cell.setClipToPadding(false);
            int iconTop = LauncherUiUtils.dp(this, 14);
            FrameLayout logo = createAppGroupLogo(id, visibleGroupIconSizePx);
            FrameLayout.LayoutParams logoParams = new FrameLayout.LayoutParams(
                    visibleGroupIconSizePx, visibleGroupIconSizePx,
                    Gravity.TOP | Gravity.CENTER_HORIZONTAL);
            logoParams.topMargin = iconTop;
            cell.addView(logo, logoParams);

            TextView label = new TextView(this);
            label.setText(getAppGroupLabel(id));
            label.setTextColor(homeLabelColor());
            label.setTypeface(Typeface.create(homeLabelTypefaceFamily(), Typeface.NORMAL));
            label.setTextSize(homeFontSizeSp());
            label.setGravity(Gravity.CENTER);
            label.setSingleLine(true);
            label.setMaxLines(1);
            label.setEllipsize(android.text.TextUtils.TruncateAt.END);
            label.setIncludeFontPadding(false);
            label.setShadowLayer(LauncherUiUtils.dp(this, 1), 0f,
                    LauncherUiUtils.dp(this, 1), Color.argb(180, 0, 0, 0));
            int labelHeight = workspaceLabelHeightPx(label);
            FrameLayout.LayoutParams labelParams = new FrameLayout.LayoutParams(
                    cellWidth, labelHeight, Gravity.TOP | Gravity.LEFT);
            labelParams.topMargin = iconTop + visibleGroupIconSizePx
                    + LauncherUiUtils.dp(this, 1);
            cell.addView(label, labelParams);
            label.setVisibility(prefs.getBoolean("show_home_labels", true)
                    ? View.VISIBLE : View.GONE);

            FrameLayout.LayoutParams cellParams = new FrameLayout.LayoutParams(
                    cellWidth, cellHeight, Gravity.TOP | Gravity.LEFT);
            cellParams.leftMargin = col * cellWidth;
            cellParams.topMargin = homeGridRowTop(row, rows, page.getHeight(), cellHeight);
            page.addView(cell, cellParams);
            installAppGroupTouch(cell, id);
        }
        // Preserve target highlight when a layout/style update rebuilds group cells.
        refreshAppGroupMoveModeHighlights();
    }

    private int[] findFreeAppGroupSlot(FrameLayout page, View ignored) {
        if (page == null || page.getWidth() <= 0 || page.getHeight() <= 0) return null;
        int columns = Math.max(1, getHomeColumns());
        int width = Math.max(1, page.getWidth());
        int cellWidth = Math.max(1, width / columns);
        int iconDp = Math.max(24, Math.min(84, prefs.getInt(KEY_ICON_SIZE, 48)));
        int cellHeight = LauncherUiUtils.dp(this, homeCellHeightDp(iconDp));
        int rows = homeGridRowsForHeight(page.getHeight(), cellHeight);
        for (int row = 0; row < rows; row++) {
            int top = homeGridRowTop(row, rows, page.getHeight(), cellHeight);
            for (int col = 0; col < columns; col++) {
                android.graphics.Rect candidate = new android.graphics.Rect(
                        col * cellWidth, top, Math.min(width, (col + 1) * cellWidth),
                        Math.min(page.getHeight(), top + cellHeight));
                boolean blocked = false;
                for (int i = 0; i < page.getChildCount(); i++) {
                    View child = page.getChildAt(i);
                    if (child == ignored || !isWorkspaceItem(child)
                            || child.getVisibility() != View.VISIBLE) continue;
                    if (isWorkspaceAppIcon(child) || isAppGroupItem(child)) {
                        if (!(child.getLayoutParams() instanceof FrameLayout.LayoutParams)) continue;
                        FrameLayout.LayoutParams lp =
                                (FrameLayout.LayoutParams) child.getLayoutParams();
                        int otherCol = Math.max(0, Math.min(columns - 1,
                                Math.round(lp.leftMargin / (float) cellWidth)));
                        int otherRow = homeGridRowFromTop(lp.topMargin, rows,
                                page.getHeight(), cellHeight);
                        if (otherCol == col && otherRow == row) {
                            blocked = true;
                            break;
                        }
                    } else {
                        int[] geometry = itemGeometry(child);
                        if (geometry[2] <= 0 || geometry[3] <= 0) continue;
                        android.graphics.Rect obstacle = collisionRect(child, geometry);
                        if (android.graphics.Rect.intersects(candidate, obstacle)) {
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

    private void createAppGroup() {
        if (appGroupCreationPending) return;
        if (pageViewport == null || pageViewport.getWidth() <= 0
                || pageViewport.getHeight() <= 0 || pagesStrip == null) {
            appGroupCreationPending = true;
            Runnable retry = () -> {
                appGroupCreationPending = false;
                createAppGroup();
            };
            if (pageViewport != null) pageViewport.post(retry);
            else if (root != null) root.post(retry);
            else appGroupCreationPending = false;
            return;
        }
        long now = android.os.SystemClock.uptimeMillis();
        if (now - lastAppGroupCreationUptime < 500L) return;
        lastAppGroupCreationUptime = now;
        appGroupCreationPending = true;
        try {
            int desiredPage = Math.max(0, Math.min(1, currentPage));
            int chosenPage = -1;
            int[] slot = null;
            for (int attempt = 0; attempt < 2; attempt++) {
                int pageIndex = (desiredPage + attempt) % 2;
                if (pageIndex >= pagesStrip.getChildCount()
                        || !(pagesStrip.getChildAt(pageIndex) instanceof FrameLayout)) continue;
                FrameLayout page = (FrameLayout) pagesStrip.getChildAt(pageIndex);
                slot = findFreeAppGroupSlot(page, null);
                if (slot != null) {
                    chosenPage = pageIndex;
                    break;
                }
            }
            if (chosenPage < 0 || slot == null) {
                Toast.makeText(this, "Both home screens are full. Move an item first.",
                        Toast.LENGTH_LONG).show();
                return;
            }
            String id = "g" + java.util.UUID.randomUUID().toString().replace("-", "");
            ArrayList<String> ids = getAppGroupIds();
            if (!ids.contains(id)) ids.add(id);
            int columns = Math.max(1, getHomeColumns());
            int height = Math.max(1, ((View) pagesStrip.getChildAt(chosenPage)).getHeight());
            int cellHeight = LauncherUiUtils.dp(this,
                    homeCellHeightDp(Math.max(24, Math.min(84, prefs.getInt(KEY_ICON_SIZE, 48)))));
            int rows = homeGridRowsForHeight(height, cellHeight);
            prefs.edit()
                    .putInt(appGroupKey("page", id), chosenPage)
                    .putFloat(appGroupKey("x", id), slot[0] / (float) Math.max(1, columns - 1))
                    .putFloat(appGroupKey("y", id), slot[1] / (float) Math.max(1, rows - 1))
                    .putString(appGroupKey("label", id), "App Group")
                    .putString(appGroupKey("members", id), "")
                    .apply();
            saveAppGroupIds(ids);
            currentPage = chosenPage;
            renderHomePages(false);
            showPage(chosenPage, false);
            Toast.makeText(this, "App group added", Toast.LENGTH_SHORT).show();
        } finally {
            appGroupCreationPending = false;
        }
    }

    private String findAppGroupAtDragBoundary(FrameLayout page,
            int boundaryLeft, int boundaryTop, int boundaryWidth, int boundaryHeight,
            int cellWidth, int cellHeight) {
        if (page == null || cellWidth <= 0 || cellHeight <= 0) return null;
        int pageWidth = page.getWidth();
        int pageHeight = page.getHeight();
        int columns = Math.max(1, getHomeColumns());
        int rows = homeGridRowsForHeight(pageHeight, cellHeight);
        int boundaryRight = boundaryLeft + Math.max(1, boundaryWidth);
        int boundaryBottom = boundaryTop + Math.max(1, boundaryHeight);
        for (int i = 0; i < page.getChildCount(); i++) {
            View child = page.getChildAt(i);
            if (!isAppGroupItem(child)
                    || !(child.getLayoutParams() instanceof FrameLayout.LayoutParams)) continue;
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) child.getLayoutParams();
            int groupCol = Math.max(0, Math.min(columns - 1,
                    Math.round(lp.leftMargin / (float) Math.max(1, cellWidth))));
            int groupRow = homeGridRowFromTop(lp.topMargin, rows, pageHeight, cellHeight);
            int gridLeft = groupCol * cellWidth;
            int gridTop = homeGridRowTop(groupRow, rows, pageHeight, cellHeight);
            int gridRight = Math.min(pageWidth, gridLeft + cellWidth);
            int gridBottom = Math.min(pageHeight, gridTop + cellHeight);
            int overlapWidth = Math.max(0, Math.min(boundaryRight, gridRight)
                    - Math.max(boundaryLeft, gridLeft));
            int overlapHeight = Math.max(0, Math.min(boundaryBottom, gridBottom)
                    - Math.max(boundaryTop, gridTop));
            long overlapArea = (long) overlapWidth * overlapHeight;
            long boundaryArea = (long) Math.max(1, boundaryRight - boundaryLeft)
                    * Math.max(1, boundaryBottom - boundaryTop);
            if (boundaryArea > 0L && overlapArea * 2L >= boundaryArea) {
                return appGroupId(child);
            }
        }
        return null;
    }

    private String findAppGroupAtGridSlot(FrameLayout page, int col, int row,
            int cellWidth, int cellHeight) {
        if (page == null) return null;
        int rows = homeGridRowsForHeight(page.getHeight(), cellHeight);
        int columns = Math.max(1, getHomeColumns());
        for (int i = 0; i < page.getChildCount(); i++) {
            View child = page.getChildAt(i);
            if (!isAppGroupItem(child)
                    || !(child.getLayoutParams() instanceof FrameLayout.LayoutParams)) continue;
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) child.getLayoutParams();
            int groupCol = Math.max(0, Math.min(columns - 1,
                    Math.round(lp.leftMargin / (float) Math.max(1, cellWidth))));
            int groupRow = homeGridRowFromTop(lp.topMargin, rows, page.getHeight(), cellHeight);
            if (groupCol == col && groupRow == row) return appGroupId(child);
        }
        return null;
    }

    private void moveHomeAppIntoGroup(String groupId, String packageName) {
        if (groupId == null || packageName == null || findApp(packageName) == null) return;
        ArrayList<String> members = getAppGroupMembers(groupId);
        if (members.contains(packageName)) {
            Toast.makeText(this, "This app is already in the group.", Toast.LENGTH_SHORT).show();
            return;
        }
        members.add(packageName);
        saveAppGroupMembers(groupId, members);
        // A group is a secondary collection; the standalone Home icon and its saved
        // coordinates remain untouched when membership is added.
        renderHomePages(false);
        Toast.makeText(this, "Added to " + getAppGroupLabel(groupId), Toast.LENGTH_SHORT).show();
    }

    private void addPackagesToAppGroup(String groupId, List<String> packageNames) {
        if (groupId == null || packageNames == null) return;
        ArrayList<String> members = getAppGroupMembers(groupId);
        boolean changed = false;
        for (String packageName : packageNames) {
            if (packageName == null || packageName.trim().isEmpty()
                    || findApp(packageName) == null || members.contains(packageName)) continue;
            members.add(packageName);
            changed = true;
        }
        if (!changed) {
            Toast.makeText(this, "Those apps are already in this group.", Toast.LENGTH_SHORT).show();
            return;
        }
        saveAppGroupMembers(groupId, members);
        renderHomePages(false);
    }

    private void restoreGroupedAppsToHome(int requestedPage, List<String> packageNames) {
        if (packageNames == null || packageNames.isEmpty()) return;
        int restored = 0;
        for (String packageName : packageNames) {
            if (packageName == null || packageName.isEmpty() || findApp(packageName) == null
                    || homePackages.contains(packageName) || dockPackages.contains(packageName)) continue;
            int chosenPage = -1;
            int[] slot = null;
            for (int attempt = 0; attempt < 2; attempt++) {
                int pageIndex = (Math.max(0, Math.min(1, requestedPage)) + attempt) % 2;
                slot = findFreeHomeAppSlot(pageIndex);
                if (slot != null) {
                    chosenPage = pageIndex;
                    break;
                }
            }
            if (chosenPage < 0 || slot == null) continue;
            int columns = getHomeColumns();
            int rows = getHomeGridRows();
            homePackages.add(packageName);
            prefs.edit().putInt(KEY_ICON_PAGE + packageName, chosenPage)
                    .putFloat(KEY_ICON_X + packageName,
                            slot[0] / (float) Math.max(1, columns - 1))
                    .putFloat(KEY_ICON_Y + packageName,
                            slot[1] / (float) Math.max(1, rows - 1)).apply();
            restored++;
        }
        persistHomeApps();
        rebuildPages();
        renderHomePages(false);
        if (restored < packageNames.size()) {
            Toast.makeText(this, "Some apps remain in the app drawer because Home is full.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void showAppGroupMemberPicker(String groupId, boolean removeMode) {
        ArrayList<String> currentMembers = getAppGroupMembers(groupId);
        ArrayList<ResolveInfo> choices = new ArrayList<ResolveInfo>();
        if (removeMode) {
            for (String packageName : currentMembers) {
                ResolveInfo app = findApp(packageName);
                if (app != null) choices.add(app);
            }
        } else {
            for (ResolveInfo app : allApps) {
                if (app == null || app.activityInfo == null
                        || app.activityInfo.packageName.equals(getPackageName())
                        || currentMembers.contains(app.activityInfo.packageName)) continue;
                choices.add(app);
            }
        }
        if (choices.isEmpty()) {
            Toast.makeText(this, removeMode ? "This group has no removable apps."
                    : "No more apps are available to add.", Toast.LENGTH_SHORT).show();
            return;
        }
        CharSequence[] labels = new CharSequence[choices.size()];
        boolean[] checked = new boolean[choices.size()];
        for (int i = 0; i < choices.size(); i++) {
            ResolveInfo app = choices.get(i);
            labels[i] = String.valueOf(app.loadLabel(packageManager));
            checked[i] = false;
        }
        ArrayList<String> selected = new ArrayList<String>();
        new android.app.AlertDialog.Builder(this)
                .setTitle(removeMode ? "Remove apps" : "Add apps")
                .setMultiChoiceItems(labels, checked, (dialog, which, isChecked) -> {
                    String packageName = choices.get(which).activityInfo.packageName;
                    if (isChecked) {
                        if (!selected.contains(packageName)) selected.add(packageName);
                    } else {
                        selected.remove(packageName);
                    }
                })
                .setNegativeButton("Cancel", null)
                .setPositiveButton(removeMode ? "Remove" : "Add", (dialog, which) -> {
                    if (selected.isEmpty()) return;
                    if (!removeMode) {
                        addPackagesToAppGroup(groupId, selected);
                    } else {
                        ArrayList<String> members = getAppGroupMembers(groupId);
                        members.removeAll(selected);
                        saveAppGroupMembers(groupId, members);
                        int groupPage = getAppGroupPage(groupId);
                        restoreGroupedAppsToHome(groupPage, selected);
                    }
                }).show();
    }

    private void showAppGroupOptions(String groupId) {
        if (groupId == null || !getAppGroupIds().contains(groupId)) return;
        showActionDialog(getAppGroupLabel(groupId),
                new String[]{"+", "−", "✎", "↗", "×"},
                new String[]{"Add apps", "Remove apps", "Rename group",
                        "Share group", "Delete group"}, (dialog, which) -> {
            if (which == 0) {
                showAppGroupMemberPicker(groupId, false);
            } else if (which == 1) {
                showAppGroupMemberPicker(groupId, true);
            } else if (which == 2) {
                android.widget.EditText input = new android.widget.EditText(this);
                input.setSingleLine(true);
                input.setText(getAppGroupLabel(groupId));
                input.setSelection(input.length());
                new android.app.AlertDialog.Builder(this).setTitle("Rename group")
                        .setView(input).setNegativeButton("Cancel", null)
                        .setPositiveButton("Save", (renameDialog, button) -> {
                            String value = input.getText().toString().trim();
                            if (!value.isEmpty()) {
                                prefs.edit().putString(appGroupKey("label", groupId), value).apply();
                                renderHomePages(false);
                            }
                        }).show();
            } else if (which == 3) {
                shareAppGroup(groupId);
            } else if (which == 4) {
                new android.app.AlertDialog.Builder(this)
                        .setTitle("Delete app group")
                        .setMessage("Delete this group? Its apps will be returned to Home where space is available.")
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Delete", (confirm, button) -> deleteAppGroup(groupId))
                        .show();
            }
        });
    }

    private void shareAppGroup(String groupId) {
        ArrayList<String> members = getAppGroupMembers(groupId);
        StringBuilder text = new StringBuilder();
        text.append("D.S Launcher app group: ").append(getAppGroupLabel(groupId)).append("\n");
        if (members.isEmpty()) {
            text.append("No apps in this group.");
        } else {
            for (String packageName : members) {
                ResolveInfo app = findApp(packageName);
                text.append("• ").append(app == null ? packageName
                        : String.valueOf(app.loadLabel(packageManager)))
                        .append(" (").append(packageName).append(")\n");
            }
        }
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_SUBJECT, getAppGroupLabel(groupId));
        send.putExtra(Intent.EXTRA_TEXT, text.toString());
        try {
            startActivity(Intent.createChooser(send, "Share app group"));
        } catch (RuntimeException error) {
            Toast.makeText(this, "Unable to share this group", Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteAppGroup(String groupId) {
        if (groupId == null) return;
        ArrayList<String> members = getAppGroupMembers(groupId);
        int oldPage = getAppGroupPage(groupId);
        ArrayList<String> ids = getAppGroupIds();
        ids.remove(groupId);
        saveAppGroupIds(ids);
        prefs.edit().remove(appGroupKey("members", groupId))
                .remove(appGroupKey("label", groupId))
                .remove(appGroupKey("page", groupId))
                .remove(appGroupKey("x", groupId))
                .remove(appGroupKey("y", groupId))
                .remove("workspace_scale_x_" + APP_GROUP_TAG_PREFIX + groupId)
                .remove("workspace_scale_y_" + APP_GROUP_TAG_PREFIX + groupId)
                .remove("workspace_base_scale_x_" + APP_GROUP_TAG_PREFIX + groupId)
                .remove("workspace_base_scale_y_" + APP_GROUP_TAG_PREFIX + groupId).apply();
        for (int p = 0; pagesStrip != null && p < pagesStrip.getChildCount(); p++) {
            ViewGroup page = (ViewGroup) pagesStrip.getChildAt(p);
            for (int i = page.getChildCount() - 1; i >= 0; i--) {
                if ((APP_GROUP_TAG_PREFIX + groupId).equals(page.getChildAt(i).getTag())) {
                    page.removeViewAt(i);
                }
            }
        }
        restoreGroupedAppsToHome(oldPage, members);
    }

    private void openAppGroup(String groupId) {
        if (groupId == null || !getAppGroupIds().contains(groupId)) return;
        ArrayList<String> members = getAppGroupMembers(groupId);
        final View openingGroupIcon = findAppGroupLogoView(groupId);
        final View openingGroupCell = findAppGroupView(groupId);
        final float openingGroupCellAlpha = openingGroupCell == null
                ? 1f : openingGroupCell.getAlpha();
        final Dialog dialog = new Dialog(this);
        final boolean[] closing = new boolean[]{false};
        // [panel start scale X/Y, icon snapshot expanded scale X/Y].
        final float[] iconScale = new float[]{0.12f, 0.12f, 1f, 1f};
        final float[] morphStartTranslation = new float[2];
        final float[] morphProgress = new float[]{0f};
        final android.widget.ImageView[] morphSnapshotRef = new android.widget.ImageView[1];
        final android.animation.ValueAnimator[] morphAnimator = new android.animation.ValueAnimator[1];
        final ArrayList<ResolveInfo> pendingMemberApps = new ArrayList<>();
        final ArrayList<ImageView> pendingMemberIconViews = new ArrayList<>();
        final ArrayList<View> pendingMemberItemViews = new ArrayList<>();
        final boolean[] memberInteractionsEnabled = {false};
        final boolean[] memberArtworkReady = {true};
        final Drawable[][] loadedMemberDrawablesRef = new Drawable[1][];
        final boolean[] groupContentRevealStarted = {false};
        final Runnable[] revealGroupContents = new Runnable[1];
        final boolean[] transitionStartScheduled = {false};
        final Runnable[] startTransitionWhenReady = new Runnable[1];
        final android.view.ViewOutlineProvider morphOutlineProvider =
                new android.view.ViewOutlineProvider() {
            @Override public void getOutline(View view, android.graphics.Outline outline) {
                float width = view.getWidth();
                float height = view.getHeight();
                if (width <= 0f || height <= 0f) {
                    outline.setEmpty();
                    return;
                }
                // Keep one static rounded-square outline throughout the animation.
                // Invalidating a changing clip outline every frame forced expensive
                // redraws on Android 6 and made the popup morph look like separate steps.
                // Match the source group's 8% rounded-square corner radius exactly.
                // Using a fixed 24dp radius changed the icon silhouette as the panel grew.
                float radius = Math.min(width, height) * 0.08f;
                outline.setRoundRect(0, 0, Math.round(width), Math.round(height), radius);
            }
        };
        dialog.setOnDismissListener(ignored -> {
            if (openingGroupCell != null) openingGroupCell.setAlpha(openingGroupCellAlpha);
            if (groupId.equals(activeAppGroupMemberMoveModeId)) clearAppGroupMemberMoveMode();
        });

        // Use a transparent full-screen dialog host so the group panel can be placed
        // beside the icon instead of being forced into the centre by Dialog's default
        // window gravity. The dim layer owns outside taps; the content panel consumes
        // inside taps so member gestures remain unchanged.
        FrameLayout overlay = new FrameLayout(this);
        overlay.setClickable(true);
        overlay.setFocusableInTouchMode(true);
        overlay.setBackgroundColor(Color.TRANSPARENT);
        View dimLayer = new View(this);
        dimLayer.setBackgroundColor(Color.BLACK);
        dimLayer.setAlpha(0f);
        // Cache the full-screen dim surface so animating its alpha does not repaint
        // the dialog's entire software content on every animation tick.
        dimLayer.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        overlay.addView(dimLayer, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(LauncherUiUtils.dp(this, 14), LauncherUiUtils.dp(this, 12),
                LauncherUiUtils.dp(this, 14), LauncherUiUtils.dp(this, 12));
        GradientDrawable panelBackground = appGroupGradient(false);
        // The panel and its clipping outline use the same permanent rounded-square shape.
        panelBackground.setCornerRadius(LauncherUiUtils.dp(this, 24));
        panel.setBackground(panelBackground);
        panel.setScaleX(0.12f);
        panel.setScaleY(0.12f);
        // Keep the not-yet-positioned popup hidden for the first layout frame.
        panel.setAlpha(0f);
        panel.setPivotX(0f);
        panel.setPivotY(0f);
        panel.setOutlineProvider(morphOutlineProvider);
        panel.setClipToOutline(false);
        // Keep the transformed panel cached from its first visible frame through close.
        // Releasing the layer after opening forces another allocation on older GPUs.
        panel.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        panel.setClickable(true);
        panel.setOnClickListener(v -> { /* Consume blank-panel taps; only outside taps close. */ });
        // Keep app rows inside the rounded card. Their ScrollView is a separate
        // viewport below the fixed name/divider header.
        panel.setClipChildren(true);
        panel.setClipToPadding(true);

        TextView title = new TextView(this);
        title.setText(getAppGroupLabel(groupId));
        title.setTextColor(Color.WHITE);
        title.setTextSize(19f);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        title.setGravity(Gravity.CENTER);
        title.setMaxLines(1);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        title.setPadding(LauncherUiUtils.dp(this, 6), LauncherUiUtils.dp(this, 2),
                LauncherUiUtils.dp(this, 6), LauncherUiUtils.dp(this, 8));
        title.setAlpha(0f);
        title.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        panel.addView(title, new LinearLayout.LayoutParams(-1, -2));

        View groupDivider = new View(this);
        groupDivider.setAlpha(0f);
        groupDivider.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        groupDivider.setBackgroundColor(Color.argb(170, 255, 255, 255));
        panel.addView(groupDivider, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, LauncherUiUtils.dp(this, 1)));

        android.widget.ScrollView scroll = new android.widget.ScrollView(this);
        scroll.setFillViewport(false);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        // The separator stays fixed while the app rows scroll underneath it. Clip the
        // scrolling viewport and content so child icons/labels cannot paint over the line.
        scroll.setClipToPadding(true);
        scroll.setClipChildren(true);
        // Hidden during the panel morph; this smaller layer fades in after the morph finishes.
        // Deferring drawable binding prevents package-resource work invalidating its animation layer.
        scroll.setTag("app_group_content_fade");
        scroll.setAlpha(0f);
        scroll.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        LinearLayout rows = new LinearLayout(this);
        rows.setOrientation(LinearLayout.VERTICAL);
        rows.setClipChildren(true);
        rows.setClipToPadding(true);
        int iconSizeDp = Math.max(24, Math.min(84, prefs.getInt(KEY_ICON_SIZE, 48)));
        int miniIconPx = Math.max(LauncherUiUtils.dp(this, 18),
                Math.round(LauncherUiUtils.dp(this, iconSizeDp) * 0.8f));
        float miniLabelSp = Math.max(7f, homeFontSizeSp() * 0.8f);
        int rowCount = 0;
        LinearLayout row = null;
        for (String packageName : members) {
            ResolveInfo app = findApp(packageName);
            if (app == null) continue;
            if (row == null || rowCount % 3 == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
                row.setClipChildren(false);
                row.setClipToPadding(false);
                rows.addView(row, new LinearLayout.LayoutParams(-1, -2));
            }
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER_HORIZONTAL);
            item.setPadding(LauncherUiUtils.dp(this, 3), LauncherUiUtils.dp(this, 5),
                    LauncherUiUtils.dp(this, 3), LauncherUiUtils.dp(this, 5));
            // The enclosing panel already scales from the group icon. Keep member
            // transforms at identity so every icon is not doing a second scale animation.
            item.setScaleX(1f);
            item.setScaleY(1f);
            item.setEnabled(false);
            pendingMemberItemViews.add(item);
            ImageView icon = new ImageView(this);
            icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            // Artwork is loaded off the UI thread so the opening motion starts immediately.
            pendingMemberApps.add(app);
            pendingMemberIconViews.add(icon);
            item.addView(icon, new LinearLayout.LayoutParams(miniIconPx, miniIconPx));
            TextView label = new TextView(this);
            String memberLabel = null;
            if (app.activityInfo != null) {
                memberLabel = preloadedAppLabels.get(
                        app.activityInfo.packageName + "/" + app.activityInfo.name);
            }
            if (memberLabel == null || memberLabel.trim().isEmpty()) {
                memberLabel = app.activityInfo != null && app.activityInfo.name != null
                        ? app.activityInfo.name : packageName;
                int dot = memberLabel.lastIndexOf('.');
                if (dot >= 0 && dot + 1 < memberLabel.length()) {
                    memberLabel = memberLabel.substring(dot + 1);
                }
            }
            label.setText(memberLabel);
            label.setContentDescription(memberLabel);
            label.setTextSize(miniLabelSp);
            label.setTextColor(homeLabelColor());
            label.setTypeface(Typeface.create(homeLabelTypefaceFamily(), Typeface.NORMAL));
            label.setGravity(Gravity.CENTER);
            label.setSingleLine(true);
            label.setEllipsize(android.text.TextUtils.TruncateAt.END);
            label.setIncludeFontPadding(false);
            label.setMaxLines(1);
            label.setVisibility(prefs.getBoolean("show_home_labels", true) ? View.VISIBLE : View.GONE);
            item.addView(label, new LinearLayout.LayoutParams(-1,
                    LauncherUiUtils.dp(this, Math.max(11, Math.round(homeLabelHeightDp() * 0.8f)))));
            installAppGroupMemberTouch(item, icon, dialog, panel, groupId, packageName, app);
            row.addView(item, new LinearLayout.LayoutParams(0, -2, 1f));
            rowCount++;
        }
        if (rowCount == 0) {
            TextView empty = new TextView(this);
            empty.setText("No apps yet\nHold this group for 2 seconds to add apps.");
            empty.setTextColor(Color.WHITE);
            empty.setTextSize(14f);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(LauncherUiUtils.dp(this, 12), LauncherUiUtils.dp(this, 20),
                    LauncherUiUtils.dp(this, 12), LauncherUiUtils.dp(this, 20));
            rows.addView(empty, new LinearLayout.LayoutParams(-1, -2));
        } else if (row != null && row.getChildCount() < 3) {
            while (row.getChildCount() < 3) {
                View spacer = new View(this);
                row.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1f));
            }
        }
        scroll.addView(rows, new FrameLayout.LayoutParams(-1, -2));
        int targetRowHeight = miniIconPx
                + LauncherUiUtils.dp(this, Math.max(11, Math.round(homeLabelHeightDp() * 0.8f)))
                + LauncherUiUtils.dp(this, 18);
        int scrollHeight = Math.min(
                Math.max(LauncherUiUtils.dp(this, 100),
                        getResources().getDisplayMetrics().heightPixels * 2 / 3),
                Math.max(LauncherUiUtils.dp(this, 120), targetRowHeight * 2));
        panel.addView(scroll, new LinearLayout.LayoutParams(-1, scrollHeight));

        revealGroupContents[0] = () -> {
            if (!dialog.isShowing() || closing[0]
                    || !memberArtworkReady[0] || groupContentRevealStarted[0]) return;
            groupContentRevealStarted[0] = true;
            Drawable[] loaded = loadedMemberDrawablesRef[0];
            if (loaded != null) {
                int count = Math.min(loaded.length, pendingMemberIconViews.size());
                for (int i = 0; i < count; i++) {
                    ImageView iconView = pendingMemberIconViews.get(i);
                    if (iconView != null) iconView.setImageDrawable(loaded[i]);
                }
            }
            // Release the full-card texture only after motion stops. The scroll viewport's
            // smaller hardware layer can now fade independently without rebuilding the card.
            // Opening progress owns the grid fade; artwork binding must not start a second animation.
            scroll.buildLayer();
        };

        // Load member artwork off the UI thread. The main panel animation starts as soon
        // as layout is ready; the completed drawables are held until the morph has finished.
        if (!pendingMemberApps.isEmpty()) {
            memberArtworkReady[0] = false;
            final ResolveInfo[] appsToLoad = pendingMemberApps.toArray(new ResolveInfo[0]);
            Thread iconLoader = new Thread(() -> {
                final Drawable[] loaded = new Drawable[appsToLoad.length];
                for (int i = 0; i < appsToLoad.length; i++) {
                    try {
                        loaded[i] = createAppGroupMemberDrawable(appsToLoad[i]);
                    } catch (RuntimeException ignored) {
                        loaded[i] = null;
                    }
                }
                handler.post(() -> {
                    if (!dialog.isShowing() || isFinishing()) return;
                    loadedMemberDrawablesRef[0] = loaded;
                    memberArtworkReady[0] = true;
                    Runnable reveal = revealGroupContents[0];
                    if (reveal != null) reveal.run();
                });
            }, "LauncherAppGroupIcons");
            iconLoader.setDaemon(true);
            iconLoader.start();
        }

        final int panelWidthLimit = Math.max(LauncherUiUtils.dp(this, 240),
                Math.min(getResources().getDisplayMetrics().widthPixels
                        - LauncherUiUtils.dp(this, 32), LauncherUiUtils.dp(this, 380)));
        final int panelHeightLimit = Math.max(LauncherUiUtils.dp(this, 180),
                Math.min(getResources().getDisplayMetrics().heightPixels
                        - LauncherUiUtils.dp(this, 64),
                        scrollHeight + LauncherUiUtils.dp(this, 84)));
        // Folder content is a compact three-column grid. Use a square panel rather
        // than an elongated capsule; the ScrollView handles extra rows.
        final int panelSide = Math.max(1, Math.min(panelWidthLimit, panelHeightLimit));
        final int panelWidth = panelSide;
        final int panelHeight = panelSide;
        FrameLayout.LayoutParams panelParams = new FrameLayout.LayoutParams(
                panelWidth, panelHeight, Gravity.TOP | Gravity.LEFT);
        overlay.addView(panel, panelParams);
        panel.bringToFront();

        overlay.setOnClickListener(v -> closeAppGroupWithAnimation(
                dialog, panel, dimLayer, closing, iconScale, morphStartTranslation,
                morphProgress, morphSnapshotRef, morphAnimator, null));
        overlay.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == android.view.KeyEvent.KEYCODE_BACK
                    && event.getAction() == android.view.KeyEvent.ACTION_UP) {
                closeAppGroupWithAnimation(dialog, panel, dimLayer, closing, iconScale,
                        morphStartTranslation, morphProgress, morphSnapshotRef, morphAnimator,
                        null);
                return true;
            }
            return keyCode == android.view.KeyEvent.KEYCODE_BACK;
        });

        dialog.setContentView(overlay);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
        if (dialog.getWindow() != null) {
            android.view.Window window = dialog.getWindow();
            window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
            View decor = window.getDecorView();
            decor.setSystemUiVisibility(decor.getSystemUiVisibility()
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            window.setGravity(Gravity.TOP | Gravity.LEFT);
            window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
        }

        overlay.requestFocus();
        overlay.post(() -> {
            if (!dialog.isShowing() || panel.getParent() != overlay) return;
            int availableW = Math.max(1, overlay.getWidth());
            int availableH = Math.max(1, overlay.getHeight());
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) panel.getLayoutParams();
            int availableSide = Math.max(1, Math.min(panelSide,
                    Math.min(availableW - LauncherUiUtils.dp(this, 16),
                            availableH - LauncherUiUtils.dp(this, 16))));
            lp.width = availableSide;
            lp.height = availableSide;

            int iconX = availableW / 2;
            int iconY = availableH / 2;
            int iconW = LauncherUiUtils.dp(this, 64);
            int iconH = iconW;
            if (openingGroupIcon != null && openingGroupIcon.getWidth() > 0
                    && openingGroupIcon.getHeight() > 0) {
                int[] iconLocation = new int[2];
                int[] overlayLocation = new int[2];
                openingGroupIcon.getLocationOnScreen(iconLocation);
                overlay.getLocationOnScreen(overlayLocation);
                iconX = iconLocation[0] - overlayLocation[0] + openingGroupIcon.getWidth() / 2;
                iconY = iconLocation[1] - overlayLocation[1] + openingGroupIcon.getHeight() / 2;
                iconW = openingGroupIcon.getWidth();
                iconH = openingGroupIcon.getHeight();
            }

            int safe = LauncherUiUtils.dp(this, 8);
            int statusBarHeight = 0;
            int statusBarId = getResources().getIdentifier("status_bar_height", "dimen", "android");
            if (statusBarId > 0) {
                statusBarHeight = getResources().getDimensionPixelSize(statusBarId);
            }
            int navigationBarHeight = 0;
            int navigationBarId = getResources().getIdentifier("navigation_bar_height", "dimen", "android");
            if (navigationBarId > 0) {
                navigationBarHeight = getResources().getDimensionPixelSize(navigationBarId);
            }
            int safeTop = Math.min(availableH - 1, statusBarHeight + safe);
            int safeBottom = Math.max(safeTop + 1, availableH - navigationBarHeight - safe);
            int safeRight = Math.max(safe + 1, availableW - safe);
            int safeSquareSide = Math.max(1, Math.min(
                    Math.min(lp.width, lp.height),
                    Math.min(safeRight - safe, safeBottom - safeTop)));
            lp.width = safeSquareSide;
            lp.height = safeSquareSide;
            // The icon uses an 8% corner radius. Scale the panel's drawable radius by
            // the same ratio for every safe-area size, so opening/closing keeps one shape.
            panelBackground.setCornerRadius(Math.min(lp.width, lp.height) * 0.08f);
            /* no dynamic outline clipping */
            ViewGroup.LayoutParams safeScrollParams = scroll.getLayoutParams();
            safeScrollParams.height = Math.max(LauncherUiUtils.dp(this, 24),
                    Math.min(scrollHeight,
                            safeSquareSide - LauncherUiUtils.dp(this, 76)));
            scroll.setLayoutParams(safeScrollParams);

            int iconLeft = iconX - Math.max(1, iconW) / 2;
            int iconTop = iconY - Math.max(1, iconH) / 2;
            android.graphics.Rect iconBounds = new android.graphics.Rect(
                    iconLeft, iconTop, iconLeft + Math.max(1, iconW), iconTop + Math.max(1, iconH));
            int gap = LauncherUiUtils.dp(this, 10);
            int[] freeSpace = new int[]{
                    Math.max(0, iconBounds.top - safeTop - gap),
                    Math.max(0, safeBottom - iconBounds.bottom - gap),
                    Math.max(0, iconBounds.left - safe - gap),
                    Math.max(0, safeRight - iconBounds.right - gap)
            };
            // Prefer the side with the most room, but only use a full-size position if
            // the panel can fit there without touching an edge or covering its own icon.
            int[] directionOrder = new int[]{0, 1, 2, 3}; // above, below, left, right
            for (int i = 0; i < directionOrder.length; i++) {
                for (int j = i + 1; j < directionOrder.length; j++) {
                    if (freeSpace[directionOrder[j]] > freeSpace[directionOrder[i]]) {
                        int swap = directionOrder[i];
                        directionOrder[i] = directionOrder[j];
                        directionOrder[j] = swap;
                    }
                }
            }
            android.graphics.Rect panelBounds = null;
            int chosenDirection = -1;
            for (int direction : directionOrder) {
                int neededSpace = direction < 2 ? lp.height : lp.width;
                if (freeSpace[direction] < neededSpace) continue;
                int candidateLeft;
                int candidateTop;
                if (direction == 0) {
                    candidateLeft = Math.max(safe, Math.min(safeRight - lp.width,
                            iconX - lp.width / 2));
                    candidateTop = iconBounds.top - gap - lp.height;
                } else if (direction == 1) {
                    candidateLeft = Math.max(safe, Math.min(safeRight - lp.width,
                            iconX - lp.width / 2));
                    candidateTop = iconBounds.bottom + gap;
                } else if (direction == 2) {
                    candidateLeft = iconBounds.left - gap - lp.width;
                    candidateTop = Math.max(safeTop, Math.min(safeBottom - lp.height,
                            iconY - lp.height / 2));
                } else {
                    candidateLeft = iconBounds.right + gap;
                    candidateTop = Math.max(safeTop, Math.min(safeBottom - lp.height,
                            iconY - lp.height / 2));
                }
                android.graphics.Rect candidate = new android.graphics.Rect(candidateLeft,
                        candidateTop, candidateLeft + lp.width, candidateTop + lp.height);
                if (candidate.left < safe || candidate.top < safeTop
                        || candidate.right > safeRight || candidate.bottom > safeBottom
                        || android.graphics.Rect.intersects(candidate, iconBounds)) continue;
                panelBounds = candidate;
                chosenDirection = direction;
                break;
            }

            if (panelBounds == null) {
                // A tall group may not fit in any direction at its preferred size. Shrink
                // the panel along its placement axis and shorten its scroll area too, so
                // it remains entirely on-screen while leaving the launcher icon exposed.
                chosenDirection = directionOrder[0];
                int fallbackSquareSide = Math.max(1, Math.min(
                        Math.min(lp.width, lp.height), freeSpace[chosenDirection]));
                lp.width = fallbackSquareSide;
                lp.height = fallbackSquareSide;
                ViewGroup.LayoutParams scrollParams = scroll.getLayoutParams();
                int fixedHeight = LauncherUiUtils.dp(this, 76);
                scrollParams.height = Math.max(LauncherUiUtils.dp(this, 24),
                        Math.min(scrollHeight, lp.height - fixedHeight));
                scroll.setLayoutParams(scrollParams);
                int candidateLeft;
                int candidateTop;
                if (chosenDirection == 0) {
                    candidateLeft = Math.max(safe, Math.min(safeRight - lp.width,
                            iconX - lp.width / 2));
                    candidateTop = iconBounds.top - gap - lp.height;
                } else if (chosenDirection == 1) {
                    candidateLeft = Math.max(safe, Math.min(safeRight - lp.width,
                            iconX - lp.width / 2));
                    candidateTop = iconBounds.bottom + gap;
                } else if (chosenDirection == 2) {
                    candidateLeft = iconBounds.left - gap - lp.width;
                    candidateTop = Math.max(safeTop, Math.min(safeBottom - lp.height,
                            iconY - lp.height / 2));
                } else {
                    candidateLeft = iconBounds.right + gap;
                    candidateTop = Math.max(safeTop, Math.min(safeBottom - lp.height,
                            iconY - lp.height / 2));
                }
                panelBounds = new android.graphics.Rect(candidateLeft, candidateTop,
                        candidateLeft + lp.width, candidateTop + lp.height);
            }
            lp.leftMargin = panelBounds.left;
            lp.topMargin = panelBounds.top;
            panel.setLayoutParams(lp);

            if (openingGroupIcon != null && iconW > 0 && iconH > 0) {
                try {
                    android.graphics.Bitmap previewBitmap = android.graphics.Bitmap.createBitmap(
                            iconW, iconH, android.graphics.Bitmap.Config.ARGB_8888);
                    openingGroupIcon.draw(new android.graphics.Canvas(previewBitmap));
                    android.widget.ImageView preview = new android.widget.ImageView(this);
                    preview.setImageBitmap(previewBitmap);
                    preview.setScaleType(android.widget.ImageView.ScaleType.FIT_XY);
                    preview.setFocusable(false);
                    preview.setClickable(false);
                    preview.setEnabled(false);
                    preview.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
                    preview.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                    FrameLayout.LayoutParams previewParams = new FrameLayout.LayoutParams(
                            iconW, iconH, Gravity.TOP | Gravity.LEFT);
                    previewParams.leftMargin = iconLeft;
                    previewParams.topMargin = iconTop;
                    overlay.addView(preview, previewParams);
                    preview.bringToFront();
                    morphSnapshotRef[0] = preview;
                    iconScale[2] = 1f;
                    iconScale[3] = 1f;
                } catch (RuntimeException ignored) {
                    morphSnapshotRef[0] = null;
                }
            }

            float pivotX = Math.max(0f, Math.min(lp.width, iconX - panelBounds.left));
            float pivotY = Math.max(0f, Math.min(lp.height, iconY - panelBounds.top));
            panel.setPivotX(pivotX);
            panel.setPivotY(pivotY);
            // One common scale keeps the rounded-rectangle corner radius circular
            // throughout the morph. Separate X/Y scales stretched the rounded icon into
            // an ellipse while the panel grew from its square source.
            float startScale = Math.min(1f, Math.min(
                    iconW / (float) Math.max(1, lp.width),
                    iconH / (float) Math.max(1, lp.height)));
            float startScaleX = startScale;
            float startScaleY = startScale;
            float panelCenterX = panelBounds.left + lp.width * 0.5f;
            float panelCenterY = panelBounds.top + lp.height * 0.5f;
            // Final panel position is unchanged; translation simply begins with its centre
            // exactly over the original icon, including when the chosen side is left/right.
            morphStartTranslation[0] = iconX - panelCenterX;
            morphStartTranslation[1] = iconY - panelCenterY;
            panel.setPivotX(lp.width * 0.5f);
            panel.setPivotY(lp.height * 0.5f);
            panel.setScaleX(startScaleX);
            panel.setScaleY(startScaleY);
            panel.setTranslationX(morphStartTranslation[0]);
            panel.setTranslationY(morphStartTranslation[1]);
            panel.setAlpha(0f);
            panel.setOutlineProvider(morphOutlineProvider);
            panel.setClipToOutline(false);
            /* no dynamic outline clipping */
            iconScale[0] = startScaleX;
            iconScale[1] = startScaleY;
            morphProgress[0] = 0f;
            dimLayer.setAlpha(0f);

            // Keep the source icon visible and the panel hidden until member artwork and
            // child layout are both ready for a single continuous transition.
            panel.setLayerType(View.LAYER_TYPE_HARDWARE, null);

            final long duration = animationDuration(240L);
            android.animation.ValueAnimator transition =
                    android.animation.ValueAnimator.ofFloat(0f, 1f);
            transition.setDuration(duration);
            transition.setInterpolator(new android.view.animation.DecelerateInterpolator(1.2f));
            transition.addUpdateListener(animation -> {
                float progress = (Float) animation.getAnimatedValue();
                morphProgress[0] = progress;
                float sx = startScaleX + (1f - startScaleX) * progress;
                float sy = startScaleY + (1f - startScaleY) * progress;
                float tx = morphStartTranslation[0] * (1f - progress);
                float ty = morphStartTranslation[1] * (1f - progress);
                panel.setScaleX(sx);
                panel.setScaleY(sy);
                panel.setTranslationX(tx);
                panel.setTranslationY(ty);

                // Mini-icons fade out by the midpoint; title and grid fade in over the second half.
                float contentAlpha = Math.max(0f, Math.min(1f, (progress - 0.5f) * 2f));
                title.setAlpha(contentAlpha);
                groupDivider.setAlpha(contentAlpha);
                scroll.setAlpha(contentAlpha);
                android.widget.ImageView snapshot = morphSnapshotRef[0];
                if (snapshot != null) {
                    snapshot.setScaleX(1f);
                    snapshot.setScaleY(1f);
                    snapshot.setTranslationX(0f);
                    snapshot.setTranslationY(0f);
                    snapshot.setAlpha(1f - Math.min(1f, progress * 2f));
                    snapshot.setVisibility(progress >= 0.5f ? View.GONE : View.VISIBLE);
                }
                if (!memberInteractionsEnabled[0] && progress >= 0.9f) {
                    memberInteractionsEnabled[0] = true;
                    for (View memberItem : pendingMemberItemViews) memberItem.setEnabled(true);
                }
                // The area around the app-group card remains fully transparent.
                dimLayer.setAlpha(0f);
            });
            transition.addListener(new android.animation.AnimatorListenerAdapter() {
                @Override public void onAnimationEnd(android.animation.Animator animation) {
                    if (closing[0] || !dialog.isShowing()) return;
                    morphProgress[0] = 1f;
                    panel.setScaleX(1f);
                    panel.setScaleY(1f);
                    panel.setTranslationX(0f);
                    panel.setTranslationY(0f);
                    title.setAlpha(1f);
                    groupDivider.setAlpha(1f);
                    scroll.setAlpha(1f);
                    dimLayer.setAlpha(0f);
                    for (View memberItem : pendingMemberItemViews) memberItem.setEnabled(true);
                    memberInteractionsEnabled[0] = true;
                    android.widget.ImageView snapshot = morphSnapshotRef[0];
                    if (snapshot != null) {
                        snapshot.setAlpha(0f);
                        snapshot.setVisibility(View.GONE);
                    }
                    /* no dynamic outline clipping */
                    Runnable reveal = revealGroupContents[0];
                    if (reveal != null) reveal.run();
                }
            });
            morphAnimator[0] = transition;
            startTransitionWhenReady[0] = () -> {
                if (!dialog.isShowing() || closing[0]
                        || transitionStartScheduled[0] || transition.isStarted()) return;
                transitionStartScheduled[0] = true;
                panel.post(() -> {
                    if (!dialog.isShowing() || closing[0] || transition.isStarted()) {
                        transitionStartScheduled[0] = false;
                        return;
                    }
                    if (panel.getWidth() <= 0 || panel.getHeight() <= 0) {
                        transitionStartScheduled[0] = false;
                        overlay.postOnAnimation(startTransitionWhenReady[0]);
                        return;
                    }
                    // Warm the separately animated child surfaces; an offscreen parent layer
                    // would need rebuilding while child alpha changes and cause stepped fades.
                    panel.setAlpha(1f);
                    title.buildLayer();
                    groupDivider.buildLayer();
                    scroll.buildLayer();
                    panel.setLayerType(View.LAYER_TYPE_NONE, null);
                    panel.setAlpha(0f);
                    overlay.postOnAnimation(() -> {
                        if (!dialog.isShowing() || closing[0] || transition.isStarted()) {
                            transitionStartScheduled[0] = false;
                            return;
                        }
                        if (openingGroupCell != null) openingGroupCell.setAlpha(0f);
                        panel.setAlpha(1f);
                        transitionStartScheduled[0] = false;
                        transition.start();
                    });
                });
            };
            startTransitionWhenReady[0].run();
        });
    }

    private void closeAppGroupWithAnimation(Dialog dialog, View panel, View dimLayer,
            boolean[] closing, float[] iconScale, float[] startTranslation,
            float[] morphProgress, android.widget.ImageView[] snapshotRef,
            android.animation.ValueAnimator[] morphAnimator,
            Runnable afterClose) {
        if (dialog == null || !dialog.isShowing() || panel == null
                || closing == null || closing[0]) return;
        closing[0] = true;
        View fadingContent = panel.findViewWithTag("app_group_content_fade");
        if (fadingContent != null) fadingContent.animate().cancel();
        if (morphAnimator != null && morphAnimator.length > 0 && morphAnimator[0] != null) {
            morphAnimator[0].cancel();
        }
        float targetScaleX = iconScale != null ? iconScale[0] : 0.12f;
        float targetScaleY = iconScale != null ? iconScale[1] : targetScaleX;
        float offsetX = startTranslation != null ? startTranslation[0] : 0f;
        float offsetY = startTranslation != null ? startTranslation[1] : 0f;
        float initialProgress = morphProgress != null
                ? Math.max(0f, Math.min(1f, morphProgress[0])) : 1f;
        android.widget.ImageView snapshot = snapshotRef != null && snapshotRef.length > 0
                ? snapshotRef[0] : null;
        if (snapshot != null) {
            snapshot.setVisibility(View.VISIBLE);
            snapshot.setAlpha(1f - Math.min(1f, initialProgress * 2f));
            snapshot.setScaleX(1f);
            snapshot.setScaleY(1f);
            snapshot.setTranslationX(0f);
            snapshot.setTranslationY(0f);
            snapshot.bringToFront();
        }
        panel.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        android.animation.ValueAnimator transition =
                android.animation.ValueAnimator.ofFloat(initialProgress, 0f);
        transition.setDuration(animationDuration(175L));
        transition.setInterpolator(new android.view.animation.DecelerateInterpolator(1.2f));
        transition.addUpdateListener(animation -> {
            float progress = (Float) animation.getAnimatedValue();
            if (morphProgress != null) morphProgress[0] = progress;
            float sx = targetScaleX + (1f - targetScaleX) * progress;
            float sy = targetScaleY + (1f - targetScaleY) * progress;
            float tx = offsetX * (1f - progress);
            float ty = offsetY * (1f - progress);
            panel.setScaleX(sx);
            panel.setScaleY(sy);
            panel.setTranslationX(tx);
            panel.setTranslationY(ty);
            // Keep panel content cached during close as well; the panel transform and
            // source-icon snapshot are the only animated foreground surfaces.
            if (snapshot != null) {
                snapshot.setScaleX(1f);
                snapshot.setScaleY(1f);
                snapshot.setTranslationX(0f);
                snapshot.setTranslationY(0f);
                snapshot.setAlpha(1f - Math.min(1f, progress * 2f));
                snapshot.setVisibility(progress >= 0.5f ? View.GONE : View.VISIBLE);
            }
            if (dimLayer != null) dimLayer.setAlpha(0f);
        });
        transition.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(android.animation.Animator animation) {
                panel.setLayerType(View.LAYER_TYPE_NONE, null);
                if (dialog.isShowing()) dialog.dismiss();
                if (afterClose != null) afterClose.run();
            }
        });
        morphAnimator[0] = transition;
        transition.start();
    }

    private void clearAppGroupMemberMoveMode() {
        View previous = activeAppGroupMemberMoveModeView;
        activeAppGroupMemberMoveModeId = null;
        activeAppGroupMemberMoveModePackage = null;
        activeAppGroupMemberMoveModeView = null;
        if (previous != null) {
            LauncherUiUtils.stopMoveHint(previous);
            previous.animate().cancel();
            previous.setTranslationX(0f);
            previous.setTranslationY(0f);
            previous.setBackground(null);
            if (Build.VERSION.SDK_INT >= 21) previous.setElevation(0f);
            previous.setAlpha(1f);
        }
    }

    private void moveAppGroupMemberToHome(String groupId, String packageName) {
        if (groupId == null || packageName == null) return;
        ArrayList<String> members = getAppGroupMembers(groupId);
        if (members.remove(packageName)) saveAppGroupMembers(groupId, members);
        clearAppGroupMemberMoveMode();
        ArrayList<String> restore = new ArrayList<String>();
        restore.add(packageName);
        // Already-present standalone icons remain untouched; only missing Home icons are restored.
        restoreGroupedAppsToHome(getAppGroupPage(groupId), restore);
        Toast.makeText(this, "Removed from group", Toast.LENGTH_SHORT).show();
    }

    private void showAppGroupMemberOptions(String groupId, String packageName) {
        ResolveInfo app = findApp(packageName);
        if (app == null || app.activityInfo == null) {
            Toast.makeText(this, "This app is no longer installed.", Toast.LENGTH_SHORT).show();
            return;
        }
        String label = String.valueOf(app.loadLabel(packageManager));
        showActionDialog(label, new String[]{"−", "×", "ⓘ"},
                new String[]{"Remove from group", "Uninstall app", "App info"},
                (optionsDialog, which) -> {
            if (which == 0) {
                moveAppGroupMemberToHome(groupId, packageName);
            } else if (which == 1) {
                try {
                    startActivity(new Intent(Intent.ACTION_DELETE,
                            android.net.Uri.parse("package:" + packageName)));
                } catch (RuntimeException error) {
                    Toast.makeText(this, "Unable to open the uninstall screen.",
                            Toast.LENGTH_SHORT).show();
                }
            } else if (which == 2) {
                try {
                    startActivity(new Intent(
                            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            android.net.Uri.parse("package:" + packageName)));
                } catch (RuntimeException error) {
                    Toast.makeText(this, "Unable to open app info.",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private View findAppGroupView(String groupId) {
        if (pagesStrip == null || groupId == null) return null;
        for (int p = 0; p < pagesStrip.getChildCount(); p++) {
            View pageView = pagesStrip.getChildAt(p);
            if (!(pageView instanceof ViewGroup)) continue;
            ViewGroup page = (ViewGroup) pageView;
            for (int i = 0; i < page.getChildCount(); i++) {
                View child = page.getChildAt(i);
                if (isAppGroupItem(child) && groupId.equals(appGroupId(child))) return child;
            }
        }
        return null;
    }

    private View findAppGroupLogoView(String groupId) {
        View cell = findAppGroupView(groupId);
        if (cell instanceof ViewGroup && ((ViewGroup) cell).getChildCount() > 0) {
            View firstChild = ((ViewGroup) cell).getChildAt(0);
            if (firstChild instanceof FrameLayout) return firstChild;
        }
        return cell;
    }

    private void installAppGroupMemberTouch(View item, View icon, Dialog groupDialog,
            LinearLayout popupPanel, String groupId, String packageName, ResolveInfo app) {
        final android.os.Handler touchHandler = new android.os.Handler(
                android.os.Looper.getMainLooper());
        final float[] down = new float[2];
        final boolean[] armed = new boolean[1];
        final boolean[] movedTooEarly = new boolean[1];
        final boolean[] draggingOut = new boolean[1];
        final boolean[] memberDragging = new boolean[1];
        final boolean[] optionsShown = new boolean[1];

        final Runnable armMove = () -> {
            if (movedTooEarly[0] || draggingOut[0] || memberDragging[0]
                    || optionsShown[0]) return;
            armed[0] = true;
            activeAppGroupMemberMoveModeId = groupId;
            activeAppGroupMemberMoveModePackage = packageName;
            activeAppGroupMemberMoveModeView = item;
            setAppGroupMoveModeVisual(item, true);
            LauncherUiUtils.startScaleHint(item, 1.08f);
            // Long-press movement owns the next move stream, but quick vertical swipes
            // before this timer still belong to the group's ScrollView.
            android.view.ViewParent parent = item.getParent();
            if (parent != null) parent.requestDisallowInterceptTouchEvent(true);
        };
        final Runnable showOptions = () -> {
            if (movedTooEarly[0] || draggingOut[0] || optionsShown[0]) return;
            optionsShown[0] = true;
            touchHandler.removeCallbacks(armMove);
            clearAppGroupMemberMoveMode();
            groupDialog.dismiss();
            showAppGroupMemberOptions(groupId, packageName);
        };

        item.setTag("launcher_app_group_member:" + packageName);
        item.setOnTouchListener((memberView, event) -> {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                boolean resumeMove = groupId.equals(activeAppGroupMemberMoveModeId)
                        && packageName.equals(activeAppGroupMemberMoveModePackage);
                if (!resumeMove && activeAppGroupMemberMoveModeView != null) {
                    clearAppGroupMemberMoveMode();
                }
                down[0] = event.getRawX();
                down[1] = event.getRawY();
                armed[0] = resumeMove;
                movedTooEarly[0] = false;
                draggingOut[0] = false;
                memberDragging[0] = false;
                optionsShown[0] = false;
                touchHandler.removeCallbacks(armMove);
                touchHandler.removeCallbacks(showOptions);
                if (!resumeMove) touchHandler.postDelayed(armMove, 1000L);
                touchHandler.postDelayed(showOptions, 2000L);
                return true;
            }
            if (action == MotionEvent.ACTION_MOVE) {
                float dx = event.getRawX() - down[0];
                float dy = event.getRawY() - down[1];
                boolean pastSlop = Math.abs(dx) > LauncherUiUtils.dp(MainActivity.this, 8)
                        || Math.abs(dy) > LauncherUiUtils.dp(MainActivity.this, 8);
                if (pastSlop && !armed[0]) {
                    movedTooEarly[0] = true;
                    touchHandler.removeCallbacks(armMove);
                    touchHandler.removeCallbacks(showOptions);
                    return true;
                }
                if (pastSlop && armed[0]) {
                    memberDragging[0] = true;
                    touchHandler.removeCallbacks(showOptions);
                    android.view.ViewParent parent = memberView.getParent();
                    if (parent != null) parent.requestDisallowInterceptTouchEvent(true);
                    memberView.setTranslationX(dx);
                    memberView.setTranslationY(dy);
                    int[] panelLocation = new int[2];
                    popupPanel.getLocationOnScreen(panelLocation);
                    float margin = LauncherUiUtils.dp(MainActivity.this, 10);
                    draggingOut[0] = event.getRawX() < panelLocation[0] - margin
                            || event.getRawY() < panelLocation[1] - margin
                            || event.getRawX() > panelLocation[0] + popupPanel.getWidth() + margin
                            || event.getRawY() > panelLocation[1] + popupPanel.getHeight() + margin;
                    memberView.setAlpha(draggingOut[0] ? 0.78f : 1f);
                }
                return true;
            }
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                String reorderTargetPackage = null;
                if (action == MotionEvent.ACTION_UP && memberDragging[0] && !draggingOut[0]) {
                    View dropTarget = findViewAtExcluding(popupPanel,
                            (int) event.getRawX(), (int) event.getRawY(), memberView);
                    while (dropTarget != null && dropTarget != popupPanel) {
                        Object tag = dropTarget.getTag();
                        if (tag instanceof String
                                && ((String) tag).startsWith("launcher_app_group_member:")) {
                            reorderTargetPackage = ((String) tag)
                                    .substring("launcher_app_group_member:".length());
                            break;
                        }
                        android.view.ViewParent dropParent = dropTarget.getParent();
                        dropTarget = dropParent instanceof View ? (View) dropParent : null;
                    }
                }
                touchHandler.removeCallbacks(armMove);
                touchHandler.removeCallbacks(showOptions);
                memberView.setTranslationX(0f);
                memberView.setTranslationY(0f);
                memberView.setAlpha(1f);
                android.view.ViewParent parent = memberView.getParent();
                if (parent != null) parent.requestDisallowInterceptTouchEvent(false);
                if (optionsShown[0]) return true;
                if (action == MotionEvent.ACTION_UP && draggingOut[0]) {
                    clearAppGroupMemberMoveMode();
                    groupDialog.dismiss();
                    moveAppGroupMemberToHome(groupId, packageName);
                    return true;
                }
                if (action == MotionEvent.ACTION_UP && memberDragging[0]) {
                    if (reorderTargetPackage != null
                            && !packageName.equals(reorderTargetPackage)) {
                        ArrayList<String> members = getAppGroupMembers(groupId);
                        int sourceIndex = members.indexOf(packageName);
                        int targetIndex = members.indexOf(reorderTargetPackage);
                        if (sourceIndex >= 0 && targetIndex >= 0) {
                            members.remove(sourceIndex);
                            targetIndex = members.indexOf(reorderTargetPackage);
                            members.add(Math.max(0, targetIndex), packageName);
                            saveAppGroupMembers(groupId, members);
                            clearAppGroupMemberMoveMode();
                            groupDialog.dismiss();
                            openAppGroup(groupId);
                            Toast.makeText(MainActivity.this, "Apps reordered",
                                    Toast.LENGTH_SHORT).show();
                            return true;
                        }
                    }
                    activeAppGroupMemberMoveModeId = groupId;
                    activeAppGroupMemberMoveModePackage = packageName;
                    activeAppGroupMemberMoveModeView = item;
                    setAppGroupMoveModeVisual(item, true);
                    return true;
                }
                if (action == MotionEvent.ACTION_UP && armed[0] && !movedTooEarly[0]) {
                    activeAppGroupMemberMoveModeId = groupId;
                    activeAppGroupMemberMoveModePackage = packageName;
                    activeAppGroupMemberMoveModeView = item;
                    setAppGroupMoveModeVisual(item, true);
                    Toast.makeText(MainActivity.this,
                            "Move mode active. Drag this app outside the group to remove it.",
                            Toast.LENGTH_SHORT).show();
                    return true;
                }
                if (action == MotionEvent.ACTION_UP && !movedTooEarly[0] && !armed[0]) {
                    clearAppGroupMemberMoveMode();
                    groupDialog.dismiss();
                    launchApp(app, icon);
                } else if (!armed[0]) {
                    memberView.setBackground(null);
                }
                return true;
            }
            return true;
        });
    }

    private GradientDrawable appGroupMoveHighlight() {
        GradientDrawable d = new GradientDrawable();
        d.setColor(Color.argb(112, 25, 105, 210));
        d.setCornerRadius(LauncherUiUtils.dp(this, 14));
        d.setStroke(LauncherUiUtils.dp(this, 2), Color.rgb(84, 176, 255));
        return d;
    }

    private void applyAppGroupMoveModeBackground(View cell, boolean active) {
        if (cell == null) return;
        if (active) {
            if (cell.getBackground() == null) cell.setBackground(appGroupMoveHighlight());
            if (Build.VERSION.SDK_INT >= 21) cell.setElevation(LauncherUiUtils.dp(this, 8));
        } else {
            if (cell.getBackground() != null) cell.setBackground(null);
            if (Build.VERSION.SDK_INT >= 21 && cell.getElevation() != 0f) {
                cell.setElevation(0f);
            }
        }
        // The cell-wide highlight can be hidden behind the logo artwork. Add the same
        // blue state directly over the logo, using a translucent fill so its mini-icons
        // remain visible. refreshAppGroupMoveModeHighlights() applies this to every group
        // while a workspace/group move mode is active.
        if (cell instanceof ViewGroup) {
            ViewGroup groupCell = (ViewGroup) cell;
            for (int i = 0; i < groupCell.getChildCount(); i++) {
                View child = groupCell.getChildAt(i);
                if (!(child instanceof ViewGroup)
                        || !"launcher_app_group_logo".equals(child.getTag())) continue;
                ViewGroup logo = (ViewGroup) child;
                for (int j = 0; j < logo.getChildCount(); j++) {
                    View overlay = logo.getChildAt(j);
                    if ("launcher_app_group_move_highlight".equals(overlay.getTag())) {
                        int visibility = active ? View.VISIBLE : View.GONE;
                        if (overlay.getVisibility() != visibility) {
                            overlay.setVisibility(visibility);
                        }
                    }
                }
            }
        }
    }

    private void refreshAppGroupMoveModeHighlights() {
        if (pagesStrip == null) return;
        for (int pageIndex = 0; pageIndex < pagesStrip.getChildCount(); pageIndex++) {
            View pageView = pagesStrip.getChildAt(pageIndex);
            if (!(pageView instanceof ViewGroup)) continue;
            ViewGroup page = (ViewGroup) pageView;
            for (int i = 0; i < page.getChildCount(); i++) {
                View child = page.getChildAt(i);
                if (!isAppGroupItem(child)) continue;
                String id = appGroupId(child);
                boolean anyGroupMoveMode = activeAppGroupMoveModeId != null;
                boolean ownMoveMode = id != null && id.equals(activeAppGroupMoveModeId);
                applyAppGroupMoveModeBackground(child,
                        workspaceMoveModeHighlightActive || anyGroupMoveMode || ownMoveMode);
            }
        }
    }

    private void setAppGroupMoveModeVisual(View cell, boolean active) {
        if (cell == null) return;
        String id = appGroupId(cell);
        boolean ownMoveMode = id != null && id.equals(activeAppGroupMoveModeId);
        boolean anyGroupMoveMode = activeAppGroupMoveModeId != null;
        boolean highlighted = active || workspaceMoveModeHighlightActive
                || anyGroupMoveMode || ownMoveMode;
        applyAppGroupMoveModeBackground(cell, highlighted);
        if (!highlighted) cell.setAlpha(1f);
        cell.invalidate();
    }

    private void installAppGroupTouch(View cell, String groupId) {
        final android.os.Handler groupHandler = new android.os.Handler(
                android.os.Looper.getMainLooper());
        final float[] down = new float[2];
        final float[] fingerOffset = new float[2];
        final int[] origin = new int[2];
        final int[] sourcePageIndex = {-1};
        final boolean[] armed = new boolean[1];
        final boolean[] dragging = new boolean[1];
        final boolean[] movedTooEarly = new boolean[1];
        final boolean[] optionsShown = new boolean[1];
        final boolean[] resumedMoveMode = new boolean[1];
        final boolean[] reparenting = new boolean[1];
        final Runnable armMove = () -> {
            if (optionsShown[0] || dragging[0]) return;
            armed[0] = true;
            activeAppGroupMoveModeId = groupId;
            setWorkspaceGridVisible(true, cell);
            refreshAppGroupMoveModeHighlights();
            setAppGroupMoveModeVisual(cell, true);
            LauncherUiUtils.startScaleHint(cell, 1.08f);
        };
        final Runnable showOptions = () -> {
            if (!dragging[0] && !movedTooEarly[0]) {
                optionsShown[0] = true;
                activeAppGroupMoveModeId = null;
                refreshAppGroupMoveModeHighlights();
                LauncherUiUtils.stopMoveHint(cell);
                setAppGroupMoveModeVisual(cell, false);
                setWorkspaceGridVisible(false, null);
                showAppGroupOptions(groupId);
            }
        };

        cell.setOnTouchListener((view, event) -> {
            if (reparenting[0] || workspaceIconReparenting) return true;
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                down[0] = event.getRawX();
                down[1] = event.getRawY();
                int[] location = new int[2];
                view.getLocationOnScreen(location);
                fingerOffset[0] = event.getRawX() - location[0];
                fingerOffset[1] = event.getRawY() - location[1];
                FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) view.getLayoutParams();
                origin[0] = lp.leftMargin;
                origin[1] = lp.topMargin;
                sourcePageIndex[0] = view.getParent() instanceof View
                        && pagesStrip != null
                        ? pagesStrip.indexOfChild((View) view.getParent()) : currentPage;
                resumedMoveMode[0] = groupId.equals(activeAppGroupMoveModeId);
                armed[0] = resumedMoveMode[0];
                dragging[0] = false;
                movedTooEarly[0] = false;
                optionsShown[0] = false;
                groupHandler.removeCallbacks(armMove);
                groupHandler.removeCallbacks(showOptions);
                groupHandler.postDelayed(armMove, 1000L);
                groupHandler.postDelayed(showOptions, 2000L);
                return true;
            }
            if (action == MotionEvent.ACTION_MOVE) {
                float dx = event.getRawX() - down[0];
                float dy = event.getRawY() - down[1];
                boolean pastSlop = Math.abs(dx) > LauncherUiUtils.dp(this, 8)
                        || Math.abs(dy) > LauncherUiUtils.dp(this, 8);
                if (pastSlop && !armed[0]) {
                    movedTooEarly[0] = true;
                    groupHandler.removeCallbacks(armMove);
                    groupHandler.removeCallbacks(showOptions);
                    return true;
                }
                if (pastSlop && armed[0] && !optionsShown[0]) {
                    boolean firstFrame = !dragging[0];
                    dragging[0] = true;
                    // Keep the active group ID set during the live drag so every group
                    // remains visibly blue until the move transaction finishes.
                    refreshAppGroupMoveModeHighlights();
                    groupHandler.removeCallbacks(showOptions);
                    if (firstFrame) {
                        LauncherUiUtils.stopMoveHint(cell);
                        cell.animate().cancel();
                        cell.setPivotX(cell.getWidth() / 2f);
                        cell.setPivotY(cell.getHeight() / 2f);
                        cell.bringToFront();
                        LauncherUiUtils.startScaleHint(cell, 1.08f);
                        beginWorkspaceTransferDrag(cell, sourcePageIndex[0],
                                event.getRawX(), event.getRawY(),
                                fingerOffset[0], fingerOffset[1]);
                    }
                    updateWorkspaceTransferEdge(event.getRawX(), event.getRawY());
                    FrameLayout page = cell.getParent() instanceof FrameLayout
                            ? (FrameLayout) cell.getParent() : null;
                    if (page != null) {
                        int[] pageLocation = new int[2];
                        page.getLocationOnScreen(pageLocation);
                        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) cell.getLayoutParams();
                        float sx = Math.max(0.01f, cell.getScaleX());
                        float sy = Math.max(0.01f, cell.getScaleY());
                        int left = Math.round(event.getRawX() - pageLocation[0]
                                - fingerOffset[0] - cell.getTranslationX()
                                - cell.getPivotX() * (1f - sx));
                        int top = Math.round(event.getRawY() - pageLocation[1]
                                - fingerOffset[1] - cell.getTranslationY()
                                - cell.getPivotY() * (1f - sy));
                        lp.leftMargin = Math.max(0, Math.min(
                                Math.max(0, page.getWidth() - cell.getWidth()), left));
                        lp.topMargin = Math.max(0, Math.min(
                                Math.max(0, page.getHeight() - cell.getHeight()), top));
                        cell.setLayoutParams(lp);
                        int transferPage = getWorkspaceTransferTargetPage();
                        if (transferPage >= 0 && transferPage != sourcePageIndex[0]) {
                            updateWorkspaceTransferDisplacements(cell, event.getRawX(), event.getRawY(),
                                    fingerOffset[0], fingerOffset[1]);
                        } else {
                            scheduleLiveDisplacementUpdate(page, cell);
                        }
                    }
                    return true;
                }
                return true;
            }
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                groupHandler.removeCallbacks(armMove);
                groupHandler.removeCallbacks(showOptions);
                boolean commit = action == MotionEvent.ACTION_UP;
                if (dragging[0]) {
                    int transferPage = finishWorkspaceTransferAnimation();
                    if (!commit || transferPage < 0) transferPage = sourcePageIndex[0];
                    transferPage = Math.max(0, Math.min(1, transferPage));
                    FrameLayout sourcePage = cell.getParent() instanceof FrameLayout
                            ? (FrameLayout) cell.getParent() : null;
                    FrameLayout targetPage = pagesStrip != null && transferPage < pagesStrip.getChildCount()
                            && pagesStrip.getChildAt(transferPage) instanceof FrameLayout
                            ? (FrameLayout) pagesStrip.getChildAt(transferPage) : sourcePage;
                    boolean movingAcrossPages = sourcePage != null && targetPage != sourcePage;
                    if (sourcePage == null || targetPage == null) commit = false;
                    int columns = Math.max(1, getHomeColumns());
                    int cellWidth = Math.max(1, targetPage == null ? 1 : targetPage.getWidth() / columns);
                    int iconDp = Math.max(24, Math.min(84, prefs.getInt(KEY_ICON_SIZE, 48)));
                    int cellHeight = LauncherUiUtils.dp(this, homeCellHeightDp(iconDp));
                    int rows = targetPage == null ? 1
                            : homeGridRowsForHeight(targetPage.getHeight(), cellHeight);
                    int left = origin[0], top = origin[1];
                    if (commit && targetPage != null) {
                        int[] targetLocation = new int[2];
                        targetPage.getLocationOnScreen(targetLocation);
                        left = Math.round(event.getRawX() - targetLocation[0] - fingerOffset[0]);
                        top = Math.round(event.getRawY() - targetLocation[1] - fingerOffset[1]);
                        left = Math.max(0, Math.min(Math.max(0, targetPage.getWidth() - cellWidth), left));
                        top = Math.max(0, Math.min(Math.max(0, targetPage.getHeight() - cellHeight), top));
                        int col = Math.max(0, Math.min(columns - 1,
                                Math.round(left / (float) cellWidth)));
                        int row = homeGridRowFromTop(top, rows, targetPage.getHeight(), cellHeight);
                        left = col * cellWidth;
                        top = homeGridRowTop(row, rows, targetPage.getHeight(), cellHeight);
                        cell.animate().cancel();
                        LauncherUiUtils.stopMoveHint(cell);
                        cell.setScaleX(1f);
                        cell.setScaleY(1f);
                        cell.setTranslationX(0f);
                        cell.setTranslationY(0f);
                        FrameLayout.LayoutParams snapped = new FrameLayout.LayoutParams(
                                cellWidth, cellHeight, Gravity.TOP | Gravity.LEFT);
                        snapped.leftMargin = left;
                        snapped.topMargin = top;
                        if (!movingAcrossPages) {
                            cell.setLayoutParams(snapped);
                            updateLiveDisplacements(targetPage, cell);
                        } else {
                            if (workspaceTransferProxy != null
                                    && workspaceTransferProxy.getParent() == targetPage
                                    && workspaceTransferProxy.getLayoutParams()
                                            instanceof FrameLayout.LayoutParams) {
                                FrameLayout.LayoutParams proxyLp =
                                        (FrameLayout.LayoutParams) workspaceTransferProxy.getLayoutParams();
                                workspaceTransferProxy.setScaleX(1f);
                                workspaceTransferProxy.setScaleY(1f);
                                workspaceTransferProxy.setTranslationX(0f);
                                workspaceTransferProxy.setTranslationY(0f);
                                proxyLp.leftMargin = left;
                                proxyLp.topMargin = top;
                                workspaceTransferProxy.setLayoutParams(proxyLp);
                                updateLiveDisplacements(targetPage, workspaceTransferProxy);
                            }
                        }
                        if (!canPlaceWorkspaceItem(targetPage, cell, left, top,
                                cellWidth, cellHeight)) {
                            commit = false;
                        } else if (movingAcrossPages) {
                            reparenting[0] = true;
                            sourcePage.removeView(cell);
                            targetPage.addView(cell, snapped);
                            reparenting[0] = false;
                            updateLiveDisplacements(targetPage, cell);
                        }
                    }
                    if (!commit && sourcePage != null) {
                        FrameLayout.LayoutParams rollback = (FrameLayout.LayoutParams) cell.getLayoutParams();
                        rollback.leftMargin = origin[0];
                        rollback.topMargin = origin[1];
                        cell.setTranslationX(0f);
                        cell.setTranslationY(0f);
                        cell.setScaleX(1f);
                        cell.setScaleY(1f);
                        cell.setAlpha(1f);
                        cell.setLayoutParams(rollback);
                    }
                    boolean displacementAccepted = finishLiveDisplacements(commit, cell);
                    if (commit && !displacementAccepted) {
                        commit = false;
                        // The drag planner can reject the full displacement transaction even
                        // after the candidate slot passed a local collision check. If the group
                        // was already reparented, put it back before ending the transfer.
                        if (sourcePage != null && cell.getParent() != sourcePage) {
                            if (cell.getParent() instanceof ViewGroup) {
                                ((ViewGroup) cell.getParent()).removeView(cell);
                            }
                            FrameLayout.LayoutParams returnParams = new FrameLayout.LayoutParams(
                                    cellWidth, cellHeight, Gravity.TOP | Gravity.LEFT);
                            returnParams.leftMargin = origin[0];
                            returnParams.topMargin = origin[1];
                            cell.setTranslationX(0f);
                            cell.setTranslationY(0f);
                            sourcePage.addView(cell, returnParams);
                        }
                    }
                    finishWorkspaceTransferDrag(cell, commit);
                    if (commit && targetPage != null) {
                        persistWorkspaceItemPosition(targetPage, cell, left, top,
                                cellWidth, cellHeight);
                        currentPage = transferPage;
                    } else if (!commit) {
                        Toast.makeText(MainActivity.this,
                                "That group position is occupied.", Toast.LENGTH_SHORT).show();
                    }
                    LauncherUiUtils.stopMoveHint(cell);
                    cell.setScaleX(1f);
                    cell.setScaleY(1f);
                    cell.setTranslationX(0f);
                    cell.setTranslationY(0f);
                    cell.setAlpha(1f);
                    setWorkspaceGridVisible(false, null);
                    activeAppGroupMoveModeId = null;
                    setAppGroupMoveModeVisual(cell, false);
                    refreshAppGroupMoveModeHighlights();
                    if (appGroupRenderDeferred) {
                        // The previous render was intentionally skipped during the
                        // gesture. It is now safe to rebuild after ACTION_UP has committed
                        // or rolled back and finishWorkspaceTransferDrag cleared the owner.
                        appGroupRenderDeferred = false;
                        renderAppGroups();
                    } else if (!commit) {
                        renderHomePages(false);
                    }
                    // Prefer whichever instance is actually attached if a previous render
                    // callback already rebuilt this group. Run once more after posted
                    // layout callbacks to enforce the one-ID/one-view invariant.
                    removeDuplicateAppGroupViews(groupId,
                            cell.getParent() != null ? cell : findAppGroupView(groupId));
                    final View preferredGroupView = cell;
                    pageViewport.post(() -> removeDuplicateAppGroupViews(groupId,
                            preferredGroupView.getParent() != null
                                    ? preferredGroupView : findAppGroupView(groupId)));
                    refreshWorkspaceGrid();
                    return true;
                }
                LauncherUiUtils.stopMoveHint(cell);
                if (commit && !optionsShown[0] && !movedTooEarly[0]) {
                    if (armed[0]) {
                        activeAppGroupMoveModeId = groupId;
                        refreshAppGroupMoveModeHighlights();
                        setAppGroupMoveModeVisual(cell, true);
                        Toast.makeText(MainActivity.this,
                                "Move mode active. Touch and drag this group to move it.",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        setAppGroupMoveModeVisual(cell, false);
                        refreshAppGroupMoveModeHighlights();
                        openAppGroup(groupId);
                    }
                } else if (!commit) {
                    activeAppGroupMoveModeId = null;
                    setAppGroupMoveModeVisual(cell, false);
                    refreshAppGroupMoveModeHighlights();
                    setWorkspaceGridVisible(false, null);
                }
                if (armed[0] || optionsShown[0]) setWorkspaceGridVisible(false, null);
                return true;
            }
            return true;
        });
    }



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

    private void ensureShortcutPageLayoutWatcher(FrameLayout page) {
        if (page == null || shortcutPageLayoutWatchers.containsKey(page)) return;
        shortcutPageLayoutWatchers.put(page, Boolean.TRUE);
        page.addOnLayoutChangeListener((view, left, top, right, bottom,
                                       oldLeft, oldTop, oldRight, oldBottom) -> {
            if (right - left == oldRight - oldLeft
                    && bottom - top == oldBottom - oldTop) return;
            syncShortcutCellsToPageGeometry((FrameLayout) view);
        });
    }

    private void syncShortcutCellsToPageGeometry(FrameLayout page) {
        if (page == null || page.getWidth() <= 0 || page.getHeight() <= 0
                || pagesStrip == null || page.getParent() != pagesStrip) return;
        int cellWidth = Math.max(1, page.getWidth() / 4);
        int cellHeight = LauncherUiUtils.dp(this,
                homeCellHeightDp(prefs == null ? 48 : prefs.getInt(KEY_ICON_SIZE, 48)));
        int rows = homeGridRowsForHeight(page.getHeight(), cellHeight);
        boolean foundShortcut = false;
        for (int i = 0; i < page.getChildCount(); i++) {
            View child = page.getChildAt(i);
            if (!isWorkspaceContactShortcut(child) || !(child instanceof ViewGroup)
                    || !(child.getLayoutParams() instanceof FrameLayout.LayoutParams)) continue;
            foundShortcut = true;
            ViewGroup tile = (ViewGroup) child;
            if (tile.getChildCount() < 2) continue;
            ImageView icon = tile.getChildAt(0) instanceof ImageView
                    ? (ImageView) tile.getChildAt(0) : null;
            TextView label = tile.getChildAt(1) instanceof TextView
                    ? (TextView) tile.getChildAt(1) : null;
            if (icon == null) continue;
            String id = shortcutIdFromTile(tile);
            if (id.isEmpty()) continue;
            float savedX = Math.max(0f, Math.min(1f, getFloatPreferenceCompat(
                    shortcutPrefKey(KEY_SHORTCUT_X, id), 0f)));
            float savedY = Math.max(0f, Math.min(1f, getFloatPreferenceCompat(
                    shortcutPrefKey(KEY_SHORTCUT_Y, id), 0f)));
            int col = Math.max(0, Math.min(3, Math.round(savedX * 3f)));
            int row = Math.max(0, Math.min(rows - 1,
                    Math.round(savedY * Math.max(1, rows - 1))));
            int iconSize = shortcutIconSizePx(cellWidth, cellHeight);
            FrameLayout.LayoutParams tileParams = (FrameLayout.LayoutParams) tile.getLayoutParams();
            tileParams.width = cellWidth;
            tileParams.height = cellHeight;
            tileParams.leftMargin = col * cellWidth;
            tileParams.topMargin = homeGridRowTop(row, rows, page.getHeight(), cellHeight);
            tileParams.gravity = Gravity.TOP | Gravity.LEFT;
            tile.setLayoutParams(tileParams);
            if (icon.getLayoutParams() instanceof FrameLayout.LayoutParams) {
                FrameLayout.LayoutParams iconParams =
                        (FrameLayout.LayoutParams) icon.getLayoutParams();
                iconParams.width = iconSize;
                iconParams.height = iconSize;
                iconParams.leftMargin = Math.max(0, (cellWidth - iconSize) / 2);
                iconParams.topMargin = LauncherUiUtils.dp(this, 14);
                icon.setLayoutParams(iconParams);
            }
            if (label != null && label.getLayoutParams() instanceof FrameLayout.LayoutParams) {
                FrameLayout.LayoutParams labelParams =
                        (FrameLayout.LayoutParams) label.getLayoutParams();
                labelParams.width = cellWidth;
                labelParams.height = workspaceLabelHeightPx(label);
                labelParams.leftMargin = 0;
                labelParams.topMargin = LauncherUiUtils.dp(this, 15) + iconSize;
                label.setLayoutParams(labelParams);
                label.post(() -> fitShortcutLabelWithDots(label));
            }
        }
        if (!foundShortcut) return;
        page.post(() -> {
            if (page.getParent() == pagesStrip && !homeIconResizeRepairPending) {
                resolveAllWorkspaceOverlaps(page, null);
            }
            ensureShortcutLabelsNotClipped();
        });
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
        float savedX = getFloatPreferenceCompat(
                shortcutPrefKey(KEY_SHORTCUT_X, shortcutId), -1f);
        float savedY = getFloatPreferenceCompat(
                shortcutPrefKey(KEY_SHORTCUT_Y, shortcutId), -1f);
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
        int tileLeft = chosenCol * cellW;
        int tileTop = homeGridRowTop(chosenRow, rows, pageViewport.getHeight(), cellH);
        ensureShortcutPageLayoutWatcher(chosenPage);
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
        updateShortcutIconAppearance(icon, tile,
                LauncherIconDrawable.clampArtworkTheme(prefs.getInt(KEY_ICON_ARTWORK_THEME,
                        LauncherIconDrawable.ARTWORK_ORIGINAL)),
                LauncherIconDrawable.backgroundColorFromPreferences(prefs));

        FrameLayout.LayoutParams iconParams = new FrameLayout.LayoutParams(
                initialIconSize, initialIconSize, Gravity.TOP | Gravity.LEFT);
        iconParams.leftMargin = Math.max(0, (cellW - initialIconSize) / 2);
        iconParams.topMargin = LauncherUiUtils.dp(this, 14);
        tile.addView(icon, iconParams);

        TextView label = LauncherUiUtils.text(this,
                prefs.getString(shortcutPrefKey(KEY_SHORTCUT_LABEL, shortcutId), "Contact shortcut"),
                homeFontSizeSp(), homeLabelColor(), false);
        label.setContentDescription(label.getText());
        label.setTypeface(Typeface.create(homeLabelTypefaceFamily(), Typeface.NORMAL));
        label.setShadowLayer(LauncherUiUtils.dp(this, 1), 0f, LauncherUiUtils.dp(this, 1),
                Color.argb(180, 0, 0, 0));
        label.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
        label.setSingleLine(true);
        label.setMaxLines(1);
        label.setEllipsize(null);
        label.setIncludeFontPadding(false);
        label.addOnLayoutChangeListener((view, left, top, right, bottom,
                                         oldLeft, oldTop, oldRight, oldBottom) -> {
            if (right - left != oldRight - oldLeft) {
                fitShortcutLabelWithDots((TextView) view);
            }
        });
        int shortcutLabelHeight = workspaceLabelHeightPx(label);
        FrameLayout.LayoutParams labelParams = new FrameLayout.LayoutParams(
                cellW, shortcutLabelHeight, Gravity.TOP | Gravity.LEFT);
        labelParams.leftMargin = 0;
        labelParams.topMargin = LauncherUiUtils.dp(this, 15) + initialIconSize;
        tile.addView(label, labelParams);
        label.setVisibility(prefs.getBoolean(KEY_SHOW_HOME_LABELS, true)
                ? View.VISIBLE : View.GONE);

        int initialLabelExtraHeight = prefs.getBoolean(KEY_SHOW_HOME_LABELS, true)
                ? shortcutLabelHeight + LauncherUiUtils.dp(this, 1) : 0;
        FrameLayout.LayoutParams tileParams = new FrameLayout.LayoutParams(
                cellW, cellH, Gravity.TOP | Gravity.LEFT);
        tileParams.leftMargin = tileLeft;
        tileParams.topMargin = tileTop;
        chosenPage.addView(tile, tileParams);
        label.post(() -> fitShortcutLabelWithDots(label));
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
                        if (commit) {
                            lp.leftMargin = left;
                            lp.topMargin = top;
                            tile.setTranslationX(0f);
                            tile.setTranslationY(0f);
                            tile.setLayoutParams(lp);
                            updateLiveDisplacements(targetPage, tile);
                        }
                        if (commit && !canPlaceWorkspaceItem(targetPage, tile, left, top,
                                Math.max(1, lp.width > 0 ? lp.width : tile.getWidth()),
                                Math.max(1, lp.height > 0 ? lp.height : tile.getHeight()))) {
                            lp.leftMargin = shortcutOrigin[0];
                            lp.topMargin = shortcutOrigin[1];
                            tile.setTranslationX(0f);
                            tile.setTranslationY(0f);
                            tile.setLayoutParams(lp);
                            finishWorkspaceTransferDrag(tile, false);
                            finishLiveDisplacements(false, tile);
                            shortcutDragging[0] = false;
                            shortcutMoveArmed[0] = false;
                            if (tile.getParent() != null) {
                                tile.getParent().requestDisallowInterceptTouchEvent(false);
                            }
                            setWorkspaceGridVisible(false, null);
                            refreshWorkspaceGrid();
                            showWorkspaceOverlapRejected();
                            return true;
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
        paint.setTypeface(Typeface.create(homeLabelTypefaceFamily(), Typeface.BOLD));
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
                FrameLayout.LayoutParams tileParams =
                        (FrameLayout.LayoutParams) shortcut.getLayoutParams();
                tileParams.width = cellWidth;
                tileParams.height = cellHeight;
                tileParams.leftMargin = slot[0] * cellWidth;
                tileParams.topMargin = homeGridRowTop(slot[1], rows, page.getHeight(), cellHeight);
                tileParams.gravity = Gravity.TOP | Gravity.LEFT;
                if (label != null) label.setTextSize(homeFontSizeSp());
                shortcut.setLayoutParams(tileParams);

                if (icon.getLayoutParams() instanceof FrameLayout.LayoutParams) {
                    FrameLayout.LayoutParams iconParams =
                            (FrameLayout.LayoutParams) icon.getLayoutParams();
                    iconParams.width = target;
                    iconParams.height = target;
                    iconParams.leftMargin = Math.max(0, (cellWidth - target) / 2);
                    iconParams.topMargin = LauncherUiUtils.dp(this, 14);
                    icon.setLayoutParams(iconParams);
                }
                if (label != null && label.getLayoutParams() instanceof FrameLayout.LayoutParams) {
                    FrameLayout.LayoutParams labelParams =
                            (FrameLayout.LayoutParams) label.getLayoutParams();
                    labelParams.width = cellWidth;
                    labelParams.height = workspaceLabelHeightPx(label);
                    labelParams.leftMargin = 0;
                    labelParams.topMargin = LauncherUiUtils.dp(this, 15) + target;
                    label.setLayoutParams(labelParams);
                    label.post(() -> fitShortcutLabelWithDots(label));
                }
            }
            page.post(() -> {
                if (pagesStrip == null || page.getParent() != pagesStrip) return;
                if (!homeIconResizeRepairPending) resolveAllWorkspaceOverlaps(page, null);
                ensureShortcutLabelsNotClipped();
                refreshWorkspaceGrid();
            });
        }
        ensureShortcutLabelsNotClipped();
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
    private void updateIconAppearance(ImageView icon, ResolveInfo app, boolean showBackground,
            int shape, int backgroundColor, int artworkTheme, boolean fitInsideBackground) {
        if (icon == null) return;
        int safeShape = LauncherIconDrawable.clampShape(shape);
        boolean outlineClip = showBackground
                && (safeShape == LauncherIconDrawable.SHAPE_SQUARE
                    || safeShape == LauncherIconDrawable.SHAPE_ROUND)
                && android.os.Build.VERSION.SDK_INT >= 21;

        // Match LauncherAppAdapter.bindIconAppearance() whenever a preference changes.
        // Mutating the Drawable alone is not enough: ImageView's native outline and the
        // wrapper's external-clip mode must change too, otherwise the old shape remains
        // visible until a later home-item rebuild.
        if (outlineClip) {
            android.view.ViewOutlineProvider provider =
                    safeShape == LauncherIconDrawable.SHAPE_ROUND
                            ? HOME_ICON_ROUND_OUTLINE : HOME_ICON_ROUNDED_SQUARE_OUTLINE;
            if (icon.getOutlineProvider() != provider) {
                icon.setOutlineProvider(provider);
                icon.invalidateOutline();
            }
            if (!icon.getClipToOutline()) {
                icon.setClipToOutline(true);
                icon.invalidateOutline();
            }
        } else if (icon.getClipToOutline()) {
            icon.setClipToOutline(false);
            icon.invalidateOutline();
        }

        Drawable current = icon.getDrawable();
        if (current instanceof LauncherIconDrawable) {
            LauncherIconDrawable wrapped = (LauncherIconDrawable) current;
            wrapped.updateAppearance(showBackground, safeShape, backgroundColor,
                    artworkTheme, fitInsideBackground);
            wrapped.setExternalShapeClip(outlineClip);
            icon.invalidate();
            return;
        }
        // A package-manager icon is only loaded when the view has not yet received
        // our wrapper. Continuous colour dragging reuses the existing drawable.
        if (app == null) return;
        Drawable loaded = app.loadIcon(packageManager);
        String cacheToken = app.activityInfo == null ? null
                : app.activityInfo.packageName + "/" + app.activityInfo.name;
        LauncherIconDrawable wrapped = loaded == null ? null : new LauncherIconDrawable(
                loaded, showBackground, safeShape, backgroundColor, artworkTheme, cacheToken,
                fitInsideBackground);
        if (wrapped != null) wrapped.setExternalShapeClip(outlineClip);
        icon.setImageDrawable(wrapped);
        icon.invalidate();
    }

    private void updateShortcutIconAppearance(ImageView icon, View shortcutTile,
            int artworkTheme, int backgroundColor) {
        if (icon == null || prefs == null) return;
        boolean enabled = prefs.getBoolean(KEY_THEME_SHORTCUT_ARTWORK, false);
        int requestedTheme = enabled
                ? LauncherIconDrawable.clampArtworkTheme(artworkTheme)
                : LauncherIconDrawable.ARTWORK_ORIGINAL;
        Drawable current = icon.getDrawable();
        if (current instanceof LauncherIconDrawable) {
            ((LauncherIconDrawable) current).updateAppearance(false,
                    LauncherIconDrawable.SHAPE_SQUARE, backgroundColor, requestedTheme, false);
            return;
        }
        // Keep the shortcut on the shared renderer even when recolouring is disabled. This
        // applies only the cached inner silhouette border to Original artwork in that case.
        if (current == null) return;
        String token = shortcutTile == null ? "shortcut:unknown"
                : String.valueOf(shortcutTile.getContentDescription());
        icon.setImageDrawable(new LauncherIconDrawable(current, false,
                LauncherIconDrawable.SHAPE_SQUARE, backgroundColor, requestedTheme,
                "shortcut:" + token, false));
    }

    private void ensureShortcutLabelsNotClipped() {
        if (pagesStrip == null) return;
        for (int pageIndex = 0; pageIndex < Math.min(2, pagesStrip.getChildCount()); pageIndex++) {
            View pageView = pagesStrip.getChildAt(pageIndex);
            if (!(pageView instanceof ViewGroup)) continue;
            if (pageView instanceof FrameLayout) {
                // Watch both screens, not just the page on which a shortcut was first created.
                // A shortcut can be moved to the other page before a later rotation/resize.
                ensureShortcutPageLayoutWatcher((FrameLayout) pageView);
            }
            ViewGroup page = (ViewGroup) pageView;
            boolean hasContactShortcut = false;
            for (int childIndex = 0; childIndex < page.getChildCount(); childIndex++) {
                Object tag = page.getChildAt(childIndex).getTag();
                if ("launcher_contact_shortcut".equals(tag)) {
                    hasContactShortcut = true;
                    if (page.getChildAt(childIndex) instanceof ViewGroup) {
                        ViewGroup shortcutTile = (ViewGroup) page.getChildAt(childIndex);
                        shortcutTile.setClipChildren(false);
                        shortcutTile.setClipToPadding(false);
                    }
                }
            }
            if (hasContactShortcut) {
                // The icon tile stays icon-sized; only its separate label view spans the
                // logical cell. The page viewport remains the clipping boundary for the screen.
                page.setClipChildren(false);
                page.setClipToPadding(false);
            }
        }
    }

    private void applyWorkspaceLabelAppearance(TextView label, boolean shortcut) {
        if (label == null) return;
        // Home and shortcut labels share the same font family and regular weight.
        label.setTypeface(Typeface.create(homeLabelTypefaceFamily(), Typeface.NORMAL));
        if (shortcut) label.setTextSize(homeFontSizeSp());
        label.setTextColor(homeLabelColor());
        label.setShadowLayer(LauncherUiUtils.dp(this, 1), 0f, LauncherUiUtils.dp(this, 1),
                Color.argb(180, 0, 0, 0));
        label.setSingleLine(true);
        label.setMaxLines(1);
        label.setEllipsize(android.text.TextUtils.TruncateAt.END);
        label.setIncludeFontPadding(false);
        if (shortcut && label.getParent() instanceof ViewGroup) {
            ViewGroup shortcutTile = (ViewGroup) label.getParent();
            shortcutTile.setClipChildren(true);
            shortcutTile.setClipToPadding(true);
            View shortcutIcon = shortcutTile.getChildCount() > 0
                    ? shortcutTile.getChildAt(0) : null;
            ViewGroup.LayoutParams iconRawParams = shortcutIcon == null
                    ? null : shortcutIcon.getLayoutParams();
            int iconHeight = iconRawParams != null && iconRawParams.height > 0
                    ? iconRawParams.height : (shortcutIcon == null ? 0 : shortcutIcon.getHeight());
            int iconTop = iconRawParams instanceof FrameLayout.LayoutParams
                    ? ((FrameLayout.LayoutParams) iconRawParams).topMargin
                    : LauncherUiUtils.dp(this, 14);
            int labelHeight = workspaceLabelHeightPx(label);
            ViewGroup.LayoutParams labelRawParams = label.getLayoutParams();
            if (labelRawParams instanceof FrameLayout.LayoutParams) {
                FrameLayout.LayoutParams labelParams = (FrameLayout.LayoutParams) labelRawParams;
                labelParams.width = shortcutTile.getParent() instanceof View
                        ? Math.max(1, ((View) shortcutTile.getParent()).getWidth() / 4)
                        : Math.max(1, shortcutTile.getWidth());
                labelParams.height = labelHeight;
                labelParams.leftMargin = 0;
                labelParams.topMargin = iconTop + iconHeight + LauncherUiUtils.dp(this, 1);
                label.setLayoutParams(labelParams);
            }
            ViewGroup.LayoutParams tileRawParams = shortcutTile.getLayoutParams();
            if (tileRawParams instanceof FrameLayout.LayoutParams) {
                FrameLayout.LayoutParams tileParams = (FrameLayout.LayoutParams) tileRawParams;
                if (shortcutTile.getParent() instanceof View && tileRawParams.width != ViewGroup.LayoutParams.MATCH_PARENT) {
                    tileParams.width = Math.max(1, ((View) shortcutTile.getParent()).getWidth() / 4);
                }
                int iconSizeDp = Math.max(32, Math.min(74,
                        prefs == null ? 48 : prefs.getInt(KEY_ICON_SIZE, 48)));
                tileParams.height = LauncherUiUtils.dp(this, homeCellHeightDp(iconSizeDp));
                shortcutTile.setLayoutParams(tileParams);
            }
        }
        if (shortcut) {
            label.post(() -> fitShortcutLabelWithDots(label));
        }
        label.invalidate();
    }

    private void refreshWorkspaceLabelAppearance() {
        if (pagesStrip != null) {
            for (int pageIndex = 0; pageIndex < Math.min(2, pagesStrip.getChildCount()); pageIndex++) {
                View pageView = pagesStrip.getChildAt(pageIndex);
                if (!(pageView instanceof ViewGroup)) continue;
                ViewGroup page = (ViewGroup) pageView;
                for (int childIndex = 0; childIndex < page.getChildCount(); childIndex++) {
                    View child = page.getChildAt(childIndex);
                    if (!(child instanceof ViewGroup) || !(child.getTag() instanceof String)) continue;
                    String tag = (String) child.getTag();
                    ViewGroup cell = (ViewGroup) child;
                    if ("launcher_contact_shortcut".equals(tag)) {
                        if (cell.getChildCount() > 1 && cell.getChildAt(1) instanceof TextView) {
                            applyWorkspaceLabelAppearance((TextView) cell.getChildAt(1), true);
                        }
                    } else if (tag.startsWith("launcher_app:")) {
                        if (cell.getChildCount() > 1 && cell.getChildAt(1) instanceof TextView) {
                            applyWorkspaceLabelAppearance((TextView) cell.getChildAt(1), false);
                        }
                    }
                }
            }
        }
        if (drawerGrid != null) {
            for (int i = 0; i < drawerGrid.getChildCount(); i++) {
                View cell = drawerGrid.getChildAt(i);
                if (cell instanceof ViewGroup && ((ViewGroup) cell).getChildCount() > 1
                        && ((ViewGroup) cell).getChildAt(1) instanceof TextView) {
                    applyWorkspaceLabelAppearance((TextView) ((ViewGroup) cell).getChildAt(1), false);
                }
            }
        }
        ensureShortcutLabelsNotClipped();
    }

    private void refreshWorkspaceIconAppearance() {
        if (prefs == null) return;
        boolean showBackground = prefs.getBoolean("icon_rounded_square_background", false);
        int shape = LauncherIconDrawable.clampShape(
                prefs.getInt("app_icon_shape", LauncherIconDrawable.SHAPE_ORIGINAL));
        int backgroundColor = LauncherIconDrawable.backgroundColorFromPreferences(prefs);
        int artworkTheme = LauncherIconDrawable.clampArtworkTheme(
                prefs.getInt(KEY_ICON_ARTWORK_THEME, LauncherIconDrawable.ARTWORK_ORIGINAL));
        boolean fitInsideBackground = prefs.getBoolean(KEY_ICON_FIT_BACKGROUND, false);

        if (pagesStrip != null) {
            for (int pageIndex = 0; pageIndex < Math.min(2, pagesStrip.getChildCount()); pageIndex++) {
                View pageView = pagesStrip.getChildAt(pageIndex);
                if (!(pageView instanceof FrameLayout)) continue;
                FrameLayout page = (FrameLayout) pageView;
                for (int childIndex = 0; childIndex < page.getChildCount(); childIndex++) {
                    View child = page.getChildAt(childIndex);
                    Object rawTag = child.getTag();
                    if (!(rawTag instanceof String)) continue;
                    String workspaceTag = (String) rawTag;
                    if ("launcher_contact_shortcut".equals(workspaceTag)) {
                        if (child instanceof ViewGroup
                                && ((ViewGroup) child).getChildCount() > 0
                                && ((ViewGroup) child).getChildAt(0) instanceof ImageView) {
                            updateShortcutIconAppearance(
                                    (ImageView) ((ViewGroup) child).getChildAt(0), child,
                                    artworkTheme, backgroundColor);
                        }
                        continue;
                    }
                    if (!workspaceTag.startsWith("launcher_app:")) continue;
                    String packageName = workspaceTag.substring("launcher_app:".length());
                    ResolveInfo app = findApp(packageName);
                    if (app == null || !(child instanceof ViewGroup)
                            || ((ViewGroup) child).getChildCount() == 0) continue;
                    View iconView = ((ViewGroup) child).getChildAt(0);
                    if (iconView instanceof ImageView) {
                        updateIconAppearance((ImageView) iconView, app, showBackground, shape,
                                backgroundColor, artworkTheme, fitInsideBackground);
                    }
                }
            }
        }

        // Update dock icons in place instead of rebuilding its views during a slider drag.
        if (dockContainer != null) {
            for (int i = 0; i < dockContainer.getChildCount(); i++) {
                View slot = dockContainer.getChildAt(i);
                if (!(slot instanceof ViewGroup)) continue;
                ViewGroup group = (ViewGroup) slot;
                for (int j = 0; j < group.getChildCount(); j++) {
                    View child = group.getChildAt(j);
                    if (child instanceof ImageView) {
                        updateIconAppearance((ImageView) child, null, showBackground, shape,
                                backgroundColor, artworkTheme, fitInsideBackground);
                    }
                }
            }
        }

        // Only visible drawer cells need immediate invalidation; off-screen cells
        // pick up the saved colours naturally when GridView next binds them.
        if (drawerGrid != null) {
            for (int i = 0; i < drawerGrid.getChildCount(); i++) {
                View cell = drawerGrid.getChildAt(i);
                if (!(cell instanceof ViewGroup)) continue;
                ViewGroup group = (ViewGroup) cell;
                if (group.getChildCount() > 0 && group.getChildAt(0) instanceof ImageView) {
                    updateIconAppearance((ImageView) group.getChildAt(0), null, showBackground,
                            shape, backgroundColor, artworkTheme, fitInsideBackground);
                }
            }
        }
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
                            LauncherIconDrawable.clampShape(prefs.getInt("app_icon_shape",
                                    LauncherIconDrawable.SHAPE_ORIGINAL)),
                            LauncherIconDrawable.backgroundColorFromPreferences(prefs),
                            LauncherIconDrawable.clampArtworkTheme(prefs.getInt(KEY_ICON_ARTWORK_THEME,
                                    LauncherIconDrawable.ARTWORK_ORIGINAL)),
                            packageName + "/" + app.activityInfo.name,
                             prefs.getBoolean(KEY_ICON_FIT_BACKGROUND, false)));
                    icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    FrameLayout.LayoutParams iconParams = new FrameLayout.LayoutParams(
                            LauncherUiUtils.dp(this, iconSize), LauncherUiUtils.dp(this, iconSize), Gravity.CENTER);
                    // Move each icon 5dp inward so the gap between the two dock apps
                    // is reduced by 10dp total without changing either hit target.
                    if (slot == 0) iconParams.leftMargin = LauncherUiUtils.dp(this, 10);
                    else if (slot == 2) iconParams.rightMargin = LauncherUiUtils.dp(this, 10);
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
        liveDragRejectedMoves.remove(movingView);
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
                        // App icons arm without showing the workspace grid until the
                        // finger actually starts dragging. Highlight groups immediately
                        // so they are visible as move targets during that ready state.
                        workspaceMoveModeHighlightActive = true;
                        refreshAppGroupMoveModeHighlights();
                        LauncherUiUtils.startMoveHint(view, this);
                    }
                };
                iconShowOptions = () -> {
                    if (activeDragView == view && !iconDragging) {
                        LauncherUiUtils.stopMoveHint(view);
                        workspaceMoveModeHighlightActive = false;
                        refreshAppGroupMoveModeHighlights();
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
                    int rows = homeGridRowsForHeight(targetHeightForGrid, slotHeight);
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
                    rows = homeGridRowsForHeight(targetHeight, cellHeight);

                    // Use the actual dragged cell boundary, matching the rectangle that follows
                    // the finger during MOVE. Do not use the artwork glyph's smaller visual bounds.
                    int dragBoundaryWidth = Math.max(1,
                            view.getWidth() > 0 ? view.getWidth() : cellWidth);
                    int dragBoundaryHeight = Math.max(1,
                            view.getHeight() > 0 ? view.getHeight() : cellHeight);
                    float desiredBoundaryLeft = event.getRawX() - viewportLocation[0]
                            - dragBoundaryWidth / 2f;
                    float desiredBoundaryTop = event.getRawY()
                            - viewportLocation[1] - iconDragFingerOffsetY;
                    int boundaryLeft = Math.max(0, Math.min(
                            Math.max(0, targetWidth - dragBoundaryWidth),
                            Math.round(desiredBoundaryLeft)));
                    // Keep the true release rectangle. Clamping it to the first/last
                    // grid row would artificially snap drops made outside the grid into
                    // a legal cell and bypass the 50% overlap threshold.
                    int boundaryTop = Math.max(0, Math.min(
                            Math.max(0, targetHeight - dragBoundaryHeight),
                            Math.round(desiredBoundaryTop)));

                    // Generic placement follows the greatest-overlap grid cell; app-group drops
                    // are detected separately against the actual target grid cell, so an adjacent
                    // cell cannot steal a qualifying 50% group drop.
                    int horizontalDirection = event.getRawX() > iconTouchRawX ? 1
                            : event.getRawX() < iconTouchRawX ? -1 : 0;
                    int verticalDirection = event.getRawY() > iconTouchRawY ? 1
                            : event.getRawY() < iconTouchRawY ? -1 : 0;
                    int[] majorityOverlapSlot = findHomeGridSlotForDragBoundary(
                            boundaryLeft, boundaryTop, dragBoundaryWidth, dragBoundaryHeight,
                            targetWidth, targetHeight, columns, rows, cellWidth, cellHeight,
                            horizontalDirection, verticalDirection);
                    boolean boundaryHasMajorityCell = majorityOverlapSlot != null;
                    int targetCol = boundaryHasMajorityCell ? majorityOverlapSlot[0]
                            : Math.max(0, Math.min(columns - 1,
                                    Math.round(boundaryLeft / (float) cellWidth)));
                    int targetRow = boundaryHasMajorityCell ? majorityOverlapSlot[1]
                            : homeGridRowFromTop(boundaryTop, rows, targetHeight, cellHeight);
                    String targetGroupId = findAppGroupAtDragBoundary(targetPage,
                            boundaryLeft, boundaryTop, dragBoundaryWidth, dragBoundaryHeight,
                            cellWidth, cellHeight);
                    if (targetGroupId == null && boundaryHasMajorityCell) {
                        targetGroupId = findAppGroupAtGridSlot(
                                targetPage, targetCol, targetRow, cellWidth, cellHeight);
                    }
                    final String dropTargetGroupId = targetGroupId;
                    if (dropTargetGroupId != null) {
                        String groupLabel = getAppGroupLabel(dropTargetGroupId);
                        ResolveInfo draggedApp = findApp(packageName);
                        String appLabel = draggedApp == null ? packageName
                                : String.valueOf(draggedApp.loadLabel(packageManager));
                        FrameLayout.LayoutParams rollbackParams =
                                (FrameLayout.LayoutParams) view.getLayoutParams();
                        rollbackParams.leftMargin = iconOriginalLeft;
                        rollbackParams.topMargin = iconOriginalTop;
                        view.setTranslationX(0f);
                        view.setTranslationY(0f);
                        view.setScaleX(1f);
                        view.setScaleY(1f);
                        view.setAlpha(1f);
                        view.setLayoutParams(rollbackParams);
                        finishWorkspaceTransferDrag(view, false);
                        finishLiveDisplacements(false, view);
                        restoreIconDragPageVisuals();
                        setWorkspaceGridVisible(false, null);
                        iconDragging = false;
                        iconDragArmed = false;
                        activeDragView = null;
                        activeDragPackage = null;
                        iconDragPageIndex = -1;
                        iconOriginalPageIndex = -1;
                        iconEdgeLatch = 0;
                        if (getAppGroupMembers(dropTargetGroupId).contains(packageName)) {
                            Toast.makeText(MainActivity.this,
                                    "This app is already in the group.", Toast.LENGTH_SHORT).show();
                            return true;
                        }
                        new android.app.AlertDialog.Builder(MainActivity.this)
                                .setTitle("Add to " + groupLabel)
                                .setMessage("Add " + appLabel + " to this app group?")
                                .setNegativeButton("Cancel", null)
                                .setPositiveButton("Add", (groupDialog, button) ->
                                        moveHomeAppIntoGroup(dropTargetGroupId, packageName))
                                .show();
                        return true;
                    }
                    int proposedIconLeft = targetCol * cellWidth;
                    int proposedIconTop = homeGridRowTop(targetRow, rows, targetHeight, cellHeight);
                    FrameLayout.LayoutParams snappedCandidate =
                            new FrameLayout.LayoutParams(cellWidth, cellHeight, Gravity.TOP | Gravity.LEFT);
                    snappedCandidate.leftMargin = proposedIconLeft;
                    snappedCandidate.topMargin = proposedIconTop;
                    view.setTranslationX(0f);
                    view.setTranslationY(0f);
                    view.setLayoutParams(snappedCandidate);
                    if (boundaryHasMajorityCell) updateLiveDisplacements(targetPage, view);
                    if (!boundaryHasMajorityCell || !canPlaceWorkspaceItem(targetPage, view,
                            proposedIconLeft, proposedIconTop, cellWidth, cellHeight)) {
                        FrameLayout.LayoutParams rollbackParams =
                                (FrameLayout.LayoutParams) view.getLayoutParams();
                        rollbackParams.leftMargin = iconOriginalLeft;
                        rollbackParams.topMargin = iconOriginalTop;
                        view.setTranslationX(0f);
                        view.setTranslationY(0f);
                        view.setScaleX(1f);
                        view.setScaleY(1f);
                        view.setAlpha(1f);
                        view.setLayoutParams(rollbackParams);
                        finishWorkspaceTransferDrag(view, false);
                        finishLiveDisplacements(false, view);
                        restoreIconDragPageVisuals();
                        setWorkspaceGridVisible(false, null);
                        iconDragging = false;
                        iconDragArmed = false;
                        activeDragView = null;
                        activeDragPackage = null;
                        iconDragPageIndex = -1;
                        iconOriginalPageIndex = -1;
                        iconEdgeLatch = 0;
                        if (boundaryHasMajorityCell) {
                            showWorkspaceOverlapRejected();
                        } else {
                            Toast.makeText(this,
                                    "Move at least half of the drag boundary into a grid cell.",
                                    Toast.LENGTH_SHORT).show();
                        }
                        return true;
                    }
                    int sourceCellWidth = Math.max(1, sourceView.getWidth() / columns);
                    int sourceCellHeight = slotHeight;
                    int sourceRows = homeGridRowsForHeight(
                            sourceView.getHeight(), sourceCellHeight);
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
                    finishWorkspaceTransferDrag(view, true);
                    int committedPage = destinationPage;
                    boolean displacementAccepted = finishLiveDisplacements(true, view);
                    if (!displacementAccepted) {
                        // Do not persist a drop after the live displacement planner has
                        // rejected its transaction. In particular, undo an app/app swap
                        // before rebuilding, or the rejected destination can be reflowed
                        // into a neighbouring cell and appear to have ignored the drop.
                        saveIconSlot(packageName, sourceCol, sourceRow, columns);
                        android.content.SharedPreferences.Editor restoreSlots = prefs.edit()
                                .putInt(KEY_ICON_PAGE + packageName, sourcePage);
                        if (otherPackage != null) {
                            saveIconSlot(otherPackage, targetCol, targetRow, columns);
                            restoreSlots.putInt(KEY_ICON_PAGE + otherPackage, destinationPage);
                        }
                        restoreSlots.apply();
                        currentPage = sourcePage;
                        restoreIconDragPageVisuals();
                        setWorkspaceGridVisible(false, null);
                        iconDragging = false;
                        iconDragArmed = false;
                        activeDragView = null;
                        activeDragPackage = null;
                        iconDragPageIndex = -1;
                        iconOriginalPageIndex = -1;
                        iconEdgeLatch = 0;
                        renderHomePages(false);
                        showPage(sourcePage, false);
                        return true;
                    }
                    saveIconSlot(packageName, targetCol, targetRow, columns);
                    prefs.edit().putInt(KEY_ICON_PAGE + packageName, committedPage).apply();
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
                            // Collision checks happened before commit; do not reflow peers.
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
                workspaceMoveModeHighlightActive = false;
                refreshAppGroupMoveModeHighlights();
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
        View incomingFace = to >= 0 && to < pagesStrip.getChildCount()
                ? pagesStrip.getChildAt(to) : null;

        if (effect == 3) {
            // The 3D projection can extend the bottom edge beyond the viewport.
            // Disable only the outer clipping/cached surfaces for this brief turn;
            // the outgoing/incoming page textures stay hardware accelerated.
            setPageFlipOverflowEnabled(true);
            // Page Flip hinges each face around the top horizontal edge. Align the
            // incoming page once beneath the outgoing page; no horizontal motion or
            // vertical strip travel is animated during the turn.
            pagesStrip.setTranslationX(-from * width);
            outgoing.setCameraDistance(10000f * density);
            outgoing.setTranslationX(0f);
            outgoing.setPivotX(width * 0.5f);
            outgoing.setPivotY(0f);
            outgoing.setRotationY(0f);
            outgoing.setRotationX(-direction * 90f * progress);
            outgoing.setTranslationZ(-density * 8f * progress);
            outgoing.setScaleX(1f);
            outgoing.setScaleY(1f);
            outgoing.setAlpha(1f - progress);
            if (incomingFace != null) {
                incomingFace.setCameraDistance(10000f * density);
                // This is a fixed layout correction so both faces share the same
                // viewport, not an animated left/right slide.
                incomingFace.setTranslationX(-direction * width);
                incomingFace.setPivotX(incomingFace.getWidth() * 0.5f);
                incomingFace.setPivotY(0f);
                incomingFace.setRotationY(0f);
                incomingFace.setRotationX(direction * 90f * (1f - progress));
                incomingFace.setTranslationZ(density * 8f * progress);
                incomingFace.setScaleX(1f);
                incomingFace.setScaleY(1f);
                incomingFace.setAlpha(progress);
            }
            return;
        }

        // Other transitions use the neighbouring faces' real horizontal layout:
        // the strip travels only for these effects (cube needs a shared moving edge).
        pagesStrip.setTranslationX(-from * width - direction * width * progress);
        float outgoingAlpha = 1f - progress;
        float incomingAlpha = progress;
        if (effect == 1) {
            // Keep both cube faces at >=70% opacity for every frame.
            outgoingAlpha = 1f - 0.3f * progress;
            incomingAlpha = 0.7f + 0.3f * progress;
        }
        outgoing.setAlpha(outgoingAlpha);
        if (incomingFace != null) incomingFace.setAlpha(incomingAlpha);

        if (effect == 2) {
            outgoing.setPivotX(direction > 0 ? width : 0f);
            outgoing.setPivotY(height * 0.5f);
            outgoing.setScaleX(1f - 0.045f * progress);
            outgoing.setScaleY(1f - 0.025f * progress);
            if (incomingFace != null) {
                incomingFace.setPivotX(direction > 0 ? 0f : incomingFace.getWidth());
                incomingFace.setPivotY(incomingFace.getHeight() * 0.5f);
                incomingFace.setScaleX(0.955f + 0.045f * progress);
                incomingFace.setScaleY(0.975f + 0.025f * progress);
            }
            return;
        }

        outgoing.setCameraDistance(10000f * density);
        if (effect == 1) {
            // Fixed 90-degree cube faces. The strip advances with the finger so
            // the outgoing and incoming faces share a hinge that travels across
            // the viewport: the first face folds away and the next enters from right.
            final float cubeAngle = 90f;
            outgoing.setPivotX(direction > 0 ? width : 0f);
            outgoing.setPivotY(height * 0.5f);
            outgoing.setRotationY(-direction * cubeAngle * progress);
            outgoing.setTranslationZ(-density * 28f * progress);
            outgoing.setTranslationX(0f);
            if (incomingFace != null) {
                incomingFace.setCameraDistance(10000f * density);
                incomingFace.setPivotX(direction > 0 ? 0f : incomingFace.getWidth());
                incomingFace.setPivotY(incomingFace.getHeight() * 0.5f);
                incomingFace.setRotationY(direction * cubeAngle * (1f - progress));
                incomingFace.setTranslationZ(density * 28f * progress);
                incomingFace.setTranslationX(0f);
            }
        } else {
            // 3D Wheel: broad rotation around the page edge, controlled by the rotation slider.
            float angle = Math.min(180f, getPageRotationDegrees());
            outgoing.setPivotX(direction > 0 ? width : 0f);
            outgoing.setPivotY(height * 0.5f);
            outgoing.setRotationY(direction * angle * progress);
            outgoing.setScaleX(1f - 0.035f * progress);
            outgoing.setScaleY(1f - 0.025f * progress);
            if (incomingFace != null) {
                incomingFace.setCameraDistance(10000f * density);
                incomingFace.setPivotX(direction > 0 ? 0f : incomingFace.getWidth());
                incomingFace.setPivotY(incomingFace.getHeight() * 0.5f);
                incomingFace.setRotationY(-direction * angle * (1f - progress));
                incomingFace.setScaleX(0.965f + 0.035f * progress);
                incomingFace.setScaleY(0.975f + 0.025f * progress);
            }
        }
    }

    private void cancelPageSettleAnimatorForReplacement() {
        android.animation.ValueAnimator old = pageSettleAnimator;
        pageSettleAnimator = null;
        if (old != null) old.cancel();
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
        cancelPageSettleAnimatorForReplacement();
        pageTransitionTargetPage = to;
        pagesStrip.animate().cancel();

        if (direction == 0 || from + direction < 0
                || from + direction >= pagesStrip.getChildCount()) {
            resetPageTransforms();
            pagesStrip.setTranslationX(-from * (float) width);
            wheelSwipeProgress = 0f;
            wheelSwipeDirection = 0;
            pageTransitionBusy = false;
            pageTransitionTargetPage = -1;
            pageSettleAnimator = null;
            startClockColorAnimator();
            return;
        }

        final android.animation.ValueAnimator animator =
                android.animation.ValueAnimator.ofFloat(startProgress, endProgress);
        pageSettleAnimator = animator;
        animator.setDuration(animationDuration(to == from ? 170L : 260L));
        animator.setInterpolator(new android.view.animation.DecelerateInterpolator(1.6f));
        animator.addUpdateListener(value -> {
            wheelSwipeProgress = ((Number) value.getAnimatedValue()).floatValue();
            applyWheelSwipe(direction, wheelSwipeProgress);
        });
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            private boolean cancelled;

            @Override public void onAnimationCancel(android.animation.Animator animation) {
                // A second touch, page rebuild, or lifecycle interruption can cancel
                // the animator. Never leave a cube face rotated or the strip between pages.
                cancelled = true;
            }

            @Override public void onAnimationEnd(android.animation.Animator animation) {
                // Replaced animators must not reset state after a newer reverse settle starts.
                if (pageSettleAnimator != animator) return;
                int settledPage = cancelled ? from : to;
                currentPage = settledPage;
                activateClockForPage(currentPage);
                pagesStrip.setTranslationX(-settledPage * (float) width);
                resetPageTransforms();
                releasePageLayers();
                wheelSwipeProgress = 0f;
                wheelSwipeDirection = 0;
                pageTransitionTargetPage = -1;
                if (pageSettleAnimator == animator) pageSettleAnimator = null;
                // Avoid invalidating the newly settled page's cached texture on the
                // exact final frame of its entrance animation.
                clockGradientResumeAtUptime = android.os.SystemClock.uptimeMillis() + 180L;
                pageTransitionBusy = false;
                startClockColorAnimator();
                if (pageIndicator != null) {
                    boolean show = pages.size() > 1 && prefs.getBoolean(KEY_SHOW_PAGE_INDICATOR, true);
                    pageIndicator.setVisibility(show ? View.VISIBLE : View.GONE);
                    pageIndicator.setText((currentPage + 1) + "  /  " + pages.size());
                }
            }
        });
        animator.start();
    }

    private void setPageFlipOverflowEnabled(boolean enabled) {
        if (enabled) {
            if (pageFlipOverflowEnabled) return;
            pageFlipOverflowEnabled = true;
            if (homeScreen != null) {
                pageFlipSavedHomeClipChildren = homeScreen.getClipChildren();
                pageFlipSavedHomeClipToPadding = homeScreen.getClipToPadding();
                homeScreen.setClipChildren(false);
                homeScreen.setClipToPadding(false);
            }
            if (pageViewport != null) {
                pageFlipSavedViewportClipChildren = pageViewport.getClipChildren();
                pageFlipSavedViewportClipToPadding = pageViewport.getClipToPadding();
                pageFlipSavedViewportLayerType = pageViewport.getLayerType();
                pageViewport.setClipChildren(false);
                pageViewport.setClipToPadding(false);
                // The viewport's fixed-size hardware layer was clipping the rotated
                // lower edge even though the incoming page itself was still moving.
                pageViewport.setLayerType(View.LAYER_TYPE_NONE, null);
            }
            if (pagesStrip != null) {
                pageFlipSavedStripClipChildren = pagesStrip.getClipChildren();
                pageFlipSavedStripClipToPadding = pagesStrip.getClipToPadding();
                pageFlipSavedStripLayerType = pagesStrip.getLayerType();
                pagesStrip.setClipChildren(false);
                pagesStrip.setClipToPadding(false);
                pagesStrip.setLayerType(View.LAYER_TYPE_NONE, null);
            }
        } else {
            if (!pageFlipOverflowEnabled) return;
            pageFlipOverflowEnabled = false;
            if (homeScreen != null) {
                homeScreen.setClipChildren(pageFlipSavedHomeClipChildren);
                homeScreen.setClipToPadding(pageFlipSavedHomeClipToPadding);
            }
            if (pageViewport != null) {
                pageViewport.setClipChildren(pageFlipSavedViewportClipChildren);
                pageViewport.setClipToPadding(pageFlipSavedViewportClipToPadding);
                pageViewport.setLayerType(pageFlipSavedViewportLayerType, null);
            }
            if (pagesStrip != null) {
                pagesStrip.setClipChildren(pageFlipSavedStripClipChildren);
                pagesStrip.setClipToPadding(pageFlipSavedStripClipToPadding);
                pagesStrip.setLayerType(pageFlipSavedStripLayerType, null);
            }
        }
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
        }
        // Restore clipping only after the destination page is aligned and unrotated.
        setPageFlipOverflowEnabled(false);
    }

    /**
     * Keep hardware display lists warm for only the visible page and its neighbour.
     * The active gesture then changes transforms on cached surfaces instead of
     * paying the layer-creation cost during the first frames of a swipe.
     */
    private void releasePageLayers() {
        if (pagesStrip == null) return;
        // Page 2 is the expensive forward-transition destination on this launcher.
        // Keep only its composed texture warm while idle; drop page 1's texture to
        // avoid permanently caching both whole-screen surfaces on older devices.
        int cachedPage = Math.min(1, pagesStrip.getChildCount() - 1);
        for (int i = 0; i < pagesStrip.getChildCount(); i++) {
            View page = pagesStrip.getChildAt(i);
            page.setLayerType(i == cachedPage ? View.LAYER_TYPE_HARDWARE : View.LAYER_TYPE_NONE, null);
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
        LauncherScreenTransitions.showHome(this, homeScreen, drawerScreen,
                () -> { drawerOpen = false; scheduleDrawerArtworkPrewarmAfterHomeTransition(); },
                getDrawerTransition());
    }

    /** Rebuild the durable drawer viewport after a close transition returns control to Home. */
    private void scheduleDrawerArtworkPrewarmAfterHomeTransition() {
        handler.postDelayed(() -> {
            if (isFinishing() || drawerOpen || homeScreen == null
                    || homeScreen.getVisibility() != View.VISIBLE) return;
            scheduleDrawerArtworkPrewarm();
        }, 500L);
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
        LauncherScreenTransitions.cancelTrackedDrawerTransitions();
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
        scheduleDrawerArtworkPrewarmAfterHomeTransition();
    }

    private void handleDrawerCachePreferenceChanged(String key) {
        if (isAppGroupAppearancePreference(key) && root != null && !isFinishing()) {
            root.post(this::renderAppGroups);
        }
        if (!isDrawerCachePreference(key)) return;
        // Preference changes run on the UI thread. Refresh the cached frame while Home is visible
        // so the next drawer opening can continue showing its last complete image meanwhile.
        if (root != null && !isFinishing()) {
            root.post(() -> {
                scheduleDrawerArtworkPrewarm();
                refreshDrawerIconAppearances();
            });
        }
    }

    private boolean isDrawerCachePreference(String key) {
        if (key == null) return false;
        return KEY_ICON_ARTWORK_THEME.equals(key)
                || KEY_ICON_BACKGROUND_COLOR.equals(key)
                || KEY_ICON_BACKGROUND_COLOR_POSITION.equals(key)
                || KEY_ICON_FIT_BACKGROUND.equals(key)
                || "icon_rounded_square_background".equals(key)
                || "app_icon_shape".equals(key)
                || KEY_DRAWER_ICON_SIZE.equals(key)
                || KEY_DRAWER_FONT_SIZE.equals(key)
                || KEY_SHOW_DRAWER_LABELS.equals(key)
                || KEY_DRAWER_COLUMNS.equals(key)
                || KEY_DRAWER_SPACING.equals(key)
                || KEY_DRAWER_OPACITY.equals(key)
                || KEY_COLOR_THEME.equals(key)
                || KEY_DRAWER_DESCENDING.equals(key);
    }

    private void scheduleDrawerArtworkPrewarm() {
        if (drawerScreen == null || drawerAdapter == null) return;
        // Keep the actual drawer laid out invisibly while Home is displayed. Per-app artwork is
        // loaded/generated for the complete app list; no viewport screenshot is saved or painted.
        if (!drawerOpen && homeScreen != null && homeScreen.getVisibility() == View.VISIBLE
                && drawerScreen.getVisibility() == View.GONE) {
            drawerScreen.setVisibility(View.INVISIBLE);
            drawerScreen.requestLayout();
        }
        if (!drawerOpen && homeScreen != null && homeScreen.getVisibility() == View.VISIBLE) {
            drawerAdapter.prewarmAllDrawerArtwork();
        }
        handler.removeCallbacks(drawerArtworkReadyCheck);
        drawerArtworkReadyChecks = 0;
        handler.postDelayed(drawerArtworkReadyCheck, 100L);
    }

    private void markDrawerOpenAndRefreshArtwork() {
        drawerOpen = true;
        handler.removeCallbacks(drawerArtworkReadyCheck);
        drawerArtworkReadyChecks = 0;
        handler.postDelayed(drawerArtworkReadyCheck, 100L);
    }

    private void showDrawer() { showDrawer(0f); }

    private void refreshDrawerIconAppearances() {
        if (drawerAdapter != null) {
            drawerAdapter.refreshVisibleIconAppearance(drawerGrid);
        }
    }

    private void showDrawer(float swipeProgress) {


        LauncherScreenTransitions.showDrawer(this, homeScreen, drawerScreen,
                this::markDrawerOpenAndRefreshArtwork, getDrawerTransition(),
                Math.max(0f, Math.min(1f, swipeProgress)));
    }

    private void scheduleDrawerSwipeFrame(float progress) {
        drawerSwipePendingProgress = Math.max(0f, Math.min(1f, progress));
        drawerSwipePendingEffect = getDrawerTransition();
        if (root == null) {
            applyDrawerSwipeFrame();
            return;
        }
        if (!drawerSwipeFrameScheduled) {
            drawerSwipeFrameScheduled = true;
            root.postOnAnimation(drawerSwipeFrame);
        }
    }

    private void applyDrawerSwipeFrame() {
        if (homeScreen == null || drawerScreen == null) return;
        // The real GridView stays live during swipe-driven transitions.
        LauncherScreenTransitions.updateDrawerSwipe(this, homeScreen, drawerScreen,
                drawerSwipePendingEffect, drawerSwipePendingProgress);
    }

    /** Ensure the last finger position is rendered before starting the settle animation. */
    private void flushDrawerSwipeFrame() {
        if (!drawerSwipeFrameScheduled) return;
        if (root != null) root.removeCallbacks(drawerSwipeFrame);
        drawerSwipeFrameScheduled = false;
        applyDrawerSwipeFrame();
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
        if (page != null) {
            for (int i = 0; i < page.getChildCount(); i++) {
                View child = page.getChildAt(i);
                if (!isAppGroupItem(child)
                        || !(child.getLayoutParams() instanceof FrameLayout.LayoutParams)) continue;
                FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) child.getLayoutParams();
                int col = Math.max(0, Math.min(columns - 1,
                        Math.round(lp.leftMargin / (float) cellWidth)));
                int row = homeGridRowFromTop(lp.topMargin, rows, height, cellHeight);
                occupied[row][col] = true;
            }
        }
        if (!sawAppView) {
            for (String existing : homePackages) {
                if (prefs.getInt(KEY_ICON_PAGE + existing, 0) != pageIndex) continue;
                float sx = getFloatPreferenceCompat(KEY_ICON_X + existing, 0f);
                float sy = getFloatPreferenceCompat(KEY_ICON_Y + existing, 0f);
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
                                || tag.startsWith(APP_GROUP_TAG_PREFIX)
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
        if (requestCode == REQUEST_HOME_ROLE) {
            if (Build.VERSION.SDK_INT >= 29) {
                if (Api29HomeRoleManager.isHomeRoleHeld(this)) {
                    Toast.makeText(this, "PhoneBridge is now the default Home app.",
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this,
                            "PhoneBridge is not the default Home app. Select it under Settings > Default apps > Home app.",
                            Toast.LENGTH_LONG).show();
                }
            }
            return;
        }
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
    /**
     * Validate the exact size and target position of a cross-page widget drop.
     * Never move neighbours or silently shrink the widget to force a fit.
     */
    private int[] prepareWorkspaceWidgetDrop(FrameLayout targetPage, View widget,
            float rawX, float rawY, float fingerOffsetX, float fingerOffsetY) {
        if (targetPage == null || widget == null || targetPage.getWidth() <= 0
                || targetPage.getHeight() <= 0 || workspaceTransferItem != widget
                || workspaceTransferTargetPage < 0 || pagesStrip == null
                || workspaceTransferTargetPage >= pagesStrip.getChildCount()) return null;
        ViewGroup.LayoutParams raw = widget.getLayoutParams();
        if (!(raw instanceof FrameLayout.LayoutParams)) return null;
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) raw;
        int width = params.width > 0 ? params.width : widget.getWidth();
        int height = params.height > 0 ? params.height : widget.getHeight();
        int left = params.leftMargin;
        int top = params.topMargin;
        if (workspaceTransferProxy != null && workspaceTransferProxy.getParent() == targetPage) {
            updateLiveDisplacements(targetPage, workspaceTransferProxy);
        }
        if (!canPlaceWorkspaceItem(targetPage, widget, left, top, width, height)) return null;
        return new int[]{left, top, width, height};
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
                    || tag.startsWith(APP_GROUP_TAG_PREFIX)
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
        addWidgetContainer(page, widget, widgetId, pageIndex, x, y, width, height,
                showNoSpaceToast, false);
    }

    private void addWidgetContainer(FrameLayout page, AppWidgetHostView widget, int widgetId, int pageIndex,
            int x, int y, int width, int height, boolean showNoSpaceToast, boolean preserveGeometry) {
        widgetInteractionController.addWidgetContainer(
                page, widget, widgetId, pageIndex, x, y, width, height,
                showNoSpaceToast, preserveGeometry);
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
                    // Saved geometry is authoritative during restoration. Do not impose
                    // new minimum dimensions or move a widget to another screen just because
                    // app icons/provider views are still rebuilding after an app/launcher update.
                    // Only clamp to the current page bounds if the available workspace changed.
                    int width = Math.max(1, Math.min(
                            prefs.getInt("widget_w_" + id, defaultWidth), availableWidth));
                    int height = Math.max(1, Math.min(
                            prefs.getInt("widget_h_" + id, defaultHeight), availableHeight));
                    int x = Math.max(0, Math.min(
                            prefs.getInt("widget_x_" + id, LauncherUiUtils.dp(this, 8)),
                            Math.max(0, availableWidth - width)));
                    int y = Math.max(0, Math.min(
                            prefs.getInt("widget_y_" + id, LauncherUiUtils.dp(this, 8)),
                            Math.max(0, availableHeight - height)));
                    FrameLayout restorePage = page;
                    int resolvedPageIndex = pageIndex;
                    AppWidgetHostView widget = widgetHost.createView(this, id, info);
                    widget.setAppWidget(id, info);
                    addWidgetContainer(restorePage, widget, id, resolvedPageIndex,
                            x, y, width, height, false, true);
                    WidgetController.updateWidgetSize(widgetManager, id, widget,
                            getResources().getDisplayMetrics().density, width, height);
                    final View candidate = restorePage.findViewWithTag("launcher_widget:" + id);
                    if (candidate instanceof FrameLayout) {
                        // Persist only the geometry actually restored (including any
                        // unavoidable page-bound clamp), without a second collision pass.
                        saveWidgetGeometry(id, resolvedPageIndex, (FrameLayout) candidate);
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

    private View findViewAtExcluding(View view, int rawX, int rawY, View excluded) {
        if (view == null || view == excluded || view.getVisibility() != View.VISIBLE) return null;
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        int localX = rawX - location[0];
        int localY = rawY - location[1];
        if (localX < 0 || localY < 0 || localX >= view.getWidth()
                || localY >= view.getHeight()) return null;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = group.getChildCount() - 1; i >= 0; i--) {
                View found = findViewAtExcluding(group.getChildAt(i), rawX, rawY, excluded);
                if (found != null) return found;
            }
        }
        return view;
    }

    private String touchStartedOnAppGroup(float rawX, float rawY) {
        View target = findViewAt(root, (int) rawX, (int) rawY);
        while (target != null) {
            if (isAppGroupItem(target)) return appGroupId(target);
            if (target == root) break;
            android.view.ViewParent parent = target.getParent();
            target = parent instanceof View ? (View) parent : null;
        }
        return null;
    }

    private void clearAppGroupMoveModeIfTouchOutside(float rawX, float rawY) {
        if (activeAppGroupMoveModeId == null) return;
        String touchedGroup = touchStartedOnAppGroup(rawX, rawY);
        if (activeAppGroupMoveModeId.equals(touchedGroup)) return;
        String previousId = activeAppGroupMoveModeId;
        activeAppGroupMoveModeId = null;
        setWorkspaceGridVisible(false, null);
        if (pagesStrip == null) return;
        for (int p = 0; p < pagesStrip.getChildCount(); p++) {
            View pageView = pagesStrip.getChildAt(p);
            if (!(pageView instanceof ViewGroup)) continue;
            ViewGroup page = (ViewGroup) pageView;
            for (int i = 0; i < page.getChildCount(); i++) {
                View child = page.getChildAt(i);
                if (isAppGroupItem(child) && previousId.equals(appGroupId(child))) {
                    LauncherUiUtils.stopMoveHint(child);
                    setAppGroupMoveModeVisual(child, false);
                    break;
                }
            }
        }
        // Recompute every group from the final mode state so no other group keeps
        // a stale blue overlay after move mode is dismissed by an outside tap.
        refreshAppGroupMoveModeHighlights();
    }

    private boolean touchStartedOnAppCell(float rawX, float rawY) {
        View target = findViewAt(root, (int) rawX, (int) rawY);
        while (target != null) {
            if ("launcher_app_cell".equals(target.getTag())
                    || isAppGroupItem(target)
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

    private boolean isViewDescendantOf(View candidate, View ancestor) {
        View current = candidate;
        while (current != null) {
            if (current == ancestor) return true;
            android.view.ViewParent parent = current.getParent();
            current = parent instanceof View ? (View) parent : null;
        }
        return false;
    }

    /**
     * Drawer cells may receive a hit while a 3D drawer transition still exposes the
     * home surface. Only launch from the drawer once its open state and hit surface agree.
     * Home cells and standalone shortcut launches are unaffected.
     */
    private boolean canLaunchAppFromSource(View sourceView) {
        if (sourceView == null || drawerGrid == null
                || !isViewDescendantOf(sourceView, drawerGrid)) return true;
        return drawerOpen && drawerScreen != null
                && drawerScreen.getVisibility() == View.VISIBLE
                && !LauncherScreenTransitions.isDrawerTransitionRunning(
                        homeScreen, drawerScreen, getDrawerTransition());
    }

    private void launchApp(ResolveInfo app, View sourceView) {
        if (app == null || app.activityInfo == null || !canLaunchAppFromSource(sourceView)) return;
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

    private boolean isClockIconArtworkActive() {
        return prefs != null
                && prefs.getBoolean(KEY_CLOCK_ICON_ARTWORK, false)
                && !prefs.getBoolean(KEY_CLOCK_DIGIT_GRADIENT, false)
                && !prefs.getBoolean(KEY_CLOCK_LIVE_GRADIENT, false);
    }

    private boolean shouldApplyClockGradientInnerBorder() {
        if (prefs == null || !prefs.getBoolean(KEY_CLOCK_DIGIT_GRADIENT, false)) return false;
        int artworkTheme = LauncherIconDrawable.clampArtworkTheme(prefs.getInt(
                KEY_ICON_ARTWORK_THEME, LauncherIconDrawable.ARTWORK_ORIGINAL));
        boolean nothingArtwork = artworkTheme == LauncherIconDrawable.ARTWORK_NOTHING_MONOCHROME;
        boolean artworkMatchingEnabled = prefs.getBoolean(KEY_CLOCK_ICON_ARTWORK, false);

        // Preserve the original gradient inner border for every non-Nothing artwork theme.
        // Additionally enable it for Nothing Monochrome when artwork matching is disabled.
        // When Nothing artwork matching is enabled, leave its existing border treatment unchanged.
        return !nothingArtwork || !artworkMatchingEnabled;
    }

    private boolean shouldApplyClockDateInnerBorder() {
        if (prefs == null) return false;
        boolean gradientEnabled = prefs.getBoolean(KEY_CLOCK_DIGIT_GRADIENT, false);
        boolean artworkMatchingEnabled = prefs.getBoolean(KEY_CLOCK_ICON_ARTWORK, false);
        int artworkTheme = LauncherIconDrawable.clampArtworkTheme(prefs.getInt(
                KEY_ICON_ARTWORK_THEME, LauncherIconDrawable.ARTWORK_ORIGINAL));
        // The black inner contour supplements gradient/matched-artwork modes,
        // but never alters the dedicated Nothing Monochrome artwork treatment.
        return (gradientEnabled || artworkMatchingEnabled)
                && artworkTheme != LauncherIconDrawable.ARTWORK_NOTHING_MONOCHROME;
    }

    private void applyClockTextInnerBorder(TextView view, boolean enabled, float strokeWidthPx) {
        if (!(view instanceof SevenSegmentClockTextView)) return;
        SevenSegmentClockTextView renderer = (SevenSegmentClockTextView) view;
        renderer.setDigitOnlyInnerBorder(false);
        renderer.setContrastInnerBorderEnabled(enabled,
                CLOCK_DATE_GRADIENT_COLORS, strokeWidthPx,
                enabled ? CLOCK_WHITE_BORDER_COLORS : null);
    }

    private void applyClockDateBlackInnerBorder(TextView view, boolean enabled,
            float innerWidthPx) {
        if (!(view instanceof SevenSegmentClockTextView)) return;
        ((SevenSegmentClockTextView) view).setSecondaryInnerBorderEnabled(
                enabled, Color.BLACK, innerWidthPx);
    }

    private void refreshClockArtworkAppearance() {
        int restorePage = Math.max(0, Math.min(1, currentPage));
        // Restore each style's own typography/shadow before applying the chosen
        // artwork. This also removes the monochrome outline when the switch turns off.
        for (int page = 0; page < clockPanelsByPage.length; page++) {
            if (clockPanelsByPage[page] == null) continue;
            activateClockForPage(page);
            applyClockStyle();
        }
        activateClockForPage(restorePage);
        updateClock();
        // Artwork-theme changes affect the panel itself as well as the digits.
        // Refresh explicitly rather than waiting for resize or a colour-phase tick.
        applyClockPanelColor(false);
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
        // Rebuild the displayed text after renderer changes, even when the clock characters
        // are identical. The LED renderer may have installed a plain NORMAL buffer; standard
        // styles require the replacement spans used for regular text/colon drawing.
        clockDigitInnerBorderModeApplied.remove(clockText);
        // Always reassert the custom renderer mode. A cached style index is not
        // proof that the active TextView still has the correct drawing mode,
        // especially when switching between the two independent page clocks.
        if (previousStyle == null || previousStyle.intValue() != style
                || clockText instanceof SevenSegmentClockTextView) {
            LauncherClockStyler.apply(this, clockText, style);
        }
        // Keep the original solid silver tone unless a clock effect overrides it.
        clockText.setTextColor(CLOCK_DIGIT_SILVER);
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
        // Reassert a drawable TextView state; alpha/color left by a previous renderer must
        // not blank standard styles when the same clock view is reused.
        clockText.setVisibility(View.VISIBLE);
        clockText.setAlpha(1f);
        if (Color.alpha(clockText.getCurrentTextColor()) == 0) {
            clockText.setTextColor(CLOCK_DIGIT_SILVER);
        }
        clockText.requestLayout();
        clockText.invalidate();
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

    private void startClockColorAnimator() {
        if (prefs == null || !prefs.getBoolean(KEY_CLOCK_LIVE_GRADIENT, false)
                || !prefs.getBoolean(KEY_CLOCK_DIGIT_GRADIENT, false)) {
            // Fixed mode needs no per-transition shader invalidation. Its phase is
            // reset when the switch changes; page settle must not redraw both clocks.
            stopClockColorAnimator();
            clockColorPhase = 0f;
            return;
        }
        if (!clockUpdatesActive || pageTransitionBusy) return;
        handler.removeCallbacks(clockColorFrame);
        clockColorLastFrameUptime = 0L;
        long now = android.os.SystemClock.uptimeMillis();
        long waitForStablePage = Math.max(0L, clockGradientResumeAtUptime - now);
        handler.postDelayed(clockColorFrame, Math.max(100L, waitForStablePage));
    }

    private void stopClockColorAnimator() {
        handler.removeCallbacks(clockColorFrame);
        clockColorLastFrameUptime = 0L;
    }

    private void applyClockColorEffects(float phase, boolean bothPages) {
        int restorePage = Math.max(0, Math.min(1, currentPage));
        int firstPage = bothPages ? 0 : restorePage;
        int lastPage = bothPages ? clockPanelsByPage.length - 1 : restorePage;
        for (int page = firstPage; page <= lastPage; page++) {
            if (clockPanelsByPage[page] == null) continue;
            activateClockForPage(page);
            applyClockColorEffectsToActivePage(phase);
        }
        activateClockForPage(restorePage);
    }

    private int[] getClockArtworkTransitionColours() {
        int artworkTheme = prefs == null
                ? LauncherIconDrawable.ARTWORK_ORIGINAL
                : LauncherIconDrawable.clampArtworkTheme(prefs.getInt(
                        KEY_ICON_ARTWORK_THEME, LauncherIconDrawable.ARTWORK_ORIGINAL));
        int[] artworkColours =
                LauncherIconDrawable.clockTransitionPaletteForArtwork(artworkTheme);
        // Nothing Monochrome uses a restrained white-to-near-white gradient for
        // artwork-matched clock text, rather than inheriting the icon's dark regions.
        if (artworkTheme == LauncherIconDrawable.ARTWORK_NOTHING_MONOCHROME) {
            return new int[] { Color.WHITE, Color.rgb(245, 246, 249), Color.rgb(220, 224, 231) };
        }
        // "Original Colours" has no universal icon palette, so use the existing Home Studio
        // spectrum as its neutral fallback. The other themes use their own real icon palettes.
        return artworkColours != null && artworkColours.length >= 2
                ? artworkColours : CLOCK_DATE_GRADIENT_COLORS;
    }

    private void applyClockColorEffectsToActivePage(float phase) {
        boolean live = prefs != null && prefs.getBoolean(KEY_CLOCK_LIVE_GRADIENT, false);
        float displayPhase = live ? phase : 0f;
        boolean digitGradient = prefs != null
                && prefs.getBoolean(KEY_CLOCK_DIGIT_GRADIENT, false);
        float borderDensity = getResources().getDisplayMetrics().density;
        // Date/AM-PM outlines are drawn by ClockTextOutlineSpan so the black inner contour
        // stays above the fill in every mode without a second renderer contour over it.
        applyClockTextInnerBorder(dateText, false, 1f * borderDensity);
        applyClockTextInnerBorder(amPmText, false, 1f * borderDensity);
        applyClockDateBlackInnerBorder(dateText, false, 0.5f * borderDensity);
        applyClockDateBlackInnerBorder(amPmText, false, 0.5f * borderDensity);
        boolean iconArtworkActive = isClockIconArtworkActive();
        boolean useClockWhiteInnerBorder = digitGradient || iconArtworkActive;
        int artworkThemeForText = prefs == null
                ? LauncherIconDrawable.ARTWORK_ORIGINAL
                : LauncherIconDrawable.clampArtworkTheme(prefs.getInt(
                        KEY_ICON_ARTWORK_THEME, LauncherIconDrawable.ARTWORK_ORIGINAL));
        int[] artworkTextColours = iconArtworkActive
                ? getClockArtworkTransitionColours()
                : CLOCK_DATE_GRADIENT_COLORS;

        // Artwork matching uses the same selected palette gradient for the date and
        // AM/PM as the digits. Its phase stays fixed when the live-gradient option is off.
        if (dateText != null) {
            if (iconArtworkActive) {
                dateText.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT);
                applyAnimatedTextGradient(dateText, artworkTextColours, 0f, clockDateGradients);
            } else if (digitGradient) {
                dateText.setShadowLayer(LauncherUiUtils.dp(this, 2), 0f,
                        LauncherUiUtils.dp(this, 1), Color.argb(220, 0, 0, 0));
                applyAnimatedTextGradient(dateText, CLOCK_DATE_GRADIENT_COLORS,
                        displayPhase, clockDateGradients);
            } else {
                clockDateGradients.remove(dateText);
                if (dateText.getPaint().getShader() != null) {
                    dateText.getPaint().setShader(null);
                    dateText.invalidate();
                }
                if (dateText.getCurrentTextColor() != CLOCK_DIGIT_SILVER) {
                    dateText.setTextColor(CLOCK_DIGIT_SILVER);
                }
                dateText.setShadowLayer(LauncherUiUtils.dp(this, 2), 0f,
                        LauncherUiUtils.dp(this, 1), Color.argb(220, 0, 0, 0));
            }
        }
        if (amPmText != null) {
            if (digitGradient || iconArtworkActive) {
                applyAnimatedTextGradient(amPmText,
                        iconArtworkActive ? artworkTextColours : CLOCK_DATE_GRADIENT_COLORS,
                        digitGradient ? displayPhase : 0f, clockDateGradients);
            } else {
                clockDateGradients.remove(amPmText);
                if (amPmText.getPaint().getShader() != null) {
                    amPmText.getPaint().setShader(null);
                }
                amPmText.setTextColor(CLOCK_DIGIT_SILVER);
                amPmText.invalidate();
            }
        }
        if (clockText == null) return;
        boolean sevenSegment = clockText instanceof SevenSegmentClockTextView
                && prefs != null && prefs.getInt(KEY_CLOCK_STYLE, 0) == 5;
        boolean neonCircuit = prefs != null && prefs.getInt(KEY_CLOCK_STYLE, 0) == 2;
        int artworkTheme = prefs == null
                ? LauncherIconDrawable.ARTWORK_ORIGINAL
                : LauncherIconDrawable.clampArtworkTheme(prefs.getInt(
                        KEY_ICON_ARTWORK_THEME, LauncherIconDrawable.ARTWORK_ORIGINAL));
        boolean nothingArtworkMatch = iconArtworkActive
                && artworkTheme == LauncherIconDrawable.ARTWORK_NOTHING_MONOCHROME;
        int[] digitColours = nothingArtworkMatch && !neonCircuit
                ? CLOCK_NOTHING_MATCH_DIGIT_GRADIENT_COLORS
                : (iconArtworkActive ? getClockArtworkTransitionColours()
                        : CLOCK_DIGIT_GRADIENT_COLORS);
        int solidDigitColour = neonCircuit && iconArtworkActive
                ? digitColours[0] : CLOCK_DIGIT_SILVER;

        // Icon-artwork matching uses the selected artwork's palette for every
        // clock style, including Neon Circuit. Colour Gradient keeps its own palette.
        boolean useDigitGradient = digitGradient || iconArtworkActive;
        float digitPhase = digitGradient ? displayPhase : 0f;

        if (clockText instanceof SevenSegmentClockTextView) {
            SevenSegmentClockTextView clockRenderer = (SevenSegmentClockTextView) clockText;
            clockRenderer.setSegmentGradientEnabled(
                    useDigitGradient && sevenSegment, digitPhase, digitColours);

            boolean useNothingInnerBorder = nothingArtworkMatch && !neonCircuit;
            boolean useGradientInnerBorder = useClockWhiteInnerBorder;
            // Standard typography clocks draw borders directly from their per-glyph
            // spans, on Android 6 as well as newer releases. A TextView-wide DST_IN pass
            // can erase or double-render glyphs after a two-digit text rebuild.
            boolean useRendererTextBorder = sevenSegment;
            // Centered strokes are masked to the glyph, so 2dp yields a 1dp inner rim
            // for typography text; the custom LED segments keep their established 1dp.
            float innerBorderWidth = useClockWhiteInnerBorder
                    ? (sevenSegment ? 1f : 2f) * borderDensity
                    : (useNothingInnerBorder ? 3f * borderDensity
                            : (sevenSegment ? 1f : 2f) * borderDensity);
            clockRenderer.setDigitOnlyInnerBorder(false);
            boolean useAnyInnerBorder = useNothingInnerBorder || useGradientInnerBorder;
            boolean drawRendererBorder = useRendererTextBorder && useAnyInnerBorder;
            clockRenderer.setContrastInnerBorderEnabled(
                    drawRendererBorder, digitColours, innerBorderWidth,
                    drawRendererBorder ? CLOCK_WHITE_BORDER_COLORS : null);
        }

        if (sevenSegment) {
            // SevenSegmentClockTextView paints its segments itself.
            clockDigitGradients.remove(clockText);
            if (clockText.getPaint().getShader() != null) {
                clockText.getPaint().setShader(null);
            }
            if (clockText.getCurrentTextColor() != CLOCK_DIGIT_SILVER) {
                clockText.setTextColor(CLOCK_DIGIT_SILVER);
            }
            clockText.invalidate();
        } else if (digitGradient) {
            applyAnimatedTextGradient(clockText, digitColours,
                    displayPhase, clockDigitGradients);
        } else if (iconArtworkActive) {
            // Phase zero pins the selected artwork theme's colours in place.
            if (clockText.getCurrentTextColor() != solidDigitColour) {
                clockText.setTextColor(solidDigitColour);
            }
            applyAnimatedTextGradient(clockText, digitColours,
                    0f, clockDigitGradients);
        } else {
            clockDigitGradients.remove(clockText);
            if (clockText.getPaint().getShader() != null) {
                clockText.getPaint().setShader(null);
                clockText.invalidate();
            }
            if (clockText.getCurrentTextColor() != solidDigitColour) {
                clockText.setTextColor(solidDigitColour);
            }
        }
    }

    private void applyAnimatedTextGradient(TextView view, int[] colors, float phase,
            java.util.Map<TextView, ClockTextGradientState> cache) {
        if (view == null || colors == null || colors.length < 2) return;
        // All clock styles share one fixed gradient coordinate system. The live time,
        // font metrics and view width must not change where colour stops appear.
        int gradientSpanPx = SevenSegmentClockTextView.stableClockGradientSpanPx(view.getContext());
        ClockTextGradientState state = cache.get(view);
        if (state == null || state.gradientSpanPx != gradientSpanPx
                || !java.util.Arrays.equals(state.colors, colors)) {
            state = new ClockTextGradientState();
            state.gradientSpanPx = gradientSpanPx;
            state.colors = colors.clone();
            state.shader = new android.graphics.LinearGradient(0f, 0f,
                    gradientSpanPx, 0f, colors, null,
                    android.graphics.Shader.TileMode.MIRROR);
            cache.put(view, state);
        }
        float normalized = phase - (float) Math.floor(phase);
        state.matrix.setTranslate(-normalized * state.gradientSpanPx * 2f, 0f);
        state.shader.setLocalMatrix(state.matrix);
        if (view.getPaint().getShader() != state.shader) {
            view.getPaint().setShader(state.shader);
        }
        view.invalidate();
    }

    private void updateClock() {
        if (clockPanelsByPage[0] == null || clockPanelsByPage[1] == null) return;
        final boolean forceTextRefresh = forceClockTextRefresh;
        forceClockTextRefresh = false;
        Date now = new Date();
        int currentColourPhase = (now.getMinutes() / 5) % 4;
        if (currentColourPhase != lastClockPanelColorPhase) applyClockPanelColor(false);
        boolean use24Hour = DateFormat.is24HourFormat(this);
        String dateValue = new SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(now);
        int restorePage = Math.max(0, Math.min(1, currentPage));
        for (int page = 0; page < clockPanelsByPage.length; page++) {
            activateClockForPage(page);
            int configuredDateSize = Math.max(8, Math.min(16,
                    prefs.getInt(clockPrefKey(KEY_CLOCK_DATE_SIZE, activeClockPage), 15)));
            int configuredPmSize = Math.max(8, Math.min(20,
                    prefs.getInt(clockPrefKey(KEY_CLOCK_PM_SIZE, activeClockPage), 14)));
            boolean iconArtworkActive = isClockIconArtworkActive();
            android.text.SpannableStringBuilder dateLine =
                    new android.text.SpannableStringBuilder(dateValue);
            int periodLength = 0;
            if (!use24Hour) {
                String period = new SimpleDateFormat("a", Locale.getDefault()).format(now);
                periodLength = period.length();
                // Separate AM/PM from the date with a comma and size it independently per page.
                dateLine.insert(0, period + ", ");
                dateLine.setSpan(new android.text.style.StyleSpan(Typeface.BOLD_ITALIC), 0,
                        periodLength, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                dateLine.setSpan(new android.text.style.TypefaceSpan(clockPmFontFamily()),
                        0, periodLength, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                if (!prefs.getBoolean(KEY_CLOCK_DIGIT_GRADIENT, false)
                        && !iconArtworkActive) {
                    dateLine.setSpan(new android.text.style.ForegroundColorSpan(CLOCK_DIGIT_SILVER),
                            0, periodLength, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                // Date sizes are in sp; AM/PM sizes are in dp. Convert both to pixel
                // size before deriving the relative span so AM/PM remains a true dp size.
                android.util.DisplayMetrics textMetrics = getResources().getDisplayMetrics();
                float periodScale = (configuredPmSize * textMetrics.density)
                        / Math.max(0.1f, configuredDateSize * textMetrics.scaledDensity);
                dateLine.setSpan(new android.text.style.RelativeSizeSpan(periodScale),
                        0, periodLength, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            // Keep AM/PM's existing styling. The date itself uses a bright silver-white
            // fill and a black outer contour in every clock preset and artwork mode.
            int dateStart = use24Hour ? 0 : periodLength + 2;
            float density = getResources().getDisplayMetrics().density;
            float whiteOuterWidth = 0.4f * density;
            float blackInnerWidth = android.util.TypedValue.applyDimension(
                    android.util.TypedValue.COMPLEX_UNIT_SP, 0.5f,
                    getResources().getDisplayMetrics());
            float dateBorderWidthPx = density * (configuredDateSize / 20f);
            int selectedArtworkTheme = LauncherIconDrawable.clampArtworkTheme(prefs.getInt(
                    KEY_ICON_ARTWORK_THEME, LauncherIconDrawable.ARTWORK_ORIGINAL));
            boolean forceWhiteDateFill = iconArtworkActive
                    && selectedArtworkTheme == LauncherIconDrawable.ARTWORK_NOTHING_MONOCHROME;
            int dateFillColor = forceWhiteDateFill ? Color.WHITE : clockDateFillColor();
            if (dateStart < dateLine.length()) {
                dateLine.setSpan(new ClockTextOutlineSpan(
                                dateFillColor, Color.BLACK,
                                dateBorderWidthPx, 0f),
                        dateStart, dateLine.length(),
                        android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            if (!use24Hour && periodLength > 0) {
                dateLine.setSpan(new ClockTextOutlineSpan(
                                Color.WHITE, whiteOuterWidth, blackInnerWidth),
                        0, periodLength, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
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
            int fallbackColonAt = clockValue.indexOf(':');
            int fallbackHourDigits = 0;
            for (int i = 0; fallbackColonAt >= 0 && i < fallbackColonAt; i++) {
                if (Character.digit(clockValue.charAt(i), 10) >= 0) fallbackHourDigits++;
            }
            // On Android 6, a ReplacementSpan at offset zero can be lost in the
            // first TextView layout after reinstall/package replacement. The working
            // one-digit path already starts with a reserved blank digit cell; give a
            // two-digit hour a zero-width leading sentinel so every style can retain
            // the same per-glyph renderer without shifting the visible time.
            boolean api23TwoDigitHourNeedsSentinel =
                    Build.VERSION.SDK_INT == Build.VERSION_CODES.M
                    && !sevenSegmentClock && fallbackHourDigits >= 2;
            int styleForClockSpacing = prefs == null ? 0 : prefs.getInt(KEY_CLOCK_STYLE, 0);
            float baseClockLetterSpacing = clockPresetLetterSpacing(styleForClockSpacing);
            if (Math.abs(clockText.getLetterSpacing() - baseClockLetterSpacing) > 0.001f) {
                clockText.setLetterSpacing(baseClockLetterSpacing);
            }
            if (clockText instanceof SevenSegmentClockTextView) {
                ((SevenSegmentClockTextView) clockText).setSevenSegmentMode(sevenSegmentClock);
            }
            if (!sevenSegmentClock) {
                if (clockText.getVisibility() != View.VISIBLE) clockText.setVisibility(View.VISIBLE);
                if (clockText.getAlpha() != 1f) clockText.setAlpha(1f);
            }
            // Two-digit standard clocks always keep a thin white inner contour. On API 23
            // the renderer draws this over native TextView glyphs; newer versions draw it
            // inside each digit/colon span. Seven-segment rendering remains unchanged.
            boolean useWhiteDigitInnerBorder = prefs.getBoolean(KEY_CLOCK_DIGIT_GRADIENT, false)
                    || isClockIconArtworkActive()
                    || (!sevenSegmentClock && fallbackHourDigits >= 2);
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
                if (api23TwoDigitHourNeedsSentinel) {
                    // U+200B has zero advance. It prevents the first ReplacementSpan
                    // starting at index 0 on API 23 without changing visible centering.
                    displayClock.insert(0, "\u200B");
                }
                // Center the colon independently of the date typography. Every numeric
                // glyph uses a span that can paint the same 1dp inner-white contour without
                // changing the existing gradient/artwork fill or the tuned digit spacing.
                int colonAt = displayClock.toString().indexOf(':');
                if (colonAt >= 0) {
                    int minuteFirst = colonAt + 1;
                    boolean hasMinutePair = minuteFirst + 1 < displayClock.length()
                            && Character.digit(displayClock.charAt(minuteFirst), 10) >= 0
                            && Character.digit(displayClock.charAt(minuteFirst + 1), 10) >= 0;
                    float tightenedDigitGap = clockText.getTextSize()
                            * Math.max(0f, clockText.getLetterSpacing()) * 0.5f;
                    int hourEnd = colonAt - 1;
                    int hourStart = hourEnd;
                    while (hourStart >= 0
                            && Character.digit(displayClock.charAt(hourStart), 10) >= 0) {
                        hourStart--;
                    }
                    hourStart++;
                    int hourDigitCount = hourEnd - hourStart + 1;
                    // Keep the native span advances for the hour/minute groups.
                    // The previous 15dp inward shift compressed their clearance too far
                    // on newer Android releases. The colon keeps its compact own advance.
                    float innerWhiteBorderPx = LauncherUiUtils.dp(this, 1f);
                    int spacingStyle = prefs == null ? 0 : prefs.getInt(KEY_CLOCK_STYLE, 0);
                    boolean widenTwoDigitGap = hourDigitCount >= 2
                            && !sevenSegmentClock && spacingStyle != 2
                            && Build.VERSION.SDK_INT >= 36;
                    float renderScaleX = Math.max(0.25f,
                            Math.abs(clockText.getScaleX())
                                    * Math.abs(clockPanel == null ? 1f : clockPanel.getScaleX()));
                    float halfGroupSpreadPx = widenTwoDigitGap
                            ? LauncherUiUtils.dp(this, 7.5f) / renderScaleX : 0f;
                    for (int digitAt = 0; digitAt < displayClock.length(); digitAt++) {
                        if (Character.digit(displayClock.charAt(digitAt), 10) < 0) continue;
                        boolean moveFirstHourDigitRight = digitAt == hourStart && hourDigitCount >= 2;
                        float gap = digitAt == minuteFirst && hasMinutePair
                                ? tightenedDigitGap : 0f;
                        float glyphOffsetX = 0f;
                        if (widenTwoDigitGap) {
                            if (digitAt >= hourStart && digitAt <= hourEnd) {
                                glyphOffsetX = -halfGroupSpreadPx;
                            } else if (digitAt >= minuteFirst && hasMinutePair
                                    && digitAt <= minuteFirst + 1) {
                                glyphOffsetX = halfGroupSpreadPx;
                            }
                        }
                        displayClock.setSpan(new TightDigitSpan(
                                        moveFirstHourDigitRight, gap, glyphOffsetX,
                                        useWhiteDigitInnerBorder, innerWhiteBorderPx),
                                digitAt, digitAt + 1,
                                android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                    displayClock.setSpan(new CenteredColonSpan(
                                    getClockColonCenterY(),
                                    useWhiteDigitInnerBorder, innerWhiteBorderPx),
                            colonAt, colonAt + 1, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
            Boolean previousDigitBorderMode = clockDigitInnerBorderModeApplied.get(clockText);
            boolean digitBorderModeChanged = previousDigitBorderMode == null
                    || previousDigitBorderMode.booleanValue() != useWhiteDigitInnerBorder;
            boolean missingDisplaySpans = false;
            int expectedColon = displayClock.toString().indexOf(':');
            if (!sevenSegmentClock && !clockValue.isEmpty() && expectedColon >= 0) {
                CharSequence existingText = clockText.getText();
                if (!(existingText instanceof android.text.Spanned)) {
                    missingDisplaySpans = true;
                } else {
                    android.text.Spanned existingSpanned = (android.text.Spanned) existingText;
                    int expectedDigitSpans = 0;
                    for (int i = 0; i < displayClock.length(); i++) {
                        if (Character.digit(displayClock.charAt(i), 10) >= 0) expectedDigitSpans++;
                    }
                    TightDigitSpan[] installedDigitSpans = existingSpanned.getSpans(
                            0, existingSpanned.length(), TightDigitSpan.class);
                    missingDisplaySpans = installedDigitSpans.length < expectedDigitSpans;
                    if (existingSpanned.getSpans(expectedColon, expectedColon + 1,
                            CenteredColonSpan.class).length == 0) {
                        missingDisplaySpans = true;
                    }
                }
            }
            if (forceTextRefresh || missingDisplaySpans
                    || !displayClock.toString().contentEquals(clockText.getText())
                    || digitBorderModeChanged) {
                // The seven-segment renderer owns its glyph geometry; standard styles
                // keep the normal TextView formatting and use a separate static shader.
                if (sevenSegmentClock) {
                    clockText.setText(displayClock, TextView.BufferType.NORMAL);
                    clockText.postInvalidateOnAnimation();
                } else {
                    // All non-seven-segment styles, including Android 6 two-digit hours,
                    // use the same mutable per-digit and centered-colon spans.
                    clockText.setText(displayClock, TextView.BufferType.SPANNABLE);
                    clockText.requestLayout();
                    clockText.invalidate();
                }
                clockDigitInnerBorderModeApplied.put(clockText, useWhiteDigitInnerBorder);
            }
            // Keep the separator aligned with the active clock's vertical transform
            // after every text refresh, independently of the date/AM-PM typography.
            syncClockColonVerticalCenter();
            // TextView may not have rebuilt its Layout yet during initial creation.
            // Re-sync each page after its own layout completes. The inactive page's
            // clock must not be skipped because the mutable active clock points elsewhere.
            final TextView pendingColonSyncTarget = clockText;
            final int pendingColonSyncPage = page;
            pendingColonSyncTarget.postOnAnimation(() -> {
                if (clockTextsByPage[pendingColonSyncPage] != pendingColonSyncTarget) return;
                int restoreColonPage = Math.max(0, Math.min(1, activeClockPage));
                activateClockForPage(pendingColonSyncPage);
                syncClockColonVerticalCenter();
                
                updateClockDigitHorizontalOffset();
                activateClockForPage(restoreColonPage);
            });
            updateClockDigitHorizontalOffset();
            int dateVisibility = prefs.getBoolean(KEY_SHOW_DATE, true) ? View.VISIBLE : View.GONE;
            if (dateText.getVisibility() != dateVisibility) dateText.setVisibility(dateVisibility);
            boolean useDateGradient = prefs.getBoolean(KEY_CLOCK_DIGIT_GRADIENT, false)
                    || isClockIconArtworkActive();
            Boolean previousDateGradient = dateGradientModeApplied.get(dateText);
            boolean dateModeChanged = previousDateGradient == null
                    || previousDateGradient.booleanValue() != useDateGradient;
            String dateAppearanceKey = configuredDateSize + ":" + configuredPmSize + ":"
                    + useDateGradient + ":" + isClockIconArtworkActive() + ":"
                    + prefs.getInt(KEY_CLOCK_DATE_FILL_COLOR_POSITION, -1) + ":"
                    + clockDateFontFamily() + ":" + clockPmFontFamily();
            String previousDateAppearance = dateTextAppearanceApplied.get(dateText);
            if (!dateLine.toString().contentEquals(dateText.getText())
                    || dateModeChanged || !dateAppearanceKey.equals(previousDateAppearance)) {
                dateText.setText(dateLine);
                dateGradientModeApplied.put(dateText, useDateGradient);
                dateTextAppearanceApplied.put(dateText, dateAppearanceKey);
            }
            // Layout, typography and panel appearance are configuration-driven and
            // are applied when the clock is created or settings change, not every minute.
        }
        activateClockForPage(restorePage);
        // A time/date refresh must only replace text. Re-running geometry here
        // recalculates the panel from the current digits/date, so the background
        // can shrink and the clock shifts horizontally at a minute/day boundary.
        // Geometry is recalculated on creation, style changes, and explicit resize.
        applyClockColorEffects(clockColorPhase, true);
        enforceApi23TwoDigitWhiteInnerBorder();
        updateClockResizeHandles();
        applyBrandVisibility();
    }

    /**
     * Android 6 uses native TextView glyphs for two-digit standard hours to avoid a
     * ReplacementSpan rendering failure. Reassert the 1dp effective white inner contour
     * after the general colour pass for every two-digit non-7-segment clock.
     */
    private void enforceApi23TwoDigitWhiteInnerBorder() {
        if (Build.VERSION.SDK_INT != Build.VERSION_CODES.M) return;
        int restorePage = Math.max(0, Math.min(1, activeClockPage));
        int style = prefs == null ? 0 : prefs.getInt(KEY_CLOCK_STYLE, 0);
        for (int page = 0; page < clockTextsByPage.length; page++) {
            TextView target = clockTextsByPage[page];
            if (!(target instanceof SevenSegmentClockTextView)) continue;
            activateClockForPage(page);
            if (style != 5) {
                // All typography styles use TightDigitSpan and CenteredColonSpan.
                // Their white inner border is painted per glyph; never replace it with
                // the TextView-wide outline, which can erase the first digit on API 23.
                ((SevenSegmentClockTextView) target).setContrastInnerBorderEnabled(
                        false, null, 0f, null);
            }
            target.invalidate();
        }
        activateClockForPage(restorePage);
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
            int side = LauncherUiUtils.dp(this, 40);
            int thin = LauncherUiUtils.dp(this, 18);
            FrameLayout.LayoutParams gripLp = new FrameLayout.LayoutParams(
                    i < 2 ? thin : side, i < 2 ? side : thin,
                    Gravity.LEFT | Gravity.TOP);
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
                    boolean firstMoveFrame = !clockWidgetDragging;
                    if (firstMoveFrame) {
                        int[] safeClockGeometry = itemGeometry(clockPanel);
                        liveDragLastSafeGeometry.put(clockPanel,
                                new int[]{safeClockGeometry[0], safeClockGeometry[1]});
                    }
                    clockWidgetDragging = true;
                    float targetX = clockWidgetStartX + dx;
                    float targetY = clockWidgetStartY + dy;
                    setClampedClockTranslation(targetX, targetY, false);
                    scheduleClockResizeHandlesUpdate();
                    if (firstMoveFrame) setWorkspaceGridVisible(true, clockPanel);
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
                    boolean releaseWasUp = event.getActionMasked() == MotionEvent.ACTION_UP;
                    boolean accepted = finishLiveDisplacements(releaseWasUp, clockPanel);
                    if (releaseWasUp && !accepted) {
                        clockPanel.setTranslationX(clockWidgetStartX);
                        clockPanel.setTranslationY(clockWidgetStartY);
                        prefs.edit()
                                .putFloat(clockPrefKey(KEY_CLOCK_WIDGET_X, activeClockPage), clockWidgetStartX)
                                .putFloat(clockPrefKey(KEY_CLOCK_WIDGET_Y, activeClockPage), clockWidgetStartY)
                                .apply();
                    }
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
        int phase = (new Date().getMinutes() / 5) % 4;
        boolean foundPagePanel = false;
        for (int page = 0; page < clockPanelsByPage.length; page++) {
            LinearLayout panel = clockPanelsByPage[page];
            if (panel == null) continue;
            applyClockPanelColorToPanel(panel, phase, animate);
            foundPagePanel = true;
        }
        // During initial home-chrome construction the first panel exists before it is
        // registered in clockPanelsByPage. Preserve that first draw as well.
        if (!foundPagePanel && clockPanel != null) {
            applyClockPanelColorToPanel(clockPanel, phase, animate);
        }
        lastClockPanelColorPhase = phase;
    }

    private void applyClockPanelColorToPanel(View hero, int phase, boolean animate) {
        if (hero == null) return;
        int[][] tones = new int[][] {
                {115, 159, 255}, {119, 137, 255}, {179, 124, 255}, {90, 190, 196}
        };
        int tonePhase = Math.max(0, Math.min(tones.length - 1, phase));
        int[] tone = tones[tonePhase];
        int opacity = Math.max(0, Math.min(100,
                prefs == null ? 50 : prefs.getInt(KEY_CLOCK_BG_OPACITY, 50)));
        // Preserve the existing background-opacity control: 50% retains the original
        // approximate alpha, while 100% is fully opaque.
        int opacityScale = opacity * 2;

        boolean nothingMonochrome = isClockIconArtworkActive()
                && prefs != null
                && LauncherIconDrawable.clampArtworkTheme(prefs.getInt(
                        KEY_ICON_ARTWORK_THEME, LauncherIconDrawable.ARTWORK_ORIGINAL))
                        == LauncherIconDrawable.ARTWORK_NOTHING_MONOCHROME;

        final int[] backgroundColours;
        if (nothingMonochrome) {
            // Pure black at both edges, fading symmetrically toward 10% of that
            // opacity at the centre. Keep the slider meaningful across the full range.
            int edgeAlpha = Math.min(255, 255 * opacity / 100);
            int midAlpha = edgeAlpha * 55 / 100;
            int centreAlpha = edgeAlpha * 10 / 100;
            int edge = Color.argb(edgeAlpha, 0, 0, 0);
            int mid = Color.argb(midAlpha, 0, 0, 0);
            int centre = Color.argb(centreAlpha, 0, 0, 0);
            backgroundColours = new int[] {edge, mid, centre, mid, edge};
        } else {
            // Existing cool teal/blue and phase-driven violet/blue treatment for other themes.
            int leftR = Math.max(55, Math.min(220, tone[0] * 2 / 3));
            int leftG = Math.max(150, Math.min(225, tone[1] + 28));
            int leftB = Math.max(170, Math.min(240, tone[2] + 18));
            int leftEdge = Color.argb(Math.min(255, 125 * opacityScale / 100),
                    leftR, leftG, leftB);
            int rightEdge = Color.argb(Math.min(255, 149 * opacityScale / 100),
                    tone[0], tone[1], tone[2]);
            int leftMid = Color.argb(Math.min(255, 72 * opacityScale / 100),
                    leftR, leftG, leftB);
            int rightMid = Color.argb(Math.min(255, 76 * opacityScale / 100),
                    tone[0], tone[1], tone[2]);
            int centre = Color.argb(Math.min(255, 22 * opacityScale / 100),
                    125, 155, 235);
            backgroundColours = new int[] {
                    leftEdge, leftMid, centre, rightMid, rightEdge
            };
        }

        GradientDrawable next = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT, backgroundColours);
        next.setCornerRadius(LauncherUiUtils.dp(this, 18));
        // Keep the background at full container height for both clock styles.
        ClockPanelBackgroundDrawable centeredBackground =
                new ClockPanelBackgroundDrawable(next, 0f);
        if (animate && !nothingMonochrome) {
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
    /**
     * Slot-based push for contiguous app rows/columns. When icon artwork leaves gaps inside grid
     * cells, the visual sweep can miss neighbours; this moves an occupied lane as one transaction.
     */
    /**
     * First-choice row/column displacement. Move the entire lane by one grid cell,
     * including icons separated by empty slots, so their relative positions do not change
     * just because a different member happened to be the first collision. When the full
     * lane cannot move safely, the caller falls back to the minimum contiguous chain.
     */
    private boolean tryShiftWholeHomeIconLaneDirection(FrameLayout page, View movingView,
            View seed, int dxCells, int dyCells, int columns, int cellWidth,
            int rows, int cellHeight) {
        if (page == null || movingView == null || seed == null
                || ((dxCells == 0) == (dyCells == 0))) return false;
        int[] seedSlot = homeGridIconSlot(page, seed, cellHeight, rows);
        if (seedSlot == null) return false;
        final boolean horizontal = dxCells != 0;
        final boolean shortcutLane = isWorkspaceContactShortcut(seed);

        java.util.ArrayList<View> lane = new java.util.ArrayList<View>();
        java.util.IdentityHashMap<View, Boolean> laneSet =
                new java.util.IdentityHashMap<View, Boolean>();
        java.util.IdentityHashMap<View, int[]> laneSlots =
                new java.util.IdentityHashMap<View, int[]>();
        for (int i = 0; i < page.getChildCount(); i++) {
            View candidate = page.getChildAt(i);
            if (candidate == movingView || candidate.getVisibility() != View.VISIBLE
                    || !isHomeGridIcon(candidate)
                    || isWorkspaceContactShortcut(candidate) != shortcutLane) continue;
            int[] slot = homeGridIconSlot(page, candidate, cellHeight, rows);
            if (slot == null) continue;
            boolean sameLane = horizontal ? slot[1] == seedSlot[1] : slot[0] == seedSlot[0];
            if (!sameLane) continue;
            lane.add(candidate);
            laneSet.put(candidate, Boolean.TRUE);
            laneSlots.put(candidate, slot);
        }
        if (lane.size() < 2 || !laneSet.containsKey(seed)) return false;

        int[] moverGeometry = itemGeometry(movingView);
        java.util.IdentityHashMap<View, int[]> targets =
                new java.util.IdentityHashMap<View, int[]>();
        java.util.IdentityHashMap<View, int[]> geometries =
                new java.util.IdentityHashMap<View, int[]>();
        for (View member : lane) {
            int[] originSlot = laneSlots.get(member);
            int targetCol = originSlot[0] + dxCells;
            int targetRow = originSlot[1] + dyCells;
            int[] geometry = itemGeometry(member);
            if (targetCol < 0 || targetCol >= columns || targetRow < 0 || targetRow >= rows
                    || geometry[2] <= 0 || geometry[3] <= 0) return false;
            int originX = homeGridIconLeftAt(member, originSlot[0], cellWidth, geometry[2]);
            int originY = homeGridIconTopAt(page, member, originSlot[1], rows, cellHeight);
            int targetX = homeGridIconLeftAt(member, targetCol, cellWidth, geometry[2]);
            int targetY = homeGridIconTopAt(page, member, targetRow, rows, cellHeight);
            if (targetX < 0 || targetY < 0
                    || targetX + geometry[2] > page.getWidth()
                    || targetY + geometry[3] > page.getHeight()) return false;

            android.graphics.Rect startVisible = appIconVisualRectAt(
                    member, originX, originY, geometry[2], geometry[3]);
            android.graphics.Rect targetVisible = appIconVisualRectAt(
                    member, targetX, targetY, geometry[2], geometry[3]);
            android.graphics.Rect movingVisible = collisionRectForInteraction(
                    movingView, moverGeometry, member);
            if (android.graphics.Rect.intersects(targetVisible, movingVisible)) return false;
            android.graphics.Rect swept = new android.graphics.Rect(startVisible);
            swept.union(targetVisible);

            for (int childIndex = 0; childIndex < page.getChildCount(); childIndex++) {
                View blocker = page.getChildAt(childIndex);
                if (blocker == member || blocker == movingView || laneSet.containsKey(blocker)
                        || blocker.getVisibility() != View.VISIBLE
                        || !isWorkspaceItem(blocker)) continue;
                int[] blockerGeometry = itemGeometry(blocker);
                if (blockerGeometry[2] <= 0 || blockerGeometry[3] <= 0) continue;
                android.graphics.Rect blockerBounds = collisionRectForInteraction(
                        blocker, blockerGeometry, member);
                if (android.graphics.Rect.intersects(swept, blockerBounds)) return false;
                int[] blockerTarget = liveDisplacementTargets.get(blocker);
                if (blockerTarget != null) {
                    android.graphics.Rect reservedBounds = collisionRectAtForInteraction(
                            blocker, blockerTarget[0], blockerTarget[1],
                            blockerGeometry[2], blockerGeometry[3], member);
                    if (android.graphics.Rect.intersects(swept, reservedBounds)) return false;
                }
            }
            targets.put(member, new int[]{targetX, targetY});
            geometries.put(member, geometry);
        }

        // Reserve all targets first so no member is planned against another member's old
        // frame or a destination that its animation has not reached yet.
        for (View member : lane) {
            int[] geometry = geometries.get(member);
            int[] target = targets.get(member);
            if (!liveDisplacedItems.containsKey(member)) {
                liveDisplacedItems.put(member, new int[]{geometry[0], geometry[1]});
            }
            liveDisplacementTargets.put(member, new int[]{target[0], target[1]});
            liveDragGeometryCache.remove(member);
        }
        for (View member : lane) {
            int[] target = targets.get(member);
            animateDisplacedItemTo(member, target[0], target[1]);
        }
        return true;
    }

    private boolean tryShiftHomeIconLaneCohort(FrameLayout page, View movingView, View seed,
            int preferredX, int preferredY, int pushDx, int pushDy) {
        if (page == null || movingView == null || seed == null
                || !isHomeGridDragMover(movingView) || !isHomeGridIcon(seed)
                || page.getWidth() <= 0 || page.getHeight() <= 0) return false;

        int columns = homeGridColumnsForItem(seed);
        int cellWidth = Math.max(1, page.getWidth() / Math.max(1, columns));
        int iconSize = prefs == null ? 48 : prefs.getInt(KEY_ICON_SIZE, 48);
        int cellHeight = LauncherUiUtils.dp(this, homeCellHeightDp(iconSize));
        int rows = homeGridRowsForHeight(page.getHeight(), cellHeight);
        int[] seedSlot = homeGridIconSlot(page, seed, cellHeight, rows);
        if (seedSlot == null) return false;

        java.util.ArrayList<int[]> directions = new java.util.ArrayList<int[]>();
        java.util.HashSet<String> seen = new java.util.HashSet<String>();
        if (Math.abs(pushDx) >= Math.abs(pushDy)) {
            addAppShiftDirection(directions, seen, Integer.signum(pushDx), 0);
            addAppShiftDirection(directions, seen, 0, Integer.signum(pushDy));
        } else {
            addAppShiftDirection(directions, seen, 0, Integer.signum(pushDy));
            addAppShiftDirection(directions, seen, Integer.signum(pushDx), 0);
        }
        if (preferredX >= 0 && preferredY >= 0) {
            int preferredCol = Math.max(0, Math.min(columns - 1,
                    Math.round(preferredX / (float) cellWidth)));
            int preferredRow = homeGridRowFromTop(
                    preferredY, rows, page.getHeight(), cellHeight);
            int dx = preferredCol - seedSlot[0];
            int dy = preferredRow - seedSlot[1];
            if (Math.abs(dx) >= Math.abs(dy)) {
                addAppShiftDirection(directions, seen, Integer.signum(dx), 0);
                addAppShiftDirection(directions, seen, 0, Integer.signum(dy));
            } else {
                addAppShiftDirection(directions, seen, 0, Integer.signum(dy));
                addAppShiftDirection(directions, seen, Integer.signum(dx), 0);
            }
        }
        int[] moverGeometry = itemGeometry(movingView);
        int[] seedGeometry = itemGeometry(seed);
        int moverCenterX = moverGeometry[0] + moverGeometry[2] / 2;
        int moverCenterY = moverGeometry[1] + moverGeometry[3] / 2;
        int seedCenterX = seedGeometry[0] + seedGeometry[2] / 2;
        int seedCenterY = seedGeometry[1] + seedGeometry[3] / 2;
        if (Math.abs(seedCenterX - moverCenterX) >= Math.abs(seedCenterY - moverCenterY)) {
            addAppShiftDirection(directions, seen, seedCenterX >= moverCenterX ? 1 : -1, 0);
            addAppShiftDirection(directions, seen, 0, seedCenterY >= moverCenterY ? 1 : -1);
        } else {
            addAppShiftDirection(directions, seen, 0, seedCenterY >= moverCenterY ? 1 : -1);
            addAppShiftDirection(directions, seen, seedCenterX >= moverCenterX ? 1 : -1, 0);
        }
        addAppShiftDirection(directions, seen, 1, 0);
        addAppShiftDirection(directions, seen, -1, 0);
        addAppShiftDirection(directions, seen, 0, 1);
        addAppShiftDirection(directions, seen, 0, -1);

        for (int[] direction : directions) {
            int dxCells = direction[0];
            int dyCells = direction[1];
            if (dxCells == 0 && dyCells == 0) continue;

            if (tryShiftWholeHomeIconLaneDirection(page, movingView, seed,
                    dxCells, dyCells, columns, cellWidth, rows, cellHeight)) return true;

            java.util.ArrayList<View> cohort = new java.util.ArrayList<View>();
            java.util.IdentityHashMap<View, Boolean> cohortSet =
                    new java.util.IdentityHashMap<View, Boolean>();
            java.util.IdentityHashMap<View, int[]> slots =
                    new java.util.IdentityHashMap<View, int[]>();
            cohort.add(seed);
            cohortSet.put(seed, Boolean.TRUE);
            slots.put(seed, seedSlot);
            int cursorCol = seedSlot[0];
            int cursorRow = seedSlot[1];
            boolean foundFreeSlot = false;
            boolean blocked = false;

            for (int steps = 0; steps < Math.max(columns, rows); steps++) {
                int nextCol = cursorCol + dxCells;
                int nextRow = cursorRow + dyCells;
                if (nextCol < 0 || nextCol >= columns || nextRow < 0 || nextRow >= rows) {
                    blocked = true;
                    break;
                }
                View occupant = null;
                for (int i = 0; i < page.getChildCount(); i++) {
                    View candidate = page.getChildAt(i);
                    if (!isHomeGridIcon(candidate) || candidate.getVisibility() != View.VISIBLE) continue;
                    int[] candidateSlot = homeGridIconSlot(page, candidate, cellHeight, rows);
                    if (candidateSlot == null || candidateSlot[0] != nextCol
                            || candidateSlot[1] != nextRow) continue;
                    boolean sameGridFamily = isWorkspaceContactShortcut(candidate)
                            == isWorkspaceContactShortcut(seed);
                    if (!sameGridFamily || cohortSet.containsKey(candidate)) {
                        blocked = true;
                        break;
                    }
                    occupant = candidate;
                    break;
                }
                if (blocked) break;
                if (occupant == null) {
                    foundFreeSlot = true;
                    break;
                }
                int[] occupantSlot = homeGridIconSlot(page, occupant, cellHeight, rows);
                if (occupantSlot == null) {
                    blocked = true;
                    break;
                }
                cohort.add(occupant);
                cohortSet.put(occupant, Boolean.TRUE);
                slots.put(occupant, occupantSlot);
                cursorCol = nextCol;
                cursorRow = nextRow;
            }
            if (blocked || !foundFreeSlot) continue;

            java.util.IdentityHashMap<View, int[]> targets =
                    new java.util.IdentityHashMap<View, int[]>();
            java.util.IdentityHashMap<View, int[]> geometries =
                    new java.util.IdentityHashMap<View, int[]>();
            for (View member : cohort) {
                int[] originSlot = slots.get(member);
                int[] geometry = itemGeometry(member);
                int targetCol = originSlot[0] + dxCells;
                int targetRow = originSlot[1] + dyCells;
                int targetX = homeGridIconLeftAt(member, targetCol, cellWidth, geometry[2]);
                int targetY = homeGridIconTopAt(page, member, targetRow, rows, cellHeight);
                if (targetX < 0 || targetY < 0
                        || targetX + geometry[2] > page.getWidth()
                        || targetY + geometry[3] > page.getHeight()) {
                    blocked = true;
                    break;
                }
                android.graphics.Rect targetVisible = appIconVisualRectAt(
                        member, targetX, targetY, geometry[2], geometry[3]);
                int originX = homeGridIconLeftAt(
                        member, originSlot[0], cellWidth, geometry[2]);
                int originY = homeGridIconTopAt(
                        page, member, originSlot[1], rows, cellHeight);
                android.graphics.Rect sweep = appIconVisualRectAt(
                        member, originX, originY, geometry[2], geometry[3]);
                sweep.union(targetVisible);
                android.graphics.Rect movingVisible = collisionRectForInteraction(
                        movingView, moverGeometry, member);
                if (android.graphics.Rect.intersects(targetVisible, movingVisible)) {
                    blocked = true;
                    break;
                }
                for (int i = 0; i < page.getChildCount(); i++) {
                    View blocker = page.getChildAt(i);
                    if (blocker == member || blocker == movingView
                            || cohortSet.containsKey(blocker)
                            || blocker.getVisibility() != View.VISIBLE
                            || !isWorkspaceItem(blocker)) continue;
                    int[] blockerGeometry = itemGeometry(blocker);
                    if (blockerGeometry[2] <= 0 || blockerGeometry[3] <= 0) continue;
                    android.graphics.Rect blockerVisible =
                            collisionRectForInteraction(blocker, blockerGeometry, member);
                    if (android.graphics.Rect.intersects(targetVisible, blockerVisible)
                            || android.graphics.Rect.intersects(sweep, blockerVisible)) {
                        blocked = true;
                        break;
                    }
                }
                if (blocked) break;
                targets.put(member, new int[]{targetX, targetY});
                geometries.put(member, geometry);
            }
            if (blocked || targets.size() != cohort.size()) continue;

            for (View member : cohort) {
                int[] geometry = geometries.get(member);
                int[] target = targets.get(member);
                if (!liveDisplacedItems.containsKey(member)) {
                    liveDisplacedItems.put(member, new int[]{geometry[0], geometry[1]});
                }
                liveDisplacementTargets.put(member, new int[]{target[0], target[1]});
                liveDragGeometryCache.remove(member);
            }
            for (View member : cohort) {
                int[] target = targets.get(member);
                animateDisplacedItemTo(member, target[0], target[1]);
            }
            return true;
        }
        return false;
    }

    private boolean tryShiftHomeIconCohort(FrameLayout page, View movingView, View seed,
            int preferredX, int preferredY, int pushDx, int pushDy) {
        if (page == null || movingView == null || seed == null
                || !isHomeGridDragMover(movingView) || !isHomeGridIcon(seed)
                || page.getWidth() <= 0 || page.getHeight() <= 0) return false;

        if (tryShiftHomeIconLaneCohort(page, movingView, seed,
                preferredX, preferredY, pushDx, pushDy)) return true;

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
        if (pushDx != 0 || pushDy != 0) {
            int pushX = Integer.signum(pushDx);
            int pushY = Integer.signum(pushDy);
            if (Math.abs(pushDx) >= Math.abs(pushDy)) {
                if (pushX != 0) addAppShiftDirection(directions, seenDirections, pushX, 0);
                if (pushY != 0) addAppShiftDirection(directions, seenDirections, 0, pushY);
            } else {
                if (pushY != 0) addAppShiftDirection(directions, seenDirections, 0, pushY);
                if (pushX != 0) addAppShiftDirection(directions, seenDirections, pushX, 0);
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
            int dxCells = direction[0];
            int dyCells = direction[1];
            if (dxCells == 0 && dyCells == 0) continue;
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

            // Follow the occupied slot chain in one grid direction. This does not depend
            // on animation-frame rectangles, which can be between cells while the row moves.
            for (int memberIndex = 0; memberIndex < cohort.size() && !blocked; memberIndex++) {
                View member = cohort.get(memberIndex);
                int[] originSlot = slots.get(member);
                int nextCol = originSlot[0] + dxCells;
                int nextRow = originSlot[1] + dyCells;
                if (nextCol < 0 || nextCol >= columns || nextRow < 0 || nextRow >= rows) {
                    blocked = true;
                    break;
                }

                View occupant = null;
                for (int childIndex = 0; childIndex < page.getChildCount(); childIndex++) {
                    View candidate = page.getChildAt(childIndex);
                    if (candidate == movingView || candidate == member
                            || candidate.getVisibility() != View.VISIBLE
                            || !isHomeGridIcon(candidate)) continue;
                    int[] candidateSlot = homeGridIconSlot(page, candidate, cellHeight, rows);
                    if (candidateSlot == null || candidateSlot[0] != nextCol
                            || candidateSlot[1] != nextRow) continue;

                    boolean sameGridFamily = isWorkspaceContactShortcut(candidate)
                            == isWorkspaceContactShortcut(seed);
                    if (!sameGridFamily || (occupant != null && occupant != candidate)) {
                        blocked = true;
                        break;
                    }
                    occupant = candidate;
                }
                if (blocked || occupant == null) continue;
                if (members.containsKey(occupant)) {
                    blocked = true;
                    break;
                }
                int[] occupiedSlot = homeGridIconSlot(page, occupant, cellHeight, rows);
                if (occupiedSlot == null) {
                    blocked = true;
                    break;
                }
                members.put(occupant, Boolean.TRUE);
                slots.put(occupant, occupiedSlot);
                cohort.add(occupant);
            }

            // A single icon is handled by the ordinary collision planner. This path is
            // specifically for a contiguous row/column that must stay spatially together.
            if (blocked || cohort.size() < 2) continue;

            java.util.IdentityHashMap<View, int[]> targets =
                    new java.util.IdentityHashMap<View, int[]>();
            java.util.IdentityHashMap<View, int[]> geometries =
                    new java.util.IdentityHashMap<View, int[]>();

            // Validate every target and swept path against widgets, clocks, shortcuts,
            // unrelated grid rows, and destinations already reserved by another animation.
            for (View member : cohort) {
                int[] originSlot = slots.get(member);
                int[] geometry = itemGeometry(member);
                int targetCol = originSlot[0] + dxCells;
                int targetRow = originSlot[1] + dyCells;
                if (targetCol < 0 || targetCol >= columns || targetRow < 0 || targetRow >= rows
                        || geometry[2] <= 0 || geometry[3] <= 0) {
                    blocked = true;
                    break;
                }
                int originX = homeGridIconLeftAt(member, originSlot[0], cellWidth, geometry[2]);
                int originY = homeGridIconTopAt(page, member, originSlot[1], rows, cellHeight);
                int targetX = homeGridIconLeftAt(member, targetCol, cellWidth, geometry[2]);
                int targetY = homeGridIconTopAt(page, member, targetRow, rows, cellHeight);
                if (targetX < 0 || targetY < 0 || targetX + geometry[2] > page.getWidth()
                        || targetY + geometry[3] > page.getHeight()) {
                    blocked = true;
                    break;
                }

                android.graphics.Rect startVisible = appIconVisualRectAt(
                        member, originX, originY, geometry[2], geometry[3]);
                android.graphics.Rect targetVisible = appIconVisualRectAt(
                        member, targetX, targetY, geometry[2], geometry[3]);
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
                    if (blocker == member || blocker == movingView || members.containsKey(blocker)
                            || blocker.getVisibility() != View.VISIBLE
                            || !isWorkspaceItem(blocker)) continue;
                    int[] blockerGeometry = itemGeometry(blocker);
                    if (blockerGeometry[2] <= 0 || blockerGeometry[3] <= 0) continue;

                    android.graphics.Rect collisionSweep = new android.graphics.Rect(swept);
                    if (!isHomeGridIcon(blocker)) {
                        int margin = workspaceCollisionMargin(member);
                        if (margin > 0) collisionSweep.inset(-margin, -margin);
                    }
                    android.graphics.Rect blockerBounds = collisionRectForInteraction(
                            blocker, blockerGeometry, member);
                    if (android.graphics.Rect.intersects(collisionSweep, blockerBounds)) {
                        blocked = true;
                        break;
                    }
                    int[] blockerTarget = liveDisplacementTargets.get(blocker);
                    if (blockerTarget != null) {
                        android.graphics.Rect reservedBounds = collisionRectAtForInteraction(
                                blocker, blockerTarget[0], blockerTarget[1],
                                blockerGeometry[2], blockerGeometry[3], member);
                        if (android.graphics.Rect.intersects(collisionSweep, reservedBounds)) {
                            blocked = true;
                            break;
                        }
                    }
                }
                if (blocked) break;
                targets.put(member, new int[]{targetX, targetY});
                geometries.put(member, geometry);
            }

            if (blocked || targets.size() != cohort.size()) continue;

            // Reserve every member before launching any animation so they all receive
            // the exact same cell vector and keep their row/column spacing unchanged.
            for (View member : cohort) {
                int[] geometry = geometries.get(member);
                int[] target = targets.get(member);
                if (!liveDisplacedItems.containsKey(member)) {
                    liveDisplacedItems.put(member, new int[]{geometry[0], geometry[1]});
                }
                liveDisplacementTargets.put(member, new int[]{target[0], target[1]});
                liveDragGeometryCache.remove(member);
            }
            for (View member : cohort) {
                int[] target = targets.get(member);
                animateDisplacedItemTo(member, target[0], target[1]);
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
        // App cells remain logical grid slots. Resize-only auto-shrink is stored
        // separately so it can reduce neighbouring artwork/labels without corrupting
        // slot coordinates or overriding deliberate widget resize state.
        if (!workspaceScaleRestored.containsKey(view)) {
            workspaceScaleRestored.put(view, Boolean.TRUE);
            float autoX = clampWorkspaceAutoScale(
                    getFloatPreferenceCompat(workspaceAutoShrinkScaleKey(view, true), 1f));
            float autoY = clampWorkspaceAutoScale(
                    getFloatPreferenceCompat(workspaceAutoShrinkScaleKey(view, false), 1f));
            if (tag.startsWith("launcher_app:")) {
                view.setScaleX(autoX);
                view.setScaleY(autoY);
                if (prefs != null) {
                    prefs.edit().remove("workspace_scale_x_" + tag)
                            .remove("workspace_scale_y_" + tag)
                            .remove("workspace_base_scale_x_" + tag)
                            .remove("workspace_base_scale_y_" + tag).apply();
                }
            } else {
                float sx = getFloatPreferenceCompat("workspace_scale_x_" + tag, 1f);
                float sy = getFloatPreferenceCompat("workspace_scale_y_" + tag, 1f);
                view.setScaleX((sx > 0f ? sx : 1f) * autoX);
                view.setScaleY((sy > 0f ? sy : 1f) * autoY);
            }
        }
        // Include the clock in workspace displacement so widgets and shortcuts
        // cannot be dropped over it without the same push-aside behavior.
        return tag.startsWith("launcher_widget:") || tag.startsWith("launcher_app:")
                || tag.startsWith(APP_GROUP_TAG_PREFIX)
                || "launcher_contact_shortcut".equals(tag)
                || "launcher_clock_widget".equals(tag);
    }


    private float clampWorkspaceAutoScale(float value) {
        return Math.max(0.25f, Math.min(1f, value));
    }

    private String workspaceAutoShrinkScaleKey(View view, boolean horizontal) {
        String tag = view == null ? "" : String.valueOf(view.getTag());
        // Every contact shortcut uses the same view tag; suffix its stable ID so only
        // the intended shortcut shrinks.
        if (isWorkspaceContactShortcut(view)) {
            String shortcutId = shortcutIdFromTile(view);
            if (!shortcutId.isEmpty()) tag += ":" + shortcutId;
        }
        return (horizontal ? KEY_RESIZE_AUTO_SCALE_X_PREFIX
                : KEY_RESIZE_AUTO_SCALE_Y_PREFIX) + tag;
    }

    /** Restore automatic shrink scales before applying a new home icon size. */
    private void clearAutoShrunkHomeGridItems() {
        if (prefs == null) return;
        if (pagesStrip != null) {
            for (int p = 0; p < pagesStrip.getChildCount(); p++) {
                View pv = pagesStrip.getChildAt(p);
                if (!(pv instanceof ViewGroup)) continue;
                ViewGroup page = (ViewGroup) pv;
                for (int i = 0; i < page.getChildCount(); i++) {
                    View item = page.getChildAt(i);
                    if (!isWorkspaceItem(item)) continue;
                    float ax = clampWorkspaceAutoScale(
                            getFloatPreferenceCompat(workspaceAutoShrinkScaleKey(item, true), 1f));
                    float ay = clampWorkspaceAutoScale(
                            getFloatPreferenceCompat(workspaceAutoShrinkScaleKey(item, false), 1f));
                    if (Math.abs(ax - 1f) > 0.001f) item.setScaleX(item.getScaleX() / ax);
                    if (Math.abs(ay - 1f) > 0.001f) item.setScaleY(item.getScaleY() / ay);
                }
            }
        }
        SharedPreferences.Editor edit = prefs.edit();
        boolean changed = false;
        for (String key : prefs.getAll().keySet()) {
            if (key.startsWith(KEY_RESIZE_AUTO_SCALE_X_PREFIX)
                    || key.startsWith(KEY_RESIZE_AUTO_SCALE_Y_PREFIX)) {
                edit.remove(key);
                changed = true;
            }
        }
        if (changed) edit.apply();
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
        return isWorkspaceAppIcon(view) || isWorkspaceContactShortcut(view)
                || isAppGroupItem(view);
    }

    private boolean isHomeGridDragMover(View view) {
        return isHomeGridIcon(view)
                || (view != null && "workspace_transfer_proxy".equals(view.getTag())
                    && isHomeGridIcon(workspaceTransferItem));
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
        // Grid item views now occupy the full measured cell. Shortcut artwork's 14dp
        // inset is handled by the icon child, so collision and drag positions use cell origin.
        return homeGridRowTop(row, rows, page.getHeight(), cellHeight);
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
        // Keep clearance around the expanded text/date footprint of typography clocks;
        // the glyph outlines and date can extend beyond the background's rounded edge.
        if (isClockWorkspaceItem(view)
                && (prefs == null || prefs.getInt(KEY_CLOCK_STYLE, 0) != 5)) {
            return LauncherUiUtils.dp(this, 5);
        }
        // Apply only a tiny gap around the visible app artwork/label bounds.
        // collisionRect() has already removed the cell's empty area, so this does
        // not make neighboring grid slots collide merely because their cells touch.
        return isHomeGridIcon(view) ? LauncherUiUtils.dp(this, 2) : 0;
    }

    private android.graphics.Rect collisionRect(View view, int[] geometry) {
        int margin = workspaceCollisionMargin(view);
        android.graphics.Rect bounds;
        if (isClockWorkspaceItem(view)) {
            // The translucent/gradient background is itself a collision surface; do not
            // limit clock collisions to the painted digits or date text.
            bounds = clockBackgroundCollisionBoundsAt(view, geometry[0], geometry[1],
                    geometry[2], geometry[3]);
        } else if (isHomeGridIcon(view)) {
            bounds = appIconVisualRectAt(view, geometry[0], geometry[1], geometry[2], geometry[3]);
        } else {
            bounds = new android.graphics.Rect(geometry[0], geometry[1],
                    geometry[0] + geometry[2], geometry[1] + geometry[3]);
        }
        if (margin != 0) bounds.inset(-margin, -margin);
        return bounds;
    }

    private android.graphics.Rect collisionRectAt(View view, int x, int y, int width, int height) {
        int margin = workspaceCollisionMargin(view);
        android.graphics.Rect bounds;
        if (isClockWorkspaceItem(view)) {
            bounds = clockBackgroundCollisionBoundsAt(view, x, y, width, height);
        } else if (isHomeGridIcon(view)) {
            bounds = appIconVisualRectAt(view, x, y, width, height);
        } else {
            bounds = new android.graphics.Rect(x, y, x + width, y + height);
        }
        if (margin != 0) bounds.inset(-margin, -margin);
        return bounds;
    }

    /**
     * Pair-aware collision bounds use actual app/shortcut/group artwork and label bounds
     * for every interaction pair, not the much larger invisible cell assigned for layout.
     * Widgets and other non-grid items continue to use their full visible container bounds.
     */
    private android.graphics.Rect collisionRectForInteraction(
            View view, int[] geometry, View other) {
        if (isClockWorkspaceItem(view)) {
            return workspaceLayoutCollisionRect(view, geometry[0], geometry[1],
                    geometry[2], geometry[3]);
        }
        // When a clock background touches a grid item, the entire grid layout slot
        // participates. Other app/app and widget/app interactions keep their usual
        // visible-artwork bounds to avoid false collisions from empty padding.
        if (isClockWorkspaceItem(other) && isAppIconCollisionParticipant(view)) {
            return new android.graphics.Rect(geometry[0], geometry[1],
                    geometry[0] + geometry[2], geometry[1] + geometry[3]);
        }
        if (isAppIconCollisionParticipant(view)) {
            return appIconVisualRectAt(view, geometry[0], geometry[1], geometry[2], geometry[3]);
        }
        return workspaceLayoutCollisionRect(view, geometry[0], geometry[1],
                geometry[2], geometry[3]);
    }

    private android.graphics.Rect collisionRectAtForInteraction(
            View view, int x, int y, int width, int height, View other) {
        if (isClockWorkspaceItem(view)) {
            return workspaceLayoutCollisionRect(view, x, y, width, height);
        }
        if (isClockWorkspaceItem(other) && isAppIconCollisionParticipant(view)) {
            return new android.graphics.Rect(x, y, x + Math.max(1, width),
                    y + Math.max(1, height));
        }
        if (isAppIconCollisionParticipant(view)) {
            return appIconVisualRectAt(view, x, y, width, height);
        }
        return workspaceLayoutCollisionRect(view, x, y, width, height);
    }

    /**
     * Map only a TextView's laid-out text lines into the clock panel's local coordinates.
     * The TextView can be much wider/taller than its painted text; using its entire view
     * rectangle created an invisible collision/clamping envelope that blocked clock drags
     * and resize gestures long before the visible digits/date reached another item or edge.
     */
    private android.graphics.RectF clockDescendantBoundsInPanel(View panel, View descendant) {
        if (panel == null || descendant == null
                || descendant.getWidth() <= 0 || descendant.getHeight() <= 0) return null;
        android.graphics.RectF bounds = null;
        if (descendant instanceof TextView) {
            TextView textView = (TextView) descendant;
            CharSequence value = textView.getText();
            android.text.Layout textLayout = textView.getLayout();
            if (value != null && value.length() > 0 && textLayout != null
                    && textLayout.getLineCount() > 0) {
                float originX = textView.getCompoundPaddingLeft() - textView.getScrollX();
                float originY = textView.getExtendedPaddingTop() - textView.getScrollY();
                for (int line = 0; line < textLayout.getLineCount(); line++) {
                    float left = originX + textLayout.getLineLeft(line);
                    float right = originX + textLayout.getLineRight(line);
                    float top = originY + textLayout.getLineTop(line);
                    float bottom = originY + textLayout.getLineBottom(line);
                    if (right < left) {
                        float swap = left;
                        left = right;
                        right = swap;
                    }
                    if (right - left < 1f || bottom <= top) continue;
                    if (bounds == null) bounds = new android.graphics.RectF(
                            left, top, right, bottom);
                    else bounds.union(left, top, right, bottom);
                }
            }
        }
        // During initial layout, the TextView has not built its Layout yet. Keep a
        // temporary fallback for that single frame; subsequent geometry updates use
        // the line bounds and remove the inflated text-view rectangle.
        if (bounds == null) {
            bounds = new android.graphics.RectF(
                    0f, 0f, descendant.getWidth(), descendant.getHeight());
        }
        View current = descendant;
        while (current != panel) {
            android.view.ViewParent rawParent = current.getParent();
            if (!(rawParent instanceof View)) return null;
            View parent = (View) rawParent;
            float sx = current.getScaleX();
            float sy = current.getScaleY();
            float left = current.getLeft() - parent.getScrollX()
                    + current.getTranslationX() + current.getPivotX() * (1f - sx);
            float top = current.getTop() - parent.getScrollY()
                    + current.getTranslationY() + current.getPivotY() * (1f - sy);
            bounds.set(left + bounds.left * sx, top + bounds.top * sy,
                    left + bounds.right * sx, top + bounds.bottom * sy);
            current = parent;
        }
        return bounds;
    }

    /**
     * Bounds of the actual clock panel background at a proposed page position. The
     * background fills this panel, so its whole rectangle is tested against neighbouring
     * layout slots, independently of where the digits/date ink happens to fall.
     */
    private android.graphics.Rect clockBackgroundCollisionBoundsAt(
            View panel, int x, int y, int width, int height) {
        return new android.graphics.Rect(
                x, y, x + Math.max(1, width), y + Math.max(1, height));
    }

    /**
     * Compatibility alias for the clock's complete panel/background footprint.
     * Keep existing edge-resize callers on the same geometry as workspace collision checks.
     */
    private android.graphics.Rect clockVisualBoundsAt(
            View panel, int x, int y, int width, int height) {
        return clockBackgroundCollisionBoundsAt(panel, x, y, width, height);
    }

    /**
     * When preset/font/date/size changes alter the rendered footprint without a drag,
     * move the clock to the nearest legal position after descendants finish layout.
     * Resize-time checks run before child layout settles and can miss the final footprint.
     */
    private boolean moveClockToNearestFreePosition(View panel) {
        if (!(panel != null && panel.getParent() instanceof FrameLayout)
                || !isClockWorkspaceItem(panel)
                || (prefs != null && prefs.getInt(KEY_CLOCK_STYLE, 0) == 5)) return true;
        FrameLayout page = (FrameLayout) panel.getParent();
        if (page.getWidth() <= 0 || page.getHeight() <= 0) return true;
        int[] geometry = itemGeometry(panel);
        int width = Math.max(1, geometry[2]);
        int height = Math.max(1, geometry[3]);
        int startX = geometry[0];
        int startY = geometry[1];
        android.graphics.Rect current = new android.graphics.Rect(
                startX, startY, startX + width, startY + height);
        if (canPlaceWorkspaceItem(page, panel, current)) return true;

        int gap = LauncherUiUtils.dp(this, 5);
        int maxX = Math.max(0, page.getWidth() - width);
        int maxY = Math.max(0, page.getHeight() - height);
        java.util.TreeSet<Integer> xs = new java.util.TreeSet<Integer>();
        java.util.TreeSet<Integer> ys = new java.util.TreeSet<Integer>();
        xs.add(Math.max(0, Math.min(maxX, startX)));
        ys.add(Math.max(0, Math.min(maxY, startY)));
        xs.add(0);
        xs.add(maxX);
        ys.add(0);
        ys.add(maxY);
        for (int i = 0; i < page.getChildCount(); i++) {
            View other = page.getChildAt(i);
            if (other == panel || other == workspaceTransferProxy
                    || !isWorkspaceItem(other) || other.getVisibility() != View.VISIBLE) continue;
            int[] otherGeometry = itemGeometry(other);
            if (otherGeometry[2] <= 0 || otherGeometry[3] <= 0) continue;
            android.graphics.Rect obstacle = collisionRect(other, otherGeometry);
            xs.add(Math.max(0, Math.min(maxX, obstacle.left - width - gap)));
            xs.add(Math.max(0, Math.min(maxX, obstacle.right + gap)));
            ys.add(Math.max(0, Math.min(maxY, obstacle.top - height - gap)));
            ys.add(Math.max(0, Math.min(maxY, obstacle.bottom + gap)));
        }

        long bestDistance = Long.MAX_VALUE;
        int bestX = -1;
        int bestY = -1;
        for (int x : xs) {
            for (int y : ys) {
                long distance = Math.abs((long) x - startX) + Math.abs((long) y - startY);
                if (distance >= bestDistance) continue;
                android.graphics.Rect candidate = new android.graphics.Rect(
                        x, y, x + width, y + height);
                if (!canPlaceWorkspaceItem(page, panel, candidate)) continue;
                bestDistance = distance;
                bestX = x;
                bestY = y;
            }
        }
        if (bestX < 0) return false;
        setClockVisualPosition(panel, bestX, bestY, true);
        return true;
    }

    private android.graphics.Rect workspaceLayoutCollisionRect(
            View view, int x, int y, int width, int height) {
        int margin = workspaceCollisionMargin(view);
        android.graphics.Rect bounds = isClockWorkspaceItem(view)
                ? clockBackgroundCollisionBoundsAt(view, x, y, width, height)
                : new android.graphics.Rect(
                        x, y, x + Math.max(1, width), y + Math.max(1, height));
        if (margin != 0) bounds.inset(-margin, -margin);
        return bounds;
    }

    private boolean overlapsWithGap(android.graphics.Rect a, android.graphics.Rect b, int gap) {
        android.graphics.Rect expanded = new android.graphics.Rect(
                a.left - gap, a.top - gap, a.right + gap, a.bottom + gap);
        return android.graphics.Rect.intersects(expanded, b);
    }

    /**
     * Returns true only when the proposed visible footprint fits in its page and
     * does not intersect another workspace item. Neighbours are never shifted or resized.
     */
    private boolean canPlaceWorkspaceItem(FrameLayout page, View movingView,
            int left, int top, int width, int height) {
        if (page == null || movingView == null || width <= 0 || height <= 0
                || page.getWidth() <= 0 || page.getHeight() <= 0
                || left < 0 || top < 0
                || left + width > page.getWidth() || top + height > page.getHeight()) return false;
        for (int i = 0; i < page.getChildCount(); i++) {
            View other = page.getChildAt(i);
            if (other == movingView || other == workspaceTransferProxy
                    || !isWorkspaceItem(other)) continue;
            int[] otherGeometry = itemGeometry(other);
            if (otherGeometry[2] <= 0 || otherGeometry[3] <= 0) continue;
            android.graphics.Rect candidate = collisionRectAtForInteraction(
                    movingView, left, top, width, height, other);
            int[] reservedTarget = liveDisplacementTargets.get(other);
            android.graphics.Rect obstacle = reservedTarget == null
                    ? collisionRectForInteraction(other, otherGeometry, movingView)
                    : collisionRectAtForInteraction(other, reservedTarget[0], reservedTarget[1],
                            otherGeometry[2], otherGeometry[3], movingView);
            if (android.graphics.Rect.intersects(candidate, obstacle)) return false;
        }
        return true;
    }

    /** Collision test for a clock resize's already-measured visible rectangle. */
    private boolean canPlaceWorkspaceItem(FrameLayout page, View movingView,
            android.graphics.Rect candidateBounds) {
        if (page == null || movingView == null || candidateBounds == null
                || page.getWidth() <= 0 || page.getHeight() <= 0
                || candidateBounds.left < 0 || candidateBounds.top < 0
                || candidateBounds.right > page.getWidth()
                || candidateBounds.bottom > page.getHeight()) return false;
        int gap = Math.max(1, LauncherUiUtils.dp(this, 5));
        android.graphics.Rect actualCandidate = isClockWorkspaceItem(movingView)
                ? clockVisualBoundsAt(movingView, candidateBounds.left, candidateBounds.top,
                        candidateBounds.width(), candidateBounds.height())
                : new android.graphics.Rect(candidateBounds);
        if (actualCandidate.left < 0 || actualCandidate.top < 0
                || actualCandidate.right > page.getWidth()
                || actualCandidate.bottom > page.getHeight()) return false;
        android.graphics.Rect protectedCandidate = new android.graphics.Rect(actualCandidate);
        protectedCandidate.inset(-gap, -gap);
        for (int i = 0; i < page.getChildCount(); i++) {
            View other = page.getChildAt(i);
            if (other == movingView || other == workspaceTransferProxy
                    || !isWorkspaceItem(other)) continue;
            int[] otherGeometry = itemGeometry(other);
            if (otherGeometry[2] <= 0 || otherGeometry[3] <= 0) continue;
            // Match the edge-constraining pass: grid apps, shortcuts and groups collide
            // by visible artwork/label; widgets collide by their actual container.
            android.graphics.Rect obstacle = collisionRectForInteraction(
                    other, otherGeometry, movingView);
            if (android.graphics.Rect.intersects(protectedCandidate, obstacle)) return false;
        }
        return true;
    }

    /**
     * Limits only the actively dragged clock edge against each workspace item's full
     * layout bounds. Unlike canPlaceWorkspaceItem(), this clamps the requested edge to
     * the nearest safe boundary instead of rejecting the entire resize frame.
     */
    private android.graphics.Rect constrainClockResizeBounds(FrameLayout page, View clock,
            int left, int top, int right, int bottom, int edge, int minWidth, int minHeight) {
        android.graphics.Rect bounds = new android.graphics.Rect(left, top, right, bottom);
        if (page == null || clock == null || edge < 1 || edge > 4) return bounds;
        int gap = Math.max(1, LauncherUiUtils.dp(this, 5));

        // More than one obstacle can constrain the same edge. Revisit the list until
        // the moving edge is stable; only that edge is changed, preserving the anchor.
        int passLimit = Math.max(1, page.getChildCount() + 1);
        for (int pass = 0; pass < passLimit; pass++) {
            boolean changed = false;
            for (int i = 0; i < page.getChildCount(); i++) {
                View other = page.getChildAt(i);
                if (other == clock || other == workspaceTransferProxy
                        || other.getVisibility() != View.VISIBLE || !isWorkspaceItem(other)) continue;
                int[] geometry = itemGeometry(other);
                if (geometry[2] <= 0 || geometry[3] <= 0) continue;

                android.graphics.Rect obstacle = collisionRectForInteraction(
                        other, geometry, clock);
                changed |= constrainClockResizeAgainstObstacle(
                        bounds, obstacle, edge, minWidth, minHeight, gap);
                int[] reserved = liveDisplacementTargets.get(other);
                if (reserved != null) {
                    android.graphics.Rect reservedBounds = collisionRectAtForInteraction(
                            other, reserved[0], reserved[1],
                            geometry[2], geometry[3], clock);
                    changed |= constrainClockResizeAgainstObstacle(
                            bounds, reservedBounds, edge, minWidth, minHeight, gap);
                }
            }
            if (!changed) break;
        }

        if (edge == 1) bounds.left = Math.max(0,
                Math.min(bounds.left, bounds.right - minWidth));
        else if (edge == 2) bounds.right = Math.min(page.getWidth(),
                Math.max(bounds.right, bounds.left + minWidth));
        else if (edge == 3) bounds.top = Math.max(0,
                Math.min(bounds.top, bounds.bottom - minHeight));
        else bounds.bottom = Math.min(page.getHeight(),
                Math.max(bounds.bottom, bounds.top + minHeight));
        return bounds;
    }

    private boolean constrainClockResizeAgainstObstacle(android.graphics.Rect bounds,
            android.graphics.Rect obstacle, int edge, int minWidth, int minHeight, int gap) {
        int oldMovingEdge;
        int newMovingEdge;
        if (edge == 1) {
            if (bounds.top >= obstacle.bottom || bounds.bottom <= obstacle.top
                    || bounds.right <= obstacle.left || bounds.left >= obstacle.right + gap) {
                return false;
            }
            oldMovingEdge = bounds.left;
            newMovingEdge = Math.min(bounds.right - minWidth,
                    Math.max(bounds.left, obstacle.right + gap));
            if (newMovingEdge <= oldMovingEdge) return false;
            bounds.left = newMovingEdge;
            return true;
        }
        if (edge == 2) {
            if (bounds.top >= obstacle.bottom || bounds.bottom <= obstacle.top
                    || bounds.left >= obstacle.right || bounds.right <= obstacle.left - gap) {
                return false;
            }
            oldMovingEdge = bounds.right;
            newMovingEdge = Math.max(bounds.left + minWidth,
                    Math.min(bounds.right, obstacle.left - gap));
            if (newMovingEdge >= oldMovingEdge) return false;
            bounds.right = newMovingEdge;
            return true;
        }
        if (edge == 3) {
            if (bounds.left >= obstacle.right || bounds.right <= obstacle.left
                    || bounds.bottom <= obstacle.top || bounds.top >= obstacle.bottom + gap) {
                return false;
            }
            oldMovingEdge = bounds.top;
            newMovingEdge = Math.min(bounds.bottom - minHeight,
                    Math.max(bounds.top, obstacle.bottom + gap));
            if (newMovingEdge <= oldMovingEdge) return false;
            bounds.top = newMovingEdge;
            return true;
        }
        if (bounds.left >= obstacle.right || bounds.right <= obstacle.left
                || bounds.top >= obstacle.bottom || bounds.bottom <= obstacle.top - gap) {
            return false;
        }
        oldMovingEdge = bounds.bottom;
        newMovingEdge = Math.max(bounds.top + minHeight,
                Math.min(bounds.bottom, obstacle.top - gap));
        if (newMovingEdge >= oldMovingEdge) return false;
        bounds.bottom = newMovingEdge;
        return true;
    }

    private void showWorkspaceOverlapRejected() {
        Toast.makeText(this, "Not possible due to overlap", Toast.LENGTH_SHORT).show();
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
        if (prefs != null && prefs.getInt(KEY_CLOCK_STYLE, 0) != 5
                && isClockWorkspaceItem(panel)) {
            android.graphics.Rect footprint = clockVisualBoundsAt(panel,
                    Math.round(visualLeft), Math.round(visualTop),
                    Math.max(1, Math.round(visualWidth)),
                    Math.max(1, Math.round(visualHeight)));
            float correctionX = 0f;
            float correctionY = 0f;
            if (footprint.width() <= parentWidth) {
                if (footprint.left < 0) correctionX = -footprint.left;
                else if (footprint.right > parentWidth) correctionX = parentWidth - footprint.right;
            } else {
                correctionX = (parentWidth - footprint.width()) * 0.5f - footprint.left;
            }
            if (footprint.height() <= parentHeight) {
                if (footprint.top < 0) correctionY = -footprint.top;
                else if (footprint.bottom > parentHeight) correctionY = parentHeight - footprint.bottom;
            } else {
                correctionY = -footprint.top;
            }
            translationX += correctionX;
            translationY += correctionY;
            return new float[]{translationX, translationY};
        }
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

    /**
     * If any transformed/layout edge case still leaves the constrained candidate too
     * close to an invisible workspace bound, shrink only the actively moving edge until
     * the nearest legal rectangle is found. This avoids freezing one resize frame.
     */
    private android.graphics.Rect settleClockResizeBounds(FrameLayout page, View clock,
            android.graphics.Rect requested, int edge, int minWidth, int minHeight) {
        android.graphics.Rect candidate = new android.graphics.Rect(requested);
        if (canApplyClockResizeCandidate(page, clock, candidate, edge)) return candidate;
        int maxSteps = edge == 1 || edge == 2
                ? Math.max(0, candidate.width() - minWidth)
                : Math.max(0, candidate.height() - minHeight);
        if (maxSteps <= 0) return candidate;

        // Shrinking toward the fixed opposite edge is monotonic for this resize axis.
        // Check only the newly occupied strip, not the clock's already-existing area.
        // Otherwise a single pre-existing overlap makes even the minimum fail and the
        // resizer appears to hit an invisible wall.
        android.graphics.Rect minimum = resizeBoundsAfterRetreat(requested, edge, maxSteps,
                minWidth, minHeight);
        if (!canApplyClockResizeCandidate(page, clock, minimum, edge)) return candidate;

        // Find the smallest retreat that makes the added strip legal.
        int low = 1;
        int high = maxSteps;
        while (low < high) {
            int middle = low + (high - low) / 2;
            android.graphics.Rect trial = resizeBoundsAfterRetreat(requested, edge, middle,
                    minWidth, minHeight);
            if (canApplyClockResizeCandidate(page, clock, trial, edge)) high = middle;
            else low = middle + 1;
        }
        return resizeBoundsAfterRetreat(requested, edge, low, minWidth, minHeight);
    }

    private android.graphics.Rect resizeBoundsAfterRetreat(android.graphics.Rect requested,
            int edge, int retreat, int minWidth, int minHeight) {
        android.graphics.Rect trial = new android.graphics.Rect(requested);
        if (edge == 1) trial.left = Math.min(trial.right - minWidth, requested.left + retreat);
        else if (edge == 2) trial.right = Math.max(trial.left + minWidth, requested.right - retreat);
        else if (edge == 3) trial.top = Math.min(trial.bottom - minHeight, requested.top + retreat);
        else if (edge == 4) trial.bottom = Math.max(trial.top + minHeight, requested.bottom - retreat);
        return trial;
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
    /**
     * Restore a displaced row/column as one transaction. If several grid items were
     * pushed by the same cell vector, returning them one at a time can let earlier items
     * occupy later items' original slots and shuffle their visual order. Either every
     * member of that lane can return safely, or none of them returns on this MOVE frame.
     *
     * @return true when this item belongs to a multi-item lane cohort, including when the
     *         whole cohort must wait; false means the caller may use ordinary item restore.
     */
    private boolean tryRestoreDisplacedGridLaneTogether(FrameLayout page, View movingView,
            int[] movingGeometry, java.util.List<View> candidates, View seed) {
        if (page == null || movingView == null || movingGeometry == null || seed == null
                || !isHomeGridIcon(seed) || seed.getParent() != page) return false;
        int[] seedOriginal = liveDisplacedItems.get(seed);
        int[] seedTarget = liveDisplacementTargets.get(seed);
        if (seedOriginal == null || seedTarget == null) return false;
        int shiftX = seedTarget[0] - seedOriginal[0];
        int shiftY = seedTarget[1] - seedOriginal[1];
        // Grid lanes translate along a single axis. Non-grid diagonal pushes keep their
        // existing independent collision handling.
        if ((shiftX == 0) == (shiftY == 0)) return false;
        boolean shortcutLane = isWorkspaceContactShortcut(seed);

        java.util.ArrayList<View> lane = new java.util.ArrayList<View>();
        java.util.IdentityHashMap<View, Boolean> laneSet =
                new java.util.IdentityHashMap<View, Boolean>();
        lane.add(seed);
        laneSet.put(seed, Boolean.TRUE);
        for (View candidate : candidates) {
            if (candidate == seed || candidate == movingView || candidate.getParent() != page
                    || !isHomeGridIcon(candidate)
                    || isWorkspaceContactShortcut(candidate) != shortcutLane) continue;
            int[] original = liveDisplacedItems.get(candidate);
            int[] target = liveDisplacementTargets.get(candidate);
            if (original == null || target == null) continue;
            if (target[0] - original[0] != shiftX
                    || target[1] - original[1] != shiftY) continue;
            boolean sameLane = shiftX != 0
                    ? original[1] == seedOriginal[1]
                    : original[0] == seedOriginal[0];
            if (sameLane && !laneSet.containsKey(candidate)) {
                lane.add(candidate);
                laneSet.put(candidate, Boolean.TRUE);
            }
        }
        if (lane.size() < 2) return false;

        boolean canRestoreWholeLane = true;
        for (View member : lane) {
            int[] original = liveDisplacedItems.get(member);
            int[] geometry = itemGeometry(member);
            if (original == null || geometry[2] <= 0 || geometry[3] <= 0) {
                canRestoreWholeLane = false;
                break;
            }
            android.graphics.Rect originalBounds = collisionRectAtForInteraction(
                    member, original[0], original[1], geometry[2], geometry[3], movingView);
            if (originalBounds.left < 0 || originalBounds.top < 0
                    || originalBounds.right > page.getWidth()
                    || originalBounds.bottom > page.getHeight()
                    || android.graphics.Rect.intersects(originalBounds,
                            collisionRectForInteraction(movingView, movingGeometry, member))) {
                canRestoreWholeLane = false;
                break;
            }

            for (int i = 0; i < page.getChildCount(); i++) {
                View blocker = page.getChildAt(i);
                if (blocker == member || blocker == movingView || laneSet.containsKey(blocker)
                        || blocker.getVisibility() != View.VISIBLE
                        || !isWorkspaceItem(blocker)) continue;
                int[] blockerGeometry = itemGeometry(blocker);
                if (blockerGeometry[2] <= 0 || blockerGeometry[3] <= 0) continue;
                android.graphics.Rect currentBounds = collisionRectForInteraction(
                        blocker, blockerGeometry, member);
                if (android.graphics.Rect.intersects(originalBounds, currentBounds)) {
                    canRestoreWholeLane = false;
                    break;
                }
                int[] blockerTarget = liveDisplacementTargets.get(blocker);
                if (blockerTarget != null) {
                    android.graphics.Rect reservedBounds = collisionRectAtForInteraction(
                            blocker, blockerTarget[0], blockerTarget[1],
                            blockerGeometry[2], blockerGeometry[3], member);
                    if (android.graphics.Rect.intersects(originalBounds, reservedBounds)) {
                        canRestoreWholeLane = false;
                        break;
                    }
                }
            }
            if (!canRestoreWholeLane) break;
        }

        if (!canRestoreWholeLane) {
            // Keep the group in its current shared displacement until every original
            // slot is free; never allow a partial restore to reorder the row.
            return true;
        }

        // Reserve all original positions before queuing any animation. Grid animation
        // batching then gives the complete lane one shared translation vector.
        for (View member : lane) {
            int[] original = liveDisplacedItems.get(member);
            liveDisplacementTargets.put(member, new int[]{original[0], original[1]});
        }
        for (View member : lane) {
            int[] original = liveDisplacedItems.get(member);
            animateDisplacedItemTo(member, original[0], original[1]);
        }
        return true;
    }

    /**
     * Reverse temporary displacement as the finger moves back out of occupied space.
     * Grid lanes are restored atomically; individual widgets and isolated icons retain
     * the existing rectangle-based restore behavior.
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

            if (tryRestoreDisplacedGridLaneTogether(
                    page, movingView, movingGeometry, candidates, displaced)) continue;

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
            boolean appIsMover = isWorkspaceAppIcon(movingView)
                    || ("workspace_transfer_proxy".equals(movingView.getTag())
                        && isWorkspaceAppIcon(workspaceTransferItem));
            if (appIsMover && isAppGroupItem(other)) continue;

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

            if ((!alreadyDisplaced || targetCollides)
                    && isHomeGridDragMover(movingView)
                    && isHomeGridIcon(other)) {
                // Prefer moving the occupied row/column as one transaction. The helper
                // reserves every lane member's destination before animation; only fall
                // back to the minimum contiguous chain when no complete lane shift is safe.
                if (tryShiftHomeIconLaneCohort(
                        page, movingView, other, bestX, bestY, pushDx, pushDy)) {
                    continue;
                }
                if (tryShiftHomeIconCohort(
                        page, movingView, other, bestX, bestY, pushDx, pushDy)) {
                    continue;
                }
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
        int[] requestedBeforeSafety = itemGeometry(movingView);
        boolean collisionSafetyRejected = enforceLiveDragCollisionSafety(page, movingView);
        int[] acceptedAfterSafety = itemGeometry(movingView);
        if (collisionSafetyRejected
                || requestedBeforeSafety[0] != acceptedAfterSafety[0]
                || requestedBeforeSafety[1] != acceptedAfterSafety[1]) {
            liveDragRejectedMoves.put(movingView, Boolean.TRUE);
        } else {
            liveDragRejectedMoves.remove(movingView);
        }
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
    private boolean enforceLiveDragCollisionSafety(FrameLayout page, View movingView) {
        if (page == null || movingView == null || movingView.getParent() != page
                || "workspace_transfer_proxy".equals(movingView.getTag())) return false;
        if (movingView == workspaceTransferItem
                && workspaceTransferOriginPage >= 0 && workspaceTransferTargetPage >= 0
                && workspaceTransferTargetPage != workspaceTransferOriginPage) return false;

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
            int[] reserved = liveDisplacementTargets.get(other);
            android.graphics.Rect otherBounds = reserved == null
                    ? collisionRectForInteraction(other, otherGeometry, movingView)
                    : collisionRectAtForInteraction(other, reserved[0], reserved[1],
                            otherGeometry[2], otherGeometry[3], movingView);
            if (android.graphics.Rect.intersects(movingBounds, otherBounds)) {
                collision = true;
                break;
            }
        }
        if (!collision) {
            liveDragLastSafeGeometry.put(movingView,
                    new int[]{geometry[0], geometry[1]});
            return false;
        }

        // Do not rewind the active drag owner during MOVE. Widgets and clocks used
        // to snap back to the last safe rectangle whenever a neighbour's displacement
        // animation had not finished yet, which made movement appear frozen. Keep the
        // finger-following geometry intact and reject only if the overlap is unresolved
        // when the transaction is finalized.
        return true;
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
        java.util.ArrayList<View> priorItems = new java.util.ArrayList<View>();
        for (int i = 0; i < page.getChildCount(); i++) {
            View item = page.getChildAt(i);
            if (!isWorkspaceItem(item) || item.getVisibility() != View.VISIBLE) continue;
            int[] geometry = itemGeometry(item);
            if (geometry[2] <= 0 || geometry[3] <= 0) continue;
            android.graphics.Rect bounds = isClockWorkspaceItem(item)
                    ? clockBackgroundCollisionBoundsAt(item, geometry[0], geometry[1],
                            geometry[2], geometry[3])
                    : (isHomeGridIcon(item)
                            ? appIconVisualRectAt(item, geometry[0], geometry[1],
                                    geometry[2], geometry[3])
                            : new android.graphics.Rect(geometry[0], geometry[1],
                                    geometry[0] + geometry[2], geometry[1] + geometry[3]));
            if (bounds.left < 0 || bounds.top < 0
                    || bounds.right > page.getWidth() || bounds.bottom > page.getHeight()) {
                return true;
            }
            for (View prior : priorItems) {
                int[] priorGeometry = itemGeometry(prior);
                android.graphics.Rect currentBounds =
                        collisionRectForInteraction(item, geometry, prior);
                android.graphics.Rect priorBounds =
                        collisionRectForInteraction(prior, priorGeometry, item);
                if (android.graphics.Rect.intersects(currentBounds, priorBounds)) return true;
            }
            priorItems.add(item);
        }
        return false;
    }

    private boolean overlapsAcceptedWorkspaceItemsAt(
            View item, int x, int y, int width, int height,
            java.util.ArrayList<View> acceptedViews) {
        if (item == null || acceptedViews == null || acceptedViews.isEmpty()) return false;
        for (View prior : acceptedViews) {
            if (prior == null || prior == item) continue;
            int[] priorGeometry = itemGeometry(prior);
            if (priorGeometry[2] <= 0 || priorGeometry[3] <= 0) continue;
            android.graphics.Rect candidate =
                    collisionRectAtForInteraction(item, x, y, width, height, prior);
            android.graphics.Rect obstacle =
                    collisionRectForInteraction(prior, priorGeometry, item);
            if (android.graphics.Rect.intersects(candidate, obstacle)) return true;
        }
        return false;
    }

    private void resolveAllWorkspaceOverlaps(FrameLayout page, View priorityView) {
        resolveAllWorkspaceOverlaps(page, priorityView, false);
    }

    private void resolveAllWorkspaceOverlaps(FrameLayout page, View priorityView,
                                             boolean preservePrioritySize) {
        if (page == null || page.getWidth() <= 0 || page.getHeight() <= 0) return;
        // The legacy resolver always rebuilt a nearest-free plan for every item,
        // even when the page was already visually collision-free. That caused needless
        // position changes and a costly post-drop pause. Make it a true no-op in that case.
        if (!hasWorkspaceVisualOverlaps(page)) return;
        java.util.ArrayList<View> items = new java.util.ArrayList<View>();
        // For explicit widget resizing, reserve the selected widget's exact dimensions first;
        // surrounding workspace items are then moved around that rectangle.
        boolean priorityIsWorkspaceItem = priorityView != null
                && priorityView.getParent() == page && isWorkspaceItem(priorityView);
        if (preservePrioritySize && priorityIsWorkspaceItem) items.add(priorityView);
        for (int i = 0; i < page.getChildCount(); i++) {
            View child = page.getChildAt(i);
            if (child == priorityView || !isWorkspaceItem(child)) continue;
            if (!preservePrioritySize
                    && (isClockWorkspaceItem(child) || isAppGroupItem(child))) items.add(0, child);
            else items.add(child);
        }
        if (!preservePrioritySize && priorityIsWorkspaceItem) items.add(priorityView);
        java.util.ArrayList<android.graphics.Rect> accepted = new java.util.ArrayList<android.graphics.Rect>();
        java.util.ArrayList<View> acceptedViews = new java.util.ArrayList<View>();
        for (View item : items) {
            if (item.getTag() instanceof String && prefs != null) {
                String tag = (String) item.getTag();
                String baseXKey = "workspace_base_scale_x_" + tag;
                String baseYKey = "workspace_base_scale_y_" + tag;
                if (prefs.contains(baseXKey) || prefs.contains(baseYKey)) {
                    item.setScaleX(getFloatPreferenceCompat(baseXKey, 1f));
                    item.setScaleY(getFloatPreferenceCompat(baseYKey, 1f));
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
            boolean currentOnLegalGridSlot = true;
            if (isHomeGridIcon(item)) {
                int gridColumns = homeGridColumnsForItem(item);
                int gridCellWidth = Math.max(1, page.getWidth() / Math.max(1, gridColumns));
                int iconSize = prefs == null ? 48 : prefs.getInt(KEY_ICON_SIZE, 48);
                int gridCellHeight = LauncherUiUtils.dp(this, homeCellHeightDp(iconSize));
                int gridRows = homeGridRowsForHeight(page.getHeight(), gridCellHeight);
                int topForSlot = isWorkspaceContactShortcut(item)
                        ? g[1] - LauncherUiUtils.dp(this, 14) : g[1];
                int gridCol = Math.max(0, Math.min(gridColumns - 1,
                        Math.round(g[0] / (float) gridCellWidth)));
                int gridRow = homeGridRowFromTop(topForSlot, gridRows, page.getHeight(), gridCellHeight);
                int expectedX = homeGridIconLeftAt(item, gridCol, gridCellWidth, w);
                int expectedY = homeGridIconTopAt(page, item, gridRow, gridRows, gridCellHeight);
                currentOnLegalGridSlot = Math.abs(expectedX - g[0]) <= 1
                        && Math.abs(expectedY - g[1]) <= 1;
            }
            if (currentInsidePage && currentOnLegalGridSlot
                    && !overlapsAcceptedWorkspaceItemsAt(
                            item, g[0], g[1], w, h, acceptedViews)) {
                accepted.add(collisionRectAt(item, g[0], g[1], w, h));
                acceptedViews.add(item);
                continue;
            }

            int bestX = -1, bestY = -1;
            long bestDistance = Long.MAX_VALUE;
            for (Integer cy : ys) for (Integer cx : xs) {
                int x = cx.intValue(), y = cy.intValue();
                if (overlapsAcceptedWorkspaceItemsAt(item, x, y, w, h, acceptedViews)) continue;
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
                        if (!overlapsAcceptedWorkspaceItemsAt(
                                item, candidateX, candidateY, w, h, acceptedViews)) {
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
                    if (!overlapsAcceptedWorkspaceItemsAt(
                            item, edge[0], edge[1], w, h, acceptedViews)) {
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

            // Never silently resize an app widget or the custom clock as a collision fallback.
            // If the page is full, retain their chosen dimensions instead of scaling them down.
            boolean keepWidgetSize = isClockWorkspaceItem(item)
                    || (item.getTag() instanceof String
                    && ((String) item.getTag()).startsWith("launcher_widget:"));
            if (bestX < 0 && !keepWidgetSize) {
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
                            if (!overlapsAcceptedWorkspaceItemsAt(
                                    item, candidateX, candidateY, w, h, acceptedViews)) {
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
        } else if (tag.startsWith(APP_GROUP_TAG_PREFIX)) {
            String id = tag.substring(APP_GROUP_TAG_PREFIX.length());
            int columns = Math.max(1, getHomeColumns());
            int cellWidth = Math.max(1, page.getWidth() / columns);
            int cellHeight = LauncherUiUtils.dp(this,
                    homeCellHeightDp(prefs.getInt(KEY_ICON_SIZE, 48)));
            int rows = homeGridRowsForHeight(page.getHeight(), cellHeight);
            int col = Math.max(0, Math.min(columns - 1, Math.round(x / (float) cellWidth)));
            int row = homeGridRowFromTop(y, rows, page.getHeight(), cellHeight);
            prefs.edit().putInt(appGroupKey("page", id), pageIndex)
                    .putFloat(appGroupKey("x", id), col / (float) Math.max(1, columns - 1))
                    .putFloat(appGroupKey("y", id), row / (float) Math.max(1, rows - 1))
                    .apply();
        } else if ("launcher_clock_widget".equals(tag)) {
            prefs.edit().putFloat(clockPrefKey(KEY_CLOCK_WIDGET_X, pageIndex), item.getTranslationX())
                    .putFloat(clockPrefKey(KEY_CLOCK_WIDGET_Y, pageIndex), item.getTranslationY()).apply();
        }
    }

    /** Finalize temporary push-aside positions on drop, or roll them back on cancel. */
    private boolean finishLiveDisplacements(boolean commit, View movingView) {
        FrameLayout pendingPage = pendingLiveDisplacementPage;
        View pendingMover = pendingLiveDisplacementMover;
        cancelPendingLiveDisplacementUpdate();
        boolean rejected = commit && Boolean.TRUE.equals(liveDragRejectedMoves.get(movingView));
        if (commit && !rejected && pendingPage != null && pendingMover == movingView
                && pendingMover.getParent() == pendingPage) {
            updateLiveDisplacements(pendingPage, pendingMover);
        }
        if (commit && Boolean.TRUE.equals(liveDragRejectedMoves.get(movingView))) {
            rejected = true;
            commit = false;
        }
        if (rejected) commit = false;
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
        liveDragRejectedMoves.remove(movingView);
        if (commit || isClockWorkspaceItem(movingView)) {
            liveDragLastSafeGeometry.remove(movingView);
        }
        if (rejected) showWorkspaceOverlapRejected();
        refreshWorkspaceGrid();
        return !rejected;
    }

    /**
     * All workspace item types use the same rectangle-based collision resolver.
     * Keeping a widget-only resolver here caused it to disagree with live drag,
     * shortcut placement and clock displacement, which could re-overlap items
     * immediately after another resolver had moved them.
     */
    private void resolveWidgetIconCollisions(FrameLayout page, FrameLayout widgetContainer) {
        // Widget insertion/restoration owns its placement decision. Provider updates must
        // never relocate surrounding icons, shortcuts, clocks, or other widgets.
        if (page == null || widgetContainer == null || widgetContainer.getParent() != page) return;
    }

    private void normalizeClockVisualModePreferences() {
        boolean matchArtwork = prefs.getBoolean(KEY_CLOCK_ICON_ARTWORK, false);
        boolean digitGradient = prefs.getBoolean(KEY_CLOCK_DIGIT_GRADIENT, false);
        boolean liveGradient = prefs.getBoolean(KEY_CLOCK_LIVE_GRADIENT, false);
        SharedPreferences.Editor edit = prefs.edit();
        boolean changed = false;
        if (matchArtwork && (digitGradient || liveGradient)) {
            // Prefer the explicitly enabled artwork switch when migrating the
            // conflicting state written by earlier builds.
            edit.putBoolean(KEY_CLOCK_DIGIT_GRADIENT, false)
                    .putBoolean(KEY_CLOCK_LIVE_GRADIENT, false);
            changed = true;
        } else if (!matchArtwork && liveGradient && !digitGradient) {
            edit.putBoolean(KEY_CLOCK_DIGIT_GRADIENT, true);
            changed = true;
        }
        if (changed) edit.apply();
    }

    private void normalizeAppIconSettings() {
        if (prefs == null) return;
        int homeSize = prefs.getInt(KEY_ICON_SIZE, 48);
        int drawerSize = prefs.getInt(KEY_DRAWER_ICON_SIZE, 48);
        int homeColumns = Math.max(3, Math.min(6, prefs.getInt(KEY_COLUMNS, COLUMNS)));
        int drawerColumns = Math.max(3, Math.min(6, prefs.getInt(KEY_DRAWER_COLUMNS, COLUMNS)));
        SharedPreferences.Editor edit = prefs.edit();

        // Migrate the old shape IDs once: Original -> Square, Triangle -> Square,
        // Pear -> Tilted Pear, Rounded Hexagon -> Hexagon. This prevents stale saved
        // integers from silently selecting the wrong new shape after the options change.
        if (!prefs.getBoolean(KEY_ICON_SHAPE_MIGRATION, false)) {
            int oldShape = prefs.getInt("app_icon_shape", LauncherIconDrawable.SHAPE_SQUARE);
            int migratedShape;
            switch (oldShape) {
                case 2: migratedShape = LauncherIconDrawable.SHAPE_TILTED_PEAR; break;
                case 3: migratedShape = LauncherIconDrawable.SHAPE_HEXAGON; break;
                case 0:
                case 1:
                default: migratedShape = LauncherIconDrawable.SHAPE_SQUARE; break;
            }
            edit.putInt("app_icon_shape", migratedShape);
            edit.putBoolean(KEY_ICON_SHAPE_MIGRATION, true);
        }

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
        if (homeFont < 8 || homeFont > 15) edit.putInt(KEY_HOME_FONT_SIZE, Math.max(8, Math.min(15, homeFont)));
        if (drawerFont < 8 || drawerFont > 15) edit.putInt(KEY_DRAWER_FONT_SIZE, Math.max(8, Math.min(15, drawerFont)));
        // Capture the current layout's row origin once. Later icon-size changes keep this
        // anchor stable, like the drawer grid, rather than moving every row to re-fill the page.
        if (!prefs.contains(KEY_HOME_GRID_ANCHOR_ICON_SIZE)) {
            edit.putInt(KEY_HOME_GRID_ANCHOR_ICON_SIZE, homeSize);
        }
        if (!prefs.contains(KEY_HOME_GRID_ANCHOR_CELL_HEIGHT_DP)) {
            edit.putInt(KEY_HOME_GRID_ANCHOR_CELL_HEIGHT_DP, homeCellHeightDp(homeSize));
        }
        // Keep every icon inside its slot at the selected size. Preserve normalized
        // positions instead of clearing them when a large size reduces column capacity.
        if (homeSize > 64 && homeColumns > 5) {
            homeColumns = 5;
            edit.putInt(KEY_COLUMNS, homeColumns);
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
     * Preserve logical workspace slots across icon-size and column changes. Legal
     * app/group slots stay fixed; only slots made invalid by the new row/column
     * capacity are assigned to the nearest free cell. Shortcuts use their own
     * four-column mapping but share the same vertical row metric.
     */
    private void remapHomeIconSlotsForColumnChange(int oldColumns, int newColumns,
            int oldIconSize, int targetIconSize) {
        remapHomeIconSlotsForLayoutChange(oldColumns, newColumns, oldIconSize,
                targetIconSize, -1, -1);
    }

    private void remapHomeIconSlotsForLayoutChange(int oldColumns, int newColumns,
            int oldIconSize, int targetIconSize, int oldCellHeightDpOverride,
            int newCellHeightDpOverride) {
        oldColumns = Math.max(3, Math.min(6, oldColumns));
        newColumns = Math.max(3, Math.min(6, newColumns));
        oldIconSize = Math.max(32, Math.min(74, oldIconSize));
        targetIconSize = Math.max(32, Math.min(74, targetIconSize));
        boolean explicitCellHeights = oldCellHeightDpOverride > 0
                && newCellHeightDpOverride > 0;
        if (pagesStrip == null
                || (oldColumns == newColumns && oldIconSize == targetIconSize
                        && (!explicitCellHeights
                                || oldCellHeightDpOverride == newCellHeightDpOverride))) return;

        int oldCellHeight = LauncherUiUtils.dp(this, explicitCellHeights
                ? oldCellHeightDpOverride : homeCellHeightDp(oldIconSize));
        int newCellHeight = LauncherUiUtils.dp(this, explicitCellHeights
                ? newCellHeightDpOverride : homeCellHeightDp(targetIconSize));
        int fallbackWidth = Math.max(1, getResources().getDisplayMetrics().widthPixels
                - LauncherUiUtils.dp(this, 40));
        int fallbackHeight = Math.max(LauncherUiUtils.dp(this, 360),
                getResources().getDisplayMetrics().heightPixels
                        - LauncherUiUtils.dp(this, 260));

        for (int pageIndex = 0; pageIndex < Math.min(2, pagesStrip.getChildCount()); pageIndex++) {
            View pageView = pagesStrip.getChildAt(pageIndex);
            if (!(pageView instanceof FrameLayout)) continue;
            FrameLayout page = (FrameLayout) pageView;
            int width = page.getWidth() > 0 ? page.getWidth()
                    : (pageViewport != null && pageViewport.getWidth() > 0
                            ? pageViewport.getWidth() : fallbackWidth);
            int height = page.getHeight() > 0 ? page.getHeight()
                    : (pageViewport != null && pageViewport.getHeight() > 0
                            ? pageViewport.getHeight() : fallbackHeight);
            int oldCellWidth = Math.max(1, width / oldColumns);
            int oldRows = Math.max(1, homeGridRowsForHeight(height, oldCellHeight));
            int rows = Math.max(1, homeGridRowsForHeight(height, newCellHeight));

            final java.util.ArrayList<View> slotItems = new java.util.ArrayList<View>();
            final java.util.IdentityHashMap<View, int[]> oldSlots =
                    new java.util.IdentityHashMap<View, int[]>();
            for (int i = 0; i < page.getChildCount(); i++) {
                View child = page.getChildAt(i);
                if ((!isWorkspaceAppIcon(child) && !isAppGroupItem(child))
                        || !(child.getLayoutParams() instanceof FrameLayout.LayoutParams)) continue;
                FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) child.getLayoutParams();
                int col = Math.max(0, Math.min(oldColumns - 1,
                        Math.round(lp.leftMargin / (float) oldCellWidth)));
                int row = homeGridRowFromTop(lp.topMargin, oldRows, height, oldCellHeight);
                slotItems.add(child);
                oldSlots.put(child, new int[]{col, row});
            }
            java.util.Collections.sort(slotItems, (first, second) -> {
                int[] a = oldSlots.get(first);
                int[] b = oldSlots.get(second);
                int rowOrder = Integer.compare(a[1], b[1]);
                if (rowOrder != 0) return rowOrder;
                if (a[0] != b[0]) return Integer.compare(a[0], b[0]);
                // When stale positions already collide, preserve the group and let
                // the ordinary icon move to the next free cell.
                if (isAppGroupItem(first) != isAppGroupItem(second)) {
                    return isAppGroupItem(first) ? -1 : 1;
                }
                return 0;
            });

            boolean[][] occupied = new boolean[rows][newColumns];
            java.util.IdentityHashMap<View, int[]> assigned =
                    new java.util.IdentityHashMap<View, int[]>();

            // First pin every still-legal slot. This avoids moving a valid item just
            // because another item that overflows is visited earlier.
            for (View item : slotItems) {
                int[] old = oldSlots.get(item);
                if (old[0] < 0 || old[0] >= newColumns || old[1] < 0 || old[1] >= rows
                        || occupied[old[1]][old[0]]) continue;
                occupied[old[1]][old[0]] = true;
                assigned.put(item, new int[]{old[0], old[1]});
            }

            // Only items whose old slot is no longer legal (or was duplicated) move.
            for (View item : slotItems) {
                if (assigned.containsKey(item)) continue;
                int[] old = oldSlots.get(item);
                int wantedCol = Math.max(0, Math.min(newColumns - 1, old[0]));
                int wantedRow = Math.max(0, Math.min(rows - 1, old[1]));
                int bestCol = -1;
                int bestRow = -1;
                int bestDistance = Integer.MAX_VALUE;
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
                if (bestCol < 0) {
                    // An over-capacity page will still be repaired by the visible-bounds
                    // collision pass after rendering; never drop the item's saved slot.
                    bestCol = wantedCol;
                    bestRow = wantedRow;
                }
                assigned.put(item, new int[]{bestCol, bestRow});
                if (bestRow >= 0 && bestRow < rows
                        && bestCol >= 0 && bestCol < newColumns) {
                    occupied[bestRow][bestCol] = true;
                }
            }

            SharedPreferences.Editor edit = prefs.edit();
            for (View item : slotItems) {
                int[] slot = assigned.get(item);
                int[] oldSlot = oldSlots.get(item);
                if (slot == null || oldSlot == null) continue;
                float normalizedX = slot[0] / (float) Math.max(1, newColumns - 1);
                float normalizedY = slot[1] / (float) Math.max(1, rows - 1);
                // A size-only change must not rewrite a still-valid item's coordinates.
                // Re-encode only an axis whose grid capacity changed or whose slot moved.
                boolean updateX = oldColumns != newColumns || slot[0] != oldSlot[0];
                boolean updateY = oldRows != rows || slot[1] != oldSlot[1];
                if (isAppGroupItem(item)) {
                    String groupId = appGroupId(item);
                    if (groupId != null) {
                        if (prefs.getInt(appGroupKey("page", groupId), pageIndex) != pageIndex) {
                            edit.putInt(appGroupKey("page", groupId), pageIndex);
                        }
                        if (updateX) edit.putFloat(appGroupKey("x", groupId), normalizedX);
                        if (updateY) edit.putFloat(appGroupKey("y", groupId), normalizedY);
                    }
                } else if (isWorkspaceAppIcon(item)) {
                    String tag = String.valueOf(item.getTag());
                    String packageName = tag.substring("launcher_app:".length());
                    if (prefs.getInt(KEY_ICON_PAGE + packageName, pageIndex) != pageIndex) {
                        edit.putInt(KEY_ICON_PAGE + packageName, pageIndex);
                    }
                    if (updateX) edit.putFloat(KEY_ICON_X + packageName, normalizedX);
                    if (updateY) edit.putFloat(KEY_ICON_Y + packageName, normalizedY);
                }
            }
            edit.apply();
        }

        // Saved contact shortcuts occupy a separate four-column grid. Preserve their
        // column and row when possible, remapping only rows invalidated by a taller slot.
        java.util.ArrayList<String> shortcutIds = getSavedShortcutIds();
        SharedPreferences.Editor shortcutEdit = prefs.edit();
        boolean changedShortcuts = false;
        for (String shortcutId : shortcutIds) {
            String gridKey = shortcutPrefKey(KEY_SHORTCUT_GRID_POSITION, shortcutId);
            if (!prefs.getBoolean(gridKey, false)) continue;
            int pageIndex = Math.max(0, Math.min(1,
                    prefs.getInt(shortcutPrefKey(KEY_SHORTCUT_PAGE, shortcutId), 0)));
            if (pageIndex >= pagesStrip.getChildCount()
                    || !(pagesStrip.getChildAt(pageIndex) instanceof FrameLayout)) continue;
            FrameLayout page = (FrameLayout) pagesStrip.getChildAt(pageIndex);
            int pageHeight = page.getHeight() > 0 ? page.getHeight()
                    : (pageViewport != null && pageViewport.getHeight() > 0
                            ? pageViewport.getHeight() : fallbackHeight);
            int oldRows = Math.max(1, homeGridRowsForHeight(pageHeight, oldCellHeight));
            int rows = Math.max(1, homeGridRowsForHeight(pageHeight, newCellHeight));
            // A row-preserving resize should leave a shortcut's stored X untouched.
            if (oldRows == rows) continue;
            float savedY = getFloatPreferenceCompat(
                    shortcutPrefKey(KEY_SHORTCUT_Y, shortcutId), -1f);
            if (savedY < 0f) continue;
            int oldRow = Math.max(0, Math.min(oldRows - 1, Math.round(
                    Math.min(1f, savedY) * Math.max(1, oldRows - 1))));
            int row = Math.max(0, Math.min(rows - 1, oldRow));
            shortcutEdit.putFloat(shortcutPrefKey(KEY_SHORTCUT_Y, shortcutId),
                    row / (float) Math.max(1, rows - 1));
            changedShortcuts = true;
        }
        if (changedShortcuts) shortcutEdit.apply();
    }

    /**
     * Resize-only collision recovery. Keep apps/groups in legal grid slots first; then
     * shrink an overlapping clock/widget, relocate it on its current page, try the other
     * page for widgets, and remove the widget only when both pages are full.
     */
    private void validateWorkspacePagesAfterIconResize() {
        validateWorkspacePagesAfterIconResize("icon-size change");
    }

    private void validateWorkspacePagesAfterIconResize(String changeReason) {
        if (pagesStrip == null) {
            homeIconResizeRepairPending = false;
            return;
        }
        resizeRepairCollisionCount = 0;
        resizeRepairClockBackgroundCollisionCount = 0;
        resizeRepairSpecialShrinkCount = 0;
        resizeRepairNeighbourIconShrinkCount = 0;
        resizeRepairMovedCount = 0;
        resizeRepairRemovedWidgetCount = 0;
        resizeRepairRemovedClockCount = 0;
        resizeRepairUnresolvedCollisionCount = 0;
        resizeRepairGroupRepositionCount = 0;
        pagesStrip.post(() -> {
            if (pagesStrip == null || isFinishing()) {
                homeIconResizeRepairPending = false;
                return;
            }
            final java.util.ArrayList<FrameLayout> targetPages =
                    new java.util.ArrayList<FrameLayout>();
            for (int i = 0; i < Math.min(2, pagesStrip.getChildCount()); i++) {
                View child = pagesStrip.getChildAt(i);
                if (child instanceof FrameLayout) targetPages.add((FrameLayout) child);
            }
            if (targetPages.isEmpty()) {
                homeIconResizeRepairPending = false;
                return;
            }
            final int[] remaining = {targetPages.size()};
            for (FrameLayout page : targetPages) {
                page.post(() -> {
                    if (pagesStrip == null || page.getParent() != pagesStrip) {
                        if (--remaining[0] == 0) homeIconResizeRepairPending = false;
                        return;
                    }
                    boolean collisionBeforeRepair = hasWorkspaceVisualOverlaps(page);
                    int collisionsBeforePage = resizeRepairCollisionCount;
                    snapAppGroupsToGridAfterIconResize(page);
                    repairResizeCollisionsOnPage(page);
                    resolveAllWorkspaceOverlaps(page, null);
                    repairResizeCollisionsOnPage(page);
                    if (collisionBeforeRepair
                            && resizeRepairCollisionCount == collisionsBeforePage) {
                        resizeRepairCollisionCount++;
                    }
                    if (--remaining[0] == 0) {
                        homeIconResizeRepairPending = false;
                        ensureShortcutLabelsNotClipped();
                        refreshWorkspaceGrid();
                        showIconResizeRecoveryPrompt(changeReason);
                    }
                });
            }
        });
    }


    private void showIconResizeRecoveryPrompt(String changeReason) {
        if (resizeRepairCollisionCount == 0 && resizeRepairGroupRepositionCount == 0
                && resizeRepairUnresolvedCollisionCount == 0) return;
        StringBuilder message = new StringBuilder();
        if (resizeRepairClockBackgroundCollisionCount > 0) {
            message.append("Clock background collision detected after ")
                    .append(changeReason).append(". ");
        } else if (resizeRepairCollisionCount > 0) {
            message.append("Workspace collision detected after ")
                    .append(changeReason).append(". ");
        }
        if (resizeRepairSpecialShrinkCount > 0) {
            message.append("Shrunk ").append(resizeRepairSpecialShrinkCount)
                    .append(" clock/widget item(s). ");
        }
        if (resizeRepairNeighbourIconShrinkCount > 0) {
            message.append("Shrunk ").append(resizeRepairNeighbourIconShrinkCount)
                    .append(" nearby app/group/shortcut icon(s). ");
        }
        if (resizeRepairMovedCount > 0) {
            message.append("Moved ").append(resizeRepairMovedCount)
                    .append(" item(s) to clear space. ");
        }
        if (resizeRepairGroupRepositionCount > 0) {
            message.append("Snapped ").append(resizeRepairGroupRepositionCount)
                    .append(" app group(s) back to the grid. ");
        }
        if (resizeRepairRemovedWidgetCount > 0) {
            message.append("Removed ").append(resizeRepairRemovedWidgetCount)
                    .append(" widget(s) from Home because both screens had no safe space. ");
        }
        if (resizeRepairRemovedClockCount > 0) {
            message.append("A clock was restored after a previous layout overflow. ");
        }
        if (resizeRepairUnresolvedCollisionCount > 0) {
            message.append("The clock stayed visible, but no fully collision-free placement was available. ")
                    .append("Move or resize another item to clear the overlap.");
        }
        Toast.makeText(this, message.toString().trim(), Toast.LENGTH_LONG).show();
    }

    private void snapAppGroupsToGridAfterIconResize(FrameLayout page) {
        if (page == null || page.getWidth() <= 0 || page.getHeight() <= 0) return;
        int pageIndex = Math.max(0, pagesStrip.indexOfChild(page));
        int columns = Math.max(1, getHomeColumns());
        int cellWidth = Math.max(1, page.getWidth() / columns);
        int iconSize = Math.max(32, Math.min(74, prefs.getInt(KEY_ICON_SIZE, 48)));
        int cellHeight = LauncherUiUtils.dp(this, homeCellHeightDp(iconSize));
        int rows = homeGridRowsForHeight(page.getHeight(), cellHeight);
        boolean[][] occupied = new boolean[rows][columns];

        // App icons reserve their logical slots first; groups must use another legal slot.
        for (int i = 0; i < page.getChildCount(); i++) {
            View child = page.getChildAt(i);
            if (!isWorkspaceAppIcon(child)
                    || !(child.getLayoutParams() instanceof FrameLayout.LayoutParams)) continue;
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) child.getLayoutParams();
            int col = Math.max(0, Math.min(columns - 1,
                    Math.round(lp.leftMargin / (float) cellWidth)));
            int row = homeGridRowFromTop(lp.topMargin, rows, page.getHeight(), cellHeight);
            occupied[row][col] = true;
        }

        java.util.ArrayList<View> groups = new java.util.ArrayList<View>();
        for (int i = 0; i < page.getChildCount(); i++) {
            View child = page.getChildAt(i);
            if (isAppGroupItem(child)
                    && child.getLayoutParams() instanceof FrameLayout.LayoutParams) groups.add(child);
        }
        for (View group : groups) {
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) group.getLayoutParams();
            int oldCol = Math.max(0, Math.min(columns - 1,
                    Math.round(lp.leftMargin / (float) cellWidth)));
            int oldRow = homeGridRowFromTop(lp.topMargin, rows, page.getHeight(), cellHeight);
            int col = oldCol;
            int row = oldRow;
            if (occupied[row][col]) {
                int bestDistance = Integer.MAX_VALUE;
                int bestCol = -1, bestRow = -1;
                for (int r = 0; r < rows; r++) {
                    for (int c = 0; c < columns; c++) {
                        if (occupied[r][c]) continue;
                        int distance = Math.abs(c - oldCol) + Math.abs(r - oldRow);
                        if (distance < bestDistance) {
                            bestDistance = distance;
                            bestCol = c;
                            bestRow = r;
                        }
                    }
                }
                if (bestCol < 0) {
                    // A full page may still have a free grid slot on the other screen.
                    int otherIndex = pageIndex == 0 ? 1 : 0;
                    if (otherIndex >= 0 && otherIndex < pagesStrip.getChildCount()
                            && pagesStrip.getChildAt(otherIndex) instanceof FrameLayout) {
                        FrameLayout other = (FrameLayout) pagesStrip.getChildAt(otherIndex);
                        int[] free = findFreeAppGroupSlot(other, null);
                        if (free != null) {
                            if (group.getParent() instanceof ViewGroup) {
                                ((ViewGroup) group.getParent()).removeView(group);
                            }
                            int otherColumns = Math.max(1, getHomeColumns());
                            int otherWidth = Math.max(1, other.getWidth() / otherColumns);
                            int otherCellHeight = LauncherUiUtils.dp(this, homeCellHeightDp(iconSize));
                            int otherRows = homeGridRowsForHeight(other.getHeight(), otherCellHeight);
                            FrameLayout.LayoutParams moved = new FrameLayout.LayoutParams(
                                    otherWidth, otherCellHeight, Gravity.TOP | Gravity.LEFT);
                            moved.leftMargin = free[0] * otherWidth;
                            moved.topMargin = homeGridRowTop(free[1], otherRows,
                                    other.getHeight(), otherCellHeight);
                            moved.rightMargin = 0;
                            moved.bottomMargin = 0;
                            group.setTranslationX(0f);
                            group.setTranslationY(0f);
                            other.addView(group, moved);
                            resizeRepairGroupRepositionCount++;
                            String groupId = appGroupId(group);
                            if (groupId != null) {
                                prefs.edit().putInt(appGroupKey("page", groupId), otherIndex)
                                        .putFloat(appGroupKey("x", groupId),
                                                free[0] / (float) Math.max(1, otherColumns - 1))
                                        .putFloat(appGroupKey("y", groupId),
                                                free[1] / (float) Math.max(1, otherRows - 1))
                                        .apply();
                            }
                            continue;
                        }
                    }
                    // If both grids are full, snap to the nearest valid slot rather than
                    // allowing the group to drift into an arbitrary pixel position.
                    bestCol = oldCol;
                    bestRow = oldRow;
                }
                col = bestCol;
                row = bestRow;
            }
            occupied[row][col] = true;
            boolean groupMoved = oldCol != col || oldRow != row
                    || group.getTranslationX() != 0f || group.getTranslationY() != 0f;
            lp.gravity = Gravity.TOP | Gravity.LEFT;
            lp.width = cellWidth;
            lp.height = cellHeight;
            lp.leftMargin = col * cellWidth;
            lp.topMargin = homeGridRowTop(row, rows, page.getHeight(), cellHeight);
            lp.rightMargin = 0;
            lp.bottomMargin = 0;
            group.setTranslationX(0f);
            group.setTranslationY(0f);
            group.setLayoutParams(lp);
            if (groupMoved) resizeRepairGroupRepositionCount++;
            String groupId = appGroupId(group);
            if (groupId != null) {
                prefs.edit().putInt(appGroupKey("page", groupId), pageIndex)
                        .putFloat(appGroupKey("x", groupId),
                                col / (float) Math.max(1, columns - 1))
                        .putFloat(appGroupKey("y", groupId),
                                row / (float) Math.max(1, rows - 1))
                        .apply();
            }
        }
    }

    private boolean resizeItemFitsAtCurrentPosition(FrameLayout page, View moving) {
        if (page == null || moving == null || moving.getParent() != page) return false;
        int[] geometry = itemGeometry(moving);
        if (geometry[2] <= 0 || geometry[3] <= 0 || geometry[0] < 0 || geometry[1] < 0
                || geometry[0] + geometry[2] > page.getWidth()
                || geometry[1] + geometry[3] > page.getHeight()) return false;
        for (int i = 0; i < page.getChildCount(); i++) {
            View other = page.getChildAt(i);
            if (other == moving || !isWorkspaceItem(other)
                    || other.getVisibility() != View.VISIBLE) continue;
            int[] otherGeometry = itemGeometry(other);
            if (otherGeometry[2] <= 0 || otherGeometry[3] <= 0) continue;
            android.graphics.Rect candidate =
                    collisionRectForInteraction(moving, geometry, other);
            android.graphics.Rect obstacle =
                    collisionRectForInteraction(other, otherGeometry, moving);
            if (android.graphics.Rect.intersects(candidate, obstacle)) return false;
        }
        return true;
    }

    private boolean resizeCandidateIsFree(FrameLayout page, View moving, int x, int y,
                                           int width, int height) {
        if (page == null || moving == null || width <= 0 || height <= 0
                || x < 0 || y < 0 || x + width > page.getWidth()
                || y + height > page.getHeight()) return false;
        for (int i = 0; i < page.getChildCount(); i++) {
            View other = page.getChildAt(i);
            if (other == moving || !isWorkspaceItem(other)
                    || other.getVisibility() != View.VISIBLE) continue;
            int[] otherGeometry = itemGeometry(other);
            if (otherGeometry[2] <= 0 || otherGeometry[3] <= 0) continue;
            android.graphics.Rect candidate =
                    collisionRectAtForInteraction(moving, x, y, width, height, other);
            android.graphics.Rect obstacle =
                    collisionRectForInteraction(other, otherGeometry, moving);
            if (android.graphics.Rect.intersects(candidate, obstacle)) return false;
        }
        return true;
    }

    private int[] findResizeFreeGridPosition(FrameLayout page, View moving) {
        if (page == null || moving == null || page.getWidth() <= 0 || page.getHeight() <= 0) return null;
        int[] g = itemGeometry(moving);
        int width = Math.max(1, g[2]), height = Math.max(1, g[3]);
        int maxX = Math.max(0, page.getWidth() - width);
        int maxY = Math.max(0, page.getHeight() - height);
        int columns = Math.max(1, getHomeColumns());
        int cellWidth = Math.max(1, page.getWidth() / columns);
        int cellHeight = LauncherUiUtils.dp(this,
                homeCellHeightDp(Math.max(32, Math.min(74, prefs.getInt(KEY_ICON_SIZE, 48)))));
        int rows = homeGridRowsForHeight(page.getHeight(), cellHeight);
        int bestX = -1, bestY = -1;
        long bestDistance = Long.MAX_VALUE;
        for (int row = 0; row < rows; row++) {
            int y = homeGridRowTop(row, rows, page.getHeight(), cellHeight);
            if (y > maxY) continue;
            for (int col = 0; col < columns; col++) {
                int x = Math.min(maxX, col * cellWidth);
                if (!resizeCandidateIsFree(page, moving, x, y, width, height)) continue;
                long distance = Math.abs((long) x - g[0]) + Math.abs((long) y - g[1]);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestX = x;
                    bestY = y;
                }
            }
        }
        return bestX < 0 ? null : new int[]{bestX, bestY};
    }

    private int[] findResizeFreePixelPosition(FrameLayout page, View moving) {
        if (page == null || moving == null || page.getWidth() <= 0 || page.getHeight() <= 0) return null;
        int[] g = itemGeometry(moving);
        int width = Math.max(1, g[2]), height = Math.max(1, g[3]);
        int maxX = Math.max(0, page.getWidth() - width);
        int maxY = Math.max(0, page.getHeight() - height);
        long bestDistance = Long.MAX_VALUE;
        int bestX = -1, bestY = -1;

        // Exact obstacle-edge candidates catch free rectangles between grid slots even
        // when no sample from the old coarse 8dp scan happened to land in that gap.
        java.util.TreeSet<Integer> xs = new java.util.TreeSet<Integer>();
        java.util.TreeSet<Integer> ys = new java.util.TreeSet<Integer>();
        xs.add(0);
        xs.add(maxX);
        xs.add(Math.max(0, Math.min(maxX, g[0])));
        ys.add(0);
        ys.add(maxY);
        ys.add(Math.max(0, Math.min(maxY, g[1])));
        int clearance = Math.max(1, workspaceCollisionMargin(moving)
                + LauncherUiUtils.dp(this, 1));
        for (int i = 0; i < page.getChildCount(); i++) {
            View other = page.getChildAt(i);
            if (other == moving || !isWorkspaceItem(other)
                    || other.getVisibility() != View.VISIBLE) continue;
            int[] otherGeometry = itemGeometry(other);
            if (otherGeometry[2] <= 0 || otherGeometry[3] <= 0) continue;
            android.graphics.Rect obstacle =
                    collisionRectForInteraction(other, otherGeometry, moving);
            xs.add(Math.max(0, Math.min(maxX, obstacle.left - width - clearance)));
            xs.add(Math.max(0, Math.min(maxX, obstacle.right + clearance)));
            ys.add(Math.max(0, Math.min(maxY, obstacle.top - height - clearance)));
            ys.add(Math.max(0, Math.min(maxY, obstacle.bottom + clearance)));
        }
        for (Integer yValue : ys) {
            int cy = yValue.intValue();
            for (Integer xValue : xs) {
                int cx = xValue.intValue();
                if (!resizeCandidateIsFree(page, moving, cx, cy, width, height)) continue;
                long distance = Math.abs((long) cx - g[0]) + Math.abs((long) cy - g[1]);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestX = cx;
                    bestY = cy;
                }
            }
        }
        if (bestX >= 0) return new int[]{bestX, bestY};

        // Irregular clock backgrounds can differ slightly from their outer rectangle.
        // Keep a finer pixel scan as a fallback if no exact edge candidate works.
        int step = Math.max(1, LauncherUiUtils.dp(this, 6));
        for (int y = 0; y <= maxY; y += step) {
            int cy = Math.min(maxY, y);
            for (int x = 0; x <= maxX; x += step) {
                int cx = Math.min(maxX, x);
                if (!resizeCandidateIsFree(page, moving, cx, cy, width, height)) continue;
                long distance = Math.abs((long) cx - g[0]) + Math.abs((long) cy - g[1]);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestX = cx;
                    bestY = cy;
                }
            }
            if (cy == maxY) break;
        }
        int[][] edges = {{maxX, Math.max(0, Math.min(maxY, g[1]))},
                {Math.max(0, Math.min(maxX, g[0])), maxY}, {maxX, maxY}, {0, 0}};
        for (int[] edge : edges) {
            if (!resizeCandidateIsFree(page, moving, edge[0], edge[1], width, height)) continue;
            long distance = Math.abs((long) edge[0] - g[0]) + Math.abs((long) edge[1] - g[1]);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestX = edge[0];
                bestY = edge[1];
            }
        }
        return bestX < 0 ? null : new int[]{bestX, bestY};
    }

    private void placeResizeRecoveredItem(FrameLayout page, View item, int pageIndex, int x, int y) {
        int[] g = itemGeometry(item);
        if (g[0] != x || g[1] != y) resizeRepairMovedCount++;
        if (isClockWorkspaceItem(item)) {
            setClockVisualPosition(item, x, y, true);
            return;
        }
        if (!(item.getLayoutParams() instanceof FrameLayout.LayoutParams)) return;
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) item.getLayoutParams();
        lp.gravity = Gravity.TOP | Gravity.LEFT;
        lp.leftMargin = x;
        lp.topMargin = y;
        lp.rightMargin = 0;
        lp.bottomMargin = 0;
        item.setLayoutParams(lp);
        if (item.getTag() instanceof String
                && ((String) item.getTag()).startsWith("launcher_widget:")) {
            try {
                int id = Integer.parseInt(((String) item.getTag()).substring("launcher_widget:".length()));
                saveWidgetGeometry(id, pageIndex, (FrameLayout) item);
            } catch (NumberFormatException ignored) {}
        }
        persistWorkspaceItemPosition(page, item, x, y, g[2], g[3]);
    }

    private boolean tryShrinkResizeItemInPlace(FrameLayout page, View item) {
        float oldX = item.getScaleX();
        float oldY = item.getScaleY();
        String keyX = workspaceAutoShrinkScaleKey(item, true);
        String keyY = workspaceAutoShrinkScaleKey(item, false);
        float priorX = clampWorkspaceAutoScale(getFloatPreferenceCompat(keyX, 1f));
        float priorY = clampWorkspaceAutoScale(getFloatPreferenceCompat(keyY, 1f));
        for (float factor = 0.95f; factor >= 0.50f; factor -= 0.05f) {
            if (priorX * factor < 0.25f || priorY * factor < 0.25f) continue;
            item.setScaleX(oldX * factor);
            item.setScaleY(oldY * factor);
            if (resizeItemFitsAtCurrentPosition(page, item)) {
                prefs.edit().putFloat(keyX, clampWorkspaceAutoScale(priorX * factor))
                        .putFloat(keyY, clampWorkspaceAutoScale(priorY * factor)).apply();
                return true;
            }
        }
        item.setScaleX(oldX);
        item.setScaleY(oldY);
        return false;
    }

    private void moveResizeWidgetToOtherPage(FrameLayout sourcePage, View widget,
                                              int targetPageIndex, int x, int y) {
        if (sourcePage == null || !(widget instanceof FrameLayout)
                || targetPageIndex < 0 || targetPageIndex >= pagesStrip.getChildCount()
                || !(pagesStrip.getChildAt(targetPageIndex) instanceof FrameLayout)) return;
        FrameLayout targetPage = (FrameLayout) pagesStrip.getChildAt(targetPageIndex);
        if (widget.getParent() instanceof ViewGroup) ((ViewGroup) widget.getParent()).removeView(widget);
        FrameLayout.LayoutParams lp = widget.getLayoutParams() instanceof FrameLayout.LayoutParams
                ? (FrameLayout.LayoutParams) widget.getLayoutParams()
                : new FrameLayout.LayoutParams(widget.getWidth(), widget.getHeight());
        lp.gravity = Gravity.TOP | Gravity.LEFT;
        lp.leftMargin = x;
        lp.topMargin = y;
        targetPage.addView(widget, lp);
        int id;
        try {
            id = Integer.parseInt(String.valueOf(widget.getTag()).substring("launcher_widget:".length()));
        } catch (RuntimeException ignored) {
            return;
        }
        widgetInteractionController.updateWidgetPageIndex((FrameLayout) widget, targetPageIndex);
        saveWidgetGeometry(id, targetPageIndex, (FrameLayout) widget);
    }

    private void removeResizeOverflowWidget(FrameLayout page, View widget) {
        int id;
        try {
            id = Integer.parseInt(String.valueOf(widget.getTag()).substring("launcher_widget:".length()));
        } catch (RuntimeException ignored) {
            return;
        }
        if (widget.getParent() instanceof ViewGroup) ((ViewGroup) widget.getParent()).removeView(widget);
        widgetInteractionController.forgetWidgetContainer((FrameLayout) widget);
        WidgetController.removeWidget(prefs, KEY_WIDGET_IDS, id);
        try { widgetHost.deleteAppWidgetId(id); } catch (Exception ignored) {}
        resizeRepairRemovedWidgetCount++;
    }

    /**
     * Last-resort clock recovery. A collision repair may fail to find an empty rectangle
     * when the workspace is saturated, but hiding/detaching the clock made a temporary
     * icon or label resize look like the clock had been deleted. Keep it attached and
     * visible, try the smallest permitted automatic scale, and clearly report any remaining
     * overlap rather than persisting a hidden-clock flag.
     */
    private void preserveResizeOverflowClock(FrameLayout page, View clock) {
        if (clock == null) return;
        int pageIndex = pagesStrip == null ? -1 : pagesStrip.indexOfChild(page);
        clock.setVisibility(View.VISIBLE);
        clock.setAlpha(1f);
        if (pageIndex < 0 || page == null || page.getWidth() <= 0 || page.getHeight() <= 0) {
            resizeRepairUnresolvedCollisionCount++;
            return;
        }
        if (clock.getParent() != page) {
            if (clock.getParent() instanceof ViewGroup) {
                ((ViewGroup) clock.getParent()).removeView(clock);
            }
            page.addView(clock, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.TOP | Gravity.LEFT));
        }

        String autoXKey = workspaceAutoShrinkScaleKey(clock, true);
        String autoYKey = workspaceAutoShrinkScaleKey(clock, false);
        float originalScaleX = Math.max(0.01f, clock.getScaleX());
        float originalScaleY = Math.max(0.01f, clock.getScaleY());
        float savedAutoX = clampWorkspaceAutoScale(getFloatPreferenceCompat(autoXKey, 1f));
        float savedAutoY = clampWorkspaceAutoScale(getFloatPreferenceCompat(autoYKey, 1f));
        float baseScaleX = originalScaleX / savedAutoX;
        float baseScaleY = originalScaleY / savedAutoY;
        boolean scaleChanged = false;

        // tryShrinkResizeItemInPlace stops at 50%; this final pass may go to the
        // launcher's 25% automatic-shrink floor before conceding that space is full.
        for (int percent = 45; percent >= 25; percent -= 5) {
            float targetAutoX = Math.max(0.25f, savedAutoX * percent / 100f);
            float targetAutoY = Math.max(0.25f, savedAutoY * percent / 100f);
            clock.setScaleX(baseScaleX * targetAutoX);
            clock.setScaleY(baseScaleY * targetAutoY);
            scaleChanged = Math.abs(clock.getScaleX() - originalScaleX) > 0.001f
                    || Math.abs(clock.getScaleY() - originalScaleY) > 0.001f;
            if (resizeItemFitsAtCurrentPosition(page, clock)) {
                prefs.edit().putFloat(autoXKey, targetAutoX)
                        .putFloat(autoYKey, targetAutoY).apply();
                if (scaleChanged) resizeRepairSpecialShrinkCount++;
                return;
            }
        }

        // Preserve the smallest attempted visible clock even if the page is genuinely
        // saturated. Clamp it to the screen bounds, persist the reduced scale, and expose
        // the unresolved background collision in the resize prompt.
        float minAutoX = 0.25f;
        float minAutoY = 0.25f;
        clock.setScaleX(baseScaleX * minAutoX);
        clock.setScaleY(baseScaleY * minAutoY);
        prefs.edit().putFloat(autoXKey, minAutoX).putFloat(autoYKey, minAutoY).apply();
        int[] geometry = itemGeometry(clock);
        int maxX = Math.max(0, page.getWidth() - geometry[2]);
        int maxY = Math.max(0, page.getHeight() - geometry[3]);
        int safeX = Math.max(0, Math.min(maxX, geometry[0]));
        int safeY = Math.max(0, Math.min(maxY, geometry[1]));
        if (safeX != geometry[0] || safeY != geometry[1]) {
            setClockVisualPosition(clock, safeX, safeY, true);
        }
        clock.setVisibility(View.VISIBLE);
        clock.setAlpha(1f);
        if (scaleChanged) resizeRepairSpecialShrinkCount++;
        resizeRepairUnresolvedCollisionCount++;
    }


    private boolean hasClockBackgroundOverlapForResize(FrameLayout page, View item) {
        if (page == null || item == null || item.getParent() != page) return false;
        int[] itemGeometry = itemGeometry(item);
        if (itemGeometry[2] <= 0 || itemGeometry[3] <= 0) return false;
        for (int i = 0; i < page.getChildCount(); i++) {
            View other = page.getChildAt(i);
            if (other == item || !isClockWorkspaceItem(other)
                    || other.getVisibility() != View.VISIBLE) continue;
            int[] clockGeometry = itemGeometry(other);
            if (clockGeometry[2] <= 0 || clockGeometry[3] <= 0) continue;
            android.graphics.Rect itemBounds =
                    collisionRectForInteraction(item, itemGeometry, other);
            android.graphics.Rect clockBounds =
                    collisionRectForInteraction(other, clockGeometry, item);
            if (android.graphics.Rect.intersects(itemBounds, clockBounds)) return true;
        }
        if (isClockWorkspaceItem(item)) {
            android.graphics.Rect clockBounds = collisionRectForInteraction(
                    item, itemGeometry, item);
            for (int i = 0; i < page.getChildCount(); i++) {
                View other = page.getChildAt(i);
                if (other == item || !isWorkspaceItem(other)
                        || other.getVisibility() != View.VISIBLE) continue;
                int[] otherGeometry = itemGeometry(other);
                if (otherGeometry[2] <= 0 || otherGeometry[3] <= 0) continue;
                android.graphics.Rect otherBounds =
                        collisionRectForInteraction(other, otherGeometry, item);
                if (android.graphics.Rect.intersects(clockBounds, otherBounds)) return true;
            }
        }
        return false;
    }

    private int[] shrinkGridIconsToCreateResizeSpace(FrameLayout searchPage, View protectedItem) {
        if (searchPage == null || protectedItem == null || searchPage.getWidth() <= 0
                || searchPage.getHeight() <= 0) return null;
        final boolean itemAlreadyOnPage = protectedItem.getParent() == searchPage;
        if (itemAlreadyOnPage && resizeItemFitsAtCurrentPosition(searchPage, protectedItem)) {
            int[] g = itemGeometry(protectedItem);
            return new int[]{g[0], g[1]};
        }
        int[] currentGeometry = itemGeometry(protectedItem);
        int[] existingSlot = findResizeFreeGridPosition(searchPage, protectedItem);
        if (existingSlot != null) return existingSlot;

        java.util.ArrayList<View> candidates = new java.util.ArrayList<View>();
        for (int i = 0; i < searchPage.getChildCount(); i++) {
            View candidate = searchPage.getChildAt(i);
            if (candidate == protectedItem || !isHomeGridIcon(candidate)
                    || !isWorkspaceItem(candidate)
                    || candidate.getVisibility() != View.VISIBLE) continue;
            candidates.add(candidate);
        }
        java.util.Collections.sort(candidates, (a, b) -> {
            int[] ga = itemGeometry(a);
            int[] gb = itemGeometry(b);
            long da = Math.abs((long) (ga[0] + ga[2] / 2)
                    - (currentGeometry[0] + currentGeometry[2] / 2))
                    + Math.abs((long) (ga[1] + ga[3] / 2)
                    - (currentGeometry[1] + currentGeometry[3] / 2));
            long db = Math.abs((long) (gb[0] + gb[2] / 2)
                    - (currentGeometry[0] + currentGeometry[2] / 2))
                    + Math.abs((long) (gb[1] + gb[3] / 2)
                    - (currentGeometry[1] + currentGeometry[3] / 2));
            return Long.compare(da, db);
        });

        // Keep a transaction of provisional scale changes. Commit only if they actually
        // give the clock/widget a collision-free current position or a valid grid slot.
        java.util.IdentityHashMap<View, float[]> provisional =
                new java.util.IdentityHashMap<View, float[]>();
        for (View candidate : candidates) {
            float originalX = candidate.getScaleX();
            float originalY = candidate.getScaleY();
            String autoXKey = workspaceAutoShrinkScaleKey(candidate, true);
            String autoYKey = workspaceAutoShrinkScaleKey(candidate, false);
            float storedAutoX = clampWorkspaceAutoScale(
                    getFloatPreferenceCompat(autoXKey, 1f));
            float storedAutoY = clampWorkspaceAutoScale(
                    getFloatPreferenceCompat(autoYKey, 1f));
            float selectedFactor = 1f;

            for (int percent = 95; percent >= 50; percent -= 5) {
                float factor = percent / 100f;
                if (storedAutoX * factor < 0.25f || storedAutoY * factor < 0.25f) continue;
                candidate.setScaleX(originalX * factor);
                candidate.setScaleY(originalY * factor);
                selectedFactor = factor;

                int[] candidateSlot;
                if (itemAlreadyOnPage && resizeItemFitsAtCurrentPosition(searchPage, protectedItem)) {
                    int[] g = itemGeometry(protectedItem);
                    candidateSlot = new int[]{g[0], g[1]};
                } else {
                    candidateSlot = findResizeFreeGridPosition(searchPage, protectedItem);
                }
                if (candidateSlot != null) {
                    provisional.put(candidate, new float[]{originalX, originalY,
                            storedAutoX, storedAutoY, selectedFactor});
                    SharedPreferences.Editor edit = prefs.edit();
                    for (java.util.Map.Entry<View, float[]> entry : provisional.entrySet()) {
                        View changed = entry.getKey();
                        float[] values = entry.getValue();
                        edit.putFloat(workspaceAutoShrinkScaleKey(changed, true),
                                        clampWorkspaceAutoScale(values[2] * values[4]))
                                .putFloat(workspaceAutoShrinkScaleKey(changed, false),
                                        clampWorkspaceAutoScale(values[3] * values[4]));
                    }
                    edit.putFloat(autoXKey, clampWorkspaceAutoScale(storedAutoX * selectedFactor))
                            .putFloat(autoYKey, clampWorkspaceAutoScale(storedAutoY * selectedFactor));
                    edit.apply();
                    resizeRepairNeighbourIconShrinkCount += provisional.size();
                    return candidateSlot;
                }
            }

            // This item alone was not enough; keep it provisionally at its minimum so
            // the next neighbour can also yield space. The preference is not written yet.
            if (selectedFactor < 1f) {
                provisional.put(candidate, new float[]{originalX, originalY,
                        storedAutoX, storedAutoY, selectedFactor});
            }
        }

        // No valid placement resulted from the shrink attempt. Roll all visual changes back.
        for (java.util.Map.Entry<View, float[]> entry : provisional.entrySet()) {
            View candidate = entry.getKey();
            float[] values = entry.getValue();
            candidate.setScaleX(values[0]);
            candidate.setScaleY(values[1]);
        }
        return null;
    }

    private void repairResizeCollisionsOnPage(FrameLayout page) {
        if (page == null || page.getWidth() <= 0 || page.getHeight() <= 0) return;
        java.util.ArrayList<View> resizeItems = new java.util.ArrayList<View>();
        for (int i = 0; i < page.getChildCount(); i++) {
            View child = page.getChildAt(i);
            if (!isWorkspaceItem(child) || child.getVisibility() != View.VISIBLE) continue;
            if (isClockWorkspaceItem(child)
                    || (child.getTag() instanceof String
                    && ((String) child.getTag()).startsWith("launcher_widget:"))) {
                resizeItems.add(child);
            }
        }
        // Keep the page's own clock first. Other widgets then route around it.
        java.util.Collections.sort(resizeItems, (a, b) ->
                Boolean.compare(isClockWorkspaceItem(b), isClockWorkspaceItem(a)));
        for (View item : resizeItems) {
            if (item.getParent() != page || item.getVisibility() != View.VISIBLE
                    || resizeItemFitsAtCurrentPosition(page, item)) continue;
            resizeRepairCollisionCount++;
            if (hasClockBackgroundOverlapForResize(page, item)) {
                resizeRepairClockBackgroundCollisionCount++;
            }

            // Priority 1: shrink the clock/widget itself before changing its location.
            if (tryShrinkResizeItemInPlace(page, item)) {
                resizeRepairSpecialShrinkCount++;
                continue;
            }

            // Priority 2: use a collision-free grid slot on this screen.
            int[] local = findResizeFreeGridPosition(page, item);
            int pageIndex = Math.max(0, pagesStrip.indexOfChild(page));
            if (local != null) {
                placeResizeRecoveredItem(page, item, pageIndex, local[0], local[1]);
                continue;
            }

            // Additional icon fallback: ordinary apps/groups/shortcuts may shrink to free
            // a slot; the conflicting clock/widget itself and every other clock/widget are
            // excluded from this neighbour-shrink step.
            int[] afterNeighbourShrink = shrinkGridIconsToCreateResizeSpace(page, item);
            if (afterNeighbourShrink != null) {
                int[] before = itemGeometry(item);
                if (before[0] != afterNeighbourShrink[0] || before[1] != afterNeighbourShrink[1]) {
                    placeResizeRecoveredItem(page, item, pageIndex,
                            afterNeighbourShrink[0], afterNeighbourShrink[1]);
                }
                continue;
            }

            // A page may contain safe space between grid slots. Test pixel positions on
            // the current page before moving a widget away or deciding to delete it.
            int[] localPixelPosition = findResizeFreePixelPosition(page, item);
            if (localPixelPosition != null) {
                placeResizeRecoveredItem(page, item, pageIndex,
                        localPixelPosition[0], localPixelPosition[1]);
                continue;
            }

            boolean widget = item.getTag() instanceof String
                    && ((String) item.getTag()).startsWith("launcher_widget:");
            if (widget) {
                // Priority 3: try the other screen. Give it the same ordinary-icon
                // shrinking fallback before declaring that neither page has room.
                int otherIndex = pageIndex == 0 ? 1 : 0;
                if (otherIndex >= 0 && otherIndex < pagesStrip.getChildCount()
                        && pagesStrip.getChildAt(otherIndex) instanceof FrameLayout) {
                    FrameLayout other = (FrameLayout) pagesStrip.getChildAt(otherIndex);
                    int[] otherPosition = findResizeFreeGridPosition(other, item);
                    if (otherPosition == null) {
                        otherPosition = shrinkGridIconsToCreateResizeSpace(other, item);
                    }
                    if (otherPosition == null) otherPosition = findResizeFreePixelPosition(other, item);
                    if (otherPosition != null) {
                        moveResizeWidgetToOtherPage(page, item, otherIndex,
                                otherPosition[0], otherPosition[1]);
                        resizeRepairMovedCount++;
                        continue;
                    }
                }
                // Last safety check before destructive removal: re-evaluate both pages
                // after neighbour-shrink attempts. Grid-only failure is not proof that
                // there is no usable rectangular gap.
                int[] finalLocalPosition = findResizeFreePixelPosition(page, item);
                if (finalLocalPosition != null) {
                    placeResizeRecoveredItem(page, item, pageIndex,
                            finalLocalPosition[0], finalLocalPosition[1]);
                    continue;
                }
                if (otherIndex >= 0 && otherIndex < pagesStrip.getChildCount()
                        && pagesStrip.getChildAt(otherIndex) instanceof FrameLayout) {
                    FrameLayout other = (FrameLayout) pagesStrip.getChildAt(otherIndex);
                    int[] finalOtherPosition = findResizeFreePixelPosition(other, item);
                    if (finalOtherPosition != null) {
                        moveResizeWidgetToOtherPage(page, item, otherIndex,
                                finalOtherPosition[0], finalOtherPosition[1]);
                        resizeRepairMovedCount++;
                        continue;
                    }
                }
                removeResizeOverflowWidget(page, item);
            } else {
                // Clock panels are page-specific. Keep their independent state on this screen:
                // try any safe pixel position, then hide this page's clock as a final resort.
                int[] freePixels = findResizeFreePixelPosition(page, item);
                if (freePixels != null) {
                    placeResizeRecoveredItem(page, item, pageIndex, freePixels[0], freePixels[1]);
                } else {
                    preserveResizeOverflowClock(page, item);
                }
            }
        }
    }

    private void validateWorkspacePagesAfterLayout() {
        if (pagesStrip == null || homeIconResizeRepairPending) return;
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
                new int[]{studioTint(accent, 128), studioTint(Color.rgb(25, 31, 50), 128)},
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

    private void addSideBySideChoicePair(LinearLayout panel,
            String leftTitle, int leftSelected, String[] leftChoices, IntAction leftAction,
            String rightTitle, int rightSelected, String[] rightChoices, IntAction rightAction) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.TOP);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2);
        rowParams.bottomMargin = LauncherUiUtils.dp(this, 7);
        panel.addView(row, rowParams);
        addCompactChoiceCard(row, leftTitle, leftSelected, leftChoices, leftAction, true);
        addCompactChoiceCard(row, rightTitle, rightSelected, rightChoices, rightAction, false);
    }

    private void addCompactChoiceCard(LinearLayout row, String title, int selected,
            String[] choices, IntAction action, boolean leftCard) {
        LinearLayout block = new LinearLayout(this);
        block.setOrientation(LinearLayout.VERTICAL);
        block.setPadding(LauncherUiUtils.dp(this, 6), LauncherUiUtils.dp(this, 7),
                LauncherUiUtils.dp(this, 6), LauncherUiUtils.dp(this, 6));
        block.setBackground(studioControlBackground());
        addStudioAccentLine(block);
        LinearLayout.LayoutParams blockParams =
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        if (leftCard) blockParams.rightMargin = LauncherUiUtils.dp(this, 3);
        else blockParams.leftMargin = LauncherUiUtils.dp(this, 3);
        row.addView(block, blockParams);

        TextView heading = LauncherUiUtils.text(this, title, 12.5f,
                Color.rgb(247, 249, 255), true);
        heading.setMaxLines(2);
        heading.setEllipsize(android.text.TextUtils.TruncateAt.END);
        block.addView(heading, new LinearLayout.LayoutParams(-1, -2));

        android.widget.HorizontalScrollView horizontal = new android.widget.HorizontalScrollView(this);
        horizontal.setHorizontalScrollBarEnabled(false);
        horizontal.setFillViewport(false);
        LinearLayout chips = new LinearLayout(this);
        chips.setGravity(Gravity.CENTER_VERTICAL);
        for (int i = 0; i < choices.length; i++) {
            final int value = i;
            boolean isSelected = i == selected;
            TextView chip = LauncherUiUtils.text(this, choices[i], 10.5f,
                    isSelected ? Color.rgb(10, 15, 27) : Color.rgb(205, 214, 233), isSelected);
            chip.setGravity(Gravity.CENTER);
            chip.setSingleLine(true);
            chip.setPadding(LauncherUiUtils.dp(this, 5), LauncherUiUtils.dp(this, 8),
                    LauncherUiUtils.dp(this, 5), LauncherUiUtils.dp(this, 8));
            chip.setBackground(LauncherUiUtils.roundRect(this,
                    isSelected
                            ? new int[]{homeStudioAccentColor, studioDarken(homeStudioAccentColor)}
                            : new int[]{Color.rgb(39, 47, 68), Color.rgb(29, 36, 54)},
                    LauncherUiUtils.dp(this, 8)));
            LinearLayout.LayoutParams chipParams = new LinearLayout.LayoutParams(-2,
                    LauncherUiUtils.dp(this, 34));
            chipParams.setMargins(LauncherUiUtils.dp(this, 1), 0, LauncherUiUtils.dp(this, 3), 0);
            chips.addView(chip, chipParams);
            chip.setOnClickListener(v -> action.onSelect(value));
        }
        horizontal.addView(chips, new android.widget.HorizontalScrollView.LayoutParams(-2, -2));
        block.addView(horizontal, new LinearLayout.LayoutParams(-1, -2));
    }

    private void showCustomizationMenu() {
        boolean restoreClock = false;
        for (int page = 0; page < clockPanelsByPage.length; page++) {
            restoreClock |= prefs.getBoolean(KEY_AUTO_REMOVED_CLOCK_PAGE_PREFIX + page, false);
        }
        if (restoreClock) {
            SharedPreferences.Editor edit = prefs.edit();
            for (int page = 0; page < clockPanelsByPage.length; page++) {
                edit.remove(KEY_AUTO_REMOVED_CLOCK_PAGE_PREFIX + page);
            }
            edit.apply();
            attachClockPanelsToPages();
        }
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
            TextView title = LauncherUiUtils.text(this, "Home Studio", 41,
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
            LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(-2, -2);
            titleParams.gravity = Gravity.CENTER_HORIZONTAL;
            panel.addView(title, titleParams);
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
            int iconSize = prefs.getInt(KEY_ICON_SIZE, 48);
            int newColumns = value + 3;
            remapHomeIconSlotsForColumnChange(oldColumns, newColumns, iconSize, iconSize);
            prefs.edit().putInt(KEY_COLUMNS, newColumns).apply();
            currentPage = Math.max(0, Math.min(1, currentPage));
            rebuildPages();
            renderHomePages(false);
            showCustomizationMenu(dialog, scroll.getScrollY(), false);
        });
        addSliderRowOnStop(content, "Home icon size", 32, 74,
                Math.max(32, Math.min(74, prefs.getInt(KEY_ICON_SIZE, 48))), " dp", newSize -> {
            int oldIconSize = Math.max(32, Math.min(74, prefs.getInt(KEY_ICON_SIZE, 48)));
            if (newSize == oldIconSize) return;
            clearAutoShrunkHomeGridItems();
            int oldColumns = getHomeColumns();
            boolean reduceColumns = newSize > 64 && oldColumns > 5;
            int newColumns = reduceColumns ? 5 : oldColumns;
            homeIconResizeRepairPending = true;
            remapHomeIconSlotsForColumnChange(oldColumns, newColumns, oldIconSize, newSize);
            SharedPreferences.Editor edit = prefs.edit().putInt(KEY_ICON_SIZE, newSize);
            if (reduceColumns) edit.putInt(KEY_COLUMNS, newColumns);
            edit.apply();
            currentPage = Math.max(0, Math.min(1, currentPage));
            rebuildPages();
            renderHomePages(false);
            renderDock();
            rebuildContactShortcutViews();
            ensureShortcutLabelsNotClipped();
            validateWorkspacePagesAfterIconResize();
        });
        addSettingRow(content, "▣", "App icon background",
                prefs.getBoolean("icon_rounded_square_background", false) ? "On" : "Off", () -> {
            prefs.edit().putBoolean("icon_rounded_square_background",
                    !prefs.getBoolean("icon_rounded_square_background", false)).apply();
            refreshWorkspaceIconAppearance();
            showCustomizationMenu(dialog, scroll.getScrollY(), false);
        });
        addSettingRow(content, "◫", "Fit icons inside background",
                prefs.getBoolean(KEY_ICON_FIT_BACKGROUND, false) ? "On" : "Off", () -> {
            prefs.edit().putBoolean(KEY_ICON_FIT_BACKGROUND,
                    !prefs.getBoolean(KEY_ICON_FIT_BACKGROUND, false)).apply();
            refreshWorkspaceIconAppearance();
            showCustomizationMenu(dialog, scroll.getScrollY(), false);
        });
        boolean nothingMonochrome = prefs.getInt(KEY_ICON_ARTWORK_THEME,
                LauncherIconDrawable.ARTWORK_ORIGINAL)
                == LauncherIconDrawable.ARTWORK_NOTHING_MONOCHROME;
        if (nothingMonochrome
                && (prefs.getInt(KEY_ICON_BACKGROUND_COLOR_POSITION, -1) != 0
                        || prefs.getInt(KEY_ICON_BACKGROUND_COLOR, -1)
                                != LauncherIconDrawable.BACKGROUND_WHITE)) {
            prefs.edit().putInt(KEY_ICON_BACKGROUND_COLOR_POSITION, 0)
                    .putInt(KEY_ICON_BACKGROUND_COLOR, LauncherIconDrawable.BACKGROUND_WHITE)
                    .apply();
        }
        addColorSliderRow(content, "App icon background colour",
                nothingMonochrome ? 0
                        : LauncherIconDrawable.backgroundPositionFromPreferences(prefs),
                !nothingMonochrome, value -> {
            // The background switch alone controls whether shapes/backgrounds are active.
            // Changing the selected colour must not silently turn the feature back on.
            prefs.edit().putInt(KEY_ICON_BACKGROUND_COLOR_POSITION, value).apply();
            refreshWorkspaceIconAppearance();
        });
        addChoiceRow(content, "App background shape",
                LauncherIconDrawable.clampShape(prefs.getInt("app_icon_shape",
                        LauncherIconDrawable.SHAPE_SQUARE)),
                new String[]{"Square", "Round", "Hexagon", "Pentagon", "Tilted Pear"}, value -> {
            prefs.edit().putInt("app_icon_shape",
                    LauncherIconDrawable.clampShape(value)).apply();
            refreshWorkspaceIconAppearance();
            showCustomizationMenu(dialog, scroll.getScrollY(), false);
        });
        addChoiceRow(content, "Icon artwork theme",
                LauncherIconDrawable.clampArtworkTheme(prefs.getInt(KEY_ICON_ARTWORK_THEME,
                        LauncherIconDrawable.ARTWORK_ORIGINAL)),
                new String[]{"Original Colours", "Spectrum Mix", "Ocean Gradient", "Sunset Gradient", "Nothing Monochrome"}, value -> {
            int selectedTheme = LauncherIconDrawable.clampArtworkTheme(value);
            SharedPreferences.Editor themeEdit = prefs.edit()
                    .putInt(KEY_ICON_ARTWORK_THEME, selectedTheme);
            if (selectedTheme == LauncherIconDrawable.ARTWORK_NOTHING_MONOCHROME) {
                themeEdit.putInt(KEY_ICON_BACKGROUND_COLOR_POSITION, 0)
                        .putInt(KEY_ICON_BACKGROUND_COLOR, LauncherIconDrawable.BACKGROUND_WHITE);
            }
            themeEdit.apply();
            LauncherIconDrawable.configurePersistentArtworkCache(this, selectedTheme);
            refreshWorkspaceIconAppearance();
            refreshClockArtworkAppearance();
            showCustomizationMenu(dialog, scroll.getScrollY(), false);
        });
        addSettingRow(content, "◈", "Theme shortcut icons",
                prefs.getBoolean(KEY_THEME_SHORTCUT_ARTWORK, false) ? "On" : "Off", () -> {
            prefs.edit().putBoolean(KEY_THEME_SHORTCUT_ARTWORK,
                    !prefs.getBoolean(KEY_THEME_SHORTCUT_ARTWORK, false)).apply();
            refreshWorkspaceIconAppearance();
            showCustomizationMenu(dialog, scroll.getScrollY(), false);
        });
        addSliderRowOnStop(content, "Home app font size", 8, 15,
                homeFontSizeSp(), " sp", value -> {
            int iconSize = Math.max(32, Math.min(74, prefs.getInt(KEY_ICON_SIZE, 48)));
            int columns = getHomeColumns();
            int oldCellHeight = homeCellHeightDp(iconSize);
            if (value == Math.round(homeFontSizeSp())) return;
            homeIconResizeRepairPending = true;
            prefs.edit().putInt(KEY_HOME_FONT_SIZE, value).apply();
            int newCellHeight = homeCellHeightDp(iconSize);
            remapHomeIconSlotsForLayoutChange(columns, columns, iconSize, iconSize,
                    oldCellHeight, newCellHeight);
            renderHomePages(false);
            rebuildContactShortcutViews();
            ensureShortcutLabelsNotClipped();
            validateWorkspacePagesAfterIconResize("label-size change");
        });
        addChoiceRow(content, "App and shortcut label font",
                Math.max(0, Math.min(3, prefs.getInt(KEY_HOME_LABEL_FONT, 0))),
                new String[]{"Sans Serif", "Serif", "Monospace", "Cursive"}, value -> {
            int iconSize = Math.max(32, Math.min(74, prefs.getInt(KEY_ICON_SIZE, 48)));
            int columns = getHomeColumns();
            int oldCellHeight = homeCellHeightDp(iconSize);
            if (value == prefs.getInt(KEY_HOME_LABEL_FONT, 0)) return;
            homeIconResizeRepairPending = true;
            prefs.edit().putInt(KEY_HOME_LABEL_FONT, value).apply();
            int newCellHeight = homeCellHeightDp(iconSize);
            remapHomeIconSlotsForLayoutChange(columns, columns, iconSize, iconSize,
                    oldCellHeight, newCellHeight);
            renderHomePages(false);
            rebuildContactShortcutViews();
            refreshWorkspaceLabelAppearance();
            validateWorkspacePagesAfterIconResize("label-font change");
            showCustomizationMenu(dialog, scroll.getScrollY(), false);
        });
        addColorSliderRow(content, "App and shortcut label colour",
                Math.max(0, Math.min(1000, prefs.getInt(KEY_HOME_LABEL_COLOR, 0))), true, value -> {
            prefs.edit().putInt(KEY_HOME_LABEL_COLOR, value).apply();
            refreshWorkspaceLabelAppearance();
        });
        int shortcutSizeMax = Math.max(24, Math.min(74, prefs.getInt(KEY_ICON_SIZE, 48)));
        addSliderRowOnStop(content, "Contact shortcut icon size", 24, shortcutSizeMax,
                Math.max(24, Math.min(shortcutSizeMax, getShortcutIconSizeDp())), " dp", value -> {
            prefs.edit().putInt(KEY_SHORTCUT_ICON_SIZE_DP, value).apply();
            applyShortcutIconSizeToWorkspace();
        });
        addSettingRow(content, "Aa", "App labels on Home", prefs.getBoolean(KEY_SHOW_HOME_LABELS, true) ? "Visible" : "Hidden", () -> {
            int iconSize = Math.max(32, Math.min(74, prefs.getInt(KEY_ICON_SIZE, 48)));
            int columns = getHomeColumns();
            int oldCellHeight = homeCellHeightDp(iconSize);
            homeIconResizeRepairPending = true;
            prefs.edit().putBoolean(KEY_SHOW_HOME_LABELS,
                    !prefs.getBoolean(KEY_SHOW_HOME_LABELS, true)).apply();
            int newCellHeight = homeCellHeightDp(iconSize);
            remapHomeIconSlotsForLayoutChange(columns, columns, iconSize, iconSize,
                    oldCellHeight, newCellHeight);
            renderHomePages(false);
            rebuildContactShortcutViews();
            validateWorkspacePagesAfterIconResize("label visibility change");
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
        addSettingRow(content, "▣", "Add an app group",
                "Create a round group icon for apps on this home screen", () -> {
            dialog.dismiss();
            createAppGroup();
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
        addSettingRow(content, "◈", "Match app icon artwork",
                prefs.getBoolean(KEY_CLOCK_ICON_ARTWORK, false) ? "On" : "Off", () -> {
            boolean enabled = !prefs.getBoolean(KEY_CLOCK_ICON_ARTWORK, false);
            SharedPreferences.Editor edit = prefs.edit().putBoolean(KEY_CLOCK_ICON_ARTWORK, enabled);
            if (enabled) {
                // Artwork mode and either gradient mode are mutually exclusive.
                edit.putBoolean(KEY_CLOCK_DIGIT_GRADIENT, false)
                        .putBoolean(KEY_CLOCK_LIVE_GRADIENT, false);
            }
            edit.apply();
            stopClockColorAnimator();
            clockColorPhase = 0f;
            refreshClockArtworkAppearance();
            showCustomizationMenu(dialog, scroll.getScrollY(), false);
        });
        addSettingRow(content, "✦", "Gradient clock digits",
                prefs.getBoolean(KEY_CLOCK_DIGIT_GRADIENT, false) ? "On" : "Off", () -> {
            boolean enabled = !prefs.getBoolean(KEY_CLOCK_DIGIT_GRADIENT, false);
            SharedPreferences.Editor edit = prefs.edit().putBoolean(KEY_CLOCK_DIGIT_GRADIENT, enabled);
            if (enabled) {
                edit.putBoolean(KEY_CLOCK_ICON_ARTWORK, false);
            } else {
                // A disabled digit gradient cannot continue running a live transition.
                edit.putBoolean(KEY_CLOCK_LIVE_GRADIENT, false);
            }
            edit.apply();
            if (!enabled || !prefs.getBoolean(KEY_CLOCK_LIVE_GRADIENT, false)) {
                stopClockColorAnimator();
                if (!enabled) clockColorPhase = 0f;
            }
            refreshClockArtworkAppearance();
            if (enabled && prefs.getBoolean(KEY_CLOCK_LIVE_GRADIENT, false)) {
                startClockColorAnimator();
            }
            showCustomizationMenu(dialog, scroll.getScrollY(), false);
        });
        addSettingRow(content, "↻", "Live clock colour transition",
                prefs.getBoolean(KEY_CLOCK_LIVE_GRADIENT, false) ? "Live" : "Fixed", () -> {
            boolean enabled = !prefs.getBoolean(KEY_CLOCK_LIVE_GRADIENT, false);
            SharedPreferences.Editor edit = prefs.edit().putBoolean(KEY_CLOCK_LIVE_GRADIENT, enabled);
            // Live animation uses the gradient mode and therefore switches artwork matching off.
            if (enabled) {
                edit.putBoolean(KEY_CLOCK_DIGIT_GRADIENT, true)
                        .putBoolean(KEY_CLOCK_ICON_ARTWORK, false);
            }
            edit.apply();
            if (!enabled) {
                stopClockColorAnimator();
                clockColorPhase = 0f;
            }
            refreshClockArtworkAppearance();
            if (enabled) startClockColorAnimator();
            showCustomizationMenu(dialog, scroll.getScrollY(), false);
        });

        addSection(content, "Clock text sizes");
        addSideBySideSliderPair(content,
                "Date size — Screen 1", 8, 16,
                Math.max(8, Math.min(16, prefs.getInt(clockPrefKey(KEY_CLOCK_DATE_SIZE, 0), 15))),
                " sp", value -> {
                    prefs.edit().putInt(clockPrefKey(KEY_CLOCK_DATE_SIZE, 0), value).apply();
                    refreshClockTypographyAndGeometry();
                },
                "Date size — Screen 2",
                Math.max(8, Math.min(16, prefs.getInt(clockPrefKey(KEY_CLOCK_DATE_SIZE, 1), 15))),
                value -> {
                    prefs.edit().putInt(clockPrefKey(KEY_CLOCK_DATE_SIZE, 1), value).apply();
                    refreshClockTypographyAndGeometry();
                });
        addSideBySideSliderPair(content,
                "AM/PM size — Screen 1", 8, 20,
                Math.max(8, Math.min(20, prefs.getInt(clockPrefKey(KEY_CLOCK_PM_SIZE, 0), 14))),
                " dp", value -> {
                    prefs.edit().putInt(clockPrefKey(KEY_CLOCK_PM_SIZE, 0), value).apply();
                    refreshClockTypographyAndGeometry();
                },
                "AM/PM size — Screen 2",
                Math.max(8, Math.min(20, prefs.getInt(clockPrefKey(KEY_CLOCK_PM_SIZE, 1), 14))),
                value -> {
                    prefs.edit().putInt(clockPrefKey(KEY_CLOCK_PM_SIZE, 1), value).apply();
                    refreshClockTypographyAndGeometry();
                });
        addSection(content, "Clock text fonts");
        addSideBySideChoicePair(content,
                "Date font",
                Math.max(0, Math.min(3, prefs.getInt(KEY_CLOCK_DATE_FONT, 0))),
                new String[]{"Cursive", "Sans", "Serif", "Monospace"}, value -> {
                    prefs.edit().putInt(KEY_CLOCK_DATE_FONT, value).apply();
                    refreshClockTypographyAndGeometry();
                    showCustomizationMenu(dialog, scroll.getScrollY(), false);
                },
                "AM/PM font",
                Math.max(0, Math.min(3, prefs.getInt(KEY_CLOCK_PM_FONT, 0))),
                new String[]{"Black Sans", "Sans", "Serif", "Monospace"}, value -> {
                    prefs.edit().putInt(KEY_CLOCK_PM_FONT, value).apply();
                    refreshClockTypographyAndGeometry();
                    showCustomizationMenu(dialog, scroll.getScrollY(), false);
                });
        addColorSliderRow(content, "Clock date inner colour",
                Math.max(0, Math.min(1000,
                        prefs.getInt(KEY_CLOCK_DATE_FILL_COLOR_POSITION, 0))),
                true, value -> {
                    prefs.edit().putInt(KEY_CLOCK_DATE_FILL_COLOR_POSITION, value).apply();
                    updateClock();
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
        addSliderRowWithHint(content, "Animation speed", 20, 300,
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
        addSliderRowOnStop(content, "App drawer font size", 8, 15,
                Math.max(8, Math.min(15, prefs.getInt(KEY_DRAWER_FONT_SIZE, 9))), " sp", value -> {
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

        TextView authorCredit = LauncherUiUtils.text(this, "made by Debarghya Sannigrahi",
                24, Color.WHITE, false);
        authorCredit.setTypeface(android.graphics.Typeface.create("cursive",
                android.graphics.Typeface.NORMAL));
        authorCredit.setGravity(Gravity.CENTER);
        authorCredit.setPadding(0, LauncherUiUtils.dp(this, 16), 0, LauncherUiUtils.dp(this, 7));
        // Match Home Studio's cyan-to-lavender-to-pink-to-warm-orange gradient,
        // but span the text itself so the complete gradient remains visible.
        authorCredit.addOnLayoutChangeListener((view, left, top, right, bottom,
                oldLeft, oldTop, oldRight, oldBottom) -> {
            float textWidth = authorCredit.getPaint().measureText(
                    authorCredit.getText().toString());
            if (right <= left || textWidth <= 0f) return;
            float startX = Math.max(0f, ((right - left) - textWidth) * 0.5f);
            authorCredit.getPaint().setShader(new android.graphics.LinearGradient(
                    startX, 0f, startX + textWidth, 0f,
                    new int[] {Color.rgb(96, 231, 255), Color.rgb(143, 143, 255),
                            Color.rgb(255, 133, 204), Color.rgb(255, 208, 125)},
                    null, android.graphics.Shader.TileMode.CLAMP));
            authorCredit.invalidate();
        });
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

    /**
     * Computes a translation that centers actual native API-23 glyph ink inside the
     * existing clock group. This avoids changing background size or widget geometry.
     */
    private void updateClockDigitHorizontalOffset() {
        if (clockPanel == null || clockText == null) return;
        int style = prefs == null ? 0 : prefs.getInt(KEY_CLOCK_STYLE, 0);
        boolean sevenSegment = clockText instanceof SevenSegmentClockTextView && style == 5;
        float panelScaleX = Math.max(0.01f, Math.abs(clockPanel.getScaleX()));
        float targetX;
        if (sevenSegment) {
            // Preserve the already-tuned seven-segment positioning exactly.
            targetX = -LauncherUiUtils.dp(this, 15) / Math.max(1f, panelScaleX);
        } else {
            CharSequence clockValue = clockText.getText() == null ? "" : clockText.getText();
            String currentClock = clockValue.toString();
            int colon = currentClock.indexOf(':');
            int hourDigits = 0;
            if (colon > 0) {
                for (int i = 0; i < colon; i++) {
                    if (Character.digit(currentClock.charAt(i), 10) >= 0) hourDigits++;
                }
            }
            float textScaleX = Math.max(0.01f, Math.abs(clockText.getScaleX()));
            if (hourDigits == 1) {
                // The invisible leading figure-space equalizes 9:xx and 10:xx widths.
                // Shift by half that reserved cell so the *visible* one-digit clock is
                // balanced, leaving the colon slightly left of the background midpoint.
                float leadingCellWidth = clockText.getPaint().measureText("\u2007");
                float trackingWidth = clockText.getTextSize() * clockText.getLetterSpacing();
                float leadingCellAdvance = Math.max(0f, leadingCellWidth + trackingWidth);
                // TranslationX is in the clock text's parent coordinates. The parent
                // panel transforms both the background and this correction together, so
                // dividing by its scale would make date-size changes move the visible clock.
                targetX = -(leadingCellAdvance * 0.5f) * textScaleX;
            } else if (colon >= 0) {
                // Center the colon itself, not merely the entire string. This accounts for
                // the custom colon/digit spans and the actual advances of the displayed
                // glyphs, while the clock group width remains independent of date sizing.
                int groupWidth = 0;
                ViewGroup.LayoutParams groupParams = clockGroup == null
                        ? null : clockGroup.getLayoutParams();
                if (groupParams != null && groupParams.width > 0) {
                    groupWidth = groupParams.width;
                } else if (clockGroup != null) {
                    groupWidth = clockGroup.getWidth();
                }
                if (groupWidth > 0) {
                    android.text.TextPaint paint = clockText.getPaint();
                    float fullTextWidth = android.text.Layout.getDesiredWidth(
                            clockValue, paint);
                    float prefixWidth = android.text.Layout.getDesiredWidth(
                            clockValue, 0, colon, paint);
                    float colonAdvance = android.text.Layout.getDesiredWidth(
                            clockValue, colon, colon + 1, paint);
                    if (colonAdvance <= 0f) {
                        colonAdvance = Math.max(1f,
                                Math.round(paint.measureText(":") * 0.45f));
                    }
                    float colonCenter = (groupWidth - fullTextWidth) * 0.5f
                            + prefixWidth + colonAdvance * 0.5f;
                    // Center the colon in parent-local coordinates. Do not divide by
                    // panelScaleX: the background receives that same parent transform.
                    float measuredTargetX = (groupWidth * 0.5f - colonCenter) * textScaleX;
                    // API 23 can briefly report inconsistent desired widths for two-digit
                    // hours with ReplacementSpans immediately after package replacement.
                    // Bound this correction so bad transient metrics cannot push all digits
                    // outside their clock group.
                    if (Float.isNaN(measuredTargetX) || Float.isInfinite(measuredTargetX)) {
                        measuredTargetX = 0f;
                    }
                    float safeOffsetLimit = Math.min(groupWidth * 0.18f,
                            LauncherUiUtils.dp(this, 24));
                    targetX = Math.max(-safeOffsetLimit,
                            Math.min(safeOffsetLimit, measuredTargetX));
                } else {
                    targetX = 0f;
                }
            } else {
                targetX = 0f;
            }
        }
        // Avoid invalidating the view when the computed offset hasn't actually changed.
        if (Math.abs(clockText.getTranslationX() - targetX) > 0.5f) {
            clockText.setTranslationX(targetX);
        }
    }

    private int measureClockDateLineWidth(String text, int dateSizeSp, int pmSizeDp) {
        if (dateText == null || text == null || text.isEmpty()) return 1;
        float scaledDensity = Math.max(0.1f, getResources().getDisplayMetrics().scaledDensity);
        float density = Math.max(0.1f, getResources().getDisplayMetrics().density);
        android.graphics.Paint datePaint = new android.graphics.Paint(dateText.getPaint());
        datePaint.setTypeface(Typeface.create(clockDateFontFamily(), Typeface.BOLD));
        datePaint.setTextSize(dateSizeSp * scaledDensity);

        float width;
        if (!DateFormat.is24HourFormat(this)) {
            String period = new SimpleDateFormat("a", Locale.getDefault()).format(new Date());
            String prefix = period + ", ";
            if (text.startsWith(prefix)) {
                android.graphics.Paint pmPaint = new android.graphics.Paint(datePaint);
                pmPaint.setTypeface(Typeface.create(clockPmFontFamily(), Typeface.BOLD_ITALIC));
                pmPaint.setTextSize(pmSizeDp * density);
                width = pmPaint.measureText(period)
                        + datePaint.measureText(", ")
                        + datePaint.measureText(text.substring(prefix.length()));
            } else {
                width = datePaint.measureText(text);
            }
        } else {
            width = datePaint.measureText(text);
        }

        // Paint.measureText omits the em-based letter spacing set on the date TextView.
        float tracking = dateText.getLetterSpacing();
        int periodChars = 0;
        if (!DateFormat.is24HourFormat(this)) {
            String period = new SimpleDateFormat("a", Locale.getDefault()).format(new Date());
            if (text.startsWith(period + ", ")) periodChars = period.length();
        }
        int dateSizedChars = Math.max(0, text.length() - periodChars);
        width += tracking * dateSizeSp * scaledDensity * Math.max(0, dateSizedChars - 1);
        if (periodChars > 1) {
            width += tracking * pmSizeDp * density * (periodChars - 1);
        }
        return Math.max(1, (int) Math.ceil(width));
    }

    private float getClockColonCenterY() {
        // Center the custom colon on the visible digit ink, not the font's full line box.
        // This keeps its dots vertically aligned in Neon Circuit and every other standard
        // clock preset despite their different baselines, ascenders, and descenders.
        if (clockText != null) {
            android.text.Layout layout = clockText.getLayout();
            if (layout != null && layout.getLineCount() > 0) {
                Paint digitPaint = new Paint(clockText.getPaint());
                android.graphics.Rect digitBounds = new android.graphics.Rect();
                digitPaint.getTextBounds("8", 0, 1, digitBounds);
                if (digitBounds.bottom > digitBounds.top) {
                    return layout.getLineBaseline(0)
                            + (digitBounds.top + digitBounds.bottom) * 0.5f;
                }
                return (layout.getLineTop(0) + layout.getLineBottom(0)) * 0.5f;
            }

            Paint.FontMetricsInt metrics = new Paint.FontMetricsInt();
            clockText.getPaint().getFontMetricsInt(metrics);
            if (metrics.bottom > metrics.top) {
                return (metrics.bottom + metrics.top) * 0.5f;
            }
            int measuredHeight = clockText.getHeight();
            if (measuredHeight > 0) return measuredHeight * 0.5f;
            ViewGroup.LayoutParams params = clockText.getLayoutParams();
            if (params != null && params.height > 0) return params.height * 0.5f;
        }
        return LauncherUiUtils.dp(this, 114) / 2f;
    }

    private float clockPresetLetterSpacing(int style) {
        switch (style) {
            case 1: return 0.12f;
            case 2: return 0.035f;
            case 3: return 0.16f;
            case 4: return 0.015f;
            case 5: return 0f;
            default: return 0.04f;
        }
    }

    private void syncClockColonVerticalCenter() {
        if (clockText == null) return;
        CharSequence value = clockText.getText();
        if (!(value instanceof android.text.Spanned)) return;
        android.text.Spanned spanned = (android.text.Spanned) value;
        CenteredColonSpan[] spans = spanned.getSpans(
                0, spanned.length(), CenteredColonSpan.class);
        if (spans.length == 0) return;
        float centerY = getClockColonCenterY();
        for (CenteredColonSpan span : spans) {
            span.setRowCenterY(centerY);
        }
        clockText.invalidate();
    }

    private void applyClockPositionAndSize() {
        if (clockPanel == null || clockText == null) return;
        final LinearLayout targetPanel = clockPanel;
        final int targetPage = Math.max(0, Math.min(1, activeClockPage));
        final boolean preservePackageRefreshPosition =
                preserveClockPositionAfterPackageRefresh[targetPage];
        final float panelTranslationAtStartX = targetPanel.getTranslationX();
        final float panelTranslationAtStartY = targetPanel.getTranslationY();
        final float preservedPanelTranslationX = preservePackageRefreshPosition
                ? packageRefreshClockTranslationX[targetPage] : panelTranslationAtStartX;
        final float preservedPanelTranslationY = preservePackageRefreshPosition
                ? packageRefreshClockTranslationY[targetPage] : panelTranslationAtStartY;
        preserveClockPositionAfterPackageRefresh[targetPage] = false;
        // A live drag/resize owns the panel transform until UP/CANCEL. A normal
        // typography/layout pass here would reset its scale/pivot and make the clock
        // jump away from the anchored edge while the user is resizing it.
        if (clockWidgetDragging && clockGesturePanel == targetPanel
                && clockGesturePage == targetPage) return;
        final boolean resizeTransactionOwnsGeometry = clockWidgetEditing
                && clockWidgetDragging && clockResizeEdge != 0
                && clockPanel == targetPanel && activeClockPage == targetPage;
        final int geometryGeneration = ++clockLayoutGenerationByPage[targetPage];
        final int configuredDateSize = Math.max(8, Math.min(16,
                prefs.getInt(clockPrefKey(KEY_CLOCK_DATE_SIZE, targetPage), 15)));
        final int configuredPmSize = Math.max(8, Math.min(20,
                prefs.getInt(clockPrefKey(KEY_CLOCK_PM_SIZE, targetPage), 14)));
        final int selectedClockStyle = prefs == null ? 0 : prefs.getInt(KEY_CLOCK_STYLE, 0);
        Integer previousPositionStyle = clockText == null ? null
                : clockPositionStyleSignatures.get(clockText);
        final boolean clockStyleChanged = previousPositionStyle != null
                && previousPositionStyle.intValue() != selectedClockStyle;
        // Date geometry changes only when its own size/typeface changes. A clock-style
        // switch must not trigger the date-size anchor correction: Neon Circuit centers
        // its glyphs within the group and would otherwise inherit a stale horizontal shift.
        final String dateGeometrySignature = configuredDateSize + ":" + configuredPmSize + ":"
                + clockDateFontFamily() + ":" + clockPmFontFamily();
        final String previousDateGeometrySignature = dateText == null ? null
                : clockDateGeometrySignatures.get(dateText);
        final FrameLayout groupAtGeometryStart = clockGroupsByPage[targetPage] != null
                ? clockGroupsByPage[targetPage] : clockGroup;
        final int[] previousClockPanelGeometry = targetPanel.getWidth() > 0
                ? itemGeometry(targetPanel) : new int[]{0, 0, 0, 0};
        final boolean preservePanelPosition = preservePackageRefreshPosition
                || (clockStyleChanged && previousClockPanelGeometry[2] > 0);
        final boolean preserveTypographyPanelCenterX =
                !(clockText instanceof SevenSegmentClockTextView && selectedClockStyle == 5)
                && !clockStyleChanged
                && previousDateGeometrySignature != null
                && !previousDateGeometrySignature.equals(dateGeometrySignature)
                && targetPanel.getWidth() > 0;
        if (clockText != null) {
            clockPositionStyleSignatures.put(clockText, selectedClockStyle);
        }
        if (clockStyleChanged && groupAtGeometryStart != null) {
            // Clear any correction from the previous preset before laying out the new one.
            groupAtGeometryStart.setTranslationX(0f);
        }
        if (dateText != null) {
            clockDateGeometrySignatures.put(dateText, dateGeometrySignature);
        }
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
        float targetClockTextSize = sevenSegment ? 110f : 78f;
        if (Math.abs(clockText.getTextSize()
                - targetClockTextSize * getResources().getDisplayMetrics().scaledDensity) > 0.5f) {
            clockText.setTextSize(targetClockTextSize);
        }

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
        if (dateText != null) {
            dateText.setTypeface(Typeface.create(clockDateFontFamily(), Typeface.BOLD));
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
            measuredDateWidth = measureClockDateLineWidth(
                    measuredDate, configuredDateSize, configuredPmSize);
        }
        final int dateWidth = measuredDateWidth;
        // Measure the panel at its natural width, then scale the complete panel.
        // This keeps the background, digits, AM/PM marker, and date in one resize
        // operation instead of shrinking only the clock glyphs inside a fixed panel.
        final float nonLedDigitScale = 1.12f;
        String widthClockValue = clockText.getText() == null
                ? "" : clockText.getText().toString();
        // Keep this reservation stable while the hour changes digit count.
        final boolean reserveTwoDigitStandardWidth = !sevenSegment
                && selectedClockStyle != 2;
        float baseTrackingWidth = clockText.getTextSize()
                * clockPresetLetterSpacing(selectedClockStyle)
                * Math.max(0, widthClockValue.length() - 1);
        float customColonAdvanceReduction = clockText.getPaint().measureText(":") * 0.55f;
        float customDigitGroupReduction = LauncherUiUtils.dp(this,
                Build.VERSION.SDK_INT >= 36 ? -15f : 15f);
        // Paint.measureText excludes TextView letter spacing, and the standard renderer
        // enlarges glyphs horizontally by 1.12x. Reserve the net width after digit-gap
        // tightening and the compact centered-colon advance.
        float requiredExtraWidth = naturalClockWidth * (nonLedDigitScale - 1f)
                + nonLedDigitScale * baseTrackingWidth
                - nonLedDigitScale * customColonAdvanceReduction
                - customDigitGroupReduction - markerWidth - LauncherUiUtils.dp(this, 5f);
        final int twoDigitStandardWidthClearance = reserveTwoDigitStandardWidth
                ? Math.max(0, (int) Math.ceil(requiredExtraWidth))
                : 0;
        final int ledGroupSpacing = sevenSegment ? LauncherUiUtils.dp(this, 30) : 0;
        final int clockWithMarkerWidth = (int) Math.ceil(naturalClockWidth)
                + twoDigitStandardWidthClearance
                + markerWidth + LauncherUiUtils.dp(this, 1) + ledGroupSpacing;
        // Keep the digit group's layout width independent from the date-line width.
        // Enlarging date/AM-PM text may widen the background, but must not move the digits.
        final int clockGroupWidth = clockWithMarkerWidth + LauncherUiUtils.dp(this, 4);
        final int contentWidth = Math.max(clockWithMarkerWidth, dateWidth)
                + LauncherUiUtils.dp(this, 4);
        final int naturalPanelWidth = Math.max(LauncherUiUtils.dp(this, 96),
                contentWidth + LauncherUiUtils.dp(this, 8));
        // Treat the clock as a workspace widget: its visual rectangle may grow
        // across the full page, but never beyond the page's horizontal bounds.
        // The cap is based on the actual page width (not half-width/grid columns),
        // so normal typography clocks can expand all the way to either screen edge.
        // The cap must use the full background panel, not only the glyph group. Otherwise
        // the LED digit-group width can allow a scaled background to exceed the page, after
        // which drag/resize clamps and handle positions disagree at the screen edge.
        float widthForScaleCap = naturalPanelWidth;
        final float stylePreservedScale = clockStyleChanged
                && previousClockPanelGeometry[2] > 0
                ? previousClockPanelGeometry[2] / (float) Math.max(1, naturalPanelWidth)
                : requestedLengthScale;
        final float lengthScale = Math.min(stylePreservedScale,
                Math.max(0.01f, availablePanelWidth / (float) widthForScaleCap));
        if (clockStyleChanged && previousClockPanelGeometry[2] > 0 && prefs != null) {
            // A preset can have a different intrinsic width because of tracking,
            // marker clearance, or typography. Save the compensating scale so later
            // refreshes do not restore the old percentage and make the clock jump again.
            int stableLengthPercent = Math.max(20, Math.min(600,
                    Math.round(lengthScale * 100f)));
            prefs.edit().putInt(clockPrefKey(KEY_CLOCK_LENGTH, targetPage),
                    stableLengthPercent).apply();
        }
        // Neon Circuit's glyphs must keep the scale they would have without the date
        // controlling the panel's width cap. The full panel still stays inside the page;
        // only this style compensates the extra horizontal reduction caused by the date.
        final boolean neonCircuitStyle = !sevenSegment && selectedClockStyle == 2;
        final float neonCircuitScaleWithoutDateCap = neonCircuitStyle
                ? Math.min(requestedLengthScale, Math.max(0.01f,
                        availablePanelWidth / (float) Math.max(1, clockWithMarkerWidth)))
                : lengthScale;
        final float neonCircuitDateScaleCompensation = neonCircuitStyle
                ? Math.max(1f, neonCircuitScaleWithoutDateCap / Math.max(0.01f, lengthScale))
                : 1f;
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
        // The complete widget includes the date/AM-PM line below the clock row.
        // A fixed row-height panel clips that line on the first home screen and makes
        // vertical resizing calculate a zero-height date allowance. Let the panel wrap
        // its children; an active resize temporarily sets an explicit target height.
        panelParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
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
            // A geometry pass created during a resize remains owned by that resize
            // even if this callback runs after finger-up. Otherwise a late callback
            // can clamp or relocate the panel after the edge-anchor correction.
            if (resizeTransactionOwnsGeometry) {
                if (activeClockPage == targetPage && clockPanel == targetPanel) {
                    updateClockResizeHandles();
                }
                return;
            }
            // Also protect a normal geometry pass if a resize began before it executed.
            if (activeClockPage == targetPage && clockPanel == targetPanel
                    && clockWidgetEditing && clockWidgetDragging && clockResizeEdge != 0) {
                updateClockResizeHandles();
                return;
            }
            if (preservePanelPosition) {
                // A style or package refresh rebuilds content, not workspace placement.
                targetPanel.setTranslationX(preservedPanelTranslationX);
                targetPanel.setTranslationY(preservedPanelTranslationY);
            } else {
                float targetTranslationX = targetPanel.getTranslationX();
                float[] bounded = clampClockTranslation(targetPanel,
                        targetTranslationX, targetPanel.getTranslationY());
                setClampedClockTranslationForPanel(
                        targetPanel, targetPage, bounded[0], bounded[1], true);
            }

            // If date typography changes the panel width, keep its visual center fixed.
            // The clock group is centered inside that panel, so the colon stays centered
            // without using date size or font metrics to calculate its horizontal offset.
            if (!preservePanelPosition && preserveTypographyPanelCenterX
                    && targetPanel.getParent() != null) {
                int[] currentClockPanelGeometry = itemGeometry(targetPanel);
                float previousCenterX = previousClockPanelGeometry[0]
                        + previousClockPanelGeometry[2] / 2f;
                float currentCenterX = currentClockPanelGeometry[0]
                        + currentClockPanelGeometry[2] / 2f;
                float correction = previousCenterX - currentCenterX;
                if (Math.abs(correction) > 0.25f) {
                    setClampedClockTranslationForPanel(targetPanel, targetPage,
                            targetPanel.getTranslationX() + correction,
                            targetPanel.getTranslationY(), true);
                }
            }
            // Recalculate horizontal alignment after the new child width and Layout have
            // been measured. The earlier synchronous pass can still see pre-update metrics.
            int restoreGeometryPage = Math.max(0, Math.min(1, activeClockPage));
            if (clockPanelsByPage[targetPage] == targetPanel
                    && clockTextsByPage[targetPage] != null) {
                activateClockForPage(targetPage);
                updateClockDigitHorizontalOffset();
                syncClockColonVerticalCenter();
                
                if (clockText != null) clockText.invalidate();
                activateClockForPage(restoreGeometryPage);
            }
            boolean activeClockResize = activeClockPage == targetPage
                    && clockPanel == targetPanel && clockWidgetDragging
                    && clockWidgetEditing && clockResizeEdge != 0;
            boolean anotherDragOwnsPage = pendingLiveDisplacementPage == targetPanel.getParent()
                    && pendingLiveDisplacementMover != null;
            if (!activeClockResize && !anotherDragOwnsPage && !preservePanelPosition) {
                // Collision relocation is reserved for ordinary layout changes, not a
                // style switch or package refresh that must preserve the chosen position.
                moveClockToNearestFreePosition(targetPanel);
            }
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
            groupParams.gravity = (sevenSegment ? alignment : Gravity.CENTER_HORIZONTAL)
                    | Gravity.TOP;
            groupParams.width = clockGroupWidth;
            groupParams.height = rowHeight;
            clockGroup.setLayoutParams(groupParams);
            // The group is centered within the background for typography clocks. Keep
            // seven-segment positioning on its established alignment path; clear any
            // stale typography offset left by earlier date-size compensation.
            clockGroup.setTranslationX(0f);
            clockGroup.setClipChildren(false);
            clockGroup.setClipToPadding(false);
        }

        // Neon Circuit needs a little extra clearance only once the hour is two digits.
        // Count the hour digits in the live value so single-digit hours keep their exact
        // existing size and alignment.
        String clockValueForWidthAdjustment = clockText.getText() == null
                ? "" : clockText.getText().toString();
        int clockColonForWidthAdjustment = clockValueForWidthAdjustment.indexOf(':');
        int hourDigitCountForWidthAdjustment = 0;
        if (clockColonForWidthAdjustment > 0) {
            for (int i = 0; i < clockColonForWidthAdjustment; i++) {
                if (Character.digit(clockValueForWidthAdjustment.charAt(i), 10) >= 0) {
                    hourDigitCountForWidthAdjustment++;
                }
            }
        }
        final boolean neonCircuitTwoDigitHour = !sevenSegment
                && prefs.getInt(KEY_CLOCK_STYLE, 0) == 2
                && hourDigitCountForWidthAdjustment > 1;

        // Center typography-clock text inside its fixed-width group; the group itself is
        // centered inside the background. The seven-segment renderer retains its left anchor.
        clockText.setGravity((sevenSegment ? Gravity.LEFT : Gravity.CENTER_HORIZONTAL)
                | (sevenSegment ? Gravity.TOP : Gravity.CENTER_VERTICAL));
        // Use geometry derived from the current layout calculation, not getWidth()/getHeight()
        // from the previous frame. Those dimensions can be stale while the user resizes the
        // panel, causing scaled digits to jump or drift until the next layout pass.
        clockText.setPivotX(clockGroupWidth / 2f);
        // The LED renderer sizes its segments from its measured view height.
        // Keep that view at the natural 114dp height and scale the rendered digits
        // by breadthScale explicitly; otherwise layout/background breadth changes
        // while the LED glyph view can retain stale measured bounds during a drag.
        clockText.setPivotY(sevenSegment ? 0f : rowHeight / 2f);
        // The typography colon must use the same vertical center as this transformed row.
        syncClockColonVerticalCenter();
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
        // When Neon Circuit shows a two-digit hour, reduce the total clock-glyph
        // width by 20dp to leave clearance inside the unchanged background. Derive
        // the factor from the stable widest-digit measurement so the clock won't
        // wobble as individual minute digits change.
        float neonCircuitWidthScale = 1f;
        if (neonCircuitTwoDigitHour) {
            float currentGlyphWidth = Math.max(1f, naturalClockWidth * nonLedDigitScale);
            neonCircuitWidthScale = Math.max(0.01f,
                    (currentGlyphWidth - LauncherUiUtils.dp(this, 20)) / currentGlyphWidth);
        }
        clockText.setScaleX(sevenSegment ? sevenSegmentWidthScale
                : nonLedDigitScale * neonCircuitWidthScale * neonCircuitDateScaleCompensation);
        // Keep the pivot tied to the newly calculated layout dimensions even during live resizing.
        clockText.setPivotX(clockGroupWidth / 2f);
        clockText.setPivotY(sevenSegment ? 0f : rowHeight / 2f);
        // Extend non-seven-segment digits another 10 dp downward while keeping
        // their upper edge anchored to its existing position against the background.
        // The added scale is based on the fixed 114 dp clock row; a 5 dp translation
        // compensates for the centered pivot so the extra height goes downward.
        clockText.setScaleY(sevenSegment ? breadthScale
                : normalizedNonLedHeightScale * nonLedDigitScale
                        * (1.20f + 20f / 114f) * breadthScale);
        // Keep the horizontal glyph anchor synchronized with the current hour shape
        // without recalculating or changing the background geometry.
        updateClockDigitHorizontalOffset();
        // Keep the non-seven-segment upper edge fixed while the added glyph height
        // extends downward; preserve a net 5 dp upward offset from the original center.
        // The panel itself no longer uses scaleY, so this offset must remain a
        // fixed physical distance rather than being inversely scaled with breadth.
        clockText.setTranslationY(sevenSegment ? 0f : LauncherUiUtils.dp(this, 5));
        ViewGroup.LayoutParams rawClockParams = clockText.getLayoutParams();
        if (rawClockParams instanceof FrameLayout.LayoutParams) {
            FrameLayout.LayoutParams textParams = (FrameLayout.LayoutParams) rawClockParams;
            // Tie every clock style to the same fixed group width. WRAP_CONTENT lets
            // proportional digits remeasure the child as the time changes, which can
            // make the glyphs drift horizontally while the background remains fixed.
            textParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
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
            dateText.setTextSize(Math.max(8, Math.min(16,
                    prefs.getInt(clockPrefKey(KEY_CLOCK_DATE_SIZE, activeClockPage), 15))));
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
            amPmText.setTypeface(Typeface.create(clockPmFontFamily(), Typeface.BOLD));
            // Keep the marker's font size independent of workspace resizing.
            // The configured AM/PM size is measured in dp.
            amPmText.setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP, configuredPmSize);
            ViewGroup.LayoutParams markerRaw = amPmText.getLayoutParams();
            if (markerRaw instanceof FrameLayout.LayoutParams) {
                FrameLayout.LayoutParams markerParams = (FrameLayout.LayoutParams) markerRaw;
                // Reserve enough width for the enlarged bold "AM"/"PM" label;
                // its configured dp size is independent of the date's sp size.
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
        if (!sevenSegment) {
            final TextView colonSyncTarget = clockText;
            targetPanel.postOnAnimation(() -> {
                if (targetPanel.getParent() == null
                        || geometryGeneration != clockLayoutGenerationByPage[targetPage]
                        || targetPanel != clockPanelsByPage[targetPage]
                        || colonSyncTarget != clockTextsByPage[targetPage]) return;
                colonSyncTarget.postOnAnimation(() -> {
                    if (targetPanel.getParent() == null
                            || geometryGeneration != clockLayoutGenerationByPage[targetPage]
                            || targetPanel != clockPanelsByPage[targetPage]
                            || colonSyncTarget != clockTextsByPage[targetPage]) return;
                    int restorePage = Math.max(0, Math.min(1, activeClockPage));
                    activateClockForPage(targetPage);
                    syncClockColonVerticalCenter();
                    
                    updateClockDigitHorizontalOffset();
                    activateClockForPage(restorePage);
                    // API 23's native baseline span may request another layout; settle it
                    // once more after that layout, rather than leaving the post-resize shift.
                    colonSyncTarget.postOnAnimation(() -> {
                        if (targetPanel.getParent() == null
                                || geometryGeneration != clockLayoutGenerationByPage[targetPage]
                                || targetPanel != clockPanelsByPage[targetPage]
                                || colonSyncTarget != clockTextsByPage[targetPage]) return;
                        int finalRestorePage = Math.max(0, Math.min(1, activeClockPage));
                        activateClockForPage(targetPage);
                        syncClockColonVerticalCenter();
                        
                        updateClockDigitHorizontalOffset();
                        activateClockForPage(finalRestorePage);
                    });
                });
            });
        }
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

    private static void drawClockGlyphInnerWhiteBorder(Canvas canvas, CharSequence text,
            int start, int end, float x, int top, int y, int bottom, Paint sourcePaint,
            float innerWhiteBorderPx) {
        if (innerWhiteBorderPx <= 0f) return;
        // Use the glyph's actual outline as a clip. This avoids Android 6 losing the
        // inward white contour during saveLayer/DST_IN compositing after a package update.
        android.graphics.Path glyphPath = new android.graphics.Path();
        Paint pathPaint = new Paint(sourcePaint);
        pathPaint.setStyle(Paint.Style.FILL);
        pathPaint.setShader(null);
        pathPaint.setXfermode(null);
        pathPaint.clearShadowLayer();
        pathPaint.getTextPath(text.toString(), start, end, x, y, glyphPath);
        if (glyphPath.isEmpty()) return;

        Paint contour = new Paint(sourcePaint);
        contour.setShader(null);
        contour.setXfermode(null);
        contour.setColor(Color.WHITE);
        contour.setStyle(Paint.Style.STROKE);
        contour.setStrokeWidth(innerWhiteBorderPx * 2f);
        contour.setStrokeJoin(Paint.Join.ROUND);
        contour.setStrokeCap(Paint.Cap.ROUND);
        contour.clearShadowLayer();
        int save = canvas.save();
        try {
            canvas.clipPath(glyphPath);
            canvas.drawText(text, start, end, x, y, contour);
        } finally {
            canvas.restoreToCount(save);
        }
    }

    private static final class TightDigitSpan extends android.text.style.ReplacementSpan {
        private final boolean moveDigitRight;
        private final float gap;
        private final float glyphOffsetX;
        private final boolean drawWhiteInnerBorder;
        private final float innerWhiteBorderPx;

        TightDigitSpan(boolean moveDigitRight, float gap, float glyphOffsetX,
                boolean drawWhiteInnerBorder, float innerWhiteBorderPx) {
            this.moveDigitRight = moveDigitRight;
            this.gap = Math.max(0f, gap);
            this.glyphOffsetX = glyphOffsetX;
            this.drawWhiteInnerBorder = drawWhiteInnerBorder;
            this.innerWhiteBorderPx = Math.max(0f, innerWhiteBorderPx);
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
            float glyphX = x + (moveDigitRight ? gap : 0f) + glyphOffsetX;
            canvas.drawText(text, start, end, glyphX, y, paint);
            if (drawWhiteInnerBorder && innerWhiteBorderPx > 0f) {
                drawClockGlyphInnerWhiteBorder(canvas, text, start, end,
                        glyphX, top, y, bottom, paint, innerWhiteBorderPx);
            }
        }
    }

    private static final class CenteredColonSpan extends android.text.style.ReplacementSpan {
        private float rowCenterY;
        private final boolean drawWhiteInnerBorder;
        private final float innerWhiteBorderPx;

        CenteredColonSpan(float rowCenterY, boolean drawWhiteInnerBorder,
                float innerWhiteBorderPx) {
            this.rowCenterY = rowCenterY;
            this.drawWhiteInnerBorder = drawWhiteInnerBorder;
            this.innerWhiteBorderPx = Math.max(0f, innerWhiteBorderPx);
        }

        void setRowCenterY(float rowCenterY) {
            this.rowCenterY = Math.max(0f, rowCenterY);
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
            float naturalWidth = paint.measureText(":");
            float radius = Math.max(1f, paint.getTextSize() * 0.045f);
            // Match the horizontal center of the colon's compact reserved advance.
            // This keeps a balanced two-digit time centered in its background.
            float colonAdvance = Math.max(1f, Math.round(naturalWidth * 0.45f));
            float centerX = x + colonAdvance * 0.5f;
            // Recompute the center from the live digit baseline. After a resize a cached
            // rowCenterY could remain inside the line box but no longer match the digit ink.
            android.graphics.Rect digitInk = new android.graphics.Rect();
            paint.getTextBounds("8", 0, 1, digitInk);
            float centerY = digitInk.bottom > digitInk.top
                    ? y + (digitInk.top + digitInk.bottom) * 0.5f
                    : (Math.min(top, bottom) + Math.max(top, bottom)) * 0.5f;
            float gap = paint.getTextSize() * 0.18f;

            Paint dots = new Paint(paint);
            dots.setXfermode(null);
            dots.setStyle(Paint.Style.FILL);
            dots.setStrokeWidth(0f);
            canvas.drawCircle(centerX, centerY - gap, radius, dots);
            canvas.drawCircle(centerX, centerY + gap, radius, dots);

            if (!drawWhiteInnerBorder || innerWhiteBorderPx <= 0f) return;
            // Clip the white stroke to the two filled dots directly; avoid DST_IN layers
            // which are unreliable on Android 6's native-text fallback.
            android.graphics.Path dotMask = new android.graphics.Path();
            dotMask.addCircle(centerX, centerY - gap, radius,
                    android.graphics.Path.Direction.CW);
            dotMask.addCircle(centerX, centerY + gap, radius,
                    android.graphics.Path.Direction.CW);
            Paint contour = new Paint(paint);
            contour.setShader(null);
            contour.setXfermode(null);
            contour.setColor(Color.WHITE);
            contour.setStyle(Paint.Style.STROKE);
            contour.setStrokeWidth(innerWhiteBorderPx * 2f);
            contour.setStrokeJoin(Paint.Join.ROUND);
            contour.setStrokeCap(Paint.Cap.ROUND);
            contour.clearShadowLayer();
            int save = canvas.save();
            try {
                canvas.clipPath(dotMask);
                canvas.drawCircle(centerX, centerY - gap, radius, contour);
                canvas.drawCircle(centerX, centerY + gap, radius, contour);
            } finally {
                canvas.restoreToCount(save);
            }
        }
    }

    private interface IntSliderAction { void onChange(int value); }

    private void addColorSliderRow(LinearLayout panel, String title, int current,
            boolean enabled, IntSliderAction action) {
        LinearLayout block = new LinearLayout(this);
        block.setOrientation(LinearLayout.VERTICAL);
        block.setPadding(LauncherUiUtils.dp(this, 10), LauncherUiUtils.dp(this, 8),
                LauncherUiUtils.dp(this, 10), LauncherUiUtils.dp(this, 8));
        block.setBackground(studioControlBackground());
        addStudioAccentLine(block);
        LinearLayout.LayoutParams blockParams = new LinearLayout.LayoutParams(-1, -2);
        blockParams.bottomMargin = LauncherUiUtils.dp(this, 7);

        TextView label = LauncherUiUtils.text(this, title, 15, Color.WHITE, true);
        TextView valueLabel = LauncherUiUtils.text(this,
                colorValueLabel(LauncherIconDrawable.spectrumColorForPosition(current)),
                13, homeStudioAccentColor, true);
        LinearLayout heading = new LinearLayout(this);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        heading.addView(label, new LinearLayout.LayoutParams(0, -2, 1f));
        heading.addView(valueLabel);
        block.addView(heading);

        int[] spectrumColors = LauncherIconDrawable.spectrumTrackColors();
        FrameLayout sliderFrame = new FrameLayout(this);
        sliderFrame.setClipChildren(false);
        sliderFrame.setClipToPadding(false);
        LinearLayout.LayoutParams sliderFrameParams = new LinearLayout.LayoutParams(
                -1, LauncherUiUtils.dp(this, 42));
        View spectrumRail = new View(this);
        GradientDrawable spectrumBackground = enabled
                ? new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, spectrumColors)
                : new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                        new int[]{Color.WHITE, Color.WHITE});
        spectrumBackground.setCornerRadius(LauncherUiUtils.dp(this, 5));
        if (!enabled) spectrumBackground.setStroke(LauncherUiUtils.dp(this, 1),
                Color.rgb(205, 210, 220));
        spectrumRail.setBackground(spectrumBackground);
        FrameLayout.LayoutParams railParams = new FrameLayout.LayoutParams(
                -1, LauncherUiUtils.dp(this, 8), Gravity.CENTER_VERTICAL);
        railParams.leftMargin = LauncherUiUtils.dp(this, 9);
        railParams.rightMargin = LauncherUiUtils.dp(this, 9);
        sliderFrame.addView(spectrumRail, railParams);

        SeekBar seekBar = new SeekBar(this);
        seekBar.setMax(1000);
        int initial = Math.max(0, Math.min(1000, current));
        seekBar.setProgress(initial);
        seekBar.setEnabled(enabled);
        seekBar.setSplitTrack(false);
        seekBar.setBackgroundColor(Color.TRANSPARENT);
        seekBar.setPadding(LauncherUiUtils.dp(this, 9), 0,
                LauncherUiUtils.dp(this, 9), 0);
        seekBar.setThumbOffset(LauncherUiUtils.dp(this, 9));
        seekBar.setProgressDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        seekBar.setProgressTintList(android.content.res.ColorStateList.valueOf(Color.TRANSPARENT));
        seekBar.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.TRANSPARENT));

        // The round 18dp thumb is 50% larger than a standard 12dp picker thumb.
        final GradientDrawable pickerThumb = new GradientDrawable();
        pickerThumb.setShape(GradientDrawable.OVAL);
        pickerThumb.setSize(LauncherUiUtils.dp(this, 18), LauncherUiUtils.dp(this, 18));
        pickerThumb.setColor(LauncherIconDrawable.spectrumColorForPosition(initial));
        pickerThumb.setStroke(LauncherUiUtils.dp(this, 2),
                enabled ? Color.WHITE : Color.rgb(160, 166, 178));
        seekBar.setThumb(pickerThumb);
        sliderFrame.addView(seekBar, new FrameLayout.LayoutParams(-1, -1));
        block.addView(sliderFrame, sliderFrameParams);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                int color = enabled ? LauncherIconDrawable.spectrumColorForPosition(progress)
                        : Color.WHITE;
                valueLabel.setText(colorValueLabel(color));
                pickerThumb.setColor(color);
                pickerThumb.invalidateSelf();
                if (enabled && fromUser) action.onChange(progress);
            }
            @Override public void onStartTrackingTouch(SeekBar bar) {}
            @Override public void onStopTrackingTouch(SeekBar bar) {}
        });
        panel.addView(block, blockParams);
    }

    private String colorValueLabel(int color) {
        return String.format(Locale.US, "#%06X", color & 0x00FFFFFF);
    }

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
        // Keep slider controls visually distinct from their dark setting cards.
        android.content.res.ColorStateList tint =
                android.content.res.ColorStateList.valueOf(Color.rgb(222, 255, 112));
        seekBar.setProgressTintList(tint);
        seekBar.setThumbTintList(tint);
        seekBar.setProgressBackgroundTintList(
                android.content.res.ColorStateList.valueOf(Color.rgb(82, 96, 122)));
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

    /** Adds two compact slider cards on one row for the two independent home-screen clocks. */
    private void addSideBySideSliderPair(LinearLayout panel,
            String leftTitle, int min, int max, int leftCurrent, String suffix,
            IntSliderAction leftAction, String rightTitle, int rightCurrent,
            IntSliderAction rightAction) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.TOP);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2);
        rowParams.bottomMargin = LauncherUiUtils.dp(this, 7);
        panel.addView(row, rowParams);
        addCompactSliderCard(row, leftTitle, min, max, leftCurrent, suffix, leftAction, true);
        addCompactSliderCard(row, rightTitle, min, max, rightCurrent, suffix, rightAction, false);
    }

    private void addCompactSliderCard(LinearLayout row, String title, int min, int max,
            int current, String suffix, IntSliderAction action, boolean leftCard) {
        LinearLayout block = new LinearLayout(this);
        block.setOrientation(LinearLayout.VERTICAL);
        block.setPadding(LauncherUiUtils.dp(this, 8), LauncherUiUtils.dp(this, 8),
                LauncherUiUtils.dp(this, 8), LauncherUiUtils.dp(this, 5));
        block.setBackground(studioControlBackground());
        addStudioAccentLine(block);

        LinearLayout.LayoutParams blockParams =
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        if (leftCard) blockParams.rightMargin = LauncherUiUtils.dp(this, 3);
        else blockParams.leftMargin = LauncherUiUtils.dp(this, 3);
        row.addView(block, blockParams);

        LinearLayout heading = new LinearLayout(this);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        TextView label = LauncherUiUtils.text(this, title, 12.5f,
                Color.rgb(247, 249, 255), true);
        label.setMaxLines(2);
        label.setEllipsize(android.text.TextUtils.TruncateAt.END);
        heading.addView(label, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView valueLabel = LauncherUiUtils.text(this,
                Math.max(min, Math.min(max, current)) + suffix, 12,
                homeStudioAccentColor, true);
        heading.addView(valueLabel);
        block.addView(heading);

        SeekBar seekBar = new SeekBar(this);
        seekBar.setMax(max - min);
        seekBar.setProgress(Math.max(0, Math.min(max - min, current - min)));
        android.content.res.ColorStateList tint =
                android.content.res.ColorStateList.valueOf(Color.rgb(222, 255, 112));
        seekBar.setProgressTintList(tint);
        seekBar.setThumbTintList(tint);
        seekBar.setProgressBackgroundTintList(
                android.content.res.ColorStateList.valueOf(Color.rgb(82, 96, 122)));
        block.addView(seekBar, new LinearLayout.LayoutParams(-1,
                LauncherUiUtils.dp(this, 38)));
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                int value = min + progress;
                valueLabel.setText(value + suffix);
                if (fromUser && action != null) action.onChange(value);
            }
            @Override public void onStartTrackingTouch(SeekBar bar) {}
            @Override public void onStopTrackingTouch(SeekBar bar) {}
        });
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
        // Keep slider controls visually distinct from their dark setting cards.
        android.content.res.ColorStateList tint =
                android.content.res.ColorStateList.valueOf(Color.rgb(222, 255, 112));
        seekBar.setProgressTintList(tint);
        seekBar.setThumbTintList(tint);
        seekBar.setProgressBackgroundTintList(
                android.content.res.ColorStateList.valueOf(Color.rgb(82, 96, 122)));
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
