package pvz.assets;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Resolves the shipped PAM catalog instead of duplicating asset paths in UI code. */
public final class AnimationCatalog {
    public record Entry(String name, String path, List<String> clips) {
    }

    private final Map<String, List<Entry>> byName = new HashMap<>();

    public AnimationCatalog(FileHandle assetsRoot) {
        FileHandle file = assetsRoot == null ? null : assetsRoot.child("animations.json");
        if (file == null || !file.exists() || file.isDirectory()) {
            return;
        }
        JsonValue root = new JsonReader().parse(file).get("animations");
        for (JsonValue item = root == null ? null : root.child; item != null; item = item.next) {
            String name = item.getString("name", "");
            String path = item.getString("path", "");
            if (name.isBlank() || path.isBlank()) {
                continue;
            }
            ArrayList<String> clips = new ArrayList<>();
            JsonValue clipObject = item.get("clips");
            for (JsonValue clip = clipObject == null ? null : clipObject.child;
                 clip != null;
                 clip = clip.next) {
                clips.add(clip.name);
            }
            Entry entry = new Entry(name, path, List.copyOf(clips));
            byName.computeIfAbsent(normalize(name), ignored -> new ArrayList<>()).add(entry);
        }
    }

    public Entry findInCategory(String category, String... names) {
        if (names == null) {
            return null;
        }
        for (String name : names) {
            if (name == null || name.isBlank()) {
                continue;
            }
            List<Entry> entries = byName.get(normalize(name));
            if (entries == null) {
                continue;
            }
            if (category == null || category.isBlank()) {
                return entries.get(0);
            }
            String marker = "/" + category.toUpperCase(Locale.ROOT) + "/";
            for (Entry entry : entries) {
                if (entry.path.toUpperCase(Locale.ROOT).contains(marker)) {
                    return entry;
                }
            }
        }
        return null;
    }

    public Entry plant(String... names) {
        return findInCategory("PLANT", names);
    }

    public Entry zombie(String... names) {
        return findInCategory("ZOMBIE", names);
    }

    public Entry effect(String... names) {
        return findInCategory("EFFECTS", names);
    }

    public Entry background(String... names) {
        return findInCategory("BACKGROUNDS", names);
    }

    public Entry mower(String... names) {
        return findInCategory("MOWERS", names);
    }

    public int size() {
        int count = 0;
        for (List<Entry> entries : byName.values()) {
            count += entries.size();
        }
        return count;
    }

    public static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
    }
}
