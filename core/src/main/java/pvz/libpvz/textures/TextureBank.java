package pvz.libpvz.textures;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Manages the loading, caching, and resolution of texture atlases and image regions used by PAM animations. */
public final class TextureBank implements Disposable {
    private final ResourceIndex resourceIndex;
    private final AtlasLocator atlasLocator;
    private final Map<String, Texture> textureByAtlasId = new HashMap<>();
    private final Map<String, TextureRegion> regionByImageId = new HashMap<>();
    private final Map<String, Set<String>> imageIdsByAtlasId = new HashMap<>(); 
    private final AssetManager assetManager = new AssetManager();
    private final Map<String, List<Runnable>> pendingAsyncCallbacks = new HashMap<>();
    
    /**
     * Initializes the TextureBank.
     * @param resolution The target resolution prefix (e.g. "1200", "768").
     * @param assetsFolder The root directory containing 'resources.json' and the 'atlases' folder.
     */
    public TextureBank(String resolution, FileHandle assetsFolder) {
        FileHandle resourcesJson = assetsFolder.child("resources.json");
        if (!resourcesJson.exists()) {
            resourcesJson = assetsFolder.child("RESOURCES.json");
        }
        this.resourceIndex = ResourceIndex.fromJson(resourcesJson, resolution);
        
        FileHandle atlasesDir = assetsFolder.child("atlases");
        if (!atlasesDir.exists()) {
            atlasesDir = assetsFolder.child("ATLASES");
        }
        this.atlasLocator = new AtlasLocator(atlasesDir);
    }
    public ResourceIndex getResourceIndex() {
        return resourceIndex;
    }

    public TextureRegion region(String imageResourceId) {
        TextureRegion cached = regionByImageId.get(imageResourceId);
        if (cached != null) {
            return cached;
        }
        ResourceIndex.ImageEntry image = resourceIndex.image(imageResourceId);
        if (image == null) {
            return null;
        }
        Texture texture = atlasTexture(image.atlasId);
        if (texture == null) {
            return null;
        }
        TextureRegion region = new TextureRegion(texture, image.ax, image.ay, image.aw, image.ah);
        regionByImageId.put(imageResourceId, region);
        imageIdsByAtlasId.computeIfAbsent(image.atlasId, k -> new HashSet<>()).add(imageResourceId);
        return region;
    }
    public TextureRegion atlas(String atlasId) {
        Texture texture = atlasTexture(atlasId);
        return texture == null ? null : new TextureRegion(texture);
    }
    /** Unloads a specific atlas from memory to free up resources. */
    public void unloadAtlas(String atlasId) {
        Texture texture = textureByAtlasId.remove(atlasId);
        if (texture != null) {
            texture.dispose();
        }
        Set<String> imageIds = imageIdsByAtlasId.remove(atlasId);
        if (imageIds != null) {
            for (String imageId : imageIds) {
                regionByImageId.remove(imageId);
            }
        }
    }

    public FileHandle atlasFile(String atlasId) {
        ResourceIndex.AtlasEntry atlas = resourceIndex.atlas(atlasId);
        if (atlas == null) {
            return null;
        }
        return atlasLocator.resolve(atlas.path);
    }
    public int exportAtlasParts(String atlasId, FileHandle outDir) {
        ResourceIndex.AtlasEntry atlas = resourceIndex.atlas(atlasId);
        if (atlas == null) {
            return 0;
        }
        FileHandle png = atlasLocator.resolve(atlas.path);
        if (png == null) {
            return 0;
        }
        Pixmap source = new Pixmap(png);
        outDir.mkdirs();
        int count = 0;
        for (String imageId : resourceIndex.imageIds()) {
            ResourceIndex.ImageEntry img = resourceIndex.image(imageId);
            if (img == null || !atlasId.equals(img.atlasId) || img.aw <= 0 || img.ah <= 0) {
                continue;
            }
            Pixmap part = new Pixmap(img.aw, img.ah, source.getFormat());
            part.drawPixmap(source, 0, 0, img.ax, img.ay, img.aw, img.ah);
            PixmapIO.writePNG(outDir.child(partFileName(img)), part);
            part.dispose();
            count++;
        }
        source.dispose();
        return count;
    }
    public int exportImage(String imageId, FileHandle outDir) {
        String atlasId = resourceIndex.image(imageId).atlasId;
        ResourceIndex.AtlasEntry atlas = resourceIndex.atlas(atlasId);
        if (atlas == null) {
            return 0;
        }
        FileHandle png = atlasLocator.resolve(atlas.path);
        if (png == null) {
            return 0;
        }
        Pixmap source = new Pixmap(png);
        outDir.mkdirs();
        int count = 0;
        for (String image : resourceIndex.imageIds()) {
            if(image != imageId) continue;
            ResourceIndex.ImageEntry img = resourceIndex.image(image);
            if (img == null || !atlasId.equals(img.atlasId) || img.aw <= 0 || img.ah <= 0) {
                continue;
            }
            Pixmap part = new Pixmap(img.aw, img.ah, source.getFormat());
            part.drawPixmap(source, 0, 0, img.ax, img.ay, img.aw, img.ah);
            PixmapIO.writePNG(outDir.child(partFileName(img)), part);
            part.dispose();
            count++;
        }
        source.dispose();
        return count;
    }

