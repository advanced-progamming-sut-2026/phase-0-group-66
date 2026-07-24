package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

final class PlantAttackSystem {
    private PlantAttackSystem() { }

    static boolean assistDisabledPlant(Game engine, Plant helper) {
        Plant target = null;
        double bestDistance = Double.MAX_VALUE;
        for (Plant plant : engine.board.getPlants()) {
            if (plant == helper || plant.getPosition() == null || plant.isDestroyed()) {
                continue;
            }
            if (plant.getFrozenHealth() <= 0 && plant.getOctopusHealth() <= 0
                && !plant.isTrappedInIceTile()) {
                continue;
            }
            int rowDistance = Math.abs(plant.getPosition().getRow()
                - helper.getPosition().getRow());
            double columnDistance = Math.abs(plant.getPosition().getColumn()
                - helper.getPosition().getColumn());
            if (!helper.isLobber() && rowDistance != 0) {
                continue;
            }
            double distance = rowDistance * engine.board.getCols() + columnDistance;
            if (distance < bestDistance) {
                bestDistance = distance;
                target = plant;
            }
        }
        if (target == null) {
            return false;
        }
        int damage = Math.max(1, helper.getEffectiveAttackPower());
        if (target.isTrappedInIceTile()) {
            GridPosition position = target.getPosition();
            Tile tile = engine.board.getTile(position.getRow(), position.getColumn());
            tile.damageIce(damage, helper.getDefinition().hasTag("Fire"));
            engine.addEvent(helper.getName() + " damaged tile ice covering "
                + target.getName() + "; remaining=" + tile.getIceHealth() + ".");
        } else if (target.getFrozenHealth() > 0) {
            target.damageIce(damage, helper.getDefinition().hasTag("Fire"));
            engine.addEvent(helper.getName() + " damaged the ice covering " + target.getName() + ".");
        } else {
            target.damageOctopus(damage);
            engine.addEvent(helper.getName() + " damaged the octopus covering " + target.getName() + ".");
        }
        return true;
    }
    static void performTrapAction(Game engine, Plant plant) {
        if (!plant.isArmed()) {
            return;
        }
        GridPosition position = plant.getPosition();
        Zombie target = engine.board.findNearestZombieAhead(position.getRow(), position.getColumn() - 0.5);
        if (target == null || target.getPosition().getColumn() > position.getColumn() + 0.85) {
            return;
        }
        switch (plant.getAbility()) {
            case ICEBERG_LETTUCE -> {
                int freezeTicks = plant.getDefinition().getAbilityParameterInt(
                    "freezeSeconds", 5) * Game.TICKS_PER_SECOND
                    + plant.getChillBonusTicks();
                int chillTicks = plant.getDefinition().getAbilityParameterInt(
                    "chillSeconds", 10) * Game.TICKS_PER_SECOND;
                target.stun(freezeTicks);
                target.chill(chillTicks);
                plant.takeDamage(Math.max(plant.getHealth(), 1));
                engine.addEvent("Iceberg Lettuce froze " + target.getName() + ".");
            }
            case TANGLE_KELP -> {
                int targetCount = 1 + plant.getUpgradeTraitInt("TARGETS_1", 0);
                int killed = killNearbyTrapTargets(engine, plant, target, targetCount, true);
                plant.takeDamage(Math.max(plant.getHealth(), 1));
                engine.addEvent("Tangle Kelp pulled " + killed + " zombie(s) underwater.");
            }
            case SQUASH -> {
                int targetCount = plant.hasUpgradeTrait("CAN_CRUSH_2X") ? 2 : 1;
                int killed = killNearbyTrapTargets(engine, plant, target, targetCount, false);
                plant.takeDamage(Math.max(plant.getHealth(), 1));
                engine.addEvent("Squash crushed " + killed + " zombie(s).");
            }
            default -> engine.detonatePlant(plant);
        }
    }
    private static int killNearbyTrapTargets(Game engine, Plant plant, Zombie first,
                                             int targetCount, boolean waterOnly) {
        int killed = 0;
        if (first != null && !first.isDead()) {
            first.kill(plant.getName());
            killed++;
        }
        GridPosition position = plant.getPosition();
        for (Zombie zombie : engine.hostileZombies()) {
            if (killed >= targetCount || zombie == first || zombie.getPosition() == null) {
                continue;
            }
            if (zombie.getPosition().getRow() != position.getRow()
                || Math.abs(zombie.getPosition().getColumn() - position.getColumn()) > 2.0) {
                continue;
            }
            if (waterOnly) {
                int col = (int) Math.floor(zombie.getPosition().getColumn());
                if (!engine.board.isInside(zombie.getPosition().getRow(), col)) {
                    continue;
                }
                TileType type = engine.board.getTile(zombie.getPosition().getRow(), col).getType();
                if (type != TileType.WATER && type != TileType.LOW_TIDE) {
                    continue;
                }
            }
            zombie.kill(plant.getName());
            killed++;
        }
        return killed;
    }

