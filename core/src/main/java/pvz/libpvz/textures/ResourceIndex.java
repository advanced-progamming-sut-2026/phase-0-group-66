package pvz.libpvz.textures;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Parses and queries the 'resources.json' file. It maps image resource IDs to their
 * corresponding texture atlases and precise pixel boundaries within those atlases.
 */
public final class ResourceIndex {
    public static final class ImageEntry {
        public final String id;
        public final String atlasId;
        public final String path;
        public final int ax;
        public final int ay;
        public final int aw;
        public final int ah;
        ImageEntry(String id, String atlasId, String path, int ax, int ay, int aw, int ah) {
            this.id = id;
            this.atlasId = atlasId;
            this.path = path;
            this.ax = ax;
            this.ay = ay;
            this.aw = aw;
            this.ah = ah;
        }
    }
    public static final class AtlasEntry {
        public final String id;
        public final String path;
        public final int width;
        public final int height;

        AtlasEntry(String id, String path, int width, int height) {
            this.id = id;
            this.path = path;
            this.width = width;
            this.height = height;
        }
    }
    private final String preferredResolution;
    private final Map<String, ImageEntry> imageById;
    private final Map<String, AtlasEntry> atlasById;
    private final java.util.Set<String> animationAtlasIds;
    private ResourceIndex(String preferredResolution,
                          Map<String, ImageEntry> imageById,
                          Map<String, AtlasEntry> atlasById,
                          java.util.Set<String> animationAtlasIds) {
        this.preferredResolution = preferredResolution;
        this.imageById = imageById;
        this.atlasById = atlasById;
        this.animationAtlasIds = animationAtlasIds;
    }
    public ImageEntry image(String id) {
        return imageById.get(id);
    }
    public AtlasEntry atlas(String id) {
        return atlasById.get(id);
    }
    public boolean isAnimationAtlas(String atlasId) {
        return animationAtlasIds.contains(atlasId);
    }
    public String preferredResolution() {
        return preferredResolution;
    }
    public int imageCount() {
        return imageById.size();
    }
    public int atlasCount() {
        return atlasById.size();
    }
    public java.util.Set<String> imageIds() {
        return java.util.Collections.unmodifiableSet(imageById.keySet());
    }
    public java.util.Set<String> atlasIds() {
        return java.util.Collections.unmodifiableSet(atlasById.keySet());
    }
    public java.util.List<String> getImagesForAtlas(String atlasId) {
        java.util.List<String> result = new java.util.ArrayList<>();
        if (atlasId == null) return result;
        for (Map.Entry<String, ImageEntry> entry : imageById.entrySet()) {
            if (atlasId.equals(entry.getValue().atlasId)) {
                result.add(entry.getKey());
            }
        }
        return result;
    }
    public static ResourceIndex fromJson(FileHandle jsonFile, String preferredResolution) {
        JsonValue root = new JsonReader().parse(jsonFile);
        Map<String, Scored<ImageEntry>> images = new HashMap<>();
        Map<String, Scored<AtlasEntry>> atlases = new HashMap<>();
        java.util.Set<String> animFamilies = new java.util.HashSet<>();
        Map<String, String> atlasFamily = new HashMap<>();
        JsonValue groups = root.get("groups");
        for (JsonValue group = groups == null ? null : groups.child; group != null; group = group.next) {
            JsonValue resources = group.get("resources");
            if (resources == null) {
                continue; 
            }
            int score = score(preferredResolution, group.getString("res", null));
            String family = familyKey(group.getString("parent", null), group.getString("id", null));
            for (JsonValue res = resources.child; res != null; res = res.next) {
                String type = res.getString("type", "");
                if ("PopAnim".equalsIgnoreCase(type)) {
                    if (family != null) animFamilies.add(family);
                    continue;
                }
                if (!"Image".equalsIgnoreCase(type)) {
                    continue;
                }
                String id = res.getString("id", null);
                if (id == null || id.isEmpty()) {
                    continue;
                }
                if (res.getBoolean("atlas", false)) {
                    keepBest(atlases, id, new AtlasEntry(
                        id, res.getString("path", ""), res.getInt("width", 0), res.getInt("height", 0)), score);
                    if (family != null) atlasFamily.put(id, family);
                } else {
                    String parent = res.getString("parent", null);
                    if (parent == null || parent.isEmpty()) {
                        continue;
                    }
                    keepBest(images, id, new ImageEntry(id, parent, res.getString("path", ""),
                        res.getInt("ax", 0), res.getInt("ay", 0), res.getInt("aw", 0), res.getInt("ah", 0)), score);
                }
            }
        }
        return finish(preferredResolution, images, atlases, animationAtlases(atlasFamily, animFamilies));
    }
    private static ResourceIndex finish(String preferredResolution,
                                        Map<String, Scored<ImageEntry>> images,
                                        Map<String, Scored<AtlasEntry>> atlases,
                                        java.util.Set<String> animationAtlasIds) {
        Map<String, ImageEntry> imageById = new HashMap<>(images.size() * 2);
        images.forEach((id, scored) -> imageById.put(id, scored.value));
        Map<String, AtlasEntry> atlasById = new HashMap<>(atlases.size() * 2);
        atlases.forEach((id, scored) -> atlasById.put(id, scored.value));
        return new ResourceIndex(preferredResolution, imageById, atlasById, animationAtlasIds);
    }
    private static String familyKey(String parent, String groupId) {
        return parent != null && !parent.isEmpty() ? parent : groupId;
    }
    private static java.util.Set<String> animationAtlases(Map<String, String> atlasFamily,
                                                          java.util.Set<String> animFamilies) {
        java.util.Set<String> result = new java.util.HashSet<>();
        atlasFamily.forEach((atlasId, family) -> {
            if (animFamilies.contains(family)) {
                result.add(atlasId);
            }
        });
        return result;
    }
    private static int score(String preferredResolution, String groupRes) {
        if (groupRes == null) {
            return 1; 
        }
        return Objects.equals(preferredResolution, groupRes) ? 100 : 0;
    }
    private static <T> void keepBest(Map<String, Scored<T>> map, String id, T value, int score) {
        Scored<T> previous = map.get(id);
        if (previous == null || score >= previous.score) {
            map.put(id, new Scored<>(value, score));
        }
    }
    private static final class Scored<T> {
        final T value;
        final int score;
        Scored(T value, int score) {
            this.value = value;
            this.score = score;
        }
    }
}