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
    private int completedMiniGames;
    private int lastChapterNumber;
    private int lastLevelNumber;
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

    public int getCompletedMiniGames() { return completedMiniGames; }
    public int getLastChapterNumber() { return lastChapterNumber; }
    public int getLastLevelNumber() { return lastLevelNumber; }

    public Set<String> getUnlockedChapters() {
        return Collections.unmodifiableSet(unlockedChapters);
    }

    public Set<String> getUnlockedLevels() {
        return Collections.unmodifiableSet(unlockedLevels);
    }


    public void unlockChapterName(String chapterName) {
        if (chapterName != null && !chapterName.isBlank()) {
            unlockedChapters.add(chapterName.trim());
        }
    }

    public void unlockLevelId(String levelId) {
        if (levelId != null && !levelId.isBlank()) {
            unlockedLevels.add(levelId.trim());
        }
    }

    public boolean isChapterUnlocked(String chapterName) {
        return chapterName != null && unlockedChapters.contains(chapterName);
    }

    public boolean isLevelUnlocked(String levelId) {
        return levelId != null && unlockedLevels.contains(levelId);
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

    public void recordCompletedMiniGame(int score) {
        completedMiniGames++;
        updateBestScore(score);
    }

    public void recordCompletedLevel(int chapterNumber, int levelNumber) {
        if (chapterNumber > lastChapterNumber
            || (chapterNumber == lastChapterNumber && levelNumber > lastLevelNumber)) {
            lastChapterNumber = chapterNumber;
            lastLevelNumber = levelNumber;
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
