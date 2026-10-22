package com.dsann.batterymonitor;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Locale;

/** Independent estimator based only on user-entered mAh and measured current. */
final class BatteryPercentCalculator {
    private static final String PREFS = "battery_percent_calculator_state";
    private static final String KEY_CUSTOM_CAPACITY_MAH = "custom_capacity_mah";
    private static final String KEY_STARTING_CHARGE_MAH = "starting_charge_mah";
    private static final String KEY_ESTIMATOR_VERSION = "estimator_version";
    private static final String KEY_INITIALIZED = "initialized";
    private static final String KEY_RUNNING = "running";
    private static final String KEY_REMAINING_MAH = "remaining_mah";
    private static final String KEY_LAST_SAMPLE_MS = "last_sample_ms";
    private static final String KEY_LAST_CURRENT = "last_current_ma";
    private static final String KEY_STATUS = "status";
    private static final String KEY_LAST_PERSIST_MS = "last_persist_ms";

    private static final double MIN_CUSTOM_CAPACITY_MAH = 100.0;
    private static final double MAX_CUSTOM_CAPACITY_MAH = 100000.0;
    private static final long MIN_SAMPLE_INTERVAL_MS = 50L;
    private static final long MAX_INTEGRATION_GAP_MS = 2L * 60L * 1000L;
    private static final long PERSIST_INTERVAL_MS = 15L * 1000L;

    private static boolean loaded;
    private static boolean initialized;
    private static boolean running;
    private static long lastSampleMs;
    private static long lastPersistMs;
    private static double capacityMah = Double.NaN;
    private static double startingChargeMah = Double.NaN;
    private static double remainingMah = Double.NaN;
    private static double previousCurrentMa = Double.NaN;
    private static String status = "";

    private BatteryPercentCalculator() {}

    static synchronized double getCustomCapacityMah(Context context) {
        load(context);
        return isAmount(capacityMah) ? capacityMah : Double.NaN;
    }

    static synchronized double getStartingChargeMah(Context context) {
        load(context);
        return isAmount(startingChargeMah) ? startingChargeMah : Double.NaN;
    }

    static synchronized Estimate startEstimate(Context context, double requestedCapacityMah,
                                               double requestedStartingMah, BatteryReader.Reading reading) {
        load(context);
        if (!isValidCapacity(requestedCapacityMah)
                || !isAmount(requestedStartingMah)
                || requestedStartingMah > requestedCapacityMah) {
            status = "";
            return snapshot();
        }
        capacityMah = requestedCapacityMah;
        startingChargeMah = requestedStartingMah;
        remainingMah = clamp(startingChargeMah, capacityMah);
        initialized = true;
        running = true;
        lastSampleMs = System.currentTimeMillis();
        previousCurrentMa = currentMa(reading);
        status = "";
        persist(context, lastSampleMs, true);
        return snapshot();
    }

    static synchronized Estimate stopMeasurement(Context context) {
        load(context);
        running = false;
        // Preserve the last estimate on screen, but do not integrate across the stop.
        lastSampleMs = 0L;
        previousCurrentMa = Double.NaN;
        status = "";
        persist(context, System.currentTimeMillis(), true);
        return snapshot();
    }

    static synchronized Estimate clearAll(Context context) {
        load(context);
        capacityMah = 0.0;
        startingChargeMah = 0.0;
        remainingMah = 0.0;
        initialized = false;
        running = false;
        lastSampleMs = 0L;
        previousCurrentMa = Double.NaN;
        status = "";
        persist(context, System.currentTimeMillis(), true);
        return snapshot();
    }

    static synchronized Estimate sample(Context context, BatteryReader.Reading reading) {
        load(context);
        if (!running || !initialized || !isValidCapacity(capacityMah)
                || !isAmount(remainingMah)) return snapshot();

        long now = System.currentTimeMillis();
        long elapsedMs = now - lastSampleMs;
        if (lastSampleMs > 0L && elapsedMs >= 0L && elapsedMs < MIN_SAMPLE_INTERVAL_MS) {
            return snapshot();
        }

        double currentMa = currentMa(reading);
        if (!isFinite(currentMa)) {
            lastSampleMs = now;
            previousCurrentMa = Double.NaN;
            status = "";
            persist(context, now, false);
            return snapshot();
        }

        if (lastSampleMs > 0L && elapsedMs > 0L && elapsedMs <= MAX_INTEGRATION_GAP_MS
                && isFinite(previousCurrentMa)) {
            double segmentMah = ((previousCurrentMa + currentMa) / 2.0)
                    * (elapsedMs / 3600000.0);
            // Negative current adds charge; positive current removes charge.
            remainingMah -= segmentMah;
            remainingMah = clamp(remainingMah, capacityMah);
        }

        // If a long gap occurred, hold the last estimate and restart from this sample.
        lastSampleMs = now;
        previousCurrentMa = currentMa;
        persist(context, now, elapsedMs > MAX_INTEGRATION_GAP_MS);
        return snapshot();
    }

