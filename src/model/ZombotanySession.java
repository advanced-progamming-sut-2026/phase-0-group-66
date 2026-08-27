package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public final class ZombotanySession extends MiniGameSession {
    private enum ZombiePower { PEASHOOTER, WALL_NUT, JALAPENO, SQUASH }

    private final Game game;
    private final Random random;
    private final Map<Zombie, ZombiePower> powers = new IdentityHashMap<>();
    private final Set<Zombie> jalapenoTriggered = Collections.newSetFromMap(
        new IdentityHashMap<>());
    private int nextSpawnTick;
    private boolean battleStarted;

    public ZombotanySession(MiniGameDefinition definition, int level) {
        super(definition, level);
        random = new Random(50_000L + level * 1877L);
        game = new Game(MiniGameData.plantFactory(), MiniGameData.zombieFactory(),
            3, Map.of(), new Inventory(), new Wallet(), 60_000L + level * 733L);
        Level battleLevel = new Level("zombotany-" + level, SeasonType.ANCIENT_EGYPT,
            level, SpecialLevelType.NORMAL, 8, 300);
        game.prepareLevel(null, battleLevel);
        game.setExternalWinControlled(true);
        nextSpawnTick = 10;
    }

    @Override
    public void execute(String command, List<String> arguments) {
        ensureRunning();
        String normalized = normalize(command);
        switch (normalized) {
            case "select", "addplant" -> selectPlant(arg(arguments, 0));
            case "remove", "removeplant" -> removePlant(arg(arguments, 0));
            case "start", "startgame" -> startBattle();
            case "plant" -> plant(arg(arguments, 0), intArg(arguments, 1) - 1,
                intArg(arguments, 2) - 1);
            case "collect", "collectsun" -> collectSun(intArg(arguments, 0) - 1,
                intArg(arguments, 1) - 1);
            case "feed" -> feedPlant(intArg(arguments, 0) - 1,
                intArg(arguments, 1) - 1);
            case "advance", "defeatzombie" -> advanceBattle(intArg(arguments, 0));
            default -> throw new IllegalArgumentException(commandHelp());
        }
    }

    private String commandHelp() {
        return "Zombotany commands: select <plant>, remove <plant>, start, "
            + "plant <type> <x> <y>, collect <x> <y>, feed <x> <y>, advance <ticks>.";
    }

    private void selectPlant(String type) {
        requireSelectionPhase();
        game.selectPlant(type);
    }

    private void removePlant(String type) {
        requireSelectionPhase();
        game.removeSelectedPlant(type);
    }

    private void requireSelectionPhase() {
        if (battleStarted) {
            throw new IllegalStateException("Plant selection is already finished.");
        }
    }

    private void startBattle() {
        requireSelectionPhase();
        game.startGame();
        game.setExternalWinControlled(true);
        removeAdventureTerrainOverlays();
        battleStarted = true;
    }

    private void removeAdventureTerrainOverlays() {
        game.tombs.clear();
        for (int row = 0; row < game.board.getRows(); row++) {
            for (int col = 0; col < game.board.getCols(); col++) {
                Tile tile = game.board.getTile(row, col);
                if (tile.getType() == TileType.TOMB) {
                    tile.setTileType(TileType.NORMAL);
                }
            }
        }
    }

    private void plant(String type, int col, int row) {
        requireBattleStarted();
        game.plant(type, row, col);
    }

    private void collectSun(int col, int row) {
        requireBattleStarted();
        game.collectSun(row, col);
    }

    private void feedPlant(int col, int row) {
        requireBattleStarted();
        game.feedPlant(row, col);
    }

    private void advanceBattle(int ticks) {
        requireBattleStarted();
        advanceTime(ticks);
    }

    private void requireBattleStarted() {
        if (!battleStarted) {
            throw new IllegalStateException("Select plants and use 'start' first.");
        }
    }

    @Override
    protected void onTick() {
        spawnZombieIfNeeded();
        applySquashCollisions();
        applyPeashooterShots();
        applyJalapenoExplosions();
        game.advanceTime(1);
        removeFinishedPowerMappings();
        evaluateBattleState();
    }

    private void spawnZombieIfNeeded() {
        if (game.getElapsedTicks() < nextSpawnTick) {
            return;
        }
        ZombiePower power = ZombiePower.values()[random.nextInt(ZombiePower.values().length)];
        Zombie zombie = createPoweredZombie(power);
        zombie.setPosition(new BoardPosition(random.nextInt(5), 8.8));
        game.board.addZombie(zombie);
        powers.put(zombie, power);
        nextSpawnTick += Math.max(18, 38 - getLevel() * 4);
    }

    private Zombie createPoweredZombie(ZombiePower power) {
        String displayName;
        int health;
        int damage;
        double speed;
        switch (power) {
            case PEASHOOTER -> {
                displayName = "Peashooter Zombie";
                health = 380 + getLevel() * 60;
                damage = 100;
                speed = 0.185;
            }
            case WALL_NUT -> {
                displayName = "Wall-nut Zombie";
                health = 1500 + getLevel() * 200;
                damage = 100;
                speed = 0.100;
            }
            case JALAPENO -> {
                displayName = "Jalapeno Zombie";
                health = 420 + getLevel() * 70;
                damage = 100;
                speed = 0.185;
            }
            case SQUASH -> {
                displayName = "Squash Zombie";
                health = 260 + getLevel() * 40;
                damage = 100;
                speed = 0.420;
            }
            default -> throw new IllegalStateException("Unsupported Zombotany power.");
        }
        String key = PlantDefinition.normalizeKey(displayName);
        ZombieDefinition definition = new ZombieDefinition(key, key, displayName,
            health, damage, speed, 100, 1, false, ZombieAbility.GENERIC,
            List.of(SeasonType.ANCIENT_EGYPT), List.of(), List.of(), Map.of());
        return new GenericZombie(definition, List.of());
    }

    private void applyPeashooterShots() {
        for (Map.Entry<Zombie, ZombiePower> entry : new ArrayList<>(powers.entrySet())) {
            Zombie zombie = entry.getKey();
            if (entry.getValue() != ZombiePower.PEASHOOTER || zombie.isDead()
                || zombie.getPosition() == null || zombie.getAgeTicks() == 0
                || zombie.getAgeTicks() % 15 != 0) {
                continue;
            }
            Plant target = nearestPlantToLeft(zombie);
            if (target != null) {
                target.takeDamage(20 + getLevel() * 5);
            }
        }
    }

    private Plant nearestPlantToLeft(Zombie zombie) {
        Plant result = null;
        double bestColumn = -1;
        for (Plant plant : game.board.getPlantsInRow(zombie.getPosition().getRow())) {
            if (plant.isDestroyed() || plant.getPosition() == null
                || plant.getPosition().getColumn() > zombie.getPosition().getColumn()) {
                continue;
            }
            if (plant.getPosition().getColumn() > bestColumn) {
                bestColumn = plant.getPosition().getColumn();
                result = plant;
            }
        }
        return result;
    }

    private void applyJalapenoExplosions() {
        for (Map.Entry<Zombie, ZombiePower> entry : new ArrayList<>(powers.entrySet())) {
            Zombie zombie = entry.getKey();
            if (entry.getValue() != ZombiePower.JALAPENO || zombie.isDead()
                || zombie.getPosition() == null || zombie.getAgeTicks() < 100
                || !jalapenoTriggered.add(zombie)) {
                continue;
            }
            for (Plant plant : new ArrayList<>(
                game.board.getPlantsInRow(zombie.getPosition().getRow()))) {
                plant.takeDamage(plant.getHealth());
            }
            zombie.kill();
        }
    }

    private void applySquashCollisions() {
        for (Map.Entry<Zombie, ZombiePower> entry : new ArrayList<>(powers.entrySet())) {
            Zombie zombie = entry.getKey();
            if (entry.getValue() != ZombiePower.SQUASH || zombie.isDead()
                || zombie.getPosition() == null) {
                continue;
            }
            Plant target = collidingPlant(zombie);
            if (target != null) {
                target.takeDamage(target.getHealth());
                zombie.kill();
            }
        }
    }

    private Plant collidingPlant(Zombie zombie) {
        for (Plant plant : game.board.getPlantsInRow(zombie.getPosition().getRow())) {
            if (!plant.isDestroyed() && plant.getPosition() != null
                && Math.abs(plant.getPosition().getColumn()
                    - zombie.getPosition().getColumn()) <= 0.70) {
                return plant;
            }
        }
        return null;
    }

    private void removeFinishedPowerMappings() {
        powers.entrySet().removeIf(entry -> entry.getKey().isDead()
            || !game.board.getZombies().contains(entry.getKey()));
        jalapenoTriggered.removeIf(zombie -> !powers.containsKey(zombie));
    }

    private void evaluateBattleState() {
        if (game.getGameState() == GameState.LOST) {
            lose();
            return;
        }
        if (game.getZombieKillCount() >= getTarget()) {
            win();
            addScore(1500 + game.getSunAmount() + game.getLawnMowerKills() * 100);
        } else {
            addScore(Math.max(0, game.getZombieKillCount() * 5 - getScore()));
        }
    }

    @Override
    protected String progressText() {
        if (!battleStarted) {
            return "selection=" + game.getSelectedPlants().size() + "/8, battle=NOT_STARTED";
        }
        return "kills=" + game.getZombieKillCount() + "/" + getTarget()
            + ", sun=" + game.getSunAmount() + ", plants=" + game.board.getPlants().size()
            + ", zombies=" + game.board.getZombies().size() + ", mowersUsed="
            + game.getLawnMowerKills();
    }

    @Override
    public String boardView() {
        if (!battleStarted) {
            return "Zombotany plant selection\nSelected: " + game.getSelectedPlants()
                + "\nUse select <plant>, remove <plant>, then start.";
        }
        StringBuilder builder = new StringBuilder(
            "Zombotany uses the Adventure battle engine (M=mower ready, x=mower used).\n");
        for (int row = 0; row < game.board.getRows(); row++) {
            LawnMower mower = game.board.getLawnMower(row);
            builder.append(mower.isActivated() ? 'x' : 'M').append(" | ");
            for (int col = 0; col < game.board.getCols(); col++) {
                builder.append(symbolAt(row, col)).append(' ');
            }
            builder.append('\n');
        }
        builder.append("Selected=").append(game.getSelectedPlants())
            .append(" | Sun=").append(game.getSunAmount())
            .append(" | Kills=").append(game.getZombieKillCount())
            .append('/').append(getTarget());
        return builder.toString();
    }

    public List<String> getSelectedPlantViews() {
        return game.getSelectedPlants();
    }

    public List<MiniGamePlantSnapshot> getPlantViews() {
        return game.getBoard().getPlants().stream()
            .filter(plant -> !plant.isDestroyed() && plant.getPosition() != null)
            .map(plant -> new MiniGamePlantSnapshot(plant.getName(),
                plant.getPosition().getRow(), plant.getPosition().getColumn(),
                plant.getHealth(), plant.getAttackPower()))
            .toList();
    }

    public List<MiniGameUnitSnapshot> getZombieViews() {
        return game.getBoard().getZombies().stream()
            .filter(zombie -> !zombie.isDead() && zombie.getPosition() != null)
            .map(zombie -> new MiniGameUnitSnapshot(zombie.getName(),
                zombie.getPosition().getRow(), zombie.getPosition().getColumn(),
                zombie.getHealth(), zombie.getMaximumHealth(), zombie.getDamage(),
                zombie.getSpeed()))
            .toList();
    }

    public record SunView(int row, int column, int amount) { }

    public List<SunView> getSunViews() {
        return game.getBoard().getSuns().stream()
            .filter(sun -> !sun.isCollected() && sun.getPosition() != null)
            .map(sun -> new SunView(sun.getPosition().getRow(), sun.getPosition().getColumn(),
                sun.getAmount()))
            .toList();
    }

    public boolean isBattleStarted() {
        return battleStarted;
    }

    public int getKills() {
        return game.getZombieKillCount();
    }

    public int getSun() {
        return game.getSunAmount();
    }

    private char symbolAt(int row, int col) {
        Tile tile = game.board.getTile(row, col);
        if (!tile.getZombies().isEmpty()) {
            return poweredZombieSymbol(tile.getZombies().get(0));
        }
        if (tile.getPlant() != null) {
            return 'P';
        }
        if (tile.getType() == TileType.TOMB) {
            return 'T';
        }
        return '.';
    }

    private char poweredZombieSymbol(Zombie zombie) {
        ZombiePower power = powers.get(zombie);
        if (power == null) {
            return 'Z';
        }
        return switch (power) {
            case PEASHOOTER -> 'p';
            case WALL_NUT -> 'w';
            case JALAPENO -> 'j';
            case SQUASH -> 's';
        };
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

    Game gameForTest() { return game; }
    boolean battleStartedForTest() { return battleStarted; }
    void spawnPowerForTest(String power, int row, double column) {
        ZombiePower type = ZombiePower.valueOf(power.toUpperCase());
        Zombie zombie = createPoweredZombie(type);
        zombie.setPosition(new BoardPosition(row, column));
        game.board.addZombie(zombie);
        powers.put(zombie, type);
    }
}
