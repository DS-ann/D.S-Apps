package com.dsann.batterymonitor;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

final class BatteryReader {
    private static final String FG_CURRENT =
            "/sys/devices/platform/battery_meter/FG_Current";
    private static final String INSTAT_VOLT =
            "/sys/class/power_supply/battery/InstatVolt";

    private static RootReader rootReader;

    private BatteryReader() {}

    enum PowerState {
        CHARGING("Charging", true, BatteryManager.BATTERY_STATUS_CHARGING),
        DISCHARGING("Discharging", false, BatteryManager.BATTERY_STATUS_DISCHARGING),
        FULL("Fully charged", false, BatteryManager.BATTERY_STATUS_FULL),
        NOT_CHARGING("Not charging", false, BatteryManager.BATTERY_STATUS_NOT_CHARGING),
        UNKNOWN("Power state unavailable", false, BatteryManager.BATTERY_STATUS_UNKNOWN);

        private final String notificationLabel;
        private final boolean charging;
        private final int androidStatus;

        PowerState(String notificationLabel, boolean charging, int androidStatus) {
            this.notificationLabel = notificationLabel;
            this.charging = charging;
            this.androidStatus = androidStatus;
        }

        String notificationLabel() {
            return notificationLabel;
        }

        boolean isCharging() {
            return charging;
        }
    }

    static final class PowerInfo {
        final PowerState state;
        final int androidStatus;
        final boolean acOnline;
        final boolean usbOnline;
        final boolean wirelessOnline;

        PowerInfo(PowerState state, boolean acOnline, boolean usbOnline, boolean wirelessOnline) {
            this.state = state == null ? PowerState.UNKNOWN : state;
            this.acOnline = acOnline;
            this.usbOnline = usbOnline;
            this.wirelessOnline = wirelessOnline;
            boolean powered = acOnline || usbOnline || wirelessOnline;
            this.androidStatus = this.state.androidStatus != BatteryManager.BATTERY_STATUS_UNKNOWN
                    ? this.state.androidStatus
                    : (powered ? BatteryManager.BATTERY_STATUS_CHARGING
                               : BatteryManager.BATTERY_STATUS_DISCHARGING);
        }

        int sourceMask() {
            return (acOnline ? 1 : 0) | (usbOnline ? 2 : 0) | (wirelessOnline ? 4 : 0);
        }

        boolean isPowered() {
            return acOnline || usbOnline || wirelessOnline;
        }
    }

    /**
     * Prefer Linux power-supply sysfs, which keeps reflecting physical state even while
     * "dumpsys battery set level" freezes Android BatteryService updates.
     */
    static PowerState getPowerState(Context context) {
        return getPowerInfo(context).state;
    }