    static void performPassivePlantAction(Game engine, Plant plant) {
        if (engine.elapsedTicks % Game.TICKS_PER_SECOND != 0) {
            return;
        }
        if (plant.getAbility() == PlantAbility.SWEET_POTATO) {
            engine.pullZombiesTowardSweetPotato(plant);
        } else if (plant.getAbility().isMint()) {
            BattleRuleSystem.empowerMintFamily(engine, plant);
        }
    }
    static void performActivePlantAction(Game engine, Plant plant) {
        PlantBehaviorFactory.create(plant.getAbility()).perform(engine, plant);
        int chance = plant.getUpgradeTraitInt("PLANT_FOOD_CHANCE_5", 0);
        if (plant.getAbility() == PlantAbility.MEGA_GATLING_PEA && chance > 0
            && engine.random.nextInt(100) < chance) {
            plant.usePlantFood();
            PlantFoodBehaviorFactory.activate(engine, plant);
            engine.addEvent("Mega Gatling Pea triggered its level-up plant-food chance.");
        }
    }
    static void fireThreepeater(Game engine, Plant plant) {
        int centerRow = plant.getPosition().getRow();
        int radius = plant.getDefinition().getAbilityParameterInt("laneRadius", 1);
        int fired = 0;
        for (int row = Math.max(0, centerRow - radius);
             row <= Math.min(engine.board.getRows() - 1, centerRow + radius); row++) {
            if (engine.fireProjectileInRow(plant, row, 1, 1)) {
                fired++;
            }
        }
        if (fired > 0) {
            engine.addEvent("Threepeater fired into " + fired + " lane(s).");
        }
    }
    static void fireRotobaga(Game engine, Plant plant) {
        int hits = fireRotobagaVolley(engine, plant);
        if (hits > 0) {
            engine.addEvent("Rotobaga hit " + hits + " diagonal target(s).");
        }
    }

    static int fireRotobagaVolley(Game engine, Plant plant) {
        GridPosition position = plant.getPosition();
        int shots = plant.getDefinition().getAbilityParameterInt("shotsPerDirection", 3);
        int hits = 0;
        for (int row : List.of(position.getRow() - 1, position.getRow() + 1)) {
            if (row < 0 || row >= engine.board.getRows()) {
                continue;
            }
            hits += hitDirectionalTarget(engine, plant,
                engine.board.findNearestZombieAhead(row, position.getColumn()), shots);
            hits += hitDirectionalTarget(engine, plant,
                engine.board.findNearestZombieBehind(row, position.getColumn()), shots);
        }
        return hits;
    }

    private static int hitDirectionalTarget(Game engine, Plant plant, Zombie target, int shots) {
        if (target == null) {
            return 0;
        }
        int hits = 0;
        for (int index = 0; index < shots && !target.isDead(); index++) {
            engine.damageZombieFromPlant(target, plant,
                Math.max(1, plant.getEffectiveAttackPower()), false);
            hits++;
        }
        return hits;
    }
    static void fireSplitPea(Game engine, Plant plant) {
        GridPosition position = plant.getPosition();
        Zombie ahead = engine.board.findNearestZombieAhead(position.getRow(), position.getColumn());
        Zombie behind = engine.board.findNearestZombieBehind(position.getRow(), position.getColumn());
        int frontShots = plant.getDefinition().getAbilityParameterInt("forwardShots", 1);
        int backShots = plant.getDefinition().getAbilityParameterInt("backwardShots", 2);
        hitDirectionalTarget(engine, plant, ahead, frontShots);
        hitDirectionalTarget(engine, plant, behind, backShots);
    }
    static void fireStarfruit(Game engine, Plant plant) {
        int hits = fireStarfruitVolley(engine, plant);
        if (hits > 0) {
            engine.addEvent("Starfruit fired in five directions and hit " + hits + " target(s).");
        }
    }

