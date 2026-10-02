package model;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

/** Small, dependency-free runtime helpers shared by the desktop and core code. */
public final class RuntimeSupport {
    public static final String RESOLVED_ASSETS_PROPERTY = "pvz.assets.resolved";

    private RuntimeSupport() {
    }

    public static Path applicationDirectory() {
        try {
            URI location = RuntimeSupport.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            Path path = Paths.get(location).toAbsolutePath().normalize();
            return Files.isRegularFile(path) ? path.getParent() : path;
        } catch (URISyntaxException | RuntimeException ignored) {
            return Paths.get("").toAbsolutePath().normalize();
        }
    }

    public static Path defaultUserDataDirectory() {
        String configured = System.getenv("PVZ_USER_DATA_DIR");
        if (configured != null && !configured.isBlank()) {
            return Paths.get(configured.trim()).toAbsolutePath().normalize();
        }

        String appData = System.getenv(isWindows() ? "APPDATA" : "XDG_DATA_HOME");
        if (appData != null && !appData.isBlank()) {
            return Paths.get(appData, "phase-0-group-66").toAbsolutePath().normalize();
        }

        String userHome = System.getProperty("user.home", ".");
        return Paths.get(userHome, ".phase-0-group-66").toAbsolutePath().normalize();
    }

    public static Path cacheDirectory() {
        String localAppData = System.getenv(isWindows() ? "LOCALAPPDATA" : "XDG_CACHE_HOME");
        if (localAppData != null && !localAppData.isBlank()) {
            return Paths.get(localAppData, "phase-0-group-66").toAbsolutePath().normalize();
        }
        return defaultUserDataDirectory().resolve("cache").toAbsolutePath().normalize();
    }

    public static void setResolvedAssets(Path assetsDirectory) {
        if (assetsDirectory != null) {
            System.setProperty(RESOLVED_ASSETS_PROPERTY,
                assetsDirectory.toAbsolutePath().normalize().toString());
        }
    }

    public static void log(String component, String message, Throwable error) {
        StringBuilder entry = new StringBuilder()
            .append(Instant.now())
            .append(" [").append(component == null ? "Runtime" : component).append("] ")
            .append(message == null ? "" : message)
            .append(System.lineSeparator());
        if (error != null) {
            StringWriter stack = new StringWriter();
            error.printStackTrace(new PrintWriter(stack));
            entry.append(stack).append(System.lineSeparator());
        }

        try {
            Path logFile = defaultUserDataDirectory().resolve("logs").resolve("phase-0-group-66.log");
            Files.createDirectories(logFile.getParent());
            Files.writeString(logFile, entry, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {
            // Logging must never become another startup failure.
        }
        System.err.print(entry);
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("windows");
    }
}
