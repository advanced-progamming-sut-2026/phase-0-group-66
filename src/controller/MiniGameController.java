package controller;

import model.MiniGameDefinition;
import model.MiniGameSession;
import model.MiniGameSessionFactory;
import model.MiniGameType;
import model.QuestEventType;
import model.User;
import network.client.NetworkIZombieSession;
import network.client.PvzNetworkClient;
import network.game.MatchRole;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MiniGameController {
    private final AuthController authController;
    private final QuestController questController;
    private final PvzNetworkClient networkClient;
    private final Map<MiniGameType, MiniGameDefinition> definitions = new LinkedHashMap<>();
    private MiniGameSession currentSession;
    private boolean rewardRecorded;

    public MiniGameController(AuthController authController, QuestController questController) {
        this(authController, questController, null);
    }

    public MiniGameController(AuthController authController, QuestController questController,
                              PvzNetworkClient networkClient) {
        this.authController = authController;
        this.questController = questController;
        this.networkClient = networkClient;
        registerDefaults();
    }

    public List<String> showMiniGamesStatus() {
        User user = authController.getCurrentUser();
        if (user == null) {
            return List.of("Login is required.");
        }
        ArrayList<String> result = new ArrayList<>();
        for (MiniGameDefinition definition : definitions.values()) {
            for (int level = 1; level <= 3; level++) {
                String state = user.getProgress().isMiniGameLevelCompleted(
                    definition.type(), level) ? "COMPLETED"
                    : user.getProgress().isMiniGameLevelUnlocked(definition.type(), level)
                    ? "UNLOCKED" : "LOCKED";
                result.add(definition.type() + " level " + level + " [" + state + "] - "
                    + definition.objective());
            }
        }
        return List.copyOf(result);
    }

    public ActionResult startMiniGame(String miniGameName, int level) {
        if (authController.getCurrentUser() == null) {
            return ActionResult.failure("Login is required.");
        }
        try {
            MiniGameType type = MiniGameType.fromText(miniGameName);
            MiniGameDefinition definition = definitions.get(type);
            if (definition == null) {
                return ActionResult.failure("Mini-game does not exist.");
            }
            User user = authController.getCurrentUser();
            if (!user.getProgress().isMiniGameLevelUnlocked(type, level)) {
                return ActionResult.failure(type + " level " + level + " is locked.");
            }
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

    public ActionResult startOnlineIZombie(int level, String matchId, MatchRole role,
                                           String opponent) {
        if (authController.getCurrentUser() == null) {
            return ActionResult.failure("Login is required.");
        }
        if (networkClient == null) {
            return ActionResult.failure("Network play is disabled.");
        }
        MiniGameDefinition definition = definitions.get(MiniGameType.I_ZOMBIE);
        try {
            currentSession = new NetworkIZombieSession(definition, level, networkClient,
                authController.getCurrentUser().getUsername(), matchId, role, opponent);
            rewardRecorded = false;
            return ActionResult.success("Online I, Zombie match started as " + role + ".");
        } catch (Exception exception) {
            return ActionResult.failure("Could not join the online match: " + exception.getMessage());
        }
    }

    public ActionResult advanceTime(int ticks) {
        return executeCommand("advance " + ticks);
    }

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
        MiniGameType type = currentSession.getDefinition().type();
        int level = currentSession.getLevel();
        int score = currentSession.getScore();
        boolean firstCompletion = user.getProgress().completeMiniGameLevel(type, level, score);
        if (firstCompletion) {
            user.getWallet().addCoins(250 * level);
            questController.recordEvent(QuestEventType.MINI_GAME_WON, 1);
            user.addNews(new model.News("Mini-game completed",
                type + " level " + level + " was completed with score " + score + "."));
            unlockNextMiniGameContent(user, type, level);
        }
        authController.saveCurrentState();
    }

    private void unlockNextMiniGameContent(User user, MiniGameType type, int level) {
        if (level < 3) {
            int nextLevel = level + 1;
            if (user.getProgress().unlockMiniGameLevel(type, nextLevel)) {
                user.addNews(new model.News("New mini-game level unlocked",
                    type + " level " + nextLevel + " is now available."));
            }
            return;
        }
        MiniGameType[] order = MiniGameType.values();
        int nextIndex = type.ordinal() + 1;
        if (nextIndex < order.length) {
            MiniGameType nextType = order[nextIndex];
            if (user.getProgress().unlockMiniGameLevel(nextType, 1)) {
                user.addNews(new model.News("New mini-game unlocked",
                    nextType + " level 1 is now available in Travel Log."));
            }
        }
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
            case I_ZOMBIE -> "Commands: deploy <level-card> <row>; advance <ticks>. "
                + "Use board to see the five cards for the selected level.";
            case BEGHOULD -> "Commands: swap <x1> <y1> <x2> <y2>; upgrade <plant>; advance <ticks>.";
            case ZOMBOTANY -> "Commands: select <plant>; remove <plant>; start; "
                + "plant <type> <x> <y>; collect <x> <y>; feed <x> <y>; advance <ticks>.";
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
