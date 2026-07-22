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
        engine.waitingSunProducers.clear();
        engine.endangeredPositions.clear();
        engine.conveyorCards.clear();
        engine.plantKillCounts.clear();
        engine.plantedPlantNames.clear();
        engine.plantedPlantFamilies.clear();
        engine.tombs.clear();
        engine.totalSunCollected = 0;
        engine.zombieKillCount = 0;
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
        engine.events.clear();
        engine.gameState = GameState.PLANT_SELECTION;
        engine.addForcedPlantSelections();
        engine.addEvent("Level prepared: " + engine.currentLevel.getLevelId() + " ("
            + engine.currentLevel.getSpecialType() + ").");
    }
    static void selectPlant(Game engine, String plantType) {
        engine.requirePlantSelection();
        if (engine.currentLevel != null && engine.currentLevel.getSpecialType() == SpecialLevelType.CONVEYOR_BELT) {
            throw new IllegalStateException("Plant selection is automatic in Conveyor Belt levels.");
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
        if (!engine.levelBoostedPlants.add(definition.getName())) {
            throw new IllegalStateException("Plant is already boosted for this level.");
        }
        engine.addEvent("Plant " + definition.getName() + " is boosted for this level.");
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
        if (engine.currentLevel.getSpecialType() == SpecialLevelType.CONVEYOR_BELT) {
            engine.autoSelectStarterPlantsForConveyor();
        } else if (engine.selectedPlants.isEmpty()) {
            engine.autoSelectStarterPlants();
        }
        engine.startGame();
    }
    static void startGame(Game engine) {
        engine.requirePlantSelection();
        if (engine.selectedPlants.isEmpty()
            && engine.currentLevel.getSpecialType() == SpecialLevelType.CONVEYOR_BELT) {
            engine.autoSelectStarterPlantsForConveyor();
        }
        if (engine.selectedPlants.isEmpty()) {
            throw new IllegalStateException("Select at least one plant before starting the game.");
        }
        engine.board = new Board();
        engine.sunAmount = engine.currentLevel.getStartingSunAmount();
        engine.elapsedTicks = 0;
        engine.nextWaveIndex = 0;
        engine.nextSkySunTick = engine.calculateNextSkySunTick();
        engine.lostPlantsCount = 0;
        engine.currentLevel.startLevel();
        engine.gameState = GameState.RUNNING;
        engine.initializeSeasonTerrain();
        engine.initializeSpecialLevel();
        engine.zombieWavesStarted = !engine.currentLevel.isWaitForZombieWaves();
        engine.addEvent("Game started with " + engine.sunAmount + " suns.");
        if (engine.zombieWavesStarted) {
            engine.startNextWave();
        } else {
            engine.addEvent("Setup phase started. Use 'start zombie waves' when ready.");
        }
    }
    static void startZombieWaves(Game engine) {
        engine.requireRunning();
        if (engine.currentLevel.getSpecialType() != SpecialLevelType.PLANT_WHAT_YOU_GET) {
            throw new IllegalStateException("This level does not have a manual wave start.");
        }
        if (engine.zombieWavesStarted) {
            throw new IllegalStateException("Zombie waves have already started.");
        }
        engine.zombieWavesStarted = true;
        for (String key : new ArrayList<>(engine.cooldownTicks.keySet())) {
            engine.cooldownTicks.put(key, 0);
        }
        engine.addEvent("Zombie waves started.");
        engine.startNextWave();
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
        wave.populate(engine.zombieFactory,
            engine.zombieFactory.getDefinitionsForSeason(engine.currentLevel.getSeason()), targetCost,
            engine.board.getRows(), engine.board.getCols() - 0.05, engine.random);
        engine.configureWaveForSeason(wave);
        engine.configureZombieDifficultyAndDrops(wave);
        wave.startWave();
        for (Zombie zombie : wave.getZombies()) {
            engine.board.addZombie(zombie);
        }
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
        boolean conveyor = engine.currentLevel.getSpecialType() == SpecialLevelType.CONVEYOR_BELT;
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
            engine.finishPlantPurchase(key, definition.getName(), plant, conveyor);
            return;
        }
        engine.validateSpecialPlantLocation(plant, row, col);
        engine.board.placePlant(plant, row, col);
        engine.finishPlantPurchase(key, definition.getName(), plant, conveyor);
        Plant activePlant = plant.getPosition() == null
            ? engine.board.getTile(row, col).getMainPlant() : plant;
        if (activePlant == null) {
            throw new IllegalStateException("Plant placement did not create an active plant.");
        }
        String detail = activePlant == plant ? "planted" : "stacked";
        engine.recordPlantUsage(plant);
        engine.addEvent("Plant " + plant.getName() + " (level " + plant.getPlantLevel()
            + ") " + detail + " at " + engine.display(row, col) + ".");
        boolean boosted = engine.applyAutomaticBoostIfPresent(activePlant);
        if (!boosted || !activePlant.isExplosive()) {
            engine.handleImmediatePlant(activePlant);
        }
        engine.cleanupDestroyedEntities();
        engine.evaluateGameState();
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
