package model;

public record MiniGameDefinition(MiniGameType type, boolean bonus, String objective,
                                 String actionName) {
    public int targetForLevel(int level) {
        if (level < 1 || level > 3) {
            throw new IllegalArgumentException("Mini-game level must be between 1 and 3.");
        }
        return switch (type) {
            case VASEBREAKER -> 12 + level * 6;
            case WALLNUT_BOWLING -> 15 + level * 10;
            case I_ZOMBIE -> 5;
            case BEGHOULD -> 75;
            case ZOMBOTANY -> 20 + level * 10;
        };
    }

    @Override
    public String toString() {
        return type + " - " + (bonus ? "BONUS" : "REQUIRED") + " - " + objective;
    }
}
