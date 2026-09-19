package com.veytrix.autopilot;

import android.graphics.Color;
import android.util.TypedValue;
import android.view.View;

/**
 * Phase 2 visual system contract.
 *
 * These tokens are intentionally independent from the existing legacy screen
 * implementation. The final UI will consume this system after each surface is
 * migrated and verified.
 */
public final class VeytrixDesignTokens {
    private VeytrixDesignTokens() {
    }

    // Surface family: pearl / white / silver / champagne.
    public static final int PEARL = Color.rgb(252, 250, 255);
    public static final int WHITE = Color.WHITE;
    public static final int SILVER = Color.rgb(232, 230, 238);
    public static final int SILVER_STRONG = Color.rgb(190, 186, 202);
    public static final int CHAMPAGNE = Color.rgb(244, 235, 214);

    // Accent family: violet / purple / magenta / pink / rose / crimson.
    public static final int VIOLET = Color.rgb(113, 70, 219);
    public static final int PURPLE = Color.rgb(139, 75, 214);
    public static final int MAGENTA = Color.rgb(201, 61, 164);
    public static final int PINK = Color.rgb(236, 101, 177);
    public static final int ROSE = Color.rgb(212, 72, 112);
    public static final int CRIMSON = Color.rgb(176, 38, 74);

    // Semantic text colors with sufficient contrast on pearl/white surfaces.
    public static final int TEXT_PRIMARY = Color.rgb(52, 43, 67);
    public static final int TEXT_SECONDARY = Color.rgb(91, 79, 107);
    public static final int TEXT_TERTIARY = Color.rgb(111, 99, 125);

    // Engineering state accents.
    public static final int STATE_IDLE = SILVER_STRONG;
    public static final int STATE_PLANNING = VIOLET;
    public static final int STATE_IMPLEMENTING = MAGENTA;
    public static final int STATE_BUILDING = PURPLE;
    public static final int STATE_TESTING = PINK;
    public static final int STATE_FAILED = CRIMSON;
    public static final int STATE_REPAIRING = ROSE;
    public static final int STATE_RETESTING = MAGENTA;
    public static final int STATE_VERIFYING = VIOLET;
    public static final int STATE_VERIFIED = Color.rgb(121, 81, 174);
    public static final int STATE_DELIVERED = Color.rgb(151, 104, 171);

    // 8dp rhythm; interactive controls should expose >=48dp hit areas.
    public static final int GRID_DP = 8;
    public static final int TOUCH_TARGET_DP = 48;
    public static final int BODY_SP = 15;
    public static final int LABEL_SP = 12;
    public static final int TITLE_SP = 22;
    public static final int SECTION_SP = 17;

    public static int dp(View view, int value) {
        return Math.round(value * view.getResources().getDisplayMetrics().density);
    }

    public static float sp(View view, int value) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                value,
                view.getResources().getDisplayMetrics()
        );
    }
}
