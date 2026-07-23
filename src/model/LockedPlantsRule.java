package model;

import java.util.List;
import java.util.Map;

public final class LockedPlantsRule implements SpecialLevelRule {
    @Override public SpecialLevelType getType() { return SpecialLevelType.LOCKED_PLANTS; }

    @Override
    public void configure(Level level) {
        level.configureLockedPlants(
            List.of("Peashooter", "Wall-nut"),
            List.of("Sunflower", "Cherry Bomb"),
            List.of("Mint"));
        level.configureFamilyRepresentativeLocks(Map.of(
            "Shooter", "Peashooter", "Wall-nut", "Wall-nut"));
    }

    @Override
    public void validatePlantSelection(Game game, PlantDefinition definition) {
        Level level = game.currentLevel;
        if (game.containsNormalized(level.getLockedPlants(), definition.getName())) {
            throw new IllegalStateException("This plant is locked in the current level.");
        }
        for (String family : level.getBannedPlantFamilies()) {
            if (game.matchesPlantFamily(definition, family)) {
                throw new IllegalStateException("The " + family
                    + " plant family is locked in this level.");
            }
        }
        for (Map.Entry<String, String> entry : level.getFamilyRepresentativePlants().entrySet()) {
            if (game.matchesPlantFamily(definition, entry.getKey())
                && !definition.getName().equalsIgnoreCase(entry.getValue())) {
                throw new IllegalStateException("Only " + entry.getValue()
                    + " is available from the " + entry.getKey() + " family.");
            }
        }
    }

    @Override
    public String status(Game game) {
        Level level = game.currentLevel;
        return "forced=" + level.getForcedPlants() + ", locked=" + level.getLockedPlants()
            + ", representatives=" + level.getFamilyRepresentativePlants();
    }

    @Override
    public String summary(Level level) {
        return "Forced plants=" + level.getForcedPlants() + ", locked="
            + level.getLockedPlants() + ", family representatives="
            + level.getFamilyRepresentativePlants() + ", banned families="
            + level.getBannedPlantFamilies() + ".";
    }
}
