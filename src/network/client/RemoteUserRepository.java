package network.client;

import model.AuthenticationResult;
import model.User;
import model.UserRepository;
import network.protocol.NetworkOperation;
import network.protocol.NetworkResponse;
import network.protocol.SecurityProfile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * UserRepository adapter backed by the Phase 3 server.
 *
 * Only the small local session file is inherited from UserRepository. Authoritative user data is
 * never written to the client users.dat file while this adapter is active.
 */
public final class RemoteUserRepository extends UserRepository {
    private final PvzNetworkClient client;
    private final Map<String, User> cache = new LinkedHashMap<>();
    private volatile String lastNetworkError;
    private volatile String recoveryToken;

    public RemoteUserRepository(Path localSessionDirectory, PvzNetworkClient client) throws IOException {
        super(localSessionDirectory, false);
        this.client = client;
    }

    @Override
    public synchronized AuthenticationResult authenticate(String username, String password) {
        try {
            var session = client.authenticate(username, password);
            User user = session.user();
            cache.put(user.getUsername(), user);
            return AuthenticationResult.success(user);
        } catch (IOException exception) {
            lastNetworkError = friendlyMessage(exception);
            return AuthenticationResult.failure(lastNetworkError);
        }
    }

    @Override
    public synchronized Optional<User> findByUsername(String username) {
        NetworkResponse response = request(NetworkOperation.FIND_USER, username);
        if (response == null || !response.isSuccessful()) {
            return Optional.empty();
        }
        User user;
        if (response.getPayload() instanceof User fullUser) {
            user = fullUser;
        } else if (response.getPayload() instanceof SecurityProfile profile) {
            user = new User(profile.username(), UUID.randomUUID().toString(),
                profile.nickname(), profile.email(), profile.gender(), profile.securityQuestion(),
                UUID.randomUUID().toString());
        } else {
            return Optional.empty();
        }
        cache.put(user.getUsername(), user);
        return Optional.of(user);
    }

    @Override
    public synchronized boolean verifySecurityAnswer(String username, String answer) {
        NetworkResponse response = request(NetworkOperation.VERIFY_SECURITY_ANSWER, username, answer);
        if (response == null || !response.isSuccessful() || !(response.getPayload() instanceof String token)) {
            return false;
        }
        recoveryToken = token;
        return true;
    }

    @Override
    public synchronized void resetPassword(String username, String newPassword) throws IOException {
        if (recoveryToken == null || recoveryToken.isBlank()) {
            throw new IllegalStateException("Verify the security answer first.");
        }
        NetworkResponse response = requestOrThrowUnauthenticated(NetworkOperation.RESET_PASSWORD,
            username, newPassword, recoveryToken);
        if (!response.isSuccessful()) {
            throw new IOException(response.getMessage());
        }
        recoveryToken = null;
    }

    public synchronized List<model.LeaderboardEntry> getNetworkLeaderboard() throws IOException {
        Object value = client.getLeaderboard();
        return value instanceof List<?> values
            ? values.stream().filter(model.LeaderboardEntry.class::isInstance)
                .map(model.LeaderboardEntry.class::cast).toList()
            : List.of();
    }

    @Override
    public synchronized boolean usernameExists(String username) {
        NetworkResponse response = request(NetworkOperation.USERNAME_EXISTS, username);
        return response != null && response.isSuccessful() && Boolean.TRUE.equals(response.getPayload());
    }

    @Override
    public synchronized Collection<User> getAllUsers() {
        return new ArrayList<>(cache.values());
    }

    @Override
    public synchronized void add(User user) throws IOException {
        NetworkResponse response = requestOrThrow(NetworkOperation.ADD_USER, user);
        if (!response.isSuccessful()) {
            throw new IllegalArgumentException(response.getMessage());
        }
        cache.put(user.getUsername(), user);
    }

    @Override
    public synchronized void rename(String oldUsername, String newUsername, User user) throws IOException {
        NetworkResponse response = requestOrThrow(NetworkOperation.RENAME_USER, oldUsername, newUsername, user);
        if (!response.isSuccessful()) {
            throw new IllegalArgumentException(response.getMessage());
        }
        cache.remove(oldUsername);
        cache.put(newUsername, user);
    }

    @Override
    public synchronized boolean delete(String username) throws IOException {
        NetworkResponse response = requestOrThrow(NetworkOperation.DELETE_USER, username);
        if (!response.isSuccessful()) {
            return false;
        }
        cache.remove(username);
        return Boolean.TRUE.equals(response.getPayload());
    }

    @Override
    public synchronized void save() throws IOException {
        // Kept for compatibility. Callers in Phase 3 use save(User) so unrelated cached accounts
        // are never overwritten with stale copies.
        for (User user : new ArrayList<>(cache.values())) {
            save(user);
        }
    }

    @Override
    public synchronized void save(User user) throws IOException {
        if (user == null) {
            return;
        }
        NetworkResponse response = requestOrThrow(NetworkOperation.SAVE_USER, user);
        if (!response.isSuccessful()) {
            throw new IOException(response.getMessage());
        }
        cache.put(user.getUsername(), user);
    }

    @Override
    public boolean isRemote() {
        return true;
    }

    @Override
    public boolean isAvailable() {
        boolean available = client.ping();
        if (available) {
            lastNetworkError = null;
        } else {
            lastNetworkError = "Cannot connect to " + client.getHost() + ":" + client.getPort();
        }
        return available;
    }

    @Override
    public String getLastAccessError() {
        return lastNetworkError;
    }

    public String getServerAddress() {
        return client.getHost() + ":" + client.getPort();
    }

    public void clearAuthentication() {
        client.logout();
        recoveryToken = null;
    }

    private NetworkResponse request(NetworkOperation operation, Object... arguments) {
        try {
            NetworkResponse response = client.request(operation, arguments);
            lastNetworkError = null;
            return response;
        } catch (IOException exception) {
            lastNetworkError = friendlyMessage(exception);
            return null;
        }
    }

    private NetworkResponse requestOrThrow(NetworkOperation operation, Object... arguments) throws IOException {
        try {
            NetworkResponse response = client.request(operation, arguments);
            lastNetworkError = null;
            return response;
        } catch (IOException exception) {
            lastNetworkError = friendlyMessage(exception);
            throw new IOException(lastNetworkError, exception);
        }
    }

    private NetworkResponse requestOrThrowUnauthenticated(NetworkOperation operation, Object... arguments)
        throws IOException {
        try {
            NetworkResponse response = client.requestUnauthenticated(operation, arguments);
            lastNetworkError = null;
            return response;
        } catch (IOException exception) {
            lastNetworkError = friendlyMessage(exception);
            throw new IOException(lastNetworkError, exception);
        }
    }

    private String friendlyMessage(IOException exception) {
        String detail = exception.getMessage();
        if (detail == null || detail.isBlank()) {
            detail = exception.getClass().getSimpleName();
        }
        return "Cannot reach Phase 3 server at " + client.getHost() + ":" + client.getPort()
            + " (" + detail + ")";
    }
}
