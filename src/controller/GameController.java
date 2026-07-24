package controller;

import model.AdventureFactory;
import model.Board;
import model.Chapter;
import model.Game;
import model.GameData;
import model.GameState;
import model.DailyScoredLevelFactory;
import model.Level;
import model.PlantDefinition;
import model.User;
import model.Zombie;
import view.GameView;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.time.LocalDate;

public class GameController {
    private final AuthController authController;
    private final GameData gameData;
    private final AdventureFactory adventureFactory;
    private final GameView view;
    private final QuestController questController;
    private final GameProgressHandler progressHandler;
    private Game game;

    public GameController(AuthController authController, GameData gameData, GameView view,
                          QuestController questController) {
        if (authController == null || gameData == null || view == null || questController == null) {
            throw new IllegalArgumentException("Game controller dependencies cannot be null.");
        }
        this.authController = authController;
        this.gameData = gameData;
        this.view = view;
        this.questController = questController;
        this.adventureFactory = new AdventureFactory();
        this.progressHandler = new GameProgressHandler(authController, questController,
            adventureFactory, view);
    }

    public ActionResult startLevel(String chapterName) {
        return startLevel(chapterName, 1);
    }

    public ActionResult startLevel(String chapterName, int levelNumber) {
        User user = authController.getCurrentUser();
        if (user == null) {
            return ActionResult.failure("Login is required.");
        }
        user.ensureStarterContent();
        Optional<Chapter> chapterResult = adventureFactory.findChapter(chapterName);
        if (chapterResult.isEmpty()) {
            return ActionResult.failure("Chapter does not exist.");
        }
        Chapter chapter = chapterResult.get();
        Optional<Level> levelResult = chapter.findLevel(levelNumber);
        if (levelResult.isEmpty()) {
            return ActionResult.failure("Level number must be between 1 and 4.");
        }
        Level level = levelResult.get();
        if (!user.getProgress().isChapterUnlocked(chapter.getName())
            || !user.getProgress().isLevelUnlocked(level.getLevelId())) {
            return ActionResult.failure("This level is locked.");
        }
        Level playLevel = level.copyForPlay();
        restrictConveyorPoolToOwnedPlants(playLevel, user);
        game = new Game(gameData.getPlantFactory(), gameData.getZombieFactory(),
            user.getDifficultyLevel(), user.getCollectionBook().getPlantLevels(),
            user.getInventory(), user.getWallet());
        game.prepareLevel(chapter, playLevel);
        progressHandler.beginAdventure();
        flushEvents();
        return ActionResult.success("Choose up to " + level.getAllowedPlantCount()
            + " plants, then use 'start game'. Special rule: "
            + level.getSpecialRuleSummary());
    }

    public ActionResult startScoredGame() {
        return startScoredGame(LocalDate.now());
    }

    public ActionResult startScoredGame(LocalDate date) {
        User user = authController.getCurrentUser();
        if (user == null) {
            return ActionResult.failure("Login is required.");
        }
        user.ensureStarterContent();
        DailyScoredLevelFactory.ScoredLevel scored =
            new DailyScoredLevelFactory().create(date);
        game = new Game(gameData.getPlantFactory(), gameData.getZombieFactory(),
            user.getDifficultyLevel(), user.getCollectionBook().getPlantLevels(),
            user.getInventory(), user.getWallet(), scored.randomSeed());
        game.prepareLevel(scored.chapter(), scored.level());
        progressHandler.beginScored(scored.date());
        flushEvents();
        return ActionResult.success("Daily scored game for " + scored.date()
            + " is ready. Every user receives the same seeded wave pattern. "
            + "Choose up to " + scored.level().getAllowedPlantCount()
            + " plants, then use 'start game'.");
    }

    public String scoreStatus() {
        return progressHandler.scoreStatus(game);
    }

    public boolean isScoredMode() {
        return progressHandler.isScoredMode();
    }

