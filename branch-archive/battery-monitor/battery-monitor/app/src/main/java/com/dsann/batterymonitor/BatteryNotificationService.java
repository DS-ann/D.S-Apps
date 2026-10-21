package com.dsann.batterymonitor;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;

public class BatteryNotificationService extends Service {
    private static final int NOTIFICATION_ID = 1001;
    private static final String CHANNEL_ID = "battery_monitor";
    private static final String PREFS = "settings";
    private static final String CALC_PREFS = "battery_percent_calculator_state";
    private static final String KEY_INTERVAL_MS = "sampling_interval_ms";
    private static final String KEY_MY_BATTERY_INTERVAL_MS = "my_battery_sampling_interval_ms";
    private static final String KEY_USE_ANDROID_PERCENT = "my_battery_use_android_percent";
    private static final String KEY_RESET_PENDING = "my_battery_android_percent_reset_pending";
    private static final String KEY_LAST_ANDROID_PERCENT = "my_battery_last_android_percent";
    private static final String KEY_CALCULATOR_RUNNING = "running";
    private static final String KEY_NOTIFICATION_ENABLED = "notification_enabled";
    private static final long MIN_INTERVAL_MS = 100L;
    private static final long MAX_INTERVAL_MS = 2000L;
    private static final long DEFAULT_INTERVAL_MS = 1000L;
    private static final long MIN_MY_BATTERY_INTERVAL_MS = 50L;
    private static final long MAX_MY_BATTERY_INTERVAL_MS = 1500L;
    private static final long DEFAULT_MY_BATTERY_INTERVAL_MS = 500L;
    private static final long NOTIFICATION_MIN_REFRESH_MS = 500L;
    private static final long ROOT_RETRY_GAP_MS = 5000L;
    private static final long RECOVERY_DELAY_MS = 15000L;

    private final Handler handler = new Handler();
    private long lastHistorySampleMs;
    private long lastCalculatorSampleMs;
    private long lastNotificationUpdateMs;
    private long lastRootCommandMs;
    private PowerManager.WakeLock wakeLock;

    private final Runnable updater = new Runnable() {
        @Override public void run() {
            try {
                updateNotification();
            } catch (Throwable ignored) {
                // Preserve the persistent loop after transient sensor/root/preferences errors.
            } finally {
                if (shouldRunService()) handler.postDelayed(this, getNextSamplingInterval());
                else stopSelf();
            }
        }
    };

    @Override public void onCreate() {
        super.onCreate();
        createChannel();
        startForeground(NOTIFICATION_ID,
                buildNotification("--", 0.0, 0L, BatteryPercentCalculator.current(this)));
        acquireCpuLock();
        updateNotification();
        if (shouldRunService()) handler.postDelayed(updater, getNextSamplingInterval());
        else stopSelf();
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (shouldRunService()) {
            if (wakeLock == null || !wakeLock.isHeld()) acquireCpuLock();
            handler.removeCallbacks(updater);
            handler.post(updater);
            return START_STICKY;
        }
        stopSelf();
        return START_NOT_STICKY;
    }

