package model;

public final class NormalLevelRule implements SpecialLevelRule {
    @Override public SpecialLevelType getType() { return SpecialLevelType.NORMAL; }
    @Override public String summary(Level level) { return "Normal battle rules."; }
}
