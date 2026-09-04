package pvz.ui;

import com.badlogic.gdx.scenes.scene2d.ui.Image;
import model.ZombieDefinition;

import java.util.Map;

public final class ZombieArtResolver {
    private static final Map<String, String> PACKET_ART = Map.ofEntries(
        Map.entry("basic-zombie", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_MUMMY"),
        Map.entry("conehead-zombie", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_MUMMY_ARMOR1"),
        Map.entry("buckethead-zombie", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_MUMMY_ARMOR2"),
        Map.entry("brickhead-zombie", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_DARK_ARMOR4"),
        Map.entry("knight-zombie", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_DARK_ARMOR3"),
        Map.entry("gargantuar", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_EGYPT_GARGANTUAR"),
        Map.entry("imp", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_EGYPT_IMP"),
        Map.entry("ra-zombie", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_RA"),
        Map.entry("explorer-zombie", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_EXPLORER"),
        Map.entry("tomb-raiser-zombie", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_TOMB_RAISER"),
        Map.entry("dodo-rider-zombie", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_ICEAGE_DODO"),
        Map.entry("hunter-zombie", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_ICEAGE_HUNTER"),
        Map.entry("troglobite", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_ICEAGE_TROGLOBITE"),
        Map.entry("fisherman-zombie", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_BEACH_FISHERMAN"),
        Map.entry("octopus-zombie", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_BEACH_OCTOPUS"),
        Map.entry("snorkel-zombie", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_BEACH_SNORKEL"),
        Map.entry("juggler-zombie", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_DARK_JUGGLER"),
        Map.entry("wizard-zombie", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_DARK_WIZARD"),
        Map.entry("king-zombie", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_DARK_KING"),
        Map.entry("dragon-imp", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_DARK_IMP_DRAGON"),
        Map.entry("all-star-zombie", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_MODERN_ALLSTAR"),
        Map.entry("parasol-zombie", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_LOSTCITY_JANE"),
        Map.entry(
            "turquoise-skull-zombie",
            "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_LOSTCITY_CRYSTALSKULL"
        ),
        Map.entry("prospector-zombie", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_PROSPECTOR"),
        Map.entry("pianist-zombie", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_PIANO"),
        Map.entry("newspaper-zombie", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_MODERN_NEWSPAPER"),
        Map.entry("arcade-zombie", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_EIGHTIES_ARCADE"),
        Map.entry("barrel-roller-zombie", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_BARRELROLLER")
    );

    private ZombieArtResolver() {
    }

    public static Image image(UiTheme theme, ZombieDefinition zombie) {
        if (theme == null || zombie == null) {
            return null;
        }
        String artId = PACKET_ART.getOrDefault(zombie.getKey(),
            "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_MUMMY");
        Image image = theme.image(artId);
        return image != null ? image : theme.image("IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_MUMMY");
    }
}
