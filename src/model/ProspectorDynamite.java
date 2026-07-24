package model;

public final class ProspectorDynamite {
    private final String ownerRuntimeId;
    private final int damagePerSecond;
    private final double speed;
    private BoardPosition position;
    private boolean active = true;

    public ProspectorDynamite(String ownerRuntimeId, BoardPosition position,
                              int damagePerSecond, double speed) {
        if (position == null || damagePerSecond < 0 || speed < 0) {
            throw new IllegalArgumentException("Invalid Prospector dynamite data.");
        }
        this.ownerRuntimeId = ownerRuntimeId == null ? "" : ownerRuntimeId;
        this.position = position;
        this.damagePerSecond = damagePerSecond;
        this.speed = speed;
    }

    public double moveOneTick() {
        double previous = position.getColumn();
        position = position.moveHorizontal(speed / Game.TICKS_PER_SECOND);
        return previous;
    }

    public void stopAt(double column) {
        position = new BoardPosition(position.getRow(), column);
    }

    public void deactivate() { active = false; }
    public boolean isActive() { return active; }
    public String getOwnerRuntimeId() { return ownerRuntimeId; }
    public BoardPosition getPosition() { return position; }
    public int getDamagePerSecond() { return damagePerSecond; }
    public double getSpeed() { return speed; }
}
