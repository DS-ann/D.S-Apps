package com.dsann.batterymonitor;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.BatteryManager;

import java.util.Locale;

/**
 * An independent battery-level estimator driven by the user's custom mAh value
 * and integrated current. It deliberately does not read system full-capacity
 * or remaining-charge-counter properties.
 */
final class BatteryPercentCalculator {
    private static final String PREFS = "battery_percent_calculator_state";
    private static final String KEY_CUSTOM_CAPACITY_MAH = "custom_capacity_mah";
    private static final String KEY_INITIALIZED = "initialized";
    private static final String KEY_REMAINING_MAH = "remaining_mah";
    private static final String KEY_LAST_SAMPLE_MS = "last_sample_ms";
    private static final String KEY_LAST_CURRENT = "last_current_ma";
    private static final String KEY_LAST_CHARGING = "last_charging";
    private static final String KEY_STATUS = "status";
    private static final String KEY_LAST_PERSIST_MS = "last_persist_ms";

    private static final double MIN_CUSTOM_CAPACITY_MAH = 100.0;
    private static final double MAX_CUSTOM_CAPACITY_MAH = 100000.0;
    private static final long MIN_SAMPLE_INTERVAL_MS = 750L;
    private static final long MAX_INTEGRATION_GAP_MS = 2L * 60L * 1000L;
    private static final long PERSIST_INTERVAL_MS = 15L * 1000L;

    private static boolean loaded;
    private static boolean initialized;
    private static boolean previousCharging;
    private static long lastSampleMs;
    private static long lastPersistMs;
    private static double capacityMah = Double.NaN;
    private static double remainingMah = Double.NaN;
    private static double previousCurrentMa = Double.NaN;
    private static String status = "Enter your custom battery capacity in mAh to start.";

    private BatteryPercentCalculator() {}

    static synchronized double getCustomCapacityMah(Context context) {
        load(context);
        return isPositive(capacityMah) ? capacityMah : Double.NaN;
    }

    static synchronized Estimate setCustomCapacity(Context context, double requestedMah,
                                                    BatteryReader.Reading reading) {
        load(context);
        if (!isValidCapacity(requestedMah)) {
            status = "Enter a capacity between 100 and 100000 mAh.";
            return snapshot();
        }

        BatteryState state = readBatteryState(context);
        capacityMah = requestedMah;
        if (state.percent >= 0) {
            startFromPercentage(context, state, reading, System.currentTimeMillis(),
                    "Custom capacity saved; baseline set from current Android level (" + state.percent + "%).");
        } else {
            initialized = false;
            remainingMah = Double.NaN;
            previousCurrentMa = Double.NaN;
            lastSampleMs = 0L;
            status = "Custom capacity saved. Waiting for the current battery percentage to establish the starting mAh.";
            persist(context, System.currentTimeMillis(), true);
        }
        return snapshot();
    }

    static synchronized Estimate sample(Context context, BatteryReader.Reading reading) {
        load(context);
        long now = System.currentTimeMillis();

        if (!isValidCapacity(capacityMah)) {
            initialized = false;
            status = "Enter your custom battery capacity in mAh to start.";
            return snapshot();
        }

        BatteryState batteryState = readBatteryState(context);
        if (!initialized || !isAmount(remainingMah)) {
            if (batteryState.percent < 0) {
                status = "Custom capacity is saved; waiting for Android's battery percentage to set a starting level.";
                persist(context, now, false);
                return snapshot();
            }
            startFromPercentage(context, batteryState, reading, now,
                    "Started from current Android level (" + batteryState.percent + "%); subsequent updates use current integration.");
            persist(context, now, true);
            return snapshot();
        }

        long elapsedMs = now - lastSampleMs;
        if (lastSampleMs > 0L && elapsedMs >= 0L && elapsedMs < MIN_SAMPLE_INTERVAL_MS) {
            return snapshot();
        }

        double currentMa = currentMa(reading);
        if (!isAmount(currentMa)) {
            lastSampleMs = now;
            previousCurrentMa = Double.NaN;
            previousCharging = batteryState.plugged;
            status = "Current unavailable; holding the calculated mAh estimate.";
            persist(context, now, false);
            return snapshot();
        }

        boolean directionChanged = batteryState.plugged != previousCharging;
        if (lastSampleMs > 0L && elapsedMs > 0L && elapsedMs <= MAX_INTEGRATION_GAP_MS) {
            if (isAmount(previousCurrentMa)) {
                double segmentMah = ((previousCurrentMa + currentMa) / 2.0)
                        * (elapsedMs / 3600000.0);
                remainingMah += previousCharging ? segmentMah : -segmentMah;
                remainingMah = clamp(remainingMah, capacityMah);
                status = "Integrated current over " + String.format(Locale.US, "%.2f", elapsedMs / 1000.0)
                        + " s • " + (previousCharging ? "charging" : "discharging");
            } else {
                status = "Current restored; waiting for the next interval to integrate mAh.";
            }
        } else if (lastSampleMs > 0L) {
            // Never invent charge changes across long/background service gaps.
            // Do not re-seed from the system percentage automatically.
            status = "Sampling gap exceeded 2 minutes; estimate held and integration restarted.";
        }

        lastSampleMs = now;
        previousCurrentMa = currentMa;
        previousCharging = batteryState.plugged;
        if (directionChanged && elapsedMs > 0L && elapsedMs <= MAX_INTEGRATION_GAP_MS) {
            status = "Current integrated using custom capacity • "
                    + (batteryState.plugged ? "charging" : "discharging");
        }
        persist(context, now, directionChanged || elapsedMs > MAX_INTEGRATION_GAP_MS);
        return snapshot();
    }

