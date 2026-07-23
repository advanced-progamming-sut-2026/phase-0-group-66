package model;

import java.util.List;

public final class SaveOurSeedsRule implements SpecialLevelRule {
    @Override public SpecialLevelType getType() { return SpecialLevelType.SAVE_OUR_SEEDS; }

    @Override
    public void configure(Level level) {
        level.configureProtectedPlants("Wall-nut", List.of(
            new GridPosition(0, 2), new GridPosition(2, 2), new GridPosition(4, 2)));
    }

    @Override public void initializeBattle(Game game) { game.initializeProtectedPlants(); }
    @Override public boolean hasSpecialLoss(Game game) { return game.board.areEndangeredPlantsEaten(); }

    @Override
    public String status(Game game) {
        return "protected remaining=" + game.protectedPlantsRemaining()
            + "/" + game.endangeredPositions.size();
    }

    @Override
    public String summary(Level level) {
        return "Protect every marked " + level.getProtectedPlantType() + ".";
    }
}
