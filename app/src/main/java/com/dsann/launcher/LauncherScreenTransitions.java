package com.dsann.launcher;

import android.app.Activity;
import android.view.View;
import android.widget.LinearLayout;

/** Owns the home/drawer transition choreography independently of screen construction. */
final class LauncherScreenTransitions {
    private LauncherScreenTransitions() {}

    // 0 = original transition, 1 = 3D Card Flip, 2 = Depth Slide, 3 = Rotating Carousel.
    private static void prepareTransitionLayers(LinearLayout homeScreen, LinearLayout drawerScreen) {
        // Cache both surfaces during transforms to avoid display-list rebuild flashes.
        if (homeScreen != null) homeScreen.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        if (drawerScreen != null) drawerScreen.setLayerType(View.LAYER_TYPE_HARDWARE, null);
    }

    private static void releaseTransitionLayer(View view) {
        if (view != null) view.setLayerType(View.LAYER_TYPE_NONE, null);
    }

    static void showHome(Activity activity, LinearLayout homeScreen, LinearLayout drawerScreen,
                         Runnable markDrawerClosed, int effect) {
        markDrawerClosed.run();
        drawerScreen.animate().cancel();
        homeScreen.animate().cancel();
        prepareTransitionLayers(homeScreen, drawerScreen);
        float density = activity.getResources().getDisplayMetrics().density;
        float width = Math.max(1, activity.getResources().getDisplayMetrics().widthPixels);
        float height = Math.max(1, activity.getResources().getDisplayMetrics().heightPixels);
        homeScreen.setCameraDistance(10000f * density);
        drawerScreen.setCameraDistance(10000f * density);
        homeScreen.setVisibility(View.VISIBLE);

        // Clear transforms left by a previous effect before setting this effect's start pose.
        homeScreen.setRotationX(0f);
        homeScreen.setRotationY(0f);
        homeScreen.setTranslationX(0f);
        homeScreen.setTranslationZ(0f);
        drawerScreen.setRotationX(0f);
        drawerScreen.setRotationY(0f);
        drawerScreen.setTranslationX(0f);
        drawerScreen.setTranslationZ(0f);

        if (effect == 1) {
            drawerScreen.setAlpha(1f);
            drawerScreen.setScaleX(1f);
            drawerScreen.setScaleY(1f);
            drawerScreen.setPivotX(width * 0.5f);
            drawerScreen.setPivotY(height * 0.5f);
            homeScreen.setAlpha(0f);
            homeScreen.setScaleX(0.94f);
            homeScreen.setScaleY(0.94f);
            homeScreen.setPivotX(width * 0.5f);
            homeScreen.setPivotY(height * 0.5f);
            homeScreen.setRotationY(-13f);
            drawerScreen.animate().alpha(0f).rotationY(18f).scaleX(0.96f).scaleY(0.96f)
                    .setDuration(LauncherMotion.duration(activity, 330L))
                    .withEndAction(() -> {
                        drawerScreen.setVisibility(View.GONE);
                        drawerScreen.setRotationY(0f);
                        releaseTransitionLayer(drawerScreen);
                    }).start();
            homeScreen.animate().alpha(1f).rotationY(0f).scaleX(1f).scaleY(1f)
                    .setDuration(LauncherMotion.duration(activity, 360L))
                    .withEndAction(() -> releaseTransitionLayer(homeScreen)).start();
        } else if (effect == 2) {
            homeScreen.setAlpha(0f);
            homeScreen.setScaleX(0.90f);
            homeScreen.setScaleY(0.90f);
            homeScreen.setTranslationY(-LauncherUiUtils.dp(activity, 18));
            homeScreen.setTranslationZ(-density * 36f);
            drawerScreen.animate().alpha(0f).translationY(LauncherUiUtils.dp(activity, 34))
                    .translationZ(-density * 40f).scaleX(0.92f).scaleY(0.92f)
                    .setDuration(LauncherMotion.duration(activity, 280L))
                    .withEndAction(() -> {
                        drawerScreen.setVisibility(View.GONE);
                        drawerScreen.setTranslationZ(0f);
                        releaseTransitionLayer(drawerScreen);
                    }).start();
            homeScreen.animate().alpha(1f).translationY(0f).translationZ(0f).scaleX(1f).scaleY(1f)
                    .setDuration(LauncherMotion.duration(activity, 360L))
                    .withEndAction(() -> releaseTransitionLayer(homeScreen)).start();
        } else if (effect == 3) {
            drawerScreen.setPivotX(width * 0.5f);
            drawerScreen.setPivotY(height * 0.5f);
            homeScreen.setPivotX(width * 0.5f);
            homeScreen.setPivotY(height * 0.5f);
            homeScreen.setAlpha(0f);
            homeScreen.setScaleX(0.92f);
            homeScreen.setScaleY(0.92f);
            homeScreen.setRotationY(-34f);
            homeScreen.setTranslationX(-width * 0.12f);
            drawerScreen.animate().alpha(0f).rotationY(62f).translationX(width * 0.18f)
                    .scaleX(0.94f).scaleY(0.94f)
                    .setDuration(LauncherMotion.duration(activity, 360L))
                    .withEndAction(() -> {
                        drawerScreen.setVisibility(View.GONE);
                        drawerScreen.setRotationY(0f);
                        drawerScreen.setTranslationX(0f);
                        releaseTransitionLayer(drawerScreen);
                    }).start();
            homeScreen.animate().alpha(1f).rotationY(0f).translationX(0f).scaleX(1f).scaleY(1f)
                    .setDuration(LauncherMotion.duration(activity, 390L))
                    .withEndAction(() -> releaseTransitionLayer(homeScreen)).start();
        } else {
            // Original transition retained as the default and selectable option.
            homeScreen.setAlpha(0.82f);
            homeScreen.setScaleX(0.96f);
            homeScreen.setScaleY(0.96f);
            homeScreen.setTranslationY(LauncherUiUtils.dp(activity, 24));
            drawerScreen.animate().alpha(0f).translationY(LauncherUiUtils.dp(activity, 40))
                    .scaleX(0.96f).scaleY(0.96f).rotationX(-7f)
                    .setDuration(LauncherMotion.duration(activity, 260L))
                    .withEndAction(() -> {
                        drawerScreen.setVisibility(View.GONE);
                        releaseTransitionLayer(drawerScreen);
                    }).start();
            homeScreen.animate().alpha(1f).translationY(0f).scaleX(1f).scaleY(1f)
                    .setDuration(LauncherMotion.duration(activity, 340L))
                    .withEndAction(() -> releaseTransitionLayer(homeScreen)).start();
        }
    }

