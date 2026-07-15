package model;

public class Projectile {
    private int damage;
    private double speed;
    private BoardPosition position;

    public void move() {
        if (position != null) {
            position = position.moveHorizontal(speed);
        }
    }

    public void hitTarget(Zombie target) {
        if (target != null) {
            target.takeDamage(damage);
        }
    }

    public BoardPosition getPosition() {
        return position;
    }
}
