package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Level {
    private String levelId;
    private String levelType;
    private SeasonType season;
    private int levelNumber;
    private int allowedPlantCount;
    private int startingSunAmount;
    private final List<Wave> waves;
    private final List<String> forcedPlants;
    private final List<String> lockedPlants;
    private final List<String> bannedPlantFamilies;
    private final LinkedHashMap<String, String> familyRepresentativePlants;
    private final List<String> conveyorPlants;
    private final List<GridPosition> protectedPlantPositions;
    private String protectedPlantType;
    private TimedWarObjective timedWarObjective;
    private int timeLimitSeconds;
    private int timedWarTarget;
    private int deadLineColumn;
    private int allowedPlantLosses;
    private boolean waitForZombieWaves;
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
        this.forcedPlants = new ArrayList<>();
        this.lockedPlants = new ArrayList<>();
        this.bannedPlantFamilies = new ArrayList<>();
        this.familyRepresentativePlants = new LinkedHashMap<>();
        this.conveyorPlants = new ArrayList<>();
        this.protectedPlantPositions = new ArrayList<>();
        this.protectedPlantType = "Wall-nut";
        this.timedWarObjective = TimedWarObjective.KILLS;
        this.deadLineColumn = 2;
        this.allowedPlantLosses = 5;
    }

    public String getLevelId() { return levelId; }
    public String getLevelType() { return levelType; }
    public SpecialLevelType getSpecialType() { return SpecialLevelType.valueOf(levelType); }
    public SeasonType getSeason() { return season; }
    public int getLevelNumber() { return levelNumber; }
    public int getAllowedPlantCount() { return allowedPlantCount; }
    public int getStartingSunAmount() { return startingSunAmount; }
    public boolean isCompleted() { return completed; }
    public List<Wave> getWaves() { return Collections.unmodifiableList(waves); }
    public List<String> getForcedPlants() { return Collections.unmodifiableList(forcedPlants); }
    public List<String> getLockedPlants() { return Collections.unmodifiableList(lockedPlants); }
    public List<String> getBannedPlantFamilies() {
        return Collections.unmodifiableList(bannedPlantFamilies);
    }
    public Map<String, String> getFamilyRepresentativePlants() {
        return Collections.unmodifiableMap(familyRepresentativePlants);
    }
    public List<String> getConveyorPlants() { return Collections.unmodifiableList(conveyorPlants); }
    public List<GridPosition> getProtectedPlantPositions() {
        return Collections.unmodifiableList(protectedPlantPositions);
    }
    public String getProtectedPlantType() { return protectedPlantType; }
    public TimedWarObjective getTimedWarObjective() { return timedWarObjective; }
    public int getTimeLimitSeconds() { return timeLimitSeconds; }
    public int getTimedWarTarget() { return timedWarTarget; }
    public int getDeadLineColumn() { return deadLineColumn; }
    public int getAllowedPlantLosses() { return allowedPlantLosses; }
    public boolean isWaitForZombieWaves() { return waitForZombieWaves; }

    public void addWave(Wave wave) {
        if (wave != null) {
            waves.add(wave);
        }
    }

    public Level configureConveyorPlants(List<String> plants) {
        replaceCleanNames(conveyorPlants, plants);
        return this;
    }

    public Level configureLockedPlants(List<String> forced, List<String> locked,
                                       List<String> bannedFamilies) {
        replaceCleanNames(forcedPlants, forced);
        replaceCleanNames(lockedPlants, locked);
        replaceCleanNames(bannedPlantFamilies, bannedFamilies);
        if (forcedPlants.size() > allowedPlantCount) {
            throw new IllegalArgumentException("Forced plants exceed the selection capacity.");
        }
        return this;
    }

    public Level configureFamilyRepresentativeLocks(Map<String, String> representatives) {
        familyRepresentativePlants.clear();
        if (representatives == null) {
            return this;
        }
        for (Map.Entry<String, String> entry : representatives.entrySet()) {
            String family = entry.getKey();
            String plant = entry.getValue();
            if (family == null || family.isBlank() || plant == null || plant.isBlank()) {
                throw new IllegalArgumentException("Family lock entries cannot be empty.");
            }
            familyRepresentativePlants.put(family.trim(), plant.trim());
        }
        return this;
    }

    public Level configureProtectedPlants(String plantType, List<GridPosition> positions) {
        if (plantType == null || plantType.isBlank()) {
            throw new IllegalArgumentException("Protected plant type cannot be empty.");
        }
        protectedPlantType = plantType.trim();
        protectedPlantPositions.clear();
        if (positions != null) {
            protectedPlantPositions.addAll(positions);
        }
        return this;
    }

    public Level configureTimedWar(TimedWarObjective objective, int timeLimit,
                                   int target) {
        if (timeLimit <= 0 || target <= 0) {
            throw new IllegalArgumentException("Timed War requires a positive limit and target.");
        }
        timedWarObjective = objective == null ? TimedWarObjective.KILLS : objective;
        timeLimitSeconds = timeLimit;
        timedWarTarget = target;
        return this;
    }

    public Level configureDeadLine(int column) {
        if (column < 0 || column >= Board.DEFAULT_COLUMNS) {
            throw new IllegalArgumentException("Dead-line column is outside the board.");
        }
        deadLineColumn = column;
        return this;
    }

    public Level configureAllowedPlantLosses(int losses) {
        if (losses <= 0) {
            throw new IllegalArgumentException("Allowed plant losses must be positive.");
        }
        allowedPlantLosses = losses;
        return this;
    }

    public Level configureWaitForZombieWaves(boolean wait) {
        waitForZombieWaves = wait;
        return this;
    }

    public String getSpecialRuleSummary() {
        return switch (getSpecialType()) {
            case NORMAL -> "Normal battle rules.";
            case CONVEYOR_BELT -> "Plants arrive on the conveyor every 12 seconds.";
            case LOCKED_PLANTS -> "Forced plants=" + forcedPlants + ", locked="
                + lockedPlants + ", family representatives=" + familyRepresentativePlants
                + ", banned families=" + bannedPlantFamilies + ".";
            case SAVE_OUR_SEEDS -> "Protect every marked " + protectedPlantType + ".";
            case TIMED_WAR -> "Reach " + timedWarTarget + " "
                + timedWarObjective.name().toLowerCase() + " within " + timeLimitSeconds
                + " seconds.";
            case NIGHT_OPS -> "No sky sun will fall.";
            case DEAD_LINE -> "Do not let zombies cross column " + (deadLineColumn + 1) + ".";
            case LOVE_YOUR_PLANTS -> "Lose fewer than " + allowedPlantLosses + " plants.";
            case PLANT_WHAT_YOU_GET -> "Use only the starting sun, then start zombie waves.";
        };
    }

    public void loadLevel() { completed = false; }
    public void startLevel() { completed = false; }
    public void completeLevel() { completed = true; }

    public Level copyForPlay() {
        Level copy = new Level(levelId, season, levelNumber, getSpecialType(),
            allowedPlantCount, startingSunAmount);
        for (Wave wave : waves) {
            copy.addWave(new Wave(wave.getWaveNumber(), wave.getDifficultyCost(), wave.getDelay()));
        }
        copy.configureConveyorPlants(conveyorPlants);
        copy.configureLockedPlants(forcedPlants, lockedPlants, bannedPlantFamilies);
        copy.configureFamilyRepresentativeLocks(familyRepresentativePlants);
        copy.configureProtectedPlants(protectedPlantType, protectedPlantPositions);
        copy.timedWarObjective = timedWarObjective;
        copy.timeLimitSeconds = timeLimitSeconds;
        copy.timedWarTarget = timedWarTarget;
        copy.deadLineColumn = deadLineColumn;
        copy.allowedPlantLosses = allowedPlantLosses;
        copy.waitForZombieWaves = waitForZombieWaves;
        return copy;
    }

    @Override
    public String toString() {
        return levelId + " [season=" + season + ", level=" + levelNumber
            + ", type=" + levelType + ", waves=" + waves.size() + "]";
    }

    private void replaceCleanNames(List<String> target, List<String> source) {
        target.clear();
        if (source == null) {
            return;
        }
        for (String value : source) {
            if (value != null && !value.isBlank()) {
                target.add(value.trim());
            }
        }
    }
}
