package model;

public class Projectile {
    private final int damage;
    private final double speed;
    private final ProjectileType type;
    private final boolean piercing;
    private BoardPosition position;
    private boolean active;

    public Projectile() {
        this(20, 5.0, new BoardPosition(0, 0), ProjectileType.NORMAL, false);
    }

    public Projectile(int damage, double speed, BoardPosition position,
                      ProjectileType type, boolean piercing) {
        if (damage < 0 || speed < 0 || position == null) {
            throw new IllegalArgumentException("Invalid projectile data.");
        }
        this.damage = damage;
        this.speed = speed;
        this.position = position;
        this.type = type == null ? ProjectileType.NORMAL : type;
        this.piercing = piercing;
        this.active = true;
    }

    public double moveOneTick() {
        double previousColumn = position.getColumn();
        position = position.moveHorizontal(speed / Game.TICKS_PER_SECOND);
        return previousColumn;
    }

    public void move() {
        moveOneTick();
    }

    public void hitTarget(Zombie target) {
        if (target == null || !active) {
            return;
        }
        if (type == ProjectileType.POISON) {
            target.takeDirectDamage(damage);
        } else if (type == ProjectileType.FIRE) {
            target.clearChill();
            target.takeDamage(damage * 2);
        } else {
            target.takeDamage(damage);
            if (type == ProjectileType.ICE) {
                target.chill(50);
            }
        }
        if (!piercing) {
            active = false;
        }
    }

    public int getDamage() {
        return damage;
    }

    public double getSpeed() {
        return speed;
    }

    public ProjectileType getType() {
        return type;
    }

    public boolean isPiercing() {
        return piercing;
    }

    public BoardPosition getPosition() {
        return position;
    }

    public boolean isActive() {
        return active;
    }

    public void deactivate() {
        active = false;
    }
}