    static synchronized Estimate current(Context context) {
        load(context);
        return snapshot();
    }

    static synchronized boolean isRunning(Context context) {
        load(context);
        return running && initialized;
    }

    private static void load(Context context) {
        if (loaded) return;
        SharedPreferences p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        int version = p.getInt(KEY_ESTIMATOR_VERSION, 0);
        capacityMah = readDouble(p, KEY_CUSTOM_CAPACITY_MAH, Double.NaN);
        startingChargeMah = version >= 2
                ? readDouble(p, KEY_STARTING_CHARGE_MAH, Double.NaN) : Double.NaN;
        initialized = version >= 2 && p.getBoolean(KEY_INITIALIZED, false);
        // Older saved estimates are not automatically resumed: require an explicit Save & start.
        running = version >= 3 && p.getBoolean(KEY_RUNNING, false);
        remainingMah = readDouble(p, KEY_REMAINING_MAH, Double.NaN);
        // Version 3 stored absolute current, so its last sample cannot safely seed signed
        // integration. Preserve the remaining mAh but wait for a fresh signed current sample.
        lastSampleMs = version >= 4 ? p.getLong(KEY_LAST_SAMPLE_MS, 0L) : 0L;
        previousCurrentMa = version >= 4
                ? readDouble(p, KEY_LAST_CURRENT, Double.NaN) : Double.NaN;
        lastPersistMs = p.getLong(KEY_LAST_PERSIST_MS, 0L);
        status = "";
        if (!isValidCapacity(capacityMah) || !isAmount(startingChargeMah)
                || startingChargeMah > capacityMah || !isAmount(remainingMah)) {
            initialized = false;
            running = false;
        }
        loaded = true;
    }

    private static double currentMa(BatteryReader.Reading reading) {
        if (reading == null || reading.currentUa == Long.MIN_VALUE) return Double.NaN;
        return (double) reading.currentUa / 1000.0;
    }

    private static boolean isFinite(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
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
                .putInt(KEY_ESTIMATOR_VERSION, 4)
                .putLong(KEY_CUSTOM_CAPACITY_MAH, Double.doubleToLongBits(capacityMah))
                .putLong(KEY_STARTING_CHARGE_MAH, Double.doubleToLongBits(startingChargeMah))
                .putBoolean(KEY_INITIALIZED, initialized)
                .putBoolean(KEY_RUNNING, running)
                .putLong(KEY_REMAINING_MAH, Double.doubleToLongBits(remainingMah))
                .putLong(KEY_LAST_SAMPLE_MS, lastSampleMs)
                .putLong(KEY_LAST_CURRENT, Double.doubleToLongBits(previousCurrentMa))
                .putString(KEY_STATUS, status)
                .putLong(KEY_LAST_PERSIST_MS, now)
                .apply();
        lastPersistMs = now;
    }

    private static Estimate snapshot() {
        if (!isValidCapacity(capacityMah) || !initialized || !isAmount(remainingMah)) {
            double remaining = isAmount(remainingMah) ? remainingMah : Double.NaN;
            double capacity = isAmount(capacityMah) ? capacityMah : Double.NaN;
            return new Estimate(-1, remaining, capacity, running, "");
        }
        int percent = (int) Math.round(clamp(remainingMah, capacityMah) * 100.0 / capacityMah);
        percent = Math.max(0, Math.min(100, percent));
        return new Estimate(percent, clamp(remainingMah, capacityMah), capacityMah, running, "");
    }

    private static double readDouble(SharedPreferences p, String key, double fallback) {
        return Double.longBitsToDouble(p.getLong(key, Double.doubleToLongBits(fallback)));
    }

    static final class Estimate {
        final int percent;
        final double percentExact;
        final double remainingMah;
        final double capacityMah;
        final boolean running;
        final String status;

        Estimate(int percent, double remainingMah, double capacityMah, boolean running, String status) {
            this.percent = percent;
            this.percentExact = isPositive(capacityMah) && isAmount(remainingMah)
                    ? Math.max(0.0, Math.min(100.0, remainingMah * 100.0 / capacityMah))
                    : Double.NaN;
            this.remainingMah = remainingMah;
            this.capacityMah = capacityMah;
            this.running = running;
            this.status = status == null ? "" : status;
        }

        boolean isAvailable() {
            return percent >= 0 && isPositive(capacityMah) && isAmount(remainingMah);
        }
    }
}
