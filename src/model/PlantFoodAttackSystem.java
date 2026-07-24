package model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;

final class PlantFoodAttackSystem {
    private PlantFoodAttackSystem() { }

    static void shooterVolley(Game game, Plant plant) {
        int shots = parameter(plant, "shots", 10);
        int hits = damageLaneSequentially(game, plant, plant.getPosition().getRow(),
            Math.max(1, plant.getEffectiveAttackPower()), shots, plant.getProjectileElementType());
        game.addEvent(plant.getName() + " fired a rapid " + shots + " shot volley with "
            + hits + " successful hit(s).");
    }

    static void repeaterVolley(Game game, Plant plant) {
        int shots = parameter(plant, "volleyShots", 10);
        int damage = Math.max(1, plant.getEffectiveAttackPower());
        int hits = damageLaneSequentially(game, plant, plant.getPosition().getRow(), damage,
            shots, ProjectileType.NORMAL);
        Zombie target = nearestAhead(game, plant, plant.getPosition().getRow());
        if (target != null) {
            int multiplier = parameter(plant, "giantMultiplier", 20);
            target.takeProjectileDamage(damage * multiplier, ProjectileType.NORMAL,
                0, false, plant.getName());
            hits++;
        }
        game.addEvent("Repeater fired its heavy volley and giant pea; hits=" + hits + ".");
    }

    static void threepeaterFan(Game game, Plant plant) {
        int volleys = parameter(plant, "volleys", 5);
        int hits = 0;
        for (int row = 0; row < game.board.getRows(); row++) {
            hits += damageLaneSequentially(game, plant, row,
                Math.max(1, plant.getEffectiveAttackPower()), volleys, ProjectileType.NORMAL);
        }
        game.addEvent("Threepeater swept all lanes with " + hits + " hit(s).");
    }

    static void snowPeaLaneFreeze(Game game, Plant plant) {
        int row = plant.getPosition().getRow();
        int freeze = parameter(plant, "freezeSeconds", 5) * Game.TICKS_PER_SECOND;
        int chill = parameter(plant, "chillSeconds", 10) * Game.TICKS_PER_SECOND
            + plant.getChillBonusTicks();
        for (Zombie zombie : hostileInRow(game, row)) {
            zombie.stun(freeze);
            zombie.chill(chill);
        }
        int volleys = parameter(plant, "volleys", 8);
        int hits = damageLaneSequentially(game, plant, row,
            Math.max(1, plant.getEffectiveAttackPower()), volleys, ProjectileType.ICE);
        game.addEvent("Snow Pea froze its lane and landed " + hits + " icy hit(s).");
    }

    static void rotobagaVolley(Game game, Plant plant) {
        int volleys = parameter(plant, "volleys", 5);
        int hits = 0;
        for (int index = 0; index < volleys; index++) {
            hits += PlantAttackSystem.fireRotobagaVolley(game, plant);
        }
        game.addEvent("Rotobaga fired diagonal plant-food volleys with " + hits + " hit(s).");
    }

    static void peaPodGiants(Game game, Plant plant) {
        int multiplier = parameter(plant, "giantMultiplier", 20);
        int heads = plant.getStackCount();
        int hits = damageLaneSequentially(game, plant, plant.getPosition().getRow(),
            Math.max(1, plant.getEffectiveAttackPower()) * multiplier, heads,
            ProjectileType.NORMAL);
        game.addEvent("Pea Pod fired " + heads + " giant pea(s), hitting " + hits + " time(s).");
    }

    static void splitPeaVolley(Game game, Plant plant) {
        int volleys = parameter(plant, "volleys", 8);
        GridPosition position = plant.getPosition();
        int hits = 0;
        for (int index = 0; index < volleys; index++) {
            Zombie ahead = game.board.findNearestZombieAhead(position.getRow(), position.getColumn());
            Zombie behind = game.board.findNearestZombieBehind(position.getRow(), position.getColumn());
            hits += hit(game, plant, ahead, plant.getEffectiveAttackPower(), ProjectileType.NORMAL);
            hits += hit(game, plant, behind, plant.getEffectiveAttackPower() * 2,
                ProjectileType.NORMAL);
        }
        game.addEvent("Split Pea fired simultaneous front/back volleys with " + hits + " hit(s).");
    }

    static void clearLane(Game game, Plant plant) {
        game.clearPlantLane(plant);
    }

    static void hypnotizeRandom(Game game, Plant plant) {
        int count = parameter(plant, "targets", 3);
        game.hypnotizeRandomZombies(count);
    }

    static void eliminateRandom(Game game, Plant plant) {
        int count = parameter(plant, "targets", 3);
        game.killRandomZombies(count, plant.getName());
    }

