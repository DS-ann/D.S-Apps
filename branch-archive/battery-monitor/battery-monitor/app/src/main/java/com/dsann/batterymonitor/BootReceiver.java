package com.dsann.batterymonitor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.os.Build;

public class BootReceiver extends BroadcastReceiver {
    private static final String ACTION_RECOVER =
            "com.dsann.batterymonitor.RECOVER_MEASUREMENT";
    private static final String ACTION_DOZE_WAKE =
            "com.dsann.batterymonitor.DOZE_WAKE";

    @Override public void onReceive(Context context, Intent intent) {
        String action = intent != null ? intent.getAction() : null;
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action)
                && !Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)
                && !ACTION_RECOVER.equals(action)
                && !ACTION_DOZE_WAKE.equals(action)) {
            return;
        }

        android.content.SharedPreferences settings =
                context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        if (DozeWhitelistController.isEnabled(context)) {
            Context appContext = context.getApplicationContext();
            Thread whitelistRestore = new Thread(
                    () -> DozeWhitelistController.reapplyIfEnabled(appContext),
                    "BatteryPulse-DozeWhitelist");
            whitelistRestore.setDaemon(true);
            whitelistRestore.start();
        }
        boolean recording = settings.getBoolean("notification_enabled", false);
        boolean alwaysBackground = settings.getBoolean("always_run_background", true);
        boolean useAppPercent =
                settings.getBoolean("my_battery_use_android_percent", false);
        boolean resetPending =
                settings.getBoolean("my_battery_android_percent_reset_pending", false);
        boolean estimatorRunning = context.getSharedPreferences(
                "battery_percent_calculator_state", Context.MODE_PRIVATE)
                .getBoolean("running", false);
        if (!recording && !alwaysBackground && !useAppPercent && !resetPending && !estimatorRunning) return;

        if (!ACTION_DOZE_WAKE.equals(action)) scheduleRecoveryRetry(context);

        Intent service = new Intent(context, BatteryNotificationService.class);
        if (ACTION_DOZE_WAKE.equals(action)) service.setAction(ACTION_DOZE_WAKE);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(service);
            } else {
                context.startService(service);
            }
        } catch (RuntimeException ignored) {
            // The retry alarm remains armed if Android or the OEM rejects this start attempt.
            scheduleRecoveryRetry(context);
        }
    }

    private static void scheduleRecoveryRetry(Context context) {
        try {
            Intent retry = new Intent(context, BootReceiver.class);
            retry.setAction(ACTION_RECOVER);
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
            PendingIntent pending = PendingIntent.getBroadcast(context, 9381, retry, flags);
            AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarm == null) return;
            long trigger = android.os.SystemClock.elapsedRealtime() + 15L * 60L * 1000L;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarm.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, trigger, pending);
            } else {
                alarm.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, trigger, pending);
            }
        } catch (Throwable ignored) { }
    }
}
