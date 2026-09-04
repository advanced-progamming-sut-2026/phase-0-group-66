package pvz.ui;

import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import model.PlantDefinition;
import model.User;
import model.ZombieDefinition;
import pvz.assets.PvzAssets;
import pvz.ui.PlantPacketCard.State;

public final class CollectionCardFactory {
    private static final float ZOMBIE_NAME_SCALE = 0.48f;
    private static final float ZOMBIE_NAME_HEIGHT = 24f;

    private final PvzAssets assets;
    private final UiTheme theme;
    private final User user;

    public CollectionCardFactory(PvzAssets assets, UiTheme theme, User user) {
        this.assets = assets;
        this.theme = theme;
        this.user = user;
    }

    public PlantPacketCard plantCard(PlantDefinition plant) {
        int level = user.getCollectionBook().getPlantLevel(plant.getName());
        int seeds = user.getInventory().getSeedPacketCount(plant.getName());
        int needed = Math.max(0, level * 10);
        boolean owned = level > 0;
        boolean boosted = user.getInventory().getStoredBoosts().getOrDefault(plant.getName(), 0) > 0;
        State state = new State(
            owned,
            true,
            false,
            boosted,
            level,
            seeds,
            needed,
            isMaxLevel(plant, level)
        );
        return new PlantPacketCard(theme, plant, state, false);
    }

    public Button zombieCard(ZombieDefinition zombie, boolean seen) {
        Button.ButtonStyle style = new Button.ButtonStyle();
        Drawable background = theme.drawable("IMAGE_UI_ALMANAC_ZOMBIE_SEED_PKT");
        if (background == null) {
            background = theme.skin().getDrawable("image_ui_dialog_asset_inner_bkgd_10");
        }
        style.up = background;
        style.down = background;
        Button card = new Button(style);
        Stack stack = new Stack();

        if (seen) {
            Image art = ZombieArtResolver.image(theme, zombie);
            if (art != null) {
                Table artLayer = new Table();
                artLayer.add(art).width(112f).height(96f).padTop(2f);
                stack.add(artLayer);
            }
        }

        Table labelLayer = new Table();
        labelLayer.bottom();
        Label name = theme.settingsLabel(seen ? zombie.getDisplayName() : "???");
        name.setAlignment(Align.center);
        name.setWrap(false);
        name.setEllipsis(true);
        name.setFontScale(ZOMBIE_NAME_SCALE);
        labelLayer.add(name).growX().minWidth(0f).height(ZOMBIE_NAME_HEIGHT).pad(3f);
        stack.add(labelLayer);

        if (!seen) {
            Table hidden = new Table();
            Label question = theme.title("?");
            question.setFontScale(1.1f);
            hidden.add(question);
            stack.add(hidden);
        }
        card.add(stack).grow();
        return card;
    }

    private boolean isMaxLevel(PlantDefinition plant, int level) {
        int maximum = Math.max(1, plant.getLevelUpgrades().size() + 1);
        return level > 0 && level >= maximum;
    }
}
