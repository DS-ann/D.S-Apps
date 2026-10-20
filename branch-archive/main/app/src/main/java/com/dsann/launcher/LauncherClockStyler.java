package com.dsann.launcher;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.widget.TextView;

/** Clock typography presets supported by Android 6 and newer. */
final class LauncherClockStyler {
    private LauncherClockStyler() {}

    static void apply(android.content.Context context, android.widget.TextView clockText, int style) {
        if (clockText == null) return;
        clockText.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT);
        switch (style) {
            case 1: // Cyber HUD: sharply condensed, italic display lettering.
                clockText.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD_ITALIC));
                clockText.setTextSize(40);
                clockText.setLetterSpacing(0.035f);
                clockText.setTextColor(Color.rgb(87, 229, 255));
                clockText.setShadowLayer(LauncherUiUtils.dp(this, 4), 0f, 0f, Color.argb(125, 0, 190, 255));
                break;
            case 2: // Neon Circuit: heavy geometric system sans, strong block-like digits.
                clockText.setTypeface(Typeface.create("sans-serif-black", Typeface.NORMAL));
                clockText.setTextSize(38);
                clockText.setLetterSpacing(0.015f);
                clockText.setTextColor(Color.rgb(112, 255, 190));
                clockText.setShadowLayer(LauncherUiUtils.dp(this, 5), 0f, 0f, Color.argb(105, 0, 255, 135));
                break;
            case 3: // Tech Minimal: thin, airy futuristic typography with no glow.
                clockText.setTypeface(Typeface.create("sans-serif-thin", Typeface.NORMAL));
                clockText.setTextSize(44);
                clockText.setLetterSpacing(0.065f);
                clockText.setTextColor(Color.rgb(225, 237, 255));
                break;
            case 4: // Terminal Digital: bold fixed-width terminal glyphs with phosphor-green readout.
                clockText.setTypeface(Typeface.create("monospace", Typeface.BOLD));
                clockText.setTextSize(39);
                clockText.setLetterSpacing(0.09f);
                clockText.setTextColor(Color.rgb(125, 255, 150));
                clockText.setShadowLayer(LauncherUiUtils.dp(this, 3), 0f, 0f, Color.argb(95, 60, 255, 100));
                break;
            case 5: // Square Pixel: custom chunky square-pixel numerals, not a system font.
                clockText.setTypeface(Typeface.create("monospace", Typeface.BOLD));
                clockText.setTextSize(40);
                clockText.setLetterSpacing(0f);
                clockText.setTextColor(Color.rgb(145, 255, 170));
                clockText.setShadowLayer(LauncherUiUtils.dp(this, 2), 0f, 0f, Color.argb(70, 60, 255, 100));
                break;
            default: // Digital: fixed-width digital readout with bold squared-off glyphs.
                clockText.setTypeface(Typeface.create("sans-serif-monospace", Typeface.BOLD));
                clockText.setTextSize(40);
                clockText.setLetterSpacing(0.055f);
                clockText.setTextColor(Color.rgb(118, 204, 255));
                clockText.setShadowLayer(LauncherUiUtils.dp(this, 5), 0f, 0f, Color.argb(135, 40, 132, 255));
                break;
        }
    }

}
