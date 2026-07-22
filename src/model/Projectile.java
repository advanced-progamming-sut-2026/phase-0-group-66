package model;

public class Projectile {
    private final int damage;
    private final double speed;
    private final ProjectileType type;
    private final boolean piercing;
    private final int chillDurationTicks;
    private final boolean lobbed;
    private final String sourcePlant;
    private BoardPosition position;
    private boolean active;
    private int remainingHits;

    public Projectile() {
        this(20, 5.0, new BoardPosition(0, 0), ProjectileType.NORMAL,
            false, 50, false, "", 1);
    }

    public Projectile(int damage, double speed, BoardPosition position,
                      ProjectileType type, boolean piercing) {
        this(damage, speed, position, type, piercing, 50, false, "",
            piercing ? Integer.MAX_VALUE : 1);
    }

    public Projectile(int damage, double speed, BoardPosition position,
                      ProjectileType type, boolean piercing, int chillDurationTicks) {
        this(damage, speed, position, type, piercing, chillDurationTicks,
            false, "", piercing ? Integer.MAX_VALUE : 1);
    }

    public Projectile(int damage, double speed, BoardPosition position,
                      ProjectileType type, boolean piercing, int chillDurationTicks,
                      boolean lobbed, String sourcePlant, int maxHits) {
        if (damage < 0 || speed < 0 || position == null || chillDurationTicks < 0
            || maxHits <= 0) {
            throw new IllegalArgumentException("Invalid projectile data.");
        }
        this.damage = damage;
        this.speed = speed;
        this.position = position;
        this.type = type == null ? ProjectileType.NORMAL : type;
        this.piercing = piercing || maxHits > 1;
        this.chillDurationTicks = chillDurationTicks;
        this.lobbed = lobbed;
        this.sourcePlant = sourcePlant == null ? "" : sourcePlant;
        this.remainingHits = maxHits;
        this.active = true;
    }

    public double moveOneTick() {
        double previousColumn = position.getColumn();
        position = position.moveHorizontal(speed / Game.TICKS_PER_SECOND);
        return previousColumn;
    }

    public void move() { moveOneTick(); }

    public boolean hitTarget(Zombie target) {
        return hitTarget(target, 1);
    }

    public boolean hitTarget(Zombie target, int damageMultiplier) {
        if (target == null || !active) {
            return false;
        }
        int actualDamage = Math.max(0, damage * Math.max(1, damageMultiplier));
        boolean affected = target.takeProjectileDamage(actualDamage, type,
            chillDurationTicks, lobbed, sourcePlant);
        if (affected) {
            remainingHits--;
        }
        if (!piercing || remainingHits <= 0) {
            active = false;
        }
        return affected;
    }

    public int getDamage() { return damage; }
    public double getSpeed() { return speed; }
    public ProjectileType getType() { return type; }
    public boolean isPiercing() { return piercing; }
    public boolean isLobbed() { return lobbed; }
    public String getSourcePlant() { return sourcePlant; }
    public int getRemainingHits() { return remainingHits; }
    public BoardPosition getPosition() { return position; }
    public boolean isActive() { return active; }
    public void deactivate() { active = false; }
}
