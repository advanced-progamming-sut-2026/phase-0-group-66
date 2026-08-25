package pvz.skin;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.Hinting;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

/**
 * A {@link Skin} that can load the FreeType fonts a Skin Composer export embeds directly in the skin JSON. The
 * plain {@code Skin} JSON loader cannot instantiate {@link FreeTypeFontGenerator} (it has no no-arg constructor),
 * so it fails on a {@code "com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator"} block. Here we register
 * a serializer that reads such a block, generates the {@link BitmapFont} from the sibling {@code .ttf}, and adds
 * it to the skin under the block's name — the standard Skin Composer recipe.
 */
public final class FreeTypeSkin extends Skin {

    public FreeTypeSkin(FileHandle skinFile) {
        super(skinFile);
    }

    @Override
    protected Json getJsonLoader(final FileHandle skinFile) {
        Json json = super.getJsonLoader(skinFile);
        final Skin skin = this;
        json.setSerializer(FreeTypeFontGenerator.class, new Json.ReadOnlySerializer<FreeTypeFontGenerator>() {
            @Override
            @SuppressWarnings("rawtypes")
            public FreeTypeFontGenerator read(Json json, JsonValue data, Class type) {
                // Pull the fields a FreeTypeFontParameter can't deserialise on its own, then read the rest.
                String path = json.readValue("font", String.class, data);
                data.remove("font");
                Hinting hinting = Hinting.valueOf(json.readValue("hinting", String.class, "AutoMedium", data));
                data.remove("hinting");
                TextureFilter minFilter = TextureFilter.valueOf(json.readValue("minFilter", String.class, "Nearest", data));
                data.remove("minFilter");
                TextureFilter magFilter = TextureFilter.valueOf(json.readValue("magFilter", String.class, "Nearest", data));
                data.remove("magFilter");

                FreeTypeFontParameter parameter = json.readValue(FreeTypeFontParameter.class, data);
                parameter.hinting = hinting;
                parameter.minFilter = minFilter;
                parameter.magFilter = magFilter;

                FreeTypeFontGenerator generator = new FreeTypeFontGenerator(skinFile.parent().child(path));
                BitmapFont font = generator.generateFont(parameter);
                skin.add(data.name, font);
                if (parameter.incremental) {
                    generator.dispose();
                    return null;
                }
                return generator;
            }
        });
        return json;
    }
}
