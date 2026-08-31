package pvz.skin;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Window;

/**
 * Loads the bundled PvZ2 skin (a Skin Composer export) from the classpath. The skin json, its atlas, and the
 * FreeType fonts it references all ship as resources under {@code skin/} in this module's jar, so callers need
 * no filesystem paths — {@link #get()} works wherever the jar is on the classpath.
 */
public final class PvzSkin {
    private PvzSkin() {
    }
    private static Skin skin;

    public static Skin get() {
        if (skin == null) {
            skin = new FreeTypeSkin(Gdx.files.classpath("skin/pvz2_skin.json"));
            registerDefaultWindowStyle(skin);
        }
        return skin;
    }

    private static void registerDefaultWindowStyle(Skin skin) {
        if (skin.has("default", Window.WindowStyle.class)) {
            return;
        }
        Label.LabelStyle labelStyle = skin.get("default", Label.LabelStyle.class);
        Window.WindowStyle style = new Window.WindowStyle();
        style.background = skin.getDrawable("image_ui_dialog_asset_inner_bkgd_10");
        style.titleFont = labelStyle.font;
        style.titleFontColor = labelStyle.fontColor == null ? Color.WHITE : labelStyle.fontColor;
        skin.add("default", style);
    }
}