    static void updateDrawerSwipe(Activity activity, LinearLayout homeScreen,
                                   LinearLayout drawerScreen, int effect, float swipeProgress) {
        float progress = Math.max(0f, Math.min(1f, swipeProgress));
        float density = activity.getResources().getDisplayMetrics().density;
        float width = Math.max(1, activity.getResources().getDisplayMetrics().widthPixels);
        float height = Math.max(1, activity.getResources().getDisplayMetrics().heightPixels);
        homeScreen.setVisibility(View.VISIBLE);
        drawerScreen.setVisibility(View.VISIBLE);
        homeScreen.setCameraDistance(10000f * density);
        drawerScreen.setCameraDistance(10000f * density);

        // These poses are updated on every ACTION_MOVE: the drawer and home
        // surface now track the finger rather than merely inheriting progress
        // after release. Each selectable effect has a matching interactive pose.
        if (effect == 1) {
            homeScreen.setPivotX(width * 0.5f);
            homeScreen.setPivotY(height * 0.5f);
            drawerScreen.setPivotX(0f);
            drawerScreen.setPivotY(height * 0.5f);
            homeScreen.setAlpha(1f - progress);
            homeScreen.setRotationY(13f * progress);
            homeScreen.setScaleX(1f - 0.06f * progress);
            homeScreen.setScaleY(1f - 0.06f * progress);
            drawerScreen.setAlpha(progress);
            drawerScreen.setRotationY(-72f * (1f - progress));
            drawerScreen.setScaleX(0.94f + 0.06f * progress);
            drawerScreen.setScaleY(0.94f + 0.06f * progress);
            drawerScreen.setTranslationX(0f);
            drawerScreen.setTranslationY(0f);
        } else if (effect == 2) {
            homeScreen.setAlpha(1f - progress);
            homeScreen.setScaleX(1f - 0.11f * progress);
            homeScreen.setScaleY(1f - 0.11f * progress);
            homeScreen.setTranslationY(-LauncherUiUtils.dp(activity, 18) * progress);
            homeScreen.setTranslationZ(-density * 36f * progress);
            drawerScreen.setAlpha(progress);
            drawerScreen.setScaleX(0.91f + 0.09f * progress);
            drawerScreen.setScaleY(0.91f + 0.09f * progress);
            drawerScreen.setTranslationY(height * 0.14f * (1f - progress));
            drawerScreen.setTranslationZ(density * 42f * (1f - progress));
            drawerScreen.setRotationX(0f);
            drawerScreen.setTranslationX(0f);
        } else if (effect == 3) {
            homeScreen.setPivotX(width * 0.5f);
            homeScreen.setPivotY(height * 0.5f);
            drawerScreen.setPivotX(width * 0.5f);
            drawerScreen.setPivotY(height * 0.5f);
            homeScreen.setAlpha(1f - progress);
            homeScreen.setRotationY(34f * progress);
            homeScreen.setTranslationX(-width * 0.12f * progress);
            homeScreen.setScaleX(1f - 0.08f * progress);
            homeScreen.setScaleY(1f - 0.08f * progress);
            drawerScreen.setAlpha(progress);
            drawerScreen.setRotationY(-68f * (1f - progress));
            drawerScreen.setTranslationX(width * 0.20f * (1f - progress));
            drawerScreen.setScaleX(0.94f + 0.06f * progress);
            drawerScreen.setScaleY(0.94f + 0.06f * progress);
            drawerScreen.setTranslationY(0f);
        } else {
            homeScreen.setAlpha(1f - 0.18f * progress);
            homeScreen.setScaleX(1f - 0.06f * progress);
            homeScreen.setScaleY(1f - 0.06f * progress);
            homeScreen.setTranslationY(-LauncherUiUtils.dp(activity, 22) * progress);
            drawerScreen.setAlpha(progress);
            drawerScreen.setScaleX(0.92f + 0.08f * progress);
            drawerScreen.setScaleY(0.92f + 0.08f * progress);
            drawerScreen.setTranslationY(height * 0.16f * (1f - progress));
            drawerScreen.setRotationX(10f * (1f - progress));
            drawerScreen.setPivotY(0f);
            drawerScreen.setTranslationZ(0f);
            drawerScreen.setRotationY(0f);
            drawerScreen.setTranslationX(0f);
        }
    }

