package network.protocol;

import java.io.Serializable;

/** Operations supported by the Phase 3 client/server protocol. */
public enum NetworkOperation implements Serializable {
    PING,
    AUTHENTICATE,
    LOGOUT,
    FIND_USER,
    GET_SECURITY_PROFILE,
    VERIFY_SECURITY_ANSWER,
    RESET_PASSWORD,
    USERNAME_EXISTS,
    GET_ALL_USERS,
    ADD_USER,
    RENAME_USER,
    DELETE_USER,
    SAVE_USER,
    MATCH_RANDOM,
    MATCH_CHALLENGE,
    MATCH_REQUESTS,
    MATCH_RESPONSE,
    MATCH_STATUS,
    MATCH_STATE,
    MATCH_ACTION,
    MATCH_REACTION,
    SUBMIT_MINIGAME_SCORE,
    SUBMIT_SCORED_SCORE,
    GET_LEADERBOARD
}
