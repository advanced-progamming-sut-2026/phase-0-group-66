package pvz.assets;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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
                return root;
            }
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
}
