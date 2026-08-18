package model;

public record MiniGamePlantSnapshot(
    String type,
    int row,
    int column,
    int health,
    int damage
) {
}
