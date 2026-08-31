package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

final class BattleTickSystem {
    private static final double PROJECTILE_HIT_RADIUS = 0.35;

    private BattleTickSystem() { }

    static void tickConveyor(Game engine) {
        if (!engine.currentLevel.getRuleStrategy().usesConveyor()
            || engine.elapsedTicks < engine.nextConveyorTick) {
            return;
        }
        engine.addConveyorCard();
        engine.nextConveyorTick += 12 * Game.TICKS_PER_SECOND;
    }
    static void addConveyorCard(Game engine) {
        if (engine.selectedPlants.isEmpty()) {
            return;
        }
        List<String> options = List.copyOf(engine.selectedPlants);
        String plant = options.get(engine.random.nextInt(options.size()));
        engine.conveyorCards.merge(plant, 1, Integer::sum);
        engine.addEvent("Conveyor produced a " + plant + " card.");
    }
    static void autoSelectStarterPlantsForConveyor(Game engine) {
        List<String> pool = engine.currentLevel.getConveyorPlants().isEmpty()
            ? List.of("Sunflower", "Peashooter", "Wall-nut")
            : engine.currentLevel.getConveyorPlants();
        for (String starter : pool) {
            PlantDefinition definition = engine.plantFactory.findDefinition(starter).orElse(null);
            if (definition != null) {
                engine.selectedPlants.add(definition.getName());
                engine.cooldownTicks.put(definition.getNormalizedName(), 0);
            }
        }
    }
    static void advanceOneTick(Game engine) {
        engine.elapsedTicks++;
        if (engine.elapsedTicks % Game.TICKS_PER_SECOND == 0) {
            engine.resetWarmedIcePositions();
        }
        tickPendingZombieSpawns(engine);
        engine.tickCooldowns();
        engine.tickConveyor();
        engine.tickSuns();
        engine.spawnSkySunIfNeeded();
        ZombieObjectSystem.ensureZombieCompanions(engine);
        ZombieAbilitySystem.refreshSnorkelStates(engine);
        engine.performPlantActions();
        engine.moveProjectilesAndResolveHits();
        moveGrapeshotFragmentsAndResolveHits(engine);
        ZombieObjectSystem.tickReflectedProjectiles(engine);
        ZombieObjectSystem.tickPushedObstacles(engine);
        ZombieObjectSystem.tickProspectorDynamites(engine);
        engine.moveZombiesAndResolveCombat();
        engine.cleanupDestroyedEntities();
        engine.recordTimedWarSample();
        engine.startNextWaveIfReady();
        engine.board.refreshZombieTiles();
        engine.evaluateGameState();
    }

