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
        void updateLiveDisplacements(FrameLayout page, View movingView);
        void finishLiveDisplacements(boolean commit, View movingView);
        void setWorkspaceGridVisible(boolean visible, View movingView);
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

    /** Hide resize outlines when the user taps anywhere except the active resize handle. */
    void clearEditChromeOutsideTouch(float rawX, float rawY) {
        for (int i = editChrome.size() - 1; i >= 0; i--) {
            EditChrome chrome = editChrome.get(i);
            if (chrome.container.getParent() == null) {
                editChrome.remove(i);
                continue;
            }
            boolean onHandle = false;
            for (View handle : chrome.resizeHandles) {
                if (handle == null || handle.getVisibility() != View.VISIBLE) continue;
                int[] location = new int[2]; handle.getLocationOnScreen(location);
                if (rawX >= location[0] && rawX < location[0] + handle.getWidth()
                        && rawY >= location[1] && rawY < location[1] + handle.getHeight()) { onHandle = true; break; }
            }
            if (!onHandle) chrome.clear.run();
        }
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
        final FrameLayout container = new FrameLayout(activity);
        container.setTag("launcher_widget:" + widgetId);
        widget.setTag("launcher_widget_content:" + widgetId);
        widget.setFocusable(true);
        container.addView(widget, new FrameLayout.LayoutParams(-1, -1));
        final View[] resizeHandles = new View[4];
        for (int edge = 0; edge < resizeHandles.length; edge++) {
            View handle = new View(activity);
            handle.setTag("launcher_widget_resize_handle");
            GradientDrawable handleBg = new GradientDrawable();
            handleBg.setColor(Color.rgb(115, 159, 255));
            handleBg.setCornerRadius(LauncherUiUtils.dp(activity, 4));
            handle.setBackground(handleBg);
            handle.setVisibility(View.GONE);
            int side = LauncherUiUtils.dp(activity, 40), thin = LauncherUiUtils.dp(activity, 18);
            FrameLayout.LayoutParams handleLp = edge < 2
                    ? new FrameLayout.LayoutParams(thin, side, edge == 0 ? Gravity.LEFT | Gravity.CENTER_VERTICAL : Gravity.RIGHT | Gravity.CENTER_VERTICAL)
                    : new FrameLayout.LayoutParams(side, thin, edge == 2 ? Gravity.TOP | Gravity.CENTER_HORIZONTAL : Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            container.addView(handle, handleLp);
            resizeHandles[edge] = handle;
        }
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(Math.max(1, width), Math.max(1, height), Gravity.TOP | Gravity.LEFT);
        lp.leftMargin = x;
        lp.topMargin = y;
        // Choose a free rectangle before the widget becomes visible. The post-layout
        // collision pass remains as a safety net, but initial placement must not flash
        // or persist on top of an app, another widget, shortcut, or clock.
        if (page.getWidth() > 0 && page.getHeight() > 0) {
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
                outerScan:
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
                        if (!blocked) {
                            bestX = candidateX;
                            bestY = candidateY;
                            break outerScan;
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
                Toast.makeText(activity, "No free space for this widget", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        page.addView(container, lp);
        container.bringToFront();
        // First placement needs the same collision pass as a completed drag/resize.
        // Post it so the page and widget have measured geometry before bounds are tested.
        page.post(() -> {
            if (container.getParent() == page) listener.resolveWidgetCollisions(page, container);
        });

        // A transparent interaction layer observes long presses across the full widget.
        // Ordinary touch events are forwarded to the AppWidgetHostView so its controls still work.
        final View gestureGrip = new View(activity);
        gestureGrip.setBackground(LauncherUiUtils.roundRect(activity, new int[]{Color.argb(75, 115, 159, 255), Color.argb(25, 115, 159, 255)}, LauncherUiUtils.dp(activity, 8)));
        FrameLayout.LayoutParams gripLp = new FrameLayout.LayoutParams(-1, -1, Gravity.TOP | Gravity.LEFT);
        container.addView(gestureGrip, gripLp);
        for (View handle : resizeHandles) { handle.bringToFront(); handle.setClickable(true); }

        final int[] editMode = {0}; // 0 idle, 1 moving
        final float[] down = {0f, 0f};
        final float[] fingerOffset = {0f, 0f};
        final int[] origin = {x, y, lp.width, lp.height};
        final int[] widgetPage = {pageIndex};
        final int[] originalPageAtDown = {pageIndex};
        final int[] edgeLatch = {0};
        final Runnable[] movePress = new Runnable[1];
        final Runnable[] menuPress = new Runnable[1];
        final Runnable[] clearEditChrome = new Runnable[1];
        final boolean[] childCanceled = {false};
        final boolean[] menuShown = {false};
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
            editMode[0] = 0;
        }));
        movePress[0] = () -> {
            editMode[0] = 1;
            LauncherUiUtils.startMoveHint(container, activity);
            listener.setWorkspaceGridVisible(false, null);
            for (View handle : resizeHandles) handle.setVisibility(View.VISIBLE);
            container.setBackground(LauncherUiUtils.roundRect(activity, new int[]{Color.argb(65, 115, 159, 255), Color.argb(30, 115, 159, 255)}, LauncherUiUtils.dp(activity, 8)));
            gestureGrip.setBackground(LauncherUiUtils.roundRect(activity, new int[]{Color.argb(20, 80, 128, 245), Color.argb(10, 54, 92, 196)}, LauncherUiUtils.dp(activity, 8)));
            Toast.makeText(activity, "Move widget", Toast.LENGTH_SHORT).show();
        };
        menuPress[0] = () -> {
            LauncherUiUtils.stopMoveHint(container);
            menuShown[0] = true;
            cancelWidgetTouch.run();
            editMode[0] = 0;
            handler.removeCallbacks(movePress[0]);
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
                int actualPage = parent instanceof View ? (currentStrip == null ? -1 : currentStrip.indexOfChild((View) parent)) : -1;
                originalPageAtDown[0] = actualPage >= 0 ? actualPage : 0;
                widgetPage[0] = originalPageAtDown[0];
                editMode[0] = 0;
                childCanceled[0] = false;
                menuShown[0] = false;
                handler.removeCallbacks(movePress[0]);
                handler.removeCallbacks(menuPress[0]);
                widget.dispatchTouchEvent(event);
                handler.postDelayed(movePress[0], 550L);
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
                if (editMode[0] == 0 && (Math.abs(dx) > LauncherUiUtils.dp(activity, 10) || Math.abs(dy) > LauncherUiUtils.dp(activity, 10))) {
                    handler.removeCallbacks(movePress[0]);
                    handler.removeCallbacks(menuPress[0]);
                    if (!childCanceled[0]) widget.dispatchTouchEvent(event);
                    return true;
                }
                if (editMode[0] == 1) {
                    LauncherUiUtils.stopMoveHint(container);
                    if (Math.abs(dx) > LauncherUiUtils.dp(activity, 10)
                            || Math.abs(dy) > LauncherUiUtils.dp(activity, 10)) handler.removeCallbacks(menuPress[0]);
                    listener.setWorkspaceGridVisible(false, null);
                    cancelWidgetTouch.run();
                    // Fixed two-screen workspace: widget dragging never changes screens.
                    edgeLatch[0] = 0;
                    LinearLayout strip = listener.getPagesStrip();
                    ViewParent currentParent = container.getParent();
                    int sourceIndex = strip == null || !(currentParent instanceof View)
                            ? widgetPage[0] : strip.indexOfChild((View) currentParent);
                    if (sourceIndex < 0) sourceIndex = widgetPage[0];
                    FrameLayout activePage = container.getParent() instanceof FrameLayout
                            ? (FrameLayout) container.getParent() : page;
                    FrameLayout.LayoutParams current = (FrameLayout.LayoutParams) container.getLayoutParams();
                    int[] pageLocation = new int[2];
                    activePage.getLocationOnScreen(pageLocation);
                    int maxX = Math.max(0, activePage.getWidth() - container.getWidth());
                    int maxY = Math.max(0, activePage.getHeight() - container.getHeight());
                    // Store coordinates relative to the still-attached source page, not the viewport.
                    // Remove the temporary cross-page translation before calculating layout margins.
                    current.leftMargin = Math.max(0, Math.min(maxX,
                            Math.round(event.getRawX() - pageLocation[0] - fingerOffset[0])));
                    current.topMargin = Math.max(0, Math.min(maxY,
                            Math.round(event.getRawY() - pageLocation[1] - fingerOffset[1])));
                    container.setLayoutParams(current);
                    listener.updateLiveDisplacements(activePage, container);
                    return true;
                }
                if (!childCanceled[0]) widget.dispatchTouchEvent(event);
                return true;
            }
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                LauncherUiUtils.stopMoveHint(container);
                handler.removeCallbacks(movePress[0]);
                handler.removeCallbacks(menuPress[0]);
                if (editMode[0] == 1) {
                    cancelWidgetTouch.run();
                    LinearLayout strip = listener.getPagesStrip();
                    FrameLayout sourcePage = container.getParent() instanceof FrameLayout
                            ? (FrameLayout) container.getParent() : page;
                    // A widget can be repositioned on its own screen, but never transferred
                    // to the other screen by dragging.
                    int destinationPage = originalPageAtDown[0];
                    listener.ensurePageExists(destinationPage);
                    FrameLayout targetPage = strip != null && destinationPage < strip.getChildCount()
                            && strip.getChildAt(destinationPage) instanceof FrameLayout
                            ? (FrameLayout) strip.getChildAt(destinationPage) : sourcePage;
                    FrameLayout.LayoutParams current = (FrameLayout.LayoutParams) container.getLayoutParams();
                    if (action == MotionEvent.ACTION_CANCEL) {
                        listener.finishLiveDisplacements(false, container);
                        current.leftMargin = origin[0];
                        current.topMargin = origin[1];
                    } else {
                        // Raw touch coordinates must be converted using the destination
                        // page itself. The viewport origin is not the page origin: on the
                        // second screen its horizontal offset differs, and using it causes
                        // drops near the top/edges to resolve against the wrong coordinates.
                        int[] targetPageLocation = new int[2];
                        targetPage.getLocationOnScreen(targetPageLocation);
                        FrameLayout.LayoutParams dropParams = (FrameLayout.LayoutParams) container.getLayoutParams();
                        int dropWidth = Math.max(1, dropParams.width > 0 ? dropParams.width : container.getWidth());
                        int dropHeight = Math.max(1, dropParams.height > 0 ? dropParams.height : container.getHeight());
                        int maxX = Math.max(0, targetPage.getWidth() - dropWidth);
                        int maxY = Math.max(0, targetPage.getHeight() - dropHeight);
                        // Preserve the exact finger-to-widget grab offset captured at
                        // ACTION_DOWN. Centering the widget under the finger on release made
                        // it jump even though the live drag itself tracked correctly.
                        // Collision resolution below may move it only when space is occupied.
                        int requestedX = Math.max(0, Math.min(maxX,
                                Math.round(event.getRawX() - targetPageLocation[0] - fingerOffset[0])));
                        int requestedY = Math.max(0, Math.min(maxY,
                                Math.round(event.getRawY() - targetPageLocation[1] - fingerOffset[1])));
                        // Exact obstacle-edge candidates avoid missing narrow free pockets
                        // and are cheaper than scanning the entire page at a fixed interval.
                        int bestX = -1, bestY = -1;
                        long bestDistance = Long.MAX_VALUE;
                        java.util.ArrayList<android.graphics.Rect> occupied =
                                new java.util.ArrayList<android.graphics.Rect>();
                        for (int ci = 0; ci < targetPage.getChildCount(); ci++) {
                            View other = targetPage.getChildAt(ci);
                            if (other == container || !(other.getTag() instanceof String)) continue;
                            String tag = (String) other.getTag();
                            if (!(tag.startsWith("launcher_widget:")
                                    || tag.startsWith("launcher_app:")
                                    || "launcher_contact_shortcut".equals(tag)
                                    || "launcher_clock_widget".equals(tag))) continue;
                            android.graphics.Rect bounds = workspaceBounds(other);
                            if (!bounds.isEmpty()) occupied.add(bounds);
                        }
                        java.util.TreeSet<Integer> xCandidates = new java.util.TreeSet<Integer>();
                        java.util.TreeSet<Integer> yCandidates = new java.util.TreeSet<Integer>();
                        xCandidates.add(requestedX);
                        yCandidates.add(requestedY);
                        xCandidates.add(0);
                        yCandidates.add(0);
                        xCandidates.add(maxX);
                        yCandidates.add(maxY);
                        for (android.graphics.Rect obstacle : occupied) {
                            xCandidates.add(Math.max(0, Math.min(maxX, obstacle.left - dropWidth)));
                            xCandidates.add(Math.max(0, Math.min(maxX, obstacle.right)));
                            yCandidates.add(Math.max(0, Math.min(maxY, obstacle.top - dropHeight)));
                            yCandidates.add(Math.max(0, Math.min(maxY, obstacle.bottom)));
                        }
                        for (Integer yValue : yCandidates) {
                            int candidateY = yValue.intValue();
                            for (Integer xValue : xCandidates) {
                                int candidateX = xValue.intValue();
                                android.graphics.Rect candidate = new android.graphics.Rect(
                                        candidateX, candidateY,
                                        candidateX + dropWidth, candidateY + dropHeight);
                                boolean blocked = false;
                                for (android.graphics.Rect obstacle : occupied) {
                                    if (android.graphics.Rect.intersects(candidate, obstacle)) {
                                        blocked = true;
                                        break;
                                    }
                                }
                                if (blocked) continue;
                                long distance = Math.abs((long) candidateX - requestedX)
                                        + Math.abs((long) candidateY - requestedY);
                                if (distance < bestDistance) {
                                    bestDistance = distance;
                                    bestX = candidateX;
                                    bestY = candidateY;
                                }
                            }
                        }
                        if (bestX >= 0 && bestY >= 0) {
                            current.leftMargin = bestX;
                            current.topMargin = bestY;
                        } else {
                            current.leftMargin = Math.min(maxX, origin[0]);
                            current.topMargin = Math.min(maxY, origin[1]);
                            Toast.makeText(activity, "No non-overlapping space for this widget", Toast.LENGTH_SHORT).show();
                        }
                    }
                    container.setTranslationX(0f);
                    container.setLayoutParams(current);
                    clearEditChrome[0].run();
                    if (sourcePage != targetPage) {
                        reparenting[0] = true;
                        sourcePage.removeView(container);
                        targetPage.addView(container, current);
                        reparenting[0] = false;
                    }
                    widgetPage[0] = destinationPage;
                    listener.saveWidgetGeometry(widgetId, destinationPage, container);
                    // Resolve the final widget position first. If collision handling
                    // relocates it, the displacement transaction can then restore
                    // neighbours instead of permanently leaving them in temporary slots.
                    listener.resolveWidgetCollisions(targetPage, container);
                    if (action == MotionEvent.ACTION_UP) listener.finishLiveDisplacements(true, container);
                    if (action == MotionEvent.ACTION_UP) listener.renderHomePages(false);
                } else if (!childCanceled[0]) {
                    widget.dispatchTouchEvent(event);
                }
                editMode[0] = 0;
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
                    android.graphics.Rect candidate = new android.graphics.Rect(left, top, left + resizeWidth, top + resizeHeight);
                    boolean overlap = false;
                    for (int i = 0; i < activePage.getChildCount(); i++) {
                        View other = activePage.getChildAt(i);
                        if (other == container || !(other.getTag() instanceof String)) continue;
                        String tag = (String) other.getTag();
                        if (!(tag.startsWith("launcher_widget:") || tag.startsWith("launcher_app:")
                                || "launcher_contact_shortcut".equals(tag) || "launcher_clock_widget".equals(tag))) continue;
                        if (android.graphics.Rect.intersects(candidate, workspaceBounds(other))) { overlap = true; break; }
                    }
                    if (!overlap) {
                        current.leftMargin = left; current.topMargin = top; current.width = resizeWidth; current.height = resizeHeight;
                        container.setLayoutParams(current);
                    }
                    listener.setWorkspaceGridVisible(false, null);
                    return true;
                }
                if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                    listener.setWorkspaceGridVisible(false, null);
                    FrameLayout.LayoutParams finalParams = (FrameLayout.LayoutParams) container.getLayoutParams();
                    if (action == MotionEvent.ACTION_CANCEL) {
                        finalParams.leftMargin = origin[0]; finalParams.topMargin = origin[1];
                        finalParams.width = origin[2]; finalParams.height = origin[3]; container.setLayoutParams(finalParams);
                    }
                    if (container.getChildAt(0) instanceof AppWidgetHostView) {
                        WidgetController.updateWidgetSize(android.appwidget.AppWidgetManager.getInstance(activity),
                                widgetId, (AppWidgetHostView) container.getChildAt(0),
                                activity.getResources().getDisplayMetrics().density, finalParams.width, finalParams.height);
                    }
                    listener.saveWidgetGeometry(widgetId, widgetPage[0], container);
                    if (action == MotionEvent.ACTION_UP && container.getParent() instanceof FrameLayout)
                        listener.resolveWidgetCollisions((FrameLayout) container.getParent(), container);
                    return true;
                }
                return true;
            });
        }
    
    }
}
