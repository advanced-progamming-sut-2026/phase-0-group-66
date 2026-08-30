package pvz.ui;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import model.Plant;
import model.PlantDefinition;
import model.Projectile;
import model.ProjectileType;
import model.SeasonType;
import model.Sun;
import model.Zombie;
import model.ZombieDefinition;
import pvz.assets.AnimationCatalog;
import pvz.assets.PvzAssets;
import pvz.libpvz.pam.PamPlayer;
import pvz.libpvz.pam.ClipRef;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class BattlePamRenderer {
    private static final float WORLD_ASSET_SCALE = 1.5625f;
    private final PvzAssets assets;
    private final PamPlayer player;
    private final AnimationCatalog catalog;
    private final Map<String, AnimationSpec> plantSpecs = new HashMap<>();
    private final Map<String, AnimationSpec> zombieSpecs = new HashMap<>();
    private final Map<String, ClipRef> clipRefs = new HashMap<>();

    public BattlePamRenderer(PvzAssets assets) {
        this.assets = assets;
        this.player = assets.animations();
        this.catalog = assets.animationCatalog();
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
        boolean drawn = draw(batch, spec.path(), clip, time, x, y, scale, false);
        if (drawn && plantFoodActive) {
            AnimationSpec food = effectSpec("PLANTFOOD_FX", "plantfood");
            draw(batch, food.path(), food.primaryClip(), time, x, y, scale * 0.92f, false);
        }
        return drawn;
    }

    public float plantActionDuration(Plant plant) {
        if (plant == null) {
            return 0f;
        }
        AnimationSpec spec = plantSpecs.computeIfAbsent(
            plant.getDefinition().getKey(),
            ignored -> resolvePlant(plant.getDefinition())
        );
        return spec.actionDuration();
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

    public boolean drawZombieDeath(
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
        return draw(batch, spec.path(), spec.deathClip(), time, x, y, scale,
            zombie.isHypnotized());
    }

    public boolean drawProjectile(
        Batch batch,
        Projectile projectile,
        float time,
        float x,
        float y,
        float scale
    ) {
        if (projectile == null) {
            return false;
        }
        String source = normalize(projectile.getSourcePlant());
        String alias = plantAlias(source);
        String typeEffect = switch (projectile.getImpactType() == null
            ? projectile.getType() : projectile.getImpactType()) {
            case FIRE -> "T_FIRE_PEA";
            case ICE -> "T_SNOW_PEA";
            case POISON -> "GOOPEASHOOTER_PROJECTILES";
            case NORMAL -> "T_PEA_PROJECTILE";
        };
        AnimationSpec spec = effectSpec(
            projectileEffectAlias(source),
            "T_" + alias + "_PROJECTILE",
            alias + "_PROJECTILE",
            "T_" + source + "_PROJECTILE",
            source + "_PROJECTILE",
            typeEffect,
            "T_PEA_PROJECTILE"
        );
        return draw(batch, spec.path(), spec.primaryClip(), time, x, y, scale * 0.62f, false);
    }

    public boolean drawSun(Batch batch, Sun sun, float time, float x, float y, float scale) {
        AnimationSpec spec = effectSpec("SUN", "animation");
        return draw(batch, spec.path(), spec.primaryClip(), time, x, y, scale * 0.58f, false);
    }

    public boolean drawWaterTile(Batch batch, float time, float x, float y, float scale) {
        AnimationSpec spec = specFromEntry(catalog.background("WATER_SQUARE"), "Water", false);
        return draw(batch, spec.path(), spec.primaryClip(), time, x, y, scale, false);
    }

    public boolean drawMower(
        Batch batch,
        SeasonType season,
        float time,
        float x,
        float y,
        float scale,
        boolean moving
    ) {
        String name = switch (season) {
            case ANCIENT_EGYPT -> "MOWER_EGYPT";
            case FROSTBITE_CAVES -> "MOWER_ICEAGE";
            case BIG_WAVE_BEACH -> "MOWER_BEACH";
            case DARK_AGES -> "MOWER_DARK";
        };
        AnimationSpec spec = specFromEntry(catalog.mower(name), moving ? "attack" : "idle", false);
        return draw(batch, spec.path(), spec.primaryClip(), time, x, y, scale, false);
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
        try {
            player.loadSync(path);
        } catch (RuntimeException exception) {
            return false;
        }
        Color previous = new Color(batch.getColor());
        if (flipped) {
            batch.setColor(0.72f, 1f, 0.72f, previous.a);
        }
        float worldScale = scale * WORLD_ASSET_SCALE;
        float scaleX = flipped ? -worldScale : worldScale;
        String clipKey = path + "\u0000" + clip;
        ClipRef clipRef = clipRefs.get(clipKey);
        if (clipRef == null) {
            clipRef = player.getClip(path, clip);
            if (clipRef == null) {
                return false;
            }
            clipRefs.put(clipKey, clipRef);
        }
        player.draw(batch, clipRef, time, x, y, scaleX, worldScale, true);
        batch.setColor(previous);
        return true;
    }

    private AnimationSpec resolvePlant(PlantDefinition definition) {
        String normalized = normalize(definition.getKey());
        String alias = plantAlias(normalized);
        AnimationCatalog.Entry catalogEntry = catalog.plant(alias, normalized,
            normalize(definition.getName()));
        if (catalogEntry != null) {
            AnimationSpec spec = specFromEntry(catalogEntry, "idle", true);
            if (spec.valid()) {
                return spec;
            }
        }
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

        String normalized = normalize(definition.getKey());
        String seasonPrefix = switch (season) {
            case ANCIENT_EGYPT -> "ZOMBIE_EGYPT";
            case FROSTBITE_CAVES -> "ZOMBIE_ICEAGE";
            case BIG_WAVE_BEACH -> "ZOMBIE_BEACH";
            case DARK_AGES -> "ZOMBIE_DARK";
        };
        String[] names = switch (normalized) {
            case "GARGANTUAR" -> new String[] {seasonPrefix + "_GARGANTUAR", "GARGANTUAR"};
            case "IMP" -> new String[] {seasonPrefix + "_IMP", "ZOMBIE_EGYPT_IMP"};
            case "BRICKHEADZOMBIE" -> new String[] {seasonPrefix + "_BASIC_BRICK",
                "ZOMBIE_DARK_BASIC_BRICK"};
            default -> new String[] {seasonPrefix + "_BASIC"};
        };
        for (String name : names) {
            AnimationSpec spec = specFromEntry(catalog.zombie(name), "walk", false);
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
            String death = chooseDeathClip(clips);
            float actionDuration = action == null ? 0f : player.clipDurationSeconds(path, action);
            return new AnimationSpec(path, primary, action, plantFood, death, actionDuration);
        } catch (RuntimeException exception) {
            return AnimationSpec.missing();
        }
    }

    private AnimationSpec specFromEntry(
        AnimationCatalog.Entry entry,
        String preferredClip,
        boolean includeActionClip
    ) {
        if (entry == null) {
            return AnimationSpec.missing();
        }
        return specIfPresent(entry.path(), preferredClip, includeActionClip);
    }

    private AnimationSpec effectSpec(String name, String preferredClip, String... fallbacks) {
        String[] candidates = new String[1 + (fallbacks == null ? 0 : fallbacks.length)];
        candidates[0] = name;
        if (fallbacks != null) {
            System.arraycopy(fallbacks, 0, candidates, 1, fallbacks.length);
        }
        return specFromEntry(catalog.effect(candidates), preferredClip, false);
    }

    private String projectileEffectAlias(String source) {
        return switch (source) {
            case "CABBAGEPULT" -> "T_CABBAGEPULT_PROJECTILE";
            case "KERNELPULT" -> "T_KERNALPULT_PROJECTILE";
            case "MELONPULT" -> "T_MELON_PROJECTILE";
            case "ROTORUTABAGA" -> "T_ROTORUTABAGA_PROJECTILE1";
            case "WINTERMELON" -> "T_WINTERMELON_PROJECTILE";
            case "HOMINGTHISTLE", "CATTAIL" -> "T_HOMING_THISTLE_PROJECTILE";
            case "DUSKLOBBER" -> "T_DUSKLOBBER_PROJECTILE";
            case "NIGHTSHADE" -> "T_NIGHTSHADE_PROJECTILE";
            case "REDSTINGER" -> "T_REDSTINGER_PROJECTILE";
            case "GUACODILE" -> "T_GUACODILE_PROJECTILE";
            case "PEPPERPULT" -> "T_PEPPERPULT_PROJECTILE";
            default -> "T_" + source + "_PROJECTILE";
        };
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

    private String chooseDeathClip(List<String> clips) {
        for (String clip : clips) {
            if (clip.equalsIgnoreCase("die") || clip.equalsIgnoreCase("death")) {
                return clip;
            }
        }
        for (String clip : clips) {
            String lower = clip.toLowerCase(Locale.ROOT);
            if (lower.contains("die") || lower.contains("death")) {
                return clip;
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
            case "CATTAIL" -> "HOMINGTHISTLE";
            case "CATTAILMINT" -> "SPEARMINT";
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
        return AnimationCatalog.normalize(value);
    }

    private record AnimationSpec(
        String path,
        String primaryClip,
        String actionClip,
        String plantFoodClip,
        String deathClip,
        float actionDuration
    ) {
        static AnimationSpec missing() {
            return new AnimationSpec(null, null, null, null, null, 0f);
        }

        boolean valid() {
            return path != null && primaryClip != null;
        }
    }
}
