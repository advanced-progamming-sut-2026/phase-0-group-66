package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

final class LevelSetupSystem {
    private LevelSetupSystem() { }

    static Map<String, Integer> getPlantKillCounts(Game engine) {
        return Collections.unmodifiableMap(engine.plantKillCounts);
    }
    static int getPlantKills(Game engine, String plantName) {
        return engine.plantKillCounts.getOrDefault(PlantDefinition.normalizeKey(plantName), 0);
    }
    static int getKillsWithinThirtySeconds(Game engine) { return engine.killsWithinThirtySeconds; }
    static int getFirstColumnNoMowerKills(Game engine) { return engine.firstColumnNoMowerKills; }
    static int getPiercingProjectileHits(Game engine) { return engine.piercingProjectileHits; }
    static int getMultiKillZombieCount(Game engine) { return engine.multiKillZombieCount; }
    static int getSunProducerPlantsPlanted(Game engine) { return engine.sunProducerPlantsPlanted; }
    static List<String> getPlantedPlantNames(Game engine) { return List.copyOf(engine.plantedPlantNames); }
    static List<String> getPlantedPlantFamilies(Game engine) { return List.copyOf(engine.plantedPlantFamilies); }
    static boolean areZombieWavesStarted(Game engine) { return engine.zombieWavesStarted; }
    static Map<String, Integer> normalizePlantLevels(Game engine, Map<String, Integer> levels) {
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
    static int adjustedWaveCost(Game engine, int baseCost) {
        double multiplier = engine.difficultyLevel / 3.0;
        return Math.max(100, (int) Math.round(baseCost * multiplier));
    }
    static void configureZombieDifficultyAndDrops(Game engine, Wave wave) {
        for (Zombie zombie : wave.getZombies()) {
            zombie.applyDifficulty(engine.difficultyLevel);
            boolean glowing = zombie.getDefinition().canSpawnPlantFood()
                && engine.random.nextInt(100) < 5;
            zombie.setGlowing(glowing);
            if (glowing) {
                engine.addEvent("A glowing " + zombie.getName() + " joined the wave.");
            }
        }
    }
    static void initializeSeasonTerrain(Game engine) {
        if (engine.currentLevel.getSeason() == SeasonType.ANCIENT_EGYPT) {
            engine.addRandomTombs(3, false);
        } else if (engine.currentLevel.getSeason() == SeasonType.FROSTBITE_CAVES) {
            engine.board.getTile(1, 4).setTileType(TileType.SLIPPERY_UP);
            engine.board.getTile(3, 5).setTileType(TileType.SLIPPERY_DOWN);
            addInitialFrozenZombie(engine, 0, 6);
            engine.board.getTile(4, 6).setTileType(TileType.ICE);
        } else if (engine.currentLevel.getSeason() == SeasonType.BIG_WAVE_BEACH) {
            engine.setBeachWaterLevel(7);
            engine.board.getTile(1, 6).setTileType(TileType.LOW_TIDE);
            engine.board.getTile(3, 6).setTileType(TileType.LOW_TIDE);
        } else if (engine.currentLevel.getSeason() == SeasonType.DARK_AGES) {
            engine.board.getTile(1, 5).setTileType(TileType.NECROMANCY);
            engine.board.getTile(3, 6).setTileType(TileType.NECROMANCY);
            engine.addRandomTombs(3, true);
        }
    }
    private static void addInitialFrozenZombie(Game engine, int row, int col) {
        Zombie zombie = engine.zombieFactory.createZombie("Basic Zombie");
        zombie.applyDifficulty(engine.difficultyLevel);
        zombie.setIceImmune(true);
        engine.board.addFrozenZombie(zombie, row, col);
        engine.addEvent("A " + zombie.getName() + " is trapped in 600 HP ice at "
            + new GridPosition(row, col) + ".");
    }
    static int addRandomTombs(Game engine, int count, boolean mayContainRewards) {
        List<GridPosition> candidates = new ArrayList<>();
        int lastCandidateColumn = Math.min(engine.board.getCols() - 1, 6);
        for (int row = 0; row < engine.board.getRows(); row++) {
            for (int col = 3; col <= lastCandidateColumn; col++) {
                GridPosition position = new GridPosition(row, col);
                Tile tile = engine.board.getTile(row, col);
                boolean suitableGround = tile.getType() == TileType.NORMAL
                    || tile.getType() == TileType.NECROMANCY;
                if (!engine.tombs.containsKey(position) && tile.getPlant() == null
                    && suitableGround && tile.getZombies().isEmpty()) {
                    candidates.add(position);
                }
            }
        }
        int tombCount = Math.min(count, candidates.size());
        for (int index = 0; index < tombCount; index++) {
            GridPosition position = candidates.remove(engine.random.nextInt(candidates.size()));
            boolean sun = mayContainRewards && engine.random.nextInt(5) == 0;
            boolean plantFood = mayContainRewards && !sun && engine.random.nextInt(10) == 0;
            Tile tile = engine.board.getTile(position.getRow(), position.getColumn());
            Tomb tomb = new Tomb(position.getRow(), position.getColumn(), sun, plantFood,
                tile.getType());
            engine.tombs.put(position, tomb);
            tile.setTileType(TileType.TOMB);
            String reward = sun ? " containing 50 sun" : plantFood
                ? " containing plant food" : "";
            String tombKind = engine.currentLevel.getSeason() == SeasonType.DARK_AGES
                ? "Dark Ages tomb" : "tomb";
            engine.addEvent("A " + tombKind + reward + " formed at " + position + ".");
        }
        return tombCount;
    }
    static void applyWaveStartSeasonEffects(Game engine, Wave wave) {
        if (engine.currentLevel.getSeason() == SeasonType.FROSTBITE_CAVES) {
            applyColdWind(engine, wave);
        } else if (engine.currentLevel.getSeason() == SeasonType.DARK_AGES) {
            int requested = 1 + engine.random.nextInt(2);
            int created = engine.addRandomTombs(requested, true);
            engine.addEvent("Dark Ages wave " + wave.getWaveNumber() + " formed "
                + created + " new tomb(s) at wave start.");
        }
    }

    private static void applyColdWind(Game engine, Wave wave) {
        ArrayList<Integer> rows = new ArrayList<>();
        for (int row = 0; row < engine.board.getRows(); row++) {
            rows.add(row);
        }
        Collections.shuffle(rows, engine.random);
        int affectedRowCount = 1 + engine.random.nextInt(Math.min(3, rows.size()));
        ArrayList<Integer> affectedRows = new ArrayList<>();
        int layeredPlants = 0;
        int firePlantsIgnored = 0;
        for (int index = 0; index < affectedRowCount; index++) {
            int row = rows.get(index);
            affectedRows.add(row + 1);
            for (Plant plant : engine.board.getPlantsInRow(row)) {
                if (plant.getDefinition().hasTag("Fire")) {
                    firePlantsIgnored++;
                } else {
                    int previousLayers = plant.getIceHits();
                    plant.addIceLayer();
                    if (plant.getIceHits() > previousLayers) {
                        layeredPlants++;
                    }
                }
            }
        }
        engine.addEvent("Cold wind struck row(s) " + affectedRows + " at the start of wave "
            + wave.getWaveNumber() + "; ice layers added=" + layeredPlants
            + ", fire plants ignored=" + firePlantsIgnored + ".");
    }

    static void configureWaveForSeason(Game engine, Wave wave) {
        boolean finalWave = engine.nextWaveIndex == engine.currentLevel.getWaves().size() - 1;
        for (Zombie zombie : wave.getZombies()) {
            if (engine.currentLevel.getSeason() == SeasonType.FROSTBITE_CAVES) {
                zombie.setIceImmune(true);
            }
            if (engine.currentLevel.getSeason() == SeasonType.ANCIENT_EGYPT
                && finalWave && engine.random.nextBoolean()) {
                double shifted = Math.max(4.0,
                    zombie.getPosition().getColumn() - 1 - engine.random.nextInt(4));
                zombie.setPosition(new BoardPosition(zombie.getPosition().getRow(), shifted));
            }
        }
        if (engine.currentLevel.getSeason() == SeasonType.BIG_WAVE_BEACH) {
            int waterStart = 6 + engine.random.nextInt(3);
            engine.setBeachWaterLevel(waterStart);
        }
        if (engine.currentLevel.getSeason() == SeasonType.DARK_AGES) {
            engine.spawnNecromancyZombie(wave);
        }
    }
    static void setBeachWaterLevel(Game engine, int startColumn) {
        for (int row = 0; row < engine.board.getRows(); row++) {
            for (int col = 0; col < engine.board.getCols(); col++) {
                Tile tile = engine.board.getTile(row, col);
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
    static void spawnNecromancyZombie(Game engine, Wave wave) {
        for (Map.Entry<GridPosition, Tomb> entry : engine.tombs.entrySet()) {
            if (!entry.getValue().isNecromancySite() || !engine.random.nextBoolean()) {
                continue;
            }
            GridPosition position = entry.getKey();
            Zombie zombie = engine.zombieFactory.createZombie("Basic Zombie");
            zombie.setPosition(new BoardPosition(position.getRow(), position.getColumn() + 0.5));
            wave.addZombie(zombie);
            engine.addEvent("Necromancy raised a " + zombie.getName() + " beneath the tomb at "
                + position + ".");
        }
    }
    static void applySlipperyTile(Game engine, Zombie zombie) {
        BoardPosition position = zombie.getPosition();
        if (position == null) {
            return;
        }
        int col = (int) Math.floor(position.getColumn());
        if (!engine.board.isInside(position.getRow(), col)) {
            return;
        }
        TileType type = engine.board.getTile(position.getRow(), col).getType();
        if (zombie.getAbility() == ZombieAbility.DODO_RIDER
            && zombie.getDefinition().getSpecialPropertyStrings(
                "projectTileTypesToFlyOver").contains(type.name())) {
            if (!zombie.isFlying()) {
                zombie.startFlight(zombie.getDefinition().getSpecialPropertyDouble(
                    "projectFlightDistanceTiles", 2.0));
                engine.addEvent("Dodo Rider flew over a " + type + " tile without changing lane.");
            }
            return;
        }
        int targetRow = position.getRow();
        if (type == TileType.SLIPPERY_UP) {
            targetRow--;
        } else if (type == TileType.SLIPPERY_DOWN) {
            targetRow++;
        }
        if (targetRow >= 0 && targetRow < engine.board.getRows() && targetRow != position.getRow()) {
            zombie.setPosition(position.withRow(targetRow));
        }
    }
    static boolean hitIceTile(Game engine, Projectile projectile, double fromColumn,
                              double toColumn) {
        int row = projectile.getPosition().getRow();
        for (int col = Math.max(0, (int) Math.floor(fromColumn));
             col <= Math.min(engine.board.getCols() - 1, (int) Math.floor(toColumn)); col++) {
            Tile tile = engine.board.getTile(row, col);
            if (tile.getType() != TileType.ICE) {
                continue;
            }
            int before = tile.getIceHealth();
            tile.damageIce(projectile.getDamage() * projectile.getDamageMultiplier(),
                projectile.getImpactType() == ProjectileType.FIRE);
            projectile.deactivate();
            int after = tile.getIceHealth();
            if (after == 0) {
                engine.addEvent("The 600 HP ice at " + tile.getPosition()
                    + " broke and released its trapped entity.");
            } else {
                engine.addEvent("Projectile damaged ice at " + tile.getPosition() + " from "
                    + before + " to " + after + " HP.");
            }
            return true;
        }
        return false;
    }

    static boolean hitTomb(Game engine, Projectile projectile, double fromColumn, double toColumn) {
        int row = projectile.getPosition().getRow();
        for (Map.Entry<GridPosition, Tomb> entry : new ArrayList<>(engine.tombs.entrySet())) {
            GridPosition position = entry.getKey();
            if (position.getRow() != row || position.getColumn() + 0.001 < fromColumn
                || position.getColumn() - 0.001 > toColumn) {
                continue;
            }
            Tomb tomb = entry.getValue();
            tomb.takeDamage(projectile.getDamage());
            projectile.deactivate();
            if (tomb.isDestroyed()) {
                engine.board.getTile(position.getRow(), position.getColumn())
                    .setTileType(tomb.getUnderlyingTileType());
                engine.tombs.remove(position);
                if (tomb.containsSun()) {
                    engine.board.addSun(new Sun(50, position));
                }
                if (tomb.containsPlantFood() && engine.inventory.getPlantFoodCapacityLeft() > 0) {
                    engine.inventory.addPlantFood(1);
                    engine.addEvent("The tomb released a plant food; total="
                        + engine.inventory.getPlantFoods() + ".");
                }
                engine.addEvent("Tomb destroyed at " + position + ".");
            }
            return true;
        }
        return false;
    }
    static boolean applyAutomaticBoostIfPresent(Game engine, Plant plant,
                                                String selectedPlantName) {
        String activeName = plant.getName();
        String selectedName = selectedPlantName == null ? activeName : selectedPlantName;
        if (engine.levelBoostedPlants.contains(selectedName)) {
            engine.activatePlantFood(plant, "level boost");
            return true;
        }
        if (engine.inventory.consumeStoredBoost(activeName)) {
            engine.activatePlantFood(plant, "stored greenhouse boost");
            engine.addEvent("Stored boost for " + activeName + " was consumed.");
            return true;
        }
        return false;
    }
    static void activatePlantFood(Game engine, Plant plant, String source) {
        if (plant == null || plant.isDestroyed() || plant.getPosition() == null) {
            throw new IllegalStateException("Plant food cannot be applied to this plant.");
        }
        if (plant.getDefinition().getPlantFoodType() == PlantFoodType.NONE) {
            engine.addEvent(plant.getName() + " has no plant-food effect.");
            return;
        }
        plant.usePlantFood();
        PlantFoodBehaviorFactory.activate(engine, plant);
        engine.cleanupDestroyedEntities();
        engine.addEvent("Plant food activated on " + plant.getName() + " from " + source + ".");
    }
    static void initializeSpecialLevel(Game engine) {
        engine.currentLevel.getRuleStrategy().initializeBattle(engine);
        engine.addEvent("Special rule: " + engine.currentLevel.getSpecialRuleSummary());
    }
    static void initializeProtectedPlants(Game engine) {
        List<GridPosition> positions = engine.currentLevel.getProtectedPlantPositions();
        if (positions.isEmpty()) {
            positions = List.of(new GridPosition(0, 2), new GridPosition(2, 2),
                new GridPosition(4, 2));
        }
        for (GridPosition position : positions) {
            Plant protectedPlant = engine.plantFactory.createPlant(engine.currentLevel.getProtectedPlantType(),
                engine.plantLevels.getOrDefault(PlantDefinition.normalizeKey(
                    engine.currentLevel.getProtectedPlantType()), 1));
            engine.board.placePlant(protectedPlant, position.getRow(), position.getColumn());
            engine.endangeredPositions.add(position);
        }
        engine.addEvent("Protected seed plants were placed at " + positions + ".");
    }
}
