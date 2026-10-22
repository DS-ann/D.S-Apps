package com.dsann.batterymonitor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A lazily opened, process-wide root shell shared by battery reading and Android percentage
 * synchronization. Access is serialized so commands and their output cannot interleave.
 */
final class RootShell {
    private static final Object LOCK = new Object();
    private static final String READY_MARKER = "__BATTERYPULSE_ROOT_READY__";
    private static final long READY_TIMEOUT_MS = 8000L;
    private static final long COMMAND_TIMEOUT_MS = 4000L;

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

                final BufferedReader reader = output;
                FutureTask<CommandResult> task = new FutureTask<>(() -> {
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
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
                });
                Thread readerThread = new Thread(task, "BatteryPulse-RootCommand");
                readerThread.setDaemon(true);
                readerThread.start();
                try {
                    return task.get(COMMAND_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                } catch (TimeoutException timeout) {
                    task.cancel(true);
                    throw new IOException("Root command timed out after "
                            + COMMAND_TIMEOUT_MS + " ms.", timeout);
                } catch (ExecutionException failed) {
                    Throwable cause = failed.getCause();
                    throw new IOException(cause == null ? "Root command failed."
                            : String.valueOf(cause.getMessage()), cause);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    task.cancel(true);
                    throw new IOException("Root command interrupted.", interrupted);
                }
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

        final BufferedReader reader = output;
        FutureTask<String> task = new FutureTask<>(reader::readLine);
        Thread readerThread = new Thread(task, "BatteryPulse-RootStartup");
        readerThread.setDaemon(true);
        readerThread.start();
        try {
            String line = task.get(READY_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (READY_MARKER.equals(line == null ? "" : line.trim())) return;
            throw new IOException("Unable to open an authorized root shell.");
        } catch (TimeoutException timeout) {
            task.cancel(true);
            throw new IOException("Root shell startup timed out.", timeout);
        } catch (ExecutionException failed) {
            Throwable cause = failed.getCause();
            throw new IOException("Unable to read root shell startup marker.", cause);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            task.cancel(true);
            throw new IOException("Root shell startup interrupted.", interrupted);
        }
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
        // Destroy first: closing a BufferedReader while another thread is blocked inside
        // readLine() can itself wait on the reader lock.
        try {
            if (process != null) process.destroy();
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
