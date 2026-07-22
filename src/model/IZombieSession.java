package model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class IZombieSession extends MiniGameSession {
    private record ZombieCard(String type, int cost, int health, int damage, double speed) { }

    private final ArrayList<MiniGamePlantUnit> plants = new ArrayList<>();
    private final ArrayList<MiniGameUnit> zombies = new ArrayList<>();
    private final LinkedHashMap<String, ZombieCard> cards = new LinkedHashMap<>();
    private final boolean[] brains = {true, true, true, true, true};
    private final Random random;
    private int sun = 150;
    private int brainsEaten;

    public IZombieSession(MiniGameDefinition definition, int level) {
        super(definition, level);
        random = new Random(30_000L + level * 2017L);
        registerCards();
        createGarden();
        createSunProducers();
    }

    @Override
    public void execute(String command, List<String> arguments) {
        ensureRunning();
        String normalized = normalize(command);
        switch (normalized) {
            case "deploy", "placezombie" -> deploy(arg(arguments, 0), intArg(arguments, 1) - 1);
            case "advance" -> advanceTime(intArg(arguments, 0));
            default -> throw new IllegalArgumentException(
                "I, Zombie commands: deploy <basic|cone|bucket|imp|allstar> <row>, advance <ticks>.");
        }
    }

    private void registerCards() {
        cards.put("basic", new ZombieCard("Basic", 50, 260, 35, 0.045));
        cards.put("cone", new ZombieCard("Conehead", 75, 520, 40, 0.04));
        cards.put("bucket", new ZombieCard("Buckethead", 125, 1000, 45, 0.032));
        cards.put("imp", new ZombieCard("Imp", 25, 140, 25, 0.07));
        cards.put("allstar", new ZombieCard("All-Star", 150, 700, 1000, 0.09));
    }

    private void createGarden() {
        String[] types = {"Peashooter", "Wall-nut", "Snow Pea", "Repeater", "Cabbage-pult"};
        for (int row = 0; row < 5; row++) {
            int count = 2 + getLevel();
            for (int index = 0; index < count; index++) {
                int column = random.nextInt(5);
                if (findPlant(row, column) != null) {
                    continue;
                }
                String type = types[random.nextInt(types.length)];
                int health = type.equals("Wall-nut") ? 900 : 260;
                int damage = type.equals("Repeater") ? 55 : type.equals("Snow Pea") ? 30 : 35;
                plants.add(new MiniGamePlantUnit(type, row, column, health, damage));
            }
        }
    }

    private void createSunProducers() {
        for (int row = 0; row < 5; row++) {
            MiniGameUnit producer = new MiniGameUnit("Sun Producer Zombie", row, 8.7,
                900, 20, 0.012);
            producer.setCooldown(40);
            zombies.add(producer);
        }
    }

    private void deploy(String cardName, int row) {
        if (row < 0 || row >= 5) {
            throw new IllegalArgumentException("Row must be between 1 and 5.");
        }
        ZombieCard card = cards.get(normalize(cardName));
        if (card == null) {
            throw new IllegalArgumentException("Unknown zombie card.");
        }
        if (sun < card.cost()) {
            throw new IllegalStateException("Not enough sun. Required: " + card.cost() + ".");
        }
        sun -= card.cost();
        zombies.add(new MiniGameUnit(card.type(), row, 8.8, card.health(), card.damage(), card.speed()));
        addScore(10);
    }

    @Override
    protected void onTick() {
        tickPlants();
        Iterator<MiniGameUnit> iterator = zombies.iterator();
        while (iterator.hasNext()) {
            MiniGameUnit zombie = iterator.next();
            zombie.tickAge();
            if (zombie.isDead()) {
                iterator.remove();
                continue;
            }
            if (zombie.getType().equals("Sun Producer Zombie")
                && zombie.getAgeTicks() % Math.max(20, 60 - getLevel() * 10) == 0) {
                sun += 25;
            }
            MiniGamePlantUnit blocker = blockingPlant(zombie);
            if (blocker != null) {
                int damage = zombie.getType().equals("All-Star") ? blocker.getHealth()
                    : Math.max(1, zombie.getDamage() / 10);
                blocker.damage(damage);
                if (blocker.isDead()) {
                    addScore(75);
                }
            } else {
                zombie.setColumn(zombie.getColumn() - zombie.getSpeed());
                if (zombie.getColumn() < -0.1 && brains[zombie.getRow()]) {
                    brains[zombie.getRow()] = false;
                    brainsEaten++;
                    addScore(500);
                    iterator.remove();
                    if (brainsEaten == 5) {
                        win();
                        addScore(1000 + sun);
                        return;
                    }
                }
            }
        }
        plants.removeIf(MiniGamePlantUnit::isDead);
        evaluateLoss();
    }

    private void tickPlants() {
        for (MiniGamePlantUnit plant : plants) {
            plant.tick();
            if (!plant.ready()) {
                continue;
            }
            MiniGameUnit target = nearestZombieToRight(plant);
            if (target != null) {
                target.damage(plant.getDamage());
                plant.setCooldown(10);
            }
        }
    }

    private MiniGameUnit nearestZombieToRight(MiniGamePlantUnit plant) {
        MiniGameUnit result = null;
        for (MiniGameUnit zombie : zombies) {
            if (zombie.isDead() || zombie.getRow() != plant.getRow()
                || zombie.getColumn() < plant.getColumn()) {
                continue;
            }
            if (result == null || zombie.getColumn() < result.getColumn()) {
                result = zombie;
            }
        }
        return result;
    }

    private MiniGamePlantUnit blockingPlant(MiniGameUnit zombie) {
        MiniGamePlantUnit result = null;
        for (MiniGamePlantUnit plant : plants) {
            if (plant.isDead() || plant.getRow() != zombie.getRow()) {
                continue;
            }
            if (zombie.getColumn() <= plant.getColumn() + 0.75
                && zombie.getColumn() >= plant.getColumn() - 0.05) {
                if (result == null || plant.getColumn() > result.getColumn()) {
                    result = plant;
                }
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

    private void evaluateLoss() {
        boolean activeAttacker = zombies.stream().anyMatch(zombie -> !zombie.isDead()
            && !zombie.getType().equals("Sun Producer Zombie"));
        int cheapest = cards.values().stream().mapToInt(ZombieCard::cost).min().orElse(25);
        boolean producerAlive = zombies.stream().anyMatch(zombie -> !zombie.isDead()
            && zombie.getType().equals("Sun Producer Zombie"));
        if (!activeAttacker && sun < cheapest && !producerAlive) {
            lose();
        }
    }

    @Override
    protected String progressText() {
        return "brains=" + brainsEaten + "/5, sun=" + sun + ", plants=" + plants.size()
            + ", zombies=" + zombies.size();
    }

    @Override
    public String boardView() {
        StringBuilder builder = new StringBuilder("I, Zombie (P=plant, Z=zombie, B=brain)\n");
        for (int row = 0; row < 5; row++) {
            builder.append(brains[row] ? 'B' : 'x').append(" | ");
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
        builder.append("Sun: ").append(sun).append(" | Cards: ");
        for (Map.Entry<String, ZombieCard> entry : cards.entrySet()) {
            builder.append(entry.getKey()).append('(').append(entry.getValue().cost()).append(") ");
        }
        return builder.toString().stripTrailing();
    }

    private String arg(List<String> args, int index) {
        if (args == null || index >= args.size()) {
            throw new IllegalArgumentException("Missing command argument.");
        }
        return args.get(index);
    }

    private int intArg(List<String> args, int index) {
        try {
            return Integer.parseInt(arg(args, index));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Expected an integer argument.");
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase().replace("-", "").replace("_", "")
            .replace(" ", "");
    }
}
