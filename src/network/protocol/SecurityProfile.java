package network.protocol;

import model.SecurityQuestion;

import java.io.Serial;
import java.io.Serializable;

/** Minimal pre-login account information needed by password recovery. */
public record SecurityProfile(String username, String nickname, String email, String gender,
                              SecurityQuestion securityQuestion) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
