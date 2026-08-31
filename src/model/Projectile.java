package model;

public class Projectile {
    private final int damage;
    private final double speed;
    private final ProjectileType type;
    private final boolean piercing;
    private final int chillDurationTicks;
    private final boolean lobbed;
    private final String sourcePlant;
    private final int poisonDurationTicks;
    private final int poisonDamagePerSecond;
    private BoardPosition position;
    private ProjectileType impactType;
    private int damageMultiplier;
    private boolean active;
    private int remainingHits;
    private double lobProgress;
    private final double lobOriginColumn;

    /** Height of the current air-projectile arc, normalized to [0, 1]. */
    public static final double LOB_ARC_DISTANCE = 3.0;

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
        this(damage, speed, position, type, piercing, chillDurationTicks, lobbed,
            sourcePlant, maxHits, 5 * Game.TICKS_PER_SECOND, Math.max(1, damage / 4));
    }

    public Projectile(int damage, double speed, BoardPosition position,
                      ProjectileType type, boolean piercing, int chillDurationTicks,
                      boolean lobbed, String sourcePlant, int maxHits,
                      int poisonDurationTicks, int poisonDamagePerSecond) {
        if (damage < 0 || speed < 0 || position == null || chillDurationTicks < 0
            || maxHits <= 0 || poisonDurationTicks < 0 || poisonDamagePerSecond < 0) {
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
        this.poisonDurationTicks = poisonDurationTicks;
        this.poisonDamagePerSecond = poisonDamagePerSecond;
        this.impactType = this.type;
        this.damageMultiplier = 1;
        this.remainingHits = maxHits;
        this.active = true;
        this.lobProgress = 0d;
        this.lobOriginColumn = position.getColumn();
    }

    public double moveOneTick() {
        double previousColumn = position.getColumn();
        double distance = speed / Game.TICKS_PER_SECOND;
        position = position.moveHorizontal(distance);
        if (lobbed) {
            lobProgress = (lobProgress + distance / LOB_ARC_DISTANCE) % 1d;
        }
        return previousColumn;
    }

    public void move() { moveOneTick(); }

    public boolean hitTarget(Zombie target) {
        return hitTarget(target, damageMultiplier, impactType);
    }

    public boolean hitTarget(Zombie target, int multiplier) {
        return hitTarget(target, multiplier, impactType);
    }

    public boolean hitTarget(Zombie target, int damageMultiplier, ProjectileType impactType) {
        if (target == null || !active) {
            return false;
        }
        int actualDamage = Math.max(0, damage * Math.max(1, damageMultiplier));
        ProjectileType resolvedType = impactType == null ? type : impactType;
        boolean affected = target.takeProjectileDamage(actualDamage, resolvedType,
            chillDurationTicks, lobbed, sourcePlant, poisonDurationTicks,
            poisonDamagePerSecond);
        if (affected) {
            remainingHits--;
        }
        if (!piercing || remainingHits <= 0) {
            active = false;
        }
        return affected;
    }

    public void igniteByTorchwood(int multiplier) {
        if (type == ProjectileType.FIRE || multiplier <= 1) {
            return;
        }
        damageMultiplier = Math.max(damageMultiplier, multiplier);
        impactType = ProjectileType.FIRE;
    }

    public int getDamage() { return damage; }
    public double getSpeed() { return speed; }
    public ProjectileType getType() { return type; }
    public ProjectileType getImpactType() { return impactType; }
    public int getDamageMultiplier() { return damageMultiplier; }
    public boolean isPiercing() { return piercing; }
    public boolean isLobbed() { return lobbed; }
    public double getLobArcHeight() {
        if (!lobbed) {
            return 0d;
        }
        // A smooth parabola: zero at launch/landing, one at the apex.
        return 4d * lobProgress * (1d - lobProgress);
    }
    public double getLobArcHeight(double renderedColumn) {
        if (!lobbed) {
            return 0d;
        }
        double progress = (renderedColumn - lobOriginColumn) / LOB_ARC_DISTANCE;
        progress -= Math.floor(progress);
        return 4d * progress * (1d - progress);
    }
    public String getSourcePlant() { return sourcePlant; }
    public int getRemainingHits() { return remainingHits; }
    public BoardPosition getPosition() { return position; }
    public boolean isActive() { return active; }
    public void deactivate() { active = false; }
}
