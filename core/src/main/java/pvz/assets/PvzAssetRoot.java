package pvz.assets;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import model.RuntimeSupport;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class PvzAssetRoot {
    private static final String ASSET_PROPERTY = "pvz.assets";
    private static final String ASSET_ENVIRONMENT = "PVZ_ASSETS_DIR";

    private PvzAssetRoot() {
    }

    public static FileHandle locate() {
        List<File> candidates = new ArrayList<>();

        String configured = System.getProperty(ASSET_PROPERTY);
        if (configured == null || configured.isBlank()) {
            configured = System.getenv(ASSET_ENVIRONMENT);
        }
        if (configured != null && !configured.isBlank()) {
            candidates.add(new File(configured.trim()));
        }

        File applicationDirectory = RuntimeSupport.applicationDirectory().toFile();
        candidates.add(new File(applicationDirectory, "assets"));
        File applicationParent = applicationDirectory.getParentFile();
        if (applicationParent != null) {
            candidates.add(new File(applicationParent, "assets"));
        }
        File workingDirectory = new File(System.getProperty("user.dir"));
        candidates.add(new File(workingDirectory, "assets"));
        candidates.add(workingDirectory);

        File parent = workingDirectory.getParentFile();
        if (parent != null) {
            candidates.add(new File(parent, "assets"));
        }

        for (File candidate : candidates) {
            FileHandle root = Gdx.files.absolute(candidate.getAbsolutePath());
            if (isValid(root)) {
                RuntimeSupport.setResolvedAssets(Path.of(root.file().toURI()));
                return root;
            }
        }

        FileHandle embedded = extractEmbeddedAssets();
        if (embedded != null) {
            RuntimeSupport.setResolvedAssets(Path.of(embedded.file().toURI()));
            return embedded;
        }

        throw new IllegalStateException(
            "PVZ asset root was not found. Expected a real assets directory containing "
                + "RESOURCES.json, ATLASES, and IMAGES. Current working directory: "
                + workingDirectory.getAbsolutePath()
        );
    }

    private static boolean isValid(FileHandle root) {
        if (root == null || !root.exists() || !root.isDirectory()) {
            return false;
        }

        FileHandle resources = root.child("RESOURCES.json");
        FileHandle atlases = root.child("ATLASES");
        FileHandle images = root.child("IMAGES");

        return resources.exists() && !resources.isDirectory()
            && atlases.exists() && atlases.isDirectory()
            && images.exists() && images.isDirectory();
    }

    private static FileHandle extractEmbeddedAssets() {
        Path codeLocation;
        try {
            codeLocation = Path.of(RuntimeSupport.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI()).toAbsolutePath().normalize();
        } catch (Exception exception) {
            RuntimeSupport.log("Assets", "Could not resolve the application JAR.", exception);
            return null;
        }

        if (!Files.isRegularFile(codeLocation)) {
            return null;
        }

        Path target = RuntimeSupport.cacheDirectory().resolve("embedded-assets");
        Path marker = target.resolve(".complete");
        try {
            if (Files.isRegularFile(marker) && isValid(Gdx.files.absolute(target.toString()))) {
                return Gdx.files.absolute(target.toString());
            }

            deleteRecursively(target);
            Path partial = target.resolveSibling("embedded-assets.partial-" + UUID.randomUUID());
            Files.createDirectories(partial);
            try (JarFile jar = new JarFile(codeLocation.toFile())) {
                boolean found = false;
                var entries = jar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (entry.isDirectory() || !name.startsWith("assets/")) {
                        continue;
                    }
                    Path destination = partial.resolve(name.substring("assets/".length())).normalize();
                    if (!destination.startsWith(partial)) {
                        throw new IOException("Invalid embedded asset path: " + name);
                    }
                    Files.createDirectories(destination.getParent());
                    try (InputStream input = jar.getInputStream(entry)) {
                        Files.copy(input, destination, StandardCopyOption.REPLACE_EXISTING);
                    }
                    found = true;
                }
                if (!found) {
                    deleteRecursively(partial);
                    return null;
                }
            }
            Files.writeString(partial.resolve(".complete"), "ok\n", StandardOpenOption.CREATE);
            try {
                Files.move(partial, target, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicMoveUnsupported) {
                Files.move(partial, target);
            }
            FileHandle extracted = Gdx.files.absolute(target.toString());
            return isValid(extracted) ? extracted : null;
        } catch (IOException | RuntimeException exception) {
            RuntimeSupport.log("Assets", "Could not extract embedded assets.", exception);
            return null;
        }
    }

    private static void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        try (var paths = Files.walk(path)) {
            paths.sorted((left, right) -> right.compareTo(left)).forEach(value -> {
                try {
                    Files.deleteIfExists(value);
                } catch (IOException exception) {
                    throw new AssetCleanupException(exception);
                }
            });
        } catch (AssetCleanupException exception) {
            throw exception.cause;
        }
    }

    private static final class AssetCleanupException extends RuntimeException {
        private final IOException cause;

        private AssetCleanupException(IOException cause) {
            this.cause = cause;
        }
    }
}
