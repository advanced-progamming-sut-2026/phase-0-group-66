package pvz.ui;

import com.badlogic.gdx.scenes.scene2d.ui.Image;
import model.PlantDefinition;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class PlantArtResolver {
    private static final Map<String, String> PACKET_ALIASES = createPacketAliases();

    private PlantArtResolver() {
    }

    public static Image packetImage(UiTheme theme, PlantDefinition plant) {
        String id = packetImageId(theme, plant);
        return id == null ? null : theme.image(id);
    }

    public static String packetImageId(UiTheme theme, PlantDefinition plant) {
        if (plant == null) {
            return null;
        }
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
        aliases.put("PRIMALSUNFLOWER", "PRIMALSUNFLOWER");
        return aliases;
    }
}
