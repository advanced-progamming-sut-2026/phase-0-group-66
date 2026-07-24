package model;

import java.io.IOException;
import java.io.ObjectInputStream;
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
    private LinkedHashSet<String> unlockedChapters;
    private LinkedHashSet<String> unlockedLevels;
    private LinkedHashSet<String> completedLevelIds;
    private LinkedHashSet<String> unlockedMiniGameLevels;
    private LinkedHashSet<String> completedMiniGameLevels;

    public GameProgress() {
        initializeCollections();
    }

    public int getGamesPlayed() { return gamesPlayed; }
    public int getCompletedLevels() { return completedLevelIds.size(); }
    public int getBestMiniGameScore() { return bestMiniGameScore; }
    public int getBestMeowPoints() { return bestMeowPoints; }
    public int getCompletedDailyQuests() { return completedDailyQuests; }
    public int getCompletedOtherQuests() { return completedOtherQuests; }
    public int getCompletedMiniGames() { return completedMiniGames; }
    public int getLastChapterNumber() { return lastChapterNumber; }
    public int getLastLevelNumber() { return lastLevelNumber; }

    public Set<String> getUnlockedChapters() {
        return Collections.unmodifiableSet(unlockedChapters);
    }

    public Set<String> getUnlockedLevels() {
        return Collections.unmodifiableSet(unlockedLevels);
    }

    public Set<String> getUnlockedMiniGameLevels() {
        return Collections.unmodifiableSet(unlockedMiniGameLevels);
    }

    public Set<String> getCompletedMiniGameLevels() {
        return Collections.unmodifiableSet(completedMiniGameLevels);
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

    public boolean unlockMiniGameLevel(MiniGameType type, int level) {
        validateMiniGameLevel(type, level);
        return unlockedMiniGameLevels.add(miniGameKey(type, level));
    }

    public boolean isMiniGameLevelUnlocked(MiniGameType type, int level) {
        validateMiniGameLevel(type, level);
        return unlockedMiniGameLevels.contains(miniGameKey(type, level));
    }

    public boolean isMiniGameLevelCompleted(MiniGameType type, int level) {
        validateMiniGameLevel(type, level);
        return completedMiniGameLevels.contains(miniGameKey(type, level));
    }

    public boolean completeMiniGameLevel(MiniGameType type, int level, int score) {
        validateMiniGameLevel(type, level);
        updateBestScore(score);
        String key = miniGameKey(type, level);
        unlockedMiniGameLevels.add(key);
        boolean firstCompletion = completedMiniGameLevels.add(key);
        if (firstCompletion) {
            completedMiniGames++;
        }
        return firstCompletion;
    }

    public void recordGamePlayed() { gamesPlayed++; }

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

    /** Legacy compatibility for older callers that did not identify the mini-game level. */
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

    private void readObject(ObjectInputStream input) throws IOException, ClassNotFoundException {
        input.defaultReadObject();
        initializeCollections();
        completedMiniGames = Math.max(completedMiniGames, completedMiniGameLevels.size());
    }

    private void initializeCollections() {
        if (unlockedChapters == null) {
            unlockedChapters = new LinkedHashSet<>();
        }
        if (unlockedLevels == null) {
            unlockedLevels = new LinkedHashSet<>();
        }
        if (completedLevelIds == null) {
            completedLevelIds = new LinkedHashSet<>();
        }
        if (unlockedMiniGameLevels == null) {
            unlockedMiniGameLevels = new LinkedHashSet<>();
        }
        if (completedMiniGameLevels == null) {
            completedMiniGameLevels = new LinkedHashSet<>();
        }
    }

    private void validateMiniGameLevel(MiniGameType type, int level) {
        if (type == null || level < 1 || level > 3) {
            throw new IllegalArgumentException("Mini-game type and level 1-3 are required.");
        }
    }

    private String miniGameKey(MiniGameType type, int level) {
        return type.name() + ":" + level;
    }
}
