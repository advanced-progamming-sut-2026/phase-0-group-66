package model;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class Game {
    public static final int TICKS_PER_SECOND = 10;
    public static final int INITIAL_PREPARATION_TICKS = 15 * TICKS_PER_SECOND;
    static final  double PROJECTILE_SPEED = 5.0;

    final  PlantFactory plantFactory;
    final  ZombieFactory zombieFactory;
    final  Random random;
    final  int difficultyLevel;
    final  Map<String, Integer> plantLevels;
    final  Inventory inventory;
    final  Wallet wallet;
    final  LinkedHashSet<String> selectedPlants;
    final  LinkedHashSet<String> levelBoostedPlants;
    final  LinkedHashMap<String, Integer> cooldownTicks;
    final  LinkedHashMap<Zombie, Integer> pendingZombieSpawns;
    final  LinkedHashSet<GridPosition> waitingSunProducers;
    final  LinkedHashSet<GridPosition> endangeredPositions;
    final  LinkedHashMap<String, Integer> conveyorCards;
    final  LinkedHashMap<String, Integer> plantKillCounts;
    final  LinkedHashSet<String> plantedPlantNames;
    final  LinkedHashSet<String> plantedPlantFamilies;
    final  LinkedHashSet<GridPosition> plantedPositions;
    final  LinkedHashSet<String> encounteredZombieNames;
    final  LinkedHashMap<GridPosition, Tomb> tombs;
    final  LinkedHashSet<GridPosition> warmedIcePositions;
    final Set<Zombie> tornadoEntryZombies;
    final LinkedHashSet<Integer> coldWindRows = new LinkedHashSet<>();
    int coldWindUntilTick;
    final  ArrayList<String> events;
    final ArrayDeque<Integer> timedWarKillSamples = new ArrayDeque<>();
    final ArrayDeque<Integer> timedWarSunSamples = new ArrayDeque<>();
    int timedWarLastKillCount;
    int timedWarLastSunCollected;

    GameState gameState;
    Chapter currentChapter;
    Level currentLevel;
    Board board;
    Wave currentWave;
    int sunAmount;
    int elapsedTicks;
    int nextWaveIndex;
    int nextSkySunTick;
    int lostPlantsCount;
    int totalSunCollected;
    int zombieKillCount;
    int explosivePlantsUsed;
    int lawnMowerKills;
    int killsWithinThirtySeconds;
    int firstColumnNoMowerKills;
    int piercingProjectileHits;
    int multiKillZombieCount;
    int lastKillTick;
    int killsAtLastKillTick;
    int sunProducerPlantsPlanted;
    int nextConveyorTick;
    boolean zombieWavesStarted;
    boolean externalWinControlled;

    public Game(PlantFactory plantFactory, ZombieFactory zombieFactory) {
        this(plantFactory, zombieFactory, 3, Map.of(), new Inventory(), new Wallet(),
            new Random());
    }
    public Game(PlantFactory plantFactory, ZombieFactory zombieFactory, int difficultyLevel,
                Map<String, Integer> plantLevels, Inventory inventory, Wallet wallet) {
        this(plantFactory, zombieFactory, difficultyLevel, plantLevels, inventory, wallet,
            new Random());
    }
    public Game(PlantFactory plantFactory, ZombieFactory zombieFactory, int difficultyLevel,
                Map<String, Integer> plantLevels, Inventory inventory, Wallet wallet,
                long randomSeed) {
        this(plantFactory, zombieFactory, difficultyLevel, plantLevels, inventory, wallet,
            new Random(randomSeed));
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
        this.plantLevels = normalizeInitialPlantLevels(plantLevels);
        this.selectedPlants = new LinkedHashSet<>();
        this.levelBoostedPlants = new LinkedHashSet<>();
        this.cooldownTicks = new LinkedHashMap<>();
        this.pendingZombieSpawns = new LinkedHashMap<>();
        this.waitingSunProducers = new LinkedHashSet<>();
        this.endangeredPositions = new LinkedHashSet<>();
        this.conveyorCards = new LinkedHashMap<>();
        this.plantKillCounts = new LinkedHashMap<>();
        this.plantedPlantNames = new LinkedHashSet<>();
        this.plantedPlantFamilies = new LinkedHashSet<>();
        this.plantedPositions = new LinkedHashSet<>();
        this.encounteredZombieNames = new LinkedHashSet<>();
        this.tombs = new LinkedHashMap<>();
        this.warmedIcePositions = new LinkedHashSet<>();
        this.tornadoEntryZombies = Collections.newSetFromMap(new IdentityHashMap<>());
        this.events = new ArrayList<>();
        this.gameState = GameState.PLANT_SELECTION;
    }
    private static Map<String, Integer> normalizeInitialPlantLevels(
            Map<String, Integer> levels) {
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
    public void prepareLevel(Chapter chapter, Level level) { BattleCommandSystem.prepareLevel(this, chapter, level); }
    public void selectPlant(String plantType) { BattleCommandSystem.selectPlant(this, plantType); }
    public void removeSelectedPlant(String plantType) { BattleCommandSystem.removeSelectedPlant(this, plantType); }
    public void boostSelectedPlant(String plantType) { BattleCommandSystem.boostSelectedPlant(this, plantType); }
    public boolean isLevelBoosted(String plantType) { return BattleCommandSystem.isLevelBoosted(this, plantType); }
    public void feedPlant(int row, int col) { BattleCommandSystem.feedPlant(this, row, col); }
    public void addPlantFoodCheat() { BattleCommandSystem.addPlantFoodCheat(this); }
    public int getPlantFoodCount() { return BattleCommandSystem.getPlantFoodCount(this); }
    public void startGame(Level level) { BattleCommandSystem.startGame(this, level); }
    public void startGame() { BattleCommandSystem.startGame(this); }
    public void startZombieWaves() { BattleCommandSystem.startZombieWaves(this); }
    public void advanceTime(int ticks) { BattleCommandSystem.advanceTime(this, ticks); }
    public void startNextWave() { BattleCommandSystem.startNextWave(this); }
    public void plant(Plant plant, int row, int col) { BattleCommandSystem.plant(this, plant, row, col); }
    public void plant(String plantType, int row, int col) { BattleCommandSystem.plant(this, plantType, row, col); }
    public void pluckPlant(int row, int col) { BattleCommandSystem.pluckPlant(this, row, col); }
    public void collectSun(int row, int col) { BattleCommandSystem.collectSun(this, row, col); }
    public void addSun(int amount) { BattleQuerySystem.addSun(this, amount); }
    public void removeAllCooldowns() { BattleQuerySystem.removeAllCooldowns(this); }
    public void releaseNuke() { BattleQuerySystem.releaseNuke(this); }
    public void spawnZombie(String zombieType, int row, double column) {
        BattleQuerySystem.spawnZombie(this, zombieType, row, column);
    }
    public boolean checkWinCondition() { return BattleQuerySystem.checkWinCondition(this); }
    public boolean checkLoseCondition() { return BattleQuerySystem.checkLoseCondition(this); }
    public List<String> drainEvents() { return BattleQuerySystem.drainEvents(this); }
    public List<String> getSelectedPlants() { return BattleQuerySystem.getSelectedPlants(this); }
    public List<String> getEncounteredZombieNames() {
        return List.copyOf(encounteredZombieNames);
    }
    public boolean isPlantAvailableForSelection(String plantType) {
        return BattleQuerySystem.isPlantAvailableForSelection(this, plantType);
    }
    public Map<String, Integer> getCooldownTicks() { return BattleQuerySystem.getCooldownTicks(this); }
    public int getCooldownTicks(String plantType) { return BattleQuerySystem.getCooldownTicks(this, plantType); }
    public String plantStatus() { return BattleQuerySystem.plantStatus(this); }
    public String tileStatus(int row, int col) { return BattleQuerySystem.tileStatus(this, row, col); }
    public String zombieInfo() { return BattleQuerySystem.zombieInfo(this); }
    public String summary() { return BattleQuerySystem.summary(this); }
    public GameState getGameState() { return BattleQuerySystem.getGameState(this); }
    public Chapter getCurrentChapter() { return BattleQuerySystem.getCurrentChapter(this); }
    public Level getCurrentLevel() { return BattleQuerySystem.getCurrentLevel(this); }
    public Board getBoard() { return BattleQuerySystem.getBoard(this); }
    public Tomb getTombAt(int row, int column) {
        return tombs.get(new GridPosition(row, column));
    }
    public Wave getCurrentWave() { return BattleQuerySystem.getCurrentWave(this); }
    public int getSunAmount() { return BattleQuerySystem.getSunAmount(this); }
    public int getElapsedTicks() { return BattleQuerySystem.getElapsedTicks(this); }
    public int getLostPlantsCount() { return BattleQuerySystem.getLostPlantsCount(this); }
    public int getTotalSunCollected() { return BattleQuerySystem.getTotalSunCollected(this); }
    public int getZombieKillCount() { return BattleQuerySystem.getZombieKillCount(this); }
    public int getExplosivePlantsUsed() { return BattleQuerySystem.getExplosivePlantsUsed(this); }
    public int getLawnMowerKills() { return BattleQuerySystem.getLawnMowerKills(this); }
    public Map<String, Integer> getConveyorCards() { return BattleQuerySystem.getConveyorCards(this); }
    public Map<String, Integer> getPlantKillCounts() { return LevelSetupSystem.getPlantKillCounts(this); }
    public int getPlantKills(String plantName) { return LevelSetupSystem.getPlantKills(this, plantName); }
    public int getKillsWithinThirtySeconds() { return LevelSetupSystem.getKillsWithinThirtySeconds(this); }
    public int getFirstColumnNoMowerKills() { return LevelSetupSystem.getFirstColumnNoMowerKills(this); }
    public int getPiercingProjectileHits() { return LevelSetupSystem.getPiercingProjectileHits(this); }
    public int getMultiKillZombieCount() { return LevelSetupSystem.getMultiKillZombieCount(this); }
    public int getSunProducerPlantsPlanted() { return LevelSetupSystem.getSunProducerPlantsPlanted(this); }
    public List<String> getPlantedPlantNames() { return LevelSetupSystem.getPlantedPlantNames(this); }
    public List<String> getPlantedPlantFamilies() { return LevelSetupSystem.getPlantedPlantFamilies(this); }
    public boolean wasRowEverPlanted(int row) {
        return LevelSetupSystem.wasRowEverPlanted(this, row);
    }
    public boolean wasColumnEverPlanted(int column) {
        return LevelSetupSystem.wasColumnEverPlanted(this, column);
    }
    public boolean wasCrossEverPlanted(int row, int column) {
        return LevelSetupSystem.wasCrossEverPlanted(this, row, column);
    }
    public double getDifficultySpeedMultiplier() {
        return DifficultyScaling.intensityFactor(difficultyLevel);
    }
    public boolean areZombieWavesStarted() { return LevelSetupSystem.areZombieWavesStarted(this); }
    void setExternalWinControlled(boolean controlled) { externalWinControlled = controlled; }
    Map<String, Integer> normalizePlantLevels(Map<String, Integer> levels) {
        return LevelSetupSystem.normalizePlantLevels(this, levels);
    }
    int adjustedWaveCost(int baseCost) { return LevelSetupSystem.adjustedWaveCost(this, baseCost); }
    void configureZombieDifficultyAndDrops(Wave wave) {
        LevelSetupSystem.configureZombieDifficultyAndDrops(this, wave);
    }
    void initializeSeasonTerrain() { LevelSetupSystem.initializeSeasonTerrain(this); }
    int addRandomTombs(int count, boolean mayContainRewards) {
        return LevelSetupSystem.addRandomTombs(this, count, mayContainRewards);
    }
    void applyWaveStartSeasonEffects(Wave wave) {
        LevelSetupSystem.applyWaveStartSeasonEffects(this, wave);
    }
    void configureWaveForSeason(Wave wave) { LevelSetupSystem.configureWaveForSeason(this, wave); }
    void setBeachWaterLevel(int startColumn) { LevelSetupSystem.setBeachWaterLevel(this, startColumn); }
    void spawnNecromancyZombie(Wave wave) { LevelSetupSystem.spawnNecromancyZombie(this, wave); }
    void applySlipperyTile(Zombie zombie) { LevelSetupSystem.applySlipperyTile(this, zombie); }
    boolean hitIceTile(Projectile projectile, double fromColumn, double toColumn) {
        return LevelSetupSystem.hitIceTile(this, projectile, fromColumn, toColumn);
    }
    boolean hitTomb(Projectile projectile, double fromColumn, double toColumn) {
        return LevelSetupSystem.hitTomb(this, projectile, fromColumn, toColumn);
    }
    boolean markIceWarmed(GridPosition position) {
        return warmedIcePositions.add(position);
    }
    void resetWarmedIcePositions() { warmedIcePositions.clear(); }
    boolean applyAutomaticBoostIfPresent(Plant plant) {
        return LevelSetupSystem.applyAutomaticBoostIfPresent(this, plant, plant.getName());
    }
    boolean applyAutomaticBoostIfPresent(Plant plant, String selectedPlantName) {
        return LevelSetupSystem.applyAutomaticBoostIfPresent(
            this, plant, selectedPlantName);
    }
    void activatePlantFood(Plant plant, String source) { LevelSetupSystem.activatePlantFood(this, plant, source); }
    void initializeSpecialLevel() { LevelSetupSystem.initializeSpecialLevel(this); }
    void initializeProtectedPlants() { LevelSetupSystem.initializeProtectedPlants(this); }
    void tickConveyor() { BattleTickSystem.tickConveyor(this); }
    void addConveyorCard() { BattleTickSystem.addConveyorCard(this); }
    void autoSelectStarterPlantsForConveyor() { BattleTickSystem.autoSelectStarterPlantsForConveyor(this); }
    void advanceOneTick() { BattleTickSystem.advanceOneTick(this); }
    void tickCooldowns() { BattleTickSystem.tickCooldowns(this); }
    void tickSuns() { BattleTickSystem.tickSuns(this); }
    void spawnSkySunIfNeeded() { BattleTickSystem.spawnSkySunIfNeeded(this); }
    boolean skySunEnabled() { return BattleTickSystem.skySunEnabled(this); }
    void performPlantActions() { BattleTickSystem.performPlantActions(this); }
    void produceSun(Plant plant) { BattleTickSystem.produceSun(this, plant); }
    int inferSunProduction(Plant plant) { return BattleTickSystem.inferSunProduction(this, plant); }
    void shootProjectiles(Plant plant) { BattleTickSystem.shootProjectiles(this, plant); }
    void attackHoming(Plant plant) { BattleTickSystem.attackHoming(this, plant); }
    void attackMelee(Plant plant) { BattleTickSystem.attackMelee(this, plant); }
    void moveProjectilesAndResolveHits() { BattleTickSystem.moveProjectilesAndResolveHits(this); }
    Zombie findProjectileTarget(int row, double fromColumn, double toColumn) {
        return BattleTickSystem.findProjectileTarget(this, row, fromColumn, toColumn);
    }
    void moveZombiesAndResolveCombat() { BattleTickSystem.moveZombiesAndResolveCombat(this); }
    void handleZombieAtHouse(Zombie crossingZombie) { BattleTickSystem.handleZombieAtHouse(this, crossingZombie); }
    void cleanupDestroyedEntities() { BattleTickSystem.cleanupDestroyedEntities(this); }
    void recordZombieKillStatistics(Zombie zombie, BoardPosition position) {
        BattleTickSystem.recordZombieKillStatistics(this, zombie, position);
    }
    void handleZombieRewards(Zombie zombie) { BattleTickSystem.handleZombieRewards(this, zombie); }
    void startNextWaveIfReady() { BattleTickSystem.startNextWaveIfReady(this); }
    void evaluateGameState() { BattleTickSystem.evaluateGameState(this); }
    boolean specialWinConditionReached() { return BattleTickSystem.specialWinConditionReached(this); }
    boolean specialLoseConditionReached() { return BattleTickSystem.specialLoseConditionReached(this); }
    int timedWarProgress() { return BattleTickSystem.timedWarProgress(this); }
    int timedWarWindowProgress() {
        ArrayDeque<Integer> samples = currentLevel.getTimedWarObjective() == TimedWarObjective.SUN
            ? timedWarSunSamples : timedWarKillSamples;
        int total = 0;
        for (int sample : samples) {
            total += sample;
        }
        return total;
    }
    void recordTimedWarSample() {
        int killDelta = Math.max(0, zombieKillCount - timedWarLastKillCount);
        int sunDelta = Math.max(0, totalSunCollected - timedWarLastSunCollected);
        timedWarLastKillCount = zombieKillCount;
        timedWarLastSunCollected = totalSunCollected;
        timedWarKillSamples.addLast(killDelta);
        timedWarSunSamples.addLast(sunDelta);
        int maxSamples = 5 * TICKS_PER_SECOND;
        while (timedWarKillSamples.size() > maxSamples) timedWarKillSamples.removeFirst();
        while (timedWarSunSamples.size() > maxSamples) timedWarSunSamples.removeFirst();
    }
    void markTornadoEntry(Zombie zombie) {
        if (zombie != null) tornadoEntryZombies.add(zombie);
    }
    public boolean enteredViaTornado(Zombie zombie) {
        return tornadoEntryZombies.contains(zombie);
    }
    void beginColdWind(int durationTicks) {
        coldWindRows.clear();
        coldWindUntilTick = elapsedTicks + Math.max(1, durationTicks);
    }
    void markColdWindRow(int row) { coldWindRows.add(row); }
    public Set<Integer> getColdWindRows() { return Collections.unmodifiableSet(coldWindRows); }
    public boolean isColdWindActive() { return elapsedTicks <= coldWindUntilTick; }
    public String specialStatus() { return PlantFoodSystem.specialStatus(this); }
    int protectedPlantsRemaining() { return PlantFoodSystem.protectedPlantsRemaining(this); }
    void handleImmediatePlant(Plant plant) { PlantFoodSystem.handleImmediatePlant(this, plant); }
    void detonatePlant(Plant plant) { PlantFoodSystem.detonatePlant(this, plant); }
    void detonatePlant(Plant plant, int damageMultiplier) {
        PlantFoodSystem.detonatePlant(this, plant, damageMultiplier);
    }
    void explodeRadioactiveSun(Sun sun) { PlantFoodSystem.explodeRadioactiveSun(this, sun); }
    void armMineWithPlantFood(Plant plant) { PlantFoodSystem.armMineWithPlantFood(this, plant); }
    void squashMultipleZombies(Plant plant, int count) { PlantFoodSystem.squashMultipleZombies(this, plant, count); }
    void drownMultipleZombies(Plant plant, int count) { PlantFoodSystem.drownMultipleZombies(this, plant, count); }
    void redirectWholeLane(Plant plant) { PlantFoodSystem.redirectWholeLane(this, plant); }
    void magnetizeAllZombies(Plant plant) { PlantFoodSystem.magnetizeAllZombies(this, plant); }
    void cloneLilyPads(Plant plant) { PlantFoodSystem.cloneLilyPads(this, plant); }
    void resetShortRangeShrooms() { PlantFoodSystem.resetShortRangeShrooms(this); }
    void hypnotizeRandomZombies(int count) { PlantFoodSystem.hypnotizeRandomZombies(this, count); }
    void killRandomZombies(int count, String sourceName) { PlantFoodSystem.killRandomZombies(this, count, sourceName); }
    void clearPlantLane(Plant plant) { PlantFoodSystem.clearPlantLane(this, plant); }
    void fumePlantFoodPush(Plant plant) { PlantFoodSystem.fumePlantFoodPush(this, plant); }
    void ensureConveyorCardAvailable(PlantDefinition definition, boolean conveyor) {
        PlantFoodSystem.ensureConveyorCardAvailable(this, definition, conveyor);
    }
    Plant createPlantForPlacement(PlantDefinition definition, int level) {
        return PlantFoodSystem.createPlantForPlacement(this, definition, level);
    }
    void finishPlantPurchase(String cooldownKey, String selectedPlantName,
                                         Plant plant, boolean conveyor) {
        PlantFoodSystem.finishPlantPurchase(this, cooldownKey, selectedPlantName, plant, conveyor);
    }
    boolean handleTerrainUtilityPlant(Plant plant, int row, int col) {
        return PlantFoodSystem.handleTerrainUtilityPlant(this, plant, row, col);
    }
    void validateSpecialPlantLocation(Plant plant, int row, int col) {
        PlantFoodSystem.validateSpecialPlantLocation(this, plant, row, col);
    }
    void warmAdjacentIce(Plant plant) { PlantFoodSystem.warmAdjacentIce(this, plant); }
    boolean assistDisabledPlant(Plant helper) { return PlantAttackSystem.assistDisabledPlant(this, helper); }
    void performTrapAction(Plant plant) { PlantAttackSystem.performTrapAction(this, plant); }
    void performPassivePlantAction(Plant plant) { PlantAttackSystem.performPassivePlantAction(this, plant); }
    void performActivePlantAction(Plant plant) { PlantAttackSystem.performActivePlantAction(this, plant); }
    void fireThreepeater(Plant plant) { PlantAttackSystem.fireThreepeater(this, plant); }
    void fireRotobaga(Plant plant) { PlantAttackSystem.fireRotobaga(this, plant); }
    void fireSplitPea(Plant plant) { PlantAttackSystem.fireSplitPea(this, plant); }
    void fireStarfruit(Plant plant) { PlantAttackSystem.fireStarfruit(this, plant); }
    void bowlBulbs(Plant plant) { PlantAttackSystem.bowlBulbs(this, plant); }
    void attackFumeShroom(Plant plant) { PlantAttackSystem.attackFumeShroom(this, plant); }
    void attackLobber(Plant plant) { PlantAttackSystem.attackLobber(this, plant); }
    void hypnotizeWithCaulipower(Plant plant) { PlantAttackSystem.hypnotizeWithCaulipower(this, plant); }
    void strikeWithBlueberry(Plant plant) { PlantAttackSystem.strikeWithBlueberry(this, plant); }
    void useMagnetShroom(Plant plant) { PlantAttackSystem.useMagnetShroom(this, plant); }
    void chompZombie(Plant plant) { PlantAttackSystem.chompZombie(this, plant); }
    boolean fireProjectileInRow(Plant plant, int row, int count, int maxHits) {
        return PlantAttackSystem.fireProjectileInRow(this, plant, row, count, maxHits);
    }
    void pullZombiesTowardSweetPotato(Plant plant) { PlantAttackSystem.pullZombiesTowardSweetPotato(this, plant); }
    void damageAdjacentZombies(Zombie center, Plant source, int damage, boolean lobbed) {
        PlantAttackSystem.damageAdjacentZombies(this, center, source, damage, lobbed);
    }
    void damageZombieFromPlant(Zombie zombie, Plant plant, int damage, boolean lobbed) {
        PlantAttackSystem.damageZombieFromPlant(this, zombie, plant, damage, lobbed);
    }
    ArrayList<Zombie> hostileZombies() { return PlantAttackSystem.hostileZombies(this); }
    boolean reflectProjectileIfNeeded(Projectile projectile, Zombie target) {
        return PlantAttackSystem.reflectProjectileIfNeeded(this, projectile, target);
    }
    Plant nearestPlantInRow(int row, double zombieColumn) {
        return PlantAttackSystem.nearestPlantInRow(this, row, zombieColumn);
    }
    int torchwoodMultiplier(Projectile projectile, double fromColumn, double toColumn) {
        return ZombieAbilitySystem.torchwoodMultiplier(this, projectile, fromColumn, toColumn);
    }
    void updateZombieEnvironmentState(Zombie zombie) { ZombieAbilitySystem.updateZombieEnvironmentState(this, zombie); }
    void performZombieSpecialAbility(Zombie zombie) { ZombieAbilitySystem.performZombieSpecialAbility(this, zombie); }
    void throwGargantuarImp(Zombie gargantuar) { ZombieAbilitySystem.throwGargantuarImp(this, gargantuar); }
    void stealSunWithRa(Zombie zombie) { ZombieAbilitySystem.stealSunWithRa(this, zombie); }
    void raiseTombs(Zombie zombie) { ZombieAbilitySystem.raiseTombs(this, zombie); }
    void throwHunterSnowball(Zombie zombie) { ZombieAbilitySystem.throwHunterSnowball(this, zombie); }
    void pushTroglobiteIce(Zombie zombie) { ZombieAbilitySystem.pushTroglobiteIce(this, zombie); }
    void hookPlantWithFisherman(Zombie zombie) { ZombieAbilitySystem.hookPlantWithFisherman(this, zombie); }
    void throwOctopus(Zombie zombie) { ZombieAbilitySystem.throwOctopus(this, zombie); }
    void transformPlantWithWizard(Zombie zombie) { ZombieAbilitySystem.transformPlantWithWizard(this, zombie); }
    void knightNearbyZombie(Zombie king) { ZombieAbilitySystem.knightNearbyZombie(this, king); }
    void useTurquoiseSkull(Zombie zombie) { ZombieAbilitySystem.useTurquoiseSkull(this, zombie); }
    void fireTurquoiseLaser(Zombie zombie) { ZombieAbilitySystem.fireTurquoiseLaser(this, zombie); }
    void launchProspectorDynamite(Zombie zombie) { ZombieAbilitySystem.launchProspectorDynamite(this, zombie); }
    void playPiano(Zombie pianist) { ZombieAbilitySystem.playPiano(this, pianist); }
    void moveHypnotizedZombie(Zombie zombie) { ZombieAbilitySystem.moveHypnotizedZombie(this, zombie); }
    Zombie nearestHostileZombieForHypnotized(Zombie ally) {
        return ZombieAbilitySystem.nearestHostileZombieForHypnotized(this, ally);
    }
    boolean shouldZombieBypassPlant(Zombie zombie, Plant plant) {
        return ZombieAbilitySystem.shouldZombieBypassPlant(this, zombie, plant);
    }
    boolean isStationaryZombie(Zombie zombie) { return ZombieAbilitySystem.isStationaryZombie(this, zombie); }
    void resolveZombiePlantCombat(Zombie zombie, Plant plant) {
        ZombieAbilitySystem.resolveZombiePlantCombat(this, zombie, plant);
    }
    void moveZombieToAdjacentLane(Zombie zombie) { ZombieAbilitySystem.moveZombieToAdjacentLane(this, zombie); }
    void explodeDestroyedDefender(Plant plant) { ZombieAbilitySystem.explodeDestroyedDefender(this, plant); }
    void removeUnsupportedWaterPlants() { BattleRuleSystem.removeUnsupportedWaterPlants(this); }
    void releaseWizardTransformations(String wizardId) {
        BattleRuleSystem.releaseWizardTransformations(this, wizardId);
    }
    void dropStolenSunFromZombie(Zombie zombie) { BattleRuleSystem.dropStolenSunFromZombie(this, zombie); }
    void removeInstantPlant(Plant plant) { BattleRuleSystem.removeInstantPlant(this, plant); }
    void activateMint(Plant mint) { BattleRuleSystem.activateMint(this, mint); }
    boolean plantMatchesMint(Plant plant, PlantAbility mint) {
        return BattleRuleSystem.plantMatchesMint(this, plant, mint);
    }
    void freezeAllZombies(Plant source, boolean killWeak) { BattleRuleSystem.freezeAllZombies(this, source, killWeak); }
    boolean explosionHits(PlantAbility ability, int rowDistance,
                                      double columnDistance) {
        return BattleRuleSystem.explosionHits(this, ability, rowDistance, columnDistance);
    }
    void launchGrapeshotFragments(Plant plant, int multiplier) {
        BattleRuleSystem.launchGrapeshotFragments(this, plant, multiplier);
    }
    int calculateNextSkySunTick() { return BattleRuleSystem.calculateNextSkySunTick(this); }
    int calculateSkySunIntervalTicks() { return BattleRuleSystem.calculateSkySunIntervalTicks(this); }
    void addForcedPlantSelections() { BattleRuleSystem.addForcedPlantSelections(this); }
    void validatePlantSelectionRule(PlantDefinition definition) {
        BattleRuleSystem.validatePlantSelectionRule(this, definition);
    }
    boolean matchesPlantFamily(PlantDefinition definition, String family) {
        return BattleRuleSystem.matchesPlantFamily(this, definition, family);
    }
    boolean containsNormalized(List<String> values, String expected) {
        return BattleRuleSystem.containsNormalized(this, values, expected);
    }
    boolean isSunProducerDefinition(PlantDefinition definition) {
        return BattleRuleSystem.isSunProducerDefinition(this, definition);
    }
    boolean isPreWaveSetup() { return BattleRuleSystem.isPreWaveSetup(this); }
    void recordPlantUsage(Plant plant) { BattleRuleSystem.recordPlantUsage(this, plant); }
    void recordPlantUsage(Plant plant, int row, int column) {
        BattleRuleSystem.recordPlantUsage(this, plant, row, column);
    }
    void autoSelectStarterPlants() { BattleRuleSystem.autoSelectStarterPlants(this); }
    void requirePlantSelection() { BattleRuleSystem.requirePlantSelection(this); }
    void requireRunning() { BattleRuleSystem.requireRunning(this); }
    void addEvent(String event) { BattleRuleSystem.addEvent(this, event); }
    void recordZombieEncounter(Zombie zombie) {
        if (zombie != null && zombie.getName() != null && !zombie.getName().isBlank()) {
            encounteredZombieNames.add(zombie.getName());
        }
    }
    String display(int row, int col) { return BattleRuleSystem.display(this, row, col); }
    String formatSeconds(int ticks) { return BattleRuleSystem.formatSeconds(this, ticks); }
    String formatColumn(double column) { return BattleRuleSystem.formatColumn(this, column); }

}
