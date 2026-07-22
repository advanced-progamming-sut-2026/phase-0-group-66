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
            if (plant.getFrozenHealth() <= 0 && plant.getOctopusHealth() <= 0) {
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
        if (target.getFrozenHealth() > 0) {
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
                target.stun(5 * Game.TICKS_PER_SECOND);
                target.chill(10 * Game.TICKS_PER_SECOND);
                plant.takeDamage(Math.max(plant.getHealth(), 1));
                engine.addEvent("Iceberg Lettuce froze " + target.getName() + ".");
            }
            case TANGLE_KELP -> {
                target.kill(plant.getName());
                plant.takeDamage(Math.max(plant.getHealth(), 1));
                engine.addEvent("Tangle Kelp pulled " + target.getName() + " underwater.");
            }
            case SQUASH -> {
                target.kill(plant.getName());
                plant.takeDamage(Math.max(plant.getHealth(), 1));
                engine.addEvent("Squash crushed " + target.getName() + ".");
            }
            default -> engine.detonatePlant(plant);
        }
    }
    static void performPassivePlantAction(Game engine, Plant plant) {
        if (engine.elapsedTicks % Game.TICKS_PER_SECOND != 0) {
            return;
        }
        if (plant.getAbility() == PlantAbility.SWEET_POTATO) {
            engine.pullZombiesTowardSweetPotato(plant);
        }
    }
    static void performActivePlantAction(Game engine, Plant plant) {
        switch (plant.getAbility()) {
            case THREEPEATER -> engine.fireThreepeater(plant);
            case ROTOBAGA -> engine.fireRotobaga(plant);
            case SPLIT_PEA -> engine.fireSplitPea(plant);
            case STARFRUIT -> engine.fireStarfruit(plant);
            case BOWLING_BULB -> engine.bowlBulbs(plant);
            case FUME_SHROOM -> engine.attackFumeShroom(plant);
            case CABBAGE_PULT, KERNEL_PULT, MELON_PULT,
                 WINTER_MELON, PEPPER_PULT -> engine.attackLobber(plant);
            case CAULIPOWER -> engine.hypnotizeWithCaulipower(plant);
            case ELECTRIC_BLUEBERRY -> engine.strikeWithBlueberry(plant);
            case MAGNET_SHROOM -> engine.useMagnetShroom(plant);
            case CHOMPER -> engine.chompZombie(plant);
            case CAT_TAIL -> engine.attackHoming(plant);
            case BONK_CHOY, PHAT_BEET, WASABI_WHIP, KIWIBEAST -> engine.attackMelee(plant);
            case TORCHWOOD, WALL_NUT, TALL_NUT, ENDURIAN, GARLIC,
                 SWEET_POTATO, EXPLODE_O_NUT, PUMPKIN, SUN_BEAN,
                 HYPNO_SHROOM, LILY_PAD, IMITATER, GENERIC -> { }
            default -> {
                if (plant.isHoming()) {
                    engine.attackHoming(plant);
                } else if (plant.isMelee()) {
                    engine.attackMelee(plant);
                } else if (plant.isShooter()) {
                    engine.shootProjectiles(plant);
                }
            }
        }
    }
    static void fireThreepeater(Game engine, Plant plant) {
        int centerRow = plant.getPosition().getRow();
        int fired = 0;
        for (int row = Math.max(0, centerRow - 1);
             row <= Math.min(engine.board.getRows() - 1, centerRow + 1); row++) {
            if (engine.fireProjectileInRow(plant, row, 1, 1)) {
                fired++;
            }
        }
        if (fired > 0) {
            engine.addEvent("Threepeater fired into " + fired + " lane(s).");
        }
    }
    static void fireRotobaga(Game engine, Plant plant) {
        GridPosition position = plant.getPosition();
        int hits = 0;
        for (int row : List.of(position.getRow() - 1, position.getRow() + 1)) {
            if (row < 0 || row >= engine.board.getRows()) {
                continue;
            }
            Zombie target = engine.board.findNearestZombieAhead(row, position.getColumn());
            if (target != null) {
                engine.damageZombieFromPlant(target, plant,
                    Math.max(1, plant.getEffectiveAttackPower()) * 3, false);
                hits++;
            }
        }
        engine.addEvent("Rotobaga hit " + hits + " diagonal target(s).");
    }
    static void fireSplitPea(Game engine, Plant plant) {
        GridPosition position = plant.getPosition();
        Zombie ahead = engine.board.findNearestZombieAhead(position.getRow(), position.getColumn());
        Zombie behind = engine.board.findNearestZombieBehind(position.getRow(), position.getColumn());
        if (ahead != null) {
            engine.damageZombieFromPlant(ahead, plant, Math.max(1, plant.getEffectiveAttackPower()), false);
        }
        if (behind != null) {
            engine.damageZombieFromPlant(behind, plant,
                Math.max(1, plant.getEffectiveAttackPower()) * 2, false);
        }
    }
    static void fireStarfruit(Game engine, Plant plant) {
        GridPosition position = plant.getPosition();
        LinkedHashSet<Zombie> targets = new LinkedHashSet<>();
        Zombie ahead = engine.board.findNearestZombieAhead(position.getRow(), position.getColumn());
        Zombie behind = engine.board.findNearestZombieBehind(position.getRow(), position.getColumn());
        if (ahead != null) {
            targets.add(ahead);
        }
        if (behind != null) {
            targets.add(behind);
        }
        for (int row : List.of(position.getRow() - 1, position.getRow() + 1)) {
            if (row >= 0 && row < engine.board.getRows()) {
                Zombie diagonal = engine.board.findNearestZombieAhead(row, position.getColumn());
                if (diagonal != null) {
                    targets.add(diagonal);
                }
            }
        }
        for (Zombie target : targets) {
            engine.damageZombieFromPlant(target, plant, Math.max(1, plant.getEffectiveAttackPower()), false);
        }
        engine.addEvent("Starfruit fired in multiple directions and hit " + targets.size() + " target(s).");
    }
    static void bowlBulbs(Game engine, Plant plant) {
        GridPosition position = plant.getPosition();
        int[] damages = {40, 120, 180};
        int row = position.getRow();
        int hits = 0;
        for (int damage : damages) {
            Zombie target = engine.board.findNearestZombieAhead(row, position.getColumn());
            if (target == null) {
                break;
            }
            engine.damageZombieFromPlant(target, plant, damage, false);
            hits++;
            row += engine.random.nextBoolean() ? 1 : -1;
            row = Math.max(0, Math.min(engine.board.getRows() - 1, row));
        }
        engine.addEvent("Bowling Bulb bounced through " + hits + " target(s).");
    }
    static void attackFumeShroom(Game engine, Plant plant) {
        GridPosition position = plant.getPosition();
        int hits = 0;
        for (Zombie zombie : new ArrayList<>(engine.board.getZombiesInRow(position.getRow()))) {
            double distance = zombie.getPosition().getColumn() - position.getColumn();
            if (!zombie.isHypnotized() && distance >= 0 && distance <= 4.0) {
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
        if (plant.getAbility() == PlantAbility.KERNEL_PULT && engine.random.nextInt(4) == 0) {
            damage = Math.max(damage, 40);
            target.stun(3 * Game.TICKS_PER_SECOND);
            engine.addEvent("Kernel-pult butter stunned " + target.getName() + ".");
        }
        engine.damageZombieFromPlant(target, plant, damage, true);
        if (plant.getDefinition().hasTag("AoE")) {
            engine.damageAdjacentZombies(target, plant, Math.max(1, damage / 2), true);
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
        Zombie target = targets.get(engine.random.nextInt(targets.size()));
        target.kill(plant.getName());
        engine.addEvent("Electric Blueberry electrocuted " + target.getName() + ".");
    }
    static void useMagnetShroom(Game engine, Plant plant) {
        Zombie target = null;
        for (Zombie zombie : engine.hostileZombies()) {
            if (zombie.hasMetalArmor()) {
                target = zombie;
                break;
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
        Zombie target = engine.board.findNearestZombieAhead(position.getRow(), position.getColumn() - 0.5);
        if (target != null && target.getPosition().getColumn() <= position.getColumn() + 1.25) {
            target.kill(plant.getName());
            plant.startDigestion(40 * Game.TICKS_PER_SECOND);
            engine.addEvent("Chomper swallowed " + target.getName() + " and started digesting.");
        }
    }
    static boolean fireProjectileInRow(Game engine, Plant plant, int row, int count, int maxHits) {
        Zombie target = engine.board.findNearestZombieAhead(row, plant.getPosition().getColumn());
        if (target == null) {
            return false;
        }
        for (int index = 0; index < count; index++) {
            Projectile projectile = new Projectile(plant.getEffectiveAttackPower(),
                Game.PROJECTILE_SPEED, new BoardPosition(row, plant.getPosition().getColumn() + 0.25),
                plant.getProjectileElementType(), maxHits > 1,
                plant.getChillDurationTicks(), false, plant.getName(), maxHits);
            engine.board.addProjectile(projectile);
        }
        return true;
    }
    static void pullZombiesTowardSweetPotato(Game engine, Plant plant) {
        int targetRow = plant.getPosition().getRow();
        for (Zombie zombie : engine.hostileZombies()) {
            if (zombie.getPosition() == null) {
                continue;
            }
            int row = zombie.getPosition().getRow();
            double distance = Math.abs(zombie.getPosition().getColumn()
                - plant.getPosition().getColumn());
            if (Math.abs(row - targetRow) == 1 && distance <= 3.0) {
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
        if (zombie == null || zombie.isDead()) {
            return;
        }
        zombie.takeProjectileDamage(Math.max(0, damage), plant.getProjectileElementType(),
            plant.getChillDurationTicks(), lobbed, plant.getName());
    }
    static ArrayList<Zombie> hostileZombies(Game engine) {
        ArrayList<Zombie> result = new ArrayList<>();
        for (Zombie zombie : engine.board.getZombies()) {
            if (!zombie.isDead() && !zombie.isHypnotized() && zombie.getPosition() != null) {
                result.add(zombie);
            }
        }
        return result;
    }
    static boolean reflectProjectileIfNeeded(Game engine, Projectile projectile, Zombie target) {
        if (target.getAbility() != ZombieAbility.JUGGLER || projectile.isLobbed()) {
            return false;
        }
        Plant victim = engine.nearestPlantInRow(target.getPosition().getRow(),
            target.getPosition().getColumn());
        if (victim != null) {
            victim.takeDamage(projectile.getDamage());
            engine.addEvent("Juggler Zombie reflected a projectile into " + victim.getName() + ".");
        }
        projectile.deactivate();
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
