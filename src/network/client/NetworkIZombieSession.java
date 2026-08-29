package network.client;

import model.IZombieSession;
import model.MiniGameDefinition;
import model.MiniGamePlantSnapshot;
import model.MiniGameUnitSnapshot;
import network.game.MatchReaction;
import network.game.MatchRole;
import network.game.NetworkIZombieState;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class NetworkIZombieSession extends IZombieSession {
    private final PvzNetworkClient client;
    private final String username;
    private final String matchId;
    private final MatchRole role;
    private final String opponent;
    private NetworkIZombieState state;

    public NetworkIZombieSession(MiniGameDefinition definition, int level, PvzNetworkClient client,
                                  String username, String matchId, MatchRole role,
                                  String opponent) throws IOException {
        super(definition, level, true);
        this.client = client;
        this.username = username;
        this.matchId = matchId;
        this.role = role;
        this.opponent = opponent;
        apply(client.matchState(username, matchId));
    }

    @Override
    public void execute(String command, List<String> arguments) {
        StringBuilder commandLine = new StringBuilder(command == null ? "" : command);
        for (String argument : arguments == null ? List.<String>of() : arguments) {
            commandLine.append(' ').append(argument);
        }
        if (commandLine.toString().trim().equalsIgnoreCase("advance 1") && role == MatchRole.PLANTS) {
            poll();
            return;
        }
        try {
            apply(client.matchAction(username, matchId, commandLine.toString().trim()));
        } catch (IOException exception) {
            throw new IllegalStateException(exception.getMessage(), exception);
        }
    }

    public void poll() {
        if (isFinished()) {
            return;
        }
        try {
            apply(client.matchState(username, matchId));
        } catch (IOException ignored) {
            // The next action will surface a readable connection error to the player.
        }
    }

    public void sendReaction(String category, String value) {
        try {
            apply(client.sendReaction(username, matchId, category, value));
        } catch (IOException exception) {
            throw new IllegalStateException(exception.getMessage(), exception);
        }
    }

    public MatchRole getRole() {
        return role;
    }

    public String getOpponent() {
        return opponent;
    }

    public List<MatchReaction> getReactions() {
        return state.reactions();
    }

    @Override
    public List<ZombieCardView> getCardViews() {
        return state.cards().stream()
            .map(card -> new ZombieCardView(card.key(), card.type(), card.cost(), card.health(),
                card.damage(), card.speed())).toList();
    }

    @Override
    public List<MiniGamePlantSnapshot> getPlantViews() {
        return state.plants().stream()
            .map(plant -> new MiniGamePlantSnapshot(plant.type(), plant.row(), plant.column(),
                plant.health(), plant.damage())).toList();
    }

    @Override
    public List<MiniGameUnitSnapshot> getZombieViews() {
        return state.zombies().stream()
            .map(zombie -> new MiniGameUnitSnapshot(zombie.type(), zombie.row(), zombie.column(),
                zombie.health(), zombie.maximumHealth(), zombie.damage(), zombie.speed())).toList();
    }

    @Override
    public boolean[] getBrains() {
        return state.brains();
    }

    @Override
    public int getSun() {
        return state.sun();
    }

    @Override
    public int getPlantSun() {
        return state.plantSun();
    }

    @Override
    public int getBrainsEaten() {
        return state.brainsEaten();
    }

    @Override
    protected String progressText() {
        return "brains=" + state.brainsEaten() + "/5, sun=" + state.sun()
            + ", plantSun=" + state.plantSun() + ", role=" + role;
    }

    @Override
    public String boardView() {
        StringBuilder builder = new StringBuilder("I, Zombie online (P=plant, Z=zombie, B=brain)\n");
        for (int row = 0; row < 5; row++) {
            builder.append(state.brains()[row] ? 'B' : 'x').append(" | ");
            for (int column = 0; column < 9; column++) {
                char symbol = '.';
                for (NetworkIZombieState.Plant plant : state.plants()) {
                    if (plant.row() == row && plant.column() == column && plant.health() > 0) {
                        symbol = 'P';
                    }
                }
                for (NetworkIZombieState.Zombie zombie : state.zombies()) {
                    if (zombie.row() == row && Math.round(zombie.column()) == column && zombie.health() > 0) {
                        symbol = zombie.type().equals("Sun Producer Zombie") ? 'S' : 'Z';
                    }
                }
                builder.append(symbol).append(' ');
            }
            builder.append('\n');
        }
        return builder.append("Sun: ").append(state.sun()).append(" | Plant sun: ")
            .append(state.plantSun()).append(" | role: ").append(role).toString().stripTrailing();
    }

    private void apply(NetworkIZombieState next) {
        if (next == null) {
            throw new IllegalStateException("Server returned no match state.");
        }
        state = next;
        syncState(next.score(), next.elapsedTicks(), next.won(), next.lost());
    }
}
