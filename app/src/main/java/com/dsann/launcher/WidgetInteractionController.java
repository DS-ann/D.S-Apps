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
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
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
        LinearLayout getPagesStrip();
    }

    private final Activity activity;
    private final Handler handler;
    private final AppWidgetHost widgetHost;
    private final SharedPreferences prefs;
    private final String widgetIdsKey;
    private final Listener listener;

    WidgetInteractionController(Activity activity, Handler handler, AppWidgetHost widgetHost,
                                SharedPreferences prefs, String widgetIdsKey, Listener listener) {
        this.activity = activity;
        this.handler = handler;
        this.widgetHost = widgetHost;
        this.prefs = prefs;
        this.widgetIdsKey = widgetIdsKey;
        this.listener = listener;
    }

    void addWidgetContainer(FrameLayout page, AppWidgetHostView widget, int widgetId, int pageIndex,
                            int x, int y, int width, int height) {
        final FrameLayout container = new FrameLayout(activity);
        container.setTag("launcher_widget:" + widgetId);
        widget.setTag("launcher_widget_content:" + widgetId);
        widget.setFocusable(true);
        container.addView(widget, new FrameLayout.LayoutParams(-1, -1));
        final View resizeHandle = new View(activity);
        GradientDrawable handleBg = new GradientDrawable();
        handleBg.setColor(Color.rgb(115, 159, 255));
        handleBg.setCornerRadius(LauncherUiUtils.dp(activity, 5));
        resizeHandle.setBackground(handleBg);
        resizeHandle.setVisibility(View.GONE);
        FrameLayout.LayoutParams handleLp = new FrameLayout.LayoutParams(LauncherUiUtils.dp(activity, 26), LauncherUiUtils.dp(activity, 26), Gravity.RIGHT | Gravity.BOTTOM);
        container.addView(resizeHandle, handleLp);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(Math.max(LauncherUiUtils.dp(activity, 140), width), Math.max(LauncherUiUtils.dp(activity, 90), height), Gravity.TOP | Gravity.LEFT);
        lp.leftMargin = x;
        lp.topMargin = y;
        page.addView(container, lp);
        container.bringToFront();

        // A slim grip at the top of each widget owns launcher edit gestures,
        // leaving the widget's normal content fully interactive.
        final View gestureGrip = new View(activity);
        gestureGrip.setBackground(LauncherUiUtils.roundRect(activity, new int[]{Color.argb(75, 115, 159, 255), Color.argb(25, 115, 159, 255)}, LauncherUiUtils.dp(activity, 8)));
        FrameLayout.LayoutParams gripLp = new FrameLayout.LayoutParams(-1, LauncherUiUtils.dp(activity, 12), Gravity.TOP);
        container.addView(gestureGrip, gripLp);

        final int[] editMode = {0}; // 0 idle, 1 moving
        final float[] down = {0f, 0f};
        final int[] origin = {x, y, lp.width, lp.height};
        final int[] widgetPage = {pageIndex};
        final Runnable[] movePress = new Runnable[1];
        final Runnable[] menuPress = new Runnable[1];
        Runnable clearEditChrome = () -> {
            resizeHandle.setVisibility(View.GONE);
            container.setBackground(null);
            gestureGrip.setBackground(LauncherUiUtils.roundRect(activity, new int[]{Color.argb(75, 115, 159, 255), Color.argb(25, 115, 159, 255)}, LauncherUiUtils.dp(activity, 8)));
        };
        movePress[0] = () -> {
            editMode[0] = 1;
            resizeHandle.setVisibility(View.VISIBLE);
            container.setBackground(LauncherUiUtils.roundRect(activity, new int[]{Color.argb(65, 115, 159, 255), Color.argb(30, 115, 159, 255)}, LauncherUiUtils.dp(activity, 8)));
            gestureGrip.setBackground(LauncherUiUtils.roundRect(activity, new int[]{Color.rgb(80, 128, 245), Color.rgb(54, 92, 196)}, LauncherUiUtils.dp(activity, 8)));
            Toast.makeText(activity, "Move widget", Toast.LENGTH_SHORT).show();
        };
        menuPress[0] = () -> {
            editMode[0] = 0;
            handler.removeCallbacks(movePress[0]);
            clearEditChrome.run();
            String[] actions = {"Remove widget", "Resize widget", "Move to next page"};
            String[] icons = {"×", "↘", "→"};
            listener.showActionDialog("Widget options", icons, actions, (dialog, which) -> {
                if (which == 0) {
                    if (container.getParent() instanceof ViewGroup) ((ViewGroup) container.getParent()).removeView(container);
                    WidgetController.removeWidget(prefs, widgetIdsKey, widgetId);
                    try { widgetHost.deleteAppWidgetId(widgetId); } catch (Exception ignored) {}
                    listener.renderHomePages(false);
                } else if (which == 1) {
                    resizeHandle.setVisibility(View.VISIBLE);
                    container.setBackground(LauncherUiUtils.roundRect(activity, new int[]{Color.argb(65, 115, 159, 255), Color.argb(30, 115, 159, 255)}, LauncherUiUtils.dp(activity, 8)));
                    Toast.makeText(activity, "Drag the blue corner handle to resize", Toast.LENGTH_LONG).show();
                } else if (which == 2) {
                    int destination = widgetPage[0] + 1;
                    // Persist the destination first, then rebuild the page strip. The
                    // destination view may not exist yet when the widget is on the last page.
                    FrameLayout.LayoutParams moved = (FrameLayout.LayoutParams) container.getLayoutParams();
                    moved.leftMargin = LauncherUiUtils.dp(activity, 8);
                    moved.topMargin = LauncherUiUtils.dp(activity, 40);
                    container.setLayoutParams(moved);
                    widgetPage[0] = destination;
                    listener.ensurePageExists(destination);
                    listener.saveWidgetGeometry(widgetId, destination, container);
                    listener.setCurrentPage(destination);
                    listener.renderHomePages(true);
                    Toast.makeText(activity, "Widget moved to next page", Toast.LENGTH_SHORT).show();
                }
            });
        };
        gestureGrip.setOnTouchListener((view, event) -> {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                down[0] = event.getRawX();
                down[1] = event.getRawY();
                FrameLayout.LayoutParams current = (FrameLayout.LayoutParams) container.getLayoutParams();
                origin[0] = current.leftMargin;
                origin[1] = current.topMargin;
                editMode[0] = 0;
                handler.removeCallbacks(movePress[0]);
                handler.removeCallbacks(menuPress[0]);
                handler.postDelayed(movePress[0], 1000L);
                handler.postDelayed(menuPress[0], 2000L);
                return true;
            }
            if (action == MotionEvent.ACTION_MOVE) {
                float dx = event.getRawX() - down[0];
                float dy = event.getRawY() - down[1];
                if (editMode[0] == 0 && (Math.abs(dx) > LauncherUiUtils.dp(activity, 10) || Math.abs(dy) > LauncherUiUtils.dp(activity, 10))) {
                    handler.removeCallbacks(movePress[0]);
                    handler.removeCallbacks(menuPress[0]);
                    return true;
                }
                if (editMode[0] == 1) {
                    handler.removeCallbacks(menuPress[0]);
                    FrameLayout.LayoutParams current = (FrameLayout.LayoutParams) container.getLayoutParams();
                    current.leftMargin = Math.max(0, Math.min(page.getWidth() - container.getWidth(), origin[0] + Math.round(dx)));
                    current.topMargin = Math.max(0, Math.min(page.getHeight() - container.getHeight(), origin[1] + Math.round(dy)));
                    container.setLayoutParams(current);
                }
                return true;
            }
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                handler.removeCallbacks(movePress[0]);
                handler.removeCallbacks(menuPress[0]);
                if (editMode[0] == 1) {
                    listener.saveWidgetGeometry(widgetId, widgetPage[0], container);
                    clearEditChrome.run();
                }
                editMode[0] = 0;
                return true;
            }
            return true;
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
                current.width = Math.max(LauncherUiUtils.dp(activity, 140), Math.min(page.getWidth() - current.leftMargin,
                        origin[2] + Math.round(event.getRawX() - down[0])));
                current.height = Math.max(LauncherUiUtils.dp(activity, 90), Math.min(page.getHeight() - current.topMargin,
                        origin[3] + Math.round(event.getRawY() - down[1])));
                container.setLayoutParams(current);
                return true;
            }
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                if (container.getChildAt(0) instanceof AppWidgetHostView) {
                    WidgetController.updateWidgetSize(android.appwidget.AppWidgetManager.getInstance(activity),
                            widgetId, (AppWidgetHostView) container.getChildAt(0),
                            activity.getResources().getDisplayMetrics().density,
                            container.getLayoutParams().width, container.getLayoutParams().height);
                }
                listener.saveWidgetGeometry(widgetId, widgetPage[0], container);
                return true;
            }
            return true;
        });
    
    }
}
