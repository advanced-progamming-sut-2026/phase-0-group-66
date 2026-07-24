package model;

import java.util.ArrayList;

final class PlantFoodSupportSystem {
    private PlantFoodSupportSystem() { }

    static void produceSun(Game game, Plant plant) {
        if (plant.getAbility() == PlantAbility.SUN_SHROOM) {
            plant.matureFully();
        }
        int amount = plant.getDefinition().getPlantFoodParameterInt("sun",
            (int) Math.round(plant.getDefinition().getPlantFoodPower()));
        amount += plant.getSunProductionBonus();
        game.sunAmount += Math.max(0, amount);
        game.addEvent(plant.getName() + " produced " + amount + " sun immediately.");
    }

    static void armAndClone(Game game, Plant plant) {
        int clones = plant.getDefinition().getPlantFoodParameterInt("clones", 2);
        GridPosition center = plant.getPosition();
        int created = 0;
        for (int radius = 1; radius < game.board.getRows() + game.board.getCols()
             && created < clones; radius++) {
            created += cloneInRadius(game, plant, center, radius, clones - created);
        }
        game.addEvent(plant.getName() + " armed and created " + created + " armed clone(s).");
    }

    private static int cloneInRadius(Game game, Plant source, GridPosition center,
                                     int radius, int remaining) {
        int created = 0;
        for (int row = 0; row < game.board.getRows() && created < remaining; row++) {
            for (int col = 0; col < game.board.getCols() && created < remaining; col++) {
                if (Math.abs(row - center.getRow()) + Math.abs(col - center.getColumn()) != radius) {
                    continue;
                }
                Tile tile = game.board.getTile(row, col);
                if (tile.getPlant() == null && tile.getType().isPlantable()) {
                    Plant clone = game.plantFactory.createPlant(
                        source.getName(), source.getPlantLevel());
                    clone.applyDifficultyTiming(game.difficultyLevel);
                    clone.usePlantFood();
                    game.board.placePlant(clone, row, col);
                    created++;
                }
            }
        }
        return created;
    }

    static void drownTargets(Game game, Plant plant) {
        ArrayList<Zombie> targets = new ArrayList<>();
        for (Zombie zombie : game.hostileZombies()) {
            int col = (int) Math.floor(zombie.getPosition().getColumn());
            if (!game.board.isInside(zombie.getPosition().getRow(), col)) {
                continue;
            }
            TileType tileType = game.board.getTile(zombie.getPosition().getRow(), col).getType();
            if (tileType == TileType.WATER || tileType == TileType.LOW_TIDE) {
                targets.add(zombie);
            }
        }
        int count = plant.getDefinition().getPlantFoodParameterInt("targets", 3);
        int killed = killRandom(game, plant, targets, count);
        game.addEvent(plant.getName() + " drowned " + killed + " water zombie(s).");
    }

    private static int killRandom(Game game, Plant plant, ArrayList<Zombie> targets, int count) {
        int killed = 0;
        while (!targets.isEmpty() && killed < count) {
            Zombie target = targets.remove(game.random.nextInt(targets.size()));
            target.kill(plant.getName());
            killed++;
        }
        return killed;
    }

    static void freezeMap(Game game, Plant plant) {
        int freeze = plant.getDefinition().getPlantFoodParameterInt("freezeSeconds", 5)
            * Game.TICKS_PER_SECOND + plant.getChillBonusTicks();
        int chill = plant.getDefinition().getPlantFoodParameterInt("chillSeconds", 10)
            * Game.TICKS_PER_SECOND;
        int affected = 0;
        for (Zombie zombie : game.hostileZombies()) {
            zombie.stun(freeze);
            zombie.chill(chill);
            affected++;
        }
        game.addEvent(plant.getName() + " froze " + affected + " zombie(s) on the map.");
    }

    static void reinforce(Game game, Plant plant) {
        int shield = plant.getDefinition().getPlantFoodParameterInt("shield",
            (int) Math.round(plant.getDefinition().getPlantFoodPower()));
        plant.addPlantFoodShield(shield);
        game.addEvent(plant.getName() + " gained a permanent " + shield + " point armor layer.");
    }

    static void reinforceEndurian(Game game, Plant plant) {
        reinforce(game, plant);
        int multiplier = plant.getDefinition().getPlantFoodParameterInt("reflectMultiplier", 2);
        plant.setReflectDamageMultiplier(multiplier);
        game.addEvent("Endurian reflection damage multiplier is now " + multiplier + ".");
    }

    static void redirectLane(Game game, Plant plant) {
        game.redirectWholeLane(plant);
    }

    static void pullToDefender(Game game, Plant plant) {
        plant.healToFull();
        double radius = plant.getDefinition().getPlantFoodParameter(
            "pullRadiusTiles", 4.0);
        PlantAttackSystem.pullZombiesTowardSweetPotato(game, plant, radius);
        game.addEvent(plant.getName() + " pulled nearby zombies and fully healed.");
    }

    static void explodingReinforce(Game game, Plant plant) {
        reinforce(game, plant);
        int damage = plant.getDefinition().getPlantFoodParameterInt("explosionDamage", 1800);
        plant.armExplosiveShield(damage);
        game.addEvent(plant.getName() + " will explode for " + damage
            + " damage when its plant-food armor breaks.");
    }

    static void blueFlame(Game game, Plant plant) {
        int multiplier = plant.getDefinition().getPlantFoodParameterInt("peaDamageMultiplier", 3);
        plant.igniteBlueFlame(multiplier);
        game.addEvent("Torchwood ignited a permanent blue " + multiplier + "x flame.");
    }

    static void removeArmor(Game game, Plant plant) {
        game.magnetizeAllZombies(plant);
    }

    static void prepareHypnoGargantuar(Game game, Plant plant) {
        plant.enableHypnoGargantuar();
        game.addEvent("Hypno-shroom will turn its eater into an allied Gargantuar.");
    }

    static void cloneSupports(Game game, Plant plant) {
        int target = plant.getDefinition().getPlantFoodParameterInt("clones", 3);
        int created = 0;
        for (int row = 0; row < game.board.getRows() && created < target; row++) {
            for (int col = 0; col < game.board.getCols() && created < target; col++) {
                Tile tile = game.board.getTile(row, col);
                boolean water = tile.getType() == TileType.WATER
                    || tile.getType() == TileType.LOW_TIDE;
                if (water && tile.getSupportPlant() == null && tile.getMainPlant() == null) {
                    Plant clone = game.plantFactory.createPlant(
                        "Lily Pad", plant.getPlantLevel());
                    clone.applyDifficultyTiming(game.difficultyLevel);
                    game.board.placePlant(clone, row, col);
                    created++;
                }
            }
        }
        game.addEvent("Lily Pad created " + created + " copy/copies.");
    }
}
