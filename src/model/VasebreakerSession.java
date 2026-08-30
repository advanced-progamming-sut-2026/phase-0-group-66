package model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class VasebreakerSession extends MiniGameSession {
    private enum VaseContent { EMPTY, PLANT_PACKET, ZOMBIE, GARGANTUAR }
    private enum VaseKind { NORMAL, PLANT, GIANT }

    private record Vase(int row, int column, VaseKind kind,
                        VaseContent content, String payload) { }
    private record Packet(int id, String plantType, int row, int column,
                          int expiresAt) { }

    private static final int ROWS = 5;
    private static final int COLS = 9;
    private final LinkedHashMap<GridPosition, Vase> vases = new LinkedHashMap<>();
    private final LinkedHashMap<Integer, Packet> packets = new LinkedHashMap<>();
    private final ArrayList<MiniGameUnit> zombies = new ArrayList<>();
    private final ArrayList<MiniGamePlantUnit> plants = new ArrayList<>();
    private final Random random;
    private int nextPacketId = 1;
    private int brokenVases;
    private int killedZombies;

    public VasebreakerSession(MiniGameDefinition definition, int level) {
        super(definition, level);
        random = new Random(10_000L + level * 7919L);
        initializeVases();
    }

    @Override
    public void execute(String command, List<String> arguments) {
        ensureRunning();
        String normalized = normalize(command);
        switch (normalized) {
            case "break", "breakvase" -> breakVase(intArg(arguments, 0),
                intArg(arguments, 1));
            case "plant", "plantpacket" -> plantPacket(intArg(arguments, 0),
                intArg(arguments, 1), intArg(arguments, 2));
            case "advance" -> advanceTime(intArg(arguments, 0));
            default -> throw new IllegalArgumentException(
                "Vasebreaker commands: break <x> <y>, plant <packetId> <x> <y>, "
                    + "advance <ticks>.");
        }
        evaluateState();
    }

    private void initializeVases() {
        int count = getTarget();
        ArrayList<GridPosition> positions = vasePositions();
        java.util.Collections.shuffle(positions, random);
        List<String> packetsPool = List.of("Peashooter", "Cabbage-pult", "Wall-nut",
            "Snow Pea", "Repeater", "Potato Mine");
        int specialPlantVases = Math.min(3, 1 + getLevel());
        for (int index = 0; index < count; index++) {
            GridPosition position = positions.get(index);
            Vase vase;
            if (index == 0) {
                vase = new Vase(position.getRow(), position.getColumn(), VaseKind.GIANT,
                    VaseContent.GARGANTUAR, "Gargantuar");
            } else if (index <= specialPlantVases) {
                String packet = packetsPool.get(random.nextInt(packetsPool.size()));
                vase = new Vase(position.getRow(), position.getColumn(), VaseKind.PLANT,
                    VaseContent.PLANT_PACKET, packet);
            } else {
                vase = createNormalVase(position, packetsPool);
            }
            vases.put(position, vase);
        }
    }

    private ArrayList<GridPosition> vasePositions() {
        ArrayList<GridPosition> positions = new ArrayList<>();
        for (int row = 0; row < ROWS; row++) {
            for (int col = 3; col < COLS; col++) {
                positions.add(new GridPosition(row, col));
            }
        }
        return positions;
    }

    private Vase createNormalVase(GridPosition position, List<String> packetsPool) {
        int roll = random.nextInt(100);
        VaseContent content;
        String payload = "";
        if (roll < 18) {
            content = VaseContent.EMPTY;
        } else if (roll < 48) {
            content = VaseContent.PLANT_PACKET;
            payload = packetsPool.get(random.nextInt(packetsPool.size()));
        } else {
            content = VaseContent.ZOMBIE;
            payload = random.nextBoolean() ? "Basic Zombie" : "Conehead Zombie";
        }
        return new Vase(position.getRow(), position.getColumn(), VaseKind.NORMAL,
            content, payload);
    }

    private void breakVase(int x, int y) {
        GridPosition position = position(x, y);
        Vase vase = vases.remove(position);
        if (vase == null) {
            throw new IllegalStateException("There is no intact vase at that position.");
        }
        brokenVases++;
        addScore(vase.kind() == VaseKind.NORMAL ? 25 : 50);
        switch (vase.content()) {
            case EMPTY -> addScore(10);
            case PLANT_PACKET -> addPacket(vase);
            case ZOMBIE -> zombies.add(createZombie(vase.payload(),
                vase.row(), vase.column()));
            case GARGANTUAR -> zombies.add(new MiniGameUnit("Gargantuar", vase.row(),
                vase.column(), 900 + getLevel() * 250, 1000, 0.025));
        }
    }

    private void addPacket(Vase vase) {
        int packetId = nextPacketId++;
        packets.put(packetId, new Packet(packetId, vase.payload(), vase.row(),
            vase.column(), -1));
    }

    private MiniGameUnit createZombie(String type, int row, int column) {
        if (type.startsWith("Conehead")) {
            return new MiniGameUnit(type, row, column, 320 + getLevel() * 80,
                45, 0.04);
        }
        return new MiniGameUnit(type, row, column, 180 + getLevel() * 50,
            35, 0.05);
    }

    private void plantPacket(int packetId, int x, int y) {
        GridPosition target = position(x, y);
        Packet packet = packets.get(packetId);
        if (packet == null) {
            throw new IllegalArgumentException("Packet does not exist or has expired.");
        }
        validatePlantTarget(target);
        packets.remove(packetId);
        int health = packet.plantType().contains("Wall") ? 700 : 250;
        int damage = packet.plantType().contains("Repeater") ? 50
            : packet.plantType().contains("Potato") ? 300 : 30;
        plants.add(new MiniGamePlantUnit(packet.plantType(), target.getRow(),
            target.getColumn(), health, damage));
        addScore(20);
    }

    private void validatePlantTarget(GridPosition target) {
        if (findPlant(target.getRow(), target.getColumn()) != null) {
            throw new IllegalStateException("That tile already contains a plant.");
        }
        if (vases.containsKey(target)) {
            throw new IllegalStateException("Break the vase before planting on that tile.");
        }
    }

    @Override
    protected void onTick() {
        expirePackets();
        tickPlants();
        tickZombies();
        plants.removeIf(MiniGamePlantUnit::isDead);
        evaluateState();
    }

    private void tickPlants() {
        for (MiniGamePlantUnit plant : plants) {
            plant.tick();
            if (plant.ready()) {
                MiniGameUnit target = nearestZombie(plant.getRow(), plant.getColumn());
                if (target != null) {
                    target.damage(plant.getDamage());
                    plant.setCooldown(Game.TICKS_PER_SECOND);
                }
            }
        }
    }

    private void tickZombies() {
        Iterator<MiniGameUnit> iterator = zombies.iterator();
        while (iterator.hasNext()) {
            MiniGameUnit zombie = iterator.next();
            zombie.tickAge();
            if (zombie.isDead()) {
                iterator.remove();
                killedZombies++;
                addScore(zombie.getType().equals("Gargantuar") ? 750 : 150);
                continue;
            }
            MiniGamePlantUnit blocker = blockingPlant(zombie);
            if (blocker != null) {
                if (zombie.ready()) {
                    blocker.damage(zombie.getDamage());
                    zombie.setCooldown(Game.TICKS_PER_SECOND);
                }
            } else {
                zombie.setColumn(zombie.getColumn() - zombie.getSpeed());
                if (zombie.getColumn() < -0.1) {
                    lose();
                    return;
                }
            }
        }
    }

    private void expirePackets() {
        packets.entrySet().removeIf(entry -> entry.getValue().expiresAt() >= 0
            && entry.getValue().expiresAt() <= getElapsedTicks());
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
            if (!zombie.isDead() && zombie.getRow() == row
                && zombie.getColumn() >= column
                && (result == null || zombie.getColumn() < result.getColumn())) {
                result = zombie;
            }
        }
        return result;
    }

    private MiniGamePlantUnit findPlant(int row, int column) {
        for (MiniGamePlantUnit plant : plants) {
            if (!plant.isDead() && plant.getRow() == row
                && plant.getColumn() == column) {
                return plant;
            }
        }
        return null;
    }

    private void evaluateState() {
        if (vases.isEmpty() && zombies.isEmpty() && !isLost()) {
            win();
            addScore(1000 + packets.size() * 25);
        }
    }

    @Override
    protected String progressText() {
        return "vases=" + brokenVases + "/" + getTarget() + ", zombiesKilled="
            + killedZombies + ", activeZombies=" + zombies.size()
            + ", packets=" + packets.size();
    }

    @Override
    public String boardView() {
        StringBuilder builder = new StringBuilder();
        builder.append("Vasebreaker (??=normal vase, PV=plant vase, GV=Gargantuar vase, ")
            .append("PL=plant, ZO=zombie, ..=empty)\n");
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                builder.append(cellSymbol(row, col)).append(' ');
            }
            builder.append('\n');
        }
        if (!packets.isEmpty()) {
            builder.append("Packets: ");
            for (Map.Entry<Integer, Packet> entry : packets.entrySet()) {
                builder.append('#').append(entry.getKey()).append('=')
                    .append(entry.getValue().plantType()).append(' ');
            }
        }
        return builder.toString().stripTrailing();
    }

    private String cellSymbol(int row, int col) {
        GridPosition position = new GridPosition(row, col);
        Vase vase = vases.get(position);
        if (vase != null) {
            return switch (vase.kind()) {
                case NORMAL -> "??";
                case PLANT -> "PV";
                case GIANT -> "GV";
            };
        }
        if (findPlant(row, col) != null) {
            return "PL";
        }
        for (MiniGameUnit zombie : zombies) {
            if (!zombie.isDead() && zombie.getRow() == row
                && Math.round(zombie.getColumn()) == col) {
                return "ZO";
            }
        }
        return "..";
    }


    public record VaseView(int row, int column, String kind) { }

    public record PacketView(int id, String plantType, int expiresAt) { }

    public List<VaseView> getVaseViews() {
        ArrayList<VaseView> result = new ArrayList<>();
        for (Vase vase : vases.values()) {
            result.add(new VaseView(vase.row(), vase.column(), vase.kind().name()));
        }
        return List.copyOf(result);
    }

    public List<PacketView> getPacketViews() {
        ArrayList<PacketView> result = new ArrayList<>();
        for (Packet packet : packets.values()) {
            result.add(new PacketView(packet.id(), packet.plantType(), packet.expiresAt()));
        }
        return List.copyOf(result);
    }

    public List<MiniGameUnitSnapshot> getZombieViews() {
        return zombies.stream().map(MiniGameUnit::snapshot).toList();
    }

    public List<MiniGamePlantSnapshot> getPlantViews() {
        return plants.stream().map(MiniGamePlantUnit::snapshot).toList();
    }

    public int getBrokenVases() {
        return brokenVases;
    }

    public int getKilledZombies() {
        return killedZombies;
    }

    private GridPosition position(int x, int y) {
        if (x < 1 || x > COLS || y < 1 || y > ROWS) {
            throw new IllegalArgumentException("Position must be inside the 9x5 board.");
        }
        return new GridPosition(y - 1, x - 1);
    }

    private int intArg(List<String> args, int index) {
        if (args == null || index >= args.size()) {
            throw new IllegalArgumentException("Missing numeric argument.");
        }
        try {
            return Integer.parseInt(args.get(index));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Expected an integer argument.");
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase().replace("-", "")
            .replace("_", "");
    }
}
