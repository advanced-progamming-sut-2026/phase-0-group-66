package network.game;

import java.io.Serializable;

public record MatchInvite(String requestId, String requester, int level) implements Serializable {
}
