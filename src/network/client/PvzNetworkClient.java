package network.client;

import network.protocol.NetworkOperation;
import network.protocol.NetworkRequest;
import network.protocol.NetworkResponse;
import network.protocol.Phase3Protocol;
import network.protocol.AuthenticatedSession;
import network.game.MatchInvite;
import network.game.MatchTicket;
import network.game.NetworkIZombieState;
import model.LeaderboardEntry;
import model.MiniGameType;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;

/**
 * Lightweight request/response TCP client. A fresh connection is used per request so account
 * persistence is independent from multiplayer session lifecycle added in later Phase 3 stages.
 */
public final class PvzNetworkClient {
    private static final int DEFAULT_CONNECT_TIMEOUT_MS = 2500;
    private static final int DEFAULT_READ_TIMEOUT_MS = 5000;

    private final String host;
    private final int port;
    private final int connectTimeoutMs;
    private final int readTimeoutMs;
    private volatile String authToken;

    public PvzNetworkClient(String host, int port) {
        this(host, port, DEFAULT_CONNECT_TIMEOUT_MS, DEFAULT_READ_TIMEOUT_MS);
    }

    public PvzNetworkClient(String host, int port, int connectTimeoutMs, int readTimeoutMs) {
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("Server host cannot be empty.");
        }
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Server port must be between 1 and 65535.");
        }
        this.host = host.trim();
        this.port = port;
        this.connectTimeoutMs = Math.max(100, connectTimeoutMs);
        this.readTimeoutMs = Math.max(100, readTimeoutMs);
    }

    public NetworkResponse request(NetworkOperation operation, Object... arguments) throws IOException {
        return requestWithToken(authToken, operation, arguments);
    }

    public NetworkResponse requestUnauthenticated(NetworkOperation operation, Object... arguments)
        throws IOException {
        return requestWithToken(null, operation, arguments);
    }

    public AuthenticatedSession authenticate(String username, String password) throws IOException {
        NetworkResponse response = requestUnauthenticated(NetworkOperation.AUTHENTICATE, username, password);
        Object value = successfulPayload(response);
        if (!(value instanceof AuthenticatedSession session)) {
            throw new IOException("Server returned an invalid authentication response.");
        }
        authToken = session.token();
        return session;
    }

    public void clearAuthentication() {
        authToken = null;
    }

    public boolean isAuthenticated() {
        return authToken != null && !authToken.isBlank();
    }

    private NetworkResponse requestWithToken(String token, NetworkOperation operation, Object... arguments)
        throws IOException {
        NetworkRequest request = new NetworkRequest(Phase3Protocol.VERSION, operation, token, arguments);
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), connectTimeoutMs);
            socket.setSoTimeout(readTimeoutMs);

            try (ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream input = new ObjectInputStream(socket.getInputStream())) {
                output.writeObject(request);
                output.flush();
                Object received = input.readObject();
                if (!(received instanceof NetworkResponse response)) {
                    throw new IOException("Server returned an invalid response.");
                }
                return response;
            } catch (ClassNotFoundException exception) {
                throw new IOException("Could not decode server response.", exception);
            }
        }
    }

    public boolean ping() {
        try {
            return request(NetworkOperation.PING).isSuccessful();
        } catch (IOException exception) {
            return false;
        }
    }

    public MatchTicket findRandomMatch(String username, int level) throws IOException {
        return payload(request(NetworkOperation.MATCH_RANDOM, username, level), MatchTicket.class);
    }

    public MatchTicket challenge(String username, String opponent, int level) throws IOException {
        return payload(request(NetworkOperation.MATCH_CHALLENGE, username, opponent, level), MatchTicket.class);
    }

    public List<MatchInvite> getMatchRequests(String username) throws IOException {
        Object value = successfulPayload(request(NetworkOperation.MATCH_REQUESTS, username));
        if (!(value instanceof List<?> values)) {
            throw new IOException("Server returned invalid match requests.");
        }
        return values.stream().filter(MatchInvite.class::isInstance).map(MatchInvite.class::cast).toList();
    }

    public MatchTicket respondToMatch(String username, String ticketId, boolean accepted) throws IOException {
        return payload(request(NetworkOperation.MATCH_RESPONSE, username, ticketId, accepted), MatchTicket.class);
    }

    public MatchTicket matchStatus(String username, String ticketId) throws IOException {
        return payload(request(NetworkOperation.MATCH_STATUS, username, ticketId), MatchTicket.class);
    }

    public NetworkIZombieState matchState(String username, String matchId) throws IOException {
        return payload(request(NetworkOperation.MATCH_STATE, username, matchId), NetworkIZombieState.class);
    }

    public NetworkIZombieState matchAction(String username, String matchId, String command) throws IOException {
        return payload(request(NetworkOperation.MATCH_ACTION, username, matchId, command), NetworkIZombieState.class);
    }

    public NetworkIZombieState sendReaction(String username, String matchId,
                                             String category, String value) throws IOException {
        return payload(request(NetworkOperation.MATCH_REACTION, username, matchId, category, value),
            NetworkIZombieState.class);
    }

    public int submitMiniGameScore(String username, MiniGameType type, int level, int score)
        throws IOException {
        Object value = successfulPayload(request(NetworkOperation.SUBMIT_MINIGAME_SCORE,
            username, type, level, score));
        if (!(value instanceof Number number)) {
            throw new IOException("Server returned an invalid score.");
        }
        return number.intValue();
    }

    public int submitScoredScore(String username, int score) throws IOException {
        Object value = successfulPayload(request(NetworkOperation.SUBMIT_SCORED_SCORE,
            username, score));
        if (!(value instanceof Number number)) {
            throw new IOException("Server returned an invalid scored-game result.");
        }
        return number.intValue();
    }

    public List<LeaderboardEntry> getLeaderboard() throws IOException {
        Object value = successfulPayload(request(NetworkOperation.GET_LEADERBOARD));
        if (!(value instanceof List<?> values)) {
            throw new IOException("Server returned an invalid leaderboard.");
        }
        return values.stream().filter(LeaderboardEntry.class::isInstance)
            .map(LeaderboardEntry.class::cast).toList();
    }

    private <T> T payload(NetworkResponse response, Class<T> type) throws IOException {
        Object value = successfulPayload(response);
        if (!type.isInstance(value)) {
            throw new IOException("Server returned an invalid payload.");
        }
        return type.cast(value);
    }

    private Object successfulPayload(NetworkResponse response) throws IOException {
        if (!response.isSuccessful()) {
            throw new IOException(response.getMessage());
        }
        return response.getPayload();
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }
}
