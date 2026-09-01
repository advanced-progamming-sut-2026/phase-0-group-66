package network.server;

import model.User;
import model.UserRepository;
import model.IZombieSession;
import model.MiniGameDefinition;
import model.MiniGameType;
import model.GameProgress;
import model.LeaderboardEntry;
import network.game.MatchInvite;
import network.game.MatchReaction;
import network.game.MatchRole;
import network.game.MatchTicket;
import network.game.NetworkIZombieState;
import network.protocol.NetworkOperation;
import network.protocol.NetworkRequest;
import network.protocol.NetworkResponse;
import network.protocol.Phase3Protocol;
import network.protocol.AuthenticatedSession;
import network.protocol.SecurityProfile;

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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Phase 3 authoritative server for account data, matchmaking, and mini-games. */
public final class PvzServer implements AutoCloseable {
    private final int port;
    private final UserRepository userRepository;
    private final ExecutorService clientExecutor;
    private final ScheduledExecutorService matchTicker;
    // ponytail: one lock keeps the assignment-sized match registry correct; split locks if throughput matters.
    private final Object matchLock = new Object();
    private final Object accountLock = new Object();
    private final Map<String, Ticket> tickets = new LinkedHashMap<>();
    private final Map<String, OnlineMatch> matches = new LinkedHashMap<>();
    private final Deque<String> randomQueue = new ArrayDeque<>();
    private final Map<String, String> sessions = new ConcurrentHashMap<>();
    private final Map<String, Long> sessionLastSeen = new ConcurrentHashMap<>();
    private final Map<String, String> passwordResetTokens = new ConcurrentHashMap<>();
    private static final long SESSION_TIMEOUT_MS = 30 * 1000L;
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
        this.matchTicker = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "pvz-match-ticker");
            thread.setDaemon(true);
            return thread;
        });
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        running = true;
        matchTicker.scheduleAtFixedRate(this::tickMatches, 100, 100, TimeUnit.MILLISECONDS);
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
            if (operation == null) {
                return NetworkResponse.failure("Network operation is required.");
            }
            cleanupExpiredState();
            if (requiresSession(operation)) {
                String authToken = request.getAuthToken();
                String sessionUser = activeSessionUser(authToken);
                if (sessionUser == null) {
                    return NetworkResponse.failure("Login is required for this operation.");
                }
                String claimedUsername = claimedUsername(request);
                if (claimedUsername != null && !sessionUser.equals(claimedUsername)) {
                    return NetworkResponse.failure("The request user does not match the logged-in account.");
                }
            }
            return switch (operation) {
                case PING -> NetworkResponse.success("pong");
                case AUTHENTICATE -> authenticate(
                    (String) request.argument(0),
                    (String) request.argument(1)
                );
                case LOGOUT -> logout(request.getAuthToken());
                case FIND_USER -> findUser(request, (String) request.argument(0));
                case GET_SECURITY_PROFILE -> getSecurityProfile((String) request.argument(0));
                case VERIFY_SECURITY_ANSWER -> verifySecurityAnswer(
                    (String) request.argument(0), (String) request.argument(1)
                );
                case RESET_PASSWORD -> resetPassword(
                    (String) request.argument(0), (String) request.argument(1), (String) request.argument(2)
                );
                case USERNAME_EXISTS -> NetworkResponse.success("Username lookup complete.",
                    userRepository.usernameExists((String) request.argument(0)));
                case GET_ALL_USERS -> NetworkResponse.failure("Listing all user accounts is not available.");
                case ADD_USER -> addUser((User) request.argument(0));
                case RENAME_USER -> renameUser(
                    request.getAuthToken(),
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
                case SUBMIT_MINIGAME_SCORE -> submitMiniGameScore(
                    (String) request.argument(0), (MiniGameType) request.argument(1),
                    intValue(request.argument(2)), intValue(request.argument(3))
                );
                case SUBMIT_SCORED_SCORE -> submitScoredScore(
                    (String) request.argument(0), intValue(request.argument(1))
                );
                case GET_LEADERBOARD -> getLeaderboard();
            };
        } catch (IndexOutOfBoundsException | ClassCastException exception) {
            return NetworkResponse.failure("Invalid arguments for " + request.getOperation() + ".");
        } catch (IOException | IllegalArgumentException | IllegalStateException exception) {
            return NetworkResponse.failure(exception.getMessage());
        }
    }

    private NetworkResponse authenticate(String username, String password) {
        synchronized (accountLock) {
            return userRepository.findByUsername(username)
                .<NetworkResponse>map(user -> {
                    if (!user.checkPassword(password)) {
                        return NetworkResponse.failure("Password is incorrect.");
                    }
                    String token = UUID.randomUUID().toString();
                    sessions.put(token, user.getUsername());
                    sessionLastSeen.put(token, System.currentTimeMillis());
                    return NetworkResponse.success("Logged in successfully.",
                        new AuthenticatedSession(token, user.copyForRollback()));
                })
                .orElseGet(() -> NetworkResponse.failure("Username does not exist."));
        }
    }

    private NetworkResponse findUser(NetworkRequest request, String username) {
        String authToken = request.getAuthToken();
        String sessionUser = activeSessionUser(authToken);
        if (sessionUser == null || !sessionUser.equals(username)) {
            return getSecurityProfile(username);
        }
        return userRepository.findByUsername(username)
            .<NetworkResponse>map(user -> NetworkResponse.success("User found.", user))
            .orElseGet(() -> NetworkResponse.failure("Username does not exist."));
    }

    private NetworkResponse getSecurityProfile(String username) {
        return userRepository.findByUsername(username)
            .<NetworkResponse>map(user -> NetworkResponse.success("Security profile loaded.",
                new SecurityProfile(user.getUsername(), user.getNickname(), user.getEmail(),
                    user.getGender(), user.getSecurityQuestion())))
            .orElseGet(() -> NetworkResponse.failure("Username does not exist."));
    }

    private NetworkResponse verifySecurityAnswer(String username, String answer) {
        if (!userRepository.verifySecurityAnswer(username, answer)) {
            return NetworkResponse.failure("Security answer is incorrect.");
        }
        String token = UUID.randomUUID().toString();
        passwordResetTokens.put(token, username);
        return NetworkResponse.success("Security answer verified.", token);
    }

    private NetworkResponse resetPassword(String username, String newPassword, String token) throws IOException {
        if (token == null || !username.equals(passwordResetTokens.get(token))) {
            return NetworkResponse.failure("Password reset authorization is invalid or expired.");
        }
        userRepository.resetPassword(username, newPassword);
        passwordResetTokens.remove(token);
        return NetworkResponse.success("Password reset successfully.");
    }

    private NetworkResponse logout(String authToken) {
        if (authToken != null) {
            sessions.remove(authToken);
            sessionLastSeen.remove(authToken);
        }
        return NetworkResponse.success("Logged out from server.");
    }

    private boolean requiresSession(NetworkOperation operation) {
        return switch (operation) {
            case PING, AUTHENTICATE, FIND_USER, GET_SECURITY_PROFILE, VERIFY_SECURITY_ANSWER,
                RESET_PASSWORD, USERNAME_EXISTS, ADD_USER, GET_LEADERBOARD -> false;
            default -> true;
        };
    }

    private String claimedUsername(NetworkRequest request) {
        return switch (request.getOperation()) {
            case RENAME_USER, MATCH_CHALLENGE -> (String) request.argument(0);
            case ADD_USER, SAVE_USER -> request.getOperation() == NetworkOperation.ADD_USER
                ? null : ((User) request.argument(0)).getUsername();
            case DELETE_USER, MATCH_RANDOM, MATCH_REQUESTS, MATCH_RESPONSE, MATCH_STATUS,
                MATCH_STATE, MATCH_ACTION, MATCH_REACTION, SUBMIT_MINIGAME_SCORE,
                SUBMIT_SCORED_SCORE -> (String) request.argument(0);
            default -> null;
        };
    }

    private NetworkResponse addUser(User user) throws IOException {
        if (user == null || user.getUsername() == null || user.getUsername().isBlank()
            || user.getNickname() == null || user.getEmail() == null || user.getGender() == null
            || user.getSecurityQuestion() == null) {
            return NetworkResponse.failure("Invalid registration data.");
        }
        userRepository.add(user);
        return NetworkResponse.success("User registered on server.");
    }

    private NetworkResponse renameUser(
        String authToken, String oldUsername, String newUsername, User user
    ) throws IOException {
        if (oldUsername == null || newUsername == null || user == null
            || !newUsername.equals(user.getUsername())) {
            return NetworkResponse.failure("Invalid username change data.");
        }
        synchronized (matchLock) {
            if (hasActiveMatch(oldUsername)) {
                return NetworkResponse.failure("Finish the active match before changing username.");
            }
            if (hasWaitingTicket(oldUsername)) {
                return NetworkResponse.failure("Cancel the pending match request before changing username.");
            }
        }
        synchronized (accountLock) {
            if (!userRepository.usernameExists(oldUsername)) {
                return NetworkResponse.failure("Username does not exist.");
            }
            userRepository.rename(oldUsername, newUsername, user);
        }
        if (authToken != null && !authToken.isBlank()) {
            sessions.put(authToken, newUsername);
        }
        return NetworkResponse.success("Username updated on server.");
    }

    private NetworkResponse deleteUser(String username) throws IOException {
        synchronized (matchLock) {
            if (hasActiveMatch(username) || hasWaitingTicket(username)) {
                return NetworkResponse.failure("Finish or cancel the active match before deleting the account.");
            }
        }
        boolean deleted;
        synchronized (accountLock) {
            deleted = userRepository.delete(username);
        }
        if (!deleted) {
            return NetworkResponse.failure("Username does not exist.");
        }
        sessions.entrySet().removeIf(entry -> username.equals(entry.getValue()));
        sessionLastSeen.entrySet().removeIf(entry -> !sessions.containsKey(entry.getKey()));
        return NetworkResponse.success("Account deleted from server.", true);
    }

    private NetworkResponse saveUser(User user) throws IOException {
        if (user == null || user.getUsername() == null || user.getUsername().isBlank()) {
            return NetworkResponse.failure("Invalid user data.");
        }
        synchronized (accountLock) {
            if (!userRepository.usernameExists(user.getUsername())) {
                return NetworkResponse.failure("User no longer exists on server.");
            }
            userRepository.replace(user);
        }
        return NetworkResponse.success("User data saved on server.");
    }

    private NetworkResponse randomMatch(String username, int level) {
        synchronized (matchLock) {
            NetworkResponse validation = validatePlayer(username, level);
            if (validation != null) {
                return validation;
            }
            if (hasActiveMatch(username)) {
                return NetworkResponse.failure("You are already in an active match.");
            }
            Ticket existing = waitingRandomTicket(username);
            if (existing != null) {
                return NetworkResponse.success("Waiting for a random opponent.", existing.view(username));
            }
            Ticket ticket = new Ticket(username, null, level);
            tickets.put(ticket.id, ticket);
            for (String ticketId : new ArrayList<>(randomQueue)) {
                Ticket opponent = tickets.get(ticketId);
                if (opponent != null && opponent.status.equals("WAITING")
                    && opponent.level == level && !opponent.requester.equals(username)) {
                    randomQueue.remove(ticketId);
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
            if (hasActiveMatch(username)) {
                return NetworkResponse.failure("You are already in an active match.");
            }
            if (opponent == null || opponent.isBlank() || username.equals(opponent)) {
                return NetworkResponse.failure("Choose a different opponent username.");
            }
            if (!userRepository.usernameExists(opponent)) {
                return NetworkResponse.failure("Opponent username does not exist.");
            }
            if (!isOnline(opponent)) {
                return NetworkResponse.failure("Opponent is not online.");
            }
            if (hasActiveMatch(opponent)) {
                return NetworkResponse.failure("Opponent is already in an active match.");
            }
            Ticket existing = waitingTicket(username, opponent);
            if (existing != null) {
                return NetworkResponse.success("Match request already sent.", existing.view(username));
            }
            if (waitingRandomTicket(username) != null) {
                return NetworkResponse.failure("Cancel the pending random match before sending a challenge.");
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
            if (!isOnline(ticket.requester) || hasActiveMatch(ticket.requester)) {
                ticket.status = "EXPIRED";
                return NetworkResponse.failure("The requesting player is no longer available.");
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
            if (action.equals("advance")) {
                return NetworkResponse.success("The server advances the match clock.", stateFor(match, username));
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
            List<String> allowed = switch (normalizedCategory) {
                case "message" -> List.of("Nice move!", "Good luck!", "Well played!");
                case "emoji" -> List.of("😀", "🔥", "😮");
                case "sticker" -> List.of("APPLAUSE", "LAUGH", "BOOM");
                default -> List.of();
            };
            if (!(normalizedCategory.equals("message") || normalizedCategory.equals("emoji")
                || normalizedCategory.equals("sticker"))) {
                return NetworkResponse.failure("Reaction category is not supported.");
            }
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

    private NetworkResponse submitMiniGameScore(String username, MiniGameType type,
                                                  int level, int score) throws IOException {
        if (username == null || username.isBlank() || type == null || level < 1 || level > 3
            || score < 0 || !userRepository.usernameExists(username)) {
            return NetworkResponse.failure("Invalid mini-game score submission.");
        }
        synchronized (accountLock) {
            User user = userRepository.findByUsername(username).orElseThrow();
            GameProgress progress = user.getProgress();
            int previous = progress.getBestMiniGameScore();
            if (score > previous) {
                progress.updateBestScore(score);
                userRepository.replace(user);
            }
            return NetworkResponse.success("Score saved on server.", Math.max(previous, score));
        }
    }

    private NetworkResponse submitScoredScore(String username, int score) throws IOException {
        if (username == null || username.isBlank() || score < 0
            || !userRepository.usernameExists(username)) {
            return NetworkResponse.failure("Invalid scored-game score submission.");
        }
        synchronized (accountLock) {
            User user = userRepository.findByUsername(username).orElseThrow();
            GameProgress progress = user.getProgress();
            int previous = progress.getBestMeowPoints();
            if (score > previous) {
                progress.updateBestMeowPoints(score);
                userRepository.replace(user);
            }
            return NetworkResponse.success("Scored-game result saved on server.",
                Math.max(previous, score));
        }
    }

    private NetworkResponse getLeaderboard() {
        synchronized (accountLock) {
            ArrayList<LeaderboardEntry> entries = new ArrayList<>();
            for (User user : userRepository.getAllUsers()) {
                GameProgress progress = user.getProgress();
                entries.add(new LeaderboardEntry(user.getUsername(),
                    progress.getLastChapterNumber(), progress.getLastLevelNumber(),
                    progress.getCompletedMiniGames(), progress.getCompletedDailyQuests(),
                    progress.getCompletedOtherQuests(), progress.getBestMeowPoints()));
            }
            entries.sort(java.util.Comparator.comparingInt(LeaderboardEntry::bestScore).reversed()
                .thenComparing(LeaderboardEntry::username));
            return NetworkResponse.success("Leaderboard loaded from server.", List.copyOf(entries));
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
        return waitingTicket(username, null);
    }

    private Ticket waitingTicket(String username, String target) {
        for (Ticket ticket : tickets.values()) {
            if (ticket.status.equals("WAITING") && ticket.requester.equals(username)
                && (target == null || target.equals(ticket.target))) {
                return ticket;
            }
        }
        return null;
    }

    private Ticket waitingRandomTicket(String username) {
        for (Ticket ticket : tickets.values()) {
            if (ticket.status.equals("WAITING") && ticket.requester.equals(username)
                && ticket.target == null) {
                return ticket;
            }
        }
        return null;
    }

    private void cleanupExpiredState() {
        synchronized (matchLock) {
            long now = System.currentTimeMillis();
            sessionLastSeen.entrySet().removeIf(entry -> {
                boolean expired = now - entry.getValue() > SESSION_TIMEOUT_MS;
                if (expired) {
                    sessions.remove(entry.getKey());
                }
                return expired;
            });
            tickets.entrySet().removeIf(entry -> entry.getValue().expired(now));
            randomQueue.removeIf(ticketId -> !tickets.containsKey(ticketId));
            matches.entrySet().removeIf(entry -> now - entry.getValue().createdAt > 30 * 60 * 1000L);
        }
    }

    private void tickMatches() {
        synchronized (matchLock) {
            for (OnlineMatch match : matches.values()) {
                if (!match.session.isFinished()) {
                    try {
                        match.session.advanceTime(1);
                    } catch (IllegalStateException ignored) {
                        // A match may finish between the check and the tick.
                    }
                }
            }
        }
    }

    private String activeSessionUser(String authToken) {
        if (authToken == null || authToken.isBlank()) {
            return null;
        }
        String username = sessions.get(authToken);
        Long lastSeen = sessionLastSeen.get(authToken);
        if (username == null || lastSeen == null
            || System.currentTimeMillis() - lastSeen > SESSION_TIMEOUT_MS) {
            sessions.remove(authToken);
            sessionLastSeen.remove(authToken);
            return null;
        }
        sessionLastSeen.put(authToken, System.currentTimeMillis());
        return username;
    }

    private boolean isOnline(String username) {
        long now = System.currentTimeMillis();
        return sessions.entrySet().stream().anyMatch(entry -> username.equals(entry.getValue())
            && now - sessionLastSeen.getOrDefault(entry.getKey(), 0L) <= SESSION_TIMEOUT_MS);
    }

    private boolean hasActiveMatch(String username) {
        return matches.values().stream().anyMatch(match -> !match.session.isFinished()
            && match.members.containsKey(username));
    }

    private boolean hasWaitingTicket(String username) {
        return tickets.values().stream().anyMatch(ticket -> ticket.status.equals("WAITING")
            && (username.equals(ticket.requester) || username.equals(ticket.target)));
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
                card.damage(), card.speed(), card.remainingCooldownTicks())).toList();
        List<NetworkIZombieState.Plant> plants = match.session.getPlantViews().stream()
            .map(plant -> new NetworkIZombieState.Plant(plant.type(), plant.row(), plant.column(),
                plant.health(), plant.damage())).toList();
        List<NetworkIZombieState.Zombie> zombies = match.session.getZombieViews().stream()
            .map(zombie -> new NetworkIZombieState.Zombie(zombie.type(), zombie.row(), zombie.column(),
                zombie.health(), zombie.maximumHealth(), zombie.damage(), zombie.speed())).toList();
        MatchRole role = match.members.get(username);
        MatchRole winner = match.session.isFinished()
            ? (match.session.isPlantVictory() ? MatchRole.PLANTS : MatchRole.ZOMBIES) : null;
        boolean won = winner == role;
        boolean lost = winner != null && winner != role;
        return new NetworkIZombieState(match.id, username, match.opponentOf(username),
            role, match.level, match.session.getScore(), match.session.getElapsedTicks(),
            won, lost, match.session.getSun(), match.session.getPlantSun(),
            match.session.getBrainsEaten(), match.session.getBrains(), cards, plants, zombies,
            match.reactions, winner);
    }

    private static final class OnlineMatch {
        private final String id;
        private final int level;
        private final IZombieSession session;
        private final long createdAt = System.currentTimeMillis();
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
        private static final long WAIT_TIMEOUT_MS = 2 * 60 * 1000L;
        private final String id = UUID.randomUUID().toString();
        private final String requester;
        private final String target;
        private final int level;
        private final long createdAt = System.currentTimeMillis();
        private final Map<String, MatchRole> roles = new LinkedHashMap<>();
        private final Map<String, String> opponents = new LinkedHashMap<>();
        private String status = "WAITING";
        private String opponent;
        private String matchId;

        private boolean expired(long now) {
            return status.equals("WAITING") && now - createdAt > WAIT_TIMEOUT_MS;
        }

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
        matchTicker.shutdownNow();
        sessions.clear();
        sessionLastSeen.clear();
        passwordResetTokens.clear();
    }
}