    static void moveGrapeshotFragmentsAndResolveHits(Game engine) {
        for (GrapeshotFragment fragment
            : new ArrayList<>(engine.board.getGrapeshotFragments())) {
            fragment.tick(engine.board);
            if (!fragment.isActive()) {
                engine.board.removeGrapeshotFragment(fragment);
            }
        }
    }
    static void tickPendingZombieSpawns(Game engine) {
        Iterator<Map.Entry<Zombie, Integer>> iterator =
            engine.pendingZombieSpawns.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Zombie, Integer> entry = iterator.next();
            int remaining = entry.getValue() - 1;
            if (remaining > 0) {
                entry.setValue(remaining);
                continue;
            }
            engine.board.addZombie(entry.getKey());
            iterator.remove();
        }
    }

    static void tickCooldowns(Game engine) {
        for (Map.Entry<String, Integer> entry : engine.cooldownTicks.entrySet()) {
            if (entry.getValue() > 0) {
                entry.setValue(entry.getValue() - 1);
            }
        }
    }
    static void tickSuns(Game engine) {
        for (Sun sun : new ArrayList<>(engine.board.getSuns())) {
            if (sun.tick()) {
                engine.addEvent("Sun reached the ground at position " + sun.getPosition() + ".");
            }
        }
    }
    static void spawnSkySunIfNeeded(Game engine) {
        if (!engine.skySunEnabled() || engine.elapsedTicks < engine.nextSkySunTick) {
            return;
        }
        int roll = engine.random.nextInt(100);
        SunType type = roll < 80 ? SunType.NORMAL : roll < 95
            ? SunType.SPECIAL : SunType.RADIOACTIVE;
        GridPosition position = new GridPosition(engine.random.nextInt(engine.board.getRows()),
            engine.random.nextInt(engine.board.getCols()));
        Sun sun = Sun.falling(type, position);
        engine.board.addSun(sun);
        engine.addEvent("New " + type.name().toLowerCase() + " sun is dropping at position "
            + position + ".");
        engine.nextSkySunTick = engine.elapsedTicks + engine.calculateSkySunIntervalTicks();
    }
    static boolean skySunEnabled(Game engine) {
        return engine.currentLevel.getSeason() != SeasonType.DARK_AGES
            && engine.currentLevel.getRuleStrategy().allowsSkySun();
    }
    static void performPlantActions(Game engine) {
        for (Plant plant : new ArrayList<>(engine.board.getPlants())) {
            if (plant.isDestroyed() || plant.getPosition() == null) {
                continue;
            }
            plant.tickRuntimeState();
            engine.warmAdjacentIce(plant);
            if (plant.isDestroyed() || !plant.isOperational()) {
                continue;
            }
            if (plant.getAbility().isMint()) {
                engine.performPassivePlantAction(plant);
                continue;
            }
            if (plant.isTrap()) {
                engine.performTrapAction(plant);
                continue;
            }
            engine.performPassivePlantAction(plant);
            if (!plant.tickActionTimer()) {
                continue;
            }
            if (!plant.isSunProducer() && engine.assistDisabledPlant(plant)) {
                plant.resetActionTimer();
                continue;
            }
            if (plant.isSunProducer()) {
                engine.produceSun(plant);
            } else {
                engine.performActivePlantAction(plant);
                plant.resetActionTimer();
            }
        }
    }
    static void produceSun(Game engine, Plant plant) {
        GridPosition position = plant.getPosition();
        if (engine.waitingSunProducers.contains(position)
            || engine.board.hasPlantGeneratedSunAt(position)) {
            return;
        }
        int amount;
        if (plant.getAbility() == PlantAbility.SUN_SHROOM) {
            amount = plant.getSunShroomProduction() + plant.getSunProductionBonus();
        } else if (plant instanceof Sunflower sunflower) {
            amount = sunflower.getProductionAmount();
        } else {
            int configured = plant.getDefinition().getAbilityParameterInt("sun",
                (int) Math.round(plant.getDefinition().getAbilityPower()));
            amount = Math.max(0, configured) + plant.getSunProductionBonus();
        }
        if (plant.hasDoubleSunChance() && engine.random.nextBoolean()) {
            amount *= 2;
        }
        Sun sun = new Sun(amount, position);
        engine.board.addSun(sun);
        engine.waitingSunProducers.add(position);
        engine.addEvent("Plant " + plant.getName() + " produced a " + amount
            + " sun at " + position + ".");
    }
    static int inferSunProduction(Game engine, Plant plant) {
        return Math.max(0, plant.getDefinition().getAbilityParameterInt("sun",
            (int) Math.round(plant.getDefinition().getAbilityPower())));
    }
    static void shootProjectiles(Game engine, Plant plant) {
        GridPosition position = plant.getPosition();
        Zombie target = engine.board.findNearestZombieAhead(position.getRow(), position.getColumn());
        GridPosition frozenTarget = engine.board.findNearestFrozenZombieTileAhead(
            position.getRow(), position.getColumn());
        if (target == null && frozenTarget == null) {
            return;
        }
        double shortRange = plant.getEffectiveRange(
            plant.getDefinition().getAbilityParameter("rangeTiles", 3.0));
        if (plant.getAbility() == PlantAbility.SHORT_RANGE_SHROOM
            && target != null
            && target.getPosition().getColumn() - position.getColumn() > shortRange + 0.5) {
            return;
        }
        int projectileCount = plant.getProjectileCount();
        int maxHits = plant.getAbility() == PlantAbility.CACTUS
            ? plant.getDefinition().getAbilityParameterInt("maxHits", 3)
                + plant.getPierceBonus()
            : plant.isPiercing() ? Integer.MAX_VALUE : 1;
        for (int index = 0; index < projectileCount; index++) {
            double startColumn = position.getColumn() + 0.25 - index * 0.03;
            int poisonSeconds = plant.getDefinition().getAbilityParameterInt(
                "poisonSeconds", 5);
            double poisonFactor = plant.getDefinition().getAbilityParameter(
                "poisonDamageFactor", 0.25);
            int poisonDamage = Math.max(0, (int) Math.round(
                plant.getEffectiveAttackPower() * poisonFactor))
                + plant.getUpgradeTraitInt("POISON_TICK_BONUS", 0);
            Projectile projectile = new Projectile(plant.getEffectiveAttackPower(),
                Game.PROJECTILE_SPEED, new BoardPosition(position.getRow(), startColumn),
                plant.getProjectileElementType(), maxHits > 1,
                plant.getChillDurationTicks(), plant.isLobber(), plant.getName(), maxHits,
                poisonSeconds * Game.TICKS_PER_SECOND, poisonDamage);
            engine.board.addProjectile(projectile);
        }
        engine.addEvent("Plant " + plant.getName() + " fired " + projectileCount
            + " projectile(s) from " + position + ".");
    }
    static void attackHoming(Game engine, Plant plant) {
        Zombie target = engine.board.findNearestZombieAnywhere();
        if (target != null) {
            engine.damageZombieFromPlant(target, plant, Math.max(1, plant.getEffectiveAttackPower()), false);
            engine.addEvent("Plant " + plant.getName() + " hit " + target.getName() + ".");
        }
    }
    static void attackMelee(Game engine, Plant plant) {
        GridPosition position = plant.getPosition();
        int hits = 0;
        for (Zombie zombie : new ArrayList<>(engine.board.getZombies())) {
            if (zombie.isDead() || zombie.isHypnotized() || zombie.getPosition() == null) {
                continue;
            }
            int rowDistance = Math.abs(zombie.getPosition().getRow() - position.getRow());
            double columnDistance = Math.abs(zombie.getPosition().getColumn()
                - position.getColumn());
            int rowRadius = plant.getDefinition().getAbilityParameterInt("rowRadius",
                plant.getAbility() == PlantAbility.PHAT_BEET
                    || plant.getAbility() == PlantAbility.KIWIBEAST ? 1 : 0);
            double baseRange = plant.getDefinition().getAbilityParameter("rangeTiles",
                rowRadius > 0 ? 1.5 : 1.25);
            double range = plant.getEffectiveRange(baseRange);
            boolean inRange = rowDistance <= rowRadius && columnDistance <= range;
            if (inRange) {
                engine.damageZombieFromPlant(zombie, plant,
                    Math.max(1, plant.getEffectiveAttackPower()), false);
                hits++;
            }
        }
        if (hits > 0) {
            engine.addEvent("Plant " + plant.getName() + " struck " + hits + " zombie(s).");
        }
    }
    static void moveProjectilesAndResolveHits(Game engine) {
        Iterator<Projectile> iterator = engine.board.getProjectiles().isEmpty()
            ? Collections.<Projectile>emptyList().iterator()
            : new ArrayList<>(engine.board.getProjectiles()).iterator();
        while (iterator.hasNext()) {
            moveProjectileAndResolveHit(engine, iterator.next());
        }
    }

    private static void moveProjectileAndResolveHit(Game engine, Projectile projectile) {
        if (!projectile.isActive()) {
            engine.board.removeProjectile(projectile);
            return;
        }
        double previousColumn = projectile.moveOneTick();
        double currentColumn = projectile.getPosition().getColumn();
        projectile.igniteByTorchwood(engine.torchwoodMultiplier(
            projectile, previousColumn, currentColumn));
        if (projectileBlockedByTerrain(engine, projectile, previousColumn, currentColumn)) {
            engine.board.removeProjectile(projectile);
            return;
        }
        Zombie target = engine.findProjectileTarget(projectile.getPosition().getRow(),
            previousColumn, currentColumn);
        PushedObstacle obstacle = ZombieObjectSystem.firstProjectileObstacle(engine,
            projectile, previousColumn, currentColumn);
        if (obstacleComesFirst(target, obstacle)) {
            ZombieObjectSystem.hitObstacleWithProjectile(engine, obstacle, projectile);
            engine.board.removeProjectile(projectile);
            return;
        }
        if (target != null) {
            resolveProjectileTargetHit(engine, projectile, target);
        }
        if (!projectile.isActive() || currentColumn > engine.board.getCols() + 1) {
            engine.board.removeProjectile(projectile);
        }
    }

    private static boolean projectileBlockedByTerrain(Game engine, Projectile projectile,
                                                       double previousColumn,
                                                       double currentColumn) {
        if (projectile.isLobbed()) {
            return false;
        }
        return engine.hitIceTile(projectile, previousColumn, currentColumn)
            || engine.hitTomb(projectile, previousColumn, currentColumn);
    }

    private static boolean obstacleComesFirst(Zombie target, PushedObstacle obstacle) {
        return obstacle != null && (target == null || obstacle.getPosition().getColumn()
            <= target.getPosition().getColumn());
    }

    private static void resolveProjectileTargetHit(Game engine, Projectile projectile,
                                                   Zombie target) {
        if (engine.reflectProjectileIfNeeded(projectile, target)) {
            engine.board.removeProjectile(projectile);
            return;
        }
        int multiplier = projectile.getDamageMultiplier();
        boolean affected = projectile.hitTarget(target, multiplier, projectile.getImpactType());
        if (affected) {
            if (projectile.isPiercing()) {
                engine.piercingProjectileHits++;
            }
            engine.addEvent("Projectile from " + projectile.getSourcePlant() + " hit "
                + target.getName() + " for " + projectile.getDamage() * multiplier + " damage.");
        } else {
            engine.addEvent(target.getName() + " blocked or avoided the projectile.");
        }
    }

    static Zombie findProjectileTarget(Game engine, int row, double fromColumn, double toColumn) {
        Zombie target = null;
        double minColumn = Math.min(fromColumn, toColumn) - PROJECTILE_HIT_RADIUS;
        double maxColumn = Math.max(fromColumn, toColumn) + PROJECTILE_HIT_RADIUS;
        for (Zombie zombie : engine.board.getZombiesInRow(row)) {
            if (zombie.isHypnotized() || zombie.isTrappedInIceTile()) {
                continue;
            }
            double column = zombie.getPosition().getColumn();
            if (column < minColumn || column > maxColumn) {
                continue;
            }
            if (target == null || column < target.getPosition().getColumn()) {
                target = zombie;
            }
        }
        return target;
    }
    static void moveZombiesAndResolveCombat(Game engine) {
        for (Zombie zombie : new ArrayList<>(engine.board.getZombies())) {
            if (zombie.isDead() || zombie.isTrappedInIceTile()) {
                continue;
            }
            zombie.tickEffects();
            engine.updateZombieEnvironmentState(zombie);
            engine.performZombieSpecialAbility(zombie);
            if (zombie.isDead()) {
                continue;
            }
            if (zombie.isHypnotized()) {
                engine.moveHypnotizedZombie(zombie);
                continue;
            }
            Plant blockingPlant = engine.board.findBlockingPlant(zombie);
            ZombieAbilitySystem.updateSnorkelCombatState(zombie, blockingPlant != null);
            if (blockingPlant != null && engine.shouldZombieBypassPlant(zombie, blockingPlant)) {
                zombie.moveOneTick();
                continue;
            }
            if (blockingPlant != null) {
                engine.resolveZombiePlantCombat(zombie, blockingPlant);
            } else if (!engine.isStationaryZombie(zombie)) {
                zombie.moveOneTick();
                engine.applySlipperyTile(zombie);
            }
            if (zombie.getPosition() != null && zombie.getPosition().getColumn() < 0) {
                engine.handleZombieAtHouse(zombie);
            }
        }
    }
    static void handleZombieAtHouse(Game engine, Zombie crossingZombie) {
        int row = crossingZombie.getPosition().getRow();
        LawnMower mower = engine.board.getLawnMower(row);
        if (mower.trigger()) {
            ArrayList<String> killed = new ArrayList<>();
            for (Zombie zombie : new ArrayList<>(engine.board.getZombiesInRow(row))) {
                if (!zombie.isBoss()) {
                    zombie.kill();
                    killed.add(zombie.getName());
                    engine.lawnMowerKills++;
                }
            }
            engine.addEvent("The lawn mower in row " + (row + 1)
                + " was triggered and killed: " + String.join(", ", killed) + ".");
        } else {
            engine.gameState = GameState.LOST;
            engine.addEvent("The zombie ate your brain; LOSER!!!");
        }
    }
    static void cleanupDestroyedEntities(Game engine) {
        BattleCleanupSystem.cleanupDestroyedEntities(engine);
    }

    static void recordZombieKillStatistics(Game engine, Zombie zombie, BoardPosition position) {
        BattleCleanupSystem.recordZombieKillStatistics(engine, zombie, position);
    }

    static void handleZombieRewards(Game engine, Zombie zombie) {
        BattleCleanupSystem.handleZombieRewards(engine, zombie);
    }

    static void startNextWaveIfReady(Game engine) {
        if (!engine.zombieWavesStarted || engine.nextWaveIndex >= engine.currentLevel.getWaves().size()
            || engine.elapsedTicks < Game.INITIAL_PREPARATION_TICKS) {
            return;
        }
        if (engine.currentWave == null) {
            engine.startNextWave();
            return;
        }
        if (engine.currentWave.hasLostAtLeastSeventyFivePercentHealth()) {
            engine.startNextWave();
        }
    }
    static void evaluateGameState(Game engine) {
        if (engine.gameState != GameState.RUNNING) {
            return;
        }
        if (engine.specialLoseConditionReached()) {
            engine.gameState = GameState.LOST;
            engine.addEvent("The special level lose condition was reached: " + engine.specialStatus());
            return;
        }
        if (engine.specialWinConditionReached() || engine.checkWinCondition()) {
            engine.gameState = GameState.WON;
            engine.currentLevel.completeLevel();
            engine.addEvent("Dear humanz, zis is not done yet; we will come back to eat your brainz, humanz.");
        }
    }
    static boolean specialWinConditionReached(Game engine) {
        return engine.currentLevel.getRuleStrategy().hasSpecialWin(engine);
    }
    static boolean specialLoseConditionReached(Game engine) {
        return engine.currentLevel.getRuleStrategy().hasSpecialLoss(engine);
    }
    static int timedWarProgress(Game engine) {
        return engine.timedWarWindowProgress();
    }
}
