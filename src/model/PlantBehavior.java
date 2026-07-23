package model;

/** Runtime strategy for a plant's active action. */
@FunctionalInterface
public interface PlantBehavior {
    void perform(Game game, Plant plant);
}
