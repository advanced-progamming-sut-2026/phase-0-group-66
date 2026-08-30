package controller;

import model.AdventureFactory;
import model.Chapter;
import model.Game;
import model.GameState;
import model.Level;
import model.MeowPointTracker;
import model.News;
import model.User;
import network.client.PvzNetworkClient;
import view.GameView;

import java.io.IOException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

final class GameProgressHandler {
    private final AuthController authController;
    private final QuestController questController;
    private final AdventureFactory adventureFactory;
    private final GameView view;
    private final PvzNetworkClient networkClient;
    private final LinkedHashMap<String, Integer> syncedPlantKills = new LinkedHashMap<>();
    private boolean resultRecorded;
    private boolean scoredMode;
    private MeowPointTracker meowPointTracker;
    private LocalDate scoredGameDate;
    private int syncedSun;
    private int syncedKills;
    private int syncedExplosives;
    private int syncedMowerKills;
    private int syncedQuickKills;
    private int syncedFirstColumnKills;

    GameProgressHandler(AuthController authController, QuestController questController,
                            AdventureFactory adventureFactory, GameView view) {
        this(authController, questController, adventureFactory, view, null);
    }

    GameProgressHandler(AuthController authController, QuestController questController,
                            AdventureFactory adventureFactory, GameView view,
                            PvzNetworkClient networkClient) {
        this.authController = authController;
        this.questController = questController;
        this.adventureFactory = adventureFactory;
        this.view = view;
        this.networkClient = networkClient;
    }

    void beginAdventure() {
        scoredMode = false;
        meowPointTracker = null;
        scoredGameDate = null;
        reset();
    }

    void beginScored(LocalDate date) {
        scoredMode = true;
        meowPointTracker = new MeowPointTracker();
        scoredGameDate = date;
        reset();
    }

    String scoreStatus(Game game) {
        if (!scoredMode || meowPointTracker == null) {
            return "No scored game is running.";
        }
        meowPointTracker.update(game);
        return "Daily arena: " + scoredGameDate + "\n" + meowPointTracker.status();
    }

    boolean isScoredMode() {
        return scoredMode;
    }

    void synchronizeQuestProgress(Game game) {
        if (game == null) {
            return;
        }
        int sunDelta = game.getTotalSunCollected() - syncedSun;
        int killDelta = game.getZombieKillCount() - syncedKills;
        int explosiveDelta = game.getExplosivePlantsUsed() - syncedExplosives;
        int mowerDelta = game.getLawnMowerKills() - syncedMowerKills;
        int quickKillDelta = game.getKillsWithinThirtySeconds() - syncedQuickKills;
        int firstColumnDelta = game.getFirstColumnNoMowerKills() - syncedFirstColumnKills;
        questController.recordCombatProgress(game, sunDelta, killDelta,
            explosiveDelta, mowerDelta, plantKillDeltas(game), quickKillDelta, firstColumnDelta);
        syncedSun = game.getTotalSunCollected();
        syncedKills = game.getZombieKillCount();
        syncedExplosives = game.getExplosivePlantsUsed();
        syncedMowerKills = game.getLawnMowerKills();
        syncedQuickKills = game.getKillsWithinThirtySeconds();
        syncedFirstColumnKills = game.getFirstColumnNoMowerKills();
        syncedPlantKills.clear();
        syncedPlantKills.putAll(game.getPlantKillCounts());
        if (scoredMode && meowPointTracker != null) {
            meowPointTracker.update(game);
        }
    }

    void synchronizeSeenZombies(Game game) {
        User user = authController.getCurrentUser();
        if (user == null || game == null || game.getBoard() == null) {
            return;
        }
        for (String zombieName : game.getEncounteredZombieNames()) {
            if (user.getCollectionBook().unlockZombie(zombieName)) {
                user.addNews(new News("New zombie discovered",
                    zombieName + " was seen for the first time and added to Collection."));
            }
        }
    }

