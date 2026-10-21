package com.dsann.batterymonitor;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;

public class BatteryNotificationService extends Service {
    private static final int NOTIFICATION_ID = 1001;
    private static final String CHANNEL_ID = "battery_monitor";
    private static final String PREFS = "settings";
    private static final String KEY_INTERVAL_MS = "sampling_interval_ms";
    private static final String KEY_MY_BATTERY_INTERVAL_MS = "my_battery_sampling_interval_ms";
    private static final long MIN_INTERVAL_MS = 100L;
    private static final long MAX_INTERVAL_MS = 2000L;
    private static final long DEFAULT_INTERVAL_MS = 1000L;
    private static final long MIN_HISTORY_WRITE_INTERVAL_MS = 500L;
    private static final long MIN_NOTIFICATION_UPDATE_INTERVAL_MS = 500L;
    private long lastHistorySampleMs = 0L;
    private long lastNotificationUpdateMs = 0L;
    private final Handler handler = new Handler();
    private final Runnable updater = new Runnable() {
        @Override public void run() {
            updateNotification();
            handler.postDelayed(this, getNextSamplingInterval());
        }
    };

    @Override public void onCreate() {
        super.onCreate();
        createChannel();
        startForeground(NOTIFICATION_ID,
                buildNotification("--", SessionStore.getActiveMah(this),
                        SessionStore.getActiveScreenOnMs(this), BatteryPercentCalculator.current(this)));
        updateNotification();
        handler.postDelayed(updater, getNextSamplingInterval());
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    private long getSamplingInterval() {
        long value = getSharedPreferences(PREFS, MODE_PRIVATE)
                .getLong(KEY_INTERVAL_MS, DEFAULT_INTERVAL_MS);
        return value < MIN_INTERVAL_MS || value > MAX_INTERVAL_MS
                ? DEFAULT_INTERVAL_MS : value;
    }

    private long getMyBatterySamplingInterval() {
        long value = getSharedPreferences(PREFS, MODE_PRIVATE)
                .getLong(KEY_MY_BATTERY_INTERVAL_MS, 500L);
        return value < 50L || value > 1500L ? 500L : value;
    }

    private long getNextSamplingInterval() {
        return Math.min(getSamplingInterval(), getMyBatterySamplingInterval());
    }

    private void updateNotification() {
        if (!getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean("notification_enabled", false)) {
            stopSelf();
            return;
        }

        BatteryReader.Reading reading = BatteryReader.read(this);
        BatteryPercentCalculator.Estimate estimate = BatteryPercentCalculator.sample(this, reading);
        long now = System.currentTimeMillis();
        long selectedInterval = getSamplingInterval();

        // Current integration follows the selected interval. The session log is throttled
        // slightly to avoid writing preferences ten times a second at the shortest setting.
        long historyInterval = Math.max(MIN_HISTORY_WRITE_INTERVAL_MS, selectedInterval);
        if (lastHistorySampleMs == 0L || now < lastHistorySampleMs
                || now - lastHistorySampleMs >= historyInterval) {
            SessionStore.sample(this, reading);
            lastHistorySampleMs = now;
        }

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        long notificationInterval = Math.max(MIN_NOTIFICATION_UPDATE_INTERVAL_MS,
                Math.min(1000L, selectedInterval));
        if (manager != null && (lastNotificationUpdateMs == 0L || now < lastNotificationUpdateMs
                || now - lastNotificationUpdateMs >= notificationInterval)) {
            double usedMah = SessionStore.getActiveMah(this);
            long screenOnMs = SessionStore.getActiveScreenOnMs(this);
            manager.notify(NOTIFICATION_ID,
                    buildNotification(reading.compact(), usedMah, screenOnMs, estimate));
            lastNotificationUpdateMs = now;
        }
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
            channel.setDescription("Calculated battery percentage");
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            channel.setShowBadge(false);
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    @Override public void onDestroy() {
        handler.removeCallbacks(updater);
        stopForeground(true);
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) {
        return null;
    }
}
