package model;

public final class DeadLineRule implements SpecialLevelRule {
    @Override public SpecialLevelType getType() { return SpecialLevelType.DEAD_LINE; }
    @Override public void configure(Level level) { level.configureDeadLine(2); }

    @Override
    public boolean hasSpecialLoss(Game game) {
        return game.board.hasZombiesCrossedColumn(game.currentLevel.getDeadLineColumn());
    }

    @Override
    public String status(Game game) {
        return "line column=" + (game.currentLevel.getDeadLineColumn() + 1);
    }

    @Override
    public String summary(Level level) {
        return "Do not let zombies cross column " + (level.getDeadLineColumn() + 1) + ".";
    }
}
