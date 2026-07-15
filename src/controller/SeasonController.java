package controller;

import model.AdvancedLevel;
import model.Board;
import model.SeasonType;

public class SeasonController {
    private final Board board;
    private final AdvancedLevel currentLevel;

    public SeasonController(Board board, AdvancedLevel currentLevel) {
        this.board = board;
        this.currentLevel = currentLevel;
    }

    public void processEnvironmentalEffects(int currentTick) {
        SeasonType season = currentLevel.getSeason();
        switch (season) {
            case ANCIENT_EGYPT:
                break;
            case FROSTBITE_CAVES:
                applyFreezingWind();
                break;
            case BIG_WAVE_BEACH:
                updateWaterTides(currentTick);
                break;
            case DARK_AGES:
                if (currentTick % 200 == 0) {
                    spawnRandomTombs();
                }
                break;
            default:
                break;
        }
    }

    private void applyFreezingWind() {
    }

    private void updateWaterTides(int tick) {
    }

    private void spawnRandomTombs() {
    }
}
