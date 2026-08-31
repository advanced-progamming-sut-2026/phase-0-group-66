package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

final class BattleCommandSystem {
    private BattleCommandSystem() { }

    static void prepareLevel(Game engine, Chapter chapter, Level level) {
        if (level == null) {
            throw new IllegalArgumentException("Level cannot be null.");
        }
        engine.currentChapter = chapter;
        engine.currentLevel = level.copyForPlay();
        engine.board = new Board();
        engine.currentWave = null;
        engine.sunAmount = 0;
        engine.elapsedTicks = 0;
        engine.nextWaveIndex = 0;
        engine.nextSkySunTick = 0;
        engine.lostPlantsCount = 0;
        engine.selectedPlants.clear();
        engine.levelBoostedPlants.clear();
        engine.cooldownTicks.clear();
        engine.pendingZombieSpawns.clear();
        engine.waitingSunProducers.clear();
        engine.endangeredPositions.clear();
        engine.conveyorCards.clear();
        engine.plantKillCounts.clear();
        engine.plantedPlantNames.clear();
        engine.plantedPlantFamilies.clear();
        engine.tombs.clear();
        engine.tornadoEntryZombies.clear();
        engine.coldWindRows.clear();
        engine.coldWindUntilTick = 0;
        engine.warmedIcePositions.clear();
        engine.totalSunCollected = 0;
        engine.zombieKillCount = 0;
        engine.timedWarKillSamples.clear();
        engine.timedWarSunSamples.clear();
        engine.timedWarLastKillCount = 0;
        engine.timedWarLastSunCollected = 0;
        engine.explosivePlantsUsed = 0;
        engine.lawnMowerKills = 0;
        engine.killsWithinThirtySeconds = 0;
        engine.firstColumnNoMowerKills = 0;
        engine.piercingProjectileHits = 0;
        engine.multiKillZombieCount = 0;
        engine.lastKillTick = -1;
        engine.killsAtLastKillTick = 0;
        engine.sunProducerPlantsPlanted = 0;
        engine.nextConveyorTick = 0;
        engine.zombieWavesStarted = false;
        engine.externalWinControlled = false;
        engine.events.clear();
        engine.gameState = GameState.PLANT_SELECTION;
        engine.addForcedPlantSelections();
        engine.addEvent("Level prepared: " + engine.currentLevel.getLevelId() + " ("
            + engine.currentLevel.getSpecialType() + ").");
    }
    static void selectPlant(Game engine, String plantType) {
        engine.requirePlantSelection();
        if (engine.currentLevel != null
            && !engine.currentLevel.getRuleStrategy().allowsManualPlantSelection()) {
            throw new IllegalStateException("Plant selection is automatic in this level.");
        }
        PlantDefinition definition = engine.plantFactory.findDefinition(plantType)
            .orElseThrow(() -> new IllegalArgumentException("Plant does not exist: " + plantType));
        engine.validatePlantSelectionRule(definition);
        String canonicalName = definition.getName();
        if (engine.selectedPlants.contains(canonicalName)) {
            throw new IllegalStateException("Plant is already selected.");
        }
        if (engine.selectedPlants.size() >= engine.currentLevel.getAllowedPlantCount()) {
            throw new IllegalStateException("No empty plant-selection slot remains.");
        }
        engine.selectedPlants.add(canonicalName);
        engine.cooldownTicks.put(definition.getNormalizedName(), 0);
        engine.addEvent("Selected plant: " + canonicalName + ".");
    }
    static void removeSelectedPlant(Game engine, String plantType) {
        engine.requirePlantSelection();
        PlantDefinition definition = engine.plantFactory.findDefinition(plantType)
            .orElseThrow(() -> new IllegalArgumentException("Plant does not exist: " + plantType));
        if (engine.containsNormalized(engine.currentLevel.getForcedPlants(), definition.getName())) {
            throw new IllegalStateException("This plant is forced and cannot be removed.");
        }
        if (!engine.selectedPlants.remove(definition.getName())) {
            throw new IllegalStateException("Plant is not selected.");
        }
        engine.cooldownTicks.remove(definition.getNormalizedName());
        engine.addEvent("Removed selected plant: " + definition.getName() + ".");
    }
    static void boostSelectedPlant(Game engine, String plantType) {
        engine.requirePlantSelection();
        PlantDefinition definition = engine.plantFactory.findDefinition(plantType)
            .orElseThrow(() -> new IllegalArgumentException("Plant does not exist: " + plantType));
        if (!engine.selectedPlants.contains(definition.getName())) {
            throw new IllegalStateException("Select the plant before boosting it.");
        }
        if (definition.getPlantFoodType() == PlantFoodType.NONE
            && definition.getAbility() != PlantAbility.IMITATER) {
            throw new IllegalStateException(definition.getName()
                + " has no plant-food effect and cannot be boosted.");
        }
        if (engine.levelBoostedPlants.contains(definition.getName())) {
            throw new IllegalStateException("Plant is already boosted for this level.");
        }
        if (!engine.wallet.spendGems(2)) {
            throw new IllegalStateException("Boosting a plant requires 2 gems.");
        }
        engine.levelBoostedPlants.add(definition.getName());
        engine.addEvent("Plant " + definition.getName()
            + " is boosted for this level; 2 gems were spent.");
    }
    static boolean isLevelBoosted(Game engine, String plantType) {
        PlantDefinition definition = engine.plantFactory.findDefinition(plantType).orElse(null);
        return definition != null && engine.levelBoostedPlants.contains(definition.getName());
    }
    static void feedPlant(Game engine, int row, int col) {
        engine.requireRunning();
        Plant plant = engine.board.getTile(row, col).getPlant();
        if (plant == null || plant.isDestroyed()) {
            throw new IllegalStateException("There is no living plant on this tile.");
        }
        if (plant.getDefinition().getPlantFoodType() == PlantFoodType.NONE) {
            throw new IllegalStateException(plant.getName() + " has no plant-food effect.");
        }
        if (!engine.inventory.consumePlantFood()) {
            throw new IllegalStateException("No plant food is available.");
        }
        engine.activatePlantFood(plant, "manual plant food");
        engine.cleanupDestroyedEntities();
        engine.evaluateGameState();
    }
    static void addPlantFoodCheat(Game engine) {
        engine.requireRunning();
        if (engine.inventory.getPlantFoodCapacityLeft() <= 0) {
            throw new IllegalStateException("Plant food storage is already full.");
        }
        engine.inventory.addPlantFood(1);
        engine.addEvent("Cheat added one plant food; total=" + engine.inventory.getPlantFoods() + ".");
    }
    static int getPlantFoodCount(Game engine) {
        return engine.inventory.getPlantFoods();
    }
    static void startGame(Game engine, Level level) {
        engine.prepareLevel(null, level);
        if (engine.currentLevel.getRuleStrategy().usesConveyor()) {
            engine.autoSelectStarterPlantsForConveyor();
        } else if (engine.selectedPlants.isEmpty()) {
            engine.autoSelectStarterPlants();
        }
        engine.startGame();
    }
    static void startGame(Game engine) {
        engine.requirePlantSelection();
        if (engine.selectedPlants.isEmpty()
            && engine.currentLevel.getRuleStrategy().usesConveyor()) {
            engine.autoSelectStarterPlantsForConveyor();
        }
        if (engine.selectedPlants.isEmpty()) {
            throw new IllegalStateException("Select at least one plant before starting the game.");
        }
        engine.board = new Board();
        engine.sunAmount = engine.currentLevel.getStartingSunAmount();
        engine.elapsedTicks = 0;
        engine.nextWaveIndex = 0;
        engine.pendingZombieSpawns.clear();
        engine.nextSkySunTick = engine.calculateNextSkySunTick();
        engine.lostPlantsCount = 0;
        engine.plantedPositions.clear();
        engine.encounteredZombieNames.clear();
        engine.plantedPlantNames.clear();
        engine.plantedPlantFamilies.clear();
        engine.sunProducerPlantsPlanted = 0;
        engine.currentLevel.startLevel();
        engine.gameState = GameState.RUNNING;
        engine.initializeSeasonTerrain();
        engine.initializeSpecialLevel();
        engine.zombieWavesStarted = !engine.currentLevel.isWaitForZombieWaves()
            && !engine.currentLevel.getRuleStrategy().requiresManualWaveStart();
        engine.addEvent("Game started with " + engine.sunAmount + " suns.");
        if (engine.zombieWavesStarted) {
            engine.addEvent("Preparation phase started. The first zombie wave will arrive after 15 seconds.");
        } else {
            engine.addEvent("Setup phase started. Use 'start zombie waves' when ready.");
        }
    }
    static void startZombieWaves(Game engine) {
        engine.requireRunning();
        if (!engine.currentLevel.getRuleStrategy().requiresManualWaveStart()) {
            throw new IllegalStateException("This level does not have a manual wave start.");
        }
        if (engine.zombieWavesStarted) {
            throw new IllegalStateException("Zombie waves have already started.");
        }
        engine.zombieWavesStarted = true;
        for (String key : new ArrayList<>(engine.cooldownTicks.keySet())) {
            engine.cooldownTicks.put(key, 0);
        }
        engine.addEvent("Zombie waves started. You have 15 seconds to prepare.");
    }
    static void advanceTime(Game engine, int ticks) {
        engine.requireRunning();
        if (ticks <= 0) {
            throw new IllegalArgumentException("Tick count must be positive.");
        }
        if (engine.isPreWaveSetup()) {
            throw new IllegalStateException("Start zombie waves before advancing time.");
        }
        for (int index = 0; index < ticks && engine.gameState == GameState.RUNNING; index++) {
            engine.advanceOneTick();
        }
    }
    static void startNextWave(Game engine) {
        engine.requireRunning();
        if (!engine.zombieWavesStarted) {
            return;
        }
        List<Wave> waves = engine.currentLevel.getWaves();
        if (engine.nextWaveIndex >= waves.size()) {
            return;
        }
        Wave wave = waves.get(engine.nextWaveIndex);
        int targetCost = engine.adjustedWaveCost(wave.getDifficultyCost());
        targetCost = levelOneSpawnCost(engine, wave, targetCost);
        wave.populate(engine.zombieFactory, waveZombieDefinitions(engine), targetCost,
            engine.board.getRows(), engine.board.getCols() - 0.05, engine.random);
        engine.applyWaveStartSeasonEffects(wave);
        engine.configureWaveForSeason(wave);
        engine.configureZombieDifficultyAndDrops(wave);
        wave.startWave();
        scheduleWaveZombies(engine, wave);
        ZombieObjectSystem.ensureZombieCompanions(engine);
        engine.currentWave = wave;
        engine.nextWaveIndex++;
        if (engine.nextWaveIndex == waves.size()) {
            engine.addEvent("The final wave has come.");
        } else {
            engine.addEvent("Wave " + wave.getWaveNumber() + " started.");
        }
        for (Zombie zombie : wave.getZombies()) {
            int lane = zombie.getPosition().getRow() + 1;
            engine.addEvent("Zombie " + zombie.getName() + " spawned at wave "
                + wave.getWaveNumber() + " in lane " + lane + " which costed "
                + zombie.getWaveCost() + ".");
        }
        engine.board.refreshZombieTiles();
    }
    private static int levelOneSpawnCost(Game engine, Wave wave, int defaultCost) {
        if (engine.currentLevel.getLevelNumber() != 1) {
            return defaultCost;
        }
        boolean finalWave = wave.getWaveNumber() == engine.currentLevel.getWaves().size();
        return finalWave ? 1000 : 500;
    }

