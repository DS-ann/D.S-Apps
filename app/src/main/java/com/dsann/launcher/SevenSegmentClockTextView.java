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
    private int[] contrastBorderColors;
    private LinearGradient segmentBorderGradient;
    private int segmentBorderGradientWidth = -1;
    private final Matrix segmentBorderGradientMatrix = new Matrix();
    private LinearGradient textBorderGradient;
    private int textBorderGradientWidth = -1;
    private float textBorderSpanWidth = -1f;
    private String textBorderValue = "";
    private final float[][] segmentRects = new float[7][4];

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
        int width = Math.max(1, getWidth());
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
        int[] nextBorderColors = null;
        if (enabled && fillColors != null && fillColors.length >= 2) {
            nextBorderColors = new int[fillColors.length];
            for (int i = 0; i < fillColors.length; i++) {
                int color = fillColors[i];
                nextBorderColors[i] = Color.argb(Color.alpha(color),
                        255 - Color.red(color), 255 - Color.green(color),
                        255 - Color.blue(color));
            }
        }

        boolean paletteChanged = !java.util.Arrays.equals(contrastBorderColors, nextBorderColors);
        float nextWidth = enabled ? Math.max(0.5f, strokeWidthPx) : 0f;
        boolean widthChanged = Math.abs(contrastInnerBorderWidth - nextWidth) > 0.01f;
        boolean nextEnabled = enabled && nextBorderColors != null;
        if (paletteChanged || widthChanged) {
            contrastBorderColors = nextBorderColors;
            segmentBorderGradient = null;
            segmentBorderGradientWidth = -1;
            textBorderGradient = null;
            textBorderGradientWidth = -1;
            textBorderSpanWidth = -1f;
            textBorderValue = "";
        }
        contrastInnerBorderEnabled = nextEnabled;
        contrastInnerBorderWidth = nextWidth;
        if (contrastInnerBorderEnabled && sevenSegmentMode && getWidth() > 0) {
            ensureSegmentBorderGradient();
        }
        invalidate();
    }

    private void ensureSegmentBorderGradient() {
        if (!contrastInnerBorderEnabled || contrastBorderColors == null) return;
        int width = Math.max(1, getWidth());
        if (segmentBorderGradient == null || segmentBorderGradientWidth != width) {
            segmentBorderGradient = new LinearGradient(0f, 0f, width, 0f,
                    contrastBorderColors, null, Shader.TileMode.MIRROR);
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
        textBorderSpanWidth = -1f;
        textBorderValue = "";
        invalidate();
    }

    private LinearGradient ensureTextBorderGradient() {
        if (!contrastInnerBorderEnabled || contrastBorderColors == null) return null;
        int width = Math.max(1, getWidth());
        String value = getText() == null ? "" : getText().toString();
        float measured = getPaint().measureText(value);
        float span = Math.max(1f, Math.min((float) width,
                measured > 0f ? measured : (float) width));
        if (textBorderGradient == null || textBorderGradientWidth != width
                || Math.abs(textBorderSpanWidth - span) > 1f
                || !textBorderValue.equals(value)) {
            float startX = (width - span) * 0.5f;
            textBorderGradient = new LinearGradient(startX, 0f,
                    startX + span, 0f, contrastBorderColors, null, Shader.TileMode.MIRROR);
            textBorderGradientWidth = width;
            textBorderSpanWidth = span;
            textBorderValue = value;
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
            if (!contrastInnerBorderEnabled || contrastBorderColors == null) {
                super.onDraw(canvas);
                return;
            }

            // Draw the normal glyphs, then create a stroked copy in a temporary layer.
            // A glyph-shaped DST_IN mask removes the half of the stroke outside the text,
            // leaving a thin inner contour rather than a halo around each character.
            super.onDraw(canvas);
            Paint textPaint = getPaint();
            Paint.Style previousStyle = textPaint.getStyle();
            float previousStrokeWidth = textPaint.getStrokeWidth();
            Paint.Join previousJoin = textPaint.getStrokeJoin();
            Paint.Cap previousCap = textPaint.getStrokeCap();
            Shader previousShader = textPaint.getShader();
            android.graphics.Xfermode previousXfermode = textPaint.getXfermode();
            int previousColor = textPaint.getColor();
            int layer = canvas.saveLayer(0f, 0f, getWidth(), getHeight(), null);
            try {
                textPaint.setXfermode(null);
                textPaint.setStyle(Paint.Style.STROKE);
                textPaint.setStrokeWidth(contrastInnerBorderWidth);
                textPaint.setStrokeJoin(Paint.Join.ROUND);
                textPaint.setStrokeCap(Paint.Cap.ROUND);
                textPaint.setShader(ensureTextBorderGradient());
                super.onDraw(canvas);

                // Keep only outline pixels that fall within the filled glyph silhouette.
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

        // Centre the actual rendered glyphs as one unit. Each digit advances by
        // 0.72 units, while its visible ink is 0.56 units wide. The colon occupies
        // its own separator slot with 15dp of breathing room on either side.
        float dp = getResources().getDisplayMetrics().density;
        float fixedColonGap = 30f * dp;
        float widthUnits = Math.max(0.01f,
                (digits - 1) * 0.72f + colons * 0.34f + 0.56f);
        float availableGlyphWidth = Math.max(1f, availableWidth - colons * fixedColonGap);
        float unit = Math.min(availableHeight * 0.91f, availableGlyphWidth / widthUnits);
        float digitWidth = unit * 0.56f;
        float digitHeight = unit * 0.92f;
        float advance = unit * 0.72f;
        float colonAdvance = unit * 0.34f + fixedColonGap;
        float totalWidth = Math.max(1f, (digits - 1) * advance
                + colons * colonAdvance + digitWidth);
        float x = leftBound + (availableWidth - totalWidth) / 2f;
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
                float radius = Math.max(2.5f, thickness * 0.38f);
                // The colon follows the hour inside its own slot; it is not the
                // centring anchor for the entire time.
                float dotX = x + colonAdvance * 0.5f;
                float upperDotY = y + digitHeight * 0.34f;
                float lowerDotY = y + digitHeight * 0.68f;
                canvas.drawCircle(dotX, upperDotY, radius, segmentPaint);
                canvas.drawCircle(dotX, lowerDotY, radius, segmentPaint);
                if (contrastInnerBorderEnabled && segmentBorderGradient != null) {
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
                x += colonAdvance;
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
