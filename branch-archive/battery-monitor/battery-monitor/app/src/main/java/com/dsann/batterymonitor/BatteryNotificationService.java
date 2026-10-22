package com.dsann.batterymonitor;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Icon;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

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
    private static final String KEY_LAST_ANDROID_POWER_STATUS = "my_battery_last_android_power_status";
    private static final String KEY_LAST_ANDROID_POWER_SOURCE = "my_battery_last_android_power_source";
    private static final String KEY_CALCULATOR_RUNNING = "running";
    private static final String KEY_NOTIFICATION_ENABLED = "notification_enabled";
    private static final long MIN_INTERVAL_MS = 100L;
    private static final long MAX_INTERVAL_MS = 2000L;
    private static final long DEFAULT_INTERVAL_MS = 1000L;
    private static final long MIN_MY_BATTERY_INTERVAL_MS = 50L;
    private static final long MAX_MY_BATTERY_INTERVAL_MS = 1500L;
    private static final long DEFAULT_MY_BATTERY_INTERVAL_MS = 500L;
    private static final long NOTIFICATION_REFRESH_INTERVAL_MS = 1000L;
    private static final long ROOT_RETRY_GAP_MS = 5000L;
    private static final long RECOVERY_DELAY_MS = 15000L;
    private static final String ACTION_RECOVER = "com.dsann.batterymonitor.RECOVER_MEASUREMENT";
    private static final String ACTION_DOZE_WAKE = "com.dsann.batterymonitor.DOZE_WAKE";
    private static final int DOZE_ALARM_REQUEST_CODE = 9382;
    private static final long DOZE_WAKE_INTERVAL_MS = 10L * 60L * 1000L;
    private static final long DOZE_WAKE_WINDOW_MS = 10000L;

    private final Handler handler = new Handler();
    private long lastHistorySampleMs;
    private long lastCalculatorSampleMs;
    private long nextNotificationUpdateUptimeMs;
    private long lastRootCommandMs;
    private int lastAttemptedPowerStatus = -1;
    private int lastAttemptedPowerSource = -1;
    private volatile boolean rootCommandInFlight;
    private final ExecutorService rootExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService samplingExecutor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean sampleInFlight = new AtomicBoolean(false);
    private volatile BatteryReader.Reading latestReading =
            new BatteryReader.Reading(Long.MIN_VALUE, Long.MIN_VALUE);
    private volatile long latestReadingAtMs;
    private PowerManager.WakeLock wakeLock;
    private boolean dozeSamplingMode;
    private boolean dozeWakeCycleInFlight;
    private boolean powerStateReceiverRegistered;
    private long dozeWakeCycleToken;
    private Runnable dozeWakeTimeout;
    private volatile Runnable pendingSampleCompletion;

    private final BroadcastReceiver powerStateReceiver = new BroadcastReceiver() {
        @Override public void onReceive(android.content.Context context, Intent intent) {
            handlePowerStateChanged();
        }
    };

    private final Runnable updater = new Runnable() {
        @Override public void run() {
            try {
                if (shouldRunService()) {
                    requestSampleUpdate();
                } else {
                    stopSelf();
                }
            } catch (Throwable ignored) {
                // A failed sample must not cancel the notification refresh loop.
            } finally {
                if (shouldRunService()) handler.postDelayed(this, getNextSamplingInterval());
            }
        }
    };

    private final Runnable notificationUpdater = new Runnable() {
        @Override public void run() {
            try {
                if (shouldRunService()) {
                    publishNotification();
                } else {
                    stopSelf();
                }
            } catch (Throwable ignored) {
                // A notification failure must not cancel the next refresh.
            } finally {
                if (shouldRunService()) {
                    long now = SystemClock.uptimeMillis();
                    if (nextNotificationUpdateUptimeMs == 0L) {
                        nextNotificationUpdateUptimeMs = now;
                    }
                    nextNotificationUpdateUptimeMs += NOTIFICATION_REFRESH_INTERVAL_MS;
                    if (nextNotificationUpdateUptimeMs <= now) {
                        nextNotificationUpdateUptimeMs = now + NOTIFICATION_REFRESH_INTERVAL_MS;
                    }
                    handler.postAtTime(this, nextNotificationUpdateUptimeMs);
                } else {
                    nextNotificationUpdateUptimeMs = 0L;
                }
            }
        }
    };

    @Override public void onCreate() {
        super.onCreate();
        createChannel();
        startForeground(NOTIFICATION_ID,
                buildNotification("--", 0.0, 0L, BatteryPercentCalculator.current(this)));

        IntentFilter powerFilter = new IntentFilter();
        powerFilter.addAction(Intent.ACTION_SCREEN_ON);
        powerFilter.addAction(Intent.ACTION_SCREEN_OFF);
        powerFilter.addAction(Intent.ACTION_POWER_CONNECTED);
        powerFilter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            powerFilter.addAction(PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED);
        }
        try {
            registerReceiver(powerStateReceiver, powerFilter);
            powerStateReceiverRegistered = true;
        } catch (Throwable ignored) { }

        if (!shouldRunService()) {
            stopSelf();
            return;
        }

        if (shouldUseDozeSamplingMode()) {
            dozeSamplingMode = true;
            SessionStore.markDozeGapExpected(this);
            cancelRecoveryAlarm();
            scheduleDozeWakeup();
            publishNotification();
        } else {
            acquireCpuLock();
            requestSampleUpdate();
            publishNotification();
            startRegularLoops(false);
        }
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (!shouldRunService()) {
            stopSelf();
            return START_NOT_STICKY;
        }

        if (shouldUseDozeSamplingMode()) {
            enterDozeMode(false);
            runDozeWakeCycle();
            return START_STICKY;
        }

        enterActiveMode(true);
        return START_STICKY;
    }

    private void startRegularLoops(boolean runImmediately) {
        handler.removeCallbacks(updater);
        handler.removeCallbacks(notificationUpdater);
        if (runImmediately) handler.post(updater);
        else handler.postDelayed(updater, getNextSamplingInterval());
        nextNotificationUpdateUptimeMs = SystemClock.uptimeMillis()
                + NOTIFICATION_REFRESH_INTERVAL_MS;
        handler.postAtTime(notificationUpdater, nextNotificationUpdateUptimeMs);
    }

    private boolean shouldUseDozeSamplingMode() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return false;
        PowerManager manager = (PowerManager) getSystemService(POWER_SERVICE);
        if (manager == null || manager.isInteractive()) return false;

        boolean pluggedIn;
        try {
            Intent battery = registerReceiver(null,
                    new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (battery == null) return false;
            pluggedIn = battery.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0;
        } catch (Throwable ignored) {
            return false;
        }
        // Screen-on or connected-to-power always restores ordinary monitoring.
        if (pluggedIn) return false;

        // Enter reduced sampling only after Android confirms active Doze.
        if (manager.isDeviceIdleMode()) return true;

        // Android reports isDeviceIdleMode() as false during maintenance windows.
        // If Doze sampling has already begun and the device remains screen-off/unplugged,
        // keep the low-power schedule through that window rather than taking a continuous
        // wake lock for the brief period when Android lifts idle restrictions.
        return dozeSamplingMode;
    }

    private void handlePowerStateChanged() {
        if (!shouldRunService()) {
            stopSelf();
            return;
        }
        boolean shouldUseDozeMode = shouldUseDozeSamplingMode();
        if (shouldUseDozeMode && !dozeSamplingMode) {
            enterDozeMode(true);
        } else if (!shouldUseDozeMode && dozeSamplingMode) {
            enterActiveMode(true);
        }
    }

    private void enterDozeMode(boolean sampleImmediately) {
        if (!dozeSamplingMode) {
            dozeSamplingMode = true;
            SessionStore.markDozeGapExpected(this);
            handler.removeCallbacks(updater);
            handler.removeCallbacks(notificationUpdater);
            nextNotificationUpdateUptimeMs = 0L;
            releaseCpuLock();
            cancelRecoveryAlarm();
            scheduleDozeWakeup();
        }
        if (sampleImmediately) runDozeWakeCycle();
    }

    private void enterActiveMode(boolean sampleImmediately) {
        if (dozeSamplingMode) {
            dozeSamplingMode = false;
            cancelDozeWakeup();
            // Invalidate any pending Doze-cycle callbacks and discard its timed lock.
            dozeWakeCycleToken++;
            dozeWakeCycleInFlight = false;
            if (dozeWakeTimeout != null) handler.removeCallbacks(dozeWakeTimeout);
            dozeWakeTimeout = null;
            pendingSampleCompletion = null;
            releaseCpuLock();
            acquireCpuLock();
        } else if (wakeLock == null || !wakeLock.isHeld()) {
            acquireCpuLock();
        }
        if (sampleImmediately) publishNotification();
        startRegularLoops(sampleImmediately);
    }

    private void runDozeWakeCycle() {
        if (!dozeSamplingMode || !shouldRunService() || dozeWakeCycleInFlight) return;

        SessionStore.markDozeGapExpected(this);
        dozeWakeCycleInFlight = true;
        final long cycleToken = ++dozeWakeCycleToken;
        acquireCpuLock(DOZE_WAKE_WINDOW_MS);
        scheduleDozeWakeup();

        Runnable timeout = new Runnable() {
            @Override public void run() {
                if (cycleToken != dozeWakeCycleToken) return;
                dozeWakeCycleInFlight = false;
                dozeWakeTimeout = null;
                releaseCpuLock();
            }
        };
        dozeWakeTimeout = timeout;
        handler.postDelayed(timeout, DOZE_WAKE_WINDOW_MS);

        requestSampleUpdate(new Runnable() {
            @Override public void run() {
                if (cycleToken != dozeWakeCycleToken) return;
                if (dozeWakeTimeout != null) handler.removeCallbacks(dozeWakeTimeout);
                dozeWakeTimeout = null;
                dozeWakeCycleInFlight = false;
                publishNotification();
                releaseCpuLock();
            }
        });
    }

    private void acquireCpuLock() {
        acquireCpuLock(0L);
    }

    private void acquireCpuLock(long timeoutMs) {
        try {
            PowerManager manager = (PowerManager) getSystemService(POWER_SERVICE);
            if (manager != null) {
                if (wakeLock == null) {
                    wakeLock = manager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                            "BatteryPulse:bounded-measurement");
                    wakeLock.setReferenceCounted(false);
                }
                if (timeoutMs > 0L && wakeLock.isHeld()) wakeLock.release();
                if (!wakeLock.isHeld()) {
                    if (timeoutMs > 0L) wakeLock.acquire(timeoutMs);
                    else wakeLock.acquire();
                }
            }
        } catch (Throwable ignored) { }
    }

    private void releaseCpuLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            try { wakeLock.release(); } catch (RuntimeException ignored) { }
        }
    }

    private PendingIntent getDozeWakePendingIntent(int intentFlags) {
        Intent wake = new Intent(this, BootReceiver.class);
        wake.setAction(ACTION_DOZE_WAKE);
        int flags = intentFlags;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(this, DOZE_ALARM_REQUEST_CODE, wake, flags);
    }

    private void scheduleDozeWakeup() {
        if (!shouldRunService()) return;
        try {
            AlarmManager alarm = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (alarm == null) return;
            PendingIntent pending = getDozeWakePendingIntent(PendingIntent.FLAG_UPDATE_CURRENT);
            long trigger = SystemClock.elapsedRealtime() + DOZE_WAKE_INTERVAL_MS;
            // A non-exact idle-allowed alarm avoids requiring special exact-alarm access.
            // Android may defer delivery, but the alarm can run while Doze is active.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarm.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, trigger, pending);
            } else {
                alarm.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, trigger, pending);
            }
        } catch (Throwable ignored) { }
    }

    private void cancelDozeWakeup() {
        try {
            int flags = PendingIntent.FLAG_NO_CREATE;
            if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
            PendingIntent pending = getDozeWakePendingIntent(flags);
            if (pending == null) return;
            AlarmManager alarm = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (alarm != null) alarm.cancel(pending);
            pending.cancel();
        } catch (Throwable ignored) { }
    }

    private void cancelRecoveryAlarm() {
        try {
            Intent recovery = new Intent(this, BootReceiver.class);
            recovery.setAction(ACTION_RECOVER);
            int flags = PendingIntent.FLAG_NO_CREATE;
            if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
            PendingIntent pending = PendingIntent.getBroadcast(this, 9381, recovery, flags);
            if (pending == null) return;
            AlarmManager alarm = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (alarm != null) alarm.cancel(pending);
            pending.cancel();
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

    /**
     * Keep sensor/root operations off the main thread. A blocked or slow sysfs/root operation
     * must not stop the foreground notification's refresh timer.
     */
    private void requestSampleUpdate() {
        requestSampleUpdate(null);
    }

    private void requestSampleUpdate(final Runnable completion) {
        if (!sampleInFlight.compareAndSet(false, true)) {
            if (completion != null) {
                pendingSampleCompletion = completion;
                // Close the small race where the existing sample finishes just as
                // this request discovers that another sample is in flight.
                if (!sampleInFlight.get()) {
                    Runnable deferred = pendingSampleCompletion;
                    pendingSampleCompletion = null;
                    if (deferred != null) handler.post(deferred);
                }
            }
            return;
        }
        try {
            samplingExecutor.execute(() -> {
                try {
                    sampleAndSyncData();
                } catch (Throwable ignored) {
                    // Keep last good sensor reading; the independent notifier remains live.
                } finally {
                    sampleInFlight.set(false);
                    Runnable deferred = pendingSampleCompletion;
                    pendingSampleCompletion = null;
                    if (completion != null) handler.post(completion);
                    if (deferred != null && deferred != completion) handler.post(deferred);
                }
            });
        } catch (RejectedExecutionException ignored) {
            sampleInFlight.set(false);
            Runnable deferred = pendingSampleCompletion;
            pendingSampleCompletion = null;
            if (completion != null) handler.post(completion);
            if (deferred != null && deferred != completion) handler.post(deferred);
        }
    }

    private void sampleAndSyncData() {
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
        latestReading = reading;
        latestReadingAtMs = System.currentTimeMillis();
        now = latestReadingAtMs;

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
    }

    /**
     * Publish the latest cached sensor reading independently of the sensor/root worker.
     * Android's own level/status and screen-on duration continue refreshing if a read stalls.
     */
    private void publishNotification() {
        android.content.SharedPreferences settings = getSharedPreferences(PREFS, MODE_PRIVATE);
        boolean recordingEnabled = settings.getBoolean(KEY_NOTIFICATION_ENABLED, false);
        boolean usePercent = settings.getBoolean(KEY_USE_ANDROID_PERCENT, false);
        boolean resetPending = settings.getBoolean(KEY_RESET_PENDING, false);
        boolean estimating = getSharedPreferences(CALC_PREFS, MODE_PRIVATE)
                .getBoolean(KEY_CALCULATOR_RUNNING, false);
        if (!recordingEnabled && !usePercent && !resetPending && !estimating) return;

        long now = System.currentTimeMillis();
        long myBatteryInterval = getMyBatterySamplingInterval();

        BatteryReader.Reading reading = latestReading;
        String liveText = reading.compact();
        long readingAge = latestReadingAtMs == 0L ? Long.MAX_VALUE : now - latestReadingAtMs;
        if (latestReadingAtMs > 0L && readingAge > Math.max(5000L, myBatteryInterval * 4L)) {
            liveText = "Sensor reading delayed • " + liveText;
        } else if (latestReadingAtMs == 0L) {
            liveText = "Reading battery data…";
        }

        NotificationManager manager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID,
                    buildNotification(liveText, SessionStore.getActiveMah(this),
                            SessionStore.getActiveScreenOnMs(this),
                            BatteryPercentCalculator.current(this)));
        }
    }

    private void applyCalculatedAndroidPercentage(BatteryPercentCalculator.Estimate estimate, long now) {
        if (rootCommandInFlight) return;

        android.content.SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        int requested = (int) Math.round(estimate.percentExact);
        BatteryReader.PowerInfo powerInfo = BatteryChargingOverride.powerInfoForAndroid(
                this, BatteryReader.getPowerInfo(this));
        int status = powerInfo.androidStatus;
        int sourceMask = powerInfo.sourceMask();
        int previousPercent = prefs.getInt(KEY_LAST_ANDROID_PERCENT, -1);
        int previousStatus = prefs.getInt(KEY_LAST_ANDROID_POWER_STATUS, -1);
        int previousSourceMask = prefs.getInt(KEY_LAST_ANDROID_POWER_SOURCE, -1);

        boolean percentChanged = previousPercent != requested;
        boolean powerStateChanged = previousStatus != status
                || previousSourceMask != sourceMask;
        if (!percentChanged && !powerStateChanged) return;

        // Percentage changes can be rate-limited, but a physical charge/unplug transition
        // should synchronize immediately. Failed state updates are still throttled.
        boolean newPhysicalStateSinceAttempt = status != lastAttemptedPowerStatus
                || sourceMask != lastAttemptedPowerSource;
        if (lastRootCommandMs > 0L && now >= lastRootCommandMs
                && now - lastRootCommandMs < ROOT_RETRY_GAP_MS
                && !newPhysicalStateSinceAttempt) return;

        lastRootCommandMs = now;
        lastAttemptedPowerStatus = status;
        lastAttemptedPowerSource = sourceMask;
        rootCommandInFlight = true;
        rootExecutor.execute(() -> {
            BatteryPercentOverride.Result result = BatteryPercentOverride.set(
                    requested, powerInfo, previousPercent, previousStatus, previousSourceMask);
            handler.post(() -> {
                rootCommandInFlight = false;
                android.content.SharedPreferences current = getSharedPreferences(PREFS, MODE_PRIVATE);
                if (result.success) {
                    current.edit()
                            .putInt(KEY_LAST_ANDROID_PERCENT, requested)
                            .putInt(KEY_LAST_ANDROID_POWER_STATUS, status)
                            .putInt(KEY_LAST_ANDROID_POWER_SOURCE, sourceMask)
                            .putBoolean(KEY_RESET_PENDING, false)
                            .apply();
                } else {
                    current.edit().remove(KEY_LAST_ANDROID_PERCENT)
                            .remove(KEY_LAST_ANDROID_POWER_STATUS)
                            .remove(KEY_LAST_ANDROID_POWER_SOURCE)
                            .apply();
                }
            });
        });
    }

    private void retryPendingAndroidPercentageReset(long now) {
        if(rootCommandInFlight)return;
        android.content.SharedPreferences prefs=getSharedPreferences(PREFS,MODE_PRIVATE);
        if(!prefs.getBoolean(KEY_RESET_PENDING,false))return;
        if(lastRootCommandMs>0L&&now>=lastRootCommandMs&&now-lastRootCommandMs<ROOT_RETRY_GAP_MS)return;
        lastRootCommandMs=now;
        rootCommandInFlight=true;
        rootExecutor.execute(()->{
            BatteryPercentOverride.Result result=BatteryChargingOverride.resetAndroidBattery(this);
            handler.post(()->{
                rootCommandInFlight=false;
                if(result.success){
                    getSharedPreferences(PREFS,MODE_PRIVATE).edit()
                            .putBoolean(KEY_RESET_PENDING,false)
                            .remove(KEY_LAST_ANDROID_PERCENT)
                            .remove(KEY_LAST_ANDROID_POWER_STATUS)
                            .remove(KEY_LAST_ANDROID_POWER_SOURCE)
                            .apply();
                }
            });
        });
    }

    private Notification buildNotification(String liveText, double usedMah, long screenOnMs,
                                           BatteryPercentCalculator.Estimate estimate) {
        Intent open=new Intent(this,MainActivity.class);
        int flags=PendingIntent.FLAG_UPDATE_CURRENT;
        if(Build.VERSION.SDK_INT>=23)flags|=PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pending=PendingIntent.getActivity(this,0,open,flags);

        boolean available=estimate!=null&&estimate.running&&estimate.isAvailable();
        BatteryReader.PowerState powerState=BatteryReader.getPowerState(this);
        String action=powerState==BatteryReader.PowerState.NOT_CHARGING
                ?"Discharging":powerState.notificationLabel();
        String title=available
                ?String.format(java.util.Locale.US,"%.2f%% • %s • %.1f mAh",
                        estimate.percentExact,action,usedMah)
                :String.format(java.util.Locale.US,"Battery • %s • %.1f mAh",action,usedMah);
        String rateText=ratePerHourText(latestReading,estimate);

        Notification.Builder builder=Build.VERSION.SDK_INT>=26
                ?new Notification.Builder(this,CHANNEL_ID)
                :new Notification.Builder(this);
        int iconPercent=available?(int)Math.round(estimate.percentExact):getAndroidBatteryPercent();
        if(Build.VERSION.SDK_INT>=23&&iconPercent>=0){
            builder.setSmallIcon(createPercentageIcon(iconPercent));
        }else{
            builder.setSmallIcon(R.drawable.ic_battery_percentage);
        }
        String details=rateText+" • Screen ON: "
                +SessionStore.formatDuration(screenOnMs)+" • "+liveText;
        builder.setVisibility(Notification.VISIBILITY_PUBLIC)
                .setContentTitle(title)
                .setContentText(details)
                .setContentIntent(pending)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setCategory(Notification.CATEGORY_STATUS);
        return builder.build();
    }

    private String ratePerHourText(BatteryReader.Reading reading,
                                   BatteryPercentCalculator.Estimate estimate){
        double capacityMah=estimate==null?Double.NaN:estimate.capacityMah;
        if(Double.isNaN(capacityMah)||Double.isInfinite(capacityMah)||capacityMah<100.0){
            capacityMah=BatteryPercentCalculator.getCustomCapacityMah(this);
        }
        double rate=BatteryRateAverager.ratePerHour(reading,capacityMah);
        if(Double.isNaN(rate)||Double.isInfinite(rate))return "--%/h";
        return String.format(java.util.Locale.US,"%+.2f%%/h",rate);
    }

    private int getAndroidBatteryPercent(){
        Intent battery=registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if(battery==null)return -1;
        int level=battery.getIntExtra(BatteryManager.EXTRA_LEVEL,-1);
        int scale=battery.getIntExtra(BatteryManager.EXTRA_SCALE,100);
        if(level<0||scale<=0)return -1;
        return Math.max(0,Math.min(100,Math.round(level*100.0f/scale)));
    }

    private Icon createPercentageIcon(int percentage){
        int value=Math.max(0,Math.min(100,percentage));
        Bitmap bitmap=Bitmap.createBitmap(48,48,Bitmap.Config.ARGB_8888);
        Canvas canvas=new Canvas(bitmap);
        Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.4f);
        canvas.drawCircle(24.0f,24.0f,20.0f,paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(value>=100?14.5f:(value>=10?19.0f:24.0f));
        Paint.FontMetrics metrics=paint.getFontMetrics();
        float baseline=24.0f-(metrics.ascent+metrics.descent)/2.0f;
        canvas.drawText(String.valueOf(value),24.0f,baseline,paint);
        return Icon.createWithBitmap(bitmap);
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Battery percentage", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Battery percentage and session recording");
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
            // Use a receiver so Android 8+ can legally restart the foreground service.
            Intent recovery = new Intent(this, BootReceiver.class);
            recovery.setAction(ACTION_RECOVER);
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
            PendingIntent pending = PendingIntent.getBroadcast(this, 9381, recovery, flags);
            long trigger = SystemClock.elapsedRealtime() + RECOVERY_DELAY_MS;
            if (alarm != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    // Recovery is best-effort; avoid depending on exact-alarm special access.
                    alarm.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, trigger, pending);
                } else {
                    alarm.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, trigger, pending);
                }
            }
        } catch (Throwable ignored) { }
    }

    @Override public void onDestroy() {
        handler.removeCallbacks(updater);
        handler.removeCallbacks(notificationUpdater);
        if (dozeWakeTimeout != null) handler.removeCallbacks(dozeWakeTimeout);
        dozeWakeTimeout = null;
        dozeWakeCycleToken++;
        dozeWakeCycleInFlight = false;
        nextNotificationUpdateUptimeMs = 0L;

        if (powerStateReceiverRegistered) {
            try { unregisterReceiver(powerStateReceiver); } catch (Throwable ignored) { }
            powerStateReceiverRegistered = false;
        }
        releaseCpuLock();

        if (shouldRunService()) {
            if (shouldUseDozeSamplingMode()) {
                cancelRecoveryAlarm();
                scheduleDozeWakeup();
            } else {
                cancelDozeWakeup();
                scheduleRecovery();
            }
        } else {
            cancelDozeWakeup();
            cancelRecoveryAlarm();
        }
        rootExecutor.shutdown();
        samplingExecutor.shutdownNow();
        stopForeground(true);
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
