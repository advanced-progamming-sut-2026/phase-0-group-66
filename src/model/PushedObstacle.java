package model;

/** A damageable object that occupies a lane independently from its owning zombie. */
public final class PushedObstacle {
    private final PushedObstacleType type;
    private final String ownerRuntimeId;
    private final int maximumHealth;
    private final double speed;
    private BoardPosition position;
    private int health;

    public PushedObstacle(PushedObstacleType type, String ownerRuntimeId,
                          BoardPosition position, int health, double speed) {
        if (type == null || position == null || health <= 0 || speed < 0) {
            throw new IllegalArgumentException("Invalid pushed obstacle data.");
        }
        this.type = type;
        this.ownerRuntimeId = ownerRuntimeId == null ? "" : ownerRuntimeId;
        this.position = position;
        this.health = health;
        this.maximumHealth = health;
        this.speed = speed;
    }

    public double moveOneTick() {
        return moveOneTick(1.0);
    }

    public double moveOneTick(double speedMultiplier) {
        double previous = position.getColumn();
        double actualMultiplier = Math.max(0, speedMultiplier);
        position = position.moveHorizontal(-speed * actualMultiplier
            / Game.TICKS_PER_SECOND);
        return previous;
    }

    public void stopAt(double column) {
        position = new BoardPosition(position.getRow(), column);
    }

    public void takeDamage(int amount) {
        health = Math.max(0, health - Math.max(0, amount));
    }

    public void destroy() { health = 0; }

    public boolean destroysPlantsOnContact() {
        return type == PushedObstacleType.ICE_BLOCK
            || type == PushedObstacleType.ARCADE_MACHINE;
    }

    public boolean blocksDirectProjectiles() { return true; }
    public boolean isDestroyed() { return health <= 0; }
    public PushedObstacleType getType() { return type; }
    public String getOwnerRuntimeId() { return ownerRuntimeId; }
    public BoardPosition getPosition() { return position; }
    public int getHealth() { return health; }
    public int getMaximumHealth() { return maximumHealth; }
    public double getSpeed() { return speed; }
}
