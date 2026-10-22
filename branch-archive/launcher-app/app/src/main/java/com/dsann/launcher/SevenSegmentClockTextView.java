package com.dsann.launcher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.TextView;

/**
 * A real seven-segment LED renderer for the launcher clock.
 * TextView still owns the value and accessibility text; only the glyph rendering changes.
 */
public final class SevenSegmentClockTextView extends TextView {
    private final Paint segmentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private boolean sevenSegmentMode;
    private boolean segmentModeInitialized;
    private int segmentColor = Color.rgb(196, 200, 209);
    private boolean segmentGradientEnabled;
    private int segmentGradientWidth = -1;
    private int[] segmentGradientColors;
    private float segmentGradientPhase;
    private LinearGradient segmentGradient;
    private final Matrix segmentGradientMatrix = new Matrix();
    private boolean contrastInnerBorderEnabled;
    private float contrastInnerBorderWidth;
    private boolean secondaryInnerBorderEnabled;
    private int secondaryInnerBorderColor = Color.BLACK;
    // Actual Paint stroke width; half of this remains visible inside the glyph mask.
    private float secondaryInnerBorderWidth;
    private boolean digitOnlyInnerBorder;
    private int[] contrastBorderColors;
    private float[] contrastBorderStops;
    private LinearGradient segmentBorderGradient;
    private int segmentBorderGradientWidth = -1;
    private final Matrix segmentBorderGradientMatrix = new Matrix();

    // Shared horizontal coordinate space for gradients on every clock style.
    static int stableClockGradientSpanPx(Context context) {
        return Math.max(1, Math.round(320f
                * context.getResources().getDisplayMetrics().density));
    }

    // Preserve the preset's normal glow, but suppress it during the inner-border mask pass.
    private float configuredTextShadowRadius;
    private float configuredTextShadowDx;
    private float configuredTextShadowDy;
    private int configuredTextShadowColor = Color.TRANSPARENT;
    private LinearGradient textBorderGradient;
    private int textBorderGradientWidth = -1;
    private final float[][] segmentRects = new float[7][4];

    @Override public void setShadowLayer(float radius, float dx, float dy, int color) {
        configuredTextShadowRadius = radius;
        configuredTextShadowDx = dx;
        configuredTextShadowDy = dy;
        configuredTextShadowColor = color;
        super.setShadowLayer(radius, dx, dy, color);
    }

    public SevenSegmentClockTextView(Context context) {
        super(context);
        initialize();
    }

    public SevenSegmentClockTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    private void initialize() {
        setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        setSingleLine(true);
        setIncludeFontPadding(false);
        setWillNotDraw(false);
        // Keep the view on Android's normal rendering pipeline. A software layer
        // caches text/glyphs into a bitmap at the untransformed view size; the clock
        // is scaled for its length and breadth, which can make enlarged digits look
        // pixelated. The custom rounded segments are compatible with hardware drawing.
        segmentPaint.setStyle(Paint.Style.FILL);
    }

