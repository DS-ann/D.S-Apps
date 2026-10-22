package com.dsann.batterymonitor;

import java.io.BufferedReader;
import java.io.InputStreamReader;

final class BatteryPercentOverride {
    private BatteryPercentOverride() {}

    static Result set(int percent) {
        if (percent < 0 || percent > 100) {
            return new Result(false, "Enter a value from 0 to 100.");
        }
        return runRoot("dumpsys battery set level " + percent,
                "Battery percentage set to " + percent + "%.");
    }

    /**
     * The "set" commands freeze Android's battery updates. Keep the framework's simulated
     * power state synchronized with physical charger state so the status-bar icon, charge
     * state broadcasts, and BatteryService-controlled charge LED continue to behave correctly.
     */
    static Result set(int percent, BatteryReader.PowerInfo powerInfo) {
        return set(percent, powerInfo, -1, -1, -1);
    }

    /**
     * Update only fields which changed. Re-applying every field for every percentage step
     * can unnecessarily trigger repeated battery-state broadcasts and charge-light updates.
     */
    static Result set(int percent, BatteryReader.PowerInfo powerInfo, int previousPercent,
                      int previousStatus, int previousSourceMask) {
        if (percent < 0 || percent > 100) {
            return new Result(false, "Enter a value from 0 to 100.");
        }
        if (powerInfo == null) return set(percent);

        java.util.ArrayList<String> commands = new java.util.ArrayList<>();
        if (previousPercent != percent) {
            commands.add("dumpsys battery set level " + percent);
        }
        if (previousStatus != powerInfo.androidStatus) {
            commands.add("dumpsys battery set status " + powerInfo.androidStatus);
        }
        if (previousSourceMask < 0 || (previousSourceMask & 1) != (powerInfo.sourceMask() & 1)) {
            commands.add("dumpsys battery set ac " + (powerInfo.acOnline ? 1 : 0));
        }
        if (previousSourceMask < 0 || (previousSourceMask & 2) != (powerInfo.sourceMask() & 2)) {
            commands.add("dumpsys battery set usb " + (powerInfo.usbOnline ? 1 : 0));
        }
        if (previousSourceMask < 0 || (previousSourceMask & 4) != (powerInfo.sourceMask() & 4)) {
            commands.add("dumpsys battery set wireless " + (powerInfo.wirelessOnline ? 1 : 0));
        }
        if (commands.isEmpty()) {
            return new Result(true, "Battery percentage and charging state are already synchronized.");
        }

        StringBuilder command = new StringBuilder();
        for (String step : commands) {
            if (command.length() > 0) command.append(" && ");
            command.append(step);
        }
        return runRoot(command.toString(),
                "Battery percentage and charging state synchronized at " + percent + "%.");
    }

    static Result reset() {
        return runRoot("dumpsys battery reset",
                "Battery percentage reset to the real value.");
    }

    private static Result runRoot(String command, String successMessage) {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", command});
            StringBuilder output = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (output.length() > 0) output.append('\n');
                    output.append(line);
                }
            }
            StringBuilder error = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(p.getErrorStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (error.length() > 0) error.append('\n');
                    error.append(line);
                }
            }
            int code = p.waitFor();
            if (code == 0) return new Result(true, successMessage);
            String detail = error.length() > 0 ? error.toString() : output.toString();
            return new Result(false, "Root command failed" +
                    (detail.isEmpty() ? "." : ": " + detail));
        } catch (Exception e) {
            return new Result(false, "Root access failed: " + e.getMessage());
        }
    }

    static final class Result {
        final boolean success;
        final String message;

        Result(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }
}
