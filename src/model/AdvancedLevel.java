package model;

public class AdvancedLevel extends Level {
    private SeasonType season;
    private SpecialLevelType specialType;
    private int timeLimit;
    private int targetKills;
    private int deadLineCol;
    private int allowedLosses;
    private int initialSunAmount;

    public AdvancedLevel() {
        this(SeasonType.ANCIENT_EGYPT, SpecialLevelType.NORMAL);
    }

    public AdvancedLevel(SeasonType season, SpecialLevelType specialType) {
        this.season = season;
        this.specialType = specialType;
    }

    public SeasonType getSeason() {
        return season;
    }

    public SpecialLevelType getSpecialType() {
        return specialType;
    }

    public boolean checkSpecialLoseConditions(Board board, int lostPlantsCount) {
        if (specialType == SpecialLevelType.DEAD_LINE) {
            return board.hasZombiesCrossedColumn(deadLineCol);
        }
        if (specialType == SpecialLevelType.LOVE_YOUR_PLANTS) {
            return lostPlantsCount >= allowedLosses;
        }
        if (specialType == SpecialLevelType.SAVE_OUR_SEEDS) {
            return board.areEndangeredPlantsEaten();
        }
        return false;
    }
}