    private static List<ZombieDefinition> waveZombieDefinitions(Game engine) {
        List<ZombieDefinition> seasonal = engine.zombieFactory
            .getDefinitionsForSeason(engine.currentLevel.getSeason());
        if (engine.currentLevel.getLevelNumber() != 1) {
            return seasonal;
        }
        ZombieDefinition basic = engine.zombieFactory.findDefinition("Basic Zombie")
            .filter(definition -> definition.isAvailableIn(engine.currentLevel.getSeason()))
            .orElse(null);
        if (basic != null) {
            return List.of(basic);
        }
        int minimumCost = seasonal.stream()
            .mapToInt(ZombieDefinition::getWavePointCost)
            .filter(cost -> cost > 0)
            .min()
            .orElseThrow(() -> new IllegalStateException("No zombie is available for level 1."));
        return seasonal.stream()
            .filter(definition -> definition.getWavePointCost() == minimumCost)
            .toList();
    }

    private static void scheduleWaveZombies(Game engine, Wave wave) {
        boolean finalWave = wave.getWaveNumber() == engine.currentLevel.getWaves().size();
        int delayTicks = 0;
        for (Zombie zombie : wave.getZombies()) {
            engine.recordZombieEncounter(zombie);
            if (delayTicks == 0) {
                engine.board.addZombie(zombie);
            } else {
                engine.pendingZombieSpawns.put(zombie, delayTicks);
            }
            delayTicks += nextSpawnGapTicks(engine, finalWave);
        }
    }

