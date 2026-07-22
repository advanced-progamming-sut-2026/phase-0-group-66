package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class AdventureFactory {
    private final List<Chapter> chapters;
    private final Map<String, Chapter> chaptersByName;

    public AdventureFactory() {
        chapters = createChapters();
        LinkedHashMap<String, Chapter> index = new LinkedHashMap<>();
        for (Chapter chapter : chapters) {
            index.put(PlantDefinition.normalizeKey(chapter.getName()), chapter);
            index.put(PlantDefinition.normalizeKey(chapter.getSeason().name()), chapter);
        }
        chaptersByName = Collections.unmodifiableMap(index);
    }

    public List<Chapter> getChapters() { return chapters; }

    public Optional<Chapter> findChapter(String name) {
        return Optional.ofNullable(chaptersByName.get(PlantDefinition.normalizeKey(name)));
    }

    private List<Chapter> createChapters() {
        ArrayList<Chapter> result = new ArrayList<>();
        result.add(createChapter(1, "Ancient Egypt", SeasonType.ANCIENT_EGYPT,
            SpecialLevelType.CONVEYOR_BELT, SpecialLevelType.LOCKED_PLANTS));
        result.add(createChapter(2, "Frostbite Caves", SeasonType.FROSTBITE_CAVES,
            SpecialLevelType.SAVE_OUR_SEEDS, SpecialLevelType.TIMED_WAR));
        result.add(createChapter(3, "Big Wave Beach", SeasonType.BIG_WAVE_BEACH,
            SpecialLevelType.DEAD_LINE, SpecialLevelType.LOVE_YOUR_PLANTS));
        result.add(createChapter(4, "Dark Ages", SeasonType.DARK_AGES,
            SpecialLevelType.NIGHT_OPS, SpecialLevelType.PLANT_WHAT_YOU_GET));
        return Collections.unmodifiableList(result);
    }

    private Chapter createChapter(int chapterNumber, String name, SeasonType season,
                                  SpecialLevelType secondType,
                                  SpecialLevelType thirdType) {
        Chapter chapter = new Chapter(name, chapterNumber, season, chapterNumber == 1);
        int[] waveCounts = {3, 4, 5, 6};
        int[] firstWaveCosts = {1000, 1250, 1500, 1750};
        for (int levelNumber = 1; levelNumber <= 4; levelNumber++) {
            SpecialLevelType type = specialTypeFor(levelNumber, secondType, thirdType);
            int startingSun = type == SpecialLevelType.PLANT_WHAT_YOU_GET ? 800 : 50;
            Level level = new Level(
                season.name().toLowerCase().replace('_', '-') + "-" + levelNumber,
                season, levelNumber, type, 8, startingSun);
            configureSpecialLevel(level);
            addWaves(level, waveCounts[levelNumber - 1], firstWaveCosts[levelNumber - 1]);
            chapter.addLevel(level);
        }
        return chapter;
    }

    private SpecialLevelType specialTypeFor(int levelNumber, SpecialLevelType second,
                                            SpecialLevelType third) {
        if (levelNumber == 2) {
            return second;
        }
        if (levelNumber == 3) {
            return third;
        }
        return SpecialLevelType.NORMAL;
    }

    private void configureSpecialLevel(Level level) {
        switch (level.getSpecialType()) {
            case CONVEYOR_BELT -> level.configureConveyorPlants(List.of(
                "Peashooter", "Cabbage-pult", "Wall-nut", "Potato Mine"));
            case LOCKED_PLANTS -> level.configureLockedPlants(
                List.of("Peashooter", "Wall-nut"),
                List.of("Sunflower", "Cherry Bomb"),
                List.of("Mint")).configureFamilyRepresentativeLocks(Map.of(
                    "Shooter", "Peashooter",
                    "Wall-nut", "Wall-nut"));
            case SAVE_OUR_SEEDS -> level.configureProtectedPlants("Wall-nut", List.of(
                new GridPosition(0, 2), new GridPosition(2, 2), new GridPosition(4, 2)));
            case TIMED_WAR -> level.configureTimedWar(TimedWarObjective.KILLS, 60, 12);
            case DEAD_LINE -> level.configureDeadLine(2);
            case LOVE_YOUR_PLANTS -> level.configureAllowedPlantLosses(5);
            case PLANT_WHAT_YOU_GET -> level.configureWaitForZombieWaves(true);
            case NORMAL, NIGHT_OPS -> {
                // No extra numeric configuration is required.
            }
        }
    }

    private void addWaves(Level level, int waveCount, int firstWaveCost) {
        int previousCost = firstWaveCost;
        for (int waveNumber = 1; waveNumber <= waveCount; waveNumber++) {
            int cost;
            if (waveNumber == 1) {
                cost = previousCost;
            } else if (waveNumber == waveCount) {
                cost = roundToNearestFifty(previousCost * 2.0);
            } else {
                cost = roundToNearestFifty(previousCost * 1.25);
            }
            level.addWave(new Wave(waveNumber, cost, 0));
            previousCost = cost;
        }
    }

    private int roundToNearestFifty(double value) {
        return Math.max(1000, (int) Math.round(value / 50.0) * 50);
    }
}
