package com.dsann.batterymonitor;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.BatteryManager;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Maintains a separate battery-level estimate by integrating measured current.
 * The Android percentage is used only to establish or recover a baseline when
 * a genuine fuel-gauge charge counter is unavailable.
 */
final class BatteryPercentCalculator {
    private static final String PREFS = "battery_percent_calculator_state";
    private static final String KEY_INITIALIZED = "initialized";
    private static final String KEY_CAPACITY_MAH = "capacity_mah";
    private static final String KEY_REMAINING_MAH = "remaining_mah";
    private static final String KEY_LAST_SAMPLE_MS = "last_sample_ms";
    private static final String KEY_LAST_CURRENT = "last_current_ma";
    private static final String KEY_LAST_CHARGING = "last_charging";
    private static final String KEY_CAPACITY_SOURCE = "capacity_source";
    private static final String KEY_STATUS = "status";
    private static final String KEY_LAST_PERSIST_MS = "last_persist_ms";

    private static final long MIN_SAMPLE_INTERVAL_MS = 750L;
    private static final long MAX_INTEGRATION_GAP_MS = 2L * 60L * 1000L;
    private static final long CAPACITY_RETRY_MS = 60L * 1000L;
    private static final long PERSIST_INTERVAL_MS = 15L * 1000L;

    private static boolean loaded;
    private static boolean initialized;
    private static boolean previousCharging;
    private static long lastSampleMs;
    private static long lastPersistMs;
    private static long lastCapacityProbeMs;
    private static double capacityMah = Double.NaN;
    private static double remainingMah = Double.NaN;
    private static double previousCurrentMa = Double.NaN;
    private static String capacitySource = "Unavailable";
    private static String status = "Waiting for the first current sample.";

    private BatteryPercentCalculator() {}

    static synchronized Estimate sample(Context context, BatteryReader.Reading reading) {
        load(context);
        long now = System.currentTimeMillis();
        BatteryState batteryState = readBatteryState(context);

        if (!initialized && (lastCapacityProbeMs == 0L
                || now - lastCapacityProbeMs >= CAPACITY_RETRY_MS || now < lastCapacityProbeMs)) {
            lastCapacityProbeMs = now;
            Bootstrap bootstrap = readBootstrap(context, capacityMah, false);
            if (isPositive(bootstrap.capacityMah)) {
                capacityMah = bootstrap.capacityMah;
                capacitySource = bootstrap.capacitySource;
            }
            if (isPositive(capacityMah) && isAmount(bootstrap.remainingMah)) {
                remainingMah = clamp(bootstrap.remainingMah, capacityMah);
                initialized = true;
                lastSampleMs = now;
                previousCurrentMa = currentMa(reading);
                previousCharging = batteryState.plugged;
                status = "Baseline: " + bootstrap.seedSource;
                persist(context, now, true);
            } else {
                status = bootstrap.message;
                persist(context, now, false);
            }
            return snapshot();
        }

        if (!initialized || !isPositive(capacityMah) || !isAmount(remainingMah)) {
            if (!isPositive(capacityMah)) status = "A genuine full-charge/design capacity was not found; waiting for device data or learned sessions.";
            return snapshot();
        }

        long sinceLast = now - lastSampleMs;
        if (lastSampleMs > 0L && sinceLast >= 0L && sinceLast < MIN_SAMPLE_INTERVAL_MS) {
            return snapshot();
        }

        if (reading == null || reading.currentUa == Long.MIN_VALUE) {
            lastSampleMs = now;
            previousCurrentMa = Double.NaN;
            previousCharging = batteryState.plugged;
            status = "Current unavailable; holding the last mAh estimate.";
            persist(context, now, false);
            return snapshot();
        }

        double currentMa = currentMa(reading);
        boolean resynced = false;
        boolean directionChanged = batteryState.plugged != previousCharging;

        if (lastSampleMs > 0L && sinceLast > 0L && sinceLast <= MAX_INTEGRATION_GAP_MS) {
            if (isAmount(previousCurrentMa)) {
                double segmentMah = ((previousCurrentMa + currentMa) / 2.0)
                        * (sinceLast / 3600000.0);
                remainingMah += previousCharging ? segmentMah : -segmentMah;
                remainingMah = clamp(remainingMah, capacityMah);
                status = "mAh integrated every 1 second • "
                        + (previousCharging ? "charging" : "discharging");
            } else {
                status = "Current restored; waiting for the next interval to integrate mAh.";
            }
        } else if (lastSampleMs > 0L) {
            // Do not invent charge changes across long or backwards-clock gaps.
            Bootstrap bootstrap = readBootstrap(context, capacityMah, true);
            if (isAmount(bootstrap.remainingMah)) {
                remainingMah = clamp(bootstrap.remainingMah, capacityMah);
                status = "Re-synced after a sampling gap • " + bootstrap.seedSource;
            } else {
                status = "Sampling gap detected; holding the last mAh estimate.";
            }
            resynced = true;
        }

        lastSampleMs = now;
        previousCurrentMa = currentMa;
        previousCharging = batteryState.plugged;
        if (directionChanged && !resynced) {
            status = "mAh integrated every 1 second • "
                    + (batteryState.plugged ? "charging" : "discharging");
        }
        persist(context, now, directionChanged || resynced);
        return snapshot();
    }

