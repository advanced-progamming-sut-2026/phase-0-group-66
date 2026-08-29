package pvz.screen;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import controller.ActionResult;
import network.game.MatchInvite;
import network.game.MatchTicket;
import pvz.PvzApplication;

import java.io.IOException;

public final class NetworkScreen extends AuthenticatedUiScreen {
    private final TextField opponent;
    private final Label status;
    private final Table requests;
    private float refreshTimer;
    private boolean launched;

    public NetworkScreen(PvzApplication app) {
        super(app);
        opponent = textField("Opponent username");
        status = statusLabel();
        requests = new Table();
        buildUi();
    }

    @Override
    public void render(float delta) {
        refreshTimer += Math.min(delta, 0.25f);
        if (refreshTimer >= 0.75f && !launched) {
            refreshTimer = 0f;
            pollMatch();
            refreshRequests();
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

        TextButton random = theme.primaryButton("FIND RANDOM OPPONENT");
        UiActions.onClick(random, () -> showResult(app.services().network().findRandom(1)));
        panel.add(random).width(360f).height(52f);
        panel.row().padTop(12f);

        Table direct = new Table();
        direct.add(opponent).width(330f).height(48f).padRight(10f);
        TextButton challenge = theme.secondaryButton("CHALLENGE");
        UiActions.onClick(challenge, () -> showResult(
            app.services().network().challenge(opponent.getText().trim(), 1)
        ));
        direct.add(challenge).width(190f).height(48f);
        panel.add(direct);
        panel.row().padTop(18f);
        panel.add(theme.heading("MATCH REQUESTS")).growX().height(36f);
        panel.row().padTop(6f);
        panel.add(requests).width(650f).height(90f);
        panel.row().padTop(12f);
        panel.add(status).width(760f).height(46f);
        panel.row().padTop(12f);

        TextButton back = theme.secondaryButton("BACK");
        UiActions.onClick(back, app::showMainMenu);
        panel.add(back).width(220f).height(48f);

        screen.add(panel).width(850f).height(480f).center();
        root.add(screen).grow();
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
            }
        } catch (IOException exception) {
            theme.showError(status, exception.getMessage());
        }
    }

    private void refreshRequests() {
        try {
            requests.clearChildren();
            for (MatchInvite invite : app.services().network().getRequests()) {
                requests.add(theme.bodyLabel(invite.requester() + " wants to play level " + invite.level()))
                    .width(350f).height(36f).padRight(8f);
                TextButton accept = theme.primaryButton("ACCEPT");
                UiActions.onClick(accept, () -> respond(invite, true));
                requests.add(accept).width(120f).height(38f).padRight(6f);
                TextButton reject = theme.secondaryButton("REJECT");
                UiActions.onClick(reject, () -> respond(invite, false));
                requests.add(reject).width(120f).height(38f);
                requests.row().padTop(6f);
            }
        } catch (IOException exception) {
            theme.showError(status, exception.getMessage());
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
}
