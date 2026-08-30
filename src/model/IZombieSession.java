package model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class IZombieSession extends MiniGameSession {
    private record ZombieCard(String type, int cost, int health, int damage, double speed) { }

    private final ArrayList<MiniGamePlantUnit> plants = new ArrayList<>();
    private final ArrayList<MiniGameUnit> zombies = new ArrayList<>();
    private final LinkedHashMap<String, ZombieCard> cards = new LinkedHashMap<>();
    private final boolean[] brains = {true, true, true, true, true};
    private final Random random;
    private final boolean multiplayer;
    private int sun = 150;
    private int plantSun = 500;
    private int brainsEaten;
    private boolean plantVictory;

    public IZombieSession(MiniGameDefinition definition, int level) {
        this(definition, level, false);
    }

    public IZombieSession(MiniGameDefinition definition, int level, boolean multiplayer) {
        super(definition, level);
        this.multiplayer = multiplayer;
        random = new Random(30_000L + level * 2017L);
        registerCards();
        if (!multiplayer) {
            createGarden();
        }
        createSunProducers();
    }

    @Override
    public void execute(String command, List<String> arguments) {
        ensureRunning();
        String normalized = normalize(command);
        switch (normalized) {
            case "deploy", "placezombie" -> deploy(arg(arguments, 0),
                intArg(arguments, 1) - 1);
            case "plant", "placeplant" -> placePlant(arg(arguments, 0),
                intArg(arguments, 1) - 1, intArg(arguments, 2) - 1);
            case "removeplant" -> removePlant(intArg(arguments, 0) - 1,
                intArg(arguments, 1) - 1);
            case "advance" -> advanceTime(intArg(arguments, 0));
            default -> throw new IllegalArgumentException(
                "I, Zombie commands: deploy <card> <row>, plant <type> <row> <column>, "
                    + "removeplant <row> <column>, advance <ticks>. "
                    + "Available cards: " + String.join(", ", cards.keySet()) + ".");
        }
    }

    private void registerCards() {
        if (getLevel() == 1) {
            addCard("basic", "Basic", 50, 260, 35, 0.045);
            addCard("cone", "Conehead", 75, 520, 40, 0.040);
            addCard("bucket", "Buckethead", 125, 1000, 45, 0.032);
            addCard("imp", "Imp", 25, 140, 25, 0.070);
            addCard("allstar", "All-Star", 150, 700, 1000, 0.090);
        } else if (getLevel() == 2) {
            addCard("newspaper", "Newspaper", 65, 430, 55, 0.045);
            addCard("prospector", "Prospector", 90, 500, 45, 0.055);
            addCard("parasol", "Parasol", 100, 650, 45, 0.040);
            addCard("ra", "Ra", 80, 480, 40, 0.042);
            addCard("explorer", "Explorer", 120, 720, 120, 0.042);
        } else {
            addCard("knight", "Knight", 175, 1800, 55, 0.030);
            addCard("blockhead", "Blockhead", 200, 2300, 55, 0.027);
            addCard("gargantuar", "Gargantuar", 250, 3000, 1000, 0.018);
            addCard("dodo", "Dodo Rider", 110, 620, 45, 0.070);
            addCard("wizard", "Wizard", 150, 850, 50, 0.032);
        }
    }

    private void addCard(String key, String type, int cost, int health,
                         int damage, double speed) {
        cards.put(key, new ZombieCard(type, cost, health, damage, speed));
    }

    private void createGarden() {
        String[] types = {"Peashooter", "Wall-nut", "Snow Pea", "Repeater",
            "Cabbage-pult"};
        for (int row = 0; row < 5; row++) {
            int count = 2 + getLevel();
            int placed = 0;
            while (placed < count) {
                int column = random.nextInt(5);
                if (findPlant(row, column) != null) {
                    continue;
                }
                String type = types[random.nextInt(types.length)];
                int health = type.equals("Wall-nut") ? 900 : 260;
                int damage = type.equals("Repeater") ? 55
                    : type.equals("Snow Pea") ? 30 : 35;
                plants.add(new MiniGamePlantUnit(type, row, column, health, damage));
                placed++;
            }
        }
    }

    private void createSunProducers() {
        for (int row = 0; row < 5; row++) {
            MiniGameUnit producer = new MiniGameUnit("Sun Producer Zombie", row, 8.7,
                900, 20, 0.012);
            producer.setCooldown(productionInterval(producer));
            zombies.add(producer);
        }
    }

    private void deploy(String cardName, int row) {
        if (row < 0 || row >= 5) {
            throw new IllegalArgumentException("Row must be between 1 and 5.");
        }
        ZombieCard card = cards.get(normalize(cardName));
        if (card == null) {
            throw new IllegalArgumentException("Unknown zombie card for this level. "
                + "Available: " + String.join(", ", cards.keySet()) + ".");
        }
        if (sun < card.cost()) {
            throw new IllegalStateException("Not enough sun. Required: " + card.cost() + ".");
        }
        sun -= card.cost();
        zombies.add(new MiniGameUnit(card.type(), row, 8.8, card.health(),
            card.damage(), card.speed()));
        addScore(10);
    }

    private void placePlant(String plantName, int row, int column) {
        if (!multiplayer) {
            throw new IllegalStateException("Plant placement is available in multiplayer mode.");
        }
        validateCell(row, column);
        if (findPlant(row, column) != null) {
            throw new IllegalStateException("That tile already contains a plant.");
        }
        PlantCard card = plantCard(plantName);
        if (plantSun < card.cost()) {
            throw new IllegalStateException("Not enough plant sun. Required: " + card.cost() + ".");
        }
        plantSun -= card.cost();
        plants.add(new MiniGamePlantUnit(card.type(), row, column, card.health(), card.damage()));
    }

    private void removePlant(int row, int column) {
        if (!multiplayer) {
            throw new IllegalStateException("Plant removal is available in multiplayer mode.");
        }
        validateCell(row, column);
        if (!plants.removeIf(plant -> !plant.isDead() && plant.getRow() == row
            && plant.getColumn() == column)) {
            throw new IllegalStateException("No plant exists on that tile.");
        }
    }

    private PlantCard plantCard(String name) {
        return switch (normalize(name)) {
            case "sunflower" -> new PlantCard("Sunflower", 50, 220, 0);
            case "peashooter" -> new PlantCard("Peashooter", 100, 300, 35);
            case "wallnut" -> new PlantCard("Wall-nut", 50, 1000, 0);
            case "snowpea" -> new PlantCard("Snow Pea", 175, 300, 30);
            case "repeater" -> new PlantCard("Repeater", 200, 320, 55);
            case "cabbagepult" -> new PlantCard("Cabbage-pult", 100, 300, 40);
            default -> throw new IllegalArgumentException(
                "Plant must be sunflower, peashooter, wallnut, snowpea, repeater, or cabbagepult.");
        };
    }

    @Override
    protected void onTick() {
        if (multiplayer && getElapsedTicks() % (5 * Game.TICKS_PER_SECOND) == 0) {
            plantSun += 25;
        }
        tickPlants();
        Iterator<MiniGameUnit> iterator = zombies.iterator();
        while (iterator.hasNext()) {
            MiniGameUnit zombie = iterator.next();
            zombie.tickAge();
            if (zombie.isDead()) {
                iterator.remove();
                continue;
            }
            produceSunIfReady(zombie);
            MiniGamePlantUnit blocker = blockingPlant(zombie);
            if (blocker != null) {
                attackPlant(zombie, blocker);
            } else {
                moveTowardBrain(zombie, iterator);
            }
            if (isWon()) {
                return;
            }
        }
        plants.removeIf(MiniGamePlantUnit::isDead);
        if (multiplayer) {
            if (getElapsedTicks() >= 120 * Game.TICKS_PER_SECOND && brainsEaten < brains.length) {
                plantVictory = true;
                win();
            }
        } else {
            evaluateLoss();
        }
    }

    private void produceSunIfReady(MiniGameUnit zombie) {
        if (!zombie.getType().equals("Sun Producer Zombie") || !zombie.ready()) {
            return;
        }
        sun += 25;
        zombie.setCooldown(productionInterval(zombie));
        addScore(5);
    }

    private int productionInterval(MiniGameUnit producer) {
        int elapsedSeconds = producer.getAgeTicks() / Game.TICKS_PER_SECOND;
        int acceleration = elapsedSeconds / 10;
        return Math.max(15, 80 - getLevel() * 5 - acceleration * 5);
    }

    private void attackPlant(MiniGameUnit zombie, MiniGamePlantUnit blocker) {
        if (!zombie.ready()) {
            return;
        }
        int damage = zombie.getType().equals("All-Star") ? blocker.getHealth()
            : Math.max(1, zombie.getDamage());
        blocker.damage(damage);
        zombie.setCooldown(Game.TICKS_PER_SECOND);
        if (blocker.isDead()) {
            addScore(75);
        }
    }

    private void moveTowardBrain(MiniGameUnit zombie, Iterator<MiniGameUnit> iterator) {
        if (zombie.getType().equals("Sun Producer Zombie")) {
            return;
        }
        zombie.setColumn(zombie.getColumn() - zombie.getSpeed());
        if (zombie.getColumn() < -0.1) {
            if (brains[zombie.getRow()]) {
                brains[zombie.getRow()] = false;
                brainsEaten++;
                addScore(500);
            }
            iterator.remove();
            if (brainsEaten == 5) {
                win();
                addScore(1000 + sun);
            }
        }
    }

    private void tickPlants() {
        for (MiniGamePlantUnit plant : plants) {
            plant.tick();
            if (multiplayer && plant.getType().equals("Sunflower") && plant.ready()) {
                plantSun += 25;
                plant.setCooldown(5 * Game.TICKS_PER_SECOND);
                continue;
            }
            if (!plant.ready()) {
                continue;
            }
            MiniGameUnit target = nearestZombieToRight(plant);
            if (target != null) {
                target.damage(plant.getDamage());
                plant.setCooldown(Game.TICKS_PER_SECOND);
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
            // Sun producers are stationary resource sources, not attack targets.
            if (zombie.getType().equals("Sun Producer Zombie")) {
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
                && zombie.getColumn() >= plant.getColumn() - 0.05
                && (result == null || plant.getColumn() > result.getColumn())) {
                result = plant;
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
        return "brains=" + brainsEaten + "/5, sun=" + sun + ", plants="
            + plants.size() + ", zombies=" + zombies.size();
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
                        symbol = zombie.getType().equals("Sun Producer Zombie") ? 'S' : 'Z';
                    }
                }
                builder.append(symbol).append(' ');
            }
            builder.append('\n');
        }
        builder.append("Sun: ").append(sun).append(" | Level ").append(getLevel())
            .append(multiplayer ? " | Plant sun: " + plantSun : "")
            .append(" cards: ");
        for (Map.Entry<String, ZombieCard> entry : cards.entrySet()) {
            builder.append(entry.getKey()).append('(').append(entry.getValue().cost())
                .append(") ");
        }
        return builder.toString().stripTrailing();
    }


    public record ZombieCardView(
        String key,
        String type,
        int cost,
        int health,
        int damage,
        double speed
    ) { }

    public List<ZombieCardView> getCardViews() {
        ArrayList<ZombieCardView> result = new ArrayList<>();
        for (Map.Entry<String, ZombieCard> entry : cards.entrySet()) {
            ZombieCard card = entry.getValue();
            result.add(new ZombieCardView(
                entry.getKey(), card.type(), card.cost(), card.health(),
                card.damage(), card.speed()
            ));
        }
        return List.copyOf(result);
    }

    public List<MiniGamePlantSnapshot> getPlantViews() {
        return plants.stream().map(MiniGamePlantUnit::snapshot).toList();
    }

    public List<MiniGameUnitSnapshot> getZombieViews() {
        return zombies.stream().map(MiniGameUnit::snapshot).toList();
    }

    public boolean[] getBrains() {
        return brains.clone();
    }

    public int getSun() {
        return sun;
    }

    public int getPlantSun() {
        return plantSun;
    }

    public int getBrainsEaten() {
        return brainsEaten;
    }

    public boolean isMultiplayer() {
        return multiplayer;
    }

    public boolean isPlantVictory() {
        return plantVictory;
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
        return value == null ? "" : value.toLowerCase().replace("-", "")
            .replace("_", "").replace(" ", "");
    }

    private void validateCell(int row, int column) {
        if (row < 0 || row >= 5 || column < 0 || column >= 9) {
            throw new IllegalArgumentException("Row must be 1-5 and column must be 1-9.");
        }
    }

    private record PlantCard(String type, int cost, int health, int damage) { }

    List<String> cardKeysForTest() { return List.copyOf(cards.keySet()); }
    int sunForTest() { return sun; }
    void clearPlantsForTest() { plants.clear(); }
}
