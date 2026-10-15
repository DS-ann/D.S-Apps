package com.dsann.launcher;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.TextView;

/** Custom square-pixel clock face used by the launcher's Square Pixel style. */
public final class SquarePixelClockTextView extends TextView {
    private final Paint pixelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final String[] digitRows = new String[]{
            "01110/11011/11011/11011/11011/11011/01110",
            "00100/01100/00100/00100/00100/00100/01110",
            "01110/11011/00011/00110/01100/11000/11111",
            "11110/00011/00011/01110/00011/00011/11110",
            "00011/00111/01011/10011/11111/00011/00011",
            "11111/11000/11000/11110/00011/00011/11110",
            "01110/11000/11000/11110/11011/11011/01110",
            "11111/00011/00110/00100/01100/01100/01100",
            "01110/11011/11011/01110/11011/11011/01110",
            "01110/11011/11011/01111/00011/00011/01110"
    };
    private SharedPreferences preferences;
    private String styleKey = "clock_style";

    public SquarePixelClockTextView(Context context) {
        super(context);
        initialize();
    }

    public SquarePixelClockTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public SquarePixelClockTextView(Context context, SharedPreferences preferences, String styleKey) {
        super(context);
        this.preferences = preferences;
        if (styleKey != null) this.styleKey = styleKey;
        initialize();
    }

    public void setClockStylePreferences(SharedPreferences preferences, String styleKey) {
        this.preferences = preferences;
        if (styleKey != null) this.styleKey = styleKey;
        invalidate();
    }

    private void initialize() {
        setGravity(Gravity.CENTER_VERTICAL);
        setSingleLine(true);
    }

    @Override protected void onDraw(Canvas canvas) {
        if (preferences == null || preferences.getInt(styleKey, 0) != 5) {
            super.onDraw(canvas);
            return;
        }
        String rawValue = getText() == null ? "" : getText().toString();
        StringBuilder visible = new StringBuilder();
        for (int i = 0; i < rawValue.length(); i++) {
            char ch = rawValue.charAt(i);
            if ((ch >= '0' && ch <= '9') || ch == ':') visible.append(ch);
        }
        String value = visible.toString();
        if (value.length() == 0) return;
        float cell = getTextSize() / 9.0f;
        float block = cell * 0.82f;
        float advance = cell * 6.0f;
        float total = value.length() * advance - cell;
        float left = Math.max(getPaddingLeft(), (getWidth() - total) / 2.0f);
        float top = (getHeight() - cell * 7.0f) / 2.0f;
        pixelPaint.setColor(getCurrentTextColor());
        pixelPaint.setStyle(Paint.Style.FILL);
        pixelPaint.setShadowLayer(dp(1), 0f, 0f, Color.argb(70, 60, 255, 100));
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == ':') {
                drawPixel(canvas, left + i * advance + cell * 2.0f, top + cell * 2.0f, block);
                drawPixel(canvas, left + i * advance + cell * 2.0f, top + cell * 5.0f, block);
                continue;
            }
            if (ch < '0' || ch > '9') continue;
            String[] rows = digitRows[ch - '0'].split("/");
            for (int row = 0; row < 7; row++) {
                for (int col = 0; col < 5; col++) {
                    if (rows[row].charAt(col) == '1') {
                        drawPixel(canvas, left + i * advance + col * cell, top + row * cell, block);
                    }
                }
            }
        }
        pixelPaint.clearShadowLayer();
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private void drawPixel(Canvas canvas, float x, float y, float size) {
        canvas.drawRect(x, y, x + size, y + size, pixelPaint);
    }
}
