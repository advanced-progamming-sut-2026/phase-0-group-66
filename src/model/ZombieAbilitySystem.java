package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

final class ZombieAbilitySystem {
    private ZombieAbilitySystem() { }

    static int torchwoodMultiplier(Game engine, Projectile projectile, double fromColumn, double toColumn) {
        PlantDefinition source = engine.plantFactory.findDefinition(
            projectile.getSourcePlant()).orElse(null);
        if (source == null || !source.hasTag("Pea")
            || projectile.getImpactType() == ProjectileType.FIRE) {
            return 1;
        }
        int row = projectile.getPosition().getRow();
        for (Plant plant : engine.board.getPlantsInRow(row)) {
            if (plant.getAbility() != PlantAbility.TORCHWOOD || plant.getPosition() == null) {
                continue;
            }
            int column = plant.getPosition().getColumn();
            if (column + 0.001 >= fromColumn && column - 0.001 <= toColumn) {
                return plant.getTorchwoodMultiplier();
            }
        }
        return 1;
    }
    static void updateZombieEnvironmentState(Game engine, Zombie zombie) {
        if (zombie.getPosition() == null) {
            return;
        }
        int col = (int) Math.floor(zombie.getPosition().getColumn());
        if (zombie.getAbility() == ZombieAbility.SNORKEL
            && engine.board.isInside(zombie.getPosition().getRow(), col)) {
            TileType type = engine.board.getTile(zombie.getPosition().getRow(), col).getType();
            zombie.setSubmerged(type == TileType.WATER || type == TileType.LOW_TIDE);
        }
    }
    static void performZombieSpecialAbility(Game engine, Zombie zombie) {
        ZombieBehaviorFactory.create(zombie.getAbility()).perform(engine, zombie);
    }
    static void throwGargantuarImp(Game engine, Zombie gargantuar) {
        if (gargantuar.isImpThrown()
            || gargantuar.getHealth() * 2 > gargantuar.getMaximumHealth()) {
            return;
        }
        Zombie imp = engine.zombieFactory.createZombie("ZombieImp");
        imp.applyDifficulty(engine.difficultyLevel);
        int row = gargantuar.getPosition().getRow();
        imp.setPosition(new BoardPosition(row, 2.0));
        engine.board.addZombie(imp);
        gargantuar.markImpThrown();
        engine.addEvent("Gargantuar threw an Imp into column 3.");
    }
    static void stealSunWithRa(Game engine, Zombie zombie) {
        if (engine.elapsedTicks % Game.TICKS_PER_SECOND != 0 || zombie.getPosition() == null) {
            return;
        }
        for (Sun sun : new ArrayList<>(engine.board.getSuns())) {
            if (sun.isCollected() || sun.getPosition() == null) {
                continue;
            }
            if (sun.getPosition().getRow() == zombie.getPosition().getRow()) {
                int amount = sun.collect();
                zombie.addStolenSun(amount);
                engine.board.removeSun(sun);
                engine.addEvent("Ra Zombie stole " + amount + " sun.");
            }
        }
    }
    static void raiseTombs(Game engine, Zombie zombie) {
        if (zombie.getAbilityCooldownTicks() > 0 || zombie.getPosition() == null) {
            return;
        }
        int created = 0;
        for (int attempts = 0; attempts < 20 && created < 2; attempts++) {
            int row = engine.random.nextInt(engine.board.getRows());
            int col = 2 + engine.random.nextInt(Math.max(1, engine.board.getCols() - 3));
            GridPosition position = new GridPosition(row, col);
            Tile tile = engine.board.getTile(row, col);
            if (tile.getPlant() == null && tile.getType() == TileType.NORMAL
                && !engine.tombs.containsKey(position)) {
                engine.tombs.put(position, new Tomb(row, col, false, false));
                tile.setTileType(TileType.TOMB);
                created++;
            }
        }
        zombie.setAbilityCooldownTicks(8 * Game.TICKS_PER_SECOND);
        if (created > 0) {
            engine.addEvent("Tomb Raiser created " + created + " tomb(s).");
        }
    }
    static void throwHunterSnowball(Game engine, Zombie zombie) {
        if (zombie.getAbilityCooldownTicks() > 0 || zombie.getPosition() == null) {
            return;
        }
        Plant target = engine.nearestPlantInRow(zombie.getPosition().getRow(),
            zombie.getPosition().getColumn());
        if (target != null) {
            target.addIceLayer();
            engine.addEvent("Hunter Zombie hit " + target.getName() + " with an ice ball ("
                + target.getIceHits() + "/3).");
        }
        zombie.setAbilityCooldownTicks(6 * Game.TICKS_PER_SECOND);
    }
    static void pushTroglobiteIce(Game engine, Zombie zombie) {
        if (zombie.getAbilityCooldownTicks() > 0 || zombie.getPosition() == null) {
            return;
        }
        Plant target = engine.nearestPlantInRow(zombie.getPosition().getRow(),
            zombie.getPosition().getColumn());
        if (target != null && target.getPosition().getColumn()
            < zombie.getPosition().getColumn()
            && zombie.getPosition().getColumn() - target.getPosition().getColumn() <= 2.0) {
            target.takeDamage(Math.max(target.getHealth(), 1));
            engine.addEvent("Troglobite pushed an ice block through " + target.getName() + ".");
        }
        zombie.setAbilityCooldownTicks(5 * Game.TICKS_PER_SECOND);
    }
    static void hookPlantWithFisherman(Game engine, Zombie zombie) {
        if (zombie.getAbilityCooldownTicks() > 0 || zombie.getPosition() == null) {
            return;
        }
        Plant target = engine.nearestPlantInRow(zombie.getPosition().getRow(),
            zombie.getPosition().getColumn());
        if (target != null && target.getPosition() != null) {
            GridPosition old = target.getPosition();
            int newCol = Math.min(engine.board.getCols() - 1, old.getColumn() + 1);
            if (newCol == (int) Math.floor(zombie.getPosition().getColumn())) {
                target.takeDamage(Math.max(target.getHealth(), 1));
                engine.addEvent("Fisherman threw away " + target.getName() + ".");
            } else if (engine.board.getTile(old.getRow(), newCol).getPlant() == null
                && engine.board.getTile(old.getRow(), newCol).getType().isPlantable()) {
                engine.board.removePlant(target);
                engine.board.placePlant(target, old.getRow(), newCol);
                engine.addEvent("Fisherman hooked " + target.getName() + " one tile forward.");
            }
        }
        zombie.setAbilityCooldownTicks(6 * Game.TICKS_PER_SECOND);
    }
    static void throwOctopus(Game engine, Zombie zombie) {
        if (zombie.getAbilityCooldownTicks() > 0 || zombie.getPosition() == null) {
            return;
        }
        Plant target = engine.nearestPlantInRow(zombie.getPosition().getRow(),
            zombie.getPosition().getColumn());
        if (target != null) {
            target.coverWithOctopus();
            engine.addEvent("Octopus Zombie covered " + target.getName() + ".");
        }
        zombie.setAbilityCooldownTicks(6 * Game.TICKS_PER_SECOND);
    }
    static void transformPlantWithWizard(Game engine, Zombie zombie) {
        if (zombie.getAbilityCooldownTicks() > 0) {
            return;
        }
        ArrayList<Plant> candidates = new ArrayList<>();
        for (Plant plant : engine.board.getPlants()) {
            if (!plant.isDestroyed() && plant.getPosition() != null
                && plant.getAbility() != PlantAbility.LILY_PAD) {
                candidates.add(plant);
            }
        }
        if (!candidates.isEmpty()) {
            Plant target = candidates.get(engine.random.nextInt(candidates.size()));
            target.transformByWizard(zombie.getRuntimeId());
            engine.addEvent("Wizard transformed " + target.getName() + " into a harmless cat.");
        }
        zombie.setAbilityCooldownTicks(8 * Game.TICKS_PER_SECOND);
    }
    static void knightNearbyZombie(Game engine, Zombie king) {
        if (king.getAbilityCooldownTicks() > 0 || king.getPosition() == null) {
            return;
        }
        Zombie target = null;
        for (Zombie zombie : engine.hostileZombies()) {
            if (zombie != king && zombie.getAbility() == ZombieAbility.BASIC
                && Math.abs(zombie.getPosition().getRow() - king.getPosition().getRow()) <= 1
                && Math.abs(zombie.getPosition().getColumn()
                    - king.getPosition().getColumn()) <= 3.0) {
                target = zombie;
                break;
            }
        }
        if (target != null) {
            target.addBonusArmor(3200);
            engine.addEvent("King promoted " + target.getName() + " to a Knight.");
        }
        king.setAbilityCooldownTicks(8 * Game.TICKS_PER_SECOND);
    }
    static void useTurquoiseSkull(Game engine, Zombie zombie) {
        if (zombie.getPosition() == null || zombie.getAbilityCooldownTicks() > 0
            || engine.elapsedTicks % Game.TICKS_PER_SECOND != 0) {
            return;
        }
        Plant target = engine.nearestPlantInRow(zombie.getPosition().getRow(),
            zombie.getPosition().getColumn());
        if (target == null || Math.abs(zombie.getPosition().getColumn()
            - target.getPosition().getColumn()) > 4.0) {
            return;
        }
        int stolen = Math.min(25, engine.sunAmount);
        engine.sunAmount -= stolen;
        zombie.addStolenSun(stolen);
        zombie.specialAbility();
        engine.addEvent("Turquoise Skull stole " + stolen + " sun while charging.");
        if (zombie.getSpecialAbilityUses() % 5 == 0) {
            engine.fireTurquoiseLaser(zombie);
            zombie.setAbilityCooldownTicks(10 * Game.TICKS_PER_SECOND);
        }
    }
    static void fireTurquoiseLaser(Game engine, Zombie zombie) {
        int row = zombie.getPosition().getRow();
        int start = (int) Math.floor(zombie.getPosition().getColumn()) - 1;
        int destroyed = 0;
        for (int col = Math.max(0, start - 3); col <= Math.min(engine.board.getCols() - 1, start); col++) {
            Plant plant = engine.board.getTile(row, col).getBlockingPlant();
            if (plant != null) {
                plant.takeDamage(Math.max(plant.getHealth(), 1));
                destroyed++;
            }
        }
        engine.addEvent("Turquoise Skull fired its laser and destroyed " + destroyed + " plant(s).");
    }
    static void launchProspectorDynamite(Game engine, Zombie zombie) {
        if (!zombie.isReversed() && !zombie.isSpecialDisabled()
            && zombie.getAgeTicks() >= 10 * Game.TICKS_PER_SECOND) {
            zombie.reverseDirection();
            engine.addEvent("Prospector's dynamite launched it toward the house from the other side.");
        }
    }
    static void playPiano(Game engine, Zombie pianist) {
        if (pianist.getAbilityCooldownTicks() > 0) {
            return;
        }
        int moved = 0;
        for (Zombie zombie : engine.hostileZombies()) {
            if (zombie == pianist || zombie.getPosition() == null) {
                continue;
            }
            int row = zombie.getPosition().getRow();
            int direction = engine.random.nextBoolean() ? 1 : -1;
            int targetRow = row + direction;
            if (targetRow >= 0 && targetRow < engine.board.getRows()) {
                zombie.setPosition(zombie.getPosition().withRow(targetRow));
                moved++;
            }
        }
        pianist.setAbilityCooldownTicks(5 * Game.TICKS_PER_SECOND);
        if (moved > 0) {
            engine.addEvent("Pianist changed the lane of " + moved + " zombie(s).");
        }
    }
    static void moveHypnotizedZombie(Game engine, Zombie zombie) {
        Zombie enemy = engine.nearestHostileZombieForHypnotized(zombie);
        if (enemy != null && Math.abs(enemy.getPosition().getColumn()
            - zombie.getPosition().getColumn()) <= 0.9) {
            enemy.takeDirectDamage(Math.max(1, zombie.getDamage()));
            return;
        }
        zombie.moveOneTick();
        if (zombie.getPosition().getColumn() > engine.board.getCols() + 1) {
            zombie.kill();
        }
    }
    static Zombie nearestHostileZombieForHypnotized(Game engine, Zombie ally) {
        Zombie target = null;
        double best = Double.MAX_VALUE;
        for (Zombie zombie : engine.board.getZombiesInRow(ally.getPosition().getRow())) {
            if (zombie.isHypnotized() || zombie.isDead()) {
                continue;
            }
            double distance = Math.abs(zombie.getPosition().getColumn()
                - ally.getPosition().getColumn());
            if (distance < best) {
                best = distance;
                target = zombie;
            }
        }
        return target;
    }
    static boolean shouldZombieBypassPlant(Game engine, Zombie zombie, Plant plant) {
        if (zombie.getAbility() != ZombieAbility.DODO_RIDER) {
            return false;
        }
        return plant.getAbility() != PlantAbility.TALL_NUT;
    }
    static boolean isStationaryZombie(Game engine, Zombie zombie) {
        return zombie.getAbility() == ZombieAbility.FISHERMAN
            || zombie.getAbility() == ZombieAbility.KING;
    }
    static void resolveZombiePlantCombat(Game engine, Zombie zombie, Plant plant) {
        if (zombie.getAbility() == ZombieAbility.WIZARD) {
            plant.transformByWizard(zombie.getRuntimeId());
            return;
        }
        if (zombie.getAbility() == ZombieAbility.GARGANTUAR
            || zombie.getAbility() == ZombieAbility.PIANIST
            || (zombie.getAbility() == ZombieAbility.ARCADE && zombie.isMachineActive())
            || (zombie.getAbility() == ZombieAbility.ALL_STAR && !zombie.isChargeUsed())) {
            plant.takeDamage(Math.max(plant.getHealth(), 1));
            zombie.markChargeUsed();
            engine.addEvent(zombie.getName() + " destroyed " + plant.getName() + " on contact.");
            return;
        }
        if (zombie.getAbility() == ZombieAbility.EXPLORER && !zombie.isSpecialDisabled()) {
            plant.takeDamage(Math.max(plant.getHealth(), 1));
            engine.addEvent("Explorer Zombie burned " + plant.getName() + ".");
            return;
        }
        if (engine.elapsedTicks % Game.TICKS_PER_SECOND != 0) {
            return;
        }
        if (plant.getAbility() == PlantAbility.HYPNO_SHROOM) {
            plant.takeDamage(Math.max(plant.getHealth(), 1));
            if (plant.consumeHypnoGargantuar()) {
                replaceWithHypnotizedGargantuar(engine, zombie, plant);
            } else {
                zombie.hypnotize();
                applyHypnoUpgradeBuffs(zombie, plant);
                engine.addEvent("Hypno-shroom converted " + zombie.getName() + ".");
            }
            return;
        }
        zombie.attackPlant(plant);
        int shieldExplosion = plant.consumeShieldExplosionDamage();
        if (shieldExplosion > 0) {
            explodePlantFoodArmor(engine, plant, shieldExplosion);
        }
        if (plant.getAbility() == PlantAbility.ENDURIAN) {
            zombie.takeDirectDamage(plant.getReflectedDamage(), plant.getName());
        } else if (plant.getAbility() == PlantAbility.SUN_BEAN) {
            int sun = plant.getDefinition().getAbilityParameterInt("sunPerHit", 5)
                + plant.getSunProductionBonus();
            engine.sunAmount += sun;
            engine.totalSunCollected += sun;
            engine.addEvent("Sun Bean produced " + sun + " sun after being hit.");
        } else if (plant.getAbility() == PlantAbility.GARLIC && !plant.isDestroyed()) {
            engine.moveZombieToAdjacentLane(zombie);
        }
        engine.addEvent("Zombie " + zombie.getName() + " attacked " + plant.getName()
            + " at " + plant.getPosition() + ".");
    }

