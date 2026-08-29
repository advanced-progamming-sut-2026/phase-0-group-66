package network.game;

import java.io.Serializable;

public record MatchTicket(String ticketId, String status, String opponent,
                          String matchId, int level, MatchRole role) implements Serializable {
    public boolean isMatched() {
        return matchId != null && role != null;
    }
}
