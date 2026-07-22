package model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public final class ZombotanySession extends MiniGameSession {
    private final ArrayList<MiniGamePlantUnit> plants = new ArrayList<>();
    private final ArrayList<MiniGameUnit> zombies = new ArrayList<>();
    private final Random random;
    private int sun = 300;
    private int kills;
    private int nextSpawnTick;

    public ZombotanySession(MiniGameDefinition definition, int level) {
        super(definition, level);
        random = new Random(50_000L + level * 1877L);
        nextSpawnTick = 10;
    }

    @Override
    public void execute(String command, List<String> arguments) {
        ensureRunning();
        String normalized = normalize(command);
        switch (normalized) {
            case "plant" -> plant(arg(arguments, 0), intArg(arguments, 1) - 1,
                intArg(arguments, 2) - 1);
            case "advance", "defeatzombie" -> advanceTime(intArg(arguments, 0));
            default -> throw new IllegalArgumentException(
                "Zombotany commands: plant <peashooter|wallnut|sunflower|snowpea> <x> <y>, advance <ticks>.");
        }
    }

    private void plant(String typeText, int col, int row) {
        if (row < 0 || row >= 5 || col < 0 || col >= 9) {
            throw new IllegalArgumentException("Position must be inside the 9x5 board.");
        }
        if (findPlant(row, col) != null) {
            throw new IllegalStateException("That tile already contains a plant.");
        }
        String normalized = normalize(typeText);
        String type;
        int cost;
        int health;
        int damage;
        switch (normalized) {
            case "peashooter" -> { type = "Peashooter"; cost = 100; health = 300; damage = 40; }
            case "wallnut" -> { type = "Wall-nut"; cost = 50; health = 1200; damage = 0; }
            case "sunflower" -> { type = "Sunflower"; cost = 50; health = 250; damage = 0; }
            case "snowpea" -> { type = "Snow Pea"; cost = 150; health = 300; damage = 35; }
            default -> throw new IllegalArgumentException("Unknown Zombotany plant.");
        }
        if (sun < cost) {
            throw new IllegalStateException("Not enough sun.");
        }
        sun -= cost;
        plants.add(new MiniGamePlantUnit(type, row, col, health, damage));
    }

    @Override
    protected void onTick() {
        if (getElapsedTicks() >= nextSpawnTick) {
            spawnZombie();
            nextSpawnTick += Math.max(15, 35 - getLevel() * 4);
        }
        tickPlants();
        Iterator<MiniGameUnit> iterator = zombies.iterator();
        while (iterator.hasNext()) {
            MiniGameUnit zombie = iterator.next();
            zombie.tickAge();
            if (zombie.isDead()) {
                iterator.remove();
                kills++;
                addScore(175);
                if (kills >= getTarget()) {
                    win();
                    addScore(1500 + sun);
                    return;
                }
                continue;
            }
            applyZombiePlantPower(zombie);
            MiniGamePlantUnit blocker = blockingPlant(zombie);
            if (blocker != null) {
                if (zombie.getType().equals("Squash Zombie")) {
                    blocker.damage(blocker.getHealth());
                    zombie.damage(zombie.getHealth());
                } else {
                    blocker.damage(Math.max(1, zombie.getDamage() / 10));
                }
            } else {
                zombie.setColumn(zombie.getColumn() - zombie.getSpeed());
                if (zombie.getColumn() < -0.1) {
                    lose();
                    return;
                }
            }
        }
        plants.removeIf(MiniGamePlantUnit::isDead);
    }

    private void tickPlants() {
        for (MiniGamePlantUnit plant : plants) {
            plant.tick();
            if (plant.getType().equals("Sunflower") && getElapsedTicks() % 50 == 0) {
                sun += 25;
            }
            if (plant.getDamage() > 0 && plant.ready()) {
                MiniGameUnit target = nearestZombie(plant.getRow(), plant.getColumn());
                if (target != null) {
                    target.damage(plant.getDamage());
                    plant.setCooldown(10);
                }
            }
        }
    }

    private void spawnZombie() {
        String[] types = {"Peashooter Zombie", "Wall-nut Zombie", "Jalapeno Zombie", "Squash Zombie"};
        String type = types[random.nextInt(types.length)];
        int health = switch (type) {
            case "Wall-nut Zombie" -> 1200;
            case "Squash Zombie" -> 240;
            default -> 420 + getLevel() * 80;
        };
        double speed = type.equals("Squash Zombie") ? 0.10 : type.equals("Wall-nut Zombie") ? 0.02 : 0.04;
        zombies.add(new MiniGameUnit(type, random.nextInt(5), 8.8, health, 40, speed));
    }

    private void applyZombiePlantPower(MiniGameUnit zombie) {
        if (zombie.getType().equals("Peashooter Zombie") && zombie.getAgeTicks() % 12 == 0) {
            MiniGamePlantUnit target = nearestPlantToLeft(zombie);
            if (target != null) {
                target.damage(35 + getLevel() * 5);
            }
        }
        if (zombie.getType().equals("Jalapeno Zombie") && zombie.getAgeTicks() == 100) {
            for (MiniGamePlantUnit plant : plants) {
                if (plant.getRow() == zombie.getRow()) {
                    plant.damage(plant.getHealth());
                }
            }
            zombie.damage(zombie.getHealth());
        }
    }

    private MiniGamePlantUnit nearestPlantToLeft(MiniGameUnit zombie) {
        MiniGamePlantUnit result = null;
        for (MiniGamePlantUnit plant : plants) {
            if (plant.isDead() || plant.getRow() != zombie.getRow()
                || plant.getColumn() > zombie.getColumn()) {
                continue;
            }
            if (result == null || plant.getColumn() > result.getColumn()) {
                result = plant;
            }
        }
        return result;
    }

    private MiniGamePlantUnit blockingPlant(MiniGameUnit zombie) {
        for (MiniGamePlantUnit plant : plants) {
            if (!plant.isDead() && plant.getRow() == zombie.getRow()
                && zombie.getColumn() <= plant.getColumn() + 0.75
                && zombie.getColumn() >= plant.getColumn() - 0.05) {
                return plant;
            }
        }
        return null;
    }

    private MiniGameUnit nearestZombie(int row, int column) {
        MiniGameUnit result = null;
        for (MiniGameUnit zombie : zombies) {
            if (!zombie.isDead() && zombie.getRow() == row && zombie.getColumn() >= column
                && (result == null || zombie.getColumn() < result.getColumn())) {
                result = zombie;
            }
        }
        return result;
    }

    private MiniGamePlantUnit findPlant(int row, int col) {
        for (MiniGamePlantUnit plant : plants) {
            if (!plant.isDead() && plant.getRow() == row && plant.getColumn() == col) {
                return plant;
            }
        }
        return null;
    }

    @Override
    protected String progressText() {
        return "kills=" + kills + "/" + getTarget() + ", sun=" + sun
            + ", plants=" + plants.size() + ", zombies=" + zombies.size();
    }

    @Override
    public String boardView() {
        StringBuilder builder = new StringBuilder("Zombotany (P=plant, Z=plant-powered zombie)\n");
        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 9; col++) {
                char symbol = findPlant(row, col) == null ? '.' : 'P';
                for (MiniGameUnit zombie : zombies) {
                    if (!zombie.isDead() && zombie.getRow() == row
                        && Math.round(zombie.getColumn()) == col) {
                        symbol = 'Z';
                    }
                }
                builder.append(symbol).append(' ');
            }
            builder.append('\n');
        }
        builder.append("Sun=").append(sun).append(", kills=").append(kills)
            .append('/').append(getTarget());
        return builder.toString();
    }

    private String arg(List<String> args, int index) {
        if (args == null || index >= args.size()) {
            throw new IllegalArgumentException("Missing command argument.");
        }
        return args.get(index);
    }

    private int intArg(List<String> args, int index) {
        try { return Integer.parseInt(arg(args, index)); }
        catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Expected an integer argument.");
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase().replace("-", "").replace("_", "")
            .replace(" ", "");
    }
}
