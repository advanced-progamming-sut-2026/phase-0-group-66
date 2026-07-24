package model;

public interface SpecialLevelRule {
    SpecialLevelType getType();

    default void configure(Level level) { }

    default void initializeBattle(Game game) { }

    default boolean allowsManualPlantSelection() { return true; }

    default boolean usesConveyor() { return false; }

    default boolean allowsSkySun() { return true; }

    default boolean requiresManualWaveStart() { return false; }

    default boolean isPreWaveSetup(Game game) { return false; }

    default void validatePlantSelection(Game game, PlantDefinition definition) { }

    default boolean hasSpecialWin(Game game) { return false; }

    default boolean hasSpecialLoss(Game game) { return false; }

    default boolean blocksNormalWin(Game game) { return false; }

    default String status(Game game) { return "normal"; }

    String summary(Level level);
}
