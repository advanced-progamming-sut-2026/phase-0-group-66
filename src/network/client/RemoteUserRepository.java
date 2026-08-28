package network.client;

import model.AuthenticationResult;
import model.User;
import model.UserRepository;
import network.protocol.NetworkOperation;
import network.protocol.NetworkResponse;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

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

    public RemoteUserRepository(Path localSessionDirectory, PvzNetworkClient client) throws IOException {
        super(localSessionDirectory, false);
        this.client = client;
    }

    @Override
    public synchronized AuthenticationResult authenticate(String username, String password) {
        NetworkResponse response = request(NetworkOperation.AUTHENTICATE, username, password);
        if (response == null) {
            return AuthenticationResult.failure(lastNetworkError == null
                ? "Could not contact the server." : lastNetworkError);
        }
        if (!response.isSuccessful() || !(response.getPayload() instanceof User user)) {
            return AuthenticationResult.failure(response.getMessage());
        }
        cache.put(user.getUsername(), user);
        return AuthenticationResult.success(user);
    }

    @Override
    public synchronized Optional<User> findByUsername(String username) {
        NetworkResponse response = request(NetworkOperation.FIND_USER, username);
        if (response == null || !response.isSuccessful() || !(response.getPayload() instanceof User user)) {
            return Optional.empty();
        }
        cache.put(user.getUsername(), user);
        return Optional.of(user);
    }

    @Override
    public synchronized boolean usernameExists(String username) {
        NetworkResponse response = request(NetworkOperation.USERNAME_EXISTS, username);
        return response != null && response.isSuccessful() && Boolean.TRUE.equals(response.getPayload());
    }

    @Override
    public synchronized Collection<User> getAllUsers() {
        NetworkResponse response = request(NetworkOperation.GET_ALL_USERS);
        if (response == null || !response.isSuccessful() || !(response.getPayload() instanceof Collection<?> values)) {
            return new ArrayList<>();
        }
        ArrayList<User> users = new ArrayList<>();
        for (Object value : values) {
            if (value instanceof User user) {
                users.add(user);
                cache.put(user.getUsername(), user);
            }
        }
        return users;
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

    private String friendlyMessage(IOException exception) {
        String detail = exception.getMessage();
        if (detail == null || detail.isBlank()) {
            detail = exception.getClass().getSimpleName();
        }
        return "Cannot reach Phase 3 server at " + client.getHost() + ":" + client.getPort()
            + " (" + detail + ")";
    }
}
