package pvz.libpvz.textures;

import com.badlogic.gdx.files.FileHandle;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class AtlasLocator {
    private final Map<String, FileHandle> pngByBaseName = new HashMap<>();
    public AtlasLocator(FileHandle atlasesDirectory) {
        if (atlasesDirectory == null || !atlasesDirectory.exists() || !atlasesDirectory.isDirectory()) {
            throw new IllegalArgumentException("ATLASES directory not found: " + atlasesDirectory);
        }
        for (FileHandle file : atlasesDirectory.list()) {
            if (file.isDirectory()) {
                continue;
            }
            if (!"png".equals(file.extension().toLowerCase(Locale.ROOT))) {
                continue; 
            }
            pngByBaseName.put(file.nameWithoutExtension().toLowerCase(Locale.ROOT), file);
        }
    }
    public FileHandle resolve(String atlasPath) {
        if (atlasPath == null) {
            return null;
        }
        return pngByBaseName.get(baseName(atlasPath).toLowerCase(Locale.ROOT));
    }
    public int pngCount() {
        return pngByBaseName.size();
    }
    private static String baseName(String path) {
        int slash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return slash >= 0 ? path.substring(slash + 1) : path;
    }
}