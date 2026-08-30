package pvz.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import controller.ActionResult;
import network.game.MatchInvite;
import network.game.MatchTicket;
import pvz.PvzApplication;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public final class NetworkScreen extends AuthenticatedUiScreen {
    private final TextField opponent;
    private final TextField level;
    private final Label status;
    private final Table requests;
    private float refreshTimer;
    private boolean launched;
    private final Set<String> announcedRequests = new HashSet<>();
    private TextButton randomButton;
    private TextButton challengeButton;
    private final ExecutorService networkExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "pvz-network-screen");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicBoolean refreshInFlight = new AtomicBoolean();
    private final AtomicBoolean matchRequestInFlight = new AtomicBoolean();
    private volatile boolean disposed;

    public NetworkScreen(PvzApplication app) {
        super(app);
        opponent = textField("Opponent username");
        level = textField("Level 1-3");
        level.setText("1");
        status = statusLabel();
        requests = new Table();
        buildUi();
    }

    @Override
    public void render(float delta) {
        refreshTimer += Math.min(delta, 0.25f);
        if (refreshTimer >= 0.75f && !launched && refreshInFlight.compareAndSet(false, true)) {
            refreshTimer = 0f;
            loadNetworkState();
        }
        super.render(delta);
    }

    private void buildUi() {
        Table screen = new Table();
        screen.top().pad(24f, 54f, 16f, 54f);
        screen.add(titleBar("NETWORK")).width(1170f).height(56f).padBottom(18f);
        screen.row();

        Table panel = theme.settingsCardPanel(24f);
        panel.add(theme.heading("I, ZOMBIE ONLINE")).growX().height(50f);
        panel.row().padTop(8f);
        Label help = theme.bodyLabel("Play as the plant or zombie side. Random matching and direct requests are available.");
        help.setWrap(true);
        panel.add(help).width(760f).height(48f);
        panel.row().padTop(16f);

        randomButton = theme.primaryButton("FIND RANDOM OPPONENT");
        UiActions.onClick(randomButton, () -> {
            int selected = selectedLevel();
            if (selected > 0) {
                requestMatch(() -> app.services().network().findRandom(selected));
            }
        });
        panel.add(randomButton).width(360f).height(52f);
        panel.row().padTop(12f);

        Table direct = new Table();
        direct.add(opponent).width(330f).height(48f).padRight(10f);
        direct.add(level).width(110f).height(48f).padRight(10f);
        challengeButton = theme.secondaryButton("CHALLENGE");
        UiActions.onClick(challengeButton, () -> {
            int selected = selectedLevel();
            if (selected > 0) {
                requestMatch(() -> app.services().network()
                    .challenge(opponent.getText().trim(), selected));
            }
        });
        direct.add(challengeButton).width(190f).height(48f);
        panel.add(direct);
        panel.row().padTop(10f);
        TextButton couch = theme.tertiaryButton("COUCH PLAY - ONE DEVICE");
        UiActions.onClick(couch, () -> {
            if (!app.startCouchIZombie(selectedLevel())) {
                theme.showError(status, "Could not start Couch Play.");
            }
        });
        panel.add(couch).width(360f).height(48f);
        panel.row().padTop(12f);
        panel.add(theme.heading("MATCH REQUESTS")).growX().height(36f);
        panel.row().padTop(6f);
        panel.add(requests).width(650f).height(90f);
        panel.row().padTop(12f);
        panel.add(status).width(760f).height(46f);
        panel.row().padTop(12f);

        TextButton back = theme.secondaryButton("BACK");
        UiActions.onClick(back, () -> {
            app.services().network().clear();
            app.showMainMenu();
        });
        panel.add(back).width(220f).height(48f);

        screen.add(panel).width(850f).height(480f).center();
        addScrollable(screen);
    }

    private void requestMatch(Supplier<ActionResult> request) {
        if (!matchRequestInFlight.compareAndSet(false, true)) {
            return;
        }
        randomButton.setDisabled(true);
        challengeButton.setDisabled(true);
        networkExecutor.submit(() -> {
            try {
                ActionResult result = request.get();
                Gdx.app.postRunnable(() -> finishMatchRequest(result));
            } catch (RuntimeException exception) {
                Gdx.app.postRunnable(() -> {
                    matchRequestInFlight.set(false);
                    if (!disposed) {
                        randomButton.setDisabled(false);
                        challengeButton.setDisabled(false);
                        theme.showError(status, exception.getMessage());
                    }
                });
            }
        });
    }

    private void finishMatchRequest(ActionResult result) {
        matchRequestInFlight.set(false);
        if (disposed) {
            return;
        }
        randomButton.setDisabled(false);
        challengeButton.setDisabled(false);
        showResult(result);
    }

    private void showResult(ActionResult result) {
        if (result.isSuccessful()) {
            theme.showSuccess(status, result.getMessage());
        } else {
            theme.showError(status, result.getMessage());
        }
    }

    private void pollMatch() {
        try {
            MatchTicket ticket = app.services().network().pollActive();
            if (ticket != null && ticket.isMatched()) {
                launched = app.startOnlineIZombie(ticket);
                if (launched) {
                    app.services().network().clear();
                }
            }
        } catch (IOException exception) {
            theme.showError(status, exception.getMessage());
        }
    }

    private void loadNetworkState() {
        networkExecutor.submit(() -> {
            try {
                MatchTicket ticket = app.services().network().pollActive();
                List<MatchInvite> invites = app.services().network().getRequests();
                Gdx.app.postRunnable(() -> {
                    refreshInFlight.set(false);
                    if (disposed) {
                        return;
                    }
                    if (ticket != null && ticket.isMatched()) {
                        launched = app.startOnlineIZombie(ticket);
                        if (launched) {
                            app.services().network().clear();
                        }
                    }
                    renderRequests(invites);
                });
            } catch (IOException exception) {
                Gdx.app.postRunnable(() -> {
                    refreshInFlight.set(false);
                    if (disposed) {
                        return;
                    }
                    theme.showError(status, exception.getMessage());
                });
            }
        });
    }

    private void refreshRequests() {
        try {
            renderRequests(app.services().network().getRequests());
        } catch (IOException exception) {
            theme.showError(status, exception.getMessage());
        }
    }

    private void renderRequests(List<MatchInvite> invites) {
        requests.clearChildren();
        for (MatchInvite invite : invites) {
                requests.add(theme.bodyLabel(invite.requester() + " wants to play level " + invite.level()))
                    .width(350f).height(36f).padRight(8f);
                TextButton accept = theme.primaryButton("ACCEPT");
                UiActions.onClick(accept, () -> respond(invite, true));
                requests.add(accept).width(120f).height(38f).padRight(6f);
                TextButton reject = theme.secondaryButton("REJECT");
                UiActions.onClick(reject, () -> respond(invite, false));
                requests.add(reject).width(120f).height(38f);
                requests.row().padTop(6f);
                if (announcedRequests.add(invite.requestId())) {
                    showInviteDialog(invite);
                }
        }
    }

    private void respond(MatchInvite invite, boolean accepted) {
        ActionResult result = app.services().network().respond(invite, accepted);
        showResult(result);
        if (accepted) {
            pollMatch();
        }
        refreshRequests();
    }

    private int selectedLevel() {
        try {
            int value = Integer.parseInt(level.getText().trim());
            if (value >= 1 && value <= 3) {
                return value;
            }
        } catch (NumberFormatException ignored) {
            // Fall through to a readable validation message.
        }
        theme.showError(status, "Level must be between 1 and 3.");
        return -1;
    }

    private void showInviteDialog(MatchInvite invite) {
        Dialog dialog = new Dialog("MATCH REQUEST", theme.skin()) {
            @Override
            protected void result(Object value) {
                super.result(value);
                if (value instanceof Boolean accepted) {
                    respond(invite, accepted);
                }
            }
        };
        dialog.text(invite.requester() + " wants to play I, Zombie level " + invite.level());
        dialog.button("ACCEPT", Boolean.TRUE);
        dialog.button("REJECT", Boolean.FALSE);
        dialog.button("CLOSE", null);
        dialog.show(stage);
    }

    @Override
    public void dispose() {
        disposed = true;
        networkExecutor.shutdownNow();
        super.dispose();
    }
}