    public ActionResult selectPlant(String plantType) {
        User user = authController.getCurrentUser();
        if (user == null) {
            return ActionResult.failure("Login is required.");
        }
        Optional<PlantDefinition> definition = gameData.getPlantFactory().findDefinition(plantType);
        if (definition.isEmpty()) {
            return ActionResult.failure("Plant does not exist.");
        }
        if (!user.getCollectionBook().getOwnedPlants().contains(definition.get().getName())) {
            return ActionResult.failure("Plant is locked and cannot be selected.");
        }
        return perform(() -> game.selectPlant(definition.get().getName()),
            "Plant selected: " + definition.get().getName() + ".");
    }

    public ActionResult removePlantSelection(String plantType) {
        return perform(() -> game.removeSelectedPlant(plantType), "Plant removed from selection.");
    }

    public ActionResult startGame() {
        ActionResult result = perform(() -> game.startGame(), "Game started.");
        if (result.isSuccessful()) {
            progressHandler.synchronizeSeenZombies(game);
            authController.saveCurrentState();
        }
        return result;
    }

    public ActionResult startZombieWaves() {
        ActionResult result = perform(() -> game.startZombieWaves(), "Zombie waves started.");
        if (result.isSuccessful()) {
            progressHandler.synchronizeSeenZombies(game);
            authController.saveCurrentState();
        }
        return result;
    }

    public String specialStatus() {
        return game == null ? "No level is prepared." : game.specialStatus();
    }

    public ActionResult boostPlant(String plantType) {
        User user = authController.getCurrentUser();
        if (user == null) {
            return ActionResult.failure("Login is required.");
        }
        if (game == null) {
            return ActionResult.failure("First choose a chapter and level.");
        }
        Optional<PlantDefinition> definition = gameData.getPlantFactory().findDefinition(plantType);
        if (definition.isEmpty()
            || !user.getCollectionBook().getOwnedPlants().contains(definition.get().getName())) {
            return ActionResult.failure("Plant is not owned.");
        }
        String canonicalName = definition.get().getName();
        if (!game.getSelectedPlants().contains(canonicalName)) {
            return ActionResult.failure("Select the plant before boosting it.");
        }
        if (game.isLevelBoosted(canonicalName)) {
            return ActionResult.failure("Plant is already boosted for this level.");
        }
        if (!user.getWallet().spendGems(2)) {
            return ActionResult.failure("Boosting a plant costs 2 gems.");
        }
        ActionResult boostResult = perform(() -> game.boostSelectedPlant(canonicalName),
            canonicalName + " will receive plant food whenever it is planted in this level.");
        if (!boostResult.isSuccessful()) {
            user.getWallet().addGems(2);
            return boostResult;
        }
        ActionResult save = authController.saveCurrentState();
        return save.isSuccessful() ? boostResult : save;
    }

    public ActionResult plantPlant(String plantType, int col, int row) {
        ActionResult result = perform(() -> {
            int internalRow = toRow(row);
            int internalColumn = toColumn(col);
            game.plant(plantType, internalRow, internalColumn);
        }, "Planting completed.");
        if (result.isSuccessful()) {
            progressHandler.synchronizeQuestProgress(game);
            authController.saveCurrentState();
        }
        return result;
    }

    public ActionResult pluckPlant(int col, int row) {
        return perform(() -> game.pluckPlant(toRow(row), toColumn(col)), "Plant removed.");
    }

    public ActionResult collectSun(int col, int row) {
        ActionResult result = perform(() -> game.collectSun(toRow(row), toColumn(col)),
            "Sun collected.");
        if (result.isSuccessful()) {
            progressHandler.synchronizeQuestProgress(game);
            progressHandler.recordGameResultIfNeeded(game);
            authController.saveCurrentState();
        }
        return result;
    }

    public ActionResult feedPlant(int col, int row) {
        ActionResult result = perform(() -> game.feedPlant(toRow(row), toColumn(col)),
            "Plant food used.");
        if (result.isSuccessful()) {
            progressHandler.synchronizeQuestProgress(game);
            authController.saveCurrentState();
        }
        return result;
    }

