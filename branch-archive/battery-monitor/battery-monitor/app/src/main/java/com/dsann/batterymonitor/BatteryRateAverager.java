package com.dsann.batterymonitor;

/**
 * Low-memory, time-weighted battery charge-rate estimate published in 20-second windows.
 *
 * Device convention: negative current enters the battery (charging), while positive current
 * leaves the battery (discharging). The rate is the signed change in battery percentage per
 * hour, independent of Android's plugged/charging status.
 */
final class BatteryRateAverager {
    private static final long WINDOW_MS = 20_000L;
    private static final long MIN_WINDOW_COVERAGE_MS = WINDOW_MS * 3L / 4L;
    private static final long MAX_SAMPLE_GAP_MS = 3_000L;
    private static final double MIN_CAPACITY_MAH = 100.0;
    private static final double MAX_CAPACITY_MAH = 100_000.0;
    // BatteryHealthReader uses this same rated capacity when the kernel exposes no design value.
    private static final double DEFAULT_CAPACITY_MAH = 4050.0;

    private static double activeCapacityMah = Double.NaN;
    private static long windowStartMs;
    private static long lastSampleAtMs;
    private static double lastCurrentMa = Double.NaN;
    private static double integratedCurrentMaMs;
    private static long integratedDurationMs;
    private static double publishedRatePercentPerHour = Double.NaN;

    private BatteryRateAverager() {}

    /**
     * Feed one signed current sample and return the latest time-weighted rate estimate.
     * Positive rates mean net charge entering the battery; negative rates mean net discharge.
     */
    static synchronized double ratePerHour(BatteryReader.Reading reading, double capacityMah) {
        long now = android.os.SystemClock.elapsedRealtime();

        // Rate display must work before the optional My Battery mAh estimator is configured.
        // Prefer its custom capacity when valid; otherwise use the Lenovo rated design capacity.
        if (!isValidCapacity(capacityMah)) capacityMah = DEFAULT_CAPACITY_MAH;

        if (capacityChanged(capacityMah) || windowStartMs == 0L) {
            reset(capacityMah, now);
        }

        if (reading == null || reading.currentUa == Long.MIN_VALUE) {
            if (lastSampleAtMs > 0L && now >= lastSampleAtMs
                    && now - lastSampleAtMs > MAX_SAMPLE_GAP_MS) {
                reset(capacityMah, now);
            }
            return publishedRatePercentPerHour;
        }

        double currentMa = (double) reading.currentUa / 1000.0;
        if (Double.isNaN(currentMa) || Double.isInfinite(currentMa)) {
            return publishedRatePercentPerHour;
        }

        if (lastSampleAtMs == 0L) {
            lastCurrentMa = currentMa;
            lastSampleAtMs = now;
            return publishedRatePercentPerHour;
        }

        if (now <= lastSampleAtMs) {
            reset(capacityMah, now);
            lastCurrentMa = currentMa;
            lastSampleAtMs = now;
            return publishedRatePercentPerHour;
        }

        long gapMs = now - lastSampleAtMs;
        if (gapMs > MAX_SAMPLE_GAP_MS) {
            reset(capacityMah, now);
            lastCurrentMa = currentMa;
            lastSampleAtMs = now;
            return Double.NaN;
        }

        long windowEndMs = windowStartMs + WINDOW_MS;
        if (now >= windowEndMs) {
            long segmentMs = windowEndMs - lastSampleAtMs;
            if (segmentMs > 0L && segmentMs <= gapMs) {
                integrate(lastCurrentMa, currentMa, segmentMs);
            }

            // Publish only after a complete 20-second window, and only when at least 75%
            // was actually observed. Never label a short or interrupted sample as a 20s average.
            publishCompletedWindow();

            windowStartMs = now;
            integratedCurrentMaMs = 0.0;
            integratedDurationMs = 0L;
            lastSampleAtMs = now;
            lastCurrentMa = currentMa;
            return publishedRatePercentPerHour;
        }

        integrate(lastCurrentMa, currentMa, gapMs);
        lastSampleAtMs = now;
        lastCurrentMa = currentMa;
        return publishedRatePercentPerHour;
    }

    private static void publishCompletedWindow() {
        if (integratedDurationMs < MIN_WINDOW_COVERAGE_MS
                || !isValidCapacity(activeCapacityMah)) {
            publishedRatePercentPerHour = Double.NaN;
            return;
        }
        double averageCurrentMa = integratedCurrentMaMs / integratedDurationMs;
        if (Double.isNaN(averageCurrentMa) || Double.isInfinite(averageCurrentMa)) {
            publishedRatePercentPerHour = Double.NaN;
            return;
        }
        // Negative current means charging, so invert current sign for battery % change.
        publishedRatePercentPerHour =
                -averageCurrentMa * 100.0 / activeCapacityMah;
    }

    private static void integrate(double previousMa, double currentMa, long durationMs) {
        if (durationMs <= 0L) return;
        integratedCurrentMaMs += ((previousMa + currentMa) / 2.0) * durationMs;
        integratedDurationMs += durationMs;
    }

    private static boolean capacityChanged(double capacityMah) {
        return Double.isNaN(activeCapacityMah)
                || Math.abs(activeCapacityMah - capacityMah) > 0.5;
    }

    private static boolean isValidCapacity(double capacityMah) {
        return !Double.isNaN(capacityMah) && !Double.isInfinite(capacityMah)
                && capacityMah >= MIN_CAPACITY_MAH && capacityMah <= MAX_CAPACITY_MAH;
    }

    private static void reset(double capacityMah, long now) {
        activeCapacityMah = capacityMah;
        windowStartMs = now;
        lastSampleAtMs = 0L;
        lastCurrentMa = Double.NaN;
        integratedCurrentMaMs = 0.0;
        integratedDurationMs = 0L;
        publishedRatePercentPerHour = Double.NaN;
    }
}
