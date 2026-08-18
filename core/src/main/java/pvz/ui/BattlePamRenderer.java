package pvz.ui;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import model.Plant;
import model.PlantDefinition;
import model.SeasonType;
import model.Zombie;
import model.ZombieDefinition;
import pvz.assets.PvzAssets;
import pvz.libpvz.pam.PamPlayer;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class BattlePamRenderer {
    private static final float WORLD_ASSET_SCALE = 1.5625f;
    private final PvzAssets assets;
    private final PamPlayer player;
    private final Map<String, AnimationSpec> plantSpecs = new HashMap<>();
    private final Map<String, AnimationSpec> zombieSpecs = new HashMap<>();

    public BattlePamRenderer(PvzAssets assets) {
        this.assets = assets;
        this.player = assets.animations();
    }

    public boolean drawPlant(
        Batch batch,
        Plant plant,
        float time,
        float x,
        float y,
        float scale,
        boolean attacking,
        boolean plantFoodActive
    ) {
        if (plant == null) {
            return false;
        }
        AnimationSpec spec = plantSpecs.computeIfAbsent(
            plant.getDefinition().getKey(),
            ignored -> resolvePlant(plant.getDefinition())
        );
        String clip;
        if (plantFoodActive && spec.plantFoodClip() != null) {
            clip = spec.plantFoodClip();
        } else if (attacking && spec.actionClip() != null) {
            clip = spec.actionClip();
        } else {
            clip = spec.primaryClip();
        }
        return draw(batch, spec.path(), clip, time, x, y, scale, false);
    }

    public boolean drawZombie(
        Batch batch,
        Zombie zombie,
        SeasonType season,
        float time,
        float x,
        float y,
        float scale
    ) {
        if (zombie == null) {
            return false;
        }
        String key = season.name() + ":" + zombie.getDefinition().getKey();
        AnimationSpec spec = zombieSpecs.computeIfAbsent(
            key,
            ignored -> resolveZombie(zombie.getDefinition(), season)
        );
        return draw(
            batch,
            spec.path(),
            spec.primaryClip(),
            time,
            x,
            y,
            scale,
            zombie.isHypnotized()
        );
    }

    private boolean draw(
        Batch batch,
        String path,
        String clip,
        float time,
        float x,
        float y,
        float scale,
        boolean flipped
    ) {
        if (path == null || clip == null) {
            return false;
        }
        Color previous = new Color(batch.getColor());
        if (flipped) {
            batch.setColor(0.72f, 1f, 0.72f, previous.a);
        }
        float worldScale = scale * WORLD_ASSET_SCALE;
        float scaleX = flipped ? -worldScale : worldScale;
        player.draw(batch, path, clip, time, x, y, scaleX, worldScale, true);
        batch.setColor(previous);
        return true;
    }

    private AnimationSpec resolvePlant(PlantDefinition definition) {
        String normalized = normalize(definition.getKey());
        String alias = plantAlias(normalized);
        String[] roots = {
            "768/INITIAL/PLANT/",
            "768/FULL/PLANT/",
            "768/INITIAL/EMPOWERMINTS/PLANT/"
        };
        for (String root : roots) {
            String path = root + alias + "/" + alias + ".PAM";
            AnimationSpec spec = specIfPresent(path, "idle", true);
            if (spec.valid()) {
                return spec;
            }
        }
        return AnimationSpec.missing();
    }

    private AnimationSpec resolveZombie(ZombieDefinition definition, SeasonType season) {
        String special = zombieSpecialAlias(definition.getKey());
        if (special != null) {
            AnimationSpec spec = findZombiePam(special, "walk");
            if (spec.valid()) {
                return spec;
            }
        }

        String basic = switch (season) {
            case ANCIENT_EGYPT -> "ZOMBIE_EGYPT_BASIC";
            case FROSTBITE_CAVES -> "ZOMBIE_ICEAGE_BASIC";
            case BIG_WAVE_BEACH -> "ZOMBIE_BEACH_BASIC";
            case DARK_AGES -> "ZOMBIE_DARK_BASIC";
        };
        return findZombiePam(basic, "walk");
    }

    private AnimationSpec findZombiePam(String name, String preferredClip) {
        String[] roots = {
            "768/INITIAL/ZOMBIE/",
            "768/FULL/ZOMBIE/"
        };
        for (String root : roots) {
            String path = root + name + "/" + name + ".PAM";
            AnimationSpec spec = specIfPresent(path, preferredClip, false);
            if (spec.valid()) {
                return spec;
            }
        }
        return AnimationSpec.missing();
    }

    private AnimationSpec specIfPresent(
        String path,
        String preferredClip,
        boolean includeActionClip
    ) {
        FileHandle file = assets.root().child("IMAGES").child(path);
        if (!file.exists()) {
            return AnimationSpec.missing();
        }
        try {
            player.loadSync(path);
            List<String> clips = player.clips(path);
            if (clips == null || clips.isEmpty()) {
                return AnimationSpec.missing();
            }
            String primary = chooseClip(clips, preferredClip);
            String action = includeActionClip ? chooseActionClip(clips) : null;
            String plantFood = includeActionClip ? choosePlantFoodClip(clips, action) : null;
            return new AnimationSpec(path, primary, action, plantFood);
        } catch (RuntimeException exception) {
            return AnimationSpec.missing();
        }
    }

    private String choosePlantFoodClip(List<String> clips, String actionFallback) {
        for (String clip : clips) {
            if (clip.equalsIgnoreCase("plantfood")) {
                return clip;
            }
        }
        for (String clip : clips) {
            String lower = clip.toLowerCase(Locale.ROOT);
            if (lower.contains("plantfood") || lower.contains("plant_food")) {
                return clip;
            }
        }
        for (String clip : clips) {
            if (clip.equalsIgnoreCase("use_action")) {
                return clip;
            }
        }
        return actionFallback;
    }

    private String chooseActionClip(List<String> clips) {
        String[] preferred = {"attack", "shoot", "fire", "use_action"};
        for (String name : preferred) {
            for (String clip : clips) {
                if (clip.equalsIgnoreCase(name)) {
                    return clip;
                }
            }
        }
        for (String name : preferred) {
            for (String clip : clips) {
                if (clip.toLowerCase(Locale.ROOT).contains(name)) {
                    return clip;
                }
            }
        }
        return null;
    }

    private String chooseClip(List<String> clips, String preferred) {
        for (String clip : clips) {
            if (clip.equalsIgnoreCase(preferred)) {
                return clip;
            }
        }
        for (String clip : clips) {
            if (clip.toLowerCase(Locale.ROOT).contains(preferred.toLowerCase(Locale.ROOT))) {
                return clip;
            }
        }
        for (String clip : clips) {
            String lower = clip.toLowerCase(Locale.ROOT);
            if (lower.contains("walk") || lower.contains("idle") || lower.contains("eat")) {
                return clip;
            }
        }
        return clips.get(0);
    }

    private String plantAlias(String normalized) {
        return switch (normalized) {
            case "TWINSUNFLOWER" -> "SUNFLOWER_TWIN";
            case "ROTOBAGA" -> "ROTORUTABAGA";
            case "MEGAGATLINGPEA" -> "MEGAGATLING";
            case "KERNELPULT" -> "KERNALPULT";
            case "PRIMALPOTATOMINE" -> "PRIMAL_POTATOMINE";
            case "ICEBERGLETTUCE" -> "ICEBURG";
            case "PHATBEET" -> "PHATBEETS";
            case "PIERCEMINT" -> "SPEARMINT";
            default -> normalized;
        };
    }

    private String zombieSpecialAlias(String key) {
        String normalized = normalize(key);
        return switch (normalized) {
            case "RAZOMBIE" -> "ZOMBIE_EGYPT_RA";
            case "EXPLORERZOMBIE" -> "ZOMBIE_EXPLORER";
            case "TOMBRAISERZOMBIE" -> "ZOMBIE_EGYPT_TOMBRAISER";
            case "DODORIDERZOMBIE" -> "ZOMBIE_ICEAGE_DODORIDER";
            case "HUNTERZOMBIE" -> "ZOMBIE_ICEAGE_HUNTER";
            case "TROGLOBITE" -> "ZOMBIE_ICEAGE_TROGLOBITE";
            case "FISHERMANZOMBIE" -> "ZOMBIE_BEACH_FISHERMAN";
            case "OCTOPUSZOMBIE" -> "ZOMBIE_BEACH_OCTOPUS";
            case "SNORKELZOMBIE" -> "ZOMBIE_BEACH_SNORKELER";
            case "WIZARDZOMBIE" -> "ZOMBIE_DARK_WIZARD";
            case "KINGZOMBIE" -> "ZOMBIE_DARK_KING";
            case "DRAGONIMP" -> "ZOMBIE_DARK_IMP_DRAGON";
            case "ALLSTARZOMBIE" -> "ZOMBIE_MODERN_ALLSTAR";
            case "PARASOLZOMBIE" -> "ZOMBIE_LOSTCITY_JANE";
            case "TURQUOISESKULLZOMBIE" -> "ZOMBIE_LOSTCITY_CRYSTALSKULL";
            case "PROSPECTORZOMBIE" -> "ZOMBIE_PROSPECTOR";
            case "PIANISTZOMBIE" -> "ZOMBIE_PIANO";
            case "NEWSPAPERZOMBIE" -> "ZOMBIE_MODERN_NEWSPAPER";
            case "ARCADEZOMBIE" -> "ZOMBIE_80S_ARCADE";
            case "BARRELROLLERZOMBIE" -> "ZOMBIE_PIRATE_BARREL_PUSHER";
            default -> null;
        };
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
    }

    private record AnimationSpec(
        String path,
        String primaryClip,
        String actionClip,
        String plantFoodClip
    ) {
        static AnimationSpec missing() {
            return new AnimationSpec(null, null, null, null);
        }

        boolean valid() {
            return path != null && primaryClip != null;
        }
    }
}
