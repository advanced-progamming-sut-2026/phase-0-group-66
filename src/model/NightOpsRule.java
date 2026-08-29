package model;

public final class NightOpsRule implements SpecialLevelRule {
    @Override public SpecialLevelType getType() { return SpecialLevelType.NIGHT_OPS; }
    @Override public boolean allowsSkySun() { return false; }
    @Override
    public String status(Game game) {
        return "sky sun disabled; win by clearing all zombie waves";
    }

    @Override
    public String summary(Level level) {
        return "Night rules: no sky sun will fall. Win by clearing every zombie wave.";
    }
}