    static synchronized Estimate current(Context context) {
        load(context);
        return snapshot();
    }

    static synchronized Estimate recalibrate(Context context, BatteryReader.Reading reading) {
        load(context);
        long now = System.currentTimeMillis();
        Bootstrap bootstrap = readBootstrap(context, capacityMah, false);
        if (!isPositive(bootstrap.capacityMah) || !isAmount(bootstrap.remainingMah)) {
            status = "Recalibration unavailable: " + bootstrap.message;
            persist(context, now, true);
            return snapshot();
        }

        capacityMah = bootstrap.capacityMah;
        remainingMah = clamp(bootstrap.remainingMah, capacityMah);
        capacitySource = bootstrap.capacitySource;
        initialized = true;
        lastSampleMs = now;
        previousCurrentMa = currentMa(reading);
        previousCharging = readBatteryState(context).plugged;
        status = "Recalibrated • " + bootstrap.seedSource;
        persist(context, now, true);
        return snapshot();
    }

    private static void load(Context context) {
        if (loaded) return;
        SharedPreferences p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        initialized = p.getBoolean(KEY_INITIALIZED, false);
        capacityMah = readDouble(p, KEY_CAPACITY_MAH, Double.NaN);
        remainingMah = readDouble(p, KEY_REMAINING_MAH, Double.NaN);
        lastSampleMs = p.getLong(KEY_LAST_SAMPLE_MS, 0L);
        previousCurrentMa = readDouble(p, KEY_LAST_CURRENT, Double.NaN);
        previousCharging = p.getBoolean(KEY_LAST_CHARGING, false);
        capacitySource = p.getString(KEY_CAPACITY_SOURCE, "Unavailable");
        status = p.getString(KEY_STATUS, "Waiting for the first current sample.");
        lastPersistMs = p.getLong(KEY_LAST_PERSIST_MS, 0L);
        if (!isPositive(capacityMah) || !isAmount(remainingMah)) initialized = false;
        loaded = true;
    }

    private static Bootstrap readBootstrap(Context context, double preferredCapacity, boolean preferExistingCapacity) {
        BatteryHealthReader.Data data = BatteryHealthReader.read(context);
        Capacity detected = resolveCapacity(context, data);
        double cap = preferExistingCapacity && isPositive(preferredCapacity)
                ? preferredCapacity : detected.capacityMah;
        String capSource = preferExistingCapacity && isPositive(preferredCapacity)
                ? capacitySource : detected.source;
        if (!isPositive(cap)) {
            cap = preferredCapacity;
            capSource = capacitySource;
        }
        if (!isPositive(cap)) {
            return new Bootstrap(Double.NaN, Double.NaN, "Unavailable", "Unavailable",
                    "No real battery capacity source is available yet.");
        }

        double remaining = Double.NaN;
        String seed = "Unavailable";
        if (data.remainingChargeUaH > 0L) {
            double counterMah = data.remainingChargeUaH / 1000.0;
            if (counterMah <= cap * 1.15) {
                remaining = clamp(counterMah, cap);
                seed = "fuel-gauge charge counter";
            }
        }
        if (!isAmount(remaining)) {
            int percent = batteryPercent(context);
            if (percent >= 0) {
                remaining = cap * percent / 100.0;
                seed = "Android percentage baseline";
            }
        }
        if (!isAmount(remaining)) {
            return new Bootstrap(cap, Double.NaN, capSource, seed,
                    "Capacity found, but neither charge counter nor battery level is available.");
        }
        return new Bootstrap(cap, remaining, capSource, seed, "Ready");
    }

