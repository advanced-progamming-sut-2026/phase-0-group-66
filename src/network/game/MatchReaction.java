package network.game;

import java.io.Serializable;

public record MatchReaction(String sender, String category, String value) implements Serializable {
}