    static int fireStarfruitVolley(Game engine, Plant plant) {
        GridPosition position = plant.getPosition();
        LinkedHashSet<Zombie> targets = new LinkedHashSet<>();
        addTarget(targets, engine.board.findNearestZombieAhead(position.getRow(),
            position.getColumn()));
        for (int row : List.of(position.getRow() - 1, position.getRow() + 1)) {
            if (row < 0 || row >= engine.board.getRows()) {
                continue;
            }
            addTarget(targets, engine.board.findNearestZombieAhead(row, position.getColumn()));
            addTarget(targets, engine.board.findNearestZombieBehind(row, position.getColumn()));
        }
        int hits = 0;
        for (Zombie target : targets) {
            engine.damageZombieFromPlant(target, plant,
                Math.max(1, plant.getEffectiveAttackPower()), false);
            hits++;
        }
        return hits;
    }

    private static void addTarget(LinkedHashSet<Zombie> targets, Zombie target) {
        if (target != null) {
            targets.add(target);
        }
    }
    static void bowlBulbs(Game engine, Plant plant) {
        int damage = plant.nextBowlingBulbDamage();
        if (damage <= 0) {
            return;
        }
        GridPosition position = plant.getPosition();
        int row = position.getRow();
        int hits = 0;
        int maximumBounces = plant.getDefinition().getAbilityParameterInt(
            "maxBounces", 3);
        for (int bounce = 0; bounce < maximumBounces; bounce++) {
            Zombie target = engine.board.findNearestZombieAhead(row, position.getColumn());
            if (target == null) {
                break;
            }
            engine.damageZombieFromPlant(target, plant, damage, false);
            hits++;
            row = nextBounceRow(engine, row);
        }
        engine.addEvent("Bowling Bulb launched a " + damage + " damage bulb with " + hits
            + " bounce hit(s).");
    }

