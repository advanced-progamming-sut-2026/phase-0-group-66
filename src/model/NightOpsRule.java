package model;

public final class NightOpsRule implements SpecialLevelRule {
    @Override public SpecialLevelType getType() { return SpecialLevelType.NIGHT_OPS; }
    @Override public boolean allowsSkySun() { return false; }
    @Override public String status(Game game) { return "sky sun disabled"; }
    @Override public String summary(Level level) { return "No sky sun will fall."; }
}
