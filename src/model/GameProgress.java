package model;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class GameProgress implements Serializable {
    private static final long serialVersionUID = 1L;

    private int gamesPlayed;
    private int bestMiniGameScore;
    private int bestMeowPoints;
    private int completedDailyQuests;
    private int completedOtherQuests;
    private final LinkedHashSet<String> unlockedChapters;
    private final LinkedHashSet<String> unlockedLevels;
    private final LinkedHashSet<String> completedLevelIds;

    public GameProgress() {
        unlockedChapters = new LinkedHashSet<>();
        unlockedLevels = new LinkedHashSet<>();
        completedLevelIds = new LinkedHashSet<>();
    }

    public int getGamesPlayed() {
        return gamesPlayed;
    }

    public int getCompletedLevels() {
        return completedLevelIds.size();
    }

    public int getBestMiniGameScore() {
        return bestMiniGameScore;
    }

    public int getBestMeowPoints() {
        return bestMeowPoints;
    }

    public int getCompletedDailyQuests() {
        return completedDailyQuests;
    }

    public int getCompletedOtherQuests() {
        return completedOtherQuests;
    }

    public Set<String> getUnlockedChapters() {
        return Collections.unmodifiableSet(unlockedChapters);
    }

    public Set<String> getUnlockedLevels() {
        return Collections.unmodifiableSet(unlockedLevels);
    }

    public void recordGamePlayed() {
        gamesPlayed++;
    }

    public void unlockChapter(Chapter chapter) {
        if (chapter != null) {
            unlockedChapters.add(chapter.getName());
        }
    }

    public void unlockLevel(Level level) {
        if (level != null) {
            unlockedLevels.add(level.getLevelId());
        }
    }

    public void completeLevel(Level level) {
        if (level != null) {
            String levelId = level.getLevelId();
            unlockedLevels.add(levelId);
            completedLevelIds.add(levelId);
        }
    }

    public void updateBestScore(int score) {
        bestMiniGameScore = Math.max(bestMiniGameScore, score);
    }

    public void updateBestMeowPoints(int score) {
        bestMeowPoints = Math.max(bestMeowPoints, score);
    }

    public void recordCompletedQuest(boolean daily) {
        if (daily) {
            completedDailyQuests++;
        } else {
            completedOtherQuests++;
        }
    }
}
