package pvz.ui;

import com.badlogic.gdx.scenes.scene2d.ui.Image;
import model.PlantDefinition;
import model.PlantFamily;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class PlantArtResolver {
    private static final Map<String, String> PACKET_ALIASES = createPacketAliases();
    private static final Map<PlantFamily, String> FAMILY_ICONS = createFamilyIcons();

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
        return theme.drawable("IMAGE_UI_PACKETS_PEASHOOTER") != null
            ? "IMAGE_UI_PACKETS_PEASHOOTER" : null;
    }

    public static Image familyIcon(UiTheme theme, PlantDefinition plant) {
        String id = familyIconId(theme, plant);
        return id == null ? null : theme.image(id);
    }

    public static String familyIconId(UiTheme theme, PlantDefinition plant) {
        if (plant == null) {
            return null;
        }
        String family = specializedFamilyIcon(plant);
        String candidate = "IMAGE_UI_PACKETS_MINTFAM_" + family;
        if (theme.drawable(candidate) != null) {
            return candidate;
        }
        return theme.drawable("IMAGE_UI_PACKETS_MINTFAM_SUN") != null
            ? "IMAGE_UI_PACKETS_MINTFAM_SUN" : null;
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
        aliases.put("CATTAIL", "HOMINGTHISTLE");
        aliases.put("CATTAILMINT", "MINTFAM_SHARP");
        aliases.put("PRIMALSUNFLOWER", "PRIMALSUNFLOWER");
        return aliases;
    }

    private static Map<PlantFamily, String> createFamilyIcons() {
        Map<PlantFamily, String> icons = new EnumMap<>(PlantFamily.class);
        icons.put(PlantFamily.SUN_PRODUCER, "SUN");
        icons.put(PlantFamily.SHOOTER, "PEASHOOTER");
        icons.put(PlantFamily.HOMING, "MAGIC");
        icons.put(PlantFamily.STRIKE_THROUGH, "SHARP");
        icons.put(PlantFamily.LOBBER, "LOBBER");
        icons.put(PlantFamily.EXPLOSIVE, "EXPLOSIVE");
        icons.put(PlantFamily.MELEE, "MELEE");
        icons.put(PlantFamily.WALL_NUT, "DEFENSE");
        icons.put(PlantFamily.MODIFIER, "TRAP");
        return icons;
    }

    private static String specializedFamilyIcon(PlantDefinition plant) {
        if (plant.hasTag("FIRE")) {
            return "FIRE";
        }
        if (plant.hasTag("ICE")) {
            return "COLD";
        }
        if (plant.hasTag("POISON")) {
            return "POISON";
        }
        if (plant.hasTag("MAGIC")) {
            return "MAGIC";
        }
        return FAMILY_ICONS.getOrDefault(plant.getFamily(), "SUN");
    }
}
