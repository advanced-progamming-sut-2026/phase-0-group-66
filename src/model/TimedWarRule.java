package model;

public final class TimedWarRule implements SpecialLevelRule {
    @Override public SpecialLevelType getType() { return SpecialLevelType.TIMED_WAR; }

    @Override
    public void configure(Level level) {
        level.configureTimedWar(TimedWarObjective.KILLS, 60, 12);
    }

    @Override
    public boolean hasSpecialWin(Game game) {
        return game.timedWarProgress() >= game.currentLevel.getTimedWarTarget();
    }

    @Override
    public boolean hasSpecialLoss(Game game) {
        int limitTicks = game.currentLevel.getTimeLimitSeconds() * Game.TICKS_PER_SECOND;
        return game.elapsedTicks >= limitTicks && !hasSpecialWin(game);
    }

    @Override public boolean blocksNormalWin(Game game) { return true; }

    @Override
    public String status(Game game) {
        int progress = game.timedWarProgress();
        int target = game.currentLevel.getTimedWarTarget();
        return "progress=" + progress + "/" + target
            + ", remaining=" + Math.max(0, target - progress) + ", time="
            + game.formatSeconds(Math.max(0, game.currentLevel.getTimeLimitSeconds()
            * Game.TICKS_PER_SECOND - game.elapsedTicks)) + "s";
    }

    @Override
    public String summary(Level level) {
        return "Reach " + level.getTimedWarTarget() + " "
            + level.getTimedWarObjective().name().toLowerCase() + " within "
            + level.getTimeLimitSeconds() + " seconds.";
    }
}
