package com.dsann.batterymonitor;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Persistent per-app exemption from Android Doze restrictions.
 *
 * Android remains free to enter Doze globally; this adds BatteryPulse to the user-controlled
 * whitelist so the foreground service can hold a partial wake lock and continue sampling.
 */
final class DozeWhitelistController {
    private static final String PREFS = "settings";
    private static final String PACKAGE_NAME = "com.dsann.batterymonitor";
    private static final String LIST_MARKER = "__BATTERYPULSE_DEVICEIDLE_LIST__";

    static final String KEY_ENABLED = "root_doze_whitelist_enabled";
    static final String KEY_SETUP_ATTEMPTED = "root_doze_whitelist_setup_attempted";

    private DozeWhitelistController() {}

    static boolean isEnabled(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_ENABLED, false);
    }

    static Result enable(Context context) {
        return changeWhitelist(context, true);
    }

    static Result disable(Context context) {
        return changeWhitelist(context, false);
    }

    /** Reassert the saved allow-list setting after Android boots or the app is replaced. */
    static Result reapplyIfEnabled(Context context) {
        if (!isEnabled(context)) {
            return new Result(true, false, "Root Doze exemption is disabled.");
        }
        return changeWhitelist(context, true);
    }

    private static Result changeWhitelist(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String sign = enabled ? "+" : "-";
        String command = "dumpsys deviceidle whitelist " + sign + PACKAGE_NAME
                + " && echo " + LIST_MARKER
                + " && dumpsys deviceidle whitelist";
        RootShell.CommandResult result = RootShell.execute(command);
        boolean previousState = prefs.getBoolean(KEY_ENABLED, false);

        if (!result.success) {
            return new Result(false, previousState,
                    failureMessage(enabled, result.output, result.exitCode));
        }

        int markerAt = result.output.lastIndexOf(LIST_MARKER);
        if (markerAt < 0) {
            return new Result(false, previousState,
                    "Root command ran, but Android's Doze whitelist could not be verified.");
        }
        String whitelist = result.output.substring(markerAt + LIST_MARKER.length());
        boolean listed = whitelist.contains(PACKAGE_NAME);
        if (enabled && !listed) {
            return new Result(false, previousState,
                    "Android did not confirm BatteryPulse on the Doze whitelist. Check root access.");
        }
        if (!enabled && listed) {
            return new Result(false, previousState,
                    "Android still lists BatteryPulse on the Doze whitelist.");
        }

        prefs.edit()
                .putBoolean(KEY_ENABLED, enabled)
                .putBoolean(KEY_SETUP_ATTEMPTED, true)
                .apply();

        return new Result(true, enabled, enabled
                ? "Root Doze exemption enabled. Continuous sampling is allowed; battery use will increase."
                : "Root Doze exemption removed. Normal Doze sampling will resume.");
    }

    private static String failureMessage(boolean enabled, String output, int exitCode) {
        String detail = output == null ? "" : output.trim();
        if (detail.isEmpty()) detail = "exit code " + exitCode;
        return (enabled ? "Could not enable root Doze exemption: "
                : "Could not remove root Doze exemption: ") + detail;
    }

    static final class Result {
        final boolean success;
        final boolean enabled;
        final String message;

        Result(boolean success, boolean enabled, String message) {
            this.success = success;
            this.enabled = enabled;
            this.message = message == null ? "" : message;
        }
    }
}
