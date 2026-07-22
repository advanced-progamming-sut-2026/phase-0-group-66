package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Game {
    public static final int TICKS_PER_SECOND = 10;
    private static final double PROJECTILE_SPEED = 5.0;

    private final PlantFactory plantFactory;
    private final ZombieFactory zombieFactory;
    private final Random random;
    private final int difficultyLevel;
    private final Map<String, Integer> plantLevels;
    private final Inventory inventory;
    private final Wallet wallet;
    private final LinkedHashSet<String> selectedPlants;
    private final LinkedHashSet<String> levelBoostedPlants;
    private final LinkedHashMap<String, Integer> cooldownTicks;
    private final LinkedHashSet<GridPosition> waitingSunProducers;
    private final LinkedHashSet<GridPosition> endangeredPositions;
    private final LinkedHashMap<String, Integer> conveyorCards;
    private final LinkedHashMap<String, Integer> plantKillCounts;
    private final LinkedHashSet<String> plantedPlantNames;
    private final LinkedHashSet<String> plantedPlantFamilies;
    private final LinkedHashMap<GridPosition, Tomb> tombs;
    private final ArrayList<String> events;

    private GameState gameState;
    private Chapter currentChapter;
    private Level currentLevel;
    private Board board;
    private Wave currentWave;
    private int sunAmount;
    private int elapsedTicks;
    private int nextWaveIndex;
    private int nextSkySunTick;
    private int lostPlantsCount;
    private int totalSunCollected;
    private int zombieKillCount;
    private int explosivePlantsUsed;
    private int lawnMowerKills;
    private int killsWithinThirtySeconds;
    private int firstColumnNoMowerKills;
    private int sunProducerPlantsPlanted;
    private int nextConveyorTick;
    private boolean zombieWavesStarted;

    public Game(PlantFactory plantFactory, ZombieFactory zombieFactory) {
        this(plantFactory, zombieFactory, 3, Map.of(), new Inventory(), new Wallet(),
            new Random());
    }

    public Game(PlantFactory plantFactory, ZombieFactory zombieFactory, int difficultyLevel,
                Map<String, Integer> plantLevels, Inventory inventory, Wallet wallet) {
        this(plantFactory, zombieFactory, difficultyLevel, plantLevels, inventory, wallet,
            new Random());
    }

    Game(PlantFactory plantFactory, ZombieFactory zombieFactory, int difficultyLevel,
         Map<String, Integer> plantLevels, Inventory inventory, Wallet wallet, Random random) {
        if (plantFactory == null || zombieFactory == null || inventory == null
            || wallet == null || random == null) {
            throw new IllegalArgumentException("Game dependencies cannot be null.");
        }
        if (difficultyLevel < 1 || difficultyLevel > 5) {
            throw new IllegalArgumentException("Difficulty level must be between 1 and 5.");
        }
        this.plantFactory = plantFactory;
        this.zombieFactory = zombieFactory;
        this.difficultyLevel = difficultyLevel;
        this.inventory = inventory;
        this.wallet = wallet;
        this.random = random;
        this.plantLevels = normalizePlantLevels(plantLevels);
        this.selectedPlants = new LinkedHashSet<>();
        this.levelBoostedPlants = new LinkedHashSet<>();
        this.cooldownTicks = new LinkedHashMap<>();
        this.waitingSunProducers = new LinkedHashSet<>();
        this.endangeredPositions = new LinkedHashSet<>();
        this.conveyorCards = new LinkedHashMap<>();
        this.plantKillCounts = new LinkedHashMap<>();
        this.plantedPlantNames = new LinkedHashSet<>();
        this.plantedPlantFamilies = new LinkedHashSet<>();
        this.tombs = new LinkedHashMap<>();
        this.events = new ArrayList<>();
        this.gameState = GameState.PLANT_SELECTION;
    }

    public void prepareLevel(Chapter chapter, Level level) {
        if (level == null) {
            throw new IllegalArgumentException("Level cannot be null.");
        }
        currentChapter = chapter;
        currentLevel = level.copyForPlay();
        board = new Board();
        currentWave = null;
        sunAmount = 0;
        elapsedTicks = 0;
        nextWaveIndex = 0;
        nextSkySunTick = 0;
        lostPlantsCount = 0;
        selectedPlants.clear();
        levelBoostedPlants.clear();
        cooldownTicks.clear();
        waitingSunProducers.clear();
        endangeredPositions.clear();
        conveyorCards.clear();
        plantKillCounts.clear();
        plantedPlantNames.clear();
        plantedPlantFamilies.clear();
        tombs.clear();
        totalSunCollected = 0;
        zombieKillCount = 0;
        explosivePlantsUsed = 0;
        lawnMowerKills = 0;
        killsWithinThirtySeconds = 0;
        firstColumnNoMowerKills = 0;
        sunProducerPlantsPlanted = 0;
        nextConveyorTick = 0;
        zombieWavesStarted = false;
        events.clear();
        gameState = GameState.PLANT_SELECTION;
        addForcedPlantSelections();
        addEvent("Level prepared: " + currentLevel.getLevelId() + " ("
            + currentLevel.getSpecialType() + ").");
    }

    public void selectPlant(String plantType) {
        requirePlantSelection();
        if (currentLevel != null && currentLevel.getSpecialType() == SpecialLevelType.CONVEYOR_BELT) {
            throw new IllegalStateException("Plant selection is automatic in Conveyor Belt levels.");
        }
        PlantDefinition definition = plantFactory.findDefinition(plantType)
            .orElseThrow(() -> new IllegalArgumentException("Plant does not exist: " + plantType));
        validatePlantSelectionRule(definition);
        String canonicalName = definition.getName();
        if (selectedPlants.contains(canonicalName)) {
            throw new IllegalStateException("Plant is already selected.");
        }
        if (selectedPlants.size() >= currentLevel.getAllowedPlantCount()) {
            throw new IllegalStateException("No empty plant-selection slot remains.");
        }
        selectedPlants.add(canonicalName);
        cooldownTicks.put(definition.getNormalizedName(), 0);
        addEvent("Selected plant: " + canonicalName + ".");
    }

    public void removeSelectedPlant(String plantType) {
        requirePlantSelection();
        PlantDefinition definition = plantFactory.findDefinition(plantType)
            .orElseThrow(() -> new IllegalArgumentException("Plant does not exist: " + plantType));
        if (containsNormalized(currentLevel.getForcedPlants(), definition.getName())) {
            throw new IllegalStateException("This plant is forced and cannot be removed.");
        }
        if (!selectedPlants.remove(definition.getName())) {
            throw new IllegalStateException("Plant is not selected.");
        }
        cooldownTicks.remove(definition.getNormalizedName());
        addEvent("Removed selected plant: " + definition.getName() + ".");
    }

    public void boostSelectedPlant(String plantType) {
        requirePlantSelection();
        PlantDefinition definition = plantFactory.findDefinition(plantType)
            .orElseThrow(() -> new IllegalArgumentException("Plant does not exist: " + plantType));
        if (!selectedPlants.contains(definition.getName())) {
            throw new IllegalStateException("Select the plant before boosting it.");
        }
        if (!levelBoostedPlants.add(definition.getName())) {
            throw new IllegalStateException("Plant is already boosted for this level.");
        }
        addEvent("Plant " + definition.getName() + " is boosted for this level.");
    }

    public boolean isLevelBoosted(String plantType) {
        PlantDefinition definition = plantFactory.findDefinition(plantType).orElse(null);
        return definition != null && levelBoostedPlants.contains(definition.getName());
    }

    public void feedPlant(int row, int col) {
        requireRunning();
        Plant plant = board.getTile(row, col).getPlant();
        if (plant == null || plant.isDestroyed()) {
            throw new IllegalStateException("There is no living plant on this tile.");
        }
        if (!inventory.consumePlantFood()) {
            throw new IllegalStateException("No plant food is available.");
        }
        activatePlantFood(plant, "manual plant food");
        cleanupDestroyedEntities();
        evaluateGameState();
    }

    public void addPlantFoodCheat() {
        requireRunning();
        if (inventory.getPlantFoodCapacityLeft() <= 0) {
            throw new IllegalStateException("Plant food storage is already full.");
        }
        inventory.addPlantFood(1);
        addEvent("Cheat added one plant food; total=" + inventory.getPlantFoods() + ".");
    }

    public int getPlantFoodCount() {
        return inventory.getPlantFoods();
    }

    public void startGame(Level level) {
        prepareLevel(null, level);
        if (currentLevel.getSpecialType() == SpecialLevelType.CONVEYOR_BELT) {
            autoSelectStarterPlantsForConveyor();
        } else if (selectedPlants.isEmpty()) {
            autoSelectStarterPlants();
        }
        startGame();
    }

    public void startGame() {
        requirePlantSelection();
        if (selectedPlants.isEmpty()
            && currentLevel.getSpecialType() == SpecialLevelType.CONVEYOR_BELT) {
            autoSelectStarterPlantsForConveyor();
        }
        if (selectedPlants.isEmpty()) {
            throw new IllegalStateException("Select at least one plant before starting the game.");
        }
        board = new Board();
        sunAmount = currentLevel.getStartingSunAmount();
        elapsedTicks = 0;
        nextWaveIndex = 0;
        nextSkySunTick = calculateNextSkySunTick();
        lostPlantsCount = 0;
        currentLevel.startLevel();
        gameState = GameState.RUNNING;
        initializeSeasonTerrain();
        initializeSpecialLevel();
        zombieWavesStarted = !currentLevel.isWaitForZombieWaves();
        addEvent("Game started with " + sunAmount + " suns.");
        if (zombieWavesStarted) {
            startNextWave();
        } else {
            addEvent("Setup phase started. Use 'start zombie waves' when ready.");
        }
    }


    public void startZombieWaves() {
        requireRunning();
        if (currentLevel.getSpecialType() != SpecialLevelType.PLANT_WHAT_YOU_GET) {
            throw new IllegalStateException("This level does not have a manual wave start.");
        }
        if (zombieWavesStarted) {
            throw new IllegalStateException("Zombie waves have already started.");
        }
        zombieWavesStarted = true;
        for (String key : new ArrayList<>(cooldownTicks.keySet())) {
            cooldownTicks.put(key, 0);
        }
        addEvent("Zombie waves started.");
        startNextWave();
    }

    public void advanceTime(int ticks) {
        requireRunning();
        if (ticks <= 0) {
            throw new IllegalArgumentException("Tick count must be positive.");
        }
        if (isPreWaveSetup()) {
            throw new IllegalStateException("Start zombie waves before advancing time.");
        }
        for (int index = 0; index < ticks && gameState == GameState.RUNNING; index++) {
            advanceOneTick();
        }
    }

    public void startNextWave() {
        requireRunning();
        if (!zombieWavesStarted) {
            return;
        }
        List<Wave> waves = currentLevel.getWaves();
        if (nextWaveIndex >= waves.size()) {
            return;
        }
        Wave wave = waves.get(nextWaveIndex);
        int targetCost = adjustedWaveCost(wave.getDifficultyCost());
        wave.populate(zombieFactory,
            zombieFactory.getDefinitionsForSeason(currentLevel.getSeason()), targetCost,
            board.getRows(), board.getCols() - 0.05, random);
        configureWaveForSeason(wave);
        configureZombieDifficultyAndDrops(wave);
        wave.startWave();
        for (Zombie zombie : wave.getZombies()) {
            board.addZombie(zombie);
        }
        currentWave = wave;
        nextWaveIndex++;
        if (nextWaveIndex == waves.size()) {
            addEvent("The final wave has come.");
        } else {
            addEvent("Wave " + wave.getWaveNumber() + " started.");
        }
        for (Zombie zombie : wave.getZombies()) {
            int lane = zombie.getPosition().getRow() + 1;
            addEvent("Zombie " + zombie.getName() + " spawned at wave "
                + wave.getWaveNumber() + " in lane " + lane + " which costed "
                + zombie.getWaveCost() + ".");
        }
        board.refreshZombieTiles();
    }

    public void plant(Plant plant, int row, int col) {
        requireRunning();
        if (plant == null) {
            throw new IllegalArgumentException("Plant cannot be null.");
        }
        plant(plant.getName(), row, col);
    }

    public void plant(String plantType, int row, int col) {
        requireRunning();
        PlantDefinition definition = plantFactory.findDefinition(plantType)
            .orElseThrow(() -> new IllegalArgumentException("Plant does not exist: " + plantType));
        if (!selectedPlants.contains(definition.getName())) {
            throw new IllegalStateException("Plant was not selected for this level.");
        }
        String key = definition.getNormalizedName();
        boolean conveyor = currentLevel.getSpecialType() == SpecialLevelType.CONVEYOR_BELT;
        boolean cooldownDisabled = conveyor || isPreWaveSetup();
        int remainingCooldown = cooldownTicks.getOrDefault(key, 0);
        if (!cooldownDisabled && remainingCooldown > 0) {
            throw new IllegalStateException("Plant is on cooldown for "
                + formatSeconds(remainingCooldown) + " seconds.");
        }
        ensureConveyorCardAvailable(definition, conveyor);
        int plantLevel = plantLevels.getOrDefault(key, 1);
        Plant plant = createPlantForPlacement(definition, plantLevel);
        if (!conveyor && sunAmount < plant.getSunCost()) {
            throw new IllegalStateException("Not enough sun.");
        }
        if (handleTerrainUtilityPlant(plant, row, col)) {
            finishPlantPurchase(key, definition.getName(), plant, conveyor);
            return;
        }
        validateSpecialPlantLocation(plant, row, col);
        board.placePlant(plant, row, col);
        finishPlantPurchase(key, definition.getName(), plant, conveyor);
        Plant activePlant = plant.getPosition() == null
            ? board.getTile(row, col).getMainPlant() : plant;
        if (activePlant == null) {
            throw new IllegalStateException("Plant placement did not create an active plant.");
        }
        String detail = activePlant == plant ? "planted" : "stacked";
        recordPlantUsage(plant);
        addEvent("Plant " + plant.getName() + " (level " + plant.getPlantLevel()
            + ") " + detail + " at " + display(row, col) + ".");
        boolean boosted = applyAutomaticBoostIfPresent(activePlant);
        if (!boosted || !activePlant.isExplosive()) {
            handleImmediatePlant(activePlant);
        }
        cleanupDestroyedEntities();
        evaluateGameState();
    }

    public void pluckPlant(int row, int col) {
        requireRunning();
        GridPosition position = new GridPosition(row, col);
        if (endangeredPositions.contains(position)) {
            throw new IllegalStateException("Protected seed plants cannot be removed.");
        }
        Plant plant = board.removePlant(row, col);
        if (plant == null) {
            throw new IllegalStateException("There is no plant on this tile.");
        }
        waitingSunProducers.remove(new GridPosition(row, col));
        addEvent("Plant " + plant.getName() + " was removed from " + display(row, col) + ".");
    }

    public void collectSun(int row, int col) {
        requireRunning();
        List<Sun> suns = board.getSunsAt(row, col);
        if (suns.isEmpty()) {
            throw new IllegalStateException("There is no sun at this position.");
        }
        Sun sun = suns.get(0);
        if (sun.getType() == SunType.RADIOACTIVE && sun.isFalling()) {
            explodeRadioactiveSun(sun);
            board.removeSun(sun);
            addEvent("Radioactive sun exploded at " + display(row, col) + ".");
            cleanupDestroyedEntities();
            evaluateGameState();
            return;
        }
        int collectedAmount = sun.collect();
        sunAmount += collectedAmount;
        totalSunCollected += collectedAmount;
        board.removeSun(sun);
        GridPosition position = new GridPosition(row, col);
        if (waitingSunProducers.remove(position)) {
            Plant producer = board.getTile(row, col).getPlant();
            if (producer != null && producer.isSunProducer()) {
                producer.resetActionTimer();
            }
        }
        addEvent("Collected " + collectedAmount + " suns at " + display(row, col)
            + "; total=" + sunAmount + ".");
        evaluateGameState();
    }

    public void addSun(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Sun amount cannot be negative.");
        }
        sunAmount += amount;
        addEvent("Cheat added " + amount + " suns; total=" + sunAmount + ".");
    }

    public void removeAllCooldowns() {
        for (String key : new ArrayList<>(cooldownTicks.keySet())) {
            cooldownTicks.put(key, 0);
        }
        addEvent("All plant cooldowns were removed.");
    }

    public void releaseNuke() {
        requireRunning();
        for (Zombie zombie : new ArrayList<>(board.getZombies())) {
            zombie.kill();
        }
        addEvent("The nuke destroyed every zombie on the board.");
        cleanupDestroyedEntities();
        evaluateGameState();
    }

    public void spawnZombie(String zombieType, int row, double column) {
        requireRunning();
        if (row < 0 || row >= board.getRows()) {
            throw new IllegalArgumentException("Zombie row is outside the board.");
        }
        Zombie zombie = zombieFactory.createZombie(zombieType);
        zombie.applyDifficulty(difficultyLevel);
        zombie.setGlowing(zombie.getDefinition().canSpawnPlantFood() && random.nextInt(100) < 5);
        zombie.setPosition(new BoardPosition(row, column));
        board.addZombie(zombie);
        addEvent("Cheat spawned " + zombie.getName() + " at ("
            + formatColumn(column) + ", " + (row + 1) + ").");
    }

    public boolean checkWinCondition() {
        if (currentLevel == null || board == null) {
            return false;
        }
        if (currentLevel.getSpecialType() == SpecialLevelType.TIMED_WAR) {
            return timedWarProgress() >= currentLevel.getTimedWarTarget();
        }
        if (currentLevel.getSpecialType() == SpecialLevelType.PLANT_WHAT_YOU_GET
            && !zombieWavesStarted) {
            return false;
        }
        return nextWaveIndex >= currentLevel.getWaves().size() && board.getZombies().isEmpty();
    }

    public boolean checkLoseCondition() {
        return gameState == GameState.LOST;
    }

    public List<String> drainEvents() {
        List<String> result = List.copyOf(events);
        events.clear();
        return result;
    }

    public List<String> getSelectedPlants() {
        return List.copyOf(selectedPlants);
    }

    public boolean isPlantAvailableForSelection(String plantType) {
        PlantDefinition definition = plantFactory.findDefinition(plantType).orElse(null);
        if (definition == null || currentLevel == null) {
            return false;
        }
        if (currentLevel.getSpecialType() == SpecialLevelType.CONVEYOR_BELT) {
            return containsNormalized(currentLevel.getConveyorPlants(), definition.getName());
        }
        try {
            validatePlantSelectionRule(definition);
            return true;
        } catch (IllegalStateException exception) {
            return false;
        }
    }

    public Map<String, Integer> getCooldownTicks() {
        return Collections.unmodifiableMap(cooldownTicks);
    }

    public int getCooldownTicks(String plantType) {
        PlantDefinition definition = plantFactory.findDefinition(plantType)
            .orElseThrow(() -> new IllegalArgumentException("Plant does not exist: " + plantType));
        return cooldownTicks.getOrDefault(definition.getNormalizedName(), 0);
    }

    public String plantStatus() {
        StringBuilder output = new StringBuilder();
        for (String name : selectedPlants) {
            PlantDefinition definition = plantFactory.findDefinition(name).orElseThrow();
            int remaining = cooldownTicks.getOrDefault(definition.getNormalizedName(), 0);
            int level = plantLevels.getOrDefault(definition.getNormalizedName(), 1);
            Plant preview = plantFactory.createPlant(name, level);
            output.append(name).append(": level=").append(preview.getPlantLevel())
                .append(", cost=").append(preview.getSunCost())
                .append(", damage=").append(preview.getAttackPower())
                .append(", health=").append(preview.getMaxHealth())
                .append(", available=").append(remaining <= 0);
            if (remaining > 0) {
                output.append(", remaining=").append(formatSeconds(remaining)).append('s');
            }
            output.append(System.lineSeparator());
        }
        return output.toString();
    }

    public String tileStatus(int row, int col) {
        Tile tile = board.getTile(row, col);
        StringBuilder output = new StringBuilder();
        output.append("Tile ").append(display(row, col)).append(": type=")
            .append(tile.getTileType()).append(System.lineSeparator());
        Plant plant = tile.getPlant();
        if (plant == null) {
            output.append("plant: none").append(System.lineSeparator());
        } else {
            output.append("plant: ").append(plant.getName()).append(", level=")
                .append(plant.getPlantLevel()).append(", health=")
                .append(plant.getHealth()).append('/').append(plant.getMaxHealth())
                .append(", shield=").append(plant.getPlantFoodShield())
                .append(System.lineSeparator());
        }
        List<Zombie> tileZombies = tile.getZombies();
        if (tileZombies.isEmpty()) {
            output.append("zombies: none").append(System.lineSeparator());
        } else {
            output.append("zombies:").append(System.lineSeparator());
            for (Zombie zombie : tileZombies) {
                output.append("- ").append(zombie.getName()).append(", health=")
                    .append(zombie.getHealth()).append(", effectiveHealth=")
                    .append(zombie.getEffectiveHealth()).append(System.lineSeparator());
            }
        }
        return output.toString();
    }

    public String zombieInfo() {
        if (board == null || board.getZombies().isEmpty()) {
            return "No zombies are on the board.";
        }
        StringBuilder output = new StringBuilder();
        for (Zombie zombie : board.getZombies()) {
            output.append(zombie.getName()).append(':').append(System.lineSeparator())
                .append("position: ").append(zombie.getPosition()).append(System.lineSeparator())
                .append("health: ").append(zombie.getHealth()).append(System.lineSeparator())
                .append("armor:").append(System.lineSeparator());
            for (Armor armor : zombie.getArmors()) {
                output.append("  ").append(armor.getDefinition().getArmorType()).append(": ")
                    .append(armor.getHealth()).append(System.lineSeparator());
            }
            output.append("effects:").append(System.lineSeparator());
            if (zombie.isGlowing()) {
                output.append("  glowing: drops plant food on death")
                    .append(System.lineSeparator());
            }
            if (zombie.getChilledTicks() > 0) {
                output.append("  chilled: ").append(formatSeconds(zombie.getChilledTicks()))
                    .append('s').append(System.lineSeparator());
            }
        }
        return output.toString();
    }

    public String summary() {
        if (currentLevel == null) {
            return "No level is prepared.";
        }
        int waveNumber = currentWave == null ? 0 : currentWave.getWaveNumber();
        return "state=" + gameState + ", level=" + currentLevel.getLevelId()
            + ", difficulty=" + difficultyLevel
            + ", wave=" + waveNumber + "/" + currentLevel.getWaves().size()
            + ", sun=" + sunAmount + ", plantFoods=" + inventory.getPlantFoods()
            + ", ticks=" + elapsedTicks
            + ", special={" + specialStatus() + "}"
            + (conveyorCards.isEmpty() ? "" : ", conveyor=" + conveyorCards);
    }

    public GameState getGameState() {
        return gameState;
    }

    public Chapter getCurrentChapter() {
        return currentChapter;
    }

    public Level getCurrentLevel() {
        return currentLevel;
    }

    public Board getBoard() {
        return board;
    }

    public Wave getCurrentWave() {
        return currentWave;
    }

    public int getSunAmount() {
        return sunAmount;
    }

    public int getElapsedTicks() {
        return elapsedTicks;
    }

    public int getLostPlantsCount() {
        return lostPlantsCount;
    }

    public int getTotalSunCollected() { return totalSunCollected; }
    public int getZombieKillCount() { return zombieKillCount; }
    public int getExplosivePlantsUsed() { return explosivePlantsUsed; }
    public int getLawnMowerKills() { return lawnMowerKills; }
    public Map<String, Integer> getConveyorCards() {
        return Collections.unmodifiableMap(conveyorCards);
    }
    public Map<String, Integer> getPlantKillCounts() {
        return Collections.unmodifiableMap(plantKillCounts);
    }
    public int getPlantKills(String plantName) {
        return plantKillCounts.getOrDefault(PlantDefinition.normalizeKey(plantName), 0);
    }
    public int getKillsWithinThirtySeconds() { return killsWithinThirtySeconds; }
    public int getFirstColumnNoMowerKills() { return firstColumnNoMowerKills; }
    public int getSunProducerPlantsPlanted() { return sunProducerPlantsPlanted; }
    public List<String> getPlantedPlantNames() { return List.copyOf(plantedPlantNames); }
    public List<String> getPlantedPlantFamilies() { return List.copyOf(plantedPlantFamilies); }
    public boolean areZombieWavesStarted() { return zombieWavesStarted; }

    private Map<String, Integer> normalizePlantLevels(Map<String, Integer> levels) {
        LinkedHashMap<String, Integer> result = new LinkedHashMap<>();
        if (levels != null) {
            for (Map.Entry<String, Integer> entry : levels.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    result.put(PlantDefinition.normalizeKey(entry.getKey()),
                        Math.max(1, entry.getValue()));
                }
            }
        }
        return Collections.unmodifiableMap(result);
    }

    private int adjustedWaveCost(int baseCost) {
        double multiplier = difficultyLevel / 3.0;
        return Math.max(100, (int) Math.round(baseCost * multiplier));
    }

    private void configureZombieDifficultyAndDrops(Wave wave) {
        for (Zombie zombie : wave.getZombies()) {
            zombie.applyDifficulty(difficultyLevel);
            boolean glowing = zombie.getDefinition().canSpawnPlantFood()
                && random.nextInt(100) < 5;
            zombie.setGlowing(glowing);
            if (glowing) {
                addEvent("A glowing " + zombie.getName() + " joined the wave.");
            }
        }
    }

    private void initializeSeasonTerrain() {
        if (currentLevel.getSeason() == SeasonType.ANCIENT_EGYPT) {
            addRandomTombs(3, false);
        } else if (currentLevel.getSeason() == SeasonType.FROSTBITE_CAVES) {
            board.getTile(1, 4).setTileType(TileType.SLIPPERY_UP);
            board.getTile(3, 5).setTileType(TileType.SLIPPERY_DOWN);
            board.getTile(0, 6).setTileType(TileType.ICE);
            board.getTile(4, 6).setTileType(TileType.ICE);
        } else if (currentLevel.getSeason() == SeasonType.BIG_WAVE_BEACH) {
            setBeachWaterLevel(7);
            board.getTile(1, 6).setTileType(TileType.LOW_TIDE);
            board.getTile(3, 6).setTileType(TileType.LOW_TIDE);
        } else if (currentLevel.getSeason() == SeasonType.DARK_AGES) {
            addRandomTombs(3, true);
            board.getTile(1, 5).setTileType(TileType.NECROMANCY);
            board.getTile(3, 6).setTileType(TileType.NECROMANCY);
        }
    }

    private void addRandomTombs(int count, boolean mayContainRewards) {
        List<GridPosition> candidates = new ArrayList<>();
        int lastCandidateColumn = Math.min(board.getCols() - 1, 6);
        for (int row = 0; row < board.getRows(); row++) {
            for (int col = 3; col <= lastCandidateColumn; col++) {
                GridPosition position = new GridPosition(row, col);
                Tile tile = board.getTile(row, col);
                if (!tombs.containsKey(position) && tile.getPlant() == null) {
                    candidates.add(position);
                }
            }
        }
        int tombCount = Math.min(count, candidates.size());
        for (int index = 0; index < tombCount; index++) {
            GridPosition position = candidates.remove(random.nextInt(candidates.size()));
            boolean sun = mayContainRewards && random.nextInt(5) == 0;
            boolean plantFood = mayContainRewards && !sun && random.nextInt(10) == 0;
            tombs.put(position, new Tomb(position.getRow(), position.getColumn(), sun, plantFood));
            board.getTile(position.getRow(), position.getColumn()).setTileType(TileType.TOMB);
        }
    }

    private void configureWaveForSeason(Wave wave) {
        boolean finalWave = nextWaveIndex == currentLevel.getWaves().size() - 1;
        for (Zombie zombie : wave.getZombies()) {
            if (currentLevel.getSeason() == SeasonType.FROSTBITE_CAVES) {
                zombie.setIceImmune(true);
            }
            if (currentLevel.getSeason() == SeasonType.ANCIENT_EGYPT
                && finalWave && random.nextBoolean()) {
                double shifted = Math.max(4.0,
                    zombie.getPosition().getColumn() - 1 - random.nextInt(4));
                zombie.setPosition(new BoardPosition(zombie.getPosition().getRow(), shifted));
            }
        }
        if (currentLevel.getSeason() == SeasonType.BIG_WAVE_BEACH) {
            int waterStart = 6 + random.nextInt(3);
            setBeachWaterLevel(waterStart);
        }
        if (currentLevel.getSeason() == SeasonType.DARK_AGES) {
            spawnNecromancyZombie(wave);
        }
    }

    private void setBeachWaterLevel(int startColumn) {
        for (int row = 0; row < board.getRows(); row++) {
            for (int col = 0; col < board.getCols(); col++) {
                Tile tile = board.getTile(row, col);
                if (col >= startColumn) {
                    tile.setTileType(TileType.WATER);
                    Plant plant = tile.getMainPlant();
                    boolean supported = tile.getSupportPlant() != null
                        && tile.getSupportPlant().getAbility() == PlantAbility.LILY_PAD;
                    if (plant != null && !plant.getDefinition().hasTag("Water") && !supported) {
                        plant.takeDamage(Math.max(plant.getHealth(), 1));
                    }
                } else if (tile.getType() == TileType.WATER) {
                    tile.setTileType(TileType.NORMAL);
                }
            }
        }
    }

    private void spawnNecromancyZombie(Wave wave) {
        for (int row = 0; row < board.getRows(); row++) {
            for (int col = 0; col < board.getCols(); col++) {
                if (board.getTile(row, col).getType() == TileType.NECROMANCY
                    && random.nextBoolean()) {
                    Zombie zombie = zombieFactory.createZombie("Basic Zombie");
                    zombie.setPosition(new BoardPosition(row, col + 0.5));
                    wave.addZombie(zombie);
                }
            }
        }
    }

    private void applySlipperyTile(Zombie zombie) {
        BoardPosition position = zombie.getPosition();
        if (position == null) {
            return;
        }
        int col = (int) Math.floor(position.getColumn());
        if (!board.isInside(position.getRow(), col)) {
            return;
        }
        TileType type = board.getTile(position.getRow(), col).getType();
        int targetRow = position.getRow();
        if (type == TileType.SLIPPERY_UP) {
            targetRow--;
        } else if (type == TileType.SLIPPERY_DOWN) {
            targetRow++;
        }
        if (targetRow >= 0 && targetRow < board.getRows() && targetRow != position.getRow()) {
            zombie.setPosition(position.withRow(targetRow));
        }
    }

    private boolean hitTomb(Projectile projectile, double fromColumn, double toColumn) {
        int row = projectile.getPosition().getRow();
        for (Map.Entry<GridPosition, Tomb> entry : new ArrayList<>(tombs.entrySet())) {
            GridPosition position = entry.getKey();
            if (position.getRow() != row || position.getColumn() + 0.001 < fromColumn
                || position.getColumn() - 0.001 > toColumn) {
                continue;
            }
            Tomb tomb = entry.getValue();
            tomb.takeDamage(projectile.getDamage());
            projectile.deactivate();
            if (tomb.isDestroyed()) {
                board.getTile(position.getRow(), position.getColumn())
                    .setTileType(TileType.NORMAL);
                tombs.remove(position);
                if (tomb.containsSun()) {
                    board.addSun(new Sun(50, position));
                }
                if (tomb.containsPlantFood() && inventory.getPlantFoodCapacityLeft() > 0) {
                    inventory.addPlantFood(1);
                    addEvent("The tomb released a plant food; total="
                        + inventory.getPlantFoods() + ".");
                }
                addEvent("Tomb destroyed at " + position + ".");
            }
            return true;
        }
        return false;
    }

    private boolean applyAutomaticBoostIfPresent(Plant plant) {
        String name = plant.getName();
        if (levelBoostedPlants.contains(name)) {
            activatePlantFood(plant, "level boost");
            return true;
        }
        if (inventory.consumeStoredBoost(name)) {
            activatePlantFood(plant, "stored greenhouse boost");
            addEvent("Stored boost for " + name + " was consumed.");
            return true;
        }
        return false;
    }

    private void activatePlantFood(Plant plant, String source) {
        if (plant == null || plant.isDestroyed() || plant.getPosition() == null) {
            throw new IllegalStateException("Plant food cannot be applied to this plant.");
        }
        plant.usePlantFood();
        PlantAbility ability = plant.getAbility();
        if (plant.isSunProducer()) {
            activateSunProducerFood(plant);
        } else {
            switch (ability) {
                case POTATO_MINE, PRIMAL_POTATO_MINE -> armMineWithPlantFood(plant);
                case CHERRY_BOMB, GRAPESHOT, JALAPENO, DOOM_SHROOM -> {
                    explosivePlantsUsed++;
                    detonatePlant(plant, 2);
                }
                case SQUASH -> squashMultipleZombies(plant, 2);
                case TANGLE_KELP -> drownMultipleZombies(plant, 3);
                case ICEBERG_LETTUCE, ICE_SHROOM -> freezeAllZombies(plant, false);
                case WALL_NUT, TALL_NUT, ENDURIAN, EXPLODE_O_NUT, PUMPKIN,
                     SUN_BEAN -> addEvent(plant.getName() + " received reinforced armor.");
                case GARLIC -> redirectWholeLane(plant);
                case SWEET_POTATO -> {
                    plant.healToFull();
                    pullZombiesTowardSweetPotato(plant);
                }
                case TORCHWOOD -> addEvent("Torchwood ignited a blue triple-damage flame.");
                case MAGNET_SHROOM -> magnetizeAllZombies(plant);
                case LILY_PAD -> cloneLilyPads(plant);
                case SHORT_RANGE_SHROOM -> resetShortRangeShrooms();
                case CAULIPOWER -> hypnotizeRandomZombies(3);
                case ELECTRIC_BLUEBERRY -> killRandomZombies(3, plant.getName());
                case CITRON -> clearPlantLane(plant);
                case CHOMPER -> killRandomZombies(3, plant.getName());
                case FUME_SHROOM -> fumePlantFoodPush(plant);
                default -> activateGeneralOffensiveFood(plant);
            }
        }
        cleanupDestroyedEntities();
        addEvent("Plant food activated on " + plant.getName() + " from " + source + ".");
    }

    private int plantFoodSunAmount(Plant plant) {
        String normalized = plant.getDefinition().getNormalizedName();
        if (normalized.equals("twinsunflower")) {
            return 250 + plant.getSunProductionBonus();
        }
        if (normalized.equals("sunshroom") || normalized.equals("primalsunflower")) {
            return 225 + plant.getSunProductionBonus();
        }
        return 150 + plant.getSunProductionBonus();
    }

    private void plantFoodShooterVolley(Plant plant) {
        GridPosition position = plant.getPosition();
        int damage = Math.max(1, plant.getAttackPower())
            * Math.max(5, plant.getProjectileCount() * 3);
        int hits = 0;
        for (Zombie zombie : new ArrayList<>(board.getZombiesInRow(position.getRow()))) {
            if (zombie.getPosition().getColumn() + 0.001 < position.getColumn()) {
                continue;
            }
            applyPlantFoodDamage(zombie, plant, damage);
            hits++;
            if (!plant.isPiercing() && hits >= 3) {
                break;
            }
        }
        addEvent(plant.getName() + " fired a plant-food volley and hit " + hits
            + " zombie(s).");
    }

    private void plantFoodHomingStrike(Plant plant) {
        ArrayList<Zombie> targets = new ArrayList<>(board.getZombies());
        targets.removeIf(Zombie::isDead);
        targets.sort((first, second) -> Integer.compare(
            second.getEffectiveHealth(), first.getEffectiveHealth()));
        int limit = Math.min(5, targets.size());
        for (int index = 0; index < limit; index++) {
            applyPlantFoodDamage(targets.get(index), plant,
                Math.max(1, plant.getAttackPower()) * 5);
        }
        addEvent(plant.getName() + " launched " + limit + " homing plant-food strike(s).");
    }

    private void plantFoodMeleeStrike(Plant plant) {
        GridPosition center = plant.getPosition();
        int hits = 0;
        for (Zombie zombie : new ArrayList<>(board.getZombies())) {
            if (zombie.getPosition() == null) {
                continue;
            }
            int rowDistance = Math.abs(zombie.getPosition().getRow() - center.getRow());
            double columnDistance = Math.abs(zombie.getPosition().getColumn()
                - center.getColumn());
            if (rowDistance <= 1 && columnDistance <= 1.5) {
                zombie.takeDamage(Math.max(1, plant.getAttackPower()) * 5, plant.getName());
                hits++;
            }
        }
        addEvent(plant.getName() + " used a plant-food area strike on " + hits
            + " zombie(s).");
    }

    private void applyPlantFoodDamage(Zombie zombie, Plant plant, int damage) {
        ProjectileType type = plant.getProjectileElementType();
        if (type == ProjectileType.POISON) {
            zombie.takeDirectDamage(damage, plant.getName());
        } else if (type == ProjectileType.FIRE) {
            zombie.clearChill();
            zombie.takeDamage(damage, plant.getName());
        } else {
            zombie.takeDamage(damage, plant.getName());
            if (type == ProjectileType.ICE) {
                zombie.chill(plant.getChillDurationTicks());
            }
        }
    }

    private void initializeSpecialLevel() {
        SpecialLevelType type = currentLevel.getSpecialType();
        if (type == SpecialLevelType.CONVEYOR_BELT) {
            if (selectedPlants.isEmpty()) {
                autoSelectStarterPlantsForConveyor();
            }
            addConveyorCard();
            nextConveyorTick = 120;
        }
        if (type == SpecialLevelType.SAVE_OUR_SEEDS) {
            initializeProtectedPlants();
        }
        addEvent("Special rule: " + currentLevel.getSpecialRuleSummary());
    }

    private void initializeProtectedPlants() {
        List<GridPosition> positions = currentLevel.getProtectedPlantPositions();
        if (positions.isEmpty()) {
            positions = List.of(new GridPosition(0, 2), new GridPosition(2, 2),
                new GridPosition(4, 2));
        }
        for (GridPosition position : positions) {
            Plant protectedPlant = plantFactory.createPlant(currentLevel.getProtectedPlantType(),
                plantLevels.getOrDefault(PlantDefinition.normalizeKey(
                    currentLevel.getProtectedPlantType()), 1));
            board.placePlant(protectedPlant, position.getRow(), position.getColumn());
            endangeredPositions.add(position);
        }
        addEvent("Protected seed plants were placed at " + positions + ".");
    }

    private void tickConveyor() {
        if (currentLevel.getSpecialType() != SpecialLevelType.CONVEYOR_BELT
            || elapsedTicks < nextConveyorTick) {
            return;
        }
        addConveyorCard();
        nextConveyorTick += 120;
    }

    private void addConveyorCard() {
        if (selectedPlants.isEmpty()) {
            return;
        }
        List<String> options = List.copyOf(selectedPlants);
        String plant = options.get(random.nextInt(options.size()));
        conveyorCards.merge(plant, 1, Integer::sum);
        addEvent("Conveyor produced a " + plant + " card.");
    }

    private void autoSelectStarterPlantsForConveyor() {
        List<String> pool = currentLevel.getConveyorPlants().isEmpty()
            ? List.of("Sunflower", "Peashooter", "Wall-nut")
            : currentLevel.getConveyorPlants();
        for (String starter : pool) {
            PlantDefinition definition = plantFactory.findDefinition(starter).orElse(null);
            if (definition != null) {
                selectedPlants.add(definition.getName());
                cooldownTicks.put(definition.getNormalizedName(), 0);
            }
        }
    }

    private void advanceOneTick() {
        elapsedTicks++;
        tickCooldowns();
        tickConveyor();
        tickSuns();
        spawnSkySunIfNeeded();
        performPlantActions();
        moveProjectilesAndResolveHits();
        moveZombiesAndResolveCombat();
        cleanupDestroyedEntities();
        startNextWaveIfReady();
        board.refreshZombieTiles();
        evaluateGameState();
    }

    private void tickCooldowns() {
        for (Map.Entry<String, Integer> entry : cooldownTicks.entrySet()) {
            if (entry.getValue() > 0) {
                entry.setValue(entry.getValue() - 1);
            }
        }
    }

    private void tickSuns() {
        for (Sun sun : new ArrayList<>(board.getSuns())) {
            if (sun.tick()) {
                addEvent("Sun reached the ground at position " + sun.getPosition() + ".");
            }
        }
    }

    private void spawnSkySunIfNeeded() {
        if (!skySunEnabled() || elapsedTicks < nextSkySunTick) {
            return;
        }
        int roll = random.nextInt(100);
        SunType type = roll < 80 ? SunType.NORMAL : roll < 95
            ? SunType.SPECIAL : SunType.RADIOACTIVE;
        GridPosition position = new GridPosition(random.nextInt(board.getRows()),
            random.nextInt(board.getCols()));
        Sun sun = Sun.falling(type, position);
        board.addSun(sun);
        addEvent("New " + type.name().toLowerCase() + " sun is dropping at position "
            + position + ".");
        nextSkySunTick = elapsedTicks + calculateSkySunIntervalTicks();
    }

    private boolean skySunEnabled() {
        SpecialLevelType type = currentLevel.getSpecialType();
        return currentLevel.getSeason() != SeasonType.DARK_AGES
            && type != SpecialLevelType.NIGHT_OPS
            && type != SpecialLevelType.PLANT_WHAT_YOU_GET;
    }

    private void performPlantActions() {
        for (Plant plant : new ArrayList<>(board.getPlants())) {
            if (plant.isDestroyed() || plant.getPosition() == null) {
                continue;
            }
            plant.tickRuntimeState();
            warmAdjacentIce(plant);
            if (plant.isDestroyed() || !plant.isOperational()) {
                continue;
            }
            if (plant.isTrap()) {
                performTrapAction(plant);
                continue;
            }
            performPassivePlantAction(plant);
            if (!plant.tickActionTimer()) {
                continue;
            }
            if (!plant.isSunProducer() && assistDisabledPlant(plant)) {
                plant.resetActionTimer();
                continue;
            }
            if (plant.isSunProducer()) {
                produceSun(plant);
            } else {
                performActivePlantAction(plant);
                plant.resetActionTimer();
            }
        }
    }

    private void produceSun(Plant plant) {
        GridPosition position = plant.getPosition();
        if (waitingSunProducers.contains(position)
            || board.hasPlantGeneratedSunAt(position)) {
            return;
        }
        int amount;
        if (plant.getAbility() == PlantAbility.SUN_SHROOM) {
            amount = plant.getSunShroomProduction() + plant.getSunProductionBonus();
        } else if (plant instanceof Sunflower sunflower) {
            amount = sunflower.getProductionAmount();
        } else {
            amount = inferSunProduction(plant) + plant.getSunProductionBonus();
        }
        if (plant.hasDoubleSunChance() && random.nextBoolean()) {
            amount *= 2;
        }
        Sun sun = new Sun(amount, position);
        board.addSun(sun);
        waitingSunProducers.add(position);
        addEvent("Plant " + plant.getName() + " produced a " + amount
            + " sun at " + position + ".");
    }

    private int inferSunProduction(Plant plant) {
        String normalized = plant.getDefinition().getNormalizedName();
        if (normalized.equals("twinsunflower")) {
            return 100;
        }
        if (normalized.equals("primalsunflower")) {
            return 75;
        }
        return 50;
    }

    private void shootProjectiles(Plant plant) {
        GridPosition position = plant.getPosition();
        Zombie target = board.findNearestZombieAhead(position.getRow(), position.getColumn());
        if (target == null) {
            return;
        }
        if (plant.getAbility() == PlantAbility.SHORT_RANGE_SHROOM
            && target.getPosition().getColumn() - position.getColumn() > 3.5) {
            return;
        }
        int projectileCount = plant.getProjectileCount();
        int maxHits = plant.getAbility() == PlantAbility.CACTUS ? 3
            : plant.isPiercing() ? Integer.MAX_VALUE : 1;
        for (int index = 0; index < projectileCount; index++) {
            double startColumn = position.getColumn() + 0.25 - index * 0.03;
            Projectile projectile = new Projectile(plant.getEffectiveAttackPower(),
                PROJECTILE_SPEED, new BoardPosition(position.getRow(), startColumn),
                plant.getProjectileElementType(), maxHits > 1,
                plant.getChillDurationTicks(), plant.isLobber(), plant.getName(), maxHits);
            board.addProjectile(projectile);
        }
        addEvent("Plant " + plant.getName() + " fired " + projectileCount
            + " projectile(s) from " + position + ".");
    }

    private void attackHoming(Plant plant) {
        Zombie target = board.findNearestZombieAnywhere();
        if (target != null) {
            damageZombieFromPlant(target, plant, Math.max(1, plant.getEffectiveAttackPower()), false);
            addEvent("Plant " + plant.getName() + " hit " + target.getName() + ".");
        }
    }

    private void attackMelee(Plant plant) {
        GridPosition position = plant.getPosition();
        int hits = 0;
        for (Zombie zombie : new ArrayList<>(board.getZombies())) {
            if (zombie.isDead() || zombie.isHypnotized() || zombie.getPosition() == null) {
                continue;
            }
            int rowDistance = Math.abs(zombie.getPosition().getRow() - position.getRow());
            double columnDistance = Math.abs(zombie.getPosition().getColumn()
                - position.getColumn());
            boolean inRange = plant.getAbility() == PlantAbility.PHAT_BEET
                || plant.getAbility() == PlantAbility.KIWIBEAST
                ? rowDistance <= 1 && columnDistance <= 1.5
                : rowDistance == 0 && columnDistance <= 1.25;
            if (inRange) {
                damageZombieFromPlant(zombie, plant,
                    Math.max(1, plant.getEffectiveAttackPower()), false);
                hits++;
            }
        }
        if (hits > 0) {
            addEvent("Plant " + plant.getName() + " struck " + hits + " zombie(s).");
        }
    }

    private void moveProjectilesAndResolveHits() {
        Iterator<Projectile> iterator = board.getProjectiles().isEmpty()
            ? Collections.<Projectile>emptyList().iterator()
            : new ArrayList<>(board.getProjectiles()).iterator();
        while (iterator.hasNext()) {
            Projectile projectile = iterator.next();
            if (!projectile.isActive()) {
                board.removeProjectile(projectile);
                continue;
            }
            double previousColumn = projectile.moveOneTick();
            double currentColumn = projectile.getPosition().getColumn();
            if (!projectile.isLobbed() && hitTomb(projectile, previousColumn, currentColumn)) {
                board.removeProjectile(projectile);
                continue;
            }
            Zombie target = findProjectileTarget(projectile.getPosition().getRow(),
                previousColumn, currentColumn);
            if (target != null) {
                if (reflectProjectileIfNeeded(projectile, target)) {
                    board.removeProjectile(projectile);
                    continue;
                }
                int multiplier = torchwoodMultiplier(projectile, previousColumn, currentColumn);
                boolean affected = projectile.hitTarget(target, multiplier);
                if (affected) {
                    addEvent("Projectile from " + projectile.getSourcePlant() + " hit "
                        + target.getName() + " for "
                        + projectile.getDamage() * multiplier + " damage.");
                } else {
                    addEvent(target.getName() + " blocked or avoided the projectile.");
                }
            }
            if (!projectile.isActive() || currentColumn > board.getCols() + 1) {
                board.removeProjectile(projectile);
            }
        }
    }

    private Zombie findProjectileTarget(int row, double fromColumn, double toColumn) {
        Zombie target = null;
        for (Zombie zombie : board.getZombiesInRow(row)) {
            if (zombie.isHypnotized()) {
                continue;
            }
            double column = zombie.getPosition().getColumn();
            if (column + 0.001 < fromColumn || column - 0.001 > toColumn) {
                continue;
            }
            if (target == null || column < target.getPosition().getColumn()) {
                target = zombie;
            }
        }
        return target;
    }

    private void moveZombiesAndResolveCombat() {
        for (Zombie zombie : new ArrayList<>(board.getZombies())) {
            if (zombie.isDead()) {
                continue;
            }
            zombie.tickEffects();
            updateZombieEnvironmentState(zombie);
            performZombieSpecialAbility(zombie);
            if (zombie.isDead()) {
                continue;
            }
            if (zombie.isHypnotized()) {
                moveHypnotizedZombie(zombie);
                continue;
            }
            Plant blockingPlant = board.findBlockingPlant(zombie);
            if (blockingPlant != null && shouldZombieBypassPlant(zombie, blockingPlant)) {
                zombie.moveOneTick();
                continue;
            }
            if (blockingPlant != null) {
                resolveZombiePlantCombat(zombie, blockingPlant);
            } else if (!isStationaryZombie(zombie)) {
                zombie.moveOneTick();
                applySlipperyTile(zombie);
            }
            if (zombie.getPosition() != null && zombie.getPosition().getColumn() < 0) {
                handleZombieAtHouse(zombie);
            }
        }
    }

    private void handleZombieAtHouse(Zombie crossingZombie) {
        int row = crossingZombie.getPosition().getRow();
        LawnMower mower = board.getLawnMower(row);
        if (mower.trigger()) {
            ArrayList<String> killed = new ArrayList<>();
            for (Zombie zombie : new ArrayList<>(board.getZombiesInRow(row))) {
                if (!zombie.isBoss()) {
                    zombie.kill();
                    killed.add(zombie.getName());
                    lawnMowerKills++;
                }
            }
            addEvent("The lawn mower in row " + (row + 1)
                + " was triggered and killed: " + String.join(", ", killed) + ".");
        } else {
            gameState = GameState.LOST;
            addEvent("The zombie ate your brain; LOSER!!!");
        }
    }

    private void cleanupDestroyedEntities() {
        for (Plant plant : new ArrayList<>(board.getPlants())) {
            if (!plant.isDestroyed() || plant.getPosition() == null) {
                continue;
            }
            GridPosition position = plant.getPosition();
            if (plant.getAbility() == PlantAbility.EXPLODE_O_NUT) {
                explodeDestroyedDefender(plant);
            }
            boolean endangered = endangeredPositions.contains(position)
                && board.getTile(position.getRow(), position.getColumn()).getMainPlant() == plant;
            board.removePlant(plant);
            waitingSunProducers.remove(position);
            if (endangered) {
                board.setEndangeredPlantsEaten(true);
            }
            lostPlantsCount++;
            addEvent("Plant " + plant.getName() + " at " + position + " is destroyed.");
        }
        removeUnsupportedWaterPlants();
        for (Zombie zombie : new ArrayList<>(board.getZombies())) {
            if (!zombie.isDead()) {
                continue;
            }
            BoardPosition position = zombie.getPosition();
            releaseWizardTransformations(zombie.getRuntimeId());
            dropStolenSunFromZombie(zombie);
            handleZombieRewards(zombie);
            recordZombieKillStatistics(zombie, position);
            board.removeZombie(zombie);
            zombieKillCount++;
            addEvent("Zombie of type " + zombie.getName() + " is dead at " + position + ".");
        }
    }

    private void recordZombieKillStatistics(Zombie zombie, BoardPosition position) {
        String sourcePlant = zombie.getLastDamageSourcePlant();
        if (sourcePlant != null && !sourcePlant.isBlank()) {
            plantKillCounts.merge(PlantDefinition.normalizeKey(sourcePlant), 1, Integer::sum);
        }
        if (elapsedTicks <= 30 * TICKS_PER_SECOND) {
            killsWithinThirtySeconds++;
        }
        if (position != null && position.getColumn() >= 0 && position.getColumn() < 1.0) {
            int row = position.getRow();
            if (row >= 0 && row < board.getRows()
                && board.getLawnMower(row).isActivated()) {
                firstColumnNoMowerKills++;
            }
        }
    }

    private void handleZombieRewards(Zombie zombie) {
        if (zombie.isRewardDropped()) {
            return;
        }
        zombie.dropReward();
        if (zombie.isGlowing()) {
            if (inventory.getPlantFoodCapacityLeft() > 0) {
                inventory.addPlantFood(1);
                addEvent("The glowing zombie dropped a plant food; you have "
                    + inventory.getPlantFoods() + " plant foods now.");
            } else {
                addEvent("The glowing zombie dropped plant food, but storage is full.");
            }
        }
        if (random.nextInt(100) >= 10) {
            return;
        }
        int rewardType = random.nextInt(3);
        if (rewardType == 0) {
            wallet.addCoins(50);
            addEvent("A zombie dropped 50 coins; you have " + wallet.getCoins()
                + " coins now.");
        } else if (rewardType == 1) {
            wallet.addGems(1);
            addEvent("A zombie dropped a diamond; you have " + wallet.getGems()
                + " diamonds now.");
        } else {
            inventory.addPot();
            addEvent("A zombie dropped a pot; you have " + inventory.getPots()
                + " pots now.");
        }
    }

    private void startNextWaveIfReady() {
        if (!zombieWavesStarted || nextWaveIndex >= currentLevel.getWaves().size()
            || currentWave == null) {
            return;
        }
        if (currentWave.hasLostAtLeastSeventyFivePercentHealth()) {
            startNextWave();
        }
    }

    private void evaluateGameState() {
        if (gameState != GameState.RUNNING) {
            return;
        }
        if (specialLoseConditionReached()) {
            gameState = GameState.LOST;
            addEvent("The special level lose condition was reached: " + specialStatus());
            return;
        }
        if (specialWinConditionReached() || checkWinCondition()) {
            gameState = GameState.WON;
            currentLevel.completeLevel();
            addEvent("Dear humanz, zis is not done yet; we will come back to eat your brainz, humanz.");
        }
    }

    private boolean specialWinConditionReached() {
        if (currentLevel.getSpecialType() != SpecialLevelType.TIMED_WAR) {
            return false;
        }
        return timedWarProgress() >= currentLevel.getTimedWarTarget();
    }

    private boolean specialLoseConditionReached() {
        SpecialLevelType type = currentLevel.getSpecialType();
        if (type == SpecialLevelType.DEAD_LINE) {
            return board.hasZombiesCrossedColumn(currentLevel.getDeadLineColumn());
        }
        if (type == SpecialLevelType.SAVE_OUR_SEEDS) {
            return board.areEndangeredPlantsEaten();
        }
        if (type == SpecialLevelType.LOVE_YOUR_PLANTS) {
            return lostPlantsCount >= currentLevel.getAllowedPlantLosses();
        }
        if (type == SpecialLevelType.TIMED_WAR) {
            int limitTicks = currentLevel.getTimeLimitSeconds() * TICKS_PER_SECOND;
            return elapsedTicks >= limitTicks
                && timedWarProgress() < currentLevel.getTimedWarTarget();
        }
        return false;
    }

    private int timedWarProgress() {
        if (currentLevel.getTimedWarObjective() == TimedWarObjective.SUN) {
            return totalSunCollected;
        }
        return zombieKillCount;
    }

    public String specialStatus() {
        if (currentLevel == null) {
            return "no level";
        }
        return switch (currentLevel.getSpecialType()) {
            case NORMAL -> "normal";
            case CONVEYOR_BELT -> "cards=" + conveyorCards;
            case LOCKED_PLANTS -> "forced=" + currentLevel.getForcedPlants()
                + ", locked=" + currentLevel.getLockedPlants() + ", representatives="
                + currentLevel.getFamilyRepresentativePlants();
            case SAVE_OUR_SEEDS -> "protected remaining=" + protectedPlantsRemaining()
                + "/" + endangeredPositions.size();
            case TIMED_WAR -> "progress=" + timedWarProgress() + "/"
                + currentLevel.getTimedWarTarget() + ", time="
                + formatSeconds(Math.max(0, currentLevel.getTimeLimitSeconds()
                * TICKS_PER_SECOND - elapsedTicks)) + "s";
            case NIGHT_OPS -> "sky sun disabled";
            case DEAD_LINE -> "line column=" + (currentLevel.getDeadLineColumn() + 1);
            case LOVE_YOUR_PLANTS -> "lost plants=" + lostPlantsCount + "/"
                + currentLevel.getAllowedPlantLosses();
            case PLANT_WHAT_YOU_GET -> "waves started=" + zombieWavesStarted
                + ", remaining sun=" + sunAmount;
        };
    }

    private int protectedPlantsRemaining() {
        int count = 0;
        for (GridPosition position : endangeredPositions) {
            Plant plant = board.getTile(position.getRow(), position.getColumn()).getMainPlant();
            if (plant != null && !plant.isDestroyed()) {
                count++;
            }
        }
        return count;
    }

    private void handleImmediatePlant(Plant plant) {
        PlantAbility ability = plant.getAbility();
        if (ability == PlantAbility.GOLD_BLOOM) {
            sunAmount += 375;
            removeInstantPlant(plant);
            addEvent("Gold Bloom produced 375 suns and disappeared.");
        } else if (ability.isMint()) {
            activateMint(plant);
        } else if (ability == PlantAbility.ICE_SHROOM) {
            freezeAllZombies(plant, false);
            removeInstantPlant(plant);
        } else if (ability == PlantAbility.CHERRY_BOMB
            || ability == PlantAbility.GRAPESHOT
            || ability == PlantAbility.JALAPENO
            || ability == PlantAbility.DOOM_SHROOM) {
            explosivePlantsUsed++;
            detonatePlant(plant);
        }
    }

    private void detonatePlant(Plant plant) {
        detonatePlant(plant, 1);
    }

    private void detonatePlant(Plant plant, int damageMultiplier) {
        GridPosition center = plant.getPosition();
        if (center == null) {
            return;
        }
        int baseDamage = plant.getDefinition().isInstantKill()
            ? Integer.MAX_VALUE / 4 : Math.max(1, plant.getEffectiveAttackPower());
        int damage = baseDamage * Math.max(1, damageMultiplier);
        PlantAbility ability = plant.getAbility();
        for (Zombie zombie : new ArrayList<>(board.getZombies())) {
            if (zombie.isDead() || zombie.isHypnotized() || zombie.getPosition() == null) {
                continue;
            }
            int rowDistance = Math.abs(zombie.getPosition().getRow() - center.getRow());
            double columnDistance = Math.abs(zombie.getPosition().getColumn() - center.getColumn());
            if (explosionHits(ability, rowDistance, columnDistance)) {
                if (ability == PlantAbility.JALAPENO) {
                    zombie.clearChill();
                }
                damageZombieFromPlant(zombie, plant, damage, false);
            }
        }
        if (ability == PlantAbility.GRAPESHOT) {
            launchGrapeshotFragments(plant, damageMultiplier);
        }
        if (ability == PlantAbility.DOOM_SHROOM) {
            board.getTile(center.getRow(), center.getColumn()).setTileType(TileType.CRATER);
        }
        plant.takeDamage(Math.max(plant.getHealth(), 1));
        addEvent("Plant " + plant.getName() + " activated at " + center + ".");
    }

    private void explodeRadioactiveSun(Sun sun) {
        GridPosition center = sun.getPosition();
        for (Zombie zombie : new ArrayList<>(board.getZombies())) {
            if (zombie.getPosition() == null) {
                continue;
            }
            int rowDistance = Math.abs(zombie.getPosition().getRow() - center.getRow());
            double columnDistance = Math.abs(zombie.getPosition().getColumn() - center.getColumn());
            if (rowDistance <= 2 && columnDistance <= 2.5) {
                zombie.takeDamage(150);
            }
        }
        for (Plant plant : new ArrayList<>(board.getPlants())) {
            GridPosition position = plant.getPosition();
            if (Math.abs(position.getRow() - center.getRow()) <= 1
                && Math.abs(position.getColumn() - center.getColumn()) <= 1) {
                plant.takeDamage(80);
            }
        }
        sun.collect();
    }

    private void activateSunProducerFood(Plant plant) {
        if (plant.getAbility() == PlantAbility.SUN_SHROOM) {
            plant.matureFully();
        }
        int amount = plantFoodSunAmount(plant);
        sunAmount += amount;
        addEvent("Plant food made " + plant.getName() + " produce " + amount
            + " suns immediately.");
    }

    private void armMineWithPlantFood(Plant plant) {
        addEvent(plant.getName() + " armed immediately.");
        GridPosition center = plant.getPosition();
        int clones = 0;
        for (int row = Math.max(0, center.getRow() - 1);
             row <= Math.min(board.getRows() - 1, center.getRow() + 1) && clones < 2; row++) {
            for (int col = Math.max(0, center.getColumn() - 1);
                 col <= Math.min(board.getCols() - 1, center.getColumn() + 1) && clones < 2; col++) {
                if (row == center.getRow() && col == center.getColumn()) {
                    continue;
                }
                Tile tile = board.getTile(row, col);
                if (tile.getPlant() == null && tile.getType().isPlantable()) {
                    Plant clone = plantFactory.createPlant(plant.getName(), plant.getPlantLevel());
                    clone.usePlantFood();
                    board.placePlant(clone, row, col);
                    clones++;
                }
            }
        }
        addEvent(plant.getName() + " created " + clones + " armed clone(s).");
    }

    private void squashMultipleZombies(Plant plant, int count) {
        ArrayList<Zombie> targets = hostileZombies();
        int killed = 0;
        while (!targets.isEmpty() && killed < count) {
            Zombie target = targets.remove(random.nextInt(targets.size()));
            target.kill(plant.getName());
            killed++;
        }
        plant.takeDamage(Math.max(plant.getHealth(), 1));
        addEvent("Squash crushed " + killed + " zombie(s) with plant food.");
    }

    private void drownMultipleZombies(Plant plant, int count) {
        ArrayList<Zombie> waterTargets = new ArrayList<>();
        for (Zombie zombie : hostileZombies()) {
            int col = (int) Math.floor(zombie.getPosition().getColumn());
            if (board.isInside(zombie.getPosition().getRow(), col)) {
                TileType type = board.getTile(zombie.getPosition().getRow(), col).getType();
                if (type == TileType.WATER || type == TileType.LOW_TIDE) {
                    waterTargets.add(zombie);
                }
            }
        }
        int killed = 0;
        while (!waterTargets.isEmpty() && killed < count) {
            Zombie target = waterTargets.remove(random.nextInt(waterTargets.size()));
            target.kill(plant.getName());
            killed++;
        }
        addEvent("Tangle Kelp drowned " + killed + " zombie(s).");
    }

    private void redirectWholeLane(Plant plant) {
        int row = plant.getPosition().getRow();
        int redirected = 0;
        for (Zombie zombie : new ArrayList<>(board.getZombiesInRow(row))) {
            if (!zombie.isHypnotized()) {
                moveZombieToAdjacentLane(zombie);
                redirected++;
            }
        }
        addEvent("Garlic redirected " + redirected + " zombie(s) from its lane.");
    }

    private void magnetizeAllZombies(Plant plant) {
        int removed = 0;
        for (Zombie zombie : hostileZombies()) {
            removed += zombie.removeMetalArmor();
        }
        addEvent("Magnet-shroom removed " + removed + " total metal armor health.");
    }

    private void cloneLilyPads(Plant plant) {
        int created = 0;
        for (int row = 0; row < board.getRows() && created < 3; row++) {
            for (int col = 0; col < board.getCols() && created < 3; col++) {
                Tile tile = board.getTile(row, col);
                boolean water = tile.getType() == TileType.WATER
                    || tile.getType() == TileType.LOW_TIDE;
                if (water && tile.getSupportPlant() == null && tile.getMainPlant() == null) {
                    Plant clone = plantFactory.createPlant("Lily Pad", plant.getPlantLevel());
                    board.placePlant(clone, row, col);
                    created++;
                }
            }
        }
        addEvent("Lily Pad created " + created + " copy/copies.");
    }

    private void resetShortRangeShrooms() {
        int reset = 0;
        for (Plant plant : board.getPlants()) {
            if (plant.getAbility() == PlantAbility.SHORT_RANGE_SHROOM) {
                plant.restoreLifetime();
                reset++;
            }
        }
        addEvent("Plant food reset the lifetime of " + reset + " short-range shroom(s).");
    }

    private void hypnotizeRandomZombies(int count) {
        ArrayList<Zombie> targets = hostileZombies();
        int affected = 0;
        while (!targets.isEmpty() && affected < count) {
            Zombie target = targets.remove(random.nextInt(targets.size()));
            target.hypnotize();
            affected++;
        }
        addEvent("Plant food hypnotized " + affected + " zombie(s).");
    }

    private void killRandomZombies(int count, String sourceName) {
        ArrayList<Zombie> targets = hostileZombies();
        int killed = 0;
        while (!targets.isEmpty() && killed < count) {
            Zombie target = targets.remove(random.nextInt(targets.size()));
            target.kill(sourceName);
            killed++;
        }
        addEvent(sourceName + " eliminated " + killed + " zombie(s).");
    }

    private void clearPlantLane(Plant plant) {
        int row = plant.getPosition().getRow();
        int killed = 0;
        for (Zombie zombie : new ArrayList<>(board.getZombiesInRow(row))) {
            if (!zombie.isHypnotized()) {
                zombie.kill(plant.getName());
                killed++;
            }
        }
        addEvent("Citron's plasma ball cleared " + killed + " zombie(s) from its lane.");
    }

    private void fumePlantFoodPush(Plant plant) {
        int row = plant.getPosition().getRow();
        int pushed = 0;
        for (Zombie zombie : new ArrayList<>(board.getZombiesInRow(row))) {
            if (!zombie.isHypnotized()) {
                zombie.takeDamage(Math.max(1, plant.getEffectiveAttackPower()) * 5, plant.getName());
                zombie.setPosition(zombie.getPosition().moveHorizontal(1.5));
                pushed++;
            }
        }
        addEvent("Fume-shroom pushed " + pushed + " zombie(s) backward.");
    }

    private void activateGeneralOffensiveFood(Plant plant) {
        if (plant.isShooter()) {
            if (plant.getAbility() == PlantAbility.THREEPEATER
                || plant.getAbility() == PlantAbility.STARFRUIT
                || plant.getAbility() == PlantAbility.ROTOBAGA) {
                for (int index = 0; index < 5; index++) {
                    performActivePlantAction(plant);
                }
            } else {
                plantFoodShooterVolley(plant);
            }
        } else if (plant.isHoming()) {
            plantFoodHomingStrike(plant);
        } else if (plant.isMelee()) {
            plantFoodMeleeStrike(plant);
        } else {
            addEvent("Plant food fully healed " + plant.getName()
                + " and granted a " + plant.getPlantFoodShield() + " point shield.");
        }
    }

    private void ensureConveyorCardAvailable(PlantDefinition definition, boolean conveyor) {
        if (conveyor && conveyorCards.getOrDefault(definition.getName(), 0) <= 0) {
            throw new IllegalStateException("No conveyor card is available for this plant.");
        }
    }

    private Plant createPlantForPlacement(PlantDefinition definition, int level) {
        if (PlantAbility.fromDefinition(definition) != PlantAbility.IMITATER) {
            return plantFactory.createPlant(definition.getName(), level);
        }
        for (String selected : selectedPlants) {
            PlantDefinition candidate = plantFactory.findDefinition(selected).orElse(null);
            if (candidate != null && PlantAbility.fromDefinition(candidate) != PlantAbility.IMITATER) {
                addEvent("Imitater copied " + candidate.getName() + ".");
                return plantFactory.createPlant(candidate.getName(),
                    plantLevels.getOrDefault(candidate.getNormalizedName(), 1));
            }
        }
        throw new IllegalStateException("Imitater needs another selected plant to copy.");
    }

    private void finishPlantPurchase(String cooldownKey, String selectedPlantName,
                                     Plant plant, boolean conveyor) {
        if (conveyor) {
            int cards = conveyorCards.getOrDefault(selectedPlantName, 0);
            conveyorCards.put(selectedPlantName, Math.max(0, cards - 1));
            return;
        }
        sunAmount -= plant.getSunCost();
        cooldownTicks.put(cooldownKey, isPreWaveSetup() ? 0 : plant.getRechargeTicks());
    }

    private boolean handleTerrainUtilityPlant(Plant plant, int row, int col) {
        GridPosition position = new GridPosition(row, col);
        Tile tile = board.getTile(row, col);
        if (plant.getAbility() == PlantAbility.HOT_POTATO) {
            if (tile.getType() != TileType.ICE) {
                throw new IllegalStateException("Hot Potato can only be used on ice.");
            }
            tile.setTileType(TileType.NORMAL);
            addEvent("Hot Potato melted the ice at " + position + ".");
            return true;
        }
        if (plant.getAbility() == PlantAbility.GRAVE_BUSTER) {
            Tomb tomb = tombs.remove(position);
            if (tomb == null) {
                throw new IllegalStateException("There is no tomb on this tile.");
            }
            tile.setTileType(TileType.NORMAL);
            addEvent("Grave Buster removed the tomb at " + position + ".");
            return true;
        }
        return false;
    }

    private void validateSpecialPlantLocation(Plant plant, int row, int col) {
        Tile tile = board.getTile(row, col);
        if (plant.getAbility() == PlantAbility.TANGLE_KELP
            && tile.getType() != TileType.WATER && tile.getType() != TileType.LOW_TIDE) {
            throw new IllegalStateException("Tangle Kelp can only be planted in water.");
        }
    }

    private void warmAdjacentIce(Plant plant) {
        if (!plant.getDefinition().hasTag("Fire") || elapsedTicks % TICKS_PER_SECOND != 0) {
            return;
        }
        GridPosition center = plant.getPosition();
        for (Plant other : board.getPlants()) {
            if (other == plant || other.getPosition() == null) {
                continue;
            }
            GridPosition position = other.getPosition();
            if (Math.abs(position.getRow() - center.getRow()) <= 1
                && Math.abs(position.getColumn() - center.getColumn()) <= 1) {
                other.damageIce(60, false);
            }
        }
        for (int row = Math.max(0, center.getRow() - 1);
             row <= Math.min(board.getRows() - 1, center.getRow() + 1); row++) {
            for (int col = Math.max(0, center.getColumn() - 1);
                 col <= Math.min(board.getCols() - 1, center.getColumn() + 1); col++) {
                if (board.getTile(row, col).getType() == TileType.ICE) {
                    board.getTile(row, col).setTileType(TileType.NORMAL);
                }
            }
        }
    }

    private boolean assistDisabledPlant(Plant helper) {
        Plant target = null;
        double bestDistance = Double.MAX_VALUE;
        for (Plant plant : board.getPlants()) {
            if (plant == helper || plant.getPosition() == null || plant.isDestroyed()) {
                continue;
            }
            if (plant.getFrozenHealth() <= 0 && plant.getOctopusHealth() <= 0) {
                continue;
            }
            int rowDistance = Math.abs(plant.getPosition().getRow()
                - helper.getPosition().getRow());
            double columnDistance = Math.abs(plant.getPosition().getColumn()
                - helper.getPosition().getColumn());
            if (!helper.isLobber() && rowDistance != 0) {
                continue;
            }
            double distance = rowDistance * board.getCols() + columnDistance;
            if (distance < bestDistance) {
                bestDistance = distance;
                target = plant;
            }
        }
        if (target == null) {
            return false;
        }
        int damage = Math.max(1, helper.getEffectiveAttackPower());
        if (target.getFrozenHealth() > 0) {
            target.damageIce(damage, helper.getDefinition().hasTag("Fire"));
            addEvent(helper.getName() + " damaged the ice covering " + target.getName() + ".");
        } else {
            target.damageOctopus(damage);
            addEvent(helper.getName() + " damaged the octopus covering " + target.getName() + ".");
        }
        return true;
    }

    private void performTrapAction(Plant plant) {
        if (!plant.isArmed()) {
            return;
        }
        GridPosition position = plant.getPosition();
        Zombie target = board.findNearestZombieAhead(position.getRow(), position.getColumn() - 0.5);
        if (target == null || target.getPosition().getColumn() > position.getColumn() + 0.85) {
            return;
        }
        switch (plant.getAbility()) {
            case ICEBERG_LETTUCE -> {
                target.stun(5 * TICKS_PER_SECOND);
                target.chill(10 * TICKS_PER_SECOND);
                plant.takeDamage(Math.max(plant.getHealth(), 1));
                addEvent("Iceberg Lettuce froze " + target.getName() + ".");
            }
            case TANGLE_KELP -> {
                target.kill(plant.getName());
                plant.takeDamage(Math.max(plant.getHealth(), 1));
                addEvent("Tangle Kelp pulled " + target.getName() + " underwater.");
            }
            case SQUASH -> {
                target.kill(plant.getName());
                plant.takeDamage(Math.max(plant.getHealth(), 1));
                addEvent("Squash crushed " + target.getName() + ".");
            }
            default -> detonatePlant(plant);
        }
    }

    private void performPassivePlantAction(Plant plant) {
        if (elapsedTicks % TICKS_PER_SECOND != 0) {
            return;
        }
        if (plant.getAbility() == PlantAbility.SWEET_POTATO) {
            pullZombiesTowardSweetPotato(plant);
        }
    }

    private void performActivePlantAction(Plant plant) {
        switch (plant.getAbility()) {
            case THREEPEATER -> fireThreepeater(plant);
            case ROTOBAGA -> fireRotobaga(plant);
            case SPLIT_PEA -> fireSplitPea(plant);
            case STARFRUIT -> fireStarfruit(plant);
            case BOWLING_BULB -> bowlBulbs(plant);
            case FUME_SHROOM -> attackFumeShroom(plant);
            case CABBAGE_PULT, KERNEL_PULT, MELON_PULT,
                 WINTER_MELON, PEPPER_PULT -> attackLobber(plant);
            case CAULIPOWER -> hypnotizeWithCaulipower(plant);
            case ELECTRIC_BLUEBERRY -> strikeWithBlueberry(plant);
            case MAGNET_SHROOM -> useMagnetShroom(plant);
            case CHOMPER -> chompZombie(plant);
            case CAT_TAIL -> attackHoming(plant);
            case BONK_CHOY, PHAT_BEET, WASABI_WHIP, KIWIBEAST -> attackMelee(plant);
            case TORCHWOOD, WALL_NUT, TALL_NUT, ENDURIAN, GARLIC,
                 SWEET_POTATO, EXPLODE_O_NUT, PUMPKIN, SUN_BEAN,
                 HYPNO_SHROOM, LILY_PAD, IMITATER, GENERIC -> { }
            default -> {
                if (plant.isHoming()) {
                    attackHoming(plant);
                } else if (plant.isMelee()) {
                    attackMelee(plant);
                } else if (plant.isShooter()) {
                    shootProjectiles(plant);
                }
            }
        }
    }

    private void fireThreepeater(Plant plant) {
        int centerRow = plant.getPosition().getRow();
        int fired = 0;
        for (int row = Math.max(0, centerRow - 1);
             row <= Math.min(board.getRows() - 1, centerRow + 1); row++) {
            if (fireProjectileInRow(plant, row, 1, 1)) {
                fired++;
            }
        }
        if (fired > 0) {
            addEvent("Threepeater fired into " + fired + " lane(s).");
        }
    }

    private void fireRotobaga(Plant plant) {
        GridPosition position = plant.getPosition();
        int hits = 0;
        for (int row : List.of(position.getRow() - 1, position.getRow() + 1)) {
            if (row < 0 || row >= board.getRows()) {
                continue;
            }
            Zombie target = board.findNearestZombieAhead(row, position.getColumn());
            if (target != null) {
                damageZombieFromPlant(target, plant,
                    Math.max(1, plant.getEffectiveAttackPower()) * 3, false);
                hits++;
            }
        }
        addEvent("Rotobaga hit " + hits + " diagonal target(s).");
    }

    private void fireSplitPea(Plant plant) {
        GridPosition position = plant.getPosition();
        Zombie ahead = board.findNearestZombieAhead(position.getRow(), position.getColumn());
        Zombie behind = board.findNearestZombieBehind(position.getRow(), position.getColumn());
        if (ahead != null) {
            damageZombieFromPlant(ahead, plant, Math.max(1, plant.getEffectiveAttackPower()), false);
        }
        if (behind != null) {
            damageZombieFromPlant(behind, plant,
                Math.max(1, plant.getEffectiveAttackPower()) * 2, false);
        }
    }

    private void fireStarfruit(Plant plant) {
        GridPosition position = plant.getPosition();
        LinkedHashSet<Zombie> targets = new LinkedHashSet<>();
        Zombie ahead = board.findNearestZombieAhead(position.getRow(), position.getColumn());
        Zombie behind = board.findNearestZombieBehind(position.getRow(), position.getColumn());
        if (ahead != null) {
            targets.add(ahead);
        }
        if (behind != null) {
            targets.add(behind);
        }
        for (int row : List.of(position.getRow() - 1, position.getRow() + 1)) {
            if (row >= 0 && row < board.getRows()) {
                Zombie diagonal = board.findNearestZombieAhead(row, position.getColumn());
                if (diagonal != null) {
                    targets.add(diagonal);
                }
            }
        }
        for (Zombie target : targets) {
            damageZombieFromPlant(target, plant, Math.max(1, plant.getEffectiveAttackPower()), false);
        }
        addEvent("Starfruit fired in multiple directions and hit " + targets.size() + " target(s).");
    }

    private void bowlBulbs(Plant plant) {
        GridPosition position = plant.getPosition();
        int[] damages = {40, 120, 180};
        int row = position.getRow();
        int hits = 0;
        for (int damage : damages) {
            Zombie target = board.findNearestZombieAhead(row, position.getColumn());
            if (target == null) {
                break;
            }
            damageZombieFromPlant(target, plant, damage, false);
            hits++;
            row += random.nextBoolean() ? 1 : -1;
            row = Math.max(0, Math.min(board.getRows() - 1, row));
        }
        addEvent("Bowling Bulb bounced through " + hits + " target(s).");
    }

    private void attackFumeShroom(Plant plant) {
        GridPosition position = plant.getPosition();
        int hits = 0;
        for (Zombie zombie : new ArrayList<>(board.getZombiesInRow(position.getRow()))) {
            double distance = zombie.getPosition().getColumn() - position.getColumn();
            if (!zombie.isHypnotized() && distance >= 0 && distance <= 4.0) {
                damageZombieFromPlant(zombie, plant,
                    Math.max(1, plant.getEffectiveAttackPower()), false);
                hits++;
            }
        }
        if (hits > 0) {
            addEvent("Fume-shroom pierced " + hits + " zombie(s).");
        }
    }

    private void attackLobber(Plant plant) {
        GridPosition position = plant.getPosition();
        Zombie target = board.findNearestZombieAhead(position.getRow(), position.getColumn());
        if (target == null) {
            return;
        }
        int damage = Math.max(1, plant.getEffectiveAttackPower());
        if (plant.getAbility() == PlantAbility.KERNEL_PULT && random.nextInt(4) == 0) {
            damage = Math.max(damage, 40);
            target.stun(3 * TICKS_PER_SECOND);
            addEvent("Kernel-pult butter stunned " + target.getName() + ".");
        }
        damageZombieFromPlant(target, plant, damage, true);
        if (plant.getDefinition().hasTag("AoE")) {
            damageAdjacentZombies(target, plant, Math.max(1, damage / 2), true);
        }
    }

    private void hypnotizeWithCaulipower(Plant plant) {
        ArrayList<Zombie> targets = hostileZombies();
        if (targets.isEmpty()) {
            return;
        }
        Zombie target = targets.get(random.nextInt(targets.size()));
        target.hypnotize();
        addEvent("Caulipower hypnotized " + target.getName() + ".");
    }

    private void strikeWithBlueberry(Plant plant) {
        ArrayList<Zombie> targets = hostileZombies();
        if (targets.isEmpty()) {
            return;
        }
        Zombie target = targets.get(random.nextInt(targets.size()));
        target.kill(plant.getName());
        addEvent("Electric Blueberry electrocuted " + target.getName() + ".");
    }

    private void useMagnetShroom(Plant plant) {
        Zombie target = null;
        for (Zombie zombie : hostileZombies()) {
            if (zombie.hasMetalArmor()) {
                target = zombie;
                break;
            }
        }
        if (target != null) {
            int removed = target.removeMetalArmor();
            addEvent("Magnet-shroom removed " + removed + " armor health from "
                + target.getName() + ".");
        }
    }

    private void chompZombie(Plant plant) {
        GridPosition position = plant.getPosition();
        Zombie target = board.findNearestZombieAhead(position.getRow(), position.getColumn() - 0.5);
        if (target != null && target.getPosition().getColumn() <= position.getColumn() + 1.25) {
            target.kill(plant.getName());
            plant.startDigestion(40 * TICKS_PER_SECOND);
            addEvent("Chomper swallowed " + target.getName() + " and started digesting.");
        }
    }

    private boolean fireProjectileInRow(Plant plant, int row, int count, int maxHits) {
        Zombie target = board.findNearestZombieAhead(row, plant.getPosition().getColumn());
        if (target == null) {
            return false;
        }
        for (int index = 0; index < count; index++) {
            Projectile projectile = new Projectile(plant.getEffectiveAttackPower(),
                PROJECTILE_SPEED, new BoardPosition(row, plant.getPosition().getColumn() + 0.25),
                plant.getProjectileElementType(), maxHits > 1,
                plant.getChillDurationTicks(), false, plant.getName(), maxHits);
            board.addProjectile(projectile);
        }
        return true;
    }

    private void pullZombiesTowardSweetPotato(Plant plant) {
        int targetRow = plant.getPosition().getRow();
        for (Zombie zombie : hostileZombies()) {
            if (zombie.getPosition() == null) {
                continue;
            }
            int row = zombie.getPosition().getRow();
            double distance = Math.abs(zombie.getPosition().getColumn()
                - plant.getPosition().getColumn());
            if (Math.abs(row - targetRow) == 1 && distance <= 3.0) {
                zombie.setPosition(zombie.getPosition().withRow(targetRow));
            }
        }
    }

    private void damageAdjacentZombies(Zombie center, Plant source, int damage, boolean lobbed) {
        if (center.getPosition() == null) {
            return;
        }
        for (Zombie zombie : hostileZombies()) {
            if (zombie == center || zombie.getPosition() == null) {
                continue;
            }
            if (Math.abs(zombie.getPosition().getRow() - center.getPosition().getRow()) <= 1
                && Math.abs(zombie.getPosition().getColumn()
                    - center.getPosition().getColumn()) <= 1.5) {
                damageZombieFromPlant(zombie, source, damage, lobbed);
            }
        }
    }

    private void damageZombieFromPlant(Zombie zombie, Plant plant, int damage, boolean lobbed) {
        if (zombie == null || zombie.isDead()) {
            return;
        }
        zombie.takeProjectileDamage(Math.max(0, damage), plant.getProjectileElementType(),
            plant.getChillDurationTicks(), lobbed, plant.getName());
    }

    private ArrayList<Zombie> hostileZombies() {
        ArrayList<Zombie> result = new ArrayList<>();
        for (Zombie zombie : board.getZombies()) {
            if (!zombie.isDead() && !zombie.isHypnotized() && zombie.getPosition() != null) {
                result.add(zombie);
            }
        }
        return result;
    }

    private boolean reflectProjectileIfNeeded(Projectile projectile, Zombie target) {
        if (target.getAbility() != ZombieAbility.JUGGLER || projectile.isLobbed()) {
            return false;
        }
        Plant victim = nearestPlantInRow(target.getPosition().getRow(),
            target.getPosition().getColumn());
        if (victim != null) {
            victim.takeDamage(projectile.getDamage());
            addEvent("Juggler Zombie reflected a projectile into " + victim.getName() + ".");
        }
        projectile.deactivate();
        return true;
    }

    private Plant nearestPlantInRow(int row, double zombieColumn) {
        Plant nearest = null;
        double best = Double.MAX_VALUE;
        for (Plant plant : board.getPlantsInRow(row)) {
            if (plant.getPosition() == null || plant.isDestroyed()) {
                continue;
            }
            double distance = Math.abs(plant.getPosition().getColumn() - zombieColumn);
            if (distance < best) {
                best = distance;
                nearest = plant;
            }
        }
        return nearest;
    }

    private int torchwoodMultiplier(Projectile projectile, double fromColumn, double toColumn) {
        String source = PlantDefinition.normalizeKey(projectile.getSourcePlant());
        if (!source.contains("pea") || projectile.getType() == ProjectileType.FIRE) {
            return 1;
        }
        int row = projectile.getPosition().getRow();
        for (Plant plant : board.getPlantsInRow(row)) {
            if (plant.getAbility() != PlantAbility.TORCHWOOD || plant.getPosition() == null) {
                continue;
            }
            int column = plant.getPosition().getColumn();
            if (column - 0.001 <= toColumn) {
                return plant.getPlantFoodShield() > 0 ? 3 : 2;
            }
        }
        return 1;
    }

    private void updateZombieEnvironmentState(Zombie zombie) {
        if (zombie.getPosition() == null) {
            return;
        }
        int col = (int) Math.floor(zombie.getPosition().getColumn());
        if (zombie.getAbility() == ZombieAbility.SNORKEL
            && board.isInside(zombie.getPosition().getRow(), col)) {
            TileType type = board.getTile(zombie.getPosition().getRow(), col).getType();
            zombie.setSubmerged(type == TileType.WATER || type == TileType.LOW_TIDE);
        }
    }

    private void performZombieSpecialAbility(Zombie zombie) {
        switch (zombie.getAbility()) {
            case GARGANTUAR -> throwGargantuarImp(zombie);
            case RA -> stealSunWithRa(zombie);
            case TOMB_RAISER -> raiseTombs(zombie);
            case HUNTER -> throwHunterSnowball(zombie);
            case TROGLOBITE -> pushTroglobiteIce(zombie);
            case FISHERMAN -> hookPlantWithFisherman(zombie);
            case OCTOPUS -> throwOctopus(zombie);
            case WIZARD -> transformPlantWithWizard(zombie);
            case KING -> knightNearbyZombie(zombie);
            case TURQUOISE_SKULL -> useTurquoiseSkull(zombie);
            case PROSPECTOR -> launchProspectorDynamite(zombie);
            case PIANIST -> playPiano(zombie);
            default -> { }
        }
    }

    private void throwGargantuarImp(Zombie gargantuar) {
        if (gargantuar.isImpThrown()
            || gargantuar.getHealth() * 2 > gargantuar.getMaximumHealth()) {
            return;
        }
        Zombie imp = zombieFactory.createZombie("ZombieImp");
        imp.applyDifficulty(difficultyLevel);
        int row = gargantuar.getPosition().getRow();
        imp.setPosition(new BoardPosition(row, 2.0));
        board.addZombie(imp);
        gargantuar.markImpThrown();
        addEvent("Gargantuar threw an Imp into column 3.");
    }

    private void stealSunWithRa(Zombie zombie) {
        if (elapsedTicks % TICKS_PER_SECOND != 0 || zombie.getPosition() == null) {
            return;
        }
        for (Sun sun : new ArrayList<>(board.getSuns())) {
            if (sun.isCollected() || sun.getPosition() == null) {
                continue;
            }
            if (sun.getPosition().getRow() == zombie.getPosition().getRow()) {
                int amount = sun.collect();
                zombie.addStolenSun(amount);
                board.removeSun(sun);
                addEvent("Ra Zombie stole " + amount + " sun.");
            }
        }
    }

    private void raiseTombs(Zombie zombie) {
        if (zombie.getAbilityCooldownTicks() > 0 || zombie.getPosition() == null) {
            return;
        }
        int created = 0;
        for (int attempts = 0; attempts < 20 && created < 2; attempts++) {
            int row = random.nextInt(board.getRows());
            int col = 2 + random.nextInt(Math.max(1, board.getCols() - 3));
            GridPosition position = new GridPosition(row, col);
            Tile tile = board.getTile(row, col);
            if (tile.getPlant() == null && tile.getType() == TileType.NORMAL
                && !tombs.containsKey(position)) {
                tombs.put(position, new Tomb(row, col, false, false));
                tile.setTileType(TileType.TOMB);
                created++;
            }
        }
        zombie.setAbilityCooldownTicks(8 * TICKS_PER_SECOND);
        if (created > 0) {
            addEvent("Tomb Raiser created " + created + " tomb(s).");
        }
    }

    private void throwHunterSnowball(Zombie zombie) {
        if (zombie.getAbilityCooldownTicks() > 0 || zombie.getPosition() == null) {
            return;
        }
        Plant target = nearestPlantInRow(zombie.getPosition().getRow(),
            zombie.getPosition().getColumn());
        if (target != null) {
            target.addIceLayer();
            addEvent("Hunter Zombie hit " + target.getName() + " with an ice ball ("
                + target.getIceHits() + "/3).");
        }
        zombie.setAbilityCooldownTicks(6 * TICKS_PER_SECOND);
    }

    private void pushTroglobiteIce(Zombie zombie) {
        if (zombie.getAbilityCooldownTicks() > 0 || zombie.getPosition() == null) {
            return;
        }
        Plant target = nearestPlantInRow(zombie.getPosition().getRow(),
            zombie.getPosition().getColumn());
        if (target != null && target.getPosition().getColumn()
            < zombie.getPosition().getColumn()
            && zombie.getPosition().getColumn() - target.getPosition().getColumn() <= 2.0) {
            target.takeDamage(Math.max(target.getHealth(), 1));
            addEvent("Troglobite pushed an ice block through " + target.getName() + ".");
        }
        zombie.setAbilityCooldownTicks(5 * TICKS_PER_SECOND);
    }

    private void hookPlantWithFisherman(Zombie zombie) {
        if (zombie.getAbilityCooldownTicks() > 0 || zombie.getPosition() == null) {
            return;
        }
        Plant target = nearestPlantInRow(zombie.getPosition().getRow(),
            zombie.getPosition().getColumn());
        if (target != null && target.getPosition() != null) {
            GridPosition old = target.getPosition();
            int newCol = Math.min(board.getCols() - 1, old.getColumn() + 1);
            if (newCol == (int) Math.floor(zombie.getPosition().getColumn())) {
                target.takeDamage(Math.max(target.getHealth(), 1));
                addEvent("Fisherman threw away " + target.getName() + ".");
            } else if (board.getTile(old.getRow(), newCol).getPlant() == null
                && board.getTile(old.getRow(), newCol).getType().isPlantable()) {
                board.removePlant(target);
                board.placePlant(target, old.getRow(), newCol);
                addEvent("Fisherman hooked " + target.getName() + " one tile forward.");
            }
        }
        zombie.setAbilityCooldownTicks(6 * TICKS_PER_SECOND);
    }

    private void throwOctopus(Zombie zombie) {
        if (zombie.getAbilityCooldownTicks() > 0 || zombie.getPosition() == null) {
            return;
        }
        Plant target = nearestPlantInRow(zombie.getPosition().getRow(),
            zombie.getPosition().getColumn());
        if (target != null) {
            target.coverWithOctopus();
            addEvent("Octopus Zombie covered " + target.getName() + ".");
        }
        zombie.setAbilityCooldownTicks(6 * TICKS_PER_SECOND);
    }

    private void transformPlantWithWizard(Zombie zombie) {
        if (zombie.getAbilityCooldownTicks() > 0) {
            return;
        }
        ArrayList<Plant> candidates = new ArrayList<>();
        for (Plant plant : board.getPlants()) {
            if (!plant.isDestroyed() && plant.getPosition() != null
                && plant.getAbility() != PlantAbility.LILY_PAD) {
                candidates.add(plant);
            }
        }
        if (!candidates.isEmpty()) {
            Plant target = candidates.get(random.nextInt(candidates.size()));
            target.transformByWizard(zombie.getRuntimeId());
            addEvent("Wizard transformed " + target.getName() + " into a harmless cat.");
        }
        zombie.setAbilityCooldownTicks(8 * TICKS_PER_SECOND);
    }

    private void knightNearbyZombie(Zombie king) {
        if (king.getAbilityCooldownTicks() > 0 || king.getPosition() == null) {
            return;
        }
        Zombie target = null;
        for (Zombie zombie : hostileZombies()) {
            if (zombie != king && zombie.getAbility() == ZombieAbility.BASIC
                && Math.abs(zombie.getPosition().getRow() - king.getPosition().getRow()) <= 1
                && Math.abs(zombie.getPosition().getColumn()
                    - king.getPosition().getColumn()) <= 3.0) {
                target = zombie;
                break;
            }
        }
        if (target != null) {
            target.addBonusArmor(3200);
            addEvent("King promoted " + target.getName() + " to a Knight.");
        }
        king.setAbilityCooldownTicks(8 * TICKS_PER_SECOND);
    }

    private void useTurquoiseSkull(Zombie zombie) {
        if (zombie.getPosition() == null || zombie.getAbilityCooldownTicks() > 0
            || elapsedTicks % TICKS_PER_SECOND != 0) {
            return;
        }
        Plant target = nearestPlantInRow(zombie.getPosition().getRow(),
            zombie.getPosition().getColumn());
        if (target == null || Math.abs(zombie.getPosition().getColumn()
            - target.getPosition().getColumn()) > 4.0) {
            return;
        }
        int stolen = Math.min(25, sunAmount);
        sunAmount -= stolen;
        zombie.addStolenSun(stolen);
        zombie.specialAbility();
        addEvent("Turquoise Skull stole " + stolen + " sun while charging.");
        if (zombie.getSpecialAbilityUses() % 5 == 0) {
            fireTurquoiseLaser(zombie);
            zombie.setAbilityCooldownTicks(10 * TICKS_PER_SECOND);
        }
    }

    private void fireTurquoiseLaser(Zombie zombie) {
        int row = zombie.getPosition().getRow();
        int start = (int) Math.floor(zombie.getPosition().getColumn()) - 1;
        int destroyed = 0;
        for (int col = Math.max(0, start - 3); col <= Math.min(board.getCols() - 1, start); col++) {
            Plant plant = board.getTile(row, col).getBlockingPlant();
            if (plant != null) {
                plant.takeDamage(Math.max(plant.getHealth(), 1));
                destroyed++;
            }
        }
        addEvent("Turquoise Skull fired its laser and destroyed " + destroyed + " plant(s).");
    }

    private void launchProspectorDynamite(Zombie zombie) {
        if (!zombie.isReversed() && !zombie.isSpecialDisabled()
            && zombie.getAgeTicks() >= 10 * TICKS_PER_SECOND) {
            zombie.reverseDirection();
            addEvent("Prospector's dynamite launched it toward the house from the other side.");
        }
    }

    private void playPiano(Zombie pianist) {
        if (pianist.getAbilityCooldownTicks() > 0) {
            return;
        }
        int moved = 0;
        for (Zombie zombie : hostileZombies()) {
            if (zombie == pianist || zombie.getPosition() == null) {
                continue;
            }
            int row = zombie.getPosition().getRow();
            int direction = random.nextBoolean() ? 1 : -1;
            int targetRow = row + direction;
            if (targetRow >= 0 && targetRow < board.getRows()) {
                zombie.setPosition(zombie.getPosition().withRow(targetRow));
                moved++;
            }
        }
        pianist.setAbilityCooldownTicks(5 * TICKS_PER_SECOND);
        if (moved > 0) {
            addEvent("Pianist changed the lane of " + moved + " zombie(s).");
        }
    }

    private void moveHypnotizedZombie(Zombie zombie) {
        Zombie enemy = nearestHostileZombieForHypnotized(zombie);
        if (enemy != null && Math.abs(enemy.getPosition().getColumn()
            - zombie.getPosition().getColumn()) <= 0.9) {
            enemy.takeDirectDamage(Math.max(1, zombie.getDamage()));
            return;
        }
        zombie.moveOneTick();
        if (zombie.getPosition().getColumn() > board.getCols() + 1) {
            zombie.kill();
        }
    }

    private Zombie nearestHostileZombieForHypnotized(Zombie ally) {
        Zombie target = null;
        double best = Double.MAX_VALUE;
        for (Zombie zombie : board.getZombiesInRow(ally.getPosition().getRow())) {
            if (zombie.isHypnotized() || zombie.isDead()) {
                continue;
            }
            double distance = Math.abs(zombie.getPosition().getColumn()
                - ally.getPosition().getColumn());
            if (distance < best) {
                best = distance;
                target = zombie;
            }
        }
        return target;
    }

    private boolean shouldZombieBypassPlant(Zombie zombie, Plant plant) {
        if (zombie.getAbility() != ZombieAbility.DODO_RIDER) {
            return false;
        }
        return plant.getAbility() != PlantAbility.TALL_NUT;
    }

    private boolean isStationaryZombie(Zombie zombie) {
        return zombie.getAbility() == ZombieAbility.FISHERMAN
            || zombie.getAbility() == ZombieAbility.KING;
    }

    private void resolveZombiePlantCombat(Zombie zombie, Plant plant) {
        if (zombie.getAbility() == ZombieAbility.WIZARD) {
            plant.transformByWizard(zombie.getRuntimeId());
            return;
        }
        if (zombie.getAbility() == ZombieAbility.GARGANTUAR
            || zombie.getAbility() == ZombieAbility.PIANIST
            || (zombie.getAbility() == ZombieAbility.ARCADE && zombie.isMachineActive())
            || (zombie.getAbility() == ZombieAbility.ALL_STAR && !zombie.isChargeUsed())) {
            plant.takeDamage(Math.max(plant.getHealth(), 1));
            zombie.markChargeUsed();
            addEvent(zombie.getName() + " destroyed " + plant.getName() + " on contact.");
            return;
        }
        if (zombie.getAbility() == ZombieAbility.EXPLORER && !zombie.isSpecialDisabled()) {
            plant.takeDamage(Math.max(plant.getHealth(), 1));
            addEvent("Explorer Zombie burned " + plant.getName() + ".");
            return;
        }
        if (elapsedTicks % TICKS_PER_SECOND != 0) {
            return;
        }
        if (plant.getAbility() == PlantAbility.HYPNO_SHROOM) {
            plant.takeDamage(Math.max(plant.getHealth(), 1));
            zombie.hypnotize();
            addEvent("Hypno-shroom converted " + zombie.getName() + ".");
            return;
        }
        zombie.attackPlant(plant);
        if (plant.getAbility() == PlantAbility.ENDURIAN) {
            zombie.takeDirectDamage(Math.max(1, plant.getEffectiveAttackPower()), plant.getName());
        } else if (plant.getAbility() == PlantAbility.SUN_BEAN) {
            sunAmount += 5;
            totalSunCollected += 5;
            addEvent("Sun Bean produced 5 sun after being hit.");
        } else if (plant.getAbility() == PlantAbility.GARLIC && !plant.isDestroyed()) {
            moveZombieToAdjacentLane(zombie);
        }
        addEvent("Zombie " + zombie.getName() + " attacked " + plant.getName()
            + " at " + plant.getPosition() + ".");
    }

    private void moveZombieToAdjacentLane(Zombie zombie) {
        int row = zombie.getPosition().getRow();
        int target = row == 0 ? 1 : row == board.getRows() - 1 ? row - 1
            : row + (random.nextBoolean() ? 1 : -1);
        zombie.setPosition(zombie.getPosition().withRow(target));
        addEvent("Garlic redirected " + zombie.getName() + " to lane " + (target + 1) + ".");
    }

    private void explodeDestroyedDefender(Plant plant) {
        GridPosition center = plant.getPosition();
        int damage = Math.max(1800, plant.getEffectiveAttackPower());
        for (Zombie zombie : hostileZombies()) {
            if (Math.abs(zombie.getPosition().getRow() - center.getRow()) <= 1
                && Math.abs(zombie.getPosition().getColumn() - center.getColumn()) <= 1.5) {
                zombie.takeDamage(damage, plant.getName());
            }
        }
        addEvent("Explode-o-nut detonated when destroyed.");
    }

    private void removeUnsupportedWaterPlants() {
        for (int row = 0; row < board.getRows(); row++) {
            for (int col = 0; col < board.getCols(); col++) {
                Tile tile = board.getTile(row, col);
                boolean water = tile.getType() == TileType.WATER
                    || tile.getType() == TileType.LOW_TIDE;
                Plant main = tile.getMainPlant();
                if (water && main != null && !main.getDefinition().hasTag("Water")
                    && tile.getSupportPlant() == null) {
                    main.takeDamage(Math.max(main.getHealth(), 1));
                }
            }
        }
    }

    private void releaseWizardTransformations(String wizardId) {
        for (Plant plant : board.getPlants()) {
            plant.releaseWizardTransformation(wizardId);
        }
    }

    private void dropStolenSunFromZombie(Zombie zombie) {
        int stolen = zombie.takeStolenSun();
        if (stolen <= 0) {
            return;
        }
        int returned = zombie.getAbility() == ZombieAbility.TURQUOISE_SKULL
            ? stolen / 2 : stolen;
        sunAmount += returned;
        addEvent(zombie.getName() + " dropped " + returned + " stolen sun.");
    }

    private void removeInstantPlant(Plant plant) {
        plant.takeDamage(Math.max(plant.getHealth(), 1));
    }

    private void activateMint(Plant mint) {
        int affected = 0;
        for (Plant plant : new ArrayList<>(board.getPlants())) {
            if (plant != mint && plantMatchesMint(plant, mint.getAbility())) {
                activatePlantFood(plant, mint.getName());
                affected++;
            }
        }
        removeInstantPlant(mint);
        addEvent(mint.getName() + " empowered " + affected + " related plant(s).");
    }

    private boolean plantMatchesMint(Plant plant, PlantAbility mint) {
        return switch (mint) {
            case ENLIGHTEN_MINT -> plant.isSunProducer();
            case APPEASE_MINT -> plant.getDefinition().getCategory().equalsIgnoreCase("Shooter");
            case ARMA_MINT -> plant.isLobber();
            case BOMBARD_MINT -> plant.isExplosive();
            case ENFORCE_MINT -> plant.isMelee();
            case REINFORCE_MINT -> plant.getDefinition().getCategory().equalsIgnoreCase("Wall-nut");
            case ENCHANT_MINT -> plant.getDefinition().getCategory().equalsIgnoreCase("Modifier");
            case PIERCE_MINT -> plant.getDefinition().getCategory().equalsIgnoreCase("Strike-through");
            case CATTAIL_MINT -> plant.isHoming();
            default -> false;
        };
    }

    private void freezeAllZombies(Plant source, boolean killWeak) {
        int affected = 0;
        for (Zombie zombie : hostileZombies()) {
            if (killWeak && zombie.getEffectiveHealth() <= source.getEffectiveAttackPower() * 5) {
                zombie.kill(source.getName());
            } else {
                zombie.stun(5 * TICKS_PER_SECOND);
                zombie.chill(10 * TICKS_PER_SECOND);
            }
            affected++;
        }
        addEvent(source.getName() + " froze " + affected + " zombie(s).");
    }

    private boolean explosionHits(PlantAbility ability, int rowDistance,
                                  double columnDistance) {
        return switch (ability) {
            case JALAPENO -> rowDistance == 0;
            case DOOM_SHROOM -> true;
            case POTATO_MINE, SQUASH, TANGLE_KELP -> rowDistance == 0
                && columnDistance <= 0.9;
            default -> rowDistance <= 1 && columnDistance <= 1.5;
        };
    }

    private void launchGrapeshotFragments(Plant plant, int multiplier) {
        ArrayList<Zombie> targets = hostileZombies();
        int fragments = Math.min(8, targets.size());
        for (int index = 0; index < fragments; index++) {
            Zombie target = targets.get(random.nextInt(targets.size()));
            target.takeDamage(100 * Math.max(1, multiplier), plant.getName());
        }
        addEvent("Grapeshot launched " + fragments + " bouncing fragment(s).");
    }

    private int calculateNextSkySunTick() {
        return calculateSkySunIntervalTicks();
    }

    private int calculateSkySunIntervalTicks() {
        double seconds = elapsedTicks / (double) TICKS_PER_SECOND;
        double baseInterval = Math.min(12.0, 6.0 + 0.05 * seconds);
        double intervalSeconds = baseInterval * difficultyLevel / 3.0;
        return Math.max(1, (int) Math.round(intervalSeconds * TICKS_PER_SECOND));
    }

    private void addForcedPlantSelections() {
        if (currentLevel == null) {
            return;
        }
        for (String plantName : currentLevel.getForcedPlants()) {
            PlantDefinition definition = plantFactory.findDefinition(plantName).orElse(null);
            if (definition != null && selectedPlants.size() < currentLevel.getAllowedPlantCount()) {
                selectedPlants.add(definition.getName());
                cooldownTicks.put(definition.getNormalizedName(), 0);
            }
        }
    }

    private void validatePlantSelectionRule(PlantDefinition definition) {
        if (currentLevel == null) {
            return;
        }
        if (containsNormalized(currentLevel.getLockedPlants(), definition.getName())) {
            throw new IllegalStateException("This plant is locked in the current level.");
        }
        for (String family : currentLevel.getBannedPlantFamilies()) {
            if (matchesPlantFamily(definition, family)) {
                throw new IllegalStateException("The " + family
                    + " plant family is locked in this level.");
            }
        }
        for (Map.Entry<String, String> entry
            : currentLevel.getFamilyRepresentativePlants().entrySet()) {
            if (matchesPlantFamily(definition, entry.getKey())
                && !definition.getName().equalsIgnoreCase(entry.getValue())) {
                throw new IllegalStateException("Only " + entry.getValue()
                    + " is available from the " + entry.getKey() + " family.");
            }
        }
        if (currentLevel.getSpecialType() == SpecialLevelType.PLANT_WHAT_YOU_GET
            && isSunProducerDefinition(definition)) {
            throw new IllegalStateException("Sun-producing plants are unavailable in this level.");
        }
    }

    private boolean matchesPlantFamily(PlantDefinition definition, String family) {
        if (definition.getCategory().equalsIgnoreCase(family) || definition.hasTag(family)) {
            return true;
        }
        String normalizedFamily = PlantDefinition.normalizeKey(family);
        return normalizedFamily.equals("mint")
            && (PlantAbility.fromDefinition(definition).isMint()
            || definition.getNormalizedName().endsWith("mint"));
    }

    private boolean containsNormalized(List<String> values, String expected) {
        String normalized = PlantDefinition.normalizeKey(expected);
        for (String value : values) {
            if (PlantDefinition.normalizeKey(value).equals(normalized)) {
                return true;
            }
        }
        return false;
    }

    private boolean isSunProducerDefinition(PlantDefinition definition) {
        return definition.getCategory().equalsIgnoreCase("Sun Producer")
            || definition.getCategory().equalsIgnoreCase("SunProducer")
            || definition.hasTag("Sun");
    }

    private boolean isPreWaveSetup() {
        return currentLevel != null
            && currentLevel.getSpecialType() == SpecialLevelType.PLANT_WHAT_YOU_GET
            && !zombieWavesStarted;
    }

    private void recordPlantUsage(Plant plant) {
        if (plant == null) {
            return;
        }
        plantedPlantNames.add(plant.getName());
        plantedPlantFamilies.add(plant.getDefinition().getCategory());
        if (plant.isSunProducer()) {
            sunProducerPlantsPlanted++;
        }
    }

    private void autoSelectStarterPlants() {
        for (String starter : List.of("Sunflower", "Peashooter", "Wall-nut")) {
            if (plantFactory.findDefinition(starter).isPresent()) {
                selectPlant(starter);
            }
        }
    }

    private void requirePlantSelection() {
        if (currentLevel == null || gameState != GameState.PLANT_SELECTION) {
            throw new IllegalStateException("A level must be prepared for plant selection.");
        }
    }

    private void requireRunning() {
        if (gameState != GameState.RUNNING || board == null) {
            throw new IllegalStateException("No game is currently running.");
        }
    }

    private void addEvent(String event) {
        events.add(event);
    }

    private String display(int row, int col) {
        return "(" + (col + 1) + ", " + (row + 1) + ")";
    }

    private String formatSeconds(int ticks) {
        return String.format("%.1f", ticks / (double) TICKS_PER_SECOND);
    }

    private String formatColumn(double column) {
        return String.format("%.2f", column + 1);
    }
}
