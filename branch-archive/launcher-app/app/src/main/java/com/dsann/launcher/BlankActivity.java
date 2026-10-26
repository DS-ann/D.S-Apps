package com.dsann.launcher;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.app.Dialog;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * The destination behind D.S Launcher's own app-drawer icon.
 * This is intentionally separate from MainActivity, which remains the HOME screen.
 */
public final class BlankActivity extends Activity {
    private static final int INK = Color.rgb(8, 11, 27);
    private static final int PANEL = Color.rgb(13, 17, 38);
    private FrameLayout root;
    private ScrollView contentScroll;
    private View instructionControl;
    private boolean compactLandscape;
    private boolean instructionPopupShowing;
    private final java.util.ArrayList<ColorFlowTextView> pageGradientViews =
            new java.util.ArrayList<ColorFlowTextView>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prepareSystemBars();
        buildPage();
    }

    private void prepareSystemBars() {
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
        window.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
    }

    private void buildPage() {
        compactLandscape = getResources().getConfiguration().orientation
                == android.content.res.Configuration.ORIENTATION_LANDSCAPE;
        root = new FrameLayout(this);
        root.setBackgroundColor(INK);

        AuroraBackgroundView background = new AuroraBackgroundView();
        root.addView(background, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        contentScroll = new ScrollView(this);
        contentScroll.setFillViewport(true);
        contentScroll.setClipToPadding(false);
        contentScroll.setVerticalScrollBarEnabled(false);
        contentScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        contentScroll.setPadding(compactLandscape ? dp(14) : dp(24),
                compactLandscape ? dp(4) : dp(34),
                compactLandscape ? dp(14) : dp(24),
                compactLandscape ? dp(52) : dp(104));

        LinearLayout stack = new LinearLayout(this);
        stack.setOrientation(LinearLayout.VERTICAL);
        stack.setGravity(compactLandscape
                ? Gravity.CENTER_HORIZONTAL | Gravity.TOP
                : Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
        stack.setPadding(dp(2), compactLandscape ? dp(4) : dp(24),
                dp(2), compactLandscape ? dp(6) : dp(22));
        contentScroll.addView(stack, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(contentScroll, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        int medallionSizeDp = compactLandscape ? 72 : 132;
        stack.addView(buildIconMedallion(medallionSizeDp),
                new LinearLayout.LayoutParams(dp(medallionSizeDp), dp(medallionSizeDp)));

        ColorFlowTextView appName = makeGradientText(getApplicationLabel(), compactLandscape ? 23f : 34f,
                "cursive", android.graphics.Typeface.BOLD);
        appName.setGravity(Gravity.CENTER);
        appName.setShadowLayer(dp(18), 0f, dp(3), Color.argb(115, 109, 91, 255));
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleParams.topMargin = dp(compactLandscape ? 4 : 20);
        stack.addView(appName, titleParams);

        LinearLayout.LayoutParams boundaryLineParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(3));
        boundaryLineParams.topMargin = dp(compactLandscape ? 4 : 17);
        stack.addView(buildColorBoundary(), boundaryLineParams);

        ColorFlowTextView version = makeGradientText(getVersionLabel(), compactLandscape ? 8.5f : 10.5f,
                "monospace", android.graphics.Typeface.BOLD);
        version.setLetterSpacing(0.13f);
        version.setGravity(Gravity.CENTER);
        version.setPadding(dp(compactLandscape ? 8 : 14),
                dp(compactLandscape ? 3 : 8), dp(compactLandscape ? 8 : 14),
                dp(compactLandscape ? 3 : 8));
        version.setBackground(solidRounded(Color.argb(135, 25, 37, 70), dp(22)));
        LinearLayout.LayoutParams versionParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        versionParams.topMargin = dp(compactLandscape ? 4 : 15);
        stack.addView(version, versionParams);

        int availableWidth = getResources().getDisplayMetrics().widthPixels
                - dp(compactLandscape ? 32 : 56);
        int minimumCardWidth = dp(compactLandscape ? 220 : 260);
        int cardWidth = Math.max(minimumCardWidth, Math.min(dp(440), availableWidth));
        LinearLayout.LayoutParams creatorParams = new LinearLayout.LayoutParams(
                cardWidth, ViewGroup.LayoutParams.WRAP_CONTENT);
        creatorParams.topMargin = dp(compactLandscape ? 6 : 31);
        stack.addView(buildCreatorCard(compactLandscape), creatorParams);

        instructionControl = buildInstructionControl();
        FrameLayout.LayoutParams instructionParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.END | Gravity.BOTTOM);
        instructionParams.rightMargin = dp(20);
        instructionParams.bottomMargin = dp(22);
        root.addView(instructionControl, instructionParams);
        instructionControl.setOnClickListener(v -> showBlankInstructionPopup());

        // Targeting modern Android requires explicit system-bar insets because the page
        // intentionally paints its backdrop edge-to-edge. Keep text and controls clear
        // of status/navigation icons across Android 6 through current releases.
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            int topInset = insets.getSystemWindowInsetTop();
            int bottomInset = insets.getSystemWindowInsetBottom();
            contentScroll.setPadding(compactLandscape ? dp(14) : dp(24),
                    topInset + dp(compactLandscape ? 4 : 18),
                    compactLandscape ? dp(14) : dp(24),
                    bottomInset + dp(compactLandscape ? 52 : 104));
            FrameLayout.LayoutParams buttonParams =
                    (FrameLayout.LayoutParams) instructionControl.getLayoutParams();
            buttonParams.rightMargin = dp(20);
            buttonParams.bottomMargin = Math.max(dp(18), bottomInset + dp(16));
            instructionControl.setLayoutParams(buttonParams);
            return insets;
        });

        setContentView(root);
        contentScroll.post(() -> {
            contentScroll.setAlpha(0f);
            contentScroll.setTranslationY(dp(20));
            contentScroll.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(540)
                    .setInterpolator(new DecelerateInterpolator(1.5f))
                    .start();
            instructionControl.setAlpha(0f);
            instructionControl.setTranslationY(dp(12));
            instructionControl.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay(140)
                    .setDuration(420)
                    .setInterpolator(new DecelerateInterpolator(1.4f))
                    .start();
        });
    }

    private FrameLayout buildIconMedallion(float sizeDp) {
        FrameLayout medallion = new FrameLayout(this);
        GradientDrawable halo = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(75, 233, 255), Color.rgb(125, 94, 255),
                        Color.rgb(255, 91, 190)});
        halo.setShape(GradientDrawable.OVAL);
        medallion.setBackground(halo);
        medallion.setElevation(dp(sizeDp * 0.075f));

        FrameLayout darkRing = new FrameLayout(this);
        darkRing.setBackground(solidOval(Color.rgb(15, 18, 43)));
        FrameLayout.LayoutParams ringParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        int ringInset = Math.max(2, dp(sizeDp * 0.03f));
        ringParams.setMargins(ringInset, ringInset, ringInset, ringInset);
        medallion.addView(darkRing, ringParams);

        ImageView appIcon = new ImageView(this);
        appIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        appIcon.setContentDescription(getApplicationLabel() + " app icon");
        appIcon.setBackground(gradientRounded(new int[]{
                Color.rgb(35, 48, 85), Color.rgb(27, 27, 62)}, dp(26)));
        int appIconSize = Math.max(dp(46), dp(sizeDp * 0.66f));
        float iconInsetDp = sizeDp * 0.045f;
        appIcon.setPadding(dp(iconInsetDp), dp(iconInsetDp),
                dp(iconInsetDp), dp(iconInsetDp));
        try {
            appIcon.setImageDrawable(getPackageManager().getApplicationIcon(getPackageName()));
        } catch (PackageManager.NameNotFoundException ignored) {
            appIcon.setImageResource(R.mipmap.ic_launcher);
        }
        FrameLayout.LayoutParams iconParams = new FrameLayout.LayoutParams(
                appIconSize, appIconSize, Gravity.CENTER);
        darkRing.addView(appIcon, iconParams);

        View cyanSpark = new View(this);
        cyanSpark.setBackground(solidOval(Color.rgb(110, 255, 225)));
        int cyanSparkSize = Math.max(dp(4), dp(sizeDp * 0.06f));
        FrameLayout.LayoutParams sparkParams = new FrameLayout.LayoutParams(
                cyanSparkSize, cyanSparkSize, Gravity.TOP | Gravity.RIGHT);
        sparkParams.topMargin = dp(sizeDp * 0.11f);
        sparkParams.rightMargin = dp(sizeDp * 0.08f);
        medallion.addView(cyanSpark, sparkParams);

        View pinkSpark = new View(this);
        pinkSpark.setBackground(solidOval(Color.rgb(255, 124, 213)));
        int pinkSparkSize = Math.max(dp(3), dp(sizeDp * 0.04f));
        FrameLayout.LayoutParams pinkParams = new FrameLayout.LayoutParams(
                pinkSparkSize, pinkSparkSize, Gravity.BOTTOM | Gravity.LEFT);
        pinkParams.bottomMargin = dp(sizeDp * 0.13f);
        pinkParams.leftMargin = dp(sizeDp * 0.09f);
        medallion.addView(pinkSpark, pinkParams);
        return medallion;
    }

    private View buildColorBoundary() {
        LinearLayout line = new LinearLayout(this);
        line.setOrientation(LinearLayout.HORIZONTAL);
        line.setGravity(Gravity.CENTER);
        int[] colors = {Color.rgb(72, 229, 255), Color.rgb(142, 111, 255),
                Color.rgb(255, 102, 188)};
        int[] widths = {dp(34), dp(18), dp(34)};
        for (int i = 0; i < colors.length; i++) {
            View segment = new View(this);
            segment.setBackground(gradientRounded(new int[]{colors[i],
                    i == 0 ? Color.rgb(83, 145, 255)
                            : (i == 1 ? Color.rgb(195, 101, 255) : Color.rgb(255, 153, 188))},
                    dp(4)));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(widths[i], dp(3));
            params.leftMargin = dp(3);
            params.rightMargin = dp(3);
            line.addView(segment, params);
        }
        return line;
    }

    private View buildCreatorCard(boolean compact) {
        FrameLayout outer = new FrameLayout(this);
        outer.setBackground(gradientRounded(new int[]{
                Color.rgb(65, 218, 255), Color.rgb(124, 101, 255),
                Color.rgb(255, 99, 190)}, dp(23)));
        outer.setPadding(dp(1), dp(1), dp(1), dp(1));
        outer.setElevation(dp(5));

        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setGravity(Gravity.CENTER);
        inner.setPadding(dp(compact ? 8 : 18), dp(compact ? 6 : 20),
                dp(compact ? 8 : 18), dp(compact ? 6 : 20));
        inner.setBackground(gradientRounded(new int[]{
                Color.argb(245, 18, 24, 49), Color.argb(248, 12, 16, 35)}, dp(22)));
        outer.addView(inner, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        ColorFlowTextView overline = makeGradientText("CREATOR CREDIT", compact ? 8f : 10f,
                "monospace", android.graphics.Typeface.BOLD);
        overline.setLetterSpacing(0.2f);
        overline.setGravity(Gravity.CENTER);
        inner.addView(overline);

        ColorFlowTextView credit = makeGradientText("Created by Debarghya Sannigrahi",
                compact ? 10.5f : 15.5f,
                "monospace", android.graphics.Typeface.BOLD);
        credit.setGravity(Gravity.CENTER);
        credit.setLineSpacing(dp(compact ? 1 : 2), 1f);
        LinearLayout.LayoutParams creditParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        creditParams.topMargin = dp(compact ? 3 : 9);
        inner.addView(credit, creditParams);

        View boundary = new View(this);
        boundary.setBackground(gradientRounded(new int[]{
                Color.argb(0, 72, 229, 255), Color.argb(190, 111, 155, 255),
                Color.argb(0, 255, 102, 188)}, dp(2)));
        LinearLayout.LayoutParams boundaryParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
        boundaryParams.topMargin = dp(compact ? 4 : 16);
        inner.addView(boundary, boundaryParams);
        return outer;
    }

    private View buildInstructionControl() {
        FrameLayout outer = new FrameLayout(this);
        outer.setPadding(dp(1), dp(1), dp(1), dp(1));
        GradientDrawable border = gradientRounded(new int[]{
                Color.rgb(76, 234, 255), Color.rgb(133, 104, 255),
                Color.rgb(255, 107, 193)}, dp(28));
        outer.setBackground(new RippleDrawable(
                ColorStateList.valueOf(Color.argb(48, 255, 255, 255)), border, null));
        outer.setElevation(dp(10));
        outer.setFocusable(true);
        outer.setClickable(true);
        outer.setContentDescription("Instructions. Opens a blank popup screen.");

        LinearLayout inside = new LinearLayout(this);
        inside.setOrientation(LinearLayout.HORIZONTAL);
        inside.setGravity(Gravity.CENTER_VERTICAL);
        inside.setPadding(dp(13), dp(10), dp(17), dp(10));
        inside.setBackground(solidRounded(Color.argb(246, 20, 24, 49), dp(27)));

        TextView question = makeText("?", 15f, Color.WHITE, true);
        question.setGravity(Gravity.CENTER);
        question.setBackground(gradientRounded(new int[]{
                Color.rgb(60, 206, 255), Color.rgb(126, 107, 255)}, dp(14)));
        LinearLayout.LayoutParams questionParams = new LinearLayout.LayoutParams(dp(26), dp(26));
        inside.addView(question, questionParams);

        ColorFlowTextView label = makeGradientText("Instructions", 13.5f,
                "monospace", android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        labelParams.leftMargin = dp(9);
        inside.addView(label, labelParams);

        TextView arrow = makeText("↗", 15f, Color.rgb(120, 225, 255), true);
        LinearLayout.LayoutParams arrowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        arrowParams.leftMargin = dp(8);
        inside.addView(arrow, arrowParams);

        outer.addView(inside, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return outer;
    }

    private void showBlankInstructionPopup() {
        pausePageGradientAnimations();
        Dialog dialog = new Dialog(this);
        dialog.setOnDismissListener(ignored -> resumePageGradientAnimations());

        FrameLayout border = new FrameLayout(this);
        border.setPadding(dp(1), dp(1), dp(1), dp(1));
        border.setBackground(gradientRounded(new int[]{
                Color.rgb(76, 234, 255), Color.rgb(130, 101, 255),
                Color.rgb(255, 104, 194), Color.rgb(255, 205, 120)}, dp(28)));

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(15), dp(14), dp(15), dp(12));
        panel.setBackground(gradientRounded(new int[]{
                Color.rgb(12, 16, 36), Color.rgb(8, 11, 27)}, dp(27)));
        border.addView(panel, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        FixedGradientTextView heading = fixedGradientText("QUICK GUIDE", 14f, true);
        heading.setLetterSpacing(0.14f);
        header.addView(heading, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView close = makeText("×", 23f, Color.WHITE, true);
        close.setTypeface(android.graphics.Typeface.create(
                "monospace", android.graphics.Typeface.BOLD));
        close.setGravity(Gravity.CENTER);
        close.setContentDescription("Close instructions");
        close.setBackground(solidRounded(Color.argb(80, 108, 137, 193), dp(12)));
        close.setPadding(dp(8), 0, dp(8), 0);
        header.addView(close, new LinearLayout.LayoutParams(dp(38), dp(34)));
        panel.addView(header);
        close.setOnClickListener(v -> dialog.dismiss());

        View topBoundary = new View(this);
        topBoundary.setBackground(gradientRounded(new int[]{
                Color.rgb(76, 234, 255), Color.rgb(138, 112, 255),
                Color.rgb(255, 108, 195), Color.rgb(255, 202, 124)}, dp(2)));
        LinearLayout.LayoutParams boundaryParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(2));
        boundaryParams.topMargin = dp(10);
        boundaryParams.bottomMargin = dp(8);
        panel.addView(topBoundary, boundaryParams);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.setVerticalScrollBarEnabled(true);
        scroll.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        scroll.setClipToPadding(false);
        LinearLayout guide = new LinearLayout(this);
        guide.setOrientation(LinearLayout.VERTICAL);
        guide.setPadding(dp(2), dp(4), dp(2), dp(8));
        scroll.addView(guide, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        addGuideSection(guide, "BASIC NAVIGATION",
                "Tap an app icon to open that app.",
                "Open Home Studio: Hold an empty area on Home screen 1 for 2 seconds.",
                "Home screen 2: Hold an empty area to choose Add apps or Add widget.",
                "Swipe left or right to switch between the two Home screens.",
                "Swipe up on Home to open the App drawer.",
                "Swipe down on Home to open Control Centre.",
                "In the drawer, swipe down or press Back to close it.",
                "Tap the search control in the drawer to find an app.");

        addGuideSection(guide, "ADD, MOVE OR REMOVE APPS",
                "Add: Open the App drawer, hold an app, then choose Add to Home.",
                "Or open Home Studio > Manage layout > Add apps to Home.",
                "Move: Hold a Home icon for about 1 second, then drag it.",
                "Options: Keep holding the Home icon still for about 2 seconds.",
                "Remove from Home removes only the icon; the app stays installed.",
                "Uninstall removes the app from the device. Confirm in Android.",
                "Dock: Drag a Home icon to the bottom dock to pin it.",
                "Hold a dock icon to remove it from the dock or move it to Home.");

        addGuideSection(guide, "MOVE ITEMS BETWEEN SCREENS",
                "Start moving the icon, widget or shortcut.",
                "Drag to the left or right edge; pause until the other screen appears.",
                "Then drop the item in a free space. Avoid covering another item.");

        addGuideSection(guide, "WIDGETS & SHORTCUTS",
                "Open Home Studio > Widgets > Add widget.",
                "Choose a Widget or Shortcut from the list; finish its setup.",
                "You can also hold empty space on Home screen 2 to add one.",
                "Move a widget: Hold it for 1 second, then drag.",
                "Hold a widget still for about 2 seconds to open its options.",
                "Choose Resize widget, drag its edge handles, then tap outside.",
                "Choose Remove widget to delete it from Home.",
                "Contact shortcut: Hold it to move pages, remove or recreate it.",
                "Direct Dial shortcuts may ask for phone permission.");

        addGuideSection(guide, "CLOCK",
                "Move: Hold the clock for about 1 second, then drag it.",
                "Resize: Keep holding for about 2 seconds to show resize handles.",
                "Drag an edge to resize; tap outside the clock to finish.",
                "Home Studio > Clocks changes clock style, size, date and fonts.");

        addGuideSection(guide, "APP GROUPS",
                "Create: Home Studio > Widgets > Add an app group.",
                "Tap a group to open it. Hold the group to see its options.",
                "Options: Add apps, Remove apps, Rename, Share or Delete group.",
                "Hold an app inside a group to see its options.",
                "Remove from group returns that app to Home; it does not uninstall it.",
                "Choose Uninstall only to remove the app from the device.");

        addGuideSection(guide, "HOME STUDIO & RESET",
                "Home Studio changes the grid, icon size, shape, artwork and labels.",
                "It also changes clocks, wallpaper, colours and page/drawer effects.",
                "Reset Home layout restores the default Home layout and shortcuts.",
                "Reset all customizations resets the launcher's settings.");
 
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        panel.addView(scroll, scrollParams);

        FixedGradientTextView footer = fixedGradientText(
                "BACK / OUTSIDE TAP  →  CLOSE", 9.5f, true);
        footer.setGravity(Gravity.CENTER);
        footer.setLetterSpacing(0.06f);
        footer.setPadding(0, dp(10), 0, dp(1));
        panel.addView(footer, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        dialog.setContentView(border);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams attributes = window.getAttributes();
            attributes.dimAmount = 0.78f;
            attributes.gravity = Gravity.CENTER;
            window.setAttributes(attributes);

            int displayWidth = getResources().getDisplayMetrics().widthPixels;
            int displayHeight = getResources().getDisplayMetrics().heightPixels;
            int popupWidth = Math.max(dp(260),
                    Math.min(dp(460), displayWidth - dp(24)));
            // The former popup was 232dp tall. Double it, while respecting shorter displays.
            int popupHeight = Math.min(dp(464), Math.max(dp(280), displayHeight - dp(48)));
            window.setLayout(popupWidth, popupHeight);
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }

        border.setAlpha(0f);
        border.setScaleX(0.96f);
        border.setScaleY(0.96f);
        border.animate().alpha(1f).scaleX(1f).scaleY(1f)
                .setDuration(260)
                .setInterpolator(new DecelerateInterpolator(1.5f))
                .start();
    }

    private void pausePageGradientAnimations() {
        instructionPopupShowing = true;
        for (ColorFlowTextView text : pageGradientViews) {
            if (text != null && text.animator != null
                    && text.animator.isStarted() && !text.animator.isPaused()) {
                text.animator.pause();
            }
        }
    }

    private void resumePageGradientAnimations() {
        instructionPopupShowing = false;
        for (ColorFlowTextView text : pageGradientViews) {
            if (text != null && text.isAttachedToWindow()
                    && text.animator != null && text.animator.isPaused()) {
                text.animator.resume();
            }
        }
    }

    private void addGuideSection(LinearLayout parent, String title, String... lines) {
        FixedGradientTextView heading = fixedGradientText(title, 11.5f, true);
        heading.setLetterSpacing(0.12f);
        heading.setPadding(0, dp(8), 0, dp(4));
        parent.addView(heading, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        for (String line : lines) {
            FixedGradientTextView row = fixedGradientText("› " + line, 11f, false);
            row.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            row.setLineSpacing(dp(2), 1f);
            row.setMaxWidth(Math.max(dp(220),
                    getResources().getDisplayMetrics().widthPixels - dp(88)));
            row.setPadding(0, dp(2), 0, dp(2));
            parent.addView(row, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        View divider = new View(this);
        divider.setBackground(gradientRounded(new int[]{
                Color.argb(0, 76, 234, 255), Color.argb(165, 130, 101, 255),
                Color.argb(165, 255, 104, 194), Color.argb(0, 255, 205, 120)}, dp(2)));
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
        dividerParams.topMargin = dp(7);
        parent.addView(divider, dividerParams);
    }

    private FixedGradientTextView fixedGradientText(CharSequence value,
            float sizeSp, boolean bold) {
        FixedGradientTextView text = new FixedGradientTextView();
        text.setText(value);
        text.setTextSize(sizeSp);
        text.setTextColor(Color.WHITE);
        text.setIncludeFontPadding(false);
        text.setTypeface(android.graphics.Typeface.create(
                "monospace", bold ? android.graphics.Typeface.BOLD
                        : android.graphics.Typeface.NORMAL));
        text.setGravity(Gravity.CENTER_VERTICAL);
        return text;
    }

    /** Fixed multicolour glyph fill; unlike the page title, this gradient never moves. */
    private final class FixedGradientTextView extends TextView {
        FixedGradientTextView() {
            super(BlankActivity.this);
        }

        @Override
        protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
            super.onSizeChanged(width, height, oldWidth, oldHeight);
            applyFixedGradient(width);
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            applyFixedGradient(getWidth());
        }

        private void applyFixedGradient(int viewWidth) {
            int availableWidth = Math.max(1, viewWidth - getPaddingLeft() - getPaddingRight());
            float measuredTextWidth = getPaint().measureText(getText().toString());
            float shaderWidth = Math.max(1f, Math.min(availableWidth,
                    measuredTextWidth > 1f ? measuredTextWidth : availableWidth));
            getPaint().setShader(new LinearGradient(0f, 0f, shaderWidth, 0f,
                    new int[]{
                            Color.rgb(83, 246, 255),
                            Color.rgb(105, 157, 255),
                            Color.rgb(174, 126, 255),
                            Color.rgb(255, 112, 205),
                            Color.rgb(255, 207, 127)
                    }, null, Shader.TileMode.CLAMP));
            invalidate();
        }
    }

    private CharSequence getApplicationLabel() {
        try {
            return getPackageManager().getApplicationLabel(
                    getPackageManager().getApplicationInfo(getPackageName(), 0));
        } catch (PackageManager.NameNotFoundException ignored) {
            return "D.S Launcher";
        }
    }

    private String getVersionLabel() {
        try {
            String version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            return "VERSION  " + (version == null ? "18.7.0" : version);
        } catch (PackageManager.NameNotFoundException ignored) {
            return "VERSION  18.7.0";
        }
    }

    private TextView makeText(CharSequence value, float sizeSp, int color, boolean medium) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(sizeSp);
        text.setTextColor(color);
        text.setIncludeFontPadding(false);
        text.setTypeface(android.graphics.Typeface.create(
                "sans-serif", medium ? android.graphics.Typeface.BOLD
                        : android.graphics.Typeface.NORMAL));
        text.setGravity(Gravity.CENTER_VERTICAL);
        return text;
    }

    private ColorFlowTextView makeGradientText(CharSequence value, float sizeSp,
            String family, int style) {
        ColorFlowTextView text = new ColorFlowTextView(this);
        text.setText(value);
        text.setTextSize(sizeSp);
        text.setIncludeFontPadding(false);
        text.setTypeface(android.graphics.Typeface.create(family, style));
        text.setGravity(Gravity.CENTER_VERTICAL);
        text.setGradientColors(new int[]{
                Color.rgb(83, 246, 255),
                Color.rgb(105, 157, 255),
                Color.rgb(174, 126, 255),
                Color.rgb(255, 112, 205),
                Color.rgb(255, 207, 127),
                Color.rgb(83, 246, 255)
        });
        pageGradientViews.add(text);
        return text;
    }

    /**
     * Animated shader text: the colour blend moves through the glyph fill rather than
     * tinting the rectangular text view. Animation runs only while attached to the window.
     * This keeps the effect smooth without a permanently active animator when the page closes.
     */
    private final class ColorFlowTextView extends TextView {
        private final Matrix shaderMatrix = new Matrix();
        private int[] gradientColors = new int[]{
                Color.rgb(83, 246, 255), Color.rgb(105, 157, 255),
                Color.rgb(174, 126, 255), Color.rgb(255, 112, 205),
                Color.rgb(255, 207, 127), Color.rgb(83, 246, 255)
        };
        private LinearGradient textGradient;
        private ValueAnimator animator;
        private float shaderSpan = 1f;
        private float gradientPhase;

        ColorFlowTextView(Context context) {
            super(context);
            setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }

        void setGradientColors(int[] colors) {
            if (colors == null || colors.length < 2) return;
            gradientColors = colors.clone();
            rebuildGradient();
        }

        @Override
        protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
            super.onSizeChanged(width, height, oldWidth, oldHeight);
            rebuildGradient();
        }

        private void rebuildGradient() {
            int width = getWidth() - getPaddingLeft() - getPaddingRight();
            if (width <= 1) return;
            shaderSpan = Math.max(1f, width);
            textGradient = new LinearGradient(0f, 0f, shaderSpan, 0f,
                    gradientColors, null, Shader.TileMode.REPEAT);
            getPaint().setShader(textGradient);
            applyGradientPhase();
            invalidate();
        }

        private void applyGradientPhase() {
            if (textGradient == null) return;
            shaderMatrix.setTranslate(-gradientPhase * shaderSpan, 0f);
            textGradient.setLocalMatrix(shaderMatrix);
            invalidate();
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            rebuildGradient();
            if (animator == null) {
                animator = ValueAnimator.ofFloat(0f, 1f);
                animator.setDuration(6200L);
                animator.setRepeatCount(ValueAnimator.INFINITE);
                animator.setRepeatMode(ValueAnimator.RESTART);
                animator.setInterpolator(new LinearInterpolator());
                animator.addUpdateListener(animation -> {
                    Object animatedValue = animation.getAnimatedValue();
                    if (animatedValue instanceof Float) {
                        gradientPhase = (Float) animatedValue;
                        applyGradientPhase();
                    }
                });
            }
            if (!animator.isStarted()) animator.start();
            if (instructionPopupShowing && animator.isRunning() && !animator.isPaused()) {
                animator.pause();
            }
        }

        @Override
        protected void onDetachedFromWindow() {
            if (animator != null) animator.cancel();
            getPaint().setShader(null);
            textGradient = null;
            super.onDetachedFromWindow();
        }
    }

    private GradientDrawable gradientRounded(int[] colors, float radiusDp) {
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR, colors);
        drawable.setCornerRadius(dp(radiusDp));
        return drawable;
    }

    private GradientDrawable solidRounded(int color, float radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        return drawable;
    }

    private GradientDrawable solidOval(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setShape(GradientDrawable.OVAL);
        return drawable;
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    /**
     * Static aurora ribbons and layered radial light give the page colour without a
     * continuously invalidating animation, keeping it light on older Android devices.
     */
    private final class AuroraBackgroundView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        AuroraBackgroundView() {
            super(BlankActivity.this);
            setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float w = getWidth();
            float h = getHeight();
            if (w <= 0f || h <= 0f) return;

            paint.setStyle(Paint.Style.FILL);
            paint.setShader(new LinearGradient(0f, 0f, w * 0.78f, h,
                    new int[]{Color.rgb(7, 11, 27), Color.rgb(15, 17, 43),
                            Color.rgb(26, 13, 43)}, null, Shader.TileMode.CLAMP));
            canvas.drawRect(0f, 0f, w, h, paint);

            paint.setShader(new RadialGradient(w * 0.02f, h * 0.22f,
                    Math.max(w, h) * 0.62f,
                    new int[]{Color.argb(94, 26, 190, 255), Color.argb(35, 53, 95, 255),
                            Color.argb(0, 29, 44, 102)}, null, Shader.TileMode.CLAMP));
            canvas.drawRect(0f, 0f, w, h, paint);

            paint.setShader(new RadialGradient(w * 0.96f, h * 0.38f,
                    Math.max(w, h) * 0.57f,
                    new int[]{Color.argb(90, 160, 73, 255), Color.argb(35, 177, 55, 221),
                            Color.argb(0, 79, 23, 124)}, null, Shader.TileMode.CLAMP));
            canvas.drawRect(0f, 0f, w, h, paint);

            paint.setShader(new RadialGradient(w * 0.82f, h * 0.96f,
                    Math.max(w, h) * 0.45f,
                    new int[]{Color.argb(65, 255, 69, 177), Color.argb(16, 226, 45, 161),
                            Color.argb(0, 96, 20, 75)}, null, Shader.TileMode.CLAMP));
            canvas.drawRect(0f, 0f, w, h, paint);

            Path upperRibbon = new Path();
            upperRibbon.moveTo(-0.12f * w, 0.38f * h);
            upperRibbon.cubicTo(0.17f * w, 0.25f * h, 0.34f * w, 0.48f * h,
                    0.55f * w, 0.37f * h);
            upperRibbon.cubicTo(0.75f * w, 0.26f * h, 0.89f * w, 0.12f * h,
                    1.12f * w, 0.20f * h);
            upperRibbon.lineTo(1.12f * w, 0.28f * h);
            upperRibbon.cubicTo(0.86f * w, 0.20f * h, 0.72f * w, 0.39f * h,
                    0.53f * w, 0.46f * h);
            upperRibbon.cubicTo(0.30f * w, 0.59f * h, 0.13f * w, 0.34f * h,
                    -0.12f * w, 0.48f * h);
            upperRibbon.close();
            paint.setShader(new LinearGradient(0f, h * 0.18f, w, h * 0.47f,
                    new int[]{Color.argb(0, 52, 219, 255), Color.argb(35, 70, 222, 255),
                            Color.argb(25, 155, 117, 255), Color.argb(0, 255, 92, 190)},
                    null, Shader.TileMode.CLAMP));
            canvas.drawPath(upperRibbon, paint);

            Path lowerRibbon = new Path();
            lowerRibbon.moveTo(-0.1f * w, 0.76f * h);
            lowerRibbon.cubicTo(0.18f * w, 0.64f * h, 0.39f * w, 0.83f * h,
                    0.59f * w, 0.72f * h);
            lowerRibbon.cubicTo(0.80f * w, 0.60f * h, 0.95f * w, 0.71f * h,
                    1.1f * w, 0.58f * h);
            lowerRibbon.lineTo(1.1f * w, 0.65f * h);
            lowerRibbon.cubicTo(0.91f * w, 0.80f * h, 0.78f * w, 0.70f * h,
                    0.60f * w, 0.80f * h);
            lowerRibbon.cubicTo(0.34f * w, 0.95f * h, 0.15f * w, 0.76f * h,
                    -0.1f * w, 0.86f * h);
            lowerRibbon.close();
            paint.setShader(new LinearGradient(0f, h * 0.66f, w, h * 0.83f,
                    new int[]{Color.argb(0, 91, 123, 255), Color.argb(20, 70, 225, 255),
                            Color.argb(28, 157, 100, 255), Color.argb(0, 255, 91, 193)},
                    null, Shader.TileMode.CLAMP));
            canvas.drawPath(lowerRibbon, paint);

            paint.setShader(null);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(1f));
            paint.setColor(Color.argb(30, 154, 215, 255));
            Path contour = new Path();
            contour.moveTo(-0.08f * w, 0.52f * h);
            contour.cubicTo(0.22f * w, 0.41f * h, 0.40f * w, 0.62f * h,
                    0.60f * w, 0.49f * h);
            contour.cubicTo(0.79f * w, 0.37f * h, 0.95f * w, 0.44f * h,
                    1.08f * w, 0.34f * h);
            canvas.drawPath(contour, paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.WHITE);
            paint.setShader(null);
        }
    }
}
