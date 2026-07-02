package model;

import java.util.List;
import java.util.Random;


public enum SeasonType {
    ANCIENT_EGYPT,
    FROSTBITE_CAVES,
    BIG_WAVE_BEACH,
    DARK_AGES
}

public class Tomb {
    private int health = 700;
    private int row;
    private int col;
    private boolean containsSun;
    private boolean containsPlantFood;

    public void takeDamage(int amount) {
        this.health -= amount;
        if (this.health <= 0) {
            destroy();
        }
    }

    private void destroy() {
    }
}
public enum SpecialLevelType {
    NORMAL,
    CONVEYOR_BELT,
    LOCKED_PLANTS,
    SAVE_OUR_SEEDS,
    TIMED_WAR,
    NIGHT_OPS,
    DEAD_LINE,
    LOVE_YOUR_PLANTS,
    PLANT_WHAT_YOU_GET
}

public class AdvancedLevel extends Level {
    private SeasonType season;
    private SpecialLevelType specialType;

    private int timeLimit;
    private int targetKills;
    private int deadLineCol;
    private int allowedLosses;
    private int initialSunAmount;

    public boolean checkSpecialLoseConditions(Board board, int lostPlantsCount) {
        if (specialType == SpecialLevelType.DEAD_LINE) {
            return board.hasZombiesCrossedColumn(deadLineCol);
        } else if (specialType == SpecialLevelType.LOVE_YOUR_PLANTS) {
            return lostPlantsCount >= allowedLosses;
        } else if (specialType == SpecialLevelType.SAVE_OUR_SEEDS) {
            return board.areEndangeredPlantsEaten();
        }
        return false;
    }
}