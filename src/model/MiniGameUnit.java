package model;

final class MiniGameUnit {
    private final String type;
    private int row;
    private double column;
    private int health;
    private final int maximumHealth;
    private final int damage;
    private final double speed;
    private double slowMultiplier = 1.0;
    private int slowTicks;
    private int cooldown;
    private int ageTicks;

    MiniGameUnit(String type, int row, double column, int health, int damage, double speed) {
        this.type = type;
        this.row = row;
        this.column = column;
        this.health = health;
        this.maximumHealth = health;
        this.damage = damage;
        this.speed = speed;
    }

    String getType() { return type; }
    int getRow() { return row; }
    double getColumn() { return column; }
    int getHealth() { return health; }
    int getMaximumHealth() { return maximumHealth; }
    int getDamage() { return damage; }
    double getSpeed() { return slowTicks > 0 ? speed * slowMultiplier : speed; }
    int getAgeTicks() { return ageTicks; }
    boolean isDead() { return health <= 0; }

    void setRow(int row) { this.row = row; }
    void setColumn(double column) { this.column = column; }
    void damage(int amount) { health = Math.max(0, health - Math.max(0, amount)); }
    void heal(int amount) { health = Math.min(maximumHealth, health + Math.max(0, amount)); }
    void tickAge() {
        ageTicks++;
        if (cooldown > 0) {
            cooldown--;
        }
        if (slowTicks > 0) {
            slowTicks--;
            if (slowTicks == 0) {
                slowMultiplier = 1.0;
            }
        }
    }

    void slow(double multiplier, int ticks) {
        if (multiplier <= 0 || multiplier > 1 || ticks <= 0) {
            return;
        }
        slowMultiplier = Math.min(slowMultiplier, multiplier);
        slowTicks = Math.max(slowTicks, ticks);
    }
    boolean ready() { return cooldown <= 0; }
    void setCooldown(int ticks) { cooldown = Math.max(0, ticks); }
}
