package pvz.ui;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import model.ZombieDefinition;
import pvz.assets.PvzAssets;
import pvz.libpvz.pam.PamPlayer;

import java.util.List;
import java.util.Locale;

public final class ZombieAnimationActor extends Actor {
    private final PamPlayer player;
    private final String pamPath;
    private final String clip;
    private float stateTime;

    public ZombieAnimationActor(PvzAssets assets, ZombieDefinition zombie) {
        player = assets.animations();
        pamPath = resolvePam(assets, zombie);
        clip = chooseClip(player, pamPath);
    }

    public boolean hasAnimation() {
        return pamPath != null && clip != null;
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        stateTime += Math.max(0f, delta);
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        if (!hasAnimation()) {
            return;
        }
        Color before = new Color(batch.getColor());
        Color tint = getColor();
        batch.setColor(tint.r, tint.g, tint.b, tint.a * parentAlpha);
        float scale = Math.min(getWidth() / 260f, getHeight() / 260f);
        float x = getX() + getWidth() * 0.5f;
        float y = getY() + getHeight() * 0.22f;
        player.draw(batch, pamPath, clip, stateTime, x, y, scale, scale, true);
        batch.setColor(before);
    }

    private static String resolvePam(PvzAssets assets, ZombieDefinition zombie) {
        String alias = zombieAlias(zombie.getKey());
        String[] roots = {
            "768/INITIAL/ZOMBIE/",
            "768/FULL/ZOMBIE/"
        };
        for (String root : roots) {
            String candidate = root + alias + "/" + alias + ".PAM";
            FileHandle file = assets.root().child("IMAGES").child(candidate);
            if (file.exists()) {
                return candidate;
            }
        }
        return resolveBasicFallback(assets);
    }

    private static String resolveBasicFallback(PvzAssets assets) {
        String path = "768/INITIAL/ZOMBIE/ZOMBIE_EGYPT_BASIC/ZOMBIE_EGYPT_BASIC.PAM";
        return assets.root().child("IMAGES").child(path).exists() ? path : null;
    }

    private static String chooseClip(PamPlayer player, String pam) {
        if (pam == null) {
            return null;
        }
        try {
            player.loadSync(pam);
            List<String> clips = player.clips(pam);
            if (clips == null || clips.isEmpty()) {
                return null;
            }
            for (String preferred : List.of("idle", "walk")) {
                for (String name : clips) {
                    if (name.equalsIgnoreCase(preferred)) {
                        return name;
                    }
                }
            }
            for (String name : clips) {
                String lower = name.toLowerCase(Locale.ROOT);
                if (lower.contains("idle") || lower.contains("walk")) {
                    return name;
                }
            }
            return clips.get(0);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static String zombieAlias(String key) {
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
            case "JUGGLERZOMBIE" -> "ZOMBIE_DARK_JESTER";
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
            case "GARGANTUAR" -> "EGYPT_GARGANTUAR";
            case "IMP" -> "ZOMBIE_EGYPT_IMP";
            default -> "ZOMBIE_EGYPT_BASIC";
        };
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
    }
}
