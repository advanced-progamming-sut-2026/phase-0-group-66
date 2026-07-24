package model;

import java.util.ArrayList;

/** Runtime system for independent objects created or pushed by special zombies. */
final class ZombieObjectSystem {
    private ZombieObjectSystem() { }

    static void ensureZombieCompanions(Game engine) {
        for (Zombie zombie : new ArrayList<>(engine.board.getZombies())) {
            initializeCompanions(engine, zombie);
        }
    }

    private static void initializeCompanions(Game engine, Zombie zombie) {
        if (zombie.areCompanionsInitialized() || zombie.getPosition() == null) {
            return;
        }
        switch (zombie.getAbility()) {
            case TROGLOBITE -> createTroglobiteIce(engine, zombie);
            case ARCADE -> createArcadeMachine(engine, zombie);
            case BARREL_ROLLER -> createBarrel(engine, zombie);
            default -> { }
        }
        zombie.markCompanionsInitialized();
    }

    private static void createTroglobiteIce(Game engine, Zombie zombie) {
        int count = zombie.getDefinition().getSpecialPropertyInt(
            "NumberOfIceblocksToSpawnWith", 3);
        int health = zombie.getDefinition().getSpecialPropertyInt(
            "projectIceBlockHealth", 600);
        double spacing = zombie.getDefinition().getSpecialPropertyDouble(
            "projectIceBlockSpacingTiles", 0.9);
        for (int index = 1; index <= count; index++) {
            double column = zombie.getPosition().getColumn() - spacing * index;
            PushedObstacle obstacle = new PushedObstacle(PushedObstacleType.ICE_BLOCK,
                zombie.getRuntimeId(), new BoardPosition(zombie.getPosition().getRow(), column),
                health, zombie.getSpeed());
            engine.board.addPushedObstacle(obstacle);
        }
        engine.addEvent("Troglobite entered with " + count + " independent ice block(s).");
    }

    private static void createArcadeMachine(Game engine, Zombie zombie) {
        int health = zombie.getDefinition().getSpecialPropertyInt(
            "projectMachineHealth", 1290);
        double spacing = zombie.getDefinition().getSpecialPropertyDouble(
            "projectMachineSpacingTiles", 0.9);
        PushedObstacle machine = new PushedObstacle(PushedObstacleType.ARCADE_MACHINE,
            zombie.getRuntimeId(), zombie.getPosition().moveHorizontal(-spacing),
            health, zombie.getSpeed());
        engine.board.addPushedObstacle(machine);
        zombie.activateMachine();
        engine.addEvent("Arcade Zombie deployed an independent arcade machine.");
    }

    private static void createBarrel(Game engine, Zombie zombie) {
        int health = zombie.getDefinition().getSpecialPropertyInt(
            "projectBarrelHealth", 1100);
        double spacing = zombie.getDefinition().getSpecialPropertyDouble(
            "projectBarrelSpacingTiles", 0.9);
        PushedObstacle barrel = new PushedObstacle(PushedObstacleType.BARREL,
            zombie.getRuntimeId(), zombie.getPosition().moveHorizontal(-spacing),
            health, zombie.getSpeed());
        engine.board.addPushedObstacle(barrel);
        engine.addEvent("Barrel Roller Zombie deployed an independent barrel.");
    }

    static void tickPushedObstacles(Game engine) {
        for (PushedObstacle obstacle
            : new ArrayList<>(engine.board.getPushedObstacles())) {
            if (obstacle.isDestroyed()) {
                resolveDestroyedObstacle(engine, obstacle);
                continue;
            }
            Zombie owner = findZombie(engine, obstacle.getOwnerRuntimeId());
            boolean ownerCanPush = owner != null && !owner.isDead()
                && !owner.isHypnotized() && owner.getStunnedTicks() <= 0
                && owner.getPosition() != null
                && owner.getPosition().getRow() == obstacle.getPosition().getRow();
            if (ownerCanPush) {
                movePushedObstacle(engine, obstacle, owner);
            }
            resolveHypnotizedCollision(engine, obstacle);
            if (obstacle.getPosition().getColumn() < -1.0) {
                engine.board.removePushedObstacle(obstacle);
            } else if (obstacle.isDestroyed()) {
                resolveDestroyedObstacle(engine, obstacle);
            }
        }
    }