    private static int nextBounceRow(Game engine, int row) {
        int direction = engine.random.nextBoolean() ? 1 : -1;
        int next = row + direction;
        if (next < 0 || next >= engine.board.getRows()) {
            next = row - direction;
        }
        return Math.max(0, Math.min(engine.board.getRows() - 1, next));
    }
    static void attackFumeShroom(Game engine, Plant plant) {
        GridPosition position = plant.getPosition();
        int hits = 0;
        for (Zombie zombie : new ArrayList<>(engine.board.getZombiesInRow(position.getRow()))) {
            double distance = zombie.getPosition().getColumn() - position.getColumn();
            double range = plant.getEffectiveRange(
                plant.getDefinition().getAbilityParameter("rangeTiles", 4.0));
            if (!zombie.isHypnotized() && distance >= 0 && distance <= range) {
                engine.damageZombieFromPlant(zombie, plant,
                    Math.max(1, plant.getEffectiveAttackPower()), false);
                hits++;
            }
        }
        if (hits > 0) {
            engine.addEvent("Fume-shroom pierced " + hits + " zombie(s).");
        }
    }
    static void attackLobber(Game engine, Plant plant) {
        GridPosition position = plant.getPosition();
        Zombie target = engine.board.findNearestZombieAhead(position.getRow(), position.getColumn());
        if (target == null) {
            return;
        }
        int damage = Math.max(1, plant.getEffectiveAttackPower());
        int butterChance = plant.getDefinition().getAbilityParameterInt(
            "butterChancePercent", 25)
            + plant.getUpgradeTraitInt("BUTTER_5", 0);
        if (plant.getAbility() == PlantAbility.KERNEL_PULT
            && engine.random.nextInt(100) < butterChance) {
            damage = Math.max(damage,
                plant.getDefinition().getAbilityParameterInt("butterDamage", 40));
            int stunSeconds = plant.getDefinition().getAbilityParameterInt("butterStunSeconds", 3);
            target.stun(stunSeconds * Game.TICKS_PER_SECOND);
            engine.addEvent("Kernel-pult butter stunned " + target.getName() + ".");
        }
        engine.damageZombieFromPlant(target, plant, damage, true);
        if (plant.getDefinition().hasTag("AoE")) {
            double factor = plant.getDefinition().getAbilityParameter(
                "splashDamageFactor", 0.5);
            int splashDamage = Math.max(1, (int) Math.round(damage * factor)
                + plant.getSplashDamageBonus());
            engine.damageAdjacentZombies(target, plant, splashDamage, true);
        }
    }
    static void hypnotizeWithCaulipower(Game engine, Plant plant) {
        ArrayList<Zombie> targets = engine.hostileZombies();
        if (targets.isEmpty()) {
            return;
        }
        Zombie target = targets.get(engine.random.nextInt(targets.size()));
        target.hypnotize();
        engine.addEvent("Caulipower hypnotized " + target.getName() + ".");
    }
    static void strikeWithBlueberry(Game engine, Plant plant) {
        ArrayList<Zombie> targets = engine.hostileZombies();
        if (targets.isEmpty()) {
            return;
        }
        Zombie target;
        if (plant.hasUpgradeTrait("TARGET_PRIORITY_UP")) {
            target = Collections.max(targets,
                (first, second) -> Integer.compare(first.getEffectiveHealth(),
                    second.getEffectiveHealth()));
        } else {
            target = targets.get(engine.random.nextInt(targets.size()));
        }
        target.kill(plant.getName());
        engine.addEvent("Electric Blueberry electrocuted " + target.getName() + ".");
    }
    static void useMagnetShroom(Game engine, Plant plant) {
        Zombie target = null;
        double range = plant.getEffectiveRange(
            plant.getDefinition().getAbilityParameter("rangeTiles", 3.0));
        double bestDistance = Double.MAX_VALUE;
        for (Zombie zombie : engine.hostileZombies()) {
            if (!zombie.hasMetalArmor() || zombie.getPosition() == null
                || zombie.getPosition().getRow() != plant.getPosition().getRow()) {
                continue;
            }
            double distance = Math.abs(zombie.getPosition().getColumn()
                - plant.getPosition().getColumn());
            if (distance <= range && distance < bestDistance) {
                target = zombie;
                bestDistance = distance;
            }
        }
        if (target != null) {
            int removed = target.removeMetalArmor();
            engine.addEvent("Magnet-shroom removed " + removed + " armor health from "
                + target.getName() + ".");
        }
    }
    static void chompZombie(Game engine, Plant plant) {
        GridPosition position = plant.getPosition();
        Zombie target = engine.board.findNearestZombieAhead(position.getRow(),
            position.getColumn() - 0.5);
        double range = plant.getEffectiveRange(
            plant.getDefinition().getAbilityParameter("rangeTiles", 1.25));
        if (target != null && target.getPosition().getColumn() <= position.getColumn() + range) {
            target.kill(plant.getName());
            int digestSeconds = plant.getDigestionSeconds();
            plant.startDigestion(digestSeconds * Game.TICKS_PER_SECOND);
            engine.addEvent("Chomper swallowed " + target.getName() + " and started digesting.");
        }
    }
    static boolean fireProjectileInRow(Game engine, Plant plant, int row, int count, int maxHits) {
        Zombie target = engine.board.findNearestZombieAhead(row, plant.getPosition().getColumn());
        if (target == null) {
            return false;
        }
        for (int index = 0; index < count; index++) {
            int poisonSeconds = plant.getDefinition().getAbilityParameterInt(
                "poisonSeconds", 5);
            double poisonFactor = plant.getDefinition().getAbilityParameter(
                "poisonDamageFactor", 0.25);
            int poisonDamage = Math.max(0, (int) Math.round(
                plant.getEffectiveAttackPower() * poisonFactor))
                + plant.getUpgradeTraitInt("POISON_TICK_BONUS", 0);
            Projectile projectile = new Projectile(plant.getEffectiveAttackPower(),
                Game.PROJECTILE_SPEED, new BoardPosition(row,
                    plant.getPosition().getColumn() + 0.25),
                plant.getProjectileElementType(), maxHits > 1,
                plant.getChillDurationTicks(), false, plant.getName(), maxHits,
                poisonSeconds * Game.TICKS_PER_SECOND, poisonDamage);
            engine.board.addProjectile(projectile);
        }
        return true;
    }
    static void pullZombiesTowardSweetPotato(Game engine, Plant plant) {
        double radius = plant.getDefinition().getAbilityParameter("pullRadiusTiles", 3.0);
        pullZombiesTowardSweetPotato(engine, plant, radius);
    }

