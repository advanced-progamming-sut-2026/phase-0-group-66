package pvz.skin;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

public class BorderedTable extends Table{
    protected final Skin skin;
    private final Drawable border;
    private final Drawable background;

    public BorderedTable() {
        super();
        this.skin = PvzSkin.get();
        pad(50);

        border = skin.getDrawable("image_ui_dialog_asset_dialogborder_10");
        background = skin.getDrawable("image_ui_dialog_asset_inner_bkgd_10");
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        Color color = getColor();
		batch.setColor(color.r, color.g, color.b, color.a * parentAlpha);
        background.draw(batch, getX() + 17, getY() + 16, getWidth() - 34, getHeight() - 32);
        super.draw(batch, parentAlpha);
		batch.setColor(color.r, color.g, color.b, color.a * parentAlpha);
        border.draw(batch, getX()-5, getY()-9, getWidth() + 10, getHeight()+10);
    }
}