    static PowerInfo getPowerInfo(Context context) {
        PowerState state = parsePowerState(
                readTextSysfs("/sys/class/power_supply/battery/status"));
        Intent battery = null;
        if (state == PowerState.UNKNOWN) {
            battery = context.registerReceiver(null,
                    new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (battery != null) {
                state = parseAndroidStatus(battery.getIntExtra(BatteryManager.EXTRA_STATUS,
                        BatteryManager.BATTERY_STATUS_UNKNOWN));
            }
        }

        PhysicalPowerSource source = readPhysicalPowerSource();
        boolean acOnline;
        boolean usbOnline;
        boolean wirelessOnline;
        if (source.known) {
            acOnline = source.acOnline;
            usbOnline = source.usbOnline;
            wirelessOnline = source.wirelessOnline;
        } else {
            if (battery == null) {
                battery = context.registerReceiver(null,
                        new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            }
            int plugged = battery == null ? 0 : battery.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
            acOnline = (plugged & BatteryManager.BATTERY_PLUGGED_AC) != 0;
            usbOnline = (plugged & BatteryManager.BATTERY_PLUGGED_USB) != 0;
            wirelessOnline = (plugged & BatteryManager.BATTERY_PLUGGED_WIRELESS) != 0;
        }

        // With sysfs source nodes unavailable, do not let a frozen framework "plugged" flag
        // survive a physical transition to discharging.
        if (!source.known && state == PowerState.DISCHARGING) {
            acOnline = false;
            usbOnline = false;
            wirelessOnline = false;
        }

        // The physical battery status is stronger evidence than stale framework flags.
        // If sysfs does not expose the charger node, infer USB power while charging/full.
        if ((state == PowerState.CHARGING || state == PowerState.FULL)
                && !acOnline && !usbOnline && !wirelessOnline) {
            usbOnline = true;
        }
        if (state == PowerState.UNKNOWN) {
            state = acOnline || usbOnline || wirelessOnline
                    ? PowerState.CHARGING : PowerState.DISCHARGING;
        }
        return new PowerInfo(state, acOnline, usbOnline, wirelessOnline);
    }

    private static PowerState parseAndroidStatus(int status) {
        switch (status) {
            case BatteryManager.BATTERY_STATUS_CHARGING:
                return PowerState.CHARGING;
            case BatteryManager.BATTERY_STATUS_DISCHARGING:
                return PowerState.DISCHARGING;
            case BatteryManager.BATTERY_STATUS_FULL:
                return PowerState.FULL;
            case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
                return PowerState.NOT_CHARGING;
            default:
                return PowerState.UNKNOWN;
        }
    }

    private static PowerState parsePowerState(String status) {
        if (status == null) return PowerState.UNKNOWN;
        String value = status.trim();
        if ("Charging".equalsIgnoreCase(value)) return PowerState.CHARGING;
        if ("Discharging".equalsIgnoreCase(value)) return PowerState.DISCHARGING;
        if ("Full".equalsIgnoreCase(value)) return PowerState.FULL;
        if ("Not charging".equalsIgnoreCase(value)) return PowerState.NOT_CHARGING;
        return PowerState.UNKNOWN;
    }

    private static final class PhysicalPowerSource {
        final boolean known;
        final boolean acOnline;
        final boolean usbOnline;
        final boolean wirelessOnline;

        PhysicalPowerSource(boolean known, boolean acOnline, boolean usbOnline,
                            boolean wirelessOnline) {
            this.known = known;
            this.acOnline = acOnline;
            this.usbOnline = usbOnline;
            this.wirelessOnline = wirelessOnline;
        }
    }

    private static PhysicalPowerSource readPhysicalPowerSource() {
        boolean known = false;
        boolean ac = false;
        boolean usb = false;
        boolean wireless = false;
        File directory = new File("/sys/class/power_supply");
        File[] supplies = directory.listFiles();
        if (supplies == null) return new PhysicalPowerSource(false, false, false, false);

        for (File supply : supplies) {
            String type = readTextSysfs(new File(supply, "type").getAbsolutePath());
            String identity = ((type == null ? "" : type) + " " + supply.getName())
                    .toLowerCase(java.util.Locale.US);
            if (identity.contains("battery")) continue;
            boolean isAc = identity.contains("mains") || identity.matches(".*\\bac\\b.*");
            boolean isUsb = identity.contains("usb");
            boolean isWireless = identity.contains("wireless");
            if (!isAc && !isUsb && !isWireless) continue;

            Long online = readSysfs(new File(supply, "online").getAbsolutePath());
            if (online == null) continue;
            known = true;
            if (online.longValue() == 0L) continue;
            if (isAc) ac = true;
            if (isUsb) usb = true;
            if (isWireless) wireless = true;
        }
        return new PhysicalPowerSource(known, ac, usb, wireless);
    }

    private static final long READ_CACHE_MS = 40L;
    private static Reading cachedReading;
    private static long cachedReadAtMs;

    static synchronized Reading read(Context context) {
        long now = System.currentTimeMillis();
        if (cachedReading != null && now >= cachedReadAtMs && now - cachedReadAtMs < READ_CACHE_MS) {
            return cachedReading;
        }
        cachedReading = readUncached(context);
        cachedReadAtMs = now;
        return cachedReading;
    }

    private static Reading readUncached(Context context) {
        Long currentRaw = readSysfs(FG_CURRENT);
        Long voltageMv = readSysfs(INSTAT_VOLT);

        // On this MTK phone FG_Current is reported in 0.1 mA units.
        // Example: 2437 means 243.7 mA, not 2437 mA.
        if (currentRaw != null) {
            long currentUa = currentRaw * 100L;
            return new Reading(
                    voltageMv != null ? voltageMv * 1000L : Long.MIN_VALUE,
                    currentUa
            );
        }

        // Keep one persistent su process instead of starting su once per read.
        RootReading rr = readAsRoot();
        if (rr != null) {
            currentRaw = rr.currentRaw;
            if (voltageMv == null) voltageMv = rr.voltageMv;
        }

        if (currentRaw != null) {
            long currentUa = currentRaw * 100L;
            return new Reading(
                    voltageMv != null ? voltageMv * 1000L : Long.MIN_VALUE,
                    currentUa
            );
        }

        // Generic Android/sysfs fallback: these current values are in microamps.
        BatteryManager bm = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
        if (bm != null && Build.VERSION.SDK_INT >= 21) {
            long currentUa = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
            if (currentUa != 0) {
                return new Reading(
                        voltageMv != null ? voltageMv * 1000L : Long.MIN_VALUE,
                        currentUa
                );
            }
        }

        Long fallbackUa = readSysfs("/sys/class/power_supply/battery/current_now");
        if (fallbackUa != null) {
            return new Reading(
                    voltageMv != null ? voltageMv * 1000L : Long.MIN_VALUE,
                    fallbackUa
            );
        }

        if (voltageMv == null) {
            android.content.Intent battery =
                    context.registerReceiver(null, new android.content.IntentFilter(
                            android.content.Intent.ACTION_BATTERY_CHANGED));
            if (battery != null) {
                int mv = battery.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0);
                if (mv > 0) voltageMv = (long) mv;
            }
        }

        if (voltageMv == null) {
            Long v = readSysfs("/sys/class/power_supply/battery/voltage_now");
            if (v != null) voltageMv = Math.round(v / 1000.0);
        }

        return new Reading(
                voltageMv != null ? voltageMv * 1000L : Long.MIN_VALUE,
                Long.MIN_VALUE
        );
    }

    private static synchronized RootReading readAsRoot() {
        try {
            if (rootReader == null || !rootReader.isAlive()) {
                if (rootReader != null) rootReader.close();
                rootReader = new RootReader();
            }
            return rootReader.read();
        } catch (Exception e) {
            if (rootReader != null) rootReader.close();
            rootReader = null;
            return null;
        }
    }

    private static String readTextSysfs(String path) {
        File f = new File(path);
        if (!f.canRead()) return null;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String value = br.readLine();
            return value == null ? null : value.trim();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Long readSysfs(String path) {
        File f = new File(path);
        if (!f.canRead()) return null;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String s = br.readLine();
            if (s == null) return null;
            return Long.parseLong(s.trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    static final class RootReader {
        private final Process process;
        private final BufferedWriter stdin;
        private final BufferedReader stdout;

        RootReader() throws IOException {
            process = Runtime.getRuntime().exec(new String[]{"su"});
            stdin = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
            stdout = new BufferedReader(new InputStreamReader(process.getInputStream()));
        }

        boolean isAlive() {
            try {
                process.exitValue();
                return false;
            } catch (IllegalThreadStateException e) {
                return true;
            }
        }

        RootReading read() throws IOException {
            stdin.write("cat " + FG_CURRENT);
            stdin.newLine();
            stdin.write("cat " + INSTAT_VOLT);
            stdin.newLine();
            stdin.flush();

            String current = stdout.readLine();
            String voltage = stdout.readLine();
            if (current == null || voltage == null) throw new IOException("su closed");

            return new RootReading(
                    Long.parseLong(current.trim()),
                    Long.parseLong(voltage.trim())
            );
        }

        void close() {
            try { stdin.write("exit"); stdin.newLine(); stdin.flush(); } catch (Exception ignored) {}
            try { stdin.close(); } catch (Exception ignored) {}
            try { stdout.close(); } catch (Exception ignored) {}
            process.destroy();
        }
    }

    static final class RootReading {
        final long currentRaw;
        final long voltageMv;

        RootReading(long currentRaw, long voltageMv) {
            this.currentRaw = currentRaw;
            this.voltageMv = voltageMv;
        }
    }

    static final class Reading {
        final long voltageUv;
        final long currentUa;

        Reading(long voltageUv, long currentUa) {
            this.voltageUv = voltageUv;
            this.currentUa = currentUa;
        }

        String voltageText() {
            return voltageUv > 0
                    ? String.format(java.util.Locale.US, "%.3f V", voltageUv / 1_000_000.0)
                    : "N/A";
        }

        String currentText() {
            if (currentUa == Long.MIN_VALUE) return "N/A";
            return String.format(java.util.Locale.US, "%+.1f mA",
                    currentUa / 1000.0);
        }

        String compact() {
            return voltageText() + "  •  " + currentText();
        }
    }
}