    private static int nextSpawnGapTicks(Game engine, boolean finalWave) {
        int minimum = finalWave ? 22 : 32;
        int variation = finalWave ? 8 : 10;
        return minimum + engine.random.nextInt(variation + 1);
    }

    static void plant(Game engine, Plant plant, int row, int col) {
        engine.requireRunning();
        if (plant == null) {
            throw new IllegalArgumentException("Plant cannot be null.");
        }
        engine.plant(plant.getName(), row, col);
    }
    static void plant(Game engine, String plantType, int row, int col) {
        engine.requireRunning();
        PlantDefinition definition = engine.plantFactory.findDefinition(plantType)
            .orElseThrow(() -> new IllegalArgumentException("Plant does not exist: " + plantType));
        if (!engine.selectedPlants.contains(definition.getName())) {
            throw new IllegalStateException("Plant was not selected for this level.");
        }
        String key = definition.getNormalizedName();
        boolean conveyor = engine.currentLevel.getRuleStrategy().usesConveyor();
        boolean cooldownDisabled = conveyor || engine.isPreWaveSetup();
        int remainingCooldown = engine.cooldownTicks.getOrDefault(key, 0);
        if (!cooldownDisabled && remainingCooldown > 0) {
            throw new IllegalStateException("Plant is on cooldown for "
                + engine.formatSeconds(remainingCooldown) + " seconds.");
        }
        engine.ensureConveyorCardAvailable(definition, conveyor);
        int plantLevel = engine.plantLevels.getOrDefault(key, 1);
        Plant plant = engine.createPlantForPlacement(definition, plantLevel);
        if (!conveyor && engine.sunAmount < plant.getSunCost()) {
            throw new IllegalStateException("Not enough sun.");
        }
        if (engine.handleTerrainUtilityPlant(plant, row, col)) {
            engine.recordPlantUsage(plant, row, col);
            engine.finishPlantPurchase(key, definition.getName(), plant, conveyor);
            return;
        }
        engine.validateSpecialPlantLocation(plant, row, col);
        engine.board.placePlant(plant, row, col);
        engine.finishPlantPurchase(key, definition.getName(), plant, conveyor);
        completePlantPlacement(engine, definition, plant, plantLevel, row, col);
        engine.cleanupDestroyedEntities();
        engine.evaluateGameState();
    }
    private static void completePlantPlacement(Game engine, PlantDefinition definition,
                                               Plant plant, int plantLevel, int row, int col) {
        Plant activePlant = plant.getPosition() == null
            ? engine.board.getTile(row, col).getMainPlant() : plant;
        if (activePlant == null) {
            throw new IllegalStateException("Plant placement did not create an active plant.");
        }
        String detail = activePlant == plant ? "planted" : "stacked";
        engine.recordPlantUsage(plant, row, col);
        engine.addEvent("Plant " + plant.getName() + " (level " + plant.getPlantLevel()
            + ") " + detail + " at " + engine.display(row, col) + ".");
        boolean boosted = engine.applyAutomaticBoostIfPresent(
            activePlant, definition.getName());
        if (!boosted && hasImitaterEntranceFood(definition, plantLevel, activePlant)) {
            engine.activatePlantFood(activePlant, "Imitater level-4 entrance effect");
            boosted = true;
        }
        if (!boosted || !activePlant.isExplosive()) {
            engine.handleImmediatePlant(activePlant);
        }
    }

