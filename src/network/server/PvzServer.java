package network.server;

import model.User;
import model.UserRepository;
import model.IZombieSession;
import model.MiniGameDefinition;
import model.MiniGameType;
import network.game.MatchInvite;
import network.game.MatchReaction;
import network.game.MatchRole;
import network.game.MatchTicket;
import network.game.NetworkIZombieState;
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
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Phase 3 authoritative server for account data. Multiplayer handlers are added in later stages. */
public final class PvzServer implements AutoCloseable {
    private final int port;
    private final UserRepository userRepository;
    private final ExecutorService clientExecutor;
    // ponytail: one lock keeps the assignment-sized match registry correct; split locks if throughput matters.
    private final Object matchLock = new Object();
    private final Map<String, Ticket> tickets = new LinkedHashMap<>();
    private final Map<String, OnlineMatch> matches = new LinkedHashMap<>();
    private final Deque<String> randomQueue = new ArrayDeque<>();
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
                case MATCH_RANDOM -> randomMatch((String) request.argument(0), intValue(request.argument(1)));
                case MATCH_CHALLENGE -> challengeMatch(
                    (String) request.argument(0), (String) request.argument(1), intValue(request.argument(2))
                );
                case MATCH_REQUESTS -> matchRequests((String) request.argument(0));
                case MATCH_RESPONSE -> respondToMatch(
                    (String) request.argument(0), (String) request.argument(1), (Boolean) request.argument(2)
                );
                case MATCH_STATUS -> matchStatus((String) request.argument(0), (String) request.argument(1));
                case MATCH_STATE -> matchState((String) request.argument(0), (String) request.argument(1));
                case MATCH_ACTION -> matchAction(
                    (String) request.argument(0), (String) request.argument(1), (String) request.argument(2)
                );
                case MATCH_REACTION -> matchReaction(
                    (String) request.argument(0), (String) request.argument(1),
                    (String) request.argument(2), (String) request.argument(3)
                );
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

    private NetworkResponse randomMatch(String username, int level) {
        synchronized (matchLock) {
            NetworkResponse validation = validatePlayer(username, level);
            if (validation != null) {
                return validation;
            }
            Ticket existing = waitingTicket(username);
            if (existing != null) {
                return NetworkResponse.success("Waiting for a random opponent.", existing.view(username));
            }
            Ticket ticket = new Ticket(username, null, level);
            tickets.put(ticket.id, ticket);
            while (!randomQueue.isEmpty()) {
                Ticket opponent = tickets.get(randomQueue.removeFirst());
                if (opponent != null && opponent.status.equals("WAITING")
                    && opponent.level == level && !opponent.requester.equals(username)) {
                    startMatch(opponent, ticket, opponent.requester, username);
                    return NetworkResponse.success("Random opponent found.", ticket.view(username));
                }
            }
            randomQueue.addLast(ticket.id);
            return NetworkResponse.success("Waiting for a random opponent.", ticket.view(username));
        }
    }

    private NetworkResponse challengeMatch(String username, String opponent, int level) {
        synchronized (matchLock) {
            NetworkResponse validation = validatePlayer(username, level);
            if (validation != null) {
                return validation;
            }
            if (opponent == null || opponent.isBlank() || username.equals(opponent)) {
                return NetworkResponse.failure("Choose a different opponent username.");
            }
            if (!userRepository.usernameExists(opponent)) {
                return NetworkResponse.failure("Opponent username does not exist.");
            }
            Ticket ticket = new Ticket(username, opponent, level);
            tickets.put(ticket.id, ticket);
            return NetworkResponse.success("Match request sent.", ticket.view(username));
        }
    }

    private NetworkResponse matchRequests(String username) {
        synchronized (matchLock) {
            ArrayList<MatchInvite> result = new ArrayList<>();
            for (Ticket ticket : tickets.values()) {
                if (ticket.status.equals("WAITING") && username.equals(ticket.target)) {
                    result.add(new MatchInvite(ticket.id, ticket.requester, ticket.level));
                }
            }
            return NetworkResponse.success("Match requests loaded.", result);
        }
    }

