package com.dsann.launcher;

import android.app.Activity;
import android.app.Dialog;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

/** Owns widget container gestures, movement, resize and action menu interactions. */
final class WidgetInteractionController {
    interface Listener {
        void saveWidgetGeometry(int widgetId, int pageIndex, FrameLayout container);
        void ensurePageExists(int pageIndex);
        void renderHomePages(boolean animate);
        void showActionDialog(String title, String[] icons, String[] labels,
                              DialogInterface.OnClickListener listener);
        void setCurrentPage(int page);
        int getCurrentPage();
        void showPage(int page, boolean animate);
        LinearLayout getPagesStrip();
        void resolveWidgetCollisions(FrameLayout page, FrameLayout widgetContainer);
        boolean canPlaceWorkspaceItem(FrameLayout page, View item,
                int left, int top, int width, int height);
        void updateLiveDisplacements(FrameLayout page, View movingView);
        void scheduleLiveDisplacementUpdate(FrameLayout page, View movingView);
        boolean finishLiveDisplacements(boolean commit, View movingView);
        void setWorkspaceGridVisible(boolean visible, View movingView);
        void beginWorkspaceTransferDrag(View item, int sourcePage, float rawX, float rawY,
                float fingerOffsetX, float fingerOffsetY);
        void updateWorkspaceTransferEdge(float rawX, float rawY);
        int getWorkspaceTransferTargetPage();
        void updateWorkspaceTransferDisplacements(View item, float rawX, float rawY,
                float fingerOffsetX, float fingerOffsetY);
        int[] prepareWidgetDrop(FrameLayout targetPage, View widget, float rawX, float rawY,
                float fingerOffsetX, float fingerOffsetY);
        void updatePlacedWidgetSize(int widgetId, FrameLayout container);
        int finishWorkspaceTransferAnimation();
        void finishWorkspaceTransferDrag(View item, boolean commit);
    }

    private final Activity activity;
    private final Handler handler;
    private final AppWidgetHost widgetHost;
    private final SharedPreferences prefs;
    private final String widgetIdsKey;
    private final Listener listener;
    private static final class EditChrome {
        final View container;
        final View[] resizeHandles;
        final Runnable clear;
        EditChrome(View container, View[] resizeHandles, Runnable clear) {
            this.container = container; this.resizeHandles = resizeHandles; this.clear = clear;
        }
    }
    private final java.util.ArrayList<EditChrome> editChrome = new java.util.ArrayList<>();

    /** Dismiss active widget-resize mode only when the tap is outside that widget. */
    boolean clearEditChromeOutsideTouch(float rawX, float rawY) {
        boolean dismissed = false;
        for (int i = editChrome.size() - 1; i >= 0; i--) {
            EditChrome chrome = editChrome.get(i);
            if (chrome.container.getParent() == null) {
                editChrome.remove(i);
                continue;
            }
            boolean resizeMode = false;
            for (View handle : chrome.resizeHandles) {
                if (handle != null && handle.getVisibility() == View.VISIBLE) {
                    resizeMode = true;
                    break;
                }
            }
            if (!resizeMode) continue;
            int[] location = new int[2];
            chrome.container.getLocationOnScreen(location);
            float scaleX = Math.max(0.01f, chrome.container.getScaleX());
            float scaleY = Math.max(0.01f, chrome.container.getScaleY());
            boolean insideContainer = rawX >= location[0]
                    && rawX < location[0] + chrome.container.getWidth() * scaleX
                    && rawY >= location[1]
                    && rawY < location[1] + chrome.container.getHeight() * scaleY;
            if (!insideContainer) {
                chrome.clear.run();
                dismissed = true;
            }
        }
        return dismissed;
    }

    WidgetInteractionController(Activity activity, Handler handler, AppWidgetHost widgetHost,
                                SharedPreferences prefs, String widgetIdsKey, Listener listener) {
        this.activity = activity;
        this.handler = handler;
        this.widgetHost = widgetHost;
        this.prefs = prefs;
        this.widgetIdsKey = widgetIdsKey;
        this.listener = listener;
    }

    /**
     * Visual workspace bounds, accounting for pending FrameLayout margins, requested
     * dimensions, translation and scale. This avoids stale geometry during drop.
     */
    private android.graphics.Rect workspaceBounds(View view) {
        if (view == null) return new android.graphics.Rect();
        int left = view.getLeft();
        int top = view.getTop();
        int width = view.getWidth();
        int height = view.getHeight();
        ViewParent parent = view.getParent();
        ViewGroup.LayoutParams raw = view.getLayoutParams();
        if (parent instanceof FrameLayout && raw instanceof FrameLayout.LayoutParams) {
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) raw;
            if (view.isLayoutRequested() || lp.leftMargin != view.getLeft()
                    || lp.topMargin != view.getTop()) {
                left = lp.leftMargin;
                top = lp.topMargin;
            }
            if (lp.width > 0) width = lp.width;
            if (lp.height > 0) height = lp.height;
        }
        float sx = Math.max(0.01f, view.getScaleX());
        float sy = Math.max(0.01f, view.getScaleY());
        left = Math.round(left + view.getTranslationX() + view.getPivotX() * (1f - sx));
        top = Math.round(top + view.getTranslationY() + view.getPivotY() * (1f - sy));
        width = Math.max(1, Math.round(Math.max(1, width) * sx));
        height = Math.max(1, Math.round(Math.max(1, height) * sy));

