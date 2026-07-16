package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class Chapter {
    private String name;
    private int chapterNumber;
    private SeasonType season;
    private final List<Level> levels;
    private boolean unlocked;

    public Chapter() {
        this("Ancient Egypt", 1, SeasonType.ANCIENT_EGYPT, true);
    }

    public Chapter(String name, int chapterNumber, SeasonType season, boolean unlocked) {
        if (name == null || name.isBlank() || chapterNumber <= 0) {
            throw new IllegalArgumentException("Invalid chapter data.");
        }
        this.name = name.trim();
        this.chapterNumber = chapterNumber;
        this.season = season == null ? SeasonType.ANCIENT_EGYPT : season;
        this.unlocked = unlocked;
        this.levels = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public int getChapterNumber() {
        return chapterNumber;
    }

    public SeasonType getSeason() {
        return season;
    }

    public boolean isUnlocked() {
        return unlocked;
    }

    public void unlockNextLevel() {
        unlocked = true;
    }

    public void unlock() {
        unlocked = true;
    }

    public List<Level> getAvailableLevels() {
        return Collections.unmodifiableList(new ArrayList<>(levels));
    }

    public List<Level> getLevels() {
        return Collections.unmodifiableList(levels);
    }

    public Optional<Level> findLevel(int levelNumber) {
        return levels.stream().filter(level -> level.getLevelNumber() == levelNumber).findFirst();
    }

    public void addLevel(Level level) {
        if (level != null) {
            levels.add(level);
        }
    }

    @Override
    public String toString() {
        return chapterNumber + ". " + name + " (" + levels.size() + " levels)";
    }
}
