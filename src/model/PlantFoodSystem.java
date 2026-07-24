package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

final class PlantFoodSystem {
    private PlantFoodSystem() { }

    static String specialStatus(Game engine) {
        if (engine.currentLevel == null) {
            return "no level";
        }
        return engine.currentLevel.getRuleStrategy().status(engine);
    }
    static int protectedPlantsRemaining(Game engine) {
        int count = 0;
        for (GridPosition position : engine.endangeredPositions) {
            Plant plant = engine.board.getTile(position.getRow(), position.getColumn()).getMainPlant();
            if (plant != null && !plant.isDestroyed()) {
                count++;
            }
        }
        return count;
    }
    static void handleImmediatePlant(Game engine, Plant plant) {
        PlantAbility ability = plant.getAbility();
        if (ability == PlantAbility.GOLD_BLOOM) {
            int amount = Math.max(0, (int) Math.round(
                plant.getDefinition().getAbilityPower())) + plant.getSunProductionBonus();
            engine.sunAmount += amount;
            engine.removeInstantPlant(plant);
            engine.addEvent("Gold Bloom produced " + amount + " suns and disappeared.");
        } else if (ability.isMint()) {
            engine.activateMint(plant);
        } else if (ability == PlantAbility.ICE_SHROOM) {
            engine.freezeAllZombies(plant, false);
            if (plant.getEffectiveAttackPower() > 0) {
                for (Zombie zombie : engine.hostileZombies()) {
                    zombie.takeProjectileDamage(plant.getEffectiveAttackPower(),
                        ProjectileType.ICE, plant.getChillDurationTicks(), false,
                        plant.getName());
                }
            }
            engine.removeInstantPlant(plant);
        } else if (ability == PlantAbility.CHERRY_BOMB
            || ability == PlantAbility.GRAPESHOT
            || ability == PlantAbility.JALAPENO
            || ability == PlantAbility.DOOM_SHROOM) {
            engine.explosivePlantsUsed++;
            engine.detonatePlant(plant);
        }
    }
    static void detonatePlant(Game engine, Plant plant) {
        engine.detonatePlant(plant, 1);
    }
    static void detonatePlant(Game engine, Plant plant, int damageMultiplier) {
        GridPosition center = plant.getPosition();
        if (center == null) {
            return;
        }
        int baseDamage = plant.getDefinition().isInstantKill()
            ? Integer.MAX_VALUE / 4 : Math.max(1, plant.getEffectiveAttackPower());
        int damage = baseDamage * Math.max(1, damageMultiplier);
        PlantAbility ability = plant.getAbility();
        for (Zombie zombie : new ArrayList<>(engine.board.getZombies())) {
            if (zombie.isDead() || zombie.isHypnotized() || zombie.getPosition() == null) {
                continue;
            }
            int rowDistance = Math.abs(zombie.getPosition().getRow() - center.getRow());
            double columnDistance = Math.abs(zombie.getPosition().getColumn() - center.getColumn());
            if (engine.explosionHits(ability, rowDistance, columnDistance)) {
                if (ability == PlantAbility.JALAPENO) {
                    zombie.clearChill();
                }
                engine.damageZombieFromPlant(zombie, plant, damage, false);
            }
        }
        if (ability == PlantAbility.GRAPESHOT) {
            engine.launchGrapeshotFragments(plant, damageMultiplier);
        }
        if (ability == PlantAbility.DOOM_SHROOM) {
            engine.board.getTile(center.getRow(), center.getColumn()).setTileType(TileType.CRATER);
        }
        plant.takeDamage(Math.max(plant.getHealth(), 1));
        engine.addEvent("Plant " + plant.getName() + " activated at " + center + ".");
    }
    static void explodeRadioactiveSun(Game engine, Sun sun) {
        GridPosition center = sun.getPosition();
        for (Zombie zombie : new ArrayList<>(engine.board.getZombies())) {
            if (zombie.getPosition() == null) {
                continue;
            }
            int rowDistance = Math.abs(zombie.getPosition().getRow() - center.getRow());
            double columnDistance = Math.abs(zombie.getPosition().getColumn() - center.getColumn());
            if (rowDistance <= 2 && columnDistance <= 2.5) {
                zombie.takeDamage(150);
            }
        }
        for (Plant plant : new ArrayList<>(engine.board.getPlants())) {
            GridPosition position = plant.getPosition();
            if (Math.abs(position.getRow() - center.getRow()) <= 1
                && Math.abs(position.getColumn() - center.getColumn()) <= 1) {
                plant.takeDamage(80);
            }
        }
        sun.collect();
    }
    static void armMineWithPlantFood(Game engine, Plant plant) {
        engine.addEvent(plant.getName() + " armed immediately.");
        GridPosition center = plant.getPosition();
        int clones = 0;
        for (int row = Math.max(0, center.getRow() - 1);
             row <= Math.min(engine.board.getRows() - 1, center.getRow() + 1) && clones < 2; row++) {
            for (int col = Math.max(0, center.getColumn() - 1);
                 col <= Math.min(engine.board.getCols() - 1, center.getColumn() + 1) && clones < 2; col++) {
                if (row == center.getRow() && col == center.getColumn()) {
                    continue;
                }
                Tile tile = engine.board.getTile(row, col);
                if (tile.getPlant() == null && tile.getType().isPlantable()) {
                    Plant clone = engine.plantFactory.createPlant(plant.getName(), plant.getPlantLevel());
                    clone.usePlantFood();
                    engine.board.placePlant(clone, row, col);
                    clones++;
                }
            }
        }
        engine.addEvent(plant.getName() + " created " + clones + " armed clone(s).");
    }
    static void squashMultipleZombies(Game engine, Plant plant, int count) {
        ArrayList<Zombie> targets = engine.hostileZombies();
        int killed = 0;
        while (!targets.isEmpty() && killed < count) {
            Zombie target = targets.remove(engine.random.nextInt(targets.size()));
            target.kill(plant.getName());
            killed++;
        }
        plant.takeDamage(Math.max(plant.getHealth(), 1));
        engine.addEvent("Squash crushed " + killed + " zombie(s) with plant food.");
    }
    static void drownMultipleZombies(Game engine, Plant plant, int count) {
        ArrayList<Zombie> waterTargets = new ArrayList<>();
        for (Zombie zombie : engine.hostileZombies()) {
            int col = (int) Math.floor(zombie.getPosition().getColumn());
            if (engine.board.isInside(zombie.getPosition().getRow(), col)) {
                TileType type = engine.board.getTile(zombie.getPosition().getRow(), col).getType();
                if (type == TileType.WATER || type == TileType.LOW_TIDE) {
                    waterTargets.add(zombie);
                }
            }
        }
        int killed = 0;
        while (!waterTargets.isEmpty() && killed < count) {
            Zombie target = waterTargets.remove(engine.random.nextInt(waterTargets.size()));
            target.kill(plant.getName());
            killed++;
        }
        engine.addEvent("Tangle Kelp drowned " + killed + " zombie(s).");
    }
    static void redirectWholeLane(Game engine, Plant plant) {
        int row = plant.getPosition().getRow();
        int redirected = 0;
        for (Zombie zombie : new ArrayList<>(engine.board.getZombiesInRow(row))) {
            if (!zombie.isHypnotized()) {
                engine.moveZombieToAdjacentLane(zombie);
                redirected++;
            }
        }
        engine.addEvent("Garlic redirected " + redirected + " zombie(s) from its lane.");
    }
    static void magnetizeAllZombies(Game engine, Plant plant) {
        int removed = 0;
        for (Zombie zombie : engine.hostileZombies()) {
            removed += zombie.removeMetalArmor();
        }
        engine.addEvent("Magnet-shroom removed " + removed + " total metal armor health.");
    }
    static void cloneLilyPads(Game engine, Plant plant) {
        int created = 0;
        for (int row = 0; row < engine.board.getRows() && created < 3; row++) {
            for (int col = 0; col < engine.board.getCols() && created < 3; col++) {
                Tile tile = engine.board.getTile(row, col);
                boolean water = tile.getType() == TileType.WATER
                    || tile.getType() == TileType.LOW_TIDE;
                if (water && tile.getSupportPlant() == null && tile.getMainPlant() == null) {
                    Plant clone = engine.plantFactory.createPlant("Lily Pad", plant.getPlantLevel());
                    engine.board.placePlant(clone, row, col);
                    created++;
                }
            }
        }
        engine.addEvent("Lily Pad created " + created + " copy/copies.");
    }
    static void resetShortRangeShrooms(Game engine) {
        int reset = 0;
        for (Plant plant : engine.board.getPlants()) {
            if (plant.getAbility() == PlantAbility.SHORT_RANGE_SHROOM) {
                plant.restoreLifetime();
                reset++;
            }
        }
        engine.addEvent("Plant food reset the lifetime of " + reset + " short-range shroom(s).");
    }
    static void hypnotizeRandomZombies(Game engine, int count) {
        ArrayList<Zombie> targets = engine.hostileZombies();
        int affected = 0;
        while (!targets.isEmpty() && affected < count) {
            Zombie target = targets.remove(engine.random.nextInt(targets.size()));
            target.hypnotize();
            affected++;
        }
        engine.addEvent("Plant food hypnotized " + affected + " zombie(s).");
    }
    static void killRandomZombies(Game engine, int count, String sourceName) {
        ArrayList<Zombie> targets = engine.hostileZombies();
        int killed = 0;
        while (!targets.isEmpty() && killed < count) {
            Zombie target = targets.remove(engine.random.nextInt(targets.size()));
            target.kill(sourceName);
            killed++;
        }
        engine.addEvent(sourceName + " eliminated " + killed + " zombie(s).");
    }
    static void clearPlantLane(Game engine, Plant plant) {
        int row = plant.getPosition().getRow();
        int killed = 0;
        for (Zombie zombie : new ArrayList<>(engine.board.getZombiesInRow(row))) {
            if (!zombie.isHypnotized()) {
                zombie.kill(plant.getName());
                killed++;
            }
        }
        engine.addEvent("Citron's plasma ball cleared " + killed + " zombie(s) from its lane.");
    }
    static void fumePlantFoodPush(Game engine, Plant plant) {
        int row = plant.getPosition().getRow();
        int pushed = 0;
        for (Zombie zombie : new ArrayList<>(engine.board.getZombiesInRow(row))) {
            if (!zombie.isHypnotized()) {
                zombie.takeDamage(Math.max(1, plant.getEffectiveAttackPower()) * 5, plant.getName());
                zombie.setPosition(zombie.getPosition().moveHorizontal(1.5));
                pushed++;
            }
        }
        engine.addEvent("Fume-shroom pushed " + pushed + " zombie(s) backward.");
    }
    static void ensureConveyorCardAvailable(Game engine, PlantDefinition definition, boolean conveyor) {
        if (conveyor && engine.conveyorCards.getOrDefault(definition.getName(), 0) <= 0) {
            throw new IllegalStateException("No conveyor card is available for this plant.");
        }
    }
    static Plant createPlantForPlacement(Game engine, PlantDefinition definition, int level) {
        if (PlantAbility.fromDefinition(definition) != PlantAbility.IMITATER) {
            return engine.plantFactory.createPlant(definition.getName(), level);
        }
        List<String> selectedPlants = new ArrayList<>(engine.selectedPlants);
        Collections.reverse(selectedPlants);
        for (String selected : selectedPlants) {
            PlantDefinition candidate = engine.plantFactory.findDefinition(selected).orElse(null);
            if (candidate != null && PlantAbility.fromDefinition(candidate) != PlantAbility.IMITATER) {
                Plant copy = engine.plantFactory.createPlant(candidate.getName(),
                    engine.plantLevels.getOrDefault(candidate.getNormalizedName(), 1));
                copy.applyImitaterCardModifiers(definition, level);
                engine.addEvent("Imitater copied " + candidate.getName()
                    + " with its own card upgrades.");
                return copy;
            }
        }
        throw new IllegalStateException("Imitater needs another selected plant to copy.");
    }
    static void finishPlantPurchase(Game engine, String cooldownKey, String selectedPlantName,
                                         Plant plant, boolean conveyor) {
        if (conveyor) {
            int cards = engine.conveyorCards.getOrDefault(selectedPlantName, 0);
            engine.conveyorCards.put(selectedPlantName, Math.max(0, cards - 1));
            return;
        }
        engine.sunAmount -= plant.getSunCost();
        engine.cooldownTicks.put(cooldownKey, engine.isPreWaveSetup() ? 0 : plant.getRechargeTicks());
    }
    static boolean handleTerrainUtilityPlant(Game engine, Plant plant, int row, int col) {
        GridPosition position = new GridPosition(row, col);
        Tile tile = engine.board.getTile(row, col);
        if (plant.getAbility() == PlantAbility.HOT_POTATO) {
            if (tile.getType() != TileType.ICE) {
                throw new IllegalStateException("Hot Potato can only be used on ice.");
            }
            int radius = plant.hasUpgradeTrait("MELT_AREA_3X3") ? 1 : 0;
            int melted = meltIceArea(engine, row, col, radius);
            explodeUtilityPlantIfUpgraded(engine, plant, position);
            engine.addEvent("Hot Potato melted " + melted + " ice tile(s) around "
                + position + ".");
            return true;
        }
        if (plant.getAbility() == PlantAbility.GRAVE_BUSTER) {
            Tomb tomb = engine.tombs.remove(position);
            if (tomb == null) {
                throw new IllegalStateException("There is no tomb on this tile.");
            }
            tile.setTileType(TileType.NORMAL);
            explodeUtilityPlantIfUpgraded(engine, plant, position);
            engine.addEvent("Grave Buster removed the tomb at " + position + ".");
            return true;
        }
        return false;
    }
    private static int meltIceArea(Game engine, int centerRow, int centerCol, int radius) {
        int melted = 0;
        for (int row = Math.max(0, centerRow - radius);
             row <= Math.min(engine.board.getRows() - 1, centerRow + radius); row++) {
            for (int col = Math.max(0, centerCol - radius);
                 col <= Math.min(engine.board.getCols() - 1, centerCol + radius); col++) {
                Tile current = engine.board.getTile(row, col);
                if (current.getType() == TileType.ICE) {
                    current.setTileType(TileType.NORMAL);
                    melted++;
                }
            }
        }
        return melted;
    }

