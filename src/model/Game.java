package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class Game {
    public static final int TICKS_PER_SECOND = 10;
    private static final double PROJECTILE_SPEED = 5.0;

    private final PlantFactory plantFactory;
    private final ZombieFactory zombieFactory;
    private final Random random;
    private final LinkedHashSet<String> selectedPlants;
    private final LinkedHashMap<String, Integer> cooldownTicks;
    private final LinkedHashSet<GridPosition> waitingSunProducers;
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

    public Game(PlantFactory plantFactory, ZombieFactory zombieFactory) {
        if (plantFactory == null || zombieFactory == null) {
            throw new IllegalArgumentException("Game factories cannot be null.");
        }
        this.plantFactory = plantFactory;
        this.zombieFactory = zombieFactory;
        this.random = new Random();
        this.selectedPlants = new LinkedHashSet<>();
        this.cooldownTicks = new LinkedHashMap<>();
        this.waitingSunProducers = new LinkedHashSet<>();
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
        cooldownTicks.clear();
        waitingSunProducers.clear();
        events.clear();
        gameState = GameState.PLANT_SELECTION;
        addEvent("Level prepared: " + currentLevel.getLevelId() + " ("
            + currentLevel.getSpecialType() + ").");
    }

    public void selectPlant(String plantType) {
        requirePlantSelection();
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

    public void startGame(Level level) {
        prepareLevel(null, level);
        if (selectedPlants.isEmpty()) {
            autoSelectStarterPlants();
        }
        startGame();
    }

    public void startGame() {
        requirePlantSelection();
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
        wave.populate(zombieFactory, board.getRows(), board.getCols() - 0.05, random);
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
        int remainingCooldown = cooldownTicks.getOrDefault(key, 0);
        if (remainingCooldown > 0) {
            throw new IllegalStateException("Plant is on cooldown for "
                + formatSeconds(remainingCooldown) + " seconds.");
        }
        if (sunAmount < definition.getCost()) {
            throw new IllegalStateException("Not enough sun.");
        }
        Plant plant = plantFactory.createPlant(definition.getName());
        board.placePlant(plant, row, col);
        sunAmount -= definition.getCost();
        int recharge = (int) Math.round(definition.getRechargeSeconds().orElse(0)
            * TICKS_PER_SECOND);
        cooldownTicks.put(key, Math.max(0, recharge));
        addEvent("Plant " + plant.getName() + " planted at " + display(row, col) + ".");
        handleImmediatePlant(plant);
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
            output.append(name).append(": cost=").append(definition.getCost())
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
            output.append("plant: ").append(plant.getName()).append(", health=")
                .append(plant.getHealth()).append('/').append(plant.getMaxHealth())
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
            + ", wave=" + waveNumber + "/" + currentLevel.getWaves().size()
            + ", sun=" + sunAmount + ", ticks=" + elapsedTicks;
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

    private void advanceOneTick() {
        elapsedTicks++;
        tickCooldowns();
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
            ? sunflower.getProductionAmount() : inferSunProduction(plant);
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
                plant.getProjectileElementType(), plant.isPiercing());
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
            lostPlantsCount++;
            addEvent("Plant " + plant.getName() + " at " + position + " is destroyed.");
        }
        for (Zombie zombie : new ArrayList<>(board.getZombies())) {
            if (!zombie.isDead()) {
                continue;
            }
            BoardPosition position = zombie.getPosition();
            board.removeZombie(zombie);
            addEvent("Zombie of type " + zombie.getName() + " is dead at " + position + ".");
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
            detonatePlant(plant);
            cleanupDestroyedEntities();
        }
    }

    private void detonatePlant(Plant plant) {
        GridPosition center = plant.getPosition();
        int damage = plant.getDefinition().isInstantKill()
            ? Integer.MAX_VALUE / 4 : Math.max(1, plant.getAttackPower());
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
        double intervalSeconds = Math.min(12.0, 6.0 + 0.05 * seconds);
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
