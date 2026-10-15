package com.dsann.launcher;

import android.app.Activity;
import android.view.View;
import android.widget.LinearLayout;

/** Owns the home/drawer transition choreography independently of screen construction. */
final class LauncherScreenTransitions {
    private LauncherScreenTransitions() {}

    static void showHome(Activity activity, LinearLayout homeScreen, LinearLayout drawerScreen,
                         Runnable markDrawerClosed) {
        markDrawerClosed.run();
        drawerScreen.animate().cancel();
        homeScreen.animate().cancel();
        homeScreen.setVisibility(View.VISIBLE);
        homeScreen.setAlpha(0.82f);
        homeScreen.setScaleX(0.96f);
        homeScreen.setScaleY(0.96f);
        homeScreen.setTranslationY(LauncherUiUtils.dp(activity, 24));
        drawerScreen.animate().alpha(0f).translationY(LauncherUiUtils.dp(activity, 40))
                .scaleX(0.96f).scaleY(0.96f).rotationX(-7f).setDuration(LauncherMotion.duration(activity, 260L))
                .withEndAction(() -> drawerScreen.setVisibility(View.GONE)).start();
        homeScreen.animate().alpha(1f).translationY(0f).scaleX(1f).scaleY(1f)
                .setDuration(LauncherMotion.duration(activity, 340L)).start();
    }

    static void showDrawer(Activity activity, LinearLayout homeScreen, LinearLayout drawerScreen,
                           Runnable beforeOpen) {
        beforeOpen.run();
        homeScreen.animate().cancel();
        drawerScreen.animate().cancel();
        drawerScreen.setVisibility(View.VISIBLE);
        drawerScreen.setAlpha(0f);
        drawerScreen.setScaleX(0.92f);
        drawerScreen.setScaleY(0.92f);
        drawerScreen.setTranslationY(activity.getResources().getDisplayMetrics().heightPixels * 0.16f);
        drawerScreen.setRotationX(10f);
        drawerScreen.setPivotY(0f);
        homeScreen.animate().alpha(0f).scaleX(0.94f).scaleY(0.94f)
                .translationY(-LauncherUiUtils.dp(activity, 22))
                .setDuration(LauncherMotion.duration(activity, 250L)).withEndAction(() -> homeScreen.setVisibility(View.GONE)).start();
        drawerScreen.animate().alpha(1f).scaleX(1f).scaleY(1f).translationY(0f).rotationX(0f)
                .setDuration(LauncherMotion.duration(activity, 420L)).start();
    }
}