    private static Capacity resolveCapacity(Context context, BatteryHealthReader.Data data) {
        if (data.fullChargeUaH > 0L) {
            String source = data.rawHealth != null && data.rawHealth.toLowerCase(Locale.US).contains("qmax")
                    ? "MTK fuel-gauge Qmax"
                    : "Device-reported full-charge capacity";
            return new Capacity(data.fullChargeUaH / 1000.0, source);
        }

        String designSource = data.designSource == null ? "" : data.designSource;
        if (data.designChargeUaH > 0L
                && !designSource.toLowerCase(Locale.US).contains("rated battery capacity")) {
            return new Capacity(data.designChargeUaH / 1000.0,
                    designSource.isEmpty() ? "Device-reported design capacity" : designSource);
        }

        double learned = estimateCapacityFromSessions(context);
        if (isPositive(learned)) {
            return new Capacity(learned, "Learned from charging-session mAh");
        }
        return new Capacity(Double.NaN, "Unavailable");
    }

    private static double estimateCapacityFromSessions(Context context) {
        ArrayList<SessionStore.Record> records = SessionStore.getRecords(context);
        double weightedCapacity = 0.0;
        double totalWeight = 0.0;
        for (SessionStore.Record record : records) {
            if (!record.charging || record.startPercent < 0 || record.endPercent < 0 || record.mah <= 0.0) {
                continue;
            }
            int delta = record.endPercent - record.startPercent;
            if (delta < 40) continue;
            double capacity = record.mah * 100.0 / delta;
            if (capacity < 1500.0 || capacity > 6000.0) continue;
            double weight = Math.min(delta, 100);
            weightedCapacity += capacity * weight;
            totalWeight += weight;
        }
        return totalWeight > 0.0 ? weightedCapacity / totalWeight : Double.NaN;
    }

    private static BatteryState readBatteryState(Context context) {
        Intent intent = context.registerReceiver(null,
                new android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (intent == null) return new BatteryState(-1, false);
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        int percent = level >= 0 && scale > 0 ? Math.round(level * 100.0f / scale) : -1;
        boolean plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0;
        return new BatteryState(percent, plugged);
    }

    private static int batteryPercent(Context context) {
        return readBatteryState(context).percent;
    }

    private static double currentMa(BatteryReader.Reading reading) {
        if (reading == null || reading.currentUa == Long.MIN_VALUE) return Double.NaN;
        return Math.abs((double) reading.currentUa) / 1000.0;
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
                .putBoolean(KEY_INITIALIZED, initialized)
                .putLong(KEY_CAPACITY_MAH, Double.doubleToLongBits(capacityMah))
                .putLong(KEY_REMAINING_MAH, Double.doubleToLongBits(remainingMah))
                .putLong(KEY_LAST_SAMPLE_MS, lastSampleMs)
                .putLong(KEY_LAST_CURRENT, Double.doubleToLongBits(previousCurrentMa))
                .putBoolean(KEY_LAST_CHARGING, previousCharging)
                .putString(KEY_CAPACITY_SOURCE, capacitySource)
                .putString(KEY_STATUS, status)
                .putLong(KEY_LAST_PERSIST_MS, now)
                .apply();
        lastPersistMs = now;
    }

    private static Estimate snapshot() {
        if (!initialized || !isPositive(capacityMah) || !isAmount(remainingMah)) {
            return new Estimate(-1, Double.NaN, capacityMah, capacitySource, status);
        }
        int percent = (int) Math.round(clamp(remainingMah, capacityMah) * 100.0 / capacityMah);
        percent = Math.max(0, Math.min(100, percent));
        return new Estimate(percent, clamp(remainingMah, capacityMah), capacityMah, capacitySource, status);
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

        Estimate(int percent, double remainingMah, double capacityMah, String capacitySource, String status) {
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

    private static final class Capacity {
        final double capacityMah;
        final String source;
        Capacity(double capacityMah, String source) {
            this.capacityMah = capacityMah;
            this.source = source;
        }
    }

    private static final class Bootstrap {
        final double capacityMah;
        final double remainingMah;
        final String capacitySource;
        final String seedSource;
        final String message;

        Bootstrap(double capacityMah, double remainingMah, String capacitySource, String seedSource, String message) {
            this.capacityMah = capacityMah;
            this.remainingMah = remainingMah;
            this.capacitySource = capacitySource;
            this.seedSource = seedSource;
            this.message = message;
        }
    }
}
