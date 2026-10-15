package com.dsann.launcher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
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
    private int segmentColor = Color.WHITE;

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
        // The renderer uses filled rounded segments (not hardware-sensitive line effects).
        setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null);
        segmentPaint.setStyle(Paint.Style.FILL);
    }

    public void setSevenSegmentMode(boolean enabled) {
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
        setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null);
        invalidate();
        requestLayout();
    }

    @Override protected void onDraw(Canvas canvas) {
        if (!sevenSegmentMode) {
            super.onDraw(canvas);
            return;
        }

        String raw = getText() == null ? "" : getText().toString();
        StringBuilder display = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            char ch = raw.charAt(i);
            int digit = Character.digit(ch, 10);
            if (digit >= 0) display.append((char) ('0' + digit));
            else if (ch == ':' || ch == '\uFF1A') display.append(':');
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

        // The whole string is sized as one unit, so 12-hour and 24-hour formats fit.
        float widthUnits = digits * 0.72f + colons * 0.34f;
        float unit = Math.min(availableHeight * 0.91f, availableWidth / widthUnits);
        float digitWidth = unit * 0.56f;
        float digitHeight = unit * 0.92f;
        float advance = unit * 0.72f;
        float totalWidth = digits * advance + colons * unit * 0.34f;
        int horizontalGravity = getGravity() & Gravity.HORIZONTAL_GRAVITY_MASK;
        float x = horizontalGravity == Gravity.LEFT ? leftBound
                : (horizontalGravity == Gravity.CENTER_HORIZONTAL
                        ? leftBound + (availableWidth - totalWidth) / 2f
                        : rightBound - totalWidth);
        float y = topBound + (availableHeight - digitHeight) / 2f;
        float thickness = Math.max(3f, digitWidth * 0.17f);
        float half = thickness / 2f;
        float mid = y + digitHeight / 2f;
        int color = segmentColor;
        int unlitColor = Color.argb(34, Color.red(color), Color.green(color), Color.blue(color));

        canvas.save();
        canvas.clipRect(leftBound, topBound, rightBound, bottomBound);
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == ':') {
                segmentPaint.setColor(color);
                float radius = Math.max(2.5f, thickness * 0.38f);
                float dotX = x + unit * 0.15f;
                canvas.drawCircle(dotX, y + digitHeight * 0.34f, radius, segmentPaint);
                canvas.drawCircle(dotX, y + digitHeight * 0.68f, radius, segmentPaint);
                x += unit * 0.34f;
                continue;
            }

            float l = x;
            float r = x + digitWidth;
            float t = y;
            float b = y + digitHeight;
            float m = mid;
            float[][] segments = new float[][] {
                    {l + half, t, r - half, t + thickness},                    // A top
                    {r - thickness, t + half, r, m - half},                   // B upper right
                    {r - thickness, m + half, r, b - half},                   // C lower right
                    {l + half, b - thickness, r - half, b},                   // D bottom
                    {l, m + half, l + thickness, b - half},                   // E lower left
                    {l, t + half, l + thickness, m - half},                   // F upper left
                    {l + half, m - half, r - half, m + half}                  // G middle
            };
            boolean[] lit = segments(ch);
            for (int j = 0; j < segments.length; j++) {
                float[] seg = segments[j];
                segmentPaint.setColor(lit[j] ? color : unlitColor);
                canvas.drawRoundRect(seg[0], seg[1], seg[2], seg[3],
                        thickness * 0.45f, thickness * 0.45f, segmentPaint);
            }
            x += advance;
        }
        canvas.restore();
    }

    private boolean[] segments(char digit) {
        switch (digit) {
            case '0': return new boolean[]{true, true, true, true, true, true, false};
            case '1': return new boolean[]{false, true, true, false, false, false, false};
            case '2': return new boolean[]{true, true, false, true, true, false, true};
            case '3': return new boolean[]{true, true, true, true, false, false, true};
            case '4': return new boolean[]{false, true, true, false, false, true, true};
            case '5': return new boolean[]{true, false, true, true, false, true, true};
            case '6': return new boolean[]{true, false, true, true, true, true, true};
            case '7': return new boolean[]{true, true, true, false, false, false, false};
            case '8': return new boolean[]{true, true, true, true, true, true, true};
            case '9': return new boolean[]{true, true, true, true, false, true, true};
            default: return new boolean[7];
        }
    }
}
