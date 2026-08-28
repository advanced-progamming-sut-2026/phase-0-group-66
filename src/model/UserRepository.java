package model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class UserRepository {
    private final Path dataDirectory;
    private final Path usersFile;
    private final Path sessionFile;
    private final Map<String, User> users;

    public UserRepository(Path dataDirectory) throws IOException {
        this(dataDirectory, true);
    }

    protected UserRepository(Path dataDirectory, boolean loadUsers) throws IOException {
        this.dataDirectory = dataDirectory;
        this.usersFile = dataDirectory.resolve("users.dat");
        this.sessionFile = dataDirectory.resolve("session.txt");
        this.users = new LinkedHashMap<>();
        Files.createDirectories(dataDirectory);
        if (loadUsers) {
            load();
        }
    }

    public synchronized Optional<User> findByUsername(String username) {
        return Optional.ofNullable(users.get(username));
    }

    public synchronized AuthenticationResult authenticate(String username, String password) {
        User user = users.get(username);
        if (user == null) {
            return AuthenticationResult.failure("Username does not exist.");
        }
        if (!user.checkPassword(password)) {
            return AuthenticationResult.failure("Password is incorrect.");
        }
        return AuthenticationResult.success(user);
    }

    public synchronized boolean usernameExists(String username) {
        return users.containsKey(username);
    }

    public synchronized Collection<User> getAllUsers() {
        return new ArrayList<>(users.values());
    }

    public synchronized void add(User user) throws IOException {
        if (usernameExists(user.getUsername())) {
            throw new IllegalArgumentException("Username already exists.");
        }
        users.put(user.getUsername(), user);
        save();
    }

    public synchronized void rename(String oldUsername, String newUsername, User user) throws IOException {
        if (!oldUsername.equals(newUsername) && usernameExists(newUsername)) {
            throw new IllegalArgumentException("Username already exists.");
        }
        users.remove(oldUsername);
        users.put(newUsername, user);
        save();
    }

    public synchronized boolean delete(String username) throws IOException {
        if (users.remove(username) == null) {
            return false;
        }
        save();
        return true;
    }

    public synchronized void save(User user) throws IOException {
        if (user == null || user.getUsername() == null || user.getUsername().isBlank()) {
            throw new IllegalArgumentException("Invalid user data.");
        }
        users.put(user.getUsername(), user);
        save();
    }

    public synchronized void replace(User user) throws IOException {
        if (user == null || user.getUsername() == null || user.getUsername().isBlank()) {
            throw new IllegalArgumentException("Invalid user data.");
        }
        if (!users.containsKey(user.getUsername())) {
            throw new IllegalArgumentException("Username does not exist.");
        }
        users.put(user.getUsername(), user);
        save();
    }

    public boolean isRemote() {
        return false;
    }

    public boolean isAvailable() {
        return true;
    }

    public String getLastAccessError() {
        return null;
    }

    public synchronized void save() throws IOException {
        Files.createDirectories(dataDirectory);
        Path temporaryFile = usersFile.resolveSibling(usersFile.getFileName() + ".tmp");
        try (ObjectOutputStream output = new ObjectOutputStream(Files.newOutputStream(temporaryFile))) {
            output.writeObject(new LinkedHashMap<>(users));
        }
        try {
            Files.move(temporaryFile, usersFile, StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException exception) {
            Files.move(temporaryFile, usersFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public synchronized void saveSession(String username) throws IOException {
        Files.createDirectories(dataDirectory);
        Files.writeString(sessionFile, username, StandardCharsets.UTF_8);
    }

    public synchronized Optional<String> loadSessionUsername() throws IOException {
        if (!Files.exists(sessionFile)) {
            return Optional.empty();
        }
        String username = Files.readString(sessionFile, StandardCharsets.UTF_8).trim();
        return username.isEmpty() ? Optional.empty() : Optional.of(username);
    }

    public synchronized void clearSession() throws IOException {
        Files.deleteIfExists(sessionFile);
    }

    @SuppressWarnings("unchecked")
    private void load() throws IOException {
        if (!Files.exists(usersFile)) {
            return;
        }
        try (ObjectInputStream input = new ObjectInputStream(Files.newInputStream(usersFile))) {
            Object stored = input.readObject();
            if (!(stored instanceof Map)) {
                throw new IOException("Invalid users data format.");
            }
            users.clear();
            users.putAll((Map<String, User>) stored);
        } catch (ClassNotFoundException exception) {
            throw new IOException("Could not read users data.", exception);
        }
    }
}
