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
    // ViewPropertyAnimator completion is tracked across its two screen animations.
    private static int propertyTransitionAnimationsRemaining;
    // True while the active transition is settling toward the fully open drawer.
    private static boolean activeDrawerTransitionToOpen;

    private static float clampProgress(float progress) {
        return Math.max(0f, Math.min(1f, progress));
    }

    static boolean isDrawerTransitionRunning(LinearLayout homeScreen,
            LinearLayout drawerScreen, int effect) {
        if (activeParallaxAnimator != null && activeParallaxAnimator.isRunning()
                && activeParallaxHome == homeScreen && activeParallaxDrawer == drawerScreen) {
            return true;
        }
        if (activeDrawerSettleAnimator != null && activeDrawerSettleAnimator.isRunning()) return true;
        return propertyTransitionAnimationsRemaining > 0;
    }

    static boolean isDrawerTransitionOpening() {
        return activeDrawerTransitionToOpen;
    }

    private static void beginPropertyTransition(boolean opening) {
        propertyTransitionAnimationsRemaining = 2;
        activeDrawerTransitionToOpen = opening;
    }

    private static void finishPropertyTransitionAnimation() {
        if (propertyTransitionAnimationsRemaining > 0) {
            propertyTransitionAnimationsRemaining--;
        }
    }

    /** Cancel all tracked animations before snapping the launcher home immediately. */
    static void cancelTrackedDrawerTransitions() {
        propertyTransitionAnimationsRemaining = 0;
        activeDrawerTransitionToOpen = false;
        cancelActiveParallaxAnimator();
        activeParallaxHome = null;
        activeParallaxDrawer = null;
        activeParallaxProgress = 0f;
        cancelActiveDrawerSettleAnimator();
    }

    static float getDrawerTransitionProgress(LinearLayout homeScreen,
            LinearLayout drawerScreen, int effect, boolean drawerOpen) {
        if (activeParallaxAnimator != null && activeParallaxAnimator.isRunning()
                && activeParallaxHome == homeScreen && activeParallaxDrawer == drawerScreen) {
            Object value = activeParallaxAnimator.getAnimatedValue();
            if (value instanceof Number) activeParallaxProgress =
                    clampProgress(((Number) value).floatValue());
            return activeParallaxProgress;
        }
        if (activeDrawerSettleAnimator != null && activeDrawerSettleAnimator.isRunning()) {
            Object value = activeDrawerSettleAnimator.getAnimatedValue();
            if (value instanceof Number) return clampProgress(((Number) value).floatValue());
        }
        // Property-animator effects expose their in-flight opening fraction through alpha.
        // Reading it avoids adding a second per-frame progress animator for touch readiness.
        if (propertyTransitionAnimationsRemaining > 0 && drawerScreen != null
                && drawerScreen.getVisibility() == View.VISIBLE) {
            return clampProgress(drawerScreen.getAlpha());
        }
        if (drawerScreen != null && drawerScreen.getVisibility() == View.GONE) return 0f;
        if (homeScreen != null && homeScreen.getVisibility() == View.GONE) return 1f;
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
        prepareTransitionLayers(homeScreen, drawerScreen);
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
        if (effect == 0) {
            float start = getDrawerTransitionProgress(homeScreen, drawerScreen, effect, true);
            markDrawerClosed.run();
            animateParallaxProgress(activity, homeScreen, drawerScreen, start, 0f, 380L,
                    () -> finishParallaxAtHome(homeScreen, drawerScreen));
            return;
        }
        markDrawerClosed.run();
        drawerScreen.animate().cancel();
        homeScreen.animate().cancel();
        beginPropertyTransition(false);
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

        if (effect == 0) {
            // Reverse of the Parallex entrance: home drops in from above while the
            // drawer moves down and fades away, with both surfaces on the same plane.
            float travelDistance = transitionHeight(activity, homeScreen, drawerScreen);
            long duration = LauncherMotion.duration(activity, 380L);
            drawerScreen.setVisibility(View.VISIBLE);
            drawerScreen.setAlpha(1f);
            drawerScreen.setScaleX(1f);
            drawerScreen.setScaleY(1f);
            drawerScreen.setRotationX(0f);
            drawerScreen.setRotationY(0f);
            drawerScreen.setTranslationX(0f);
            drawerScreen.setTranslationY(0f);
            drawerScreen.setTranslationZ(0f);
            homeScreen.setAlpha(0f);
            homeScreen.setScaleX(1f);
            homeScreen.setScaleY(1f);
            homeScreen.setRotationX(0f);
            homeScreen.setRotationY(0f);
            homeScreen.setTranslationX(0f);
            homeScreen.setTranslationY(-travelDistance);
            homeScreen.setTranslationZ(0f);
            drawerScreen.animate().alpha(0f).translationY(travelDistance)
                    .translationX(0f).translationZ(0f).rotationX(0f).rotationY(0f)
                    .scaleX(1f).scaleY(1f).setDuration(duration)
                    .withEndAction(() -> {
                        drawerScreen.setVisibility(View.GONE);
                        drawerScreen.setAlpha(1f);
                        drawerScreen.setTranslationX(0f);
                        drawerScreen.setTranslationY(0f);
                        drawerScreen.setTranslationZ(0f);
                        drawerScreen.setRotationX(0f);
                        drawerScreen.setRotationY(0f);
                        drawerScreen.setScaleX(1f);
                        drawerScreen.setScaleY(1f);
                        releaseTransitionLayer(drawerScreen);
                        finishPropertyTransitionAnimation();
                    }).start();
            homeScreen.animate().alpha(1f).translationY(0f).translationX(0f)
                    .translationZ(0f).rotationX(0f).rotationY(0f)
                    .scaleX(1f).scaleY(1f).setDuration(duration)
                    .withEndAction(() -> { releaseTransitionLayer(homeScreen); finishPropertyTransitionAnimation(); }).start();
        } else if (effect == 1) {
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
                        finishPropertyTransitionAnimation();
                    }).start();
            homeScreen.animate().alpha(1f).rotationY(0f).scaleX(1f).scaleY(1f)
                    .setDuration(LauncherMotion.duration(activity, 360L))
                    .withEndAction(() -> { releaseTransitionLayer(homeScreen); finishPropertyTransitionAnimation(); }).start();
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
                        finishPropertyTransitionAnimation();
                    }).start();
            homeScreen.animate().alpha(1f).translationY(0f).translationZ(0f).scaleX(1f).scaleY(1f)
                    .setDuration(LauncherMotion.duration(activity, 360L))
                    .withEndAction(() -> { releaseTransitionLayer(homeScreen); finishPropertyTransitionAnimation(); }).start();
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
                        finishPropertyTransitionAnimation();
                    }).start();
            homeScreen.animate().alpha(1f).rotationY(0f).translationX(0f).scaleX(1f).scaleY(1f)
                    .setDuration(LauncherMotion.duration(activity, 390L))
                    .withEndAction(() -> { releaseTransitionLayer(homeScreen); finishPropertyTransitionAnimation(); }).start();
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
                        finishPropertyTransitionAnimation();
                    }).start();
            homeScreen.animate().alpha(1f).translationY(0f).scaleX(1f).scaleY(1f)
                    .setDuration(LauncherMotion.duration(activity, 340L))
                    .withEndAction(() -> { releaseTransitionLayer(homeScreen); finishPropertyTransitionAnimation(); }).start();
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

        // Reuse the same layer caching used by the settle animator, so the first
        // finger-controlled frames do not continuously redraw both full-screen surfaces.
        prepareTransitionLayers(homeScreen, drawerScreen);
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
        if (effect == 0) {
            if (isDrawerTransitionRunning(homeScreen, drawerScreen, effect) && progress <= 0.001f) {
                progress = getDrawerTransitionProgress(homeScreen, drawerScreen, effect, true);
            }
            beforeOpen.run();
            animateParallaxProgress(activity, homeScreen, drawerScreen, progress, 1f, 380L,
                    () -> finishParallaxAtDrawer(homeScreen, drawerScreen));
            return;
        }
        beforeOpen.run();
        homeScreen.animate().cancel();
        drawerScreen.animate().cancel();
        prepareTransitionLayers(homeScreen, drawerScreen);
        updateDrawerSwipe(activity, homeScreen, drawerScreen, effect, progress);
        float remaining = 1f - progress;
        long duration = LauncherMotion.duration(activity,
                Math.max(100L, (long) ((effect == 0 ? 380L : 420L) * remaining)));
        beginPropertyTransition(true);
        float density = activity.getResources().getDisplayMetrics().density;
        float width = Math.max(1, activity.getResources().getDisplayMetrics().widthPixels);
        float height = Math.max(1, activity.getResources().getDisplayMetrics().heightPixels);

        if (effect == 0) {
            float travelDistance = transitionHeight(activity, homeScreen, drawerScreen);
            homeScreen.animate().alpha(0f).translationY(-travelDistance)
                    .translationX(0f).translationZ(0f).rotationX(0f).rotationY(0f)
                    .scaleX(1f).scaleY(1f).setDuration(duration)
                    .withEndAction(() -> {
                        homeScreen.setVisibility(View.GONE);
                        homeScreen.setAlpha(1f);
                        homeScreen.setTranslationX(0f);
                        homeScreen.setTranslationY(0f);
                        homeScreen.setTranslationZ(0f);
                        homeScreen.setRotationX(0f);
                        homeScreen.setRotationY(0f);
                        homeScreen.setScaleX(1f);
                        homeScreen.setScaleY(1f);
                        releaseTransitionLayer(homeScreen);
                        finishPropertyTransitionAnimation();
                    }).start();
            drawerScreen.animate().alpha(1f).translationY(0f)
                    .translationX(0f).translationZ(0f).rotationX(0f).rotationY(0f)
                    .scaleX(1f).scaleY(1f).setDuration(duration)
                    .withEndAction(() -> { releaseTransitionLayer(drawerScreen); finishPropertyTransitionAnimation(); }).start();
        } else if (effect == 1) {
            homeScreen.animate().alpha(0f).rotationY(13f).scaleX(0.94f).scaleY(0.94f)
                    .setDuration(duration)
                    .withEndAction(() -> {
                        homeScreen.setVisibility(View.GONE);
                        releaseTransitionLayer(homeScreen);
                        finishPropertyTransitionAnimation();
                    }).start();
            drawerScreen.animate().alpha(1f).rotationY(0f).translationX(0f).translationY(0f)
                    .scaleX(1f).scaleY(1f)
                    .setDuration(duration)
                    .withEndAction(() -> { releaseTransitionLayer(drawerScreen); finishPropertyTransitionAnimation(); }).start();
        } else if (effect == 2) {
            homeScreen.animate().alpha(0f).scaleX(0.89f).scaleY(0.89f)
                    .translationY(-LauncherUiUtils.dp(activity, 18)).translationZ(-density * 36f)
                    .setDuration(duration)
                    .withEndAction(() -> {
                        homeScreen.setVisibility(View.GONE);
                        releaseTransitionLayer(homeScreen);
                        finishPropertyTransitionAnimation();
                    }).start();
            drawerScreen.animate().alpha(1f).scaleX(1f).scaleY(1f).translationY(0f)
                    .translationX(0f).translationZ(0f).rotationX(0f)
                    .setDuration(duration)
                    .withEndAction(() -> { releaseTransitionLayer(drawerScreen); finishPropertyTransitionAnimation(); }).start();
        } else if (effect == 3) {
            homeScreen.animate().alpha(0f).rotationY(34f).translationX(-width * 0.12f)
                    .scaleX(0.92f).scaleY(0.92f)
                    .setDuration(duration)
                    .withEndAction(() -> {
                        homeScreen.setVisibility(View.GONE);
                        releaseTransitionLayer(homeScreen);
                        finishPropertyTransitionAnimation();
                    }).start();
            drawerScreen.animate().alpha(1f).rotationY(0f).translationX(0f).translationY(0f)
                    .scaleX(1f).scaleY(1f)
                    .setDuration(duration)
                    .withEndAction(() -> { releaseTransitionLayer(drawerScreen); finishPropertyTransitionAnimation(); }).start();
        } else {
            homeScreen.animate().alpha(0f).scaleX(0.94f).scaleY(0.94f)
                    .translationY(-LauncherUiUtils.dp(activity, 22))
                    .setDuration(duration)
                    .withEndAction(() -> {
                        homeScreen.setVisibility(View.GONE);
                        releaseTransitionLayer(homeScreen);
                        finishPropertyTransitionAnimation();
                    }).start();
            drawerScreen.animate().alpha(1f).scaleX(1f).scaleY(1f).translationY(0f)
                    .translationX(0f).translationZ(0f).rotationX(0f).rotationY(0f)
                    .setDuration(duration)
                    .withEndAction(() -> { releaseTransitionLayer(drawerScreen); finishPropertyTransitionAnimation(); }).start();
        }
    }

    /**
     * Complete a finger-driven drawer close. The release animation continues from
     * the exact gesture pose instead of snapping into the canned showHome transition.
     */
    static void finishDrawerSwipeToHome(Activity activity, LinearLayout homeScreen,
                                        LinearLayout drawerScreen, Runnable markDrawerClosed,
                                        int effect, float swipeProgress) {
        activeDrawerTransitionToOpen = false;
        final float start = clampProgress(swipeProgress);
        if (effect == 0) {
            animateParallaxProgress(activity, homeScreen, drawerScreen, start, 0f, 300L, () -> {
                markDrawerClosed.run();
                finishParallaxAtHome(homeScreen, drawerScreen);
            });
            return;
        }
        homeScreen.animate().cancel();
        drawerScreen.animate().cancel();
        prepareTransitionLayers(homeScreen, drawerScreen);
        cancelActiveDrawerSettleAnimator();
        android.animation.ValueAnimator animator =
                android.animation.ValueAnimator.ofFloat(start, 0f);
        activeDrawerSettleAnimator = animator;
        animator.setDuration(LauncherMotion.duration(activity,
                Math.max(110L, (long) (300L * start))));
        animator.setInterpolator(new android.view.animation.DecelerateInterpolator(1.5f));
        animator.addUpdateListener(value ->
                updateDrawerSwipe(activity, homeScreen, drawerScreen, effect,
                        (Float) value.getAnimatedValue()));
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(android.animation.Animator animation) {
                if (activeDrawerSettleAnimator != animator) return;
                activeDrawerSettleAnimator = null;
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
        activeDrawerTransitionToOpen = true;
        final float start = clampProgress(swipeProgress);
        if (effect == 0) {
            animateParallaxProgress(activity, homeScreen, drawerScreen, start, 1f, 300L, () -> {
                markDrawerOpen.run();
                finishParallaxAtDrawer(homeScreen, drawerScreen);
            });
            return;
        }
        homeScreen.animate().cancel();
        drawerScreen.animate().cancel();
        prepareTransitionLayers(homeScreen, drawerScreen);
        homeScreen.setVisibility(View.VISIBLE);
        drawerScreen.setVisibility(View.VISIBLE);
        cancelActiveDrawerSettleAnimator();
        android.animation.ValueAnimator animator =
                android.animation.ValueAnimator.ofFloat(start, 1f);
        activeDrawerSettleAnimator = animator;
        animator.setDuration(LauncherMotion.duration(activity,
                Math.max(110L, (long) (300L * (1f - start)))));
        animator.setInterpolator(new android.view.animation.DecelerateInterpolator(1.5f));
        animator.addUpdateListener(value ->
                updateDrawerSwipe(activity, homeScreen, drawerScreen, effect,
                        (Float) value.getAnimatedValue()));
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(android.animation.Animator animation) {
                if (activeDrawerSettleAnimator != animator) return;
                activeDrawerSettleAnimator = null;
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
        activeDrawerTransitionToOpen = false;
        if (effect == 0) {
            float start = clampProgress(activeParallaxProgress);
            animateParallaxProgress(activity, homeScreen, drawerScreen, start, 0f, 180L,
                    () -> finishParallaxAtHome(homeScreen, drawerScreen));
            return;
        }
        homeScreen.animate().cancel();
        drawerScreen.animate().cancel();
        prepareTransitionLayers(homeScreen, drawerScreen);
        beginPropertyTransition(false);
        homeScreen.setVisibility(View.VISIBLE);
        drawerScreen.setVisibility(View.VISIBLE);
        float drawerExitY = effect == 0
                ? transitionHeight(activity, homeScreen, drawerScreen) : 0f;
        homeScreen.animate().alpha(1f).rotationX(0f).rotationY(0f).translationX(0f)
                .translationY(0f).translationZ(0f).scaleX(1f).scaleY(1f)
                .setDuration(LauncherMotion.duration(activity, 180L))
                .withEndAction(() -> { releaseTransitionLayer(homeScreen); finishPropertyTransitionAnimation(); }).start();
        drawerScreen.animate().alpha(0f).rotationX(0f).rotationY(0f).translationX(0f)
                .translationY(drawerExitY).translationZ(0f).scaleX(1f).scaleY(1f)
                .setDuration(LauncherMotion.duration(activity, 180L))
                .withEndAction(() -> {
                    drawerScreen.setVisibility(View.GONE);
                    drawerScreen.setAlpha(1f);
                    drawerScreen.setScaleX(1f);
                    drawerScreen.setScaleY(1f);
                    drawerScreen.setRotationX(0f);
                    drawerScreen.setRotationY(0f);
                    drawerScreen.setTranslationX(0f);
                    drawerScreen.setTranslationY(0f);
                    drawerScreen.setTranslationZ(0f);
                    releaseTransitionLayer(drawerScreen);
                    finishPropertyTransitionAnimation();
                }).start();
    }

}
