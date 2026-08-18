package model;

public record MiniGameUnitSnapshot(
    String type,
    int row,
    double column,
    int health,
    int maximumHealth,
    int damage,
    double speed
) {
}
