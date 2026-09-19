package com.veytrix.autopilot;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Non-secret operator preferences used by the native control surface.
 * Credentials remain isolated in VeytrixSecureStore.
 */
public final class VeytrixControlPreferences {
    private static final String PREFS = "veytrix_control_v1";
    private static final String ENGINE = "engine";
    private static final String VERIFICATION_DEPTH = "verification_depth";
    private static final String NOTIFICATIONS = "notifications";

    private final SharedPreferences prefs;

    public VeytrixControlPreferences(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public String getEngine() {
        return prefs.getString(ENGINE, "auto");
    }

    public void setEngine(String engine) {
        VeytrixInputValidator.validateEngine(engine);
        prefs.edit().putString(ENGINE, engine).apply();
    }

    public int getVerificationDepth() {
        return prefs.getInt(VERIFICATION_DEPTH, 2);
    }

    public void setVerificationDepth(int depth) {
        VeytrixInputValidator.validateVerificationDepth(depth);
        prefs.edit().putInt(VERIFICATION_DEPTH, depth).apply();
    }

    public boolean isNotificationsEnabled() {
        return prefs.getBoolean(NOTIFICATIONS, true);
    }

    public void setNotificationsEnabled(boolean enabled) {
        prefs.edit().putBoolean(NOTIFICATIONS, enabled).apply();
    }
}
