package com.dsann.launcher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Color;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.TextView;

/** TextView-backed seven-segment digital clock face; native text is used for other presets. */
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
    }

    public void setSevenSegmentMode(boolean enabled) {
        sevenSegmentMode = enabled;
        invalidate();
        requestLayout();
    }

    @Override protected void onDraw(Canvas canvas) {
        if (!sevenSegmentMode) {
            super.onDraw(canvas);
            return;
        }

        String raw = getText() == null ? "" : getText().toString();
        StringBuilder valueBuilder = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            char ch = raw.charAt(i);
            int digit = Character.digit(ch, 10);
            if (digit >= 0) valueBuilder.append((char) ('0' + digit));
            else if (ch == ':') valueBuilder.append(':');
        }
        String value = valueBuilder.toString();
        if (value.isEmpty()) return;

        float availableWidth = Math.max(1f, getWidth() - getPaddingLeft() - getPaddingRight());
        float availableHeight = Math.max(1f, getHeight() - getPaddingTop() - getPaddingBottom());
        int digitCount = 0;
        for (int i = 0; i < value.length(); i++) if (value.charAt(i) >= '0' && value.charAt(i) <= '9') digitCount++;
        int colonCount = value.length() - digitCount;
        float unit = Math.min(availableHeight * 0.88f,
                availableWidth / Math.max(1f, digitCount * 0.62f + colonCount * 0.30f + 0.08f));
        float digitWidth = unit * 0.52f;
        float digitHeight = unit * 0.88f;
        float horizontalLength = digitWidth * 0.72f;
        float verticalLength = digitHeight * 0.34f;
        float stroke = Math.max(2f, unit * 0.085f);
        float advance = unit * 0.62f;
        float totalWidth = digitCount * advance + colonCount * unit * 0.30f;
        float x = getPaddingLeft() + Math.max(0f, availableWidth - totalWidth);
        float y = getPaddingTop() + (availableHeight - digitHeight) / 2f;

        segmentPaint.setStyle(Paint.Style.STROKE);
        segmentPaint.setStrokeCap(Paint.Cap.ROUND);
        segmentPaint.setStrokeJoin(Paint.Join.ROUND);
        segmentPaint.setStrokeWidth(stroke);
        int color = getCurrentTextColor();
        float half = stroke / 2f;

        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == ':') {
                segmentPaint.setStyle(Paint.Style.FILL);
                segmentPaint.setColor(Color.argb(235, Color.red(color), Color.green(color), Color.blue(color)));
                float dotRadius = stroke * 0.65f;
                canvas.drawCircle(x + unit * 0.10f, y + digitHeight * 0.36f, dotRadius, segmentPaint);
                canvas.drawCircle(x + unit * 0.10f, y + digitHeight * 0.68f, dotRadius, segmentPaint);
                segmentPaint.setStyle(Paint.Style.STROKE);
                x += unit * 0.30f;
                continue;
            }
            if (ch < '0' || ch > '9') continue;

            // Segment order: top, upper-right, lower-right, bottom, lower-left, upper-left, middle.
            boolean[] active = segments(ch);
            float left = x + half;
            float right = x + digitWidth - half;
            float top = y + half;
            float bottom = y + digitHeight - half;
            float mid = y + digitHeight / 2f;
            float inset = stroke * 0.65f;

            float[][] lines = new float[][] {
                    {left + inset, top, right - inset, top},
                    {right, top + inset, right, mid - inset},
                    {right, mid + inset, right, bottom - inset},
                    {left + inset, bottom, right - inset, bottom},
                    {left, mid + inset, left, bottom - inset},
                    {left, top + inset, left, mid - inset},
                    {left + inset, mid, right - inset, mid}
            };

            // A faint unlit segment gives the display a real LED-panel appearance.
            segmentPaint.setColor(Color.argb(25, Color.red(color), Color.green(color), Color.blue(color)));
            for (float[] line : lines) canvas.drawLine(line[0], line[1], line[2], line[3], segmentPaint);
            segmentPaint.setColor(color);
            segmentPaint.setShadowLayer(unit * 0.035f, 0f, 0f,
                    Color.argb(100, Color.red(color), Color.green(color), Color.blue(color)));
            for (int segment = 0; segment < lines.length; segment++) {
                if (active[segment]) {
                    float[] line = lines[segment];
                    canvas.drawLine(line[0], line[1], line[2], line[3], segmentPaint);
                }
            }
            segmentPaint.clearShadowLayer();
            x += advance;
        }
        segmentPaint.setStyle(Paint.Style.FILL);
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
