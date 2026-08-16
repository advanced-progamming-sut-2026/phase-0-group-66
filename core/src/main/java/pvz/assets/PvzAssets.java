package pvz.assets;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Disposable;
import pvz.libpvz.pam.PamPlayer;
import pvz.libpvz.textures.TextureBank;
import pvz.skin.PvzSkin;
import pvz.ui.UiTheme;

public final class PvzAssets implements Disposable {
    private final FileHandle root;
    private final TextureBank textureBank;
    private final PamPlayer pamPlayer;
    private final Skin skin;
    private final UiAtlasHelper uiAtlas;
    private final UiTheme uiTheme;

    public PvzAssets() {
        root = PvzAssetRoot.locate();
        textureBank = new TextureBank("768", root);
        pamPlayer = new PamPlayer(textureBank, root);
        skin = PvzSkin.get();
        uiAtlas = new UiAtlasHelper(textureBank);
        uiTheme = new UiTheme(skin, uiAtlas);
    }

    public FileHandle root() {
        return root;
    }

    public TextureBank textures() {
        return textureBank;
    }

    public PamPlayer animations() {
        return pamPlayer;
    }

    public Skin skin() {
        return skin;
    }

    public UiAtlasHelper uiAtlas() {
        return uiAtlas;
    }

    public UiTheme uiTheme() {
        return uiTheme;
    }

    public void update() {
        textureBank.update();
    }

    @Override
    public void dispose() {
        textureBank.dispose();
        skin.dispose();
    }
}
