package pvz.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup;
import com.badlogic.gdx.utils.Scaling;
import model.MiniGamePlantSnapshot;
import model.MiniGameUnitSnapshot;
import model.PlantDefinition;
import model.ZombieDefinition;
import pvz.PvzApplication;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class MiniGameUnitLayer extends WidgetGroup {
    private static final int ROWS = 5;
    private static final int COLS = 9;

    private final PvzApplication app;
    private final ArrayList<Actor> plantActors = new ArrayList<>();
    private final ArrayList<Actor> zombieActors = new ArrayList<>();
    private List<MiniGamePlantSnapshot> plants = List.of();
    private List<MiniGameUnitSnapshot> zombies = List.of();

    public MiniGameUnitLayer(PvzApplication app) {
        this.app = app;
        setTransform(false);
    }

    public void setPlants(List<MiniGamePlantSnapshot> next) {
        List<MiniGamePlantSnapshot> safe = next == null ? List.of() : List.copyOf(next);
        if (needsPlantRebuild(safe)) {
            rebuildPlants(safe);
        }
        plants = safe;
        invalidate();
    }

    public void setZombies(List<MiniGameUnitSnapshot> next) {
        List<MiniGameUnitSnapshot> safe = next == null ? List.of() : List.copyOf(next);
        if (needsZombieRebuild(safe)) {
            rebuildZombies(safe);
        }
        zombies = safe;
        invalidate();
    }

    @Override
    public void layout() {
        float cellWidth = getWidth() / COLS;
        float cellHeight = getHeight() / ROWS;
        for (int index = 0; index < plants.size(); index++) {
            MiniGamePlantSnapshot plant = plants.get(index);
            Actor actor = plantActors.get(index);
            float width = cellWidth * 0.96f;
            float height = cellHeight * 1.18f;
            float x = plant.column() * cellWidth + (cellWidth - width) * 0.5f;
            float y = (ROWS - 1 - plant.row()) * cellHeight - cellHeight * 0.07f;
            actor.setBounds(x, y, width, height);
        }
        for (int index = 0; index < zombies.size(); index++) {
            MiniGameUnitSnapshot zombie = zombies.get(index);
            Actor actor = zombieActors.get(index);
            float width = cellWidth * 1.20f;
            float height = cellHeight * 1.55f;
            float x = (float) zombie.column() * cellWidth - width * 0.42f;
            float y = (ROWS - 1 - zombie.row()) * cellHeight - cellHeight * 0.20f;
            actor.setBounds(x, y, width, height);
            float healthRatio = zombie.maximumHealth() <= 0
                ? 1f : zombie.health() / (float) zombie.maximumHealth();
            actor.getColor().a = Math.max(0.45f, Math.min(1f, 0.55f + healthRatio * 0.45f));
        }
    }

    private boolean needsPlantRebuild(List<MiniGamePlantSnapshot> next) {
        if (next.size() != plantActors.size() || next.size() != plants.size()) {
            return true;
        }
        for (int index = 0; index < next.size(); index++) {
            if (!next.get(index).type().equals(plants.get(index).type())) {
                return true;
            }
        }
        return false;
    }

    private boolean needsZombieRebuild(List<MiniGameUnitSnapshot> next) {
        if (next.size() != zombieActors.size() || next.size() != zombies.size()) {
            return true;
        }
        for (int index = 0; index < next.size(); index++) {
            if (!next.get(index).type().equals(zombies.get(index).type())) {
                return true;
            }
        }
        return false;
    }

    private void rebuildPlants(List<MiniGamePlantSnapshot> next) {
        for (Actor actor : plantActors) {
            actor.remove();
        }
        plantActors.clear();
        for (MiniGamePlantSnapshot plant : next) {
            Actor actor = createPlantActor(plant.type());
            plantActors.add(actor);
            addActor(actor);
        }
    }

    private void rebuildZombies(List<MiniGameUnitSnapshot> next) {
        for (Actor actor : zombieActors) {
            actor.remove();
        }
        zombieActors.clear();
        for (MiniGameUnitSnapshot zombie : next) {
            Actor actor = createZombieActor(zombie.type());
            zombieActors.add(actor);
            addActor(actor);
        }
    }

    private Actor createPlantActor(String type) {
        PlantDefinition definition = app.services().gameData().getPlantFactory()
            .findDefinition(type).orElse(null);
        if (definition != null) {
            PlantAnimationActor animation = new PlantAnimationActor(app.assets(), definition);
            if (animation.hasAnimation()) {
                return animation;
            }
            Image image = PlantArtResolver.packetImage(app.assets().uiTheme(), definition);
            if (image != null) {
                image.setScaling(Scaling.fit);
                return image;
            }
        }
        return new Image(app.assets().skin().getDrawable("image_ui_dialog_asset_inner_bkgd_10"));
    }

    private Actor createZombieActor(String type) {
        if (isSunProducer(type)) {
            Image sun = app.assets().uiTheme().image(UiTheme.COIN_ICON);
            if (sun != null) {
                sun.setScaling(Scaling.fit);
                return sun;
            }
        }
        ZombieDefinition definition = resolveZombie(type);
        if (definition != null) {
            ZombieAnimationActor animation = new ZombieAnimationActor(app.assets(), definition);
            if (animation.hasAnimation()) {
                return animation;
            }
            Image image = ZombieArtResolver.image(app.assets().uiTheme(), definition);
            if (image != null) {
                image.setScaling(Scaling.fit);
                return image;
            }
        }
        return new Image(app.assets().skin().getDrawable("image_ui_dialog_asset_inner_bkgd_10"));
    }

    private ZombieDefinition resolveZombie(String type) {
        String lookup = switch (normalize(type)) {
            case "basic" -> "Basic Zombie";
            case "conehead" -> "Conehead Zombie";
            case "buckethead" -> "Buckethead Zombie";
            case "allstar" -> "All-Star Zombie";
            case "newspaper" -> "Newspaper Zombie";
            case "prospector" -> "Prospector Zombie";
            case "parasol" -> "Parasol Zombie";
            case "ra" -> "Ra Zombie";
            case "explorer" -> "Explorer Zombie";
            case "knight" -> "Knight Zombie";
            case "blockhead" -> "Brickhead Zombie";
            case "dodorider" -> "Dodo Rider Zombie";
            case "wizard" -> "Wizard Zombie";
            case "bowlingzombie" -> "Basic Zombie";
            default -> type;
        };
        return app.services().gameData().getZombieFactory().findDefinition(lookup)
            .orElseGet(() -> app.services().gameData().getZombieFactory()
                .findDefinition("Basic Zombie").orElse(null));
    }

    private boolean isSunProducer(String type) {
        return normalize(type).equals("sunproducerzombie");
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]", "");
    }
}
