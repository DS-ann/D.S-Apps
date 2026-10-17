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
        final View resizeHandle;
        final Runnable clear;
        EditChrome(View container, View resizeHandle, Runnable clear) {
            this.container = container;
            this.resizeHandle = resizeHandle;
            this.clear = clear;
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
            if (chrome.resizeHandle.getVisibility() != View.VISIBLE) continue;
            int[] location = new int[2];
            chrome.resizeHandle.getLocationOnScreen(location);
            boolean onHandle = rawX >= location[0] && rawX < location[0] + chrome.resizeHandle.getWidth()
                    && rawY >= location[1] && rawY < location[1] + chrome.resizeHandle.getHeight();
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
        final View resizeHandle = new View(activity);
        resizeHandle.setTag("launcher_widget_resize_handle");
        GradientDrawable handleBg = new GradientDrawable();
        handleBg.setColor(Color.rgb(115, 159, 255));
        handleBg.setCornerRadius(LauncherUiUtils.dp(activity, 5));
        resizeHandle.setBackground(handleBg);
        resizeHandle.setVisibility(View.GONE);
        FrameLayout.LayoutParams handleLp = new FrameLayout.LayoutParams(LauncherUiUtils.dp(activity, 26), LauncherUiUtils.dp(activity, 26), Gravity.RIGHT | Gravity.BOTTOM);
        container.addView(resizeHandle, handleLp);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(Math.max(1, width), Math.max(1, height), Gravity.TOP | Gravity.LEFT);
        lp.leftMargin = x;
        lp.topMargin = y;
        page.addView(container, lp);
        container.bringToFront();

        // A transparent interaction layer observes long presses across the full widget.
        // Ordinary touch events are forwarded to the AppWidgetHostView so its controls still work.
        final View gestureGrip = new View(activity);
        gestureGrip.setBackground(LauncherUiUtils.roundRect(activity, new int[]{Color.argb(75, 115, 159, 255), Color.argb(25, 115, 159, 255)}, LauncherUiUtils.dp(activity, 8)));
        FrameLayout.LayoutParams gripLp = new FrameLayout.LayoutParams(-1, -1, Gravity.TOP | Gravity.LEFT);
        container.addView(gestureGrip, gripLp);
        resizeHandle.bringToFront();

        final int[] editMode = {0}; // 0 idle, 1 moving
        final float[] down = {0f, 0f};
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
            resizeHandle.setVisibility(View.GONE);
            container.setBackground(null);
            gestureGrip.setBackgroundColor(Color.TRANSPARENT);
        };
        editChrome.add(new EditChrome(container, resizeHandle, () -> {
            clearEditChrome[0].run();
            editMode[0] = 0;
        }));
        movePress[0] = () -> {
            editMode[0] = 1;
            LauncherUiUtils.startMoveHint(container, activity);
            listener.setWorkspaceGridVisible(false, null);
            resizeHandle.setVisibility(View.VISIBLE);
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
                    resizeHandle.setVisibility(View.VISIBLE);
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
                            Math.round(event.getRawX() - pageLocation[0] - container.getTranslationX() - container.getWidth() / 2f)));
                    current.topMargin = Math.max(0, Math.min(maxY,
                            Math.round(event.getRawY() - pageLocation[1] - container.getHeight() / 2f)));
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
                        int[] viewportLocation = new int[2];
                        View viewport = strip == null ? null : (View) strip.getParent();
                        if (viewport != null) viewport.getLocationOnScreen(viewportLocation);
                        int maxX = Math.max(0, targetPage.getWidth() - container.getWidth());
                        int maxY = Math.max(0, targetPage.getHeight() - container.getHeight());
                        int requestedX = Math.max(0, Math.min(maxX,
                                Math.round(event.getRawX() - viewportLocation[0] - container.getWidth() / 2f)));
                        int requestedY = Math.max(0, Math.min(maxY,
                                Math.round(event.getRawY() - viewportLocation[1] - container.getHeight() / 2f)));
                        // Widgets use free positioning, not app-icon grid snapping.
                        // Keep the requested point when clear; otherwise find the nearest
                        // clear point in small increments so widgets never overlap items.
                        int step = Math.max(1, LauncherUiUtils.dp(activity, 8));
                        int bestX = -1, bestY = -1, bestDistance = Integer.MAX_VALUE;
                        for (int gy = Math.max(0, requestedY - container.getHeight());
                                gy <= Math.min(maxY, requestedY + container.getHeight()); gy += step) {
                            for (int gx = Math.max(0, requestedX - container.getWidth());
                                    gx <= Math.min(maxX, requestedX + container.getWidth()); gx += step) {
                                boolean blocked = false;
                                android.graphics.Rect candidate = new android.graphics.Rect(
                                        gx, gy, gx + container.getWidth(), gy + container.getHeight());
                                for (int ci = 0; ci < targetPage.getChildCount(); ci++) {
                                    View other = targetPage.getChildAt(ci);
                                    if (other == container || !(other.getTag() instanceof String)) continue;
                                    String tag = (String) other.getTag();
                                    if (!(tag.startsWith("launcher_widget:")
                                            || tag.startsWith("launcher_app:")
                                            || "launcher_contact_shortcut".equals(tag)
                                            || "launcher_clock_widget".equals(tag))) continue;
                                    FrameLayout.LayoutParams op = other.getLayoutParams() instanceof FrameLayout.LayoutParams
                                            ? (FrameLayout.LayoutParams) other.getLayoutParams() : null;
                                    int ox = op == null ? other.getLeft() : op.leftMargin;
                                    int oy = op == null ? other.getTop() : op.topMargin;
                                    int ow = op != null && op.width > 0 ? op.width : other.getWidth();
                                    int oh = op != null && op.height > 0 ? op.height : other.getHeight();
                                    if (ow > 0 && oh > 0 && android.graphics.Rect.intersects(
                                            candidate, new android.graphics.Rect(ox, oy, ox + ow, oy + oh))) {
                                        blocked = true;
                                        break;
                                    }
                                }
                                if (blocked) continue;
                                int distance = Math.abs(gx - requestedX) + Math.abs(gy - requestedY);
                                if (distance < bestDistance) {
                                    bestDistance = distance;
                                    bestX = gx;
                                    bestY = gy;
                                }
                            }
                        }
                        if (bestDistance != Integer.MAX_VALUE) {
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
                    if (action == MotionEvent.ACTION_UP) listener.finishLiveDisplacements(true, container);
                    listener.saveWidgetGeometry(widgetId, destinationPage, container);
                    listener.resolveWidgetCollisions(targetPage, container);
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
        resizeHandle.setOnTouchListener((v, event) -> {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                listener.setWorkspaceGridVisible(false, null);
                down[0] = event.getRawX(); down[1] = event.getRawY();
                FrameLayout.LayoutParams current = (FrameLayout.LayoutParams) container.getLayoutParams();
                origin[2] = current.width; origin[3] = current.height;
                return true;
            }
            if (action == MotionEvent.ACTION_MOVE) {
                FrameLayout activePage = container.getParent() instanceof FrameLayout
                        ? (FrameLayout) container.getParent() : page;
                FrameLayout.LayoutParams current = (FrameLayout.LayoutParams) container.getLayoutParams();
                int previousWidth = current.width;
                int previousHeight = current.height;
                // Clamp the minimum size to the space actually available. On narrow
                // Android 6 phones, forcing a 140dp/90dp minimum after the max clamp
                // can push the widget outside the workspace.
                int availableWidth = Math.max(1, activePage.getWidth() - current.leftMargin);
                int availableHeight = Math.max(1, activePage.getHeight() - current.topMargin);
                int minimumWidth = Math.min(LauncherUiUtils.dp(activity, 140), availableWidth);
                int minimumHeight = Math.min(LauncherUiUtils.dp(activity, 90), availableHeight);
                current.width = Math.max(minimumWidth, Math.min(availableWidth,
                        origin[2] + Math.round(event.getRawX() - down[0])));
                current.height = Math.max(minimumHeight, Math.min(availableHeight,
                        origin[3] + Math.round(event.getRawY() - down[1])));
                // Resizing may not grow through another widget. Reject the proposed
                // frame rather than allowing overlapping AppWidgetHostViews.
                boolean overlapsWidget = false;
                int right = current.leftMargin + current.width;
                int bottom = current.topMargin + current.height;
                for (int i = 0; i < activePage.getChildCount(); i++) {
                    View other = activePage.getChildAt(i);
                    if (other == container || !(other.getTag() instanceof String)
                            || !((String) other.getTag()).startsWith("launcher_widget:")) continue;
                    FrameLayout.LayoutParams otherParams =
                            (FrameLayout.LayoutParams) other.getLayoutParams();
                    int otherRight = otherParams.leftMargin + other.getWidth();
                    int otherBottom = otherParams.topMargin + other.getHeight();
                    if (current.leftMargin < otherRight && right > otherParams.leftMargin
                            && current.topMargin < otherBottom && bottom > otherParams.topMargin) {
                        overlapsWidget = true;
                        break;
                    }
                }
                if (overlapsWidget) {
                    current.width = previousWidth;
                    current.height = previousHeight;
                }
                container.setLayoutParams(current);
                listener.setWorkspaceGridVisible(false, null);
                return true;
            }
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                listener.setWorkspaceGridVisible(false, null);
                FrameLayout.LayoutParams finalParams =
                        (FrameLayout.LayoutParams) container.getLayoutParams();
                if (action == MotionEvent.ACTION_CANCEL) {
                    // A cancelled resize must not commit a partial size. Restore the
                    // dimensions captured at ACTION_DOWN, just as a cancelled move
                    // restores its original position.
                    finalParams.width = origin[2];
                    finalParams.height = origin[3];
                    container.setLayoutParams(finalParams);
                }
                if (container.getChildAt(0) instanceof AppWidgetHostView) {
                    WidgetController.updateWidgetSize(android.appwidget.AppWidgetManager.getInstance(activity),
                            widgetId, (AppWidgetHostView) container.getChildAt(0),
                            activity.getResources().getDisplayMetrics().density,
                            finalParams.width, finalParams.height);
                }
                listener.saveWidgetGeometry(widgetId, widgetPage[0], container);
                if (action == MotionEvent.ACTION_UP && container.getParent() instanceof FrameLayout) {
                    listener.resolveWidgetCollisions((FrameLayout) container.getParent(), container);
                }
                return true;
            }
            return true;
        });
    
    }
}
