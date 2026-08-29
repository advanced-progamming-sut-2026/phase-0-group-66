package controller;

import model.User;
import network.client.PvzNetworkClient;
import network.game.MatchInvite;
import network.game.MatchTicket;

import java.io.IOException;
import java.util.List;

public final class NetworkMatchController {
    private final AuthController authController;
    private final PvzNetworkClient client;
    private MatchTicket activeTicket;

    public NetworkMatchController(AuthController authController, PvzNetworkClient client) {
        this.authController = authController;
        this.client = client;
    }

    public boolean isEnabled() {
        return client != null;
    }

    public ActionResult findRandom(int level) {
        return run(() -> {
            activeTicket = client.findRandomMatch(username(), level);
            return activeTicket.status().equals("MATCHED")
                ? "Random opponent found: " + activeTicket.opponent()
                : "Waiting for a random opponent...";
        });
    }

    public ActionResult challenge(String opponent, int level) {
        return run(() -> {
            activeTicket = client.challenge(username(), opponent, level);
            return "Match request sent to " + opponent + ".";
        });
    }

    public List<MatchInvite> getRequests() throws IOException {
        if (!isEnabled()) {
            return List.of();
        }
        return client.getMatchRequests(username());
    }

    public ActionResult respond(MatchInvite invite, boolean accepted) {
        return run(() -> {
            activeTicket = client.respondToMatch(username(), invite.requestId(), accepted);
            return accepted ? "Match request accepted." : "Match request rejected.";
        });
    }

    public MatchTicket pollActive() throws IOException {
        if (activeTicket == null || !isEnabled()) {
            return activeTicket;
        }
        activeTicket = client.matchStatus(username(), activeTicket.ticketId());
        return activeTicket;
    }

    public MatchTicket getActiveTicket() {
        return activeTicket;
    }

    public void clear() {
        activeTicket = null;
    }

    private String username() {
        User user = authController.getCurrentUser();
        if (user == null) {
            throw new IllegalStateException("Login is required.");
        }
        return user.getUsername();
    }

    private ActionResult run(Operation operation) {
        if (!isEnabled()) {
            return ActionResult.failure("Network play is disabled.");
        }
        try {
            return ActionResult.success(operation.run());
        } catch (IOException | IllegalArgumentException | IllegalStateException exception) {
            return ActionResult.failure(exception.getMessage());
        }
    }

    @FunctionalInterface
    private interface Operation {
        String run() throws IOException;
    }
}
