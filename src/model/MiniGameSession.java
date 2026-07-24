package model;

import java.util.List;

public abstract class MiniGameSession {
    private final MiniGameDefinition definition;
    private final int level;
    private final int target;
    private int score;
    private int elapsedTicks;
    private boolean won;
    private boolean lost;

    protected MiniGameSession(MiniGameDefinition definition, int level) {
        if (definition == null) {
            throw new IllegalArgumentException("Mini-game definition is required.");
        }
        if (level < 1 || level > 3) {
            throw new IllegalArgumentException("Mini-game level must be between 1 and 3.");
        }
        this.definition = definition;
        this.level = level;
        this.target = definition.targetForLevel(level);
    }

    public abstract void execute(String command, List<String> arguments);

    public abstract String boardView();

    protected abstract String progressText();

    public final void advanceTime(int ticks) {
        ensureRunning();
        if (ticks <= 0) {
            throw new IllegalArgumentException("Tick count must be positive.");
        }
        for (int index = 0; index < ticks && !isFinished(); index++) {
            elapsedTicks++;
            onTick();
        }
    }

    protected abstract void onTick();

    public void perform(String action, int amount) {
        execute(action, List.of(Integer.toString(amount)));
    }

    protected final void addScore(int amount) {
        score = Math.max(0, score + amount);
    }

    protected final void win() {
        won = true;
        lost = false;
    }

    protected final void lose() {
        lost = true;
        won = false;
    }

    protected final void ensureRunning() {
        if (won) {
            throw new IllegalStateException("Mini-game is already won.");
        }
        if (lost) {
            throw new IllegalStateException("Mini-game is already lost.");
        }
    }

    public final MiniGameDefinition getDefinition() { return definition; }
    public final int getLevel() { return level; }
    public final int getTarget() { return target; }
    public final int getScore() { return score; }
    public final int getElapsedTicks() { return elapsedTicks; }
    public final boolean isWon() { return won; }
    public final boolean isLost() { return lost; }
    public final boolean isFinished() { return won || lost; }

    public final String status() {
        String state = won ? "WON" : lost ? "LOST" : "RUNNING";
        return definition.type() + " level " + level + ": " + progressText()
            + ", score=" + score + ", time=" + elapsedTicks + " ticks, state=" + state;
    }
}
