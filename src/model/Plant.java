package model;

public abstract class Plant {
    protected String name;
    protected int health;
    protected int sunCost;
    protected GridPosition position;
    protected int attackPower;
    protected int cooldown;

    public void attack() {
    }

    public void takeDamage(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Damage cannot be negative.");
        }
        health = Math.max(0, health - amount);
    }

    public void usePlantFood() {
    }

    public boolean isAvailable() {
        return cooldown <= 0;
    }

    public GridPosition getPosition() {
        return position;
    }

    public void setPosition(GridPosition position) {
        this.position = position;
    }
}
