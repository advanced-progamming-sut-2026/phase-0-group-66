package model;

/** A projectile sent back toward plants by a spinning Juggler/Jester zombie. */
public final class ReflectedProjectile {
    private final int damage;
    private final double speed;
    private final ProjectileType type;
    private BoardPosition position;
    private boolean active = true;

    public ReflectedProjectile(int damage, double speed, BoardPosition position,
                               ProjectileType type) {
        if (damage < 0 || speed < 0 || position == null) {
            throw new IllegalArgumentException("Invalid reflected projectile data.");
        }
        this.damage = damage;
        this.speed = speed;
        this.position = position;
        this.type = type == null ? ProjectileType.NORMAL : type;
    }

    public double moveOneTick() {
        double previous = position.getColumn();
        position = position.moveHorizontal(-speed / Game.TICKS_PER_SECOND);
        return previous;
    }

    public void hitPlant(Plant plant) {
        if (plant == null || !active) {
            return;
        }
        if (type == ProjectileType.ICE) {
            plant.addIceLayer();
        } else if (type == ProjectileType.FIRE) {
            plant.damageIce(damage, true);
        }
        plant.takeDamage(damage);
        active = false;
    }

    public void deactivate() { active = false; }
    public boolean isActive() { return active; }
    public BoardPosition getPosition() { return position; }
    public int getDamage() { return damage; }
    public double getSpeed() { return speed; }
    public ProjectileType getType() { return type; }
}
