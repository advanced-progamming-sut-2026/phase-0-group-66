package network.game;

import java.io.Serializable;
import java.util.List;

public record NetworkIZombieState(
    String matchId,
    String selfUsername,
    String opponentUsername,
    MatchRole role,
    int level,
    int score,
    int elapsedTicks,
    boolean won,
    boolean lost,
    int sun,
    int plantSun,
    int brainsEaten,
    boolean[] brains,
    List<Card> cards,
    List<Plant> plants,
    List<Zombie> zombies,
    List<MatchReaction> reactions
) implements Serializable {
    public NetworkIZombieState {
        brains = brains == null ? new boolean[0] : brains.clone();
        cards = List.copyOf(cards == null ? List.of() : cards);
        plants = List.copyOf(plants == null ? List.of() : plants);
        zombies = List.copyOf(zombies == null ? List.of() : zombies);
        reactions = List.copyOf(reactions == null ? List.of() : reactions);
    }

    @Override
    public boolean[] brains() {
        return brains.clone();
    }

    public record Card(String key, String type, int cost, int health,
                       int damage, double speed) implements Serializable { }

    public record Plant(String type, int row, int column, int health,
                        int damage) implements Serializable { }

    public record Zombie(String type, int row, double column, int health,
                         int maximumHealth, int damage, double speed) implements Serializable { }
}
