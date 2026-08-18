package pvz.screen;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import controller.ActionResult;
import controller.MiniGameController;
import model.Game;
import model.MiniGameSession;
import pvz.PvzApplication;

public abstract class MiniGamePlayScreen extends AuthenticatedUiScreen {
    private static final float TICK_SECONDS = 1f / Game.TICKS_PER_SECOND;

    protected final MiniGameController miniGames;
    protected final MiniGameSession session;
    protected final Label message;
    private float tickAccumulator;

    protected MiniGamePlayScreen(PvzApplication app) {
        super(app);
        miniGames = app.services().miniGames();
        session = miniGames.getCurrentSession();
        if (session == null) {
            throw new IllegalStateException("A mini-game session must be started first.");
        }
        message = theme.statusLabel();
    }

    @Override
    public void render(float delta) {
        advanceSession(Math.min(delta, 0.25f));
        super.render(delta);
    }

    protected final void execute(String command) {
        ActionResult result = miniGames.executeCommand(command);
        if (result.isSuccessful()) {
            theme.showSuccess(message, shortStatus());
        } else {
            theme.showError(message, result.getMessage());
        }
        refreshFromSession();
    }

    protected final String shortStatus() {
        if (session.isWon()) {
            return "Victory! Score: " + session.getScore();
        }
        if (session.isLost()) {
            return "Defeat. Score: " + session.getScore();
        }
        return "Score " + session.getScore() + "  |  Time "
            + (session.getElapsedTicks() / Game.TICKS_PER_SECOND) + "s";
    }

    protected abstract void refreshFromSession();

    private void advanceSession(float delta) {
        if (session.isFinished()) {
            return;
        }
        tickAccumulator += delta;
        boolean changed = false;
        while (tickAccumulator >= TICK_SECONDS && !session.isFinished()) {
            tickAccumulator -= TICK_SECONDS;
            ActionResult result = miniGames.advanceTime(1);
            if (!result.isSuccessful()) {
                theme.showError(message, result.getMessage());
                break;
            }
            changed = true;
        }
        if (changed) {
            theme.showSuccess(message, shortStatus());
            refreshFromSession();
        }
    }
}
