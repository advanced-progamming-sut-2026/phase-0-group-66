package model;

import java.util.ArrayList;

final class BattleCleanupSystem {
    private BattleCleanupSystem() {
    }

    static void cleanupDestroyedEntities(Game engine) {
        cleanupPlants(engine);
        engine.removeUnsupportedWaterPlants();
        cleanupZombies(engine);
    }

    private static void cleanupPlants(Game engine) {
        for (Plant plant : new ArrayList<>(engine.board.getPlants())) {
            if (!plant.isDestroyed() || plant.getPosition() == null) {
                continue;
            }
            GridPosition position = plant.getPosition();
            triggerPlantDeathEffects(engine, plant);
            boolean endangered = engine.endangeredPositions.contains(position)
                && engine.board.getTile(position.getRow(), position.getColumn()).getMainPlant() == plant;
            engine.board.removePlant(plant);
            engine.waitingSunProducers.remove(position);
            if (endangered) {
                engine.board.setEndangeredPlantsEaten(true);
            }
            engine.lostPlantsCount++;
            engine.addEvent("Plant " + plant.getName() + " at " + position + " is destroyed.");
        }
    }

    private static void triggerPlantDeathEffects(Game engine, Plant plant) {
        if (plant.getAbility() == PlantAbility.EXPLODE_O_NUT) {
            engine.explodeDestroyedDefender(plant);
        }
        if (plant.getAbility() == PlantAbility.TORCHWOOD
            && plant.hasUpgradeTrait("AOE_ON_DEATH")) {
            explodeTorchwoodOnDeath(engine, plant);
        }
    }

    private static void cleanupZombies(Game engine) {
        for (Zombie zombie : new ArrayList<>(engine.board.getZombies())) {
            if (!zombie.isDead()) {
                continue;
            }
            BoardPosition position = zombie.getPosition();
            engine.releaseWizardTransformations(zombie.getRuntimeId());
            engine.dropStolenSunFromZombie(zombie);
            handleZombieRewards(engine, zombie);
            recordZombieKillStatistics(engine, zombie, position);
            engine.board.removeZombie(zombie);
            engine.zombieKillCount++;
            engine.addEvent("Zombie of type " + zombie.getName() + " is dead at " + position + ".");
        }
    }

    private static void explodeTorchwoodOnDeath(Game engine, Plant plant) {
        int damage = plant.getDefinition().getAbilityParameterInt("deathExplosionDamage", 500);
        GridPosition center = plant.getPosition();
        int hits = 0;
        for (Zombie zombie : engine.hostileZombies()) {
            if (Math.abs(zombie.getPosition().getRow() - center.getRow()) <= 1
                && Math.abs(zombie.getPosition().getColumn() - center.getColumn()) <= 1.5) {
                zombie.takeProjectileDamage(damage, ProjectileType.FIRE, 0, false,
                    plant.getName());
                hits++;
            }
        }
        engine.addEvent("Torchwood's level upgrade exploded on death and hit "
            + hits + " zombie(s).");
    }

    static void recordZombieKillStatistics(Game engine, Zombie zombie, BoardPosition position) {
        updateMultiKillStatistics(engine);
        String sourcePlant = zombie.getLastDamageSourcePlant();
        if (sourcePlant != null && !sourcePlant.isBlank()) {
            engine.plantKillCounts.merge(PlantDefinition.normalizeKey(sourcePlant), 1, Integer::sum);
        }
        if (engine.elapsedTicks <= 30 * Game.TICKS_PER_SECOND) {
            engine.killsWithinThirtySeconds++;
        }
        if (isFirstColumnKillWithoutMower(engine, position)) {
            engine.firstColumnNoMowerKills++;
        }
    }

    private static void updateMultiKillStatistics(Game engine) {
        if (engine.lastKillTick == engine.elapsedTicks) {
            engine.killsAtLastKillTick++;
            engine.multiKillZombieCount++;
        } else {
            engine.lastKillTick = engine.elapsedTicks;
            engine.killsAtLastKillTick = 1;
        }
    }

    private static boolean isFirstColumnKillWithoutMower(Game engine, BoardPosition position) {
        if (position == null || position.getColumn() < 0 || position.getColumn() >= 1.0) {
            return false;
        }
        int row = position.getRow();
        return row >= 0 && row < engine.board.getRows()
            && !engine.board.getLawnMower(row).isActivated();
    }

    static void handleZombieRewards(Game engine, Zombie zombie) {
        if (zombie.isRewardDropped()) {
            return;
        }
        zombie.dropReward();
        dropPlantFood(engine, zombie);
        if (engine.random.nextInt(100) < 10) {
            dropRandomLoot(engine);
        }
    }

    private static void dropPlantFood(Game engine, Zombie zombie) {
        if (!zombie.isGlowing()) {
            return;
        }
        if (engine.inventory.getPlantFoodCapacityLeft() > 0) {
            engine.inventory.addPlantFood(1);
            engine.addEvent("The glowing zombie dropped a plant food; you have "
                + engine.inventory.getPlantFoods() + " plant foods now.");
        } else {
            engine.addEvent("The glowing zombie dropped plant food, but storage is full.");
        }
    }

    private static void dropRandomLoot(Game engine) {
        int rewardType = engine.random.nextInt(3);
        if (rewardType == 0) {
            engine.wallet.addCoins(50);
            engine.addEvent("A zombie dropped 50 coins; you have " + engine.wallet.getCoins()
                + " coins now.");
        } else if (rewardType == 1) {
            engine.wallet.addGems(1);
            engine.addEvent("A zombie dropped a diamond; you have " + engine.wallet.getGems()
                + " diamonds now.");
        } else {
            engine.inventory.addPot();
            engine.addEvent("A zombie dropped a pot; you have " + engine.inventory.getPots()
                + " pots now.");
        }
    }
}
