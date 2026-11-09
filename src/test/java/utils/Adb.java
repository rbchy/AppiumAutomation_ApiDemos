package utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Runs adb commands against one device and fails loudly if they do not succeed. */
public final class Adb {

    private Adb() {
    }

    /** {@code adb -s <device> emu <args>} — emulator console commands (calls, SMS, battery). */
    public static String emu(String device, String... args) {
        List<String> cmd = new ArrayList<>(List.of("adb", "-s", device, "emu"));
        cmd.addAll(List.of(args));
        return run(cmd);
    }

    /** {@code adb -s <device> shell <command>}. */
    public static String shell(String device, String command) {
        return run(List.of("adb", "-s", device, "shell", command));
    }

    private static String run(List<String> cmd) {
        try {
            Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            if (!p.waitFor(30, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                throw new IllegalStateException("Timed out: " + String.join(" ", cmd));
            }
            String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            if (p.exitValue() != 0 || out.contains("KO")) {
                throw new IllegalStateException(String.join(" ", cmd) + " failed: " + out);
            }
            return out;
        } catch (IOException e) {
            throw new IllegalStateException("adb is not on the PATH", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
