package model;

public class Tomb {
    private int health;
    private final GridPosition position;
    private final boolean containsSun;
    private final boolean containsPlantFood;
    private boolean destroyed;

    public Tomb(int row, int column, boolean containsSun, boolean containsPlantFood) {
        this.health = 700;
        this.position = new GridPosition(row, column);
        this.containsSun = containsSun;
        this.containsPlantFood = containsPlantFood;
    }

    public void takeDamage(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Damage cannot be negative.");
        }
        health = Math.max(0, health - amount);
        destroyed = health == 0;
    }

    public int getHealth() {
        return health;
    }

    public GridPosition getPosition() {
        return position;
    }

    public boolean containsSun() {
        return containsSun;
    }

    public boolean containsPlantFood() {
        return containsPlantFood;
    }

    public boolean isDestroyed() {
        return destroyed;
    }
}
