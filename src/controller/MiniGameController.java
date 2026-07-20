package controller;

import model.MiniGameDefinition;
import model.MiniGameSession;
import model.MiniGameType;
import model.QuestEventType;
import model.User;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MiniGameController {
    private final AuthController authController;
    private final QuestController questController;
    private final Map<MiniGameType, MiniGameDefinition> definitions = new LinkedHashMap<>();
    private MiniGameSession currentSession;

    public MiniGameController(AuthController authController, QuestController questController) {
        this.authController = authController;
        this.questController = questController;
        registerDefaults();
    }

    public List<String> showMiniGamesStatus() {
        return definitions.values().stream().map(MiniGameDefinition::toString).toList();
    }

    public ActionResult startMiniGame(String miniGameName, int level) {
        if (authController.getCurrentUser() == null) {
            return ActionResult.failure("Login is required.");
        }
        try {
            MiniGameType type = MiniGameType.fromText(miniGameName);
            MiniGameDefinition definition = definitions.get(type);
            currentSession = new MiniGameSession(definition, level);
            return ActionResult.success("Started " + type + " level " + level
                + ". Use action: " + definition.actionName() + ".");
        } catch (IllegalArgumentException exception) {
            return ActionResult.failure(exception.getMessage());
        }
    }

    public ActionResult performAction(String action, int amount) {
        if (currentSession == null) {
            return ActionResult.failure("Start a mini-game first.");
        }
        try {
            boolean wasWon = currentSession.isWon();
            currentSession.perform(action, amount);
            if (!wasWon && currentSession.isWon()) {
                finishCurrentSession();
            }
            return ActionResult.success(currentSession.status());
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return ActionResult.failure(exception.getMessage());
        }
    }

    public String currentStatus() {
        return currentSession == null ? "No mini-game is running." : currentSession.status();
    }

    private void finishCurrentSession() {
        User user = authController.getCurrentUser();
        int score = currentSession.getScore();
        user.getProgress().recordCompletedMiniGame(score);
        user.getProgress().updateBestMeowPoints(score);
        user.getWallet().addCoins(250 * currentSession.getLevel());
        questController.recordEvent(QuestEventType.MINI_GAME_WON, 1);
        user.addNews(new model.News("Mini-game completed",
            currentSession.getDefinition().type() + " level " + currentSession.getLevel()
                + " was completed with score " + score + "."));
        authController.saveCurrentState();
    }

    private void registerDefaults() {
        register(MiniGameType.VASEBREAKER, false,
            "Break every vase while surviving the released zombies.", "break-vase");
        register(MiniGameType.WALLNUT_BOWLING, false,
            "Defeat zombies with normal, explosive, and giant wall-nuts.", "roll-wallnut");
        register(MiniGameType.I_ZOMBIE, false,
            "Use zombies to eat all five brains with limited sun.", "eat-brain");
        register(MiniGameType.BEGHOULD, true,
            "Create enough three-or-more plant matches.", "make-match");
        register(MiniGameType.ZOMBOTANY, true,
            "Defeat plant-powered zombies in an endless-wave battle.", "defeat-zombie");
    }

    private void register(MiniGameType type, boolean bonus, String objective, String action) {
        definitions.put(type, new MiniGameDefinition(type, bonus, objective, action));
    }
}
