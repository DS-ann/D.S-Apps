package com.dsann.batterymonitor;

/**
 * Low-memory, one-minute time-weighted average for battery charge/discharge rate.
 * One shared in-process accumulator is used by the activity and foreground service.
 */
final class BatteryRateAverager {
    private static final long WINDOW_MS = 60_000L;
    private static final long MAX_SAMPLE_GAP_MS = 3_000L;
    private static final double MIN_CAPACITY_MAH = 100.0;
    private static final double MAX_CAPACITY_MAH = 100_000.0;

    private enum Direction { UNKNOWN, CHARGING, DISCHARGING, FULL }

    private static Direction activeDirection = Direction.UNKNOWN;
    private static double activeCapacityMah = Double.NaN;
    private static long windowStartMs;
    private static long lastSampleAtMs;
    private static double lastCurrentMa = Double.NaN;
    private static double integratedCurrentMaMs;
    private static long integratedDurationMs;
    private static double publishedRatePercentPerHour = Double.NaN;

    private BatteryRateAverager() {}

    /**
     * Feed a current sample and return the last completed one-minute rate.
     * The first numeric charge/discharge rate is available after one full minute of
     * valid, sufficiently continuous samples. Thereafter it changes once per minute.
     */
    static synchronized double ratePerHour(BatteryReader.Reading reading,
                                            BatteryReader.PowerState state,
                                            double capacityMah) {
        long now = android.os.SystemClock.elapsedRealtime();
        Direction direction = directionOf(state);

        if (!isValidCapacity(capacityMah) || direction == Direction.UNKNOWN) {
            reset(Direction.UNKNOWN, Double.NaN, 0L);
            return Double.NaN;
        }

        if (direction == Direction.FULL) {
            if (activeDirection != Direction.FULL || capacityChanged(capacityMah)) {
                reset(Direction.FULL, capacityMah, now);
            }
            publishedRatePercentPerHour = 0.0;
            return 0.0;
        }

        if (activeDirection != direction || capacityChanged(capacityMah)
                || windowStartMs == 0L) {
            reset(direction, capacityMah, now);
        }

        if (reading == null || reading.currentUa == Long.MIN_VALUE) {
            if (lastSampleAtMs > 0L && now >= lastSampleAtMs
                    && now - lastSampleAtMs > MAX_SAMPLE_GAP_MS) {
                reset(direction, capacityMah, now);
            }
            return publishedRatePercentPerHour;
        }

        double currentMa = Math.abs((double) reading.currentUa) / 1000.0;
        if (Double.isNaN(currentMa) || Double.isInfinite(currentMa)) {
            return publishedRatePercentPerHour;
        }

        if (lastSampleAtMs == 0L) {
            lastCurrentMa = currentMa;
            lastSampleAtMs = now;
            return publishedRatePercentPerHour;
        }

        if (now <= lastSampleAtMs) {
            reset(direction, capacityMah, now);
            lastCurrentMa = currentMa;
            lastSampleAtMs = now;
            return publishedRatePercentPerHour;
        }

        long gapMs = now - lastSampleAtMs;
        if (gapMs > MAX_SAMPLE_GAP_MS) {
            reset(direction, capacityMah, now);
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

            // Only publish after at least 45 seconds of valid integration. This tolerates
            // occasional scheduling jitter without reporting a partial or sparse minute.
            if (integratedDurationMs >= WINDOW_MS * 3L / 4L) {
                double averageCurrentMa = integratedCurrentMaMs / integratedDurationMs;
                publishedRatePercentPerHour = averageCurrentMa * 100.0 / activeCapacityMah;
            } else {
                publishedRatePercentPerHour = Double.NaN;
            }

            // Start the next minute at this sample. Keep the most recent current as its
            // first sample, but don't carry any integral across the minute boundary.
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

    private static void integrate(double previousMa, double currentMa, long durationMs) {
        if (durationMs <= 0L) return;
        integratedCurrentMaMs += ((previousMa + currentMa) / 2.0) * durationMs;
        integratedDurationMs += durationMs;
    }

    private static Direction directionOf(BatteryReader.PowerState state) {
        if (state == BatteryReader.PowerState.CHARGING) return Direction.CHARGING;
        if (state == BatteryReader.PowerState.DISCHARGING
                || state == BatteryReader.PowerState.NOT_CHARGING) return Direction.DISCHARGING;
        if (state == BatteryReader.PowerState.FULL) return Direction.FULL;
        return Direction.UNKNOWN;
    }

    private static boolean capacityChanged(double capacityMah) {
        return Double.isNaN(activeCapacityMah)
                || Math.abs(activeCapacityMah - capacityMah) > 0.5;
    }

    private static boolean isValidCapacity(double capacityMah) {
        return !Double.isNaN(capacityMah) && !Double.isInfinite(capacityMah)
                && capacityMah >= MIN_CAPACITY_MAH && capacityMah <= MAX_CAPACITY_MAH;
    }

    private static void reset(Direction direction, double capacityMah, long now) {
        activeDirection = direction;
        activeCapacityMah = capacityMah;
        windowStartMs = now;
        lastSampleAtMs = 0L;
        lastCurrentMa = Double.NaN;
        integratedCurrentMaMs = 0.0;
        integratedDurationMs = 0L;
        publishedRatePercentPerHour = Double.NaN;
    }
}