    void recordGameResultIfNeeded(Game game) {
        if (resultRecorded || game == null || !isFinished(game.getGameState())) {
            return;
        }
        User user = authController.getCurrentUser();
        if (user == null) {
            return;
        }
        resultRecorded = true;
        GameState state = game.getGameState();
        boolean won = state == GameState.WON;
        user.getProgress().recordGamePlayed();
        questController.recordLevelResult(game, user.getDifficultyLevel(), won);
        recordScoredResult(game, user);
        recordAdventureResult(game, user, won);
        saveAndShowResult(state);
    }

    private Map<String, Integer> plantKillDeltas(Game game) {
        LinkedHashMap<String, Integer> result = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : game.getPlantKillCounts().entrySet()) {
            int delta = entry.getValue() - syncedPlantKills.getOrDefault(entry.getKey(), 0);
            if (delta > 0) {
                result.put(entry.getKey(), delta);
            }
        }
        return result;
    }

    private boolean isFinished(GameState state) {
        return state == GameState.WON || state == GameState.LOST;
    }

    private void recordScoredResult(Game game, User user) {
        if (!scoredMode || meowPointTracker == null) {
            return;
        }
        meowPointTracker.finalizeScore(game);
        int score = meowPointTracker.getTotalScore();
        if (networkClient != null && networkClient.isAuthenticated()) {
            try {
                score = networkClient.submitScoredScore(user.getUsername(), score);
            } catch (IOException exception) {
                view.showMessage("Scored result was saved locally, but server sync failed: "
                    + exception.getMessage());
            }
        }
        user.getProgress().updateBestMeowPoints(score);
        user.getWallet().addCoins(Math.max(100, score / 20));
        user.addNews(new News("Scored game result",
            "Daily arena " + scoredGameDate + " finished with " + score + " meowpoints."));
    }

    private void recordAdventureResult(Game game, User user, boolean won) {
        if (!won || scoredMode) {
            return;
        }
        Level level = game.getCurrentLevel();
        Chapter chapter = game.getCurrentChapter();
        user.getProgress().completeLevel(level);
        if (chapter != null) {
            user.getProgress().recordCompletedLevel(chapter.getChapterNumber(),
                level.getLevelNumber());
        }
        unlockFollowingContent(user, chapter, level);
    }

    private void unlockFollowingContent(User user, Chapter chapter, Level level) {
        if (chapter == null || level == null) {
            return;
        }
        if (level.getLevelNumber() < 4) {
            chapter.findLevel(level.getLevelNumber() + 1).ifPresent(next -> {
                user.getProgress().unlockLevelId(next.getLevelId());
                user.addNews(new News("New level unlocked",
                    "Level " + next.getLevelNumber() + " of " + chapter.getName()
                        + " is now available."));
            });
            return;
        }
        int nextChapterIndex = chapter.getChapterNumber();
        if (nextChapterIndex >= adventureFactory.getChapters().size()) {
            return;
        }
        Chapter nextChapter = adventureFactory.getChapters().get(nextChapterIndex);
        user.getProgress().unlockChapterName(nextChapter.getName());
        nextChapter.findLevel(1)
            .ifPresent(next -> user.getProgress().unlockLevelId(next.getLevelId()));
        user.addNews(new News("New chapter unlocked",
            nextChapter.getName() + " is now available."));
    }

    private void saveAndShowResult(GameState state) {
        ActionResult saveResult = authController.saveCurrentState();
        if (!saveResult.isSuccessful()) {
            view.showMessage(saveResult.getMessage());
        }
        view.showGameOver(state == GameState.WON);
    }

    private void reset() {
        resultRecorded = false;
        syncedSun = 0;
        syncedKills = 0;
        syncedExplosives = 0;
        syncedMowerKills = 0;
        syncedQuickKills = 0;
        syncedFirstColumnKills = 0;
        syncedPlantKills.clear();
    }
}