    static void showDrawer(Activity activity, LinearLayout homeScreen, LinearLayout drawerScreen,
                           Runnable beforeOpen, int effect) {
        showDrawer(activity, homeScreen, drawerScreen, beforeOpen, effect, 0f);
    }

    static void showDrawer(Activity activity, LinearLayout homeScreen, LinearLayout drawerScreen,
                           Runnable beforeOpen, int effect, float swipeProgress) {
        beforeOpen.run();
        float progress = Math.max(0f, Math.min(1f, swipeProgress));
        homeScreen.animate().cancel();
        drawerScreen.animate().cancel();
        prepareTransitionLayers(homeScreen, drawerScreen);
        updateDrawerSwipe(activity, homeScreen, drawerScreen, effect, progress);
        float remaining = 1f - progress;
        long duration = LauncherMotion.duration(activity, Math.max(100L, (long) (420L * remaining)));
        float density = activity.getResources().getDisplayMetrics().density;
        float width = Math.max(1, activity.getResources().getDisplayMetrics().widthPixels);
        float height = Math.max(1, activity.getResources().getDisplayMetrics().heightPixels);

        if (effect == 1) {
            homeScreen.animate().alpha(0f).rotationY(13f).scaleX(0.94f).scaleY(0.94f)
                    .setDuration(duration)
                    .withEndAction(() -> {
                        homeScreen.setVisibility(View.GONE);
                        releaseTransitionLayer(homeScreen);
                    }).start();
            drawerScreen.animate().alpha(1f).rotationY(0f).translationX(0f).translationY(0f)
                    .scaleX(1f).scaleY(1f)
                    .setDuration(duration)
                    .withEndAction(() -> releaseTransitionLayer(drawerScreen)).start();
        } else if (effect == 2) {
            homeScreen.animate().alpha(0f).scaleX(0.89f).scaleY(0.89f)
                    .translationY(-LauncherUiUtils.dp(activity, 18)).translationZ(-density * 36f)
                    .setDuration(duration)
                    .withEndAction(() -> homeScreen.setVisibility(View.GONE)).start();
            drawerScreen.animate().alpha(1f).scaleX(1f).scaleY(1f).translationY(0f)
                    .translationX(0f).translationZ(0f).rotationX(0f)
                    .setDuration(duration).start();
        } else if (effect == 3) {
            homeScreen.animate().alpha(0f).rotationY(34f).translationX(-width * 0.12f)
                    .scaleX(0.92f).scaleY(0.92f)
                    .setDuration(duration)
                    .withEndAction(() -> homeScreen.setVisibility(View.GONE)).start();
            drawerScreen.animate().alpha(1f).rotationY(0f).translationX(0f).translationY(0f)
                    .scaleX(1f).scaleY(1f)
                    .setDuration(duration).start();
        } else {
            homeScreen.animate().alpha(0f).scaleX(0.94f).scaleY(0.94f)
                    .translationY(-LauncherUiUtils.dp(activity, 22))
                    .setDuration(duration)
                    .withEndAction(() -> homeScreen.setVisibility(View.GONE)).start();
            drawerScreen.animate().alpha(1f).scaleX(1f).scaleY(1f).translationY(0f)
                    .translationX(0f).translationZ(0f).rotationX(0f).rotationY(0f)
                    .setDuration(duration).start();
        }
    }

