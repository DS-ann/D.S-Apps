package com.dsann.launcher;

import android.app.Activity;
import android.app.Dialog;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
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
        contentScroll.setPadding(dp(24), dp(34), dp(24), dp(104));

        LinearLayout stack = new LinearLayout(this);
        stack.setOrientation(LinearLayout.VERTICAL);
        stack.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
        stack.setPadding(dp(2), dp(24), dp(2), dp(22));
        contentScroll.addView(stack, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(contentScroll, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        stack.addView(buildIconMedallion(), new LinearLayout.LayoutParams(dp(132), dp(132)));

        TextView appName = makeText(getApplicationLabel(), 34f, Color.WHITE, true);
        appName.setGravity(Gravity.CENTER);
        appName.setShadowLayer(dp(18), 0f, dp(3), Color.argb(115, 109, 91, 255));
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleParams.topMargin = dp(20);
        stack.addView(appName, titleParams);

        stack.addView(buildColorBoundary(), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(3)) {{
                    topMargin = dp(17);
                }});

        TextView version = makeText(getVersionLabel(), 10.5f, Color.rgb(177, 223, 255), true);
        version.setLetterSpacing(0.13f);
        version.setGravity(Gravity.CENTER);
        version.setPadding(dp(14), dp(8), dp(14), dp(8));
        version.setBackground(solidRounded(Color.argb(135, 25, 37, 70), dp(22)));
        LinearLayout.LayoutParams versionParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        versionParams.topMargin = dp(15);
        stack.addView(version, versionParams);

        int availableWidth = getResources().getDisplayMetrics().widthPixels - dp(56);
        int cardWidth = Math.max(dp(260), Math.min(dp(440), availableWidth));
        LinearLayout.LayoutParams creatorParams = new LinearLayout.LayoutParams(
                cardWidth, ViewGroup.LayoutParams.WRAP_CONTENT);
        creatorParams.topMargin = dp(31);
        stack.addView(buildCreatorCard(), creatorParams);

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
            contentScroll.setPadding(dp(24), topInset + dp(18), dp(24), bottomInset + dp(104));
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

    private FrameLayout buildIconMedallion() {
        FrameLayout medallion = new FrameLayout(this);
        GradientDrawable halo = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(75, 233, 255), Color.rgb(125, 94, 255),
                        Color.rgb(255, 91, 190)});
        halo.setShape(GradientDrawable.OVAL);
        medallion.setBackground(halo);
        medallion.setElevation(dp(10));

        FrameLayout darkRing = new FrameLayout(this);
        darkRing.setBackground(solidOval(Color.rgb(15, 18, 43)));
        FrameLayout.LayoutParams ringParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        ringParams.setMargins(dp(4), dp(4), dp(4), dp(4));
        medallion.addView(darkRing, ringParams);

        ImageView appIcon = new ImageView(this);
        appIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        appIcon.setContentDescription(getApplicationLabel() + " app icon");
        appIcon.setBackground(gradientRounded(new int[]{
                Color.rgb(35, 48, 85), Color.rgb(27, 27, 62)}, dp(26)));
        appIcon.setPadding(dp(6), dp(6), dp(6), dp(6));
        try {
            appIcon.setImageDrawable(getPackageManager().getApplicationIcon(getPackageName()));
        } catch (PackageManager.NameNotFoundException ignored) {
            appIcon.setImageResource(R.mipmap.ic_launcher);
        }
        FrameLayout.LayoutParams iconParams = new FrameLayout.LayoutParams(dp(88), dp(88),
                Gravity.CENTER);
        darkRing.addView(appIcon, iconParams);

        View cyanSpark = new View(this);
        cyanSpark.setBackground(solidOval(Color.rgb(110, 255, 225)));
        FrameLayout.LayoutParams sparkParams = new FrameLayout.LayoutParams(dp(8), dp(8),
                Gravity.TOP | Gravity.RIGHT);
        sparkParams.topMargin = dp(15);
        sparkParams.rightMargin = dp(11);
        medallion.addView(cyanSpark, sparkParams);

        View pinkSpark = new View(this);
        pinkSpark.setBackground(solidOval(Color.rgb(255, 124, 213)));
        FrameLayout.LayoutParams pinkParams = new FrameLayout.LayoutParams(dp(5), dp(5),
                Gravity.BOTTOM | Gravity.LEFT);
        pinkParams.bottomMargin = dp(17);
        pinkParams.leftMargin = dp(12);
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

    private View buildCreatorCard() {
        FrameLayout outer = new FrameLayout(this);
        outer.setBackground(gradientRounded(new int[]{
                Color.rgb(65, 218, 255), Color.rgb(124, 101, 255),
                Color.rgb(255, 99, 190)}, dp(23)));
        outer.setPadding(dp(1), dp(1), dp(1), dp(1));
        outer.setElevation(dp(5));

        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setGravity(Gravity.CENTER);
        inner.setPadding(dp(18), dp(20), dp(18), dp(20));
        inner.setBackground(gradientRounded(new int[]{
                Color.argb(245, 18, 24, 49), Color.argb(248, 12, 16, 35)}, dp(22)));
        outer.addView(inner, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView overline = makeText("CREATOR CREDIT", 10f,
                Color.rgb(116, 228, 255), true);
        overline.setLetterSpacing(0.2f);
        overline.setGravity(Gravity.CENTER);
        inner.addView(overline);

        TextView credit = makeText("Created by Debarghya Sannigrahi", 15.5f,
                Color.rgb(248, 247, 255), true);
        credit.setGravity(Gravity.CENTER);
        credit.setLineSpacing(dp(2), 1f);
        LinearLayout.LayoutParams creditParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        creditParams.topMargin = dp(9);
        inner.addView(credit, creditParams);

        View boundary = new View(this);
        boundary.setBackground(gradientRounded(new int[]{
                Color.argb(0, 72, 229, 255), Color.argb(190, 111, 155, 255),
                Color.argb(0, 255, 102, 188)}, dp(2)));
        LinearLayout.LayoutParams boundaryParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
        boundaryParams.topMargin = dp(16);
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

        TextView label = makeText("Instructions", 13.5f,
                Color.rgb(248, 247, 255), true);
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
        Dialog dialog = new Dialog(this);
        FrameLayout panel = new FrameLayout(this);
        panel.setPadding(dp(1), dp(1), dp(1), dp(1));
        panel.setBackground(gradientRounded(new int[]{
                Color.rgb(68, 230, 255), Color.rgb(131, 99, 255),
                Color.rgb(255, 96, 190)}, dp(30)));
        panel.setContentDescription("Blank instructions popup. Press Back or tap outside to close.");
        panel.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);

        View blankSurface = new View(this);
        blankSurface.setBackground(gradientRounded(new int[]{
                Color.rgb(12, 16, 36), Color.rgb(9, 12, 29)}, dp(29)));
        panel.addView(blankSurface, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        dialog.setContentView(panel);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams attributes = window.getAttributes();
            attributes.dimAmount = 0.76f;
            window.setAttributes(attributes);
            int displayWidth = getResources().getDisplayMetrics().widthPixels;
            int popupWidth = Math.max(dp(260), Math.min(dp(460), displayWidth - dp(44)));
            window.setLayout(popupWidth, dp(232));
        }

        panel.setAlpha(0f);
        panel.setScaleX(0.9f);
        panel.setScaleY(0.9f);
        panel.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(280)
                .setInterpolator(new DecelerateInterpolator(1.6f))
                .start();
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