    private static void replaceWithHypnotizedGargantuar(Game engine, Zombie eater,
                                                       Plant hypnoShroom) {
        BoardPosition position = eater.getPosition();
        eater.kill("Hypno-shroom");
        Zombie ally = engine.zombieFactory.createZombie("Gargantuar");
        ally.applyDifficulty(engine.difficultyLevel);
        ally.setPosition(position);
        ally.hypnotize();
        applyHypnoUpgradeBuffs(ally, hypnoShroom);
        engine.board.addZombie(ally);
        engine.addEvent("Hypno-shroom transformed its eater into an allied Gargantuar.");
    }

    private static void applyHypnoUpgradeBuffs(Zombie ally, Plant hypnoShroom) {
        if (hypnoShroom.hasUpgradeTrait("ZOMBIE_HP_BUFF")) {
            int percent = hypnoShroom.getDefinition().getAbilityParameterInt(
                "hypnotizedHealthBuffPercent", 50);
            ally.buffHealthPercent(percent);
        }
        if (hypnoShroom.hasUpgradeTrait("ZOMBIE_DMG_BUFF")) {
            int percent = hypnoShroom.getDefinition().getAbilityParameterInt(
                "hypnotizedDamageBuffPercent", 50);
            ally.buffDamagePercent(percent);
        }
    }

