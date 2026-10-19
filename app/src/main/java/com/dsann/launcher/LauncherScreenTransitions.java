package com.dsann.launcher;

import android.app.Activity;
import android.view.View;
import android.widget.LinearLayout;

/** Owns the home/drawer transition choreography independently of screen construction. */
final class LauncherScreenTransitions {
    private LauncherScreenTransitions() {}

    // Parallex uses one progress animator for both surfaces.
    private static android.animation.ValueAnimator activeParallaxAnimator;
    private static LinearLayout activeParallaxHome;
    private static LinearLayout activeParallaxDrawer;
    private static float activeParallaxProgress;
    // Tracks finger-release settling for non-Parallax drawer effects too.
    private static android.animation.ValueAnimator activeDrawerSettleAnimator;
    // True while the active transition is settling toward the fully open drawer.
    private static boolean activeDrawerTransitionToOpen;

    private static float clampProgress(float progress) {
        return Math.max(0f, Math.min(1f, progress));
    }

    static boolean isDrawerTransitionRunning(LinearLayout homeScreen,
            LinearLayout drawerScreen, int effect) {
        if (activeParallaxAnimator != null && activeParallaxAnimator.isStarted()
                && activeParallaxHome == homeScreen && activeParallaxDrawer == drawerScreen) {
            return true;
        }
        return activeDrawerSettleAnimator != null && activeDrawerSettleAnimator.isStarted();
    }

    static boolean isDrawerTransitionOpening() {
        return activeDrawerTransitionToOpen;
    }

    /** Cancel all tracked animations before snapping the launcher home immediately. */
    static void cancelTrackedDrawerTransitions() {
        activeDrawerTransitionToOpen = false;
        cancelActiveParallaxAnimator();
        activeParallaxHome = null;
        activeParallaxDrawer = null;
        activeParallaxProgress = 0f;
        cancelActiveDrawerSettleAnimator();
    }

    static float getDrawerTransitionProgress(LinearLayout homeScreen,
            LinearLayout drawerScreen, int effect, boolean drawerOpen) {
        if (activeParallaxAnimator != null && activeParallaxAnimator.isStarted()
                && activeParallaxHome == homeScreen && activeParallaxDrawer == drawerScreen) {
            Object value = activeParallaxAnimator.getAnimatedValue();
            if (value instanceof Number) activeParallaxProgress =
                    clampProgress(((Number) value).floatValue());
            return activeParallaxProgress;
        }
        if (activeDrawerSettleAnimator != null && activeDrawerSettleAnimator.isStarted()) {
            Object value = activeDrawerSettleAnimator.getAnimatedValue();
            if (value instanceof Number) return clampProgress(((Number) value).floatValue());
        }
        if (drawerScreen != null && drawerScreen.getVisibility() == View.GONE) return 0f;
        if (homeScreen != null && homeScreen.getVisibility() == View.GONE) return 1f;
        // Both surfaces are intentionally visible during a finger-controlled partial
        // swipe. Preserve that exact progress if the gesture is cancelled/released.
        if (homeScreen != null && drawerScreen != null
                && homeScreen.getVisibility() == View.VISIBLE
                && drawerScreen.getVisibility() == View.VISIBLE) {
            return clampProgress(activeParallaxProgress);
        }
        return drawerOpen ? 1f : 0f;
    }

    private static void cancelActiveDrawerSettleAnimator() {
        android.animation.ValueAnimator old = activeDrawerSettleAnimator;
        activeDrawerSettleAnimator = null;
        if (old != null) old.cancel();
    }

    private static void cancelActiveParallaxAnimator() {
        android.animation.ValueAnimator old = activeParallaxAnimator;
        activeParallaxAnimator = null;
        if (old != null) old.cancel();
    }

    private static void resetTransitionPose(View view) {
        if (view == null) return;
        view.setAlpha(1f);
        view.setRotationX(0f);
        view.setRotationY(0f);
        view.setTranslationX(0f);
        view.setTranslationY(0f);
        view.setTranslationZ(0f);
        view.setScaleX(1f);
        view.setScaleY(1f);
    }

    private static void finishParallaxAtHome(LinearLayout homeScreen, LinearLayout drawerScreen) {
        homeScreen.setVisibility(View.VISIBLE);
        resetTransitionPose(homeScreen);
        drawerScreen.setVisibility(View.GONE);
        resetTransitionPose(drawerScreen);
        releaseTransitionLayer(homeScreen);
        releaseTransitionLayer(drawerScreen);
    }

