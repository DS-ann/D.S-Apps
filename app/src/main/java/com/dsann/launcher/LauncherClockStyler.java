package com.dsann.launcher;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.widget.TextView;

/** Keeps clock typography presets separate from launcher layout code. */
final class LauncherClockStyler {
    private LauncherClockStyler() {}

    static void apply(Activity activity, TextView clock, int style) {
        if (clock == null) return;
        if (clock instanceof SevenSegmentClockTextView) {
            ((SevenSegmentClockTextView) clock).setSevenSegmentMode(style == 5);
        }
        switch (style) {
            case 1:
                clock.setTypeface(Typeface.create("monospace", Typeface.BOLD));
                clock.setLetterSpacing(0.12f);
                clock.setShadowLayer(12f, 0f, 0f, Color.rgb(40, 190, 255));
                break;
            case 2:
                clock.setTypeface(Typeface.create("cursive", Typeface.NORMAL));
                clock.setLetterSpacing(0.035f);
                clock.setShadowLayer(10f, 0f, 0f, Color.rgb(111, 90, 255));
                break;
            case 3:
                clock.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
                clock.setLetterSpacing(0.16f);
                clock.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT);
                break;
            case 4:
                clock.setTypeface(Typeface.create("serif", Typeface.BOLD));
                clock.setLetterSpacing(0.015f);
                clock.setShadowLayer(5f, 0f, 0f, Color.rgb(75, 255, 160));
                break;
            case 5:
                clock.setTypeface(Typeface.create("monospace", Typeface.BOLD));
                clock.setLetterSpacing(0f);
                clock.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT);
                break;
            default:
                clock.setTypeface(Typeface.create("sans-serif-condensed-light", Typeface.NORMAL));
                clock.setLetterSpacing(0.04f);
                clock.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT);
                break;
        }
    }
}
