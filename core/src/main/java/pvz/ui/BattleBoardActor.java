package pvz.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.utils.Disposable;
import controller.GameController;
import model.Board;
import model.LawnMower;
import model.Level;
import model.Plant;
import model.Projectile;
import model.SeasonType;
import model.Sun;
import model.Zombie;
import pvz.assets.PvzAssets;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public final class BattleBoardActor extends Actor implements Disposable {
    private final PvzAssets assets;
    private final GameController controller;
    private final Level level;
    private final BattlePamRenderer pamRenderer;
    private final BiConsumer<Integer, Integer> cellClick;
    private final Texture pixel;
    private final BoardGeometry geometry;
    private final Map<Plant, Float> attackStartedAt = new IdentityHashMap<>();
    private final Map<Plant, Float> attackUntil = new IdentityHashMap<>();
    private final Map<Plant, Integer> lastActionTicks = new IdentityHashMap<>();
    private final Map<Plant, Float> plantFoodStartedAt = new IdentityHashMap<>();
    private final Map<Plant, Float> plantFoodUntil = new IdentityHashMap<>();
    private final Map<Zombie, Float> foodDeathStartedAt = new IdentityHashMap<>();
    private final Map<Zombie, Float> foodDeathUntil = new IdentityHashMap<>();

    private TextureRegion backgroundLeft;
    private TextureRegion backgroundMain;
    private TextureRegion backgroundRight;
    private TextureRegion sunIcon;
    private TextureRegion projectileIcon;
    private TextureRegion mowerIcon;
    private float animationTime;
    private float cameraPan;
    private boolean showGrid;

    public BattleBoardActor(
        PvzAssets assets,
        GameController controller,
        Level level,
        BiConsumer<Integer, Integer> cellClick
    ) {
        this.assets = assets;
        this.controller = controller;
        this.level = level;
        this.cellClick = cellClick;
        this.pamRenderer = new BattlePamRenderer(assets);
        this.pixel = createPixel();
        this.geometry = BoardGeometry.forSeason(level.getSeason());
        loadRegions();
        configureInput();
    }

    private void configureInput() {
        setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.enabled);
        addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                int col = columnAt(x);
                int row = rowAt(y);
                if (row < 0 || col < 0) {
                    return false;
                }
                cellClick.accept(col + 1, row + 1);
                return true;
            }
        });
    }

    public void setShowGrid(boolean showGrid) {
        this.showGrid = showGrid;
    }

    public void setCameraPan(float cameraPan) {
        this.cameraPan = Math.max(0f, Math.min(1f, cameraPan));
    }

    public void triggerPlantFood(Plant plant, float durationSeconds) {
        if (plant == null) {
            return;
        }
        plantFoodStartedAt.put(plant, animationTime);
        plantFoodUntil.put(plant, animationTime + Math.max(0.25f, durationSeconds));
        attackStartedAt.remove(plant);
        attackUntil.remove(plant);
    }

    public void showPlantFoodCasualties(List<Zombie> zombies, float durationSeconds) {
        if (zombies == null || zombies.isEmpty()) {
            return;
        }
        float duration = Math.max(0.20f, durationSeconds);
        for (Zombie zombie : zombies) {
            if (zombie == null || zombie.getPosition() == null) {
                continue;
            }
            foodDeathStartedAt.put(zombie, animationTime);
            foodDeathUntil.put(zombie, animationTime + duration);
        }
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        animationTime += Math.max(0f, delta);
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        model.Game game = controller.getGame();
        if (game == null || game.getBoard() == null) {
            return;
        }
        Color previous = new Color(batch.getColor());
        batch.setColor(1f, 1f, 1f, parentAlpha);
        drawBackground(batch);
        if (showGrid) {
            drawGrid(batch, parentAlpha);
        }
        drawMowers(batch);
        drawPlants(batch, game.getBoard());
        drawProjectiles(batch, game.getBoard());
        drawZombies(batch, game.getBoard());
        drawSuns(batch, game.getBoard());
        batch.setColor(previous);
    }

    private void drawBackground(Batch batch) {
        if (backgroundMain == null) {
            drawFallbackBackground(batch);
            return;
        }
        float height = getHeight();
        float x = panoramaLeft();
        x = drawPiece(batch, backgroundLeft, x, height);
        x = drawPiece(batch, backgroundMain, x, height);
        drawPiece(batch, backgroundRight, x, height);
    }

    private float drawPiece(Batch batch, TextureRegion region, float x, float height) {
        if (region == null) {
            return x;
        }
        float width = pieceWidth(region, height);
        batch.draw(region, x, getY(), width, height);
        return x + width;
    }

    private void drawFallbackBackground(Batch batch) {
        batch.setColor(0.22f, 0.48f, 0.20f, 1f);
        batch.draw(pixel, getX(), getY(), getWidth(), getHeight());
        batch.setColor(Color.WHITE);
    }

    private void drawGrid(Batch batch, float parentAlpha) {
        float left = gridLeft();
        float bottom = gridBottom();
        float width = gridWidth();
        float height = gridHeight();
        float cellWidth = width / Board.DEFAULT_COLUMNS;
        float cellHeight = height / Board.DEFAULT_ROWS;
        batch.setColor(1f, 1f, 1f, 0.24f * parentAlpha);
        for (int col = 0; col <= Board.DEFAULT_COLUMNS; col++) {
            float x = left + col * cellWidth;
            batch.draw(pixel, x - 1f, bottom, 2f, height);
        }
        for (int row = 0; row <= Board.DEFAULT_ROWS; row++) {
            float y = bottom + row * cellHeight;
            batch.draw(pixel, left, y - 1f, width, 2f);
        }
        batch.setColor(Color.WHITE);
    }

    private void drawPlants(Batch batch, Board board) {
        float scale = cellHeight() / 235f * 0.96f;
        attackStartedAt.keySet().removeIf(plant -> !board.getPlants().contains(plant));
        attackUntil.keySet().removeIf(plant -> !board.getPlants().contains(plant));
        lastActionTicks.keySet().removeIf(plant -> !board.getPlants().contains(plant));
        plantFoodStartedAt.keySet().removeIf(plant -> !board.getPlants().contains(plant));
        plantFoodUntil.keySet().removeIf(plant -> !board.getPlants().contains(plant));
        for (Plant plant : board.getPlants()) {
            if (plant == null || plant.isDestroyed() || plant.getPosition() == null) {
                continue;
            }
            int row = plant.getPosition().getRow();
            int col = plant.getPosition().getColumn();
            float x = cellCenterX(col);
            float y = cellBottom(row) + cellHeight() * 0.38f;
            boolean plantFoodActive = isPlantFoodActive(plant);
            boolean attacking = !plantFoodActive && updateAttackState(plant, board);
            float clipTime;
            if (plantFoodActive) {
                clipTime = animationTime - plantFoodStartedAt.getOrDefault(plant, animationTime);
            } else if (attacking) {
                clipTime = animationTime - attackStartedAt.getOrDefault(plant, animationTime);
            } else {
                clipTime = animationTime;
            }
            boolean animated = pamRenderer.drawPlant(
                batch, plant, clipTime, x, y, scale, attacking, plantFoodActive
            );
            if (!animated) {
                drawFallbackPlant(batch, x, y, plant);
            }
            if (plantFoodActive) {
                drawPlantFoodVolley(batch, plant, row, x, y);
            }
            float barWidth = Math.max(30f, cellWidth() * 0.52f);
            drawHealth(batch, x, cellTop(row) - 7f, plant.getHealth(), plant.getMaxHealth(), barWidth);
        }
    }

    private void drawPlantFoodVolley(Batch batch, Plant plant, int row, float x, float y) {
        if (projectileIcon == null || !usesPeaPlantFoodVolley(plant)) {
            return;
        }
        float start = plantFoodStartedAt.getOrDefault(plant, animationTime);
        float elapsed = Math.max(0f, animationTime - start);
        float travelWidth = Math.max(cellWidth() * 2f, gridLeft() + gridWidth() - x);
        float peaSize = Math.max(11f, Math.min(cellWidth(), cellHeight()) * 0.18f);
        for (int index = 0; index < 10; index++) {
            float shotTime = index * 0.085f;
            float flight = (elapsed - shotTime) / 0.70f;
            if (flight < 0f || flight > 1f) {
                continue;
            }
            float px = x + flight * travelWidth;
            float py = cellBottom(row) + cellHeight() * 0.56f;
            batch.draw(projectileIcon, px - peaSize / 2f, py - peaSize / 2f, peaSize, peaSize);
        }
    }

    private boolean usesPeaPlantFoodVolley(Plant plant) {
        String key = plant.getDefinition().getKey().toLowerCase();
        return key.contains("peashooter")
            || key.equals("repeater")
            || key.equals("threepeater")
            || key.equals("pea-pod")
            || key.equals("split-pea")
            || key.equals("mega-gatling-pea")
            || key.equals("snow-pea");
    }

    private boolean isPlantFoodActive(Plant plant) {
        float until = plantFoodUntil.getOrDefault(plant, -1f);
        if (animationTime <= until) {
            return true;
        }
        plantFoodStartedAt.remove(plant);
        plantFoodUntil.remove(plant);
        return false;
    }

    private boolean updateAttackState(Plant plant, Board board) {
        int currentTicks = plant.getActionTicksRemaining();
        Integer previousTicks = lastActionTicks.put(plant, currentTicks);
        if (!plant.isShooter()) {
            attackStartedAt.remove(plant);
            attackUntil.remove(plant);
            return false;
        }
        boolean timerReset = previousTicks != null && currentTicks > previousTicks + 2;
        boolean hasTarget = board.findNearestZombieAhead(
            plant.getPosition().getRow(),
            plant.getPosition().getColumn()
        ) != null;
        if (timerReset && hasTarget) {
            attackStartedAt.put(plant, animationTime);
            attackUntil.put(plant, animationTime + 0.52f);
        }
        float until = attackUntil.getOrDefault(plant, -1f);
        if (animationTime <= until) {
            return true;
        }
        attackStartedAt.remove(plant);
        attackUntil.remove(plant);
        return false;
    }

    private void drawZombies(Batch batch, Board board) {
        float scale = cellHeight() / 250f * 0.82f;
        SeasonType season = level.getSeason();
        List<Zombie> zombies = zombiesToDraw(board);
        int[] previewLaneCount = new int[Board.DEFAULT_ROWS];
        for (Zombie zombie : zombies) {
            if (zombie == null || zombie.isDead() || zombie.getPosition() == null) {
                continue;
            }
            int row = zombie.getPosition().getRow();
            double column = zombie.getPosition().getColumn();
            if (cameraPan > 0.05f && row >= 0 && row < previewLaneCount.length) {
                column += previewLaneCount[row]++ * 0.34d;
            }
            float x = gridLeft() + (float) ((column + 0.5d) * cellWidth());
            float y = cellBottom(row) + cellHeight() * 0.34f;
            boolean animated = pamRenderer.drawZombie(
                batch, zombie, season, animationTime, x, y, scale
            );
            if (!animated) {
                drawFallbackZombie(batch, x, y);
            }
            float barWidth = Math.max(34f, cellWidth() * 0.52f);
            drawHealth(
                batch,
                x,
                cellTop(row) - 2f,
                zombie.getEffectiveHealth(),
                Math.max(1, zombie.getMaximumHealth()),
                barWidth
            );
        }
        drawPlantFoodDeathGhosts(batch, season, scale);
    }

    private void drawPlantFoodDeathGhosts(Batch batch, SeasonType season, float scale) {
        foodDeathUntil.entrySet().removeIf(entry -> animationTime > entry.getValue());
        foodDeathStartedAt.keySet().removeIf(zombie -> !foodDeathUntil.containsKey(zombie));
        Color original = new Color(batch.getColor());
        for (Map.Entry<Zombie, Float> entry : foodDeathUntil.entrySet()) {
            Zombie zombie = entry.getKey();
            if (zombie == null || zombie.getPosition() == null) {
                continue;
            }
            float start = foodDeathStartedAt.getOrDefault(zombie, animationTime);
            float duration = Math.max(0.01f, entry.getValue() - start);
            float progress = Math.max(0f, Math.min(1f, (animationTime - start) / duration));
            float alpha = 1f - progress;
            int row = zombie.getPosition().getRow();
            double column = zombie.getPosition().getColumn();
            float x = gridLeft() + (float) ((column + 0.5d) * cellWidth());
            float y = cellBottom(row) + cellHeight() * (0.34f - 0.08f * progress);
            batch.setColor(1f, 1f, 1f, original.a * alpha);
            pamRenderer.drawZombie(batch, zombie, season, animationTime, x, y, scale);
        }
        batch.setColor(original);
    }

    private List<Zombie> zombiesToDraw(Board board) {
        model.Game game = controller.getGame();
        if (cameraPan > 0.05f && game != null && game.getCurrentWave() != null) {
            return game.getCurrentWave().getZombies();
        }
        return board.getZombies();
    }

    private void drawProjectiles(Batch batch, Board board) {
        for (Projectile projectile : board.getProjectiles()) {
            if (projectile == null || !projectile.isActive() || projectile.getPosition() == null) {
                continue;
            }
            float x = gridLeft()
                + (float) ((projectile.getPosition().getColumn() + 0.5d) * cellWidth());
            int row = projectile.getPosition().getRow();
            float y = cellBottom(row) + cellHeight() * 0.55f;
            float size = Math.max(12f, Math.min(cellWidth(), cellHeight()) * 0.20f);
            if (projectileIcon != null) {
                batch.draw(projectileIcon, x - size / 2f, y - size / 2f, size, size);
            } else {
                batch.setColor(0.25f, 0.92f, 0.18f, 1f);
                batch.draw(pixel, x - size / 2f, y - size / 2f, size, size);
                batch.setColor(Color.WHITE);
            }
        }
    }

    private void drawSuns(Batch batch, Board board) {
        for (Sun sun : board.getSuns()) {
            if (sun == null || sun.isCollected() || sun.getPosition() == null) {
                continue;
            }
            int row = sun.getPosition().getRow();
            int col = sun.getPosition().getColumn();
            float x = cellCenterX(col);
            float y = cellBottom(row) + cellHeight() * 0.66f;
            float size = Math.min(cellWidth(), cellHeight()) * 0.46f;
            if (sunIcon != null) {
                batch.draw(sunIcon, x - size / 2f, y - size / 2f, size, size);
            } else {
                batch.setColor(1f, 0.85f, 0.12f, 1f);
                batch.draw(pixel, x - size / 2f, y - size / 2f, size, size);
                batch.setColor(Color.WHITE);
            }
        }
    }

    private void drawMowers(Batch batch) {
        if (mowerIcon == null) {
            return;
        }
        Board board = controller.getGame().getBoard();
        float width = cellWidth() * 0.72f;
        float height = cellHeight() * 0.58f;
        for (LawnMower mower : board.getLawnMowers()) {
            if (mower.isActivated()) {
                continue;
            }
            float x = gridLeft() - width * 0.90f;
            float y = cellBottom(mower.getRow()) + (cellHeight() - height) / 2f;
            batch.draw(mowerIcon, x, y, width, height);
        }
    }

    private void drawFallbackPlant(Batch batch, float x, float y, Plant plant) {
        TextureRegion packet = packetRegion(plant.getDefinition());
        float size = Math.min(cellWidth(), cellHeight()) * 0.78f;
        if (packet != null) {
            batch.draw(packet, x - size / 2f, y - size * 0.35f, size, size);
            return;
        }
        batch.setColor(0.20f, 0.75f, 0.18f, 1f);
        batch.draw(pixel, x - size / 3f, y - size / 4f, size * 0.66f, size * 0.66f);
        batch.setColor(Color.WHITE);
    }

    private void drawFallbackZombie(Batch batch, float x, float y) {
        float width = cellWidth() * 0.48f;
        float height = cellHeight() * 0.82f;
        batch.setColor(0.54f, 0.62f, 0.45f, 1f);
        batch.draw(pixel, x - width / 2f, y - height * 0.25f, width, height);
        batch.setColor(Color.WHITE);
    }

    private void drawHealth(
        Batch batch,
        float centerX,
        float y,
        int current,
        int maximum,
        float width
    ) {
        float ratio = Math.max(0f, Math.min(1f, current / (float) Math.max(1, maximum)));
        batch.setColor(0.12f, 0.08f, 0.04f, 0.82f);
        batch.draw(pixel, centerX - width / 2f, y, width, 5f);
        if (ratio > 0f) {
            float red = ratio < 0.35f ? 0.86f : 0.20f;
            float green = ratio < 0.35f ? 0.18f : 0.76f;
            batch.setColor(red, green, 0.12f, 1f);
            batch.draw(pixel, centerX - width / 2f + 1f, y + 1f, (width - 2f) * ratio, 3f);
        }
        batch.setColor(Color.WHITE);
    }

    private TextureRegion packetRegion(model.PlantDefinition definition) {
        String normalized = definition.getKey().toUpperCase().replaceAll("[^A-Z0-9]", "");
        normalized = switch (normalized) {
            case "GOOPEASHOOTER" -> "POISONPEASHOOTER";
            case "MEGAGATLINGPEA" -> "MEGAGATLING";
            case "CHERRYBOMB" -> "CHERRY_BOMB";
            case "ICEBERGLETTUCE" -> "ICEBURG";
            case "PIERCEMINT" -> "SPEARMINT";
            default -> normalized;
        };
        return assets.uiAtlas().region("IMAGE_UI_PACKETS_" + normalized);
    }

    private int columnAt(float localX) {
        if (localX < gridLeftLocal() || localX > gridLeftLocal() + gridWidth()) {
            return -1;
        }
        int col = (int) ((localX - gridLeftLocal()) / cellWidth());
        return Math.max(0, Math.min(Board.DEFAULT_COLUMNS - 1, col));
    }

    private int rowAt(float localY) {
        if (localY < gridBottomLocal() || localY > gridBottomLocal() + gridHeight()) {
            return -1;
        }
        int visualRow = (int) ((localY - gridBottomLocal()) / cellHeight());
        int modelRow = Board.DEFAULT_ROWS - 1 - visualRow;
        return Math.max(0, Math.min(Board.DEFAULT_ROWS - 1, modelRow));
    }

    private float cellCenterX(int col) {
        return gridLeft() + (col + 0.5f) * cellWidth();
    }

    private float cellBottom(int modelRow) {
        int visualRow = Board.DEFAULT_ROWS - 1 - modelRow;
        return gridBottom() + visualRow * cellHeight();
    }

    private float cellTop(int modelRow) {
        return cellBottom(modelRow) + cellHeight();
    }

    private float gridLeft() {
        return getX() + gridLeftLocal();
    }

    private float gridBottom() {
        return getY() + gridBottomLocal();
    }

    private float gridLeftLocal() {
        return mainLeft() - getX() + geometry.left() * mainWidth();
    }

    private float gridBottomLocal() {
        return geometry.bottom() * getHeight();
    }

    private float gridWidth() {
        return mainWidth() * geometry.width();
    }

    private float gridHeight() {
        return getHeight() * geometry.height();
    }

    private float cellWidth() {
        return gridWidth() / Board.DEFAULT_COLUMNS;
    }

    private float cellHeight() {
        return gridHeight() / Board.DEFAULT_ROWS;
    }

    private float panoramaLeft() {
        return getX() + (getWidth() - panoramaWidth()) / 2f + cameraOffsetX();
    }

    private float cameraOffsetX() {
        float overflow = Math.max(0f, panoramaWidth() - getWidth());
        return -overflow * 0.48f * cameraPan;
    }

    private float panoramaWidth() {
        return pieceWidth(backgroundLeft, getHeight())
            + pieceWidth(backgroundMain, getHeight())
            + pieceWidth(backgroundRight, getHeight());
    }

    private float mainLeft() {
        return panoramaLeft() + pieceWidth(backgroundLeft, getHeight());
    }

    private float mainWidth() {
        return pieceWidth(backgroundMain, getHeight());
    }

    private float pieceWidth(TextureRegion region, float height) {
        if (region == null || region.getRegionHeight() <= 0) {
            return 0f;
        }
        return height * region.getRegionWidth() / region.getRegionHeight();
    }

    private void loadRegions() {
        String prefix = backgroundPrefix(level.getSeason());
        backgroundMain = assets.uiAtlas().region(prefix + "_TEXTURE");
        backgroundLeft = assets.uiAtlas().region(prefix + "_TEXTURE_LEFT");
        backgroundRight = assets.uiAtlas().region(prefix + "_TEXTURE_RIGHT");
        sunIcon = assets.uiAtlas().region("IMAGE_UI_HUD_INGAME_SUN");
        projectileIcon = findFirstRegion(
            "IMAGE_EFFECTS_T_PEA_PROJECTILE_T_PEA_PROJECTILE_39X36",
            "IMAGE_EFFECTS_T_PEA_PROJECTILE_T_PEA_PROJECTILE_39X36_2"
        );
        mowerIcon = assets.uiAtlas().region(mowerId(level.getSeason()));
    }

    private TextureRegion findFirstRegion(String... ids) {
        for (String id : ids) {
            TextureRegion region = assets.uiAtlas().region(id);
            if (region != null) {
                return region;
            }
        }
        return null;
    }

    private String backgroundPrefix(SeasonType season) {
        return switch (season) {
            case ANCIENT_EGYPT -> "IMAGE_BACKGROUNDS_EGYPT";
            case FROSTBITE_CAVES -> "IMAGE_BACKGROUNDS_ICEAGE";
            case BIG_WAVE_BEACH -> "IMAGE_BACKGROUNDS_BEACH";
            case DARK_AGES -> "IMAGE_BACKGROUNDS_DARK";
        };
    }

    private String mowerId(SeasonType season) {
        return switch (season) {
            case ANCIENT_EGYPT -> "IMAGE_MOWERS_MOWER_EGYPT_MOWER_EGYPT_96X63";
            case FROSTBITE_CAVES -> "IMAGE_MOWERS_MOWER_ICEAGE_MOWER_ICEAGE_99X85";
            case BIG_WAVE_BEACH -> "IMAGE_MOWERS_MOWER_BEACH_MOWER_BEACH_209X75";
            case DARK_AGES -> "IMAGE_MOWERS_MOWER_DARK_MOWER_DARK_140X91";
        };
    }

    private Texture createPixel() {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }

    @Override
    public void dispose() {
        pixel.dispose();
    }

    private record BoardGeometry(float left, float bottom, float width, float height) {
        private static BoardGeometry forSeason(SeasonType season) {
            return switch (season) {
                case ANCIENT_EGYPT -> new BoardGeometry(0.244f, 0.102f, 0.728f, 0.640f);
                case FROSTBITE_CAVES -> new BoardGeometry(0.250f, 0.134f, 0.716f, 0.601f);
                case BIG_WAVE_BEACH -> new BoardGeometry(0.251f, 0.104f, 0.718f, 0.634f);
                case DARK_AGES -> new BoardGeometry(0.246f, 0.099f, 0.723f, 0.643f);
            };
        }
    }
}