    private void acquireCpuLock() {
        try {
            PowerManager manager = (PowerManager) getSystemService(POWER_SERVICE);
            if (manager != null) {
                if (wakeLock == null) {
                    wakeLock = manager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                            "BatteryPulse:continuous-measurement");
                    wakeLock.setReferenceCounted(false);
                }
                if (!wakeLock.isHeld()) wakeLock.acquire();
            }
        } catch (Throwable ignored) { }
    }

    private boolean shouldRunService() {
        android.content.SharedPreferences settings = getSharedPreferences(PREFS, MODE_PRIVATE);
        boolean recording = settings.getBoolean(KEY_NOTIFICATION_ENABLED, false);
        boolean usePercent = settings.getBoolean(KEY_USE_ANDROID_PERCENT, false);
        boolean resetPending = settings.getBoolean(KEY_RESET_PENDING, false);
        boolean estimatorRunning = getSharedPreferences(CALC_PREFS, MODE_PRIVATE)
                .getBoolean(KEY_CALCULATOR_RUNNING, false);
        return recording || usePercent || resetPending || estimatorRunning;
    }

    private long getSamplingInterval() {
        long value = getSharedPreferences(PREFS, MODE_PRIVATE)
                .getLong(KEY_INTERVAL_MS, DEFAULT_INTERVAL_MS);
        return value < MIN_INTERVAL_MS || value > MAX_INTERVAL_MS ? DEFAULT_INTERVAL_MS : value;
    }

    private long getMyBatterySamplingInterval() {
        long value = getSharedPreferences(PREFS, MODE_PRIVATE)
                .getLong(KEY_MY_BATTERY_INTERVAL_MS, DEFAULT_MY_BATTERY_INTERVAL_MS);
        return value < MIN_MY_BATTERY_INTERVAL_MS || value > MAX_MY_BATTERY_INTERVAL_MS
                ? DEFAULT_MY_BATTERY_INTERVAL_MS : value;
    }

    private long getNextSamplingInterval() {
        android.content.SharedPreferences settings = getSharedPreferences(PREFS, MODE_PRIVATE);
        boolean recording = settings.getBoolean(KEY_NOTIFICATION_ENABLED, false);
        boolean appPercent = settings.getBoolean(KEY_USE_ANDROID_PERCENT, false);
        boolean estimating = getSharedPreferences(CALC_PREFS, MODE_PRIVATE)
                .getBoolean(KEY_CALCULATOR_RUNNING, false);
        if (recording && (appPercent || estimating))
            return Math.min(getSamplingInterval(), getMyBatterySamplingInterval());
        return recording ? getSamplingInterval() : getMyBatterySamplingInterval();
    }

    private void updateNotification() {
        android.content.SharedPreferences settings = getSharedPreferences(PREFS, MODE_PRIVATE);
        long now = System.currentTimeMillis();

        if (settings.getBoolean(KEY_RESET_PENDING, false)) {
            retryPendingAndroidPercentageReset(now);
            settings = getSharedPreferences(PREFS, MODE_PRIVATE);
        }
        boolean recordingEnabled = settings.getBoolean(KEY_NOTIFICATION_ENABLED, false);
        boolean usePercent = settings.getBoolean(KEY_USE_ANDROID_PERCENT, false);
        boolean estimating = getSharedPreferences(CALC_PREFS, MODE_PRIVATE)
                .getBoolean(KEY_CALCULATOR_RUNNING, false);
        if (!recordingEnabled && !usePercent
                && !settings.getBoolean(KEY_RESET_PENDING, false) && !estimating) return;

        BatteryReader.Reading reading = BatteryReader.read(this);
        BatteryPercentCalculator.Estimate estimate = BatteryPercentCalculator.current(this);
        long myBatteryInterval = getMyBatterySamplingInterval();
        if (lastCalculatorSampleMs == 0L || now < lastCalculatorSampleMs
                || now - lastCalculatorSampleMs >= myBatteryInterval) {
            estimate = BatteryPercentCalculator.sample(this, reading);
            lastCalculatorSampleMs = now;
        }

        settings = getSharedPreferences(PREFS, MODE_PRIVATE);
        usePercent = settings.getBoolean(KEY_USE_ANDROID_PERCENT, false);
        if (usePercent) {
            if (estimate != null && estimate.running && estimate.isAvailable()) {
                applyCalculatedAndroidPercentage(estimate, now);
            } else {
                settings.edit().putBoolean(KEY_USE_ANDROID_PERCENT, false)
                        .putBoolean(KEY_RESET_PENDING, true).apply();
                retryPendingAndroidPercentageReset(now);
            }
        }

        settings = getSharedPreferences(PREFS, MODE_PRIVATE);
        recordingEnabled = settings.getBoolean(KEY_NOTIFICATION_ENABLED, false);
        long sessionInterval = getSamplingInterval();
        if (recordingEnabled && (lastHistorySampleMs == 0L || now < lastHistorySampleMs
                || now - lastHistorySampleMs >= sessionInterval)) {
            SessionStore.sample(this, reading);
            lastHistorySampleMs = now;
        }

        long notifyEvery = Math.max(NOTIFICATION_MIN_REFRESH_MS,
                Math.min(1000L, recordingEnabled ? sessionInterval : myBatteryInterval));
        if (now - lastNotificationUpdateMs >= notifyEvery || lastNotificationUpdateMs == 0L) {
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.notify(NOTIFICATION_ID,
                        buildNotification(reading.compact(), SessionStore.getActiveMah(this),
                                SessionStore.getActiveScreenOnMs(this), BatteryPercentCalculator.current(this)));
            }
            lastNotificationUpdateMs = now;
        }
    }

    private void applyCalculatedAndroidPercentage(BatteryPercentCalculator.Estimate estimate, long now) {
        android.content.SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        int requested = estimate.percent;
        int lastApplied = prefs.getInt(KEY_LAST_ANDROID_PERCENT, -1);
        int currentAndroidLevel = getAndroidReportedPercent();
        if (lastApplied == requested && currentAndroidLevel == requested) return;
        if (lastRootCommandMs > 0L && now >= lastRootCommandMs
                && now - lastRootCommandMs < ROOT_RETRY_GAP_MS) return;

        lastRootCommandMs = now;
        BatteryPercentOverride.Result result = BatteryPercentOverride.set(requested);
        if (result.success) {
            prefs.edit().putInt(KEY_LAST_ANDROID_PERCENT, requested)
                    .putBoolean(KEY_RESET_PENDING, false).apply();
        }
    }

    private void retryPendingAndroidPercentageReset(long now) {
        android.content.SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        if (!prefs.getBoolean(KEY_RESET_PENDING, false)) return;
        if (lastRootCommandMs > 0L && now >= lastRootCommandMs
                && now - lastRootCommandMs < ROOT_RETRY_GAP_MS) return;
        lastRootCommandMs = now;
        BatteryPercentOverride.Result result = BatteryPercentOverride.reset();
        if (result.success) {
            prefs.edit().putBoolean(KEY_RESET_PENDING, false)
                    .remove(KEY_LAST_ANDROID_PERCENT).apply();
        }
    }

    private int getAndroidReportedPercent() {
        Intent battery = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (battery == null) return -1;
        int level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        return level < 0 || scale <= 0 ? -1
                : Math.max(0, Math.min(100, Math.round(level * 100f / scale)));
    }

    private Notification buildNotification(String liveText, double usedMah, long screenOnMs,
                                           BatteryPercentCalculator.Estimate estimate) {
        Intent open = new Intent(this, MainActivity.class);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, open, flags);
        boolean available = estimate != null && estimate.running && estimate.isAvailable();
        String title = available
                ? String.format(java.util.Locale.US, "Calculated %d%% • Used %.1f mAh",
                        estimate.percent, usedMah)
                : String.format(java.util.Locale.US, "Battery • Used %.1f mAh", usedMah);
        String measuredPercentage = available ? estimate.percent + "%" : "--";
        String details = measuredPercentage + " • Screen ON: "
                + SessionStore.formatDuration(screenOnMs) + " • " + liveText;

        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);
        builder.setSmallIcon(R.drawable.ic_battery_percentage)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setContentTitle(title)
                .setContentText(details)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setCategory(Notification.CATEGORY_STATUS);
        return builder.build();
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Battery percentage", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Calculated battery percentage and session recording");
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            channel.setShowBadge(false);
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    @Override public void onTaskRemoved(Intent rootIntent) {
        scheduleRecovery();
        super.onTaskRemoved(rootIntent);
    }

    private void scheduleRecovery() {
        if (!shouldRunService()) return;
        try {
            AlarmManager alarm = (AlarmManager) getSystemService(ALARM_SERVICE);
            Intent recovery = new Intent(this, BootReceiver.class);
            recovery.setAction(BootReceiver.ACTION_RECOVER_SERVICE);
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
            PendingIntent pending = PendingIntent.getBroadcast(this, 9381, recovery, flags);
            long trigger = SystemClock.elapsedRealtime() + RECOVERY_DELAY_MS;
            if (alarm != null) {
                if (Build.VERSION.SDK_INT >= 23) alarm.setExactAndAllowWhileIdle(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP, trigger, pending);
                else alarm.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, trigger, pending);
            }
        } catch (Throwable ignored) { }
    }

    @Override public void onDestroy() {
        handler.removeCallbacks(updater);
        if (wakeLock != null && wakeLock.isHeld()) {
            try { wakeLock.release(); } catch (RuntimeException ignored) { }
        }
        if (shouldRunService()) scheduleRecovery();
        stopForeground(true);
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
