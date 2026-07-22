package model;

public final class MiniGameSessionFactory {
    private MiniGameSessionFactory() { }

    public static MiniGameSession create(MiniGameDefinition definition, int level) {
        return switch (definition.type()) {
            case VASEBREAKER -> new VasebreakerSession(definition, level);
            case WALLNUT_BOWLING -> new WallnutBowlingSession(definition, level);
            case I_ZOMBIE -> new IZombieSession(definition, level);
            case BEGHOULD -> new BeghouldSession(definition, level);
            case ZOMBOTANY -> new ZombotanySession(definition, level);
        };
    }
}