        // App cells fill their invisible home-grid slot. Only the artwork and painted
        // label are visible collision surfaces; treating the whole cell as occupied
        // rejected valid widget placements in the open padding around an icon.
        if (view.getTag() instanceof String
                && ((String) view.getTag()).startsWith("launcher_app:")
                && view instanceof ViewGroup) {
            ViewGroup cell = (ViewGroup) view;
            android.graphics.Rect cellBounds =
                    new android.graphics.Rect(left, top, left + width, top + height);
            float parentScaleX = Math.max(0.01f, view.getScaleX());
            float parentScaleY = Math.max(0.01f, view.getScaleY());
            android.graphics.Rect visual = null;
            int visibleChildCount = Math.min(2, cell.getChildCount());
            for (int i = 0; i < visibleChildCount; i++) {
                View child = cell.getChildAt(i);
                if (child == null || child.getVisibility() != View.VISIBLE
                        || child.getWidth() <= 0 || child.getHeight() <= 0) continue;
                float childLeft = child.getLeft() + child.getTranslationX()
                        + child.getPivotX() * (1f - child.getScaleX());
                float childTop = child.getTop() + child.getTranslationY()
                        + child.getPivotY() * (1f - child.getScaleY());
                float childWidth = child.getWidth() * Math.max(0.01f, child.getScaleX());
                float childHeight = child.getHeight() * Math.max(0.01f, child.getScaleY());

                // TextViews occupy the whole cell width, but only the centered glyph
                // run is painted. Do not make the invisible label padding a blocker.
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
                    childLeft += label.getCompoundPaddingLeft()
                            + Math.max(0f, (label.getWidth()
                                    - label.getCompoundPaddingLeft()
                                    - label.getCompoundPaddingRight() - paintedWidth) / 2f);
                    childTop += label.getCompoundPaddingTop()
                            + Math.max(0f, (label.getHeight()
                                    - label.getCompoundPaddingTop()
                                    - label.getCompoundPaddingBottom() - paintedHeight) / 2f);
                    childWidth = paintedWidth;
                    childHeight = paintedHeight;
                }

                int childX = left + Math.round(childLeft * parentScaleX);
                int childY = top + Math.round(childTop * parentScaleY);
                android.graphics.Rect childBounds = new android.graphics.Rect(
                        childX, childY,
                        childX + Math.max(1, Math.round(childWidth * parentScaleX)),
                        childY + Math.max(1, Math.round(childHeight * parentScaleY)));
                if (cell.getClipChildren()) childBounds.intersect(cellBounds);
                if (childBounds.isEmpty()) continue;
                if (visual == null) visual = childBounds;
                else visual.union(childBounds);
            }
            if (visual != null && !visual.isEmpty()) return visual;
        }
        return new android.graphics.Rect(left, top, left + width, top + height);
    }

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

    void addWidgetContainer(FrameLayout page, AppWidgetHostView widget, int widgetId, int pageIndex,
                            int x, int y, int width, int height) {
        addWidgetContainer(page, widget, widgetId, pageIndex, x, y, width, height, true);
    }

    void addWidgetContainer(FrameLayout page, AppWidgetHostView widget, int widgetId, int pageIndex,
                            int x, int y, int width, int height, boolean showNoSpaceToast) {
        addWidgetContainer(page, widget, widgetId, pageIndex, x, y, width, height,
                showNoSpaceToast, false);
    }

    void addWidgetContainer(FrameLayout page, AppWidgetHostView widget, int widgetId, int pageIndex,
                            int x, int y, int width, int height, boolean showNoSpaceToast,
                            boolean preserveGeometry) {
        final FrameLayout container = new FrameLayout(activity);
        container.setTag("launcher_widget:" + widgetId);
        widget.setTag("launcher_widget_content:" + widgetId);
        widget.setFocusable(true);
        container.addView(widget, new FrameLayout.LayoutParams(-1, -1));
        final View[] resizeHandles = new View[4];
        for (int edge = 0; edge < resizeHandles.length; edge++) {
            final int resizeEdge = edge;
            View handle = new View(activity);
            handle.setTag("launcher_widget_resize_handle");
            final int touchLength = LauncherUiUtils.dp(activity, 44);
            final int touchThickness = LauncherUiUtils.dp(activity, 24);
            final int visualLength = LauncherUiUtils.dp(activity, 30);
            final int visualThickness = LauncherUiUtils.dp(activity, 4);
            final int edgeGap = LauncherUiUtils.dp(activity, 1);
            GradientDrawable handleBg = new GradientDrawable();
            handleBg.setColor(Color.rgb(115, 159, 255));
            handleBg.setCornerRadius(visualThickness / 2f);
            int centeredInset = Math.max(0, (touchLength - visualLength) / 2);
            int innerEdgeInset = Math.max(0, touchThickness - visualThickness - edgeGap);
            int insetLeft = resizeEdge == 0 ? edgeGap
                    : resizeEdge == 1 ? innerEdgeInset : centeredInset;
            int insetTop = resizeEdge == 2 ? edgeGap
                    : resizeEdge == 3 ? innerEdgeInset : centeredInset;
            int insetRight = resizeEdge == 1 ? edgeGap
                    : resizeEdge == 0 ? innerEdgeInset : centeredInset;
            int insetBottom = resizeEdge == 3 ? edgeGap
                    : resizeEdge == 2 ? innerEdgeInset : centeredInset;
            handle.setBackground(new android.graphics.drawable.InsetDrawable(
                    handleBg, insetLeft, insetTop, insetRight, insetBottom));
            handle.setVisibility(View.GONE);
            FrameLayout.LayoutParams handleLp = edge < 2
                    ? new FrameLayout.LayoutParams(touchThickness, touchLength,
                            edge == 0 ? Gravity.LEFT | Gravity.CENTER_VERTICAL
                                    : Gravity.RIGHT | Gravity.CENTER_VERTICAL)
                    : new FrameLayout.LayoutParams(touchLength, touchThickness,
                            edge == 2 ? Gravity.TOP | Gravity.CENTER_HORIZONTAL
                                    : Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            container.addView(handle, handleLp);
            resizeHandles[edge] = handle;
        }
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(Math.max(1, width), Math.max(1, height), Gravity.TOP | Gravity.LEFT);
        lp.leftMargin = x;
        lp.topMargin = y;
        // Choose a free rectangle before the widget becomes visible. The post-layout
        // collision pass remains as a safety net, but initial placement must not flash
        // or persist on top of an app, another widget, shortcut, or clock.
        if (preserveGeometry && page.getWidth() > 0 && page.getHeight() > 0) {
            // A restoration must not be re-laid out based on transient provider/app
            // positions; retain the saved rectangle and only enforce page boundaries.
            lp.leftMargin = Math.max(0, Math.min(Math.max(0, page.getWidth() - lp.width), x));
            lp.topMargin = Math.max(0, Math.min(Math.max(0, page.getHeight() - lp.height), y));
        }
        if (!preserveGeometry && page.getWidth() > 0 && page.getHeight() > 0) {
            int maxX = Math.max(0, page.getWidth() - lp.width);
            int maxY = Math.max(0, page.getHeight() - lp.height);
            int desiredX = Math.max(0, Math.min(maxX, x));
            int desiredY = Math.max(0, Math.min(maxY, y));
            int bestX = -1, bestY = -1;
            long bestDistance = Long.MAX_VALUE;

            // Cache occupied visual rectangles once. The old 24dp-step scan could
            // skip narrow, valid gaps and incorrectly tell the user the page was full.
            java.util.ArrayList<android.graphics.Rect> occupied =
                    new java.util.ArrayList<android.graphics.Rect>();
            for (int i = 0; i < page.getChildCount(); i++) {
                View other = page.getChildAt(i);
                if (!(other.getTag() instanceof String)) continue;
                String tag = (String) other.getTag();
                if (!(tag.startsWith("launcher_app:") || tag.startsWith("launcher_widget:")
                        || "launcher_contact_shortcut".equals(tag)
                        || "launcher_clock_widget".equals(tag))) continue;
                android.graphics.Rect bounds = workspaceBounds(other);
                if (!bounds.isEmpty()) occupied.add(bounds);
            }

            // Test the requested position and exact obstacle edges first. For
            // axis-aligned rectangles these boundary candidates find narrow gaps
            // without a dense full-screen scan on every widget addition.
            java.util.TreeSet<Integer> xCandidates = new java.util.TreeSet<Integer>();
            java.util.TreeSet<Integer> yCandidates = new java.util.TreeSet<Integer>();
            xCandidates.add(desiredX);
            yCandidates.add(desiredY);
            xCandidates.add(0);
            yCandidates.add(0);
            xCandidates.add(maxX);
            yCandidates.add(maxY);
            for (android.graphics.Rect obstacle : occupied) {
                xCandidates.add(Math.max(0, Math.min(maxX, obstacle.left - lp.width)));
                xCandidates.add(Math.max(0, Math.min(maxX, obstacle.right)));
                yCandidates.add(Math.max(0, Math.min(maxY, obstacle.top - lp.height)));
                yCandidates.add(Math.max(0, Math.min(maxY, obstacle.bottom)));
            }
            for (Integer cy : yCandidates) {
                for (Integer cx : xCandidates) {
                    int candidateX = cx.intValue(), candidateY = cy.intValue();
                    android.graphics.Rect candidate = new android.graphics.Rect(
                            candidateX, candidateY, candidateX + lp.width, candidateY + lp.height);
                    boolean blocked = false;
                    for (android.graphics.Rect obstacle : occupied) {
                        if (android.graphics.Rect.intersects(candidate, obstacle)) {
                            blocked = true;
                            break;
                        }
                    }
                    if (blocked) continue;
                    long distance = Math.abs((long) candidateX - desiredX)
                            + Math.abs((long) candidateY - desiredY);
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        bestX = candidateX;
                        bestY = candidateY;
                    }
                }
            }

            // Boundary combinations are usually sufficient, but scan at a finer
            // interval as a fallback for irregular/rounded widget footprints.
            if (bestX < 0) {
                int step = Math.max(1, LauncherUiUtils.dp(activity, 6));
                for (int cy = 0; cy <= maxY; cy += step) {
                    int candidateY = Math.min(maxY, cy);
                    for (int cx = 0; cx <= maxX; cx += step) {
                        int candidateX = Math.min(maxX, cx);
                        android.graphics.Rect candidate = new android.graphics.Rect(
                                candidateX, candidateY,
                                candidateX + lp.width, candidateY + lp.height);
                        boolean blocked = false;
                        for (android.graphics.Rect obstacle : occupied) {
                            if (android.graphics.Rect.intersects(candidate, obstacle)) {
                                blocked = true;
                                break;
                            }
                        }
                        if (blocked) continue;
                        long distance = Math.abs((long) candidateX - desiredX)
                                + Math.abs((long) candidateY - desiredY);
                        if (distance < bestDistance) {
                            bestDistance = distance;
                            bestX = candidateX;
                            bestY = candidateY;
                        }
                    }
                    if (candidateY == maxY) break;
                }
                int[][] edges = {{maxX, desiredY}, {desiredX, maxY}, {maxX, maxY}};
                for (int[] edge : edges) {
                    android.graphics.Rect candidate = new android.graphics.Rect(
                            edge[0], edge[1], edge[0] + lp.width, edge[1] + lp.height);
                    boolean blocked = false;
                    for (android.graphics.Rect obstacle : occupied) {
                        if (android.graphics.Rect.intersects(candidate, obstacle)) {
                            blocked = true;
                            break;
                        }
                    }
                    if (!blocked) {
                        long distance = Math.abs((long) edge[0] - desiredX)
                                + Math.abs((long) edge[1] - desiredY);
                        if (distance < bestDistance) {
                            bestDistance = distance;
                            bestX = edge[0];
                            bestY = edge[1];
                        }
                    }
                }
            }
            if (bestX >= 0) {
                lp.leftMargin = bestX;
                lp.topMargin = bestY;
            } else {
                // Do not intentionally place a new widget over existing content.
                if (showNoSpaceToast) {
                    Toast.makeText(activity, "No free space for this widget", Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }
        page.addView(container, lp);
        container.bringToFront();
        // First placement needs the same collision pass as a completed drag/resize.
        // Post it so the page and widget have measured geometry before bounds are tested.
        page.post(() -> {
            // Restoration must not run the placement resolver: provider updates can
            // temporarily change measured bounds and otherwise overwrite saved geometry.
            if (!preserveGeometry && container.getParent() == page) {
                listener.resolveWidgetCollisions(page, container);
            }
        });

        // A transparent interaction layer observes long presses across the full widget.
        // Ordinary touch events are forwarded to the AppWidgetHostView so its controls still work.
        final View gestureGrip = new View(activity);
        gestureGrip.setBackground(LauncherUiUtils.roundRect(activity, new int[]{Color.argb(75, 115, 159, 255), Color.argb(25, 115, 159, 255)}, LauncherUiUtils.dp(activity, 8)));
        FrameLayout.LayoutParams gripLp = new FrameLayout.LayoutParams(-1, -1, Gravity.TOP | Gravity.LEFT);
        container.addView(gestureGrip, gripLp);
        for (View handle : resizeHandles) { handle.bringToFront(); handle.setClickable(true); }

        final int[] editMode = {0}; // 0 idle, 1 move-ready / moving
        final float[] down = {0f, 0f};
        final float[] fingerOffset = {0f, 0f};
        final int[] origin = {x, y, lp.width, lp.height};
        final boolean[] resizeRejectedAtLastPosition = {false};
        final int[] widgetPage = {pageIndex};
        final int[] originalPageAtDown = {pageIndex};
        final int[] edgeLatch = {0};
        final Runnable[] movePress = new Runnable[1];
        final Runnable[] menuPress = new Runnable[1];
        final Runnable[] clearEditChrome = new Runnable[1];
        final boolean[] childCanceled = {false};
        final boolean[] resizeModeTouch = {false};
        final boolean[] menuShown = {false};
        final boolean[] dragging = {false};
        // Removing a touched widget synchronously dispatches ACTION_CANCEL on some Android
        // versions. Guard reparenting so that cancellation cannot recursively remove it.
        final boolean[] reparenting = {false};
        final float[] lastTouch = {0f, 0f};
        final long[] downTime = {0L};
        Runnable cancelWidgetTouch = () -> {
            if (childCanceled[0]) return;
            long now = SystemClock.uptimeMillis();
            MotionEvent cancel = MotionEvent.obtain(downTime[0], now, MotionEvent.ACTION_CANCEL,
                    lastTouch[0], lastTouch[1], 0);
            try { widget.dispatchTouchEvent(cancel); } finally { cancel.recycle(); }
            childCanceled[0] = true;
        };
        gestureGrip.setBackgroundColor(Color.TRANSPARENT);
        clearEditChrome[0] = () -> {
            listener.setWorkspaceGridVisible(false, null);
            for (View handle : resizeHandles) handle.setVisibility(View.GONE);
            container.setBackground(null);
            gestureGrip.setBackgroundColor(Color.TRANSPARENT);
        };
        editChrome.add(new EditChrome(container, resizeHandles, () -> {
            clearEditChrome[0].run();
            if (editMode[0] == 1) LauncherUiUtils.stopMoveHint(container);
            editMode[0] = 0;
            dragging[0] = false;
        }));

        // Long press arms movement with a steady 10% enlargement. It deliberately does
        // not expose resize handles or run any wobble/bobbing animation.
        movePress[0] = () -> {
            if (menuShown[0] || dragging[0] || reparenting[0]) return;
            editMode[0] = 1;
            LauncherUiUtils.startScaleHint(container, 1.10f);
            cancelWidgetTouch.run();
        };
        // The two-second stationary hold remains the explicit widget-options gesture.
        // Once a real drag starts, its timer is canceled and can never open a menu mid-drag.
        menuPress[0] = () -> {
            if (dragging[0] || menuShown[0] || reparenting[0]) return;
            handler.removeCallbacks(movePress[0]);
            if (editMode[0] == 1) LauncherUiUtils.stopMoveHint(container);
            editMode[0] = 0;
            menuShown[0] = true;
            cancelWidgetTouch.run();
            clearEditChrome[0].run();
            String[] actions = {"Remove widget", "Resize widget"};
            String[] icons = {"×", "↘"};
            listener.showActionDialog("Widget options", icons, actions, (dialog, which) -> {
                if (which == 0) {
                    reparenting[0] = true;
                    if (container.getParent() instanceof ViewGroup) ((ViewGroup) container.getParent()).removeView(container);
                    WidgetController.removeWidget(prefs, widgetIdsKey, widgetId);
                    try { widgetHost.deleteAppWidgetId(widgetId); } catch (Exception ignored) {}
                    listener.renderHomePages(false);
                } else if (which == 1) {
                    for (View handle : resizeHandles) handle.setVisibility(View.VISIBLE);
                    container.setBackground(LauncherUiUtils.roundRect(activity, new int[]{Color.argb(65, 115, 159, 255), Color.argb(30, 115, 159, 255)}, LauncherUiUtils.dp(activity, 8)));
                    Toast.makeText(activity, "Drag the blue corner handle to resize", Toast.LENGTH_LONG).show();
                }
            });
        };
        gestureGrip.setOnTouchListener((view, event) -> {
            // removeView() can dispatch a nested ACTION_CANCEL while the old parent is
            // detaching this view. Never run the gesture completion path recursively.
            if (reparenting[0]) return true;
            int action = event.getActionMasked();
            if (resizeModeTouch[0]) {
                if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                    resizeModeTouch[0] = false;
                }
                return true;
            }
            if (action == MotionEvent.ACTION_DOWN) {
                for (View handle : resizeHandles) {
                    if (handle != null && handle.getVisibility() == View.VISIBLE) {
                        // Taps inside the widget are inert while it is in resize mode.
                        resizeModeTouch[0] = true;
                        return true;
                    }
                }
            }
            lastTouch[0] = event.getX();
            lastTouch[1] = event.getY();

            if (action == MotionEvent.ACTION_DOWN) {
                down[0] = event.getRawX();
                down[1] = event.getRawY();
                downTime[0] = event.getDownTime();
                int[] containerLocation = new int[2];
                container.getLocationOnScreen(containerLocation);
                fingerOffset[0] = down[0] - containerLocation[0];
                fingerOffset[1] = down[1] - containerLocation[1];
                FrameLayout.LayoutParams current = (FrameLayout.LayoutParams) container.getLayoutParams();
                origin[0] = current.leftMargin;
                origin[1] = current.topMargin;
                origin[2] = current.width;
                origin[3] = current.height;
                LinearLayout currentStrip = listener.getPagesStrip();
                ViewParent parent = container.getParent();
                int actualPage = parent instanceof View
                        ? (currentStrip == null ? -1 : currentStrip.indexOfChild((View) parent)) : -1;
                originalPageAtDown[0] = actualPage >= 0 ? actualPage : 0;
                widgetPage[0] = originalPageAtDown[0];
                editMode[0] = 0;
                dragging[0] = false;
                resizeRejectedAtLastPosition[0] = false;
                childCanceled[0] = false;
                menuShown[0] = false;
                handler.removeCallbacks(movePress[0]);
                handler.removeCallbacks(menuPress[0]);
                widget.dispatchTouchEvent(event);
                handler.postDelayed(movePress[0], 1000L);
                handler.postDelayed(menuPress[0], 2000L);
                return true;
            }

            if (menuShown[0]) {
                if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                    handler.removeCallbacks(movePress[0]);
                    handler.removeCallbacks(menuPress[0]);
                }
                return true;
            }

            if (action == MotionEvent.ACTION_MOVE) {
                float dx = event.getRawX() - down[0];
                float dy = event.getRawY() - down[1];
                boolean pastSlop = Math.abs(dx) > LauncherUiUtils.dp(activity, 10)
                        || Math.abs(dy) > LauncherUiUtils.dp(activity, 10);

                // Until long-press arms movement, preserve normal widget taps and scrolling.
                if (editMode[0] == 0) {
                    if (pastSlop) {
                        handler.removeCallbacks(movePress[0]);
                        handler.removeCallbacks(menuPress[0]);
                        if (!childCanceled[0]) widget.dispatchTouchEvent(event);
                    }
                    return true;
                }

                // Ignore tiny touch jitter after arming. Only a real finger displacement
                // starts a drag; after that, keep moving mode alive through every MOVE event.
                if (!dragging[0]) {
                    if (!pastSlop) return true;
                    LauncherUiUtils.stopMoveHint(container);
                    dragging[0] = true;
                    handler.removeCallbacks(menuPress[0]);
                    listener.beginWorkspaceTransferDrag(container, originalPageAtDown[0],
                            event.getRawX(), event.getRawY(), fingerOffset[0], fingerOffset[1]);
                }
                listener.updateWorkspaceTransferEdge(event.getRawX(), event.getRawY());
                cancelWidgetTouch.run();
                ViewParent gestureParent = gestureGrip.getParent();
                if (gestureParent instanceof ViewGroup) {
                    ((ViewGroup) gestureParent).requestDisallowInterceptTouchEvent(true);
                }

                LinearLayout strip = listener.getPagesStrip();
                FrameLayout sourcePage = container.getParent() instanceof FrameLayout
                        ? (FrameLayout) container.getParent() : page;
                // Keep the actively touched view in its original parent while the other
                // page is revealed. Reparenting a live touch target sends ACTION_CANCEL.
                int destinationPage = listener.getWorkspaceTransferTargetPage();
                if (destinationPage < 0) destinationPage = originalPageAtDown[0];
                listener.ensurePageExists(destinationPage);
                FrameLayout targetPage = strip != null && destinationPage < strip.getChildCount()
                        && strip.getChildAt(destinationPage) instanceof FrameLayout
                        ? (FrameLayout) strip.getChildAt(destinationPage) : sourcePage;
                FrameLayout.LayoutParams current = (FrameLayout.LayoutParams) container.getLayoutParams();
                int[] sourcePageLocation = new int[2];
                sourcePage.getLocationOnScreen(sourcePageLocation);
                int dropWidth = Math.max(1, current.width > 0 ? current.width : container.getWidth());
                int dropHeight = Math.max(1, current.height > 0 ? current.height : container.getHeight());
                int maxX = Math.max(0, sourcePage.getWidth() - dropWidth);
                int maxY = Math.max(0, sourcePage.getHeight() - dropHeight);
                int left = Math.round(event.getRawX() - sourcePageLocation[0]
                        - fingerOffset[0] - container.getTranslationX()
                        - container.getPivotX() * (1f - Math.max(0.01f, container.getScaleX())));
                int top = Math.round(event.getRawY() - sourcePageLocation[1]
                        - fingerOffset[1] - container.getTranslationY()
                        - container.getPivotY() * (1f - Math.max(0.01f, container.getScaleY())));
                int candidateLeft = Math.max(0, Math.min(maxX, left));
                int candidateTop = Math.max(0, Math.min(maxY, top));
                boolean crossPagePreview = destinationPage != originalPageAtDown[0];
                current.leftMargin = candidateLeft;
                current.topMargin = candidateTop;
                container.setLayoutParams(current);
                if (crossPagePreview) {
                    listener.updateWorkspaceTransferDisplacements(container, event.getRawX(),
                            event.getRawY(), fingerOffset[0], fingerOffset[1]);
                } else {
                    listener.scheduleLiveDisplacementUpdate(sourcePage, container);
                }
                widgetPage[0] = destinationPage;
                return true;
            }

            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                handler.removeCallbacks(movePress[0]);
                handler.removeCallbacks(menuPress[0]);
                boolean didDrag = dragging[0];

                if (editMode[0] == 1) {
                    LauncherUiUtils.stopMoveHint(container);
                    editMode[0] = 0;
                }

                if (didDrag) {
                    boolean commit = action == MotionEvent.ACTION_UP;
                    int targetIndex = listener.finishWorkspaceTransferAnimation();
                    if (!commit || targetIndex < 0) targetIndex = originalPageAtDown[0];
                    FrameLayout sourcePage = container.getParent() instanceof FrameLayout
                            ? (FrameLayout) container.getParent() : page;
                    LinearLayout strip = listener.getPagesStrip();
                    FrameLayout targetPage = sourcePage;
                    if (commit && strip != null && targetIndex >= 0
                            && targetIndex < strip.getChildCount()
                            && strip.getChildAt(targetIndex) instanceof FrameLayout) {
                        targetPage = (FrameLayout) strip.getChildAt(targetIndex);
                    }
                    FrameLayout.LayoutParams current = (FrameLayout.LayoutParams) container.getLayoutParams();
                    if (commit && targetPage != sourcePage) {
                        // Cross-page drops are recalculated in the target page's coordinate
                        // space and then passed through prepareWidgetDrop() for collision-safe
                        // placement. Same-page drops retain the last collision-safe geometry
                        // from MOVE handling; otherwise this raw finger coordinate can put a
                        // widget back under an oversized clock on ACTION_UP.
                        int[] targetLocation = new int[2];
                        targetPage.getLocationOnScreen(targetLocation);
                        int dropWidth = Math.max(1, current.width > 0 ? current.width : container.getWidth());
                        int dropHeight = Math.max(1, current.height > 0 ? current.height : container.getHeight());
                        current.leftMargin = Math.max(0, Math.min(Math.max(0, targetPage.getWidth() - dropWidth),
                                Math.round(event.getRawX() - targetLocation[0] - fingerOffset[0])));
                        current.topMargin = Math.max(0, Math.min(Math.max(0, targetPage.getHeight() - dropHeight),
                                Math.round(event.getRawY() - targetLocation[1] - fingerOffset[1])));
                    } else if (!commit) {
                        current.leftMargin = origin[0];
                        current.topMargin = origin[1];
                    }
                    container.setTranslationX(0f);
                    container.setTranslationY(0f);
                    container.setLayoutParams(current);
                    int[] preparedDrop = commit && targetPage != sourcePage
                            ? listener.prepareWidgetDrop(targetPage, container,
                                    event.getRawX(), event.getRawY(),
                                    fingerOffset[0], fingerOffset[1]) : null;
                    if (commit && targetPage != sourcePage && preparedDrop == null) {
                        // The destination cannot hold the widget at the exact requested size
                        // and position. Roll back instead of committing an overlap.
                        current.leftMargin = origin[0];
                        current.topMargin = origin[1];
                        container.setTranslationX(0f);
                        container.setTranslationY(0f);
                        container.setLayoutParams(current);
                        widgetPage[0] = originalPageAtDown[0];
                        listener.finishWorkspaceTransferDrag(container, false);
                        listener.setWorkspaceGridVisible(false, null);
                        listener.finishLiveDisplacements(false, container);
                        Toast.makeText(activity, "Not possible due to overlap",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        if (commit && targetPage != sourcePage) {
                            int oldWidth = current.width;
                            int oldHeight = current.height;
                            current.leftMargin = preparedDrop[0];
                            current.topMargin = preparedDrop[1];
                            current.width = preparedDrop[2];
                            current.height = preparedDrop[3];
                            container.setLayoutParams(current);
                            reparenting[0] = true;
                            sourcePage.removeView(container);
                            targetPage.addView(container, current);
                            reparenting[0] = false;
                            widgetPage[0] = targetIndex;
                            if (current.width != oldWidth || current.height != oldHeight) {
                                listener.updatePlacedWidgetSize(widgetId, container);
                            }
                            // Recheck the real, possibly resized widget at its final geometry.
                            listener.updateLiveDisplacements(targetPage, container);
                        } else {
                            widgetPage[0] = originalPageAtDown[0];
                        }
                        // Flush the final collision pass BEFORE transfer cleanup. The cleanup
                        // clears the mover's last-safe geometry; doing it first lets a pending
                        // ACTION_UP frame miss its rollback point and commit under a clock.
                        boolean accepted = listener.finishLiveDisplacements(commit, container);
                        if (commit && !accepted) {
                            FrameLayout.LayoutParams rejectedParams =
                                    (FrameLayout.LayoutParams) container.getLayoutParams();
                            rejectedParams.leftMargin = origin[0];
                            rejectedParams.topMargin = origin[1];
                            rejectedParams.width = origin[2];
                            rejectedParams.height = origin[3];
                            container.setTranslationX(0f);
                            container.setTranslationY(0f);
                            if (container.getParent() instanceof ViewGroup
                                    && container.getParent() != sourcePage) {
                                ViewGroup rejectedParent = (ViewGroup) container.getParent();
                                reparenting[0] = true;
                                rejectedParent.removeView(container);
                                sourcePage.addView(container, rejectedParams);
                                reparenting[0] = false;
                            } else {
                                container.setLayoutParams(rejectedParams);
                            }
                            widgetPage[0] = originalPageAtDown[0];
                            listener.finishWorkspaceTransferDrag(container, false);
                            listener.setCurrentPage(originalPageAtDown[0]);
                            listener.showPage(originalPageAtDown[0], false);
                        } else {
                            listener.finishWorkspaceTransferDrag(container, commit);
                        }
                        listener.setWorkspaceGridVisible(false, null);
                        if (commit && accepted) listener.saveWidgetGeometry(widgetId, widgetPage[0], container);
                    }
                } else {
                    listener.setWorkspaceGridVisible(false, null);
                    if (!childCanceled[0]) widget.dispatchTouchEvent(event);
                }

                ViewParent gestureParent = gestureGrip.getParent();
                if (gestureParent instanceof ViewGroup) {
                    ((ViewGroup) gestureParent).requestDisallowInterceptTouchEvent(false);
                }
                editMode[0] = 0;
                dragging[0] = false;
                return true;
            }

            if (!childCanceled[0]) widget.dispatchTouchEvent(event);
            return true;
        });
        for (int edge = 0; edge < resizeHandles.length; edge++) {
            final int resizeEdge = edge;
            resizeHandles[edge].setOnTouchListener((v, event) -> {
                int action = event.getActionMasked();
                if (action == MotionEvent.ACTION_DOWN) {
                    listener.setWorkspaceGridVisible(false, null);
                    resizeRejectedAtLastPosition[0] = false;
                    down[0] = event.getRawX(); down[1] = event.getRawY();
                    FrameLayout.LayoutParams current = (FrameLayout.LayoutParams) container.getLayoutParams();
                    origin[0] = current.leftMargin; origin[1] = current.topMargin;
                    origin[2] = current.width > 0 ? current.width : container.getWidth();
                    origin[3] = current.height > 0 ? current.height : container.getHeight();
                    container.getParent().requestDisallowInterceptTouchEvent(true);
                    return true;
                }
                if (action == MotionEvent.ACTION_MOVE) {
                    FrameLayout activePage = container.getParent() instanceof FrameLayout ? (FrameLayout) container.getParent() : page;
                    FrameLayout.LayoutParams current = (FrameLayout.LayoutParams) container.getLayoutParams();
                    int dx = Math.round(event.getRawX() - down[0]), dy = Math.round(event.getRawY() - down[1]);
                    int minW = Math.min(LauncherUiUtils.dp(activity, 140), Math.max(1, activePage.getWidth()));
                    int minH = Math.min(LauncherUiUtils.dp(activity, 90), Math.max(1, activePage.getHeight()));
                    int left = origin[0], top = origin[1], resizeWidth = origin[2], resizeHeight = origin[3];
                    // Keep the opposite edge fixed while dragging left/top handles.
                    // Clamp the moving edge itself, then derive size from the fixed edge.
                    if (resizeEdge == 0) {
                        int fixedRight = origin[0] + origin[2];
                        left = Math.max(0, Math.min(origin[0] + dx, fixedRight - minW));
                        resizeWidth = fixedRight - left;
                    } else if (resizeEdge == 1) {
                        resizeWidth = Math.max(minW, Math.min(origin[2] + dx, activePage.getWidth() - origin[0]));
                    } else if (resizeEdge == 2) {
                        int fixedBottom = origin[1] + origin[3];
                        top = Math.max(0, Math.min(origin[1] + dy, fixedBottom - minH));
                        resizeHeight = fixedBottom - top;
                    } else {
                        resizeHeight = Math.max(minH, Math.min(origin[3] + dy, activePage.getHeight() - origin[1]));
                    }
                    left = Math.max(0, Math.min(left, activePage.getWidth() - minW));
                    top = Math.max(0, Math.min(top, activePage.getHeight() - minH));
                    resizeWidth = Math.max(minW, Math.min(resizeWidth, activePage.getWidth() - left));
                    resizeHeight = Math.max(minH, Math.min(resizeHeight, activePage.getHeight() - top));
                    if (!listener.canPlaceWorkspaceItem(activePage, container,
                            left, top, resizeWidth, resizeHeight)) {
                        resizeRejectedAtLastPosition[0] = true;
                        return true;
                    }
                    resizeRejectedAtLastPosition[0] = false;
                    current.leftMargin = left;
                    current.topMargin = top;
                    current.width = resizeWidth;
                    current.height = resizeHeight;
                    container.setLayoutParams(current);
                    return true;
                }
                if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                    listener.setWorkspaceGridVisible(false, null);
                    FrameLayout.LayoutParams finalParams = (FrameLayout.LayoutParams) container.getLayoutParams();
                    if (action == MotionEvent.ACTION_CANCEL) {
                        finalParams.leftMargin = origin[0]; finalParams.topMargin = origin[1];
                        finalParams.width = origin[2]; finalParams.height = origin[3]; container.setLayoutParams(finalParams);
                    }
                    if (action == MotionEvent.ACTION_UP && resizeRejectedAtLastPosition[0]) {
                        Toast.makeText(activity, "Not possible due to overlap", Toast.LENGTH_SHORT).show();
                    }
                    if (container.getChildAt(0) instanceof AppWidgetHostView) {
                        WidgetController.updateWidgetSize(android.appwidget.AppWidgetManager.getInstance(activity),
                                widgetId, (AppWidgetHostView) container.getChildAt(0),
                                activity.getResources().getDisplayMetrics().density, finalParams.width, finalParams.height);
                    }
                    listener.saveWidgetGeometry(widgetId, widgetPage[0], container);
                    return true;
                }
                return true;
            });
        }
    
    }
}
