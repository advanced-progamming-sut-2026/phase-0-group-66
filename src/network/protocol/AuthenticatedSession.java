package network.protocol;

import model.User;

import java.io.Serial;
import java.io.Serializable;

public record AuthenticatedSession(String token, User user) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public AuthenticatedSession {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Authentication token cannot be empty.");
        }
        if (user == null) {
            throw new IllegalArgumentException("Authenticated user cannot be null.");
        }
    }
}
