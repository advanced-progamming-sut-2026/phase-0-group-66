package controller;

import model.AdvancedLevel;
import model.Board;
import model.Plant;
import model.SeasonType;
import model.Tile;
import model.TileType;

import java.util.Random;

public class SeasonController {
    private final Board board;
    private final AdvancedLevel currentLevel;
    private final Random random = new Random();

    public SeasonController(Board board, AdvancedLevel currentLevel) {
        if (board == null || currentLevel == null) {
            throw new IllegalArgumentException("Season controller dependencies cannot be null.");
        }
        this.board = board;
        this.currentLevel = currentLevel;
    }

    public void processEnvironmentalEffects(int currentTick) {
        SeasonType season = currentLevel.getSeason();
        if (season == SeasonType.FROSTBITE_CAVES && currentTick % 200 == 0) {
            applyFreezingWind();
        } else if (season == SeasonType.BIG_WAVE_BEACH && currentTick % 150 == 0) {
            updateWaterTides(currentTick);
        } else if (season == SeasonType.DARK_AGES && currentTick % 200 == 0) {
            spawnRandomTomb();
        }
    }

    private void applyFreezingWind() {
        int affectedRow = random.nextInt(board.getRows());
        for (int col = 0; col < board.getCols(); col++) {
            Tile tile = board.getTile(affectedRow, col);
            if (tile.getPlant() == null && tile.getType() == TileType.NORMAL) {
                tile.setTileType(TileType.ICE);
            }
        }
    }

    private void updateWaterTides(int tick) {
        int waterStart = tick % 300 == 0 ? 6 : 7;
        for (int row = 0; row < board.getRows(); row++) {
            for (int col = 0; col < board.getCols(); col++) {
                Tile tile = board.getTile(row, col);
                if (col >= waterStart) {
                    Plant plant = tile.getPlant();
                    if (plant != null && !plant.getDefinition().hasTag("Water")) {
                        board.removePlant(row, col);
                    }
                    tile.setTileType(TileType.WATER);
                } else if (tile.getType() == TileType.WATER) {
                    tile.setTileType(TileType.NORMAL);
                }
            }
        }
    }

    private void spawnRandomTomb() {
        for (int attempt = 0; attempt < 20; attempt++) {
            int row = random.nextInt(board.getRows());
            int col = 2 + random.nextInt(Math.max(1, board.getCols() - 4));
            Tile tile = board.getTile(row, col);
            if (tile.getType() == TileType.NORMAL && tile.getPlant() == null) {
                tile.setTileType(TileType.TOMB);
                return;
            }
        }
    }
}
