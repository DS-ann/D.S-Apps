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
        segmentPaint.setStrokeCap(Paint.Cap.ROUND);
        segmentPaint.setStrokeJoin(Paint.Join.ROUND);
        segmentPaint.setStyle(Paint.Style.STROKE);
    }

    public void setSevenSegmentMode(boolean enabled) {
        if (sevenSegmentMode == enabled) return;
        sevenSegmentMode = enabled;
        setLayerType(ViewLayerType(), null);
        invalidate();
        requestLayout();
    }

    // Kept separate so this class stays compatible with Android 6 (API 23).
    private int ViewLayerType() {
        return android.view.View.LAYER_TYPE_SOFTWARE;
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
        if (value.length() == 0) return;

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
        if (digits == 0) return;

        // The whole string is sized as one unit, so 12-hour and 24-hour formats fit.
        float widthUnits = digits * 0.72f + colons * 0.34f;
        float unit = Math.min(availableHeight * 0.91f, availableWidth / widthUnits);
        float digitWidth = unit * 0.56f;
        float digitHeight = unit * 0.92f;
        float advance = unit * 0.72f;
        float totalWidth = digits * advance + colons * unit * 0.34f;
        float x = rightBound - totalWidth;
        float y = topBound + (availableHeight - digitHeight) / 2f;
        float stroke = Math.max(2f, unit * 0.075f);
        float inset = stroke * 0.72f;
        float halfStroke = stroke / 2f;
        float mid = y + digitHeight / 2f;
        int color = getCurrentTextColor();

        canvas.save();
        canvas.clipRect(leftBound, topBound, rightBound, bottomBound);
        segmentPaint.setStrokeWidth(stroke);
        segmentPaint.setStyle(Paint.Style.STROKE);

        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == ':') {
                segmentPaint.setStyle(Paint.Style.FILL);
                segmentPaint.setColor(Color.argb(235, Color.red(color), Color.green(color), Color.blue(color)));
                float radius = Math.max(2f, stroke * 0.62f);
                float dotX = x + unit * 0.15f;
                canvas.drawCircle(dotX, y + digitHeight * 0.34f, radius, segmentPaint);
                canvas.drawCircle(dotX, y + digitHeight * 0.68f, radius, segmentPaint);
                segmentPaint.setStyle(Paint.Style.STROKE);
                x += unit * 0.34f;
                continue;
            }

            float l = x + halfStroke;
            float r = x + digitWidth - halfStroke;
            float t = y + halfStroke;
            float b = y + digitHeight - halfStroke;
            float m = mid;
            float[][] lines = new float[][] {
                    {l + inset, t, r - inset, t},             // A: top
                    {r, t + inset, r, m - inset},             // B: upper right
                    {r, m + inset, r, b - inset},             // C: lower right
                    {l + inset, b, r - inset, b},             // D: bottom
                    {l, m + inset, l, b - inset},             // E: lower left
                    {l, t + inset, l, m - inset},             // F: upper left
                    {l + inset, m, r - inset, m}              // G: middle
            };

            // Draw dim, unlit segments first, then the lit segments above them.
            segmentPaint.setColor(Color.argb(28, Color.red(color), Color.green(color), Color.blue(color)));
            for (float[] line : lines) canvas.drawLine(line[0], line[1], line[2], line[3], segmentPaint);
            boolean[] lit = segments(ch);
            segmentPaint.setColor(color);
            for (int j = 0; j < lines.length; j++) {
                if (lit[j]) {
                    float[] line = lines[j];
                    canvas.drawLine(line[0], line[1], line[2], line[3], segmentPaint);
                }
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
