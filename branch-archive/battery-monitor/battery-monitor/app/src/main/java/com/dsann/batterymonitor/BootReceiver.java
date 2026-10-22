package com.dsann.batterymonitor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class BootReceiver extends BroadcastReceiver {
    private static final String ACTION_RECOVER =
            "com.dsann.batterymonitor.RECOVER_MEASUREMENT";

    @Override public void onReceive(Context context, Intent intent) {
        String action = intent != null ? intent.getAction() : null;
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action)
                && !Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)
                && !ACTION_RECOVER.equals(action)) {
            return;
        }

        android.content.SharedPreferences settings =
                context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        boolean recording = settings.getBoolean("notification_enabled", false);
        boolean useAppPercent =
                settings.getBoolean("my_battery_use_android_percent", false);
        boolean resetPending =
                settings.getBoolean("my_battery_android_percent_reset_pending", false);
        boolean estimatorRunning = context.getSharedPreferences(
                "battery_percent_calculator_state", Context.MODE_PRIVATE)
                .getBoolean("running", false);
        if (!recording && !useAppPercent && !resetPending && !estimatorRunning) return;

        Intent service = new Intent(context, BatteryNotificationService.class);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(service);
            } else {
                context.startService(service);
            }
        } catch (RuntimeException ignored) {
            // START_STICKY and the scheduled alarm provide further recovery opportunities.
        }
    }
}
