package model;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class Plant {
    private static final Pattern MULTIPLIER = Pattern.compile("[x×](\\d+)", Pattern.CASE_INSENSITIVE);

    protected final PlantDefinition definition;
    protected String name;
    protected int health;
    protected final int maxHealth;
    protected int sunCost;
    protected GridPosition position;
    protected int attackPower;
    protected int cooldown;
    private final int actionIntervalTicks;
    private int actionTicksRemaining;

    protected Plant(PlantDefinition definition) {
        if (definition == null) {
            throw new IllegalArgumentException("Plant definition cannot be null.");
        }
        this.definition = definition;
        this.name = definition.getName();
        this.maxHealth = Math.max(1, definition.getBaseHealth());
        this.health = maxHealth;
        this.sunCost = definition.getCost();
        this.attackPower = definition.getBaseDamage();
        double interval = definition.getActionIntervalSeconds().orElse(1.0);
        this.actionIntervalTicks = Math.max(1, (int) Math.round(interval * Game.TICKS_PER_SECOND));
        this.actionTicksRemaining = actionIntervalTicks;
    }

    public void attack() {
    }

    public void takeDamage(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Damage cannot be negative.");
        }
        health = Math.max(0, health - amount);
    }

    public void healToFull() {
        health = maxHealth;
    }

    public void usePlantFood() {
        healToFull();
        actionTicksRemaining = 0;
    }

    public boolean tickActionTimer() {
        if (actionTicksRemaining > 0) {
            actionTicksRemaining--;
        }
        return actionTicksRemaining <= 0;
    }

    public void resetActionTimer() {
        actionTicksRemaining = actionIntervalTicks;
    }

    public boolean isAvailable() {
        return cooldown <= 0;
    }

    public boolean isDestroyed() {
        return health <= 0;
    }

    public boolean isSunProducer() {
        return categoryEquals("Sun Producer");
    }

    public boolean isShooter() {
        return categoryEquals("Shooter") || categoryEquals("Lobber")
            || categoryEquals("Strike-through");
    }

    public boolean isHoming() {
        return categoryEquals("Homing");
    }

    public boolean isMelee() {
        return categoryEquals("Melee");
    }

    public boolean isExplosive() {
        return categoryEquals("Explosive") || definition.hasTag("Explosive");
    }

    public boolean isTrap() {
        return definition.hasTag("Trap");
    }

    public boolean isPiercing() {
        return categoryEquals("Strike-through");
    }

    public int getProjectileCount() {
        Matcher matcher = MULTIPLIER.matcher(definition.getDamage());
        if (matcher.find()) {
            return Math.max(1, Integer.parseInt(matcher.group(1)));
        }
        if (definition.getNormalizedName().equals("threepeater")) {
            return 3;
        }
        return 1;
    }

    public ProjectileType getProjectileElementType() {
        if (definition.hasTag("Poison")) {
            return ProjectileType.POISON;
        }
        if (definition.hasTag("Fire")) {
            return ProjectileType.FIRE;
        }
        if (definition.hasTag("Ice")) {
            return ProjectileType.ICE;
        }
        return ProjectileType.NORMAL;
    }

    public int getActionIntervalTicks() {
        return actionIntervalTicks;
    }

    public int getActionTicksRemaining() {
        return actionTicksRemaining;
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

    public int getMaxHealth() {
        return maxHealth;
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

    private boolean categoryEquals(String expected) {
        return definition.getCategory().trim().toLowerCase(Locale.ROOT)
            .equals(expected.toLowerCase(Locale.ROOT));
    }
}
