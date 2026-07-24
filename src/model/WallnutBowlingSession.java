package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public final class WallnutBowlingSession extends MiniGameSession {
    private enum NutType { NORMAL, EXPLOSIVE, GIANT }

    private static final class RollingNut {
        private final NutType type;
        private final Set<MiniGameUnit> hitZombies = Collections.newSetFromMap(
            new IdentityHashMap<>());
        private double row;
        private double column;
        private int verticalDirection;
        private int hitCount;
        private boolean active = true;

        RollingNut(NutType type, int row) {
            this.type = type;
            this.row = row;
            column = 2.0;
        }
    }

    private static final double NUT_SPEED = 0.15;
    private static final double DIAGONAL_SPEED = 0.10;
    private final ArrayList<MiniGameUnit> zombies = new ArrayList<>();
    private final ArrayList<RollingNut> rollingNuts = new ArrayList<>();
    private final LinkedHashMap<NutType, Integer> conveyor = new LinkedHashMap<>();
    private final Random random;
    private final int normalImpactDamage;
    private int kills;
    private int nextCardTick;

    public WallnutBowlingSession(MiniGameDefinition definition, int level) {
        super(definition, level);
        random = new Random(20_000L + level * 3571L);
        normalImpactDamage = MiniGameData.zombieFactory().findDefinition("Basic Zombie")
            .map(ZombieDefinition::getHitpoints).orElse(300);
        for (NutType type : NutType.values()) {
            conveyor.put(type, 0);
        }
        addCard();
        spawnInitialZombies();
        nextCardTick = 40;
    }

    @Override
    public void execute(String command, List<String> arguments) {
        ensureRunning();
        String normalized = normalize(command);
        switch (normalized) {
            case "bowl", "rollwallnut" -> bowl(parseNut(arg(arguments, 0)),
                intArg(arguments, 1) - 1);
            case "advance" -> advanceTime(intArg(arguments, 0));
            default -> throw new IllegalArgumentException(
                "Wall-nut Bowling commands: bowl <normal|explosive|giant> <row>, "
                    + "advance <ticks>.");
        }
    }

    private void spawnInitialZombies() {
        int count = getTarget() + 5;
        for (int index = 0; index < count; index++) {
            int row = random.nextInt(5);
            double column = 5.0 + random.nextDouble() * 4.0 + index * 0.08;
            int health = 180 + getLevel() * 45 + random.nextInt(100);
            zombies.add(new MiniGameUnit("Bowling Zombie", row, column, health,
                35 + getLevel() * 5, 0.018 + getLevel() * 0.003));
        }
    }

    private void bowl(NutType type, int row) {
        if (row < 0 || row >= 5) {
            throw new IllegalArgumentException("Row must be between 1 and 5.");
        }
        int count = conveyor.get(type);
        if (count <= 0) {
            throw new IllegalStateException("No " + type.name().toLowerCase()
                + " nut is ready.");
        }
        conveyor.put(type, count - 1);
        rollingNuts.add(new RollingNut(type, row));
        addScore(20);
    }

    @Override
    protected void onTick() {
        addConveyorCardIfNeeded();
        moveNutsAndResolveCollisions();
        moveZombies();
        cleanupKills();
        rollingNuts.removeIf(nut -> !nut.active || nut.column > 9.6);
        if (kills >= getTarget()) {
            win();
            addScore(1000);
        }
    }

    private void addConveyorCardIfNeeded() {
        if (getElapsedTicks() >= nextCardTick) {
            addCard();
            nextCardTick += Math.max(25, 50 - getLevel() * 5);
        }
    }

    private void moveNutsAndResolveCollisions() {
        for (RollingNut nut : rollingNuts) {
            if (!nut.active) {
                continue;
            }
            nut.column += NUT_SPEED;
            nut.row += nut.verticalDirection * DIAGONAL_SPEED;
            bounceAtBoardEdge(nut);
            resolveNutCollisions(nut);
        }
    }

    private void bounceAtBoardEdge(RollingNut nut) {
        if (nut.row < 0) {
            nut.row = -nut.row;
            nut.verticalDirection = 1;
        } else if (nut.row > 4) {
            nut.row = 8 - nut.row;
            nut.verticalDirection = -1;
        }
    }

    private void resolveNutCollisions(RollingNut nut) {
        for (MiniGameUnit zombie : zombies) {
            if (!nut.active || zombie.isDead() || nut.hitZombies.contains(zombie)) {
                continue;
            }
            if (Math.abs(zombie.getColumn() - nut.column) > 0.35
                || Math.abs(zombie.getRow() - nut.row) > 0.45) {
                continue;
            }
            nut.hitZombies.add(zombie);
            switch (nut.type) {
                case NORMAL -> hitWithNormalNut(nut, zombie);
                case EXPLOSIVE -> explodeNut(nut, zombie);
                case GIANT -> crushWithGiantNut(zombie);
            }
        }
    }

    private void hitWithNormalNut(RollingNut nut, MiniGameUnit zombie) {
        zombie.damage(normalImpactDamage);
        nut.hitCount++;
        if (nut.hitCount == 1) {
            nut.verticalDirection = chooseFirstBounceDirection(nut.row);
        } else {
            nut.verticalDirection = nut.verticalDirection == 0 ? 1 : -nut.verticalDirection;
        }
        addScore(120 + nut.hitCount * 25);
    }

    private int chooseFirstBounceDirection(double row) {
        if (row <= 0.2) {
            return 1;
        }
        if (row >= 3.8) {
            return -1;
        }
        return random.nextBoolean() ? 1 : -1;
    }

    private void explodeNut(RollingNut nut, MiniGameUnit impact) {
        int hitCount = 0;
        for (MiniGameUnit zombie : zombies) {
            if (!zombie.isDead() && Math.abs(zombie.getRow() - impact.getRow()) <= 1
                && Math.abs(zombie.getColumn() - impact.getColumn()) <= 1.0) {
                zombie.damage(650 + getLevel() * 100);
                hitCount++;
            }
        }
        nut.active = false;
        addScore(250 + hitCount * 100);
    }

    private void crushWithGiantNut(MiniGameUnit zombie) {
        zombie.damage(zombie.getHealth());
        addScore(200);
    }

    private void moveZombies() {
        for (MiniGameUnit zombie : zombies) {
            if (zombie.isDead()) {
                continue;
            }
            zombie.tickAge();
            zombie.setColumn(zombie.getColumn() - zombie.getSpeed());
            if (zombie.getColumn() < -0.1) {
                lose();
                return;
            }
        }
    }

    private void cleanupKills() {
        Iterator<MiniGameUnit> iterator = zombies.iterator();
        while (iterator.hasNext()) {
            MiniGameUnit zombie = iterator.next();
            if (zombie.isDead()) {
                iterator.remove();
                kills++;
                addScore(125);
            }
        }
    }

    private void addCard() {
        int roll = random.nextInt(100);
        NutType type = roll < 65 ? NutType.NORMAL : roll < 90
            ? NutType.EXPLOSIVE : NutType.GIANT;
        conveyor.put(type, conveyor.get(type) + 1);
    }

    @Override
    protected String progressText() {
        return "kills=" + kills + "/" + getTarget() + ", activeZombies="
            + zombies.size() + ", rollingNuts=" + rollingNuts.size()
            + ", conveyor=" + conveyor;
    }

    @Override
    public String boardView() {
        StringBuilder builder = new StringBuilder(
            "Wall-nut Bowling (Z=zombie, N=normal, E=explosive, G=giant; red line after column 3)\n");
        for (int row = 0; row < 5; row++) {
            builder.append(row + 1).append(" | ");
            for (int col = 0; col < 9; col++) {
                char symbol = symbolAt(row, col);
                builder.append(symbol).append(col == 2 ? " || " : " ");
            }
            builder.append('\n');
        }
        builder.append("Conveyor: ");
        for (Map.Entry<NutType, Integer> entry : conveyor.entrySet()) {
            builder.append(entry.getKey()).append('=').append(entry.getValue()).append(' ');
        }
        return builder.toString().stripTrailing();
    }

    private char symbolAt(int row, int col) {
        for (RollingNut nut : rollingNuts) {
            if (nut.active && Math.round(nut.row) == row && Math.round(nut.column) == col) {
                return switch (nut.type) {
                    case NORMAL -> 'N';
                    case EXPLOSIVE -> 'E';
                    case GIANT -> 'G';
                };
            }
        }
        for (MiniGameUnit zombie : zombies) {
            if (!zombie.isDead() && zombie.getRow() == row
                && Math.round(zombie.getColumn()) == col) {
                return 'Z';
            }
        }
        return '.';
    }

    private NutType parseNut(String text) {
        try {
            return NutType.valueOf(text.trim().toUpperCase());
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Nut type must be normal, explosive, or giant.");
        }
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
            .replace("_", "");
    }

    void grantNutForTest(String type) {
        NutType nutType = parseNut(type);
        conveyor.put(nutType, conveyor.get(nutType) + 1);
    }

    void clearZombiesForTest() { zombies.clear(); }

    void addZombieForTest(int row, double column, int health) {
        zombies.add(new MiniGameUnit("Test Zombie", row, column, health, 30, 0));
    }

    int rollingNutCountForTest() { return rollingNuts.size(); }
    int zombieCountForTest() { return zombies.size(); }
    int firstZombieHealthForTest() {
        return zombies.isEmpty() ? 0 : zombies.get(0).getHealth();
    }
}
