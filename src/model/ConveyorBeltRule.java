package model;

import java.util.List;

public final class ConveyorBeltRule implements SpecialLevelRule {
    @Override public SpecialLevelType getType() { return SpecialLevelType.CONVEYOR_BELT; }

    @Override
    public void configure(Level level) {
        level.configureConveyorPlants(List.of(
            "Peashooter", "Cabbage-pult", "Wall-nut", "Potato Mine"));
    }

    @Override
    public void initializeBattle(Game game) {
        if (game.selectedPlants.isEmpty()) {
            game.autoSelectStarterPlantsForConveyor();
        }
        game.addConveyorCard();
        game.nextConveyorTick = 12 * Game.TICKS_PER_SECOND;
    }

    @Override public boolean allowsManualPlantSelection() { return false; }
    @Override public boolean usesConveyor() { return true; }
    @Override public String status(Game game) { return "cards=" + game.conveyorCards; }

    @Override
    public String summary(Level level) {
        return "Plants arrive on the conveyor every 12 seconds.";
    }
}
