package controller;

import model.MiniGameDefinition;
import model.MiniGameSession;
import model.MiniGameSessionFactory;
import model.MiniGameType;
import model.QuestEventType;
import model.User;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MiniGameController {
    private final AuthController authController;
    private final QuestController questController;
    private final Map<MiniGameType, MiniGameDefinition> definitions = new LinkedHashMap<>();
    private MiniGameSession currentSession;
    private boolean rewardRecorded;

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
            currentSession = MiniGameSessionFactory.create(definition, level);
            rewardRecorded = false;
            return ActionResult.success("Started " + type + " level " + level + ". "
                + commandHelp(type));
        } catch (IllegalArgumentException exception) {
            return ActionResult.failure(exception.getMessage());
        }
    }

    public ActionResult executeCommand(String commandLine) {
        if (currentSession == null) {
            return ActionResult.failure("Start a mini-game first.");
        }
        if (commandLine == null || commandLine.isBlank()) {
            return ActionResult.failure("Mini-game command cannot be empty.");
        }
        try {
            List<String> tokens = tokenize(commandLine.trim());
            String command = tokens.get(0);
            currentSession.execute(command, tokens.subList(1, tokens.size()));
            finishCurrentSessionIfNeeded();
            return ActionResult.success(currentSession.status());
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return ActionResult.failure(exception.getMessage());
        }
    }

    public ActionResult advanceTime(int ticks) {
        return executeCommand("advance " + ticks);
    }

    /** Backward-compatible API, now dispatching to real mini-game commands. */
    public ActionResult performAction(String action, int amount) {
        if (currentSession == null) {
            return ActionResult.failure("Start a mini-game first.");
        }
        try {
            currentSession.perform(action, amount);
            finishCurrentSessionIfNeeded();
            return ActionResult.success(currentSession.status());
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return ActionResult.failure(exception.getMessage());
        }
    }

    public String currentStatus() {
        return currentSession == null ? "No mini-game is running." : currentSession.status();
    }

    public String currentBoard() {
        return currentSession == null ? "No mini-game is running." : currentSession.boardView();
    }

    public String currentHelp() {
        return currentSession == null ? "Start a mini-game first."
            : commandHelp(currentSession.getDefinition().type());
    }

    public MiniGameSession getCurrentSession() { return currentSession; }

    private void finishCurrentSessionIfNeeded() {
        if (currentSession == null || rewardRecorded || !currentSession.isFinished()) {
            return;
        }
        rewardRecorded = true;
        if (!currentSession.isWon()) {
            authController.saveCurrentState();
            return;
        }
        User user = authController.getCurrentUser();
        int score = currentSession.getScore();
        user.getProgress().recordCompletedMiniGame(score);
        user.getWallet().addCoins(250 * currentSession.getLevel());
        questController.recordEvent(QuestEventType.MINI_GAME_WON, 1);
        user.addNews(new model.News("Mini-game completed",
            currentSession.getDefinition().type() + " level " + currentSession.getLevel()
                + " was completed with score " + score + "."));
        authController.saveCurrentState();
    }

    private void registerDefaults() {
        register(MiniGameType.VASEBREAKER, false,
            "Break every vase, use temporary plant packets, and survive released zombies.",
            "break");
        register(MiniGameType.WALLNUT_BOWLING, false,
            "Use conveyor-delivered normal, explosive, and giant wall-nuts.", "bowl");
        register(MiniGameType.I_ZOMBIE, false,
            "Spend sun on zombies and eat all five brains.", "deploy");
        register(MiniGameType.BEGHOULD, true,
            "Swap adjacent plants to create matches while zombies attack.", "swap");
        register(MiniGameType.ZOMBOTANY, true,
            "Defeat zombies carrying Peashooter, Wall-nut, Jalapeno, and Squash powers.",
            "plant");
    }

    private void register(MiniGameType type, boolean bonus, String objective, String action) {
        definitions.put(type, new MiniGameDefinition(type, bonus, objective, action));
    }

    private String commandHelp(MiniGameType type) {
        return switch (type) {
            case VASEBREAKER -> "Commands: break <x> <y>; plant <packetId> <x> <y>; advance <ticks>.";
            case WALLNUT_BOWLING -> "Commands: bowl <normal|explosive|giant> <row>; advance <ticks>.";
            case I_ZOMBIE -> "Commands: deploy <basic|cone|bucket|imp|allstar> <row>; advance <ticks>.";
            case BEGHOULD -> "Commands: swap <x1> <y1> <x2> <y2>; upgrade <plant>; advance <ticks>.";
            case ZOMBOTANY -> "Commands: plant <type> <x> <y>; advance <ticks>.";
        };
    }

    private List<String> tokenize(String commandLine) {
        ArrayList<String> tokens = new ArrayList<>();
        for (String token : commandLine.split("\\s+")) {
            if (!token.isBlank()) {
                tokens.add(token);
            }
        }
        return tokens;
    }
}
