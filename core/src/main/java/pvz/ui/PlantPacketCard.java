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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class PlantPacketCard extends Button {
    private static final String READY = "IMAGE_UI_PACKETS_READY";
    private static final String SELECTED = "IMAGE_UI_PACKETS_SELECTED";
    private static final String BOOSTED = "IMAGE_UI_PACKETS_BOOST";
    private static final String EMPTY = "IMAGE_UI_PACKETS_EMPTY_PACKET";
    private static final String LOCK = "IMAGE_UI_PACKETS_LOCKED";
    private static final String SELECT_CORNERS = "IMAGE_UI_PACKETS_SELECT";
    private static final String BOOST_ICON = "IMAGE_UI_ALMANAC_ALMANAC_BOOST";
    private static final String SUN_ICON = "IMAGE_UI_HUD_INGAME_SUN";

    private static final Map<String, String> PACKET_ALIASES = createPacketAliases();

    private final PlantDefinition definition;

    public PlantPacketCard(UiTheme theme, PlantDefinition definition, State state, boolean compact) {
        super(new ButtonStyle());
        this.definition = definition;
        add(buildCard(theme, definition, state, compact)).grow();
    }

    public PlantDefinition definition() {
        return definition;
    }

    private Table buildCard(UiTheme theme, PlantDefinition plant, State state, boolean compact) {
        Table card = new Table();
        card.top();
        Stack packet = buildPacket(theme, plant, state);
        card.add(packet).growX().height(compact ? 76f : 94f);
        if (!compact) {
            card.row();
            Label name = theme.fieldLabel(plant.getName());
            name.setAlignment(Align.center);
            name.setWrap(false);
            name.setEllipsis(true);
            name.setFontScale(0.72f);
            card.add(name).growX().height(25f).padTop(2f);
            card.row();
            Label seeds = theme.settingsLabel(seedText(state));
            seeds.setAlignment(Align.center);
            seeds.setFontScale(0.66f);
            card.add(seeds).growX().height(20f);
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

        String plantImageId = packetImageId(theme, plant);
        Image plantImage = plantImageId == null ? null : theme.image(plantImageId);
        if (plantImage != null) {
            plantImage.setScaling(Scaling.fit);
            if (!state.owned() || !state.available()) {
                plantImage.setColor(0.40f, 0.40f, 0.40f, 0.72f);
            }
            Table plantLayer = new Table();
            plantLayer.add(plantImage).size(82f, 70f).center();
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
                lockLayer.add(lock).size(34f);
            }
            packet.add(lockLayer);
        }
        return packet;
    }

    private Table buildTopLayer(UiTheme theme, State state) {
        Table top = new Table();
        top.top();
        Label level = theme.settingsLabel("Lv " + state.level());
        level.setFontScale(0.62f);
        top.add(level).left().pad(4f, 6f, 0f, 0f);
        top.add().expandX();
        if (state.boosted()) {
            Image boost = theme.image(BOOST_ICON);
            if (boost != null) {
                top.add(boost).size(24f).pad(3f, 0f, 0f, 5f);
            }
        }
        return top;
    }

    private Table buildBottomLayer(UiTheme theme, PlantDefinition plant, State state) {
        Table bottom = new Table();
        bottom.bottom().left();
        Image sun = theme.image(SUN_ICON);
        if (sun != null) {
            bottom.add(sun).size(21f).pad(0f, 4f, 3f, 5f);
        }
        Label cost = theme.settingsLabel(Integer.toString(plant.getCost()));
        cost.setFontScale(0.70f);
        bottom.add(cost).padBottom(4f);
        bottom.add().expandX();
        if (state.owned() && !state.available() && !state.selected()) {
            Label blocked = theme.settingsLabel("BLOCKED");
            blocked.setColor(Color.FIREBRICK);
            blocked.setFontScale(0.54f);
            bottom.add(blocked).padRight(5f).padBottom(5f);
        }
        return bottom;
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

    private String packetImageId(UiTheme theme, PlantDefinition plant) {
        String normalized = normalize(plant.getKey());
        String alias = PACKET_ALIASES.getOrDefault(normalized, normalized);
        String candidate = "IMAGE_UI_PACKETS_" + alias;
        if (theme.drawable(candidate) != null) {
            return candidate;
        }
        candidate = "IMAGE_UI_PACKETS_" + normalize(plant.getName());
        if (theme.drawable(candidate) != null) {
            return candidate;
        }
        return null;
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
    }

    private static Map<String, String> createPacketAliases() {
        Map<String, String> aliases = new HashMap<>();
        aliases.put("GOOPEASHOOTER", "POISONPEASHOOTER");
        aliases.put("MEGAGATLINGPEA", "MEGAGATLING");
        aliases.put("CHERRYBOMB", "CHERRY_BOMB");
        aliases.put("ICEBERGLETTUCE", "ICEBURG");
        aliases.put("PIERCEMINT", "SPEARMINT");
        aliases.put("CATTAILMINT", "MINTFAM_SHARP");
        return aliases;
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