    static void explosiveBulbs(Game game, Plant plant) {
        int count = parameter(plant, "bulbs", 3);
        int multiplier = parameter(plant, "damageMultiplier", 5);
        ArrayList<Zombie> targets = game.hostileZombies();
        int hits = 0;
        for (int index = 0; index < count && !targets.isEmpty(); index++) {
            Zombie target = targets.remove(game.random.nextInt(targets.size()));
            int damage = Math.max(1, plant.getEffectiveAttackPower()) * multiplier;
            double splashFactor = plant.getDefinition().getPlantFoodParameter(
                "splashDamageFactor", 0.5);
            damageArea(game, plant, target, damage, splashFactor,
                ProjectileType.NORMAL, false);
            hits++;
        }
        game.addEvent("Bowling Bulb launched " + hits + " explosive bouncing bulb(s).");
    }

    static void cactusPierce(Game game, Plant plant) {
        int multiplier = parameter(plant, "damageMultiplier", 10);
        int damage = Math.max(1, plant.getEffectiveAttackPower()) * multiplier;
        int hits = 0;
        for (Zombie zombie : hostileInRow(game, plant.getPosition().getRow())) {
            hits += hit(game, plant, zombie, damage, ProjectileType.NORMAL);
        }
        game.addEvent("Cactus fired an electric unlimited-piercing spike through " + hits
            + " zombie(s).");
    }

    static void fireLaneVolley(Game game, Plant plant) {
        int volleys = parameter(plant, "volleys", 10);
        int damage = Math.max(1, plant.getEffectiveAttackPower()) * volleys;
        int hits = 0;
        for (Zombie zombie : hostileInRow(game, plant.getPosition().getRow())) {
            zombie.clearChill();
            hits += hit(game, plant, zombie, damage, ProjectileType.FIRE);
        }
        game.addEvent("Fire Peashooter burned its whole lane; targets=" + hits + ".");
    }

    static void starfruitVolley(Game game, Plant plant) {
        int volleys = parameter(plant, "volleys", 5);
        int hits = 0;
        for (int index = 0; index < volleys; index++) {
            hits += PlantAttackSystem.fireStarfruitVolley(game, plant);
        }
        game.addEvent("Starfruit fired omni-directional volleys with " + hits + " hit(s).");
    }

    static void poisonVolley(Game game, Plant plant) {
        int volleys = parameter(plant, "volleys", 10);
        int seconds = parameter(plant, "poisonSeconds", 10);
        double factor = plant.getDefinition().getPlantFoodParameter("poisonDamageFactor", 0.5);
        int damage = Math.max(1, plant.getEffectiveAttackPower()) * volleys;
        int hits = 0;
        for (Zombie zombie : hostileInRow(game, plant.getPosition().getRow())) {
            zombie.takeDirectDamage(damage, plant.getName());
            zombie.poison(seconds * Game.TICKS_PER_SECOND,
                Math.max(1, (int) Math.round(damage * factor))
                    + plant.getUpgradeTraitInt("POISON_TICK_BONUS", 0),
                plant.getName());
            hits++;
        }
        game.addEvent("Goo Peashooter poisoned " + hits + " zombie(s) with its barrage.");
    }

    static void gatlingVolley(Game game, Plant plant) {
        int shots = parameter(plant, "volleyShots", 24);
        int damage = Math.max(1, plant.getEffectiveAttackPower());
        int hits = damageLaneSequentially(game, plant, plant.getPosition().getRow(), damage,
            shots, ProjectileType.NORMAL);
        int giantShots = parameter(plant, "giantShots", 4);
        int giantMultiplier = parameter(plant, "giantMultiplier", 20);
        hits += damageLaneSequentially(game, plant, plant.getPosition().getRow(),
            damage * giantMultiplier, giantShots, ProjectileType.NORMAL);
        game.addEvent("Mega Gatling Pea completed its massive barrage with " + hits + " hit(s).");
    }

    static void shroomVolley(Game game, Plant plant) {
        int volleys = parameter(plant, "volleys", 8);
        int hits = damageLaneSequentially(game, plant, plant.getPosition().getRow(),
            Math.max(1, plant.getEffectiveAttackPower()), volleys, ProjectileType.NORMAL);
        int reset = 0;
        String key = plant.getDefinition().getNormalizedName();
        for (Plant current : game.board.getPlants()) {
            if (current.getDefinition().getNormalizedName().equals(key)) {
                current.restoreLifetime();
                reset++;
            }
        }
        game.addEvent(plant.getName() + " fired a barrage with " + hits + " hit(s) and reset "
            + reset + " matching shroom lifetime(s).");
    }

