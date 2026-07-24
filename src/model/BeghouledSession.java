package model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public final class BeghouledSession extends MiniGameSession {
    private static final int ROWS = 5;
    private static final int COLS = 9;
    private static final String[] TYPES = {
        "Peashooter", "Wall-nut", "Puff-shroom", "Cabbage-pult", "Melon-pult"
    };

    private final BeghouledCombatPlant[][] grid = new BeghouledCombatPlant[ROWS][COLS];
    private final boolean[][] crater = new boolean[ROWS][COLS];
    private final ArrayList<MiniGameUnit> zombies = new ArrayList<>();
    private final PlantFactory plantFactory;
    private final Random random;
    private int sun;
    private int matches;
    private int nextZombieTick;

    public BeghouledSession(MiniGameDefinition definition, int level) {
        super(definition, level);
        plantFactory = MiniGameData.plantFactory();
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
                intArg(arguments, 1) - 1, intArg(arguments, 2) - 1,
                intArg(arguments, 3) - 1);
            case "upgrade" -> upgrade(arg(arguments, 0));
            case "advance" -> advanceTime(intArg(arguments, 0));
            default -> throw new IllegalArgumentException(
                "Beghouled commands: swap <x1> <y1> <x2> <y2>, "
                    + "upgrade <plant>, advance <ticks>.");
        }
    }

    private BeghouledCombatPlant createPlant(String type) {
        return new BeghouledCombatPlant(type, profile(type));
    }

    private BeghouledCombatProfile profile(String type) {
        Plant plant = plantFactory.createPlant(type);
        PlantAbility ability = plant.getAbility();
        BeghouledAttackStyle style = attackStyle(ability, plant.getAttackPower());
        int splash = style == BeghouledAttackStyle.SPLASH
            ? Math.max(1, plant.getAttackPower() / 2 + plant.getSplashDamageBonus()) : 0;
        return new BeghouledCombatProfile(plant.getMaxHealth(), plant.getEffectiveAttackPower(),
            plant.getActionIntervalTicks(), plant.getProjectileCount(), style,
            splash, plant.getChillDurationTicks());
    }

    private BeghouledAttackStyle attackStyle(PlantAbility ability, int damage) {
        if (damage <= 0) {
            return BeghouledAttackStyle.NONE;
        }
        if (ability == PlantAbility.SHORT_RANGE_SHROOM
            || ability == PlantAbility.FUME_SHROOM) {
            return BeghouledAttackStyle.PIERCE;
        }
        if (ability == PlantAbility.CABBAGE_PULT || ability == PlantAbility.KERNEL_PULT
            || ability == PlantAbility.MELON_PULT || ability == PlantAbility.WINTER_MELON
            || ability == PlantAbility.PEPPER_PULT) {
            return BeghouledAttackStyle.SPLASH;
        }
        return BeghouledAttackStyle.DIRECT;
    }

    private void fillBoard() {
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                if (!crater[row][col]) {
                    grid[row][col] = createPlant(randomType());
                }
            }
        }
    }

    private void removeInitialMatches() {
        for (int attempts = 0; attempts < 40; attempts++) {
            Set<GridPosition> found = findMatches();
            if (found.isEmpty()) {
                return;
            }
            for (GridPosition position : found) {
                grid[position.getRow()][position.getColumn()] = createPlant(randomType());
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
        exchange(y1, x1, y2, x2);
        Set<GridPosition> found = findMatches();
        if (found.isEmpty()) {
            exchange(y1, x1, y2, x2);
            throw new IllegalStateException("The swap must create a match of at least three.");
        }
        resolveCascades(found);
        evaluateMatchVictory();
    }

    private void exchange(int rowOne, int colOne, int rowTwo, int colTwo) {
        BeghouledCombatPlant first = grid[rowOne][colOne];
        grid[rowOne][colOne] = grid[rowTwo][colTwo];
        grid[rowTwo][colTwo] = first;
    }

    private void evaluateMatchVictory() {
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
            ArrayDeque<BeghouledCombatPlant> values = new ArrayDeque<>();
            for (int row = ROWS - 1; row >= 0; row--) {
                if (!crater[row][col] && grid[row][col] != null) {
                    values.addLast(grid[row][col]);
                }
            }
            for (int row = ROWS - 1; row >= 0; row--) {
                if (crater[row][col]) {
                    grid[row][col] = null;
                } else {
                    grid[row][col] = values.isEmpty()
                        ? createPlant(randomType()) : values.removeFirst();
                }
            }
        }
    }

    private Set<GridPosition> findMatches() {
        HashSet<GridPosition> result = new HashSet<>();
        findHorizontalMatches(result);
        findVerticalMatches(result);
        return result;
    }

    private void findHorizontalMatches(Set<GridPosition> result) {
        for (int row = 0; row < ROWS; row++) {
            int start = 0;
            while (start < COLS) {
                String value = typeAt(row, start);
                int end = start + 1;
                while (end < COLS && value != null && value.equals(typeAt(row, end))) {
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
    }

    private void findVerticalMatches(Set<GridPosition> result) {
        for (int col = 0; col < COLS; col++) {
            int start = 0;
            while (start < ROWS) {
                String value = typeAt(start, col);
                int end = start + 1;
                while (end < ROWS && value != null && value.equals(typeAt(end, col))) {
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
    }

    private String typeAt(int row, int col) {
        return grid[row][col] == null ? null : grid[row][col].type;
    }

    private void upgrade(String plantName) {
        Upgrade upgrade = resolveUpgrade(plantName);
        if (sun < upgrade.cost()) {
            throw new IllegalStateException("Not enough sun for the upgrade.");
        }
        int changed = 0;
        BeghouledCombatProfile targetProfile = profile(upgrade.to());
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                BeghouledCombatPlant plant = grid[row][col];
                if (plant != null && upgrade.from().equals(plant.type)) {
                    plant.upgrade(upgrade.to(), targetProfile);
                    changed++;
                }
            }
        }
        if (changed == 0) {
            throw new IllegalStateException("No " + upgrade.from() + " exists on the board.");
        }
        sun -= upgrade.cost();
        addScore(changed * 40);
    }

    private record Upgrade(String from, String to, int cost) { }

    private Upgrade resolveUpgrade(String plantName) {
        return switch (PlantDefinition.normalizeKey(plantName)) {
            case "peashooter" -> new Upgrade("Peashooter", "Repeater", 500);
            case "repeater" -> new Upgrade("Repeater", "Mega Gatling Pea", 1500);
            case "wallnut" -> new Upgrade("Wall-nut", "Tall-nut", 500);
            case "puffshroom" -> new Upgrade("Puff-shroom", "Fume-shroom", 250);
            case "cabbagepult" -> new Upgrade("Cabbage-pult", "Melon-pult", 1000);
            case "melonpult" -> new Upgrade("Melon-pult", "Winter Melon", 750);
            default -> throw new IllegalArgumentException(
                "That plant has no Beghouled upgrade.");
        };
    }

    @Override
    protected void onTick() {
        spawnZombieIfNeeded();
        tickPlantsAndAttack();
        tickZombiesAndCombat();
        cleanupDeadZombies();
        if (matches >= getTarget()) {
            evaluateMatchVictory();
        }
    }

    private void spawnZombieIfNeeded() {
        if (getElapsedTicks() < nextZombieTick) {
            return;
        }
        zombies.add(new MiniGameUnit("Beghouled Zombie", random.nextInt(ROWS), 8.8,
            300 + getLevel() * 80, 30 + getLevel() * 5,
            0.025 + getLevel() * 0.004));
        nextZombieTick += Math.max(20, 45 - getLevel() * 5);
    }

    private void tickPlantsAndAttack() {
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                BeghouledCombatPlant plant = grid[row][col];
                if (plant == null) {
                    continue;
                }
                plant.tick();
                if (plant.ready()) {
                    attackFrom(row, col, plant);
                }
            }
        }
    }

    private void attackFrom(int row, int col, BeghouledCombatPlant plant) {
        MiniGameUnit target = nearestZombieAhead(row, col);
        if (target == null) {
            return;
        }
        switch (plant.profile.style()) {
            case DIRECT -> damageDirect(target, plant.profile);
            case PIERCE -> damagePiercing(row, col, plant.profile);
            case SPLASH -> damageSplash(target, plant.profile);
            case NONE -> { return; }
        }
        plant.resetCooldown();
    }

    private void damageDirect(MiniGameUnit target, BeghouledCombatProfile profile) {
        target.damage(profile.damage() * profile.projectileCount());
        applyChill(target, profile);
    }

    private void damagePiercing(int row, int col, BeghouledCombatProfile profile) {
        for (MiniGameUnit zombie : zombies) {
            if (!zombie.isDead() && zombie.getRow() == row && zombie.getColumn() >= col) {
                zombie.damage(profile.damage() * profile.projectileCount());
                applyChill(zombie, profile);
            }
        }
    }

    private void damageSplash(MiniGameUnit target, BeghouledCombatProfile profile) {
        target.damage(profile.damage() * profile.projectileCount());
        applyChill(target, profile);
        for (MiniGameUnit zombie : zombies) {
            if (zombie == target || zombie.isDead()) {
                continue;
            }
            if (Math.abs(zombie.getRow() - target.getRow()) <= 1
                && Math.abs(zombie.getColumn() - target.getColumn()) <= 1.25) {
                zombie.damage(profile.splashDamage());
                applyChill(zombie, profile);
            }
        }
    }

    private void applyChill(MiniGameUnit zombie, BeghouledCombatProfile profile) {
        if (profile.chillTicks() > 0) {
            zombie.slow(0.5, profile.chillTicks());
        }
    }

    private MiniGameUnit nearestZombieAhead(int row, int col) {
        MiniGameUnit result = null;
        for (MiniGameUnit zombie : zombies) {
            if (zombie.isDead() || zombie.getRow() != row || zombie.getColumn() < col) {
                continue;
            }
            if (result == null || zombie.getColumn() < result.getColumn()) {
                result = zombie;
            }
        }
        return result;
    }

    private void tickZombiesAndCombat() {
        for (MiniGameUnit zombie : zombies) {
            if (zombie.isDead()) {
                continue;
            }
            zombie.tickAge();
            BeghouledCombatPlant blocker = blockingPlant(zombie);
            if (blocker != null) {
                attackPlant(zombie, blocker);
            } else {
                zombie.setColumn(zombie.getColumn() - zombie.getSpeed());
                if (zombie.getColumn() < -0.1) {
                    lose();
                    return;
                }
            }
        }
    }

    private BeghouledCombatPlant blockingPlant(MiniGameUnit zombie) {
        int row = zombie.getRow();
        int col = Math.max(0, Math.min(COLS - 1, (int) Math.floor(zombie.getColumn())));
        BeghouledCombatPlant plant = grid[row][col];
        if (plant != null && zombie.getColumn() <= col + 0.85) {
            return plant;
        }
        return null;
    }

    private void attackPlant(MiniGameUnit zombie, BeghouledCombatPlant plant) {
        if (!zombie.ready()) {
            return;
        }
        plant.damage(zombie.getDamage());
        zombie.setCooldown(Game.TICKS_PER_SECOND);
        if (plant.isDead()) {
            int row = zombie.getRow();
            int col = Math.max(0, Math.min(COLS - 1,
                (int) Math.floor(zombie.getColumn())));
            grid[row][col] = null;
            crater[row][col] = true;
            addScore(-50);
        }
    }

    private void cleanupDeadZombies() {
        int before = zombies.size();
        zombies.removeIf(MiniGameUnit::isDead);
        addScore((before - zombies.size()) * 150);
    }

    private boolean hasPossibleMove() {
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                if (crater[row][col]) {
                    continue;
                }
                if (col + 1 < COLS && !crater[row][col + 1]
                    && createsMatch(row, col, row, col + 1)) {
                    return true;
                }
                if (row + 1 < ROWS && !crater[row + 1][col]
                    && createsMatch(row, col, row + 1, col)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean createsMatch(int rowOne, int colOne, int rowTwo, int colTwo) {
        exchange(rowOne, colOne, rowTwo, colTwo);
        boolean result = !findMatches().isEmpty();
        exchange(rowOne, colOne, rowTwo, colTwo);
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
        return BeghouledBoardRenderer.render(grid, crater, zombies, sun, matches, getTarget());
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

    void setSunForTest(int amount) { sun = Math.max(0, amount); }
    void setPlantForTest(int row, int col, String type) { grid[row][col] = createPlant(type); }
    void addZombieForTest(int row, double col, int health) {
        zombies.add(new MiniGameUnit("Test Zombie", row, col, health, 30, 0));
    }
    int firstZombieHealthForTest() { return zombies.isEmpty() ? 0 : zombies.get(0).getHealth(); }
    void disableZombieSpawnsForTest() { nextZombieTick = Integer.MAX_VALUE; }
    int plantHealthForTest(int row, int col) {
        return grid[row][col] == null ? 0 : grid[row][col].health;
    }
    String plantTypeForTest(int row, int col) {
        return grid[row][col] == null ? "" : grid[row][col].type;
    }
}