    static synchronized Estimate current(Context context) {
        load(context);
        return snapshot();
    }

    /**
     * Explicit user-requested re-baseline. This uses the current Android level
     * only when the user taps Reset estimate; no system capacity/counter is read.
     */
    static synchronized Estimate recalibrate(Context context, BatteryReader.Reading reading) {
        load(context);
        if (!isValidCapacity(capacityMah)) {
            status = "Enter and save a custom capacity before resetting the estimate.";
            return snapshot();
        }

        BatteryState state = readBatteryState(context);
        if (state.percent < 0) {
            status = "Cannot reset the estimate because the current battery percentage is unavailable.";
            persist(context, System.currentTimeMillis(), true);
            return snapshot();
        }

        startFromPercentage(context, state, reading, System.currentTimeMillis(),
                "Estimate reset from current Android level (" + state.percent + "%).");
        return snapshot();
    }

    private static void startFromPercentage(Context context, BatteryState state,
                                            BatteryReader.Reading reading, long now, String message) {
        remainingMah = clamp(capacityMah * state.percent / 100.0, capacityMah);
        initialized = true;
        lastSampleMs = now;
        previousCurrentMa = currentMa(reading);
        previousCharging = state.plugged;
        status = message;
        persist(context, now, true);
    }

    private static void load(Context context) {
        if (loaded) return;
        SharedPreferences p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        capacityMah = readDouble(p, KEY_CUSTOM_CAPACITY_MAH, Double.NaN);
        initialized = p.getBoolean(KEY_INITIALIZED, false);
        remainingMah = readDouble(p, KEY_REMAINING_MAH, Double.NaN);
        lastSampleMs = p.getLong(KEY_LAST_SAMPLE_MS, 0L);
        previousCurrentMa = readDouble(p, KEY_LAST_CURRENT, Double.NaN);
        previousCharging = p.getBoolean(KEY_LAST_CHARGING, false);
        status = p.getString(KEY_STATUS, "Enter your custom battery capacity in mAh to start.");
        lastPersistMs = p.getLong(KEY_LAST_PERSIST_MS, 0L);
        if (!isValidCapacity(capacityMah) || !isAmount(remainingMah)) initialized = false;
        loaded = true;
    }

    private static BatteryState readBatteryState(Context context) {
        Intent intent = context.registerReceiver(null,
                new android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (intent == null) return new BatteryState(-1, false);
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        int percent = level >= 0 && scale > 0 ? Math.max(0, Math.min(100,
                Math.round(level * 100.0f / scale))) : -1;
        boolean plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0;
        return new BatteryState(percent, plugged);
    }

    private static double currentMa(BatteryReader.Reading reading) {
        if (reading == null || reading.currentUa == Long.MIN_VALUE) return Double.NaN;
        return Math.abs((double) reading.currentUa) / 1000.0;
    }

    private static boolean isValidCapacity(double value) {
        return isAmount(value) && value >= MIN_CUSTOM_CAPACITY_MAH
                && value <= MAX_CUSTOM_CAPACITY_MAH;
    }

    private static double clamp(double value, double capacity) {
        return Math.max(0.0, Math.min(capacity, value));
    }

    private static boolean isPositive(double value) {
        return isAmount(value) && value > 0.0;
    }

    private static boolean isAmount(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value) && value >= 0.0;
    }

    private static void persist(Context context, long now, boolean force) {
        if (!force && lastPersistMs > 0L && now >= lastPersistMs
                && now - lastPersistMs < PERSIST_INTERVAL_MS) return;
        SharedPreferences p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        p.edit()
                .putLong(KEY_CUSTOM_CAPACITY_MAH, Double.doubleToLongBits(capacityMah))
                .putBoolean(KEY_INITIALIZED, initialized)
                .putLong(KEY_REMAINING_MAH, Double.doubleToLongBits(remainingMah))
                .putLong(KEY_LAST_SAMPLE_MS, lastSampleMs)
                .putLong(KEY_LAST_CURRENT, Double.doubleToLongBits(previousCurrentMa))
                .putBoolean(KEY_LAST_CHARGING, previousCharging)
                .putString(KEY_STATUS, status)
                .putLong(KEY_LAST_PERSIST_MS, now)
                .apply();
        lastPersistMs = now;
    }

    private static Estimate snapshot() {
        if (!isValidCapacity(capacityMah) || !initialized || !isAmount(remainingMah)) {
            return new Estimate(-1, Double.NaN, capacityMah, "Custom capacity not set", status);
        }
        int percent = (int) Math.round(clamp(remainingMah, capacityMah) * 100.0 / capacityMah);
        percent = Math.max(0, Math.min(100, percent));
        return new Estimate(percent, clamp(remainingMah, capacityMah), capacityMah,
                "User-defined capacity", status);
    }

    private static double readDouble(SharedPreferences p, String key, double fallback) {
        return Double.longBitsToDouble(p.getLong(key, Double.doubleToLongBits(fallback)));
    }

    static final class Estimate {
        final int percent;
        final double remainingMah;
        final double capacityMah;
        final String capacitySource;
        final String status;

        Estimate(int percent, double remainingMah, double capacityMah,
                 String capacitySource, String status) {
            this.percent = percent;
            this.remainingMah = remainingMah;
            this.capacityMah = capacityMah;
            this.capacitySource = capacitySource == null ? "Unavailable" : capacitySource;
            this.status = status == null ? "Unavailable" : status;
        }

        boolean isAvailable() {
            return percent >= 0 && isPositive(capacityMah) && isAmount(remainingMah);
        }
    }

    private static final class BatteryState {
        final int percent;
        final boolean plugged;

        BatteryState(int percent, boolean plugged) {
            this.percent = percent;
            this.plugged = plugged;
        }
    }
}
