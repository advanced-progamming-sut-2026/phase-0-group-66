package model;

public class AdvancedLevel extends Level {
    private final SeasonType advancedSeason;
    private final SpecialLevelType advancedSpecialType;
    private int timeLimitSeconds;
    private int targetKills;
    private int deadLineColumn = 2;
    private int allowedLosses = 5;
    private int initialSunAmount = 50;

    public AdvancedLevel() {
        this(SeasonType.ANCIENT_EGYPT, SpecialLevelType.NORMAL);
    }

    public AdvancedLevel(SeasonType season, SpecialLevelType specialType) {
        super("advanced-" + season.name().toLowerCase(), season, 1, specialType, 8, 50);
        advancedSeason = season;
        advancedSpecialType = specialType;
    }

    @Override
    public SeasonType getSeason() { return advancedSeason; }

    @Override
    public SpecialLevelType getSpecialType() { return advancedSpecialType; }

    public int getTimeLimitSeconds() { return timeLimitSeconds; }
    public int getTargetKills() { return targetKills; }
    public int getDeadLineColumn() { return deadLineColumn; }
    public int getAllowedLosses() { return allowedLosses; }
    public int getInitialSunAmount() { return initialSunAmount; }

    public void setTimeLimitSeconds(int value) { timeLimitSeconds = Math.max(0, value); }
    public void setTargetKills(int value) { targetKills = Math.max(0, value); }
    public void setDeadLineColumn(int value) { deadLineColumn = Math.max(0, value); }
    public void setAllowedLosses(int value) { allowedLosses = Math.max(0, value); }
    public void setInitialSunAmount(int value) { initialSunAmount = Math.max(0, value); }

    public boolean checkSpecialLoseConditions(Board board, int lostPlantsCount) {
        if (advancedSpecialType == SpecialLevelType.DEAD_LINE) {
            return board.hasZombiesCrossedColumn(deadLineColumn);
        }
        if (advancedSpecialType == SpecialLevelType.LOVE_YOUR_PLANTS) {
            return lostPlantsCount >= allowedLosses;
        }
        return advancedSpecialType == SpecialLevelType.SAVE_OUR_SEEDS
            && board.areEndangeredPlantsEaten();
    }
}
