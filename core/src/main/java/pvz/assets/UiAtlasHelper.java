package pvz.assets;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import pvz.libpvz.textures.TextureBank;

import java.util.HashMap;
import java.util.Map;

public final class UiAtlasHelper {
    private final TextureBank textures;
    private final Map<String, Drawable> drawableCache = new HashMap<>();

    public UiAtlasHelper(TextureBank textures) {
        this.textures = textures;
    }

    public TextureRegion region(String imageId) {
        TextureRegion region = textures.region(imageId);
        if (region != null) {
            region.getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        }
        return region;
    }

    public Drawable drawable(String imageId) {
        if (drawableCache.containsKey(imageId)) {
            return drawableCache.get(imageId);
        }
        TextureRegion region = region(imageId);
        if (region == null) {
            return null;
        }
        Drawable drawable = new TextureRegionDrawable(region);
        drawableCache.put(imageId, drawable);
        return drawable;
    }

    public Image image(String imageId, Scaling scaling) {
        Drawable drawable = drawable(imageId);
        if (drawable == null) {
            return null;
        }
        Image image = new Image(drawable);
        image.setScaling(scaling);
        return image;
    }
}
