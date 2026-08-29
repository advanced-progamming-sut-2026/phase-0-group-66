package network.protocol;

import model.User;

import java.io.Serial;
import java.io.Serializable;

/** The authenticated user plus the short-lived server session token. */
public record AuthenticatedSession(String token, User user) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