    /**
     * Complete a finger-driven drawer close. The release animation continues from
     * the exact gesture pose instead of snapping into the canned showHome transition.
     */
    static void finishDrawerSwipeToHome(Activity activity, LinearLayout homeScreen,
                                        LinearLayout drawerScreen, Runnable markDrawerClosed,
                                        int effect, float swipeProgress) {
        final float start = Math.max(0f, Math.min(1f, swipeProgress));
        homeScreen.animate().cancel();
        drawerScreen.animate().cancel();
        prepareTransitionLayers(homeScreen, drawerScreen);
        android.animation.ValueAnimator animator =
                android.animation.ValueAnimator.ofFloat(start, 0f);
        animator.setDuration(LauncherMotion.duration(activity,
                Math.max(110L, (long) (300L * start))));
        animator.setInterpolator(new android.view.animation.DecelerateInterpolator(1.5f));
        animator.addUpdateListener(value ->
                updateDrawerSwipe(activity, homeScreen, drawerScreen, effect,
                        (Float) value.getAnimatedValue()));
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(android.animation.Animator animation) {
                markDrawerClosed.run();
                homeScreen.setVisibility(View.VISIBLE);
                homeScreen.setAlpha(1f);
                homeScreen.setRotationX(0f);
                homeScreen.setRotationY(0f);
                homeScreen.setTranslationX(0f);
                homeScreen.setTranslationY(0f);
                homeScreen.setTranslationZ(0f);
                homeScreen.setScaleX(1f);
                homeScreen.setScaleY(1f);
                drawerScreen.setVisibility(View.GONE);
                drawerScreen.setAlpha(1f);
                drawerScreen.setRotationX(0f);
                drawerScreen.setRotationY(0f);
                drawerScreen.setTranslationX(0f);
                drawerScreen.setTranslationY(0f);
                drawerScreen.setTranslationZ(0f);
                drawerScreen.setScaleX(1f);
                drawerScreen.setScaleY(1f);
                releaseTransitionLayer(homeScreen);
                releaseTransitionLayer(drawerScreen);
            }
        });
        animator.start();
    }

    /** Return a cancelled finger-driven close gesture to the fully open drawer. */
    static void finishDrawerSwipeToDrawer(Activity activity, LinearLayout homeScreen,
                                          LinearLayout drawerScreen, Runnable markDrawerOpen,
                                          int effect, float swipeProgress) {
        final float start = Math.max(0f, Math.min(1f, swipeProgress));
        homeScreen.animate().cancel();
        drawerScreen.animate().cancel();
        prepareTransitionLayers(homeScreen, drawerScreen);
        homeScreen.setVisibility(View.VISIBLE);
        drawerScreen.setVisibility(View.VISIBLE);
        android.animation.ValueAnimator animator =
                android.animation.ValueAnimator.ofFloat(start, 1f);
        animator.setDuration(LauncherMotion.duration(activity,
                Math.max(110L, (long) (300L * (1f - start)))));
        animator.setInterpolator(new android.view.animation.DecelerateInterpolator(1.5f));
        animator.addUpdateListener(value ->
                updateDrawerSwipe(activity, homeScreen, drawerScreen, effect,
                        (Float) value.getAnimatedValue()));
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(android.animation.Animator animation) {
                markDrawerOpen.run();
                homeScreen.setVisibility(View.GONE);
                homeScreen.setAlpha(1f);
                homeScreen.setRotationX(0f);
                homeScreen.setRotationY(0f);
                homeScreen.setTranslationX(0f);
                homeScreen.setTranslationY(0f);
                homeScreen.setTranslationZ(0f);
                homeScreen.setScaleX(1f);
                homeScreen.setScaleY(1f);
                drawerScreen.setVisibility(View.VISIBLE);
                drawerScreen.setAlpha(1f);
                drawerScreen.setRotationX(0f);
                drawerScreen.setRotationY(0f);
                drawerScreen.setTranslationX(0f);
                drawerScreen.setTranslationY(0f);
                drawerScreen.setTranslationZ(0f);
                drawerScreen.setScaleX(1f);
                drawerScreen.setScaleY(1f);
                releaseTransitionLayer(homeScreen);
                releaseTransitionLayer(drawerScreen);
            }
        });
        animator.start();
    }

    static void cancelDrawerSwipe(Activity activity, LinearLayout homeScreen,
                                 LinearLayout drawerScreen, int effect) {
        homeScreen.animate().cancel();
        drawerScreen.animate().cancel();
        prepareTransitionLayers(homeScreen, drawerScreen);
        homeScreen.setVisibility(View.VISIBLE);
        drawerScreen.setVisibility(View.VISIBLE);
        homeScreen.animate().alpha(1f).rotationX(0f).rotationY(0f).translationX(0f)
                .translationY(0f).translationZ(0f).scaleX(1f).scaleY(1f)
                .setDuration(LauncherMotion.duration(activity, 180L))
                .withEndAction(() -> releaseTransitionLayer(homeScreen)).start();
        drawerScreen.animate().alpha(0f).rotationX(0f).rotationY(0f).translationX(0f)
                .translationY(0f).translationZ(0f).scaleX(1f).scaleY(1f)
                .setDuration(LauncherMotion.duration(activity, 180L))
                .withEndAction(() -> {
                    drawerScreen.setVisibility(View.GONE);
                    drawerScreen.setAlpha(1f);
                    drawerScreen.setScaleX(1f);
                    drawerScreen.setScaleY(1f);
                    releaseTransitionLayer(drawerScreen);
                }).start();
    }

}
