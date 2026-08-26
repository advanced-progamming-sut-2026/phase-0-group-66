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
import java.util.Map;

public final class ZombieAnimationActor extends Actor {
    private static final Map<String, String> PAM_BY_KEY = Map.ofEntries(
        Map.entry("basic-zombie", "768/INITIAL/ZOMBIE/ZOMBIE_EGYPT_BASIC/ZOMBIE_EGYPT_BASIC.PAM"),
        Map.entry("gargantuar", "768/INITIAL/ZOMBIE/EGYPT_GARGANTUAR/EGYPT_GARGANTUAR.PAM"),
        Map.entry("imp", "768/INITIAL/ZOMBIE/ZOMBIE_EGYPT_IMP/ZOMBIE_EGYPT_IMP.PAM"),
        Map.entry("ra-zombie", "768/INITIAL/ZOMBIE/ZOMBIE_EGYPT_RA/ZOMBIE_EGYPT_RA.PAM"),
        Map.entry("tomb-raiser-zombie", "768/INITIAL/ZOMBIE/ZOMBIE_EGYPT_TOMBRAISER/ZOMBIE_EGYPT_TOMBRAISER.PAM"),
        Map.entry("dodo-rider-zombie", "768/FULL/ZOMBIE/ZOMBIE_ICEAGE_DODORIDER/ZOMBIE_ICEAGE_DODORIDER.PAM"),
        Map.entry("hunter-zombie", "768/FULL/ZOMBIE/ZOMBIE_ICEAGE_HUNTER/ZOMBIE_ICEAGE_HUNTER.PAM"),
        Map.entry("troglobite", "768/FULL/ZOMBIE/ZOMBIE_ICEAGE_TROGLOBITE/ZOMBIE_ICEAGE_TROGLOBITE.PAM"),
        Map.entry("fisherman-zombie", "768/FULL/ZOMBIE/ZOMBIE_BEACH_FISHERMAN/ZOMBIE_BEACH_FISHERMAN.PAM"),
        Map.entry("octopus-zombie", "768/FULL/ZOMBIE/ZOMBIE_BEACH_OCTOPUS/ZOMBIE_BEACH_OCTOPUS.PAM"),
        Map.entry("snorkel-zombie", "768/FULL/ZOMBIE/ZOMBIE_BEACH_SNORKELER/ZOMBIE_BEACH_SNORKELER.PAM"),
        Map.entry("juggler-zombie", "768/FULL/ZOMBIE/ZOMBIE_DARK_JESTER/ZOMBIE_DARK_JESTER.PAM"),
        Map.entry("wizard-zombie", "768/FULL/ZOMBIE/ZOMBIE_DARK_WIZARD/ZOMBIE_DARK_WIZARD.PAM"),
        Map.entry("king-zombie", "768/FULL/ZOMBIE/ZOMBIE_DARK_KING/ZOMBIE_DARK_KING.PAM"),
        Map.entry("dragon-imp", "768/FULL/ZOMBIE/ZOMBIE_DARK_IMP_DRAGON/ZOMBIE_DARK_IMP_DRAGON.PAM"),
        Map.entry("all-star-zombie", "768/FULL/ZOMBIE/ZOMBIE_MODERN_ALLSTAR/ZOMBIE_MODERN_ALLSTAR.PAM"),
        Map.entry("parasol-zombie", "768/FULL/ZOMBIE/ZOMBIE_LOSTCITY_JANE/ZOMBIE_LOSTCITY_JANE.PAM"),
        Map.entry(
            "turquoise-skull-zombie",
            "768/FULL/ZOMBIE/ZOMBIE_LOSTCITY_CRYSTALSKULL/ZOMBIE_LOSTCITY_CRYSTALSKULL.PAM"
        ),
        Map.entry("prospector-zombie", "768/FULL/ZOMBIE/ZOMBIE_PROSPECTOR/ZOMBIE_PROSPECTOR.PAM"),
        Map.entry("pianist-zombie", "768/FULL/ZOMBIE/ZOMBIE_PIANO/ZOMBIE_PIANO.PAM"),
        Map.entry("newspaper-zombie", "768/FULL/ZOMBIE/ZOMBIE_MODERN_NEWSPAPER/ZOMBIE_MODERN_NEWSPAPER.PAM"),
        Map.entry("arcade-zombie", "768/FULL/ZOMBIE/ZOMBIE_80S_ARCADE/ZOMBIE_80S_ARCADE.PAM"),
        Map.entry(
            "barrel-roller-zombie",
            "768/FULL/ZOMBIE/ZOMBIE_PIRATE_BARREL_PUSHER/ZOMBIE_PIRATE_BARREL_PUSHER.PAM"
        )
    );

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
        float scale = Math.min(getWidth() / 270f, getHeight() / 270f);
        float x = getX() + getWidth() * 0.5f;
        float y = getY() + getHeight() * 0.20f;
        player.draw(batch, pamPath, clip, stateTime, x, y, scale, scale, true);
        batch.setColor(before);
    }

    private static String resolvePam(PvzAssets assets, ZombieDefinition zombie) {
        if (zombie == null || zombie.getKey() == null) {
            return null;
        }
        String path = PAM_BY_KEY.get(zombie.getKey());
        if (path == null) {
            return null;
        }
        FileHandle file = assets.root().child("IMAGES").child(path);
        return file.exists() ? path : null;
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
            for (String preferred : List.of("walk", "idle")) {
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
}
