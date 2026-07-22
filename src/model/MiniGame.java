package model;

public class MiniGame {
    private final MiniGameDefinition definition;

    public MiniGame(MiniGameDefinition definition) {
        this.definition = definition;
    }

    public MiniGameDefinition getDefinition() {
        return definition;
    }

    public MiniGameSession start(int level) {
        return MiniGameSessionFactory.create(definition, level);
    }
}