    private static void explodeUtilityPlantIfUpgraded(Game engine, Plant plant,
                                                       GridPosition center) {
        if (!plant.hasUpgradeTrait("EXPLODE_ON_FINISH")) {
            return;
        }
        int damage = plant.getDefinition().getAbilityParameterInt(
            "finishExplosionDamage", 500);
        int hits = 0;
        for (Zombie zombie : engine.hostileZombies()) {
            if (Math.abs(zombie.getPosition().getRow() - center.getRow()) <= 1
                && Math.abs(zombie.getPosition().getColumn() - center.getColumn()) <= 1.5) {
                zombie.takeProjectileDamage(damage, ProjectileType.FIRE, 0, false,
                    plant.getName());
                hits++;
            }
        }
        engine.addEvent(plant.getName() + " exploded on finish and hit " + hits
            + " zombie(s).");
    }

    static void validateSpecialPlantLocation(Game engine, Plant plant, int row, int col) {
        Tile tile = engine.board.getTile(row, col);
        if (plant.getAbility() == PlantAbility.TANGLE_KELP
            && tile.getType() != TileType.WATER && tile.getType() != TileType.LOW_TIDE) {
            throw new IllegalStateException("Tangle Kelp can only be planted in water.");
        }
    }
    static void warmAdjacentIce(Game engine, Plant plant) {
        if (!plant.getDefinition().hasTag("Fire") || engine.elapsedTicks % Game.TICKS_PER_SECOND != 0) {
            return;
        }
        GridPosition center = plant.getPosition();
        int radius = plant.getWarmthRadius();
        for (Plant other : engine.board.getPlants()) {
            if (other == plant || other.getPosition() == null) {
                continue;
            }
            GridPosition position = other.getPosition();
            if (Math.abs(position.getRow() - center.getRow()) <= radius
                && Math.abs(position.getColumn() - center.getColumn()) <= radius) {
                other.damageIce(60, false);
            }
        }
        for (int row = Math.max(0, center.getRow() - radius);
             row <= Math.min(engine.board.getRows() - 1, center.getRow() + radius); row++) {
            for (int col = Math.max(0, center.getColumn() - radius);
                 col <= Math.min(engine.board.getCols() - 1, center.getColumn() + radius); col++) {
                Tile tile = engine.board.getTile(row, col);
                if (tile.getType() == TileType.ICE) {
                    tile.damageIce(60, false);
                }
            }
        }
    }
}
