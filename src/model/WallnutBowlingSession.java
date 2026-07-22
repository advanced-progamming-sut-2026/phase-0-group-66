package model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class WallnutBowlingSession extends MiniGameSession {
    private enum NutType { NORMAL, EXPLOSIVE, GIANT }

    private final ArrayList<MiniGameUnit> zombies = new ArrayList<>();
    private final LinkedHashMap<NutType, Integer> conveyor = new LinkedHashMap<>();
    private final Random random;
    private int kills;
    private int nextCardTick;

    public WallnutBowlingSession(MiniGameDefinition definition, int level) {
        super(definition, level);
        random = new Random(20_000L + level * 3571L);
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
                "Wall-nut Bowling commands: bowl <normal|explosive|giant> <row>, advance <ticks>.");
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
            throw new IllegalStateException("No " + type.name().toLowerCase() + " nut is ready.");
        }
        conveyor.put(type, count - 1);
        switch (type) {
            case NORMAL -> rollNormal(row);
            case EXPLOSIVE -> rollExplosive(row);
            case GIANT -> rollGiant(row);
        }
        cleanupKills();
        if (kills >= getTarget()) {
            win();
            addScore(1000);
        }
    }

    private void rollNormal(int startingRow) {
        int row = startingRow;
        int hits = 0;
        while (hits < 5) {
            MiniGameUnit target = nearestInRow(row);
            if (target == null) {
                break;
            }
            target.damage(220 + getLevel() * 35);
            hits++;
            addScore(100 + hits * 20);
            int direction = hits % 2 == 1 ? 1 : -1;
            row = Math.max(0, Math.min(4, row + direction));
        }
    }

    private void rollExplosive(int row) {
        MiniGameUnit target = nearestInRow(row);
        if (target == null) {
            addScore(10);
            return;
        }
        double center = target.getColumn();
        int hitCount = 0;
        for (MiniGameUnit zombie : zombies) {
            if (!zombie.isDead() && Math.abs(zombie.getRow() - row) <= 1
                && Math.abs(zombie.getColumn() - center) <= 1.2) {
                zombie.damage(650 + getLevel() * 100);
                hitCount++;
            }
        }
        addScore(250 + hitCount * 100);
    }

    private void rollGiant(int row) {
        int hitCount = 0;
        for (MiniGameUnit zombie : zombies) {
            if (!zombie.isDead() && zombie.getRow() == row) {
                zombie.damage(zombie.getHealth());
                hitCount++;
            }
        }
        addScore(hitCount * 180);
    }

    @Override
    protected void onTick() {
        if (getElapsedTicks() >= nextCardTick) {
            addCard();
            nextCardTick += Math.max(25, 50 - getLevel() * 5);
        }
        Iterator<MiniGameUnit> iterator = zombies.iterator();
        while (iterator.hasNext()) {
            MiniGameUnit zombie = iterator.next();
            zombie.tickAge();
            if (zombie.isDead()) {
                iterator.remove();
                kills++;
                addScore(125);
                continue;
            }
            zombie.setColumn(zombie.getColumn() - zombie.getSpeed());
            if (zombie.getColumn() < -0.1) {
                lose();
                return;
            }
        }
        if (kills >= getTarget()) {
            win();
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
        NutType type = roll < 65 ? NutType.NORMAL : roll < 90 ? NutType.EXPLOSIVE : NutType.GIANT;
        conveyor.put(type, conveyor.get(type) + 1);
    }

    private MiniGameUnit nearestInRow(int row) {
        MiniGameUnit result = null;
        for (MiniGameUnit zombie : zombies) {
            if (!zombie.isDead() && zombie.getRow() == row
                && (result == null || zombie.getColumn() < result.getColumn())) {
                result = zombie;
            }
        }
        return result;
    }

    @Override
    protected String progressText() {
        return "kills=" + kills + "/" + getTarget() + ", activeZombies=" + zombies.size()
            + ", conveyor=" + conveyor;
    }

    @Override
    public String boardView() {
        StringBuilder builder = new StringBuilder("Wall-nut Bowling (Z=zombie, red line after column 3)\n");
        for (int row = 0; row < 5; row++) {
            builder.append(row + 1).append(" | ");
            for (int col = 0; col < 9; col++) {
                char symbol = '.';
                for (MiniGameUnit zombie : zombies) {
                    if (!zombie.isDead() && zombie.getRow() == row
                        && Math.round(zombie.getColumn()) == col) {
                        symbol = 'Z';
                    }
                }
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
        return value == null ? "" : value.toLowerCase().replace("-", "").replace("_", "");
    }
}
