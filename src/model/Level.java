package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Level {
    private String levelId;
    private String levelType;
    private SeasonType season;
    private int levelNumber;
    private int allowedPlantCount;
    private int startingSunAmount;
    private final List<Wave> waves;
    private boolean completed;

    public Level() {
        this("unknown-level", SeasonType.ANCIENT_EGYPT, 1,
            SpecialLevelType.NORMAL, 8, 50);
    }

    public Level(String levelId, SeasonType season, int levelNumber,
                 SpecialLevelType specialType, int allowedPlantCount, int startingSunAmount) {
        if (levelId == null || levelId.isBlank()) {
            throw new IllegalArgumentException("Level id cannot be empty.");
        }
        if (levelNumber <= 0 || allowedPlantCount <= 0 || startingSunAmount < 0) {
            throw new IllegalArgumentException("Invalid level settings.");
        }
        this.levelId = levelId.trim();
        this.season = season == null ? SeasonType.ANCIENT_EGYPT : season;
        this.levelNumber = levelNumber;
        this.levelType = (specialType == null ? SpecialLevelType.NORMAL : specialType).name();
        this.allowedPlantCount = allowedPlantCount;
        this.startingSunAmount = startingSunAmount;
        this.waves = new ArrayList<>();
    }

    public String getLevelId() {
        return levelId;
    }

    public String getLevelType() {
        return levelType;
    }

    public SpecialLevelType getSpecialType() {
        return SpecialLevelType.valueOf(levelType);
    }

    public SeasonType getSeason() {
        return season;
    }

    public int getLevelNumber() {
        return levelNumber;
    }

    public int getAllowedPlantCount() {
        return allowedPlantCount;
    }

    public int getStartingSunAmount() {
        return startingSunAmount;
    }

    public boolean isCompleted() {
        return completed;
    }

    public List<Wave> getWaves() {
        return Collections.unmodifiableList(waves);
    }

    public void addWave(Wave wave) {
        if (wave != null) {
            waves.add(wave);
        }
    }

    public void loadLevel() {
    }

    public void startLevel() {
        completed = false;
    }

    public void completeLevel() {
        completed = true;
    }

    public Level copyForPlay() {
        Level copy = new Level(levelId, season, levelNumber, getSpecialType(),
            allowedPlantCount, startingSunAmount);
        for (Wave wave : waves) {
            copy.addWave(new Wave(wave.getWaveNumber(), wave.getDifficultyCost(), wave.getDelay()));
        }
        return copy;
    }

    @Override
    public String toString() {
        return levelId + " [season=" + season + ", level=" + levelNumber
            + ", type=" + levelType + ", waves=" + waves.size() + "]";
    }
}
