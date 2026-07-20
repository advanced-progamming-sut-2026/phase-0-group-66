package model;

public class MiniGameSession {
    private final MiniGameDefinition definition;
    private final int level;
    private final int target;
    private int progress;
    private int score;
    private int sun;
    private boolean won;

    public MiniGameSession(MiniGameDefinition definition, int level) {
        this.definition = definition;
        this.level = level;
        this.target = definition.targetForLevel(level);
        this.sun = definition.type() == MiniGameType.I_ZOMBIE ? 150 : 0;
    }

    public void perform(String action, int amount) {
        if (won) {
            throw new IllegalStateException("Mini-game is already completed.");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Action amount must be positive.");
        }
        String expected = definition.actionName();
        if (!expected.equalsIgnoreCase(action)) {
            throw new IllegalArgumentException("Expected action: " + expected + ".");
        }
        int applied = Math.min(amount, target - progress);
        if (definition.type() == MiniGameType.I_ZOMBIE) {
            int cost = applied * 25;
            if (sun < cost) {
                throw new IllegalStateException("Not enough sun for this zombie action.");
            }
            sun -= cost;
        }
        progress += applied;
        score += applied * (100 + level * 25);
        if (definition.type() == MiniGameType.BEGHOULD) {
            sun += applied * 50;
        }
        won = progress >= target;
    }

    public MiniGameDefinition getDefinition() { return definition; }
    public int getLevel() { return level; }
    public int getTarget() { return target; }
    public int getProgress() { return progress; }
    public int getScore() { return score; }
    public int getSun() { return sun; }
    public boolean isWon() { return won; }

    public String status() {
        return definition.type() + " level " + level + ": " + progress + "/" + target
            + ", score=" + score + ", sun=" + sun + ", state=" + (won ? "WON" : "RUNNING");
    }
}
