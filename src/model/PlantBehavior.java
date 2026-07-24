package model;

@FunctionalInterface
public interface PlantBehavior {
    void perform(Game game, Plant plant);
}