    private static String partFileName(ResourceIndex.ImageEntry img) {
        String name = img.path == null ? "" : img.path.replace('\\', '/');
        int slash = name.lastIndexOf('/');
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        return (name.isEmpty() ? img.id : name) + ".png";
    }
    public ResourceIndex resourceIndex() {
        return resourceIndex;
    }
    public int loadedTextureCount() {
        return textureByAtlasId.size();
    }
    /** Must be called continuously in your game's render loop to process asynchronous texture loading. */
    public void update() {
        assetManager.update();
        
        List<String> completedAtlases = null;
        for (Map.Entry<String, List<Runnable>> entry : pendingAsyncCallbacks.entrySet()) {
            String atlasId = entry.getKey();
            ResourceIndex.AtlasEntry atlas = resourceIndex.atlas(atlasId);
            if (atlas == null) {
                if (completedAtlases == null) completedAtlases = new ArrayList<>();
                completedAtlases.add(atlasId);
                continue;
            }
            FileHandle png = atlasLocator.resolve(atlas.path);
            if (png == null || assetManager.isLoaded(png.path())) {
                if (png != null && assetManager.isLoaded(png.path()) && !textureByAtlasId.containsKey(atlasId)) {
                    Texture texture = assetManager.get(png.path(), Texture.class);
                    texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                    textureByAtlasId.put(atlasId, texture);
                }
                if (completedAtlases == null) completedAtlases = new ArrayList<>();
                completedAtlases.add(atlasId);
            }
        }
        
        if (completedAtlases != null) {
            for (String atlasId : completedAtlases) {
                List<Runnable> callbacks = pendingAsyncCallbacks.remove(atlasId);
                if (callbacks != null) {
                    for (Runnable callback : callbacks) {
                        callback.run();
                    }
                }
            }
        }
    }
    public void loadSync(String atlasId) {
        resolveTexture(atlasId);
    }
    public void loadAsync(String atlasId, Runnable callback) {
        if (textureByAtlasId.containsKey(atlasId)) {
            if (callback != null) callback.run();
            return;
        }
        ResourceIndex.AtlasEntry atlas = resourceIndex.atlas(atlasId);
        if (atlas == null) {
            if (callback != null) callback.run();
            return;
        }
        FileHandle png = atlasLocator.resolve(atlas.path);
        if (png == null) {
            if (callback != null) callback.run();
            return;
        }
        if (!assetManager.contains(png.path())) {
            assetManager.load(png.path(), Texture.class);
        }
        if (callback != null) {
            pendingAsyncCallbacks.computeIfAbsent(atlasId, k -> new ArrayList<>()).add(callback);
        }
    }
    private Texture atlasTexture(String atlasId) {
        Texture cached = textureByAtlasId.get(atlasId);
        if (cached != null) {
            return cached;
        }
        return resolveTexture(atlasId);
    }
    private Texture resolveTexture(String atlasId) {
        Texture cached = textureByAtlasId.get(atlasId);
        if (cached != null) {
            return cached;
        }
        ResourceIndex.AtlasEntry atlas = resourceIndex.atlas(atlasId);
        if (atlas == null) {
            return null;
        }
        FileHandle png = atlasLocator.resolve(atlas.path);
        if (png == null) {
            return null;
        }
        if (!assetManager.contains(png.path())) {
            assetManager.load(png.path(), Texture.class);
        }
        assetManager.finishLoadingAsset(png.path());
        Texture texture = assetManager.get(png.path(), Texture.class);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        textureByAtlasId.put(atlasId, texture);
        return texture;
    }
    /** Frees all memory associated with this TextureBank. Call this when shutting down the game state. */
    @Override
    public void dispose() {
        assetManager.dispose();
        textureByAtlasId.clear();
        regionByImageId.clear();
        imageIdsByAtlasId.clear();
    }
}