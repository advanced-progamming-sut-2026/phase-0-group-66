package model;

public final class Armor {
    private final ArmorDefinition definition;
    private int health;

    public Armor(ArmorDefinition definition) {
        if (definition == null) {
            throw new IllegalArgumentException("Armor definition cannot be null.");
        }
        this.definition = definition;
        this.health = definition.getBaseHealth();
    }

    public ArmorDefinition getDefinition() { return definition; }
    public int getHealth() { return health; }
    public boolean isDestroyed() { return health <= 0; }

    public void scaleHealth(double factor) {
        if (factor <= 0) {
            throw new IllegalArgumentException("Armor health factor must be positive.");
        }
        health = Math.max(1, (int) Math.round(health * factor));
    }

    public int absorbDamage(int damage) {
        if (damage < 0) {
            throw new IllegalArgumentException("Damage cannot be negative.");
        }
        int absorbed = Math.min(health, damage);
        health -= absorbed;
        return damage - absorbed;
    }
}
