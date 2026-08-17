package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

final class BattleQuerySystem {
    private BattleQuerySystem() { }

    static void addSun(Game engine, int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Sun amount cannot be negative.");
        }
        engine.sunAmount += amount;
        engine.addEvent("Cheat added " + amount + " suns; total=" + engine.sunAmount + ".");
    }
    static void removeAllCooldowns(Game engine) {
        for (String key : new ArrayList<>(engine.cooldownTicks.keySet())) {
            engine.cooldownTicks.put(key, 0);
        }
        engine.addEvent("All plant cooldowns were removed.");
    }
    static void releaseNuke(Game engine) {
        engine.requireRunning();
        for (Zombie zombie : new ArrayList<>(engine.board.getZombies())) {
            zombie.kill();
        }
        engine.addEvent("The nuke destroyed every zombie on the board.");
        engine.cleanupDestroyedEntities();
        engine.evaluateGameState();
    }
    static void spawnZombie(Game engine, String zombieType, int row, double column) {
        engine.requireRunning();
        if (row < 0 || row >= engine.board.getRows()) {
            throw new IllegalArgumentException("Zombie row is outside the board.");
        }
        Zombie zombie = engine.zombieFactory.createZombie(zombieType);
        zombie.applyDifficulty(engine.difficultyLevel);
        zombie.setGlowing(zombie.getDefinition().canSpawnPlantFood() && engine.random.nextInt(100) < 5);
        zombie.setPosition(new BoardPosition(row, column));
        engine.board.addZombie(zombie);
        engine.recordZombieEncounter(zombie);
        ZombieObjectSystem.ensureZombieCompanions(engine);
        engine.addEvent("Cheat spawned " + zombie.getName() + " at ("
            + engine.formatColumn(column) + ", " + (row + 1) + ").");
    }
    static boolean checkWinCondition(Game engine) {
        if (engine.currentLevel == null || engine.board == null) {
            return false;
        }
        SpecialLevelRule rule = engine.currentLevel.getRuleStrategy();
        if (rule.hasSpecialWin(engine)) {
            return true;
        }
        if (rule.blocksNormalWin(engine) || engine.externalWinControlled) {
            return false;
        }
        return engine.nextWaveIndex >= engine.currentLevel.getWaves().size()
            && engine.pendingZombieSpawns.isEmpty()
            && engine.board.getZombies().isEmpty()
            && engine.board.getProspectorDynamites().isEmpty();
    }
    static boolean checkLoseCondition(Game engine) {
        return engine.gameState == GameState.LOST;
    }
    static List<String> drainEvents(Game engine) {
        List<String> result = List.copyOf(engine.events);
        engine.events.clear();
        return result;
    }
    static List<String> getSelectedPlants(Game engine) {
        return List.copyOf(engine.selectedPlants);
    }
    static boolean isPlantAvailableForSelection(Game engine, String plantType) {
        PlantDefinition definition = engine.plantFactory.findDefinition(plantType).orElse(null);
        if (definition == null || engine.currentLevel == null) {
            return false;
        }
        if (engine.currentLevel.getRuleStrategy().usesConveyor()) {
            return engine.containsNormalized(engine.currentLevel.getConveyorPlants(),
                definition.getName());
        }
        try {
            engine.validatePlantSelectionRule(definition);
            return true;
        } catch (IllegalStateException exception) {
            return false;
        }
    }
    static Map<String, Integer> getCooldownTicks(Game engine) {
        return Collections.unmodifiableMap(engine.cooldownTicks);
    }
    static int getCooldownTicks(Game engine, String plantType) {
        PlantDefinition definition = engine.plantFactory.findDefinition(plantType)
            .orElseThrow(() -> new IllegalArgumentException("Plant does not exist: " + plantType));
        return engine.cooldownTicks.getOrDefault(definition.getNormalizedName(), 0);
    }
    static String plantStatus(Game engine) {
        StringBuilder output = new StringBuilder();
        for (String name : engine.selectedPlants) {
            PlantDefinition definition = engine.plantFactory.findDefinition(name).orElseThrow();
            int remaining = engine.cooldownTicks.getOrDefault(definition.getNormalizedName(), 0);
            int level = engine.plantLevels.getOrDefault(definition.getNormalizedName(), 1);
            Plant preview = engine.plantFactory.createPlant(name, level);
            output.append(name).append(": level=").append(preview.getPlantLevel())
                .append(", cost=").append(preview.getSunCost())
                .append(", damage=").append(preview.getAttackPower())
                .append(", health=").append(preview.getMaxHealth())
                .append(", available=").append(remaining <= 0);
            if (remaining > 0) {
                output.append(", remaining=").append(engine.formatSeconds(remaining)).append('s');
            }
            output.append(System.lineSeparator());
        }
        return output.toString();
    }
    static String tileStatus(Game engine, int row, int col) {
        Tile tile = engine.board.getTile(row, col);
        StringBuilder output = new StringBuilder();
        output.append("Tile ").append(engine.display(row, col)).append(": type=")
            .append(tile.getTileType());
        if (tile.getType() == TileType.ICE) {
            output.append(", iceHealth=").append(tile.getIceHealth()).append("/600")
                .append(", trappedEntity=").append(tile.hasTrappedEntity());
        }
        output.append(System.lineSeparator());
        Plant plant = tile.getPlant();
        if (plant == null) {
            output.append("plant: none").append(System.lineSeparator());
        } else {
            output.append("plant: ").append(plant.getName()).append(", level=")
                .append(plant.getPlantLevel()).append(", health=")
                .append(plant.getHealth()).append('/').append(plant.getMaxHealth())
                .append(", shield=").append(plant.getPlantFoodShield())
                .append(", trappedInTileIce=").append(plant.isTrappedInIceTile())
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
                    .append(zombie.getEffectiveHealth()).append(", trappedInTileIce=")
                    .append(zombie.isTrappedInIceTile()).append(System.lineSeparator());
            }
        }
        List<PushedObstacle> obstacles = engine.board.getPushedObstaclesAt(row, col);
        if (!obstacles.isEmpty()) {
            output.append("lane objects:").append(System.lineSeparator());
            for (PushedObstacle obstacle : obstacles) {
                output.append("- ").append(obstacle.getType()).append(", health=")
                    .append(obstacle.getHealth()).append('/')
                    .append(obstacle.getMaximumHealth()).append(System.lineSeparator());
            }
        }
        for (ProspectorDynamite dynamite
            : engine.board.getProspectorDynamitesAt(row, col)) {
            output.append("- PROSPECTOR_DYNAMITE, damagePerSecond=")
                .append(dynamite.getDamagePerSecond()).append(System.lineSeparator());
        }
        return output.toString();
    }
    static String zombieInfo(Game engine) {
        if (engine.board == null || engine.board.getZombies().isEmpty()) {
            return "No zombies are on the board.";
        }
        StringBuilder output = new StringBuilder();
        for (Zombie zombie : engine.board.getZombies()) {
            output.append(zombie.getName()).append(':').append(System.lineSeparator())
                .append("position: ").append(zombie.getPosition()).append(System.lineSeparator())
                .append("health: ").append(zombie.getHealth()).append(System.lineSeparator())
                .append("armor:").append(System.lineSeparator());
            for (Armor armor : zombie.getArmors()) {
                output.append("  ").append(armor.getDefinition().getArmorType()).append(": ")
                    .append(armor.getHealth()).append(System.lineSeparator());
            }
            output.append("effects:").append(System.lineSeparator());
            if (zombie.isTrappedInIceTile()) {
                output.append("  trapped in tile ice: inactive until 600 HP ice breaks")
                    .append(System.lineSeparator());
            }
            if (zombie.isGlowing()) {
                output.append("  glowing: drops plant food on death")
                    .append(System.lineSeparator());
            }
            if (zombie.getChilledTicks() > 0) {
                output.append("  chilled: ").append(engine.formatSeconds(zombie.getChilledTicks()))
                    .append('s').append(System.lineSeparator());
            }
            if (zombie.isSubmerged()) {
                output.append("  submerged: direct shots blocked")
                    .append(System.lineSeparator());
            }
            if (zombie.isSurfacedForCombat()) {
                output.append("  surfaced: vulnerable while eating")
                    .append(System.lineSeparator());
            }
            if (zombie.isJuggling()) {
                output.append("  spinning: ")
                    .append(engine.formatSeconds(zombie.getJugglingTicks()))
                    .append('s').append(System.lineSeparator());
            }
            if (zombie.isFlying()) {
                output.append("  flying: ")
                    .append(engine.formatColumn(zombie.getFlightDistanceRemaining()))
                    .append(" tiles remaining").append(System.lineSeparator());
            }
        }
        return output.toString();
    }
    static String summary(Game engine) {
        if (engine.currentLevel == null) {
            return "No level is prepared.";
        }
        int waveNumber = engine.currentWave == null ? 0 : engine.currentWave.getWaveNumber();
        return "state=" + engine.gameState + ", level=" + engine.currentLevel.getLevelId()
            + ", difficulty=" + engine.difficultyLevel
            + ", wave=" + waveNumber + "/" + engine.currentLevel.getWaves().size()
            + ", sun=" + engine.sunAmount + ", plantFoods=" + engine.inventory.getPlantFoods()
            + ", ticks=" + engine.elapsedTicks
            + ", special={" + engine.specialStatus() + "}"
            + (engine.conveyorCards.isEmpty() ? "" : ", conveyor=" + engine.conveyorCards);
    }
    static GameState getGameState(Game engine) {
        return engine.gameState;
    }
    static Chapter getCurrentChapter(Game engine) {
        return engine.currentChapter;
    }
    static Level getCurrentLevel(Game engine) {
        return engine.currentLevel;
    }
    static Board getBoard(Game engine) {
        return engine.board;
    }
    static Wave getCurrentWave(Game engine) {
        return engine.currentWave;
    }
    static int getSunAmount(Game engine) {
        return engine.sunAmount;
    }
    static int getElapsedTicks(Game engine) {
        return engine.elapsedTicks;
    }
    static int getLostPlantsCount(Game engine) {
        return engine.lostPlantsCount;
    }
    static int getTotalSunCollected(Game engine) { return engine.totalSunCollected; }
    static int getZombieKillCount(Game engine) { return engine.zombieKillCount; }
    static int getExplosivePlantsUsed(Game engine) { return engine.explosivePlantsUsed; }
    static int getLawnMowerKills(Game engine) { return engine.lawnMowerKills; }
    static Map<String, Integer> getConveyorCards(Game engine) {
        return Collections.unmodifiableMap(engine.conveyorCards);
    }
}
