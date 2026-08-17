package pvz.ui;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import model.PlantDefinition;
import pvz.assets.PvzAssets;
import pvz.libpvz.pam.PamPlayer;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class PlantAnimationActor extends Actor {
    private static final Map<String, String> PAM_ALIASES = createAliases();

    private final PamPlayer player;
    private final String pamPath;
    private final String clip;
    private float stateTime;

    public PlantAnimationActor(PvzAssets assets, PlantDefinition plant) {
        player = assets.animations();
        pamPath = resolvePam(assets, plant);
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
        Color color = getColor();
        batch.setColor(color.r, color.g, color.b, color.a * parentAlpha);
        float scale = Math.min(getWidth() / 230f, getHeight() / 230f);
        float x = getX() + getWidth() * 0.5f;
        float y = getY() + getHeight() * 0.52f;
        player.draw(batch, pamPath, clip, stateTime, x, y, scale, scale, true);
        batch.setColor(before);
    }

    private static String resolvePam(PvzAssets assets, PlantDefinition plant) {
        if (plant == null) {
            return null;
        }
        String normalized = normalize(plant.getKey());
        String alias = PAM_ALIASES.getOrDefault(normalized, normalized);
        String[] roots = {
            "768/INITIAL/PLANT/",
            "768/FULL/PLANT/",
            "768/INITIAL/EMPOWERMINTS/PLANT/"
        };
        for (String root : roots) {
            String candidate = root + alias + "/" + alias + ".PAM";
            FileHandle file = assets.root().child("IMAGES").child(candidate);
            if (file.exists()) {
                return candidate;
            }
        }
        return null;
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
            for (String name : clips) {
                if ("idle".equalsIgnoreCase(name)) {
                    return name;
                }
            }
            for (String name : clips) {
                if (name.toLowerCase(Locale.ROOT).contains("idle")) {
                    return name;
                }
            }
            return clips.get(0);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
    }

    private static Map<String, String> createAliases() {
        Map<String, String> aliases = new HashMap<>();
        aliases.put("TWINSUNFLOWER", "SUNFLOWER_TWIN");
        aliases.put("PRIMALSUNFLOWER", "PRIMAL_SUNFLOWER");
        aliases.put("ROTOBAGA", "ROTORUTABAGA");
        aliases.put("MEGAGATLINGPEA", "MEGAGATLING");
        aliases.put("KERNELPULT", "KERNALPULT");
        aliases.put("PRIMALPOTATOMINE", "PRIMAL_POTATOMINE");
        aliases.put("ICEBERGLETTUCE", "ICEBURG");
        aliases.put("PHATBEET", "PHATBEETS");
        aliases.put("PIERCEMINT", "SPEARMINT");
        return aliases;
    }
}
