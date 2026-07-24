package model;

public class Tomb {
    private int health;
    private final GridPosition position;
    private final boolean containsSun;
    private final boolean containsPlantFood;
    private final TileType underlyingTileType;
    private boolean destroyed;

    public Tomb(int row, int column, boolean containsSun, boolean containsPlantFood) {
        this(row, column, containsSun, containsPlantFood, TileType.NORMAL);
    }

    public Tomb(int row, int column, boolean containsSun, boolean containsPlantFood,
                TileType underlyingTileType) {
        this.health = 700;
        this.position = new GridPosition(row, column);
        this.containsSun = containsSun;
        this.containsPlantFood = containsPlantFood;
        this.underlyingTileType = underlyingTileType == null
            || underlyingTileType == TileType.TOMB ? TileType.NORMAL : underlyingTileType;
    }

    public void takeDamage(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Damage cannot be negative.");
        }
        health = Math.max(0, health - amount);
        destroyed = health == 0;
    }

    public int getHealth() { return health; }
    public GridPosition getPosition() { return position; }
    public boolean containsSun() { return containsSun; }
    public boolean containsPlantFood() { return containsPlantFood; }
    public TileType getUnderlyingTileType() { return underlyingTileType; }
    public boolean isNecromancySite() { return underlyingTileType == TileType.NECROMANCY; }
    public boolean isDestroyed() { return destroyed; }
}