    public ActionResult addPlantFoodCheat() {
        ActionResult result = perform(() -> game.addPlantFoodCheat(),
            "Plant food cheat applied.");
        if (result.isSuccessful()) {
            authController.saveCurrentState();
        }
        return result;
    }

    public String plantFoodAmount() {
        if (game == null) {
            return "No level is prepared.";
        }
        return "Plant foods: " + game.getPlantFoodCount();
    }

    public ActionResult advanceTime(int ticks) {
        ActionResult result = perform(() -> game.advanceTime(ticks),
            "Advanced time by " + ticks + " tick(s).");
        if (result.isSuccessful()) {
            progressHandler.synchronizeSeenZombies(game);
            progressHandler.synchronizeQuestProgress(game);
            progressHandler.recordGameResultIfNeeded(game);
            authController.saveCurrentState();
        }
        return result;
    }

    public ActionResult addSuns(int count) {
        return perform(() -> game.addSun(count), "Sun cheat applied.");
    }

    public ActionResult removeCooldowns() {
        return perform(() -> game.removeAllCooldowns(), "Cooldown cheat applied.");
    }

    public ActionResult releaseNuke() {
        ActionResult result = perform(() -> game.releaseNuke(), "Nuke released.");
        if (result.isSuccessful()) {
            progressHandler.synchronizeQuestProgress(game);
            progressHandler.recordGameResultIfNeeded(game);
            authController.saveCurrentState();
        }
        return result;
    }

    public ActionResult spawnZombie(String zombieType, int col, int row) {
        ActionResult result = perform(() -> game.spawnZombie(zombieType, toRow(row), col - 1.0),
            "Zombie spawned.");
        if (result.isSuccessful()) {
            progressHandler.synchronizeSeenZombies(game);
            authController.saveCurrentState();
        }
        return result;
    }

    public ActionResult addWalletCurrency(int count, String type) {
        User user = authController.getCurrentUser();
        if (user == null) {
            return ActionResult.failure("Login is required.");
        }
        if (count < 0) {
            return ActionResult.failure("Count cannot be negative.");
        }
        if ("coin".equals(type)) {
            user.getWallet().addCoins(count);
        } else if ("diamond".equals(type)) {
            user.getWallet().addGems(count);
        } else {
            return ActionResult.failure("Currency must be coin or diamond.");
        }
        ActionResult saveResult = authController.saveCurrentState();
        return saveResult.isSuccessful()
            ? ActionResult.success("Wallet updated.") : saveResult;
    }

    public String walletAmount(String type) {
        User user = authController.getCurrentUser();
        if (user == null) {
            return "Login is required.";
        }
        if ("coin".equals(type)) {
            return "Coins: " + user.getWallet().getCoins();
        }
        return "Diamonds: " + user.getWallet().getGems();
    }

    public List<String> getChapterDescriptions() {
        User user = authController.getCurrentUser();
        ArrayList<String> result = new ArrayList<>();
        for (Chapter chapter : adventureFactory.getChapters()) {
            boolean unlocked = user != null
                && user.getProgress().isChapterUnlocked(chapter.getName());
            result.add(chapter.getName() + " - " + (unlocked ? "unlocked" : "locked"));
        }
        return List.copyOf(result);
    }

    public List<String> getLevelDescriptions(String chapterName) {
        Optional<Chapter> chapterResult = adventureFactory.findChapter(chapterName);
        if (chapterResult.isEmpty()) {
            return List.of("Chapter does not exist.");
        }
        User user = authController.getCurrentUser();
        ArrayList<String> result = new ArrayList<>();
        for (Level level : chapterResult.get().getLevels()) {
            boolean unlocked = user != null
                && user.getProgress().isLevelUnlocked(level.getLevelId());
            int firstCost = level.getWaves().get(0).getDifficultyCost();
            int lastCost = level.getWaves().get(level.getWaves().size() - 1).getDifficultyCost();
            result.add("Level " + level.getLevelNumber() + ": type=" + level.getSpecialType()
                + ", waves=" + level.getWaves().size() + ", waveCost=" + firstCost
                + ".." + lastCost + ", " + (unlocked ? "unlocked" : "locked"));
        }
        return List.copyOf(result);
    }

