package controller;

import model.Game;
import model.PlantDefinition;
import model.PlantFactory;
import model.QuestDefinition;
import model.QuestProgress;

import java.util.List;
import java.util.Map;

final class QuestCombatProgressRecorder {
    private final PlantFactory plantFactory;

    QuestCombatProgressRecorder(PlantFactory plantFactory) {
        this.plantFactory = plantFactory;
    }

    void recordZombieQuest(QuestDefinition definition, QuestProgress progress,
                           Game game, int killDelta,
                           Map<String, Integer> plantKillDeltas,
                           int quickKillDelta, int firstColumnKillDelta) {
        String parameter = definition.getParameter();
        if (parameter.equals("ANY_PLANT")) {
            recordSinglePlantOnlyKills(progress, definition, game, killDelta, plantKillDeltas);
            return;
        }
        if (parameter.equals("CACTUS")) {
            recordCactusOnlyKills(progress, definition, game, killDelta, plantKillDeltas);
            return;
        }
        if (killDelta <= 0 && plantKillDeltas.isEmpty()
            && quickKillDelta <= 0 && firstColumnKillDelta <= 0) {
            return;
        }
        switch (parameter) {
            case "" -> progress.addProgress(killDelta, definition.getTarget());
            case "ANY_CHAPTER" -> progress.addBucketProgress(
                game.getCurrentLevel().getSeason().name(), killDelta, definition.getTarget());
            case "TIME_LIMIT_30_SECONDS" -> progress.updateMaximum(
                game.getKillsWithinThirtySeconds(), definition.getTarget());
            case "FIRST_COLUMN_NO_MOWER" -> progress.addProgress(firstColumnKillDelta,
                definition.getTarget());
            default -> {
            }
        }
    }

    private void recordSinglePlantOnlyKills(QuestProgress progress,
                                             QuestDefinition definition,
                                             Game game,
                                             int totalKillDelta,
                                             Map<String, Integer> plantKillDeltas) {
        if (progress.isCompleted(definition.getTarget())) {
            return;
        }
        String usedPlantKey = onlyHistoricallyUsedPlant(game);
        if (usedPlantKey == null) {
            if (!game.getPlantedPlantNames().isEmpty()) {
                progress.resetProgress();
            }
            return;
        }
        PlantDefinition usedPlant = plantFactory.findDefinition(usedPlantKey).orElse(null);
        if (usedPlant == null || !isAttackingPlant(usedPlant)) {
            progress.resetProgress();
            return;
        }
        if (totalKillDelta <= 0 && plantKillDeltas.isEmpty()) {
            return;
        }
        Map.Entry<String, Integer> onlySource = singleValidPlantSource(plantKillDeltas);
        int attributedKills = sumPositiveKills(plantKillDeltas);
        if (onlySource == null || attributedKills != totalKillDelta
            || !PlantDefinition.normalizeKey(onlySource.getKey()).equals(usedPlantKey)
            || conflictsWithExistingPlantBucket(progress, usedPlantKey)) {
            progress.resetProgress();
            return;
        }
        progress.addBucketProgress(usedPlantKey, onlySource.getValue(), definition.getTarget());
    }

    private void recordCactusOnlyKills(QuestProgress progress,
                                       QuestDefinition definition,
                                       Game game,
                                       int totalKillDelta,
                                       Map<String, Integer> plantKillDeltas) {
        if (progress.isCompleted(definition.getTarget())) {
            return;
        }
        String cactusKey = PlantDefinition.normalizeKey("Cactus");
        List<String> usedPlants = game.getPlantedPlantNames();
        if (!usedPlants.isEmpty() && usedPlants.stream().anyMatch(name ->
            !PlantDefinition.normalizeKey(name).equals(cactusKey))) {
            progress.resetProgress();
            return;
        }
        if (totalKillDelta <= 0 && plantKillDeltas.isEmpty()) {
            return;
        }
        int cactusKills = cactusKills(plantKillDeltas, cactusKey);
        if (usedPlants.isEmpty() || cactusKills <= 0 || cactusKills != totalKillDelta
            || sumPositiveKills(plantKillDeltas) != cactusKills) {
            progress.resetProgress();
            return;
        }
        progress.addProgress(cactusKills, definition.getTarget());
    }

    private int cactusKills(Map<String, Integer> plantKillDeltas, String cactusKey) {
        int total = 0;
        for (Map.Entry<String, Integer> entry : plantKillDeltas.entrySet()) {
            if (PlantDefinition.normalizeKey(entry.getKey()).equals(cactusKey)
                && entry.getValue() != null && entry.getValue() > 0) {
                total += entry.getValue();
            }
        }
        return total;
    }

    private String onlyHistoricallyUsedPlant(Game game) {
        String onlyKey = null;
        for (String name : game.getPlantedPlantNames()) {
            String key = PlantDefinition.normalizeKey(name);
            if (onlyKey != null && !onlyKey.equals(key)) {
                return null;
            }
            onlyKey = key;
        }
        return onlyKey;
    }

    private Map.Entry<String, Integer> singleValidPlantSource(
            Map<String, Integer> plantKillDeltas) {
        Map.Entry<String, Integer> result = null;
        for (Map.Entry<String, Integer> entry : plantKillDeltas.entrySet()) {
            if (entry.getValue() == null || entry.getValue() <= 0) {
                continue;
            }
            if (result != null) {
                return null;
            }
            result = entry;
        }
        return result;
    }

    private int sumPositiveKills(Map<String, Integer> plantKillDeltas) {
        int total = 0;
        for (Integer value : plantKillDeltas.values()) {
            if (value != null && value > 0) {
                total += value;
            }
        }
        return total;
    }

    private boolean conflictsWithExistingPlantBucket(QuestProgress progress, String plantKey) {
        for (Map.Entry<String, Integer> entry : progress.getBucketProgress().entrySet()) {
            if (entry.getValue() > 0 && !entry.getKey().equals(plantKey)) {
                return true;
            }
        }
        return false;
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
}
