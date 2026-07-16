package model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public final class DataFileLocator {
    private static final String DATA_DIRECTORY_PROPERTY = "pvz.data.dir";
    private static final String DATA_DIRECTORY_ENVIRONMENT = "PVZ_DATA_DIR";

    private DataFileLocator() {
    }

    public static Path locate(String fileName) throws IOException {
        for (Path directory : candidateDirectories()) {
            Path candidate = directory.resolve(fileName).normalize();
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        throw new IOException("Data file was not found: " + fileName
            + ". Set -D" + DATA_DIRECTORY_PROPERTY + "=<data-directory> if needed.");
    }

    private static List<Path> candidateDirectories() {
        ArrayList<Path> directories = new ArrayList<>();
        addConfiguredPath(directories, System.getProperty(DATA_DIRECTORY_PROPERTY));
        addConfiguredPath(directories, System.getenv(DATA_DIRECTORY_ENVIRONMENT));
        directories.add(Paths.get("src", "assets", "data"));
        directories.add(Paths.get("assets", "data"));
        directories.add(Paths.get("data"));
        return directories;
    }

    private static void addConfiguredPath(List<Path> directories, String configuredPath) {
        if (configuredPath != null && !configuredPath.isBlank()) {
            directories.add(Paths.get(configuredPath.trim()));
        }
    }
}