    public List<String> getAllPlants() {
        return gameData.getPlantFactory().getAllPlants();
    }

    public List<String> getAvailablePlants() {
        User user = authController.getCurrentUser();
        if (user == null) {
            return List.of();
        }
        user.ensureStarterContent();
        if (game == null) {
            return List.copyOf(user.getCollectionBook().getOwnedPlants());
        }
        return user.getCollectionBook().getOwnedPlants().stream()
            .filter(game::isPlantAvailableForSelection)
            .toList();
    }

    public List<String> getSelectedPlants() {
        return game == null ? List.of() : game.getSelectedPlants();
    }

    public void printMap() {
        if (game == null || game.getBoard() == null) {
            view.showMessage("No level is prepared.");
            return;
        }
        view.showGameSummary(game.summary());
        view.showMap(game.getBoard());
    }

    public void showPlantStatus() {
        if (game == null) {
            view.showMessage("No level is prepared.");
            return;
        }
        view.showText(game.plantStatus());
    }

    public void showTileStatus(int col, int row) {
        try {
            view.showText(game.tileStatus(toRow(row), toColumn(col)));
        } catch (IllegalArgumentException | IllegalStateException | IndexOutOfBoundsException exception) {
            view.showMessage(exception.getMessage());
        }
    }

    public void showZombieInfo() {
        if (game == null) {
            view.showMessage("No level is prepared.");
            return;
        }
        view.showText(game.zombieInfo());
    }

    public String sunAmount() {
        if (game == null) {
            return "No level is prepared.";
        }
        return "Sun amount: " + game.getSunAmount();
    }

    public Game getGame() {
        return game;
    }

    public boolean isGameFinished() {
        return game != null && (game.getGameState() == GameState.WON
            || game.getGameState() == GameState.LOST);
    }

    private void restrictConveyorPoolToOwnedPlants(Level level, User user) {
        if (!level.getRuleStrategy().usesConveyor()) {
            return;
        }
        List<String> owned = level.getConveyorPlants().stream()
            .filter(user.getCollectionBook().getOwnedPlants()::contains)
            .toList();
        if (owned.isEmpty()) {
            owned = user.getCollectionBook().getOwnedPlants().stream()
                .filter(name -> gameData.getPlantFactory().findDefinition(name).isPresent())
                .limit(level.getAllowedPlantCount())
                .toList();
        }
        level.configureConveyorPlants(owned);
    }

    private ActionResult perform(GameAction action, String successMessage) {
        if (game == null) {
            return ActionResult.failure("First choose a chapter and level.");
        }
        try {
            action.run();
            flushEvents();
            return ActionResult.success(successMessage);
        } catch (IllegalArgumentException | IllegalStateException | IndexOutOfBoundsException exception) {
            return ActionResult.failure(exception.getMessage());
        }
    }

    private void flushEvents() {
        if (game == null) {
            return;
        }
        for (String event : game.drainEvents()) {
            view.showMessage(event);
        }
    }

    private int toRow(int commandRow) {
        if (commandRow < 1 || commandRow > Board.DEFAULT_ROWS) {
            throw new IllegalArgumentException("Row must be between 1 and 5.");
        }
        return commandRow - 1;
    }

    private int toColumn(int commandColumn) {
        if (commandColumn < 1 || commandColumn > Board.DEFAULT_COLUMNS) {
            throw new IllegalArgumentException("Column must be between 1 and 9.");
        }
        return commandColumn - 1;
    }

    @FunctionalInterface
    private interface GameAction {
        void run();
    }
}
