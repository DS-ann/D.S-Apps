package com.dsann.batterymonitor;

import android.content.Context;
import android.content.SharedPreferences;

/** Android-reported charging override and rooted MTK fuel-gauge daemon controls. */
final class BatteryChargingOverride {
    private static final String PREFS = "settings";

    static final String KEY_ENABLED = "fake_android_charging_enabled";
    static final String KEY_MANUAL_PERCENT_ENABLED = "manual_android_percent_override_enabled";
    static final String KEY_MANUAL_PERCENT = "manual_android_percent_override_value";

    private static final String KEY_USE_ANDROID_PERCENT = "my_battery_use_android_percent";
    private static final String KEY_LAST_ANDROID_PERCENT = "my_battery_last_android_percent";
    private static final String KEY_LAST_ANDROID_POWER_STATUS = "my_battery_last_android_power_status";
    private static final String KEY_LAST_ANDROID_POWER_SOURCE = "my_battery_last_android_power_source";
    private static final String FG_DAEMON_DISABLE =
            "/sys/devices/platform/battery_meter/FG_daemon_disable";

    private BatteryChargingOverride() {}

    static boolean isEnabled(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_ENABLED, false);
    }

    static BatteryReader.PowerInfo powerInfoForAndroid(Context context,
                                                        BatteryReader.PowerInfo physical) {
        if (!isEnabled(context)) return physical;
        return new BatteryReader.PowerInfo(BatteryReader.PowerState.CHARGING,
                true, false, false);
    }

    static Result setEnabled(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        boolean current = prefs.getBoolean(KEY_ENABLED, false);
        if (current == enabled) {
            return new Result(true, current, current
                    ? "Fake Android charging is already enabled."
                    : "Fake Android charging is already disabled.");
        }

        if (enabled) {
            RootShell.CommandResult applied = RootShell.execute(fakeChargingCommand());
            if (!applied.success) {
                return new Result(false, false, rootFailure("Could not enable fake charging", applied));
            }
            prefs.edit().putBoolean(KEY_ENABLED, true).apply();
            return new Result(true, true,
                    "Fake Android charging enabled. Physical charging is unchanged.");
        }

        RootShell.CommandResult reset = RootShell.execute("dumpsys battery reset");
        if (!reset.success) {
            return new Result(false, true, rootFailure("Could not restore Android battery reporting", reset));
        }
        prefs.edit().putBoolean(KEY_ENABLED, false).apply();

        boolean usingAppPercent = prefs.getBoolean(KEY_USE_ANDROID_PERCENT, false);
        int restorePercent = usingAppPercent
                ? prefs.getInt(KEY_LAST_ANDROID_PERCENT, -1) : -1;
        if (restorePercent < 0 && !usingAppPercent
                && prefs.getBoolean(KEY_MANUAL_PERCENT_ENABLED, false)) {
            restorePercent = prefs.getInt(KEY_MANUAL_PERCENT, -1);
        }

        if (restorePercent >= 0 && restorePercent <= 100) {
            BatteryReader.PowerInfo physical = BatteryReader.getPowerInfo(context);
            BatteryPercentOverride.Result restored =
                    BatteryPercentOverride.set(restorePercent, physical);
            if (!restored.success) {
                return new Result(false, false,
                        "Fake charging is off, but the saved Android battery level could not be restored: "
                                + restored.message);
            }
            if (usingAppPercent) {
                prefs.edit()
                        .putInt(KEY_LAST_ANDROID_PERCENT, restorePercent)
                        .putInt(KEY_LAST_ANDROID_POWER_STATUS, physical.androidStatus)
                        .putInt(KEY_LAST_ANDROID_POWER_SOURCE, physical.sourceMask())
                        .apply();
            }
        }

        return new Result(true, false, "Fake Android charging disabled; Android reporting restored.");
    }

    static Result reapplyIfEnabled(Context context) {
        if (!isEnabled(context)) {
            return new Result(true, false, "Fake Android charging is disabled.");
        }
        RootShell.CommandResult applied = RootShell.execute(fakeChargingCommand());
        if (!applied.success) {
            return new Result(false, true, rootFailure("Fake charging is saved as enabled, but reapplying it failed", applied));
        }
        return new Result(true, true,
                "Fake Android charging re-applied. Physical charging is unchanged.");
    }

    /** Reset a battery-level override while retaining the independent fake-charge switch. */
    static BatteryPercentOverride.Result resetAndroidBattery(Context context) {
        BatteryPercentOverride.Result reset = BatteryPercentOverride.reset();
        if (!reset.success || !isEnabled(context)) return reset;

        RootShell.CommandResult applied = RootShell.execute(fakeChargingCommand());
        if (!applied.success) {
            return new BatteryPercentOverride.Result(false,
                    "Battery reset succeeded, but fake charging could not be reapplied: "
                            + rootFailure("root command failed", applied));
        }
        return new BatteryPercentOverride.Result(true,
                "Android battery-level override reset; fake charging remains enabled.");
    }

    static Result setFuelGaugeDaemonDisabled(boolean disabled) {
        String value = disabled ? "1" : "0";
        RootShell.CommandResult result =
                RootShell.execute("echo " + value + " > " + FG_DAEMON_DISABLE);
        if (!result.success) {
            return new Result(false, false,
                    rootFailure(disabled ? "Could not disable the fuel-gauge daemon"
                            : "Could not enable the fuel-gauge daemon", result));
        }
        return new Result(true, false, disabled
                ? "Fuel-gauge daemon disabled (FG_daemon_disable=1). Enable it again after testing."
                : "Fuel-gauge daemon enabled (FG_daemon_disable=0).");
    }

    private static String fakeChargingCommand() {
        // Android's BatteryService reports an AC source and CHARGING status; kernel readings
        // used by BatteryPulse remain physical and are not modified by this simulation.
        return "dumpsys battery set ac 1"
                + " && dumpsys battery set usb 0"
                + " && dumpsys battery set wireless 0"
                + " && dumpsys battery set status 2";
    }

    private static String rootFailure(String action, RootShell.CommandResult result) {
        String detail = result.output == null ? "" : result.output.trim();
        if (detail.isEmpty()) detail = "exit code " + result.exitCode;
        return action + ": " + detail;
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