    private NetworkResponse respondToMatch(String username, String ticketId, boolean accepted) {
        synchronized (matchLock) {
            Ticket ticket = tickets.get(ticketId);
            if (ticket == null || !username.equals(ticket.target) || !ticket.status.equals("WAITING")) {
                return NetworkResponse.failure("Match request is no longer available.");
            }
            if (!accepted) {
                ticket.status = "REJECTED";
                return NetworkResponse.success("Match request rejected.", ticket.view(username));
            }
            startMatch(ticket, ticket, ticket.requester, username);
            return NetworkResponse.success("Match request accepted.", ticket.view(username));
        }
    }

    private NetworkResponse matchStatus(String username, String ticketId) {
        synchronized (matchLock) {
            Ticket ticket = tickets.get(ticketId);
            if (ticket == null || !ticket.belongsTo(username)) {
                return NetworkResponse.failure("Match ticket was not found.");
            }
            return NetworkResponse.success("Match status loaded.", ticket.view(username));
        }
    }

    private NetworkResponse matchState(String username, String matchId) {
        synchronized (matchLock) {
            OnlineMatch match = matches.get(matchId);
            if (match == null || !match.members.containsKey(username)) {
                return NetworkResponse.failure("Match was not found.");
            }
            return NetworkResponse.success("Match state loaded.", stateFor(match, username));
        }
    }

    private NetworkResponse matchAction(String username, String matchId, String command) {
        synchronized (matchLock) {
            OnlineMatch match = matches.get(matchId);
            if (match == null || !match.members.containsKey(username)) {
                return NetworkResponse.failure("Match was not found.");
            }
            if (command == null || command.isBlank()) {
                return NetworkResponse.failure("Match action cannot be empty.");
            }
            String[] tokens = command.trim().split("\\s+");
            String action = tokens[0].toLowerCase();
            MatchRole role = match.members.get(username);
            if (role == MatchRole.ZOMBIES && !(action.equals("deploy") || action.equals("placezombie")
                || action.equals("advance"))) {
                return NetworkResponse.failure("The zombie player can deploy zombies or advance the game.");
            }
            if (role == MatchRole.PLANTS && !(action.equals("plant") || action.equals("placeplant")
                || action.equals("removeplant"))) {
                return NetworkResponse.failure("The plant player can place or remove plants.");
            }
            if (role == MatchRole.PLANTS && action.equals("advance")) {
                return NetworkResponse.success("The zombie player advances the match.", stateFor(match, username));
            }
            try {
                match.session.execute(action, Arrays.asList(tokens).subList(1, tokens.length));
                return NetworkResponse.success("Match action applied.", stateFor(match, username));
            } catch (IllegalArgumentException | IllegalStateException exception) {
                return NetworkResponse.failure(exception.getMessage());
            }
        }
    }

    private NetworkResponse matchReaction(String username, String matchId, String category, String value) {
        synchronized (matchLock) {
            OnlineMatch match = matches.get(matchId);
            if (match == null || !match.members.containsKey(username)) {
                return NetworkResponse.failure("Match was not found.");
            }
            String normalizedCategory = category == null ? "" : category.toLowerCase();
            if (!(normalizedCategory.equals("message") || normalizedCategory.equals("emoji"))) {
                return NetworkResponse.failure("Reaction category must be message or emoji.");
            }
            List<String> allowed = normalizedCategory.equals("message")
                ? List.of("Nice move!", "Good luck!", "Well played!")
                : List.of("😀", "🔥", "😮");
            if (!allowed.contains(value)) {
                return NetworkResponse.failure("That reaction is not available.");
            }
            if (match.reactions.size() >= 24) {
                match.reactions.remove(0);
            }
            match.reactions.add(new MatchReaction(username, normalizedCategory, value));
            return NetworkResponse.success("Reaction sent.", stateFor(match, username));
        }
    }

    private NetworkResponse validatePlayer(String username, int level) {
        if (username == null || username.isBlank() || !userRepository.usernameExists(username)) {
            return NetworkResponse.failure("Player username does not exist.");
        }
        if (level < 1 || level > 3) {
            return NetworkResponse.failure("I, Zombie level must be between 1 and 3.");
        }
        return null;
    }