    private static void movePushedObstacle(Game engine, PushedObstacle obstacle,
                                             Zombie owner) {
        if (obstacle.getType() == PushedObstacleType.BARREL
            && findTouchingPlant(engine, obstacle.getPosition()) != null) {
            return;
        }
        double speedMultiplier = owner.getChilledTicks() > 0 ? 0.5 : 1.0;
        double previous = obstacle.moveOneTick(speedMultiplier);
        double current = obstacle.getPosition().getColumn();
        Plant target = findPlantCrossed(engine, obstacle.getPosition().getRow(),
            previous, current, false);
        if (target == null) {
            return;
        }
        if (obstacle.destroysPlantsOnContact()) {
            target.takeDamage(Math.max(1, target.getHealth()));
            engine.addEvent(obstacle.getType() + " crushed " + target.getName() + ".");
        } else {
            obstacle.stopAt(target.getPosition().getColumn() + 0.82);
        }
    }

    private static void resolveHypnotizedCollision(Game engine,
                                                    PushedObstacle obstacle) {
        for (Zombie zombie : new ArrayList<>(engine.board.getZombiesInRow(
            obstacle.getPosition().getRow()))) {
            if (!zombie.isHypnotized() || zombie.isDead()
                || Math.abs(zombie.getPosition().getColumn()
                    - obstacle.getPosition().getColumn()) > 0.75) {
                continue;
            }
            if (obstacle.getType() == PushedObstacleType.BARREL) {
                if (engine.elapsedTicks % Game.TICKS_PER_SECOND == 0) {
                    obstacle.takeDamage(Math.max(1, zombie.getDamage()));
                    engine.addEvent("A hypnotized zombie damaged the barrel for "
                        + Math.max(1, zombie.getDamage()) + ".");
                }
            } else {
                zombie.kill();
                engine.addEvent(obstacle.getType()
                    + " instantly destroyed a hypnotized zombie.");
            }
        }
    }

    static PushedObstacle firstProjectileObstacle(Game engine, Projectile projectile,
                                                   double fromColumn, double toColumn) {
        if (projectile.isLobbed()) {
            return null;
        }
        return engine.board.findFirstObstacleCrossed(projectile.getPosition().getRow(),
            fromColumn, toColumn);
    }

    static void hitObstacleWithProjectile(Game engine, PushedObstacle obstacle,
                                          Projectile projectile) {
        int damage = projectile.getDamage() * Math.max(1, projectile.getDamageMultiplier());
        obstacle.takeDamage(damage);
        projectile.deactivate();
        engine.addEvent("Projectile from " + projectile.getSourcePlant() + " hit "
            + obstacle.getType() + " for " + damage + " damage.");
        if (obstacle.isDestroyed()) {
            resolveDestroyedObstacle(engine, obstacle);
        }
    }

    private static void resolveDestroyedObstacle(Game engine,
                                                  PushedObstacle obstacle) {
        if (!engine.board.getPushedObstacles().contains(obstacle)) {
            return;
        }
        if (obstacle.getType() == PushedObstacleType.ARCADE_MACHINE) {
            Zombie owner = findZombie(engine, obstacle.getOwnerRuntimeId());
            if (owner != null) {
                owner.breakMachine();
            }
            engine.addEvent("The independent arcade machine was destroyed.");
        } else if (obstacle.getType() == PushedObstacleType.BARREL) {
            spawnBarrelImps(engine, obstacle);
            engine.addEvent("The barrel broke and released its Imp passengers.");
        } else {
            engine.addEvent("A Troglobite ice block was destroyed.");
        }
        engine.board.removePushedObstacle(obstacle);
    }

