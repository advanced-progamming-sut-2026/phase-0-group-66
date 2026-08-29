package pvz.screen;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import controller.ActionResult;
import controller.MiniGameController;
import model.Game;
import model.MiniGameSession;
import network.client.NetworkIZombieSession;
import pvz.PvzApplication;
import pvz.app.PvzAudio;

public abstract class MiniGamePlayScreen extends AuthenticatedUiScreen {
    private static final float TICK_SECONDS = 1f / Game.TICKS_PER_SECOND;

    protected final MiniGameController miniGames;
    protected final MiniGameSession session;
    protected final Label message;
    private float tickAccumulator;
    private float networkPollAccumulator;
    private boolean completionSoundPlayed;

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
        if (session instanceof NetworkIZombieSession online) {
            networkPollAccumulator += Math.min(delta, 0.25f);
            if (networkPollAccumulator >= 0.5f) {
                networkPollAccumulator = 0f;
                online.poll();
                refreshFromSession();
            }
        }
        advanceSession(Math.min(delta, 0.25f));
        super.render(delta);
    }

    protected final NetworkIZombieSession onlineSession() {
        return session instanceof NetworkIZombieSession online ? online : null;
    }

    protected final void execute(String command) {
        ActionResult result = miniGames.executeCommand(command);
        if (result.isSuccessful()) {
            playCommandSound(command);
            theme.showSuccess(message, shortStatus());
        } else {
            theme.showError(message, result.getMessage());
        }
        refreshFromSession();
        playCompletionSoundIfNeeded();
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
            playCompletionSoundIfNeeded();
        }
    }

    private void playCommandSound(String command) {
        String normalized = command == null ? "" : command.trim().toLowerCase();
        if (normalized.startsWith("break")) {
            app.audio().playSfx(PvzAudio.EXPLOSION_SOUND);
        } else if (normalized.startsWith("bowl") && normalized.contains("explosive")) {
            app.audio().playSfx(PvzAudio.EXPLOSION_SOUND);
        } else if (normalized.startsWith("start")) {
            app.audio().playSfx(PvzAudio.ZOMBIES_COMING_SOUND);
        } else if (normalized.startsWith("deploy")) {
            app.audio().playSfx(PvzAudio.ZOMBIES_SOUND);
        }
    }

    private void playCompletionSoundIfNeeded() {
        if (completionSoundPlayed || !session.isFinished()) {
            return;
        }
        completionSoundPlayed = true;
        app.audio().playSfx(session.isWon() ? PvzAudio.WIN_SOUND : PvzAudio.LOSS_SOUND);
    }
}
