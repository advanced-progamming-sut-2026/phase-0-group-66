package model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public final class BeghouldSession extends MiniGameSession {
    private static final int ROWS = 5;
    private static final int COLS = 9;
    private static final String[] TYPES = {
        "Peashooter", "Wall-nut", "Puff-shroom", "Cabbage-pult", "Melon-pult"
    };
    private final String[][] grid = new String[ROWS][COLS];
    private final boolean[][] crater = new boolean[ROWS][COLS];
    private final ArrayList<MiniGameUnit> zombies = new ArrayList<>();
    private final Random random;
    private int sun;
    private int matches;
    private int nextZombieTick;

    public BeghouldSession(MiniGameDefinition definition, int level) {
        super(definition, level);
        random = new Random(40_000L + level * 1237L);
        fillBoard();
        removeInitialMatches();
        nextZombieTick = 20;
    }

    @Override
    public void execute(String command, List<String> arguments) {
        ensureRunning();
        String normalized = normalize(command);
        switch (normalized) {
            case "swap", "makematch" -> swap(intArg(arguments, 0) - 1,
                intArg(arguments, 1) - 1, intArg(arguments, 2) - 1, intArg(arguments, 3) - 1);
            case "upgrade" -> upgrade(arg(arguments, 0));
            case "advance" -> advanceTime(intArg(arguments, 0));
            default -> throw new IllegalArgumentException(
                "Beghouled commands: swap <x1> <y1> <x2> <y2>, upgrade <plant>, advance <ticks>.");
        }
    }

    private void fillBoard() {
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                grid[row][col] = randomType();
            }
        }
    }

    private void removeInitialMatches() {
        for (int attempts = 0; attempts < 20; attempts++) {
            Set<GridPosition> found = findMatches();
            if (found.isEmpty()) {
                return;
            }
            for (GridPosition position : found) {
                grid[position.getRow()][position.getColumn()] = randomType();
            }
        }
    }

    private void swap(int x1, int y1, int x2, int y2) {
        validate(y1, x1);
        validate(y2, x2);
        if (Math.abs(x1 - x2) + Math.abs(y1 - y2) != 1) {
            throw new IllegalArgumentException("Only adjacent plants can be swapped.");
        }
        if (crater[y1][x1] || crater[y2][x2]) {
            throw new IllegalStateException("A crater cannot contain or receive a plant.");
        }
        String first = grid[y1][x1];
        grid[y1][x1] = grid[y2][x2];
        grid[y2][x2] = first;
        Set<GridPosition> found = findMatches();
        if (found.isEmpty()) {
            first = grid[y1][x1];
            grid[y1][x1] = grid[y2][x2];
            grid[y2][x2] = first;
            throw new IllegalStateException("The swap must create a match of at least three.");
        }
        resolveCascades(found);
        if (matches >= getTarget()) {
            zombies.clear();
            win();
            addScore(1500 + sun);
        } else if (!hasPossibleMove()) {
            resetBoard();
        }
    }

    private void resolveCascades(Set<GridPosition> initial) {
        Set<GridPosition> current = initial;
        int cascade = 0;
        while (!current.isEmpty()) {
            cascade++;
            int size = current.size();
            int baseSuns = Math.max(1, size - 2);
            sun += 50 * (baseSuns + (cascade > 1 ? 1 : 0));
            matches++;
            addScore(size * 80 + cascade * 100);
            for (GridPosition position : current) {
                grid[position.getRow()][position.getColumn()] = null;
            }
            collapse();
            current = findMatches();
        }
    }

    private void collapse() {
        for (int col = 0; col < COLS; col++) {
            ArrayDeque<String> values = new ArrayDeque<>();
            for (int row = ROWS - 1; row >= 0; row--) {
                if (!crater[row][col] && grid[row][col] != null) {
                    values.addLast(grid[row][col]);
                }
            }
            for (int row = ROWS - 1; row >= 0; row--) {
                if (crater[row][col]) {
                    grid[row][col] = null;
                } else {
                    grid[row][col] = values.isEmpty() ? randomType() : values.removeFirst();
                }
            }
        }
    }

    private Set<GridPosition> findMatches() {
        HashSet<GridPosition> result = new HashSet<>();
        for (int row = 0; row < ROWS; row++) {
            int start = 0;
            while (start < COLS) {
                String value = grid[row][start];
                int end = start + 1;
                while (end < COLS && value != null && value.equals(grid[row][end])) {
                    end++;
                }
                if (value != null && end - start >= 3) {
                    for (int col = start; col < end; col++) {
                        result.add(new GridPosition(row, col));
                    }
                }
                start = end;
            }
        }
        for (int col = 0; col < COLS; col++) {
            int start = 0;
            while (start < ROWS) {
                String value = grid[start][col];
                int end = start + 1;
                while (end < ROWS && value != null && value.equals(grid[end][col])) {
                    end++;
                }
                if (value != null && end - start >= 3) {
                    for (int row = start; row < end; row++) {
                        result.add(new GridPosition(row, col));
                    }
                }
                start = end;
            }
        }
        return result;
    }

    private void upgrade(String plantName) {
        String normalized = PlantDefinition.normalizeKey(plantName);
        String from;
        String to;
        int cost;
        if (normalized.equals("peashooter")) {
            from = "Peashooter"; to = "Repeater"; cost = 500;
        } else if (normalized.equals("repeater")) {
            from = "Repeater"; to = "Mega Gatling Pea"; cost = 1500;
        } else if (normalized.equals("wallnut")) {
            from = "Wall-nut"; to = "Tall-nut"; cost = 500;
        } else if (normalized.equals("puffshroom")) {
            from = "Puff-shroom"; to = "Fume-shroom"; cost = 250;
        } else if (normalized.equals("cabbagepult")) {
            from = "Cabbage-pult"; to = "Melon-pult"; cost = 1000;
        } else if (normalized.equals("melonpult")) {
            from = "Melon-pult"; to = "Winter Melon"; cost = 750;
        } else {
            throw new IllegalArgumentException("That plant has no Beghouled upgrade.");
        }
        if (sun < cost) {
            throw new IllegalStateException("Not enough sun for the upgrade.");
        }
        int changed = 0;
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                if (from.equals(grid[row][col])) {
                    grid[row][col] = to;
                    changed++;
                }
            }
        }
        if (changed == 0) {
            throw new IllegalStateException("No " + from + " exists on the board.");
        }
        sun -= cost;
        addScore(changed * 40);
    }

    @Override
    protected void onTick() {
        if (getElapsedTicks() >= nextZombieTick) {
            zombies.add(new MiniGameUnit("Beghouled Zombie", random.nextInt(ROWS), 8.8,
                300 + getLevel() * 80, 30, 0.025 + getLevel() * 0.004));
            nextZombieTick += Math.max(20, 45 - getLevel() * 5);
        }
        for (MiniGameUnit zombie : zombies) {
            if (zombie.isDead()) {
                continue;
            }
            int row = zombie.getRow();
            int col = Math.max(0, Math.min(COLS - 1, (int) Math.floor(zombie.getColumn())));
            if (grid[row][col] != null) {
                grid[row][col] = null;
                crater[row][col] = true;
                zombie.setColumn(zombie.getColumn() - 0.25);
                addScore(-50);
            } else {
                zombie.setColumn(zombie.getColumn() - zombie.getSpeed());
            }
            if (zombie.getColumn() < -0.1) {
                lose();
                return;
            }
        }
    }

    private boolean hasPossibleMove() {
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                if (crater[row][col]) {
                    continue;
                }
                if (col + 1 < COLS && !crater[row][col + 1] && createsMatch(row, col, row, col + 1)) {
                    return true;
                }
                if (row + 1 < ROWS && !crater[row + 1][col] && createsMatch(row, col, row + 1, col)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean createsMatch(int r1, int c1, int r2, int c2) {
        String first = grid[r1][c1];
        grid[r1][c1] = grid[r2][c2];
        grid[r2][c2] = first;
        boolean result = !findMatches().isEmpty();
        first = grid[r1][c1];
        grid[r1][c1] = grid[r2][c2];
        grid[r2][c2] = first;
        return result;
    }

    private void resetBoard() {
        fillBoard();
        removeInitialMatches();
        addScore(-100);
    }

    @Override
    protected String progressText() {
        return "matches=" + matches + "/" + getTarget() + ", sun=" + sun
            + ", zombies=" + zombies.size();
    }

    @Override
    public String boardView() {
        StringBuilder builder = new StringBuilder("Beghouled board (#=crater)\n");
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                if (crater[row][col]) {
                    builder.append("# ");
                } else {
                    builder.append(symbol(grid[row][col])).append(' ');
                }
            }
            builder.append('\n');
        }
        builder.append("Legend: P=Peashooter, W=Wall-nut, F=Puff-shroom, C=Cabbage-pult, M=Melon-pult\n")
            .append("Sun=").append(sun).append(", matches=").append(matches)
            .append('/').append(getTarget());
        return builder.toString();
    }

    private char symbol(String type) {
        if (type == null) { return '.'; }
        return switch (PlantDefinition.normalizeKey(type)) {
            case "peashooter", "repeater", "megagatlingpea" -> 'P';
            case "wallnut", "tallnut" -> 'W';
            case "puffshroom", "fumeshroom" -> 'F';
            case "cabbagepult" -> 'C';
            case "melonpult", "wintermelon" -> 'M';
            default -> '?';
        };
    }

    private String randomType() { return TYPES[random.nextInt(TYPES.length)]; }

    private void validate(int row, int col) {
        if (row < 0 || row >= ROWS || col < 0 || col >= COLS) {
            throw new IllegalArgumentException("Position must be inside the 9x5 board.");
        }
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
        return value == null ? "" : value.toLowerCase().replace("-", "").replace("_", "");
    }
}
