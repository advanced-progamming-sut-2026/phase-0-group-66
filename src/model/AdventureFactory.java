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

    public List<Chapter> getChapters() {
        return chapters;
    }

    public Optional<Chapter> findChapter(String name) {
        return Optional.ofNullable(chaptersByName.get(PlantDefinition.normalizeKey(name)));
    }

    private List<Chapter> createChapters() {
        ArrayList<Chapter> result = new ArrayList<>();
        result.add(createChapter(1, "Ancient Egypt", SeasonType.ANCIENT_EGYPT,
            SpecialLevelType.CONVEYOR_BELT));
        result.add(createChapter(2, "Frostbite Caves", SeasonType.FROSTBITE_CAVES,
            SpecialLevelType.SAVE_OUR_SEEDS));
        result.add(createChapter(3, "Big Wave Beach", SeasonType.BIG_WAVE_BEACH,
            SpecialLevelType.DEAD_LINE));
        result.add(createChapter(4, "Dark Ages", SeasonType.DARK_AGES,
            SpecialLevelType.NIGHT_OPS));
        return Collections.unmodifiableList(result);
    }

    private int roundToNearestFifty(double value) {
        return Math.max(1000, (int) Math.round(value / 50.0) * 50);
    }

    private Chapter createChapter(int chapterNumber, String name, SeasonType season,
                                  SpecialLevelType thirdLevelType) {
        Chapter chapter = new Chapter(name, chapterNumber, season, chapterNumber == 1);
        int[] waveCounts = {3, 4, 5, 6};
        int[] firstWaveCosts = {1000, 1250, 1500, 1750};
        for (int levelNumber = 1; levelNumber <= 4; levelNumber++) {
            SpecialLevelType type = levelNumber == 3
                ? thirdLevelType : SpecialLevelType.NORMAL;
            int startingSun = type == SpecialLevelType.PLANT_WHAT_YOU_GET ? 800 : 50;
            Level level = new Level(
                season.name().toLowerCase().replace('_', '-') + "-" + levelNumber,
                season, levelNumber, type, 8, startingSun);
            int previousCost = firstWaveCosts[levelNumber - 1];
            for (int waveNumber = 1; waveNumber <= waveCounts[levelNumber - 1]; waveNumber++) {
                int cost;
                if (waveNumber == 1) {
                    cost = previousCost;
                } else if (waveNumber == waveCounts[levelNumber - 1]) {
                    cost = roundToNearestFifty(previousCost * 2.0);
                } else {
                    cost = roundToNearestFifty(previousCost * 1.25);
                }
                level.addWave(new Wave(waveNumber, cost, 0));
                previousCost = cost;
            }
            chapter.addLevel(level);
        }
        return chapter;
    }
}