    private static void explodePlantFoodArmor(Game engine, Plant plant, int damage) {
        GridPosition center = plant.getPosition();
        int hits = 0;
        for (Zombie target : engine.hostileZombies()) {
            if (Math.abs(target.getPosition().getRow() - center.getRow()) <= 1
                && Math.abs(target.getPosition().getColumn() - center.getColumn()) <= 1.5) {
                target.takeDamage(damage, plant.getName());
                hits++;
            }
        }
        engine.addEvent(plant.getName() + " plant-food armor exploded and hit " + hits
            + " zombie(s).");
    }
    static void moveZombieToAdjacentLane(Game engine, Zombie zombie) {
        int row = zombie.getPosition().getRow();
        int target = row == 0 ? 1 : row == engine.board.getRows() - 1 ? row - 1
            : row + (engine.random.nextBoolean() ? 1 : -1);
        zombie.setPosition(zombie.getPosition().withRow(target));
        engine.addEvent("Garlic redirected " + zombie.getName() + " to lane " + (target + 1) + ".");
    }
    static void explodeDestroyedDefender(Game engine, Plant plant) {
        GridPosition center = plant.getPosition();
        int damage = Math.max(1800, plant.getEffectiveAttackPower());
        for (Zombie zombie : engine.hostileZombies()) {
            if (Math.abs(zombie.getPosition().getRow() - center.getRow()) <= 1
                && Math.abs(zombie.getPosition().getColumn() - center.getColumn()) <= 1.5) {
                zombie.takeDamage(damage, plant.getName());
            }
        }
        engine.addEvent("Explode-o-nut detonated when destroyed.");
    }
}
