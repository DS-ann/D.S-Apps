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
import java.util.Locale;

public class BatteryNotificationService extends Service {
    private static final int NOTIFICATION_ID = 1001;
    private static final String CHANNEL_ID = "battery_monitor";
    private static final String PREFS = "settings";
    private static final String KEY_INTERVAL_MS = "sampling_interval_ms";
    private static final long CALCULATOR_INTERVAL_MS = 1000L;
    private long lastHistorySampleMs = 0L;
    private long lastNotificationUpdateMs = 0L;
    private final Handler handler = new Handler();
    private final Runnable updater = new Runnable() {
        @Override public void run() {
            updateNotification();
            handler.postDelayed(this, CALCULATOR_INTERVAL_MS);
        }
    };

    @Override public void onCreate() {
        super.onCreate();
        createChannel();
        startForeground(NOTIFICATION_ID, buildNotification("Reading battery...", 0.0, 0, BatteryPercentCalculator.current(this)));
        updateNotification();
        handler.postDelayed(updater, getSamplingInterval());
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    private long getSamplingInterval() {
        return getSharedPreferences(PREFS, MODE_PRIVATE).getLong(KEY_INTERVAL_MS, 15000L);
    }

    private void updateNotification() {
        if (!getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean("notification_enabled", false)) {
            stopSelf();
            return;
        }
        BatteryReader.Reading r = BatteryReader.read(this);
        BatteryPercentCalculator.Estimate estimate = BatteryPercentCalculator.sample(this, r);
        long now = System.currentTimeMillis();
        long interval = getSamplingInterval();

        // The independent percentage calculator uses 1-second current samples.
        // Keep heavier session-history writes and notification updates on the
        // user-selected history interval.
        if (lastHistorySampleMs == 0L || now < lastHistorySampleMs
                || now - lastHistorySampleMs >= interval) {
            SessionStore.sample(this, r);
            lastHistorySampleMs = now;
        }

        double mah = SessionStore.getActiveMah(this);
        long screenOnMs = SessionStore.getActiveScreenOnMs(this);
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null && (lastNotificationUpdateMs == 0L || now < lastNotificationUpdateMs
                || now - lastNotificationUpdateMs >= interval)) {
            nm.notify(NOTIFICATION_ID, buildNotification(r.compact(), mah, screenOnMs, estimate));
            lastNotificationUpdateMs = now;
        }
    }

    private Notification buildNotification(String text, double mah, long screenOnMs,
                                           BatteryPercentCalculator.Estimate estimate) {
        Intent open = new Intent(this, MainActivity.class);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pi = PendingIntent.getActivity(this, 0, open, flags);

        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);
        String title = estimate != null && estimate.isAvailable()
                ? String.format(Locale.US, "Calculated %d%% • Used %.1f mAh", estimate.percent, mah)
                : String.format(Locale.US, "Battery • Used %.1f mAh", mah);
        String remaining = estimate != null && estimate.isAvailable()
                ? String.format(Locale.US, "Remaining %.0f/%.0f mAh", estimate.remainingMah, estimate.capacityMah)
                : "Calculated percentage unavailable";
        b.setSmallIcon(android.R.drawable.ic_menu_info_details)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setContentTitle(title)
                .setContentText(remaining + " • Screen ON: " + SessionStore.formatDuration(screenOnMs) + " • " + text)
                .setContentIntent(pi)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setCategory(Notification.CATEGORY_STATUS);
        return b.build();
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel c = new NotificationChannel(
                    CHANNEL_ID, "Battery monitor", NotificationManager.IMPORTANCE_LOW);
            c.setDescription("Battery voltage, current and session usage");
            c.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            c.setShowBadge(false);
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (nm != null) nm.createNotificationChannel(c);
        }
    }

    @Override public void onDestroy() {
        handler.removeCallbacks(updater);
        // Do not finish the session here. Android/MIUI may destroy and recreate the
        // foreground service temporarily; the persisted session must survive that gap.
        stopForeground(true);
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) {
        return null;
    }
}