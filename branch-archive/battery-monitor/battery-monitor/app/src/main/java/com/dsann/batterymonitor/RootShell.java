package com.dsann.batterymonitor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

/**
 * A lazily opened, process-wide root shell shared by battery reading and Android percentage
 * synchronization. Access is serialized so commands and their output cannot interleave.
 */
final class RootShell {
    private static final Object LOCK = new Object();
    private static final String READY_MARKER = "__BATTERYPULSE_ROOT_READY__";

    private static Process process;
    private static BufferedWriter input;
    private static BufferedReader output;
    private static long commandSequence;

    private RootShell() {}

    static CommandResult execute(String command) {
        if (command == null || command.trim().isEmpty()) {
            return new CommandResult(false, "Empty root command.", 2);
        }
        synchronized (LOCK) {
            try {
                ensureStartedLocked();
                long sequence = ++commandSequence;
                String marker = "__BATTERYPULSE_COMMAND_COMPLETE_" + sequence + "__:";
                input.write("( " + command + " ) 2>&1");
                input.newLine();
                input.write("__bp_exit=$?");
                input.newLine();
                input.write("echo " + marker + "$__bp_exit");
                input.newLine();
                input.flush();

                StringBuilder result = new StringBuilder();
                String line;
                while ((line = output.readLine()) != null) {
                    if (line.startsWith(marker)) {
                        int exitCode;
                        try {
                            exitCode = Integer.parseInt(line.substring(marker.length()).trim());
                        } catch (NumberFormatException badStatus) {
                            throw new IOException("Invalid root command status.");
                        }
                        return new CommandResult(exitCode == 0, result.toString(), exitCode);
                    }
                    if (result.length() > 0) result.append('\n');
                    result.append(line);
                }
                throw new IOException("Root shell closed before completing the command.");
            } catch (Exception error) {
                closeLocked();
                String message = error.getMessage();
                return new CommandResult(false,
                        message == null || message.trim().isEmpty()
                                ? "Root shell failed." : "Root shell failed: " + message,
                        127);
            }
        }
    }

    private static void ensureStartedLocked() throws IOException {
        if (isAliveLocked()) return;
        closeLocked();

        ProcessBuilder builder = new ProcessBuilder("su");
        builder.redirectErrorStream(true);
        process = builder.start();
        input = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
        output = new BufferedReader(new InputStreamReader(process.getInputStream()));

        input.write("echo " + READY_MARKER);
        input.newLine();
        input.flush();

        String line;
        while ((line = output.readLine()) != null) {
            if (READY_MARKER.equals(line.trim())) return;
        }
        throw new IOException("Unable to open an authorized root shell.");
    }

    private static boolean isAliveLocked() {
        if (process == null) return false;
        try {
            process.exitValue();
            return false;
        } catch (IllegalThreadStateException stillRunning) {
            return true;
        }
    }

    private static void closeLocked() {
        try {
            if (input != null) {
                input.write("exit");
                input.newLine();
                input.flush();
            }
        } catch (Exception ignored) {
        }
        try {
            if (input != null) input.close();
        } catch (Exception ignored) {
        }
        try {
            if (output != null) output.close();
        } catch (Exception ignored) {
        }
        try {
            if (process != null) process.destroy();
        } catch (Exception ignored) {
        }
        input = null;
        output = null;
        process = null;
    }

    static final class CommandResult {
        final boolean success;
        final String output;
        final int exitCode;

        CommandResult(boolean success, String output, int exitCode) {
            this.success = success;
            this.output = output == null ? "" : output;
            this.exitCode = exitCode;
        }
    }
}
