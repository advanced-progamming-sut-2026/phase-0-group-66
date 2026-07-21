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
    private int nextConveyorTick;

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
        tombs.clear();
        totalSunCollected = 0;
        zombieKillCount = 0;
        explosivePlantsUsed = 0;
        lawnMowerKills = 0;
        nextConveyorTick = 0;
        events.clear();
        gameState = GameState.PLANT_SELECTION;
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
        addEvent("Game started with " + sunAmount + " suns.");
        startNextWave();
    }

    public void advanceTime(int ticks) {
        requireRunning();
        if (ticks <= 0) {
            throw new IllegalArgumentException("Tick count must be positive.");
        }
        for (int index = 0; index < ticks && gameState == GameState.RUNNING; index++) {
            advanceOneTick();
        }
    }

    public void startNextWave() {
        requireRunning();
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
        int remainingCooldown = cooldownTicks.getOrDefault(key, 0);
        if (!conveyor && remainingCooldown > 0) {
            throw new IllegalStateException("Plant is on cooldown for "
                + formatSeconds(remainingCooldown) + " seconds.");
        }
        if (conveyor) {
            int cards = conveyorCards.getOrDefault(definition.getName(), 0);
            if (cards <= 0) {
                throw new IllegalStateException("No conveyor card is available for this plant.");
            }
            conveyorCards.put(definition.getName(), cards - 1);
        }
        int plantLevel = plantLevels.getOrDefault(key, 1);
        Plant plant = plantFactory.createPlant(definition.getName(), plantLevel);
        if (!conveyor && sunAmount < plant.getSunCost()) {
            throw new IllegalStateException("Not enough sun.");
        }
        board.placePlant(plant, row, col);
        if (!conveyor) {
            sunAmount -= plant.getSunCost();
            cooldownTicks.put(key, plant.getRechargeTicks());
        }
        addEvent("Plant " + plant.getName() + " (level " + plant.getPlantLevel()
            + ") planted at " + display(row, col) + ".");
        boolean boosted = applyAutomaticBoostIfPresent(plant);
        if (!boosted || !plant.isExplosive()) {
            handleImmediatePlant(plant);
        }
        if (plant.isDestroyed()) {
            cleanupDestroyedEntities();
        }
    }

    public void pluckPlant(int row, int col) {
        requireRunning();
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
        int created = 0;
        while (created < count) {
            int row = random.nextInt(board.getRows());
            int col = 3 + random.nextInt(4);
            GridPosition position = new GridPosition(row, col);
            if (tombs.containsKey(position) || board.getTile(row, col).getPlant() != null) {
                continue;
            }
            boolean sun = mayContainRewards && random.nextInt(5) == 0;
            boolean plantFood = mayContainRewards && !sun && random.nextInt(10) == 0;
            tombs.put(position, new Tomb(row, col, sun, plantFood));
            board.getTile(row, col).setTileType(TileType.TOMB);
            created++;
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
                    Plant plant = tile.getPlant();
                    if (plant != null && !plant.getDefinition().hasTag("Water")
                        && !plant.getDefinition().getNormalizedName().contains("lilypad")) {
                        plant.takeDamage(plant.getHealth());
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
        if (plant.isSunProducer()) {
            int amount = plantFoodSunAmount(plant);
            sunAmount += amount;
            addEvent("Plant food made " + plant.getName() + " produce " + amount
                + " suns immediately.");
        } else if (plant.isExplosive()) {
            explosivePlantsUsed++;
            detonatePlant(plant, 2);
        } else if (plant.isShooter()) {
            plantFoodShooterVolley(plant);
        } else if (plant.isHoming()) {
            plantFoodHomingStrike(plant);
        } else if (plant.isMelee()) {
            plantFoodMeleeStrike(plant);
        } else {
            addEvent("Plant food fully healed " + plant.getName()
                + " and granted a " + plant.getPlantFoodShield() + " point shield.");
        }
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
                zombie.takeDamage(Math.max(1, plant.getAttackPower()) * 5);
                hits++;
            }
        }
        addEvent(plant.getName() + " used a plant-food area strike on " + hits
            + " zombie(s).");
    }

    private void applyPlantFoodDamage(Zombie zombie, Plant plant, int damage) {
        ProjectileType type = plant.getProjectileElementType();
        if (type == ProjectileType.POISON) {
            zombie.takeDirectDamage(damage);
        } else if (type == ProjectileType.FIRE) {
            zombie.clearChill();
            zombie.takeDamage(damage * 2);
        } else {
            zombie.takeDamage(damage);
            if (type == ProjectileType.ICE) {
                zombie.chill(plant.getChillDurationTicks());
            }
        }
    }

    private void initializeSpecialLevel() {
        if (currentLevel.getSpecialType() == SpecialLevelType.CONVEYOR_BELT) {
            if (selectedPlants.isEmpty()) {
                autoSelectStarterPlantsForConveyor();
            }
            addConveyorCard();
            nextConveyorTick = 120;
        }
        if (currentLevel.getSpecialType() == SpecialLevelType.SAVE_OUR_SEEDS) {
            for (int row : List.of(0, 2, 4)) {
                Plant protectedPlant = plantFactory.createPlant("Wall-nut",
                    plantLevels.getOrDefault("wallnut", 1));
                board.placePlant(protectedPlant, row, 2);
                endangeredPositions.add(new GridPosition(row, 2));
            }
            addEvent("Protected seed plants were placed in column 3.");
        }
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
        for (String starter : List.of("Sunflower", "Peashooter", "Wall-nut")) {
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
        return currentLevel.getSeason() != SeasonType.DARK_AGES
            && currentLevel.getSpecialType() != SpecialLevelType.NIGHT_OPS;
    }

    private void performPlantActions() {
        for (Plant plant : new ArrayList<>(board.getPlants())) {
            if (plant.isDestroyed() || plant.getPosition() == null) {
                continue;
            }
            if (plant.isTrap()) {
                Zombie target = board.findNearestZombieAhead(plant.getPosition().getRow(),
                    plant.getPosition().getColumn() - 0.5);
                if (target != null && target.getPosition().getColumn()
                    <= plant.getPosition().getColumn() + 0.8) {
                    detonatePlant(plant);
                }
                continue;
            }
            if (!plant.tickActionTimer()) {
                continue;
            }
            if (plant.isSunProducer()) {
                produceSun(plant);
            } else if (plant.isShooter()) {
                shootProjectiles(plant);
                plant.resetActionTimer();
            } else if (plant.isHoming()) {
                attackHoming(plant);
                plant.resetActionTimer();
            } else if (plant.isMelee()) {
                attackMelee(plant);
                plant.resetActionTimer();
            } else {
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
        int amount = plant instanceof Sunflower sunflower
            ? sunflower.getProductionAmount() : inferSunProduction(plant)
                + plant.getSunProductionBonus();
        if (plant.hasDoubleSunChance() && random.nextBoolean()) {
            amount *= 2;
        }
        Sun sun = new Sun(amount, position);
        board.addSun(sun);
        waitingSunProducers.add(position);
        addEvent("Plant " + plant.getName() + " produced a sun at " + position + ".");
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
        int projectileCount = plant.getProjectileCount();
        for (int index = 0; index < projectileCount; index++) {
            double startColumn = position.getColumn() + 0.25 - index * 0.03;
            Projectile projectile = new Projectile(plant.getAttackPower(), PROJECTILE_SPEED,
                new BoardPosition(position.getRow(), startColumn),
                plant.getProjectileElementType(), plant.isPiercing(),
                plant.getChillDurationTicks());
            board.addProjectile(projectile);
        }
        addEvent("Plant " + plant.getName() + " fired " + projectileCount
            + " projectile(s) from " + position + ".");
    }

    private void attackHoming(Plant plant) {
        Zombie target = board.findNearestZombieAnywhere();
        if (target != null) {
            target.takeDamage(Math.max(1, plant.getAttackPower()));
            addEvent("Plant " + plant.getName() + " hit " + target.getName() + ".");
        }
    }

    private void attackMelee(Plant plant) {
        GridPosition position = plant.getPosition();
        Zombie target = board.findNearestZombieAhead(position.getRow(), position.getColumn() - 0.5);
        if (target != null && target.getPosition().getColumn() <= position.getColumn() + 1.25) {
            target.takeDamage(Math.max(1, plant.getAttackPower()));
            addEvent("Plant " + plant.getName() + " struck " + target.getName() + ".");
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
            if (hitTomb(projectile, previousColumn, currentColumn)) {
                board.removeProjectile(projectile);
                continue;
            }
            Zombie target = findProjectileTarget(projectile.getPosition().getRow(),
                previousColumn, currentColumn);
            if (target != null) {
                projectile.hitTarget(target);
                addEvent("Projectile hit " + target.getName() + " for "
                    + projectile.getDamage() + " base damage.");
            }
            if (!projectile.isActive() || currentColumn > board.getCols() + 1) {
                board.removeProjectile(projectile);
            }
        }
    }

    private Zombie findProjectileTarget(int row, double fromColumn, double toColumn) {
        Zombie target = null;
        for (Zombie zombie : board.getZombiesInRow(row)) {
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
            Plant blockingPlant = board.findBlockingPlant(zombie);
            if (blockingPlant != null) {
                if (elapsedTicks % TICKS_PER_SECOND == 0) {
                    zombie.attackPlant(blockingPlant);
                    addEvent("Zombie " + zombie.getName() + " attacked "
                        + blockingPlant.getName() + " at " + blockingPlant.getPosition() + ".");
                }
            } else {
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
            board.removePlant(position.getRow(), position.getColumn());
            waitingSunProducers.remove(position);
            if (endangeredPositions.contains(position)) {
                board.setEndangeredPlantsEaten(true);
            }
            lostPlantsCount++;
            addEvent("Plant " + plant.getName() + " at " + position + " is destroyed.");
        }
        for (Zombie zombie : new ArrayList<>(board.getZombies())) {
            if (!zombie.isDead()) {
                continue;
            }
            BoardPosition position = zombie.getPosition();
            handleZombieRewards(zombie);
            board.removeZombie(zombie);
            zombieKillCount++;
            addEvent("Zombie of type " + zombie.getName() + " is dead at " + position + ".");
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
        if (nextWaveIndex >= currentLevel.getWaves().size() || currentWave == null) {
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
            addEvent("The special level lose condition was reached.");
            return;
        }
        if (checkWinCondition()) {
            gameState = GameState.WON;
            currentLevel.completeLevel();
            addEvent("Dear humanz, zis is not done yet; we will come back to eat your brainz, humanz.");
        }
    }

    private boolean specialLoseConditionReached() {
        if (currentLevel.getSpecialType() == SpecialLevelType.DEAD_LINE) {
            return board.hasZombiesCrossedColumn(2);
        }
        if (currentLevel.getSpecialType() == SpecialLevelType.SAVE_OUR_SEEDS) {
            return board.areEndangeredPlantsEaten();
        }
        return false;
    }

    private void handleImmediatePlant(Plant plant) {
        String normalized = plant.getDefinition().getNormalizedName();
        if (normalized.equals("goldbloom")) {
            sunAmount += 375;
            GridPosition position = plant.getPosition();
            board.removePlant(position.getRow(), position.getColumn());
            addEvent("Gold Bloom produced 375 suns and disappeared.");
        } else if (plant.isExplosive() && !plant.isTrap()) {
            explosivePlantsUsed++;
            detonatePlant(plant);
            cleanupDestroyedEntities();
        }
    }

    private void detonatePlant(Plant plant) {
        detonatePlant(plant, 1);
    }

    private void detonatePlant(Plant plant, int damageMultiplier) {
        GridPosition center = plant.getPosition();
        int damage = plant.getDefinition().isInstantKill()
            ? Integer.MAX_VALUE / 4 : Math.max(1, plant.getAttackPower())
                * Math.max(1, damageMultiplier);
        boolean wholeRow = plant.getDefinition().getNormalizedName().contains("jalapeno");
        for (Zombie zombie : new ArrayList<>(board.getZombies())) {
            if (zombie.getPosition() == null) {
                continue;
            }
            int rowDistance = Math.abs(zombie.getPosition().getRow() - center.getRow());
            double columnDistance = Math.abs(zombie.getPosition().getColumn() - center.getColumn());
            if ((wholeRow && rowDistance == 0) || (!wholeRow && rowDistance <= 1
                && columnDistance <= 1.5)) {
                zombie.takeDamage(damage);
            }
        }
        plant.takeDamage(plant.getHealth());
        addEvent("Plant " + plant.getName() + " exploded at " + center + ".");
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

    private int calculateNextSkySunTick() {
        return calculateSkySunIntervalTicks();
    }

    private int calculateSkySunIntervalTicks() {
        double seconds = elapsedTicks / (double) TICKS_PER_SECOND;
        double baseInterval = Math.min(12.0, 6.0 + 0.05 * seconds);
        double intervalSeconds = baseInterval * difficultyLevel / 3.0;
        return Math.max(1, (int) Math.round(intervalSeconds * TICKS_PER_SECOND));
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
