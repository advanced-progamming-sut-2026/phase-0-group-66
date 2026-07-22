package model;

final class MiniGamePlantUnit {
    private final String type;
    private final int row;
    private final int column;
    private int health;
    private int damage;
    private int cooldown;

    MiniGamePlantUnit(String type, int row, int column, int health, int damage) {
        this.type = type;
        this.row = row;
        this.column = column;
        this.health = health;
        this.damage = damage;
    }

    String getType() { return type; }
    int getRow() { return row; }
    int getColumn() { return column; }
    int getHealth() { return health; }
    int getDamage() { return damage; }
    boolean isDead() { return health <= 0; }
    boolean ready() { return cooldown <= 0; }
    void damage(int amount) { health = Math.max(0, health - Math.max(0, amount)); }
    void increaseDamage(int amount) { damage += Math.max(0, amount); }
    void setCooldown(int ticks) { cooldown = Math.max(0, ticks); }
    void tick() { if (cooldown > 0) { cooldown--; } }
}