    @Override public void setTextColor(int color) {
        super.setTextColor(color);
        if (Color.alpha(color) != 0) segmentColor = color;
        invalidate();
    }

    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (segmentGradientEnabled && w > 0) {
            segmentGradientWidth = -1;
            ensureSegmentGradient();
        }
        if (contrastInnerBorderEnabled && sevenSegmentMode && w > 0) {
            segmentBorderGradientWidth = -1;
            ensureSegmentBorderGradient();
        }
        if (w != oldw) {
            textBorderGradient = null;
            textBorderGradientWidth = -1;
        }
        invalidate();
    }

    /**
     * Adds a second, solid-color inner contour alongside the existing contrast border.
     * innerWidthPx is the requested visible inner thickness; the outside half is masked.
     */
    public void setSecondaryInnerBorderEnabled(boolean enabled, int color, float innerWidthPx) {
        boolean nextEnabled = enabled && innerWidthPx > 0f;
        float nextStrokeWidth = nextEnabled ? Math.max(0.1f, innerWidthPx * 2f) : 0f;
        if (secondaryInnerBorderEnabled == nextEnabled
                && secondaryInnerBorderColor == color
                && Math.abs(secondaryInnerBorderWidth - nextStrokeWidth) < 0.01f) {
            return;
        }
        secondaryInnerBorderEnabled = nextEnabled;
        secondaryInnerBorderColor = color;
        secondaryInnerBorderWidth = nextStrokeWidth;
        invalidate();
    }

    public void setSegmentGradientEnabled(boolean enabled, float phase, int[] colors) {
        if (!enabled) {
            if (segmentGradientEnabled || segmentPaint.getShader() != null) {
                segmentGradientEnabled = false;
                segmentPaint.setShader(null);
                invalidate();
            }
            return;
        }
        if (colors == null || colors.length < 2) return;
        segmentGradientEnabled = true;
        segmentGradientPhase = phase - (float) Math.floor(phase);
        if (segmentGradientColors == null || !java.util.Arrays.equals(segmentGradientColors, colors)) {
            segmentGradientColors = colors.clone();
            segmentGradientWidth = -1;
        }
        ensureSegmentGradient();
        invalidate();
    }

    private void ensureSegmentGradient() {
        int width = stableClockGradientSpanPx(getContext());
        if (segmentGradient == null || segmentGradientWidth != width) {
            int[] colors = segmentGradientColors == null
                    ? new int[]{Color.rgb(77, 163, 255), Color.rgb(71, 240, 232),
                            Color.rgb(169, 139, 255), Color.rgb(255, 120, 191),
                            Color.rgb(217, 247, 255)}
                    : segmentGradientColors;
            segmentGradient = new LinearGradient(0f, 0f, width, 0f,
                    colors, null, Shader.TileMode.MIRROR);
            segmentGradientWidth = width;
        }
        segmentGradientMatrix.setTranslate(-segmentGradientPhase * width * 2f, 0f);
        segmentGradient.setLocalMatrix(segmentGradientMatrix);
        segmentPaint.setShader(segmentGradientEnabled ? segmentGradient : null);
    }

    /**
     * Adds a thin contrast line inside digit contours for Nothing Monochrome artwork.
     * The outline palette is the RGB inverse of the fill palette, so white receives black,
     * black receives white, and transition greys receive complementary greys.
     */
    public void setContrastInnerBorderEnabled(boolean enabled, int[] fillColors,
            float strokeWidthPx) {
        setContrastInnerBorderEnabled(enabled, fillColors, strokeWidthPx, null);
    }

    /**
     * Enables an inner glyph/segment border. An explicit border palette allows artwork
     * matching to keep the digit fill gradient independent from the white outline.
     */
    public void setContrastInnerBorderEnabled(boolean enabled, int[] fillColors,
            float strokeWidthPx, int[] explicitBorderColors) {
        int[] nextBorderColors = null;
        float[] nextBorderStops = null;
        if (enabled && explicitBorderColors != null && explicitBorderColors.length >= 2) {
            nextBorderColors = explicitBorderColors.clone();
        } else if (enabled && fillColors != null && fillColors.length >= 2) {
            if (isWhiteBlackWhitePalette(fillColors)) {
                // Reverse the current border polarity: white regions get black borders,
                // black regions get white borders. Keep the same transition thresholds.
                nextBorderColors = new int[] {
                        Color.BLACK, Color.BLACK, Color.WHITE, Color.WHITE,
                        Color.WHITE, Color.BLACK, Color.BLACK
                };
                nextBorderStops = new float[] {
                        0f, 0.04f, 0.453f, 0.5f, 0.547f, 0.96f, 1f
                };
            } else {
                // Preserve the incoming colour order for other palettes rather than
                // automatically inverting each border colour.
                nextBorderColors = fillColors.clone();
            }
        }

        boolean paletteChanged = !java.util.Arrays.equals(contrastBorderColors, nextBorderColors);
        boolean stopsChanged = !java.util.Arrays.equals(contrastBorderStops, nextBorderStops);
        float nextWidth = enabled ? Math.max(0.5f, strokeWidthPx) : 0f;
        boolean widthChanged = Math.abs(contrastInnerBorderWidth - nextWidth) > 0.01f;
        boolean nextEnabled = enabled && nextBorderColors != null;
        if (paletteChanged || stopsChanged || widthChanged) {
            contrastBorderColors = nextBorderColors;
            contrastBorderStops = nextBorderStops;
            segmentBorderGradient = null;
            segmentBorderGradientWidth = -1;
            textBorderGradient = null;
            textBorderGradientWidth = -1;
        }
        contrastInnerBorderEnabled = nextEnabled;
        contrastInnerBorderWidth = nextWidth;
        if (contrastInnerBorderEnabled && sevenSegmentMode && getWidth() > 0) {
            ensureSegmentBorderGradient();
        }
        invalidate();
    }

    /** Keeps the new gradient border on the numeric segments, not the colon dots. */
    void setDigitOnlyInnerBorder(boolean enabled) {
        if (digitOnlyInnerBorder == enabled) return;
        digitOnlyInnerBorder = enabled;
        invalidate();
    }

    private static boolean isWhiteBlackWhitePalette(int[] colors) {
        if (colors == null || colors.length < 3) return false;
        return isNearWhite(colors[0])
                && isNearBlack(colors[colors.length / 2])
                && isNearWhite(colors[colors.length - 1]);
    }

    private static boolean isNearWhite(int color) {
        return Color.red(color) >= 230 && Color.green(color) >= 230
                && Color.blue(color) >= 230;
    }

    private static boolean isNearBlack(int color) {
        return Color.red(color) <= 25 && Color.green(color) <= 25
                && Color.blue(color) <= 25;
    }

    private void ensureSegmentBorderGradient() {
        if (!contrastInnerBorderEnabled || contrastBorderColors == null) return;
        int width = Math.max(1, getWidth());
        if (segmentBorderGradient == null || segmentBorderGradientWidth != width) {
            segmentBorderGradient = new LinearGradient(0f, 0f, width, 0f,
                    contrastBorderColors, contrastBorderStops, Shader.TileMode.MIRROR);
            segmentBorderGradientWidth = width;
        }
        segmentBorderGradientMatrix.setTranslate(-segmentGradientPhase * width * 2f, 0f);
        segmentBorderGradient.setLocalMatrix(segmentBorderGradientMatrix);
    }

    /**
     * Typeface, tracking and font changes can alter glyph contours while keeping
     * almost the same measured advance. Explicitly invalidate the monochrome border
     * shader after a clock preset changes so its gradient is rebuilt for the new glyphs.
     */
    void invalidateContrastTextBorderCache() {
        textBorderGradient = null;
        textBorderGradientWidth = -1;
        invalidate();
    }

    private LinearGradient ensureTextBorderGradient() {
        if (!contrastInnerBorderEnabled || contrastBorderColors == null) return null;
        int width = stableClockGradientSpanPx(getContext());
        if (textBorderGradient == null || textBorderGradientWidth != width) {
            // This shader shares the fixed coordinate span and origin with the fill shader.
            textBorderGradient = new LinearGradient(0f, 0f,
                    width, 0f, contrastBorderColors, contrastBorderStops,
                    Shader.TileMode.MIRROR);
            textBorderGradientWidth = width;
        }
        return textBorderGradient;
    }

    public void setSevenSegmentMode(boolean enabled) {
        if (segmentModeInitialized && sevenSegmentMode == enabled) return;
        segmentModeInitialized = true;
        // Keep standard TextView measurement for every preset. A larger invisible
        // measurement font gives WRAP_CONTENT enough width for the custom LED
        // geometry; onDraw itself sizes the segments to the actual view bounds.
        float targetSize = enabled ? 110f : 78f;
        if (Math.abs(getTextSize() - targetSize * getResources().getDisplayMetrics().scaledDensity) > 0.5f) {
            setTextSize(targetSize);
        }
        if (enabled) {
            int current = getCurrentTextColor();
            if (Color.alpha(current) != 0) segmentColor = current;
            sevenSegmentMode = true;
            // Keep the normal text color as a visible fallback for unsupported
            // locale/time formats. In LED mode onDraw skips TextView glyphs and
            // paints the segments itself, so transparent text is unnecessary.
        } else {
            sevenSegmentMode = false;
            super.setTextColor(segmentColor);
        }
        // Do not re-enable a cached software bitmap when switching clock styles.
        // Let Android draw typography and the custom segment geometry at display scale.
        if (getLayerType() == android.view.View.LAYER_TYPE_SOFTWARE) {
            setLayerType(android.view.View.LAYER_TYPE_NONE, null);
        }
        invalidate();
        requestLayout();
    }

    @Override protected void onDraw(Canvas canvas) {
        if (!sevenSegmentMode) {
            boolean drawContrastBorder = contrastInnerBorderEnabled
                    && contrastBorderColors != null;
            boolean drawSecondaryBorder = secondaryInnerBorderEnabled
                    && secondaryInnerBorderWidth > 0f;
            if (!drawContrastBorder && !drawSecondaryBorder) {
                super.onDraw(canvas);
                return;
            }

            // Android 6's native-text two-digit-hour fallback deliberately avoids
            // ReplacementSpan glyphs. Draw its inner border per laid-out glyph instead
            // of relying on TextView's repeated onDraw + DST_IN layer pass, which can
            // lose the contour on API 23 after a package replacement.
            if (android.os.Build.VERSION.SDK_INT == android.os.Build.VERSION_CODES.M
                    && drawContrastBorder && !sevenSegmentMode) {
                super.onDraw(canvas);
                drawApi23NativeTextInnerBorder(canvas, drawSecondaryBorder);
                return;
            }

            // Draw the normal glyphs, then stroke them in a temporary layer. A glyph-shaped
            // DST_IN mask removes all stroke pixels outside the fill, keeping both contours
            // genuinely inside the text instead of producing a halo around each character.
            super.onDraw(canvas);
            Paint textPaint = getPaint();
            Paint.Style previousStyle = textPaint.getStyle();
            float previousStrokeWidth = textPaint.getStrokeWidth();
            Paint.Join previousJoin = textPaint.getStrokeJoin();
            Paint.Cap previousCap = textPaint.getStrokeCap();
            Shader previousShader = textPaint.getShader();
            android.graphics.Xfermode previousXfermode = textPaint.getXfermode();
            int previousColor = textPaint.getColor();
            float previousShadowRadius = configuredTextShadowRadius;
            float previousShadowDx = configuredTextShadowDx;
            float previousShadowDy = configuredTextShadowDy;
            int previousShadowColor = configuredTextShadowColor;
            int layer = canvas.saveLayer(0f, 0f, getWidth(), getHeight(), null);
            try {
                // Keep both strokes sharp; the regular text still retains its normal glow.
                textPaint.clearShadowLayer();
                textPaint.setXfermode(null);
                textPaint.setStyle(Paint.Style.STROKE);
                textPaint.setStrokeJoin(Paint.Join.ROUND);
                textPaint.setStrokeCap(Paint.Cap.ROUND);
                if (drawContrastBorder) {
                    textPaint.setStrokeWidth(contrastInnerBorderWidth);
                    textPaint.setShader(ensureTextBorderGradient());
                    super.onDraw(canvas);
                }
                if (drawSecondaryBorder) {
                    // Draw the fine black contour second, so it stays visible at the glyph
                    // edge while the remaining inner strip of the existing border is retained.
                    textPaint.setStrokeWidth(secondaryInnerBorderWidth);
                    textPaint.setShader(null);
                    textPaint.setColor(secondaryInnerBorderColor);
                    textPaint.setXfermode(null);
                    super.onDraw(canvas);
                }

                // Preserve only the stroke pixels within the filled glyph silhouette.
                textPaint.setStyle(Paint.Style.FILL);
                textPaint.setStrokeWidth(previousStrokeWidth);
                textPaint.setShader(null);
                textPaint.setColor(Color.WHITE);
                textPaint.setXfermode(new android.graphics.PorterDuffXfermode(
                        android.graphics.PorterDuff.Mode.DST_IN));
                super.onDraw(canvas);
            } finally {
                canvas.restoreToCount(layer);
                textPaint.setStyle(previousStyle);
                textPaint.setStrokeWidth(previousStrokeWidth);
                textPaint.setStrokeJoin(previousJoin);
                textPaint.setStrokeCap(previousCap);
                textPaint.setShader(previousShader);
                textPaint.setXfermode(previousXfermode);
                textPaint.setColor(previousColor);
                if (previousShadowRadius > 0f && Color.alpha(previousShadowColor) != 0) {
                    textPaint.setShadowLayer(previousShadowRadius, previousShadowDx,
                            previousShadowDy, previousShadowColor);
                } else {
                    textPaint.clearShadowLayer();
                }
            }
            return;
        }

        String raw = getText() == null ? "" : getText().toString();
        StringBuilder display = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            char ch = raw.charAt(i);
            int digit = Character.digit(ch, 10);
            if (digit >= 0) {
                display.append((char) ('0' + digit));
            } else if (ch == ':' || ch == '\uFF1A' || ch == '.' || ch == '\u2236') {
                // Reserve the first hour digit cell when the hour is 1–9. Leave it
                // visually blank (not a zero) so the time and AM/PM edge stay aligned
                // with the two-digit 10–12 hour layout.
                if (display.length() == 1) display.insert(0, ' ');
                // Normalize common locale-specific time separators for LED rendering.
                display.append(':');
            }
        }
        String value = display.toString();
        // Never leave the clock invisible if an unexpected format reaches this view.
        if (value.length() == 0) {
            super.onDraw(canvas);
            return;
        }

        float leftBound = getPaddingLeft();
        float rightBound = getWidth() - getPaddingRight();
        float topBound = getPaddingTop();
        float bottomBound = getHeight() - getPaddingBottom();
        float availableWidth = Math.max(1f, rightBound - leftBound);
        float availableHeight = Math.max(1f, bottomBound - topBound);
        float dp = getResources().getDisplayMetrics().density;

        int digits = 0;
        int colons = 0;
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) == ':') colons++;
            else digits++;
        }
        if (digits == 0) {
            super.onDraw(canvas);
            return;
        }

        // Reserve the fixed colon/minute gaps in the width budget. Without this, the
        // hour group can extend beyond the left edge when the clock is resized narrow.
        float widthUnits = digits * 0.72f + colons * 0.34f;
        float fixedGapPx = colons > 0 ? 30f * dp : 0f;
        float usableGlyphWidth = Math.max(1f, availableWidth - fixedGapPx);
        float unit = Math.min(availableHeight * 0.91f, usableGlyphWidth / widthUnits);
        float digitWidth = unit * 0.56f;
        float digitHeight = unit * 0.92f;
        float advance = unit * 0.72f;
        float totalWidth = digits * advance + colons * unit * 0.34f;
        int colonIndex = value.indexOf(':');
        // Keep all digits inside the widget. The width budget above already includes
        // the colon and minute-group gaps, so no portion of the first digit is clipped.
        float baseDotX = leftBound + availableWidth * 0.5f - unit * 0.28f;
        // Keep the hour digits anchored where they were. Move the colon 15dp
        // right, then leave a further 15dp gap before the minute group.
        float hourStartX = colonIndex >= 0
                ? baseDotX - unit * 0.17f - (colonIndex * advance)
                : leftBound + (availableWidth - totalWidth) / 2f;
        float dotX = baseDotX + 15f * dp;
        float minuteStartX = dotX + unit * 0.17f + 15f * dp;
        int horizontalGravity = getGravity() & Gravity.HORIZONTAL_GRAVITY_MASK;
        if (colonIndex < 0 && horizontalGravity == Gravity.LEFT) hourStartX = leftBound;
        else if (colonIndex < 0 && horizontalGravity == Gravity.RIGHT) {
            hourStartX = rightBound - totalWidth;
        }
        float x = hourStartX;
        float y = topBound + (availableHeight - digitHeight) / 2f;
        float thickness = Math.max(3f, digitWidth * 0.17f);
        float half = thickness / 2f;
        float mid = y + digitHeight / 2f;
        int color = segmentColor;

        canvas.save();
        canvas.clipRect(leftBound, topBound, rightBound, bottomBound);
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == ':') {
                segmentPaint.setStyle(Paint.Style.FILL);
                segmentPaint.setShader(segmentGradientEnabled ? segmentGradient : null);
                segmentPaint.setColor(segmentGradientEnabled ? Color.WHITE : color);
                // Keep the doubled dot radius, but reduce center-to-center
                // separation by 40% from the previous doubled spacing.
                float radius = 2f * Math.max(2.5f, thickness * 0.38f);
                float colonCenterY = y + digitHeight * 0.51f;
                float colonHalfSpread = digitHeight * 0.204f;
                float upperDotY = colonCenterY - colonHalfSpread;
                float lowerDotY = colonCenterY + colonHalfSpread;
                canvas.drawCircle(dotX, upperDotY, radius, segmentPaint);
                canvas.drawCircle(dotX, lowerDotY, radius, segmentPaint);
                if (!digitOnlyInnerBorder && contrastInnerBorderEnabled
                        && segmentBorderGradient != null) {
                    float insetRadius = Math.max(0f, radius - contrastInnerBorderWidth * 0.5f);
                    segmentPaint.setStyle(Paint.Style.STROKE);
                    segmentPaint.setStrokeWidth(contrastInnerBorderWidth);
                    segmentPaint.setShader(segmentBorderGradient);
                    segmentPaint.setColor(Color.WHITE);
                    canvas.drawCircle(dotX, upperDotY, insetRadius, segmentPaint);
                    canvas.drawCircle(dotX, lowerDotY, insetRadius, segmentPaint);
                    segmentPaint.setStyle(Paint.Style.FILL);
                    segmentPaint.setStrokeWidth(0f);
                }
                x = minuteStartX;
                continue;
            }

            if (colonIndex >= 0 && i > colonIndex) {
                int minuteDigitIndex = i - colonIndex - 1;
                x = minuteStartX + minuteDigitIndex * advance;
            }
            float l = x;
            float r = x + digitWidth;
            float t = y;
            float b = y + digitHeight;
            float m = mid;
            float[][] segments = segmentRects;
            setSegment(segments[0], l + half, t, r - half, t + thickness);
            setSegment(segments[1], r - thickness, t + half, r, m - half);
            setSegment(segments[2], r - thickness, m + half, r, b - half);
            setSegment(segments[3], l + half, b - thickness, r - half, b);
            setSegment(segments[4], l, m + half, l + thickness, b - half);
            setSegment(segments[5], l, t + half, l + thickness, m - half);
            setSegment(segments[6], l + half, m - half, r - half, m + half);
            int litMask = segmentMask(ch);
            for (int j = 0; j < segments.length; j++) {
                float[] seg = segments[j];
                boolean lit = (litMask & (1 << j)) != 0;
                // Do not draw unlit segments at all. Even a very low-alpha fill leaves
                // faint ghost bars visible on dark backgrounds.
                if (!lit) continue;
                segmentPaint.setStyle(Paint.Style.FILL);
                segmentPaint.setShader(segmentGradientEnabled ? segmentGradient : null);
                segmentPaint.setColor(segmentGradientEnabled ? Color.WHITE : color);
                canvas.drawRoundRect(seg[0], seg[1], seg[2], seg[3],
                        thickness * 0.45f, thickness * 0.45f, segmentPaint);

                if (contrastInnerBorderEnabled && segmentBorderGradient != null) {
                    // Inset the stroke by half its width so the outline stays inside the
                    // rounded segment, instead of growing the digit's visible bounds.
                    float inset = contrastInnerBorderWidth * 0.5f;
                    float innerLeft = seg[0] + inset;
                    float innerTop = seg[1] + inset;
                    float innerRight = seg[2] - inset;
                    float innerBottom = seg[3] - inset;
                    if (innerRight > innerLeft && innerBottom > innerTop) {
                        segmentPaint.setStyle(Paint.Style.STROKE);
                        segmentPaint.setStrokeWidth(contrastInnerBorderWidth);
                        segmentPaint.setShader(segmentBorderGradient);
                        segmentPaint.setColor(Color.WHITE);
                        float innerRadius = Math.max(0f, thickness * 0.45f - inset);
                        canvas.drawRoundRect(innerLeft, innerTop, innerRight, innerBottom,
                                innerRadius, innerRadius, segmentPaint);
                        segmentPaint.setStyle(Paint.Style.FILL);
                        segmentPaint.setStrokeWidth(0f);
                    }
                }
            }
            x += advance;
        }
        segmentPaint.setStyle(Paint.Style.FILL);
        segmentPaint.setStrokeWidth(0f);
        segmentPaint.setShader(segmentGradientEnabled ? segmentGradient : null);
        canvas.restore();
    }

    private final android.graphics.Path api23NativeGlyphPath = new android.graphics.Path();
    // Paint.getTextPath's API-23-compatible overload accepts a char array rather than
    // CharSequence. Reuse the one-character buffer to avoid per-glyph allocations.
    private final char[] api23NativeGlyphChars = new char[1];
    private final android.text.TextPaint api23NativeOutlinePaint =
            new android.text.TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final android.text.TextPaint api23NativeSecondaryPaint =
            new android.text.TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final android.text.TextPaint api23NativeMaskPaint =
            new android.text.TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final android.graphics.Path api23NativeGlyphMask = new android.graphics.Path();

    /**
     * API-23-safe inner contour for ordinary TextView glyphs. It follows Layout's real
     * horizontal positions, applies drawing-only spans (including the shrunken native
     * colon), and clips each centered stroke to its glyph outline so the border stays
     * inside the fill without needing a destination-in bitmap mask.
     */
    private void drawApi23NativeTextInnerBorder(Canvas canvas, boolean drawSecondaryBorder) {
        android.text.Layout layout = getLayout();
        CharSequence value = getText();
        if (layout == null || value == null || value.length() == 0
                || layout.getLineCount() == 0 || contrastInnerBorderWidth <= 0f
                || getWidth() <= 0 || getHeight() <= 0) return;

        android.text.Spanned spanned = value instanceof android.text.Spanned
                ? (android.text.Spanned) value : null;
        float layoutOriginX = getCompoundPaddingLeft() - getScrollX();
        int firstBaseline = layout.getLineBaseline(0);
        int viewBaseline = getBaseline();

        // Android 6 can lose DST_IN contours on a TextView layer after PACKAGE_REPLACED.
        // Build the centered stroke geometry in software paths, intersect it with each
        // glyph's actual fill silhouette, then draw that inner strip as a normal fill path.
        // This avoids per-frame saveLayer compositing and guarantees the contour stays inside.
        for (int line = 0; line < layout.getLineCount(); line++) {
            int start = layout.getLineStart(line);
            int end = Math.min(value.length(), layout.getLineEnd(line));
            float baseline = viewBaseline + layout.getLineBaseline(line) - firstBaseline;
            for (int index = start; index < end; index++) {
                char ch = value.charAt(index);
                if (Character.isWhitespace(ch) || Character.isSpaceChar(ch)
                        || ch == '\n' || ch == '\r') continue;

                api23NativeOutlinePaint.set(getPaint());
                api23NativeOutlinePaint.baselineShift = getPaint().baselineShift;
                if (spanned != null) {
                    android.text.style.CharacterStyle[] styles = spanned.getSpans(
                            index, index + 1, android.text.style.CharacterStyle.class);
                    for (android.text.style.CharacterStyle style : styles) {
                        style.updateDrawState(api23NativeOutlinePaint);
                    }
                }

                float x = layoutOriginX + layout.getPrimaryHorizontal(index);
                float y = baseline + api23NativeOutlinePaint.baselineShift;
                api23NativeGlyphPath.reset();
                api23NativeOutlinePaint.getTextPath(
                        value.toString(), index, index + 1, x, y, api23NativeGlyphPath);
                if (api23NativeGlyphPath.isEmpty()) continue;

                api23NativeOutlinePaint.setColor(Color.WHITE);
                api23NativeOutlinePaint.setShader(null);
                api23NativeOutlinePaint.setXfermode(null);
                api23NativeOutlinePaint.setStyle(Paint.Style.STROKE);
                api23NativeOutlinePaint.setStrokeWidth(contrastInnerBorderWidth);
                api23NativeOutlinePaint.setStrokeJoin(Paint.Join.ROUND);
                api23NativeOutlinePaint.setStrokeCap(Paint.Cap.ROUND);
                api23NativeOutlinePaint.clearShadowLayer();

                api23NativeGlyphMask.reset();
                boolean hasInnerContour = api23NativeOutlinePaint.getFillPath(
                        api23NativeGlyphPath, api23NativeGlyphMask);
                if (hasInnerContour) {
                    hasInnerContour = api23NativeGlyphMask.op(
                            api23NativeGlyphPath, android.graphics.Path.Op.INTERSECT);
                }
                if (hasInnerContour) {
                    api23NativeMaskPaint.set(api23NativeOutlinePaint);
                    api23NativeMaskPaint.setStyle(Paint.Style.FILL);
                    api23NativeMaskPaint.setStrokeWidth(0f);
                    api23NativeMaskPaint.setColor(Color.WHITE);
                    api23NativeMaskPaint.setShader(null);
                    api23NativeMaskPaint.setXfermode(null);
                    api23NativeMaskPaint.clearShadowLayer();
                    canvas.drawPath(api23NativeGlyphMask, api23NativeMaskPaint);
                }

                if (drawSecondaryBorder && secondaryInnerBorderEnabled
                        && secondaryInnerBorderWidth > 0f) {
                    api23NativeSecondaryPaint.set(api23NativeOutlinePaint);
                    api23NativeSecondaryPaint.setShader(null);
                    api23NativeSecondaryPaint.setXfermode(null);
                    api23NativeSecondaryPaint.setStyle(Paint.Style.STROKE);
                    api23NativeSecondaryPaint.setStrokeWidth(secondaryInnerBorderWidth);
                    api23NativeSecondaryPaint.setColor(secondaryInnerBorderColor);
                    api23NativeSecondaryPaint.setStrokeJoin(Paint.Join.ROUND);
                    api23NativeSecondaryPaint.setStrokeCap(Paint.Cap.ROUND);
                    api23NativeSecondaryPaint.clearShadowLayer();

                    api23NativeGlyphMask.reset();
                    boolean hasSecondaryContour = api23NativeSecondaryPaint.getFillPath(
                            api23NativeGlyphPath, api23NativeGlyphMask);
                    if (hasSecondaryContour) {
                        hasSecondaryContour = api23NativeGlyphMask.op(
                                api23NativeGlyphPath, android.graphics.Path.Op.INTERSECT);
                    }
                    if (hasSecondaryContour) {
                        api23NativeSecondaryPaint.setStyle(Paint.Style.FILL);
                        api23NativeSecondaryPaint.setStrokeWidth(0f);
                        canvas.drawPath(api23NativeGlyphMask, api23NativeSecondaryPaint);
                    }
                }
            }
        }
    }

    private static void setSegment(float[] out, float left, float top, float right, float bottom) {
        out[0] = left;
        out[1] = top;
        out[2] = right;
        out[3] = bottom;
    }

    // Bit positions map to segments A through G; returning a mask avoids allocating
    // a new boolean array for each digit during every redraw.
    private int segmentMask(char digit) {
        switch (digit) {
            case '0': return 63;
            case '1': return 6;
            case '2': return 91;
            case '3': return 79;
            case '4': return 102;
            case '5': return 109;
            case '6': return 125;
            case '7': return 7;
            case '8': return 127;
            case '9': return 111;
            default: return 0;
        }
    }
}
