package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Chapter {
    private String name;
    private int chapterNumber;
    private final List<Level> levels;
    private boolean unlocked;

    public Chapter() {
        levels = new ArrayList<>();
    }

    public String getName() {
        return name == null ? "chapter-" + chapterNumber : name;
    }

    public int getChapterNumber() {
        return chapterNumber;
    }

    public boolean isUnlocked() {
        return unlocked;
    }

    public void unlockNextLevel() {
        unlocked = true;
    }

    public List<Level> getAvailableLevels() {
        List<Level> available = new ArrayList<>();
        for (Level level : levels) {
            if (!level.isCompleted()) {
                available.add(level);
            }
        }
        return Collections.unmodifiableList(available);
    }

    public void addLevel(Level level) {
        if (level != null) {
            levels.add(level);
        }
    }
}
