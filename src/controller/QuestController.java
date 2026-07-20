package controller;

import model.Game;
import model.PlantDefinition;
import model.PlantFactory;
import model.QuestCategory;
import model.QuestDefinition;
import model.QuestEventType;
import model.QuestFactory;
import model.QuestProgress;
import model.RewardType;
import model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class QuestController {
    private final AuthController authController;
    private final QuestFactory questFactory;
    private final PlantFactory plantFactory;
    private final Random random = new Random();

    public QuestController(AuthController authController, QuestFactory questFactory,
                           PlantFactory plantFactory) {
        this.authController = authController;
        this.questFactory = questFactory;
        this.plantFactory = plantFactory;
    }

    public List<String> getQuestsPage(String pageName) {
        User user = authController.getCurrentUser();
        if (user == null) {
            return List.of("Login is required.");
        }
        String page = normalizePage(pageName);
        ArrayList<String> result = new ArrayList<>();
        for (QuestDefinition definition : questFactory.getAllDefinitions()) {
            if (!page.equals("ALL") && !definition.getCategory().name().equals(page)) {
                continue;
            }
            QuestProgress progress = user.getQuestLog().getProgress(definition);
            String status = progress.isRewardClaimed() ? "CLAIMED"
                : progress.isCompleted(definition.getTarget()) ? "READY_TO_CLAIM" : "ACTIVE";
            result.add(definition.getId() + ". " + definition.getTitle() + " ["
                + definition.getCategory() + "/" + definition.getPriority() + "] "
                + progress.getProgress() + "/" + definition.getTarget() + " - " + status
                + " - reward: " + definition.getRewardAmount() + " "
                + definition.getRewardType());
        }
        return List.copyOf(result);
    }

    public ActionResult claimReward(int questId) {
        User user = authController.getCurrentUser();
        if (user == null) {
            return ActionResult.failure("Login is required.");
        }
        QuestDefinition definition = questFactory.findDefinition(questId).orElse(null);
        if (definition == null) {
            return ActionResult.failure("Quest does not exist.");
        }
        QuestProgress progress = user.getQuestLog().getProgress(definition);
        if (!progress.isCompleted(definition.getTarget())) {
            return ActionResult.failure("Quest is not completed.");
        }
        if (progress.isRewardClaimed()) {
            return ActionResult.failure("Quest reward was already claimed.");
        }
        applyReward(user, definition);
        progress.claim();
        user.getProgress().recordCompletedQuest(definition.getCategory() == QuestCategory.DAILY);
        user.addNews(new model.News("Quest completed", definition.getTitle()
            + " reward was claimed."));
        ActionResult save = authController.saveCurrentState();
        return save.isSuccessful() ? ActionResult.success("Quest reward claimed.") : save;
    }

    public void recordEvent(QuestEventType eventType, int amount) {
        User user = authController.getCurrentUser();
        if (user == null || amount <= 0) {
            return;
        }
        for (QuestDefinition definition : questFactory.getAllDefinitions()) {
            if (definition.getEventType() == eventType && definition.getParameter().isBlank()) {
                user.getQuestLog().getProgress(definition).addProgress(amount, definition.getTarget());
            }
        }
    }

    public void recordCombatProgress(Game game, int sunDelta, int killDelta,
                                     int explosiveDelta, int mowerDelta) {
        User user = authController.getCurrentUser();
        if (user == null || game == null) {
            return;
        }
        for (QuestDefinition definition : questFactory.getAllDefinitions()) {
            int amount = switch (definition.getEventType()) {
                case SUN_COLLECTED -> sunDelta;
                case EXPLOSIVE_PLANT_USED -> explosiveDelta;
                case LAWN_MOWER_KILL -> mowerDelta;
                case ZOMBIE_KILLED -> eligibleZombieKills(definition, game, killDelta);
                default -> 0;
            };
            if (amount > 0) {
                user.getQuestLog().getProgress(definition)
                    .addProgress(amount, definition.getTarget());
            }
        }
    }

    public void recordLevelWin(Game game, int difficultyLevel) {
        User user = authController.getCurrentUser();
        if (user == null || game == null) {
            return;
        }
        for (QuestDefinition definition : questFactory.getAllDefinitions()) {
            if (definition.getEventType() == QuestEventType.LEVEL_WON
                && levelConditionMatches(definition.getParameter(), game, difficultyLevel)) {
                user.getQuestLog().getProgress(definition)
                    .addProgress(1, definition.getTarget());
            }
        }
    }

    public QuestFactory getQuestFactory() {
        return questFactory;
    }

    private int eligibleZombieKills(QuestDefinition definition, Game game, int killDelta) {
        if (killDelta <= 0) {
            return 0;
        }
        String parameter = definition.getParameter();
        if (parameter.isBlank() || parameter.equals("ANY_CHAPTER")
            || parameter.equals("ANY_PLANT")) {
            return killDelta;
        }
        if (parameter.equals("CACTUS")) {
            return game.getSelectedPlants().size() == 1
                && game.getSelectedPlants().contains("Cactus") ? killDelta : 0;
        }
        if (parameter.equals("TIME_LIMIT_30_SECONDS")) {
            return game.getElapsedTicks() <= 30 * Game.TICKS_PER_SECOND ? killDelta : 0;
        }
        return 0;
    }

    private boolean levelConditionMatches(String parameter, Game game, int difficultyLevel) {
        if (parameter.isBlank()) {
            return true;
        }
        return switch (parameter) {
            case "MAX_LOSSES_5" -> game.getLostPlantsCount() <= 5;
            case "FINAL_SUN_0" -> game.getSunAmount() == 0;
            case "SYMMETRIC_GARDEN" -> game.getBoard().isHorizontallySymmetric();
            case "ASYMMETRIC_GARDEN" -> !game.getBoard().isHorizontallySymmetric();
            case "DIFFICULTY_5" -> difficultyLevel == 5;
            case "EMPTY_COLUMN" -> game.getBoard().hasEmptyColumn();
            case "EMPTY_ROW" -> game.getBoard().hasEmptyRow();
            case "EMPTY_CROSS" -> game.getBoard().hasEmptyCross();
            case "MUSHROOMS_ONLY" -> selectedPlantsMatch(game, "Shroom", Integer.MAX_VALUE);
            case "MAX_SUN_PRODUCERS_3" -> selectedPlantsMatch(game, "Sun Producer", 3);
            case "SINGLE_FAMILY" -> selectedPlantCategories(game) <= 1;
            case "BANNED_FAMILY" -> true;
            default -> true;
        };
    }

    private boolean selectedPlantsMatch(Game game, String value, int maximumCount) {
        if (game.getSelectedPlants().isEmpty() || game.getSelectedPlants().size() > maximumCount) {
            return false;
        }
        for (String name : game.getSelectedPlants()) {
            PlantDefinition plant = plantFactory.findDefinition(name).orElse(null);
            if (plant == null || !(plant.hasTag(value)
                || plant.getCategory().equalsIgnoreCase(value))) {
                return false;
            }
        }
        return true;
    }

    private int selectedPlantCategories(Game game) {
        return (int) game.getSelectedPlants().stream()
            .map(name -> plantFactory.findDefinition(name).map(PlantDefinition::getCategory)
                .orElse("UNKNOWN"))
            .distinct().count();
    }

    private void applyReward(User user, QuestDefinition definition) {
        int amount = definition.getRewardAmount();
        RewardType type = definition.getRewardType();
        if (type == RewardType.COINS) {
            user.getWallet().addCoins(amount);
        } else if (type == RewardType.GEMS) {
            user.getWallet().addGems(amount);
        } else if (type == RewardType.SEED_PACKETS) {
            String plant = randomOwnedPlant(user);
            if (plant != null) {
                user.getInventory().addSeedPacket(plant, amount);
            }
        } else if (type == RewardType.RANDOM_PLANT) {
            unlockRandomPlant(user);
        }
    }

    private String randomOwnedPlant(User user) {
        List<String> plants = List.copyOf(user.getCollectionBook().getOwnedPlants());
        return plants.isEmpty() ? null : plants.get(random.nextInt(plants.size()));
    }

    private void unlockRandomPlant(User user) {
        List<PlantDefinition> locked = plantFactory.getAllDefinitions().stream()
            .filter(plant -> !user.getCollectionBook().getOwnedPlants().contains(plant.getName()))
            .toList();
        if (!locked.isEmpty()) {
            String name = locked.get(random.nextInt(locked.size())).getName();
            user.getCollectionBook().unlockPlant(name);
            user.addNews(new model.News("New plant unlocked", name + " was unlocked by a quest."));
        }
    }

    private String normalizePage(String pageName) {
        if (pageName == null) {
            return "";
        }
        String normalized = pageName.trim().replace('-', '_').replace(' ', '_')
            .toUpperCase(Locale.ROOT);
        if (normalized.equals("STORY")) {
            return "MAIN";
        }
        return normalized;
    }
}
