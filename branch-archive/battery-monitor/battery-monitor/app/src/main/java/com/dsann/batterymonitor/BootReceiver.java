package com.dsann.batterymonitor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class BootReceiver extends BroadcastReceiver {
    static final String ACTION_RECOVER_SERVICE = "com.dsann.batterymonitor.ACTION_RECOVER_SERVICE";
    private static final String PREFS = "settings";
    private static final String CALC_PREFS = "battery_percent_calculator_state";

    @Override public void onReceive(Context context, Intent intent) {
        String action = intent != null ? intent.getAction() : null;
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action)
                && !Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)
                && !ACTION_RECOVER_SERVICE.equals(action)) return;

        android.content.SharedPreferences settings =
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        boolean backgroundRecording = settings.getBoolean("notification_enabled", false);
        boolean useAppPercentage = settings.getBoolean("my_battery_use_android_percent", false);
        boolean resetPending = settings.getBoolean("my_battery_android_percent_reset_pending", false);
        boolean estimatorRunning = context.getSharedPreferences(CALC_PREFS, Context.MODE_PRIVATE)
                .getBoolean("running", false);
        if (!backgroundRecording && !useAppPercentage && !resetPending && !estimatorRunning) return;

        Intent service = new Intent(context, BatteryNotificationService.class);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(service);
            else context.startService(service);
        } catch (RuntimeException ignored) {
            // Recovery is also covered by START_STICKY and the service exact-alarm path.
        }
    }
}
