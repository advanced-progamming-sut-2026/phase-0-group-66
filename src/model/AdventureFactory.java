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
    private final LevelFactory levelFactory;

    public AdventureFactory() {
        levelFactory = new LevelFactory();
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
            Level level = levelFactory.createAdventureLevel(season, levelNumber, type,
                waveCounts[levelNumber - 1], firstWaveCosts[levelNumber - 1]);
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
}