    static void knockbackBlast(Game game, Plant plant) {
        int multiplier = parameter(plant, "damageMultiplier", 5);
        double knockback = plant.getDefinition().getPlantFoodParameter("knockbackTiles", 1.5);
        int hits = 0;
        for (Zombie zombie : hostileInRow(game, plant.getPosition().getRow())) {
            hit(game, plant, zombie, Math.max(1, plant.getEffectiveAttackPower()) * multiplier,
                ProjectileType.NORMAL);
            zombie.setPosition(zombie.getPosition().moveHorizontal(knockback));
            hits++;
        }
        game.addEvent("Fume-shroom pushed back " + hits + " zombie(s).");
    }

    static void lobberBarrage(Game game, Plant plant) {
        int count = parameter(plant, "targets", 5);
        int multiplier = parameter(plant, "damageMultiplier", 2);
        int hits = randomDirectHits(game, plant, count,
            Math.max(1, plant.getEffectiveAttackPower()) * multiplier, ProjectileType.NORMAL);
        game.addEvent(plant.getName() + " lobbed plant-food shots at " + hits + " target(s).");
    }

    static void butterAll(Game game, Plant plant) {
        int stunSeconds = parameter(plant, "stunSeconds", 5);
        int hits = 0;
        for (Zombie zombie : game.hostileZombies()) {
            zombie.takeProjectileDamage(Math.max(40, plant.getEffectiveAttackPower()),
                ProjectileType.NORMAL, 0, true, plant.getName());
            zombie.stun(stunSeconds * Game.TICKS_PER_SECOND);
            hits++;
        }
        game.addEvent("Kernel-pult buttered all " + hits + " zombie(s).");
    }

    static void melonBarrage(Game game, Plant plant, boolean icy, boolean fiery) {
        int count = parameter(plant, "targets", 3);
        int multiplier = parameter(plant, "damageMultiplier", 3);
        ProjectileType type = icy ? ProjectileType.ICE : fiery ? ProjectileType.FIRE
            : ProjectileType.NORMAL;
        ArrayList<Zombie> targets = game.hostileZombies();
        int hits = 0;
        double splashFactor = plant.getDefinition().getPlantFoodParameter(
            "splashDamageFactor", 1.0);
        for (int index = 0; index < count && !targets.isEmpty(); index++) {
            Zombie target = targets.remove(game.random.nextInt(targets.size()));
            int damage = Math.max(1, plant.getEffectiveAttackPower()) * multiplier;
            damageArea(game, plant, target, damage, splashFactor, type, true);
            hits++;
        }
        game.addEvent(plant.getName() + " completed a giant lobber barrage on " + hits
            + " target(s).");
    }

    static void multiSmash(Game game, Plant plant) {
        game.squashMultipleZombies(plant, parameter(plant, "targets", 2));
    }

    static void bonkArea(Game game, Plant plant) {
        int repetitions = parameter(plant, "repetitions", 10);
        int damage = Math.max(1, plant.getEffectiveAttackPower()) * repetitions;
        int hits = damageNearby(game, plant, damage, ProjectileType.NORMAL,
            parameter(plant, "rowRadius", 1),
            plant.getDefinition().getPlantFoodParameter("columnRadius", 1.5));
        game.addEvent("Bonk Choy rapidly punched " + hits + " nearby zombie(s).");
    }

    static void phatBeetWave(Game game, Plant plant) {
        int multiplier = parameter(plant, "damageMultiplier", 10);
        int hits = damageNearby(game, plant,
            Math.max(1, plant.getEffectiveAttackPower()) * multiplier,
            ProjectileType.NORMAL, parameter(plant, "rowRadius", 1),
            plant.getDefinition().getPlantFoodParameter("columnRadius", 1.5));
        game.addEvent("Phat Beet hit " + hits + " nearby zombie(s) with a powerful shockwave.");
    }

    static void chomperSwallow(Game game, Plant plant) {
        int count = parameter(plant, "targets", 3);
        ArrayList<Zombie> targets = game.hostileZombies();
        targets.sort(Comparator.comparingDouble(zombie -> distance(plant, zombie)));
        int swallowed = 0;
        while (!targets.isEmpty() && swallowed < count) {
            targets.remove(0).kill(plant.getName());
            swallowed++;
        }
        game.addEvent("Chomper swallowed " + swallowed + " zombie(s) at range.");
    }

    static void wasabiSpin(Game game, Plant plant) {
        int multiplier = parameter(plant, "damageMultiplier", 8);
        int hits = damageNearby(game, plant,
            Math.max(1, plant.getEffectiveAttackPower()) * multiplier,
            ProjectileType.FIRE, parameter(plant, "rowRadius", 1),
            plant.getDefinition().getPlantFoodParameter("columnRadius", 1.5));
        game.addEvent("Wasabi Whip spun through " + hits + " nearby zombie(s).");
    }

