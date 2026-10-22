package com.dsann.batterymonitor;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Color;
import android.graphics.drawable.Icon;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    private boolean rootCommandInFlight;
    private final ExecutorService rootExecutor=Executors.newSingleThreadExecutor();
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
        if(rootCommandInFlight)return;
        android.content.SharedPreferences prefs=getSharedPreferences(PREFS,MODE_PRIVATE);
        int requested=(int)Math.round(estimate.percentExact);
        if(prefs.getInt(KEY_LAST_ANDROID_PERCENT,-1)==requested)return;
        if(lastRootCommandMs>0L&&now>=lastRootCommandMs&&now-lastRootCommandMs<ROOT_RETRY_GAP_MS)return;
        lastRootCommandMs=now;
        rootCommandInFlight=true;
        rootExecutor.execute(()->{
            BatteryPercentOverride.Result result=BatteryPercentOverride.set(requested);
            handler.post(()->{
                rootCommandInFlight=false;
                android.content.SharedPreferences current=getSharedPreferences(PREFS,MODE_PRIVATE);
                if(result.success){
                    current.edit().putInt(KEY_LAST_ANDROID_PERCENT,requested)
                            .putBoolean(KEY_RESET_PENDING,false).apply();
                }else{
                    current.edit().remove(KEY_LAST_ANDROID_PERCENT).apply();
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
            BatteryPercentOverride.Result result=BatteryPercentOverride.reset();
            handler.post(()->{
                rootCommandInFlight=false;
                if(result.success){
                    getSharedPreferences(PREFS,MODE_PRIVATE).edit()
                            .putBoolean(KEY_RESET_PENDING,false).remove(KEY_LAST_ANDROID_PERCENT).apply();
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
        boolean charging=isPluggedIn();
        String action=charging?"Charged":"Used";
        String title=available
                ?String.format(java.util.Locale.US,"Calculated %.2f%% • %s %.1f mAh",
                        estimate.percentExact,action,usedMah)
                :String.format(java.util.Locale.US,"Battery • %s %.1f mAh",action,usedMah);
        String measuredPercentage=available
                ?String.format(java.util.Locale.US,"%.2f%%",estimate.percentExact):"--";

        Notification.Builder builder=Build.VERSION.SDK_INT>=26
                ?new Notification.Builder(this,CHANNEL_ID)
                :new Notification.Builder(this);
        if(available&&Build.VERSION.SDK_INT>=23){
            builder.setSmallIcon(createBatteryIcon(estimate.percentExact));
        }else{
            builder.setSmallIcon(R.drawable.ic_battery_percentage);
        }
        String details=measuredPercentage+" • Screen ON: "
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

    private boolean isPluggedIn(){
        Intent battery=registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        return battery!=null&&battery.getIntExtra(BatteryManager.EXTRA_PLUGGED,0)!=0;
    }

    private Icon createBatteryIcon(double percentage){
        Bitmap bitmap=Bitmap.createBitmap(48,48,Bitmap.Config.ARGB_8888);
        Canvas canvas=new Canvas(bitmap);
        Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(3.5f);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawRoundRect(new RectF(5,8,39,40),4,4,paint);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(new RectF(40,18,46,30),2,2,paint);
        float clamped=(float)Math.max(0.0,Math.min(100.0,percentage));
        float fillWidth=26.0f*clamped/100.0f;
        if(fillWidth>0)canvas.drawRect(9,12,9+fillWidth,36,paint);
        return Icon.createWithBitmap(bitmap);
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
            Intent recovery = new Intent(this, BatteryNotificationService.class);
            recovery.setAction("com.dsann.batterymonitor.RECOVER_MEASUREMENT");
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
            PendingIntent pending = PendingIntent.getService(this, 9381, recovery, flags);
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
        rootExecutor.shutdown();
        stopForeground(true);
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
