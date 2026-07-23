package model;

public final class LoveYourPlantsRule implements SpecialLevelRule {
    @Override public SpecialLevelType getType() { return SpecialLevelType.LOVE_YOUR_PLANTS; }
    @Override public void configure(Level level) { level.configureAllowedPlantLosses(5); }

    @Override
    public boolean hasSpecialLoss(Game game) {
        return game.lostPlantsCount >= game.currentLevel.getAllowedPlantLosses();
    }

    @Override
    public String status(Game game) {
        return "lost plants=" + game.lostPlantsCount + "/"
            + game.currentLevel.getAllowedPlantLosses();
    }

    @Override
    public String summary(Level level) {
        return "Lose fewer than " + level.getAllowedPlantLosses() + " plants.";
    }
}
