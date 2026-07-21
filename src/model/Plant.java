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
    private final int plantLevel;
    private final int actionIntervalTicks;
    private final int rechargeTicks;
    private final int sunProductionBonus;
    private final boolean doubleSunChance;
    private final int chillDurationTicks;
    private final int pierceBonus;
    private int actionTicksRemaining;
    private int plantFoodShield;

    protected Plant(PlantDefinition definition) {
        this(definition, 1);
    }

    protected Plant(PlantDefinition definition, int level) {
        if (definition == null) {
            throw new IllegalArgumentException("Plant definition cannot be null.");
        }
        PlantStats stats = PlantStats.calculate(definition, level);
        this.definition = definition;
        this.name = definition.getName();
        this.plantLevel = stats.getLevel();
        this.maxHealth = stats.getMaxHealth();
        this.health = maxHealth;
        this.sunCost = stats.getCost();
        this.attackPower = stats.getDamage();
        this.actionIntervalTicks = Math.max(1, (int) Math.round(
            stats.getActionIntervalSeconds() * Game.TICKS_PER_SECOND));
        this.rechargeTicks = Math.max(0, (int) Math.round(
            stats.getRechargeSeconds() * Game.TICKS_PER_SECOND));
        this.sunProductionBonus = stats.getSunProductionBonus();
        this.doubleSunChance = stats.hasDoubleSunChance();
        this.chillDurationTicks = 50 + stats.getChillBonusTicks();
        this.pierceBonus = stats.getPierceBonus();
        this.actionTicksRemaining = actionIntervalTicks;
    }

    public void attack() {
        actionTicksRemaining = 0;
    }

    public void takeDamage(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Damage cannot be negative.");
        }
        int remaining = amount;
        if (plantFoodShield > 0) {
            int absorbed = Math.min(plantFoodShield, remaining);
            plantFoodShield -= absorbed;
            remaining -= absorbed;
        }
        health = Math.max(0, health - remaining);
    }

    public void healToFull() {
        health = maxHealth;
    }

    public void usePlantFood() {
        healToFull();
        plantFoodShield = Math.max(plantFoodShield, maxHealth);
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
        return categoryEquals("Strike-through") || pierceBonus > 0;
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

    public int getPlantLevel() { return plantLevel; }
    public int getActionIntervalTicks() { return actionIntervalTicks; }
    public int getActionTicksRemaining() { return actionTicksRemaining; }
    public int getRechargeTicks() { return rechargeTicks; }
    public int getSunProductionBonus() { return sunProductionBonus; }
    public boolean hasDoubleSunChance() { return doubleSunChance; }
    public int getChillDurationTicks() { return chillDurationTicks; }
    public int getPlantFoodShield() { return plantFoodShield; }

    public PlantDefinition getDefinition() { return definition; }
    public String getName() { return name; }
    public int getHealth() { return health; }
    public int getMaxHealth() { return maxHealth; }
    public int getSunCost() { return sunCost; }
    public int getAttackPower() { return attackPower; }
    public int getCooldown() { return cooldown; }

    public void setCooldown(int cooldown) {
        this.cooldown = Math.max(0, cooldown);
    }

    public GridPosition getPosition() { return position; }

    public void setPosition(GridPosition position) {
        this.position = position;
    }

    private boolean categoryEquals(String expected) {
        return definition.getCategory().trim().toLowerCase(Locale.ROOT)
            .equals(expected.toLowerCase(Locale.ROOT));
    }
}
