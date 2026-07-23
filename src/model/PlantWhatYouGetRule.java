package model;

public final class PlantWhatYouGetRule implements SpecialLevelRule {
    @Override public SpecialLevelType getType() { return SpecialLevelType.PLANT_WHAT_YOU_GET; }
    @Override public void configure(Level level) { level.configureWaitForZombieWaves(true); }
    @Override public boolean allowsSkySun() { return false; }
    @Override public boolean requiresManualWaveStart() { return true; }

    @Override
    public boolean isPreWaveSetup(Game game) {
        return !game.zombieWavesStarted;
    }

    @Override
    public void validatePlantSelection(Game game, PlantDefinition definition) {
        if (game.isSunProducerDefinition(definition)) {
            throw new IllegalStateException("Sun-producing plants are unavailable in this level.");
        }
    }

    @Override
    public boolean blocksNormalWin(Game game) {
        return !game.zombieWavesStarted;
    }

    @Override
    public String status(Game game) {
        return "waves started=" + game.zombieWavesStarted + ", remaining sun=" + game.sunAmount;
    }

    @Override
    public String summary(Level level) {
        return "Use only the starting sun, then start zombie waves.";
    }
}