    static void pullZombiesTowardSweetPotato(Game engine, Plant plant, double radius) {
        int targetRow = plant.getPosition().getRow();
        for (Zombie zombie : engine.hostileZombies()) {
            if (zombie.getPosition() == null) {
                continue;
            }
            int row = zombie.getPosition().getRow();
            double distance = Math.abs(zombie.getPosition().getColumn()
                - plant.getPosition().getColumn());
            if (Math.abs(row - targetRow) == 1 && distance <= Math.max(0.0, radius)) {
                zombie.setPosition(zombie.getPosition().withRow(targetRow));
            }
        }
    }
    static void damageAdjacentZombies(Game engine, Zombie center, Plant source, int damage, boolean lobbed) {
        if (center.getPosition() == null) {
            return;
        }
        for (Zombie zombie : engine.hostileZombies()) {
            if (zombie == center || zombie.getPosition() == null) {
                continue;
            }
            if (Math.abs(zombie.getPosition().getRow() - center.getPosition().getRow()) <= 1
                && Math.abs(zombie.getPosition().getColumn()
                    - center.getPosition().getColumn()) <= 1.5) {
                engine.damageZombieFromPlant(zombie, source, damage, lobbed);
            }
        }
    }
    static void damageZombieFromPlant(Game engine, Zombie zombie, Plant plant, int damage, boolean lobbed) {
        if (zombie == null || zombie.isDead() || zombie.isTrappedInIceTile()) {
            return;
        }
        zombie.takeProjectileDamage(Math.max(0, damage), plant.getProjectileElementType(),
            plant.getChillDurationTicks(), lobbed, plant.getName());
    }
    static ArrayList<Zombie> hostileZombies(Game engine) {
        ArrayList<Zombie> result = new ArrayList<>();
        for (Zombie zombie : engine.board.getZombies()) {
            if (!zombie.isDead() && !zombie.isHypnotized() && zombie.getPosition() != null
                && !zombie.isTrappedInIceTile()) {
                result.add(zombie);
            }
        }
        return result;
    }
    static boolean reflectProjectileIfNeeded(Game engine, Projectile projectile, Zombie target) {
        if (target.getAbility() != ZombieAbility.JUGGLER || projectile.isLobbed()) {
            return false;
        }
        int spinSeconds = target.getDefinition().getSpecialPropertyInt(
            "projectSpinDurationSeconds", 2);
        target.startJuggling(Math.max(1, spinSeconds) * Game.TICKS_PER_SECOND);
        int reflectedDamage = projectile.getDamage()
            * Math.max(1, projectile.getDamageMultiplier());
        ReflectedProjectile reflected = new ReflectedProjectile(reflectedDamage,
            projectile.getSpeed(), new BoardPosition(target.getPosition().getRow(),
                target.getPosition().getColumn() - 0.05), projectile.getImpactType());
        engine.board.addReflectedProjectile(reflected);
        projectile.deactivate();
        engine.addEvent("Juggler Zombie started spinning and reflected a "
            + projectile.getImpactType() + " projectile toward the plants.");
        return true;
    }
    static Plant nearestPlantInRow(Game engine, int row, double zombieColumn) {
        Plant nearest = null;
        double best = Double.MAX_VALUE;
        for (Plant plant : engine.board.getPlantsInRow(row)) {
            if (plant.getPosition() == null || plant.isDestroyed()) {
                continue;
            }
            double distance = Math.abs(plant.getPosition().getColumn() - zombieColumn);
            if (distance < best) {
                best = distance;
                nearest = plant;
            }
        }
        return nearest;
    }
}
