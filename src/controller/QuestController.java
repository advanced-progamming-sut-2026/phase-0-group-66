package controller;

import model.Board;
import model.Game;
import model.PlantDefinition;
import model.PlantFactory;
import model.QuestCategory;
import model.QuestDefinition;
import model.QuestEventType;
import model.QuestFactory;
import model.QuestPriority;
import model.QuestProgress;
import model.RewardType;
import model.SeasonType;
import model.User;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class QuestController {
    private final AuthController authController;
    private final QuestFactory questFactory;
    private final PlantFactory plantFactory;
    private final Random random = new Random();

    public QuestController(AuthController authController, QuestFactory questFactory,
                           PlantFactory plantFactory) {
        if (authController == null || questFactory == null || plantFactory == null) {
            throw new IllegalArgumentException("Quest controller dependencies cannot be null.");
        }
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
        if (!isValidPage(page)) {
            return List.of("Quest page must be ALL, MAIN, DAILY, or EPIC.");
        }
        ArrayList<QuestDefinition> definitions = new ArrayList<>();
        for (QuestDefinition definition : questFactory.getAllDefinitions()) {
            if (page.equals("ALL") || definition.getCategory().name().equals(page)) {
                definitions.add(definition);
            }
        }
        definitions.sort(Comparator.comparingInt(this::priorityRank)
            .thenComparingInt(QuestDefinition::getId));
        ArrayList<String> result = new ArrayList<>();
        for (QuestDefinition definition : definitions) {
            QuestProgress progress = user.getQuestLog().getProgress(definition);
            String status = progress.isRewardClaimed() ? "CLAIMED"
                : progress.isCompleted(definition.getTarget()) ? "READY_TO_CLAIM" : "ACTIVE";
            result.add(definition.getId() + ". " + definition.getTitle() + " ["
                + definition.getCategory() + "/" + definition.getPriority() + "] "
                + conditionDescription(definition) + " | " + progress.getProgress() + "/"
                + definition.getTarget() + " - " + status + " - reward: "
                + definition.getRewardAmount() + " " + definition.getRewardType());
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
        if (user == null || eventType == null || amount <= 0) {
            return;
        }
        for (QuestDefinition definition : questFactory.getAllDefinitions()) {
            if (definition.getEventType() == eventType && definition.getParameter().isBlank()) {
                user.getQuestLog().getProgress(definition).addProgress(amount,
                    definition.getTarget());
            }
        }
    }

    public void recordCombatProgress(Game game, int sunDelta, int killDelta,
                                     int explosiveDelta, int mowerDelta) {
        recordCombatProgress(game, sunDelta, killDelta, explosiveDelta, mowerDelta,
            Map.of(), 0, 0);
    }

    public void recordCombatProgress(Game game, int sunDelta, int killDelta,
                                     int explosiveDelta, int mowerDelta,
                                     Map<String, Integer> plantKillDeltas,
                                     int quickKillDelta, int firstColumnKillDelta) {
        User user = authController.getCurrentUser();
        if (user == null || game == null) {
            return;
        }
        for (QuestDefinition definition : questFactory.getAllDefinitions()) {
            QuestProgress progress = user.getQuestLog().getProgress(definition);
            switch (definition.getEventType()) {
                case SUN_COLLECTED -> progress.addProgress(sunDelta, definition.getTarget());
                case EXPLOSIVE_PLANT_USED -> progress.updateMaximum(
                    game.getExplosivePlantsUsed(), definition.getTarget());
                case LAWN_MOWER_KILL -> progress.addProgress(mowerDelta, definition.getTarget());
                case ZOMBIE_KILLED -> recordZombieQuest(definition, progress, game,
                    killDelta, plantKillDeltas, quickKillDelta, firstColumnKillDelta);
                default -> {
                    // Level and menu events are recorded by their dedicated methods.
                }
            }
        }
    }

    public void recordLevelWin(Game game, int difficultyLevel) {
        recordLevelResult(game, difficultyLevel, true);
    }

    public void recordLevelResult(Game game, int difficultyLevel, boolean won) {
        User user = authController.getCurrentUser();
        if (user == null || game == null) {
            return;
        }
        for (QuestDefinition definition : questFactory.getAllDefinitions()) {
            if (definition.getEventType() != QuestEventType.LEVEL_WON) {
                continue;
            }
            QuestProgress progress = user.getQuestLog().getProgress(definition);
            if (definition.getParameter().equals("DIFFICULTY_5")) {
                updateDifficultyStreak(progress, definition, difficultyLevel, won);
            } else if (won && levelConditionMatches(definition.getParameter(), game,
                difficultyLevel)) {
                progress.addProgress(1, definition.getTarget());
            }
        }
    }

    public QuestFactory getQuestFactory() { return questFactory; }

    public String getDailyBannedFamily() {
        List<String> families = plantFactory.getAllDefinitions().stream()
            .map(PlantDefinition::getCategory)
            .filter(value -> value != null && !value.isBlank())
            .distinct()
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .toList();
        if (families.isEmpty()) {
            return "UNKNOWN";
        }
        int index = Math.floorMod((int) LocalDate.now().toEpochDay(), families.size());
        return families.get(index);
    }

    public int getDailyEmptyColumn() {
        return dailyIndex(Board.DEFAULT_COLUMNS, 17) + 1;
    }

    public int getDailyEmptyRow() {
        return dailyIndex(Board.DEFAULT_ROWS, 31) + 1;
    }

    public int getDailyEmptyCrossIndex() {
        return dailyIndex(Math.min(Board.DEFAULT_ROWS, Board.DEFAULT_COLUMNS), 47) + 1;
    }

    private void recordZombieQuest(QuestDefinition definition, QuestProgress progress,
                                   Game game, int killDelta,
                                   Map<String, Integer> plantKillDeltas,
                                   int quickKillDelta, int firstColumnKillDelta) {
        if (killDelta <= 0 && plantKillDeltas.isEmpty()
            && quickKillDelta <= 0 && firstColumnKillDelta <= 0) {
            return;
        }
        String parameter = definition.getParameter();
        switch (parameter) {
            case "" -> progress.addProgress(killDelta, definition.getTarget());
            case "ANY_CHAPTER" -> progress.addBucketProgress(
                game.getCurrentLevel().getSeason().name(), killDelta, definition.getTarget());
            case "ANY_PLANT" -> recordAnyAttackingPlantKills(progress, definition,
                plantKillDeltas);
            case "CACTUS" -> progress.addProgress(
                plantKillDeltas.getOrDefault(PlantDefinition.normalizeKey("Cactus"), 0),
                definition.getTarget());
            case "TIME_LIMIT_30_SECONDS" -> progress.updateMaximum(
                game.getKillsWithinThirtySeconds(), definition.getTarget());
            case "FIRST_COLUMN_NO_MOWER" -> progress.addProgress(firstColumnKillDelta,
                definition.getTarget());
            default -> {
                // Unknown parameters do not receive accidental progress.
            }
        }
    }

    private void recordAnyAttackingPlantKills(QuestProgress progress,
                                               QuestDefinition definition,
                                               Map<String, Integer> plantKillDeltas) {
        for (Map.Entry<String, Integer> entry : plantKillDeltas.entrySet()) {
            PlantDefinition plant = plantFactory.findDefinition(entry.getKey()).orElse(null);
            if (plant != null && isAttackingPlant(plant)) {
                progress.addBucketProgress(plant.getNormalizedName(), entry.getValue(),
                    definition.getTarget());
            }
        }
    }

    private boolean isAttackingPlant(PlantDefinition plant) {
        String category = plant.getCategory();
        return plant.getBaseDamage() > 0 || plant.isInstantKill()
            || category.equalsIgnoreCase("Shooter")
            || category.equalsIgnoreCase("Lobber")
            || category.equalsIgnoreCase("Melee Attacker")
            || category.equalsIgnoreCase("Melee")
            || category.equalsIgnoreCase("Explosive")
            || category.equalsIgnoreCase("Strike-through")
            || category.equalsIgnoreCase("Homing");
    }

    private void updateDifficultyStreak(QuestProgress progress, QuestDefinition definition,
                                        int difficultyLevel, boolean won) {
        if (progress.isCompleted(definition.getTarget())) {
            return;
        }
        if (won && difficultyLevel == 5) {
            progress.addProgress(1, definition.getTarget());
        } else {
            progress.resetProgress();
        }
    }

    private boolean levelConditionMatches(String parameter, Game game, int difficultyLevel) {
        if (parameter.isBlank()) {
            return true;
        }
        return switch (parameter) {
            case "MAX_LOSSES_5" -> game.getLostPlantsCount() <= 5;
            case "FINAL_SUN_0" -> game.getSunAmount() == 0;
            case "SYMMETRIC_GARDEN" -> !game.getPlantedPlantNames().isEmpty()
                && game.getBoard().isHorizontallySymmetric();
            case "ASYMMETRIC_GARDEN" -> !game.getPlantedPlantNames().isEmpty()
                && !game.getBoard().isHorizontallySymmetric();
            case "DIFFICULTY_5" -> difficultyLevel == 5;
            case "EMPTY_COLUMN" -> game.getBoard().isColumnEmpty(getDailyEmptyColumn() - 1);
            case "EMPTY_ROW" -> game.getBoard().isRowEmpty(getDailyEmptyRow() - 1);
            case "EMPTY_CROSS" -> game.getBoard().isCrossEmpty(
                getDailyEmptyCrossIndex() - 1, getDailyEmptyCrossIndex() - 1);
            case "MUSHROOMS_ONLY" -> isDayLevel(game)
                && usedPlantsMatch(game, "Shroom");
            case "MAX_SUN_PRODUCERS_3" -> !game.getPlantedPlantNames().isEmpty()
                && game.getSunProducerPlantsPlanted() <= 3;
            case "SINGLE_FAMILY" -> killedOnlyWithOnePlantFamily(game);
            case "BANNED_FAMILY" -> !game.getPlantedPlantNames().isEmpty()
                && !usedFamily(game, getDailyBannedFamily());
            default -> false;
        };
    }

    private boolean isDayLevel(Game game) {
        return game.getCurrentLevel().getSeason() != SeasonType.DARK_AGES
            && game.getCurrentLevel().getRuleStrategy().allowsSkySun();
    }

    private boolean usedPlantsMatch(Game game, String tagOrCategory) {
        if (game.getPlantedPlantNames().isEmpty()) {
            return false;
        }
        for (String name : game.getPlantedPlantNames()) {
            PlantDefinition plant = plantFactory.findDefinition(name).orElse(null);
            if (plant == null || !(plant.hasTag(tagOrCategory)
                || plant.getCategory().equalsIgnoreCase(tagOrCategory))) {
                return false;
            }
        }
        return true;
    }

    private boolean killedOnlyWithOnePlantFamily(Game game) {
        int attributedKills = 0;
        ArrayList<String> families = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : game.getPlantKillCounts().entrySet()) {
            if (entry.getValue() == null || entry.getValue() <= 0) {
                continue;
            }
            PlantDefinition plant = plantFactory.findDefinition(entry.getKey()).orElse(null);
            if (plant == null) {
                return false;
            }
            attributedKills += entry.getValue();
            if (families.stream().noneMatch(value -> value.equalsIgnoreCase(
                plant.getCategory()))) {
                families.add(plant.getCategory());
            }
        }
        return attributedKills > 0 && attributedKills == game.getZombieKillCount()
            && families.size() == 1;
    }

    private boolean usedFamily(Game game, String family) {
        for (String used : game.getPlantedPlantFamilies()) {
            if (used.equalsIgnoreCase(family)) {
                return true;
            }
        }
        return false;
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
        user.ensureStarterContent();
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
            user.addNews(new model.News("New plant unlocked", name
                + " was unlocked by a quest."));
            return;
        }
        String fallback = randomOwnedPlant(user);
        if (fallback != null) {
            user.getInventory().addSeedPacket(fallback, 10);
        }
    }

    private String conditionDescription(QuestDefinition definition) {
        return switch (definition.getParameter()) {
            case "BANNED_FAMILY" -> definition.getDescription()
                + " (today's banned family: " + getDailyBannedFamily() + ")";
            case "EMPTY_COLUMN" -> definition.getDescription()
                + " (today's column: " + getDailyEmptyColumn() + ")";
            case "EMPTY_ROW" -> definition.getDescription()
                + " (today's row: " + getDailyEmptyRow() + ")";
            case "EMPTY_CROSS" -> definition.getDescription()
                + " (today's row and column: " + getDailyEmptyCrossIndex() + ")";
            default -> definition.getDescription();
        };
    }

    private int dailyIndex(int bound, int salt) {
        long day = LocalDate.now().toEpochDay();
        return Math.floorMod((int) (day + salt), bound);
    }

    private int priorityRank(QuestDefinition definition) {
        QuestPriority priority = definition.getPriority();
        return switch (priority) {
            case CRITICAL -> 0;
            case HIGH -> 1;
            case MEDIUM -> 2;
            case LOW -> 3;
        };
    }

    private boolean isValidPage(String page) {
        return page.equals("ALL") || page.equals("MAIN") || page.equals("DAILY")
            || page.equals("EPIC");
    }

    private String normalizePage(String pageName) {
        if (pageName == null || pageName.isBlank()) {
            return "ALL";
        }
        String normalized = pageName.trim().replace('-', '_').replace(' ', '_')
            .toUpperCase(Locale.ROOT);
        if (normalized.equals("STORY")) {
            return "MAIN";
        }
        return normalized;
    }
}