    private Ticket waitingTicket(String username) {
        for (Ticket ticket : tickets.values()) {
            if (ticket.status.equals("WAITING") && ticket.requester.equals(username)) {
                return ticket;
            }
        }
        return null;
    }

    private void startMatch(Ticket first, Ticket second, String zombies, String plants) {
        MiniGameDefinition definition = new MiniGameDefinition(
            MiniGameType.I_ZOMBIE, false, "Play I, Zombie online.", "deploy"
        );
        OnlineMatch match = new OnlineMatch(UUID.randomUUID().toString(), first.level,
            new IZombieSession(definition, first.level, true));
        match.members.put(zombies, MatchRole.ZOMBIES);
        match.members.put(plants, MatchRole.PLANTS);
        matches.put(match.id, match);
        first.status = "MATCHED";
        first.matchId = match.id;
        first.opponent = plants;
        first.opponents.put(zombies, plants);
        first.opponents.put(plants, zombies);
        first.roles.put(zombies, MatchRole.ZOMBIES);
        first.roles.put(plants, MatchRole.PLANTS);
        second.status = "MATCHED";
        second.matchId = match.id;
        second.opponent = zombies;
        second.opponents.put(zombies, plants);
        second.opponents.put(plants, zombies);
        second.roles.put(zombies, MatchRole.ZOMBIES);
        second.roles.put(plants, MatchRole.PLANTS);
    }

    private int intValue(Object value) {
        if (!(value instanceof Number number)) {
            throw new ClassCastException();
        }
        return number.intValue();
    }

    private NetworkIZombieState stateFor(OnlineMatch match, String username) {
        List<NetworkIZombieState.Card> cards = match.session.getCardViews().stream()
            .map(card -> new NetworkIZombieState.Card(card.key(), card.type(), card.cost(), card.health(),
                card.damage(), card.speed())).toList();
        List<NetworkIZombieState.Plant> plants = match.session.getPlantViews().stream()
            .map(plant -> new NetworkIZombieState.Plant(plant.type(), plant.row(), plant.column(),
                plant.health(), plant.damage())).toList();
        List<NetworkIZombieState.Zombie> zombies = match.session.getZombieViews().stream()
            .map(zombie -> new NetworkIZombieState.Zombie(zombie.type(), zombie.row(), zombie.column(),
                zombie.health(), zombie.maximumHealth(), zombie.damage(), zombie.speed())).toList();
        return new NetworkIZombieState(match.id, username, match.opponentOf(username),
            match.members.get(username), match.level, match.session.getScore(), match.session.getElapsedTicks(),
            match.session.isWon(), match.session.isLost(), match.session.getSun(), match.session.getPlantSun(),
            match.session.getBrainsEaten(), match.session.getBrains(), cards, plants, zombies, match.reactions);
    }

    private static final class OnlineMatch {
        private final String id;
        private final int level;
        private final IZombieSession session;
        private final Map<String, MatchRole> members = new LinkedHashMap<>();
        private final ArrayList<MatchReaction> reactions = new ArrayList<>();

        private OnlineMatch(String id, int level, IZombieSession session) {
            this.id = id;
            this.level = level;
            this.session = session;
        }

        private String opponentOf(String username) {
            for (String member : members.keySet()) {
                if (!member.equals(username)) {
                    return member;
                }
            }
            return "";
        }
    }

    private static final class Ticket {
        private final String id = UUID.randomUUID().toString();
        private final String requester;
        private final String target;
        private final int level;
        private final Map<String, MatchRole> roles = new LinkedHashMap<>();
        private final Map<String, String> opponents = new LinkedHashMap<>();
        private String status = "WAITING";
        private String opponent;
        private String matchId;

        private Ticket(String requester, String target, int level) {
            this.requester = requester;
            this.target = target;
            this.level = level;
        }

        private boolean belongsTo(String username) {
            return requester.equals(username) || username.equals(target);
        }

        private MatchTicket view(String username) {
            return new MatchTicket(id, status, opponents.getOrDefault(username, opponent),
                matchId, level, roles.get(username));
        }
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
