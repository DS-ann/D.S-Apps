package com.dsann.batterymonitor;

public final class DozeController {
    private DozeController() {}

    public static Result forceDoze() {
        return runRoot("dumpsys deviceidle force-idle", "Doze forced on.");
    }

    public static Result disableForceDoze() {
        return runRoot("dumpsys deviceidle unforce",
                "Forced Doze disabled. Normal idle behavior restored.");
    }

    private static Result runRoot(String command, String success) {
        RootShell.CommandResult result = RootShell.execute(command);
        if (result.success) return new Result(true, success);
        String message = result.output == null ? "" : result.output.trim();
        if (message.isEmpty()) message = "Command failed (exit code " + result.exitCode + ").";
        return new Result(false, message);
    }

    public static final class Result {
        public final boolean success;
        public final String message;
        Result(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }
}
