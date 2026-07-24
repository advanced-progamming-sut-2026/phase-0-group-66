package controller;

import model.AdvancedLevel;
import model.Board;
import model.Plant;
import model.SeasonType;
import model.Tile;
import model.TileType;

/** Legacy beach-tide adapter. Wave-start Frostbite and Dark Ages effects live in Game. */
public class SeasonController {
    private final Board board;
    private final AdvancedLevel currentLevel;

    public SeasonController(Board board, AdvancedLevel currentLevel) {
        if (board == null || currentLevel == null) {
            throw new IllegalArgumentException("Season controller dependencies cannot be null.");
        }
        this.board = board;
        this.currentLevel = currentLevel;
    }

    public void processEnvironmentalEffects(int currentTick) {
        if (currentLevel.getSeason() == SeasonType.BIG_WAVE_BEACH
            && currentTick % 150 == 0) {
            updateWaterTides(currentTick);
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
}
