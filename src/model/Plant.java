package model;

public abstract class Plant {
    protected final PlantDefinition definition;
    protected String name;
    protected int health;
    protected int sunCost;
    protected GridPosition position;
    protected int attackPower;
    protected int cooldown;

    protected Plant(PlantDefinition definition) {
        if (definition == null) {
            throw new IllegalArgumentException("Plant definition cannot be null.");
        }
        this.definition = definition;
        this.name = definition.getName();
        this.health = definition.getBaseHealth();
        this.sunCost = definition.getCost();
        this.attackPower = definition.getBaseDamage();
    }

    public void attack() {
    }

    public void takeDamage(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Damage cannot be negative.");
        }
        health = Math.max(0, health - amount);
    }

    public void usePlantFood() {
    }

    public boolean isAvailable() {
        return cooldown <= 0;
    }

    public boolean isDestroyed() {
        return health <= 0;
    }

    public PlantDefinition getDefinition() {
        return definition;
    }

    public String getName() {
        return name;
    }

    public int getHealth() {
        return health;
    }

    public int getSunCost() {
        return sunCost;
    }

    public int getAttackPower() {
        return attackPower;
    }

    public int getCooldown() {
        return cooldown;
    }

    public void setCooldown(int cooldown) {
        this.cooldown = Math.max(0, cooldown);
    }

    public GridPosition getPosition() {
        return position;
    }

    public void setPosition(GridPosition position) {
        this.position = position;
    }
}
