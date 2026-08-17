package pvz.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import model.PlantDefinition;

public final class PlantPacketCard extends Button {
    public static final float COLLECTION_WIDTH = 136f;
    public static final float COLLECTION_HEIGHT = 130f;

    private static final String READY = "IMAGE_UI_PACKETS_READY";
    private static final String SELECTED = "IMAGE_UI_PACKETS_SELECTED";
    private static final String BOOSTED = "IMAGE_UI_PACKETS_BOOST";
    private static final String EMPTY = "IMAGE_UI_PACKETS_EMPTY_PACKET";
    private static final String LOCK = "IMAGE_UI_PACKETS_LOCKED";
    private static final String SELECT_CORNERS = "IMAGE_UI_PACKETS_SELECT";
    private static final String BOOST_ICON = "IMAGE_UI_ALMANAC_ALMANAC_BOOST";
    private static final String SUN_ICON = "IMAGE_UI_HUD_INGAME_SUN";

    private final PlantDefinition definition;

    public PlantPacketCard(UiTheme theme, PlantDefinition definition, State state, boolean compact) {
        super(new ButtonStyle());
        this.definition = definition;
        Table content = buildCard(theme, definition, state, compact);
        if (compact) {
            add(content).grow();
        } else {
            add(content).width(COLLECTION_WIDTH).height(COLLECTION_HEIGHT);
        }
    }

    public PlantDefinition definition() {
        return definition;
    }

    private Table buildCard(UiTheme theme, PlantDefinition plant, State state, boolean compact) {
        Table card = new Table();
        card.top();
        card.defaults().minWidth(0f);
        float width = compact ? 104f : COLLECTION_WIDTH;
        float packetHeight = compact ? 76f : 84f;

        card.add(buildPacket(theme, plant, state))
            .width(width)
            .height(packetHeight)
            .minWidth(0f);

        if (!compact) {
            card.row();
            Label name = theme.fieldLabel(plant.getName());
            name.setAlignment(Align.center);
            name.setWrap(false);
            name.setEllipsis(true);
            name.setFontScale(nameScale(plant.getName()));
            card.add(name)
                .width(width)
                .height(24f)
                .minWidth(0f)
                .padTop(1f);

            card.row();
            Label stateLabel = theme.settingsLabel(seedText(state));
            stateLabel.setAlignment(Align.center);
            stateLabel.setWrap(false);
            stateLabel.setEllipsis(true);
            stateLabel.setFontScale(0.57f);
            card.add(stateLabel)
                .width(width)
                .height(20f)
                .minWidth(0f);
        }
        return card;
    }

    private Stack buildPacket(UiTheme theme, PlantDefinition plant, State state) {
        Stack packet = new Stack();
        Image background = theme.image(backgroundFor(state));
        if (background != null) {
            background.setScaling(Scaling.stretch);
            packet.add(background);
        }

        Image plantImage = PlantArtResolver.packetImage(theme, plant);
        if (plantImage != null) {
            plantImage.setScaling(Scaling.fit);
            if (!state.owned() || !state.available()) {
                plantImage.setColor(0.40f, 0.40f, 0.40f, 0.72f);
            }
            Table plantLayer = new Table();
            plantLayer.add(plantImage).size(78f, 66f).center();
            packet.add(plantLayer);
        }

        packet.add(buildTopLayer(theme, state));
        packet.add(buildBottomLayer(theme, plant, state));

        if (state.selected()) {
            Image selected = theme.image(SELECT_CORNERS);
            if (selected != null) {
                selected.setScaling(Scaling.stretch);
                packet.add(selected);
            }
        }
        if (!state.owned()) {
            Table lockLayer = new Table();
            Image lock = theme.image(LOCK);
            if (lock != null) {
                lockLayer.add(lock).size(32f);
            }
            packet.add(lockLayer);
        }
        return packet;
    }

    private Table buildTopLayer(UiTheme theme, State state) {
        Table top = new Table();
        top.top();
        Label level = theme.settingsLabel("Lv " + state.level());
        level.setFontScale(0.56f);
        top.add(level).left().pad(3f, 5f, 0f, 0f);
        top.add().expandX();
        if (state.boosted()) {
            Image boost = theme.image(BOOST_ICON);
            if (boost != null) {
                top.add(boost).size(21f).pad(3f, 0f, 0f, 4f);
            }
        }
        return top;
    }

    private Table buildBottomLayer(UiTheme theme, PlantDefinition plant, State state) {
        Table bottom = new Table();
        bottom.bottom().left();
        Image sun = theme.image(SUN_ICON);
        if (sun != null) {
            bottom.add(sun).size(18f).pad(0f, 3f, 3f, 4f);
        }
        Label cost = theme.settingsLabel(Integer.toString(plant.getCost()));
        cost.setFontScale(0.62f);
        bottom.add(cost).padBottom(3f);
        bottom.add().expandX();
        if (state.owned() && !state.available() && !state.selected()) {
            Label blocked = theme.settingsLabel("BLOCKED");
            blocked.setColor(Color.FIREBRICK);
            blocked.setFontScale(0.46f);
            bottom.add(blocked).padRight(4f).padBottom(4f);
        }
        return bottom;
    }

    private float nameScale(String name) {
        int length = name == null ? 0 : name.length();
        if (length >= 19) {
            return 0.48f;
        }
        if (length >= 16) {
            return 0.53f;
        }
        if (length >= 13) {
            return 0.58f;
        }
        return 0.64f;
    }

    private String seedText(State state) {
        if (!state.owned()) {
            return "LOCKED";
        }
        if (state.maxLevel()) {
            return "MAX LEVEL";
        }
        return "Seeds " + state.seedPackets() + " / " + state.seedPacketsNeeded();
    }

    private String backgroundFor(State state) {
        if (!state.owned()) {
            return EMPTY;
        }
        if (state.boosted()) {
            return BOOSTED;
        }
        if (state.selected()) {
            return SELECTED;
        }
        return READY;
    }

    public record State(
        boolean owned,
        boolean available,
        boolean selected,
        boolean boosted,
        int level,
        int seedPackets,
        int seedPacketsNeeded,
        boolean maxLevel
    ) {
    }
}