    private static boolean hasImitaterEntranceFood(PlantDefinition definition, int plantLevel,
                                                    Plant activePlant) {
        return definition.getAbility() == PlantAbility.IMITATER
            && PlantStats.calculate(definition, plantLevel)
                .hasTrait("PLANT_FOOD_ON_ENTERANCE")
            && activePlant.getDefinition().getPlantFoodType() != PlantFoodType.NONE;
    }

    static void pluckPlant(Game engine, int row, int col) {
        engine.requireRunning();
        GridPosition position = new GridPosition(row, col);
        if (engine.endangeredPositions.contains(position)) {
            throw new IllegalStateException("Protected seed plants cannot be removed.");
        }
        Plant plant = engine.board.removePlant(row, col);
        if (plant == null) {
            throw new IllegalStateException("There is no plant on this tile.");
        }
        engine.waitingSunProducers.remove(new GridPosition(row, col));
        engine.addEvent("Plant " + plant.getName() + " was removed from " + engine.display(row, col) + ".");
    }
    static void collectSun(Game engine, int row, int col) {
        engine.requireRunning();
        List<Sun> suns = engine.board.getSunsAt(row, col);
        if (suns.isEmpty()) {
            throw new IllegalStateException("There is no sun at this position.");
        }
        Sun sun = suns.get(0);
        if (sun.getType() == SunType.RADIOACTIVE && sun.isFalling()) {
            engine.explodeRadioactiveSun(sun);
            engine.board.removeSun(sun);
            engine.addEvent("Radioactive sun exploded at " + engine.display(row, col) + ".");
            engine.cleanupDestroyedEntities();
            engine.evaluateGameState();
            return;
        }
        int collectedAmount = sun.collect();
        engine.sunAmount += collectedAmount;
        engine.totalSunCollected += collectedAmount;
        engine.board.removeSun(sun);
        GridPosition position = new GridPosition(row, col);
        if (engine.waitingSunProducers.remove(position)) {
            Plant producer = engine.board.getTile(row, col).getPlant();
            if (producer != null && producer.isSunProducer()) {
                producer.resetActionTimer();
            }
        }
        engine.addEvent("Collected " + collectedAmount + " suns at " + engine.display(row, col)
            + "; total=" + engine.sunAmount + ".");
        engine.evaluateGameState();
    }
}
