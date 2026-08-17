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
import model.Tile;
import model.Zombie;
import pvz.assets.PvzAssets;

import java.util.function.BiConsumer;

public final class BattleBoardActor extends Actor implements Disposable {
    private static final float GRID_LEFT = 0.205f;
    private static final float GRID_BOTTOM = 0.115f;
    private static final float GRID_WIDTH = 0.735f;
    private static final float GRID_HEIGHT = 0.765f;

    private final PvzAssets assets;
    private final GameController controller;
    private final Level level;
    private final BattlePamRenderer pamRenderer;
    private final BiConsumer<Integer, Integer> cellClick;
    private final Texture pixel;

    private TextureRegion background;
    private TextureRegion sunIcon;
    private TextureRegion projectileIcon;
    private TextureRegion mowerIcon;
    private float animationTime;
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
        loadRegions();
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
        if (background != null) {
            batch.draw(background, getX(), getY(), getWidth(), getHeight());
            return;
        }
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
        float scale = cellHeight() / 235f * 1.18f;
        for (Plant plant : board.getPlants()) {
            if (plant == null || plant.isDestroyed() || plant.getPosition() == null) {
                continue;
            }
            int row = plant.getPosition().getRow();
            int col = plant.getPosition().getColumn();
            float x = cellCenterX(col);
            float y = cellBottom(row) + cellHeight() * 0.43f;
            boolean animated = pamRenderer.drawPlant(batch, plant, animationTime, x, y, scale);
            if (!animated) {
                drawFallbackPlant(batch, x, y, plant);
            }
            drawHealth(batch, x, cellTop(row) - 8f, plant.getHealth(), plant.getMaxHealth(), 38f);
        }
    }

    private void drawZombies(Batch batch, Board board) {
        float scale = cellHeight() / 250f * 1.24f;
        SeasonType season = level.getSeason();
        for (Zombie zombie : board.getZombies()) {
            if (zombie == null || zombie.isDead() || zombie.getPosition() == null) {
                continue;
            }
            int row = zombie.getPosition().getRow();
            double column = zombie.getPosition().getColumn();
            float x = gridLeft() + (float) ((column + 0.5d) * cellWidth());
            float y = cellBottom(row) + cellHeight() * 0.42f;
            boolean animated = pamRenderer.drawZombie(batch, zombie, season, animationTime, x, y, scale);
            if (!animated) {
                drawFallbackZombie(batch, x, y);
            }
            drawHealth(
                batch,
                x,
                cellTop(row) - 2f,
                zombie.getEffectiveHealth(),
                Math.max(1, zombie.getMaximumHealth()),
                44f
            );
        }
    }

    private void drawProjectiles(Batch batch, Board board) {
        for (Projectile projectile : board.getProjectiles()) {
            if (projectile == null || !projectile.isActive() || projectile.getPosition() == null) {
                continue;
            }
            float x = gridLeft()
                + (float) ((projectile.getPosition().getColumn() + 0.5d) * cellWidth());
            int row = projectile.getPosition().getRow();
            float y = cellBottom(row) + cellHeight() * 0.56f;
            if (projectileIcon != null) {
                batch.draw(projectileIcon, x - 8f, y - 8f, 16f, 16f);
            } else {
                batch.setColor(0.25f, 0.92f, 0.18f, 1f);
                batch.draw(pixel, x - 6f, y - 6f, 12f, 12f);
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
            float size = Math.min(cellWidth(), cellHeight()) * 0.48f;
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
        float width = cellWidth() * 0.74f;
        float height = cellHeight() * 0.58f;
        for (LawnMower mower : board.getLawnMowers()) {
            if (mower.isActivated()) {
                continue;
            }
            float x = gridLeft() - width * 0.83f;
            float y = cellBottom(mower.getRow()) + (cellHeight() - height) / 2f;
            batch.draw(mowerIcon, x, y, width, height);
        }
    }

    private void drawFallbackPlant(Batch batch, float x, float y, Plant plant) {
        TextureRegion packet = packetRegion(plant.getDefinition());
        float size = Math.min(cellWidth(), cellHeight()) * 0.72f;
        if (packet != null) {
            batch.draw(packet, x - size / 2f, y - size * 0.35f, size, size);
            return;
        }
        batch.setColor(0.20f, 0.75f, 0.18f, 1f);
        batch.draw(pixel, x - size / 3f, y - size / 4f, size * 0.66f, size * 0.66f);
        batch.setColor(Color.WHITE);
    }

    private void drawFallbackZombie(Batch batch, float x, float y) {
        float width = cellWidth() * 0.42f;
        float height = cellHeight() * 0.70f;
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
        return getWidth() * GRID_LEFT;
    }

    private float gridBottomLocal() {
        return getHeight() * GRID_BOTTOM;
    }

    private float gridWidth() {
        return getWidth() * GRID_WIDTH;
    }

    private float gridHeight() {
        return getHeight() * GRID_HEIGHT;
    }

    private float cellWidth() {
        return gridWidth() / Board.DEFAULT_COLUMNS;
    }

    private float cellHeight() {
        return gridHeight() / Board.DEFAULT_ROWS;
    }

    private void loadRegions() {
        background = assets.uiAtlas().region(backgroundId(level.getSeason()));
        sunIcon = assets.uiAtlas().region("IMAGE_UI_HUD_INGAME_SUN");
        projectileIcon = findFirstRegion(
            "IMAGE_EFFECTS_PEA_PROJECTILE_PEA_PROJECTILE_25X25",
            "IMAGE_EFFECTS_PEA_PROJECTILE_PEA_PROJECTILE_26X26",
            "IMAGE_EFFECTS_PEA_PROJECTILE_PEA_PROJECTILE_30X30"
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

    private String backgroundId(SeasonType season) {
        return switch (season) {
            case ANCIENT_EGYPT -> "IMAGE_BACKGROUNDS_EGYPT_TEXTURE";
            case FROSTBITE_CAVES -> "IMAGE_BACKGROUNDS_ICEAGE_TEXTURE";
            case BIG_WAVE_BEACH -> "IMAGE_BACKGROUNDS_BEACH_TEXTURE";
            case DARK_AGES -> "IMAGE_BACKGROUNDS_DARK_TEXTURE";
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
}
