package network.server;

import model.User;
import model.UserRepository;
import network.protocol.NetworkOperation;
import network.protocol.NetworkRequest;
import network.protocol.NetworkResponse;
import network.protocol.Phase3Protocol;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Phase 3 authoritative server for account data. Multiplayer handlers are added in later stages. */
public final class PvzServer implements AutoCloseable {
    private final int port;
    private final UserRepository userRepository;
    private final ExecutorService clientExecutor;
    private volatile boolean running;
    private ServerSocket serverSocket;

    public PvzServer(int port, UserRepository userRepository) {
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Server port must be between 1 and 65535.");
        }
        this.port = port;
        this.userRepository = userRepository;
        this.clientExecutor = Executors.newCachedThreadPool(runnable -> {
            Thread thread = new Thread(runnable, "pvz-client-handler");
            thread.setDaemon(true);
            return thread;
        });
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        running = true;
        while (running) {
            try {
                Socket socket = serverSocket.accept();
                clientExecutor.submit(() -> handleClient(socket));
            } catch (IOException exception) {
                if (running) {
                    throw exception;
                }
            }
        }
    }

    private void handleClient(Socket socket) {
        try (socket;
             ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream input = new ObjectInputStream(socket.getInputStream())) {
            Object received = input.readObject();
            NetworkResponse response;
            if (!(received instanceof NetworkRequest request)) {
                response = NetworkResponse.failure("Invalid request format.");
            } else if (request.getProtocolVersion() != Phase3Protocol.VERSION) {
                response = NetworkResponse.failure("Unsupported protocol version.");
            } else {
                response = process(request);
            }
            output.writeObject(response);
            output.flush();
        } catch (EOFException ignored) {
            // Client disconnected before completing a request.
        } catch (IOException | ClassNotFoundException exception) {
            System.err.println("Client request failed: " + exception.getMessage());
        } catch (RuntimeException exception) {
            System.err.println("Client request rejected: " + exception.getMessage());
        }
    }

    private NetworkResponse process(NetworkRequest request) {
        try {
            NetworkOperation operation = request.getOperation();
            return switch (operation) {
                case PING -> NetworkResponse.success("pong");
                case AUTHENTICATE -> authenticate(
                    (String) request.argument(0),
                    (String) request.argument(1)
                );
                case FIND_USER -> findUser((String) request.argument(0));
                case USERNAME_EXISTS -> NetworkResponse.success("Username lookup complete.",
                    userRepository.usernameExists((String) request.argument(0)));
                case GET_ALL_USERS -> NetworkResponse.success("Users loaded.",
                    new ArrayList<>(userRepository.getAllUsers()));
                case ADD_USER -> addUser((User) request.argument(0));
                case RENAME_USER -> renameUser(
                    (String) request.argument(0),
                    (String) request.argument(1),
                    (User) request.argument(2)
                );
                case DELETE_USER -> deleteUser((String) request.argument(0));
                case SAVE_USER -> saveUser((User) request.argument(0));
            };
        } catch (IndexOutOfBoundsException | ClassCastException exception) {
            return NetworkResponse.failure("Invalid arguments for " + request.getOperation() + ".");
        } catch (IOException | IllegalArgumentException exception) {
            return NetworkResponse.failure(exception.getMessage());
        }
    }

    private NetworkResponse authenticate(String username, String password) {
        return userRepository.findByUsername(username)
            .<NetworkResponse>map(user -> user.checkPassword(password)
                ? NetworkResponse.success("Logged in successfully.", user)
                : NetworkResponse.failure("Password is incorrect."))
            .orElseGet(() -> NetworkResponse.failure("Username does not exist."));
    }

    private NetworkResponse findUser(String username) {
        return userRepository.findByUsername(username)
            .<NetworkResponse>map(user -> NetworkResponse.success("User found.", user))
            .orElseGet(() -> NetworkResponse.failure("Username does not exist."));
    }

    private NetworkResponse addUser(User user) throws IOException {
        userRepository.add(user);
        return NetworkResponse.success("User registered on server.");
    }

    private NetworkResponse renameUser(String oldUsername, String newUsername, User user) throws IOException {
        userRepository.rename(oldUsername, newUsername, user);
        return NetworkResponse.success("Username updated on server.");
    }

    private NetworkResponse deleteUser(String username) throws IOException {
        boolean deleted = userRepository.delete(username);
        if (!deleted) {
            return NetworkResponse.failure("Username does not exist.");
        }
        return NetworkResponse.success("Account deleted from server.", true);
    }

    private NetworkResponse saveUser(User user) throws IOException {
        if (user == null || user.getUsername() == null || user.getUsername().isBlank()) {
            return NetworkResponse.failure("Invalid user data.");
        }
        if (!userRepository.usernameExists(user.getUsername())) {
            return NetworkResponse.failure("User no longer exists on server.");
        }
        userRepository.replace(user);
        return NetworkResponse.success("User data saved on server.");
    }

    @Override
    public void close() {
        running = false;
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {
                // Closing the server is best-effort.
            }
        }
        clientExecutor.shutdownNow();
    }
}