    private static void spawnBarrelImps(Game engine, PushedObstacle barrel) {
        Zombie owner = findZombie(engine, barrel.getOwnerRuntimeId());
        int count = owner == null ? 2 : owner.getDefinition().getSpecialPropertyInt(
            "projectImpCount", 2);
        for (int index = 0; index < count; index++) {
            Zombie imp = engine.zombieFactory.createZombie("ZombieImp");
            imp.applyDifficulty(engine.difficultyLevel);
            imp.setPosition(new BoardPosition(barrel.getPosition().getRow(),
                barrel.getPosition().getColumn() + index * 0.08));
            engine.board.addZombie(imp);
        }
    }

    static void tickProspectorDynamites(Game engine) {
        for (ProspectorDynamite dynamite
            : new ArrayList<>(engine.board.getProspectorDynamites())) {
            if (!dynamite.isActive()) {
                engine.board.removeProspectorDynamite(dynamite);
                continue;
            }
            Plant touching = findTouchingPlant(engine, dynamite.getPosition());
            if (touching != null) {
                if (engine.elapsedTicks % Game.TICKS_PER_SECOND == 0) {
                    touching.takeDamage(dynamite.getDamagePerSecond());
                    engine.addEvent("Prospector dynamite attacked " + touching.getName()
                        + " from behind.");
                }
                continue;
            }
            double previous = dynamite.moveOneTick();
            double current = dynamite.getPosition().getColumn();
            Plant crossed = findPlantCrossed(engine, dynamite.getPosition().getRow(),
                previous, current, true);
            if (crossed != null) {
                dynamite.stopAt(crossed.getPosition().getColumn() - 0.82);
            }
            if (dynamite.getPosition().getColumn() > engine.board.getCols() + 1.0) {
                dynamite.deactivate();
                engine.board.removeProspectorDynamite(dynamite);
            }
        }
    }

    static void tickReflectedProjectiles(Game engine) {
        for (ReflectedProjectile projectile
            : new ArrayList<>(engine.board.getReflectedProjectiles())) {
            if (!projectile.isActive()) {
                engine.board.removeReflectedProjectile(projectile);
                continue;
            }
            double previous = projectile.moveOneTick();
            Plant target = findPlantCrossed(engine, projectile.getPosition().getRow(),
                previous, projectile.getPosition().getColumn(), false);
            if (target != null) {
                projectile.hitPlant(target);
                engine.addEvent("A reflected " + projectile.getType()
                    + " projectile hit " + target.getName() + ".");
            }
            if (!projectile.isActive() || projectile.getPosition().getColumn() < -1.0) {
                engine.board.removeReflectedProjectile(projectile);
            }
        }
    }

    private static Plant findTouchingPlant(Game engine, BoardPosition position) {
        Plant nearest = null;
        double best = Double.MAX_VALUE;
        for (Plant plant : engine.board.getPlantsInRow(position.getRow())) {
            if (plant.isDestroyed() || plant.getPosition() == null) {
                continue;
            }
            double distance = Math.abs(plant.getPosition().getColumn()
                - position.getColumn());
            if (distance <= 0.82 && distance < best) {
                best = distance;
                nearest = plant;
            }
        }
        return nearest;
    }

    private static Plant findPlantCrossed(Game engine, int row, double fromColumn,
                                          double toColumn, boolean movingRight) {
        Plant nearest = null;
        for (Plant plant : engine.board.getPlantsInRow(row)) {
            if (plant.isDestroyed() || plant.getPosition() == null) {
                continue;
            }
            double column = plant.getPosition().getColumn();
            double low = Math.min(fromColumn, toColumn) - 0.001;
            double high = Math.max(fromColumn, toColumn) + 0.001;
            if (column < low || column > high) {
                continue;
            }
            if (nearest == null || isCloser(column,
                nearest.getPosition().getColumn(), movingRight)) {
                nearest = plant;
            }
        }
        return nearest;
    }

    private static boolean isCloser(double candidate, double current,
                                    boolean movingRight) {
        return movingRight ? candidate < current : candidate > current;
    }

    private static Zombie findZombie(Game engine, String runtimeId) {
        for (Zombie zombie : engine.board.getZombies()) {
            if (zombie.getRuntimeId().equals(runtimeId)) {
                return zombie;
            }
        }
        return null;
    }
}
