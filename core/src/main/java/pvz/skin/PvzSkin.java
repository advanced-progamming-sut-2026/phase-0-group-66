package pvz.skin;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

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
        if(skin == null) skin = new FreeTypeSkin(Gdx.files.classpath("skin/pvz2_skin.json"));
        return skin;
    }
}
