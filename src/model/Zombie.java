package model;

public abstract class Zombie {
    protected String name;
    protected int health;
    protected double speed;
    protected int damage;
    protected int waveCost;
    protected BoardPosition position;

    public void move() {
        if (position != null) {
            position = position.moveHorizontal(-speed);
        }
    }

    public void attackPlant(Plant target) {
        if (target != null) {
            target.takeDamage(damage);
        }
    }

    public void takeDamage(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Damage cannot be negative.");
        }
        health = Math.max(0, health - amount);
    }

    public void dropReward() {
    }

    public void specialAbility() {
    }

    public BoardPosition getPosition() {
        return position;
    }

    public void setPosition(BoardPosition position) {
        this.position = position;
    }
}