    static void kiwibeastSlam(Game game, Plant plant) {
        int multiplier = parameter(plant, "damageMultiplier", 10);
        int hits = damageNearby(game, plant,
            Math.max(1, plant.getEffectiveAttackPower()) * multiplier,
            ProjectileType.NORMAL, parameter(plant, "rowRadius", 2),
            plant.getDefinition().getPlantFoodParameter("columnRadius", 2.5));
        game.addEvent("Kiwibeast slammed the ground and hit " + hits + " zombie(s).");
    }

    static void homingVolley(Game game, Plant plant) {
        int shots = parameter(plant, "shots", 10);
        int hits = 0;
        for (int index = 0; index < shots; index++) {
            Zombie target = nearestAnywhere(game);
            if (target == null) {
                break;
            }
            hits += hit(game, plant, target, Math.max(1, plant.getEffectiveAttackPower()),
                ProjectileType.NORMAL);
        }
        game.addEvent(plant.getName() + " fired a homing barrage with " + hits + " hit(s).");
    }

    private static int damageLaneSequentially(Game game, Plant plant, int row, int damage,
                                              int shots, ProjectileType type) {
        int hits = 0;
        for (int index = 0; index < shots; index++) {
            Zombie target = nearestAhead(game, plant, row);
            if (target == null) {
                break;
            }
            hits += hit(game, plant, target, damage, type);
        }
        return hits;
    }

    private static Zombie nearestAhead(Game game, Plant plant, int row) {
        return game.board.findNearestZombieAhead(row, plant.getPosition().getColumn());
    }

    private static Zombie nearestAnywhere(Game game) {
        return game.board.findNearestZombieAnywhere();
    }

    private static int hit(Game game, Plant plant, Zombie zombie, int damage,
                           ProjectileType type) {
        if (zombie == null || zombie.isDead()) {
            return 0;
        }
        boolean affected = zombie.takeProjectileDamage(Math.max(1, damage), type,
            plant.getChillDurationTicks(), false, plant.getName());
        return affected ? 1 : 0;
    }

    private static int randomDirectHits(Game game, Plant plant, int count, int damage,
                                        ProjectileType type) {
        ArrayList<Zombie> targets = game.hostileZombies();
        int hits = 0;
        for (int index = 0; index < count && !targets.isEmpty(); index++) {
            Zombie target = targets.remove(game.random.nextInt(targets.size()));
            hits += hit(game, plant, target, damage, type);
        }
        return hits;
    }

    private static void damageArea(Game game, Plant plant, Zombie center, int damage,
                                   double splashFactor, ProjectileType type, boolean lobbed) {
        if (center == null || center.getPosition() == null) {
            return;
        }
        for (Zombie zombie : game.hostileZombies()) {
            if (zombie.getPosition() == null) {
                continue;
            }
            int rowDistance = Math.abs(zombie.getPosition().getRow()
                - center.getPosition().getRow());
            double colDistance = Math.abs(zombie.getPosition().getColumn()
                - center.getPosition().getColumn());
            if (rowDistance > 1 || colDistance > 1.5) {
                continue;
            }
            int appliedDamage = zombie == center ? damage
                : Math.max(1, (int) Math.round(damage * splashFactor));
            zombie.takeProjectileDamage(appliedDamage, type, plant.getChillDurationTicks(),
                lobbed, plant.getName());
        }
    }

    private static int damageNearby(Game game, Plant plant, int damage, ProjectileType type,
                                    int rowRadius, double columnRadius) {
        int hits = 0;
        GridPosition center = plant.getPosition();
        for (Zombie zombie : game.hostileZombies()) {
            int rowDistance = Math.abs(zombie.getPosition().getRow() - center.getRow());
            double colDistance = Math.abs(zombie.getPosition().getColumn() - center.getColumn());
            if (rowDistance <= rowRadius && colDistance <= columnRadius) {
                hits += hit(game, plant, zombie, damage, type);
            }
        }
        return hits;
    }

    private static List<Zombie> hostileInRow(Game game, int row) {
        ArrayList<Zombie> result = new ArrayList<>();
        for (Zombie zombie : game.board.getZombiesInRow(row)) {
            if (!zombie.isHypnotized() && !zombie.isDead()) {
                result.add(zombie);
            }
        }
        return result;
    }

    private static double distance(Plant plant, Zombie zombie) {
        GridPosition source = plant.getPosition();
        BoardPosition target = zombie.getPosition();
        return Math.abs(source.getRow() - target.getRow()) * 10.0
            + Math.abs(source.getColumn() - target.getColumn());
    }

    private static int parameter(Plant plant, String name, int fallback) {
        return plant.getDefinition().getPlantFoodParameterInt(name, fallback);
    }
}