    private static void finishParallaxAtDrawer(LinearLayout homeScreen, LinearLayout drawerScreen) {
        homeScreen.setVisibility(View.GONE);
        resetTransitionPose(homeScreen);
        drawerScreen.setVisibility(View.VISIBLE);
        resetTransitionPose(drawerScreen);
        releaseTransitionLayer(homeScreen);
        releaseTransitionLayer(drawerScreen);
    }

    private static void animateParallaxProgress(Activity activity, LinearLayout homeScreen,
            LinearLayout drawerScreen, float start, float end, long fullDuration,
            Runnable endAction) {
        cancelActiveParallaxAnimator();
        homeScreen.animate().cancel();
        drawerScreen.animate().cancel();
        // Alpha/translation are render-thread properties on the hardware-accelerated
        // launcher. Separate full-screen hardware layers cost two large textures and
        // are repeatedly recreated on drawer open, producing the Parallax hitch.
        releaseTransitionLayer(homeScreen);
        releaseTransitionLayer(drawerScreen);
        homeScreen.setVisibility(View.VISIBLE);
        drawerScreen.setVisibility(View.VISIBLE);
        final float from = clampProgress(start);
        final float to = clampProgress(end);
        activeDrawerTransitionToOpen = to >= 0.5f;
        activeParallaxProgress = from;
        activeParallaxHome = homeScreen;
        activeParallaxDrawer = drawerScreen;
        updateDrawerSwipe(activity, homeScreen, drawerScreen, 0, from);

        final android.animation.ValueAnimator animator =
                android.animation.ValueAnimator.ofFloat(from, to);
        long duration = Math.max(110L, Math.round(fullDuration * Math.abs(to - from)));
        animator.setDuration(LauncherMotion.duration(activity, duration));
        animator.setInterpolator(new android.view.animation.DecelerateInterpolator(1.5f));
        activeParallaxAnimator = animator;
        animator.addUpdateListener(value -> {
            if (activeParallaxAnimator != animator) return;
            activeParallaxProgress = clampProgress(((Number) value.getAnimatedValue()).floatValue());
            updateDrawerSwipe(activity, homeScreen, drawerScreen, 0, activeParallaxProgress);
        });
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(android.animation.Animator animation) {
                if (activeParallaxAnimator != animator) return;
                activeParallaxAnimator = null;
                activeParallaxProgress = to;
                if (endAction != null) endAction.run();
            }
        });
        animator.start();
    }

    // 0 = Parallex, 1 = 3D Card Flip, 2 = Depth Slide, 3 = Rotating Carousel.
    private static void prepareTransitionLayers(LinearLayout homeScreen, LinearLayout drawerScreen) {
        // Cache both surfaces during transforms to avoid display-list rebuild flashes.
        // Avoid resetting an already-correct layer type: that can invalidate/rebuild
        // expensive full-screen layers during rapid repeated transitions.
        if (homeScreen != null && homeScreen.getLayerType() != View.LAYER_TYPE_HARDWARE) {
            homeScreen.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }
        if (drawerScreen != null && drawerScreen.getLayerType() != View.LAYER_TYPE_HARDWARE) {
            drawerScreen.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }
    }

    private static void releaseTransitionLayer(View view) {
        if (view != null && view.getLayerType() != View.LAYER_TYPE_NONE) {
            view.setLayerType(View.LAYER_TYPE_NONE, null);
        }
    }

    /** Travel distance is the actual layout height, not the physical display height. */
    private static float transitionHeight(Activity activity, LinearLayout homeScreen,
            LinearLayout drawerScreen) {
        int homeHeight = homeScreen == null ? 0 : homeScreen.getHeight();
        int drawerHeight = drawerScreen == null ? 0 : drawerScreen.getHeight();
        int measuredHeight = Math.max(homeHeight, drawerHeight);
        if (measuredHeight <= 0) {
            measuredHeight = activity.getResources().getDisplayMetrics().heightPixels;
        }
        return Math.max(1, measuredHeight);
    }

    static void showHome(Activity activity, LinearLayout homeScreen, LinearLayout drawerScreen,
                         Runnable markDrawerClosed, int effect) {
        float start = getDrawerTransitionProgress(homeScreen, drawerScreen, effect, true);
        markDrawerClosed.run();
        long fullDuration = effect == 0 ? 380L
                : effect == 1 ? 360L
                : effect == 2 ? 360L
                : effect == 3 ? 390L : 340L;
        animateDrawerProgress(activity, homeScreen, drawerScreen, effect, start, 0f,
                fullDuration, false, () -> finishParallaxAtHome(homeScreen, drawerScreen));
    }

    /**
     * All non-Parallax drawer effects use the same progress-driven renderer as
     * finger gestures. A reversal therefore continues from the exact current pose
     * instead of switching between two different ViewPropertyAnimator trajectories.
     */
    private static void animateDrawerProgress(Activity activity, LinearLayout homeScreen,
            LinearLayout drawerScreen, int effect, float start, float end, long fullDuration,
            boolean opening, Runnable endAction) {
        final float from = clampProgress(start);
        final float to = clampProgress(end);
        activeDrawerTransitionToOpen = opening;
        if (effect == 0) {
            cancelActiveDrawerSettleAnimator();
            animateParallaxProgress(activity, homeScreen, drawerScreen, from, to,
                    fullDuration, endAction);
            return;
        }

        cancelActiveParallaxAnimator();
        activeParallaxHome = null;
        activeParallaxDrawer = null;
        homeScreen.animate().cancel();
        drawerScreen.animate().cancel();
        cancelActiveDrawerSettleAnimator();
        prepareTransitionLayers(homeScreen, drawerScreen);
        homeScreen.setVisibility(View.VISIBLE);
        drawerScreen.setVisibility(View.VISIBLE);
        updateDrawerSwipe(activity, homeScreen, drawerScreen, effect, from);

        final android.animation.ValueAnimator animator =
                android.animation.ValueAnimator.ofFloat(from, to);
        activeDrawerSettleAnimator = animator;
        long duration = Math.max(110L, Math.round(fullDuration * Math.abs(to - from)));
        animator.setDuration(LauncherMotion.duration(activity, duration));
        animator.setInterpolator(new android.view.animation.DecelerateInterpolator(1.5f));
        animator.addUpdateListener(value -> {
            if (activeDrawerSettleAnimator != animator) return;
            updateDrawerSwipe(activity, homeScreen, drawerScreen, effect,
                    ((Number) value.getAnimatedValue()).floatValue());
        });
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(android.animation.Animator animation) {
                if (activeDrawerSettleAnimator != animator) return;
                activeDrawerSettleAnimator = null;
                if (endAction != null) endAction.run();
            }
        });
        animator.start();
    }

    /** Freeze the in-flight transition while deciding whether this touch reverses it. */
    static void pauseDrawerTransition(LinearLayout homeScreen, LinearLayout drawerScreen, int effect) {
        if (effect == 0 && activeParallaxAnimator != null
                && activeParallaxAnimator.isStarted() && !activeParallaxAnimator.isPaused()
                && activeParallaxHome == homeScreen && activeParallaxDrawer == drawerScreen) {
            activeParallaxAnimator.pause();
        } else if (activeDrawerSettleAnimator != null
                && activeDrawerSettleAnimator.isStarted()
                && !activeDrawerSettleAnimator.isPaused()) {
            activeDrawerSettleAnimator.pause();
        }
    }

    /** Resume a paused transition when the touch was not a reverse swipe. */
    static void resumeDrawerTransitionIfPaused() {
        if (activeParallaxAnimator != null && activeParallaxAnimator.isStarted()
                && activeParallaxAnimator.isPaused()) {
            activeParallaxAnimator.resume();
        }
        if (activeDrawerSettleAnimator != null && activeDrawerSettleAnimator.isStarted()
                && activeDrawerSettleAnimator.isPaused()) {
            activeDrawerSettleAnimator.resume();
        }
    }

    /**
     * Parallax only animates alpha and vertical translation. Keep its finger-driven
     * path lightweight: prepare cached layers once, repair stale transforms only when
     * necessary, and avoid writing unused rotation/scale/depth properties every frame.
     */
    private static void updateParallaxSwipe(Activity activity, LinearLayout homeScreen,
                                             LinearLayout drawerScreen, float progress) {
        if (homeScreen == null || drawerScreen == null) return;
        if (homeScreen.getVisibility() != View.VISIBLE) homeScreen.setVisibility(View.VISIBLE);
        if (drawerScreen.getVisibility() != View.VISIBLE) drawerScreen.setVisibility(View.VISIBLE);

        // Do not allocate full-screen hardware layers for property-only Parallax.
        // This keeps partial swipes cheap and avoids a cached parent texture interfering
        // with the drawer GridView when input becomes live near 80% progress.
        resetParallaxStaticPose(homeScreen);
        resetParallaxStaticPose(drawerScreen);

        float travelDistance = transitionHeight(activity, homeScreen, drawerScreen);
        homeScreen.setAlpha(1f - progress);
        homeScreen.setTranslationY(-travelDistance * progress);
        drawerScreen.setAlpha(progress);
        drawerScreen.setTranslationY(travelDistance * (1f - progress));
    }

    /** Reset only non-Parallax properties, and only if an interrupted effect left them changed. */
    private static void resetParallaxStaticPose(View view) {
        if (view.getTranslationX() != 0f) view.setTranslationX(0f);
        if (view.getTranslationZ() != 0f) view.setTranslationZ(0f);
        if (view.getRotationX() != 0f) view.setRotationX(0f);
        if (view.getRotationY() != 0f) view.setRotationY(0f);
        if (view.getScaleX() != 1f) view.setScaleX(1f);
        if (view.getScaleY() != 1f) view.setScaleY(1f);
    }

    static void updateDrawerSwipe(Activity activity, LinearLayout homeScreen,
                                   LinearLayout drawerScreen, int effect, float swipeProgress) {
        float progress = clampProgress(swipeProgress);
        activeParallaxProgress = progress;
        if (effect == 0) {
            updateParallaxSwipe(activity, homeScreen, drawerScreen, progress);
            return;
        }

        float density = activity.getResources().getDisplayMetrics().density;
        float width = Math.max(1, activity.getResources().getDisplayMetrics().widthPixels);
        float height = Math.max(1, activity.getResources().getDisplayMetrics().heightPixels);
        if (homeScreen.getVisibility() != View.VISIBLE) homeScreen.setVisibility(View.VISIBLE);
        if (drawerScreen.getVisibility() != View.VISIBLE) drawerScreen.setVisibility(View.VISIBLE);
        if (effect == 1 || effect == 3 || effect > 3) {
            float cameraDistance = 10000f * density;
            if (homeScreen.getCameraDistance() != cameraDistance) {
                homeScreen.setCameraDistance(cameraDistance);
            }
            if (drawerScreen.getCameraDistance() != cameraDistance) {
                drawerScreen.setCameraDistance(cameraDistance);
            }
        }

        // Non-Parallax poses track the finger directly; keep their existing motion intact.
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
        float progress = clampProgress(swipeProgress);
        if (progress <= 0.001f
                && isDrawerTransitionRunning(homeScreen, drawerScreen, effect)) {
            progress = getDrawerTransitionProgress(homeScreen, drawerScreen, effect, false);
        }
        beforeOpen.run();
        animateDrawerProgress(activity, homeScreen, drawerScreen, effect, progress, 1f,
                effect == 0 ? 380L : 420L, true,
                () -> finishParallaxAtDrawer(homeScreen, drawerScreen));
    }

    /**
     * Complete a finger-driven drawer close without changing the animation path.
     */
    static void finishDrawerSwipeToHome(Activity activity, LinearLayout homeScreen,
                                        LinearLayout drawerScreen, Runnable markDrawerClosed,
                                        int effect, float swipeProgress) {
        animateDrawerProgress(activity, homeScreen, drawerScreen, effect, swipeProgress, 0f,
                300L, false, () -> {
                    markDrawerClosed.run();
                    finishParallaxAtHome(homeScreen, drawerScreen);
                });
    }

    /** Complete or reverse a finger-driven drawer opening from its exact current progress. */
    static void finishDrawerSwipeToDrawer(Activity activity, LinearLayout homeScreen,
                                          LinearLayout drawerScreen, Runnable markDrawerOpen,
                                          int effect, float swipeProgress) {
        animateDrawerProgress(activity, homeScreen, drawerScreen, effect, swipeProgress, 1f,
                300L, true, () -> {
                    markDrawerOpen.run();
                    finishParallaxAtDrawer(homeScreen, drawerScreen);
                });
    }

    static void cancelDrawerSwipe(Activity activity, LinearLayout homeScreen,
                                  LinearLayout drawerScreen, int effect) {
        float start = getDrawerTransitionProgress(homeScreen, drawerScreen, effect, false);
        animateDrawerProgress(activity, homeScreen, drawerScreen, effect, start, 0f,
                180L, false, () -> finishParallaxAtHome(homeScreen, drawerScreen));
    }
}
